package script.systems.mechanic;/*
@Origin: dsrc.script.systems.mechanic
@Author: BubbaJoeX
@Purpose: Toolkit for vehicle tuning.
@Created: Tuesday 9/26/2023, at 12:25am
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

import static script.library.vehicle.s_varInfoConversions;
import static script.library.vehicle.s_varInfoNames;

public class toolkit extends base_script
{
    public static int PAYOUT_AMOUNT = 24000;
    public static float VEH_MAX_SPEED = 45.5f;
    public static float VEH_MAX_HEIGHT = 8.0f; //any higher and you can't radial the mount
    public static float VEH_MAX_ACCEL = 40.0f;
    public static float VEH_MAX_DECEL = 40.0f;
    public static String MECHANIC_VAR = "vehicle_mechanic";
    public static String[] TOOLKIT_TYPES = {
            "Speed",
            "Hover Height",
            "Acceleration",
            "Banking",
            "Turn Rate",
            "Deceleration",
            "Hover Height Damping"
    };

    public toolkit()
    {
    }

    public static int setValue(obj_id vehicle, float value, int var_index) throws InterruptedException
    {
        String vi_name = s_varInfoNames[var_index];
        float vi_conversion = s_varInfoConversions[var_index];
        int ivalue = (int) (value * vi_conversion);
        setRangedIntCustomVarValue(vehicle, vi_name, ivalue);
        return ivalue;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setObjVar(self, "mechanic.modifier", 1.0f);
        reInitToolkit(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        reInitToolkit(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.SERVER_MENU1, unlocalized("Tune Vehicle"));
        if (isGod(player))
        {
            int mama = mi.addRootMenu(menu_info_types.SERVER_MENU2, gm.SID_RADIAL_GM_ROOT);
            //@Note: Which type, speed, decel, etc etc
            mi.addSubMenu(mama, menu_info_types.SERVER_MENU3, unlocalized("Set Toolkit Type"));
            //@Note: How much to change the value by
            mi.addSubMenu(mama, menu_info_types.SERVER_MENU4, unlocalized("Set Toolkit Power"));
            //@Note: Reset the toolkit, stripping all objvars
            mi.addSubMenu(mama, menu_info_types.SERVER_MENU5, unlocalized("Reset Tool Kit"));
        }
        return SCRIPT_CONTINUE;
    }

    private string_id unlocalized(String godMenu)
    {
        return new string_id(godMenu);
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1)
        {
            if (!utils.isNestedWithinAPlayer(self))
            {
                return SCRIPT_CONTINUE;
            }
            if (!vehicle.isRidingVehicle(player))
            {
                broadcast(player, "You must be in your vehicle to apply this tune-up.");
                return SCRIPT_CONTINUE;
            }
            if (!isNearGarage(player))
            {
                broadcast(player, "You must near a garage bay to apply this tune-up.");
                return SCRIPT_CONTINUE;
            }
            if (vehicle.getMaximumSpeed(vehicle.getMountId(player)) >= VEH_MAX_SPEED && !isGod(player))
            {
                broadcast(player, "Your vehicle cannot possibly accept any more tuning for speed.");
                return SCRIPT_CONTINUE;
            }
            if (vehicle.getHoverHeight(vehicle.getMountId(player)) >= VEH_MAX_HEIGHT && !isGod(player))
            {
                broadcast(player, "Your vehicle cannot possibly accept any more tuning for height.");
                return SCRIPT_CONTINUE;
            }
            if (vehicle.getAccelMax(vehicle.getMountId(player)) >= VEH_MAX_ACCEL && !isGod(player))
            {
                broadcast(player, "Your vehicle cannot possibly accept any more tuning for acceleration.");
                return SCRIPT_CONTINUE;
            }
            if (vehicle.getDecel(vehicle.getMountId(player)) >= VEH_MAX_DECEL && !isGod(player))
            {
                broadcast(player, "Your vehicle cannot possibly accept any more tuning for deceleration.");
                return SCRIPT_CONTINUE;
            }
            String[] NO_EDIT_VEHICLES = {"jetpack", "920"};
            for (String litmus : NO_EDIT_VEHICLES)
            {
                if (getTemplateName(vehicle.getMountId(player)).contains(litmus))
                {
                    broadcast(player, "This vehicle does not qualify for any statistic enhancements,");
                    return SCRIPT_CONTINUE;
                }
            }

            obj_id veh = getMountId(player);
            obj_id rider = getRiderId(veh);
            float currentValue;
            switch (getTookitType(self))
            {
                case 1:
                    currentValue = vehicle.getMaximumSpeed(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_SPEED_MAX);
                    broadcast(rider, "You have increased your vehicle's maximum speed by " + getTookitPower(self));
                    break;
                case 13:
                    for (String litmus : NO_EDIT_VEHICLES)
                    {
                        if (getTemplateName(vehicle.getMountId(player)).contains(litmus) && !isGod(player))
                        {
                            broadcast(player, "This vehicle cannot have it's hover height modified.");
                            return SCRIPT_CONTINUE;
                        }
                    }
                    currentValue = vehicle.getHoverHeight(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_HOVER_HEIGHT);
                    broadcast(rider, "You have increased your vehicle's maximum hover height by " + getTookitPower(self));
                    break;
                case 5:
                    currentValue = vehicle.getAccelMax(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_ACCEL_MAX);
                    broadcast(rider, "You have increased your vehicle's acceleration by " + getTookitPower(self));
                    break;
                case 12:
                    currentValue = vehicle.getBankingAngle(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_BANKING);
                    broadcast(rider, "You have increased your vehicle's banking by " + getTookitPower(self));
                    break;
                case 4:
                    currentValue = vehicle.getTurnRateMax(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_TURN_RATE_MAX);
                    broadcast(rider, "You have increased your vehicle's turning by " + getTookitPower(self));
                    break;
                case 6:
                    currentValue = vehicle.getDecel(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_DECEL);
                    broadcast(rider, "You have increased your vehicle's deceleration by " + getTookitPower(self));
                    break;
                case 10:
                    currentValue = vehicle.getDampingHeight(veh);
                    currentValue += getTookitPower(self);
                    setValue(veh, currentValue, vehicle.VAR_DAMP_HEIGHT);
                    broadcast(rider, "You have increased your vehicle's damping height by " + getTookitPower(self));
                    break;
                default:
                    broadcast(rider, "This toolkit seems to be malfunctioning.");
                    break;
            }
            listAndSaveAllModifiers(self, player);
            obj_id creator = getCrafter(self);
            if (isIdValid(creator))
            {
                xp.grant(creator, "reverse_engineering", 1500);
                broadcast(creator, "You have been paid " + PAYOUT_AMOUNT + " credits for your vehicle tuning services.");
            }
            else
            {
                LOG("ethereal", "[Mechanic}: ERROR! NO creator found for toolkit xp payout. Continuing as normal.");
            }
            if (!isGod(player))
            {
                decrementCount(self);
            }
            else
            {
                debugConsoleMsg(player, "[GOD MODE]: Vehicle tuning applied, not decrementing!");
            }
            doCoolEffects(veh);
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU2)
        {
            if (!isGod(player))
            {
                return SCRIPT_CONTINUE;
            }
            listAndSaveAllModifiers(self, player);
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_MENU3)
        {
            if (isGod(player))
            {
                sui.listbox(self, player, "Select tool type.", sui.OK_CANCEL, "[GM] Mechanic", TOOLKIT_TYPES, "handleToolkitTypeSelect");
                return SCRIPT_CONTINUE;
            }
            else
            {
                broadcast(player, "You must be a god to use this function.");
                return SCRIPT_CONTINUE;
            }
        }
        if (item == menu_info_types.SERVER_MENU4)
        {
            if (isGod(player))
            {
                sui.inputbox(self, player, "Input tool modifer (float).", sui.OK_CANCEL, "[GM] Mechanic", sui.INPUT_NORMAL, null, "handleToolkitPowerSelect", null);
                return SCRIPT_CONTINUE;
            }
            else
            {
                broadcast(player, "You must be a god to use this function.");
                return SCRIPT_CONTINUE;
            }

        }
        if (item == menu_info_types.SERVER_MENU5)
        {
            removeAllObjVars(self);
            reInitToolkit(self);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    private void reInitToolkit(obj_id self)
    {
        //setName(self, "Vehicular Tool Kit");
        //setDescriptionString(self, "This toolkit can be used to modify your vehicle's performance. \n\nThe toolkit must be used near a garage, whether that be private or municipal.");
    }

    public float getTookitPower(obj_id self)
    {
        return getFloatObjVar(self, "mechanic.modifier");
    }

    public int getTookitType(obj_id self)
    {
        if (hasObjVar(self, "mechanic.toolkit.speed"))
        {
            return vehicle.VAR_SPEED_MAX;
        }
        if (hasObjVar(self, "mechanic.toolkit.height"))
        {
            return vehicle.VAR_HOVER_HEIGHT;
        }
        if (hasObjVar(self, "mechanic.toolkit.acceleration"))
        {
            return vehicle.VAR_ACCEL_MAX;
        }
        if (hasObjVar(self, "mechanic.toolkit.banking"))
        {
            return vehicle.VAR_BANKING;
        }
        if (hasObjVar(self, "mechanic.toolkit.turning"))
        {
            return vehicle.VAR_TURN_RATE_MIN;
        }
        if (hasObjVar(self, "mechanic.toolkit.deceleration"))
        {
            return vehicle.VAR_DECEL;
        }
        if (hasObjVar(self, "mechanic.toolkit.damping_height"))
        {
            return vehicle.VAR_DAMP_HEIGHT;
        }
        else
        {
            return 1; //default to speed_max
        }
    }

    public boolean isNearGarage(obj_id player) throws InterruptedException
    {
        if (isGod(player))
        {
            return true;
        }
        obj_id[] objects = getObjectsInRange(getLocation(player), 12f);
        for (obj_id object : objects)
        {
            if (getTemplateName(object).contains("object/building/player/player_garage"))
            {
                return true;
            }
            else if (getTemplateName(object).contains("parking_garage"))
            {
                return true;
            }
        }
        return false;
    }

    public void setToolKitType(obj_id self, String type) throws InterruptedException
    {
        setObjVar(self, "mechanic.toolkit." + type, true);
    }

    public void setToolkitPower(obj_id self, float power)
    {
        setObjVar(self, "mechanic.modifier", power);
    }

    public boolean isMunicipal(location loc) throws InterruptedException
    {
        obj_id[] objects = getObjectsInRange(loc, 115);
        for (obj_id object : objects)
        {
            if (getTemplateName(object).contains("object/building/player/city/"))
            {
                return true;
            }
        }
        return false;
    }

    public void listAndSaveAllModifiers(obj_id self, obj_id player) throws InterruptedException
    {
        //@TODO^: Implement the setting of the stats in the control device script so it calls the stats upon summoning the vehicle.
        String vehicleType = getShortenTemplateName(self, getMountId(player));
        if (!vehicle.isRidingVehicle(player))
        {
            broadcast(player, "You must be in your vehicle to save your diagnostics.");
        }
        String prompt = "Mechanic Summary:\n";
        obj_id veh_id = getMountId(player);
        obj_id vehicleControlDevice = callable.getCallableCD(veh_id);
        float minspeed = vehicle.getMinimumSpeed(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".minspeed", minspeed);
        prompt += "Minimum Speed: " + minspeed + "\n";
        float maxspeed = vehicle.getMaximumSpeed(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".maxspeed", maxspeed);
        prompt += "Maximum Speed: " + maxspeed + "\n";
        float height = vehicle.getHoverHeight(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".height", height);
        prompt += "Hover Height: " + height + "\n";
        float acceleration = vehicle.getAccelMin(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".acceleration", acceleration);
        prompt += "Acceleration (min): " + acceleration + "\n";
        float accelerationmax = vehicle.getAccelMax(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".accelerationmax", accelerationmax);
        prompt += "Acceleration (max): " + accelerationmax + "\n";
        float banking = vehicle.getBankingAngle(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".banking", banking);
        prompt += "Banking: " + banking + "\n";
        float turning = vehicle.getTurnRateMin(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".turning", turning);
        prompt += "Turning Rate: " + turning + "\n";
        float turning_max = vehicle.getTurnRateMax(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".turning_max", turning_max);
        prompt += "Turning Rate (max): " + turning_max + "\n";
        float deceleration = vehicle.getDecel(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".deceleration", deceleration);
        prompt += "Deceleration: " + deceleration + "\n";
        float glide = vehicle.getGlide(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".glide", glide);
        prompt += "Glide: " + glide + "\n";
        float autolevel = vehicle.getAutoLevelling(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".autolevel", autolevel);
        prompt += "Auto-Level: " + autolevel + "\n";
        float dampingheight = vehicle.getDampingHeight(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".dampingheight", dampingheight);
        prompt += "Damping Height: " + dampingheight + "\n";
        float dampingpitch = vehicle.getDampingPitch(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".dampingpitch", dampingpitch);
        prompt += "Damping Pitch: " + dampingpitch + "\n";
        float dampingroll = vehicle.getDampingRoll(veh_id);
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".dampingroll", dampingroll);
        prompt += "Damping Roll: " + dampingroll + "\n";
        boolean strafe = vehicle.getStrafe(veh_id);
        boolean lava = hasObjVar(veh_id, "vehicle.lava_resistance");
        setObjVar(vehicleControlDevice, MECHANIC_VAR + "." + vehicleType + ".strafe", strafe);
        prompt += "Strafe: " + strafe + "\n";
        prompt += "Lava Resistant: " + lava + "\n";
        int page = sui.msgbox(self, player, prompt, sui.OK_ONLY, "Vehicle: \"" + getPrettyName(vehicleType) + "\" Diagnostics", "noHandler");
        setSUIProperty(page, sui.MSGBOX_BTN_OK, sui.PROP_TEXT, "Acknowledge");
        broadcast(player, "Vehicle diagnostics saved.");
    }

    public String getPrettyName(String input) throws InterruptedException
    {
        String[] arr = input.split("_");
        StringBuffer sb = new StringBuffer();
        for (String s : arr)
        {
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(" ");
        }
        //remove object/mobile/vehicle/
        sb.delete(0, 22);
        return sb.toString().trim();
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(self, "mechanic.toolkit.speed"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Speed";
            idx++;
        }
        else if (hasObjVar(self, "mechanic.toolkit.height"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Height";
            idx++;
        }
        else if (hasObjVar(self, "mechanic.toolkit.acceleration"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Acceleration";
            idx++;
        }
        else if (hasObjVar(self, "mechanic.toolkit.banking"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Banking";
            idx++;
        }
        else if (hasObjVar(self, "mechanic.toolkit.turning"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Turning";
            idx++;
        }
        else if (hasObjVar(self, "mechanic.toolkit.deceleration"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Deceleration";
            idx++;
        }
        else if (hasObjVar(self, "mechanic.toolkit.damping_height"))
        {
            names[idx] = utils.packStringId(new string_id("Type"));
            attribs[idx] = "Vehicular Height Negotiation";
            idx++;
        }
        if (hasObjVar(self, "mechanic.modifier"))
        {
            float modifier = getFloatObjVar(self, "mechanic.modifier");
            names[idx] = utils.packStringId(new string_id("Power"));
            attribs[idx] = Float.toString(modifier);
            idx++;
        }
        names[idx] = utils.packStringId(new string_id("Status"));
        attribs[idx] = hasObjVar(self, "mechanic.modifier") ? "Ready" : "Not Configured";
        idx++;
        return SCRIPT_CONTINUE;
    }

    public int handleToolkitTypeSelect(obj_id self, dictionary webster) throws InterruptedException//@TODO: order these with the string array and implement power change sui
    {
        obj_id pOwner = sui.getPlayerId(webster);
        int bp = sui.getIntButtonPressed(webster);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(webster);
        switch (idx)
        {
            case 0:
                setToolKitType(self, "speed");
                break;
            case 1:
                setToolKitType(self, "height");
                break;
            case 2:
                setToolKitType(self, "acceleration");
                break;
            case 3:
                setToolKitType(self, "acceleration_max");
                break;
            case 4:
                setToolKitType(self, "banking");
                break;
            case 5:
                setToolKitType(self, "turning");
                break;
            case 6:
                setToolKitType(self, "turning_max");
                break;
            case 7:
                setToolKitType(self, "deceleration");
                break;
            case 8:
                setToolKitType(self, "glide");
                break;
            case 9:
                setToolKitType(self, "autolevelling");
                break;
            case 10:
                setToolKitType(self, "damping_height");
                break;
            case 11:
                setToolKitType(self, "damping_pitch");
                break;
            case 12:
                setToolKitType(self, "damping_roll");
                break;
            case 13:
                setToolKitType(self, "strafe");
                break;
        }
        broadcast(pOwner, "Toolkit type set to " + TOOLKIT_TYPES[idx] + ".");
        return SCRIPT_CONTINUE;
    }

    public int handleToolkitPowerSelect(obj_id self, dictionary webster) throws InterruptedException//@TODO: order these with the string array and implement power change sui
    {
        obj_id pOwner = sui.getPlayerId(webster);
        int bp = sui.getIntButtonPressed(webster);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String inputText = sui.getInputBoxText(webster);
        if (inputText == null || inputText.equals(""))
        {
            return SCRIPT_CONTINUE;
        }
        float power = Float.parseFloat(inputText);
        setToolkitPower(self, power);
        return SCRIPT_CONTINUE;
    }

    public boolean isNumbers(String str) throws InterruptedException
    {
        for (int i = 0; i < str.length(); i++)
        {
            if (!Character.isDigit(str.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    public String getShortenTemplateName(obj_id self, obj_id vehicle)
    {
        String template = getTemplateName(vehicle);
        template = template.replaceAll("object/mobile/vehicle/shared_", "");
        template = template.replaceAll(".iff", "");
        return template;
    }

    private void doCoolEffects(obj_id vehicle)
    {
        location corpsePosition = getLocation(vehicle);
        float radius = 4.0f;
        location[] offsets = new location[16];

        for (int i = 0; i < 16; i++)
        {
            float angle = (float) (i * Math.PI / 8);
            float xOffset = radius * (float) Math.cos(angle);
            float zOffset = radius * (float) Math.sin(angle);
            offsets[i] = new location(corpsePosition.x + xOffset, corpsePosition.y + 2.0f, corpsePosition.z + zOffset, corpsePosition.area, corpsePosition.cell);
        }

        for (location offset : offsets)
        {
            if (isInWorldCell(vehicle))
            {
                playClientEffectLoc(vehicle, "clienteffect/veh_thruster_damage_1.cef", offset, 1f);
            }
        }
    }
}