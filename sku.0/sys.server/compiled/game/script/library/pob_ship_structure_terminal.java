package script.library;

import script.*;

/**
 * Housing-style radial entries for players inside POB ships at a space terminal.
 * Called from {@code space.terminal.terminal_space} on every menu request so script reload applies without relying on menu logic in {@code terminal.terminal_structure}.
 */
public class pob_ship_structure_terminal extends script.base_script
{
    public pob_ship_structure_terminal()
    {
    }

    public static final string_id SID_TERMINAL_PERMISSIONS = new string_id("player_structure", "permissions");
    public static final string_id SID_TERMINAL_MANAGEMENT = new string_id("player_structure", "management");
    public static final string_id SID_TERMINAL_PERMISSIONS_ENTER = new string_id("player_structure", "permission_enter");
    public static final string_id SID_TERMINAL_PERMISSIONS_BANNED = new string_id("player_structure", "permission_banned");
    public static final string_id SID_TERMINAL_PERMISSIONS_ADMIN = new string_id("player_structure", "permission_admin");
    public static final string_id SID_TERMINAL_MANAGEMENT_STATUS = new string_id("player_structure", "management_status");
    public static final string_id SID_TERMINAL_MANAGEMENT_PRIVACY = new string_id("player_structure", "management_privacy");
    public static final string_id SID_TERMINAL_MANAGEMENT_PRIVACY_PRIVATE = new string_id("player_structure", "management_privacy_private");
    public static final string_id SID_TERMINAL_MANAGEMENT_PRIVACY_PUBLIC = new string_id("player_structure", "management_privacy_public");
    public static final string_id SID_TERMINAL_CREATE_VENDOR = new string_id("player_structure", "create_vendor");
    public static final string_id SID_TERMINAL_NAME_STRUCTURE = new string_id("player_structure", "management_name_structure");
    public static final string_id SID_FIND_ALL_HOUSE_ITEMS = new string_id("player_structure", "find_items_find_all_house_items");
    public static final string_id SID_SEARCH_FOR_HOUSE_ITEMS = new string_id("player_structure", "find_items_search_for_house_items");
    public static final string_id SID_MOVE_FIRST_ITEM = new string_id("player_structure", "move_first_item");
    public static final string_id SID_DELETE_ALL_ITEMS = new string_id("player_structure", "delete_all_items");
    public static final string_id SID_TERMINAL_LIGHTSWITCH = new string_id("Modify Structure Lighting");
    public static final string_id SID_TERMINAL_REDEED_STORAGE = new string_id("player_structure", "redeed_storage");
    public static final string_id SID_REVERT_CUSTOM_SIGN = new string_id("player_structure", "revert_sign");
    public static final string_id SID_TERMINAL_MANAGEMENT_SPECIAL_SIGNS = new string_id("player_structure", "special_sign_management");

    public static void appendRadialMenus(obj_id terminal, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isIdValid(terminal) || !isIdValid(player))
        {
            return;
        }
        if (isDead(player) || isIncapacitated(player))
        {
            return;
        }

        obj_id shipFromPlayer = space_transition.getContainingShip(player);
        if (!isIdValid(shipFromPlayer))
        {
            return;
        }

        obj_id shipFromTerminal = space_transition.getContainingShip(terminal);
        obj_id shipFromTerminalTop = obj_id.NULL_ID;
        obj_id topMost = getTopMostContainer(terminal);
        if (isIdValid(topMost) && isGameObjectTypeOf(getGameObjectType(topMost), GOT_ship))
        {
            shipFromTerminalTop = topMost;
        }

        final boolean terminalOnSameShip =
                (isIdValid(shipFromTerminal) && shipFromTerminal == shipFromPlayer)
                        || (isIdValid(shipFromTerminalTop) && shipFromTerminalTop == shipFromPlayer);
        if (!terminalOnSameShip)
        {
            return;
        }

        obj_id structure = shipFromPlayer;
        if (!isGameObjectTypeOf(getGameObjectType(structure), GOT_ship) || player_structure.isBuilding(structure))
        {
            return;
        }

        obj_id shipOwner = getOwner(structure);
        if (!isIdValid(shipOwner) || !charactersAreSamePlayer(player, shipOwner))
        {
            return;
        }

        final boolean isStructureOwner = charactersAreSamePlayer(player, shipOwner);

        final int management_root = mi.addRootMenu(menu_info_types.SERVER_TERMINAL_MANAGEMENT, SID_TERMINAL_MANAGEMENT);
        mi.addSubMenu(management_root, menu_info_types.SERVER_TERMINAL_MANAGEMENT_STATUS, SID_TERMINAL_MANAGEMENT_STATUS);
        mi.addSubMenu(management_root, menu_info_types.SET_NAME, SID_TERMINAL_NAME_STRUCTURE);

        final int permissions_root = mi.addRootMenu(menu_info_types.SERVER_TERMINAL_PERMISSIONS, SID_TERMINAL_PERMISSIONS);
        mi.addSubMenu(permissions_root, menu_info_types.SERVER_TERMINAL_PERMISSIONS_ADMIN, SID_TERMINAL_PERMISSIONS_ADMIN);
        mi.addSubMenu(permissions_root, menu_info_types.SERVER_TERMINAL_PERMISSIONS_ENTER, SID_TERMINAL_PERMISSIONS_ENTER);
        mi.addSubMenu(permissions_root, menu_info_types.SERVER_TERMINAL_PERMISSIONS_BANNED, SID_TERMINAL_PERMISSIONS_BANNED);

        string_id privacyMenu_sid = SID_TERMINAL_MANAGEMENT_PRIVACY;
        if (permissionsIsPublic(structure))
        {
            privacyMenu_sid = SID_TERMINAL_MANAGEMENT_PRIVACY_PUBLIC;
        }
        else
        {
            privacyMenu_sid = SID_TERMINAL_MANAGEMENT_PRIVACY_PRIVATE;
        }
        mi.addSubMenu(management_root, menu_info_types.SERVER_TERMINAL_MANAGEMENT_PRIVACY, privacyMenu_sid);

        if (getSkillStatMod(player, "manage_vendor") > 0)
        {
            mi.addSubMenu(management_root, menu_info_types.SERVER_TERMINAL_CREATE_VENDOR, SID_TERMINAL_CREATE_VENDOR);
        }

        mi.addSubMenu(management_root, menu_info_types.SERVER_MENU12, SID_FIND_ALL_HOUSE_ITEMS);
        mi.addSubMenu(management_root, menu_info_types.SERVER_MENU13, SID_SEARCH_FOR_HOUSE_ITEMS);
        mi.addSubMenu(management_root, menu_info_types.SERVER_MENU9, SID_MOVE_FIRST_ITEM);
        mi.addSubMenu(management_root, menu_info_types.SERVER_MENU2, SID_DELETE_ALL_ITEMS);
        mi.addSubMenu(management_root, menu_info_types.SERVER_MENU17, SID_TERMINAL_LIGHTSWITCH);

        if (isStructureOwner && hasObjVar(structure, player_structure.OBJVAR_STRUCTURE_STORAGE_INCREASE))
        {
            mi.addSubMenu(management_root, menu_info_types.DICE_ROLL, SID_TERMINAL_REDEED_STORAGE);
        }

        if (hasObjVar(structure, player_structure.MODIFIED_HOUSE_SIGN) && isStructureOwner)
        {
            mi.addSubMenu(management_root, menu_info_types.SERVER_MENU11, SID_REVERT_CUSTOM_SIGN);
        }

        if (hasObjVar(structure, player_structure.SPECIAL_SIGN) || player_structure.hasSpecialSignSkillMod(player, structure))
        {
            mi.addSubMenu(management_root, menu_info_types.SERVER_MENU14, SID_TERMINAL_MANAGEMENT_SPECIAL_SIGNS);
        }
    }
}
