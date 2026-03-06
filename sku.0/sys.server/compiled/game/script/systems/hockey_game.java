package script.systems;

/**
 * Hockey Game System
 *
 * A goal-based game where players push objects (pucks) into goals.
 * Uses spawn eggs as goal detection zones.
 *
 * Setup:
 * 1. Place two spawn eggs as goals (attach this script to each)
 * 2. Set objvar "hockey.team" to "red" or "blue" on each goal
 * 3. Spawn a puck object and attach "systems.hockey_puck" script
 * 4. Players push the puck into the opposing team's goal to score
 *
 * @author Titan Development Team
 * @created 2026
 */

import script.*;
import script.library.*;

public class hockey_game extends script.base_script
{
    // =====================================================================
    // CONSTANTS
    // =====================================================================

    // Objvar keys
    public static final String OBJVAR_TEAM = "hockey.team";
    public static final String OBJVAR_GAME_ID = "hockey.gameId";
    public static final String OBJVAR_GOAL_RADIUS = "hockey.goalRadius";
    public static final String OBJVAR_SCORE_RED = "hockey.scoreRed";
    public static final String OBJVAR_SCORE_BLUE = "hockey.scoreBlue";
    public static final String OBJVAR_LAST_GOAL_TIME = "hockey.lastGoalTime";
    public static final String OBJVAR_PUCK_ID = "hockey.puckId";
    public static final String OBJVAR_GAME_ACTIVE = "hockey.gameActive";
    public static final String OBJVAR_PUCK_SPAWN_LOC = "hockey.puckSpawnLoc";

    // Default values
    public static final float DEFAULT_GOAL_RADIUS = 2.0f;
    public static final float GOAL_COOLDOWN = 3.0f; // seconds between goals

    // Teams
    public static final String TEAM_RED = "red";
    public static final String TEAM_BLUE = "blue";

    // =====================================================================
    // INITIALIZATION
    // =====================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        // Set default team if not set
        if (!hasObjVar(self, OBJVAR_TEAM))
        {
            setObjVar(self, OBJVAR_TEAM, TEAM_RED);
        }

        // Set default goal radius
        if (!hasObjVar(self, OBJVAR_GOAL_RADIUS))
        {
            setObjVar(self, OBJVAR_GOAL_RADIUS, DEFAULT_GOAL_RADIUS);
        }

        // Initialize scores
        if (!hasObjVar(self, OBJVAR_SCORE_RED))
        {
            setObjVar(self, OBJVAR_SCORE_RED, 0);
        }
        if (!hasObjVar(self, OBJVAR_SCORE_BLUE))
        {
            setObjVar(self, OBJVAR_SCORE_BLUE, 0);
        }

        // Set game as active
        setObjVar(self, OBJVAR_GAME_ACTIVE, 1);

        // Start goal detection loop
        messageTo(self, "OnGoalDetectionTick", null, 0.1f, false);

        sendSystemMessageToNearby(self, 50.0f, "Hockey goal initialized for team: " + getStringObjVar(self, OBJVAR_TEAM));

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // RADIAL MENU
    // =====================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        // Admin options
        if (isGod(player))
        {
            int root = mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Hockey Game"));
            mi.addSubMenu(root, menu_info_types.SERVER_MENU2, string_id.unlocalized("Spawn Puck"));
            mi.addSubMenu(root, menu_info_types.SERVER_MENU3, string_id.unlocalized("Reset Scores"));
            mi.addSubMenu(root, menu_info_types.SERVER_MENU4, string_id.unlocalized("Show Scores"));
            mi.addSubMenu(root, menu_info_types.SERVER_MENU5, string_id.unlocalized("Set as Red Goal"));
            mi.addSubMenu(root, menu_info_types.SERVER_MENU6, string_id.unlocalized("Set as Blue Goal"));
            mi.addSubMenu(root, menu_info_types.SERVER_MENU7, string_id.unlocalized("Toggle Game Active"));
        }
        else
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU4, string_id.unlocalized("Show Scores"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        switch (item)
        {
            case menu_info_types.SERVER_MENU2: // Spawn Puck
                spawnPuck(self, player);
                break;

            case menu_info_types.SERVER_MENU3: // Reset Scores
                resetScores(self);
                sendSystemMessage(player, string_id.unlocalized("Scores reset!"));
                break;

            case menu_info_types.SERVER_MENU4: // Show Scores
                showScores(self, player);
                break;

            case menu_info_types.SERVER_MENU5: // Set Red
                setObjVar(self, OBJVAR_TEAM, TEAM_RED);
                sendSystemMessage(player, string_id.unlocalized("This goal is now RED team's goal."));
                break;

            case menu_info_types.SERVER_MENU6: // Set Blue
                setObjVar(self, OBJVAR_TEAM, TEAM_BLUE);
                sendSystemMessage(player, string_id.unlocalized("This goal is now BLUE team's goal."));
                break;

            case menu_info_types.SERVER_MENU7: // Toggle Active
                int active = getIntObjVar(self, OBJVAR_GAME_ACTIVE);
                setObjVar(self, OBJVAR_GAME_ACTIVE, active == 1 ? 0 : 1);
                sendSystemMessage(player, string_id.unlocalized("Game is now " + (active == 1 ? "PAUSED" : "ACTIVE")));
                break;
        }

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // GOAL DETECTION
    // =====================================================================

    public int OnGoalDetectionTick(obj_id self, dictionary params) throws InterruptedException
    {
        // Check if game is active
        if (getIntObjVar(self, OBJVAR_GAME_ACTIVE) != 1)
        {
            // Reschedule even if inactive
            messageTo(self, "OnGoalDetectionTick", null, 0.5f, false);
            return SCRIPT_CONTINUE;
        }

        // Check goal cooldown
        int lastGoalTime = getIntObjVar(self, OBJVAR_LAST_GOAL_TIME);
        int currentTime = getGameTime();
        if (currentTime - lastGoalTime < GOAL_COOLDOWN)
        {
            messageTo(self, "OnGoalDetectionTick", null, 0.1f, false);
            return SCRIPT_CONTINUE;
        }

        // Get goal position and radius
        location goalLoc = getLocation(self);
        float goalRadius = getFloatObjVar(self, OBJVAR_GOAL_RADIUS);
        if (goalRadius <= 0.0f)
            goalRadius = DEFAULT_GOAL_RADIUS;

        // Find all nearby tangible objects
        obj_id[] nearbyObjects = getObjectsInRange(self, goalRadius + 5.0f);

        if (nearbyObjects != null)
        {
            for (int i = 0; i < nearbyObjects.length; i++)
            {
                obj_id obj = nearbyObjects[i];
                if (!isIdValid(obj))
                    continue;

                // Check if this is a hockey puck
                if (!hasScript(obj, "systems.hockey_puck"))
                    continue;

                // Check distance
                float distance = getDistance(self, obj);
                if (distance <= goalRadius)
                {
                    // GOAL!
                    onGoalScored(self, obj);
                    break;
                }
            }
        }

        // Schedule next tick
        messageTo(self, "OnGoalDetectionTick", null, 0.1f, false);

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // SCORING
    // =====================================================================

    private void onGoalScored(obj_id goal, obj_id puck) throws InterruptedException
    {
        String goalTeam = getStringObjVar(goal, OBJVAR_TEAM);

        // Scoring in opponent's goal means YOUR team gets a point
        // So if this is RED's goal, BLUE scores
        String scoringTeam = goalTeam.equals(TEAM_RED) ? TEAM_BLUE : TEAM_RED;

        // Increment score
        int redScore = getIntObjVar(goal, OBJVAR_SCORE_RED);
        int blueScore = getIntObjVar(goal, OBJVAR_SCORE_BLUE);

        if (scoringTeam.equals(TEAM_RED))
        {
            redScore++;
            setObjVar(goal, OBJVAR_SCORE_RED, redScore);
        }
        else
        {
            blueScore++;
            setObjVar(goal, OBJVAR_SCORE_BLUE, blueScore);
        }

        // Set cooldown
        setObjVar(goal, OBJVAR_LAST_GOAL_TIME, getGameTime());

        // Announce goal
        String announcement = "\\#FF0000GOAL!\\#FFFFFF " + scoringTeam.toUpperCase() + " SCORES! " +
            "\\#FF0000RED: " + redScore + " \\#0000FFBLUE: " + blueScore;
        sendSystemMessageToNearby(goal, 100.0f, announcement);

        // Play effect on goal (if available)
        playClientEffectObj(goal, "clienteffect/combat_explosion_lair_large.cef", goal, "");

        // Reset puck to center
        resetPuck(goal, puck);

        // Sync scores to other goal
        syncScoresToOtherGoal(goal);
    }

    private void resetPuck(obj_id goal, obj_id puck) throws InterruptedException
    {
        // Check if there's a stored puck spawn location
        if (hasObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".x"))
        {
            float x = getFloatObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".x");
            float y = getFloatObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".y");
            float z = getFloatObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".z");
            String area = getStringObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".area");

            location spawnLoc = new location(x, y, z, area);
            setLocation(puck, spawnLoc);
        }
        else
        {
            // Default: move puck to midpoint between goals
            // For now, just move it up and let it drop
            location puckLoc = getLocation(puck);
            location goalLoc = getLocation(goal);

            // Move toward center (away from goal)
            String team = getStringObjVar(goal, OBJVAR_TEAM);
            float offsetX = (team.equals(TEAM_RED)) ? 10.0f : -10.0f;

            puckLoc.x = goalLoc.x + offsetX;
            puckLoc.y = goalLoc.y + 2.0f; // Drop from above
            setLocation(puck, puckLoc);
        }

        // Clear any existing velocity on the puck
        if (hasScript(puck, "handler.tangible_dynamics_handler"))
        {
            tangible_dynamics.clearPushForce(puck);
        }

        // Apply a small bounce for visual feedback
        tangible_dynamics.enableDynamics(puck);
        tangible_dynamics.applyBounceEffect(puck, 9.8f, 0.5f, 3.0f, 2.0f);
    }

    private void syncScoresToOtherGoal(obj_id thisGoal) throws InterruptedException
    {
        // Find other goals in range and sync scores
        obj_id[] nearbyObjects = getObjectsInRange(thisGoal, 200.0f);

        if (nearbyObjects != null)
        {
            int redScore = getIntObjVar(thisGoal, OBJVAR_SCORE_RED);
            int blueScore = getIntObjVar(thisGoal, OBJVAR_SCORE_BLUE);

            for (int i = 0; i < nearbyObjects.length; i++)
            {
                obj_id obj = nearbyObjects[i];
                if (!isIdValid(obj) || obj.equals(thisGoal))
                    continue;

                if (hasScript(obj, "systems.hockey_game"))
                {
                    setObjVar(obj, OBJVAR_SCORE_RED, redScore);
                    setObjVar(obj, OBJVAR_SCORE_BLUE, blueScore);
                }
            }
        }
    }

    // =====================================================================
    // UTILITIES
    // =====================================================================

    private void spawnPuck(obj_id goal, obj_id player) throws InterruptedException
    {
        // Spawn a puck near the player
        location playerLoc = getLocation(player);
        playerLoc.y += 1.0f; // Spawn slightly above ground

        // Create a simple object as the puck
        obj_id puck = createObject("object/tangible/furniture/all/frn_all_crate_s01.iff", playerLoc);

        if (isIdValid(puck))
        {
            // Attach puck script
            attachScript(puck, "systems.hockey_puck");

            // Enable dynamics for hockey puck mode
            tangible_dynamics.enableDynamics(puck);

            // Enable collision push (hockey puck mode)
            removeObjVar(puck, "collideBlock");
            setCondition(puck, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

            // Store puck spawn location on the goal
            setObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".x", playerLoc.x);
            setObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".y", playerLoc.y - 1.0f);
            setObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".z", playerLoc.z);
            setObjVar(goal, OBJVAR_PUCK_SPAWN_LOC + ".area", playerLoc.area);

            // Store puck ID
            setObjVar(goal, OBJVAR_PUCK_ID, puck);

            // Apply initial bounce
            tangible_dynamics.applyBounceEffect(puck, 9.8f, 0.6f, 4.0f, 2.0f);

            sendSystemMessage(player, string_id.unlocalized("Puck spawned! Push it into the opposing goal to score."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Failed to spawn puck."));
        }
    }

    private void resetScores(obj_id goal) throws InterruptedException
    {
        setObjVar(goal, OBJVAR_SCORE_RED, 0);
        setObjVar(goal, OBJVAR_SCORE_BLUE, 0);

        // Sync to other goals
        syncScoresToOtherGoal(goal);

        sendSystemMessageToNearby(goal, 100.0f, "Hockey game scores have been reset!");
    }

    private void showScores(obj_id goal, obj_id player) throws InterruptedException
    {
        int redScore = getIntObjVar(goal, OBJVAR_SCORE_RED);
        int blueScore = getIntObjVar(goal, OBJVAR_SCORE_BLUE);

        String msg = "\\#FFFFFF=== HOCKEY SCORES ===\n" +
            "\\#FF0000RED TEAM: " + redScore + "\n" +
            "\\#0000FFBLUE TEAM: " + blueScore;

        sendSystemMessage(player, string_id.unlocalized(msg));
    }

    private void sendSystemMessageToNearby(obj_id center, float range, String message) throws InterruptedException
    {
        obj_id[] nearbyPlayers = getPlayerCreaturesInRange(center, range);
        if (nearbyPlayers != null)
        {
            for (int i = 0; i < nearbyPlayers.length; i++)
            {
                if (isIdValid(nearbyPlayers[i]))
                {
                    sendSystemMessage(nearbyPlayers[i], string_id.unlocalized(message));
                }
            }
        }
    }
}

