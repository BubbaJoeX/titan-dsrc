package script.object.tangible.terminal;

import script.*;
import script.library.sui;
import script.library.utils;

public class terminal_claim_management extends script.base_script
{
    public static final String PID_MAIN = "claimTerm.main";

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, "claim.id"))
        {
            return SCRIPT_CONTINUE;
        }
        menu_info_data mid = mi.getMenuItemByType(menu_info_types.ITEM_USE);
        if (mid != null)
        {
            mid.setServerNotify(true);
        }
        else
        {
            mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("ui_radial", "item_use"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE)
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, "claim.id"))
        {
            return SCRIPT_CONTINUE;
        }
        int claimId = getIntObjVar(self, "claim.id");
        String title = "Claim Management";
        String prompt = "Choose an action for claim #" + claimId;
        String[] options = new String[]
        {
            "Pay maintenance (1000 cr)",
            "Withdraw taxed resources (generic key)",
            "Ban look-at target",
            "Unban look-at target",
            "Close"
        };
        if (sui.hasPid(player, PID_MAIN))
        {
            sui.closeSUI(player, utils.getIntScriptVar(player, PID_MAIN));
        }
        int pid = sui.listbox(self, player, prompt, sui.OK_CANCEL, title, options, "handleClaimTerminalMain", true, false);
        utils.setScriptVar(player, PID_MAIN, pid);
        return SCRIPT_CONTINUE;
    }

    public int handleClaimTerminalMain(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        if (row < 0)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id terminal = self;
        if (row == 0)
        {
            claimPayMaintenance(player, terminal, 1000);
            sendSystemMessageTestingOnly(player, "Maintenance payment attempted.");
        }
        else if (row == 1)
        {
            if (claimWithdrawTax(player, terminal, "generic", 10))
            {
                sendSystemMessageTestingOnly(player, "Withdrew 10 units from generic tax bucket (if available).");
            }
            else
            {
                sendSystemMessageTestingOnly(player, "Withdraw failed (balance or permissions).");
            }
        }
        else if (row == 2)
        {
            obj_id target = getLookAtTarget(player);
            if (isIdValid(target) && isPlayer(target))
            {
                claimAddBan(player, terminal, target);
                sendSystemMessageTestingOnly(player, "Ban recorded for character.");
            }
            else
            {
                sendSystemMessageTestingOnly(player, "Look at a player to ban.");
            }
        }
        else if (row == 3)
        {
            obj_id target = getLookAtTarget(player);
            if (isIdValid(target) && isPlayer(target))
            {
                claimRemoveBan(player, terminal, target);
                sendSystemMessageTestingOnly(player, "Unban attempted.");
            }
        }
        return SCRIPT_CONTINUE;
    }
}
