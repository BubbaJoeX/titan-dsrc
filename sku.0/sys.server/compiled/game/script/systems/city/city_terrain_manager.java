package script.systems.city;

import script.*;
import script.library.*;
import script.systems.city.city_terrain_handler;

import java.util.Vector;

/**
 * City Terrain Manager Script
 * Handles terrain painting (radius and road), bulldozing, and persistence.
 * Attached to city hall structures.
 */
public class city_terrain_manager extends script.base_script
{
    public city_terrain_manager()
    {
    }

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    public static final String TERRAIN_VAR_ROOT = "city.terrain";
    public static final String ROAD_MARKER_VAR = "city.terrain.road_marker";

    // Shader templates available for painting
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

    public static final String[] TERRAIN_SHADER_NAMES = {
        "Desert Sand",
        "Grass",
        "Dirt",
        "Rock",
        "Forest Floor",
        "Cobblestone Road",
        "Duracrete",
        "Gravel Path",
        "Metal Plating",
        "Packed Sand"
    };

    // Radius limits based on city rank
    public static final int[] PAINT_RADIUS_MAX = {0, 10, 15, 20, 30, 40, 50};

    // Road width limits
    public static final int ROAD_WIDTH_MIN = 2;
    public static final int ROAD_WIDTH_MAX = 10;

    // Bulldoze blend distance
    public static final int BULLDOZE_BLEND_DISTANCE = 20;

    // ========================================================================
    // MENU HANDLING
    // ========================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id structure = getTopMostContainer(self);
        int cityId = findCityByCityHall(structure);

        if (!cityExists(cityId))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id mayor = cityGetLeader(cityId);

        if (player != mayor && !isGod(player))
        {
            return SCRIPT_CONTINUE;
        }

        int menu = mi.addRootMenu(menu_info_types.SERVER_MENU40, new string_id("city/city", "terrain_management"));
        mi.addSubMenu(menu, menu_info_types.SERVER_MENU41, new string_id("city/city", "paint_terrain_radius"));
        mi.addSubMenu(menu, menu_info_types.SERVER_MENU42, new string_id("city/city", "paint_terrain_road"));
        mi.addSubMenu(menu, menu_info_types.SERVER_MENU43, new string_id("city/city", "bulldoze_city"));
        mi.addSubMenu(menu, menu_info_types.SERVER_MENU44, new string_id("city/city", "view_terrain_regions"));
        mi.addSubMenu(menu, menu_info_types.SERVER_MENU45, new string_id("city/city", "remove_terrain_region"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id structure = getTopMostContainer(self);
        int cityId = findCityByCityHall(structure);

        if (!cityExists(cityId))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id mayor = cityGetLeader(cityId);

        if (player != mayor && !isGod(player))
        {
            return SCRIPT_CONTINUE;
        }

        switch (item)
        {
            case menu_info_types.SERVER_MENU41:
                showRadiusPaintUI(player, self, cityId);
                break;
            case menu_info_types.SERVER_MENU42:
                showRoadPaintUI(player, self, cityId);
                break;
            case menu_info_types.SERVER_MENU43:
                showBulldozeUI(player, self, cityId);
                break;
            case menu_info_types.SERVER_MENU44:
                showTerrainRegions(player, self, cityId);
                break;
            case menu_info_types.SERVER_MENU45:
                showRemoveRegionUI(player, self, cityId);
                break;
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // RADIUS PAINTING
    // ========================================================================

    public void showRadiusPaintUI(obj_id player, obj_id terminal, int cityId) throws InterruptedException
    {
        int cityRank = city.getCityRank(cityId);
        int maxRadius = PAINT_RADIUS_MAX[Math.min(cityRank, PAINT_RADIUS_MAX.length - 1)];

        if (maxRadius <= 0)
        {
            sendSystemMessage(player, new string_id("city/city", "terrain_rank_too_low"));
            return;
        }

        utils.setScriptVar(player, "terrain.city_id", cityId);
        utils.setScriptVar(player, "terrain.max_radius", maxRadius);

        sui.listbox(terminal, player, "Select terrain shader to paint:", sui.OK_CANCEL,
                   "Select Terrain Shader", TERRAIN_SHADER_NAMES, "handleRadiusShaderSelection", true, false);
    }

    public int handleRadiusShaderSelection(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        int idx = sui.getListboxSelectedRow(params);

        if (btn != sui.BP_OK || idx < 0 || idx >= TERRAIN_SHADERS.length)
        {
            return SCRIPT_CONTINUE;
        }

        utils.setScriptVar(player, "terrain.shader", TERRAIN_SHADERS[idx]);
        utils.setScriptVar(player, "terrain.shader_name", TERRAIN_SHADER_NAMES[idx]);

        int maxRadius = utils.getIntScriptVar(player, "terrain.max_radius");

        sui.inputbox(self, player, "Enter paint radius (5 - " + maxRadius + " meters):",
                    sui.OK_CANCEL, "Set Radius", sui.INPUT_NORMAL, "10", "handleRadiusInput", null);

        return SCRIPT_CONTINUE;
    }

    public int handleRadiusInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        String input = sui.getInputBoxText(params);

        if (btn != sui.BP_OK || input == null || input.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        int radius;
        try
        {
            radius = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, new string_id("city/city", "invalid_number"));
            return SCRIPT_CONTINUE;
        }

        int maxRadius = utils.getIntScriptVar(player, "terrain.max_radius");

        if (radius < 5 || radius > maxRadius)
        {
            sendSystemMessage(player, new string_id("city/city", "radius_out_of_range"));
            return SCRIPT_CONTINUE;
        }

        utils.setScriptVar(player, "terrain.radius", radius);

        sendSystemMessage(player, new string_id("city/city", "click_to_paint_radius"));

        // Listen for terrain click
        listenToMessage(player, "terrainPaintClick");
        setObjVar(player, "terrain.awaiting_click", 1);
        setObjVar(player, "terrain.paint_mode", "RADIUS");

        return SCRIPT_CONTINUE;
    }

    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        if (!hasObjVar(self, "terrain.awaiting_click"))
        {
            return SCRIPT_CONTINUE;
        }

        if (text.startsWith("/painthere"))
        {
            location loc = getLocation(self);

            String paintMode = getStringObjVar(self, "terrain.paint_mode");
            int cityId = utils.getIntScriptVar(self, "terrain.city_id");
            String shader = utils.getStringScriptVar(self, "terrain.shader");

            if (paintMode.equals("RADIUS"))
            {
                int radius = utils.getIntScriptVar(self, "terrain.radius");
                applyRadiusPaint(self, cityId, loc, radius, shader);
            }
            else if (paintMode.equals("ROAD_FIRST"))
            {
                // Set first marker
                setObjVar(self, ROAD_MARKER_VAR + ".first_x", loc.x);
                setObjVar(self, ROAD_MARKER_VAR + ".first_z", loc.z);
                setObjVar(self, "terrain.paint_mode", "ROAD_SECOND");
                sendSystemMessage(self, new string_id("city/city", "first_marker_set"));
                sendSystemMessage(self, new string_id("city/city", "walk_to_second_marker"));
            }
            else if (paintMode.equals("ROAD_SECOND"))
            {
                float startX = getFloatObjVar(self, ROAD_MARKER_VAR + ".first_x");
                float startZ = getFloatObjVar(self, ROAD_MARKER_VAR + ".first_z");
                int width = utils.getIntScriptVar(self, "terrain.road_width");

                applyRoadPaint(self, cityId, startX, startZ, loc.x, loc.z, width, shader);

                removeObjVar(self, ROAD_MARKER_VAR);
            }

            removeObjVar(self, "terrain.awaiting_click");
            removeObjVar(self, "terrain.paint_mode");
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // ROAD PAINTING
    // ========================================================================

    public void showRoadPaintUI(obj_id player, obj_id terminal, int cityId) throws InterruptedException
    {
        int cityRank = city.getCityRank(cityId);

        if (cityRank < 2)
        {
            sendSystemMessage(player, new string_id("city/city", "terrain_rank_too_low"));
            return;
        }

        utils.setScriptVar(player, "terrain.city_id", cityId);

        sui.listbox(terminal, player, "Select road surface shader:", sui.OK_CANCEL,
                   "Select Road Surface", TERRAIN_SHADER_NAMES, "handleRoadShaderSelection", true, false);
    }

    public int handleRoadShaderSelection(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        int idx = sui.getListboxSelectedRow(params);

        if (btn != sui.BP_OK || idx < 0 || idx >= TERRAIN_SHADERS.length)
        {
            return SCRIPT_CONTINUE;
        }

        utils.setScriptVar(player, "terrain.shader", TERRAIN_SHADERS[idx]);
        utils.setScriptVar(player, "terrain.shader_name", TERRAIN_SHADER_NAMES[idx]);

        sui.inputbox(self, player, "Enter road width (" + ROAD_WIDTH_MIN + " - " + ROAD_WIDTH_MAX + " meters):",
                    sui.OK_CANCEL, "Set Road Width", sui.INPUT_NORMAL, "4", "handleRoadWidthInput", null);

        return SCRIPT_CONTINUE;
    }

    public int handleRoadWidthInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        String input = sui.getInputBoxText(params);

        if (btn != sui.BP_OK || input == null || input.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        int width;
        try
        {
            width = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, new string_id("city/city", "invalid_number"));
            return SCRIPT_CONTINUE;
        }

        if (width < ROAD_WIDTH_MIN || width > ROAD_WIDTH_MAX)
        {
            sendSystemMessage(player, new string_id("city/city", "width_out_of_range"));
            return SCRIPT_CONTINUE;
        }

        utils.setScriptVar(player, "terrain.road_width", width);

        sendSystemMessage(player, new string_id("city/city", "walk_to_first_marker"));

        setObjVar(player, "terrain.awaiting_click", 1);
        setObjVar(player, "terrain.paint_mode", "ROAD_FIRST");

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // BULLDOZE SYSTEM
    // ========================================================================

    public void showBulldozeUI(obj_id player, obj_id terminal, int cityId) throws InterruptedException
    {
        int cityRank = city.getCityRank(cityId);

        if (cityRank < 3)
        {
            sendSystemMessage(player, new string_id("city/city", "bulldoze_rank_too_low"));
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);

        // Check if already bulldozed
        if (hasObjVar(cityHall, "city.bulldozed"))
        {
            sendSystemMessage(player, new string_id("city/city", "city_already_bulldozed"));
            return;
        }

        utils.setScriptVar(player, "terrain.city_id", cityId);

        String message = "WARNING: This will flatten all terrain within city limits.\n\n";
        message += "All structures will be adjusted to the new ground level.\n";
        message += "This action cannot be easily undone.\n\n";
        message += "Do you wish to proceed?";

        sui.msgbox(terminal, player, message, sui.YES_NO, "Bulldoze City", sui.MSG_QUESTION, "handleBulldozeConfirm");
    }

    public int handleBulldozeConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);

        if (btn != sui.BP_OK)
        {
            return SCRIPT_CONTINUE;
        }

        int cityId = utils.getIntScriptVar(player, "terrain.city_id");

        sui.inputbox(self, player, "Enter target height (leave blank for average terrain height):",
                    sui.OK_CANCEL, "Set Bulldoze Height", sui.INPUT_NORMAL, "", "handleBulldozeHeightInput", null);

        return SCRIPT_CONTINUE;
    }

    public int handleBulldozeHeightInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        String input = sui.getInputBoxText(params);

        if (btn != sui.BP_OK)
        {
            return SCRIPT_CONTINUE;
        }

        int cityId = utils.getIntScriptVar(player, "terrain.city_id");

        float targetHeight;

        if (input == null || input.isEmpty())
        {
            // Calculate average terrain height
            targetHeight = calculateAverageTerrainHeight(cityId);
        }
        else
        {
            try
            {
                targetHeight = Float.parseFloat(input);
            }
            catch (NumberFormatException e)
            {
                sendSystemMessage(player, new string_id("city/city", "invalid_number"));
                return SCRIPT_CONTINUE;
            }
        }

        applyBulldoze(player, cityId, targetHeight);

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // VIEW/REMOVE REGIONS
    // ========================================================================

    public void showTerrainRegions(obj_id player, obj_id terminal, int cityId) throws InterruptedException
    {
        obj_id cityHall = cityGetCityHall(cityId);

        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");

        if (regionIds == null || regionIds.length == 0)
        {
            sendSystemMessage(player, new string_id("city/city", "no_terrain_regions"));
            return;
        }

        String message = "City Terrain Regions:\n\n";

        for (int i = 0; i < regionIds.length; i++)
        {
            String regionType = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionIds[i] + ".type");
            String shader = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionIds[i] + ".shader_name");

            message += (i + 1) + ". [" + regionType + "] " + shader + "\n";
        }

        sui.msgbox(terminal, player, message, sui.OK_ONLY, "Terrain Regions", sui.MSG_NORMAL, null);
    }

    public void showRemoveRegionUI(obj_id player, obj_id terminal, int cityId) throws InterruptedException
    {
        obj_id cityHall = cityGetCityHall(cityId);

        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");

        if (regionIds == null || regionIds.length == 0)
        {
            sendSystemMessage(player, new string_id("city/city", "no_terrain_regions"));
            return;
        }

        String[] displayNames = new String[regionIds.length];

        for (int i = 0; i < regionIds.length; i++)
        {
            String regionType = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionIds[i] + ".type");
            String shader = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionIds[i] + ".shader_name");
            displayNames[i] = "[" + regionType + "] " + shader;
        }

        utils.setScriptVar(player, "terrain.city_id", cityId);
        utils.setScriptVar(player, "terrain.region_ids", regionIds);

        sui.listbox(terminal, player, "Select region to remove:", sui.OK_CANCEL,
                   "Remove Terrain Region", displayNames, "handleRemoveRegionSelection", true, false);
    }

    public int handleRemoveRegionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        int idx = sui.getListboxSelectedRow(params);

        if (btn != sui.BP_OK || idx < 0)
        {
            return SCRIPT_CONTINUE;
        }

        int cityId = utils.getIntScriptVar(player, "terrain.city_id");
        String[] regionIds = utils.getStringArrayScriptVar(player, "terrain.region_ids");

        if (regionIds == null || idx >= regionIds.length)
        {
            return SCRIPT_CONTINUE;
        }

        removeTerrainRegion(player, cityId, regionIds[idx]);

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // TERRAIN APPLICATION FUNCTIONS
    // ========================================================================

    public void applyRadiusPaint(obj_id player, int cityId, location center, int radius, String shader) throws InterruptedException
    {
        // Validate within city bounds
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        float dist = utils.getDistance2D(center, cityLoc);
        if (dist + radius > cityRadius)
        {
            sendSystemMessage(player, new string_id("city/city", "paint_outside_city"));
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);

        // Generate unique region ID
        String regionId = "R" + System.currentTimeMillis();

        // Store region data
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type", "RADIUS");
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_x", center.x);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_z", center.z);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".radius", radius);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader", shader);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader_name", utils.getStringScriptVar(player, "terrain.shader_name"));
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".created", getGameTime());

        // Add to region list
        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds == null)
        {
            regionIds = new String[0];
        }
        regionIds = utils.addElement(regionIds, regionId);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids", regionIds);

        // Apply terrain modification via server call
        applyTerrainShaderRadius(cityId, center.x, center.z, radius, shader);

        sendSystemMessage(player, new string_id("city/city", "terrain_painted_radius"));

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Terrain painted (radius). City: " + cityName + " Player: " + player + " Shader: " + shader + " Radius: " + radius);
    }

    public void applyRoadPaint(obj_id player, int cityId, float startX, float startZ, float endX, float endZ, int width, String shader) throws InterruptedException
    {
        // Validate within city bounds
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

        // Generate unique region ID
        String regionId = "R" + System.currentTimeMillis();

        // Store region data
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type", "ROAD");
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_x", startX);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_z", startZ);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_x", endX);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_z", endZ);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".width", width);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader", shader);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader_name", utils.getStringScriptVar(player, "terrain.shader_name"));
        setObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".created", getGameTime());

        // Add to region list
        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");
        if (regionIds == null)
        {
            regionIds = new String[0];
        }
        regionIds = utils.addElement(regionIds, regionId);
        setObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids", regionIds);

        // Apply terrain modification via server call
        applyTerrainShaderRoad(cityId, startX, startZ, endX, endZ, width, shader);

        sendSystemMessage(player, new string_id("city/city", "terrain_painted_road"));

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Terrain painted (road). City: " + cityName + " Player: " + player + " Shader: " + shader + " Width: " + width);
    }

    public void applyBulldoze(obj_id player, int cityId, float targetHeight) throws InterruptedException
    {
        obj_id cityHall = cityGetCityHall(cityId);
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        // Store bulldoze state
        setObjVar(cityHall, "city.bulldozed", 1);
        setObjVar(cityHall, "city.bulldozed_height", targetHeight);
        setObjVar(cityHall, "city.bulldozed_time", getGameTime());

        // Apply terrain flattening via server call
        applyTerrainFlatten(cityId, cityLoc.x, cityLoc.z, cityRadius, targetHeight, BULLDOZE_BLEND_DISTANCE);

        // Adjust all structures to new height
        adjustStructuresToHeight(cityId, targetHeight);

        sendSystemMessage(player, new string_id("city/city", "city_bulldozed"));

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "City bulldozed. City: " + cityName + " Player: " + player + " Height: " + targetHeight);
    }

    public void removeTerrainRegion(obj_id player, int cityId, String regionId) throws InterruptedException
    {
        obj_id cityHall = cityGetCityHall(cityId);

        String regionType = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type");

        // Remove terrain modification via server call
        removeTerrainModification(cityId, regionId);

        // Remove region data
        removeObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId);

        // Remove from region list
        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");
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
            setObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids", newRegionIds);
        }

        sendSystemMessage(player, new string_id("city/city", "terrain_region_removed"));

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Terrain region removed. City: " + cityName + " Player: " + player + " Region: " + regionId);
    }

    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================

    public float calculateAverageTerrainHeight(int cityId) throws InterruptedException
    {
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        float totalHeight = 0;
        int sampleCount = 0;

        // Sample terrain at various points within city
        for (int x = -cityRadius; x <= cityRadius; x += 50)
        {
            for (int z = -cityRadius; z <= cityRadius; z += 50)
            {
                float dist = (float)Math.sqrt(x * x + z * z);
                if (dist <= cityRadius)
                {
                    float height = getHeightAtLocation(cityLoc.x + x, cityLoc.z + z);
                    totalHeight += height;
                    sampleCount++;
                }
            }
        }

        return sampleCount > 0 ? totalHeight / sampleCount : 0;
    }

    public void adjustStructuresToHeight(int cityId, float targetHeight) throws InterruptedException
    {
        obj_id[] structures = cityGetStructureIds(cityId);

        if (structures == null)
        {
            return;
        }

        for (obj_id structure : structures)
        {
            if (isIdValid(structure) && exists(structure))
            {
                location loc = getLocation(structure);
                loc.y = targetHeight;
                setLocation(structure, loc);
            }
        }
    }

    // ========================================================================
    // SERVER CALLS (These will call C++ methods)
    // ========================================================================

    public void applyTerrainShaderRadius(int cityId, float centerX, float centerZ, int radius, String shader) throws InterruptedException
    {
        // Call server terrain system
        dictionary params = new dictionary();
        params.put("cityId", cityId);
        params.put("type", "RADIUS");
        params.put("centerX", centerX);
        params.put("centerZ", centerZ);
        params.put("radius", radius);
        params.put("shader", shader);

        // Send to all clients in area
        broadcastTerrainUpdate(cityId, params);
    }

    public void applyTerrainShaderRoad(int cityId, float startX, float startZ, float endX, float endZ, int width, String shader) throws InterruptedException
    {
        dictionary params = new dictionary();
        params.put("cityId", cityId);
        params.put("type", "ROAD");
        params.put("startX", startX);
        params.put("startZ", startZ);
        params.put("endX", endX);
        params.put("endZ", endZ);
        params.put("width", width);
        params.put("shader", shader);

        broadcastTerrainUpdate(cityId, params);
    }

    public void applyTerrainFlatten(int cityId, float centerX, float centerZ, int radius, float height, int blendDist) throws InterruptedException
    {
        dictionary params = new dictionary();
        params.put("cityId", cityId);
        params.put("type", "FLATTEN");
        params.put("centerX", centerX);
        params.put("centerZ", centerZ);
        params.put("radius", radius);
        params.put("height", height);
        params.put("blendDist", blendDist);

        broadcastTerrainUpdate(cityId, params);
    }

    public void removeTerrainModification(int cityId, String regionId) throws InterruptedException
    {
        dictionary params = new dictionary();
        params.put("cityId", cityId);
        params.put("type", "REMOVE");
        params.put("regionId", regionId);

        broadcastTerrainUpdate(cityId, params);
    }

    public void broadcastTerrainUpdate(int cityId, dictionary params) throws InterruptedException
    {
        location cityLoc = cityGetLocation(cityId);
        int cityRadius = cityGetRadius(cityId);

        // Get all players in city area
        obj_id[] players = getPlayerCreaturesInRange(cityLoc, cityRadius + 100);

        if (players != null)
        {
            String type = params.getString("type");
            int modType = 0;

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
            else if (type.equals("REMOVE"))
            {
                modType = 3;
            }

            String regionId = params.getString("regionId");
            if (regionId == null)
            {
                regionId = "R" + System.currentTimeMillis();
            }

            String shader = params.getString("shader");
            if (shader == null)
            {
                shader = "";
            }

            float centerX = params.getFloat("centerX");
            float centerZ = params.getFloat("centerZ");
            float radius = params.getFloat("radius");
            float endX = params.getFloat("endX");
            float endZ = params.getFloat("endZ");
            float width = params.getFloat("width");
            float height = params.getFloat("height");
            float blendDist = params.getFloat("blendDist");

            // Handle ROAD type - convert start/end to center/end format
            if (type.equals("ROAD"))
            {
                centerX = params.getFloat("startX");
                centerZ = params.getFloat("startZ");
                endX = params.getFloat("endX");
                endZ = params.getFloat("endZ");
            }

            for (obj_id player : players)
            {
                if (isIdValid(player))
                {
                    city_terrain_handler.sendTerrainModifyToClient(player, cityId, modType, regionId,
                                                                    shader, centerX, centerZ, radius,
                                                                    endX, endZ, width, height, blendDist);
                }
            }
        }
    }

    // ========================================================================
    // PERSISTENCE - Called on server startup
    // ========================================================================

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        // Re-apply all terrain modifications on server startup
        messageTo(self, "reapplyTerrainModifications", null, 5, false);
        return SCRIPT_CONTINUE;
    }

    public int reapplyTerrainModifications(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id structure = getTopMostContainer(self);
        int cityId = findCityByCityHall(structure);

        if (!cityExists(cityId))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id cityHall = cityGetCityHall(cityId);

        // Reapply bulldoze if present
        if (hasObjVar(cityHall, "city.bulldozed"))
        {
            float height = getFloatObjVar(cityHall, "city.bulldozed_height");
            location cityLoc = cityGetLocation(cityId);
            int cityRadius = cityGetRadius(cityId);
            applyTerrainFlatten(cityId, cityLoc.x, cityLoc.z, cityRadius, height, BULLDOZE_BLEND_DISTANCE);
        }

        // Reapply all terrain regions
        String[] regionIds = getStringArrayObjVar(cityHall, TERRAIN_VAR_ROOT + ".region_ids");

        if (regionIds == null)
        {
            return SCRIPT_CONTINUE;
        }

        for (String regionId : regionIds)
        {
            String type = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".type");
            String shader = getStringObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".shader");

            if (type.equals("RADIUS"))
            {
                float centerX = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_x");
                float centerZ = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".center_z");
                int radius = getIntObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".radius");
                applyTerrainShaderRadius(cityId, centerX, centerZ, radius, shader);
            }
            else if (type.equals("ROAD"))
            {
                float startX = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_x");
                float startZ = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".start_z");
                float endX = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_x");
                float endZ = getFloatObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".end_z");
                int width = getIntObjVar(cityHall, TERRAIN_VAR_ROOT + "." + regionId + ".width");
                applyTerrainShaderRoad(cityId, startX, startZ, endX, endZ, width, shader);
            }
        }

        return SCRIPT_CONTINUE;
    }
}



