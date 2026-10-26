package script.space.vessel;

import script.dictionary;
import script.obj_id;

/**
 * On a vessel's ramp and cabin cells (attached by vessel_controller): tells the vessel when a player walks in, so
 * it can check their ticket at the top of the ramp and greet them in the cabin, and gives the player
 * space.vessel.vessel_aboard (back aboard after a logout aboard).
 */
public class vessel_cell extends script.base_script
{
    public vessel_cell()
    {
    }

    public int OnReceivedItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (!isPlayer(item))
            return SCRIPT_CONTINUE;
        obj_id ship = getContainedBy(self);
        if (!isIdValid(ship))
            return SCRIPT_CONTINUE;
        if (!hasScript(item, "space.vessel.vessel_aboard"))
            attachScript(item, "space.vessel.vessel_aboard");          // logging out aboard comes back aboard
        dictionary d = new dictionary();
        d.put("player", item);
        d.put("cell", self);
        d.put("from", srcContainer);
        messageTo(ship, "vesselCellEntered", d, 0.0f, false);
        return SCRIPT_CONTINUE;
    }
}
