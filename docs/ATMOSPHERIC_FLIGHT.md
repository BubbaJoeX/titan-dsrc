# Atmospheric Flight System

Atmospheric flight allows players to pilot ships on ground scenes (e.g., Tatooine, Naboo, Corellia) rather than only in dedicated space zones. Ships fly above terrain, interact with ground objects, and can land/park for boarding.

---

## Table of Contents

1. [Scene Detection](#scene-detection)
2. [Ship Launching](#ship-launching)
3. [Ship Landing & Packing](#ship-landing--packing)
4. [Single Ship Policy](#single-ship-policy)
5. [Autopilot System](#autopilot-system)
6. [Ship Summon](#ship-summon)
7. [POB Ship Boarding & Parking](#pob-ship-boarding--parking)
8. [Disembarking](#disembarking)
9. [Ship-to-Ground Combat](#ship-to-ground-combat)
10. [AI Ships on Ground](#ai-ships-on-ground)
11. [Visibility System](#visibility-system)
12. [Altitude Enforcement & Space Transition](#altitude-enforcement--space-transition)
13. [DPVS Crash Prevention](#dpvs-crash-prevention)
14. [Planet Map Integration](#planet-map-integration)
15. [Native C++ Methods](#native-c-methods)
16. [File Reference](#file-reference)

---

## Scene Detection

Atmospheric flight is available on all ground scenes **except** `kashyyyk_`* and `mustafar`.


| Function                                  | Language      | Location                                         |
| ----------------------------------------- | ------------- | ------------------------------------------------ |
| `ServerWorld::isAtmosphericFlightScene()` | C++           | `ServerWorld.cpp`                                |
| `isAtmosphericFlightScene()`              | Java (native) | `base_class.java`                                |
| `isShipScene()`                           | Both          | Returns true for space **or** atmospheric flight |
| `isSpaceOrAtmosphericScene()`             | Java          | `space_transition.java`                          |


---

## Ship Launching

**Trigger**: Radial menu on a Ship Control Device (SCD) → **Atmospheric Flight** → **Launch Ship**, or via a Starship Terminal.

**Flow**:

1. `ship_control_device.OnObjectMenuRequest` adds the **Atmospheric Flight** submenu with **Launch Ship** if conditions are met.
2. `ship_control_device.OnObjectMenuSelect` calls `space_transition.launchToAtmosphere(player, scd)`.
3. `launchToAtmosphere` validates the single-ship policy, sets launch objvars, and calls `handlePotentialSceneChange`.
4. `handlePotentialSceneChange` calls `unpackShipForPlayer` which places the ship at the player's location + 200m altitude, pilots the player, makes POB cells public, attaches `ship_atmospheric_boarding`, and starts the altitude monitor.

**Files**: `ship_control_device.java`, `terminal_space.java`, `space_transition.java`

---

## Ship Landing & Packing

**Trigger**: Radial on SCD → **Atmospheric Flight** → **Land Ship**.

**Flow**:

1. `ship_control_device.OnObjectMenuSelect` calls `space_transition.packShip(ship)`.
2. `packShip` clears autopilot, detaches boarding script, ejects all players to terrain.
3. In atmospheric flight, the actual ship teardown is **delayed 5 seconds** via `messageTo("delayedPackShipFinalize")`. The handler then moves the ship 10 km away from the owner (so the client drops it from DPVS), waits 2 more seconds, then calls `packShipFinalize` to prevent a DPVS crash (see below).
4. `packShipFinalize` puts the ship back into the SCD and dirties all SCD radial menus.

**Files**: `space_transition.java`, `combat_ship.java` (handler for `delayedPackShipFinalize`)

---

## Single Ship Policy

Only one ship may be deployed at a time per player in atmospheric flight.

- `space_transition.getDeployedShipForPlayer(player)` scans the player's SCDs for one whose contents are empty but has a `"ship"` objvar pointing to a valid object in the world.
- `launchToAtmosphere` checks this before allowing a new launch.
- The SCD radial menu hides **Launch Ship** if another SCD's ship is already deployed.
- `space_transition.dirtyAllShipControlDevices(player)` sends `sendDirtyObjectMenuNotification` to all SCDs after launch or land, so radial menus update immediately without re-opening.

**Files**: `space_transition.java`, `ship_control_device.java`

---

## Autopilot System

A physics-driven autopilot that flies POB ships autonomously through four phases.

### Phases


| Phase          | Enum                | Behavior                                                                                                                 |
| -------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| **Ascending**  | `AP_ASCENDING (1)`  | Elevator-style vertical climb to 500m above terrain. No yaw/pitch/roll changes.                                          |
| **Cruising**   | `AP_CRUISING (2)`   | Yaw toward target, full throttle. Fixed cruise altitude (no terrain following). Boosted speed (engine + booster × 0.75). |
| **Descending** | `AP_DESCENDING (3)` | Elevator-style vertical descent to 200m above destination terrain. No yaw/pitch/roll changes.                            |
| **Arrived**    | `AP_ARRIVED (4)`    | All inputs zero. Autopilot disengaged.                                                                                   |


### C++ Implementation (`PlayerShipController`)

- `setAutopilotTarget(target, takeoffAlt, landingAlt)` -- Flattens ship transform (yaw only), resets dynamics model, begins ascending.
- `realAlter()` -- Per-frame state machine. During cruising, uses `AutopilotShipObjectInterface` which overrides max speed (base + booster × 0.75), yaw rate (2.0 rad/s), and zeroes pitch/roll limits.
- `receiveTransform()` -- Returns early when autopilot is active, blocking client input.
- Yaw control uses a dead-zone (0.02 rad / ~1°) with direct `setYawRate(0)` to prevent oscillation/spinning.

### Java Monitoring (`combat_ship.java`)

- `shipAutoPilotEngage` -- Validates ownership, calls native `shipSetAutopilotTarget`, calculates ETA, broadcasts roleplay messages.
- `shipAutoPilotTick` -- Polls `shipGetAutopilotPhase()` every 2 seconds. On phase transitions, broadcasts messages (ascending, cruising, descending, arrived). Periodic status updates during cruising (distance, bearing, ETA).
- `shipAutoPilotCancel` / `shipAutoPilotCancelInternal` -- Clears autopilot, broadcasts cancellation messages.

### Triggering Autopilot

- **Planet Map** (right-click → **Auto-Pilot Here**): Works whether piloting or just aboard a POB ship as owner.
- **Cancel**: Planet Map → **Cancel Auto-Pilot**, or a pilot taking the helm.

### Constants


| Constant                    | Value                |
| --------------------------- | -------------------- |
| `AUTOPILOT_TAKEOFF_ALT`     | 500m                 |
| `AUTOPILOT_LANDING_ALT`     | 200m                 |
| `AUTOPILOT_MONITOR_RATE`    | 2 seconds            |
| `AUTOPILOT_STATUS_INTERVAL` | Every 10 ticks (20s) |
| Elevator speed              | 30 m/s               |


**Files**: `PlayerShipController.cpp`, `PlayerShipController.h`, `combat_ship.java`, `player_vehicle.java`, `SwgCuiPlanetMap.cpp`

---

## Ship Summon

Allows a player on the ground to summon their deployed POB ship to their location via autopilot.

**Script**: `space.ship.summon_ship` -- Attach to any crafted object to grant the radial.

**Trigger**: Radial menu → **Summon Ship** (only visible in atmospheric flight when a deployed ship exists and the player is not aboard it).

**Behavior**:

1. Finds the player's deployed ship by scanning SCDs.
2. Validates: ship has interior, not already on autopilot, not being piloted.
3. Sends `messageTo(ship, "shipSummonEngage")` with the player's X/Z coordinates.
4. Ship autopilots to the player: 500m takeoff, **50m landing altitude** (close enough to board).
5. Ground player receives periodic status messages (distance, ETA).
6. On arrival: "Your ship has arrived at your location. You may now board your ship."
7. Passengers aboard the ship also receive roleplay messages throughout.

**Files**: `summon_ship.java`, `combat_ship.java` (`shipSummonEngage` handler)

---

## POB Ship Boarding & Parking

POB ships can be "parked" in atmospheric flight -- the ship stays loaded in the world after the pilot exits, allowing players to board from the ground.

### Boarding from Ground

**Script**: `space.ship.ship_atmospheric_boarding` (auto-attached on launch)

**Trigger**: Radial menu on the ship exterior → **Board Ship** (within 500m, atmospheric flight, ship has interior).

**Behavior**: Transfers the player into the ship's first cell via `space_transition.boardShipFromGround()`. Permissions are managed via SUI (owner can set public/private/friends-only).

### Exiting to Ground

**Script**: `escape_hatch.java`

**Trigger**: Radial on escape pod/hatch → **Depart through Boarding Ramp** (only in atmospheric flight, ship not being piloted).

**Behavior**: Calls `space_transition.disembarkShip()` which places the player on terrain below the ship.

**Files**: `ship_atmospheric_boarding.java`, `escape_hatch.java`, `space_transition.java`

---

## Disembarking

`space_transition.disembarkShip(player, ship)`:

- Unpilots if the player is the pilot.
- Stands the player up.
- Calculates ground position below the ship.
- In atmospheric flight: uses `setLocation` (same-scene teleport).
- In space: uses `warpPlayer` (scene transition).

**Files**: `space_transition.java`

---

## Ship-to-Ground Combat

Ship weapons can damage ground targets in atmospheric flight.

- **Terrain collision**: Projectiles stop when hitting terrain height.
- **Creature hits**: Proximity-based hit detection for creatures without collision extents.
- **Damage**: Ship weapon damage applied to ground creatures.
- **Splash damage**: `applySplashDamage()` for area-of-effect hits.
- **PvP rules**: Only damages players who are overt factionally, TEF'd, or dueling.

**Files**: `ProjectileManager.cpp`, combat scripts

---

## AI Ships on Ground

### Spawner Script (`ship_atmospheric_spawner.java`)

Attach to a spawner object with objvars:

- `atmo.spawns` -- String array of ship template types
- `atmo.spawnCount` -- Number of ships per type
- `atmo.cruiseAltitude` -- Flight altitude (default 200m)
- `atmo.spawnRadius` -- Spawn radius around spawner
- `atmo.behavior` -- `loiter`, `patrol`, or `idle`

Ships spawn at terrain-aware altitudes and follow patrol paths where each waypoint's Y is `getHeightAtLocation() + cruiseAlt`.

### QA Tool (`qatool.java`)

**Command**: `/qat spawnAtmoShip <type> <count> <behavior> <altitude>`

**Behaviors**: `loiter`, `patrol`, `idle`, `follow`, `attack`

**Example**: `/qat spawnAtmoShip awing_tier6 3 loiter 200`

**Files**: `ship_atmospheric_spawner.java`, `qatool.java`

---

## Visibility System

Ships at altitude need to see ground objects (NPCs, buildings, creatures) and vice versa. The ground visibility system (`NetworkTriggerVolume`) has limited vertical range, so atmospheric flight leverages the 3D `SpaceVisibilityManager`.

**Changes**:

- `Client.cpp`, `ObserveTracker.cpp`, `CreatureObject.cpp`, `ServerObject.cpp`: Changed `isSpaceScene()` checks to `isShipScene()` so atmospheric flight scenes use `SpaceVisibilityManager`.
- `ServerObject.cpp`: Removed filter that prevented non-player objects (NPCs, buildings) from being added to `SpaceVisibilityManager` in atmospheric scenes.
- `SpaceVisibilityManager.cpp`: Increased `ms_maxCoordinate` from 4096 to 8192 to cover full ground map extents.

**Files**: `ServerObject.cpp`, `SpaceVisibilityManager.cpp`, `Client.cpp`, `ObserveTracker.cpp`, `CreatureObject.cpp`

---

## Altitude Enforcement & Space Transition

### Minimum Altitude

`PlayerShipController::realAlter()` enforces a minimum altitude of terrain + 15m in atmospheric scenes, preventing ships from clipping into terrain.

### Space Transition

`combat_ship.checkAtmosphericAltitude` (scheduled every 2s):

- At 4000m: Broadcasts warning -- "Approaching upper atmosphere boundary."
- At 5000m: Triggers warp to the adjacent space zone (e.g., `tatooine` → `space_tatooine`).

**Files**: `PlayerShipController.cpp`, `combat_ship.java`

---

## DPVS Crash Prevention

**Problem**: When packing a ship in atmospheric flight, the server sends player extraction and ship destruction messages in the same frame. The client's dPVS occlusion culling library crashes (access violation in `dpvs.dll`) because it references ship cell data that is being destroyed.

**Fix**: In atmospheric flight, `packShip` defers the actual ship teardown by 5 seconds via `messageTo(ship, "delayedPackShipFinalize", null, 5.0f)`. When that runs, the server first moves the ship to a location 10 km away from the owner (`prepareShipForPackDpvsSafe`), so the client can drop it from DPVS occlusion. After 2 more seconds, `delayedPackShipFinalizePhase2` runs and calls `space_transition.packShipFinalize()`, putting the ship into the SCD. The ship is thus packed only after it has left the player's view, avoiding the dpvs.dll access violation. After moving the player to terrain, the server also sends a `warpPlayer` to the same location so the client performs a full relocation.

**Files**: `space_transition.java`, `combat_ship.java`

---

## Planet Map Integration

`SwgCuiPlanetMap.cpp` adds context menu options when right-clicking the map:


| Option                | Condition                                                     | Action                                   |
| --------------------- | ------------------------------------------------------------- | ---------------------------------------- |
| **Auto-Pilot Here**   | Piloting a ship, or in skyway, or aboard own POB ship in atmo | Engages autopilot to clicked coordinates |
| **Cancel Auto-Pilot** | Inside POB ship in atmo (not piloting)                        | Sends `AutoPilotCancel` message          |


**Flow**:

- If piloting: client-side `PlayerShipController::engageAutopilotToLocation()`.
- If aboard POB (not piloting): sends `GenericValueTypeMessage("AutoPilotWaypoint")` → `player_vehicle.handleAutoPilotWaypoint` → `messageTo(ship, "shipAutoPilotEngage")`.

**Files**: `SwgCuiPlanetMap.cpp`, `player_vehicle.java`

---

## Native C++ Methods

Exposed to Java scripts via JNI (`ScriptMethodsPilot.cpp`, declared in `base_class.java`):


| Method                   | Signature                                     | Purpose                                                         |
| ------------------------ | --------------------------------------------- | --------------------------------------------------------------- |
| `shipSetAutopilotTarget` | `(obj_id, float, float, float, float) → bool` | Start autopilot: ship, targetX, targetZ, takeoffAlt, landingAlt |
| `shipClearAutopilot`     | `(obj_id) → bool`                             | Stop autopilot, reset dynamics                                  |
| `shipIsAutopilotActive`  | `(obj_id) → bool`                             | Check if autopilot is running                                   |
| `shipGetAutopilotPhase`  | `(obj_id) → int`                              | Get current phase (0-4)                                         |


**Files**: `ScriptMethodsPilot.cpp`, `base_class.java`, `PlayerShipController.cpp`

---

## File Reference

### Server Scripts (Java)


| File                             | Purpose                                                                                            |
| -------------------------------- | -------------------------------------------------------------------------------------------------- |
| `space_transition.java`          | Core launch/land/pack/unpack/disembark/boarding logic, single ship policy, SCD dirty notifications |
| `combat_ship.java`               | Autopilot engage/tick/cancel/summon, altitude checks, delayed pack handler                         |
| `ship_control_device.java`       | SCD radial menu (Launch/Land/Repair)                                                               |
| `terminal_space.java`            | Starship terminal launch-to-atmosphere                                                             |
| `player_vehicle.java`            | Routes autopilot/cancel messages from client to ship                                               |
| `escape_hatch.java`              | Boarding ramp departure                                                                            |
| `ship_atmospheric_boarding.java` | Ground-to-ship boarding radial and permissions                                                     |
| `ship_atmospheric_spawner.java`  | AI ship spawner for atmospheric scenes                                                             |
| `summon_ship.java`               | Ship summon radial script                                                                          |
| `qatool.java`                    | `/qat spawnAtmoShip` command                                                                       |
| `base_class.java`                | Native method declarations                                                                         |


### Server C++


| File                          | Purpose                                                          |
| ----------------------------- | ---------------------------------------------------------------- |
| `PlayerShipController.cpp/.h` | Autopilot state machine, physics overrides, altitude enforcement |
| `ScriptMethodsPilot.cpp`      | JNI bindings for autopilot native methods                        |
| `ServerObject.cpp`            | Visibility manager registration for atmospheric objects          |
| `SpaceVisibilityManager.cpp`  | 3D visibility grid (expanded to 8192 for ground maps)            |
| `Client.cpp`                  | Client visibility registration for ship scenes                   |
| `ObserveTracker.cpp`          | Observation routing for ship scenes                              |
| `CreatureObject.cpp`          | Container transfer visibility updates                            |
| `CreatureObject_Ships.cpp`    | Unpilot transform flattening (DPVS crash avoidance)              |
| `ServerWorld.cpp`             | `isAtmosphericFlightScene()` / `isShipScene()` definitions       |


### Client C++


| File                                | Purpose                                                 |
| ----------------------------------- | ------------------------------------------------------- |
| `SwgCuiPlanetMap.cpp`               | Planet map autopilot UI options                         |
| `FreeChaseCamera.cpp`               | Camera yaw fix for ship interiors in atmospheric flight |
| `PlayerShipController.cpp` (client) | Client-side autopilot engagement                        |


