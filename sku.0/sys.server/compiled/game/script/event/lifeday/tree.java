package script.event.lifeday;/*
@Origin: dsrc.script.event.lifeday
@Author: BubbaJoeX
@Purpose: Decoration Item or convert to Hard Wood
@Created: Saturday, 11/18/2023, at 7:56 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.titan_player;
import script.library.utils;

public class tree extends base_script
{
    public void reInitialize(obj_id self) throws InterruptedException
    {
        setName(self, "Life Day Tree");
        setDescriptionString(self, "This tree can be used for resources or decoration.");
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        reInitialize(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        reInitialize(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (isDead(player) || isIncapacitated(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (isInWorldCell(self))
        {
            return SCRIPT_CONTINUE;
        }
        if (getOwner(self) != player)
        {
            return SCRIPT_CONTINUE;
        }
        if (canManipulate(player, self, true, true, 15, true))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Retrieve Wood"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1)
        {
            obj_id inventory = utils.getInventoryContainer(player);
            makeWood(player, inventory);
            destroyObject(self);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    private void makeWood(obj_id player, obj_id inventory)
    {
        obj_id rtype = pickRandomNonDepeletedResource("wood");
        if (!isIdValid(rtype))
        {
            titan_player.logEthereal("Life Day Tree", "makeWood No valid resource type found for *wood*");
            return;
        }
        String crateTemplate = getResourceContainerForType(rtype);
        if (!crateTemplate.isEmpty())
        {
            int woodAmount = rand(2400, 5000);
            obj_id crate = createObject(crateTemplate, inventory, "");
            if (addResourceToContainer(crate, rtype, woodAmount, player))
            {
                //always use broadcast over sendSystemMessage. It's easier to type.
                broadcast(player, "You have harvested " + getResourceName(rtype) + " from the tree.");
            }
            else
            {
                LOG("events", "Life Day Tree: Player " + player + " failed to harvest " + getResourceName(rtype) + " from the tree.");
            }
        }
    }
}
