package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * The recaptured AAT (contract 3.2, phase 4): a two-seat dynamic mount (mount.dm.*). The first player aboard drives
 * (seat 1): a creature mount that is not GOT_vehicle, so the client uses camera-relative strafing. The second is the
 * gunner (seat 2): /titanAatFire (naboo_battle_player.cmdTitanAatFire) turns the turret toward the look-at target and
 * fires a plasma shell with a 6 m splash. The tank itself is invulnerable; it sits idle until someone drives it.
 */
public class aat_captured extends script.base_script
{
    public aat_captured()
    {
    }

    public static final String SV_DRIVER = "hrc_nboo.driver";
    public static final String SV_GUNNER = "hrc_nboo.gunner";
    public static final float BOARD_RANGE = 12.0f;
    public static final float FIRE_RANGE = 160.0f;
    public static final int FIRE_COOLDOWN = 3;
    public static final int SHELL_DAMAGE = 4000;

    // seat offsets (metres, mount space; 0 = the client's own seat hardpoints). Tune here if riders float.
    public static final float[][] SEATS = { { 0.0f, 0.0f, 0.0f }, { 0.0f, 0.0f, 0.0f } };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooCapturedSetup", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooCapturedSetup(obj_id self, dictionary params) throws InterruptedException
    {
        naboo_battle_lib.snapToGround(self, 0.0f);
        naboo_battle_lib.initTurret(self);
        setInvulnerable(self, true);
        utils.setScriptVar(self, "petIgnoreAttacks", 1);
        removeTriggerVolume(ai_lib.ALERT_VOLUME_NAME);
        removeTriggerVolume(ai_lib.AGGRO_VOLUME_NAME);
        stop(self);
        setObjVar(self, mount_maker.VAR_DM_ACTIVE, 1);
        setObjVar(self, mount_maker.VAR_DM_CAPACITY, 2);
        for (int i = 0; i < SEATS.length; ++i)
        {
            String base = "mount.dm.seat." + i + ".";
            setObjVar(self, base + "ox", SEATS[i][0]);
            setObjVar(self, base + "oy", SEATS[i][1]);
            setObjVar(self, base + "oz", SEATS[i][2]);
        }
        if (!getMountsEnabled() || !makeDynamicMountable(self))
            CustomerServiceLog("instance-" + naboo_battle_lib.INSTANCE_NAME, "captured AAT " + self + ": makeDynamicMountable failed (mounts enabled: " + getMountsEnabled() + ")");
        messageTo(self, "hrc_nbooCapturedTick", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public obj_id driver(obj_id self) throws InterruptedException
    {
        obj_id d = utils.hasScriptVar(self, SV_DRIVER) ? utils.getObjIdScriptVar(self, SV_DRIVER) : null;
        return isIdValid(d) && exists(d) && getMountId(d) == self ? d : null;
    }

    public obj_id gunner(obj_id self) throws InterruptedException
    {
        obj_id g = utils.hasScriptVar(self, SV_GUNNER) ? utils.getObjIdScriptVar(self, SV_GUNNER) : null;
        return isIdValid(g) && exists(g) && getMountId(g) == self ? g : null;
    }

    // ------------------------------------------------------------------ radial

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isPlayer(player))
            return SCRIPT_CONTINUE;
        if (getMountId(player) == self)
            mi.addRootMenu(menu_info_types.SERVER_MENU3, naboo_battle_lib.sid("menu_climb_out"));
        else if (!isIdValid(driver(self)))
            mi.addRootMenu(menu_info_types.SERVER_MENU1, naboo_battle_lib.sid("menu_drive"));
        else if (!isIdValid(gunner(self)))
            mi.addRootMenu(menu_info_types.SERVER_MENU2, naboo_battle_lib.sid("menu_gunner"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU3)
        {
            climbOut(self, player);
            return SCRIPT_CONTINUE;
        }
        if (item != menu_info_types.SERVER_MENU1 && item != menu_info_types.SERVER_MENU2)
            return SCRIPT_CONTINUE;
        if (getDistance(self, player) > BOARD_RANGE)
        {
            naboo_battle_lib.message(player, "sys_aat_too_far");
            return SCRIPT_CONTINUE;
        }
        if (isIdValid(getMountId(player)) || ai_lib.aiIsDead(player) || isIncapacitated(player))
            return SCRIPT_CONTINUE;
        boolean wantsDrive = item == menu_info_types.SERVER_MENU1;
        obj_id d = driver(self);
        obj_id g = gunner(self);
        if (wantsDrive && isIdValid(d))
            wantsDrive = false;                         // someone got there first: take the gun instead
        if (!wantsDrive && !isIdValid(d))
        {
            naboo_battle_lib.message(player, "sys_aat_driver_first");
            return SCRIPT_CONTINUE;
        }
        if (!wantsDrive && isIdValid(g))
        {
            naboo_battle_lib.message(player, "sys_aat_full");
            return SCRIPT_CONTINUE;
        }
        faceTo(player, self);
        posture.stand(player);
        queueClear(player);
        if (!mountCreature(player, self))
        {
            sendSystemMessage(player, pet_lib.SID_SYS_CANT_MOUNT);
            return SCRIPT_CONTINUE;
        }
        stop(self);
        setState(player, STATE_RIDING_MOUNT, true);
        if (wantsDrive)
        {
            utils.setScriptVar(self, SV_DRIVER, player);
            pet_lib.setMountedMovementRate(player, self);
            naboo_battle_lib.message(player, "sys_aat_driver_hint");
        }
        else
        {
            utils.setScriptVar(self, SV_GUNNER, player);
            naboo_battle_lib.message(player, "sys_aat_gunner_hint");
            obj_id controller = naboo_battle_lib.getController(self);
            if (isIdValid(controller))
                messageTo(controller, "hrc_nbooGunnerSeated", null, 0.0f, false);
        }
        return SCRIPT_CONTINUE;
    }

    public void climbOut(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
            return;
        boolean wasDriver = player == driver(self);
        if (getMountId(player) == self)
        {
            dismountCreature(player);
            pet_lib.setUnmountedMovementRate(player, self);
        }
        if (wasDriver)
        {
            utils.removeScriptVar(self, SV_DRIVER);
            obj_id g = gunner(self);
            if (isIdValid(g))
            {
                naboo_battle_lib.message(g, "sys_aat_driver_left");
                climbOut(self, g);
            }
            stop(self);
        }
        else if (player == (utils.hasScriptVar(self, SV_GUNNER) ? utils.getObjIdScriptVar(self, SV_GUNNER) : null))
            utils.removeScriptVar(self, SV_GUNNER);
    }

    /** Keeps the seat bookkeeping honest (players can also leave with /dismount, log out or die). */
    public int hrc_nbooCapturedTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!exists(self))
            return SCRIPT_CONTINUE;
        if (utils.hasScriptVar(self, SV_DRIVER) && !isIdValid(driver(self)))
        {
            utils.removeScriptVar(self, SV_DRIVER);
            obj_id g = gunner(self);
            if (isIdValid(g))
            {
                naboo_battle_lib.message(g, "sys_aat_driver_left");
                climbOut(self, g);
            }
            stop(self);
        }
        if (utils.hasScriptVar(self, SV_GUNNER) && !isIdValid(gunner(self)))
            utils.removeScriptVar(self, SV_GUNNER);
        messageTo(self, "hrc_nbooCapturedTick", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ the gun

    /** From naboo_battle_player.cmdTitanAatFire: params player, target (may be null). */
    public int hrc_nbooGunnerFire(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        if (!isIdValid(player) || player != gunner(self))
        {
            naboo_battle_lib.message(player, "sys_aat_not_gunner");
            return SCRIPT_CONTINUE;
        }
        int now = getGameTime();
        if (now < utils.getIntScriptVar(self, "hrc_nboo.next_shot"))
        {
            naboo_battle_lib.message(player, "sys_aat_fire_cooldown");
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(self, "hrc_nboo.next_shot", now + FIRE_COOLDOWN);
        obj_id target = params.getObjId("target");
        location aim;
        if (isIdValid(target) && exists(target) && target != self && target != player && getDistance(self, target) <= FIRE_RANGE)
            aim = getLocation(target);
        else
            aim = naboo_battle_lib.turretForward(self, 90.0f);
        float traverse = naboo_battle_lib.aimTurret(self, aim);
        dictionary d = new dictionary();
        d.put("loc", aim);
        d.put("player", player);
        messageTo(self, "hrc_nbooCapturedShell", d, Math.min(traverse, 1.2f) + 0.1f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooCapturedShell(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location aim = params.getLocation("loc");
        float flight = naboo_battle_lib.fireProjectile(controller, naboo_battle_lib.PLASMA_SHELL, self, "muzzle", aim, naboo_battle_lib.SHELL_SPEED);
        messageTo(self, "hrc_nbooCapturedImpact", params, flight, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooCapturedImpact(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location aim = params.getLocation("loc");
        naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_SHELL_IMPACT, aim);
        naboo_battle_lib.splashDamage(controller, aim, naboo_battle_lib.SHELL_SPLASH, SHELL_DAMAGE, false, params.getObjId("player"));
        return SCRIPT_CONTINUE;
    }
}
