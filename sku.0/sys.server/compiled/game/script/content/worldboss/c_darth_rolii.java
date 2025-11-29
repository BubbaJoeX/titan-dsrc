package script.content.worldboss;/*
@Origin: dsrc.script.theme_park.world_boss.master_controller_darth_rolii
@Author: BubbaJoeX
@Purpose: Handles the world boss upon spawning, combat ratios and death.
@Notes;
    Darth Rolii is a Sith world boss that spawns in the Chommell Sector on Tatooine.
    Darth Rolii has a variety of mechanics that make him a challenging fight.
        1. Darth Rolii will speak a spam message.
        2. Darth Rolii will disarm players of their weapons.
        3. Darth Rolii will bombard players, causing massive damage.
        4. Darth Rolii will enrage players, causing them to add hate.
        5. Darth Rolii will enter his last stand, calling upon the full power of the Dark Side of the Force.
        6. Darth Rolii will say a final flavor message before death.
@Requirements: script.player.player_nb, script.library.nb_player
@Created: Sunday, 2/01/2023, at 11:42 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.obj_id;

import java.util.Arrays;

public class c_darth_rolii extends script.base_script
{
    public static final String VOLUME_NAME = "aggressive_area";
    public String[] SITH_BATTLE_CHANTS = {
            "I am your new master...",
            "Feel the power of the dark side!",
            "Embrace your anger and unleash it!",
            "Sith shall conquer all in our path!",
            "Submit to me, you whelp!",
            "Crush the weak, let hatred fuel your strength!",
            "With passion and fury, we shall prevail!",
            "The Sith rise, the galaxy trembles!",
            "No mercy, no compassion, only power!",
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Darth Rolii");
        setDescriptionString(self, "A powerful rogue Sith apprentice.");
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.gizmo"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.gizmo");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.gizmo", "Active");
        titan_utils.markAsEventSpawn(self);
        titan_player.doWorldBossAnnounce(self, titan_player.WORLD_BOSS_GIZMO);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[World Boss System]: Rolii has been defeated.");
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        if (isGod(killer))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.gizmo"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.gizmo");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.gizmo", "Inactive");
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
        if (!utils.hasScriptVar(self, "chirp"))
        {
            //buff.applyBuff((self), "me_buff_health_2", 600);
            // buff.applyBuff((self), "me_buff_action_3", 600);
            buff.applyBuff((self), "me_buff_strength_3", 600);
            buff.applyBuff((self), "me_buff_agility_3", 600);
            buff.applyBuff((self), "me_buff_precision_3", 600);
            buff.applyBuff((self), "me_buff_melee_gb_1", 600);
            //buff.applyBuff((self), "me_buff_ranged_gb_1", 600);
            chat.chat(self, SITH_BATTLE_CHANTS[rand(0, SITH_BATTLE_CHANTS.length - 1)]);
            utils.setScriptVar(self, "chirp", 1);
        }
        if (percentHealth <= 75)
        {
            if (!utils.hasScriptVar(self, "hasSpawned"))
            {
                chat.chat(self, "You will perish!");
                for (obj_id who : players)
                {
                    broadcast(who, "Darth Rolii has lost his enhancements.  The Force says the time to strike is now!");
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
                chat.chat(self, SITH_BATTLE_CHANTS[rand(0, SITH_BATTLE_CHANTS.length - 1)]);
                bombard(self, players);
                utils.setScriptVar(self, "hasBeenBombed", 1);
            }
        }
        if (percentHealth <= 25)
        {
            if (!utils.hasScriptVar(self, "hasDisarmed"))
            {
                chat.chat(self, SITH_BATTLE_CHANTS[rand(0, SITH_BATTLE_CHANTS.length - 1)]);
                for (obj_id who : players)
                {
                    broadcast(who, "The most recent attack by " + getFirstName(attacker) + " has enraged Darth Roli, causing him to stifle all players!");
                }
                stunPlayers(self, players);
                utils.setScriptVar(self, "hasDisarmed", 1);
            }
        }
        if (percentHealth <= 10)
        {
            if (!utils.hasScriptVar(self, "hasLastStand"))
            {
                buff.removeAllBuffs(self);
                for (obj_id who : players)
                {
                    buff.applyBuff(who, "event_combat", 60, 150);
                    buff.removeAllDebuffs(who);
                    broadcast(who, "Darth Rolii has entered his last stand, calling upon the full power of the Dark Side of the Force!");
                    debugConsoleMsg(who, "\\#DAA520" + "ATTACK NOW!" + "\\#.");
                }
                buff.applyBuff(self, "event_buff_dev", 30, 150);
                utils.setScriptVar(self, "hasLastStand", 1);
            }
        }
        if (percentHealth <= 1)
        {
            if (!utils.hasScriptVar(self, "hasLastStandMsg"))
            {
                chat.chat(self, SITH_BATTLE_CHANTS[rand(0, SITH_BATTLE_CHANTS.length - 1)]);
                utils.setScriptVar(self, "hasLastStandMsg", 1);
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
        String[] stunMessages = {"Ahhh!", "Ouch!", "Ow!", "Ugh!", "Yikes!", "Mercy!!!"};
        for (obj_id iTarget : targets)
        {
            sendConsoleCommand("/kneel", iTarget);
            sendConsoleCommand("/say " + stunMessages[rand(0, stunMessages.length - 1)], iTarget);
            broadcast(iTarget, "The Dark Side of the Force brings you to your knees!");
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
            playClientEffectObj(iTarget, "clienteffect/avatar_explosion_02.cef", self, "");
            reduceHealth(iTarget, rand(1200, 3000));
            reduceAction(iTarget, rand(1200, 3000));
        }
    }

    public boolean reduceHealth(obj_id player, int amt)
    {
        damage(player, DAMAGE_ENERGY, HIT_LOCATION_BODY, amt / 2);
        damage(player, DAMAGE_KINETIC, HIT_LOCATION_BODY, amt / 2);
        return true;
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
                if (!hasObjVar(player, "wb.rolii"))
                {
                    setObjVar(player, "wb.rolii", 1);
                }
                else
                {
                    int count = getIntObjVar(player, "wb.rolii");
                    if (count >= 10 && !hasObjVar(player, "wb.rolii_title"))
                    {
                        broadcast(player, "You have received the title " + color("DAA520", "Apprentice of the Force") + " for killing Darth Rolii 10 times!");
                        grantSkill(player, "title_world_boss_rolii");
                        setObjVar(player, "wb.rolii_title", 1);
                    }
                    else
                    {
                        setObjVar(player, "wb.rolii", count + 1);
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

    public String[][] stripDuplicateStationIds(obj_id[] players) throws InterruptedException
    {
        String[][] playerStationIds = new String[0][2];
        for (obj_id player : players)
        {
            if (isIdValid(player))
            {
                int stationId = getPlayerStationId(player);
                if (stationId > 0)
                {
                    // Check if station ID already exists in the array
                    boolean found = false;
                    for (String[] playerStationId : playerStationIds)
                    {
                        if (playerStationId[1].equals(String.valueOf(stationId)))
                        {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        String[] playerInfo = {String.valueOf(player), String.valueOf(stationId)};
                        playerStationIds = addElement(playerStationIds, playerInfo);
                    }
                }
            }
        }
        return playerStationIds;
    }

    private String[][] addElement(String[][] array, String[] element)
    {
        String[][] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[array.length] = element;
        return newArray;
    }
}