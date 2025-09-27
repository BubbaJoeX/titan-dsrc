package script.developer.bubbajoe;/*
@Origin: script.developer.bubbajoe.bday_gift
@Author: BubbaJoeX
@Purpose: Cake slice script, awards collection items, and if used again within 2 hours, grants a burst run buff.
@Requirements: script.developer.bubbajoe.sync
@Origin: dsrc.script.developer.bubbajoe.bday_gift
@Created: Saturday, 3/30/2023, at 7:12 PM
*/

/*
 * Copyright © SWG-OR 2024.
 *
 * Unauthorized usage, viewing or sharing of this file is prohibited.
 */

import script.library.buff;
import script.library.collection;
import script.library.utils;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class bday_gift extends script.base_script
{
    private static final int BDAY_BURST_DUR = 14400;
    private static final int BDAY_BURST_VAL = 83;
    public static int COOLDOWN_TIME = 14400 * 2;
    public static String NULL_DESC_PREFIX = "Cut from the most beautiful cake Master Abbub has ever made, this tasty slice will make you feel all cozy inside. \n\n\\#7FFFD4Happy Birthday, ";

    private void setupSlice(obj_id self)
    {
        setObjVar(self, "used.timestamp", getGameTime() - COOLDOWN_TIME);
        setName(self, "Slice of Birthday Cake");
        if (hasScript(self, "item.food"))
        {
            detachScript(self, "item.food");
        }
        if (!hasScript(self, "item.special.nomove"))
        {
            attachScript(self, "item.special.nomove");
        }
        if (!hasObjVar(self, "noTrade"))
        {
            setObjVar(self, "noTrade", 1);
        }
    }

    public int OnAttach(obj_id self)
    {
        setupSlice(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        setupSlice(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, "bday_gift.consumed"))
        {
            int mainMenu = mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Eat Slice"));
        }
        else if (hasObjVar(self, "bday_gift.show_admire_menu"))
        {
            int secondaryMenu = mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Go For Seconds"));
        }
        sendDirtyObjectMenuNotification(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            if (!hasObjVar(self, "bday_gift.consumed"))
            {
                int amount = rand(5, 8);
                for (int i = 0; i < amount; i++)
                {
                    obj_id collectionItem = collection.grantRandomCollectionItem(player, "datatables/loot/loot_items/collectible/magseal_loot.iff", "collections");
                    setObjVar(collectionItem, "collection.gameTimeGrant", getGameTime());
                }
                broadcast(player, "You have taken a few bites of this slice of cake. However, you wish to preserve this to display to your friends the most fabulous cake made by Master Abbub!");
                setDescriptionStringId(self, new string_id(NULL_DESC_PREFIX + getPlayerFullName(player) + "!"));
                setObjVar(self, "bday_gift.consumed", 1);
                setObjVar(self, "bday_gift.show_admire_menu", 1);
                setObjVar(self, "null_desc", NULL_DESC_PREFIX + "Bubba Joe!");
                attachScript(self, "developer.bubbajoe.sync");
                LOG("ethereal", "[Birthday Gift]: Player " + getPlayerFullName(player) + " has consumed a slice of cake, granting rewards.");
            }
        }
        if (item == menu_info_types.SERVER_MENU1)
        {
            if (getGameTime() > (getIntObjVar(self, "used.timestamp") + COOLDOWN_TIME))
            {
                broadcast(player, "This cake seems to be missing a few bites, regardless, you go for more.");
                buff.applyBuff(player, "burstRun", (float) BDAY_BURST_DUR, (float) BDAY_BURST_VAL);
                LOG("ethereal", "[Birthday Gift]: Player " + getPlayerFullName(player) + " has used a slice of cake.");
                setObjVar(self, "used.timestamp", getGameTime());
            }
            else
            {
                broadcast(player, "You probably should refrigerate this slice of cake so it doesn't go bad.");
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        if (hasObjVar(self, "bday_gift.consumed"))
        {
            int idx = utils.getValidAttributeIndex(names);
            int lastUsed = getIntObjVar(self, "used.timestamp");
            names[idx] = "effect";
            attribs[idx] = utils.packStringId(new string_id("\\#7FFFD4Birthday Celebration"));
            idx++;
            names[idx] = "duration";
            attribs[idx] = BDAY_BURST_DUR + "s";
            idx++;
            String NO = "\\#DD1234" + "No" + "\\#.";
            String YES = "\\#32CD32" + "Yes" + "\\#.";
            if (getGameTime() < (lastUsed + COOLDOWN_TIME))
            {
                names[idx] = "ready";
                attribs[idx] = NO;
                idx++;
            }
            else
            {
                names[idx] = "ready";
                attribs[idx] = YES;
                idx++;
            }
        }
        return SCRIPT_CONTINUE;
    }
}
