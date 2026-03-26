package script.guild;

import script.dictionary;
import script.location;
import script.obj_id;
import script.string_id;
import script.library.guild_space_station;

/**
 * Guild space station building (dungeon_hub). Handles permission sync and maintenance reminders.
 */
public class guild_space_station_building extends script.base_script
{
    public guild_space_station_building()
    {
    }

    public static final float TICK_S = 300.0f;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, "guildStation.spawnedTerminal"))
        {
            obj_id cell = getCellId(self, "hangarbay1");
            if (isIdValid(cell))
            {
                location loc = new location(5.0f, 0.0f, 5.0f, "dungeon_hub", cell);
                obj_id term = createObjectInCell("object/tangible/terminal/terminal_quad_screen.iff", self, "hangarbay1", loc);
                if (isIdValid(term))
                    attachScript(term, "guild.guild_space_station_terminal");
                setObjVar(self, "guildStation.spawnedTerminal", true);
            }
        }
        messageTo(self, "guildStationTick", null, TICK_S, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "guildStationTick", null, TICK_S, false);
        return SCRIPT_CONTINUE;
    }

    public int guildStationTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
        guild_space_station.syncBuildingPermissions(self, guildId);
        if (guild_space_station.isMaintenanceOverdue(self))
        {
            obj_id[] members = guildGetMemberIds(guildId);
            if (members != null)
            {
                for (obj_id m : members)
                {
                    if (isIdValid(m) && exists(m) && isPlayer(m))
                        sendSystemMessage(m, string_id.unlocalized("[Guild Station] Maintenance overdue. Pay at the station management terminal."));
                }
            }
        }
        messageTo(self, "guildStationTick", null, TICK_S, false);
        return SCRIPT_CONTINUE;
    }

    public int guildStationPushCw(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
        getClusterWideData(guild_space_station.CW_MANAGER, guild_space_station.cwElementName(guildId), true, self);
        return SCRIPT_CONTINUE;
    }

    public int OnClusterWideDataResponse(obj_id self, String manage_name, String name, int request_id, String[] element_name_list, dictionary[] data, int lock_key) throws InterruptedException
    {
        if (!manage_name.equals(guild_space_station.CW_MANAGER))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
        guild_space_station.onClusterWidePushResponse(self, guildId, manage_name, name, request_id, element_name_list, data, lock_key);
        return SCRIPT_CONTINUE;
    }
}
