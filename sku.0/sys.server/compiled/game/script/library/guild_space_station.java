package script.library;

import script.dictionary;
import script.location;
import script.obj_id;
import script.string_id;
import script.library.callable;
import script.library.space_transition;
import script.library.sui;
import script.library.utils;

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
    /** Set on player while cross-scene transfer to dungeon_hub is pending (comlink warp). */
    public static final String OV_PENDING_COMLINK_WARP = "guildStation.pendingComlinkWarp";
    /** Comlink ClusterWide follow-up: warp vs orbit refresh (scriptvar on comlink). */
    public static final String SV_COMLINK_CW_ACTION = "guildStation.pendingCwAction";
    public static final String COMLINK_ACTION_WARP = "warp";
    public static final String COMLINK_ACTION_ORBIT = "orbit";

    private static final String TEMPLATE_ORBIT_SHIP = "object/ship/spacestation_neutral.iff";

    public static String cwElementName(int guildId)
    {
        return "guild_" + guildId;
    }

    /**
     * Dismount, store callables, pack surface ship before station travel.
     * In atmospheric flight or space, do not call packShip then warp in the same frame — packShip defers finalize and
     * cross-scene warps have caused client AlterScheduler crashes. Unpilot instead; ground players still get packShip.
     */
    public static void preparePlayerForGuildStationTravel(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
            return;
        utils.dismountRiderJetpackCheck(player);
        callable.storeCallables(player);
        if (isAtmosphericFlightScene() || isSpaceScene())
        {
            obj_id piloted = getPilotedShip(player);
            if (isIdValid(piloted))
                unpilotShip(player);
            return;
        }
        obj_id ship = space_transition.getContainingShip(player);
        if (isIdValid(ship) && exists(ship))
            space_transition.packShip(ship);
    }

    /** SUI summary for orbit marker "Station Information" option. */
    public static void showStationInformationToPlayer(obj_id player, int guildId) throws InterruptedException
    {
        if (guildId <= 0 || !guildExists(guildId))
        {
            sendSystemMessage(player, string_id.unlocalized("No guild data is available for this station."));
            return;
        }
        String name = guildGetName(guildId);
        String abbrev = guildGetAbbrev(guildId);
        int members = guildGetCountMembersOnly(guildId);
        String msg = "Guild: " + name + " <" + abbrev + ">\nRegistered members: " + members;
        sui.msgbox(player, player, msg, sui.OK_ONLY, "Station Information", sui.MSG_NORMAL, "noHandler");
    }

    /** Cache on guild terminal: hide purchase menu after a station is registered in ClusterWideData. */
    public static void markGuildTerminalPurchaseCached(obj_id terminal, int guildId) throws InterruptedException
    {
        if (isIdValid(terminal) && exists(terminal))
            utils.setScriptVar(terminal, "guildStation.reg." + guildId, true);
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
     * Orbit beacon: invulnerable, conversable, display name {@code Station: <abbrev>}. Call from marker attach/init
     * whenever the object is created or refreshed (e.g. comlink orbit move).
     */
    public static void applyOrbitMarkerPresentation(obj_id marker) throws InterruptedException
    {
        if (!isIdValid(marker) || !exists(marker))
            return;
        setInvulnerable(marker, true);
        setCondition(marker, CONDITION_CONVERSABLE);
        if (!hasObjVar(marker, OV_GUILD_ID))
            return;
        int guildId = getIntObjVar(marker, OV_GUILD_ID);
        if (guildId <= 0 || !guildExists(guildId))
            return;
        setName(marker, "Station: <" + guildGetAbbrev(guildId) + ">");
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
            applyOrbitMarkerPresentation(marker);
        }
        return marker;
    }

    /**
     * Refresh orbit marker at the player's current outdoor position (comlink flow). Updates building objvars when the
     * station exists on this process, otherwise updates ClusterWideData only.
     */
    public static void applyOrbitBeaconRefreshFromComlink(obj_id player, int guildId, dictionary cw) throws InterruptedException
    {
        if (cw == null || player == null)
            return;
        location loc = getLocation(player);
        if (loc == null || loc.area == null)
            return;
        String planet = loc.area;
        float ox = loc.x;
        float oz = loc.z;
        obj_id oldMarker = cw.containsKey("orbit_marker_id") ? cw.getObjId("orbit_marker_id") : obj_id.NULL_ID;
        obj_id newMarker = spawnOrbitMarkerForPlanet(player, guildId, planet, ox, oz, oldMarker);
        if (!isIdValid(newMarker))
            return;

        obj_id building = cw.containsKey("building_id") ? cw.getObjId("building_id") : obj_id.NULL_ID;
        if (isIdValid(building) && exists(building))
        {
            setObjVar(building, OV_ORBIT_PLANET, planet);
            setObjVar(building, OV_ORBIT_X, ox);
            setObjVar(building, OV_ORBIT_Z, oz);
            setObjVar(building, OV_ORBIT_MARKER, newMarker);
            messageTo(building, "guildStationPushCw", null, 0.5f, false);
        }
        else
        {
            cw.put("orbit_planet", planet);
            cw.put("orbit_x", ox);
            cw.put("orbit_z", oz);
            cw.put("orbit_marker_id", newMarker);
            replaceClusterWideData(CW_MANAGER, cwElementName(guildId), cw, true, -1);
        }
        sendSystemMessage(player, string_id.unlocalized("Orbit beacon updated above your current position."));
    }

    public static void syncBuildingPermissions(obj_id building, int guildId) throws InterruptedException
    {
        if (!isIdValid(building))
            return;
        obj_id leader = guildGetLeader(guildId);
        if (isIdValid(leader) && exists(leader))
            setOwner(building, leader);
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

    /** ClusterWide snapshot when the hub building does not exist yet (purchase finalized on a non-hub scene). */
    public static dictionary buildCwSnapshotWithoutBuilding(int guildId, String planet, float ox, float oz, obj_id orbitMarkerId, int maintenanceNext) throws InterruptedException
    {
        dictionary d = new dictionary();
        d.put("guild_id", guildId);
        if (planet != null && planet.length() > 0)
            d.put("orbit_planet", planet);
        d.put("orbit_x", ox);
        d.put("orbit_z", oz);
        if (isIdValid(orbitMarkerId))
            d.put("orbit_marker_id", orbitMarkerId);
        d.put("maintenance_next", maintenanceNext);
        d.put("access_mode", ACCESS_GUILD);
        return d;
    }

    /** Apply ClusterWide fields to a freshly spawned hub building (lazy spawn on dungeon_hub). */
    public static void applyClusterWideToBuilding(obj_id building, int guildId, dictionary cw) throws InterruptedException
    {
        if (!isIdValid(building) || cw == null)
            return;
        setObjVar(building, OV_GUILD_ID, guildId);
        if (cw.containsKey("orbit_planet"))
            setObjVar(building, OV_ORBIT_PLANET, cw.getString("orbit_planet"));
        if (cw.containsKey("orbit_x"))
            setObjVar(building, OV_ORBIT_X, cw.getFloat("orbit_x"));
        if (cw.containsKey("orbit_z"))
            setObjVar(building, OV_ORBIT_Z, cw.getFloat("orbit_z"));
        if (cw.containsKey("maintenance_next"))
            setObjVar(building, OV_MAINTENANCE_NEXT, cw.getInt("maintenance_next"));
        if (cw.containsKey("access_mode"))
            setObjVar(building, OV_ACCESS_MODE, cw.getInt("access_mode"));
        if (cw.containsKey("access_min_rank"))
            setObjVar(building, OV_ACCESS_MIN_RANK, cw.getString("access_min_rank"));
        if (cw.containsKey("access_whitelist"))
            setObjVar(building, OV_ACCESS_WHITELIST, cw.getString("access_whitelist"));
        if (cw.containsKey("orbit_marker_id"))
        {
            obj_id marker = cw.getObjId("orbit_marker_id");
            if (isIdValid(marker))
                setObjVar(building, OV_ORBIT_MARKER, marker);
        }
    }

    /**
     * Ensures the guild station building exists in the current process (dungeon_hub). Purchase may run on another scene,
     * so CW can list a building_id that does not exist here until we spawn it.
     */
    public static obj_id ensureStationBuildingOnHub(obj_id player, int guildId, dictionary cw) throws InterruptedException
    {
        if (cw == null || !getCurrentSceneName().equals("dungeon_hub"))
            return obj_id.NULL_ID;
        obj_id building = cw.containsKey("building_id") ? cw.getObjId("building_id") : obj_id.NULL_ID;
        if (isIdValid(building) && exists(building))
            return building;

        location hubLoc = computeHubSlot(guildId);
        building = createObject("object/building/hub/space_station.iff", hubLoc);
        if (!isIdValid(building))
        {
            CustomerServiceLog("guild_space_station", "ensureStationBuildingOnHub: createObject failed guild " + guildId + " player " + player);
            return obj_id.NULL_ID;
        }
        applyClusterWideToBuilding(building, guildId, cw);
        syncBuildingPermissions(building, guildId);
        obj_id marker = hasObjVar(building, OV_ORBIT_MARKER) ? getObjIdObjVar(building, OV_ORBIT_MARKER) : obj_id.NULL_ID;
        dictionary snap = buildCwSnapshot(building, guildId, marker);
        replaceClusterWideData(CW_MANAGER, cwElementName(guildId), snap, true, -1);
        return building;
    }

    /**
     * Completes comlink flow after cross-scene transfer: player arrived on dungeon_hub and ClusterWideData arrived on the player.
     * @return true if this response was consumed (caller should return SCRIPT_CONTINUE)
     */
    public static boolean handlePendingComlinkClusterResponse(obj_id player, String manage_name, dictionary[] data, int lock_key) throws InterruptedException
    {
        if (!manage_name.equals(CW_MANAGER) || !hasObjVar(player, OV_PENDING_COMLINK_WARP))
            return false;
        removeObjVar(player, OV_PENDING_COMLINK_WARP);
        if (data != null && data.length > 0 && data[0] != null)
        {
            warpPlayerToStation(player, data[0]);
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("[Navicomputer] No guild station registration found."));
        }
        if (lock_key != 0)
        {
            releaseClusterWideDataLock(manage_name, lock_key);
        }
        return true;
    }

    /** ClusterWideData row for this guild. */
    public static void warpPlayerToStation(obj_id player, dictionary cw) throws InterruptedException
    {
        if (cw == null)
            return;
        int guildId = cw.getInt("guild_id");

        if (!getCurrentSceneName().equals("dungeon_hub"))
        {
            if (!playerCanEnterStation(player, guildId, cw))
            {
                sendSystemMessage(player, string_id.unlocalized("[Navicomputer] You are not cleared for this station."));
                return;
            }
            preparePlayerForGuildStationTravel(player);
            setObjVar(player, OV_PENDING_COMLINK_WARP, guildId);
            location hubLoc = computeHubSlot(guildId);
            warpPlayer(player, "dungeon_hub", hubLoc.x, hubLoc.y, hubLoc.z, null, hubLoc.x, hubLoc.y, hubLoc.z, "msgGuildStationComlinkTravelComplete", true);
            return;
        }

        obj_id building = ensureStationBuildingOnHub(player, guildId, cw);
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
        preparePlayerForGuildStationTravel(player);
        // forceLoadScreen true: required when coming from another scene/process so the client loads the interior cell (same pattern as space_dungeon.moveSinglePlayerIntoDungeon).
        warpPlayer(player, bLoc.area, bLoc.x, bLoc.y, bLoc.z, building, "hangarbay1", 5.0f, 0.0f, 5.0f, "", true);
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
        if (data != null && data.length > 0 && data[0] != null && data[0].containsKey("guild_id"))
        {
            sendSystemMessage(player, string_id.unlocalized("Your guild already has a registered space station."));
            releaseClusterWideDataLock(manage_name, lock_key);
            return;
        }

        int nextMaint = getCalendarTime() + MAINTENANCE_PERIOD_SEC;
        obj_id orbitMarker = spawnOrbitMarkerForPlanet(player, guildId, planet, ox, oz, obj_id.NULL_ID);

        if (!getCurrentSceneName().equals("dungeon_hub"))
        {
            dictionary snap = buildCwSnapshotWithoutBuilding(guildId, planet, ox, oz, orbitMarker, nextMaint);
            replaceClusterWideData(CW_MANAGER, el, snap, true, lock_key);
            releaseClusterWideDataLock(manage_name, lock_key);

            grantComlink(player, guildId);
            markGuildTerminalPurchaseCached(terminal, guildId);
            sendSystemMessage(player, string_id.unlocalized("[Guild] Guild space station registered. Visit the hub sector to materialize the station, or use your comlink while there."));
            CustomerServiceLog("guild_space_station", "Guild " + guildId + " purchased station (hub building deferred) leader " + player);
            return;
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
        setObjVar(building, OV_MAINTENANCE_NEXT, nextMaint);

        if (isIdValid(orbitMarker))
            setObjVar(building, OV_ORBIT_MARKER, orbitMarker);

        syncBuildingPermissions(building, guildId);

        dictionary snap = buildCwSnapshot(building, guildId, orbitMarker);
        replaceClusterWideData(CW_MANAGER, el, snap, true, lock_key);
        releaseClusterWideDataLock(manage_name, lock_key);

        grantComlink(player, guildId);
        markGuildTerminalPurchaseCached(terminal, guildId);
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
