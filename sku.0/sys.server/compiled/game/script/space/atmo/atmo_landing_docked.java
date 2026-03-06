package script.space.atmo;

import script.*;
import script.library.*;

/**
 * Ship script to handle docking at atmospheric landing points.
 * When this script is attached, the ship is in a DOCKED state.
 *
 * Docked state restrictions:
 * - Ship cannot be piloted
 * - Auto-pilot cannot be engaged
 * - Cannot land at another location
 *
 * To leave docked state, player must use Ship Management Terminal to undock.
 * Undocking moves ship upward to a safe altitude before allowing normal operations.
 *
 * Docking control UI is handled by terminal_pob_ship.java
 */
public class atmo_landing_docked extends script.base_script
{
    public static final String OBJVAR_DOCK_EXPIRY = "atmo.landing.dockExpiry";
    public static final String OBJVAR_LANDING_TARGET = "atmo.landing.target";
    public static final String OBJVAR_LANDING_NAME = "atmo.landing.name";
    public static final String OBJVAR_DOCKED = "atmo.landing.docked";

    public static final int WARNING_TIME_1 = 60;
    public static final int WARNING_TIME_2 = 30;
    public static final int WARNING_TIME_3 = 10;

    public static final float UNDOCK_ALTITUDE_OFFSET = 100.0f;
    public static final float UNDOCK_RANDOM_RANGE = 50.0f;

    public static final String SND_ALARM = "sound/cbt_msl_alarm_incoming.snd";
    public static final String SND_COMM = "sound/sys_comm_generic.snd";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        // Mark ship as docked
        setObjVar(self, OBJVAR_DOCKED, true);
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        clearLandingPointOccupancy(self);
        removeObjVar(self, "atmo.landing");
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self) throws InterruptedException
    {
        clearLandingPointOccupancy(self);
        return SCRIPT_CONTINUE;
    }

    /**
     * Helper to clear the landing point occupancy when ship departs or is destroyed.
     */
    private void clearLandingPointOccupancy(obj_id ship) throws InterruptedException
    {
        if (hasObjVar(ship, OBJVAR_LANDING_TARGET))
        {
            obj_id landingPoint = getObjIdObjVar(ship, OBJVAR_LANDING_TARGET);
            if (isIdValid(landingPoint) && exists(landingPoint))
            {
                dictionary departedParams = new dictionary();
                departedParams.put("ship", ship);
                messageTo(landingPoint, "handleShipDeparted", departedParams, 0, false);
            }
        }
    }

    /**
     * Check if a ship is currently docked at a landing point.
     */
    public static boolean isShipDocked(obj_id ship) throws InterruptedException
    {
        return hasScript(ship, "space.atmo.atmo_landing_docked") && hasObjVar(ship, OBJVAR_DOCKED);
    }

    /**
     * Undock the ship from the landing point.
     * Moves ship upward to safe altitude before allowing normal operations.
     */
    public int handleUndockRequest(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");

        if (!hasObjVar(self, OBJVAR_DOCKED))
        {
            if (isIdValid(player))
                sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Ship is not docked.");
            return SCRIPT_CONTINUE;
        }

        String landingName = hasObjVar(self, OBJVAR_LANDING_NAME) ? getStringObjVar(self, OBJVAR_LANDING_NAME) : "Landing Pad";

        // Calculate undock position - move upward and slightly random horizontal offset
        location shipLoc = getLocation(self);
        float offsetX = rand(-UNDOCK_RANDOM_RANGE, UNDOCK_RANDOM_RANGE);
        float offsetZ = rand(-UNDOCK_RANDOM_RANGE, UNDOCK_RANDOM_RANGE);

        // Get terrain height at new position to ensure we're above it
        float newX = shipLoc.x + offsetX;
        float newZ = shipLoc.z + offsetZ;
        float terrainHeight = getHeightAtLocation(newX, newZ);
        float newY = Math.max(shipLoc.y + UNDOCK_ALTITUDE_OFFSET, terrainHeight + UNDOCK_ALTITUDE_OFFSET);

        location undockLoc = new location(newX, newY, newZ, shipLoc.area);

        // Notify occupants
        notifyShipOccupants(self, " ");
        notifyShipOccupants(self, "\\#00ccff========================================");
        notifyShipOccupants(self, "\\#00ccff[Docking Control]: Undocking from " + landingName + "...");
        notifyShipOccupants(self, "\\#00ccff========================================");
        notifyShipOccupants(self, " ");

        playSoundOnOccupants(self, SND_COMM);

        // Move ship to undock position
        setLocation(self, undockLoc);

        notifyShipOccupants(self, "\\#88ddaa  Ship has undocked and moved to safe altitude.");
        notifyShipOccupants(self, "\\#88ddaa  Coordinates: [" + Math.round(newX) + ", " + Math.round(newY) + ", " + Math.round(newZ) + "]");
        notifyShipOccupants(self, "\\#88ddaa  You may now pilot the ship or engage auto-pilot.");
        notifyShipOccupants(self, " ");

        if (isIdValid(player))
        {
            sendSystemMessageTestingOnly(player, "\\#00ff88[Docking Control]: Successfully undocked from " + landingName + ".");
        }

        // Remove docked state and detach script (this will trigger OnDetach to clear landing point)
        detachScript(self, "space.atmo.atmo_landing_docked");

        return SCRIPT_CONTINUE;
    }

    public int checkDockingTimer(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_DOCK_EXPIRY))
            return SCRIPT_CONTINUE;

        int expiry = getIntObjVar(self, OBJVAR_DOCK_EXPIRY);
        int now = getGameTime();
        int remaining = expiry - now;

        if (remaining <= 0)
        {
            handleDockingExpired(self);
            return SCRIPT_CONTINUE;
        }

        if (remaining <= WARNING_TIME_1 && remaining > WARNING_TIME_1 - 1)
            warnOccupants(self, remaining);
        else if (remaining <= WARNING_TIME_2 && remaining > WARNING_TIME_2 - 1)
            warnOccupants(self, remaining);
        else if (remaining <= WARNING_TIME_3 && remaining > WARNING_TIME_3 - 1)
            warnOccupants(self, remaining);

        int nextCheck = 1;
        messageTo(self, "checkDockingTimer", null, nextCheck, false);
        return SCRIPT_CONTINUE;
    }

    private void warnOccupants(obj_id ship, int secondsRemaining) throws InterruptedException
    {
        java.util.Vector players = space_transition.getContainedPlayers(ship, null);
        if (players == null)
            return;

        String name = hasObjVar(ship, OBJVAR_LANDING_NAME) ? getStringObjVar(ship, OBJVAR_LANDING_NAME) : "Landing Pad";

        for (Object p : players)
        {
            obj_id player = (obj_id) p;
            if (!isIdValid(player))
                continue;

            play2dNonLoopingSound(player, SND_ALARM);

            prose_package pp = new prose_package();
            pp.stringId = string_id.unlocalized("Docking time expiring in " + secondsRemaining + " seconds!");
            pp.digitInteger = secondsRemaining;

            commPlayer(ship, player, pp, "object/mobile/space_comm_station.iff");

            sendSystemMessageTestingOnly(player, "\\#ffaa44[Docking Control]: WARNING - " + secondsRemaining + " seconds remaining at " + name + "!");
            sendSystemMessageTestingOnly(player, "\\#ffaa44  Use the ship terminal to extend docking time or undock.");
        }
    }

    private void handleDockingExpired(obj_id ship) throws InterruptedException
    {
        obj_id landingPoint = null;
        if (hasObjVar(ship, OBJVAR_LANDING_TARGET))
            landingPoint = getObjIdObjVar(ship, OBJVAR_LANDING_TARGET);

        java.util.Vector players = space_transition.getContainedPlayers(ship, null);

        if (players != null && players.size() > 0)
        {
            location disembarkLoc = null;
            if (isIdValid(landingPoint) && exists(landingPoint))
            {
                disembarkLoc = atmo_landing_registry.getDisembarkLocation(landingPoint);
            }

            if (disembarkLoc == null)
            {
                location shipLoc = getLocation(ship);
                disembarkLoc = new location(shipLoc.x, 0, shipLoc.z, shipLoc.area);
                float terrainHeight = getHeightAtLocation(disembarkLoc.x, disembarkLoc.z);
                disembarkLoc.y = terrainHeight;
            }

            for (Object p : players)
            {
                obj_id player = (obj_id) p;
                if (!isIdValid(player))
                    continue;

                play2dNonLoopingSound(player, SND_ALARM);
                sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Your docking time has expired!");
                sendSystemMessageTestingOnly(player, "\\#ff4444  You are being relocated away from the landing pad.");

                location randomLoc = getRandomLocationNearby(disembarkLoc, 200.0f);
                warpPlayer(player, randomLoc.area, randomLoc.x, randomLoc.y, randomLoc.z, null, 0, 0, 0);
            }
        }

        // Force undock the ship
        dictionary undockParams = new dictionary();
        messageTo(ship, "handleUndockRequest", undockParams, 0, false);
    }

    private location getRandomLocationNearby(location center, float radius) throws InterruptedException
    {
        float angle = rand(0.0f, (float)(2.0 * Math.PI));
        float dist = rand(radius * 0.5f, radius);

        float x = center.x + (float)(dist * Math.cos(angle));
        float z = center.z + (float)(dist * Math.sin(angle));
        float y = getHeightAtLocation(x, z);

        return new location(x, y, z, center.area);
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

    private void playSoundOnOccupants(obj_id ship, String sound) throws InterruptedException
    {
        java.util.Vector players = space_transition.getContainedPlayers(ship, null);
        if (players != null)
        {
            for (Object p : players)
            {
                obj_id player = (obj_id) p;
                if (isIdValid(player))
                    play2dNonLoopingSound(player, sound);
            }
        }
    }
}
