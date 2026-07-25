package script.systems.companion;

import script.*;
import script.library.*;
import script.systems.combat.combat_base;

/**
 * Runs combat actions from the companion pet bar with {@code testPetBar} so heals/attacks originate on the companion.
 */
public class companion_combat_helper extends combat_base
{
    public companion_combat_helper()
    {
    }
    public static boolean castAbilityFromCompanionBar(obj_id player, String actionName, obj_id target) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || actionName == null || actionName.length() < 1)
        {
            return false;
        }
        obj_id pet = companion_lib.getPetBarCombatCreature(player);
        if (!companion_lib.isStoryCompanionPet(pet))
        {
            return false;
        }
        if (!isIdValid(target))
        {
            target = getIntendedTarget(player);
        }
        if (!isIdValid(target))
        {
            target = getLookAtTarget(player);
        }
        if (!isIdValid(target))
        {
            target = player;
        }
        companion_combat_helper helper = new companion_combat_helper();
        return helper.combatStandardAction(actionName, player, target, "", "handleCompanionCastSuccess", "handleCompanionCastFail", true);
    }
    public int handleCompanionCastSuccess(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int handleCompanionCastFail(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
}
