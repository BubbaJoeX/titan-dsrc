package script.space.crafting;

import script.*;
import script.library.space_transition;
import script.library.utils;

public class escape_hatch extends script.base_script
{
    public escape_hatch()
    {
    }
    public int OnAttach(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("space/space_interaction", "eject"));

        obj_id objShip = space_transition.getContainingShip(player);
        if (isIdValid(objShip) && space_transition.isAtmosphericFlightScene())
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Depart through Boarding Ramp"));
        }
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id objPlayer, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1)
        {
            obj_id objShip = space_transition.getContainingShip(objPlayer);
            if (!isIdValid(objShip))
                return SCRIPT_CONTINUE;

            if (!space_transition.isAtmosphericFlightScene())
            {
                sendSystemMessage(objPlayer, new string_id("space/space_interaction", "no_atmospheric_flight"));
                return SCRIPT_CONTINUE;
            }

            obj_id pilot = getPilotId(objShip);
            if (isIdValid(pilot))
            {
                sendSystemMessage(objPlayer, new string_id("space/space_interaction", "ship_in_flight"));
                return SCRIPT_CONTINUE;
            }

            sendSystemMessage(objPlayer, new string_id("space/space_interaction", "disembarking"));
            space_transition.disembarkShip(objPlayer, objShip);
            return SCRIPT_CONTINUE;
        }

        if (item == menu_info_types.ITEM_USE)
        {
            obj_id objShip = space_transition.getContainingShip(objPlayer);
            obj_id objOwner = getOwner(objShip);
            if (objOwner == objPlayer)
            {
                string_id strSpam = new string_id("space/space_interaction", "ejecting");
                sendSystemMessage(objPlayer, strSpam);
                utils.setLocalVar(objShip, "intEjecting", 1);
                dictionary dctParams = new dictionary();
                dctParams.put("objShip", objShip);
                messageTo(objShip, "megaDamage", dctParams, 2, false);
                return SCRIPT_CONTINUE;
            }
            else
            {
                string_id strSpam = new string_id("space/space_interaction", "ejecting");
                sendSystemMessage(objPlayer, strSpam);
                space_transition.teleportPlayerToLaunchLoc(objPlayer);
            }
        }
        return SCRIPT_CONTINUE;
    }
}
