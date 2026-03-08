package script.systems.city;

import script.*;
import script.library.*;

public class city_expulsion_handler extends script.base_script
{
    public city_expulsion_handler()
    {
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setObjVar(self, "city.expulsion.heartbeat_active", 1);
        messageTo(self, "handleExpulsionHeartbeat", null, 5.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int handleExpulsionGraceExpired(obj_id self, dictionary params) throws InterruptedException
    {
        int cityId = params.getInt("cityId");

        if (!city.isInCityBounds(self, cityId))
        {
            city.clearExpulsion(self);
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, "city.expulsion.attackable", 1);

        sendSystemMessage(self, city.SID_NOW_ATTACKABLE_BY_MILITIA);

        playClientEffectObj(self, "clienteffect/combat_special_defender_flinch.cef", self, "root");

        return SCRIPT_CONTINUE;
    }

    public int handleExpulsionHeartbeat(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, "city.expulsion.city_id"))
        {
            city.clearExpulsion(self);
            return SCRIPT_CONTINUE;
        }

        int cityId = getIntObjVar(self, "city.expulsion.city_id");

        if (!city.isInCityBounds(self, cityId))
        {
            city.clearExpulsion(self);
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(self, "city.expulsion.heartbeat_active"))
        {
            messageTo(self, "handleExpulsionHeartbeat", null, 5.0f, false);
        }

        return SCRIPT_CONTINUE;
    }

    public int handleExpulsionTEFExpired(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, "city.expulsion.tef_active"))
        {
            return SCRIPT_CONTINUE;
        }

        int tefExpires = getIntObjVar(self, "city.expulsion.tef_expires");
        int curTime = getGameTime();

        if (curTime >= tefExpires)
        {
            removeObjVar(self, "city.expulsion.tef_active");
            removeObjVar(self, "city.expulsion.tef_expires");
            sendSystemMessage(self, new string_id("city/city", "tef_expired"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "city.expulsion.heartbeat_active");
        return SCRIPT_CONTINUE;
    }

    public int OnDefenderCombatAction(obj_id self, obj_id attacker, obj_id weapon, int combatType, int hitLocation, int damage, int bleed, int critical) throws InterruptedException
    {
        if (!hasObjVar(self, "city.expulsion.city_id"))
        {
            return SCRIPT_CONTINUE;
        }

        if (getIntObjVar(self, "city.expulsion.attackable") != 1)
        {
            return SCRIPT_CONTINUE;
        }

        int cityId = getIntObjVar(self, "city.expulsion.city_id");

        if (city.hasMilitiaFlag(attacker, cityId))
        {
            city.applyExpulsionTEF(self);
        }

        return SCRIPT_CONTINUE;
    }

    public int OnAboutToBeTransferred(obj_id self, obj_id destContainer, obj_id transferer) throws InterruptedException
    {
        if (!hasObjVar(self, "city.expulsion.tef_active"))
        {
            return SCRIPT_CONTINUE;
        }

        if (getIntObjVar(self, "city.expulsion.tef_active") != 1)
        {
            return SCRIPT_CONTINUE;
        }

        if (!isIdValid(destContainer))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id building = getTopMostContainer(destContainer);
        if (isIdValid(building) && isBuildingObject(building))
        {
            int cityId = getIntObjVar(self, "city.expulsion.city_id");
            int buildingCityId = city.getCityAtLocation(getLocation(building), 0);

            if (cityId == buildingCityId)
            {
                sendSystemMessage(self, city.SID_CANNOT_ENTER_BUILDING_TEF);
                return SCRIPT_OVERRIDE;
            }
        }

        return SCRIPT_CONTINUE;
    }

    public static boolean isBuildingObject(obj_id obj) throws InterruptedException
    {
        String template = getTemplateName(obj);
        return template != null && template.contains("building");
    }
}


