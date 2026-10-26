package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * Trade Federation MTT (contract 3.1): glides in to its pad (TangibleDynamics push in slowing legs as the ease-out;
 * on the server a push follows the terrain), settles, opens the front cowl (hp_lid), lowers the step plate (hp_chin), runs the droid rack out
 * (hp_rack, 6 s), drops two files of folded B1s in front of hp_deploy, swaps the rack to the empty one, and covers the
 * landing with its trunk cannons. Attackable (60000 HP); destroyed it explodes and is removed.
 *
 * Objvars from the sequencer row: hrc_nboo.pad_x / pad_z (offset from the controller). The hull faces west (yaw 270).
 */
public class mtt extends script.base_script
{
    public mtt()
    {
    }

    public static final float HOVER = 0.0f;                    // pushed tangibles follow the terrain (+0.05 m) on the server
    // glide in: constant-speed legs, slower toward the pad (a push with drag would be decayed twice on the server)
    public static final float[][] LEGS = { { 32.0f, 120.0f }, { 16.0f, 40.0f }, { 6.0f, 0.0f } };   // speed, metres left when it ends
    public static final int DROIDS_PER_FILE = 8;
    public static final float DEPLOY_AHEAD = 22.0f;            // hp_deploy, on the ground ahead of the hull
    public static final float CANNON_RANGE = 90.0f;
    public static final String[] MUZZLES = { "muzzle_l1", "muzzle_l2", "muzzle_r1", "muzzle_r2" };

    // slot, hardpoint, appearance, mode, ax, ay, az, amount, seconds (verified in the client by the lead)
    public static final String[] LID = { "lid", "hp_lid", "appearance/titan_hrc_nboo_mtt_lid.apt", "rot", "1", "0", "0", "-100", "2.5" };
    public static final String[] CHIN = { "chin", "hp_chin", "appearance/titan_hrc_nboo_mtt_chin.apt", "rot", "1", "0", "0", "70", "1.5" };
    public static final String[] RACK = { "rack", "hp_rack", "appearance/titan_hrc_nboo_mtt_rack.apt", "move", "0", "0", "1", "10", "6" };
    public static final String[] RACK_EMPTY = { "rack", "hp_rack", "appearance/titan_hrc_nboo_mtt_rack_empty.apt", "move", "0", "0", "1", "10", "0.1" };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooMttSetup", null, 0.5f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooMttSetup(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        naboo_battle_lib.snapToGround(self, HOVER);
        setYaw(self, 270.0f);
        vessel.setPart(self, LID, 0.0f, 0.0f);
        vessel.setPart(self, CHIN, 0.0f, 0.0f);
        vessel.setPart(self, RACK, 0.0f, 0.0f);
        location pad = naboo_battle_lib.offset(controller, getFloatObjVar(self, "hrc_nboo.pad_x"), getFloatObjVar(self, "hrc_nboo.pad_z"));
        pad.y += HOVER;
        setObjVar(self, "hrc_nboo.pad", pad);
        messageTo(self, "hrc_nbooMttLeg", null, 0.1f, false);
        return SCRIPT_CONTINUE;
    }

    /** One glide leg: push toward the pad at LEGS[i] speed until LEGS[i][1] metres are left. */
    public int hrc_nbooMttLeg(obj_id self, dictionary params) throws InterruptedException
    {
        int leg = params != null ? params.getInt("leg") : 0;
        location pad = getLocationObjVar(self, "hrc_nboo.pad");
        if (pad == null || isDisabled(self))
            return SCRIPT_CONTINUE;
        location here = getLocation(self);
        float dx = pad.x - here.x, dz = pad.z - here.z;
        float dist = (float)Math.sqrt(dx * dx + dz * dz);
        while (leg < LEGS.length && dist <= LEGS[leg][1] + 0.5f)
            ++leg;
        if (leg >= LEGS.length || dist < 0.5f)
        {
            messageTo(self, "hrc_nbooMttSettle", null, 0.1f, false);
            return SCRIPT_CONTINUE;
        }
        float speed = LEGS[leg][0];
        float seconds = (dist - LEGS[leg][1]) / speed;
        tangible_dynamics.applyPushForce(self, dx / dist * speed, 0.0f, dz / dist * speed, seconds, tangible_dynamics.SPACE_WORLD);
        dictionary d = new dictionary();
        d.put("leg", leg + 1);
        messageTo(self, "hrc_nbooMttLeg", d, Math.max(0.1f, seconds), false);
        return SCRIPT_CONTINUE;
    }

    /** Snap the last metre or two, then open up: lid at 0 s, chin at 1 s, rack at 2 s (6 s travel). */
    public int hrc_nbooMttSettle(obj_id self, dictionary params) throws InterruptedException
    {
        if (isDisabled(self))
            return SCRIPT_CONTINUE;
        tangible_dynamics.clearAllForces(self);
        location pad = getLocationObjVar(self, "hrc_nboo.pad");
        if (pad != null)
            setLocation(self, pad);
        setYaw(self, 270.0f);
        obj_id controller = naboo_battle_lib.getController(self);
        if (isIdValid(controller))
            messageTo(controller, "hrc_nbooMttLanded", null, 0.0f, false);
        vessel.setPart(self, LID, 1.0f, 0.0f);
        vessel.setPart(self, CHIN, 1.0f, 1.0f);
        vessel.setPart(self, RACK, 1.0f, 2.0f);
        messageTo(self, "hrc_nbooMttDeploy", null, 8.2f, false);
        messageTo(self, "hrc_nbooMttGuns", null, 4.0f, false);
        return SCRIPT_CONTINUE;
    }

    /** Two files of folded droids from hp_deploy forward; then the rack is shown empty. */
    public int hrc_nbooMttDeploy(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller) || isDisabled(self))
            return SCRIPT_CONTINUE;
        location here = getLocation(self);
        double yaw = Math.toRadians(getYaw(self));
        float fx = (float)Math.sin(yaw), fz = (float)Math.cos(yaw);
        float rx = fz, rz = -fx;
        int n = 0;
        for (int file = -1; file <= 1; file += 2)
        {
            for (int i = 0; i < DROIDS_PER_FILE; ++i)
            {
                float along = DEPLOY_AHEAD + 1.5f + 2.2f * i;
                float side = 2.5f * file;
                location l = naboo_battle_lib.groundAt(new location(here.x + fx * along + rx * side, here.y, here.z + fz * along + rz * side, here.area));
                String mob = (i % 4 == 3) ? "hrc_nboo_b1_pilot" : "hrc_nboo_b1";
                obj_id droid = create.object(mob, l);
                if (!isIdValid(droid))
                    continue;
                setYaw(droid, getYaw(self));
                naboo_battle_lib.adopt(controller, droid, "p2_droid");
                setObjVar(droid, "hrc_nboo.attack", "line");
                attachScript(droid, naboo_battle_lib.SCRIPT_AI_CONTROLLER);
                attachScript(droid, naboo_battle_lib.SCRIPT_FOLDED);
                ++n;
            }
        }
        vessel.setPart(self, RACK_EMPTY, 1.0f, 0.0f);
        messageTo(controller, "hrc_nbooDroidsDeployed", null, 0.0f, false);
        CustomerServiceLog("instance-" + naboo_battle_lib.INSTANCE_NAME, "MTT " + self + " deployed " + n + " droids");
        return SCRIPT_CONTINUE;
    }

    /** The twin cannons in the trunk pods cover the landing zone. */
    public int hrc_nbooMttGuns(obj_id self, dictionary params) throws InterruptedException
    {
        if (isDisabled(self) || !exists(self))
            return SCRIPT_CONTINUE;
        obj_id controller = naboo_battle_lib.getController(self);
        if (!isIdValid(controller))
            return SCRIPT_CONTINUE;
        location here = getLocation(self);
        obj_id target = naboo_battle_lib.nearest(naboo_battle_lib.alliesInRange(here, CANNON_RANGE), here);
        if (isIdValid(target))
        {
            int shot = utils.getIntScriptVar(self, "hrc_nboo.shot");
            utils.setScriptVar(self, "hrc_nboo.shot", shot + 1);
            location aim = getLocation(target);
            aim.y += 1.0f;
            naboo_battle_lib.fireProjectile(controller, naboo_battle_lib.MTT_BLASTER, self, MUZZLES[shot % MUZZLES.length], aim, 150.0f);
            naboo_battle_lib.fireProjectile(controller, naboo_battle_lib.MTT_BLASTER, self, MUZZLES[(shot + 1) % MUZZLES.length], aim, 150.0f);
            if (!naboo_battle_lib.isInsideDome(controller, aim))
                damage(target, DAMAGE_ENERGY, HIT_LOCATION_BODY, rand(350, 650));
        }
        messageTo(self, "hrc_nbooMttGuns", null, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectDisabled(obj_id self, obj_id killer) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "hrc_nboo.destroyed"))
            return SCRIPT_CONTINUE;
        utils.setScriptVar(self, "hrc_nboo.destroyed", 1);
        obj_id controller = naboo_battle_lib.getController(self);
        location here = getLocation(self);
        if (isIdValid(controller))
        {
            naboo_battle_lib.playEffect(controller, naboo_battle_lib.FX_SHELL_IMPACT, here);
            naboo_battle_lib.playEffect(controller, "clienteffect/restuss_event_hanging_smoke.cef", here);
        }
        tangible_dynamics.clearAllForces(self);
        messageTo(self, "hrc_nbooMttRemove", null, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooMttRemove(obj_id self, dictionary params) throws InterruptedException
    {
        trial.cleanupObject(self);
        return SCRIPT_CONTINUE;
    }
}
