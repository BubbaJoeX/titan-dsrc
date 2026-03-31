package script.terminal;

import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.sui;

/**
 * GM / test-center editor for {@code hp_dyn.*} hardpoint overlays (app / light / fx).
 * <p>
 * Uses only existing {@link menu_info_types} entries. Menu IDs consumed here are documented
 * below; attach this script to a <b>dedicated</b> object (do not mix with other terminals that
 * use the same {@code SERVER_MENU*} types on the same object). Skip {@code SERVER_MENU38}–
 * {@code SERVER_MENU40} so {@link gm_dynamic_light} can share the same prop if desired.
 * <ul>
 *   <li>Root: {@code SERVER_MENU21}–{@code SERVER_MENU24}</li>
 *   <li>Misc: {@code SERVER_MENU25}–{@code SERVER_MENU26}</li>
 *   <li>App: {@code SERVER_MENU27}–{@code SERVER_MENU33}</li>
 *   <li>Light: {@code SERVER_MENU34}–{@code SERVER_MENU37}, {@code SERVER_MENU41}–{@code SERVER_MENU44}</li>
 *   <li>Fx: {@code SERVER_MENU45}–{@code SERVER_MENU52}</li>
 * </ul>
 */
public class gm_dynamic_hardpoint extends script.base_script
{
    private static final int ROOT_MISC = menu_info_types.SERVER_MENU21;
    private static final int ROOT_APP = menu_info_types.SERVER_MENU22;
    private static final int ROOT_LIGHT = menu_info_types.SERVER_MENU23;
    private static final int ROOT_FX = menu_info_types.SERVER_MENU24;

    private static final int MISC_SLOT = menu_info_types.SERVER_MENU25;
    private static final int MISC_CLEAR_ALL = menu_info_types.SERVER_MENU26;

    private static final int APP_HP = menu_info_types.SERVER_MENU27;
    private static final int APP_PATH = menu_info_types.SERVER_MENU28;
    private static final int APP_OX = menu_info_types.SERVER_MENU29;
    private static final int APP_OY = menu_info_types.SERVER_MENU30;
    private static final int APP_OZ = menu_info_types.SERVER_MENU31;
    private static final int APP_COMMIT = menu_info_types.SERVER_MENU32;
    private static final int APP_CLEAR = menu_info_types.SERVER_MENU33;

    private static final int LIGHT_HP = menu_info_types.SERVER_MENU34;
    private static final int LIGHT_R = menu_info_types.SERVER_MENU35;
    private static final int LIGHT_G = menu_info_types.SERVER_MENU36;
    private static final int LIGHT_B = menu_info_types.SERVER_MENU37;
    private static final int LIGHT_RANGE = menu_info_types.SERVER_MENU41;
    private static final int LIGHT_INTENSITY = menu_info_types.SERVER_MENU42;
    private static final int LIGHT_COMMIT = menu_info_types.SERVER_MENU43;
    private static final int LIGHT_CLEAR = menu_info_types.SERVER_MENU44;

    private static final int FX_HP = menu_info_types.SERVER_MENU45;
    private static final int FX_PATH = menu_info_types.SERVER_MENU46;
    private static final int FX_OX = menu_info_types.SERVER_MENU47;
    private static final int FX_OY = menu_info_types.SERVER_MENU48;
    private static final int FX_OZ = menu_info_types.SERVER_MENU49;
    private static final int FX_SCALE = menu_info_types.SERVER_MENU50;
    private static final int FX_COMMIT = menu_info_types.SERVER_MENU51;
    private static final int FX_CLEAR = menu_info_types.SERVER_MENU52;

    private static final String OV_PLAYER_SLOT = "gm.hp_dyn.slot";
    private static final int SLOT_MIN = 0;
    private static final int SLOT_MAX = 32;

    private static boolean canEdit(obj_id player)
    {
        return isIdValid(player) && (isGod(player) || hasObjVar(player, "test_center"));
    }

    private static int getSlot(obj_id player) throws InterruptedException
    {
        if (!hasObjVar(player, OV_PLAYER_SLOT))
            return 0;
        int s = getIntObjVar(player, OV_PLAYER_SLOT);
        if (s < SLOT_MIN)
            return SLOT_MIN;
        if (s > SLOT_MAX)
            return SLOT_MAX;
        return s;
    }

    private static String slotPath(obj_id player) throws InterruptedException
    {
        return "hp_dyn." + getSlot(player);
    }

    private static void clearSlotTree(obj_id self, obj_id player) throws InterruptedException
    {
        removeObjVar(self, slotPath(player));
    }

    private static void clearAllHpDyn(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "hp_dyn");
    }

    private static void promptInt(obj_id self, obj_id player, String title, String prompt, String handler, int current) throws InterruptedException
    {
        sui.inputbox(self, player, prompt, title, handler, sui.MAX_INPUT_LENGTH, false, Integer.toString(current));
    }

    private static void promptFloat(obj_id self, obj_id player, String title, String prompt, String handler, float current) throws InterruptedException
    {
        sui.inputbox(self, player, prompt, title, handler, sui.MAX_INPUT_LENGTH, false, Float.toString(current));
    }

    private static void promptString(obj_id self, obj_id player, String title, String prompt, String handler, String current) throws InterruptedException
    {
        if (current == null)
            current = "";
        sui.inputbox(self, player, prompt, title, handler, sui.MAX_INPUT_LENGTH, false, current);
    }

    private static String readInputStringRaw(dictionary params, obj_id player) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
            return null;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            return null;
        return raw.trim();
    }

    private static String getSlotString(obj_id self, obj_id player, String key) throws InterruptedException
    {
        String p = slotPath(player) + "." + key;
        if (!hasObjVar(self, p))
            return "";
        return getStringObjVar(self, p);
    }

    private static float getSlotFloat(obj_id self, obj_id player, String key, float def) throws InterruptedException
    {
        String p = slotPath(player) + "." + key;
        if (!hasObjVar(self, p))
            return def;
        return getFloatObjVar(self, p);
    }

    private static void ensureAppDefaults(obj_id self, obj_id player) throws InterruptedException
    {
        String p = slotPath(player);
        if (!hasObjVar(self, p + ".ox"))
            setObjVar(self, p + ".ox", 0.0f);
        if (!hasObjVar(self, p + ".oy"))
            setObjVar(self, p + ".oy", 0.0f);
        if (!hasObjVar(self, p + ".oz"))
            setObjVar(self, p + ".oz", 0.0f);
    }

    private static void ensureLightDefaults(obj_id self, obj_id player) throws InterruptedException
    {
        String p = slotPath(player);
        if (!hasObjVar(self, p + ".r"))
            setObjVar(self, p + ".r", 1.0f);
        if (!hasObjVar(self, p + ".g"))
            setObjVar(self, p + ".g", 1.0f);
        if (!hasObjVar(self, p + ".b"))
            setObjVar(self, p + ".b", 1.0f);
        if (!hasObjVar(self, p + ".range"))
            setObjVar(self, p + ".range", 10.0f);
        if (!hasObjVar(self, p + ".intensity"))
            setObjVar(self, p + ".intensity", 1.0f);
    }

    private static void ensureFxDefaults(obj_id self, obj_id player) throws InterruptedException
    {
        String p = slotPath(player);
        if (!hasObjVar(self, p + ".ox"))
            setObjVar(self, p + ".ox", 0.0f);
        if (!hasObjVar(self, p + ".oy"))
            setObjVar(self, p + ".oy", 0.0f);
        if (!hasObjVar(self, p + ".oz"))
            setObjVar(self, p + ".oz", 0.0f);
        if (!hasObjVar(self, p + ".scale"))
            setObjVar(self, p + ".scale", 1.0f);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!canEdit(player))
            return SCRIPT_CONTINUE;

        int idMisc = mi.addRootMenu(ROOT_MISC, string_id.unlocalized("GM: hp_dyn misc"));
        mi.addSubMenu(idMisc, MISC_SLOT, string_id.unlocalized("Set slot index (" + SLOT_MIN + "-" + SLOT_MAX + ")"));
        mi.addSubMenu(idMisc, MISC_CLEAR_ALL, string_id.unlocalized("Clear all hp_dyn"));

        int idApp = mi.addRootMenu(ROOT_APP, string_id.unlocalized("GM: hp_dyn appearance"));
        mi.addSubMenu(idApp, APP_HP, string_id.unlocalized("Hardpoint name"));
        mi.addSubMenu(idApp, APP_PATH, string_id.unlocalized("Appearance path"));
        mi.addSubMenu(idApp, APP_OX, string_id.unlocalized("Offset X"));
        mi.addSubMenu(idApp, APP_OY, string_id.unlocalized("Offset Y"));
        mi.addSubMenu(idApp, APP_OZ, string_id.unlocalized("Offset Z"));
        mi.addSubMenu(idApp, APP_COMMIT, string_id.unlocalized("Commit as APP"));
        mi.addSubMenu(idApp, APP_CLEAR, string_id.unlocalized("Clear this slot"));

        int idLight = mi.addRootMenu(ROOT_LIGHT, string_id.unlocalized("GM: hp_dyn light"));
        mi.addSubMenu(idLight, LIGHT_HP, string_id.unlocalized("Hardpoint name"));
        mi.addSubMenu(idLight, LIGHT_R, string_id.unlocalized("R"));
        mi.addSubMenu(idLight, LIGHT_G, string_id.unlocalized("G"));
        mi.addSubMenu(idLight, LIGHT_B, string_id.unlocalized("B"));
        mi.addSubMenu(idLight, LIGHT_RANGE, string_id.unlocalized("Range"));
        mi.addSubMenu(idLight, LIGHT_INTENSITY, string_id.unlocalized("Intensity"));
        mi.addSubMenu(idLight, LIGHT_COMMIT, string_id.unlocalized("Commit as LIGHT"));
        mi.addSubMenu(idLight, LIGHT_CLEAR, string_id.unlocalized("Clear this slot"));

        int idFx = mi.addRootMenu(ROOT_FX, string_id.unlocalized("GM: hp_dyn FX"));
        mi.addSubMenu(idFx, FX_HP, string_id.unlocalized("Hardpoint name"));
        mi.addSubMenu(idFx, FX_PATH, string_id.unlocalized("FX path"));
        mi.addSubMenu(idFx, FX_OX, string_id.unlocalized("Offset X"));
        mi.addSubMenu(idFx, FX_OY, string_id.unlocalized("Offset Y"));
        mi.addSubMenu(idFx, FX_OZ, string_id.unlocalized("Offset Z"));
        mi.addSubMenu(idFx, FX_SCALE, string_id.unlocalized("Scale"));
        mi.addSubMenu(idFx, FX_COMMIT, string_id.unlocalized("Commit as FX"));
        mi.addSubMenu(idFx, FX_CLEAR, string_id.unlocalized("Clear this slot"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!canEdit(player))
            return SCRIPT_CONTINUE;

        if (item == MISC_CLEAR_ALL)
        {
            clearAllHpDyn(self);
            sendSystemMessage(player, string_id.unlocalized("Removed hp_dyn from this object."));
            return SCRIPT_CONTINUE;
        }

        if (item == MISC_SLOT)
        {
            promptInt(self, player, "hp_dyn slot", "Slot index " + SLOT_MIN + "–" + SLOT_MAX + " (current " + getSlot(player) + "):", "handleGmHpDynSlotInput", getSlot(player));
            return SCRIPT_CONTINUE;
        }

        if (item == APP_CLEAR || item == LIGHT_CLEAR || item == FX_CLEAR)
        {
            clearSlotTree(self, player);
            sendSystemMessage(player, string_id.unlocalized("Cleared hp_dyn slot " + getSlot(player) + "."));
            return SCRIPT_CONTINUE;
        }

        if (item == APP_COMMIT)
        {
            String path = getSlotString(self, player, "path");
            if (path.length() == 0)
            {
                sendSystemMessage(player, string_id.unlocalized("Set appearance path before commit."));
                return SCRIPT_CONTINUE;
            }
            ensureAppDefaults(self, player);
            setObjVar(self, slotPath(player) + ".kind", "app");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as app."));
            return SCRIPT_CONTINUE;
        }

        if (item == LIGHT_COMMIT)
        {
            ensureLightDefaults(self, player);
            setObjVar(self, slotPath(player) + ".kind", "light");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as light."));
            return SCRIPT_CONTINUE;
        }

        if (item == FX_COMMIT)
        {
            String path = getSlotString(self, player, "path");
            if (path.length() == 0)
            {
                sendSystemMessage(player, string_id.unlocalized("Set FX path before commit."));
                return SCRIPT_CONTINUE;
            }
            ensureFxDefaults(self, player);
            setObjVar(self, slotPath(player) + ".kind", "fx");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as fx."));
            return SCRIPT_CONTINUE;
        }

        if (item == APP_HP)
        {
            promptString(self, player, "hp (app)", "Hardpoint mesh name (empty = default):", "handleGmHpDynAppHpInput", getSlotString(self, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_PATH)
        {
            promptString(self, player, "path (app)", "Client appearance .apt path:", "handleGmHpDynAppPathInput", getSlotString(self, player, "path"));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_OX)
        {
            promptFloat(self, player, "ox (app)", "Offset X:", "handleGmHpDynAppOxInput", getSlotFloat(self, player, "ox", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_OY)
        {
            promptFloat(self, player, "oy (app)", "Offset Y:", "handleGmHpDynAppOyInput", getSlotFloat(self, player, "oy", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_OZ)
        {
            promptFloat(self, player, "oz (app)", "Offset Z:", "handleGmHpDynAppOzInput", getSlotFloat(self, player, "oz", 0.0f));
            return SCRIPT_CONTINUE;
        }

        if (item == LIGHT_HP)
        {
            promptString(self, player, "hp (light)", "Hardpoint mesh name (empty = default):", "handleGmHpDynLightHpInput", getSlotString(self, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_R)
        {
            promptFloat(self, player, "r (light)", "Red 0–1+:", "handleGmHpDynLightRInput", getSlotFloat(self, player, "r", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_G)
        {
            promptFloat(self, player, "g (light)", "Green 0–1+:", "handleGmHpDynLightGInput", getSlotFloat(self, player, "g", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_B)
        {
            promptFloat(self, player, "b (light)", "Blue 0–1+:", "handleGmHpDynLightBInput", getSlotFloat(self, player, "b", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_RANGE)
        {
            promptFloat(self, player, "range (light)", "Light range:", "handleGmHpDynLightRangeInput", getSlotFloat(self, player, "range", 10.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_INTENSITY)
        {
            promptFloat(self, player, "intensity (light)", "Light intensity:", "handleGmHpDynLightIntInput", getSlotFloat(self, player, "intensity", 1.0f));
            return SCRIPT_CONTINUE;
        }

        if (item == FX_HP)
        {
            promptString(self, player, "hp (fx)", "Hardpoint mesh name (empty = default):", "handleGmHpDynFxHpInput", getSlotString(self, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_PATH)
        {
            promptString(self, player, "path (fx)", "Client FX path:", "handleGmHpDynFxPathInput", getSlotString(self, player, "path"));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_OX)
        {
            promptFloat(self, player, "ox (fx)", "Offset X:", "handleGmHpDynFxOxInput", getSlotFloat(self, player, "ox", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_OY)
        {
            promptFloat(self, player, "oy (fx)", "Offset Y:", "handleGmHpDynFxOyInput", getSlotFloat(self, player, "oy", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_OZ)
        {
            promptFloat(self, player, "oz (fx)", "Offset Z:", "handleGmHpDynFxOzInput", getSlotFloat(self, player, "oz", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_SCALE)
        {
            promptFloat(self, player, "scale (fx)", "FX scale:", "handleGmHpDynFxScaleInput", getSlotFloat(self, player, "scale", 1.0f));
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynSlotInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            return SCRIPT_CONTINUE;
        raw = raw.trim();
        int v;
        try
        {
            v = Integer.parseInt(raw);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid integer."));
            return SCRIPT_CONTINUE;
        }
        if (v < SLOT_MIN || v > SLOT_MAX)
        {
            sendSystemMessage(player, string_id.unlocalized("Slot must be " + SLOT_MIN + "–" + SLOT_MAX + "."));
            return SCRIPT_CONTINUE;
        }
        setObjVar(player, OV_PLAYER_SLOT, v);
        sendSystemMessage(player, string_id.unlocalized("hp_dyn edit slot set to " + v + "."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynAppHpInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = slotPath(player) + ".hp";
        if (s.length() == 0)
            removeObjVar(self, p);
        else
            setObjVar(self, p, s);
        sendSystemMessage(player, string_id.unlocalized("App hardpoint updated."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynAppPathInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        setObjVar(self, slotPath(player) + ".path", s);
        sendSystemMessage(player, string_id.unlocalized("App path updated."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynAppOxInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "ox", "App offset X");
    }

    public int handleGmHpDynAppOyInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "oy", "App offset Y");
    }

    public int handleGmHpDynAppOzInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "oz", "App offset Z");
    }

    public int handleGmHpDynLightHpInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = slotPath(player) + ".hp";
        if (s.length() == 0)
            removeObjVar(self, p);
        else
            setObjVar(self, p, s);
        sendSystemMessage(player, string_id.unlocalized("Light hardpoint updated."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynLightRInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "r", "Light R");
    }

    public int handleGmHpDynLightGInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "g", "Light G");
    }

    public int handleGmHpDynLightBInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "b", "Light B");
    }

    public int handleGmHpDynLightRangeInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "range", "Light range");
    }

    public int handleGmHpDynLightIntInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "intensity", "Light intensity");
    }

    public int handleGmHpDynFxHpInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = slotPath(player) + ".hp";
        if (s.length() == 0)
            removeObjVar(self, p);
        else
            setObjVar(self, p, s);
        sendSystemMessage(player, string_id.unlocalized("FX hardpoint updated."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynFxPathInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        setObjVar(self, slotPath(player) + ".path", s);
        sendSystemMessage(player, string_id.unlocalized("FX path updated."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynFxOxInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "ox", "FX offset X");
    }

    public int handleGmHpDynFxOyInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "oy", "FX offset Y");
    }

    public int handleGmHpDynFxOzInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "oz", "FX offset Z");
    }

    public int handleGmHpDynFxScaleInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleFloatSlot(self, params, "scale", "FX scale");
    }

    private int handleFloatSlot(obj_id self, dictionary params, String key, String label) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            return SCRIPT_CONTINUE;
        raw = raw.trim();
        float v;
        try
        {
            v = Float.parseFloat(raw);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid number for " + label + "."));
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, slotPath(player) + "." + key, v);
        sendSystemMessage(player, string_id.unlocalized(label + " updated."));
        return SCRIPT_CONTINUE;
    }
}
