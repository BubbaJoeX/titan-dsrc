package script.space.vessel;

import script.*;
import script.library.*;

/**
 * The helm console of a private Titan vessel (interior buildout row, ship_interiors/<vessel>.tab): from inside the
 * hull cannot be targeted, so the console shows its owner the vessel's own Flight / Paint menu
 * (vessel.addFlightMenu) and hands the pick to the vessel (vessel_controller.vesselHelm).
 */
public class vessel_helm extends script.base_script
{
    public vessel_helm()
    {
    }

    private obj_id getShip(obj_id self) throws InterruptedException
    {
        obj_id top = getTopMostContainer(self);
        return isIdValid(top) && !top.equals(self) && vessel.isVessel(top) ? top : null;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id ship = getShip(self);
        if (ship != null)
            vessel.addFlightMenu(ship, player, mi);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id ship = getShip(self);
        if (ship == null || !vessel.canFly(ship, player))
            return SCRIPT_CONTINUE;
        dictionary d = new dictionary();
        d.put("player", player);
        d.put("item", item);
        messageTo(ship, "vesselHelm", d, 0.0f, false);
        return SCRIPT_CONTINUE;
    }
}
