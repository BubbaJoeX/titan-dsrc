package script.item.special;

import script.*;
import script.library.*;

public class light_controller extends script.base_script
{
    public static final int LIFESPAN = 120;

    /** Persisted on the cell object by server {@code CellObject::setCellLightColor}. */
    public static final String OV_CELL_LIGHT_R = "lights.cell.r";
    public static final String OV_CELL_LIGHT_G = "lights.cell.g";
    public static final String OV_CELL_LIGHT_B = "lights.cell.b";
    public static final String OV_CELL_LIGHT_BRIGHTNESS = "lights.cell.brightness";

    /** Listbox row label -- opens custom HTML-style color entry instead of preset floats. */
    public static final String COLOR_HTML_OPTION_LABEL = "HTML Color (hex / rgb)";

    public static final String[][] COLOR_PRESETS = {
        {"Bright White",        "1.0",  "1.0",  "1.0"},
        {"Warm White",          "1.0",  "0.9",  "0.8"},
        {"Cool White",          "0.85", "0.9",  "1.0"},
        {"Daylight",            "0.95", "0.95", "1.0"},
        {"Moonlight",           "0.6",  "0.65", "0.85"},
        {"Soft Yellow",         "1.0",  "0.95", "0.7"},
        {"Golden",              "1.0",  "0.85", "0.45"},
        {"Amber",               "1.0",  "0.75", "0.3"},
        {"Orange",              "1.0",  "0.5",  "0.15"},
        {"Burnt Orange",        "0.85", "0.35", "0.1"},
        {"Red",                 "1.0",  "0.15", "0.1"},
        {"Deep Red",            "0.7",  "0.05", "0.05"},
        {"Crimson",             "0.85", "0.1",  "0.15"},
        {"Rose",                "1.0",  "0.3",  "0.4"},
        {"Pink",                "1.0",  "0.4",  "0.6"},
        {"Hot Pink",            "1.0",  "0.2",  "0.5"},
        {"Magenta",             "0.9",  "0.2",  "0.8"},
        {"Violet",              "0.7",  "0.15", "0.9"},
        {"Purple",              "0.6",  "0.2",  "1.0"},
        {"Deep Purple",         "0.4",  "0.1",  "0.7"},
        {"Indigo",              "0.3",  "0.15", "0.85"},
        {"Royal Blue",          "0.25", "0.25", "1.0"},
        {"Blue",                "0.2",  "0.3",  "1.0"},
        {"Sky Blue",            "0.4",  "0.6",  "1.0"},
        {"Ice Blue",            "0.6",  "0.8",  "1.0"},
        {"Cyan",                "0.2",  "0.9",  "1.0"},
        {"Teal",                "0.2",  "0.8",  "0.7"},
        {"Aquamarine",          "0.3",  "1.0",  "0.8"},
        {"Seafoam",             "0.5",  "0.9",  "0.7"},
        {"Mint",                "0.6",  "1.0",  "0.6"},
        {"Green",               "0.2",  "1.0",  "0.3"},
        {"Forest Green",        "0.15", "0.6",  "0.2"},
        {"Olive",               "0.5",  "0.5",  "0.15"},
        {"Lime",                "0.6",  "1.0",  "0.2"},
        {"Chartreuse",          "0.8",  "1.0",  "0.15"},
        {"Peach",               "1.0",  "0.7",  "0.5"},
        {"Coral",               "1.0",  "0.5",  "0.4"},
        {"Salmon",              "1.0",  "0.6",  "0.5"},
        {"Lavender",            "0.75", "0.6",  "0.9"},
        {"Candlelight",         "1.0",  "0.65", "0.2"},
        {"Fireplace",           "1.0",  "0.4",  "0.1"},
        {"Neon Green",          "0.3",  "1.0",  "0.1"},
        {"Neon Pink",           "1.0",  "0.1",  "0.6"},
        {"Neon Blue",           "0.1",  "0.2",  "1.0"},
        {"Blacklight",          "0.25", "0.0",  "0.6"},
        {"Sepia",               "0.7",  "0.5",  "0.3"},
        {"Dim (25%)",           "0.25", "0.25", "0.25"},
        {"Near Dark (10%)",     "0.1",  "0.1",  "0.1"},
        {"Absolute Black",      "0.0",  "0.0",  "0.0"},
        {"Lights Off",          "0.02", "0.02", "0.02"},
        {COLOR_HTML_OPTION_LABEL, "0", "0", "0"},
    };

    public static final String[][] BRIGHTNESS_PRESETS = {
        {"0%   - Blackout",       "0.0"},
        {"1%   - Trace",          "0.01"},
        {"2%   - Deep Dim",       "0.02"},
        {"5%   - Near Dark",      "0.05"},
        {"10%  - Very Dim",       "0.1"},
        {"25%  - Dim",            "0.25"},
        {"50%  - Low",            "0.5"},
        {"75%  - Medium",         "0.75"},
        {"100% - Normal",         "1.0"},
        {"125% - Bright",         "1.25"},
        {"150% - Very Bright",    "1.5"},
        {"200% - Blinding",       "2.0"},
    };

    // ---- Lifecycle ----

    public int OnAttach(obj_id self) throws InterruptedException
    {
        float rightNow = getGameTime();
        setObjVar(self, "item.temporary.time_stamp", rightNow);

        float lifeSpan = getLifeSpan(self);
        messageTo(self, "selfDestruct", null, lifeSpan, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        float lifeSpan = getLifeSpan(self);
        float dieTime = getDieTime(lifeSpan, self);
        if (dieTime < 1)
        {
            messageTo(self, "selfDestruct", null, 1.0f, false);
        }
        else
        {
            messageTo(self, "selfDestruct", null, dieTime, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int selfDestruct(obj_id self, dictionary params) throws InterruptedException
    {
        if (self.isBeingDestroyed())
            return SCRIPT_CONTINUE;

        float lifeSpan = getLifeSpan(self);
        float dieTime = getDieTime(lifeSpan, self);
        if (dieTime < 1)
        {
            destroyObject(self);
        }
        else
        {
            messageTo(self, "selfDestruct", null, dieTime, false);
        }
        return SCRIPT_CONTINUE;
    }

    // ---- Attributes (Examine Window) ----

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
            return SCRIPT_CONTINUE;

        float lifeSpan = getLifeSpan(self);
        float dieTime = getDieTime(lifeSpan, self);
        int timeLeft = Math.max(0, (int) dieTime);

        names[idx] = "storyteller_time_remaining";
        attribs[idx] = utils.formatTimeVerbose(timeLeft);
        idx++;

        if (idx < names.length && hasObjVar(self, "lightswitch.structure"))
        {
            obj_id structure = getObjIdObjVar(self, "lightswitch.structure");
            String structureName = getName(structure);
            if (structureName != null && !structureName.equals(""))
            {
                names[idx] = "lightswitch_structure";
                attribs[idx] = structureName;
            }
        }

        return SCRIPT_CONTINUE;
    }

    // ---- Radial Menu ----

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!utils.isNestedWithinAPlayer(self))
            return SCRIPT_CONTINUE;

        obj_id owner = utils.getContainingPlayer(self);
        if (!isIdValid(owner) || owner != player)
            return SCRIPT_CONTINUE;

        int root = mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Lighting Controls"));

        mi.addSubMenu(root, menu_info_types.SERVER_MENU2, new string_id("Set Color (This Room)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU3, new string_id("Set Color (All Rooms)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU4, new string_id("Set Brightness (This Room)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU5, new string_id("Set Brightness (All Rooms)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU6, new string_id("Set Color + Brightness (This Room)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU7, new string_id("Set Color + Brightness (All Rooms)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU8, new string_id("Copy Room Lighting"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU9, new string_id("Paste Lighting (This Room)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU10, new string_id("Paste Lighting (All Rooms)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU11, new string_id("Reset (This Room)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU12, new string_id("Reset (All Rooms)"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id owner = utils.getContainingPlayer(self);
        if (!isIdValid(owner) || owner != player)
            return SCRIPT_CONTINUE;

        obj_id structure = getObjIdObjVar(self, "lightswitch.structure");
        if (!isIdValid(structure))
        {
            sendSystemMessage(player, "This light controller is no longer linked to a structure.", null);
            return SCRIPT_CONTINUE;
        }

        obj_id structureOwner = getOwner(structure);
        boolean permitted = player_structure.isAdmin(structure, player)
                || player_structure.isOwner(structure, player)
                || (isIdValid(structureOwner) && charactersAreSamePlayer(player, structureOwner));
        if (!permitted)
        {
            sendSystemMessage(player, "You no longer have permission to modify this structure's lights.", null);
            return SCRIPT_CONTINUE;
        }

        if (item == menu_info_types.SERVER_MENU2)
        {
            utils.setScriptVar(player, "lightswitch.allRooms", false);
            utils.setScriptVar(player, "lightswitch.structure", structure);
            utils.setScriptVar(player, "lightswitch.mode", "color");
            showColorPicker(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU3)
        {
            utils.setScriptVar(player, "lightswitch.allRooms", true);
            utils.setScriptVar(player, "lightswitch.structure", structure);
            utils.setScriptVar(player, "lightswitch.mode", "color");
            showColorPicker(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU4)
        {
            utils.setScriptVar(player, "lightswitch.allRooms", false);
            utils.setScriptVar(player, "lightswitch.structure", structure);
            utils.setScriptVar(player, "lightswitch.mode", "brightness");
            showBrightnessPicker(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU5)
        {
            utils.setScriptVar(player, "lightswitch.allRooms", true);
            utils.setScriptVar(player, "lightswitch.structure", structure);
            utils.setScriptVar(player, "lightswitch.mode", "brightness");
            showBrightnessPicker(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU6)
        {
            utils.setScriptVar(player, "lightswitch.allRooms", false);
            utils.setScriptVar(player, "lightswitch.structure", structure);
            utils.setScriptVar(player, "lightswitch.mode", "combo");
            showColorPicker(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU7)
        {
            utils.setScriptVar(player, "lightswitch.allRooms", true);
            utils.setScriptVar(player, "lightswitch.structure", structure);
            utils.setScriptVar(player, "lightswitch.mode", "combo");
            showColorPicker(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU8)
        {
            copyRoomLighting(self, player, structure);
        }
        else if (item == menu_info_types.SERVER_MENU9)
        {
            pasteLighting(self, player, structure, false);
        }
        else if (item == menu_info_types.SERVER_MENU10)
        {
            pasteLighting(self, player, structure, true);
        }
        else if (item == menu_info_types.SERVER_MENU11)
        {
            resetCurrentRoom(player, structure);
        }
        else if (item == menu_info_types.SERVER_MENU12)
        {
            resetAllLights(player, structure);
            sendSystemMessage(player, "All lights have been reset to default.", null);
        }

        return SCRIPT_CONTINUE;
    }

    // ---- SUI: Color Picker ----

    public void showColorPicker(obj_id self, obj_id player) throws InterruptedException
    {
        String[] colorNames = new String[COLOR_PRESETS.length];
        for (int i = 0; i < COLOR_PRESETS.length; i++)
        {
            colorNames[i] = COLOR_PRESETS[i][0];
        }

        sui.listbox(self, player, "Select a light color:", sui.OK_CANCEL, "\\#pcontrast2 Light Color", colorNames, "handleColorSelect", true, false);
    }

    /** Prompt for HTML/CSS style color string and convert to RGB (0..1) chrominance. */
    public void showHtmlColorInput(obj_id self, obj_id player) throws InterruptedException
    {
        String prompt =
            "Enter HTML/CSS style color:\\n" +
            "- Hex: #RGB, #RRGGBB, or #RRGGBBAA (alpha ignored)\\n" +
            "- rgb(r,g,b) or rgba(...) -- values 0-255 or 0-1 per channel\\n" +
            "- Three numbers separated by commas, same scales";

        sui.inputbox(self, player, prompt, "\\#pcontrast2 HTML Color", "handleHtmlColorInput", sui.MAX_INPUT_LENGTH, false, "#FFFFFF");
    }

    public int handleHtmlColorInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        obj_id structure = utils.getObjIdScriptVar(player, "lightswitch.structure");
        if (!isIdValid(structure))
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        String text = sui.getInputBoxText(params);
        float[] rgb = new float[3];
        if (!parseHtmlStyleColorToRgb(text, rgb))
        {
            sendSystemMessage(player, "Invalid color. Examples: #FF8040  rgb(255,128,64)  0.2,0.8,1", null);
            showHtmlColorInput(self, player);
            return SCRIPT_CONTINUE;
        }

        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];

        String mode = utils.getStringScriptVar(player, "lightswitch.mode");
        if (mode != null && mode.equals("combo"))
        {
            utils.setScriptVar(player, "lightswitch.pendingR", r);
            utils.setScriptVar(player, "lightswitch.pendingG", g);
            utils.setScriptVar(player, "lightswitch.pendingB", b);
            showBrightnessPicker(self, player);
            return SCRIPT_CONTINUE;
        }

        boolean allRooms = utils.getBooleanScriptVar(player, "lightswitch.allRooms");

        if (allRooms)
        {
            applyColorToAllCells(structure, r, g, b);
        }
        else
        {
            applyColorToCurrentCell(player, structure, r, g, b);
        }

        sendSystemMessage(player, "Light color set from HTML-style input.", null);
        cleanupScriptVars(player);
        return SCRIPT_CONTINUE;
    }

    /** Parses #RGB/#RRGGBB, rgb()/rgba(), or three comma-separated numbers into linear 0..1 floats. */
    private boolean parseHtmlStyleColorToRgb(String raw, float[] rgbOut)
    {
        if (raw == null || rgbOut == null || rgbOut.length < 3)
            return false;

        String s = raw.trim();
        if (s.length() == 0)
            return false;

        try
        {
            if (s.charAt(0) == '#')
                return parseHexColorDigits(s.substring(1).trim(), rgbOut);

            String lower = s.toLowerCase();
            if (lower.startsWith("rgb"))
            {
                int lp = s.indexOf('(');
                int rp = s.lastIndexOf(')');
                if (lp < 0 || rp <= lp)
                    return false;
                String inner = s.substring(lp + 1, rp).trim();
                java.util.StringTokenizer tok = new java.util.StringTokenizer(inner, ",");
                if (tok.countTokens() < 3)
                    return false;
                float x = Float.parseFloat(tok.nextToken().trim());
                float y = Float.parseFloat(tok.nextToken().trim());
                float z = Float.parseFloat(tok.nextToken().trim());
                scaleRgbTripletToUnitFloat(x, y, z, rgbOut);
                return true;
            }

            java.util.StringTokenizer ctok = new java.util.StringTokenizer(s, ",");
            if (ctok.countTokens() >= 3)
            {
                float x = Float.parseFloat(ctok.nextToken().trim());
                float y = Float.parseFloat(ctok.nextToken().trim());
                float z = Float.parseFloat(ctok.nextToken().trim());
                scaleRgbTripletToUnitFloat(x, y, z, rgbOut);
                return true;
            }
        }
        catch (NumberFormatException e)
        {
            return false;
        }
        catch (StringIndexOutOfBoundsException e)
        {
            return false;
        }

        return false;
    }

    private boolean parseHexColorDigits(String hex, float[] rgbOut)
    {
        if (hex.regionMatches(true, 0, "0x", 0, 2))
            hex = hex.substring(2);

        hex = hex.trim();
        if (hex.length() == 3)
        {
            int rv = Integer.parseInt(hex.substring(0, 1), 16);
            int gv = Integer.parseInt(hex.substring(1, 2), 16);
            int bv = Integer.parseInt(hex.substring(2, 3), 16);
            rgbOut[0] = clamp01(((rv << 4) | rv) / 255.0f);
            rgbOut[1] = clamp01(((gv << 4) | gv) / 255.0f);
            rgbOut[2] = clamp01(((bv << 4) | bv) / 255.0f);
            return true;
        }

        if (hex.length() == 6 || hex.length() == 8)
        {
            int rgbPacked = Integer.parseInt(hex.substring(0, 6), 16);
            rgbOut[0] = clamp01(((rgbPacked >> 16) & 0xFF) / 255.0f);
            rgbOut[1] = clamp01(((rgbPacked >> 8) & 0xFF) / 255.0f);
            rgbOut[2] = clamp01((rgbPacked & 0xFF) / 255.0f);
            return true;
        }

        return false;
    }

    /** If any component is clearly above 1, treat triplet as 0–255 byte values. */
    private void scaleRgbTripletToUnitFloat(float x, float y, float z, float[] rgbOut)
    {
        float max = x > y ? (x > z ? x : z) : (y > z ? y : z);
        float scale = (max > 1.001f) ? (1.0f / 255.0f) : 1.0f;
        rgbOut[0] = clamp01(x * scale);
        rgbOut[1] = clamp01(y * scale);
        rgbOut[2] = clamp01(z * scale);
    }

    private float clamp01(float v)
    {
        if (v < 0.0f)
            return 0.0f;
        if (v > 1.0f)
            return 1.0f;
        return v;
    }

    public int handleColorSelect(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        int row = sui.getListboxSelectedRow(params);
        if (row < 0 || row >= COLOR_PRESETS.length)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        if (COLOR_PRESETS[row][0].equals(COLOR_HTML_OPTION_LABEL))
        {
            showHtmlColorInput(self, player);
            return SCRIPT_CONTINUE;
        }

        obj_id structure = utils.getObjIdScriptVar(player, "lightswitch.structure");
        if (!isIdValid(structure))
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        float r = Float.parseFloat(COLOR_PRESETS[row][1]);
        float g = Float.parseFloat(COLOR_PRESETS[row][2]);
        float b = Float.parseFloat(COLOR_PRESETS[row][3]);

        String mode = utils.getStringScriptVar(player, "lightswitch.mode");
        if (mode != null && mode.equals("combo"))
        {
            utils.setScriptVar(player, "lightswitch.pendingR", r);
            utils.setScriptVar(player, "lightswitch.pendingG", g);
            utils.setScriptVar(player, "lightswitch.pendingB", b);
            showBrightnessPicker(self, player);
            return SCRIPT_CONTINUE;
        }

        boolean allRooms = utils.getBooleanScriptVar(player, "lightswitch.allRooms");

        if (allRooms)
        {
            applyColorToAllCells(structure, r, g, b);
        }
        else
        {
            applyColorToCurrentCell(player, structure, r, g, b);
        }

        sendSystemMessage(player, "Light color set to: " + COLOR_PRESETS[row][0], null);
        cleanupScriptVars(player);
        return SCRIPT_CONTINUE;
    }

    // ---- SUI: Brightness Picker ----

    public void showBrightnessPicker(obj_id self, obj_id player) throws InterruptedException
    {
        String[] brightnessNames = new String[BRIGHTNESS_PRESETS.length];
        for (int i = 0; i < BRIGHTNESS_PRESETS.length; i++)
        {
            brightnessNames[i] = BRIGHTNESS_PRESETS[i][0];
        }

        sui.listbox(self, player, "Select a brightness level:", sui.OK_CANCEL, "\\#pcontrast2 Brightness", brightnessNames, "handleBrightnessSelect", true, false);
    }

    public int handleBrightnessSelect(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        int row = sui.getListboxSelectedRow(params);
        if (row < 0 || row >= BRIGHTNESS_PRESETS.length)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        obj_id structure = utils.getObjIdScriptVar(player, "lightswitch.structure");
        if (!isIdValid(structure))
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        float brightness = Float.parseFloat(BRIGHTNESS_PRESETS[row][1]);
        boolean allRooms = utils.getBooleanScriptVar(player, "lightswitch.allRooms");

        String mode = utils.getStringScriptVar(player, "lightswitch.mode");
        if (mode != null && mode.equals("combo"))
        {
            float r = utils.getFloatScriptVar(player, "lightswitch.pendingR");
            float g = utils.getFloatScriptVar(player, "lightswitch.pendingG");
            float b = utils.getFloatScriptVar(player, "lightswitch.pendingB");

            if (allRooms)
            {
                applyFullToAllCells(structure, r, g, b, brightness);
            }
            else
            {
                applyFullToCurrentCell(player, structure, r, g, b, brightness);
            }

            sendSystemMessage(player, "Color and brightness applied.", null);
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        if (allRooms)
        {
            applyBrightnessToAllCells(structure, brightness);
        }
        else
        {
            applyBrightnessToCurrentCell(player, structure, brightness);
        }

        sendSystemMessage(player, "Brightness set to: " + BRIGHTNESS_PRESETS[row][0], null);
        cleanupScriptVars(player);
        return SCRIPT_CONTINUE;
    }

    // ---- Copy / Paste ----

    public static void removeCellLightingObjVars(obj_id cellObj) throws InterruptedException
    {
        if (!isIdValid(cellObj))
        {
            return;
        }
        removeObjVar(cellObj, OV_CELL_LIGHT_R);
        removeObjVar(cellObj, OV_CELL_LIGHT_G);
        removeObjVar(cellObj, OV_CELL_LIGHT_B);
        removeObjVar(cellObj, OV_CELL_LIGHT_BRIGHTNESS);
    }

    /**
     * Reads {@code lights.cell.*} chrominance (r,g,b template factors). Falls back to legacy {@code cellLights.<n>.*}
     * on the structure, else white -- matching {@link #copyRoomLighting}.
     */
    private void readStoredChrominance(obj_id cellObj, obj_id structure, float[] rgb) throws InterruptedException
    {
        rgb[0] = 1.0f;
        rgb[1] = 1.0f;
        rgb[2] = 1.0f;
        if (!isIdValid(cellObj))
            return;
        if (hasObjVar(cellObj, OV_CELL_LIGHT_R))
        {
            rgb[0] = getFloatObjVar(cellObj, OV_CELL_LIGHT_R);
            rgb[1] = getFloatObjVar(cellObj, OV_CELL_LIGHT_G);
            rgb[2] = getFloatObjVar(cellObj, OV_CELL_LIGHT_B);
            return;
        }
        if (!isIdValid(structure))
            return;
        int cellNum = getCellNumber(cellObj, structure);
        if (cellNum < 0)
            return;
        String legacyBase = "cellLights." + cellNum;
        if (!hasObjVar(structure, legacyBase + ".r"))
            return;
        rgb[0] = getFloatObjVar(structure, legacyBase + ".r");
        rgb[1] = getFloatObjVar(structure, legacyBase + ".g");
        rgb[2] = getFloatObjVar(structure, legacyBase + ".b");
    }

    /** Stored brightness multiplier; defaults to 1 when unset (matches legacy neutral lighting). */
    private float readStoredBrightness(obj_id cellObj, obj_id structure) throws InterruptedException
    {
        if (!isIdValid(cellObj))
            return 1.0f;
        if (hasObjVar(cellObj, OV_CELL_LIGHT_BRIGHTNESS))
            return getFloatObjVar(cellObj, OV_CELL_LIGHT_BRIGHTNESS);
        if (!isIdValid(structure))
            return 1.0f;
        int cellNum = getCellNumber(cellObj, structure);
        if (cellNum < 0)
            return 1.0f;
        String legacyBase = "cellLights." + cellNum;
        if (!hasObjVar(structure, legacyBase + ".brightness"))
            return 1.0f;
        return getFloatObjVar(structure, legacyBase + ".brightness");
    }

    public void copyRoomLighting(obj_id self, obj_id player, obj_id structure) throws InterruptedException
    {
        obj_id cellObj = getCurrentCell(player, structure);
        if (!isIdValid(cellObj))
            return;

        int cellNum = getCellNumber(cellObj, structure);
        if (cellNum < 0)
        {
            sendSystemMessage(player, "Unable to identify this room.", null);
            return;
        }

        float r;
        float g;
        float b;
        float brightness;
        if (hasObjVar(cellObj, OV_CELL_LIGHT_R))
        {
            r = getFloatObjVar(cellObj, OV_CELL_LIGHT_R);
            g = getFloatObjVar(cellObj, OV_CELL_LIGHT_G);
            b = getFloatObjVar(cellObj, OV_CELL_LIGHT_B);
            brightness = getFloatObjVar(cellObj, OV_CELL_LIGHT_BRIGHTNESS);
        }
        else
        {
            String legacyBase = "cellLights." + cellNum;
            if (!hasObjVar(structure, legacyBase + ".r"))
            {
                sendSystemMessage(player, "This room is using default lighting. Set a custom color first.", null);
                return;
            }
            r = getFloatObjVar(structure, legacyBase + ".r");
            g = getFloatObjVar(structure, legacyBase + ".g");
            b = getFloatObjVar(structure, legacyBase + ".b");
            brightness = getFloatObjVar(structure, legacyBase + ".brightness");
        }

        utils.setScriptVar(player, "lightswitch.clipboard.r", r);
        utils.setScriptVar(player, "lightswitch.clipboard.g", g);
        utils.setScriptVar(player, "lightswitch.clipboard.b", b);
        utils.setScriptVar(player, "lightswitch.clipboard.brightness", brightness);

        sendSystemMessage(player, "Room lighting copied to clipboard.", null);
    }

    public void pasteLighting(obj_id self, obj_id player, obj_id structure, boolean allRooms) throws InterruptedException
    {
        if (!utils.hasScriptVar(player, "lightswitch.clipboard.r"))
        {
            sendSystemMessage(player, "No lighting data on clipboard. Use 'Copy Room Lighting' first.", null);
            return;
        }

        float r = utils.getFloatScriptVar(player, "lightswitch.clipboard.r");
        float g = utils.getFloatScriptVar(player, "lightswitch.clipboard.g");
        float b = utils.getFloatScriptVar(player, "lightswitch.clipboard.b");
        float brightness = utils.getFloatScriptVar(player, "lightswitch.clipboard.brightness");

        if (allRooms)
        {
            applyFullToAllCells(structure, r, g, b, brightness);
            sendSystemMessage(player, "Pasted lighting to all rooms.", null);
        }
        else
        {
            applyFullToCurrentCell(player, structure, r, g, b, brightness);
            sendSystemMessage(player, "Pasted lighting to this room.", null);
        }
    }

    // ---- Light Application ----

    public void applyColorToAllCells(obj_id structure, float r, float g, float b) throws InterruptedException
    {
        obj_id[] cellIds = getCellIds(structure);
        if (cellIds == null)
            return;

        for (int i = 0; i < cellIds.length; i++)
        {
            if (!isIdValid(cellIds[i]))
                continue;
            float brightness = readStoredBrightness(cellIds[i], structure);
            setCellLight(cellIds[i], r, g, b, brightness);
        }
    }

    public void applyColorToCurrentCell(obj_id player, obj_id structure, float r, float g, float b) throws InterruptedException
    {
        obj_id cellObj = getCurrentCell(player, structure);
        if (!isIdValid(cellObj))
            return;

        float brightness = readStoredBrightness(cellObj, structure);
        setCellLight(cellObj, r, g, b, brightness);
    }

    public void applyBrightnessToAllCells(obj_id structure, float brightness) throws InterruptedException
    {
        obj_id[] cellIds = getCellIds(structure);
        if (cellIds == null)
            return;

        float[] rgb = new float[3];
        for (int i = 0; i < cellIds.length; i++)
        {
            if (!isIdValid(cellIds[i]))
                continue;
            readStoredChrominance(cellIds[i], structure, rgb);
            setCellLight(cellIds[i], rgb[0], rgb[1], rgb[2], brightness);
        }
    }

    public void applyBrightnessToCurrentCell(obj_id player, obj_id structure, float brightness) throws InterruptedException
    {
        obj_id cellObj = getCurrentCell(player, structure);
        if (!isIdValid(cellObj))
            return;

        float[] rgb = new float[3];
        readStoredChrominance(cellObj, structure, rgb);
        setCellLight(cellObj, rgb[0], rgb[1], rgb[2], brightness);
    }

    public void applyFullToAllCells(obj_id structure, float r, float g, float b, float brightness) throws InterruptedException
    {
        obj_id[] cellIds = getCellIds(structure);
        if (cellIds == null)
            return;

        for (int i = 0; i < cellIds.length; i++)
        {
            if (isIdValid(cellIds[i]))
                setCellLight(cellIds[i], r, g, b, brightness);
        }
    }

    public void applyFullToCurrentCell(obj_id player, obj_id structure, float r, float g, float b, float brightness) throws InterruptedException
    {
        obj_id cellObj = getCurrentCell(player, structure);
        if (!isIdValid(cellObj))
            return;

        setCellLight(cellObj, r, g, b, brightness);
    }

    public void resetCurrentRoom(obj_id player, obj_id structure) throws InterruptedException
    {
        obj_id cellObj = getCurrentCell(player, structure);
        if (!isIdValid(cellObj))
            return;

        int cellNum = getCellNumber(cellObj, structure);
        removeCellLightingObjVars(cellObj);
        if (cellNum >= 0)
        {
            removeObjVar(structure, "cellLights." + cellNum);
        }
        setCellLight(cellObj, 1.0f, 1.0f, 1.0f, 1.0f);

        sendSystemMessage(player, "This room's lights have been reset to default.", null);
    }

    public void resetAllLights(obj_id player, obj_id structure) throws InterruptedException
    {
        obj_id[] cellIds = getCellIds(structure);
        if (cellIds == null)
            return;

        for (int i = 0; i < cellIds.length; i++)
        {
            if (isIdValid(cellIds[i]))
            {
                removeCellLightingObjVars(cellIds[i]);
                setCellLight(cellIds[i], 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        removeObjVar(structure, "cellLights");
    }

    // ---- Helpers ----

    public obj_id getCurrentCell(obj_id player, obj_id structure) throws InterruptedException
    {
        if (!isIdValid(player) || !isIdValid(structure))
        {
            return obj_id.NULL_ID;
        }

        obj_id walk = getContainedBy(player);
        int guard = 0;
        while (isIdValid(walk) && guard++ < 64)
        {
            obj_id parent = getContainedBy(walk);
            if (!isIdValid(parent))
            {
                break;
            }
            if (parent == structure)
            {
                return walk;
            }
            walk = parent;
        }

        sendSystemMessage(player, "You must be inside the structure to change this room's lights.", null);
        return obj_id.NULL_ID;
    }

    public int getCellNumber(obj_id cellObj, obj_id structure) throws InterruptedException
    {
        obj_id[] cellIds = getCellIds(structure);
        if (cellIds == null)
            return -1;

        for (int i = 0; i < cellIds.length; i++)
        {
            if (isIdValid(cellIds[i]) && cellIds[i].equals(cellObj))
                return i + 1;
        }
        return -1;
    }

    public float getLifeSpan(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, "item.lifespan"))
            return getIntObjVar(self, "item.lifespan");
        return LIFESPAN;
    }

    public float getDieTime(float lifeSpan, obj_id self) throws InterruptedException
    {
        float timeStamp = getFloatObjVar(self, "item.temporary.time_stamp");
        float deathStamp = timeStamp + lifeSpan;
        float rightNow = getGameTime();
        return deathStamp - rightNow;
    }

    public void cleanupScriptVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, "lightswitch.structure");
        utils.removeScriptVar(player, "lightswitch.allRooms");
        utils.removeScriptVar(player, "lightswitch.mode");
        utils.removeScriptVar(player, "lightswitch.pendingR");
        utils.removeScriptVar(player, "lightswitch.pendingG");
        utils.removeScriptVar(player, "lightswitch.pendingB");
    }

    public int OnAboutToBeTransferred(obj_id self, obj_id destContainer, obj_id transferer) throws InterruptedException
    {
        if (isIdValid(transferer))
        {
            obj_id owner = utils.getContainingPlayer(self);
            if (isIdValid(owner) && owner == transferer)
            {
                if (!utils.isNestedWithin(destContainer, transferer))
                {
                    sendSystemMessage(transferer, "The Remote Light Controller cannot be traded or dropped.", null);
                    return SCRIPT_OVERRIDE;
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
}
