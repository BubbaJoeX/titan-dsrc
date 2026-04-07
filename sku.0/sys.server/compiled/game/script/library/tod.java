package script.library;

/**
 * Time-of-day helpers for scene environment cycle ({@link #getLocalTime()}, {@link #getLocalDayLength()}).
 * <p>
 * Semantics match {@code base_class} natives: normalized cycle position {@code t} in {@code [0, 1)} is
 * <b>day</b> when {@code t < getLocalDayLength()} and <b>night</b> when {@code t >= getLocalDayLength()}.
 * Use with spawners via {@link #isNightOnlySpawner(obj_id)} / {@link #canSpawnForTimePolicy(obj_id)}.
 */
public class tod extends script.base_script
{
	public tod()
	{
	}

	/**
	 * When non-zero on a spawner, {@link #canSpawnForTimePolicy(obj_id)} requires {@link #isNight()}.
	 */
	public static final String OBJVAR_SPAWNER_NIGHT_ONLY = "spawner.night_only";

	/**
	 * When non-zero on a spawner, {@link #canSpawnForTimePolicy(obj_id)} requires {@link #isDay()}.
	 */
	public static final String OBJVAR_SPAWNER_DAY_ONLY = "spawner.day_only";

	/**
	 * Seconds before retrying spawn when {@link #OBJVAR_SPAWNER_NIGHT_ONLY} / {@link #OBJVAR_SPAWNER_DAY_ONLY} blocks a tick (min 5).
	 */
	public static final String OBJVAR_TOD_RETRY_SECONDS = "spawner.tod_retry_seconds";

	public static final float DEFAULT_TOD_RETRY_SECONDS = 60f;

	/** Normalized position in the terrain environment cycle [0, 1). */
	public static float getCyclePosition() throws InterruptedException
	{
		return getLocalTime();
	}

	/**
	 * Fraction of the full cycle that counts as &quot;day&quot; (from cycle start). Remainder is night.
	 * Returns 0 if terrain/time is unavailable (see {@link #hasSceneCycle()}).
	 */
	public static float getDayFractionOfCycle() throws InterruptedException
	{
		return getLocalDayLength();
	}

	/** True when {@link #getLocalDayLength()} reports a usable day/night split (terrain present). */
	public static boolean hasSceneCycle() throws InterruptedException
	{
		return getLocalDayLength() > 0.f;
	}

	/**
	 * Day phase: {@code getCyclePosition() < getDayFractionOfCycle()}.
	 * If there is no scene cycle, treated as day so open-world spawners are not blocked.
	 */
	public static boolean isDay() throws InterruptedException
	{
		float split = getLocalDayLength();
		if (split <= 0.f)
		{
			return true;
		}
		return getLocalTime() < split;
	}

	/**
	 * Night phase: {@code getCyclePosition() >= getDayFractionOfCycle()}.
	 * If there is no scene cycle, false (night-only spawners do not run without a valid cycle).
	 */
	public static boolean isNight() throws InterruptedException
	{
		float split = getLocalDayLength();
		if (split <= 0.f)
		{
			return false;
		}
		return getLocalTime() >= split;
	}

	/** Position within the night segment [0, 1), or 0 when not night or no cycle. */
	public static float getNightProgress() throws InterruptedException
	{
		if (!isNight())
		{
			return 0.f;
		}
		float split = getLocalDayLength();
		float t = getLocalTime();
		float nightLen = 1.f - split;
		if (nightLen <= 1.0e-6f)
		{
			return 0.f;
		}
		return (t - split) / nightLen;
	}

	/** Position within the day segment [0, 1), or 0 when not day or no cycle. */
	public static float getDayProgress() throws InterruptedException
	{
		if (!isDay() || !hasSceneCycle())
		{
			return 0.f;
		}
		float split = getLocalDayLength();
		if (split <= 1.0e-6f)
		{
			return 0.f;
		}
		return getLocalTime() / split;
	}

	public static boolean isNightOnlySpawner(obj_id spawner) throws InterruptedException
	{
		return isIdValid(spawner) && hasObjVar(spawner, OBJVAR_SPAWNER_NIGHT_ONLY) && getIntObjVar(spawner, OBJVAR_SPAWNER_NIGHT_ONLY) != 0;
	}

	public static boolean isDayOnlySpawner(obj_id spawner) throws InterruptedException
	{
		return isIdValid(spawner) && hasObjVar(spawner, OBJVAR_SPAWNER_DAY_ONLY) && getIntObjVar(spawner, OBJVAR_SPAWNER_DAY_ONLY) != 0;
	}

	/**
	 * Whether a spawn attempt should proceed for this spawner&apos;s time policy.
	 * Night-only and day-only are mutually exclusive; if both objvars are set, night-only wins.
	 */
	public static boolean canSpawnForTimePolicy(obj_id spawner) throws InterruptedException
	{
		if (!isIdValid(spawner))
		{
			return true;
		}
		if (isNightOnlySpawner(spawner))
		{
			return isNight();
		}
		if (isDayOnlySpawner(spawner))
		{
			return isDay();
		}
		return true;
	}

	/** Delay before rescheduling a time-gated spawner after a blocked attempt. */
	public static float getTimePolicyRetrySeconds(obj_id spawner) throws InterruptedException
	{
		if (!isIdValid(spawner) || !hasObjVar(spawner, OBJVAR_TOD_RETRY_SECONDS))
		{
			return DEFAULT_TOD_RETRY_SECONDS;
		}
		float r = getFloatObjVar(spawner, OBJVAR_TOD_RETRY_SECONDS);
		if (r < 5f)
		{
			return 5f;
		}
		return r;
	}

	/** True if the spawner uses night-only or day-only policy. */
	public static boolean hasTimeSpawnPolicy(obj_id spawner) throws InterruptedException
	{
		return isNightOnlySpawner(spawner) || isDayOnlySpawner(spawner);
	}

	/**
	 * GM / script: set normalized environment time; see {@link #setLocalTime(float)}.
	 *
	 * @param normalized clamped to [0, 1]
	 * @return false if the native could not apply (no terrain, etc.)
	 */
	public static boolean setSceneCyclePosition(float normalized) throws InterruptedException
	{
		if (normalized < 0.f)
		{
			normalized = 0.f;
		}
		else if (normalized > 1.f)
		{
			normalized = 1.f;
		}
		return setLocalTime(normalized);
	}
}
