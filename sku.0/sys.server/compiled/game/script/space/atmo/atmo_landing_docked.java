package script.space.atmo;

import script.*;
import script.library.*;

/**
 * Ship script to handle docking at atmospheric landing points.
 * Manages docking timer warnings and expiration.
 * Docking control UI is handled by terminal_pob_ship.java
 */
public class atmo_landing_docked extends script.base_script
{
    public static final String OBJVAR_DOCK_EXPIRY = "atmo.landing.dockExpiry";
    public static final String OBJVAR_LANDING_TARGET = "atmo.landing.target";
    public static final String OBJVAR_LANDING_NAME = "atmo.landing.name";

    public static final int WARNING_TIME_1 = 60;
    public static final int WARNING_TIME_2 = 30;
    public static final int WARNING_TIME_3 = 10;

    public static final String SND_ALARM = "sound/cbt_msl_alarm_incoming.snd";
    public static final String SND_COMM = "sound/sys_comm_generic.snd";

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

        if (remaining == WARNING_TIME_1 || remaining == WARNING_TIME_2 || remaining == WARNING_TIME_3)
        {
            warnOccupants(self, remaining);
        }

        int nextCheck = remaining;
        if (remaining > WARNING_TIME_1)
            nextCheck = remaining - WARNING_TIME_1;
        else if (remaining > WARNING_TIME_2)
            nextCheck = remaining - WARNING_TIME_2;
        else if (remaining > WARNING_TIME_3)
            nextCheck = remaining - WARNING_TIME_3;
        else
            nextCheck = remaining;

        if (nextCheck < 1)
            nextCheck = 1;

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
            sendSystemMessageTestingOnly(player, "\\#ffaa44  Use the ship terminal to extend docking time.");
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

        if (isIdValid(landingPoint) && exists(landingPoint))
        {
            dictionary departedParams = new dictionary();
            departedParams.put("ship", ship);
            messageTo(landingPoint, "handleShipDeparted", departedParams, 0, false);
        }

        removeObjVar(ship, "atmo.landing");
        detachScript(ship, "space.atmo.atmo_landing_docked");
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
}
