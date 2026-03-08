package script.systems.city;

import script.*;
import script.library.*;

/**
 * Attached to players when they enter a city to sync terrain data.
 */
public class player_city_terrain_sync extends script.base_script
{
    public player_city_terrain_sync()
    {
    }

    public static final String TERRAIN_VAR_ROOT = "city.terrain";

    /**
     * Called when player enters a city region
     */
    public int OnEnteredCityRegion(obj_id self, int cityId) throws InterruptedException
    {
        syncTerrainForCity(self, cityId);
        return SCRIPT_CONTINUE;
    }

    /**
     * Called when player leaves a city region
     */
    public int OnLeftCityRegion(obj_id self, int cityId) throws InterruptedException
    {
        sendTerrainClearToClient(self, cityId);
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
        if (regionIds != null && regionIds.length > 0)
        {
            // Send each region to the client
            for (String regionId : regionIds)
            {
                sendRegionToClient(player, cityId, cityHall, regionId);
            }
        }

        // Check for bulldozed state
        if (hasObjVar(cityHall, "city.bulldozed"))
        {
            float bulldozedHeight = getFloatObjVar(cityHall, "city.bulldozed_height");
            location cityLoc = cityGetLocation(cityId);
            int cityRadius = cityGetRadius(cityId);

            city_terrain_handler.sendTerrainModifyToClient(player, cityId, 2, "bulldoze_" + cityId, "",
                                       (float)cityLoc.x, (float)cityLoc.z, (float)cityRadius, 0, 0, 0, bulldozedHeight, 20.0f);
        }
    }

    /**
     * Send a single region to the client
     */
    private void sendRegionToClient(obj_id player, int cityId, obj_id cityHall, String regionId) throws InterruptedException
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

        // Handle ROAD type - start_x/z are stored as center_x/z in ROAD type
        if (type != null && type.equals("ROAD"))
        {
            centerX = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_x");
            centerZ = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_z");
        }

        city_terrain_handler.sendTerrainModifyToClient(player, cityId, modType, regionId,
                                   shader != null ? shader : "", centerX, centerZ, radius,
                                   endX, endZ, width, height, blendDist);
    }

    /**
     * Send terrain clear message to client
     */
    public void sendTerrainClearToClient(obj_id player, int cityId) throws InterruptedException
    {
        city_terrain_handler.sendTerrainModifyToClient(player, cityId, 4, "", "", 0, 0, 0, 0, 0, 0, 0, 0);
    }
}

