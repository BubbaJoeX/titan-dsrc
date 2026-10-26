package script.library;

import script.*;

import java.util.Vector;

/**
 * Titan boardable vessels: POB ships players walk into up a ramp (the starport transport, the Sorosuub yacht
 * variant). The vessel's moving parts (ramp, landing legs) are hp_dyn "part" objvars the client animates; it flies
 * scripted flight paths (shipSetFlightPath) that can end on the ground; its ramp is a cell of its own, locked by
 * cell permissions while the ramp is up, and its cabin admits ticketed passengers only.
 *
 * Vessel data: datatables/space/titan_vessels.tab (written by SwgMayaMCP examples/make_titan_vessels.py from the
 * Maya build): the hull frame above the ground point when parked, the ramp / cabin cell names and the parts.
 */
public class vessel extends script.base_script
{
    public vessel()
    {
    }

    public static final String TABLE = "datatables/space/titan_vessels.iff";
    public static final String SERVICE_TABLE = "datatables/space/titan_vessel_services.iff";
    public static final String HYPERSPACE_TABLE = "datatables/space/titan_hyperspace.iff";

    public static final String SCRIPT_CONTROLLER = "space.vessel.vessel_controller";
    public static final String SCRIPT_CELL = "space.vessel.vessel_cell";
    public static final String SCRIPT_SERVICE = "space.vessel.shuttle_service";
    public static final String SCRIPT_PASSENGER = "space.vessel.vessel_passenger";
    public static final String SERVICE_OBJECT = "object/tangible/theme_park/invisible_object.iff";   // the pad controller

    // objvars on a vessel
    public static final String OV_SERVICE = "vessel.service";          // the shuttle service (pad controller) flying it
    public static final String OV_STATE = "vessel.state";
    public static final String OV_FLIGHT = "vessel.flight";            // flight number shared by the vessels of one trip
    public static final String OV_DEST_PLANET = "vessel.dest.planet";
    public static final String OV_DEST_POINT = "vessel.dest.point";
    public static final String OV_ETA = "vessel.eta";                  // game time the hyperspace leg ends
    public static final String OV_RAMP_OPEN = "vessel.ramp_open";
    public static final String OV_GEAR_DOWN = "vessel.gear_down";
    public static final String OV_PAX_IDS = "vessel.pax.ids";
    public static final String OV_PAX_PLANETS = "vessel.pax.planets";
    public static final String OV_PAX_POINTS = "vessel.pax.points";
    public static final String OV_PAX_TICKETS = "vessel.pax.tickets";
    // scriptvars on a vessel: the flight path it is flying (to continue it smoothly)
    public static final String SV_PATH = "vessel.path";
    public static final String SV_PATH_DONE = "vessel.path_done";      // handler messaged when the path ends

    // objvars on a passenger (vessel_passenger)
    public static final String OV_VOYAGE = "vessel_voyage";
    public static final String OV_VOYAGE_FLIGHT = "vessel_voyage.flight";
    public static final String OV_VOYAGE_PLANET = "vessel_voyage.planet";
    public static final String OV_VOYAGE_POINT = "vessel_voyage.point";
    public static final String OV_VOYAGE_ETA = "vessel_voyage.eta";
    public static final String OV_VOYAGE_FROM = "vessel_voyage.from";
    public static final String OV_VOYAGE_TEMPLATE = "vessel_voyage.template";
    public static final String OV_VOYAGE_HOLD = "vessel_voyage.hold";

    public static final int STATE_PARKED = 0;          // on the ground, ramp up
    public static final int STATE_BOARDING = 1;        // on the ground, ramp down
    public static final int STATE_DEPARTING = 2;       // ramp closing, lift-off, climb-out
    public static final int STATE_CRUISE = 3;          // atmospheric flight to another port on the same planet
    public static final int STATE_HYPERSPACE = 4;      // in transit between planets
    public static final int STATE_HOLDING = 5;         // waiting for the pad
    public static final int STATE_ARRIVING = 6;        // approach and touchdown
    public static final int STATE_RETIRING = 7;        // empty, flying off to be removed
    public static final int STATE_AIRBORNE = 8;        // a privately flown vessel (the yacht) in the air under its owner's orders
    public static final int STATE_STOWED = 9;          // empty and out of sight, kept by its service for the next flight

    // objvar on a shuttle service: its stowed vessels. Service vessels are reused rather than destroyed: destroying POB
    // ships in an atmospheric-flight scene corrupts the game server's heap (README, "Boardable vessels").
    public static final String OV_SPARES = "vessel.service.spares";

    public static final String HYPERSPACE_APPEARANCE = "appearance/titan_hyperspace_tunnel.apt";
    public static final float NOTIFY_RANGE = 96.0f;
    public static final float BOARDING_RANGE = 64.0f;   // ticket holders this close to a boarding vessel get cabin access

    // ------------------------------------------------------------------
    // vessel data
    // ------------------------------------------------------------------

    public static class Config
    {
        public String template;
        public float landX, landY, landZ, landPitch;
        public String rampCell, cabinCell;
        public String[][] parts;      // slot, hardpoint, appearance, mode, ax, ay, az, amount, seconds
        public float cruiseSpeed;
        public String shipName;
    }

    public static Config getConfig(String template) throws InterruptedException
    {
        if (template == null)
            return null;
        int row = dataTableSearchColumnForString(template, "template", TABLE);
        if (row < 0)
            return null;
        dictionary d = dataTableGetRow(TABLE, row);
        if (d == null)
            return null;
        Config c = new Config();
        c.template = template;
        c.landX = d.getFloat("landX");
        c.landY = d.getFloat("landY");
        c.landZ = d.getFloat("landZ");
        c.landPitch = d.getFloat("landPitch");
        c.rampCell = d.getString("rampCell");
        c.cabinCell = d.getString("cabinCell");
        c.cruiseSpeed = d.getFloat("cruiseSpeed");
        c.shipName = d.getString("shipName");
        String[] parts = split(d.getString("parts"), ';');
        c.parts = new String[parts.length][];
        for (int i = 0; i < parts.length; ++i)
            c.parts[i] = split(parts[i], ',');
        return c;
    }

    // A template's hull plan in its frame {half width, length aft, length fore} (titan_vessels.tab hullHalfWidth,
    // hullLengthAft, hullLengthFore), or null when unknown. Not a Config field: hot reloads don't reload nested classes.
    public static float[] getHullPlan(String template) throws InterruptedException
    {
        int row = template == null ? -1 : dataTableSearchColumnForString(template, "template", TABLE);
        if (row < 0)
            return null;
        dictionary d = dataTableGetRow(TABLE, row);
        if (d == null || !d.containsKey("hullHalfWidth") || d.getFloat("hullHalfWidth") <= 0.0f)
            return null;
        return new float[]{d.getFloat("hullHalfWidth"), d.getFloat("hullLengthAft"), d.getFloat("hullLengthFore")};
    }

    public static Config getConfig(obj_id ship) throws InterruptedException
    {
        return isIdValid(ship) ? getConfig(getTemplateName(ship)) : null;
    }

    public static boolean isVessel(obj_id obj) throws InterruptedException
    {
        return isIdValid(obj) && exists(obj) && hasScript(obj, SCRIPT_CONTROLLER);
    }

    // ------------------------------------------------------------------
    // the owner's controls: the hull's radial and the helm console (space.vessel.vessel_helm) show the same menu;
    // vessel_controller.flightMenuSelect acts on it
    // ------------------------------------------------------------------

    public static boolean isLanded(obj_id ship) throws InterruptedException
    {
        int state = getState(ship);
        return state == STATE_PARKED || state == STATE_BOARDING;
    }

    // private (not a shuttle service run) and the player its owner, or staff
    public static boolean canFly(obj_id ship, obj_id player) throws InterruptedException
    {
        return !hasObjVar(ship, OV_SERVICE) && (isGod(player) || getOwner(ship) == player);
    }

    // templates declaring /private/index_color_1 and _2 (the hull and trim zones of the hue shaders)
    public static boolean isPaintable(obj_id ship) throws InterruptedException
    {
        return hue.getVarColorIndex(ship, hue.INDEX_1) >= 0 && hue.getVarColorIndex(ship, hue.INDEX_2) >= 0;
    }

    public static void addFlightMenu(obj_id ship, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!canFly(ship, player))
            return;
        int flight = mi.addRootMenu(menu_info_types.SERVER_MENU14, string_id.unlocalized("Flight"));
        if (isLanded(ship))
        {
            mi.addSubMenu(flight, menu_info_types.SERVER_MENU15, string_id.unlocalized(isRampOpen(ship) ? "Raise Ramp" : "Lower Ramp"));
            mi.addSubMenu(flight, menu_info_types.SERVER_MENU16, string_id.unlocalized("Take Off"));
        }
        else if (getState(ship) == STATE_AIRBORNE)
            mi.addSubMenu(flight, menu_info_types.SERVER_MENU17, string_id.unlocalized("Land Here"));
        if (isPaintable(ship))
        {
            int paint = mi.addRootMenu(menu_info_types.SERVER_MENU18, string_id.unlocalized("Paint"));
            mi.addSubMenu(paint, menu_info_types.SERVER_MENU19, string_id.unlocalized("Hull Colour"));
            mi.addSubMenu(paint, menu_info_types.SERVER_MENU20, string_id.unlocalized("Trim Colour"));
        }
        mi.addRootMenu(menu_info_types.SERVER_MENU21, string_id.unlocalized("Name Vessel"));
    }

    // A ship still wearing its template name: none, an encoded "@file:key" or the raw "file:key" an unresolved one reads as.
    public static boolean hasDefaultName(obj_id ship) throws InterruptedException
    {
        String n = getName(ship);
        return n == null || n.length() == 0 || n.startsWith("@") || (n.indexOf(':') > 0 && n.indexOf(' ') < 0);
    }

    // Give a vessel its titan_vessels.tab shipName unless someone has named it.
    public static void nameVessel(obj_id ship, Config c) throws InterruptedException
    {
        if (c != null && c.shipName != null && c.shipName.length() > 0 && hasDefaultName(ship))
            setName(ship, c.shipName);
    }

    public static int getState(obj_id ship) throws InterruptedException
    {
        return hasObjVar(ship, OV_STATE) ? getIntObjVar(ship, OV_STATE) : STATE_PARKED;
    }

    public static void setState(obj_id ship, int state) throws InterruptedException
    {
        setObjVar(ship, OV_STATE, state);
    }

    // ------------------------------------------------------------------
    // moving parts (hp_dyn "part" rows; TangibleObject packs them for the client)
    // ------------------------------------------------------------------

    /** Move one part to `state` (0 = stowed as modelled, 1 = deployed) after `delay` seconds. */
    public static void setPart(obj_id ship, String[] part, float state, float delay) throws InterruptedException
    {
        if (part == null || part.length < 9)
            return;
        String root = "hp_dyn." + part[0] + ".";
        setObjVar(ship, root + "kind", "part");
        setObjVar(ship, root + "hp", part[1]);
        setObjVar(ship, root + "path", part[2]);
        setObjVar(ship, root + "mode", part[3]);
        setObjVar(ship, root + "ax", utils.stringToFloat(part[4]));
        setObjVar(ship, root + "ay", utils.stringToFloat(part[5]));
        setObjVar(ship, root + "az", utils.stringToFloat(part[6]));
        setObjVar(ship, root + "amount", utils.stringToFloat(part[7]));
        setObjVar(ship, root + "seconds", utils.stringToFloat(part[8]));
        setObjVar(ship, root + "delay", delay);
        setObjVar(ship, root + "state", state);
        if (part.length >= 12)
        {
            // slide,x,y,z: stowed offset of a rot part, slid home before it turns (a ramp run out of the hull)
            setObjVar(ship, root + "mx", utils.stringToFloat(part[9]));
            setObjVar(ship, root + "my", utils.stringToFloat(part[10]));
            setObjVar(ship, root + "mz", utils.stringToFloat(part[11]));
        }
    }

    public static boolean isRampPart(String[] part)
    {
        return part != null && part.length > 0 && part[0].startsWith("ramp");
    }

    public static final String OV_GEAR_SEQ = "vessel.gear_seq";

    /** Landing legs (every part but the ramp) down or up, starting in `delay` seconds. A delayed move is sent when it
     *  starts (vessel_controller.vesselGear), not as a client-side delay: a client that first sees the ship before
     *  then builds its legs in their current pose, so they swing down in flight instead of hanging deployed from the
     *  moment the ship appears. The latest call wins. */
    public static void setGear(obj_id ship, boolean down, float delay) throws InterruptedException
    {
        int seq = (hasObjVar(ship, OV_GEAR_SEQ) ? getIntObjVar(ship, OV_GEAR_SEQ) : 0) + 1;
        setObjVar(ship, OV_GEAR_SEQ, seq);
        if (delay <= 0.25f)
        {
            applyGear(ship, down);
            return;
        }
        dictionary d = new dictionary();
        d.put("down", down);
        d.put("seq", seq);
        messageTo(ship, "vesselGear", d, delay, false);
    }

    /** vessel_controller.vesselGear: a delayed setGear coming due, unless a later call superseded it. */
    public static void gearDue(obj_id ship, dictionary params) throws InterruptedException
    {
        if (params == null || !hasObjVar(ship, OV_GEAR_SEQ) || getIntObjVar(ship, OV_GEAR_SEQ) != params.getInt("seq"))
            return;
        applyGear(ship, params.getBoolean("down"));
    }

    private static void applyGear(obj_id ship, boolean down) throws InterruptedException
    {
        Config c = getConfig(ship);
        if (c == null)
            return;
        for (String[] part : c.parts)
            if (!isRampPart(part))
                setPart(ship, part, down ? 1.0f : 0.0f, 0.0f);
        setObjVar(ship, OV_GEAR_DOWN, down ? 1 : 0);
    }

    // ------------------------------------------------------------------
    // landing sites: level ground clear of buildings, statics and other hulls under a hull plan (the deed's Call
    // Vessel pad, a summoned starship setting down, Land Here)
    // ------------------------------------------------------------------

    public static final String OV_SHIP_HULL = "vessel.hull";                // float[]{half width, length aft, length fore}
    public static final String OV_SHIP_DEED = "vessel.deed";                // on the vessel: the deed it answers to
    public static final String OV_DEED_HALF_WIDTH = "vessel_deed.half_width";
    public static final float PAD_SLOPE = 3.0f;                   // metres of rise over a pad that still count as level,
    public static final float PAD_SLOPE_PER_M = 0.02f;            // plus this much per metre of hull length
    public static final float PAD_MARGIN = 3.0f;                  // clear ground past the hull's sides

    // distance from point (px, pz) to the segment (ax, az)-(bx, bz)
    public static float pointSegment(float px, float pz, float ax, float az, float bx, float bz)
    {
        float dx = bx - ax, dz = bz - az, len2 = dx * dx + dz * dz;
        float t = len2 > 0.0f ? Math.max(0.0f, Math.min(1.0f, ((px - ax) * dx + (pz - az) * dz) / len2)) : 0.0f;
        float ex = px - (ax + dx * t), ez = pz - (az + dz * t);
        return (float)Math.sqrt(ex * ex + ez * ez);
    }

    // closest distance between two keels (segments that don't cross: the pad check never lands one across another)
    public static float segmentSegment(float[] a, float[] b)
    {
        return Math.min(Math.min(pointSegment(a[0], a[1], b[0], b[1], b[2], b[3]), pointSegment(a[2], a[3], b[0], b[1], b[2], b[3])),
                Math.min(pointSegment(b[0], b[1], a[0], a[1], a[2], a[3]), pointSegment(b[2], b[3], a[0], a[1], a[2], a[3])));
    }

    // another vessel's keel (x0, z0, x1, z1) and half width from its titan_vessels.tab hull plan, else the one stamped
    // on it or its deed's (a service shuttle has none: its frame, 15 m)
    public static float[] keelOf(obj_id other) throws InterruptedException
    {
        location at = getLocation(other);
        float[] plan = getHullPlan(getTemplateName(other));
        if (plan == null && hasObjVar(other, OV_SHIP_HULL))
            plan = getFloatArrayObjVar(other, OV_SHIP_HULL);
        obj_id deed = hasObjVar(other, OV_SHIP_DEED) ? getObjIdObjVar(other, OV_SHIP_DEED) : null;
        if (plan == null && isIdValid(deed) && exists(deed) && hasObjVar(deed, OV_DEED_HALF_WIDTH))
            plan = new float[]{getFloatObjVar(deed, OV_DEED_HALF_WIDTH), getFloatObjVar(deed, "vessel_deed.length_aft"), getFloatObjVar(deed, "vessel_deed.length_fore")};
        if (plan == null || plan.length < 3)
            return new float[]{at.x, at.z, at.x, at.z, 15.0f};
        double h = Math.toRadians(getVesselYaw(other));
        float ax = (float)Math.sin(h), az = (float)Math.cos(h);
        return new float[]{at.x - ax * plan[1], at.z - az * plan[1], at.x + ax * plan[2], at.z + az * plan[2], plan[0]};
    }

    // The hull's plan on the ground (a strip `half` either side of the keel from aft to fore along `heading`): level
    // (samples down the keel and both sides) and no building, installation, static or other vessel on it.
    public static boolean padIsClear(location pad, float heading, float half, float aft, float fore, obj_id ship) throws InterruptedException
    {
        double h = Math.toRadians(heading);
        float ax = (float)Math.sin(h), az = (float)Math.cos(h);         // along the keel
        float sx = az, sz = -ax;                                        // across
        int n = Math.max(1, (int)Math.ceil((fore - aft) / half));
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int i = 0; i <= n; ++i)
        {
            float t = aft + (fore - aft) * i / n;
            for (int side = -1; side <= 1; ++side)
            {
                float y = getHeightAtLocation(pad.x + ax * t + sx * half * side, pad.z + az * t + sz * half * side);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        if (hi - lo > PAD_SLOPE + PAD_SLOPE_PER_M * (fore - aft))
        {
            LOG("vessel", "landing site " + pad.x + "," + pad.z + " heading " + heading + ": not level (" + (hi - lo) + " m)");
            return false;
        }
        float mid = (aft + fore) / 2.0f;
        location centre = new location(pad.x + ax * mid, getHeightAtLocation(pad.x + ax * mid, pad.z + az * mid), pad.z + az * mid, pad.area, null);
        obj_id[] near = getNonCreaturesInRange(centre, (fore - aft) / 2.0f + half + 300.0f);   // a Nebulon's keel reaches 270 m
        for (int i = 0; near != null && i < near.length; ++i)
        {
            obj_id o = near[i];
            if (!isIdValid(o) || o.equals(ship) || isIdValid(getContainedBy(o)))
                continue;
            // clear ground past the hull: to another vessel's hull (its keel and half width), else past the object's
            // position (its centre) by what its type says about its size
            int got = getGameObjectType(o);
            float margin = isVessel(o) ? 10.0f : isGameObjectTypeOf(got, GOT_building) ? 15.0f
                    : isGameObjectTypeOf(got, GOT_installation) ? 8.0f : got == GOT_static ? 3.0f : -1.0f;
            if (margin < 0.0f)
                continue;
            float gap;
            if (isVessel(o))
            {
                float[] k = keelOf(o);
                gap = segmentSegment(new float[]{pad.x + ax * aft, pad.z + az * aft, pad.x + ax * fore, pad.z + az * fore}, k) - k[4];
            }
            else
            {
                location at = getLocation(o);
                gap = pointSegment(at.x, at.z, pad.x + ax * aft, pad.z + az * aft, pad.x + ax * fore, pad.z + az * fore);
            }
            if (gap < half + margin)
            {
                LOG("vessel", "landing site " + pad.x + "," + pad.z + " heading " + heading + ": " + o + " " + getTemplateName(o) + " in the way");
                return false;
            }
        }
        return true;
    }


    /** Can the ship set down on its legs straight below where it is, heading as it heads? */
    public static boolean canSetDownHere(obj_id ship) throws InterruptedException
    {
        location here = getLocation(ship);
        if (isIdValid(here.cell))
            return true;                                            // a hangar cell: its floor is the pad
        float[] plan = getHullPlan(getTemplateName(ship));
        if (plan == null && hasObjVar(ship, OV_SHIP_HULL))
            plan = getFloatArrayObjVar(ship, OV_SHIP_HULL);
        if (plan == null || plan.length < 3)
            return true;
        return padIsClear(here, getVesselYaw(ship), plan[0] + PAD_MARGIN, -plan[1], plan[2], ship);
    }

    public static boolean isGearDown(obj_id ship) throws InterruptedException
    {
        return hasObjVar(ship, OV_GEAR_DOWN) && getIntObjVar(ship, OV_GEAR_DOWN) != 0;
    }

    public static boolean isRampOpen(obj_id ship) throws InterruptedException
    {
        return hasObjVar(ship, OV_RAMP_OPEN) && getIntObjVar(ship, OV_RAMP_OPEN) != 0;
    }

    /**
     * Lower or raise the boarding ramp. The ramp cell is public while the ramp is down and private (no one admitted)
     * while it is up, so the client and the server stop anyone walking onto a raised ramp; raising it first moves
     * anyone standing on it off the ship, or into the cabin when they are a passenger.
     */
    public static void setRamp(obj_id ship, boolean open, float delay) throws InterruptedException
    {
        Config c = getConfig(ship);
        if (c == null)
            return;
        obj_id rampCell = cellOf(ship, c.rampCell);
        if (!open && isIdValid(rampCell))
        {
            obj_id[] contents = getContents(rampCell);
            if (contents != null)
                for (obj_id o : contents)
                    if (isPlayer(o) && !isGod(o))           // staff stay where they are, ticket or not
                    {
                        if (isPassenger(ship, o))
                            moveIntoCabin(ship, o);
                        else
                            moveOffShip(ship, o);
                    }
        }
        for (String[] part : c.parts)
            if (isRampPart(part))
                setPart(ship, part, open ? 1.0f : 0.0f, delay);
        setObjVar(ship, OV_RAMP_OPEN, open ? 1 : 0);
        if (isIdValid(rampCell))
        {
            if (open)
                permissionsMakePublic(rampCell);
            else
                permissionsMakePrivate(rampCell);
        }
        refreshCellAccess(ship);
    }

    // ------------------------------------------------------------------
    // a vessel flown as a player starship (an SCD in its owner's datapad; atmospheric flight, summon, landing pads):
    // a scripted flight path owns the ship's transform until it is cleared, so every hand-over to the pilot or the
    // autopilot clears it first. While it is in the air the ship is AIRBORNE (its owner's helm then offers Land Here).
    // ------------------------------------------------------------------

    public static boolean isStarship(obj_id ship) throws InterruptedException
    {
        return isVessel(ship) && !hasObjVar(ship, OV_SERVICE) && space_utils.isPobType(ship);
    }

    private static void clearFlight(obj_id ship) throws InterruptedException
    {
        shipClearFlightPath(ship);
        utils.removeScriptVar(ship, SV_PATH);
        utils.removeScriptVar(ship, SV_PATH_DONE);
    }

    /** The pilot has the helm of a ship in the air: their controls move it from here. */
    public static void releaseToPilot(obj_id ship) throws InterruptedException
    {
        clearFlight(ship);
        setState(ship, STATE_AIRBORNE);
        if (isGearDown(ship))
            setGear(ship, false, 2.0f);
    }

    /** The atmospheric autopilot (summon, landing pad) takes the ship, landed or not: ramp up, legs up as it climbs. */
    public static void releaseForAutopilot(obj_id ship) throws InterruptedException
    {
        if (isRampOpen(ship))
            setRamp(ship, false, 0.0f);
        clearFlight(ship);
        setState(ship, STATE_AIRBORNE);
        if (isGearDown(ship))
            setGear(ship, false, 3.0f);
    }

    /** Launched from the datapad (space_transition.unpackShipForPlayer): in the air, legs up, ramp shut and private. */
    public static void onUnpacked(obj_id ship) throws InterruptedException
    {
        clearFlight(ship);
        setState(ship, STATE_AIRBORNE);
        setGear(ship, false, 0.0f);
        setRamp(ship, false, 0.0f);
    }

    /** Stored in the datapad (space_transition.packShip): nothing scripted survives into the next launch. */
    public static void onPacked(obj_id ship) throws InterruptedException
    {
        clearFlight(ship);
        setState(ship, STATE_AIRBORNE);
        setGear(ship, false, 0.0f);
        setRamp(ship, false, 0.0f);
    }

    /** Snap every part (no animation for clients that see the vessel for the first time). */
    public static void initParts(obj_id ship, boolean landed) throws InterruptedException
    {
        Config c = getConfig(ship);
        if (c == null)
            return;
        for (String[] part : c.parts)
            setPart(ship, part, (landed && !isRampPart(part)) ? 1.0f : 0.0f, 0.0f);
        setObjVar(ship, OV_GEAR_DOWN, landed ? 1 : 0);
        setObjVar(ship, OV_RAMP_OPEN, 0);
        obj_id rampCell = cellOf(ship, c.rampCell);
        if (isIdValid(rampCell))
            permissionsMakePrivate(rampCell);
        obj_id cabin = cellOf(ship, c.cabinCell);
        if (isIdValid(cabin))
            permissionsMakePrivate(cabin);
    }

    /** The hyperspace tunnel round the hull, seen through the windows while in transit. */
    public static void setHyperspaceVisual(obj_id ship, boolean on) throws InterruptedException
    {
        if (on)
        {
            setObjVar(ship, "hp_dyn.zz_hyperspace.kind", "app");
            setObjVar(ship, "hp_dyn.zz_hyperspace.hp", "-");
            setObjVar(ship, "hp_dyn.zz_hyperspace.path", HYPERSPACE_APPEARANCE);
        }
        else
            removeObjVar(ship, "hp_dyn.zz_hyperspace");
    }

    // ------------------------------------------------------------------
    // cells, passengers
    // ------------------------------------------------------------------

    /** A cell of the vessel by name; null for "" (the yacht has no passenger cabin). */
    public static obj_id cellOf(obj_id ship, String name) throws InterruptedException
    {
        return name == null || name.length() == 0 ? null : getCellId(ship, name);
    }

    public static obj_id getRampCell(obj_id ship) throws InterruptedException
    {
        Config c = getConfig(ship);
        return c != null ? cellOf(ship, c.rampCell) : null;
    }

    public static obj_id getCabinCell(obj_id ship) throws InterruptedException
    {
        Config c = getConfig(ship);
        return c != null ? cellOf(ship, c.cabinCell) : null;
    }

    /** Players inside the vessel (any cell). */
    public static obj_id[] getPlayersAboard(obj_id ship) throws InterruptedException
    {
        Vector out = new Vector();
        String[] cells = getCellNames(ship);
        if (cells != null)
            for (String name : cells)
            {
                obj_id cell = getCellId(ship, name);
                obj_id[] contents = isIdValid(cell) ? getContents(cell) : null;
                if (contents != null)
                    for (obj_id o : contents)
                        if (isPlayer(o))
                            out.add(o);
            }
        obj_id[] result = new obj_id[out.size()];
        out.toArray(result);
        return result;
    }

    public static boolean isAboard(obj_id ship, obj_id player) throws InterruptedException
    {
        return isIdValid(player) && exists(player) && space_transition.getContainingShip(player) == ship;
    }

    public static obj_id[] getPassengerIds(obj_id ship) throws InterruptedException
    {
        return hasObjVar(ship, OV_PAX_IDS) ? getObjIdArrayObjVar(ship, OV_PAX_IDS) : new obj_id[0];
    }

    public static int getPassengerIndex(obj_id ship, obj_id player) throws InterruptedException
    {
        obj_id[] ids = getPassengerIds(ship);
        for (int i = 0; i < ids.length; ++i)
            if (ids[i] == player)
                return i;
        return -1;
    }

    public static boolean isPassenger(obj_id ship, obj_id player) throws InterruptedException
    {
        return getPassengerIndex(ship, player) >= 0;
    }

    public static String[] getPassengerDestination(obj_id ship, obj_id player) throws InterruptedException
    {
        int i = getPassengerIndex(ship, player);
        if (i < 0)
            return null;
        return new String[]{ getStringArrayObjVar(ship, OV_PAX_PLANETS)[i], getStringArrayObjVar(ship, OV_PAX_POINTS)[i] };
    }

    /** Book a passenger (ticket checked by player_travel.boardShuttle): the cabin admits them from now on. */
    public static void addPassenger(obj_id ship, obj_id player, String planet, String point, obj_id ticket) throws InterruptedException
    {
        removePassenger(ship, player);
        obj_id[] ids = getPassengerIds(ship);
        String[] planets = hasObjVar(ship, OV_PAX_PLANETS) ? getStringArrayObjVar(ship, OV_PAX_PLANETS) : new String[0];
        String[] points = hasObjVar(ship, OV_PAX_POINTS) ? getStringArrayObjVar(ship, OV_PAX_POINTS) : new String[0];
        obj_id[] tickets = hasObjVar(ship, OV_PAX_TICKETS) ? getObjIdArrayObjVar(ship, OV_PAX_TICKETS) : new obj_id[0];
        int n = ids.length;
        obj_id[] ids2 = new obj_id[n + 1];
        String[] planets2 = new String[n + 1];
        String[] points2 = new String[n + 1];
        obj_id[] tickets2 = new obj_id[n + 1];
        for (int i = 0; i < n; ++i)
        {
            ids2[i] = ids[i];
            planets2[i] = planets[i];
            points2[i] = points[i];
            tickets2[i] = tickets[i];
        }
        ids2[n] = player;
        planets2[n] = planet;
        points2[n] = point;
        tickets2[n] = isIdValid(ticket) ? ticket : obj_id.NULL_ID;
        setObjVar(ship, OV_PAX_IDS, ids2);
        setObjVar(ship, OV_PAX_PLANETS, planets2);
        setObjVar(ship, OV_PAX_POINTS, points2);
        setObjVar(ship, OV_PAX_TICKETS, tickets2);
        syncCabinAccess(ship);
    }

    public static void removePassenger(obj_id ship, obj_id player) throws InterruptedException
    {
        int idx = getPassengerIndex(ship, player);
        if (idx < 0)
            return;
        obj_id[] ids = getPassengerIds(ship);
        String[] planets = getStringArrayObjVar(ship, OV_PAX_PLANETS);
        String[] points = getStringArrayObjVar(ship, OV_PAX_POINTS);
        obj_id[] tickets = getObjIdArrayObjVar(ship, OV_PAX_TICKETS);
        int n = ids.length - 1;
        if (n <= 0)
        {
            clearPassengers(ship);
            return;
        }
        obj_id[] ids2 = new obj_id[n];
        String[] planets2 = new String[n];
        String[] points2 = new String[n];
        obj_id[] tickets2 = new obj_id[n];
        for (int i = 0, j = 0; i < ids.length; ++i)
        {
            if (i == idx)
                continue;
            ids2[j] = ids[i];
            planets2[j] = planets[i];
            points2[j] = points[i];
            tickets2[j] = tickets[i];
            ++j;
        }
        setObjVar(ship, OV_PAX_IDS, ids2);
        setObjVar(ship, OV_PAX_PLANETS, planets2);
        setObjVar(ship, OV_PAX_POINTS, points2);
        setObjVar(ship, OV_PAX_TICKETS, tickets2);
        syncCabinAccess(ship);
    }

    /**
     * Who may be in the cabin: booked passengers, anyone aboard (an arrival walking off keeps access until they're
     * off; the server expels a player who moves in a cell they may not enter), staff (gods) nearby, and while the
     * vessel boards, ticket holders near it (so the cabin door never stalls them while their booking goes through).
     * The ramp cell (private while the ramp is up) lets staff in too. Every other name on the lists goes, and players
     * nearby get the change.
     */
    public static void syncCabinAccess(obj_id ship) throws InterruptedException
    {
        obj_id cabin = getCabinCell(ship);
        obj_id ramp = getRampCell(ship);
        obj_id[] near = getObjectsInRange(getLocation(ship), NOTIFY_RANGE);
        java.util.HashMap staff = new java.util.HashMap();                 // lower-case name -> player
        if (near != null)
            for (obj_id p : near)
                if (isPlayer(p) && isGod(p))
                    staff.put(getFirstName(p).toLowerCase(), p);
        java.util.HashMap want = new java.util.HashMap(staff);
        for (obj_id p : getPassengerIds(ship))
            if (isIdValid(p) && exists(p))
                want.put(getFirstName(p).toLowerCase(), p);
        for (obj_id p : getPlayersAboard(ship))
            want.put(getFirstName(p).toLowerCase(), p);
        obj_id service = hasObjVar(ship, OV_SERVICE) ? getObjIdObjVar(ship, OV_SERVICE) : null;
        if (getState(ship) == STATE_BOARDING && isIdValid(service) && hasObjVar(service, "vessel.service.point") && near != null)
        {
            String point = getStringObjVar(service, "vessel.service.point");
            for (obj_id p : near)
                if (isPlayer(p) && getDistance(p, ship) <= BOARDING_RANGE && holdsTicketFrom(p, getCurrentSceneName(), point))
                    want.put(getFirstName(p).toLowerCase(), p);
        }
        syncAllowed(cabin, want, near);
        // a floor hatch (Portal::isPassable: cells named hatch*) admits no one while it is shut, staff included,
        // or they would drop through the closed hatch onto the stowed ramp
        syncAllowed(ramp, isHatch(ship) && !isRampOpen(ship) ? new java.util.HashMap() : staff, near);
    }

    /** The ramp cell is a floor hatch under a deck people also walk over (the Lambda's corridor). */
    public static boolean isHatch(obj_id ship) throws InterruptedException
    {
        Config c = getConfig(ship);
        return c != null && c.rampCell != null && c.rampCell.startsWith("hatch");
    }

    /** Make a cell's allowed list exactly `want` (lower-case name -> player); tell nearby players when it changed. */
    private static void syncAllowed(obj_id cell, java.util.HashMap want, obj_id[] near) throws InterruptedException
    {
        if (!isIdValid(cell))
            return;
        java.util.HashSet have = new java.util.HashSet();
        boolean changed = false;
        String[] allowed = permissionsGetAllowed(cell);
        if (allowed != null)
            for (String name : allowed)
            {
                if (name == null)
                    continue;
                if (want.containsKey(name.toLowerCase()))
                    have.add(name.toLowerCase());
                else
                {
                    permissionsRemoveAllowed(cell, name);
                    changed = true;
                }
            }
        for (Object key : want.keySet())
            if (!have.contains(key))
            {
                permissionsAddAllowed(cell, getFirstName((obj_id)want.get(key)));
                changed = true;
            }
        if (changed && near != null)
            for (obj_id p : near)
                if (isPlayer(p))
                    sendDirtyCellPermissionsUpdate(cell, p, permissionsIsPublic(cell) || want.containsKey(getFirstName(p).toLowerCase()));
    }

    /** The nearest other port on this planet with a Titan vessel service (staff riding along without a booking go there). */
    public static String nearestOtherServicePoint(obj_id ship, String exceptPoint) throws InterruptedException
    {
        String planet = getCurrentSceneName();
        String[] points = getPlanetTravelPoints(planet);
        String best = null;
        float bestDistance = Float.MAX_VALUE;
        location here = getLocation(ship);
        for (int i = 0; points != null && i < points.length; ++i)
        {
            if (points[i].equals(exceptPoint) || !isIdValid(findService(planet, points[i])))
                continue;
            location at = getPlanetTravelPointLocation(planet, points[i]);
            float d = at == null ? Float.MAX_VALUE : (float)Math.hypot(at.x - here.x, at.z - here.z);
            if (d < bestDistance)
            {
                bestDistance = d;
                best = points[i];
            }
        }
        return best;
    }

    /** A ticket in the player's inventory valid from (planet, point). */
    public static boolean holdsTicketFrom(obj_id player, String planet, String point) throws InterruptedException
    {
        obj_id inventory = utils.getInventoryContainer(player);
        obj_id[] contents = isIdValid(inventory) ? utils.getContents(inventory, false) : null;
        if (point == null || contents == null)
            return false;
        for (obj_id o : contents)
            if (travel.isTravelTicketValid(o, planet, point))
                return true;
        return false;
    }

    /** Keep an empty, out-of-sight service vessel for `service`'s next flight. */
    public static void addSpare(obj_id service, obj_id ship) throws InterruptedException
    {
        Vector spares = new Vector();
        if (hasObjVar(service, OV_SPARES))
            for (obj_id s : getObjIdArrayObjVar(service, OV_SPARES))
                if (isIdValid(s) && s != ship)
                    spares.add(s);
        spares.add(ship);
        setObjVar(service, OV_SPARES, (obj_id[])spares.toArray(new obj_id[0]));
        setState(ship, STATE_STOWED);
    }

    /**
     * One of `service`'s stowed vessels of `template`, moved to (x, y, z) and held still there facing `heading`; null when
     * it has none. Spares that no longer exist are dropped from the list.
     */
    public static obj_id takeSpare(obj_id service, String template, float x, float y, float z, float heading, float pitch) throws InterruptedException
    {
        return takeSpare(service, template, null, x, y, z, heading, pitch);
    }

    /**
     * takeSpare in `cell` (a hangar cell of another pob; null = the world), (x, y, z) in that cell's space. Only a spare
     * already in that same cell (or, for null, in the world) is reused: a vessel never moves between the world and a
     * cell (clients don't re-parent a ship into a cell), so a hangar's spare is the one stowed in place there.
     */
    public static obj_id takeSpare(obj_id service, String template, obj_id cell, float x, float y, float z, float heading, float pitch) throws InterruptedException
    {
        if (!isIdValid(service) || !hasObjVar(service, OV_SPARES))
            return null;
        Vector keep = new Vector();
        obj_id found = null;
        for (obj_id s : getObjIdArrayObjVar(service, OV_SPARES))
        {
            if (!isIdValid(s) || !exists(s) || !isVessel(s))
                continue;
            if (found == null && getState(s) == STATE_STOWED && template.equals(getTemplateName(s)) && sameCell(getParkedCell(s), cell))
                found = s;
            else
                keep.add(s);
        }
        if (keep.isEmpty())
            removeObjVar(service, OV_SPARES);
        else
            setObjVar(service, OV_SPARES, (obj_id[])keep.toArray(new obj_id[0]));
        if (found == null)
            return null;
        transform t = getTransform_o2p(found);
        t.setPosition_p(x, y, z);
        setTransform_o2p(found, t);
        fly(found, new Path().key(0.0f, x, y, z, heading, pitch, 0.0f), null);
        // a hangar spare stays parked on its legs where it was stowed; a world spare comes back airborne
        initParts(found, isIdValid(cell));
        setState(found, STATE_PARKED);
        return found;
    }

    public static void clearPassengers(obj_id ship) throws InterruptedException
    {
        removeObjVar(ship, OV_PAX_IDS);
        removeObjVar(ship, OV_PAX_PLANETS);
        removeObjVar(ship, OV_PAX_POINTS);
        removeObjVar(ship, OV_PAX_TICKETS);
        obj_id cabin = getCabinCell(ship);
        if (isIdValid(cabin))
            permissionsMakePrivate(cabin);
        syncCabinAccess(ship);
    }

    /**
     * player_travel's boardShuttle / ticket list on a shuttle service: book the seat on the vessel boarding at its pad
     * instead of the stock warp. The ticket stays in the inventory until the doors close (shuttle_service.serviceDepart
     * takes it). A player booked from the ground still walks up the ramp; the cabin admits them from now on.
     */
    public static boolean bookPassenger(obj_id service, obj_id player, obj_id ticket, String planet, String point) throws InterruptedException
    {
        obj_id ship = hasObjVar(service, "vessel.service.current") ? getObjIdObjVar(service, "vessel.service.current") : null;
        if (!isVessel(ship) || getState(ship) != STATE_BOARDING)
        {
            sendSystemMessage(player, new string_id("travel", "shuttle_not_available"));
            return false;
        }
        String flight = flightNumber(ship);
        boolean rebooked = isPassenger(ship, player);
        addPassenger(ship, player, planet, point, ticket);
        String where = point + ", " + planetDisplayName(planet);
        if (isAboard(ship, player))
            sendSystemMessage(player, string_id.unlocalized("Welcome aboard flight " + flight + " to " + where + ". Please take a seat in the cabin."));
        else
            sendSystemMessage(player, string_id.unlocalized((rebooked ? "Your seat on flight " : "You have a seat on flight ") + flight
                    + " to " + where + ". Please board by the ramp."));
        LOG("vessel", "booked " + player + " on " + ship + " (flight " + flight + ") to " + planet + "/" + point + " with ticket " + ticket);
        return true;
    }

    /** Re-send the ramp / cabin access of the vessel to everyone near it (cell access is per player). */
    public static void refreshCellAccess(obj_id ship) throws InterruptedException
    {
        obj_id ramp = getRampCell(ship);
        obj_id cabin = getCabinCell(ship);
        boolean open = isRampOpen(ship);
        boolean hatch = isHatch(ship);
        obj_id[] near = getObjectsInRange(getLocation(ship), NOTIFY_RANGE);
        if (near == null)
            return;
        java.util.HashSet admitted = new java.util.HashSet();
        String[] allowed = isIdValid(cabin) ? permissionsGetAllowed(cabin) : null;
        if (allowed != null)
            for (String name : allowed)
                if (name != null)
                    admitted.add(name.toLowerCase());
        for (obj_id p : near)
        {
            if (!isPlayer(p))
                continue;
            if (isIdValid(ramp))
                sendDirtyCellPermissionsUpdate(ramp, p, open || (isGod(p) && !hatch));
            if (isIdValid(cabin))
                sendDirtyCellPermissionsUpdate(cabin, p, admitted.contains(getFirstName(p).toLowerCase()));
        }
    }

    /** Cell-space point -> location in that cell. */
    public static location cellLocation(obj_id ship, String cellName, float x, float y, float z) throws InterruptedException
    {
        obj_id cell = getCellId(ship, cellName);
        location loc = new location(x, y, z, getLocation(ship).area, cell);
        return loc;
    }

    /** The boarding spot in the cabin (the interior buildout's locEntrance egg), else the cabin's first point. */
    public static void moveIntoCabin(obj_id ship, obj_id player) throws InterruptedException
    {
        location dest = space_transition.getShipBoardingDestination(ship);
        if (dest == null)
        {
            dest = new location();
            dest.cell = getCabinCell(ship);
        }
        setLocation(player, dest);
    }

    /** Off the ship: on the ground beyond the ramp foot, or under the ship in flight. */
    public static void moveOffShip(obj_id ship, obj_id player) throws InterruptedException
    {
        // down a starport's landing well the terrain under the bay is cut away (its height there is meaningless):
        // the bay floor - the starport's arrival point by the gate lift for its shuttle service, the lounge door
        // for a ship berthed by its traffic control
        location bay = wellSetDownPoint(ship);
        if (bay != null)
        {
            setLocation(player, bay);
            return;
        }
        location here = getLocation(ship);
        float yaw = getVesselYaw(ship);
        double h = Math.toRadians(yaw);
        // to the vessel's left (the transport's ramp side), clear of the hull (a big hull's plan: past its side)
        float[] plan = getHullPlan(getTemplateName(ship));
        double out = plan != null && !isIdValid(here.cell) ? Math.max(12.0, plan[0] + 6.0) : 12.0;
        float ox = (float)(-Math.cos(h) * out), oz = (float)(Math.sin(h) * out);
        location ground = groundPoint(ship, here.x + ox, here.z + oz);
        // in a hangar cell, closer in when 12 m out is off the cell floor (a wall), else either side
        if (isIdValid(ground.cell))
        {
            float[] sides = { 12.0f, 8.0f, 5.0f, -12.0f, -8.0f, -5.0f };
            for (float d : sides)
            {
                location l = groundPoint(ship, here.x + ox * d / 12.0f, here.z + oz * d / 12.0f);
                if (isValidInteriorLocation(l))
                {
                    ground = l;
                    break;
                }
            }
        }
        setLocation(player, ground);
    }

    // ------------------------------------------------------------------
    // parked inside another pob's cell (a hangar: the Theed hangar, a bay of the Titan atmo starport)
    // ------------------------------------------------------------------
    // The engine treats a vessel in a cell like one in the world cell: its exterior portals open into that cell and
    // players walk between the two. Its location is then cell-relative (getLocation(ship).cell is the hangar cell), and
    // so are its poses and flight paths. A vessel never crosses between the world and a cell: create it in the cell
    // (parkInCell) and keep it there - it can't fly out through the hangar door.

    public static final String OV_CELL_FLOOR = "vessel.cell.floor_y";   // floor height under a vessel parked in a cell (cell space)
    public static final float CELL_HOVER_HEIGHT = 4.0f;                 // highest take-off hover inside a hangar cell

    /** The cell of another pob the vessel sits in (a hangar), or null when it is in the world. */
    public static obj_id getParkedCell(obj_id ship) throws InterruptedException
    {
        location here = getLocation(ship);
        return here != null && isIdValid(here.cell) ? here.cell : null;
    }

    public static boolean isInCell(obj_id ship) throws InterruptedException
    {
        return getParkedCell(ship) != null;
    }

    private static boolean sameCell(obj_id a, obj_id b)
    {
        return a == null ? b == null : a.equals(b);
    }

    /** Ground height at (x, z) in the vessel's parent space: the terrain in the world, the floor it was parked on in a cell. */
    public static float groundHeight(obj_id ship, float x, float z) throws InterruptedException
    {
        if (!isInCell(ship))
            return getHeightAtLocation(x, z);
        if (hasObjVar(ship, OV_CELL_FLOOR))
            return getFloatObjVar(ship, OV_CELL_FLOOR);
        Config c = getConfig(ship);
        return getLocation(ship).y - (c != null ? c.landY : 0.0f);
    }

    /** (x, z) in the vessel's parent space on the ground there: a world location, or one in the hangar cell. */
    public static location groundPoint(obj_id ship, float x, float z) throws InterruptedException
    {
        location here = getLocation(ship);
        return new location(x, groundHeight(ship, x, z), z, here.area, getParkedCell(ship));
    }

    /**
     * The pad point under a parked vessel for landedPose, from the object marking it (a service's pad controller):
     * its spot on the terrain in the world; in a cell its own height, since it stands on the cell floor.
     */
    public static location padLocation(obj_id padObject) throws InterruptedException
    {
        location pad = getLocation(padObject);
        if (!isIdValid(pad.cell))
            pad.y = getHeightAtLocation(pad.x, pad.z);
        return pad;
    }

    /** One key holding the vessel still at `pose` ({x, y, z, heading, pitch}, parent space). */
    public static Path holdPath(float[] pose)
    {
        return new Path().key(0.0f, pose[0], pose[1], pose[2], pose[3], pose[4], 0.0f);
    }

    /**
     * A vessel of `template` created parked on its legs in `cell` (a hangar cell of another pob) over the cell-space
     * floor point (x, floorY, z), facing `heading` (degrees, cell space); null on failure. It is created in the cell
     * (never moved into one) and held there by a one-key path.
     */
    public static obj_id parkInCell(String template, obj_id cell, float x, float floorY, float z, float heading) throws InterruptedException
    {
        Config c = getConfig(template);
        if (c == null || !isIdValid(cell) || !exists(cell))
            return null;
        location pad = new location(x, floorY, z, getLocation(cell).area, cell);
        float[] pose = landedPose(c, pad, heading);
        obj_id ship = createObject(template, new location(pose[0], pose[1], pose[2], pad.area, cell));
        if (!isIdValid(ship))
        {
            LOG("vessel", "parkInCell: could not create " + template + " in cell " + cell);
            return null;
        }
        setObjVar(ship, OV_CELL_FLOOR, floorY);
        fly(ship, holdPath(pose), null);
        initParts(ship, true);
        setState(ship, STATE_PARKED);
        return ship;
    }

    /** Set everyone aboard down beside the vessel (moveOffShip): on the ground, or on the hangar floor in a cell. */
    public static void setDownPassengers(obj_id ship) throws InterruptedException
    {
        for (obj_id p : getPlayersAboard(ship))
            moveOffShip(ship, p);
    }

    // ------------------------------------------------------------------
    // poses and flight paths
    // ------------------------------------------------------------------

    public static float getVesselYaw(obj_id ship) throws InterruptedException
    {
        transform t = getTransform_o2p(ship);
        vector k = t.getLocalFrameK_p();
        return (float)Math.toDegrees(Math.atan2(k.x, k.z));
    }

    /**
     * Parked pose for a pad: {x, y, z, heading, pitch}. pad = the ground point under the vessel, heading in degrees; all in
     * the pad's space: the world, or a hangar cell (pad.y its floor height, see padLocation / parkInCell).
     */
    public static float[] landedPose(Config c, location pad, float heading)
    {
        double h = Math.toRadians(heading);
        float x = pad.x + (float)(c.landX * Math.cos(h) + c.landZ * Math.sin(h));
        float z = pad.z + (float)(-c.landX * Math.sin(h) + c.landZ * Math.cos(h));
        return new float[]{ x, pad.y + c.landY, z, heading, c.landPitch };
    }

    /** A flight path being put together: world-space keys (heading / pitch nose up / roll right wing down, degrees). */
    public static class Path
    {
        public Vector t = new Vector(), x = new Vector(), y = new Vector(), z = new Vector(), yaw = new Vector(),
            pitch = new Vector(), roll = new Vector();

        public Path key(float time, float px, float py, float pz, float pyaw, float ppitch, float proll)
        {
            t.add(time); x.add(px); y.add(py); z.add(pz); yaw.add(pyaw); pitch.add(ppitch); roll.add(proll);
            return this;
        }

        /** A key in a frame at (ox, oy, oz) facing `heading`: lx right, ly up, lz forward. */
        public Path local(float time, float ox, float oy, float oz, float heading, float lx, float ly, float lz, float dyaw, float ppitch, float proll)
        {
            double h = Math.toRadians(heading);
            float px = ox + (float)(lx * Math.cos(h) + lz * Math.sin(h));
            float pz = oz + (float)(-lx * Math.sin(h) + lz * Math.cos(h));
            return key(time, px, oy + ly, pz, heading + dyaw, ppitch, proll);
        }

        public int size()
        {
            return t.size();
        }

        public float last(Vector v)
        {
            return (Float)v.get(v.size() - 1);
        }

        public float duration()
        {
            return size() > 0 ? last(t) : 0.0f;
        }

        public float[] arr(Vector v)
        {
            float[] a = new float[v.size()];
            for (int i = 0; i < a.length; ++i)
                a[i] = (Float)v.get(i);
            return a;
        }

        /** Every key shifted in time. */
        public Path shift(float dt)
        {
            for (int i = 0; i < t.size(); ++i)
                t.set(i, (Float)t.get(i) + dt);
            return this;
        }

        public void append(Path other, float dt)
        {
            for (int i = 0; i < other.size(); ++i)
                key((Float)other.t.get(i) + dt, (Float)other.x.get(i), (Float)other.y.get(i), (Float)other.z.get(i),
                    (Float)other.yaw.get(i), (Float)other.pitch.get(i), (Float)other.roll.get(i));
        }
    }

    /** Fly `path` (keys at negative times are lead-in: the ship starts moving); `doneHandler` is messaged at its end. */
    public static boolean fly(obj_id ship, Path path, String doneHandler) throws InterruptedException
    {
        if (path.size() < 1)
            return false;
        boolean ok = shipSetFlightPath(ship, path.arr(path.t), path.arr(path.x), path.arr(path.y), path.arr(path.z), path.arr(path.yaw), path.arr(path.pitch), path.arr(path.roll));
        if (!ok)
            return false;
        dictionary d = new dictionary();
        d.put("t", path.arr(path.t));
        d.put("x", path.arr(path.x));
        d.put("y", path.arr(path.y));
        d.put("z", path.arr(path.z));
        d.put("yaw", path.arr(path.yaw));
        d.put("pitch", path.arr(path.pitch));
        d.put("roll", path.arr(path.roll));
        utils.setScriptVar(ship, SV_PATH, d);
        if (doneHandler != null)
            utils.setScriptVar(ship, SV_PATH_DONE, doneHandler);
        else
            utils.removeScriptVar(ship, SV_PATH_DONE);
        return true;
    }

    /**
     * Where the current path has the ship now and how it moves: {x, y, z, yaw, pitch, roll, vx, vy, vz} (same Hermite
     * the server's PlayerShipController evaluates), or null without a path.
     */
    public static float[] pathState(obj_id ship) throws InterruptedException
    {
        if (!utils.hasScriptVar(ship, SV_PATH))
            return null;
        float elapsed = shipGetFlightPathElapsed(ship);
        if (elapsed < 0.0f)
            return null;
        dictionary d = utils.getDictionaryScriptVar(ship, SV_PATH);
        float[] t = d.getFloatArray("t");
        String[] keys = { "x", "y", "z", "yaw", "pitch", "roll" };
        float[] out = new float[9];
        int n = t.length;
        if (n == 1)
        {
            for (int k = 0; k < 6; ++k)
                out[k] = d.getFloatArray(keys[k])[0];
            return out;
        }
        int seg = 0;
        while (seg + 2 < n && t[seg + 1] <= elapsed)
            ++seg;
        float dt = t[seg + 1] - t[seg];
        float s = dt > 0.0f ? Math.max(0.0f, Math.min(1.0f, (elapsed - t[seg]) / dt)) : 1.0f;
        for (int k = 0; k < 6; ++k)
        {
            float[] v = d.getFloatArray(keys[k]);
            float m0 = tangent(t, v, seg), m1 = tangent(t, v, seg + 1);
            float s2 = s * s, s3 = s2 * s;
            out[k] = v[seg] * (2 * s3 - 3 * s2 + 1) + m0 * dt * (s3 - 2 * s2 + s) + v[seg + 1] * (3 * s2 - 2 * s3) + m1 * dt * (s3 - s2);
            if (k < 3)
                out[6 + k] = dt > 0.0f ? (v[seg] * (6 * s2 - 6 * s) + m0 * dt * (3 * s2 - 4 * s + 1) + v[seg + 1] * (6 * s - 6 * s2) + m1 * dt * (3 * s2 - 2 * s)) / dt : 0.0f;
        }
        if (elapsed >= t[n - 1])
            out[6] = out[7] = out[8] = 0.0f;
        return out;
    }

    private static float tangent(float[] t, float[] v, int i)
    {
        if (i == 0 || i + 1 >= t.length)
            return 0.0f;
        float dt = t[i + 1] - t[i - 1];
        return dt > 0.0f ? (v[i + 1] - v[i - 1]) / dt : 0.0f;
    }

    /** A path that starts where the ship is, moving as it moves (a lead-in key 2 s back along its velocity). */
    /** Lift-off from where the vessel sits to a hover `height` m up: gentle unstick, then a straight climb. */
    public static Path takeOffPath(obj_id ship, float height) throws InterruptedException
    {
        location here = getLocation(ship);
        // parked in a hangar cell: only a hover under the roof (it can't leave the cell), keys in cell space
        if (isIdValid(here.cell))
            height = Math.min(height, CELL_HOVER_HEIGHT);
        float yaw = getVesselYaw(ship);
        Config c = getConfig(ship);
        float pitch = c != null ? c.landPitch : 0.0f;
        return new Path().key(0.0f, here.x, here.y, here.z, yaw, pitch, 0.0f)
            .key(3.0f, here.x, here.y + 1.5f, here.z, yaw, pitch * 0.5f, 0.0f)
            .key(10.0f, here.x, here.y + height * 0.4f, here.z, yaw, 0.0f, 0.0f)
            .key(18.0f, here.x, here.y + height, here.z, yaw, 0.0f, 0.0f);
    }

    /** From wherever it is to parked on the ground straight below: over the spot first, then down on its legs. */
    public static Path landHerePath(obj_id ship) throws InterruptedException
    {
        Config c = getConfig(ship);
        Path p = continuePath(ship);
        float x = last(p.x), z = last(p.z), yaw = last(p.yaw);
        float[] pose = landedPose(c, groundPoint(ship, x, z), yaw);
        float t0 = p.duration();
        float above = Math.max(pose[1] + 12.0f, Math.min(last(p.y), pose[1] + 40.0f));
        if (isInCell(ship))
            above = Math.max(pose[1], Math.min(last(p.y), pose[1] + CELL_HOVER_HEIGHT));   // under the hangar roof
        p.key(t0 + 6.0f, pose[0], above, pose[2], yaw, 0.0f, 0.0f);
        p.key(t0 + 16.0f, pose[0], pose[1], pose[2], yaw, pose[4], 0.0f);
        return p;
    }

    private static float last(Vector v)
    {
        return (Float)v.get(v.size() - 1);
    }

    public static Path continuePath(obj_id ship) throws InterruptedException
    {
        Path p = new Path();
        float[] st = pathState(ship);
        if (st == null)
        {
            location here = getLocation(ship);
            transform tr = getTransform_o2p(ship);
            vector k = tr.getLocalFrameK_p();
            float pitch = (float)Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, k.y))));
            p.key(0.0f, here.x, here.y, here.z, getVesselYaw(ship), pitch, 0.0f);
            return p;
        }
        p.key(-2.0f, st[0] - 2.0f * st[6], st[1] - 2.0f * st[7], st[2] - 2.0f * st[8], st[3], st[4], st[5]);
        p.key(0.0f, st[0], st[1], st[2], st[3], st[4], st[5]);
        return p;
    }

    /** Height above which a route between two points is clear: highest terrain sampled every ~400 m + clearance. */
    public static float routeAltitude(float x0, float z0, float x1, float z1, float clearance) throws InterruptedException
    {
        float dx = x1 - x0, dz = z1 - z0;
        float len = (float)Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int)(len / 400.0f));
        float top = -10000.0f;
        for (int i = 0; i <= steps; ++i)
        {
            float f = (float)i / steps;
            top = Math.max(top, getHeightAtLocation(x0 + dx * f, z0 + dz * f));
        }
        return top + clearance;
    }

    /**
     * Lift-off and climb-out from a parked pose (pose = landedPose), modelled on the stock transport take-off: a slow
     * vertical lift while the legs retract, then nose up and away along the pad heading.
     * Returns the path; the climb-out (the jump point / turn-out) is at the last key.
     */
    public static Path departurePath(float[] pose, float startTime) throws InterruptedException
    {
        float x = pose[0], y = pose[1], z = pose[2], h = pose[3];
        Path p = new Path();
        p.local(startTime + 0.0f, x, y, z, h, 0, 0, 0, 0, pose[4], 0);
        p.local(startTime + 4.0f, x, y, z, h, 0, 3, 0, 0, 0, 0);
        p.local(startTime + 8.0f, x, y, z, h, 0, 14, 2, 0, 2, 0);
        p.local(startTime + 13.0f, x, y, z, h, 0, 40, 30, 0, 8, 0);
        p.local(startTime + 19.0f, x, y, z, h, 0, 95, 140, 0, 12, 0);
        p.local(startTime + 26.0f, x, y, z, h, 0, 200, 420, 0, 14, 0);
        p.local(startTime + 34.0f, x, y, z, h, 0, 380, 900, 0, 14, 0);
        p.local(startTime + 44.0f, x, y, z, h, 0, 680, 1750, 0, 12, 0);
        p.local(startTime + 56.0f, x, y, z, h, 0, 1100, 3200, 0, 12, 0);
        return p;
    }

    /**
     * Final approach to a parked pose, modelled on the stock transport landing: a long descending glide in along the
     * pad heading, a flare, a hover over the pad and a slow settle onto the legs. Keys are relative to `startTime`;
     * the first key (lead-in, moving ~110 m/s) is 3 s earlier.
     */
    public static Path approachPath(float[] pose, float startTime) throws InterruptedException
    {
        float x = pose[0], y = pose[1], z = pose[2], h = pose[3];
        Path p = new Path();
        p.local(startTime - 3.0f, x, y, z, h, 30, 470, -1890, 0, -8, -6);
        p.local(startTime + 0.0f, x, y, z, h, 25, 420, -1550, 0, -8, -5);
        p.local(startTime + 8.0f, x, y, z, h, 10, 230, -760, 0, -6, 4);
        p.local(startTime + 15.0f, x, y, z, h, -6, 110, -300, 0, -2, -3);
        p.local(startTime + 21.0f, x, y, z, h, 0, 45, -90, 0, 4, 0);
        p.local(startTime + 26.0f, x, y, z, h, 0, 16, -18, 0, 5, 0);
        p.local(startTime + 31.0f, x, y, z, h, 0, 6, -2, 0, 2, 0);
        p.local(startTime + 36.0f, x, y, z, h, 0, 1.2f, 0, 0, pose[4] * 0.5f + 0.25f, 0);
        p.local(startTime + 40.0f, x, y, z, h, 0, 0, 0, 0, pose[4], 0);
        return p;
    }

    // A landing well (titan_atmo_starport): a ship hovers over the shaft, sinks down it to its bay's level and backs into
    // the bay facing the shaft; it leaves the same way, nose first. Used by the shuttle service and starport traffic.
    public static final float WELL_HOVER = 30.0f;          // over the rim: where approaches end / climb-outs start
    public static final float WELL_SINK = 9.0f;            // seconds down (or up) the shaft (~7 m/s for the top level)
    public static final float WELL_BACK = 7.0f;            // seconds between the shaft's axis and the berth
    public static final float WELL_PAUSE = 1.5f;           // settling over the shaft's axis at the bay's level
    public static final float WELL_LIFT = 2.5f;            // seconds onto / off the legs
    public static final float WELL_TIME = WELL_LIFT + WELL_BACK + WELL_PAUSE + WELL_SINK;

    /** From the hover over a well (pose {x, y, z, heading, pitch}, at t0) down the shaft and back into its bay onto pose. */
    public static Path wellInPath(float[] hover, float[] pose, float t0)
    {
        float lift = 1.5f;
        float h = pose[3];
        float back = t0 + WELL_SINK + WELL_PAUSE;
        return new Path()
            .key(t0, hover[0], hover[1], hover[2], h, 0.0f, 0.0f)
            .key(t0 + WELL_SINK * 0.25f, hover[0], hover[1] - 8.0f, hover[2], h, 0.0f, 0.0f)
            .key(t0 + WELL_SINK, hover[0], pose[1] + lift, hover[2], h, 0.0f, 0.0f)
            .key(back, hover[0], pose[1] + lift, hover[2], h, 0.0f, 0.0f)
            .key(back + WELL_BACK * 0.85f, hover[0] + (pose[0] - hover[0]) * 0.95f, pose[1] + lift,
                 hover[2] + (pose[2] - hover[2]) * 0.95f, h, 0.0f, 0.0f)
            .key(back + WELL_BACK, pose[0], pose[1] + lift, pose[2], h, 0.0f, 0.0f)
            .key(t0 + WELL_TIME, pose[0], pose[1], pose[2], h, pose[4], 0.0f);
    }

    /** The reverse: off the legs, forward out of the bay to the shaft's axis and up it to the hover (t0 = lift-off). */
    public static Path wellOutPath(float[] pose, float[] hover, float t0)
    {
        float lift = 1.5f;
        float h = pose[3];
        float axis = t0 + WELL_LIFT + WELL_BACK;
        return new Path()
            .key(t0, pose[0], pose[1], pose[2], h, pose[4], 0.0f)
            .key(t0 + WELL_LIFT, pose[0], pose[1] + lift, pose[2], h, 0.0f, 0.0f)
            .key(axis, hover[0], pose[1] + lift, hover[2], h, 0.0f, 0.0f)
            .key(axis + WELL_PAUSE, hover[0], pose[1] + lift + 1.0f, hover[2], h, 0.0f, 0.0f)
            .key(t0 + WELL_TIME - WELL_SINK * 0.25f, hover[0], hover[1] - 8.0f, hover[2], h, 0.0f, 0.0f)
            .key(t0 + WELL_TIME, hover[0], hover[1], hover[2], h, 0.0f, 0.0f);
    }

    /** Where people leave a vessel parked in a landing well's bay (see moveOffShip), or null when it isn't. */
    public static location wellSetDownPoint(obj_id ship) throws InterruptedException
    {
        location berth = script.space.atmo.starport_traffic.disembarkPoint(ship);
        if (berth != null)
            return berth;
        obj_id service = hasObjVar(ship, OV_SERVICE) ? getObjIdObjVar(ship, OV_SERVICE) : null;
        if (!isIdValid(service) || !exists(service) || !hasObjVar(service, "vessel.service.well")
                || !hasObjVar(service, "vessel.service.point"))
            return null;
        location arrival = getPlanetTravelPointLocation(getCurrentSceneName(), getStringObjVar(service, "vessel.service.point"));
        if (arrival == null)
            return null;
        location here = getLocation(ship);
        return new location(arrival.x, arrival.y, arrival.z, here.area, null);
    }

    public static final float APPROACH_DURATION = 40.0f;
    public static final float APPROACH_GEAR_AT = 22.0f;      // legs start down this long into the approach

    /** Where an approach to `pose` begins (its key at t = 0): {x, y, z, heading}. */
    public static float[] approachEntry(float[] pose) throws InterruptedException
    {
        Path p = approachPath(pose, 0.0f);
        return new float[]{ (Float)p.x.get(1), (Float)p.y.get(1), (Float)p.z.get(1), pose[3] };
    }

    /**
     * Cruise from where the ship is to `to` (x, y, z, heading) at `speed` m/s over the terrain: banking turn onto the
     * course, keys about every 8 s at a clear altitude, and the turn onto the arrival heading. Starts from `start`
     * (a path holding the ship's current state, see continuePath).
     */
    public static Path cruisePath(Path start, float[] to, float speed) throws InterruptedException
    {
        Path p = start;
        float t = p.duration();
        float x = p.last(p.x), y = p.last(p.y), z = p.last(p.z), yaw = p.last(p.yaw);
        float dx = to[0] - x, dz = to[2] - z;
        float dist = (float)Math.sqrt(dx * dx + dz * dz);
        float course = (float)Math.toDegrees(Math.atan2(dx, dz));
        float alt = Math.max(Math.max(y, to[1]), routeAltitude(x, z, to[0], to[2], 450.0f));
        float turn = wrap(course - yaw);
        float turnTime = Math.max(6.0f, Math.abs(turn) / 9.0f);           // ~9 deg/s standard rate turn
        float bank = Math.max(-25.0f, Math.min(25.0f, turn * 0.5f));
        // swing onto the course while climbing
        float lead = Math.min(dist * 0.25f, speed * turnTime * 0.6f);
        p.key(t + turnTime * 0.5f, x + (float)Math.sin(Math.toRadians(yaw + turn * 0.25f)) * lead * 0.5f, (y + alt) * 0.5f,
              z + (float)Math.cos(Math.toRadians(yaw + turn * 0.25f)) * lead * 0.5f, yaw + turn * 0.5f, 4.0f, bank);
        float sx = x + (float)Math.sin(Math.toRadians(course)) * lead, sz = z + (float)Math.cos(Math.toRadians(course)) * lead;
        t += turnTime;
        p.key(t, sx, alt, sz, course, 2.0f, 0.0f);
        // cruise legs
        float rx = to[0] - sx, rz = to[2] - sz;
        float rest = (float)Math.sqrt(rx * rx + rz * rz);
        float legLen = speed * 8.0f;
        int legs = Math.max(1, (int)(rest / legLen));
        float endTurn = wrap(to[3] - course);
        for (int i = 1; i <= legs; ++i)
        {
            float f = (float)i / legs;
            float px = sx + rx * f, pz = sz + rz * f;
            float ground = getHeightAtLocation(px, pz);
            float py = (i == legs) ? to[1] : Math.max(alt, ground + 450.0f);
            float pyaw = (i == legs) ? to[3] : course + ((i == legs - 1) ? endTurn * 0.5f : 0.0f);
            float proll = (i == legs - 1) ? Math.max(-20.0f, Math.min(20.0f, endTurn * 0.4f)) : 0.0f;
            t += (rest / legs) / speed;
            p.key(t, px, py, pz, pyaw, (i == legs) ? -6.0f : 0.0f, proll);
        }
        return p;
    }

    public static float wrap(float degrees)
    {
        while (degrees > 180.0f)
            degrees -= 360.0f;
        while (degrees < -180.0f)
            degrees += 360.0f;
        return degrees;
    }

    /** A wide holding orbit round a point at the ship's height, one lap, continuing from the ship's state. */
    public static Path holdingPath(obj_id ship, float cx, float cz, float radius, float speed) throws InterruptedException
    {
        Path p = continuePath(ship);
        float x = p.last(p.x), y = p.last(p.y), z = p.last(p.z);
        float a0 = (float)Math.atan2(x - cx, z - cz);
        float t = 0.0f;
        int n = 12;
        float lapTime = (float)(2.0 * Math.PI * radius / speed);
        for (int i = 1; i <= n; ++i)
        {
            float a = a0 + (float)(2.0 * Math.PI * i / n);
            float px = cx + (float)Math.sin(a) * radius, pz = cz + (float)Math.cos(a) * radius;
            float heading = (float)Math.toDegrees(a) + 90.0f;
            t += lapTime / n;
            p.key(t, px, y, pz, heading, 0.0f, 12.0f);
        }
        return p;
    }

    // ------------------------------------------------------------------
    // hyperspace
    // ------------------------------------------------------------------

    /**
     * Seconds in hyperspace between two planets: galactic grid distance (datatables/space/titan_hyperspace.tab,
     * the planets' grid squares on the standard galaxy map) x seconds per grid square, plus the jump itself.
     */
    public static int hyperspaceSeconds(String fromPlanet, String toPlanet) throws InterruptedException
    {
        float[] a = galacticPosition(fromPlanet), b = galacticPosition(toPlanet);
        float base = 45.0f, perSquare = 30.0f;
        if (a == null || b == null)
            return (int)(base + perSquare * 4.0f);
        float dx = a[0] - b[0], dy = a[1] - b[1];
        return (int)(base + perSquare * (float)Math.sqrt(dx * dx + dy * dy));
    }

    public static float[] galacticPosition(String planet) throws InterruptedException
    {
        if (planet == null)
            return null;
        String p = planet.startsWith("kashyyyk") ? "kashyyyk" : planet;
        int row = dataTableSearchColumnForString(p, "planet", HYPERSPACE_TABLE);
        if (row < 0)
            return null;
        return new float[]{ dataTableGetFloat(HYPERSPACE_TABLE, row, "gridX"), dataTableGetFloat(HYPERSPACE_TABLE, row, "gridY") };
    }

    public static String hyperlaneName(String fromPlanet, String toPlanet) throws InterruptedException
    {
        int row = dataTableSearchColumnForString(toPlanet, "planet", HYPERSPACE_TABLE);
        return row >= 0 ? dataTableGetString(HYPERSPACE_TABLE, row, "route") : "the hyperlanes";
    }

    public static String planetDisplayName(String planet)
    {
        if (planet == null || planet.length() == 0)
            return "";
        String p = planet.startsWith("kashyyyk") ? "kashyyyk" : planet;
        return p.substring(0, 1).toUpperCase() + p.substring(1);
    }

    // ------------------------------------------------------------------
    // shuttle services (pad controllers at starports)
    // ------------------------------------------------------------------

    /** The vessel template a starport structure's shuttle service flies, or null (stock creature shuttle). */
    public static String getServiceVessel(String structureTemplate) throws InterruptedException
    {
        if (structureTemplate == null || !dataTableOpen(SERVICE_TABLE))
            return null;
        int row = dataTableSearchColumnForString(structureTemplate, "structure", SERVICE_TABLE);
        if (row < 0 || dataTableGetInt(SERVICE_TABLE, row, "enabled") == 0)
            return null;
        return dataTableGetString(SERVICE_TABLE, row, "vessel");
    }

    public static boolean isService(obj_id obj) throws InterruptedException
    {
        return isIdValid(obj) && exists(obj) && hasScript(obj, SCRIPT_SERVICE);
    }

    /** The shuttle service of a travel point on this game server's planet, or null. */
    public static obj_id findService(String planet, String point) throws InterruptedException
    {
        if (planet == null || point == null || !planet.equals(getCurrentSceneName()))
            return null;
        location arrival = getPlanetTravelPointLocation(planet, point);
        if (arrival == null)
            return null;
        obj_id[] near = getObjectsInRange(arrival, 220.0f);
        if (near != null)
            for (obj_id o : near)
                if (isService(o) && point.equals(getStringObjVar(o, "vessel.service.point")))
                    return o;
        return null;
    }

    /** Messages (unlocalized text) to everyone aboard. */
    public static void announce(obj_id ship, String text) throws InterruptedException
    {
        obj_id[] aboard = getPlayersAboard(ship);
        for (obj_id p : aboard)
            sendSystemMessage(p, string_id.unlocalized(text));
    }

    public static String flightNumber(obj_id ship) throws InterruptedException
    {
        if (!hasObjVar(ship, OV_FLIGHT))
            setObjVar(ship, OV_FLIGHT, "TT-" + (100 + rand(0, 899)));
        return getStringObjVar(ship, OV_FLIGHT);
    }
}
