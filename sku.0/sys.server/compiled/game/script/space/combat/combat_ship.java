package script.space.combat;

import script.*;
import script.library.*;
import script.space.atmo.atmo_landing_registry;

import java.util.Vector;

public class combat_ship extends script.base_script
{
    public combat_ship()
    {
    }
    public static final float BROKEN_COMPONENT_DEFAULT_MASS = 50000.0f;
    public static final float SPACE_YACHT_COMPONENT_DEFAULT_MASS = 0.0f;
    public static final string_id SID_TARGET_DISABLED = new string_id("space/quest", "target_disabled2");
    public static final int DROID_VOCALIZE_REACT_CHANCE = 2;
    public static final int SHIP_DAMAGED_SKILLMOD_PENALTY_TIME = 10;
    public static final int SHIP_FIRED_SKILLMOD_PENALTY_TIME = 5;
    public static final float STUNNED_COMPONENT_LOOP_TIME = 5.0f;
    public static final String NO_DAMAGE_WARN = "clienteffect/cbt_friendlyfire_warn.cef";

    // NPC POB Ship waypoint cycling constants
    public static final String NPC_CONTROLLER_OBJVAR = "npc_pob.controller";
    public static final String NPC_CURRENT_WAYPOINT_INDEX = "npc_pob.currentWaypointIndex";
    public static final String NPC_LANDING_END_TIME = "npc_pob.landingEndTime";
    public static final String NPC_DATATABLE_PREFIX = "datatables/npc_shuttle/";
    public static final String NPC_DATATABLE_SUFFIX = ".iff";
    public static final int NPC_WAYPOINT_CHECK_INTERVAL = 2;
    private static final float NPC_SHUTTLE_LOG_RANGE = 2000.0f;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        int[] intSlots = getShipChassisSlots(self);
        for (int intI = 0; intI < intSlots[intI]; intI++)
        {
            setShipComponentEfficiencyGeneral(self, intSlots[intI], 1.0f);
            setShipComponentEfficiencyEnergy(self, intSlots[intI], 1.0f);
        }

        // Check if this is an NPC-controlled ship and start waypoint management
        if (hasObjVar(self, NPC_CONTROLLER_OBJVAR))
        {
            messageTo(self, "npcWaypointTick", null, NPC_WAYPOINT_CHECK_INTERVAL, false);
        }

        messageTo(self, "setupRotationalVelocity", null, 2, false);
        return SCRIPT_CONTINUE;
    }
    public int setupRotationalVelocity(obj_id self, dictionary params) throws InterruptedException
    {
        if (isShipSlotInstalled(self, space_crafting.ENGINE))
        {
            space_crafting.setupChassisDifferentiation(self);
        }
        else 
        {
        }
        return SCRIPT_CONTINUE;
    }
    public int OnLogin(obj_id self) throws InterruptedException
    {
        space_combat.clearDeathFlags(self);
        return SCRIPT_CONTINUE;
    }
    public int OnShipHitByLightning(obj_id self, int frontBack, float damage) throws InterruptedException
    {
        if (damage > 0.0f)
        {
            notifyShipDamage(self, null, damage);
        }
        float fltRemainingDamage = space_combat.doShieldDamage(null, self, space_combat.SHIP, damage, frontBack);
        return SCRIPT_CONTINUE;
    }
    public int OnShipHitByEnvironment(obj_id self, int frontBack, float damage) throws InterruptedException
    {
        if (damage > 0.0f)
        {
            notifyShipDamage(self, null, damage);
        }
        float fltRemainingDamage = space_combat.doShieldDamage(null, self, space_combat.SHIP, damage, frontBack);
        return SCRIPT_CONTINUE;
    }
    public int OnShipWasHit(obj_id self, obj_id objAttacker, int intWeaponIndex, boolean isMissile, int missileType, int intTargetedComponent, boolean fromPlayerAutoTurret, float hitLocationX_o, float hitLocationY_o, float hitLocationZ_o) throws InterruptedException
    {
        int intDisabledTime = getIntObjVar(self, "isDisabled");
        if (intDisabledTime > 0)
        {
            int intTime = getGameTime();
            if (intTime > intDisabledTime)
            {
                setObjVar(self, "isDisabled", 0);
            }
            else 
            {
                return SCRIPT_CONTINUE;
            }
        }
        int intWeaponSlot = intWeaponIndex + ship_chassis_slot_type.SCST_weapon_0;
        if (hasObjVar(self, "intInvincible"))
        {
            ship_ai.unitAddDamageTaken(self, objAttacker, 1.0f);
            return SCRIPT_CONTINUE;
        }
        boolean bossShip = false;
        if (hasObjVar(self, "bossType"))
        {
            bossShip = true;
        }
        if (space_utils.isPlayerControlledShip(objAttacker) && hasObjVar(self, "objMissionOwner"))
        {
            obj_id objOwner = getObjIdObjVar(self, "objMissionOwner");
            if (isIdValid(objOwner) && exists(objOwner))
            {
                boolean absorb = true;
                obj_id group_id = getGroupObject(objOwner);
                if (isIdValid(group_id))
                {
                    obj_id[] groupMembers = space_utils.getSpaceGroupMemberIds(group_id);
                    if (groupMembers != null)
                    {
                        for (obj_id groupMember : groupMembers) {
                            if (objAttacker == space_transition.getContainingShip(groupMember)) {
                                absorb = false;
                                break;
                            }
                        }
                    }
                }
                if (space_transition.getContainingShip(objOwner) == objAttacker)
                {
                    absorb = false;
                }
                if (absorb)
                {
                    Vector gunners = space_utils.getGunnersInShip(objAttacker);
                    if (gunners == null || gunners.size() == 0)
                    {
                        playClientEffectObj(getPilotId(objAttacker), NO_DAMAGE_WARN, getPilotId(objAttacker), "");
                    }
                    else 
                    {
                        for (Object gunner : gunners) {
                            playClientEffectObj(((obj_id) gunner), NO_DAMAGE_WARN, ((obj_id) gunner), "");
                        }
                    }
                    return SCRIPT_CONTINUE;
                }
            }
        }
        if (space_combat.hasDeathFlags(self))
        {
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(self, "intNoPlayerDamage") && space_utils.isPlayerControlledShip(objAttacker))
        {
            return SCRIPT_CONTINUE;
        }
        if(hasObjVar(self, "intPvPDamageOnly") && space_utils.isPlayerControlledShip(objAttacker) && !(pvpGetType(objAttacker) == PVPTYPE_DECLARED)){
            return SCRIPT_CONTINUE;
        }
        if (!isShipSlotTargetable(self, intTargetedComponent))
        {
            intTargetedComponent = space_combat.SHIP;
        }
        if (hasScript(self, "space.combat.combat_ship_capital"))
        {
            return SCRIPT_CONTINUE;
        }
        if (!pvpCanAttack(objAttacker, self))
        {
            return SCRIPT_CONTINUE;
        }
        pvpAttackPerformed(objAttacker, self);
        if (hasScript(self, "e3demo.spawner_nebulon") || (hasScript(self, "e3demo.nebulon_damaged")))
        {
            return SCRIPT_CONTINUE;
        }
        space_combat.checkAndPerformCombatTaunts(objAttacker, self, "fltAttackTauntChance", "hitYou", 0);
        space_combat.checkAndPerformCombatTaunts(self, objAttacker, "fltDefendTauntChance", "gotHit", 0);
        obj_id objPilot = getPilotId(objAttacker);
        transform attackerTransform_w = getTransform_o2w(objAttacker);
        transform defenderTransform_w = getTransform_o2w(self);
        vector hitDirection_o = defenderTransform_w.rotateTranslate_p2l(attackerTransform_w.getPosition_p());
        int intSide = 0;
        if (hitDirection_o.z < 0.0f)
        {
            intSide = 1;
        }
        if (utils.checkConfigFlag("ScriptFlags", "e3Demo"))
        {
            if (space_utils.isPlayerControlledShip(self))
            {
                int intSlot = rand(1, 4);
                int intIntensity = rand(1, 100);
                space_combat.doInteriorDamageNotification(self, intSlot, 100, intIntensity);
                return SCRIPT_CONTINUE;
            }
        }
        float fltDamage = space_combat.getShipWeaponDamage(objAttacker, self, intWeaponSlot, isMissile);

        // scale back cap ship to cap ship damage for space gcw battles
        if(hasScript(self, "systems.gcw.space.capital_ship") && hasScript(objAttacker, "systems.gcw.space.capital_ship")){
            // scale damage down 50%
            fltDamage = fltDamage * 0.25f;
        }

        if (isIdValid(getPilotId(self)))
        {
            if (hasObjVar(getPilotId(self), "intCombatDebug"))
            {
                if (isMissile)
                {
                    sendSystemMessageTestingOnly(getPilotId(self), "MISSILED! for " + fltDamage + "from " + objAttacker + " missile type is " + missileType);
                }
            }
        }
        if (fltDamage > 0.0f)
        {
            notifyShipDamage(self, objAttacker, fltDamage);
            ship_ai.unitAddDamageTaken(self, objAttacker, fltDamage);
        }
        if (space_utils.isPlayerControlledShip(self))
        {
            if (!utils.hasLocalVar(self, "cmd.wasDamagedSkillMod"))
            {
                int time = getGameTime();
                utils.setLocalVar(self, "cmd.wasDamagedSkillMod", SHIP_DAMAGED_SKILLMOD_PENALTY_TIME + time);
            }
        }
        float fltRemainingDamage = space_combat.doShieldDamage(objAttacker, self, intWeaponSlot, fltDamage, intSide);
        if (fltRemainingDamage > 0)
        {
            if (bossShip && !utils.hasScriptVar(self, "shieldDepleted"))
            {
                messageTo(self, "shieldDepleted", null, 0.0f, false);
            }
            fltRemainingDamage = space_combat.doArmorDamage(objAttacker, self, intWeaponSlot, fltRemainingDamage, intSide);
            if (fltRemainingDamage > 0)
            {
                // this case prevents a player from doing component or chassis damage during a space GCW fight.
                if(space_utils.isPlayerControlledShip(objAttacker) && hasScript(self, "systems.gcw.space.capital_ship")){
                    return SCRIPT_CONTINUE;
                }
                if (bossShip && !utils.hasScriptVar(self, "armorDepleted"))
                {
                    messageTo(self, "armorDepleted", null, 0.0f, false);
                }
                fltRemainingDamage = space_combat.doComponentDamage(objAttacker, self, intWeaponSlot, intTargetedComponent, fltRemainingDamage, intSide);
                if (fltRemainingDamage > 0)
                {
                    if (rand(1, 10) < DROID_VOCALIZE_REACT_CHANCE)
                    {
                        if (space_utils.isPlayerControlledShip(self))
                        {
                            space_combat.flightDroidVocalize(self, 1);
                        }
                    }
                    fltRemainingDamage = space_combat.doChassisDamage(objAttacker, self, intWeaponSlot, fltRemainingDamage);
                    if (fltRemainingDamage > 0)
                    {
                        setShipCurrentChassisHitPoints(self, 0.0f);
                        obj_id objDefenderPilot = getPilotId(self);
                        if (!space_utils.isPlayerControlledShip(self))
                        {
                            if (space_utils.isPlayerControlledShip(objAttacker) || (hasObjVar(objAttacker, "commanderPlayer")))
                            {
                                if (utils.hasLocalVar(self, "space.give_rewards"))
                                {
                                    utils.setLocalVar(self, "space.give_rewards", 2);
                                }
                                else 
                                {
                                    utils.setLocalVar(self, "space.give_rewards", 1);
                                }
                                space_combat.checkAndPerformCombatTaunts(self, objAttacker, "fltDieTauntChance", "death", 0);
                                space_combat.targetDestroyed(self);
                                return SCRIPT_CONTINUE;
                            }
                            else 
                            {
                                space_combat.targetDestroyed(self);
                                return SCRIPT_CONTINUE;
                            }
                        }
                        else 
                        {
                            space_combat.setDeathFlags(self);
                            space_combat.sendDestructionNotification(self, objAttacker);
                            float fltIntensity = rand(0, 1.0f);
                            handleShipDestruction(self, fltIntensity);
                            if (space_utils.isPlayerControlledShip(objAttacker))
                            {
                                obj_id[] crew = space_utils.getAllPlayersInShip(objAttacker);
                                gcw.grantSpacePvpKillCredit(getPilotId(self), crew);
                                utils.setScriptVar(self, "intPVPKill", 1);
                            }
                            messageTo(self, "killSpacePlayer", null, 10.0f, true);
                            space_combat.doDeathCleanup(self);
                            CustomerServiceLog("space_death", "%TU " + self + " Has been killed by " + objAttacker, getOwner(objAttacker));
                            if (space_battlefield.isInBattlefield(self))
                            {
                                CustomerServiceLog("battlefield", "%TU " + self + " Has been killed by " + objAttacker, getOwner(objAttacker));
                            }
                        }
                    }
                }
                if (rand(1, 10) < DROID_VOCALIZE_REACT_CHANCE)
                {
                    if (space_utils.isPlayerControlledShip(self))
                    {
                        space_combat.flightDroidVocalize(self, 2);
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int killSpacePlayer(obj_id self, dictionary params) throws InterruptedException
    {
        Vector objPlayers = space_transition.getContainedPlayers(self, null);
        if (objPlayers != null)
        {
            for (Object objPlayer : objPlayers) {
                space_combat.strikeBomberCleanup(((obj_id) objPlayer));
            }
        }
        space_combat.killSpacePlayer(self);
        space_combat.clearDeathFlags(self);
        return SCRIPT_CONTINUE;
    }
    public int OnSpaceUnitEnterCombat(obj_id self, obj_id objTarget) throws InterruptedException
    {
        setCondition(self, CONDITION_WINGS_OPENED);
        space_combat.checkAndPerformCombatTaunts(self, objTarget, "fltIntroTauntChance", "entercombat", 0);
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        obj_id player = utils.getContainingPlayer(self);
        setCondition(self, CONDITION_ON);
        String strChassisType = getShipChassisType(self);
        int[] intSlots = space_crafting.getShipInstalledSlots(self);
        for (int intSlot : intSlots) {
            int currentSlotComponentType = ship_chassis_slot_type.getComponentTypeForSlot(intSlot);
            if (currentSlotComponentType != ship_component_type.SCT_modification) {
                float currentComponentMass = getShipComponentMass(self, intSlot);
                if (strChassisType.equals("player_sorosuub_space_yacht")) {
                    if (currentComponentMass != 0) {
                        setShipComponentMass(self, intSlot, SPACE_YACHT_COMPONENT_DEFAULT_MASS);
                    }
                } else if (currentComponentMass == 0) {
                    setShipComponentMass(self, intSlot, BROKEN_COMPONENT_DEFAULT_MASS);
                }
            }
            if (intSlot == space_crafting.ENGINE) {
                space_crafting.setupChassisDifferentiation(self);
            }
            space_combat.recalculateEfficiency(intSlot, self);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnDestroy(obj_id self) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self))
        {
            return SCRIPT_CONTINUE;
        }

        atmo_landing_registry.detachShipFromLandingPoint(self);

        obj_id[] notifylist = getObjIdArrayObjVar(self, "destroynotify");
        if (notifylist != null)
        {
            dictionary outparams = new dictionary();
            outparams.put("object", self);
            for (obj_id obj_id : notifylist) {
                if (exists(obj_id) && (obj_id.isLoaded())) {
                    space_utils.notifyObject(obj_id, "shipDestroyed", outparams);
                }
            }
        }
        if (!space_utils.isPlayerControlledShip(self))
        {
            if (hasObjVar(self, "objParent"))
            {
                space_content.notifySpawner(self);
            }
        }
        if (utils.hasLocalVar(self, "space.give_rewards"))
        {
            if (utils.getIntLocalVar(self, "space.give_rewards") == 1)
            {
                space_combat.grantRewardsAndCreditForKills(self);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int objectDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objPilot = getPilotId(self);
        if (!space_utils.isPlayerControlledShip(self))
        {
            if (hasObjVar(self, "objParent"))
            {
                space_content.notifySpawner(self);
            }
            float fltIntensity = rand(0, 1.0f);
            handleShipDestruction(self, fltIntensity);
        }
        else 
        {
            float fltIntensity = rand(0, 1.0f);
            handleShipDestruction(self, fltIntensity);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
    public int targetDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        if (!space_utils.isPlayerControlledShip(self))
        {
            return SCRIPT_CONTINUE;
        }
        Vector objOfficers = space_utils.getShipOfficers(self);
        for (Object objOfficer : objOfficers) {
            space_utils.notifyObject(((obj_id) objOfficer), "targetDestroyed", params);
        }
        return SCRIPT_CONTINUE;
    }
    public int targetDisabled(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objDefender = params.getObjId("objDefender");
        if (!space_utils.isPlayerControlledShip(self))
        {
            return SCRIPT_CONTINUE;
        }
        Vector objOfficers = space_utils.getShipOfficers(self);
        for (Object objOfficer : objOfficers) {
            space_utils.notifyObject(((obj_id) objOfficer), "targetDisabled", params);
            space_utils.sendSystemMessageShip(self, SID_TARGET_DISABLED, true, true, true, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int disableSelf(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objAttacker = params.getObjId("objShip");
        float fltRemainingDamage = space_combat.doComponentDamage(objAttacker, self, 0, ship_chassis_slot_type.SCST_reactor, 500000, 0);
        return SCRIPT_CONTINUE;
    }
    public int megaDamage(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objAttacker = params.getObjId("objShip");
        if (space_utils.isPlayerControlledShip(objAttacker))
        {
            obj_id objPilot = getPilotId(objAttacker);
            if (isIdValid(objPilot))
            {
                if (!isGod(objPilot))
                {
                    if (!utils.hasLocalVar(self, "intEjecting"))
                    {
                        return SCRIPT_CONTINUE;
                    }
                    else 
                    {
                        CustomerServiceLog("space_death", "%TU " + objAttacker + " is EJECTING!", getOwner(objAttacker));
                        utils.removeLocalVar(self, "intEjecting");
                        float fltDamage = getShipMaximumChassisHitPoints(self) * 0.20f;
                        space_combat.doChassisDamage(objAttacker, self, 1, fltDamage);
                        obj_id objDefenderPilot = getPilotId(self);
                        if (!isIdValid(objDefenderPilot))
                        {
                            objDefenderPilot = getOwner(self);
                        }
                        space_combat.sendDestructionNotification(self, objAttacker);
                        float fltIntensity = rand(0, 1.0f);
                        handleShipDestruction(self, fltIntensity);
                        messageTo(self, "killSpacePlayer", null, space_combat.SPACE_DEATH_DELAY, true);
                        space_combat.doDeathCleanup(self);
                    }
                }
            }
        }
        float fltDamage = 200000;
        int intSide = 1;
        int intTargetedComponent = 112;
        int intWeaponSlot = space_crafting.WEAPON_0;
        space_combat.doArmorDamage(objAttacker, self, intWeaponSlot, fltDamage, intSide);
        space_combat.doComponentDamage(objAttacker, self, intWeaponSlot, intTargetedComponent, fltDamage, intSide);
        space_combat.doChassisDamage(objAttacker, self, 0, fltDamage);
        if (!space_utils.isPlayerControlledShip(self))
        {
            space_combat.grantRewardsAndCreditForKills(self);
            space_combat.targetDestroyed(self);
            return SCRIPT_CONTINUE;
        }
        else 
        {
            obj_id objDefenderPilot = getPilotId(self);
            if (!isIdValid(objDefenderPilot))
            {
                objDefenderPilot = getOwner(self);
            }
            if (space_utils.isPlayerControlledShip(objAttacker))
            {
                utils.setScriptVar(self, "intPVPKill", 1);
            }
            space_combat.setDeathFlags(self);
            dictionary dctParams = new dictionary();
            dctParams.put("objAttacker", objAttacker);
            dctParams.put("objShip", self);
            space_utils.notifyObject(objDefenderPilot, "playerShipDestroyed", dctParams);
            float fltIntensity = rand(0, 1.0f);
            handleShipDestruction(self, fltIntensity);
            messageTo(self, "killSpacePlayer", null, space_combat.SPACE_DEATH_DELAY, true);
            space_combat.doDeathCleanup(self);
            CustomerServiceLog("space_death", "%TU " + self + " Has been killed by " + objAttacker, getOwner(objAttacker));
            if (space_battlefield.isInBattlefield(self))
            {
                CustomerServiceLog("battlefield", "%TU " + self + " Has been killed by " + objAttacker, getOwner(objAttacker));
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int OnTryToEquipDroidControlDeviceInShip(obj_id self, obj_id objPlayer, obj_id objControlDevice) throws InterruptedException
    {
        if (!isIdValid(objControlDevice))
        {
            return SCRIPT_CONTINUE;
        }
        if (space_crafting.isUsableAstromechPet(objControlDevice))
        {
            if (space_crafting.isCertifiedForAstromech(objControlDevice, objPlayer))
            {
                if (space_crafting.isUsingCorrectComputer(objControlDevice, self))
                {
                    if (isShipSlotInstalled(self, ship_chassis_slot_type.SCST_droid_interface))
                    {
                        if (!isShipComponentDisabled(self, ship_chassis_slot_type.SCST_droid_interface))
                        {
                            associateDroidControlDeviceWithShip(self, objControlDevice);
                        }
                        else 
                        {
                            string_id strSpam = new string_id("space/space_interaction", "droid_interface_disabled");
                            sendSystemMessage(objPlayer, strSpam);
                            associateDroidControlDeviceWithShip(self, objControlDevice);
                        }
                        return SCRIPT_CONTINUE;
                    }
                    else 
                    {
                        associateDroidControlDeviceWithShip(self, objControlDevice);
                        string_id strSpam = new string_id("space/space_interaction", "no_droid_command_module");
                        sendSystemMessage(objPlayer, strSpam);
                        return SCRIPT_CONTINUE;
                    }
                }
                else 
                {
                    if (hasObjVar(objControlDevice, "pet.creatureName"))
                    {
                        string_id strSpam = new string_id("space/space_interaction", "need_flight_computer");
                        sendSystemMessage(objPlayer, strSpam);
                    }
                    else 
                    {
                        string_id strSpam = new string_id("space/space_interaction", "need_astromech");
                        sendSystemMessage(objPlayer, strSpam);
                    }
                }
            }
            else 
            {
                string_id strSpam = new string_id("space/space_interaction", "droid_not_certified");
                sendSystemMessage(objPlayer, strSpam);
                return SCRIPT_CONTINUE;
            }
        }
        else 
        {
            string_id strSpam = new string_id("space/space_interaction", "not_an_astromech_for_space");
            sendSystemMessage(objPlayer, strSpam);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnShipComponentUninstalling(obj_id self, obj_id uninstallerId, int intSlot, obj_id targetContainer) throws InterruptedException
    {
        if (intSlot == ship_component_type.SCT_droid_interface)
        {
            obj_id objDroidControlDevice = getDroidControlDeviceForShip(self);
            if (isIdValid(objDroidControlDevice))
            {
                removeDroidControlDeviceFromShip(self);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int OnShipComponentUninstalled(obj_id self, obj_id uninstallerId, obj_id componentId, int slot, obj_id targetContainer) throws InterruptedException
    {
        if (slot == ship_chassis_slot_type.SCST_cargo_hold)
        {
            removeObjVar(self, "ship_comp.cargo_hold.contents_types");
            removeObjVar(self, "ship_comp.cargo_hold.contents_amounts");
            setObjVar(self, "ship_comp.cargo_hold.contents_current", (int)0);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnDroppedItemOntoShipComponent(obj_id self, int intSlot, obj_id objItem, obj_id objPlayer) throws InterruptedException
    {
        if (hasObjVar(objItem, "weapon.intAmmoType"))
        {
            if (space_crafting.isWeaponAmmo(objItem))
            {
                if (space_crafting.isProperAmmoForWeapon(objItem, self, intSlot))
                {
                    space_crafting.applyAmmoToWeapon(self, objItem, intSlot, objPlayer, true);
                }
                else 
                {
                    string_id strSpam = new string_id("space/space_interaction", "no_ammo_allowed");
                    sendSystemMessage(objPlayer, strSpam);
                    return SCRIPT_CONTINUE;
                }
            }
            else 
            {
                string_id strSpam = new string_id("space/space_interaction", "not_missile_ammo");
                sendSystemMessage(objPlayer, strSpam);
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int notifyOnDestroy(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id obj = params.getObjId("object");
        if (!isIdValid(obj))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id[] notifyobjs = getObjIdArrayObjVar(self, "destroynotify");
        obj_id[] newnotifyobjs = null;
        if (notifyobjs == null)
        {
            newnotifyobjs = new obj_id[1];
            newnotifyobjs[0] = obj;
        }
        else 
        {
            newnotifyobjs = new obj_id[notifyobjs.length + 1];
            for (int i = 0; i < notifyobjs.length; i++)
            {
                newnotifyobjs[i] = notifyobjs[i];
            }
            newnotifyobjs[notifyobjs.length] = obj;
        }
        setObjVar(self, "destroynotify", newnotifyobjs);
        return SCRIPT_CONTINUE;
    }
    public int OnShipDisabled(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objAttacker = params.getObjId("objAttacker");
        if (hasScript(self, "space.ai.space_ai"))
        {
            ship_ai.unitIdle(self);
            ship_ai.unitSetAttackOrders(self, ship_ai.ATTACK_ORDERS_HOLD_FIRE);
            detachScript(self, "space.ai.space_ai");
            dictionary outparams = new dictionary();
            outparams.put("attacker", objAttacker);
            messageTo(self, "selfDestruct", outparams, 120.0f + rand() * 60.0f, false);
            if (!hasObjVar(self, "objMissionOwner"))
            {
                setObjVar(self, "objMissionOwner", getPilotId(objAttacker));
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int selfDestruct(obj_id self, dictionary params) throws InterruptedException
    {
        removeObjVar(self, "objMissionOwner");
        if (utils.hasScriptVar(self, "being_docked"))
        {
            messageTo(self, "selfDestruct", params, 30.0f, false);
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "selfDestruct", 1);
        obj_id attacker = params.getObjId("attacker");
        if (isIdValid(attacker))
        {
            if ((!exists(attacker) || (!attacker.isLoaded())))
            {
                attacker = self;
            }
        }
        space_combat.doChassisDamage(attacker, self, 0, 45000000);
        space_combat.targetDestroyed(self);
        return SCRIPT_CONTINUE;
    }
    public int reactorPumpPulseTimeout(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        int pumpPulseLoops = params.getInt("loops");
        obj_id pilot = params.getObjId("pilot");
        debugServerConsoleMsg(null, "+++ SPACE COMMAND . combat_ship.reactorPumpPulseTimeout +++ ARRIVED in messagehandler. ObjID of ship was: " + ship + " objID of pilot was: " + pilot + " number of loops was: " + pumpPulseLoops);
        if (pumpPulseLoops > 1)
        {
            string_id strSpam = new string_id("space/space_interaction", "power_spike");
            space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
            --pumpPulseLoops;
            params.put("ship", ship);
            params.put("loops", pumpPulseLoops);
            params.put("pilot", pilot);
            messageTo(self, "reactorPumpPulseTimeout", params, 5.0f, false);
        }
        else if (pumpPulseLoops == 1)
        {
            string_id strSpam = new string_id("space/space_interaction", "reactor_normalizing");
            space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
            --pumpPulseLoops;
            params.put("ship", ship);
            params.put("loops", pumpPulseLoops);
            params.put("pilot", pilot);
            messageTo(self, "reactorPumpPulseTimeout", params, 5.0f, false);
        }
        else 
        {
            if (isIdValid(ship))
            {
                string_id strSpam = new string_id("space/space_interaction", "reactor_stabilized");
                space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
                space_pilot_command.allPurposeShipComponentReset(ship);
                utils.removeScriptVar(pilot, "cmd.reactorPumpPulse");
            }
            else 
            {
                debugServerConsoleMsg(null, "+++ MH reactorPumpPulseTimeout . obj_id of the ship passed into the reactor reset function doesn't come back as valid. What the!?.");
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int unScramReactor(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        int scramLoops = params.getInt("loops");
        obj_id pilot = params.getObjId("pilot");
        if (scramLoops > 1)
        {
            string_id strSpam = new string_id("space/space_interaction", "reactor_standby");
            space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
            String cefPlayBackHardpoint = space_combat.targetHardpointForCefPlayback(ship);
            playClientEffectObj(pilot, "clienteffect/space_command/scram_reactor_shutdown_alarm.cef", ship, cefPlayBackHardpoint);
            --scramLoops;
            params.put("ship", ship);
            params.put("loops", scramLoops);
            params.put("pilot", pilot);
            messageTo(self, "unScramReactor", params, 5.0f, false);
        }
        else if (scramLoops == 1)
        {
            string_id strSpam = new string_id("space/space_interaction", "beginning_reactor_restart");
            space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
            String cefPlayBackHardpoint = space_combat.targetHardpointForCefPlayback(ship);
            playClientEffectObj(pilot, "clienteffect/space_command/scram_reactor_startup_engine.cef", ship, cefPlayBackHardpoint);
            --scramLoops;
            params.put("ship", ship);
            params.put("loops", scramLoops);
            params.put("pilot", pilot);
            messageTo(self, "unScramReactor", params, 8.0f, false);
        }
        else 
        {
            space_pilot_command.allPurposeShipComponentReset(ship);
            utils.removeScriptVar(pilot, "cmd.reactorPumpPulse");
        }
        return SCRIPT_CONTINUE;
    }
    public int OnShipComponentPowerSufficient(obj_id self, int intSlot, float fltPowerReceived) throws InterruptedException
    {
        obj_id objPilot = getPilotId(self);
        float fltCurrentHitPoints = getShipComponentHitpointsCurrent(self, intSlot);
        if (fltCurrentHitPoints > 0)
        {
            space_utils.setComponentDisabled(self, intSlot, false);
            space_combat.recalculateEfficiency(intSlot, self);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnShipComponentPowerInsufficient(obj_id self, int intSlot, float fltPowerRequired, float fltPowerReceived) throws InterruptedException
    {
        if (fltPowerRequired == 0)
        {
            fltPowerRequired = 1.0f;
        }
        float fltTest = fltPowerReceived / fltPowerRequired;
        if (fltTest < space_combat.MINIMUM_EFFICIENCY)
        {
            space_utils.setComponentDisabled(self, intSlot, true);
            setShipComponentDisabledNeedsPower(self, intSlot, true);
        }
        else 
        {
            space_combat.recalculateEfficiencyGeneral(intSlot, self, fltTest);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnShipComponentInstalling(obj_id self, obj_id installerId, obj_id componentId, int slot) throws InterruptedException
    {
        obj_id owner = getOwner(self);
        if (isIdValid(owner))
        {
            if (!hasCertificationsForItem(owner, self))
            {
                string_id strSpam = new string_id("space/space_interaction", "certification_ship_none");
                space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
                return SCRIPT_OVERRIDE;
            }
            else 
            {
                if (!hasCertificationsForItem(owner, componentId))
                {
                    string_id strSpam = new string_id("space/space_interaction", "certification_ordnance_none");
                    space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
                    return SCRIPT_OVERRIDE;
                }
            }
            if (!isGameObjectTypeOf(componentId, GOT_ship_component_modification))
            {
                if (hasObjVar(componentId, "ship_comp.mass"))
                {
                    float componentMass = space_crafting.getComponentMass(componentId);
                    if (componentMass == 0)
                    {
                        string_id strSpam = new string_id("space/space_interaction", "installing_zero_mass_component");
                        space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
                        return SCRIPT_OVERRIDE;
                    }
                }
            }
        }
        else 
        {
            debugServerConsoleMsg(null, "+++ COMBAT_SHIP.OnShipComponentInstalling +++ Unable to find ships owner, so cannot check certifications. What the!?.");
        }
        return SCRIPT_CONTINUE;
    }
    public int OnShipComponentInstalled(obj_id self, obj_id objInstaller, int intSlot) throws InterruptedException
    {
        if (intSlot == space_crafting.ENGINE)
        {
            space_crafting.setupChassisDifferentiation(self);
        }
        space_pilot_command.allPurposeShipComponentReset(self);
        return SCRIPT_CONTINUE;
    }
    public int OnShipFiredCountermeasure(obj_id self, int intWeaponIndex, obj_id objPlayer) throws InterruptedException
    {
        int intSlot = intWeaponIndex + ship_chassis_slot_type.SCST_weapon_first;
        float fltMinDefense = getShipWeaponDamageMaximum(self, intSlot);
        float fltMaxDefense = getShipWeaponDamageMaximum(self, intSlot);
        float fltRoll = rand(fltMinDefense, fltMaxDefense);
        int intMissile = getNearestUnlockedMissileForTarget(self);
        if (intMissile == 0)
        {
            launchCountermeasure(self, 0, false, 0);
        }
        else 
        {
            if (fltRoll > getMissileDefenseRoll(intMissile))
            {
                launchCountermeasure(self, intMissile, true, 0);
            }
            else 
            {
                launchCountermeasure(self, intMissile, false, 0);
            }
        }
        applyFiredWeaponsSkillMod(self);
        return SCRIPT_CONTINUE;
    }
    public int OnShipFiredMissile(obj_id self, int intMissileId, int intWeaponIndex, int intMissileType, obj_id objPilot, obj_id objDefender, int intTargetedSlot) throws InterruptedException
    {
        int intSlot = intWeaponIndex + ship_chassis_slot_type.SCST_weapon_first;
        if (isIdValid(objPilot))
        {
            if (utils.checkConfigFlag("ScriptFlags", "e3Demo"))
            {
                setShipWeaponAmmoCurrent(self, intSlot, getShipWeaponAmmoCurrent(self, intSlot));
                return SCRIPT_CONTINUE;
            }
            applyFiredWeaponsSkillMod(self);
        }
        return SCRIPT_CONTINUE;
    }
    public int getMissileDefenseRoll(int intMissileId) throws InterruptedException
    {
        int intMissileType = getTypeByMissile(intMissileId);
        dictionary dctRow = dataTableGetRow("datatables/space/missiles.iff", intMissileType);
        return (dctRow.getInt("intCountermeasureDifficulty"));
    }
    public int flightDroidVocalize(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        int vocalizePriority = params.getInt("vocalizePriority");
        if (space_utils.isPlayerControlledShip(self))
        {
            space_combat.flightDroidVocalize(ship, vocalizePriority);
        }
        return SCRIPT_CONTINUE;
    }
    public int emergencyPowerTimeout(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("ship");
        obj_id pilot = params.getObjId("pilot");
        float emergencyPowerTime = params.getFloat("emergencyPowerTime");
        if (!isIdValid(ship))
        {
            debugServerConsoleMsg(null, "+++ MH emergencyPowerTimeout . obj_id of the ship passed into the emergency power timeout function doesn't come back as valid. What the!?.");
            return SCRIPT_CONTINUE;
        }
        string_id strSpam = new string_id("space/space_interaction", "emergency_reset");
        space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
        String cefPlayBackHardpoint = space_combat.targetHardpointForCefPlayback(ship);
        playClientEffectObj(self, "clienteffect/space_command/emergency_power_off.cef", ship, cefPlayBackHardpoint);
        space_pilot_command.allPurposeShipComponentReset(ship);
        if (utils.hasLocalVar(ship, "cmd.emergWeapon"))
        {
            utils.removeLocalVar(ship, "cmd.emergWeapon");
        }
        if (utils.hasLocalVar(ship, "cmd.emergShields"))
        {
            utils.removeLocalVar(ship, "cmd.emergShields");
        }
        if (utils.hasLocalVar(ship, "cmd.emergThrust"))
        {
            utils.removeLocalVar(ship, "cmd.emergThrust");
        }
        return SCRIPT_CONTINUE;
    }
    public void applyFiredWeaponsSkillMod(obj_id ship) throws InterruptedException
    {
        if (!utils.hasLocalVar(ship, "cmd.firedWeaponsSkillMod"))
        {
            int time = getGameTime();
            utils.setLocalVar(ship, "cmd.firedWeaponsSkillMod", SHIP_FIRED_SKILLMOD_PENALTY_TIME + time);
        }
        return;
    }
    public int componentsStunned(obj_id self, dictionary params) throws InterruptedException
    {
        Vector stunnedComponents = params.getResizeableIntArray("stunned_components");
        int stunDuration = params.getInt("stun_loops");
        obj_id pilot = null;
        String cefPlayBackHardpoint = space_combat.targetHardpointForCefPlayback(self);
        boolean boolPlayerShip = false;
        if (!space_utils.isPlayerControlledShip(self))
        {
            boolPlayerShip = true;
        }
        if (stunDuration > 20)
        {
            if (boolPlayerShip)
            {
                playClientEffectObj(self, "clienteffect/space_command/cbt_impact_emp_hvy.cef", self, "");
                if (stunnedComponents.size() > 1)
                {
                    string_id strSpam = new string_id("space/space_pilot_command", "multiple_systems_disrupted");
                    space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
                }
                else 
                {
                    string_id strSpam = new string_id("space/space_pilot_command", "system_disrupted");
                    space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
                }
            }
            else 
            {
                playClientEffectObj(self, "clienteffect/space_command/cbt_impact_emp_hvy_noshake.cef", self, "");
            }
        }
        else if (stunDuration < 20 && stunDuration > 0)
        {
            if (boolPlayerShip)
            {
                string_id strSpam = new string_id("space/space_pilot_command", "disrupted_standby");
                space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
            }
        }
        else 
        {
            if (boolPlayerShip)
            {
                string_id strSpam = new string_id("space/space_pilot_command", "sub_system_restart");
                space_utils.sendSystemMessageShip(self, strSpam, true, false, true, true);
            }
            for (Object stunnedComponent : stunnedComponents) {
                space_utils.setComponentDisabled(self, (Integer) stunnedComponent, false);
                space_combat.recalculateEfficiency((Integer) stunnedComponent, self);
            }
            return SCRIPT_CONTINUE;
        }
        stunDuration--;
        params.put("stunned_components", stunnedComponents);
        params.put("stun_loops", stunDuration);
        messageTo(self, "componentsStunned", params, STUNNED_COMPONENT_LOOP_TIME, false);
        return SCRIPT_CONTINUE;
    }
    public int vRepairDamageCEFLoop(obj_id self, dictionary params) throws InterruptedException
    {
        int damageLoops = params.getInt("damage_loops");
        obj_id pilot = params.getObjId("pilot");
        debugServerConsoleMsg(null, "vRepairDamageCEFLoop **********  just entered message handler. Recieved number of loops of: " + damageLoops + " and pilot objId of: " + pilot);
        String cefPlayBackHardpoint = space_combat.targetHardpointForCefPlayback(self);
        String clientEffect = space_pilot_command.randomWeldingCEFPicker();
        debugServerConsoleMsg(null, "vRepairDamageCEFLoop **********  cef chosen for playback is " + clientEffect);
        transform t = new transform();
        t = t.move_p(new vector(0.0f, 1.7f, 0.0f));
        debugServerConsoleMsg(null, "vRepairDamageCEFLoop **********  DID CRAZY TRANSFORM STUFF. HERE COMES THE EFFECT!!! ");
        if (!playClientEffectObj(pilot, clientEffect, self, null, t))
        {
            debugServerConsoleMsg(null, "vRepairDamageCEFLoop **********  FAILED TO PLAYBACK CEF ");
        }
        damageLoops--;
        if (damageLoops > 0)
        {
            params.put("damage_loops", damageLoops);
            params.put("pilot", pilot);
            messageTo(self, "vRepairDamageCEFLoop", params, 3.0f, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnSpaceUnitDocked(obj_id self, obj_id target) throws InterruptedException
    {
        obj_id objPilot = getPilotId(self);
        if (!space_utils.isPlayerControlledShip(self))
        {
            return SCRIPT_CONTINUE;
        }
        dictionary outparams = new dictionary();
        outparams.put("target", target);
        space_utils.notifyObject(objPilot, "spaceUnitDocked", outparams);
        return SCRIPT_CONTINUE;
    }
    public int OnSpaceUnitUnDocked(obj_id self, obj_id target, boolean dockSuccessful) throws InterruptedException
    {
        obj_id objPilot = getPilotId(self);
        if(!isValidId(objPilot) || !exists(objPilot)){
            return SCRIPT_CONTINUE;
        }
        if (!space_utils.isPlayerControlledShip(self))
        {
            return SCRIPT_CONTINUE;
        }
        dictionary outparams = new dictionary();
        outparams.put("target", target);
        if (dockSuccessful)
        {
            space_utils.notifyObject(objPilot, "spaceUnitUnDocked", outparams);
        }
        else 
        {
            space_utils.notifyObject(objPilot, "spaceUnitDockingFailed", outparams);
        }
        return SCRIPT_CONTINUE;
    }
    public int openComm(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objStation = params.getObjId("objStation");
        obj_id objPilot = getPilotId(self);
        queueCommand(objPilot, (80588750), objStation, "0   ", COMMAND_PRIORITY_FRONT);
        return SCRIPT_CONTINUE;
    }
    public int OnSpeaking(obj_id self, String strText) throws InterruptedException
    {
        obj_id objPilot = getPilotId(self);
        if (isIdValid(objPilot))
        {
            if (isGod(objPilot) || (utils.checkConfigFlag("scriptFlags", "e3Demo")))
            {
                Object[] newParams = new Object[2];
                newParams[0] = objPilot;
                newParams[1] = strText;
                space_utils.callTrigger("OnSpeaking", newParams);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int checkSpacePVPStatus(obj_id self, dictionary params) throws InterruptedException
    {
        space_transition.updatePVPStatus(self);
        return SCRIPT_CONTINUE;
    }
    public int destroySelf(obj_id self, dictionary params) throws InterruptedException
    {
        setObjVar(self, "intCleaningUp", 1);
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
    public static final float ATMOSPHERIC_WARN_ALTITUDE = 4000.0f;
    public static final float ATMOSPHERIC_WARP_ALTITUDE = 5000.0f;
    public int checkAtmosphericAltitude(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;
        obj_id pilot = getPilotId(self);
        if (!isIdValid(pilot))
            return SCRIPT_CONTINUE;
        location shipLoc = getLocation(self);
        float altitude = shipLoc.y;
        if (altitude >= ATMOSPHERIC_WARP_ALTITUDE)
        {
            location spaceDest = space_transition.getSpaceZoneForGroundScene(shipLoc.area);
            if (spaceDest == null)
            {
                sendSystemMessage(pilot, "No adjacent space zone found for this planet.", null);
                messageTo(self, "checkAtmosphericAltitude", null, 2.0f, false);
                return SCRIPT_CONTINUE;
            }
            removeObjVar(self, "space.altitude.warned");
            hyperspacePlayerToLocation(pilot, spaceDest.area, spaceDest.x, spaceDest.y, spaceDest.z, null, spaceDest.x, spaceDest.y, spaceDest.z, null, false);
            return SCRIPT_CONTINUE;
        }
        if (altitude >= ATMOSPHERIC_WARN_ALTITUDE)
        {
            if (!hasObjVar(self, "space.altitude.warned"))
            {
                setObjVar(self, "space.altitude.warned", true);
                sendSystemMessage(pilot, "\\#ff4444You are reaching the lithosphere, make the ascent or turn back now!", null);
            }
        }
        else
        {
            if (hasObjVar(self, "space.altitude.warned"))
                removeObjVar(self, "space.altitude.warned");
        }
        messageTo(self, "checkAtmosphericAltitude", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // Server-Side Atmospheric Auto-Pilot (physics-driven)
    // Separate from planet-map autopilot: planet map uses sendAutoPilotEngage(player)
    // and vehicle_base objvars (autopilot.active). This path uses shipSetAutopilotTarget(ship)
    // and space.autopilot.* objvars; used by summon_ship and NPC POB shuttle.
    // =====================================================================

    public static final String OV_AUTOPILOT_ROOT     = "space.autopilot";
    public static final String OV_AUTOPILOT_ACTIVE    = "space.autopilot.active";
    public static final String OV_AUTOPILOT_TARGET_X  = "space.autopilot.targetX";
    public static final String OV_AUTOPILOT_TARGET_Z  = "space.autopilot.targetZ";
    public static final String OV_AUTOPILOT_OWNER     = "space.autopilot.owner";
    public static final String OV_AUTOPILOT_TICKS     = "space.autopilot.ticks";
    public static final String OV_AUTOPILOT_LAST_PHASE = "space.autopilot.lastPhase";
    public static final float  AUTOPILOT_TAKEOFF_ALT  = 500.0f;
    public static final float  AUTOPILOT_LANDING_ALT  = 50.0f;
    public static final float  AUTOPILOT_MONITOR_RATE = 2.0f;
    public static final int    AUTOPILOT_STATUS_INTERVAL = 10;
    /** Max tick count to wait for engine to report autopilot active after shipSetAutopilotTarget (server-driven only). Planet map uses client sendAutoPilotEngage and does not use this tick. */
    private static final int   AUTOPILOT_WAIT_ACTIVE_TICKS = 5;

    public static final int AP_NONE       = 0;
    public static final int AP_ASCENDING  = 1;
    public static final int AP_CRUISING   = 2;
    public static final int AP_DESCENDING = 3;
    public static final int AP_ARRIVED    = 4;

    public static final String SND_COMM    = "sound/sys_comm_generic.snd";
    public static final String SND_ALARM   = "sound/cbt_msl_alarm_incoming.snd";
    /** Vertical m/s for atmospheric autopilot ETA math; keep in sync with {@code PlayerShipController} autopilot elevator speed. */
    public static final float  ELEVATOR_SPEED = 60.0f;
    private static final float SHUTTLE_LOG_RANGE = 2000.0f;

    /**
     * Finishes atmospheric autopilot when the destination is reached: summon messaging, landing-pad handoff, clears script autopilot state.
     * Safe to call when {@code atmo.landing.landed_at} is already set (landing handoff becomes a no-op for pad vars).
     */
    private void finishAutopilotArrival(obj_id self, location shipLoc) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE) && !hasObjVar(self, "atmo.landing.target") && !hasObjVar(self, OV_SUMMON_OWNER))
            return;

        shipClearAutopilot(self);

        boolean wasSummon = hasObjVar(self, OV_SUMMON_OWNER);
        obj_id summonOwner = wasSummon ? getObjIdObjVar(self, OV_SUMMON_OWNER) : null;
        boolean wasLandingPoint = hasObjVar(self, "atmo.landing.target");
        obj_id landingTarget = wasLandingPoint ? getObjIdObjVar(self, "atmo.landing.target") : null;

        playSoundOnShipOccupants(self, SND_COMM);

        broadcastToShip(self, " ");
        broadcastToShip(self, "\\#00ff88========================================");
        broadcastToShip(self, "\\#00ff88[Navicomputer]: Destination reached. Auto-pilot disengaged.");
        broadcastToShip(self, "\\#00ff88========================================");
        broadcastToShip(self, " ");
        broadcastToShip(self, "\\#88ddaa  Coordinates: [" + formatCoord(shipLoc.x) + ", " + formatCoord(shipLoc.y) + ", " + formatCoord(shipLoc.z) + "]");
        broadcastToShip(self, " ");

        if (wasSummon)
        {
            boolean quietArr = hasObjVar(self, OV_SUMMON_QUIET_ARRIVAL) && getBooleanObjVar(self, OV_SUMMON_QUIET_ARRIVAL);
            removeObjVar(self, OV_SUMMON_QUIET_ARRIVAL);
            removeObjVar(self, OV_SUMMON_CUR_TAKEOFF);
            removeObjVar(self, OV_SUMMON_CUR_LANDING);

            if (!quietArr)
            {
                broadcastToShip(self, "\\#88ddaa  The ship has arrived at the summoned location.");
                if (isIdValid(summonOwner) && exists(summonOwner))
                {
                    play2dNonLoopingSound(summonOwner, SND_COMM);
                    sendSystemMessageTestingOnly(summonOwner, "\\#00ff88[Navicomputer]: Your ship has arrived at your location.");
                    sendSystemMessageTestingOnly(summonOwner, "\\#88ddaa  Coordinates: [" + formatCoord(shipLoc.x) + ", " + formatCoord(shipLoc.y) + ", " + formatCoord(shipLoc.z) + "]");
                    sendSystemMessageTestingOnly(summonOwner, "\\#88ddaa  You may now board your ship.");
                }
            }
            else if (isIdValid(summonOwner) && exists(summonOwner))
            {
                sendSystemMessageTestingOnly(summonOwner, "\\#778899[Navicomputer]: Holding follow-orbit position.");
            }

            if (isIdValid(summonOwner) && exists(summonOwner)
                && hasObjVar(self, OV_SUMMON_FOLLOW_ACTIVE) && getBooleanObjVar(self, OV_SUMMON_FOLLOW_ACTIVE))
            {
                if (!quietArr)
                    sendSystemMessageTestingOnly(summonOwner, "\\#aaddff[Navicomputer]: Auto-follow active — high orbit near you (" + SUMMON_FOLLOW_COST_PER_HOUR + " cr/hr).");
                messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
            }
        }
        else if (wasLandingPoint && isIdValid(landingTarget) && exists(landingTarget))
        {
            if (!hasObjVar(self, "atmo.landing.landed_at"))
            {
                String landingName = hasObjVar(self, "atmo.landing.name") ? getStringObjVar(self, "atmo.landing.name") : "Landing Pad";
                broadcastToShip(self, "\\#88ddaa  Welcome to " + landingName + ".");
                broadcastToShip(self, "\\#88ddaa   You have successfully landed.");

                dictionary arrivedParams = new dictionary();
                arrivedParams.put("ship", self);
                messageTo(landingTarget, "handleShipArrived", arrivedParams, 0, false);

                removeObjVar(self, "atmo.landing.target");
                removeObjVar(self, "atmo.landing.yaw");
                removeObjVar(self, "atmo.landing.name");
            }
        }
        else
        {
            broadcastToShip(self, "\\#88ddaa  We have arrived at our destination. You may now disembark");
            broadcastToShip(self, "\\#88ddaa   or land the vessel. Thank you for flying with us.");
        }
        broadcastToShip(self, " ");

        removeObjVar(self, OV_AUTOPILOT_ROOT);
        if (utils.hasScriptVar(self, "combat_ship.shuttleTickLogged"))
            utils.removeScriptVar(self, "combat_ship.shuttleTickLogged");
    }

    private void broadcastToShip(obj_id ship, String message) throws InterruptedException
    {
        Vector players = space_transition.getContainedPlayers(ship, null);
        if (players != null)
        {
            for (Object p : players)
            {
                obj_id player = (obj_id) p;
                if (isIdValid(player))
                    sendSystemMessageTestingOnly(player, message);
            }
        }
    }

    private void playSoundOnShipOccupants(obj_id ship, String sound) throws InterruptedException
    {
        Vector players = space_transition.getContainedPlayers(ship, null);
        if (players != null)
        {
            for (Object p : players)
            {
                obj_id player = (obj_id) p;
                if (isIdValid(player))
                    play2dNonLoopingSound(player, sound);
            }
        }
    }

    private String formatCoord(float v)
    {
        return String.valueOf(Math.round(v));
    }

    private float getDirectionBearing(float dx, float dz)
    {
        double rad = StrictMath.atan2(dx, dz);
        float deg = (float)(rad * (180.0 / Math.PI));
        if (deg < 0) deg += 360.0f;
        return deg;
    }

    private String getBearingCardinal(float bearing)
    {
        if (bearing >= 337.5f || bearing < 22.5f)   return "North";
        if (bearing >= 22.5f  && bearing < 67.5f)   return "North-East";
        if (bearing >= 67.5f  && bearing < 112.5f)  return "East";
        if (bearing >= 112.5f && bearing < 157.5f)  return "South-East";
        if (bearing >= 157.5f && bearing < 202.5f)  return "South";
        if (bearing >= 202.5f && bearing < 247.5f)  return "South-West";
        if (bearing >= 247.5f && bearing < 292.5f)  return "West";
        return "North-West";
    }

    private String formatETA(float distMeters, float speedMps)
    {
        if (speedMps <= 0.0f)
            return "calculating...";
        float seconds = distMeters / speedMps;
        int mins = (int)(seconds / 60.0f);
        int secs = (int)(seconds % 60.0f);
        if (mins > 0)
            return mins + "m " + secs + "s";
        return secs + "s";
    }

    private String formatFullETA(float horizDist, float cruiseSpeed, float takeoffAlt, float landingAlt)
    {
        float ascentTime = takeoffAlt / ELEVATOR_SPEED;
        float descentTime = (takeoffAlt - landingAlt) / ELEVATOR_SPEED;
        float cruiseTime = (cruiseSpeed > 0.0f) ? (horizDist / cruiseSpeed) : 0.0f;
        float totalSeconds = ascentTime + cruiseTime + descentTime;
        int mins = (int)(totalSeconds / 60.0f);
        int secs = (int)(totalSeconds % 60.0f);
        if (mins > 0)
            return mins + "m " + secs + "s";
        return secs + "s";
    }

    public int shipAutoPilotEngage(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        // Block autopilot if ship is docked - must undock from terminal first
        if (hasObjVar(self, "atmo.landing.docked"))
        {
            obj_id owner = params.getObjId("owner");
            if (isIdValid(owner))
            {
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Cannot engage auto-pilot while docked.");
                sendSystemMessageTestingOnly(owner, "\\#ffaa44  Use the Starship Management Terminal to undock first.");
            }
            return SCRIPT_CONTINUE;
        }

        /*if (!space_utils.isShipWithInterior(self))
        {
            obj_id owner = params.getObjId("owner");
            if (isIdValid(owner))
                sendSystemMessageTestingOnly(owner, "Auto-pilot is only available on ships with an interior.");
            return SCRIPT_CONTINUE;
        }*/

        float targetX = params.getFloat("x");
        float targetZ = params.getFloat("z");
        obj_id owner = params.getObjId("owner");
        boolean npcControlled = params.getBoolean("npcControlled");

        // Use provided altitudes for NPC shuttles, otherwise use defaults
        float takeoffAlt = params.containsKey("takeoffAlt") ? params.getFloat("takeoffAlt") : AUTOPILOT_TAKEOFF_ALT;
        float landingAlt = params.containsKey("landingAlt") ? params.getFloat("landingAlt") : AUTOPILOT_LANDING_ALT;

        if (npcControlled)
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle combat_ship: shipAutoPilotEngage npcControlled=true targetX=" + targetX + " targetZ=" + targetZ + " takeoffAlt=" + takeoffAlt + " landingAlt=" + landingAlt);

        if (!npcControlled && (!isIdValid(owner) || getOwner(self) != owner))
        {
            if (isIdValid(owner))
                sendSystemMessageTestingOnly(owner, "Only the ship owner may engage the auto-pilot.");
            return SCRIPT_CONTINUE;
        }
        if (npcControlled)
            owner = isIdValid(getOwner(self)) ? getOwner(self) : self;

        if (hasObjVar(self, OV_AUTOPILOT_ACTIVE))
        {
            shipClearAutopilot(self);
            removeObjVar(self, OV_AUTOPILOT_ROOT);
            broadcastToShip(self, "\\#00ccff[Navicomputer]: Previous auto-pilot course cancelled. Recalculating...");
        }

        if (!shipSetAutopilotTarget(self, targetX, targetZ, takeoffAlt, landingAlt))
        {
            if (npcControlled)
                script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle combat_ship: shipSetAutopilotTarget FAILED");
            sendSystemMessageTestingOnly(owner, "Failed to engage auto-pilot on this ship.");
            return SCRIPT_CONTINUE;
        }

        if (npcControlled)
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle combat_ship: shipSetAutopilotTarget ok, scheduling tick in " + AUTOPILOT_MONITOR_RATE + "s");

        setObjVar(self, OV_AUTOPILOT_ACTIVE, true);
        setObjVar(self, OV_AUTOPILOT_TARGET_X, targetX);
        setObjVar(self, OV_AUTOPILOT_TARGET_Z, targetZ);
        setObjVar(self, OV_AUTOPILOT_OWNER, owner);
        setObjVar(self, OV_AUTOPILOT_TICKS, 0);
        setObjVar(self, OV_AUTOPILOT_LAST_PHASE, AP_NONE);

        location shipLoc = getLocation(self);
        float dx = targetX - shipLoc.x;
        float dz = targetZ - shipLoc.z;
        float dist = (float) StrictMath.sqrt(dx * dx + dz * dz);
        float bearing = getDirectionBearing(dx, dz);
        String cardinal = getBearingCardinal(bearing);

        float estSpeed = getShipEngineSpeedMaximum(self) * 2.5f;
        if (estSpeed <= 0.0f)
            estSpeed = 50.0f;

        float terrainHere = getHeightAtLocation(shipLoc.x, shipLoc.z);
        float cruiseYTarget = terrainHere + takeoffAlt;
        float terrainAtDest = getHeightAtLocation(targetX, targetZ);
        float landingY = terrainAtDest + landingAlt;

        float climbM = cruiseYTarget - shipLoc.y;
        if (climbM < 0.0f)
            climbM = 0.0f;
        float descendM = cruiseYTarget - landingY;
        if (descendM < 0.0f)
            descendM = 0.0f;
        float tVert = (climbM + descendM) / ELEVATOR_SPEED;
        float cruiseTime = dist / estSpeed;
        int etaTotal = (int)(tVert + cruiseTime) + 5;
        int etaMins = etaTotal / 60;
        int etaSecs = etaTotal % 60;
        String eta = etaMins > 0 ? (etaMins + "m " + etaSecs + "s") : (etaSecs + "s");

        playSoundOnShipOccupants(self, SND_COMM);

        broadcastToShip(self, " ");
        broadcastToShip(self, "\\#00ccff========================================");
        broadcastToShip(self, "\\#00ccff[Navicomputer]: Auto-Pilot coordinates locked");
        broadcastToShip(self, "\\#00ccff  Destination: [" + formatCoord(targetX) + ", " + formatCoord(landingY) + ", " + formatCoord(targetZ) + "]");
        broadcastToShip(self, "\\#00ccff========================================");
        broadcastToShip(self, " ");
        broadcastToShip(self, "\\#aaddff  Bearing: " + formatCoord(bearing) + "\\#778899 deg \\#aaddff(" + cardinal + ")");
        broadcastToShip(self, "\\#aaddff  Distance: " + formatCoord(dist) + "m");
        broadcastToShip(self, "\\#aaddff  Est. Arrival: " + eta);
        broadcastToShip(self, " ");
        broadcastToShip(self, "\\#88bbdd   All hands, prepare for departure.");
        broadcastToShip(self, "\\#88bbdd   Ascending to cruise altitude " + formatCoord(cruiseYTarget) + "m...");
        broadcastToShip(self, " ");

        messageTo(self, "shipAutoPilotTick", null, AUTOPILOT_MONITOR_RATE, false);
        return SCRIPT_CONTINUE;
    }

    public int shipAutoPilotTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            return SCRIPT_CONTINUE;

        if (hasObjVar(self, "npc_pob.controller") && !utils.hasScriptVar(self, "combat_ship.shuttleTickLogged"))
        {
            utils.setScriptVar(self, "combat_ship.shuttleTickLogged", 1);
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle combat_ship: shipAutoPilotTick running (first time)");
        }

        int ticks = hasObjVar(self, OV_AUTOPILOT_TICKS) ? getIntObjVar(self, OV_AUTOPILOT_TICKS) : 0;
        int lastPhase = hasObjVar(self, OV_AUTOPILOT_LAST_PHASE) ? getIntObjVar(self, OV_AUTOPILOT_LAST_PHASE) : AP_NONE;

        boolean npcShuttle = hasObjVar(self, "npc_pob.controller");

        if (npcShuttle && ticks == 0) {
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG A: about to check autopilot active");
        }

        // For NPC shuttles, trust that shipSetAutopilotTarget succeeded and skip the engine check
        // Player ships still need to verify autopilot is active
        boolean autopilotActive = npcShuttle ? true : shipIsAutopilotActive(self);

        if (npcShuttle && ticks == 0) {
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG B: autopilotActive=" + autopilotActive + " (NPC shuttle, trusting engine)");
        }

        if (!autopilotActive)
        {
            if (npcShuttle && ticks == 0) {
                script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG C: entering wait block, lastPhase=" + lastPhase);
            }
            if (lastPhase != AP_NONE)
            {
                if (npcShuttle) script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG: canceling, lastPhase was " + lastPhase);
                int ep = shipGetAutopilotPhase(self);
                location loc = getLocation(self);
                if (ep == AP_ARRIVED)
                    finishAutopilotArrival(self, loc);
                else
                {
                    if (hasObjVar(self, "atmo.landing.target") && !hasObjVar(self, "atmo.landing.landed_at") && !hasObjVar(self, "atmo.landing.docked"))
                        atmo_landing_registry.detachShipFromLandingPoint(self);
                    removeObjVar(self, OV_AUTOPILOT_ROOT);
                }
                return SCRIPT_CONTINUE;
            }
            ticks++;
            setObjVar(self, OV_AUTOPILOT_TICKS, ticks);
            if (npcShuttle) script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG D: waiting, ticks=" + ticks + "/" + AUTOPILOT_WAIT_ACTIVE_TICKS);
            if (ticks >= AUTOPILOT_WAIT_ACTIVE_TICKS)
            {
                if (npcShuttle) script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG: timeout reached, clearing autopilot");
                removeObjVar(self, OV_AUTOPILOT_ROOT);
                return SCRIPT_CONTINUE;
            }
            messageTo(self, "shipAutoPilotTick", null, AUTOPILOT_MONITOR_RATE, false);
            if (npcShuttle) script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG E: rescheduled tick");
            return SCRIPT_CONTINUE;
        }

        if (npcShuttle) {
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG F: autopilot IS active, proceeding with phase logic");
        }

        if (!isAtmosphericFlightScene())
        {
            if (npcShuttle) script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG: no longer in atmo scene");
            shipAutoPilotCancelInternal(self, "Auto-pilot disengaged: no longer in atmospheric flight.");
            return SCRIPT_CONTINUE;
        }

        obj_id pilot = getPilotId(self);
        if (isIdValid(pilot))
        {
            // Allow autopilot while the ship owner who engaged it is in the pilot seat (planet map, etc.).
            boolean ownerIsPilot = hasObjVar(self, OV_AUTOPILOT_OWNER)
                && getObjIdObjVar(self, OV_AUTOPILOT_OWNER) == pilot;
            if (!ownerIsPilot)
            {
                shipAutoPilotCancelInternal(self, "Auto-pilot disengaged: a pilot has taken the helm.");
                return SCRIPT_CONTINUE;
            }
        }

        float targetX = getFloatObjVar(self, OV_AUTOPILOT_TARGET_X);
        float targetZ = getFloatObjVar(self, OV_AUTOPILOT_TARGET_Z);
        ticks++;
        setObjVar(self, OV_AUTOPILOT_TICKS, ticks);

        int phase = shipGetAutopilotPhase(self);
        if (npcShuttle && phase != lastPhase) {
            script_logs.logToGodsInRange(self, SHUTTLE_LOG_RANGE, "Shuttle DEBUG: phase change to " + phase);
        }

        location shipLoc = getLocation(self);
        float dx = targetX - shipLoc.x;
        float dz = targetZ - shipLoc.z;
        float horizDist = (float) StrictMath.sqrt(dx * dx + dz * dz);

        if (phase != lastPhase)
        {
            setObjVar(self, OV_AUTOPILOT_LAST_PHASE, phase);

            boolean summonQuiet = hasObjVar(self, OV_SUMMON_QUIET_ARRIVAL) && getBooleanObjVar(self, OV_SUMMON_QUIET_ARRIVAL);

            switch (phase)
            {
                case AP_ASCENDING:
                    if (!summonQuiet)
                    {
                        playSoundOnShipOccupants(self, SND_COMM);
                        broadcastToShip(self, "\\#aaddff[Navicomputer]: Ascending to cruise altitude...");
                    }
                    break;
                case AP_CRUISING:
                {
                    if (!summonQuiet)
                    {
                        float bearing = getDirectionBearing(dx, dz);
                        String cardinal = getBearingCardinal(bearing);
                        float estSpeed = getShipEngineSpeedMaximum(self) * 2.5f;
                        String eta = formatETA(horizDist, estSpeed);
                        playSoundOnShipOccupants(self, SND_COMM);
                        broadcastToShip(self, " ");
                        broadcastToShip(self, "\\#00ccff[Navicomputer]: Cruise altitude reached. Departing now.");
                        broadcastToShip(self, "\\#aaddff  Heading: " + formatCoord(bearing) + " deg (" + cardinal + ") | Distance: " + formatCoord(horizDist) + "m | ETA: " + eta);
                        broadcastToShip(self, " ");
                        broadcastToShip(self, "\\#88bbdd  We have reached cruise altitude. You are free to move about the cabin.");
                        broadcastToShip(self, " ");
                    }
                    break;
                }
                case AP_DESCENDING:
                    if (!summonQuiet)
                    {
                        playSoundOnShipOccupants(self, SND_COMM);
                        broadcastToShip(self, " ");
                        broadcastToShip(self, "\\#aaddff[Navicomputer]: Approaching destination. Beginning descent...");
                        broadcastToShip(self, " ");
                        broadcastToShip(self, "\\#88bbdd  Attention passengers, we are beginning our descent.");
                        broadcastToShip(self, "\\#88bbdd   Please prepare for arrival.");
                        broadcastToShip(self, " ");
                    }
                    break;
                case AP_ARRIVED:
                {
                    finishAutopilotArrival(self, shipLoc);
                    return SCRIPT_CONTINUE;
                }
            }
        }

        // Engine reported AP_ARRIVED without a phase delta vs last tick — still run arrival once.
        if (phase == AP_ARRIVED && hasObjVar(self, OV_AUTOPILOT_ACTIVE) && hasObjVar(self, "atmo.landing.target") && !hasObjVar(self, "atmo.landing.landed_at"))
        {
            finishAutopilotArrival(self, shipLoc);
            return SCRIPT_CONTINUE;
        }

        if (phase == AP_CRUISING && ticks % AUTOPILOT_STATUS_INTERVAL == 0)
        {
            boolean followCruiseHold = hasObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD) && getBooleanObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD);
            if (!followCruiseHold)
            {
                float bearing = getDirectionBearing(dx, dz);
                String cardinal = getBearingCardinal(bearing);
                float estSpeed = getShipEngineSpeedMaximum(self) * 2.5f;
                float descentTime = 0.0f;
                if (hasObjVar(self, OV_SUMMON_OWNER))
                {
                    float st = hasObjVar(self, OV_SUMMON_CUR_TAKEOFF) ? getFloatObjVar(self, OV_SUMMON_CUR_TAKEOFF) : SUMMON_TAKEOFF_ALT;
                    float sl = hasObjVar(self, OV_SUMMON_CUR_LANDING) ? getFloatObjVar(self, OV_SUMMON_CUR_LANDING) : SUMMON_LANDING_ALT;
                    descentTime = (st - sl) / ELEVATOR_SPEED;
                }
                else
                    descentTime = (AUTOPILOT_TAKEOFF_ALT - AUTOPILOT_LANDING_ALT) / ELEVATOR_SPEED;
                float cruiseTime = (estSpeed > 0.0f) ? (horizDist / estSpeed) : 0.0f;
                String eta = formatETA(cruiseTime + descentTime, 1.0f);
                String statusMsg = "\\#778899[Navicomputer]: " + formatCoord(horizDist) + "m remaining | " + cardinal + " | Alt " + formatCoord(shipLoc.y) + "m | ETA: " + eta;

                playSoundOnShipOccupants(self, SND_COMM);
                broadcastToShip(self, statusMsg);

                if (hasObjVar(self, OV_SUMMON_OWNER))
                {
                    obj_id summonOwner = getObjIdObjVar(self, OV_SUMMON_OWNER);
                    if (isIdValid(summonOwner) && exists(summonOwner))
                    {
                        play2dNonLoopingSound(summonOwner, SND_COMM);
                        sendSystemMessageTestingOnly(summonOwner, statusMsg);
                    }
                }
            }
        }

        messageTo(self, "shipAutoPilotTick", null, AUTOPILOT_MONITOR_RATE, false);
        return SCRIPT_CONTINUE;
    }

    public int shipAutoPilotCancel(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            return SCRIPT_CONTINUE;
        shipAutoPilotCancelInternal(self, null);
        return SCRIPT_CONTINUE;
    }

    private void shipAutoPilotCancelInternal(obj_id ship, String reason) throws InterruptedException
    {
        shipClearAutopilot(ship);

        // Clear reservation only (still on approach — not landed / docked)
        if (hasObjVar(ship, "atmo.landing.target")
            && !hasObjVar(ship, "atmo.landing.landed_at")
            && !hasObjVar(ship, "atmo.landing.docked"))
        {
            atmo_landing_registry.detachShipFromLandingPoint(ship);
        }

        removeObjVar(ship, OV_AUTOPILOT_ROOT);
        clearSummonFollowMode(ship);
        removeObjVar(ship, OV_SUMMON_QUIET_ARRIVAL);
        removeObjVar(ship, OV_SUMMON_CUR_TAKEOFF);
        removeObjVar(ship, OV_SUMMON_CUR_LANDING);

        broadcastToShip(ship, " ");
        broadcastToShip(ship, "\\#ffaa44========================================");
        if (reason != null && reason.length() > 0)
            broadcastToShip(ship, "\\#ffaa44[Navicomputer]: " + reason);
        else
            broadcastToShip(ship, "\\#ffaa44[Navicomputer]: Auto-pilot disengaged by the captain.");
        broadcastToShip(ship, "\\#ffaa44========================================");
        broadcastToShip(ship, " ");
        broadcastToShip(ship, "\\#ddbb88  Attention passengers, the captain has taken manual control.");
        broadcastToShip(ship, "\\#ddbb88   Please remain seated until further notice.");
        broadcastToShip(ship, " ");
    }

    // =====================================================================
    // Ship Summon (auto-pilot to player's location)
    // =====================================================================

    public static final String OV_SUMMON_OWNER = "space.autopilot.summonOwner";
    public static final float  SUMMON_TAKEOFF_ALT  = 500.0f;
    public static final float  SUMMON_LANDING_ALT  = 25.0f;

    /** Owner-paid orbit follow: ship re-summons to high AGL near owner on a timer. */
    public static final String OV_SUMMON_FOLLOW_ACTIVE = "space.summon.follow_active";
    public static final String OV_SUMMON_FOLLOW_NEXT_BILL = "space.summon.follow_next_bill";
    public static final String OV_SUMMON_FOLLOW_OWNER = "space.summon.follow_owner";
    public static final String OV_SUMMON_FOLLOW_CRUISE_HOLD = "space.summon.follow_cruise_hold";
    public static final String OV_SUMMON_QUIET_ARRIVAL = "space.summon.quiet_arrival";
    private static final String OV_SUMMON_CUR_TAKEOFF = "space.summon.cur_takeoff";
    private static final String OV_SUMMON_CUR_LANDING = "space.summon.cur_landing";

    public static final int SUMMON_FOLLOW_COST_PER_HOUR = 20000;
    /** Paid orbit mode: one-time activation + per-shot turret billing when firing from the ship datapad (see {@code space_turret}). */
    public static final String OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE = "space.summon.bombardment_orbit_active";
    public static final int SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST = 50000;
    public static final int SUMMON_BOMBARDMENT_CREDIT_PER_SHOT = 1000;
    /** Cruise height above terrain (m) for bombardment orbit — low hold (standard auto-follow uses {@link #SUMMON_FOLLOW_TAKEOFF_ALT}). */
    public static final float SUMMON_BOMBARDMENT_ORBIT_AGL = 100.0f;
    /** Max horizontal distance (m) from ship to picked ground point for datapad bombardment. */
    public static final float SUMMON_BOMBARDMENT_INSTANT_HORIZONTAL_RANGE = 4000.0f;
    public static final float SUMMON_FOLLOW_ORBIT_RADIUS = 190.0f;
    public static final float SUMMON_FOLLOW_TAKEOFF_ALT = 880.0f;
    public static final float SUMMON_FOLLOW_LANDING_ALT = 420.0f;
    /** Billing / target refresh while auto-follow is active (engine updates heading each alter). */
    private static final int SUMMON_FOLLOW_TICK_S = 5;

    private void clearSummonFollowMode(obj_id ship) throws InterruptedException
    {
        removeObjVar(ship, OV_SUMMON_FOLLOW_ACTIVE);
        removeObjVar(ship, OV_SUMMON_FOLLOW_NEXT_BILL);
        removeObjVar(ship, OV_SUMMON_FOLLOW_OWNER);
        removeObjVar(ship, OV_SUMMON_FOLLOW_CRUISE_HOLD);
        removeObjVar(ship, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE);
    }

    public int summonFollowDisable(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id owner = params.containsKey("owner") ? params.getObjId("owner") : null;
        if (isIdValid(owner) && getOwner(self) != owner)
            return SCRIPT_CONTINUE;

        obj_id notify = isIdValid(owner) ? owner : getOwner(self);

        clearSummonFollowMode(self);
        if (hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            shipAutoPilotCancelInternal(self, "Auto-follow orbit disabled.");
        else if (isIdValid(notify))
            sendSystemMessageTestingOnly(notify, "\\#aaddff[Navicomputer]: Auto-follow orbit disabled.");
        return SCRIPT_CONTINUE;
    }

    public int summonFollowEnable(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        obj_id owner = params.getObjId("owner");
        if (!isIdValid(owner) || getOwner(self) != owner)
            return SCRIPT_CONTINUE;

        if (hasObjVar(self, "atmo.landing.docked"))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Undock before enabling auto-follow.");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(self, OV_SUMMON_FOLLOW_ACTIVE) && getBooleanObjVar(self, OV_SUMMON_FOLLOW_ACTIVE))
        {
            if (hasObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) && getBooleanObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
                sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Bombardment orbit is active. Disable it first to use standard auto-follow.");
            else
                sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Auto-follow is already active.");
            return SCRIPT_CONTINUE;
        }

        if (isIdValid(getPilotId(self)))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Auto-follow cannot start while a pilot has the helm.");
            return SCRIPT_CONTINUE;
        }

        if (getTotalMoney(owner) < SUMMON_FOLLOW_COST_PER_HOUR)
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: You need at least " + SUMMON_FOLLOW_COST_PER_HOUR + " credits for the first hour of auto-follow.");
            return SCRIPT_CONTINUE;
        }

        if (!transferBankCreditsToNamedAccount(owner, money.ACCT_TRAVEL, SUMMON_FOLLOW_COST_PER_HOUR, "noHandler", "noHandler", new dictionary()))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Payment failed. Auto-follow not started.");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, OV_SUMMON_FOLLOW_ACTIVE, true);
        setObjVar(self, OV_SUMMON_FOLLOW_OWNER, owner);
        setObjVar(self, OV_SUMMON_FOLLOW_NEXT_BILL, getGameTime() + 3600);

        location playerLoc = getWorldLocation(owner);
        dictionary wp = new dictionary();
        wp.put("x", playerLoc.x);
        wp.put("z", playerLoc.z);
        wp.put("owner", owner);
        wp.put("summon", true);
        wp.put("cruiseHold", true);
        wp.put("takeoffAlt", SUMMON_FOLLOW_TAKEOFF_ALT);
        wp.put("landingAlt", SUMMON_FOLLOW_TAKEOFF_ALT);
        shipSummonEngage(self, wp);

        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
        {
            clearSummonFollowMode(self);
            if (transferBankCreditsFromNamedAccount(money.ACCT_TRAVEL, owner, SUMMON_FOLLOW_COST_PER_HOUR, "noHandler", "noHandler", new dictionary()))
                sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Navicomputer could not engage. Your payment was refunded.");
            else
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Auto-follow cancelled — refund failed. Contact support if credits are missing.");
            return SCRIPT_CONTINUE;
        }

        sendSystemMessageTestingOnly(owner, "\\#00ff88[Navicomputer]: Auto-follow orbit enabled. " + SUMMON_FOLLOW_COST_PER_HOUR + " credits charged for the first hour.");
        sendSystemMessageTestingOnly(owner, "\\#aaddff  Your ship will hold high orbit near you (" + SUMMON_FOLLOW_COST_PER_HOUR + " cr each additional hour).");
        messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
        return SCRIPT_CONTINUE;
    }

    /** Low-altitude orbit ({@link #SUMMON_BOMBARDMENT_ORBIT_AGL} m AGL) vs standard auto-follow; 50k activation (no hourly fee); turret shots bill per hit from the datapad ground pick. */
    public int summonBombardmentOrbitEnable(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        obj_id owner = params.getObjId("owner");
        if (!isIdValid(owner) || getOwner(self) != owner)
            return SCRIPT_CONTINUE;

        if (hasObjVar(self, "atmo.landing.docked"))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Undock before enabling bombardment orbit.");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(self, OV_SUMMON_FOLLOW_ACTIVE) && getBooleanObjVar(self, OV_SUMMON_FOLLOW_ACTIVE))
        {
            if (hasObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) && getBooleanObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
                sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Bombardment orbit is already active.");
            else
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Disable standard auto-follow first, then enable bombardment orbit.");
            return SCRIPT_CONTINUE;
        }

        if (isIdValid(getPilotId(self)))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Bombardment orbit cannot start while a pilot has the helm.");
            return SCRIPT_CONTINUE;
        }

        if (getTotalMoney(owner) < SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST)
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: You need at least " + SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST + " credits to activate bombardment orbit.");
            return SCRIPT_CONTINUE;
        }

        if (!transferBankCreditsToNamedAccount(owner, money.ACCT_TRAVEL, SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST, "noHandler", "noHandler", new dictionary()))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Payment failed. Bombardment orbit not started.");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, OV_SUMMON_FOLLOW_ACTIVE, true);
        setObjVar(self, OV_SUMMON_FOLLOW_OWNER, owner);
        setObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE, true);
        setObjVar(self, OV_SUMMON_FOLLOW_NEXT_BILL, getGameTime() + 86400 * 365);

        location playerLoc = getWorldLocation(owner);
        dictionary wp = new dictionary();
        wp.put("x", playerLoc.x);
        wp.put("z", playerLoc.z);
        wp.put("owner", owner);
        wp.put("summon", true);
        wp.put("cruiseHold", true);
        wp.put("takeoffAlt", SUMMON_BOMBARDMENT_ORBIT_AGL);
        wp.put("landingAlt", SUMMON_BOMBARDMENT_ORBIT_AGL);
        shipSummonEngage(self, wp);

        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
        {
            clearSummonFollowMode(self);
            if (transferBankCreditsFromNamedAccount(money.ACCT_TRAVEL, owner, SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST, "noHandler", "noHandler", new dictionary()))
                sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Navicomputer could not engage. Your payment was refunded.");
            else
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Bombardment orbit cancelled — refund failed. Contact support if credits are missing.");
            return SCRIPT_CONTINUE;
        }

        sendSystemMessageTestingOnly(owner, "\\#00ff88[Navicomputer]: Bombardment orbit enabled. " + SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST + " credits charged.");
        sendSystemMessageTestingOnly(owner, "\\#aaddff  Use \"Bombard ground target\" on your ship datapad: " + SUMMON_BOMBARDMENT_CREDIT_PER_SHOT + " credits per successful turret shot (when in range).");
        messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
        return SCRIPT_CONTINUE;
    }

    public int summonFollowTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_SUMMON_FOLLOW_ACTIVE) || !getBooleanObjVar(self, OV_SUMMON_FOLLOW_ACTIVE))
            return SCRIPT_CONTINUE;

        if (!isAtmosphericFlightScene())
        {
            clearSummonFollowMode(self);
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(self, "atmo.landing.docked"))
        {
            clearSummonFollowMode(self);
            return SCRIPT_CONTINUE;
        }

        obj_id owner = hasObjVar(self, OV_SUMMON_FOLLOW_OWNER) ? getObjIdObjVar(self, OV_SUMMON_FOLLOW_OWNER) : getOwner(self);
        if (!isIdValid(owner) || !exists(owner) || getOwner(self) != owner)
        {
            clearSummonFollowMode(self);
            return SCRIPT_CONTINUE;
        }

        if (isIdValid(getPilotId(self)))
        {
            clearSummonFollowMode(self);
            sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Auto-follow stopped — a pilot has the helm.");
            return SCRIPT_CONTINUE;
        }

        if (space_transition.getContainingShip(owner) == self)
        {
            messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
            return SCRIPT_CONTINUE;
        }

        int now = getGameTime();
        int nextBill = getIntObjVar(self, OV_SUMMON_FOLLOW_NEXT_BILL);
        boolean bombardmentOrbit = hasObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) && getBooleanObjVar(self, OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE);
        if (!bombardmentOrbit && now >= nextBill)
        {
            if (getTotalMoney(owner) < SUMMON_FOLLOW_COST_PER_HOUR)
            {
                clearSummonFollowMode(self);
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Auto-follow ended — insufficient credits for the next hour.");
                if (hasObjVar(self, OV_AUTOPILOT_ACTIVE))
                    shipAutoPilotCancelInternal(self, "Auto-follow suspended: payment failed.");
                return SCRIPT_CONTINUE;
            }
            if (!transferBankCreditsToNamedAccount(owner, money.ACCT_TRAVEL, SUMMON_FOLLOW_COST_PER_HOUR, "noHandler", "noHandler", new dictionary()))
            {
                clearSummonFollowMode(self);
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Auto-follow ended — payment failed.");
                if (hasObjVar(self, OV_AUTOPILOT_ACTIVE))
                    shipAutoPilotCancelInternal(self, "Auto-follow suspended: payment failed.");
                return SCRIPT_CONTINUE;
            }
            setObjVar(self, OV_SUMMON_FOLLOW_NEXT_BILL, now + 3600);
            sendSystemMessageTestingOnly(owner, "\\#aaddff[Navicomputer]: Auto-follow: " + SUMMON_FOLLOW_COST_PER_HOUR + " credits charged for another hour.");
        }

        boolean cruiseHold = hasObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD) && getBooleanObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD);

        if (cruiseHold && shipIsAutopilotActive(self))
        {
            location pl = getWorldLocation(owner);
            float angle = (now % 628) * 0.01f;
            float tx = pl.x + (float) StrictMath.cos(angle) * SUMMON_FOLLOW_ORBIT_RADIUS;
            float tz = pl.z + (float) StrictMath.sin(angle) * SUMMON_FOLLOW_ORBIT_RADIUS;
            shipUpdateAutopilotTargetXZ(self, tx, tz);
            setObjVar(self, OV_AUTOPILOT_TARGET_X, tx);
            setObjVar(self, OV_AUTOPILOT_TARGET_Z, tz);
            messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
            return SCRIPT_CONTINUE;
        }

        if (shipIsAutopilotActive(self))
        {
            messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
            return SCRIPT_CONTINUE;
        }

        location pl = getWorldLocation(owner);
        float angle = (now % 628) * 0.01f;
        float tx = pl.x + (float) StrictMath.cos(angle) * SUMMON_FOLLOW_ORBIT_RADIUS;
        float tz = pl.z + (float) StrictMath.sin(angle) * SUMMON_FOLLOW_ORBIT_RADIUS;

        float orbitAgl = bombardmentOrbit ? SUMMON_BOMBARDMENT_ORBIT_AGL : SUMMON_FOLLOW_TAKEOFF_ALT;
        dictionary wp = new dictionary();
        wp.put("x", tx);
        wp.put("z", tz);
        wp.put("owner", owner);
        wp.put("summon", true);
        wp.put("cruiseHold", true);
        wp.put("takeoffAlt", orbitAgl);
        wp.put("landingAlt", orbitAgl);
        wp.put("quietSummon", true);
        shipSummonEngage(self, wp);

        messageTo(self, "summonFollowTick", null, SUMMON_FOLLOW_TICK_S, false);
        return SCRIPT_CONTINUE;
    }

    /** End paid follow and fly a normal summon (descend to landing altitude near owner). */
    public int summonFollowReturnToLanding(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        obj_id owner = params.getObjId("owner");
        if (!isIdValid(owner) || getOwner(self) != owner)
            return SCRIPT_CONTINUE;

        if (hasObjVar(self, "atmo.landing.docked"))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Undock before returning to landing altitude.");
            return SCRIPT_CONTINUE;
        }

        if (!hasObjVar(self, OV_SUMMON_FOLLOW_ACTIVE) || !getBooleanObjVar(self, OV_SUMMON_FOLLOW_ACTIVE))
        {
            sendSystemMessageTestingOnly(owner, "\\#ffaa44[Navicomputer]: Auto-follow is not active.");
            return SCRIPT_CONTINUE;
        }

        if (isIdValid(getPilotId(self)))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: A pilot has the helm.");
            return SCRIPT_CONTINUE;
        }

        clearSummonFollowMode(self);

        location playerLoc = getWorldLocation(owner);
        dictionary wp = new dictionary();
        wp.put("x", playerLoc.x);
        wp.put("z", playerLoc.z);
        wp.put("owner", owner);
        wp.put("summon", true);
        shipSummonEngage(self, wp);

        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
        {
            sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Auto-follow was stopped, but the ship could not engage for landing altitude.");
            return SCRIPT_CONTINUE;
        }

        sendSystemMessageTestingOnly(owner, "\\#aaddff[Navicomputer]: Auto-follow ended. Bringing the ship to landing altitude at your position.");
        return SCRIPT_CONTINUE;
    }

    public int shipSummonEngage(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        // Block summon if ship is docked - must undock from terminal first
        if (hasObjVar(self, "atmo.landing.docked"))
        {
            obj_id owner = params.getObjId("owner");
            if (isIdValid(owner))
            {
                sendSystemMessageTestingOnly(owner, "\\#ff4444[Navicomputer]: Cannot summon ship while it is docked.");
                sendSystemMessageTestingOnly(owner, "\\#ffaa44  Someone aboard must use the Starship Management Terminal to undock first.");
            }
            return SCRIPT_CONTINUE;
        }

        atmo_landing_registry.detachShipFromLandingPoint(self);

        if (!space_utils.isShipWithInterior(self))
        {
            obj_id owner = params.getObjId("owner");
            if (isIdValid(owner))
                sendSystemMessageTestingOnly(owner, "Only ships with an interior can be summoned.");
            return SCRIPT_CONTINUE;
        }

        float targetX = params.getFloat("x");
        float targetZ = params.getFloat("z");
        obj_id owner = params.getObjId("owner");

        if (!isIdValid(owner) || getOwner(self) != owner)
        {
            if (isIdValid(owner))
                sendSystemMessageTestingOnly(owner, "Only the ship owner may summon this vessel.");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(self, OV_AUTOPILOT_ACTIVE))
        {
            shipClearAutopilot(self);
            removeObjVar(self, OV_AUTOPILOT_ROOT);
        }

        float takeoffA = params.containsKey("takeoffAlt") ? params.getFloat("takeoffAlt") : SUMMON_TAKEOFF_ALT;
        float landingA = params.containsKey("landingAlt") ? params.getFloat("landingAlt") : SUMMON_LANDING_ALT;
        boolean quietSummon = params.containsKey("quietSummon") && params.getBoolean("quietSummon");

        if (quietSummon)
            setObjVar(self, OV_SUMMON_QUIET_ARRIVAL, true);
        else
            removeObjVar(self, OV_SUMMON_QUIET_ARRIVAL);

        setObjVar(self, OV_SUMMON_CUR_TAKEOFF, takeoffA);
        setObjVar(self, OV_SUMMON_CUR_LANDING, landingA);

        boolean cruiseHold = params.containsKey("cruiseHold") && params.getBoolean("cruiseHold");
        if (cruiseHold)
            setObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD, true);
        else
            removeObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD);

        boolean apOk = cruiseHold
            ? shipSetAutopilotTargetHold(self, targetX, targetZ, takeoffA, landingA, true)
            : shipSetAutopilotTarget(self, targetX, targetZ, takeoffA, landingA);
        if (!apOk)
        {
            removeObjVar(self, OV_SUMMON_QUIET_ARRIVAL);
            removeObjVar(self, OV_SUMMON_CUR_TAKEOFF);
            removeObjVar(self, OV_SUMMON_CUR_LANDING);
            removeObjVar(self, OV_SUMMON_FOLLOW_CRUISE_HOLD);
            sendSystemMessageTestingOnly(owner, "Failed to summon ship. Auto-pilot could not engage.");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, OV_AUTOPILOT_ACTIVE, true);
        setObjVar(self, OV_AUTOPILOT_TARGET_X, targetX);
        setObjVar(self, OV_AUTOPILOT_TARGET_Z, targetZ);
        setObjVar(self, OV_AUTOPILOT_OWNER, owner);
        setObjVar(self, OV_AUTOPILOT_TICKS, 0);
        setObjVar(self, OV_AUTOPILOT_LAST_PHASE, AP_NONE);
        setObjVar(self, OV_SUMMON_OWNER, owner);

        location shipLoc = getLocation(self);
        float dx = targetX - shipLoc.x;
        float dz = targetZ - shipLoc.z;
        float dist = (float) StrictMath.sqrt(dx * dx + dz * dz);
        float bearing = getDirectionBearing(dx, dz);
        String cardinal = getBearingCardinal(bearing);

        float estSpeed = getShipEngineSpeedMaximum(self) * 2.5f;
        String eta = formatFullETA(dist, estSpeed, takeoffA, landingA);

        if (!quietSummon)
        {
            play2dNonLoopingSound(owner, SND_ALARM);
            playSoundOnShipOccupants(self, SND_ALARM);

            sendSystemMessageTestingOnly(owner, "\\#00ccff[Navicomputer]: Ship summoned. En route to your location.");
            sendSystemMessageTestingOnly(owner, "\\#aaddff  Distance: " + formatCoord(dist) + "m | Bearing: " + cardinal + " | ETA: " + eta);

            broadcastToShip(self, " ");
            broadcastToShip(self, "\\#00ccff========================================");
            broadcastToShip(self, "\\#00ccff[Navicomputer]: Ship summoned by owner.");
            broadcastToShip(self, "\\#00ccff  Destination: [" + formatCoord(targetX) + ", " + formatCoord(targetZ) + "]");
            broadcastToShip(self, "\\#00ccff========================================");
            broadcastToShip(self, " ");
            broadcastToShip(self, "\\#88bbdd  Attention: the ship has been summoned remotely.");
            broadcastToShip(self, "\\#88bbdd   Ascending to cruise altitude " + formatCoord(takeoffA) + "m...");
            broadcastToShip(self, " ");
        }
        else
        {
            sendSystemMessageTestingOnly(owner, "\\#778899[Navicomputer]: Repositioning to follow orbit… ETA ~" + eta + ".");
        }

        messageTo(self, "shipAutoPilotTick", null, AUTOPILOT_MONITOR_RATE, false);
        return SCRIPT_CONTINUE;
    }

    public int delayedPackShipFinalize(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, "space.packPending"))
            return SCRIPT_CONTINUE;
        if (getTopMostContainer(self) != self)
            return SCRIPT_CONTINUE;
        space_transition.prepareShipForPackDpvsSafe(self);
        return SCRIPT_CONTINUE;
    }
    public int delayedPackShipFinalizePhase2(obj_id self, dictionary params) throws InterruptedException
    {
        if (getTopMostContainer(self) != self)
            return SCRIPT_CONTINUE;
        space_transition.packShipFinalize(self);
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // NPC POB Ship Autonomous Waypoint Cycling
    // =====================================================================
    //
    // FLOW CHAIN:
    //
    // [Spawn Egg (Terminal in World)]
    //   |
    //   +-> OnAttach (npc_pob_ship_spawner.java)
    //   |     |
    //   |     +-> spawnShip() -> creates ship object
    //   |     |
    //   |     +-> setObjVar(ship, "npc_pob.controller", 1)
    //   |
    //   +-> [Ship Attaches to combat_ship.java]
    //   |     |
    //   |     +-> OnAttach() (combat_ship.java, line ~42)
    //   |     |     |
    //   |     |     +-> detects npc_pob.controller objvar
    //   |     |     |
    //   |     |     +-> messageTo(self, "npcWaypointTick", ..., 2s)
    //   |     |
    //   |     +-> npcWaypointTick() runs every 2 seconds
    //   |           |
    //   |           +-> First run: initializes waypoint system
    //   |           |     |
    //   |           |     +-> reads datatables/npc_shuttle/naboo.iff
    //   |           |     |
    //   |           |     +-> finds 6 waypoints (Theed, Lake Retreat, etc.)
    //   |           |     |
    //   |           |     +-> setObjVar(ship, npc_pob.currentWaypointIndex, 0)
    //   |           |     |
    //   |           |     +-> npcFlyToNextWaypoint()
    //   |           |           |
    //   |           |           +-> reads waypoint 0: x=-4858, z=4164, landingDuration=60
    //   |           |           |
    //   |           |           +-> messageTo(ship, "shipAutoPilotEngage", x, z)
    //   |           |           |
    //   |           |           +-> setObjVar(ship, npc_pob.currentWaypointIndex, 1)
    //   |           |           |
    //   |           |           +-> setObjVar(ship, npc_pob.landingEndTime, now + 60)
    //   |           |
    //   |           +-> Second+ runs: checks landing timer
    //   |                 |
    //   |                 +-> if (now >= landingEndTime)
    //   |                 |     |
    //   |                 |     +-> npcFlyToNextWaypoint() to waypoint 1
    //   |                 |
    //   |                 +-> else: wait for landing duration to expire
    //   |
    //   +-> [Ship Autopilot]
    //   |     |
    //   |     +-> shipAutoPilotEngage() handles actual flight
    //   |     |
    //   |     +-> phases: 1=Ascending, 2=Cruising, 3=Descending, 4=Arrived
    //   |     |
    //   |     +-> after phase 4: autopilotActive returns false
    //   |
    //   +-> [Landing Phase]
    //         |
    //         +-> npcWaypointTick detects landing timer active
    //         |
    //         +-> waits for landingDuration seconds
    //         |
    //         +-> calls npcFlyToNextWaypoint() again
    //         |
    //         +-> LOOP repeats: Theed -> Lake Retreat -> Keren -> Moenia -> Kaadara -> Deeja Peak -> repeat
    //
    // KEY POINTS:
    // - Spawner ONLY spawns ship and sets npc_pob.controller objvar
    // - Ship script handles ALL waypoint logic autonomously
    // - Waypoint data comes from datatables/npc_shuttle/<planet>.iff
    // - Landing duration is per-waypoint from datatable (default 60s)
    // - Ship cycles endlessly through all waypoints without spawner involvement
    // =====================================================================

    public int npcWaypointTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, NPC_CONTROLLER_OBJVAR))
            return SCRIPT_CONTINUE;

        // Continue the tick loop
        messageTo(self, "npcWaypointTick", null, NPC_WAYPOINT_CHECK_INTERVAL, false);

        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        // Check if ship has a pilot (manual control)
        obj_id pilot = getPilotId(self);
        if (isIdValid(pilot))
            return SCRIPT_CONTINUE;

        // Check if currently in landing phase
        int landingEndTime = hasObjVar(self, NPC_LANDING_END_TIME) ? getIntObjVar(self, NPC_LANDING_END_TIME) : 0;
        int now = getGameTime();

        int currentIdx = hasObjVar(self, NPC_CURRENT_WAYPOINT_INDEX) ? getIntObjVar(self, NPC_CURRENT_WAYPOINT_INDEX) : -1;
        script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle DEBUG: tick - landingEndTime=" + landingEndTime + " now=" + now + " currentIdx=" + currentIdx + " autopilot=" + shipIsAutopilotActive(self));

        if (landingEndTime > 0)
        {
            // Currently landing, check if landing duration expired
            if (now >= landingEndTime)
            {
                script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: landing expired at waypoint " + currentIdx);
                removeObjVar(self, NPC_LANDING_END_TIME);
                // Advance to next waypoint and fly
                String dtPath = npcGetDatatablePathForCurrentScene(self);
                int numWaypoints = npcGetNumWaypointsForPath(dtPath);
                script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: dtPath=" + dtPath + " numWaypoints=" + numWaypoints);

                if (numWaypoints > 0)
                {
                    int nextIdx = (currentIdx + 1) % numWaypoints;
                    setObjVar(self, NPC_CURRENT_WAYPOINT_INDEX, nextIdx);
                    script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: advancing from waypoint " + currentIdx + " to " + nextIdx + ", calling npcFlyToNextWaypoint");
                    npcFlyToNextWaypoint(self);
                }
                else
                {
                    script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle ERROR: numWaypoints is 0 or less!");
                }
            }
            else
            {
                script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: landing in progress - " + (landingEndTime - now) + "s remaining");
            }
            return SCRIPT_CONTINUE;
        }

        // Check if ship is currently flying (autopilot active)
        boolean autopilotActive = shipIsAutopilotActive(self);
        int phase = shipGetAutopilotPhase(self);

        script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle DEBUG: phase=" + phase + " autopilotActive=" + autopilotActive);

        if (autopilotActive || phase != 0)
        {
            // Currently flying or in autopilot system, do nothing
            script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: autopilot active at waypoint " + currentIdx + ", flying (phase=" + phase + ")");
            return SCRIPT_CONTINUE;
        }

        // Ship has completed autopilot and is not in any phase - set up landing
        // First check if we need to initialize
        if (currentIdx < 0 || !hasObjVar(self, NPC_CURRENT_WAYPOINT_INDEX))
        {
            // Initialize waypoint queue
            String dtPath = npcGetDatatablePathForCurrentScene(self);
            int numWaypoints = npcGetNumWaypointsForPath(dtPath);
            script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: INITIALIZING - dtPath=" + dtPath + " numWaypoints=" + numWaypoints);

            if (numWaypoints > 0)
            {
                setObjVar(self, NPC_CURRENT_WAYPOINT_INDEX, 0);
                script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: initialized waypoint system with " + numWaypoints + " waypoints from " + dtPath);
                npcFlyToNextWaypoint(self);
            }
            else
            {
                script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: ERROR - no waypoints found, check datatable " + dtPath);
            }
        }
        else
        {
            // Ship has landed, start the landing timer if not already started
            if (landingEndTime == 0)
            {
                String dtPath = npcGetDatatablePathForCurrentScene(self);
                try
                {
                    int landingDuration = dataTableGetInt(dtPath, currentIdx, "landingDuration");
                    if (landingDuration <= 0)
                        landingDuration = 60;

                    int newLandingEndTime = getGameTime() + landingDuration;
                    setObjVar(self, NPC_LANDING_END_TIME, newLandingEndTime);
                    script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: ship landed at waypoint " + currentIdx + ", starting " + landingDuration + "s landing timer");
                }
                catch (Throwable t)
                {
                    script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: ERROR setting landing timer - " + t.getMessage());
                }
            }
        }

        return SCRIPT_CONTINUE;
    }

    private void npcFlyToNextWaypoint(obj_id self) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return;

        String dtPath = npcGetDatatablePathForCurrentScene(self);
        int numWaypoints = npcGetNumWaypointsForPath(dtPath);

        if (numWaypoints <= 0)
            return;

        int currentIdx = hasObjVar(self, NPC_CURRENT_WAYPOINT_INDEX) ? getIntObjVar(self, NPC_CURRENT_WAYPOINT_INDEX) : 0;

        try
        {
            float x = dataTableGetFloat(dtPath, currentIdx, "x");
            float z = dataTableGetFloat(dtPath, currentIdx, "z");
            int landingDuration = dataTableGetInt(dtPath, currentIdx, "landingDuration");
            if (landingDuration <= 0)
                landingDuration = 60;

            // Get terrain height at waypoint and calculate altitude relative to terrain
            float terrainHeight = getHeightAtLocation(x, z);
            float takeoffAlt = terrainHeight + AUTOPILOT_TAKEOFF_ALT;
            float landingAlt = terrainHeight + AUTOPILOT_LANDING_ALT;

            script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: flying to waypoint " + currentIdx + " (" + x + ", " + z + "), terrain=" + terrainHeight + "m, takeoff=" + takeoffAlt + "m, landing=" + landingAlt + "m, landing duration " + landingDuration + "s");

            // Fly to this waypoint with terrain-relative altitudes
            dictionary flyParams = new dictionary();
            flyParams.put("x", x);
            flyParams.put("z", z);
            flyParams.put("takeoffAlt", takeoffAlt);
            flyParams.put("landingAlt", landingAlt);
            flyParams.put("npcControlled", true);
            obj_id owner = getOwner(self);
            flyParams.put("owner", isIdValid(owner) ? owner : self);
            messageTo(self, "shipAutoPilotEngage", flyParams, 0, false);

            // Schedule landing end time (don't change index yet - that happens when landing expires)
            int landingEndTime = getGameTime() + landingDuration;
            setObjVar(self, NPC_LANDING_END_TIME, landingEndTime);
        }
        catch (Throwable t)
        {
            script_logs.logToGodsInRange(self, NPC_SHUTTLE_LOG_RANGE, "NPC Shuttle: ERROR reading waypoint data - " + t.getMessage());
        }
    }

    private String npcGetDatatablePathForCurrentScene(obj_id self) throws InterruptedException
    {
        String scene = getLocation(self).area;
        String planet = npcGetPlanetFromScene(scene);
        return NPC_DATATABLE_PREFIX + planet + NPC_DATATABLE_SUFFIX;
    }

    private static String npcGetPlanetFromScene(String scene)
    {
        if (scene == null || scene.length() == 0)
            return "";
        int idx = scene.indexOf('_');
        return idx > 0 ? scene.substring(0, idx) : scene;
    }

    private int npcGetNumWaypointsForPath(String dtPath)
    {
        try
        {
            return dataTableGetNumRows(dtPath);
        }
        catch (Throwable t)
        {
            return 0;
        }
    }
}
