package script.library;

import script.*;
import script.library.money;
import script.space.combat.combat_ship;

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

    /** {@link #tryInstantOrbitalStrikeAfterGroundMark} — not in bombardment orbit or wrong scene / owner. */
    public static final int INSTANT_MARK_NOT_ELIGIBLE = -1;
    /** {@link #tryInstantOrbitalStrikeAfterGroundMark} — bombardment orbit active but ship farther than max horizontal range. */
    public static final int INSTANT_MARK_TOO_FAR = -2;

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
     * Call right after {@link #setPlayerGroundStrikeTargetFromGroundPick}. If bombardment orbit is active and the ship is within
     * horizontal range of the stored point, runs the same per-shot charged salvo as the management terminal.
     * @return shots fired (including 0 if in range but nothing fired), or {@link #INSTANT_MARK_NOT_ELIGIBLE}, or {@link #INSTANT_MARK_TOO_FAR}
     */
    public static int tryInstantOrbitalStrikeAfterGroundMark(obj_id ship, obj_id player, float maxHorizontalRange) throws InterruptedException
    {
        if (!isIdValid(ship) || !isIdValid(player))
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        if (!isAtmosphericFlightScene())
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        obj_id owner = getOwner(ship);
        if (!isIdValid(owner) || owner != player)
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        if (!hasObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) || !getBooleanObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        location target = getPlayerGroundStrikeTarget(player);
        if (target == null)
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        String scene = getCurrentSceneName();
        if (target.area == null || !target.area.equals(scene))
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        location shipLoc = getWorldLocation(ship);
        if (shipLoc.area == null || !shipLoc.area.equals(scene))
        {
            return INSTANT_MARK_NOT_ELIGIBLE;
        }
        float dx = shipLoc.x - target.x;
        float dz = shipLoc.z - target.z;
        float horiz = (float) StrictMath.sqrt(dx * dx + dz * dz);
        if (horiz > maxHorizontalRange)
        {
            return INSTANT_MARK_TOO_FAR;
        }
        return fireAllTurretsAtWorldLocationWithPerShotCharge(ship, player, target, combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT);
    }

    /**
     * Same as {@link #fireAllTurretsAtWorldLocation}, but charges {@code payer} {@code creditsPerShot} for each successful shot
     * (paid before firing; refunded if the weapon fails to fire).
     */
    public static int fireAllTurretsAtWorldLocationWithPerShotCharge(obj_id ship, obj_id payer, location loc, int creditsPerShot) throws InterruptedException
    {
        if (!isIdValid(ship) || !isIdValid(payer) || loc == null || creditsPerShot <= 0)
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
            if (getTotalMoney(payer) < creditsPerShot)
            {
                if (fired == 0)
                {
                    sendSystemMessage(payer, string_id.unlocalized("You need at least " + creditsPerShot + " credits per turret shot."));
                }
                else
                {
                    sendSystemMessage(payer, string_id.unlocalized("Insufficient credits — strike stopped after " + fired + " shot(s)."));
                }
                return fired;
            }
            if (!transferBankCreditsToNamedAccount(payer, money.ACCT_TRAVEL, creditsPerShot, "noHandler", "noHandler", new dictionary()))
            {
                sendSystemMessage(payer, string_id.unlocalized("Payment failed."));
                return fired;
            }
            if (!shipFireTurretAtWorldLocation(ship, widx, loc.x, loc.y, loc.z))
            {
                transferBankCreditsFromNamedAccount(money.ACCT_TRAVEL, payer, creditsPerShot, "noHandler", "noHandler", new dictionary());
                continue;
            }
            ++fired;
        }
        return fired;
    }

    /**
     * Uses {@link #getPlayerGroundStrikeTarget} (e.g. {@code summon_ship} “Mark for bombardment” on the datapad).
     * Requires {@link combat_ship#OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE} on the ship; bills per successful shot.
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
        obj_id owner = getOwner(ship);
        if (!isIdValid(owner) || owner != player)
        {
            sendSystemMessage(player, string_id.unlocalized("Only the ship owner may use orbital strike."));
            return 0;
        }
        if (!hasObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) || !getBooleanObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
        {
            sendSystemMessage(player, string_id.unlocalized("Orbital strike requires Bombardment orbit ("
                + combat_ship.SUMMON_BOMBARDMENT_ORBIT_ACTIVATION_COST + " cr) from your datapad Starship Remote."));
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
        int n = fireAllTurretsAtWorldLocationWithPerShotCharge(ship, player, target, combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT);
        if (n == 0)
        {
            sendSystemMessage(player, string_id.unlocalized("No turrets fired (arcs, cooldowns, non-turret weapons, or insufficient credits)."));
        }
        else
        {
            sendSystemMessage(player, string_id.unlocalized("Fired " + n + " turret shot(s) (" + combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT + " cr each)."));
        }
        return n;
    }
}
