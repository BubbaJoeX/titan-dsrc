package script.library;

import script.*;

/**
 * Open-world claim management UI (owner-only actions enforced server-side).
 * Used by the placed city-flag marker and optional claim management terminals.
 */
public class claim_management extends script.base_script
{
    public static final String SCRIPT_NAME = "library.claim_management";

    public static final string_id SID_MANAGEMENT = new string_id("player_structure", "management");
    public static final string_id SID_WHILE_DEAD = new string_id("player_structure", "while_dead");

    public static final String VAR_TERMINAL = "claim.mgmt.terminal";
    public static final String VAR_PENDING_ACTION = "claim.mgmt.pending";
    public static final String VAR_PENDING_NAME = "claim.mgmt.pendingName";

    public static final String PID_MAIN = "claim.mgmt.main";
    public static final String PID_SUB = "claim.mgmt.sub";
    public static final String PID_INPUT = "claim.mgmt.input";

    public static final int MAINTENANCE_FEE_CREDITS = 1000;
    public static final int MAINTENANCE_INTERVAL_DAYS = 30;
    public static final int VISITOR_TAX_PERCENT = 10;
    public static final String DEFAULT_TAX_RESOURCE = "generic";

    public static final int MENU_MAIN = 0;
    public static final int MENU_STATUS = 1;
    public static final int MENU_MAINTENANCE = 2;
    public static final int MENU_TAX = 3;
    public static final int MENU_ACCESS = 4;
    public static final int MENU_LOOKAT = 5;
    public static final int MENU_HELP = 6;

    public static final String PENDING_PAY_CUSTOM = "payCustom";
    public static final String PENDING_WITHDRAW_CUSTOM = "withdrawCustom";
    public static final String PENDING_WITHDRAW_RESOURCE = "withdrawResource";
    public static final String PENDING_GRANT = "grant";
    public static final String PENDING_REVOKE = "revoke";
    public static final String PENDING_BAN = "ban";
    public static final String PENDING_UNBAN = "unban";

    // ----------------------------------------------------------------------

    public static void openMainPanel(obj_id marker, obj_id player) throws InterruptedException
    {
        if (!isIdValid(marker) || !isIdValid(player))
        {
            return;
        }
        if (isDead(player) || isIncapacitated(player))
        {
            sendSystemMessage(player, SID_WHILE_DEAD);
            return;
        }
        if (!hasObjVar(marker, "claim.id"))
        {
            sendSystemMessage(player, new string_id("error_message", "prose_no_permission"));
            return;
        }
        if (!claimCanManageTerminal(player, marker))
        {
            sendSystemMessage(player, new string_id("error_message", "prose_no_permission"));
            return;
        }

        if (!hasScript(marker, SCRIPT_NAME))
        {
            attachScript(marker, SCRIPT_NAME);
        }

        utils.setScriptVar(player, VAR_TERMINAL, marker);
        showMainMenu(marker, player);
    }

    // ----------------------------------------------------------------------

    public static void showMainMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        String prompt = buildDashboardPrompt(marker, player) + "\nSelect a category:";

        String[] options = new String[]
        {
            "Status & Condition",
            "Maintenance & Upkeep",
            "Tax & Visitor Revenue",
            "Access & Permissions",
            "Look-At Quick Actions",
            "Help & Information",
            "Close"
        };

        closePid(player, PID_MAIN);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim Management", options,
            "handleClaimMgmtMain", true, false);
        sui.setPid(player, pid, PID_MAIN);
    }

    public static void showStatusMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        String prompt = buildStatusDetailPrompt(marker, player);

        String[] options = new String[]
        {
            "Pay maintenance now...",
            "Refresh status",
            "Return to main menu"
        };

        closePid(player, PID_SUB);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim — Status", options,
            "handleClaimMgmtStatus", true, false);
        sui.setPid(player, pid, PID_SUB);
    }

    public static void showMaintenanceMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        int fee = getMaintenanceFee(marker);
        String prompt =
            "Maintenance & Upkeep\n"
            + "-------------------\n\n"
            + "Keep credits prepaid to avoid repossession of this claim.\n"
            + "Standard cycle: " + formatCredits(fee) + " per " + MAINTENANCE_INTERVAL_DAYS + " days.\n\n"
            + "Choose payment:";

        String[] options = new String[]
        {
            "Pay 1 cycle (" + formatCredits(fee) + ")",
            "Pay 3 cycles (" + formatCredits(fee * 3) + ")",
            "Pay 6 cycles (" + formatCredits(fee * 6) + ")",
            "Pay custom amount...",
            "Return to main menu"
        };

        closePid(player, PID_SUB);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim — Maintenance", options,
            "handleClaimMgmtMaintenance", true, false);
        sui.setPid(player, pid, PID_SUB);
    }

    public static void showTaxMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        int claimId = getIntObjVar(marker, "claim.id");
        int taxBalance = claimGetTaxBalance(claimId, DEFAULT_TAX_RESOURCE);

        String prompt =
            "Tax & Visitor Revenue\n"
            + "-------------------\n\n"
            + "Visitors who sample resources inside your claim pay a tax.\n"
            + "Taxed units are stored here for withdrawal by the owner.\n\n"
            + "Balance (" + DEFAULT_TAX_RESOURCE + "): " + taxBalance + "\n\n"
            + "Choose action:";

        String[] options = new String[]
        {
            "Withdraw 10 (" + DEFAULT_TAX_RESOURCE + ")",
            "Withdraw 100 (" + DEFAULT_TAX_RESOURCE + ")",
            "Withdraw custom amount...",
            "Withdraw other resource type...",
            "Refresh balance",
            "Return to main menu"
        };

        closePid(player, PID_SUB);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim — Tax Revenue", options,
            "handleClaimMgmtTax", true, false);
        sui.setPid(player, pid, PID_SUB);
    }

    public static void showAccessMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        String prompt =
            "Access & Permissions\n"
            + "-------------------\n\n"
            + "Owner (you): full management via this flag.\n"
            + "Granted players: decorate inside the footprint only.\n"
            + "Banned players: ejected and blocked from entry.\n\n"
            + "Manage by character first name:";

        String[] options = new String[]
        {
            "Grant decorate permission...",
            "Revoke decorate permission...",
            "Ban player from claim...",
            "Unban player from claim...",
            "Return to main menu"
        };

        closePid(player, PID_SUB);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim — Access", options,
            "handleClaimMgmtAccess", true, false);
        sui.setPid(player, pid, PID_SUB);
    }

    public static void showLookAtMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        obj_id target = getLookAtTarget(player);
        String targetLine = "No player targeted.";
        if (isIdValid(target) && isPlayer(target))
        {
            targetLine = "Current target: " + getName(target);
        }

        String prompt =
            "Look-At Quick Actions\n"
            + "-------------------\n\n"
            + targetLine + "\n\n"
            + "Face a player, then choose:";

        String[] options = new String[]
        {
            "Grant decorate to look-at target",
            "Revoke decorate from look-at target",
            "Ban look-at target from claim",
            "Unban look-at target from claim",
            "Return to main menu"
        };

        closePid(player, PID_SUB);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim — Quick Actions", options,
            "handleClaimMgmtLookAt", true, false);
        sui.setPid(player, pid, PID_SUB);
    }

    public static void showHelpMenu(obj_id marker, obj_id player) throws InterruptedException
    {
        String prompt =
            "Help & Information\n"
            + "-------------------\n\n"
            + "* Your claim is a circular open-world area centered on this flag.\n"
            + "* Prepay maintenance to keep the claim active.\n"
            + "* Visitor resource tax applies to sampling inside your footprint.\n"
            + "* Grant decorate permission to allow trusted players to place and\n"
            + "  move items inside the claim (not no-trade items on open ground).\n"
            + "* Banned players cannot enter and are ejected if inside.\n"
            + "* Only the claim owner may use this management panel.\n";

        String[] options = new String[]
        {
            "Return to main menu"
        };

        closePid(player, PID_SUB);
        int pid = sui.listbox(marker, player, prompt, sui.OK_CANCEL, "Claim — Help", options,
            "handleClaimMgmtHelp", true, false);
        sui.setPid(player, pid, PID_SUB);
    }

    // ----------------------------------------------------------------------

    public int handleClaimMgmtMain(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        if (row < 0 || row >= 6)
        {
            return SCRIPT_CONTINUE;
        }
        switch (row)
        {
            case 0:
                showStatusMenu(self, player);
                break;
            case 1:
                showMaintenanceMenu(self, player);
                break;
            case 2:
                showTaxMenu(self, player);
                break;
            case 3:
                showAccessMenu(self, player);
                break;
            case 4:
                showLookAtMenu(self, player);
                break;
            case 5:
                showHelpMenu(self, player);
                break;
            default:
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtStatus(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        switch (row)
        {
            case 0:
                showMaintenanceMenu(self, player);
                break;
            case 1:
                showStatusMenu(self, player);
                break;
            case 2:
            default:
                showMainMenu(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtMaintenance(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        int fee = getMaintenanceFee(self);
        switch (row)
        {
            case 0:
                payMaintenance(self, player, fee);
                break;
            case 1:
                payMaintenance(self, player, fee * 3);
                break;
            case 2:
                payMaintenance(self, player, fee * 6);
                break;
            case 3:
                promptInput(self, player, PENDING_PAY_CUSTOM, "Pay Maintenance",
                    "Enter credits to apply toward upkeep:", "handleClaimMgmtPayCustom");
                return SCRIPT_CONTINUE;
            case 4:
            default:
                showMainMenu(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtTax(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        switch (row)
        {
            case 0:
                withdrawTax(self, player, DEFAULT_TAX_RESOURCE, 10);
                break;
            case 1:
                withdrawTax(self, player, DEFAULT_TAX_RESOURCE, 100);
                break;
            case 2:
                promptInput(self, player, PENDING_WITHDRAW_CUSTOM, "Withdraw Tax",
                    "Enter amount to withdraw (" + DEFAULT_TAX_RESOURCE + "):", "handleClaimMgmtWithdrawCustom");
                return SCRIPT_CONTINUE;
            case 3:
                promptInput(self, player, PENDING_WITHDRAW_RESOURCE, "Withdraw Tax",
                    "Enter resource key (e.g. generic, ore, water):", "handleClaimMgmtWithdrawResource");
                return SCRIPT_CONTINUE;
            case 4:
                showTaxMenu(self, player);
                break;
            case 5:
            default:
                showMainMenu(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtAccess(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        switch (row)
        {
            case 0:
                promptInput(self, player, PENDING_GRANT, "Grant Decorate",
                    "Enter character first name:", "handleClaimMgmtNameAction");
                return SCRIPT_CONTINUE;
            case 1:
                promptInput(self, player, PENDING_REVOKE, "Revoke Decorate",
                    "Enter character first name:", "handleClaimMgmtNameAction");
                return SCRIPT_CONTINUE;
            case 2:
                promptInput(self, player, PENDING_BAN, "Ban From Claim",
                    "Enter character first name:", "handleClaimMgmtNameAction");
                return SCRIPT_CONTINUE;
            case 3:
                promptInput(self, player, PENDING_UNBAN, "Unban From Claim",
                    "Enter character first name:", "handleClaimMgmtNameAction");
                return SCRIPT_CONTINUE;
            case 4:
            default:
                showMainMenu(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtLookAt(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getLookAtTarget(player);
        if (!isIdValid(target) || !isPlayer(target))
        {
            sendSystemMessage(player, string_id.unlocalized("Face a player and try again."));
            showLookAtMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        switch (row)
        {
            case 0:
                grantAllowed(self, player, target);
                break;
            case 1:
                revokeAllowed(self, player, target);
                break;
            case 2:
                confirmBan(self, player, target);
                return SCRIPT_CONTINUE;
            case 3:
                unban(self, player, target);
                break;
            case 4:
            default:
                showMainMenu(self, player);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtHelp(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtPayCustom(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            showMaintenanceMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        int amount = parsePositiveInt(sui.getInputBoxText(params));
        if (amount <= 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid amount."));
            showMaintenanceMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        payMaintenance(self, player, amount);
        showMaintenanceMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtWithdrawCustom(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            showTaxMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        int amount = parsePositiveInt(sui.getInputBoxText(params));
        if (amount <= 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid amount."));
            showTaxMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        withdrawTax(self, player, DEFAULT_TAX_RESOURCE, amount);
        showTaxMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtWithdrawResource(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            showTaxMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        String key = sui.getInputBoxText(params);
        if (key == null || key.trim().equals(""))
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid resource key."));
            showTaxMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        promptInput(self, player, PENDING_WITHDRAW_CUSTOM, "Withdraw Tax",
            "Enter amount for '" + key.trim() + "':", "handleClaimMgmtWithdrawResourceAmount");
        utils.setScriptVar(player, "claim.mgmt.withdrawKey", key.trim().toLowerCase());
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtWithdrawResourceAmount(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            showTaxMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        String key = utils.getStringScriptVar(player, "claim.mgmt.withdrawKey");
        if (key == null || key.equals(""))
        {
            key = DEFAULT_TAX_RESOURCE;
        }
        int amount = parsePositiveInt(sui.getInputBoxText(params));
        if (amount <= 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid amount."));
            showTaxMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        withdrawTax(self, player, key, amount);
        showTaxMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtNameAction(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            showAccessMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        String action = utils.getStringScriptVar(player, VAR_PENDING_ACTION);
        String name = sui.getInputBoxText(params);
        obj_id target = resolvePlayerByFirstName(name);
        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("No online player matched that name."));
            showAccessMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        if (!isPlayer(target))
        {
            sendSystemMessage(player, string_id.unlocalized("That target is not a player."));
            showAccessMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        if (PENDING_GRANT.equals(action))
        {
            grantAllowed(self, player, target);
        }
        else if (PENDING_REVOKE.equals(action))
        {
            revokeAllowed(self, player, target);
        }
        else if (PENDING_BAN.equals(action))
        {
            utils.setScriptVar(player, VAR_PENDING_NAME, getName(target));
            utils.setScriptVar(player, "claim.mgmt.banTarget", target);
            sui.msgbox(self, player,
                "Ban " + getName(target) + " from this claim?\n\nThey will be ejected if inside and cannot re-enter.",
                sui.YES_NO, "Confirm Ban", sui.MSG_EXCLAMATION, "handleClaimMgmtBanConfirm");
            return SCRIPT_CONTINUE;
        }
        else if (PENDING_UNBAN.equals(action))
        {
            unban(self, player, target);
        }
        showAccessMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleClaimMgmtBanConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            showAccessMenu(self, player);
            return SCRIPT_CONTINUE;
        }
        obj_id target = utils.getObjIdScriptVar(player, "claim.mgmt.banTarget");
        if (isIdValid(target))
        {
            ban(self, player, target);
        }
        showAccessMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    // ----------------------------------------------------------------------

    private static void payMaintenance(obj_id terminal, obj_id player, int credits) throws InterruptedException
    {
        if (credits <= 0)
        {
            return;
        }
        if (claimPayMaintenance(player, terminal, credits))
        {
            sendSystemMessage(player, string_id.unlocalized("Applied " + formatCredits(credits) + " toward claim upkeep."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Maintenance payment failed. Check credits and ownership."));
        }
    }

    private static void withdrawTax(obj_id terminal, obj_id player, String resourceKey, int amount) throws InterruptedException
    {
        if (amount <= 0 || resourceKey == null || resourceKey.equals(""))
        {
            return;
        }
        if (claimWithdrawTax(player, terminal, resourceKey, amount))
        {
            sendSystemMessage(player, string_id.unlocalized("Withdrew " + amount + " from tax bucket '" + resourceKey + "'."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Withdraw failed (balance, permissions, or invalid amount)."));
        }
    }

    private static void grantAllowed(obj_id terminal, obj_id player, obj_id target) throws InterruptedException
    {
        if (claimAddAllowed(player, terminal, target))
        {
            sendSystemMessage(player, string_id.unlocalized("Decorate permission granted to " + getName(target) + "."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Could not grant permission."));
        }
    }

    private static void revokeAllowed(obj_id terminal, obj_id player, obj_id target) throws InterruptedException
    {
        if (claimRemoveAllowed(player, terminal, target))
        {
            sendSystemMessage(player, string_id.unlocalized("Decorate permission revoked from " + getName(target) + "."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Could not revoke permission."));
        }
    }

    private static void ban(obj_id terminal, obj_id player, obj_id target) throws InterruptedException
    {
        if (claimAddBan(player, terminal, target))
        {
            sendSystemMessage(player, string_id.unlocalized(getName(target) + " has been banned from this claim."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Ban failed."));
        }
    }

    private static void unban(obj_id terminal, obj_id player, obj_id target) throws InterruptedException
    {
        if (claimRemoveBan(player, terminal, target))
        {
            sendSystemMessage(player, string_id.unlocalized(getName(target) + " has been unbanned from this claim."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Unban failed."));
        }
    }

    private static void confirmBan(obj_id self, obj_id player, obj_id target) throws InterruptedException
    {
        utils.setScriptVar(player, "claim.mgmt.banTarget", target);
        sui.msgbox(self, player,
            "Ban " + getName(target) + " from this claim?\n\nThey will be ejected if inside and cannot re-enter.",
            sui.YES_NO, "Confirm Ban", sui.MSG_EXCLAMATION, "handleClaimMgmtBanConfirm");
    }

    private static void promptInput(obj_id self, obj_id player, String pendingAction, String title, String prompt, String handler) throws InterruptedException
    {
        utils.setScriptVar(player, VAR_PENDING_ACTION, pendingAction);
        closePid(player, PID_INPUT);
        int pid = sui.inputbox(self, player, prompt, title, handler, 32, false, "");
        sui.setPid(player, pid, PID_INPUT);
    }

    private static void closePid(obj_id player, String pidKey) throws InterruptedException
    {
        if (sui.hasPid(player, pidKey))
        {
            sui.closeSUI(player, sui.getPid(player, pidKey));
        }
    }

    private static String buildDashboardPrompt(obj_id marker, obj_id player) throws InterruptedException
    {
        int claimId = getIntObjVar(marker, "claim.id");
        float radius = getFloatObjVar(marker, "claim.footprint_radius_m");
        if (radius <= 0f)
        {
            radius = 32f;
        }
        location loc = getLocation(marker);
        String planet = loc.area;
        if (planet == null || planet.equals(""))
        {
            planet = "Unknown";
        }

        int taxBalance = claimGetTaxBalance(claimId, DEFAULT_TAX_RESOURCE);
        int fee = getMaintenanceFee(marker);
        int prepay = claimGetMaintenancePrepay(player, marker);
        int dueTime = claimGetMaintenanceDueTime(player, marker);
        int now = getGameTime();
        int secondsUntilDue = dueTime > now ? dueTime - now : 0;
        String dueLine = secondsUntilDue > 0 ? utils.formatTimeVerbose(secondsUntilDue) : "due now";

        return
            "-------------------\n"
            + "  OPEN WORLD CLAIM\n"
            + "-------------------\n\n"
            + "Claim ID:      " + claimId + "\n"
            + "Planet:        " + planet + "\n"
            + "Center:        " + Math.round(loc.x) + ", " + Math.round(loc.z) + "\n"
            + "Footprint:     " + formatRadius(radius) + " radius\n"
            + "Upkeep:        " + formatCredits(fee) + " / " + MAINTENANCE_INTERVAL_DAYS + " days\n"
            + "Prepaid:       " + formatCredits(prepay) + "\n"
            + "Next billing:  " + dueLine + "\n"
            + "Visitor tax:   " + VISITOR_TAX_PERCENT + "% (sampled resources)\n"
            + "Tax balance:   " + taxBalance + " (" + DEFAULT_TAX_RESOURCE + ")\n";
    }

    private static String buildStatusDetailPrompt(obj_id marker, obj_id player) throws InterruptedException
    {
        int claimId = getIntObjVar(marker, "claim.id");
        float radius = getFloatObjVar(marker, "claim.footprint_radius_m");
        if (radius <= 0f)
        {
            radius = 32f;
        }
        location loc = getLocation(marker);
        int fee = getMaintenanceFee(marker);
        int prepay = claimGetMaintenancePrepay(player, marker);
        int dueTime = claimGetMaintenanceDueTime(player, marker);
        int status = claimGetClaimStatus(player, marker);
        int now = getGameTime();
        int secondsUntilDue = dueTime > now ? dueTime - now : 0;
        String statusLine = status == 0 ? "Active" : "Suspended / repossession pending";
        String dueLine = secondsUntilDue > 0 ? utils.formatTimeVerbose(secondsUntilDue) : "billing overdue — pay upkeep immediately";
        int cyclesCovered = fee > 0 ? prepay / fee : 0;
        int remainder = fee > 0 ? prepay % fee : prepay;

        return
            "Status & Condition\n"
            + "-------------------\n\n"
            + "Condition:     " + statusLine + "\n"
            + "Claim ID:      " + claimId + "\n"
            + "Location:      " + loc.area + " @ " + Math.round(loc.x) + ", " + Math.round(loc.z) + "\n"
            + "Footprint:     " + formatRadius(radius) + " circular\n\n"
            + "Maintenance cycle: " + formatCredits(fee) + " every " + MAINTENANCE_INTERVAL_DAYS + " days\n"
            + "Prepaid credits:   " + formatCredits(prepay) + "\n"
            + "Cycles covered:    " + cyclesCovered + " full, " + formatCredits(remainder) + " toward next\n"
            + "Next billing:      " + dueLine + "\n\n"
            + "If billing lapses, the claim flag is repossessed and contents returned.";
    }

    private static int getMaintenanceFee(obj_id marker) throws InterruptedException
    {
        if (hasObjVar(marker, "claim.maintenance_fee_credits"))
        {
            int fee = getIntObjVar(marker, "claim.maintenance_fee_credits");
            if (fee > 0)
            {
                return fee;
            }
        }
        return MAINTENANCE_FEE_CREDITS;
    }

    private static obj_id resolvePlayerByFirstName(String name) throws InterruptedException
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

    private static int parsePositiveInt(String text) throws InterruptedException
    {
        if (text == null)
        {
            return -1;
        }
        try
        {
            return Integer.parseInt(text.trim());
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    private static String formatCredits(int amount) throws InterruptedException
    {
        String raw = Integer.toString(amount);
        StringBuilder sb = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; ++i)
        {
            if (i > 0 && (len - i) % 3 == 0)
            {
                sb.append(',');
            }
            sb.append(raw.charAt(i));
        }
        return sb.toString() + " cr";
    }

    private static String formatRadius(float radius) throws InterruptedException
    {
        int r = Math.round(radius);
        return Integer.toString(r) + "m";
    }
}
