package script.space;

import script.*;
import script.library.*;

/**
 * Spawner for NPC POB shuttle with repeating waypoint cycle.
 * Waypoints and landingDuration are read from datatables/npc_shuttle/<planet>.iff (compiled from .tab).
 *
 * Uses object/ship/player/player_sorosuub_space_yacht.iff
 */
public class npc_pob_ship_spawner extends script.base_script
{
    private static final String SHIP_TEMPLATE = "object/ship/player/player_sorosuub_space_yacht.iff";
    private static final String DATATABLE_PREFIX = "datatables/npc_shuttle/";
    private static final String DATATABLE_SUFFIX = ".iff";
    private static final float SPAWN_ALTITUDE = 200.0f;
    private static final int TICK_INTERVAL = 60;
    private static final int DEFAULT_LANDING_DURATION = 30;

    private static final String OBJVAR_SHIP = "npc_pob.spawner.ship";
    private static final String OBJVAR_WAYPOINT_INDEX = "npc_pob.spawner.waypointIndex";
    private static final String OBJVAR_LAST_ARRIVAL = "npc_pob.spawner.lastArrival";

    private static String getPlanetFromScene(String scene)
    {
        if (scene == null || scene.length() == 0)
            return "";
        int idx = scene.indexOf('_');
        return idx > 0 ? scene.substring(0, idx) : scene;
    }

    private String getDatatablePath(obj_id self) throws InterruptedException
    {
        String scene = getLocation(self).area;
        String planet = getPlanetFromScene(scene);
        return DATATABLE_PREFIX + planet + DATATABLE_SUFFIX;
    }

    private int getNumWaypoints(obj_id self) throws InterruptedException
    {
        try
        {
            return dataTableGetNumRows(getDatatablePath(self));
        }
        catch (Throwable t)
        {
            return 0;
        }
    }

    private int getLandingDuration(obj_id self, int waypointIndex) throws InterruptedException
    {
        try
        {
            int d = dataTableGetInt(getDatatablePath(self), waypointIndex, "landingDuration");
            return d > 0 ? d : DEFAULT_LANDING_DURATION;
        }
        catch (Throwable t)
        {
            return DEFAULT_LANDING_DURATION;
        }
    }

    public npc_pob_ship_spawner()
    {
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (space_transition.isAtmosphericFlightScene() && isGod(player))
            mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Reset Shuttle"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1 && isGod(player))
        {
            if (!space_transition.isAtmosphericFlightScene())
            {
                sendSystemMessage(player, string_id.unlocalized("Shuttle can only be reset in atmospheric flight."));
                return SCRIPT_CONTINUE;
            }
            obj_id oldShip = hasObjVar(self, OBJVAR_SHIP) ? getObjIdObjVar(self, OBJVAR_SHIP) : null;
            if (isIdValid(oldShip) && exists(oldShip))
                destroyObject(oldShip);
            removeObjVar(self, OBJVAR_SHIP);
            removeObjVar(self, OBJVAR_WAYPOINT_INDEX);
            removeObjVar(self, OBJVAR_LAST_ARRIVAL);
            int numWaypoints = getNumWaypoints(self);
            if (numWaypoints <= 0)
            {
                sendSystemMessage(player, string_id.unlocalized("No shuttle route data; cannot spawn."));
                return SCRIPT_CONTINUE;
            }
            obj_id ship = spawnShip(self);
            if (isIdValid(ship))
            {
                setObjVar(self, OBJVAR_SHIP, ship);
                setObjVar(self, OBJVAR_WAYPOINT_INDEX, 0);
                flyToWaypoint(self, ship, 0);
                sendSystemMessage(player, string_id.unlocalized("Shuttle reset and spawned at waypoint 0."));
            }
            else
                sendSystemMessage(player, string_id.unlocalized("Failed to spawn shuttle."));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
        {
            messageTo(self, "npcPobSpawnerTick", null, TICK_INTERVAL, false);
            return SCRIPT_CONTINUE;
        }

        int numWaypoints = getNumWaypoints(self);
        if (numWaypoints > 0)
        {
            obj_id ship = spawnShip(self);
            if (isIdValid(ship))
            {
                setObjVar(self, OBJVAR_SHIP, ship);
                setObjVar(self, OBJVAR_WAYPOINT_INDEX, 0);
                flyToWaypoint(self, ship, 0);
            }
        }
        messageTo(self, "npcPobSpawnerTick", null, TICK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    public int npcPobSpawnerTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
        {
            messageTo(self, "npcPobSpawnerTick", null, TICK_INTERVAL, false);
            return SCRIPT_CONTINUE;
        }

        obj_id ship = hasObjVar(self, OBJVAR_SHIP) ? getObjIdObjVar(self, OBJVAR_SHIP) : null;
        if (!isIdValid(ship) || !exists(ship))
        {
            ship = spawnShip(self);
            if (isIdValid(ship))
            {
                setObjVar(self, OBJVAR_SHIP, ship);
                setObjVar(self, OBJVAR_WAYPOINT_INDEX, 0);
                setObjVar(self, OBJVAR_LAST_ARRIVAL, getGameTime());
            }
        }

        if (isIdValid(ship) && exists(ship))
        {
            boolean autopilotActive = shipIsAutopilotActive(ship);
            obj_id pilot = getPilotId(ship);
            boolean hasPilot = isIdValid(pilot);

            if (!autopilotActive && !hasPilot)
            {
                int idx = hasObjVar(self, OBJVAR_WAYPOINT_INDEX) ? getIntObjVar(self, OBJVAR_WAYPOINT_INDEX) : 0;
                int landingDuration = getLandingDuration(self, idx);
                int lastArrival = hasObjVar(self, OBJVAR_LAST_ARRIVAL) ? getIntObjVar(self, OBJVAR_LAST_ARRIVAL) : 0;
                int now = getGameTime();
                if (now - lastArrival >= landingDuration)
                {
                    int numWaypoints = getNumWaypoints(self);
                    if (numWaypoints <= 0)
                    {
                        messageTo(self, "npcPobSpawnerTick", null, TICK_INTERVAL, false);
                        return SCRIPT_CONTINUE;
                    }
                    // Cycle: 0 -> 1 -> ... -> (n-1) -> 0 (endless)
                    int nextIdx = (idx + 1) % numWaypoints;
                    setObjVar(self, OBJVAR_WAYPOINT_INDEX, nextIdx);
                    setObjVar(self, OBJVAR_LAST_ARRIVAL, now);
                    flyToWaypoint(self, ship, nextIdx);
                }
            }
        }

        messageTo(self, "npcPobSpawnerTick", null, TICK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    private obj_id spawnShip(obj_id self) throws InterruptedException
    {
        String dtPath = getDatatablePath(self);
        float x = dataTableGetFloat(dtPath, 0, "x");
        float z = dataTableGetFloat(dtPath, 0, "z");
        float groundY = getHeightAtLocation(x, z);
        float y = groundY + SPAWN_ALTITUDE;
        String scene = getLocation(self).area;
        location loc = new location(x, y, z, scene, null);

        obj_id ship = createObject(SHIP_TEMPLATE, loc);
        if (!isIdValid(ship))
            return ship;

        if (!hasScript(ship, "space.npc_pob_ship_controller"))
            attachScript(ship, "space.npc_pob_ship_controller");
        if (!hasScript(ship, "space.ship.ship_atmospheric_boarding"))
            attachScript(ship, "space.ship.ship_atmospheric_boarding");

        String planet = getPlanetFromScene(scene);
        setName(ship, planet != null && planet.length() > 0 ? (planet.substring(0, 1).toUpperCase() + planet.substring(1) + " Shuttle") : "Shuttle");
        setObjVar(self, OBJVAR_LAST_ARRIVAL, getGameTime());
        return ship;
    }

    private void flyToWaypoint(obj_id self, obj_id ship, int index) throws InterruptedException
    {
        int numWaypoints = getNumWaypoints(self);
        if (index < 0 || index >= numWaypoints)
            return;
        String dtPath = getDatatablePath(self);
        float x = dataTableGetFloat(dtPath, index, "x");
        float z = dataTableGetFloat(dtPath, index, "z");
        dictionary params = new dictionary();
        params.put("x", x);
        params.put("z", z);
        messageTo(ship, "npcPobFlyTo", params, 0, false);
    }
}
