package script.library;

import script.*;

import java.util.StringTokenizer;

/**
 * Builds payloads for the client zone ability tray and sends updates via native hooks.
 * Payload format (per ability, joined by '|'):
 * encounterKey^name^icon^damage^damageType^cooldown^skill^displayName^displayDescription
 */
public class dynamic_encounter_ui extends script.base_script
{
    public static final String ZONE_ABILITIES_TABLE = dynamic_encounter.ZONE_ABILITIES_TABLE;

    public dynamic_encounter_ui()
    {
    }

    public static void showZoneAbilityTray(obj_id player, String encounterKey, String[] abilityNames)
    {
        if (player == null || encounterKey == null || abilityNames == null || abilityNames.length == 0)
            return;
        String payload = buildTrayPayload(encounterKey, abilityNames);
        if (payload == null || payload.length() == 0)
            return;
        base_class.sendZoneAbilityTrayUpdate(player, payload);
    }

    public static void hideZoneAbilityTray(obj_id player)
    {
        if (player == null)
            return;
        base_class.sendZoneAbilityTrayClose(player);
    }

    public static String buildTrayPayload(String encounterKey, String[] abilityNames)
    {
        if (encounterKey == null || abilityNames == null)
            return null;
        StringBuilder sb = new StringBuilder(256);
        boolean first = true;
        for (int i = 0; i < abilityNames.length; ++i)
        {
            String name = abilityNames[i];
            if (name == null || name.length() == 0)
                continue;
            dictionary row = dataTableGetRow(ZONE_ABILITIES_TABLE, name);
            if (row == null)
                continue;
            int damage = row.getInt("damage");
            int damageType = row.getInt("damage_type");
            int cooldown = row.getInt("cooldown");
            String icon = row.getString("icon");
            String displayName = row.getString("display_name");
            String displayDesc = row.getString("display_description");
            String skillReq = row.getString("skill_required");
            if (icon == null)
                icon = "";
            if (displayName == null)
                displayName = name;
            if (displayDesc == null)
                displayDesc = "";
            if (skillReq == null)
                skillReq = "";
            displayDesc = displayDesc.replace('|', ' ');
            displayDesc = displayDesc.replace('^', ' ');
            if (!first)
                sb.append('|');
            first = false;
            sb.append(encounterKey);
            sb.append('^');
            sb.append(name);
            sb.append('^');
            sb.append(icon);
            sb.append('^');
            sb.append(damage);
            sb.append('^');
            sb.append(damageType);
            sb.append('^');
            sb.append(cooldown);
            sb.append('^');
            sb.append(skillReq);
            sb.append('^');
            sb.append(displayName);
            sb.append('^');
            sb.append(displayDesc);
        }
        if (first)
            return null;
        return sb.toString();
    }

    /**
     * Parse comma-separated ability list from an objvar string.
     */
    public static String[] parseAbilityListCsv(String csv)
    {
        if (csv == null || csv.length() == 0)
            return null;
        StringTokenizer st = new StringTokenizer(csv, ",");
        int n = st.countTokens();
        if (n == 0)
            return null;
        String[] out = new String[n];
        for (int i = 0; i < n; ++i)
            out[i] = st.nextToken().trim();
        return out;
    }
}
