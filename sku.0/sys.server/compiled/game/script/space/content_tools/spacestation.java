package script.space.content_tools;

import script.*;
import script.library.*;

import java.util.Vector;

public class spacestation extends script.base_script
{
    public spacestation()
    {
    }
    public static final string_id SID_DESCEND = string_id.unlocalized("Descend to Planet");
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id containingShip = space_transition.getContainingShip(player);
        if (isIdValid(containingShip))
        {
            float dist = getDistance(self, containingShip);
            if (dist <= space_transition.STATION_COMM_MAX_DISTANCE)
            {
                String groundScene = space_content.getGroundSceneForSpaceScene(getCurrentSceneName());
                if (groundScene != null && !groundScene.isEmpty())
                {
                    mi.addRootMenu(menu_info_types.SERVER_MENU9, SID_DESCEND);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU9)
        {
            space_content.descendToPlanet(player, self);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        requestPreloadCompleteTrigger(self);
        setObjVar(self, "intInvincible", 1);
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        requestPreloadCompleteTrigger(self);
        return SCRIPT_CONTINUE;
    }
    public int OnPreloadComplete(obj_id self) throws InterruptedException
    {
        messageTo(self, "registerStation", null, 2, false);
        String strName = getStringObjVar(self, "strName");
        if (strName != null)
        {
            string_id strSpam = new string_id("space/space_mobile_type", strName);
            setName(self, strSpam);
        }
        return SCRIPT_CONTINUE;
    }
    public int registerStation(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("space", "Registering space station");
        obj_id objQuestManager = getNamedObject(space_quest.QUEST_MANAGER);
        if (!isIdValid(objQuestManager))
        {
            LOG("space", "NO QUEST MANAGER OBJECT FOUND!");
            return SCRIPT_CONTINUE;
        }
        registerStationWithManager(objQuestManager, self);
        return SCRIPT_CONTINUE;
    }
    public void registerStationWithManager(obj_id objManager, obj_id objStation) throws InterruptedException
    {
        LOG("space", "Registering with " + objManager);
        Vector objSpaceStations = utils.getResizeableObjIdArrayScriptVar(objManager, "objSpaceStations");
        if ((objSpaceStations == null) || (objSpaceStations.size() == 0))
        {
            objSpaceStations = utils.addElement(objSpaceStations, objStation);
        }
        else 
        {
            int intIndex = utils.getElementPositionInArray(objSpaceStations, objStation);
            if (intIndex < 0)
            {
                objSpaceStations = utils.addElement(objSpaceStations, objStation);
            }
        }
        utils.setScriptVar(objManager, "objSpaceStations", objSpaceStations);
    }
}
