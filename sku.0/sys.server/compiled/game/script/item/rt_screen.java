package script.item;

import script.*;
import script.library.*;

/**
 * RT Screen - Display surface for RT Camera feeds.
 * Shows real-time view from linked camera.
 *
 * Objvars:
 *   rt_screen.linkedCamera - obj_id of linked camera
 *   rt_screen.owner - obj_id of owner player
 *   rt_screen.isDisplaying - boolean, whether screen is showing feed
 *   rt_screen.resolution - int, resolution setting (256, 512)
 *   rt_screen.name - string, custom name for the screen
 */
public class rt_screen extends script.base_script
{
    public static final String OBJVAR_ROOT = "rt_screen";
    public static final String OBJVAR_LINKED_CAMERA = OBJVAR_ROOT + ".linkedCamera";
    public static final String OBJVAR_OWNER = OBJVAR_ROOT + ".owner";
    public static final String OBJVAR_IS_DISPLAYING = OBJVAR_ROOT + ".isDisplaying";
    public static final String OBJVAR_RESOLUTION = OBJVAR_ROOT + ".resolution";
    public static final String OBJVAR_NAME = OBJVAR_ROOT + ".name";

    public static final int DEFAULT_RESOLUTION = 512;
    public static final int[] VALID_RESOLUTIONS = {256, 512};
    public static final float MAX_VIEW_DISTANCE = 50.0f;

    public static final int MENU_VIEW_FEED = menu_info_types.SERVER_MENU1;
    public static final int MENU_STOP_VIEWING = menu_info_types.SERVER_MENU2;
    public static final int MENU_LINK_CAMERA = menu_info_types.SERVER_MENU3;
    public static final int MENU_UNLINK = menu_info_types.SERVER_MENU4;
    public static final int MENU_SET_RESOLUTION = menu_info_types.SERVER_MENU5;
    public static final int MENU_SET_NAME = menu_info_types.SERVER_MENU6;
    public static final int MENU_PICK_UP = menu_info_types.SERVER_MENU7;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OBJVAR_RESOLUTION))
            setObjVar(self, OBJVAR_RESOLUTION, DEFAULT_RESOLUTION);
        if (!hasObjVar(self, OBJVAR_IS_DISPLAYING))
            setObjVar(self, OBJVAR_IS_DISPLAYING, false);
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

        boolean hasLinkedCamera = hasObjVar(self, OBJVAR_LINKED_CAMERA);

        // View feed - available to anyone within range if camera is active
        if (hasLinkedCamera)
        {
            obj_id camera = getObjIdObjVar(self, OBJVAR_LINKED_CAMERA);
            if (isIdValid(camera) && exists(camera))
            {
                boolean cameraActive = hasObjVar(camera, "rt_camera.isActive") && getBooleanObjVar(camera, "rt_camera.isActive");
                if (cameraActive)
                {
                    mi.addRootMenu(MENU_VIEW_FEED, string_id.unlocalized("View Camera Feed"));
                }
            }
        }

        if (isOwner || isGod)
        {
            if (!hasLinkedCamera)
            {
                mi.addRootMenu(MENU_LINK_CAMERA, string_id.unlocalized("Link to Camera"));
            }
            else
            {
                mi.addRootMenu(MENU_UNLINK, string_id.unlocalized("Unlink Camera"));
            }

            mi.addRootMenu(MENU_SET_RESOLUTION, string_id.unlocalized("Set Resolution"));
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

        if (item == MENU_VIEW_FEED)
        {
            startViewingFeed(self, player);
            return SCRIPT_CONTINUE;
        }

        if (!isOwner && !isGod)
            return SCRIPT_CONTINUE;

        if (item == MENU_LINK_CAMERA)
        {
            // Check if player has a pending camera link
            if (utils.hasScriptVar(player, "rt_camera.pendingLink"))
            {
                obj_id camera = utils.getObjIdScriptVar(player, "rt_camera.pendingLink");
                utils.removeScriptVar(player, "rt_camera.pendingLink");

                if (isIdValid(camera) && exists(camera))
                {
                    dictionary params = new dictionary();
                    params.put("screen", self);
                    params.put("player", player);
                    messageTo(camera, "handleLinkRequest", params, 0, false);
                }
                else
                {
                    sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Camera no longer exists.");
                }
            }
            else
            {
                sendSystemMessageTestingOnly(player, "\\#00ccff[RT Screen]: First use 'Link to Screen' on an RT Camera, then use this screen.");
            }
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_UNLINK)
        {
            unlinkCamera(self, player);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_SET_RESOLUTION)
        {
            String[] options = new String[VALID_RESOLUTIONS.length];
            for (int i = 0; i < VALID_RESOLUTIONS.length; i++)
            {
                options[i] = VALID_RESOLUTIONS[i] + "x" + VALID_RESOLUTIONS[i];
            }
            int pid = sui.listbox(self, player, "Select screen resolution:", sui.OK_CANCEL, "Set Resolution", options, "handleSetResolution", true, false);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_SET_NAME)
        {
            String currentName = hasObjVar(self, OBJVAR_NAME) ? getStringObjVar(self, OBJVAR_NAME) : "RT Screen";
            sui.inputbox(self, player, "Enter screen name:", sui.OK_CANCEL, "Set Screen Name", sui.INPUT_NORMAL, currentName, "handleSetName", null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_PICK_UP)
        {
            pickUpScreen(self, player);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    private void startViewingFeed(obj_id screen, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(screen, OBJVAR_LINKED_CAMERA))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: No camera linked.");
            return;
        }

        obj_id camera = getObjIdObjVar(screen, OBJVAR_LINKED_CAMERA);
        if (!isIdValid(camera) || !exists(camera))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Camera no longer exists.");
            removeObjVar(screen, OBJVAR_LINKED_CAMERA);
            return;
        }

        boolean cameraActive = hasObjVar(camera, "rt_camera.isActive") && getBooleanObjVar(camera, "rt_camera.isActive");
        if (!cameraActive)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Camera is not active.");
            return;
        }

        // Check distance to screen
        float dist = getDistance(player, screen);
        if (dist > MAX_VIEW_DISTANCE)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Move closer to view the feed.");
            return;
        }

        // Send message to client to start rendering camera feed
        int resolution = hasObjVar(screen, OBJVAR_RESOLUTION) ? getIntObjVar(screen, OBJVAR_RESOLUTION) : DEFAULT_RESOLUTION;
        float fov = hasObjVar(camera, "rt_camera.fov") ? getFloatObjVar(camera, "rt_camera.fov") : 60.0f;

        dictionary params = new dictionary();
        params.put("cameraId", camera);
        params.put("screenId", screen);
        params.put("resolution", resolution);
        params.put("fov", fov);

        // Get camera transform data
        location camLoc = getLocation(camera);
        if (camLoc != null)
        {
            params.put("camX", camLoc.x);
            params.put("camY", camLoc.y);
            params.put("camZ", camLoc.z);
        }

        messageTo(player, "handleRtCameraFeed", params, 0, false);

        String cameraName = hasObjVar(camera, "rt_camera.name") ? getStringObjVar(camera, "rt_camera.name") : "RT Camera";
        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Screen]: Viewing feed from '" + cameraName + "'.");
    }

    private void unlinkCamera(obj_id screen, obj_id player) throws InterruptedException
    {
        if (!hasObjVar(screen, OBJVAR_LINKED_CAMERA))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: No camera linked.");
            return;
        }

        obj_id camera = getObjIdObjVar(screen, OBJVAR_LINKED_CAMERA);

        removeObjVar(screen, OBJVAR_LINKED_CAMERA);
        setObjVar(screen, OBJVAR_IS_DISPLAYING, false);

        if (isIdValid(camera) && exists(camera))
        {
            removeObjVar(camera, "rt_camera.linkedScreen");
            setObjVar(camera, "rt_camera.isActive", false);
        }

        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Screen]: Camera unlinked.");
    }

    public int handleSetResolution(obj_id self, dictionary params) throws InterruptedException
    {
        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        int selectedRow = sui.getListboxSelectedRow(params);

        if (selectedRow >= 0 && selectedRow < VALID_RESOLUTIONS.length)
        {
            int resolution = VALID_RESOLUTIONS[selectedRow];
            setObjVar(self, OBJVAR_RESOLUTION, resolution);
            sendSystemMessageTestingOnly(player, "\\#00ff88[RT Screen]: Resolution set to " + resolution + "x" + resolution + ".");
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
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Name cannot be empty.");
            return SCRIPT_CONTINUE;
        }

        if (name.length() > 64)
            name = name.substring(0, 64);

        setObjVar(self, OBJVAR_NAME, name.trim());
        setName(self, name.trim());
        sendSystemMessageTestingOnly(player, "\\#00ff88[RT Screen]: Name set to '" + name.trim() + "'.");

        return SCRIPT_CONTINUE;
    }

    private void pickUpScreen(obj_id screen, obj_id player) throws InterruptedException
    {
        // Unlink first if linked
        if (hasObjVar(screen, OBJVAR_LINKED_CAMERA))
        {
            unlinkCamera(screen, player);
        }

        obj_id inventory = utils.getInventoryContainer(player);
        if (!isIdValid(inventory))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Cannot access inventory.");
            return;
        }

        if (putIn(screen, inventory))
        {
            sendSystemMessageTestingOnly(player, "\\#00ff88[RT Screen]: Picked up.");
        }
        else
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444[RT Screen]: Inventory full.");
        }
    }

    /**
     * Called when linked camera is unlinked.
     */
    public int handleCameraUnlinked(obj_id self, dictionary params) throws InterruptedException
    {
        removeObjVar(self, OBJVAR_LINKED_CAMERA);
        setObjVar(self, OBJVAR_IS_DISPLAYING, false);
        return SCRIPT_CONTINUE;
    }

    /**
     * Called when camera active state changes.
     */
    public int handleCameraActiveChanged(obj_id self, dictionary params) throws InterruptedException
    {
        boolean active = params.getBoolean("active");
        if (!active)
        {
            setObjVar(self, OBJVAR_IS_DISPLAYING, false);
        }
        return SCRIPT_CONTINUE;
    }

    /**
     * Called when camera settings are updated.
     */
    public int handleCameraUpdated(obj_id self, dictionary params) throws InterruptedException
    {
        // Could notify viewing players of changes
        return SCRIPT_CONTINUE;
    }
}

