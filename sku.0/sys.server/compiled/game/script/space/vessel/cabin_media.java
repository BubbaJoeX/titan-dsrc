package script.space.vessel;

import script.*;
import script.library.*;

/**
 * The cabin screen of a Titan vessel (the interior buildout puts it on the frn_titan_veh_wall_monitor): in-flight
 * entertainment to pass the trip. Channels are the rows of datatables/space/titan_cabin_media.tab (name, url, aspect;
 * written by SwgMayaMCP examples/make_cabin_media.py: YouTube links the client resolves with yt-dlp, then local files);
 * the screen tunes in when the vessel is built and anyone aboard can change channel, put on a video link of their own
 * or switch it off from the radial menu. Playing is the magic video player condition with the stream.* objvars, so
 * every passenger watches the same picture; the cabin's ceiling speakers (frn_titan_veh_ceiling_speaker, linked here)
 * carry the sound.
 */
public class cabin_media extends script.base_script
{
    public cabin_media()
    {
    }

    public static final String TABLE = "datatables/space/titan_cabin_media.iff";
    public static final String OV_CHANNEL = "cabin_media.channel";
    public static final String OV_OFF = "cabin_media.off";
    public static final String OV_LINK = "cabin_media.link";          // a video link a passenger put on
    public static final String SPEAKER_TEMPLATE = "object/tangible/furniture/titan/frn_titan_veh_ceiling_speaker.iff";

    private static final int MENU_ROOT = menu_info_types.SERVER_MENU33;
    private static final int MENU_NEXT = menu_info_types.SERVER_MENU34;
    private static final int MENU_PREVIOUS = menu_info_types.SERVER_MENU35;
    private static final int MENU_GUIDE = menu_info_types.SERVER_MENU36;
    private static final int MENU_POWER = menu_info_types.SERVER_MENU37;
    private static final int MENU_LINK = menu_info_types.SERVER_MENU38;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "cabinMediaStart", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "cabinMediaStart", null, 2.0f, false);
        return SCRIPT_CONTINUE;
    }

    public int cabinMediaStart(obj_id self, dictionary params) throws InterruptedException
    {
        linkSpeakers(self);
        if (hasObjVar(self, OV_OFF))
            return SCRIPT_CONTINUE;
        if (hasObjVar(self, OV_LINK))
            play(self, getStringObjVar(self, OV_LINK), "16:9", false);
        else
            tune(self, hasObjVar(self, OV_CHANNEL) ? getIntObjVar(self, OV_CHANNEL) : 0, false);
        return SCRIPT_CONTINUE;
    }

    /** The cabin's ceiling speakers carry this screen's sound (a screen plays muted until a speaker is linked). */
    private void linkSpeakers(obj_id self) throws InterruptedException
    {
        obj_id ship = getTopMostContainer(self);
        obj_id[] cells = isIdValid(ship) ? getContents(ship) : null;
        if (cells == null)
            return;
        for (obj_id cell : cells)
        {
            obj_id[] contents = getContents(cell);
            if (contents == null)
                continue;
            for (obj_id o : contents)
                if (SPEAKER_TEMPLATE.equals(getTemplateName(o)))
                    setObjVar(o, "video_emitter.parent_id", self.toString());
        }
    }

    private int channelCount() throws InterruptedException
    {
        return dataTableOpen(TABLE) ? dataTableGetNumRows(TABLE) : 0;
    }

    private String channelName(int channel) throws InterruptedException
    {
        return dataTableGetString(TABLE, channel, "name");
    }

    /** Play channel `channel` (wraps round the guide) from the start; `keepTime` keeps the running clock. */
    private boolean tune(obj_id self, int channel, boolean keepTime) throws InterruptedException
    {
        int n = channelCount();
        if (n <= 0)
            return false;
        channel = ((channel % n) + n) % n;
        String url = dataTableGetString(TABLE, channel, "url");
        String aspect = dataTableGetString(TABLE, channel, "aspect");
        if (url == null || url.length() == 0)
            return false;
        setObjVar(self, OV_CHANNEL, channel);
        removeObjVar(self, OV_LINK);
        play(self, url, aspect, keepTime);
        return true;
    }

    /** Put `url` on the screen, looping, in step for everyone aboard (the clients seek to the shared start time). */
    private void play(obj_id self, String url, String aspect, boolean keepTime) throws InterruptedException
    {
        setObjVar(self, "stream.url", url);
        setObjVar(self, "stream.loop", "1");
        setObjVar(self, "stream.aspect", aspect == null || aspect.length() == 0 ? "16:9" : aspect);
        setObjVar(self, "timestamp", "0");
        if (!keepTime || !hasObjVar(self, "stream.startTime"))
            setObjVar(self, "stream.startTime", String.valueOf(getCalendarTime()));
        removeObjVar(self, OV_OFF);
        setCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
    }

    private void switchOff(obj_id self) throws InterruptedException
    {
        setObjVar(self, OV_OFF, 1);
        clearCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
    }

    private boolean isOn(obj_id self) throws InterruptedException
    {
        return hasCondition(self, CONDITION_MAGIC_VIDEO_PLAYER);
    }

    /** Anyone aboard the same vessel (or staff) may work the screen. */
    private boolean canUse(obj_id self, obj_id player) throws InterruptedException
    {
        if (isGod(player))
            return true;
        obj_id ship = getTopMostContainer(self);
        return isIdValid(ship) && ship == getTopMostContainer(player);
    }

    private void tell(obj_id self, obj_id player) throws InterruptedException
    {
        if (!isOn(self))
            sendSystemMessage(player, string_id.unlocalized("The cabin screen is off."));
        else if (hasObjVar(self, OV_LINK))
            sendSystemMessage(player, string_id.unlocalized("Now showing a passenger's link: " + getStringObjVar(self, OV_LINK)));
        else
            sendSystemMessage(player, string_id.unlocalized("Now showing: " + channelName(getIntObjVar(self, OV_CHANNEL))
                    + " (channel " + (getIntObjVar(self, OV_CHANNEL) + 1) + " of " + channelCount() + ")"));
    }

    // ------------------------------------------------------------------
    // radial menu
    // ------------------------------------------------------------------

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!canUse(self, player) || channelCount() <= 0)
            return SCRIPT_CONTINUE;
        int root = mi.addRootMenu(MENU_ROOT, string_id.unlocalized("In-Flight Entertainment"));
        mi.addSubMenu(root, MENU_NEXT, string_id.unlocalized("Next Channel"));
        mi.addSubMenu(root, MENU_PREVIOUS, string_id.unlocalized("Previous Channel"));
        mi.addSubMenu(root, MENU_GUIDE, string_id.unlocalized("Channel Guide"));
        mi.addSubMenu(root, MENU_LINK, string_id.unlocalized("Play a Video Link..."));
        mi.addSubMenu(root, MENU_POWER, string_id.unlocalized(isOn(self) ? "Turn Off" : "Turn On"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!canUse(self, player))
            return SCRIPT_CONTINUE;
        int channel = hasObjVar(self, OV_CHANNEL) ? getIntObjVar(self, OV_CHANNEL) : 0;
        if (item == MENU_ROOT)
            tell(self, player);
        else if (item == MENU_NEXT)
        {
            tune(self, channel + 1, false);
            tell(self, player);
        }
        else if (item == MENU_PREVIOUS)
        {
            tune(self, channel - 1, false);
            tell(self, player);
        }
        else if (item == MENU_GUIDE)
            showGuide(self, player);
        else if (item == MENU_LINK)
            sui.inputbox(self, player, "Paste a video link (YouTube, Vimeo, Twitch, or a direct .mp4 / .m3u8 address). "
                    + "Everyone aboard sees it.", "Play a Video Link", "handleCabinLink", 512, false, "");
        else if (item == MENU_POWER)
        {
            if (isOn(self))
                switchOff(self);
            else
                tune(self, channel, false);
            tell(self, player);
        }
        return SCRIPT_CONTINUE;
    }

    private void showGuide(obj_id self, obj_id player) throws InterruptedException
    {
        int n = channelCount();
        String[] rows = new String[n];
        int current = hasObjVar(self, OV_CHANNEL) && isOn(self) ? getIntObjVar(self, OV_CHANNEL) : -1;
        for (int i = 0; i < n; ++i)
            rows[i] = (i + 1) + ". " + channelName(i) + (i == current ? "  (now showing)" : "");
        sui.listbox(self, player, "Choose a channel for the cabin screen.", sui.OK_CANCEL, "In-Flight Entertainment", rows,
                "handleChannelGuide", true, false);
    }

    public int handleCabinLink(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canUse(self, player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        String url = sui.getInputBoxText(params);
        url = url == null ? "" : url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://"))
        {
            sendSystemMessage(player, string_id.unlocalized("That isn't a video link (it should start with http:// or https://)."));
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, OV_LINK, url);
        play(self, url, "16:9", false);
        tell(self, player);
        LOG("vessel", "cabin_media: " + getName(player) + " (" + player + ") put " + url + " on " + self);
        return SCRIPT_CONTINUE;
    }

    public int handleChannelGuide(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !canUse(self, player) || sui.getIntButtonPressed(params) == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;
        int row = sui.getListboxSelectedRow(params);
        if (row < 0)
            return SCRIPT_CONTINUE;
        tune(self, row, false);
        tell(self, player);
        return SCRIPT_CONTINUE;
    }
}
