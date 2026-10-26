package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * A B1 dropped off an MTT rack, folded for transport. The fold is the creature mood "titan_folded": moods reach
 * clients in the baselines, so the droid is folded from the first frame anyone sees it (titan_hrc_nboo_b1.lat wraps
 * every standing loop in a mood selector that holds titan_hrc_b1_folded.ans). After FOLD_HOLD seconds - by then every
 * client in range has loaded it - it plays "power_up" (3.5 s still folded, a boot twitch, 2.5 s unfolding) and the
 * mood goes back to neutral underneath. From the drop until it stands it is passive and takes double damage; then
 * normal AI takes over and it marches on the Gungan line (droid_unit).
 */
public class folded_droid extends script.base_script
{
    public folded_droid()
    {
    }

    public static final String FOLDED_MOOD = "titan_folded";
    public static final float FOLD_HOLD = 2.5f;
    public static final float POWER_UP_SECONDS = 6.0f;
    public static final String SV_DOUBLING = "hrc_nboo.doubling";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setObjVar(self, "hrc_nboo.folded", 1);
        utils.setScriptVar(self, "petIgnoreAttacks", 1);
        stop(self);
        removeTriggerVolume(ai_lib.ALERT_VOLUME_NAME);
        removeTriggerVolume(ai_lib.AGGRO_VOLUME_NAME);
        setAnimationMood(self, FOLDED_MOOD);
        messageTo(self, "hrc_nbooPowerUp", null, FOLD_HOLD, false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooPowerUp(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self))
            return SCRIPT_CONTINUE;
        doAnimationAction(self, "power_up");
        setAnimationMood(self, "neutral");          // under the action, so the droid stands when it ends
        messageTo(self, "hrc_nbooUnfolded", null, POWER_UP_SECONDS, false);
        return SCRIPT_CONTINUE;
    }

    /** Double damage while folded: whatever a hit did, it does again. */
    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id weapon, int[] damage) throws InterruptedException
    {
        if (!hasObjVar(self, "hrc_nboo.folded") || utils.hasScriptVar(self, SV_DOUBLING) || damage == null)
            return SCRIPT_CONTINUE;
        int total = 0;
        for (int d : damage)
            total += Math.max(0, d);
        if (total <= 0 || !naboo_battle_lib.isAlive(self))
            return SCRIPT_CONTINUE;
        utils.setScriptVar(self, SV_DOUBLING, 1);
        damage(self, DAMAGE_KINETIC, HIT_LOCATION_BODY, total);
        utils.removeScriptVar(self, SV_DOUBLING);
        if (isIdValid(attacker) && exists(attacker))
            addHate(self, attacker, total);
        return SCRIPT_CONTINUE;
    }

    /** Stock AI may reset moods on its calm timers: keep the fold until the power-up starts. */
    public int resumeDefaultCalmBehavior(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, "hrc_nboo.folded"))
        {
            stop(self);
            return SCRIPT_OVERRIDE;
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooUnfolded(obj_id self, dictionary params) throws InterruptedException
    {
        removeObjVar(self, "hrc_nboo.folded");
        utils.removeScriptVar(self, "petIgnoreAttacks");
        if (naboo_battle_lib.isAlive(self) && !hasObjVar(self, "hrc_nboo.frozen"))
        {
            ai_lib.resetCombatTriggerVolumes();
            messageTo(self, "hrc_nbooMarch", null, 0.5f, false);
        }
        detachScript(self, naboo_battle_lib.SCRIPT_FOLDED);
        return SCRIPT_CONTINUE;
    }
}
