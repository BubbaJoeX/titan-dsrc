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
    public static final String OBJVAR_LOC_OFFSET = OBJVAR_ROOT + ".loc_offset";
    public static final String OBJVAR_OCCUPIED_BY = OBJVAR_ROOT + ".occupied_by";
    public static final String OBJVAR_OCCUPIED_ETA = OBJVAR_ROOT + ".occupied_eta";

    public static final String MAP_CATEGORY = "atmo_landing";
    public static final String MAP_SUBCATEGORY = "";

    public static final float DEFAULT_CRUISE_OFFSET = 50.0f;
    public static final float DEFAULT_LANDING_OFFSET = 20.0f;

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
     * Check if the landing point is occupied.
     */
    public static boolean isOccupied(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

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
        updateMapStatus(landingPoint);
    }

    /**
     * Get the ship occupying a landing point.
     */
    public static obj_id getOccupyingShip(obj_id landingPoint) throws InterruptedException
    {
        if (!isOccupied(landingPoint))
            return null;
        return getObjIdObjVar(landingPoint, OBJVAR_OCCUPIED_BY);
    }

    /**
     * Get cruise altitude for a landing point (loc.y + 50).
     */
    public static float getCruiseAltitude(obj_id landingPoint) throws InterruptedException
    {
        location loc = getLandingLocation(landingPoint);
        if (loc == null)
            return 500.0f;
        return loc.y + DEFAULT_CRUISE_OFFSET;
    }

    /**
     * Get approach altitude for a landing point (loc.y + 20).
     */
    public static float getApproachAltitude(obj_id landingPoint) throws InterruptedException
    {
        location loc = getLandingLocation(landingPoint);
        if (loc == null)
            return 50.0f;
        return loc.y + DEFAULT_LANDING_OFFSET;
    }

    /**
     * Register a landing point on the planet map.
     */
    public static boolean registerOnMap(obj_id landingPoint) throws InterruptedException
    {
        if (!isLandingPoint(landingPoint))
            return false;

        if (!space_transition.isAtmosphericFlightScene())
            return false;

        String name = getLandingPointName(landingPoint);
        location loc = getLandingLocation(landingPoint);

        if (name == null || name.isEmpty() || loc == null)
            return false;

        String displayName = getDisplayName(landingPoint);
        byte flags = isOccupied(landingPoint) ? MLF_INACTIVE : MLF_ACTIVE;

        return addPlanetaryMapLocation(landingPoint, displayName, (int)loc.x, (int)loc.z, MAP_CATEGORY, MAP_SUBCATEGORY, MLT_STATIC, flags);
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

        if (!space_transition.isAtmosphericFlightScene())
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

        if (isOccupied(landingPoint) || isEnRoute(landingPoint))
            return name + " (OCCUPIED)";
        else
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

        map_location[] mapLocs = getPlanetaryMapLocations(MAP_CATEGORY, MAP_SUBCATEGORY);
        if (mapLocs == null || mapLocs.length == 0)
            return new obj_id[0];

        Vector result = new Vector();
        for (map_location ml : mapLocs)
        {
            obj_id locId = ml.getLocationId();
            if (isIdValid(locId) && exists(locId) && isLandingPoint(locId))
                result.add(locId);
        }

        obj_id[] arr = new obj_id[result.size()];
        result.toArray(arr);
        return arr;
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

