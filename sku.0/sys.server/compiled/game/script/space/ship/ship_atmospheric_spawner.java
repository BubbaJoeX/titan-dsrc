package script.space.ship;

import script.*;
import script.library.*;

import java.util.Vector;

public class ship_atmospheric_spawner extends script.base_script
{
    public ship_atmospheric_spawner()
    {
    }

    public static final float DEFAULT_CRUISE_ALTITUDE = 200.0f;
    public static final float MIN_SPAWN_ALTITUDE = 100.0f;
    public static final float MAX_SPAWN_ALTITUDE = 500.0f;
    public static final float DEFAULT_PATROL_RADIUS = 2000.0f;
    public static final float DEFAULT_LOITER_MIN = 500.0f;
    public static final float DEFAULT_LOITER_MAX = 2000.0f;
    public static final int DEFAULT_PATROL_POINTS = 12;

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        if (utils.checkConfigFlag("ScriptFlags", "spawnersOn"))
        {
            messageTo(self, "atmosphericStartSpawning", null, 5.0f, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        if (utils.checkConfigFlag("ScriptFlags", "spawnersOn"))
        {
            messageTo(self, "atmosphericStartSpawning", null, 5.0f, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int atmosphericStartSpawning(obj_id self, dictionary params) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        String[] strSpawns = null;
        if (hasObjVar(self, "atmo.spawns"))
        {
            strSpawns = getStringArrayObjVar(self, "atmo.spawns");
        }
        if (strSpawns == null || strSpawns.length == 0)
            return SCRIPT_CONTINUE;

        int spawnCount = 1;
        if (hasObjVar(self, "atmo.spawnCount"))
            spawnCount = getIntObjVar(self, "atmo.spawnCount");

        for (int i = 0; i < spawnCount; i++)
        {
            String shipType = strSpawns[rand(0, strSpawns.length - 1)];
            dictionary spawnParams = new dictionary();
            spawnParams.put("shipType", shipType);
            float delay = rand(1.0f, 5.0f) * (i + 1);
            messageTo(self, "atmosphericDoSpawn", spawnParams, delay, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int atmosphericDoSpawn(obj_id self, dictionary params) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        String shipType = params.getString("shipType");
        if (shipType == null || shipType.isEmpty())
            return SCRIPT_CONTINUE;

        location myLoc = getLocation(self);
        float spawnAlt = DEFAULT_CRUISE_ALTITUDE;
        if (hasObjVar(self, "atmo.cruiseAltitude"))
            spawnAlt = getFloatObjVar(self, "atmo.cruiseAltitude");

        float spawnRadius = 500.0f;
        if (hasObjVar(self, "atmo.spawnRadius"))
            spawnRadius = getFloatObjVar(self, "atmo.spawnRadius");

        float offsetX = rand(-spawnRadius, spawnRadius);
        float offsetZ = rand(-spawnRadius, spawnRadius);
        float spawnX = myLoc.x + offsetX;
        float spawnZ = myLoc.z + offsetZ;

        float terrainY = getHeightAtLocation(spawnX, spawnZ);
        float spawnY = terrainY + spawnAlt;

        transform spawnTransform = new transform();
        spawnTransform = spawnTransform.setPosition_p(spawnX, spawnY, spawnZ);

        obj_id ship = space_create.createShip(shipType, spawnTransform);
        if (!isIdValid(ship))
            return SCRIPT_CONTINUE;

        setObjVar(ship, "objParent", self);
        setObjVar(ship, "atmo.spawned", true);
        setObjVar(ship, "atmo.cruiseAltitude", spawnAlt);

        String behavior = "loiter";
        if (hasObjVar(self, "atmo.behavior"))
            behavior = getStringObjVar(self, "atmo.behavior");

        setupAtmosphericBehavior(ship, self, behavior, spawnAlt);

        return SCRIPT_CONTINUE;
    }

    public void setupAtmosphericBehavior(obj_id ship, obj_id spawner, String behavior, float cruiseAlt) throws InterruptedException
    {
        location spawnerLoc = getLocation(spawner);

        switch (behavior)
        {
            case "loiter":
            {
                float minDist = DEFAULT_LOITER_MIN;
                float maxDist = DEFAULT_LOITER_MAX;
                if (hasObjVar(spawner, "atmo.loiterMin"))
                    minDist = getFloatObjVar(spawner, "atmo.loiterMin");
                if (hasObjVar(spawner, "atmo.loiterMax"))
                    maxDist = getFloatObjVar(spawner, "atmo.loiterMax");

                transform[] path = createAtmosphericPatrolLoiter(spawnerLoc.x, spawnerLoc.z, minDist, maxDist, cruiseAlt, DEFAULT_PATROL_POINTS);
                ship_ai.unitPatrol(ship, path);
                break;
            }
            case "patrol":
            {
                float radius = DEFAULT_PATROL_RADIUS;
                if (hasObjVar(spawner, "atmo.patrolRadius"))
                    radius = getFloatObjVar(spawner, "atmo.patrolRadius");

                transform[] path = createAtmosphericPatrolCircle(spawnerLoc.x, spawnerLoc.z, radius, cruiseAlt, DEFAULT_PATROL_POINTS);
                ship_ai.unitPatrol(ship, path);
                break;
            }
            case "idle":
            {
                ship_ai.unitIdle(ship);
                break;
            }
            default:
            {
                transform[] path = createAtmosphericPatrolLoiter(spawnerLoc.x, spawnerLoc.z, DEFAULT_LOITER_MIN, DEFAULT_LOITER_MAX, cruiseAlt, DEFAULT_PATROL_POINTS);
                ship_ai.unitPatrol(ship, path);
                break;
            }
        }
    }

    /**
     * Creates a circular patrol path at a fixed altitude above terrain.
     * Each waypoint's Y is computed from the terrain height at that XZ + cruiseAlt.
     */
    public static transform[] createAtmosphericPatrolCircle(float centerX, float centerZ, float radius, float cruiseAlt, int numPoints) throws InterruptedException
    {
        transform[] path = new transform[numPoints];
        for (int i = 0; i < numPoints; i++)
        {
            float radian = (float)Math.PI * 2.0f * ((float)i / numPoints);
            float x = centerX + (float)StrictMath.sin(radian) * radius;
            float z = centerZ + (float)StrictMath.cos(radian) * radius;
            float terrainY = getHeightAtLocation(x, z);
            float y = terrainY + cruiseAlt;
            path[i] = transform.identity.setPosition_p(x, y, z);
        }
        return path;
    }

    /**
     * Creates a randomized loiter patrol path with terrain-aware altitudes.
     * Points are scattered around the center within [minDist, maxDist], each at cruiseAlt above terrain.
     */
    public static transform[] createAtmosphericPatrolLoiter(float centerX, float centerZ, float minDist, float maxDist, float cruiseAlt, int numPoints) throws InterruptedException
    {
        transform[] path = new transform[numPoints];
        for (int i = 0; i < numPoints; i++)
        {
            float angle = (float)(Math.random() * Math.PI * 2.0);
            float dist = minDist + (float)(Math.random() * (maxDist - minDist));
            float x = centerX + (float)StrictMath.cos(angle) * dist;
            float z = centerZ + (float)StrictMath.sin(angle) * dist;
            float terrainY = getHeightAtLocation(x, z);
            float y = terrainY + cruiseAlt;
            path[i] = transform.identity.setPosition_p(x, y, z);
        }
        return path;
    }

    public int childDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        if (!space_transition.isAtmosphericFlightScene())
            return SCRIPT_CONTINUE;

        float minRespawn = 60.0f;
        float maxRespawn = 180.0f;
        if (hasObjVar(self, "atmo.minRespawnTime"))
            minRespawn = getFloatObjVar(self, "atmo.minRespawnTime");
        if (hasObjVar(self, "atmo.maxRespawnTime"))
            maxRespawn = getFloatObjVar(self, "atmo.maxRespawnTime");

        String[] strSpawns = null;
        if (hasObjVar(self, "atmo.spawns"))
            strSpawns = getStringArrayObjVar(self, "atmo.spawns");

        if (strSpawns == null || strSpawns.length == 0)
            return SCRIPT_CONTINUE;

        dictionary spawnParams = new dictionary();
        spawnParams.put("shipType", strSpawns[rand(0, strSpawns.length - 1)]);
        messageTo(self, "atmosphericDoSpawn", spawnParams, rand(minRespawn, maxRespawn), false);

        return SCRIPT_CONTINUE;
    }
}
