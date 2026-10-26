package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * NPC AAT (contract 3.2): drives to its station, turns the turret (hp_dyn part, state = yaw / 360) toward whatever it
 * shoots, and fires main-gun plasma shells with a 6 m splash on top of its AI cannon. In phase 2 (objvar
 * hrc_nboo.bombard) it shells the raised Gungan dome: the shells burst on the shield and hurt nobody inside.
 * Objvars from the sequencer row: hrc_nboo.station_x / station_z, hrc_nboo.bombard, hrc_nboo.counted (phase 4
 * tanks), hrc_nboo.command (OOM-9's tank).
 */
public class aat_npc extends script.base_script
{
    public aat_npc()
    {
    }

    public static final float TICK = 2.0f;
    public static final int MAIN_GUN_COOLDOWN = 8;
    public static final int SHELL_DAMAGE = 1800;
    public static final int COMMAND_SHELL_DAMAGE = 2600;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooAatSetup", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooAatSetup(obj_id self, dictionary params) throws InterruptedException
    {
        naboo_battle_lib.snapToGround(self, 0.0f);
        naboo_battle_lib.initTurret(self);
        obj_id controller = naboo_battle_lib.getController(self);
        if (isIdValid(controller) && hasObjVar(self, "hrc_nboo.station_x"))
        {
            location station = naboo_battle_lib.offset(controller, getFloatObjVar(self, "hrc_nboo.station_x"), getFloatObjVar(self, "hrc_nboo.station_z"));
            setObjVar(self, "hrc_nboo.station", station);
            aiSetHomeLocation(self, station);
            setMovementRun(self);
            pathTo(self, station);
        }
        messageTo(self, "hrc_nbooAatTick", null, TICK, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooAatTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self) || hasObjVar(self, "hrc_nboo.frozen"))
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        int now = getGameTime();
        location here = getLocation(self);
        obj_id target = ai_lib.isInCombat(self) ? getHateTarget(self) : null;
        if (isIdValid(target) && exists(target))
        {
            location aim = getLocation(target);
            float traverse = naboo_battle_lib.aimTurret(self, aim);
            if (now >= utils.getIntScriptVar(self, "hrc_nboo.next_main"))
            {
                utils.setScriptVar(self, "hrc_nboo.next_main", now + MAIN_GUN_COOLDOWN + rand(0, 3));
                dictionary d = new dictionary();
                d.put("loc", aim);
                messageTo(self, "hrc_nbooAatMainGun", d, Math.min(traverse, 1.5f) + 0.2f, false);
            }
        }
        else if (hasObjVar(self, "hrc_nboo.bombard") && naboo_battle_lib.isDomeUp(controller))
        {
            location station = hasObjVar(self, "hrc_nboo.station") ? getLocationObjVar(self, "hrc_nboo.station") : here;
            if (getDistance(here, station) < 30.0f && now >= utils.getIntScriptVar(self, "hrc_nboo.next_main"))
            {
                utils.setScriptVar(self, "hrc_nboo.next_main", now + rand(6, 10));
                location hit = naboo_battle_lib.domePointFacing(controller, here);
                float traverse = naboo_battle_lib.aimTurret(self, hit);
                dictionary d = new dictionary();
                d.put("loc", hit);
                d.put("dome", 1);
                messageTo(self, "hrc_nbooAatMainGun", d, Math.min(traverse, 1.5f) + 0.2f, false);
            }
        }
        else if (!ai_lib.isInCombat(self) && hasObjVar(self, "hrc_nboo.station"))
        {
            location station = getLocationObjVar(self, "hrc_nboo.station");
            if (getDistance(here, station) > 30.0f)
                pathTo(self, station);
        }
        messageTo(self, "hrc_nbooAatTick", null, TICK, false);
        return SCRIPT_CONTINUE;
    }

    /** Main gun: a plasma shell from "muzzle". Shots into the raised dome burst on its surface. */
    public int hrc_nbooAatMainGun(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self))
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location aim = params.getLocation("loc");
        boolean onDome = params.getInt("dome") != 0;
        if (!onDome && naboo_battle_lib.isInsideDome(controller, aim))
        {
            aim = naboo_battle_lib.domeEntryPoint(controller, getLocation(self), aim);
            onDome = true;
        }
        float flight = naboo_battle_lib.fireProjectile(controller, naboo_battle_lib.PLASMA_SHELL, self, "muzzle", aim, naboo_battle_lib.SHELL_SPEED);
        dictionary d = new dictionary();
        d.put("loc", aim);
        d.put("dome", onDome ? 1 : 0);
        d.put("damage", hasObjVar(self, "hrc_nboo.command") ? COMMAND_SHELL_DAMAGE : SHELL_DAMAGE);
        messageTo(self, "hrc_nbooAatImpact", d, flight, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooAatImpact(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location aim = params.getLocation("loc");
        if (params.getInt("dome") != 0)
        {
            naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_DOME_HIT, aim);
            obj_id dome = naboo_battle_lib.firstSpawned(controller, "dome");
            if (isIdValid(dome))
            {
                dictionary d = new dictionary();
                d.put("loc", aim);
                messageTo(dome, "hrc_nbooDomeHit", d, 0.0f, false);
            }
            return SCRIPT_CONTINUE;
        }
        naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_SHELL_IMPACT, aim);
        naboo_battle_lib.splashDamage(controller, aim, naboo_battle_lib.SHELL_SPLASH, params.getInt("damage"), true, null);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ destruction

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        destroyed(self);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        destroyed(self);
        return SCRIPT_CONTINUE;
    }

    public void destroyed(obj_id self) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "hrc_nboo.destroyed") || hasObjVar(self, "hrc_nboo.frozen"))
            return;
        utils.setScriptVar(self, "hrc_nboo.destroyed", 1);
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return;
        location here = getLocation(self);
        naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_SHELL_IMPACT, here);
        dictionary d = new dictionary();
        d.put("counted", hasObjVar(self, "hrc_nboo.counted") ? 1 : 0);
        d.put("command", hasObjVar(self, "hrc_nboo.command") ? 1 : 0);
        d.put("loc", here);
        messageTo(controller, "hrc_nbooAatDestroyed", d, 0.0f, false);
    }

    /** The control ship is gone: the tank powers down with the rest of the army. */
    public int hrc_nbooPowerDown(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self))
            return SCRIPT_CONTINUE;
        setObjVar(self, "hrc_nboo.frozen", 1);
        utils.setScriptVar(self, "petIgnoreAttacks", 1);
        stopCombat(self);
        clearHateList(self);
        stop(self);
        removeTriggerVolume(ai_lib.ALERT_VOLUME_NAME);
        removeTriggerVolume(ai_lib.AGGRO_VOLUME_NAME);
        setInvulnerable(self, true);
        return SCRIPT_CONTINUE;
    }
}
