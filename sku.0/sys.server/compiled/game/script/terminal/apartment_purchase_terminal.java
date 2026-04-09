package script.terminal;

import script.*;
import script.systems.apartment.apartment_lib;

public class apartment_purchase_terminal extends script.base_script
{
    public apartment_purchase_terminal()
    {
    }

    public static final int MENU_PURCHASE_ROOM = menu_info_types.SERVER_MENU51;
    public static final int MENU_RENEW_ROOMS = menu_info_types.SERVER_MENU52;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Apartment Rental Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id building = hasObjVar(self, apartment_lib.OV_TERMINAL_BUILDING) ? getObjIdObjVar(self, apartment_lib.OV_TERMINAL_BUILDING) : obj_id.NULL_ID;
        if (!isIdValid(building) || !exists(building))
        {
            return SCRIPT_CONTINUE;
        }
        if (!apartment_lib.isApartmentBuilding(building))
        {
            return SCRIPT_CONTINUE;
        }

        mi.addRootMenu(MENU_PURCHASE_ROOM, string_id.unlocalized("Purchase Room"));
        if (apartment_lib.countRoomsForPlayer(building, player) > 0)
        {
            mi.addRootMenu(MENU_RENEW_ROOMS, string_id.unlocalized("Renew My Rooms"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id building = hasObjVar(self, apartment_lib.OV_TERMINAL_BUILDING) ? getObjIdObjVar(self, apartment_lib.OV_TERMINAL_BUILDING) : obj_id.NULL_ID;
        if (!isIdValid(building) || !exists(building))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Linked building not found.");
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_PURCHASE_ROOM)
        {
            dictionary d = new dictionary();
            d.put("player", player);
            messageTo(building, "apartmentOpenRentListForPlayer", d, 0.0f, false);
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_RENEW_ROOMS)
        {
            dictionary d = new dictionary();
            d.put("player", player);
            messageTo(building, "apartmentRenewRoomsForPlayer", d, 0.0f, false);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
}
