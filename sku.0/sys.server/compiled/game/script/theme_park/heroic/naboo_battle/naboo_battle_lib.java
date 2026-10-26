package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

import java.util.Vector;

/**
 * Shared constants and helpers for the Battle of Naboo heroic ("as told by Jar Jar Binks").
 * Contract: docs/content/battle-of-naboo/README.md. Positions are offsets from the controller egg at world (0, 0)
 * on adventure_heroic tile 05; heights always come from the terrain.
 */
public class naboo_battle_lib extends script.base_script
{
    public naboo_battle_lib()
    {
    }

    public static final String STF = "theme_park/heroic_naboo_battle";
    public static final String INSTANCE_NAME = "heroic_naboo_battle";
    public static final String QUEST = "heroic_naboo_battle";
    public static final String SCRIPT_CONTROLLER = "theme_park.heroic.naboo_battle.naboo_battle_controller";
    public static final String SCRIPT_DROID = "theme_park.heroic.naboo_battle.droid_unit";
    public static final String SCRIPT_FOLDED = "theme_park.heroic.naboo_battle.folded_droid";
    public static final String SCRIPT_AI_CONTROLLER = "systems.dungeon_sequencer.ai_controller";
    public static final String JAR_JAR_TEMPLATE = "object/mobile/titan/heroic/naboo/jar_jar_binks.iff";

    // client projectiles (shared weapon templates; their weaponEffect rows draw the shells)
    public static final String PLASMA_SHELL = "object/weapon/ranged/vehicle/shared_titan_hrc_nboo_plasma_shell.iff";
    public static final String BOOMA = "object/weapon/ranged/vehicle/shared_titan_hrc_booma.iff";
    public static final String MTT_BLASTER = "object/weapon/ranged/vehicle/shared_titan_hrc_mtt_blaster.iff";

    public static final String FX_SHELL_IMPACT = "clienteffect/combat_explosion_lair_large.cef";
    public static final String FX_DOME_HIT = "clienteffect/cbt_hit_shield_ion_cannon_ion.cef";
    public static final String FX_BOOMA_IMPACT = "clienteffect/avatar_wke_electric_01.cef";
    public static final String FX_BIG_EXPLOSION = "clienteffect/restuss_event_big_explosion.cef";

    public static final String TURRET_APPEARANCE = "appearance/titan_hrc_nboo_aat_turret.apt";
    public static final String STAP_HULL = "appearance/titan_hrc_nboo_stap_hull.apt";
    public static final String SHIELD_HIT_APPEARANCE = "appearance/titan_hrc_nboo_shield_hit.apt";
    public static final String SHIELD_HIT_PARTICLE = "appearance/pt_droideka_shield.prt";

    // controller objvars (read by every encounter script)
    public static final String OV_PHASE = "hrc_nboo.phase";
    public static final String OV_DOME_UP = "hrc_nboo.dome_up";
    public static final String OV_RALLIED = "hrc_nboo.rallied";

    // world layout, offsets from the controller (contract section 2; generated rows: examples/naboo_battle_layout.py)
    public static final float DOME_X = -60.0f;
    public static final float DOME_Z = 0.0f;
    public static final float DOME_R = 90.0f;
    public static final float DOME_SQUASH = 0.82f;              // the dome is DOME_R * 0.82 high (naboo_battle.shield_dome)
    public static final float LINE_R = DOME_R - 12.0f;          // the Gungan line stands on this arc round the dome centre
    public static final float[][] GENERATORS = { { -94.0f, 0.0f }, { -56.0f, 40.0f }, { -56.0f, -40.0f } };
    public static final float[] CAMP = { -112.0f, 0.0f };
    public static final float[] CAPTURED_AAT = { -24.0f, -30.0f };
    public static final float[][] CATAPULTS = { { -12.0f, 58.0f }, { -12.0f, -58.0f } };
    // where the folded droids land: 22-40 m west of each MTT pad (150, +-44), mtt.DEPLOY_AHEAD
    public static final float[][] MTT_DROP = { { 118.0f, -44.0f }, { 118.0f, 44.0f } };

    public static final String SOCIAL_DROID = "hrc_nboo_droid";
    public static final String SOCIAL_GUNGAN = "hrc_nboo_gungan";

    // the AAT turret: an hp_dyn part on hp_turret, rot about Y, state = yaw / 360 (client clamps state to 0..1)
    public static final String TURRET_SLOT = "hp_dyn.turret.";
    public static final float TURRET_DEG_PER_SEC = 60.0f;
    /** Flip to -1 if the turret turns the wrong way in the client (the exporter mirrors X). */
    public static final float TURRET_YAW_SIGN = 1.0f;

    public static final float SHELL_SPEED = 70.0f;
    public static final float SHELL_SPLASH = 6.0f;

    // ------------------------------------------------------------------ controller / locations

    public static obj_id getController(obj_id obj) throws InterruptedException
    {
        if (!isIdValid(obj) || !exists(obj))
            return null;
        if (hasScript(obj, SCRIPT_CONTROLLER))
            return obj;
        obj_id parent = trial.getParent(obj);
        if (isIdValid(parent) && parent != obj && exists(parent) && hasScript(parent, SCRIPT_CONTROLLER))
            return parent;
        obj_id area = instance.getAreaInstanceController(obj);
        if (isIdValid(area) && exists(area) && hasScript(area, SCRIPT_CONTROLLER))
            return area;
        return null;
    }

    public static int getPhase(obj_id controller) throws InterruptedException
    {
        return isIdValid(controller) && hasObjVar(controller, OV_PHASE) ? getIntObjVar(controller, OV_PHASE) : 0;
    }

    public static boolean isDomeUp(obj_id controller) throws InterruptedException
    {
        return isIdValid(controller) && hasObjVar(controller, OV_DOME_UP) && getIntObjVar(controller, OV_DOME_UP) != 0;
    }

    /** World location at an offset from the controller, on the terrain. */
    public static location offset(obj_id controller, float dx, float dz) throws InterruptedException
    {
        location c = getLocation(controller);
        float x = c.x + dx;
        float z = c.z + dz;
        return new location(x, getHeightAtLocation(x, z), z, c.area);
    }

    public static location groundAt(location loc) throws InterruptedException
    {
        location l = (location)loc.clone();
        l.y = getHeightAtLocation(l.x, l.z);
        return l;
    }

    /** Put a freshly spawned outdoor object on the terrain (sequencer rows carry y offsets of 13 m). */
    public static void snapToGround(obj_id obj, float lift) throws InterruptedException
    {
        location loc = getLocation(obj);
        if (loc == null || isIdValid(loc.cell))
            return;
        float h = getHeightAtLocation(loc.x, loc.z) + lift;
        if (Math.abs(loc.y - h) > 0.05f)
        {
            loc.y = h;
            setLocation(obj, loc);
        }
    }

    public static float distance2d(location a, location b)
    {
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        return (float)Math.sqrt(dx * dx + dz * dz);
    }

    /** Heading in degrees (0 = +z, 90 = +x) from a to b. */
    public static float headingTo(location a, location b)
    {
        float h = (float)Math.toDegrees(Math.atan2(b.x - a.x, b.z - a.z));
        return h < 0.0f ? h + 360.0f : h;
    }

    public static location domeCentre(obj_id controller) throws InterruptedException
    {
        return offset(controller, DOME_X, DOME_Z);
    }

    public static boolean isInsideDome(obj_id controller, location loc) throws InterruptedException
    {
        return isDomeUp(controller) && distance2d(loc, domeCentre(controller)) < DOME_R - 2.0f;
    }

    /** A point on the dome surface facing `from`, a little scattered (for bombardment hits). */
    public static location domePointFacing(obj_id controller, location from) throws InterruptedException
    {
        location c = domeCentre(controller);
        double az = Math.atan2(from.x - c.x, from.z - c.z) + Math.toRadians(rand(-28.0f, 28.0f));
        double el = Math.toRadians(rand(12.0f, 55.0f));
        float r = DOME_R * (float)Math.cos(el);
        return new location(c.x + r * (float)Math.sin(az), c.y + DOME_R * DOME_SQUASH * (float)Math.sin(el), c.z + r * (float)Math.cos(az), c.area);
    }

    /** The Gungan line arc at z (offset from the controller); out > 0 = that far east of it, outside the dome. */
    public static location linePoint(obj_id controller, float z, float out) throws InterruptedException
    {
        float r = LINE_R + out;
        float zz = Math.max(-r, Math.min(r, z));
        return offset(controller, DOME_X + (float)Math.sqrt(r * r - zz * zz), zz);
    }

    /** Where a straight shot from `from` to `to` first meets the dome (the closest surface point if it is inside). */
    public static location domeEntryPoint(obj_id controller, location from, location to) throws InterruptedException
    {
        location c = domeCentre(controller);
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        // the dome is an ellipsoid (squashed in y): intersect in the space where it is a sphere
        dy /= DOME_SQUASH;
        double fx = from.x - c.x, fy = (from.y - c.y) / DOME_SQUASH, fz = from.z - c.z;
        double a = dx * dx + dy * dy + dz * dz;
        double b = 2.0 * (fx * dx + fy * dy + fz * dz);
        double cc = fx * fx + fy * fy + fz * fz - DOME_R * DOME_R;
        double disc = b * b - 4.0 * a * cc;
        if (a < 0.001 || disc < 0.0 || cc < 0.0)
            return domePointFacing(controller, from);
        double t = (-b - Math.sqrt(disc)) / (2.0 * a);
        if (t < 0.0 || t > 1.0)
            return domePointFacing(controller, from);
        return new location((float)(from.x + dx * t), (float)Math.max(c.y + 1.0, from.y + dy * DOME_SQUASH * t), (float)(from.z + dz * t), c.area);
    }

    // ------------------------------------------------------------------ players, narration, messages

    public static obj_id[] getPlayers(obj_id controller) throws InterruptedException
    {
        if (!isIdValid(controller) || !exists(controller))
            return new obj_id[0];
        obj_id[] players = instance.getPlayersInInstanceArea(controller);
        return players != null ? players : new obj_id[0];
    }

    public static string_id sid(String key)
    {
        return new string_id(STF, key);
    }

    /** Jar Jar in the comm window (his head) plus a chat copy. `di` < 0 = no number in the line. */
    public static void narrate(obj_id controller, String key, float duration, int di) throws InterruptedException
    {
        obj_id[] players = getPlayers(controller);
        if (players.length == 0)
            return;
        prose_package pp = prose.getPackage(sid(key));
        if (di >= 0)
            prose.setDI(pp, di);
        commPlayers(controller, JAR_JAR_TEMPLATE, null, duration, players, pp);
        prose_package chatLine;
        if (di >= 0)
            chatLine = pp;
        else
        {
            chatLine = prose.getPackage(sid("chat_prefix"));
            prose.setTO(chatLine, sid(key));
        }
        for (obj_id player : players)
            sendSystemMessageProse(player, chatLine);
    }

    public static void narrate(obj_id controller, String key, float duration) throws InterruptedException
    {
        narrate(controller, key, duration, -1);
    }

    public static void message(obj_id player, String key) throws InterruptedException
    {
        if (isIdValid(player))
            sendSystemMessage(player, sid(key));
    }

    public static void messageAll(obj_id controller, String key, int di) throws InterruptedException
    {
        obj_id[] players = getPlayers(controller);
        prose_package pp = prose.getPackage(sid(key));
        if (di >= 0)
            prose.setDI(pp, di);
        for (obj_id player : players)
            sendSystemMessageProse(player, pp);
    }

    // ------------------------------------------------------------------ rally points: where the narration sends the group

    public static final String RALLY_TEMPLATE = "object/tangible/titan/heroic/naboo/rally_marker.iff";
    public static final String OV_RALLY_LOC = "hrc_nboo.rally.loc";
    public static final String OV_RALLY_KEY = "hrc_nboo.rally.key";
    public static final String SV_WAYPOINT = "hrc_nboo.waypoint";
    public static final String SV_RALLY_MARKERS = "hrc_nboo.rally_markers";

    /**
     * Mark the current objective: a gold light shaft with the Historical "H" emblem on each spot (instance children,
     * so a reset removes them), an active waypoint for every player on the first spot, and the objective line `key`
     * (with %DI = di when di >= 0) as a system message. Late joiners get the same from syncRally.
     */
    public static void setRally(obj_id controller, location[] spots, String key, int di) throws InterruptedException
    {
        clearRally(controller);
        Vector markers = new Vector();
        for (location spot : spots)
        {
            if (spot == null)
                continue;
            obj_id marker = create.object(RALLY_TEMPLATE, groundAt(spot));
            if (!isIdValid(marker))
                continue;
            adopt(controller, marker, "rally");
            utils.addElement(markers, marker);
        }
        // the controller keeps its own list: the sequencer's child list can be missing (a reset in progress)
        if (!markers.isEmpty())
            utils.setScriptVar(controller, SV_RALLY_MARKERS, markers);
        if (spots.length > 0 && spots[0] != null)
        {
            setObjVar(controller, OV_RALLY_LOC, groundAt(spots[0]));
            setObjVar(controller, OV_RALLY_KEY, key);
        }
        for (obj_id player : getPlayers(controller))
            syncRally(controller, player);
        messageAll(controller, key, di);
    }

    public static void setRally(obj_id controller, location spot, String key, int di) throws InterruptedException
    {
        setRally(controller, new location[]{ spot }, key, di);
    }

    /** Markers at whatever is still spawned under these spawn ids (a generator not yet running, the captured AAT). */
    public static location[] spotsOf(obj_id controller, String[] spawnIds) throws InterruptedException
    {
        Vector out = new Vector();
        for (String id : spawnIds)
            for (obj_id o : spawned(controller, id))
                if (isAlive(o) || !isMob(o))
                    out.add(getLocation(o));
        location[] result = new location[out.size()];
        out.toArray(result);
        return result;
    }

    public static void clearRally(obj_id controller) throws InterruptedException
    {
        for (obj_id m : spawned(controller, "rally"))
            trial.cleanupObject(m);
        if (utils.hasScriptVar(controller, SV_RALLY_MARKERS))
        {
            obj_id[] mine = utils.getObjIdArrayScriptVar(controller, SV_RALLY_MARKERS);
            utils.removeScriptVar(controller, SV_RALLY_MARKERS);
            if (mine != null)
                for (obj_id m : mine)
                    if (isIdValid(m) && exists(m))
                        destroyObject(m);
        }
        removeObjVar(controller, "hrc_nboo.rally");
        for (obj_id player : getPlayers(controller))
            clearWaypoint(player);
    }

    /** The player's waypoint follows the current rally point (called on rally changes, late join and login). */
    public static void syncRally(obj_id controller, obj_id player) throws InterruptedException
    {
        clearWaypoint(player);
        if (!hasObjVar(controller, OV_RALLY_LOC) || !isIdValid(player) || !exists(player))
            return;
        location loc = getLocationObjVar(controller, OV_RALLY_LOC);
        obj_id wp = createWaypointInDatapad(player, loc);
        if (!isIdValid(wp))
            return;
        setWaypointName(wp, "@" + STF + ":wp_" + getStringObjVar(controller, OV_RALLY_KEY));
        setWaypointColor(wp, "yellow");
        setWaypointVisible(wp, true);
        setWaypointActive(wp, true);
        utils.setScriptVar(player, SV_WAYPOINT, wp);
    }

    public static void clearWaypoint(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player) || !utils.hasScriptVar(player, SV_WAYPOINT))
            return;
        obj_id wp = utils.getObjIdScriptVar(player, SV_WAYPOINT);
        utils.removeScriptVar(player, SV_WAYPOINT);
        if (isIdValid(wp))
            destroyWaypointInDatapad(wp, player);
    }

    // ------------------------------------------------------------------ sides

    public static String socialGroup(obj_id o) throws InterruptedException
    {
        return hasObjVar(o, "socialGroup") ? getStringObjVar(o, "socialGroup") : "";
    }

    public static boolean isAlive(obj_id o) throws InterruptedException
    {
        return isIdValid(o) && exists(o) && !isDead(o) && !isIncapacitated(o);
    }

    public static boolean isDroid(obj_id o) throws InterruptedException
    {
        return isIdValid(o) && exists(o) && isMob(o) && !isPlayer(o) && SOCIAL_DROID.equals(socialGroup(o));
    }

    public static boolean isGungan(obj_id o) throws InterruptedException
    {
        return isIdValid(o) && exists(o) && isMob(o) && !isPlayer(o) && SOCIAL_GUNGAN.equals(socialGroup(o));
    }

    /** Enemies of the Gungan side: living droids (not shut down). */
    public static obj_id[] droidsInRange(location loc, float range) throws InterruptedException
    {
        Vector out = new Vector();
        obj_id[] all = getCreaturesInRange(loc, range);
        if (all != null)
            for (obj_id o : all)
                if (isDroid(o) && isAlive(o) && !isInvulnerable(o))
                    out.add(o);
        obj_id[] result = new obj_id[out.size()];
        out.toArray(result);
        return result;
    }

    /** Enemies of the droid side: players and Gungans (generators included). */
    public static obj_id[] alliesInRange(location loc, float range) throws InterruptedException
    {
        Vector out = new Vector();
        obj_id[] all = getCreaturesInRange(loc, range);
        if (all != null)
            for (obj_id o : all)
                if ((isPlayer(o) || isGungan(o)) && isAlive(o) && !isInvulnerable(o))
                    out.add(o);
        obj_id[] result = new obj_id[out.size()];
        out.toArray(result);
        return result;
    }

    public static obj_id nearest(obj_id[] list, location from) throws InterruptedException
    {
        obj_id best = null;
        float bestD = Float.MAX_VALUE;
        if (list == null)
            return null;
        for (obj_id o : list)
        {
            float d = getDistance(from, getLocation(o));
            if (d < bestD)
            {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ AAT turret

    public static void initTurret(obj_id aat) throws InterruptedException
    {
        setObjVar(aat, TURRET_SLOT + "kind", "part");
        setObjVar(aat, TURRET_SLOT + "hp", "hp_turret");
        setObjVar(aat, TURRET_SLOT + "path", TURRET_APPEARANCE);
        setObjVar(aat, TURRET_SLOT + "mode", "rot");
        setObjVar(aat, TURRET_SLOT + "ax", 0.0f);
        setObjVar(aat, TURRET_SLOT + "ay", 1.0f);
        setObjVar(aat, TURRET_SLOT + "az", 0.0f);
        setObjVar(aat, TURRET_SLOT + "amount", 360.0f);
        setObjVar(aat, TURRET_SLOT + "seconds", 1.0f);
        setObjVar(aat, TURRET_SLOT + "delay", 0.0f);
        setObjVar(aat, TURRET_SLOT + "state", 0.0f);
    }

    /** Turn the turret toward `target`; returns the traverse time in seconds. */
    public static float aimTurret(obj_id aat, location target) throws InterruptedException
    {
        if (!isIdValid(aat) || target == null)
            return 0.0f;
        float rel = (headingTo(getLocation(aat), target) - getYaw(aat)) * TURRET_YAW_SIGN;
        rel = rel % 360.0f;
        if (rel < 0.0f)
            rel += 360.0f;
        float state = rel / 360.0f;
        float old = hasObjVar(aat, TURRET_SLOT + "state") ? getFloatObjVar(aat, TURRET_SLOT + "state") : 0.0f;
        float seconds = Math.max(0.2f, Math.abs(state - old) * 360.0f / TURRET_DEG_PER_SEC);
        if (Math.abs(state - old) < 0.003f)
            return 0.0f;
        setObjVar(aat, TURRET_SLOT + "seconds", seconds);
        setObjVar(aat, TURRET_SLOT + "state", state);
        return seconds;
    }

    /** Forward point of the turret gun, `range` metres out (for shots without a target). */
    public static location turretForward(obj_id aat, float range) throws InterruptedException
    {
        float state = hasObjVar(aat, TURRET_SLOT + "state") ? getFloatObjVar(aat, TURRET_SLOT + "state") : 0.0f;
        double h = Math.toRadians(getYaw(aat) + state * 360.0f * TURRET_YAW_SIGN);
        location l = getLocation(aat);
        return groundAt(new location(l.x + range * (float)Math.sin(h), l.y, l.z + range * (float)Math.cos(h), l.area));
    }

    // ------------------------------------------------------------------ projectiles and splash

    /** A player close to `near` whose observers will all see a client projectile (projectiles are sent per client). */
    public static obj_id projectileViewer(obj_id controller, location near) throws InterruptedException
    {
        return nearest(getPlayers(controller), near);
    }

    /** Fire a client-side shell from a hardpoint; returns the flight time in seconds (0 if nobody can see it). */
    public static float fireProjectile(obj_id controller, String weapon, obj_id source, String hardpoint, location target, float speed) throws InterruptedException
    {
        obj_id viewer = projectileViewer(controller, target);
        float flight = getDistance(getLocation(source), target) / speed;
        if (isIdValid(viewer))
            createClientProjectileObjectToLocation(viewer, weapon, source, hardpoint, target, speed, flight + 1.0f, false, 0, 0, 0, 0);
        return flight;
    }

    public static void playEffect(obj_id controller, String effect, location loc) throws InterruptedException
    {
        obj_id[] players = getPlayers(controller);
        if (players.length > 0)
            playClientEffectLoc(players, effect, loc, 0.0f);
    }

    /**
     * Splash damage around `centre`: full damage at the centre, half at the edge. `droidSide` = the shell was fired
     * by the droids (hurts players and Gungans, blocked by the raised dome), else by the Gungan side (hurts droids).
     * `credit`: the player who fired, put on each victim's hate list.
     */
    public static int splashDamage(obj_id controller, location centre, float radius, int damage, boolean droidSide, obj_id credit) throws InterruptedException
    {
        if (droidSide && isInsideDome(controller, centre))
            return 0;
        obj_id[] victims = droidSide ? alliesInRange(centre, radius) : droidsInRange(centre, radius);
        int hits = 0;
        for (obj_id v : victims)
        {
            float d = getDistance(centre, getLocation(v));
            int amount = (int)(damage * (1.0f - 0.5f * Math.min(1.0f, d / radius)));
            if (amount <= 0)
                continue;
            damage(v, DAMAGE_BLAST, HIT_LOCATION_BODY, amount);
            if (isIdValid(credit) && exists(credit) && !droidSide)
                addHate(v, credit, amount);
            ++hits;
        }
        return hits;
    }

    // ------------------------------------------------------------------ registration with the sequencer

    /** Make a script-spawned object a child of the instance (cleanup, deleteSpawn and counting by spawn_id). */
    public static void adopt(obj_id controller, obj_id child, String spawnId) throws InterruptedException
    {
        if (!isIdValid(controller) || !isIdValid(child))
            return;
        setObjVar(child, "spawn_id", spawnId);
        trial.markAsTempObject(child, true);
        trial.setParent(controller, child, true);
        trial.setInterest(child);
        trial.registerObjectWithSequencer(controller, child);
    }

    /** Sequencer trigger by name (rows with this triggerId spawn / run). */
    public static void fireTrigger(obj_id controller, String triggerName) throws InterruptedException
    {
        dictionary d = trial.getSessionDict(controller);
        d.put("triggerType", "triggerId");
        d.put("triggerName", triggerName);
        messageTo(controller, "triggerFired", d, 0.0f, false);
    }

    /** trial's getObjectsInInstanceBy* throw until the sequencer has spawned something (no child array yet). */
    public static boolean hasChildren(obj_id controller) throws InterruptedException
    {
        obj_id parent = trial.getParent(controller);
        return isIdValid(parent) && utils.getDictionaryScriptVar(parent, trial.PROT_CHILD_ARRAY) != null;
    }

    /** Every live spawned child of the instance carrying `objvar`; never null. */
    public static obj_id[] children(obj_id controller, String objvar) throws InterruptedException
    {
        obj_id[] all = hasChildren(controller) ? trial.getSpawnedChildren(trial.getParent(controller)) : null;
        obj_id[] found = all != null ? trial.getObjectsInListWithObjVar(all, objvar) : null;
        return found != null ? found : new obj_id[0];
    }

    public static obj_id[] spawned(obj_id controller, String spawnId) throws InterruptedException
    {
        if (!hasChildren(controller))
            return new obj_id[0];
        obj_id[] list = trial.getObjectsInInstanceBySpawnId(controller, spawnId);
        if (list == null)
            return new obj_id[0];
        Vector out = new Vector();
        for (obj_id o : list)
            if (isIdValid(o) && exists(o))
                out.add(o);
        obj_id[] result = new obj_id[out.size()];
        out.toArray(result);
        return result;
    }

    public static obj_id firstSpawned(obj_id controller, String spawnId) throws InterruptedException
    {
        obj_id[] list = spawned(controller, spawnId);
        return list.length > 0 ? list[0] : null;
    }

    // ------------------------------------------------------------------ appearance helpers

    /** Droid markings from creatures.tab objvars hrc_nboo.tex1 / col1 / col2 (npc_droid_battle_droid.pal). */
    public static void paint(obj_id npc) throws InterruptedException
    {
        if (hasObjVar(npc, "hrc_nboo.tex1"))
            hue.setTexture(npc, 1, getIntObjVar(npc, "hrc_nboo.tex1"));
        if (hasObjVar(npc, "hrc_nboo.col1"))
            hue.setColor(npc, "/private/index_color_1", getIntObjVar(npc, "hrc_nboo.col1"));
        if (hasObjVar(npc, "hrc_nboo.col2"))
            hue.setColor(npc, "/private/index_color_2", getIntObjVar(npc, "hrc_nboo.col2"));
    }

    public static void setAppSlot(obj_id obj, String slot, String hardpoint, String path, float ox, float oy, float oz) throws InterruptedException
    {
        String root = "hp_dyn." + slot + ".";
        setObjVar(obj, root + "kind", "app");
        setObjVar(obj, root + "hp", hardpoint);
        setObjVar(obj, root + "path", path);
        setObjVar(obj, root + "ox", ox);
        setObjVar(obj, root + "oy", oy);
        setObjVar(obj, root + "oz", oz);
    }

    public static void setFxSlot(obj_id obj, String slot, String hardpoint, String path, float ox, float oy, float oz, float scale) throws InterruptedException
    {
        String root = "hp_dyn." + slot + ".";
        setObjVar(obj, root + "kind", "fx");
        setObjVar(obj, root + "hp", hardpoint);
        setObjVar(obj, root + "path", path);
        setObjVar(obj, root + "ox", ox);
        setObjVar(obj, root + "oy", oy);
        setObjVar(obj, root + "oz", oz);
        setObjVar(obj, root + "scale", scale);
    }

    public static void clearSlot(obj_id obj, String slot) throws InterruptedException
    {
        removeObjVar(obj, "hp_dyn." + slot);
    }
}
