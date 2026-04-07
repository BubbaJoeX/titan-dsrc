package script.content;/*
@Origin: dsrc.script.content.mannequin
@Purpose: Invulnerable display mannequin with radial controls for name, raw/logical animation, loop, clear, and bind-pose reset.
@Notes: Requires client support for ans:/ansl:/anim:clear/anim:bind (see ClientController). Radial editing is god-only.
*/

import script.*;
import script.library.sui;
import script.library.utils;

public class mannequin extends base_script
{
    public static final String OV_INITIALIZED = "content.mannequin.initialized";
    public static final String OV_ANIM = "content.mannequin.animSpec";
    public static final String OV_LOOP = "content.mannequin.animLoop";

    private static final int M_ROOT = menu_info_types.SERVER_MENU52;
    private static final int M_SET_NAME = menu_info_types.SERVER_MENU47;
    private static final int M_SET_ANIM = menu_info_types.SERVER_MENU48;
    private static final int M_TOGGLE_LOOP = menu_info_types.SERVER_MENU49;
    private static final int M_APPLY = menu_info_types.SERVER_MENU50;
    private static final int M_CLEAR = menu_info_types.SERVER_MENU51;
    private static final int M_BIND = menu_info_types.SERVER_MENU53;

    public int OnAttach(obj_id self)
    {
        syncBasics(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        syncBasics(self);
        try
        {
            applyStoredAnimation(self);
        }
        catch (InterruptedException ignored)
        {
        }
        return SCRIPT_CONTINUE;
    }

    private void syncBasics(obj_id self)
    {
        setInvulnerable(self, true);
        if (!hasObjVar(self, OV_INITIALIZED))
        {
            setObjVar(self, OV_INITIALIZED, 1);
            setName(self, "Mannequin");
        }
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        names[idx] = utils.packStringId(new string_id("Animation"));
        attribs[idx] = hasObjVar(self, OV_ANIM) ? getStringObjVar(self, OV_ANIM) : "(none)";
        idx++;
        names[idx] = utils.packStringId(new string_id("Loop .ans"));
        attribs[idx] = (hasObjVar(self, OV_LOOP) && getIntObjVar(self, OV_LOOP) != 0) ? "On" : "Off";
        idx++;
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isGod(player) || !canManipulate(player, self, true, true, 32.0f, false))
        {
            return SCRIPT_CONTINUE;
        }
        int root = mi.addRootMenu(M_ROOT, string_id.unlocalized("Mannequin"));
        mi.addSubMenu(root, M_SET_NAME, string_id.unlocalized("Set Name"));
        mi.addSubMenu(root, M_SET_ANIM, string_id.unlocalized("Set Animation"));
        mi.addSubMenu(root, M_TOGGLE_LOOP, string_id.unlocalized("Toggle Animation Loop (.ans)"));
        mi.addSubMenu(root, M_APPLY, string_id.unlocalized("Apply Animation"));
        mi.addSubMenu(root, M_CLEAR, string_id.unlocalized("Clear Animation"));
        mi.addSubMenu(root, M_BIND, string_id.unlocalized("Set To Bind (reset pose)"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isGod(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (item == M_SET_NAME)
        {
            sui.inputbox(self, player, "Display name for this mannequin.", "Mannequin Name", "handleSetName", getName(self));
            return SCRIPT_CONTINUE;
        }
        if (item == M_SET_ANIM)
        {
            String cur = hasObjVar(self, OV_ANIM) ? getStringObjVar(self, OV_ANIM) : "";
            String prompt = "Logical .ash action, or .ans path (relative to appearance/animation/ or full appearance/...), or ans:/ansl: prefix.";
            sui.inputbox(self, player, prompt, "Mannequin Animation", "handleSetAnim", cur);
            return SCRIPT_CONTINUE;
        }
        if (item == M_TOGGLE_LOOP)
        {
            int loop = (hasObjVar(self, OV_LOOP) && getIntObjVar(self, OV_LOOP) != 0) ? 0 : 1;
            setObjVar(self, OV_LOOP, loop);
            broadcast(player, "Mannequin .ans loop is now " + (loop != 0 ? "ON" : "OFF") + ". Use Apply if a clip is already playing.");
            return SCRIPT_CONTINUE;
        }
        if (item == M_APPLY)
        {
            applyStoredAnimation(self);
            broadcast(player, "Applied animation to mannequin (see stored spec in examine).");
            return SCRIPT_CONTINUE;
        }
        if (item == M_CLEAR)
        {
            doAnimationAction(self, "anim:clear");
            broadcast(player, "Sent clear to clients (stops raw .ans loop track).");
            return SCRIPT_CONTINUE;
        }
        if (item == M_BIND)
        {
            doAnimationAction(self, "anim:bind");
            broadcast(player, "Sent bind reset to clients (stop loop/action/add tracks).");
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSetName(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        String name = sui.getInputBoxText(params);
        if (name != null && !name.trim().isEmpty())
        {
            setName(self, name.trim());
            broadcast(player, "Mannequin name updated.");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSetAnim(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        String spec = sui.getInputBoxText(params);
        if (spec == null || spec.trim().isEmpty())
        {
            removeObjVar(self, OV_ANIM);
            broadcast(player, "Cleared stored animation spec.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, OV_ANIM, spec.trim());
        broadcast(player, "Stored animation spec. Use Apply to play.");
        return SCRIPT_CONTINUE;
    }

    private void applyStoredAnimation(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OV_ANIM))
        {
            return;
        }
        String spec = getStringObjVar(self, OV_ANIM);
        if (spec == null)
        {
            return;
        }
        spec = spec.trim();
        if (spec.isEmpty())
        {
            return;
        }
        boolean loop = hasObjVar(self, OV_LOOP) && getIntObjVar(self, OV_LOOP) != 0;
        String lower = spec.toLowerCase();

        if (lower.startsWith("ans:") || lower.startsWith("ansl:"))
        {
            doAnimationAction(self, spec);
            return;
        }
        if (lower.endsWith(".ans") || lower.startsWith("appearance/"))
        {
            doAnimationAction(self, (loop ? "ansl:" : "ans:") + spec);
            return;
        }
        doAnimationAction(self, spec);
    }
}
