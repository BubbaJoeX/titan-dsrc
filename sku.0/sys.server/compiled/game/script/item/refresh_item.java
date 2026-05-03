package script.item;

import script.*;
import script.library.prose;
import script.library.utils;

public class refresh_item extends script.base_script
{
    public refresh_item()
    {
    }

    public static final string_id SID_NOT_YET = new string_id("base_player", "not_yet");
    public static final string_id SID_NOT_WHILE_IN_COMBAT = new string_id("base_player", "not_while_in_combat");
    public static final string_id SID_NO_NEED_TO_HEAL = new string_id("base_player", "no_need_to_heal");
    public static final string_id SID_ITEM_NOT_IN_INVENTORY = new string_id("base_player", "not_in_your_inventory");

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (canManipulate(player, self, true, true, 15, true))
        {
            if (utils.isNestedWithinAPlayer(self))
            {
                menu_info_data mid = mi.getMenuItemByType(menu_info_types.ITEM_USE);
                if (mid != null)
                {
                    mid.setServerNotify(true);
                }
                else
                {
                    mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("ui_radial", "item_use"));
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (utils.getContainingPlayer(self) != player)
        {
            return SCRIPT_CONTINUE;
        }

        if (item == menu_info_types.ITEM_USE)
        {
            if (isIncapacitated(player) || isDead(player))
            {
                sendSystemMessage(player, new string_id("spam", "cant_do_it_state"));
                return SCRIPT_CONTINUE;
            }

            if (getState(player, STATE_COMBAT) > 0)
            {
                sendSystemMessage(player, SID_NOT_WHILE_IN_COMBAT);
                return SCRIPT_CONTINUE;
            }

            // Check if player is already at max health
            int max_hp = getMaxAttrib(player, HEALTH);
            int hp = getAttrib(player, HEALTH);
            int to_heal = max_hp - hp;

            if (to_heal <= 0)
            {
                sendSystemMessage(player, SID_NO_NEED_TO_HEAL);
                return SCRIPT_CONTINUE;
            }

            // Restore health to maximum
            setAttrib(player, HEALTH, max_hp);
            setAttrib(player, ACTION, getMaxAttrib(player, ACTION));
            setAttrib(player, MIND, getMaxAttrib(player, MIND));

            // Show visual feedback
            prose_package pp = new prose_package();
            pp = prose.setStringId(pp, new string_id("healing", "heal_fly"));
            pp = prose.setDI(pp, to_heal);
            pp = prose.setTO(pp, "HEALTH");
            showFlyTextPrivateProseWithFlags(player, player, pp, 2.0f, colors.SEAGREEN, FLY_TEXT_FLAG_IS_HEAL);

            // Play animation and effect
            doAnimationAction(player, "gesture_fortify");
            playClientEffectObj(player, "clienteffect/bacta_bomb.cef", player, "");

            // Decrement item count
            if (getCount(self) > 0)
            {
                static_item.decrementStaticItem(self);
            }
        }
        return SCRIPT_CONTINUE;
    }
}
