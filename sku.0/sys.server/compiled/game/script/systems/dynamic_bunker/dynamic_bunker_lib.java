package script.systems.dynamic_bunker;

import script.*;

/**
 * Helpers for runtime bunker room grafting.
 *
 * Preferred entry (no setup):
 *   /dynamicBunker
 *   openDynamicBunkerFloorplanHere(player)
 *
 * Room catalog is discovered by the GameServer from object templates + POB cells.
 * roomId format: dyn|donorPob|donorCellIndex|donorPortalIndex
 */
public class dynamic_bunker_lib extends script.base_script
{
    public dynamic_bunker_lib()
    {
    }

    // Optional legacy terminal objvars (not required).
    public static final String OV_SOCKET_CELL = "dynamicBunker.socket.cell";
    public static final String OV_SOCKET_PORTAL = "dynamicBunker.socket.portal";
    public static final String OV_ASSIGNED_ROOM = "dynamicBunker.assignedRoom";

    /**
     * Open floorplan UI for the building the player is standing in.
     */
    public static boolean openHere(obj_id player) throws InterruptedException
    {
        return openDynamicBunkerFloorplanHere(player);
    }

    /**
     * Parse a runtime dyn| room id. Returns null if malformed.
     */
    public static dictionary parseDynRoomId(String roomId)
    {
        if (roomId == null || !roomId.startsWith("dyn|"))
        {
            return null;
        }

        int p1 = roomId.indexOf('|', 4);
        if (p1 < 0)
        {
            return null;
        }
        int p2 = roomId.indexOf('|', p1 + 1);
        if (p2 < 0)
        {
            return null;
        }

        String donorPob = roomId.substring(4, p1);
        int donorCell;
        int donorPortal;
        try
        {
            donorCell = Integer.parseInt(roomId.substring(p1 + 1, p2));
            donorPortal = Integer.parseInt(roomId.substring(p2 + 1));
        }
        catch (NumberFormatException e)
        {
            return null;
        }

        if (donorPob.length() < 1 || donorCell < 1 || donorPortal < 0)
        {
            return null;
        }

        dictionary def = new dictionary();
        def.put("room_id", roomId);
        def.put("donor_pob", donorPob);
        def.put("donor_cell_index", donorCell);
        def.put("donor_portal_index", donorPortal);
        def.put("socket_type", "auto");
        return def;
    }

    public static obj_id assignRoom(obj_id building, int hostCellIndex, int hostPortalIndex, String roomId) throws InterruptedException
    {
        if (!isIdValid(building) || roomId == null || roomId.length() < 1)
        {
            return null;
        }
        dictionary def = parseDynRoomId(roomId);
        if (def == null)
        {
            return null;
        }
        String donorPob = def.getString("donor_pob");
        int donorCell = def.getInt("donor_cell_index");
        int donorPortal = def.getInt("donor_portal_index");
        obj_id cell = addRoomHook(building, hostCellIndex, hostPortalIndex, donorPob, donorCell, donorPortal);
        if (isIdValid(cell))
        {
            setObjVar(building, OV_ASSIGNED_ROOM + "." + hostCellIndex + "." + hostPortalIndex, roomId);
        }
        return cell;
    }

    public static boolean linkPortals(obj_id building, int cellA, int portalA, int cellB, int portalB) throws InterruptedException
    {
        return linkRoomPortals(building, cellA, portalA, cellB, portalB);
    }
}
