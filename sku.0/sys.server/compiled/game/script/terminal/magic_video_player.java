package script.terminal;

import script.dictionary;
import script.library.sui;
import script.library.utils;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class magic_video_player extends script.base_script
{
    private static final int MENU_VIDEO_ROOT = menu_info_types.SERVER_MENU30;
    private static final int MENU_VIDEO_SET_URL = menu_info_types.SERVER_MENU31;
    private static final int MENU_VIDEO_SET_TIMESTAMP = menu_info_types.SERVER_MENU32;
    private static final int MENU_VIDEO_STOP = menu_info_types.SERVER_MENU33;
    private static final int MENU_VIDEO_INFO = menu_info_types.SERVER_MENU34;

    private static final String OBJVAR_STREAM_URL = "stream.url";
    private static final String OBJVAR_TIMESTAMP = "timestamp";

    private static final String MASTER_ENTERTAINER_SKILL = "social_entertainer_master";

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int root = mi.addRootMenu(MENU_VIDEO_ROOT, string_id.unlocalized("Video Management"));

        String currentUrl = "";
        if (hasObjVar(self, OBJVAR_STREAM_URL))
            currentUrl = getStringObjVar(self, OBJVAR_STREAM_URL);

        String currentTimestamp = "0";
        if (hasObjVar(self, OBJVAR_TIMESTAMP))
            currentTimestamp = getStringObjVar(self, OBJVAR_TIMESTAMP);

        boolean canModify = canModifyVideoPlayer(player);

        if (canModify)
        {
            mi.addSubMenu(root, MENU_VIDEO_SET_URL, string_id.unlocalized("Set URL"));
            mi.addSubMenu(root, MENU_VIDEO_SET_TIMESTAMP, string_id.unlocalized("Set Timestamp: " + currentTimestamp + "s"));
            mi.addSubMenu(root, MENU_VIDEO_STOP, string_id.unlocalized("Stop Video"));
        }

        String urlDisplay = currentUrl.length() > 40 ? currentUrl.substring(0, 40) + "..." : currentUrl;
        mi.addSubMenu(root, MENU_VIDEO_INFO, string_id.unlocalized("Now Playing: " + (urlDisplay.isEmpty() ? "(none)" : urlDisplay)));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!canModifyVideoPlayer(player))
        {
            sendSystemMessage(player, string_id.unlocalized("You do not have permission to modify this video player."));
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_VIDEO_SET_URL)
        {
            int pid = sui.inputbox(self, player, "Enter the video stream URL:", sui.OK_CANCEL, "Set Video URL", sui.INPUT_NORMAL, null, "handleSetVideoUrl", null);
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VIDEO_SET_TIMESTAMP)
        {
            int pid = sui.inputbox(self, player, "Enter the timestamp in seconds to seek to:", sui.OK_CANCEL, "Set Timestamp", sui.INPUT_NORMAL, null, "handleSetVideoTimestamp", null);
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VIDEO_STOP)
        {
            removeObjVar(self, OBJVAR_STREAM_URL);
            removeObjVar(self, OBJVAR_TIMESTAMP);
            sendSystemMessage(player, string_id.unlocalized("Video stopped."));
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleSetVideoUrl(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
            return SCRIPT_CONTINUE;

        if (!canModifyVideoPlayer(player))
        {
            sendSystemMessage(player, string_id.unlocalized("You do not have permission to modify this video player."));
            return SCRIPT_CONTINUE;
        }

        String url = sui.getInputBoxText(params);
        if (url == null || url.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid URL."));
            return SCRIPT_CONTINUE;
        }

        url = url.trim();
        setObjVar(self, OBJVAR_STREAM_URL, url);
        sendSystemMessage(player, string_id.unlocalized("Video URL set to: " + url));
        LOG("video_player", "[VideoPlayer] " + getName(player) + " (" + player + ") set stream URL on " + self + " to: " + url);

        return SCRIPT_CONTINUE;
    }

    public int handleSetVideoTimestamp(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player))
            return SCRIPT_CONTINUE;

        if (!canModifyVideoPlayer(player))
        {
            sendSystemMessage(player, string_id.unlocalized("You do not have permission to modify this video player."));
            return SCRIPT_CONTINUE;
        }

        String timestampStr = sui.getInputBoxText(params);
        if (timestampStr == null || timestampStr.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid timestamp."));
            return SCRIPT_CONTINUE;
        }

        try
        {
            int timestamp = Integer.parseInt(timestampStr.trim());
            if (timestamp < 0)
                timestamp = 0;
            setObjVar(self, OBJVAR_TIMESTAMP, String.valueOf(timestamp));
            sendSystemMessage(player, string_id.unlocalized("Timestamp set to: " + timestamp + " seconds."));
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid timestamp. Please enter a number in seconds."));
        }

        return SCRIPT_CONTINUE;
    }

    private boolean canModifyVideoPlayer(obj_id player) throws InterruptedException
    {
        if (isGod(player))
            return true;

        if (hasSkill(player, MASTER_ENTERTAINER_SKILL))
            return true;

        return false;
    }
}
