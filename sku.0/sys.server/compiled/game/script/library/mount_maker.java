package script.library;

import script.obj_id;

/**
 * Server-side coupling for dynamic mount authoring: designer attunement objvars plus safety flags.
 * Hardpoint overlay ({@code hp_dyn}) slot selection uses {@link script.library.dynamic_hardpoint#OV_HP_SLOT} on the
 * designer player (shared with {@link script.terminal.gm_dynamic_hardpoint} radials).
 * Optional {@link #possessionEnter}: swap the client's authoritative primary to the creature (real ObjController
 * movement, not decorator {@code moveFurniture}). Client must receive {@code ControlAssumed} for the mount id.
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
     * {@link #endDesignerSession} first, which releases mount-maker possession and would break control if the menu is
     * reopened while possessing the same mount.
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
            mountMakerPossessionLeave(designer, creature);
        removeObjVar(designer, OV_PLAYER_MOUNT);
        if (isIdValid(creature) && exists(creature))
        {
            if (hasObjVar(creature, OV_CREATURE_DESIGNER) && getObjIdObjVar(creature, OV_CREATURE_DESIGNER) == designer)
                removeObjVar(creature, OV_CREATURE_DESIGNER);
            setInvulnerable(creature, false);
        }
    }

    /**
     * Network-level possess: server sets client primary to {@code mount}. Requires god, active designer session
     * on this creature, and an NPC (non-avatar) creature template.
     */
    public static boolean possessionEnter(obj_id designer, obj_id mount) throws InterruptedException
    {
        if (!isDesignerAuthorized(designer) || !isGod(designer) || !isIdValid(mount) || !exists(mount))
            return false;
        if (!hasObjVar(mount, OV_CREATURE_DESIGNER) || getObjIdObjVar(mount, OV_CREATURE_DESIGNER) != designer)
            return false;
        return mountMakerPossessionEnter(designer, mount);
    }

    /** Release {@link #possessionEnter}; safe if not possessing. */
    public static boolean possessionLeave(obj_id designer, obj_id mount) throws InterruptedException
    {
        if (!isIdValid(designer) || !isIdValid(mount))
            return false;
        return mountMakerPossessionLeave(designer, mount);
    }

    private mount_maker()
    {
    }
}
