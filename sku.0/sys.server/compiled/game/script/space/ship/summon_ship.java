package script.space.ship;

import script.*;
import script.library.space_transition;
import script.library.space_utils;
import script.library.utils;
import script.library.sui;
import script.space.atmo.atmo_landing_registry;
import script.space.combat.combat_ship;

import java.util.Vector;

public class summon_ship extends script.base_script
{
    public static final float SUMMON_TAKEOFF_ALT  = 500.0f;
    public static final float SUMMON_LANDING_ALT  = 50.0f;

    public static final int MENU_SUMMON_SHIP = menu_info_types.SERVER_MENU1;
    public static final int MENU_LAND_AT_POINT = menu_info_types.SERVER_MENU2;
    public static final int MENU_FOLLOW_ENABLE = menu_info_types.SERVER_MENU3;
    public static final int MENU_FOLLOW_DISABLE = menu_info_types.SERVER_MENU4;

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        if (utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id ship = findDeployedShipForPlayer(player);
        if (!isIdValid(ship))
            return SCRIPT_CONTINUE;

        if (space_transition.getContainingShip(player) == ship)
        {
            mi.addRootMenu(MENU_LAND_AT_POINT, string_id.unlocalized("Land at Landing Point"));
            return SCRIPT_CONTINUE;
        }

        mi.addRootMenu(MENU_SUMMON_SHIP, string_id.unlocalized("Summon Ship"));
        mi.addRootMenu(MENU_LAND_AT_POINT, string_id.unlocalized("Remote Land at Point"));

        boolean followOn = hasObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE)
            && getBooleanObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE);
        if (followOn)
            mi.addRootMenu(MENU_FOLLOW_DISABLE, string_id.unlocalized("Disable Auto-Follow Ship"));
        else
            mi.addRootMenu(MENU_FOLLOW_ENABLE, string_id.unlocalized("Enable Auto-Follow Ship"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_SUMMON_SHIP)
        {
            return handleSummonShip(self, player);
        }
        else if (item == MENU_LAND_AT_POINT)
        {
            return handleLandAtPoint(self, player);
        }
        else if (item == MENU_FOLLOW_ENABLE)
        {
            return handleEnableAutoFollow(self, player);
        }
        else if (item == MENU_FOLLOW_DISABLE)
        {
            return handleDisableAutoFollow(self, player);
        }

        return SCRIPT_CONTINUE;
    }

    private int handleSummonShip(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessageTestingOnly(player, "You can only summon your ship during atmospheric flight.");
            return SCRIPT_CONTINUE;
        }

        if (utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id ship = findDeployedShipForPlayer(player);
        if (!isIdValid(ship))
        {
            sendSystemMessageTestingOnly(player, "You do not have a ship deployed in this area.");
            return SCRIPT_CONTINUE;
        }

        if (space_transition.getContainingShip(player) == ship)
        {
            sendSystemMessageTestingOnly(player, "You are already aboard this ship.");
            return SCRIPT_CONTINUE;
        }

        if (!space_utils.isShipWithInterior(ship))
        {
            sendSystemMessageTestingOnly(player, "Only ships with an interior can be summoned via auto-pilot.");
            return SCRIPT_CONTINUE;
        }

        if (shipIsAutopilotActive(ship))
        {
            sendSystemMessageTestingOnly(player, "Your ship is already en route. Please wait for it to arrive.");
            return SCRIPT_CONTINUE;
        }

        obj_id pilot = getPilotId(ship);
        if (isIdValid(pilot))
        {
            sendSystemMessageTestingOnly(player, "Your ship is currently being piloted and cannot be summoned.");
            return SCRIPT_CONTINUE;
        }

        location playerLoc = getWorldLocation(player);
        float targetX = playerLoc.x;
        float targetZ = playerLoc.z;

        dictionary wpParams = new dictionary();
        wpParams.put("x", targetX);
        wpParams.put("z", targetZ);
        wpParams.put("owner", player);
        wpParams.put("summon", true);
        messageTo(ship, "shipSummonEngage", wpParams, 0, false);

        sendSystemMessageTestingOnly(player, "Summoning your ship to your location...");
        return SCRIPT_CONTINUE;
    }

    private int handleEnableAutoFollow(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessageTestingOnly(player, "You can only use auto-follow during atmospheric flight.");
            return SCRIPT_CONTINUE;
        }

        if (utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id ship = findDeployedShipForPlayer(player);
        if (!isIdValid(ship))
        {
            sendSystemMessageTestingOnly(player, "You do not have a ship deployed in this area.");
            return SCRIPT_CONTINUE;
        }

        if (space_transition.getContainingShip(player) == ship)
        {
            sendSystemMessageTestingOnly(player, "Leave the ship to change auto-follow from your datapad.");
            return SCRIPT_CONTINUE;
        }

        if (!space_utils.isShipWithInterior(ship))
        {
            sendSystemMessageTestingOnly(player, "Only ships with an interior support auto-follow.");
            return SCRIPT_CONTINUE;
        }

        if (shipIsAutopilotActive(ship))
        {
            sendSystemMessageTestingOnly(player, "Your ship is already en route. Wait for it to arrive, then enable auto-follow.");
            return SCRIPT_CONTINUE;
        }

        obj_id pilot = getPilotId(ship);
        if (isIdValid(pilot))
        {
            sendSystemMessageTestingOnly(player, "Your ship is being piloted and cannot enter auto-follow.");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE) && getBooleanObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE))
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Navicomputer]: Auto-follow is already active.");
            return SCRIPT_CONTINUE;
        }

        String title = "Auto-Follow Ship";
        String prompt = "Your ship will hold high orbit near you and re-position as you move.\\n\\n"
            + "Cost: " + combat_ship.SUMMON_FOLLOW_COST_PER_HOUR + " credits per hour (first hour charged when you confirm).\\n\\n"
            + "Enable auto-follow?";

        utils.setScriptVar(player, "summon.follow.ship", ship);
        sui.msgbox(self, player, prompt, sui.YES_NO, title, "handleSummonFollowConfirm");
        return SCRIPT_CONTINUE;
    }

    public int handleSummonFollowConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        obj_id player = sui.getPlayerId(params);
        if (bp != sui.BP_OK || !isIdValid(player))
        {
            utils.removeScriptVar(player, "summon.follow.ship");
            return SCRIPT_CONTINUE;
        }

        obj_id ship = utils.getObjIdScriptVar(player, "summon.follow.ship");
        utils.removeScriptVar(player, "summon.follow.ship");

        if (!isAtmosphericFlightScene() || utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id current = findDeployedShipForPlayer(player);
        if (!isIdValid(ship) || !exists(ship) || ship != current)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Navicomputer]: Ship no longer available for auto-follow.");
            return SCRIPT_CONTINUE;
        }

        if (space_transition.getContainingShip(player) == ship)
            return SCRIPT_CONTINUE;

        if (!space_utils.isShipWithInterior(ship) || shipIsAutopilotActive(ship))
            return SCRIPT_CONTINUE;

        if (isIdValid(getPilotId(ship)))
            return SCRIPT_CONTINUE;

        dictionary d = new dictionary();
        d.put("owner", player);
        messageTo(ship, "summonFollowEnable", d, 0, false);
        return SCRIPT_CONTINUE;
    }

    private int handleDisableAutoFollow(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessageTestingOnly(player, "You can only change auto-follow during atmospheric flight.");
            return SCRIPT_CONTINUE;
        }

        if (utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id ship = findDeployedShipForPlayer(player);
        if (!isIdValid(ship))
        {
            sendSystemMessageTestingOnly(player, "You do not have a ship deployed in this area.");
            return SCRIPT_CONTINUE;
        }

        if (space_transition.getContainingShip(player) == ship)
        {
            sendSystemMessageTestingOnly(player, "Leave the ship to change auto-follow from your datapad.");
            return SCRIPT_CONTINUE;
        }

        if (!hasObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE) || !getBooleanObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE))
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Navicomputer]: Auto-follow is not active.");
            return SCRIPT_CONTINUE;
        }

        dictionary d = new dictionary();
        d.put("owner", player);
        messageTo(ship, "summonFollowDisable", d, 0, false);
        return SCRIPT_CONTINUE;
    }

    private int handleLandAtPoint(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessageTestingOnly(player, "You can only land at a landing point during atmospheric flight.");
            return SCRIPT_CONTINUE;
        }

        obj_id ship = findDeployedShipForPlayer(player);
        if (!isIdValid(ship))
        {
            sendSystemMessageTestingOnly(player, "You do not have a ship deployed in this area.");
            return SCRIPT_CONTINUE;
        }

        if (!space_utils.isShipWithInterior(ship))
        {
            sendSystemMessageTestingOnly(player, "Only ships with an interior can land at landing points.");
            return SCRIPT_CONTINUE;
        }

        if (shipIsAutopilotActive(ship))
        {
            sendSystemMessageTestingOnly(player, "Your ship is already en route. Please wait for it to arrive.");
            return SCRIPT_CONTINUE;
        }

        obj_id[] landingPoints = atmo_landing_registry.getAllLandingPointsInScene();
        if (landingPoints == null || landingPoints.length == 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Landing Control]: No landing points available on this planet.");
            return SCRIPT_CONTINUE;
        }

        Vector entries = new Vector();
        Vector pointIds = new Vector();

        for (obj_id lp : landingPoints)
        {
            if (!isIdValid(lp) || !exists(lp))
                continue;

            String name = atmo_landing_registry.getLandingPointName(lp);
            boolean occupied = atmo_landing_registry.isOccupied(lp);
            boolean enRoute = atmo_landing_registry.isEnRoute(lp);

            String status;
            if (occupied)
                status = "\\#ff4444" + name + " (OCCUPIED)";
            else if (enRoute)
                status = "\\#ffaa44" + name + " (RESERVED)";
            else
                status = "\\#88ff88" + name + " (AVAILABLE)";

            entries.add(status);
            pointIds.add(lp);
        }

        if (entries.size() == 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Landing Control]: No landing points available.");
            return SCRIPT_CONTINUE;
        }

        String title = "Select Landing Point";
        String prompt = "Choose a landing point for your ship.\\nGreen = Available, Yellow = Reserved, Red = Occupied";

        int pid = sui.listbox(self, player, prompt, sui.OK_CANCEL, title, entries, "handleLandingPointSelection");
        if (pid > -1)
        {
            utils.setScriptVar(player, "landing.ship", ship);
            utils.setBatchScriptVar(player, "landing.points", pointIds);
        }

        return SCRIPT_CONTINUE;
    }

    public int handleLandingPointSelection(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);

        if (bp != sui.BP_OK)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        int selectedRow = sui.getListboxSelectedRow(params);
        if (selectedRow < 0)
        {
            cleanupScriptVars(player);
            return SCRIPT_CONTINUE;
        }

        obj_id ship = utils.getObjIdScriptVar(player, "landing.ship");
        Vector pointIds = utils.getResizeableObjIdBatchScriptVar(player, "landing.points");
        cleanupScriptVars(player);

        if (!isIdValid(ship) || !exists(ship))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Landing Control]: Ship not found.");
            return SCRIPT_CONTINUE;
        }

        if (pointIds == null || selectedRow >= pointIds.size())
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Landing Control]: Invalid selection.");
            return SCRIPT_CONTINUE;
        }

        obj_id landingPoint = (obj_id) pointIds.get(selectedRow);
        if (!isIdValid(landingPoint) || !exists(landingPoint))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Landing Control]: Landing point no longer exists.");
            return SCRIPT_CONTINUE;
        }

        if (atmo_landing_registry.isOccupied(landingPoint))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Landing Control]: This landing point is occupied.");
            return SCRIPT_CONTINUE;
        }

        if (atmo_landing_registry.isEnRoute(landingPoint))
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Landing Control]: Another ship is already en route to this landing point.");
            return SCRIPT_CONTINUE;
        }

        dictionary landingParams = new dictionary();
        landingParams.put("ship", ship);
        landingParams.put("pilot", player);
        messageTo(landingPoint, "handleLandingRequest", landingParams, 0, false);

        return SCRIPT_CONTINUE;
    }

    private void cleanupScriptVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, "landing.ship");
        utils.removeBatchScriptVar(player, "landing.points");
    }

    private obj_id findDeployedShipForPlayer(obj_id player) throws InterruptedException
    {
        obj_id[] scds = space_transition.findShipControlDevicesForPlayer(player);
        if (scds == null)
            return null;

        for (obj_id scd : scds)
        {
            if (!isIdValid(scd))
                continue;

            obj_id[] contents = getContents(scd);
            if (contents != null && contents.length > 0)
                continue;

            if (!hasObjVar(scd, "ship"))
                continue;

            obj_id ship = getObjIdObjVar(scd, "ship");
            if (isIdValid(ship) && exists(ship))
                return ship;
        }
        return null;
    }
}
