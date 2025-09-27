package script.event.gjpud;/*
@Origin: dsrc.script.event.gjpud
@Author:  BubbaJoeX
@Purpose: Converts Scrap Heaps into Event Tokens
@Requirements: GM Intervention to spawn junk
@Notes: Needs model
@Created: Thursday, 7/11/2024, at 3:48 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.library.static_item;
import script.library.utils;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class converter extends script.base_script
{
    public String GJPUD_ITEM = "item_gjpud_scrap_heap";
    public String GJPUD_REWARD = "item_event_token_01_01";
    public int GJPUD_TURN_IN_COUNT = 10;

    public int OnAttach(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int sync(obj_id self)
    {
        setName(self, "GJPUD Scrapper");
        setDescriptionString(self, "This recycler converts Scrap Heaps into Event Tokens, a currency which can be used to redeem Trading Card Game rewards.");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        if (!getEncodedName(self).equals("GJPUD Scrapper"))
        {
            sync(self);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Recycle Scrap Heaps"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            if (getScrapHeaps(player) == SCRIPT_CONTINUE)
            {
                broadcast(player, "Transaction successful.");
            }
            else
            {
                LOG("ethereal", "[GJPUD v2]: Converted returned a bad integer.");
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int getScrapHeaps(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        if (!isIdValid(pInv))
        {
            return SCRIPT_CONTINUE;
        }
        if (getFilledVolume(pInv) >= getTotalVolume(pInv))
        {
            broadcast(player, "You do not have enough space in your inventory to recycle scrap heaps.");
            return SCRIPT_CONTINUE;
        }
        obj_id[] contents = getContents(pInv);
        if (contents == null)
        {
            broadcast(player, "Unable to process transaction.");
        }
        if (contents != null)
        {
            for (obj_id solo : contents)
            {
                if (static_item.isStaticItem(solo))
                {
                    if (static_item.getStaticItemName(solo).equals(GJPUD_ITEM))
                    {
                        if (getCount(solo) >= GJPUD_TURN_IN_COUNT)
                        {
                            setCount(solo, getCount(solo) - GJPUD_TURN_IN_COUNT);
                            static_item.createNewItemFunction(GJPUD_REWARD, pInv);
                            broadcast(player, "You have succesfully recycled 5 scrap heaps for an Event Token.");
                        }
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
}
