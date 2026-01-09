package script.developer.bubbajoe;

/**
 * @Origin: dsrc.script.developer.bubbajoe
 * @Author: BubbaJoeX
 * @Purpose: Dynamic turn-in system - configurable via objvars for any NPC/terminal
 * @Requirements: Set objvars on the object to configure turn-in requirements and rewards
 * @Notes:
 *   Configuration objvars:
 *     turn_in.name           - Display name for the turn-in (string)
 *     turn_in.required_skill - Required skill to use (string, optional)
 *     turn_in.timeout        - Seconds before auto-reset (int, default 300)
 *     turn_in.spawn_guards   - Whether to spawn guards on first item (boolean, default false)
 *     turn_in.guard_table    - Datatable for guard spawns (string, optional)
 *
 *   Item requirements (indexed):
 *     turn_in.items.0.template  - Item template path
 *     turn_in.items.0.name      - Display name for messages
 *     turn_in.items.0.count     - Required count (default 1)
 *     turn_in.items.0.consume   - Whether to destroy item (default true)
 *     turn_in.items.0.group     - Item group (items in same group are mutually exclusive)
 *
 *   Rewards (indexed):
 *     turn_in.rewards.0.type     - "item", "credits", "xp", "badge", "script_call"
 *     turn_in.rewards.0.value    - Template/amount/badge name/script method
 *     turn_in.rewards.0.count    - Count for items (default 1)
 *
 * @Created: Tuesday, 12/9/2025
 * @Copyright: SWG: Titan 2025
 */

import script.*;
import script.library.*;

import static script.library.badge.grantBadge;

public class turn_in extends script.base_script
{
    // ========================================================================
    // CONSTANTS
    // ========================================================================

    public static final String LOG_CHANNEL = "turn_in";
    public static final String SCRIPT_NAME = "developer.bubbajoe.turn_in";

    // Objvar paths
    public static final String VAR_BASE = "turn_in";
    public static final String VAR_NAME = VAR_BASE + ".name";
    public static final String VAR_REQUIRED_SKILL = VAR_BASE + ".required_skill";
    public static final String VAR_TIMEOUT = VAR_BASE + ".timeout";
    public static final String VAR_SPAWN_GUARDS = VAR_BASE + ".spawn_guards";
    public static final String VAR_GUARD_TABLE = VAR_BASE + ".guard_table";
    public static final String VAR_ITEMS = VAR_BASE + ".items";
    public static final String VAR_REWARDS = VAR_BASE + ".rewards";

    // Runtime state objvars
    public static final String VAR_CURRENT_USER = VAR_BASE + ".current_user";
    public static final String VAR_COLLECTED = VAR_BASE + ".collected";
    public static final String VAR_ACTIVE_GROUP = VAR_BASE + ".active_group";
    public static final String VAR_IN_PROGRESS = VAR_BASE + ".in_progress";

    // Reward types
    public static final String REWARD_ITEM = "item";
    public static final String REWARD_STATIC_ITEM = "static_item";
    public static final String REWARD_CREDITS = "credits";
    public static final String REWARD_XP = "xp";
    public static final String REWARD_BADGE = "badge";
    public static final String REWARD_SCRIPT_CALL = "script_call";

    // Defaults
    public static final int DEFAULT_TIMEOUT = 300; // 5 minutes
    public static final int DEFAULT_ITEM_COUNT = 1;

    // String IDs
    public static final String STF_FILE = "developer/turn_in";
    public static final string_id SID_SKILL_REQUIRED = new string_id(STF_FILE, "skill_required");
    public static final string_id SID_IN_USE = new string_id(STF_FILE, "in_use_by_another");
    public static final string_id SID_ALREADY_HAS = new string_id(STF_FILE, "already_has_component");
    public static final string_id SID_WRONG_GROUP = new string_id(STF_FILE, "wrong_item_group");
    public static final string_id SID_ITEM_ACCEPTED = new string_id(STF_FILE, "item_accepted");
    public static final string_id SID_ITEMS_NEEDED = new string_id(STF_FILE, "items_still_needed");
    public static final string_id SID_TURN_IN_COMPLETE = new string_id(STF_FILE, "turn_in_complete");
    public static final string_id SID_REWARD_GRANTED = new string_id(STF_FILE, "reward_granted");
    public static final string_id SID_INVALID_ITEM = new string_id(STF_FILE, "invalid_item");
    public static final string_id SID_TIMED_OUT = new string_id(STF_FILE, "session_timed_out");

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public turn_in()
    {
    }

    // ========================================================================
    // SCRIPT ENTRY POINTS
    // ========================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "OnAttach: Script attached to " + self);
        initializeTurnIn(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "OnInitialize: Initializing " + self);
        initializeTurnIn(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isValidId(player) || !isPlayer(player))
        {
            return SCRIPT_CONTINUE;
        }

        // Add "Turn In Items" option
        String turnInName = getTurnInName(self);
        int menuOption = mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id(STF_FILE, "menu_turn_in"));

        // Add "Check Progress" option if in progress
        if (isUserInProgress(self, player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU2, new string_id(STF_FILE, "menu_check_progress"));
        }

        // Add "Cancel" option if this player started it
        if (isCurrentUser(self, player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU3, new string_id(STF_FILE, "menu_cancel"));
        }

        //if god, add "Modify Rewards" option, dynamically incrementing the SERVER_MENU## value
        if (isGod(player))
        {
            int godMenuOption = mi.addRootMenu(menu_info_types.SERVER_MENU4,
                    new string_id(STF_FILE, "menu_modify_rewards"));

            int base = menu_info_types.SERVER_MENU4;

            for (int i = 0; i < 20; i++) {
                mi.addSubMenu(
                        godMenuOption,
                        base + i,
                        new string_id(STF_FILE, "Manipulate Reward: " + (i + 1))
                );
            }
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isValidId(player) || !isPlayer(player))
        {
            return SCRIPT_CONTINUE;
        }

        if (item == menu_info_types.SERVER_MENU1)
        {
            // Turn In Items - show what's needed
            showRequirements(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU2)
        {
            // Check Progress
            showProgress(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU3)
        {
            // Cancel
            if (isCurrentUser(self, player))
            {
                resetTurnIn(self);
                sendSystemMessage(player, new string_id(STF_FILE, "cancelled"));
            }
        }

        else if (isGod(player))
        {
            int base = menu_info_types.SERVER_MENU4;
            if (item >= base && item < base + 20)
            {
                int rewardIndex = item - base;
                sui.inputbox(self, player, "Modify Reward " + (rewardIndex + 1),
                        "Enter reward type,value,count separated by commas:\n" +
                                "Type: item, static_item, credits, xp, badge, script_call\n" +
                                "Value: template path / amount / badge name / script method\n" +
                                "Count: number of items (default 1)\n\n" +
                                "Example: item,object/tangible/item.iff,2", null, "handleModifyReward");
            }
        }

        return SCRIPT_CONTINUE;
    }

    public int OnGiveItem(obj_id self, obj_id item, obj_id giver) throws InterruptedException
    {
        if (!isValidId(giver) || !isPlayer(giver))
        {
            return SCRIPT_CONTINUE;
        }

        LOG(LOG_CHANNEL, "OnGiveItem: Player " + giver + " giving item " + item + " to " + self);

        // Check skill requirement
        if (!checkSkillRequirement(self, giver))
        {
            sendSystemMessage(giver, SID_SKILL_REQUIRED);
            return SCRIPT_CONTINUE;
        }

        // Check if another player is using this
        if (hasObjVar(self, VAR_CURRENT_USER))
        {
            obj_id currentUser = getObjIdObjVar(self, VAR_CURRENT_USER);
            if (isValidId(currentUser) && currentUser != giver)
            {
                sendSystemMessage(giver, SID_IN_USE);
                return SCRIPT_CONTINUE;
            }
        }

        // First item - initialize session
        if (!hasObjVar(self, VAR_CURRENT_USER))
        {
            startSession(self, giver);
        }

        // Process the item
        boolean accepted = processItem(self, giver, item);

        if (accepted)
        {
            // Check if turn-in is complete
            if (checkComplete(self, giver))
            {
                completeTurnIn(self, giver);
            }
            else
            {
                showProgress(self, giver);
            }
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // SESSION MANAGEMENT
    // ========================================================================

    private void initializeTurnIn(obj_id self) throws InterruptedException
    {
        // Ensure clean state on init
        if (!hasObjVar(self, VAR_NAME))
        {
            setObjVar(self, VAR_NAME, "Turn-In Station");
        }
    }

    private void startSession(obj_id self, obj_id player) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "startSession: Player " + player + " starting session on " + self);

        setObjVar(self, VAR_CURRENT_USER, player);
        setObjVar(self, VAR_IN_PROGRESS, true);
        setObjVar(player, "turn_in.active_station", self);

        // Spawn guards if configured
        if (hasObjVar(self, VAR_SPAWN_GUARDS) && getIntObjVar(self, VAR_SPAWN_GUARDS) == 1)
        {
            spawnGuards(self);
        }

        // Schedule timeout
        int timeout = DEFAULT_TIMEOUT;
        if (hasObjVar(self, VAR_TIMEOUT))
        {
            timeout = getIntObjVar(self, VAR_TIMEOUT);
        }

        dictionary params = new dictionary();
        params.put("player", player);
        messageTo(self, "handleTimeout", params, timeout, false);
    }

    public int handleTimeout(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, VAR_CURRENT_USER))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id player = params.getObjId("player");
        obj_id currentUser = getObjIdObjVar(self, VAR_CURRENT_USER);

        // Only timeout if same player
        if (isValidId(player) && isValidId(currentUser) && player == currentUser)
        {
            LOG(LOG_CHANNEL, "handleTimeout: Session timed out for player " + player);

            if (isValidId(player) && isPlayer(player))
            {
                sendSystemMessage(player, SID_TIMED_OUT);
            }

            resetTurnIn(self);
        }

        return SCRIPT_CONTINUE;
    }

    private void resetTurnIn(obj_id self) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "resetTurnIn: Resetting " + self);

        // Clear player's reference
        if (hasObjVar(self, VAR_CURRENT_USER))
        {
            obj_id player = getObjIdObjVar(self, VAR_CURRENT_USER);
            if (isValidId(player))
            {
                removeObjVar(player, "turn_in.active_station");
            }
        }

        // Clear all runtime state
        removeObjVar(self, VAR_CURRENT_USER);
        removeObjVar(self, VAR_COLLECTED);
        removeObjVar(self, VAR_ACTIVE_GROUP);
        removeObjVar(self, VAR_IN_PROGRESS);
    }

    // ========================================================================
    // ITEM PROCESSING
    // ========================================================================

    private boolean processItem(obj_id self, obj_id player, obj_id item) throws InterruptedException
    {
        String itemTemplate = getTemplateName(item);

        if (itemTemplate == null || itemTemplate.length() == 0)
        {
            sendSystemMessage(player, SID_INVALID_ITEM);
            return false;
        }

        // Find matching requirement
        int itemIndex = findItemRequirement(self, itemTemplate);

        if (itemIndex < 0)
        {
            sendSystemMessage(player, SID_INVALID_ITEM);
            return false;
        }

        String itemPath = VAR_ITEMS + "." + itemIndex;

        // Check if already collected
        String collectedPath = VAR_COLLECTED + "." + itemIndex;
        if (hasObjVar(self, collectedPath))
        {
            int collected = getIntObjVar(self, collectedPath);
            int required = getRequiredCount(self, itemIndex);

            if (collected >= required)
            {
                sendSystemMessage(player, SID_ALREADY_HAS);
                return false;
            }
        }

        // Check item group (mutually exclusive items)
        if (hasObjVar(self, itemPath + ".group"))
        {
            String itemGroup = getStringObjVar(self, itemPath + ".group");

            if (hasObjVar(self, VAR_ACTIVE_GROUP))
            {
                String activeGroup = getStringObjVar(self, VAR_ACTIVE_GROUP);

                if (!itemGroup.equals(activeGroup))
                {
                    sendSystemMessage(player, SID_WRONG_GROUP);
                    return false;
                }
            }
            else
            {
                // Set active group
                setObjVar(self, VAR_ACTIVE_GROUP, itemGroup);
            }
        }

        // Accept the item
        int currentCount = 0;
        if (hasObjVar(self, collectedPath))
        {
            currentCount = getIntObjVar(self, collectedPath);
        }
        setObjVar(self, collectedPath, currentCount + 1);

        // Consume item if configured (default true)
        boolean consume = true;
        if (hasObjVar(self, itemPath + ".consume"))
        {
            consume = getIntObjVar(self, itemPath + ".consume") != 0;
        }

        if (consume)
        {
            destroyObject(item);
        }

        LOG(LOG_CHANNEL, "processItem: Accepted " + itemTemplate + " from player " + player);
        sendSystemMessage(player, SID_ITEM_ACCEPTED);

        return true;
    }

    private int findItemRequirement(obj_id self, String itemTemplate) throws InterruptedException
    {
        // Search through item requirements
        for (int i = 0; i < 20; i++) // Max 20 item types
        {
            String path = VAR_ITEMS + "." + i + ".template";

            if (!hasObjVar(self, path))
            {
                break;
            }

            String reqTemplate = getStringObjVar(self, path);
            if (reqTemplate != null && reqTemplate.equals(itemTemplate))
            {
                return i;
            }
        }

        return -1;
    }

    private int getRequiredCount(obj_id self, int itemIndex) throws InterruptedException
    {
        String path = VAR_ITEMS + "." + itemIndex + ".count";

        if (hasObjVar(self, path))
        {
            return getIntObjVar(self, path);
        }

        return DEFAULT_ITEM_COUNT;
    }

    // ========================================================================
    // COMPLETION & REWARDS
    // ========================================================================

    private boolean checkComplete(obj_id self, obj_id player) throws InterruptedException
    {
        // Check all required items in the active group
        String activeGroup = null;
        if (hasObjVar(self, VAR_ACTIVE_GROUP))
        {
            activeGroup = getStringObjVar(self, VAR_ACTIVE_GROUP);
        }

        for (int i = 0; i < 20; i++)
        {
            String basePath = VAR_ITEMS + "." + i;

            if (!hasObjVar(self, basePath + ".template"))
            {
                break;
            }

            // Skip items not in active group
            if (activeGroup != null && hasObjVar(self, basePath + ".group"))
            {
                String itemGroup = getStringObjVar(self, basePath + ".group");
                if (!itemGroup.equals(activeGroup))
                {
                    continue;
                }
            }
            else if (activeGroup != null)
            {
                // Item has no group but we have an active group - skip non-grouped check
                // Actually, non-grouped items are always required
            }

            // Check if this item is collected
            int required = getRequiredCount(self, i);
            int collected = 0;

            String collectedPath = VAR_COLLECTED + "." + i;
            if (hasObjVar(self, collectedPath))
            {
                collected = getIntObjVar(self, collectedPath);
            }

            if (collected < required)
            {
                return false;
            }
        }

        return true;
    }

    private void completeTurnIn(obj_id self, obj_id player) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "completeTurnIn: Player " + player + " completed turn-in on " + self);

        sendSystemMessage(player, SID_TURN_IN_COMPLETE);

        // Grant rewards
        grantRewards(self, player);

        // Reset for next use
        resetTurnIn(self);
    }

    private void grantRewards(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id inventory = utils.getInventoryContainer(player);

        for (int i = 0; i < 20; i++) // Max 20 rewards
        {
            String basePath = VAR_REWARDS + "." + i;

            if (!hasObjVar(self, basePath + ".type"))
            {
                break;
            }

            String rewardType = getStringObjVar(self, basePath + ".type");
            String rewardValue = "";

            if (hasObjVar(self, basePath + ".value"))
            {
                rewardValue = getStringObjVar(self, basePath + ".value");
            }

            int rewardCount = 1;
            if (hasObjVar(self, basePath + ".count"))
            {
                rewardCount = getIntObjVar(self, basePath + ".count");
            }

            LOG(LOG_CHANNEL, "grantRewards: Granting " + rewardType + " = " + rewardValue + " x" + rewardCount);

            if (rewardType.equals(REWARD_ITEM))
            {
                // Create object from template
                for (int j = 0; j < rewardCount; j++)
                {
                    obj_id reward = createObject(rewardValue, inventory, "");
                    if (isValidId(reward))
                    {
                        LOG(LOG_CHANNEL, "grantRewards: Created item " + reward);
                    }
                }
            }
            else if (rewardType.equals(REWARD_STATIC_ITEM))
            {
                // Create from static item table
                for (int j = 0; j < rewardCount; j++)
                {
                    obj_id reward = static_item.createNewItemFunction(rewardValue, inventory);
                    if (isValidId(reward))
                    {
                        LOG(LOG_CHANNEL, "grantRewards: Created static item " + reward);
                    }
                }
            }
            else if (rewardType.equals(REWARD_CREDITS))
            {
                // Grant credits
                int amount = 0;
                try
                {
                    amount = Integer.parseInt(rewardValue);
                }
                catch (NumberFormatException e)
                {
                    amount = rewardCount;
                }

                if (amount > 0)
                {
                    money.bankTo(money.ACCT_CUSTOMER_SERVICE, player, amount);
                    LOG(LOG_CHANNEL, "grantRewards: Granted " + amount + " credits");
                }
            }
            else if (rewardType.equals(REWARD_XP))
            {
                // Grant XP
                int amount = rewardCount;
                String xpType = rewardValue;

                if (xpType != null && xpType.length() > 0)
                {
                    xp.grant(player, xpType, amount);
                    LOG(LOG_CHANNEL, "grantRewards: Granted " + amount + " " + xpType + " XP");
                }
            }
            else if (rewardType.equals(REWARD_BADGE))
            {
                // Grant badge
                if (rewardValue != null && !rewardValue.isEmpty())
                {
                    grantBadge(player, rewardValue);
                    LOG(LOG_CHANNEL, "grantRewards: Granted badge " + rewardValue);
                }
            }
            else if (rewardType.equals(REWARD_SCRIPT_CALL))
            {
                // Call a script method via messageTo
                if (rewardValue != null && !rewardValue.isEmpty())
                {
                    dictionary params = new dictionary();
                    params.put("player", player);
                    params.put("station", self);
                    messageTo(self, rewardValue, params, 0, false);
                    LOG(LOG_CHANNEL, "grantRewards: Called script method " + rewardValue);
                }
            }
        }

        sendSystemMessage(player, SID_REWARD_GRANTED);
    }

    // ========================================================================
    // GUARD SPAWNING
    // ========================================================================

    private void spawnGuards(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, VAR_GUARD_TABLE))
        {
            return;
        }

        String guardTable = getStringObjVar(self, VAR_GUARD_TABLE);

        if (guardTable == null || guardTable.length() == 0)
        {
            return;
        }

        obj_id structure = getTopMostContainer(self);
        int numGuards = dataTableGetNumRows(guardTable);

        for (int i = 0; i < numGuards; i++)
        {
            String spawn = dataTableGetString(guardTable, i, "spawns");
            float xCoord = dataTableGetFloat(guardTable, i, "loc_x");
            float yCoord = dataTableGetFloat(guardTable, i, "loc_y");
            float zCoord = dataTableGetFloat(guardTable, i, "loc_z");

            location selfLoc = getLocation(self);
            String planet = selfLoc.area;

            String spawnRoom = dataTableGetString(guardTable, i, "room");
            obj_id room = null;

            if (spawnRoom != null && spawnRoom.length() > 0 && isValidId(structure))
            {
                room = getCellId(structure, spawnRoom);
            }

            location spawnPoint = new location(xCoord, yCoord, zCoord, planet, room);
            obj_id spawnedCreature = create.object(spawn, spawnPoint);

            if (isValidId(spawnedCreature))
            {
                // Track spawned guard
                setObjVar(spawnedCreature, "turn_in.station", self);
                LOG(LOG_CHANNEL, "spawnGuards: Spawned " + spawn + " at " + spawnPoint);
            }
        }
    }

    // ========================================================================
    // UI / MESSAGES
    // ========================================================================

    private void showRequirements(obj_id self, obj_id player) throws InterruptedException
    {
        String turnInName = getTurnInName(self);
        sendSystemMessage(player, new string_id(STF_FILE, "requirements_header"));

        String activeGroup = null;
        if (hasObjVar(self, VAR_ACTIVE_GROUP))
        {
            activeGroup = getStringObjVar(self, VAR_ACTIVE_GROUP);
        }

        for (int i = 0; i < 20; i++)
        {
            String basePath = VAR_ITEMS + "." + i;

            if (!hasObjVar(self, basePath + ".template"))
            {
                break;
            }

            // Skip items not in active group (if group is set)
            if (activeGroup != null && hasObjVar(self, basePath + ".group"))
            {
                String itemGroup = getStringObjVar(self, basePath + ".group");
                if (!itemGroup.equals(activeGroup))
                {
                    continue;
                }
            }

            String itemName = "Unknown Item";
            if (hasObjVar(self, basePath + ".name"))
            {
                itemName = getStringObjVar(self, basePath + ".name");
            }

            int required = getRequiredCount(self, i);

            broadcast(player, "  - " + itemName + " (x" + required + ")");
        }
    }

    private void showProgress(obj_id self, obj_id player) throws InterruptedException
    {
        sendSystemMessage(player, SID_ITEMS_NEEDED);

        String activeGroup = null;
        if (hasObjVar(self, VAR_ACTIVE_GROUP))
        {
            activeGroup = getStringObjVar(self, VAR_ACTIVE_GROUP);
        }

        for (int i = 0; i < 20; i++)
        {
            String basePath = VAR_ITEMS + "." + i;

            if (!hasObjVar(self, basePath + ".template"))
            {
                break;
            }

            // Skip items not in active group
            if (activeGroup != null && hasObjVar(self, basePath + ".group"))
            {
                String itemGroup = getStringObjVar(self, basePath + ".group");
                if (!itemGroup.equals(activeGroup))
                {
                    continue;
                }
            }

            String itemName = "Unknown Item";
            if (hasObjVar(self, basePath + ".name"))
            {
                itemName = getStringObjVar(self, basePath + ".name");
            }

            int required = getRequiredCount(self, i);
            int collected = 0;

            String collectedPath = VAR_COLLECTED + "." + i;
            if (hasObjVar(self, collectedPath))
            {
                collected = getIntObjVar(self, collectedPath);
            }

            if (collected < required)
            {
                int remaining = required - collected;
                broadcast(player, "  - " + itemName + ": " + collected + "/" + required + " (" + remaining + " needed)");
            }
        }
    }

    // ========================================================================
    // UTILITY FUNCTIONS
    // ========================================================================

    private String getTurnInName(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, VAR_NAME))
        {
            return getStringObjVar(self, VAR_NAME);
        }
        return "Turn-In Station";
    }

    private boolean checkSkillRequirement(obj_id self, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(self, VAR_REQUIRED_SKILL))
        {
            return true;
        }

        String requiredSkill = getStringObjVar(self, VAR_REQUIRED_SKILL);
        if (requiredSkill == null || requiredSkill.length() == 0)
        {
            return true;
        }

        return hasSkill(player, requiredSkill);
    }

    private boolean isCurrentUser(obj_id self, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(self, VAR_CURRENT_USER))
        {
            return false;
        }

        obj_id currentUser = getObjIdObjVar(self, VAR_CURRENT_USER);
        return isValidId(currentUser) && currentUser == player;
    }

    private boolean isUserInProgress(obj_id self, obj_id player) throws InterruptedException
    {
        return isCurrentUser(self, player) && hasObjVar(self, VAR_IN_PROGRESS);
    }

    // ========================================================================
    // RESET HANDLER (for room/dungeon systems)
    // ========================================================================

    public int roomReset(obj_id self, dictionary params) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "roomReset: Resetting turn-in station " + self);
        resetTurnIn(self);
        return SCRIPT_CONTINUE;
    }

    public int cleanUp(obj_id self, dictionary params) throws InterruptedException
    {
        LOG(LOG_CHANNEL, "cleanUp: Cleaning up turn-in station " + self);
        resetTurnIn(self);
        return SCRIPT_CONTINUE;
    }

}
