package script.terminal;

import script.*;
import script.systems.dynamic_bunker.dynamic_bunker_lib;

/**
 * Opens the client bunker floorplan UI (room catalog + top-down preview + snap sockets).
 *
 * Objvars:
 *   dynamicBunker.socket.cell   (int) default host cell index
 *   dynamicBunker.socket.portal (int) default host portal index
 */
public class terminal_dynamic_bunker extends script.base_script
{
    public terminal_dynamic_bunker()
    {
    }

    public static final int MENU_ASSIGN_ROOM = menu_info_types.SERVER_MENU50;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Bunker Floorplan Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, dynamic_bunker_lib.OV_SOCKET_CELL) || !hasObjVar(self, dynamic_bunker_lib.OV_SOCKET_PORTAL))
        {
            return SCRIPT_CONTINUE;
        }
        mi.addRootMenu(MENU_ASSIGN_ROOM, string_id.unlocalized("Bunker Floorplan"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != MENU_ASSIGN_ROOM)
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, dynamic_bunker_lib.OV_SOCKET_CELL) || !hasObjVar(self, dynamic_bunker_lib.OV_SOCKET_PORTAL))
        {
            sendSystemMessageTestingOnly(player, "[DynamicBunker] Terminal is missing socket cell/portal objvars.");
            return SCRIPT_CONTINUE;
        }

        obj_id building = getTopMostContainer(self);
        if (!isIdValid(building))
        {
            sendSystemMessageTestingOnly(player, "[DynamicBunker] Could not resolve parent building.");
            return SCRIPT_CONTINUE;
        }

        int hostCell = getIntObjVar(self, dynamic_bunker_lib.OV_SOCKET_CELL);
        int hostPortal = getIntObjVar(self, dynamic_bunker_lib.OV_SOCKET_PORTAL);
        if (!openDynamicBunkerFloorplan(player, building, self, hostCell, hostPortal))
        {
            sendSystemMessageTestingOnly(player, "[DynamicBunker] Failed to open floorplan UI. Is the client build current?");
        }
        return SCRIPT_CONTINUE;
    }
}
