package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * Every droid of the Battle of Naboo (B1s, STAP scouts, OOM-9): markings, the march on the Gungan line or the
 * generators, picking fights with Gungans, death reports to the controller, and the epilogue power-down.
 * Attached from creatures.tab; the sequencer row (or the MTT) sets hrc_nboo.attack = line | generators afterwards.
 */
public class droid_unit extends script.base_script
{
    public droid_unit()
    {
    }

    public static final float TICK = 4.0f;
    public static final float ENGAGE_RANGE = 45.0f;
    public static final float GENERATOR_ENGAGE_RANGE = 70.0f;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooDroidSetup", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDroidSetup(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self))
            return SCRIPT_CONTINUE;
        if (!hasObjVar(self, "hrc_nboo.folded"))
            naboo_battle_lib.snapToGround(self, 0.0f);
        naboo_battle_lib.paint(self);
        // STAP scouts: the creature is the B1 in its riding pose; the speeder hangs on the object origin ("-" finds
        // no saddle on a B1 skeleton, so it stays put instead of following a joint)
        if ("hrc_nboo_stap".equals(hasObjVar(self, "creature_type") ? getStringObjVar(self, "creature_type") : ""))
            naboo_battle_lib.setAppSlot(self, "stap", "-", naboo_battle_lib.STAP_HULL, 0.0f, 0.0f, 0.0f);
        if (!hasObjVar(self, "hrc_nboo.folded"))
            messageTo(self, "hrc_nbooMarch", null, rand(0.5f, 2.5f), false);
        messageTo(self, "hrc_nbooDroidTick", null, TICK + rand(0.0f, 2.0f), false);
        return SCRIPT_CONTINUE;
    }

    /** Where this droid is headed: a point on the Gungan line, or the nearest shield generator. */
    public location destination(obj_id self, obj_id controller) throws InterruptedException
    {
        String attack = hasObjVar(self, "hrc_nboo.attack") ? getStringObjVar(self, "hrc_nboo.attack") : "line";
        location here = getLocation(self);
        if (attack.equals("generators"))
        {
            location best = null;
            float bestD = Float.MAX_VALUE;
            for (float[] g : naboo_battle_lib.GENERATORS)
            {
                location l = naboo_battle_lib.offset(controller, g[0] + 18.0f, g[1] + rand(-10.0f, 10.0f));
                float d = getDistance(here, l);
                if (d < bestD)
                {
                    bestD = d;
                    best = l;
                }
            }
            return best;
        }
        // just outside the dome, in front of the stretch of the Gungan line facing this droid
        location c = getLocation(controller);
        float z = Math.max(-60.0f, Math.min(60.0f, here.z - c.z + rand(-12.0f, 12.0f)));
        return naboo_battle_lib.linePoint(controller, z, rand(16.0f, 26.0f));
    }

    public int hrc_nbooMarch(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self) || hasObjVar(self, "hrc_nboo.frozen") || hasObjVar(self, "hrc_nboo.folded"))
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location dest = destination(self, controller);
        if (dest == null)
            return SCRIPT_CONTINUE;
        setObjVar(self, "hrc_nboo.dest", dest);
        aiSetHomeLocation(self, dest);
        if (!ai_lib.isInCombat(self))
        {
            setMovementRun(self);       // MTT pads to the Gungan line is ~150 m
            pathTo(self, dest);
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDroidTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self) || hasObjVar(self, "hrc_nboo.frozen"))
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location here = getLocation(self);
        if (!hasObjVar(self, "hrc_nboo.folded"))
        {
            if (!utils.hasScriptVar(self, "hrc_nboo.reported_shield") && naboo_battle_lib.getPhase(controller) == 2
                    && naboo_battle_lib.isInsideDome(controller, here))
            {
                utils.setScriptVar(self, "hrc_nboo.reported_shield", 1);
                messageTo(controller, "hrc_nbooDroidInsideShield", null, 0.0f, false);
            }
            if (!ai_lib.isInCombat(self))
                engage(self, controller, here);
        }
        messageTo(self, "hrc_nbooDroidTick", null, TICK, false);
        return SCRIPT_CONTINUE;
    }

    /** Pick a fight with the Gungan side (players are found by the normal aggro volumes). */
    public void engage(obj_id self, obj_id controller, location here) throws InterruptedException
    {
        String attack = hasObjVar(self, "hrc_nboo.attack") ? getStringObjVar(self, "hrc_nboo.attack") : "line";
        obj_id target = null;
        if (attack.equals("generators"))
        {
            obj_id[] near = naboo_battle_lib.alliesInRange(here, GENERATOR_ENGAGE_RANGE);
            for (obj_id o : near)
                if (hasScript(o, "theme_park.heroic.naboo_battle.shield_generator"))
                {
                    target = o;
                    break;
                }
        }
        if (!isIdValid(target))
        {
            obj_id[] near = naboo_battle_lib.alliesInRange(here, ENGAGE_RANGE);
            java.util.Vector gungans = new java.util.Vector();
            for (obj_id o : near)
                if (naboo_battle_lib.isGungan(o))
                    gungans.add(o);
            obj_id[] g = new obj_id[gungans.size()];
            gungans.toArray(g);
            target = naboo_battle_lib.nearest(g, here);
        }
        if (isIdValid(target) && pvpCanAttack(self, target))
        {
            startCombat(self, target);
            return;
        }
        // nothing to shoot: keep walking to the objective (AI may have parked us after a fight)
        location dest = hasObjVar(self, "hrc_nboo.dest") ? getLocationObjVar(self, "hrc_nboo.dest") : null;
        if (dest != null && getDistance(here, dest) > 12.0f && !hasObjVar(self, "hrc_nboo.folded"))
            pathTo(self, dest);
    }

    // ------------------------------------------------------------------ death

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        report(self);
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        report(self);
        return SCRIPT_CONTINUE;
    }

    public void report(obj_id self) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "hrc_nboo.reported_death") || hasObjVar(self, "hrc_nboo.frozen"))
            return;
        utils.setScriptVar(self, "hrc_nboo.reported_death", 1);
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return;
        dictionary d = new dictionary();
        d.put("type", hasObjVar(self, "creature_type") ? getStringObjVar(self, "creature_type") : "");
        messageTo(controller, "hrc_nbooDroidDestroyed", d, 0.0f, false);
        if ("hrc_nboo_b1_commander".equals(d.getString("type")))
            messageTo(controller, "hrc_nbooCommanderDestroyed", d, 0.0f, false);
    }

    // ------------------------------------------------------------------ epilogue: the control ship is gone

    public int hrc_nbooPowerDown(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self) || hasObjVar(self, "hrc_nboo.frozen"))
            return SCRIPT_CONTINUE;
        setObjVar(self, "hrc_nboo.frozen", 1);
        removeObjVar(self, "hrc_nboo.folded");
        utils.setScriptVar(self, "petIgnoreAttacks", 1);
        stopCombat(self);
        clearHateList(self);
        stop(self);
        removeTriggerVolume(ai_lib.ALERT_VOLUME_NAME);
        removeTriggerVolume(ai_lib.AGGRO_VOLUME_NAME);
        setWantSawAttackTriggers(self, false);
        setInvulnerable(self, true);
        doAnimationAction(self, "power_down");
        messageTo(self, "hrc_nbooStayDown", null, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    /** Keep the shut-down droid still (stock AI resumes calm behaviour on its own timers). */
    public int hrc_nbooStayDown(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, "hrc_nboo.frozen") || !exists(self))
            return SCRIPT_CONTINUE;
        stop(self);
        if (ai_lib.isInCombat(self))
        {
            stopCombat(self);
            clearHateList(self);
        }
        messageTo(self, "hrc_nbooStayDown", null, 5.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int resumeDefaultCalmBehavior(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, "hrc_nboo.frozen"))
        {
            stop(self);
            return SCRIPT_OVERRIDE;
        }
        return SCRIPT_CONTINUE;
    }
}
