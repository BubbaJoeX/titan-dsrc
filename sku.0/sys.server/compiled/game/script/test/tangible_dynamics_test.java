package script.test;

/*
@Origin: dsrc.script.test
@Author: Titan Development Team
@Purpose: Test script for TangibleDynamics system
@Note: Demonstrates all features: push, spin, breathing, bounce, wobble, orbit, drag, easing
@Requirements: tangible_dynamics library, tangible_dynamics_handler on target
@Created: 2025
@Updated: 2026 - bounce, wobble, orbit, drag, easing tests
@Copyright © SWG: Titan 2026.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class tangible_dynamics_test extends script.base_script
{
    // Test menu options
    private static final int MENU_TEST_DYNAMICS   = menu_info_types.SERVER_MENU_1;
    private static final int MENU_PUSH_TEST       = menu_info_types.SERVER_MENU_2;
    private static final int MENU_SPIN_TEST       = menu_info_types.SERVER_MENU_3;
    private static final int MENU_BREATHING_TEST  = menu_info_types.SERVER_MENU_4;
    private static final int MENU_COMBINED_TEST   = menu_info_types.SERVER_MENU_5;
    private static final int MENU_BOUNCE_TEST     = menu_info_types.SERVER_MENU_6;
    private static final int MENU_WOBBLE_TEST     = menu_info_types.SERVER_MENU_7;
    private static final int MENU_ORBIT_TEST      = menu_info_types.SERVER_MENU_8;
    private static final int MENU_DRAG_TEST       = menu_info_types.SERVER_MENU_9;
    private static final int MENU_CLEAR_ALL       = menu_info_types.SERVER_MENU_10;

    /**
     * Main initialization for test object
     */
    public int OnInitialize(obj_id self) throws InterruptedException
    {
        // Attach the handler script if not already attached
        attachScript(self, "handler.tangible_dynamics_handler");

        setScriptVar(self, "test_object", true);
        return SCRIPT_CONTINUE;
    }

    /**
     * Show test menu when right-clicked
     */
    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        if (names == null || attribs == null || names.length != attribs.length)
            return SCRIPT_CONTINUE;

        int index = 0;
        if (index < names.length)
        {
            boolean active = hasCondition(self, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
            names[index] = "Dynamics Active";
            attribs[index] = active ? "YES" : "NO";
            index++;
        }

        if (index < names.length && hasObjVar(self, "dynamics.push.vx"))
        {
            float vx = getFloatObjVar(self, "dynamics.push.vx");
            float vy = getFloatObjVar(self, "dynamics.push.vy");
            float vz = getFloatObjVar(self, "dynamics.push.vz");
            names[index] = "Push Velocity";
            attribs[index] = String.format("(%.2f, %.2f, %.2f)", vx, vy, vz);
            index++;
        }

        if (index < names.length && hasObjVar(self, "dynamics.spin.yaw"))
        {
            float yaw = getFloatObjVar(self, "dynamics.spin.yaw");
            names[index] = "Spin Rate";
            attribs[index] = String.format("%.2f rad/s", yaw);
            index++;
        }

        if (index < names.length && hasObjVar(self, "dynamics.breathing.min"))
        {
            float minS = getFloatObjVar(self, "dynamics.breathing.min");
            float maxS = getFloatObjVar(self, "dynamics.breathing.max");
            names[index] = "Breathing";
            attribs[index] = String.format("%.2f - %.2f", minS, maxS);
            index++;
        }

        return SCRIPT_CONTINUE;
    }

    /**
     * Handle menu selections
     */
    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (player == null)
            return SCRIPT_CONTINUE;

        if (item == MENU_PUSH_TEST)
            testPushForce(self, player);
        else if (item == MENU_SPIN_TEST)
            testSpinForce(self, player);
        else if (item == MENU_BREATHING_TEST)
            testBreathingEffect(self, player);
        else if (item == MENU_COMBINED_TEST)
            testCombinedForces(self, player);
        else if (item == MENU_BOUNCE_TEST)
            testBounceEffect(self, player);
        else if (item == MENU_WOBBLE_TEST)
            testWobbleEffect(self, player);
        else if (item == MENU_ORBIT_TEST)
            testOrbitEffect(self, player);
        else if (item == MENU_DRAG_TEST)
            testDragForce(self, player);
        else if (item == MENU_CLEAR_ALL)
        {
            tangible_dynamics.clearAllForces(self);
            sendSystemMessage(player, "All dynamics forces cleared.");
        }

        return SCRIPT_CONTINUE;
    }

    // --- Push: upward for 3 seconds ---
    private void testPushForce(obj_id self, obj_id player) throws InterruptedException
    {
        tangible_dynamics.applyPushForce(self, 0.0f, 3.0f, 0.0f, 3.0f, tangible_dynamics.SPACE_WORLD);
        sendSystemMessage(player, "Push force applied: upward 3m/s for 3s.");
    }

    // --- Spin: yaw 180deg/s for 5 seconds ---
    private void testSpinForce(obj_id self, obj_id player) throws InterruptedException
    {
        float rotSpeed = (float)Math.PI;
        tangible_dynamics.applySpinForce(self, rotSpeed, 0.0f, 0.0f, 5.0f, false);
        sendSystemMessage(player, "Spin applied: PI rad/s yaw for 5s.");
    }

    // --- Breathing: scale 0.8-1.2 for 4 seconds ---
    private void testBreathingEffect(obj_id self, obj_id player) throws InterruptedException
    {
        tangible_dynamics.applyBreathingEffect(self, 0.8f, 1.2f, 1.5f, 4.0f);
        sendSystemMessage(player, "Breathing applied: 0.8-1.2 scale for 4s.");
    }

    // --- Bounce: gravity 9.8, elasticity 0.7, initial launch 8 m/s ---
    private void testBounceEffect(obj_id self, obj_id player) throws InterruptedException
    {
        tangible_dynamics.applyBounceEffect(self, 9.8f, 0.7f, 8.0f, 10.0f);
        sendSystemMessage(player, "Bounce applied: gravity=9.8, elasticity=0.7, launch=8m/s.");
    }

    // --- Wobble: gentle oscillation on all axes ---
    private void testWobbleEffect(obj_id self, obj_id player) throws InterruptedException
    {
        tangible_dynamics.applyWobbleEffect(self,
            0.3f, 0.2f, 0.3f,   // amplitude X, Y, Z (meters)
            1.0f, 1.5f, 0.8f,   // frequency X, Y, Z (cycles/sec)
            6.0f);               // duration
        sendSystemMessage(player, "Wobble applied: oscillating for 6s.");
    }

    // --- Orbit: circle around current position ---
    private void testOrbitEffect(obj_id self, obj_id player) throws InterruptedException
    {
        location loc = getLocation(self);
        tangible_dynamics.applyOrbitEffect(self,
            loc.x, loc.y, loc.z,  // center = current position
            3.0f,                  // radius 3m
            (float)Math.PI,        // PI rad/s = half revolution per second
            8.0f);                 // duration
        sendSystemMessage(player, "Orbit applied: 3m radius, half-rev/s for 8s.");
    }

    // --- Push with drag: shove sideways with heavy friction ---
    private void testDragForce(obj_id self, obj_id player) throws InterruptedException
    {
        tangible_dynamics.applyPushForceWithDrag(self,
            5.0f, 0.0f, 0.0f,  // velocity X only
            2.0f,                // drag coefficient (heavy)
            -1.0f,               // no duration limit (drag stops it)
            tangible_dynamics.SPACE_WORLD);
        sendSystemMessage(player, "Push with drag applied: 5m/s sideways, drag=2.0.");
    }

    // --- Combined: push + spin + breathing ---
    private void testCombinedForces(obj_id self, obj_id player) throws InterruptedException
    {
        float spinSpeed = (float)Math.PI / 2.0f;
        tangible_dynamics.applyCombinedForces(self,
            0.0f, 1.5f, 0.0f,   // push (gentle up)
            spinSpeed, 0.0f, 0.0f, // spin
            0.9f, 1.1f, 1.0f,   // breathing
            6.0f);               // duration
        sendSystemMessage(player, "Combined forces applied for 6s.");
    }

    /**
     * Get menu options
     */
    public void GetMenuItems(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (player == null)
            return;

        int root = mi.addRootMenu(MENU_TEST_DYNAMICS, new string_id("tangible_dynamics", "test_menu_title"));

        mi.addSubMenu(root, MENU_PUSH_TEST, new string_id("tangible_dynamics", "test_push"));
        mi.addSubMenu(root, MENU_SPIN_TEST, new string_id("tangible_dynamics", "test_spin"));
        mi.addSubMenu(root, MENU_BREATHING_TEST, new string_id("tangible_dynamics", "test_breathing"));
        mi.addSubMenu(root, MENU_BOUNCE_TEST, new string_id("tangible_dynamics", "test_bounce"));
        mi.addSubMenu(root, MENU_WOBBLE_TEST, new string_id("tangible_dynamics", "test_wobble"));
        mi.addSubMenu(root, MENU_ORBIT_TEST, new string_id("tangible_dynamics", "test_orbit"));
        mi.addSubMenu(root, MENU_DRAG_TEST, new string_id("tangible_dynamics", "test_drag"));
        mi.addSubMenu(root, MENU_COMBINED_TEST, new string_id("tangible_dynamics", "test_combined"));

        mi.addSeparator(root);
        mi.addSubMenu(root, MENU_CLEAR_ALL, new string_id("tangible_dynamics", "clear_all"));
    }
}
