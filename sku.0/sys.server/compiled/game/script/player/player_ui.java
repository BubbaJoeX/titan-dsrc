package script.player;

import script.dictionary;
import script.library.buff;
import script.library.static_item;
import script.library.sui;
import script.library.utils;
import script.obj_id;
import script.location;

/**
 * Admin Panel UI - Comprehensive administration interface
 * Uses custom SUI page: Script.adminPanel
 * Provides lookup and management for: Items, Creatures, Buffs, Skills
 * Supports spawn (for items/creatures) and grant (for buffs/skills)
 *
 * @author Titan Admin System
 * @version 2.0
 */
public class player_ui extends script.base_script
{
    // ========================================
    // CONSTANTS
    // ========================================

    // God level requirements
    private static final int GOD_LEVEL_REQUIRED = 15;

    // Panel categories (matches UI tab indices)
    private static final int TAB_ITEMS = 0;
    private static final int TAB_CREATURES = 1;
    private static final int TAB_BUFFS = 2;
    private static final int TAB_SKILLS = 3;

    // ScriptVar keys
    private static final String SCRIPTVAR_BASE = "admin_panel";
    private static final String SCRIPTVAR_SUI_PID = SCRIPTVAR_BASE + ".sui_pid";
    private static final String SCRIPTVAR_CURRENT_TAB = SCRIPTVAR_BASE + ".current_tab";
    private static final String SCRIPTVAR_SEARCH_RESULTS = SCRIPTVAR_BASE + ".search_results";
    private static final String SCRIPTVAR_SEARCH_INDEX = SCRIPTVAR_BASE + ".search_index";

    // SUI Page path
    private static final String SUI_ADMIN_PANEL = "Script.adminPanel";

    // SUI Component paths
    private static final String COMP_SEARCH_INPUT = "adminPanel.comp.searchSection.searchInput";
    private static final String COMP_RESULTS_LIST = "adminPanel.comp.resultsSection.lstResults";
    private static final String COMP_RESULTS_DATA = "adminPanel.comp.resultsSection.dataResults";
    private static final String COMP_RESULTS_LABEL = "adminPanel.comp.resultsSection.lblResults";
    private static final String COMP_STATUS_LABEL = "adminPanel.comp.resultsSection.lblStatus";
    private static final String COMP_BTN_SPAWN = "adminPanel.comp.resultsSection.btnSpawn";
    private static final String COMP_BTN_GRANT = "adminPanel.comp.resultsSection.btnGrant";
    private static final String COMP_TITLE = "adminPanel.bg.caption.lblTitle";
    private static final String COMP_GOD_LEVEL = "adminPanel.bg.lblGodLevel";
    private static final String COMP_HELP = "adminPanel.comp.infoSection.lblHelp";

    // Datatables
    private static final String DATATABLE_MASTER_ITEM = "datatables/item/master_item/master_item.iff";
    private static final String DATATABLE_CREATURES = "datatables/mob/creatures.iff";
    private static final String DATATABLE_BUFFS = "datatables/buff/buff.iff";
    private static final String DATATABLE_SKILLS = "datatables/skill/skills.iff";

    // Datatable columns
    private static final String ITEM_NAME_COL = "name";
    private static final String ITEM_TEMPLATE_COL = "template_name";
    private static final String CREATURE_NAME_COL = "creature_name";
    private static final String CREATURE_TEMPLATE_COL = "template_name";
    private static final String BUFF_NAME_COL = "name";
    private static final String SKILL_NAME_COL = "skill_name";

    // Max results
    private static final int MAX_SEARCH_RESULTS = 100;

    // Creature spawn distance
    private static final float CREATURE_SPAWN_DISTANCE = 3.0f;

    // ========================================
    // LIFECYCLE
    // ========================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!validateAdminAccess(self))
        {
            sendSystemMessage(self, "You do not have permission to use the Admin Panel.", null);
            detachScript(self, "player.player_ui");
            return SCRIPT_CONTINUE;
        }

        sendSystemMessage(self, "Admin Panel ready. Type '/admin' to open.", null);
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        closeAdminPanel(self);
        return SCRIPT_CONTINUE;
    }

    // ========================================
    // COMMAND HANDLER
    // ========================================

    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        if (text == null || text.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        String lowerText = text.toLowerCase().trim();

        if (lowerText.equals("/adminpanel") || lowerText.equals("/admin"))
        {
            if (!validateAdminAccess(self))
            {
                sendSystemMessage(self, "Access denied.", null);
                return SCRIPT_CONTINUE;
            }

            openAdminPanel(self);
            return SCRIPT_OVERRIDE;
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================
    // ADMIN PANEL MANAGEMENT
    // ========================================

    private void openAdminPanel(obj_id player) throws InterruptedException
    {
        // Close existing panel if open
        closeAdminPanel(player);

        // Create the SUI page
        int pid = createSUIPage(SUI_ADMIN_PANEL, player, player, "handleAdminPanelCallback");

        if (pid < 0)
        {
            sendSystemMessage(player, "Error: Could not create Admin Panel UI.", null);
            return;
        }

        // Store the page ID
        utils.setScriptVar(player, SCRIPTVAR_SUI_PID, pid);
        utils.setScriptVar(player, SCRIPTVAR_CURRENT_TAB, TAB_ITEMS);

        // Initialize the panel
        initializePanel(player, pid);

        // Subscribe to UI events
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.tabs.btnItems", "handleTabItems");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.tabs.btnCreatures", "handleTabCreatures");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.tabs.btnBuffs", "handleTabBuffs");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.tabs.btnSkills", "handleTabSkills");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.searchSection.btnSearch", "handleSearch");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.resultsSection.btnSpawn", "handleSpawn");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.comp.resultsSection.btnGrant", "handleGrant");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "adminPanel.btnClose", "handleClose");

        // Subscribe to input properties
        subscribeToSUIProperty(pid, COMP_SEARCH_INPUT, sui.PROP_LOCALTEXT);
        subscribeToSUIProperty(pid, COMP_RESULTS_LIST, sui.PROP_SELECTEDROW);

        // Show the page
        showSUIPage(pid);
    }

    private void closeAdminPanel(obj_id player) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(player, SCRIPTVAR_SUI_PID);
        if (pid > 0)
        {
            forceCloseSUIPage(pid);
        }
        cleanupScriptVars(player);
    }

    private void initializePanel(obj_id player, int pid) throws InterruptedException
    {
        // Set god level display
        int godLevel = getGodLevel(player);
        setSUIProperty(pid, COMP_GOD_LEVEL, sui.PROP_TEXT, "God Level: " + godLevel);

        // Set initial tab state (Items)
        updateTabState(player, pid, TAB_ITEMS);
    }

    private void updateTabState(obj_id player, int pid, int tab) throws InterruptedException
    {
        utils.setScriptVar(player, SCRIPTVAR_CURRENT_TAB, tab);

        String title;
        String helpText;
        boolean showSpawn = false;
        boolean showGrant = false;

        switch (tab)
        {
            case TAB_ITEMS:
                title = "Admin Panel - Static Items";
                helpText = "Search for items by name. Select from results and click Spawn to add to inventory.";
                showSpawn = true;
                break;
            case TAB_CREATURES:
                title = "Admin Panel - Creatures";
                helpText = "Search for creatures by name. Select from results and click Spawn to create nearby.";
                showSpawn = true;
                break;
            case TAB_BUFFS:
                title = "Admin Panel - Buffs";
                helpText = "Search for buffs by name. Select from results and click Grant to apply to yourself.";
                showGrant = true;
                break;
            case TAB_SKILLS:
                title = "Admin Panel - Skills";
                helpText = "Search for skills by name. Select from results and click Grant to learn the skill.";
                showGrant = true;
                break;
            default:
                title = "Admin Panel";
                helpText = "Select a category tab above.";
                break;
        }

        setSUIProperty(pid, COMP_TITLE, sui.PROP_TEXT, title);
        setSUIProperty(pid, COMP_HELP, sui.PROP_TEXT, helpText);
        setSUIProperty(pid, COMP_BTN_SPAWN, sui.PROP_VISIBLE, showSpawn ? "true" : "false");
        setSUIProperty(pid, COMP_BTN_GRANT, sui.PROP_VISIBLE, showGrant ? "true" : "false");
        setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "");

        // Clear results
        clearSUIDataSource(pid, COMP_RESULTS_DATA);
        setSUIProperty(pid, COMP_RESULTS_LABEL, sui.PROP_TEXT, "Results (0):");
        utils.removeScriptVar(player, SCRIPTVAR_SEARCH_RESULTS);

        flushSUIPage(pid);
    }

    // ========================================
    // TAB HANDLERS
    // ========================================

    public int handleTabItems(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid > 0)
        {
            updateTabState(self, pid, TAB_ITEMS);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleTabCreatures(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid > 0)
        {
            updateTabState(self, pid, TAB_CREATURES);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleTabBuffs(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid > 0)
        {
            updateTabState(self, pid, TAB_BUFFS);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleTabSkills(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid > 0)
        {
            updateTabState(self, pid, TAB_SKILLS);
        }
        return SCRIPT_CONTINUE;
    }

    // ========================================
    // SEARCH HANDLER
    // ========================================

    public int handleSearch(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid <= 0)
        {
            return SCRIPT_CONTINUE;
        }

        String searchText = params.getString(COMP_SEARCH_INPUT + "." + sui.PROP_LOCALTEXT);
        if (searchText == null || searchText.trim().isEmpty())
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FF0000Please enter a search term.");
            flushSUIPage(pid);
            return SCRIPT_CONTINUE;
        }

        searchText = searchText.trim().toLowerCase();
        int currentTab = utils.getIntScriptVar(self, SCRIPTVAR_CURRENT_TAB);

        String[] results = performSearch(self, currentTab, searchText);

        if (results == null || results.length == 0)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FF0000No results found for: " + searchText);
            clearSUIDataSource(pid, COMP_RESULTS_DATA);
            setSUIProperty(pid, COMP_RESULTS_LABEL, sui.PROP_TEXT, "Results (0):");
            utils.removeScriptVar(self, SCRIPTVAR_SEARCH_RESULTS);
        }
        else
        {
            // Store results for later use
            utils.setScriptVar(self, SCRIPTVAR_SEARCH_RESULTS, results);

            // Populate the list
            clearSUIDataSource(pid, COMP_RESULTS_DATA);
            for (int i = 0; i < results.length; i++)
            {
                addSUIDataItem(pid, COMP_RESULTS_DATA, String.valueOf(i));
                setSUIProperty(pid, COMP_RESULTS_DATA + "." + i, sui.PROP_TEXT, results[i]);
            }

            String resultText = "Results (" + results.length + "):";
            if (results.length >= MAX_SEARCH_RESULTS)
            {
                resultText += " (Limited)";
            }
            setSUIProperty(pid, COMP_RESULTS_LABEL, sui.PROP_TEXT, resultText);
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#00FF00Found " + results.length + " results.");
        }

        flushSUIPage(pid);
        return SCRIPT_CONTINUE;
    }

    private String[] performSearch(obj_id player, int tab, String searchText) throws InterruptedException
    {
        try
        {
            String datatable;
            String column;

            switch (tab)
            {
                case TAB_ITEMS:
                    return searchItems(searchText);
                case TAB_CREATURES:
                    return searchCreatures(searchText);
                case TAB_BUFFS:
                    return searchBuffs(searchText);
                case TAB_SKILLS:
                    return searchSkills(searchText);
                default:
                    return null;
            }
        }
        catch (Exception e)
        {
            LOG("admin_panel", "Error during search: " + e.toString());
            return null;
        }
    }

    private String[] searchItems(String searchText) throws InterruptedException
    {
        String[] itemNames = dataTableGetStringColumn(DATATABLE_MASTER_ITEM, ITEM_NAME_COL);
        if (itemNames == null || itemNames.length == 0)
        {
            return null;
        }

        java.util.Vector<String> matches = new java.util.Vector<>();
        searchText = searchText.toLowerCase();

        for (String name : itemNames)
        {
            if (name != null && !name.isEmpty() && name.toLowerCase().contains(searchText))
            {
                matches.add(name);
                if (matches.size() >= MAX_SEARCH_RESULTS)
                {
                    break;
                }
            }
        }

        return matches.size() > 0 ? matches.toArray(new String[0]) : null;
    }

    private String[] searchCreatures(String searchText) throws InterruptedException
    {
        String[] creatureNames = dataTableGetStringColumn(DATATABLE_CREATURES, CREATURE_NAME_COL);
        if (creatureNames == null || creatureNames.length == 0)
        {
            return null;
        }

        java.util.Vector<String> matches = new java.util.Vector<>();
        searchText = searchText.toLowerCase();

        for (String name : creatureNames)
        {
            if (name != null && !name.isEmpty() && name.toLowerCase().contains(searchText))
            {
                matches.add(name);
                if (matches.size() >= MAX_SEARCH_RESULTS)
                {
                    break;
                }
            }
        }

        return matches.size() > 0 ? matches.toArray(new String[0]) : null;
    }

    private String[] searchBuffs(String searchText) throws InterruptedException
    {
        String[] buffNames = dataTableGetStringColumn(DATATABLE_BUFFS, BUFF_NAME_COL);
        if (buffNames == null || buffNames.length == 0)
        {
            return null;
        }

        java.util.Vector<String> matches = new java.util.Vector<>();
        searchText = searchText.toLowerCase();

        for (String name : buffNames)
        {
            if (name != null && !name.isEmpty() && name.toLowerCase().contains(searchText))
            {
                matches.add(name);
                if (matches.size() >= MAX_SEARCH_RESULTS)
                {
                    break;
                }
            }
        }

        return matches.size() > 0 ? matches.toArray(new String[0]) : null;
    }

    private String[] searchSkills(String searchText) throws InterruptedException
    {
        String[] skillNames = dataTableGetStringColumn(DATATABLE_SKILLS, SKILL_NAME_COL);
        if (skillNames == null || skillNames.length == 0)
        {
            return null;
        }

        java.util.Vector<String> matches = new java.util.Vector<>();
        searchText = searchText.toLowerCase();

        for (String name : skillNames)
        {
            if (name != null && !name.isEmpty() && name.toLowerCase().contains(searchText))
            {
                matches.add(name);
                if (matches.size() >= MAX_SEARCH_RESULTS)
                {
                    break;
                }
            }
        }

        return matches.size() > 0 ? matches.toArray(new String[0]) : null;
    }

    // ========================================
    // ACTION HANDLERS
    // ========================================

    public int handleSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid <= 0)
        {
            return SCRIPT_CONTINUE;
        }

        String[] results = utils.getStringArrayScriptVar(self, SCRIPTVAR_SEARCH_RESULTS);
        if (results == null || results.length == 0)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FF0000No results to select from.");
            flushSUIPage(pid);
            return SCRIPT_CONTINUE;
        }

        String selectedRowStr = params.getString(COMP_RESULTS_LIST + "." + sui.PROP_SELECTEDROW);
        int selectedRow = utils.stringToInt(selectedRowStr);

        if (selectedRow < 0 || selectedRow >= results.length)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FFFF00Please select an item from the list.");
            flushSUIPage(pid);
            return SCRIPT_CONTINUE;
        }

        String selectedName = results[selectedRow];
        int currentTab = utils.getIntScriptVar(self, SCRIPTVAR_CURRENT_TAB);

        boolean success = false;
        String actionType = "";

        if (currentTab == TAB_ITEMS)
        {
            success = spawnItem(self, selectedName);
            actionType = "Item spawn";
        }
        else if (currentTab == TAB_CREATURES)
        {
            success = spawnCreature(self, selectedName);
            actionType = "Creature spawn";
        }

        if (success)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#00FF00" + actionType + " successful: " + selectedName);
            CustomerServiceLog("adminPanel", getPlayerName(self) + " (" + self + ") " + actionType.toLowerCase() + ": " + selectedName);
        }
        else
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FF0000" + actionType + " failed: " + selectedName);
        }

        flushSUIPage(pid);
        return SCRIPT_CONTINUE;
    }

    public int handleGrant(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(self, SCRIPTVAR_SUI_PID);
        if (pid <= 0)
        {
            return SCRIPT_CONTINUE;
        }

        String[] results = utils.getStringArrayScriptVar(self, SCRIPTVAR_SEARCH_RESULTS);
        if (results == null || results.length == 0)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FF0000No results to select from.");
            flushSUIPage(pid);
            return SCRIPT_CONTINUE;
        }

        String selectedRowStr = params.getString(COMP_RESULTS_LIST + "." + sui.PROP_SELECTEDROW);
        int selectedRow = utils.stringToInt(selectedRowStr);

        if (selectedRow < 0 || selectedRow >= results.length)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FFFF00Please select an item from the list.");
            flushSUIPage(pid);
            return SCRIPT_CONTINUE;
        }

        String selectedName = results[selectedRow];
        int currentTab = utils.getIntScriptVar(self, SCRIPTVAR_CURRENT_TAB);

        boolean success = false;
        String actionType = "";

        if (currentTab == TAB_BUFFS)
        {
            success = grantBuff(self, selectedName);
            actionType = "Buff grant";
        }
        else if (currentTab == TAB_SKILLS)
        {
            if (hasSkill(self, selectedName))
            {
                setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FFFF00You already have skill: " + selectedName);
                flushSUIPage(pid);
                return SCRIPT_CONTINUE;
            }
            success = grantSkillByName(self, selectedName);
            actionType = "Skill grant";
        }

        if (success)
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#00FF00" + actionType + " successful: " + selectedName);
            CustomerServiceLog("adminPanel", getPlayerName(self) + " (" + self + ") " + actionType.toLowerCase() + ": " + selectedName);
        }
        else
        {
            setSUIProperty(pid, COMP_STATUS_LABEL, sui.PROP_TEXT, "\\#FF0000" + actionType + " failed: " + selectedName);
        }

        flushSUIPage(pid);
        return SCRIPT_CONTINUE;
    }

    public int handleClose(obj_id self, dictionary params) throws InterruptedException
    {
        closeAdminPanel(self);
        sendSystemMessage(self, "Admin Panel closed.", null);
        return SCRIPT_CONTINUE;
    }

    public int handleAdminPanelCallback(obj_id self, dictionary params) throws InterruptedException
    {
        // Generic callback for panel close via X button or cancel
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            cleanupScriptVars(self);
        }
        return SCRIPT_CONTINUE;
    }

    // ========================================
    // SPAWN/GRANT IMPLEMENTATIONS
    // ========================================

    private boolean spawnItem(obj_id player, String itemName) throws InterruptedException
    {
        try
        {
            // Validate player and get inventory
            if (!isIdValid(player) || !isPlayer(player))
            {
                LOG("admin_panel", "Invalid player for item spawn: " + player);
                return false;
            }

            obj_id inventory = utils.getInventoryContainer(player);
            if (!isIdValid(inventory))
            {
                LOG("admin_panel", "Could not get inventory for player: " + player);
                return false;
            }

            // Attempt to create item
            obj_id newItem = static_item.createNewItemFunction(itemName, inventory);

            if (!isIdValid(newItem))
            {
                LOG("admin_panel", "Failed to create item: " + itemName + " for player: " + player);
                return false;
            }

            LOG("admin_panel", "Successfully spawned item: " + itemName + " for player: " + player);
            return true;
        }
        catch (Exception e)
        {
            LOG("admin_panel", "Exception spawning item " + itemName + ": " + e.toString());
            return false;
        }
    }

    private boolean spawnCreature(obj_id player, String creatureName) throws InterruptedException
    {
        try
        {
            if (!isIdValid(player) || !isPlayer(player))
            {
                LOG("admin_panel", "Invalid player for creature spawn: " + player);
                return false;
            }

            location spawnLoc = getLocation(player);
            if (spawnLoc == null)
            {
                LOG("admin_panel", "Could not get location for player: " + player);
                return false;
            }

            // Offset spawn location in front of player based on yaw
            float yaw = getYaw(player);
            double radians = Math.toRadians(yaw);
            spawnLoc.x += (float)(Math.sin(radians) * CREATURE_SPAWN_DISTANCE);
            spawnLoc.z += (float)(Math.cos(radians) * CREATURE_SPAWN_DISTANCE);

            // Build template path if needed
            String template = creatureName;
            if (!template.startsWith("object/"))
            {
                template = "object/mobile/" + template;
            }
            if (!template.endsWith(".iff"))
            {
                template += ".iff";
            }

            obj_id creature = createObject(template, spawnLoc);

            if (!isIdValid(creature))
            {
                LOG("admin_panel", "Failed to create creature: " + template + " at location: " + spawnLoc);
                return false;
            }

            LOG("admin_panel", "Successfully spawned creature: " + creatureName + " for player: " + player + " at " + spawnLoc);
            return true;
        }
        catch (Exception e)
        {
            LOG("admin_panel", "Exception spawning creature " + creatureName + ": " + e.toString());
            return false;
        }
    }

    private boolean grantBuff(obj_id player, String buffName) throws InterruptedException
    {
        try
        {
            if (!isIdValid(player) || !isPlayer(player))
            {
                LOG("admin_panel", "Invalid player for buff grant: " + player);
                return false;
            }

            if (!buff.canApplyBuff(player, buffName))
            {
                LOG("admin_panel", "Cannot apply buff: " + buffName + " to player: " + player);
                return false;
            }

            buff.applyBuff(player, buffName);
            LOG("admin_panel", "Successfully granted buff: " + buffName + " to player: " + player);
            return true;
        }
        catch (Exception e)
        {
            LOG("admin_panel", "Exception granting buff " + buffName + ": " + e.toString());
            return false;
        }
    }

    private boolean grantSkillByName(obj_id player, String skillName) throws InterruptedException
    {
        try
        {
            if (!isIdValid(player) || !isPlayer(player))
            {
                LOG("admin_panel", "Invalid player for skill grant: " + player);
                return false;
            }

            if (hasSkill(player, skillName))
            {
                LOG("admin_panel", "Player already has skill: " + skillName);
                return false;
            }

            grantSkill(player, skillName);
            LOG("admin_panel", "Successfully granted skill: " + skillName + " to player: " + player);
            return true;
        }
        catch (Exception e)
        {
            LOG("admin_panel", "Exception granting skill " + skillName + ": " + e.toString());
            return false;
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private boolean validateAdminAccess(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !isPlayer(player))
        {
            return false;
        }

        if (!isGod(player))
        {
            return false;
        }

        int godLevel = getGodLevel(player);
        return godLevel >= GOD_LEVEL_REQUIRED;
    }

    private void cleanupScriptVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, SCRIPTVAR_BASE);
    }

    // ========================================
    // PUBLIC API
    // ========================================

    /**
     * Static method to open the admin panel for a player
     * Can be called from other scripts or commands
     */
    public static void showAdminPanel(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !isPlayer(player))
        {
            return;
        }

        if (!hasScript(player, "player.player_ui"))
        {
            attachScript(player, "player.player_ui");
        }

        dictionary d = new dictionary();
        d.put("action", "open");
        messageTo(player, "msgOpenAdminPanel", d, 0, false);
    }

    public int msgOpenAdminPanel(obj_id self, dictionary params) throws InterruptedException
    {
        if (validateAdminAccess(self))
        {
            openAdminPanel(self);
        }
        return SCRIPT_CONTINUE;
    }
}
