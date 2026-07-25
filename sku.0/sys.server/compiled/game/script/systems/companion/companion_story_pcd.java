package script.systems.companion;

import script.*;
import script.library.*;

/**
 * Story-companion pet control device: one radial entry opens a comprehensive SUI hub for role, training, and bar management.
 */
public class companion_story_pcd extends script.base_script
{
    public static final int MENU_COMPANION_OPTIONS = menu_info_types.SERVER_MENU17;
    public static final String SV_TRAIN_PCD = "companion.train.pcd";
    public static final String SV_TRAIN_SKILL = "companion.train.skillPick";
    public static final String SV_TRAIN_CORE_SLOT = "companion.train.coreSlot";
    public static final String SV_TRAIN_COMMANDS = "companion.train.commands";
    /** Main hub listbox row indices. */
    public static final int HUB_ROLE_TANK = 0;
    public static final int HUB_ROLE_HEALER = 1;
    public static final int HUB_ROLE_DPS = 2;
    public static final int HUB_TRAIN_ABILITY = 3;
    public static final int HUB_CLEAR_ABILITY = 4;
    public static final int HUB_TRAIN_CORE = 5;
    public static final int HUB_CLEAR_CORE = 6;
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
        mi.addRootMenu(MENU_COMPANION_OPTIONS, string_id.unlocalized("Companion Options"));
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != MENU_COMPANION_OPTIONS)
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, companion_lib.OBJVAR_STORY_COMPANION_ID))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id pad = utils.getPlayerDatapad(player);
        if (getContainedBy(self) != pad)
        {
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(player, SV_TRAIN_PCD, self);
        openCompanionHubSui(self, player);
        return SCRIPT_CONTINUE;
    }
    private void openCompanionHubSui(obj_id self, obj_id player) throws InterruptedException
    {
        String[] options =
        {
            "Combat Role: Tank",
            "Combat Role: Healer",
            "Combat Role: Damage",
            "Train Ability Slot...",
            "Clear Ability Slot...",
            "Train Core Bar Slot...",
            "Clear Core Bar Slot..."
        };
        sui.listbox(self, player, "Manage combat role, taught abilities, and core pet bar slots.", sui.OK_CANCEL, "Companion Options", options, "handleCompanionHubSui", true, false);
    }
    public int handleCompanionHubSui(obj_id self, dictionary params) throws InterruptedException
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
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_ROLE_TANK)
        {
            applyRoleAndRefresh(self, player, companion_lib.STANCE_TANK);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_ROLE_HEALER)
        {
            applyRoleAndRefresh(self, player, companion_lib.STANCE_HEALER);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_ROLE_DPS)
        {
            applyRoleAndRefresh(self, player, companion_lib.STANCE_DPS);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_TRAIN_ABILITY)
        {
            openTrainAbilityCommandSui(self, player);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_CLEAR_ABILITY)
        {
            openClearTaughtSlotSui(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_TRAIN_CORE)
        {
            openTrainCoreSlotSui(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        if (row == HUB_CLEAR_CORE)
        {
            openClearCoreSlotSui(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    private void applyRoleAndRefresh(obj_id self, obj_id player, int stance) throws InterruptedException
    {
        companion_lib.applyStanceToActivePet(self, stance);
        obj_id petOut = callable.getCDCallable(self);
        if (isIdValid(petOut) && exists(petOut) && companion_lib.isStoryCompanionPet(petOut))
        {
            companion_lib.refreshStoryCompanionPetBar(player, petOut);
        }
        sendSystemMessage(player, string_id.unlocalized("Companion role set to " + companion_lib.stanceToLabel(stance) + "."));
    }
    private void openTrainAbilityCommandSui(obj_id self, obj_id player) throws InterruptedException
    {
        String[] skills = companion_lib.getCompanionTrainableCommandList(player);
        if (skills == null || skills.length < 1)
        {
            sendSystemMessage(player, string_id.unlocalized("You have no eligible combat commands to teach (requires a script hook in the command table)."));
            clearTrainScriptVars(player);
            return;
        }
        utils.setScriptVar(player, SV_TRAIN_COMMANDS, skills);
        prose_package[] rows = companion_lib.buildCommandProseList(skills);
        sui.listbox(self, player, "Choose a command you know. It will be queued on your companion from the pet bar (companion cooldown, not yours).", sui.OK_CANCEL, "Train ability", rows, "handleTrainSkillPickSui", true, false);
    }
    private void openClearTaughtSlotSui(obj_id self, obj_id player, obj_id pcd) throws InterruptedException
    {
        String[] taught = companion_lib.getTaughtAbilitiesArray(pcd);
        String[] slots = new String[companion_lib.TAUGHT_SLOT_COUNT];
        for (int i = 0; i < companion_lib.TAUGHT_SLOT_COUNT; ++i)
        {
            String cmd = (taught != null && i < taught.length) ? taught[i] : "empty";
            slots[i] = companion_lib.createCompanionBarSlotPickerEntry("Ability slot " + (i + 1), cmd);
        }
        sui.listbox(self, player, "Remove the taught command from a pet bar slot.", sui.OK_CANCEL, "Clear ability slot", slots, "handleClearTaughtSlotSui", true, false);
    }
    private void openTrainCoreSlotSui(obj_id self, obj_id player, obj_id pcd) throws InterruptedException
    {
        String[] core = companion_lib.getCoreBarCommandsArray(pcd);
        String[] slots = new String[companion_lib.CORE_BAR_SLOT_COUNT];
        for (int i = 0; i < companion_lib.CORE_BAR_SLOT_COUNT; ++i)
        {
            String cmd = (core != null && i < core.length) ? core[i] : "empty";
            slots[i] = companion_lib.createCompanionBarSlotPickerEntry("Core slot " + (i + 1), cmd);
        }
        sui.listbox(self, player, "Choose which core pet bar slot to program (attack / follow / stay style).", sui.OK_CANCEL, "Core slot", slots, "handleTrainCoreBarSlotSui", true, false);
    }
    private void openClearCoreSlotSui(obj_id self, obj_id player, obj_id pcd) throws InterruptedException
    {
        String[] core = companion_lib.getCoreBarCommandsArray(pcd);
        String[] slots = new String[companion_lib.CORE_BAR_SLOT_COUNT];
        for (int i = 0; i < companion_lib.CORE_BAR_SLOT_COUNT; ++i)
        {
            String cmd = (core != null && i < core.length) ? core[i] : "empty";
            slots[i] = companion_lib.createCompanionBarSlotPickerEntry("Core slot " + (i + 1), cmd);
        }
        sui.listbox(self, player, "Clear a programmable core bar slot.", sui.OK_CANCEL, "Clear core slot", slots, "handleClearCoreBarSlotSui", true, false);
    }
    private void clearTrainScriptVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, SV_TRAIN_PCD);
        utils.removeScriptVar(player, SV_TRAIN_SKILL);
        utils.removeScriptVar(player, SV_TRAIN_CORE_SLOT);
        utils.removeScriptVar(player, SV_TRAIN_COMMANDS);
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
            openCompanionHubSui(self, player);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        String[] skills = utils.getStringArrayScriptVar(player, SV_TRAIN_COMMANDS);
        if (skills == null || row < 0 || row >= skills.length)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(player, SV_TRAIN_SKILL, skills[row]);
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        String[] taught = companion_lib.getTaughtAbilitiesArray(pcd);
        String[] slots = new String[companion_lib.TAUGHT_SLOT_COUNT];
        for (int i = 0; i < companion_lib.TAUGHT_SLOT_COUNT; ++i)
        {
            String cmd = (taught != null && i < taught.length) ? taught[i] : "empty";
            slots[i] = companion_lib.createCompanionBarSlotPickerEntry("Ability slot " + (i + 1), cmd);
        }
        sui.listbox(self, player, "Assign to which pet bar slot?", sui.OK_CANCEL, "Ability slot", slots, "handleTrainSlotPickSui", true, false);
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
            openTrainAbilityCommandSui(self, player);
            return SCRIPT_CONTINUE;
        }
        int slotRow = sui.getListboxSelectedRow(params);
        if (slotRow < 0 || slotRow >= companion_lib.TAUGHT_SLOT_COUNT)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        String skill = utils.getStringScriptVar(player, SV_TRAIN_SKILL);
        companion_lib.setTaughtAbilityOnPcd(pcd, slotRow, skill, player);
        clearTrainScriptVars(player);
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
            openCompanionHubSui(self, player);
            return SCRIPT_CONTINUE;
        }
        int slotRow = sui.getListboxSelectedRow(params);
        if (slotRow < 0 || slotRow >= companion_lib.TAUGHT_SLOT_COUNT)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        companion_lib.setTaughtAbilityOnPcd(pcd, slotRow, "empty", player);
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    public int handleTrainCoreBarSlotSui(obj_id self, dictionary params) throws InterruptedException
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
            openCompanionHubSui(self, player);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        if (row < 0 || row >= companion_lib.CORE_BAR_SLOT_COUNT)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(player, SV_TRAIN_CORE_SLOT, row);
        String[] skills = companion_lib.getCompanionCoreBarTrainableCommandList(player);
        if (skills == null || skills.length < 1)
        {
            sendSystemMessage(player, string_id.unlocalized("You have no eligible commands for the core bar (try beastmaster pet commands such as attack / follow / stay)."));
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(player, SV_TRAIN_COMMANDS, skills);
        prose_package[] rows = companion_lib.buildCommandProseList(skills);
        sui.listbox(self, player, "Pick the command this core slot will run (queued on the companion).", sui.OK_CANCEL, "Core command", rows, "handleTrainCoreBarSkillSui", true, false);
        return SCRIPT_CONTINUE;
    }
    public int handleTrainCoreBarSkillSui(obj_id self, dictionary params) throws InterruptedException
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
            obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
            if (isIdValid(pcd))
            {
                openTrainCoreSlotSui(self, player, pcd);
            }
            else
            {
                clearTrainScriptVars(player);
            }
            return SCRIPT_CONTINUE;
        }
        int skillRow = sui.getListboxSelectedRow(params);
        String[] skills = utils.getStringArrayScriptVar(player, SV_TRAIN_COMMANDS);
        if (skills == null || skillRow < 0 || skillRow >= skills.length)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        int coreSlot = utils.getIntScriptVar(player, SV_TRAIN_CORE_SLOT);
        if (coreSlot < 0 || coreSlot >= companion_lib.CORE_BAR_SLOT_COUNT)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        companion_lib.setCoreBarCommandOnPcd(pcd, coreSlot, skills[skillRow], player);
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    public int handleClearCoreBarSlotSui(obj_id self, dictionary params) throws InterruptedException
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
            openCompanionHubSui(self, player);
            return SCRIPT_CONTINUE;
        }
        int slotRow = sui.getListboxSelectedRow(params);
        if (slotRow < 0 || slotRow >= companion_lib.CORE_BAR_SLOT_COUNT)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        obj_id pcd = utils.getObjIdScriptVar(player, SV_TRAIN_PCD);
        if (!isIdValid(pcd) || !exists(pcd) || pcd != self)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        companion_lib.setCoreBarCommandOnPcd(pcd, slotRow, "empty", player);
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
}
