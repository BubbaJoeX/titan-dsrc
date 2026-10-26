package script.space.vessel;

import script.*;
import script.library.*;

/**
 * On every Titan boardable vessel (server template script). Keeps the vessel's parts and cell locks set up, runs the
 * flight path bookkeeping (path-done handlers), the ramp-top ticket check, the hyperspace leg and its cabin
 * announcements, and retires the vessel safely. Scheduling (when to land, board and leave) belongs to the starport
 * shuttle service flying it (space.vessel.shuttle_service); a private vessel (the yacht) flies on its owner's radial:
 * ramp, take off to a hover, land where it hovers.
 */
public class vessel_controller extends script.base_script
{
    public vessel_controller()
    {
    }

    public static final float TICK = 1.0f;
    public static final int HYPERSPACE_NOTICE = 45;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setupVessel(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setupVessel(self);
        return SCRIPT_CONTINUE;
    }

    private void setupVessel(obj_id self) throws InterruptedException
    {
        vessel.Config c = vessel.getConfig(self);
        if (c == null)
        {
            LOG("vessel", "vessel_controller: no titan_vessels row for " + getTemplateName(self));
            return;
        }
        if (!utils.hasScriptVar(self, "vessel.setup"))
        {
            utils.setScriptVar(self, "vessel.setup", 1);
            boolean landed = hasObjVar(self, "vessel.spawn_landed");
            vessel.initParts(self, landed);
            obj_id ramp = vessel.cellOf(self, c.rampCell);
            obj_id cabin = vessel.cellOf(self, c.cabinCell);
            if (isIdValid(ramp) && !hasScript(ramp, vessel.SCRIPT_CELL))
                attachScript(ramp, vessel.SCRIPT_CELL);
            if (isIdValid(cabin) && !hasScript(cabin, vessel.SCRIPT_CELL))
                attachScript(cabin, vessel.SCRIPT_CELL);
            vessel.nameVessel(self, c);
            messageTo(self, "vesselTick", null, TICK, false);
        }
    }

    // ------------------------------------------------------------------
    // tick: path ends, hyperspace
    // ------------------------------------------------------------------

    // The tick re-arms before its work, so an exception in one tick (a script reload, a bad row) can't end the loop.
    // Each loop carries a generation: a tick sent without one (start-up, `titan.sh vessel <scene> retick`) starts a
    // new generation and any older loop still running drops out at its next tick.
    public int vesselTick(obj_id self, dictionary params) throws InterruptedException
    {
        int gen = params != null && params.containsKey("gen") ? params.getInt("gen") : -1;
        int current = utils.hasScriptVar(self, SV_TICK_GEN) ? utils.getIntScriptVar(self, SV_TICK_GEN) : 0;
        if (gen >= 0 && gen != current)
            return SCRIPT_CONTINUE;
        if (gen < 0)
        {
            current += 1;
            utils.setScriptVar(self, SV_TICK_GEN, current);
        }
        if (vessel.getState(self) != vessel.STATE_RETIRING)
        {
            dictionary next = new dictionary();
            next.put("gen", current);
            messageTo(self, "vesselTick", next, TICK, false);
        }
        if (utils.hasScriptVar(self, vessel.SV_PATH_DONE))
        {
            float elapsed = shipGetFlightPathElapsed(self);
            float duration = shipGetFlightPathDuration(self);
            if (duration < 0.0f || elapsed >= duration - 0.05f)
            {
                String handler = utils.getStringScriptVar(self, vessel.SV_PATH_DONE);
                utils.removeScriptVar(self, vessel.SV_PATH_DONE);
                dictionary d = new dictionary();
                d.put("vessel", self);
                if (handler.startsWith("svc:"))
                {
                    obj_id service = hasObjVar(self, vessel.OV_SERVICE) ? getObjIdObjVar(self, vessel.OV_SERVICE) : null;
                    if (isIdValid(service))
                        messageTo(service, handler.substring(4), d, 0.0f, false);
                }
                else
                    messageTo(self, handler, d, 0.0f, false);
            }
        }
        if (vessel.getState(self) == vessel.STATE_HYPERSPACE && hasObjVar(self, vessel.OV_ETA))
        {
            int left = getIntObjVar(self, vessel.OV_ETA) - getGameTime();
            int last = utils.hasScriptVar(self, "vessel.hyper_notice") ? utils.getIntScriptVar(self, "vessel.hyper_notice") : 0;
            if (left <= 0)
                messageTo(self, "vesselExitHyperspace", null, 0.0f, false);
            else if (getGameTime() - last >= HYPERSPACE_NOTICE)
            {
                utils.setScriptVar(self, "vessel.hyper_notice", getGameTime());
                String dest = vessel.planetDisplayName(getStringObjVar(self, vessel.OV_DEST_PLANET));
                vessel.announce(self, "[Flight " + vessel.flightNumber(self) + "] In hyperspace on " + getStringObjVar(self, "vessel.route")
                        + ". Arrival at " + dest + " in " + minutes(left) + ".");
            }
        }
        int state = vessel.getState(self);
        if (state != vessel.STATE_STOWED && state != vessel.STATE_RETIRING)
            vessel.syncCabinAccess(self);
        return SCRIPT_CONTINUE;
    }

    private static final String SV_TICK_GEN = "vessel.tick_gen";

    private static String minutes(int seconds)
    {
        if (seconds < 60)
            return seconds + " seconds";
        int m = seconds / 60, s = seconds % 60;
        return m + (m == 1 ? " minute" : " minutes") + (s >= 10 ? " " + s + " s" : "");
    }

    // ------------------------------------------------------------------
    // ramp top: tickets
    // ------------------------------------------------------------------

    public int vesselCellEntered(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        obj_id cell = params.getObjId("cell");
        if (!isIdValid(player) || !exists(player))
            return SCRIPT_CONTINUE;
        int state = vessel.getState(self);
        boolean rampCell = cell == vessel.getRampCell(self);
        String flight = vessel.flightNumber(self);
        obj_id service = hasObjVar(self, vessel.OV_SERVICE) ? getObjIdObjVar(self, vessel.OV_SERVICE) : null;
        if (isPrivate(self))
        {
            // no tickets on a private vessel: the cabin admits its owner (vessel.syncCabinAccess); greet them as
            // they step on from outside, not on the way down
            obj_id from = params.getObjId("from");
            boolean boarding = !isIdValid(from) || !self.equals(getContainedBy(from));
            if (rampCell && boarding && player == getOwner(self) && (!utils.hasScriptVar(player, "vessel.welcomed")
                    || utils.getIntScriptVar(player, "vessel.welcomed") < getGameTime() - 60))
            {
                utils.setScriptVar(player, "vessel.welcomed", getGameTime());
                sendSystemMessage(player, string_id.unlocalized(getName(self) + ": welcome aboard. Flight controls and paint are on the helm console."));
            }
            return SCRIPT_CONTINUE;
        }
        if (rampCell && state == vessel.STATE_BOARDING)
        {
            if (vessel.isPassenger(self, player))
            {
                String[] dest = vessel.getPassengerDestination(self, player);
                sendSystemMessage(player, string_id.unlocalized("Welcome aboard flight " + flight + " to " + dest[1] + ", "
                        + vessel.planetDisplayName(dest[0]) + ". Please take a seat in the cabin."));
            }
            else if (isGod(player))
                sendSystemMessage(player, string_id.unlocalized("Flight " + flight + ": welcome aboard. Staff ride along without a ticket."));
            else if (isIdValid(service) && hasValidTicket(player, service))
            {
                // the stock boarding command shows the ticket list (or takes the only ticket) and books the seat
                queueCommand(player, (1573732770), service, "", COMMAND_PRIORITY_DEFAULT);
            }
            else if (!utils.hasScriptVar(player, "vessel.no_ticket_told") || utils.getIntScriptVar(player, "vessel.no_ticket_told") < getGameTime() - 20)
            {
                utils.setScriptVar(player, "vessel.no_ticket_told", getGameTime());
                sendSystemMessage(player, string_id.unlocalized("The steward checks your papers: you need a ticket for this departure to go aboard. "
                        + "Tickets are sold at the starport's travel terminals."));
            }
        }
        else if (!rampCell && vessel.isPassenger(self, player) && state == vessel.STATE_BOARDING)
        {
            int departs = isIdValid(service) && hasObjVar(service, "vessel.service.departs") ? getIntObjVar(service, "vessel.service.departs") - getGameTime() : -1;
            sendSystemMessage(player, string_id.unlocalized("Flight " + flight + ": find a seat and enjoy the cabin screen."
                    + (departs > 0 ? " We lift off in " + minutes(departs) + "." : "")));
        }
        return SCRIPT_CONTINUE;
    }

    private boolean hasValidTicket(obj_id player, obj_id service) throws InterruptedException
    {
        String point = hasObjVar(service, "vessel.service.point") ? getStringObjVar(service, "vessel.service.point") : null;
        return vessel.holdsTicketFrom(player, getCurrentSceneName(), point);
    }

    // ------------------------------------------------------------------
    // hyperspace
    // ------------------------------------------------------------------

    /** params: planet, point, eta (game time), route. Puts the vessel in transit where it is (a still hover). */
    public int vesselEnterHyperspace(obj_id self, dictionary params) throws InterruptedException
    {
        setObjVar(self, vessel.OV_DEST_PLANET, params.getString("planet"));
        setObjVar(self, vessel.OV_DEST_POINT, params.getString("point"));
        setObjVar(self, vessel.OV_ETA, params.getInt("eta"));
        setObjVar(self, "vessel.route", params.getString("route"));
        vessel.setState(self, vessel.STATE_HYPERSPACE);
        vessel.setGear(self, false, 0.0f);
        vessel.setHyperspaceVisual(self, true);
        utils.setScriptVar(self, "vessel.hyper_notice", getGameTime());
        return SCRIPT_CONTINUE;
    }

    public int vesselExitHyperspace(obj_id self, dictionary params) throws InterruptedException
    {
        if (vessel.getState(self) != vessel.STATE_HYPERSPACE)
            return SCRIPT_CONTINUE;
        vessel.setHyperspaceVisual(self, false);
        vessel.setState(self, vessel.STATE_HOLDING);
        String planet = getStringObjVar(self, vessel.OV_DEST_PLANET);
        String point = getStringObjVar(self, vessel.OV_DEST_POINT);
        playClientEffectObj(vessel.getPlayersAboard(self), "clienteffect/hyper_space_warp_end.cef", self, "");
        vessel.announce(self, "[Flight " + vessel.flightNumber(self) + "] Dropping out of hyperspace over "
                + vessel.planetDisplayName(planet) + ". Approaching " + point + ".");
        requestLanding(self);
        return SCRIPT_CONTINUE;
    }

    /** Ask the destination port for its pad; it clears us (approach) or we hold and ask again each lap. */
    private void requestLanding(obj_id self) throws InterruptedException
    {
        String planet = getStringObjVar(self, vessel.OV_DEST_PLANET);
        String point = getStringObjVar(self, vessel.OV_DEST_POINT);
        obj_id service = vessel.findService(planet, point);
        if (!isIdValid(service))
        {
            LOG("vessel", "vessel_controller: " + self + " found no shuttle service for " + planet + "/" + point + "; setting passengers down");
            setDownPassengersAt(self, planet, point);
            messageTo(self, "vesselRetire", null, 5.0f, false);
            return;
        }
        dictionary d = new dictionary();
        d.put("vessel", self);
        messageTo(service, "serviceRequestLanding", d, 0.0f, false);
    }

    /** No port to land at: passengers go to the travel point's arrival spot (the stock travel result). */
    private void setDownPassengersAt(obj_id self, String planet, String point) throws InterruptedException
    {
        for (obj_id p : vessel.getPlayersAboard(self))
            travel.movePlayerToDestination(p, planet, point);
    }

    /** Holding orbit round (x, z) until the port clears us; params: x, z. */
    public int vesselHold(obj_id self, dictionary params) throws InterruptedException
    {
        vessel.setState(self, vessel.STATE_HOLDING);
        float cx = params.getFloat("x"), cz = params.getFloat("z");
        vessel.Config c = vessel.getConfig(self);
        vessel.fly(self, vessel.holdingPath(self, cx, cz, 1500.0f, c != null ? c.cruiseSpeed * 0.7f : 110.0f), "vesselHoldLap");
        return SCRIPT_CONTINUE;
    }

    public int vesselHoldLap(obj_id self, dictionary params) throws InterruptedException
    {
        if (vessel.getState(self) == vessel.STATE_HOLDING)
            requestLanding(self);
        return SCRIPT_CONTINUE;
    }

    /** End of a same-planet cruise: ask for the pad. */
    public int vesselCruiseDone(obj_id self, dictionary params) throws InterruptedException
    {
        vessel.setState(self, vessel.STATE_HOLDING);
        requestLanding(self);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // retiring
    // ------------------------------------------------------------------

    /**
     * Take the vessel out of service without crashing clients that see it: anyone still aboard is set down first, then
     * the ship is moved far away (clients drop it from DPVS). A service vessel is then stowed there for its service's
     * next flight; destroying POB ships in an atmospheric-flight scene corrupts the game server's heap, so only vessels
     * with no service left are destroyed, a few seconds later (see atmospheric-flight.md, DPVS).
     */
    public int vesselRetire(obj_id self, dictionary params) throws InterruptedException
    {
        vessel.setState(self, vessel.STATE_RETIRING);
        for (obj_id p : vessel.getPlayersAboard(self))
        {
            if (hasObjVar(self, vessel.OV_DEST_PLANET))
                travel.movePlayerToDestination(p, getStringObjVar(self, vessel.OV_DEST_PLANET), getStringObjVar(self, vessel.OV_DEST_POINT));
            else
                vessel.moveOffShip(self, p);
        }
        shipClearFlightPath(self);
        location here = getLocation(self);
        if (isIdValid(here.cell))
        {
            // parked in a hangar cell: it can't leave the cell (moving it out would re-parent it on clients), so it is
            // stowed in place on its legs with the ramp up; takeSpare reuses it there
            vessel.Config c = vessel.getConfig(self);
            vessel.fly(self, vessel.holdPath(new float[]{ here.x, here.y, here.z, vessel.getVesselYaw(self), c != null ? c.landPitch : 0.0f }), null);
            vessel.setRamp(self, false, 0.0f);
            obj_id cellService = hasObjVar(self, vessel.OV_SERVICE) ? getObjIdObjVar(self, vessel.OV_SERVICE) : null;
            if (!isIdValid(cellService) || !exists(cellService))
            {
                messageTo(self, "vesselRetireFinal", null, 3.0f, false);
                return SCRIPT_CONTINUE;
            }
            vessel.clearPassengers(self);
            vessel.setHyperspaceVisual(self, false);
            removeObjVar(self, vessel.OV_DEST_PLANET);
            removeObjVar(self, vessel.OV_DEST_POINT);
            removeObjVar(self, vessel.OV_ETA);
            removeObjVar(self, vessel.OV_FLIGHT);
            removeObjVar(self, "vessel.route");
            vessel.addSpare(cellService, self);
            return SCRIPT_CONTINUE;
        }
        float far = 7000.0f;
        float x = here.x > 0 ? here.x - far : here.x + far;
        float z = here.z > 0 ? here.z - far : here.z + far;
        float y = Math.max(here.y, getHeightAtLocation(x, z) + 2000.0f);
        transform t = getTransform_o2p(self);
        t.setPosition_p(x, y, z);
        setTransform_o2p(self, t);
        obj_id service = hasObjVar(self, vessel.OV_SERVICE) ? getObjIdObjVar(self, vessel.OV_SERVICE) : null;
        if (isIdValid(service) && exists(service))
        {
            vessel.fly(self, new vessel.Path().key(0.0f, x, y, z, getYaw(self), 0.0f, 0.0f), null);
            vessel.clearPassengers(self);
            vessel.setHyperspaceVisual(self, false);
            removeObjVar(self, vessel.OV_DEST_PLANET);
            removeObjVar(self, vessel.OV_DEST_POINT);
            removeObjVar(self, vessel.OV_ETA);
            removeObjVar(self, vessel.OV_FLIGHT);
            removeObjVar(self, "vessel.route");
            vessel.addSpare(service, self);
            return SCRIPT_CONTINUE;
        }
        messageTo(self, "vesselRetireFinal", null, 3.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int vesselRetireFinal(obj_id self, dictionary params) throws InterruptedException
    {
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // a deeded private vessel (space.vessel.vessel_deed): sent away it climbs out and is stowed, never destroyed
    // ------------------------------------------------------------------

    public int vesselSendAway(obj_id self, dictionary params) throws InterruptedException
    {
        int state = vessel.getState(self);
        if (state != vessel.STATE_PARKED && state != vessel.STATE_BOARDING)
            return SCRIPT_CONTINUE;
        float delay = 0.0f;
        if (vessel.isRampOpen(self))
        {
            vessel.setRamp(self, false, 0.0f);
            delay = 6.0f;                            // the ramp is up before the lift
        }
        vessel.setState(self, vessel.STATE_DEPARTING);
        messageTo(self, "vesselClimbOut", null, Math.max(0.1f, delay), false);
        return SCRIPT_CONTINUE;
    }

    public int vesselClimbOut(obj_id self, dictionary params) throws InterruptedException
    {
        if (vessel.getState(self) != vessel.STATE_DEPARTING)
            return SCRIPT_CONTINUE;
        location here = getLocation(self);
        vessel.Config c = vessel.getConfig(self);
        float[] pose = new float[]{ here.x, here.y, here.z, vessel.getVesselYaw(self), c != null ? c.landPitch : 0.0f };
        vessel.setGear(self, false, 5.0f);
        playClientEffectObj(self, FX_TAKEOFF, self, "");
        vessel.fly(self, vessel.departurePath(pose, 0.0f), "vesselStow");
        return SCRIPT_CONTINUE;
    }

    public int vesselStow(obj_id self, dictionary params) throws InterruptedException
    {
        for (obj_id p : vessel.getPlayersAboard(self))
            vessel.moveOffShip(self, p);
        shipClearFlightPath(self);
        location here = getLocation(self);
        float far = 7000.0f;
        float x = here.x > 0 ? here.x - far : here.x + far;
        float z = here.z > 0 ? here.z - far : here.z + far;
        float y = Math.max(here.y, getHeightAtLocation(x, z) + 2000.0f);
        transform t = getTransform_o2p(self);
        t.setPosition_p(x, y, z);
        setTransform_o2p(self, t);
        vessel.fly(self, new vessel.Path().key(0.0f, x, y, z, getYaw(self), 0.0f, 0.0f), null);
        vessel.setState(self, vessel.STATE_STOWED);
        obj_id owner = getOwner(self);
        if (isIdValid(owner) && exists(owner))
            sendSystemMessage(owner, string_id.unlocalized(getName(self) + " is stowed; call it from its deed."));
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // private flight (the yacht): its owner lowers the ramp, takes off and lands where it hovers
    // ------------------------------------------------------------------

    public static final float HOVER_HEIGHT = 45.0f;

    private boolean isPrivate(obj_id self) throws InterruptedException
    {
        return !hasObjVar(self, vessel.OV_SERVICE);
    }

    private boolean isLanded(obj_id self) throws InterruptedException
    {
        int state = vessel.getState(self);
        return state == vessel.STATE_PARKED || state == vessel.STATE_BOARDING;
    }

    private void tellOwnerAndCrew(obj_id self, String text) throws InterruptedException
    {
        obj_id owner = getOwner(self);
        vessel.announce(self, text);
        if (isIdValid(owner) && exists(owner) && !vessel.isAboard(self, owner))
            sendSystemMessage(owner, string_id.unlocalized(text));
    }

    /** params (optional): open (int 1/0); toggles without it. Landed only. */
    public int vesselRamp(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isLanded(self))
            return SCRIPT_CONTINUE;
        boolean open = params != null && params.containsKey("open") ? params.getInt("open") != 0 : !vessel.isRampOpen(self);
        vessel.setRamp(self, open, 0.0f);
        vessel.setState(self, open ? vessel.STATE_BOARDING : vessel.STATE_PARKED);
        return SCRIPT_CONTINUE;
    }

    public int vesselTakeOff(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isPrivate(self) || !isLanded(self))
            return SCRIPT_CONTINUE;
        float delay = 0.0f;
        if (vessel.isRampOpen(self))
        {
            vessel.setRamp(self, false, 0.0f);       // moves anyone on the ramp off (or in) first
            delay = 4.5f;                            // the plank tilts up, then slides home, before the lift
        }
        vessel.setState(self, vessel.STATE_DEPARTING);
        dictionary d = new dictionary();
        messageTo(self, "vesselLiftOff", d, delay, false);
        tellOwnerAndCrew(self, getName(self) + ": ramp up, lifting off.");
        return SCRIPT_CONTINUE;
    }

    public int vesselLiftOff(obj_id self, dictionary params) throws InterruptedException
    {
        if (vessel.getState(self) != vessel.STATE_DEPARTING)
            return SCRIPT_CONTINUE;
        vessel.setGear(self, false, 4.0f);
        playClientEffectObj(self, FX_TAKEOFF, self, "");
        vessel.fly(self, vessel.takeOffPath(self, HOVER_HEIGHT), "vesselAirborne");
        return SCRIPT_CONTINUE;
    }

    // stock transport cues: the takeoff roar and dust ring, the landing roar (the touchdown thump is FX_HIT_GROUND)
    public static final String FX_TAKEOFF = "clienteffect/transport_takeoff.cef";
    public static final String FX_LANDING = "clienteffect/transport_landing.cef";
    public static final String FX_HIT_GROUND = "clienteffect/transport_hitground.cef";
    public static final float LANDING_CUE = 10.0f;                 // seconds before touchdown

    /** Scheduled LANDING_CUE seconds before a landing path ends (vessel_deed's call, Land Here). */
    public int vesselLandingCue(obj_id self, dictionary params) throws InterruptedException
    {
        if (vessel.getState(self) == vessel.STATE_ARRIVING)
            playClientEffectObj(self, FX_LANDING, self, "");
        return SCRIPT_CONTINUE;
    }

    /** library.vessel.setGear's delayed leg move coming due. */
    public int vesselGear(obj_id self, dictionary params) throws InterruptedException
    {
        vessel.gearDue(self, params);
        return SCRIPT_CONTINUE;
    }

    public int vesselAirborne(obj_id self, dictionary params) throws InterruptedException
    {
        vessel.setState(self, vessel.STATE_AIRBORNE);
        if (utils.hasScriptVar(self, SV_HANDOVER))
        {
            // lifted off for the pilot who took the helm: the controls are theirs from the hover
            utils.removeScriptVar(self, SV_HANDOVER);
            if (isIdValid(getPilotId(self)))
            {
                vessel.releaseToPilot(self);
                tellOwnerAndCrew(self, getName(self) + ": airborne. You have the controls.");
                return SCRIPT_CONTINUE;
            }
        }
        tellOwnerAndCrew(self, getName(self) + ": airborne, holding a hover.");
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // flown as a player starship (vessel.isStarship): the stock pilot seat, the atmospheric autopilot and summon
    // ------------------------------------------------------------------

    private static final String SV_HANDOVER = "vessel.handover";
    public static final float SET_DOWN_AGL = 120.0f;       // left lower than this, the ship sets down on its legs

    /** combat_ship_player.cmdPilotShip seated a pilot: a landed ship lifts off first, then the controls are theirs. */
    public int vesselPilotSeated(obj_id self, dictionary params) throws InterruptedException
    {
        if (isLanded(self))
        {
            utils.setScriptVar(self, SV_HANDOVER, 1);
            vessel.setState(self, vessel.STATE_PARKED);
            messageTo(self, "vesselTakeOff", null, 0.0f, false);
            return SCRIPT_CONTINUE;
        }
        int state = vessel.getState(self);
        if (state == vessel.STATE_DEPARTING)
        {
            utils.setScriptVar(self, SV_HANDOVER, 1);          // already lifting off: the hover hands over
            return SCRIPT_CONTINUE;
        }
        vessel.releaseToPilot(self);
        return SCRIPT_CONTINUE;
    }

    /** combat_ship_player.cmdLeaveStation: the pilot let go of the controls. Low over open ground it sets down. */
    public int vesselPilotLeft(obj_id self, dictionary params) throws InterruptedException
    {
        if (isLanded(self) || isIdValid(getPilotId(self)))
            return SCRIPT_CONTINUE;
        location here = getLocation(self);
        if (isIdValid(here.cell) || here.y - getHeightAtLocation(here.x, here.z) > SET_DOWN_AGL)
            return SCRIPT_CONTINUE;
        vessel.setState(self, vessel.STATE_AIRBORNE);
        messageTo(self, "vesselLandHere", null, 0.5f, false);
        return SCRIPT_CONTINUE;
    }

    /** combat_ship: a summoned ship arrived over its owner; it settles onto its legs if the ground below takes it. */
    public int vesselSetDown(obj_id self, dictionary params) throws InterruptedException
    {
        if (isLanded(self) || isIdValid(getPilotId(self)))
            return SCRIPT_CONTINUE;
        vessel.setState(self, vessel.STATE_AIRBORNE);
        messageTo(self, "vesselLandHere", null, 0.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int vesselLandHere(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isPrivate(self) || vessel.getState(self) != vessel.STATE_AIRBORNE)
            return SCRIPT_CONTINUE;
        if (!vessel.canSetDownHere(self))
        {
            vessel.setGear(self, false, 1.0f);
            tellOwnerAndCrew(self, getName(self) + ": no level, open ground under us to set down on. Holding a hover.");
            return SCRIPT_CONTINUE;
        }
        vessel.setState(self, vessel.STATE_ARRIVING);
        vessel.Path p = vessel.landHerePath(self);
        vessel.setGear(self, true, Math.max(0.0f, p.duration() - 9.0f));
        vessel.fly(self, p, "vesselLanded");
        messageTo(self, "vesselLandingCue", null, Math.max(0.0f, p.duration() - LANDING_CUE), false);
        tellOwnerAndCrew(self, getName(self) + ": setting down.");
        return SCRIPT_CONTINUE;
    }

    public int vesselLanded(obj_id self, dictionary params) throws InterruptedException
    {
        playClientEffectObj(self, FX_HIT_GROUND, self, "");
        obj_id owner = getOwner(self);
        if (isIdValid(owner) && exists(owner) && vessel.getPlayersAboard(self).length == 0)
        {
            // called in empty from its deed: the ramp comes down for the owner waiting outside
            vessel.setRamp(self, true, 1.0f);
            vessel.setState(self, vessel.STATE_BOARDING);
            tellOwnerAndCrew(self, getName(self) + ": down and safe. Ramp lowered; come aboard.");
            return SCRIPT_CONTINUE;
        }
        vessel.setState(self, vessel.STATE_PARKED);
        tellOwnerAndCrew(self, getName(self) + ": down and safe. Lower the ramp from the helm to disembark.");
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------
    // paint: vessels whose template declares /private/index_color_1 and _2 (hull and trim zones of the hue shaders)
    // ------------------------------------------------------------------

    private int paint(obj_id self, dictionary params, String variable) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int idx = sui.getColorPickerIndex(params);
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL || idx < 0 || !vessel.canFly(self, player))
            return SCRIPT_CONTINUE;
        hue.setColor(self, variable, idx);
        return SCRIPT_CONTINUE;
    }

    // the stock colour picker over the ship's paint palette, previewing on the hull as the owner browses
    private void openPaint(obj_id self, obj_id player, String variable, String handler, String zone) throws InterruptedException
    {
        int pid = sui.colorize(self, player, self, variable, handler);
        if (pid < 0)
            return;
        setSUIProperty(pid, sui.COLORPICKER_TITLE, sui.PROP_TEXT, getName(self) + ": " + zone);
        flushSUIPage(pid);
    }

    public int handlePaintHull(obj_id self, dictionary params) throws InterruptedException
    {
        return paint(self, params, hue.INDEX_1);
    }

    public int handlePaintTrim(obj_id self, dictionary params) throws InterruptedException
    {
        return paint(self, params, hue.INDEX_2);
    }

    // ------------------------------------------------------------------
    // radial: the owner's flight controls; god tools
    // ------------------------------------------------------------------

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        vessel.addFlightMenu(self, player, mi);
        if (!isGod(player))
            return SCRIPT_CONTINUE;
        int root = mi.addRootMenu(menu_info_types.SERVER_MENU10, string_id.unlocalized("Vessel (god)"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU11, string_id.unlocalized(vessel.isRampOpen(self) ? "Raise ramp" : "Lower ramp"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU12, string_id.unlocalized(vessel.isGearDown(self) ? "Gear up" : "Gear down"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU13, string_id.unlocalized("Board (cabin)"));
        return SCRIPT_CONTINUE;
    }

    // an item of vessel.addFlightMenu, picked on the hull or on the helm console
    private void flightMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!vessel.canFly(self, player))
            return;
        if (item == menu_info_types.SERVER_MENU15)
            messageTo(self, "vesselRamp", null, 0.0f, false);
        else if (item == menu_info_types.SERVER_MENU16)
            messageTo(self, "vesselTakeOff", null, 0.0f, false);
        else if (item == menu_info_types.SERVER_MENU17)
            messageTo(self, "vesselLandHere", null, 0.0f, false);
        else if (item == menu_info_types.SERVER_MENU19 && vessel.isPaintable(self))
            openPaint(self, player, hue.INDEX_1, "handlePaintHull", "Hull Colour");
        else if (item == menu_info_types.SERVER_MENU20 && vessel.isPaintable(self))
            openPaint(self, player, hue.INDEX_2, "handlePaintTrim", "Trim Colour");
        else if (item == menu_info_types.SERVER_MENU21)
            sui.inputbox(self, player, "The name painted on your vessel and shown over it (up to " + NAME_MAX + " characters).",
                    "Name Vessel", "handleNameVessel", NAME_MAX, true, getName(self));
    }

    private static final int NAME_MAX = 40;

    public int handleNameVessel(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL || !vessel.canFly(self, player))
            return SCRIPT_CONTINUE;
        String name = sui.getInputBoxText(params);
        name = name == null ? "" : name.trim();
        if (name.length() == 0 || name.length() > NAME_MAX || isObscene(name) || !isAppropriateText(name))
        {
            sendSystemMessage(player, string_id.unlocalized("That name can't be used for a vessel."));
            return SCRIPT_CONTINUE;
        }
        setName(self, name);
        sendSystemMessage(player, string_id.unlocalized("Your vessel is now the " + name + "."));
        return SCRIPT_CONTINUE;
    }

    /** From the helm console (space.vessel.vessel_helm): params player, item. */
    public int vesselHelm(obj_id self, dictionary params) throws InterruptedException
    {
        flightMenuSelect(self, params.getObjId("player"), params.getInt("item"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        flightMenuSelect(self, player, item);
        if (!isGod(player))
            return SCRIPT_CONTINUE;
        if (item == menu_info_types.SERVER_MENU11)
            vessel.setRamp(self, !vessel.isRampOpen(self), 0.0f);
        else if (item == menu_info_types.SERVER_MENU12)
            vessel.setGear(self, !vessel.isGearDown(self), 0.0f);
        else if (item == menu_info_types.SERVER_MENU13)
        {
            obj_id cabin = vessel.getCabinCell(self);
            if (isIdValid(cabin))
            {
                permissionsAddAllowed(cabin, getFirstName(player));
                sendDirtyCellPermissionsUpdate(cabin, player, true);
            }
            vessel.moveIntoCabin(self, player);
        }
        return SCRIPT_CONTINUE;
    }
}
