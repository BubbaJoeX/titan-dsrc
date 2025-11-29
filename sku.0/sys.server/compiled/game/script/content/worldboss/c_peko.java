package script.content.worldboss;/*
@Origin: dsrc.script.theme_park.world_boss.master_controller_peko
@Author: BubbaJoeX
@Purpose: Handles the world boss upon spawning, combat ratios and death.
@Notes;
    The Mutated Peko-Peko Empress is a mutated Peko-Peko world boss that spawns in the Chommell Sector on Tatooine.
    The Mutated Peko-Peko Empress has a variety of mechanics that make her a challenging fight.
        1. The Mutated Peko-Peko Empress will speak a spam message.
        2. The Mutated Peko-Peko Empress will disarm players of their weapons.
        3. The Mutated Peko-Peko Empress will knock back players, causing them to stagger.
        4. The Mutated Peko-Peko Empress will call upon her whelps to aid her in her final stand.
        5. The Mutated Peko-Peko Empress will say a final flavor message before death.
@Requirements: script.player.player_nb, script.library.nb_player
@Created: Sunday, 2/01/2023, at 11:42 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.location;
import script.obj_id;

public class c_peko extends script.base_script
{
    public static final String VOLUME_NAME = "aggressive_area";
    public String[] SQUAWK_MSGS = {
            "<LOUD AVIAN NOISES>",
            "<ANGRY AVIAN NOISES>",
            "<UPSET AVIAN NOISES>",
            "<DISPLEASED AVIAN NOISES>",
            "<RIGHTEOUS AVIAN NOISES>",
            "<DISTURBING AVIAN NOISES>",
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Mutated Peko-Peko Empress");
        setDescriptionString(self, "A powerful and mutated Peko-Peko.");
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.peko"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.peko");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.peko", "Active");
        titan_utils.markAsEventSpawn(self);
        titan_player.doWorldBossAnnounce(self, titan_player.WORLD_BOSS_PEKO);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[World Boss System]: Peko has been killed");
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        if (isGod(killer))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id tatooine = getPlanetByName("tatooine");
        if (hasObjVar(tatooine, "dungeon_finder.world_boss.peko"))
        {
            removeObjVar(tatooine, "dungeon_finder.world_boss.peko");
        }
        setObjVar(tatooine, "dungeon_finder.world_boss.peko", "Inactive");
        titan_player.doWorldBossDeathMsg(self, killer); //only thing that is needed.
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
            chat.chat(self, SQUAWK_MSGS[rand(0, SQUAWK_MSGS.length - 1)]);
            utils.setScriptVar(self, "chirp", 1);
        }
        if (percentHealth <= 75)
        {
            if (!utils.hasScriptVar(self, "hasSpawned"))
            {
                titan_player.createCircleSpawn(self, self, "peko_peko", 12, 24);
                utils.setScriptVar(self, "hasSpawned", 1);
                return SCRIPT_CONTINUE;
            }
        }
        if (percentHealth <= 50)
        {
            if (!utils.hasScriptVar(self, "hasKnockedBack"))
            {
                chat.chat(self, SQUAWK_MSGS[rand(0, SQUAWK_MSGS.length - 1)]);
                staggerPlayers(self, players);
                utils.setScriptVar(self, "hasKnockedBack", 1);
            }
        }
        if (percentHealth <= 25)
        {
            if (!utils.hasScriptVar(self, "hasDisarmed"))
            {
                chat.chat(self, SQUAWK_MSGS[rand(0, SQUAWK_MSGS.length - 1)]);
                for (obj_id who : players)
                {
                    obj_id heldWeapon = getCurrentWeapon(who);
                    if (isIdValid(heldWeapon))
                    {
                        broadcast(who, "The most recent attack from " + getFirstName(attacker) + " caused the Mutated Peko-Peko Empress to disarm you with a wind gust!");
                        putIn(heldWeapon, utils.getInventoryContainer(who));
                    }
                }
                utils.setScriptVar(self, "hasDisarmed", 1);
            }
        }
        if (percentHealth <= 10)
        {
            if (!utils.hasScriptVar(self, "hasLastStand"))
            {
                buff.removeAllBuffs(self);
                titan_player.createCircleSpawn(self, self, "peko_peko_albatross_high", 4, 24);
                staggerPlayers(self, players);
                for (obj_id who : players)
                {
                    broadcast(who, "The Mutated Peko-Peko Empress has called upon her fledglings to aid her in her final stand!");
                }
                utils.setScriptVar(self, "hasLastStand", 1);
            }
        }
        return SCRIPT_CONTINUE;
    }

    public void staggerPlayers(obj_id self, obj_id[] targets) throws InterruptedException
    {
        float MAX_DISTANCE = titan_player.WORLD_BOSS_CREDIT_RANGE + 12f; //bring them outside the credit range, so if they are drags, they will need to manually run back.
        playClientEffectObj(targets, "clienteffect/cr_bodyfall_huge.cef", self, "");
        if (targets == null)
        {
            return;
        }
        for (obj_id iTarget : targets)
        {
            int playerHealth = getHealth(iTarget);
            int playerAction = getAction(iTarget);
            int statDrain = playerHealth / 2;
            int actionDrain = playerAction / 2;
            setHealth(iTarget, playerHealth - statDrain);
            setAction(iTarget, playerAction - actionDrain);
            location stagger = getLocation(iTarget);
            stagger.x = stagger.x + rand(-MAX_DISTANCE, MAX_DISTANCE);
            stagger.z = stagger.z + rand(-MAX_DISTANCE, MAX_DISTANCE);
            stagger.y = getHeightAtLocation(stagger.x, stagger.z);
            stagger.area = getCurrentSceneName();
            warpPlayer(iTarget, stagger.area, stagger.x, stagger.y, stagger.z, null, 0, 0, 0);
            broadcast(iTarget, "The wind from the Mutated Peko-Peko's wings have knocked you back!");
            if (buff.hasBuff(iTarget, "co_position_secured"))
            {
                buff.removeBuff(iTarget, "co_position_secured");
            }
            if (buff.hasBuff(iTarget, "co_base_of_operations"))
            {
                buff.removeBuff(iTarget, "co_base_of_operations");
            }
            sendConsoleCommand("/stopFollow", iTarget);
            faceTo(iTarget, self);
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
                else
                {
                    LOG("ethereal", "[World Boss System]: No valid ID for deathblower. Not handing out bonus tokens.");
                }
                showLootBox(player, new obj_id[]{token});
                if (!hasObjVar(player, "wb.peko"))
                {
                    setObjVar(player, "wb.peko", 1);
                }
                else
                {
                    int count = getIntObjVar(player, "wb.peko");
                    if (count >= 10 && !hasObjVar(player, "wb.peko_title"))
                    {
                        broadcast(player, "You have received the title " + color("DAA520", "Vanquisher of the Peko-Peko") + " for killing the Mutated Peko-Peko Empress 10 times!");
                        grantSkill(player, "title_world_boss_peko");
                        setObjVar(player, "wb.peko_title", 1);
                    }
                    else
                    {
                        setObjVar(player, "wb.peko", count + 1);
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
        return SCRIPT_CONTINUE;
    }

    public String color(String color, String text) throws InterruptedException
    {
        return "\\#" + color + text + "\\#.";
    }
}
