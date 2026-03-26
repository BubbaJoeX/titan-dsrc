package script.guild;

import script.dictionary;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.guild_space_station;
import script.library.money;
import script.library.sui;
import script.library.utils;

/**
 * Interior terminal: hierarchical SUI for maintenance, access, orbit info, and cluster sync.
 * Root radial uses a single entry to avoid menu spam.
 */
public class guild_space_station_terminal extends script.base_script
{
    private static final String SV_MGMT_BUILDING = "guildStation.mgmt.building";
    private static final String SV_MGMT_TERMINAL = "guildStation.mgmt.terminal";
    private static final String SV_WHITELIST_IDS = "guildStation.mgmt.whitelistIds";

    public guild_space_station_terminal()
    {
    }

    private static boolean mayConfigureStation(obj_id player, int guildId) throws InterruptedException
    {
        if (isGod(player))
            return true;
        obj_id leader = guildGetLeader(guildId);
        return isIdValid(leader) && leader == player;
    }

    private static void bindMgmt(obj_id terminal, obj_id player, obj_id building) throws InterruptedException
    {
        utils.setScriptVar(player, SV_MGMT_TERMINAL, terminal);
        utils.setScriptVar(player, SV_MGMT_BUILDING, building);
    }

    private static void clearMgmt(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, SV_MGMT_TERMINAL);
        utils.removeScriptVar(player, SV_MGMT_BUILDING);
    }

    private static obj_id getMgmtBuilding(obj_id player) throws InterruptedException
    {
        return utils.getObjIdScriptVar(player, SV_MGMT_BUILDING);
    }

    private int safeGuildId(obj_id building) throws InterruptedException
    {
        if (!isIdValid(building) || !hasObjVar(building, guild_space_station.OV_GUILD_ID))
            return 0;
        return getIntObjVar(building, guild_space_station.OV_GUILD_ID);
    }

    private void openMainMenu(obj_id self, obj_id player, obj_id building) throws InterruptedException
    {
        bindMgmt(self, player, building);
        String[] rows = {
            "Status & overview",
            "Maintenance...",
            "Access control...",
            "Orbit beacon...",
            "Tools & sync..."
        };
        sui.listbox(self, player, "Choose a category.", sui.OK_CANCEL, "Guild Station", rows, "handleStationMgmtRoot", true);
    }

    private void openMaintenanceSub(obj_id self, obj_id player) throws InterruptedException
    {
        String[] rows = {
            "Pay maintenance (" + guild_space_station.MAINTENANCE_COST_CREDITS + " credits)",
            "Next billing cycle",
            "<- Main menu"
        };
        sui.listbox(self, player, "Maintenance options.", sui.OK_CANCEL, "Maintenance", rows, "handleStationMgmtMaintenance", true);
    }

    private void openAccessSub(obj_id self, obj_id player, boolean leader) throws InterruptedException
    {
        if (leader)
        {
            String[] rows = {
                "Set access: all guild members",
                "Set access: minimum guild rank...",
                "Set access: whitelist only",
                "Whitelist: add character (first name)...",
                "Whitelist: remove character...",
                "View current policy",
                "<- Main menu"
            };
            sui.listbox(self, player, "Who may enter the station interior?", sui.OK_CANCEL, "Access control", rows, "handleStationMgmtAccess", true);
        }
        else
        {
            String[] rows = {
                "View current policy",
                "<- Main menu"
            };
            sui.listbox(self, player, "Access information (configuration is leader-only).", sui.OK_CANCEL, "Access control", rows, "handleStationMgmtAccessMember", true);
        }
    }

    private void openOrbitSub(obj_id self, obj_id player) throws InterruptedException
    {
        String[] rows = {
            "Show orbit / beacon details",
            "How to move the orbit beacon",
            "<- Main menu"
        };
        sui.listbox(self, player, "Orbital beacon (surface coordinates).", sui.OK_CANCEL, "Orbit beacon", rows, "handleStationMgmtOrbit", true);
    }

    private void openToolsSub(obj_id self, obj_id player, boolean leader) throws InterruptedException
    {
        if (leader)
        {
            String[] rows = {
                "Grant replacement guild comlink",
                "Push registration to cluster (sync)",
                "<- Main menu"
            };
            sui.listbox(self, player, "Leader tools.", sui.OK_CANCEL, "Tools", rows, "handleStationMgmtTools", true);
        }
        else
        {
            String[] rows = { "<- Main menu" };
            sui.listbox(self, player, "Only the guild leader may use station tools.", sui.OK_CANCEL, "Tools", rows, "handleStationMgmtToolsMember", true);
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id building = getTopMostContainer(self);
        if (!isIdValid(building) || !hasObjVar(building, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        if (getGuildId(player) != guildId && !isGod(player))
            return SCRIPT_CONTINUE;
        mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Guild station..."));
        menu_info_data md = mi.getMenuItemByType(menu_info_types.SERVER_MENU1);
        if (md != null)
            md.setServerNotify(true);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id building = getTopMostContainer(self);
        if (!isIdValid(building) || !hasObjVar(building, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        if (getGuildId(player) != guildId && !isGod(player))
            return SCRIPT_CONTINUE;

        if (item == menu_info_types.SERVER_MENU1)
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtRoot(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building) || !hasObjVar(building, guild_space_station.OV_GUILD_ID))
        {
            clearMgmt(player);
            return SCRIPT_CONTINUE;
        }
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            clearMgmt(player);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (bp == sui.BP_OK && idx < 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Select an entry, then confirm."));
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        switch (idx)
        {
            case 0:
                sui.msgbox(self, player, guild_space_station.formatStationStatusSummary(building, guildId), sui.OK_ONLY, "Station status", sui.MSG_NORMAL, "noHandler");
                break;
            case 1:
                openMaintenanceSub(self, player);
                break;
            case 2:
                openAccessSub(self, player, mayConfigureStation(player, guildId));
                break;
            case 3:
                openOrbitSub(self, player);
                break;
            case 4:
                openToolsSub(self, player, mayConfigureStation(player, guildId));
                break;
            default:
                openMainMenu(self, player, building);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtMaintenance(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (bp == sui.BP_OK && idx < 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Select an entry, then confirm."));
            openMaintenanceSub(self, player);
            return SCRIPT_CONTINUE;
        }
        if (idx == 0)
        {
            dictionary d = new dictionary();
            d.put("guildStationBuilding", building);
            money.requestPayment(player, money.ACCT_TRAVEL, guild_space_station.MAINTENANCE_COST_CREDITS, "guildStationMaintenancePaid", d, true);
            openMaintenanceSub(self, player);
            return SCRIPT_CONTINUE;
        }
        if (idx == 1)
        {
            String msg;
            if (hasObjVar(building, guild_space_station.OV_MAINTENANCE_NEXT))
            {
                int t = getIntObjVar(building, guild_space_station.OV_MAINTENANCE_NEXT);
                msg = "Next billing due: " + getCalendarTimeStringLocal(t) + "\n";
                if (getCalendarTime() >= t)
                    msg += "Status: OVERDUE - pay from this terminal.";
                else
                    msg += "Status: current.";
            }
            else
                msg = "No billing cycle is set yet. Pay maintenance once to schedule the next due date.";
            sui.msgbox(self, player, msg, sui.OK_ONLY, "Maintenance schedule", sui.MSG_NORMAL, "noHandler");
            openMaintenanceSub(self, player);
            return SCRIPT_CONTINUE;
        }
        openMainMenu(self, player, building);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtAccessMember(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (idx == 0)
            sui.msgbox(self, player, guild_space_station.describeAccessPolicy(building), sui.OK_ONLY, "Access policy", sui.MSG_NORMAL, "noHandler");
        openMainMenu(self, player, building);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtAccess(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (bp == sui.BP_OK && idx < 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Select an entry, then confirm."));
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        if (idx == 0)
        {
            setObjVar(building, guild_space_station.OV_ACCESS_MODE, guild_space_station.ACCESS_GUILD);
            removeObjVar(building, guild_space_station.OV_ACCESS_MIN_RANK);
            removeObjVar(building, guild_space_station.OV_ACCESS_WHITELIST);
            guild_space_station.syncBuildingPermissions(building, guildId);
            messageTo(building, "guildStationPushCw", null, 0.5f, false);
            sendSystemMessage(player, string_id.unlocalized("Access: all guild members."));
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        if (idx == 1)
        {
            sui.inputbox(self, player, getString(string_id.unlocalized("Enter the minimum guild rank (must match your guild rank names).")), sui.OK_CANCEL, getString(string_id.unlocalized("Minimum rank")), sui.INPUT_NORMAL, null, "handleStationMgmtRankInput");
            return SCRIPT_CONTINUE;
        }
        if (idx == 2)
        {
            setObjVar(building, guild_space_station.OV_ACCESS_MODE, guild_space_station.ACCESS_WHITELIST);
            removeObjVar(building, guild_space_station.OV_ACCESS_MIN_RANK);
            if (!hasObjVar(building, guild_space_station.OV_ACCESS_WHITELIST))
                setObjVar(building, guild_space_station.OV_ACCESS_WHITELIST, "");
            guild_space_station.syncBuildingPermissions(building, guildId);
            messageTo(building, "guildStationPushCw", null, 0.5f, false);
            sendSystemMessage(player, string_id.unlocalized("Access: whitelist only. Add names below if the list is empty."));
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        if (idx == 3)
        {
            sui.inputbox(self, player, getString(string_id.unlocalized("Character FIRST name to whitelist (exact match, case-insensitive).")), sui.OK_CANCEL, getString(string_id.unlocalized("Add to whitelist")), sui.INPUT_NORMAL, null, "handleStationMgmtWhitelistAdd");
            return SCRIPT_CONTINUE;
        }
        if (idx == 4)
        {
            String packed = hasObjVar(building, guild_space_station.OV_ACCESS_WHITELIST) ? getStringObjVar(building, guild_space_station.OV_ACCESS_WHITELIST) : "";
            obj_id[] ids = guild_space_station.parseWhitelistObjIds(packed);
            if (ids.length < 1)
            {
                sui.msgbox(self, player, "The whitelist is empty.", sui.OK_ONLY, "Whitelist", sui.MSG_NORMAL, "noHandler");
                openAccessSub(self, player, true);
                return SCRIPT_CONTINUE;
            }
            String[] rows = new String[ids.length];
            for (int i = 0; i < ids.length; i++)
                rows[i] = "Remove: " + getPlayerName(ids[i]);
            utils.setScriptVar(player, SV_WHITELIST_IDS, ids);
            sui.listbox(self, player, "Select a character to remove from the whitelist.", sui.OK_CANCEL, "Remove from whitelist", rows, "handleStationMgmtWhitelistRemove", true);
            return SCRIPT_CONTINUE;
        }
        if (idx == 5)
        {
            sui.msgbox(self, player, guild_space_station.describeAccessPolicy(building), sui.OK_ONLY, "Access policy", sui.MSG_NORMAL, "noHandler");
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        openMainMenu(self, player, building);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtWhitelistRemove(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
        {
            utils.removeScriptVar(player, SV_WHITELIST_IDS);
            return SCRIPT_CONTINUE;
        }
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL || !utils.hasScriptVar(player, SV_WHITELIST_IDS))
        {
            utils.removeScriptVar(player, SV_WHITELIST_IDS);
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        obj_id[] ids = utils.getObjIdArrayScriptVar(player, SV_WHITELIST_IDS);
        utils.removeScriptVar(player, SV_WHITELIST_IDS);
        if (ids == null || ids.length < 1)
        {
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0 || idx >= ids.length)
        {
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        String packed = hasObjVar(building, guild_space_station.OV_ACCESS_WHITELIST) ? getStringObjVar(building, guild_space_station.OV_ACCESS_WHITELIST) : "";
        packed = guild_space_station.whitelistRemovePacked(packed, ids[idx]);
        setObjVar(building, guild_space_station.OV_ACCESS_WHITELIST, packed);
        guild_space_station.syncBuildingPermissions(building, guildId);
        messageTo(building, "guildStationPushCw", null, 0.5f, false);
        sendSystemMessage(player, string_id.unlocalized("Removed from station whitelist."));
        openAccessSub(self, player, true);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtWhitelistAdd(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        String raw = sui.getInputBoxText(params);
        String name = raw == null ? "" : raw.trim();
        if (name.length() < 1)
        {
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        obj_id target = getPlayerIdFromFirstName(name.toLowerCase());
        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("No character found with that first name."));
            openAccessSub(self, player, true);
            return SCRIPT_CONTINUE;
        }
        String packed = hasObjVar(building, guild_space_station.OV_ACCESS_WHITELIST) ? getStringObjVar(building, guild_space_station.OV_ACCESS_WHITELIST) : "";
        packed = guild_space_station.whitelistAddPacked(packed, target);
        setObjVar(building, guild_space_station.OV_ACCESS_WHITELIST, packed);
        setObjVar(building, guild_space_station.OV_ACCESS_MODE, guild_space_station.ACCESS_WHITELIST);
        removeObjVar(building, guild_space_station.OV_ACCESS_MIN_RANK);
        guild_space_station.syncBuildingPermissions(building, guildId);
        messageTo(building, "guildStationPushCw", null, 0.5f, false);
        sendSystemMessage(player, string_id.unlocalized("Added to whitelist: " + getPlayerName(target)));
        openAccessSub(self, player, true);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtOrbit(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (idx == 0)
            sui.msgbox(self, player, guild_space_station.formatOrbitBeaconDetails(building), sui.OK_ONLY, "Orbit beacon", sui.MSG_NORMAL, "noHandler");
        else if (idx == 1)
        {
            sui.msgbox(
                self,
                player,
                "To move the orbital beacon, take your guild station comlink to the desired planet. Stand outdoors at the ground point you want, then use the comlink and choose to refresh the orbit beacon.\n\nYou cannot relocate the beacon from this hub terminal.",
                sui.OK_ONLY,
                "Relocate beacon",
                sui.MSG_NORMAL,
                "noHandler");
        }
        else
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        openOrbitSub(self, player);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtTools(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (idx == 0)
        {
            guild_space_station.grantComlink(player, guildId);
            sendSystemMessage(player, string_id.unlocalized("A replacement comlink was added to your inventory."));
        }
        else if (idx == 1)
        {
            messageTo(building, "guildStationPushCw", null, 0.5f, false);
            sendSystemMessage(player, string_id.unlocalized("Cluster registration update queued."));
        }
        else
        {
            openMainMenu(self, player, building);
            return SCRIPT_CONTINUE;
        }
        openToolsSub(self, player, true);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtToolsMember(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id building = getMgmtBuilding(player);
        if (!isIdValid(building) || !exists(building))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        openMainMenu(self, player, building);
        return SCRIPT_CONTINUE;
    }

    public int handleStationMgmtRankInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id buildingRef = getMgmtBuilding(player);
        if (!isIdValid(buildingRef))
            buildingRef = utils.getObjIdScriptVar(player, "guildStation.terminalBuilding");
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
        {
            openAccessSub(self, player, mayConfigureStation(player, safeGuildId(buildingRef)));
            return SCRIPT_CONTINUE;
        }
        String rank = sui.getInputBoxText(params);
        obj_id building = utils.getObjIdScriptVar(player, SV_MGMT_BUILDING);
        if (!isIdValid(building))
            building = utils.getObjIdScriptVar(player, "guildStation.terminalBuilding");
        if (utils.hasScriptVar(player, "guildStation.terminalBuilding"))
            utils.removeScriptVar(player, "guildStation.terminalBuilding");
        if (!isIdValid(building) || rank == null || rank.length() < 1)
        {
            openAccessSub(self, player, mayConfigureStation(player, safeGuildId(building)));
            return SCRIPT_CONTINUE;
        }
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        setObjVar(building, guild_space_station.OV_ACCESS_MODE, guild_space_station.ACCESS_RANK);
        setObjVar(building, guild_space_station.OV_ACCESS_MIN_RANK, rank);
        removeObjVar(building, guild_space_station.OV_ACCESS_WHITELIST);
        guild_space_station.syncBuildingPermissions(building, guildId);
        messageTo(building, "guildStationPushCw", null, 0.5f, false);
        sendSystemMessage(player, string_id.unlocalized("Access: guild rank " + rank + " and above."));
        openAccessSub(self, player, mayConfigureStation(player, guildId));
        return SCRIPT_CONTINUE;
    }

    /** Legacy radial callback name; forwards to {@link #handleStationMgmtRankInput}. */
    public int handleGuildStationRankInput(obj_id self, dictionary params) throws InterruptedException
    {
        return handleStationMgmtRankInput(self, params);
    }
}
