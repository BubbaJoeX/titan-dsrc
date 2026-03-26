package script.guild;

import script.conversation.base.ConvoResponse;
import script.conversation.base.conversation_base;
import script.dictionary;
import script.library.ai_lib;
import script.library.guild_space_station;
import script.library.utils;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

/**
 * Orbit beacon: invulnerable. The template is a {@code ShipObject}, so the radial often omits {@code CONVERSE_START};
 * we always add {@code SERVER_MENU1}/{@code SERVER_MENU2}. {@code /comm} and {@code conversationStart} still use the
 * normal NPC conversation path ({@link #OnStartNpcConversation}) — that must not return {@code SCRIPT_OVERRIDE} alone or
 * those commands do nothing on the client.
 */
public class guild_space_station_orbit_marker extends conversation_base
{
    public static final String CONVERSATION = "guild.guild_space_station_orbit_marker";
    public static final String SCRIPT_NAME = "guild_space_station_orbit_marker";
    private static final int BRANCH_MAIN = 1;

    public guild_space_station_orbit_marker()
    {
        super.scriptName = SCRIPT_NAME;
        super.conversation = CONVERSATION;
    }

    @Override
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        guild_space_station.applyOrbitMarkerPresentation(self);
        return SCRIPT_CONTINUE;
    }

    @Override
    public int OnAttach(obj_id self) throws InterruptedException
    {
        guild_space_station.applyOrbitMarkerPresentation(self);
        return SCRIPT_CONTINUE;
    }

    @Override
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException
    {
        // Same pattern as conversation_base: client handles CONVERSE_START (npcConversationStart) without server notify.
        int convMenu = menuInfo.addRootMenu(menu_info_types.CONVERSE_START, null);
        menu_info_data convData = menuInfo.getMenuItemById(convMenu);
        if (convData != null)
            convData.setServerNotify(false);
        menuInfo.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Request Landing"));
        menuInfo.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Station Information"));
        menu_info_data md1 = menuInfo.getMenuItemByType(menu_info_types.SERVER_MENU1);
        if (md1 != null)
            md1.setServerNotify(true);
        menu_info_data md2 = menuInfo.getMenuItemByType(menu_info_types.SERVER_MENU2);
        if (md2 != null)
            md2.setServerNotify(true);
        setCondition(self, CONDITION_CONVERSABLE);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.SERVER_MENU1 && item != menu_info_types.SERVER_MENU2)
            return SCRIPT_CONTINUE;
        if (!hasObjVar(self, guild_space_station.OV_GUILD_ID))
            return SCRIPT_CONTINUE;
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);
        if (item == menu_info_types.SERVER_MENU1)
        {
            handleOrbitLandingRequest(self, player, guildId);
            return SCRIPT_CONTINUE;
        }
        guild_space_station.showStationInformationToPlayer(player, guildId);
        return SCRIPT_CONTINUE;
    }

    private void handleOrbitLandingRequest(obj_id self, obj_id player, int guildId) throws InterruptedException
    {
        utils.setScriptVar(self, "guildStation.pendingPlayer", player);
        getClusterWideData(guild_space_station.CW_MANAGER, guild_space_station.cwElementName(guildId), false, self);
    }

    @Override
    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        // Do not return SCRIPT_OVERRIDE alone — the client needs npcEndConversationWithMessage or it never clears comm UI.
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
            return serverSide_endConversation(player, "Unable to establish a comm channel while in combat.");
        if (!hasObjVar(self, guild_space_station.OV_GUILD_ID))
            return serverSide_endConversation(player, "This beacon is not transmitting a guild identity.");
        return serverSide_startConversation(
            player,
            self,
            "Guild orbital station beacon online.\nHow may I direct your traffic?",
            BRANCH_MAIN,
            new ConvoResponse[] {
                convo("landing", "Request Landing."),
                convo("station_info", "Station Information.")
            }
        );
    }

    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(SCRIPT_NAME))
            return SCRIPT_CONTINUE;
        if (!utils.hasScriptVar(player, conversation + ".branchId"))
            return SCRIPT_CONTINUE;
        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");
        if (branchId != BRANCH_MAIN)
        {
            utils.removeScriptVar(player, conversation + ".branchId");
            return SCRIPT_CONTINUE;
        }
        int guildId = getIntObjVar(self, guild_space_station.OV_GUILD_ID);

        if (responseIdIs(response, "landing"))
        {
            serverSide_endConversation(player, "Acknowledged. Navicomputer is processing your clearance request.");
            handleOrbitLandingRequest(self, player, guildId);
            return SCRIPT_CONTINUE;
        }
        if (responseIdIs(response, "station_info"))
        {
            guild_space_station.showStationInformationToPlayer(player, guildId);
            return serverSide_endConversation(player, "End of transmission.");
        }
        utils.removeScriptVar(player, conversation + ".branchId");
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
        guild_space_station.handleOrbitLandingClusterResponse(player, data[0], self);
        if (lock_key != 0)
            releaseClusterWideDataLock(manage_name, lock_key);
        return SCRIPT_CONTINUE;
    }

    /** Clears deferred POB docking prompts if guild members leave the SUI open past the timeout. */
    public int guildPobLandingTimeout(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        if (!isIdValid(ship) || !exists(ship))
            return SCRIPT_CONTINUE;
        guild_space_station.clearPobLandingState(ship);
        return SCRIPT_CONTINUE;
    }
}
