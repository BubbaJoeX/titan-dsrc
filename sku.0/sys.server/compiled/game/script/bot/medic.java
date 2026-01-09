package script.bot;/*
@Origin: script.bot.medic
@Author: BubbaJoeX
@Purpose: Low-grade Medical Buffs.
@Requirements: None
@Note: This does not replace high-grade medic buffs. The values below should fall well under base medic buffs with no jewelry or buffs.
@Copyright © SWG: Titan 2025
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.ai_lib;
import script.library.buff;
import script.library.combat;
import script.library.consts;

public class medic extends base_script
{
    public int OnInitialize(obj_id self)//persisted load
    {
        reinitialize(self);
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self)//dynamic/one time load
    {
        reinitialize(self);
        persistObject(self);
        return SCRIPT_CONTINUE;
    }

    public int reinitialize(obj_id self)
    {
        removeTriggerVolume("healDroid");
        createTriggerVolume("healDroid", consts.AI_SERVICE_RANGE, true);
        setName(self, "FX-7 Medical Droid");
        setDescriptionStringId(self, string_id.unlocalized("This FX-7 medical droid is a prototype model. It is designed to stay stationary, while healing and enhancing all those who are nearby."));
        return SCRIPT_CONTINUE;
    }

    public int OnTriggerVolumeEntered(obj_id self, String volumeName, obj_id breacher) throws InterruptedException
    {
        if (!isPlayer(breacher))
        {
            return SCRIPT_CONTINUE;
        }
        if (combat.isInCombat(breacher))
        {
            broadcast(breacher, "This service droid cannot assist those engaged in combat.");
            return SCRIPT_CONTINUE;
        }
        if (isInvulnerable(breacher))
        {
            broadcast(breacher, "This service droid cannot assist those who are invulnerable.");
            return SCRIPT_CONTINUE;
        }
        if (volumeName.equals("healDroid"))
        {
            int maxHealth = getMaxAttrib(breacher, HEALTH);
            int maxAction = getMaxAttrib(breacher, ACTION);
            setHealth(breacher, maxHealth);
            setAction(breacher, maxAction);
            int cooldownTime = 300;
            int buffTime = 3600;
            if (hasObjVar(breacher, "healDroid.cooldown"))
            {
                int cooldown = getIntObjVar(breacher, "healDroid.cooldown");
                if (getGameTime() < cooldown)
                {
                    broadcast(breacher, "You must wait " + ((cooldown - getGameTime()) / 60) + " minutes before receiving another enhancement of this caliber.");
                    return SCRIPT_CONTINUE;
                }
            }
            ai_lib.aiStopFollowing(self);
            stop(self);
            faceToBehavior(self, breacher);
            mapped_sort<String, Integer> buffs = new mapped_sort<>();
            buffs.put("me_buff_health_2", 225);
            buffs.put("me_buff_action_3", 225);
            buffs.put("me_buff_strength_3", 50);
            buffs.put("me_buff_agility_3", 50);
            buffs.put("me_buff_precision_3", 50);
            buffs.put("me_buff_melee_gb_1", 5);
            buffs.put("me_buff_ranged_gb_1", 5);
            for (String key : buffs.keySet())
            {
                int value = buffs.get(key);
                buff.applyBuff(breacher, key, (float) buffTime, value);
                showFlyText(self, new string_id("+"), 2.0f, color.GREEN);
            }
            setObjVar(breacher, "healDroid.cooldown", getGameTime() + cooldownTime);
            showFlyText(self, new string_id("- DIRECTIVE RECEIVED -"), 2.0f, color.RED);
            showFlyText(breacher, new string_id("- ENHANCED -"), 2.0f, color.GOLDENROD);
            LOG("ethereal", "[Medical Droid]: Player " + getPlayerFullName(breacher) + " has been enhanced with a full medic buff set. Values set to [225, 225, 50, 50, 50, 5, 2].");
        }
        return SCRIPT_CONTINUE;
    }

    public String convertToBinary(String text)
    {
        StringBuilder binary = new StringBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            binary.append(Integer.toBinaryString(text.charAt(i))).append(" ");
        }
        return binary.toString();
    }

}
