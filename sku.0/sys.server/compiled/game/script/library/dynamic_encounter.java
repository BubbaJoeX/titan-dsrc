package script.library;

import script.*;

/**
 * Data and queries for dynamic encounters. Trigger volumes must be created on the encounter
 * object from its own script (createTriggerVolume/removeTriggerVolume use {@code getSelf()}).
 */
public class dynamic_encounter extends script.base_script
{
    public static final String ZONE_ABILITIES_TABLE = "datatables/dynamic_encounter/zone_abilities.tab";

    public dynamic_encounter()
    {
    }

    public static dictionary getAbilityRow(String abilityName)
    {
        if (abilityName == null || abilityName.length() == 0)
            return null;
        return dataTableGetRow(ZONE_ABILITIES_TABLE, abilityName);
    }

    public static boolean hasVolume(obj_id owner, String volumeName)
    {
        if (owner == null || volumeName == null || volumeName.length() == 0)
            return false;
        return hasTriggerVolume(owner, volumeName);
    }
}
