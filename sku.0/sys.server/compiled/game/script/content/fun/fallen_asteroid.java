package script.content.fun;/*
@Origin: dsrc.script.content.fun
@Author:  BubbaJoeX
@Purpose: Allows players to mine ore from a fallen asteroid
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Tuesday, 2/25/2025, at 7:43 PM, 
@Copyright © SWG: New Beginnings 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.utils;

import static script.library.sui.*;

public class fallen_asteroid extends base_script
{

    public static final boolean LOGGING = false;
    private static final int REQUIRED_LEVEL = 10;
    private static final String MINING_TIMER = "mining";
    private static final int MINING_DURATION = 15;
    private static final String MINING_TITLE = "Mining Asteroid";
    private static final String MINING_PROMPT = "Time remaining to complete mining: ";

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
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }

        if (getLevel(player) >= REQUIRED_LEVEL)
        {
            mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Mine Asteroid"));
        }
        else
        {
            sendSystemMessage(player, "You must be at least level " + REQUIRED_LEVEL + " to mine this asteroid.", "");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE || !isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }

        if (getLevel(player) < REQUIRED_LEVEL)
        {
            sendSystemMessage(player, "You must be at least level " + REQUIRED_LEVEL + " to mine this asteroid.", "");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(player, MINING_TIMER))
        {
            sendSystemMessage(player, "You are already mining the asteroid.", "");
            return SCRIPT_CONTINUE;
        }

        doAnimationAction(player, "manipulate_low");
        setObjVar(player, MINING_TIMER, getGameTime());

        int flags = CD_EVENT_NONE;
        flags |= CD_EVENT_COMBAT;
        flags |= CD_EVENT_LOCOMOTION;
        flags |= CD_EVENT_INCAPACITATE;
        int pid = smartCountdownTimerSUI(self, player, "quest_countdown_timer", string_id.unlocalized(MINING_TITLE), 0, 15, "handleMiningComplete", 15.0f, flags);
        if (pid > -1)
        {
            dictionary params = new dictionary();
            params.put("player", player);
            params.put("pid", pid);
            messageTo(self, "handleMiningComplete", params, MINING_DURATION, false);
            sendSystemMessage(player, "You begin mining the asteroid...", "");
        }

        return SCRIPT_CONTINUE;
    }

    public int handleMiningComplete(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        if (!isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }

        int pid = params.getInt("pid");
        forceCloseSUIPage(pid);
        removeObjVar(player, MINING_TIMER);
        grantResources(player);
        broadcast(player, "You've successfully mined resources from the asteroid!");
        return SCRIPT_CONTINUE;
    }

    private void grantResources(obj_id player) throws InterruptedException
    {
        obj_id resourceType = getResourceTypeByName("asteroid");
        createResourceCrate(resourceType, rand(240, 630), utils.getInventoryContainer(player));
        blog("Granting asteroid resource to player: " + player);
    }

    public void blog(String msg)
    {
        if (LOGGING)
        {
            LOG("ethereal", "[fallen_asteroid]: " + msg);
        }
    }
}
