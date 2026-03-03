package script.space.ship;

import script.*;
import script.library.space_transition;
import script.library.space_utils;

public class ship_atmospheric_boarding extends script.base_script
{
    public ship_atmospheric_boarding()
    {
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        obj_id containingShip = space_transition.getContainingShip(player);
        if (isIdValid(containingShip))
            return SCRIPT_CONTINUE;

        if (!space_utils.isShipWithInterior(self))
            return SCRIPT_CONTINUE;

        if (!space_transition.isShipParkedInWorld(self))
            return SCRIPT_CONTINUE;

        float dist = getDistance(player, self);
        if (dist > 500.0f)
            return SCRIPT_CONTINUE;

        mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Board Ship"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.SERVER_MENU1)
            return SCRIPT_CONTINUE;

        if (!space_transition.isAtmosphericFlightScene())
        {
            sendSystemMessage(player, new string_id("space/space_interaction", "no_atmospheric_flight"));
            return SCRIPT_CONTINUE;
        }

        obj_id containingShip = space_transition.getContainingShip(player);
        if (isIdValid(containingShip))
        {
            sendSystemMessage(player, new string_id("space/space_interaction", "already_in_ship"));
            return SCRIPT_CONTINUE;
        }

        if (!space_transition.isShipParkedInWorld(self))
        {
            sendSystemMessage(player, new string_id("space/space_interaction", "ship_in_flight"));
            return SCRIPT_CONTINUE;
        }

        float dist = getDistance(player, self);
        if (dist > 500.0f)
        {
            sendSystemMessage(player, new string_id("space/space_interaction", "too_far_to_board"));
            return SCRIPT_CONTINUE;
        }

        if (!space_transition.boardShipFromGround(player, self))
            return SCRIPT_CONTINUE;

        return SCRIPT_CONTINUE;
    }
}
