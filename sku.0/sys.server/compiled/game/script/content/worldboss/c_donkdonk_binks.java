package script.content.worldboss;/*
@Origin: dsrc.script.theme_park.world_boss.master_controller_donkdonk_binks
@Author: BubbaJoeX
@Purpose: Handles the world boss upon spawning, combat ratios and death.
@Notes;
    Donk-Donk Binks is a Gungan world boss that spawns in the Chommell Sector on Tatooine.
    Donk-Donk Binks has a variety of mechanics that make him a challenging fight.
        1. Donk-Donk Binks will speak a spam message.
        2. Donk-Donk Binks will disarm players of their weapons.
        3. Donk-Donk Binks will drug players, causing them to fall asleep.
        4. Donk-Donk Binks will call for reinforcements. (Donk-Donk Binks Add)
        5. Donk-Donk Binks will throw a booma, causing massive damage to players.
        6. Donk-Donk Binks will say a final flavor message before death.
@Requirements: script.player.player_nb, script.library.nb_player
@Created: Sunday, 2/01/2023, at 11:42 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.obj_id;
import script.string_id;

public class c_donkdonk_binks extends script.base_script
{
    public String[] GUNGAN_MSGS = {
            "Yous no escape me!",
            "Yous mula be moole!",
            "Yousa think yousa so bombad!",
            "Yous weapons will adden nicely to meesa collection!",
            "Me warned yousa!",
            "Ya-hoo yousa, stopen dat!",
            "Dat's it!",
            "No maken mesa usen a booma!",
            "Stopen da doo-doo, yousa cannot defeat mesa in combat!"
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Donk-Donk Binks");
        setDescriptionStringId(self, string_id.unlocalized("A distant cousin to Jar-Jar Binks, this criminal Gungan is notorious for causing havoc in the Chommell Sector"));
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.donkdonk_binks"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.donkdonk_binks");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.donkdonk_binks", "Active");
        titan_utils.markAsEventSpawn(self);
        titan_player.doWorldBossAnnounce(self, titan_player.WORLD_BOSS_DONKDONK);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[World Boss System]: Donk-Donk has been defeated.");
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.donkdonk_binks"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.donkdonk_binks");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.donkdonk_binks", "Inactive");
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
        if (!utils.hasScriptVar(self, "speaken"))
        {
            chat.chat(self, GUNGAN_MSGS[rand(0, GUNGAN_MSGS.length - 1)]);
            utils.setScriptVar(self, "speaken", 1);
        }
        if (percentHealth <= 75)
        {
            if (!utils.hasScriptVar(self, "hasSpawned"))
            {
                chat.chat(self, "Okie dey, you asken for it!");
                for (obj_id who : players)
                {
                    broadcast(who, "Donk-Donk Binks has called for reinforcements!");
                    create.object("world_boss_donkdonk_binks_add", getLocation(who)); //Spawn 1 add per player in range.
                }
                utils.setScriptVar(self, "hasSpawned", 1);
                return SCRIPT_CONTINUE;
            }
        }
        if (percentHealth <= 74 && percentHealth >= 72)
        {
            if (!utils.hasScriptVar(self, "chatSpam"))
            {
                chat.chat(self, GUNGAN_MSGS[rand(0, GUNGAN_MSGS.length - 1)]);
                utils.setScriptVar(self, "chatSpam", 1);
            }
        }
        if (percentHealth <= 65 || percentHealth <= 55)
        {
            if (!utils.hasScriptVar(self, "chatSpamRandom1"))
            {
                chat.chat(self, GUNGAN_MSGS[rand(0, GUNGAN_MSGS.length - 1)]);
                utils.setScriptVar(self, "chatSpamRandom1", 1);
            }
            if (!utils.hasScriptVar(self, "chatSpamRandom2"))
            {
                chat.chat(self, GUNGAN_MSGS[rand(0, GUNGAN_MSGS.length - 1)]);
                utils.setScriptVar(self, "chatSpamRandom2", 1);
            }
        }
        if (percentHealth <= 50)
        {
            if (!utils.hasScriptVar(self, "hasBeenBoomad"))
            {
                chat.chat(self, GUNGAN_MSGS[rand(0, GUNGAN_MSGS.length - 1)]);
                chat.chat(self, "Whoopsies... Meesa slipped!");
                booma(self, players);
                utils.setScriptVar(self, "hasBeenBoomad", 1);
            }
        }
        if (percentHealth <= 37)
        {
            if (!utils.hasScriptVar(self, "hasBeenDisarmed"))
            {
                chat.chat(self, GUNGAN_MSGS[rand(0, GUNGAN_MSGS.length - 1)]);
                chat.chat(self, "Disarmen timen! Binksen no liken yousa weapons!");
                disarmPlayer(players);
                summonCompanions(self, attacker, "world_boss_donkdonk_binks_add", 6, 12.0f);
                for (obj_id who : players)
                {
                    broadcast(who, "Donk-Donk Binks has called for more reinforcements from the Swamp Village!");
                }
                utils.setScriptVar(self, "hasBeenDisarmed", 1);
            }
        }
        if (percentHealth <= 20)
        {
            if (!utils.hasScriptVar(self, "hasDruggedPlayers"))
            {
                chat.chat(self, "Meesa gonna make yousa feel verrry sleepy!");
                drugPlayers(self, players);
                utils.setScriptVar(self, "hasDruggedPlayers", 1);
            }
        }
        if (percentHealth <= 8)
        {
            if (!utils.hasScriptVar(self, "hasLastBoomad"))
            {
                chat.chat(self, "Meesa gonna make yousa go * BOOM! *");
                booma(self, players);
                utils.setScriptVar(self, "hasLastBoomad", 1);
            }
        }
        if (percentHealth <= 2)
        {
            if (!utils.hasScriptVar(self, "lastMsg"))
            {
                chat.chat(self, "If only me didn't liven a life of bombad crime...");
                utils.setScriptVar(self, "lastMsg", 1);
            }
        }
        else
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public void drugPlayers(obj_id self, obj_id[] targets) throws InterruptedException
    {
        if (targets == null)
        {
            return;
        }
        for (obj_id iTarget : targets)
        {
            sendConsoleCommand("/prone", iTarget);
            sendConsoleCommand("/dumpPausedCommands", iTarget);
            broadcast(iTarget, "You have been poisoned by Donk-Donk Binks!");
            buff.applyBuff(iTarget, "me_stasis_1", 10, 5);
            faceTo(iTarget, self);
        }
        chat.chat(self, "Yousa gonna sleep now!");
    }

    public void booma(obj_id self, obj_id[] targets) throws InterruptedException
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
            reduceHealth(iTarget, rand(1900, 6000));
            reduceAction(iTarget, rand(1000, 2000));
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
                "Help!"
        };
        for (obj_id i : player)
        {
            chat.chat(i, SHOUT_MSGS[rand(0, SHOUT_MSGS.length - 1)]);
            obj_id heldWeapon = getCurrentWeapon(i);
            if (isIdValid(heldWeapon))
            {
                putIn(heldWeapon, utils.getInventoryContainer(i));
                debugConsoleMsg(i, "\\#DD1234Donk-Donk Binks has disarmed you of your weapon!\\#.");
            }
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
                obj_id token = static_item.createNewItemFunction("item_world_boss_token_01_01", player);
                if (isIdValid(token))
                {
                    int multiplier = utils.getIntBonusValue("wb");
                    int newCount = 5 * multiplier;
                    setCount(token, newCount);
                    sendSystemMessage(player, "You have received " + color("DAA520", String.valueOf(newCount)) + " World Boss Tokens.", null);
                }
                showLootBox(player, new obj_id[]{token});
                if (!hasObjVar(player, "wb.donkdonk"))
                {
                    setObjVar(player, "wb.donkdonk", 1);
                }
                else
                {
                    int count = getIntObjVar(player, "wb.donkdonk");
                    if (count >= 10 && !hasObjVar(player, "wb.donkdonk_title"))
                    {
                        broadcast(player, "You have received the title " + color("DAA520", "Bombad General") + " for killing the Donk-Donk Binks 10 times!");
                        grantSkill(player, "title_world_boss_donkdonk");
                        setObjVar(player, "wb.donkdonk_title", 1);
                    }
                    else
                    {
                        setObjVar(player, "wb.donkdonk", count + 1);
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
