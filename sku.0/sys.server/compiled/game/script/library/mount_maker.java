package script.library;

import script.obj_id;

/**
 * Server-side coupling for dynamic mount authoring: designer attunement objvars plus safety flags.
 * Client drive mode uses decorator WASD ({@code /mountMakerDrive}); see {@link script.creature.creature_dynamic_mount}.
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
        removeObjVar(designer, OV_PLAYER_MOUNT);
        if (isIdValid(creature) && exists(creature))
        {
            if (hasObjVar(creature, OV_CREATURE_DESIGNER) && getObjIdObjVar(creature, OV_CREATURE_DESIGNER) == designer)
                removeObjVar(creature, OV_CREATURE_DESIGNER);
            setInvulnerable(creature, false);
        }
    }

    private mount_maker()
    {
    }
}
