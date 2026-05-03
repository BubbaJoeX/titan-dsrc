package script.creature;

import java.io.IOException;

import script.*;
import script.library.dynamic_hardpoint;
import script.library.dynamic_mount;
import script.library.mount_maker;
import script.library.sui;
import script.library.utils;
import script.menu_info_types;

/**
 * Authoring UI for dynamic mounts ({@code mount.dm.*} plus optional {@code hp_dyn.*} saddle overlay).
 * <p>
 * Attach only to a <b>dedicated</b> test creature — this script reserves {@link menu_info_types#SERVER_MENU53}.
 * Attachments / saddles / lights / FX use {@code hp_dyn.*} via the same listbox ({@link script.library.dynamic_hardpoint}).
 * Optional {@code terminal.gm_dynamic_hardpoint} remains for radial editing on a terminal prop. {@code ai.ai} may attach this
 * script when using the GM radial -> Mount maker on an NPC.
 * <p>
 * Full in-game authoring: toggle {@code /decoratorCamera}, optional {@code /mountMakerDrive} (teleport-style fallback),
 * or listbox <b>Ride mount (seat 0)</b> for normal {@code mountCreature} behavior like production mounts; optionally {@code /mountMakerLockNorth 1};
 * click-to-select accessories and use decorator gizmos ({@code TAB}/{@code R}); use listbox Session rows for
 * {@link script.library.mount_maker} safety on the creature.
 * <p>
 * Listbox and inputbox SUIs use the <b>player</b> as {@code sui} owner so callbacks run on {@code player.base.base_player}
 * (same pattern as cloning / revive listboxes). The creature oid is stored on the player via
 * {@link #SCRIPTVAR_MM_AUTH_CREATURE}.
 */
public class creature_dynamic_mount extends script.base_script
{
    private static final int MENU_ROOT = menu_info_types.SERVER_MENU53;

    private static final String OV_SEAT = "creature_dynamic_mount.edit_seat";

    /** Set on the designer player while the Dynamic mount authoring menu is in use (player-owned SUI). */
    public static final String SCRIPTVAR_MM_AUTH_CREATURE = "creature_dynamic_mount.mm_auth_creature";

    /** Callback names — must match {@code player.base.base_player} handler methods. */
    public static final String HANDLER_MM_MAIN = "handleMmDmMainList";
    private static final String HANDLER_MM_CAP = "handleMmDmCapacityInput";
    private static final String HANDLER_MM_SEAT = "handleMmDmSeatIndexInput";
    private static final String HANDLER_MM_POSE = "handleMmDmPoseInput";
    private static final String HANDLER_MM_OX = "handleMmDmOxInput";
    private static final String HANDLER_MM_OY = "handleMmDmOyInput";
    private static final String HANDLER_MM_OZ = "handleMmDmOzInput";
    private static final String HANDLER_MM_EXPORT = "handleMmDmExportNameInput";
    private static final String HANDLER_MM_IMPORT = "handleMmDmImportNameInput";

    private static boolean canEdit(obj_id player) throws InterruptedException
    {
        return isIdValid(player) && (isGod(player) || hasObjVar(player, "test_center"));
    }

    private static int getEditSeat(obj_id player) throws InterruptedException
    {
        if (!hasObjVar(player, OV_SEAT))
            return 0;
        int s = getIntObjVar(player, OV_SEAT);
        if (s < 0)
            return 0;
        if (s > 7)
            return 7;
        return s;
    }

    private static String seatBase(obj_id player) throws InterruptedException
    {
        return "mount.dm.seat." + getEditSeat(player) + ".";
    }

    private static void sendInvalid(obj_id player, String msg) throws InterruptedException
    {
        sendSystemMessage(player, string_id.unlocalized(msg));
    }

    /**
     * Opens the Dynamic mount authoring listbox. Static so {@link script.ai.ai} can invoke it immediately after
     * {@code attachScript} — avoids {@code messageTo} while {@code m_attachingScript} is active (drops handlers).
     * <p>
     * Registers {@link #SCRIPTVAR_MM_AUTH_CREATURE} on the player and opens {@code sui.listbox(player, player, ...)}
     * so Ok/inputbox callbacks resolve on the player ({@code base_player}), not the creature.
     */
    public static void openAuthoringMainMenu(obj_id self, obj_id player) throws InterruptedException
    {
        mount_maker.ensureDesignerSessionForCreature(self, player);
        utils.setScriptVar(player, SCRIPTVAR_MM_AUTH_CREATURE, self);
        int cap = hasObjVar(self, dynamic_mount.VAR_DM_CAPACITY) ? getIntObjVar(self, dynamic_mount.VAR_DM_CAPACITY) : 1;
        cap = Math.min(8, Math.max(1, cap));
        int seat = getEditSeat(player);
        String pose = "normal";
        String pPose = "mount.dm.seat." + seat + ".pose";
        if (hasObjVar(self, pPose))
            pose = getStringObjVar(self, pPose);
        float ox = hasObjVar(self, "mount.dm.seat." + seat + ".ox") ? getFloatObjVar(self, "mount.dm.seat." + seat + ".ox") : 0.f;
        float oy = hasObjVar(self, "mount.dm.seat." + seat + ".oy") ? getFloatObjVar(self, "mount.dm.seat." + seat + ".oy") : 0.f;
        float oz = hasObjVar(self, "mount.dm.seat." + seat + ".oz") ? getFloatObjVar(self, "mount.dm.seat." + seat + ".oz") : 0.f;

        String[] rows = new String[]
        {
            "Set capacity (1-8) [now " + cap + "]",
            "Set seat index to edit (0-" + (cap - 1) + ") [now " + seat + "]",
            "Set pose for seat " + seat + " [now " + pose + "]",
            "Set offset X for seat " + seat + " [now " + ox + "]",
            "Set offset Y for seat " + seat + " [now " + oy + "]",
            "Set offset Z for seat " + seat + " [now " + oz + "]",
            "Attachments / saddle / lights / FX (hp_dyn)...",
            "Export preset (.mountpreset: mount.dm + hp_dyn)",
            "Load preset from var/mounting_presets/...",
            "Finalize (makeDynamicMountable + active)",
            "SERVER: Begin designer session (invuln + ignore combat)",
            "SERVER: End designer session",
            "Clear mount.dm and hp_dyn on this creature",
            "SERVER: Ride mount (seat 0 — same as mountCreature)",
            "SERVER: Dismount",
        };
        sui.listbox(player, player, "Mount maker: rider geometry (mount.dm), then attachments (hp_dyn), export, finalize. Ride mount to snap to seat 0 and drive like a normal rider.", sui.OK_CANCEL, "Mount maker", rows, HANDLER_MM_MAIN, true);
    }

    /** Legacy messageTo target; forwards to {@link #openAuthoringMainMenu}. */
    public int handleGmMountMakerOpen(obj_id self, dictionary params)
        throws InterruptedException {
        obj_id player = params.getObjId("player");
        if (!mount_maker.isDesignerAuthorized(player)) {
            return SCRIPT_CONTINUE;
        }
        openAuthoringMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    private static void showMainMenu(obj_id self, obj_id player) throws InterruptedException
    {
        openAuthoringMainMenu(self, player);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        mi.addRootMenu(MENU_ROOT, string_id.unlocalized("GM: Dynamic mount"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (item != MENU_ROOT)
            return SCRIPT_CONTINUE;
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    /** Invoked from {@code base_player} — {@code self} is the mount creature being authored. */
    public static int mountMakerMainList(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) != sui.BP_OK)
            return SCRIPT_CONTINUE;
        int row = sui.getListboxSelectedRow(params);
        switch (row)
        {
            case 0:
                sui.inputbox(player, player, "Capacity 1-8:", "Dynamic mount",
                        HANDLER_MM_CAP, sui.MAX_INPUT_LENGTH, false,
                        Integer.toString(hasObjVar(self, dynamic_mount.VAR_DM_CAPACITY) ? getIntObjVar(self, dynamic_mount.VAR_DM_CAPACITY) : 1));
                break;
            case 1:
                sui.inputbox(player, player, "Seat index 0-7 (must be < capacity):", "Dynamic mount",
                        HANDLER_MM_SEAT, sui.MAX_INPUT_LENGTH, false, Integer.toString(getEditSeat(player)));
                break;
            case 2:
                sui.inputbox(player, player, "Animation pose name (e.g. rider, normal):", "Dynamic mount",
                        HANDLER_MM_POSE, sui.MAX_INPUT_LENGTH, false,
                        hasObjVar(self, seatBase(player) + "pose") ? getStringObjVar(self, seatBase(player) + "pose") : "normal");
                break;
            case 3:
                sui.inputbox(player, player, "Rider offset X:", "Dynamic mount",
                        HANDLER_MM_OX, sui.MAX_INPUT_LENGTH, false,
                        Float.toString(hasObjVar(self, seatBase(player) + "ox") ? getFloatObjVar(self, seatBase(player) + "ox") : 0.f));
                break;
            case 4:
                sui.inputbox(player, player, "Rider offset Y:", "Dynamic mount",
                        HANDLER_MM_OY, sui.MAX_INPUT_LENGTH, false,
                        Float.toString(hasObjVar(self, seatBase(player) + "oy") ? getFloatObjVar(self, seatBase(player) + "oy") : 0.f));
                break;
            case 5:
                sui.inputbox(player, player, "Rider offset Z:", "Dynamic mount",
                        HANDLER_MM_OZ, sui.MAX_INPUT_LENGTH, false,
                        Float.toString(hasObjVar(self, seatBase(player) + "oz") ? getFloatObjVar(self, seatBase(player) + "oz") : 0.f));
                break;
            case 6:
                dynamic_hardpoint.openHpDynAuthoringListbox(self, player);
                break;
            case 7:
                sui.inputbox(player, player, "Preset base name (no path, saved as name.mountpreset):", "Export",
                        HANDLER_MM_EXPORT, sui.MAX_INPUT_LENGTH, false, "my_mount");
                break;
            case 8:
                sui.inputbox(player, player, "Preset base name to load:", "Import",
                        HANDLER_MM_IMPORT, sui.MAX_INPUT_LENGTH, false, "my_mount");
                break;
            case 9:
                finalizeDynamicMount(self, player);
                showMainMenu(self, player);
                break;
            case 10:
                mount_maker.ensureDesignerSessionForCreature(self, player);
                sendInvalid(player, "mount maker: server session started (invuln + ignore combat).");
                showMainMenu(self, player);
                break;
            case 11:
                mount_maker.endDesignerSession(player);
                sendInvalid(player, "mount maker: server session ended.");
                showMainMenu(self, player);
                break;
            case 12:
                removeObjVar(self, "mount.dm");
                removeObjVar(self, "hp_dyn");
                sendInvalid(player, "Cleared mount.dm and hp_dyn.");
                showMainMenu(self, player);
                break;
            case 13:
                if (mount_maker.possessionEnter(player, self))
                    sendInvalid(player, "Riding this mount (seat 0). Use Dismount or end designer session when done.");
                else
                    sendInvalid(player, "Ride failed: mounts enabled, mountable template with free seat, and not already on another mount.");
                showMainMenu(self, player);
                break;
            case 14:
                mount_maker.possessionLeave(player, self);
                sendInvalid(player, "Dismount / legacy possession clear attempted.");
                showMainMenu(self, player);
                break;
            default:
                return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    private static void finalizeDynamicMount(obj_id self, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(self, dynamic_mount.VAR_DM_ACTIVE))
            setObjVar(self, dynamic_mount.VAR_DM_ACTIVE, 1);
        if (!hasObjVar(self, dynamic_mount.VAR_DM_CAPACITY))
            dynamic_mount.ensureMountDefaults(self, 1);
        if (makeDynamicMountable(self))
            sendInvalid(player, "finalize: makeDynamicMountable succeeded.");
        else
            sendInvalid(player, "finalize: makeDynamicMountable failed.");
    }

    public static int mountMakerCapacityInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null || raw.trim().length() == 0)
            return SCRIPT_CONTINUE;
        int v;
        try
        {
            v = Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException ex)
        {
            sendInvalid(player, "Invalid integer.");
            return SCRIPT_CONTINUE;
        }
        dynamic_mount.ensureMountDefaults(self, v);
        sendInvalid(player, "capacity set.");
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMakerSeatIndexInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            return SCRIPT_CONTINUE;
        int v;
        try
        {
            v = Integer.parseInt(raw.trim());
        }
        catch (NumberFormatException ex)
        {
            sendInvalid(player, "Invalid integer.");
            return SCRIPT_CONTINUE;
        }
        int cap = hasObjVar(self, dynamic_mount.VAR_DM_CAPACITY) ? getIntObjVar(self, dynamic_mount.VAR_DM_CAPACITY) : 1;
        cap = Math.min(8, Math.max(1, cap));
        if (v < 0 || v >= cap)
        {
            sendInvalid(player, "Seat index must be 0 .. capacity-1.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(player, OV_SEAT, v);
        sendInvalid(player, "edit seat = " + v);
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMakerPoseInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            raw = "";
        raw = raw.trim();
        if (raw.length() == 0)
            raw = "normal";
        setObjVar(self, seatBase(player) + "pose", raw);
        sendInvalid(player, "pose updated.");
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMakerOxInput(obj_id self, dictionary params) throws InterruptedException
    {
        return mountMakerFloat(self, params, "ox");
    }

    public static int mountMakerOyInput(obj_id self, dictionary params) throws InterruptedException
    {
        return mountMakerFloat(self, params, "oy");
    }

    public static int mountMakerOzInput(obj_id self, dictionary params) throws InterruptedException
    {
        return mountMakerFloat(self, params, "oz");
    }

    private static int mountMakerFloat(obj_id self, dictionary params, String keySuffix) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null || raw.trim().length() == 0)
            return SCRIPT_CONTINUE;
        float f;
        try
        {
            f = Float.parseFloat(raw.trim());
        }
        catch (NumberFormatException ex)
        {
            sendInvalid(player, "Invalid float.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, seatBase(player) + keySuffix, f);
        sendInvalid(player, keySuffix + " updated.");
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMakerExportNameInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            raw = "";
        raw = raw.trim();
        if (raw.length() == 0)
            raw = "unnamed_export";
        try
        {
            dynamic_mount.exportObjVarsToFile(self, raw);
            sendInvalid(player, "Exported " + dynamic_mount.presetFileForName(raw).getAbsolutePath());
        }
        catch (IOException ex)
        {
            sendInvalid(player, "Export failed: " + ex.getMessage());
        }
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    public static int mountMakerImportNameInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!canEdit(player))
            return SCRIPT_CONTINUE;
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String raw = sui.getInputBoxText(params);
        if (raw == null)
            raw = "";
        raw = raw.trim();
        if (raw.length() == 0)
        {
            sendInvalid(player, "Preset name empty.");
            return SCRIPT_CONTINUE;
        }
        try
        {
            dynamic_mount.applyPresetFromFile(self, raw);
            sendInvalid(player, "Loaded preset " + raw + ". Finalize via menu when ready.");
        }
        catch (IOException ex)
        {
            sendInvalid(player, "Import failed: " + ex.getMessage());
        }
        showMainMenu(self, player);
        return SCRIPT_CONTINUE;
    }

    /** Public no-arg ctor required by {@link script.script_class_loader} (reflective {@code newInstance}). */
    public creature_dynamic_mount()
    {
    }
}
