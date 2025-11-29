package script.content.worldboss;/*
@Origin: dsrc.script.theme_park.world_boss.master_controller_mandalorian_crusader
@Author: BubbaJoeX
@Purpose: Handles the world boss upon spawning, combat ratios and death.
@Notes;
    The Mandalorian Crusader is a Mandalorian world boss that spawns near the Death Watch Bunker on Endor
    The Mandalorian Crusader has a variety of mechanics that make him a challenging fight.
        1. The Mandalorian Crusader will speak a spam message.
        2. The Mandalorian Crusader will disarm players of their weapons.
        3. The Mandalorian Crusader will bombard players, causing massive damage.
        4. The Mandalorian Crusader will increase his focus, causing players to enrage.
        5. The Mandalorian Crusader will enter his last stand, calling upon the full power of the Mandalorian Crusader.
        6. The Mandalorian Crusader will say a final flavor message before death
@Requirements: script.player.player_nb, script.library.nb_player
@Created: Sunday, 2/01/2023, at 11:42 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.obj_id;
import script.string_id;

public class c_crusader extends script.base_script
{
    public static final String VOLUME_NAME = "aggressive_area";
    public String[] MAND_MSGS = {
            "Cowards!",
            "You will not escape me!",
            "I will not be defeated!",
            "I will not be stopped!",
            "I will not be denied!",
            "I will not be ignored!",
            "I will not be forgotten!",
            "I will not be defeated!",
            "I am Mand'alor!",
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "The Crusader");
        setDescriptionStringId(self, string_id.unlocalized("A powerful rogue Mandalorian."));
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.pax"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.pax");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.pax", "Active");
        titan_utils.markAsEventSpawn(self);
        titan_player.doWorldBossAnnounce(self, titan_player.WORLD_BOSS_PAX);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[World Boss System]: Crusader has been killed.");
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        if (isGod(killer))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.pax"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.pax");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.pax", "Inactive");
        titan_player.doWorldBossDeathMsg(self, killer);
        return SCRIPT_CONTINUE;
    }

    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id wpn, int[] damage) throws InterruptedException
    {
        obj_id[] players = getPlayerCreaturesInRange(self, 64.0f);
        int health = getHealth(self);
        int maxHealth = getMaxHealth(self);
        int percentHealth = (health * 100) / maxHealth;
        if (attacker == self) //this is a self damage check
        {
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(self, "chirp"))
        {
            chat.chat(self, MAND_MSGS[rand(0, MAND_MSGS.length - 1)]);
            utils.setScriptVar(self, "chirp", 1);
        }
        if (percentHealth <= 75)
        {
            if (!utils.hasScriptVar(self, "hasSpawned"))
            {
                chat.chat(self, "I will not be challenged in such uncivil ways!");
                for (obj_id who : players)
                {
                    broadcast(who, "The crusader has lost his enhancements. Strike now!");
                }
                buff.removeAllBuffs(self);
                utils.setScriptVar(self, "hasSpawned", 1);
                return SCRIPT_CONTINUE;
            }
        }
        if (percentHealth <= 50)
        {
            if (!utils.hasScriptVar(self, "hasBeenBombed"))
            {
                chat.chat(self, MAND_MSGS[rand(0, MAND_MSGS.length - 1)]);
                chat.chat(self, "Eat Durasteel!");
                bombard(self, players);
                utils.setScriptVar(self, "hasBeenBombed", 1);
            }
        }
        if (percentHealth <= 20)
        {
            if (!utils.hasScriptVar(self, "hasDisarmed"))
            {
                chat.chat(self, MAND_MSGS[rand(0, MAND_MSGS.length - 1)]);
                for (obj_id who : players)
                {
                    broadcast(who, "The most recent attack from " + getFirstName(attacker) + " has enraged the crusader, causing him to increase his focus.");
                }
                utils.setScriptVar(self, "hasDisarmed", 1);
            }
        }
        if (percentHealth <= 8)
        {
            if (!utils.hasScriptVar(self, "hasLastStand"))
            {
                buff.removeAllBuffs(self);
                for (obj_id who : players)
                {
                    broadcast(who, "The crusader has entered his last stand!");
                }
                chat.chat(self, "This Is The Way.");
                buff.applyBuff(self, "crystal_buff", 30, 10);
                utils.setScriptVar(self, "hasLastStand", 1);
            }
        }
        if (percentHealth <= 1)
        {
            if (!utils.hasScriptVar(self, "lastMandMsg"))
            {
                chat.chat(self, MAND_MSGS[(MAND_MSGS.length - 1)]);
                utils.setScriptVar(self, "lastMandMsg", 1);
            }
        }
        else
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public void stunPlayers(obj_id self, obj_id[] targets) throws InterruptedException
    {
        playClientEffectObj(targets, "clienteffect/cr_bodyfall_huge.cef", self, "");
        if (targets == null)
        {
            return;
        }
        for (obj_id iTarget : targets)
        {
            broadcast(iTarget, "The crusader attempts to bring you to your knees!");
            chat.chat(self, "Now, witness the power of a TRUE Mandalorian!");
            faceTo(iTarget, self);
        }
    }

    public void bombard(obj_id self, obj_id[] targets) throws InterruptedException
    {
        if (targets == null)
        {
            return;
        }
        for (obj_id iTarget : targets)
        {
            playClientEffectObj(iTarget, "clienteffect/avatar_explosion_02.cef", iTarget, "");
            reduceHealth(iTarget, rand(3200, 5000));
            reduceAction(iTarget, rand(3200, 5000));
        }
    }

    public boolean reduceHealth(obj_id player, int amt)
    {
        return setAttrib(player, HEALTH, getMaxAttrib(player, HEALTH) - amt);
    }

    public boolean reduceAction(obj_id player, int amt)
    {
        return setAttrib(player, ACTION, getMaxAttrib(player, ACTION) - amt);
    }

    public int aiCorpsePrepared(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id landedDeathBlow = getObjIdObjVar(self, xp.VAR_LANDED_DEATHBLOW);
        obj_id[] players = getPlayerCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
        String jediElderSignal = "trial_of_the_elders_wb";
        String jediElderQuest = "quest/trial_of_the_elder";
        String jediElderTaskName = "slayer_world_boss";
        for (obj_id player : players)
        {
            if (isIdValid(player))
            {
                if (utils.getPlayerProfession(player) == utils.FORCE_SENSITIVE)
                {
                    if (groundquests.isQuestActive(player, jediElderQuest) && groundquests.isTaskActive(player, jediElderQuest, jediElderTaskName))
                    {
                        groundquests.sendSignal(player, jediElderSignal);
                    }
                }
            }
        }
        obj_id[] finalList = station_lib.processPlayerListAndRemoveDuplicates(players);
        for (obj_id player : finalList)
        {
            if (isIdValid(player))
            {
                obj_id token = static_item.createNewItemFunction("item_world_boss_token_01_01", utils.getInventoryContainer(player));
                if (isIdValid(token))
                {
                    int multiplier = utils.getIntBonusValue("wb");
                    int newCount = 5 * multiplier;
                    setCount(token, newCount);
                    sendSystemMessage(player, "You have received " + color("DAA520", String.valueOf(newCount)) + " World Boss Tokens.", null);
                }
                showLootBox(player, new obj_id[]{token});
                if (!hasObjVar(player, "wb.mando"))
                {
                    setObjVar(player, "wb.mando", 1);
                }
                else
                {
                    int count = getIntObjVar(player, "wb.mando");
                    if (count >= 10 && !hasObjVar(player, "wb.mando_title"))
                    {
                        broadcast(player, "You have received the title " + color("DAA520", "The Crusader's Bane") + " for killing The Crusader 10 times!");
                        grantSkill(player, "title_world_boss_crusader");
                        setObjVar(player, "wb.mando_title", 1);
                    }
                    else
                    {
                        setObjVar(player, "wb.mando", count + 1);
                    }
                }
            }
            obj_id randomPlayer = players[rand(0, players.length - 1)];
            if (isIdValid(randomPlayer))
            {
                obj_id inv = utils.getInventoryContainer(randomPlayer);
                broadcast(randomPlayer, "You have found some interesting items off of The Crusader...");
                create.createObject("object/tangible/loot/dungeon/death_watch_bunker/binary_liquid.iff", inv, "");
                create.createObject("object/tangible/loot/dungeon/death_watch_bunker/emulsion_protection.iff", inv, "");
            }
            else
            {
                LOG("ethereal", "[World Boss System]: (The Crusader) No valid ID for randomly selected player. Not handing out themed loot.");
            }

        }
        if (isIdValid(landedDeathBlow))
        {
            obj_id victorInv = utils.getInventoryContainer(landedDeathBlow);
            static_item.createNewItemFunction("item_world_boss_token_01_01", victorInv, 5);
            broadcast(landedDeathBlow, "You have received " + color("DAA520", "5") + " World Boss Tokens for landing the final blow!");
        }
        else
        {
            LOG("ethereal", "[World Boss System]: (The Crusader) No valid ID for deathblower. Not handing out bonus tokens.");
        }
        int random = rand(1, 100);
        if (random >= 75)
        {
            static_item.createNewItemFunction("item_content_bunker_house_02", utils.getInventoryContainer(self));
            LOG("ethereal", "[World Boss System]: (The Crusader) has dropped a bunker house. Giving to the corpse.");
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public String color(String color, String text) throws InterruptedException
    {
        return "\\#" + color + text + "\\#.";
    }
}
