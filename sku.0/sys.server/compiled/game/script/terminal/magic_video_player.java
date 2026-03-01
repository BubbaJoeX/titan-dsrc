package script.terminal;

import script.dictionary;
import script.library.sui;
import script.library.utils;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;
import script.sui_event_type;

public class magic_video_player extends script.base_script
{
    private static final int MENU_VIDEO_ROOT = menu_info_types.SERVER_MENU30;
    private static final int MENU_VIDEO_MANAGE = menu_info_types.SERVER_MENU31;
    private static final int MENU_VIDEO_INFO = menu_info_types.SERVER_MENU32;

    private static final String SUI_VIDEO_PLAYER = "Script.videoPlayer";

    private static final String OBJVAR_STREAM_URL = "stream.url";
    private static final String OBJVAR_TIMESTAMP = "timestamp";
    private static final String OBJVAR_STREAM_LOOP = "stream.loop";
    private static final String OBJVAR_STREAM_ASPECT = "stream.aspect";
    private static final String OBJVAR_STREAM_START_TIME = "stream.startTime";

    private static final String MASTER_ENTERTAINER_SKILL = "social_entertainer_master";

    private static final String SV_PID = "video_mgmt.pid";

    private static final String COMP = "comp.";
    private static final String LBL_STATUS   = COMP + "statusSection.lblStatus";
    private static final String LBL_URL      = COMP + "statusSection.lblUrl";
    private static final String LBL_LOOP     = COMP + "statusSection.lblLoop";
    private static final String LBL_ASPECT   = COMP + "statusSection.lblAspect";
    private static final String LBL_ELAPSED  = COMP + "statusSection.lblElapsed";
    private static final String LBL_ID       = COMP + "statusSection.lblId";
    private static final String LBL_NAME     = COMP + "statusSection.lblName";
    private static final String LBL_FEEDBACK = COMP + "feedbackSection.lblFeedback";
    private static final String BTN_PLAY     = COMP + "playbackControls.btnPlay";
    private static final String BTN_STOP     = COMP + "playbackControls.btnStop";
    private static final String BTN_LOOP     = COMP + "playbackControls.btnLoop";
    private static final String BTN_ASPECT   = COMP + "playbackControls.btnAspect";

    // ======================================================================
    // Radial menu
    // ======================================================================

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        String currentUrl = "";
        if (hasObjVar(self, OBJVAR_STREAM_URL))
            currentUrl = getStringObjVar(self, OBJVAR_STREAM_URL);

        boolean isPlaying = hasCondition(self, CONDITION_MAGIC_VIDEO_PLAYER) && !currentUrl.isEmpty();

        int root = mi.addRootMenu(MENU_VIDEO_ROOT, string_id.unlocalized("Video Management"));

        if (canModifyVideoPlayer(player))
            mi.addSubMenu(root, MENU_VIDEO_MANAGE, string_id.unlocalized("Open Control Panel"));

        String statusText = isPlaying ? "\\#00FF00 Playing" : (currentUrl.isEmpty() ? "\\#888888 No URL" : "\\#FF4444 Stopped");
        mi.addSubMenu(root, MENU_VIDEO_INFO, string_id.unlocalized("Status: " + statusText));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_VIDEO_MANAGE)
        {
            if (!canModifyVideoPlayer(player))
            {
                sendSystemMessage(player, string_id.unlocalized("You do not have permission to modify this video player."));
                return SCRIPT_CONTINUE;
            }
            showControlPanel(self, player);
        }
        else if (item == MENU_VIDEO_INFO)
        {
            showInfoPanel(self, player);
        }

        return SCRIPT_CONTINUE;
    }

    // ======================================================================
    // Control Panel
    // ======================================================================

    private void showControlPanel(obj_id self, obj_id player) throws InterruptedException
    {
        int pid = createSUIPage(SUI_VIDEO_PLAYER, self, player, "handlePanelClose");

        if (pid < 0)
        {
            sendSystemMessage(player, string_id.unlocalized("Failed to open video player panel."));
            return;
        }

        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, BTN_PLAY, "handlePlay");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, BTN_STOP, "handleStop");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, BTN_LOOP, "handleToggleLoop");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, BTN_ASPECT, "handleToggleAspect");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, COMP + "actionControls.btnSetUrl", "handleSetUrlPrompt");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, COMP + "actionControls.btnSetTimestamp", "handleSetTimestampPrompt");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, COMP + "actionControls.btnSpawnSpeaker", "handleSpawnSpeaker");

        populatePanel(self, pid);

        showSUIPage(pid);
        utils.setScriptVar(player, SV_PID, pid);
    }

    private void populatePanel(obj_id self, int pid) throws InterruptedException
    {
        String url = hasObjVar(self, OBJVAR_STREAM_URL) ? getStringObjVar(self, OBJVAR_STREAM_URL) : "";
        if (url == null) url = "";
        boolean isPlaying = hasCondition(self, CONDITION_MAGIC_VIDEO_PLAYER) && !url.isEmpty();

        String loop = "Off";
        if (hasObjVar(self, OBJVAR_STREAM_LOOP))
        {
            String lv = getStringObjVar(self, OBJVAR_STREAM_LOOP);
            if (lv != null && lv.equals("1")) loop = "On";
        }

        String aspect = "4:3";
        if (hasObjVar(self, OBJVAR_STREAM_ASPECT))
        {
            String av = getStringObjVar(self, OBJVAR_STREAM_ASPECT);
            if (av != null && !av.isEmpty()) aspect = av;
        }

        String elapsed = "-";
        if (isPlaying && hasObjVar(self, OBJVAR_STREAM_START_TIME))
        {
            String st = getStringObjVar(self, OBJVAR_STREAM_START_TIME);
            if (st != null && !st.isEmpty())
            {
                try
                {
                    int startEpoch = Integer.parseInt(st);
                    int now = getCalendarTime();
                    int secs = now - startEpoch;
                    if (secs < 0) secs = 0;
                    int mins = secs / 60;
                    int hours = mins / 60;
                    elapsed = (hours > 0 ? hours + "h " : "") + (mins % 60) + "m " + (secs % 60) + "s";
                }
                catch (NumberFormatException ignored) {}
            }
        }

        String name = getName(self);
        if (name == null || name.isEmpty()) name = "(unnamed)";

        String urlDisplay = url.isEmpty() ? "(none)" : (url.length() > 55 ? url.substring(0, 55) + "..." : url);

        setSUIProperty(pid, LBL_STATUS, sui.PROP_TEXT, "Status: " + (isPlaying ? "\\#00FF00 PLAYING" : "\\#FF4444 STOPPED"));
        setSUIProperty(pid, LBL_URL, sui.PROP_TEXT, "URL: " + urlDisplay);
        setSUIProperty(pid, LBL_LOOP, sui.PROP_TEXT, "Loop: " + loop);
        setSUIProperty(pid, LBL_ASPECT, sui.PROP_TEXT, "Aspect: " + aspect);
        setSUIProperty(pid, LBL_ELAPSED, sui.PROP_TEXT, "Elapsed: " + elapsed);
        setSUIProperty(pid, LBL_ID, sui.PROP_TEXT, "ID: " + self);
        setSUIProperty(pid, LBL_NAME, sui.PROP_TEXT, name);

        setSUIProperty(pid, BTN_LOOP, sui.PROP_TEXT, "Loop: " + loop);
        setSUIProperty(pid, BTN_ASPECT, sui.PROP_TEXT, "Aspect: " + aspect);

        setSUIProperty(pid, "bg.caption.lblTitle", sui.PROP_TEXT, "Video Player - " + name);
    }

    private void refreshPanel(obj_id self, obj_id player, String feedback) throws InterruptedException
    {
        int pid = utils.getIntScriptVar(player, SV_PID);
        if (pid <= 0)
            return;

        populatePanel(self, pid);

        if (feedback != null && !feedback.isEmpty())
            setSUIProperty(pid, LBL_FEEDBACK, sui.PROP_TEXT, feedback);

        flushSUIPage(pid);
    }

    // ======================================================================
    // Button Handlers
    // ======================================================================

    public int handlePlay(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        String url = hasObjVar(self, OBJVAR_STREAM_URL) ? getStringObjVar(self, OBJVAR_STREAM_URL) : "";
        if (url == null || url.isEmpty())
        {
            refreshPanel(self, player, "\\#FF4444 No URL set. Use Set URL first.");
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, OBJVAR_STREAM_START_TIME, String.valueOf(getCalendarTime()));
        setCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
        refreshPanel(self, player, "\\#00FF00 Video playing.");
        return SCRIPT_CONTINUE;
    }

    public int handleStop(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        clearCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
        refreshPanel(self, player, "\\#FFFF00 Video stopped.");
        return SCRIPT_CONTINUE;
    }

    public int handleToggleLoop(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        String currentLoop = "0";
        if (hasObjVar(self, OBJVAR_STREAM_LOOP))
            currentLoop = getStringObjVar(self, OBJVAR_STREAM_LOOP);

        String newLoop = (currentLoop != null && currentLoop.equals("1")) ? "0" : "1";
        setObjVar(self, OBJVAR_STREAM_LOOP, newLoop);
        refreshPanel(self, player, "Loop " + (newLoop.equals("1") ? "\\#00FF00 enabled" : "\\#FFFF00 disabled") + ".");
        return SCRIPT_CONTINUE;
    }

    public int handleToggleAspect(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        String currentAspect = "4:3";
        if (hasObjVar(self, OBJVAR_STREAM_ASPECT))
            currentAspect = getStringObjVar(self, OBJVAR_STREAM_ASPECT);

        String newAspect = (currentAspect != null && currentAspect.equals("16:9")) ? "4:3" : "16:9";
        setObjVar(self, OBJVAR_STREAM_ASPECT, newAspect);
        refreshPanel(self, player, "Aspect set to " + newAspect + ".");
        return SCRIPT_CONTINUE;
    }

    public int handleSetUrlPrompt(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        sui.inputbox(self, player, "Enter the video stream URL:", sui.OK_CANCEL, "Set Video URL", sui.INPUT_NORMAL, null, "handleSetVideoUrl", null);
        return SCRIPT_CONTINUE;
    }

    public int handleSetTimestampPrompt(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        sui.inputbox(self, player, "Enter the timestamp in seconds to seek to:", sui.OK_CANCEL, "Set Timestamp", sui.INPUT_NORMAL, null, "handleSetVideoTimestamp", null);
        return SCRIPT_CONTINUE;
    }

    public int handleSpawnSpeaker(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        obj_id speaker = utils.spawnVideoSpeaker(player, self);
        if (!isIdValid(speaker))
        {
            refreshPanel(self, player, "\\#FF4444 Failed to create speaker.");
            return SCRIPT_CONTINUE;
        }

        refreshPanel(self, player, "\\#00FF00 Speaker spawned.");
        return SCRIPT_CONTINUE;
    }

    public int handlePanelClose(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (isIdValid(player))
            utils.removeScriptVar(player, SV_PID);
        return SCRIPT_CONTINUE;
    }

    // ======================================================================
    // Input Box Callbacks
    // ======================================================================

    public int handleSetVideoUrl(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        String url = sui.getInputBoxText(params);
        if (url == null || url.isEmpty())
        {
            refreshPanel(self, player, "\\#FF4444 Invalid URL.");
            return SCRIPT_CONTINUE;
        }

        url = url.trim();
        setObjVar(self, OBJVAR_STREAM_URL, url);
        if (!hasObjVar(self, OBJVAR_TIMESTAMP))
            setObjVar(self, OBJVAR_TIMESTAMP, "0");
        if (!hasObjVar(self, OBJVAR_STREAM_LOOP))
            setObjVar(self, OBJVAR_STREAM_LOOP, "1");
        if (!hasObjVar(self, OBJVAR_STREAM_ASPECT))
            setObjVar(self, OBJVAR_STREAM_ASPECT, "4:3");
        setObjVar(self, OBJVAR_STREAM_START_TIME, String.valueOf(getCalendarTime()));
        setCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);

        LOG("video_player", "[VideoPlayer] " + getName(player) + " (" + player + ") set stream URL on " + self + " to: " + url);
        refreshPanel(self, player, "\\#00FF00 URL set. Playing.");
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
        if (!isIdValid(player) || !canModifyVideoPlayer(player))
            return SCRIPT_CONTINUE;

        String timestampStr = sui.getInputBoxText(params);
        if (timestampStr == null || timestampStr.isEmpty())
        {
            refreshPanel(self, player, "\\#FF4444 Invalid timestamp.");
            return SCRIPT_CONTINUE;
        }

        try
        {
            int timestamp = Integer.parseInt(timestampStr.trim());
            if (timestamp < 0) timestamp = 0;
            setObjVar(self, OBJVAR_TIMESTAMP, String.valueOf(timestamp));
            refreshPanel(self, player, "Timestamp set to " + timestamp + "s.");
        }
        catch (NumberFormatException e)
        {
            refreshPanel(self, player, "\\#FF4444 Invalid number.");
        }

        return SCRIPT_CONTINUE;
    }

    // ======================================================================
    // Info Panel (read-only, for all users)
    // ======================================================================

    private void showInfoPanel(obj_id self, obj_id player) throws InterruptedException
    {
        String url = hasObjVar(self, OBJVAR_STREAM_URL) ? getStringObjVar(self, OBJVAR_STREAM_URL) : "(none)";
        if (url == null) url = "(none)";
        boolean isPlaying = hasCondition(self, CONDITION_MAGIC_VIDEO_PLAYER) && !url.equals("(none)");

        String name = getName(self);
        if (name == null || name.isEmpty()) name = "(unnamed)";

        String loop = "Off";
        if (hasObjVar(self, OBJVAR_STREAM_LOOP))
        {
            String lv = getStringObjVar(self, OBJVAR_STREAM_LOOP);
            if (lv != null && lv.equals("1")) loop = "On";
        }

        String aspect = "4:3";
        if (hasObjVar(self, OBJVAR_STREAM_ASPECT))
        {
            String av = getStringObjVar(self, OBJVAR_STREAM_ASPECT);
            if (av != null && !av.isEmpty()) aspect = av;
        }

        String info = "\\#00BFFF === VIDEO PLAYER INFO ===\\#FFFFFF\n\n"
            + "Name:    " + name + "\n"
            + "Status:  " + (isPlaying ? "\\#00FF00 Playing" : "\\#FF4444 Stopped") + "\\#FFFFFF\n"
            + "URL:     " + url + "\n"
            + "Loop:    " + loop + "\n"
            + "Aspect:  " + aspect + "\n"
            + "ID:      " + self;

        sui.msgbox(self, player, info, sui.OK_ONLY, "Video Player Info", sui.MSG_NORMAL, "noHandler");
    }

    public int noHandler(obj_id self, dictionary params) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    // ======================================================================
    // Permission check
    // ======================================================================

    private boolean canModifyVideoPlayer(obj_id player) throws InterruptedException
    {
        if (isGod(player))
            return true;

        if (hasSkill(player, MASTER_ENTERTAINER_SKILL))
            return true;

        return false;
    }
}
