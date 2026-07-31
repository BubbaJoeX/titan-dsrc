package script.structure.filler;

import script.dictionary;
import script.library.structure;
import script.obj_id;

public class filler extends script.base_script
{
    public filler()
    {
    }
    public static final String SCRIPT_ME = "structure.filler.filler";
    public static final float DELAY_TIME = 5.0f;
    public static final float INITIAL_STAGGER_WINDOW = 30.0f;
    public static final float BATCH_DELAY = 1.0f;
    public static final String HANDLER_INIT_FILLER_SPAWN = "handleInitFillerSpawn";
    public static final String HANDLER_CLEANUP_FILLER_SPAWN = "handleCleanupFillerSpawn";
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if (!hasMessageTo(self, HANDLER_INIT_FILLER_SPAWN))
        {
            float stagger = ((self.getValue() & 0x7fffffffL) % (long)(INITIAL_STAGGER_WINDOW * 1000.0f)) / 1000.0f;
            messageTo(self, HANDLER_INIT_FILLER_SPAWN, null, DELAY_TIME + stagger, false);
        }
        return SCRIPT_CONTINUE;
    }
    public int OnDetach(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int OnUnloadedFromMemory(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int OnDestroy(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }
    public int handleInitFillerSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        /*if (!structure.initializeFillerSpawns(self))
        {
            messageTo(self, HANDLER_INIT_FILLER_SPAWN, null, BATCH_DELAY, false);
        }*/
        return SCRIPT_CONTINUE;
    }
    public int handleCleanupFillerSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        structure.cleanupFillerSpawns(self);
        return SCRIPT_CONTINUE;
    }
}
