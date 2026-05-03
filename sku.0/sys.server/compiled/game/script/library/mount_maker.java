package script.library;

import script.obj_id;

/**
 * Server-side coupling for dynamic mount authoring: designer attunement objvars plus safety flags.
 * Hardpoint overlay ({@code hp_dyn}) slot selection uses {@link script.library.dynamic_hardpoint#OV_HP_SLOT} on the
 * designer player (shared with {@link script.terminal.gm_dynamic_hardpoint} radials).
 * <p>
 * {@link #possessionEnter}: {@code mountCreature} (rider slotted / seat metadata) then {@code mountMakerPossessionEnter}
 * so client primary follows the mount — movement and rider stay one unit. {@link #possessionLeave} ends possession
 * first, then dismounts. Use {@link #emergencyUnmountAll}, {@link #onPlayerLogoutCleanup}, or {@code /mountMakerExit}
 * if the listbox is unavailable. Riding does not require {@link #beginDesignerSession}; mount is invulnerable while ridden.
 */
public class mount_maker extends script.base_script
{
    /** Creature being edited references the CSR/designer authoritative id. */
    public static final String OV_CREATURE_DESIGNER = "mount_maker.designer";
    /** Designer references the creature they are editing (cleared on logout by menu). */
    public static final String OV_PLAYER_MOUNT = "mount_maker.editing_creature";

    /** Matches {@link script.creature.creature_dynamic_mount#SCRIPTVAR_MM_AUTH_CREATURE} (avoid circular import). */
    private static final String SCRIPTVAR_MM_AUTH_CREATURE = "creature_dynamic_mount.mm_auth_creature";

    public static boolean isDesignerAuthorized(obj_id player) throws InterruptedException
    {
        return isIdValid(player) && (isGod(player) || hasObjVar(player, "test_center"));
    }

    /**
     * Start a designer session on {@code creature} only if one is not already active for this creature/designer pair.
     * Avoids calling {@link #beginDesignerSession} when already in sync — {@link #beginDesignerSession} always runs
     * {@link #endDesignerSession} first, which would dismount / clear session if the menu is reopened carelessly.
     */
    public static void ensureDesignerSessionForCreature(obj_id creature, obj_id designer) throws InterruptedException
    {
        if (!isIdValid(creature) || !isIdValid(designer) || !isMob(creature) || !isDesignerAuthorized(designer))
            return;
        if (hasObjVar(creature, OV_CREATURE_DESIGNER) && getObjIdObjVar(creature, OV_CREATURE_DESIGNER) == designer
                && hasObjVar(designer, OV_PLAYER_MOUNT) && getObjIdObjVar(designer, OV_PLAYER_MOUNT) == creature)
            return;
        beginDesignerSession(creature, designer);
    }

    public static void beginDesignerSession(obj_id creature, obj_id designer) throws InterruptedException
    {
        if (!isIdValid(creature) || !isIdValid(designer) || !isMob(creature) || !isDesignerAuthorized(designer))
            return;
        endDesignerSession(designer);
        setObjVar(creature, OV_CREATURE_DESIGNER, designer);
        setObjVar(designer, OV_PLAYER_MOUNT, creature);
        setInvulnerable(creature, true);
        ai_lib.setIgnoreCombat(creature);
        posture.stand(creature);
    }

    public static void endDesignerSession(obj_id designer) throws InterruptedException
    {
        if (!isIdValid(designer) || !hasObjVar(designer, OV_PLAYER_MOUNT))
            return;
        obj_id creature = getObjIdObjVar(designer, OV_PLAYER_MOUNT);
        if (isIdValid(creature) && exists(creature))
        {
            mountMakerPossessionLeave(designer, creature);
            if (getState(designer, STATE_RIDING_MOUNT) > 0 && getMountId(designer) == creature)
                dismountCreature(designer);
        }
        removeObjVar(designer, OV_PLAYER_MOUNT);
        if (isIdValid(creature) && exists(creature))
        {
            if (hasObjVar(creature, OV_CREATURE_DESIGNER) && getObjIdObjVar(creature, OV_CREATURE_DESIGNER) == designer)
                removeObjVar(creature, OV_CREATURE_DESIGNER);
            setInvulnerable(creature, false);
        }
    }

    private static boolean isActiveDesignerSession(obj_id designer, obj_id mount) throws InterruptedException
    {
        return hasObjVar(mount, OV_CREATURE_DESIGNER) && getObjIdObjVar(mount, OV_CREATURE_DESIGNER) == designer
                && hasObjVar(designer, OV_PLAYER_MOUNT) && getObjIdObjVar(designer, OV_PLAYER_MOUNT) == mount;
    }

    /**
     * Dismount, end mount-maker possession, end designer session, and clear mount SUI scriptvar — safe if nothing active.
     */
    public static void emergencyUnmountAll(obj_id designer) throws InterruptedException
    {
        if (!isIdValid(designer))
            return;
        obj_id ridMount = (getState(designer, STATE_RIDING_MOUNT) > 0) ? getMountId(designer) : null;
        if (isIdValid(ridMount) && exists(ridMount))
            mountMakerPossessionLeave(designer, ridMount);
        if (getState(designer, STATE_RIDING_MOUNT) > 0)
            dismountCreature(designer);
        endDesignerSession(designer);
        if (isIdValid(ridMount) && exists(ridMount) && !hasObjVar(ridMount, OV_CREATURE_DESIGNER))
            setInvulnerable(ridMount, false);
        utils.removeScriptVar(designer, SCRIPTVAR_MM_AUTH_CREATURE);
    }

    /** Called from player logout handlers so control / session cannot outlive the client. */
    public static void onPlayerLogoutCleanup(obj_id designer) throws InterruptedException
    {
        emergencyUnmountAll(designer);
    }

    /**
     * Mount then assume client primary on the mount (same pipeline as production mount + authoring drive).
     * Does not require a designer session. Mount is invulnerable while ridden.
     */
    public static boolean possessionEnter(obj_id designer, obj_id mount) throws InterruptedException
    {
        if (!isDesignerAuthorized(designer) || !isIdValid(mount) || !exists(mount))
            return false;
        if (!getMountsEnabled())
            return false;
        if (getState(designer, STATE_RIDING_MOUNT) > 0)
            return getMountId(designer) == mount;
        if (!hasObjVar(mount, dynamic_mount.VAR_DM_ACTIVE))
            setObjVar(mount, dynamic_mount.VAR_DM_ACTIVE, 1);
        if (!hasObjVar(mount, dynamic_mount.VAR_DM_CAPACITY))
            dynamic_mount.ensureMountDefaults(mount, 1);
        if (!makeDynamicMountable(mount))
            return false;
        if (!doesMountHaveRoom(mount))
            return false;
        if (!mountCreature(designer, mount))
            return false;
        setInvulnerable(mount, true);
        mountMakerPossessionEnter(designer, mount);
        return true;
    }

    /** End possession first, then dismount; restores avatar primary before container detach. */
    public static boolean possessionLeave(obj_id designer, obj_id mount) throws InterruptedException
    {
        if (!isIdValid(designer) || !isIdValid(mount))
            return false;
        boolean ok = false;
        if (mountMakerPossessionLeave(designer, mount))
            ok = true;
        if (getState(designer, STATE_RIDING_MOUNT) > 0 && getMountId(designer) == mount)
        {
            dismountCreature(designer);
            ok = true;
            if (isActiveDesignerSession(designer, mount))
                setInvulnerable(mount, true);
            else
                setInvulnerable(mount, false);
        }
        return ok;
    }

    private mount_maker()
    {
    }
}
