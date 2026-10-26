package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * Gungan Grand Army NPCs (warriors, kaadu riders, Captain Tarpals). creatures.tab gives them ignorePlayer (players
 * cannot hit them and they never attack players) and the faction hrc_nboo_gungan, whose enemy in faction.tab is
 * hrc_nboo_droid, so their aggro volumes pick fights with droids on their own. This script adds the warriors' energy
 * shields (hp_dyn app on lWrist) and, for the battle line, actively engages the nearest droid in reach so the line
 * always fights. Roles (spawn objvar hrc_nboo.role): line, staging, tarpals.
 */
public class gungan_ally extends script.base_script
{
    public gungan_ally()
    {
    }

    public static final String HAND_SHIELD = "appearance/titan_hrc_nboo_hand_shield.apt";
    public static final float TICK = 4.0f;
    public static final float ENGAGE_RANGE = 50.0f;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooGunganSetup", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    public String role(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, "hrc_nboo.role") ? getStringObjVar(self, "hrc_nboo.role") : "line";
    }

    public int hrc_nbooGunganSetup(obj_id self, dictionary params) throws InterruptedException
    {
        naboo_battle_lib.snapToGround(self, 0.0f);
        String type = hasObjVar(self, "creature_type") ? getStringObjVar(self, "creature_type") : "";
        if (type.equals("hrc_nboo_gungan_warrior"))
            naboo_battle_lib.setAppSlot(self, "hand_shield", "lWrist", HAND_SHIELD, 0.0f, 0.0f, 0.0f);
        aiSetHomeLocation(self, getLocation(self));
        if (role(self).equals("line"))
            messageTo(self, "hrc_nbooGunganTick", null, TICK + rand(0.0f, 2.0f), false);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooGunganTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!naboo_battle_lib.isAlive(self))
            return SCRIPT_CONTINUE;
        if (!ai_lib.isInCombat(self))
        {
            location here = getLocation(self);
            obj_id droid = naboo_battle_lib.nearest(naboo_battle_lib.droidsInRange(here, ENGAGE_RANGE), here);
            if (isIdValid(droid) && pvpCanAttack(self, droid))
                startCombat(self, droid);
        }
        messageTo(self, "hrc_nbooGunganTick", null, TICK, false);
        return SCRIPT_CONTINUE;
    }

    /** Players never hurt the Grand Army (ignorePlayer also stops most attacks before they land). */
    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id weapon, int[] damage) throws InterruptedException
    {
        if (!isIdValid(attacker) || damage == null)
            return SCRIPT_CONTINUE;
        obj_id master = isPlayer(attacker) ? attacker : getMaster(attacker);
        if (!isIdValid(master) || !isPlayer(master))
            return SCRIPT_CONTINUE;
        int total = 0;
        for (int d : damage)
            total += Math.max(0, d);
        if (total > 0 && naboo_battle_lib.isAlive(self))
            addToHealth(self, total);
        return SCRIPT_CONTINUE;
    }
}
