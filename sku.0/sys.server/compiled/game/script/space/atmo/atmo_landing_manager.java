package script.space.atmo;

import script.*;
import script.library.*;

import java.util.Vector;

/**
 * Policy and planet-map routing for atmospheric landing platforms (vertical / pad gameplay).
 * <p>
 * Extends the base {@code atmo.landing_point.*} schema with map category/subcategory,
 * access rules, fee overrides, dock duration overrides, and extend-dock tuning.
 * <p>
 * {@code atmo.landing_point.map.*} objvars are optional metadata for scripts (filtering, tools). They are
 * not passed to {@code addPlanetaryMapLocation} -- the engine only accepts categories/subcategories defined
 * in {@code planet_map_cat.tab} (currently only {@code atmo_landing} with an empty subcategory).
 */
public class atmo_landing_manager extends script.base_script
{
    /**
     * Optional script-only tags (e.g. summon UI filters). Not used for planet map registration.
     */
    public static final String OBJVAR_MAP = atmo_landing_registry.OBJVAR_ROOT + ".map";
    public static final String OBJVAR_MAP_CATEGORY = OBJVAR_MAP + ".category";
    public static final String OBJVAR_MAP_SUBCATEGORY = OBJVAR_MAP + ".subcategory";

    /** Root for access, fees, and duration policy. */
    public static final String OBJVAR_POLICY = atmo_landing_registry.OBJVAR_ROOT + ".policy";
    public static final String OBJVAR_ACCESS_MODE = OBJVAR_POLICY + ".access_mode";
    /** When true, only the ship's owner may request landing ({@code pilot == getOwner(ship)}). */
    public static final String OBJVAR_OWNER_PILOT_ONLY = OBJVAR_POLICY + ".owner_pilot_only";
    /** Aligned faction ids ({@code pvpGetAlignedFaction}) allowed when mode is {@link #ACCESS_FACTION_WHITELIST}. */
    public static final String OBJVAR_ALLOWED_FACTIONS = OBJVAR_POLICY + ".allowed_factions";
    /** Guild id required when mode is {@link #ACCESS_GUILD_ONLY}. */
    public static final String OBJVAR_GUILD_ID = OBJVAR_POLICY + ".guild_id";
    /** Explicit player allow-list when mode is {@link #ACCESS_PLAYER_ALLOWLIST}. */
    public static final String OBJVAR_ALLOWED_PLAYERS = OBJVAR_POLICY + ".allowed_players";
    /** Optional: pilot must have this skill name ({@link #hasSkill}). */
    public static final String OBJVAR_REQUIRED_SKILL = OBJVAR_POLICY + ".required_skill";
    /** Optional: minimum combat level ({@link #getLevel}). */
    public static final String OBJVAR_MIN_LEVEL = OBJVAR_POLICY + ".min_level";

    /**
     * Optional extra gate: pilot must be in a guild whose abbreviation or full name equals this (case-insensitive).
     * Runs in addition to {@link #ACCESS_GUILD_ONLY} / {@link #OBJVAR_GUILD_ID}.
     */
    public static final String OBJVAR_REQUIRED_GUILD_TAG = OBJVAR_POLICY + ".required_guild_tag";
    /**
     * Optional aligned-faction gate ({@link #pvpGetAlignedFaction}): {@code rebel}, {@code imperial}, or {@code neutral}.
     */
    public static final String OBJVAR_REQUIRED_ALIGNED_FACTION_NAME = OBJVAR_POLICY + ".required_aligned_faction";
    /** Optional: pilot must match at least one profession ({@code utils.isProfession}); ints match {@code utils.COMMANDO}, etc. */
    public static final String OBJVAR_REQUIRED_PROFESSIONS_ANY = OBJVAR_POLICY + ".required_professions_any";

    public static final String OBJVAR_LANDING_FEE = OBJVAR_POLICY + ".landing_fee_credits";
    public static final String OBJVAR_IGNORE_CITY_LANDING_TAX = OBJVAR_POLICY + ".ignore_city_landing_tax";
    public static final String OBJVAR_WAIVE_LANDING_FEE = OBJVAR_POLICY + ".waive_landing_fee";

    /**
     * If set, overrides {@code atmo.landing_point.time_to_disembark} for effective dock seconds.
     * Use {@code -1} for unlimited (same semantics as registry).
     */
    public static final String OBJVAR_DOCK_DURATION_OVERRIDE = OBJVAR_POLICY + ".dock_duration_seconds";
    public static final String OBJVAR_ALLOW_EXTEND_DOCK = OBJVAR_POLICY + ".allow_extend_dock";
    public static final String OBJVAR_EXTEND_DOCK_CREDITS = OBJVAR_POLICY + ".extend_dock_credits";
    public static final String OBJVAR_EXTEND_DOCK_SECONDS = OBJVAR_POLICY + ".extend_dock_seconds";
    /**
     * Extra seconds after {@code atmo.landing.dockExpiry} before forced relocation; extend still works during this buffer.
     * Displayed "time until forced departure" includes this grace.
     */
    public static final String OBJVAR_DOCK_GRACE_SECONDS = OBJVAR_POLICY + ".dock_grace_seconds";
    public static final int DEFAULT_DOCK_GRACE_SECONDS = 120;

    public static final int ACCESS_PUBLIC = 0;
    public static final int ACCESS_FACTION_WHITELIST = 1;
    public static final int ACCESS_GUILD_ONLY = 2;
    public static final int ACCESS_PLAYER_ALLOWLIST = 3;
    public static final int ACCESS_GM_ONLY = 4;

    public static String getMapCategory(obj_id landingPoint) throws InterruptedException
    {
        if (hasObjVar(landingPoint, OBJVAR_MAP_CATEGORY))
            return getStringObjVar(landingPoint, OBJVAR_MAP_CATEGORY);
        return atmo_landing_registry.MAP_CATEGORY;
    }

    public static String getMapSubcategory(obj_id landingPoint) throws InterruptedException
    {
        if (hasObjVar(landingPoint, OBJVAR_MAP_SUBCATEGORY))
            return getStringObjVar(landingPoint, OBJVAR_MAP_SUBCATEGORY);
        return atmo_landing_registry.MAP_SUBCATEGORY;
    }

    public static int getAccessMode(obj_id landingPoint) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(landingPoint))
            return ACCESS_PUBLIC;
        if (hasObjVar(landingPoint, OBJVAR_ACCESS_MODE))
            return getIntObjVar(landingPoint, OBJVAR_ACCESS_MODE);
        return ACCESS_PUBLIC;
    }

    public static boolean isOwnerPilotOnly(obj_id landingPoint) throws InterruptedException
    {
        return atmo_landing_registry.isLandingPoint(landingPoint)
            && hasObjVar(landingPoint, OBJVAR_OWNER_PILOT_ONLY)
            && getBooleanObjVar(landingPoint, OBJVAR_OWNER_PILOT_ONLY);
    }

    /**
     * Effective dock duration in seconds; {@code -1} means unlimited.
     */
    public static int getEffectiveDockDurationSeconds(obj_id landingPoint) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(landingPoint))
            return -1;
        if (hasObjVar(landingPoint, OBJVAR_DOCK_DURATION_OVERRIDE))
            return getIntObjVar(landingPoint, OBJVAR_DOCK_DURATION_OVERRIDE);
        return atmo_landing_registry.getTimeToDisembark(landingPoint);
    }

    public static boolean allowsExtendDock(obj_id landingPoint) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(landingPoint))
            return true;
        if (hasObjVar(landingPoint, OBJVAR_ALLOW_EXTEND_DOCK))
            return getBooleanObjVar(landingPoint, OBJVAR_ALLOW_EXTEND_DOCK);
        return true;
    }

    public static int getExtendDockCredits(obj_id landingPoint) throws InterruptedException
    {
        if (atmo_landing_registry.isLandingPoint(landingPoint) && hasObjVar(landingPoint, OBJVAR_EXTEND_DOCK_CREDITS))
            return getIntObjVar(landingPoint, OBJVAR_EXTEND_DOCK_CREDITS);
        return -1;
    }

    public static int getExtendDockSeconds(obj_id landingPoint) throws InterruptedException
    {
        if (atmo_landing_registry.isLandingPoint(landingPoint) && hasObjVar(landingPoint, OBJVAR_EXTEND_DOCK_SECONDS))
            return getIntObjVar(landingPoint, OBJVAR_EXTEND_DOCK_SECONDS);
        return -1;
    }

    /** @return seconds after {@code dockExpiry} before forced departure; {@code 0} if explicitly set on the egg/pad. */
    public static int getDockGraceSeconds(obj_id padOrEgg) throws InterruptedException
    {
        if (!isIdValid(padOrEgg) || !exists(padOrEgg))
            return DEFAULT_DOCK_GRACE_SECONDS;
        if (hasObjVar(padOrEgg, OBJVAR_DOCK_GRACE_SECONDS))
            return Math.max(0, getIntObjVar(padOrEgg, OBJVAR_DOCK_GRACE_SECONDS));
        return DEFAULT_DOCK_GRACE_SECONDS;
    }

    public static boolean shouldWaiveLandingFee(obj_id landingPoint) throws InterruptedException
    {
        return atmo_landing_registry.isLandingPoint(landingPoint)
            && hasObjVar(landingPoint, OBJVAR_WAIVE_LANDING_FEE)
            && getBooleanObjVar(landingPoint, OBJVAR_WAIVE_LANDING_FEE);
    }

    public static boolean shouldIgnoreCityLandingTax(obj_id landingPoint) throws InterruptedException
    {
        return atmo_landing_registry.isLandingPoint(landingPoint)
            && hasObjVar(landingPoint, OBJVAR_IGNORE_CITY_LANDING_TAX)
            && getBooleanObjVar(landingPoint, OBJVAR_IGNORE_CITY_LANDING_TAX);
    }

    /**
     * Base landing fee for this pad, or {@code -1} to use script default ({@code atmo_landing_point.MINIMUM_LANDING_FEE}).
     */
    public static int getConfiguredLandingFeeCredits(obj_id landingPoint) throws InterruptedException
    {
        if (atmo_landing_registry.isLandingPoint(landingPoint) && hasObjVar(landingPoint, OBJVAR_LANDING_FEE))
            return getIntObjVar(landingPoint, OBJVAR_LANDING_FEE);
        return -1;
    }

    /**
     * Credits charged for initial landing (before city tax). Does not apply waivers -- caller checks {@link #shouldWaiveLandingFee}.
     */
    public static int resolveBaseLandingFeeCredits(obj_id landingPoint, int scriptDefaultMinimum) throws InterruptedException
    {
        int configured = getConfiguredLandingFeeCredits(landingPoint);
        if (configured >= 0)
            return configured;
        return scriptDefaultMinimum;
    }

    public static obj_id resolveLandingPointFromShip(obj_id ship) throws InterruptedException
    {
        if (!isIdValid(ship) || !exists(ship))
            return null;
        if (hasObjVar(ship, "atmo.landing.target"))
        {
            obj_id t = getObjIdObjVar(ship, "atmo.landing.target");
            if (isIdValid(t) && exists(t))
                return t;
        }
        if (hasObjVar(ship, "atmo.landing.landed_at"))
        {
            obj_id t = getObjIdObjVar(ship, "atmo.landing.landed_at");
            if (isIdValid(t) && exists(t))
                return t;
        }
        return null;
    }

    public static boolean canPilotLandAt(obj_id landingPoint, obj_id pilot, obj_id ship) throws InterruptedException
    {
        return getLandingAccessDenialReason(landingPoint, pilot, ship) == null;
    }

    /**
     * @return {@code null} if allowed, otherwise a short English reason for GM / debug / system messages.
     */
    public static String getLandingAccessDenialReason(obj_id landingPoint, obj_id pilot, obj_id ship) throws InterruptedException
    {
        if (!atmo_landing_registry.isLandingPoint(landingPoint))
            return "invalid_landing_point";
        if (!isIdValid(pilot) || !exists(pilot))
            return "invalid_pilot";
        if (!isIdValid(ship) || !exists(ship))
            return "invalid_ship";

        if (isGod(pilot))
            return null;

        if (isOwnerPilotOnly(landingPoint) && getOwner(ship) != pilot)
            return "owner_pilot_only";

        int mode = getAccessMode(landingPoint);
        if (mode == ACCESS_GM_ONLY)
            return "gm_only";

        if (mode == ACCESS_FACTION_WHITELIST)
        {
            int[] facs = getIntArrayObjVar(landingPoint, OBJVAR_ALLOWED_FACTIONS);
            if (facs == null || facs.length == 0)
                return "faction_whitelist_empty";
            int aligned = pvpGetAlignedFaction(pilot);
            boolean ok = false;
            for (int f : facs)
            {
                if (f == aligned)
                {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return "faction_not_allowed";
        }
        else if (mode == ACCESS_GUILD_ONLY)
        {
            if (!hasObjVar(landingPoint, OBJVAR_GUILD_ID))
                return "guild_not_configured";
            int need = getIntObjVar(landingPoint, OBJVAR_GUILD_ID);
            if (need <= 0 || getGuildId(pilot) != need)
                return "guild_mismatch";
        }
        else if (mode == ACCESS_PLAYER_ALLOWLIST)
        {
            obj_id[] allowed = getObjIdArrayObjVar(landingPoint, OBJVAR_ALLOWED_PLAYERS);
            if (allowed == null || allowed.length == 0)
                return "allowlist_empty";
            boolean ok = false;
            for (obj_id a : allowed)
            {
                if (isIdValid(a) && a == pilot)
                {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return "not_on_allowlist";
        }

        if (hasObjVar(landingPoint, OBJVAR_REQUIRED_SKILL))
        {
            String sk = getStringObjVar(landingPoint, OBJVAR_REQUIRED_SKILL);
            if (sk != null && sk.length() > 0 && !hasSkill(pilot, sk))
                return "missing_required_skill";
        }

        if (hasObjVar(landingPoint, OBJVAR_MIN_LEVEL))
        {
            int minL = getIntObjVar(landingPoint, OBJVAR_MIN_LEVEL);
            if (getLevel(pilot) < minL)
                return "below_min_level";
        }

        if (hasObjVar(landingPoint, OBJVAR_REQUIRED_GUILD_TAG))
        {
            String needTag = getStringObjVar(landingPoint, OBJVAR_REQUIRED_GUILD_TAG);
            if (needTag != null)
            {
                needTag = needTag.trim();
                if (needTag.length() > 0)
                {
                    int gid = getGuildId(pilot);
                    if (gid <= 0)
                        return "guild_tag_required";
                    String abbr = guildGetAbbrev(gid);
                    String gname = guildGetName(gid);
                    if (abbr == null)
                        abbr = "";
                    if (gname == null)
                        gname = "";
                    if (!needTag.equalsIgnoreCase(abbr.trim()) && !needTag.equalsIgnoreCase(gname.trim()))
                        return "guild_tag_mismatch";
                }
            }
        }

        if (hasObjVar(landingPoint, OBJVAR_REQUIRED_ALIGNED_FACTION_NAME))
        {
            String facName = getStringObjVar(landingPoint, OBJVAR_REQUIRED_ALIGNED_FACTION_NAME);
            if (facName != null)
            {
                facName = facName.trim().toLowerCase();
                if (facName.length() > 0)
                {
                    int needHash = alignedFactionNameToHash(facName);
                    if (needHash == Integer.MIN_VALUE)
                        return "invalid_required_faction_config";
                    if (pvpGetAlignedFaction(pilot) != needHash)
                        return "aligned_faction_mismatch";
                }
            }
        }

        if (hasObjVar(landingPoint, OBJVAR_REQUIRED_PROFESSIONS_ANY))
        {
            int[] profs = getIntArrayObjVar(landingPoint, OBJVAR_REQUIRED_PROFESSIONS_ANY);
            if (profs != null && profs.length > 0)
            {
                boolean any = false;
                for (int p : profs)
                {
                    if (utils.isProfession(pilot, p))
                    {
                        any = true;
                        break;
                    }
                }
                if (!any)
                    return "profession_not_allowed";
            }
        }

        return null;
    }

    /** @return faction hash, or {@code Integer.MIN_VALUE} if unknown */
    private static int alignedFactionNameToHash(String normalizedLower)
    {
        if (normalizedLower.equals("rebel"))
            return FACTION_HASH_REBEL;
        if (normalizedLower.equals("imperial"))
            return FACTION_HASH_IMPERIAL;
        if (normalizedLower.equals("neutral"))
            return FACTION_HASH_NEUTRAL;
        return Integer.MIN_VALUE;
    }

    /**
     * After a ship has arrived: restore {@code atmo.landing.target} / {@code atmo.landing.name} for terminal + dock scripts,
     * and if the pad has a positive time limit, attach {@code space.atmo.atmo_landing_docked} and start the expiry timer.
     */
    public static void onShipLandedAtPoint(obj_id ship, obj_id landingPoint) throws InterruptedException
    {
        if (!isIdValid(ship) || !exists(ship) || !atmo_landing_registry.isLandingPoint(landingPoint))
            return;

        setObjVar(ship, "atmo.landing.target", landingPoint);
        String nm = atmo_landing_registry.getLandingPointName(landingPoint);
        if (nm != null && nm.length() > 0)
            setObjVar(ship, "atmo.landing.name", nm);

        int dockSec = getEffectiveDockDurationSeconds(landingPoint);
        if (dockSec <= 0)
            return;

        if (!hasScript(ship, "space.atmo.atmo_landing_docked"))
            attachScript(ship, "space.atmo.atmo_landing_docked");

        setObjVar(ship, atmo_landing_docked.OBJVAR_DOCK_EXPIRY, getGameTime() + dockSec);
        messageTo(ship, "checkDockingTimer", null, 1, false);
    }

    /**
     * Landing pads registered under {@code atmo_landing} with empty subcategory (the only valid pair in planet_map_cat).
     */
    public static obj_id[] getAllLandingPointsInSceneMerged() throws InterruptedException
    {
        return getAllLandingPointsInSceneForCategory(atmo_landing_registry.MAP_CATEGORY, atmo_landing_registry.MAP_SUBCATEGORY);
    }

    /**
     * @deprecated Prefer {@link #getAllLandingPointsInSceneMerged()}. Non-empty subcategories must exist in
     * {@code planet_map_cat.tab} or {@code getPlanetaryMapLocations} / registration will not match.
     */
    public static obj_id[] getAllLandingPointsInSceneMergedForCategory(String mapCategory) throws InterruptedException
    {
        return getAllLandingPointsInSceneForCategory(mapCategory, atmo_landing_registry.MAP_SUBCATEGORY);
    }

    public static obj_id[] getAllLandingPointsInSceneForCategory(String mapCategory, String mapSubcategory) throws InterruptedException
    {
        String scene = getCurrentSceneName();
        if (scene == null || scene.isEmpty())
            return new obj_id[0];

        map_location[] mapLocs = getPlanetaryMapLocations(mapCategory, mapSubcategory);
        if (mapLocs == null || mapLocs.length == 0)
            return new obj_id[0];

        Vector result = new Vector();
        for (map_location ml : mapLocs)
        {
            obj_id locId = ml.getLocationId();
            if (isIdValid(locId) && exists(locId) && atmo_landing_registry.isLandingPoint(locId))
                result.add(locId);
        }

        obj_id[] arr = new obj_id[result.size()];
        result.toArray(arr);
        return arr;
    }
}
