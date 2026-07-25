package script.systems.companion;

import script.*;
import script.library.*;

import java.util.Vector;

/**
 * Story-companion pet control device: radial entry opens a comprehensive SUI hub for role, naming,
 * appearance (full tier), weapon management, and pet bar training.
 */
public class companion_story_pcd extends script.base_script
{
    public static final int MENU_COMPANION_OPTIONS = menu_info_types.SERVER_MENU17;
    public static final String SV_TRAIN_PCD = "companion.train.pcd";
    public static final String SV_TRAIN_SKILL = "companion.train.skillPick";
    public static final String SV_TRAIN_CORE_SLOT = "companion.train.coreSlot";
    public static final String SV_TRAIN_COMMANDS = "companion.train.commands";
    public static final String SV_HUB_ACTIONS = "companion.hub.actions";
    public static final String SV_ROW_IDS = "companion.appearance.rowIds";
    public static final String ACTION_ROLE_TANK = "role_tank";
    public static final String ACTION_ROLE_HEALER = "role_healer";
    public static final String ACTION_ROLE_DPS = "role_dps";
    public static final String ACTION_RENAME = "rename";
    public static final String ACTION_ADD_WEARABLE = "add_wearable";
    public static final String ACTION_REMOVE_WEARABLE = "remove_wearable";
    public static final String ACTION_WEAPON_INFO = "weapon_info";
    public static final String ACTION_WEAPON_CLEAR = "weapon_clear";
    public static final String ACTION_WEAPON_RETURN = "weapon_return";
    public static final String ACTION_TRAIN_ABILITY = "train_ability";
    public static final String ACTION_CLEAR_ABILITY = "clear_ability";
    public static final String ACTION_TRAIN_CORE = "train_core";
    public static final String ACTION_CLEAR_CORE = "clear_core";
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
        String hubLabel = "Companion Options";
        if (companion_lib.canRenameStoryCompanion(self))
        {
            String dn = companion_lib.getCompanionDisplayName(self);
            if (dn != null && dn.length() > 0)
            {
                hubLabel = dn + " - Options";
            }
        }
        mi.addRootMenu(MENU_COMPANION_OPTIONS, string_id.unlocalized(hubLabel));
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
        Vector labels = new Vector();
        Vector actions = new Vector();
        labels.add("Combat Role: Tank");
        actions.add(ACTION_ROLE_TANK);
        labels.add("Combat Role: Healer");
        actions.add(ACTION_ROLE_HEALER);
        labels.add("Combat Role: Damage");
        actions.add(ACTION_ROLE_DPS);
        if (companion_lib.canRenameStoryCompanion(self))
        {
            labels.add("Rename Companion...");
            actions.add(ACTION_RENAME);
        }
        if (companion_lib.canCustomizeStoryCompanionAppearance(self))
        {
            labels.add("Customize Appearance: Add Wearable...");
            actions.add(ACTION_ADD_WEARABLE);
            labels.add("Customize Appearance: Remove Wearable...");
            actions.add(ACTION_REMOVE_WEARABLE);
        }
        labels.add("Weapon: " + companion_lib.describeStoryCompanionWeapon(self));
        actions.add(ACTION_WEAPON_INFO);
        labels.add("Clear Companion Weapon");
        actions.add(ACTION_WEAPON_CLEAR);
        labels.add("Return Weapon to Inventory");
        actions.add(ACTION_WEAPON_RETURN);
        labels.add("Train Ability Slot...");
        actions.add(ACTION_TRAIN_ABILITY);
        labels.add("Clear Ability Slot...");
        actions.add(ACTION_CLEAR_ABILITY);
        labels.add("Train Core Bar Slot...");
        actions.add(ACTION_TRAIN_CORE);
        labels.add("Clear Core Bar Slot...");
        actions.add(ACTION_CLEAR_CORE);
        String[] options = new String[labels.size()];
        String[] actionList = new String[actions.size()];
        for (int i = 0; i < labels.size(); ++i)
        {
            options[i] = (String)labels.get(i);
            actionList[i] = (String)actions.get(i);
        }
        utils.setScriptVar(player, SV_HUB_ACTIONS, actionList);
        String title = companion_lib.getCompanionDisplayName(self);
        if (title == null || title.length() < 1)
        {
            title = "Companion Options";
        }
        else
        {
            title = title + " - Options";
        }
        sui.listbox(self, player, "Manage combat role, name, appearance, weapon, taught abilities, and core pet bar slots.", sui.OK_CANCEL, title, options, "handleCompanionHubSui", true, false);
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
        String[] actionList = utils.getStringArrayScriptVar(player, SV_HUB_ACTIONS);
        if (actionList == null || row < 0 || row >= actionList.length)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        String action = actionList[row];
        if (ACTION_ROLE_TANK.equals(action))
        {
            applyRoleAndRefresh(self, player, companion_lib.STANCE_TANK);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_ROLE_HEALER.equals(action))
        {
            applyRoleAndRefresh(self, player, companion_lib.STANCE_HEALER);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_ROLE_DPS.equals(action))
        {
            applyRoleAndRefresh(self, player, companion_lib.STANCE_DPS);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_RENAME.equals(action))
        {
            sui.inputbox(self, player, "Enter a display name for this companion (32 characters max).", "Rename Companion", "handleRenameSui", companion_lib.getCompanionDisplayName(self));
            return SCRIPT_CONTINUE;
        }
        if (ACTION_ADD_WEARABLE.equals(action))
        {
            showAddWearableListbox(self, player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_REMOVE_WEARABLE.equals(action))
        {
            showRemoveWearableListbox(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_WEAPON_INFO.equals(action))
        {
            sendSystemMessage(player, string_id.unlocalized("Companion weapon: " + companion_lib.describeStoryCompanionWeapon(self) + ". Drag a weapon onto your active companion to equip."));
            openCompanionHubSui(self, player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_WEAPON_CLEAR.equals(action))
        {
            companion_lib.clearStoryCompanionWeapon(self, player);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_WEAPON_RETURN.equals(action))
        {
            companion_lib.returnWeaponToPlayerFromPcd(self, player);
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_TRAIN_ABILITY.equals(action))
        {
            openTrainAbilityCommandSui(self, player);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_CLEAR_ABILITY.equals(action))
        {
            openClearTaughtSlotSui(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_TRAIN_CORE.equals(action))
        {
            openTrainCoreSlotSui(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        if (ACTION_CLEAR_CORE.equals(action))
        {
            openClearCoreSlotSui(self, player, pcd);
            return SCRIPT_CONTINUE;
        }
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    public int handleRenameSui(obj_id self, dictionary params) throws InterruptedException
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
        String name = sui.getInputBoxText(params);
        companion_lib.setCompanionDisplayName(self, name);
        sendSystemMessage(player, string_id.unlocalized("Companion renamed to " + companion_lib.getCompanionDisplayName(self) + "."));
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    private void showAddWearableListbox(obj_id self, obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, SV_ROW_IDS);
        obj_id pInv = utils.getInventoryContainer(player);
        if (!isIdValid(pInv))
        {
            sendSystemMessage(player, string_id.unlocalized("No player inventory."));
            clearTrainScriptVars(player);
            return;
        }
        obj_id[] contents = getContents(pInv);
        Vector labelVec = new Vector();
        Vector idVec = new Vector();
        if (contents != null)
        {
            for (obj_id item : contents)
            {
                if (companion_lib.isCompanionDressableTangible(item))
                {
                    idVec.add(item);
                    String nm = getName(item);
                    String st = getStaticItemName(item);
                    labelVec.add((nm != null ? nm : "?") + " - " + (st != null ? st : getTemplateName(item)));
                }
            }
        }
        if (idVec.size() == 0)
        {
            sendSystemMessage(player, string_id.unlocalized("No equippable armor, clothing, jewelry, or cybernetics in your inventory."));
            openCompanionHubSui(self, player);
            return;
        }
        obj_id[] ids = new obj_id[idVec.size()];
        for (int i = 0; i < idVec.size(); ++i)
        {
            ids[i] = (obj_id)idVec.get(i);
        }
        utils.setScriptVar(player, SV_ROW_IDS, ids);
        String[] rows = new String[labelVec.size()];
        for (int i = 0; i < labelVec.size(); ++i)
        {
            rows[i] = (String)labelVec.get(i);
        }
        sui.listbox(self, player, "Choose an item from your inventory to equip on this companion.", sui.OK_CANCEL, "Companion: add wearable", rows, "handleAddWearablePick", true, false);
    }
    public int handleAddWearablePick(obj_id self, dictionary params) throws InterruptedException
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
        obj_id[] ids = utils.getObjIdArrayScriptVar(player, SV_ROW_IDS);
        if (ids == null || row < 0 || row >= ids.length)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        companion_lib.equipCompanionWearableFromPlayer(self, player, ids[row]);
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    private void showRemoveWearableListbox(obj_id self, obj_id player, obj_id pcd) throws InterruptedException
    {
        utils.removeScriptVar(player, SV_ROW_IDS);
        Vector labelVec = new Vector();
        Vector idVec = new Vector();
        obj_id pet = callable.getCDCallable(pcd);
        if (isIdValid(pet) && exists(pet))
        {
            obj_id[] equipped = getAllWornItems(pet, false);
            if (equipped != null)
            {
                for (int i = 0; i < equipped.length; ++i)
                {
                    addWearableRow(idVec, labelVec, equipped[i]);
                }
            }
            obj_id appInv = getAppearanceInventory(pet);
            if (isIdValid(appInv))
            {
                obj_id[] inInv = getContents(appInv);
                if (inInv != null)
                {
                    for (int i = 0; i < inInv.length; ++i)
                    {
                        addWearableRow(idVec, labelVec, inInv[i]);
                    }
                }
            }
        }
        if (hasObjVar(pcd, companion_lib.OBJVAR_GEAR_HOLD))
        {
            obj_id hold = getObjIdObjVar(pcd, companion_lib.OBJVAR_GEAR_HOLD);
            if (isIdValid(hold))
            {
                obj_id[] stored = getContents(hold);
                if (stored != null)
                {
                    for (int i = 0; i < stored.length; ++i)
                    {
                        if (companion_lib.isCompanionDressableTangible(stored[i]) && !isWeapon(stored[i]))
                        {
                            addWearableRow(idVec, labelVec, stored[i]);
                        }
                    }
                }
            }
        }
        if (idVec.size() == 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Nothing equipped or stored for this companion to remove."));
            openCompanionHubSui(self, player);
            return;
        }
        obj_id[] ids = new obj_id[idVec.size()];
        for (int i = 0; i < idVec.size(); ++i)
        {
            ids[i] = (obj_id)idVec.get(i);
        }
        utils.setScriptVar(player, SV_ROW_IDS, ids);
        String[] rows = new String[labelVec.size()];
        for (int i = 0; i < labelVec.size(); ++i)
        {
            rows[i] = (String)labelVec.get(i);
        }
        sui.listbox(self, player, "Choose a worn or stored piece to remove to your inventory.", sui.OK_CANCEL, "Companion: remove wearable", rows, "handleRemoveWearablePick", true, false);
    }
    public int handleRemoveWearablePick(obj_id self, dictionary params) throws InterruptedException
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
        obj_id[] ids = utils.getObjIdArrayScriptVar(player, SV_ROW_IDS);
        if (ids == null || row < 0 || row >= ids.length)
        {
            clearTrainScriptVars(player);
            return SCRIPT_CONTINUE;
        }
        companion_lib.removeCompanionWearableToPlayer(self, player, ids[row]);
        clearTrainScriptVars(player);
        return SCRIPT_CONTINUE;
    }
    private static void addWearableRow(Vector idVec, Vector labelVec, obj_id item) throws InterruptedException
    {
        if (!isIdValid(item) || !exists(item))
        {
            return;
        }
        for (int i = 0; i < idVec.size(); ++i)
        {
            if (((obj_id)idVec.get(i)).equals(item))
            {
                return;
            }
        }
        idVec.add(item);
        String nm = getName(item);
        labelVec.add(nm != null ? nm : getTemplateName(item));
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
        utils.removeScriptVar(player, SV_HUB_ACTIONS);
        utils.removeScriptVar(player, SV_ROW_IDS);
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
