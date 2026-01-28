package script.terminal;

import script.obj_id;
import script.menu_info;
import script.menu_info_types;

public class terminal_movement_mode extends script.base_script
{
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_MOVEMENT_MODE, null);
        return SCRIPT_CONTINUE;
    }
}
