package script.titan;

import script.location;
import script.obj_id;

/*
 * Server-console handlers for local development (no player or client needed). Called through
 * the CentralServer console, e.g. from D:\titan\wsl\titan.sh:
 *
 *   game any runScript titan.dev_console spawn <scene> <template> <x> <z>
 *   game any runScript titan.dev_console destroy <scene> <oid>
 *
 * "game any" picks a random game server, so each handler first checks that it runs on the
 * server for <scene> and otherwise answers "wrong scene" (titan.sh spawn/despawn retry).
 */
public class dev_console extends script.base_script
{
    public dev_console()
    {
    }
    private static final String WRONG_SCENE = "wrong scene ";

    public String spawn(String args) throws InterruptedException
    {
        String[] a = args.trim().split("\\s+");
        if (a.length < 4)
        {
            return "usage: spawn <scene> <template> <x> <z>";
        }
        String scene = getCurrentSceneName();
        if (!a[0].equals(scene))
        {
            return WRONG_SCENE + scene;
        }
        float x = Float.parseFloat(a[2]);
        float z = Float.parseFloat(a[3]);
        location loc = new location(x, getHeightAtLocation(x, z), z, scene);
        obj_id obj = createObject(a[1], loc);
        if (!isIdValid(obj))
        {
            return "spawn failed: " + a[1] + " at " + loc;
        }
        return "spawned " + obj + " " + getTemplateName(obj) + " at " + getLocation(obj);
    }

    public String destroy(String args) throws InterruptedException
    {
        String[] a = args.trim().split("\\s+");
        if (a.length < 2)
        {
            return "usage: destroy <scene> <oid>";
        }
        String scene = getCurrentSceneName();
        if (!a[0].equals(scene))
        {
            return WRONG_SCENE + scene;
        }
        obj_id obj = obj_id.getObjId(Long.parseLong(a[1]));
        if (!isIdValid(obj) || !exists(obj))
        {
            return "no object " + a[1] + " on " + scene;
        }
        return destroyObject(obj) ? "destroyed " + a[1] : "destroy failed " + a[1];
    }

    /*
     * Titan vessel services (script.space.vessel.shuttle_service):
     *   vessel <scene> list                  every travel point's service, its vessel and where it is
     *   vessel <scene> status <vessel oid>   state, pose, flight path, ramp / gear, passengers
     *   vessel <scene> depart <service oid>  close the ramp and take off now
     *   vessel <scene> arrive <service oid>  bring the next vessel in now (pad must be free)
     *   vessel <scene> restart <service oid> retire the current vessel and park a fresh one
     *   vessel <scene> park <template> <x> <z> <heading>   a private vessel (the yacht) parked on its legs
     *   vessel <scene> parkcell <template> <cell oid> <x> <floorY> <z> <heading>   parked inside a hangar cell (cell space)
     *   vessel <scene> parkat <template> <oid> [heading]    parked where <oid> stands (its hangar cell, or the terrain)
     *   vessel <scene> ramp|takeoff|land <vessel oid>       its owner's flight controls
     *   vessel <scene> near <x> <z> <r>                     vessels around a point
     *   vessel <scene> traffic <structure oid>   add starport traffic control to an already initialized starport
     *   vessel <scene> dock|undock <traffic control oid> <ship oid>   land a ship in a bay of a starport's well (no
     *                                         pilot, no fee) / fly it back out (space.atmo.starport_traffic)
     *   vessel <scene> starport <structure oid> <point name> [cost]   make a spawned starport a travel point and
     *                                         start its shuttle service (travel.initializeStarport)
     */
    /** On a player (attached by "vessel <scene> grant"): an owned ship and its control device, then detach. */
    public int devGrantShip(obj_id self, script.dictionary params) throws InterruptedException
    {
        String ship = params.getString("ship");
        obj_id scd = script.library.space_utils.createShipControlDevice(self, ship, true);
        LOG("titan", "dev_console grant " + ship + " to " + self + ": " + scd);
        detachScript(self, "titan.dev_console");
        return SCRIPT_CONTINUE;
    }

    public String vessel(String args) throws InterruptedException
    {
        // console handlers run without an owner script: messageTo with a delay > 0 (0 looks up the sender)
        String[] a = args.trim().split("\\s+");
        if (a.length < 2)
        {
            return "usage: vessel <scene> list|status <oid>|depart <oid>|arrive <oid>|restart <oid>";
        }
        String scene = getCurrentSceneName();
        if (!a[0].equals(scene))
        {
            return WRONG_SCENE + scene;
        }
        if (a[1].equals("list"))
        {
            StringBuilder sb = new StringBuilder();
            String[] points = getPlanetTravelPoints(scene);
            if (points != null)
            {
                for (String point : points)
                {
                    obj_id service = script.library.vessel.findService(scene, point);
                    if (!isIdValid(service))
                    {
                        continue;
                    }
                    obj_id ship = hasObjVar(service, "vessel.service.current") ? getObjIdObjVar(service, "vessel.service.current") : null;
                    sb.append(point).append(": service ").append(service).append(" state ").append(getIntObjVar(service, "vessel.service.state"))
                      .append(" at ").append(getLocation(service)).append(" yaw ").append(getYaw(service));
                    if (isIdValid(ship) && exists(ship))
                    {
                        sb.append(" | ").append(describeVessel(ship));
                    }
                    if (hasObjVar(service, script.library.vessel.OV_SPARES))
                    {
                        sb.append(" | stowed:");
                        for (obj_id spare : getObjIdArrayObjVar(service, script.library.vessel.OV_SPARES))
                        {
                            sb.append(" ").append(spare).append(exists(spare) ? "" : "(gone)");
                        }
                    }
                    sb.append("\n");
                }
            }
            return sb.length() > 0 ? sb.toString() : "no vessel services on " + scene;
        }
        if (a[1].equals("stress") && a.length >= 3)
        {
            // the shuttle service's vessel lifecycle, compressed: create (with its interior), fly 15 s, retire
            int n = Integer.parseInt(a[2]);
            String template = a.length >= 4 ? a[3] : "object/ship/player/player_titan_transport_shuttle.iff";
            // mode: retire (fly, then the service retire: teleport + destroy), fly (fly, then destroy), static (no flight,
            // destroys itself), none (no flight, nothing scheduled: destroy it from outside)
            String mode = a.length >= 5 ? a[4] : "retire";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; ++i)
            {
                float x = 3150.0f + rand(0, 400), z = -4450.0f + rand(0, 300);
                float y = getHeightAtLocation(x, z) + 40.0f;
                obj_id ship = createObject(template, new location(x, y, z, scene, null));
                if (!isIdValid(ship))
                {
                    continue;
                }
                script.library.vessel.Path p = new script.library.vessel.Path()
                    .key(0.0f, x, y, z, 0.0f, 0.0f, 0.0f)
                    .key(15.0f, x, y + 30.0f, z + 400.0f, 0.0f, 0.0f, 0.0f);
                if (mode.equals("static"))
                {
                    messageTo(ship, "vesselRetireFinal", null, 15.0f, false);
                }
                else if (mode.equals("none") || mode.equals("nobuild"))
                {
                    // created only: the caller destroys it (despawn) from outside; nobuild skips the interior buildout
                    if (mode.equals("nobuild"))
                    {
                        setObjVar(ship, "strBuildout", 1);
                    }
                }
                else
                {
                    script.library.vessel.fly(ship, p, mode.equals("fly") ? "vesselRetireFinal" : "vesselRetire");
                }
                script.library.vessel.initParts(ship, false);
                sb.append(ship).append(" ");
            }
            return "stress: " + sb.toString();
        }
        if (a[1].equals("travel"))
        {
            // what a ticket purchase checks: the travel points each planet knows, their costs, the planet routes
            StringBuilder sb = new StringBuilder();
            String[] planets = {"tatooine", "naboo", "corellia", "geonosis", "talus", "rori", "dantooine", "lok", "endor", "yavin4", "dathomir"};
            for (String planet : planets)
            {
                String[] points = getPlanetTravelPoints(planet);
                sb.append(planet).append(" route-from-").append(scene).append("=").append(getPlanetTravelCost(scene, planet)).append(": ");
                for (int i = 0; points != null && i < points.length; ++i)
                {
                    sb.append(points[i]).append("(").append(getPlanetTravelPointCost(planet, points[i]))
                      .append(getPlanetTravelPointInterplanetary(planet, points[i]) ? ",inter" : "").append(") ");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
        if (a[1].equals("terminals"))
        {
            // the travel terminals at each travel point of this planet and what purchaseTicket would say for a trip
            // from there to the first other point (same planet) and to each other running planet's first point
            StringBuilder sb = new StringBuilder();
            String[] points = getPlanetTravelPoints(scene);
            for (int i = 0; points != null && i < points.length; ++i)
            {
                location at = getPlanetTravelPointLocation(scene, points[i]);
                sb.append(points[i]).append(" at ").append(at == null ? "?" : (int)at.x + " " + (int)at.z).append(": ");
                obj_id[] near = at == null ? null : getObjectsInRange(at, 120.0f);
                int found = 0;
                for (int j = 0; near != null && j < near.length; ++j)
                {
                    if (!hasScript(near[j], "terminal.terminal_travel"))
                    {
                        continue;
                    }
                    ++found;
                    obj_id starport = script.library.travel.getStarportFromTerminal(near[j]);
                    String point = script.library.travel.getTravelPointName(starport);
                    sb.append("\n  terminal ").append(near[j]).append(" starport=").append(starport).append(" point=").append(point);
                    if (point == null)
                    {
                        continue;
                    }
                    String other = points[(i + 1) % points.length];
                    sb.append(" | to ").append(other).append(": ").append(ticketCheck(scene, point, scene, other));
                    String[] planets = {"naboo", "corellia", "geonosis"};
                    for (String planet : planets)
                    {
                        String[] there = getPlanetTravelPoints(planet);
                        sb.append(" | to ").append(planet).append(": ")
                          .append(there == null || there.length == 0 ? "no travel points (planet not running)" : ticketCheck(scene, point, planet, there[0]));
                    }
                }
                sb.append(found == 0 ? "no travel terminal within 120 m" : "").append("\n");
            }
            return sb.toString();
        }
        if (a[1].equals("near") && a.length >= 5)
        {
            // vessels within r of (x, z)
            location at = new location(Float.parseFloat(a[2]), 0.0f, Float.parseFloat(a[3]), scene, null);
            at.y = getHeightAtLocation(at.x, at.z);
            obj_id[] near = getObjectsInRange(at, Float.parseFloat(a[4]));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; near != null && i < near.length; ++i)
            {
                if (script.library.vessel.isVessel(near[i]))
                {
                    sb.append(describeVessel(near[i])).append("\n");
                }
            }
            return sb.length() > 0 ? sb.toString() : "no vessels there";
        }
        if (a[1].equals("datapad") && a.length >= 3)
        {
            // a player's datapad: the ship control devices (and what each holds) for radial tests
            obj_id player = a[2].matches("\\d+") ? obj_id.getObjId(Long.parseLong(a[2])) : getPlayerIdFromFirstName(a[2]);
            if (!isIdValid(player) || !exists(player))
            {
                return "no player " + a[2] + " on " + scene;
            }
            obj_id pad = script.library.utils.getPlayerDatapad(player);
            obj_id[] items = isIdValid(pad) ? getContents(pad) : null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; items != null && i < items.length; ++i)
            {
                sb.append("  ").append(items[i]).append(" ").append(getTemplateName(items[i]));
                obj_id[] inner = getContents(items[i]);
                for (int j = 0; inner != null && j < inner.length; ++j)
                    sb.append(" > ").append(inner[j]).append(" ").append(getTemplateName(inner[j]));
                sb.append("\n");
            }
            return sb.length() > 0 ? sb.toString() : "  datapad empty";
        }
        if (a[1].equals("deed") && a.length >= 4)
        {
            // a vessel deed (object/tangible/titan/vessel_deed/<name>_deed.iff) into a player's inventory
            obj_id player = a[3].matches("\\d+") ? obj_id.getObjId(Long.parseLong(a[3])) : getPlayerIdFromFirstName(a[3]);
            if (!isIdValid(player) || !exists(player))
            {
                return "no player " + a[3] + " on " + scene;
            }
            obj_id inv = script.library.utils.getInventoryContainer(player);
            obj_id deed = isIdValid(inv) ? createObject(a[2], inv, "") : null;
            return isIdValid(deed) ? "deed " + deed + " (" + a[2] + ") in " + getName(player) + "'s inventory" : "deed failed: " + a[2];
        }
        if (a[1].equals("park") && a.length >= 6)
        {
            String template = a[2];
            float x = Float.parseFloat(a[3]), z = Float.parseFloat(a[4]), heading = Float.parseFloat(a[5]);
            script.library.vessel.Config c = script.library.vessel.getConfig(template);
            if (c == null)
            {
                return "no titan_vessels row for " + template;
            }
            location pad = new location(x, getHeightAtLocation(x, z), z, scene, null);
            float[] pose = script.library.vessel.landedPose(c, pad, heading);
            obj_id ship = createObject(template, new location(pose[0], pose[1], pose[2], scene, null));
            if (!isIdValid(ship))
            {
                return "park failed: " + template;
            }
            script.library.vessel.fly(ship, new script.library.vessel.Path().key(0.0f, pose[0], pose[1], pose[2], pose[3], pose[4], 0.0f), null);
            script.library.vessel.initParts(ship, true);
            script.library.vessel.setState(ship, script.library.vessel.STATE_PARKED);
            return "parked " + ship + " " + template + " at " + pose[0] + " " + pose[1] + " " + pose[2] + " heading " + pose[3];
        }
        if (a[1].equals("parkcell") && a.length >= 8)
        {
            // parked inside another pob's cell (a hangar): cell-space floor point and heading
            obj_id cell = obj_id.getObjId(Long.parseLong(a[3]));
            obj_id ship = script.library.vessel.parkInCell(a[2], cell, Float.parseFloat(a[4]), Float.parseFloat(a[5]), Float.parseFloat(a[6]), Float.parseFloat(a[7]));
            return isIdValid(ship) ? "parked " + ship + " " + a[2] + " in cell " + cell + " at " + getLocation(ship) : "park failed: " + a[2] + " in cell " + a[3];
        }
        if (a[1].equals("parkat") && a.length >= 4)
        {
            // parked where an object (a player standing in a hangar cell) is: in its cell on the floor it stands on, else on
            // the terrain; heading defaults to the object's
            obj_id at = obj_id.getObjId(Long.parseLong(a[3]));
            if (!isIdValid(at) || !exists(at))
            {
                return "no object " + a[3] + " on " + scene;
            }
            location here = getLocation(at);
            float heading = a.length >= 5 ? Float.parseFloat(a[4]) : getYaw(at);
            if (!isIdValid(here.cell))
            {
                return vessel(scene + " park " + a[2] + " " + here.x + " " + here.z + " " + heading);
            }
            obj_id ship = script.library.vessel.parkInCell(a[2], here.cell, here.x, here.y, here.z, heading);
            return isIdValid(ship) ? "parked " + ship + " " + a[2] + " in cell " + here.cell + " at " + getLocation(ship) : "park failed: " + a[2] + " in cell " + here.cell;
        }
        if (a.length < 3)
        {
            return "usage: vessel <scene> " + a[1] + " <oid>";
        }
        if (a[1].equals("retick"))
        {
            // restart every vessel's tick loop on this scene (vessel_controller.vesselTick: a new generation
            // supersedes the old loop, so a live one is not doubled)
            obj_id[] all = getObjectsInRange(new location(0.0f, 0.0f, 0.0f, scene, null), 16384.0f);
            int n = 0;
            for (int i = 0; all != null && i < all.length; ++i)
                if (script.library.vessel.isVessel(all[i]))
                {
                    messageTo(all[i], "vesselTick", null, rand(0.1f, 1.0f), false);
                    ++n;
                }
            return "retick sent to " + n + " vessels on " + scene;
        }
        if (a[1].equals("warp") && a.length >= 5)
        {
            // a test character to a world point on this scene: vessel <scene> warp <firstname|oid> <x> <z>
            obj_id player = a[2].matches("\\d+") ? obj_id.getObjId(Long.parseLong(a[2])) : getPlayerIdFromFirstName(a[2].toLowerCase());
            if (!isIdValid(player) || !exists(player))
                return "no player " + a[2] + " on " + scene;
            float x = Float.parseFloat(a[3]), z = Float.parseFloat(a[4]);
            setLocation(player, new location(x, getHeightAtLocation(x, z), z, scene, null));
            return "warped " + player + " to " + x + ", " + z;
        }
        if (a[1].equals("paint") && a.length >= 5)
        {
            // what the Paint radial's colour picker sets: vessel <scene> paint <oid> <hull|trim> <palette index>
            obj_id ship = obj_id.getObjId(Long.parseLong(a[2]));
            if (!script.library.vessel.isVessel(ship))
                return "no vessel " + a[2] + " on " + scene;
            String var = a[3].equals("trim") ? script.library.hue.INDEX_2 : script.library.hue.INDEX_1;
            script.library.hue.setColor(ship, var, Integer.parseInt(a[4]));
            return "painted " + ship + " " + var + " = " + script.library.hue.getVarColorIndex(ship, var);
        }
        if (a[1].equals("who") && a.length >= 3)
        {
            // a signed-in character and the ship it is aboard: where, autopilot, landing / starport state
            obj_id player = getPlayerIdFromFirstName(a[2].toLowerCase());
            if (!isIdValid(player) || !exists(player))
                return "no player " + a[2] + " on " + scene;
            StringBuilder sb = new StringBuilder(a[2] + " " + player + " at " + getLocation(player) + (isGod(player) ? " (god)" : ""));
            obj_id ship = script.library.space_transition.getContainingShip(player);
            if (isIdValid(ship))
            {
                sb.append("\n  aboard ").append(ship).append(" ").append(getTemplateName(ship)).append(" at ").append(getLocation(ship))
                  .append(" owner ").append(getOwner(ship)).append(" autopilot ").append(shipIsAutopilotActive(ship))
                  .append(" phase ").append(shipGetAutopilotPhase(ship)).append(" path ").append(shipGetFlightPathElapsed(ship))
                  .append("/").append(shipGetFlightPathDuration(ship));
                for (String ov : new String[]{"atmo.landing.target", "atmo.landing.landed_at", "atmo.landing.docked", "atmo.starport.bay", "atmo.starport.control"})
                    if (hasObjVar(ship, ov))
                        sb.append("\n  ").append(ov).append(" = ").append(getObjVar(ship, ov).getData());
            }
            sb.append("\n  credits ").append(script.library.money.getTotalMoney(player));
            return sb.toString();
        }
        if ((a[1].equals("grant") || a[1].equals("send")) && a.length >= 4)
        {
            // live tests with a signed-in character (by first name): grant <name> <ship> gives an owned ship and its
            // control device (in atmospheric flight it launches at once), send <name> <x> <z> [y] moves it there
            obj_id player = getPlayerIdFromFirstName(a[2].toLowerCase());
            if (!isIdValid(player) || !exists(player))
                return "no player " + a[2] + " on " + scene;
            if (a[1].equals("grant"))
            {
                // ship creation needs an owner script (it launches and pilots): run it on the player, by message
                if (!hasScript(player, "titan.dev_console"))
                    attachScript(player, "titan.dev_console");
                script.dictionary d = new script.dictionary();
                d.put("ship", a[3]);
                messageTo(player, "devGrantShip", d, 1.0f, false);
                return "granting " + a[2] + " a " + a[3] + " (it launches in atmospheric flight)";
            }
            // send <name> <x> <z> [y] [planet]: another planet's y defaults to 0 (the client settles onto the ground)
            float x = Float.parseFloat(a[3]), z = Float.parseFloat(a[4]);
            String to = a.length >= 7 ? a[6] : scene;
            float y = a.length >= 6 ? Float.parseFloat(a[5]) : (to.equals(scene) ? getHeightAtLocation(x, z) : 0.0f);
            warpPlayer(player, to, x, y, z, null, 0.0f, 0.0f, 0.0f);
            return "sent " + a[2] + " to " + to + " " + x + " " + y + " " + z;
        }
        obj_id obj = obj_id.getObjId(Long.parseLong(a[2]));
        if (!isIdValid(obj) || !exists(obj))
        {
            return "no object " + a[2] + " on " + scene;
        }
        if (a[1].equals("status"))
        {
            return describeVessel(obj);
        }
        if (a[1].equals("traffic"))
        {
            // add traffic control to a starport initialized before it existed (its base objects keep it)
            String point = script.library.travel.getTravelPointName(obj);
            obj_id control = script.space.atmo.starport_traffic.create(obj, point != null ? point : "Starport");
            if (!isIdValid(control))
                return "no landing well / bays for " + getTemplateName(obj);
            if (hasObjVar(obj, script.library.travel.VAR_BASE_OBJECT))
            {
                java.util.Vector list = new java.util.Vector(java.util.Arrays.asList(getObjIdArrayObjVar(obj, script.library.travel.VAR_BASE_OBJECT)));
                list.add(control);
                setObjVar(obj, script.library.travel.VAR_BASE_OBJECT, list);
            }
            return "traffic control " + control + " at " + getLocation(control);
        }
        if (a[1].equals("bays"))
        {
            // starport traffic control: every bay, who holds it, and its lounge's access
            obj_id st = hasObjVar(obj, script.space.atmo.starport_traffic.OV_STRUCTURE) ? getObjIdObjVar(obj, script.space.atmo.starport_traffic.OV_STRUCTURE) : null;
            StringBuilder sb = new StringBuilder("traffic " + obj + " free " + script.space.atmo.starport_traffic.freeBays(obj));
            for (script.space.atmo.starport_traffic.Bay b : script.space.atmo.starport_traffic.getBays(st))
            {
                String key = String.valueOf(b.row);
                obj_id ship = hasObjVar(obj, script.space.atmo.starport_traffic.OV_SHIP + key) ? getObjIdObjVar(obj, script.space.atmo.starport_traffic.OV_SHIP + key) : null;
                obj_id cell = getCellId(st, b.lounge);
                String[] allowed = isIdValid(cell) ? permissionsGetAllowed(cell) : null;
                if (ship == null && !b.reserved && (allowed == null || allowed.length == 0) && isIdValid(cell) && !permissionsIsPublic(cell))
                    continue;           // free and locked: the usual case
                sb.append("\n  bay ").append(b.bay).append(" L").append(b.level).append(" ").append(b.kind).append(b.reserved ? " reserved" : "")
                  .append(" ship=").append(ship).append(" state=").append(hasObjVar(obj, script.space.atmo.starport_traffic.OV_STATE + key) ? getIntObjVar(obj, script.space.atmo.starport_traffic.OV_STATE + key) : 0)
                  .append(" lounge ").append(!isIdValid(cell) ? "?" : permissionsIsPublic(cell) ? "public" : "private")
                  .append(allowed != null && allowed.length > 0 ? " allowed " + String.join(",", allowed) : "");
            }
            return sb.toString();
        }
        if ((a[1].equals("dock") || a[1].equals("undock")) && a.length >= 4)
        {
            // starport traffic control (space.atmo.starport_traffic): land a ship in a bay without a pilot, or fly it out
            obj_id ship = obj_id.getObjId(Long.parseLong(a[3]));
            if (!isIdValid(ship) || !exists(ship))
                return "no ship " + a[3];
            script.dictionary d = new script.dictionary();
            d.put("ship", ship);
            messageTo(obj, a[1].equals("dock") ? "trafficDock" : "trafficUndock", d, 1.0f, false);
            return a[1] + " sent to " + obj + " for " + ship;
        }
        if (a[1].equals("starport") && a.length >= 4)
        {
            // the travel point name is the rest up to an optional trailing cost
            int cost = 100;
            int end = a.length;
            if (a.length >= 5 && a[a.length - 1].matches("\\d+"))
            {
                cost = Integer.parseInt(a[a.length - 1]);
                end = a.length - 1;
            }
            String point = String.join(" ", java.util.Arrays.copyOfRange(a, 3, end));
            // the starport's own script initializes it (initializeStarport logs, which needs an owner script):
            // its retry handler, told this is the last try. Run the command again to see what it created.
            boolean first = !hasObjVar(obj, script.library.travel.VAR_BASE_OBJECT);
            if (first)
            {
                setObjVar(obj, script.library.travel.VAR_TRAVEL_POINT_NAME, point);
                setObjVar(obj, script.library.travel.VAR_TRAVEL_COST, cost);
                script.dictionary d = new script.dictionary();
                d.put("number_of_retry", 4);
                messageTo(obj, "retryInitializeStarport", d, 1.0f, false);
            }
            StringBuilder sb = new StringBuilder("starport " + obj + " '" + point + "' " + (first ? "initializing" : "objects:"));
            if (hasObjVar(obj, script.library.travel.VAR_BASE_OBJECT))
                for (obj_id o : getObjIdArrayObjVar(obj, script.library.travel.VAR_BASE_OBJECT))
                    sb.append("\n  ").append(o).append(" ").append(getTemplateName(o)).append(" at ").append(getLocation(o))
                      .append(hasObjVar(o, "vessel.service.well") ? " well" : "");
            return sb.toString();
        }
        if (a[1].equals("cabin"))
        {
            // the interior buildout: what each cell holds; the cabin screen's channel
            StringBuilder sb = new StringBuilder();
            String[] cells = getCellNames(obj);
            for (int i = 0; cells != null && i < cells.length; ++i)
            {
                obj_id cell = getCellId(obj, cells[i]);
                obj_id[] contents = isIdValid(cell) ? getContents(cell) : null;
                sb.append(cells[i]).append(": ").append(contents == null ? 0 : contents.length).append(" objects");
                java.util.TreeMap counts = new java.util.TreeMap();
                int speakers = 0, linked = 0;
                for (int j = 0; contents != null && j < contents.length; ++j)
                {
                    String t = getTemplateName(contents[j]);
                    t = t == null ? "?" : t.substring(t.lastIndexOf('/') + 1);
                    counts.put(t, (counts.containsKey(t) ? (Integer)counts.get(t) : 0) + 1);
                    if (t.startsWith("frn_titan_veh_ceiling_speaker"))
                    {
                        ++speakers;
                        if (hasObjVar(contents[j], "video_emitter.parent_id"))
                            ++linked;
                    }
                    if (hasScript(contents[j], "space.vessel.cabin_media"))
                    {
                        sb.append(" | screen ").append(contents[j]).append(" playing ").append(hasCondition(contents[j], CONDITION_MAGIC_VIDEO_PLAYER))
                          .append(" ").append(hasObjVar(contents[j], "stream.url") ? getStringObjVar(contents[j], "stream.url") : "-");
                    }
                }
                if (!counts.isEmpty())
                    sb.append(" | ").append(counts);
                if (speakers > 0)
                    sb.append(" | speakers linked ").append(linked).append("/").append(speakers);
                sb.append("\n");
            }
            return sb.toString();
        }
        script.dictionary d = new script.dictionary();
        obj_id ship = hasObjVar(obj, "vessel.service.current") ? getObjIdObjVar(obj, "vessel.service.current") : null;
        if (isIdValid(ship))
        {
            d.put("vessel", ship);
        }
        if (a[1].equals("depart"))
        {
            messageTo(obj, "serviceDepart", d, 0.1f, false);
        }
        else if (a[1].equals("arrive"))
        {
            messageTo(obj, "serviceNextArrival", null, 0.1f, false);
        }
        else if (a[1].equals("ramp") || a[1].equals("takeoff") || a[1].equals("land"))
        {
            String handler = a[1].equals("ramp") ? "vesselRamp" : a[1].equals("takeoff") ? "vesselTakeOff" : "vesselLandHere";
            messageTo(obj, handler, null, 0.1f, false);
            return a[1] + " sent to " + obj + ": " + describeVessel(obj);
        }
        else if (a[1].equals("restart"))
        {
            script.library.utils.removeScriptVar(obj, "vessel.service.started");
            messageTo(obj, "serviceStart", null, 0.1f, false);
        }
        else
        {
            return "unknown vessel command " + a[1];
        }
        return a[1] + " sent to " + obj + " (vessel " + ship + ")";
    }

    // the route checks of travel.purchaseTicket (everything before the player's money and inventory)
    private String ticketCheck(String planet1, String point1, String planet2, String point2) throws InterruptedException
    {
        if (planet1.equals(planet2) && point1.equals(point2))
        {
            return "same_spot";
        }
        if (!planet1.equals(planet2) && !getPlanetTravelPointInterplanetary(planet1, point1))
        {
            return "shuttle_interplanet_fail";
        }
        int planetCost = getPlanetTravelCost(planet1, planet2);
        int cost1 = getPlanetTravelPointCost(planet1, point1);
        int cost2 = getPlanetTravelPointCost(planet2, point2);
        if (planetCost == 0)
        {
            return "route_invalid";
        }
        if (cost1 == 0)
        {
            return "departure_invalid";
        }
        if (cost2 == 0)
        {
            return "destination_invalid";
        }
        return "ok " + (planetCost + cost1 + cost2) + " cr";
    }

    private String describeVessel(obj_id ship) throws InterruptedException
    {
        String flight = hasObjVar(ship, "vessel.flight") ? getStringObjVar(ship, "vessel.flight") : "-";
        return "vessel " + ship + " " + getTemplateName(ship) + " flight " + flight + " state " + script.library.vessel.getState(ship)
            + " at " + getLocation(ship) + " yaw " + script.library.vessel.getVesselYaw(ship)
            + " path " + shipGetFlightPathElapsed(ship) + "/" + shipGetFlightPathDuration(ship)
            + " ramp " + (script.library.vessel.isRampOpen(ship) ? "down" : "up") + " gear " + (script.library.vessel.isGearDown(ship) ? "down" : "up")
            + " pax " + script.library.vessel.getPassengerIds(ship).length + " aboard " + script.library.vessel.getPlayersAboard(ship).length;
    }
}
