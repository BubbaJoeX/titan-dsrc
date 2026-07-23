package script.theme_park.poi.tatooine.city;

import script.dictionary;
import script.library.create;
import script.obj_id;

public class poi_city_street_music extends script.base_script
{
    public static final String VAR_MUSICIAN = "musician";
    public static final String VAR_MUSICIAN_OWNER = "streetMusic.owner";
    public static final String VAR_SHUTTING_DOWN = "streetMusic.shuttingDown";
    public static final float MUSICIAN_RECOVERY_RADIUS = 4.0f;
    public poi_city_street_music()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        initializeStreetMusic(self);
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        removeObjVar(self, VAR_SHUTTING_DOWN);
        initializeStreetMusic(self);
        return SCRIPT_CONTINUE;
    }
    public int OnDestroy(obj_id self) throws InterruptedException
    {
        setObjVar(self, VAR_SHUTTING_DOWN, true);
        obj_id musician = getObjIdObjVar(self, VAR_MUSICIAN);
        removeObjVar(self, VAR_MUSICIAN);
        if (exists(musician))
        {
            destroyObject(musician);
        }
        return SCRIPT_CONTINUE;
    }
    public int handlePlaying(obj_id self, dictionary params) throws InterruptedException
    {
        if (!exists(self) || hasObjVar(self, VAR_SHUTTING_DOWN))
        {
            return SCRIPT_CONTINUE;
        }
        messageTo(self, "handlePlaying", null, 600, false);
        obj_id musician = getObjIdObjVar(self, VAR_MUSICIAN);
        if (!exists(musician))
        {
            return SCRIPT_CONTINUE;
        }
        setAnimationMood(musician, "whatever");
        return SCRIPT_CONTINUE;
    }
    public void initializeStreetMusic(obj_id self) throws InterruptedException
    {
        obj_id musician = getObjIdObjVar(self, VAR_MUSICIAN);
        if (!exists(musician))
        {
            musician = findExistingMusician(self);
            if (exists(musician))
            {
                setObjVar(self, VAR_MUSICIAN, musician);
            }
            else
            {
                spawnMusician(self);
            }
        }
        if (!hasMessageTo(self, "handlePlaying"))
        {
            messageTo(self, "handlePlaying", null, 10.0f, false);
        }
        if (hasScript(self, "theme_park.poi.launch") && !hasMessageTo(self, "checkForScripts"))
        {
            messageTo(self, "checkForScripts", null, 10.0f, false);
        }
    }
    public void spawnMusician(obj_id baseObject) throws InterruptedException
    {
        if (hasObjVar(baseObject, VAR_SHUTTING_DOWN))
        {
            return;
        }
        obj_id musician = create.themeParkObject("noble", 1.0f, 0.0f, "objectDestroyed", 0.0f);
        if (!isIdValid(musician))
        {
            LOG("poi_city_street_music", "Unable to create street musician for " + baseObject);
            return;
        }
        obj_id instrument = createObject("object/tangible/instrument/kloo_horn.iff", musician, "");
        if (!isIdValid(instrument))
        {
            LOG("poi_city_street_music", "Unable to equip street musician " + musician + " with a kloo horn");
        }
        setObjVar(musician, VAR_MUSICIAN_OWNER, baseObject);
        setObjVar(baseObject, VAR_MUSICIAN, musician);
    }
    public obj_id findExistingMusician(obj_id baseObject) throws InterruptedException
    {
        obj_id[] musicians = getAllObjectsWithObjVar(getLocation(baseObject), MUSICIAN_RECOVERY_RADIUS, VAR_MUSICIAN_OWNER);
        if (musicians != null)
        {
            for (obj_id musician : musicians)
            {
                if (exists(musician) && getObjIdObjVar(musician, VAR_MUSICIAN_OWNER) == baseObject)
                {
                    return musician;
                }
            }
        }
        return obj_id.NULL_ID;
    }
    public int objectDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        removeObjVar(self, VAR_MUSICIAN);
        if (!exists(self) || hasObjVar(self, VAR_SHUTTING_DOWN))
        {
            return SCRIPT_CONTINUE;
        }
        spawnMusician(self);
        return SCRIPT_CONTINUE;
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
