package script.library;/*
@Origin: dsrc.script.library
@Author: BubbaJoeX
@Purpose: JSON parser/creator for trivial local storage.
@Notes:
    This contents of this script should not be used for anything player facing. This is solely to parse data via java.
@Created: Monday, 10/30/2023, at 6:55 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

public class json extends script.base_script
{

    public String addTo(String json, String key, String value)
    {
        if (json == null || json.isEmpty())
        {
            json = "{";
        }
        else
        {
            json = json.substring(0, json.length() - 1);
            json += ",";
        }
        json += "\"" + key + "\":\"" + value + "\"}";
        return json;
    }

    public String createJSON(String[] keys, String[] values)
    {
        if (keys.length != values.length)
        {
            LOG("json", "createJSON: keys and values must be the same length");
        }
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < keys.length; i++)
        {
            json.append("\"").append(keys[i]).append("\":\"").append(values[i]).append("\"");
            if (i < keys.length - 1)
            {
                json.append(",");
            }
        }
        json.append("}");
        return json.toString();
    }

    public void packageJSON(String json)
    {
        if (json == null || json.isEmpty())
        {
            LOG("json", "packageJSON: json is null or empty");
            return;
        }
        String[] keys = split(json, ',');
        for (int i = 0; i < keys.length; i++)
        {
            String[] key = split(keys[i], ':');
            if (key.length != 2)
            {
                LOG("json", "packageJSON: invalid key/value pair");
                continue;
            }
            String k = key[0].substring(1, key[0].length() - 1);
            String v = key[1].substring(1, key[1].length() - 1);
            LOG("json", "packageJSON: key: " + k + " value: " + v);
        }
    }

    public void deJSONify(String json)
    {
        if (json == null || json.isEmpty())
        {
            LOG("json", "deJSONify: json is null or empty");
            return;
        }
        String[] keys = split(json, ',');
        for (int i = 0; i < keys.length; i++)
        {
            String[] key = split(keys[i], ':');
            if (key.length != 2)
            {
                LOG("json", "deJSONify: invalid key/value pair");
                continue;
            }
            String k = key[0].substring(1, key[0].length() - 1);
            String v = key[1].substring(1, key[1].length() - 1);
            LOG("json", "deJSONify: key: " + k + " value: " + v);
        }
    }
}
