package script.systems.city;

import script.*;
import script.library.*;

/**
 * Player script that handles terrain painting commands.
 * Attached temporarily when player is painting terrain.
 */
public class city_terrain_painter extends script.base_script
{
    public city_terrain_painter()
    {
    }

    public static final String TERRAIN_VAR_ROOT = "city.terrain";
    public static final String ROAD_MARKER_VAR = "city.terrain.road_marker";

    public static final String[] TERRAIN_SHADERS = {
        "terrain/tatooine_ground.sht",
        "terrain/naboo_grass.sht",
        "terrain/corellia_dirt.sht",
        "terrain/dathomir_rock.sht",
        "terrain/endor_forest.sht",
        "terrain/cobblestone_road.sht",
        "terrain/duracrete_floor.sht",
        "terrain/gravel_path.sht",
        "terrain/metal_plating.sht",
        "terrain/sand_packed.sht"
    };

    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        if (!hasObjVar(self, "terrain.awaiting_paint"))
        {
            detachScript(self, "systems.city.city_terrain_painter");
            return SCRIPT_CONTINUE;
        }

        if (!text.toLowerCase().startsWith("/painthere"))
        {
            return SCRIPT_CONTINUE;
        }

        location loc = getLocation(self);
        String paintMode = getStringObjVar(self, "terrain.paint_mode");
        int cityId = utils.getIntScriptVar(self, "terrain.city_id");
        String shader = utils.getStringScriptVar(self, "terrain.shader");
        String shaderName = utils.getStringScriptVar(self, "terrain.shader_name");

        if (paintMode == null)
        {
            cleanupPainting(self);
            return SCRIPT_CONTINUE;
        }

        if (paintMode.equals("RADIUS"))
        {
            int radius = utils.getIntScriptVar(self, "terrain.radius");
            applyRadiusPaint(self, cityId, loc, radius, shader, shaderName);
            cleanupPainting(self);
        }
        else if (paintMode.equals("ROAD_FIRST"))
        {
            setObjVar(self, ROAD_MARKER_VAR + ".first_x", loc.x);
            setObjVar(self, ROAD_MARKER_VAR + ".first_z", loc.z);
            setObjVar(self, "terrain.paint_mode", "ROAD_SECOND");

            sendSystemMessage(self, new string_id("city/city", "first_marker_set"));
            sendSystemMessage(self, new string_id("city/city", "walk_to_second_marker"));

            playClientEffectLoc(self, "clienteffect/waypoint_activate.cef", loc, 0);
        }
        else if (paintMode.equals("ROAD_SECOND"))
        {
            float startX = getFloatObjVar(self, ROAD_MARKER_VAR + ".first_x");
            float startZ = getFloatObjVar(self, ROAD_MARKER_VAR + ".first_z");
            int width = utils.getIntScriptVar(self, "terrain.road_width");

            applyRoadPaint(self, cityId, startX, startZ, loc.x, loc.z, width, shader, shaderName);
            cleanupPainting(self);
        }

        return SCRIPT_CONTINUE;
    }

    private void cleanupPainting(obj_id player) throws InterruptedException
    {
        removeObjVar(player, "terrain.awaiting_paint");
        removeObjVar(player, "terrain.paint_mode");
        removeObjVar(player, ROAD_MARKER_VAR);
        utils.removeScriptVar(player, "terrain.city_id");
        utils.removeScriptVar(player, "terrain.shader");
        utils.removeScriptVar(player, "terrain.shader_name");
        utils.removeScriptVar(player, "terrain.radius");
        utils.removeScriptVar(player, "terrain.road_width");
        utils.removeScriptVar(player, "terrain.max_radius");

        detachScript(player, "systems.city.city_terrain_painter");
    }

    public void applyRadiusPaint(obj_id player, int cityId, location center, int radius, String shader, String shaderName) throws InterruptedException
    {
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        float dist = utils.getDistance2D(center, cityLoc);
        if (dist + radius > cityRadius)
        {
            sendSystemMessage(player, new string_id("city/city", "paint_outside_city"));
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);

        String regionId = "R" + System.currentTimeMillis();

        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type", "RADIUS");
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_x", center.x);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_z", center.z);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".radius", radius);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader", shader);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader_name", shaderName);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".created", getGameTime());

        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds == null)
        {
            regionIds = new String[0];
        }
        regionIds = utils.addElement(regionIds, regionId);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids", regionIds);

        broadcastTerrainShaderRadius(cityId, center.x, center.z, radius, shader);

        sendSystemMessage(player, new string_id("city/city", "terrain_painted_radius"));

        playClientEffectLoc(player, "clienteffect/combat_vehicle_hit.cef", center, 0);

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Terrain painted (radius). City: " + cityName + " Player: " + player + " Shader: " + shader + " Radius: " + radius);
    }

    public void applyRoadPaint(obj_id player, int cityId, float startX, float startZ, float endX, float endZ, int width, String shader, String shaderName) throws InterruptedException
    {
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        location startLoc = new location(startX, 0, startZ, cityLoc.area, null);
        location endLoc = new location(endX, 0, endZ, cityLoc.area, null);

        float distStart = utils.getDistance2D(startLoc, cityLoc);
        float distEnd = utils.getDistance2D(endLoc, cityLoc);

        if (distStart > cityRadius || distEnd > cityRadius)
        {
            sendSystemMessage(player, new string_id("city/city", "paint_outside_city"));
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);

        String regionId = "R" + System.currentTimeMillis();

        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type", "ROAD");
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_x", startX);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_z", startZ);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_x", endX);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_z", endZ);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".width", width);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader", shader);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader_name", shaderName);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".created", getGameTime());

        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds == null)
        {
            regionIds = new String[0];
        }
        regionIds = utils.addElement(regionIds, regionId);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids", regionIds);

        broadcastTerrainShaderRoad(cityId, startX, startZ, endX, endZ, width, shader);

        sendSystemMessage(player, new string_id("city/city", "terrain_painted_road"));

        playClientEffectLoc(player, "clienteffect/combat_vehicle_hit.cef", startLoc, 0);
        playClientEffectLoc(player, "clienteffect/combat_vehicle_hit.cef", endLoc, 0);

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Terrain painted (road). City: " + cityName + " Player: " + player + " Shader: " + shader + " Width: " + width);
    }

    public void broadcastTerrainShaderRadius(int cityId, float centerX, float centerZ, int radius, String shader) throws InterruptedException
    {
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        obj_id[] players = getPlayerCreaturesInRange(cityLoc, cityRadius + 100);

        if (players != null)
        {
            dictionary params = new dictionary();
            params.put("type", "RADIUS");
            params.put("cityId", cityId);
            params.put("centerX", centerX);
            params.put("centerZ", centerZ);
            params.put("radius", radius);
            params.put("shader", shader);

            for (obj_id player : players)
            {
                if (isIdValid(player))
                {
                    messageTo(player, "handleTerrainUpdate", params, 0, false);
                }
            }
        }
    }

    public void broadcastTerrainShaderRoad(int cityId, float startX, float startZ, float endX, float endZ, int width, String shader) throws InterruptedException
    {
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        obj_id[] players = getPlayerCreaturesInRange(cityLoc, cityRadius + 100);

        if (players != null)
        {
            dictionary params = new dictionary();
            params.put("type", "ROAD");
            params.put("cityId", cityId);
            params.put("startX", startX);
            params.put("startZ", startZ);
            params.put("endX", endX);
            params.put("endZ", endZ);
            params.put("width", width);
            params.put("shader", shader);

            for (obj_id player : players)
            {
                if (isIdValid(player))
                {
                    messageTo(player, "handleTerrainUpdate", params, 0, false);
                }
            }
        }
    }
}

