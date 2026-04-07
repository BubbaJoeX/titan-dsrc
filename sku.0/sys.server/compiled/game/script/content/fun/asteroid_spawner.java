package script.content.fun;/*
@Origin: dsrc.script.content.fun
@Author:  BubbaJoeX
@Purpose: Triggers a staged asteroid shower with warnings, waves, and local effects
@Requirements: <no requirements>
@Notes: Optional objvars on the spawner object:
  fun.asteroid.count (int) — fragments to spawn, default 8
  fun.asteroid.range (float) — scatter radius in meters, default 7530
  fun.asteroid.waves (int) — impact waves, default 3
  fun.asteroid.wave_gap (float) — seconds between waves, default 2.5
  fun.asteroid.warning_delay (float) — seconds before first sky effects, default 2.0
  fun.asteroid.notify_radius (float) — players who get system messages / sounds, default 384
  fun.asteroid.cooldown (int) — seconds before the event can run again, default 90; set 0 to disable
  Spawn templates: discovered at runtime via getObjectTemplateNamesWithPrefix("object/ship/asteroid") (CRC table), with fallback to object/tangible/usable/asteroid.iff if none.
@Created: Tuesday, 2/25/2025, at 7:56 PM,
@Copyright © SWG: Titan 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;

public class asteroid_spawner extends base_script
{
    public static final boolean LOGGING = false;

    private static final float DEFAULT_SPAWN_RANGE = 7530.0f;
    private static final int DEFAULT_ASTEROID_COUNT = 8;
    private static final int DEFAULT_WAVES = 3;
    private static final float DEFAULT_WAVE_GAP = 2.5f;
    private static final float DEFAULT_WARNING_DELAY = 2.0f;
    private static final float DEFAULT_NOTIFY_RADIUS = 384.0f;
    private static final int DEFAULT_COOLDOWN_SEC = 90;

    private static final String ASTEROID_TEMPLATE = "object/tangible/usable/asteroid.iff";
    private static final String COOLDOWN_OBJVAR = "fun.asteroid.last_shower";

    private static final String SND_ALERT = "sound/sys_comm_generic.snd";

    private static final String CEF_SKY_BURST = "clienteffect/lair_hvy_damage_fire.cef";
    private static final String CEF_IMPACT = "clienteffect/combat_explosion_lair_large.cef";
    private static final String CEF_DUST = "clienteffect/lair_med_damage_smoke.cef";

    private static final String[] WARNING_LINES =
            {
                    "The ground trembles. A distant roar builds in the upper atmosphere...",
                    "Long-range sensors scream—debris cluster inbound on a decaying trajectory.",
                    "A glittering cascade tears through the sky: meteoric fragments incoming!",
                    "Thermal bloom overhead. Whatever just entered the well is coming down hard.",
            };

    private static final String[] WAVE_LINES =
            {
                    "Impacts register—fragments are striking the surface nearby.",
                    "Another salvo hammers the terrain; dust and ionized grit fill the air.",
                    "The barrage continues—each strike leaves a smoldering crater of ore-rich rock.",
            };

    private static final String[] ASTEROID_NAMES =
            {
                    "Fallen Asteroid",
                    "Meteoric Fragment",
                    "Scorched Nickel Fragment",
                    "Iron Meteorite Chunk",
                    "Hypervelocity Spall",
                    "Orbital Debris Cluster",
            };

    public int OnAttach(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int sync(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isGod(player))
        {
            return SCRIPT_CONTINUE;
        }
        mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Meteor shower (spectacle)"));
        mi.addRootMenu(menu_info_types.SERVER_MENU1, string_id.unlocalized("Meteor shower (quick)"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isGod(player))
        {
            return SCRIPT_CONTINUE;
        }

        boolean quick = item == menu_info_types.SERVER_MENU1;
        if (item != menu_info_types.ITEM_USE && !quick)
        {
            return SCRIPT_CONTINUE;
        }

        int cd = getCooldownSeconds(self);
        if (cd > 0)
        {
            int last = getIntObjVar(self, COOLDOWN_OBJVAR);
            int elapsed = getGameTime() - last;
            if (elapsed < cd)
            {
                sendSystemMessage(player, "Meteor relay is still cooling down. - " + (cd - elapsed) + "s remaining.", "");
                return SCRIPT_CONTINUE;
            }
        }

        if (quick)
        {
            dictionary p = buildEventParams(self, player, true);
            messageTo(self, "asteroidSpawnWave", p, 0.35f, false);
        }
        else
        {
            dictionary p = buildEventParams(self, player, false);
            float warnDelay = getFloatConfig(self, "fun.asteroid.warning_delay", DEFAULT_WARNING_DELAY);
            messageTo(self, "asteroidPhaseWarning", p, warnDelay, false);
        }

        broadcast(player, quick ? "Releasing meteor impacts (quick sequence)..." : "Calibrating meteor shower sequence...");
        return SCRIPT_CONTINUE;
    }

    public int asteroidPhaseWarning(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
        {
            return SCRIPT_CONTINUE;
        }

        location anchor = unpackAnchor(params);
        float notifyR = params.getFloat("notify_radius");
        obj_id instigator = params.getObjId("instigator");

        obj_id[] audience = getAllPlayers(anchor, notifyR);
        String warn = WARNING_LINES[rand(0, WARNING_LINES.length - 1)];
        notifyAudience(audience, warn);

        playHorizonEffects(anchor, audience);
        playAlertSounds(audience);

        float waveGap = params.getFloat("wave_gap");
        messageTo(self, "asteroidSpawnWave", params, waveGap + 1.25f, false);

        blog("Warning phase at " + anchor.toLogFormat() + " instigator=" + instigator);
        return SCRIPT_CONTINUE;
    }

    public int asteroidSpawnWave(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
        {
            return SCRIPT_CONTINUE;
        }

        location anchor = unpackAnchor(params);
        float range = params.getFloat("range");
        int wave = params.getInt("wave");
        int waves = params.getInt("waves");
        int total = params.getInt("total");
        float notifyR = params.getFloat("notify_radius");
        float waveGap = params.getFloat("wave_gap");
        obj_id instigator = params.getObjId("instigator");
        boolean quick = params.getBoolean("quick");

        int forWave = asteroidsForWave(wave, waves, total);
        obj_id[] audience = getAllPlayers(anchor, notifyR);

        if (forWave > 0 && !quick)
        {
            notifyAudience(audience, WAVE_LINES[rand(0, WAVE_LINES.length - 1)]);
        }

        for (int i = 0; i < forWave; i++)
        {
            location impact = randomImpactLocation(anchor, range);
            obj_id asteroid = createObject(pickAsteroidTemplate(), impact);

            if (isIdValid(asteroid))
            {
                attachScript(asteroid, "content.fun.fallen_asteroid");
                setName(asteroid, ASTEROID_NAMES[rand(0, ASTEROID_NAMES.length - 1)]);
                playImpactEffects(impact, audience);
                blog("Spawned asteroid at: " + impact.toLogFormat());
            }
        }

        int nextWave = wave + 1;
        if (nextWave < waves && remainingAfterWave(nextWave, waves, total) > 0)
        {
            params.put("wave", nextWave);
            messageTo(self, "asteroidSpawnWave", params, waveGap, false);
        }
        else
        {
            int cd = getCooldownSeconds(self);
            if (cd > 0)
            {
                setObjVar(self, COOLDOWN_OBJVAR, getGameTime());
            }
            String who = isIdValid(instigator) ? getPlayerName(instigator) : "Unknown";
            notifyAudience(audience, "The meteor shower subsides.");
            blog("Meteor shower complete.");
        }

        return SCRIPT_CONTINUE;
    }

    private static dictionary buildEventParams(obj_id self, obj_id instigator, boolean quick) throws InterruptedException
    {
        location anchor = getAnchorLocation(self);
        int total = getIntConfig(self, "fun.asteroid.count", DEFAULT_ASTEROID_COUNT);
        int waves = getIntConfig(self, "fun.asteroid.waves", DEFAULT_WAVES);
        if (waves < 1)
        {
            waves = 1;
        }
        if (total < 1)
        {
            total = 1;
        }

        dictionary p = new dictionary();
        p.put("ax", anchor.x);
        p.put("ay", anchor.y);
        p.put("az", anchor.z);
        p.put("scene", anchor.area);
        p.put("range", getFloatConfig(self, "fun.asteroid.range", DEFAULT_SPAWN_RANGE));
        p.put("waves", waves);
        p.put("total", total);
        p.put("wave", 0);
        p.put("wave_gap", getFloatConfig(self, "fun.asteroid.wave_gap", DEFAULT_WAVE_GAP));
        p.put("notify_radius", getFloatConfig(self, "fun.asteroid.notify_radius", DEFAULT_NOTIFY_RADIUS));
        p.put("instigator", instigator);
        p.put("quick", quick);
        return p;
    }

    private static location getAnchorLocation(obj_id self) throws InterruptedException
    {
        location here = getLocation(self);
        if (here == null)
        {
            return new location(0, 0, 0, getCurrentSceneName());
        }
        return here;
    }

    private static location unpackAnchor(dictionary params) throws InterruptedException
    {
        String scene = params.getString("scene");
        if (scene == null || scene.isEmpty())
        {
            scene = getCurrentSceneName();
        }
        return new location(params.getFloat("ax"), params.getFloat("ay"), params.getFloat("az"), scene);
    }

    private static int asteroidsForWave(int waveIndex, int waveCount, int total)
    {
        int base = total / waveCount;
        int rem = total % waveCount;
        return base + (waveIndex < rem ? 1 : 0);
    }

    private static int remainingAfterWave(int nextWaveIndex, int waveCount, int total)
    {
        int used = 0;
        for (int w = 0; w < nextWaveIndex; w++)
        {
            used += asteroidsForWave(w, waveCount, total);
        }
        return total - used;
    }

    private static location randomImpactLocation(location anchor, float range) throws InterruptedException
    {
        float angle = frand(0.0f, (float) (2.0 * Math.PI));
        float distance = frand(0.0f, range);
        float dx = distance * (float) Math.cos(angle);
        float dz = distance * (float) Math.sin(angle);
        float x = anchor.x + dx;
        float z = anchor.z + dz;
        float y = getHeightAtLocation(x, z) + 0.25f;
        return new location(x, y, z, anchor.area);
    }

    private static void playHorizonEffects(location anchor, obj_id[] audience) throws InterruptedException
    {
        if (audience == null || audience.length == 0)
        {
            return;
        }
        float ring = Math.min(220.0f, DEFAULT_SPAWN_RANGE * 0.03f);
        for (int i = 0; i < 5; i++)
        {
            float a = (float) (2.0 * Math.PI * i / 5.0);
            float x = anchor.x + ring * (float) Math.cos(a);
            float z = anchor.z + ring * (float) Math.sin(a);
            float y = getHeightAtLocation(x, z) + 55.0f + frand(0.0f, 25.0f);
            location sky = new location(x, y, z, anchor.area);
            playClientEffectLoc(audience, CEF_SKY_BURST, sky, 0.0f);
        }
    }

    private static void playImpactEffects(location impact, obj_id[] audience) throws InterruptedException
    {
        if (audience == null || audience.length == 0)
        {
            return;
        }
        playClientEffectLoc(audience, CEF_IMPACT, impact, 0.0f);
        playClientEffectLoc(audience, CEF_DUST, impact, 0.0f);
    }

    private static void playAlertSounds(obj_id[] audience) throws InterruptedException
    {
        if (audience == null)
        {
            return;
        }
        for (int i = 0; i < audience.length; i++)
        {
            if (isIdValid(audience[i]) && exists(audience[i]))
            {
                play2dNonLoopingSound(audience[i], SND_ALERT);
            }
        }
    }

    private static void notifyAudience(obj_id[] audience, String msg) throws InterruptedException
    {
        if (audience == null || msg == null)
        {
            return;
        }
        for (int i = 0; i < audience.length; i++)
        {
            if (isIdValid(audience[i]) && exists(audience[i]))
            {
                sendSystemMessage(audience[i], msg, "");
            }
        }
    }

    private static int getIntConfig(obj_id self, String key, int defaultVal) throws InterruptedException
    {
        if (hasObjVar(self, key))
        {
            return getIntObjVar(self, key);
        }
        return defaultVal;
    }

    private static float getFloatConfig(obj_id self, String key, float defaultVal) throws InterruptedException
    {
        if (hasObjVar(self, key))
        {
            return getFloatObjVar(self, key);
        }
        return defaultVal;
    }

    private static int getCooldownSeconds(obj_id self) throws InterruptedException
    {
        return getIntConfig(self, "fun.asteroid.cooldown", DEFAULT_COOLDOWN_SEC);
    }

    private static float frand(float min, float max)
    {
        return min + (float) Math.random() * (max - min);
    }

    /**
     * Templates whose server pathname starts with {@link #ASTEROID_SHIP_TEMPLATE_PREFIX}, cached after first use.
     */
    private static String[] getShipAsteroidTemplates()
    {
        String[] c = s_cachedShipAsteroidTemplates;
        if (c != null)
        {
            return c;
        }
        synchronized (asteroid_spawner.class)
        {
            if (s_cachedShipAsteroidTemplates == null)
            {
                String[] found = getObjectTemplateNamesWithPrefix(ASTEROID_SHIP_TEMPLATE_PREFIX);
                if (found == null || found.length == 0)
                {
                    found = new String[]{FALLBACK_ASTEROID_TEMPLATE};
                }
                s_cachedShipAsteroidTemplates = found;
            }
            return s_cachedShipAsteroidTemplates;
        }
    }

    private static String pickAsteroidTemplate()
    {
        String[] t = getShipAsteroidTemplates();
        if (t == null || t.length == 0)
        {
            return FALLBACK_ASTEROID_TEMPLATE;
        }
        return t[rand(0, t.length - 1)];
    }

    public void blog(String msg)
    {
        if (LOGGING)
        {
            LOG("ethereal", "[asteroid_spawner]: " + msg);
        }
    }
}
