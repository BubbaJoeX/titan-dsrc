package script.guild;

import script.dictionary;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.guild_space_station;
import script.library.sui;
import script.library.utils;

/**
 * Inventory guild device (comlink): SUI menu for docking warp vs orbit beacon refresh.
 */
public class guild_space_station_comlink extends script.base_script
{
    public guild_space_station_comlink()
    {
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (getContainedBy(self) == utils.getInventoryContainer(player))
        {
            menu_info_data data = mi.getMenuItemByType(menu_info_types.ITEM_USE);
            if (data == null)
                mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Guild Station Comlink"));
            data = mi.getMenuItemByType(menu_info_types.ITEM_USE);
            if (data != null)
                data.setServerNotify(true);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE)
            return SCRIPT_CONTINUE;
        if (getContainedBy(self) != utils.getInventoryContainer(player))
        {
            sendSystemMessage(player, string_id.unlocalized("Keep the comlink in your inventory to use it."));
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        if (getGuildId(player) != getIntObjVar(self, guild_space_station.OV_GUILD_ID))
        {
            sendSystemMessage(player, string_id.unlocalized("This comlink is tuned to another guild."));
            return SCRIPT_CONTINUE;
        }
        String[] options = new String[]
        {
            "Request docking clearance (travel to guild station)",
            "Orbit beacon: place visual above your current position"
        };
        sui.listbox(self, player, "Select an action:", sui.OK_CANCEL, "Guild Station Comlink", options, "handleGuildComlinkMainMenu", true, false);
        return SCRIPT_CONTINUE;
    }

    public int handleGuildComlinkMainMenu(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;
        if (!isIdValid(player) || !exists(player))
            return SCRIPT_CONTINUE;
        if (getContainedBy(self) != utils.getInventoryContainer(player))
            return SCRIPT_CONTINUE;
        int row = sui.getListboxSelectedRow(params);
        if (row == 0)
        {
            utils.setScriptVar(self, "guildStation.pendingPlayer", player);
            utils.setScriptVar(self, guild_space_station.SV_COMLINK_CW_ACTION, guild_space_station.COMLINK_ACTION_WARP);
            int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
            getClusterWideData(guild_space_station.CW_MANAGER, guild_space_station.cwElementName(guildId), false, self);
            return SCRIPT_CONTINUE;
        }
        if (row == 1)
        {
            sui.msgbox(self, player, "Place the orbital station prop roughly 3000m above your current position on this planet? You must be outdoors on the correct planet.", sui.OK_CANCEL, "Orbit Beacon", sui.MSG_NORMAL, "handleGuildComlinkOrbitConfirm");
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleGuildComlinkOrbitConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;
        if (!isIdValid(player) || !exists(player))
            return SCRIPT_CONTINUE;
        if (getContainedBy(self) != utils.getInventoryContainer(player))
            return SCRIPT_CONTINUE;
        utils.setScriptVar(self, "guildStation.pendingPlayer", player);
        utils.setScriptVar(self, guild_space_station.SV_COMLINK_CW_ACTION, guild_space_station.COMLINK_ACTION_ORBIT);
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
        getClusterWideData(guild_space_station.CW_MANAGER, guild_space_station.cwElementName(guildId), false, self);
        return SCRIPT_CONTINUE;
    }

    public int OnClusterWideDataResponse(obj_id self, String manage_name, String name, int request_id, String[] element_name_list, dictionary[] data, int lock_key) throws InterruptedException
    {
        if (!manage_name.equals(guild_space_station.CW_MANAGER))
            return SCRIPT_CONTINUE;
        obj_id player = utils.getObjIdScriptVar(self, "guildStation.pendingPlayer");
        utils.removeScriptVar(self, "guildStation.pendingPlayer");
        String action = utils.hasScriptVar(self, guild_space_station.SV_COMLINK_CW_ACTION) ? utils.getStringScriptVar(self, guild_space_station.SV_COMLINK_CW_ACTION) : guild_space_station.COMLINK_ACTION_WARP;
        utils.removeScriptVar(self, guild_space_station.SV_COMLINK_CW_ACTION);
        if (!isIdValid(player) || !exists(player))
        {
            if (lock_key != 0)
                releaseClusterWideDataLock(manage_name, lock_key);
            return SCRIPT_CONTINUE;
        }
        if (data == null || data.length < 1 || data[0] == null)
        {
            sendSystemMessage(player, string_id.unlocalized("[Comlink] No guild station registration found."));
            if (lock_key != 0)
                releaseClusterWideDataLock(manage_name, lock_key);
            return SCRIPT_CONTINUE;
        }
        dictionary d = data[0];
        int guildId = d.getInt("guild_id");
        if (guild_space_station.COMLINK_ACTION_ORBIT.equals(action))
        {
            guild_space_station.applyOrbitBeaconRefreshFromComlink(player, guildId, d);
        }
        else
        {
            guild_space_station.warpPlayerToStation(player, d);
        }
        if (lock_key != 0)
            releaseClusterWideDataLock(manage_name, lock_key);
        return SCRIPT_CONTINUE;
    }
}
