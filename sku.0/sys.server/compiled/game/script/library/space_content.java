package script.library;

import script.*;

public class space_content extends script.base_script
{
    public space_content()
    {
    }
    public static final String LAUNCH_LOCATION_DATATABLE_NAME = "datatables/space_zones/launch_locations.iff";
    public static final String LAUNCH_LOCATION_COLUMN_POINTNAME = "pointName";
    public static final String LAUNCH_LOCATION_COLUMN_GROUND_SCENE = "groundScene";
    public static final String LAUNCH_LOCATION_COLUMN_GROUND_X = "groundX";
    public static final String LAUNCH_LOCATION_COLUMN_GROUND_Y = "groundY";
    public static final String LAUNCH_LOCATION_COLUMN_GROUND_Z = "groundZ";
    public static final string_id SID_BAD_TRAVEL_POINT = new string_id("space/space_interaction", "bad_travel_point");
    public static final string_id SID_NO_HOMING_BEACON = new string_id("space/space_interaction", "no_homing_beacon");
    public static void landPlayer(obj_id objPlayer, obj_id objStation, String strDestination) throws InterruptedException
    {
        float fltDistance = getDistance(objStation, space_transition.getContainingShip(objPlayer));
        if (fltDistance > space_transition.STATION_COMM_MAX_DISTANCE)
        {
            string_id strSpam = new string_id("space/space_interaction", "too_far");
            sendSystemMessage(objPlayer, strSpam);
            return;
        }
        int row = dataTableSearchColumnForString(strDestination, LAUNCH_LOCATION_COLUMN_POINTNAME, LAUNCH_LOCATION_DATATABLE_NAME);
        if (row < 0)
        {
            prose_package ppBadTravelPoint = prose.getPackage(SID_BAD_TRAVEL_POINT);
            prose.setTO(ppBadTravelPoint, strDestination);
            sendSystemMessageProse(objPlayer, ppBadTravelPoint);
            return;
        }
        else 
        {
            obj_id objShip = space_transition.getContainingShip(objPlayer);
            if (isIdValid(objShip))
            {
                space_combat.clearHyperspace(objShip);
                space_transition.clearOvertStatus(objShip);
                space_transition.packShip(objShip);
            }
            String groundScene = dataTableGetString(LAUNCH_LOCATION_DATATABLE_NAME, row, LAUNCH_LOCATION_COLUMN_GROUND_SCENE);
            float groundX = dataTableGetFloat(LAUNCH_LOCATION_DATATABLE_NAME, row, LAUNCH_LOCATION_COLUMN_GROUND_X);
            float groundY = dataTableGetFloat(LAUNCH_LOCATION_DATATABLE_NAME, row, LAUNCH_LOCATION_COLUMN_GROUND_Y);
            float groundZ = dataTableGetFloat(LAUNCH_LOCATION_DATATABLE_NAME, row, LAUNCH_LOCATION_COLUMN_GROUND_Z);
            float theta = rand() * (2.0f * (float)Math.PI);
            float radius = 2.0f + rand() * 3.0f;
            groundX += radius * StrictMath.cos(theta);
            groundZ += radius * StrictMath.sin(theta);
            warpPlayer(objPlayer, groundScene, groundX, groundY, groundZ, null, groundX, groundY, groundZ);
        }
    }
    public static void descendToPlanet(obj_id objPlayer, obj_id objStation) throws InterruptedException
    {
        obj_id objShip = space_transition.getContainingShip(objPlayer);
        if (!isIdValid(objShip))
        {
            sendSystemMessage(objPlayer, new string_id("space/space_interaction", "land_ship_failed"));
            return;
        }
        float fltDistance = getDistance(objStation, objShip);
        if (fltDistance > space_transition.STATION_COMM_MAX_DISTANCE)
        {
            string_id strSpam = new string_id("space/space_interaction", "too_far");
            sendSystemMessage(objPlayer, strSpam);
            return;
        }
        String spaceScene = getCurrentSceneName();
        String groundScene = getGroundSceneForSpaceScene(spaceScene);
        if (groundScene == null || groundScene.isEmpty())
        {
            sendSystemMessage(objPlayer, new string_id("space/space_interaction", "no_planet_below"));
            return;
        }
        space_combat.clearHyperspace(objShip);
        space_transition.clearOvertStatus(objShip);
        warpPlayer(objShip, groundScene, 0.0f, 2000.0f, 0.0f, null, 0.0f, 2000.0f, 0.0f);
    }
    public static String getGroundSceneForSpaceScene(String spaceScene) throws InterruptedException
    {
        int numRows = dataTableGetNumRows(LAUNCH_LOCATION_DATATABLE_NAME);
        for (int i = 0; i < numRows; ++i)
        {
            String scene = dataTableGetString(LAUNCH_LOCATION_DATATABLE_NAME, i, "spaceScene");
            if (scene != null && scene.equals(spaceScene))
            {
                return dataTableGetString(LAUNCH_LOCATION_DATATABLE_NAME, i, LAUNCH_LOCATION_COLUMN_GROUND_SCENE);
            }
        }
        return null;
    }
    public static String getPlanetForLaunchLocation(String pointName) throws InterruptedException
    {
        int row = dataTableSearchColumnForString(pointName, LAUNCH_LOCATION_COLUMN_POINTNAME, LAUNCH_LOCATION_DATATABLE_NAME);
        String ret = null;
        if (row != -1)
        {
            ret = dataTableGetString(LAUNCH_LOCATION_DATATABLE_NAME, row, LAUNCH_LOCATION_COLUMN_GROUND_SCENE);
        }
        return ret;
    }
    public static void notifySpawner(obj_id objObject) throws InterruptedException
    {
        if (!isIdValid(objObject))
        {
            return;
        }
        if (hasObjVar(objObject, "intNotified"))
        {
            return;
        }
        if (hasScript(objObject, "space.content_tools.squad_member"))
        {
            return;
        }
        obj_id objParent = getObjIdObjVar(objObject, "objParent");
        if (isIdValid(objParent))
        {
            setObjVar(objObject, "intNotified", 1);
            space_utils.notifyObject(objParent, "childDestroyed", null);
        }
    }
    public static void landPlayerHoming(obj_id objPlayer, obj_id objStation) throws InterruptedException
    {
        float fltDistance = getDistance(objStation, space_transition.getContainingShip(objPlayer));
        if (fltDistance > space_transition.STATION_COMM_MAX_DISTANCE)
        {
            string_id strSpam = new string_id("space/space_interaction", "too_far");
            sendSystemMessage(objPlayer, strSpam);
            return;
        }
        if (!hasObjVar(objPlayer, "homingBeacon.planet") && !hasObjVar(objPlayer, "homingBeacon.houseLoc"))
        {
            sendSystemMessage(objPlayer, SID_NO_HOMING_BEACON);
            return;
        }
        else 
        {
            obj_id objShip = space_transition.getContainingShip(objPlayer);
            if (isIdValid(objShip))
            {
                space_combat.clearHyperspace(objShip);
                space_transition.clearOvertStatus(objShip);
                space_transition.packShip(objShip);
            }
            String groundScene = getStringObjVar(objPlayer, "homingBeacon.planet");
            location houseLoc = getLocationObjVar(objPlayer, "homingBeacon.houseLoc");
            location landingSpot = locations.getGoodLocationAroundLocation(houseLoc, 30.0f, 30.0f, 30.0f, 30.0f);
            // Note: The Y location cannot be accurately calculated due to issue with the server code (see
            landingSpot.y = houseLoc.y - 5;
            obj_id house = getObjIdObjVar(objPlayer, "homingBeacon.houseId");
            utils.warpPlayer(objPlayer, groundScene, landingSpot);
            dictionary params = new dictionary();
            params.put("landingSpot", landingSpot);
            params.put("groundScene", groundScene);
            params.put("player", objPlayer);
            params.put("house", house);
            messageTo(house, "houseCheck", params, 40, false);
        }
    }
}
