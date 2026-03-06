package script.systems;

/**
 * Hockey Game Manager
 *
 * A centralized manager for hockey games. Attach to any object to create a game control station.
 * This script provides easy setup commands and game management.
 *
 * Usage:
 * 1. Attach this script to any object (terminal, sign, etc.)
 * 2. Use radial menu to set up goals, spawn pucks, manage games
 *
 * @author Titan Development Team
 * @created 2026
 */

import script.*;
import script.library.*;

public class hockey_manager extends script.base_script
{
    // =====================================================================
    // CONSTANTS
    // =====================================================================

    public static final String OBJVAR_RED_GOAL = "hockey.redGoalId";
    public static final String OBJVAR_BLUE_GOAL = "hockey.blueGoalId";
    public static final String OBJVAR_PUCK = "hockey.puckId";
    public static final String OBJVAR_CENTER_LOC = "hockey.centerLoc";
    public static final String OBJVAR_GAME_NAME = "hockey.gameName";
    public static final String OBJVAR_WIN_SCORE = "hockey.winScore";

    // Default settings
    public static final int DEFAULT_WIN_SCORE = 5;
    public static final String DEFAULT_PUCK_TEMPLATE = "object/tangible/furniture/all/frn_all_crate_s01.iff";
    public static final String DEFAULT_GOAL_TEMPLATE = "object/tangible/spawning/spawn_egg.iff";

    // =====================================================================
    // INITIALIZATION
    // =====================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_GAME_NAME))
        {
            setObjVar(self, OBJVAR_GAME_NAME, "Hockey Arena");
        }

        if (!hasObjVar(self, OBJVAR_WIN_SCORE))
        {
            setObjVar(self, OBJVAR_WIN_SCORE, DEFAULT_WIN_SCORE);
        }

        setName(self, "Hockey Game Manager");

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // RADIAL MENU
    // =====================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isGod(player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("View Scores"));
            return SCRIPT_CONTINUE;
        }

        int root = mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Hockey Manager"));

        // Setup submenu
        int setup = mi.addSubMenu(root, menu_info_types.SERVER_MENU2, string_id.unlocalized("[Setup]"));
        mi.addSubMenu(setup, menu_info_types.SERVER_MENU3, string_id.unlocalized("Place Red Goal Here"));
        mi.addSubMenu(setup, menu_info_types.SERVER_MENU4, string_id.unlocalized("Place Blue Goal Here"));
        mi.addSubMenu(setup, menu_info_types.SERVER_MENU5, string_id.unlocalized("Set Center Point Here"));
        mi.addSubMenu(setup, menu_info_types.SERVER_MENU6, string_id.unlocalized("Auto-Setup Arena"));

        // Game submenu
        int game = mi.addSubMenu(root, menu_info_types.SERVER_MENU7, string_id.unlocalized("[Game]"));
        mi.addSubMenu(game, menu_info_types.SERVER_MENU8, string_id.unlocalized("Spawn Puck at Center"));
        mi.addSubMenu(game, menu_info_types.SERVER_MENU9, string_id.unlocalized("Reset Scores"));
        mi.addSubMenu(game, menu_info_types.SERVER_MENU10, string_id.unlocalized("Start New Game"));

        // Info
        mi.addSubMenu(root, menu_info_types.SERVER_MENU11, string_id.unlocalized("View Scores"));
        mi.addSubMenu(root, menu_info_types.SERVER_MENU12, string_id.unlocalized("Show Setup Info"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        switch (item)
        {
            case menu_info_types.SERVER_MENU3: // Place Red Goal
                placeGoal(self, player, "red");
                break;

            case menu_info_types.SERVER_MENU4: // Place Blue Goal
                placeGoal(self, player, "blue");
                break;

            case menu_info_types.SERVER_MENU5: // Set Center
                setCenterPoint(self, player);
                break;

            case menu_info_types.SERVER_MENU6: // Auto-Setup
                autoSetupArena(self, player);
                break;

            case menu_info_types.SERVER_MENU8: // Spawn Puck
                spawnPuckAtCenter(self, player);
                break;

            case menu_info_types.SERVER_MENU9: // Reset Scores
                resetScores(self, player);
                break;

            case menu_info_types.SERVER_MENU10: // New Game
                startNewGame(self, player);
                break;

            case menu_info_types.SERVER_MENU1: // View Scores (non-admin)
            case menu_info_types.SERVER_MENU11: // View Scores (admin)
                showScores(self, player);
                break;

            case menu_info_types.SERVER_MENU12: // Show Info
                showSetupInfo(self, player);
                break;
        }

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // SETUP FUNCTIONS
    // =====================================================================

    private void placeGoal(obj_id manager, obj_id player, String team) throws InterruptedException
    {
        location playerLoc = getLocation(player);

        // Create goal (spawn egg)
        obj_id goal = createObject(DEFAULT_GOAL_TEMPLATE, playerLoc);

        if (!isIdValid(goal))
        {
            sendSystemMessage(player, string_id.unlocalized("Failed to create goal!"));
            return;
        }

        // Attach hockey game script
        attachScript(goal, "systems.hockey_game");

        // Set team
        setObjVar(goal, "hockey.team", team);

        // Store reference
        if (team.equals("red"))
        {
            // Clean up old goal if exists
            obj_id oldGoal = getObjIdObjVar(manager, OBJVAR_RED_GOAL);
            if (isIdValid(oldGoal))
            {
                destroyObject(oldGoal);
            }
            setObjVar(manager, OBJVAR_RED_GOAL, goal);
            setName(goal, "RED GOAL");
        }
        else
        {
            obj_id oldGoal = getObjIdObjVar(manager, OBJVAR_BLUE_GOAL);
            if (isIdValid(oldGoal))
            {
                destroyObject(oldGoal);
            }
            setObjVar(manager, OBJVAR_BLUE_GOAL, goal);
            setName(goal, "BLUE GOAL");
        }

        sendSystemMessage(player, string_id.unlocalized(team.toUpperCase() + " goal placed at your location!"));
    }

    private void setCenterPoint(obj_id manager, obj_id player) throws InterruptedException
    {
        location playerLoc = getLocation(player);

        setObjVar(manager, OBJVAR_CENTER_LOC + ".x", playerLoc.x);
        setObjVar(manager, OBJVAR_CENTER_LOC + ".y", playerLoc.y);
        setObjVar(manager, OBJVAR_CENTER_LOC + ".z", playerLoc.z);
        setObjVar(manager, OBJVAR_CENTER_LOC + ".area", playerLoc.area);

        sendSystemMessage(player, string_id.unlocalized("Center point set at your location!"));
    }

    private void autoSetupArena(obj_id manager, obj_id player) throws InterruptedException
    {
        location playerLoc = getLocation(player);

        // Set center at player location
        setCenterPoint(manager, player);

        // Place red goal 20m in front
        location redLoc = new location(playerLoc.x + 20.0f, playerLoc.y, playerLoc.z, playerLoc.area);
        obj_id redGoal = createObject(DEFAULT_GOAL_TEMPLATE, redLoc);
        if (isIdValid(redGoal))
        {
            attachScript(redGoal, "systems.hockey_game");
            setObjVar(redGoal, "hockey.team", "red");
            setName(redGoal, "RED GOAL");
            setObjVar(manager, OBJVAR_RED_GOAL, redGoal);
        }

        // Place blue goal 20m behind
        location blueLoc = new location(playerLoc.x - 20.0f, playerLoc.y, playerLoc.z, playerLoc.area);
        obj_id blueGoal = createObject(DEFAULT_GOAL_TEMPLATE, blueLoc);
        if (isIdValid(blueGoal))
        {
            attachScript(blueGoal, "systems.hockey_game");
            setObjVar(blueGoal, "hockey.team", "blue");
            setName(blueGoal, "BLUE GOAL");
            setObjVar(manager, OBJVAR_BLUE_GOAL, blueGoal);
        }

        // Spawn puck at center
        spawnPuckAtCenter(manager, player);

        sendSystemMessage(player, string_id.unlocalized("Hockey arena auto-setup complete! Goals 40m apart with puck in center."));
    }

    // =====================================================================
    // GAME FUNCTIONS
    // =====================================================================

    private void spawnPuckAtCenter(obj_id manager, obj_id player) throws InterruptedException
    {
        location centerLoc;

        // Get center location or use player location
        if (hasObjVar(manager, OBJVAR_CENTER_LOC + ".x"))
        {
            float x = getFloatObjVar(manager, OBJVAR_CENTER_LOC + ".x");
            float y = getFloatObjVar(manager, OBJVAR_CENTER_LOC + ".y");
            float z = getFloatObjVar(manager, OBJVAR_CENTER_LOC + ".z");
            String area = getStringObjVar(manager, OBJVAR_CENTER_LOC + ".area");
            centerLoc = new location(x, y + 1.0f, z, area);
        }
        else
        {
            centerLoc = getLocation(player);
            centerLoc.y += 1.0f;
        }

        // Remove old puck if exists
        obj_id oldPuck = getObjIdObjVar(manager, OBJVAR_PUCK);
        if (isIdValid(oldPuck))
        {
            destroyObject(oldPuck);
        }

        // Create new puck
        obj_id puck = createObject(DEFAULT_PUCK_TEMPLATE, centerLoc);

        if (isIdValid(puck))
        {
            attachScript(puck, "systems.hockey_puck");
            setObjVar(manager, OBJVAR_PUCK, puck);

            // Store center as puck spawn point on goals
            obj_id redGoal = getObjIdObjVar(manager, OBJVAR_RED_GOAL);
            obj_id blueGoal = getObjIdObjVar(manager, OBJVAR_BLUE_GOAL);

            if (isIdValid(redGoal))
            {
                setObjVar(redGoal, "hockey.puckSpawnLoc.x", centerLoc.x);
                setObjVar(redGoal, "hockey.puckSpawnLoc.y", centerLoc.y - 1.0f);
                setObjVar(redGoal, "hockey.puckSpawnLoc.z", centerLoc.z);
                setObjVar(redGoal, "hockey.puckSpawnLoc.area", centerLoc.area);
            }
            if (isIdValid(blueGoal))
            {
                setObjVar(blueGoal, "hockey.puckSpawnLoc.x", centerLoc.x);
                setObjVar(blueGoal, "hockey.puckSpawnLoc.y", centerLoc.y - 1.0f);
                setObjVar(blueGoal, "hockey.puckSpawnLoc.z", centerLoc.z);
                setObjVar(blueGoal, "hockey.puckSpawnLoc.area", centerLoc.area);
            }

            sendSystemMessage(player, string_id.unlocalized("Puck spawned at center!"));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Failed to spawn puck!"));
        }
    }

    private void resetScores(obj_id manager, obj_id player) throws InterruptedException
    {
        obj_id redGoal = getObjIdObjVar(manager, OBJVAR_RED_GOAL);
        obj_id blueGoal = getObjIdObjVar(manager, OBJVAR_BLUE_GOAL);

        if (isIdValid(redGoal))
        {
            setObjVar(redGoal, "hockey.scoreRed", 0);
            setObjVar(redGoal, "hockey.scoreBlue", 0);
        }
        if (isIdValid(blueGoal))
        {
            setObjVar(blueGoal, "hockey.scoreRed", 0);
            setObjVar(blueGoal, "hockey.scoreBlue", 0);
        }

        // Announce
        location loc = getLocation(manager);
        obj_id[] nearbyPlayers = getPlayerCreaturesInRange(manager, 100.0f);
        if (nearbyPlayers != null)
        {
            for (int i = 0; i < nearbyPlayers.length; i++)
            {
                sendSystemMessage(nearbyPlayers[i], string_id.unlocalized("\\#FFFF00Hockey scores have been reset!"));
            }
        }

        sendSystemMessage(player, string_id.unlocalized("Scores reset!"));
    }

    private void startNewGame(obj_id manager, obj_id player) throws InterruptedException
    {
        // Reset scores
        resetScores(manager, player);

        // Respawn puck at center
        spawnPuckAtCenter(manager, player);

        // Announce
        obj_id[] nearbyPlayers = getPlayerCreaturesInRange(manager, 100.0f);
        if (nearbyPlayers != null)
        {
            for (int i = 0; i < nearbyPlayers.length; i++)
            {
                sendSystemMessage(nearbyPlayers[i], string_id.unlocalized("\\#00FF00=== NEW HOCKEY GAME STARTED ==="));
                sendSystemMessage(nearbyPlayers[i], string_id.unlocalized("Push the puck into the opposing goal to score!"));
            }
        }
    }

    // =====================================================================
    // INFO FUNCTIONS
    // =====================================================================

    private void showScores(obj_id manager, obj_id player) throws InterruptedException
    {
        obj_id redGoal = getObjIdObjVar(manager, OBJVAR_RED_GOAL);

        int redScore = 0;
        int blueScore = 0;

        if (isIdValid(redGoal))
        {
            redScore = getIntObjVar(redGoal, "hockey.scoreRed");
            blueScore = getIntObjVar(redGoal, "hockey.scoreBlue");
        }

        String gameName = getStringObjVar(manager, OBJVAR_GAME_NAME);

        String msg = "\\#FFFFFF=== " + gameName + " ===\n" +
            "\\#FF0000RED TEAM: " + redScore + "\n" +
            "\\#0000FFBLUE TEAM: " + blueScore;

        sendSystemMessage(player, string_id.unlocalized(msg));
    }

    private void showSetupInfo(obj_id manager, obj_id player) throws InterruptedException
    {
        obj_id redGoal = getObjIdObjVar(manager, OBJVAR_RED_GOAL);
        obj_id blueGoal = getObjIdObjVar(manager, OBJVAR_BLUE_GOAL);
        obj_id puck = getObjIdObjVar(manager, OBJVAR_PUCK);

        boolean hasCenter = hasObjVar(manager, OBJVAR_CENTER_LOC + ".x");

        String info = "=== HOCKEY ARENA SETUP ===\n" +
            "Red Goal: " + (isIdValid(redGoal) ? "PLACED" : "NOT SET") + "\n" +
            "Blue Goal: " + (isIdValid(blueGoal) ? "PLACED" : "NOT SET") + "\n" +
            "Center Point: " + (hasCenter ? "SET" : "NOT SET") + "\n" +
            "Puck: " + (isIdValid(puck) ? "SPAWNED" : "NOT SPAWNED");

        sendSystemMessage(player, string_id.unlocalized(info));
    }
}

