package script.library;

import script.*;

/**
 * POB / player ship turret helpers: fire at a world point.
 * <p>
 * Ground aim points are stored from {@code OnGroundTargetLoc} (e.g. {@code space.ship.summon_ship}
 * “Mark for bombardment”): {@code ITEM_USE}, then build a {@link location} from the tool and x,y,z.
 */
public class space_turret extends script.base_script
{
    public space_turret()
    {
    }

    /** Last world point picked via ground targeting, stored on the player (see {@link #setPlayerGroundStrikeTargetFromGroundPick}). */
    public static final String OV_PLAYER_GROUND_STRIKE_TARGET = "space.orbitalStrike.groundLoc";

    public static void setPlayerGroundStrikeTargetLocation(obj_id player, location loc) throws InterruptedException
    {
        if (!isIdValid(player) || loc == null)
        {
            return;
        }
        setObjVar(player, OV_PLAYER_GROUND_STRIKE_TARGET, loc);
    }

    /**
     * Same construction as {@code summon_ship.OnGroundTargetLoc}: base {@code getLocation(tool)}, assign x,y,z, optional cell from player.
     */
    public static void setPlayerGroundStrikeTargetFromGroundPick(obj_id player, obj_id tool, float x, float y, float z) throws InterruptedException
    {
        if (!isIdValid(player) || !isIdValid(tool))
        {
            return;
        }
        location loc = getLocation(tool);
        loc.x = x;
        loc.y = y;
        loc.z = z;
        if (!isInWorldCell(player))
        {
            loc.cell = getContainedBy(player);
        }
        setPlayerGroundStrikeTargetLocation(player, loc);
    }

    public static location getPlayerGroundStrikeTarget(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !hasObjVar(player, OV_PLAYER_GROUND_STRIKE_TARGET))
        {
            return null;
        }
        return getLocationObjVar(player, OV_PLAYER_GROUND_STRIKE_TARGET);
    }

    public static void clearPlayerGroundStrikeTarget(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player) || !hasObjVar(player, OV_PLAYER_GROUND_STRIKE_TARGET))
        {
            return;
        }
        removeObjVar(player, OV_PLAYER_GROUND_STRIKE_TARGET);
    }

    /**
     * Fire every installed turret weapon at {@code loc} (weapon index derived from chassis slots).
     * @return number of turrets that actually fired (per-server weapon rules)
     */
    public static int fireAllTurretsAtWorldLocation(obj_id ship, location loc) throws InterruptedException
    {
        if (!isIdValid(ship) || loc == null)
        {
            return 0;
        }
        int fired = 0;
        int[] slots = getShipChassisSlots(ship);
        if (slots == null)
        {
            return 0;
        }
        for (int i = 0; i < slots.length; ++i)
        {
            int slot = slots[i];
            if (slot < ship_chassis_slot_type.SCST_weapon_first || slot > ship_chassis_slot_type.SCST_weapon_last)
            {
                continue;
            }
            int widx = slot - ship_chassis_slot_type.SCST_weapon_0;
            if (!shipIsWeaponTurret(ship, widx))
            {
                continue;
            }
            if (shipFireTurretAtWorldLocation(ship, widx, loc.x, loc.y, loc.z))
            {
                ++fired;
            }
        }
        return fired;
    }

    /**
     * Uses {@link #getPlayerGroundStrikeTarget} (e.g. {@code summon_ship} “Mark for bombardment” on the datapad).
     * Must match current scene and atmospheric flight.
     */
    public static int fireOrbitalStrikeFromStoredGroundTarget(obj_id ship, obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
        {
            return 0;
        }
        if (!isAtmosphericFlightScene())
        {
            sendSystemMessage(player, string_id.unlocalized("Orbital strike is only available in atmospheric flight."));
            return 0;
        }
        location target = getPlayerGroundStrikeTarget(player);
        if (target == null)
        {
            sendSystemMessage(player, string_id.unlocalized("No bombardment point stored. Use your ship datapad (Mark for bombardment) on the surface first."));
            return 0;
        }
        String scene = getCurrentSceneName();
        if (target.area == null || !target.area.equals(scene))
        {
            sendSystemMessage(player, string_id.unlocalized("Your ground target is in a different area."));
            return 0;
        }
        int n = fireAllTurretsAtWorldLocation(ship, target);
        if (n == 0)
        {
            sendSystemMessage(player, string_id.unlocalized("No turrets fired (arcs, cooldowns, or non-turret weapons)."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Fired " + n + " turret shot(s) at your ground target."));
        }
        return n;
    }
}
