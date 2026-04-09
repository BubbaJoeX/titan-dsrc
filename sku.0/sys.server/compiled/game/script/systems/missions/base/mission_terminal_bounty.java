package script.systems.missions.base;

import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.bounty_hunter;
import script.library.money;
import script.library.sui;
import script.library.utils;

public class mission_terminal_bounty extends script.base_script
{
    public mission_terminal_bounty()
    {
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setObjVar(self, "intBounty", 1);
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        float fltDistance = getDistance(player, self);
        if (fltDistance > 10)
        {
            return SCRIPT_CONTINUE;
        }
        debugServerConsoleMsg(self, "mission_terminal OnObjectMenuRequest");
        mi.addRootMenu(menu_info_types.MISSION_TERMINAL_LIST, null);
        mi.addRootMenu(menu_info_types.SERVER_MENU3, string_id.unlocalized("Post Item Bounty"));
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.MISSION_TERMINAL_LIST)
        {
            // Integrate with standard bounty board action: while opening the mission board,
            // also auto-issue the next available item bounty contract (if eligible).
            if (hasSkill(player, "class_bountyhunter_phase1_novice") && !isIdValid(bounty_hunter.getItemBountyMission(player)))
            {
                int lastSeenId = hasObjVar(player, "bh.itemBounty.lastSeenId") ? getIntObjVar(player, "bh.itemBounty.lastSeenId") : 0;
                obj_id mission = bounty_hunter.createNextItemBountyMissionForHunter(player, self, lastSeenId);
                if (isIdValid(mission))
                {
                    int acceptedId = getIntObjVar(mission, "bh.itemBountyId");
                    setObjVar(player, "bh.itemBounty.lastSeenId", acceptedId);
                    sendSystemMessage(player, "Item bounty accepted from mission board: #" + acceptedId + ".", null);
                }
            }
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU3)
        {
            obj_id target = getLookAtTarget(player);
            if (!bounty_hunter.canCreateItemBounty(player, target, true))
            {
                return SCRIPT_CONTINUE;
            }
            utils.setScriptVar(player, "bh.itemBounty.postTarget", target);
            utils.setScriptVar(player, "bh.itemBounty.postTerminal", self);
            sui.inputbox(self, player, "Enter item bounty reward (" + bounty_hunter.MIN_BOUNTY_SET + " - " + bounty_hunter.MAX_BOUNTY_SET + "):", "Item Bounty Reward", "handleItemBountyRewardInput", "" + bounty_hunter.MIN_BOUNTY_SET);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
    public int handleItemBountyRewardInput(obj_id self, dictionary params) throws InterruptedException
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
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(player, "bh.itemBounty.postTarget");
            utils.removeScriptVar(player, "bh.itemBounty.postTerminal");
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(player, "bh.itemBounty.postTarget") || !utils.hasScriptVar(player, "bh.itemBounty.postTerminal"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = utils.getObjIdScriptVar(player, "bh.itemBounty.postTarget");
        obj_id terminal = utils.getObjIdScriptVar(player, "bh.itemBounty.postTerminal");
        utils.removeScriptVar(player, "bh.itemBounty.postTarget");
        utils.removeScriptVar(player, "bh.itemBounty.postTerminal");
        if (!bounty_hunter.canCreateItemBounty(player, target, true))
        {
            return SCRIPT_CONTINUE;
        }
        int reward = utils.stringToInt(sui.getInputBoxText(params));
        if (reward < bounty_hunter.MIN_BOUNTY_SET || reward > bounty_hunter.MAX_BOUNTY_SET)
        {
            sendSystemMessage(player, "Item bounty reward is outside the valid range.", null);
            return SCRIPT_CONTINUE;
        }
        if (reward > getTotalMoney(player))
        {
            sendSystemMessage(player, "Insufficient funds for item bounty.", null);
            return SCRIPT_CONTINUE;
        }
        dictionary d = new dictionary();
        d.put("targetItem", target);
        d.put("reward", reward);
        d.put("terminal", terminal);
        money.pay(player, money.ACCT_BOUNTY, reward, "handleItemBountyEscrowPaid", d, true);
        return SCRIPT_CONTINUE;
    }
    public int handleItemBountyEscrowPaid(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        if (money.getReturnCode(params) == money.RET_FAIL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id item = params.getObjId("targetItem");
        int reward = params.getInt("reward");
        obj_id terminal = params.getObjId("terminal");
        if (!bounty_hunter.createItemBounty(self, item, reward, terminal))
        {
            money.bankTo(money.ACCT_BOUNTY, self, reward);
            sendSystemMessage(self, "Item bounty creation failed; escrow refunded.", null);
            return SCRIPT_CONTINUE;
        }
        sendSystemMessage(self, "Item bounty posted to the board.", null);
        return SCRIPT_CONTINUE;
    }
}
