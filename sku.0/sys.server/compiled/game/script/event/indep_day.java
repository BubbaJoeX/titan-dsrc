package script.event;/*
@Origin: dsrc.script.event
@Author:  BubbaJoeX
@Purpose: Event Grant for July 4th
@Requirements: <no requirements>
@Notes: Once per account
@Created: Wednesday, 7/3/2024, at 9:32 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.gm;
import script.library.utils;

public class indep_day extends base_script
{
    public int OnAttach(obj_id self)
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    private void setup(obj_id self)
    {
        setName(self, "Event Token Dispenser");
        setDescriptionString(self, "This object will grant players one (1) Event Token per account to claim a TCG item of their choice.");

    }

    public int OnInitialize(obj_id self)
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Claim Holiday Gift"));
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU40, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU1, new string_id("Reset For Self"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            if (!hasObjVar(self, "event_grant." + getPlayerStationId(player)))
            {
                grantReward(self, player);
            }
            else
            {
                broadcast(player, "You have already claimed this gift, and cannot claim another.");
                return SCRIPT_CONTINUE;
            }
        }
        else if (item == menu_info_types.SERVER_MENU1 && isGod(player))
        {
            removeObjVar(self, "event_grant." + getPlayerStationId(player));
            LOG("ethereal", "[July 4th-8th Event]: Player " + getPlayerFullName(player) + " has reset their check for the event token grant.");
        }
        return SCRIPT_CONTINUE;
    }

    private void grantReward(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        if (isIdValid(pInv))
        {
            obj_id event_token = createObject("object/tangible/loot/misc/marauder_token.iff", pInv, "");
            setName(event_token, "Event Token");
            setDescriptionString(event_token, "This token may be used to claim one (1) TCG item of your choice.");
            attachScript(event_token, "content.tcg_voucher_vendor");
            setObjVar(self, "event_grant." + getPlayerStationId(player), true);
            broadcast(player, "You have claimed this gift.");
            LOG("ethereal", "[July 4th-8th Event]: Player " + getPlayerFullName(player) + " has claimed their event token.");
        }
    }
}
