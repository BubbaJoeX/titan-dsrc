package script.content.fun;/*
@Origin: dsrc.script.content.fun
@Author:  BubbaJoeX
@Purpose: Mine ore from a fallen asteroid fragment with timed extraction and feedback
@Requirements: <no requirements>
@Notes: Optional objvars on this object (set in the editor or by a spawner):
  fun.fallen_asteroid.level_req (int) — minimum character level, default 10
  fun.fallen_asteroid.duration (int) — channel seconds, default 15 (max 60)
  fun.fallen_asteroid.sui_range (float) — stay within this distance of the fragment during the timer, default 15
  fun.fallen_asteroid.finish_range (float) — max distance when the channel completes or it fails, default 18
  fun.fallen_asteroid.min_yield (int) / max_yield (int) — crate units, defaults 240 / 630
  fun.fallen_asteroid.resource (string) — resource type name for getResourceTypeByName, default "asteroid"
  fun.fallen_asteroid.max_harvests (int) — successful extractions before depletion, default 1
  fun.fallen_asteroid.destroy_when_depleted (boolean) — remove the object when exhausted, default true
@Created: Tuesday, 2/25/2025, at 7:43 PM,
@Copyright © SWG: Titan 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.utils;

import static script.library.sui.*;

public class fallen_asteroid extends base_script
{
    public static final boolean LOGGING = false;

    private static final String MINING_TIMER = "fun.fallen_asteroid.mining";
    private static final String ACTIVE_MINER = "fun.fallen_asteroid.active_miner";
    private static final String HARVESTS_DONE = "fun.fallen_asteroid.harvests_done";

    private static final int DEFAULT_LEVEL = 10;
    private static final int DEFAULT_DURATION = 15;
    private static final int DURATION_CAP = 60;
    private static final float DEFAULT_SUI_RANGE = 15.0f;
    private static final float DEFAULT_FINISH_RANGE = 18.0f;

    private static final String DEFAULT_RESOURCE = "asteroid";

    private static final String CEF_IMPACT = "clienteffect/combat_explosion_lair_large.cef";
    private static final String CEF_DUST = "clienteffect/lair_med_damage_smoke.cef";

    private static final String SND_START = "sound/item_fusioncutter_start.snd";
    private static final String SND_SUCCESS = "sound/item_ding.snd";

    private static final String[] START_LINES =
            {
                    "You brace against the fragment and begin cutting into the vitrified crust...",
                    "Micro-fractures sparkle as your tools bite into the meteoric alloy.",
                    "Ionized dust swirls; you work methodically, freeing usable ore from the wreckage.",
            };

    private static final String[] SUCCESS_LINES =
            {
                    "A satisfying crack—and a dense cluster of raw material breaks free into your pack.",
                    "The fragment yields; cooling slag patters to the ground as you stow the haul.",
                    "Veins of ore part under pressure. This strike paid off.",
            };

    public int OnAttach(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int sync(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }

        if (isDepleted(self))
        {
            sendSystemMessage(player, "This meteorite has already been stripped bare—only cooling slag remains.", "");
            return SCRIPT_CONTINUE;
        }

        int req = getIntSetting(self, "fun.fallen_asteroid.level_req", DEFAULT_LEVEL);
        if (getLevel(player) < req)
        {
            sendSystemMessage(player, "You lack the experience to work this deposit safely. (Requires level " + req + ".)", "");
            return SCRIPT_CONTINUE;
        }

        obj_id busy = getObjIdObjVar(self, ACTIVE_MINER);
        if (isIdValid(busy) && busy != player && exists(busy))
        {
            sendSystemMessage(player, "Someone else is already working this fragment.", "");
            return SCRIPT_CONTINUE;
        }

        mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Extract meteoric ore"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != menu_info_types.ITEM_USE || !isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }

        if (!exists(self) || isDepleted(self))
        {
            sendSystemMessage(player, "There is nothing left to harvest here.", "");
            return SCRIPT_CONTINUE;
        }

        int req = getIntSetting(self, "fun.fallen_asteroid.level_req", DEFAULT_LEVEL);
        if (getLevel(player) < req)
        {
            sendSystemMessage(player, "You must be at least level " + req + " to extract from this fragment.", "");
            return SCRIPT_CONTINUE;
        }

        obj_id busy = getObjIdObjVar(self, ACTIVE_MINER);
        if (isIdValid(busy) && busy != player && exists(busy))
        {
            sendSystemMessage(player, "Someone else is already working this fragment.", "");
            return SCRIPT_CONTINUE;
        }

        if (hasObjVar(player, MINING_TIMER))
        {
            sendSystemMessage(player, "You are already extracting a deposit.", "");
            return SCRIPT_CONTINUE;
        }

        int duration = getIntSetting(self, "fun.fallen_asteroid.duration", DEFAULT_DURATION);
        if (duration < 1)
        {
            duration = 1;
        }
        if (duration > DURATION_CAP)
        {
            duration = DURATION_CAP;
        }

        float suiRange = getFloatSetting(self, "fun.fallen_asteroid.sui_range", DEFAULT_SUI_RANGE);

        doAnimationAction(player, "manipulate_low");
        playMiningFeedback(self, player, true);

        setObjVar(player, MINING_TIMER, getGameTime());
        setObjVar(self, ACTIVE_MINER, player);

        int flags = CD_EVENT_NONE;
        flags |= CD_EVENT_COMBAT;
        flags |= CD_EVENT_LOCOMOTION;
        flags |= CD_EVENT_INCAPACITATE;

        String title = "Extracting ore";
        int pid = smartCountdownTimerSUI(self, player, "quest_countdown_timer", string_id.unlocalized(title), 0, duration, "handleMiningComplete", suiRange, flags);
        if (pid > -1)
        {
            dictionary params = new dictionary();
            params.put("player", player);
            params.put("pid", pid);
            messageTo(self, "handleMiningComplete", params, duration, false);
            sendSystemMessage(player, START_LINES[rand(0, START_LINES.length - 1)], "");
        }
        else
        {
            removeObjVar(player, MINING_TIMER);
            removeObjVar(self, ACTIVE_MINER);
        }

        return SCRIPT_CONTINUE;
    }

    public int handleMiningComplete(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
        {
            return SCRIPT_CONTINUE;
        }

        obj_id player = params.getObjId("player");
        int pid = params.getInt("pid");

        if (isIdValid(player) && pid > -1)
        {
            forceCloseSUIPage(pid);
        }

        if (!exists(self))
        {
            if (isIdValid(player))
            {
                removeObjVar(player, MINING_TIMER);
            }
            return SCRIPT_CONTINUE;
        }

        removeObjVar(self, ACTIVE_MINER);

        if (!isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }

        removeObjVar(player, MINING_TIMER);

        if (isDepleted(self))
        {
            return SCRIPT_CONTINUE;
        }

        float finishRange = getFloatSetting(self, "fun.fallen_asteroid.finish_range", DEFAULT_FINISH_RANGE);
        if (getDistance(player, self) > finishRange)
        {
            sendSystemMessage(player, "You moved too far from the fragment—the ore cools and locks back into the rock.", "");
            return SCRIPT_CONTINUE;
        }

        grantResources(self, player);
        playMiningFeedback(self, player, false);

        int maxHarvests = getIntSetting(self, "fun.fallen_asteroid.max_harvests", 1);
        if (maxHarvests < 1)
        {
            maxHarvests = 1;
        }

        int done = 0;
        if (hasObjVar(self, HARVESTS_DONE))
        {
            done = getIntObjVar(self, HARVESTS_DONE);
        }
        done++;
        setObjVar(self, HARVESTS_DONE, done);

        sendSystemMessage(player, SUCCESS_LINES[rand(0, SUCCESS_LINES.length - 1)], "");
        blog("Granted resources to " + player + " harvest " + done + "/" + maxHarvests);

        if (done >= maxHarvests)
        {
            boolean destroy = getBoolSetting(self, "fun.fallen_asteroid.destroy_when_depleted", true);
            setObjVar(self, "fun.fallen_asteroid.depleted", 1);
            setName(self, "Spent meteorite rubble");
            if (destroy)
            {
                destroyObject(self);
            }
        }
        else
        {
            sendSystemMessage(player, "The mass still holds recoverable material—you could extract again.", "");
        }

        return SCRIPT_CONTINUE;
    }

    private boolean isDepleted(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, "fun.fallen_asteroid.depleted");
    }

    private void playMiningFeedback(obj_id self, obj_id player, boolean starting) throws InterruptedException
    {
        location loc = getLocation(self);
        if (loc == null)
        {
            return;
        }
        if (starting)
        {
            playClientEffectLoc(player, CEF_DUST, loc, 0.0f);
            playClientEffectObj(player, SND_START, player, "");
        }
        else
        {
            playClientEffectLoc(player, CEF_IMPACT, loc, 0.0f);
            playClientEffectLoc(player, CEF_DUST, loc, 0.0f);
            play2dNonLoopingSound(player, SND_SUCCESS);
        }
    }

    private void grantResources(obj_id self, obj_id player) throws InterruptedException
    {
        String resName = DEFAULT_RESOURCE;
        if (hasObjVar(self, "fun.fallen_asteroid.resource"))
        {
            resName = getStringObjVar(self, "fun.fallen_asteroid.resource");
        }

        obj_id resourceType = getResourceTypeByName(resName);
        if (!isIdValid(resourceType))
        {
            blog("Bad resource type '" + resName + "', falling back to default.");
            resourceType = getResourceTypeByName(DEFAULT_RESOURCE);
        }
        if (!isIdValid(resourceType))
        {
            sendSystemMessage(player, "The deposit yields nothing usable—resource type is misconfigured.", "");
            return;
        }

        int minY = getIntSetting(self, "fun.fallen_asteroid.min_yield", 240);
        int maxY = getIntSetting(self, "fun.fallen_asteroid.max_yield", 630);
        if (minY > maxY)
        {
            int t = minY;
            minY = maxY;
            maxY = t;
        }

        int amount = rand(minY, maxY);
        createResourceCrate(resourceType, amount, utils.getInventoryContainer(player));
    }

    private int getIntSetting(obj_id self, String key, int defaultVal) throws InterruptedException
    {
        if (hasObjVar(self, key))
        {
            return getIntObjVar(self, key);
        }
        return defaultVal;
    }

    private float getFloatSetting(obj_id self, String key, float defaultVal) throws InterruptedException
    {
        if (hasObjVar(self, key))
        {
            return getFloatObjVar(self, key);
        }
        return defaultVal;
    }

    private boolean getBoolSetting(obj_id self, String key, boolean defaultVal) throws InterruptedException
    {
        if (hasObjVar(self, key))
        {
            return getBooleanObjVar(self, key);
        }
        return defaultVal;
    }

    public void blog(String msg)
    {
        if (LOGGING)
        {
            LOG("ethereal", "[fallen_asteroid]: " + msg);
        }
    }
}
