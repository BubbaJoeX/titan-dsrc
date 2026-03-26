package script.guild;

import script.dictionary;
import script.obj_id;
import script.string_id;
import script.library.guild_space_station;
import script.library.utils;

/**
 * Atmospheric prop ~3000m above the orbit point; players request docking (same as comlink).
 */
public class guild_space_station_orbit_marker extends script.base_script
{
    public guild_space_station_orbit_marker()
    {
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Request Docking Clearance"));
        menu_info_data data = mi.getMenuItemByType(menu_info_types.ITEM_USE);
        if (data != null)
            data.setServerNotify(true);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE)
            return SCRIPT_CONTINUE;
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
            sendSystemMessage(player, string_id.unlocalized("[Navicomputer] Station not registered."));
            if (lock_key != 0)
                releaseClusterWideDataLock(manage_name, lock_key);
            return SCRIPT_CONTINUE;
        }
        guild_space_station.warpPlayerToStation(player, data[0]);
        if (lock_key != 0)
            releaseClusterWideDataLock(manage_name, lock_key);
        return SCRIPT_CONTINUE;
    }
}
