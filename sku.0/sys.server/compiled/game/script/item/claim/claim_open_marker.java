package script.item.claim;

import script.*;
import script.library.sui;
import script.library.utils;

public class claim_open_marker extends script.base_script
{
    public int handleClaimRepossession(obj_id self, dictionary params) throws InterruptedException
    {
        destroyObject(self);
        return SCRIPT_CONTINUE;
    }
}
