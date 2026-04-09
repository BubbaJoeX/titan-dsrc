package script.terminal;

import script.*;
import script.systems.apartment.apartment_lib;

public class apartment_room_terminal extends script.base_script
{
    public apartment_room_terminal()
    {
    }

    public static final int MENU_RENEW_ROOM = menu_info_types.SERVER_MENU53;
    public static final int MENU_ROOM_STATUS = menu_info_types.SERVER_MENU52;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Apartment Room Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, apartment_lib.OV_TERMINAL_BUILDING) || !hasObjVar(self, apartment_lib.OV_TERMINAL_CELL))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id building = getObjIdObjVar(self, apartment_lib.OV_TERMINAL_BUILDING);
        String cellName = getStringObjVar(self, apartment_lib.OV_TERMINAL_CELL);
        if (!isIdValid(building) || cellName == null || cellName.length() < 1)
        {
            return SCRIPT_CONTINUE;
        }
        if (!apartment_lib.isApartmentBuilding(building))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id tenant = apartment_lib.getUnitTenant(building, cellName);
        if (isIdValid(tenant) && tenant == player)
        {
            mi.addRootMenu(MENU_RENEW_ROOM, string_id.unlocalized("Renew This Room"));
            mi.addRootMenu(MENU_ROOM_STATUS, string_id.unlocalized("Show Room Status"));
        }
        else if (isGod(player) || player == getOwner(building))
        {
            mi.addRootMenu(MENU_ROOM_STATUS, string_id.unlocalized("Show Room Status"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!hasObjVar(self, apartment_lib.OV_TERMINAL_BUILDING) || !hasObjVar(self, apartment_lib.OV_TERMINAL_CELL))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id building = getObjIdObjVar(self, apartment_lib.OV_TERMINAL_BUILDING);
        String cellName = getStringObjVar(self, apartment_lib.OV_TERMINAL_CELL);
        if (!isIdValid(building) || cellName == null || cellName.length() < 1)
        {
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_RENEW_ROOM)
        {
            if (apartment_lib.renewSingleRoom(building, player, cellName))
            {
                sendSystemMessageTestingOnly(player, "[Apartment] Renewed room " + cellName + ".");
            }
            else
            {
                sendSystemMessageTestingOnly(player, "[Apartment] Renewal failed for " + cellName + ".");
            }
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_ROOM_STATUS)
        {
            String status = apartment_lib.getUnitStatus(building, cellName);
            String ownerText = apartment_lib.getUnitTenantName(building, cellName);
            int nextDue = apartment_lib.getUnitNextDue(building, cellName);
            if (ownerText == null || ownerText.length() < 1)
            {
                ownerText = "None";
            }
            sendSystemMessageTestingOnly(player, "[Apartment] Room: " + cellName + " | Status: " + status + " | Owner: " + ownerText + " | NextDue(GameTime): " + nextDue);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
}
