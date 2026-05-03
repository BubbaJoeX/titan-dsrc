package script.library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import script.obj_id;

/**
 * Dynamic mount preset I/O and default objvar layout (pairs with engine mount.dm + hp_dyn replication).
 * <p>
 * <b>Designer flow:</b> edit on a creature via {@link script.creature.creature_dynamic_mount} (seats + {@code hp_dyn}
 * attachments/lights/FX), export preset — done. <b>Live spawn:</b> GM or script spawns the creature template, then calls
 * {@link #applyPresetToSpawn} (alias of {@link #applyPresetFromFile}) to apply {@code mount.dm.*} and {@code hp_dyn.*}
 * from the exported file before mounting logic runs.
 * <p>
 * Authoring listbox/inputbox SUIs use {@code sui.listbox(player, player, ...)} / {@code sui.inputbox(player, player, ...)}
 * so callbacks resolve on {@code player.base.base_player}; the creature oid is stored in
 * {@link script.creature.creature_dynamic_mount#SCRIPTVAR_MM_AUTH_CREATURE}. Terminal radials use object-owned SUIs
 * ({@code terminal.gm_dynamic_hardpoint}).
 */
public class dynamic_mount extends script.base_script
{
    public static final String VAR_DM_ACTIVE = "mount.dm.active";
    public static final String VAR_DM_CAPACITY = "mount.dm.capacity";

    private static final String PRESET_DIR = "/home/swg/swg-main/exe/linux/var/mounting_presets/";

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

    /** Full path for a preset file (.mountpreset appended if missing). */
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

    /**
     * Apply an exported preset to a spawned creature (same as {@link #applyPresetFromFile}). Use after spawning the mount
     * instance so seat layout and {@code hp_dyn} overlays match the designer export.
     */
    public static void applyPresetToSpawn(obj_id creature, String baseName) throws IOException, InterruptedException
    {
        applyPresetFromFile(creature, baseName);
    }

    private dynamic_mount()
    {
    }
}
