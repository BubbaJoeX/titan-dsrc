package script.item;

import script.*;
import script.library.*;

/**
 * RT Camera - Real-Time Camera object for surveillance/monitoring systems.
 * This object captures a view of the world and can be linked to RT Screens.
 *
 * Objvars:
 *   rt_camera.linkedScreen - obj_id of linked screen
 *   rt_camera.owner - obj_id of owner player
 *   rt_camera.isActive - boolean, whether camera is actively streaming
 *   rt_camera.fov - float, field of view in degrees (default 60)
 *   rt_camera.name - string, custom name for the camera
 */
public class rt_camera extends script.base_script
{
    public static final String OBJVAR_ROOT = "rt_camera";
    public static final String OBJVAR_LINKED_SCREEN = OBJVAR_ROOT + ".linkedScreen";
    public static final String OBJVAR_OWNER = OBJVAR_ROOT + ".owner";
    public static final String OBJVAR_IS_ACTIVE = OBJVAR_ROOT + ".isActive";
    public static final String OBJVAR_FOV = OBJVAR_ROOT + ".fov";
    public static final String OBJVAR_NAME = OBJVAR_ROOT + ".name";

    public static final float DEFAULT_FOV = 60.0f;
    public static final float MIN_FOV = 30.0f;
    public static final float MAX_FOV = 120.0f;
    public static final float MAX_LINK_DISTANCE = 1000.0f;

    public static final int MENU_LINK_SCREEN = menu_info_types.SERVER_MENU1;
    public static final int MENU_UNLINK = menu_info_types.SERVER_MENU2;
    public static final int MENU_TOGGLE_ACTIVE = menu_info_types.SERVER_MENU3;
    public static final int MENU_SET_FOV = menu_info_types.SERVER_MENU4;
    public static final int MENU_SET_NAME = menu_info_types.SERVER_MENU5;
    public static final int MENU_PICK_UP = menu_info_types.SERVER_MENU6;
    public static final int MENU_LOCK_TO_PARENT = menu_info_types.SERVER_MENU7;
    public static final int MENU_UNLOCK_FROM_PARENT = menu_info_types.SERVER_MENU8;
    public static final int MENU_ROOT_LINKING = menu_info_types.SERVER_MENU40;
    public static final int MENU_ROOT_SETTINGS = menu_info_types.SERVER_MENU41;
    public static final int MENU_ROOT_MOUNTING = menu_info_types.SERVER_MENU42;
    public static final int MENU_ROOT_INVENTORY = menu_info_types.SERVER_MENU43;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_FOV))
            setObjVar(self, OBJVAR_FOV, DEFAULT_FOV);
        if (!hasObjVar(self, OBJVAR_IS_ACTIVE))
            setObjVar(self, OBJVAR_IS_ACTIVE, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self) throws InterruptedException
    {
        // Clean up linked screen when camera is destroyed
        if (hasObjVar(self, OBJVAR_LINKED_SCREEN))
        {
            obj_id screen = getObjIdObjVar(self, OBJVAR_LINKED_SCREEN);
            if (isIdValid(screen) && exists(screen))
            {
                // Clear the screen's link to this camera
                removeObjVar(screen, "rt_screen.linkedCamera");
                setObjVar(screen, "rt_screen.linkedCamera", "");
                removeObjVar(screen, "rt_camera.resolution");
            }
        }

        // Clear synced objvars to notify clients
        setObjVar(self, OBJVAR_IS_ACTIVE, 0);
        removeObjVar(self, OBJVAR_LINKED_SCREEN);

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id owner = getOwner(self);
        boolean isOwner = isIdValid(owner) && owner.equals(player);
        boolean isGod = isGod(player);

        if (isOwner || isGod)
        {
            boolean hasLinkedScreen = hasObjVar(self, OBJVAR_LINKED_SCREEN);
            boolean isActive = hasObjVar(self, OBJVAR_IS_ACTIVE) && getBooleanObjVar(self, OBJVAR_IS_ACTIVE);
            boolean isLockedToParent = hasObjVar(self, "dynamics.lockParent.parentId");

            int linkRoot = mi.addRootMenu(MENU_ROOT_LINKING, string_id.unlocalized("Link & Stream"));
            int settingsRoot = mi.addRootMenu(MENU_ROOT_SETTINGS, string_id.unlocalized("Settings"));
            int mountRoot = mi.addRootMenu(MENU_ROOT_MOUNTING, string_id.unlocalized("Mounting"));
            int inventoryRoot = mi.addRootMenu(MENU_ROOT_INVENTORY, string_id.unlocalized("Inventory"));

            if (!hasLinkedScreen)
            {
                mi.addSubMenu(linkRoot, MENU_LINK_SCREEN, string_id.unlocalized("Link to Screen"));
            }
            else
            {
                mi.addSubMenu(linkRoot, MENU_UNLINK, string_id.unlocalized("Unlink Screen"));
                mi.addSubMenu(linkRoot, MENU_TOGGLE_ACTIVE, string_id.unlocalized(isActive ? "Deactivate" : "Activate"));
            }

            mi.addSubMenu(settingsRoot, MENU_SET_FOV, string_id.unlocalized("Set Field of View"));
            mi.addSubMenu(settingsRoot, MENU_SET_NAME, string_id.unlocalized("Set Name"));

            // Lock to parent options
            if (!isLockedToParent)
            {
                mi.addSubMenu(mountRoot, MENU_LOCK_TO_PARENT, string_id.unlocalized("Lock to Target"));
            }
            else
            {
                mi.addSubMenu(mountRoot, MENU_UNLOCK_FROM_PARENT, string_id.unlocalized("Unlock from Parent"));
            }

            mi.addSubMenu(inventoryRoot, MENU_PICK_UP, string_id.unlocalized("Pick Up"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        obj_id owner = getOwner(self);
        boolean isOwner = isIdValid(owner) && owner.equals(player);
        boolean isGod = isGod(player);

        if (!isOwner && !isGod)
            return SCRIPT_CONTINUE;

        if (item == MENU_LINK_SCREEN)
        {
            sendSystemMessageTestingOnly(player, "\\#00ccff[RT Camera]: Target an RT Screen and use this camera again to link them.");
            utils.setScriptVar(player, "rt_camera.pendingLink", self);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_UNLINK)
        {
            unlinkScreen(self, player);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_TOGGLE_ACTIVE)
        {
            toggleActive(self, player);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_SET_FOV)
        {
            float currentFov = hasObjVar(self, OBJVAR_FOV) ? getFloatObjVar(self, OBJVAR_FOV) : DEFAULT_FOV;
            String prompt = "Enter field of view (30-120 degrees)\\nCurrent: " + (int)currentFov;
            sui.inputbox(self, player, prompt, sui.OK_CANCEL, "Set Field of View", sui.INPUT_NORMAL, new String[]{String.valueOf((int)currentFov)}, "handleSetFov", null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_SET_NAME)
        {
            String currentName = hasObjVar(self, OBJVAR_NAME) ? getStringObjVar(self, OBJVAR_NAME) : "RT Camera";
            sui.inputbox(self, player, "Enter camera name:", sui.OK_CANCEL, "Set Camera Name", sui.INPUT_NORMAL, new String[]{currentName}, "handleSetName", null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_PICK_UP)
        {
            pickUpCamera(self, player);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_LOCK_TO_PARENT)
        {
            startLockToParent(self, player);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_UNLOCK_FROM_PARENT)
        {
            unlockFromParent(self, player);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleSetFov(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        String input = sui.getInputBoxText(params);

        try
        {
            float fov = Float.parseFloat(input);
            if (fov < MIN_FOV || fov > MAX_FOV)
            {
                sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: FOV must be between " + (int)MIN_FOV + " and " + (int)MAX_FOV + " degrees.");
                return SCRIPT_CONTINUE;
            }

            setObjVar(self, OBJVAR_FOV, fov);
            sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Field of view set to " + (int)fov + " degrees.");

            notifyLinkedScreen(self);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Invalid number.");
        }

        return SCRIPT_CONTINUE;
    }

    public int handleSetName(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        String name = sui.getInputBoxText(params);

        if (name == null || name.trim().isEmpty())
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Name cannot be empty.");
            return SCRIPT_CONTINUE;
        }

        if (name.length() > 64)
            name = name.substring(0, 64);

        setObjVar(self, OBJVAR_NAME, name.trim());
        setName(self, name.trim());
        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Name set to '" + name.trim() + "'.");

        return SCRIPT_CONTINUE;
    }

    private void unlinkScreen(obj_id camera, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(camera, OBJVAR_LINKED_SCREEN))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: No screen linked.");
            return;
        }

        obj_id screen = getObjIdObjVar(camera, OBJVAR_LINKED_SCREEN);

        // Deactivate first
        setObjVar(camera, OBJVAR_IS_ACTIVE, false);

        // Clear linkage on both sides
        removeObjVar(camera, OBJVAR_LINKED_SCREEN);

        if (isIdValid(screen) && exists(screen))
        {
            removeObjVar(screen, "rt_screen.linkedCamera");

            // Notify screen to stop displaying
            dictionary params = new dictionary();
            params.put("camera", camera);
            messageTo(screen, "handleCameraUnlinked", params, 0, false);
        }

        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Screen unlinked.");
    }

    private void toggleActive(obj_id camera, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(camera, OBJVAR_LINKED_SCREEN))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Link a screen first.");
            return;
        }

        boolean isActive = hasObjVar(camera, OBJVAR_IS_ACTIVE) && getBooleanObjVar(camera, OBJVAR_IS_ACTIVE);
        boolean newActive = !isActive;
        setObjVar(camera, OBJVAR_IS_ACTIVE, newActive);

        // This triggers the synced variable update in the server's alter()
        // The server reads rt_camera.isActive objvar and syncs to m_rtCameraActive

        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: " + (newActive ? "Activated" : "Deactivated") + ".");
    }

    private void notifyLinkedScreen(obj_id camera) throws InterruptedException
    {
        if (!hasObjVar(camera, OBJVAR_LINKED_SCREEN))
            return;

        obj_id screen = getObjIdObjVar(camera, OBJVAR_LINKED_SCREEN);
        if (isIdValid(screen) && exists(screen))
        {
            dictionary params = new dictionary();
            params.put("camera", camera);
            messageTo(screen, "handleCameraUpdated", params, 0, false);
        }
    }

    private void pickUpCamera(obj_id camera, obj_id player) throws InterruptedException
    {
        // Unlink first if linked
        if (hasObjVar(camera, OBJVAR_LINKED_SCREEN))
        {
            unlinkScreen(camera, player);
        }

        obj_id inventory = utils.getInventoryContainer(player);
        if (!isIdValid(inventory))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Cannot access inventory.");
            return;
        }

        if (putIn(camera, inventory))
        {
            sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Picked up.");
        }
        else
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Inventory full.");
        }
    }

    private void startLockToParent(obj_id camera, obj_id player) throws InterruptedException
    {
        obj_id target = getIntendedTarget(player);
        if (!isIdValid(target) || target.equals(camera))
        {
            sendSystemMessageTestingOnly(player, "\\#00ccff[RT Camera]: Target an object to lock the camera to it.");
            sendSystemMessageTestingOnly(player, "\\#aaaaaa[RT Camera]: The camera will move with the target object.");
            utils.setScriptVar(player, "rt_camera.pendingLockToParent", camera);
            return;
        }

        // Lock camera to target at current relative position
        location camLoc = getLocation(camera);
        location targetLoc = getLocation(target);

        if (camLoc == null || targetLoc == null)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Failed to get positions.");
            return;
        }

        // Calculate offset
        float dx = camLoc.x - targetLoc.x;
        float dy = camLoc.y - targetLoc.y;
        float dz = camLoc.z - targetLoc.z;

        // Apply lock to parent effect via tangible_dynamics
        tangible_dynamics.applyLockToParentEffect(camera, target, dx, dy, dz, 0.0f, 0.0f, 0.0f, true, -1.0f);

        String targetName = getName(target);
        if (targetName == null || targetName.isEmpty())
            targetName = "object";

        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Locked to '" + targetName + "'. Camera will now move with target.");
    }

    private void unlockFromParent(obj_id camera, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(camera, "dynamics.lockParent.parentId"))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Camera is not locked to any parent.");
            return;
        }

        tangible_dynamics.clearLockToParentEffect(camera);
        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Unlocked from parent. Camera is now stationary.");
    }

    /**
     * Called when a screen attempts to link to this camera.
     */
    public int handleLinkRequest(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id screen = params.getObjId("screen");
        obj_id player = params.getObjId("player");

        if (!isIdValid(screen) || !isIdValid(player))
            return SCRIPT_CONTINUE;

        // Check distance
        float dist = getDistance(self, screen);
        if (dist > MAX_LINK_DISTANCE)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Camera]: Screen is too far away (max " + (int)MAX_LINK_DISTANCE + "m).");
            return SCRIPT_CONTINUE;
        }

        // Check if already linked
        if (hasObjVar(self, OBJVAR_LINKED_SCREEN))
        {
            obj_id existingScreen = getObjIdObjVar(self, OBJVAR_LINKED_SCREEN);
            if (isIdValid(existingScreen) && existingScreen.equals(screen))
            {
                sendSystemMessageTestingOnly(player, "\\#ffaa44[RT Camera]: Already linked to this screen.");
                return SCRIPT_CONTINUE;
            }

            // Unlink existing
            unlinkScreen(self, player);
        }

        // Link
        setObjVar(self, OBJVAR_LINKED_SCREEN, screen);
        setObjVar(screen, "rt_screen.linkedCamera", self);

        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: Linked to screen successfully!");

        return SCRIPT_CONTINUE;
    }

    /**
     * Get camera data for client rendering.
     */
    public static dictionary getCameraData(obj_id camera) throws InterruptedException
    {
        if (!isIdValid(camera) || !exists(camera))
            return null;

        dictionary data = new dictionary();
        data.put("cameraId", camera);
        data.put("fov", hasObjVar(camera, OBJVAR_FOV) ? getFloatObjVar(camera, OBJVAR_FOV) : DEFAULT_FOV);
        data.put("isActive", hasObjVar(camera, OBJVAR_IS_ACTIVE) && getBooleanObjVar(camera, OBJVAR_IS_ACTIVE));

        location loc = getLocation(camera);
        if (loc != null)
        {
            data.put("x", loc.x);
            data.put("y", loc.y);
            data.put("z", loc.z);
            data.put("area", loc.area);
        }

        if (hasObjVar(camera, OBJVAR_LINKED_SCREEN))
            data.put("linkedScreen", getObjIdObjVar(camera, OBJVAR_LINKED_SCREEN));

        return data;
    }
}

