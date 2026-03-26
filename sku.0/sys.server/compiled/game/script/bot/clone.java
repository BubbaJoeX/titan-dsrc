package script.bot;/*
@Origin: script.bot.clone
@Author: OmegaSWG
@Purpose: AI Bots for testing and development.
@Requirements: $Orion/src:bots
@Note: This file is a work in progress and is not yet complete. See reqs. above for use.
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import java.util.Random;
import java.util.StringTokenizer;

import script.*;
import script.library.*;
import script.library.bot_lib;

import script.combat_engine.combat_data;

public class clone extends base_script
{
    public static final String[] buffComponentKeys =
            {
                    "kinetic",
                    "energy",
                    "action_cost_reduction",
                    "dodge",
                    "strength",
                    "constitution",
                    "stamina",
                    "precision",
                    "agility",
                    "luck",
                    "critical_hit",
                    "healing",
                    "healer",
                    "reactive_go_with_the_flow",
                    "flush_with_success",
                    "reactive_second_chance"
            };
    public static final int[] buffComponentValues =
            {
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15,
                    15
            };

    public clone()
    {
    }

    public static int getFactionId(String faction) throws InterruptedException
    {
        if (faction == null)
        {
            LOG("force_rank", "force_rank.getFactionId -- faction is null.");
            return 0;
        }
        int faction_num = factions.getFactionNumber(faction);
        if (faction_num == -1)
        {
            return 0;
        }
        return dataTableGetInt("datatables/faction/faction.iff", faction_num, "pvpFaction");
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        Object[] triggerParams = new Object[]{self};
        try
        {
            int err = script_entry.runScripts("OnLogin", triggerParams);
        } catch (Exception ex)
        {
            LOG("ScriptEntry", ex.toString());
        }
        return SCRIPT_CONTINUE;
    }

    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws InterruptedException
    {
        if (!isGod(speaker))
        {
            return SCRIPT_CONTINUE;
        }
        if (text.equals("update"))
        {
            transferPlayerData(self, speaker, null);
        }
        else if (text.equals("duel"))
        {
            queueCommand(self, getStringCrc((toLower("duel"))), speaker, "", COMMAND_PRIORITY_NORMAL);
        }
        else if (text.equals("faction"))
        {
            int n = (int) (Math.random() * 100);
            if (n <= 50)
            {
                pvpSetAlignedFaction(self, getFactionId("Imperial"));
            }
            else
            {
                pvpSetAlignedFaction(self, getFactionId("Rebel"));
            }
            pvpMakeDeclared(self);
        }
        else if (text.equals("buffs"))
        {
            buff.applyBuff(self, "buildabuff_inspiration", 7200);
            utils.setScriptVar(self, "performance.buildabuff.buffComponentKeys", buffComponentKeys);
            utils.setScriptVar(self, "performance.buildabuff.buffComponentValues", buffComponentValues);
            utils.setScriptVar(self, "performance.buildabuff.player", self);
            buff.applyBuff(self, "me_buff_health_2", 7200);
            buff.applyBuff(self, "me_buff_action_3", 7200);
            buff.applyBuff(self, "me_buff_strength_3", 7200);
            buff.applyBuff(self, "me_buff_agility_3", 7200);
            buff.applyBuff(self, "me_buff_precision_3", 7200);
            buff.applyBuff(self, "me_buff_melee_gb_1", 7200);
            buff.applyBuff(self, "me_buff_ranged_gb_1", 7200);
            buff.applyBuff(self, "of_buff_def_9", 7200);
            buff.applyBuff(self, "frogBuff", 7200);
            buff.applyBuff(self, "of_focus_fire_6", 7200);
            buff.applyBuff(self, "of_tactical_drop_6", 7200);
            buff.applyBuff(self, "banner_buff_commando", 7200);
            buff.applyBuff(self, "banner_buff_smuggler", 7200);
            buff.applyBuff(self, "banner_buff_medic", 7200);
            buff.applyBuff(self, "banner_buff_officer", 7200);
            buff.applyBuff(self, "banner_buff_spy", 7200);
            buff.applyBuff(self, "banner_buff_bounty_hunter", 7200);
            buff.applyBuff(self, "banner_buff_force_sensitive", 7200);
            broadcast(self, "GOD Buffs Granted");
        }
        else if (text.equals("revive"))
        {
            if (isDead(self))
            {
                resurrect(self);
            }
            if (getHealth(self) < 2500)
            {
                setHealth(self, getMaxHealth(self));
            }
        }
        else if (text.equals("level"))
        {
            int level = getLevel(speaker);
            setLevel(self, level);
        }
        else if (text.equals("ai"))
        {
            int on = utils.getIntScriptVar(self, "playerAi.on");
            if (on != 1)
            {
                messageTo(self, "playerMiscLoop", null, 1, false);
                messageTo(self, "playerCombatLoop", null, 1, false);
                messageTo(self, "playerMovementLoop", null, 1, false);
                utils.setScriptVar(self, "playerAi.on", 1);
            }
        }
        else if (text.equals("stance"))
        {
            queueCommand(self, getStringCrc((toLower("fs_buff_def_1_1"))), self, "", COMMAND_PRIORITY_NORMAL);
        }
        else if (text.equals("die"))
        {
            destroyObjectSimulator(self);
        }
        else if (text.equals("follow"))
        {
            follow(self, getIntendedTarget(speaker), 1, 5);
        }
        else if (text.equals("stay"))
        {
            stop(self);
        }
        else if (text.equals("attack"))
        {
            combat.startCombat(self, getIntendedTarget(speaker));
        }
        else if (text.equals("do"))
        {
            StringTokenizer st = new StringTokenizer(text);
            String completeToken = st.nextToken();
            while (st.hasMoreTokens())
            {
                completeToken += " " + st.nextToken();
            }
            queueCommand(self, getStringCrc((toLower(completeToken))), self, "", COMMAND_PRIORITY_NORMAL);
        }
        else if (text.equals("teach"))
        {
            String[] skills = getSkillListingForPlayer(speaker);
            for (int i = 0; i < skills.length; i++)
            {
                if (hasSkill(self, skills[i]))
                {
                    grantSkill(self, skills[i]);
                }
            }
        }
        else if (text.equals("scrub"))
        {
            String[] skills = getSkillListingForPlayer(self);
            for (int i = 0; i < skills.length; i++)
            {
                if (hasSkill(self, skills[i]))
                {
                    revokeSkill(self, skills[i]);
                }
            }
        }

        return SCRIPT_CONTINUE;
    }

    public int playerMiscLoop(obj_id self, dictionary params) throws InterruptedException
    {
        location loc = getLocation(self);
        warpPlayer(self, loc.area, loc.x, 0.0f, loc.z, null, 0.0f, 0.0f, 0.0f);
        if (isDead(self))
        {
            resurrect(self);
            setHealth(self, getMaxHealth(self));
            buff.removeBuff(self, "incapWeaken");
        }
        setPosture(self, POSTURE_UPRIGHT);
        messageTo(self, "playerMiscLoop", null, 5.0f, false);
        return SCRIPT_CONTINUE;
    }

    public boolean performCmd(obj_id self, obj_id target, String cmd) throws InterruptedException
    {
        combat_data cd = combat_engine.getCombatData(cmd);
        if (cd == null)
        {
            return false;
        }

        int groupCrc = getStringCrc(cd.cooldownGroup);
        final float coolDownLeft = getCooldownTimeLeft(self, groupCrc);
        if (coolDownLeft <= 0.0f)
        {
            int currentActionCrc = 0;
            if (combat.canPerformAction(cmd, self) == combat.ACTION_SUCCESS)
            {
                currentActionCrc = getStringCrc(cmd.toLowerCase());
            }
            if (currentActionCrc != 0)
            {
                queueCommand(self, currentActionCrc, target, "", COMMAND_PRIORITY_FRONT);
                return true;
            }
        }

        return false;
    }

    public int playerCombatLoop(obj_id self, dictionary params) throws InterruptedException
    {
        int n = 0;
        obj_id enemy = findBestEnemy(self);
        if (enemy == null)
            return SCRIPT_CONTINUE;
        else
            utils.setScriptVar(self, "playerAi.enemy_oid", enemy);

        setLookAtTarget(self, enemy);
        faceTo(self, enemy);
        startCombat(self, enemy);

        // find a new enemy if we've failed to engage with our current one
        if (!isEnemyWithinRange(self, enemy, 0.80f))
        {
            int retries = utils.getIntScriptVar(self, "playerAi.attack_attempts");
            if (retries >= 30)
            {
                enemy = findBestEnemy(self);
            }
            else
            {
                utils.setScriptVar(self, "playerAi.attack_attempts", retries + 1);
            }
        }
        else
        {
            utils.setScriptVar(self, "playerAi.attack_attempts", 0);
        }

        String DEFAULT_ATTACK = combat.getAttackName(self);
        performCmd(self, enemy, DEFAULT_ATTACK);

        //queueCommand(self, getStringCrc((combat.getAttackName(self)).toString()), enemy, "", COMMAND_PRIORITY_FRONT);
        //queueCommand(self, getStringCrc((toLower(combat.getAttackName(self))).toString()), enemy, "", COMMAND_PRIORITY_FRONT);

        String skillTemplate = getSkillTemplate(self);
        if (skillTemplate.indexOf("force") > -1)
        {
            if (getHealth(self) <= 8000)
            {
                //queueCommand(self, getStringCrc((toLower("fs_sh_3")).toString()), null, "", COMMAND_PRIORITY_FRONT);
                performCmd(self, null, "fs_sh_3");
            }

            String[] hostile = new String[]{
                    "fs_sweep_7",
                    "fs_force_spark",
                    "fs_dm_7",
                    "fs_dm_2_3",
                    "fs_dm_2_2"
            };
            n = (int) (Math.random() * hostile.length);
            //queueCommand(self, getStringCrc(hostile[n]), enemy, "", COMMAND_PRIORITY_FRONT);
            performCmd(self, enemy, hostile[n]);

            String[] helps = new String[]{
                    "fs_saber_reflect",
                    "saber_block",
                    "saberblock"
            };
            n = (int) (Math.random() * helps.length);
            //queueCommand(self, getStringCrc(helps[n]), null, "", COMMAND_PRIORITY_FRONT);
            //queueCommand(self, getStringCrc(helps[n]), self, "", COMMAND_PRIORITY_FRONT);
            performCmd(self, null, helps[n]);
            performCmd(self, self, helps[n]);
        }
        else
        {
            String[] cmds = getCommandListingForPlayer(self);
            n = (int) (Math.random() * cmds.length);
            //queueCommand(self, getStringCrc(cmds[n]), enemy, "", COMMAND_PRIORITY_FRONT);
            //queueCommand(self, getStringCrc(cmds[n]), self, "", COMMAND_PRIORITY_FRONT);
            //queueCommand(self, getStringCrc(cmds[n]), null, "", COMMAND_PRIORITY_FRONT);
            performCmd(self, enemy, cmds[n]);
            performCmd(self, self, cmds[n]);
        }

        messageTo(self, "playerCombatLoop", null, (float) (Math.random() * 0.35f) + 0.3f, false);
        return SCRIPT_CONTINUE;
    }

    public int playerMovementLoop(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id enemy = findBestEnemy(self);
        float nextInterval = 0.4f;

        // determine if they need to flee
        boolean flee = getHealth(self) <= 8000;
        flee &= (float) getHealth(enemy) > (0.8f * getHealth(self));

        // move towards our target, ensuring we're in range
        // goal: distance less than 10% from max range
        if (flee || !isEnemyWithinRange(self, enemy, 0.80f))
        {
            location newLoc = flee ? getEnemyAvgLoc(self) : getLocation(enemy);
            moveToPosition(self, newLoc, nextInterval / 2, flee);
        }

        messageTo(self, "playerMovementLoop", null, nextInterval, false);
        return SCRIPT_CONTINUE;
    }

    public boolean isEnemyWithinRange(obj_id self, obj_id enemy, float threshold) throws InterruptedException
    {
        obj_id weapon = getCurrentWeapon(self);
        float maxRange = getMaxRange(weapon);
        float distance = getDistance(self, enemy);
        return distance < maxRange * threshold;
    }

    public location getEnemyAvgLoc(obj_id self) throws InterruptedException
    {
        location enemyAvgLoc = null;
        obj_id[] enemies = getWhoIsTargetingMe(self);
        for (obj_id e : enemies)
        {
            boolean isValidEnemy = isValidEnemy(self, e);
            if (isValidEnemy)
            {
                if (enemyAvgLoc == null)
                {
                    enemyAvgLoc = getLocation(e);
                }
                else
                {
                    location loc = getLocation(e);
                    enemyAvgLoc.x = (enemyAvgLoc.x + loc.x) / 2;
                    enemyAvgLoc.z = (enemyAvgLoc.z + loc.z) / 2;
                }
            }
        }
        return enemyAvgLoc;
    }

    public obj_id findBestEnemy(obj_id self) throws InterruptedException
    {
        int n = 0;
        obj_id enemy = null;
        obj_id[] enemies = null;

        // first priority is existing target
        enemy = utils.getObjIdScriptVar(self, "playerAi.enemy_oid");
        if (isValidEnemy(self, enemy))
        {
            return enemy;
        }

        // second priority are people who are targetting us right now
        enemies = getWhoIsTargetingMe(self);
        for (obj_id e : enemies)
        {
            if (isValidEnemy(self, e))
            {
                return e;
            }
        }

        // third priority is any pvp target we can attack, aka enemies
        enemies = pvpGetEnemiesInRange(self, self, 128.0f);
        for (int i = 0; i < enemies.length; i++)
        {
            // randomly select a pvp enemy for combat
            n = (int) (Math.random() * enemies.length);
            enemy = enemies[n];
            if (isValidEnemy(self, enemy))
            {
                return enemy;
            }
        }

        return enemy;
    }

    public boolean isValidEnemy(obj_id self, obj_id enemy) throws InterruptedException
    {
        boolean result = isValidId(enemy);
        result &= pvpCanAttack(self, enemy);
        result &= !isDead(enemy);
        result &= !isIncapacitated(enemy);
        result &= !isInvulnerable(enemy);
        return result;
    }

    public boolean moveToPosition(obj_id self, location loc, float deltaTime, boolean flee) throws InterruptedException
    {
        location myLoc = getLocation(self);
        float movementSpeed = getRunSpeed(self);
        float travelDistance = movementSpeed * deltaTime;

        if (flee)
        {
            travelDistance = -travelDistance;
        }

        if (loc != null)
        {
            float x_d = loc.x - myLoc.x;
            float z_d = loc.z - myLoc.z;
            if (Math.abs(x_d) > 0.1 && Math.abs(z_d) > 0.1)
            {
                float deltaMovementNormal = (float) Math.sqrt((x_d * x_d) + (z_d * z_d));
                myLoc.x += (x_d / deltaMovementNormal) * travelDistance;
                myLoc.z += (z_d / deltaMovementNormal) * travelDistance;
                //warpPlayer(self, myLoc.area, myLoc.x + x_d, 0.0f, myLoc.z + z_d, null, 0.0f, 0.0f, 0.0f);
                setLocation(self, myLoc);
            }
            return true;
        }
        return false;
    }

}