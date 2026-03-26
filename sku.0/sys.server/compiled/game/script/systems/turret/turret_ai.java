package script.systems.turret;

import script.combat_engine.attacker_data;
import script.combat_engine.defender_data;
import script.combat_engine.hit_result;
import script.combat_engine.weapon_data;
import script.dictionary;
import script.library.*;
import script.location;
import script.obj_id;
import script.obj_var;

import java.util.Vector;

public class turret_ai extends script.systems.combat.combat_base_old
{
    public turret_ai()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        turret.activateTurret(self);
        ai_lib.setAttackable(self, true);
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        String template = getTemplateName(self);
        if (template.contains("adv"))
        {
            return SCRIPT_CONTINUE;
        }
        explodeTurret(self, self);
        return SCRIPT_CONTINUE;
    }
    public int OnPvpFactionChanged(obj_id self, int oldFaction, int newFaction) throws InterruptedException
    {
        setTurretAttributes(self, newFaction);
        return SCRIPT_CONTINUE;
    }
    public void setTurretAttributes(obj_id self, int turretFaction) throws InterruptedException
    {
        if (hasTriggerVolume(self, turret.ALERT_VOLUME_NAME))
        {
            removeTriggerVolume(turret.ALERT_VOLUME_NAME);
        }
        if (hasTriggerVolume(self, turret.VOL_TOO_CLOSE))
        {
            removeTriggerVolume(turret.VOL_TOO_CLOSE);
        }
        clearAttributeInterested(self, attrib.ALL);
        clearAttributeInterested(self, attrib.IMPERIAL);
        clearAttributeInterested(self, attrib.REBEL);
        clearAttributeInterested(self, attrib.HUTT);
        createTriggerVolume(turret.ALERT_VOLUME_NAME, turret.ALERT_VOLUME_SIZE, true);
        switch (turretFaction)
        {
            case (370444368):
            setAttributeInterested(self, attrib.ALL);
            setAttributeInterested(self, attrib.IMPERIAL);
            setAttributeInterested(self, attrib.HUTT);
            pvpSetAlignedFaction(self, (370444368));
            pvpMakeCovert(self);
            break;
            case (-615855020):
            setAttributeInterested(self, attrib.ALL);
            setAttributeInterested(self, attrib.REBEL);
            setAttributeInterested(self, attrib.HUTT);
            pvpSetAlignedFaction(self, (-615855020));
            pvpMakeCovert(self);
            break;
            default:
            setAttributeInterested(self, attrib.ALL);
            break;
        }
    }
    public int OnRemovingFromWorld(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, "lair.deadLair"))
        {
            destroyObject(self);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnAboutToOpenContainer(obj_id self, obj_id who) throws InterruptedException
    {
        if (isIdValid(who) && !isGod(who))
        {
            return SCRIPT_OVERRIDE;
        }
        return SCRIPT_CONTINUE;
    }
    public int OnAboutToLoseItem(obj_id self, obj_id destContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (isIdValid(transferer) && !isGod(transferer))
        {
            return SCRIPT_OVERRIDE;
        }
        return SCRIPT_CONTINUE;
    }
    public int OnAboutToReceiveItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (isIdValid(transferer) && !isGod(transferer))
        {
            return SCRIPT_OVERRIDE;
        }
        return SCRIPT_CONTINUE;
    }
    public int OnTriggerVolumeEntered(obj_id self, String volumeName, obj_id who) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "turret.gunner.suspendAiTriggers"))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(who))
        {
            return SCRIPT_CONTINUE;
        }
        int curHP = getHitpoints(self);
        if (curHP < 1)
        {
            explodeTurret(self, who);
            return SCRIPT_CONTINUE;
        }
        if (!pvpCanAttack(self, who))
        {
            return SCRIPT_CONTINUE;
        }
        if (volumeName.equals(turret.ALERT_VOLUME_NAME) && who != self)
        {
            turret.addTarget(self, who);
        }
        else if (volumeName.equals(turret.VOL_TOO_CLOSE) && who != self)
        {
            turret.removeTarget(self, who);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnTriggerVolumeExited(obj_id self, String volumeName, obj_id who) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "turret.gunner.suspendAiTriggers"))
        {
            return SCRIPT_CONTINUE;
        }
        if (volumeName.equals(turret.ALERT_VOLUME_NAME) && who != self)
        {
            turret.removeTarget(self, who);
            if (turret.isEngaged(self))
            {
                obj_id target = utils.getObjIdScriptVar(self, turret.SCRIPTVAR_ENGAGED);
                if (target == who)
                {
                    turret.disengage(self);
                }
            }
        }
        else if (volumeName.equals(turret.VOL_TOO_CLOSE) && who != self)
        {
            turret.addTarget(self, who);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnObjectDamaged(obj_id self, obj_id attacker, obj_id weapon, int damage) throws InterruptedException
    {
        int curHP = getHitpoints(self);
        if (curHP < 1)
        {
            explodeTurret(self, attacker);
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(self, "turret.gunner.suspendAiTriggers"))
        {
            turret.addTarget(self, attacker);
        }
        if (!utils.hasScriptVar(self, "playingEffect"))
        {
            int smolder = 2000;
            int fire = 1000;
            if (curHP < smolder)
            {
                if (curHP < fire)
                {
                    location death = getLocation(self);
                    playClientEffectLoc(attacker, "clienteffect/lair_hvy_damage_fire.cef", death, 0);
                    utils.setScriptVar(self, "playingEffect", 1);
                    messageTo(self, "effectManager", null, 15, true);
                }
                else 
                {
                    location death = getLocation(self);
                    playClientEffectLoc(attacker, "clienteffect/lair_med_damage_smoke.cef", death, 0);
                    utils.setScriptVar(self, "playingEffect", 1);
                    messageTo(self, "effectManager", null, 15, true);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int OnSawAttack(obj_id self, obj_id defender, obj_id[] attackers) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "turret.gunner.suspendAiTriggers"))
        {
            return SCRIPT_CONTINUE;
        }
        if (getConfigSetting("GameServer", "disableAICombat") != null)
        {
            setWantSawAttackTriggers(self, false);
            return SCRIPT_CONTINUE;
        }
        int numAtt = attackers.length;
        for (obj_id attacker : attackers) {
            if (pvpCanAttack(self, attacker)) {
                turret.addTarget(self, attacker);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public obj_id getGoodTurretTarget() throws InterruptedException
    {
        obj_id self = getSelf();
        if (!utils.hasScriptVar(self, turret.SCRIPTVAR_TARGETS))
        {
            return null;
        }
        obj_id[] old_targets = utils.getObjIdBatchScriptVar(self, turret.SCRIPTVAR_TARGETS);
        if ((old_targets == null) || (old_targets.length == 0))
        {
            utils.removeBatchScriptVar(self, turret.SCRIPTVAR_TARGETS);
            return null;
        }
        obj_id[] good_targets = cullInvalidTargets(self, old_targets);
        obj_id[] targets = removeIncapacitatedTargets(self, old_targets);
        if (targets.length == 0)
        {
            targets = good_targets;
            for (obj_id target : targets) {
                if (isIncapacitated(target) && !isDead(target) && getDistance(self, target) <= turret.FACTION_TURRET_RANGE && !pet_lib.isPet(target)) {
                    pclib.coupDeGrace(target, self);
                }
            }
            return obj_id.NULL_ID;
        }
        if (utils.hasScriptVar(self, xp.VAR_ATTACKER_LIST))
        {
            Vector attackerList = utils.getResizeableObjIdBatchScriptVar(self, xp.VAR_ATTACKER_LIST);
            if (attackerList != null && attackerList.size() > 0)
            {
                obj_var[] ovs = new obj_var[attackerList.size()];
                for (int i = 0; i < attackerList.size(); i++)
                {
                    String scriptVarPath = xp.VAR_ATTACKER_LIST + "." + ((obj_id)attackerList.get(i)) + ".damage";
                    ovs[i] = new obj_var((attackerList.elementAt(i)).toString(), utils.getIntScriptVar(self, scriptVarPath));
                }
                ovs = list.quickSort(0, ovs.length - 1, ovs);
                if ((ovs != null) && (ovs.length > 0))
                {
                    for (obj_var ov : ovs) {
                        String ovName = ov.getName();
                        obj_id tmp = utils.stringToObjId(ovName);
                        if (isIdValid(tmp)) {
                            if (utils.getElementPositionInArray(targets, tmp) > -1) {
                                if (canSee(self, tmp)) {
                                    return tmp;
                                }
                            }
                        }
                    }
                }
            }
        }
        int idx = rand(0, targets.length - 1);
        if (!turret.isValidTarget(self, targets[idx]))
        {
            turret.removeTarget(self, targets[idx]);
            return self;
        }
        return targets[idx];
    }
    public obj_id[] cullInvalidTargets(obj_id self, obj_id[] old_targets) throws InterruptedException
    {
        if (old_targets == null || old_targets.length == 0)
        {
            return null;
        }
        Vector toRemove = new Vector();
        toRemove.setSize(0);
        for (obj_id old_target : old_targets) {
            if (!turret.isValidTarget(self, old_target) || getDistance(self, old_target) > turret.FACTION_TURRET_RANGE) {
                toRemove = utils.addElement(toRemove, old_target);
            }
        }
        if (toRemove != null && toRemove.size() > 0)
        {
            Vector targets = new Vector();
            targets = utils.removeElements(targets, toRemove);
            obj_id[] _targets = new obj_id[0];
            if (targets != null)
            {
                _targets = new obj_id[targets.size()];
                targets.toArray(_targets);
            }
            return _targets;
        }
        return old_targets;
    }
    public obj_id[] removeIncapacitatedTargets(obj_id self, obj_id[] targets) throws InterruptedException
    {
        if (!isIdValid(self) || (targets == null) || (targets.length == 0))
        {
            return null;
        }
        Vector newTargets = new Vector();
        newTargets.setSize(0);
        for (obj_id target : targets) {
            if (!ai_lib.isAiDead(target) && getDistance(self, target) <= turret.FACTION_TURRET_RANGE) {
                newTargets = utils.addElement(newTargets, target);
            }
        }
        obj_id[] _newTargets = new obj_id[0];
        if (newTargets != null)
        {
            _newTargets = new obj_id[newTargets.size()];
            newTargets.toArray(_newTargets);
        }
        return _newTargets;
    }
    public void explodeTurret(obj_id turretid, obj_id killer) throws InterruptedException
    {
        obj_id base = getObjIdObjVar(turretid, hq.VAR_DEFENSE_PARENT);
        obj_id[] enemies = getWhoIsTargetingMe(turretid);
        if (enemies != null)
        {
            for (obj_id enemy : enemies) {
                queueClearCommandsFromGroup(enemy, (-506878646));
                queueClearCommandsFromGroup(enemy, (-1170591580));
                queueClearCommandsFromGroup(enemy, (391413347));
                setTarget(enemy, null);
            }
        }
        location death = getLocation(turretid);
        playClientEffectLoc(killer, "clienteffect/combat_explosion_lair_large.cef", death, 0);
        if (hasObjVar(turretid, player_structure.VAR_PLAYER_STRUCTURE))
        {
            turret.deactivateTurret(turretid);
            player_structure.destroyStructure(turretid, false);
        }
        else 
        {
            turret.deactivateTurret(turretid);
            hq.validateDefenseTracking(base);
            messageTo(turretid, "handleDestroyTurret", null, 2, false);
        }
        hq.validateDefenseTracking(base);
        messageTo(turretid, "handleDestroyTurret", null, 10.0f, false);
    }
    public float doAttack() throws InterruptedException
    {
        obj_id self = getSelf();
        obj_id base = getObjIdObjVar(self, hq.VAR_DEFENSE_PARENT);
        if (!turret.isActive(self))
        {
            return -1.0f;
        }
        int curHP = getHitpoints(self);
        if (curHP < 1)
        {
            hq.validateDefenseTracking(base);
            messageTo(self, "handleDestroyTurret", null, 2, false);
            return -1.0f;
        }
        obj_id target = utils.getObjIdScriptVar(self, turret.SCRIPTVAR_ENGAGED);
        if (!isIdValid(target) || !target.isLoaded() || (ai_lib.isAiDead(target)) || !canSee(self, target))
        {
            obj_id tmptarget = getGoodTurretTarget();
            if (tmptarget == null)
            {
                return -2.0f;
            }
            else if (tmptarget == obj_id.NULL_ID)
            {
                if (utils.hasScriptVar(self, "NULL_ID Counter"))
                {
                    if (utils.getIntScriptVar(self, "NULL_ID Counter") > 5)
                    {
                        return -2.0f;
                    }
                    else 
                    {
                        int searchLoopCounter = utils.getIntScriptVar(self, "NULL_ID Counter");
                        utils.setScriptVar(self, "NULL_ID Counter", ++searchLoopCounter);
                    }
                }
                else 
                {
                    utils.setScriptVar(self, "NULL_ID Counter", 1);
                }
                return 5.0f;
            }
            else if (tmptarget == self)
            {
                return 1.0f;
            }
            else if (tmptarget == target)
            {
                return 5.0f;
            }
            target = tmptarget;
            turret.engageTarget(self, target);
        }
        return doAttack(target);
    }
    public float doAttack(obj_id target) throws InterruptedException
    {
        return doAttack(target, obj_id.NULL_ID);
    }
    /**
     * @param gunnerForCredit player credited for damage/XP/loot/credits; use {@link obj_id#NULL_ID} for AI turret shots (attacker = turret).
     */
    public float doAttack(obj_id target, obj_id gunnerForCredit) throws InterruptedException
    {
        obj_id self = getSelf();
        obj_id base = getObjIdObjVar(self, hq.VAR_DEFENSE_PARENT);
        if (!isIdValid(self) || !isIdValid(target))
        {
            return -1.0f;
        }
        int curHP = getHitpoints(self);
        if (curHP < 1)
        {
            hq.validateDefenseTracking(base);
            messageTo(self, "handleDestroyTurret", null, 2, false);
            return -1.0f;
        }
        if (!hasObjVar(self, "objWeapon"))
        {
            return -1.0f;
        }
        if (!canSee(self, target))
        {
            obj_id tmptarget = getGoodTurretTarget();
            if (isIdValid(tmptarget) && tmptarget != target)
            {
                target = tmptarget;
                turret.engageTarget(self, target);
            }
            else 
            {
                return 2.0f;
            }
        }
        if (!turret.isValidTarget(self, target))
        {
            turret.removeTarget(self, target);
            return 2.0f;
        }
        messageTo(self, "reconfirmTarget", null, 5, false);
        final float DAMAGE_MODIFIER = 2.0f;
        final float HEALTH_COST_MODIFIER = 0;
        
        final float ACTION_COST_MODIFIER = 0;
        final int BASE_TO_HIT_MODIFIER = 0;
        final float AMMO_COST_MODIFIER = 1.0f;
        String[] strTimeMods = 
        {
            ""
        };
        String[] strDamageMods = 
        {
            ""
        };
        String[] strCostMods = 
        {
        };
        String[] strToHitMods = 
        {
            ""
        };
        String[] strBlockMods = 
        {
            ""
        };
        String[] strEvadeMods = 
        {
            ""
        };
        String[] strCounterAttackMods = 
        {
            ""
        };
        int intBlockMod = 1000;
        int intEvadeMod = 1000;
        int intCounterAttackMod = 1000;
        int intAttackerEndPosture = POSTURE_NONE;
        int intDefenderEndPosture = POSTURE_NONE;
        String strPlaybackAction = "fire_turret";
        int[] intEffects = 
        {
        };
        float[] fltEffectDurations = 
        {
        };
        int intChanceToApplyEffect = 0;
        obj_id objWeapon = getObjIdObjVar(self, "objWeapon");
        obj_id combatAttacker = self;
        final boolean gunnerPlayerShot = isIdValid(gunnerForCredit) && isPlayer(gunnerForCredit);
        if (gunnerPlayerShot)
        {
            combatAttacker = gunnerForCredit;
        }
        attacker_data cbtAttackerData = new attacker_data();
        weapon_data cbtWeaponData = new weapon_data();
        obj_id[] objDefenders = new obj_id[1];
        objDefenders[0] = target;
        defender_data[] cbtDefenderData = new defender_data[objDefenders.length];
        if (!getCombatData(combatAttacker, objDefenders, cbtAttackerData, cbtDefenderData, cbtWeaponData))
        {
            return -1.0f;
        }
        cbtWeaponData = getWeaponData(objWeapon);
        hit_result[] cbtHitData = new hit_result[1];
        cbtHitData[0] = calculateHit(cbtAttackerData, cbtDefenderData[0], cbtWeaponData);
        if (gunnerPlayerShot && cbtHitData[0].success)
        {
            int pct = turret_gunner_lib.getGunnerDamagePercent(self);
            int scaled = turret_gunner_lib.computeGunnerPercentHitDamage(target, pct);
            cbtHitData[0].damage = scaled;
            cbtHitData[0].elementalDamage = 0;
            cbtHitData[0].critDamage = 0;
            cbtHitData[0].bleedDamage = 0;
        }
        attacker_results cbtAttackerResults = new attacker_results();
        cbtAttackerResults.id = combatAttacker;
        defender_results[] cbtDefenderResults = new defender_results[1];
        cbtDefenderResults[0] = new defender_results();
        cbtAttackerResults.endPosture = -1;
        debugServerConsoleMsg(self, "posture set");
        cbtAttackerResults.weapon = objWeapon;
        debugServerConsoleMsg(self, "weapon");
        cbtDefenderResults[0].id = target;
        debugServerConsoleMsg(self, "target");
        cbtDefenderResults[0].endPosture = getPosture(target);
        debugServerConsoleMsg(self, "defender posture");
        if (cbtHitData[0].success)
        {
            cbtDefenderResults[0].result = COMBAT_RESULT_HIT;
        }
        else 
        {
            cbtDefenderResults[0].result = COMBAT_RESULT_MISS;
        }
        debugServerConsoleMsg(self, "hitdata");
        finalizeDamage(cbtAttackerData.id, cbtWeaponData, cbtDefenderData, cbtHitData, cbtDefenderResults, null);
        String[] strPlaybackNames = makePlaybackNames("fire_turret", cbtHitData, cbtWeaponData, cbtDefenderResults);
        doCombatResults(strPlaybackNames[0], cbtAttackerResults, cbtDefenderResults);
        combat.doBasicCombatSpam("shoot", cbtAttackerResults, cbtDefenderResults, cbtHitData);
        float recycleDelay = cbtWeaponData.attackSpeed + rand(0, 2);
        if (hasObjVar(self, "turret.dev.attackSpeedScale"))
        {
            recycleDelay *= getFloatObjVar(self, "turret.dev.attackSpeedScale");
        }
        return recycleDelay;
    }
    public int handleGunnerDirectionalShot(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id gunner = params.getObjId("gunner");
        if (!isIdValid(gunner) || !turret_gunner_lib.isOccupied(self) || turret_gunner_lib.getOccupant(self) != gunner)
        {
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(self, turret_gunner_lib.SCRIPTVAR_SUSPEND_AI_TRIGGERS))
        {
            return SCRIPT_CONTINUE;
        }
        float ax = params.getFloat("aimX");
        float ay = params.getFloat("aimY");
        float az = params.getFloat("aimZ");
        location turretLoc = getLocation(self);
        location aimLoc = new location(ax, ay, az, turretLoc.area, turretLoc.cell);
        // Wider half-angle than AI bursts: gunner aims by camera; ~45° total cone matches FPS tolerance.
        obj_id[] inCone = getObjectsInCone(self, aimLoc, turret.FACTION_TURRET_RANGE, 22.5f);
        if (inCone == null || inCone.length == 0)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id best = null;
        float bestDistSq = Float.MAX_VALUE;
        for (obj_id oid : inCone)
        {
            if (!isIdValid(oid) || oid == self || oid == gunner)
            {
                continue;
            }
            if (!turret.isValidTarget(self, oid))
            {
                continue;
            }
            location oLoc = getLocation(oid);
            float dx = oLoc.x - aimLoc.x;
            float dy = oLoc.y - aimLoc.y;
            float dz = oLoc.z - aimLoc.z;
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestDistSq)
            {
                bestDistSq = d2;
                best = oid;
            }
        }
        if (!isIdValid(best))
        {
            return SCRIPT_CONTINUE;
        }
        turret.engageTarget(self, best);
        doAttack(best, gunner);
        return SCRIPT_CONTINUE;
    }
    public int handleGunnerSingleShot(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id gunner = params.getObjId("gunner");
        obj_id tgt = params.getObjId("target");
        if (!isIdValid(gunner) || !turret_gunner_lib.isOccupied(self) || turret_gunner_lib.getOccupant(self) != gunner)
        {
            return SCRIPT_CONTINUE;
        }
        if (!utils.hasScriptVar(self, turret_gunner_lib.SCRIPTVAR_SUSPEND_AI_TRIGGERS))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(tgt) || !turret.isValidTarget(self, tgt))
        {
            return SCRIPT_CONTINUE;
        }
        turret.engageTarget(self, tgt);
        doAttack(tgt, gunner);
        return SCRIPT_CONTINUE;
    }
    public int handleTurretAttack(obj_id self, dictionary params) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "messageToSerialNum"))
        {
            int storedSerialNumber = utils.getIntScriptVar(self, "messageToSerialNum");
            int sentSerialNumber = params.getInt("messageToSerialNum");
            if (storedSerialNumber != sentSerialNumber)
            {
                return SCRIPT_CONTINUE;
            }
        }
        if (!turret.isActive(self))
        {
            return SCRIPT_CONTINUE;
        }
        float delay = doAttack();
        if (delay > 0.0f)
        {
            int serialNumber = (int)rand(0, 100);
            params.put("messageToSerialNum", serialNumber);
            utils.setScriptVar(self, "messageToSerialNum", serialNumber);
            messageTo(self, "handleTurretAttack", params, delay, false);
        }
        else 
        {
            utils.removeScriptVar(self, turret.SCRIPTVAR_ENGAGED);
            utils.removeScriptVar(self, "messageToSerialNum");
            utils.removeScriptVar(self, "ai.combat.isInCombat");
        }
        return SCRIPT_CONTINUE;
    }
    public int effectManager(obj_id self, dictionary params) throws InterruptedException
    {
        utils.removeScriptVar(self, "playingEffect");
        return SCRIPT_CONTINUE;
    }
    public int expireLair(obj_id self, dictionary params) throws InterruptedException
    {
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
    public int enemyDecloaked(obj_id self, dictionary params) throws InterruptedException
    {
        if ((params == null) || (params.isEmpty()))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = params.getObjId("target");
        if (isIdValid(target))
        {
            if (getDistance(self, target) <= turret.FACTION_TURRET_RANGE)
            {
                turret.addTarget(self, target);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int handleDestroyTurret(obj_id self, dictionary params) throws InterruptedException
    {
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
}
