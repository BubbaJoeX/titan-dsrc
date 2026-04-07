package script.library;

import script.dictionary;
import script.location;
import script.obj_id;

/**
 * Player-mounted installation turrets: occupancy, invulnerability, seat offset, AI trigger suspend.
 * <p>
 * Designer / dev objvars on the <b>turret</b>:<br>
 * {@code turret.playerControllable} (int/bool, non-zero = mountable radial)<br>
 * {@code turret.gunner.off_x}, {@code off_y}, {@code off_z} (float, meters in parent/world frame — see setLocation)<br>
 * {@code turret.gunner.max_range} (float, optional — radial mount range, default 12m)<br>
 * {@code turret.gunner.damage_percent} (int, optional — gunner hit damage as % of defender max HP, 1–100, default 12)<br>
 * {@code turret.gunner.sync_leader} (int, optional — non-zero = GM “sync mode”: other turrets in range mirror gunner fire)<br>
 * {@code turret.gunner.sync_range} (float, optional — meters for sync neighbor search, default 80)<br>
 * {@code turret.dev.attackSpeedScale} (float, optional — multiplies post-shot recycle delay in {@code turret_ai})<br>
 * <p>
 * Gunner aim is driven client-side (camera + turret mesh slew) with server combat commands
 * {@code turretGunnerAim} / {@code turretGunnerFire}; this library keeps seat + state ready.
 */
public class turret_gunner_lib extends script.base_script
{
	public turret_gunner_lib()
	{
	}

	/** When set (non-zero), {@link script.systems.turret.turret_gunner_station} swaps {@code turret_ai} for {@code turret_gunner_combat} on load. */
	public static final String VAR_DEV_GUNNER_ONLY = "turret.dev.gunnerOnly";

	public static final String VAR_PLAYER_CONTROLLABLE = "turret.playerControllable";
	public static final String VAR_GUNNER_OCCUPANT = "turret.gunner.occupant";
	public static final String VAR_PLAYER_MOUNTED_ON = "turretGunner.mountedOn";

	public static final String VAR_OFF_X = "turret.gunner.off_x";
	public static final String VAR_OFF_Y = "turret.gunner.off_y";
	public static final String VAR_OFF_Z = "turret.gunner.off_z";
	public static final String VAR_EYE_OFF_X = "turret.gunner.eye_off_x";
	public static final String VAR_EYE_OFF_Y = "turret.gunner.eye_off_y";
	public static final String VAR_EYE_OFF_Z = "turret.gunner.eye_off_z";
	public static final String VAR_MAX_RANGE = "turret.gunner.max_range";

	public static final String SCRIPTVAR_SUSPEND_AI_TRIGGERS = "turret.gunner.suspendAiTriggers";

	/** Batch script var on player while the “load weapon stats” listbox is open. */
	public static final String SCRIPTVAR_LOAD_WEAPON_IDS = "turret.gunner.loadWeaponIds";

	/** Integer objvar: non-zero means this turret is a sync leader (GM). */
	public static final String VAR_SYNC_LEADER = "turret.gunner.sync_leader";
	/** Float objvar: max distance (m) from leader to slave turrets for sync. */
	public static final String VAR_SYNC_RANGE = "turret.gunner.sync_range";
	public static final float DEFAULT_SYNC_RANGE = 80.0f;

	/** World-space aim point (from gunner camera); used for cone acquisition instead of reticle object id. */
	public static final String SCRIPTVAR_AIM_WX = "turret.gunner.aim_wx";
	public static final String SCRIPTVAR_AIM_WY = "turret.gunner.aim_wy";
	public static final String SCRIPTVAR_AIM_WZ = "turret.gunner.aim_wz";

	public static final String VAR_RET_CELL = "turretGunner.ret.cell";
	public static final String VAR_RET_X = "turretGunner.ret.x";
	public static final String VAR_RET_Y = "turretGunner.ret.y";
	public static final String VAR_RET_Z = "turretGunner.ret.z";
	public static final String VAR_RET_SCENE = "turretGunner.ret.scene";

	public static final float DEFAULT_MAX_MOUNT_RANGE = 12.0f;

	public static boolean isPlayerControllableTurret(obj_id turret) throws InterruptedException
	{
		return isIdValid(turret) && exists(turret) && hasObjVar(turret, VAR_PLAYER_CONTROLLABLE) && getIntObjVar(turret, VAR_PLAYER_CONTROLLABLE) != 0;
	}

	public static boolean isOccupied(obj_id turret) throws InterruptedException
	{
		if (!isIdValid(turret) || !exists(turret) || !hasObjVar(turret, VAR_GUNNER_OCCUPANT))
		{
			return false;
		}
		obj_id g = getObjIdObjVar(turret, VAR_GUNNER_OCCUPANT);
		return isIdValid(g) && exists(g);
	}

	public static obj_id getOccupant(obj_id turret) throws InterruptedException
	{
		if (!isOccupied(turret))
		{
			return obj_id.NULL_ID;
		}
		return getObjIdObjVar(turret, VAR_GUNNER_OCCUPANT);
	}

	public static float getMaxMountRange(obj_id turret) throws InterruptedException
	{
		if (hasObjVar(turret, VAR_MAX_RANGE))
		{
			return getFloatObjVar(turret, VAR_MAX_RANGE);
		}
		return DEFAULT_MAX_MOUNT_RANGE;
	}

	/**
	 * True if {@code item} is anywhere under the player's container tree (inventory, equipped slots, bags, etc.).
	 */
	public static boolean isItemInPlayerContainerTree(obj_id player, obj_id item) throws InterruptedException
	{
		if (!isIdValid(player) || !isIdValid(item))
		{
			return false;
		}
		obj_id c = getContainedBy(item);
		while (isIdValid(c))
		{
			if (c == player)
			{
				return true;
			}
			c = getContainedBy(c);
		}
		return false;
	}

	/**
	 * Copies min/max damage, attack speed, and elemental damage from {@code weaponSource} onto the turret's
	 * {@code objWeapon} (weapon template unchanged).
	 */
	public static void applyWeaponStatsFromWeaponObject(obj_id turret, obj_id weaponSource) throws InterruptedException
	{
		if (!isIdValid(turret) || !isIdValid(weaponSource) || !exists(turret) || !exists(weaponSource))
		{
			return;
		}
		if (!isWeapon(weaponSource))
		{
			return;
		}
		if (!hasObjVar(turret, "objWeapon") && !script.library.turret.createWeapon(turret))
		{
			return;
		}
		obj_id tw = getObjIdObjVar(turret, "objWeapon");
		if (!isIdValid(tw))
		{
			return;
		}
		int minD = getWeaponMinDamage(weaponSource);
		int maxD = getWeaponMaxDamage(weaponSource);
		if (maxD < minD)
		{
			int swap = minD;
			minD = maxD;
			maxD = swap;
		}
		setWeaponMinDamage(tw, minD);
		setWeaponMaxDamage(tw, maxD);
		float spd = getWeaponAttackSpeed(weaponSource);
		if (spd > 0.0f)
		{
			setWeaponAttackSpeed(tw, spd);
		}
		int elemType = getWeaponElementalType(weaponSource);
		int elemVal = getWeaponElementalValue(weaponSource);
		setWeaponElementalDamage(tw, elemType, elemVal);
	}

	/**
	 * Applies stats from a weapon the player actually carries (inventory / equipment tree).
	 */
	public static boolean tryApplyWeaponStatsFromInventoryWeapon(obj_id turret, obj_id player, obj_id weapon) throws InterruptedException
	{
		if (!isIdValid(turret) || !isIdValid(player) || !isIdValid(weapon))
		{
			return false;
		}
		if (!exists(turret) || !exists(player) || !exists(weapon))
		{
			return false;
		}
		if (!isWeapon(weapon))
		{
			return false;
		}
		if (!isItemInPlayerContainerTree(player, weapon))
		{
			return false;
		}
		applyWeaponStatsFromWeaponObject(turret, weapon);
		return true;
	}

	/**
	 * Copies base kinetic DPS band, attack speed, and elemental damage from the player's held weapon onto the
	 * turret's spawned {@code objWeapon} (template/type unchanged).
	 */
	public static void applyPlayerHeldWeaponStatsToTurretWeapon(obj_id turret, obj_id player) throws InterruptedException
	{
		if (!isIdValid(turret) || !isIdValid(player) || !exists(turret) || !exists(player))
		{
			return;
		}
		obj_id held = getHeldWeapon(player);
		if (!isIdValid(held) || !isWeapon(held))
		{
			return;
		}
		applyWeaponStatsFromWeaponObject(turret, held);
	}

	public static float getSyncRangeMeters(obj_id turret) throws InterruptedException
	{
		if (!hasObjVar(turret, VAR_SYNC_RANGE))
		{
			return DEFAULT_SYNC_RANGE;
		}
		float r = getFloatObjVar(turret, VAR_SYNC_RANGE);
		if (r < 1.0f)
		{
			return 1.0f;
		}
		if (r > 512.0f)
		{
			return 512.0f;
		}
		return r;
	}

	public static boolean isSyncLeader(obj_id turret) throws InterruptedException
	{
		return isIdValid(turret) && exists(turret) && hasObjVar(turret, VAR_SYNC_LEADER) && getIntObjVar(turret, VAR_SYNC_LEADER) != 0;
	}

	private static boolean isTurretSyncNeighbor(obj_id candidate, obj_id leader) throws InterruptedException
	{
		if (!isIdValid(candidate) || candidate == leader)
		{
			return false;
		}
		if (!exists(candidate))
		{
			return false;
		}
		if (getHitpoints(candidate) < 1)
		{
			return false;
		}
		if (!hasObjVar(candidate, "objWeapon"))
		{
			return false;
		}
		if (utils.hasScriptVar(candidate, SCRIPTVAR_SUSPEND_AI_TRIGGERS))
		{
			return false;
		}
		return hasScript(candidate, "systems.turret.turret_ai") || hasScript(candidate, "systems.turret.turret_gunner_combat");
	}

	/**
	 * After a successful gunner shot from {@code leader}, tells nearby eligible turrets to fire in parallel.
	 *
	 * @param useWorldAimCone if true, slaves use the leader's world aim point and each picks a target in cone; if false, slaves shoot {@code explicitTarget} using the leader gunner's PvP rules.
	 */
	public static void propagateGunnerSyncFire(obj_id leader, obj_id gunner, obj_id explicitTarget, float aimX, float aimY, float aimZ, boolean useWorldAimCone) throws InterruptedException
	{
		if (!isSyncLeader(leader))
		{
			return;
		}
		if (!isIdValid(gunner) || !exists(gunner) || !isPlayer(gunner))
		{
			return;
		}
		float range = getSyncRangeMeters(leader);
		location loc = getLocation(leader);
		obj_id[] near = getObjectsInRange(loc, range);
		if (near == null || near.length == 0)
		{
			return;
		}
		float stagger = 0.0f;
		for (obj_id other : near)
		{
			if (!isTurretSyncNeighbor(other, leader))
			{
				continue;
			}
			if (getDistance(leader, other) > range)
			{
				continue;
			}
			dictionary d = new dictionary();
			d.put("syncGunner", gunner);
			if (useWorldAimCone)
			{
				d.put("aimX", aimX);
				d.put("aimY", aimY);
				d.put("aimZ", aimZ);
				messageTo(other, "handleSyncedSalvoDirectional", d, stagger, false);
			}
			else
			{
				if (!isIdValid(explicitTarget))
				{
					continue;
				}
				d.put("syncTarget", explicitTarget);
				messageTo(other, "handleSyncedSalvoTarget", d, stagger, false);
			}
			stagger += 0.03f;
			if (stagger > 0.45f)
			{
				stagger = 0.45f;
			}
		}
	}

	public static void clearAiTargetsAndEngagement(obj_id turretObj) throws InterruptedException
	{
		if (!isIdValid(turretObj))
		{
			return;
		}
		turret.disengage(turretObj);
		if (utils.hasScriptVar(turretObj, turret.SCRIPTVAR_TARGETS))
		{
			utils.removeBatchScriptVar(turretObj, turret.SCRIPTVAR_TARGETS);
		}
		utils.removeScriptVar(turretObj, "ai.combat.isInCombat");
		utils.removeScriptVar(turretObj, turret.SCRIPTVAR_ENGAGED);
	}

	public static boolean tryMount(obj_id turret, obj_id player) throws InterruptedException
	{
		if (!isIdValid(turret) || !isIdValid(player) || !exists(turret) || !exists(player))
		{
			return false;
		}
		if (!isPlayerControllableTurret(turret))
		{
			return false;
		}
		if (isOccupied(turret))
		{
			return false;
		}
		if (isDead(player) || isIncapacitated(player))
		{
			return false;
		}
		float maxR = getMaxMountRange(turret);
		if (getDistance(turret, player) > maxR + 0.5f)
		{
			return false;
		}

		location here = getLocation(player);
		setObjVar(player, VAR_RET_X, here.x);
		setObjVar(player, VAR_RET_Y, here.y);
		setObjVar(player, VAR_RET_Z, here.z);
		setObjVar(player, VAR_RET_CELL, here.cell);
		setObjVar(player, VAR_RET_SCENE, getCurrentSceneName());

		location seat = getLocation(turret);
		float ox = hasObjVar(turret, VAR_OFF_X) ? getFloatObjVar(turret, VAR_OFF_X) : 0.0f;
		float oy = hasObjVar(turret, VAR_OFF_Y) ? getFloatObjVar(turret, VAR_OFF_Y) : 0.25f;
		float oz = hasObjVar(turret, VAR_OFF_Z) ? getFloatObjVar(turret, VAR_OFF_Z) : 0.0f;
		seat.x += ox;
		seat.y += oy;
		seat.z += oz;

		clearAiTargetsAndEngagement(turret);
		utils.setScriptVar(turret, SCRIPTVAR_SUSPEND_AI_TRIGGERS, true);

		setObjVar(turret, VAR_GUNNER_OCCUPANT, player);
		setObjVar(player, VAR_PLAYER_MOUNTED_ON, turret);

		setInvulnerable(player, true);
		setLocation(player, seat);
		setPosture(player, POSTURE_UPRIGHT);
		setAnimationMood(player, "calm");

		setTurretGunnerMountTurretId(player, turret);

		// Designer-controlled camera origin offset (local turret frame).
		// Defaults to a reasonable "eye height" in case objvars are not present.
		float eyeX = hasObjVar(turret, VAR_EYE_OFF_X) ? getFloatObjVar(turret, VAR_EYE_OFF_X) : 0.0f;
		float eyeY = hasObjVar(turret, VAR_EYE_OFF_Y) ? getFloatObjVar(turret, VAR_EYE_OFF_Y) : 1.6f;
		float eyeZ = hasObjVar(turret, VAR_EYE_OFF_Z) ? getFloatObjVar(turret, VAR_EYE_OFF_Z) : 0.0f;
		setTurretGunnerEyeOffsets(player, eyeX, eyeY, eyeZ);

		applyPlayerHeldWeaponStatsToTurretWeapon(turret, player);

		return true;
	}

	public static boolean tryUnmount(obj_id turret, obj_id player) throws InterruptedException
	{
		if (!isIdValid(turret) || !isIdValid(player))
		{
			return false;
		}
		if (!hasObjVar(turret, VAR_GUNNER_OCCUPANT) || getObjIdObjVar(turret, VAR_GUNNER_OCCUPANT) != player)
		{
			return false;
		}

		utils.removeScriptVar(turret, SCRIPTVAR_SUSPEND_AI_TRIGGERS);
		utils.removeScriptVar(turret, "turret.gunner.manualTarget");
		utils.removeScriptVar(turret, SCRIPTVAR_AIM_WX);
		utils.removeScriptVar(turret, SCRIPTVAR_AIM_WY);
		utils.removeScriptVar(turret, SCRIPTVAR_AIM_WZ);

		utils.removeBatchScriptVar(player, SCRIPTVAR_LOAD_WEAPON_IDS);

		removeObjVar(turret, VAR_GUNNER_OCCUPANT);
		removeObjVar(player, VAR_PLAYER_MOUNTED_ON);

		setTurretGunnerMountTurretId(player, obj_id.NULL_ID);

		// Clear offsets to prevent a stale camera origin when switching views.
		setTurretGunnerEyeOffsets(player, 0.0f, 1.6f, 0.0f);

		setInvulnerable(player, false);

		if (hasObjVar(player, VAR_RET_X))
		{
			location back = new location();
			back.x = getFloatObjVar(player, VAR_RET_X);
			back.y = getFloatObjVar(player, VAR_RET_Y);
			back.z = getFloatObjVar(player, VAR_RET_Z);
			back.cell = getObjIdObjVar(player, VAR_RET_CELL);
			String sc = getStringObjVar(player, VAR_RET_SCENE);
			if (sc != null && sc.equals(getCurrentSceneName()))
			{
				setLocation(player, back);
			}
		}
		removeObjVar(player, VAR_RET_X);
		removeObjVar(player, VAR_RET_Y);
		removeObjVar(player, VAR_RET_Z);
		removeObjVar(player, VAR_RET_CELL);
		removeObjVar(player, VAR_RET_SCENE);

		setPosture(player, POSTURE_UPRIGHT);
		setAnimationMood(player, "calm");

		return true;
	}

	public static void forceUnmountIfOccupant(obj_id turret) throws InterruptedException
	{
		obj_id g = getOccupant(turret);
		if (isIdValid(g))
		{
			tryUnmount(turret, g);
		}
		else if (hasObjVar(turret, VAR_GUNNER_OCCUPANT))
		{
			utils.removeScriptVar(turret, SCRIPTVAR_SUSPEND_AI_TRIGGERS);
			utils.removeScriptVar(turret, SCRIPTVAR_AIM_WX);
			utils.removeScriptVar(turret, SCRIPTVAR_AIM_WY);
			utils.removeScriptVar(turret, SCRIPTVAR_AIM_WZ);
			removeObjVar(turret, VAR_GUNNER_OCCUPANT);
		}
	}

	public static void onPlayerLogout(obj_id player) throws InterruptedException
	{
		if (!isIdValid(player) || !hasObjVar(player, VAR_PLAYER_MOUNTED_ON))
		{
			return;
		}
		obj_id turret = getObjIdObjVar(player, VAR_PLAYER_MOUNTED_ON);
		if (isIdValid(turret) && exists(turret))
		{
			tryUnmount(turret, player);
		}
		else
		{
			setInvulnerable(player, false);
			setPosture(player, POSTURE_UPRIGHT);
			setAnimationMood(player, "calm");
			removeObjVar(player, VAR_PLAYER_MOUNTED_ON);
			removeObjVar(player, VAR_RET_X);
			removeObjVar(player, VAR_RET_Y);
			removeObjVar(player, VAR_RET_Z);
			removeObjVar(player, VAR_RET_CELL);
			removeObjVar(player, VAR_RET_SCENE);
		}
	}

	/** Integer objvar on turret: max % of defender max HP per gunner hit (1–100). Default {@link #DEFAULT_GUNNER_DAMAGE_PERCENT}. */
	public static final String VAR_GUNNER_DAMAGE_PERCENT = "turret.gunner.damage_percent";
	public static final int DEFAULT_GUNNER_DAMAGE_PERCENT = 12;

	public static int getGunnerDamagePercent(obj_id turret) throws InterruptedException
	{
		if (!isIdValid(turret) || !hasObjVar(turret, VAR_GUNNER_DAMAGE_PERCENT))
		{
			return DEFAULT_GUNNER_DAMAGE_PERCENT;
		}
		int p = getIntObjVar(turret, VAR_GUNNER_DAMAGE_PERCENT);
		if (p < 1)
		{
			return 1;
		}
		if (p > 100)
		{
			return 100;
		}
		return p;
	}

	/** Gunner hit damage: {@code percent}% of defender max hitpoints (rounded), at least 1. */
	public static int computeGunnerPercentHitDamage(obj_id defender, int percent) throws InterruptedException
	{
		if (!isIdValid(defender))
		{
			return 0;
		}
		int maxHp = getMaxHitpoints(defender);
		if (maxHp < 1)
		{
			maxHp = 1;
		}
		int pct = percent;
		if (pct < 1)
		{
			pct = 1;
		}
		if (pct > 100)
		{
			pct = 100;
		}
		long raw = ((long) maxHp * (long) pct + 50L) / 100L;
		if (raw < 1L)
		{
			raw = 1L;
		}
		if (raw > (long) Integer.MAX_VALUE)
		{
			return Integer.MAX_VALUE;
		}
		return (int) raw;
	}
}
