package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * player_script of the heroic_naboo_battle instance (instance_datatable): re-syncs the ground quest with the
 * encounter on arrival, late join and login, and carries the gunner command /titanAatFire of the captured AAT.
 */
public class naboo_battle_player extends script.base_script
{
    public naboo_battle_player()
    {
    }

    public static final String SCRIPT = "theme_park.heroic.naboo_battle.naboo_battle_player";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "hrc_nbooSyncQuest", null, 10.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnLogin(obj_id self) throws InterruptedException
    {
        String area = locations.getBuildoutAreaName(self);
        if (area == null || !area.equals(naboo_battle_lib.INSTANCE_NAME))
        {
            detachScript(self, SCRIPT);
            return SCRIPT_CONTINUE;
        }
        messageTo(self, "hrc_nbooSyncQuest", null, 10.0f, false);
        return SCRIPT_CONTINUE;
    }

    /** Leaving the instance mid-story clears the journal entry; a finished story stays completed. */
    public int OnDetach(obj_id self) throws InterruptedException
    {
        if (groundquests.isQuestActive(self, naboo_battle_lib.QUEST))
            groundquests.clearQuest(self, naboo_battle_lib.QUEST);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooSyncQuest(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id controller = instance.getAreaInstanceController(self);
        if (!isIdValid(controller) || !exists(controller))
        {
            // still on the way in (the script is attached before the warp): try again for a few minutes
            int tries = params != null ? params.getInt("tries") : 0;
            if (tries < 18)
            {
                dictionary d = new dictionary();
                d.put("tries", tries + 1);
                messageTo(self, "hrc_nbooSyncQuest", d, 10.0f, false);
            }
            return SCRIPT_CONTINUE;
        }
        dictionary d = new dictionary();
        d.put("player", self);
        messageTo(controller, "hrc_nbooRequestQuestSync", d, 0.0f, false);
        return SCRIPT_CONTINUE;
    }

    /** /titanAatFire: the gunner of the captured AAT fires a plasma shell at the look-at target. */
    public int cmdTitanAatFire(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        obj_id mount = getMountId(self);
        if (!isIdValid(mount) || !exists(mount) || !hasScript(mount, "theme_park.heroic.naboo_battle.aat_captured"))
        {
            naboo_battle_lib.message(self, "sys_aat_not_gunner");
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(target) || target == self || target == mount)
            target = getLookAtTarget(self);
        dictionary d = new dictionary();
        d.put("player", self);
        if (isIdValid(target) && target != self && target != mount)
            d.put("target", target);
        messageTo(mount, "hrc_nbooGunnerFire", d, 0.0f, false);
        return SCRIPT_CONTINUE;
    }
}
