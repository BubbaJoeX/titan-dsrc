package script.player;

import script.dictionary;
import script.library.sui;
import script.location;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class player_video_player extends script.base_script
{
    private static final String REQUIRED_SKILL = "class_entertainer_phase4_master";

    private static final String SPEAKER_TEMPLATE = "object/tangible/loot/misc/speaker_s01.iff";

    private static final String OBJVAR_STREAM_URL = "stream.url";
    private static final String OBJVAR_TIMESTAMP = "timestamp";
    private static final String OBJVAR_STREAM_LOOP = "stream.loop";
    private static final String OBJVAR_STREAM_ASPECT = "stream.aspect";
    private static final String OBJVAR_STREAM_START_TIME = "stream.startTime";
    private static final String OBJVAR_EMITTER_PARENT_ID = "video_emitter.parent_id";

    private static final int MENU_VP_ROOT = menu_info_types.SERVER_MENU40;
    private static final int MENU_VP_SET_URL = menu_info_types.SERVER_MENU41;
    private static final int MENU_VP_SET_TIMESTAMP = menu_info_types.SERVER_MENU42;
    private static final int MENU_VP_SPAWN_SPEAKER = menu_info_types.SERVER_MENU43;
    private static final int MENU_VP_TOGGLE_LOOP = menu_info_types.SERVER_MENU44;
    private static final int MENU_VP_TOGGLE_ASPECT = menu_info_types.SERVER_MENU45;
    private static final int MENU_VP_STOP = menu_info_types.SERVER_MENU46;
    private static final int MENU_VP_PLAY = menu_info_types.SERVER_MENU47;
    private static final int MENU_VP_INFO = menu_info_types.SERVER_MENU48;

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasSkill(player, REQUIRED_SKILL))
            return SCRIPT_CONTINUE;

        int root = mi.addRootMenu(MENU_VP_ROOT, string_id.unlocalized("Video Player"));

        String currentUrl = "";
        if (hasObjVar(self, OBJVAR_STREAM_URL))
            currentUrl = getStringObjVar(self, OBJVAR_STREAM_URL);

        String currentTimestamp = "0";
        if (hasObjVar(self, OBJVAR_TIMESTAMP))
            currentTimestamp = getStringObjVar(self, OBJVAR_TIMESTAMP);

        mi.addSubMenu(root, MENU_VP_SET_URL, string_id.unlocalized("Set URL"));
        mi.addSubMenu(root, MENU_VP_SET_TIMESTAMP, string_id.unlocalized("Set Timestamp: " + currentTimestamp + "s"));
        mi.addSubMenu(root, MENU_VP_SPAWN_SPEAKER, string_id.unlocalized("Spawn Speaker"));

        String loopState = "OFF";
        if (hasObjVar(self, OBJVAR_STREAM_LOOP))
        {
            String loopVal = getStringObjVar(self, OBJVAR_STREAM_LOOP);
            if (loopVal != null && loopVal.equals("1"))
                loopState = "ON";
        }
        mi.addSubMenu(root, MENU_VP_TOGGLE_LOOP, string_id.unlocalized("Loop: " + loopState));

        String aspectState = "4:3";
        if (hasObjVar(self, OBJVAR_STREAM_ASPECT))
        {
            String aspectVal = getStringObjVar(self, OBJVAR_STREAM_ASPECT);
            if (aspectVal != null && aspectVal.equals("16:9"))
                aspectState = "16:9";
        }
        mi.addSubMenu(root, MENU_VP_TOGGLE_ASPECT, string_id.unlocalized("Aspect: " + aspectState));

        boolean isPlaying = hasCondition(self, CONDITION_MAGIC_VIDEO_PLAYER) && !currentUrl.isEmpty();

        if (isPlaying)
            mi.addSubMenu(root, MENU_VP_STOP, string_id.unlocalized("Stop Video"));
        else if (!currentUrl.isEmpty())
            mi.addSubMenu(root, MENU_VP_PLAY, string_id.unlocalized("Play Video"));

        String urlDisplay = currentUrl.length() > 40 ? currentUrl.substring(0, 40) + "..." : currentUrl;
        mi.addSubMenu(root, MENU_VP_INFO, string_id.unlocalized("Now Playing: " + (isPlaying ? (urlDisplay.isEmpty() ? "(none)" : urlDisplay) : "(stopped)")));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!hasSkill(player, REQUIRED_SKILL))
        {
            sendSystemMessage(player, string_id.unlocalized("You must be a Master Entertainer to use this."));
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_VP_SET_URL)
        {
            int pid = sui.inputbox(self, player, "Enter the video stream URL:", sui.OK_CANCEL, "Set Video URL", sui.INPUT_NORMAL, null, "handleSetVideoUrl", null);
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VP_SET_TIMESTAMP)
        {
            int pid = sui.inputbox(self, player, "Enter the timestamp in seconds to seek to:", sui.OK_CANCEL, "Set Timestamp", sui.INPUT_NORMAL, null, "handleSetVideoTimestamp", null);
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VP_SPAWN_SPEAKER)
        {
            location loc = getLocation(player);
            obj_id speaker = createObject(SPEAKER_TEMPLATE, loc);
            if (!isIdValid(speaker))
            {
                sendSystemMessage(player, string_id.unlocalized("Failed to create speaker object."));
                return SCRIPT_CONTINUE;
            }
            setObjVar(speaker, OBJVAR_EMITTER_PARENT_ID, self.toString());
            setName(speaker, "Video Speaker");
            sendSystemMessage(player, string_id.unlocalized("Speaker spawned and linked to this video player."));
            LOG("video_player", "[PlayerVideoPlayer] " + getName(player) + " spawned speaker " + speaker + " linked to " + self);
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VP_TOGGLE_LOOP)
        {
            String currentLoop = "0";
            if (hasObjVar(self, OBJVAR_STREAM_LOOP))
                currentLoop = getStringObjVar(self, OBJVAR_STREAM_LOOP);

            String newLoop = (currentLoop != null && currentLoop.equals("1")) ? "0" : "1";
            setObjVar(self, OBJVAR_STREAM_LOOP, newLoop);
            sendSystemMessage(player, string_id.unlocalized("Loop " + (newLoop.equals("1") ? "enabled" : "disabled") + "."));
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VP_TOGGLE_ASPECT)
        {
            String currentAspect = "4:3";
            if (hasObjVar(self, OBJVAR_STREAM_ASPECT))
                currentAspect = getStringObjVar(self, OBJVAR_STREAM_ASPECT);

            String newAspect = (currentAspect != null && currentAspect.equals("16:9")) ? "4:3" : "16:9";
            setObjVar(self, OBJVAR_STREAM_ASPECT, newAspect);
            sendSystemMessage(player, string_id.unlocalized("Aspect ratio set to " + newAspect + "."));
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VP_STOP)
        {
            clearCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
            sendSystemMessage(player, string_id.unlocalized("Video stopped."));
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_VP_PLAY)
        {
            if (hasObjVar(self, OBJVAR_STREAM_URL))
            {
                setObjVar(self, OBJVAR_STREAM_START_TIME, String.valueOf(getCalendarTime()));
                setCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
                sendSystemMessage(player, string_id.unlocalized("Video playing."));
            }
            else
            {
                sendSystemMessage(player, string_id.unlocalized("No URL set. Use Set URL first."));
            }
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

        if (!hasSkill(player, REQUIRED_SKILL))
        {
            sendSystemMessage(player, string_id.unlocalized("You must be a Master Entertainer to use this."));
            return SCRIPT_CONTINUE;
        }

        String url = sui.getInputBoxText(params);
        if (url == null || url.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid URL."));
            return SCRIPT_CONTINUE;
        }

        url = url.trim();
        setCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
        setObjVar(self, OBJVAR_STREAM_URL, url);
        if (!hasObjVar(self, OBJVAR_TIMESTAMP))
            setObjVar(self, OBJVAR_TIMESTAMP, "0");
        if (!hasObjVar(self, OBJVAR_STREAM_LOOP))
            setObjVar(self, OBJVAR_STREAM_LOOP, "1");
        if (!hasObjVar(self, OBJVAR_STREAM_ASPECT))
            setObjVar(self, OBJVAR_STREAM_ASPECT, "4:3");
        setObjVar(self, OBJVAR_STREAM_START_TIME, String.valueOf(getCalendarTime()));

        sendSystemMessage(player, string_id.unlocalized("Video URL set to: " + url));
        LOG("video_player", "[PlayerVideoPlayer] " + getName(player) + " (" + player + ") set stream URL on " + self + " to: " + url);

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

        if (!hasSkill(player, REQUIRED_SKILL))
        {
            sendSystemMessage(player, string_id.unlocalized("You must be a Master Entertainer to use this."));
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
}
