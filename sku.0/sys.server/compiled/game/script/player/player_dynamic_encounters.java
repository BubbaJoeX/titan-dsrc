package script.player;

import script.*;
import script.library.dynamic_encounter;
import script.library.dynamic_encounter_ui;

/**
 * Zone encounter UI wiring, ability use validation, and script hooks for scripted abilities.
 */
public class player_dynamic_encounters extends script.base_script
{
    public static final String OBJVAR_ACTIVE_ENCOUNTER = "dynamic_encounter.active_encounter";
    public static final String OBJVAR_CD_PREFIX = "dynamic_encounter.cd.";

    public player_dynamic_encounters()
    {
    }

    public int OnAttach(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    /**
     * Helper namespace for UI entry points (ability tray).
     */
    public static class _ui
    {
        public static void showZoneAbilityTray(obj_id player, String encounterKey, String[] abilityNames)
        {
            dynamic_encounter_ui.showZoneAbilityTray(player, encounterKey, abilityNames);
        }

        public static void hideZoneAbilityTray(obj_id player)
        {
            dynamic_encounter_ui.hideZoneAbilityTray(player);
        }
    }

    /**
     * Client → server ability activation (see Client.cpp / ZoneAbilityUseRequest).
     * Params: {@code encounterKey|abilityName}
     */
    public int handleZoneAbilityUseRequest(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (self == null || params == null || params.length() == 0)
            return SCRIPT_CONTINUE;

        int sep = params.indexOf('|');
        if (sep <= 0 || sep >= params.length() - 1)
            return SCRIPT_CONTINUE;

        String encounterKey = params.substring(0, sep).trim();
        String abilityName = params.substring(sep + 1).trim();
        if (encounterKey.length() == 0 || abilityName.length() == 0)
            return SCRIPT_CONTINUE;

        String active = getStringObjVar(self, OBJVAR_ACTIVE_ENCOUNTER);
        if (active == null || !active.equals(encounterKey))
            return SCRIPT_CONTINUE;

        dictionary row = dynamic_encounter.getAbilityRow(abilityName);
        if (row == null)
            return SCRIPT_CONTINUE;

        String skillReq = row.getString("skill_required");
        if (skillReq != null && skillReq.length() > 0 && !hasSkill(self, skillReq))
            return SCRIPT_CONTINUE;

        int cooldown = row.getInt("cooldown");
        if (cooldown > 0)
        {
            String cdKey = OBJVAR_CD_PREFIX + abilityName;
            if (hasObjVar(self, cdKey))
            {
                int end = getIntObjVar(self, cdKey);
                if (getGameTime() < end)
                    return SCRIPT_CONTINUE;
            }
        }

        int damage = row.getInt("damage");
        if (damage == -1)
        {
            dictionary d = new dictionary();
            d.put("encounterKey", encounterKey);
            d.put("abilityName", abilityName);
            messageTo(self, "OnZoneScriptAbility", d, 0, false);
        }
        else if (damage > 0)
        {
            int damageType = row.getInt("damage_type");
            obj_id tgt = getIntendedTarget(self);
            if (isIdValid(tgt) && !isDead(tgt))
            {
                damage(tgt, damageType, HIT_LOCATION_BODY, damage);
            }
        }

        if (cooldown > 0)
        {
            setObjVar(self, OBJVAR_CD_PREFIX + abilityName, getGameTime() + cooldown);
        }

        return SCRIPT_CONTINUE;
    }

    /**
     * Override in additional scripts or handle in message chain for {@code damage == -1} abilities.
     */
    public int OnZoneScriptAbility(obj_id self, dictionary params) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
}
