package script.gm;

import script.*;
import script.library.*;
import script.space.atmo.*;

/**
 * GM script to configure atmospheric landing point spawn eggs via radial menus.
 * Attach to any spawn egg to enable landing point configuration.
 */
public class atmo_landing_spawner_config extends script.base_script
{
    public static final String LANDING_POINT_SCRIPT = "space.atmo.atmo_landing_point";

    public static final int MENU_CONFIGURE = menu_info_types.SERVER_MENU1;
    public static final int MENU_SET_NAME = menu_info_types.SERVER_MENU2;
    public static final int MENU_SET_LOC = menu_info_types.SERVER_MENU3;
    public static final int MENU_SET_DISEMBARK = menu_info_types.SERVER_MENU4;
    public static final int MENU_SET_YAW = menu_info_types.SERVER_MENU5;
    public static final int MENU_SET_TIME = menu_info_types.SERVER_MENU6;
    public static final int MENU_SHOW_CONFIG = menu_info_types.SERVER_MENU7;
    public static final int MENU_CLEAR_CONFIG = menu_info_types.SERVER_MENU8;
    public static final int MENU_APPLY = menu_info_types.SERVER_MENU9;

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isGod(player))
            return SCRIPT_CONTINUE;

        int configRoot = mi.addRootMenu(MENU_CONFIGURE, string_id.unlocalized("Configure Landing Point"));

        mi.addSubMenu(configRoot, MENU_SET_NAME, string_id.unlocalized("Set Name"));
        mi.addSubMenu(configRoot, MENU_SET_LOC, string_id.unlocalized("Set Location (From Position)"));
        mi.addSubMenu(configRoot, MENU_SET_DISEMBARK, string_id.unlocalized("Set Disembark Location"));
        mi.addSubMenu(configRoot, MENU_SET_YAW, string_id.unlocalized("Set Yaw Angle"));
        mi.addSubMenu(configRoot, MENU_SET_TIME, string_id.unlocalized("Set Time Limit"));
        mi.addSubMenu(configRoot, MENU_SHOW_CONFIG, string_id.unlocalized("Show Configuration"));
        mi.addSubMenu(configRoot, MENU_CLEAR_CONFIG, string_id.unlocalized("Clear Configuration"));
        mi.addSubMenu(configRoot, MENU_APPLY, string_id.unlocalized("Apply & Activate"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isGod(player))
            return SCRIPT_CONTINUE;

        if (item == menu_info_types.SERVER_MENU2)
        {
            showSetNameUI(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU3)
        {
            setLocationFromPlayer(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU4)
        {
            setDisembarkFromPlayer(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU5)
        {
            showSetYawUI(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU6)
        {
            showSetTimeUI(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU7)
        {
            showCurrentConfig(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU8)
        {
            clearConfig(self, player);
        }
        else if (item == menu_info_types.SERVER_MENU9)
        {
            applyConfig(self, player);
        }

        return SCRIPT_CONTINUE;
    }

    private void showSetNameUI(obj_id self, obj_id player) throws InterruptedException
    {
        String currentName = "";
        if (hasObjVar(self, atmo_landing_registry.OBJVAR_NAME))
            currentName = getStringObjVar(self, atmo_landing_registry.OBJVAR_NAME);

        String title = "Set Landing Point Name";
        String prompt = "Enter the name for this landing point (e.g., 'Docking Bay 327'):";

        sui.inputbox(self, player, prompt, title, "handleSetName", currentName);
    }

    public int handleSetName(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);

        if (bp != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String name = sui.getInputBoxText(params);
        if (name == null || name.isEmpty())
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Name cannot be empty.");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, atmo_landing_registry.OBJVAR_NAME, name);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing point name set to: " + name);

        return SCRIPT_CONTINUE;
    }

    private void setLocationFromPlayer(obj_id self, obj_id player) throws InterruptedException
    {
        location playerLoc = getLocation(player);

        obj_id ship = space_transition.getContainingShip(player);
        if (isIdValid(ship))
        {
            playerLoc = getLocation(ship);
        }

        setObjVar(self, atmo_landing_registry.OBJVAR_LOC, playerLoc);

        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing location set to your current position:");
        sendSystemMessageTestingOnly(player, "\\#aaddff  X: " + Math.round(playerLoc.x) + ", Y: " + Math.round(playerLoc.y) + ", Z: " + Math.round(playerLoc.z));
    }

    private void setDisembarkFromPlayer(obj_id self, obj_id player) throws InterruptedException
    {
        location playerLoc = getLocation(player);

        setObjVar(self, atmo_landing_registry.OBJVAR_DISEMBARK_LOC, playerLoc);

        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Disembark location set to your current position:");
        sendSystemMessageTestingOnly(player, "\\#aaddff  X: " + Math.round(playerLoc.x) + ", Y: " + Math.round(playerLoc.y) + ", Z: " + Math.round(playerLoc.z));
    }

    private void showSetYawUI(obj_id self, obj_id player) throws InterruptedException
    {
        float currentYaw = 0.0f;
        if (hasObjVar(self, atmo_landing_registry.OBJVAR_YAW))
            currentYaw = getFloatObjVar(self, atmo_landing_registry.OBJVAR_YAW);

        String title = "Set Landing Yaw";
        String prompt = "Enter the yaw angle in degrees (0-360):\n\nCurrent yaw: " + currentYaw + " degrees\n\nTip: Use /getYaw command on your ship to get current heading.";

        int pid = sui.inputbox(self, player, prompt, title, "handleSetYaw", String.valueOf(currentYaw));
    }

    public int handleSetYaw(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);

        if (bp != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        float yaw = 0.0f;

        try
        {
            yaw = Float.parseFloat(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Invalid yaw value. Please enter a number.");
            return SCRIPT_CONTINUE;
        }

        while (yaw < 0) yaw += 360;
        while (yaw >= 360) yaw -= 360;

        setObjVar(self, atmo_landing_registry.OBJVAR_YAW, yaw);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing yaw set to: " + yaw + " degrees");

        return SCRIPT_CONTINUE;
    }

    private void showSetTimeUI(obj_id self, obj_id player) throws InterruptedException
    {
        int currentTime = -1;
        if (hasObjVar(self, atmo_landing_registry.OBJVAR_TIME_TO_DISEMBARK))
            currentTime = getIntObjVar(self, atmo_landing_registry.OBJVAR_TIME_TO_DISEMBARK);

        String title = "Set Docking Time Limit";
        String prompt = "Enter the time limit in seconds (or -1 for unlimited):\n\nCurrent: " + (currentTime == -1 ? "Unlimited" : (currentTime + " seconds"));

        int pid = sui.inputbox(self, player, prompt, title, "handleSetTime", String.valueOf(currentTime));
    }

    public int handleSetTime(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);

        if (bp != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        int time = -1;

        try
        {
            time = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Invalid time value. Please enter a number.");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, atmo_landing_registry.OBJVAR_TIME_TO_DISEMBARK, time);

        if (time == -1)
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Docking time set to: Unlimited");
        else
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Docking time set to: " + time + " seconds");

        return SCRIPT_CONTINUE;
    }

    private void showCurrentConfig(obj_id self, obj_id player) throws InterruptedException
    {
        sendSystemMessageTestingOnly(player, "\\#00ccff========================================");
        sendSystemMessageTestingOnly(player, "\\#00ccff Landing Point Configuration");
        sendSystemMessageTestingOnly(player, "\\#00ccff========================================");

        if (hasObjVar(self, atmo_landing_registry.OBJVAR_NAME))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Name: " + getStringObjVar(self, atmo_landing_registry.OBJVAR_NAME));
        else
            sendSystemMessageTestingOnly(player, "\\#ff4444  Name: (NOT SET)");

        if (hasObjVar(self, atmo_landing_registry.OBJVAR_LOC))
        {
            location loc = getLocationObjVar(self, atmo_landing_registry.OBJVAR_LOC);
            sendSystemMessageTestingOnly(player, "\\#aaddff  Location: [" + Math.round(loc.x) + ", " + Math.round(loc.y) + ", " + Math.round(loc.z) + "]");
        }
        else
            sendSystemMessageTestingOnly(player, "\\#ff4444  Location: (NOT SET)");

        if (hasObjVar(self, atmo_landing_registry.OBJVAR_DISEMBARK_LOC))
        {
            location loc = getLocationObjVar(self, atmo_landing_registry.OBJVAR_DISEMBARK_LOC);
            sendSystemMessageTestingOnly(player, "\\#aaddff  Disembark: [" + Math.round(loc.x) + ", " + Math.round(loc.y) + ", " + Math.round(loc.z) + "]");
        }
        else
            sendSystemMessageTestingOnly(player, "\\#778899  Disembark: (Using landing location)");

        if (hasObjVar(self, atmo_landing_registry.OBJVAR_YAW))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Yaw: " + getFloatObjVar(self, atmo_landing_registry.OBJVAR_YAW) + " degrees");
        else
            sendSystemMessageTestingOnly(player, "\\#778899  Yaw: 0 degrees (default)");

        if (hasObjVar(self, atmo_landing_registry.OBJVAR_TIME_TO_DISEMBARK))
        {
            int time = getIntObjVar(self, atmo_landing_registry.OBJVAR_TIME_TO_DISEMBARK);
            sendSystemMessageTestingOnly(player, "\\#aaddff  Time Limit: " + (time == -1 ? "Unlimited" : (time + " seconds")));
        }
        else
            sendSystemMessageTestingOnly(player, "\\#778899  Time Limit: Unlimited (default)");

        boolean hasScript = hasScript(self, LANDING_POINT_SCRIPT);
        sendSystemMessageTestingOnly(player, "\\#aaddff  Script Attached: " + (hasScript ? "Yes" : "No"));

        if (atmo_landing_registry.isLandingPoint(self))
        {
            sendSystemMessageTestingOnly(player, "\\#00ff88  Status: VALID - Ready to register");
        }
        else
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444  Status: INCOMPLETE - Need name and location");
        }

        sendSystemMessageTestingOnly(player, "\\#00ccff========================================");
    }

    private void clearConfig(obj_id self, obj_id player) throws InterruptedException
    {
        atmo_landing_registry.clearLandingPointConfig(self);

        if (hasScript(self, LANDING_POINT_SCRIPT))
            detachScript(self, LANDING_POINT_SCRIPT);

        sendSystemMessageTestingOnly(player, "\\#ffaa44[GM]: Landing point configuration cleared.");
    }

    private void applyConfig(obj_id self, obj_id player) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(self))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Configuration incomplete. Name and location are required.");
            return;
        }

        if (!hasScript(self, LANDING_POINT_SCRIPT))
        {
            attachScript(self, LANDING_POINT_SCRIPT);
        }

        boolean registered = atmo_landing_registry.registerOnMap(self);
        if (registered)
        {
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing point configured and registered on planet map!");
        }
        else
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[GM]: Landing point configured but failed to register on map.");
        }

        String name = atmo_landing_registry.getLandingPointName(self);
        sendSystemMessageTestingOnly(player, "\\#aaddff  Landing point '" + name + "' is now active.");
    }
}
