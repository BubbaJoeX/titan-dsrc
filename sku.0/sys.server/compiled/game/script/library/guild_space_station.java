package script.library;

import script.dictionary;
import script.location;
import script.obj_id;
import script.string_id;
import java.util.Vector;

import script.library.callable;
import script.library.space_transition;
import script.library.space_utils;
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
    /** Player scriptvar: pending getClusterWideData("guild_*") to respawn hub buildings / orbit markers after restart. */
    public static final String SV_BOOTSTRAP_PENDING = "guildStation.bootstrapCwPending";

    /** Player objvar: POB ship to return to from the guild station hangar (airlock). */
    public static final String OV_GUILD_RETURN_SHIP = "guildStation.returnShip";
    /** Ship scriptvar: ClusterWide row while guild members confirm POB station docking. */
    public static final String SV_LANDING_CW = "guildStation.landingCw";
    /** Ship scriptvar: number of guild members who have not yet answered the docking prompt. */
    public static final String SV_LANDING_REMAINING = "guildStation.landingRemaining";

    private static final String TEMPLATE_ORBIT_SHIP = "object/ship/spacestation_neutral.iff";

    public static String cwElementName(int guildId)
    {
        return "guild_" + guildId;
    }

    /**
     * Dismount, store callables, pack surface ship before station travel.
     * In space or atmospheric flight, only {@link #unpilotShip} — do not call {@link space_transition#packShip} before
     * {@code warpPlayer}: packing then cross-scene warp has left ships stuck, unpackable, or SCD-desynced. Ground players
     * still get {@code packShip} for their deployed ship.
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
     * Orbit beacon: invulnerable, conversable, display name {@code Station: [abbrev]} (no angle brackets — client comm UI). Call from marker attach/init
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
        // Avoid '<'/'>' in names — client/UI can treat them as markup and break hail/comm on ships.
        setName(marker, "Station: [" + guildGetAbbrev(guildId) + "]");
    }

    /**
     * Destroys known orbit beacon objects before placing a new one (CW row and building objvar can reference different ids).
     * Call with ids from registration; each id is destroyed at most once.
     */
    public static void destroyOrbitMarkersForGuildMove(obj_id markerFromCw, obj_id markerFromBuilding) throws InterruptedException
    {
        if (isIdValid(markerFromCw) && exists(markerFromCw))
            destroyObject(markerFromCw);
        if (isIdValid(markerFromBuilding) && exists(markerFromBuilding))
            destroyObject(markerFromBuilding);
    }

    /**
     * Spawns the atmospheric "orbit" prop over the given point. Must be invoked on a server running {@code planet}'s scene.
     */
    public static obj_id spawnOrbitMarkerForPlanet(obj_id player, int guildId, String planet, float x, float z, obj_id destroyOld) throws InterruptedException
    {
        if (!getCurrentSceneName().equals(planet))
        {
            if (isIdValid(player) && exists(player))
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
        obj_id building = cw.containsKey("building_id") ? cw.getObjId("building_id") : obj_id.NULL_ID;
        obj_id oldFromCw = cw.containsKey("orbit_marker_id") ? cw.getObjId("orbit_marker_id") : obj_id.NULL_ID;
        obj_id oldFromBuilding = obj_id.NULL_ID;
        if (isIdValid(building) && exists(building) && hasObjVar(building, OV_ORBIT_MARKER))
            oldFromBuilding = getObjIdObjVar(building, OV_ORBIT_MARKER);
        destroyOrbitMarkersForGuildMove(oldFromCw, oldFromBuilding);
        obj_id newMarker = spawnOrbitMarkerForPlanet(player, guildId, planet, ox, oz, obj_id.NULL_ID);
        if (!isIdValid(newMarker))
            return;

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

    /** Remember POB for airlock return from the station hangar (cleared when used or invalid). */
    private static void registerGuildStationReturnShip(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
            return;
        obj_id ship = space_transition.getContainingShip(player);
        if (space_utils.isShipWithInterior(ship))
            setObjVar(player, OV_GUILD_RETURN_SHIP, ship);
    }

    /**
     * Orbit beacon: after ClusterWide resolves, either prompt guild members in a POB or warp immediately.
     * @return true if landing was deferred (SUIs); false if caller should invoke {@link #warpPlayerToStation} immediately.
     */
    public static boolean startPobGuildLandingIfNeeded(obj_id player, dictionary cw, obj_id orbitMarker) throws InterruptedException
    {
        if (cw == null || !isIdValid(player) || !exists(player))
            return false;
        int guildId = cw.getInt("guild_id");
        obj_id ship = space_transition.getContainingShip(player);
        if (!isIdValid(ship) || !exists(ship) || !space_utils.isShipWithInterior(ship))
            return false;
        Vector all = space_transition.getContainedPlayers(ship, null);
        if (all == null || all.size() < 1)
            return false;
        Vector guildMembers = new Vector();
        for (int i = 0; i < all.size(); ++i)
        {
            obj_id p = (obj_id)all.get(i);
            if (isIdValid(p) && exists(p) && getGuildId(p) == guildId)
                guildMembers.add(p);
        }
        for (int i = 0; i < all.size(); ++i)
        {
            obj_id p = (obj_id)all.get(i);
            if (isIdValid(p) && exists(p) && getGuildId(p) != guildId)
                sendSystemMessage(p, string_id.unlocalized("You remain aboard while guild members handle the station visit."));
        }
        if (guildMembers.size() <= 1)
            return false;
        utils.setScriptVar(ship, SV_LANDING_CW, cw);
        utils.setScriptVar(ship, SV_LANDING_REMAINING, guildMembers.size());
        String prompt = "Your guild station has cleared this ship for docking.\n\nExit to the station interior?";
        for (int i = 0; i < guildMembers.size(); ++i)
        {
            obj_id member = (obj_id)guildMembers.get(i);
            if (isIdValid(member) && exists(member))
                sui.msgbox(member, member, prompt, sui.YES_NO, "Station docking", sui.MSG_QUESTION, "handleGuildStationPobLandingOptIn");
        }
        if (isIdValid(orbitMarker) && exists(orbitMarker))
        {
            dictionary to = new dictionary();
            to.put("ship", ship);
            messageTo(orbitMarker, "guildPobLandingTimeout", to, 120.0f, false);
        }
        return true;
    }

    public static void clearPobLandingState(obj_id ship) throws InterruptedException
    {
        if (!isIdValid(ship))
            return;
        utils.removeScriptVar(ship, SV_LANDING_CW);
        utils.removeScriptVar(ship, SV_LANDING_REMAINING);
    }

    /** Called from {@code base_player.handleGuildStationPobLandingOptIn}. */
    public static void onGuildLandingOptInFromPlayer(obj_id player, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        obj_id ship = space_transition.getContainingShip(player);
        if (!isIdValid(ship) || !exists(ship))
            return;
        if (!utils.hasScriptVar(ship, SV_LANDING_REMAINING))
        {
            sendSystemMessage(player, string_id.unlocalized("That docking request has expired or already completed."));
            return;
        }
        dictionary cw = utils.getDictionaryScriptVar(ship, SV_LANDING_CW);
        if (cw == null)
        {
            clearPobLandingState(ship);
            return;
        }
        int remaining = utils.getIntScriptVar(ship, SV_LANDING_REMAINING);
        remaining--;
        utils.setScriptVar(ship, SV_LANDING_REMAINING, remaining);
        if (bp == sui.BP_OK)
        {
            warpPlayerToStation(player, cw);
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("You remain aboard the vessel."));
        }
        if (remaining <= 0)
            clearPobLandingState(ship);
    }

    /**
     * Return from guild station hangar to the interior of the ship that was left in space (airlock).
     */
    public static void warpPlayerReturnToShipFromStation(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
            return;
        if (!hasObjVar(player, OV_GUILD_RETURN_SHIP))
        {
            sendSystemMessage(player, string_id.unlocalized("No vessel is registered for airlock return. Land from a player ship with an interior to use this."));
            return;
        }
        obj_id ship = getObjIdObjVar(player, OV_GUILD_RETURN_SHIP);
        if (!isIdValid(ship) || !exists(ship))
        {
            sendSystemMessage(player, string_id.unlocalized("Your vessel is no longer in the sector."));
            removeObjVar(player, OV_GUILD_RETURN_SHIP);
            return;
        }
        if (!space_utils.isShipWithInterior(ship))
        {
            removeObjVar(player, OV_GUILD_RETURN_SHIP);
            return;
        }
        location shipLoc = getLocation(ship);
        if (shipLoc == null || shipLoc.area == null || shipLoc.area.length() < 1)
        {
            sendSystemMessage(player, string_id.unlocalized("Unable to locate your vessel."));
            return;
        }
        location dest = space_transition.getShipBoardingDestination(ship);
        if (dest == null)
        {
            sendSystemMessage(player, string_id.unlocalized("Unable to find an airlock entry point on your vessel."));
            return;
        }
        String[] cellNames = getCellNames(ship);
        if (cellNames != null)
        {
            String boarderName = getFirstName(player);
            for (String cellName : cellNames)
            {
                obj_id cellId = getCellId(ship, cellName);
                if (isIdValid(cellId))
                {
                    permissionsAddAllowed(cellId, boarderName);
                    sendDirtyCellPermissionsUpdate(cellId, player, true);
                }
            }
        }
        removeObjVar(player, OV_GUILD_RETURN_SHIP);
        warpPlayer(player, shipLoc.area, shipLoc.x, shipLoc.y, shipLoc.z, dest.cell, dest.x, dest.y, dest.z, "", true);
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

    /**
     * Orbit beacon landing after ClusterWide row is available: multi-guild POB prompts opt-in; otherwise warps immediately.
     */
    public static void handleOrbitLandingClusterResponse(obj_id player, dictionary cw, obj_id orbitMarker) throws InterruptedException
    {
        if (cw == null || player == null)
            return;
        if (startPobGuildLandingIfNeeded(player, cw, orbitMarker))
            return;
        warpPlayerToStation(player, cw);
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
            registerGuildStationReturnShip(player);
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
        registerGuildStationReturnShip(player);
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

    public static String accessModeLabel(int mode)
    {
        switch (mode)
        {
            case ACCESS_GUILD:
                return "All guild members";
            case ACCESS_RANK:
                return "Minimum rank";
            case ACCESS_WHITELIST:
                return "Whitelist only";
            default:
                return "Unknown";
        }
    }

    public static String describeAccessPolicy(obj_id building) throws InterruptedException
    {
        int mode = hasObjVar(building, OV_ACCESS_MODE) ? getIntObjVar(building, OV_ACCESS_MODE) : ACCESS_GUILD;
        StringBuilder sb = new StringBuilder();
        sb.append("Mode: ").append(accessModeLabel(mode)).append("\n");
        if (mode == ACCESS_RANK && hasObjVar(building, OV_ACCESS_MIN_RANK))
            sb.append("Minimum rank: ").append(getStringObjVar(building, OV_ACCESS_MIN_RANK)).append("\n");
        if (mode == ACCESS_WHITELIST)
        {
            String packed = hasObjVar(building, OV_ACCESS_WHITELIST) ? getStringObjVar(building, OV_ACCESS_WHITELIST) : "";
            sb.append("Whitelist entries: ").append(countWhitelistEntries(packed)).append("\n");
        }
        return sb.toString();
    }

    public static int countWhitelistEntries(String packed)
    {
        if (packed == null || packed.length() < 1)
            return 0;
        String[] parts = split(packed, ':');
        int c = 0;
        for (String p : parts)
        {
            if (p != null && p.trim().length() > 0)
                c++;
        }
        return c;
    }

    public static String whitelistAddPacked(String packed, obj_id pid) throws InterruptedException
    {
        if (!isIdValid(pid))
            return packed != null ? packed : "";
        String needle = Long.toString(pid.getValue());
        if (packed == null || packed.length() < 1)
            return needle;
        String[] parts = split(packed, ':');
        for (String p : parts)
        {
            if (p != null && p.trim().equals(needle))
                return packed;
        }
        return packed + ":" + needle;
    }

    public static String whitelistRemovePacked(String packed, obj_id pid) throws InterruptedException
    {
        if (packed == null || packed.length() < 1 || !isIdValid(pid))
            return packed != null ? packed : "";
        String needle = Long.toString(pid.getValue());
        String[] parts = split(packed, ':');
        StringBuilder out = new StringBuilder();
        for (String p : parts)
        {
            if (p == null || p.trim().length() < 1)
                continue;
            if (p.trim().equals(needle))
                continue;
            if (out.length() > 0)
                out.append(':');
            out.append(p.trim());
        }
        return out.toString();
    }

    public static obj_id[] parseWhitelistObjIds(String packed) throws InterruptedException
    {
        if (packed == null || packed.length() < 1)
            return new obj_id[0];
        String[] parts = split(packed, ':');
        Vector v = new Vector();
        for (String p : parts)
        {
            if (p == null || p.trim().length() < 1)
                continue;
            try
            {
                long lid = Long.parseLong(p.trim());
                obj_id oid = obj_id.getObjId(lid);
                if (isIdValid(oid))
                    v.add(oid);
            }
            catch (NumberFormatException ignore)
            {
            }
        }
        obj_id[] out = new obj_id[v.size()];
        v.copyInto(out);
        return out;
    }

    /** Multi-line summary for management terminal / msgbox. */
    public static String formatStationStatusSummary(obj_id building, int guildId) throws InterruptedException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Guild: ").append(guildGetName(guildId)).append(" <").append(guildGetAbbrev(guildId)).append(">\n");
        sb.append("Registered members: ").append(guildGetCountMembersOnly(guildId)).append("\n");
        if (hasObjVar(building, OV_MAINTENANCE_NEXT))
        {
            int t = getIntObjVar(building, OV_MAINTENANCE_NEXT);
            sb.append("Next maintenance due: ").append(getCalendarTimeStringLocal(t)).append("\n");
            if (getCalendarTime() >= t)
                sb.append("Billing status: OVERDUE\n");
            else
                sb.append("Billing status: current\n");
        }
        else
            sb.append("Maintenance: not scheduled — pay to set the next cycle.\n");
        sb.append(describeAccessPolicy(building));
        if (hasObjVar(building, OV_ORBIT_PLANET))
        {
            sb.append("Orbit planet: ").append(getStringObjVar(building, OV_ORBIT_PLANET));
            sb.append("\nSurface X/Z: ").append(getFloatObjVar(building, OV_ORBIT_X)).append(", ").append(getFloatObjVar(building, OV_ORBIT_Z)).append("\n");
            obj_id m = hasObjVar(building, OV_ORBIT_MARKER) ? getObjIdObjVar(building, OV_ORBIT_MARKER) : obj_id.NULL_ID;
            sb.append("Beacon marker: ").append(isIdValid(m) && exists(m) ? "present" : "missing or offline").append("\n");
        }
        else
            sb.append("Orbit beacon: not registered.\n");
        return sb.toString();
    }

    public static String formatOrbitBeaconDetails(obj_id building) throws InterruptedException
    {
        if (!hasObjVar(building, OV_ORBIT_PLANET))
            return "No orbit beacon is registered for this station.";
        StringBuilder sb = new StringBuilder();
        sb.append("Planet: ").append(getStringObjVar(building, OV_ORBIT_PLANET));
        sb.append("\nGround X: ").append(getFloatObjVar(building, OV_ORBIT_X));
        sb.append("\nGround Z: ").append(getFloatObjVar(building, OV_ORBIT_Z));
        sb.append("\nAltitude over terrain: ").append(ORBIT_HEIGHT_M).append(" m (fixed).");
        obj_id m = hasObjVar(building, OV_ORBIT_MARKER) ? getObjIdObjVar(building, OV_ORBIT_MARKER) : obj_id.NULL_ID;
        sb.append("\nMarker object: ").append(isIdValid(m) && exists(m) ? "active" : "not found in scene");
        return sb.toString();
    }

    /**
     * Queues a cluster-wide read of all guild station rows; {@link #handleBootstrapClusterResponse} respawns missing
     * dungeon_hub buildings and planetary orbit markers (obj_ids in CW do not survive process restart).
     */
    public static void requestGuildStationSceneBootstrap(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
            return;
        utils.setScriptVar(player, SV_BOOTSTRAP_PENDING, true);
        getClusterWideData(CW_MANAGER, "guild_*", false, player);
    }

    /**
     * @return true if this was a bootstrap response (consumes pending flag; releases lock when non-zero).
     */
    public static boolean handleBootstrapClusterResponse(obj_id player, String manage_name, dictionary[] data, int lock_key) throws InterruptedException
    {
        if (!manage_name.equals(CW_MANAGER) || !utils.hasScriptVar(player, SV_BOOTSTRAP_PENDING))
            return false;
        utils.removeScriptVar(player, SV_BOOTSTRAP_PENDING);
        try
        {
            if (data != null)
            {
                for (int i = 0; i < data.length; ++i)
                {
                    dictionary row = data[i];
                    if (row == null || !row.containsKey("guild_id"))
                        continue;
                    int guildId = row.getInt("guild_id");
                    if (guildId <= 0)
                        continue;
                    if (getCurrentSceneName().equals("dungeon_hub"))
                        ensureStationBuildingOnHub(player, guildId, row);
                    if (row.containsKey("orbit_planet") && getCurrentSceneName().equals(row.getString("orbit_planet")))
                        ensureOrbitMarkerFromClusterRow(row, guildId);
                }
            }
        }
        finally
        {
            if (lock_key != 0)
                releaseClusterWideDataLock(manage_name, lock_key);
        }
        return true;
    }

    /**
     * If CW lists an orbit marker that no longer exists (new process), spawn one and update CW / local building objvars.
     */
    public static void ensureOrbitMarkerFromClusterRow(dictionary cw, int guildId) throws InterruptedException
    {
        if (cw == null || !cw.containsKey("orbit_planet") || !cw.containsKey("orbit_x") || !cw.containsKey("orbit_z"))
            return;
        String planet = cw.getString("orbit_planet");
        if (!getCurrentSceneName().equals(planet))
            return;
        obj_id markerId = cw.containsKey("orbit_marker_id") ? cw.getObjId("orbit_marker_id") : obj_id.NULL_ID;
        if (isIdValid(markerId) && exists(markerId))
            return;
        float ox = cw.getFloat("orbit_x");
        float oz = cw.getFloat("orbit_z");
        obj_id newMarker = spawnOrbitMarkerForPlanet(null, guildId, planet, ox, oz, obj_id.NULL_ID);
        if (!isIdValid(newMarker))
            return;
        cw.put("orbit_marker_id", newMarker);
        obj_id building = cw.containsKey("building_id") ? cw.getObjId("building_id") : obj_id.NULL_ID;
        if (!isIdValid(building) || !exists(building))
        {
            cw.remove("building_id");
            building = obj_id.NULL_ID;
        }
        if (isIdValid(building) && exists(building) && hasObjVar(building, OV_GUILD_ID))
            setObjVar(building, OV_ORBIT_MARKER, newMarker);
        replaceClusterWideData(CW_MANAGER, cwElementName(guildId), cw, true, -1);
    }
}
