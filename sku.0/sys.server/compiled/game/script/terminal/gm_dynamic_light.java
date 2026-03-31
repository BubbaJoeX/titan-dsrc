package script.terminal;

import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.sui;

import java.util.StringTokenizer;

/**
 * Attach to any tangible with CDF / point lights. Lets GMs and test-center designers
 * toggle dynamic light override and edit R, G, B, range, intensity (same objvars as housing scripts).
 */
public class gm_dynamic_light extends script.base_script
{
    private static final int MENU_TOGGLE = menu_info_types.SERVER_MENU38;
    private static final int MENU_EDIT_VALUES = menu_info_types.SERVER_MENU39;
    private static final int MENU_CLEAR_CDF = menu_info_types.SERVER_MENU40;

    private static final String OV_OVERRIDE = "dynamicLight.override";
    private static final String OV_R = "dynamicLight.r";
    private static final String OV_G = "dynamicLight.g";
    private static final String OV_B = "dynamicLight.b";
    private static final String OV_RANGE = "dynamicLight.range";
    private static final String OV_INTENSITY = "dynamicLight.intensity";

    /** Default warm white when turning on without prior values. */
    private static final float DEF_R = 1.0f;
    private static final float DEF_G = 0.92f;
    private static final float DEF_B = 0.85f;
    private static final float DEF_RANGE = 10.0f;
    private static final float DEF_INTENSITY = 1.0f;

    private static boolean canEditLight(obj_id player)
    {
        return isIdValid(player) && (isGod(player) || hasObjVar(player, "test_center"));
    }

    private static boolean isOverrideOn(obj_id self)
    {
        return hasObjVar(self, OV_OVERRIDE) && getIntObjVar(self, OV_OVERRIDE) != 0;
    }

    private static void clearDynamicLightObjVars(obj_id self) throws InterruptedException
    {
        removeObjVar(self, OV_OVERRIDE);
        removeObjVar(self, OV_R);
        removeObjVar(self, OV_G);
        removeObjVar(self, OV_B);
        removeObjVar(self, OV_RANGE);
        removeObjVar(self, OV_INTENSITY);
    }

    private static void applyDynamicLight(obj_id self, float r, float g, float b, float range, float intensity) throws InterruptedException
    {
        setObjVar(self, OV_OVERRIDE, 1);
        setObjVar(self, OV_R, r);
        setObjVar(self, OV_G, g);
        setObjVar(self, OV_B, b);
        setObjVar(self, OV_RANGE, range);
        setObjVar(self, OV_INTENSITY, intensity);
    }

    private static String currentValuesPromptLine(obj_id self)
    {
        float r = DEF_R;
        float g = DEF_G;
        float b = DEF_B;
        float range = DEF_RANGE;
        float intensity = DEF_INTENSITY;
        if (hasObjVar(self, OV_R))
            r = getFloatObjVar(self, OV_R);
        if (hasObjVar(self, OV_G))
            g = getFloatObjVar(self, OV_G);
        if (hasObjVar(self, OV_B))
            b = getFloatObjVar(self, OV_B);
        if (hasObjVar(self, OV_RANGE))
            range = getFloatObjVar(self, OV_RANGE);
        if (hasObjVar(self, OV_INTENSITY))
            intensity = getFloatObjVar(self, OV_INTENSITY);
        return r + " " + g + " " + b + " " + range + " " + intensity;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!canEditLight(player))
            return SCRIPT_CONTINUE;

        mi.addRootMenu(MENU_TOGGLE, string_id.unlocalized("Light: " + (isOverrideOn(self) ? "On - tap to turn off" : "Off - tap to turn on")));
        mi.addRootMenu(MENU_EDIT_VALUES, string_id.unlocalized("Set Values"));
        mi.addRootMenu(MENU_CLEAR_CDF, string_id.unlocalized("Clear/Restore"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!canEditLight(player))
            return SCRIPT_CONTINUE;

        if (item == MENU_TOGGLE)
        {
            if (isOverrideOn(self))
            {
                clearDynamicLightObjVars(self);
                sendSystemMessage(player, string_id.unlocalized("Dynamic light off - using defaults on this object."));
            }
            else
            {
                float r = DEF_R;
                float g = DEF_G;
                float b = DEF_B;
                float range = DEF_RANGE;
                float intensity = DEF_INTENSITY;
                if (hasObjVar(self, OV_R))
                    r = getFloatObjVar(self, OV_R);
                if (hasObjVar(self, OV_G))
                    g = getFloatObjVar(self, OV_G);
                if (hasObjVar(self, OV_B))
                    b = getFloatObjVar(self, OV_B);
                if (hasObjVar(self, OV_RANGE))
                    range = getFloatObjVar(self, OV_RANGE);
                if (hasObjVar(self, OV_INTENSITY))
                    intensity = getFloatObjVar(self, OV_INTENSITY);
                applyDynamicLight(self, r, g, b, range, intensity);
                sendSystemMessage(player, string_id.unlocalized("Dynamic light on - RGB " + r + " " + g + " " + b + ", range " + range + ", intensity " + intensity));
            }
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_CLEAR_CDF)
        {
            clearDynamicLightObjVars(self);
            sendSystemMessage(player, string_id.unlocalized("Cleared dynamic light override."));
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_EDIT_VALUES)
        {
            String defaults = currentValuesPromptLine(self);
            String prompt = "Enter five numbers separated by spaces:\nR G B Range Intensity\n\nExample: 1 0.9 0.8 12 0.75\nCurrent: " + defaults;
            sui.inputbox(self, player, prompt, "Dynamic light", "handleLightValuesInput", 128, false, defaults);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleLightValuesInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEditLight(player))
            return SCRIPT_CONTINUE;
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        String raw = sui.getInputBoxText(params);
        if (raw == null)
            raw = "";
        raw = raw.trim();
        StringTokenizer tok = new StringTokenizer(raw);
        if (tok.countTokens() < 5)
        {
            sendSystemMessage(player, string_id.unlocalized("Need 5 values: R G B Range Intensity (space-separated)."));
            return SCRIPT_CONTINUE;
        }
        try
        {
            float r = Float.parseFloat(tok.nextToken());
            float g = Float.parseFloat(tok.nextToken());
            float b = Float.parseFloat(tok.nextToken());
            float range = Float.parseFloat(tok.nextToken());
            float intensity = Float.parseFloat(tok.nextToken());
            applyDynamicLight(self, r, g, b, range, intensity);
            sendSystemMessage(player, string_id.unlocalized("Dynamic light set: RGB " + r + " " + g + " " + b + ", range " + range + ", intensity " + intensity));
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid number. Use decimals e.g. 1.0 1.0 1.0 10.0 1.0"));
        }
        return SCRIPT_CONTINUE;
    }
}
