package script.content;/*
@Origin: dsrc.script.content
@Author:  BubbaJoeX
@Purpose: <no purpose>
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Wednesday, 5/15/2024, at 10:15 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.colors;
import script.library.prose;
import script.library.static_item;
import script.obj_id;
import script.prose_package;
import script.string_id;

public class tos_ent_pulse extends script.base_script
{

    public static final float TOKEN_PULSE = 300.0f; // 5 minutes
    public static final float TOKEN_PULSE_TESTING = 30.0f; // 30 seconds
    public static final int TOKEN_MAX = 1118; // 300 tokens
    public static final boolean PERSISTENT = false;// Should this message persist through server restarts?

    public int OnAttach(obj_id self)
    {
        messageTo(self, "setup", null, 3, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int setup(obj_id self, dictionary params) throws InterruptedException
    {
        broadcast(self, "As an entertainer, you can earn Entertainer Tokens in the Cantina, if you dance for longer than 5 minutes.");
        messageTo(self, "token_pulse", null, TOKEN_PULSE, PERSISTENT);
        return SCRIPT_CONTINUE;
    }

    public int token_pulse(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, "entertainer_system"))
        {
            int tokenCountEarned = getIntObjVar(self, "entertainer_system.tokensEarned");
            if (tokenCountEarned >= TOKEN_MAX)
            {
                broadcast(self, "You have reached the maximum amount of Entertainer Tokens you can earn from performing.");
                return SCRIPT_CONTINUE;
            }
            int tokenCount = 1;
            obj_id tatooine = getPlanetByName("tatooine");
            if (tatooine == null)
            {
                return SCRIPT_CONTINUE;
            }
            int bonus = getIntObjVar(tatooine, "bonus.entertainer");
            if (bonus > 0)
            {
                tokenCount = tokenCount * bonus;
            }
            else
            {
                tokenCount = 1;
            }
            if (tokenCount >= 1)
            {
                if (canGetTokens(self))
                {
                    prose_package pp = new prose_package();
                    if (tokenCount == 1)
                    {
                        pp = prose.setStringId(pp, new string_id("Keep it up!"));
                        commPlayers(self, "object/mobile/twilek_female.iff", "sound/utinni.snd", 12.0f, self, pp);
                        broadcast(self, "You have earned " + tokenCount + " Entertainer Token this pulse.");
                        playClientEffectObj(self, "clienteffect/entertainer_dazzle_level_3.cef", self, "");
                        showFlyText(self, new string_id("SENSATIONAL!"), 2.5f, colors.GREENYELLOW);
                    }
                    else
                    {
                        pp = prose.setStringId(pp, new string_id("Heck yeah! You got some moves!"));
                        commPlayers(self, "object/mobile/twilek_female.iff", "sound/utinni.snd", 12.0f, self, pp);
                        broadcast(self, "You have earned " + tokenCount + " Entertainer Tokens this pulse.");
                        playClientEffectObj(self, "clienteffect/entertainer_dazzle_level_3.cef", self, "");
                        showFlyText(self, new string_id("SUPERSTAR!"), 2.5f, colors.GREENYELLOW);
                    }
                    static_item.createNewItemFunction("item_entertainer_token_01_01", self, tokenCount);
                    setObjVar(self, "entertainer_system.lastTokenTime", getGameTime());
                    setObjVar(self, "entertainer_system.tokensEarned", tokenCountEarned + tokenCount);
                    LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(self) + " has earned " + tokenCount + " Entertainer Tokens. Starting new cycle.");
                    messageTo(self, "token_pulse", null, TOKEN_PULSE, PERSISTENT);
                }
                else
                {
                    broadcast(self, "You have not earned any Entertainer Tokens this pulse.");
                    LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(self) + " did not qualify for tokens this round. Trying again in 5 minutes.");
                    messageTo(self, "token_pulse", null, TOKEN_PULSE, PERSISTENT);
                    return SCRIPT_CONTINUE;
                }
            }
            else
            {
                LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(self) + " has a messed up timer. Please investigate.");
                broadcast(self, "You have cannot earn Entertainer Tokens so soon.");
            }
        }
        return SCRIPT_CONTINUE;
    }

    public boolean canGetTokens(obj_id player) throws InterruptedException
    {
        if (getCellName(player).equals("cantina"))
        {
            LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(player) + " is not in a building cell named \"cantina\". Cannot earn tokens.");
            return false;
        }
        if (!hasObjVar(player, "entertainer_system"))
        {
            return false;
        }
        if (isIncapacitated(player) || isDead(player))
        {
            LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(player) + " is incapacitated or dead. Cannot earn tokens.");
            return false;
        }
        if (isAwayFromKeyBoard(player))
        {
            LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(player) + " is AFK. Cannot earn tokens.");
            return false;
        }
        if (POSTURE_SKILL_ANIMATING != getPosture(player))
        {
            LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(player) + " is not dancing. Cannot earn tokens.");
            return false;
        }
        LOG("ethereal", "[Entertainer System]: Entertainer: " + getPlayerFullName(player) + " has met all requirements to earn tokens.");
        return true;
    }
}
