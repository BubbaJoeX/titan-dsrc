package script.systems.companion;

import script.*;
import script.library.*;

/**
 * Extra radial on story-companion pet control devices: combat role (Tank / Healer / Damage) and teach pet-bar abilities from the player’s command list.
 */
public class companion_story_pcd extends script.base_script
{
    public static final int MENU_COMBAT_ROLE = menu_info_types.SERVER_MENU17;
    public static final int MENU_TRAIN_ABILITIES = menu_info_types.SERVER_MENU20;
    public static final int MENU_CLEAR_TAUGHT = menu_info_types.SERVER_MENU21;
    public static final String SV_TRAIN_PCD = "companion.train.pcd";
    public static final String SV_TRAIN_SKILL = "companion.train.skillPick";
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
        mi.addRootMenu(MENU_TRAIN_ABILITIES, string_id.unlocalized("Train Companion Abilities"));
        mi.addRootMenu(MENU_CLEAR_TAUGHT, string_id.unlocalized("Clear Taught Ability Slot"));
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_CLEAR_TAUGHT)
        {
            if (!hasObjVar(self, companion_lib.OBJVAR_STORY_COMPANION_ID))
            {
                return SCRIPT_CONTINUE;
            }
            obj_id pad2 = utils.getPlayerDatapad(player);
            if (getContainedBy(self) != pad2)
            {
                return SCRIPT_CONTINUE;
            }
            utils.setScriptVar(player, SV_TRAIN_PCD, self);
            String[] slots = 
            {
                "Clear slot 1",
                "Clear slot 2",
                "Clear slot 3"
            };
            sui.listbox(self, player, "Remove the taught command from a pet bar slot.", sui.OK_CANCEL, "Clear slot", slots, "handleClearTaughtSlotSui", true, false);
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_TRAIN_ABILITIES)
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
            String[] skills = companion_lib.getCompanionTrainableCommandList(player);
            if (skills == null || skills.length < 1)
            {
                sendSystemMessage(player, string_id.unlocalized("You have no eligible combat commands to teach (requires a script hook in the command table)."));
                return SCRIPT_CONTINUE;
            }
            utils.setScriptVar(player, SV_TRAIN_PCD, self);
            sui.listbox(self, player, "Choose a command you know. It will be queued on your companion from the pet bar (companion cooldown, not yours).", sui.OK_CANCEL, "Train ability", skills, "handleTrainSkillPickSui", true, false);
            return SCRIPT_CONTINUE;
        }
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
        obj_id petOut = callable.getCDCallable(self);
        if (isIdValid(petOut) && exists(petOut) && companion_lib.isStoryCompanionPet(petOut))
        {
            companion_lib.refreshStoryCompanionPetBar(player, petOut);
        }
        sendSystemMessage(player, string_id.unlocalized("Companion role set to " + companion_lib.stanceToLabel(stance) + "."));
        return SCRIPT_CONTINUE;
    }
    public int handleTrainSkillPickSui(obj_id self, dictionary params) throws InterruptedException
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
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        String[] skills = companion_lib.getCompanionTrainableCommandList(player);
        if (skills == null || row < 0 || row >= skills.length)
        {
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(player, SV_TRAIN_SKILL, skills[row]);
        String[] slots = 
        {
            "Slot 1 (pet bar)",
            "Slot 2 (pet bar)",
            "Slot 3 (pet bar)"
        };
        sui.listbox(self, player, "Assign to which pet bar slot?", sui.OK_CANCEL, "Slot", slots, "handleTrainSlotPickSui", true, false);
        return SCRIPT_CONTINUE;
    }
    public int handleTrainSlotPickSui(obj_id self, dictionary params) throws InterruptedException
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
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            utils.removeScriptVar(player, SV_TRAIN_SKILL);
            return SCRIPT_CONTINUE;
        }
        int slotRow = sui.getListboxSelectedRow(params);
        if (slotRow < 0 || slotRow > 2)
        {
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            utils.removeScriptVar(player, SV_TRAIN_SKILL);
            return SCRIPT_CONTINUE;
        }
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            utils.removeScriptVar(player, SV_TRAIN_SKILL);
            return SCRIPT_CONTINUE;
        }
        String skill = utils.getStringScriptVar(player, SV_TRAIN_SKILL);
        companion_lib.setTaughtAbilityOnPcd(pcd, slotRow, skill, player);
        utils.removeScriptVar(player, SV_TRAIN_PCD);
        utils.removeScriptVar(player, SV_TRAIN_SKILL);
        return SCRIPT_CONTINUE;
    }
    public int handleClearTaughtSlotSui(obj_id self, dictionary params) throws InterruptedException
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
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            return SCRIPT_CONTINUE;
        }
        int slotRow = sui.getListboxSelectedRow(params);
        if (slotRow < 0 || slotRow > 2)
        {
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            return SCRIPT_CONTINUE;
        }
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            utils.removeScriptVar(player, SV_TRAIN_PCD);
            return SCRIPT_CONTINUE;
        }
        companion_lib.setTaughtAbilityOnPcd(pcd, slotRow, "empty", player);
        utils.removeScriptVar(player, SV_TRAIN_PCD);
        return SCRIPT_CONTINUE;
    }
}
