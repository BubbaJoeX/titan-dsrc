package script.library;

import script.*;
import script.library.money;
import script.space.combat.combat_ship;

/**
 * POB / player ship turret helpers: fire at a world point (e.g. datapad {@code OnGroundTargetLoc} ground pick).
 */
public class space_turret extends script.base_script
{
    public space_turret()
    {
    }

    /** {@link #fireOrbitalStrikeAtGroundPick} -- not in bombardment orbit, wrong scene / owner, or invalid. */
    public static final int ORBITAL_FIRE_NOT_ELIGIBLE = -1;
    /** {@link #fireOrbitalStrikeAtGroundPick} -- bombardment orbit active but ship farther than max horizontal range. */
    public static final int ORBITAL_FIRE_TOO_FAR = -2;

    /**
     * World location for a ground-targeting pick (same construction as {@code summon_ship.OnGroundTargetLoc}).
     */
    public static location locationFromGroundPick(obj_id tool, obj_id player, float x, float y, float z) throws InterruptedException
    {
        location loc = getLocation(tool);
        loc.x = x;
        loc.y = y;
        loc.z = z;
        if (!isInWorldCell(player))
        {
            loc.cell = getContainedBy(player);
        }
        return loc;
    }

    /**
     * Fire paid turrets at {@code target} if bombardment orbit is active and the ship is within horizontal range.
     * @return shots fired (including 0 if in range but nothing fired), or {@link #ORBITAL_FIRE_NOT_ELIGIBLE}, or {@link #ORBITAL_FIRE_TOO_FAR}
     */
    public static int fireOrbitalStrikeAtGroundPick(obj_id ship, obj_id player, location target, float maxHorizontalRange) throws InterruptedException
    {
        if (!isIdValid(ship) || !isIdValid(player) || target == null)
        {
            return ORBITAL_FIRE_NOT_ELIGIBLE;
        }
        if (!isAtmosphericFlightScene())
        {
            return ORBITAL_FIRE_NOT_ELIGIBLE;
        }
        obj_id owner = getOwner(ship);
        if (!isIdValid(owner) || owner != player)
        {
            return ORBITAL_FIRE_NOT_ELIGIBLE;
        }
        if (!hasObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE) || !getBooleanObjVar(ship, combat_ship.OV_SUMMON_BOMBARDMENT_ORBIT_ACTIVE))
        {
            return ORBITAL_FIRE_NOT_ELIGIBLE;
        }
        String scene = getCurrentSceneName();
        if (target.area == null || !target.area.equals(scene))
        {
            return ORBITAL_FIRE_NOT_ELIGIBLE;
        }
        location shipLoc = getWorldLocation(ship);
        if (shipLoc.area == null || !shipLoc.area.equals(scene))
        {
            return ORBITAL_FIRE_NOT_ELIGIBLE;
        }
        float dx = shipLoc.x - target.x;
        float dz = shipLoc.z - target.z;
        float horiz = (float) StrictMath.sqrt(dx * dx + dz * dz);
        if (horiz > maxHorizontalRange)
        {
            return ORBITAL_FIRE_TOO_FAR;
        }
        return fireAllTurretsAtWorldLocationWithPerShotCharge(ship, player, target, combat_ship.SUMMON_BOMBARDMENT_CREDIT_PER_SHOT);
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
                    sendSystemMessage(payer, string_id.unlocalized("Insufficient credits -- strike stopped after " + fired + " shot(s)."));
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
}
