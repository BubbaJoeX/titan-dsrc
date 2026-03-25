package script.library;

import script.location;
import script.obj_id;

/**
 * Player-mounted installation turrets: occupancy, invulnerability, seat offset, AI trigger suspend.
 * <p>
 * Designer / dev objvars on the <b>turret</b>:<br>
 * {@code turret.playerControllable} (int/bool, non-zero = mountable radial)<br>
 * {@code turret.gunner.off_x}, {@code off_y}, {@code off_z} (float, meters in parent/world frame — see setLocation)<br>
 * {@code turret.gunner.max_range} (float, optional — radial mount range, default 12m)<br>
 * {@code turret.dev.attackSpeedScale} (float, optional — multiplies post-shot recycle delay in {@code turret_ai})<br>
 * <p>
 * Phase 2 (client / command table): mouse yaw/pitch and WASD strafe should drive aim via
 * {@code systems.turret.turret_gunner_station} messages or a dedicated command; this library keeps seat + state ready.
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
	public static final String VAR_MAX_RANGE = "turret.gunner.max_range";

	public static final String SCRIPTVAR_SUSPEND_AI_TRIGGERS = "turret.gunner.suspendAiTriggers";

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

		setTurretGunnerMountTurretId(player, turret);

		// Designer-controlled camera origin offset (local turret frame).
		// Defaults to a reasonable "eye height" in case objvars are not present.
		float eyeX = hasObjVar(turret, "turret.gunner.eye_off_x") ? getFloatObjVar(turret, "turret.gunner.eye_off_x") : 0.0f;
		float eyeY = hasObjVar(turret, "turret.gunner.eye_off_y") ? getFloatObjVar(turret, "turret.gunner.eye_off_y") : 1.6f;
		float eyeZ = hasObjVar(turret, "turret.gunner.eye_off_z") ? getFloatObjVar(turret, "turret.gunner.eye_off_z") : 0.0f;
		setTurretGunnerEyeOffsets(player, eyeX, eyeY, eyeZ);

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
			removeObjVar(player, VAR_PLAYER_MOUNTED_ON);
			removeObjVar(player, VAR_RET_X);
			removeObjVar(player, VAR_RET_Y);
			removeObjVar(player, VAR_RET_Z);
			removeObjVar(player, VAR_RET_CELL);
			removeObjVar(player, VAR_RET_SCENE);
		}
	}
}
