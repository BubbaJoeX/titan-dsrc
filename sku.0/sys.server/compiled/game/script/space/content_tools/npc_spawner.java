package script.space.content_tools;

import script.dictionary;
import script.library.create;
import script.library.planetary_map;
import script.library.utils;
import script.location;
import script.obj_id;
import script.string_id;

public class npc_spawner extends script.base_script
{
    public npc_spawner()
    {
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        LOG("space", "ONINTIAILIZE");

        // Try to get spawn ID from objvar first (new method)
        String strSpawnId = null;
        if (hasObjVar(self, "spawn.id"))
        {
            strSpawnId = getStringObjVar(self, "spawn.id");
        }
        else
        {
            // Fall back to object ID (legacy method)
            strSpawnId = "" + self;
        }

        dictionary dctSpawnInfo = dataTableGetRow("datatables/space_content/npc_spawners.iff", strSpawnId);
        if (dctSpawnInfo == null)
        {
            LOG("space", "NO ENTRY FOR " + strSpawnId + " (objvar or ID)");
            obj_id objTest = createObject("object/tangible/gravestone/gravestone01.iff", getLocation(self));
            setName(objTest, "BAD NPC SPAWNER IS HERE!, NO ENTRY IN NPC SPAWNERS DATATABLE FOR: " + strSpawnId);
            return SCRIPT_CONTINUE;
        }

        spawnNpcFromData(self, dctSpawnInfo);
        return SCRIPT_CONTINUE;
    }

    /**
     * Spawns an NPC using data from the npc_spawners datatable
     * @param self The spawner object
     * @param dctSpawnInfo Dictionary containing spawn data
     */
    private void spawnNpcFromData(obj_id self, dictionary dctSpawnInfo) throws InterruptedException
    {
        String strTemplate = dctSpawnInfo.getString("strTemplate");
        if (strTemplate.equals("unused"))
        {
            return;
        }

        location locTest = getLocation(self);
        obj_id objNPC = null;

        try
        {
            objNPC = createObject(strTemplate, getTransform_o2p(self), locTest.cell);
        }
        catch(Throwable err)
        {
            LOG("space", "BAD TEMPLATE FOR " + self);
            locTest.x = locTest.x + 1;
            obj_id objTest = createObject("object/tangible/gravestone/gravestone01.iff", locTest);
            setName(objTest, "BAD NPC SPAWNER IS HERE!, BAD TEMPLATE IN NPC SPAWNERS DATATABLE FOR ENTRY " + self + " TEMPLATE IS " + strTemplate);
            return;
        }

        attachScriptsToNpc(objNPC, dctSpawnInfo, self);
        setNpcName(objNPC, dctSpawnInfo);
        configureNpcProperties(objNPC);
        addMapLocation(self, dctSpawnInfo, locTest);
    }

    /**
     * Attaches scripts from the datatable to the spawned NPC
     * @param objNPC The spawned NPC
     * @param dctSpawnInfo Dictionary containing spawn data
     * @param self The spawner object
     */
    private void attachScriptsToNpc(obj_id objNPC, dictionary dctSpawnInfo, obj_id self) throws InterruptedException
    {
        for (int intI = 1; intI < 5; intI++)
        {
            String strScript = dctSpawnInfo.getString("strScript" + intI);
            if (!strScript.equals(""))
            {
                if (!hasScript(objNPC, strScript))
                {
                    int intReturn = attachScript(objNPC, strScript);
                    if (intReturn != SCRIPT_CONTINUE)
                    {
                        if (!hasScript(objNPC, strScript))
                        {
                            setName(objNPC, "BAD SCRIPT IN TABLE FOR " + self + " SCRIPT IS " + strScript);
                        }
                        return;
                    }
                }
            }
        }
    }

    /**
     * Sets the NPC's name from the datatable
     * @param objNPC The spawned NPC
     * @param dctSpawnInfo Dictionary containing spawn data
     */
    private void setNpcName(obj_id objNPC, dictionary dctSpawnInfo) throws InterruptedException
    {
        String sidName = dctSpawnInfo.getString("sidName");
        if (!sidName.equals(""))
        {
            setName(objNPC, "");
            setName(objNPC, new string_id("npc_spawner_n", sidName));
        }
    }

    /**
     * Configures basic NPC properties (invulnerability, conditions)
     * @param objNPC The spawned NPC
     */
    private void configureNpcProperties(obj_id objNPC) throws InterruptedException
    {
        setInvulnerable(objNPC, true);
        if (isMob(objNPC))
        {
            setCondition(objNPC, CONDITION_SPACE_INTERESTING);
            setCondition(objNPC, CONDITION_CONVERSABLE);
        }
    }

    /**
     * Adds planetary map location if configured in the datatable
     * @param self The spawner object
     * @param dctSpawnInfo Dictionary containing spawn data
     * @param locTest Location of the spawner
     */
    private void addMapLocation(obj_id self, dictionary dctSpawnInfo, location locTest) throws InterruptedException
    {
        String strPrimaryCategory = dctSpawnInfo.getString("strPrimaryCategory");
        if (!strPrimaryCategory.equals(""))
        {
            String strSecondaryCategory = dctSpawnInfo.getString("strSecondaryCategory");
            String sidName = dctSpawnInfo.getString("sidName");
            LOG("space", "strLocationName [npc_spawner_n]:" + sidName);
            string_id strMapNameId = new string_id("npc_spawner_n", sidName);
            obj_id objContainer = getTopMostContainer(self);
            if (isIdValid(objContainer))
            {
                locTest = getLocation(objContainer);
            }
            addPlanetaryMapLocation(self, utils.packStringId(strMapNameId), (int)locTest.x, (int)locTest.z, strPrimaryCategory, strSecondaryCategory, MLT_STATIC, planetary_map.NO_FLAG);
        }
    }
}
