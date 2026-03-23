package script.library;

import script.*;

import java.util.Vector;

/**
 * Story companions (SWTOR-style roster): datatable-defined NPC pets with influence and combat role.
 * Uses existing {@link pet_lib#PET_TYPE_NPC} / callable combat-pet rules (one out at a time, shared stored cap).
 */
public class companion_lib extends script.base_script
{
    public companion_lib()
    {
    }
    public static final String STORY_COMPANIONS_TABLE = "datatables/companion/story_companions.iff";
    public static final String OBJVAR_STORY_COMPANION_ID = "companion.storyId";
    public static final String OBJVAR_COMBAT_STANCE = "companion.stance";
    public static final String OBJVAR_INFLUENCE_PREFIX = "companion.influence.";
    public static final int STANCE_TANK = 0;
    public static final int STANCE_HEALER = 1;
    public static final int STANCE_DPS = 2;
    public static final int MAX_INFLUENCE = 10000;
    public static final int MIN_INFLUENCE = 0;
    public static int parseRoleString(String role) throws InterruptedException
    {
        if (role == null)
        {
            return STANCE_DPS;
        }
        String r = role.toLowerCase();
        if (r.equals("tank"))
        {
            return STANCE_TANK;
        }
        if (r.equals("healer") || r.equals("heal"))
        {
            return STANCE_HEALER;
        }
        return STANCE_DPS;
    }
    public static String stanceToLabel(int stance) throws InterruptedException
    {
        switch (stance)
        {
            case STANCE_TANK:
            return "Tank";
            case STANCE_HEALER:
            return "Healer";
            case STANCE_DPS:
            default:
            return "Damage";
        }
    }
    public static boolean isValidStoryCompanionRow(String companionId) throws InterruptedException
    {
        if (companionId == null || companionId.length() < 1)
        {
            return false;
        }
        String creatureName = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "creature_name");
        return creatureName != null && creatureName.length() > 0;
    }
    /** Display name (unique per design); falls back to {@code companionId} if unset. */
    public static String getStoryCompanionDisplayName(String companionId) throws InterruptedException
    {
        if (!isValidStoryCompanionRow(companionId))
        {
            return null;
        }
        String name = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "companion_name");
        if (name == null || name.length() < 1)
        {
            return companionId;
        }
        return name;
    }
    /** Creatures-table weapon column token, or empty to keep the mob’s default weapons. */
    public static String getStoryCompanionWeaponKey(String companionId) throws InterruptedException
    {
        if (!isValidStoryCompanionRow(companionId))
        {
            return "";
        }
        String w = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "companion_weapon");
        return w != null ? w : "";
    }
    /** Master-item / static item names from {@code companion_favorite_gifts}; empty cell = none. */
    public static String[] getStoryCompanionFavoriteGifts(String companionId) throws InterruptedException
    {
        if (!isValidStoryCompanionRow(companionId))
        {
            return new String[0];
        }
        String raw = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "companion_favorite_gifts");
        if (raw == null || raw.length() < 1)
        {
            return new String[0];
        }
        String[] parts = utils.split(raw, ',');
        if (parts == null || parts.length < 1)
        {
            return new String[0];
        }
        Vector cleaned = new Vector();
        for (int i = 0; i < parts.length; i++)
        {
            String t = parts[i];
            if (t == null)
            {
                continue;
            }
            t = t.trim();
            if (t.length() > 0)
            {
                cleaned.addElement(t);
            }
        }
        String[] out = new String[cleaned.size()];
        cleaned.copyInto(out);
        return out;
    }
    /** Movement scale hint; table default {@code 1.2f} when missing or non-positive. */
    public static float getStoryCompanionSpeed(String companionId) throws InterruptedException
    {
        if (!isValidStoryCompanionRow(companionId))
        {
            return 1.2f;
        }
        float v = dataTableGetFloat(STORY_COMPANIONS_TABLE, companionId, "companion_speed");
        if (v <= 0f)
        {
            return 1.2f;
        }
        return v;
    }
    public static boolean playerOwnsStoryCompanion(obj_id player, String companionId) throws InterruptedException
    {
        return isIdValid(findStoryCompanionControlDevice(player, companionId));
    }
    public static obj_id findStoryCompanionControlDevice(obj_id player, String companionId) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || companionId == null || companionId.length() < 1)
        {
            return obj_id.NULL_ID;
        }
        obj_id datapad = utils.getPlayerDatapad(player);
        if (!isIdValid(datapad))
        {
            return obj_id.NULL_ID;
        }
        obj_id[] items = getContents(datapad);
        if (items == null)
        {
            return obj_id.NULL_ID;
        }
        for (obj_id item : items)
        {
            if (!isIdValid(item) || !exists(item))
            {
                continue;
            }
            if (callable.getControlDeviceType(item) != callable.CALLABLE_TYPE_COMBAT_PET)
            {
                continue;
            }
            if (!hasObjVar(item, OBJVAR_STORY_COMPANION_ID))
            {
                continue;
            }
            String id = getStringObjVar(item, OBJVAR_STORY_COMPANION_ID);
            if (companionId.equals(id))
            {
                return item;
            }
        }
        return obj_id.NULL_ID;
    }
    public static String getActiveStoryCompanionId(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return null;
        }
        obj_id pet = callable.getCallable(player, callable.CALLABLE_TYPE_COMBAT_PET);
        if (!isIdValid(pet) || !exists(pet))
        {
            return null;
        }
        if (!hasObjVar(pet, OBJVAR_STORY_COMPANION_ID))
        {
            return null;
        }
        return getStringObjVar(pet, OBJVAR_STORY_COMPANION_ID);
    }
    public static int getInfluence(obj_id player, String companionId) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || companionId == null || companionId.length() < 1)
        {
            return MIN_INFLUENCE;
        }
        String key = OBJVAR_INFLUENCE_PREFIX + companionId;
        if (!hasObjVar(player, key))
        {
            return MIN_INFLUENCE;
        }
        return getIntObjVar(player, key);
    }
    public static void setInfluence(obj_id player, String companionId, int value) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || companionId == null || companionId.length() < 1)
        {
            return;
        }
        if (value < MIN_INFLUENCE)
        {
            value = MIN_INFLUENCE;
        }
        if (value > MAX_INFLUENCE)
        {
            value = MAX_INFLUENCE;
        }
        setObjVar(player, OBJVAR_INFLUENCE_PREFIX + companionId, value);
    }
    public static void modifyInfluence(obj_id player, String companionId, int delta) throws InterruptedException
    {
        if (delta == 0)
        {
            return;
        }
        int cur = getInfluence(player, companionId);
        setInfluence(player, companionId, cur + delta);
    }
    public static int getInfluenceTier(int influence) throws InterruptedException
    {
        if (influence < 0)
        {
            influence = 0;
        }
        return 1 + (influence / 1000);
    }
    public static int resolveSpawnLevel(String creatureName, int tableLevel) throws InterruptedException
    {
        if (tableLevel > 0)
        {
            return tableLevel;
        }
        int base = utils.dataTableGetInt(create.CREATURE_TABLE, creatureName, "BaseLevel");
        if (base < 1)
        {
            base = 1;
        }
        return base;
    }
    /**
     * For mobs spawned from a creatures-table name (e.g. {@code aaph_koden}): remove every script on the object,
     * then attach only the default creature AI/combat stack (no datatable {@code scripts} column entries such as
     * {@code npc.static_npc.*} or conversations). Appearance, objvars, and stats from {@code initializeCreature} stay.
     */
    public static void stripCreatureScriptsForStoryCompanion(obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet))
        {
            return;
        }
        for (int pass = 0; pass < 8; pass++)
        {
            String[] scripts = getScriptList(pet);
            if (scripts == null || scripts.length == 0)
            {
                break;
            }
            for (int i = 0; i < scripts.length; i++)
            {
                String s = scripts[i];
                if (s != null && s.length() > 0)
                {
                    detachScript(pet, s);
                }
            }
        }
        create.attachCreatureScripts(pet, "", true);
    }
    /**
     * Adds a story companion as a packed NPC pet control device on the datapad (SWTOR-style unlock).
     * @return the control device, or null on failure
     */
    public static obj_id grantStoryCompanionToDatapad(obj_id player, String companionId) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return null;
        }
        if (!isValidStoryCompanionRow(companionId))
        {
            return null;
        }
        if (playerOwnsStoryCompanion(player, companionId))
        {
            return findStoryCompanionControlDevice(player, companionId);
        }
        if (pet_lib.hasMaxStoredPetsOfType(player, pet_lib.PET_TYPE_NPC))
        {
            return null;
        }
        String creatureName = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "creature_name");
        int tableLevel = dataTableGetInt(STORY_COMPANIONS_TABLE, companionId, "level");
        int spawnLevel = resolveSpawnLevel(creatureName, tableLevel);
        String roleStr = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "role");
        int stance = parseRoleString(roleStr);
        location loc = getLocation(player);
        obj_id pet = create.createCreature(creatureName, loc, spawnLevel, true, true);
        if (!isIdValid(pet) || !exists(pet))
        {
            return null;
        }
        stripCreatureScriptsForStoryCompanion(pet);
        setObjVar(pet, "ai.pet.type", pet_lib.PET_TYPE_NPC);
        setObjVar(pet, OBJVAR_STORY_COMPANION_ID, companionId);
        setObjVar(pet, OBJVAR_COMBAT_STANCE, stance);
        setMaster(pet, player);
        obj_id cd = pet_lib.makeControlDevice(player, pet);
        if (!isIdValid(cd) || !exists(cd))
        {
            destroyObject(pet);
            return null;
        }
        setObjVar(cd, OBJVAR_STORY_COMPANION_ID, companionId);
        setObjVar(cd, OBJVAR_COMBAT_STANCE, stance);
        if (!hasScript(cd, "systems.companion.companion_story_pcd"))
        {
            attachScript(cd, "systems.companion.companion_story_pcd");
        }
        if (!hasScript(player, "ai.pet_master"))
        {
            attachScript(player, "ai.pet_master");
        }
        String msg = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "grant_message");
        pet_lib.storePet(pet, player);
        if (msg != null && msg.length() > 0)
        {
            sendSystemMessage(player, string_id.unlocalized(msg));
        }
        return cd;
    }
    public static void applyStanceToActivePet(obj_id controlDevice, int stance) throws InterruptedException
    {
        if (!isIdValid(controlDevice) || !exists(controlDevice))
        {
            return;
        }
        setObjVar(controlDevice, OBJVAR_COMBAT_STANCE, stance);
        obj_id pet = callable.getCDCallable(controlDevice);
        if (isIdValid(pet) && exists(pet))
        {
            setObjVar(pet, OBJVAR_COMBAT_STANCE, stance);
        }
    }
    /**
     * Mobile template for Greeata’s cantina recruiter. {@code object/mobile/greeata.iff} points at
     * {@code appearance/greeata.sat}, which is often missing on the client and renders as a cube.
     * Twi’lek female uses a standard shipped mesh and matches the character better than {@code shared_greeata}’s Rodian species block.
     */
    public static final String GREEATA_WORLD_MOBILE_TEMPLATE = "object/mobile/twilek_female.iff";
    public static final String GREEATA_CONVERSATION_SCRIPT = "conversation.companion_greeata";
    public static void detachAllScriptsFromObject(obj_id obj) throws InterruptedException
    {
        if (!isIdValid(obj) || !exists(obj))
        {
            return;
        }
        for (int pass = 0; pass < 16; pass++)
        {
            String[] scripts = getScriptList(obj);
            if (scripts == null || scripts.length == 0)
            {
                break;
            }
            for (int i = 0; i < scripts.length; i++)
            {
                String s = scripts[i];
                if (s != null && s.length() > 0)
                {
                    detachScript(obj, s);
                }
            }
        }
    }
    public static void clearCreatureStatesForHireNpc(obj_id npc) throws InterruptedException
    {
        if (!isIdValid(npc) || !exists(npc))
        {
            return;
        }
        for (int i = 0; i < STATE_NUMBER_OF_STATES; i++)
        {
            setState(npc, i, false);
        }
    }
    /**
     * Strip every script, neutralize faction/PvP, reset states/posture, clear master, then attach a hire dialog only.
     */
    public static void prepareHireConversationNpc(obj_id npc, String conversationScriptName) throws InterruptedException
    {
        if (!isIdValid(npc) || !exists(npc) || conversationScriptName == null || conversationScriptName.length() < 1)
        {
            return;
        }
        detachAllScriptsFromObject(npc);
        factions.clearFaction(npc);
        stop(npc);
        clearCreatureStatesForHireNpc(npc);
        setPosture(npc, POSTURE_UPRIGHT);
        obj_id master = getMaster(npc);
        if (isIdValid(master))
        {
            setMaster(npc, obj_id.NULL_ID);
        }
        if (!hasScript(npc, conversationScriptName))
        {
            attachScript(npc, conversationScriptName);
        }
    }
    /**
     * Hire UI script for a {@code story_companions} row. Extend when adding new companion conversations.
     */
    public static String resolveHireConversationScript(String storyCompanionId) throws InterruptedException
    {
        if (storyCompanionId == null || storyCompanionId.length() < 1)
        {
            return null;
        }
        if ("companion_greeata".equals(storyCompanionId))
        {
            return GREEATA_CONVERSATION_SCRIPT;
        }
        return null;
    }
    /**
     * Developer / cantina setup: wipe NPC to hire-only form and attach the right {@code conversation.*} script.
     */
    public static boolean applyMakeHireableToNpc(obj_id npc, String storyCompanionId) throws InterruptedException
    {
        if (!isIdValid(npc) || !exists(npc) || isPlayer(npc) || !isMob(npc))
        {
            return false;
        }
        if (!isValidStoryCompanionRow(storyCompanionId))
        {
            return false;
        }
        String convo = resolveHireConversationScript(storyCompanionId);
        if (convo == null)
        {
            return false;
        }
        prepareHireConversationNpc(npc, convo);
        if ("companion_greeata".equals(storyCompanionId))
        {
            setName(npc, "Greeata");
        }
        return true;
    }
}
