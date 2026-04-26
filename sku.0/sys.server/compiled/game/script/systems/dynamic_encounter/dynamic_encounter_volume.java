package script.systems.dynamic_encounter;

import script.*;
import script.library.dynamic_encounter_ui;
import script.player.player_dynamic_encounters;

/**
 * Attach to an encounter object. Objvars:
 * <ul>
 *   <li>{@code dynamic_encounter.volume_name} — trigger volume name (default {@code encounter_trigger})</li>
 *   <li>{@code dynamic_encounter.radius} — sphere radius (default 10)</li>
 *   <li>{@code dynamic_encounter.ability_list} — comma-separated keys into {@code zone_abilities.tab}</li>
 *   <li>{@code dynamic_encounter.encounter_key} — optional id string; defaults to this object's id string</li>
 * </ul>
 */
public class dynamic_encounter_volume extends script.base_script
{
    public int OnAttach(obj_id self) throws InterruptedException
    {
        String volName = getStringObjVar(self, "dynamic_encounter.volume_name");
        if (volName == null || volName.length() == 0)
            volName = "encounter_trigger";

        float radius = 10.0f;
        if (hasObjVar(self, "dynamic_encounter.radius"))
            radius = getFloatObjVar(self, "dynamic_encounter.radius");

        createTriggerVolume(volName, radius, true);
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        String volName = getStringObjVar(self, "dynamic_encounter.volume_name");
        if (volName == null || volName.length() == 0)
            volName = "encounter_trigger";
        removeTriggerVolume(volName);
        return SCRIPT_CONTINUE;
    }

    public int OnTriggerVolumeEntered(obj_id self, String volumeName, obj_id breacher) throws InterruptedException
    {
        if (!isPlayer(breacher))
            return SCRIPT_CONTINUE;

        String volName = getStringObjVar(self, "dynamic_encounter.volume_name");
        if (volName == null || volName.length() == 0)
            volName = "encounter_trigger";
        if (!volName.equals(volumeName))
            return SCRIPT_CONTINUE;

        String abilitiesCsv = getStringObjVar(self, "dynamic_encounter.ability_list");
        String[] abilities = dynamic_encounter_ui.parseAbilityListCsv(abilitiesCsv);
        if (abilities == null)
            return SCRIPT_CONTINUE;

        String encounterKey = getStringObjVar(self, "dynamic_encounter.encounter_key");
        if (encounterKey == null || encounterKey.length() == 0)
            encounterKey = self.toString();

        setObjVar(breacher, player_dynamic_encounters.OBJVAR_ACTIVE_ENCOUNTER, encounterKey);
        player_dynamic_encounters._ui.showZoneAbilityTray(breacher, encounterKey, abilities);
        return SCRIPT_CONTINUE;
    }

    public int OnTriggerVolumeExited(obj_id self, String volumeName, obj_id breacher) throws InterruptedException
    {
        if (!isPlayer(breacher))
            return SCRIPT_CONTINUE;

        String volName = getStringObjVar(self, "dynamic_encounter.volume_name");
        if (volName == null || volName.length() == 0)
            volName = "encounter_trigger";
        if (!volName.equals(volumeName))
            return SCRIPT_CONTINUE;

        removeObjVar(breacher, player_dynamic_encounters.OBJVAR_ACTIVE_ENCOUNTER);
        player_dynamic_encounters._ui.hideZoneAbilityTray(breacher);
        return SCRIPT_CONTINUE;
    }
}
