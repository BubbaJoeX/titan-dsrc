package script.event.gjpud;/*
@Origin: dsrc.script.event.gjpud.recycler
@Author: BubbaJoeX
@Purpose: Turn-in object for the Galactic Junk Pick Up Day event.
@Notes;
    This script handles the logic for turning in junk objects, and returning the appropriate message.
    This script uses radial menu configuration.
    Rewards can be set via objvar with an appropriate static item string.
@TODO:
    Fix recycles points starting at 0 and not increasing until two increments have passed.
@Created: Sunday, 1/30/2024, at 11:42 PM,
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class recycler extends base_script
{
    public static final int CASH_AMOUNT = rand(1500, 2500);
    public static final String STF = "city/city";
    public static String REWARD_SETUP_TITLE = "GJPUD REWARD MENU";
    public static String REWARD_SETUP_PROMPT = "Paste a ***STATIC ITEM*** string here, from master_item table, for the player to be rewarded. You are modifying Tier: ";

    public recycler()
    {
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        menu_info_data mid = mi.getMenuItemByType(menu_info_types.ITEM_USE);
        int menu = mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Recycle Item"));
        int menu2 = mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Check Score"));
        int menu4 = mi.addRootMenu(menu_info_types.SERVER_MENU13, new string_id("Current Rewards"));
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU40, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU3, new string_id("Clear Rewards"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU4, new string_id("Clear Points (Self)"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU12, new string_id("Clear Total Scrapped"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU5, new string_id("Set Item to Recycle"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU7, new string_id("Set Tier 1 Reward"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU8, new string_id("Set Tier 2 Reward"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU9, new string_id("Set Tier 3 Reward"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU10, new string_id("Set Tier 4 Reward"));
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU11, new string_id("Set Tier 5 Reward"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "GJPUD Workbench");
        setDescriptionString(self, "Use this workbench to turn in junk heaps collected around the galaxy!");
        LOG("events", "[GJPUD Workstation]: Attached workstation script to object " + getTemplateName(self) + " (" + self + ").");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, "event.reward_cycle"))
        {
            setObjVar(self, "event.reward_cycle", "\\#7FFFD4I\\#. II III IV V VI");
            setObjVar(self, "event.reward_cycle_int", 1);
        }
        setName(self, "GJPUD Workbench");
        setDescriptionString(self, "Use this workbench to turn in junk heaps collected around the galaxy!");
        LOG("events", "[GJPUD Workstation] Initialized workstation object.");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            String ITEM = getStringObjVar(self, "item_to_use");
            if (utils.playerHasItemByTemplateInInventoryOrEquipped(player, ITEM))
            {
                int stamps = getIntObjVar(player, "item_turn_in.stamps");
                obj_id item_to_delete = utils.getItemPlayerHasByTemplate(player, ITEM); // This will be the first item it can find. It's not picky.
                if (getCount(item_to_delete) > 1)
                {
                    setCount(item_to_delete, getCount(item_to_delete) - 1);
                    if (getCount(item_to_delete) == 1)
                    {
                        destroyObject(item_to_delete);
                    }
                }
                else
                {
                    destroyObject(item_to_delete);
                }
                if (stamps > 50)
                {
                    int pointValuePerScrap = (rand(15, 25));
                    if (factions.isDeclared(player))
                    {
                        gcw.grantModifiedGcwPoints(player, pointValuePerScrap * 5, gcw.GCW_POINT_TYPE_GROUND_PVP, "GJPUD- PvP");
                        static_item.createNewItemFunction("item_world_boss_token_01_01", utils.getInventoryContainer(player), 5);
                        broadcast(player, "You have received 5 World Boss Tokens!");
                    }
                    else
                    {
                        if (factions.isRebel(player) || factions.isImperial(player))
                        {
                            gcw.grantModifiedGcwPoints(player, pointValuePerScrap, gcw.GCW_POINT_TYPE_GROUND_PVE, "GJPUD - PvE");
                            static_item.createNewItemFunction("item_world_boss_token_01_01", utils.getInventoryContainer(player), 2);
                            broadcast(player, "You have received 2 World Boss Tokens!");
                        }
                        else
                        {
                            static_item.createNewItemFunction("item_world_boss_token_01_01", utils.getInventoryContainer(player), 1);
                            broadcast(player, "You have received a World Boss Token!");
                        }
                    }
                }
                else if (stamps < 50)
                {
                    broadcast(player, "You have successfully recycled an item and have earned one (1) point toward a reward. You now have a total of " + stamps + " points.");
                }
                if (!hasObjVar(player, "item_turn_in.stamps"))
                {
                    setObjVar(player, "item_turn_in.stamps", 1);
                }
                else if (hasObjVar(player, "item_turn_in.stamps"))
                {
                    int currentStamps = getIntObjVar(player, "item_turn_in.stamps");
                    setObjVar(player, "item_turn_in.stamps", currentStamps + 1);
                    checkForRewards(self, player);
                }
                if (!hasObjVar(self, "event.total_recycled"))
                {
                    setObjVar(self, "event.total_recycled", 1);
                }
                else
                {
                    int current = getIntObjVar(self, "event.total_recycled");
                    setObjVar(self, "event.total_recycled", current + 1);
                }
                setObjVar(self, "last_used", getPlayerFullName(player));
            }
            else if (!utils.playerHasItemByTemplate(player, getStringObjVar(self, "item_to_use")))
            {
                broadcast(player, "You do not have anything to recycle at this time.");
            }
        }
        if (item == menu_info_types.SERVER_MENU1)
        {
            String title = "GALACTIC JUNK PICKUP DAY";
            String buffer = "You current have " + getIntObjVar(player, "item_turn_in.stamps") + " recycle points!";
            sui.msgbox(self, player, buffer, title);
        }
        if (item == menu_info_types.SERVER_MENU2)
        {
            broadcast(player, "This menu will reset rewards, or clear points. choose wisely my dudes");
        }
        if (item == menu_info_types.SERVER_MENU3)
        {
            removeObjVar(player, "stamps.t1_reward_given");
            removeObjVar(player, "stamps.t2_reward_given");
            removeObjVar(player, "stamps.t3_reward_given");
            removeObjVar(player, "stamps.t4_reward_given");
            removeObjVar(player, "stamps.t5_reward_given");
            broadcast(player, "Rewards reset!");
            LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has reset their rewards.");
        }
        if (item == menu_info_types.SERVER_MENU4)
        {
            setObjVar(player, "item_turn_in.stamps", 0);
            broadcast(player, "Recycle Points reset to zero!");
            LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has reset the total points.");
        }
        if (item == menu_info_types.SERVER_MENU5)
        {
            String objectToCheck = "";
            String buffer = "Paste a non-shared object template here to check. This item should ideally be a custom object related to GJPUD.";
            String title = "Item Check";
            sui.inputbox(self, player, buffer, title, "handleCheckRequest", objectToCheck);
        }
        if (item == menu_info_types.SERVER_MENU7)
        {
            String t1ToCheck = "";
            String buffer = REWARD_SETUP_PROMPT + " 1";
            String title = REWARD_SETUP_TITLE;
            sui.inputbox(self, player, buffer, title, "handleT1Request", t1ToCheck);
        }
        if (item == menu_info_types.SERVER_MENU8)
        {
            String t2ToCheck = "";
            String buffer = REWARD_SETUP_PROMPT + " 2";
            String title = REWARD_SETUP_TITLE;
            sui.inputbox(self, player, buffer, title, "handleT2Request", t2ToCheck);
        }
        if (item == menu_info_types.SERVER_MENU9)
        {
            String t3ToCheck = "";
            String buffer = REWARD_SETUP_PROMPT + " 3";
            String title = REWARD_SETUP_TITLE;
            sui.inputbox(self, player, buffer, title, "handleT3Request", t3ToCheck);
        }
        if (item == menu_info_types.SERVER_MENU10)
        {
            String t4ToCheck = "";
            String buffer = REWARD_SETUP_PROMPT + " 4";
            String title = REWARD_SETUP_TITLE;
            sui.inputbox(self, player, buffer, title, "handleT4Request", t4ToCheck);
        }
        if (item == menu_info_types.SERVER_MENU11)
        {
            String t5ToCheck = "";
            String buffer = REWARD_SETUP_PROMPT + " 5";
            String title = REWARD_SETUP_TITLE;
            sui.inputbox(self, player, buffer, title, "handleT5Request", t5ToCheck);
        }
        if (item == menu_info_types.SERVER_MENU12)
        {
            setObjVar(self, "event.total_recycled", 0);
            LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has reset the total points.");
        }
        if (item == menu_info_types.SERVER_MENU13)
        {
            String prompt;
            String title = "Current Rewards";
            if (hasObjVar(self, "event.earth_day_t1_reward"))
            {
                prompt = "\\#F1E5ACReward Schedule\\#.: " + getStringObjVar(self, "event.reward_cycle") + "\n";
                prompt += "\\#FFA500Tier I\\#.: " + getRewardName(self, getStringObjVar(self, "event.earth_day_t1_reward")) + "\n";
                prompt += "\t\\#FFD700" + getRewardDesc(self, getStringObjVar(self, "event.earth_day_t1_reward")) + "\n\\#.\n";
            }
            else
            {
                prompt = "Tier I: \\#ff0000None\n\\#.";
            }
            if (hasObjVar(self, "event.earth_day_t2_reward"))
            {
                prompt += "\\#FFA500Tier II\\#.: " + getRewardName(self, getStringObjVar(self, "event.earth_day_t2_reward")) + "\n";
                prompt += "\t\\#FFD700" + getRewardDesc(self, getStringObjVar(self, "event.earth_day_t2_reward")) + "\n\\#.\n";
            }
            else
            {
                prompt += "Tier II: \\#ff0000None\n\\#.";
            }
            if (hasObjVar(self, "event.earth_day_t3_reward"))
            {
                prompt += "\\#FFA500Tier III\\#.: " + getRewardName(self, getStringObjVar(self, "event.earth_day_t3_reward")) + "\n";
                prompt += "\t\\#FFD700" + getRewardDesc(self, getStringObjVar(self, "event.earth_day_t3_reward")) + "\n\\#.\n";
            }
            else
            {
                prompt += "Tier III: \\#ff0000None\n\\#.";
            }
            if (hasObjVar(self, "event.earth_day_t4_reward"))
            {
                prompt += "\\#FFA500Tier IV\\#.: " + getRewardName(self, getStringObjVar(self, "event.earth_day_t4_reward")) + "\n";
                prompt += "\t\\#FFD700" + getRewardDesc(self, getStringObjVar(self, "event.earth_day_t4_reward")) + "\n\\#.\n";
            }
            else
            {
                prompt += "Tier IV: \\#ff0000None\n\\#.";
            }
            if (hasObjVar(self, "event.earth_day_t5_reward"))
            {
                prompt += "\\#FFA500Tier V\\#.: " + getRewardName(self, getStringObjVar(self, "event.earth_day_t5_reward")) + "\n";
                prompt += "\t\\#FFD700" + getRewardDesc(self, getStringObjVar(self, "event.earth_day_t5_reward")) + "\n\\#.";
            }
            else
            {
                prompt += "Tier V: \\#ff0000None\n";
            }
            int page = sui.createSUIPage(sui.SUI_MSGBOX, self, player, "noHandler");
            setSUIProperty(page, "", "Size", "1024,768");
            setSUIProperty(page, "", "Location", "500,500");
            setSUIProperty(page, sui.MSGBOX_PROMPT, sui.PROP_TEXT, prompt);
            setSUIProperty(page, sui.MSGBOX_PROMPT, "Font", "bold_22");
            setSUIProperty(page, sui.MSGBOX_TITLE, sui.PROP_TEXT, title);
            sui.msgboxButtonSetup(page, sui.OK_ONLY);
            sui.showSUIPage(page);
        }
        return SCRIPT_CONTINUE;
    }

    public String getRewardName(obj_id self, String key) throws InterruptedException
    {
        dictionary params = static_item.getMasterItemDictionary(key);
        return params.getString("string_name");
    }

    public String getRewardDesc(obj_id self, String key) throws InterruptedException
    {
        dictionary params = static_item.getMasterItemDictionary(key);
        return params.getString("string_detail");
    }

    public int checkForRewards(obj_id self, obj_id player) throws InterruptedException
    {
        int stamps = getIntObjVar(player, "item_turn_in.stamps");
        if (stamps > 50)
        {
            payCredits(player);
        }
        if (stamps == 10 && !hasObjVar(player, "stamps.t1_reward_given"))
        {
            String title = "Congratulations!";
            String buffer = "You have earned enough points for the GJPUD Tier I reward!";
            sui.msgbox(self, player, buffer, title);
            setObjVar(player, "stamps.t1_reward_given", true);
            grantT1Reward(player);
        }
        if (stamps == 20 && !hasObjVar(player, "stamps.t2_reward_given"))
        {
            String title = "Congratulations!";
            String buffer = "You have earned enough points for the GJPUD Tier II reward!";
            sui.msgbox(self, player, buffer, title);
            setObjVar(player, "stamps.t2_reward_given", true);
            grantT2Reward(player);
        }
        if (stamps == 35 && !hasObjVar(player, "stamps.t3_reward_given"))
        {
            String title = "Congratulations!";
            String buffer = "You have earned enough points for the GJPUD Tier III reward!";
            sui.msgbox(self, player, buffer, title);
            setObjVar(player, "stamps.t3_reward_given", true);
            grantT3Reward(player);
        }
        if (stamps == 45 && !hasObjVar(player, "stamps.t4_reward_given"))
        {
            String title = "Congratulations!";
            String buffer = "You have earned enough points for the GJPUD Tier IV reward!";
            sui.msgbox(self, player, buffer, title);
            setObjVar(player, "stamps.t4_reward_given", true);
            grantT4Reward(player);
        }
        if (stamps == 50 && !hasObjVar(player, "stamps.t5_reward_given"))
        {
            String title = "Congratulations!";
            String buffer = "You have earned enough points for the GJPUD Tier V reward!";
            sui.msgbox(self, player, buffer, title);
            setObjVar(player, "stamps.t5_reward_given", true);
            grantT5Reward(player);
        }
        return SCRIPT_CONTINUE;
    }

    public int grantT1Reward(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        String t1_reward = getStringObjVar(getSelf(), "event.earth_day_t1_reward");
        obj_id rewardedItem = static_item.createNewItemFunction(t1_reward, pInv);
        obj_id[] items = new obj_id[1];
        items[0] = rewardedItem;
        loot.showLootBox(player, items);
        broadcast(player, "You have recieved the GJPUD Tier I reward!");
        LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has received the GJPUD Tier I reward!");
        return SCRIPT_CONTINUE;
    }

    public int grantT2Reward(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        String t2_reward = getStringObjVar(getSelf(), "event.earth_day_t2_reward");
        obj_id rewardedItem = static_item.createNewItemFunction(t2_reward, pInv);
        obj_id[] items = new obj_id[1];
        items[0] = rewardedItem;
        loot.showLootBox(player, items);
        broadcast(player, "You have recieved the GJPUD Tier II reward!");
        LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has received the GJPUD Tier II reward!");
        return SCRIPT_CONTINUE;
    }

    public int grantT3Reward(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        String t3_reward = getStringObjVar(getSelf(), "event.earth_day_t3_reward");
        obj_id rewardedItem = static_item.createNewItemFunction(t3_reward, pInv);
        obj_id[] items = new obj_id[1];
        items[0] = rewardedItem;
        loot.showLootBox(player, items);
        broadcast(player, "You have recieved the GJPUD Tier III reward!");
        LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has received the GJPUD Tier III reward!");
        return SCRIPT_CONTINUE;
    }

    public int grantT4Reward(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        String t4_reward = getStringObjVar(getSelf(), "event.earth_day_t4_reward");
        obj_id rewardedItem = static_item.createNewItemFunction(t4_reward, pInv);
        obj_id[] items = new obj_id[1];
        items[0] = rewardedItem;
        loot.showLootBox(player, items);
        broadcast(player, "You have recieved the GJPUD Tier IV reward!");
        LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has received the GJPUD Tier IV reward!");
        return SCRIPT_CONTINUE;
    }

    public int grantT5Reward(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        String t5_reward = getStringObjVar(getSelf(), "event.earth_day_t5_reward");
        obj_id rewardedItem = static_item.createNewItemFunction(t5_reward, pInv);
        obj_id[] items = new obj_id[1];
        items[0] = rewardedItem;
        loot.showLootBox(player, items);
        broadcast(player, "You have recieved the GJPUD Tier V reward!");// and the Galactic Recycling Association badge and title!
        LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has recieved the GJPUD Tier V reward!");
        return SCRIPT_CONTINUE;
    }

    public void payCredits(obj_id player) throws InterruptedException
    {
        int moneyz = getTotalMoney(player);
        if (moneyz <= 250000)
        {
            dictionary d = new dictionary();
            d.put("payoutTarget", player);
            money.systemPayout(money.ACCT_NPC_LOOT, player, CASH_AMOUNT, "handlePayoutToPlayer", d);
            LOG("events", "GJPUD Workstation: Payed out " + CASH_AMOUNT + " credits to " + player + " for recycling.");
        }
        else
        {
            broadcast(player, "The Galactic Junk Pickup Day coordinators thank you for your work, but you currently exceed the maximum amount we can give you. Have you considered donating to a charity?");
        }
    }

    public int handlePayoutToPlayer(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("payoutTarget");
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        int retCode = params.getInt(money.DICT_CODE);
        if (retCode == money.RET_SUCCESS)
        {
            String terminal = "Galactic Junk Pickup Day Workstation";
            broadcast(player, "You receive " + CASH_AMOUNT + " credits from the " + terminal + " for your hard work keeping the galaxy clean.");
        }
        else if (retCode == money.RET_FAIL)
        {
            broadcast(player, "The system is unable to complete the transaction.");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleCheckRequest(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String objectToCheck = sui.getInputBoxText(params);
        if (objectToCheck == null)
        {
            sendSystemMessage(self, new string_id(STF, "not_valid_message"));
            return SCRIPT_CONTINUE;
        }
        if (!objectToCheck.endsWith(".iff"))
        {
            broadcast(player, "The item to check must be in .iff form. (object/tangible/*/*.iff)");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "item_to_use", objectToCheck);
        LOG("events", "GJPUD Workstation: Player " + getPlayerFullName(player) + " has set the item to recycle to " + objectToCheck);
        return SCRIPT_CONTINUE;
    }

    public int handleT1Request(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String t1ToCheck = sui.getInputBoxText(params);
        if (t1ToCheck == null)
        {
            sendSystemMessage(self, new string_id(STF, "not_valid_message"));
            return SCRIPT_CONTINUE;
        }
        if (t1ToCheck.endsWith(".iff"))
        {
            broadcast(player, "Only Static Items are allowed to be rewarded from this event.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "event.earth_day_t1_reward", t1ToCheck);
        return SCRIPT_CONTINUE;
    }

    public int handleT2Request(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String t2ToCheck = sui.getInputBoxText(params);
        if (t2ToCheck == null)
        {
            sendSystemMessage(self, new string_id(STF, "not_valid_message"));
            return SCRIPT_CONTINUE;
        }
        if (t2ToCheck.endsWith(".iff"))
        {
            broadcast(player, "Only Static Items are allowed to be rewarded from this event.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "event.earth_day_t2_reward", t2ToCheck);
        return SCRIPT_CONTINUE;
    }

    public int handleT3Request(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String t3ToCheck = sui.getInputBoxText(params);
        if (t3ToCheck == null)
        {
            sendSystemMessage(self, new string_id(STF, "not_valid_message"));
            return SCRIPT_CONTINUE;
        }
        if (t3ToCheck.endsWith(".iff"))
        {
            broadcast(player, "Only Static Items are allowed to be rewarded from this event.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "event.earth_day_t3_reward", t3ToCheck);
        return SCRIPT_CONTINUE;
    }

    public int handleT4Request(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String t4ToCheck = sui.getInputBoxText(params);
        if (t4ToCheck == null)
        {
            sendSystemMessage(self, new string_id(STF, "not_valid_message"));
            return SCRIPT_CONTINUE;
        }
        if (t4ToCheck.endsWith(".iff"))
        {
            broadcast(player, "Only Static Items are allowed to be rewarded from this event.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "event.earth_day_t4_reward", t4ToCheck);
        return SCRIPT_CONTINUE;
    }

    public int handleT5Request(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String t5ToCheck = sui.getInputBoxText(params);
        if (t5ToCheck == null)
        {
            sendSystemMessage(self, new string_id(STF, "not_valid_message"));
            return SCRIPT_CONTINUE;
        }
        if (t5ToCheck.endsWith(".iff"))
        {
            broadcast(player, "Only Static Items are allowed to be rewarded from this event.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "event.earth_day_t5_reward", t5ToCheck);
        return SCRIPT_CONTINUE;
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        names[idx] = utils.packStringId(new string_id("Reward cycle"));
        attribs[idx] = getStringObjVar(self, "event.reward_cycle");
        idx++;
        names[idx] = utils.packStringId(new string_id("Total Junk Scrapped"));
        attribs[idx] = getIntObjVar(self, "event.total_recycled") + " pieces of junk.";
        idx++;
        if (isGod(player))
        {
            names[idx] = utils.packStringId(new string_id("Template To Check"));
            attribs[idx] = getStringObjVar(self, "item_to_use");
            idx++;
            names[idx] = utils.packStringId(new string_id("Last Participant"));
            attribs[idx] = getStringObjVar(self, "last_used");
            idx++;
            names[idx] = utils.packStringId(new string_id("Top Participant"));
            attribs[idx] = getStringObjVar(self, "last_used");
            idx++;
        }
        return SCRIPT_CONTINUE;
    }
}
