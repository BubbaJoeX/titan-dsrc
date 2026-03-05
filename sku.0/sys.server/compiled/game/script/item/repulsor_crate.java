package script.item;

/*
@Origin: dsrc.script.item
@Author: Titan Development Team
@Purpose: Repulsor Crate - Star Wars style hovering cargo crate
@Note: When activated, the crate hovers and follows the player.
       When deactivated, it smoothly lands at the player's feet.
@Requirements: TangibleDynamics system, handler script
@Created: 2026
@Copyright © SWG: Titan 2026.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class repulsor_crate extends script.base_script
{
    // Configuration constants
    public static final float HOVER_HEIGHT = 0.8f;           // Height above ground when hovering
    public static final float BOB_AMPLITUDE = 0.05f;         // Subtle bob up/down
    public static final float BOB_SPEED = 0.6f;              // Bob cycles per second
    public static final float FOLLOW_DISTANCE = 2.0f;        // Distance behind player
    public static final float FOLLOW_SPEED = 4.0f;           // Movement speed when following
    public static final float LANDING_HEIGHT = 0.0f;         // Height when landed (at feet)
    public static final float ACTIVATION_RANGE = 10.0f;      // Max range to activate

    // ObjVar names
    public static final String VAR_ACTIVE = "repulsor.active";
    public static final String VAR_OWNER = "repulsor.owner";

    // =====================================================================
    // INITIALIZATION
    // =====================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        // Ensure the dynamics handler is attached
        if (!hasScript(self, "handler.tangible_dynamics_handler"))
        {
            attachScript(self, "handler.tangible_dynamics_handler");
        }
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // OBJECT MENU
    // =====================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        boolean isActive = hasObjVar(self, VAR_ACTIVE) && getBooleanObjVar(self, VAR_ACTIVE);

        // Only check range if crate is NOT active (if it's active and following, always allow interaction)
        if (!isActive && getDistance(self, player) > ACTIVATION_RANGE)
            return SCRIPT_CONTINUE;

        obj_id currentOwner = null;
        if (hasObjVar(self, VAR_OWNER))
        {
            currentOwner = getObjIdObjVar(self, VAR_OWNER);
        }

        if (!isActive)
        {
            // Crate is landed - show activate option
            mi.addRootMenu(menu_info_types.SERVER_ITEM_OPTIONS, string_id.unlocalized("Activate Repulsor"));
            mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Disable Repulsor"));
        }
        else
        {
            // Crate is hovering
            if (isIdValid(currentOwner) && currentOwner.equals(player))
            {
                // This player owns the crate - show deactivate option
                mi.addRootMenu(menu_info_types.SERVER_ITEM_OPTIONS, string_id.unlocalized("Deactivate Repulsor"));
                mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Disable Repulsor"));
            }
            else if (isIdValid(currentOwner))
            {
                // Someone else owns it - show in use (not clickable)
                mi.addRootMenu(menu_info_types.SERVER_ITEM_OPTIONS, string_id.unlocalized("In Use"));
            }
            else
            {
                // Active but no valid owner - allow anyone to deactivate
                mi.addRootMenu(menu_info_types.SERVER_ITEM_OPTIONS, string_id.unlocalized("Deactivate Repulsor"));
                mi.addRootMenu(menu_info_types.SERVER_MENU2, string_id.unlocalized("Disable Repulsor"));
            }
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        // Handle Disable Repulsor option
        if (item == menu_info_types.SERVER_MENU2)
        {
            disableRepulsor(self, player);
            return SCRIPT_CONTINUE;
        }

        if (item != menu_info_types.SERVER_ITEM_OPTIONS)
            return SCRIPT_CONTINUE;

        boolean isActive = hasObjVar(self, VAR_ACTIVE) && getBooleanObjVar(self, VAR_ACTIVE);
        obj_id currentOwner = null;
        if (hasObjVar(self, VAR_OWNER))
        {
            currentOwner = getObjIdObjVar(self, VAR_OWNER);
        }

        if (!isActive)
        {
            // Activate the repulsor
            activateRepulsor(self, player);
        }
        else if (!isIdValid(currentOwner) || currentOwner.equals(player))
        {
            // Deactivate the repulsor (owner or no owner)
            deactivateRepulsor(self, player);
        }
        else
        {
            // In use by someone else
            sendSystemMessageTestingOnly(player, "This repulsor crate is being used by someone else.");
        }

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // ACTIVATION / DEACTIVATION
    // =====================================================================

    /**
     * Activate the repulsor crate - start hovering and following the player
     */
    public void activateRepulsor(obj_id self, obj_id player) throws InterruptedException
    {
        // Check range
        if (getDistance(self, player) > ACTIVATION_RANGE)
        {
            sendSystemMessageTestingOnly(player, "You are too far away from the crate.");
            return;
        }

        // Set ownership
        setObjVar(self, VAR_ACTIVE, true);
        setObjVar(self, VAR_OWNER, player);

        // Enable dynamics condition
        setCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

        // Apply follow effect - crate follows the player
        tangible_dynamics.applyFollowTargetEffect(self, player, FOLLOW_DISTANCE, FOLLOW_SPEED, HOVER_HEIGHT, BOB_AMPLITUDE, -1.0f);

        // Visual/audio feedback
        sendSystemMessageTestingOnly(player, "Repulsor crate activated. The crate now follows you.");

        // Play activation sound effect
        playClientEffectObj(player, "sound/item_repulsor_activate.snd", self, "");
    }

    /**
     * Deactivate the repulsor crate - land it at its CURRENT position
     * Uses the crate's actual position as source of truth, not the player's position
     */
    public void deactivateRepulsor(obj_id self, obj_id player) throws InterruptedException
    {
        // Clear follow effect first
        tangible_dynamics.clearFollowTargetEffect(self);

        // Get the CRATE's current position - this is the source of truth
        location crateLoc = getLocation(self);

        // Get terrain height at the crate's current position for landing
        float terrainHeight = getHeightAtLocation(crateLoc.x, crateLoc.z);
        if (terrainHeight < -100000.0f)
        {
            // Fallback if terrain query fails
            terrainHeight = crateLoc.y - HOVER_HEIGHT;
        }

        // Land the crate at its current X/Z position, but on the ground
        location landingLoc = new location(
            crateLoc.x,
            terrainHeight + LANDING_HEIGHT,
            crateLoc.z,
            crateLoc.area,
            crateLoc.cell
        );

        // Move the crate to the landing position immediately
        setLocation(self, landingLoc);

        // Clear ownership
        removeObjVar(self, VAR_ACTIVE);
        removeObjVar(self, VAR_OWNER);

        // Clear all dynamics and condition
        tangible_dynamics.clearAllForces(self);
        clearCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

        // Visual/audio feedback
        sendSystemMessageTestingOnly(player, "Repulsor crate deactivated. The crate has landed.");

        // Play deactivation sound effect
        playClientEffectObj(player, "sound/item_repulsor_deactivate.snd", self, "");
    }

    /**
     * Message handler for when landing is complete
     */
    public int handleLandingComplete(obj_id self, dictionary params) throws InterruptedException
    {
        // Clear all dynamics
        tangible_dynamics.clearAllForces(self);
        clearCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

        // Set final position on ground
        if (params != null)
        {
            float landX = params.getFloat("landX");
            float landY = params.getFloat("landY");
            float landZ = params.getFloat("landZ");
            String area = params.getString("area");

            // Get terrain height at landing position for proper grounding
            location finalLoc = new location(landX, landY, landZ, area, null);
            float terrainHeight = getHeightAtLocation(landX, landZ);
            if (terrainHeight > -100000.0f)
            {
                finalLoc.y = terrainHeight;
            }

            setLocation(self, finalLoc);
        }

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // SAFETY CHECKS
    // =====================================================================

    /**
     * If the owner logs out or goes too far, deactivate the crate
     */
    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws InterruptedException
    {
        // This is just a placeholder - actual disconnect handling would be via other triggers
        return SCRIPT_CONTINUE;
    }

    /**
     * Periodic check to ensure owner is still valid and in range
     */
    public int handleOwnerCheck(obj_id self, dictionary params) throws InterruptedException
    {
        boolean isActive = getBooleanObjVar(self, VAR_ACTIVE);
        if (!isActive)
            return SCRIPT_CONTINUE;

        obj_id owner = getObjIdObjVar(self, VAR_OWNER);
        if (!isIdValid(owner) || !exists(owner))
        {
            // Owner no longer exists, emergency land
            emergencyLand(self);
            return SCRIPT_CONTINUE;
        }

        // Check range
        float distance = getDistance(self, owner);
        if (distance > ACTIVATION_RANGE * 3.0f)
        {
            // Too far away, emergency land
            sendSystemMessageTestingOnly(owner, "Repulsor crate lost connection - landing.");
            emergencyLand(self);
            return SCRIPT_CONTINUE;
        }

        // Schedule next check
        messageTo(self, "handleOwnerCheck", null, 5.0f, false);

        return SCRIPT_CONTINUE;
    }

    /**
     * Emergency landing - immediate stop and land
     */
    public void emergencyLand(obj_id self) throws InterruptedException
    {
        tangible_dynamics.clearAllForces(self);
        clearCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
        removeObjVar(self, VAR_ACTIVE);
        removeObjVar(self, VAR_OWNER);

        // Drop to terrain
        location loc = getLocation(self);
        float terrainHeight = getHeightAtLocation(loc.x, loc.z);
        if (terrainHeight > -100000.0f)
        {
            loc.y = terrainHeight;
            setLocation(self, loc);
        }
    }

    /**
     * Disable the repulsor completely - removes the script from the object
     */
    public void disableRepulsor(obj_id self, obj_id player) throws InterruptedException
    {
        // First, land the crate if it's active
        boolean isActive = getBooleanObjVar(self, VAR_ACTIVE);
        if (isActive)
        {
            emergencyLand(self);
        }

        // Clear all dynamics and condition
        tangible_dynamics.clearAllForces(self);
        clearCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);

        // Remove all objvars
        removeObjVar(self, VAR_ACTIVE);
        removeObjVar(self, VAR_OWNER);

        // Remove the handler script
        if (hasScript(self, "handler.tangible_dynamics_handler"))
        {
            detachScript(self, "handler.tangible_dynamics_handler");
        }

        // Visual/audio feedback
        sendSystemMessageTestingOnly(player, "Repulsor system disabled and removed from object.");

        // Remove this script last
        detachScript(self, "item.repulsor_crate");
    }

    // =====================================================================
    // COMMAND INTERFACE (for GM/dev use)
    // =====================================================================

    /**
     * Alternative activation via command
     */
    public int cmdActivateRepulsor(obj_id self, obj_id target, String params, float defaultTime, obj_id invoker) throws InterruptedException
    {
        if (!isIdValid(invoker))
            return SCRIPT_CONTINUE;

        boolean isActive = getBooleanObjVar(self, VAR_ACTIVE);

        if (!isActive)
        {
            activateRepulsor(self, invoker);
        }
        else
        {
            obj_id owner = getObjIdObjVar(self, VAR_OWNER);
            if (isIdValid(owner) && owner.equals(invoker))
            {
                deactivateRepulsor(self, invoker);
            }
        }

        return SCRIPT_CONTINUE;
    }
}


