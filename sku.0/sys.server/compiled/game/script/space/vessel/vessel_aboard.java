package script.space.vessel;

import script.*;
import script.library.*;

/**
 * On a player who has boarded a private Titan vessel (vessel_cell attaches it at the ramp): logging out aboard and
 * back in puts them back in the cell they left. The game server would otherwise eject them to the ground under the
 * vessel's frame (its login containment restore does not take POB ship cells on a ground scene).
 */
public class vessel_aboard extends script.base_script
{
    public vessel_aboard()
    {
    }

    public static final String OV_RELOG = "vessel.relog";
    public static final String OV_SHIP = OV_RELOG + ".ship";
    public static final String OV_CELL = OV_RELOG + ".cell";
    public static final String OV_POS = OV_RELOG + ".pos";

    public int OnLogout(obj_id self) throws InterruptedException
    {
        removeObjVar(self, OV_RELOG);
        obj_id ship = space_transition.getContainingShip(self);
        location here = getLocation(self);
        if (!vessel.isVessel(ship) || hasObjVar(ship, vessel.OV_SERVICE) || here == null || !isIdValid(here.cell))
            return SCRIPT_CONTINUE;
        setObjVar(self, OV_SHIP, ship);
        setObjVar(self, OV_CELL, here.cell);
        setObjVar(self, OV_POS, new float[]{here.x, here.y, here.z});
        return SCRIPT_CONTINUE;
    }

    public int OnLogin(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, OV_RELOG))
            messageTo(self, "vesselRelog", null, 2.0f, false);     // once the vessel round the player is loaded
        return SCRIPT_CONTINUE;
    }

    public int vesselRelog(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_RELOG))
            return SCRIPT_CONTINUE;
        obj_id ship = getObjIdObjVar(self, OV_SHIP);
        obj_id cell = getObjIdObjVar(self, OV_CELL);
        float[] pos = getFloatArrayObjVar(self, OV_POS);
        removeObjVar(self, OV_RELOG);
        if (!vessel.isVessel(ship) || !isIdValid(cell) || !exists(cell) || !ship.equals(getContainedBy(cell)) || pos == null
                || ship.equals(space_transition.getContainingShip(self)))
            return SCRIPT_CONTINUE;
        setLocation(self, new location(pos[0], pos[1], pos[2], getLocation(ship).area, cell));
        return SCRIPT_CONTINUE;
    }
}
