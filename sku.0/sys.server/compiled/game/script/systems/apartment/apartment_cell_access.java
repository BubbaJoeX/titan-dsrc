package script.systems.apartment;

import script.*;

public class apartment_cell_access extends script.base_script
{
    public apartment_cell_access()
    {
    }

    public int OnAboutToReceiveItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (!isIdValid(item) || !isPlayer(item))
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, apartment_lib.OV_CELL_BUILDING) || !hasObjVar(self, apartment_lib.OV_CELL_NAME))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id building = getObjIdObjVar(self, apartment_lib.OV_CELL_BUILDING);
        String cellName = getStringObjVar(self, apartment_lib.OV_CELL_NAME);
        if (!isIdValid(building) || cellName == null || cellName.length() < 1)
        {
            return SCRIPT_CONTINUE;
        }
        if (!apartment_lib.isApartmentBuilding(building))
        {
            return SCRIPT_CONTINUE;
        }

        if (!apartment_lib.isPlayerAuthorizedForUnit(building, cellName, item))
        {
            sendSystemMessageTestingOnly(item, "[Apartment] You do not have access to this room.");
            return SCRIPT_OVERRIDE;
        }

        return SCRIPT_CONTINUE;
    }
}
