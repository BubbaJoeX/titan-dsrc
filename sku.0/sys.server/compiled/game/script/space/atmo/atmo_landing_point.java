package script.space.atmo;

import script.*;
import script.library.*;

/**
 * Script for atmospheric landing point spawn eggs.
 * Attach this script to spawn eggs with atmo.landing_point.* objvars.
 *
 * Required objvars:
 *   atmo.landing_point.loc - fly to location (includes height)
 *   atmo.landing_point.name - display name on planet map
 *
 * Optional objvars:
 *   atmo.landing_point.disembark_loc - location to disembark when landed
 *   atmo.landing_point.yaw - yaw angle for ship to land at
 *   atmo.landing_point.time_to_disembark - time allowed docked (-1 = forever)
 *   atmo.landing_point.loc_offset - optional offset for small platforms
 */
public class atmo_landing_point extends script.base_script
{
    public static final String SCRIPT_NAME = "space.atmo.atmo_landing_point";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        registerLandingPoint(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "delayedRegister", null, 2, false);
        return SCRIPT_CONTINUE;
    }

    public int delayedRegister(obj_id self, dictionary params) throws InterruptedException
    {
        registerLandingPoint(self);
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        atmo_landing_registry.unregisterFromMap(self);
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self) throws InterruptedException
    {
        atmo_landing_registry.unregisterFromMap(self);
        return SCRIPT_CONTINUE;
    }

    private void registerLandingPoint(obj_id self) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(self))
        {
            return;
        }

        atmo_landing_registry.registerOnMap(self);
    }

    public int handleLandingRequest(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        obj_id pilot = params.getObjId("pilot");

        if (!isIdValid(ship) || !isIdValid(pilot))
            return SCRIPT_CONTINUE;

        if (!atmo_landing_registry.isLandingPoint(self))
        {
            sendSystemMessageTestingOnly(pilot, "This landing point is not properly configured.");
            return SCRIPT_CONTINUE;
        }

        if (atmo_landing_registry.isOccupied(self))
        {
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Landing Control]: This landing pad is currently occupied.");
            return SCRIPT_CONTINUE;
        }

        if (atmo_landing_registry.isEnRoute(self))
        {
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44[Landing Control]: Another ship is already en route to this landing pad.");
            return SCRIPT_CONTINUE;
        }

        int eta = atmo_landing_registry.calculateETA(ship, self);
        if (!atmo_landing_registry.reserveLandingPoint(self, ship, eta))
        {
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Landing Control]: Unable to reserve landing pad. Please try again.");
            return SCRIPT_CONTINUE;
        }

        location landingLoc = atmo_landing_registry.getLandingLocation(self);
        float cruiseAlt = atmo_landing_registry.getCruiseAltitude(self);
        float landingAlt = atmo_landing_registry.getApproachAltitude(self);
        float yaw = atmo_landing_registry.getLandingYaw(self);
        String name = atmo_landing_registry.getLandingPointName(self);

        setObjVar(ship, "atmo.landing.target", self);
        setObjVar(ship, "atmo.landing.yaw", yaw);
        setObjVar(ship, "atmo.landing.name", name);

        dictionary flyParams = new dictionary();
        flyParams.put("x", landingLoc.x);
        flyParams.put("z", landingLoc.z);
        flyParams.put("takeoffAlt", cruiseAlt);
        flyParams.put("landingAlt", landingAlt);
        flyParams.put("owner", pilot);
        flyParams.put("landingPointTarget", self);
        messageTo(ship, "shipAutoPilotEngage", flyParams, 0, false);

        play2dNonLoopingSound(pilot, "sound/sys_comm_generic.snd");
        sendSystemMessageTestingOnly(pilot, "\\#00ccff[Landing Control]: Landing clearance granted for " + name + ".");
        sendSystemMessageTestingOnly(pilot, "\\#aaddff  Auto-pilot engaged. ETA: " + eta + " seconds.");

        return SCRIPT_CONTINUE;
    }

    public int handleShipArrived(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");

        if (!isIdValid(ship) || !exists(ship))
            return SCRIPT_CONTINUE;

        atmo_landing_registry.occupyLandingPoint(self, ship);

        float yaw = atmo_landing_registry.getLandingYaw(self);
        applyShipYaw(ship, yaw);

        int timeToDisembark = atmo_landing_registry.getTimeToDisembark(self);
        if (timeToDisembark > 0)
        {
            setObjVar(ship, "atmo.landing.dockExpiry", getGameTime() + timeToDisembark);
            messageTo(ship, "checkDockingTimer", null, timeToDisembark, false);

            notifyShipOccupants(ship, "\\#88ddaa[Docking Control]: You have " + timeToDisembark + " seconds of docking time.");
            notifyShipOccupants(ship, "\\#88ddaa  Use the radial menu to extend docking time if needed.");
        }

        return SCRIPT_CONTINUE;
    }

    public int handleShipDeparted(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");

        atmo_landing_registry.clearOccupancy(self);

        if (isIdValid(ship) && exists(ship))
        {
            removeObjVar(ship, "atmo.landing");
        }

        return SCRIPT_CONTINUE;
    }

    private void applyShipYaw(obj_id ship, float yaw) throws InterruptedException
    {
        if (!isIdValid(ship) || !exists(ship))
            return;

        setYaw(ship, yaw);
    }

    private void notifyShipOccupants(obj_id ship, String message) throws InterruptedException
    {
        java.util.Vector players = space_transition.getContainedPlayers(ship, null);
        if (players != null)
        {
            for (Object p : players)
            {
                obj_id player = (obj_id) p;
                if (isIdValid(player))
                    sendSystemMessageTestingOnly(player, message);
            }
        }
    }
}

