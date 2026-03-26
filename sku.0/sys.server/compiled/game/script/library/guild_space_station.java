package script.library;

import script.dictionary;
import script.location;
import script.obj_id;
import script.string_id;

/**
 * Guild-owned space station instance (dungeon_hub building + orbit marker + ClusterWideData).
 */
public class guild_space_station extends script.base_script
{
    public guild_space_station()
    {
    }

    public static final String CW_MANAGER = "guild_space_stations";
    public static final int PURCHASE_COST_CREDITS = 50_000_000;
    public static final int MAINTENANCE_COST_CREDITS = 500_000;
    /** Seconds between maintenance bills (7 days). */
    public static final int MAINTENANCE_PERIOD_SEC = 7 * 24 * 60 * 60;
    /** AGL for the "fake" station over the planet. */
    public static final float ORBIT_HEIGHT_M = 3000.0f;

    public static final String OV_GUILD_ID = "guildStation.guildId";
    public static final String OV_BUILDING_ID = "guildStation.buildingId";
    public static final String OV_ORBIT_PLANET = "guildStation.orbit.planet";
    public static final String OV_ORBIT_X = "guildStation.orbit.x";
    public static final String OV_ORBIT_Z = "guildStation.orbit.z";
    public static final String OV_ORBIT_MARKER = "guildStation.orbit.markerId";
    public static final String OV_MAINTENANCE_NEXT = "guildStation.maintenance.nextTime";
    public static final String OV_ACCESS_MODE = "guildStation.access.mode";
    /** 0 = all guild members; 1 = min guild rank only; 2 = whitelist objIds packed */
    public static final int ACCESS_GUILD = 0;
    public static final int ACCESS_RANK = 1;
    public static final int ACCESS_WHITELIST = 2;
    public static final String OV_ACCESS_MIN_RANK = "guildStation.access.minRank";
    public static final String OV_ACCESS_WHITELIST = "guildStation.access.whitelist";

    private static final String TEMPLATE_ORBIT_SHIP = "object/ship/spacestation_neutral.iff";

    public static String cwElementName(int guildId)
    {
        return "guild_" + guildId;
    }

    public static location computeHubSlot(int guildId)
    {
        int slot = Math.abs(guildId) % 400;
        int row = slot / 20;
        int col = slot % 20;
        location loc = new location();
        loc.area = "dungeon_hub";
        loc.x = col * 200.0f - 2000.0f;
        loc.y = 0.0f;
        loc.z = row * 200.0f - 2000.0f;
        return loc;
    }

    public static void grantComlink(obj_id player, int guildId) throws InterruptedException
    {
        obj_id inv = utils.getInventoryContainer(player);
        if (!isIdValid(inv))
            return;
        obj_id comlink = createObject("object/tangible/loot/tool/guild_space_station_comlink.iff", inv, "");
        if (!isIdValid(comlink))
            return;
        setObjVar(comlink, OV_GUILD_ID, guildId);
    }

    /**
     * Spawns the atmospheric "orbit" prop over the given point. Must be invoked on a server running {@code planet}'s scene.
     */
    public static obj_id spawnOrbitMarkerForPlanet(obj_id player, int guildId, String planet, float x, float z, obj_id destroyOld) throws InterruptedException
    {
        if (!getCurrentSceneName().equals(planet))
        {
            sendSystemMessage(player, string_id.unlocalized("Orbit beacon must be refreshed while you are on " + planet + "."));
            return obj_id.NULL_ID;
        }
        if (isIdValid(destroyOld) && exists(destroyOld))
            destroyObject(destroyOld);
        float y = getHeightAtLocation(x, z) + ORBIT_HEIGHT_M;
        location loc = new location(x, y, z, planet);
        obj_id marker = createObject(TEMPLATE_ORBIT_SHIP, loc);
        if (isIdValid(marker))
        {
            setObjVar(marker, OV_GUILD_ID, guildId);
            attachScript(marker, "guild.guild_space_station_orbit_marker");
        }
        return marker;
    }

    public static void syncBuildingPermissions(obj_id building, int guildId) throws InterruptedException
    {
        if (!isIdValid(building))
            return;
        int mode = hasObjVar(building, OV_ACCESS_MODE) ? getIntObjVar(building, OV_ACCESS_MODE) : ACCESS_GUILD;
        permissionsRemoveAllAllowed(building);
        permissionsMakePrivate(building);

        if (mode == ACCESS_WHITELIST)
        {
            String packed = getStringObjVar(building, OV_ACCESS_WHITELIST);
            if (packed != null && packed.length() > 0)
            {
                String[] ids = split(packed, ':');
                for (String s : ids)
                {
                    try
                    {
                        long lid = Long.parseLong(s.trim());
                        obj_id who = obj_id.getObjId(lid);
                        if (isIdValid(who) && exists(who))
                            permissionsAddAllowed(building, getPlayerName(who));
                    }
                    catch (NumberFormatException ignore)
                    {
                    }
                }
            }
            return;
        }

        obj_id[] members = guildGetMemberIds(guildId);
        if (members == null)
            return;
        String minRank = null;
        if (mode == ACCESS_RANK && hasObjVar(building, OV_ACCESS_MIN_RANK))
            minRank = getStringObjVar(building, OV_ACCESS_MIN_RANK);

        for (obj_id m : members)
        {
            if (!isIdValid(m) || !exists(m))
                continue;
            if (minRank != null && minRank.length() > 0 && !guildHasMemberRank(guildId, m, minRank))
                continue;
            permissionsAddAllowed(building, getPlayerName(m));
        }
    }

    public static boolean playerCanEnterStation(obj_id player, int guildId, dictionary cw) throws InterruptedException
    {
        if (isGod(player))
            return true;
        if (getGuildId(player) != guildId)
            return false;
        int mode = (cw != null && cw.containsKey("access_mode")) ? cw.getInt("access_mode") : ACCESS_GUILD;
        if (mode == ACCESS_GUILD)
            return true;
        if (mode == ACCESS_RANK)
        {
            String minRank = (cw != null && cw.containsKey("access_min_rank")) ? cw.getString("access_min_rank") : "";
            return minRank == null || minRank.length() < 1 || guildHasMemberRank(guildId, player, minRank);
        }
        if (mode == ACCESS_WHITELIST)
        {
            String packed = (cw != null && cw.containsKey("access_whitelist")) ? cw.getString("access_whitelist") : "";
            if (packed == null || packed.length() < 1)
                return false;
            String needle = Long.toString(player.getValue());
            return packed.indexOf(needle) >= 0;
        }
        return false;
    }

    public static boolean isMaintenanceOverdue(obj_id building) throws InterruptedException
    {
        if (!hasObjVar(building, OV_MAINTENANCE_NEXT))
            return false;
        return getCalendarTime() >= getIntObjVar(building, OV_MAINTENANCE_NEXT);
    }

    /** ClusterWideData row for this guild. */
    public static void warpPlayerToStation(obj_id player, dictionary cw) throws InterruptedException
    {
        if (cw == null)
            return;
        obj_id building = cw.getObjId("building_id");
        int guildId = cw.getInt("guild_id");
        if (!isIdValid(building) || !exists(building))
        {
            sendSystemMessage(player, string_id.unlocalized("[Navicomputer] Guild station is offline."));
            return;
        }
        if (!playerCanEnterStation(player, guildId, cw))
        {
            sendSystemMessage(player, string_id.unlocalized("[Navicomputer] You are not cleared for this station."));
            return;
        }
        if (!isIdValid(getCellId(building, "hangarbay1")))
        {
            sendSystemMessage(player, string_id.unlocalized("[Navicomputer] Docking bay not found."));
            return;
        }
        location bLoc = getLocation(building);
        if (bLoc == null || bLoc.area == null || bLoc.area.length() < 1)
        {
            sendSystemMessage(player, string_id.unlocalized("[Navicomputer] Guild station is offline."));
            return;
        }
        warpPlayer(player, bLoc.area, bLoc.x, bLoc.y, bLoc.z, building, "hangarbay1", 5.0f, 0.0f, 5.0f);
    }

    public static dictionary buildCwSnapshot(obj_id building, int guildId, obj_id orbitMarkerId) throws InterruptedException
    {
        dictionary d = new dictionary();
        d.put("guild_id", guildId);
        d.put("building_id", building);
        if (hasObjVar(building, OV_ORBIT_PLANET))
            d.put("orbit_planet", getStringObjVar(building, OV_ORBIT_PLANET));
        if (hasObjVar(building, OV_ORBIT_X))
            d.put("orbit_x", getFloatObjVar(building, OV_ORBIT_X));
        if (hasObjVar(building, OV_ORBIT_Z))
            d.put("orbit_z", getFloatObjVar(building, OV_ORBIT_Z));
        if (isIdValid(orbitMarkerId))
            d.put("orbit_marker_id", orbitMarkerId);
        else if (hasObjVar(building, OV_ORBIT_MARKER))
            d.put("orbit_marker_id", getObjIdObjVar(building, OV_ORBIT_MARKER));
        if (hasObjVar(building, OV_MAINTENANCE_NEXT))
            d.put("maintenance_next", getIntObjVar(building, OV_MAINTENANCE_NEXT));
        int mode = hasObjVar(building, OV_ACCESS_MODE) ? getIntObjVar(building, OV_ACCESS_MODE) : ACCESS_GUILD;
        d.put("access_mode", mode);
        if (hasObjVar(building, OV_ACCESS_MIN_RANK))
            d.put("access_min_rank", getStringObjVar(building, OV_ACCESS_MIN_RANK));
        if (hasObjVar(building, OV_ACCESS_WHITELIST))
            d.put("access_whitelist", getStringObjVar(building, OV_ACCESS_WHITELIST));
        return d;
    }

    /**
     * Completes a leader purchase after ClusterWideData lock (callback from guild terminal).
     */
    public static void onClusterWidePurchaseResponse(obj_id terminal, obj_id player, int guildId, String planet, float ox, float oz, String manage_name, String name, int request_id, String[] element_name_list, dictionary[] data, int lock_key) throws InterruptedException
    {
        String el = cwElementName(guildId);
        if (data != null && data.length > 0 && data[0] != null)
        {
            obj_id existing = data[0].getObjId("building_id");
            if (isIdValid(existing) && exists(existing))
            {
                sendSystemMessage(player, string_id.unlocalized("Your guild already has a registered space station."));
                releaseClusterWideDataLock(manage_name, lock_key);
                return;
            }
        }

        location hubLoc = computeHubSlot(guildId);
        obj_id building = createObject("object/building/hub/space_station.iff", hubLoc);
        if (!isIdValid(building))
        {
            sendSystemMessage(player, string_id.unlocalized("Failed to deploy station in Unknown Regions."));
            releaseClusterWideDataLock(manage_name, lock_key);
            return;
        }

        setObjVar(building, OV_GUILD_ID, guildId);
        setObjVar(building, OV_ORBIT_PLANET, planet);
        setObjVar(building, OV_ORBIT_X, ox);
        setObjVar(building, OV_ORBIT_Z, oz);
        setObjVar(building, OV_ACCESS_MODE, ACCESS_GUILD);
        int nextMaint = getCalendarTime() + MAINTENANCE_PERIOD_SEC;
        setObjVar(building, OV_MAINTENANCE_NEXT, nextMaint);

        obj_id oldMarker = obj_id.NULL_ID;
        obj_id orbitMarker = spawnOrbitMarkerForPlanet(player, guildId, planet, ox, oz, oldMarker);
        if (isIdValid(orbitMarker))
            setObjVar(building, OV_ORBIT_MARKER, orbitMarker);

        syncBuildingPermissions(building, guildId);

        dictionary snap = buildCwSnapshot(building, guildId, orbitMarker);
        replaceClusterWideData(CW_MANAGER, el, snap, true, lock_key);
        releaseClusterWideDataLock(manage_name, lock_key);

        grantComlink(player, guildId);
        sendSystemMessage(player, string_id.unlocalized("[Guild] Guild space station deployed. A comlink has been placed in your inventory."));
        CustomerServiceLog("guild_space_station", "Guild " + guildId + " purchased station building " + building + " leader " + player);
    }

    public static void onClusterWidePushResponse(obj_id building, int guildId, String manage_name, String name, int request_id, String[] element_name_list, dictionary[] data, int lock_key) throws InterruptedException
    {
        if (!isIdValid(building))
        {
            releaseClusterWideDataLock(manage_name, lock_key);
            return;
        }
        obj_id marker = hasObjVar(building, OV_ORBIT_MARKER) ? getObjIdObjVar(building, OV_ORBIT_MARKER) : obj_id.NULL_ID;
        dictionary snap = buildCwSnapshot(building, guildId, marker);
        replaceClusterWideData(CW_MANAGER, cwElementName(guildId), snap, true, lock_key);
        releaseClusterWideDataLock(manage_name, lock_key);
    }
}
