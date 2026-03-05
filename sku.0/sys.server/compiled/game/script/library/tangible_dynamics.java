package script.library;

/*
@Origin: dsrc.script.library
@Author: Titan Development Team
@Purpose: Library for applying and managing TangibleDynamics effects
@Note: Provides push, spin, breathing, bounce, wobble, and orbit effects
@Requirements: Base Dynamics system, condition system
@Created: 2025
@Updated: 2026 - Added bounce, wobble, orbit, drag, easing
@Copyright © SWG: Titan 2026.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class tangible_dynamics extends script.base_script
{
    // Force mode bitmask values (match C++ TangibleDynamics::ForceMode)
    public static final int FORCE_MODE_NONE      = 0;
    public static final int FORCE_MODE_PUSH      = (1 << 0);
    public static final int FORCE_MODE_SPIN      = (1 << 1);
    public static final int FORCE_MODE_BREATHING = (1 << 2);
    public static final int FORCE_MODE_BOUNCE    = (1 << 3);
    public static final int FORCE_MODE_WOBBLE    = (1 << 4);
    public static final int FORCE_MODE_ORBIT     = (1 << 5);

    // Movement spaces
    public static final int SPACE_WORLD  = 0;
    public static final int SPACE_PARENT = 1;
    public static final int SPACE_OBJECT = 2;

    // Easing types (match C++ TangibleDynamics::EaseType)
    public static final int EASE_NONE       = 0;
    public static final int EASE_IN         = 1;
    public static final int EASE_OUT        = 2;
    public static final int EASE_IN_OUT     = 3;

    // =====================================================================
    // PUSH / SHOVE
    // =====================================================================

    /**
     * Apply a push/shove force to an object
     */
    public static void applyPushForce(obj_id target, float velocityX, float velocityY, float velocityZ, float duration, int space) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_push");
        params.put("velocityX", velocityX);
        params.put("velocityY", velocityY);
        params.put("velocityZ", velocityZ);
        params.put("duration", duration);
        params.put("space", space);
        params.put("drag", 0.0f);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a push/shove force with drag (velocity decays exponentially)
     * @param drag Drag coefficient (0=no drag, 1=moderate, 5+=heavy drag)
     */
    public static void applyPushForceWithDrag(obj_id target, float velocityX, float velocityY, float velocityZ, float drag, float duration, int space) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_push");
        params.put("velocityX", velocityX);
        params.put("velocityY", velocityY);
        params.put("velocityZ", velocityZ);
        params.put("duration", duration);
        params.put("space", space);
        params.put("drag", drag);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // SPIN / ROTATION
    // =====================================================================

    /**
     * Apply a spinning/rotation force to an object
     */
    public static void applySpinForce(obj_id target, float rotYaw, float rotPitch, float rotRoll, float duration, boolean aroundCenter) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_spin");
        params.put("rotYaw", rotYaw);
        params.put("rotPitch", rotPitch);
        params.put("rotRoll", rotRoll);
        params.put("duration", duration);
        params.put("aroundCenter", aroundCenter);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // BREATHING / PULSING (sinusoidal)
    // =====================================================================

    /**
     * Apply a breathing/pulsing effect to an object
     */
    public static void applyBreathingEffect(obj_id target, float minScale, float maxScale, float speed, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        if (minScale <= 0.0f || maxScale <= 0.0f || minScale > maxScale) return;

        dictionary params = new dictionary();
        params.put("command", "apply_breathing");
        params.put("minScale", minScale);
        params.put("maxScale", maxScale);
        params.put("speed", speed);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // BOUNCE (gravity + elasticity)
    // =====================================================================

    /**
     * Apply a bounce effect - object pops up then bounces with gravity
     * @param gravity Gravity acceleration (9.8 = earth-like)
     * @param elasticity Bounce factor 0..1 (0=no bounce, 1=perfect bounce)
     * @param initialUpVelocity Initial upward velocity (m/s)
     * @param duration Max duration (-1 for until bounce settles)
     */
    public static void applyBounceEffect(obj_id target, float gravity, float elasticity, float initialUpVelocity, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_bounce");
        params.put("gravity", gravity);
        params.put("elasticity", elasticity);
        params.put("initialUpVelocity", initialUpVelocity);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // WOBBLE (sinusoidal position oscillation)
    // =====================================================================

    /**
     * Apply a wobble effect - object oscillates around its position
     * @param ampX Amplitude on X axis (meters)
     * @param ampY Amplitude on Y axis (meters)
     * @param ampZ Amplitude on Z axis (meters)
     * @param freqX Frequency on X axis (cycles per second)
     * @param freqY Frequency on Y axis (cycles per second)
     * @param freqZ Frequency on Z axis (cycles per second)
     * @param duration Duration in seconds (-1 for infinite)
     */
    public static void applyWobbleEffect(obj_id target, float ampX, float ampY, float ampZ, float freqX, float freqY, float freqZ, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_wobble");
        params.put("ampX", ampX);
        params.put("ampY", ampY);
        params.put("ampZ", ampZ);
        params.put("freqX", freqX);
        params.put("freqY", freqY);
        params.put("freqZ", freqZ);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // ORBIT (circular motion around a point)
    // =====================================================================

    /**
     * Apply an orbit effect - object orbits around a center point on XZ plane
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param centerZ Center Z coordinate
     * @param radius Orbit radius in meters
     * @param radiansPerSecond Angular speed (PI = half revolution per second)
     * @param duration Duration in seconds (-1 for infinite)
     */
    public static void applyOrbitEffect(obj_id target, float centerX, float centerY, float centerZ, float radius, float radiansPerSecond, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_orbit");
        params.put("centerX", centerX);
        params.put("centerY", centerY);
        params.put("centerZ", centerZ);
        params.put("radius", radius);
        params.put("radiansPerSecond", radiansPerSecond);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // EASING
    // =====================================================================

    /**
     * Set easing mode for all active/future forces on an object
     * @param easeType One of EASE_NONE, EASE_IN, EASE_OUT, EASE_IN_OUT
     * @param easeDuration How long the ease ramp takes (seconds)
     */
    public static void setEasing(obj_id target, int easeType, float easeDuration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "set_easing");
        params.put("easeType", easeType);
        params.put("easeDuration", easeDuration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // COMBINED
    // =====================================================================

    /**
     * Apply combined forces (push, spin, and breathing) simultaneously
     */
    public static void applyCombinedForces(obj_id target, float pushX, float pushY, float pushZ, float spinYaw, float spinPitch, float spinRoll, float breatheMin, float breatheMax, float breatheSpeed, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_combined");
        params.put("pushX", pushX);
        params.put("pushY", pushY);
        params.put("pushZ", pushZ);
        params.put("spinYaw", spinYaw);
        params.put("spinPitch", spinPitch);
        params.put("spinRoll", spinRoll);
        params.put("breatheMin", breatheMin);
        params.put("breatheMax", breatheMax);
        params.put("breatheSpeed", breatheSpeed);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    // =====================================================================
    // CLEAR FORCES
    // =====================================================================

    public static void clearAllForces(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_all");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
        clearCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    public static void clearPushForce(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_push");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    public static void clearSpinForce(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_spin");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    public static void clearBreathingEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_breathing");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    public static void clearBounceEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_bounce");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    public static void clearWobbleEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_wobble");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    public static void clearOrbitEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_orbit");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private static boolean isValidTarget(obj_id target)
    {
        return target != null && target.getAsLong() > 0;
    }
}
