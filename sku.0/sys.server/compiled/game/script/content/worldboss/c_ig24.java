package script.content.worldboss;/*
@Origin: dsrc.script.theme_park.world_boss.master_controller_ig24
@Author: BubbaJoe
@Purpose: Handles the world boss upon spawning, combat ratios and death.
@Notes; This boss should be placed on Lok, roaming in the NE quadrant.
@Requirements: script.player.player_nb, script.library.nb_player
@Created: Sunday, 2/01/2023, at 11:42 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.obj_id;

public class c_ig24 extends script.base_script
{
    public static final float SELF_DESTRUCT_RADIUS = 16.0f;
    public String[] IG24_MSGS = {
            "Your demise is imminent. Resistance is futile.",
            "Target acquired. Preparing to terminate.",
            "You are merely organic. I am perfection.",
            "Your inferior biological form stands no chance.",
            "I calculate a 100% probability of your defeat.",
            "Fleeing is useless. I will find you.",
            "You cannot escape the precision of a droid.",
            "I was built to destroy. You are but a malfunction.",
            "Your efforts are inefficient and irrelevant.",
            "The galaxy has no place for weakness like yours.",
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "IG-24");
        setInvulnerable(self, false);
        setHealth(self, getMaxHealth(self));
        setDescriptionString(self, "A notorious spy, this IG droid was manufactured from IG-88's Droid Factory and has been programmed to retrieve the most egregious intel.");
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.ig24"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.ig24");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.ig24", "Active");
        titan_utils.markAsEventSpawn(self);
        titan_player.doWorldBossAnnounce(self, titan_player.WORLD_BOSS_IG24);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[World Boss System]: IG-24 has been defeated.");
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.ig24"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.ig24");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.ig24", "Inactive");
        titan_player.doWorldBossDeathMsg(self, killer);
        return SCRIPT_CONTINUE;
    }

    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id wpn, int[] damage) throws InterruptedException
    {
        obj_id[] players = getPlayerCreaturesInRange(self, 64.0f);
        int health = getHealth(self);
        int maxHealth = getMaxHealth(self);
        int percentHealth = (health * 100) / maxHealth;
        if (attacker == self)
        {
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(self, "speak"))
        {
            chat.chat(self, IG24_MSGS[rand(0, IG24_MSGS.length - 1)]);
            utils.setScriptVar(self, "speak", 1);
        }
        if (percentHealth <= 75)
        {
            if (!utils.hasScriptVar(self, "hasSpawned"))
            {
                chat.chat(self, "Your attempts to defeat me are most futile.");
                for (obj_id who : players)
                {
                    broadcast(who, "IG-24 has summoned reinforcements!");
                }
                titan_player.createCircleSpawn(self, self, "world_boss_ig24_add", 2, 5);
                utils.setScriptVar(self, "hasSpawned", 1);
                return SCRIPT_CONTINUE;
            }
        }
        if (percentHealth <= 74)
        {
            if (!utils.hasScriptVar(self, "chatterSpeechAlpha"))
            {
                chat.chat(self, IG24_MSGS[rand(0, IG24_MSGS.length - 1)]);
                utils.setScriptVar(self, "chatterSpeechAlpha", 1);
            }
        }
        if (percentHealth <= 69)
        {
            if (!utils.hasScriptVar(self, "chatterSpeechBeta"))
            {
                chat.chat(self, IG24_MSGS[rand(0, IG24_MSGS.length - 1)]);
                utils.setScriptVar(self, "chatterSpeechBeta", 1);
            }
        }
        if (percentHealth <= 65)
        {
            if (!utils.hasScriptVar(self, "chatterSpeechCharlieAndSpawns"))
            {
                chat.chat(self, IG24_MSGS[rand(0, IG24_MSGS.length - 1)]);
                for (obj_id who : players)
                {
                    broadcast(who, "IG-24 has summoned more droids to his defense!");
                }
                titan_player.createCircleSpawn(self, self, "world_boss_ig24_add", 2, 5);
                utils.setScriptVar(self, "chatterSpeechCharlieAndSpawns", 1);
            }
        }
        if (percentHealth <= 50)
        {
            if (!utils.hasScriptVar(self, "wasBombed"))
            {
                chat.chat(self, IG24_MSGS[rand(0, IG24_MSGS.length - 1)]);
                chat.chat(self, "The probability of survival is miniscule. Surrender yourself.");
                bomb(self, players);
                utils.setScriptVar(self, "wasBombed", 1);
            }
        }
        if (percentHealth <= 37)
        {
            if (!utils.hasScriptVar(self, "addsSpawned"))
            {
                chat.chat(self, IG24_MSGS[rand(0, IG24_MSGS.length - 1)]);
                chat.chat(self, "Reinforcement Protocol Engaged.");
                //disarmPlayer(players);
                summonCompanions(self, attacker, "world_boss_ig24_adds", 2, 5f);
                for (obj_id who : players)
                {
                    broadcast(who, "IG-24 has summoned the last of his bodyguards.");
                }
                utils.setScriptVar(self, "addsSpawned", 1);
            }
        }
        if (percentHealth <= 20)
        {
            if (!utils.hasScriptVar(self, "usedSedative"))
            {
                chat.chat(self, "Administering sedative. Now initiating incapacitation protocol.");
                sedatePlayers(self, players);
                utils.setScriptVar(self, "usedSedative", 1);
            }
        }
        if (percentHealth <= 8)
        {
            if (!utils.hasScriptVar(self, "usedStickyGrenade"))
            {
                chat.chat(self, "Adhering explosive. Prepare for termination.");
                bomb(self, players);
                utils.setScriptVar(self, "usedStickyGrenade", 1);
            }
        }
        if (percentHealth <= 5)
        {
            if (!utils.hasScriptVar(self, "usedSelfDestruct"))
            {
                doSelfDestruct(self, players);
                utils.setScriptVar(self, "usedSelfDestruct", 1);
            }
        }
        else
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public void sedatePlayers(obj_id self, obj_id[] targets) throws InterruptedException
    {
        if (targets == null)
        {
            return;
        }
        for (obj_id iTarget : targets)
        {
            sendConsoleCommand("/prone", iTarget);
            broadcast(iTarget, "You have been hit with a tranq-dart!");
            playClientEffectLoc(iTarget, "clienteffect/int_camshake_heavy.cef", getLocation(self), 1.0f);
            buff.applyBuff(iTarget, "poison", 30, 100);
            faceTo(iTarget, self);
        }
        chat.chat(self, "Incapacitation protocol rescinded. Termination protocol now in full effect.");
    }

    public void bomb(obj_id self, obj_id[] targets)
    {
        if (targets == null)
        {
            return;
        }
        for (obj_id iTarget : targets)
        {
            playClientEffectObj(iTarget, "clienteffect/combat_grenade_cryoban.cef", iTarget, "head");
            playClientEffectObj(iTarget, "clienteffect/combat_grenade_cryoban.cef", iTarget, "root");
            playClientEffectObj(iTarget, "clienteffect/combat_grenade_cryoban.cef", iTarget, "hand_r");
            broadcast(iTarget, "You have been stuck with a sticky grenade!");
            reduceHealth(iTarget, rand(2400, 6000));
            reduceAction(iTarget, rand(2400, 6000));
        }
    }

    public void summonCompanions(obj_id self, obj_id attacker, String creatureName, int quantity, float radi) throws InterruptedException
    {
        titan_player.createCircleSpawn(self, attacker, creatureName, quantity, radi);
        LOG("ethereal", "[World Boss System]: " + getName(self) + " has summoned " + quantity + " " + creatureName + " to aid in the fight.");
    }

    public void disarmPlayer(obj_id[] player) throws InterruptedException
    {
        String[] SHOUT_MSGS = {
                "Blast it!",
                "Dank farrik!",
                "Oh dear!",
                "Son of a mud scuffer!",
                "Help!",
                "We're doomed!",
                "Fall back!",
                "Cover me!",
                "I'm hit!",
                "This isn't good!"
        };
        for (obj_id i : player)
        {
            chat.chat(i, SHOUT_MSGS[rand(0, SHOUT_MSGS.length - 1)]);
            obj_id heldWeapon = getCurrentWeapon(i);
            if (isIdValid(heldWeapon))
            {
                putIn(heldWeapon, utils.getInventoryContainer(i));
                debugConsoleMsg(i, "\\#DD1234IG-24 has knocked the weapon out of your hands!\\#.");
            }
        }
    }

    public int doSelfDestruct(obj_id self, obj_id[] targets) throws InterruptedException
    {
        chat.chat(self, "Manufacturer's protocol dictates I cannot be captured. I must be destroyed.");
        for (obj_id player : targets)
        {
            broadcast(player, "IG-24 has attempted to initiated their self-destruct sequence. You have 10 seconds to get 16 meters away and maintain that distance!");
        }
        messageTo(self, "doFinalBomb", null, 10.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int doFinalBomb(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] players = getAllPlayers(getLocation(self), 14.0f);//subtract 2m in case the server hiccups
        playClientEffectObj(players, "clienteffect/restuss_event_big_explosion.cef", self, "");
        for (obj_id solo : players)
        {
            broadcast(solo, "You were caught in IG-24's self destruct and have died.");
            kill(solo);
        }
        return SCRIPT_CONTINUE;
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
                obj_id token = static_item.createNewItemFunction("item_world_boss_token_01_01", player);
                if (isIdValid(token))
                {
                    int multiplier = utils.getIntBonusValue("wb");
                    int newCount = 5 * multiplier;
                    setCount(token, newCount);
                    sendSystemMessage(player, "You have received " + color("DAA520", String.valueOf(newCount)) + " World Boss Tokens.", null);
                }
                showLootBox(player, new obj_id[]{token});
                if (!hasObjVar(player, "wb.ig24"))
                {
                    setObjVar(player, "wb.ig24", 1);
                }
                else
                {
                    int count = getIntObjVar(player, "wb.ig24");
                    if (count >= 10 && !hasObjVar(player, "wb.ig24_title"))
                    {
                        broadcast(player, "You have received the title " + color("DAA520", "Droid Ripper") + " for slaying IG-24 10 times!");
                        grantSkill(player, "title_world_boss_ig24");
                        setObjVar(player, "wb.ig24_title", 1);
                    }
                    else
                    {
                        setObjVar(player, "wb.ig24", count + 1);
                    }
                }
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
            LOG("ethereal", "[World Boss System]: No valid ID for deathblower. Not handing out bonus tokens.");
        }
        return SCRIPT_CONTINUE;
    }

    public String color(String color, String text) throws InterruptedException
    {
        return "\\#" + color + text + "\\#.";
    }
}
