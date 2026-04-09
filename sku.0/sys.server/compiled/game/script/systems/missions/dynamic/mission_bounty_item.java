package script.systems.missions.dynamic;

import script.dictionary;
import script.library.bounty_hunter;
import script.library.missions;
import script.obj_id;

public class mission_bounty_item extends script.systems.missions.base.mission_dynamic_base
{
    public mission_bounty_item()
    {
    }
    public int OnStartMission(obj_id self, dictionary params) throws InterruptedException
    {
        setObjVar(self, "intState", missions.STATE_MISSION_IN_PROGRESS);
        if (!hasObjVar(self, "bh.itemBountyClueStage"))
        {
            setObjVar(self, "bh.itemBountyClueStage", 0);
        }
        obj_id hunter = getMissionHolder(self);
        if (isIdValid(hunter))
        {
            sendSystemMessage(hunter, "[Item Bounty] Contract accepted. Acquire the requested item and turn it in to a bounty broker.", null);
            messageTo(self, "sendItemBountyClue", null, 2.0f, true);
        }
        return SCRIPT_CONTINUE;
    }
    public int sendItemBountyClue(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id hunter = getMissionHolder(self);
        if (!isIdValid(hunter))
        {
            return SCRIPT_CONTINUE;
        }
        int stage = hasObjVar(self, "bh.itemBountyClueStage") ? getIntObjVar(self, "bh.itemBountyClueStage") : 0;
        obj_id item = hasObjVar(self, "bh.itemBountyTarget") ? getObjIdObjVar(self, "bh.itemBountyTarget") : obj_id.NULL_ID;
        if (stage <= 0)
        {
            sendSystemMessage(hunter, "[Item Bounty] Clue: target profile is " + (isCrafted(item) ? "crafted" : "static loot") + ".", null);
            setObjVar(self, "bh.itemBountyClueStage", 1);
        }
        else if (stage == 1)
        {
            String template = isIdValid(item) ? getTemplateName(item) : "";
            if (template == null)
            {
                template = "";
            }
            if (template.length() > 0)
            {
                String[] parts = split(template, '/');
                String category = (parts != null && parts.length > 2) ? parts[2] : "unknown";
                sendSystemMessage(hunter, "[Item Bounty] Clue: object class trace indicates '" + category + "'.", null);
            }
            else
            {
                sendSystemMessage(hunter, "[Item Bounty] Clue: metadata signal is unstable.", null);
            }
            setObjVar(self, "bh.itemBountyClueStage", 2);
        }
        else
        {
            sendSystemMessage(hunter, "[Item Bounty] Clue: hand-in location is any bounty broker.", null);
        }
        return SCRIPT_CONTINUE;
    }
    public int abortMission(obj_id self, dictionary params) throws InterruptedException
    {
        endMission(self);
        return SCRIPT_CONTINUE;
    }
}
