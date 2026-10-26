package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/**
 * Heroic: The Battle of Naboo, as told by Jar Jar Binks - the encounter controller on the instance egg (with
 * systems.instance.instance_manager and systems.dungeon_sequencer.sequence_controller).
 *
 * Phases (contract section 1): 1 Da Grand Army, 2 Da Droid Army, 3 Da Shield Falls, 4 Da Tank, 5 Da Big Boom,
 * 6 epilogue. Spawns go through the sequencer table datatables/spawning/heroic/heroic_naboo_battle (triggers named
 * pN_*); this script narrates, counts, advances phases, signals the ground quest, pays out and resets a phase when
 * the whole group is down at once.
 *
 * GM dry run without players: messageTo the egg "hrc_nbooGmAdvance" (no params) ends the current phase.
 */
public class naboo_battle_controller extends script.base_script
{
    public naboo_battle_controller()
    {
    }

    public static final String SESSION = "hrc_nboo";
    public static final float TICK = 5.0f;
    public static final int P2_KILLS = 30;
    public static final int P2_SECONDS = 300;
    public static final int P4_TANKS = 3;
    public static final int HOLDOUT_SECONDS = 60;
    public static final int RALLY_SECONDS = 45;      // phase 1: Tarpals gives the order when the group reaches him, or after this
    public static final int DRAIN_SECONDS = 90;      // phase 3: generator 1 starts failing on its own after this
    public static final int TOKEN_INDEX = 22;        // trial.HEROIC_TOKENS: item_heroic_token_naboo_battle_01_01
    public static final int TOKENS_BASE = 3;

    public static final String[] SIGNALS = { "hrc_nboo_signal_enter", "hrc_nboo_signal_p1", "hrc_nboo_signal_p2", "hrc_nboo_signal_p3",
        "hrc_nboo_signal_p4", "hrc_nboo_signal_p5", "hrc_nboo_signal_epilogue" };

    // objvars on the egg (other scripts read phase / dome / rallied through naboo_battle_lib)
    public static final String OV = "hrc_nboo.";

    // ------------------------------------------------------------------ lifecycle

    public int OnAttach(obj_id self) throws InterruptedException
    {
        resetState(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        resetState(self);
        return SCRIPT_CONTINUE;
    }

    /** sequence_controller.beginSpawn spawns the default rows after 5 s (the army, generators, catapults). */
    public int beginSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        resetState(self);
        dictionary d = session(self);
        messageTo(self, "hrc_nbooStartPhase", withPhase(d, 1), 12.0f, false);
        messageTo(self, "hrc_nbooTick", session(self), TICK, false);
        return SCRIPT_CONTINUE;
    }

    public int cleanupSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        resetState(self);
        return SCRIPT_CONTINUE;
    }

    public void resetState(obj_id self) throws InterruptedException
    {
        trial.bumpSession(self, SESSION);
        naboo_battle_lib.clearRally(self);
        removeObjVar(self, "hrc_nboo");
        setObjVar(self, naboo_battle_lib.OV_PHASE, 0);
        setObjVar(self, naboo_battle_lib.OV_DOME_UP, 0);
        setObjVar(self, naboo_battle_lib.OV_RALLIED, 0);
    }

    public dictionary session(obj_id self) throws InterruptedException
    {
        return trial.getSessionDict(self, SESSION);
    }

    public boolean live(obj_id self, dictionary params) throws InterruptedException
    {
        return params != null && trial.verifySession(self, params, SESSION);
    }

    public dictionary withPhase(dictionary d, int phase)
    {
        d.put("phase", phase);
        return d;
    }

    public int phase(obj_id self) throws InterruptedException
    {
        return naboo_battle_lib.getPhase(self);
    }

    public int iv(obj_id self, String name) throws InterruptedException
    {
        return hasObjVar(self, OV + name) ? getIntObjVar(self, OV + name) : 0;
    }

    public void setIv(obj_id self, String name, int value) throws InterruptedException
    {
        setObjVar(self, OV + name, value);
    }

    /** Run a handler later, only if the encounter has not been reset meanwhile. */
    public void later(obj_id self, String handler, float delay) throws InterruptedException
    {
        messageTo(self, handler, session(self), delay, false);
    }

    public void sayLater(obj_id self, String key, float delay, float duration) throws InterruptedException
    {
        dictionary d = session(self);
        d.put("key", key);
        d.put("duration", duration);
        d.put("phase", phase(self));
        messageTo(self, "hrc_nbooNarrate", d, delay, false);
    }

    public int hrc_nbooNarrate(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params) || params.getInt("phase") != phase(self))
            return SCRIPT_CONTINUE;
        naboo_battle_lib.narrate(self, params.getString("key"), params.getFloat("duration"));
        return SCRIPT_CONTINUE;
    }

    public void signalAll(obj_id self, int step) throws InterruptedException
    {
        obj_id[] players = naboo_battle_lib.getPlayers(self);
        if (players.length > 0)
            groundquests.sendSignal(players, SIGNALS[step]);
    }

    // ------------------------------------------------------------------ heartbeat: rally, timers, waves, wipe check

    public int hrc_nbooTick(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params))
            return SCRIPT_CONTINUE;
        int p = phase(self);
        int now = getGameTime();
        int elapsed = now - iv(self, "phase_start");
        obj_id[] players = naboo_battle_lib.getPlayers(self);
        if (p >= 1 && p <= 5 && iv(self, "resetting") == 0)
        {
            checkWipe(self, players);
            switch (p)
            {
                case 1:
                    // after Jar Jar's intro and "go see Tarpals": standing by him (or 75 s) gets the order
                    if (iv(self, "rallied") == 0 && elapsed >= 15 && (elapsed > RALLY_SECONDS || anyPlayerNear(self, players, "tarpals", 20.0f)))
                        rally(self);
                    break;
                case 2:
                    if (elapsed >= P2_SECONDS)
                        endPhase2(self);
                    else if (elapsed - iv(self, "last_squad") >= 45 && elapsed > 60)
                    {
                        setIv(self, "last_squad", elapsed);
                        if (aliveDroids(self) < 30)
                        {
                            int n = iv(self, "squad") + 1;
                            setIv(self, "squad", n);
                            naboo_battle_lib.fireTrigger(self, (n % 2) == 1 ? "p2_squad_a" : "p2_squad_b");
                        }
                    }
                    break;
                case 3:
                    if (elapsed >= DRAIN_SECONDS && iv(self, "drain") == 0)
                    {
                        setIv(self, "drain", 1);
                        obj_id g1 = naboo_battle_lib.firstSpawned(self, "gen_1");
                        if (isIdValid(g1))
                            messageTo(g1, "hrc_nbooStartDrain", null, 0.0f, false);
                    }
                    if (elapsed - iv(self, "last_wave") >= 50 && elapsed > 40)
                    {
                        setIv(self, "last_wave", elapsed);
                        if (aliveDroids(self) < 30)
                            naboo_battle_lib.fireTrigger(self, "p3_wave");
                    }
                    break;
                case 5:
                    if (iv(self, "holdout_end") > 0)
                    {
                        int left = iv(self, "holdout_end") - now;
                        if (left > 0 && (left / 30) != iv(self, "holdout_mark") && left % 30 < (int)TICK)
                        {
                            setIv(self, "holdout_mark", left / 30);
                            naboo_battle_lib.messageAll(self, "sys_holdout", left);
                        }
                        if (now - iv(self, "last_wave") >= 30)
                        {
                            setIv(self, "last_wave", now);
                            if (aliveDroids(self) < 30)
                                naboo_battle_lib.fireTrigger(self, "p5_wave");
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        messageTo(self, "hrc_nbooTick", params, TICK, false);
        return SCRIPT_CONTINUE;
    }

    public boolean anyPlayerNear(obj_id self, obj_id[] players, String spawnId, float range) throws InterruptedException
    {
        obj_id who = naboo_battle_lib.firstSpawned(self, spawnId);
        if (!isIdValid(who))
            return false;
        location l = getLocation(who);
        for (obj_id p : players)
            if (getDistance(getLocation(p), l) < range)
                return true;
        return false;
    }

    public int aliveDroids(obj_id self) throws InterruptedException
    {
        int n = 0;
        for (obj_id o : naboo_battle_lib.children(self, "socialGroup"))
            if (naboo_battle_lib.isDroid(o) && naboo_battle_lib.isAlive(o))
                ++n;
        return n;
    }

    /** Everyone in the instance down at the same time on two ticks in a row: the phase starts over. */
    public void checkWipe(obj_id self, obj_id[] players) throws InterruptedException
    {
        if (players.length == 0)
        {
            setIv(self, "wipe", 0);
            return;
        }
        for (obj_id p : players)
            if (!isIncapacitated(p) && !isDead(p))
            {
                setIv(self, "wipe", 0);
                return;
            }
        int w = iv(self, "wipe") + 1;
        setIv(self, "wipe", w);
        if (w >= 2)
            failPhase(self);
    }

    // ------------------------------------------------------------------ phase starts

    public int hrc_nbooStartPhase(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params))
            return SCRIPT_CONTINUE;
        int p = params.getInt("phase");
        // a start for a phase already reached is stale (the GM advance from phase 0, a doubled phase end); only the
        // wipe restart (failPhase sets "resetting") starts the current phase again. 11, 21... are "phase 1 done" etc.
        int reached = phase(self) >= 11 ? phase(self) / 10 : phase(self);
        if (reached > p || (reached == p && iv(self, "resetting") == 0))
            return SCRIPT_CONTINUE;
        // the story starts when the first player has arrived (a cross-planet warp takes a while), at most 3 min late
        if (p == 1 && naboo_battle_lib.getPlayers(self).length == 0 && params.getInt("wait") < 36)
        {
            params.put("wait", params.getInt("wait") + 1);
            messageTo(self, "hrc_nbooStartPhase", params, 5.0f, false);
            return SCRIPT_CONTINUE;
        }
        startPhase(self, p);
        return SCRIPT_CONTINUE;
    }

    public void startPhase(obj_id self, int p) throws InterruptedException
    {
        setObjVar(self, naboo_battle_lib.OV_PHASE, p);
        setIv(self, "phase_start", getGameTime());
        setIv(self, "resetting", 0);
        setIv(self, "wipe", 0);
        setIv(self, "last_squad", 0);
        setIv(self, "last_wave", 0);
        CustomerServiceLog("instance-" + naboo_battle_lib.INSTANCE_NAME, "Battle of Naboo (" + self + ") phase " + p + " started");
        switch (p)
        {
            case 1:
                setIv(self, "gens_on", 0);
                setIv(self, "rallied", 0);
                setObjVar(self, naboo_battle_lib.OV_RALLIED, 0);
                naboo_battle_lib.narrate(self, "p1_intro", 10.0f);
                sayLater(self, "p1_rally", 11.0f, 7.0f);
                naboo_battle_lib.setRally(self, naboo_battle_lib.spotsOf(self, new String[]{ "tarpals" }), "obj_rally", -1);
                syncAllPlayers(self);
                messageTo(self, "hrc_nbooPhaseTrigger", trigger(self, "p1_scouts", 1), 50.0f, false);
                break;
            case 2:
                setIv(self, "kills", 0);
                setIv(self, "squad", 0);
                naboo_battle_lib.narrate(self, "p2_intro", 10.0f);
                naboo_battle_lib.setRally(self, naboo_battle_lib.linePoint(self, 0.0f, 0.0f), "obj_line", P2_KILLS);
                naboo_battle_lib.fireTrigger(self, "p2_tanks");
                messageTo(self, "hrc_nbooPhaseTrigger", trigger(self, "p2_mtts", 2), 15.0f, false);
                break;
            case 3:
                setIv(self, "drain", 0);
                naboo_battle_lib.narrate(self, "p3_intro", 8.0f);
                sayLater(self, "p3_task", 9.0f, 7.0f);
                naboo_battle_lib.setRally(self, generatorSpots(self, false), "obj_defend", -1);
                naboo_battle_lib.fireTrigger(self, "p3_assault");
                break;
            case 4:
                setIv(self, "tanks", 0);
                // the bombardment tanks of phase 2 pull back; the three that roll in now are the ones that count
                for (obj_id old : naboo_battle_lib.spawned(self, "p2_aat"))
                    trial.cleanupObject(old);
                naboo_battle_lib.narrate(self, "p4_intro", 10.0f);
                naboo_battle_lib.setRally(self, new location[]{ naboo_battle_lib.offset(self, naboo_battle_lib.CAPTURED_AAT[0], naboo_battle_lib.CAPTURED_AAT[1]),
                    naboo_battle_lib.offset(self, naboo_battle_lib.CATAPULTS[0][0], naboo_battle_lib.CATAPULTS[0][1]),
                    naboo_battle_lib.offset(self, naboo_battle_lib.CATAPULTS[1][0], naboo_battle_lib.CATAPULTS[1][1]) }, "obj_tank", P4_TANKS);
                if (iv(self, "captured_spawned") == 0)
                {
                    setIv(self, "captured_spawned", 1);
                    naboo_battle_lib.fireTrigger(self, "p4_captured");
                }
                messageTo(self, "hrc_nbooPhaseTrigger", trigger(self, "p4_enemies", 4), 20.0f, false);
                sayLater(self, "p4_task", 11.0f, 8.0f);
                sayLater(self, "p4_catapult", 26.0f, 6.0f);
                break;
            case 5:
                setIv(self, "holdout_end", 0);
                setIv(self, "holdout_mark", -1);
                naboo_battle_lib.narrate(self, "p5_intro", 11.0f);
                naboo_battle_lib.setRally(self, naboo_battle_lib.linePoint(self, 0.0f, 40.0f), "obj_boss", -1);
                naboo_battle_lib.fireTrigger(self, "p5_boss");
                sayLater(self, "p5_boss", 14.0f, 6.0f);
                break;
            default:
                break;
        }
    }

    public dictionary trigger(obj_id self, String name, int p) throws InterruptedException
    {
        dictionary d = session(self);
        d.put("trigger", name);
        d.put("phase", p);
        return d;
    }

    public int hrc_nbooPhaseTrigger(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params) || params.getInt("phase") != phase(self) || iv(self, "resetting") != 0)
            return SCRIPT_CONTINUE;
        naboo_battle_lib.fireTrigger(self, params.getString("trigger"));
        if ("p1_scouts".equals(params.getString("trigger")))
            sayLater(self, "p1_scouts", 6.0f, 6.0f);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ phase 1: rally and generators

    public void rally(obj_id self) throws InterruptedException
    {
        setIv(self, "rallied", 1);
        setObjVar(self, naboo_battle_lib.OV_RALLIED, 1);
        naboo_battle_lib.narrate(self, "p1_task", 9.0f);
        rallyGenerators(self);
    }

    /** Markers on the generators not yet running (phase 1). */
    public void rallyGenerators(obj_id self) throws InterruptedException
    {
        location[] l = generatorSpots(self, true);
        naboo_battle_lib.setRally(self, l, "obj_generators", l.length);
    }

    /** Where to stand at each generator: 12 m on the camp side of the fambaa (inside the 18 m calibrate range; a
     *  marker at the fambaa's centre hid its emblem in the beast). onlyOff = skip the ones already running. */
    public location[] generatorSpots(obj_id self, boolean onlyOff) throws InterruptedException
    {
        java.util.Vector spots = new java.util.Vector();
        for (int g = 1; g <= 3; ++g)
        {
            obj_id gen = naboo_battle_lib.firstSpawned(self, "gen_" + g);
            if (!isIdValid(gen) || (onlyOff && hasObjVar(gen, "hrc_nboo.on") && getIntObjVar(gen, "hrc_nboo.on") != 0))
                continue;
            location at = getLocation(gen);
            at.x -= 12.0f;
            spots.add(at);
        }
        location[] l = new location[spots.size()];
        spots.toArray(l);
        return l;
    }

    public int hrc_nbooGeneratorOn(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) != 1)
            return SCRIPT_CONTINUE;
        int gen = params.getInt("gen");
        int mask = iv(self, "gen_mask");
        if ((mask & (1 << gen)) != 0)
            return SCRIPT_CONTINUE;
        mask |= 1 << gen;
        setIv(self, "gen_mask", mask);
        int on = Integer.bitCount(mask);
        setIv(self, "gens_on", on);
        if (on < 3)
        {
            if (iv(self, "rallied") != 0)
                rallyGenerators(self);
            if (on == 2)
                naboo_battle_lib.narrate(self, "p1_generator_last", 5.0f);
            else
                naboo_battle_lib.narrate(self, "p1_generator", 5.0f, 3 - on);
            return SCRIPT_CONTINUE;
        }
        // all three running: the shield rises
        setObjVar(self, naboo_battle_lib.OV_PHASE, 11);     // between phases: generators stay on, no wipe reset
        naboo_battle_lib.clearRally(self);
        naboo_battle_lib.fireTrigger(self, "dome_up");
        setObjVar(self, naboo_battle_lib.OV_DOME_UP, 1);
        sayLater(self, "p1_done", 5.0f, 8.0f);
        signalAll(self, 1);
        messageTo(self, "hrc_nbooStartPhase", withPhase(session(self), 2), 18.0f, false);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ phase 2: the droid army

    public int hrc_nbooMttLanded(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) == 2 && iv(self, "said_mtt") == 0)
        {
            setIv(self, "said_mtt", 1);
            naboo_battle_lib.narrate(self, "p2_mtt", 8.0f);
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDroidsDeployed(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) == 2 && iv(self, "said_deploy") == 0)
        {
            setIv(self, "said_deploy", 1);
            naboo_battle_lib.narrate(self, "p2_task", 5.0f);
            sayLater(self, "p2_unfold", 5.5f, 4.0f);
            naboo_battle_lib.setRally(self, new location[]{ naboo_battle_lib.offset(self, naboo_battle_lib.MTT_DROP[0][0], naboo_battle_lib.MTT_DROP[0][1]),
                naboo_battle_lib.offset(self, naboo_battle_lib.MTT_DROP[1][0], naboo_battle_lib.MTT_DROP[1][1]) }, "obj_unfold", -1);
            later(self, "hrc_nbooRallyLine", 25.0f);
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooRallyLine(obj_id self, dictionary params) throws InterruptedException
    {
        if (live(self, params) && phase(self) == 2)
            naboo_battle_lib.setRally(self, naboo_battle_lib.linePoint(self, 0.0f, 0.0f), "obj_line", Math.max(0, P2_KILLS - iv(self, "kills")));
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDroidInsideShield(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) == 2 && iv(self, "said_march") == 0)
        {
            setIv(self, "said_march", 1);
            naboo_battle_lib.narrate(self, "p2_march", 7.0f);
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooDroidDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) != 2 || iv(self, "resetting") != 0)
            return SCRIPT_CONTINUE;
        int k = iv(self, "kills") + 1;
        setIv(self, "kills", k);
        if (k % 10 == 0 && k < P2_KILLS)
            naboo_battle_lib.messageAll(self, "sys_droids_destroyed", k);
        if (k >= P2_KILLS)
            endPhase2(self);
        return SCRIPT_CONTINUE;
    }

    public void endPhase2(obj_id self) throws InterruptedException
    {
        if (phase(self) != 2)
            return;
        setObjVar(self, naboo_battle_lib.OV_PHASE, 21);
        naboo_battle_lib.clearRally(self);
        naboo_battle_lib.narrate(self, "p2_done", 8.0f);
        signalAll(self, 2);
        messageTo(self, "hrc_nbooStartPhase", withPhase(session(self), 3), 12.0f, false);
    }

    // ------------------------------------------------------------------ phase 3: the generators fall

    public int hrc_nbooGeneratorHit(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) != 3)
            return SCRIPT_CONTINUE;
        int now = getGameTime();
        if (now - iv(self, "said_hit") > 45)
        {
            setIv(self, "said_hit", now);
            naboo_battle_lib.narrate(self, "p3_gen_hit", 4.0f);
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooGeneratorDown(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) != 3 || params.getInt("gen") != 1)
            return SCRIPT_CONTINUE;
        setObjVar(self, naboo_battle_lib.OV_PHASE, 31);
        naboo_battle_lib.clearRally(self);
        setObjVar(self, naboo_battle_lib.OV_DOME_UP, 0);
        obj_id dome = naboo_battle_lib.firstSpawned(self, "dome");
        if (isIdValid(dome))
            messageTo(dome, "hrc_nbooDomeFall", null, 0.0f, false);
        for (int g = 2; g <= 3; ++g)
        {
            obj_id gen = naboo_battle_lib.firstSpawned(self, "gen_" + g);
            if (isIdValid(gen))
                messageTo(gen, "hrc_nbooShieldLost", null, 0.0f, false);
        }
        naboo_battle_lib.narrate(self, "p3_fall", 7.0f);
        dictionary d = session(self);
        d.put("key", "p3_tanks");
        d.put("duration", 6.0f);
        d.put("phase", 31);
        messageTo(self, "hrc_nbooNarrate", d, 8.0f, false);
        signalAll(self, 3);
        messageTo(self, "hrc_nbooStartPhase", withPhase(session(self), 4), 15.0f, false);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ phase 4: the captured AAT

    public int hrc_nbooGunnerSeated(obj_id self, dictionary params) throws InterruptedException
    {
        if (iv(self, "said_gunner") == 0)
        {
            setIv(self, "said_gunner", 1);
            naboo_battle_lib.narrate(self, "p4_gunner", 5.0f);
        }
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooAatDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        if (params.getInt("command") != 0)
        {
            if (phase(self) == 5 && iv(self, "holdout_end") == 0)
                startHoldout(self, params.getLocation("loc"));
            return SCRIPT_CONTINUE;
        }
        if (phase(self) != 4 || params.getInt("counted") == 0 || iv(self, "resetting") != 0)
            return SCRIPT_CONTINUE;
        int t = iv(self, "tanks") + 1;
        setIv(self, "tanks", t);
        naboo_battle_lib.narrate(self, "p4_tank_down", 4.0f);
        naboo_battle_lib.messageAll(self, "sys_tanks_destroyed", t);
        if (t >= P4_TANKS)
        {
            setObjVar(self, naboo_battle_lib.OV_PHASE, 41);
            naboo_battle_lib.clearRally(self);
            signalAll(self, 4);
            messageTo(self, "hrc_nbooStartPhase", withPhase(session(self), 5), 10.0f, false);
        }
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ phase 5: the commander and the big boom

    public void startHoldout(obj_id self, location wreck) throws InterruptedException
    {
        int now = getGameTime();
        setIv(self, "holdout_end", now + HOLDOUT_SECONDS);
        setIv(self, "last_wave", now);
        naboo_battle_lib.setRally(self, naboo_battle_lib.linePoint(self, 0.0f, 0.0f), "obj_holdout", HOLDOUT_SECONDS);
        // OOM-9 bails out of the burning tank
        if (wreck != null)
        {
            location l = naboo_battle_lib.groundAt(wreck);
            l.x += 4.0f;
            obj_id commander = create.object("hrc_nboo_b1_commander", l);
            if (isIdValid(commander))
            {
                naboo_battle_lib.adopt(self, commander, "p5_commander");
                setObjVar(commander, "hrc_nboo.attack", "line");
                attachScript(commander, naboo_battle_lib.SCRIPT_AI_CONTROLLER);
            }
        }
        dictionary d = session(self);
        d.put("phase", 5);
        messageTo(self, "hrc_nbooControlShip", d, HOLDOUT_SECONDS, false);
    }

    public int hrc_nbooCommanderDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        if (phase(self) == 5)
            setIv(self, "commander_dead", 1);
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooControlShip(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params) || phase(self) != 5)
            return SCRIPT_CONTINUE;
        setObjVar(self, naboo_battle_lib.OV_PHASE, 51);
        naboo_battle_lib.clearRally(self);
        naboo_battle_lib.narrate(self, "p5_control_ship", 9.0f);
        obj_id[] players = naboo_battle_lib.getPlayers(self);
        location sky = skyLocation(self, players);
        if (players.length > 0)
            playClientEffectLoc(players, naboo_battle_lib.FX_BIG_EXPLOSION, sky, 0.0f);
        later(self, "hrc_nbooShutdown", 6.0f);
        return SCRIPT_CONTINUE;
    }

    /** High in the sky to the north-east of the group (an explosion far out would be culled). */
    public location skyLocation(obj_id self, obj_id[] players) throws InterruptedException
    {
        float x = 0.0f, z = 0.0f;
        if (players.length > 0)
        {
            for (obj_id p : players)
            {
                location l = getLocation(p);
                x += l.x;
                z += l.z;
            }
            x /= players.length;
            z /= players.length;
        }
        else
        {
            location c = getLocation(self);
            x = c.x;
            z = c.z;
        }
        location c = getLocation(self);
        return new location(x + 450.0f, getHeightAtLocation(x, z) + 380.0f, z + 450.0f, c.area);
    }

    public int hrc_nbooShutdown(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params))
            return SCRIPT_CONTINUE;
        for (obj_id o : naboo_battle_lib.children(self, "socialGroup"))
            if (naboo_battle_lib.isDroid(o))
                messageTo(o, "hrc_nbooPowerDown", null, rand(0.0f, 2.5f), false);
        dictionary d = session(self);
        d.put("key", "p5_shutdown");
        d.put("duration", 7.0f);
        d.put("phase", 51);
        messageTo(self, "hrc_nbooNarrate", d, 3.0f, false);
        d = session(self);
        d.put("key", "p5_victory");
        d.put("duration", 7.0f);
        d.put("phase", 51);
        messageTo(self, "hrc_nbooNarrate", d, 11.0f, false);
        later(self, "hrc_nbooEpilogue", 19.0f);
        signalAll(self, 5);
        return SCRIPT_CONTINUE;
    }

    // ------------------------------------------------------------------ epilogue and rewards

    public int hrc_nbooEpilogue(obj_id self, dictionary params) throws InterruptedException
    {
        if (!live(self, params) || phase(self) != 51)
            return SCRIPT_CONTINUE;
        setObjVar(self, naboo_battle_lib.OV_PHASE, 6);
        naboo_battle_lib.narrate(self, "epilogue_thanks", 9.0f);
        obj_id[] players = naboo_battle_lib.getPlayers(self);
        int tokens = TOKENS_BASE + iv(self, "commander_dead") + (iv(self, "wipes") == 0 ? 1 : 0);
        if (players.length > 0)
        {
            dictionary dict = new dictionary();
            dict.put("tokenIndex", TOKEN_INDEX);
            dict.put("tokenCount", tokens);
            utils.messageTo(players, "handleAwardtoken", dict, 0, false);
            groundquests.sendSignal(players, SIGNALS[6]);
        }
        logVictory(self, players, tokens);
        instance.setClock(self, 300);
        return SCRIPT_CONTINUE;
    }

    public void logVictory(obj_id self, obj_id[] players, int tokens) throws InterruptedException
    {
        String log = "instance-" + naboo_battle_lib.INSTANCE_NAME;
        String when = getCalendarTimeStringLocal(getCalendarTime());
        obj_id group = players.length > 0 ? getGroupObject(players[0]) : null;
        CustomerServiceLog(log, "Battle of Naboo won in instance (" + self + ") by group (" + group + ") at " + when + ", wipes " + iv(self, "wipes") + ", tokens " + tokens);
        for (obj_id p : players)
            CustomerServiceLog(log, "  member " + getFirstName(p) + " (" + p + ") " + skill.getProfessionName(getSkillTemplate(p)));
    }

    // ------------------------------------------------------------------ failure and reset

    public void failPhase(obj_id self) throws InterruptedException
    {
        int p = phase(self);
        if (p < 1 || p > 5 || iv(self, "resetting") != 0)
            return;
        setIv(self, "resetting", 1);
        setIv(self, "wipes", iv(self, "wipes") + 1);
        CustomerServiceLog("instance-" + naboo_battle_lib.INSTANCE_NAME, "Battle of Naboo (" + self + ") wiped in phase " + p);
        naboo_battle_lib.narrate(self, "fail", 7.0f);
        naboo_battle_lib.messageAll(self, "sys_phase_reset", -1);
        naboo_battle_lib.fireTrigger(self, "p" + p + "_reset");
        if (p == 1)
        {
            setIv(self, "gen_mask", 0);
            setIv(self, "gens_on", 0);
        }
        for (int g = 1; g <= 3 && (p == 1 || p == 3); ++g)
        {
            obj_id gen = naboo_battle_lib.firstSpawned(self, "gen_" + g);
            if (isIdValid(gen))
                messageTo(gen, "hrc_nbooResetGenerator", null, 0.0f, false);
        }
        if (p == 5)
            setIv(self, "holdout_end", 0);
        trial.bumpSession(self, SESSION);       // drops the old phase's pending timers and narration
        messageTo(self, "hrc_nbooTick", session(self), TICK, false);
        messageTo(self, "hrc_nbooStartPhase", withPhase(session(self), p), 25.0f, false);
    }

    // ------------------------------------------------------------------ ground quest sync (late join, login)

    public void syncAllPlayers(obj_id self) throws InterruptedException
    {
        for (obj_id p : naboo_battle_lib.getPlayers(self))
            syncPlayer(self, p);
    }

    public void syncPlayer(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
            return;
        if (!groundquests.isQuestActive(player, naboo_battle_lib.QUEST))
            groundquests.requestGrantQuest(player, naboo_battle_lib.QUEST);
        naboo_battle_lib.syncRally(self, player);
        dictionary d = new dictionary();
        d.put("player", player);
        messageTo(self, "hrc_nbooSendQuestSignals", d, 4.0f, false);
    }

    public int hrc_nbooRequestQuestSync(obj_id self, dictionary params) throws InterruptedException
    {
        syncPlayer(self, params.getObjId("player"));
        return SCRIPT_CONTINUE;
    }

    public int hrc_nbooSendQuestSignals(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        if (!isIdValid(player) || !exists(player))
            return SCRIPT_CONTINUE;
        int p = phase(self);
        int done = completedSteps(p);
        for (int i = 0; i <= done && i < SIGNALS.length; ++i)
            groundquests.sendSignal(player, SIGNALS[i]);
        return SCRIPT_CONTINUE;
    }

    /** Index of the last quest signal already earned for a phase value (between-phase values are 11, 21, ...). */
    public int completedSteps(int p)
    {
        if (p >= 51 || p == 6)
            return p == 6 ? 6 : 5;
        if (p >= 11)
            return p / 10;
        return Math.max(0, p - 1);
    }

    // ------------------------------------------------------------------ GM helpers

    /** GM dry run: ends the current phase as if its goal had been met. */
    public int hrc_nbooGmAdvance(obj_id self, dictionary params) throws InterruptedException
    {
        int p = phase(self);
        CustomerServiceLog("instance-" + naboo_battle_lib.INSTANCE_NAME, "Battle of Naboo (" + self + ") GM advance from phase " + p);
        switch (p)
        {
            case 0:
                startPhase(self, 1);
                break;
            case 1:
                for (int g = 1; g <= 3; ++g)
                {
                    dictionary d = new dictionary();
                    d.put("gen", g);
                    messageTo(self, "hrc_nbooGeneratorOn", d, 0.0f, false);
                }
                break;
            case 2:
                endPhase2(self);
                break;
            case 3:
            {
                dictionary d = new dictionary();
                d.put("gen", 1);
                messageTo(self, "hrc_nbooGeneratorDown", d, 0.0f, false);
                break;
            }
            case 4:
                setIv(self, "tanks", P4_TANKS - 1);
                dictionary d = new dictionary();
                d.put("counted", 1);
                messageTo(self, "hrc_nbooAatDestroyed", d, 0.0f, false);
                break;
            case 5:
                if (iv(self, "holdout_end") == 0)
                    startHoldout(self, null);
                else
                {
                    dictionary s = session(self);
                    s.put("phase", 5);
                    messageTo(self, "hrc_nbooControlShip", s, 0.0f, false);
                }
                break;
            default:
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws InterruptedException
    {
        if (isIdValid(speaker) && isGod(speaker) && text != null && text.equals("nboo advance"))
            messageTo(self, "hrc_nbooGmAdvance", null, 0.0f, false);
        return SCRIPT_CONTINUE;
    }
}
