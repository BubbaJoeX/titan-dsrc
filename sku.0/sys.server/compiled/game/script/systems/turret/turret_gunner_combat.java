package script.systems.turret;

import script.library.ai_lib;
import script.library.turret;
import script.library.utils;
import script.obj_id;

/**
 * Combat-only slice of {@link turret_ai} for player gunner installations: keeps weapon setup and
 * gunner-driven shots ({@code handleGunnerSingleShot}) but does not auto-acquire targets via triggers.
 * Used when {@code systems.turret.turret_ai} is detached in favor of manual gunner control.
 */
public class
turret_gunner_combat extends turret_ai
{
	public turret_gunner_combat()
	{
	}

	@Override
	public int OnAttach(obj_id self) throws InterruptedException
	{
		turret.activateTurret(self);
		ai_lib.setAttackable(self, true);
		return SCRIPT_CONTINUE;
	}

	@Override
	public int OnInitialize(obj_id self) throws InterruptedException
	{
		return SCRIPT_CONTINUE;
	}

	@Override
	public int OnTriggerVolumeEntered(obj_id self, String volumeName, obj_id who) throws InterruptedException
	{
		return SCRIPT_CONTINUE;
	}

	@Override
	public int OnTriggerVolumeExited(obj_id self, String volumeName, obj_id who) throws InterruptedException
	{
		return SCRIPT_CONTINUE;
	}

	@Override
	public int OnSawAttack(obj_id self, obj_id defender, obj_id[] attackers) throws InterruptedException
	{
		return SCRIPT_CONTINUE;
	}

	@Override
	public int OnObjectDamaged(obj_id self, obj_id attacker, obj_id weapon, int damage) throws InterruptedException
	{
		int curHP = getHitpoints(self);
		if (curHP < 1)
		{
			explodeTurret(self, attacker);
			return SCRIPT_CONTINUE;
		}
		if (!utils.hasScriptVar(self, "playingEffect"))
		{
			int smolder = 2000;
			int fire = 1000;
			if (curHP < smolder)
			{
				if (curHP < fire)
				{
					playClientEffectLoc(attacker, "clienteffect/lair_hvy_damage_fire.cef", getLocation(self), 0);
					utils.setScriptVar(self, "playingEffect", 1);
					messageTo(self, "effectManager", null, 15, true);
				}
				else
				{
					playClientEffectLoc(attacker, "clienteffect/lair_med_damage_smoke.cef", getLocation(self), 0);
					utils.setScriptVar(self, "playingEffect", 1);
					messageTo(self, "effectManager", null, 15, true);
				}
			}
		}
		return SCRIPT_CONTINUE;
	}
}
