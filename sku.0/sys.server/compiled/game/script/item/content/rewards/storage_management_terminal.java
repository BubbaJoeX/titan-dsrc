package script.item.content.rewards;

import script.*;
import script.library.sui;
import script.library.utils;

import java.util.ArrayList;

public class storage_management_terminal extends script.base_script
{
    public static final boolean LOGGING = false;

    private static final String OV_PREFIX   = "smt.rules";
    private static final String OV_COUNT    = OV_PREFIX + ".count";
    private static final String SV_PREFIX   = "smt.session";

    private static final String[] GOT_CATEGORIES = {
        "Armor",
        "Clothing",
        "Component",
        "Data",
        "Deed",
        "Jewelry",
        "Misc",
        "Resource Container",
        "Tool",
        "Weapon",
        "Powerup: Weapon",
        "Powerup: Armor",
        "Ship Component",
    };

    private static final int[] GOT_CATEGORY_IDS = {
        GOT_armor,
        GOT_clothing,
        GOT_component,
        GOT_data,
        GOT_deed,
        GOT_jewelry,
        GOT_misc,
        GOT_resource_container,
        GOT_tool,
        GOT_weapon,
        GOT_powerup_weapon,
        GOT_powerup_armor,
        GOT_ship_component,
    };

    // ---------------------------------------------------------------
    //  Rule data model
    // ---------------------------------------------------------------

    private static String ruleOv(int idx, String field)
    {
        return OV_PREFIX + "." + idx + "." + field;
    }

    private int getRuleCount(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, OV_COUNT))
            return getIntObjVar(self, OV_COUNT);
        return 0;
    }

    private void setRuleCount(obj_id self, int count) throws InterruptedException
    {
        setObjVar(self, OV_COUNT, count);
    }

    private String getRuleType(obj_id self, int idx) throws InterruptedException
    {
        return getStringObjVar(self, ruleOv(idx, "type"));
    }

    private String getRuleValue(obj_id self, int idx) throws InterruptedException
    {
        return getStringObjVar(self, ruleOv(idx, "value"));
    }

    private obj_id getRuleTarget(obj_id self, int idx) throws InterruptedException
    {
        return getObjIdObjVar(self, ruleOv(idx, "target"));
    }

    private String getRuleTargetName(obj_id self, int idx) throws InterruptedException
    {
        return getStringObjVar(self, ruleOv(idx, "target_name"));
    }

    private void setRule(obj_id self, int idx, String type, String value, obj_id target, String targetName) throws InterruptedException
    {
        setObjVar(self, ruleOv(idx, "type"), type);
        setObjVar(self, ruleOv(idx, "value"), value);
        setObjVar(self, ruleOv(idx, "target"), target);
        setObjVar(self, ruleOv(idx, "target_name"), targetName);
    }

    private void removeRuleObjVars(obj_id self, int idx) throws InterruptedException
    {
        removeObjVar(self, OV_PREFIX + "." + idx);
    }

    private void addRule(obj_id self, String type, String value, obj_id target, String targetName) throws InterruptedException
    {
        int count = getRuleCount(self);
        setRule(self, count, type, value, target, targetName);
        setRuleCount(self, count + 1);
    }

    private void removeRule(obj_id self, int idx) throws InterruptedException
    {
        int count = getRuleCount(self);
        if (idx < 0 || idx >= count)
            return;

        removeRuleObjVars(self, idx);

        for (int i = idx + 1; i < count; i++)
        {
            String t = getRuleType(self, i);
            String v = getRuleValue(self, i);
            obj_id tgt = getRuleTarget(self, i);
            String tn = getRuleTargetName(self, i);
            removeRuleObjVars(self, i);
            setRule(self, i - 1, t, v, tgt, tn);
        }

        setRuleCount(self, count - 1);
    }

    private void swapRules(obj_id self, int a, int b) throws InterruptedException
    {
        String tA = getRuleType(self, a);
        String vA = getRuleValue(self, a);
        obj_id gA = getRuleTarget(self, a);
        String nA = getRuleTargetName(self, a);

        setRule(self, a, getRuleType(self, b), getRuleValue(self, b), getRuleTarget(self, b), getRuleTargetName(self, b));
        setRule(self, b, tA, vA, gA, nA);
    }

    private void clearAllRules(obj_id self) throws InterruptedException
    {
        removeObjVar(self, OV_PREFIX);
    }

    private String formatRule(obj_id self, int idx) throws InterruptedException
    {
        String type = getRuleType(self, idx);
        String value = getRuleValue(self, idx);
        String targetName = getRuleTargetName(self, idx);
        String typeLabel;
        if (type.equals("got"))
            typeLabel = "GOT";
        else if (type.equals("template"))
            typeLabel = "Template";
        else
            typeLabel = "Name";
        return (idx + 1) + " " + typeLabel + ": " + value + " - " + targetName;
    }

    // ---------------------------------------------------------------
    //  Container discovery
    // ---------------------------------------------------------------

    private ArrayList<obj_id> getValidContainers(obj_id structure, obj_id self) throws InterruptedException
    {
        ArrayList<obj_id> containers = new ArrayList<>();
        obj_id[] cells = getContents(structure);
        if (cells == null)
            return containers;

        for (obj_id cell : cells)
        {
            if (!isIdValid(cell))
                continue;

            addChildContainers(cell, containers, self);
        }
        return containers;
    }

    private void addChildContainers(obj_id parent, ArrayList<obj_id> out, obj_id self) throws InterruptedException
    {
        obj_id[] contents = getContents(parent);
        if (contents == null)
            return;

        for (obj_id child : contents)
        {
            if (!isIdValid(child) || child.equals(self))
                continue;
            if (isPlayer(child))
                continue;

            int got = getGameObjectType(child);
            if (isGameObjectTypeOf(got, GOT_creature))
                continue;
            if (isGameObjectTypeOf(got, GOT_vendor))
                continue;
            if (isGameObjectTypeOf(got, GOT_terminal))
                continue;
            if (isGameObjectTypeOf(got, GOT_installation))
                continue;

            boolean isContainer = isGameObjectTypeOf(got, GOT_misc_container)
                || isGameObjectTypeOf(got, GOT_misc_container_wearable)
                || isGameObjectTypeOf(got, GOT_misc_container_public)
                || isGameObjectTypeOf(got, GOT_misc_container_ship_loot);

            if (!isContainer)
            {
                obj_id[] sub = getContents(child);
                if (sub != null && sub.length > 0)
                    isContainer = true;
            }

            if (isContainer)
            {
                out.add(child);
                addChildContainers(child, out, self);
            }
        }
    }

    private String getContainerDisplayName(obj_id container) throws InterruptedException
    {
        String name = getEncodedName(container);
        if (name == null || name.isEmpty())
            name = getTemplateName(container);
        return name;
    }

    // ---------------------------------------------------------------
    //  Sortable item scan
    // ---------------------------------------------------------------

    private ArrayList<obj_id> getSortableItems(obj_id structure, obj_id self) throws InterruptedException
    {
        ArrayList<obj_id> items = new ArrayList<>();
        obj_id[] cells = getContents(structure);
        if (cells == null)
            return items;

        for (obj_id cell : cells)
        {
            if (!isIdValid(cell))
                continue;
            collectItems(cell, items, self);
        }
        return items;
    }

    private void collectItems(obj_id parent, ArrayList<obj_id> out, obj_id self) throws InterruptedException
    {
        obj_id[] contents = getContents(parent);
        if (contents == null)
            return;

        for (obj_id child : contents)
        {
            if (!isIdValid(child) || child.equals(self))
                continue;
            if (isPlayer(child))
                continue;

            int got = getGameObjectType(child);
            if (isGameObjectTypeOf(got, GOT_vendor))
                continue;
            if (isGameObjectTypeOf(got, GOT_static))
                continue;
            if (isGameObjectTypeOf(got, GOT_installation))
                continue;
            if (isGameObjectTypeOf(got, GOT_terminal))
                continue;
            if (isGameObjectTypeOf(got, GOT_creature))
                continue;

            String tpl = getTemplateName(child);
            if (tpl.contains("visible_crafting_station") || tpl.startsWith("object/tangible/hopper"))
                continue;

            boolean isContainer = isGameObjectTypeOf(got, GOT_misc_container)
                || isGameObjectTypeOf(got, GOT_misc_container_wearable)
                || isGameObjectTypeOf(got, GOT_misc_container_public)
                || isGameObjectTypeOf(got, GOT_misc_container_ship_loot);

            if (isContainer)
            {
                collectItems(child, out, self);
            }
            else
            {
                out.add(child);

                obj_id[] sub = getContents(child);
                if (sub != null && sub.length > 0)
                    collectItems(child, out, self);
            }
        }
    }

    // ---------------------------------------------------------------
    //  Sort engine
    // ---------------------------------------------------------------

    private boolean itemMatchesRule(obj_id item, String ruleType, String ruleValue) throws InterruptedException
    {
        if (ruleType.equals("got"))
        {
            int gotId = getGameObjectTypeFromName(ruleValue);
            if (gotId <= 0)
                return false;
            return isGameObjectTypeOf(item, gotId);
        }
        else if (ruleType.equals("template"))
        {
            String tpl = getTemplateName(item);
            return tpl != null && tpl.toLowerCase().contains(ruleValue.toLowerCase());
        }
        else if (ruleType.equals("name"))
        {
            String name = getEncodedName(item);
            return name != null && name.toLowerCase().contains(ruleValue.toLowerCase());
        }
        return false;
    }

    private int[] executeSortRules(obj_id self, obj_id player, obj_id structure) throws InterruptedException
    {
        int ruleCount = getRuleCount(self);
        int moved = 0;
        int noMatch = 0;
        int failed = 0;

        if (ruleCount == 0)
            return new int[]{0, 0, 0};

        ArrayList<obj_id> items = getSortableItems(structure, self);

        for (obj_id item : items)
        {
            boolean matched = false;
            for (int r = 0; r < ruleCount; r++)
            {
                String rType = getRuleType(self, r);
                String rValue = getRuleValue(self, r);
                obj_id target = getRuleTarget(self, r);

                if (!isIdValid(target))
                    continue;

                if (itemMatchesRule(item, rType, rValue))
                {
                    matched = true;

                    obj_id currentContainer = getContainedBy(item);
                    if (currentContainer.equals(target))
                    {
                        moved++;
                        break;
                    }

                    if (putIn(item, target))
                    {
                        moved++;
                    }
                    else
                    {
                        failed++;
                        blog("Failed to move " + item + " to " + target);
                    }
                    break;
                }
            }
            if (!matched)
                noMatch++;
        }

        return new int[]{moved, noMatch, failed};
    }

    // ---------------------------------------------------------------
    //  Session script-var helpers
    // ---------------------------------------------------------------

    private static String svKey(String key)
    {
        return SV_PREFIX + "." + key;
    }

    private void svSetString(obj_id player, String key, String val)
    {
        utils.setScriptVar(player, svKey(key), val);
    }

    private void svSetInt(obj_id player, String key, int val)
    {
        utils.setScriptVar(player, svKey(key), val);
    }

    private void svSetObjId(obj_id player, String key, obj_id val)
    {
        utils.setScriptVar(player, svKey(key), val);
    }

    private void svSetObjIdArray(obj_id player, String key, obj_id[] val)
    {
        utils.setScriptVar(player, svKey(key), val);
    }

    private String svGetString(obj_id player, String key)
    {
        return utils.getStringScriptVar(player, svKey(key));
    }

    private int svGetInt(obj_id player, String key)
    {
        return utils.getIntScriptVar(player, svKey(key));
    }

    private obj_id svGetObjId(obj_id player, String key)
    {
        return utils.getObjIdScriptVar(player, svKey(key));
    }

    private obj_id[] svGetObjIdArray(obj_id player, String key)
    {
        return utils.getObjIdArrayScriptVar(player, svKey(key));
    }

    private void svClear(obj_id player)
    {
        utils.removeScriptVarTree(player, SV_PREFIX);
    }

    // ---------------------------------------------------------------
    //  Triggers
    // ---------------------------------------------------------------

    public int OnAttach(obj_id self)
    {
        blog("Entered OnAttach");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        blog("Entered OnInitialize");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (canManipulate(player, self, true, true, 15, true) || isGod(player))
        {
            mi.addRootMenu(menu_info_types.ITEM_USE, toDummy("Manage Storage"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE)
            return SCRIPT_CONTINUE;

        obj_id structure = getTopMostContainer(self);
        if (!isIdValid(structure))
        {
            broadcast(player, "This terminal must be placed inside a structure");
            return SCRIPT_CONTINUE;
        }

        obj_id owner = getOwner(structure);
        if (!player.equals(owner) && !isGod(player))
        {
            broadcast(player, "You must be the owner of this structure to use this terminal");
            return SCRIPT_CONTINUE;
        }

        svSetObjId(player, "structure", structure);
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Main menu
    // ---------------------------------------------------------------

    private void showMainMenu(obj_id self, obj_id player) throws InterruptedException
    {
        int ruleCount = getRuleCount(self);
        String[] options = {
            "View Rules - " + ruleCount + " configured",
            "Add Rule",
            "Sort Now",
            "Clear All Rules"
        };
        sui.listbox(self, player, "Storage Management Terminal\nConfigure item routing rules; then Sort to move items to their assigned containers", sui.OK_CANCEL, "Storage Management", options, "handleMainMenu", true);
    }

    public int handleMainMenu(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            svClear(player);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        switch (idx)
        {
            case 0:
                showRuleList(self, player);
                break;
            case 1:
                showAddRuleTypeMenu(self, player);
                break;
            case 2:
                doSort(self, player);
                break;
            case 3:
                showClearConfirm(self, player);
                break;
            default:
                svClear(player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  View / edit rules
    // ---------------------------------------------------------------

    private void showRuleList(obj_id self, obj_id player) throws InterruptedException
    {
        int count = getRuleCount(self);
        if (count == 0)
        {
            sui.msgbox(self, player, "No rules configured yet; use Add Rule to create one", sui.OK_ONLY, "Storage Management", "handleRuleListBack");
            return;
        }

        String[] entries = new String[count];
        for (int i = 0; i < count; i++)
            entries[i] = formatRule(self, i);

        sui.listbox(self, player, "Select a rule to manage", sui.OK_CANCEL, "Rules", entries, "handleRuleSelect", true);
    }

    public int handleRuleListBack(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleRuleSelect(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            showMainMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0 || idx >= getRuleCount(self))
        {
            showMainMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        svSetInt(player, "editIdx", idx);
        String ruleText = formatRule(self, idx);

        String[] actions = {"Delete", "Move Up", "Move Down", "Back to Rules"};
        sui.listbox(self, player, "Rule: " + ruleText + "\nSelect an action", sui.OK_CANCEL, "Manage Rule", actions, "handleRuleAction", true);
        return SCRIPT_CONTINUE;
    }

    public int handleRuleAction(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            showRuleList(self, player);
            return SCRIPT_CONTINUE;
        }

        int action = sui.getListboxSelectedRow(params);
        int ruleIdx = svGetInt(player, "editIdx");
        int count = getRuleCount(self);

        switch (action)
        {
            case 0: // Delete
                removeRule(self, ruleIdx);
                broadcast(player, "Rule deleted");
                showRuleList(self, player);
                break;
            case 1: // Move Up
                if (ruleIdx > 0)
                {
                    swapRules(self, ruleIdx, ruleIdx - 1);
                    broadcast(player, "Rule moved up");
                }
                showRuleList(self, player);
                break;
            case 2: // Move Down
                if (ruleIdx < count - 1)
                {
                    swapRules(self, ruleIdx, ruleIdx + 1);
                    broadcast(player, "Rule moved down");
                }
                showRuleList(self, player);
                break;
            case 3: // Back
                showRuleList(self, player);
                break;
            default:
                showRuleList(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Add rule wizard — step 1: match type
    // ---------------------------------------------------------------

    private void showAddRuleTypeMenu(obj_id self, obj_id player) throws InterruptedException
    {
        String[] types = {"Game Object Type - category", "Template Name - substring", "Item Name - substring"};
        sui.listbox(self, player, "Select what this rule should match on", sui.OK_CANCEL, "Add Rule - Match Type", types, "handleAddRuleType", true);
    }

    public int handleAddRuleType(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            showMainMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        switch (idx)
        {
            case 0:
                svSetString(player, "newRuleType", "got");
                showGotCategoryMenu(self, player);
                break;
            case 1:
                svSetString(player, "newRuleType", "template");
                sui.inputbox(self, player, "Enter a template name substring to match:", "Add Rule - Template", "handleAddRuleValue", "");
                break;
            case 2:
                svSetString(player, "newRuleType", "name");
                sui.inputbox(self, player, "Enter an item name substring to match:", "Add Rule - Item Name", "handleAddRuleValue", "");
                break;
            default:
                showMainMenu(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Add rule wizard — step 2a: GOT category selection
    // ---------------------------------------------------------------

    private void showGotCategoryMenu(obj_id self, obj_id player) throws InterruptedException
    {
        sui.listbox(self, player, "Select a Game Object Type category", sui.OK_CANCEL, "Add Rule - Category", GOT_CATEGORIES, "handleAddRuleGotCategory", true);
    }

    public int handleAddRuleGotCategory(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            showAddRuleTypeMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0 || idx >= GOT_CATEGORIES.length)
        {
            showAddRuleTypeMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        String gotName = getGameObjectTypeName(GOT_CATEGORY_IDS[idx]);
        svSetString(player, "newRuleValue", gotName);
        svSetString(player, "newRuleDisplayValue", GOT_CATEGORIES[idx]);
        showTargetContainerMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Add rule wizard — step 2b: freeform text input
    // ---------------------------------------------------------------

    public int handleAddRuleValue(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            showAddRuleTypeMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        String text = sui.getInputBoxText(params);
        if (text == null || text.trim().isEmpty())
        {
            broadcast(player, "Value cannot be empty");
            showAddRuleTypeMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        svSetString(player, "newRuleValue", text.trim());
        svSetString(player, "newRuleDisplayValue", text.trim());
        showTargetContainerMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Add rule wizard — step 3: target container
    // ---------------------------------------------------------------

    private void showTargetContainerMenu(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id structure = svGetObjId(player, "structure");
        if (structure == null || !isIdValid(structure))
        {
            broadcast(player, "Structure not found");
            showMainMenu(self, player);
            return;
        }

        ArrayList<obj_id> containers = getValidContainers(structure, self);
        if (containers.isEmpty())
        {
            broadcast(player, "No valid containers found in this structure");
            showMainMenu(self, player);
            return;
        }

        String[] names = new String[containers.size()];
        obj_id[] ids = new obj_id[containers.size()];
        for (int i = 0; i < containers.size(); i++)
        {
            ids[i] = containers.get(i);
            names[i] = getContainerDisplayName(ids[i]);
        }

        svSetObjIdArray(player, "containerIds", ids);
        sui.listbox(self, player, "Select the destination container for this rule", sui.OK_CANCEL, "Add Rule - Destination", names, "handleAddRuleTarget", true);
    }

    public int handleAddRuleTarget(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            showAddRuleTypeMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        obj_id[] containerIds = svGetObjIdArray(player, "containerIds");
        if (containerIds == null || idx < 0 || idx >= containerIds.length)
        {
            showMainMenu(self, player);
            return SCRIPT_CONTINUE;
        }

        String ruleType = svGetString(player, "newRuleType");
        String ruleValue = svGetString(player, "newRuleValue");
        obj_id target = containerIds[idx];
        String targetName = getContainerDisplayName(target);

        addRule(self, ruleType, ruleValue, target, targetName);

        String displayValue = svGetString(player, "newRuleDisplayValue");
        if (displayValue == null)
            displayValue = ruleValue;
        broadcast(player, "Rule added: " + ruleType.toUpperCase() + " " + displayValue + " - " + targetName);

        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Sort execution
    // ---------------------------------------------------------------

    private void doSort(obj_id self, obj_id player) throws InterruptedException
    {
        int ruleCount = getRuleCount(self);
        if (ruleCount == 0)
        {
            broadcast(player, "No rules configured; add rules first");
            showMainMenu(self, player);
            return;
        }

        obj_id structure = svGetObjId(player, "structure");
        if (structure == null || !isIdValid(structure))
        {
            broadcast(player, "Structure not found");
            return;
        }

        int[] results = executeSortRules(self, player, structure);
        int moved = results[0];
        int noMatch = results[1];
        int failed = results[2];

        String summary = "Sort complete\n\n"
            + "Items routed: " + moved + "\n"
            + "No matching rule: " + noMatch + "\n"
            + "Failed to move: " + failed;

        sui.msgbox(self, player, summary, sui.OK_ONLY, "Sort Results", "handleSortDone");
    }

    public int handleSortDone(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Clear all rules
    // ---------------------------------------------------------------

    private void showClearConfirm(obj_id self, obj_id player) throws InterruptedException
    {
        sui.msgbox(self, player, "Are you sure you want to delete ALL routing rules", sui.OK_CANCEL, "Confirm Clear", "handleClearConfirm");
    }

    public int handleClearConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_OK)
        {
            clearAllRules(self);
            broadcast(player, "All rules cleared");
        }
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ---------------------------------------------------------------
    //  Utilities
    // ---------------------------------------------------------------

    public string_id toDummy(String txt)
    {
        return new string_id(txt);
    }

    public void blog(String message)
    {
        if (LOGGING)
        {
            LOG("ethereal", "[SMT]: " + message);
        }
    }
}
