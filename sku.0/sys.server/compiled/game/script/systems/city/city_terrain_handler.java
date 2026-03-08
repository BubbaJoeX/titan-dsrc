package script.systems.city;

import script.*;
import script.library.*;

/**
 * Player-side handler for city terrain updates.
 * Attached to players to handle terrain sync when entering cities.
 */
public class city_terrain_handler extends script.base_script
{
    public city_terrain_handler()
    {
    }

    public static final String TERRAIN_VAR_ROOT = "city.terrain";

    /**
     * Called when player enters a city region
     */
    public int OnEnteredCity(obj_id self, int cityId) throws InterruptedException
    {
        syncTerrainForCity(self, cityId);
        return SCRIPT_CONTINUE;
    }

    /**
     * Called when player leaves a city region
     */
    public int OnLeftCity(obj_id self, int cityId) throws InterruptedException
    {
        clearTerrainForCity(self, cityId);
        return SCRIPT_CONTINUE;
    }

    /**
     * Sync all terrain modifications for a city to the client
     */
    public void syncTerrainForCity(obj_id player, int cityId) throws InterruptedException
    {
        if (!cityExists(cityId))
        {
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);
        if (!isIdValid(cityHall))
        {
            return;
        }

        // Get all terrain regions for this city
        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds == null || regionIds.length == 0)
        {
            return;
        }

        // Send each region to the client
        for (String regionId : regionIds)
        {
            String type = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type");
            String shader = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader");

            int modType = 0;
            if (type != null)
            {
                if (type.equals("RADIUS"))
                {
                    modType = 0;
                }
                else if (type.equals("ROAD"))
                {
                    modType = 1;
                }
                else if (type.equals("FLATTEN"))
                {
                    modType = 2;
                }
            }

            float centerX = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_x");
            float centerZ = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_z");
            float radius = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".radius");
            float endX = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_x");
            float endZ = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_z");
            float width = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".width");
            float height = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".height");
            float blendDist = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".blend_dist");

            // Send terrain modify message to client
            sendTerrainModifyToClient(player, cityId, modType, regionId, shader,
                                       centerX, centerZ, radius, endX, endZ, width, height, blendDist);
        }

        // Check for bulldozed state
        if (hasObjVar(cityHall, "city.bulldozed"))
        {
            float bulldozedHeight = getFloatObjVar(cityHall, "city.bulldozed_height");
            location cityLoc = cityGetLocation(cityId);
            int cityRadius = cityGetRadius(cityId);

            sendTerrainModifyToClient(player, cityId, 2, "bulldoze_" + cityId, "",
                                       cityLoc.x, cityLoc.z, (float)cityRadius, 0, 0, 0, bulldozedHeight, 20.0f);
        }
    }

    /**
     * Clear terrain modifications for a city from the client
     */
    public void clearTerrainForCity(obj_id player, int cityId) throws InterruptedException
    {
        // Send clear all message for this city
        sendTerrainClearToClient(player, cityId);
    }

    /**
     * Send terrain modification to client via controller message
     */
    public static void sendTerrainModifyToClient(obj_id player, int cityId, int modType, String regionId,
                                                  String shader, float centerX, float centerZ, float radius,
                                                  float endX, float endZ, float width, float height,
                                                  float blendDist) throws InterruptedException
    {
        // Use controller message to send to client
        // The client's CityTerrainLayerManager will handle this
        dictionary params = new dictionary();
        params.put("cityId", cityId);
        params.put("modType", modType);
        params.put("regionId", regionId);
        params.put("shader", shader != null ? shader : "");
        params.put("centerX", centerX);
        params.put("centerZ", centerZ);
        params.put("radius", radius);
        params.put("endX", endX);
        params.put("endZ", endZ);
        params.put("width", width);
        params.put("height", height);
        params.put("blendDist", blendDist);

        // Send as cmdSceneMsg for client handling
        sendSystemMessageProse(player, packTerrainMessage(params));
    }

    /**
     * Send terrain clear message to client
     */
    public static void sendTerrainClearToClient(obj_id player, int cityId) throws InterruptedException
    {
        dictionary params = new dictionary();
        params.put("cityId", cityId);
        params.put("modType", 4); // MT_CLEAR_ALL
        params.put("regionId", "");
        params.put("shader", "");
        params.put("centerX", 0.0f);
        params.put("centerZ", 0.0f);
        params.put("radius", 0.0f);
        params.put("endX", 0.0f);
        params.put("endZ", 0.0f);
        params.put("width", 0.0f);
        params.put("height", 0.0f);
        params.put("blendDist", 0.0f);

        sendSystemMessageProse(player, packTerrainMessage(params));
    }

    /**
     * Pack terrain message into prose_package for client
     */
    private static prose_package packTerrainMessage(dictionary params) throws InterruptedException
    {
        prose_package pp = new prose_package();
        pp.stringId = new string_id("terrain", "terrain_modify");
        pp.actor.set(params.getString("regionId"));
        pp.target.set(params.getString("shader"));

        // Encode numeric values in other prose slots
        String encoded = params.getInt("cityId") + "," +
                         params.getInt("modType") + "," +
                         params.getFloat("centerX") + "," +
                         params.getFloat("centerZ") + "," +
                         params.getFloat("radius") + "," +
                         params.getFloat("endX") + "," +
                         params.getFloat("endZ") + "," +
                         params.getFloat("width") + "," +
                         params.getFloat("height") + "," +
                         params.getFloat("blendDist");
        pp.other.set(encoded);

        return pp;
    }

    /**
     * Request shader list from server for UI
     */
    public int requestShaderList(obj_id self, dictionary params) throws InterruptedException
    {
        // Server will enumerate available shaders and send to client
        // For now, we use a predefined list from the planet's terrain data

        String planet = getCurrentSceneName();
        String[] shaders = getTerrainShadersForPlanet(planet);
        String[] names = getTerrainShaderNamesForPlanet(planet);

        // Store in script var for UI to access
        utils.setScriptVar(self, "terrain.available_shaders", shaders);
        utils.setScriptVar(self, "terrain.available_shader_names", names);

        return SCRIPT_CONTINUE;
    }

    /**
     * Get terrain shaders available for a planet
     */
    public static String[] getTerrainShadersForPlanet(String planet) throws InterruptedException
    {
        // These are dynamically loaded based on the planet's terrain generator
        // For now, return a comprehensive list of common shaders
        return new String[] {
            "terrain/" + planet + "_ground.sht",
            "terrain/dirt.sht",
            "terrain/grass.sht",
            "terrain/rock.sht",
            "terrain/sand.sht",
            "terrain/cobblestone.sht",
            "terrain/duracrete.sht",
            "terrain/metal_floor.sht",
            "terrain/gravel.sht",
            "terrain/mud.sht",
            "terrain/snow.sht",
            "terrain/lava.sht",
            "terrain/water_shallow.sht",
            "terrain/swamp.sht",
            "terrain/forest_floor.sht"
        };
    }

    /**
     * Get terrain shader display names for a planet
     */
    public static String[] getTerrainShaderNamesForPlanet(String planet) throws InterruptedException
    {
        return new String[] {
            "Native Ground",
            "Dirt",
            "Grass",
            "Rock",
            "Sand",
            "Cobblestone",
            "Duracrete",
            "Metal Floor",
            "Gravel",
            "Mud",
            "Snow",
            "Lava",
            "Shallow Water",
            "Swamp",
            "Forest Floor"
        };
    }

    // ========================================================================
    // SCRIPT TRIGGERS - Called from C++ CityTerrainService
    // ========================================================================

    /**
     * Called from C++ when storing a terrain region
     * Attached to City Hall
     */
    public int OnStoreTerrainRegion(obj_id self, String regionId, int modType, String shader,
                                     float centerX, float centerZ, float radius,
                                     float endX, float endZ, float width,
                                     float height, float blendDist) throws InterruptedException
    {
        String typeStr = "RADIUS";
        if (modType == 1)
        {
            typeStr = "ROAD";
        }
        else if (modType == 2)
        {
            typeStr = "FLATTEN";
        }

        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".type", typeStr);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".shader", shader);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".center_x", centerX);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".center_z", centerZ);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".radius", radius);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".end_x", endX);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".end_z", endZ);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".width", width);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".height", height);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".blend_dist", blendDist);
        setObjVar(self, TERRAIN_VAR_ROOT + "." + regionId + ".created", getGameTime());

        // Add to region list
        String[] regionIds = getStringArrayObjVar(self, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds == null)
        {
            regionIds = new String[0];
        }
        regionIds = utils.addElement(regionIds, regionId);
        setObjVar(self, TERRAIN_VAR_ROOT + ".region_ids", regionIds);

        return SCRIPT_CONTINUE;
    }

    /**
     * Called from C++ when removing a terrain region
     * Attached to City Hall
     */
    public int OnRemoveTerrainRegion(obj_id self, String regionId) throws InterruptedException
    {
        removeObjVar(self, TERRAIN_VAR_ROOT + "." + regionId);

        // Remove from region list
        String[] regionIds = getStringArrayObjVar(self, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds != null)
        {
            String[] newRegionIds = new String[regionIds.length - 1];
            int j = 0;
            for (int i = 0; i < regionIds.length; i++)
            {
                if (!regionIds[i].equals(regionId))
                {
                    if (j < newRegionIds.length)
                    {
                        newRegionIds[j++] = regionIds[i];
                    }
                }
            }
            if (newRegionIds.length > 0)
            {
                setObjVar(self, TERRAIN_VAR_ROOT + ".region_ids", newRegionIds);
            }
            else
            {
                removeObjVar(self, TERRAIN_VAR_ROOT + ".region_ids");
            }
        }

        return SCRIPT_CONTINUE;
    }

    /**
     * Called from C++ when syncing terrain to a player
     * Attached to Player
     */
    public int OnRequestCityTerrainSync(obj_id self, int cityId) throws InterruptedException
    {
        syncTerrainForCity(self, cityId);
        return SCRIPT_CONTINUE;
    }
}
