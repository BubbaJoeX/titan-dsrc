package script.space.atmo;

import script.*;
import script.library.*;

import java.util.Vector;

/**
 * Library for managing atmospheric landing points dynamically.
 * Landing points are spawn eggs with atmo.landing_point.* objvars.
 */
public class atmo_landing_registry extends script.base_script
{
    public static final String OBJVAR_ROOT = "atmo.landing_point";
    public static final String OBJVAR_LOC = OBJVAR_ROOT + ".loc";
    public static final String OBJVAR_DISEMBARK_LOC = OBJVAR_ROOT + ".disembark_loc";
    public static final String OBJVAR_YAW = OBJVAR_ROOT + ".yaw";
    public static final String OBJVAR_NAME = OBJVAR_ROOT + ".name";
    public static final String OBJVAR_TIME_TO_DISEMBARK = OBJVAR_ROOT + ".time_to_disembark";
    /** Optional world Y (meters) for autopilot landing height; if unset, {@link #getApproachAltitude} uses {@code loc.y}. */
    public static final String OBJVAR_LANDING_ALTITUDE = OBJVAR_ROOT + ".landing_altitude";
    public static final String OBJVAR_LOC_OFFSET = OBJVAR_ROOT + ".loc_offset";
    public static final String OBJVAR_OCCUPIED_BY = OBJVAR_ROOT + ".occupied_by";
    public static final String OBJVAR_OCCUPIED_ETA = OBJVAR_ROOT + ".occupied_eta";
    public static final String OBJVAR_OCCUPIED_STATE = OBJVAR_ROOT + ".occupied_state";

    // Occupancy states
    public static final int OCCUPANCY_NONE = 0;
    public static final int OCCUPANCY_RESERVED = 1;  // Ship en route
    public static final int OCCUPANCY_LANDED = 2;    // Ship landed

    public static final String MAP_CATEGORY = "atmo_landing";
    public static final String MAP_SUBCATEGORY = "";

    public static final float DEFAULT_CRUISE_ALTITUDE = 1200.0f;  // Cruise altitude from terrain

    public static final int EXTEND_DOCK_COST_MIN = 15000;
    public static final int EXTEND_DOCK_COST_MAX = 25000;
    public static final int EXTEND_DOCK_TIME = 300;

    /**
     * Check if an object is a valid landing point.
     */
    public static boolean isLandingPoint(obj_id self) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self))
            return false;
        return hasObjVar(self, OBJVAR_LOC) && hasObjVar(self, OBJVAR_NAME);
    }

    /**
     * Get the landing point name.
     */
    public static String getLandingPointName(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return "";
        return getStringObjVar(landingPoint, OBJVAR_NAME);
    }

    /**
     * Get the fly-to location for landing.
     */
    public static location getLandingLocation(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return null;
        return getLocationObjVar(landingPoint, OBJVAR_LOC);
    }

    /**
     * Get the disembark location.
     */
    public static location getDisembarkLocation(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return null;
        if (hasObjVar(landingPoint, OBJVAR_DISEMBARK_LOC))
            return getLocationObjVar(landingPoint, OBJVAR_DISEMBARK_LOC);
        return getLandingLocation(landingPoint);
    }

    /**
     * Get the yaw angle for landing.
     */
    public static float getLandingYaw(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return 0.0f;
        if (hasObjVar(landingPoint, OBJVAR_YAW))
            return getFloatObjVar(landingPoint, OBJVAR_YAW);
        return 0.0f;
    }

    /**
     * Get the time allowed to remain docked (-1 = forever).
     */
    public static int getTimeToDisembark(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return -1;
        if (hasObjVar(landingPoint, OBJVAR_TIME_TO_DISEMBARK))
            return getIntObjVar(landingPoint, OBJVAR_TIME_TO_DISEMBARK);
        return -1;
    }

    /**
     * Get optional location offset for small platforms.
     */
    public static location getLocationOffset(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return null;
        if (hasObjVar(landingPoint, OBJVAR_LOC_OFFSET))
            return getLocationObjVar(landingPoint, OBJVAR_LOC_OFFSET);
        return null;
    }

    /**
     * Check if the landing point is occupied (either reserved or docked).
     */
    public static boolean isOccupied(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        // Check explicit occupancy state first
        if (hasObjVar(landingPoint, OBJVAR_OCCUPIED_STATE))
        {
            int state = getIntObjVar(landingPoint, OBJVAR_OCCUPIED_STATE);
            if (state == OCCUPANCY_LANDED)
            {
                // Validate the ship still exists
                if (hasObjVar(landingPoint, OBJVAR_OCCUPIED_BY))
                {
                    obj_id occupier = getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
                    if (isIdValid(occupier) && exists(occupier))
                        return true;
                }
                // Ship no longer exists, clear it
                clearOccupancy(landingPoint);
                return false;
            }
            else if (state == OCCUPANCY_RESERVED)
            {
                // Reserved but not yet docked - check ETA
                return isEnRoute(landingPoint);
            }
        }

        // Fallback to legacy check
        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_BY))
            return false;

        obj_id occupier = getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
        if (!isIdValid(occupier) || !exists(occupier))
        {
            clearOccupancy(landingPoint);
            return false;
        }

        return true;
    }

    /**
     * Check if a ship has landed at this point (not just reserved/en route).
     */
    public static boolean isLanded(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_STATE))
            return false;

        int state = getIntObjVar(landingPoint, OBJVAR_OCCUPIED_STATE);
        if (state != OCCUPANCY_LANDED)
            return false;

        // Validate occupier exists
        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_BY))
            return false;

        obj_id occupier = getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
        return isIdValid(occupier) && exists(occupier);
    }

    /**
     * Check if a ship is en route but not yet arrived (via ETA check).
     */
    public static boolean isEnRoute(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_ETA))
            return false;

        int eta = getIntObjVar(landingPoint, OBJVAR_OCCUPIED_ETA);
        int now = getGameTime();

        if (now > eta + 30)
        {
            removeObjVar(landingPoint, OBJVAR_OCCUPIED_ETA);
            if (!isOccupied(landingPoint))
                clearOccupancy(landingPoint);
            return false;
        }

        return true;
    }

    /**
     * Validate and clear stale en-route reservations.
     * Called by periodic heartbeat from landing point.
     */
    public static void validateEnRoute(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return;

        // Check if in reserved state
        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_STATE))
            return;

        int state = getIntObjVar(landingPoint, OBJVAR_OCCUPIED_STATE);
        if (state != OCCUPANCY_RESERVED)
            return;

        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_ETA))
        {
            // Reserved but no ETA - invalid state, clear it
            clearOccupancy(landingPoint);
            return;
        }

        int eta = getIntObjVar(landingPoint, OBJVAR_OCCUPIED_ETA);
        int now = getGameTime();

        // If ETA has passed by more than 60 seconds, clear the reservation
        if (now > eta + 60)
        {
            obj_id reservedShip = null;
            if (hasObjVar(landingPoint, OBJVAR_OCCUPIED_BY))
                reservedShip = getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);

            // Check if ship still exists and has actually landed
            if (isIdValid(reservedShip) && exists(reservedShip))
            {
                // If ship has landed_at reference to this landing point, upgrade to LANDED
                if (hasObjVar(reservedShip, "atmo.landing.landed_at"))
                {
                    obj_id landedAt = getObjIdObjVar(reservedShip, "atmo.landing.landed_at");
                    if (isIdValid(landedAt) && landedAt.equals(landingPoint))
                    {
                        setObjVar(landingPoint, OBJVAR_OCCUPIED_STATE, OCCUPANCY_LANDED);
                        removeObjVar(landingPoint, OBJVAR_OCCUPIED_ETA);
                        updateMapStatus(landingPoint);
                        return;
                    }
                }
            }

            // Ship didn't arrive or doesn't exist, clear reservation
            clearOccupancy(landingPoint);
        }
    }

    /**
     * Reserve a landing point for a ship en route.
     */
    public static boolean reserveLandingPoint(obj_id landingPoint, obj_id ship, int etaSeconds) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        if (isOccupied(landingPoint) || isEnRoute(landingPoint))
            return false;

        setObjVar(landingPoint, OBJVAR_OCCUPIED_BY, ship);
        setObjVar(landingPoint, OBJVAR_OCCUPIED_ETA, getGameTime() + etaSeconds);
        setObjVar(landingPoint, OBJVAR_OCCUPIED_STATE, OCCUPANCY_RESERVED);
        updateMapStatus(landingPoint);
        return true;
    }

    /**
     * Mark a landing point as occupied by a ship that has landed.
     */
    public static boolean occupyLandingPoint(obj_id landingPoint, obj_id ship) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        setObjVar(landingPoint, OBJVAR_OCCUPIED_BY, ship);
        setObjVar(landingPoint, OBJVAR_OCCUPIED_STATE, OCCUPANCY_LANDED);
        removeObjVar(landingPoint, OBJVAR_OCCUPIED_ETA);
        updateMapStatus(landingPoint);
        return true;
    }

    /**
     * Clear occupancy of a landing point.
     */
    public static void clearOccupancy(obj_id landingPoint) throws InterruptedException
    {
        if (!isIdValid(landingPoint) || !exists(landingPoint))
            return;

        removeObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
        removeObjVar(landingPoint, OBJVAR_OCCUPIED_ETA);
        setObjVar(landingPoint, OBJVAR_OCCUPIED_STATE, OCCUPANCY_NONE);
        updateMapStatus(landingPoint);
    }

    /**
     * Tell the pad tied to this ship (via {@code atmo.landing.landed_at} or {@code atmo.landing.target}) to release
     * occupancy and strip landing objvars from the ship. Use whenever the vessel leaves without the landing point
     * script running its normal flow (pack, city eject, summon, etc.).
     */
    public static void detachShipFromLandingPoint(obj_id ship) throws InterruptedException
    {
        if (!isIdValid(ship))
            return;

        obj_id pad = null;
        if (hasObjVar(ship, "atmo.landing.landed_at"))
            pad = getObjIdObjVar(ship, "atmo.landing.landed_at");
        else if (hasObjVar(ship, "atmo.landing.target"))
            pad = getObjIdObjVar(ship, "atmo.landing.target");

        if (isIdValid(pad) && exists(pad) && isLandingPoint(pad))
        {
            dictionary departedParams = new dictionary();
            departedParams.put("ship", ship);
            messageTo(pad, "handleShipDeparted", departedParams, 0, false);
            return;
        }

        if (hasObjVar(ship, "atmo.landing"))
            removeObjVar(ship, "atmo.landing");
    }

    /**
     * Get the ship occupying a landing point.
     * Returns null if no ship is occupying or the ship no longer exists.
     */
    public static obj_id getOccupyingShip(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return null;

        if (!hasObjVar(landingPoint, OBJVAR_OCCUPIED_BY))
            return null;

        obj_id occupier = getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
        if (!isIdValid(occupier) || !exists(occupier))
            return null;

        return occupier;
    }

    /**
     * Get cruise altitude for a landing point (1200m from terrain).
     */
    public static float getCruiseAltitude(obj_id landingPoint) throws InterruptedException
    {
        location loc = getLandingLocation(landingPoint);
        if (loc == null)
            return DEFAULT_CRUISE_ALTITUDE;

        // Get terrain height at landing location and add cruise altitude
        float terrainHeight = getHeightAtLocation(loc.x, loc.z);
        return terrainHeight + DEFAULT_CRUISE_ALTITUDE;
    }

    /**
     * World Y (meters) the ship should use as landing altitude for autopilot.
     * Uses {@link #OBJVAR_LANDING_ALTITUDE} when set; otherwise {@code loc.y} from the fly-to location.
     */
    public static float getApproachAltitude(obj_id landingPoint) throws InterruptedException
    {
        location loc = getLandingLocation(landingPoint);
        if (loc == null)
            return 50.0f;
        if (hasObjVar(landingPoint, OBJVAR_LANDING_ALTITUDE))
            return getFloatObjVar(landingPoint, OBJVAR_LANDING_ALTITUDE);
        return loc.y;
    }

    /**
     * Register a landing point on the planet map.
     */
    public static boolean registerOnMap(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        String name = getLandingPointName(landingPoint);
        location loc = getLandingLocation(landingPoint);

        if (name == null || name.isEmpty() || loc == null)
            return false;

        String displayName = getDisplayName(landingPoint);
        byte flags = isOccupied(landingPoint) ? MLF_INACTIVE : MLF_ACTIVE;

        // Native map API requires names present in planet_map_cat.tab. Only "atmo_landing" exists (no subcategories).
        return addPlanetaryMapLocation(landingPoint, displayName, (int)loc.x, (int)loc.z, MAP_CATEGORY, MAP_SUBCATEGORY, MLT_DYNAMIC, flags);
    }

    /**
     * Remove a landing point from the planet map.
     */
    public static void unregisterFromMap(obj_id landingPoint) throws InterruptedException
    {
        if (!isIdValid(landingPoint))
            return;
        removePlanetaryMapLocation(landingPoint);
    }

    /**
     * Update the map status (occupied/available) for a landing point.
     */
    public static void updateMapStatus(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return;

        unregisterFromMap(landingPoint);
        registerOnMap(landingPoint);
    }

    /**
     * Get the display name for the planet map (includes status).
     */
    public static String getDisplayName(obj_id landingPoint) throws InterruptedException
    {
        String name = getLandingPointName(landingPoint);
        if (name == null || name.isEmpty())
            return "Unknown Landing Point";

        // Check explicit state first
        if (hasObjVar(landingPoint, OBJVAR_OCCUPIED_STATE))
        {
            int state = getIntObjVar(landingPoint, OBJVAR_OCCUPIED_STATE);
            if (state == OCCUPANCY_LANDED || state == OCCUPANCY_RESERVED)
            {
                // Validate occupier still exists before showing as occupied
                if (hasObjVar(landingPoint, OBJVAR_OCCUPIED_BY))
                {
                    obj_id occupier = getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
                    if (isIdValid(occupier) && exists(occupier))
                        return name + " (OCCUPIED)";
                }
            }
        }

        // Fallback checks
        if (isOccupied(landingPoint) || isEnRoute(landingPoint))
            return name + " (OCCUPIED)";

        return name + " (AVAILABLE)";
    }

    /**
     * Get all landing points in the current scene.
     */
    public static obj_id[] getAllLandingPointsInScene() throws InterruptedException
    {
        String scene = getCurrentSceneName();
        if (scene == null || scene.isEmpty())
            return new obj_id[0];

        return atmo_landing_manager.getAllLandingPointsInSceneMerged();
    }

    /**
     * Calculate ETA to a landing point from a ship position.
     */
    public static int calculateETA(obj_id ship, obj_id landingPoint) throws InterruptedException
    {
        if (!isIdValid(ship) || !isLandingPoint(landingPoint))
            return 0;

        location shipLoc = getLocation(ship);
        location destLoc = getLandingLocation(landingPoint);

        if (shipLoc == null || destLoc == null)
            return 0;

        float dx = destLoc.x - shipLoc.x;
        float dz = destLoc.z - shipLoc.z;
        float dist = (float) StrictMath.sqrt(dx * dx + dz * dz);

        float speed = getShipEngineSpeedMaximum(ship) * 2.5f;
        if (speed <= 0)
            speed = 50.0f;

        float cruiseAlt = getCruiseAltitude(landingPoint);
        float landingAlt = getApproachAltitude(landingPoint);
        float elevatorSpeed = 30.0f;

        float ascentTime = cruiseAlt / elevatorSpeed;
        float descentTime = (cruiseAlt - landingAlt) / elevatorSpeed;
        float cruiseTime = dist / speed;

        return (int)(ascentTime + cruiseTime + descentTime) + 5;
    }

    /**
     * Configure a spawn egg as a landing point via GM tool.
     */
    public static void configureLandingPoint(obj_id egg, String name, location loc, location disembarkLoc, float yaw, int timeToDisembark) throws InterruptedException
    {
        if (!isIdValid(egg) || !exists(egg))
            return;

        setObjVar(egg, OBJVAR_NAME, name);
        setObjVar(egg, OBJVAR_LOC, loc);

        if (disembarkLoc != null)
            setObjVar(egg, OBJVAR_DISEMBARK_LOC, disembarkLoc);

        setObjVar(egg, OBJVAR_YAW, yaw);
        setObjVar(egg, OBJVAR_TIME_TO_DISEMBARK, timeToDisembark);

        registerOnMap(egg);
    }

    /**
     * Clear landing point configuration from a spawn egg.
     */
    public static void clearLandingPointConfig(obj_id egg) throws InterruptedException
    {
        if (!isIdValid(egg) || !exists(egg))
            return;

        unregisterFromMap(egg);
        removeObjVar(egg, OBJVAR_ROOT);
    }
}

