package script.systems;

/**
 * Hockey Puck Script
 *
 * Attached to objects that act as pucks in the hockey game.
 * Enables hockey puck physics (push on collision, terrain following).
 *
 * @author Titan Development Team
 * @created 2026
 */

import script.*;
import script.library.*;

public class hockey_puck extends script.base_script
{
    // =====================================================================
    // CONSTANTS
    // =====================================================================

    public static final String OBJVAR_LAST_PUSHER = "hockey.lastPusher";
    public static final String OBJVAR_PUSH_COUNT = "hockey.pushCount";
    public static final String OBJVAR_GLOW_COLOR = "hockey.glowColor";

    // Physics settings - tuned for reliable collisions
    public static final float PUSH_SPEED = 8.0f;
    public static final float PUSH_DRAG = 0.3f;  // Lower drag = slides longer
    public static final float COLLISION_RADIUS = 2.5f;  // Larger radius for reliable detection

    // =====================================================================
    // INITIALIZATION
    // =====================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        // Enable tangible dynamics
        tangible_dynamics.enableDynamics(self);

        // Set hockey puck physics parameters
        setObjVar(self, "dynamics.pushSpeed", PUSH_SPEED);
        setObjVar(self, "dynamics.pushDrag", PUSH_DRAG);
        setObjVar(self, "dynamics.collisionRadius", COLLISION_RADIUS);

        // Remove collision block to enable push-on-collision
        removeObjVar(self, "collideBlock");

        // Set the dynamics condition
        setCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

        // Initialize push count
        setObjVar(self, OBJVAR_PUSH_COUNT, 0);

        // Make it visually distinct
        setName(self, "Hockey Puck");

        LOG("hockey", "Hockey puck initialized: " + self);

        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        // Clear dynamics
        tangible_dynamics.clearAllForces(self);

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // RADIAL MENU
    // =====================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Kick Puck"));

        if (isGod(player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Reset Puck"));
            mi.addRootMenu(menu_info_types.SERVER_MENU3, string_id.unlocalized("Puck Stats"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        switch (item)
        {
            case menu_info_types.SERVER_MENU1: // Kick
                kickPuck(self, player);
                break;

            case menu_info_types.SERVER_MENU2: // Reset
                resetPuck(self, player);
                break;

            case menu_info_types.SERVER_MENU3: // Stats
                showStats(self, player);
                break;
        }

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // PUCK ACTIONS
    // =====================================================================

    private void kickPuck(obj_id puck, obj_id player) throws InterruptedException
    {
        // Check distance
        float distance = getDistance(puck, player);
        if (distance > 5.0f)
        {
            sendSystemMessage(player, string_id.unlocalized("You're too far away to kick the puck!"));
            return;
        }

        // Get player facing direction
        location playerLoc = getLocation(player);
        location puckLoc = getLocation(puck);

        // Calculate kick direction (from player to puck, or player facing)
        float dirX = puckLoc.x - playerLoc.x;
        float dirZ = puckLoc.z - playerLoc.z;
        float mag = (float)Math.sqrt(dirX * dirX + dirZ * dirZ);

        if (mag > 0.1f)
        {
            dirX /= mag;
            dirZ /= mag;
        }
        else
        {
            // Player is on top of puck - use player facing
            dirX = 1.0f;
            dirZ = 0.0f;
        }

        // Apply kick force
        float kickSpeed = PUSH_SPEED * 1.5f; // Kicks are stronger than walks
        tangible_dynamics.applyPushForceWithDrag(puck, dirX * kickSpeed, 0.0f, dirZ * kickSpeed, PUSH_DRAG, -1.0f, 0);

        // Track last pusher
        setObjVar(puck, OBJVAR_LAST_PUSHER, player);
        int pushCount = getIntObjVar(puck, OBJVAR_PUSH_COUNT);
        setObjVar(puck, OBJVAR_PUSH_COUNT, pushCount + 1);

        sendSystemMessage(player, string_id.unlocalized("You kick the puck!"));
    }

    private void resetPuck(obj_id puck, obj_id player) throws InterruptedException
    {
        // Clear all forces
        tangible_dynamics.clearAllForces(puck);

        // Re-enable dynamics
        tangible_dynamics.enableDynamics(puck);
        removeObjVar(puck, "collideBlock");
        setCondition(puck, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

        // Move to player location
        location playerLoc = getLocation(player);
        playerLoc.x += 2.0f; // Offset slightly
        playerLoc.y += 1.0f; // Drop from above
        setLocation(puck, playerLoc);

        // Apply drop bounce
        tangible_dynamics.applyBounceEffect(puck, 9.8f, 0.5f, 2.0f, 2.0f);

        sendSystemMessage(player, string_id.unlocalized("Puck reset to your location."));
    }

    private void showStats(obj_id puck, obj_id player) throws InterruptedException
    {
        int pushCount = getIntObjVar(puck, OBJVAR_PUSH_COUNT);
        obj_id lastPusher = getObjIdObjVar(puck, OBJVAR_LAST_PUSHER);

        String lastPusherName = "None";
        if (isIdValid(lastPusher))
        {
            lastPusherName = getName(lastPusher);
        }

        String stats = "=== PUCK STATS ===\n" +
            "Total Pushes: " + pushCount + "\n" +
            "Last Pusher: " + lastPusherName;

        sendSystemMessage(player, string_id.unlocalized(stats));
    }

    // =====================================================================
    // EVENT HANDLERS
    // =====================================================================

    /**
     * Called when the puck is pushed by collision detection
     * This is triggered by the TangibleDynamics collision system
     */
    public int OnTangibleDynamicsPushed(obj_id self, dictionary params) throws InterruptedException
    {
        if (params != null)
        {
            obj_id pusher = params.getObjId("pusher");
            if (isIdValid(pusher))
            {
                setObjVar(self, OBJVAR_LAST_PUSHER, pusher);
                int pushCount = getIntObjVar(self, OBJVAR_PUSH_COUNT);
                setObjVar(self, OBJVAR_PUSH_COUNT, pushCount + 1);
            }
        }

        return SCRIPT_CONTINUE;
    }
}

