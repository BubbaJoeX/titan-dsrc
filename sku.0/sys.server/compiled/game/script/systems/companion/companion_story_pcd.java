package script.systems.companion;

import script.*;
import script.library.*;

/**
 * Extra radial on story-companion pet control devices: pick Tank / Healer / Damage (SWTOR-style role).
 */
public class companion_story_pcd extends script.base_script
{
    public static final int MENU_COMBAT_ROLE = menu_info_types.SERVER_MENU17;
    public companion_story_pcd()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, companion_lib.OBJVAR_STORY_COMPANION_ID))
        {
            detachScript(self, "systems.companion.companion_story_pcd");
        }
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, companion_lib.OBJVAR_STORY_COMPANION_ID))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id pad = utils.getPlayerDatapad(player);
        if (getContainedBy(self) != pad)
        {
            return SCRIPT_CONTINUE;
        }
        mi.addRootMenu(MENU_COMBAT_ROLE, string_id.unlocalized("Companion Combat Role"));
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != MENU_COMBAT_ROLE)
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, companion_lib.OBJVAR_STORY_COMPANION_ID))
        {
            return SCRIPT_CONTINUE;
        }
        String[] options = 
        {
            companion_lib.stanceToLabel(companion_lib.STANCE_TANK),
            companion_lib.stanceToLabel(companion_lib.STANCE_HEALER),
            companion_lib.stanceToLabel(companion_lib.STANCE_DPS)
        };
        sui.listbox(self, player, "Choose how this companion supports you in combat.", sui.OK_CANCEL, "Combat role", options, "handleRoleSui", true, false);
        return SCRIPT_CONTINUE;
    }
    public int handleRoleSui(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL || btn == sui.BP_REVERT)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        int stance = companion_lib.STANCE_DPS;
        if (row == 0)
        {
            stance = companion_lib.STANCE_TANK;
        }
        else if (row == 1)
        {
            stance = companion_lib.STANCE_HEALER;
        }
        companion_lib.applyStanceToActivePet(self, stance);
        sendSystemMessage(player, string_id.unlocalized("Companion role set to " + companion_lib.stanceToLabel(stance) + "."));
        return SCRIPT_CONTINUE;
    }
}
