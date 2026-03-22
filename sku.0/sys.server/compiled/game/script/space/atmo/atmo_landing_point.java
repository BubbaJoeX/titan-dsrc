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
 *   atmo.landing_point.landing_altitude - optional world Y (m) for autopilot touchdown; default is loc.y
 *   atmo.landing_point.loc_offset - optional offset for small platforms
 *
 * Extended policy (see {@link atmo_landing_manager}): map.category / map.subcategory (script metadata only;
 * planet map registration is always category {@code atmo_landing}), policy.access_mode, faction/guild/allowlist,
 * fees, dock duration override, extend-dock tuning.
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
     * Clears stale occupancy if ship no longer exists or has departed.
     */
    public int validateOccupancy(obj_id self, dictionary params) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(self))
            return SCRIPT_CONTINUE;

        // Check if we have a landed ship
        if (atmo_landing_registry.isLanded(self))
        {
            obj_id occupier = atmo_landing_registry.getOccupyingShip(self);
            if (isIdValid(occupier))
            {
                // Verify ship still exists
                if (!exists(occupier))
                {
                    atmo_landing_registry.clearOccupancy(self);
                }
                // Verify ship still has landed_at reference to this landing point
                else if (hasObjVar(occupier, "atmo.landing.landed_at"))
                {
                    obj_id landedAt = getObjIdObjVar(occupier, "atmo.landing.landed_at");
                    if (!isIdValid(landedAt) || landedAt != self)
                    {
                        atmo_landing_registry.clearOccupancy(self);
                    }
                }
                else
                {
                    // Ship doesn't have landed_at reference - it has departed
                    atmo_landing_registry.clearOccupancy(self);
                }
            }
            else
            {
                atmo_landing_registry.clearOccupancy(self);
            }
        }

        // Check and validate ETA reservations
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

        // Block if ship is already docked (separate docking system)
        if (hasObjVar(ship, "atmo.landing.docked"))
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_ALREADY_DOCKED));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Your ship is already docked at a platform.");
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Use the Starship Management Terminal to undock first.");
            return SCRIPT_CONTINUE;
        }

        // Block if ship is already landed somewhere
        if (hasObjVar(ship, "atmo.landing.landed_at"))
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_ALREADY_DOCKED));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Your ship is already landed at another location.");
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Take off first before requesting new landing clearance.");
            return SCRIPT_CONTINUE;
        }

        if (!atmo_landing_registry.isLandingPoint(self))
        {
            sendSystemMessageTestingOnly(pilot, "This landing point is not properly configured.");
            return SCRIPT_CONTINUE;
        }

        if (!atmo_landing_manager.canPilotLandAt(self, pilot, ship))
        {
            String reason = atmo_landing_manager.getLandingAccessDenialReason(self, pilot, ship);
            commPlayerIDA(pilot, "Imperial Docking Authority. Landing clearance denied. This platform is restricted.");
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: You are not cleared to land at this platform.");
            if (reason != null)
                sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Reason: " + mapAccessDenialReasonForPlayer(reason));
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

        boolean waiveFees = atmo_landing_manager.shouldWaiveLandingFee(self);
        int baseLandingFee = atmo_landing_manager.resolveBaseLandingFeeCredits(self, MINIMUM_LANDING_FEE);
        boolean ignoreCityTax = atmo_landing_manager.shouldIgnoreCityLandingTax(self);

        // Check and charge landing fee
        int totalFunds = money.getTotalMoney(pilot);
        int totalLandingFee = waiveFees ? 0 : baseLandingFee;
        int cityLandingTax = 0;
        int cityId = 0;
        String cityName = null;

        // Check if landing point is in a city and apply city landing tax
        location landingLoc = atmo_landing_registry.getLandingLocation(self);
        if (!ignoreCityTax && landingLoc != null)
        {
            cityId = getCityAtLocation(landingLoc, 0);
            if (cityId > 0 && cityExists(cityId))
            {
                cityLandingTax = city.getStarshipLandingTax(cityId);
                if (cityLandingTax > 0)
                {
                    totalLandingFee += cityLandingTax;
                    cityName = cityGetName(cityId);
                }
            }
        }

        if (totalFunds < totalLandingFee)
        {
            commPlayerIDA(pilot, getRandomMessage(IDA_INSUFFICIENT_FUNDS));
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Insufficient funds for landing fee.");
            if (!waiveFees)
                sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Landing fee: " + baseLandingFee + " credits.");
            if (cityLandingTax > 0)
            {
                sendSystemMessageTestingOnly(pilot, "\\#ffaa44  " + cityName + " City Landing Tax: " + cityLandingTax + " credits.");
                sendSystemMessageTestingOnly(pilot, "\\#ffaa44  Total required: " + totalLandingFee + " credits.");
            }
            sendSystemMessageTestingOnly(pilot, "\\#ffaa44  You have: " + totalFunds + " credits.");

            // Eject ship from city if in city bounds
            if (cityId > 0 && cityLandingTax > 0)
            {
                city.ejectShipFromCity(ship, cityId);
            }
            return SCRIPT_CONTINUE;
        }

        // Charge the base landing fee (to travel system account)
        if (!waiveFees)
        {
            if (!transferBankCreditsToNamedAccount(pilot, money.ACCT_TRAVEL, baseLandingFee, "noHandler", "noHandler", new dictionary()))
            {
                commPlayerIDA(pilot, getRandomMessage(IDA_INSUFFICIENT_FUNDS));
                sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Unable to process landing fee payment.");
                return SCRIPT_CONTINUE;
            }
        }

        // Charge city landing tax if applicable
        if (cityLandingTax > 0 && cityId > 0)
        {
            obj_id cityHall = cityGetCityHall(cityId);
            if (isIdValid(cityHall))
            {
                if (!money.pay(pilot, cityHall, cityLandingTax, "landing_tax", null, false))
                {
                    // Refund base fee if city tax fails
                    if (!waiveFees)
                        transferBankCreditsFromNamedAccount(money.ACCT_TRAVEL, pilot, baseLandingFee, "noHandler", "noHandler", new dictionary());
                    commPlayerIDA(pilot, getRandomMessage(IDA_INSUFFICIENT_FUNDS));
                    sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Unable to process city landing tax.");
                    return SCRIPT_CONTINUE;
                }
            }
        }

        int eta = atmo_landing_registry.calculateETA(ship, self);
        if (!atmo_landing_registry.reserveLandingPoint(self, ship, eta))
        {
            sendSystemMessageTestingOnly(pilot, "\\#ff4444[Imperial Docking Authority]: Unable to reserve landing pad. Please try again.");
            return SCRIPT_CONTINUE;
        }

        landingLoc = atmo_landing_registry.getLandingLocation(self);
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
        if (waiveFees)
            sendSystemMessageTestingOnly(pilot, "\\#88ddaa  Landing fees waived for this platform.");
        else
            sendSystemMessageTestingOnly(pilot, "\\#88ddaa  Landing fee of " + baseLandingFee + " credits has been charged.");
        if (cityLandingTax > 0 && cityName != null)
        {
            sendSystemMessageTestingOnly(pilot, "\\#88ddaa  " + cityName + " City Landing Tax: " + cityLandingTax + " credits.");
        }
        sendSystemMessageTestingOnly(pilot, "\\#aaddff  Auto-pilot engaged. ETA: " + eta + " seconds.");

        return SCRIPT_CONTINUE;
    }

    public int handleShipArrived(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");

        if (!isIdValid(ship) || !exists(ship))
            return SCRIPT_CONTINUE;

        // Mark as occupied with LANDED state (not docked - docking is a separate system)
        atmo_landing_registry.occupyLandingPoint(self, ship);

        // Store reference on ship for tracking
        setObjVar(ship, "atmo.landing.landed_at", self);

        atmo_landing_manager.onShipLandedAtPoint(ship, self);

        float yaw = atmo_landing_registry.getLandingYaw(self);
        applyShipYaw(ship, yaw);

        // Schedule multiple delayed map updates to ensure status is properly visible
        messageTo(self, "refreshMapStatus", null, 1, false);
        messageTo(self, "refreshMapStatus", null, 3, false);
        messageTo(self, "refreshMapStatus", null, 5, false);

        String landingName = atmo_landing_registry.getLandingPointName(self);
        notifyShipOccupants(ship, "\\#88ddaa[Imperial Docking Authority]: You have landed at " + landingName + ".");
        notifyShipOccupants(ship, "\\#88ddaa  You may take off at any time by piloting the ship.");

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

        // Clear occupancy state
        atmo_landing_registry.clearOccupancy(self);

        // Schedule multiple delayed map updates to ensure status is properly visible
        messageTo(self, "refreshMapStatus", null, 1, false);
        messageTo(self, "refreshMapStatus", null, 3, false);
        messageTo(self, "refreshMapStatus", null, 5, false);

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

    private static String mapAccessDenialReasonForPlayer(String code)
    {
        if (code == null)
            return "access denied";
        if (code.equals("owner_pilot_only"))
            return "only the ship owner may request landing here";
        if (code.equals("gm_only"))
            return "authorized personnel only";
        if (code.equals("faction_not_allowed"))
            return "your faction is not cleared for this platform";
        if (code.equals("faction_whitelist_empty"))
            return "platform faction access is not configured";
        if (code.equals("guild_mismatch"))
            return "your guild is not assigned to this platform";
        if (code.equals("guild_not_configured"))
            return "guild-only platform is missing guild_id";
        if (code.equals("not_on_allowlist"))
            return "you are not on the landing allow-list";
        if (code.equals("allowlist_empty"))
            return "allow-list is empty";
        if (code.equals("missing_required_skill"))
            return "missing required skill certification";
        if (code.equals("below_min_level"))
            return "below minimum combat level";
        return code;
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

