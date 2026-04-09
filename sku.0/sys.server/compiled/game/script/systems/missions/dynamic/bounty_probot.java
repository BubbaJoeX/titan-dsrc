package script.systems.missions.dynamic;

import script.dictionary;
import script.library.utils;
import script.location;
import script.obj_id;

public class bounty_probot extends script.systems.missions.base.mission_dynamic_base
{
    public static final String OBJVAR_HUNT_MODE = "bh.huntMode";
    public static final String OBJVAR_HUNT_TARGET = "bh.hunt.target";
    public static final String OBJVAR_HUNT_HUNTER = "bh.hunt.hunter";
    public static final String OBJVAR_HUNT_MISSION = "bh.hunt.mission";
    public static final String OBJVAR_HUNT_DROID_TYPE = "bh.hunt.droidType";
    public bounty_probot()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        setInvulnerable(self, true);
        messageTo(self, "destroySelf", null, 180, true);
        if (!hasScript(self, "conversation.bounty_probot"))
        {
            attachScript(self, "conversation.bounty_probot");
        }
        return SCRIPT_CONTINUE;
    }
    public int setup_Droid(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id objWaypoint = params.getObjId("objWaypoint");
        obj_id objPlayer = params.getObjId("objPlayer");
        obj_id objMission = params.getObjId("objMission");
        int intTrackType = params.getInt("intTrackType");
        int intDroidType = params.getInt("intDroidType");
        setObjVar(self, "objPlayer", objPlayer);
        setObjVar(self, "objMission", objMission);
        setObjVar(self, "intTrackType", intTrackType);
        setObjVar(self, "intDroidType", intDroidType);
        setObjVar(self, "objWaypoint", objWaypoint);
        return SCRIPT_CONTINUE;
    }
    public int setupHuntTarget(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = params.getObjId("target");
        obj_id hunter = params.getObjId("hunter");
        obj_id mission = params.getObjId("mission");
        int droidType = params.getInt("droidType");
        if (!isIdValid(target) || !isPlayer(target))
        {
            messageTo(self, "delete_Self", null, 0.0f, true);
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, OBJVAR_HUNT_MODE, 1);
        setObjVar(self, OBJVAR_HUNT_TARGET, target);
        setObjVar(self, OBJVAR_HUNT_HUNTER, hunter);
        setObjVar(self, OBJVAR_HUNT_MISSION, mission);
        setObjVar(self, OBJVAR_HUNT_DROID_TYPE, droidType);
        setInvulnerable(self, false);
        setMovementRun(self);
        follow(self, target, 2.0f, 4.0f);
        pvpSetPermanentPersonalEnemyFlag(self, target);
        pvpSetPermanentPersonalEnemyFlag(target, self);
        startCombat(self, target);
        addHate(self, target, 1000.0f);
        messageTo(self, "huntPulse", null, 3.0f, true);
        messageTo(self, "destroySelf", null, 240.0f, true);
        return SCRIPT_CONTINUE;
    }
    public int refreshHuntTarget(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_HUNT_MODE) || getIntObjVar(self, OBJVAR_HUNT_MODE) != 1)
        {
            return setupHuntTarget(self, params);
        }
        if (params != null && !params.isEmpty())
        {
            obj_id target = params.getObjId("target");
            obj_id hunter = params.getObjId("hunter");
            obj_id mission = params.getObjId("mission");
            int droidType = params.getInt("droidType");
            if (isIdValid(target))
            {
                setObjVar(self, OBJVAR_HUNT_TARGET, target);
                follow(self, target, 2.0f, 4.0f);
                startCombat(self, target);
                addHate(self, target, 1000.0f);
            }
            if (isIdValid(hunter))
            {
                setObjVar(self, OBJVAR_HUNT_HUNTER, hunter);
            }
            if (isIdValid(mission))
            {
                setObjVar(self, OBJVAR_HUNT_MISSION, mission);
            }
            if (droidType == DROID_PROBOT || droidType == DROID_SEEKER)
            {
                setObjVar(self, OBJVAR_HUNT_DROID_TYPE, droidType);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int huntPulse(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_HUNT_MODE) || getIntObjVar(self, OBJVAR_HUNT_MODE) != 1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = hasObjVar(self, OBJVAR_HUNT_TARGET) ? getObjIdObjVar(self, OBJVAR_HUNT_TARGET) : obj_id.NULL_ID;
        obj_id hunter = hasObjVar(self, OBJVAR_HUNT_HUNTER) ? getObjIdObjVar(self, OBJVAR_HUNT_HUNTER) : obj_id.NULL_ID;
        if (!isIdValid(target) || !exists(target) || isDead(target))
        {
            messageTo(self, "delete_Self", null, 0.0f, true);
            return SCRIPT_CONTINUE;
        }
        follow(self, target, 2.0f, 4.0f);
        startCombat(self, target);
        addHate(self, target, 1000.0f);
        if (getDistance(self, target) <= 64.0f)
        {
            int pettyDamage = rand(100, 200);
            damage(target, DAMAGE_KINETIC, HIT_LOCATION_BODY, pettyDamage);
        }
        if (isIdValid(hunter) && exists(hunter))
        {
            location hLoc = getLocation(hunter);
            location tLoc = getLocation(target);
            if (hLoc != null && tLoc != null && hLoc.area.equals(tLoc.area) && getDistance(hunter, target) <= 96.0f)
            {
                sendSystemMessage(hunter, "[BH Intel] Probe shadow has target in visual range.", null);
                messageTo(self, "delete_Self", null, 0.0f, true);
                return SCRIPT_CONTINUE;
            }
        }
        messageTo(self, "huntPulse", null, rand(6, 10), true);
        return SCRIPT_CONTINUE;
    }
    public int destroySelf(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] objPlayers = getAllPlayers(getLocation(self), 64);
        if (objPlayers != null && objPlayers.length > 0)
        {
            playClientEffectLoc(objPlayers[0], "clienteffect/combat_explosion_lair_large.cef", getLocation(self), 0);
        }
        obj_id objMission = getObjIdObjVar(self, "objMission");
        messageTo(objMission, "stopTracking", null, 0, true);
        messageTo(self, "delete_Self", null, 0, true);
        return SCRIPT_CONTINUE;
    }
    public int delete_Self(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, OBJVAR_HUNT_TARGET))
        {
            obj_id target = getObjIdObjVar(self, OBJVAR_HUNT_TARGET);
            if (isIdValid(target) && utils.hasScriptVar(target, "bh.remoteProbe.activeDroid"))
            {
                obj_id active = utils.getObjIdScriptVar(target, "bh.remoteProbe.activeDroid");
                if (active == self)
                {
                    utils.removeScriptVar(target, "bh.remoteProbe.activeDroid");
                }
            }
        }
        obj_id objWaypoint = getObjIdObjVar(self, "objWaypoint");
        destroyObject(objWaypoint);
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
    public int take_Off(obj_id self, dictionary params) throws InterruptedException
    {
        doAnimationAction(self, "sp_13");
        dictionary dctParams = new dictionary();
        obj_id objMission = getObjIdObjVar(self, "objMission");
        int intTrackType = getIntObjVar(self, "intTrackType");
        int intDroidType = getIntObjVar(self, "intDroidType");
        dctParams.put("intDroidType", intDroidType);
        dctParams.put("intTrackType", DROID_FIND_TARGET);
        messageTo(objMission, "findTarget", dctParams, 10, true);
        utils.sendPostureChange(self, POSTURE_SITTING);
        messageTo(self, "delete_Self", null, 10, true);
        return SCRIPT_CONTINUE;
    }
    public int OnIncapacitated(obj_id self, obj_id attacker) throws InterruptedException
    {
        messageTo(self, "delete_Self", null, 0.0f, true);
        return SCRIPT_CONTINUE;
    }
    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        messageTo(self, "delete_Self", null, 0.0f, true);
        return SCRIPT_CONTINUE;
    }
}
