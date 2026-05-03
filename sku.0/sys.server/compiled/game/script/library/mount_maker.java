package script.library;

import script.obj_id;

/**
 * Server-side coupling for dynamic mount authoring: designer attunement objvars plus safety flags.
 * Hardpoint overlay ({@code hp_dyn}) slot selection uses {@link script.library.dynamic_hardpoint#OV_HP_SLOT} on the
 * designer player (shared with {@link script.terminal.gm_dynamic_hardpoint} radials).
 * <p>
 * {@link #possessionEnter} / {@link #possessionLeave} use the normal mount pipeline ({@code mountCreature} /
 * {@code dismountCreature}). Riding does not require {@link #beginDesignerSession}; the mount is invulnerable while
 * ridden, and stays invulnerable after dismount only if a designer session is still active on that creature.
 */
public class mount_maker extends script.base_script
{
    /** Creature being edited references the CSR/designer authoritative id. */
    public static final String OV_CREATURE_DESIGNER = "mount_maker.designer";
    /** Designer references the creature they are editing (cleared on logout by menu). */
    public static final String OV_PLAYER_MOUNT = "mount_maker.editing_creature";

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
            if (getState(designer, STATE_RIDING_MOUNT) > 0 && getMountId(designer) == creature)
                dismountCreature(designer);
            mountMakerPossessionLeave(designer, creature);
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
     * Mount the designer on this creature (normal {@code mountCreature} — same as production mounts, first rider slot).
     * Does <b>not</b> require a designer session. Mount is set invulnerable while ridden; on dismount, invulnerability is
     * cleared unless an active designer session still protects this mount.
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
        return true;
    }

    /** Dismount if riding this mount; clears ride invuln unless designer session still holds the mount protected. */
    public static boolean possessionLeave(obj_id designer, obj_id mount) throws InterruptedException
    {
        if (!isIdValid(designer) || !isIdValid(mount))
            return false;
        boolean ok = false;
        if (getState(designer, STATE_RIDING_MOUNT) > 0 && getMountId(designer) == mount)
        {
            dismountCreature(designer);
            ok = true;
            if (isActiveDesignerSession(designer, mount))
                setInvulnerable(mount, true);
            else
                setInvulnerable(mount, false);
        }
        if (mountMakerPossessionLeave(designer, mount))
            ok = true;
        return ok;
    }

    private mount_maker()
    {
    }
}
