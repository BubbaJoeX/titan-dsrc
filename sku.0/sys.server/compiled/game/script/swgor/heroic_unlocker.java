package script.swgor;/*
@Origin: dsrc.script.swgor
@Author:  BubbaJoeX
@Purpose: <no purpose>
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Friday, 2/14/2025, at 12:29 PM,
@Copyright © SWG - OR 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.groundquests;
import script.library.instance;
import script.library.sui;
import script.library.utils;

public class heroic_unlocker extends script.base_script
{

    public static final int MIN_LEVEL = 85;
    String[] HEROIC_FLAGS = {
            "heroic_exar_kun",
            "heroic_star_destroyer",
            "heroic_tusken_army",
            "heroic_axkva_min",
            "heroic_ig88",
            "echo_base"
    };

    public int OnAttach(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int sync(obj_id self)
    {
        setObjVar(self, "noTrade", 1);
        setObjVar(self, "noTradeShared", 1);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (canManipulate(player, self, false, true, 4f, false))
        {
            if (!utils.isNestedWithinAPlayer(self))
            {
                return SCRIPT_CONTINUE;
            }
            if (hasObjVar(player, "heroic_boost") && !isGod(player))
            {
                broadcast(player, "You have already used an unlock device and may not use another.");
                return SCRIPT_CONTINUE;
            }
            if ((getLevel(player) < MIN_LEVEL) && (!isGod(player)))
            {
                broadcast(player, "You must be level " + MIN_LEVEL + "  to unlock Heroics.");
                return SCRIPT_CONTINUE;
            }
            mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Unlock Heroic Instances"));
        }
        else
        {
            broadcast(player, "This object does not interest you.");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            if (utils.isNestedWithinAPlayer(self))
            {
                unlockHeroics(player);
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_CONTINUE;
    }

    public void unlockHeroics(obj_id player) throws InterruptedException
    {
        setObjVar(player, "towship.metTravel", true);
        flagInstanceAndNotify(player, "heroic_axkva_min", "Flagged: Axkva Min");
        flagInstanceAndNotify(player, "heroic_exar_kun", "Flagged: Exar Kun");
        flagInstanceAndNotify(player, "echo_base", "Flagged: Echo Base");
        flagInstanceAndNotify(player, "heroic_ig88", "Flagged: IG-88");
        flagInstanceAndNotify(player, "heroic_star_destroyer", "Flagged: Lost Star Destroyer");
        flagInstanceAndNotify(player, "heroic_tusken_army", "Flagged: Tusken Army");

        sui.msgbox(player, player,
                "Congratulations! You have been granted access to all Heroic Instances for this character.",
                sui.OK_ONLY, "SWG - OR", "noHandler"
        );
        playClientEffectObj(player, "clienteffect/level_granted_chronicles.cef", player, "");
        setObjVar(player, "heroic_boost", true);
        destroyObject(getSelf());
    }

    private void completeQuestAndNotify(obj_id player, String quest, String message) throws InterruptedException
    {
        groundquests.completeQuest(player, quest);
        sendConsoleMessage(player, message);
    }

    private void flagInstanceAndNotify(obj_id player, String instanceName, String message) throws InterruptedException
    {
        instance.flagPlayerForInstance(player, instanceName);
        //append the message to turn light blue
        message = "\\#cceeff" + message + "\\#.";
        sendConsoleMessage(player, message);
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx != -1)
        {
            names[idx] = utils.packStringId(new string_id("Unlock Heroic Instances"));
            attribs[idx] = canUnlock(player) ? "\\#00ff00Yes\\#." : "\\#ff0000No\\#.";
            idx++;
            String[] instanceNames = {
                    "Exar Kun", "Lost Star Destroyer", "Tusken Army",
                    "Axkva Min", "IG-88", "Echo Base"
            };
            for (int i = 0; i < HEROIC_FLAGS.length; i++)
            {
                names[idx] = utils.packStringId(new string_id(instanceNames[i]));
                attribs[idx] = instance.isFlaggedForInstance(player, HEROIC_FLAGS[i]) ? "\\#00ff00Yes\\#." : "\\#ff0000No\\#.";
                idx++;
            }
        }
        return SCRIPT_CONTINUE;
    }

    private boolean canUnlock(obj_id player)
    {
        if (hasObjVar(player, "heroic_boost"))
        {
            return false;
        }
        return getLevel(player) >= MIN_LEVEL;
    }
}
