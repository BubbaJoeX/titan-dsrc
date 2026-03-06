package script.handler;

/*
@Origin: dsrc.script.handler
@Author: Titan Development Team
@Purpose: Handler script for TangibleDynamics effects on objects
@Note: Attach this script to any object that should support dynamics effects
@Requirements: Base script system, TangibleDynamics library
@Created: 2025
@Updated: 2026 - Added bounce, wobble, orbit, drag, easing handlers
@Copyright © SWG: Titan 2026.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class tangible_dynamics_handler extends script.base_script
{
    // Message handler for dynamics commands
    public int OnTangibleDynamics(obj_id self, dictionary params) throws InterruptedException
    {
        if (self == null || params == null)
            return SCRIPT_CONTINUE;

        String command = params.getString("command");
        if (command == null)
            return SCRIPT_CONTINUE;

        // Apply commands
        if (command.equals("apply_push"))
            handleApplyPush(self, params);
        else if (command.equals("apply_spin"))
            handleApplySpin(self, params);
        else if (command.equals("apply_breathing"))
            handleApplyBreathing(self, params);
        else if (command.equals("apply_bounce"))
            handleApplyBounce(self, params);
        else if (command.equals("apply_wobble"))
            handleApplyWobble(self, params);
        else if (command.equals("apply_orbit"))
            handleApplyOrbit(self, params);
        else if (command.equals("apply_hover"))
            handleApplyHover(self, params);
        else if (command.equals("apply_follow_target"))
            handleApplyFollowTarget(self, params);
        else if (command.equals("apply_sway"))
            handleApplySway(self, params);
        else if (command.equals("apply_shake"))
            handleApplyShake(self, params);
        else if (command.equals("apply_float"))
            handleApplyFloat(self, params);
        else if (command.equals("apply_conveyor"))
            handleApplyConveyor(self, params);
        else if (command.equals("apply_carousel"))
            handleApplyCarousel(self, params);
        else if (command.equals("apply_combined"))
            handleApplyCombined(self, params);
        else if (command.equals("set_easing"))
            handleSetEasing(self, params);
        // Clear commands
        else if (command.equals("clear_all"))
            handleClearAll(self);
        else if (command.equals("clear_push"))
            handleClearPush(self);
        else if (command.equals("clear_spin"))
            handleClearSpin(self);
        else if (command.equals("clear_breathing"))
            handleClearBreathing(self);
        else if (command.equals("clear_bounce"))
            handleClearBounce(self);
        else if (command.equals("clear_wobble"))
            handleClearWobble(self);
        else if (command.equals("clear_orbit"))
            handleClearOrbit(self);
        else if (command.equals("clear_hover"))
            handleClearHover(self);
        else if (command.equals("clear_follow_target"))
            handleClearFollowTarget(self);
        else if (command.equals("clear_sway"))
            handleClearSway(self);
        else if (command.equals("clear_shake"))
            handleClearShake(self);
        else if (command.equals("clear_float"))
            handleClearFloat(self);
        else if (command.equals("clear_conveyor"))
            handleClearConveyor(self);
        else if (command.equals("clear_carousel"))
            handleClearCarousel(self);

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // APPLY HANDLERS
    // =====================================================================

    private int handleApplyPush(obj_id self, dictionary params) throws InterruptedException
    {
        float velocityX = params.getFloat("velocityX");
        float velocityY = params.getFloat("velocityY");
        float velocityZ = params.getFloat("velocityZ");
        float duration = params.getFloat("duration");
        int space = params.getInt("space");
        float drag = params.getFloat("drag");

        setObjVar(self, "dynamics.push.vx", velocityX);
        setObjVar(self, "dynamics.push.vy", velocityY);
        setObjVar(self, "dynamics.push.vz", velocityZ);
        setObjVar(self, "dynamics.push.duration", duration);
        setObjVar(self, "dynamics.push.space", space);
        setObjVar(self, "dynamics.push.drag", drag);

        if (duration > 0.0f)
            messageTo(self, "OnPushForceTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleApplySpin(obj_id self, dictionary params) throws InterruptedException
    {
        float rotYaw = params.getFloat("rotYaw");
        float rotPitch = params.getFloat("rotPitch");
        float rotRoll = params.getFloat("rotRoll");
        float duration = params.getFloat("duration");
        boolean aroundCenter = params.getBoolean("aroundCenter");

        setObjVar(self, "dynamics.spin.yaw", rotYaw);
        setObjVar(self, "dynamics.spin.pitch", rotPitch);
        setObjVar(self, "dynamics.spin.roll", rotRoll);
        setObjVar(self, "dynamics.spin.duration", duration);
        setObjVar(self, "dynamics.spin.aroundCenter", aroundCenter);

        if (duration > 0.0f)
            messageTo(self, "OnSpinForceTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleApplyBreathing(obj_id self, dictionary params) throws InterruptedException
    {
        float minScale = params.getFloat("minScale");
        float maxScale = params.getFloat("maxScale");
        float speed = params.getFloat("speed");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.breathing.min", minScale);
        setObjVar(self, "dynamics.breathing.max", maxScale);
        setObjVar(self, "dynamics.breathing.speed", speed);
        setObjVar(self, "dynamics.breathing.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnBreathingEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleApplyBounce(obj_id self, dictionary params) throws InterruptedException
    {
        float gravity = params.getFloat("gravity");
        float elasticity = params.getFloat("elasticity");
        float initialUpVelocity = params.getFloat("initialUpVelocity");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.bounce.gravity", gravity);
        setObjVar(self, "dynamics.bounce.elasticity", elasticity);
        setObjVar(self, "dynamics.bounce.velocity", initialUpVelocity);
        setObjVar(self, "dynamics.bounce.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnBounceEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleApplyWobble(obj_id self, dictionary params) throws InterruptedException
    {
        float ampX = params.getFloat("ampX");
        float ampY = params.getFloat("ampY");
        float ampZ = params.getFloat("ampZ");
        float freqX = params.getFloat("freqX");
        float freqY = params.getFloat("freqY");
        float freqZ = params.getFloat("freqZ");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.wobble.ampX", ampX);
        setObjVar(self, "dynamics.wobble.ampY", ampY);
        setObjVar(self, "dynamics.wobble.ampZ", ampZ);
        setObjVar(self, "dynamics.wobble.freqX", freqX);
        setObjVar(self, "dynamics.wobble.freqY", freqY);
        setObjVar(self, "dynamics.wobble.freqZ", freqZ);
        setObjVar(self, "dynamics.wobble.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnWobbleEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleApplyOrbit(obj_id self, dictionary params) throws InterruptedException
    {
        float centerX = params.getFloat("centerX");
        float centerY = params.getFloat("centerY");
        float centerZ = params.getFloat("centerZ");
        float radius = params.getFloat("radius");
        float radiansPerSecond = params.getFloat("radiansPerSecond");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.orbit.centerX", centerX);
        setObjVar(self, "dynamics.orbit.centerY", centerY);
        setObjVar(self, "dynamics.orbit.centerZ", centerZ);
        setObjVar(self, "dynamics.orbit.radius", radius);
        setObjVar(self, "dynamics.orbit.speed", radiansPerSecond);
        setObjVar(self, "dynamics.orbit.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnOrbitEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleSetEasing(obj_id self, dictionary params) throws InterruptedException
    {
        int easeType = params.getInt("easeType");
        float easeDuration = params.getFloat("easeDuration");

        setObjVar(self, "dynamics.easing.type", easeType);
        setObjVar(self, "dynamics.easing.duration", easeDuration);

        return SCRIPT_CONTINUE;
    }

    private int handleApplyCombined(obj_id self, dictionary params) throws InterruptedException
    {
        dictionary pushParams = new dictionary();
        pushParams.put("command", "apply_push");
        pushParams.put("velocityX", params.getFloat("pushX"));
        pushParams.put("velocityY", params.getFloat("pushY"));
        pushParams.put("velocityZ", params.getFloat("pushZ"));
        pushParams.put("duration", params.getFloat("duration"));
        pushParams.put("space", script.library.tangible_dynamics.SPACE_WORLD);
        pushParams.put("drag", 0.0f);
        handleApplyPush(self, pushParams);

        dictionary spinParams = new dictionary();
        spinParams.put("command", "apply_spin");
        spinParams.put("rotYaw", params.getFloat("spinYaw"));
        spinParams.put("rotPitch", params.getFloat("spinPitch"));
        spinParams.put("rotRoll", params.getFloat("spinRoll"));
        spinParams.put("duration", params.getFloat("duration"));
        spinParams.put("aroundCenter", false);
        handleApplySpin(self, spinParams);

        dictionary breatheParams = new dictionary();
        breatheParams.put("command", "apply_breathing");
        breatheParams.put("minScale", params.getFloat("breatheMin"));
        breatheParams.put("maxScale", params.getFloat("breatheMax"));
        breatheParams.put("speed", params.getFloat("breatheSpeed"));
        breatheParams.put("duration", params.getFloat("duration"));
        handleApplyBreathing(self, breatheParams);

        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // CLEAR HANDLERS
    // =====================================================================

    private int handleClearAll(obj_id self) throws InterruptedException
    {
        handleClearPush(self);
        handleClearSpin(self);
        handleClearBreathing(self);
        handleClearBounce(self);
        handleClearWobble(self);
        handleClearOrbit(self);
         handleClearHover(self);
        handleClearFollowTarget(self);
        removeObjVar(self, "dynamics.easing.type");
        removeObjVar(self, "dynamics.easing.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleClearPush(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.push.vx");
        removeObjVar(self, "dynamics.push.vy");
        removeObjVar(self, "dynamics.push.vz");
        removeObjVar(self, "dynamics.push.duration");
        removeObjVar(self, "dynamics.push.space");
        removeObjVar(self, "dynamics.push.drag");
        return SCRIPT_CONTINUE;
    }

    private int handleClearSpin(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.spin.yaw");
        removeObjVar(self, "dynamics.spin.pitch");
        removeObjVar(self, "dynamics.spin.roll");
        removeObjVar(self, "dynamics.spin.duration");
        removeObjVar(self, "dynamics.spin.aroundCenter");
        return SCRIPT_CONTINUE;
    }

    private int handleClearBreathing(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.breathing.min");
        removeObjVar(self, "dynamics.breathing.max");
        removeObjVar(self, "dynamics.breathing.speed");
        removeObjVar(self, "dynamics.breathing.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleClearBounce(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.bounce.gravity");
        removeObjVar(self, "dynamics.bounce.elasticity");
        removeObjVar(self, "dynamics.bounce.velocity");
        removeObjVar(self, "dynamics.bounce.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleClearWobble(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.wobble.ampX");
        removeObjVar(self, "dynamics.wobble.ampY");
        removeObjVar(self, "dynamics.wobble.ampZ");
        removeObjVar(self, "dynamics.wobble.freqX");
        removeObjVar(self, "dynamics.wobble.freqY");
        removeObjVar(self, "dynamics.wobble.freqZ");
        removeObjVar(self, "dynamics.wobble.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleClearOrbit(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.orbit.centerX");
        removeObjVar(self, "dynamics.orbit.centerY");
        removeObjVar(self, "dynamics.orbit.centerZ");
        removeObjVar(self, "dynamics.orbit.radius");
        removeObjVar(self, "dynamics.orbit.speed");
        removeObjVar(self, "dynamics.orbit.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleApplyHover(obj_id self, dictionary params) throws InterruptedException
    {
        float hoverHeight = params.getFloat("hoverHeight");
        float bobAmplitude = params.getFloat("bobAmplitude");
        float bobSpeed = params.getFloat("bobSpeed");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.hover.height", hoverHeight);
        setObjVar(self, "dynamics.hover.bobAmplitude", bobAmplitude);
        setObjVar(self, "dynamics.hover.bobSpeed", bobSpeed);
        setObjVar(self, "dynamics.hover.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnHoverEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleApplyFollowTarget(obj_id self, dictionary params) throws InterruptedException
    {
        long followTargetIdValue = params.getLong("followTargetId");
        obj_id followTargetId = obj_id.getObjId(followTargetIdValue);
        float followDistance = params.getFloat("followDistance");
        float followSpeed = params.getFloat("followSpeed");
        float hoverHeight = params.getFloat("hoverHeight");
        float bobAmplitude = params.getFloat("bobAmplitude");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.follow.targetId", followTargetId);
        setObjVar(self, "dynamics.follow.distance", followDistance);
        setObjVar(self, "dynamics.follow.speed", followSpeed);
        setObjVar(self, "dynamics.follow.hoverHeight", hoverHeight);
        setObjVar(self, "dynamics.follow.bobAmplitude", bobAmplitude);
        setObjVar(self, "dynamics.follow.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnFollowTargetEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleClearHover(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.hover.height");
        removeObjVar(self, "dynamics.hover.bobAmplitude");
        removeObjVar(self, "dynamics.hover.bobSpeed");
        removeObjVar(self, "dynamics.hover.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleClearFollowTarget(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.follow.targetId");
        removeObjVar(self, "dynamics.follow.distance");
        removeObjVar(self, "dynamics.follow.speed");
        removeObjVar(self, "dynamics.follow.hoverHeight");
        removeObjVar(self, "dynamics.follow.bobAmplitude");
        removeObjVar(self, "dynamics.follow.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleApplySway(obj_id self, dictionary params) throws InterruptedException
    {
        float swingAngle = params.getFloat("swingAngle");
        float swingSpeed = params.getFloat("swingSpeed");
        float damping = params.getFloat("damping");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.sway.swingAngle", swingAngle);
        setObjVar(self, "dynamics.sway.swingSpeed", swingSpeed);
        setObjVar(self, "dynamics.sway.damping", damping);
        setObjVar(self, "dynamics.sway.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnSwayEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleClearSway(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.sway.swingAngle");
        removeObjVar(self, "dynamics.sway.swingSpeed");
        removeObjVar(self, "dynamics.sway.damping");
        removeObjVar(self, "dynamics.sway.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleApplyShake(obj_id self, dictionary params) throws InterruptedException
    {
        float intensity = params.getFloat("intensity");
        float frequency = params.getFloat("frequency");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.shake.intensity", intensity);
        setObjVar(self, "dynamics.shake.frequency", frequency);
        setObjVar(self, "dynamics.shake.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnShakeEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleClearShake(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.shake.intensity");
        removeObjVar(self, "dynamics.shake.frequency");
        removeObjVar(self, "dynamics.shake.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleApplyFloat(obj_id self, dictionary params) throws InterruptedException
    {
        float floatHeight = params.getFloat("floatHeight");
        float driftSpeed = params.getFloat("driftSpeed");
        float randomStrength = params.getFloat("randomStrength");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.float.height", floatHeight);
        setObjVar(self, "dynamics.float.driftSpeed", driftSpeed);
        setObjVar(self, "dynamics.float.randomStrength", randomStrength);
        setObjVar(self, "dynamics.float.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnFloatEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleClearFloat(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.float.height");
        removeObjVar(self, "dynamics.float.driftSpeed");
        removeObjVar(self, "dynamics.float.randomStrength");
        removeObjVar(self, "dynamics.float.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleApplyConveyor(obj_id self, dictionary params) throws InterruptedException
    {
        float directionX = params.getFloat("directionX");
        float directionY = params.getFloat("directionY");
        float directionZ = params.getFloat("directionZ");
        float speed = params.getFloat("speed");
        float wrapDistance = params.getFloat("wrapDistance");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.conveyor.dirX", directionX);
        setObjVar(self, "dynamics.conveyor.dirY", directionY);
        setObjVar(self, "dynamics.conveyor.dirZ", directionZ);
        setObjVar(self, "dynamics.conveyor.speed", speed);
        setObjVar(self, "dynamics.conveyor.wrapDistance", wrapDistance);
        setObjVar(self, "dynamics.conveyor.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnConveyorEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleClearConveyor(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.conveyor.dirX");
        removeObjVar(self, "dynamics.conveyor.dirY");
        removeObjVar(self, "dynamics.conveyor.dirZ");
        removeObjVar(self, "dynamics.conveyor.speed");
        removeObjVar(self, "dynamics.conveyor.wrapDistance");
        removeObjVar(self, "dynamics.conveyor.duration");
        return SCRIPT_CONTINUE;
    }

    private int handleApplyCarousel(obj_id self, dictionary params) throws InterruptedException
    {
        float centerX = params.getFloat("centerX");
        float centerY = params.getFloat("centerY");
        float centerZ = params.getFloat("centerZ");
        float radius = params.getFloat("radius");
        float rotationSpeed = params.getFloat("rotationSpeed");
        float verticalAmplitude = params.getFloat("verticalAmplitude");
        float verticalSpeed = params.getFloat("verticalSpeed");
        float duration = params.getFloat("duration");

        setObjVar(self, "dynamics.carousel.centerX", centerX);
        setObjVar(self, "dynamics.carousel.centerY", centerY);
        setObjVar(self, "dynamics.carousel.centerZ", centerZ);
        setObjVar(self, "dynamics.carousel.radius", radius);
        setObjVar(self, "dynamics.carousel.rotationSpeed", rotationSpeed);
        setObjVar(self, "dynamics.carousel.verticalAmplitude", verticalAmplitude);
        setObjVar(self, "dynamics.carousel.verticalSpeed", verticalSpeed);
        setObjVar(self, "dynamics.carousel.duration", duration);

        if (duration > 0.0f)
            messageTo(self, "OnCarouselEffectTick", null, (long)(duration * 1000), false);

        return SCRIPT_CONTINUE;
    }

    private int handleClearCarousel(obj_id self) throws InterruptedException
    {
        removeObjVar(self, "dynamics.carousel.centerX");
        removeObjVar(self, "dynamics.carousel.centerY");
        removeObjVar(self, "dynamics.carousel.centerZ");
        removeObjVar(self, "dynamics.carousel.radius");
        removeObjVar(self, "dynamics.carousel.rotationSpeed");
        removeObjVar(self, "dynamics.carousel.verticalAmplitude");
        removeObjVar(self, "dynamics.carousel.verticalSpeed");
        removeObjVar(self, "dynamics.carousel.duration");
        return SCRIPT_CONTINUE;
    }

    // =====================================================================
    // DURATION EXPIRY CALLBACKS
    // =====================================================================

    public int OnPushForceTick(obj_id self) throws InterruptedException
    {
        handleClearPush(self);
        return SCRIPT_CONTINUE;
    }

    public int OnSpinForceTick(obj_id self) throws InterruptedException
    {
        handleClearSpin(self);
        return SCRIPT_CONTINUE;
    }

    public int OnBreathingEffectTick(obj_id self) throws InterruptedException
    {
        handleClearBreathing(self);
        return SCRIPT_CONTINUE;
    }

    public int OnBounceEffectTick(obj_id self) throws InterruptedException
    {
        handleClearBounce(self);
        return SCRIPT_CONTINUE;
    }

    public int OnWobbleEffectTick(obj_id self) throws InterruptedException
    {
        handleClearWobble(self);
        return SCRIPT_CONTINUE;
    }

    public int OnOrbitEffectTick(obj_id self) throws InterruptedException
    {
        handleClearOrbit(self);
        return SCRIPT_CONTINUE;
    }

    public int OnHoverEffectTick(obj_id self) throws InterruptedException
    {
        handleClearHover(self);
        return SCRIPT_CONTINUE;
    }

    public int OnFollowTargetEffectTick(obj_id self) throws InterruptedException
    {
        handleClearFollowTarget(self);
        return SCRIPT_CONTINUE;
    }

    public int OnSwayEffectTick(obj_id self) throws InterruptedException
    {
        handleClearSway(self);
        return SCRIPT_CONTINUE;
    }

    public int OnShakeEffectTick(obj_id self) throws InterruptedException
    {
        handleClearShake(self);
        return SCRIPT_CONTINUE;
    }

    public int OnFloatEffectTick(obj_id self) throws InterruptedException
    {
        handleClearFloat(self);
        return SCRIPT_CONTINUE;
    }

    public int OnConveyorEffectTick(obj_id self) throws InterruptedException
    {
        handleClearConveyor(self);
        return SCRIPT_CONTINUE;
    }

    public int OnCarouselEffectTick(obj_id self) throws InterruptedException
    {
        handleClearCarousel(self);
        return SCRIPT_CONTINUE;
    }
}
