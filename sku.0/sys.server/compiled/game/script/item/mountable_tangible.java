package script.item;

/*
@Origin: dsrc.script.item
@Author: Titan Development Team
@Purpose: Allows players to sit/mount on tangible objects and move with them
@Note: Works with TangibleDynamics - players will move with hovering, carousel, etc.
@Requirements: TangibleDynamics system
@Created: 2026
@Copyright © SWG: Titan 2026.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class mountable_tangible extends script.base_script
{
    // Objvar keys
    public static final String OBJVAR_MOUNTED_PLAYER = "mountable.mountedPlayer";
    public static final String OBJVAR_SEAT_OFFSET_X = "mountable.seatOffsetX";
    public static final String OBJVAR_SEAT_OFFSET_Y = "mountable.seatOffsetY";
    public static final String OBJVAR_SEAT_OFFSET_Z = "mountable.seatOffsetZ";
    public static final String OBJVAR_LOCK_ORIENTATION = "mountable.lockOrientation";
    public static final String OBJVAR_MOUNT_TIME = "mountable.mountTime";

    // Default seat offset (player sits on top of object)
    public static final float DEFAULT_SEAT_OFFSET_Y = 0.5f;

    // Update interval for position sync (seconds)
    public static final float UPDATE_INTERVAL = 0.1f;

    // =====================================================================
    // INITIALIZATION
    // =====================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        // Set default seat offset if not already set
        if (!hasObjVar(self, OBJVAR_SEAT_OFFSET_Y))
        {
            setObjVar(self, OBJVAR_SEAT_OFFSET_X, 0.0f);
            setObjVar(self, OBJVAR_SEAT_OFFSET_Y, DEFAULT_SEAT_OFFSET_Y);
            setObjVar(self, OBJVAR_SEAT_OFFSET_Z, 0.0f);
        }
        // Default to locking orientation (true riding experience)
        if (!hasObjVar(self, OBJVAR_LOCK_ORIENTATION))
        {
            setObjVar(self, OBJVAR_LOCK_ORIENTATION, 1);
        }
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // RADIAL MENU
    // =====================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        // Check if someone is already mounted
        obj_id mountedPlayer = getMountedPlayer(self);

        if (isIdValid(mountedPlayer))
        {
            // Only the mounted player can dismount
            if (mountedPlayer.equals(player))
            {
                mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Dismount"));
            }
        }
        else
        {
            // No one mounted - show mount option
            mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Sit / Mount"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1)
        {
            obj_id mountedPlayer = getMountedPlayer(self);

            if (isIdValid(mountedPlayer) && mountedPlayer.equals(player))
            {
                // Dismount
                dismountPlayer(self, player);
            }
            else if (!isIdValid(mountedPlayer))
            {
                // Mount
                mountPlayer(self, player);
            }
            else
            {
                sendSystemMessage(player, string_id.unlocalized("Someone is already seated on this object."));
            }
        }

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // MOUNT / DISMOUNT
    // =====================================================================

    /**
     * Mount a player onto this object
     */
    public void mountPlayer(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isIdValid(self) || !isIdValid(player))
            return;

        // Check distance - must be within 3m
        float distance = getDistance(self, player);
        if (distance > 3.0f)
        {
            sendSystemMessage(player, string_id.unlocalized("You are too far away to sit on that."));
            return;
        }

        // Get seat offset
        float offsetX = getFloatObjVar(self, OBJVAR_SEAT_OFFSET_X);
        float offsetY = getFloatObjVar(self, OBJVAR_SEAT_OFFSET_Y);
        float offsetZ = getFloatObjVar(self, OBJVAR_SEAT_OFFSET_Z);

        // Get lock orientation setting (default to true for true riding experience)
        boolean lockOrientation = true;
        if (hasObjVar(self, OBJVAR_LOCK_ORIENTATION))
        {
            lockOrientation = (getIntObjVar(self, OBJVAR_LOCK_ORIENTATION) != 0);
        }

        // Store mounted player
        setObjVar(self, OBJVAR_MOUNTED_PLAYER, player);
        setObjVar(self, OBJVAR_MOUNT_TIME, getGameTime());

        // Store original player location for dismount
        location playerLoc = getLocation(player);
        setObjVar(player, "mountable.originalX", playerLoc.x);
        setObjVar(player, "mountable.originalY", playerLoc.y);
        setObjVar(player, "mountable.originalZ", playerLoc.z);
        setObjVar(player, "mountable.mountedObject", self);

        // Put player in sitting posture
        setPosture(player, POSTURE_SITTING);

        // Mount the player on the tangible object (sends message to client for seamless position locking)
        mountTangibleObject(player, self, offsetX, offsetY, offsetZ, lockOrientation);

        // Start server-side validation loop (checks posture, disconnects, etc.)
        messageTo(self, "OnMountPositionUpdate", null, 1.0f, false);

        // Notify player
        sendSystemMessage(player, string_id.unlocalized("You sit on the object."));

        // Log
        LOG("mountable", player + " mounted " + self);
    }

    /**
     * Dismount a player from this object
     */
    public void dismountPlayer(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isIdValid(self) || !isIdValid(player))
            return;

        // Clear mounted player reference
        removeObjVar(self, OBJVAR_MOUNTED_PLAYER);
        removeObjVar(self, OBJVAR_MOUNT_TIME);

        // Dismount from tangible object (sends message to client to unlock position)
        dismountTangibleObject(player);

        // Get current object position for dismount location
        location objectLoc = getLocation(self);

        // Place player slightly offset from object
        location dismountLoc = new location();
        dismountLoc.x = objectLoc.x + 1.0f;
        dismountLoc.y = objectLoc.y;
        dismountLoc.z = objectLoc.z;
        dismountLoc.area = objectLoc.area;
        dismountLoc.cell = objectLoc.cell;

        // Clear player's mount reference
        removeObjVar(player, "mountable.originalX");
        removeObjVar(player, "mountable.originalY");
        removeObjVar(player, "mountable.originalZ");
        removeObjVar(player, "mountable.mountedObject");

        // Move player to dismount location
        setLocation(player, dismountLoc);

        // Restore standing posture
        setPosture(player, POSTURE_UPRIGHT);

        // Notify player
        sendSystemMessage(player, string_id.unlocalized("You dismount from the object."));

        // Log
        LOG("mountable", player + " dismounted from " + self);
    }

    // =====================================================================
    // POSITION UPDATES (Server-side validation only - client handles smooth movement)
    // =====================================================================

    /**
     * Called periodically to validate mounted player state
     * Note: Actual position updates are handled client-side for seamless movement
     */
    public int OnMountPositionUpdate(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = getMountedPlayer(self);

        if (!isIdValid(player))
        {
            // No mounted player, stop updates
            return SCRIPT_CONTINUE;
        }

        // Check if player is still in sitting posture
        int posture = getPosture(player);
        if (posture != POSTURE_SITTING)
        {
            // Player stood up or moved - dismount
            dismountPlayer(self, player);
            return SCRIPT_CONTINUE;
        }

        // Check if player still has mount reference
        if (!isMountedOnTangibleObject(player))
        {
            // Client-side mount was lost - dismount on server
            dismountPlayer(self, player);
            return SCRIPT_CONTINUE;
        }

        // Schedule next validation check (less frequent since client handles movement)
        messageTo(self, "OnMountPositionUpdate", null, 1.0f, false);

        return SCRIPT_CONTINUE;
    }

    /**
     * Update the mounted player's position to match the object's current position
     * Note: This is now only used for server-side validation, not smooth movement
     */
    private void updateMountedPlayerPosition(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isIdValid(self) || !isIdValid(player))
            return;

        // Get object's current position
        location objectLoc = getLocation(self);

        // Get seat offset
        float offsetX = getFloatObjVar(self, OBJVAR_SEAT_OFFSET_X);
        float offsetY = getFloatObjVar(self, OBJVAR_SEAT_OFFSET_Y);
        float offsetZ = getFloatObjVar(self, OBJVAR_SEAT_OFFSET_Z);

        // Calculate player position
        location playerLoc = new location();
        playerLoc.x = objectLoc.x + offsetX;
        playerLoc.y = objectLoc.y + offsetY;
        playerLoc.z = objectLoc.z + offsetZ;
        playerLoc.area = objectLoc.area;
        playerLoc.cell = objectLoc.cell;

        // Move player to new position (without changing posture)
        setLocation(player, playerLoc);
    }

    // =====================================================================
    // EVENT HANDLERS
    // =====================================================================

    /**
     * Handle object being destroyed while player is mounted
     */
    public int OnDestroy(obj_id self) throws InterruptedException
    {
        obj_id player = getMountedPlayer(self);
        if (isIdValid(player))
        {
            // Force dismount
            removeObjVar(player, "mountable.mountedObject");
            setPosture(player, POSTURE_UPRIGHT);
            sendSystemMessage(player, string_id.unlocalized("The object you were sitting on was destroyed."));
        }
        return SCRIPT_CONTINUE;
    }

    /**
     * Handle dynamics updates - this is called when the object's dynamics position changes
     */
    public int OnTangibleDynamicsPositionUpdate(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = getMountedPlayer(self);
        if (isIdValid(player))
        {
            updateMountedPlayerPosition(self, player);
        }
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    /**
     * Get the currently mounted player, or null if none
     */
    private obj_id getMountedPlayer(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, OBJVAR_MOUNTED_PLAYER))
        {
            return getObjIdObjVar(self, OBJVAR_MOUNTED_PLAYER);
        }
        return null;
    }

    /**
     * Set the seat offset for this mountable object
     */
    public static void setSeatOffset(obj_id target, float x, float y, float z) throws InterruptedException
    {
        setObjVar(target, OBJVAR_SEAT_OFFSET_X, x);
        setObjVar(target, OBJVAR_SEAT_OFFSET_Y, y);
        setObjVar(target, OBJVAR_SEAT_OFFSET_Z, z);
    }

    /**
     * Make an object mountable (attach this script)
     */
    public static void makeMountable(obj_id target) throws InterruptedException
    {
        if (!hasScript(target, "item.mountable_tangible"))
        {
            attachScript(target, "item.mountable_tangible");
        }
    }

    /**
     * Make an object mountable with custom seat offset
     */
    public static void makeMountable(obj_id target, float seatOffsetY) throws InterruptedException
    {
        makeMountable(target);
        setSeatOffset(target, 0.0f, seatOffsetY, 0.0f);
    }
}

