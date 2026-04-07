package script.event.lifeday;/*
@Origin: dsrc.script.event.anniversary
@Author:  BubbaJoeX
@Purpose: <no purpose>
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Wednesday, 8/14/2024, at 11:31 PM,
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.gm;
import script.library.static_item;
import script.library.sui;
import script.library.utils;

import java.util.HashSet;

public class new_years_gift extends script.event.anniversary.stadium_lib
{
    public int setup(obj_id self)
    {
        setName(self, "New Years Gift Box");
        setDescriptionString(self, "A gift box from the SWG-OR team to celebrate the new year.");
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self)
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (canManipulate(player, self, true, true, 15, true))
        {
            mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Open Gift"));
        }
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU40, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU1, new string_id("Override Gift Lockout"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            obj_id inv = utils.getInventoryContainer(player);
            if (inv == null)
            {
                return SCRIPT_CONTINUE;
            }
            grantObjects(self, player, inv);
        }
        if (item == menu_info_types.SERVER_MENU1 && isGod(player))
        {
            obj_id inv = utils.getInventoryContainer(player);
            if (inv == null)
            {
                return SCRIPT_CONTINUE;
            }
            grantObjects(self, player, inv);
        }
        return SCRIPT_CONTINUE;
    }

    public int grantObjects(obj_id self, obj_id player, obj_id inventory) throws InterruptedException
    {
        //return if less than 6 inventory slots
        if (getVolumeFree(inventory) < 2)
        {
            broadcast(player, "You do not have enough inventory space to open the gift (2 slots required).");
            return SCRIPT_CONTINUE;
        }
        String[] objects = {
                "item_event_token_01_01",
                "item_event_token_01_01",
        };
        HashSet theSet = new HashSet();
        for (String object : objects)
        {
            obj_id item = static_item.createNewItemFunction(object, inventory);
            if (isIdValid(item))
            {
                theSet.add(item);
            }
        }
        obj_id tradableHolocron = static_item.createNewItemFunction("item_auto_level_90_buddy_conversion", inventory);
        detachScript(tradableHolocron, "item.special.nomove");
        detachScript(tradableHolocron, "item.static_item_base");
        setDescriptionString(tradableHolocron, "This Holocron of Knowledge can be used by one of your characters or be traded to a friend to increase their level to 90.");
        if (isIdValid(tradableHolocron))
        {
            theSet.add(tradableHolocron);
        }
        obj_id[] items = new obj_id[theSet.size()];
        theSet.toArray(items);
        showLootBox(player, items);
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
}
