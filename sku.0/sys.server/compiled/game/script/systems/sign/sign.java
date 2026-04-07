package script.systems.sign;

/*
 * Copyright © SWG: Titan 2024.
 *
 * Unauthorized usage, viewing or sharing of this file is prohibited.
 */

import script.*;
import script.library.*;

public class sign extends script.base_script
{
    public static final boolean LOGGING_ON = false;
    public static final String LOGGING_CATEGORY = "packup";
    public static final string_id SID_TERMINAL_MANAGEMENT = new string_id("player_structure", "management");
    public static final string_id SID_TERMINAL_PACK_HOUSE = new string_id("sui", "packup_house");
    public static final string_id EMAIL_TITLE = new string_id("spam", "email_title");
    public static final string_id EMAIL_BODY = new string_id("spam", "email_body");
    public static final string_id SID_OWNER_PACKUP_AT_TERMINAL = new string_id("player_structure", "onwer_packup_at_terminal");
    public static final int minTimeDelayBetweenSameServerRequests = 300;
    public static final String timeOfLastSameServerRequest = "timeOfLastSameServerRequest";
    public static final string_id SID_MAYOR_HOUSE_SIGN_DISPLAY = new string_id("city/city", "house_owner");
    public static final string_id SID_TERMINAL_CITY_PACK_HOUSE = new string_id("city/city", "city_packup_house");
    public static final string_id SID_NOT_CITY_ABANDONED = new string_id("city/city", "not_city_abandoned");
    public static final int cityMinTimeDelayBetweenSameServerRequests = 300;
    public static final String cityTimeOfLastSameServerRequest = "timeOfLastSameServerRequest";
    /** Persisted display name when set by a god (see God: Set Sign Name). */
    public static final String OV_SIGN_GOD_CUSTOM_NAME = "sign.godCustomName";
    public static final int MAX_SIGN_GOD_NAME_LEN = 128;
    //@TODO: Add in custom door bell

    public sign()
    {
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        applyGodCustomSignName(self);
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        applyGodCustomSignName(self);
        return SCRIPT_CONTINUE;
    }

    private void applyGodCustomSignName(obj_id self) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || !hasObjVar(self, OV_SIGN_GOD_CUSTOM_NAME))
        {
            return;
        }
        String n = getStringObjVar(self, OV_SIGN_GOD_CUSTOM_NAME);
        if (n == null)
        {
            return;
        }
        n = n.trim();
        if (n.isEmpty())
        {
            removeObjVar(self, OV_SIGN_GOD_CUSTOM_NAME);
            return;
        }
        if (n.length() > MAX_SIGN_GOD_NAME_LEN)
        {
            n = n.substring(0, MAX_SIGN_GOD_NAME_LEN);
        }
        setName(self, n);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        menu_info_data menuData = mi.getMenuItemByType(menu_info_types.ITEM_USE);
        if (menuData != null)
        {
            menuData.setServerNotify(true);
        }
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU28, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU12, string_id.unlocalized("Set Sign Name"));
        }
        deltadictionary scriptvars = self.getScriptVars();
        if (!utils.hasScriptVar(self, "player_structure.parent"))
        {
            if (!isGod(player))
            {
                LOG("sissynoid", "Player (" + player + ")" + getPlayerFullName(player) + " attempted to access a house sign(" + self + ") - and the sign has no parent (parent is the House ObjId).");
                CustomerServiceLog("playerStructure", "Player (" + player + ")" + getPlayerFullName(player) + " attempted to access a house sign(" + self + ") - and the sign has no parent (parent is the House ObjId).");
            }
            return SCRIPT_CONTINUE;
        }
        mi.addRootMenu(menu_info_types.SERVER_MENU3, new string_id("Ring Doorbell"));
        obj_id house = scriptvars.getObjId("player_structure.parent");
        if (player_structure.canShowPackOption(player, house))
        {
            int management_root = mi.addRootMenu(menu_info_types.SERVER_TERMINAL_MANAGEMENT, SID_TERMINAL_MANAGEMENT);
            mi.addSubMenu(management_root, menu_info_types.SERVER_MENU10, SID_TERMINAL_PACK_HOUSE);
        }
        if (player_structure.doesUnmarkedStructureQualifyForHousePackup(house) && !player_structure.isAbandoned(house) && player_structure.isCityAbandoned(house) && cityIsInactivePackupActive())
        {
            int management_root = mi.addRootMenu(menu_info_types.SERVER_TERMINAL_MANAGEMENT, SID_TERMINAL_MANAGEMENT);
            mi.addSubMenu(management_root, menu_info_types.SERVER_MENU11, SID_TERMINAL_CITY_PACK_HOUSE);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU12)
        {
            if (!isGod(player))
            {
                return SCRIPT_CONTINUE;
            }
            String def = "";
            if (hasObjVar(self, OV_SIGN_GOD_CUSTOM_NAME))
            {
                def = getStringObjVar(self, OV_SIGN_GOD_CUSTOM_NAME);
            }
            String prompt = "Enter display name for this sign (saved to objvar " + OV_SIGN_GOD_CUSTOM_NAME + "). Leave empty to clear.";
            String title = "God: Sign Name";
            sui.inputbox(self, player, prompt, title, "handleSignGodCustomName", MAX_SIGN_GOD_NAME_LEN, false, def);
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.ITEM_USE)
        {
            obj_id parent = obj_id.NULL_ID;
            String text = "";
            if (utils.hasScriptVar(self, "player_structure.parent"))
            {
                parent = utils.getObjIdScriptVar(self, "player_structure.parent");
                if (player_structure.isPlayerStructure(parent))
                {
                    text = getName(self);
                    int cityId = getCityAtLocation(getLocation(parent), 0);
                    if (cityId > 0 && city.isTheCityMayor(player, cityId))
                    {
                        obj_id houseOwnerId = player_structure.getStructureOwnerObjId(parent);
                        String houseOwnerName = getPlayerFullName(houseOwnerId);
                        prose_package pp = new prose_package();
                        prose.setStringId(pp, SID_MAYOR_HOUSE_SIGN_DISPLAY);
                        prose.setTT(pp, text);
                        prose.setTU(pp, houseOwnerName);
                        sui.msgbox(self, player, pp, "noHandlerNeeded");
                    }
                    else
                    {
                        if (hasObjVar(self, "sign.customText"))
                        {
                            text = getStringObjVar(self, "sign.customText");
                            sui.msgbox(self, player, text, sui.OK_ONLY, getEncodedName(self), "noHandlerNeeded");
                        }
                        else
                        {
                            text = getName(self);
                            sui.msgbox(self, player, text, sui.OK_ONLY, getEncodedName(self), "noHandlerNeeded");
                        }
                    }
                    return SCRIPT_CONTINUE;
                }
                else
                {
                    if (hasObjVar(self, "sign.customText"))
                    {
                        text = getStringObjVar(self, "sign.customText");
                        sui.msgbox(self, player, text, sui.OK_ONLY, getEncodedName(self), "noHandlerNeeded");
                    }
                    else
                    {
                        text = getName(self);
                        sui.msgbox(self, player, text, sui.OK_ONLY, getEncodedName(self), "noHandlerNeeded");
                    }
                    return SCRIPT_CONTINUE;
                }
            }
            if (hasObjVar(self, "sign.customText"))
            {
                text = getStringObjVar(self, "sign.customText");
            }
            else
            {
                string_id desc = getDescriptionStringId(self);
                text = utils.packStringId(desc);
            }
            //sui.msgbox(self, player, text, "noHandlerNeeded");
            String signName = getEncodedName(self);
            if (signName.length() > 80)
            {
                signName = "Signage";
            }
            sui.msgbox(self, player, text, sui.OK_ONLY, getEncodedName(self), "noHandlerNeeded");
        }
        if (item == menu_info_types.SERVER_MENU10)
        {
            deltadictionary scriptvars = self.getScriptVars();
            obj_id house = scriptvars.getObjId("player_structure.parent");
            if (player_structure.isOwner(house, player))
            {
                sendSystemMessage(player, SID_OWNER_PACKUP_AT_TERMINAL);
                return SCRIPT_CONTINUE;
            }
            if (player_structure.isPlayerGatedFromHousePackUp(player))
            {
                return SCRIPT_CONTINUE;
            }
            if (!player_structure.canPlayerPackAbandonedStructure(player, house))
            {
                if (isAtPendingLoadRequestLimit())
                {
                    sendSystemMessage(player, new string_id("player_structure", "abandoned_structure_pack_up_try_again_later"));
                }
                else if (player_structure.isAbandoned(house) && (!house.isAuthoritative() || !player.isAuthoritative()))
                {
                    if (!utils.hasScriptVar(player, timeOfLastSameServerRequest) || utils.getIntScriptVar(player, timeOfLastSameServerRequest) < getGameTime())
                    {
                        requestSameServer(player, house);
                        utils.setScriptVar(player, timeOfLastSameServerRequest, getGameTime() + minTimeDelayBetweenSameServerRequests);
                        utils.setScriptVar(player, "requestedSameServerToAbandonHouse", house);
                    }
                    sendSystemMessage(player, new string_id("player_structure", "abandoned_structure_pack_up_please_wait_processing"));
                }
                return SCRIPT_CONTINUE;
            }
            if (isIdValid(house) && !utils.hasScriptVar(house, player_structure.SCRIPTVAR_HOUSE_PACKUP_LOCKOUT_TIME))
            {
                dictionary params = new dictionary();
                params.put("house", house);
                params.put("player", player);
                messageTo(house, "packAbandonedBuilding", params, 4, false);
                messageTo(player, "handlePlayerStructurePackupLockout", params, 0, false);
                if (!hasObjVar(player, player_structure.HOUSE_PACKUP_ARRAY_OBJVAR))
                {
                    String recipient = getPlayerName(player);
                    utils.sendMail(EMAIL_TITLE, EMAIL_BODY, recipient, "Galactic Vacant Building Demolishing Movement");
                }
            }
        }
        if (item == menu_info_types.SERVER_MENU11 && cityIsInactivePackupActive())
        {
            obj_id sign = getSelf();
            AttemptPackCityAbandonedStructure(player, sign);
        }
        if (item == menu_info_types.SERVER_MENU3)
        {
            playClientEffectObj(player, "sound/item_fusioncutter_start.snd", player, "");
            String commPrompt = colors_hex.HEADER + colors_hex.AQUAMARINE + getPlayerFullName(player) + " is at this structure's entrance.";
            obj_id house = utils.getObjIdScriptVar(self, "player_structure.parent");
            obj_id[] occupants = player_structure.getPlayersInBuilding(house);
            if (isIdValid(house))
            {
                if (occupants.length == 0)
                {
                    broadcast(player, "There does not appear to be anyone home.");
                    return SCRIPT_CONTINUE;
                }
                for (obj_id occupant : occupants)
                {
                    String doorbellSnd = getStringObjVar(house, "player_structure.doorbell_snd");
                    if (doorbellSnd == null || doorbellSnd.isEmpty())
                    {
                        doorbellSnd = "sound/item_fusioncutter_start.snd";
                    }
                    playClientEffectObj(occupant, doorbellSnd, occupant, "");
                    prose_package pp = new prose_package();
                    prose.setStringId(pp, new string_id(commPrompt));
                    commPlayer(player, occupant, pp);
                    broadcast(player, "You have rang this structure's doorbell.");
                    return SCRIPT_CONTINUE;
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSignGodCustomName(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !isGod(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String text = sui.getInputBoxText(params);
        if (text == null)
        {
            text = "";
        }
        text = text.trim();
        if (text.length() > MAX_SIGN_GOD_NAME_LEN)
        {
            text = text.substring(0, MAX_SIGN_GOD_NAME_LEN);
        }
        if (text.isEmpty())
        {
            removeObjVar(self, OV_SIGN_GOD_CUSTOM_NAME);
            sendSystemMessage(player, "God custom sign name cleared (" + OV_SIGN_GOD_CUSTOM_NAME + ").", null);
            CustomerServiceLog("playerStructure", "God " + getPlayerFullName(player) + " cleared god sign name on " + self);
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, OV_SIGN_GOD_CUSTOM_NAME, text);
        setName(self, text);
        sendSystemMessage(player, "Sign name set and saved.", null);
        CustomerServiceLog("playerStructure", "God " + getPlayerFullName(player) + " set god sign name on " + self + " to: " + text);
        return SCRIPT_CONTINUE;
    }

    public int handleRemoteCommandCityHousePackup(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            CustomerServiceLog("playerStructure", "Remote City Abandoned Packup - Params were NULL when messageTo was sent.");
            return SCRIPT_CONTINUE;
        }
        obj_id player = params.getObjId("player");
        obj_id paramsSign = params.getObjId("sign");
        if (!isIdValid(player) || !isIdValid(paramsSign))
        {
            CustomerServiceLog("playerStructure", "Remote City Abandoned Packup - Params were bad: Player(" + player + "), paramsSign(" + paramsSign + ")");
            return SCRIPT_CONTINUE;
        }
        if (paramsSign != self)
        {
            CustomerServiceLog("playerStructure", "Remote City Abandoned Packup - Player(" + player + ") was attempting to pack a house - but the signs don't match: paramsSign(" + paramsSign + "), self(" + self + ")");
            return SCRIPT_CONTINUE;
        }
        AttemptPackCityAbandonedStructure(player, paramsSign);
        return SCRIPT_CONTINUE;
    }

    public void AttemptPackCityAbandonedStructure(obj_id player, obj_id sign) throws InterruptedException
    {
        if (!utils.hasScriptVar(sign, "player_structure.parent"))
        {
            CustomerServiceLog("playerStructure", "Player(" + player + ") attempted city pack a structure - but the House Sign(" + sign + ") has invalid data.");
            return;
        }
        obj_id house = utils.getObjIdScriptVar(sign, "player_structure.parent");
        if (!house.isAuthoritative() || !player.isAuthoritative())
        {
            LOG("sissynoid", "Player (" + player + ")" + getPlayerFullName(player) + " attempted to access a house sign(" + sign + ") - Structure and Sign were on different server processes - requesting Same Server.");
            CustomerServiceLog("playerStructure", "Player (" + player + ")" + getPlayerFullName(player) + " attempted to access a house sign(" + sign + ") - Structure and Sign were on different server processes - requesting Same Server.");
            if (!utils.hasScriptVar(player, cityTimeOfLastSameServerRequest) || utils.getIntScriptVar(player, cityTimeOfLastSameServerRequest) < getGameTime())
            {
                requestSameServer(player, house);
                utils.setScriptVar(player, cityTimeOfLastSameServerRequest, getGameTime() + cityMinTimeDelayBetweenSameServerRequests);
                utils.setScriptVar(player, "cityRequestedSameServerToAbandonHouse", house);
            }
            sendSystemMessage(player, new string_id("player_structure", "abandoned_structure_pack_up_please_wait_processing"));
        }
        else
        {
            player_structure.confirmCityAbandonedAndPack(house, player);
        }
    }

    public boolean blog(String msg) throws InterruptedException
    {
        if (LOGGING_ON && msg != null && !msg.isEmpty())
        {
            LOG(LOGGING_CATEGORY, msg);
        }
        return true;
    }
}