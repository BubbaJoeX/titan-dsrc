package script.space.atmo;

import script.*;
import script.library.*;

/**
 * Traffic control for a starport with a landing well (titan_atmo_starport): one planet-map landing point for the
 * whole well. A pilot's "Land Here" gets a free hangar bay that fits the ship (datatables/space/titan_starport_bays.tab,
 * titan_starport_ships.tab); the autopilot flies to a hover over the shaft, then the ship sinks down the well and backs
 * into the bay (vessel.wellInPath) and the bay's lounge is let to its owner while it is berthed. Taking the helm in a
 * bay flies it back out of the well (vessel.wellOutPath) before the pilot gets control at the hover.
 *
 * Created by travel.initializeStarport for structures with rows in the bays table (one per starport, in its base
 * objects), standing at the hover over the shaft. The shuttle service's reserved bay is never let.
 */
public class starport_traffic extends script.base_script
{
    public starport_traffic()
    {
    }

    public static final String SCRIPT = "space.atmo.starport_traffic";
    public static final String BAYS_TABLE = "datatables/space/titan_starport_bays.iff";
    public static final String SHIPS_TABLE = "datatables/space/titan_starport_ships.iff";

    public static final String OV_STRUCTURE = "starport.traffic.structure";
    public static final String OV_SHIP = "starport.traffic.bay.ship.";     // + row: the ship holding that bay
    public static final String OV_STATE = "starport.traffic.bay.state.";   // + row: INBOUND / BERTHED / OUTBOUND
    public static final String OV_SINCE = "starport.traffic.bay.since.";   // + row: game time of the last change
    public static final String OV_FEE = "starport.traffic.fee";           // docking fee (credits), default DEFAULT_FEE
    public static final String OV_NAME = "starport.traffic.name";

    public static final String OV_SHIP_BAY = "atmo.starport.bay";         // on the ship: its bay row
    public static final String OV_SHIP_CONTROL = "atmo.starport.control"; // on the ship: this traffic control
    // on a berthed ship: no line-of-sight test to use it (board, radial menus). The engine aims its LOS ray at the top
    // of the target's bounding sphere - for a ship in a bay that is up through the bay's ceiling - so people on the bay
    // floor were told they "can't see" their own ship (CreatureObject::canManipulate)
    public static final String OV_SKIP_LOS = "skipLosCheck";

    public static final int INBOUND = 1;
    public static final int BERTHED = 2;
    public static final int OUTBOUND = 3;

    public static final int DEFAULT_FEE = 500;
    public static final int INBOUND_TIMEOUT = 900;          // a bay held for a ship that never arrives is freed
    public static final float CHECK_INTERVAL = 60.0f;
    public static final float TURN_TIME = 3.0f;             // over the shaft: turn onto the bay's heading first
    public static final float TAKEOFF_ALT = 500.0f;         // the autopilot's cruise climb from far away...
    public static final float TAKEOFF_ALT_NEAR = 120.0f;    // ...and from within NEAR_RANGE of the well
    public static final float NEAR_RANGE = 2000.0f;
    // restricted airspace round the starport (setShipNoFlyZone): pilots can't fly in - landing and leaving are flown
    // by traffic control - so ships don't collide in the well or grief the bays. Gods are exempt.
    public static final float NO_FLY_RADIUS = 150.0f;       // the ring, the halls and the yacht pads
    public static final float NO_FLY_HEIGHT = 150.0f;       // over the well's rim

    // ------------------------------------------------------------------
    // the well and its bays
    // ------------------------------------------------------------------

    /** One bay on one level, in world space. */
    public static class Bay
    {
        public int row, bay, level;
        public String kind, lounge;
        public boolean reserved;
        public float x, y, z, yaw, depth, width;
    }

    /** One ship class that berths in bays. */
    public static class Ship
    {
        public String name;
        public float landY, centerX, centerZ, along, across, yawOffset;
    }

    /**
     * Traffic control for a starport with a landing well and bays (null when it has none): created at the hover over
     * the shaft. travel.initializeStarport keeps it among the starport's base objects.
     */
    public static obj_id create(obj_id structure, String name) throws InterruptedException
    {
        float[] well = getWell(structure);
        if (well == null || getBays(structure).length == 0)
            return null;
        location hover = new location(well[0], well[2] + vessel.WELL_HOVER, well[1], getLocation(structure).area, obj_id.NULL_ID);
        obj_id control = createObject(vessel.SERVICE_OBJECT, hover);
        if (!isIdValid(control))
            return null;
        setObjVar(control, OV_STRUCTURE, structure);
        setObjVar(control, OV_NAME, name);
        attachScript(control, SCRIPT);
        return control;
    }

    private static obj_id structure(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, OV_STRUCTURE) ? getObjIdObjVar(self, OV_STRUCTURE) : null;
    }

    /** Building-local engine point -> world (as travel's well: the structure's position plus its rotation). */
    private static float[] toWorld(obj_id structure, float x, float y, float z) throws InterruptedException
    {
        float[] r = travel.transformDeltaWorldYaw(structure, x, z);
        location s = getLocation(structure);
        return new float[]{ s.x + r[0], s.y + y, s.z + r[1] };
    }

    public static Bay[] getBays(obj_id structure) throws InterruptedException
    {
        if (!isIdValid(structure) || !dataTableOpen(BAYS_TABLE))
            return new Bay[0];
        String template = getTemplateName(structure);
        float sYaw = getYaw(structure);
        java.util.Vector out = new java.util.Vector();
        int rows = dataTableGetNumRows(BAYS_TABLE);
        for (int i = 0; i < rows; ++i)
        {
            dictionary d = dataTableGetRow(BAYS_TABLE, i);
            if (!template.equals(d.getString("structure")))
                continue;
            Bay b = new Bay();
            b.row = i;
            b.bay = d.getInt("bay");
            b.level = d.getInt("level");
            b.kind = d.getString("kind");
            b.lounge = d.getString("lounge");
            b.reserved = d.getInt("reserved") != 0;
            float[] w = toWorld(structure, d.getFloat("x"), d.getFloat("y"), d.getFloat("z"));
            b.x = w[0];
            b.y = w[1];
            b.z = w[2];
            b.yaw = d.getFloat("yaw") + sYaw;
            b.depth = d.getFloat("depth");
            b.width = d.getFloat("width");
            out.add(b);
        }
        Bay[] a = new Bay[out.size()];
        out.toArray(a);
        return a;
    }

    public static Bay getBay(obj_id structure, int row) throws InterruptedException
    {
        for (Bay b : getBays(structure))
            if (b.row == row)
                return b;
        return null;
    }

    public static Ship getShip(obj_id ship) throws InterruptedException
    {
        if (!isIdValid(ship) || !dataTableOpen(SHIPS_TABLE))
            return null;
        int row = dataTableSearchColumnForString(getTemplateName(ship), "template", SHIPS_TABLE);
        if (row < 0)
            return null;
        dictionary d = dataTableGetRow(SHIPS_TABLE, row);
        Ship s = new Ship();
        s.name = d.getString("name");
        s.landY = d.getFloat("land_y");
        s.centerX = d.getFloat("center_x");
        s.centerZ = d.getFloat("center_z");
        s.along = d.getFloat("along");
        s.across = d.getFloat("across");
        s.yawOffset = d.getFloat("yaw_offset");
        return s;
    }

    /** The shaft: {x, z, rim y} in world space, from the starport's row in titan_vessel_services.tab. */
    public static float[] getWell(obj_id structure) throws InterruptedException
    {
        if (!isIdValid(structure) || !dataTableOpen(vessel.SERVICE_TABLE))
            return null;
        int row = dataTableSearchColumnForString(getTemplateName(structure), "structure", vessel.SERVICE_TABLE);
        if (row < 0 || dataTableGetInt(vessel.SERVICE_TABLE, row, "well") != 1)
            return null;
        float[] w = toWorld(structure, dataTableGetFloat(vessel.SERVICE_TABLE, row, "well_x"),
                dataTableGetFloat(vessel.SERVICE_TABLE, row, "well_top"), dataTableGetFloat(vessel.SERVICE_TABLE, row, "well_z"));
        return new float[]{ w[0], w[2], w[1] };
    }

    /** The ship's parked pose in a bay {x, y, z, heading, pitch}: its hull centred on the bay's clear floor. */
    public static float[] berthPose(Bay b, Ship s)
    {
        float h = b.yaw + s.yawOffset;
        double r = Math.toRadians(h);
        float x = b.x - (float)(s.centerX * Math.cos(r) + s.centerZ * Math.sin(r));
        float z = b.z - (float)(-s.centerX * Math.sin(r) + s.centerZ * Math.cos(r));
        return new float[]{ x, b.y + s.landY, z, h, 0.0f };
    }

    /** The hover over the shaft a ship's approach ends at (and its climb-out starts from). */
    public static float[] hoverPose(float[] well, Ship s, float heading)
    {
        return new float[]{ well[0], well[2] + vessel.WELL_HOVER + s.landY, well[1], heading, 0.0f };
    }

    /**
     * Where people leaving a ship berthed in a bay are set down (space_transition.disembarkShip): on the bay floor in
     * front of the lounge door, behind the hull - the terrain under the ship is cut away. Null when not berthed here.
     */
    public static location disembarkPoint(obj_id ship) throws InterruptedException
    {
        if (!isIdValid(ship) || !hasObjVar(ship, OV_SHIP_BAY) || !hasObjVar(ship, OV_SHIP_CONTROL))
            return null;
        obj_id control = getObjIdObjVar(ship, OV_SHIP_CONTROL);
        if (!isIdValid(control) || !exists(control))
            return null;
        Bay b = getBay(structure(control), getIntObjVar(ship, OV_SHIP_BAY));
        if (b == null)
            return null;
        double h = Math.toRadians(b.yaw);              // the bay's heading faces the shaft: the lounge is behind
        float back = b.depth / 2 + 0.75f;
        return new location(b.x - (float)Math.sin(h) * back, b.y, b.z - (float)Math.cos(h) * back, getLocation(ship).area, null);
    }

    /** Does the ship fit the bay (its hull along and across the bay's clear floor)? */
    public static boolean fits(Bay b, Ship s)
    {
        return s.along <= b.depth && s.across <= b.width;
    }

    private static boolean isFree(obj_id self, Bay b) throws InterruptedException
    {
        if (b.reserved)
            return false;
        if (!hasObjVar(self, OV_SHIP + b.row))
            return true;
        obj_id ship = getObjIdObjVar(self, OV_SHIP + b.row);
        return !isIdValid(ship) || !exists(ship);
    }

    /** The bay to give a ship: the smallest free one it fits, top levels first. */
    public static Bay pickBay(obj_id self, Ship s) throws InterruptedException
    {
        Bay best = null;
        for (Bay b : getBays(structure(self)))
        {
            if (!isFree(self, b) || !fits(b, s))
                continue;
            if (best == null || b.depth * b.width < best.depth * best.width
                    || (b.depth * b.width == best.depth * best.width && b.level < best.level))
                best = b;
        }
        return best;
    }

    public static int freeBays(obj_id self) throws InterruptedException
    {
        int n = 0;
        for (Bay b : getBays(structure(self)))
            if (isFree(self, b))
                ++n;
        return n;
    }

    private static String bayName(Bay b)
    {
        return "Bay " + b.bay + ", level " + b.level + " (" + b.kind + ")";
    }

    private static obj_id loungeCell(obj_id self, Bay b) throws InterruptedException
    {
        obj_id st = structure(self);
        return isIdValid(st) ? getCellId(st, b.lounge) : null;
    }

    private static void tell(obj_id ship, obj_id pilot, String text) throws InterruptedException
    {
        if (isIdValid(pilot) && exists(pilot))
            sendSystemMessageTestingOnly(pilot, text);
        if (isIdValid(ship) && exists(ship))
            for (obj_id p : vessel.getPlayersAboard(ship))
                if (p != pilot)
                    sendSystemMessageTestingOnly(p, text);
    }

    // ------------------------------------------------------------------
    // setup, the map, upkeep
    // ------------------------------------------------------------------

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "trafficSetup", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "trafficSetup", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self) throws InterruptedException
    {
        removePlanetaryMapLocation(self);
        clearShipNoFlyZone(self);
        return SCRIPT_CONTINUE;
    }

    /** Lounges of the rentable bays are private until let; the landing point goes on the planet map. */
    public int trafficSetup(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id st = structure(self);
        if (!isIdValid(st))
            return SCRIPT_CONTINUE;
        for (Bay b : getBays(st))
        {
            obj_id cell = loungeCell(self, b);
            if (!isIdValid(cell))
                continue;
            if (b.reserved)
                permissionsMakePublic(cell);
            else if (isFree(self, b))
            {
                permissionsMakePrivate(cell);
                permissionsRemoveAllAllowed(cell);
            }
        }
        // the registry's landing-point schema, so the planet map's "Land Here" and take-off detection reach us
        location here = getLocation(self);
        setObjVar(self, atmo_landing_registry.OBJVAR_LOC, here);
        if (!hasObjVar(self, OV_NAME))
            setObjVar(self, OV_NAME, "Atmo Starport");
        setObjVar(self, atmo_landing_registry.OBJVAR_NAME, getStringObjVar(self, OV_NAME) + " Docking");
        updateMap(self);
        restrictAirspace(self);
        if (!utils.hasScriptVar(self, "starport.traffic.ticking"))
        {
            utils.setScriptVar(self, "starport.traffic.ticking", 1);
            messageTo(self, "trafficCheck", null, CHECK_INTERVAL, false);
        }
        return SCRIPT_CONTINUE;
    }

    private static void restrictAirspace(obj_id self) throws InterruptedException
    {
        float[] well = getWell(structure(self));
        if (well != null && space_transition.isAtmosphericFlightScene())
            setShipNoFlyZone(self, well[0], well[1], NO_FLY_RADIUS, well[2] + NO_FLY_HEIGHT);
    }

    private void updateMap(obj_id self) throws InterruptedException
    {
        removePlanetaryMapLocation(self);
        if (!space_transition.isAtmosphericFlightScene())
            return;
        location here = getLocation(self);
        int free = freeBays(self);
        String name = getStringObjVar(self, atmo_landing_registry.OBJVAR_NAME) + " (" + free + (free == 1 ? " bay" : " bays") + " free)";
        addPlanetaryMapLocation(self, name, (int)here.x, (int)here.z, atmo_landing_registry.MAP_CATEGORY,
                atmo_landing_registry.MAP_SUBCATEGORY, MLT_DYNAMIC, free > 0 ? MLF_ACTIVE : MLF_INACTIVE);
    }

    /** Free bays held for ships that are gone, or that never arrived. */
    public int trafficCheck(obj_id self, dictionary params) throws InterruptedException
    {
        boolean changed = false;
        int now = getGameTime();
        for (Bay b : getBays(structure(self)))
        {
            if (!hasObjVar(self, OV_SHIP + b.row))
                continue;
            obj_id ship = getObjIdObjVar(self, OV_SHIP + b.row);
            int state = getIntObjVar(self, OV_STATE + b.row);
            int since = hasObjVar(self, OV_SINCE + b.row) ? getIntObjVar(self, OV_SINCE + b.row) : now;
            if (!isIdValid(ship) || !exists(ship) || (state == INBOUND && now - since > INBOUND_TIMEOUT))
            {
                release(self, b, ship);
                changed = true;
            }
            else if (state == BERTHED && !hasObjVar(ship, OV_SKIP_LOS))
                setObjVar(ship, OV_SKIP_LOS, 1);      // ships berthed before the flag existed
        }
        if (changed)
            updateMap(self);
        restrictAirspace(self);         // (again after a script reload, which keeps no engine state)
        messageTo(self, "trafficCheck", null, CHECK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    private static void setState(obj_id self, Bay b, obj_id ship, int state) throws InterruptedException
    {
        setObjVar(self, OV_SHIP + b.row, ship);
        setObjVar(self, OV_STATE + b.row, state);
        setObjVar(self, OV_SINCE + b.row, getGameTime());
    }

    /** The bay is free again: its lounge locked, the ship's claim on it gone. */
    private static void release(obj_id self, Bay b, obj_id ship) throws InterruptedException
    {
        removeObjVar(self, OV_SHIP + b.row);
        removeObjVar(self, OV_STATE + b.row);
        removeObjVar(self, OV_SINCE + b.row);
        obj_id cell = loungeCell(self, b);
        if (isIdValid(cell))
        {
            permissionsRemoveAllAllowed(cell);
            permissionsMakePrivate(cell);
        }
        if (isIdValid(ship) && exists(ship))
        {
            removeObjVar(ship, OV_SHIP_BAY);
            removeObjVar(ship, OV_SHIP_CONTROL);
            removeObjVar(ship, OV_SKIP_LOS);
            if (hasObjVar(ship, "atmo.landing"))
                removeObjVar(ship, "atmo.landing");
        }
    }

    private Bay bayOf(obj_id self, obj_id ship) throws InterruptedException
    {
        if (!isIdValid(ship) || !hasObjVar(ship, OV_SHIP_BAY) || !hasObjVar(ship, OV_SHIP_CONTROL)
                || getObjIdObjVar(ship, OV_SHIP_CONTROL) != self)
            return null;
        Bay b = getBay(structure(self), getIntObjVar(ship, OV_SHIP_BAY));
        if (b == null || !hasObjVar(self, OV_SHIP + b.row) || getObjIdObjVar(self, OV_SHIP + b.row) != ship)
            return null;
        return b;
    }

    // ------------------------------------------------------------------
    // landing: clearance, the approach, down the well
    // ------------------------------------------------------------------

    /**
     * A landing request (player_vehicle.handleAtmoLandingRequest, or the dev console with no pilot: no fee): pick a bay,
     * take the fee and send the ship's autopilot to the hover over the shaft.
     */
    public int handleLandingRequest(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        obj_id pilot = params.getObjId("pilot");
        if (!isIdValid(ship) || !exists(ship))
            return SCRIPT_CONTINUE;
        String control = "\\#00ccff[" + getStringObjVar(self, OV_NAME) + " Control]: ";
        // a booking left on the ship by a traffic control that is gone (its starport removed) or whose bay is not held
        // for this ship any more: drop it
        if (hasObjVar(ship, OV_SHIP_BAY) && bayOf(self, ship) == null)
        {
            obj_id other = hasObjVar(ship, OV_SHIP_CONTROL) ? getObjIdObjVar(ship, OV_SHIP_CONTROL) : null;
            if (!isIdValid(other) || !exists(other) || other == self)
            {
                removeObjVar(ship, OV_SHIP_BAY);
                removeObjVar(ship, OV_SHIP_CONTROL);
                removeObjVar(ship, OV_SKIP_LOS);
                if (hasObjVar(ship, "atmo.landing"))
                    removeObjVar(ship, "atmo.landing");
            }
        }
        if (hasObjVar(ship, "atmo.landing.docked") || hasObjVar(ship, "atmo.landing.landed_at"))
        {
            tell(ship, pilot, control + "\\#ff4444Your ship is already landed. Take off first.");
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(ship, OV_SHIP_BAY))
        {
            tell(ship, pilot, control + "\\#ffaa44You already hold a bay here; your approach is under way.");
            return SCRIPT_CONTINUE;
        }
        obj_id st = structure(self);
        float[] well = getWell(st);
        Ship s = getShip(ship);
        if (well == null)
            return SCRIPT_CONTINUE;
        if (s == null)
        {
            tell(ship, pilot, control + "\\#ff4444Your ship does not fit our hangar bays. Large craft use the landing pads outside the ring.");
            return SCRIPT_CONTINUE;
        }
        Bay b = pickBay(self, s);
        if (b == null)
        {
            tell(ship, pilot, control + "\\#ff4444Every bay that takes a " + s.name + " is let. Please try again later.");
            return SCRIPT_CONTINUE;
        }
        int fee = hasObjVar(self, OV_FEE) ? getIntObjVar(self, OV_FEE) : DEFAULT_FEE;
        if (isIdValid(pilot) && fee > 0)
        {
            if (money.getTotalMoney(pilot) < fee
                    || !transferBankCreditsToNamedAccount(pilot, money.ACCT_TRAVEL, fee, "noHandler", "noHandler", new dictionary()))
            {
                tell(ship, pilot, control + "\\#ff4444The docking fee is " + fee + " credits; you cannot cover it.");
                return SCRIPT_CONTINUE;
            }
        }
        setState(self, b, ship, INBOUND);
        setObjVar(ship, OV_SHIP_BAY, b.row);
        setObjVar(ship, OV_SHIP_CONTROL, self);
        setObjVar(ship, "atmo.landing.target", self);
        setObjVar(ship, "atmo.landing.name", getStringObjVar(self, OV_NAME));

        float[] hover = hoverPose(well, s, b.yaw + s.yawOffset);
        dictionary fly = new dictionary();
        fly.put("x", hover[0]);
        fly.put("z", hover[2]);
        location from = getLocation(ship);
        float range = (float)Math.hypot(from.x - hover[0], from.z - hover[2]);
        fly.put("takeoffAlt", range < NEAR_RANGE ? TAKEOFF_ALT_NEAR : TAKEOFF_ALT);
        fly.put("landingAlt", Math.max(2.0f, hover[1] - getHeightAtLocation(hover[0], hover[2])));
        fly.put("landingY", hover[1]);
        fly.put("landingPointTarget", self);
        if (isIdValid(pilot))
            fly.put("owner", pilot);
        else
            fly.put("npcControlled", true);
        shipClearFlightPath(ship);         // a scripted path (a parked hold) owns the transform; the autopilot needs it
        messageTo(ship, "shipAutoPilotEngage", fly, 0, false);
        updateMap(self);

        tell(ship, pilot, control + "Clearance granted. You are assigned " + bayName(b) + ".");
        if (isIdValid(pilot) && fee > 0)
            tell(ship, pilot, "\\#88ddaa  Docking fee of " + fee + " credits charged. The bay's lounge is yours while you are berthed.");
        tell(ship, pilot, "\\#aaddff  Auto-pilot engaged: approach to the landing well, then down to your bay.");
        return SCRIPT_CONTINUE;
    }

    /** The autopilot has the ship hovering over the shaft (combat_ship, AP_ARRIVED): turn, sink, back into the bay. */
    public int handleShipArrived(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        Bay b = bayOf(self, ship);
        if (b == null || getIntObjVar(self, OV_STATE + b.row) != INBOUND)
            return SCRIPT_CONTINUE;
        Ship s = getShip(ship);
        float[] well = getWell(structure(self));
        if (s == null || well == null)
            return SCRIPT_CONTINUE;
        shipClearAutopilot(ship);
        float[] pose = berthPose(b, s);
        location at = getLocation(ship);
        float[] hover = { at.x, at.y, at.z, pose[3], 0.0f };
        vessel.Path path = new vessel.Path().key(0.0f, at.x, at.y, at.z, vessel.getVesselYaw(ship), 0.0f, 0.0f);
        // over the shaft's axis, turned onto the bay's heading, then the well
        float[] axis = { well[0], at.y, well[1], pose[3], 0.0f };
        path.append(vessel.wellInPath(axis, pose, 0.0f), TURN_TIME);
        vessel.fly(ship, path, null);
        dictionary d = new dictionary();
        d.put("ship", ship);
        messageTo(self, "bayBerthed", d, path.duration() + 0.5f, false);
        tell(ship, getOwner(ship), "\\#00ccff[" + getStringObjVar(self, OV_NAME) + " Control]: Bringing you down the well to " + bayName(b) + ".");
        return SCRIPT_CONTINUE;
    }

    /** Down in the bay: landed (taking the helm flies it back out), the lounge let to the owner. */
    public int bayBerthed(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        Bay b = bayOf(self, ship);
        if (b == null || getIntObjVar(self, OV_STATE + b.row) != INBOUND)
            return SCRIPT_CONTINUE;
        setState(self, b, ship, BERTHED);
        setObjVar(ship, "atmo.landing.landed_at", self);
        setObjVar(ship, OV_SKIP_LOS, 1);
        obj_id owner = getOwner(ship);
        obj_id cell = loungeCell(self, b);
        if (isIdValid(cell) && isIdValid(owner))
            permissionsAddAllowed(cell, getFirstName(owner));
        tell(ship, owner, "\\#88ddaa[" + getStringObjVar(self, OV_NAME) + " Control]: Welcome. You are berthed in " + bayName(b)
                + ". Your lounge is at the back of the bay; taking the helm flies you back out of the well.");
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // leaving: out of the bay, up the well
    // ------------------------------------------------------------------

    /**
     * The ship is leaving (atmo_landing_registry.detachShipFromLandingPoint: its pilot took the helm, or it was packed,
     * destroyed or ejected): fly it out of the well when it is berthed, else just free the bay.
     */
    public int handleShipDeparted(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        Bay b = bayOf(self, ship);
        if (b == null)
            return SCRIPT_CONTINUE;
        int state = getIntObjVar(self, OV_STATE + b.row);
        Ship s = getShip(ship);
        float[] well = getWell(structure(self));
        if (state != BERTHED || s == null || well == null || !exists(ship))
        {
            if (state != OUTBOUND)
            {
                release(self, b, ship);
                updateMap(self);
            }
            return SCRIPT_CONTINUE;
        }
        setState(self, b, ship, OUTBOUND);
        removeObjVar(ship, "atmo.landing");
        removeObjVar(ship, OV_SKIP_LOS);
        obj_id cell = loungeCell(self, b);
        if (isIdValid(cell))
        {
            permissionsRemoveAllAllowed(cell);
            permissionsMakePrivate(cell);
        }
        location at = getLocation(ship);
        float[] pose = { at.x, at.y, at.z, vessel.getVesselYaw(ship), 0.0f };
        vessel.Path path = vessel.wellOutPath(pose, hoverPose(well, s, pose[3]), 0.0f);
        vessel.fly(ship, path, null);
        dictionary d = new dictionary();
        d.put("ship", ship);
        messageTo(self, "bayClear", d, path.duration() + 0.5f, false);
        tell(ship, getOwner(ship), "\\#00ccff[" + getStringObjVar(self, OV_NAME) + " Control]: Departure cleared. Taking you up the well; you have the helm at the top.");
        return SCRIPT_CONTINUE;
    }

    /** Out over the shaft: the pilot has the ship, the bay is free. */
    public int bayClear(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        Bay b = bayOf(self, ship);
        if (b == null || getIntObjVar(self, OV_STATE + b.row) != OUTBOUND)
            return SCRIPT_CONTINUE;
        if (exists(ship))
        {
            shipClearFlightPath(ship);
            tell(ship, getOwner(ship), "\\#88ddaa[" + getStringObjVar(self, OV_NAME) + " Control]: You are clear of the well. Safe flying.");
        }
        release(self, b, ship);
        updateMap(self);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // dev console (titan.dev_console vessel <scene> dock|undock): no player needed
    // ------------------------------------------------------------------

    /** Land a ship without a pilot (no fee): the same request a pilot makes. */
    public int trafficDock(obj_id self, dictionary params) throws InterruptedException
    {
        return handleLandingRequest(self, params);
    }

    /** Take a berthed ship out as if its pilot took the helm. */
    public int trafficUndock(obj_id self, dictionary params) throws InterruptedException
    {
        return handleShipDeparted(self, params);
    }
}
