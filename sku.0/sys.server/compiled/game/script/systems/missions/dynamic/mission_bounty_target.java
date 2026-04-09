package script.systems.missions.dynamic;

import script.dictionary;
import script.library.ai_lib;
import script.library.bounty_hunter;
import script.library.missions;
import script.obj_id;

public class mission_bounty_target extends script.systems.missions.base.mission_dynamic_base
{
    public mission_bounty_target()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        obj_id objHunter = getObjIdObjVar(self, "objHunter");
        if (objHunter != null)
        {
            pvpSetPermanentPersonalEnemyFlag(self, objHunter);
        }
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_SENTINEL);
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        obj_id objHunter = getObjIdObjVar(self, "objHunter");
        if (objHunter != null)
        {
            pvpSetPermanentPersonalEnemyFlag(self, objHunter);
        }
        if (!hasScript(self, "systems.missions.base.mission_cleanup_tracker"))
        {
            attachScript(self, "systems.missions.base.mission_cleanup_tracker");
        }
        return SCRIPT_CONTINUE;
    }
    public int OnRemovingFromWorld(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int OnAddedToWorld(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int OnDeath(obj_id self, obj_id objKiller, obj_id objCorpse) throws InterruptedException
    {
        LOG("mission", "Killed by " + objKiller);
        setObjVar(self, "intKilled", 1);
        obj_id mission = getObjIdObjVar(self, "objMission");
        if (isIdValid(mission))
        {
            setObjVar(mission, "intState", missions.STATE_MISSION_COMPLETE);
            messageTo(mission, "bountySuccess", null, 0, true);
        }
        setObjVar(self, "intKilled", 1);
        return SCRIPT_CONTINUE;
    }
    public int OnIncapacitated(obj_id self, obj_id objKiller) throws InterruptedException
    {
        int intHealth = getHealth(self);
        int intAction = getAction(self);
        int intMind = getMind(self);
        if (intHealth < 0)
        {
            setHealth(self, -10000);
        }
        if (intAction < 0)
        {
            setAction(self, -10000);
        }
        if (intMind < 0)
        {
            setMind(self, -10000);
        }
        setObjVar(self, "intKilled", 1);
        obj_id mission = getObjIdObjVar(self, "objMission");
        if (isIdValid(mission) && !hasObjVar(mission, "intCompleted"))
        {
            dictionary d = new dictionary();
            d.put("capture", 1);
            setObjVar(mission, "intState", missions.STATE_MISSION_COMPLETE);
            messageTo(mission, "bountySuccess", d, 0.0f, true);
            obj_id hunter = getMissionHolder(mission);
            if (isIdValid(hunter))
            {
                bounty_hunter.advanceInvestigationStage(mission, "capture", 0.15f);
                sendSystemMessage(hunter, "[BH] Target captured alive.", null);
            }
            messageTo(self, "destroySelf", null, 6.0f, true);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnRecapacitated(obj_id self) throws InterruptedException
    {
        return SCRIPT_OVERRIDE;
    }
    public int destroySelf(obj_id self, dictionary params) throws InterruptedException
    {
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
}
