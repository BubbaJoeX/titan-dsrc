package script.structure;

import script.*;
import script.library.sui;
import script.library.utils;
import script.systems.apartment.apartment_lib;

public class apartment_building extends script.base_script
{
    public apartment_building()
    {
    }

    public static final int MENU_ROOT_APARTMENT = menu_info_types.SERVER_MENU41;
    public static final int MENU_RENT_ROOM = menu_info_types.SERVER_MENU42;
    public static final int MENU_RENEW_MY_ROOMS = menu_info_types.SERVER_MENU43;
    public static final int MENU_ADMIN_INIT = menu_info_types.SERVER_MENU44;
    public static final int MENU_ADMIN_TOGGLE_PUBLIC = menu_info_types.SERVER_MENU45;
    public static final int MENU_ADMIN_EVICT_CURRENT = menu_info_types.SERVER_MENU46;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "apartmentInitializeBuilding", null, 0.1f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if (apartment_lib.isApartmentBuilding(self))
        {
            apartment_lib.initializeApartment(self, obj_id.NULL_ID);
            apartment_lib.ensurePurchaseTerminal(self);
            scheduleHeartbeat(self);
        }
        return SCRIPT_CONTINUE;
    }

    public int apartmentInitializeBuilding(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id actor = getOwner(self);
        if (params != null && params.containsKey("actor"))
        {
            actor = params.getObjId("actor");
        }

        apartment_lib.initializeApartment(self, actor);
        apartment_lib.ensurePurchaseTerminal(self);
        scheduleHeartbeat(self);
        return SCRIPT_CONTINUE;
    }

    public int apartmentOpenAdminPanelRequest(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params != null && params.containsKey("player") ? params.getObjId("player") : obj_id.NULL_ID;
        if (isIdValid(player))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Use radial menu > Apartment.");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!apartment_lib.isApartmentBuilding(self))
        {
            return SCRIPT_CONTINUE;
        }

        if (player == getOwner(self) || isGod(player))
        {
            int root = mi.addRootMenu(MENU_ROOT_APARTMENT, string_id.unlocalized("Apartment (GM/Admin)"));
            mi.addSubMenu(root, MENU_ADMIN_INIT, string_id.unlocalized("Rebuild Apartment Index"));

            String currentCell = apartment_lib.getPlayerCurrentCellName(self, player);
            if (currentCell != null)
            {
                String status = apartment_lib.getUnitStatus(self, currentCell);
                if (!apartment_lib.UNIT_STATUS_OCCUPIED.equals(status))
                {
                    String publicText = apartment_lib.UNIT_STATUS_PUBLIC.equals(status) ? "Set Room Private" : "Set Room Public";
                    mi.addSubMenu(root, MENU_ADMIN_TOGGLE_PUBLIC, string_id.unlocalized(publicText));
                }
                if (apartment_lib.UNIT_STATUS_OCCUPIED.equals(status))
                {
                    mi.addSubMenu(root, MENU_ADMIN_EVICT_CURRENT, string_id.unlocalized("Evict Current Room Tenant"));
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!apartment_lib.isApartmentBuilding(self))
        {
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_ADMIN_INIT && (player == getOwner(self) || isGod(player)))
        {
            apartment_lib.initializeApartment(self, player);
            sendSystemMessageTestingOnly(player, "[Apartment] Reconciled apartment units.");
            scheduleHeartbeat(self);
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_ADMIN_TOGGLE_PUBLIC && (player == getOwner(self) || isGod(player)))
        {
            toggleCurrentCellPublic(self, player);
            return SCRIPT_CONTINUE;
        }
        if (item == MENU_ADMIN_EVICT_CURRENT && (player == getOwner(self) || isGod(player)))
        {
            evictCurrentCell(self, player);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int apartmentOpenRentListForPlayer(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || !params.containsKey("player"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = params.getObjId("player");
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        openRentList(self, player);
        return SCRIPT_CONTINUE;
    }

    public int apartmentRenewRoomsForPlayer(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || !params.containsKey("player"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = params.getObjId("player");
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        renewPlayerRooms(self, player);
        return SCRIPT_CONTINUE;
    }

    public void openRentList(obj_id self, obj_id player) throws InterruptedException
    {
        String[] cells = apartment_lib.getRentableCells(self);
        if (cells == null || cells.length < 1)
        {
            sendSystemMessageTestingOnly(player, "[Apartment] No vacant rooms are currently available.");
            return;
        }
        utils.setScriptVar(player, "apartment.rent.building", self);
        utils.setScriptVar(player, "apartment.rent.cells", cells);
        sui.listbox(self, player, "Choose a vacant room to rent (15-day prepay + 15% tax).", sui.OK_CANCEL, "Room Rental", cells, "handleApartmentRentSelect", true, false);
    }

    public int handleApartmentRentSelect(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.getInt("buttonPressed") != sui.BP_OK)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(player, "apartment.rent.building") || !utils.hasScriptVar(player, "apartment.rent.cells"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id building = utils.getObjIdScriptVar(player, "apartment.rent.building");
        String[] cells = utils.getStringArrayScriptVar(player, "apartment.rent.cells");
        utils.removeScriptVar(player, "apartment.rent.building");
        utils.removeScriptVar(player, "apartment.rent.cells");

        if (building != self || cells == null || cells.length < 1)
        {
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0 || idx >= cells.length)
        {
            return SCRIPT_CONTINUE;
        }

        String selectedCell = cells[idx];
        apartment_lib.ensureUnitInitialized(self, selectedCell);
        apartment_lib.rentUnit(self, player, selectedCell);
        return SCRIPT_CONTINUE;
    }

    public void renewPlayerRooms(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return;
        }
        String[] cells = getCellNames(self);
        if (cells == null)
        {
            return;
        }

        int renewed = 0;
        for (int i = 0; i < cells.length; ++i)
        {
            String cellName = cells[i];
            if (!apartment_lib.UNIT_STATUS_OCCUPIED.equals(apartment_lib.getUnitStatus(self, cellName)))
            {
                continue;
            }
            obj_id tenant = apartment_lib.getUnitTenant(self, cellName);
            if (!isIdValid(tenant) || tenant != player)
            {
                continue;
            }

            if (apartment_lib.chargeRent(player, self))
            {
                int nextDue = apartment_lib.getUnitNextDue(self, cellName);
                if (nextDue < getGameTime())
                {
                    nextDue = getGameTime();
                }
                apartment_lib.setUnitNextDue(self, cellName, nextDue + apartment_lib.getCycleSeconds(self));
                ++renewed;
            }
            else
            {
                sendSystemMessageTestingOnly(player, "[Apartment] Renewal failed for " + cellName + " (insufficient funds).");
            }
        }

        if (renewed > 0)
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Renewed " + renewed + " room(s).");
        }
    }

    public void toggleCurrentCellPublic(obj_id self, obj_id player) throws InterruptedException
    {
        String cellName = apartment_lib.getPlayerCurrentCellName(self, player);
        if (cellName == null)
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Stand in a valid room cell to toggle public/private.");
            return;
        }
        String status = apartment_lib.getUnitStatus(self, cellName);
        if (apartment_lib.UNIT_STATUS_OCCUPIED.equals(status))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Occupied rooms cannot be toggled public/private.");
            return;
        }
        boolean toPublic = !apartment_lib.UNIT_STATUS_PUBLIC.equals(status);
        apartment_lib.setUnitPublic(self, cellName, toPublic);
        sendSystemMessageTestingOnly(player, "[Apartment] " + cellName + " is now " + (toPublic ? "PUBLIC" : "PRIVATE") + ".");
    }

    public void evictCurrentCell(obj_id self, obj_id player) throws InterruptedException
    {
        String cellName = apartment_lib.getPlayerCurrentCellName(self, player);
        if (cellName == null)
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Stand in an occupied room to evict.");
            return;
        }
        if (!apartment_lib.UNIT_STATUS_OCCUPIED.equals(apartment_lib.getUnitStatus(self, cellName)))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Current room is not occupied.");
            return;
        }
        apartment_lib.evictUnit(self, cellName, "admin eviction");
        sendSystemMessageTestingOnly(player, "[Apartment] Tenant evicted from " + cellName + ".");
    }

    public void scheduleHeartbeat(obj_id self) throws InterruptedException
    {
        int seconds = apartment_lib.getHeartbeatSeconds(self);
        messageTo(self, "apartmentHeartbeat", null, (float)seconds, false);
    }

    public int apartmentHeartbeat(obj_id self, dictionary params) throws InterruptedException
    {
        if (!apartment_lib.isApartmentBuilding(self))
        {
            return SCRIPT_CONTINUE;
        }

        apartment_lib.ensurePurchaseTerminal(self);

        String[] cells = getCellNames(self);
        if (cells != null)
        {
            int now = getGameTime();
            int cycleCharge = apartment_lib.calculateCycleCharge(self);
            int cycleSeconds = apartment_lib.getCycleSeconds(self);

            for (int i = 0; i < cells.length; ++i)
            {
                String cellName = cells[i];
                if (!apartment_lib.UNIT_STATUS_OCCUPIED.equals(apartment_lib.getUnitStatus(self, cellName)))
                {
                    continue;
                }
                int due = apartment_lib.getUnitNextDue(self, cellName);
                if (due > now)
                {
                    continue;
                }

                obj_id tenant = apartment_lib.getUnitTenant(self, cellName);
                if (!isIdValid(tenant))
                {
                    apartment_lib.evictUnit(self, cellName, "invalid tenant");
                    continue;
                }

                apartment_lib.ensureManageTerminal(self, cellName, tenant);

                dictionary pay = new dictionary();
                pay.put("amount", cycleCharge);
                pay.put("offlineAmount", cycleCharge);
                pay.put("account", script.library.money.ACCT_STRUCTURE_MAINTENANCE);
                pay.put("successCallback", "apartmentRentPaymentSucceeded");
                pay.put("failCallback", "apartmentRentPaymentFailed");
                pay.put("replyTo", self);
                pay.put("owner", tenant);
                pay.put("unitCell", cellName);
                pay.put("cycleSeconds", cycleSeconds);

                messageTo(tenant, "transferMoneyToNamedAccount", pay, 0.0f, false, self, "ownerNotOnline");
            }
        }

        scheduleHeartbeat(self);
        return SCRIPT_CONTINUE;
    }

    public int apartmentRentPaymentSucceeded(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || !params.containsKey("unitCell"))
        {
            return SCRIPT_CONTINUE;
        }
        String cellName = params.getString("unitCell");
        obj_id tenant = params.containsKey("owner") ? params.getObjId("owner") : apartment_lib.getUnitTenant(self, cellName);
        if (!isIdValid(tenant) || tenant != apartment_lib.getUnitTenant(self, cellName))
        {
            return SCRIPT_CONTINUE;
        }

        int cycleSeconds = params.containsKey("cycleSeconds") ? params.getInt("cycleSeconds") : apartment_lib.getCycleSeconds(self);
        int due = apartment_lib.getUnitNextDue(self, cellName);
        if (due < getGameTime())
        {
            due = getGameTime();
        }
        apartment_lib.setUnitNextDue(self, cellName, due + cycleSeconds);
        sendSystemMessage(tenant, "[Apartment] Rent payment received for " + cellName + ".", null);
        return SCRIPT_CONTINUE;
    }

    public int apartmentRentPaymentFailed(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || !params.containsKey("unitCell"))
        {
            return SCRIPT_CONTINUE;
        }
        String cellName = params.getString("unitCell");
        apartment_lib.evictUnit(self, cellName, "delinquent rent");
        return SCRIPT_CONTINUE;
    }
}
