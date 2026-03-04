package script.terminal;

import script.*;
import script.library.*;
import script.region;
import script.space.npc_pob_ship_controller;

/**
 * Terminal for NPC-controlled POB shuttle load/unload.
 *
 * Setup:
 * - Terminal on ground at a stop: set objvar npc_pob.linkedShip = ship. Shows "Board Shuttle".
 * - Terminal inside the ship: uses getContainingShip. Shows "Disembark".
 * - God mode: "Link Terminal to NPC Shuttle" links only to ships with npc_pob.controller (NPC POB shuttle).
 */
public class terminal_npc_pob_shuttle extends script.base_script
{
    private static final String OBJVAR_LINKED_SHIP = "npc_pob.linkedShip";
    private static final float BOARD_RANGE = 512.0f;
    private static final float LINK_RANGE_GOD = 2000.0f;
    private static final String DATATABLE_PREFIX = "datatables/npc_shuttle/";
    private static final String DATATABLE_SUFFIX = ".iff";
    private static final int TRAVEL_COST = 2500;

    public terminal_npc_pob_shuttle()
    {
    }

    private obj_id getShip(obj_id self) throws InterruptedException
    {
        obj_id ship = space_transition.getContainingShip(self);
        if (isIdValid(ship))
            return ship;
        if (hasObjVar(self, OBJVAR_LINKED_SHIP))
            return getObjIdObjVar(self, OBJVAR_LINKED_SHIP);
        return null;
    }

    private static String getPlanetFromScene(String scene)
    {
        if (scene == null || scene.length() == 0)
            return "";
        int idx = scene.indexOf('_');
        return idx > 0 ? scene.substring(0, idx) : scene;
    }

    private static String getLocalizedRegionAt(location loc) throws InterruptedException
    {
        if (loc == null)
            return null;
        region[] regs = getRegionsAtPoint(loc);
        if (regs == null || regs.length == 0)
            return null;
        for (region r : regs)
        {
            String name = r.getName();
            if (name != null && name.length() > 0)
            {
                if (name.startsWith("@"))
                {
                    string_id sid = utils.unpackString(name);
                    if (sid != null)
                        return localize(sid);
                    return name;
                }
                return name;
            }
        }
        return null;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        obj_id ship = getShip(self);
        obj_id playerShip = space_transition.getContainingShip(player);

        if (isIdValid(playerShip))
        {
            obj_id termShip = getShip(self);
            if (isIdValid(termShip) && termShip == playerShip)
                mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Disembark"));
        }
        else if (isIdValid(ship) && exists(ship) && hasObjVar(ship, script.space.npc_pob_ship_controller.OBJVAR_CONTROLLER))
        {
            if (!space_transition.isShipParkedInWorld(ship))
                return SCRIPT_CONTINUE;
            float dist = getDistance(player, ship);
            if (dist <= BOARD_RANGE)
                mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Board Shuttle"));
        }

        if (space_transition.isAtmosphericFlightScene())
            mi.addRootMenu(menu_info_types.SERVER_MENU4, string_id.unlocalized("Shuttle Information"));

        if (isGod(player) && space_transition.isAtmosphericFlightScene())
        {
            obj_id[] nearby = getObjectsInRange(self, LINK_RANGE_GOD);
            if (nearby != null)
            {
                for (obj_id o : nearby)
                {
                    if (isIdValid(o) && getTopMostContainer(o) == o && space_utils.isShipWithInterior(o)
                            && hasObjVar(o, npc_pob_ship_controller.OBJVAR_CONTROLLER))
                    {
                        mi.addRootMenu(menu_info_types.SERVER_MENU3, string_id.unlocalized("Link Terminal to NPC Shuttle"));
                        break;
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1)
        {
            obj_id ship = space_transition.getContainingShip(player);
            if (!isIdValid(ship))
                return SCRIPT_CONTINUE;
            if (!space_transition.isAtmosphericFlightScene())
            {
                sendSystemMessage(player, string_id.unlocalized("You can only disembark during atmospheric flight."));
                return SCRIPT_CONTINUE;
            }
            if (isIdValid(getPilotId(ship)))
            {
                sendSystemMessage(player, string_id.unlocalized("You cannot disembark while the ship is being piloted."));
                return SCRIPT_CONTINUE;
            }
            space_transition.disembarkShip(player, ship);
            return SCRIPT_CONTINUE;
        }

        if (item == menu_info_types.SERVER_MENU2)
        {
            obj_id ship = getObjIdObjVar(self, OBJVAR_LINKED_SHIP);
            if (!isIdValid(ship) || !exists(ship))
            {
                sendSystemMessage(player, string_id.unlocalized("The shuttle is not available."));
                return SCRIPT_CONTINUE;
            }
            if (!space_transition.isShipParkedInWorld(ship))
            {
                sendSystemMessage(player, string_id.unlocalized("The shuttle is not parked here."));
                return SCRIPT_CONTINUE;
            }
            if (getDistance(player, ship) > BOARD_RANGE)
            {
                sendSystemMessage(player, string_id.unlocalized("You are too far from the shuttle to board."));
                return SCRIPT_CONTINUE;
            }
            if (getBankBalance(player) < TRAVEL_COST)
            {
                sendSystemMessage(player, string_id.unlocalized("You cannot afford the shuttle fare. Cost: " + TRAVEL_COST + " bank credits."));
                return SCRIPT_CONTINUE;
            }
            if (!transferBankCreditsToNamedAccount(player, money.ACCT_TRAVEL, TRAVEL_COST, "noHandler", "noHandler", new dictionary()))
            {
                sendSystemMessage(player, string_id.unlocalized("You cannot afford the shuttle fare. Cost: " + TRAVEL_COST + " bankcredits."));
                return SCRIPT_CONTINUE;
            }
            if (!space_transition.boardShipFromGround(player, ship))
                return SCRIPT_CONTINUE;
            return SCRIPT_CONTINUE;
        }

        if (item == menu_info_types.SERVER_MENU3 && isGod(player))
        {
            obj_id[] nearby = getObjectsInRange(self, LINK_RANGE_GOD);
            if (nearby != null)
            {
                for (obj_id o : nearby)
                {
                    if (isIdValid(o) && getTopMostContainer(o) == o && space_utils.isShipWithInterior(o)
                            && hasObjVar(o, npc_pob_ship_controller.OBJVAR_CONTROLLER))
                    {
                        setObjVar(self, OBJVAR_LINKED_SHIP, o);
                        sendSystemMessage(player, string_id.unlocalized("Terminal linked to NPC shuttle."));
                        return SCRIPT_CONTINUE;
                    }
                }
            }
            sendSystemMessage(player, string_id.unlocalized("No NPC POB shuttle within range to link."));
        }

        if (item == menu_info_types.SERVER_MENU4)
        {
            obj_id ship = getShip(self);
            String scene = getLocation(self).area;
            String planet = getPlanetFromScene(scene);
            String dtPath = DATATABLE_PREFIX + planet + DATATABLE_SUFFIX;
            int numRows = 0;
            try
            {
                numRows = dataTableGetNumRows(dtPath);
            }
            catch (Throwable t)
            {
            }
            if (numRows <= 0)
            {
                sendSystemMessage(player, string_id.unlocalized("No shuttle route data for this planet."));
                return SCRIPT_CONTINUE;
            }
            if (!isIdValid(ship) || !exists(ship))
            {
                sendSystemMessage(player, string_id.unlocalized("No shuttle linked to this terminal."));
                return SCRIPT_CONTINUE;
            }
            location shipLoc = getLocation(ship);
            String nearestCity = null;
            float nearestDistSq = Float.MAX_VALUE;
            for (int i = 0; i < numRows; i++)
            {
                String city = dataTableGetString(dtPath, i, "city");
                if (city == null || city.length() == 0)
                    city = dataTableGetString(dtPath, i, 0);
                if (city == null || city.length() == 0)
                    city = "Waypoint " + (i + 1);
                float sx = dataTableGetFloat(dtPath, i, "x");
                float sy = dataTableGetFloat(dtPath, i, "y");
                float sz = dataTableGetFloat(dtPath, i, "z");
                float dx = shipLoc.x - sx;
                float dy = shipLoc.y - sy;
                float dz = shipLoc.z - sz;
                float distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < nearestDistSq)
                {
                    nearestDistSq = distSq;
                    nearestCity = city;
                }
            }
            if (nearestCity != null && nearestCity.length() > 0)
            {
                String msg = "Shuttle is currently at: " + nearestCity;
                String regionName = getLocalizedRegionAt(shipLoc);
                if (regionName != null && regionName.length() > 0)
                    msg += "\nRegion: " + regionName;
                sendSystemMessage(player, string_id.unlocalized(msg));
            }
            else
                sendSystemMessage(player, string_id.unlocalized("Shuttle location could not be determined."));
        }
        return SCRIPT_CONTINUE;
    }
}
