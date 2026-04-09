package script.library;

import script.*;
import java.util.Vector;

public class bounty_hunter extends script.base_script
{
    public bounty_hunter()
    {
    }
    public static final String[] LOW_PAYOUT_COMMS = 
    {
        "pp_small_1",
        "pp_small_2",
        "pp_small_3",
        "pp_small_4",
        "pp_small_5"
    };
    public static final String[] MEDIUM_PAYOUT_COMMS = 
    {
        "pp_normal_1",
        "pp_normal_2",
        "pp_normal_3",
        "pp_normal_4",
        "pp_normal_5"
    };
    public static final String[] HIGH_PAYOUT_COMMS = 
    {
        "pp_big_1",
        "pp_big_2",
        "pp_big_3",
        "pp_big_4",
        "pp_big_5"
    };
    public static final String[] TALKING_COMM_CHARACTER = 
    {
        "object/mobile/dressed_tatooine_jabba_thug.iff",
        "object/mobile/dressed_tatooine_jabba_thief.iff",
        "object/mobile/dressed_tatooine_jabba_henchman.iff",
        "object/mobile/dressed_tatooine_jabba_enforcer.iff",
        "object/mobile/ephant_mon.iff",
        "object/mobile/dressed_tatooine_jabba_assassin.iff"
    };
    public static final int BOUNTY_PAYOUT_AMOUNT_MINIMUM = 50;
    public static final int BOUNTY_PAYOUT_AMOUNT_MAXIMUM = 2000;
    public static final boolean BOUNTY_DO_FREQUENCY_ADJUSTER = false;
    public static final int BOUNTY_FREQUENCY = 60;
    public static final float BOUNTY_PAYOUT_ADJUSTER = 0.0f;
    public static final float BOUNTY_COLLECT_TIME_LIMIT = 600.0f;
    public static final boolean BOUNTY_DO_LEVEL_ADJUSTER = true;
    public static final int BOUNTY_FLOOD_CONTROL_DELAY = 60;
    public static final int BOUNTY_MISSION_TIME_LIMIT = 259200;
    public static final int MAX_BOUNTY = 2000000000;
    public static final int MAX_BOUNTY_SET = 1000000;
    public static final int MIN_BOUNTY_SET = 20000;
    public static final String CREATURE_TABLE = "datatables/mob/creatures.iff";
    public static final String BOUNTY_CHECK_TABLE = "datatables/missions/bounty/bounty_check.iff";
    public static final String STF = "bounty_hunter";
    public static final string_id PROSE_NO_BOUNTY_MINUTE = new string_id(STF, "no_bounties_while");
    public static final string_id PROSE_NO_BOUNTY_SECONDS = new string_id(STF, "no_bounties_soon");
    public static final string_id PROSE_NO_BOUNTY = new string_id(STF, "prose_no_bounty");
    public static final string_id NO_BOUNTY_TARGET = new string_id(STF, "no_bounty_target");
    public static final string_id NO_BOUNTY_TARGET_ALREADY = new string_id(STF, "no_bounty_target_already");
    public static final string_id NO_BOUNTY_PLAYER = new string_id(STF, "no_bounty_player");
    public static final string_id BOUNTY_TOO_LOW_LEVEL = new string_id(STF, "no_bounty_too_low");
    public static final string_id FUGITIVE = new string_id(STF, "fugitive");
    public static final string_id BOUNTY_ALREADY = new string_id(STF, "bounty_already_issued");
    public static final string_id BOUNTY_CHECK_PAYMENT = new string_id(STF, "bounty_check_payment");
    public static final string_id STF_NO_BOUNTIES = new string_id(STF, "flood_control");
    public static final string_id ALREADY_HAVE_TARGET = new string_id(STF, "already_have_target");
    public static final string_id ALREADY_BEING_HUNTED = new string_id(STF, "already_being_hunted");
    public static final string_id TARGET_COLLECTING_BOUNTY = new string_id(STF, "target_collecting_bounty");
    public static final string_id BOUNTY_FAILED_HUNTER = new string_id(STF, "bounty_failed_hunter");
    public static final string_id BOUNTY_FAILED_TARGET = new string_id(STF, "bounty_failed_target");
    public static final string_id BOUNTY_SUCCESS_HUNTER = new string_id(STF, "bounty_success_hunter");
    public static final string_id BOUNTY_SUCCESS_TARGET = new string_id(STF, "bounty_success_target");
    public static final boolean CONST_FLAG_DO_LOGGING = true;
    public static final int DROID_PROBOT = 1;
    public static final int DROID_SEEKER = 2;
    public static final int DROID_TRACK_TARGET = 1;
    public static final int DROID_FIND_TARGET = 2;
    public static final int INVESTIGATION_STAGE_COLD = 0;
    public static final int INVESTIGATION_STAGE_WARM = 1;
    public static final int INVESTIGATION_STAGE_CONFIRMED = 2;
    public static final float INVESTIGATION_DECAY_PER_SECOND = 0.0012f;
    public static final float INVESTIGATION_COLD_THRESHOLD = 0.34f;
    public static final float INVESTIGATION_WARM_THRESHOLD = 0.67f;
    public static final String OBJVAR_INVEST_STAGE = "bh.invest.stage";
    public static final String OBJVAR_INVEST_CONFIDENCE = "bh.invest.confidence";
    public static final String OBJVAR_INVEST_LAST_UPDATE = "bh.invest.lastUpdate";
    public static final String OBJVAR_INVEST_LAST_CLUE = "bh.invest.lastClueType";
    public static final String OBJVAR_COUNTERPLAY_JAM_UNTIL = "bh.counterplay.jamUntil";
    public static final String OBJVAR_COUNTERPLAY_DECOY_UNTIL = "bh.counterplay.decoyUntil";
    public static final String OBJVAR_COUNTERPLAY_DECOY_STRENGTH = "bh.counterplay.decoyStrength";
    public static final String OBJVAR_REMOTE_PROBE_NEXT_DISPATCH = "bh.remoteProbe.nextDispatch";
    public static final String OBJVAR_RENOWN_POINTS = "bh.renown.points";
    public static final String OBJVAR_RENOWN_RANK = "bh.renown.rank";
    public static final String OBJVAR_ANTIGRIEF_LAST_TARGET = "bh.antigrief.lastTarget";
    public static final String OBJVAR_ANTIGRIEF_LAST_REWARD_TIME = "bh.antigrief.lastRewardTime";
    public static final String OBJVAR_ANTIGRIEF_LAST_POST_TIME = "bh.antigrief.lastPostTime";
    public static final String OBJVAR_CONTRACT_TYPE = "bh.contract.type";
    public static final String CONTRACT_TYPE_PVE = "PVE_NPC";
    public static final String CONTRACT_TYPE_PVP = "PVP_PLAYER";
    public static final int ANTIGRIEF_REWARD_COOLDOWN = 1800;
    public static final int ANTIGRIEF_POST_COOLDOWN = 300;
    public static final String HEAT_ROOT = "bh.heat";
    public static final float HEAT_DECAY_PER_SECOND = 0.004f;
    public static final float HEAT_MAX = 100.0f;
    public static final float HEAT_MIN = 0.0f;
    public static final String ITEM_BOUNTY_ROOT = "bh.itemBounty";
    public static final String ITEM_BOUNTY_STATE_OPEN = "OPEN";
    public static final String ITEM_BOUNTY_STATE_ACCEPTED = "ACCEPTED";
    public static final String ITEM_BOUNTY_STATE_READY = "READY_FOR_PICKUP";
    public static final String ITEM_BOUNTY_STATE_PICKED_UP = "PICKED_UP";
    public static final String GALAXY_SHARED_STORE_PLANET = "tatooine";
    public static void debugLogging(String section, String message) throws InterruptedException
    {
        if (CONST_FLAG_DO_LOGGING)
        {
            LOG("debug/bounty_hunter.scriptlib/" + section, message);
        }
    }
    public static float clamp(float value, float min, float max) throws InterruptedException
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }
    public static void initializeMissionContractType(obj_id mission) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return;
        }
        if (hasObjVar(mission, "objTarget"))
        {
            setObjVar(mission, OBJVAR_CONTRACT_TYPE, CONTRACT_TYPE_PVP);
        }
        else
        {
            setObjVar(mission, OBJVAR_CONTRACT_TYPE, CONTRACT_TYPE_PVE);
        }
    }
    public static String getMissionContractType(obj_id mission) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return CONTRACT_TYPE_PVE;
        }
        if (hasObjVar(mission, OBJVAR_CONTRACT_TYPE))
        {
            return getStringObjVar(mission, OBJVAR_CONTRACT_TYPE);
        }
        initializeMissionContractType(mission);
        return hasObjVar(mission, OBJVAR_CONTRACT_TYPE) ? getStringObjVar(mission, OBJVAR_CONTRACT_TYPE) : CONTRACT_TYPE_PVE;
    }
    public static int getNextContractId() throws InterruptedException
    {
        obj_id store = getHeatStoreObject();
        if (!isIdValid(store))
        {
            return 0;
        }
        String key = "bh.contract.nextId";
        int next = hasObjVar(store, key) ? getIntObjVar(store, key) : 1;
        setObjVar(store, key, next + 1);
        return next;
    }
    public static boolean openContract(obj_id issuer, obj_id target, int escrow, String contractType) throws InterruptedException
    {
        if (!isIdValid(issuer) || !isIdValid(target) || escrow <= 0)
        {
            return false;
        }
        int id = getNextContractId();
        if (id <= 0)
        {
            return false;
        }
        String root = "bh.contract." + id;
        setObjVar(target, root + ".id", id);
        setObjVar(target, root + ".issuer", issuer);
        setObjVar(target, root + ".escrow", escrow);
        setObjVar(target, root + ".type", (contractType == null || contractType.length() < 1) ? CONTRACT_TYPE_PVP : contractType);
        setObjVar(target, root + ".state", "OPEN");
        setObjVar(target, root + ".posted", getGameTime());
        setObjVar(target, "bh.contract.activeId", id);
        CustomerServiceLog("bounty", "Contract " + id + " opened by %TU on %TT for escrow " + escrow, issuer, target);
        return true;
    }
    public static boolean acceptContract(obj_id hunter, obj_id target) throws InterruptedException
    {
        if (!isIdValid(hunter) || !isIdValid(target))
        {
            return false;
        }
        if (!hasObjVar(target, "bh.contract.activeId"))
        {
            return false;
        }
        int id = getIntObjVar(target, "bh.contract.activeId");
        String root = "bh.contract." + id;
        if (!hasObjVar(target, root + ".state"))
        {
            return false;
        }
        String state = getStringObjVar(target, root + ".state");
        if (!state.equals("OPEN"))
        {
            return false;
        }
        setObjVar(target, root + ".state", "ACCEPTED");
        setObjVar(target, root + ".hunter", hunter);
        setObjVar(target, root + ".accepted", getGameTime());
        CustomerServiceLog("bounty", "Contract " + id + " accepted by %TU for target %TT", hunter, target);
        return true;
    }
    public static obj_id getItemBountyStore() throws InterruptedException
    {
        // This store object is intentionally galaxy-global for the shard.
        // Using a stable world object keeps item bounties visible from any planet terminal.
        obj_id store = getPlanetByName(GALAXY_SHARED_STORE_PLANET);
        if (!isIdValid(store))
        {
            store = getHeatStoreObject();
        }
        return store;
    }
    public static int getNextItemBountyId() throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store))
        {
            return 0;
        }
        String key = ITEM_BOUNTY_ROOT + ".nextId";
        int next = hasObjVar(store, key) ? getIntObjVar(store, key) : 1;
        setObjVar(store, key, next + 1);
        return next;
    }
    public static boolean isStaticItemNoTradeSharedName(String staticItemName) throws InterruptedException
    {
        if (staticItemName == null || staticItemName.length() < 1)
        {
            return false;
        }
        String[] noTradeShared = dataTableGetStringColumn(static_item.ITEM_NO_TRADE_SHARED_TABLE, 0);
        if (noTradeShared != null)
        {
            for (String entry : noTradeShared)
            {
                if (staticItemName.equals(entry))
                {
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean canCreateItemBounty(obj_id creator, obj_id item, boolean sendMessages) throws InterruptedException
    {
        if (!isIdValid(creator) || !isPlayer(creator) || !isIdValid(item))
        {
            return false;
        }
        if (isPlayer(item))
        {
            if (sendMessages)
            {
                sendSystemMessage(creator, "Item bounty target must be an item object.", null);
            }
            return false;
        }
        if (isNoTradeShared(item))
        {
            if (sendMessages)
            {
                sendSystemMessage(creator, "This item is no-trade/shared and cannot be bountied.", null);
            }
            return false;
        }
        boolean crafted = isCrafted(item);
        boolean staticLoot = static_item.isStaticItem(item);
        if (!crafted && !staticLoot)
        {
            if (sendMessages)
            {
                sendSystemMessage(creator, "Only crafted or static loot items can receive item bounties.", null);
            }
            return false;
        }
        if (staticLoot)
        {
            String staticName = static_item.getStaticItemName(item);
            if (static_item.isUniqueStaticItem(item) || isStaticItemNoTradeSharedName(staticName))
            {
                if (sendMessages)
                {
                    sendSystemMessage(creator, "Unique or no-trade static items cannot receive item bounties.", null);
                }
                return false;
            }
        }
        return true;
    }
    public static boolean createItemBounty(obj_id creator, obj_id item, int reward, obj_id terminal) throws InterruptedException
    {
        if (!canCreateItemBounty(creator, item, true))
        {
            return false;
        }
        if (reward < MIN_BOUNTY_SET || reward > MAX_BOUNTY_SET)
        {
            sendSystemMessage(creator, "Item bounty reward must be within standard bounty limits.", null);
            return false;
        }
        obj_id store = getItemBountyStore();
        if (!isIdValid(store))
        {
            return false;
        }
        int id = getNextItemBountyId();
        if (id <= 0)
        {
            return false;
        }
        String root = ITEM_BOUNTY_ROOT + "." + id;
        setObjVar(store, root + ".id", id);
        setObjVar(store, root + ".state", ITEM_BOUNTY_STATE_OPEN);
        setObjVar(store, root + ".creator", creator);
        setObjVar(store, root + ".item", item);
        setObjVar(store, root + ".reward", reward);
        setObjVar(store, root + ".created", getGameTime());
        if (isIdValid(terminal))
        {
            setObjVar(store, root + ".terminal", terminal);
        }
        String template = getTemplateName(item);
        if (template != null && template.length() > 0)
        {
            setObjVar(store, root + ".template", template);
        }
        String staticName = static_item.getStaticItemName(item);
        if (staticName != null && staticName.length() > 0)
        {
            setObjVar(store, root + ".staticName", staticName);
        }
        if (isCrafted(item))
        {
            setObjVar(store, root + ".crafted", 1);
            obj_id crafter = getCrafter(item);
            if (isIdValid(crafter))
            {
                setObjVar(store, root + ".crafter", crafter);
            }
        }
        else
        {
            setObjVar(store, root + ".crafted", 0);
        }
        CustomerServiceLog("bounty", "%TU posted item bounty id " + id + " on item %TT for " + reward + " credits", creator, item);
        return true;
    }
    public static int[] getOpenItemBountyIds(int limit) throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store))
        {
            return new int[0];
        }
        int next = hasObjVar(store, ITEM_BOUNTY_ROOT + ".nextId") ? getIntObjVar(store, ITEM_BOUNTY_ROOT + ".nextId") : 1;
        Vector out = new Vector();
        out.setSize(0);
        for (int i = 1; i < next; ++i)
        {
            String root = ITEM_BOUNTY_ROOT + "." + i;
            if (!hasObjVar(store, root + ".state"))
            {
                continue;
            }
            String state = getStringObjVar(store, root + ".state");
            if (!ITEM_BOUNTY_STATE_OPEN.equals(state))
            {
                continue;
            }
            out = utils.addElement(out, i);
            if (limit > 0 && out.size() >= limit)
            {
                break;
            }
        }
        int[] ids = new int[out.size()];
        for (int j = 0; j < out.size(); ++j)
        {
            ids[j] = ((Integer)out.get(j));
        }
        return ids;
    }
    public static int getNextOpenItemBountyId(int afterId) throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store))
        {
            return 0;
        }
        int next = hasObjVar(store, ITEM_BOUNTY_ROOT + ".nextId") ? getIntObjVar(store, ITEM_BOUNTY_ROOT + ".nextId") : 1;
        if (next <= 1)
        {
            return 0;
        }
        int start = afterId + 1;
        if (start < 1 || start >= next)
        {
            start = 1;
        }
        for (int pass = 0; pass < 2; ++pass)
        {
            int begin = (pass == 0) ? start : 1;
            int end = (pass == 0) ? next - 1 : start - 1;
            for (int i = begin; i <= end; ++i)
            {
                String root = ITEM_BOUNTY_ROOT + "." + i;
                if (!hasObjVar(store, root + ".state"))
                {
                    continue;
                }
                if (ITEM_BOUNTY_STATE_OPEN.equals(getStringObjVar(store, root + ".state")))
                {
                    return i;
                }
            }
        }
        return 0;
    }
    public static String getItemBountyBoardLine(int bountyId) throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store))
        {
            return "Unavailable bounty";
        }
        String root = ITEM_BOUNTY_ROOT + "." + bountyId;
        if (!hasObjVar(store, root + ".state"))
        {
            return "Unavailable bounty";
        }
        int reward = hasObjVar(store, root + ".reward") ? getIntObjVar(store, root + ".reward") : 0;
        String profile = (hasObjVar(store, root + ".crafted") && getIntObjVar(store, root + ".crafted") == 1) ? "Crafted profile" : "Static profile";
        return "[ID " + bountyId + "] " + profile + " - Reward " + reward + " cr";
    }
    public static obj_id createItemBountyMissionForHunter(obj_id hunter, obj_id broker, int bountyId) throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store) || !isIdValid(hunter))
        {
            return obj_id.NULL_ID;
        }
        String root = ITEM_BOUNTY_ROOT + "." + bountyId;
        if (!hasObjVar(store, root + ".state") || !ITEM_BOUNTY_STATE_OPEN.equals(getStringObjVar(store, root + ".state")))
        {
            return obj_id.NULL_ID;
        }
        obj_id item = getObjIdObjVar(store, root + ".item");
        if (!isIdValid(item))
        {
            return obj_id.NULL_ID;
        }
        obj_id missionData = createMissionObjectInCreatureMissionBag(hunter);
        if (!isIdValid(missionData))
        {
            return obj_id.NULL_ID;
        }
        setMissionType(missionData, "bounty");
        setMissionRootScriptName(missionData, "systems.missions.dynamic.mission_bounty_item");
        setMissionStartLocation(missionData, getLocation(hunter));
        if (isIdValid(broker))
        {
            setMissionEndLocation(missionData, getLocation(broker));
            setObjVar(missionData, "bh.itemBountyBroker", broker);
        }
        int reward = hasObjVar(store, root + ".reward") ? getIntObjVar(store, root + ".reward") : 0;
        setMissionReward(missionData, reward);
        if (hasObjVar(store, root + ".template"))
        {
            setMissionTargetAppearance(missionData, getStringObjVar(store, root + ".template"));
        }
        setObjVar(missionData, "bh.itemBountyId", bountyId);
        setObjVar(missionData, "bh.itemBountyTarget", item);
        setObjVar(missionData, "bh.itemBountyCreator", getObjIdObjVar(store, root + ".creator"));
        setObjVar(missionData, "bh.itemBountyClueStage", 0);
        assignMission(missionData, hunter);
        setObjVar(store, root + ".state", ITEM_BOUNTY_STATE_ACCEPTED);
        setObjVar(store, root + ".hunter", hunter);
        setObjVar(store, root + ".accepted", getGameTime());
        obj_id creator = hasObjVar(store, root + ".creator") ? getObjIdObjVar(store, root + ".creator") : obj_id.NULL_ID;
        if (isIdValid(creator) && exists(creator))
        {
            sendSystemMessage(creator, "Item bounty #" + bountyId + " has been accepted by a hunter.", null);
        }
        CustomerServiceLog("bounty", "%TU accepted item bounty id " + bountyId, hunter);
        return missionData;
    }
    public static obj_id createNextItemBountyMissionForHunter(obj_id hunter, obj_id broker, int afterId) throws InterruptedException
    {
        int id = getNextOpenItemBountyId(afterId);
        if (id <= 0)
        {
            return obj_id.NULL_ID;
        }
        return createItemBountyMissionForHunter(hunter, broker, id);
    }
    public static obj_id getItemBountyMission(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return obj_id.NULL_ID;
        }
        obj_id[] missionList = getMissionObjects(player);
        if (missionList == null || missionList.length < 1)
        {
            return obj_id.NULL_ID;
        }
        for (obj_id mission : missionList)
        {
            if (hasObjVar(mission, "bh.itemBountyId"))
            {
                return mission;
            }
        }
        return obj_id.NULL_ID;
    }
    public static boolean turnInItemBounty(obj_id hunter, obj_id broker) throws InterruptedException
    {
        obj_id mission = getItemBountyMission(hunter);
        if (!isIdValid(mission))
        {
            sendSystemMessage(hunter, "You do not have an active item bounty mission.", null);
            return false;
        }
        obj_id item = getObjIdObjVar(mission, "bh.itemBountyTarget");
        if (!isIdValid(item) || !pclib.isContainedByPlayer(hunter, item))
        {
            sendSystemMessage(hunter, "You do not currently possess the required item.", null);
            return false;
        }
        obj_id brokerInv = utils.getInventoryContainer(broker);
        if (!isIdValid(brokerInv))
        {
            sendSystemMessage(hunter, "Broker cannot accept turn-in right now.", null);
            return false;
        }
        if (!putIn(item, brokerInv, hunter))
        {
            if (!putIn(item, brokerInv))
            {
                sendSystemMessage(hunter, "Turn-in failed. Broker storage is unavailable.", null);
                return false;
            }
        }
        int bountyId = getIntObjVar(mission, "bh.itemBountyId");
        obj_id store = getItemBountyStore();
        String root = ITEM_BOUNTY_ROOT + "." + bountyId;
        setObjVar(store, root + ".state", ITEM_BOUNTY_STATE_READY);
        setObjVar(store, root + ".broker", broker);
        setObjVar(store, root + ".storedItem", item);
        setObjVar(store, root + ".turnedIn", getGameTime());
        obj_id creator = hasObjVar(store, root + ".creator") ? getObjIdObjVar(store, root + ".creator") : obj_id.NULL_ID;
        if (isIdValid(creator))
        {
            if (exists(creator))
            {
                sendSystemMessage(creator, "Your item bounty #" + bountyId + " is ready for pickup at a bounty broker.", null);
            }
            setObjVar(creator, "bh.itemBounty.pendingPickup." + bountyId, 1);
        }
        int reward = hasObjVar(store, root + ".reward") ? getIntObjVar(store, root + ".reward") : 0;
        money.bankTo(money.ACCT_BOUNTY, hunter, reward);
        sendSystemMessage(hunter, "Item bounty completed and reward paid.", null);
        endMission(mission);
        return true;
    }
    public static int[] getReadyItemBountiesForCreator(obj_id creator, obj_id broker) throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store) || !isIdValid(creator))
        {
            return new int[0];
        }
        int next = hasObjVar(store, ITEM_BOUNTY_ROOT + ".nextId") ? getIntObjVar(store, ITEM_BOUNTY_ROOT + ".nextId") : 1;
        Vector out = new Vector();
        out.setSize(0);
        for (int i = 1; i < next; ++i)
        {
            String root = ITEM_BOUNTY_ROOT + "." + i;
            if (!hasObjVar(store, root + ".state") || !ITEM_BOUNTY_STATE_READY.equals(getStringObjVar(store, root + ".state")))
            {
                continue;
            }
            obj_id owner = hasObjVar(store, root + ".creator") ? getObjIdObjVar(store, root + ".creator") : obj_id.NULL_ID;
            if (owner != creator)
            {
                continue;
            }
            if (isIdValid(broker) && hasObjVar(store, root + ".broker"))
            {
                if (getObjIdObjVar(store, root + ".broker") != broker)
                {
                    continue;
                }
            }
            out = utils.addElement(out, i);
        }
        int[] ids = new int[out.size()];
        for (int j = 0; j < out.size(); ++j)
        {
            ids[j] = ((Integer)out.get(j));
        }
        return ids;
    }
    public static boolean pickupItemBounty(obj_id creator, obj_id broker, int bountyId) throws InterruptedException
    {
        obj_id store = getItemBountyStore();
        if (!isIdValid(store) || !isIdValid(creator))
        {
            return false;
        }
        String root = ITEM_BOUNTY_ROOT + "." + bountyId;
        if (!hasObjVar(store, root + ".state") || !ITEM_BOUNTY_STATE_READY.equals(getStringObjVar(store, root + ".state")))
        {
            return false;
        }
        obj_id owner = hasObjVar(store, root + ".creator") ? getObjIdObjVar(store, root + ".creator") : obj_id.NULL_ID;
        if (owner != creator)
        {
            return false;
        }
        if (isIdValid(broker) && hasObjVar(store, root + ".broker"))
        {
            if (getObjIdObjVar(store, root + ".broker") != broker)
            {
                return false;
            }
        }
        obj_id item = hasObjVar(store, root + ".storedItem") ? getObjIdObjVar(store, root + ".storedItem") : obj_id.NULL_ID;
        if (!isIdValid(item))
        {
            return false;
        }
        obj_id inv = utils.getInventoryContainer(creator);
        if (!isIdValid(inv))
        {
            return false;
        }
        if (!putIn(item, inv, creator))
        {
            if (!putIn(item, inv))
            {
                return false;
            }
        }
        setObjVar(store, root + ".state", ITEM_BOUNTY_STATE_PICKED_UP);
        setObjVar(store, root + ".pickedUp", getGameTime());
        if (hasObjVar(creator, "bh.itemBounty.pendingPickup." + bountyId))
        {
            removeObjVar(creator, "bh.itemBounty.pendingPickup." + bountyId);
        }
        sendSystemMessage(creator, "Recovered item bounty #" + bountyId + " picked up.", null);
        return true;
    }
    public static void initializeInvestigationState(obj_id mission) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return;
        }
        if (!hasObjVar(mission, OBJVAR_INVEST_STAGE))
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_COLD);
        }
        if (!hasObjVar(mission, OBJVAR_INVEST_CONFIDENCE))
        {
            setObjVar(mission, OBJVAR_INVEST_CONFIDENCE, 0.20f);
        }
        setObjVar(mission, OBJVAR_INVEST_LAST_UPDATE, getGameTime());
        if (!hasObjVar(mission, OBJVAR_INVEST_LAST_CLUE))
        {
            setObjVar(mission, OBJVAR_INVEST_LAST_CLUE, "init");
        }
        initializeMissionContractType(mission);
    }
    public static void applyInvestigationDecay(obj_id mission) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return;
        }
        initializeInvestigationState(mission);
        int now = getGameTime();
        int lastUpdate = getIntObjVar(mission, OBJVAR_INVEST_LAST_UPDATE);
        if (lastUpdate <= 0 || now <= lastUpdate)
        {
            setObjVar(mission, OBJVAR_INVEST_LAST_UPDATE, now);
            return;
        }
        float current = getFloatObjVar(mission, OBJVAR_INVEST_CONFIDENCE);
        float delta = (now - lastUpdate) * INVESTIGATION_DECAY_PER_SECOND;
        float next = clamp(current - delta, 0.0f, 1.0f);
        setObjVar(mission, OBJVAR_INVEST_CONFIDENCE, next);
        setObjVar(mission, OBJVAR_INVEST_LAST_UPDATE, now);
        if (next < INVESTIGATION_COLD_THRESHOLD)
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_COLD);
        }
        else if (next < INVESTIGATION_WARM_THRESHOLD)
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_WARM);
        }
        else
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_CONFIRMED);
        }
    }
    public static float getInvestigationConfidence(obj_id mission) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return 0.0f;
        }
        applyInvestigationDecay(mission);
        return hasObjVar(mission, OBJVAR_INVEST_CONFIDENCE) ? getFloatObjVar(mission, OBJVAR_INVEST_CONFIDENCE) : 0.0f;
    }
    public static int getInvestigationStage(obj_id mission) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return INVESTIGATION_STAGE_COLD;
        }
        applyInvestigationDecay(mission);
        return hasObjVar(mission, OBJVAR_INVEST_STAGE) ? getIntObjVar(mission, OBJVAR_INVEST_STAGE) : INVESTIGATION_STAGE_COLD;
    }
    public static float getClueDeltaForType(String clueType) throws InterruptedException
    {
        if (clueType == null || clueType.length() < 1)
        {
            return 0.10f;
        }
        if (clueType.equals("informant"))
        {
            return 0.18f;
        }
        if (clueType.equals("probe"))
        {
            return 0.25f;
        }
        if (clueType.equals("seeker"))
        {
            return 0.22f;
        }
        if (clueType.equals("combat"))
        {
            return 0.14f;
        }
        if (clueType.equals("capture"))
        {
            return 0.35f;
        }
        if (clueType.equals("failed_track"))
        {
            return -0.10f;
        }
        return 0.10f;
    }
    public static float advanceInvestigationStage(obj_id mission, String clueType) throws InterruptedException
    {
        return advanceInvestigationStage(mission, clueType, 0.0f);
    }
    public static float advanceInvestigationStage(obj_id mission, String clueType, float bonus) throws InterruptedException
    {
        if (!isIdValid(mission))
        {
            return 0.0f;
        }
        applyInvestigationDecay(mission);
        float current = hasObjVar(mission, OBJVAR_INVEST_CONFIDENCE) ? getFloatObjVar(mission, OBJVAR_INVEST_CONFIDENCE) : 0.0f;
        float next = current + getClueDeltaForType(clueType) + bonus;
        next = clamp(next, 0.0f, 1.0f);
        setObjVar(mission, OBJVAR_INVEST_CONFIDENCE, next);
        setObjVar(mission, OBJVAR_INVEST_LAST_UPDATE, getGameTime());
        if (clueType != null && clueType.length() > 0)
        {
            setObjVar(mission, OBJVAR_INVEST_LAST_CLUE, clueType);
        }
        if (next < INVESTIGATION_COLD_THRESHOLD)
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_COLD);
        }
        else if (next < INVESTIGATION_WARM_THRESHOLD)
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_WARM);
        }
        else
        {
            setObjVar(mission, OBJVAR_INVEST_STAGE, INVESTIGATION_STAGE_CONFIRMED);
        }
        return next;
    }
    public static String getTrailBandLabel(obj_id mission) throws InterruptedException
    {
        int stage = getInvestigationStage(mission);
        if (stage <= INVESTIGATION_STAGE_COLD)
        {
            return "COLD";
        }
        if (stage == INVESTIGATION_STAGE_WARM)
        {
            return "WARM";
        }
        return "CONFIRMED";
    }
    public static void sendTrailBandMessage(obj_id player, obj_id mission) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return;
        }
        String label = getTrailBandLabel(mission);
        float confidence = getInvestigationConfidence(mission) * 100.0f;
        sendSystemMessage(player, "[BH Intel] " + label + " trail (" + (int)confidence + "% confidence).", null);
    }
    public static location adjustTrackingLocationForIntel(obj_id mission, obj_id hunter, obj_id target, location resolvedLocation, int droidType) throws InterruptedException
    {
        if (resolvedLocation == null)
        {
            return null;
        }
        location adjusted = (location)resolvedLocation.clone();
        adjusted.cell = obj_id.NULL_ID;
        int stage = getInvestigationStage(mission);
        if (isIdValid(target) && isTrackerJammed(target))
        {
            advanceInvestigationStage(mission, "failed_track", -0.08f);
            sendSystemMessage(hunter, "[BH Intel] Tracking signal is being jammed.", null);
            stage = getInvestigationStage(mission);
        }
        if (isIdValid(target) && hasDecoySignal(target))
        {
            float strength = hasObjVar(target, OBJVAR_COUNTERPLAY_DECOY_STRENGTH) ? getFloatObjVar(target, OBJVAR_COUNTERPLAY_DECOY_STRENGTH) : 1.0f;
            int decoyScatter = (int)(1800.0f * strength);
            adjusted.x = adjusted.x + rand(-decoyScatter, decoyScatter);
            adjusted.z = adjusted.z + rand(-decoyScatter, decoyScatter);
            sendSystemMessage(hunter, "[BH Intel] Decoy echoes detected in this sector.", null);
            return adjusted;
        }
        if (stage == INVESTIGATION_STAGE_COLD)
        {
            int scatter = (droidType == DROID_SEEKER) ? 2200 : 3200;
            adjusted.x = adjusted.x + rand(-scatter, scatter);
            adjusted.z = adjusted.z + rand(-scatter, scatter);
        }
        else if (stage == INVESTIGATION_STAGE_WARM)
        {
            int scatter = (droidType == DROID_SEEKER) ? 850 : 1250;
            adjusted.x = adjusted.x + rand(-scatter, scatter);
            adjusted.z = adjusted.z + rand(-scatter, scatter);
        }
        return adjusted;
    }
    public static boolean requestRemoteProbeEngagement(obj_id mission, obj_id hunter, obj_id target, int droidType, String hunterPlanet) throws InterruptedException
    {
        if (!isIdValid(mission) || !isIdValid(hunter) || !isIdValid(target) || !isPlayer(target))
        {
            return false;
        }
        if (droidType != DROID_PROBOT && droidType != DROID_SEEKER)
        {
            return false;
        }
        location targetLocation = getLocation(target);
        if (targetLocation == null)
        {
            obj_id topmost = getTopMostContainer(target);
            if (isIdValid(topmost))
            {
                targetLocation = getLocation(topmost);
            }
        }
        if (targetLocation == null)
        {
            return false;
        }
        if (droidType == DROID_SEEKER)
        {
            if (hunterPlanet == null || hunterPlanet.length() < 1 || !targetLocation.area.equals(hunterPlanet))
            {
                return false;
            }
        }
        int now = getGameTime();
        String throttlePath = OBJVAR_REMOTE_PROBE_NEXT_DISPATCH + "." + droidType;
        if (hasObjVar(mission, throttlePath))
        {
            int next = getIntObjVar(mission, throttlePath);
            if (next > now)
            {
                return false;
            }
        }
        int cooldown = (droidType == DROID_SEEKER) ? 25 : 45;
        setObjVar(mission, throttlePath, now + cooldown);
        dictionary params = new dictionary();
        params.put("hunter", hunter);
        params.put("mission", mission);
        params.put("droidType", droidType);
        params.put("hunterPlanet", hunterPlanet);
        params.put("targetSnapshot", targetLocation);
        messageTo(target, "handleBountyProbeEngagement", params, 0.0f, true);
        return true;
    }
    public static void applyTrackerJam(obj_id target, int durationSeconds) throws InterruptedException
    {
        if (!isIdValid(target))
        {
            return;
        }
        int duration = durationSeconds;
        if (duration < 10)
        {
            duration = 10;
        }
        setObjVar(target, OBJVAR_COUNTERPLAY_JAM_UNTIL, getGameTime() + duration);
    }
    public static void applyDecoySignal(obj_id target, int durationSeconds) throws InterruptedException
    {
        if (!isIdValid(target))
        {
            return;
        }
        int duration = durationSeconds;
        if (duration < 10)
        {
            duration = 10;
        }
        setObjVar(target, OBJVAR_COUNTERPLAY_DECOY_UNTIL, getGameTime() + duration);
        setObjVar(target, OBJVAR_COUNTERPLAY_DECOY_STRENGTH, 1.0f + (rand(0, 40) / 100.0f));
    }
    public static boolean isTrackerJammed(obj_id target) throws InterruptedException
    {
        if (!isIdValid(target) || !hasObjVar(target, OBJVAR_COUNTERPLAY_JAM_UNTIL))
        {
            return false;
        }
        int now = getGameTime();
        int until = getIntObjVar(target, OBJVAR_COUNTERPLAY_JAM_UNTIL);
        if (until <= now)
        {
            removeObjVar(target, OBJVAR_COUNTERPLAY_JAM_UNTIL);
            return false;
        }
        return true;
    }
    public static boolean hasDecoySignal(obj_id target) throws InterruptedException
    {
        if (!isIdValid(target) || !hasObjVar(target, OBJVAR_COUNTERPLAY_DECOY_UNTIL))
        {
            return false;
        }
        int now = getGameTime();
        int until = getIntObjVar(target, OBJVAR_COUNTERPLAY_DECOY_UNTIL);
        if (until <= now)
        {
            removeObjVar(target, OBJVAR_COUNTERPLAY_DECOY_UNTIL);
            if (hasObjVar(target, OBJVAR_COUNTERPLAY_DECOY_STRENGTH))
            {
                removeObjVar(target, OBJVAR_COUNTERPLAY_DECOY_STRENGTH);
            }
            return false;
        }
        return true;
    }
    public static boolean canPostPlayerBounty(obj_id issuer, obj_id target, int amount, boolean sendMessages) throws InterruptedException
    {
        if (!isIdValid(issuer) || !isIdValid(target))
        {
            return false;
        }
        if (issuer == target)
        {
            if (sendMessages)
            {
                sendSystemMessage(issuer, "You cannot post a bounty on yourself.", null);
            }
            return false;
        }
        if (amount < MIN_BOUNTY_SET)
        {
            if (sendMessages)
            {
                sendSystemMessage(issuer, "Bounty amount below minimum.", null);
            }
            return false;
        }
        if (getLevel(issuer) < 20 || getLevel(target) < 20)
        {
            if (sendMessages)
            {
                sendSystemMessage(issuer, "Both participants must be level 20+ for player bounties.", null);
            }
            return false;
        }
        if (hasObjVar(issuer, OBJVAR_ANTIGRIEF_LAST_POST_TIME))
        {
            int nextPost = getIntObjVar(issuer, OBJVAR_ANTIGRIEF_LAST_POST_TIME);
            if (nextPost > getGameTime())
            {
                if (sendMessages)
                {
                    sendSystemMessage(issuer, "You recently posted a bounty. Please wait before posting another.", null);
                }
                return false;
            }
        }
        return true;
    }
    public static void markPlayerBountyPost(obj_id issuer) throws InterruptedException
    {
        if (!isIdValid(issuer))
        {
            return;
        }
        setObjVar(issuer, OBJVAR_ANTIGRIEF_LAST_POST_TIME, getGameTime() + ANTIGRIEF_POST_COOLDOWN);
    }
    public static boolean canRewardHunterForTarget(obj_id hunter, obj_id target) throws InterruptedException
    {
        if (!isIdValid(hunter) || !isIdValid(target))
        {
            return false;
        }
        if (hasObjVar(hunter, OBJVAR_ANTIGRIEF_LAST_TARGET) && hasObjVar(hunter, OBJVAR_ANTIGRIEF_LAST_REWARD_TIME))
        {
            obj_id lastTarget = getObjIdObjVar(hunter, OBJVAR_ANTIGRIEF_LAST_TARGET);
            int lastRewardTime = getIntObjVar(hunter, OBJVAR_ANTIGRIEF_LAST_REWARD_TIME);
            if (lastTarget == target && ((getGameTime() - lastRewardTime) < ANTIGRIEF_REWARD_COOLDOWN))
            {
                return false;
            }
        }
        return true;
    }
    public static void recordHunterReward(obj_id hunter, obj_id target) throws InterruptedException
    {
        if (!isIdValid(hunter) || !isIdValid(target))
        {
            return;
        }
        setObjVar(hunter, OBJVAR_ANTIGRIEF_LAST_TARGET, target);
        setObjVar(hunter, OBJVAR_ANTIGRIEF_LAST_REWARD_TIME, getGameTime());
    }
    public static int getRenownRankForPoints(int points) throws InterruptedException
    {
        if (points >= 5000)
        {
            return 6;
        }
        if (points >= 3000)
        {
            return 5;
        }
        if (points >= 1800)
        {
            return 4;
        }
        if (points >= 1000)
        {
            return 3;
        }
        if (points >= 450)
        {
            return 2;
        }
        if (points >= 150)
        {
            return 1;
        }
        return 0;
    }
    public static int getRenown(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !hasObjVar(player, OBJVAR_RENOWN_POINTS))
        {
            return 0;
        }
        return getIntObjVar(player, OBJVAR_RENOWN_POINTS);
    }
    public static int getRenownRank(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return 0;
        }
        int points = getRenown(player);
        int rank = getRenownRankForPoints(points);
        setObjVar(player, OBJVAR_RENOWN_RANK, rank);
        return rank;
    }
    public static void addRenown(obj_id player, String reason, int amount) throws InterruptedException
    {
        if (!isIdValid(player) || amount == 0)
        {
            return;
        }
        int points = getRenown(player);
        points += amount;
        if (points < 0)
        {
            points = 0;
        }
        setObjVar(player, OBJVAR_RENOWN_POINTS, points);
        int oldRank = hasObjVar(player, OBJVAR_RENOWN_RANK) ? getIntObjVar(player, OBJVAR_RENOWN_RANK) : 0;
        int newRank = getRenownRankForPoints(points);
        setObjVar(player, OBJVAR_RENOWN_RANK, newRank);
        if (newRank > oldRank)
        {
            sendSystemMessage(player, "[BH Renown] Rank increased to " + newRank + ".", null);
        }
    }
    public static float getRenownPayoutModifier(obj_id player) throws InterruptedException
    {
        int rank = getRenownRank(player);
        float modifier = 1.0f + (rank * 0.04f);
        if (modifier > 1.30f)
        {
            modifier = 1.30f;
        }
        return modifier;
    }
    public static String normalizeHeatRegion(String region) throws InterruptedException
    {
        if (region == null || region.length() < 1)
        {
            return "unknown";
        }
        return toLower(region).replace('-', '_').replace(' ', '_');
    }
    public static obj_id getHeatStoreObject() throws InterruptedException
    {
        return getPlanetByName("tatooine");
    }
    public static float getRegionalHeat(String region) throws InterruptedException
    {
        obj_id store = getHeatStoreObject();
        if (!isIdValid(store))
        {
            return 0.0f;
        }
        String key = normalizeHeatRegion(region);
        String valuePath = HEAT_ROOT + "." + key + ".value";
        String lastPath = HEAT_ROOT + "." + key + ".last";
        if (!hasObjVar(store, valuePath))
        {
            return 0.0f;
        }
        float value = getFloatObjVar(store, valuePath);
        int now = getGameTime();
        int last = hasObjVar(store, lastPath) ? getIntObjVar(store, lastPath) : now;
        if (now > last)
        {
            value = value - ((now - last) * HEAT_DECAY_PER_SECOND);
            value = clamp(value, HEAT_MIN, HEAT_MAX);
            setObjVar(store, valuePath, value);
            setObjVar(store, lastPath, now);
        }
        return value;
    }
    public static float adjustRegionalHeat(String region, float delta) throws InterruptedException
    {
        obj_id store = getHeatStoreObject();
        if (!isIdValid(store))
        {
            return 0.0f;
        }
        String key = normalizeHeatRegion(region);
        String valuePath = HEAT_ROOT + "." + key + ".value";
        String lastPath = HEAT_ROOT + "." + key + ".last";
        float value = getRegionalHeat(region);
        value = clamp(value + delta, HEAT_MIN, HEAT_MAX);
        setObjVar(store, valuePath, value);
        setObjVar(store, lastPath, getGameTime());
        return value;
    }
    public static float getRegionalHeatAtLocation(location loc) throws InterruptedException
    {
        if (loc == null)
        {
            return 0.0f;
        }
        return getRegionalHeat(loc.area);
    }
    public static int getHeatPayoutBonusPercent(location loc) throws InterruptedException
    {
        float heat = getRegionalHeatAtLocation(loc);
        if (heat < 10.0f)
        {
            return 0;
        }
        int pct = (int)(heat / 10.0f) * 2;
        if (pct > 20)
        {
            pct = 20;
        }
        return pct;
    }
    public static obj_id[] getEligibleGroupParticipants(obj_id hunter, obj_id target, float maxRange) throws InterruptedException
    {
        Vector participants = new Vector();
        participants.setSize(0);
        if (!isIdValid(hunter))
        {
            return new obj_id[0];
        }
        obj_id groupId = getGroupObject(hunter);
        if (!isIdValid(groupId))
        {
            obj_id[] solo = new obj_id[1];
            solo[0] = hunter;
            return solo;
        }
        obj_id[] members = getGroupMemberIds(groupId);
        if (members == null || members.length < 1)
        {
            obj_id[] solo = new obj_id[1];
            solo[0] = hunter;
            return solo;
        }
        for (obj_id member : members)
        {
            if (!isIdValid(member) || !isPlayer(member) || isDead(member))
            {
                continue;
            }
            if (getDistance(member, hunter) > maxRange)
            {
                continue;
            }
            if (isIdValid(target) && isPlayer(target))
            {
                if (!isBeingHuntedByBountyHunter(target, member))
                {
                    continue;
                }
            }
            participants = utils.addElement(participants, member);
        }
        if (participants.size() < 1)
        {
            participants = utils.addElement(participants, hunter);
        }
        obj_id[] out = new obj_id[participants.size()];
        for (int i = 0; i < participants.size(); i++)
        {
            out[i] = ((obj_id)participants.get(i));
        }
        return out;
    }
    public static boolean isSpammingBountyCheck(obj_id player, boolean sayProse) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return false;
        }
        if (utils.hasScriptVar(player, "bountyCheckFloodControl"))
        {
            int storedTime = utils.getIntScriptVar(player, "bountyCheckFloodControl");
            int currentTime = getGameTime();
            if (storedTime > currentTime)
            {
                int timeDelta = storedTime - currentTime;
                String timeScale = "seconds";
                if (sayProse)
                {
                    if (timeDelta / 60 > 1)
                    {
                        timeDelta = (int)timeDelta / 60;
                        timeScale = "minutes";
                        String talkingCharacter = TALKING_COMM_CHARACTER[(rand(0, TALKING_COMM_CHARACTER.length - 1))];
                        prose_package pp = new prose_package();
                        pp.stringId = PROSE_NO_BOUNTY_MINUTE;
                        pp.digitInteger = timeDelta;
                        commPlayer(player, player, pp, talkingCharacter);
                    }
                    else 
                    {
                        sendSystemMessage(player, PROSE_NO_BOUNTY_SECONDS);
                    }
                }
                return true;
            }
        }
        return false;
    }
    public static boolean canCheckForBounty(obj_id player, obj_id target) throws InterruptedException
    {
        if (isSpammingBountyCheck(player, true))
        {
            return false;
        }
        if (!isIdValid(target))
        {
            debugLogging("//// canCheckForBounty: ", "////>>>> target invalid");
            return false;
        }
        if (target == player)
        {
            debugLogging("//// canCheckForBounty: ", "////>>>> target is yourself");
            return false;
        }
        if (isDead(target) || isIncapacitated(target))
        {
            debugLogging("//// canCheckForBounty: ", "////>>>> target is is dead, or incapped, or not an npc");
            return false;
        }
        obj_id container = getContainedBy(target);
        if (isPlayer(container))
        {
            debugLogging("//// canCheckForBounty: ", "////>>>> target is is in a container");
            return false;
        }
        if (utils.hasScriptVar(target, "noBountyCheck"))
        {
            sendSystemMessage(player, NO_BOUNTY_TARGET);
            debugLogging("//// canCheckForBounty: ", "////>>>> target has already been checked for bounty");
            return false;
        }
        if (isPlayer(target))
        {
            sendSystemMessage(player, NO_BOUNTY_PLAYER);
            debugLogging("//// canCheckForBounty: ", "////>>>> not an npc... !isNpc");
            return false;
        }
        else 
        {
            if (!ai_lib.isNpc(target))
            {
                sendSystemMessage(player, NO_BOUNTY_TARGET);
                debugLogging("//// canCheckForBounty: ", "////>>>> not an npc... !isNpc");
                return false;
            }
            String targetName = getCreatureName(target);
            if (utils.dataTableGetInt(CREATURE_TABLE, targetName, "difficultyClass") != 0)
            {
                sendSystemMessage(player, NO_BOUNTY_TARGET);
                debugLogging("//// canCheckForBounty: ", "////>>>> ou can't attack that... elite or boss");
                return false;
            }
            if (!pvpCanAttack(player, target))
            {
                sendSystemMessage(player, NO_BOUNTY_TARGET);
                debugLogging("//// canCheckForBounty: ", "////>>>> ou can't attack that... !pvpCanAttack");
                return false;
            }
            if (utils.hasScriptVar(target, "bountyCheck"))
            {
                obj_id sameBountyHunter = utils.getObjIdScriptVar(target, "bountyCheck");
                if (sameBountyHunter == player)
                {
                    sendSystemMessage(player, BOUNTY_ALREADY);
                    debugLogging("//// canCheckForBounty: ", "////>>>> target already has a bounty");
                    return false;
                }
            }
            if (hasObjVar(target, "noBountyCheck"))
            {
                sendSystemMessage(player, NO_BOUNTY_TARGET);
                debugLogging("//// canCheckForBounty: ", "////>>>> target already has a bounty");
                return false;
            }
            int targetLevel = getLevel(target);
            if (targetLevel < 70)
            {
                sendSystemMessage(player, BOUNTY_TOO_LOW_LEVEL);
                debugLogging("//// canCheckForBounty: ", "////>>>> target is too easy to give you a bounty on");
                return false;
            }
        }
        return true;
    }
    public static boolean checkForPresenceOfBounty(obj_id player) throws InterruptedException
    {
        return checkForPresenceOfBounty(player, obj_id.NULL_ID);
    }
    public static boolean checkForPresenceOfBounty(obj_id player, obj_id target) throws InterruptedException
    {
        debugLogging("//// checkForPresenceOfBounty: ", "////>>>> entered");
        if (isIdValid(target) && isPlayer(target))
        {
            sendSystemMessage(player, NO_BOUNTY_TARGET);
        }
        else 
        {
            if (!isGod(player))
            {
                if (11 > rand(1, 100))
                {
                    sendSystemMessage(player, FUGITIVE);
                    return true;
                }
                sendSystemMessage(player, NO_BOUNTY_TARGET);
                return false;
            }
            debugSpeakMsg(player, "Bypassing random check due to God mode");
            sendSystemMessage(player, FUGITIVE);
            return true;
        }
        return false;
    }
    public static void spawnFugitive(obj_id player, location spawnLoc) throws InterruptedException
    {
        String[] fugitive = dataTableGetStringColumnNoDefaults(BOUNTY_CHECK_TABLE, "fugitive");
        String mob = new String();
        if (dataTableHasColumn(BOUNTY_CHECK_TABLE, "chance"))
        {
            int[] weights = dataTableGetIntColumnNoDefaults(BOUNTY_CHECK_TABLE, "chance");
            if (weights != null && weights.length > 0)
            {
                int total = 0;
                for (int weight : weights) {
                    total += weight;
                }
                if (total < 1)
                {
                    total = 1;
                }
                int roll = rand(1, total);
                int idx = -1;
                int low_range = 0;
                for (int j = 0; j < weights.length; j++)
                {
                    int high_range = low_range + weights[j];
                    if (roll > low_range && roll <= high_range)
                    {
                        idx = j;
                        break;
                    }
                    low_range = high_range;
                }
                if (idx >= 0)
                {
                    if (idx >= fugitive.length)
                    {
                        idx = fugitive.length - 1;
                    }
                    mob = fugitive[idx];
                    obj_id spawnedFugitive = create.object(mob, spawnLoc);
                    createFugitive(player, spawnedFugitive);
                }
                else 
                {
                    return;
                }
            }
            else 
            {
                int pick = rand(0, fugitive.length - 1);
                mob = fugitive[pick];
                obj_id spawnedFugitive = create.object(mob, spawnLoc);
                createFugitive(player, spawnedFugitive);
            }
        }
        else 
        {
            int pick = rand(0, fugitive.length - 1);
            mob = fugitive[pick];
            obj_id spawnedFugitive = create.object(mob, spawnLoc);
            createFugitive(player, spawnedFugitive);
        }
        return;
    }
    public static void createFugitive(obj_id player, obj_id spawnedFugitive) throws InterruptedException
    {
        playMusic(player, spawnedFugitive, "sound/mus_duel_of_the_fates_lcv.snd", 0, false);
        pvpSetAlignedFaction(spawnedFugitive, (84709322));
        pvpSetPermanentPersonalEnemyFlag(spawnedFugitive, player);
        pvpSetPermanentPersonalEnemyFlag(player, spawnedFugitive);
        startCombat(spawnedFugitive, player);
        addHate(spawnedFugitive, player, 1000.0f);
        setObjVar(spawnedFugitive, "soloCollection", player);
        return;
    }
    public static void payBountyCheckReward(obj_id killer, obj_id target) throws InterruptedException
    {
        int payBracket = getIntObjVar(target, "bountyCheckPayBracket");
        sendSystemMessage(killer, BOUNTY_CHECK_PAYMENT);
        if (payBracket == 0)
        {
            CustomerServiceLog("bounty", "Something is wrong. " + killer + " killed an npc that should have been a Bounty Check target but the NPC ( " + target + " ) did not have the obj var for payout set up correctly");
        }
        if (payBracket == 1)
        {
            money.bankTo(money.ACCT_BOUNTY, killer, 15000);
        }
        if (payBracket == 2)
        {
            money.bankTo(money.ACCT_BOUNTY, killer, 25000);
        }
        return;
    }
    public static boolean initiatePlayerBountyCollection(obj_id player, obj_id target, int amount) throws InterruptedException
    {
        return true;
    }
    public static boolean offerCommandCheckBounty(obj_id player, obj_id target, int amount) throws InterruptedException
    {
        return true;
    }
    public static void awardBounty(obj_id player, String creatureName, int amount) throws InterruptedException
    {
        obj_id storedTarget = utils.getObjIdScriptVar(player, "currentBounty");
        dictionary params = new dictionary();
        params.put("amount", amount);
        params.put("creatureName", getEncodedName(storedTarget));
        money.systemPayout(money.ACCT_BOUNTY_CHECK, player, amount, "handleAwardedBountyCheck", params);
    }
    public static void showSetBountySUI(obj_id player, obj_id killer) throws InterruptedException
    {
        String prompt = "@bounty_hunter:setbounty_prompt1 ";
        prompt += getName(killer) + "? ";
        prompt += "@bounty_hunter:setbounty_prompt2";
        prompt += " " + getTotalMoney(player);
        String title = "@bounty_hunter:setbounty_title";
        int pid = createSUIPage(sui.SUI_INPUTBOX, player, player, "handleSetBounty");
        sui.setAutosaveProperty(pid, false);
        sui.setSizeProperty(pid, 300, 325);
        sui.setLocationProperty(pid, 400, 200);
        setSUIProperty(pid, sui.INPUTBOX_PROMPT, sui.PROP_TEXT, prompt);
        setSUIProperty(pid, sui.INPUTBOX_TITLE, sui.PROP_TEXT, title);
        sui.inputboxButtonSetup(pid, sui.OK_CANCEL);
        sui.inputboxStyleSetup(pid, sui.INPUT_NORMAL);
        setSUIProperty(pid, sui.INPUTBOX_INPUT, "MaxLength", "20");
        setSUIProperty(pid, sui.INPUTBOX_COMBO, "MaxLength", "20");
        subscribeToSUIProperty(pid, sui.INPUTBOX_INPUT, sui.PROP_LOCALTEXT);
        subscribeToSUIProperty(pid, sui.INPUTBOX_COMBO, sui.PROP_SELECTEDTEXT);
        showSUIPage(pid);
        utils.setScriptVar(player, "setbounty.killer", killer);
    }
    public static void endBountySession(obj_id hunter, obj_id target, boolean hunterWon) throws InterruptedException
    {
    }
    public static void winBountyMission(obj_id hunter, obj_id target) throws InterruptedException
    {
        int bountyValue = 0;
        if (hasObjVar(target, "bounty.amount"))
        {
            bountyValue = getIntObjVar(target, "bounty.amount");
        }
        if (!canRewardHunterForTarget(hunter, target))
        {
            bountyValue = (int)(bountyValue * 0.10f);
            sendSystemMessage(hunter, "[BH Anti-Grief] Repeat target detected; payout reduced.", null);
        }
        float renownMod = getRenownPayoutModifier(hunter);
        bountyValue = (int)(bountyValue * renownMod);
        int heatBonusPercent = getHeatPayoutBonusPercent(getLocation(target));
        if (heatBonusPercent > 0)
        {
            bountyValue = bountyValue + ((bountyValue * heatBonusPercent) / 100);
        }
        obj_id[] payoutParticipants = getEligibleGroupParticipants(hunter, target, 192.0f);
        int participantCount = (payoutParticipants == null || payoutParticipants.length < 1) ? 1 : payoutParticipants.length;
        int hunterPayout = bountyValue;
        int share = 0;
        if (participantCount > 1)
        {
            share = bountyValue / participantCount;
            if (share < 1)
            {
                share = 1;
            }
            hunterPayout = bountyValue - (share * (participantCount - 1));
            if (hunterPayout < 1)
            {
                hunterPayout = 1;
            }
        }
        dictionary d = new dictionary();
        d.put("target", target);
        d.put("bounty", hunterPayout);
        money.systemPayout(money.ACCT_BOUNTY, hunter, hunterPayout, "handleAwardedPlayerBounty", d);
        if (participantCount > 1 && share > 0)
        {
            for (obj_id participant : payoutParticipants)
            {
                if (participant == hunter)
                {
                    continue;
                }
                money.bankTo(money.ACCT_BOUNTY, participant, share);
                sendSystemMessage(participant, "[BH Group Hunt] You received " + share + " credits for assisting.", null);
            }
        }
        recordHunterReward(hunter, target);
        addRenown(hunter, "player_bounty_kill", 50);
        location targetLoc = getLocation(target);
        if (targetLoc != null)
        {
            adjustRegionalHeat(targetLoc.area, 6.0f);
        }
        float factionAdj = getBountyFactionPointAdjustment(hunter, target);
        if (factionAdj != 0.0f)
        {
            factions.addFactionStanding(hunter, factions.getFactionNameByHashCode(pvpGetAlignedFaction(hunter)), factionAdj);
        }
        prose_package pp = new prose_package();
        pp = prose.setStringId(pp, new string_id("bounty_hunter", "bounty_success_hunter"));
        pp = prose.setTT(pp, target);
        pp = prose.setDI(pp, bountyValue);
        sendSystemMessageProse(hunter, pp);
        pp = prose.setStringId(pp, new string_id("bounty_hunter", "bounty_success_target"));
        pp = prose.setTT(pp, hunter);
        sendSystemMessageProse(target, pp);
        obj_id[] hunters = getJediBounties(target);
        if (hunters != null && hunters.length > 0)
        {
            for (obj_id hunter1 : hunters) {
                if (hunter1 != hunter) {
                    messageTo(hunter1, "handleBountyMissionIncomplete", d, 0.0f, true);
                }
            }
        }
        obj_id mission = getBountyMission(hunter);
        if (isIdValid(mission))
        {
            endMission(mission);
        }
        removeObjVar(target, "bounty");
        setJediBountyValue(target, 0);
        removeAllJediBounties(target);
    }
    public static void winBountyMissionCapture(obj_id hunter, obj_id target) throws InterruptedException
    {
        int bountyValue = 0;
        if (hasObjVar(target, "bounty.amount"))
        {
            bountyValue = getIntObjVar(target, "bounty.amount");
        }
        bountyValue = (int)(bountyValue * 1.15f);
        float renownMod = getRenownPayoutModifier(hunter);
        bountyValue = (int)(bountyValue * renownMod);
        dictionary d = new dictionary();
        d.put("target", target);
        d.put("bounty", bountyValue);
        money.systemPayout(money.ACCT_BOUNTY, hunter, bountyValue, "handleAwardedPlayerBounty", d);
        addRenown(hunter, "player_bounty_capture", 70);
        recordHunterReward(hunter, target);
        prose_package pp = new prose_package();
        pp = prose.setStringId(pp, new string_id("bounty_hunter", "bounty_success_hunter"));
        pp = prose.setTT(pp, target);
        pp = prose.setDI(pp, bountyValue);
        sendSystemMessageProse(hunter, pp);
        removeObjVar(target, "bounty");
        setJediBountyValue(target, 0);
        removeAllJediBounties(target);
    }
    public static void loseBountyMission(obj_id hunter, obj_id target) throws InterruptedException
    {
        prose_package pp = new prose_package();
        pp = prose.setStringId(pp, new string_id("bounty_hunter", "bounty_failed_hunter"));
        pp = prose.setTT(pp, target);
        sendSystemMessageProse(hunter, pp);
        pp = prose.setStringId(pp, new string_id("bounty_hunter", "bounty_failed_target"));
        pp = prose.setTT(pp, hunter);
        sendSystemMessageProse(target, pp);
        obj_id mission = getBountyMission(hunter);
        if (isIdValid(mission))
        {
            endMission(mission);
        }
        removeJediBounty(target, hunter);
        // remove TEFs that were set when players engaged in battle
        if(isPlayer(hunter) && isPlayer(target)) {
            if (pvpHasPersonalEnemyFlag(target, hunter)) pvpRemovePersonalEnemyFlags(target, hunter);
            if (pvpHasPersonalEnemyFlag(hunter, target)) pvpRemovePersonalEnemyFlags(hunter, target);
        }
        CustomerServiceLog("bounty", "%TU was defeated by %TT and failed to collect the bounty on %PT head", hunter, target);
    }
    public static obj_id getBountyMission(obj_id player) throws InterruptedException
    {
        return getBountyMission(player, obj_id.NULL_ID);
    }
    public static obj_id getBountyMission(obj_id player, obj_id target) throws InterruptedException
    {
        obj_id lastMissionId = null;
        if (isIdValid(player))
        {
            obj_id[] missionList = getMissionObjects(player);
            if (missionList != null)
            {
                for (obj_id obj_id : missionList) {
                    String type = getMissionType(obj_id);
                    if (type.equals("bounty")) {
                        if (hasObjVar(obj_id, "bh.itemBountyId")) {
                            continue;
                        }
                        if (isIdValid(target)) {
                            if (hasObjVar(obj_id, "objTarget")) {
                                obj_id missionTarget = getObjIdObjVar(obj_id, "objTarget");
                                if (missionTarget == target) {
                                    return obj_id;
                                }
                            }
                        } else {
                            lastMissionId = obj_id;
                        }
                    }
                }
            }
        }
        return lastMissionId;
    }
    public static boolean hasMaxBountyMissionsOnTarget(obj_id target) throws InterruptedException
    {
        obj_id[] hunters = getJediBounties(target);
        if (hunters == null || hunters.length == 0)
        {
            return false;
        }
        int numHunters = hunters.length;
        String Smax = getConfigSetting("GameServer", "maxJediBounties");
        int maxHunters = 3;
        if (Smax != null && !Smax.equals(""))
        {
            Integer Imax = Integer.getInteger(Smax);
            if (Imax != null)
            {
                maxHunters = Imax;
            }
        }
        if (numHunters >= maxHunters)
        {
            return true;
        }
        return false;
    }
    public static float getBountyFactionPointAdjustment(obj_id hunter, obj_id target) throws InterruptedException
    {
        float pvpRating = pvp.getCurrentPvPRating(target);
        float points = 0.0f;
        if ((pvpGetAlignedFaction(hunter) != 0) && (pvpGetAlignedFaction(hunter) == pvpGetAlignedFaction(target)))
        {
            points = pvpRating / 4.0f;
            points *= -1.0f;
        }
        else if (factions.pvpAreFactionsOpposed(pvpGetAlignedFaction(hunter), pvpGetAlignedFaction(target)))
        {
            points = pvpRating / 10.0f;
        }
        return points;
    }
    public static void probeDroidTrackTarget(obj_id player, obj_id droid) throws InterruptedException
    {
        int intDroidType = getIntObjVar(droid, "intDroidType");
        obj_id objBountyMission = getBountyMission(player);
        obj_id objMission = getBountyMission(player);
        dictionary dctParams = new dictionary();
        dctParams.put("objPlayer", player);
        if (!hasCommand(player, "droid_track"))
        {
            return;
        }
        int intState = getIntObjVar(objMission, "intState");
        if (intState != 1)
        {
            string_id strSpam = new string_id("mission/mission_generic", "bounty_no_signature");
            sendSystemMessage(player, strSpam);
            return;
        }
        if (utils.hasScriptVar(objMission, "intTracking"))
        {
            string_id strSpam = new string_id("mission/mission_generic", "bounty_already_tracking");
            sendSystemMessage(player, strSpam);
            return;
        }
        if (hasObjVar(objMission, "objTarget"))
        {
            obj_id objTarget = getObjIdObjVar(objMission, "objTarget");
            if (isIdValid(objTarget))
            {
                dictionary dctJediInfo = requestJedi(objTarget);
                if (dctJediInfo == null)
                {
                    string_id strSpam = new string_id("mission/mission_generic", "jedi_not_online");
                    sendSystemMessage(player, strSpam);
                    return;
                }
                else 
                {
                    boolean boolOnline = dctJediInfo.getBoolean("online");
                    if (!boolOnline)
                    {
                        string_id strSpam = new string_id("mission/mission_generic", "jedi_not_online");
                        sendSystemMessage(player, strSpam);
                        return;
                    }
                }
            }
        }
        setObjVar(droid, "objPlayer", player);
        if (intDroidType == DROID_PROBOT)
        {
            bounty_hunter.callProbot(player, droid, objMission);
        }
        if (intDroidType == DROID_SEEKER)
        {
            location locSpawnLocation = getLocation(player);
            if (isValidId(locSpawnLocation.cell))
            {
                string_id strResponse = new string_id("mission/mission_generic", "not_in_house");
                sendSystemMessage(player, strResponse);
                return;
            }
            if ((toLower(locSpawnLocation.area)).startsWith("kashyyyk") || (toLower(locSpawnLocation.area)).startsWith("mustafar"))
            {
                sendSystemMessage(player, new string_id("mission/mission_generic", "no_seek"));
                return;
            }
            utils.setScriptVar(objMission, "intTracking", 1);
            debugServerConsoleMsg(droid, "seeker");
            dctParams.put("intDroidType", intDroidType);
            dctParams.put("objDroid", droid);
            dctParams.put("intTrackType", DROID_TRACK_TARGET);
            setObjVar(objMission, "intDroidType", intDroidType);
            setObjVar(objMission, "objDroid", droid);
            setObjVar(objMission, "intTrackType", DROID_TRACK_TARGET);
            if (!hasObjVar(objMission, "intMissionDynamic"))
            {
                dctParams.put("playerBounty", 1);
            }
            location locHeading = getHeading(player);
            locSpawnLocation.x = locSpawnLocation.x + locHeading.x;
            locSpawnLocation.z = locSpawnLocation.z + locHeading.z;
            obj_id objSeeker = createObject("object/creature/npc/droid/bounty_seeker.iff", locSpawnLocation);
            messageTo(objSeeker, "takeOff", null, 5, true);
            string_id strSpam = new string_id("mission/mission_generic", "seeker_droid_launched");
            sendSystemMessage(player, strSpam);
            messageTo(objMission, "halfwayNotification", dctParams, 40, true);
            messageTo(objMission, "halfwayNotification", dctParams, 60, true);
            messageTo(objMission, "findTarget", dctParams, 20, true);
            int intCount = getCount(droid);
            intCount = intCount - 1;
            if (intCount < 0)
            {
                destroyObject(droid);
            }
            else 
            {
                setCount(droid, intCount);
            }
        }
        return;
    }
    public static boolean callProbot(obj_id player, obj_id droid, obj_id objMission) throws InterruptedException
    {
        if (!isValidId(player) || !exists(player))
        {
            return false;
        }
        if (!isValidId(droid) || !exists(droid))
        {
            return false;
        }
        location currentPlayerLocation = getLocation(player);
        if (isValidId(currentPlayerLocation.cell))
        {
            string_id strResponse = new string_id("mission/mission_generic", "not_in_house");
            sendSystemMessage(player, strResponse);
            return false;
        }
        dictionary dctParams = new dictionary();
        dctParams.put("objPlayer", player);
        dctParams.put("intDroidType", DROID_PROBOT);
        dctParams.put("objDroid", droid);
        dctParams.put("intTrackType", DROID_TRACK_TARGET);
        if (!hasObjVar(objMission, "intMissionDynamic"))
        {
            dctParams.put("playerBounty", 1);
        }
        messageTo(droid, "droid_Probot_Start", dctParams, 0, true);
        return true;
    }
}
