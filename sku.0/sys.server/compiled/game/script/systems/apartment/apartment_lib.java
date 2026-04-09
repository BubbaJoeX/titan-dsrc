package script.systems.apartment;

import script.*;
import script.library.money;
import script.library.player_structure;
import script.library.utils;

import java.util.Vector;

public class apartment_lib extends script.base_script
{
    public apartment_lib()
    {
    }

    public static final String APARTMENT_ROOT = "apartment";
    public static final String OV_ENABLED = APARTMENT_ROOT + ".enabled";
    public static final String OV_BASE_RENT = APARTMENT_ROOT + ".baseRentPerDay";
    public static final String OV_TAX_PCT = APARTMENT_ROOT + ".taxPct";
    public static final String OV_PREPAY_DAYS = APARTMENT_ROOT + ".prepayDays";
    public static final String OV_MAX_ROOMS = APARTMENT_ROOT + ".maxRoomsPerPlayer";
    public static final String OV_TENANTS_ROOT = APARTMENT_ROOT + ".tenants";
    public static final String OV_UNITS_ROOT = APARTMENT_ROOT + ".units";
    public static final String OV_HEARTBEAT = APARTMENT_ROOT + ".heartbeatSeconds";
    public static final String OV_PURCHASE_TERMINAL = APARTMENT_ROOT + ".purchaseTerminal";
    public static final String OV_TERMINAL_MARKER = APARTMENT_ROOT + ".terminal";
    public static final String OV_TERMINAL_BUILDING = APARTMENT_ROOT + ".terminal.building";
    public static final String OV_TERMINAL_CELL = APARTMENT_ROOT + ".terminal.cell";

    public static final String UNIT_STATUS_VACANT = "vacant";
    public static final String UNIT_STATUS_OCCUPIED = "occupied";
    public static final String UNIT_STATUS_PUBLIC = "public";

    public static final int DEFAULT_BASE_RENT = 5000;
    public static final float DEFAULT_TAX_PCT = 0.15f;
    public static final int DEFAULT_PREPAY_DAYS = 15;
    public static final int DEFAULT_MAX_ROOMS = 2;
    public static final int DEFAULT_HEARTBEAT_SECONDS = 3600;
    public static final int DAY_SECONDS = 86400;
    public static final String TERMINAL_TEMPLATE = "object/tangible/terminal/terminal_command_console.iff";

    public static boolean isApartmentBuilding(obj_id building) throws InterruptedException
    {
        return isIdValid(building) && hasObjVar(building, OV_ENABLED) && getIntObjVar(building, OV_ENABLED) == 1;
    }

    public static void ensureDefaults(obj_id building) throws InterruptedException
    {
        if (!hasObjVar(building, OV_BASE_RENT))
        {
            setObjVar(building, OV_BASE_RENT, DEFAULT_BASE_RENT);
        }
        if (!hasObjVar(building, OV_TAX_PCT))
        {
            setObjVar(building, OV_TAX_PCT, DEFAULT_TAX_PCT);
        }
        if (!hasObjVar(building, OV_PREPAY_DAYS))
        {
            setObjVar(building, OV_PREPAY_DAYS, DEFAULT_PREPAY_DAYS);
        }
        if (!hasObjVar(building, OV_MAX_ROOMS))
        {
            setObjVar(building, OV_MAX_ROOMS, DEFAULT_MAX_ROOMS);
        }
        if (!hasObjVar(building, OV_HEARTBEAT))
        {
            setObjVar(building, OV_HEARTBEAT, DEFAULT_HEARTBEAT_SECONDS);
        }
    }

    public static void initializeApartment(obj_id building, obj_id actor) throws InterruptedException
    {
        if (!isIdValid(building) || !player_structure.isBuilding(building))
        {
            return;
        }

        ensureDefaults(building);
        setObjVar(building, OV_ENABLED, 1);

        String[] cells = getCellNames(building);
        if (cells == null)
        {
            return;
        }

        for (int i = 0; i < cells.length; ++i)
        {
            String cellName = cells[i];
            ensureUnitInitialized(building, cellName);
        }

        ensurePurchaseTerminal(building);

        if (isIdValid(actor))
        {
            sendSystemMessageTestingOnly(actor, "[Apartment] Apartment mode enabled.");
        }
    }

    public static String getUnitPath(String cellName)
    {
        return OV_UNITS_ROOT + "." + cellName;
    }

    public static String getTenantPath(String playerKey)
    {
        return OV_TENANTS_ROOT + "." + playerKey + ".rooms";
    }

    public static String getPlayerKey(obj_id player)
    {
        return player.toString();
    }

    public static int getHeartbeatSeconds(obj_id building) throws InterruptedException
    {
        int value = hasObjVar(building, OV_HEARTBEAT) ? getIntObjVar(building, OV_HEARTBEAT) : DEFAULT_HEARTBEAT_SECONDS;
        if (value < 60)
        {
            value = 60;
        }
        return value;
    }

    public static int getPrepayDays(obj_id building) throws InterruptedException
    {
        int days = hasObjVar(building, OV_PREPAY_DAYS) ? getIntObjVar(building, OV_PREPAY_DAYS) : DEFAULT_PREPAY_DAYS;
        if (days < 1)
        {
            days = DEFAULT_PREPAY_DAYS;
        }
        return days;
    }

    public static int getRoomCap(obj_id building) throws InterruptedException
    {
        int cap = hasObjVar(building, OV_MAX_ROOMS) ? getIntObjVar(building, OV_MAX_ROOMS) : DEFAULT_MAX_ROOMS;
        if (cap < 1)
        {
            cap = DEFAULT_MAX_ROOMS;
        }
        return cap;
    }

    public static int getBaseRentPerDay(obj_id building) throws InterruptedException
    {
        int rent = hasObjVar(building, OV_BASE_RENT) ? getIntObjVar(building, OV_BASE_RENT) : DEFAULT_BASE_RENT;
        if (rent < 0)
        {
            rent = DEFAULT_BASE_RENT;
        }
        return rent;
    }

    public static float getTaxPct(obj_id building) throws InterruptedException
    {
        float tax = hasObjVar(building, OV_TAX_PCT) ? getFloatObjVar(building, OV_TAX_PCT) : DEFAULT_TAX_PCT;
        if (tax < 0.0f)
        {
            tax = DEFAULT_TAX_PCT;
        }
        return tax;
    }

    public static int calculateCycleCharge(obj_id building) throws InterruptedException
    {
        int base = getBaseRentPerDay(building) * getPrepayDays(building);
        float taxPct = getTaxPct(building);
        int tax = Math.round((float)base * taxPct);
        return base + tax;
    }

    public static int getCycleSeconds(obj_id building) throws InterruptedException
    {
        return getPrepayDays(building) * DAY_SECONDS;
    }

    public static String getUnitStatus(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".status";
        if (!hasObjVar(building, path))
        {
            return UNIT_STATUS_VACANT;
        }
        String status = getStringObjVar(building, path);
        if (status == null || status.length() < 1)
        {
            return UNIT_STATUS_VACANT;
        }
        return status;
    }

    public static boolean isUnitRentable(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".rentable";
        if (!hasObjVar(building, path))
        {
            return true;
        }
        return getIntObjVar(building, path) == 1;
    }

    public static void setUnitRentable(obj_id building, String cellName, boolean rentable) throws InterruptedException
    {
        setObjVar(building, getUnitPath(cellName) + ".rentable", rentable ? 1 : 0);
    }

    public static void setUnitStatus(obj_id building, String cellName, String status) throws InterruptedException
    {
        setObjVar(building, getUnitPath(cellName) + ".status", status);
    }

    public static obj_id getUnitTenant(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".tenant";
        if (!hasObjVar(building, path))
        {
            return obj_id.NULL_ID;
        }
        return getObjIdObjVar(building, path);
    }

    public static String getUnitTenantName(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".tenantName";
        if (!hasObjVar(building, path))
        {
            return "";
        }
        String n = getStringObjVar(building, path);
        return n == null ? "" : n;
    }

    public static int getUnitNextDue(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".nextDue";
        return hasObjVar(building, path) ? getIntObjVar(building, path) : 0;
    }

    public static void setUnitNextDue(obj_id building, String cellName, int nextDue) throws InterruptedException
    {
        setObjVar(building, getUnitPath(cellName) + ".nextDue", nextDue);
        setObjVar(building, getUnitPath(cellName) + ".lastBill", getGameTime());
    }

    public static void clearUnitTenant(obj_id building, String cellName) throws InterruptedException
    {
        String unitPath = getUnitPath(cellName);
        removeObjVar(building, unitPath + ".tenant");
        removeObjVar(building, unitPath + ".tenantName");
        removeObjVar(building, unitPath + ".nextDue");
        removeObjVar(building, unitPath + ".lastBill");
    }

    public static void ensureUnitInitialized(obj_id building, String cellName) throws InterruptedException
    {
        String unitPath = getUnitPath(cellName);
        if (!hasObjVar(building, unitPath + ".status"))
        {
            setObjVar(building, unitPath + ".status", UNIT_STATUS_VACANT);
        }
        if (!hasObjVar(building, unitPath + ".rentable"))
        {
            setObjVar(building, unitPath + ".rentable", 1);
        }
        cleanupLegacyUnitMarker(building, cellName);
        applyUnitPermissions(building, cellName);
        refreshUnitLabel(building, cellName);

        String status = getUnitStatus(building, cellName);
        if (UNIT_STATUS_OCCUPIED.equals(status))
        {
            obj_id tenant = getUnitTenant(building, cellName);
            if (isIdValid(tenant))
            {
                ensureManageTerminal(building, cellName, tenant);
            }
            else
            {
                destroyManageTerminal(building, cellName);
            }
        }
        else
        {
            destroyManageTerminal(building, cellName);
        }
    }

    public static void ensurePurchaseTerminal(obj_id building) throws InterruptedException
    {
        if (!isIdValid(building))
        {
            return;
        }

        if (hasObjVar(building, OV_PURCHASE_TERMINAL))
        {
            obj_id existing = getObjIdObjVar(building, OV_PURCHASE_TERMINAL);
            if (isIdValid(existing) && exists(existing))
            {
                if (!hasScript(existing, "terminal.apartment_purchase_terminal"))
                {
                    attachScript(existing, "terminal.apartment_purchase_terminal");
                }
                setObjVar(existing, OV_TERMINAL_MARKER, 1);
                setObjVar(existing, OV_TERMINAL_BUILDING, building);
                setName(existing, "Apartment Rental Terminal");
                return;
            }
        }

        location loc = getLocation(building);
        if (loc == null)
        {
            return;
        }
        obj_id terminal = createObject(TERMINAL_TEMPLATE, loc);
        if (!isIdValid(terminal))
        {
            return;
        }
        attachScript(terminal, "terminal.apartment_purchase_terminal");
        setObjVar(terminal, OV_TERMINAL_MARKER, 1);
        setObjVar(terminal, OV_TERMINAL_BUILDING, building);
        setName(terminal, "Apartment Rental Terminal");
        setObjVar(building, OV_PURCHASE_TERMINAL, terminal);
    }

    public static obj_id getManageTerminal(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".manageTerminal";
        if (!hasObjVar(building, path))
        {
            return obj_id.NULL_ID;
        }
        return getObjIdObjVar(building, path);
    }

    public static void destroyManageTerminal(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".manageTerminal";
        if (!hasObjVar(building, path))
        {
            return;
        }
        obj_id terminal = getObjIdObjVar(building, path);
        if (isIdValid(terminal) && exists(terminal))
        {
            destroyObject(terminal);
        }
        removeObjVar(building, path);
    }

    public static void ensureManageTerminal(obj_id building, String cellName, obj_id tenant) throws InterruptedException
    {
        if (!isIdValid(building) || !isIdValid(tenant))
        {
            return;
        }
        if (!UNIT_STATUS_OCCUPIED.equals(getUnitStatus(building, cellName)))
        {
            return;
        }

        obj_id existing = getManageTerminal(building, cellName);
        if (isIdValid(existing) && exists(existing))
        {
            if (!hasScript(existing, "terminal.apartment_room_terminal"))
            {
                attachScript(existing, "terminal.apartment_room_terminal");
            }
            setObjVar(existing, OV_TERMINAL_MARKER, 1);
            setObjVar(existing, OV_TERMINAL_BUILDING, building);
            setObjVar(existing, OV_TERMINAL_CELL, cellName);
            setName(existing, "Apartment Room Terminal");
            return;
        }

        location roomLoc = getGoodLocation(building, cellName);
        obj_id terminal = obj_id.NULL_ID;
        if (roomLoc != null)
        {
            terminal = createObjectInCell(TERMINAL_TEMPLATE, building, cellName, roomLoc);
        }
        if (!isIdValid(terminal))
        {
            terminal = createObjectInCell(TERMINAL_TEMPLATE, building, cellName);
        }
        if (!isIdValid(terminal))
        {
            return;
        }

        attachScript(terminal, "terminal.apartment_room_terminal");
        setObjVar(terminal, OV_TERMINAL_MARKER, 1);
        setObjVar(terminal, OV_TERMINAL_BUILDING, building);
        setObjVar(terminal, OV_TERMINAL_CELL, cellName);
        setName(terminal, "Apartment Room Terminal");
        setObjVar(building, getUnitPath(cellName) + ".manageTerminal", terminal);
    }

    public static void cleanupLegacyUnitMarker(obj_id building, String cellName) throws InterruptedException
    {
        String path = getUnitPath(cellName) + ".forcefield";
        if (hasObjVar(building, path))
        {
            obj_id marker = getObjIdObjVar(building, path);
            if (isIdValid(marker) && exists(marker))
            {
                destroyObject(marker);
            }
            removeObjVar(building, path);
        }
        removeObjVar(building, getUnitPath(cellName) + ".labelObject");
        removeObjVar(building, getUnitPath(cellName) + ".forcefieldColor");
    }

    public static String computeUnitLabel(obj_id building, String cellName) throws InterruptedException
    {
        String status = getUnitStatus(building, cellName);
        if (UNIT_STATUS_PUBLIC.equals(status))
        {
            return "PUBLIC";
        }
        if (UNIT_STATUS_OCCUPIED.equals(status))
        {
            String n = getUnitTenantName(building, cellName);
            if (n == null || n.length() < 1)
            {
                obj_id t = getUnitTenant(building, cellName);
                if (isIdValid(t))
                {
                    n = getPlayerName(t);
                }
            }
            if (n == null || n.length() < 1)
            {
                n = "Unknown";
            }
            return "Owner: " + n;
        }
        if (!isUnitRentable(building, cellName))
        {
            return "";
        }
        return "VACANT";
    }

    public static void refreshUnitLabel(obj_id building, String cellName) throws InterruptedException
    {
        String label = computeUnitLabel(building, cellName);
        setObjVar(building, getUnitPath(cellName) + ".labelText", label);

        obj_id cell = getCellId(building, cellName);
        if (!isIdValid(cell))
        {
            return;
        }
        // Portal door forcefield barriers render from portal style; this sets the
        // in-cell CUI label text shown at the portal boundary for room state.
        setCellLabel(cell, label);
        setCellLabelOffset(cell, 0.0f, 1.6f, 0.0f);
    }

    public static void applyUnitPermissions(obj_id building, String cellName) throws InterruptedException
    {
        obj_id cell = getCellId(building, cellName);
        if (!isIdValid(cell))
        {
            return;
        }

        permissionsRemoveAllAllowed(cell);
        permissionsRemoveAllBanned(cell);

        // Non-rentable rooms are intentionally open/public.
        if (!isUnitRentable(building, cellName))
        {
            permissionsMakePublic(cell);
            obj_id owner = getOwner(building);
            if (isIdValid(owner))
            {
                sendDirtyCellPermissionsUpdate(cell, owner, true);
            }
            obj_id tenant = getUnitTenant(building, cellName);
            if (isIdValid(tenant))
            {
                sendDirtyCellPermissionsUpdate(cell, tenant, true);
            }
            return;
        }

        String status = getUnitStatus(building, cellName);
        if (UNIT_STATUS_PUBLIC.equals(status))
        {
            permissionsMakePublic(cell);
            obj_id owner = getOwner(building);
            if (isIdValid(owner))
            {
                sendDirtyCellPermissionsUpdate(cell, owner, true);
            }
            return;
        }

        permissionsMakePrivate(cell);

        obj_id owner = getOwner(building);
        if (isIdValid(owner))
        {
            permissionsAddAllowed(cell, getPlayerName(owner));
        }

        obj_id tenant = getUnitTenant(building, cellName);
        if (isIdValid(tenant))
        {
            permissionsAddAllowed(cell, getPlayerName(tenant));
        }

        if (isIdValid(owner))
        {
            sendDirtyCellPermissionsUpdate(cell, owner, true);
        }
        if (isIdValid(tenant))
        {
            sendDirtyCellPermissionsUpdate(cell, tenant, true);
        }
    }

    public static int countRoomsForPlayer(obj_id building, obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return 0;
        }
        String[] cells = getCellNames(building);
        if (cells == null)
        {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < cells.length; ++i)
        {
            String cellName = cells[i];
            if (UNIT_STATUS_OCCUPIED.equals(getUnitStatus(building, cellName)))
            {
                obj_id tenant = getUnitTenant(building, cellName);
                if (isIdValid(tenant) && tenant == player)
                {
                    ++count;
                }
            }
        }
        return count;
    }

    public static String[] getRentableCells(obj_id building) throws InterruptedException
    {
        String[] cells = getCellNames(building);
        if (cells == null)
        {
            return new String[0];
        }
        Vector out = new Vector();
        for (int i = 0; i < cells.length; ++i)
        {
            String c = cells[i];
            if (UNIT_STATUS_VACANT.equals(getUnitStatus(building, c)) && isUnitRentable(building, c))
            {
                out = utils.addElement(out, c);
            }
        }
        String[] values = new String[out.size()];
        out.toArray(values);
        return values;
    }

    public static boolean chargeRent(obj_id player, obj_id building) throws InterruptedException
    {
        int amount = calculateCycleCharge(building);
        if (amount <= 0)
        {
            return false;
        }
        if (getTotalMoney(player) < amount)
        {
            return false;
        }
        return money.pay(player, money.ACCT_STRUCTURE_MAINTENANCE, amount, "", null, false);
    }

    public static boolean rentUnit(obj_id building, obj_id player, String cellName) throws InterruptedException
    {
        if (!isApartmentBuilding(building))
        {
            return false;
        }
        if (!isIdValid(player) || !isPlayer(player))
        {
            return false;
        }
        if (!UNIT_STATUS_VACANT.equals(getUnitStatus(building, cellName)))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] That room is no longer vacant.");
            return false;
        }
        if (!isUnitRentable(building, cellName))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] That room is not currently available for rent.");
            return false;
        }
        if (countRoomsForPlayer(building, player) >= getRoomCap(building))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Room limit reached for this building.");
            return false;
        }

        int amount = calculateCycleCharge(building);
        if (getTotalMoney(player) < amount)
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Insufficient funds (" + amount + " required).");
            return false;
        }
        if (!chargeRent(player, building))
        {
            sendSystemMessageTestingOnly(player, "[Apartment] Payment failed.");
            return false;
        }

        assignTenant(building, cellName, player);
        ensureManageTerminal(building, cellName, player);
        sendSystemMessageTestingOnly(player, "[Apartment] Room rented. Next due in " + getPrepayDays(building) + " days.");
        return true;
    }

    public static void assignTenant(obj_id building, String cellName, obj_id tenant) throws InterruptedException
    {
        String unitPath = getUnitPath(cellName);
        setObjVar(building, unitPath + ".tenant", tenant);
        setObjVar(building, unitPath + ".tenantName", getPlayerName(tenant));
        setObjVar(building, unitPath + ".status", UNIT_STATUS_OCCUPIED);
        setUnitNextDue(building, cellName, getGameTime() + getCycleSeconds(building));

        updateTenantIndex(building, tenant);
        applyUnitPermissions(building, cellName);
        refreshUnitLabel(building, cellName);
    }

    public static void setUnitPublic(obj_id building, String cellName, boolean value) throws InterruptedException
    {
        if (value)
        {
            if (UNIT_STATUS_OCCUPIED.equals(getUnitStatus(building, cellName)))
            {
                return;
            }
            setUnitStatus(building, cellName, UNIT_STATUS_PUBLIC);
            clearUnitTenant(building, cellName);
        }
        else
        {
            setUnitStatus(building, cellName, UNIT_STATUS_VACANT);
        }

        applyUnitPermissions(building, cellName);
        refreshUnitLabel(building, cellName);
    }

    public static void evictUnit(obj_id building, String cellName, String reason) throws InterruptedException
    {
        if (!UNIT_STATUS_OCCUPIED.equals(getUnitStatus(building, cellName)))
        {
            return;
        }

        obj_id tenant = getUnitTenant(building, cellName);
        recoverRoomContents(building, cellName, tenant);
        destroyManageTerminal(building, cellName);
        clearUnitTenant(building, cellName);
        setUnitStatus(building, cellName, UNIT_STATUS_VACANT);
        applyUnitPermissions(building, cellName);
        refreshUnitLabel(building, cellName);

        if (isIdValid(tenant))
        {
            String msg = "[Apartment] Your rental room was released";
            if (reason != null && reason.length() > 0)
            {
                msg += " (" + reason + ")";
            }
            msg += ". Room contents were recovered to your datapad.";
            sendSystemMessage(tenant, msg, null);
            updateTenantIndex(building, tenant);
        }
    }

    public static void recoverRoomContents(obj_id building, String cellName, obj_id tenant) throws InterruptedException
    {
        if (!isIdValid(tenant))
        {
            return;
        }
        obj_id cell = getCellId(building, cellName);
        if (!isIdValid(cell))
        {
            return;
        }
        obj_id[] contents = getContents(cell);
        if (contents == null)
        {
            return;
        }

        for (int i = 0; i < contents.length; ++i)
        {
            obj_id content = contents[i];
            if (!isIdValid(content))
            {
                continue;
            }
            if (isPlayer(content) || isMob(content))
            {
                continue;
            }
            if (hasObjVar(content, OV_TERMINAL_MARKER))
            {
                continue;
            }

            moveToOfflinePlayerDatapadAndUnload(content, tenant, 10);
            fixLoadWith(content, tenant, 9);
        }
    }

    public static String getPlayerCurrentCellName(obj_id building, obj_id player) throws InterruptedException
    {
        if (!isIdValid(building) || !isIdValid(player))
        {
            return null;
        }
        location loc = getLocation(player);
        if (loc == null || !isIdValid(loc.cell))
        {
            return null;
        }

        String[] cells = getCellNames(building);
        if (cells == null)
        {
            return null;
        }
        for (int i = 0; i < cells.length; ++i)
        {
            String c = cells[i];
            obj_id cell = getCellId(building, c);
            if (isIdValid(cell) && cell == loc.cell)
            {
                return c;
            }
        }
        return null;
    }

    public static void updateTenantIndex(obj_id building, obj_id tenant) throws InterruptedException
    {
        if (!isIdValid(tenant))
        {
            return;
        }
        String[] cells = getCellNames(building);
        if (cells == null)
        {
            return;
        }
        String key = getPlayerKey(tenant);
        Vector rooms = new Vector();
        for (int i = 0; i < cells.length; ++i)
        {
            String c = cells[i];
            if (UNIT_STATUS_OCCUPIED.equals(getUnitStatus(building, c)))
            {
                obj_id unitTenant = getUnitTenant(building, c);
                if (isIdValid(unitTenant) && unitTenant == tenant)
                {
                    rooms = utils.addElement(rooms, c);
                }
            }
        }

        String tenantPath = getTenantPath(key);
        if (rooms.size() == 0)
        {
            removeObjVar(building, tenantPath);
            return;
        }
        String[] roomArray = new String[rooms.size()];
        rooms.toArray(roomArray);
        setObjVar(building, tenantPath, roomArray);
    }

    public static boolean renewSingleRoom(obj_id building, obj_id player, String cellName) throws InterruptedException
    {
        if (!isIdValid(building) || !isIdValid(player))
        {
            return false;
        }
        if (!UNIT_STATUS_OCCUPIED.equals(getUnitStatus(building, cellName)))
        {
            return false;
        }
        obj_id tenant = getUnitTenant(building, cellName);
        if (!isIdValid(tenant) || tenant != player)
        {
            return false;
        }
        if (!chargeRent(player, building))
        {
            return false;
        }
        int nextDue = getUnitNextDue(building, cellName);
        if (nextDue < getGameTime())
        {
            nextDue = getGameTime();
        }
        setUnitNextDue(building, cellName, nextDue + getCycleSeconds(building));
        return true;
    }
}
