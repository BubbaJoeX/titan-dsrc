package script.space.vessel;

import script.*;
import script.library.*;

/**
 * A Titan vessel deed (object/tangible/titan/vessel_deed/*): the owner's claim on one private boardable vessel
 * (Lambda shuttle, Corellian corvette, Nebulon-B ...). From the owner's inventory:
 *   Call      the vessel flies in on an approach and sets down broadside to the owner, clear of them by the deed's
 *             clearance (its half-width and some); the first call creates it and makes the owner its owner. A vessel
 *             already parked elsewhere (empty) is brought round the same way.
 *   Send Away the vessel (empty, landed) lifts off and is stowed out of sight. Vessels are never destroyed: destroying
 *             POB ships in ground scenes corrupts the game server's heap (see script.library.vessel).
 * Objvars: vessel_deed.template (the ship's server template), vessel_deed.clearance (metres), vessel_deed.ship.
 */
public class vessel_deed extends script.base_script
{
    public vessel_deed()
    {
    }

    public static final String OV_TEMPLATE = "vessel_deed.template";
    public static final String OV_CLEARANCE = "vessel_deed.clearance";
    public static final String OV_SHIP = "vessel_deed.ship";
    public static final String OV_SHIP_DEED = "vessel.deed";              // on the vessel: the deed it answers to

    private obj_id getShip(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OV_SHIP))
            return null;
        obj_id ship = getObjIdObjVar(self, OV_SHIP);
        return isIdValid(ship) && exists(ship) ? ship : null;
    }

    private boolean holds(obj_id self, obj_id player) throws InterruptedException
    {
        return isIdValid(player) && utils.isNestedWithin(self, player);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!holds(self, player))
            return SCRIPT_CONTINUE;
        obj_id ship = getShip(self);
        mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized(ship == null ? "Call Vessel" : "Call Vessel Here"));
        if (ship != null && vessel.getState(ship) != vessel.STATE_STOWED)
            mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Send Vessel Away"));
        // the hull's own radial is out of reach on the big hulls (its origin stands high over the ground)
        if (ship != null && vessel.isLanded(ship) && isNear(player, ship))
            mi.addRootMenu(menu_info_types.SERVER_MENU3, string_id.unlocalized(vessel.isRampOpen(ship) ? "Raise Vessel Ramp" : "Lower Vessel Ramp"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!holds(self, player))
            return SCRIPT_CONTINUE;
        if (item == menu_info_types.SERVER_MENU1)
            callVessel(self, player);
        else if (item == menu_info_types.SERVER_MENU2)
            sendAway(self, player);
        else if (item == menu_info_types.SERVER_MENU3)
        {
            obj_id ship = getShip(self);
            if (ship != null && vessel.isLanded(ship) && isNear(player, ship))
            {
                dictionary d = new dictionary();
                d.put("open", vessel.isRampOpen(ship) ? 0 : 1);
                messageTo(ship, "vesselRamp", d, 0.0f, false);
            }
        }
        return SCRIPT_CONTINUE;
    }

    private static final float RAMP_REACH = 400.0f;                 // metres from the hull frame (the Nebulon is 472 m long)

    private boolean isNear(obj_id player, obj_id ship) throws InterruptedException
    {
        location p = getLocation(getTopMostContainer(player)), s = getLocation(ship);
        return p != null && s != null && p.area.equals(s.area) && getDistance(p, s) < RAMP_REACH;
    }

    private void callVessel(obj_id self, obj_id player) throws InterruptedException
    {
        if (isIdValid(getLocation(player).cell))
        {
            sendSystemMessage(player, string_id.unlocalized("You must be outdoors to call your vessel."));
            return;
        }
        String template = getStringObjVar(self, OV_TEMPLATE);
        vessel.Config c = vessel.getConfig(template);
        if (c == null)
        {
            sendSystemMessage(player, string_id.unlocalized("This deed names no known vessel."));
            return;
        }
        obj_id ship = getShip(self);
        if (ship != null)
        {
            vessel.nameVessel(ship, c);
            int state = vessel.getState(ship);
            if (state != vessel.STATE_PARKED && state != vessel.STATE_BOARDING && state != vessel.STATE_STOWED)
            {
                sendSystemMessage(player, string_id.unlocalized(getName(ship) + " is busy; call it when it has landed."));
                return;
            }
            if (vessel.getPlayersAboard(ship).length > 0)
            {
                sendSystemMessage(player, string_id.unlocalized(getName(ship) + " has people aboard; it stays where it is."));
                return;
            }
        }
        // the pad: broadside to the caller, clear of them; the vessel faces the caller's right. Ahead of the caller
        // first, then to their left, right and behind: the first flat spot with no structure or vessel on it.
        location here = getLocation(player);
        float clearance = hasObjVar(self, OV_CLEARANCE) ? getFloatObjVar(self, OV_CLEARANCE) : 40.0f;
        float yaw = pickPadYaw(self, here, getYaw(player), clearance, ship);
        if (Float.isNaN(yaw))
        {
            sendSystemMessage(player, string_id.unlocalized("There is no clear, level ground around you for your vessel to land. Move to open ground."));
            return;
        }
        double h = Math.toRadians(yaw);
        float px = here.x + (float)(Math.sin(h) * clearance);
        float pz = here.z + (float)(Math.cos(h) * clearance);
        location pad = new location(px, getHeightAtLocation(px, pz), pz, here.area, null);
        float heading = vessel.wrap(yaw + 90.0f);
        float[] pose = vessel.landedPose(c, pad, heading);
        float[] entry = vessel.approachEntry(pose);
        location start = new location(entry[0], entry[1], entry[2], here.area, null);
        if (ship == null)
        {
            ship = createObject(template, start);
            if (!isIdValid(ship))
            {
                sendSystemMessage(player, string_id.unlocalized("Your vessel could not be called."));
                return;
            }
            setOwner(ship, player);
            vessel.nameVessel(ship, c);
            setObjVar(ship, OV_SHIP_DEED, self);
            setObjVar(self, OV_SHIP, ship);
        }
        else
        {
            if (vessel.isRampOpen(ship))
                vessel.setRamp(ship, false, 0.0f);
            setLocation(ship, start);
        }
        stampHullPlan(self, ship, clearance);
        vessel.setState(ship, vessel.STATE_ARRIVING);
        vessel.setGear(ship, false, 0.0f);
        vessel.setGear(ship, true, vessel.APPROACH_GEAR_AT);
        vessel.fly(ship, vessel.approachPath(pose, 0.0f), "vesselLanded");
        messageTo(ship, "vesselLandingCue", null, vessel.APPROACH_DURATION - vessel_controller.LANDING_CUE, false);
        sendSystemMessage(player, string_id.unlocalized(getName(ship) + " is on its way: about "
                + (int)vessel.APPROACH_DURATION + " seconds out."));
    }

    public static final String OV_HALF_WIDTH = "vessel_deed.half_width";    // hull plan (hull frame, metres), written by
    public static final String OV_LENGTH_AFT = "vessel_deed.length_aft";    // make_titan_vessels from the hull boxes
    public static final String OV_LENGTH_FORE = "vessel_deed.length_fore";
    private static final float PAD_SLOPE = 3.0f;                   // metres of rise over a pad that still count as level,
    private static final float PAD_SLOPE_PER_M = 0.02f;            // plus this much per metre of hull length
    private static final float PAD_MARGIN = 3.0f;                  // clear ground past the hull's sides

    // the hull plan on the vessel too, so other vessels' pad checks see its whole keel, not just its frame
    public static final String OV_SHIP_HULL = "vessel.hull";                // float[]{half width, length aft, length fore}

    private void stampHullPlan(obj_id self, obj_id ship, float clearance) throws InterruptedException
    {
        setObjVar(ship, OV_SHIP_HULL, hullPlan(self, clearance));
    }

    // this deed's vessel's plan {half width, length aft, length fore}: titan_vessels.tab, else the deed's objvars,
    // else a disc of 0.8 x the clearance
    private float[] hullPlan(obj_id self, float clearance) throws InterruptedException
    {
        float[] plan = vessel.getHullPlan(getStringObjVar(self, OV_TEMPLATE));
        if (plan != null)
            return plan;
        float half = hasObjVar(self, OV_HALF_WIDTH) ? getFloatObjVar(self, OV_HALF_WIDTH) : clearance * 0.8f;
        return new float[]{half, hasObjVar(self, OV_LENGTH_AFT) ? getFloatObjVar(self, OV_LENGTH_AFT) : half,
                hasObjVar(self, OV_LENGTH_FORE) ? getFloatObjVar(self, OV_LENGTH_FORE) : half};
    }

    // distance from point (px, pz) to the segment (ax, az)-(bx, bz)
    private static float pointSegment(float px, float pz, float ax, float az, float bx, float bz)
    {
        float dx = bx - ax, dz = bz - az, len2 = dx * dx + dz * dz;
        float t = len2 > 0.0f ? Math.max(0.0f, Math.min(1.0f, ((px - ax) * dx + (pz - az) * dz) / len2)) : 0.0f;
        float ex = px - (ax + dx * t), ez = pz - (az + dz * t);
        return (float)Math.sqrt(ex * ex + ez * ez);
    }

    // closest distance between two keels (segments that don't cross: the pad check never lands one across another)
    private static float segmentSegment(float[] a, float[] b)
    {
        return Math.min(Math.min(pointSegment(a[0], a[1], b[0], b[1], b[2], b[3]), pointSegment(a[2], a[3], b[0], b[1], b[2], b[3])),
                Math.min(pointSegment(b[0], b[1], a[0], a[1], a[2], a[3]), pointSegment(b[2], b[3], a[0], a[1], a[2], a[3])));
    }

    // another vessel's keel (x0, z0, x1, z1) and half width from its titan_vessels.tab hull plan, else the one stamped
    // on it or its deed's (a service shuttle has none: its frame, 15 m)
    private static float[] keelOf(obj_id other) throws InterruptedException
    {
        location at = getLocation(other);
        float[] plan = vessel.getHullPlan(getTemplateName(other));
        if (plan == null && hasObjVar(other, OV_SHIP_HULL))
            plan = getFloatArrayObjVar(other, OV_SHIP_HULL);
        obj_id deed = hasObjVar(other, OV_SHIP_DEED) ? getObjIdObjVar(other, OV_SHIP_DEED) : null;
        if (plan == null && isIdValid(deed) && exists(deed) && hasObjVar(deed, OV_HALF_WIDTH))
            plan = new float[]{getFloatObjVar(deed, OV_HALF_WIDTH), getFloatObjVar(deed, OV_LENGTH_AFT), getFloatObjVar(deed, OV_LENGTH_FORE)};
        if (plan == null || plan.length < 3)
            return new float[]{at.x, at.z, at.x, at.z, 15.0f};
        double h = Math.toRadians(vessel.getVesselYaw(other));
        float ax = (float)Math.sin(h), az = (float)Math.cos(h);
        return new float[]{at.x - ax * plan[1], at.z - az * plan[1], at.x + ax * plan[2], at.z + az * plan[2], plan[0]};
    }

    // The caller's yaw turned toward the first clear landing pad (ahead, left, right, behind), or NaN when none is.
    // The vessel lands broadside at `clearance`, heading the caller's yaw + 90.
    private float pickPadYaw(obj_id self, location here, float yaw, float clearance, obj_id ship) throws InterruptedException
    {
        float[] plan = hullPlan(self, clearance);
        float half = plan[0] + PAD_MARGIN, aft = -plan[1], fore = plan[2];
        float[] turns = {0.0f, -90.0f, 90.0f, 180.0f};
        for (float turn : turns)
        {
            float y = vessel.wrap(yaw + turn);
            double h = Math.toRadians(y);
            location pad = new location(here.x + (float)(Math.sin(h) * clearance), 0.0f, here.z + (float)(Math.cos(h) * clearance), here.area, null);
            if (padIsClear(pad, vessel.wrap(y + 90.0f), half, aft, fore, ship))
                return y;
        }
        return Float.NaN;
    }

    // The hull's plan on the ground (a strip `half` either side of the keel from aft to fore along `heading`): level
    // (samples down the keel and both sides) and no building, installation, static or other vessel on it.
    private boolean padIsClear(location pad, float heading, float half, float aft, float fore, obj_id ship) throws InterruptedException
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
            LOG("vessel", "deed pad " + pad.x + "," + pad.z + " heading " + heading + ": not level (" + (hi - lo) + " m)");
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
            float margin = vessel.isVessel(o) ? 10.0f : isGameObjectTypeOf(got, GOT_building) ? 15.0f
                    : isGameObjectTypeOf(got, GOT_installation) ? 8.0f : got == GOT_static ? 3.0f : -1.0f;
            if (margin < 0.0f)
                continue;
            float gap;
            if (vessel.isVessel(o))
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
                LOG("vessel", "deed pad " + pad.x + "," + pad.z + " heading " + heading + ": " + o + " " + getTemplateName(o) + " in the way");
                return false;
            }
        }
        return true;
    }

    private void sendAway(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id ship = getShip(self);
        if (ship == null)
            return;
        int state = vessel.getState(ship);
        if (state != vessel.STATE_PARKED && state != vessel.STATE_BOARDING)
        {
            sendSystemMessage(player, string_id.unlocalized(getName(ship) + " can only be sent away once it has landed."));
            return;
        }
        if (vessel.getPlayersAboard(ship).length > 0)
        {
            sendSystemMessage(player, string_id.unlocalized(getName(ship) + " has people aboard; ask them to step off first."));
            return;
        }
        messageTo(ship, "vesselSendAway", null, 0.1f, false);
        sendSystemMessage(player, string_id.unlocalized(getName(ship) + ": ramp up, departing."));
    }
}
