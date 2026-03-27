package script.library;

import script.*;

import java.util.Arrays;
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
    /** 0 = prefer melee commands, 1 = prefer ranged (humanoid pet bar). */
    public static final String OBJVAR_WEAPON_MODE = "companion.weaponMode";
    public static final int WEAPON_MODE_MELEE = 0;
    public static final int WEAPON_MODE_RANGED = 1;
    /** Up to three player commands the owner teaches; stored on PCD and pet (humanoid bar). */
    public static final String OBJVAR_TAUGHT_ABILITIES = "companion.taughtAbilities";
    public static final int TAUGHT_SLOT_COUNT = 3;
    public static final String COMMAND_TABLE_PATH = "datatables/command/command_table.iff";
    /** Player command names (see command_table.tab + player_beastmaster); used instead of beast_specials for humanoid skeleton companions. */
    public static final String CMD_BAR_WEAPON_TOGGLE = "companion_bar_weapon_toggle";
    public static final String CMD_BAR_SLOT_A = "companion_bar_slot_a";
    public static final String CMD_BAR_SLOT_B = "companion_bar_slot_b";
    public static final String CMD_BAR_SLOT_C = "companion_bar_slot_c";
    public static final String[] HUMANOID_COMPANION_BAR_COMMANDS = 
    {
        CMD_BAR_WEAPON_TOGGLE,
        CMD_BAR_SLOT_A,
        CMD_BAR_SLOT_B,
        CMD_BAR_SLOT_C
    };
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
    public static boolean isStoryCompanionControlDevice(obj_id cd) throws InterruptedException
    {
        return isIdValid(cd) && exists(cd) && hasObjVar(cd, OBJVAR_STORY_COMPANION_ID);
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
    /**
     * Story companion combat level from the owner: if {@code level} in {@code story_companions} is 0, uses the
     * player's level; if positive, uses the lesser of that cap and the player's level. Matches pet combat rules
     * (non-traders capped at 60 like crafted pets).
     */
    public static int resolveEffectiveStoryCompanionLevel(obj_id player, String creatureName, int tableLevel) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return 1;
        }
        int pl = getLevel(player);
        if (pl < 1)
        {
            pl = 1;
        }
        int cap = pl;
        if (tableLevel > 0)
        {
            cap = Math.min(tableLevel, pl);
        }
        if (cap > 60 && !craftinglib.isTrader(player))
        {
            cap = 60;
        }
        return cap;
    }
    /**
     * Writes {@code creature_attribs.level} and combat stats on the PCD from {@code stat_balance.iff} for the
     * effective level (player-based). Call before {@code pet_lib#createPetFromData} and after granting.
     */
    public static void applyStoryCompanionPcdStatsForPlayer(obj_id player, obj_id pcd) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        String companionId = getStringObjVar(pcd, OBJVAR_STORY_COMPANION_ID);
        String creatureName = getStringObjVar(pcd, "pet.creatureName");
        if (creatureName == null || creatureName.length() < 1)
        {
            return;
        }
        if (!isValidStoryCompanionRow(companionId))
        {
            return;
        }
        int tableLevel = dataTableGetInt(STORY_COMPANIONS_TABLE, companionId, "level");
        int level = resolveEffectiveStoryCompanionLevel(player, creatureName, tableLevel);
        if (level < 1)
        {
            level = 1;
        }
        int health = dataTableGetInt(pet_lib.TBL_MOB_STAT_BALANCE, level - 1, "HP");
        int iconst = dataTableGetInt(pet_lib.TBL_MOB_STAT_BALANCE, level - 1, "HealthRegen");
        float dps = dataTableGetFloat(pet_lib.TBL_MOB_STAT_BALANCE, level - 1, "damagePerSecond");
        int minDamage = Math.round((dps * 2.0f) * 0.5f);
        int maxDamage = Math.round((dps * 2.0f) * 1.5f);
        int toHit = dataTableGetInt(pet_lib.TBL_MOB_STAT_BALANCE, level - 1, "ToHit");
        int defenseValue = dataTableGetInt(pet_lib.TBL_MOB_STAT_BALANCE, level - 1, "Def");
        int general_protection = dataTableGetInt(pet_lib.TBL_MOB_STAT_BALANCE, level - 1, "Armor");
        bio_engineer.stripOldStats(pcd);
        setObjVar(pcd, "creature_attribs.level", level);
        setObjVar(pcd, "creature_attribs." + create.MAXATTRIBNAMES[HEALTH], health);
        setObjVar(pcd, "creature_attribs." + create.MAXATTRIBNAMES[CONSTITUTION], iconst);
        setObjVar(pcd, "creature_attribs.minDamage", minDamage);
        setObjVar(pcd, "creature_attribs.maxDamage", maxDamage);
        setObjVar(pcd, "creature_attribs.toHitChance", toHit);
        setObjVar(pcd, "creature_attribs.defenseValue", defenseValue);
        setObjVar(pcd, "creature_attribs.general_protection", general_protection);
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
        int spawnLevel = resolveEffectiveStoryCompanionLevel(player, creatureName, tableLevel);
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
        setObjVar(cd, OBJVAR_TAUGHT_ABILITIES, new String[]
        {
            "empty",
            "empty",
            "empty"
        });
        if (!hasScript(cd, "systems.companion.companion_story_pcd"))
        {
            attachScript(cd, "systems.companion.companion_story_pcd");
        }
        applyStoryCompanionPcdStatsForPlayer(player, cd);
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
    /** Set on hire NPCs using {@link #applyMakeHireableToNpc}; read by {@link #GENERIC_HIRE_CONVERSATION_SCRIPT}. */
    public static final String OBJVAR_HIRE_COMPANION_ID = "companion.hireCompanionId";
    public static final String GENERIC_HIRE_CONVERSATION_SCRIPT = "conversation.companion_common_hire";
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
     * Hire UI script for a {@code story_companions} row: Greeata uses {@link #GREEATA_CONVERSATION_SCRIPT}; all other valid rows use {@link #GENERIC_HIRE_CONVERSATION_SCRIPT}.
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
        if (isValidStoryCompanionRow(storyCompanionId))
        {
            return GENERIC_HIRE_CONVERSATION_SCRIPT;
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
        setObjVar(npc, OBJVAR_HIRE_COMPANION_ID, storyCompanionId);
        if ("companion_greeata".equals(storyCompanionId))
        {
            setName(npc, "Greeata");
        }
        return true;
    }
    public static boolean isStoryCompanionPet(obj_id pet) throws InterruptedException
    {
        return isIdValid(pet) && exists(pet) && hasObjVar(pet, OBJVAR_STORY_COMPANION_ID);
    }
    /**
     * {@link pet_lib#createPetFromData} spawns a fresh creature; copy roster identity from the PCD so scripts
     * ({@link #isStoryCompanionPet}, incap rules, pet bar) match the first grant.
     */
    public static void copyStoryCompanionIdentityFromPcdToPet(obj_id pcd, obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pcd) || !exists(pcd) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        if (!isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        copyObjVar(pcd, pet, OBJVAR_STORY_COMPANION_ID);
        if (hasObjVar(pcd, OBJVAR_COMBAT_STANCE))
        {
            copyObjVar(pcd, pet, OBJVAR_COMBAT_STANCE);
        }
        if (hasObjVar(pcd, OBJVAR_WEAPON_MODE))
        {
            copyObjVar(pcd, pet, OBJVAR_WEAPON_MODE);
        }
        if (hasObjVar(pcd, OBJVAR_TAUGHT_ABILITIES))
        {
            copyObjVar(pcd, pet, OBJVAR_TAUGHT_ABILITIES);
        }
        syncCompanionTaughtCommandGrants(pet);
    }
    /**
     * Aligns the summoned companion’s GCW/PvP faction with the owner (Imperial/Rebel) or clears it when the owner is neutral.
     * Call after {@link #copyStoryCompanionIdentityFromPcdToPet}.
     */
    public static void applyStoryCompanionFactionFromOwner(obj_id owner, obj_id pet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(owner) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        if (!isStoryCompanionPet(pet))
        {
            return;
        }
        String ownerFaction = factions.getFaction(owner);
        if (ownerFaction != null && ownerFaction.length() > 0)
        {
            factions.setFaction(pet, ownerFaction);
        }
        else
        {
            factions.clearFaction(pet);
        }
    }
    /**
     * Creature that should receive BM pet-bar commands: a called beast-master pet if any, else an active story companion combat pet.
     */
    public static obj_id getPetBarCombatCreature(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return null;
        }
        obj_id beast = beast_lib.getBeastOnPlayer(player);
        if (isIdValid(beast) && exists(beast))
        {
            return beast;
        }
        obj_id combat = callable.getCallable(player, callable.CALLABLE_TYPE_COMBAT_PET);
        if (isStoryCompanionPet(combat))
        {
            return combat;
        }
        return null;
    }
    /** Human-shaped companions use player-style pet bar commands instead of {@code beast_specials}. */
    public static boolean usesHumanoidStoryCompanionPetBar(obj_id pet) throws InterruptedException
    {
        return isStoryCompanionPet(pet) && ai_lib.isHumanSkeleton(pet);
    }
    public static void grantHumanoidCompanionBarCommands(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return;
        }
        for (int i = 0; i < HUMANOID_COMPANION_BAR_COMMANDS.length; ++i)
        {
            grantCommand(player, HUMANOID_COMPANION_BAR_COMMANDS[i]);
        }
    }
    public static void revokeHumanoidCompanionBarCommands(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return;
        }
        for (int i = 0; i < HUMANOID_COMPANION_BAR_COMMANDS.length; ++i)
        {
            revokeCommand(player, HUMANOID_COMPANION_BAR_COMMANDS[i]);
        }
    }
    public static String[] getHumanoidStoryCompanionTrainedCommands()
    {
        return new String[]
        {
            CMD_BAR_WEAPON_TOGGLE,
            CMD_BAR_SLOT_A,
            CMD_BAR_SLOT_B,
            CMD_BAR_SLOT_C
        };
    }
    public static String[] buildHumanoidStoryCompanionPetBar(obj_id player, obj_id pet) throws InterruptedException
    {
        String[] barData = (String[])beast_lib.PET_BAR_DEFAULT_ARRAY.clone();
        barData[0] = beast_lib.BM_COMMAND_ATTACK;
        barData[1] = beast_lib.BM_COMMAND_FOLLOW;
        barData[2] = beast_lib.BM_COMMAND_STAY;
        barData[7] = beast_lib.BM_COMMAND_DISABLED;
        barData[8] = beast_lib.BM_COMMAND_DISABLED;
        String[] cmds = getHumanoidStoryCompanionTrainedCommands();
        barData[3] = cmds[0];
        barData[4] = cmds[1];
        barData[5] = cmds[2];
        barData[6] = cmds[3];
        return barData;
    }
    public static void toggleCompanionWeaponModeFromBar(obj_id player) throws InterruptedException
    {
        obj_id pet = getPetBarCombatCreature(player);
        if (!isStoryCompanionPet(pet) || !usesHumanoidStoryCompanionPetBar(pet))
        {
            return;
        }
        int mode = WEAPON_MODE_MELEE;
        if (hasObjVar(pet, OBJVAR_WEAPON_MODE))
        {
            mode = getIntObjVar(pet, OBJVAR_WEAPON_MODE);
        }
        mode = (mode == WEAPON_MODE_MELEE) ? WEAPON_MODE_RANGED : WEAPON_MODE_MELEE;
        setObjVar(pet, OBJVAR_WEAPON_MODE, mode);
        obj_id target = getIntendedTarget(player);
        if (isIdValid(target) && pvpCanAttack(pet, target))
        {
            if (mode == WEAPON_MODE_MELEE)
            {
                queueCommand(pet, getStringCrc("meleeHit"), target, "", COMMAND_PRIORITY_DEFAULT);
            }
            else
            {
                queueCommand(pet, getStringCrc("rangedShot"), target, "", COMMAND_PRIORITY_DEFAULT);
            }
        }
        sendSystemMessage(player, string_id.unlocalized(mode == WEAPON_MODE_MELEE ? "Companion weapon focus: melee." : "Companion weapon focus: ranged."));
    }
    public static String[] getTaughtAbilitiesArray(obj_id obj) throws InterruptedException
    {
        String[] def = new String[TAUGHT_SLOT_COUNT];
        for (int i = 0; i < TAUGHT_SLOT_COUNT; ++i)
        {
            def[i] = "empty";
        }
        if (!isIdValid(obj) || !exists(obj) || !hasObjVar(obj, OBJVAR_TAUGHT_ABILITIES))
        {
            return def;
        }
        String[] raw = getStringArrayObjVar(obj, OBJVAR_TAUGHT_ABILITIES);
        if (raw == null || raw.length < TAUGHT_SLOT_COUNT)
        {
            return def;
        }
        for (int i = 0; i < TAUGHT_SLOT_COUNT; ++i)
        {
            if (raw[i] != null && raw[i].length() > 0 && !raw[i].equals("empty"))
            {
                def[i] = raw[i];
            }
        }
        return def;
    }
    public static boolean canPlayerTeachCommandToCompanion(obj_id player, String commandName) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || commandName == null || commandName.length() < 1)
        {
            return false;
        }
        if (commandName.equals("empty"))
        {
            return false;
        }
        if (commandName.startsWith("companion_bar"))
        {
            return false;
        }
        if (!hasCommand(player, commandName))
        {
            return false;
        }
        if (dataTableSearchColumnForString(commandName, "commandName", COMMAND_TABLE_PATH) < 0)
        {
            return false;
        }
        String hook = dataTableGetString(COMMAND_TABLE_PATH, commandName, "scriptHook");
        if (hook == null || hook.length() < 1)
        {
            return false;
        }
        return true;
    }
    public static String[] getCompanionTrainableCommandList(obj_id player) throws InterruptedException
    {
        String[] all = getCommandListingForPlayer(player);
        if (all == null || all.length < 1)
        {
            return new String[0];
        }
        Vector v = new Vector();
        for (int i = 0; i < all.length; ++i)
        {
            String c = all[i];
            if (c == null)
            {
                continue;
            }
            if (canPlayerTeachCommandToCompanion(player, c))
            {
                v.addElement(c);
            }
        }
        String[] out = new String[v.size()];
        v.copyInto(out);
        Arrays.sort(out);
        return out;
    }
    public static void revokeAllTaughtCommandsFromPet(obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet))
        {
            return;
        }
        String[] t = getTaughtAbilitiesArray(pet);
        for (int i = 0; i < TAUGHT_SLOT_COUNT; ++i)
        {
            if (t[i] != null && !t[i].equals("empty"))
            {
                revokeCommand(pet, t[i]);
            }
        }
    }
    public static void syncCompanionTaughtCommandGrants(obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet) || !isStoryCompanionPet(pet))
        {
            return;
        }
        revokeAllTaughtCommandsFromPet(pet);
        String[] t = getTaughtAbilitiesArray(pet);
        for (int i = 0; i < TAUGHT_SLOT_COUNT; ++i)
        {
            if (t[i] != null && !t[i].equals("empty"))
            {
                grantCommand(pet, t[i]);
            }
        }
    }
    public static void setTaughtAbilityOnPcd(obj_id pcd, int slot, String skillName, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || slot < 0 || slot >= TAUGHT_SLOT_COUNT || !beast_lib.isValidPlayer(player))
        {
            return;
        }
        String normalized = skillName == null ? "empty" : skillName.trim();
        if (normalized.length() < 1)
        {
            normalized = "empty";
        }
        if (!normalized.equals("empty") && !canPlayerTeachCommandToCompanion(player, normalized))
        {
            sendSystemMessage(player, string_id.unlocalized("You cannot teach that ability to this companion."));
            return;
        }
        String[] t = getTaughtAbilitiesArray(pcd);
        String oldSkill = t[slot];
        t[slot] = normalized;
        setObjVar(pcd, OBJVAR_TAUGHT_ABILITIES, t);
        obj_id pet = callable.getCDCallable(pcd);
        if (isIdValid(pet) && exists(pet) && isStoryCompanionPet(pet))
        {
            setObjVar(pet, OBJVAR_TAUGHT_ABILITIES, t);
            syncCompanionTaughtCommandGrants(pet);
            refreshStoryCompanionPetBar(player, pet);
        }
        if (normalized.equals("empty"))
        {
            sendSystemMessage(player, string_id.unlocalized("Companion ability slot " + (slot + 1) + " cleared."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Companion will use \"" + normalized + "\" from pet bar slot " + (slot + 1) + " (separate cooldown from the companion)."));
        }
    }
    public static void executeCompanionTaughtSlot(obj_id player, int slotIndex) throws InterruptedException
    {
        if (slotIndex < 0 || slotIndex >= TAUGHT_SLOT_COUNT)
        {
            return;
        }
        obj_id pet = getPetBarCombatCreature(player);
        if (!isStoryCompanionPet(pet) || !usesHumanoidStoryCompanionPetBar(pet))
        {
            sendSystemMessage(player, string_id.unlocalized("No humanoid companion is active."));
            return;
        }
        String[] t = getTaughtAbilitiesArray(pet);
        String skill = t[slotIndex];
        if (skill == null || skill.equals("empty"))
        {
            sendSystemMessage(player, string_id.unlocalized("Train an ability in this slot from the companion control device."));
            return;
        }
        obj_id target = getIntendedTarget(player);
        if (!isIdValid(target))
        {
            target = getLookAtTarget(player);
        }
        if (!isIdValid(target))
        {
            sendSystemMessage(player, string_id.unlocalized("Select a valid target for this ability."));
            return;
        }
        queueCommand(pet, getStringCrc(skill.toLowerCase()), target, "", COMMAND_PRIORITY_DEFAULT);
    }
    public static boolean isValidBeastSpecialForStoryPetBar(String abilityName) throws InterruptedException
    {
        if (abilityName == null || abilityName.length() < 1 || abilityName.equals("empty"))
        {
            return false;
        }
        return dataTableSearchColumnForString(abilityName, "ability_name", beast_lib.BEASTS_SPECIALS) > -1;
    }
    /** Up to four {@code beast_specials} ability_name entries for the BM-style pet bar (comma-separated in datatable). */
    public static String[] getStoryCompanionTrainedSkillsFromTable(String companionId) throws InterruptedException
    {
        String[] trained = 
        {
            "empty",
            "empty",
            "empty",
            "empty"
        };
        if (!isValidStoryCompanionRow(companionId))
        {
            return trained;
        }
        String raw = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "companion_pet_bar_abilities");
        if (raw == null || raw.length() < 1)
        {
            return trained;
        }
        String[] parts = utils.split(raw, ',');
        if (parts == null)
        {
            return trained;
        }
        int idx = 0;
        for (int p = 0; p < parts.length && idx < trained.length; ++p)
        {
            String t = parts[p];
            if (t == null)
            {
                continue;
            }
            t = t.trim();
            if (t.length() < 1)
            {
                continue;
            }
            if (isValidBeastSpecialForStoryPetBar(t))
            {
                trained[idx++] = t;
            }
        }
        return trained;
    }
    public static String[] buildStoryCompanionPetBar(obj_id player, obj_id pet, String[] knownSkills) throws InterruptedException
    {
        String[] barData = (String[])beast_lib.PET_BAR_DEFAULT_ARRAY.clone();
        barData[0] = beast_lib.BM_COMMAND_ATTACK;
        barData[1] = beast_lib.BM_COMMAND_FOLLOW;
        barData[2] = beast_lib.BM_COMMAND_STAY;
        barData[7] = beast_lib.BM_COMMAND_DISABLED;
        barData[8] = beast_lib.BM_COMMAND_DISABLED;
        if (knownSkills != null && knownSkills.length > 0 && knownSkills[0] != null && !knownSkills[0].equals("empty"))
        {
            barData[3] = knownSkills[0];
        }
        int additional = getSkillStatisticModifier(player, "expertise_bm_add_pet_bar");
        if (additional > 3)
        {
            additional = 3;
        }
        for (int i = 0; i < additional; ++i)
        {
            if (knownSkills != null && knownSkills.length > i + 1)
            {
                String s = knownSkills[i + 1];
                if (s != null && !s.equals("empty"))
                {
                    barData[i + 4] = s;
                }
            }
        }
        return barData;
    }
    /**
     * Links the client BM pet bar to this story companion: {@code setBeastmasterPet}, command slots, and {@code abilities.trained_skills} on the pet.
     */
    public static void refreshStoryCompanionPetBar(obj_id player, obj_id pet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isStoryCompanionPet(pet))
        {
            return;
        }
        if (usesHumanoidStoryCompanionPetBar(pet))
        {
            revokeHumanoidCompanionBarCommands(player);
            grantHumanoidCompanionBarCommands(player);
            String[] trained = getHumanoidStoryCompanionTrainedCommands();
            setObjVar(pet, beast_lib.PET_TRAINED_SKILLS_LIST, trained);
            String[] bar = buildHumanoidStoryCompanionPetBar(player, pet);
            setBeastmasterPet(player, pet);
            setBeastmasterPetCommands(player, bar);
        }
        else
        {
            revokeHumanoidCompanionBarCommands(player);
            String companionId = getStringObjVar(pet, OBJVAR_STORY_COMPANION_ID);
            String[] trained = getStoryCompanionTrainedSkillsFromTable(companionId);
            setObjVar(pet, beast_lib.PET_TRAINED_SKILLS_LIST, trained);
            String[] bar = buildStoryCompanionPetBar(player, pet, trained);
            setBeastmasterPet(player, pet);
            setBeastmasterPetCommands(player, bar);
        }
        String[] toggles = 
        {
            "",
            "",
            "",
            "",
            ""
        };
        setBeastmasterToggledPetCommands(player, toggles);
    }
    public static void clearStoryCompanionPetBarIfActive(obj_id player, obj_id pet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isIdValid(pet))
        {
            return;
        }
        obj_id cur = getBeastmasterPet(player);
        if (cur == pet)
        {
            revokeHumanoidCompanionBarCommands(player);
            setBeastmasterPet(player, null);
            setBeastmasterPetCommands(player, (String[])beast_lib.PET_BAR_DEFAULT_ARRAY.clone());
            String[] toggles = 
            {
                "",
                "",
                "",
                "",
                ""
            };
            setBeastmasterToggledPetCommands(player, toggles);
        }
    }
}
