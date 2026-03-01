package script.terminal;

import script.dictionary;
import script.library.sui;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class magic_video_emitter extends script.base_script
{
    private static final int MENU_EMITTER_ROOT = menu_info_types.SERVER_MENU37;
    private static final int MENU_EMITTER_DESTROY = menu_info_types.SERVER_MENU38;
    private static final int MENU_EMITTER_INFO = menu_info_types.SERVER_MENU39;

    private static final String OBJVAR_EMITTER_PARENT_ID = "video_emitter.parent_id";

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int root = mi.addRootMenu(MENU_EMITTER_ROOT, string_id.unlocalized("Speaker"));

        String parentId = "";
        if (hasObjVar(self, OBJVAR_EMITTER_PARENT_ID))
            parentId = getStringObjVar(self, OBJVAR_EMITTER_PARENT_ID);

        mi.addSubMenu(root, MENU_EMITTER_INFO, string_id.unlocalized("Linked to: " + (parentId.isEmpty() ? "(none)" : parentId)));

        if (canModifyEmitter(player))
        {
            mi.addSubMenu(root, MENU_EMITTER_DESTROY, string_id.unlocalized("Destroy Speaker"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_EMITTER_DESTROY)
        {
            if (!canModifyEmitter(player))
            {
                sendSystemMessage(player, string_id.unlocalized("You do not have permission to destroy this speaker."));
                return SCRIPT_CONTINUE;
            }

            sendSystemMessage(player, string_id.unlocalized("Speaker destroyed."));
            LOG("video_player", "[VideoEmitter] " + getName(player) + " destroyed speaker " + self);
            destroyObject(self);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    private boolean canModifyEmitter(obj_id player) throws InterruptedException
    {
        if (isGod(player))
            return true;

        if (hasSkill(player, "social_entertainer_master"))
            return true;

        return false;
    }
}
