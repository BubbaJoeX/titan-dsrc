package script.guild;

import script.dictionary;
import script.location;
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
 * Interior terminal: maintenance payment, access policy, orbit beacon refresh.
 */
public class guild_space_station_terminal extends script.base_script
{
    public guild_space_station_terminal()
    {
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id building = getTopMostContainer(self);
        if (!isIdValid(building) || !hasObjVar(building, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        if (getGuildId(player) != guildId && !isGod(player))
            return SCRIPT_CONTINUE;
        mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Pay Station Maintenance (500,000 cr)"));
        mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Access: All Guild Members"));
        mi.addRootMenu(menu_info_types.SERVER_MENU3, string_id.unlocalized("Access: Minimum Rank..."));
        mi.addRootMenu(menu_info_types.SERVER_MENU4, string_id.unlocalized("Refresh Orbit Beacon Here"));
        for (int mt : new int[] { menu_info_types.SERVER_MENU1, menu_info_types.SERVER_MENU2, menu_info_types.SERVER_MENU3, menu_info_types.SERVER_MENU4 })
        {
            menu_info_data md = mi.getMenuItemByType(mt);
            if (md != null)
                md.setServerNotify(true);
        }
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
            dictionary d = new dictionary();
            d.put("guildStationBuilding", building);
            money.requestPayment(player, money.ACCT_TRAVEL, guild_space_station.MAINTENANCE_COST_CREDITS, "guildStationMaintenancePaid", d, true);
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU2)
        {
            setObjVar(building, guild_space_station.OV_ACCESS_MODE, guild_space_station.ACCESS_GUILD);
            removeObjVar(building, guild_space_station.OV_ACCESS_MIN_RANK);
            removeObjVar(building, guild_space_station.OV_ACCESS_WHITELIST);
            guild_space_station.syncBuildingPermissions(building, guildId);
            messageTo(building, "guildStationPushCw", null, 0.5f, false);
            sendSystemMessage(player, string_id.unlocalized("Access: all guild members."));
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU3)
        {
            utils.setScriptVar(player, "guildStation.terminalBuilding", building);
            sui.inputbox(self, player, getString(string_id.unlocalized("Enter the minimum guild rank name (must match guild rank system).")), sui.OK_CANCEL, getString(string_id.unlocalized("Guild Rank Gate")), sui.INPUT_NORMAL, null, "handleGuildStationRankInput");
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU4)
        {
            location loc = getLocation(player);
            setObjVar(building, guild_space_station.OV_ORBIT_PLANET, loc.area);
            setObjVar(building, guild_space_station.OV_ORBIT_X, loc.x);
            setObjVar(building, guild_space_station.OV_ORBIT_Z, loc.z);
            obj_id old = hasObjVar(building, guild_space_station.OV_ORBIT_MARKER) ? getObjIdObjVar(building, guild_space_station.OV_ORBIT_MARKER) : obj_id.NULL_ID;
            obj_id m = guild_space_station.spawnOrbitMarkerForPlanet(player, guildId, loc.area, loc.x, loc.z, old);
            if (isIdValid(m))
                setObjVar(building, guild_space_station.OV_ORBIT_MARKER, m);
            messageTo(building, "guildStationPushCw", null, 0.5f, false);
            sendSystemMessage(player, string_id.unlocalized("Orbit beacon updated above your current position."));
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleGuildStationRankInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;
        String rank = sui.getInputBoxText(params);
        obj_id building = utils.getObjIdScriptVar(player, "guildStation.terminalBuilding");
        utils.removeScriptVar(player, "guildStation.terminalBuilding");
        if (!isIdValid(building) || rank == null || rank.length() < 1)
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(building, guild_space_station.OV_GUILD_ID);
        setObjVar(building, guild_space_station.OV_ACCESS_MODE, guild_space_station.ACCESS_RANK);
        setObjVar(building, guild_space_station.OV_ACCESS_MIN_RANK, rank);
        removeObjVar(building, guild_space_station.OV_ACCESS_WHITELIST);
        guild_space_station.syncBuildingPermissions(building, guildId);
        messageTo(building, "guildStationPushCw", null, 0.5f, false);
        sendSystemMessage(player, string_id.unlocalized("Access: guild rank " + rank + " and above."));
        return SCRIPT_CONTINUE;
    }
}
