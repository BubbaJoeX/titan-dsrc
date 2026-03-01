package script.terminal;

import script.dictionary;
import script.library.sui;
import script.location;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class video_player_manager extends script.base_script
{
    private static final int MENU_MANAGER_ROOT = menu_info_types.SERVER_MENU50;
    private static final int MENU_MANAGER_LIST = menu_info_types.SERVER_MENU51;
    private static final int MENU_MANAGER_LIST_SPEAKERS = menu_info_types.SERVER_MENU52;

    private static final String OBJVAR_STREAM_URL = "stream.url";
    private static final String OBJVAR_STREAM_LOOP = "stream.loop";
    private static final String OBJVAR_STREAM_ASPECT = "stream.aspect";
    private static final String OBJVAR_STREAM_START_TIME = "stream.startTime";
    private static final String OBJVAR_EMITTER_PARENT_ID = "video_emitter.parent_id";

    private static final float SCAN_RANGE = 512.0f;

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isGod(player))
            return SCRIPT_CONTINUE;

        int root = mi.addRootMenu(MENU_MANAGER_ROOT, string_id.unlocalized("Video Manager"));
        mi.addSubMenu(root, MENU_MANAGER_LIST, string_id.unlocalized("List Video Players (" + (int)SCAN_RANGE + "m)"));
        mi.addSubMenu(root, MENU_MANAGER_LIST_SPEAKERS, string_id.unlocalized("List Speakers (" + (int)SCAN_RANGE + "m)"));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isGod(player))
            return SCRIPT_CONTINUE;

        if (item == MENU_MANAGER_LIST)
        {
            showVideoPlayerList(self, player);
            return SCRIPT_CONTINUE;
        }
        else if (item == MENU_MANAGER_LIST_SPEAKERS)
        {
            showSpeakerList(self, player);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    private void showVideoPlayerList(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id[] nearby = getObjectsInRange(player, SCAN_RANGE);
        if (nearby == null || nearby.length == 0)
        {
            sendSystemMessage(player, string_id.unlocalized("No objects found in range."));
            return;
        }

        java.util.Vector entries = new java.util.Vector();
        java.util.Vector ids = new java.util.Vector();

        for (int i = 0; i < nearby.length; i++)
        {
            if (!isIdValid(nearby[i]))
                continue;
            if (!hasCondition(nearby[i], CONDITION_MAGIC_VIDEO_PLAYER))
                continue;

            String url = "";
            if (hasObjVar(nearby[i], OBJVAR_STREAM_URL))
                url = getStringObjVar(nearby[i], OBJVAR_STREAM_URL);

            String loop = "off";
            if (hasObjVar(nearby[i], OBJVAR_STREAM_LOOP))
            {
                String lv = getStringObjVar(nearby[i], OBJVAR_STREAM_LOOP);
                if (lv != null && lv.equals("1"))
                    loop = "on";
            }

            String aspect = "4:3";
            if (hasObjVar(nearby[i], OBJVAR_STREAM_ASPECT))
            {
                String av = getStringObjVar(nearby[i], OBJVAR_STREAM_ASPECT);
                if (av != null)
                    aspect = av;
            }

            String name = getName(nearby[i]);
            if (name == null || name.isEmpty())
                name = "(unnamed)";

            location loc = getLocation(nearby[i]);
            float dist = getDistance(player, nearby[i]);

            String urlShort = url.length() > 35 ? url.substring(0, 35) + "..." : url;
            String entry = nearby[i] + " | " + name + " | " + urlShort + " | loop:" + loop + " | " + aspect + " | " + (int)dist + "m";
            entries.add(entry);
            ids.add(nearby[i].toString());
        }

        if (entries.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("No active video players found within " + (int)SCAN_RANGE + "m."));
            return;
        }

        String[] entryArray = new String[entries.size()];
        entries.toArray(entryArray);

        String[] idArray = new String[ids.size()];
        ids.toArray(idArray);

        setObjVar(self, "video_manager.ids", idArray);

        sui.listbox(self, player, "Active Video Players (" + entries.size() + " found)", sui.OK_CANCEL, "Video Player Manager", entryArray, "handleVideoPlayerSelected", true);
    }

    private void showSpeakerList(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id[] nearby = getObjectsInRange(player, SCAN_RANGE);
        if (nearby == null || nearby.length == 0)
        {
            sendSystemMessage(player, string_id.unlocalized("No objects found in range."));
            return;
        }

        java.util.Vector entries = new java.util.Vector();
        java.util.Vector ids = new java.util.Vector();

        for (int i = 0; i < nearby.length; i++)
        {
            if (!isIdValid(nearby[i]))
                continue;
            if (!hasObjVar(nearby[i], OBJVAR_EMITTER_PARENT_ID))
                continue;

            String parentId = getStringObjVar(nearby[i], OBJVAR_EMITTER_PARENT_ID);
            String name = getName(nearby[i]);
            if (name == null || name.isEmpty())
                name = "(unnamed)";

            float dist = getDistance(player, nearby[i]);
            String entry = nearby[i] + " | " + name + " | linked:" + parentId + " | " + (int)dist + "m";
            entries.add(entry);
            ids.add(nearby[i].toString());
        }

        if (entries.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("No speakers found within " + (int)SCAN_RANGE + "m."));
            return;
        }

        String[] entryArray = new String[entries.size()];
        entries.toArray(entryArray);

        String[] idArray = new String[ids.size()];
        ids.toArray(idArray);

        setObjVar(self, "video_manager.speaker_ids", idArray);

        sui.listbox(self, player, "Speakers (" + entries.size() + " found)", sui.OK_CANCEL, "Speaker Manager", entryArray, "handleSpeakerSelected", true);
    }

    public int handleVideoPlayerSelected(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !isGod(player))
            return SCRIPT_CONTINUE;

        int row = sui.getListboxSelectedRow(params);
        if (row < 0)
            return SCRIPT_CONTINUE;

        if (!hasObjVar(self, "video_manager.ids"))
            return SCRIPT_CONTINUE;

        String[] ids = getStringArrayObjVar(self, "video_manager.ids");
        if (ids == null || row >= ids.length)
            return SCRIPT_CONTINUE;

        obj_id target = obj_id.getObjId(ids[row]);
        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid object."));
            return SCRIPT_CONTINUE;
        }

        String url = "";
        if (hasObjVar(target, OBJVAR_STREAM_URL))
            url = getStringObjVar(target, OBJVAR_STREAM_URL);

        String startTime = "N/A";
        if (hasObjVar(target, OBJVAR_STREAM_START_TIME))
            startTime = getStringObjVar(target, OBJVAR_STREAM_START_TIME);

        boolean isPlaying = hasCondition(target, CONDITION_MAGIC_VIDEO_PLAYER);

        String info = "Object: " + target + "\n"
            + "Name: " + getName(target) + "\n"
            + "URL: " + url + "\n"
            + "Status: " + (isPlaying ? "PLAYING" : "STOPPED") + "\n"
            + "Start Time: " + startTime + "\n\n"
            + "Press OK to STOP this video player.\n"
            + "Press Cancel to go back.";

        setObjVar(self, "video_manager.selected", target.toString());
        sui.msgbox(self, player, info, sui.OK_CANCEL, "Video Player Details", sui.MSG_NORMAL, "handleVideoPlayerAction");

        return SCRIPT_CONTINUE;
    }

    public int handleVideoPlayerAction(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !isGod(player))
            return SCRIPT_CONTINUE;

        if (!hasObjVar(self, "video_manager.selected"))
            return SCRIPT_CONTINUE;

        String targetStr = getStringObjVar(self, "video_manager.selected");
        obj_id target = obj_id.getObjId(targetStr);

        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("Object no longer valid."));
            return SCRIPT_CONTINUE;
        }

        if (hasCondition(target, CONDITION_MAGIC_VIDEO_PLAYER))
        {
            clearCondition(target, CONDITION_MAGIC_VIDEO_PLAYER);
            sendSystemMessage(player, string_id.unlocalized("Video player " + target + " stopped."));
            LOG("video_manager", "[VideoManager] " + getName(player) + " stopped video player " + target);
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Video player " + target + " is already stopped."));
        }

        removeObjVar(self, "video_manager.selected");
        return SCRIPT_CONTINUE;
    }

    public int handleSpeakerSelected(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !isGod(player))
            return SCRIPT_CONTINUE;

        int row = sui.getListboxSelectedRow(params);
        if (row < 0)
            return SCRIPT_CONTINUE;

        if (!hasObjVar(self, "video_manager.speaker_ids"))
            return SCRIPT_CONTINUE;

        String[] ids = getStringArrayObjVar(self, "video_manager.speaker_ids");
        if (ids == null || row >= ids.length)
            return SCRIPT_CONTINUE;

        obj_id target = obj_id.getObjId(ids[row]);
        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("Invalid object."));
            return SCRIPT_CONTINUE;
        }

        String parentId = "";
        if (hasObjVar(target, OBJVAR_EMITTER_PARENT_ID))
            parentId = getStringObjVar(target, OBJVAR_EMITTER_PARENT_ID);

        String info = "Speaker: " + target + "\n"
            + "Name: " + getName(target) + "\n"
            + "Linked to: " + parentId + "\n\n"
            + "Press OK to DESTROY this speaker.\n"
            + "Press Cancel to go back.";

        setObjVar(self, "video_manager.selected_speaker", target.toString());
        sui.msgbox(self, player, info, sui.OK_CANCEL, "Speaker Details", sui.MSG_NORMAL, "handleSpeakerAction");

        return SCRIPT_CONTINUE;
    }

    public int handleSpeakerAction(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        int event = sui.getIntButtonPressed(params);
        if (event == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        obj_id player = sui.getPlayerId(params);
        if (!isIdValid(player) || !isGod(player))
            return SCRIPT_CONTINUE;

        if (!hasObjVar(self, "video_manager.selected_speaker"))
            return SCRIPT_CONTINUE;

        String targetStr = getStringObjVar(self, "video_manager.selected_speaker");
        obj_id target = obj_id.getObjId(targetStr);

        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("Speaker no longer valid."));
            return SCRIPT_CONTINUE;
        }

        sendSystemMessage(player, string_id.unlocalized("Speaker " + target + " destroyed."));
        LOG("video_manager", "[VideoManager] " + getName(player) + " destroyed speaker " + target);
        destroyObject(target);

        removeObjVar(self, "video_manager.selected_speaker");
        return SCRIPT_CONTINUE;
    }
}
