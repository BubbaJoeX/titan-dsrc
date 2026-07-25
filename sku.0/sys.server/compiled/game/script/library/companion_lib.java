package script.library;

import script.*;
import script.systems.companion.companion_combat_helper;

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
    /** Full-tier companions for each playable player species and gender (see {@code companion_spawn_template} column). */
    public static final String[] PLAYABLE_SPECIES_COMPANION_IDS =
    {
        "companion_human_male",
        "companion_human_female",
        "companion_bothan_male",
        "companion_bothan_female",
        "companion_ithorian_male",
        "companion_ithorian_female",
        "companion_moncal_male",
        "companion_moncal_female",
        "companion_rodian_male",
        "companion_rodian_female",
        "companion_sullustan_male",
        "companion_sullustan_female",
        "companion_trandoshan_male",
        "companion_trandoshan_female",
        "companion_twilek_male",
        "companion_twilek_female",
        "companion_wookiee_male",
        "companion_wookiee_female",
        "companion_zabrak_male",
        "companion_zabrak_female"
    };
    public static final String OBJVAR_STORY_COMPANION_ID = "companion.storyId";
    /** Owner-assigned display name; falls back to datatable companion_name. */
    public static final String OBJVAR_DISPLAY_NAME = "companion.displayName";
    /** Player-creature template path for full-customization companions (e.g. object/creature/player/human_male.iff). */
    public static final String OBJVAR_SPAWN_TEMPLATE = "companion.spawnTemplate";
    /** Persisted gifted/default weapon while the companion is stored (obj_id in owner datapad gear hold). */
    public static final String OBJVAR_STORED_WEAPON = "companion.storedWeapon";
    /** Hidden inventory container on the owner datapad keyed to this PCD. */
    public static final String OBJVAR_GEAR_HOLD = "companion.gearHold";
    /** Marks objects stashed for a story companion PCD. */
    public static final String OBJVAR_BOUND_PCD = "companion.boundPcd";
    public static final String CUSTOMIZATION_TIER_NONE = "none";
    public static final String CUSTOMIZATION_TIER_CELEBRITY = "celebrity";
    public static final String CUSTOMIZATION_TIER_FULL = "full";
    /** Hidden datapad stash; use creature inventory stubs (droid_inventory is not in the compiled template list). */
    public static final String GEAR_HOLD_TEMPLATE = "object/tangible/inventory/creature_inventory_1.iff";
    public static final String OBJVAR_CUSTOMIZATION_SCALE = "companion.customization.scale";
    /** Server hair template path (e.g. object/tangible/hair/human/human_male_s01.iff). Empty = bald. */
    public static final String OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE = "companion.customization.hairTemplate";
    public static final String OBJVAR_CUSTOMIZATION_HAIR_VARS = "companion.customization.hairVars";
    public static final String SCRIPTVAR_SKIP_RANDOM_HUE = "companion.skipRandomHue";
    public static final String[] GEAR_HOLD_TEMPLATE_FALLBACKS = 
    {
        "object/tangible/inventory/creature_inventory_2.iff",
        "object/tangible/inventory/creature_inventory_3.iff",
        "object/tangible/inventory/creature_inventory_4.iff"
    };
    public static final String OBJVAR_COMBAT_STANCE = "companion.stance";
    /** 0 = prefer melee commands, 1 = prefer ranged (humanoid pet bar). */
    public static final String OBJVAR_WEAPON_MODE = "companion.weaponMode";
    public static final int WEAPON_MODE_MELEE = 0;
    public static final int WEAPON_MODE_RANGED = 1;
    /** Up to four player commands the owner teaches; stored on PCD and pet (humanoid bar). */
    public static final String OBJVAR_TAUGHT_ABILITIES = "companion.taughtAbilities";
    public static final int TAUGHT_SLOT_COUNT = 4;
    /**
     * Three programmable pet-bar slots (attack / follow / stay style) — slash command names the owner assigns; stored on PCD and pet.
     */
    public static final String OBJVAR_CORE_BAR_COMMANDS = "companion.coreBarCommands";
    public static final int CORE_BAR_SLOT_COUNT = 3;
    public static final String COMMAND_TABLE_PATH = "datatables/command/command_table.iff";
    /** Player command names (see command_table.tab + player_beastmaster); used instead of beast_specials for humanoid skeleton companions. */
    public static final String CMD_BAR_WEAPON_TOGGLE = "companion_bar_weapon_toggle";
    public static final String CMD_BAR_SLOT_A = "companion_bar_slot_a";
    public static final String CMD_BAR_SLOT_B = "companion_bar_slot_b";
    public static final String CMD_BAR_SLOT_C = "companion_bar_slot_c";
    public static final String CMD_BAR_SLOT_D = "companion_bar_slot_d";
    /** Executable pet-bar wrappers (suffix avoids client icon path stripping on {@code _0}). */
    public static final String CMD_BAR_CORE_SLOT_0 = "companion_bar_core_slot0";
    public static final String CMD_BAR_CORE_SLOT_1 = "companion_bar_core_slot1";
    public static final String CMD_BAR_CORE_SLOT_2 = "companion_bar_core_slot2";
    public static final String[] COMPANION_CORE_BAR_WRAPPER_COMMANDS = 
    {
        CMD_BAR_CORE_SLOT_0,
        CMD_BAR_CORE_SLOT_1,
        CMD_BAR_CORE_SLOT_2
    };
    /** Granted with {@link #COMPANION_CORE_BAR_WRAPPER_COMMANDS} when a story companion pet bar is active. */
    public static final String[] HUMANOID_COMPANION_ONLY_BAR_COMMANDS = 
    {
        CMD_BAR_WEAPON_TOGGLE,
        CMD_BAR_SLOT_A,
        CMD_BAR_SLOT_B,
        CMD_BAR_SLOT_C,
        CMD_BAR_SLOT_D
    };
    /**
     * Pet bar strings sent to the client may use {@code base|displayCommand}; the client parses this in
     * {@code SwgCuiToolbar::onPetCommandsChanged} so icons/tooltips use {@code displayCommand} while execution stays {@code base}.
     */
    public static final String PET_BAR_CMD_DISPLAY_SEPARATOR = "|";
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
    /** {@code none}, {@code celebrity}, or {@code full} from {@code companion_customization}; defaults to {@code none}. */
    public static String getStoryCompanionCustomizationTier(String companionId) throws InterruptedException
    {
        if (!isValidStoryCompanionRow(companionId))
        {
            return CUSTOMIZATION_TIER_NONE;
        }
        String tier = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "companion_customization");
        if (tier == null || tier.length() < 1)
        {
            return CUSTOMIZATION_TIER_NONE;
        }
        tier = tier.trim().toLowerCase();
        if (tier.equals(CUSTOMIZATION_TIER_CELEBRITY) || tier.equals(CUSTOMIZATION_TIER_FULL))
        {
            return tier;
        }
        return CUSTOMIZATION_TIER_NONE;
    }
    public static String getStoryCompanionCustomizationTierFromPcd(obj_id pcd) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd))
        {
            return CUSTOMIZATION_TIER_NONE;
        }
        return getStoryCompanionCustomizationTier(getStringObjVar(pcd, OBJVAR_STORY_COMPANION_ID));
    }
    public static boolean canRenameStoryCompanion(obj_id pcd) throws InterruptedException
    {
        String tier = getStoryCompanionCustomizationTierFromPcd(pcd);
        return tier.equals(CUSTOMIZATION_TIER_CELEBRITY) || tier.equals(CUSTOMIZATION_TIER_FULL);
    }
    public static boolean canCustomizeStoryCompanionAppearance(obj_id pcd) throws InterruptedException
    {
        return CUSTOMIZATION_TIER_FULL.equals(getStoryCompanionCustomizationTierFromPcd(pcd));
    }
    /** Resolved display name: PCD objvar, then datatable default. */
    public static String getCompanionDisplayName(obj_id pcd) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd))
        {
            return null;
        }
        if (hasObjVar(pcd, OBJVAR_DISPLAY_NAME))
        {
            String custom = getStringObjVar(pcd, OBJVAR_DISPLAY_NAME);
            if (custom != null && custom.length() > 0)
            {
                return custom;
            }
        }
        return getStoryCompanionDisplayName(getStringObjVar(pcd, OBJVAR_STORY_COMPANION_ID));
    }
    public static void setCompanionDisplayName(obj_id pcd, String name) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        if (name == null || name.trim().length() < 1)
        {
            removeObjVar(pcd, OBJVAR_DISPLAY_NAME);
            name = getStoryCompanionDisplayName(getStringObjVar(pcd, OBJVAR_STORY_COMPANION_ID));
        }
        else
        {
            name = name.trim();
            if (name.length() > 32)
            {
                name = name.substring(0, 32);
            }
            setObjVar(pcd, OBJVAR_DISPLAY_NAME, name);
        }
        setName(pcd, name);
        obj_id pet = callable.getCDCallable(pcd);
        if (isIdValid(pet) && exists(pet))
        {
            setName(pet, name);
        }
    }
    public static void applyStoryCompanionDisplayName(obj_id pcd, obj_id pet) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        String name = getCompanionDisplayName(pcd);
        if (name != null && name.length() > 0)
        {
            setName(pet, name);
            setName(pcd, name);
        }
    }
    /** {@code object/creature/player/{species}_{gender}.iff} from the granting player. */
    public static String resolvePlayerSpeciesTemplate(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return "object/creature/player/human_male.iff";
        }
        String species = utils.getPlayerSpeciesName(getSpecies(player));
        if ("moncalamari".equals(species))
        {
            species = "moncal";
        }
        if (species == null || species.length() < 1 || species.equals("unknown"))
        {
            species = "human";
        }
        String gender = (getGender(player) == GENDER_FEMALE) ? "female" : "male";
        return "object/creature/player/" + species + "_" + gender + ".iff";
    }
    /**
     * Player creature templates register as players on client/server and break pet interaction.
     * Map to {@code object/creature/npc/base/shared_{species}_base_{gender}.iff} instead.
     */
    public static String normalizeStoryCompanionSpawnTemplate(String templatePath) throws InterruptedException
    {
        if (templatePath == null || templatePath.length() < 1)
        {
            return templatePath;
        }
        String t = templatePath.trim();
        if (t.indexOf("/creature/npc/") >= 0)
        {
            return t;
        }
        if (t.indexOf("/creature/player/") < 0)
        {
            return t;
        }
        int slash = t.lastIndexOf('/');
        if (slash < 0 || slash >= t.length() - 1)
        {
            return t;
        }
        String file = t.substring(slash + 1);
        if (file.endsWith(".iff"))
        {
            file = file.substring(0, file.length() - 4);
        }
        int us = file.lastIndexOf('_');
        if (us <= 0)
        {
            return t;
        }
        String species = file.substring(0, us);
        String gender = file.substring(us + 1);
        if (!gender.equals("male") && !gender.equals("female"))
        {
            return t;
        }
        if (gender.equals("female") && (species.equals("sullustan") || species.equals("ithorian")))
        {
            return "object/creature/npc/base/shared_" + species + "_base_male.iff";
        }
        return "object/creature/npc/base/shared_" + species + "_base_" + gender + ".iff";
    }
    /** Full-tier spawn template: datatable override or granting player's species, always as an NPC base template. */
    public static String resolveStoryCompanionGrantTemplate(String companionId, obj_id player) throws InterruptedException
    {
        String resolved = null;
        if (isValidStoryCompanionRow(companionId))
        {
            String fixed = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "companion_spawn_template");
            if (fixed != null && fixed.trim().length() > 0)
            {
                resolved = fixed.trim();
            }
        }
        if (resolved == null)
        {
            resolved = resolvePlayerSpeciesTemplate(player);
        }
        return normalizeStoryCompanionSpawnTemplate(resolved);
    }
    /** Creatures-table row used to spawn a full-tier companion as a real NPC mob (never a player template). */
    public static String resolvePlayableCommonerCreatureName(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return "commoner";
        }
        String species = utils.getPlayerSpeciesName(getSpecies(player));
        if ("moncalamari".equals(species))
        {
            species = "moncal";
        }
        if (species == null || species.length() < 1 || species.equals("unknown"))
        {
            species = "human";
        }
        String gender = (getGender(player) == GENDER_FEMALE) ? "female" : "male";
        String specific = "commoner_" + species + "_" + gender;
        if (utils.dataTableGetRow(create.CREATURE_TABLE, specific) != null)
        {
            return specific;
        }
        specific = "commoner_" + species;
        if (utils.dataTableGetRow(create.CREATURE_TABLE, specific) != null)
        {
            return specific;
        }
        return "commoner";
    }
    public static String resolveStoryCompanionCreatureName(String companionId, obj_id player) throws InterruptedException
    {
        if (!isValidStoryCompanionRow(companionId))
        {
            return "commoner";
        }
        String creatureName = dataTableGetString(STORY_COMPANIONS_TABLE, companionId, "creature_name");
        if (creatureName == null || creatureName.length() < 1)
        {
            creatureName = "commoner";
        }
        if (!CUSTOMIZATION_TIER_FULL.equals(getStoryCompanionCustomizationTier(companionId)))
        {
            return creatureName;
        }
        if (creatureName.equals("commoner"))
        {
            return resolvePlayableCommonerCreatureName(player);
        }
        return creatureName;
    }
    /** Server object template path for {@code createObject} (strips {@code shared_} prefix from grant templates). */
    public static String resolveStoryCompanionServerObjectTemplate(String companionId, obj_id player) throws InterruptedException
    {
        String template = resolveStoryCompanionGrantTemplate(companionId, player);
        if (template == null || template.length() < 1)
        {
            return null;
        }
        if (template.indexOf("/shared_") >= 0)
        {
            return template.replace("/shared_", "/");
        }
        return template;
    }
    /**
     * Player-style appearance: suppress template baked wearables on clients, ensure appearance inventory, strip NPC defaults.
     */
    public static void setupStoryCompanionPlayerAppearance(obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet))
        {
            return;
        }
        setSuppressTemplateClientDataFile(pet, true);
        ensureAppearanceInventory(pet);
        stripStoryCompanionDefaultWearables(pet);
    }
    private static String normalizeCustomizationVarPath(String varPath) throws InterruptedException
    {
        if (varPath == null || varPath.length() < 1)
        {
            return varPath;
        }
        if (varPath.startsWith("/"))
        {
            return varPath;
        }
        return "/" + varPath;
    }
    /** Persists hair, face, body morphs, palette colors, and scale on the story companion PCD. */
    public static void saveStoryCompanionCustomizationToPcd(obj_id pet, obj_id pcd) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        setObjVar(pcd, pet_lib.VAR_PALVAR_BASE, 1);
        setObjVar(pcd, OBJVAR_CUSTOMIZATION_SCALE, getScale(pet));
        obj_id hair = getObjectInSlot(pet, slots.HAIR);
        if (isIdValid(hair) && exists(hair))
        {
            setObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE, getTemplateName(hair));
            custom_var[] hairVars = getAllCustomVars(hair);
            if (hairVars != null)
            {
                for (int h = 0; h < hairVars.length; ++h)
                {
                    custom_var hcv = hairVars[h];
                    if (hcv == null || !(hcv instanceof ranged_int_custom_var))
                    {
                        continue;
                    }
                    ranged_int_custom_var hri = (ranged_int_custom_var) hcv;
                    String hairVarName = hri.getVarName();
                    if (hairVarName == null || hairVarName.length() < 1)
                    {
                        continue;
                    }
                    setObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS + "." + hairVarName, hri.getValue());
                }
            }
        }
        else
        {
            removeObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE);
            removeObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS);
        }
        custom_var[] allVars = getAllCustomVars(pet);
        if (allVars != null)
        {
            for (int i = 0; i < allVars.length; ++i)
            {
                custom_var cv = allVars[i];
                if (cv == null || !(cv instanceof ranged_int_custom_var))
                {
                    continue;
                }
                ranged_int_custom_var ri = (ranged_int_custom_var) cv;
                String varName = ri.getVarName();
                if (varName == null || varName.length() < 1)
                {
                    continue;
                }
                setObjVar(pcd, pet_lib.VAR_PALVAR_VARS + "." + varName, ri.getValue());
            }
        }
    }
    /** Restores saved hair, face, body, palette, and scale from the PCD onto a freshly spawned pet. */
    public static void restoreStoryCompanionCustomizationFromPcd(obj_id pet, obj_id pcd) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        if (!hasObjVar(pcd, pet_lib.VAR_PALVAR_BASE))
        {
            return;
        }
        if (hasObjVar(pcd, OBJVAR_CUSTOMIZATION_SCALE))
        {
            setScale(pet, getFloatObjVar(pcd, OBJVAR_CUSTOMIZATION_SCALE));
        }
        obj_var_list ovl = getObjVarList(pcd, pet_lib.VAR_PALVAR_VARS);
        if (ovl == null)
        {
            return;
        }
        int numItem = ovl.getNumItems();
        for (int i = 0; i < numItem; ++i)
        {
            obj_var ov = ovl.getObjVar(i);
            if (ov == null)
            {
                continue;
            }
            String var = ov.getName();
            int val = ov.getIntData();
            if (var == null || var.length() < 1)
            {
                continue;
            }
            if (var.indexOf("index_texture") >= 0 || var.endsWith("_texture_1"))
            {
                setRangedIntCustomVarValue(pet, normalizeCustomizationVarPath(var), val);
            }
            else if (!hue.setColor(pet, var, val))
            {
                setRangedIntCustomVarValue(pet, normalizeCustomizationVarPath(var), val);
            }
        }
        applyStoryCompanionHairFromPcd(pet, pcd);
    }
    /** Creates or removes hair on the companion pet from PCD-persisted hair template and vars. */
    public static void applyStoryCompanionHairFromPcd(obj_id pet, obj_id pcd) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        obj_id existingHair = getObjectInSlot(pet, slots.HAIR);
        if (!hasObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE))
        {
            if (isIdValid(existingHair) && exists(existingHair))
            {
                destroyObject(existingHair);
            }
            return;
        }
        String hairTemplate = getStringObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE);
        if (hairTemplate == null || hairTemplate.length() < 1)
        {
            if (isIdValid(existingHair) && exists(existingHair))
            {
                destroyObject(existingHair);
            }
            return;
        }
        if (isIdValid(existingHair) && exists(existingHair) && hairTemplate.equals(getTemplateName(existingHair)))
        {
            applyStoryCompanionHairVarsFromPcd(existingHair, pcd);
            return;
        }
        if (isIdValid(existingHair) && exists(existingHair))
        {
            destroyObject(existingHair);
        }
        obj_id newHair = createObject(hairTemplate, pet, slots.HAIR);
        if (isIdValid(newHair) && exists(newHair))
        {
            applyStoryCompanionHairVarsFromPcd(newHair, pcd);
        }
    }
    private static void applyStoryCompanionHairVarsFromPcd(obj_id hair, obj_id pcd) throws InterruptedException
    {
        if (!isIdValid(hair) || !exists(hair) || !hasObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS))
        {
            return;
        }
        obj_var_list ovl = getObjVarList(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS);
        if (ovl == null)
        {
            return;
        }
        int numItem = ovl.getNumItems();
        for (int i = 0; i < numItem; ++i)
        {
            obj_var ov = ovl.getObjVar(i);
            if (ov == null)
            {
                continue;
            }
            String var = ov.getName();
            int val = ov.getIntData();
            if (var == null || var.length() < 1)
            {
                continue;
            }
            if (var.indexOf("index_texture") >= 0 || var.endsWith("_texture_1"))
            {
                setRangedIntCustomVarValue(hair, normalizeCustomizationVarPath(var), val);
            }
            else if (!hue.setColor(hair, var, val))
            {
                setRangedIntCustomVarValue(hair, normalizeCustomizationVarPath(var), val);
            }
        }
    }
    private static obj_id finishStoryCompanionCreatureSpawn(String creatureName, dictionary creatureDict, obj_id pet, obj_id owner, obj_id pcd, int level, boolean withAi, boolean isPet) throws InterruptedException
    {
        if (!isIdValid(pet) || creatureDict == null)
        {
            return null;
        }
        if (isPet)
        {
            utils.setScriptVar(pet, "petBeingInitialized", true);
        }
        utils.setScriptVar(pet, "spawnedBy", owner);
        utils.setScriptVar(pet, SCRIPTVAR_SKIP_RANDOM_HUE, 1);
        setObjVar(pet, create.INITIALIZE_CREATURE_DO_NOT_SCALE_OBJVAR, 1);
        create.initializeCreature(pet, creatureName, creatureDict, level);
        create.attachCreatureScripts(pet, creatureDict.getString("scripts"), withAi);
        setupStoryCompanionPlayerAppearance(pet);
        if (isIdValid(pcd) && hasObjVar(pcd, pet_lib.VAR_PALVAR_BASE))
        {
            restoreStoryCompanionCustomizationFromPcd(pet, pcd);
        }
        return pet;
    }
    /**
     * Spawns a full-customization story companion from the naked NPC base template (not dressed {@code commoner_*} mobiles).
     */
    public static obj_id spawnStoryCompanionCreature(String companionId, obj_id player, location loc, int level, boolean withAi, boolean isPet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isValidStoryCompanionRow(companionId) || loc == null)
        {
            return null;
        }
        String creatureName = resolveStoryCompanionCreatureName(companionId, player);
        dictionary creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, creatureName);
        if (creatureDict == null)
        {
            creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, "commoner");
        }
        if (creatureDict == null)
        {
            return null;
        }
        String template = resolveStoryCompanionServerObjectTemplate(companionId, player);
        if (template == null || template.length() < 1)
        {
            return create.createCreature(creatureName, loc, level, withAi, isPet);
        }
        obj_id pet = createObject(template, loc);
        return finishStoryCompanionCreatureSpawn(creatureName, creatureDict, pet, player, obj_id.NULL_ID, level, withAi, isPet);
    }
    public static obj_id spawnStoryCompanionCreature(String companionId, obj_id player, obj_id pcd, location loc, int level, boolean withAi, boolean isPet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isValidStoryCompanionRow(companionId) || loc == null)
        {
            return null;
        }
        String creatureName = resolveStoryCompanionCreatureName(companionId, player);
        dictionary creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, creatureName);
        if (creatureDict == null)
        {
            creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, "commoner");
        }
        if (creatureDict == null)
        {
            return null;
        }
        String template = resolveStoryCompanionServerObjectTemplate(companionId, player);
        if (template == null || template.length() < 1)
        {
            return create.createCreature(creatureName, loc, level, withAi, isPet);
        }
        obj_id pet = createObject(template, loc);
        return finishStoryCompanionCreatureSpawn(creatureName, creatureDict, pet, player, pcd, level, withAi, isPet);
    }
    public static obj_id spawnStoryCompanionCreature(String companionId, obj_id player, obj_id container, int level, boolean withAi, boolean isPet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isValidStoryCompanionRow(companionId) || !isIdValid(container))
        {
            return null;
        }
        String creatureName = resolveStoryCompanionCreatureName(companionId, player);
        dictionary creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, creatureName);
        if (creatureDict == null)
        {
            creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, "commoner");
        }
        if (creatureDict == null)
        {
            return null;
        }
        String template = resolveStoryCompanionServerObjectTemplate(companionId, player);
        if (template == null || template.length() < 1)
        {
            return create.createCreature(creatureName, container, level, withAi, isPet);
        }
        obj_id pet = createObject(template, container, "");
        return finishStoryCompanionCreatureSpawn(creatureName, creatureDict, pet, player, obj_id.NULL_ID, level, withAi, isPet);
    }
    public static obj_id spawnStoryCompanionCreature(String companionId, obj_id player, obj_id pcd, obj_id container, int level, boolean withAi, boolean isPet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isValidStoryCompanionRow(companionId) || !isIdValid(container))
        {
            return null;
        }
        String creatureName = resolveStoryCompanionCreatureName(companionId, player);
        dictionary creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, creatureName);
        if (creatureDict == null)
        {
            creatureDict = utils.dataTableGetRow(create.CREATURE_TABLE, "commoner");
        }
        if (creatureDict == null)
        {
            return null;
        }
        String template = resolveStoryCompanionServerObjectTemplate(companionId, player);
        if (template == null || template.length() < 1)
        {
            return create.createCreature(creatureName, container, level, withAi, isPet);
        }
        obj_id pet = createObject(template, container, "");
        return finishStoryCompanionCreatureSpawn(creatureName, creatureDict, pet, player, pcd, level, withAi, isPet);
    }
    /** Legacy PCDs stored player or npc .iff spawn paths; rewrite to creatures-table names. */
    public static void migrateStoryCompanionPcdSpawnData(obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || !beast_lib.isValidPlayer(player))
        {
            return;
        }
        String companionId = getStringObjVar(pcd, OBJVAR_STORY_COMPANION_ID);
        String resolvedCreature = resolveStoryCompanionCreatureName(companionId, player);
        if (hasObjVar(pcd, OBJVAR_SPAWN_TEMPLATE))
        {
            removeObjVar(pcd, OBJVAR_SPAWN_TEMPLATE);
        }
        String current = hasObjVar(pcd, "pet.creatureName") ? getStringObjVar(pcd, "pet.creatureName") : "";
        if (current == null || current.length() < 1 || current.equals("commoner") || current.indexOf(".iff") >= 0 || current.indexOf("/") >= 0)
        {
            setObjVar(pcd, "pet.creatureName", resolvedCreature);
        }
    }
    public static boolean hasStoryCompanionSpawnTemplate(obj_id pcd) throws InterruptedException
    {
        return isStoryCompanionControlDevice(pcd) && hasObjVar(pcd, OBJVAR_SPAWN_TEMPLATE) && getStringObjVar(pcd, OBJVAR_SPAWN_TEMPLATE).length() > 0;
    }
    public static String getStoryCompanionSpawnTemplate(obj_id pcd) throws InterruptedException
    {
        if (!hasStoryCompanionSpawnTemplate(pcd))
        {
            return null;
        }
        String raw = getStringObjVar(pcd, OBJVAR_SPAWN_TEMPLATE);
        String normalized = normalizeStoryCompanionSpawnTemplate(raw);
        if (normalized != null && !normalized.equals(raw))
        {
            setObjVar(pcd, OBJVAR_SPAWN_TEMPLATE, normalized);
        }
        return normalized;
    }
    public static obj_id ensureCompanionGearHold(obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || !beast_lib.isValidPlayer(player))
        {
            return obj_id.NULL_ID;
        }
        if (hasObjVar(pcd, OBJVAR_GEAR_HOLD))
        {
            obj_id hold = getObjIdObjVar(pcd, OBJVAR_GEAR_HOLD);
            if (isIdValid(hold) && exists(hold))
            {
                return hold;
            }
        }
        obj_id datapad = utils.getPlayerDatapad(player);
        if (!isIdValid(datapad))
        {
            return obj_id.NULL_ID;
        }
        obj_id hold = createObject(GEAR_HOLD_TEMPLATE, datapad, "");
        if (!isIdValid(hold))
        {
            for (int i = 0; i < GEAR_HOLD_TEMPLATE_FALLBACKS.length; ++i)
            {
                hold = createObject(GEAR_HOLD_TEMPLATE_FALLBACKS[i], datapad, "");
                if (isIdValid(hold))
                {
                    break;
                }
            }
        }
        if (!isIdValid(hold))
        {
            return obj_id.NULL_ID;
        }
        setObjVar(hold, OBJVAR_BOUND_PCD, pcd);
        setObjVar(hold, "noTrade", 1);
        setName(hold, " ");
        setObjVar(pcd, OBJVAR_GEAR_HOLD, hold);
        return hold;
    }
    private static boolean isCompanionCreatureWeapon(obj_id weapon) throws InterruptedException
    {
        return isIdValid(weapon) && (utils.hasScriptVar(weapon, "isCreatureWeapon") || hasObjVar(weapon, "isCreatureWeapon"));
    }
    private static obj_id getStoryCompanionEquippedPlayerWeapon(obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet))
        {
            return obj_id.NULL_ID;
        }
        obj_id cur = getCurrentWeapon(pet);
        if (!isIdValid(cur))
        {
            cur = getDefaultWeapon(pet);
        }
        if (!isIdValid(cur) || !exists(cur) || isCompanionCreatureWeapon(cur))
        {
            return obj_id.NULL_ID;
        }
        return cur;
    }
    /**
     * Keeps a player-bound companion weapon active; {@code aiEquipPrimaryWeapon} otherwise swaps to creature defaults (unarmed).
     */
    public static void ensureStoryCompanionCombatWeapon(obj_id pet) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet))
        {
            return;
        }
        obj_id weapon = getStoryCompanionEquippedPlayerWeapon(pet);
        if (!isIdValid(weapon))
        {
            return;
        }
        obj_id cur = getCurrentWeapon(pet);
        if (cur != weapon)
        {
            setCurrentWeapon(pet, weapon);
            clearAiWeaponCombatProfiles(pet);
        }
    }
    /**
     * Active combat pet that receives owner group-buff sharing: BM beast if out, else active story companion.
     */
    public static obj_id getOwnerBuffSharePet(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return obj_id.NULL_ID;
        }
        obj_id beast = beast_lib.getBeastOnPlayer(player);
        if (isIdValid(beast) && exists(beast))
        {
            return beast;
        }
        return getActiveStoryCompanionPet(player);
    }
    /**
     * Match follow speed to the owner's movement mode when close; sprint only to catch up or when the owner is running.
     */
    public static void syncStoryCompanionFollowSpeed(obj_id pet, obj_id master) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet) || !beast_lib.isValidPlayer(master))
        {
            return;
        }
        if (utils.getBooleanScriptVar(pet, "ai.pet.staying"))
        {
            return;
        }
        if (ai_lib.isInCombat(pet))
        {
            setMovementRun(pet);
            return;
        }
        float dist = getDistance(pet, master);
        if (dist >= 5.0f)
        {
            setMovementRun(pet);
            return;
        }
        int masterLoc = getLocomotion(master);
        if (masterLoc == LOCOMOTION_RUNNING)
        {
            setMovementRun(pet);
        }
        else
        {
            setMovementWalk(pet);
        }
    }
    /** No script {@code unequip}; clear active weapon if needed, then move via {@link #putInOverloaded}. */
    private static boolean moveItemFromCreatureToContainer(obj_id item, obj_id container, obj_id player) throws InterruptedException
    {
        if (!isIdValid(item) || !exists(item) || !isIdValid(container))
        {
            return false;
        }
        obj_id creature = getTopMostContainer(item);
        if (isIdValid(creature) && exists(creature) && creature != item && isWeapon(item))
        {
            obj_id cur = getCurrentWeapon(creature);
            if (item == cur)
            {
                obj_id def = getDefaultWeapon(creature);
                if (isIdValid(def) && exists(def))
                {
                    setCurrentWeapon(creature, def);
                }
                else
                {
                    setCurrentWeapon(creature, obj_id.NULL_ID);
                }
            }
        }
        if (putInOverloaded(item, container))
        {
            return true;
        }
        return putIn(item, container, player);
    }
    public static void stashStoryCompanionWeapon(obj_id weapon, obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isIdValid(weapon) || !exists(weapon) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        obj_id hold = ensureCompanionGearHold(pcd, player);
        if (!isIdValid(hold))
        {
            return;
        }
        if (moveItemFromCreatureToContainer(weapon, hold, player))
        {
            setObjVar(weapon, OBJVAR_BOUND_PCD, pcd);
            setObjVar(weapon, "noTrade", 1);
            setObjVar(pcd, OBJVAR_STORED_WEAPON, weapon);
        }
    }
    public static void saveStoryCompanionWeaponOnStore(obj_id pet, obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        obj_id weapon = getStoryCompanionEquippedPlayerWeapon(pet);
        if (isIdValid(weapon))
        {
            stashStoryCompanionWeapon(weapon, pcd, player);
        }
    }
    public static boolean restoreStoryCompanionWeaponOnSummon(obj_id pet, obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet) || !isStoryCompanionControlDevice(pcd))
        {
            return false;
        }
        if (hasObjVar(pcd, OBJVAR_STORED_WEAPON))
        {
            obj_id weapon = getObjIdObjVar(pcd, OBJVAR_STORED_WEAPON);
            if (isIdValid(weapon) && exists(weapon))
            {
                obj_id petInv = utils.getInventoryContainer(pet);
                if (isIdValid(petInv) && getContainedBy(weapon) != petInv)
                {
                    putIn(weapon, petInv, player);
                }
                if (setCurrentWeapon(pet, weapon) || equip(weapon, pet))
                {
                    setCurrentWeapon(pet, weapon);
                    clearAiWeaponCombatProfiles(pet);
                    return true;
                }
            }
            else
            {
                removeObjVar(pcd, OBJVAR_STORED_WEAPON);
            }
        }
        String companionId = getStringObjVar(pcd, OBJVAR_STORY_COMPANION_ID);
        return applyDefaultStoryCompanionWeapon(pet, companionId);
    }
    /** Grants datatable {@code companion_weapon} when set (template .iff path). */
    public static boolean applyDefaultStoryCompanionWeapon(obj_id pet, String companionId) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet) || companionId == null)
        {
            return false;
        }
        String weaponKey = getStoryCompanionWeaponKey(companionId);
        if (weaponKey == null || weaponKey.length() < 1 || !weaponKey.endsWith(".iff"))
        {
            return false;
        }
        obj_id petInv = utils.getInventoryContainer(pet);
        if (!isIdValid(petInv))
        {
            return false;
        }
        obj_id weapon = weapons.createWeapon(weaponKey, petInv, 0.85f);
        if (!isIdValid(weapon))
        {
            weapon = createObject(weaponKey, petInv, "");
        }
        if (!isIdValid(weapon))
        {
            return false;
        }
        if (equip(weapon, pet, "hold_r") || equip(weapon, pet))
        {
            setCurrentWeapon(pet, weapon);
            clearAiWeaponCombatProfiles(pet);
            return true;
        }
        return false;
    }
    public static boolean clearStoryCompanionWeapon(obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || !beast_lib.isValidPlayer(player))
        {
            return false;
        }
        obj_id pet = callable.getCDCallable(pcd);
        if (isIdValid(pet) && exists(pet))
        {
            obj_id weapon = getStoryCompanionEquippedPlayerWeapon(pet);
            if (isIdValid(weapon))
            {
                destroyObject(weapon);
            }
        }
        if (hasObjVar(pcd, OBJVAR_STORED_WEAPON))
        {
            obj_id stored = getObjIdObjVar(pcd, OBJVAR_STORED_WEAPON);
            if (isIdValid(stored) && exists(stored))
            {
                destroyObject(stored);
            }
            removeObjVar(pcd, OBJVAR_STORED_WEAPON);
        }
        sendSystemMessage(player, string_id.unlocalized("Companion weapon cleared."));
        return true;
    }
    public static boolean returnWeaponToPlayerFromPcd(obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || !beast_lib.isValidPlayer(player))
        {
            return false;
        }
        obj_id weapon = obj_id.NULL_ID;
        if (hasObjVar(pcd, OBJVAR_STORED_WEAPON))
        {
            weapon = getObjIdObjVar(pcd, OBJVAR_STORED_WEAPON);
        }
        obj_id pet = callable.getCDCallable(pcd);
        if (!isIdValid(weapon) && isIdValid(pet) && exists(pet))
        {
            weapon = getStoryCompanionEquippedPlayerWeapon(pet);
        }
        if (!isIdValid(weapon) || !exists(weapon))
        {
            sendSystemMessage(player, string_id.unlocalized("Your companion has no player weapon to return."));
            return false;
        }
        obj_id ownerInv = utils.getInventoryContainer(player);
        if (!isIdValid(ownerInv))
        {
            return false;
        }
        if (!moveItemFromCreatureToContainer(weapon, ownerInv, player))
        {
            sendSystemMessage(player, string_id.unlocalized("No room in your inventory for the companion's weapon."));
            return false;
        }
        removeObjVar(weapon, OBJVAR_BOUND_PCD);
        removeObjVar(weapon, "noTrade");
        removeObjVar(pcd, OBJVAR_STORED_WEAPON);
        sendSystemMessage(player, string_id.unlocalized("The companion's weapon was returned to your inventory."));
        return true;
    }
    public static String describeStoryCompanionWeapon(obj_id pcd) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd))
        {
            return "None";
        }
        obj_id weapon = obj_id.NULL_ID;
        if (hasObjVar(pcd, OBJVAR_STORED_WEAPON))
        {
            weapon = getObjIdObjVar(pcd, OBJVAR_STORED_WEAPON);
        }
        obj_id pet = callable.getCDCallable(pcd);
        if (!isIdValid(weapon) && isIdValid(pet) && exists(pet))
        {
            weapon = getStoryCompanionEquippedPlayerWeapon(pet);
        }
        if (!isIdValid(weapon) || !exists(weapon))
        {
            return "None (default creature weapons)";
        }
        String nm = getName(weapon);
        if (nm == null || nm.length() < 1)
        {
            nm = getStaticItemName(weapon);
        }
        if (nm == null || nm.length() < 1)
        {
            nm = getTemplateName(weapon);
        }
        return nm != null ? nm : "Unknown weapon";
    }
    public static boolean isCompanionDressableTangible(obj_id item)
    {
        if (!isIdValid(item) || !exists(item) || !isTangible(item))
        {
            return false;
        }
        int got = getGameObjectType(item);
        return isGameObjectTypeOf(got, GOT_armor)
            || isGameObjectTypeOf(got, GOT_clothing)
            || isGameObjectTypeOf(got, GOT_weapon)
            || isGameObjectTypeOf(got, GOT_jewelry)
            || isGameObjectTypeOf(got, GOT_cybernetic)
            || got == GOT_misc_appearance_only
            || got == GOT_misc_appearance_only_invisible
            || got == GOT_misc_container_wearable;
    }
    public static void saveStoryCompanionAppearanceOnStore(obj_id pet, obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!canCustomizeStoryCompanionAppearance(pcd) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        obj_id hold = ensureCompanionGearHold(pcd, player);
        if (!isIdValid(hold))
        {
            return;
        }
        obj_id[] equipped = getAllWornItems(pet, false);
        if (equipped != null)
        {
            for (int i = 0; i < equipped.length; ++i)
            {
                obj_id item = equipped[i];
                if (!isIdValid(item) || !exists(item))
                {
                    continue;
                }
                if (isWeapon(item) && item == getCurrentWeapon(pet))
                {
                    continue;
                }
                if (isCompanionDressableTangible(item) && !isCompanionCreatureWeapon(item))
                {
                    moveItemFromCreatureToContainer(item, hold, player);
                    setObjVar(item, OBJVAR_BOUND_PCD, pcd);
                    setObjVar(item, "noTrade", 1);
                }
            }
        }
        obj_id appInv = getAppearanceInventory(pet);
        if (isIdValid(appInv))
        {
            obj_id[] inApp = getContents(appInv);
            if (inApp != null)
            {
                for (int i = 0; i < inApp.length; ++i)
                {
                    obj_id item = inApp[i];
                    if (isIdValid(item) && exists(item))
                    {
                        putIn(item, hold, player);
                        setObjVar(item, OBJVAR_BOUND_PCD, pcd);
                        setObjVar(item, "noTrade", 1);
                    }
                }
            }
        }
    }
    /** Remove template default clothes from a freshly spawned story companion; player-bound gear is kept. */
    public static void stripStoryCompanionDefaultWearables(obj_id pet) throws InterruptedException
    {
        if (!isIdValid(pet) || !exists(pet))
        {
            return;
        }
        obj_id[] equipped = getAllWornItems(pet, false);
        if (equipped != null)
        {
            for (int i = 0; i < equipped.length; ++i)
            {
                obj_id item = equipped[i];
                if (!isIdValid(item) || !exists(item))
                {
                    continue;
                }
                if (hasObjVar(item, OBJVAR_BOUND_PCD))
                {
                    continue;
                }
                if (isCompanionCreatureWeapon(item))
                {
                    continue;
                }
                if (isWeapon(item))
                {
                    continue;
                }
                if (!isCompanionDressableTangible(item))
                {
                    continue;
                }
                destroyObject(item);
            }
        }
        obj_id appInv = getAppearanceInventory(pet);
        if (isIdValid(appInv))
        {
            obj_id[] inApp = getContents(appInv);
            if (inApp != null)
            {
                for (int i = 0; i < inApp.length; ++i)
                {
                    obj_id item = inApp[i];
                    if (!isIdValid(item) || !exists(item))
                    {
                        continue;
                    }
                    if (hasObjVar(item, OBJVAR_BOUND_PCD))
                    {
                        continue;
                    }
                    destroyObject(item);
                }
            }
        }
    }
    public static void restoreStoryCompanionAppearanceOnSummon(obj_id pet, obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!canCustomizeStoryCompanionAppearance(pcd) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        ensureAppearanceInventory(pet);
        obj_id hold = obj_id.NULL_ID;
        if (hasObjVar(pcd, OBJVAR_GEAR_HOLD))
        {
            hold = getObjIdObjVar(pcd, OBJVAR_GEAR_HOLD);
        }
        if (!isIdValid(hold))
        {
            return;
        }
        obj_id appInv = getAppearanceInventory(pet);
        obj_id[] contents = getContents(hold);
        if (contents == null)
        {
            return;
        }
        for (int i = 0; i < contents.length; ++i)
        {
            obj_id item = contents[i];
            if (!isIdValid(item) || !exists(item) || !isCompanionDressableTangible(item) || isWeapon(item))
            {
                continue;
            }
            if (isIdValid(appInv))
            {
                putIn(item, appInv, player);
            }
            equip(item, pet);
        }
    }
    public static boolean equipCompanionWearableFromPlayer(obj_id pcd, obj_id player, obj_id item) throws InterruptedException
    {
        if (!canCustomizeStoryCompanionAppearance(pcd) || !beast_lib.isValidPlayer(player) || !isCompanionDressableTangible(item))
        {
            return false;
        }
        obj_id playerInv = utils.getInventoryContainer(player);
        if (!isIdValid(playerInv) || getContainedBy(item) != playerInv)
        {
            sendSystemMessage(player, string_id.unlocalized("That item must be in your inventory."));
            return false;
        }
        obj_id pet = callable.getCDCallable(pcd);
        if (isIdValid(pet) && exists(pet))
        {
            setupStoryCompanionPlayerAppearance(pet);
            obj_id appInv = getAppearanceInventory(pet);
            if (!isIdValid(appInv))
            {
                sendSystemMessage(player, string_id.unlocalized("This companion cannot equip appearance items (missing appearance slot)."));
                return false;
            }
            if (!canEquipWearable(pet, item))
            {
                sendSystemMessage(player, string_id.unlocalized("Your companion cannot wear that item."));
                return false;
            }
            if (!putIn(item, appInv, player))
            {
                sendSystemMessage(player, string_id.unlocalized("Could not move the item to your companion."));
                return false;
            }
            boolean ok = isWeapon(item) ? (equip(item, pet, "hold_r") || equip(item, pet, "hold_l")) : equip(item, pet);
            if (!ok)
            {
                sendSystemMessage(player, string_id.unlocalized("The item could not be equipped (species or slot mismatch)."));
                return false;
            }
            setObjVar(item, OBJVAR_BOUND_PCD, pcd);
            setObjVar(item, "noTrade", 1);
            sendSystemMessage(player, string_id.unlocalized("Equipped item on your companion."));
            return true;
        }
        obj_id hold = ensureCompanionGearHold(pcd, player);
        if (!isIdValid(hold) || !putIn(item, hold, player))
        {
            sendSystemMessage(player, string_id.unlocalized("Could not stash the item for your stored companion."));
            return false;
        }
        setObjVar(item, OBJVAR_BOUND_PCD, pcd);
        setObjVar(item, "noTrade", 1);
        sendSystemMessage(player, string_id.unlocalized("Item saved for your companion (call them out to wear it)."));
        return true;
    }
    public static boolean removeCompanionWearableToPlayer(obj_id pcd, obj_id player, obj_id item) throws InterruptedException
    {
        if (!canCustomizeStoryCompanionAppearance(pcd) || !beast_lib.isValidPlayer(player) || !isIdValid(item) || !exists(item))
        {
            return false;
        }
        if (hasObjVar(item, OBJVAR_BOUND_PCD))
        {
            obj_id boundPcd = getObjIdObjVar(item, OBJVAR_BOUND_PCD);
            if (boundPcd != pcd)
            {
                sendSystemMessage(player, string_id.unlocalized("That item is not bound to this companion."));
                return false;
            }
        }
        obj_id ownerInv = utils.getInventoryContainer(player);
        if (!isIdValid(ownerInv))
        {
            return false;
        }
        if (!moveItemFromCreatureToContainer(item, ownerInv, player))
        {
            if (!putIn(item, ownerInv, player))
            {
                sendSystemMessage(player, string_id.unlocalized("No room in your inventory for that item."));
                return false;
            }
        }
        removeObjVar(item, OBJVAR_BOUND_PCD);
        removeObjVar(item, "noTrade");
        sendSystemMessage(player, string_id.unlocalized("Removed item from your companion."));
        return true;
    }
    public static void saveStoryCompanionStateOnStore(obj_id pet, obj_id pcd, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet) || !isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        saveStoryCompanionWeaponOnStore(pet, pcd, player);
        saveStoryCompanionAppearanceOnStore(pet, pcd, player);
        if (canCustomizeStoryCompanionAppearance(pcd))
        {
            saveStoryCompanionCustomizationToPcd(pet, pcd);
        }
        applyStoryCompanionDisplayName(pcd, pet);
    }
    /** Post-summon setup: scripts, identity, appearance, weapon, name, customization. */
    public static void finishStoryCompanionSummon(obj_id player, obj_id pcd, obj_id pet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isStoryCompanionControlDevice(pcd) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        stripCreatureScriptsForStoryCompanion(pet);
        setObjVar(pet, "ai.pet.type", pet_lib.PET_TYPE_NPC);
        setObjVar(pet, "ai.pet", true);
        copyStoryCompanionIdentityFromPcdToPet(pcd, pet);
        applyStoryCompanionFactionFromOwner(player, pet);
        applyStoryCompanionLivePetStats(player, pcd, pet);
        applyStoryCompanionDisplayName(pcd, pet);
        if (canCustomizeStoryCompanionAppearance(pcd))
        {
            setupStoryCompanionPlayerAppearance(pet);
            restoreStoryCompanionCustomizationFromPcd(pet, pcd);
            restoreStoryCompanionAppearanceOnSummon(pet, pcd, player);
        }
        restoreStoryCompanionWeaponOnSummon(pet, pcd, player);
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
    /** Active called-out story companion pet, or {@link obj_id#NULL_ID}. */
    public static obj_id getActiveStoryCompanionPet(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return obj_id.NULL_ID;
        }
        obj_id pet = callable.getCallable(player, callable.CALLABLE_TYPE_COMBAT_PET);
        if (!isIdValid(pet) || !exists(pet) || !isStoryCompanionPet(pet))
        {
            return obj_id.NULL_ID;
        }
        return pet;
    }
    /** PCD for the active story companion pet, or {@link obj_id#NULL_ID}. */
    public static obj_id getActiveStoryCompanionPcd(obj_id player) throws InterruptedException
    {
        String companionId = getActiveStoryCompanionId(player);
        if (companionId == null || companionId.length() < 1)
        {
            return obj_id.NULL_ID;
        }
        return findStoryCompanionControlDevice(player, companionId);
    }
    /**
     * Applies {@code varPath=index} pairs separated by {@code ;} to the active companion pet and persists on the PCD.
     * Used by the client Companion tab Image Design screen.
     */
    public static boolean applyCompanionCustomizationParams(obj_id pet, obj_id pcd, String params) throws InterruptedException
    {
        if (!canCustomizeStoryCompanionAppearance(pcd) || !isIdValid(pet) || !exists(pet) || params == null || params.length() < 1)
        {
            return false;
        }
        String[] pairs = split(params, ';');
        if (pairs == null || pairs.length < 1)
        {
            return false;
        }
        String hairTemplate = null;
        boolean hairTemplateSet = false;
        boolean baldRequested = false;
        dictionary hairVars = new dictionary();
        boolean changed = false;
        for (int i = 0; i < pairs.length; ++i)
        {
            String pair = pairs[i];
            if (pair == null || pair.length() < 1)
            {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq <= 0)
            {
                continue;
            }
            String var = pair.substring(0, eq).trim();
            String valStr = pair.substring(eq + 1).trim();
            if (var.length() < 1)
            {
                continue;
            }
            if (var.equals("hairTemplate"))
            {
                hairTemplateSet = true;
                if (valStr.length() < 1)
                {
                    baldRequested = true;
                }
                else
                {
                    hairTemplate = valStr;
                }
                continue;
            }
            if (var.startsWith("hair/"))
            {
                if (valStr.length() < 1)
                {
                    continue;
                }
                hairVars.put(var.substring(5), utils.stringToInt(valStr));
                continue;
            }
            if (valStr.length() < 1)
            {
                continue;
            }
            int val = utils.stringToInt(valStr);
            if (var.indexOf("index_texture") >= 0 || var.endsWith("_texture_1"))
            {
                setRangedIntCustomVarValue(pet, var, val);
                changed = true;
            }
            else
            {
                if (hue.setColor(pet, var, val))
                {
                    changed = true;
                }
            }
        }
        if (hairTemplateSet)
        {
            if (baldRequested)
            {
                removeObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE);
                removeObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS);
                obj_id existingHair = getObjectInSlot(pet, slots.HAIR);
                if (isIdValid(existingHair) && exists(existingHair))
                {
                    destroyObject(existingHair);
                }
            }
            else if (hairTemplate != null && hairTemplate.length() > 0)
            {
                setObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_TEMPLATE, hairTemplate);
                obj_id existingHair = getObjectInSlot(pet, slots.HAIR);
                if (isIdValid(existingHair) && exists(existingHair) && !hairTemplate.equals(getTemplateName(existingHair)))
                {
                    destroyObject(existingHair);
                    existingHair = obj_id.NULL_ID;
                }
                obj_id hairObj = existingHair;
                if (!isIdValid(hairObj) || !exists(hairObj))
                {
                    hairObj = createObject(hairTemplate, pet, slots.HAIR);
                }
                if (isIdValid(hairObj) && exists(hairObj))
                {
                    removeObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS);
                    for (java.util.Enumeration e = hairVars.keys(); e.hasMoreElements();)
                    {
                        String hairVar = (String) e.nextElement();
                        int hairVal = hairVars.getInt(hairVar);
                        setObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS + "." + hairVar, hairVal);
                        if (hairVar.indexOf("index_texture") >= 0 || hairVar.endsWith("_texture_1"))
                        {
                            setRangedIntCustomVarValue(hairObj, normalizeCustomizationVarPath(hairVar), hairVal);
                        }
                        else if (!hue.setColor(hairObj, hairVar, hairVal))
                        {
                            setRangedIntCustomVarValue(hairObj, normalizeCustomizationVarPath(hairVar), hairVal);
                        }
                    }
                }
            }
            changed = true;
        }
        else if (!hairVars.isEmpty())
        {
            obj_id hairObj = getObjectInSlot(pet, slots.HAIR);
            if (isIdValid(hairObj) && exists(hairObj))
            {
                removeObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS);
                for (java.util.Enumeration e = hairVars.keys(); e.hasMoreElements();)
                {
                    String hairVar = (String) e.nextElement();
                    int hairVal = hairVars.getInt(hairVar);
                    setObjVar(pcd, OBJVAR_CUSTOMIZATION_HAIR_VARS + "." + hairVar, hairVal);
                    if (hairVar.indexOf("index_texture") >= 0 || hairVar.endsWith("_texture_1"))
                    {
                        setRangedIntCustomVarValue(hairObj, normalizeCustomizationVarPath(hairVar), hairVal);
                    }
                    else if (!hue.setColor(hairObj, hairVar, hairVal))
                    {
                        setRangedIntCustomVarValue(hairObj, normalizeCustomizationVarPath(hairVar), hairVal);
                    }
                }
                changed = true;
            }
        }
        if (changed)
        {
            utils.setScriptVar(pet, "customizationUpdated", 1);
            saveStoryCompanionCustomizationToPcd(pet, pcd);
        }
        return changed;
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
     * Applies refreshed PCD stats to a summoned story companion at full scale (no crafted-pet level 60 cap).
     */
    public static void applyStoryCompanionLivePetStats(obj_id player, obj_id pcd, obj_id pet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isStoryCompanionControlDevice(pcd) || !isIdValid(pet) || !exists(pet))
        {
            return;
        }
        applyStoryCompanionPcdStatsForPlayer(player, pcd);
        int level = getIntObjVar(pcd, "creature_attribs.level");
        if (level < 1)
        {
            level = 1;
        }
        int myHealth = getIntObjVar(pcd, "creature_attribs." + create.MAXATTRIBNAMES[HEALTH]);
        int healthRegen = getIntObjVar(pcd, "creature_attribs." + create.MAXATTRIBNAMES[CONSTITUTION]);
        int minDamage = getIntObjVar(pcd, "creature_attribs.minDamage");
        int maxDamage = getIntObjVar(pcd, "creature_attribs.maxDamage");
        int toHit = getIntObjVar(pcd, "creature_attribs.toHitChance") - 5;
        int defenseValue = getIntObjVar(pcd, "creature_attribs.defenseValue") - 5;
        int general_protection = getIntObjVar(pcd, "creature_attribs.general_protection");
        setLevel(pet, level);
        utils.setScriptVar(pet, "ai.level", level);
        if (myHealth < 1)
        {
            myHealth = 1;
        }
        setMaxAttrib(pet, HEALTH, myHealth);
        setAttrib(pet, HEALTH, myHealth);
        setMaxAttrib(pet, ACTION, 1000);
        setAttrib(pet, ACTION, 1000);
        setMaxAttrib(pet, MIND, 1000);
        setAttrib(pet, MIND, 1000);
        pet_lib.fixMinRegenStats(pet);
        if (healthRegen >= 1)
        {
            setRegenRate(pet, CONSTITUTION, 150);
        }
        int[] armorData = new int[10];
        armorData[1] = general_protection;
        create.initializeArmor(pet, armorData);
        create.applySkillStatisticModifiers(pet, toHit, defenseValue);
        utils.setScriptVar(pet, "ai.combat.minDamage", minDamage);
        utils.setScriptVar(pet, "ai.combat.maxDamage", maxDamage);
    }
    public static void resyncStoryCompanionLevelForActivePet(obj_id player) throws InterruptedException
    {
        obj_id pet = getPetBarCombatCreature(player);
        if (!isStoryCompanionPet(pet))
        {
            return;
        }
        obj_id pcd = pet_lib.getPetControlDevice(pet);
        if (!isStoryCompanionControlDevice(pcd))
        {
            return;
        }
        applyStoryCompanionPcdStatsForPlayer(player, pcd);
        applyStoryCompanionLivePetStats(player, pcd, pet);
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
        if (isPlayer(pet))
        {
            debugServerConsoleMsg(null, "WARNING: story companion is a player template and cannot use pet AI. Store/recall after re-grant or run migrate on PCD.");
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
        clearAiWeaponCombatProfiles(pet);
    }
    /** Grants all {@link #PLAYABLE_SPECIES_COMPANION_IDS} to the player datapad. */
    public static int grantPlayableSpeciesCompanionsToPlayer(obj_id player) throws InterruptedException
    {
        int granted = 0;
        for (int i = 0; i < PLAYABLE_SPECIES_COMPANION_IDS.length; ++i)
        {
            obj_id cd = grantStoryCompanionToDatapad(player, PLAYABLE_SPECIES_COMPANION_IDS[i]);
            if (isIdValid(cd))
            {
                granted++;
            }
        }
        return granted;
    }
    /** Grants every row in {@link #STORY_COMPANIONS_TABLE} (skips invalid rows). */
    public static int grantAllStoryCompanionsToPlayer(obj_id player) throws InterruptedException
    {
        int granted = 0;
        int rows = dataTableGetNumRows(STORY_COMPANIONS_TABLE);
        for (int i = 0; i < rows; ++i)
        {
            String id = dataTableGetString(STORY_COMPANIONS_TABLE, i, "companion_id");
            if (id == null || id.length() < 1 || !isValidStoryCompanionRow(id))
            {
                continue;
            }
            obj_id cd = grantStoryCompanionToDatapad(player, id);
            if (isIdValid(cd))
            {
                granted++;
            }
        }
        return granted;
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
            obj_id cd = findStoryCompanionControlDevice(player, companionId);
            if (isIdValid(cd))
            {
                migrateStoryCompanionPcdSpawnData(cd, player);
            }
            return cd;
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
        String customizationTier = getStoryCompanionCustomizationTier(companionId);
        location loc = getLocation(player);
        String spawnCreatureName = resolveStoryCompanionCreatureName(companionId, player);
        obj_id pet = null;
        if (CUSTOMIZATION_TIER_FULL.equals(customizationTier))
        {
            pet = spawnStoryCompanionCreature(companionId, player, loc, spawnLevel, true, true);
        }
        else
        {
            pet = create.createCreature(spawnCreatureName, loc, spawnLevel, true, true);
        }
        if (!isIdValid(pet) || !exists(pet))
        {
            return null;
        }
        stripCreatureScriptsForStoryCompanion(pet);
        setObjVar(pet, "ai.pet.type", pet_lib.PET_TYPE_NPC);
        setObjVar(pet, "ai.pet", true);
        setObjVar(pet, OBJVAR_STORY_COMPANION_ID, companionId);
        setObjVar(pet, OBJVAR_COMBAT_STANCE, stance);
        setMaster(pet, player);
        obj_id cd = pet_lib.makeControlDevice(player, pet);
        if (!isIdValid(cd) || !exists(cd))
        {
            destroyObject(pet);
            return null;
        }
        setObjVar(cd, "pet.creatureName", spawnCreatureName);
        setObjVar(pet, "pet.creatureName", spawnCreatureName);
        setObjVar(cd, OBJVAR_STORY_COMPANION_ID, companionId);
        setObjVar(cd, OBJVAR_COMBAT_STANCE, stance);
        if (CUSTOMIZATION_TIER_FULL.equals(customizationTier))
        {
            setObjVar(cd, pet_lib.VAR_PALVAR_BASE, 1);
        }
        String displayDefault = getStoryCompanionDisplayName(companionId);
        if (displayDefault != null && displayDefault.length() > 0)
        {
            setCompanionDisplayName(cd, displayDefault);
        }
        String[] taughtEmpty = 
        {
            "empty",
            "empty",
            "empty",
            "empty"
        };
        setObjVar(cd, OBJVAR_TAUGHT_ABILITIES, taughtEmpty);
        setObjVar(pet, OBJVAR_TAUGHT_ABILITIES, taughtEmpty);
        String[] coreDefaults = 
        {
            beast_lib.BM_COMMAND_ATTACK,
            beast_lib.BM_COMMAND_FOLLOW,
            beast_lib.BM_COMMAND_STAY
        };
        setObjVar(cd, OBJVAR_CORE_BAR_COMMANDS, coreDefaults);
        setObjVar(pet, OBJVAR_CORE_BAR_COMMANDS, coreDefaults);
        if (!hasScript(cd, "systems.companion.companion_story_pcd"))
        {
            attachScript(cd, "systems.companion.companion_story_pcd");
        }
        applyStoryCompanionPcdStatsForPlayer(player, cd);
        applyDefaultStoryCompanionWeapon(pet, companionId);
        if (CUSTOMIZATION_TIER_FULL.equals(customizationTier))
        {
            saveStoryCompanionCustomizationToPcd(pet, cd);
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
        if (hasObjVar(pcd, OBJVAR_CORE_BAR_COMMANDS))
        {
            copyObjVar(pcd, pet, OBJVAR_CORE_BAR_COMMANDS);
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
    public static void grantCompanionCoreBarUiCommands(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return;
        }
        for (int i = 0; i < COMPANION_CORE_BAR_WRAPPER_COMMANDS.length; ++i)
        {
            grantCommand(player, COMPANION_CORE_BAR_WRAPPER_COMMANDS[i]);
        }
    }
    public static void grantHumanoidCompanionOnlyBarCommands(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return;
        }
        for (int i = 0; i < HUMANOID_COMPANION_ONLY_BAR_COMMANDS.length; ++i)
        {
            grantCommand(player, HUMANOID_COMPANION_ONLY_BAR_COMMANDS[i]);
        }
    }
    /** Revokes every story-companion pet bar UI command (core wrappers + humanoid-only). */
    public static void revokeStoryCompanionPetBarUiCommands(obj_id player) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player))
        {
            return;
        }
        for (int i = 0; i < COMPANION_CORE_BAR_WRAPPER_COMMANDS.length; ++i)
        {
            revokeCommand(player, COMPANION_CORE_BAR_WRAPPER_COMMANDS[i]);
        }
        for (int i = 0; i < HUMANOID_COMPANION_ONLY_BAR_COMMANDS.length; ++i)
        {
            revokeCommand(player, HUMANOID_COMPANION_ONLY_BAR_COMMANDS[i]);
        }
    }
    public static void grantHumanoidCompanionBarCommands(obj_id player) throws InterruptedException
    {
        grantCompanionCoreBarUiCommands(player);
        grantHumanoidCompanionOnlyBarCommands(player);
    }
    public static void revokeHumanoidCompanionBarCommands(obj_id player) throws InterruptedException
    {
        revokeStoryCompanionPetBarUiCommands(player);
    }
    /** Slot placeholder commands for the four teachable humanoid bar positions (matches {@link #TAUGHT_SLOT_COUNT}). */
    public static String[] getHumanoidCompanionSlotPlaceholders()
    {
        return new String[]
        {
            CMD_BAR_SLOT_A,
            CMD_BAR_SLOT_B,
            CMD_BAR_SLOT_C,
            CMD_BAR_SLOT_D
        };
    }
    /**
     * Values for {@link beast_lib#PET_TRAINED_SKILLS_LIST}: the client and {@link beast_lib#canPerformCommand} use this list for
     * pet-bar slot labels/icons and for validating actions queued on the pet. Each slot must reflect a taught player command when set
     * (not only the {@code companion_bar_slot_*} placeholders), or the bar will not update and taught abilities will not validate.
     */
    public static String[] buildHumanoidStoryCompanionTrainedSkillsForPet(obj_id pet) throws InterruptedException
    {
        String[] cmds = getHumanoidCompanionSlotPlaceholders();
        String[] out = new String[4];
        String[] taught = getTaughtAbilitiesArray(pet);
        for (int i = 0; i < 4; ++i)
        {
            if (taught[i] != null && !taught[i].equals("empty"))
            {
                out[i] = taught[i];
            }
            else
            {
                out[i] = cmds[i];
            }
        }
        return out;
    }
    /**
     * Encodes a pet bar slot for the client UI bridge: {@code companion_bar_slot_a|meleeHit} shows meleeHit icon/tooltip but still executes {@code companion_bar_slot_a}.
     */
    public static String encodeCompanionBarSlotForClientUi(String slotCommand, String taughtOrEmpty) throws InterruptedException
    {
        if (slotCommand == null || slotCommand.length() < 1)
        {
            return "";
        }
        if (taughtOrEmpty == null || taughtOrEmpty.length() < 1 || taughtOrEmpty.equals("empty"))
        {
            return slotCommand;
        }
        if (taughtOrEmpty.indexOf(PET_BAR_CMD_DISPLAY_SEPARATOR) >= 0)
        {
            return slotCommand;
        }
        return slotCommand + PET_BAR_CMD_DISPLAY_SEPARATOR + taughtOrEmpty;
    }
    /** String table for localized command names in SUI listboxes ({@code cmd_n}, same as beastmaster training UI). */
    public static final String CMD_NAME_STRING_TABLE = "cmd_n";
    /** Client tooltip table ({@code StringTables::Cmd::descs}). */
    public static final String CMD_DESC_STRING_TABLE = "cmd/descs";
    /**
     * Localized listbox row for a teachable command; mirrors {@link pet_lib#createLearnCommandListEntry}.
     */
    public static String createCompanionCommandListEntry(String commandName) throws InterruptedException
    {
        if (commandName == null || commandName.length() < 1)
        {
            return "";
        }
        prose_package pp = new prose_package();
        pp.stringId = new string_id("pet/pet_ability", "learn_command_list_entry");
        pp.actor.set(new string_id(CMD_NAME_STRING_TABLE, commandName));
        pp.target.set(" ");
        return " \0" + packOutOfBandProsePackage(null, pp);
    }
    /** Builds a {@code prose_package[]} listbox source with localized command names. */
    public static prose_package[] buildCommandProseList(String[] commands) throws InterruptedException
    {
        if (commands == null || commands.length < 1)
        {
            return new prose_package[0];
        }
        prose_package[] rows = new prose_package[commands.length];
        for (int i = 0; i < commands.length; ++i)
        {
            rows[i] = prose.getPackage(new string_id(CMD_NAME_STRING_TABLE, commands[i]));
        }
        return rows;
    }
    /** Slot picker row showing slot index and current assignment (localized command name or Empty). */
    public static String createCompanionBarSlotPickerEntry(String slotLabel, String commandOrEmpty) throws InterruptedException
    {
        if (commandOrEmpty == null || commandOrEmpty.length() < 1 || commandOrEmpty.equals("empty"))
        {
            return slotLabel + " (Empty)";
        }
        prose_package pp = new prose_package();
        pp.stringId = new string_id("pet/pet_ability", "learn_command_list_entry");
        pp.actor.set(new string_id(CMD_NAME_STRING_TABLE, commandOrEmpty));
        pp.target.set(" - " + slotLabel);
        return " \0" + packOutOfBandProsePackage(null, pp);
    }
    public static String[] buildHumanoidStoryCompanionPetBar(obj_id player, obj_id pet) throws InterruptedException
    {
        String[] barData = (String[])beast_lib.PET_BAR_DEFAULT_ARRAY.clone();
        barData[7] = beast_lib.BM_COMMAND_DISABLED;
        barData[8] = beast_lib.BM_COMMAND_DISABLED;
        String[] coreCmd = getCoreBarCommandsArray(pet);
        for (int i = 0; i < CORE_BAR_SLOT_COUNT; ++i)
        {
            String w = getCoreBarWrapperCommandForIndex(i);
            String c = coreCmd[i];
            if (c == null || c.equals("empty"))
            {
                barData[i] = "empty";
            }
            else
            {
                barData[i] = encodeCompanionBarSlotForClientUi(w, c);
            }
        }
        String[] cmds = getHumanoidCompanionSlotPlaceholders();
        String[] taught = getTaughtAbilitiesArray(pet);
        for (int i = 0; i < 4; ++i)
        {
            if (taught[i] == null || taught[i].equals("empty"))
            {
                barData[3 + i] = "empty";
            }
            else
            {
                barData[3 + i] = encodeCompanionBarSlotForClientUi(cmds[i], taught[i]);
            }
        }
        return barData;
    }
    /**
     * Owner drags a weapon onto the story companion: equip it; previous non-creature weapon goes to the owner's inventory.
     */
    public static boolean handleStoryCompanionWeaponGift(obj_id pet, obj_id item, obj_id giver) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet) || !isIdValid(item) || !exists(item) || !isWeapon(item))
        {
            return false;
        }
        if (!beast_lib.isValidPlayer(giver))
        {
            return false;
        }
        obj_id master = getMaster(pet);
        if (!isIdValid(master) || giver != master)
        {
            return false;
        }
        obj_id ownerInv = utils.getInventoryContainer(giver);
        if (!isIdValid(ownerInv))
        {
            return false;
        }
        obj_id cur = getCurrentWeapon(pet);
        if (!isIdValid(cur))
        {
            cur = getDefaultWeapon(pet);
        }
        if (isIdValid(cur) && exists(cur) && cur != item)
        {
            if (!isCompanionCreatureWeapon(cur))
            {
                if (!putIn(cur, ownerInv, giver))
                {
                    sendSystemMessage(giver, string_id.unlocalized("No room in your inventory for the companion's old weapon."));
                    return false;
                }
            }
        }
        obj_id petInv = utils.getInventoryContainer(pet);
        if (isIdValid(petInv))
        {
            if (getContainedBy(item) != petInv)
            {
                if (!putIn(item, petInv, giver))
                {
                    sendSystemMessage(giver, string_id.unlocalized("Could not move the weapon to your companion."));
                    return false;
                }
            }
        }
        else
        {
            if (!equip(item, pet))
            {
                sendSystemMessage(giver, string_id.unlocalized("Could not equip the weapon on your companion."));
                return false;
            }
        }
        if (!setCurrentWeapon(pet, item))
        {
            sendSystemMessage(giver, string_id.unlocalized("Your companion could not equip that weapon."));
            clearAiWeaponCombatProfiles(pet);
            return true;
        }
        clearAiWeaponCombatProfiles(pet);
        obj_id pcd = pet_lib.getPetControlDevice(pet);
        if (isStoryCompanionControlDevice(pcd))
        {
            removeObjVar(pcd, OBJVAR_STORED_WEAPON);
        }
        sendSystemMessage(giver, string_id.unlocalized("Your companion equips the new weapon; the old one was moved to your inventory."));
        return true;
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
        if (raw == null || raw.length < 1)
        {
            return def;
        }
        for (int i = 0; i < TAUGHT_SLOT_COUNT; ++i)
        {
            if (i < raw.length && raw[i] != null && raw[i].length() > 0 && !raw[i].equals("empty"))
            {
                def[i] = raw[i];
            }
        }
        return def;
    }
    public static String getCoreBarWrapperCommandForIndex(int index)
    {
        switch (index)
        {
            case 0:
            return CMD_BAR_CORE_SLOT_0;
            case 1:
            return CMD_BAR_CORE_SLOT_1;
            case 2:
            return CMD_BAR_CORE_SLOT_2;
            default:
            return "";
        }
    }
    public static String[] getCoreBarCommandsArray(obj_id obj) throws InterruptedException
    {
        if (!isIdValid(obj) || !exists(obj) || !hasObjVar(obj, OBJVAR_CORE_BAR_COMMANDS))
        {
            return new String[]
            {
                beast_lib.BM_COMMAND_ATTACK,
                beast_lib.BM_COMMAND_FOLLOW,
                beast_lib.BM_COMMAND_STAY
            };
        }
        String[] def = new String[CORE_BAR_SLOT_COUNT];
        for (int i = 0; i < CORE_BAR_SLOT_COUNT; ++i)
        {
            def[i] = "empty";
        }
        String[] raw = getStringArrayObjVar(obj, OBJVAR_CORE_BAR_COMMANDS);
        if (raw == null || raw.length < 1)
        {
            return def;
        }
        for (int i = 0; i < CORE_BAR_SLOT_COUNT; ++i)
        {
            if (i < raw.length && raw[i] != null && raw[i].length() > 0)
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
        if (commandName.indexOf(PET_BAR_CMD_DISPLAY_SEPARATOR) >= 0)
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
    /**
     * Core bar slots accept standard pet / beastmaster commands (and any command the strict ability teach path allows).
     */
    public static boolean canPlayerTeachCoreBarCommand(obj_id player, String commandName) throws InterruptedException
    {
        if (canPlayerTeachCommandToCompanion(player, commandName))
        {
            return true;
        }
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
        if (commandName.indexOf(PET_BAR_CMD_DISPLAY_SEPARATOR) >= 0)
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
        if (hook != null && hook.length() > 0)
        {
            return true;
        }
        return commandName.startsWith("bm_") || commandName.startsWith("pet");
    }
    public static String[] getCompanionCoreBarTrainableCommandList(obj_id player) throws InterruptedException
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
            if (canPlayerTeachCoreBarCommand(player, c))
            {
                v.addElement(c);
            }
        }
        String[] out = new String[v.size()];
        v.copyInto(out);
        Arrays.sort(out);
        return out;
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
            prose_package msg = prose.getPackage(new string_id("pet/pet_ability", "learn_command_list_entry"));
            msg.actor.set(new string_id(CMD_NAME_STRING_TABLE, normalized));
            msg.target.set(string_id.unlocalized(" (pet bar slot " + (slot + 1) + ", companion cooldown)"));
            sendSystemMessageProse(player, msg);
        }
    }
    public static void setCoreBarCommandOnPcd(obj_id pcd, int slot, String commandName, obj_id player) throws InterruptedException
    {
        if (!isStoryCompanionControlDevice(pcd) || slot < 0 || slot >= CORE_BAR_SLOT_COUNT || !beast_lib.isValidPlayer(player))
        {
            return;
        }
        String normalized = commandName == null ? "empty" : commandName.trim();
        if (normalized.length() < 1)
        {
            normalized = "empty";
        }
        if (!normalized.equals("empty") && !canPlayerTeachCoreBarCommand(player, normalized))
        {
            sendSystemMessage(player, string_id.unlocalized("You cannot assign that command to this companion core bar slot."));
            return;
        }
        String[] c = getCoreBarCommandsArray(pcd);
        c[slot] = normalized;
        setObjVar(pcd, OBJVAR_CORE_BAR_COMMANDS, c);
        obj_id pet = callable.getCDCallable(pcd);
        if (isIdValid(pet) && exists(pet) && isStoryCompanionPet(pet))
        {
            setObjVar(pet, OBJVAR_CORE_BAR_COMMANDS, c);
            refreshStoryCompanionPetBar(player, pet);
        }
        if (normalized.equals("empty"))
        {
            sendSystemMessage(player, string_id.unlocalized("Companion core bar slot " + (slot + 1) + " cleared."));
        }
        else
        {
            prose_package msg = prose.getPackage(new string_id("pet/pet_ability", "learn_command_list_entry"));
            msg.actor.set(new string_id(CMD_NAME_STRING_TABLE, normalized));
            msg.target.set(string_id.unlocalized(" (core bar slot " + (slot + 1) + ")"));
            sendSystemMessageProse(player, msg);
        }
    }
    public static void executeCompanionCoreBarSlot(obj_id player, int slotIndex) throws InterruptedException
    {
        if (slotIndex < 0 || slotIndex >= CORE_BAR_SLOT_COUNT)
        {
            return;
        }
        obj_id pet = getPetBarCombatCreature(player);
        if (!isStoryCompanionPet(pet))
        {
            sendSystemMessage(player, string_id.unlocalized("No story companion is active."));
            return;
        }
        String[] core = getCoreBarCommandsArray(pet);
        String cmd = core[slotIndex];
        if (cmd == null || cmd.equals("empty"))
        {
            sendSystemMessage(player, string_id.unlocalized("Assign a command to this core slot from the companion control device."));
            return;
        }
        if (!isCompanionAbilityOffCooldown(pet, cmd))
        {
            sendSystemMessage(player, string_id.unlocalized("That companion core bar command is still recharging."));
            return;
        }
        if (cmd.equals(beast_lib.BM_COMMAND_ATTACK) || cmd.equals("bm_pet_attack_1"))
        {
            pet_lib.doAttackCommand(pet, player);
            setCompanionAbilityCooldown(pet, cmd, getCompanionAbilityCooldownSeconds(cmd));
            return;
        }
        if (cmd.equals(beast_lib.BM_COMMAND_FOLLOW) || cmd.equals("bm_follow_1"))
        {
            beast_lib.doFollowCommand(pet, player);
            setCompanionAbilityCooldown(pet, cmd, getCompanionAbilityCooldownSeconds(cmd));
            return;
        }
        if (cmd.equals(beast_lib.BM_COMMAND_STAY) || cmd.equals("bm_stay_1"))
        {
            beast_lib.doStayCommand(pet, player);
            setCompanionAbilityCooldown(pet, cmd, getCompanionAbilityCooldownSeconds(cmd));
            return;
        }
        if (isValidBeastSpecialForStoryPetBar(cmd))
        {
            obj_id target = getIntendedTarget(pet);
            if (!isIdValid(target))
            {
                target = getIntendedTarget(player);
            }
            if (!isIdValid(target))
            {
                target = getLookAtTarget(player);
            }
            if (!isIdValid(target))
            {
                target = player;
            }
            queueCommand(pet, getStringCrc(cmd.toLowerCase()), target, "", COMMAND_PRIORITY_DEFAULT);
            setCompanionAbilityCooldown(pet, cmd, getCompanionAbilityCooldownSeconds(cmd));
            return;
        }
        if (companion_combat_helper.castAbilityFromCompanionBar(player, cmd, obj_id.NULL_ID))
        {
            setCompanionAbilityCooldown(pet, cmd, getCompanionAbilityCooldownSeconds(cmd));
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
        if (!isCompanionAbilityOffCooldown(pet, skill))
        {
            sendSystemMessage(player, string_id.unlocalized("That companion ability is still recharging."));
            return;
        }
        if (companion_combat_helper.castAbilityFromCompanionBar(player, skill, obj_id.NULL_ID))
        {
            setCompanionAbilityCooldown(pet, skill, getCompanionAbilityCooldownSeconds(skill));
        }
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
        barData[7] = beast_lib.BM_COMMAND_DISABLED;
        barData[8] = beast_lib.BM_COMMAND_DISABLED;
        String[] coreCmd = getCoreBarCommandsArray(pet);
        for (int i = 0; i < CORE_BAR_SLOT_COUNT; ++i)
        {
            String w = getCoreBarWrapperCommandForIndex(i);
            String c = coreCmd[i];
            if (c == null || c.equals("empty"))
            {
                barData[i] = "empty";
            }
            else
            {
                barData[i] = encodeCompanionBarSlotForClientUi(w, c);
            }
        }
        for (int i = 0; i < 4; ++i)
        {
            String s = "empty";
            if (knownSkills != null && i < knownSkills.length && knownSkills[i] != null && knownSkills[i].length() > 0 && !knownSkills[i].equals("empty"))
            {
                s = knownSkills[i];
            }
            barData[3 + i] = s.equals("empty") ? "empty" : s;
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
        revokeHumanoidCompanionBarCommands(player);
        grantCompanionCoreBarUiCommands(player);
        if (usesHumanoidStoryCompanionPetBar(pet))
        {
            grantHumanoidCompanionOnlyBarCommands(player);
            String[] trained = buildHumanoidStoryCompanionTrainedSkillsForPet(pet);
            setObjVar(pet, beast_lib.PET_TRAINED_SKILLS_LIST, trained);
            String[] bar = buildHumanoidStoryCompanionPetBar(player, pet);
            setBeastmasterPet(player, pet);
            setBeastmasterPetCommands(player, bar);
        }
        else
        {
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
        syncCompanionPetStanceUiToPlayer(player, pet);
    }
    /**
     * Pushes {@link #OBJVAR_COMBAT_STANCE} to the client via {@link script.base_class#setCompanionPetStanceUi} for the pet bar role icon.
     */
    private static void syncCompanionPetStanceUiToPlayer(obj_id player, obj_id pet) throws InterruptedException
    {
        if (!beast_lib.isValidPlayer(player) || !isIdValid(pet))
        {
            return;
        }
        int stance = STANCE_DPS;
        if (hasObjVar(pet, OBJVAR_COMBAT_STANCE))
        {
            stance = getIntObjVar(pet, OBJVAR_COMBAT_STANCE);
        }
        if (stance < STANCE_TANK || stance > STANCE_DPS)
        {
            stance = STANCE_DPS;
        }
        setCompanionPetStanceUi(player, stance);
    }

    /** Per-ability cooldown objvars on the pet (separate from the master's command timers). */
    public static final String OBJVAR_ABILITY_COOLDOWN_PREFIX = "companion.cooldown.";
    public static final float COMPANION_COMBAT_TICK_INTERVAL = 2.0f;
    public static final String MESSAGE_COMPANION_COMBAT_TICK = "companionCombatTick";
    public static final String SCRIPTVAR_COMPANION_COMBAT_LOOP = "companion.combatLoopActive";

    public static void startStoryCompanionCombatLoop(obj_id pet) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet))
        {
            return;
        }
        if (utils.hasScriptVar(pet, SCRIPTVAR_COMPANION_COMBAT_LOOP))
        {
            return;
        }
        utils.setScriptVar(pet, SCRIPTVAR_COMPANION_COMBAT_LOOP, 1);
        messageTo(pet, MESSAGE_COMPANION_COMBAT_TICK, null, COMPANION_COMBAT_TICK_INTERVAL, false);
    }

    public static void stopStoryCompanionCombatLoop(obj_id pet) throws InterruptedException
    {
        utils.removeScriptVar(pet, SCRIPTVAR_COMPANION_COMBAT_LOOP);
    }

    public static int companionCombatTick(obj_id pet, dictionary params) throws InterruptedException
    {
        if (!isStoryCompanionPet(pet) || !isIdValid(pet) || !exists(pet) || isDead(pet))
        {
            stopStoryCompanionCombatLoop(pet);
            return SCRIPT_CONTINUE;
        }
        obj_id master = getMaster(pet);
        if (!isIdValid(master) || !exists(master) || isDead(master))
        {
            stopStoryCompanionCombatLoop(pet);
            return SCRIPT_CONTINUE;
        }

        messageTo(pet, MESSAGE_COMPANION_COMBAT_TICK, null, COMPANION_COMBAT_TICK_INTERVAL, false);

        if (ai_lib.isInCombat(master))
        {
            obj_id masterTarget = getIntendedTarget(master);
            if (!isIdValid(masterTarget))
            {
                masterTarget = getLookAtTarget(master);
            }
            if (isIdValid(masterTarget) && exists(masterTarget) && !isDead(masterTarget) && masterTarget != pet && masterTarget != master)
            {
                if (!ai_lib.isInCombat(pet) || getIntendedTarget(pet) != masterTarget)
                {
                    addHate(pet, masterTarget, 1.0f);
                }
            }
        }

        if (ai_lib.isInCombat(pet))
        {
            tryUseAutonomousCompanionAbility(pet);
        }
        return SCRIPT_CONTINUE;
    }

    public static void onMasterDefended(obj_id master, obj_id attacker) throws InterruptedException
    {
        obj_id pet = getPetBarCombatCreature(master);
        if (!isStoryCompanionPet(pet))
        {
            return;
        }
        if (!isIdValid(attacker) || !exists(attacker) || isDead(attacker) || attacker == pet || attacker == master)
        {
            return;
        }
        if (!ai_lib.isInCombat(pet))
        {
            addHate(pet, attacker, 2.0f);
        }
        startStoryCompanionCombatLoop(pet);
    }

    public static boolean isCompanionAbilityOffCooldown(obj_id pet, String abilityName) throws InterruptedException
    {
        if (abilityName == null || abilityName.length() < 1 || abilityName.equals("empty"))
        {
            return false;
        }
        String key = OBJVAR_ABILITY_COOLDOWN_PREFIX + abilityName;
        if (!hasObjVar(pet, key))
        {
            return true;
        }
        return getIntObjVar(pet, key) <= getGameTime();
    }

    public static void setCompanionAbilityCooldown(obj_id pet, String abilityName, int durationSec) throws InterruptedException
    {
        if (durationSec < 1)
        {
            durationSec = 1;
        }
        setObjVar(pet, OBJVAR_ABILITY_COOLDOWN_PREFIX + abilityName, getGameTime() + durationSec);
    }

    public static int getCompanionAbilityCooldownSeconds(String abilityName) throws InterruptedException
    {
        if (abilityName == null || abilityName.length() < 1)
        {
            return 8;
        }
        int row = dataTableSearchColumnForString(abilityName, "commandName", COMMAND_TABLE_PATH);
        if (row < 0)
        {
            row = dataTableSearchColumnForString(abilityName.toLowerCase(), "commandName", COMMAND_TABLE_PATH);
        }
        if (row >= 0)
        {
            float cd = dataTableGetFloat(COMMAND_TABLE_PATH, row, "defaultTime");
            if (cd > 0f)
            {
                return (int) cd;
            }
        }
        if (isValidBeastSpecialForStoryPetBar(abilityName))
        {
            int beastRow = dataTableSearchColumnForString(abilityName, "ability_name", beast_lib.BEASTS_SPECIALS);
            if (beastRow >= 0)
            {
                float cd = dataTableGetFloat(beast_lib.BEASTS_SPECIALS, beastRow, "cooldown");
                if (cd > 0f)
                {
                    return (int) cd;
                }
            }
        }
        return 8;
    }

    public static void tryUseAutonomousCompanionAbility(obj_id pet) throws InterruptedException
    {
        obj_id target = getIntendedTarget(pet);
        if (!isIdValid(target))
        {
            target = getLookAtTarget(pet);
        }
        if (!isIdValid(target) || !exists(target) || isDead(target))
        {
            return;
        }

        int stance = STANCE_DPS;
        if (hasObjVar(pet, OBJVAR_COMBAT_STANCE))
        {
            stance = getIntObjVar(pet, OBJVAR_COMBAT_STANCE);
        }

        String[] abilities = pickAutonomousAbilitiesForStance(pet, stance);
        if (abilities == null)
        {
            return;
        }

        for (int i = 0; i < abilities.length; ++i)
        {
            String skill = abilities[i];
            if (skill == null || skill.length() < 1 || skill.equals("empty"))
            {
                continue;
            }
            if (!isCompanionAbilityOffCooldown(pet, skill))
            {
                continue;
            }
            if (isValidBeastSpecialForStoryPetBar(skill))
            {
                queueCommand(pet, getStringCrc(skill.toLowerCase()), target, "", COMMAND_PRIORITY_DEFAULT);
            }
            else if (companion_combat_helper.castAbilityFromCompanionBar(getMaster(pet), skill, target))
            {
                // cast through combat helper (testPetBar redirects to companion)
            }
            else
            {
                continue;
            }
            setCompanionAbilityCooldown(pet, skill, getCompanionAbilityCooldownSeconds(skill));
            return;
        }
    }

    private static String[] pickAutonomousAbilitiesForStance(obj_id pet, int stance) throws InterruptedException
    {
        String[] source;
        if (usesHumanoidStoryCompanionPetBar(pet))
        {
            source = getTaughtAbilitiesArray(pet);
        }
        else
        {
            String companionId = getStringObjVar(pet, OBJVAR_STORY_COMPANION_ID);
            source = getStoryCompanionTrainedSkillsFromTable(companionId);
        }
        if (source == null || source.length < 1)
        {
            return source;
        }

        // Stance ordering: tank prefers first slots, healer middle, dps all — simple spec bias without new datatable columns.
        if (stance == STANCE_TANK && source.length > 1)
        {
            return new String[]
            {
                source[0],
                source.length > 1 ? source[1] : "empty"
            };
        }
        if (stance == STANCE_HEALER && source.length > 2)
        {
            return new String[]
            {
                source[1],
                source[2]
            };
        }
        return source;
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
