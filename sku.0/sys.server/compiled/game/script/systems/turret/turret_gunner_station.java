package script.systems.turret;

import script.dictionary;
import script.menu_info_types;
import script.library.sui;
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
	private static final float GM_NUDGE_STEP = 0.1f;

	private static final string_id SID_ENTER = new string_id("", "Enter Gunner Seat");
	private static final string_id SID_EXIT = new string_id("", "Exit Gunner Seat");
	private static final string_id SID_GM_SEAT = new string_id("", "GM: Set seat offsets (x y z)");
	private static final string_id SID_GM_EYE = new string_id("", "GM: Set eye/camera offsets (x y z)");
	private static final string_id SID_GM_NUDGE = new string_id("", "GM: Nudge offsets…");

	public turret_gunner_station()
	{
	}

	public int OnInitialize(obj_id self) throws InterruptedException
	{
		if (hasObjVar(self, turret_gunner_lib.VAR_DEV_GUNNER_ONLY) && getIntObjVar(self, turret_gunner_lib.VAR_DEV_GUNNER_ONLY) != 0)
		{
			if (hasScript(self, "systems.turret.turret_ai"))
			{
				detachScript(self, "systems.turret.turret_ai");
			}
			if (!hasScript(self, "systems.turret.turret_gunner_combat"))
			{
				attachScript(self, "systems.turret.turret_gunner_combat");
			}
		}
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

		// God tools reuse generic SERVER_MENU slots (labels from SID_*; already in radial_menu.tab).
		if (isGod(player))
		{
			mi.addRootMenu(menu_info_types.SERVER_MENU1, SID_GM_SEAT);
			mi.addRootMenu(menu_info_types.SERVER_MENU2, SID_GM_EYE);
			mi.addRootMenu(menu_info_types.SERVER_MENU3, SID_GM_NUDGE);
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
		if (item == menu_info_types.SERVER_MENU1)
		{
			if (!isGod(player))
			{
				return SCRIPT_CONTINUE;
			}
			float ox = hasObjVar(self, turret_gunner_lib.VAR_OFF_X) ? getFloatObjVar(self, turret_gunner_lib.VAR_OFF_X) : 0.0f;
			float oy = hasObjVar(self, turret_gunner_lib.VAR_OFF_Y) ? getFloatObjVar(self, turret_gunner_lib.VAR_OFF_Y) : 0.25f;
			float oz = hasObjVar(self, turret_gunner_lib.VAR_OFF_Z) ? getFloatObjVar(self, turret_gunner_lib.VAR_OFF_Z) : 0.0f;
			String def = ox + " " + oy + " " + oz;
			String prompt = "Seat position offsets relative to turret (meters). Enter three numbers: x y z\nExample: 0 0.25 0";
			sui.inputbox(self, player, prompt, "GM: Seat offsets", "handleGmSeatOffsetsInput", def);
			return SCRIPT_CONTINUE;
		}
		if (item == menu_info_types.SERVER_MENU2)
		{
			if (!isGod(player))
			{
				return SCRIPT_CONTINUE;
			}
			float ex = hasObjVar(self, turret_gunner_lib.VAR_EYE_OFF_X) ? getFloatObjVar(self, turret_gunner_lib.VAR_EYE_OFF_X) : 0.0f;
			float ey = hasObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Y) ? getFloatObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Y) : 1.6f;
			float ez = hasObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Z) ? getFloatObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Z) : 0.0f;
			String def = ex + " " + ey + " " + ez;
			String prompt = "FPS camera origin in turret-local space (meters). Enter three numbers: x y z\nExample: 0 1.6 0";
			sui.inputbox(self, player, prompt, "GM: Eye offsets", "handleGmEyeOffsetsInput", def);
			return SCRIPT_CONTINUE;
		}
		if (item == menu_info_types.SERVER_MENU3)
		{
			if (!isGod(player))
			{
				return SCRIPT_CONTINUE;
			}
			String[] items =
			{
				"Eye Y +0.1",
				"Eye Y -0.1",
				"Eye X +0.1",
				"Eye X -0.1",
				"Eye Z +0.1",
				"Eye Z -0.1",
				"Seat Y +0.1",
				"Seat Y -0.1",
				"Seat X +0.1",
				"Seat X -0.1",
				"Seat Z +0.1",
				"Seat Z -0.1",
				"Reset eye → 0 1.6 0",
				"Reset seat → 0 0.25 0"
			};
			sui.listbox(self, player, "Adjust turret.gunner offsets in small steps (meters).", sui.OK_CANCEL, "GM: Nudge offsets", items, "handleGmNudgeOffsets", true, false);
			return SCRIPT_CONTINUE;
		}
		return SCRIPT_CONTINUE;
	}

	public int handleGmSeatOffsetsInput(obj_id self, dictionary params) throws InterruptedException
	{
		obj_id player = sui.getPlayerId(params);
		if (!isGod(player))
		{
			return SCRIPT_CONTINUE;
		}
		if (sui.getIntButtonPressed(params) != sui.BP_OK)
		{
			return SCRIPT_CONTINUE;
		}
		String raw = sui.getInputBoxText(params);
		if (raw == null || raw.trim().isEmpty())
		{
			sendSystemMessageTestingOnly(player, "[GM] Empty input.");
			return SCRIPT_CONTINUE;
		}
		String[] tok = raw.trim().split("[\\s,]+");
		if (tok.length < 3)
		{
			sendSystemMessageTestingOnly(player, "[GM] Need three numbers: x y z");
			return SCRIPT_CONTINUE;
		}
		try
		{
			float x = Float.parseFloat(tok[0]);
			float y = Float.parseFloat(tok[1]);
			float z = Float.parseFloat(tok[2]);
			setObjVar(self, turret_gunner_lib.VAR_OFF_X, x);
			setObjVar(self, turret_gunner_lib.VAR_OFF_Y, y);
			setObjVar(self, turret_gunner_lib.VAR_OFF_Z, z);
			sendSystemMessageTestingOnly(player, "[GM] Seat offsets set: " + x + " " + y + " " + z);
		}
		catch (NumberFormatException e)
		{
			sendSystemMessageTestingOnly(player, "[GM] Invalid number format.");
		}
		return SCRIPT_CONTINUE;
	}

	public int handleGmEyeOffsetsInput(obj_id self, dictionary params) throws InterruptedException
	{
		obj_id player = sui.getPlayerId(params);
		if (!isGod(player))
		{
			return SCRIPT_CONTINUE;
		}
		if (sui.getIntButtonPressed(params) != sui.BP_OK)
		{
			return SCRIPT_CONTINUE;
		}
		String raw = sui.getInputBoxText(params);
		if (raw == null || raw.trim().isEmpty())
		{
			sendSystemMessageTestingOnly(player, "[GM] Empty input.");
			return SCRIPT_CONTINUE;
		}
		String[] tok = raw.trim().split("[\\s,]+");
		if (tok.length < 3)
		{
			sendSystemMessageTestingOnly(player, "[GM] Need three numbers: x y z");
			return SCRIPT_CONTINUE;
		}
		try
		{
			float x = Float.parseFloat(tok[0]);
			float y = Float.parseFloat(tok[1]);
			float z = Float.parseFloat(tok[2]);
			setObjVar(self, turret_gunner_lib.VAR_EYE_OFF_X, x);
			setObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Y, y);
			setObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Z, z);
			sendSystemMessageTestingOnly(player, "[GM] Eye offsets set: " + x + " " + y + " " + z);
			if (turret_gunner_lib.isOccupied(self))
			{
				obj_id g = turret_gunner_lib.getOccupant(self);
				if (isIdValid(g))
				{
					setTurretGunnerEyeOffsets(g, x, y, z);
				}
			}
		}
		catch (NumberFormatException e)
		{
			sendSystemMessageTestingOnly(player, "[GM] Invalid number format.");
		}
		return SCRIPT_CONTINUE;
	}

	public int handleGmNudgeOffsets(obj_id self, dictionary params) throws InterruptedException
	{
		obj_id player = sui.getPlayerId(params);
		if (!isGod(player))
		{
			return SCRIPT_CONTINUE;
		}
		if (sui.getIntButtonPressed(params) != sui.BP_OK)
		{
			return SCRIPT_CONTINUE;
		}
		int row = sui.getListboxSelectedRow(params);
		float ex = hasObjVar(self, turret_gunner_lib.VAR_EYE_OFF_X) ? getFloatObjVar(self, turret_gunner_lib.VAR_EYE_OFF_X) : 0.0f;
		float ey = hasObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Y) ? getFloatObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Y) : 1.6f;
		float ez = hasObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Z) ? getFloatObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Z) : 0.0f;
		float sx = hasObjVar(self, turret_gunner_lib.VAR_OFF_X) ? getFloatObjVar(self, turret_gunner_lib.VAR_OFF_X) : 0.0f;
		float sy = hasObjVar(self, turret_gunner_lib.VAR_OFF_Y) ? getFloatObjVar(self, turret_gunner_lib.VAR_OFF_Y) : 0.25f;
		float sz = hasObjVar(self, turret_gunner_lib.VAR_OFF_Z) ? getFloatObjVar(self, turret_gunner_lib.VAR_OFF_Z) : 0.0f;
		switch (row)
		{
			case 0:
				ey += GM_NUDGE_STEP;
				break;
			case 1:
				ey -= GM_NUDGE_STEP;
				break;
			case 2:
				ex += GM_NUDGE_STEP;
				break;
			case 3:
				ex -= GM_NUDGE_STEP;
				break;
			case 4:
				ez += GM_NUDGE_STEP;
				break;
			case 5:
				ez -= GM_NUDGE_STEP;
				break;
			case 6:
				sy += GM_NUDGE_STEP;
				break;
			case 7:
				sy -= GM_NUDGE_STEP;
				break;
			case 8:
				sx += GM_NUDGE_STEP;
				break;
			case 9:
				sx -= GM_NUDGE_STEP;
				break;
			case 10:
				sz += GM_NUDGE_STEP;
				break;
			case 11:
				sz -= GM_NUDGE_STEP;
				break;
			case 12:
				ex = 0.0f;
				ey = 1.6f;
				ez = 0.0f;
				break;
			case 13:
				sx = 0.0f;
				sy = 0.25f;
				sz = 0.0f;
				break;
			default:
				return SCRIPT_CONTINUE;
		}
		if (row <= 5 || row == 12)
		{
			setObjVar(self, turret_gunner_lib.VAR_EYE_OFF_X, ex);
			setObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Y, ey);
			setObjVar(self, turret_gunner_lib.VAR_EYE_OFF_Z, ez);
			sendSystemMessageTestingOnly(player, "[GM] Eye offsets now: " + ex + " " + ey + " " + ez);
			if (turret_gunner_lib.isOccupied(self))
			{
				obj_id g = turret_gunner_lib.getOccupant(self);
				if (isIdValid(g))
				{
					setTurretGunnerEyeOffsets(g, ex, ey, ez);
				}
			}
		}
		if ((row >= 6 && row <= 11) || row == 13)
		{
			setObjVar(self, turret_gunner_lib.VAR_OFF_X, sx);
			setObjVar(self, turret_gunner_lib.VAR_OFF_Y, sy);
			setObjVar(self, turret_gunner_lib.VAR_OFF_Z, sz);
			sendSystemMessageTestingOnly(player, "[GM] Seat offsets now: " + sx + " " + sy + " " + sz);
		}
		return SCRIPT_CONTINUE;
	}
}
