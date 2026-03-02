package script.player;/*
@Origin: dsrc.script.player
@Author:  BubbaJoeX
@Purpose: Vehicle UI functions
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Tuesday, 6/4/2024, at 12:00 AM, 
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.

@TODO: Change the objvars to be under the "vehicle." objvar root.
    Figure out why the weapon won't fire. (most likely the geonosian weapon. :3 )
    Figure out a good way to hide the window when the player dismounts.
    Figure out a way to make the vehicle **properly** stop when the player uses the E-Brake.
*/

import script.combat_engine;
import script.dictionary;
import script.library.ai_lib;
import script.library.sui;
import script.library.vehicle;
import script.location;
import script.obj_id;
import script.string_id;

public class player_vehicle extends script.base_script
{
    public int THRUST_COOLDOWN = 180;
    public static boolean ENABLE_WEAPONS = true;
    public static boolean ENABLE_INFORMATION = true;
    public static String OBJVAR_VEHICLE_ROOT = "vehicle.";
    public static String OBJVAR_VEHICLE_LAST_THRUST = OBJVAR_VEHICLE_ROOT + "lastThrust";
    public static String OBJVAR_VEHICLE_LAST_FIRE = OBJVAR_VEHICLE_ROOT + "lastFire";
    public static String OBJVAR_VEHICLE_MY_VEHICLE = OBJVAR_VEHICLE_ROOT + "myCurrentVehicle";
    public static String OBJVAR_VEHICLE_PID = OBJVAR_VEHICLE_ROOT + "pid";


    public static int showVehicleSUI(obj_id who) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Entered /Script.vehicleHorn!");
        String page = "/Script.vehicleHorn";
        int suiPage = createSUIPage(page, who, who);
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "honkButton", "onVehicleHonk");
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "thrustButton", "onVehicleThrust");
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "stopButton", "onVehicleStop");
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "increaseButton", "onVehicleSpeedIncrease");
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "decreaseButton", "onVehicleStopDecrease");
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "fireButton", "onVehicleFire");
        subscribeToSUIEvent(suiPage, sui_event_type.SET_onButton, "infoButton", "onGetVehicleInfo");
        if (ENABLE_WEAPONS)
        {
            setSUIProperty(suiPage, "/Script.vehicleHorn.fireButton", "Visible", "true");
        }
        else if (!isGod(who))
        {
            setSUIProperty(suiPage, "/Script.vehicleHorn.fireButton", "Visible", "false");
        }
        if (ENABLE_INFORMATION)
        {
            setSUIProperty(suiPage, "/Script.vehicleHorn.infoButton", "Visible", "true");
        }
        else if (!isGod(who))
        {
            setSUIProperty(suiPage, "/Script.vehicleHorn.infoButton", "Visible", "false");
        }
        setSUIAssociatedObject(suiPage, who);
        boolean showResult = showSUIPage(suiPage);
        if (!showResult)
        {
            broadcast(who, "Cannot display UI page '/Script.vehicleHorn");
        }
        flushSUIPage(suiPage);
        sui.setPid(who, suiPage, "vehicleHornPid");
        setObjVar(who, "vehicleHorn.pid", suiPage);
        LOG("ethereal", "[Vehicle SUI]: Setting tracked PID \"vehicleHorn.pid\" to: " + suiPage);
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self)
    {
        if (!isPlayer(self))
        {
            return SCRIPT_CONTINUE;
        }
        messageTo(self, "handleStart", null, 1.0f, false);
        LOG("ethereal", "[Vehicle SUI]: Attached Vehicle SUI handler to " + getPlayerFullName(self) + "!");
        return SCRIPT_CONTINUE;
    }

    public int handleStart(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Script was attached to " + getPlayerFullName(self) + " and player is riding a vehicle!");
        obj_id vehicle = getMountId(self);
        LOG("ethereal", "[Vehicle SUI]: Vehicle obj_id for " + getPlayerFullName(self) + " is " + vehicle + "!");
        if (vehicle == null)
        {
            return SCRIPT_CONTINUE;
        }
        if (getMaster(vehicle) != self)
        {
            LOG("ethereal", "[Vehicle SUI]: Player " + getPlayerFullName(self) + " is not the master of the vehicle! Aborting.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "myCurrentVehicle", vehicle);
        LOG("ethereal", "[Vehicle SUI]: Setting objvar \"myCurrentVehicle\" to " + vehicle + " for " + getPlayerFullName(self) + "!");
        LOG("ethereal", "[Vehicle SUI]: Showing Vehicle SUI for " + getPlayerFullName(self) + "!");
        showVehicleSUI(self);
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "myCurrentVehicle");
        removeObjVar(self, "vehicleHorn.pid");
        handleVehicleDismount(self);// not working?
        sui.closeSUI(self, getIntObjVar(self, "vehicleHorn.pid"));
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public obj_id getVehicle(obj_id self) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Got vehicle obj_id for " + self + " as " + getObjIdObjVar(self, "myCurrentVehicle") + "!");
        obj_id vehicle = getObjIdObjVar(self, "myCurrentVehicle");
        if (vehicle == null)
        {
            handleVehicleDismount(self);
            LOG("ethereal", "[Vehicle SUI]: Vehicle obj_id for " + self + " is null! Most likely got stored or destroyed!");
            return null;
        }
        return vehicle;
    }

    public int onVehicleThrust(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Entered onVehicleThrust");
        int lastThrust = getIntObjVar(self, "vehicle.lastThrust");
        if (lastThrust + THRUST_COOLDOWN > getGameTime())
        {
            broadcast(self, "You must wait " + (lastThrust + THRUST_COOLDOWN - getGameTime()) + " seconds before you can maximize your thrusters again!");
            return SCRIPT_CONTINUE;
        }
        obj_id vehicleObj = getVehicle(self);
        if (vehicleObj == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        if (getState(self, STATE_RIDING_MOUNT) != 1)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        float currentSpeed = vehicle.getMaximumSpeed(vehicleObj);
        broadcast(self, "You have maximized thrust for a top speed of " + (currentSpeed + 15f) + " m/s");
        params.put("currentSpeed", currentSpeed);
        vehicle.setMaximumSpeed(vehicleObj, currentSpeed + 15f);
        messageTo(self, "resetSpeed", params, 60f, false);
        setObjVar(self, "vehicle.lastThrust", getGameTime());
        play2dNonLoopingSound(self, "sound/veh_engine_boost.snd");
        playClientEffectObj(self, "clienteffect/trap_electric_01.cef", self, "");

        return SCRIPT_CONTINUE;
    }

    public int onVehicleHonk(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Entered onVehicleHonk");
        playClientEffectObj(self, "sound/veh_horn.snd", getMountId(self), "");
        return SCRIPT_CONTINUE;
    }

    public int onAirspeederControl(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        obj_id vehicleObj = getMountId(self);
        if (!isIdValid(vehicleObj) || getState(self, STATE_RIDING_MOUNT) != 1)
            return SCRIPT_CONTINUE;
        if (!hasObjVar(vehicleObj, "airspeeder.panelRider") || getObjIdObjVar(vehicleObj, "airspeeder.panelRider") != self)
            return SCRIPT_CONTINUE;
        if (params == null || params.length() == 0)
            return SCRIPT_CONTINUE;
        String action = params.trim().toLowerCase();
        if (action.equals("skyway"))
        {
            if (hasObjVar(vehicleObj, "airspeeder.ascending"))
            {
                messageTo(vehicleObj, "cancelSkywayAscent", null, 0, false);
            }
            else if (hasObjVar(vehicleObj, "airspeeder.active"))
            {
                messageTo(vehicleObj, "startSkywayDescent", null, 0, false);
            }
            else
            {
                messageTo(vehicleObj, "startSkywayAscent", null, 0, false);
            }
        }
        else if (action.equals("boost"))
        {
            boolean enable = !hasObjVar(vehicleObj, "airspeeder.boostActive");
            vehicle.setBoostMode(vehicleObj, enable);
        }
        else if (action.equals("traffic"))
        {
            boolean enable = !hasObjVar(vehicleObj, "airspeeder.trafficActive");
            vehicle.setTrafficMode(vehicleObj, enable);
        }
        else if (action.equals("horn"))
        {
            int lastHorn = getIntObjVar(vehicleObj, "airspeeder.lastHorn");
            int now = getGameTime();
            if (now - lastHorn < 3)
                return SCRIPT_CONTINUE;
            setObjVar(vehicleObj, "airspeeder.lastHorn", now);
            obj_id[] nearby = getPlayerCreaturesInRange(vehicleObj, 128.0f);
            if (nearby != null && nearby.length > 0)
            {
                playClientEffectObj(nearby, "sound/veh_horn.snd", vehicleObj, "");
            }
            String[] hornQuips = {
                "Watch it!",
                "Hey, watch where you're going!",
                "Out of the way!",
                "Move it!",
                "Are you crazy?!",
                "Whoa! Easy there!",
                "Hey! I'm walkin' here!",
                "Learn to drive!"
            };
            obj_id[] npcs = getNPCsInRange(vehicleObj, 32.0f);
            if (npcs != null)
            {
                for (int i = 0; i < npcs.length; i++)
                {
                    if (!isIdValid(npcs[i]))
                        continue;
                    if (!hasScript(npcs[i], "ai.ai"))
                        continue;
                    if (ai_lib.isInCombat(npcs[i]))
                        continue;
                    if (hasCondition(npcs[i], CONDITION_INVULNERABLE) || hasCondition(npcs[i], CONDITION_CONVERSABLE))
                        continue;
                    setMovementRun(npcs[i]);
                    ai_lib.pathAwayFrom(npcs[i], vehicleObj);
                    if (ai_lib.isHumanoid(npcs[i]) && rand(1, 100) <= 30)
                    {
                        showFlyText(npcs[i], string_id.unlocalized(hornQuips[rand(0, hornQuips.length - 1)]), 1.5f, 255, 255, 255);
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int onAirspeederCrash(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        obj_id vehicleObj = getMountId(self);
        if (!isIdValid(vehicleObj) || getState(self, STATE_RIDING_MOUNT) != 1)
            return SCRIPT_CONTINUE;
        if (!hasObjVar(vehicleObj, "airspeeder.active"))
            return SCRIPT_CONTINUE;
        messageTo(vehicleObj, "handleSkywayCollision", null, 0, false);
        return SCRIPT_CONTINUE;
    }

    public int handleAutoPilotWaypoint(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id vehicleObj = getMountId(self);
        if (!isIdValid(vehicleObj) || getState(self, STATE_RIDING_MOUNT) != 1)
            return SCRIPT_CONTINUE;
        if (!hasObjVar(vehicleObj, "airspeeder.active"))
        {
            sendSystemMessageTestingOnly(self, "You must be in skyway mode to use auto-pilot.");
            return SCRIPT_CONTINUE;
        }
        float x = params.getFloat("x");
        float z = params.getFloat("z");
        dictionary wpParams = new dictionary();
        wpParams.put("x", x);
        wpParams.put("z", z);
        messageTo(vehicleObj, "addAutoPilotWaypoint", wpParams, 0, false);
        return SCRIPT_CONTINUE;
    }

    public int onAutoPilotCancel(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        obj_id vehicleObj = getMountId(self);
        if (!isIdValid(vehicleObj) || getState(self, STATE_RIDING_MOUNT) != 1)
            return SCRIPT_CONTINUE;
        messageTo(vehicleObj, "cancelAutoPilot", null, 0, false);
        return SCRIPT_CONTINUE;
    }

    public int onVehicleFire(obj_id self, dictionary params) throws InterruptedException
    {
        int lastFire = getIntObjVar(self, "vehicle.lastFire");
        if (lastFire + 2 > getGameTime() && !isGod(self))
        {
            broadcast(self, "Your weapons are still cooling down!");
            return SCRIPT_CONTINUE;
        }
        if (!ENABLE_WEAPONS)
        {
            broadcast(self, "Vehicle Arsenal is disabled!");
            return SCRIPT_CONTINUE;
        }
        obj_id sourceObject = getMountId(self);
        obj_id trueTarget = getIntendedTarget(self);
        if (sourceObject == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        if (getState(self, STATE_RIDING_MOUNT) != 1)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        if (trueTarget != null && trueTarget != sourceObject)
        {
            if (isPlayer(trueTarget) && !isGod(self))
            {
                return SCRIPT_CONTINUE;
            }
            createClientProjectileObjectToObject(self, "object/weapon/ranged/heavy/shared_heavy_lightning_beam.iff", sourceObject, "player", trueTarget, "", 275.0f, 7.0f, true, 201, 242, 155, 1);
            createClientProjectileObjectToObject(self, "object/weapon/ranged/heavy/shared_heavy_lightning_beam.iff", sourceObject, "player", trueTarget, "", 275.0f, 7.0f, true, 201, 242, 155, 1);
            playClientEffectObj(self, "sound/wep_pistol_geo_sonic_blaster.snd", sourceObject, "player");
            playClientEffectObj(self, "sound/wep_pistol_geo_sonic_blaster.snd", sourceObject, "player");
            if (!isInvulnerable(getIntendedTarget(self)) && getCondition(getIntendedTarget(self)) != CONDITION_CONVERSABLE)
            {
                damage(trueTarget, DAMAGE_BLAST, HIT_LOCATION_HEAD, rand(50, getMaxHealth(trueTarget)));
                /*combat_engine.hit_result hitResult = new combat_engine.hit_result();
                hitResult.hitLocation = HIT_LOCATION_BODY;
                hitResult.damage = rand(50, getMaxHealth(trueTarget) / 3);
                hitResult.miss = false;
                hitResult.rawDamage = hitResult.damage;
                if (doDamage(self, self, getCurrentWeapon(self), hitResult))
                {
                    hitResult.success = true;
                    LOG("ethereal", "[Vehicle SUI]: Successfully damaged target " + trueTarget + " (" + getCreatureName(trueTarget) + ") !");
                }
                else
                {
                    LOG("ethereal", "[Vehicle SUI]: Failed to damage target " + trueTarget + "!");
                }*/
                setObjVar(self, "vehicle.lastFire", getGameTime());
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int onVehicleStop(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id vehicleObj = getVehicle(self);
        if (vehicleObj == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        if (getState(self, STATE_RIDING_MOUNT) != 1)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        LOG("ethereal", "[Vehicle SUI]: Entered onVehicleStop");
        float originalDecel = vehicle.getDecel(vehicleObj);
        location stoppingPoint = getLocation(self);
        setState(self, STATE_FROZEN, true);
        vehicle.setDecel(vehicleObj, 1000f);
        dictionary params2 = new dictionary();
        params2.put("originalDecel", originalDecel);
        setLocation(self, stoppingPoint);
        setState(self, STATE_FROZEN, false);
        messageTo(self, "resetDecel", params2, 5f, false);
        return SCRIPT_CONTINUE;
    }

    public int resetSpeed(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id vehicleObj = getVehicle(self);
        float speed = params.getFloat("currentSpeed");
        vehicle.setMaximumSpeed(vehicleObj, speed);
        broadcast(self, "Your speed has been throttled back to " + speed + " m/s as your engines are overheating.");
        LOG("ethereal", "[Vehicle SUI]: Resetting speed for vehicle " + vehicleObj + " to " + speed + " m/s");
        return SCRIPT_CONTINUE;
    }

    public int onVehicleSpeedIncrease(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Entered onVehicleSpeedIncrease");
        obj_id vehicleObj = getVehicle(self);
        if (vehicleObj == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        if (getState(self, STATE_RIDING_MOUNT) != 1)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        float maxSpeed = vehicle.getMaximumSpeed(vehicleObj);
        if (maxSpeed >= 80f)
        {
            broadcast(self, "You cannot increase your speed any further without damaging your engines!");
            return SCRIPT_CONTINUE;
        }
        if (maxSpeed + 5f > 80f)
        {
            broadcast(self, "You cannot increase your speed any further without damaging your engines!");
            return SCRIPT_CONTINUE;
        }
        else
        {
            broadcast(self, "You have increased your speed by 5 m/s to " + (maxSpeed + 5f) + " m/s");
            vehicle.setMaximumSpeed(vehicleObj, maxSpeed + 5f);
            LOG("ethereal", "[Vehicle SUI]: Increasing speed for vehicle " + vehicleObj + " to " + (maxSpeed + 5f) + " m/s");
        }
        play2dNonLoopingSound(self, "sound/shp_eng_quantum_ion_drive.snd");
        return SCRIPT_CONTINUE;
    }

    public int onVehicleStopDecrease(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[Vehicle SUI]: Entered onVehicleSpeedDecrease");
        obj_id vehicleObj = getVehicle(self);
        if (vehicleObj == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        if (getState(self, STATE_RIDING_MOUNT) != 1)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        float maxSpeed = vehicle.getMaximumSpeed(vehicleObj);
        if (maxSpeed <= 5f)
        {
            broadcast(self, "You cannot decrease your speed any further.");
            return SCRIPT_CONTINUE;
        }
        if (maxSpeed - 5f <= 5f)
        {
            broadcast(self, "You cannot decrease your speed any further.");
            return SCRIPT_CONTINUE;
        }
        else
        {
            broadcast(self, "You have decreased your speed by 5 m/s to " + (maxSpeed - 5f) + " m/s");
            vehicle.setMaximumSpeed(vehicleObj, maxSpeed - 5f);
            LOG("ethereal", "[Vehicle SUI]: Decreasing speed for vehicle " + vehicleObj + " to " + (maxSpeed - 5f) + " m/s");
        }
        play2dNonLoopingSound(self, "sound/shp_eng_quantum_ion_drive.snd");
        return SCRIPT_CONTINUE;
    }

    public int resetDecel(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id vehicleObj = getVehicle(self);
        if (vehicleObj == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        vehicle.setDecel(vehicleObj, params.getFloat("originalDecel"));
        LOG("ethereal", "[Vehicle SUI]: Resetting deceleration for " + self + " to " + params.getFloat("originalDecel") + "!");
        return SCRIPT_CONTINUE;
    }

    public int onGetVehicleInfo(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id vehicleObj = getVehicle(self);
        if (vehicleObj == null)
        {
            handleVehicleDismount(self);
            return SCRIPT_CONTINUE;
        }
        String prompt = "";
        prompt += "Vehicle Info:\n";
        prompt += "Vehicle Name: " + getVehicleName(self, vehicleObj) + "\n";
        prompt += "Hitpoints: " + vehicle.getHitpoints(vehicleObj) + "\n";
        prompt += "Speed (Min): " + vehicle.getMinimumSpeed(vehicleObj) + " m/s\n";
        prompt += "Speed (Max): " + vehicle.getMaximumSpeed(vehicleObj) + " m/s\n";
        prompt += "Acceleration (Min): " + vehicle.getAccelMin(vehicleObj) + " m/s\n";
        prompt += "Acceleration (Max): " + vehicle.getAccelMax(vehicleObj) + " m/s\n";
        prompt += "Deceleration: " + vehicle.getDecel(vehicleObj) + " m/s\n";
        prompt += "Minimum Turn Rate: " + vehicle.getTurnRateMin(vehicleObj) + " degrees/sec\n";
        prompt += "Maximum Turn Rate: " + vehicle.getTurnRateMax(vehicleObj) + " degrees/sec\n";
        prompt += "Repulsor Level: " + vehicle.getHoverHeight(vehicleObj) + "m from terrain.\n";
        prompt += "Dampening: \n";
        prompt += "\tHeight: " + vehicle.getDampingHeight(vehicleObj) + "\n";
        prompt += "\tPitch: " + vehicle.getDampingPitch(vehicleObj) + "\n";
        prompt += "\tRoll: " + vehicle.getDampingRoll(vehicleObj) + "\n";
        prompt += "Glide: " + vehicle.getGlide(vehicleObj) + "\n";
        prompt += "Auto Leveling: " + vehicle.getAutoLevelling(vehicleObj) + "\n";
        String title = "Vehicle Information";
        sui.msgbox(self, self, prompt, sui.OK_ONLY, title, "noHandler");
        return SCRIPT_CONTINUE;
    }

    private String getVehicleName(obj_id self, obj_id vehicleId)
    {
        return vehicle.getShortenTemplateName(self, vehicleId);
    }

    public int handleVehicleDismount(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "myCurrentVehicle");
        removeObjVar(self, "vehicleHorn.pid");
        removeObjVar(self, "vehicle.lastThrust");
        removeObjVar(self, "vehicle.lastFire");
        closeOldWindow(self);
        //vehicle.dismountCreature(self);
        return SCRIPT_CONTINUE;
    }

    public static void closeOldWindow(obj_id who) throws InterruptedException
    {
        int pid = sui.getPid(who, "vehicleHornPid");
        if (pid > -1)
        {
            forceCloseSUIPage(pid);
            sui.removePid(who, "vehicleHornPid");
        }
    }
}
