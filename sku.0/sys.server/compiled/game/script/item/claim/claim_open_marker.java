package script.item.claim;

import script.*;
import script.library.sui;
import script.library.utils;

/**
 * Placed claim marker (city flag). Sole interactable for claim management.
 */
public class claim_open_marker extends script.base_script
{
    public static final String PID_MAIN = "claimTerm.main";
    public static final String PID_GRANT_ALLOWED = "claimTerm.grantAllowed";
    public static final String PID_REVOKE_ALLOWED = "claimTerm.revokeAllowed";
    public static final String PID_BAN = "claimTerm.ban";
    public static final String PID_UNBAN = "claimTerm.unban";

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
            "Grant decorate permission (by name)",
            "Revoke decorate permission (by name)",
            "Ban character (by name)",
            "Unban character (by name)",
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
            promptCharacterName(self, player, PID_GRANT_ALLOWED, "Grant Decorate Permission",
                "Enter the character's first name (partial match uses first name lookup):",
                "handleGrantAllowedByName");
        }
        else if (row == 3)
        {
            promptCharacterName(self, player, PID_REVOKE_ALLOWED, "Revoke Decorate Permission",
                "Enter the character's first name to revoke decorate permission:",
                "handleRevokeAllowedByName");
        }
        else if (row == 4)
        {
            promptCharacterName(self, player, PID_BAN, "Ban From Claim",
                "Enter the character's first name to ban from this claim:",
                "handleBanByName");
        }
        else if (row == 5)
        {
            promptCharacterName(self, player, PID_UNBAN, "Unban From Claim",
                "Enter the character's first name to unban:",
                "handleUnbanByName");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleGrantAllowedByName(obj_id self, dictionary params) throws InterruptedException
    {
        return handleCharacterNameAction(self, params, true, false, false);
    }

    public int handleRevokeAllowedByName(obj_id self, dictionary params) throws InterruptedException
    {
        return handleCharacterNameAction(self, params, false, true, false);
    }

    public int handleBanByName(obj_id self, dictionary params) throws InterruptedException
    {
        return handleCharacterNameAction(self, params, false, false, true);
    }

    public int handleUnbanByName(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            return SCRIPT_CONTINUE;
        }
        String name = sui.getInputBoxText(params);
        obj_id target = resolvePlayerByFirstName(name);
        if (!isIdValid(target))
        {
            sendSystemMessageTestingOnly(player, "No online player matched that name.");
            return SCRIPT_CONTINUE;
        }
        claimRemoveBan(player, self, target);
        sendSystemMessageTestingOnly(player, "Unban attempted for " + getName(target) + ".");
        return SCRIPT_CONTINUE;
    }

    public int handleClaimRepossession(obj_id self, dictionary params) throws InterruptedException
    {
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }

    private void promptCharacterName(obj_id self, obj_id player, String pidKey, String title, String prompt, String handler) throws InterruptedException
    {
        if (sui.hasPid(player, pidKey))
        {
            sui.closeSUI(player, utils.getIntScriptVar(player, pidKey));
        }
        int pid = sui.inputbox(self, player, prompt, title, handler, 32, false, "");
        utils.setScriptVar(player, pidKey, pid);
    }

    private int handleCharacterNameAction(obj_id self, dictionary params, boolean grantAllowed, boolean revokeAllowed, boolean ban) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            return SCRIPT_CONTINUE;
        }
        String name = sui.getInputBoxText(params);
        obj_id target = resolvePlayerByFirstName(name);
        if (!isIdValid(target))
        {
            sendSystemMessageTestingOnly(player, "No online player matched that name.");
            return SCRIPT_CONTINUE;
        }
        if (!isPlayer(target))
        {
            sendSystemMessageTestingOnly(player, "That target is not a player.");
            return SCRIPT_CONTINUE;
        }
        if (grantAllowed)
        {
            if (claimAddAllowed(player, self, target))
            {
                sendSystemMessageTestingOnly(player, "Decorate permission granted to " + getName(target) + ".");
            }
            else
            {
                sendSystemMessageTestingOnly(player, "Grant failed (permissions).");
            }
        }
        else if (revokeAllowed)
        {
            if (claimRemoveAllowed(player, self, target))
            {
                sendSystemMessageTestingOnly(player, "Decorate permission revoked from " + getName(target) + ".");
            }
            else
            {
                sendSystemMessageTestingOnly(player, "Revoke failed (permissions).");
            }
        }
        else if (ban)
        {
            claimAddBan(player, self, target);
            sendSystemMessageTestingOnly(player, "Ban recorded for " + getName(target) + ".");
        }
        return SCRIPT_CONTINUE;
    }

    private obj_id resolvePlayerByFirstName(String name) throws InterruptedException
    {
        if (name == null)
        {
            return obj_id.NULL_ID;
        }
        String trimmed = name.trim();
        if (trimmed.equals(""))
        {
            return obj_id.NULL_ID;
        }
        return getPlayerIdFromFirstName(trimmed.toLowerCase());
    }
}
