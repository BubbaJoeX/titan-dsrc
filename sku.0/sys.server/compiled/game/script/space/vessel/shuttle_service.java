package script.space.vessel;

import script.*;
import script.library.*;

import java.util.Vector;

/**
 * A starport's shuttle service: the pad where its Titan vessel (a boardable POB transport) lands, boards and leaves.
 * Created by travel.initializeStarport in place of the stock creature transport for the structures listed in
 * datatables/space/titan_vessel_services.tab; it is the starport's "shuttle" for the ticket collector and the
 * boardShuttle command, and books passengers onto the vessel instead of warping them.
 *
 * The cycle, timed like the stock transport (GROUND_TIME / AIR_TIME of the starport table):
 *   arrival     an empty vessel (or one carrying passengers here) flies its approach and touches down
 *   boarding    ramp down; ticket holders walk up the ramp and take a seat; arrivals walk off
 *   departure   ramp up, lift-off and climb-out; at the jump point passengers bound off-planet go to hyperspace
 *               (their vessel on the destination server), the others cruise to their port on this planet
 *   away        the pad is free; the next vessel arrives after AIR_TIME, or sooner when one is holding for it
 */
public class shuttle_service extends script.base_script
{
    public shuttle_service()
    {
    }

    public static final String OV_POINT = "vessel.service.point";
    public static final String OV_TEMPLATE = "vessel.service.template";
    public static final String OV_STATE = "vessel.service.state";
    public static final String OV_CURRENT = "vessel.service.current";
    public static final String OV_DEPARTS = "vessel.service.departs";
    public static final String SV_QUEUE = "vessel.service.queue";

    public static final int AWAY = 0;
    public static final int INBOUND = 1;
    public static final int LANDED = 2;
    public static final int DEPARTING = 3;

    public static final float JUMP_POINT = 44.0f;          // seconds into the departure path (~700 m up, 1.75 km out)
    public static final float PAD_CLEAR = 16.0f;           // the pad is free this long after lift-off

    // a pad in a hangar bay off a landing well (titan_atmo_starport): "vessel.service.well" = {x, z, rim y} of the
    // shaft in world space (travel.initializeStarport, from titan_vessel_services.tab). The vessel flies the well's
    // paths (vessel.wellInPath / wellOutPath); the pad controller stands on the bay floor at the berth.
    public static final String OV_WELL = "vessel.service.well";
    private static final float WELL_TIME = vessel.WELL_TIME;
    private static final float WELL_SINK = vessel.WELL_SINK;
    private static final float WELL_BACK = vessel.WELL_BACK;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "serviceStart", null, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "serviceStart", null, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self) throws InterruptedException
    {
        obj_id ship = current(self);
        if (vessel.isVessel(ship) && vessel.getPlayersAboard(ship).length == 0)
            messageTo(ship, "vesselRetire", null, 0.0f, false);
        if (hasObjVar(self, vessel.OV_SPARES))
            for (obj_id spare : getObjIdArrayObjVar(self, vessel.OV_SPARES))
                if (vessel.isVessel(spare))
                    messageTo(spare, "vesselRetireFinal", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------

    private obj_id current(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, OV_CURRENT) ? getObjIdObjVar(self, OV_CURRENT) : null;
    }

    private int state(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, OV_STATE) ? getIntObjVar(self, OV_STATE) : AWAY;
    }

    private obj_id starport(obj_id self) throws InterruptedException
    {
        return travel.getStarportFromTerminal(self);
    }

    /** Parked pose of the service's vessel type on this pad. */
    private float[] padPose(obj_id self, vessel.Config c) throws InterruptedException
    {
        return vessel.landedPose(c, padGround(self), getYaw(self));
    }

    private static boolean hasWell(obj_id service) throws InterruptedException
    {
        return hasObjVar(service, OV_WELL);
    }

    /** Where a service's arrivals fly their approach to: the pad itself, or the hover over its landing well. */
    public static float[] approachTarget(obj_id service, vessel.Config c) throws InterruptedException
    {
        if (!hasWell(service))
            return vessel.landedPose(c, padGround(service), getYaw(service));
        float[] w = getFloatArrayObjVar(service, OV_WELL);
        return new float[]{ w[0], w[2] + vessel.WELL_HOVER + c.landY, w[1], getYaw(service), 0.0f };
    }

    private vessel.Config config(obj_id self) throws InterruptedException
    {
        return vessel.getConfig(getStringObjVar(self, OV_TEMPLATE));
    }

    private void setAvailable(obj_id self, boolean available) throws InterruptedException
    {
        obj_id sp = starport(self);
        if (!isIdValid(sp))
            return;
        setObjVar(sp, travel.VAR_SHUTTLE_AVAILABLE, available ? 1 : 0);
        setObjVar(sp, travel.VAR_SHUTTLE_TIMESTAMP, getGameTime());
    }

    private void tellNearby(obj_id self, String text, float range) throws InterruptedException
    {
        obj_id[] near = getObjectsInRange(getLocation(self), range);
        if (near != null)
            for (obj_id p : near)
                if (isPlayer(p))
                    sendSystemMessage(p, string_id.unlocalized(text));
        obj_id ship = current(self);
        if (vessel.isVessel(ship))
            for (obj_id p : vessel.getPlayersAboard(ship))
                if (near == null || !java.util.Arrays.asList(near).contains(p))
                    sendSystemMessage(p, string_id.unlocalized(text));
    }

    /** A vessel of this service's type, held still at `pose` ({x, y, z, heading, pitch}): a stowed one if it has one. */
    private obj_id spawnVessel(obj_id self, float[] pose) throws InterruptedException
    {
        String template = getStringObjVar(self, OV_TEMPLATE);
        obj_id spare = vessel.takeSpare(self, template, pose[0], pose[1], pose[2], pose[3], pose[4]);
        if (isIdValid(spare))
        {
            setObjVar(spare, vessel.OV_SERVICE, self);
            vessel.flightNumber(spare);
            return spare;
        }
        location loc = new location(pose[0], pose[1], pose[2], getLocation(self).area, null);
        obj_id ship = createObject(template, loc);
        if (!isIdValid(ship))
        {
            LOG("vessel", "shuttle_service " + self + ": could not create " + template);
            return null;
        }
        setObjVar(ship, vessel.OV_SERVICE, self);
        vessel.Path hold = new vessel.Path().key(0.0f, pose[0], pose[1], pose[2], pose[3], pose[4], 0.0f);
        vessel.fly(ship, hold, null);
        vessel.flightNumber(ship);
        return ship;
    }

    // ------------------------------------------------------------------
    // start: a vessel already on the pad
    // ------------------------------------------------------------------

    public int serviceStart(obj_id self, dictionary params) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "vessel.service.started"))
            return SCRIPT_CONTINUE;
        vessel.Config c = config(self);
        if (c == null)
        {
            LOG("vessel", "shuttle_service " + self + ": no vessel data for " + getStringObjVar(self, OV_TEMPLATE));
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(self, "vessel.service.started", 1);
        // the vessel it had before (a script reload or restart): reuse it when it's empty, else set its people down
        obj_id old = current(self);
        if (vessel.isVessel(old))
        {
            if (vessel.getPlayersAboard(old).length == 0)
            {
                vessel.clearPassengers(old);
                vessel.addSpare(self, old);
            }
            else
                messageTo(old, "vesselRetire", null, 0.0f, false);
        }
        float[] pose = padPose(self, c);
        obj_id ship = spawnVessel(self, pose);
        if (!isIdValid(ship))
            return SCRIPT_CONTINUE;
        setObjVar(self, OV_CURRENT, ship);
        vessel.initParts(ship, true);
        // starports booted together would otherwise run their departures in lockstep
        beginBoarding(self, ship, rand(0, 150));
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // boarding
    // ------------------------------------------------------------------

    private void beginBoarding(obj_id self, obj_id ship) throws InterruptedException
    {
        beginBoarding(self, ship, 0);
    }

    private void beginBoarding(obj_id self, obj_id ship, int extraGroundTime) throws InterruptedException
    {
        setObjVar(self, OV_STATE, LANDED);
        vessel.setState(ship, vessel.STATE_BOARDING);
        vessel.setRamp(ship, true, 0.5f);
        obj_id sp = starport(self);
        int ground = (isIdValid(sp) ? travel.getGroundTime(sp) : 300) + extraGroundTime;
        int departs = getGameTime() + ground;
        setObjVar(self, OV_DEPARTS, departs);
        setAvailable(self, true);
        String point = getStringObjVar(self, OV_POINT);
        tellNearby(self, "Flight " + vessel.flightNumber(ship) + " is boarding at " + point + ". Ticket holders may board by the ramp.", 64.0f);
        dictionary d = new dictionary();
        d.put("vessel", ship);
        if (ground > 70)
            messageTo(self, "serviceBoardingCall", d, ground - 60, false);
        messageTo(self, "serviceFinalCall", d, Math.max(1, ground - 20), false);
        messageTo(self, "serviceDepart", d, ground, false);
    }

    public int serviceBoardingCall(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (ship == current(self) && state(self) == LANDED)
            tellNearby(self, "Flight " + vessel.flightNumber(ship) + " departs in one minute. All passengers please board.", 64.0f);
        return SCRIPT_CONTINUE;
    }

    public int serviceFinalCall(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (ship == current(self) && state(self) == LANDED)
            tellNearby(self, "Final call for flight " + vessel.flightNumber(ship) + ". The ramp closes in 20 seconds.", 64.0f);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // departure
    // ------------------------------------------------------------------

    public int serviceDepart(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (ship != current(self) || state(self) != LANDED || !vessel.isVessel(ship))
            return SCRIPT_CONTINUE;
        setObjVar(self, OV_STATE, DEPARTING);
        setAvailable(self, false);
        vessel.setState(ship, vessel.STATE_DEPARTING);
        String flight = vessel.flightNumber(ship);
        // passengers not aboard missed it (their ticket stays good); anyone aboard without a booking is set down
        obj_id[] booked = vessel.getPassengerIds(ship);
        for (obj_id p : booked)
            if (!vessel.isAboard(ship, p))
            {
                vessel.removePassenger(ship, p);
                if (isIdValid(p) && exists(p))
                    sendSystemMessage(p, string_id.unlocalized("Flight " + flight + " has left without you. Your ticket is still valid for the next departure."));
            }
        for (obj_id p : vessel.getPlayersAboard(ship))
        {
            if (!vessel.isPassenger(ship, p) && isGod(p))
                continue;                                   // staff ride along without a ticket
            if (!vessel.isPassenger(ship, p))
            {
                vessel.moveOffShip(ship, p);
                sendSystemMessage(p, string_id.unlocalized("Flight " + flight + " is departing: the crew show you off the ship."));
                continue;
            }
            // the ticket is taken now, as the stock boarding command does
            obj_id ticket = getObjIdArrayObjVar(ship, vessel.OV_PAX_TICKETS)[vessel.getPassengerIndex(ship, p)];
            if (!isIdValid(ticket) || !exists(ticket) || utils.getContainingPlayer(ticket) != p)
            {
                vessel.removePassenger(ship, p);
                vessel.moveOffShip(ship, p);
                sendSystemMessage(p, string_id.unlocalized("You no longer hold a ticket for flight " + flight + "."));
                continue;
            }
            destroyObject(ticket);
        }
        vessel.setRamp(ship, false, 0.0f);
        int count = vessel.getPassengerIds(ship).length;
        vessel.announce(ship, "[Flight " + flight + "] Doors closing. Please remain seated; we lift off shortly.");
        playClientEffectObj(ship, "clienteffect/transport_enginestart.cef", ship, "hp_engine_1");
        LOG("vessel", "shuttle_service " + self + ": flight " + flight + " departing " + getStringObjVar(self, OV_POINT) + " with " + count + " passengers");
        dictionary d = new dictionary();
        d.put("vessel", ship);
        messageTo(self, "serviceLiftoff", d, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int serviceLiftoff(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (!vessel.isVessel(ship))
            return SCRIPT_CONTINUE;
        vessel.Config c = config(self);
        float[] pose = padPose(self, c);
        vessel.Path path;
        float well = 0.0f;                                  // time spent getting out of the well first
        if (hasWell(self))
        {
            float[] hover = approachTarget(self, c);
            path = vessel.wellOutPath(pose, hover, 0.0f);
            well = path.duration();
            path.append(vessel.departurePath(hover, 0.0f), well);
            vessel.setGear(ship, false, well - 2.0f);       // legs up at the top of the shaft
        }
        else
        {
            path = vessel.departurePath(pose, 0.0f);
            vessel.setGear(ship, false, 1.0f);
        }
        vessel.fly(ship, path, "svc:serviceDepartureEnd");
        playClientEffectObj(ship, "clienteffect/transport_takeoff.cef", ship, "hp_engine_1");
        dictionary d = new dictionary();
        d.put("vessel", ship);
        messageTo(self, "serviceJumpPoint", d, well + JUMP_POINT, false);
        // a bay is clear once the vessel is out over the shaft
        messageTo(self, "servicePadClear", d, well > 0.0f ? vessel.WELL_LIFT + WELL_BACK : PAD_CLEAR, false);
        return SCRIPT_CONTINUE;
    }

    public int servicePadClear(obj_id self, dictionary params) throws InterruptedException
    {
        if (state(self) == DEPARTING)
        {
            setObjVar(self, OV_STATE, AWAY);
            removeObjVar(self, OV_CURRENT);
            obj_id sp = starport(self);
            int air = isIdValid(sp) ? travel.getAirTime(sp) : 60;
            setAvailable(self, false);
            messageTo(self, "serviceNextArrival", null, air, false);
            serviceNextHolding(self);
        }
        return SCRIPT_CONTINUE;
    }

    /**
     * At the jump point: passengers bound off-planet enter hyperspace (to their vessel on the destination server); the
     * vessel itself cruises on with the passengers for one port on this planet (clones take any other port's), or flies
     * off and is retired when it has none.
     */
    public int serviceJumpPoint(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (!vessel.isVessel(ship))
            return SCRIPT_CONTINUE;
        String here = getCurrentSceneName();
        String flight = vessel.flightNumber(ship);
        obj_id[] aboard = vessel.getPlayersAboard(ship);
        Vector localPoints = new Vector();
        for (obj_id p : aboard)
        {
            if (!vessel.isPassenger(ship, p) && isGod(p))
            {
                // staff riding along without a booking: the flight takes them to the nearest other port, so it lands
                String point = vessel.nearestOtherServicePoint(ship, getStringObjVar(self, OV_POINT));
                if (point != null)
                    vessel.addPassenger(ship, p, here, point, obj_id.NULL_ID);
            }
            String[] dest = vessel.getPassengerDestination(ship, p);
            if (dest == null)
                continue;
            if (!dest[0].equals(here))
                sendThroughHyperspace(ship, p, dest[0], dest[1], flight);
            else if (!localPoints.contains(dest[1]))
                localPoints.add(dest[1]);
        }
        if (localPoints.isEmpty())
        {
            if (aboard.length > 0)
                vessel.announce(ship, "[Flight " + flight + "] Jump to lightspeed.");
            utils.setScriptVar(ship, vessel.SV_PATH_DONE, "vesselRetire");     // fly off out of sight, then go
            return SCRIPT_CONTINUE;
        }
        for (int i = 0; i < localPoints.size(); ++i)
        {
            String point = (String)localPoints.get(i);
            obj_id carrier = ship;
            if (i > 0)
            {
                carrier = cloneVessel(self, ship);
                if (!isIdValid(carrier))
                    continue;
                for (obj_id p : aboard)
                {
                    String[] dest = vessel.getPassengerDestination(ship, p);
                    if (dest != null && dest[0].equals(here) && dest[1].equals(point))
                    {
                        vessel.addPassenger(carrier, p, dest[0], dest[1], obj_id.NULL_ID);
                        vessel.removePassenger(ship, p);
                        vessel.moveIntoCabin(carrier, p);
                    }
                }
            }
            cruiseTo(carrier, here, point);
        }
        return SCRIPT_CONTINUE;
    }

    private obj_id cloneVessel(obj_id self, obj_id ship) throws InterruptedException
    {
        location at = getLocation(ship);
        obj_id clone = vessel.takeSpare(self, getTemplateName(ship), at.x, at.y, at.z, getYaw(ship), 0.0f);
        if (!isIdValid(clone))
            clone = createObject(getTemplateName(ship), at);
        if (!isIdValid(clone))
            return null;
        setTransform_o2p(clone, getTransform_o2p(ship));
        setObjVar(clone, vessel.OV_SERVICE, self);
        setObjVar(clone, vessel.OV_FLIGHT, vessel.flightNumber(ship) + "B");
        vessel.initParts(clone, false);
        vessel.fly(clone, vessel.continuePath(ship), null);
        return clone;
    }

    /** Cruise on this planet to `point`'s approach and ask its service for the pad there. */
    private void cruiseTo(obj_id ship, String planet, String point) throws InterruptedException
    {
        obj_id dest = vessel.findService(planet, point);
        vessel.Config c = vessel.getConfig(ship);
        setObjVar(ship, vessel.OV_DEST_PLANET, planet);
        setObjVar(ship, vessel.OV_DEST_POINT, point);
        if (!isIdValid(dest) || c == null)
        {
            // no Titan service there (a stock shuttleport): set them down at the travel point once out of sight
            LOG("vessel", "shuttle_service: no vessel service at " + planet + "/" + point + "; passengers set down there");
            utils.setScriptVar(ship, vessel.SV_PATH_DONE, "vesselRetire");
            return;
        }
        float[] entry = vessel.approachEntry(approachTarget(dest, c));
        vessel.setState(ship, vessel.STATE_CRUISE);
        vessel.Path p = vessel.cruisePath(vessel.continuePath(ship), entry, c.cruiseSpeed);
        vessel.fly(ship, p, "vesselCruiseDone");
        int minutes = Math.max(1, Math.round(p.duration() / 60.0f));
        vessel.announce(ship, "[Flight " + vessel.flightNumber(ship) + "] Cruising to " + point + ". Flight time about "
                + minutes + (minutes == 1 ? " minute." : " minutes."));
    }

    /** A service's pad on the ground: the terrain under it, or the bay floor it stands on down a landing well. */
    private static location padGround(obj_id service) throws InterruptedException
    {
        location pad = getLocation(service);
        if (!hasWell(service))
            pad.y = getHeightAtLocation(pad.x, pad.z);
        return pad;
    }

    /** Book the player's hyperspace leg and send them to the destination planet (vessel_passenger takes over). */
    private void sendThroughHyperspace(obj_id ship, obj_id player, String planet, String point, String flight) throws InterruptedException
    {
        String here = getCurrentSceneName();
        int seconds = vessel.hyperspaceSeconds(here, planet);
        location arrival = getPlanetTravelPointLocation(planet, point);
        if (arrival == null)
        {
            travel.movePlayerToDestination(player, planet, point);
            return;
        }
        setObjVar(player, vessel.OV_VOYAGE_FLIGHT, flight);
        setObjVar(player, vessel.OV_VOYAGE_PLANET, planet);
        setObjVar(player, vessel.OV_VOYAGE_POINT, point);
        setObjVar(player, vessel.OV_VOYAGE_ETA, getGameTime() + seconds);
        setObjVar(player, vessel.OV_VOYAGE_FROM, here);
        setObjVar(player, vessel.OV_VOYAGE_TEMPLATE, getTemplateName(ship));
        if (!hasScript(player, vessel.SCRIPT_PASSENGER))
            attachScript(player, vessel.SCRIPT_PASSENGER);
        vessel.removePassenger(ship, player);
        playClientEffectObj(player, "clienteffect/hyper_space_warp_enter.cef", ship, "");
        sendSystemMessage(player, string_id.unlocalized("[Flight " + flight + "] Jumping to lightspeed. Next stop: "
                + vessel.planetDisplayName(planet) + " via " + vessel.hyperlaneName(here, planet) + "."));
        float holdY = 2600.0f;
        warpPlayer(player, planet, arrival.x, holdY, arrival.z, null, 0.0f, 0.0f, 0.0f, "vesselVoyageArrived", true);
    }

    public int serviceDepartureEnd(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (vessel.isVessel(ship) && vessel.getState(ship) == vessel.STATE_DEPARTING && vessel.getPlayersAboard(ship).length == 0)
            messageTo(ship, "vesselRetire", null, 0.0f, false);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // arrivals: our own empty vessel, or one with passengers holding for the pad
    // ------------------------------------------------------------------

    public int serviceNextArrival(obj_id self, dictionary params) throws InterruptedException
    {
        if (state(self) != AWAY)
            return SCRIPT_CONTINUE;
        if (serviceNextHolding(self))
            return SCRIPT_CONTINUE;
        vessel.Config c = config(self);
        if (c == null)
            return SCRIPT_CONTINUE;
        float[] pose = padPose(self, c);
        float[] target = approachTarget(self, c);
        vessel.Path approach = vessel.approachPath(target, 0.0f);
        if (hasWell(self))
            approach.append(vessel.wellInPath(target, pose, 0.0f), approach.duration());
        float[] start = { (Float)approach.x.get(1), (Float)approach.y.get(1), (Float)approach.z.get(1), pose[3], -8.0f };
        obj_id ship = spawnVessel(self, start);
        if (!isIdValid(ship))
            return SCRIPT_CONTINUE;
        vessel.initParts(ship, false);
        clearToLand(self, ship, approach);
        return SCRIPT_CONTINUE;
    }

    /** Clear the first vessel holding for the pad; true when there was one. */
    private boolean serviceNextHolding(obj_id self) throws InterruptedException
    {
        Vector queue = utils.hasScriptVar(self, SV_QUEUE) ? utils.getResizeableObjIdArrayScriptVar(self, SV_QUEUE) : null;
        while (queue != null && !queue.isEmpty())
        {
            obj_id ship = (obj_id)queue.remove(0);
            utils.setScriptVar(self, SV_QUEUE, queue);
            if (vessel.isVessel(ship))
            {
                clearToLand(self, ship, null);
                return true;
            }
        }
        return false;
    }

    public int serviceRequestLanding(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (!vessel.isVessel(ship))
            return SCRIPT_CONTINUE;
        if (state(self) == AWAY)
        {
            clearToLand(self, ship, null);
            return SCRIPT_CONTINUE;
        }
        Vector queue = utils.hasScriptVar(self, SV_QUEUE) ? utils.getResizeableObjIdArrayScriptVar(self, SV_QUEUE) : new Vector();
        if (!queue.contains(ship))
            queue.add(ship);
        utils.setScriptVar(self, SV_QUEUE, queue);
        location pad = getLocation(self);
        dictionary d = new dictionary();
        d.put("x", pad.x);
        d.put("z", pad.z);
        messageTo(ship, "vesselHold", d, 0.0f, false);
        vessel.announce(ship, "[Flight " + vessel.flightNumber(ship) + "] Holding for the pad at " + getStringObjVar(self, OV_POINT)
                + "; the shuttle there is still boarding.");
        return SCRIPT_CONTINUE;
    }

    /** Approach and touchdown: from where the vessel is (cruising, holding, out of hyperspace) or a given approach. */
    private void clearToLand(obj_id self, obj_id ship, vessel.Path approach) throws InterruptedException
    {
        vessel.Config c = config(self);
        float[] pose = padPose(self, c);
        float[] target = approachTarget(self, c);
        vessel.Path path;
        if (approach != null)
            path = approach;
        else
        {
            // cruise from wherever it is to the approach entry, then the approach (without its lead-in key)
            vessel.Path full = vessel.approachPath(target, 0.0f);
            float[] entry = vessel.approachEntry(target);
            path = vessel.cruisePath(vessel.continuePath(ship), entry, c.cruiseSpeed);
            float t0 = path.duration();
            vessel.Path tail = new vessel.Path();
            for (int i = 2; i < full.size(); ++i)
                tail.key((Float)full.t.get(i), (Float)full.x.get(i), (Float)full.y.get(i), (Float)full.z.get(i),
                         (Float)full.yaw.get(i), (Float)full.pitch.get(i), (Float)full.roll.get(i));
            path.append(tail, t0);
            if (hasWell(self))
                path.append(vessel.wellInPath(target, pose, 0.0f), path.duration());
        }
        // legs down late on the approach, or halfway down the well (it hovers over the shaft with them up)
        float gearAt = hasWell(self) ? Math.max(0.0f, path.duration() - WELL_TIME + WELL_SINK * 0.5f)
                : Math.max(0.0f, path.duration() - (vessel.APPROACH_DURATION - vessel.APPROACH_GEAR_AT));
        setObjVar(ship, vessel.OV_SERVICE, self);
        setObjVar(self, OV_CURRENT, ship);
        setObjVar(self, OV_STATE, INBOUND);
        vessel.setState(ship, vessel.STATE_ARRIVING);
        vessel.fly(ship, path, "svc:serviceTouchdown");
        vessel.setGear(ship, true, gearAt);
        dictionary d = new dictionary();
        d.put("vessel", ship);
        messageTo(self, "serviceLandingEffects", d, Math.max(0.0f, path.duration() - 12.0f), false);
        if (vessel.getPlayersAboard(ship).length > 0)
            vessel.announce(ship, "[Flight " + vessel.flightNumber(ship) + "] Cleared to land at " + getStringObjVar(self, OV_POINT)
                    + ". We touch down in " + Math.round(path.duration()) + " seconds.");
    }

    public int serviceLandingEffects(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (vessel.isVessel(ship) && ship == current(self))
            playClientEffectObj(ship, "clienteffect/transport_landing.cef", ship, "hp_engine_1");
        return SCRIPT_CONTINUE;
    }

    public int serviceTouchdown(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id ship = params.getObjId("vessel");
        if (!vessel.isVessel(ship) || ship != current(self))
            return SCRIPT_CONTINUE;
        playClientEffectObj(ship, "clienteffect/transport_hitground.cef", ship, "hp_engine_1");
        String point = getStringObjVar(self, OV_POINT);
        String planet = vessel.planetDisplayName(getCurrentSceneName());
        // arrivals: the voyage ends here; they walk off by the ramp
        for (obj_id p : vessel.getPlayersAboard(ship))
        {
            sendSystemMessage(p, string_id.unlocalized("[Flight " + vessel.flightNumber(ship) + "] Welcome to " + point + ", " + planet
                    + ". Please disembark by the ramp."));
            if (hasObjVar(p, vessel.OV_VOYAGE))
                removeObjVar(p, vessel.OV_VOYAGE);
            if (hasScript(p, vessel.SCRIPT_PASSENGER))
                detachScript(p, vessel.SCRIPT_PASSENGER);
        }
        vessel.clearPassengers(ship);
        removeObjVar(ship, vessel.OV_DEST_PLANET);
        removeObjVar(ship, vessel.OV_DEST_POINT);
        removeObjVar(ship, vessel.OV_FLIGHT);
        beginBoarding(self, ship);
        return SCRIPT_CONTINUE;
    }
}
