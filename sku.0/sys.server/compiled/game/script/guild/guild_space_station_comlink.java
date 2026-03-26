package script.guild;

import script.dictionary;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.library.guild_space_station;
import script.library.utils;

/**
 * Inventory comlink: requests clearance and warps to the guild's dungeon_hub station instance.
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
                mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Contact Guild Station"));
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
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
        utils.setScriptVar(self, "guildStation.pendingPlayer", player);
        getClusterWideData(guild_space_station.CW_MANAGER, guild_space_station.cwElementName(guildId), false, self);
        return SCRIPT_CONTINUE;
    }

    public int OnClusterWideDataResponse(obj_id self, String manage_name, String name, int request_id, String[] element_name_list, dictionary[] data, int lock_key) throws InterruptedException
    {
        if (!manage_name.equals(guild_space_station.CW_MANAGER))
            return SCRIPT_CONTINUE;
        obj_id player = utils.getObjIdScriptVar(self, "guildStation.pendingPlayer");
        utils.removeScriptVar(self, "guildStation.pendingPlayer");
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
        guild_space_station.warpPlayerToStation(player, d);
        if (lock_key != 0)
            releaseClusterWideDataLock(manage_name, lock_key);
        return SCRIPT_CONTINUE;
    }
}
