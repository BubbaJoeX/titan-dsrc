package script.space.ship;

import script.*;
import script.library.space_transition;
import script.library.space_turret;
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

    private static final int MENU_STARSHIP_REMOTE = menu_info_types.SERVER_MENU1;
    /** Ground targeting ({@link #OnGroundTargetLoc}) — same menu type as other datapad ground-pick tools. */
    private static final int MENU_GROUND_BOMBARD = menu_info_types.ITEM_USE;

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        if (utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        if (!isIdValid(findDeployedShipForPlayer(player)))
            return SCRIPT_CONTINUE;

        mi.addRootMenu(MENU_STARSHIP_REMOTE, string_id.unlocalized("Starship Remote"));
        mi.addRootMenu(MENU_GROUND_BOMBARD, string_id.unlocalized("Bombard ground target"));
        return SCRIPT_CONTINUE;
    }

    public int OnGroundTargetLoc(obj_id self, obj_id player, int menuItem, float x, float y, float z) throws InterruptedException
    {
        if (menuItem != MENU_GROUND_BOMBARD)
            return SCRIPT_CONTINUE;

        if (!isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        if (utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id ship = findDeployedShipForPlayer(player);
        if (!isIdValid(ship))
            return SCRIPT_CONTINUE;

        location aim = space_turret.locationFromGroundPick(self, player, x, y, z);
        int r = space_turret.fireOrbitalStrikeAtGroundPick(ship, player, aim, combat_ship.SUMMON_BOMBARDMENT_INSTANT_HORIZONTAL_RANGE);
        if (r == space_turret.ORBITAL_FIRE_NOT_ELIGIBLE)
        {
            sendSystemMessageTestingOnly(player, "\\#88ff88[Navicomputer]: Enable bombardment orbit from Starship Remote on your datapad first, then pick a ground target to fire.");
        }
        else if (r == space_turret.ORBITAL_FIRE_TOO_FAR)
        {
            sendSystemMessageTestingOnly(player, "\\#88ff88[Navicomputer]: Ship too far — move within "
                + (int) combat_ship.SUMMON_BOMBARDMENT_INSTANT_HORIZONTAL_RANGE + " m horizontal of this point or bring the ship closer.");
        }
        else if (r > 0)
        {
            sendSystemMessageTestingOnly(player, "\\#88ff88[Navicomputer]: Fired " + r + " turret shot(s) (" + combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT + " cr each).");
        }
        else
        {
            sendSystemMessageTestingOnly(player, "\\#88ff88[Navicomputer]: In range; no shots fired (turrets not ready, arcs, or insufficient credits).");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_STARSHIP_REMOTE)
            return openStarshipRemoteListbox(self, player);
        return SCRIPT_CONTINUE;
    }

    private int openStarshipRemoteListbox(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessageTestingOnly(player, "You can only use starship remote during atmospheric flight.");
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

        boolean aboard = space_transition.getContainingShip(player) == ship;
        boolean followOn = hasObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE)
            && getBooleanObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE);
        boolean bombardmentOn = followOn && hasObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE)
            && getBooleanObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE);
        boolean normalFollowOn = followOn && !bombardmentOn;

        Vector entries = new Vector();
        Vector actions = new Vector();

        if (aboard)
        {
            entries.add("Land at landing point…");
            actions.add("land");
        }
        else
        {
            entries.add("Summon ship to my location");
            actions.add("summon");

            if (bombardmentOn)
            {
                entries.add("Return to landing altitude (ends bombardment orbit)");
                actions.add("return_landing");
                entries.add("Disable bombardment orbit");
                actions.add("follow_disable");
            }
            else if (normalFollowOn)
            {
                entries.add("Return to landing altitude (ends auto-follow)");
                actions.add("return_landing");
                entries.add("Disable auto-follow");
                actions.add("follow_disable");
            }
            else
            {
                entries.add("Enable auto-follow — high orbit (" + combat_ship.SUMMON_FOLLOW_COST_PER_HOUR + " cr/hr)");
                actions.add("follow_enable");
                entries.add("Enable bombardment orbit — " + combat_ship.SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST + " cr + "
                    + combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT + " cr per strike");
                actions.add("bombardment_enable");
            }

            entries.add("Remote land at landing point…");
            actions.add("remote_land");
        }

        String[] actionArr = new String[actions.size()];
        for (int i = 0; i < actions.size(); i++)
            actionArr[i] = (String) actions.get(i);

        utils.setScriptVar(player, "summon.menu.ship", ship);
        utils.setBatchScriptVar(player, "summon.menu.actions", actionArr);

        String title = "Starship Remote";
        String prompt = "Select an action for your deployed ship.";

        sui.listbox(self, player, prompt, sui.OK_CANCEL, title, entries, "handleSummonMenuSelection");
        return SCRIPT_CONTINUE;
    }

    public int handleSummonMenuSelection(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);

        if (bp != sui.BP_OK)
        {
            cleanupSummonMenuVars(player);
            return SCRIPT_CONTINUE;
        }

        int row = sui.getListboxSelectedRow(params);
        if (row < 0)
        {
            cleanupSummonMenuVars(player);
            return SCRIPT_CONTINUE;
        }

        obj_id ship = utils.getObjIdScriptVar(player, "summon.menu.ship");
        String[] actionArr = utils.getStringBatchScriptVar(player, "summon.menu.actions");
        cleanupSummonMenuVars(player);

        if (!isAtmosphericFlightScene() || utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id current = findDeployedShipForPlayer(player);
        if (!isIdValid(ship) || !exists(ship) || ship != current)
        {
            sendSystemMessageTestingOnly(player, "Your deployed ship is no longer available.");
            return SCRIPT_CONTINUE;
        }

        if (actionArr == null || row >= actionArr.length)
            return SCRIPT_CONTINUE;

        String action = actionArr[row];

        if (action.equals("land"))
            return handleLandAtPoint(self, player);
        if (action.equals("summon"))
            return handleSummonShip(self, player);
        if (action.equals("remote_land"))
            return handleLandAtPoint(self, player);
        if (action.equals("follow_enable"))
            return handleEnableAutoFollow(self, player);
        if (action.equals("bombardment_enable"))
            return handleEnableBombardmentOrbit(self, player);
        if (action.equals("follow_disable"))
            return handleDisableAutoFollow(self, player);
        if (action.equals("return_landing"))
            return handleReturnToLandingAltitude(self, player, ship);

        return SCRIPT_CONTINUE;
    }

    private void cleanupSummonMenuVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, "summon.menu.ship");
        utils.removeBatchScriptVar(player, "summon.menu.actions");
    }

    private int handleReturnToLandingAltitude(obj_id self, obj_id player, obj_id ship) throws InterruptedException
    {
        if (space_transition.getContainingShip(player) == ship)
        {
            sendSystemMessageTestingOnly(player, "Leave the ship to use this option from your datapad.");
            return SCRIPT_CONTINUE;
        }

        if (!space_utils.isShipWithInterior(ship))
        {
            sendSystemMessageTestingOnly(player, "Only ships with an interior support this command.");
            return SCRIPT_CONTINUE;
        }

        if (!hasObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE) || !getBooleanObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE))
        {
            sendSystemMessageTestingOnly(player, "\\#ffaa44[Navicomputer]: Auto-follow is not active.");
            return SCRIPT_CONTINUE;
        }

        dictionary d = new dictionary();
        d.put("owner", player);
        messageTo(ship, "summonFollowReturnToLanding", d, 0, false);
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
        dictionary wpParams = new dictionary();
        wpParams.put("x", playerLoc.x);
        wpParams.put("z", playerLoc.z);
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
            if (hasObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) && getBooleanObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
                sendSystemMessageTestingOnly(player, "\\#ffaa44[Navicomputer]: Bombardment orbit is active. Disable it first to use standard auto-follow.");
            else
                sendSystemMessageTestingOnly(player, "\\#ffaa44[Navicomputer]: Auto-follow is already active.");
            return SCRIPT_CONTINUE;
        }

        String title = "Auto-Follow Ship";
        String prompt = "Your ship will climb to high cruise altitude, stay in the sky, and turn toward you as you move (no landing until you choose).\\n\\n"
            + "Cost: " + combat_ship.SUMMON_FOLLOW_COST_PER_HOUR + " credits per hour (first hour charged when you confirm).\\n\\n"
            + "Use 'Return to landing altitude' in Starship Remote to descend and end auto-follow.\\n\\n"
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

    private int handleEnableBombardmentOrbit(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessageTestingOnly(player, "You can only use this during atmospheric flight.");
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
            sendSystemMessageTestingOnly(player, "Leave the ship to enable bombardment orbit from your datapad.");
            return SCRIPT_CONTINUE;
        }

        if (!space_utils.isShipWithInterior(ship))
        {
            sendSystemMessageTestingOnly(player, "Only ships with an interior support bombardment orbit.");
            return SCRIPT_CONTINUE;
        }

        if (shipIsAutopilotActive(ship))
        {
            sendSystemMessageTestingOnly(player, "Your ship is already en route. Wait for it to arrive, then try again.");
            return SCRIPT_CONTINUE;
        }

        if (isIdValid(getPilotId(ship)))
        {
            sendSystemMessageTestingOnly(player, "Your ship is being piloted and cannot enter bombardment orbit.");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE) && getBooleanObjVar(ship, combat_ship.OV_SUMMON_FOLLOW_ACTIVE))
        {
            if (hasObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) && getBooleanObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
                sendSystemMessageTestingOnly(player, "\\#ffaa44[Navicomputer]: Bombardment orbit is already active.");
            else
                sendSystemMessageTestingOnly(player, "\\#ff4444[Navicomputer]: Disable standard auto-follow first, then enable bombardment orbit.");
            return SCRIPT_CONTINUE;
        }

        String title = "Bombardment orbit";
        String prompt = "Your ship holds a low orbit about " + (int) combat_ship.SUMMON_BOMBARDMENT_ORBIT_AGL + " m above terrain (not the high auto-follow altitude).\\n\\n"
            + "Activation: " + combat_ship.SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST + " credits (one time).\\n"
            + "Each successful turret shot from \"Bombard ground target\" on this datapad costs " + combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT + " credits.\\n\\n"
            + "Enable bombardment orbit?";

        utils.setScriptVar(player, "summon.bombard.ship", ship);
        sui.msgbox(self, player, prompt, sui.YES_NO, title, "handleBombardmentOrbitConfirm");
        return SCRIPT_CONTINUE;
    }

    public int handleBombardmentOrbitConfirm(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        obj_id player = sui.getPlayerId(params);
        if (bp != sui.BP_OK || !isIdValid(player))
        {
            utils.removeScriptVar(player, "summon.bombard.ship");
            return SCRIPT_CONTINUE;
        }

        obj_id ship = utils.getObjIdScriptVar(player, "summon.bombard.ship");
        utils.removeScriptVar(player, "summon.bombard.ship");

        if (!isAtmosphericFlightScene() || utils.getContainingPlayer(self) != player)
            return SCRIPT_CONTINUE;

        obj_id current = findDeployedShipForPlayer(player);
        if (!isIdValid(ship) || !exists(ship) || ship != current)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[Navicomputer]: Ship no longer available.");
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
        messageTo(ship, "summonBombardmentOrbitEnable", d, 0, false);
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
