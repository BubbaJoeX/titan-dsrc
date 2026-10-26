package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * Gungan booma catapult (contract 3.4): radial "Launch Booma" flings an energy booma at the player's look-at target
 * (a droid in range) or the nearest droid in front; it bursts with an 8 m electric splash. One shot every 4 s.
 */
public class catapult extends script.base_script
{
    public catapult()
    {
    }

    public static final float USE_RANGE = 8.0f;
    public static final float TARGET_RANGE = 220.0f;
    public static final int COOLDOWN = 4;
    public static final float BOOMA_SPEED = 45.0f;
    public static final float SPLASH = 8.0f;
    public static final int DAMAGE = 2600;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooCatapultSetup", null, 0.5f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooCatapultSetup(obj_id self, dictionary params) throws InterruptedException
    {
        naboo_battle_lib.snapToGround(self, 0.0f);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (isPlayer(player) && naboo_battle_lib.getPhase(controller) >= 2)
            mi.addRootMenu(menu_info_types.SERVER_MENU1, naboo_battle_lib.sid("menu_launch_booma"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.SERVER_MENU1)
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller) || naboo_battle_lib.getPhase(controller) < 2)
            return SCRIPT_CONTINUE;
        if (getDistance(self, player) > USE_RANGE)
        {
            naboo_battle_lib.message(player, "sys_catapult_too_far");
            return SCRIPT_CONTINUE;
        }
        int now = getGameTime();
        if (now < utils.getIntScriptVar(self, "hrc_nboo.next_shot"))
        {
            naboo_battle_lib.message(player, "sys_catapult_cooldown");
            return SCRIPT_CONTINUE;
        }
        obj_id target = pickTarget(self, player);
        if (!isIdValid(target))
        {
            naboo_battle_lib.message(player, "sys_catapult_no_target");
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(self, "hrc_nboo.next_shot", now + COOLDOWN);
        location aim = getLocation(target);
        float flight = naboo_battle_lib.fireProjectile(controller, naboo_battle_lib.BOOMA, self, "hp_launch", aim, BOOMA_SPEED);
        doAnimationAction(player, "manipulate_medium");
        naboo_battle_lib.message(player, "sys_catapult_fired");
        dictionary d = new dictionary();
        d.put("loc", aim);
        d.put("player", player);
        messageTo(self, "hrc_nbooBoomaImpact", d, flight, false);
        return SCRIPT_CONTINUE;
    }

    /** The player's look-at target when it is a droid in range, else the nearest droid ahead (west is behind us). */
    public obj_id pickTarget(obj_id self, obj_id player) throws InterruptedException
    {
        location here = getLocation(self);
        obj_id look = getLookAtTarget(player);
        if (naboo_battle_lib.isDroid(look) && naboo_battle_lib.isAlive(look) && getDistance(here, getLocation(look)) <= TARGET_RANGE)
            return look;
        obj_id best = null;
        float bestD = Float.MAX_VALUE;
        for (obj_id d : naboo_battle_lib.droidsInRange(here, TARGET_RANGE))
        {
            location l = getLocation(d);
            if (l.x < here.x)
                continue;
            float dist = getDistance(here, l);
            if (dist < bestD)
            {
                bestD = dist;
                best = d;
            }
        }
        return best;
    }

    public int hrc_nbooBoomaImpact(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location aim = params.getLocation("loc");
        naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_BOOMA_IMPACT, aim);
        naboo_battle_lib.splashDamage(controller, aim, SPLASH, DAMAGE, false, params.getObjId("player"));
        return SCRIPT_CONTINUE;
    }
}
