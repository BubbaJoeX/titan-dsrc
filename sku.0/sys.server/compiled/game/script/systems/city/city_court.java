package script.systems.city;

import script.*;
import script.library.*;

public class city_court extends script.base_script
{
    public city_court()
    {
    }

    public int handleEvictionGraceExpired(obj_id self, dictionary params) throws InterruptedException
    {
        int cityId = params.getInt("cityId");

        if (!hasObjVar(self, "city.eviction.initiated_time"))
        {
            return SCRIPT_CONTINUE;
        }

        if (getIntObjVar(self, "city.eviction.appeal_pending") == 1)
        {
            return SCRIPT_CONTINUE;
        }

        city.removeCitizen(self, cityId);

        removeObjVar(self, "city.eviction");

        sendSystemMessage(self, new string_id("city/city", "eviction_complete"));

        String cityName = cityGetName(cityId);
        obj_id cityHall = cityGetCityHall(cityId);
        CustomerServiceLog("player_city", "Eviction completed (grace expired). City: " + cityName + " (" + cityId + "/" + cityHall + ") Citizen: " + self);

        return SCRIPT_CONTINUE;
    }

    public static String[] getEvictionStatus(obj_id citizen) throws InterruptedException
    {
        if (!hasObjVar(citizen, "city.eviction.initiated_time"))
        {
            return null;
        }

        int initiatedTime = getIntObjVar(citizen, "city.eviction.initiated_time");
        String reason = getStringObjVar(citizen, "city.eviction.reason");
        int appealPending = getIntObjVar(citizen, "city.eviction.appeal_pending");
        int graceExpires = getIntObjVar(citizen, "city.eviction.grace_expires");

        int curTime = getGameTime();
        int timeRemaining = graceExpires - curTime;

        String status;
        if (appealPending == 1)
        {
            status = "APPEALED";
        }
        else if (timeRemaining > 0)
        {
            status = "PENDING";
        }
        else
        {
            status = "EXPIRED";
        }

        String[] result = new String[4];
        result[0] = status;
        result[1] = reason;
        result[2] = String.valueOf(timeRemaining);
        result[3] = appealPending == 1 ? "true" : "false";

        return result;
    }

    public static String[] getPendingAppeals(int cityId) throws InterruptedException
    {
        obj_id[] citizens = cityGetCitizenIds(cityId);
        if (citizens == null)
        {
            return new String[0];
        }

        java.util.Vector appeals = new java.util.Vector();

        for (obj_id citizen : citizens)
        {
            if (hasObjVar(citizen, "city.eviction.appeal_pending"))
            {
                if (getIntObjVar(citizen, "city.eviction.appeal_pending") == 1)
                {
                    String citizenName = cityGetCitizenName(cityId, citizen);
                    String reason = getStringObjVar(citizen, "city.eviction.reason");
                    String defense = getStringObjVar(citizen, "city.eviction.appeal_defense");
                    appeals.add(citizen + "|" + citizenName + "|" + reason + "|" + defense);
                }
            }
        }

        String[] result = new String[appeals.size()];
        for (int i = 0; i < appeals.size(); i++)
        {
            result[i] = (String)appeals.get(i);
        }

        return result;
    }
}

