package script.terminal;

import script.dictionary;
import script.menu_info;
import script.obj_id;
import script.string_id;
import script.library.dynamic_hardpoint;
import script.library.mount_maker;
import script.library.sui;

/**
 * GM / test-center terminal for {@code hp_dyn.*} hardpoint overlays (app / saddle / light / fx).
 * <p>
 * Radial UI delegates to {@link script.library.dynamic_hardpoint#handleTerminalMenuSelect}; SUI callbacks stay on this
 * script so object-hosted prompts resolve correctly ({@code suiOwner} = terminal).
 * <p>
 * Menu IDs are defined on {@link script.library.dynamic_hardpoint}. Attach this script to a dedicated object (do not mix
 * with other terminals using the same {@code SERVER_MENU*} types). Skip {@code SERVER_MENU38}–{@code SERVER_MENU40} so
 * {@link gm_dynamic_light} can share the same prop if desired.
 */
public class gm_dynamic_hardpoint extends script.base_script
{
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

    private static int handleFloatSlot(obj_id self, dictionary params, String key, String label) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!mount_maker.isDesignerAuthorized(player))
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
        setObjVar(self, dynamic_hardpoint.slotPath(player) + "." + key, v);
        sendSystemMessage(player, string_id.unlocalized(label + " updated."));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!mount_maker.isDesignerAuthorized(player))
            return SCRIPT_CONTINUE;

        int idMisc = mi.addRootMenu(dynamic_hardpoint.ROOT_MISC, string_id.unlocalized("GM: hp_dyn misc"));
        mi.addSubMenu(idMisc, dynamic_hardpoint.MISC_SLOT, string_id.unlocalized("Set slot index (" + dynamic_hardpoint.SLOT_MIN + "-" + dynamic_hardpoint.SLOT_MAX + ")"));
        mi.addSubMenu(idMisc, dynamic_hardpoint.MISC_CLEAR_ALL, string_id.unlocalized("Clear all hp_dyn"));

        int idApp = mi.addRootMenu(dynamic_hardpoint.ROOT_APP, string_id.unlocalized("GM: hp_dyn appearance"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_HP, string_id.unlocalized("Hardpoint name"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_PATH, string_id.unlocalized("Appearance path"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_OX, string_id.unlocalized("Offset X"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_OY, string_id.unlocalized("Offset Y"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_OZ, string_id.unlocalized("Offset Z"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_COMMIT, string_id.unlocalized("Commit as APP"));
        mi.addSubMenu(idApp, dynamic_hardpoint.APP_CLEAR, string_id.unlocalized("Clear this slot"));

        int idLight = mi.addRootMenu(dynamic_hardpoint.ROOT_LIGHT, string_id.unlocalized("GM: hp_dyn light"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_HP, string_id.unlocalized("Hardpoint name"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_R, string_id.unlocalized("R"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_G, string_id.unlocalized("G"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_B, string_id.unlocalized("B"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_RANGE, string_id.unlocalized("Range"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_INTENSITY, string_id.unlocalized("Intensity"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_COMMIT, string_id.unlocalized("Commit as LIGHT"));
        mi.addSubMenu(idLight, dynamic_hardpoint.LIGHT_CLEAR, string_id.unlocalized("Clear this slot"));

        int idFx = mi.addRootMenu(dynamic_hardpoint.ROOT_FX, string_id.unlocalized("GM: hp_dyn FX"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_HP, string_id.unlocalized("Hardpoint name"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_PATH, string_id.unlocalized("FX path"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_OX, string_id.unlocalized("Offset X"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_OY, string_id.unlocalized("Offset Y"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_OZ, string_id.unlocalized("Offset Z"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_SCALE, string_id.unlocalized("Scale"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_COMMIT, string_id.unlocalized("Commit as FX"));
        mi.addSubMenu(idFx, dynamic_hardpoint.FX_CLEAR, string_id.unlocalized("Clear this slot"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        return dynamic_hardpoint.handleTerminalMenuSelect(self, player, item);
    }

    public int handleGmHpDynSlotInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!mount_maker.isDesignerAuthorized(player))
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
        if (v < dynamic_hardpoint.SLOT_MIN || v > dynamic_hardpoint.SLOT_MAX)
        {
            sendSystemMessage(player, string_id.unlocalized("Slot must be " + dynamic_hardpoint.SLOT_MIN + "-" + dynamic_hardpoint.SLOT_MAX + "."));
            return SCRIPT_CONTINUE;
        }
        setObjVar(player, dynamic_hardpoint.OV_HP_SLOT, v);
        sendSystemMessage(player, string_id.unlocalized("hp_dyn edit slot set to " + v + "."));
        return SCRIPT_CONTINUE;
    }

    public int handleGmHpDynAppHpInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!mount_maker.isDesignerAuthorized(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = dynamic_hardpoint.slotPath(player) + ".hp";
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
        if (!mount_maker.isDesignerAuthorized(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        setObjVar(self, dynamic_hardpoint.slotPath(player) + ".path", s);
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
        if (!mount_maker.isDesignerAuthorized(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = dynamic_hardpoint.slotPath(player) + ".hp";
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
        if (!mount_maker.isDesignerAuthorized(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        String p = dynamic_hardpoint.slotPath(player) + ".hp";
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
        if (!mount_maker.isDesignerAuthorized(player))
            return SCRIPT_CONTINUE;
        String s = readInputStringRaw(params, player);
        if (s == null)
            return SCRIPT_CONTINUE;
        setObjVar(self, dynamic_hardpoint.slotPath(player) + ".path", s);
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
}
