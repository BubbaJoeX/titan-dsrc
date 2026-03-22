package script.terminal;

import script.*;
import script.library.*;
import script.space.atmo.atmo_landing_docked;
import script.space.atmo.atmo_landing_manager;
import script.space.atmo.atmo_landing_registry;

public class terminal_pob_ship extends script.base_script
{
    public terminal_pob_ship()
    {
    }

    // Menu IDs - organized to prevent collisions
    private static final int MENU_STRUCTURE_PERMISSIONS = menu_info_types.SERVER_MENU1;
    private static final int MENU_MOVE_FIRST_ITEM = menu_info_types.SERVER_MENU2;
    private static final int MENU_DELETE_ALL_ITEMS = menu_info_types.SERVER_MENU3;
    private static final int MENU_BOARDING_PERMISSIONS = menu_info_types.SERVER_MENU4;
    private static final int MENU_DOCKING_ROOT = menu_info_types.SERVER_MENU5;
    private static final int MENU_CHECK_DOCKING_TIME = menu_info_types.SERVER_MENU6;
    private static final int MENU_EXTEND_DOCKING = menu_info_types.SERVER_MENU7;
    private static final int MENU_MANAGE_ACCESS = menu_info_types.SERVER_MENU8;
    private static final int MENU_STORAGE_REDEED = menu_info_types.SERVER_MENU9;
    private static final int MENU_ITEM_MANAGEMENT = menu_info_types.SERVER_MENU10;
    private static final int MENU_MOORAGE_INFO = menu_info_types.SERVER_MENU14;
    private static final int MENU_FIND_ALL_ITEMS = menu_info_types.SERVER_MENU11;
    private static final int MENU_SEARCH_ITEMS = menu_info_types.SERVER_MENU12;
    private static final int MENU_UNDOCK = menu_info_types.SERVER_MENU13;

    public static final string_id SID_TERMINAL_PERMISSIONS = new string_id("player_structure", "permissions");
    public static final string_id SID_MOVE_FIRST_ITEM = new string_id("player_structure", "move_first_item");
    public static final string_id SID_MOVED_FIRST_ITEM = new string_id("player_structure", "moved_first_item_pob");
    public static final string_id SID_DELETE_ALL_ITEMS = new string_id("player_structure", "delete_all_items");
    public static final string_id SID_ITEMS_DELETED = new string_id("player_structure", "items_deleted_pob_ship");
    public static final string_id SID_ROOT_ITEM_MENU = new string_id("player_structure", "find_items_root_menu");
    public static final string_id SID_FIND_ALL_HOUSE_ITEMS = new string_id("player_structure", "find_items_find_all_house_items");
    public static final string_id SID_SEARCH_FOR_HOUSE_ITEMS = new string_id("player_structure", "find_items_search_for_house_items");
    public static final string_id SID_TERMINAL_REDEED_STORAGE = new string_id("player_structure", "redeed_storage");
    public static final string_id SID_STORAGE_INCREASE_REDEED_TITLE = new string_id("player_structure", "sui_storage_redeed_title");
    public static final string_id SID_STORAGE_INCREASE_REDEED_PROMPT = new string_id("player_structure", "sui_storage_redeed_prompt");
    public static final string_id SID_BOARDING_PERMISSIONS = string_id.unlocalized("Boarding Permissions");
    public static final string_id SID_DOCKING = string_id.unlocalized("Docking");
    public static final string_id SID_MOORAGE_INFO = string_id.unlocalized("Moorage Status");
    public static final string_id SID_CHECK_DOCKING_TIME = string_id.unlocalized("Check Docking Time");
    public static final string_id SID_EXTEND_DOCKING = string_id.unlocalized("Extend Docking Time");
    public static final string_id SID_UNDOCK = string_id.unlocalized("Undock Ship");
    public static final string_id SID_MANAGE_ACCESS = string_id.unlocalized("Manage Access");
    private static final String BOARDING_PERMISSIONS_PID = "boardingPermissions.pid";

    public static final int EXTEND_COST = 20000;
    public static final int EXTEND_TIME = 300;

    /** Same root as {@code combat_ship.OV_AUTOPILOT_ACTIVE} — server atmospheric autopilot. */
    private static final String OV_SHIP_AUTOPILOT_ACTIVE = "space.autopilot.active";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Starship Management Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setName(self, "Starship Management Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id ship = space_transition.getContainingShip(self);
        if (isIdValid(ship) && getOwner(ship) == player)
        {
            // Item management submenu
            int rootItemMenu = mi.addRootMenu(MENU_ITEM_MANAGEMENT, SID_ROOT_ITEM_MENU);
            mi.addSubMenu(rootItemMenu, MENU_FIND_ALL_ITEMS, SID_FIND_ALL_HOUSE_ITEMS);
            mi.addSubMenu(rootItemMenu, MENU_SEARCH_ITEMS, SID_SEARCH_FOR_HOUSE_ITEMS);
            mi.addSubMenu(rootItemMenu, MENU_MOVE_FIRST_ITEM, SID_MOVE_FIRST_ITEM);
            mi.addSubMenu(rootItemMenu, MENU_DELETE_ALL_ITEMS, SID_DELETE_ALL_ITEMS);

            // Manage Access submenu - combines structure permissions and boarding permissions
            int accessMenu = mi.addRootMenu(MENU_MANAGE_ACCESS, SID_MANAGE_ACCESS);
            mi.addSubMenu(accessMenu, MENU_STRUCTURE_PERMISSIONS, SID_TERMINAL_PERMISSIONS);
            mi.addSubMenu(accessMenu, MENU_BOARDING_PERMISSIONS, SID_BOARDING_PERMISSIONS);

            // Storage redeed (if applicable)
            if (hasObjVar(ship, player_structure.OBJVAR_STRUCTURE_STORAGE_INCREASE))
            {
                mi.addRootMenu(MENU_STORAGE_REDEED, SID_TERMINAL_REDEED_STORAGE);
            }

        }

        // Docking — all passengers; timed-mooring actions when docked, status when not
        if (isIdValid(ship))
        {
            int dockingRoot = mi.addRootMenu(MENU_DOCKING_ROOT, SID_DOCKING);
            if (hasObjVar(ship, "atmo.landing.docked"))
            {
                mi.addSubMenu(dockingRoot, MENU_CHECK_DOCKING_TIME, SID_CHECK_DOCKING_TIME);
                if (hasObjVar(ship, "atmo.landing.dockExpiry") && canOfferExtendDocking(ship))
                {
                    mi.addSubMenu(dockingRoot, MENU_EXTEND_DOCKING, SID_EXTEND_DOCKING);
                }
                mi.addSubMenu(dockingRoot, MENU_UNDOCK, SID_UNDOCK);
            }
            else
            {
                mi.addSubMenu(dockingRoot, MENU_MOORAGE_INFO, SID_MOORAGE_INFO);
            }
        }

        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id ship = space_transition.getContainingShip(self);

        if (isIdValid(ship) && item == MENU_MOORAGE_INFO)
        {
            showMoorageStatus(ship, player);
            return SCRIPT_CONTINUE;
        }

        // Timed mooring / undock — all players aboard when docked at landing point
        if (isIdValid(ship) && hasObjVar(ship, "atmo.landing.docked"))
        {
            if (item == MENU_CHECK_DOCKING_TIME)
            {
                showDockingTimeRemaining(ship, player);
                return SCRIPT_CONTINUE;
            }
            else if (item == MENU_EXTEND_DOCKING)
            {
                showExtendDockingUI(self, ship, player);
                return SCRIPT_CONTINUE;
            }
            else if (item == MENU_UNDOCK)
            {
                showUndockConfirmation(self, ship, player);
                return SCRIPT_CONTINUE;
            }
        }

        if (isIdValid(ship) && getOwner(ship) == player)
        {
            if (item == MENU_BOARDING_PERMISSIONS)
            {
                showBoardingPermissionsMenu(self, player, ship);
                return SCRIPT_CONTINUE;
            }
            if (item == MENU_STRUCTURE_PERMISSIONS)
            {
                queueCommand(player, (1768087594), self, "admin", COMMAND_PRIORITY_DEFAULT);
            }
            else if (item == MENU_MOVE_FIRST_ITEM)
            {
                sui.msgbox(self, player, "@player_structure:move_first_item_d", sui.OK_CANCEL, "@player_structure:move_first_item", sui.MSG_QUESTION, "handleMoveFirstItem");
            }
            else if (item == MENU_DELETE_ALL_ITEMS)
            {
                sui.msgbox(self, player, "@player_structure:delete_all_items_d", sui.OK_CANCEL, "@player_structure:delete_all_items", sui.MSG_QUESTION, "handleDeleteSecondConfirm");
            }
            else if (item == MENU_STORAGE_REDEED)
            {
                if (!hasObjVar(ship, player_structure.OBJVAR_STRUCTURE_STORAGE_INCREASE))
                {
                    return SCRIPT_CONTINUE;
                }
                player_structure.displayAvailableNonGenericStorageTypes(player, self, ship);
            }
            else if (item == MENU_FIND_ALL_ITEMS)
            {
                int lockoutEnds = -1;
                if (hasObjVar(self, "findItems.lockout"))
                {
                    lockoutEnds = getIntObjVar(self, "findItems.lockout");
                }
                int currentTime = getGameTime();
                if (currentTime > lockoutEnds || isGod(player))
                {
                    player_structure.initializeFindAllItemsInHouse(self, player);
                    setObjVar(self, "findItems.lockout", currentTime + player_structure.HOUSE_ITEMS_SEARCH_LOCKOUT);
                }
                else 
                {
                    string_id message = new string_id("player_structure", "find_items_locked_out");
                    prose_package pp = prose.getPackage(message, player, player);
                    prose.setTO(pp, utils.formatTimeVerbose(lockoutEnds - currentTime));
                    sendSystemMessageProse(player, pp);
                }
            }
            else if (item == MENU_SEARCH_ITEMS)
            {
                int lockoutEnds = -1;
                if (hasObjVar(self, "findItems.lockout"))
                {
                    lockoutEnds = getIntObjVar(self, "findItems.lockout");
                }
                int currentTime = getGameTime();
                if (currentTime > lockoutEnds || isGod(player))
                {
                    player_structure.initializeItemSearchInHouse(self, player);
                    setObjVar(self, "findItems.lockout", currentTime + player_structure.HOUSE_ITEMS_SEARCH_LOCKOUT);
                }
                else 
                {
                    string_id message = new string_id("player_structure", "find_items_locked_out");
                    prose_package pp = prose.getPackage(message, player, player);
                    prose.setTO(pp, utils.formatTimeVerbose(lockoutEnds - currentTime));
                    sendSystemMessageProse(player, pp);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int handleStorageRedeedChoice(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String accessFee = sui.getInputBoxText(params);
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id objShip = space_transition.getContainingShip(self);
        if (!isIdValid(objShip) || getOwner(objShip) != player)
        {
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(objShip, player_structure.OBJVAR_STRUCTURE_STORAGE_INCREASE))
        {
            int storageRedeedSelected = 0;
            if (params.containsKey(sui.LISTBOX_LIST + "." + sui.PROP_SELECTEDROW))
            {
                storageRedeedSelected = sui.getListboxSelectedRow(params);
                if (storageRedeedSelected < 0)
                {
                    return SCRIPT_CONTINUE;
                }
            }
            if (player_structure.decrementStorageAmount(player, objShip, self, storageRedeedSelected))
            {
                sendSystemMessage(player, new string_id("player_structure", "storage_increase_redeeded"));
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int handleMoveFirstItem(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_CANCEL)
        {
            obj_id ship = space_transition.getContainingShip(self);
            if (isIdValid(ship) && getOwner(ship) == player)
            {
                moveHouseItemToPlayer(ship, player, 0);
                sendSystemMessage(player, SID_MOVED_FIRST_ITEM);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int handleDeleteSecondConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_CANCEL)
        {
            sui.msgbox(self, player, "@player_structure:delete_all_items_second_d_pob_ship", sui.OK_CANCEL, "@player_structure:delete_all_items", sui.MSG_QUESTION, "handleDeleteItems");
        }
        return SCRIPT_CONTINUE;
    }
    public int handleDeleteItems(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_CANCEL)
        {
            obj_id ship = space_transition.getContainingShip(self);
            if (isIdValid(ship) && getOwner(ship) == player)
            {
                deleteAllHouseItems(ship, player);
                fixHouseItemLimit(ship);
                sendSystemMessage(player, SID_ITEMS_DELETED);
                CustomerServiceLog("playerStructure", "deleteAllItems (Deleting all objects in ship by player's request.) Player: " + player + " (" + getName(player) + ") Ship: " + ship);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int handlePlayerStructureFindItemsListResponse(obj_id self, dictionary params) throws InterruptedException
    {
        player_structure.handleFindItemsListResponse(self, params);
        return SCRIPT_CONTINUE;
    }
    public int handlePlayerStructureFindItemsPageResponse(obj_id self, dictionary params) throws InterruptedException
    {
        player_structure.handleFindItemsChangePageResponse(self, params);
        return SCRIPT_CONTINUE;
    }
    public int handlePlayerStructureSearchItemsGetKeyword(obj_id self, dictionary params) throws InterruptedException
    {
        player_structure.handleSearchItemsGetKeyword(self, params);
        return SCRIPT_CONTINUE;
    }
    public int handlePlayerStructureSearchItemsSelectedResponse(obj_id self, dictionary params) throws InterruptedException
    {
        player_structure.handleSearchItemsSelectedResponse(self, params);
        return SCRIPT_CONTINUE;
    }
    public void showBoardingPermissionsMenu(obj_id self, obj_id player, obj_id ship) throws InterruptedException
    {
        boolean isPublic = space_transition.getBoardingIsPublic(ship);
        String[] allowedList = space_transition.getBoardingAllowed(ship);
        String[] bannedList = space_transition.getBoardingBanned(ship);

        java.util.ArrayList<String> entries = new java.util.ArrayList<String>();
        entries.add(isPublic ? "[Public Boarding: ON] - Toggle" : "[Public Boarding: OFF] - Toggle");
        entries.add("--- Allowed Players ---");
        if (allowedList != null)
        {
            for (String name : allowedList)
                entries.add("  " + name);
        }
        entries.add("Add Player to Allowed List");
        entries.add("--- Banned Players ---");
        if (bannedList != null)
        {
            for (String name : bannedList)
                entries.add("  " + name);
        }
        entries.add("Add Player to Ban List");

        String[] entryArray = entries.toArray(new String[0]);
        int allowedCount = allowedList != null ? allowedList.length : 0;
        int bannedCount = bannedList != null ? bannedList.length : 0;
        utils.setScriptVar(self, "boardingPermissions.ship", ship);
        utils.setScriptVar(self, "boardingPermissions.allowedCount", allowedCount);
        utils.setScriptVar(self, "boardingPermissions.bannedCount", bannedCount);

        int existingPid = sui.getPid(player, BOARDING_PERMISSIONS_PID);
        if (existingPid > -1)
        {
            forceCloseSUIPage(existingPid);
            sui.removePid(player, BOARDING_PERMISSIONS_PID);
        }

        int pid = sui.listbox(self, player, "Manage who can board your ship when parked.", sui.OK_CANCEL, "Boarding Permissions", entryArray, "handleBoardingPermissions", true, false);
        sui.setPid(player, pid, BOARDING_PERMISSIONS_PID);
        showSUIPage(pid);
    }
    public int handleBoardingPermissions(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            sui.removePid(sui.getPlayerId(params), BOARDING_PERMISSIONS_PID);
            return SCRIPT_CONTINUE;
        }

        obj_id player = sui.getPlayerId(params);
        obj_id ship = utils.getObjIdScriptVar(self, "boardingPermissions.ship");
        if (!isIdValid(ship) || getOwner(ship) != player)
            return SCRIPT_CONTINUE;

        int selectedRow = sui.getListboxSelectedRow(params);
        if (selectedRow < 0)
            return SCRIPT_CONTINUE;

        int allowedCount = utils.getIntScriptVar(self, "boardingPermissions.allowedCount");
        int bannedCount = utils.getIntScriptVar(self, "boardingPermissions.bannedCount");

        if (selectedRow == 0)
        {
            boolean isPublic = space_transition.getBoardingIsPublic(ship);
            space_transition.setBoardingIsPublic(ship, !isPublic);
            sendSystemMessage(player, string_id.unlocalized(isPublic ? "Ship boarding set to PRIVATE." : "Ship boarding set to PUBLIC."));
            showBoardingPermissionsMenu(self, player, ship);
            return SCRIPT_CONTINUE;
        }

        int addAllowedRow = 2 + allowedCount;
        int addBannedRow = 2 + allowedCount + 1 + bannedCount + 1;

        if (selectedRow == addAllowedRow)
        {
            sui.inputbox(self, player, "Enter the name of the player to allow boarding:", sui.OK_CANCEL, "Add Allowed Player", sui.INPUT_NORMAL, null, "handleAddAllowed", null);
            return SCRIPT_CONTINUE;
        }

        if (selectedRow == addBannedRow)
        {
            sui.inputbox(self, player, "Enter the name of the player to ban from boarding:", sui.OK_CANCEL, "Add Banned Player", sui.INPUT_NORMAL, null, "handleAddBanned", null);
            return SCRIPT_CONTINUE;
        }

        if (selectedRow >= 2 && selectedRow < addAllowedRow)
        {
            String[] list = space_transition.getBoardingAllowed(ship);
            int idx = selectedRow - 2;
            if (list != null && idx < list.length)
            {
                space_transition.removeBoardingAllowed(ship, list[idx]);
                sendSystemMessage(player, string_id.unlocalized("Removed " + list[idx] + " from allowed list."));
            }
            showBoardingPermissionsMenu(self, player, ship);
            return SCRIPT_CONTINUE;
        }

        int bannedStart = addAllowedRow + 1;
        if (selectedRow >= bannedStart + 1 && selectedRow < addBannedRow)
        {
            String[] list = space_transition.getBoardingBanned(ship);
            int idx = selectedRow - bannedStart - 1;
            if (list != null && idx < list.length)
            {
                space_transition.removeBoardingBanned(ship, list[idx]);
                sendSystemMessage(player, string_id.unlocalized("Removed " + list[idx] + " from ban list."));
            }
            showBoardingPermissionsMenu(self, player, ship);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }
    public int handleAddAllowed(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        obj_id ship = utils.getObjIdScriptVar(self, "boardingPermissions.ship");
        if (!isIdValid(ship) || getOwner(ship) != player)
            return SCRIPT_CONTINUE;

        String name = sui.getInputBoxText(params);
        if (name != null && name.length() > 0)
        {
            space_transition.addBoardingAllowed(ship, name);
            sendSystemMessage(player, string_id.unlocalized(name + " added to allowed boarding list."));
        }
        showBoardingPermissionsMenu(self, player, ship);
        return SCRIPT_CONTINUE;
    }
    public int handleAddBanned(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        obj_id ship = utils.getObjIdScriptVar(self, "boardingPermissions.ship");
        if (!isIdValid(ship) || getOwner(ship) != player)
            return SCRIPT_CONTINUE;

        String name = sui.getInputBoxText(params);
        if (name != null && name.length() > 0)
        {
            space_transition.addBoardingBanned(ship, name);
            sendSystemMessage(player, string_id.unlocalized(name + " added to boarding ban list."));
        }
        showBoardingPermissionsMenu(self, player, ship);
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // Docking / moorage
    // =====================================================================

    private void showMoorageStatus(obj_id ship, obj_id player) throws InterruptedException
    {
        if (hasObjVar(ship, "atmo.landing.landed_at"))
        {
            String name = hasObjVar(ship, "atmo.landing.name") ? getStringObjVar(ship, "atmo.landing.name") : "a landing point";
            sendSystemMessageTestingOnly(player, "\\#aaddff[Docking]: Landed at " + name + ".");
            sendSystemMessageTestingOnly(player, "\\#88ddaa  The captain may take the helm from the bridge to depart when ready.");
            return;
        }

        boolean autopilotOn = hasObjVar(ship, OV_SHIP_AUTOPILOT_ACTIVE);

        if (hasObjVar(ship, "atmo.landing.target"))
        {
            obj_id pad = getObjIdObjVar(ship, "atmo.landing.target");
            if (isIdValid(pad) && exists(pad) && atmo_landing_registry.isLandingPoint(pad))
            {
                obj_id occ = atmo_landing_registry.getOccupyingShip(pad);
                boolean weHoldPad = isIdValid(occ) && occ == ship;

                // Pad already shows this ship landed — moored even if script autopilot objvars were not cleared yet.
                if (weHoldPad && atmo_landing_registry.isLanded(pad))
                {
                    String name = atmo_landing_registry.getLandingPointName(pad);
                    if (name == null || name.isEmpty())
                        name = hasObjVar(ship, "atmo.landing.name") ? getStringObjVar(ship, "atmo.landing.name") : "a landing point";
                    sendSystemMessageTestingOnly(player, "\\#aaddff[Docking]: Landed at " + name + ".");
                    sendSystemMessageTestingOnly(player, "\\#88ddaa  The captain may take the helm from the bridge to depart when ready.");
                    return;
                }

                if (autopilotOn || (weHoldPad && atmo_landing_registry.isEnRoute(pad)))
                {
                    sendSystemMessageTestingOnly(player, "\\#aaddff[Docking]: Auto-pilot is routing to a landing platform.");
                    return;
                }
            }
        }

        sendSystemMessageTestingOnly(player, "\\#778899[Docking]: Not moored at an atmospheric landing platform.");
    }

    private boolean canOfferExtendDocking(obj_id ship) throws InterruptedException
    {
        obj_id pad = atmo_landing_manager.resolveLandingPointFromShip(ship);
        if (!isIdValid(pad))
            return true;
        return atmo_landing_manager.allowsExtendDock(pad);
    }

    private int getExtendCostForShip(obj_id ship) throws InterruptedException
    {
        obj_id pad = atmo_landing_manager.resolveLandingPointFromShip(ship);
        if (isIdValid(pad))
        {
            int v = atmo_landing_manager.getExtendDockCredits(pad);
            if (v >= 0)
                return v;
        }
        return EXTEND_COST;
    }

    private int getExtendSecondsForShip(obj_id ship) throws InterruptedException
    {
        obj_id pad = atmo_landing_manager.resolveLandingPointFromShip(ship);
        if (isIdValid(pad))
        {
            int v = atmo_landing_manager.getExtendDockSeconds(pad);
            if (v > 0)
                return v;
        }
        return EXTEND_TIME;
    }

    /** Game time when the ship is forced off the pad ({@code dockExpiry} + platform grace). */
    private int getHardDockExpiryForShip(obj_id ship) throws InterruptedException
    {
        int expiry = getIntObjVar(ship, "atmo.landing.dockExpiry");
        obj_id pad = atmo_landing_manager.resolveLandingPointFromShip(ship);
        return expiry + atmo_landing_manager.getDockGraceSeconds(pad);
    }

    private void showDockingTimeRemaining(obj_id ship, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(ship, "atmo.landing.dockExpiry"))
        {
            sendSystemMessageTestingOnly(player, "\\#88ddaa[Docking Control]: You have unlimited docking time at this location.");
            return;
        }

        int now = getGameTime();
        int hardExpiry = getHardDockExpiryForShip(ship);
        int remaining = hardExpiry - now;
        obj_id pad = atmo_landing_manager.resolveLandingPointFromShip(ship);
        int grace = atmo_landing_manager.getDockGraceSeconds(pad);
        int paidThrough = getIntObjVar(ship, "atmo.landing.dockExpiry");
        int paidRemaining = paidThrough - now;

        String name = hasObjVar(ship, "atmo.landing.name") ? getStringObjVar(ship, "atmo.landing.name") : "Landing Pad";
        int extendCost = getExtendCostForShip(ship);
        int extendSec = getExtendSecondsForShip(ship);

        if (remaining <= 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Forced departure is overdue — leave the pad or expect auto-relocation.");
        }
        else
        {
            int rm = remaining / 60;
            int rs = remaining % 60;
            String timeStr = rm > 0 ? (rm + "m " + rs + "s") : (rs + "s");
            sendSystemMessageTestingOnly(player, "\\#aaddff[Docking Control]: " + name);
            sendSystemMessageTestingOnly(player, "\\#aaddff  Time until forced departure: " + timeStr + (grace > 0 ? " (includes " + grace + "s platform grace)" : ""));
            if (grace > 0 && paidRemaining <= 0 && remaining > 0)
                sendSystemMessageTestingOnly(player, "\\#ffaa44  Paid mooring time has ended — you are in the grace window. Extend at this terminal to add time.");
            else if (grace > 0 && paidRemaining > 0)
            {
                int pm = paidRemaining / 60;
                int ps = paidRemaining % 60;
                String paidStr = pm > 0 ? (pm + "m " + ps + "s") : (ps + "s");
                sendSystemMessageTestingOnly(player, "\\#778899  Paid mooring window: " + paidStr + " — extend before it ends to avoid relying on grace.");
            }
            if (canOfferExtendDocking(ship))
                sendSystemMessageTestingOnly(player, "\\#aaddff  Extension: " + extendCost + " credits for +" + (extendSec / 60) + " min (Starship Management Terminal → Docking).");
            else
                sendSystemMessageTestingOnly(player, "\\#aaddff  Extensions are not available at this platform.");
        }
    }

    private void showExtendDockingUI(obj_id terminal, obj_id ship, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(ship, "atmo.landing.dockExpiry"))
        {
            sendSystemMessageTestingOnly(player, "\\#88ddaa[Docking Control]: You have unlimited docking time. No extension needed.");
            return;
        }

        if (!canOfferExtendDocking(ship))
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Docking Control]: This platform does not allow docking time extensions.");
            return;
        }

        String name = hasObjVar(ship, "atmo.landing.name") ? getStringObjVar(ship, "atmo.landing.name") : "Landing Pad";
        int hardExpiry = getHardDockExpiryForShip(ship);
        int remaining = hardExpiry - getGameTime();
        int mins = remaining / 60;
        int secs = remaining % 60;
        String timeStr = mins > 0 ? (mins + "m " + secs + "s") : (secs + "s");
        int extendCost = getExtendCostForShip(ship);
        int extendSec = getExtendSecondsForShip(ship);
        obj_id pad = atmo_landing_manager.resolveLandingPointFromShip(ship);
        int grace = atmo_landing_manager.getDockGraceSeconds(pad);

        String title = "Extend Docking Time";
        String prompt = "\\#00ccffLocation: " + name + "\n\n" +
                        "\\#aaddffTime until forced departure: " + timeStr + (grace > 0 ? " (incl. " + grace + "s grace)" : "") + "\n\n" +
                        "\\#ffffffPay " + extendCost + " credits to add " + (extendSec / 60) + " minutes to your mooring window?";

        utils.setScriptVar(player, "docking.extend.ship", ship);
        sui.msgbox(terminal, player, prompt, sui.YES_NO, title, "handleExtendDocking");
    }

    public int handleExtendDocking(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp != sui.BP_OK)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
            return SCRIPT_CONTINUE;

        obj_id ship = utils.getObjIdScriptVar(player, "docking.extend.ship");
        utils.removeScriptVar(player, "docking.extend.ship");

        if (!isIdValid(ship) || !exists(ship))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Unable to extend docking time. Ship not found.");
            return SCRIPT_CONTINUE;
        }

        if (!hasObjVar(ship, "atmo.landing.dockExpiry"))
        {
            sendSystemMessageTestingOnly(player, "\\#88ddaa[Docking Control]: No docking timer active.");
            return SCRIPT_CONTINUE;
        }

        if (!canOfferExtendDocking(ship))
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Docking Control]: This platform does not allow docking time extensions.");
            return SCRIPT_CONTINUE;
        }

        int extendCost = getExtendCostForShip(ship);
        int extendSec = getExtendSecondsForShip(ship);

        int bankBalance = getBankBalance(player);
        int cashBalance = getCashBalance(player);
        int totalCredits = bankBalance + cashBalance;

        if (totalCredits < extendCost)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Insufficient credits. You need " + extendCost + " credits.");
            return SCRIPT_CONTINUE;
        }

        // Transfer credits to docking fees account
        dictionary pay = new dictionary();
        pay.put("dock_extend_player", player);
        pay.put("dock_extend_ship", ship);
        pay.put("dock_extend_sec", extendSec);
        pay.put("dock_extend_cost", extendCost);

        if (!transferBankCreditsToNamedAccount(player, money.ACCT_TRAVEL, extendCost, "handleDockingPaymentSuccess", "handleDockingPaymentFail", pay))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Could not start payment. Please try again.");
            return SCRIPT_CONTINUE;
        }

        sendSystemMessageTestingOnly(player, "\\#aaddff[Docking Control]: Processing payment...");
        return SCRIPT_CONTINUE;
    }

    public int handleDockingPaymentSuccess(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        obj_id player = params.getObjId("dock_extend_player");
        obj_id ship = params.getObjId("dock_extend_ship");
        int extendSec = params.getInt("dock_extend_sec");
        int extendCost = params.getInt("dock_extend_cost");

        if (!isIdValid(ship) || !exists(ship) || !hasObjVar(ship, "atmo.landing.dockExpiry"))
            return SCRIPT_CONTINUE;

        if (!hasObjVar(ship, "atmo.landing.docked"))
        {
            if (isIdValid(player))
                sendSystemMessageTestingOnly(player, "\\#ffaa44[Docking Control]: Ship is no longer docked; mooring time was not extended.");
            return SCRIPT_CONTINUE;
        }

        int currentExpiry = getIntObjVar(ship, "atmo.landing.dockExpiry");
        setObjVar(ship, "atmo.landing.dockExpiry", currentExpiry + extendSec);
        atmo_landing_docked.kickDockTimer(ship);

        if (isIdValid(player))
        {
            play2dNonLoopingSound(player, "sound/sys_comm_generic.snd");
            sendSystemMessageTestingOnly(player, "\\#00ff88[Docking Control]: Mooring extended by " + (extendSec / 60) + " minutes.");
            sendSystemMessageTestingOnly(player, "\\#aaddff  " + extendCost + " credits charged. Dock timer updated.");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleDockingPaymentFail(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;
        obj_id player = params.getObjId("dock_extend_player");
        if (isIdValid(player))
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Payment failed. Mooring was not extended.");
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // Undock Methods
    // =====================================================================

    private void showUndockConfirmation(obj_id terminal, obj_id ship, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(ship, "atmo.landing.docked"))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Ship is not docked.");
            return;
        }

        String name = hasObjVar(ship, "atmo.landing.name") ? getStringObjVar(ship, "atmo.landing.name") : "Landing Pad";

        String title = "Undock Ship";
        String prompt = "\\#00ccffCurrently docked at: " + name + "\n\n" +
                        "\\#ffffffUndocking will move your ship to a safe altitude above the landing point.\n\n" +
                        "\\#ffaa44Are you sure you want to undock?";

        utils.setScriptVar(player, "undock.ship", ship);
        sui.msgbox(terminal, player, prompt, sui.YES_NO, title, "handleUndockConfirmation");
    }

    public int handleUndockConfirmation(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp != sui.BP_OK)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
            return SCRIPT_CONTINUE;

        obj_id ship = utils.getObjIdScriptVar(player, "undock.ship");
        utils.removeScriptVar(player, "undock.ship");

        if (!isIdValid(ship) || !exists(ship))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Unable to undock. Ship not found.");
            return SCRIPT_CONTINUE;
        }

        if (!hasObjVar(ship, "atmo.landing.docked"))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Docking Control]: Ship is not docked.");
            return SCRIPT_CONTINUE;
        }

        // Send undock request to the ship's docked script
        dictionary undockParams = new dictionary();
        undockParams.put("player", player);
        messageTo(ship, "handleUndockRequest", undockParams, 0, false);

        return SCRIPT_CONTINUE;
    }
}
