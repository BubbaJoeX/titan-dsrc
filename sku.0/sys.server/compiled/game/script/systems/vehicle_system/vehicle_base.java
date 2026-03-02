package script.systems.vehicle_system;

import script.*;
import script.library.*;

public class vehicle_base extends script.base_script
{
    public vehicle_base()
    {
    }
    public static final String MENU_FILE = "pet/pet_menu";
    public static final String VCDPING_VEHICLE_SCRIPT_NAME = "systems.vehicle_system.vehicle_ping";
    public static final String MESSAGE_VEHICLE_ID = "vehicleId";
    public static final string_id SID_CITY_GARAGE_BANNED = new string_id("city/city", "garage_banned");
    public static final string_id SID_NO_GROUND_VEHICLE_IN_SPACE = new string_id("space/space_interaction", "no_ground_vehicle_in_space");
    public static final boolean debug = false;

    public static final String OV_AIRSPEEDER_ACTIVE = "airspeeder.active";
    public static final String OV_AIRSPEEDER_ASCENDING = "airspeeder.ascending";
    public static final String OV_AIRSPEEDER_PANEL_RIDER = "airspeeder.panelRider";
    public static final String OV_AIRSPEEDER_SAVED_HOVER = "airspeeder.savedHoverHeight";
    public static final String OV_AIRSPEEDER_SAVED_SPEED = "airspeeder.savedSpeed";
    public static final String OV_AIRSPEEDER_SAVED_MIN_SPEED = "airspeeder.savedMinSpeed";
    public static final String OV_AIRSPEEDER_SAVED_ACCEL_MIN = "airspeeder.savedAccelMin";
    public static final String OV_AIRSPEEDER_SAVED_ACCEL_MAX = "airspeeder.savedAccelMax";
    public static final String OV_AIRSPEEDER_SAVED_DECEL = "airspeeder.savedDecel";
    public static final String OV_AIRSPEEDER_SAVED_TURN_MAX = "airspeeder.savedTurnMax";
    public static final String OV_AIRSPEEDER_SAVED_BANKING = "airspeeder.savedBanking";
    public static final float AIRSPEEDER_HOVER_HEIGHT = 155.0f;
    public static final float AIRSPEEDER_SPEED = 200.0f;
    public static final float AIRSPEEDER_MIN_SPEED = 10.0f;
    public static final float AIRSPEEDER_ACCEL_MIN = 25.0f;
    public static final float AIRSPEEDER_ACCEL_MAX = 50.0f;
    public static final float AIRSPEEDER_DECEL = 30.0f;
    public static final float AIRSPEEDER_TURN_RATE_MAX = 180.0f;
    public static final float AIRSPEEDER_BANKING_ANGLE = 60.0f;
    public static final float AIRSPEEDER_CLIMB_RATE = 25.0f;
    public static final float AIRSPEEDER_TICK_INTERVAL = 0.1f;
    public static final float AIRSPEEDER_HELIX_TURNS = 0.5f;

    public static final String OV_AUTOPILOT_ACTIVE = "autopilot.active";
    public static final String OV_AUTOPILOT_INDEX = "autopilot.currentIndex";
    public static final String OV_GPS_MUTED = "airspeeder.gpsMuted";
    public static final float AUTOPILOT_BOARDING_DELAY = 15.0f;

    public void sendGpsMessage(obj_id vehicle, obj_id rider, String message) throws InterruptedException
    {
        if (!isIdValid(rider))
            return;
        if (hasObjVar(vehicle, OV_GPS_MUTED))
            return;
        sendSystemMessageTestingOnly(rider, message);
    }
    public int revertVehicleMod(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || !params.containsKey("type"))
        {
            return SCRIPT_CONTINUE;
        }
        int type = params.getInt("type");
        if (type == vehicle.MOD_TYPE_MAX_SPEED)
        {
            float oldSpeed = 0.0f;
            if (hasObjVar(self, vehicle.OBJVAR_MOD_MAX_SPEED_OLD))
            {
                oldSpeed = getFloatObjVar(self, vehicle.OBJVAR_MOD_MAX_SPEED_OLD);
                removeObjVar(self, vehicle.OBJVAR_MOD_MAX_SPEED_OLD);
            }
            if (hasObjVar(self, vehicle.OBJVAR_MOD_MAX_SPEED_DURATION))
            {
                removeObjVar(self, vehicle.OBJVAR_MOD_MAX_SPEED_DURATION);
            }
            if (oldSpeed > 0.0f)
            {
                vehicle.setMaximumSpeed(self, oldSpeed);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setAttributeAttained(self, attrib.VEHICLE);
        if (!hasScript(self, VCDPING_VEHICLE_SCRIPT_NAME))
        {
            if (debug)
            {
                LOG("vcdping-debug", "vehicle_base.OnInitialize(): attaching script [" + VCDPING_VEHICLE_SCRIPT_NAME + "] to vehicle id=[" + self + "]");
            }
            attachScript(self, VCDPING_VEHICLE_SCRIPT_NAME);
        }
        sendDestroyUnattendedVehicleSignal(self);
        messageTo(self, "handleAirspeederCheck", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }
    public int checkForJetpack(obj_id self, dictionary params) throws InterruptedException
    {
        String creature_name = getTemplateName(self);
        obj_id player = params.getObjId("player");
        boolean storeJetpack = false;
        if (vehicle.isJetPackVehicle(self))
        {
            if (getMountsEnabled())
            {
                debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ getMountsEneabled returnted TRUE");
                if (pet_lib.canMount(self, player))
                {
                    debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ pet_lib.canMount(self,player) returned TRUE");
                    queueCommand(player, (-536363215), self, creature_name, COMMAND_PRIORITY_FRONT);
                    debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ just attempted to Enqueue MOUNT command");
                }
                else 
                {
                    storeJetpack = true;
                }
            }
            else 
            {
                storeJetpack = true;
            }
        }
        if (storeJetpack)
        {
            string_id jetpackStoredMsg = new string_id("pet/pet_menu", "jetpack_stored");
            sendSystemMessage(player, jetpackStoredMsg);
            obj_id petControlDevice = callable.getCallableCD(self);
            vehicle.storeVehicle(petControlDevice, player);
        }
        return SCRIPT_CONTINUE;
    }
    public int handleVehicleDecay(obj_id self, dictionary params) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int handleAirspeederCheck(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
        {
            if (hasObjVar(self, "airspeeder.panelRider"))
            {
                obj_id panelRider = getObjIdObjVar(self, "airspeeder.panelRider");
                if (isIdValid(panelRider))
                {
                    showAirspeederPanel(panelRider, false);
                }
                removeObjVar(self, "airspeeder.panelRider");
            }
            if (hasObjVar(self, OV_AIRSPEEDER_ASCENDING))
            {
                removeObjVar(self, OV_AIRSPEEDER_ASCENDING);
                if (hasObjVar(self, OV_AIRSPEEDER_SAVED_HOVER))
                {
                    vehicle.setHoverHeight(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_HOVER));
                    removeObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);
                }
            }
            if (hasObjVar(self, "airspeeder.active"))
            {
                exitAirspeederModeLocal(self);
            }
            messageTo(self, "handleAirspeederCheck", null, 2.0f, false);
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, "airspeeder.panelRider") && rider == getMaster(self))
        {
            showAirspeederPanel(rider, true);
            setObjVar(self, "airspeeder.panelRider", rider);
            if (!hasScript(rider, "player.player_vehicle"))
            {
                attachScript(rider, "player.player_vehicle");
            }
        }
        messageTo(self, "handleAirspeederCheck", null, 1.0f, false);
        return SCRIPT_CONTINUE;
    }
    public int startSkywayAscent(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id rider = getRiderId(self);
        if (!isIdValid(rider) || isSpaceScene())
            return SCRIPT_CONTINUE;
        if (hasObjVar(self, OV_AIRSPEEDER_ACTIVE) || hasObjVar(self, OV_AIRSPEEDER_ASCENDING))
            return SCRIPT_CONTINUE;
        float currentHover = vehicle.getHoverHeight(self);
        if (!hasObjVar(self, OV_AIRSPEEDER_SAVED_HOVER))
        {
            setObjVar(self, OV_AIRSPEEDER_SAVED_HOVER, currentHover);
        }
        setObjVar(self, OV_AIRSPEEDER_ASCENDING, 1);
        dictionary ascentParams = new dictionary();
        ascentParams.put("startYaw", getYaw(self));
        ascentParams.put("startHover", currentHover);
        messageTo(self, "continueSkywayAscent", ascentParams, AIRSPEEDER_TICK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }
    public int continueSkywayAscent(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AIRSPEEDER_ASCENDING))
            return SCRIPT_CONTINUE;
        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
        {
            removeObjVar(self, OV_AIRSPEEDER_ASCENDING);
            if (hasObjVar(self, OV_AIRSPEEDER_SAVED_HOVER))
            {
                vehicle.setHoverHeight(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_HOVER));
                removeObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);
            }
            return SCRIPT_CONTINUE;
        }
        float currentHover = vehicle.getHoverHeight(self);
        float newHover = currentHover + (AIRSPEEDER_CLIMB_RATE * AIRSPEEDER_TICK_INTERVAL);
        float startHover = params.getFloat("startHover");
        float startYaw = params.getFloat("startYaw");
        float totalDistance = AIRSPEEDER_HOVER_HEIGHT - startHover;
        if (totalDistance <= 0.0f)
            totalDistance = 1.0f;
        float progress = (newHover - startHover) / totalDistance;
        float newYaw = startYaw + (360.0f * AIRSPEEDER_HELIX_TURNS * Math.min(1.0f, progress));
        setYaw(self, newYaw);
        if (newHover >= AIRSPEEDER_HOVER_HEIGHT)
        {
            vehicle.setHoverHeight(self, AIRSPEEDER_HOVER_HEIGHT);
            removeObjVar(self, OV_AIRSPEEDER_ASCENDING);
            setObjVar(self, OV_AIRSPEEDER_ACTIVE, 1);
            setObjectCollidable(self, false);
            setObjectCollidable(rider, false);
            setObjVar(self, OV_AIRSPEEDER_SAVED_SPEED, vehicle.getMaximumSpeed(self));
            setObjVar(self, OV_AIRSPEEDER_SAVED_MIN_SPEED, vehicle.getMinimumSpeed(self));
            setObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MIN, vehicle.getAccelMin(self));
            setObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MAX, vehicle.getAccelMax(self));
            setObjVar(self, OV_AIRSPEEDER_SAVED_DECEL, vehicle.getDecel(self));
            setObjVar(self, OV_AIRSPEEDER_SAVED_TURN_MAX, vehicle.getTurnRateMax(self));
            setObjVar(self, OV_AIRSPEEDER_SAVED_BANKING, vehicle.getBankingAngle(self));
            vehicle.setMaximumSpeed(self, AIRSPEEDER_SPEED);
            vehicle.setMinimumSpeed(self, AIRSPEEDER_MIN_SPEED);
            vehicle.setAccelMin(self, AIRSPEEDER_ACCEL_MIN);
            vehicle.setAccelMax(self, AIRSPEEDER_ACCEL_MAX);
            vehicle.setDecel(self, AIRSPEEDER_DECEL);
            vehicle.setTurnRateMax(self, AIRSPEEDER_TURN_RATE_MAX);
            vehicle.setBankingAngle(self, AIRSPEEDER_BANKING_ANGLE);

            if (hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            {
                sendAutoPilotCurrentWaypoint(self, rider);
            }
        }
        else
        {
            vehicle.setHoverHeight(self, newHover);
            params.put("startHover", startHover);
            params.put("startYaw", startYaw);
            messageTo(self, "continueSkywayAscent", params, AIRSPEEDER_TICK_INTERVAL, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int cancelSkywayAscent(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AIRSPEEDER_ASCENDING))
            return SCRIPT_CONTINUE;
        removeObjVar(self, OV_AIRSPEEDER_ASCENDING);
        float currentHover = vehicle.getHoverHeight(self);
        float targetHover = 0.5f;
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_HOVER))
        {
            targetHover = getFloatObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);
            removeObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);
        }
        dictionary descentParams = new dictionary();
        descentParams.put("startHover", currentHover);
        descentParams.put("targetHover", targetHover);
        descentParams.put("startYaw", getYaw(self));
        messageTo(self, "continueSkywayDescent", descentParams, AIRSPEEDER_TICK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }
    public int startSkywayDescent(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id rider = getRiderId(self);
        if (!isIdValid(rider) || isSpaceScene())
            return SCRIPT_CONTINUE;
        if (!hasObjVar(self, OV_AIRSPEEDER_ACTIVE))
            return SCRIPT_CONTINUE;
        float currentHover = vehicle.getHoverHeight(self);
        float targetHover = 0.5f;
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_HOVER))
        {
            targetHover = getFloatObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);
            removeObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);
        }
        exitAirspeederModeLocal(self);
        dictionary descentParams = new dictionary();
        descentParams.put("startHover", currentHover);
        descentParams.put("targetHover", targetHover);
        descentParams.put("startYaw", getYaw(self));
        messageTo(self, "continueSkywayDescent", descentParams, AIRSPEEDER_TICK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }
    public int continueSkywayDescent(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
            return SCRIPT_CONTINUE;
        float currentHover = vehicle.getHoverHeight(self);
        float targetHover = params.getFloat("targetHover");
        float startHover = params.getFloat("startHover");
        float startYaw = params.getFloat("startYaw");
        float newHover = currentHover - (AIRSPEEDER_CLIMB_RATE * AIRSPEEDER_TICK_INTERVAL);
        float totalDistance = startHover - targetHover;
        if (totalDistance <= 0.0f)
            totalDistance = 1.0f;
        float progress = (startHover - newHover) / totalDistance;
        float newYaw = startYaw + (360.0f * AIRSPEEDER_HELIX_TURNS * Math.min(1.0f, progress));
        setYaw(self, newYaw);
        if (newHover <= targetHover)
        {
            vehicle.setHoverHeight(self, targetHover);
        }
        else
        {
            vehicle.setHoverHeight(self, newHover);
            params.put("startHover", startHover);
            params.put("targetHover", targetHover);
            params.put("startYaw", startYaw);
            messageTo(self, "continueSkywayDescent", params, AIRSPEEDER_TICK_INTERVAL, false);
        }
        return SCRIPT_CONTINUE;
    }
    public void exitAirspeederModeLocal(obj_id veh) throws InterruptedException
    {
        cancelAutoPilotLocal(veh);
        vehicle.setBoostMode(veh, false);
        vehicle.setTrafficMode(veh, false);
        removeObjVar(veh, OV_AIRSPEEDER_ACTIVE);
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_HOVER))
        {
            vehicle.setHoverHeight(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_HOVER));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_HOVER);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_SPEED))
        {
            vehicle.setMaximumSpeed(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_SPEED));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_SPEED);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_MIN_SPEED))
        {
            vehicle.setMinimumSpeed(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_MIN_SPEED));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_MIN_SPEED);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_ACCEL_MIN))
        {
            vehicle.setAccelMin(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_ACCEL_MIN));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_ACCEL_MIN);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_ACCEL_MAX))
        {
            vehicle.setAccelMax(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_ACCEL_MAX));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_ACCEL_MAX);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_DECEL))
        {
            vehicle.setDecel(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_DECEL));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_DECEL);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_TURN_MAX))
        {
            vehicle.setTurnRateMax(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_TURN_MAX));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_TURN_MAX);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_SAVED_BANKING))
        {
            vehicle.setBankingAngle(veh, getFloatObjVar(veh, OV_AIRSPEEDER_SAVED_BANKING));
            removeObjVar(veh, OV_AIRSPEEDER_SAVED_BANKING);
        }
        setObjectCollidable(veh, true);
        obj_id rider = getRiderId(veh);
        if (isIdValid(rider))
        {
            setObjectCollidable(rider, true);
        }
        if (hasObjVar(veh, OV_AIRSPEEDER_PANEL_RIDER))
        {
            obj_id panelRider = getObjIdObjVar(veh, OV_AIRSPEEDER_PANEL_RIDER);
            if (isIdValid(panelRider))
            {
                showAirspeederPanel(panelRider, false);
            }
            removeObjVar(veh, OV_AIRSPEEDER_PANEL_RIDER);
        }
    }
    public int handleSkywayCollision(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id rider = getRiderId(self);
        cancelAutoPilotLocal(self);
        exitAirspeederModeLocal(self);
        if (isIdValid(rider))
        {
            playClientEffectObj(rider, "clienteffect/avatar_hallway_explosion.cef", self, "");
            pet_lib.doDismountNow(rider, false);
            setPosture(rider, POSTURE_INCAPACITATED);
            sendSystemMessageTestingOnly(rider, "Your speeder crashed into a building!");
        }
        obj_id vcd = callable.getCallableCD(self);
        if (isIdValid(vcd))
        {
            vehicle.storeVehicle(vcd, rider, false);
        }
        else
        {
            setHitpoints(self, 0);
        }
        return SCRIPT_CONTINUE;
    }

    public int addAutoPilotWaypoint(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;
        float x = params.getFloat("x");
        float z = params.getFloat("z");
        location vehicleLoc = getLocation(self);
        location dest = new location(x, vehicleLoc.y, z, vehicleLoc.area, vehicleLoc.cell);

        location[] queue = null;
        if (hasObjVar(self, "autopilot.queue"))
            queue = getLocationArrayObjVar(self, "autopilot.queue");

        if (queue == null)
        {
            queue = new location[]{dest};
        }
        else
        {
            location[] newQueue = new location[queue.length + 1];
            for (int i = 0; i < queue.length; i++)
                newQueue[i] = queue[i];
            newQueue[queue.length] = dest;
            queue = newQueue;
        }
        setObjVar(self, "autopilot.queue", queue);

        obj_id rider = getRiderId(self);
        if (isIdValid(rider))
        {
            sendGpsMessage(self, rider, "Auto-Pilot: Waypoint added (" + queue.length + " in queue)");
        }

        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
        {
            messageTo(self, "startAutoPilot", null, 0, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int startAutoPilot(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AIRSPEEDER_ACTIVE))
            return SCRIPT_CONTINUE;
        if (!hasObjVar(self, "autopilot.queue"))
            return SCRIPT_CONTINUE;

        setObjVar(self, OV_AUTOPILOT_ACTIVE, 1);
        setObjVar(self, OV_AUTOPILOT_INDEX, 0);

        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
            return SCRIPT_CONTINUE;

        sendGpsMessage(self, rider, "Auto-Pilot engaged.");
        sendAutoPilotCurrentWaypoint(self, rider);
        return SCRIPT_CONTINUE;
    }

    public void sendAutoPilotCurrentWaypoint(obj_id self, obj_id rider) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            return;

        location[] queue = getLocationArrayObjVar(self, "autopilot.queue");
        int idx = hasObjVar(self, OV_AUTOPILOT_INDEX) ? getIntObjVar(self, OV_AUTOPILOT_INDEX) : 0;

        if (queue == null || idx >= queue.length)
        {
            cancelAutoPilotLocal(self);
            sendGpsMessage(self, rider, "Auto-Pilot: All waypoints reached.");
            return;
        }

        location target = queue[idx];
        int remaining = queue.length - idx;
        sendAutoPilotEngage(rider, target.x, target.z);
        sendGpsMessage(self, rider, "Auto-Pilot: Heading to waypoint " + (idx + 1) + " of " + queue.length + ".");
    }

    public int handleAutoPilotArrived(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            return SCRIPT_CONTINUE;

        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
        {
            cancelAutoPilotLocal(self);
            return SCRIPT_CONTINUE;
        }

        sendAutoPilotDisengage(rider);

        int idx = hasObjVar(self, OV_AUTOPILOT_INDEX) ? getIntObjVar(self, OV_AUTOPILOT_INDEX) : 0;
        idx++;
        setObjVar(self, OV_AUTOPILOT_INDEX, idx);

        location[] queue = getLocationArrayObjVar(self, "autopilot.queue");
        int remaining = (queue != null) ? queue.length - idx : 0;

        if (remaining <= 0)
        {
            sendGpsMessage(self, rider, "Auto-Pilot: All waypoints reached. Descending.");
            cancelAutoPilotLocal(self);
            messageTo(self, "startSkywayDescent", null, 0, false);
            return SCRIPT_CONTINUE;
        }

        sendGpsMessage(self, rider, "Auto-Pilot: Arrived. Departing in " + (int) AUTOPILOT_BOARDING_DELAY + " seconds. (" + remaining + " waypoint" + (remaining == 1 ? "" : "s") + " remaining)");
        beginAutoPilotDescent(self);
        messageTo(self, "autoPilotResumeNext", null, AUTOPILOT_BOARDING_DELAY, false);
        return SCRIPT_CONTINUE;
    }

    public void beginAutoPilotDescent(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AIRSPEEDER_ACTIVE))
            return;

        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
            return;

        float currentHover = vehicle.getHoverHeight(self);
        float targetHover = 0.5f;
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_HOVER))
            targetHover = getFloatObjVar(self, OV_AIRSPEEDER_SAVED_HOVER);

        removeObjVar(self, OV_AIRSPEEDER_ACTIVE);
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_SPEED))
        {
            vehicle.setMaximumSpeed(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_SPEED));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_SPEED);
        }
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_MIN_SPEED))
        {
            vehicle.setMinimumSpeed(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_MIN_SPEED));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_MIN_SPEED);
        }
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MIN))
        {
            vehicle.setAccelMin(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MIN));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MIN);
        }
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MAX))
        {
            vehicle.setAccelMax(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MAX));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_ACCEL_MAX);
        }
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_DECEL))
        {
            vehicle.setDecel(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_DECEL));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_DECEL);
        }
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_TURN_MAX))
        {
            vehicle.setTurnRateMax(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_TURN_MAX));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_TURN_MAX);
        }
        if (hasObjVar(self, OV_AIRSPEEDER_SAVED_BANKING))
        {
            vehicle.setBankingAngle(self, getFloatObjVar(self, OV_AIRSPEEDER_SAVED_BANKING));
            removeObjVar(self, OV_AIRSPEEDER_SAVED_BANKING);
        }
        setObjectCollidable(self, true);
        setObjectCollidable(rider, true);

        dictionary descentParams = new dictionary();
        descentParams.put("startHover", currentHover);
        descentParams.put("targetHover", targetHover);
        descentParams.put("startYaw", getYaw(self));
        messageTo(self, "continueSkywayDescent", descentParams, AIRSPEEDER_TICK_INTERVAL, false);
    }

    public int autoPilotResumeNext(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, OV_AUTOPILOT_ACTIVE))
            return SCRIPT_CONTINUE;

        obj_id rider = getRiderId(self);
        if (!isIdValid(rider))
        {
            cancelAutoPilotLocal(self);
            return SCRIPT_CONTINUE;
        }

        sendGpsMessage(self, rider, "Auto-Pilot: Resuming. Ascending...");
        messageTo(self, "startSkywayAscent", null, 0, false);
        return SCRIPT_CONTINUE;
    }

    public int cancelAutoPilot(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id rider = getRiderId(self);
        cancelAutoPilotLocal(self);
        if (isIdValid(rider))
            sendGpsMessage(self, rider, "Auto-Pilot disengaged.");
        return SCRIPT_CONTINUE;
    }

    public void cancelAutoPilotLocal(obj_id veh) throws InterruptedException
    {
        boolean wasActive = hasObjVar(veh, OV_AUTOPILOT_ACTIVE);
        if (wasActive)
            removeObjVar(veh, OV_AUTOPILOT_ACTIVE);
        if (hasObjVar(veh, OV_AUTOPILOT_INDEX))
            removeObjVar(veh, OV_AUTOPILOT_INDEX);
        if (hasObjVar(veh, "autopilot.queue"))
            removeObjVar(veh, "autopilot.queue");

        if (wasActive)
        {
            obj_id rider = getRiderId(veh);
            if (isIdValid(rider))
                sendAutoPilotDisengage(rider);
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (isDead(self) || ai_lib.aiIsDead(player) || self == null || self == obj_id.NULL_ID || !isIdValid(self))
        {
            return SCRIPT_CONTINUE;
        }
        boolean isMountedOn = isMountedOnCreatureQueried(self, player);
        boolean isOwnedByPlayer = player == getMaster(self);
        sendDestroyUnattendedVehicleSignal(self);
        if (!isOwnedByPlayer && !isMountedOn)
        {
            LOG("special_sign", "!isOwnedByPlayer && !isMountedOn");
            if (!doesMountHaveRoom(self))
            {
                LOG("special_sign", "!doesMountHaveRoom(self)");
                return SCRIPT_CONTINUE;
            }
            else if (!vehicle.mountPermissionCheck(self, player, false))
            {
                LOG("special_sign", "vehicle.mountPermissionCheck ");
                return SCRIPT_CONTINUE;
            }
        }
        else 
        {
            LOG("special_sign", "isOwnedByPlayer || isMountedOn");
        }
        obj_id petControlDevice = callable.getCallableCD(self);
        String vehicle_name = getTemplateName(self);
        if (getMountsEnabled())
        {
            if (!vehicle.isJetPackVehicle(self))
            {
                if (pet_lib.canMount(self, player) || isMountedOn)
                {
                    mi.addRootMenu(menu_info_types.SERVER_VEHICLE_ENTER_EXIT, new string_id(MENU_FILE, "menu_enter_exit"));
                }
            }
        }
        if (!isOwnedByPlayer)
        {
            return SCRIPT_CONTINUE;
        }
        if (!isSpaceScene())
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Advanced Piloting"));
        }
        if (ai_lib.isInCombat(player) || pet_lib.wasInCombatRecently(self, player, false))
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, battlefield.VAR_CONSTRUCTED) && !ai_lib.isInCombat(player))
        {
            mi.addRootMenu(menu_info_types.PET_STORE, new string_id(MENU_FILE, "menu_store"));
        }
        if (ai_lib.isAiDead(self))
        {
            return SCRIPT_CONTINUE;
        }
        if (utils.hasScriptVar(self, "inRepairZone") && (!isDisabled(self) || (vehicle.canRepairDisabledVehicle(petControlDevice) && isDisabled(self))))
        {
            obj_id garage = utils.getObjIdScriptVar(self, "inRepairZone");
            if (isIdValid(garage) && exists(garage) && garage.isLoaded())
            {
                mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id(MENU_FILE, "menu_repair_vehicle"));
            }
        }
        else if (isDisabled(self) && hasBarcRepairKit(player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("barc_repair", "refurbish_barc"));
        }
        return SCRIPT_CONTINUE;
    }
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (isSpaceScene())
        {
            sendSystemMessage(player, SID_NO_GROUND_VEHICLE_IN_SPACE);
            return SCRIPT_CONTINUE;
        }
        if (ai_lib.aiIsDead(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (ai_lib.isInCombat(player) || pet_lib.wasInCombatRecently(self, player, false))
        {
            return SCRIPT_CONTINUE;
        }
        if (item != menu_info_types.SERVER_VEHICLE_ENTER_EXIT && item != menu_info_types.SERVER_MENU2 && pet_lib.isPet(self) && pet_lib.hasMaster(self) && player != getMaster(self))
        {
            return SCRIPT_CONTINUE;
        }
        if (ai_lib.isAiDead(player))
        {
            return SCRIPT_CONTINUE;
        }
        String creature_name = getName(self);
        obj_id petControlDevice = callable.getCallableCD(self);
        if (item == menu_info_types.PET_STORE)
        {
            if (!hasObjVar(self, battlefield.VAR_CONSTRUCTED))
            {
                if (hasObjVar(self, OV_AIRSPEEDER_ACTIVE) || hasObjVar(self, OV_AIRSPEEDER_ASCENDING))
                {
                    exitAirspeederModeLocal(self);
                }
                vehicle.storeVehicle(petControlDevice, player);
            }
            return SCRIPT_CONTINUE;
        }
        else if (item == menu_info_types.SERVER_VEHICLE_ENTER_EXIT)
        {
            debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ SERVER_VEHICLE_ENTER_EXIT menu object selected");
            if (getMountsEnabled())
            {
                debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ getMountsEneabled returnted TRUE");
                if (isMountedOnCreatureQueried(self, player))
                {
                    queueCommand(player, (117012717), self, creature_name, COMMAND_PRIORITY_FRONT);
                }
                else if (pet_lib.canMount(self, player))
                {
                    debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ pet_lib.canMount(self,player) returned TRUE");
                    queueCommand(player, (-536363215), self, creature_name, COMMAND_PRIORITY_FRONT);
                    debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ just attempted to Enqueue MOUNT command");
                }
            }
            else 
            {
                debugServerConsoleMsg(player, "+++ pet . onObjectMenuSelect +++ getMountsEneabled returnted FALSE");
            }
        }
        else if (item == menu_info_types.SERVER_MENU2)
        {
            showAirspeederPanel(player, true);
            setObjVar(self, "airspeeder.panelRider", player);
            if (!hasScript(player, "player.player_vehicle"))
            {
                attachScript(player, "player.player_vehicle");
            }
        }
        else if (item == menu_info_types.SERVER_MENU1)
        {
            if (ai_lib.isInCombat(player))
            {
                return SCRIPT_CONTINUE;
            }
            if (utils.hasScriptVar(self, "inRepairZone") && (!isDisabled(self) || (vehicle.canRepairDisabledVehicle(petControlDevice) && isDisabled(self))))
            {
                int city_id = getCityAtLocation(getLocation(self), 0);
                if ((city_id > 0) && city.isCityBanned(player, city_id))
                {
                    sendSystemMessage(player, SID_CITY_GARAGE_BANNED);
                    return SCRIPT_CONTINUE;
                }
                vehicle.repairVehicle(player, self);
                sendDirtyObjectMenuNotification(self);
            }
            else if (isDisabled(self) && hasBarcRepairKit(player))
            {
                vehicle.restoreVehicle(player, self);
                sendDirtyObjectMenuNotification(self);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public boolean isMountedOnCreatureQueried(obj_id pet, obj_id player) throws InterruptedException
    {
        if (!isIdValid(pet))
        {
            return false;
        }
        if (!isIdValid(player))
        {
            return false;
        }
        obj_id playerCurrentMount = getMountId(player);
        if (!isIdValid(playerCurrentMount))
        {
            return false;
        }
        if (playerCurrentMount != pet)
        {
            return false;
        }
        return true;
    }
    public boolean canTrainAsMount(obj_id pet, obj_id player) throws InterruptedException
    {
        if (!isIdValid(pet))
        {
            debugServerConsoleMsg(player, "+++ PET_LIB . canTrainAsMount +++ isIdValid failed for (pet)");
            return false;
        }
        if (!isIdValid(player))
        {
            debugServerConsoleMsg(player, "+++ PET_LIB . canTrainAsMount +++ isIdValid failed for (player)");
            return false;
        }
        if (!(couldPetBeMadeMountable(pet) == MSC_CREATURE_MOUNTABLE))
        {
            debugServerConsoleMsg(player, "+++ PET_LIB . canTrainAsMount +++ couldPetBeMadeMountable (pet) returned something other than MSC_CREATURE_MOUNTABLE ");
            debugServerConsoleMsg(player, "+++ PET_LIB . canTrainAsMount +++ couldPetBeMadeMountable (pet) returned " + couldPetBeMadeMountable(pet));
            return false;
        }
        return true;
    }
    public boolean trainMount(obj_id pet, obj_id player) throws InterruptedException
    {
        if (!isIdValid(pet) || !isIdValid(player))
        {
            return false;
        }
        if (!canTrainAsMount(pet, player))
        {
            return false;
        }
        if (!makePetAMount(pet, player))
        {
            debugServerConsoleMsg(player, "+++ VEHICLE . onAttach +++ makePetAMount(self,player) returned FALSE");
            return false;
        }
        else 
        {
            debugServerConsoleMsg(player, "+++ VEHICLE . onAttach +++ makePetAMount(self,player) returned TRUE. YEAH!");
        }
        return true;
    }
    public boolean makePetAMount(obj_id pet, obj_id player) throws InterruptedException
    {
        if (!isIdValid(pet) || !isIdValid(player))
        {
            return false;
        }
        obj_id petControlDevice = callable.getCallableCD(pet);
        if (!(couldPetBeMadeMountable(pet) == 0))
        {
            debugServerConsoleMsg(player, "+++ VEHICLE . onAttach +++ couldPetBeMadeMountable(pet) returned FALSE.");
            return false;
        }
        if (!makePetMountable(pet))
        {
            debugServerConsoleMsg(player, "+++ VEHICLE . onAttach +++ makePetMountable(pet) returned FALSE.");
            return false;
        }
        else 
        {
            setObjVar(petControlDevice, "ai.pet.trainedMount", 1);
        }
        return true;
    }
    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(self, "ai.pet.masterName"))
        {
            names[idx] = "owner";
            attribs[idx] = getStringObjVar(self, "ai.pet.masterName");
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }
    public int OnObjectDisabled(obj_id self, obj_id killer) throws InterruptedException
    {
        if (hasObjVar(self, OV_AIRSPEEDER_ACTIVE))
        {
            exitAirspeederModeLocal(self);
        }
        obj_id owner = getMaster(self);
        if (isIdValid(owner))
        {
            obj_id vcd = callable.getCallableCD(self);
            if (isIdValid(vcd))
            {
                String killerDesc = null;
                if (isIdValid(killer))
                {
                    String killerName = getPlayerName(killer);
                    if (killerName != null && killerName.length() > 0)
                    {
                        killerDesc = "player " + killer + "(" + killerName + ")";
                    }
                    else 
                    {
                        killerDesc = "npc " + killer + "(" + getName(killer) + ")";
                    }
                }
                else if (getIntObjVar(vcd, "attrib.hit_points") > 0)
                {
                    killerDesc = "decay";
                }
                if (killerDesc != null)
                {
                    CustomerServiceLog("vehicle", "vehicle template:" + getTemplateName(self) + " vcd:" + vcd + " owner:" + owner + "(" + getName(owner) + ") disabled by " + killerDesc);
                }
            }
            sendSystemMessage(owner, pet_lib.SID_SYS_VEHICLE_DISABLED);
            LOG("vehicle_base", "It is destroyed");
            obj_id rider = getRiderId(self);
            if (isIdValid(rider))
            {
                utils.dismountRiderJetpackCheck(rider);
            }
            if (ai_lib.isInCombat(self))
            {
                vehicle.storeVehicle(vcd, rider, false);
            }
            else 
            {
                messageTo(self, "handleDisabledPackRequest", null, 120, false);
            }
            int vehicleBuff = buff.getBuffOnTargetFromGroup(rider, "vehicle");
            if (vehicleBuff != 0)
            {
                buff.removeBuff(rider, vehicleBuff);
            }
        }
        return SCRIPT_CONTINUE;
    }
    public int OnDestroy(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, "pet.controlDestroyed"))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id master = getMaster(self);
        boolean isFactionPet = (ai_lib.isNpc(self) || ai_lib.aiGetNiche(self) == NICHE_VEHICLE || ai_lib.isAndroid(self));
        obj_id petControlDevice = callable.getCallableCD(self);
        if (hasObjVar(self, battlefield.VAR_CONSTRUCTED) || (isFactionPet && ai_lib.aiIsDead(self)))
        {
            if (isIdValid(petControlDevice))
            {
                messageTo(petControlDevice, "handleFlagDeadCreature", null, 0, false);
            }
        }
        if (isIdValid(petControlDevice))
        {
            obj_id currentPet = callable.getCDCallable(petControlDevice);
            if (isIdValid(currentPet) && currentPet == self)
            {
                pet_lib.savePetInfo(self, petControlDevice);
                setObjVar(petControlDevice, "pet.timeStored", getGameTime());
                callable.setCDCallable(petControlDevice, null);
            }
        }
        if (hasObjVar(self, OV_AIRSPEEDER_ACTIVE))
        {
            exitAirspeederModeLocal(self);
        }
        return SCRIPT_CONTINUE;
    }
    public int handleDisabledPackRequest(obj_id self, dictionary params) throws InterruptedException
    {
        if (isDisabled(self))
        {
            messageTo(self, "handlePackRequest", null, 1, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int handlePackRequest(obj_id self, dictionary params) throws InterruptedException
    {
        debugServerConsoleMsg(null, "+++ vehicle_base.messageHandler handlePackRequest +++ entered HANDLEPACKREQUEST message handler");
        obj_id rider = getRiderId(self);
        if (isIdValid(rider))
        {
            boolean dismountSuccess = pet_lib.doDismountNow(rider);
            if (!dismountSuccess)
            {
                LOG("mounts-bug", "vehicle_base.messageHandler handlePackRequest(): creature [" + self + "], rider [" + rider + "] failed to dismount, aborting pack request.  This mount/vehicle probably is in an invalid state now.");
                return SCRIPT_CONTINUE;
            }
        }
        debugServerConsoleMsg(null, "+++ vehicle_base.messageHandler handlePackRequest +++ destroying the vehicle now");
        obj_id vehicleControlDevice = callable.getCallableCD(self);
        vehicle.saveVehicleInfo(vehicleControlDevice, self);
        utils.setScriptVar(self, "stored", true);
        dictionary messageData = new dictionary();
        messageData.put(MESSAGE_VEHICLE_ID, self);
        sendDirtyObjectMenuNotification(vehicleControlDevice);
        if (destroyObject(self))
        {
            messageTo(vehicleControlDevice, "handleRemoveCurrentVehicle", messageData, 1, false);
        }
        else 
        {
            debugServerConsoleMsg(null, "+++ vehicle_base.messageHandler handlePackRequest +++ WARNINGWARNING - FAILED TO DESTROY SELF");
        }
        return SCRIPT_CONTINUE;
    }
    public int destroyNow(obj_id self, dictionary params) throws InterruptedException
    {
        setObjVar(self, "pet.controlDestroyed", true);
        CustomerServiceLog("vehicle_bug", "vehicle_base - destroyNow::Recieved signal to destroy with ID: " + params.getInt("signalId"));
        destroyObject(self);
        return SCRIPT_OVERRIDE;
    }
    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id weapon, int[] damage) throws InterruptedException
    {
        utils.setScriptVar(self, "pet.combatEnded", getGameTime());
        return SCRIPT_CONTINUE;
    }
    public int OnObjectDamaged(obj_id self, obj_id attacker, obj_id weapon, int damage) throws InterruptedException
    {
        utils.setScriptVar(self, "pet.combatEnded", getGameTime());
        return SCRIPT_CONTINUE;
    }
    public int handleSetColors(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        setColors(self, params);
        return SCRIPT_CONTINUE;
    }
    public int handleSetCustomization(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        dictionary dc = params.getDictionary("dc");
        boolean updatedColors = setColors(self, dc);
        params.remove("dc");
        obj_id tool = params.getObjId("tool");
        if (!isIdValid(tool))
        {
            return SCRIPT_CONTINUE;
        }
        if (updatedColors)
        {
            messageTo(tool, "customizationSuccess", params, 0.0f, false);
        }
        else 
        {
            messageTo(tool, "customizationFailed", params, 0.0f, false);
        }
        return SCRIPT_CONTINUE;
    }
    public boolean setColors(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return false;
        }
        boolean litmus = true;
        java.util.Enumeration keys = params.keys();
        while (keys.hasMoreElements())
        {
            String var = (String)keys.nextElement();
            int idx = params.getInt(var);
            litmus &= hue.setColor(self, var, idx);
        }
        return litmus;
    }
    public boolean hasBarcRepairKit(obj_id player) throws InterruptedException
    {
        obj_id tool = utils.getItemPlayerHasByTemplate(player, "object/tangible/item/ep3/barc_repair_tool.iff");
        return (isIdValid(tool));
    }
    public void sendDestroyUnattendedVehicleSignal(obj_id vehicle) throws InterruptedException
    {
        trial.bumpSession(vehicle);
        messageTo(vehicle, "handleDestroyUnattended", trial.getSessionDict(vehicle), 600, false);
    }
    public int handleDestroyUnattended(obj_id self, dictionary params) throws InterruptedException
    {
        if (!trial.verifySession(self, params))
        {
            return SCRIPT_CONTINUE;
        }
        if (vehicle.isBattlefieldVehicle(self))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id rider = getRiderId(self);
        if (isIdValid(rider))
        {
            sendDestroyUnattendedVehicleSignal(self);
            return SCRIPT_CONTINUE;
        }
        obj_id vcd = callable.getCallableCD(self);
        if (!isIdValid(vcd))
        {
            params.put("signalId", 0);
            messageTo(self, "destroyNow", params, 0, false);
            return SCRIPT_CONTINUE;
        }
        obj_id currentVehicle = callable.getCDCallable(vcd);
        if (!isIdValid(currentVehicle) || currentVehicle != self)
        {
            params.put("signalId", 1);
            messageTo(self, "destroyNow", params, 0, false);
            CustomerServiceLog("vehicle_bug", "vehicle_base - handleDestroyUnattended::Validating current vehicle(" + currentVehicle + ") vs self(" + self + "): " + params.getInt("signalId"));
            return SCRIPT_CONTINUE;
        }
        obj_id player = utils.getContainingPlayer(vcd);
        vehicle.storeVehicle(vcd, player);
        return SCRIPT_CONTINUE;
    }
}
