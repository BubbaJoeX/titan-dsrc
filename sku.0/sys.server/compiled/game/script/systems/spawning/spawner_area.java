package script.systems.spawning;

import script.*;
import script.library.*;

public class spawner_area extends script.base_script
{
    public spawner_area()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, "registerWithController"))
        {
            setObjVar(self, "registerWithController", 1);
        }
        if (hasObjVar(self, "strName"))
        {
            setName(self, getStringObjVar(self, "strName"));
        }
        if (canSpawnByConfigSetting())
        {
            messageTo(self, "doSpawnEvent", null, 20, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, "registerWithController"))
        {
            setObjVar(self, "registerWithController", 1);
        }
        if (hasObjVar(self, "strName"))
        {
            setName(self, getStringObjVar(self, "strName"));
        }
        if (canSpawnByConfigSetting())
        {
            messageTo(self, "doSpawnEvent", null, 20, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int doSpawnEvent(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, "spawner_lock"))
        {
            setName(self, "(LOCKED SPAWNER: " + getStringObjVar(self, "strSpawns") + ")");
            return SCRIPT_CONTINUE;
        }
        if (!tod.canSpawnForTimePolicy(self))
        {
            if (tod.hasTimeSpawnPolicy(self))
            {
                messageTo(self, "doSpawnEvent", null, tod.getTimePolicyRetrySeconds(self), false);
            }
            return SCRIPT_CONTINUE;
        }
        if (!spawning.checkSpawnCount(self))
        {
            return SCRIPT_CONTINUE;
        }
        int intGoodLocationSpawner = getIntObjVar(self, "intGoodLocationSpawner");
        float fltRadius = getFloatObjVar(self, "fltRadius");
        String strSpawnType = getStringObjVar(self, "strSpawns");
        float fltSize = 8.0f;
        String strSpawn = strSpawnType;
        String strFileName = "datatables/spawning/ground_spawning/types/" + strSpawnType + ".iff";
        if (dataTableOpen(strFileName))
        {
            String[] strSpawns = dataTableGetStringColumnNoDefaults(strFileName, "strItem");
            float[] fltSizes = dataTableGetFloatColumn(strFileName, "fltSize");
            if (strSpawns == null || strSpawns.length == 0)
            {
                setName(self, "Mangled spawner. strFileName is " + strFileName + " I couldnt find any spawns in that file.");
            }
            if (fltSizes.length == 0)
            {
                setName(self, "Missing spawn sizes in " + strFileName);
            }
            if (fltSizes.length != strSpawns.length)
            {
                setName(self, "MISSING VALUES: Each spawn must have an associated size (" + strFileName + ")");
            }
            int intRoll = rand(0, strSpawns.length - 1);
            if(intRoll >= fltSizes.length) {
                intRoll = fltSizes.length - 1;
            }
            strSpawn = strSpawns[intRoll];
            fltSize = fltSizes[intRoll];
        }
        if (strSpawn == null || strSpawn.length() < 1)
        {
            setName(self, "Mangled spawner. strSpawn objvar is missing.");
            return SCRIPT_CONTINUE;
        }
        location locTest = spawning.getRandomLocationInCircle(getLocation(self), fltRadius);
        fltSize = getClosestSize(fltSize);
        if (intGoodLocationSpawner > 0)
        {
            requestLocation(self, strSpawn, locTest, rand(100, 200), fltSize, true, true);
        }
        else 
        {
            createMob(strSpawn, null, locTest, fltRadius, self);
        }
        return SCRIPT_CONTINUE;
    }
    public float getClosestSize(float fltOriginalSize) throws InterruptedException
    {
        if (fltOriginalSize <= 4.0f)
        {
            return 4.0f;
        }
        if (fltOriginalSize <= 8.0f)
        {
            return 8.0f;
        }
        if (fltOriginalSize <= 12.0f)
        {
            return 12.0f;
        }
        if (fltOriginalSize <= 16.0f)
        {
            return 16.0f;
        }
        if (fltOriginalSize <= 32.0f)
        {
            return 32.0f;
        }
        else if (fltOriginalSize <= 48.0f)
        {
            return 48.0f;
        }
        else if (fltOriginalSize <= 64.0f)
        {
            return 64.0f;
        }
        else if (fltOriginalSize <= 80.0f)
        {
            return 80.0f;
        }
        else if (fltOriginalSize <= 96.0f)
        {
            return 96.0f;
        }
        return 32.0f;
    }
    private float getRespawnTime(obj_id self){
        try {
            if (hasObjVar(self, "fltRespawnTime")) {
                return getFloatObjVar(self, "fltRespawnTime");
            } else {
                return rand(getFloatObjVar(self, "fltMinSpawnTime"), getFloatObjVar(self, "fltMaxSpawnTime"));
            }
        }
        catch(Exception e){
            setName(self, "BAD SPAWNEGG!!  Could not get a respawn time from datatable.");
            return 0;
        }
    }
    public void createMob(String strId, obj_id objLocationObject, location locLocation, float fltRadius, obj_id self) throws InterruptedException
    {
        if (!tod.canSpawnForTimePolicy(self))
        {
            if (tod.hasTimeSpawnPolicy(self))
            {
                messageTo(self, "doSpawnEvent", null, tod.getTimePolicyRetrySeconds(self), false);
            }
            return;
        }
        if (!spawning.checkSpawnCount(self))
        {
            return;
        }
        int intIndex = strId.indexOf(".iff");

        if (isIdValid(objLocationObject))
        {
            destroyObject(objLocationObject);
        }
        obj_id objTemplate;
        if (intIndex > -1)
        {
            objTemplate = createObject(strId, locLocation);
            if (!isIdValid(objTemplate))
            {
                setName(self, "BAD MOB OF TYPE " + strId);
                return;
            }
        }
        else 
        {
            objTemplate = create.object(strId, locLocation);
            if (!isIdValid(objTemplate))
            {
                setName(self, "BAD MOB OF TYPE " + strId);
                return;
            }
            int intBehavior = getIntObjVar(self, "intDefaultBehavior");
            ai_lib.setDefaultCalmBehavior(objTemplate, intBehavior);
        }
        float fltRespawnTime = getRespawnTime(self);
        spawning.incrementSpawnCount(self);
        spawning.addToSpawnDebugList(self, objTemplate);
        setObjVar(objTemplate, "objParent", self);
        setObjVar(objTemplate, "fltRespawnTime", fltRespawnTime);
        attachScript(objTemplate, "systems.spawning.spawned_tracker");
        setYaw(objTemplate, getYaw(self));

        if (!spawning.checkSpawnCount(self))
        {
            return;
        }
        messageTo(self, "doSpawnEvent", null, fltRespawnTime, false);
    }
    public int OnLocationReceived(obj_id self, String strId, obj_id objLocationObject, location locLocation, float fltRadius) throws InterruptedException
    {
        if (isIdValid(objLocationObject))
        {
            createMob(strId, objLocationObject, locLocation, fltRadius, self);
        }
        else 
        {
            messageTo(self, "doSpawnEvent", null, getRespawnTime(self), false);
        }
        return SCRIPT_CONTINUE;
    }
    public int spawnDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        int intCurrentSpawnCount = utils.getIntScriptVar(self, "intCurrentSpawnCount");
        intCurrentSpawnCount--;
        if (intCurrentSpawnCount >= 0)
        {
            utils.setScriptVar(self, "intCurrentSpawnCount", intCurrentSpawnCount);
        }
        else 
        {
            utils.setScriptVar(self, "intCurrentSpawnCount", 0);
        }
        messageTo(self, "doSpawnEvent", null, 2, false);
        return SCRIPT_CONTINUE;
    }
    public boolean canSpawnByConfigSetting() throws InterruptedException
    {
        return true;
    }
    public int OnDestroy(obj_id self) throws InterruptedException {
        if (utils.hasScriptVar(self, "debugSpawnList")) {
            obj_id[] spawns = utils.getObjIdArrayScriptVar(self, "debugSpawnList");
            if (spawns == null) {
                return SCRIPT_CONTINUE;
            }
            for (obj_id spawn : spawns) {
                if (isIdValid(spawn) && exists(spawn)) {
                    messageTo(spawn, "selfDestruct", null, 5, false);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int dad = mi.addRootMenu(menu_info_types.SERVER_MENU10, new string_id("Spawner Controls"));
        mi.addSubMenu(dad, menu_info_types.SERVER_MENU11, new string_id("Force Spawn"));
        mi.addSubMenu(dad, menu_info_types.SERVER_MENU12, new string_id("Toggle Spawner"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU11)
        {
            messageTo(self, "doSpawnEvent", null, 2.0f, false);
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU11) {
            boolean status = getBooleanObjVar(self, "spawner_lock");
            if (status)
            {
                removeObjVar(self, "spawner_lock");
            }
            else {
                setObjVar(self,"spawner_lock", true);
            }
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
}
