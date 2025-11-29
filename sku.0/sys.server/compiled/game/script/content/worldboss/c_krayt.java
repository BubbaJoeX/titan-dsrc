package script.content.worldboss;/*
@Origin: dsrc.script.theme_park.world_boss.master_controller_darth_rolii
@Author: BubbaJoeX
@Purpose: Handles the world boss upon spawning, combat ratios and death.
@Notes;
    Elder Krayt Dragon Ancient is a powerful world boss that spawns in the Dune Sea on Tatooine.
    Elder Krayt Dragon Ancient has a variety of mechanics that make him a challenging fight.
        1. Elder Krayt Dragon Ancient will speak a spam message.
        2. Elder Krayt Dragon Ancient will poison players.
        3. Elder Krayt Dragon Ancient will rampage players.
        4. Elder Krayt Dragon Ancient will call for assistance from the sands below.
        5. Elder Krayt Dragon Ancient will spew acid.
        6. Elder Krayt Dragon Ancient will stomp the ground.
@Requirements: script.player.player_nb, script.library.nb_player
@Created: Sunday, 2/01/2023, at 11:42 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.obj_id;
import script.string_id;

public class c_krayt extends script.base_script
{
    public static final String VOLUME_NAME = "aggressive_area";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Elder Krayt Dragon Ancient");
        setDescriptionString(self, "A powerful and ancient Krayt Dragon.");
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.krayt"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.krayt");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.krayt", "Active");
        titan_utils.markAsEventSpawn(self);
        titan_player.doWorldBossAnnounce(self, titan_player.WORLD_BOSS_KRAYT);
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        if (isGod(killer))
        {
            return SCRIPT_CONTINUE;
        }
        LOG("ethereal", "[World Boss System]: Krayt has been killed by " + killer);
        titan_player.doWorldBossDeathMsg(self, killer);
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.krayt"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.krayt");
            LOG("ethereal", "[World Boss System]: System has removed the world boss objvar from Tatooine");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.krayt", "Inactive");
        LOG("ethereal", "[World Boss System]: System has set the world boss objvar to inactive on Tatooine");
        return SCRIPT_CONTINUE;
    }

    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id wpn, int[] damage) throws InterruptedException
    {
        int health = getHealth(self);
        int maxHealth = getMaxHealth(self);
        int percentHealth = (health * 100) / maxHealth;
        if (attacker == self)
        {
            return SCRIPT_CONTINUE;
        }
        if (percentHealth < 90)
        {
            if (!utils.hasScriptVar(self, "krayt_poison"))
            {
                obj_id[] aoe_targets = getCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
                showFlyText(self, new string_id("- POISON -"), 5.5f, colors.GREEN);
                for (obj_id testSubjects : aoe_targets)
                {
                    buff.applyBuff(testSubjects, "poison", 60.0f, 5.0f);
                    broadcast(testSubjects, "The Elder Krayt Dragon Ancient has hit you with poison!");
                    playClientEffectLoc(testSubjects, "clienteffect/combat_grenade_poison.cef", getLocation(testSubjects), 1.0f);
                }
                utils.setScriptVar(self, "krayt_poison", 1);
            }
        }
        if (percentHealth < 70)
        {
            if (!utils.hasScriptVar(self, "krayt_rampage"))
            {
                obj_id[] aoe_targets = getCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
                showFlyText(self, new string_id("- RAMPAGE -"), 5.5f, colors.RED);
                for (obj_id testSubjects : aoe_targets)
                {
                    int randDamage = rand(1688, 2694);
                    buff.applyBuff(testSubjects, "poison", 60.0f, 5.0f);
                    broadcast(testSubjects, "The Elder Krayt Dragon Ancient has gone on a rampage!");
                    damage(testSubjects, DAMAGE_ELEMENTAL_HEAT, HIT_LOCATION_BODY, randDamage);
                    LOG("ethereal", "[World Boss System]: Krayt has damaged " + aoe_targets + " for a total of " + randDamage + " damage.");
                }
                utils.setScriptVar(self, "krayt_rampage", 1);
            }
        }
        if (percentHealth < 50)
        {
            if (!utils.hasScriptVar(self, "krayt_adds"))
            {
                showFlyText(self, new string_id("- DISTRESS -"), 5.5f, colors.ORANGE);
                obj_id[] aoe_targets = getCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
                for (obj_id testSubjects : aoe_targets)
                {
                    broadcast(testSubjects, "The Elder Krayt Dragon Ancient has yelled for assistance from the sands below!");
                }
                titan_player.createCircleSpawn(self, self, "krayt_dragon_adolescent", 6, 12);
                utils.setScriptVar(self, "krayt_adds", 1);
            }
        }
        if (percentHealth < 30)
        {
            if (!utils.hasScriptVar(self, "krayt_devastation"))
            {
                obj_id[] aoe_targets = getCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
                showFlyText(self, new string_id("- GRGGRGRGRGRLGLGLRGLR -"), 5.5f, colors.GOLD);
                for (obj_id testSubjects : aoe_targets)
                {
                    int randDamage = rand(1800, 2000);
                    broadcast(testSubjects, "The Elder Krayt Dragon Ancient has spewed acid!");
                    damage(testSubjects, DAMAGE_ELEMENTAL_ACID, HIT_LOCATION_HEAD, randDamage);
                    playClientEffectLoc(testSubjects, "clienteffect/combat_grenade_poison.cef", getLocation(testSubjects), 1.0f);
                }
            }
            utils.setScriptVar(self, "krayt_devastation", 1);
        }
        if (percentHealth < 10)
        {
            if (!utils.hasScriptVar(self, "krayt_stomp"))
            {

                obj_id[] aoe_targets = getCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
                for (obj_id testSubjects : aoe_targets)
                {
                    playClientEffectObj(testSubjects, "clienteffect/int_camshake_heavy.cef", self, "root");
                    playClientEffectObj(testSubjects, "clienteffect/int_camshake_medium.cef", self, "root");
                    playClientEffectObj(testSubjects, "clienteffect/int_camshake_light.cef", self, "root");
                    broadcast(testSubjects, "The Elder Krayt Dragon Ancient has stomped the ground!");
                    sendConsoleCommand("/kneel", testSubjects);
                    sendConsoleCommand("/prone", testSubjects);
                }
                chat.chat(self, "-EAR PIERCING ROAR-");
                utils.setScriptVar(self, "krayt_stomp", 1);
            }
        }
        if (percentHealth < 5)
        {
            if (!utils.hasScriptVar(self, "krayt_final"))
            {
                obj_id[] aoe_targets = getCreaturesInRange(self, titan_player.WORLD_BOSS_CREDIT_RANGE);
                for (obj_id testSubjects : aoe_targets)
                {
                    playClientEffectObj(testSubjects, "clienteffect/int_camshake_light.cef", self, "");
                    broadcast(testSubjects, "The Elder Krayt Dragon Ancient has been weakened!");
                }
                setWeaponAttackSpeed(aiGetPrimaryWeapon(self), 0.1f);
                chat.chat(self, "-YELP-");
                utils.setScriptVar(self, "krayt_final", 1);
            }

        }
        return SCRIPT_CONTINUE;
    }

    public void createStomachContents(obj_id self, obj_id playerInventory, int count) throws InterruptedException
    {
        //@NOTE: This is a mechanic to "give" the players the contents of the stomach, as the Krayt most likely has been eating a lot of people.
        String JUNK_TABLE = "datatables/crafting/reverse_engineering_junk.iff";
        String column = "note";
        int JUNK_COUNT = count;
        for (int i = 0; i < JUNK_COUNT; i++)
        {
            String junk = dataTableGetString(JUNK_TABLE, rand(1, dataTableGetNumRows(JUNK_TABLE)), column);
            obj_id junkItem = static_item.createNewItemFunction(junk, playerInventory);
            if (isIdValid(junkItem))
            {
                if (junk.contains("heroic_") || junk.contains("_heroic_") || junk.contains("meatlump")) //@NOTE: This is to prevent the system from giving out junk that is not supposed to be given out (hoth, meatlump, etc, etc)
                {
                    --JUNK_COUNT;
                    continue;
                }
                setCount(junkItem, rand(1, 3));
            }
        }

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
                if (!hasObjVar(player, "wb.krayt"))
                {
                    setObjVar(player, "wb.krayt", 1);
                }
                else
                {
                    int count = getIntObjVar(player, "wb.krayt");
                    if (count >= 10 && !hasObjVar(player, "wb.krayt_title"))
                    {
                        broadcast(player, "You have received the title " + color("DAA520", "Slayer of the Ancients") + " for killing the Elder Krayt Dragon Ancient 10 times!");
                        grantSkill(player, "title_world_boss_krayt");
                        setObjVar(player, "wb.krayt_title", 1);
                    }
                    else
                    {
                        setObjVar(player, "wb.krayt", count + 1);
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
        int random = rand(1, 100);
        if (random >= 75)
        {
            obj_id bunkerRewardee = players[rand(0, players.length - 1)];
            static_item.createNewItemFunction("item_content_bunker_house_02", utils.getInventoryContainer(bunkerRewardee));
            broadcast(bunkerRewardee, "You have found an interesting structure deed within this creature's stomach!");
            LOG("ethereal", "[World Boss System]: System has given " + getPlayerFullName(bunkerRewardee) + " a housing deed, selected at random.");
        }
        obj_id[] topDamagers = getObjIdArrayObjVar(self, xp.VAR_TOP_DAMAGERS);
        for (obj_id anAttacker : topDamagers)
        {
            broadcast(anAttacker, "You have found some strange items within this creature's stomach!");
            createStomachContents(self, utils.getInventoryContainer(anAttacker), 5);
            showFlyText(self, new string_id("+ REGURGITATION + "), 1.4f, colors.GREENYELLOW);
            LOG("ethereal", "[World Boss System]: System has given " + getPlayerFullName(anAttacker) + " stomach contents for being on the attacker list");
        }
        int chance = rand(0, 100);
        if (chance >= 75)
        {
            broadcast(landedDeathBlow, "You have found a flawless pearl within this creature's stomach!");
            static_item.createNewItemFunction("item_krayt_pearl_04_20", utils.getInventoryContainer(landedDeathBlow));
            LOG("ethereal", "[World Boss System]: System has given " + getPlayerFullName(landedDeathBlow) + " a flawless pearl for killing the Krayt.");
        }
        else if (chance <= 25)
        {
            broadcast(landedDeathBlow, "You have found a near flawless pearl within this creature's stomach!");
            static_item.createNewItemFunction("item_krayt_pearl_04_19", utils.getInventoryContainer(landedDeathBlow));
            LOG("ethereal", "[World Boss System]: System has given " + getPlayerFullName(landedDeathBlow) + " a near flawless pearl for killing Krayt,");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[World Boss System]: Krayt has been killed [OnDeath]");
        return SCRIPT_CONTINUE;
    }

    public String color(String color, String text) throws InterruptedException
    {
        return "\\#" + color + text + "\\#.";
    }
}
