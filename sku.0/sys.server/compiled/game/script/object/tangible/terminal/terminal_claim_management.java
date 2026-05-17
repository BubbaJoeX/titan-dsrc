package script.object.tangible.terminal;

import script.*;
import script.library.claim_management;

/**
 * Optional dedicated claim management terminal (shares UI with city-flag marker).
 */
public class terminal_claim_management extends script.base_script
{
    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, "claim.id"))
        {
            return SCRIPT_CONTINUE;
        }
        if (isDead(player) || isIncapacitated(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (!claimCanManageTerminal(player, self))
        {
            return SCRIPT_CONTINUE;
        }
        mi.addRootMenu(menu_info_types.SERVER_TERMINAL_MANAGEMENT, claim_management.SID_MANAGEMENT);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.SERVER_TERMINAL_MANAGEMENT)
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasObjVar(self, "claim.id"))
        {
            return SCRIPT_CONTINUE;
        }
        claim_management.openMainPanel(self, player);
        return SCRIPT_CONTINUE;
    }
}
