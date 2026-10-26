package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * The Gungan shield dome (r 150 m, origin = dome centre on the ground, no collision). It rises out of the ground when
 * the third generator runs, shows shell hits as ripples (hp_dyn fx/app slots at the hit point, plus a shield-hit
 * client effect from the shooter), and when generator 1 fails it shudders and sinks away. Vertical motion uses the
 * TangibleDynamics hover (push is terrain-following "hockey puck" physics on the server).
 */
public class shield_dome extends script.base_script
{
    public shield_dome()
    {
    }

    public static final float RISE_DEPTH = 40.0f;
    public static final float RISE_SECONDS = 4.0f;
    public static final int HIT_SLOTS = 4;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooDomeRise", null, 0.5f, false);
        return SCRIPT_CONTINUE;
    }

    /** Starts 40 m down and rises with a TangibleDynamics hover (server and client ease to terrain + 0). */
    public int hrc_nbooDomeRise(obj_id self, dictionary params) throws InterruptedException
    {
        location l = getLocation(self);
        float ground = getHeightAtLocation(l.x, l.z);
        setObjVar(self, "hrc_nboo.ground", ground);
        l.y = ground - RISE_DEPTH;
        setLocation(self, l);
        setYaw(self, 0.0f);
        tangible_dynamics.applyHoverEffect(self, 0.0f, 0.0f, 0.0f, RISE_SECONDS);
        messageTo(self, "hrc_nbooDomeRisen", null, RISE_SECONDS + 0.2f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDomeRisen(obj_id self, dictionary params) throws InterruptedException
    {
        tangible_dynamics.clearAllForces(self);
        location l = getLocation(self);
        l.y = getFloatObjVar(self, "hrc_nboo.ground");
        setLocation(self, l);
        return SCRIPT_CONTINUE;
    }

    /** A shell bursts on the membrane at `loc` (world). The ripple pieces sit at the hit point in dome space. */
    public int hrc_nbooDomeHit(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, "hrc_nboo.falling"))
            return SCRIPT_CONTINUE;
        location hit = params.getLocation("loc");
        location c = getLocation(self);
        if (hit == null)
            return SCRIPT_CONTINUE;
        int n = utils.getIntScriptVar(self, "hrc_nboo.hits");
        utils.setScriptVar(self, "hrc_nboo.hits", n + 1);
        String slot = "hit" + (n % HIT_SLOTS);
        float ox = hit.x - c.x, oy = hit.y - c.y, oz = hit.z - c.z;
        naboo_battle_lib.setFxSlot(self, slot, "-", naboo_battle_lib.SHIELD_HIT_PARTICLE, ox, oy, oz, 4.0f);
        naboo_battle_lib.setAppSlot(self, slot + "a", "-", naboo_battle_lib.SHIELD_HIT_APPEARANCE, ox, oy, oz);
        dictionary d = new dictionary();
        d.put("slot", slot);
        d.put("n", n + 1);
        messageTo(self, "hrc_nbooDomeHitClear", d, 1.5f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDomeHitClear(obj_id self, dictionary params) throws InterruptedException
    {
        String slot = params.getString("slot");
        // a newer hit may have reused the slot already
        if (utils.getIntScriptVar(self, "hrc_nboo.hits") - params.getInt("n") < HIT_SLOTS)
        {
            naboo_battle_lib.clearSlot(self, slot);
            naboo_battle_lib.clearSlot(self, slot + "a");
        }
        return SCRIPT_CONTINUE;
    }

    /** Generator 1 is gone: the shield flickers (shake) and falls (sinks into the ground), then is removed. */
    public int hrc_nbooDomeFall(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, "hrc_nboo.falling"))
            return SCRIPT_CONTINUE;
        setObjVar(self, "hrc_nboo.falling", 1);
        for (int i = 0; i < HIT_SLOTS; ++i)
        {
            naboo_battle_lib.clearSlot(self, "hit" + i);
            naboo_battle_lib.clearSlot(self, "hit" + i + "a");
        }
        tangible_dynamics.applyShakeEffect(self, 0.6f, 9.0f, 2.0f);
        messageTo(self, "hrc_nbooDomeSink", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDomeSink(obj_id self, dictionary params) throws InterruptedException
    {
        tangible_dynamics.clearAllForces(self);
        tangible_dynamics.applyHoverEffect(self, -RISE_DEPTH, 0.0f, 0.0f, 3.0f);
        messageTo(self, "hrc_nbooDomeRemove", null, 3.2f, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDomeRemove(obj_id self, dictionary params) throws InterruptedException
    {
        tangible_dynamics.clearAllForces(self);
        trial.cleanupObject(self);
        return SCRIPT_CONTINUE;
    }
}
