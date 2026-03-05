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
    public static final int FORCE_MODE_NONE         = 0;
    public static final int FORCE_MODE_PUSH         = (1 << 0);
    public static final int FORCE_MODE_SPIN         = (1 << 1);
    public static final int FORCE_MODE_BREATHING    = (1 << 2);
    public static final int FORCE_MODE_BOUNCE       = (1 << 3);
    public static final int FORCE_MODE_WOBBLE       = (1 << 4);
    public static final int FORCE_MODE_ORBIT        = (1 << 5);
    public static final int FORCE_MODE_HOVER        = (1 << 6);
    public static final int FORCE_MODE_FOLLOW_TARGET = (1 << 7);
    public static final int FORCE_MODE_SWAY         = (1 << 8);
    public static final int FORCE_MODE_SHAKE        = (1 << 9);
    public static final int FORCE_MODE_FLOAT        = (1 << 10);
    public static final int FORCE_MODE_CONVEYOR     = (1 << 11);

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

    public static void clearHoverEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_hover");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    public static void clearFollowTargetEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_follow_target");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // HOVER (terrain-following with slight bob)
    // =====================================================================

    /**
     * Apply a hover effect - object floats above terrain with slight bob
     * @param target Object to apply effect to
     * @param hoverHeight Height above terrain (default 1.0m)
     * @param bobAmplitude How much it bobs up/down (default 0.1m)
     * @param bobSpeed How fast it bobs (cycles per second, default 1.0)
     * @param duration Duration in seconds (-1 = infinite)
     */
    public static void applyHoverEffect(obj_id target, float hoverHeight, float bobAmplitude, float bobSpeed, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_hover");
        params.put("hoverHeight", hoverHeight);
        params.put("bobAmplitude", bobAmplitude);
        params.put("bobSpeed", bobSpeed);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a simple hover effect with defaults
     */
    public static void applyHoverEffect(obj_id target, float hoverHeight) throws InterruptedException
    {
        applyHoverEffect(target, hoverHeight, 0.1f, 1.0f, -1.0f);
    }

    // =====================================================================
    // FOLLOW TARGET (hover + follow another object, matching rotation)
    // =====================================================================

    /**
     * Apply a follow target effect - object follows another object, hovers, and matches rotation
     * Like a camera drone or companion pet
     * @param target Object to apply effect to
     * @param followTarget Object to follow
     * @param followDistance Distance to maintain behind target (default 2.0m)
     * @param followSpeed Movement speed toward target (default 3.0 m/s)
     * @param hoverHeight Height above terrain (default 1.0m)
     * @param bobAmplitude How much it bobs up/down (default 0.05m)
     * @param duration Duration in seconds (-1 = infinite)
     */
    public static void applyFollowTargetEffect(obj_id target, obj_id followTarget, float followDistance, float followSpeed, float hoverHeight, float bobAmplitude, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        if (!isIdValid(followTarget)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_follow_target");
        params.put("followTargetId", followTarget.getValue());
        params.put("followDistance", followDistance);
        params.put("followSpeed", followSpeed);
        params.put("hoverHeight", hoverHeight);
        params.put("bobAmplitude", bobAmplitude);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a simple follow target effect with defaults
     */
    public static void applyFollowTargetEffect(obj_id target, obj_id followTarget) throws InterruptedException
    {
        applyFollowTargetEffect(target, followTarget, 2.0f, 3.0f, 1.0f, 0.05f, -1.0f);
    }

    /**
     * Apply follow target effect with distance and speed
     */
    public static void applyFollowTargetEffect(obj_id target, obj_id followTarget, float followDistance, float followSpeed) throws InterruptedException
    {
        applyFollowTargetEffect(target, followTarget, followDistance, followSpeed, 1.0f, 0.05f, -1.0f);
    }

    // =====================================================================
    // SWAY/PENDULUM (swinging back and forth like a hanging sign)
    // =====================================================================

    /**
     * Apply a sway/pendulum effect - object swings back and forth
     * Great for hanging signs, chandeliers, chains, lanterns
     * @param target Object to apply effect to
     * @param swingAngle Maximum swing angle in radians (default 0.1 rad ~ 6 degrees)
     * @param swingSpeed Swing cycles per second (default 1.0)
     * @param damping How quickly swing decays (0=no damping, higher=faster decay)
     * @param duration Duration in seconds (-1 = infinite)
     */
    public static void applySwayEffect(obj_id target, float swingAngle, float swingSpeed, float damping, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_sway");
        params.put("swingAngle", swingAngle);
        params.put("swingSpeed", swingSpeed);
        params.put("damping", damping);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a simple sway effect with defaults
     */
    public static void applySwayEffect(obj_id target) throws InterruptedException
    {
        applySwayEffect(target, 0.1f, 1.0f, 0.0f, -1.0f);
    }

    /**
     * Apply sway with angle and speed
     */
    public static void applySwayEffect(obj_id target, float swingAngle, float swingSpeed) throws InterruptedException
    {
        applySwayEffect(target, swingAngle, swingSpeed, 0.0f, -1.0f);
    }

    public static void clearSwayEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_sway");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // SHAKE/VIBRATE (rapid small position offsets for emphasis)
    // =====================================================================

    /**
     * Apply a shake/vibrate effect - rapid small movements
     * Great for explosions, machinery, damaged equipment, alerts
     * @param target Object to apply effect to
     * @param intensity How much to shake (meters, default 0.1m)
     * @param frequency Shakes per second (default 10)
     * @param duration Duration in seconds (-1 = infinite)
     */
    public static void applyShakeEffect(obj_id target, float intensity, float frequency, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_shake");
        params.put("intensity", intensity);
        params.put("frequency", frequency);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a simple shake effect with defaults
     */
    public static void applyShakeEffect(obj_id target, float duration) throws InterruptedException
    {
        applyShakeEffect(target, 0.1f, 10.0f, duration);
    }

    public static void clearShakeEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_shake");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // FLOAT/LEVITATE (slow drift up and down with slight random movement)
    // =====================================================================

    /**
     * Apply a float/levitate effect - slow drift with organic randomness
     * Great for magical items, holocrons, force-sensitive objects, ghosts
     * @param target Object to apply effect to
     * @param floatHeight Height range to drift (default 0.5m)
     * @param driftSpeed How fast it drifts up/down (cycles per second, default 0.5)
     * @param randomStrength Random horizontal drift amount (default 0.1m)
     * @param duration Duration in seconds (-1 = infinite)
     */
    public static void applyFloatEffect(obj_id target, float floatHeight, float driftSpeed, float randomStrength, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_float");
        params.put("floatHeight", floatHeight);
        params.put("driftSpeed", driftSpeed);
        params.put("randomStrength", randomStrength);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a simple float effect with defaults
     */
    public static void applyFloatEffect(obj_id target) throws InterruptedException
    {
        applyFloatEffect(target, 0.5f, 0.5f, 0.1f, -1.0f);
    }

    /**
     * Apply float effect with height
     */
    public static void applyFloatEffect(obj_id target, float floatHeight) throws InterruptedException
    {
        applyFloatEffect(target, floatHeight, 0.5f, 0.1f, -1.0f);
    }

    public static void clearFloatEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_float");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // CONVEYOR (continuous linear movement with optional wrap)
    // =====================================================================

    /**
     * Apply a conveyor effect - continuous linear movement in one direction
     * Great for factory belts, flowing water effects, escalators
     * @param target Object to apply effect to
     * @param directionX X component of direction vector (will be normalized)
     * @param directionY Y component of direction vector
     * @param directionZ Z component of direction vector
     * @param speed Movement speed in meters per second
     * @param wrapDistance Distance to travel before wrapping back to start (0=no wrap)
     * @param duration Duration in seconds (-1 = infinite)
     */
    public static void applyConveyorEffect(obj_id target, float directionX, float directionY, float directionZ, float speed, float wrapDistance, float duration) throws InterruptedException
    {
        if (!isValidTarget(target)) return;

        dictionary params = new dictionary();
        params.put("command", "apply_conveyor");
        params.put("directionX", directionX);
        params.put("directionY", directionY);
        params.put("directionZ", directionZ);
        params.put("speed", speed);
        params.put("wrapDistance", wrapDistance);
        params.put("duration", duration);

        messageTo(target, "OnTangibleDynamics", params, 0, false);
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply conveyor effect with just direction and speed (no wrap, infinite duration)
     */
    public static void applyConveyorEffect(obj_id target, float directionX, float directionY, float directionZ, float speed) throws InterruptedException
    {
        applyConveyorEffect(target, directionX, directionY, directionZ, speed, 0.0f, -1.0f);
    }

    /**
     * Apply conveyor effect with wrap (object loops back after traveling wrapDistance)
     */
    public static void applyConveyorEffectWithWrap(obj_id target, float directionX, float directionY, float directionZ, float speed, float wrapDistance) throws InterruptedException
    {
        applyConveyorEffect(target, directionX, directionY, directionZ, speed, wrapDistance, -1.0f);
    }

    public static void clearConveyorEffect(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        dictionary params = new dictionary();
        params.put("command", "clear_conveyor");
        messageTo(target, "OnTangibleDynamics", params, 0, false);
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private static boolean isValidTarget(obj_id target)
    {
        return isIdValid(target);
    }

    // =====================================================================
    // UTILITY FUNCTIONS
    // =====================================================================

    /**
     * Enable dynamics on an object (sets condition and attaches handler script)
     */
    public static void enableDynamics(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        attachScript(target, "handler.tangible_dynamics_handler");
        setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Disable dynamics on an object (clears all forces and removes condition)
     */
    public static void disableDynamics(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return;
        clearAllForces(target);
        clearCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Check if dynamics is enabled on an object
     */
    public static boolean isDynamicsEnabled(obj_id target) throws InterruptedException
    {
        if (!isValidTarget(target)) return false;
        return hasCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
    }

    /**
     * Apply a simple push in a direction (world space, no drag)
     */
    public static void push(obj_id target, float vx, float vy, float vz) throws InterruptedException
    {
        applyPushForce(target, vx, vy, vz, -1.0f, SPACE_WORLD);
    }

    /**
     * Apply a simple push upward
     */
    public static void pushUp(obj_id target, float velocity) throws InterruptedException
    {
        applyPushForce(target, 0.0f, velocity, 0.0f, -1.0f, SPACE_WORLD);
    }

    /**
     * Apply a simple spin (yaw only)
     */
    public static void spin(obj_id target, float radiansPerSecond) throws InterruptedException
    {
        applySpinForce(target, radiansPerSecond, 0.0f, 0.0f, -1.0f, false);
    }

    /**
     * Apply a simple breathing effect
     */
    public static void breathe(obj_id target, float minScale, float maxScale) throws InterruptedException
    {
        applyBreathingEffect(target, minScale, maxScale, 1.0f, -1.0f);
    }

    /**
     * Apply a simple bounce (spawn bounce)
     */
    public static void bounce(obj_id target) throws InterruptedException
    {
        applyBounceEffect(target, 9.8f, 0.6f, 4.0f, 5.0f);
    }

    /**
     * Apply a simple hover
     */
    public static void hover(obj_id target, float height) throws InterruptedException
    {
        applyHoverEffect(target, height, 0.1f, 1.0f, -1.0f);
    }

    /**
     * Make an object follow another object
     */
    public static void follow(obj_id target, obj_id followTarget) throws InterruptedException
    {
        applyFollowTargetEffect(target, followTarget, 2.0f, 3.0f, 1.0f, 0.05f, -1.0f);
    }

    /**
     * Apply a simple sway (for hanging objects)
     */
    public static void sway(obj_id target) throws InterruptedException
    {
        applySwayEffect(target, 0.1f, 1.0f, 0.0f, -1.0f);
    }

    /**
     * Apply a simple shake for a duration
     */
    public static void shake(obj_id target, float duration) throws InterruptedException
    {
        applyShakeEffect(target, 0.1f, 10.0f, duration);
    }

    /**
     * Apply a simple float/levitate
     */
    public static void levitate(obj_id target) throws InterruptedException
    {
        applyFloatEffect(target, 0.5f, 0.5f, 0.1f, -1.0f);
    }

    /**
     * Apply a conveyor belt effect
     */
    public static void convey(obj_id target, float dirX, float dirZ, float speed, float wrapDistance) throws InterruptedException
    {
        applyConveyorEffect(target, dirX, 0.0f, dirZ, speed, wrapDistance, -1.0f);
    }

    /**
     * Make an object orbit around its current position
     */
    public static void orbitSelf(obj_id target, float radius, float speed) throws InterruptedException
    {
        location loc = getLocation(target);
        applyOrbitEffect(target, loc.x, loc.y, loc.z, radius, speed, -1.0f);
    }

    /**
     * Make an object wobble in place
     */
    public static void wobble(obj_id target, float amplitude) throws InterruptedException
    {
        applyWobbleEffect(target, amplitude, amplitude * 0.5f, amplitude, 1.0f, 1.5f, 0.8f, -1.0f);
    }
}
