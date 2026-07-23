package script.terminal;

import script.*;
import script.systems.dynamic_bunker.dynamic_bunker_lib;

/**
 * Optional convenience terminal. Prefer the slash command:
 *   /dynamicBunker
 * which needs no scripts or objvars — just stand inside a POB.
 *
 * If this script is attached, the radial works with zero objvar setup
 * (socket is auto-detected from the building / player cell).
 */
public class terminal_dynamic_bunker extends script.base_script
{
    public terminal_dynamic_bunker()
    {
    }

    public static final int MENU_ASSIGN_ROOM = menu_info_types.SERVER_MENU50;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Bunker Floorplan Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(MENU_ASSIGN_ROOM, string_id.unlocalized("Bunker Floorplan"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != MENU_ASSIGN_ROOM)
        {
            return SCRIPT_CONTINUE;
        }

        // Auto path: building the player is in (no objvars). Prefer /dynamicBunker in chat.
        if (openDynamicBunkerFloorplanHere(player))
        {
            return SCRIPT_CONTINUE;
        }

        sendSystemMessageTestingOnly(player, "[DynamicBunker] Stand inside a POB building, or use /dynamicBunker.");
        return SCRIPT_CONTINUE;
    }
}
