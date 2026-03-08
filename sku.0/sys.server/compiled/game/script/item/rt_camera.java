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

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        obj_id owner = getOwner(self);
        boolean isOwner = isIdValid(owner) && owner.equals(player);
        boolean isGod = isGod(player);

        if (isOwner || isGod)
        {
            boolean hasLinkedScreen = hasObjVar(self, OBJVAR_LINKED_SCREEN);
            boolean isActive = hasObjVar(self, OBJVAR_IS_ACTIVE) && getBooleanObjVar(self, OBJVAR_IS_ACTIVE);

            if (!hasLinkedScreen)
            {
                mi.addRootMenu(MENU_LINK_SCREEN, string_id.unlocalized("Link to Screen"));
            }
            else
            {
                mi.addRootMenu(MENU_UNLINK, string_id.unlocalized("Unlink Screen"));
                mi.addRootMenu(MENU_TOGGLE_ACTIVE, string_id.unlocalized(isActive ? "Deactivate" : "Activate"));
            }

            mi.addRootMenu(MENU_SET_FOV, string_id.unlocalized("Set Field of View"));
            mi.addRootMenu(MENU_SET_NAME, string_id.unlocalized("Set Name"));
            mi.addRootMenu(MENU_PICK_UP, string_id.unlocalized("Pick Up"));
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
            sui.inputbox(self, player, prompt, sui.OK_CANCEL, "Set Field of View", sui.INPUT_NORMAL, String.valueOf((int)currentFov), "handleSetFov", null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_SET_NAME)
        {
            String currentName = hasObjVar(self, OBJVAR_NAME) ? getStringObjVar(self, OBJVAR_NAME) : "RT Camera";
            sui.inputbox(self, player, "Enter camera name:", sui.OK_CANCEL, "Set Camera Name", sui.INPUT_NORMAL, currentName, "handleSetName", null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_PICK_UP)
        {
            pickUpCamera(self, player);
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
        setObjVar(camera, OBJVAR_IS_ACTIVE, !isActive);

        obj_id screen = getObjIdObjVar(camera, OBJVAR_LINKED_SCREEN);
        if (isIdValid(screen) && exists(screen))
        {
            dictionary params = new dictionary();
            params.put("camera", camera);
            params.put("active", !isActive);
            messageTo(screen, "handleCameraActiveChanged", params, 0, false);
        }

        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Camera]: " + (!isActive ? "Activated" : "Deactivated") + ".");
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

