package script.library;

import script.creature.creature_dynamic_mount;
import script.dictionary;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.sui;
import script.library.utils;

/**
 * Unified {@code hp_dyn.*} authoring: attachment/saddle appearance (app), lights, and FX on a creature.
 * Used by {@link script.terminal.gm_dynamic_hardpoint} (terminal radial) and
 * {@link script.creature.creature_dynamic_mount} (mount maker listbox). Slot selection is stored on the designer
 * player ({@link #OV_HP_SLOT}) so player-owned SUIs resolve on {@code player.base.base_player}.
 */
public class dynamic_hardpoint extends script.base_script
{
    public static final String OV_HP_SLOT = "mount_maker.hp_dyn_slot";
    /** Legacy terminal sessions may still have this objvar on the player. */
    private static final String OV_HP_SLOT_LEGACY = "gm.hp_dyn.slot";

    public static final int SLOT_MIN = 0;
    public static final int SLOT_MAX = 32;

    // --- Same menu ids as terminal.gm_dynamic_hardpoint (radial) ---
    public static final int ROOT_MISC = menu_info_types.SERVER_MENU21;
    public static final int ROOT_APP = menu_info_types.SERVER_MENU22;
    public static final int ROOT_LIGHT = menu_info_types.SERVER_MENU23;
    public static final int ROOT_FX = menu_info_types.SERVER_MENU24;
    public static final int MISC_SLOT = menu_info_types.SERVER_MENU25;
    public static final int MISC_CLEAR_ALL = menu_info_types.SERVER_MENU26;
    public static final int APP_HP = menu_info_types.SERVER_MENU27;
    public static final int APP_PATH = menu_info_types.SERVER_MENU28;
    public static final int APP_OX = menu_info_types.SERVER_MENU29;
    public static final int APP_OY = menu_info_types.SERVER_MENU30;
    public static final int APP_OZ = menu_info_types.SERVER_MENU31;
    public static final int APP_COMMIT = menu_info_types.SERVER_MENU32;
    public static final int APP_CLEAR = menu_info_types.SERVER_MENU33;
    public static final int LIGHT_HP = menu_info_types.SERVER_MENU34;
    public static final int LIGHT_R = menu_info_types.SERVER_MENU35;
    public static final int LIGHT_G = menu_info_types.SERVER_MENU36;
    public static final int LIGHT_B = menu_info_types.SERVER_MENU37;
    public static final int LIGHT_RANGE = menu_info_types.SERVER_MENU41;
    public static final int LIGHT_INTENSITY = menu_info_types.SERVER_MENU42;
    public static final int LIGHT_COMMIT = menu_info_types.SERVER_MENU43;
    public static final int LIGHT_CLEAR = menu_info_types.SERVER_MENU44;
    public static final int FX_HP = menu_info_types.SERVER_MENU45;
    public static final int FX_PATH = menu_info_types.SERVER_MENU46;
    public static final int FX_OX = menu_info_types.SERVER_MENU47;
    public static final int FX_OY = menu_info_types.SERVER_MENU48;
    public static final int FX_OZ = menu_info_types.SERVER_MENU49;
    public static final int FX_SCALE = menu_info_types.SERVER_MENU50;
    public static final int FX_COMMIT = menu_info_types.SERVER_MENU51;
    public static final int FX_CLEAR = menu_info_types.SERVER_MENU52;

    /** Mount maker listbox callbacks — registered on {@code base_player}. */
    public static final String HANDLER_HP_MAIN = "handleMmHpDynMainList";
    public static final String HANDLER_HP_SLOT = "handleMmHpDynSlotInput";
    public static final String HANDLER_HP_APP_HP = "handleMmHpDynAppHpInput";
    public static final String HANDLER_HP_APP_PATH = "handleMmHpDynAppPathInput";
    public static final String HANDLER_HP_APP_OX = "handleMmHpDynAppOxInput";
    public static final String HANDLER_HP_APP_OY = "handleMmHpDynAppOyInput";
    public static final String HANDLER_HP_APP_OZ = "handleMmHpDynAppOzInput";
    public static final String HANDLER_HP_LT_HP = "handleMmHpDynLightHpInput";
    public static final String HANDLER_HP_LT_R = "handleMmHpDynLightRInput";
    public static final String HANDLER_HP_LT_G = "handleMmHpDynLightGInput";
    public static final String HANDLER_HP_LT_B = "handleMmHpDynLightBInput";
    public static final String HANDLER_HP_LT_RANGE = "handleMmHpDynLightRangeInput";
    public static final String HANDLER_HP_LT_INT = "handleMmHpDynLightIntInput";
    public static final String HANDLER_HP_FX_HP = "handleMmHpDynFxHpInput";
    public static final String HANDLER_HP_FX_PATH = "handleMmHpDynFxPathInput";
    public static final String HANDLER_HP_FX_OX = "handleMmHpDynFxOxInput";
    public static final String HANDLER_HP_FX_OY = "handleMmHpDynFxOyInput";
    public static final String HANDLER_HP_FX_OZ = "handleMmHpDynFxOzInput";
    public static final String HANDLER_HP_FX_SCALE = "handleMmHpDynFxScaleInput";

    private static boolean canEdit(obj_id player) throws InterruptedException
    {
        return mount_maker.isDesignerAuthorized(player);
    }

    public static int getSlot(obj_id player) throws InterruptedException
    {
        if (hasObjVar(player, OV_HP_SLOT))
            return clampSlot(getIntObjVar(player, OV_HP_SLOT));
        if (hasObjVar(player, OV_HP_SLOT_LEGACY))
            return clampSlot(getIntObjVar(player, OV_HP_SLOT_LEGACY));
        return 0;
    }

    private static int clampSlot(int s)
    {
        if (s < SLOT_MIN)
            return SLOT_MIN;
        if (s > SLOT_MAX)
            return SLOT_MAX;
        return s;
    }

    public static String slotPath(obj_id player) throws InterruptedException
    {
        return "hp_dyn." + getSlot(player);
    }

    private static void clearSlotTree(obj_id creature, obj_id player) throws InterruptedException
    {
        removeObjVar(creature, slotPath(player));
    }

    private static void clearAllHpDyn(obj_id creature) throws InterruptedException
    {
        removeObjVar(creature, "hp_dyn");
    }

    public static String getSlotString(obj_id creature, obj_id player, String key) throws InterruptedException
    {
        String p = slotPath(player) + "." + key;
        if (!hasObjVar(creature, p))
            return "";
        return getStringObjVar(creature, p);
    }

    public static float getSlotFloat(obj_id creature, obj_id player, String key, float def) throws InterruptedException
    {
        String p = slotPath(player) + "." + key;
        if (!hasObjVar(creature, p))
            return def;
        return getFloatObjVar(creature, p);
    }

    private static void ensureAppDefaults(obj_id creature, obj_id player) throws InterruptedException
    {
        String p = slotPath(player);
        if (!hasObjVar(creature, p + ".ox"))
            setObjVar(creature, p + ".ox", 0.0f);
        if (!hasObjVar(creature, p + ".oy"))
            setObjVar(creature, p + ".oy", 0.0f);
        if (!hasObjVar(creature, p + ".oz"))
            setObjVar(creature, p + ".oz", 0.0f);
    }

    private static void ensureLightDefaults(obj_id creature, obj_id player) throws InterruptedException
    {
        String p = slotPath(player);
        if (!hasObjVar(creature, p + ".r"))
            setObjVar(creature, p + ".r", 1.0f);
        if (!hasObjVar(creature, p + ".g"))
            setObjVar(creature, p + ".g", 1.0f);
        if (!hasObjVar(creature, p + ".b"))
            setObjVar(creature, p + ".b", 1.0f);
        if (!hasObjVar(creature, p + ".range"))
            setObjVar(creature, p + ".range", 10.0f);
        if (!hasObjVar(creature, p + ".intensity"))
            setObjVar(creature, p + ".intensity", 1.0f);
    }

    private static void ensureFxDefaults(obj_id creature, obj_id player) throws InterruptedException
    {
        String p = slotPath(player);
        if (!hasObjVar(creature, p + ".ox"))
            setObjVar(creature, p + ".ox", 0.0f);
        if (!hasObjVar(creature, p + ".oy"))
            setObjVar(creature, p + ".oy", 0.0f);
        if (!hasObjVar(creature, p + ".oz"))
            setObjVar(creature, p + ".oz", 0.0f);
        if (!hasObjVar(creature, p + ".scale"))
            setObjVar(creature, p + ".scale", 1.0f);
    }

    /** Terminal / object-hosted SUI: {@code suiOwner} receives callbacks (e.g. {@code gm_dynamic_hardpoint}). */
    private static void promptInt(obj_id suiOwner, obj_id suiViewer, String title, String prompt, String handler, int current) throws InterruptedException
    {
        sui.inputbox(suiOwner, suiViewer, prompt, title, handler, sui.MAX_INPUT_LENGTH, false, Integer.toString(current));
    }

    private static void promptFloat(obj_id suiOwner, obj_id suiViewer, String title, String prompt, String handler, float current) throws InterruptedException
    {
        sui.inputbox(suiOwner, suiViewer, prompt, title, handler, sui.MAX_INPUT_LENGTH, false, Float.toString(current));
    }

    private static void promptString(obj_id suiOwner, obj_id suiViewer, String title, String prompt, String handler, String current) throws InterruptedException
    {
        if (current == null)
            current = "";
        sui.inputbox(suiOwner, suiViewer, prompt, title, handler, sui.MAX_INPUT_LENGTH, false, current);
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

    private static int handleFloatSlot(obj_id creature, dictionary params, String key, String label) throws InterruptedException
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
        setObjVar(creature, slotPath(player) + "." + key, v);
        sendSystemMessage(player, string_id.unlocalized(label + " updated."));
        return SCRIPT_CONTINUE;
    }

    // --- Terminal radial (unchanged menu ids): target is terminal or creature ---
    public static int handleTerminalMenuSelect(obj_id target, obj_id player, int item) throws InterruptedException
    {
        if (!canEdit(player))
            return SCRIPT_CONTINUE;

        if (item == MISC_CLEAR_ALL)
        {
            clearAllHpDyn(target);
            sendSystemMessage(player, string_id.unlocalized("Removed hp_dyn from this object."));
            return SCRIPT_CONTINUE;
        }

        if (item == MISC_SLOT)
        {
            promptInt(target, player, "hp_dyn slot", "Slot index " + SLOT_MIN + "-" + SLOT_MAX + " (current " + getSlot(player) + "):", "handleGmHpDynSlotInput", getSlot(player));
            return SCRIPT_CONTINUE;
        }

        if (item == APP_CLEAR || item == LIGHT_CLEAR || item == FX_CLEAR)
        {
            clearSlotTree(target, player);
            sendSystemMessage(player, string_id.unlocalized("Cleared hp_dyn slot " + getSlot(player) + "."));
            return SCRIPT_CONTINUE;
        }

        if (item == APP_COMMIT)
        {
            String path = getSlotString(target, player, "path");
            if (path.length() == 0)
            {
                sendSystemMessage(player, string_id.unlocalized("Set appearance path before commit."));
                return SCRIPT_CONTINUE;
            }
            ensureAppDefaults(target, player);
            setObjVar(target, slotPath(player) + ".kind", "app");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as app."));
            return SCRIPT_CONTINUE;
        }

        if (item == LIGHT_COMMIT)
        {
            ensureLightDefaults(target, player);
            setObjVar(target, slotPath(player) + ".kind", "light");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as light."));
            return SCRIPT_CONTINUE;
        }

        if (item == FX_COMMIT)
        {
            String path = getSlotString(target, player, "path");
            if (path.length() == 0)
            {
                sendSystemMessage(player, string_id.unlocalized("Set FX path before commit."));
                return SCRIPT_CONTINUE;
            }
            ensureFxDefaults(target, player);
            setObjVar(target, slotPath(player) + ".kind", "fx");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as fx."));
            return SCRIPT_CONTINUE;
        }

        if (item == APP_HP)
        {
            promptString(target, player, "hp (app)", "Hardpoint mesh name (empty = default):", "handleGmHpDynAppHpInput", getSlotString(target, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_PATH)
        {
            promptString(target, player, "path (app)", "Client appearance .apt path (saddle / attached object):", "handleGmHpDynAppPathInput", getSlotString(target, player, "path"));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_OX)
        {
            promptFloat(target, player, "ox (app)", "Offset X:", "handleGmHpDynAppOxInput", getSlotFloat(target, player, "ox", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_OY)
        {
            promptFloat(target, player, "oy (app)", "Offset Y:", "handleGmHpDynAppOyInput", getSlotFloat(target, player, "oy", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == APP_OZ)
        {
            promptFloat(target, player, "oz (app)", "Offset Z:", "handleGmHpDynAppOzInput", getSlotFloat(target, player, "oz", 0.0f));
            return SCRIPT_CONTINUE;
        }

        if (item == LIGHT_HP)
        {
            promptString(target, player, "hp (light)", "Hardpoint mesh name (empty = default):", "handleGmHpDynLightHpInput", getSlotString(target, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_R)
        {
            promptFloat(target, player, "r (light)", "Red 0-1+:", "handleGmHpDynLightRInput", getSlotFloat(target, player, "r", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_G)
        {
            promptFloat(target, player, "g (light)", "Green 0-1+:", "handleGmHpDynLightGInput", getSlotFloat(target, player, "g", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_B)
        {
            promptFloat(target, player, "b (light)", "Blue 0-1+:", "handleGmHpDynLightBInput", getSlotFloat(target, player, "b", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_RANGE)
        {
            promptFloat(target, player, "range (light)", "Light range:", "handleGmHpDynLightRangeInput", getSlotFloat(target, player, "range", 10.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == LIGHT_INTENSITY)
        {
            promptFloat(target, player, "intensity (light)", "Light intensity:", "handleGmHpDynLightIntInput", getSlotFloat(target, player, "intensity", 1.0f));
            return SCRIPT_CONTINUE;
        }

        if (item == FX_HP)
        {
            promptString(target, player, "hp (fx)", "Hardpoint mesh name (empty = default):", "handleGmHpDynFxHpInput", getSlotString(target, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_PATH)
        {
            promptString(target, player, "path (fx)", "Client FX path:", "handleGmHpDynFxPathInput", getSlotString(target, player, "path"));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_OX)
        {
            promptFloat(target, player, "ox (fx)", "Offset X:", "handleGmHpDynFxOxInput", getSlotFloat(target, player, "ox", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_OY)
        {
            promptFloat(target, player, "oy (fx)", "Offset Y:", "handleGmHpDynFxOyInput", getSlotFloat(target, player, "oy", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_OZ)
        {
            promptFloat(target, player, "oz (fx)", "Offset Z:", "handleGmHpDynFxOzInput", getSlotFloat(target, player, "oz", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (item == FX_SCALE)
        {
            promptFloat(target, player, "scale (fx)", "FX scale:", "handleGmHpDynFxScaleInput", getSlotFloat(target, player, "scale", 1.0f));
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    /** Mount maker: hp_dyn submenu listbox (player-owned SUI). */
    public static void openHpDynAuthoringListbox(obj_id creature, obj_id player) throws InterruptedException
    {
        mount_maker.ensureDesignerSessionForCreature(creature, player);
        utils.setScriptVar(player, creature_dynamic_mount.SCRIPTVAR_MM_AUTH_CREATURE, creature);
        int slot = getSlot(player);
        String[] rows = new String[]
        {
            "« Back to Attach Mount Scripts main menu",
            "Set hp_dyn edit slot (0-32) [now " + slot + "]",
            "Clear ALL hp_dyn on this creature",
            "--- APP (saddle / attached appearance) ---",
            "APP: Hardpoint mesh name",
            "APP: Appearance .apt path",
            "APP: Offset X",
            "APP: Offset Y",
            "APP: Offset Z",
            "APP: Commit slot as attachment (kind=app)",
            "APP: Clear this slot only",
            "--- LIGHT ---",
            "LIGHT: Hardpoint mesh name",
            "LIGHT: Red",
            "LIGHT: Green",
            "LIGHT: Blue",
            "LIGHT: Range",
            "LIGHT: Intensity",
            "LIGHT: Commit slot as light",
            "LIGHT: Clear this slot only",
            "--- FX ---",
            "FX: Hardpoint mesh name",
            "FX: Client FX path",
            "FX: Offset X",
            "FX: Offset Y",
            "FX: Offset Z",
            "FX: Scale",
            "FX: Commit slot as FX",
            "FX: Clear this slot only",
        };
        sui.listbox(player, player,
                "hp_dyn: appearances, lights, FX per slot. Hardpoint blank or \"-\" follows the saddle bone on creatures (skeletal); set an explicit name for other bones. Export includes hp_dyn.* with mount.dm.",
                sui.OK_CANCEL, "Attachments (hp_dyn)", rows, HANDLER_HP_MAIN, true);
    }

    private static final int HP_ROW_BACK = 0;
    private static final int HP_ROW_SLOT = 1;
    private static final int HP_ROW_CLEAR_ALL = 2;
    private static final int HP_ROW_APP_HP = 4;
    private static final int HP_ROW_APP_PATH = 5;
    private static final int HP_ROW_APP_OX = 6;
    private static final int HP_ROW_APP_OY = 7;
    private static final int HP_ROW_APP_OZ = 8;
    private static final int HP_ROW_APP_COMMIT = 9;
    private static final int HP_ROW_APP_CLEAR = 10;
    private static final int HP_ROW_LT_HP = 12;
    private static final int HP_ROW_LT_R = 13;
    private static final int HP_ROW_LT_G = 14;
    private static final int HP_ROW_LT_B = 15;
    private static final int HP_ROW_LT_RANGE = 16;
    private static final int HP_ROW_LT_INT = 17;
    private static final int HP_ROW_LT_COMMIT = 18;
    private static final int HP_ROW_LT_CLEAR = 19;
    private static final int HP_ROW_FX_HP = 21;
    private static final int HP_ROW_FX_PATH = 22;
    private static final int HP_ROW_FX_OX = 23;
    private static final int HP_ROW_FX_OY = 24;
    private static final int HP_ROW_FX_OZ = 25;
    private static final int HP_ROW_FX_SCALE = 26;
    private static final int HP_ROW_FX_COMMIT = 27;
    private static final int HP_ROW_FX_CLEAR = 28;

    public static int mountMakerHpDynMainList(obj_id creature, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;
        int row = sui.getListboxSelectedRow(params);
        if (row == HP_ROW_BACK)
        {
            creature_dynamic_mount.openAuthoringMainMenu(creature, player);
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_SLOT)
        {
            promptInt(player, player, "hp_dyn slot", "Slot index " + SLOT_MIN + "-" + SLOT_MAX + " (current " + getSlot(player) + "):", HANDLER_HP_SLOT, getSlot(player));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_CLEAR_ALL)
        {
            clearAllHpDyn(creature);
            sendSystemMessage(player, string_id.unlocalized("Removed hp_dyn from this creature."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }

        if (row == HP_ROW_APP_HP)
        {
            promptString(player, player, "hp (app)", "Hardpoint mesh name (empty = default):", HANDLER_HP_APP_HP, getSlotString(creature, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_APP_PATH)
        {
            promptString(player, player, "path (app)", "Client appearance .apt path:", HANDLER_HP_APP_PATH, getSlotString(creature, player, "path"));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_APP_OX)
        {
            promptFloat(player, player, "ox (app)", "Offset X:", HANDLER_HP_APP_OX, getSlotFloat(creature, player, "ox", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_APP_OY)
        {
            promptFloat(player, player, "oy (app)", "Offset Y:", HANDLER_HP_APP_OY, getSlotFloat(creature, player, "oy", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_APP_OZ)
        {
            promptFloat(player, player, "oz (app)", "Offset Z:", HANDLER_HP_APP_OZ, getSlotFloat(creature, player, "oz", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_APP_COMMIT)
        {
            String path = getSlotString(creature, player, "path");
            if (path.length() == 0)
            {
                sendSystemMessage(player, string_id.unlocalized("Set appearance path before commit."));
                openHpDynAuthoringListbox(creature, player);
                return SCRIPT_CONTINUE;
            }
            ensureAppDefaults(creature, player);
            setObjVar(creature, slotPath(player) + ".kind", "app");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as app."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_APP_CLEAR)
        {
            clearSlotTree(creature, player);
            sendSystemMessage(player, string_id.unlocalized("Cleared hp_dyn slot " + getSlot(player) + "."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }

        if (row == HP_ROW_LT_HP)
        {
            promptString(player, player, "hp (light)", "Hardpoint mesh name (empty = default):", HANDLER_HP_LT_HP, getSlotString(creature, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_R)
        {
            promptFloat(player, player, "r (light)", "Red 0-1+:", HANDLER_HP_LT_R, getSlotFloat(creature, player, "r", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_G)
        {
            promptFloat(player, player, "g (light)", "Green 0-1+:", HANDLER_HP_LT_G, getSlotFloat(creature, player, "g", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_B)
        {
            promptFloat(player, player, "b (light)", "Blue 0-1+:", HANDLER_HP_LT_B, getSlotFloat(creature, player, "b", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_RANGE)
        {
            promptFloat(player, player, "range (light)", "Light range:", HANDLER_HP_LT_RANGE, getSlotFloat(creature, player, "range", 10.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_INT)
        {
            promptFloat(player, player, "intensity (light)", "Light intensity:", HANDLER_HP_LT_INT, getSlotFloat(creature, player, "intensity", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_COMMIT)
        {
            ensureLightDefaults(creature, player);
            setObjVar(creature, slotPath(player) + ".kind", "light");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as light."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_LT_CLEAR)
        {
            clearSlotTree(creature, player);
            sendSystemMessage(player, string_id.unlocalized("Cleared hp_dyn slot " + getSlot(player) + "."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }

        if (row == HP_ROW_FX_HP)
        {
            promptString(player, player, "hp (fx)", "Hardpoint mesh name (empty = default):", HANDLER_HP_FX_HP, getSlotString(creature, player, "hp"));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_PATH)
        {
            promptString(player, player, "path (fx)", "Client FX path:", HANDLER_HP_FX_PATH, getSlotString(creature, player, "path"));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_OX)
        {
            promptFloat(player, player, "ox (fx)", "Offset X:", HANDLER_HP_FX_OX, getSlotFloat(creature, player, "ox", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_OY)
        {
            promptFloat(player, player, "oy (fx)", "Offset Y:", HANDLER_HP_FX_OY, getSlotFloat(creature, player, "oy", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_OZ)
        {
            promptFloat(player, player, "oz (fx)", "Offset Z:", HANDLER_HP_FX_OZ, getSlotFloat(creature, player, "oz", 0.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_SCALE)
        {
            promptFloat(player, player, "scale (fx)", "FX scale:", HANDLER_HP_FX_SCALE, getSlotFloat(creature, player, "scale", 1.0f));
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_COMMIT)
        {
            String path = getSlotString(creature, player, "path");
            if (path.length() == 0)
            {
                sendSystemMessage(player, string_id.unlocalized("Set FX path before commit."));
                openHpDynAuthoringListbox(creature, player);
                return SCRIPT_CONTINUE;
            }
            ensureFxDefaults(creature, player);
            setObjVar(creature, slotPath(player) + ".kind", "fx");
            sendSystemMessage(player, string_id.unlocalized("hp_dyn slot " + getSlot(player) + " committed as fx."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }
        if (row == HP_ROW_FX_CLEAR)
        {
            clearSlotTree(creature, player);
            sendSystemMessage(player, string_id.unlocalized("Cleared hp_dyn slot " + getSlot(player) + "."));
            openHpDynAuthoringListbox(creature, player);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynSlotInput(obj_id creature, dictionary params) throws InterruptedException
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
            sendSystemMessage(player, string_id.unlocalized("Slot must be " + SLOT_MIN + "-" + SLOT_MAX + "."));
            return SCRIPT_CONTINUE;
        }
        setObjVar(player, OV_HP_SLOT, v);
        sendSystemMessage(player, string_id.unlocalized("hp_dyn edit slot set to " + v + "."));
        openHpDynAuthoringListbox(creature, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynAppHpInput(obj_id creature, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = slotPath(player) + ".hp";
        if (s.length() == 0)
            removeObjVar(creature, p);
        else
            setObjVar(creature, p, s);
        sendSystemMessage(player, string_id.unlocalized("App hardpoint updated."));
        openHpDynAuthoringListbox(creature, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynAppPathInput(obj_id creature, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        setObjVar(creature, slotPath(player) + ".path", s);
        sendSystemMessage(player, string_id.unlocalized("App path updated."));
        openHpDynAuthoringListbox(creature, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynAppOxInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "ox", "App offset X");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynAppOyInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "oy", "App offset Y");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynAppOzInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "oz", "App offset Z");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynLightHpInput(obj_id creature, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = slotPath(player) + ".hp";
        if (s.length() == 0)
            removeObjVar(creature, p);
        else
            setObjVar(creature, p, s);
        sendSystemMessage(player, string_id.unlocalized("Light hardpoint updated."));
        openHpDynAuthoringListbox(creature, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynLightRInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "r", "Light R");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynLightGInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "g", "Light G");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynLightBInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "b", "Light B");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynLightRangeInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "range", "Light range");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynLightIntInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "intensity", "Light intensity");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynFxHpInput(obj_id creature, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = slotPath(player) + ".hp";
        if (s.length() == 0)
            removeObjVar(creature, p);
        else
            setObjVar(creature, p, s);
        sendSystemMessage(player, string_id.unlocalized("FX hardpoint updated."));
        openHpDynAuthoringListbox(creature, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynFxPathInput(obj_id creature, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        setObjVar(creature, slotPath(player) + ".path", s);
        sendSystemMessage(player, string_id.unlocalized("FX path updated."));
        openHpDynAuthoringListbox(creature, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynFxOxInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "ox", "FX offset X");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynFxOyInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "oy", "FX offset Y");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynFxOzInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "oz", "FX offset Z");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    public static int mountMmHpDynFxScaleInput(obj_id creature, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        handleFloatSlot(creature, params, "scale", "FX scale");
        openHpDynAuthoringListbox(creature, sui.getPlayerId(params));
        return SCRIPT_CONTINUE;
    }

    private dynamic_hardpoint()
    {
    }
}
