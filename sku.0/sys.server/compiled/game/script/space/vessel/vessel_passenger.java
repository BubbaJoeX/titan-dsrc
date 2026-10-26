package script.space.vessel;

import script.*;
import script.library.*;

/**
 * On a player while their shuttle is between planets (attached by shuttle_service at the jump point). The jump is the
 * zone change: the player arrives on the destination planet's server in hyperspace, where this puts them aboard the
 * flight's transit vessel (the first passenger to arrive brings it in) - the same transport, cabin and seats - which
 * drops out of hyperspace at the ETA and lands at the destination port. Logging in mid-voyage without a vessel
 * around finishes the trip the stock way.
 */
public class vessel_passenger extends script.base_script
{
    public vessel_passenger()
    {
    }

    public int vesselVoyageArrived(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, vessel.OV_VOYAGE_FLIGHT))
        {
            detachScript(self, vessel.SCRIPT_PASSENGER);
            return SCRIPT_CONTINUE;
        }
        String planet = getStringObjVar(self, vessel.OV_VOYAGE_PLANET);
        if (!planet.equals(getCurrentSceneName()))
            return SCRIPT_CONTINUE;
        boardTransit(self);
        return SCRIPT_CONTINUE;
    }

    public int vesselVoyageBoard(obj_id self, dictionary params) throws InterruptedException
    {
        boardTransit(self);
        return SCRIPT_CONTINUE;
    }

    private void boardTransit(obj_id self) throws InterruptedException
    {
        String flight = getStringObjVar(self, vessel.OV_VOYAGE_FLIGHT);
        String planet = getStringObjVar(self, vessel.OV_VOYAGE_PLANET);
        String point = getStringObjVar(self, vessel.OV_VOYAGE_POINT);
        int eta = getIntObjVar(self, vessel.OV_VOYAGE_ETA);
        obj_id ship = findTransit(self, flight);
        if (!isIdValid(ship))
            ship = createTransit(self, flight, planet, point, eta);
        if (!isIdValid(ship))
        {
            finishTheStockWay(self);
            return;
        }
        if (space_transition.getShipBoardingDestination(ship) == null)
        {
            // the interior buildout (and its boarding spot) comes a moment after the vessel; try again shortly
            int tries = utils.hasScriptVar(self, "vessel.board_tries") ? utils.getIntScriptVar(self, "vessel.board_tries") : 0;
            if (tries < 10)
            {
                utils.setScriptVar(self, "vessel.board_tries", tries + 1);
                messageTo(self, "vesselVoyageBoard", null, 0.5f, false);
                return;
            }
        }
        utils.removeScriptVar(self, "vessel.board_tries");
        vessel.addPassenger(ship, self, planet, point, obj_id.NULL_ID);
        vessel.moveIntoCabin(ship, self);
        int left = Math.max(0, eta - getGameTime());
        sendSystemMessage(self, string_id.unlocalized("[Flight " + flight + "] In hyperspace for " + vessel.planetDisplayName(planet)
                + ". Arrival in about " + Math.max(1, Math.round(left / 60.0f)) + (left < 90 ? " minute." : " minutes.")
                + " Feel free to move about the cabin."));
    }

    private obj_id findTransit(obj_id self, String flight) throws InterruptedException
    {
        obj_id[] near = getObjectsInRange(getWorldLocation(self), 400.0f);
        if (near != null)
            for (obj_id o : near)
                if (vessel.isVessel(o) && hasObjVar(o, vessel.OV_FLIGHT) && flight.equals(getStringObjVar(o, vessel.OV_FLIGHT))
                        && vessel.getState(o) == vessel.STATE_HYPERSPACE)
                    return o;
        return null;
    }

    /** The flight's vessel, holding still in hyperspace where the passengers arrive, facing the destination port. */
    private obj_id createTransit(obj_id self, String flight, String planet, String point, int eta) throws InterruptedException
    {
        String template = getStringObjVar(self, vessel.OV_VOYAGE_TEMPLATE);
        location here = getWorldLocation(self);
        obj_id service = vessel.findService(planet, point);
        float heading = 0.0f;
        if (isIdValid(service))
        {
            location pad = getLocation(service);
            heading = (float)Math.toDegrees(Math.atan2(pad.x - here.x, pad.z - here.z));
        }
        location loc = new location(here.x, here.y, here.z, here.area, null);
        obj_id ship = vessel.takeSpare(service, template, here.x, here.y, here.z, heading, 0.0f);
        if (!isIdValid(ship))
            ship = createObject(template, loc);
        if (!isIdValid(ship))
            return null;
        vessel.Path hold = new vessel.Path().key(0.0f, here.x, here.y, here.z, heading, 0.0f, 0.0f);
        vessel.fly(ship, hold, null);
        setObjVar(ship, vessel.OV_FLIGHT, flight);
        vessel.initParts(ship, false);
        dictionary d = new dictionary();
        d.put("planet", planet);
        d.put("point", point);
        d.put("eta", eta);
        d.put("route", vessel.hyperlaneName(getStringObjVar(self, vessel.OV_VOYAGE_FROM), planet));
        messageTo(ship, "vesselEnterHyperspace", d, 0.0f, false);
        LOG("vessel", "vessel_passenger: transit vessel " + ship + " for flight " + flight + " to " + planet + "/" + point + ", eta " + eta);
        return ship;
    }

    private void finishTheStockWay(obj_id self) throws InterruptedException
    {
        String planet = getStringObjVar(self, vessel.OV_VOYAGE_PLANET);
        String point = getStringObjVar(self, vessel.OV_VOYAGE_POINT);
        removeObjVar(self, vessel.OV_VOYAGE);
        travel.movePlayerToDestination(self, planet, point);
        detachScript(self, vessel.SCRIPT_PASSENGER);
    }

    public int OnLogin(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, vessel.OV_VOYAGE_FLIGHT))
        {
            detachScript(self, vessel.SCRIPT_PASSENGER);
            return SCRIPT_CONTINUE;
        }
        obj_id ship = space_transition.getContainingShip(self);
        if (vessel.isVessel(ship))
            return SCRIPT_CONTINUE;
        // the voyage's vessel is gone (server restart, logged out mid-flight): arrive the stock way
        messageTo(self, "vesselVoyageRecover", null, 5.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int vesselVoyageRecover(obj_id self, dictionary params) throws InterruptedException
    {
        if (hasObjVar(self, vessel.OV_VOYAGE_FLIGHT) && !vessel.isVessel(space_transition.getContainingShip(self)))
            finishTheStockWay(self);
        return SCRIPT_CONTINUE;
    }
}
