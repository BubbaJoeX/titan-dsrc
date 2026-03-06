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
    public static final int OCCUPANCY_CHECK_INTERVAL = 30;
    public static final int MINIMUM_LANDING_FEE = 5000;

    // Imperial Docking Authority appearance templates
    public static final String[] IDA_APPEARANCES = {
        "object/mobile/sd_engineer_01.iff",
        "object/mobile/sd_engineer_02.iff",
        "object/mobile/sd_engineer_03.iff",
        "object/mobile/sd_engineer_04.iff",
        "object/mobile/sd_engineer_05.iff"
    };

    // Flavor text for various scenarios
    public static final String[] IDA_CLEARANCE_GRANTED = {
        "Imperial Docking Authority. Landing clearance granted. Proceed to designated coordinates.",
        "IDA Control. You are cleared for approach. Maintain heading and reduce speed.",
        "Docking Authority here. Payment received, transmitting landing vector now.",
        "This is IDA Tower. Clearance confirmed. Welcome to Imperial-controlled airspace.",
        "Imperial Control. Your credentials check out. You may proceed to landing."
    };

    public static final String[] IDA_INSUFFICIENT_FUNDS = {
        "Imperial Docking Authority. Insufficient funds detected. Landing denied.",
        "IDA Control. Your account balance does not meet docking requirements. Request denied.",
        "Docking Authority. Payment failure. You are not authorized to land at this time.",
        "This is IDA Tower. We cannot process your landing fee. Clearance denied.",
        "Imperial Control. Negative on landing clearance. Secure adequate funds and try again."
    };

    public static final String[] IDA_PAD_OCCUPIED = {
        "Imperial Docking Authority. Requested pad is currently occupied. Find another.",
        "IDA Control. Negative on that vector. Landing zone is not available.",
        "Docking Authority. That platform is in use. Select an alternate landing site.",
        "This is IDA Tower. Pad occupancy detected. Your request cannot be processed.",
        "Imperial Control. Landing denied. Current vessel has not cleared the platform."
    };

    public static final String[] IDA_ALREADY_DOCKED = {
        "Imperial Docking Authority. Your vessel is already docked. Undock before requesting new clearance.",
        "IDA Control. Records show your ship is currently moored. Clear your berth first.",
        "Docking Authority. You cannot request landing while already occupying a pad.",
        "This is IDA Tower. Duplicate docking request denied. Undock from current position.",
        "Imperial Control. System shows active docking status. Request invalid."
    };

    public static final String[] IDA_EN_ROUTE = {
        "Imperial Docking Authority. Another vessel is inbound to that location. Stand by.",
        "IDA Control. Traffic alert. That pad has incoming traffic. Request denied.",
        "Docking Authority. Negative. We have a ship on approach to those coordinates.",
        "This is IDA Tower. Holding pattern required. Platform has priority traffic.",
        "Imperial Control. Landing vector occupied. Select alternate destination."
    };

    public static final String SND_IDA_COMM = "sound/sys_comm_imperial.snd";

    private String getRandomIDAAppearance() throws InterruptedException
    {
        int index = rand(0, IDA_APPEARANCES.length - 1);
        return IDA_APPEARANCES[index];
    }

    private String getRandomMessage(String[] messages) throws InterruptedException
    {
        int index = rand(0, messages.length - 1);
        return messages[index];
    }

    private void commPlayerIDA(obj_id player, String message) throws InterruptedException
    {
        if (!isIdValid(player))
            return;

        play2dNonLoopingSound(player, SND_IDA_COMM);

        prose_package pp = new prose_package();
        pp.stringId = string_id.unlocalized(message);

        commPlayer(getSelf(), player, pp, getRandomIDAAppearance());
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        registerLandingPoint(self);
        startOccupancyHeartbeat(self);
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
        startOccupancyHeartbeat(self);
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

    private void startOccupancyHeartbeat(obj_id self) throws InterruptedException
    {
        messageTo(self, "validateOccupancy", null, OCCUPANCY_CHECK_INTERVAL, false);
    }

    /**
     * Periodic heartbeat to validate that occupying ship still exists.
     * Clears stale occupancy if ship no longer exists.
     */
    public int validateOccupancy(obj_id self, dictionary params) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(self))
            return SCRIPT_CONTINUE;

        // Check if there's an occupying ship
        obj_id occupier = atmo_landing_registry.getOccupyingShip(self);
        if (isIdValid(occupier))
        {
            // Verify ship still exists
            if (!exists(occupier))
            {
                atmo_landing_registry.clearOccupancy(self);
            }
            else
            {
                // Verify ship still has docked state - if not, clear
                if (!hasObjVar(occupier, "atmo.landing.docked"))
                {
                    atmo_landing_registry.clearOccupancy(self);
                }
            }
        }

        // Check ETA reservations
        atmo_landing_registry.validateEnRoute(self);

        // Schedule next check
        messageTo(self, "validateOccupancy", null, OCCUPANCY_CHECK_INTERVAL, false);
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

        // Block if ship is already docked somewhere
        if (hasObjVar(ship, "atmo.landing.docked"))
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_ALREADY_DOCKED));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Your ship is already docked at a landing point.");
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Use the Starship Management Terminal to undock first.");
            return SCRIPT_CONTINUE;
        }

        if (!atmo_landing_registry.isLandingPoint(self))
        {
            sendSystemMessageTestingOnly(pilot, "This landing point is not properly configured.");
            return SCRIPT_CONTINUE;
        }

        if (atmo_landing_registry.isOccupied(self))
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_PAD_OCCUPIED));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: This landing pad is currently occupied.");
            return SCRIPT_CONTINUE;
        }

        if (atmo_landing_registry.isEnRoute(self))
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_EN_ROUTE));
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44[Imperial Docking Authority]: Another ship is already en route to this landing pad.");
            return SCRIPT_CONTINUE;
        }

        // Check and charge landing fee
        int totalFunds = money.getTotalMoney(pilot);
        if (totalFunds < MINIMUM_LANDING_FEE)
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_INSUFFICIENT_FUNDS));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Insufficient funds for landing fee.");
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Landing fee: " + MINIMUM_LANDING_FEE + " credits. You have: " + totalFunds + " credits.");
            return SCRIPT_CONTINUE;
        }

        // Charge the landing fee (to travel system account)
        if (!transferBankCreditsToNamedAccount(pilot, money.ACCT_TRAVEL, MINIMUM_LANDING_FEE, "noHandler", "noHandler", new dictionary()))
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_INSUFFICIENT_FUNDS));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Unable to process landing fee payment.");
            return SCRIPT_CONTINUE;
        }

        int eta = atmo_landing_registry.calculateETA(ship, self);
        if (!atmo_landing_registry.reserveLandingPoint(self, ship, eta))
        {
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Unable to reserve landing pad. Please try again.");
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

        // Comm the pilot with clearance granted
        commPlayerIDA(pilot, getRandomMessage(IDA_CLEARANCE_GRANTED));
        sendSystemMessageTestingOnly(pilot, "\\#00ccff[Imperial Docking Authority]: Landing clearance granted for " + name + ".");
        sendSystemMessageTestingOnly(pilot, "\\#88ddaa  Landing fee of " + MINIMUM_LANDING_FEE + " credits has been charged.");
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

        // Schedule a delayed map update to ensure status is properly visible
        messageTo(self, "refreshMapStatus", null, 2, false);

        int timeToDisembark = atmo_landing_registry.getTimeToDisembark(self);
        if (timeToDisembark > 0)
        {
            setObjVar(ship, "atmo.landing.dockExpiry", getGameTime() + timeToDisembark);
            messageTo(ship, "checkDockingTimer", null, 1, false);

            notifyShipOccupants(ship, "\\#88ddaa[Docking Control]: You have " + (timeToDisembark / 60) + " minutes of docking time.");
            notifyShipOccupants(ship, "\\#88ddaa  Use the Starship Management Terminal to extend docking time if needed.");
        }
        else
        {
            notifyShipOccupants(ship, "\\#88ddaa[Docking Control]: Unlimited docking time at this location.");
        }

        return SCRIPT_CONTINUE;
    }

    public int refreshMapStatus(obj_id self, dictionary params) throws InterruptedException
    {
        atmo_landing_registry.updateMapStatus(self);
        return SCRIPT_CONTINUE;
    }

    public int handleShipDeparted(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");

        atmo_landing_registry.clearOccupancy(self);

        // Schedule a delayed map update to ensure status is properly visible
        messageTo(self, "refreshMapStatus", null, 2, false);

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

