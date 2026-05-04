package script.library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import script.obj_id;
import script.string_id;

/**
 * Dynamic mount authoring: designer session, ride + drive, and preset I/O ({@code mount.dm.*} / {@code hp_dyn.*}).
 * Preset helpers live here (formerly {@code dynamic_mount}) so one compiled unit loads with mount maker workflows.
 * <p>
 * Hardpoint overlay ({@code hp_dyn}) slot selection uses {@link script.library.dynamic_hardpoint#OV_HP_SLOT} on the
 * designer player (shared with {@link script.terminal.gm_dynamic_hardpoint} radials).
 * <p>
 * {@link #possessionEnter}: {@code mountCreature} then {@code mountMakerPossessionEnter}. Use {@link #emergencyUnmountAll},
 * {@link #onPlayerLogoutCleanup}, or {@code /mountMakerExit} if the listbox is unavailable.
 */
public class mount_maker extends script.base_script
{
    public static final String VAR_DM_ACTIVE = "mount.dm.active";
    public static final String VAR_DM_CAPACITY = "mount.dm.capacity";

    private static final String PRESET_DIR = "/home/swg/swg-main/exe/linux/var/mounting_presets/";

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

    public static void ensureMountDefaults(obj_id creature, int capacity) throws InterruptedException
    {
        capacity = Math.min(8, Math.max(1, capacity));
        setObjVar(creature, VAR_DM_ACTIVE, 1);
        setObjVar(creature, VAR_DM_CAPACITY, capacity);

        for (int i = 0; i < capacity; ++i)
        {
            String base = "mount.dm.seat." + i + ".";
            if (!hasObjVar(creature, base + "pose"))
                setObjVar(creature, base + "pose", "normal");
            if (!hasObjVar(creature, base + "ox"))
                setObjVar(creature, base + "ox", 0.f);
            if (!hasObjVar(creature, base + "oy"))
                setObjVar(creature, base + "oy", 0.f);
            if (!hasObjVar(creature, base + "oz"))
                setObjVar(creature, base + "oz", 0.f);
        }
    }

    public static File presetFileForName(String baseName)
    {
        if (baseName == null || baseName.isEmpty())
            baseName = "unnamed";
        if (baseName.endsWith(".mountpreset"))
            return new File(PRESET_DIR + baseName);
        return new File(PRESET_DIR + baseName + ".mountpreset");
    }

    public static void exportObjVarsToFile(obj_id creature, String baseName) throws IOException, InterruptedException
    {
        File out = presetFileForName(baseName);
        File parent = out.getParentFile();
        if (parent != null)
            parent.mkdirs();

        BufferedWriter w = new BufferedWriter(new FileWriter(out));
        try
        {
            w.write("# Dynamic mount preset (key=value per line). Keys are objvar paths.\n");
            w.write("mount.dm.active=" + (hasObjVar(creature, VAR_DM_ACTIVE) ? Integer.toString(getIntObjVar(creature, VAR_DM_ACTIVE)) : "1"));
            w.write("\n");
            if (hasObjVar(creature, VAR_DM_CAPACITY))
            {
                w.write("mount.dm.capacity=" + Integer.toString(getIntObjVar(creature, VAR_DM_CAPACITY)));
                w.write("\n");
            }

            int cap = hasObjVar(creature, VAR_DM_CAPACITY) ? getIntObjVar(creature, VAR_DM_CAPACITY) : 1;
            cap = Math.min(8, Math.max(1, cap));
            for (int i = 0; i < cap; ++i)
            {
                String pPose = "mount.dm.seat." + i + ".pose";
                String pox = "mount.dm.seat." + i + ".ox";
                String poy = "mount.dm.seat." + i + ".oy";
                String poz = "mount.dm.seat." + i + ".oz";
                if (hasObjVar(creature, pPose))
                {
                    w.write(pPose + "=" + getStringObjVar(creature, pPose));
                    w.write("\n");
                }
                if (hasObjVar(creature, pox))
                {
                    w.write(pox + "=" + Float.toString(getFloatObjVar(creature, pox)));
                    w.write("\n");
                }
                if (hasObjVar(creature, poy))
                {
                    w.write(poy + "=" + Float.toString(getFloatObjVar(creature, poy)));
                    w.write("\n");
                }
                if (hasObjVar(creature, poz))
                {
                    w.write(poz + "=" + Float.toString(getFloatObjVar(creature, poz)));
                    w.write("\n");
                }
            }

            for (int s = 0; s < 32; ++s)
            {
                String root = "hp_dyn." + s;
                if (!hasObjVar(creature, root))
                    continue;
                String[] keys =
                { "kind", "hp", "path", "ox", "oy", "oz", "r", "g", "b", "range", "intensity", "scale" };
                for (int k = 0; k < keys.length; ++k)
                {
                    String full = root + "." + keys[k];
                    if (!hasObjVar(creature, full))
                        continue;
                    if (keys[k].equals("kind") || keys[k].equals("hp") || keys[k].equals("path"))
                    {
                        w.write(full + "=" + getStringObjVar(creature, full));
                    }
                    else
                    {
                        w.write(full + "=" + Float.toString(getFloatObjVar(creature, full)));
                    }
                    w.write("\n");
                }
            }
        }
        finally
        {
            w.close();
        }
    }

    public static void applyPresetFromFile(obj_id creature, String baseName) throws IOException, InterruptedException
    {
        File in = presetFileForName(baseName);
        if (!in.exists())
            throw new IOException("Preset not found: " + in.getAbsolutePath());

        removeObjVar(creature, "mount.dm");
        removeObjVar(creature, "hp_dyn");

        BufferedReader r = new BufferedReader(new FileReader(in));
        try
        {
            String line;
            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#"))
                    continue;
                int eq = line.indexOf('=');
                if (eq <= 0)
                    continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                if (key.length() == 0)
                    continue;

                if (key.endsWith(".kind") || key.endsWith(".hp") || key.endsWith(".path") || key.endsWith(".pose"))
                {
                    setObjVar(creature, key, val);
                }
                else
                {
                    try
                    {
                        if (val.indexOf('.') >= 0 || val.indexOf('e') >= 0 || val.indexOf('E') >= 0)
                            setObjVar(creature, key, Float.parseFloat(val));
                        else
                            setObjVar(creature, key, Integer.parseInt(val));
                    }
                    catch (NumberFormatException ex)
                    {
                        setObjVar(creature, key, val);
                    }
                }
            }
        }
        finally
        {
            r.close();
        }

        if (!hasObjVar(creature, VAR_DM_ACTIVE))
            setObjVar(creature, VAR_DM_ACTIVE, 1);
        if (!hasObjVar(creature, VAR_DM_CAPACITY))
            setObjVar(creature, VAR_DM_CAPACITY, 1);
    }

    /** Alias for GM/script spawn: apply exported {@code mount.dm} + {@code hp_dyn} preset to a creature instance. */
    public static void applyPresetToSpawn(obj_id creature, String baseName) throws IOException, InterruptedException
    {
        applyPresetFromFile(creature, baseName);
    }

    /**
     * Start a designer session on {@code creature} only if one is not already active for this creature/designer pair.
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

    public static void onPlayerLogoutCleanup(obj_id designer) throws InterruptedException
    {
        emergencyUnmountAll(designer);
    }

    public static boolean possessionEnter(obj_id designer, obj_id mount) throws InterruptedException
    {
        if (!isDesignerAuthorized(designer) || !isIdValid(mount) || !exists(mount))
            return false;
        if (!getMountsEnabled())
            return false;
        if (getState(designer, STATE_RIDING_MOUNT) > 0)
            return getMountId(designer) == mount;
        if (!hasObjVar(mount, VAR_DM_ACTIVE))
            setObjVar(mount, VAR_DM_ACTIVE, 1);
        if (!hasObjVar(mount, VAR_DM_CAPACITY))
            ensureMountDefaults(mount, 1);
        ensureDesignerSessionForCreature(mount, designer);
        posture.stand(designer);
        if (!makeDynamicMountable(mount))
            return false;
        if (!doesMountHaveRoom(mount))
            return false;
        if (!mountCreature(designer, mount))
            return false;
        setInvulnerable(mount, true);
        if (!mountMakerPossessionEnter(designer, mount))
        {
            dismountCreature(designer);
            sendSystemMessage(designer, string_id.unlocalized(
                    "Mount maker: mounted then possession failed — dismounted. Update server; mount.dm bypasses can_create_avatar and script TRIG blocks for rider transfer."));
            return false;
        }
        return true;
    }

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

    /**
     * Radial / command helpers: finalized dynamic mounts ({@link #VAR_DM_ACTIVE}) use possession + {@link #possessionLeave}
     * so {@code /dismount} must run that path, not only {@link pet_lib#doDismountNow}.
     */
    public static boolean canRadialMountDynamic(obj_id mount, obj_id player) throws InterruptedException
    {
        if (!getMountsEnabled() || !isIdValid(mount) || !exists(mount) || !isIdValid(player) || !exists(player))
            return false;
        if (!hasObjVar(mount, VAR_DM_ACTIVE))
            return false;
        if (isDead(mount) || ai_lib.isIncapacitated(mount))
            return false;
        if (ai_lib.aiIsDead(player))
            return false;
        obj_id cur = getMountId(player);
        if (isIdValid(cur))
            return false;
        int dist = (int)(getDistance(mount, player));
        if (dist > pet_lib.MAX_PET_MOUNT_OFFER_DISTANCE)
            return false;
        return doesMountHaveRoom(mount);
    }

    public static boolean isMountedOnDynamicMount(obj_id mount, obj_id player) throws InterruptedException
    {
        if (!isIdValid(mount) || !exists(mount) || !isIdValid(player) || !exists(player))
            return false;
        if (!hasObjVar(mount, VAR_DM_ACTIVE))
            return false;
        return getMountId(player) == mount && getState(player, STATE_RIDING_MOUNT) > 0;
    }

    /**
     * Player faces mount, then mounts: designers use {@link #possessionEnter} (drive); others use {@code mountCreature} only.
     */
    public static boolean mountFromRadial(obj_id player, obj_id mount) throws InterruptedException
    {
        if (!canRadialMountDynamic(mount, player))
            return false;
        faceTo(player, mount);
        posture.stand(player);
        if (isDesignerAuthorized(player))
            return possessionEnter(player, mount);
        queueClear(player);
        if (!mountCreature(player, mount))
            return false;
        pet_lib.setMountedMovementRate(player, mount);
        setState(player, STATE_RIDING_MOUNT, true);
        return true;
    }

    /** Radial dismount: face mount, release possession + rider slot for dynamic mounts. */
    public static boolean dismountFromRadial(obj_id player, obj_id mount) throws InterruptedException
    {
        if (!isMountedOnDynamicMount(mount, player))
            return false;
        faceTo(player, mount);
        return possessionLeave(player, mount);
    }

    private mount_maker()
    {
    }
}
