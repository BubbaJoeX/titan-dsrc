package script.theme_park.poi.tatooine.city;

import script.dictionary;
import script.library.ai_lib;
import script.library.create;
import script.location;
import script.obj_id;

public class poi_city_droid_convo extends script.base_script
{
    public poi_city_droid_convo()
    {
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        obj_id guy = getObjIdObjVar(self, "guy1");
        if (!exists(guy))
        {
            guy = spawnGuyOne(self);
        }
        obj_id guy2 = getObjIdObjVar(self, "guy2");
        if (!exists(guy2))
        {
            guy2 = spawnGuyTwo(self);
        }
        if (!exists(getObjIdObjVar(self, "droidSpawn")))
        {
            spawnDroid(self);
        }
        faceTo(guy, guy2);
        if (!hasMessageTo(self, "handleChatting"))
        {
            messageTo(self, "handleChatting", null, 10.0f, false);
        }
        if (!hasMessageTo(self, "checkForScripts"))
        {
            messageTo(self, "checkForScripts", null, 10, true);
        }
        return SCRIPT_CONTINUE;
    }
    public obj_id spawnGuyOne(obj_id baseObject) throws InterruptedException
    {
        location here = getLocation(baseObject);
        here.x = here.x + 1;
        obj_id guy1 = create.object("commoner", here);
        setObjVar(baseObject, "guy1", guy1);
        return guy1;
    }
    public obj_id spawnGuyTwo(obj_id baseObject) throws InterruptedException
    {
        location here = getLocation(baseObject);
        here.x = here.x - 1;
        obj_id guy2 = create.object("commoner", here);
        setObjVar(baseObject, "guy2", guy2);
        return guy2;
    }
    public void spawnDroid(obj_id baseObject) throws InterruptedException
    {
        if (exists(getObjIdObjVar(baseObject, "droidSpawn")))
        {
            return;
        }
        location here = getLocation(baseObject);
        here.z = here.z + 1;
        String droidToSpawn = getRandomDroid();
        obj_id droid = create.object(droidToSpawn, here);
        ai_lib.setDefaultCalmBehavior(droid, ai_lib.BEHAVIOR_SENTINEL);
        setObjVar(baseObject, "droidSpawn", droid);
    }
    public int handleDeadGuyOne(obj_id self, dictionary params) throws InterruptedException
    {
        spawnGuyOne(self);
        return SCRIPT_CONTINUE;
    }
    public int handleDeadGuyTwo(obj_id self, dictionary params) throws InterruptedException
    {
        spawnGuyTwo(self);
        return SCRIPT_CONTINUE;
    }
    public int handleDeadDroid(obj_id self, dictionary params) throws InterruptedException
    {
        removeObjVar(self, "droidSpawn");
        spawnDroid(self);
        return SCRIPT_CONTINUE;
    }
    public int handleChatting(obj_id self, dictionary params) throws InterruptedException
    {
        messageTo(self, "handleChatting", null, 600, false);
        obj_id guy1 = getObjIdObjVar(self, "guy1");
        obj_id guy2 = getObjIdObjVar(self, "guy2");
        if (!exists(guy1) || !exists(guy2))
        {
            return SCRIPT_CONTINUE;
        }
        faceTo(guy1, guy2);
        faceTo(guy2, guy1);
        setAnimationMood(guy1, "conversation");
        setAnimationMood(guy2, "conversation");
        return SCRIPT_CONTINUE;
    }
    public String getRandomDroid() throws InterruptedException
    {
        int dnum = rand(2, 5);
        String droid = "r" + dnum;
        return droid;
    }
    public int checkForScripts(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasScript(self, "theme_park.poi.launch"))
        {
            detachScript(self, "theme_park.poi.launch");
        }
        return SCRIPT_CONTINUE;
    }
}
