package script.systems.turret;

import script.library.menu_info_types;
import script.library.turret_gunner_lib;
import script.library.utils;
import script.menu_info;
import script.obj_id;
import script.string_id;

/**
 * Radial: enter/exit gunner seat on player-controllable installation turrets.
 * Requires {@code turret.playerControllable} = 1 (or non-zero) on the turret.
 */
public class turret_gunner_station extends script.base_script
{
	public turret_gunner_station()
	{
	}

	private static final string_id SID_ENTER = new string_id("", "Enter Gunner Seat");
	private static final string_id SID_EXIT = new string_id("", "Exit Gunner Seat");

	public int OnInitialize(obj_id self) throws InterruptedException
	{
		if (turret_gunner_lib.isOccupied(self))
		{
			obj_id g = turret_gunner_lib.getOccupant(self);
			if (!isIdValid(g) || !exists(g))
			{
				utils.removeScriptVar(self, turret_gunner_lib.SCRIPTVAR_SUSPEND_AI_TRIGGERS);
				removeObjVar(self, turret_gunner_lib.VAR_GUNNER_OCCUPANT);
			}
		}
		return SCRIPT_CONTINUE;
	}

	public int OnRemovingFromWorld(obj_id self) throws InterruptedException
	{
		turret_gunner_lib.forceUnmountIfOccupant(self);
		return SCRIPT_CONTINUE;
	}

	public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
	{
		if (!isIdValid(player) || !isPlayer(player))
		{
			return SCRIPT_CONTINUE;
		}
		if (!turret_gunner_lib.isPlayerControllableTurret(self))
		{
			return SCRIPT_CONTINUE;
		}
		if (turret_gunner_lib.getMaxMountRange(self) + 1.0f < getDistance(self, player))
		{
			return SCRIPT_CONTINUE;
		}

		if (!turret_gunner_lib.isOccupied(self))
		{
			mi.addRootMenu(menu_info_types.SERVER_TURRET_GUNNER_ENTER, SID_ENTER);
		}
		else
		{
			obj_id occ = turret_gunner_lib.getOccupant(self);
			if (isIdValid(occ) && occ == player)
			{
				mi.addRootMenu(menu_info_types.SERVER_TURRET_GUNNER_EXIT, SID_EXIT);
			}
		}
		return SCRIPT_CONTINUE;
	}

	public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
	{
		if (!isIdValid(player) || !isPlayer(player))
		{
			return SCRIPT_CONTINUE;
		}
		if (item == menu_info_types.SERVER_TURRET_GUNNER_ENTER)
		{
			if (turret_gunner_lib.tryMount(self, player))
			{
				sendSystemMessageTestingOnly(player, "Mounted turret gunner seat.");
			}
			else
			{
				sendSystemMessageTestingOnly(player, "Cannot mount turret (occupied, out of range, or not controllable).");
			}
			return SCRIPT_CONTINUE;
		}
		if (item == menu_info_types.SERVER_TURRET_GUNNER_EXIT)
		{
			if (turret_gunner_lib.tryUnmount(self, player))
			{
				sendSystemMessageTestingOnly(player, "Left turret gunner seat.");
			}
			return SCRIPT_CONTINUE;
		}
		return SCRIPT_CONTINUE;
	}
}
