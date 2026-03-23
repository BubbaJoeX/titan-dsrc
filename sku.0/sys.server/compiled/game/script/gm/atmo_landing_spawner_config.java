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
    public static final int MENU_SET_LANDING_ALT = menu_info_types.SERVER_MENU10;
    public static final int MENU_SET_GUILD_TAG = menu_info_types.SERVER_MENU11;
    public static final int MENU_SET_REQ_FACTION = menu_info_types.SERVER_MENU12;
    public static final int MENU_SET_REQ_PROFESSIONS = menu_info_types.SERVER_MENU13;
    public static final int MENU_SET_LANDING_FEE = menu_info_types.SERVER_MENU14;
    public static final int MENU_SET_DOCK_GRACE = menu_info_types.SERVER_MENU15;
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
        mi.addSubMenu(configRoot, MENU_SET_LANDING_ALT, string_id.unlocalized("Set Landing Altitude"));
        mi.addSubMenu(configRoot, MENU_SET_GUILD_TAG, string_id.unlocalized("Req. Guild Tag"));
        mi.addSubMenu(configRoot, MENU_SET_REQ_FACTION, string_id.unlocalized("Req. Aligned Faction"));
        mi.addSubMenu(configRoot, MENU_SET_REQ_PROFESSIONS, string_id.unlocalized("Req. Professions (any)"));
        mi.addSubMenu(configRoot, MENU_SET_LANDING_FEE, string_id.unlocalized("Landing Fee (credits)"));
        mi.addSubMenu(configRoot, MENU_SET_DOCK_GRACE, string_id.unlocalized("Dock Grace (seconds)"));
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
        else if (item == MENU_SET_LANDING_ALT)
        {
            showSetLandingAltitudeUI(self, player);
        }
        else if (item == MENU_SET_GUILD_TAG)
        {
            showSetGuildTagUI(self, player);
        }
        else if (item == MENU_SET_REQ_FACTION)
        {
            showSetReqFactionUI(self, player);
        }
        else if (item == MENU_SET_REQ_PROFESSIONS)
        {
            showSetReqProfessionsUI(self, player);
        }
        else if (item == MENU_SET_LANDING_FEE)
        {
            showSetLandingFeeUI(self, player);
        }
        else if (item == MENU_SET_DOCK_GRACE)
        {
            showSetDockGraceUI(self, player);
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

    private void showSetLandingAltitudeUI(obj_id self, obj_id player) throws InterruptedException
    {
        String current = "";
        if (hasObjVar(self, atmo_landing_registry.OBJVAR_LANDING_ALTITUDE))
            current = String.valueOf(getFloatObjVar(self, atmo_landing_registry.OBJVAR_LANDING_ALTITUDE));

        String title = "Set Landing Altitude";
        String prompt = "World Y (meters) for autopilot touchdown.\n\nLeave empty and OK to clear (use fly-to location Y).\n\nCurrent: "
            + (current.isEmpty() ? "(from landing loc Y)" : current);

        sui.inputbox(self, player, prompt, title, "handleSetLandingAltitude", current);
    }

    public int handleSetLandingAltitude(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);

        if (bp != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        if (input == null)
            input = "";
        input = input.trim();

        if (input.isEmpty())
        {
            removeObjVar(self, atmo_landing_registry.OBJVAR_LANDING_ALTITUDE);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing altitude cleared -- autopilot will use landing location Y.");
            return SCRIPT_CONTINUE;
        }

        float alt;
        try
        {
            alt = Float.parseFloat(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Invalid number. Enter altitude in meters (e.g. 125.5).");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, atmo_landing_registry.OBJVAR_LANDING_ALTITUDE, alt);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing altitude set to Y = " + alt + " m");
        return SCRIPT_CONTINUE;
    }

    private void showSetGuildTagUI(obj_id self, obj_id player) throws InterruptedException
    {
        String cur = "";
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_GUILD_TAG))
            cur = getStringObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_GUILD_TAG);

        String title = "Required Guild Tag";
        String prompt = "Pilot's guild abbreviation or full name must match (case-insensitive).\n\nEmpty + OK clears.\n\nCurrent: "
            + (cur.isEmpty() ? "(none)" : cur);

        sui.inputbox(self, player, prompt, title, "handleSetGuildTag", cur);
    }

    public int handleSetGuildTag(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        if (input == null)
            input = "";
        input = input.trim();
        if (input.isEmpty())
        {
            removeObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_GUILD_TAG);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required guild tag cleared.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_GUILD_TAG, input);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required guild tag set: " + input);
        return SCRIPT_CONTINUE;
    }

    private void showSetReqFactionUI(obj_id self, obj_id player) throws InterruptedException
    {
        String cur = "";
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_ALIGNED_FACTION_NAME))
            cur = getStringObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_ALIGNED_FACTION_NAME);

        String title = "Required Aligned Faction";
        String prompt = "Enter rebel, imperial, or neutral (pvp aligned faction).\n\nEmpty + OK clears.\n\nCurrent: "
            + (cur.isEmpty() ? "(none)" : cur);

        sui.inputbox(self, player, prompt, title, "handleSetReqFaction", cur);
    }

    public int handleSetReqFaction(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        if (input == null)
            input = "";
        input = input.trim().toLowerCase();
        if (input.isEmpty())
        {
            removeObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_ALIGNED_FACTION_NAME);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required aligned faction cleared.");
            return SCRIPT_CONTINUE;
        }
        if (!input.equals("rebel") && !input.equals("imperial") && !input.equals("neutral"))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Use rebel, imperial, or neutral.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_ALIGNED_FACTION_NAME, input);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required aligned faction set: " + input);
        return SCRIPT_CONTINUE;
    }

    private void showSetReqProfessionsUI(obj_id self, obj_id player) throws InterruptedException
    {
        String cur = "";
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY))
        {
            int[] arr = getIntArrayObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY);
            if (arr != null && arr.length > 0)
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length; i++)
                {
                    if (i > 0)
                        sb.append(",");
                    sb.append(arr[i]);
                }
                cur = sb.toString();
            }
        }

        String title = "Required Professions (any match)";
        String prompt = "Comma-separated utils profession ints (e.g. 1=commando,2=smuggler,7=force).\nPilot must match at least one.\n\nEmpty + OK clears.\n\nCurrent: "
            + (cur.isEmpty() ? "(none)" : cur);

        sui.inputbox(self, player, prompt, title, "handleSetReqProfessions", cur);
    }

    public int handleSetReqProfessions(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        if (input == null)
            input = "";
        input = input.trim();
        if (input.isEmpty())
        {
            removeObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required professions cleared.");
            return SCRIPT_CONTINUE;
        }

        String[] parts = input.split(",");
        int[] out = new int[parts.length];
        int n = 0;
        for (String part : parts)
        {
            part = part.trim();
            if (part.isEmpty())
                continue;
            try
            {
                out[n++] = Integer.parseInt(part);
            }
            catch (NumberFormatException e)
            {
                sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Invalid number in list: " + part);
                return SCRIPT_CONTINUE;
            }
        }
        if (n == 0)
        {
            removeObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required professions cleared.");
            return SCRIPT_CONTINUE;
        }
        int[] trimmed = new int[n];
        System.arraycopy(out, 0, trimmed, 0, n);
        setObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY, trimmed);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Required professions set (" + n + " value(s)).");
        return SCRIPT_CONTINUE;
    }

    private void showSetLandingFeeUI(obj_id self, obj_id player) throws InterruptedException
    {
        String cur = "";
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_LANDING_FEE))
            cur = String.valueOf(getIntObjVar(self, atmo_landing_manager.OBJVAR_LANDING_FEE));

        String title = "Landing Fee (credits)";
        String prompt = "Base landing fee before city tax.\n\n-1 + OK = use script default minimum.\n\nCurrent: "
            + (cur.isEmpty() ? "(default)" : cur);

        sui.inputbox(self, player, prompt, title, "handleSetLandingFee", cur.isEmpty() ? "-1" : cur);
    }

    public int handleSetLandingFee(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        if (input == null)
            input = "";
        input = input.trim();
        if (input.isEmpty() || input.equals("-1"))
        {
            removeObjVar(self, atmo_landing_manager.OBJVAR_LANDING_FEE);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing fee cleared -- pad uses script default.");
            return SCRIPT_CONTINUE;
        }
        int fee;
        try
        {
            fee = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Invalid integer.");
            return SCRIPT_CONTINUE;
        }
        if (fee < 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Use a non-negative fee or -1 for default.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, atmo_landing_manager.OBJVAR_LANDING_FEE, fee);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Landing fee set: " + fee + " credits");
        return SCRIPT_CONTINUE;
    }

    private void showSetDockGraceUI(obj_id self, obj_id player) throws InterruptedException
    {
        String cur;
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_DOCK_GRACE_SECONDS))
            cur = String.valueOf(getIntObjVar(self, atmo_landing_manager.OBJVAR_DOCK_GRACE_SECONDS));
        else
            cur = "(default " + atmo_landing_manager.DEFAULT_DOCK_GRACE_SECONDS + " s)";

        String title = "Dock Grace (seconds)";
        String prompt = "Seconds after paid mooring time (dockExpiry) before forced auto-departure.\n"
            + "Extensions at the ship terminal still work during this buffer.\n\n"
            + "0 = no grace. Empty + OK = clear override.\n\nCurrent: " + cur;

        sui.inputbox(self, player, prompt, title, "handleSetDockGrace", hasObjVar(self, atmo_landing_manager.OBJVAR_DOCK_GRACE_SECONDS) ? String.valueOf(getIntObjVar(self, atmo_landing_manager.OBJVAR_DOCK_GRACE_SECONDS)) : "");
    }

    public int handleSetDockGrace(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;

        String input = sui.getInputBoxText(params);
        if (input == null)
            input = "";
        input = input.trim();
        if (input.isEmpty())
        {
            removeObjVar(self, atmo_landing_manager.OBJVAR_DOCK_GRACE_SECONDS);
            sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Dock grace cleared -- pads use default (" + atmo_landing_manager.DEFAULT_DOCK_GRACE_SECONDS + " s).");
            return SCRIPT_CONTINUE;
        }
        int sec;
        try
        {
            sec = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Enter a whole number of seconds (0 or more).");
            return SCRIPT_CONTINUE;
        }
        if (sec < 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[GM]: Grace cannot be negative.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, atmo_landing_manager.OBJVAR_DOCK_GRACE_SECONDS, sec);
        sendSystemMessageTestingOnly(player, "\\#00ff88[GM]: Dock grace set: " + sec + " s after mooring expiry");
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
            if (hasObjVar(self, atmo_landing_registry.OBJVAR_LANDING_ALTITUDE))
                sendSystemMessageTestingOnly(player, "\\#aaddff  Landing altitude Y: " + getFloatObjVar(self, atmo_landing_registry.OBJVAR_LANDING_ALTITUDE) + " m (override)");
            else
                sendSystemMessageTestingOnly(player, "\\#778899  Landing altitude Y: (uses location Y: " + Math.round(loc.y) + ")");
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

        if (hasObjVar(self, atmo_landing_manager.OBJVAR_DOCK_DURATION_OVERRIDE))
        {
            int pol = getIntObjVar(self, atmo_landing_manager.OBJVAR_DOCK_DURATION_OVERRIDE);
            sendSystemMessageTestingOnly(player, "\\#aaddff  Policy dock override: " + (pol == -1 ? "Unlimited" : (pol + " sec")));
        }

        if (hasObjVar(self, atmo_landing_manager.OBJVAR_ACCESS_MODE))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Access mode: " + getIntObjVar(self, atmo_landing_manager.OBJVAR_ACCESS_MODE) + " (0 public, 1 faction list, 2 guild id, 3 allowlist, 4 GM)");
        if (atmo_landing_manager.isOwnerPilotOnly(self))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Owner pilot only: yes");

        if (hasObjVar(self, atmo_landing_manager.OBJVAR_LANDING_FEE))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Landing fee (policy): " + getIntObjVar(self, atmo_landing_manager.OBJVAR_LANDING_FEE) + " cr");
        if (atmo_landing_manager.shouldWaiveLandingFee(self))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Waive landing fee: yes");
        if (atmo_landing_manager.shouldIgnoreCityLandingTax(self))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Ignore city landing tax: yes");

        if (hasObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_GUILD_TAG))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Req. guild tag: " + getStringObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_GUILD_TAG));
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_ALIGNED_FACTION_NAME))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Req. aligned faction: " + getStringObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_ALIGNED_FACTION_NAME));
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY))
        {
            int[] rp = getIntArrayObjVar(self, atmo_landing_manager.OBJVAR_REQUIRED_PROFESSIONS_ANY);
            if (rp != null && rp.length > 0)
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rp.length; i++)
                {
                    if (i > 0)
                        sb.append(",");
                    sb.append(rp[i]);
                }
                sendSystemMessageTestingOnly(player, "\\#aaddff  Req. professions (any): " + sb);
            }
        }

        if (hasObjVar(self, atmo_landing_manager.OBJVAR_EXTEND_DOCK_CREDITS))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Extend dock credits override: " + getIntObjVar(self, atmo_landing_manager.OBJVAR_EXTEND_DOCK_CREDITS));
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_EXTEND_DOCK_SECONDS))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Extend dock seconds override: " + getIntObjVar(self, atmo_landing_manager.OBJVAR_EXTEND_DOCK_SECONDS));
        if (hasObjVar(self, atmo_landing_manager.OBJVAR_ALLOW_EXTEND_DOCK) && !getBooleanObjVar(self, atmo_landing_manager.OBJVAR_ALLOW_EXTEND_DOCK))
            sendSystemMessageTestingOnly(player, "\\#aaddff  Extend dock: disabled");

        sendSystemMessageTestingOnly(player, "\\#aaddff  Dock grace (after mooring): " + atmo_landing_manager.getDockGraceSeconds(self) + " s");

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
