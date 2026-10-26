package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * A fambaa carrying a Gungan shield generator (hp_dyn app on root, contract 3.4).
 * Phase 1: radial "Calibrate Generator" (8 s, interrupted by damage or walking away) switches it on; the controller
 * raises the dome when all three run. Phase 3: the droids go for it; it has HP (hp_value from creatures.tab).
 * Generators 2 and 3 hold at 25 %; generator 1 is the one that fails - at 0 % the dome falls (scripted collapse,
 * and after two minutes of phase 3 it also starts to drain on its own so the story always moves on).
 * Outside phase 3 it shrugs off damage. It never fights back and never walks (forceNoMovement).
 */
public class shield_generator extends script.base_script
{
    public shield_generator()
    {
    }

    public static final String GENERATOR_APPEARANCE = "appearance/titan_hrc_nboo_shield_generator.apt";
    public static final String ARC_PARTICLE = "appearance/pt_blue_electric_field.prt";
    public static final int CALIBRATE_SECONDS = 8;
    public static final float CALIBRATE_RANGE = 18.0f;
    public static final float FLOOR_PERCENT = 25.0f;
    public static final String SV_CAL = "hrc_nboo.calibrator";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooGeneratorSetup", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooGeneratorSetup(obj_id self, dictionary params) throws InterruptedException
    {
        naboo_battle_lib.snapToGround(self, 0.0f);
        setObjVar(self, "forceNoMovement", 1);
        utils.setScriptVar(self, "petIgnoreAttacks", 1);
        stop(self);
        naboo_battle_lib.setAppSlot(self, "generator", "root", GENERATOR_APPEARANCE, 0.0f, 0.0f, 0.0f);
        setObjVar(self, "hrc_nboo.on", 0);
        return SCRIPT_CONTINUE;
    }

    public int gen(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, "hrc_nboo.gen") ? getIntObjVar(self, "hrc_nboo.gen") : 1;
    }

    public boolean isOn(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, "hrc_nboo.on") && getIntObjVar(self, "hrc_nboo.on") != 0;
    }

    public void setRunning(obj_id self, boolean on) throws InterruptedException
    {
        setObjVar(self, "hrc_nboo.on", on ? 1 : 0);
        if (on)
            naboo_battle_lib.setFxSlot(self, "arc", "root", ARC_PARTICLE, 0.0f, 9.0f, 0.0f, 2.0f);
        else
            naboo_battle_lib.clearSlot(self, "arc");
    }

    // ------------------------------------------------------------------ phase 1: calibration

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (isPlayer(player) && naboo_battle_lib.getPhase(controller) == 1 && !isOn(self))
            mi.addRootMenu(menu_info_types.SERVER_MENU1, naboo_battle_lib.sid("menu_calibrate"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.SERVER_MENU1)
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (naboo_battle_lib.getPhase(controller) != 1)
            return SCRIPT_CONTINUE;
        if (isOn(self))
        {
            naboo_battle_lib.message(player, "sys_generator_already");
            return SCRIPT_CONTINUE;
        }
        if (getIntObjVar(controller, naboo_battle_lib.OV_RALLIED) == 0)
        {
            naboo_battle_lib.message(player, "sys_generator_not_yet");
            return SCRIPT_CONTINUE;
        }
        if (getDistance(self, player) > CALIBRATE_RANGE)
        {
            naboo_battle_lib.message(player, "sys_generator_too_far");
            return SCRIPT_CONTINUE;
        }
        obj_id busy = utils.hasScriptVar(self, SV_CAL) ? utils.getObjIdScriptVar(self, SV_CAL) : null;
        if (isIdValid(busy) && busy != player && exists(busy))
            return SCRIPT_CONTINUE;
        utils.setScriptVar(self, SV_CAL, player);
        int session = utils.getIntScriptVar(self, "hrc_nboo.cal_session") + 1;
        utils.setScriptVar(self, "hrc_nboo.cal_session", session);
        naboo_battle_lib.message(player, "sys_generator_calibrating");
        faceTo(player, self);
        doAnimationAction(player, "manipulate_medium");
        dictionary d = new dictionary();
        d.put("player", player);
        d.put("session", session);
        d.put("health", getAttrib(player, HEALTH));
        d.put("tick", 0);
        messageTo(self, "hrc_nbooCalibrateTick", d, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooCalibrateTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (params.getInt("session") != utils.getIntScriptVar(self, "hrc_nboo.cal_session"))
            return SCRIPT_CONTINUE;
        obj_id player = params.getObjId("player");
        obj_id controller = naboo_battle_lib.getController(self);
        boolean ok = isIdValid(player) && exists(player) && naboo_battle_lib.isAlive(player) && !isOn(self)
                && naboo_battle_lib.getPhase(controller) == 1 && getDistance(self, player) <= CALIBRATE_RANGE + 2.0f
                && getAttrib(player, HEALTH) >= params.getInt("health");
        if (!ok)
        {
            utils.removeScriptVar(self, SV_CAL);
            if (isIdValid(player) && exists(player) && !isOn(self))
                naboo_battle_lib.message(player, "sys_generator_interrupted");
            return SCRIPT_CONTINUE;
        }
        int tick = params.getInt("tick") + 1;
        if (tick < CALIBRATE_SECONDS)
        {
            params.put("tick", tick);
            params.put("health", getAttrib(player, HEALTH));      // regeneration does not count as a hit
            if (tick % 3 == 0)
                doAnimationAction(player, "manipulate_medium");
            messageTo(self, "hrc_nbooCalibrateTick", params, 1.0f, false);
            return SCRIPT_CONTINUE;
        }
        utils.removeScriptVar(self, SV_CAL);
        setRunning(self, true);
        naboo_battle_lib.message(player, "sys_generator_online");
        dictionary d = new dictionary();
        d.put("gen", gen(self));
        messageTo(controller, "hrc_nbooGeneratorOn", d, 0.0f, false);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ phase 3: HP

    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id weapon, int[] damage) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        int max = getMaxAttrib(self, HEALTH);
        int hp = getAttrib(self, HEALTH);
        if (max <= 0)
            return SCRIPT_CONTINUE;
        if (naboo_battle_lib.getPhase(controller) != 3 || hasObjVar(self, "hrc_nboo.down"))
        {
            setAttrib(self, HEALTH, max);             // only the assault of phase 3 can hurt the generators
            return SCRIPT_CONTINUE;
        }
        float pct = 100.0f * hp / max;
        if (gen(self) != 1 && pct < FLOOR_PERCENT)
        {
            setAttrib(self, HEALTH, (int)(max * FLOOR_PERCENT / 100.0f));
            pct = FLOOR_PERCENT;
        }
        reportHealth(self, controller, pct);
        if (gen(self) == 1 && pct <= 2.0f)
            goDown(self, controller);
        return SCRIPT_CONTINUE;
    }

    public void reportHealth(obj_id self, obj_id controller, float pct) throws InterruptedException
    {
        int band = (int)(pct / 25.0f);                    // 75 / 50 / 25 % announcements
        int lastBand = hasObjVar(self, "hrc_nboo.band") ? getIntObjVar(self, "hrc_nboo.band") : 4;
        if (band < lastBand)
        {
            setObjVar(self, "hrc_nboo.band", band);
            prose_package pp = prose.getPackage(naboo_battle_lib.sid("sys_generator_health"));
            prose.setTO(pp, self);
            prose.setDI(pp, Math.max(1, (int)pct));
            for (obj_id p : naboo_battle_lib.getPlayers(controller))
                sendSystemMessageProse(p, pp);
        }
        int now = getGameTime();
        if (now - utils.getIntScriptVar(self, "hrc_nboo.last_report") >= 10)
        {
            utils.setScriptVar(self, "hrc_nboo.last_report", now);
            dictionary d = new dictionary();
            d.put("gen", gen(self));
            d.put("pct", (int)pct);
            messageTo(controller, "hrc_nbooGeneratorHit", d, 0.0f, false);
        }
    }

    public void goDown(obj_id self, obj_id controller) throws InterruptedException
    {
        if (hasObjVar(self, "hrc_nboo.down"))
            return;
        setObjVar(self, "hrc_nboo.down", 1);
        setAttrib(self, HEALTH, Math.max(1, getMaxAttrib(self, HEALTH) / 100));
        setInvulnerable(self, true);
        setRunning(self, false);
        prose_package pp = prose.getPackage(naboo_battle_lib.sid("sys_generator_down"));
        prose.setTO(pp, self);
        for (obj_id p : naboo_battle_lib.getPlayers(controller))
            sendSystemMessageProse(p, pp);
        naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_SHELL_IMPACT, getLocation(self));
        dictionary d = new dictionary();
        d.put("gen", gen(self));
        messageTo(controller, "hrc_nbooGeneratorDown", d, 0.0f, false);
    }

    /** Droid sappers at work: generator 1 loses 1 % every 2 s until it fails. */
    public int hrc_nbooStartDrain(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller) || naboo_battle_lib.getPhase(controller) != 3 || hasObjVar(self, "hrc_nboo.down"))
            return SCRIPT_CONTINUE;
        int max = getMaxAttrib(self, HEALTH);
        int hp = getAttrib(self, HEALTH) - Math.max(1, max / 100);
        if (hp <= max / 50)
        {
            goDown(self, controller);
            return SCRIPT_CONTINUE;
        }
        setAttrib(self, HEALTH, hp);
        reportHealth(self, controller, 100.0f * hp / max);
        messageTo(self, "hrc_nbooStartDrain", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    /** The dome has fallen: the other generators stop and stand by (they survive the battle). */
    public int hrc_nbooShieldLost(obj_id self, dictionary params) throws InterruptedException
    {
        setRunning(self, false);
        setInvulnerable(self, true);
        return SCRIPT_CONTINUE;
    }

    /** Phase restart after a wipe: full health; phase 1 also switches it off again. */
    public int hrc_nbooResetGenerator(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        removeObjVar(self, "hrc_nboo.down");
        removeObjVar(self, "hrc_nboo.band");
        setInvulnerable(self, false);
        setAttrib(self, HEALTH, getMaxAttrib(self, HEALTH));
        utils.removeScriptVar(self, SV_CAL);
        if (naboo_battle_lib.getPhase(controller) == 1)
            setRunning(self, false);
        return SCRIPT_CONTINUE;
    }
}
