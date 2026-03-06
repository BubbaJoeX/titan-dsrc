package script.developer.bubbajoe;/*
@Origin: dsrc.script.developer.bubbajoe.player_developer
@Author: BubbaJoeX
@Purpose: Developer script for SWG-OR
@Requirements
    This script contains many unhandled and unchecked operations. Use at your own risk. This script also contains code cherry-picked from SWG-Source/dsrc:3.1. Update accordingly.
@Notes:
    Please target yourself before running any command unless it returns a message to not to.

@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/


import script.*;
import script.ai.ai;
import script.library.*;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.*;

import static java.nio.file.Files.walk;
import static script.library.static_item.MASTER_ITEM_TABLE;
import static script.library.sui.*;
import static script.library.utils.*;


@SuppressWarnings("ALL")
public class player_developer extends base_script
{
    public static final String API = "localhost:5000";
    public static final String PFP = "localhost:5000/pfp.png";
    public static final int STIPEND = 150000;
    public static float PLANETWIDE = 16000.0f;
    private final int pageSize = 10; // Number of items per page
    // Variables for pagination
    private int currentPage = 0; // Current page
    private int totalPages = 0; // Total number of pages
    private String[][] paginatedData = new String[0][0]; // Placeholder for paginated data
    private String searchFilter = ""; // Filter for searching by name

    public player_developer()
    {
    }

    public static String[] listObjectFilesByTerm(String searchTerm)
    {
        List<String> fileList = new ArrayList<>();
        try
        {
            walk(Paths.get("/home/swg/swg-main/data/sku.0/sys.server/compiled/game/object/"))
                    .filter(Files::isRegularFile)
                    .forEach(path ->
                    {
                        String filePath = path.toString();
                        //LOG("ethereal", "[Template Lookup]: Found file: " + filePath);
                        if (filePath.contains("/home/swg/swg-main/data/sku.0/sys.server/compiled/game/") && filePath.contains(searchTerm) && filePath.endsWith(".iff"))
                        {
                            filePath = filePath.replace("/home/swg/swg-main/data/sku.0/sys.server/compiled/game/", "");
                            //LOG("ethereal", "[Template Lookup]: Adding file: " + filePath);
                            fileList.add(filePath);
                        }
                    });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        Collections.sort(fileList);
        LOG("ethereal", "[Template Lookup]: Sorted. Returning " + fileList.size() + " files");
        return fileList.toArray(new String[0]);
    }

    public static String[] listTangibleObjectFilesByTerm(String searchTerm)
    {
        List<String> fileList = new ArrayList<>();
        try
        {
            walk(Paths.get("/home/swg/swg-main/data/sku.0/sys.server/compiled/game/object/tangible"))
                    .filter(Files::isRegularFile)
                    .forEach(path ->
                    {
                        String filePath = path.toString();
                        //LOG("ethereal", "[Template Lookup]: Found file: " + filePath);
                        if (filePath.contains("/home/swg/swg-main/data/sku.0/sys.server/compiled/game/") && filePath.contains(searchTerm) && filePath.endsWith(".iff"))
                        {
                            filePath = filePath.replace("/home/swg/swg-main/data/sku.0/sys.server/compiled/game/", "");
                            //LOG("ethereal", "[Template Lookup]: Adding file: " + filePath);
                            fileList.add(filePath);
                        }
                    });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        Collections.sort(fileList);
        LOG("ethereal", "[Template Lookup]: Sorted. Returning " + fileList.size() + " files");
        return fileList.toArray(new String[0]);
    }

    private static String[] getCreatureColumns()
    {
        String columnsNames = "creatureName,BaseLevel,Damagelevelmodifier,StatLevelModifier,ToHitLevelModifier,ArmorLevelModifier,difficultyClass,stealingFlags,notes,where,socialGroup,pvpFaction,isSpecialForces,template,minScale,maxScale,hue,minDrainModifier,maxDrainModifier,minFaucetModifier,maxFaucetModifier,armorKinetic,armorEnergy,armorBlast,armorHeat,armorCold,armorElectric,armorAcid,armorStun,attackSpeed,hasResources,milkType,meat,meatType,hide,hideType,bone,boneType,rare,rareType,geneProfile,minCash,maxCash,intLootRolls,intRollPercent,lootTable,lootList,collectionRoll,collectionLoot,col_faction,chronicleLootChance,chronicleLootCategory,canTame,wildAbilityList,niche,invulnerable,rootImmune,snareImmune,stunImmune,mezImmune,canNotPunish,tauntImmune,ignorePlayer,skillmods,objvars,scripts,clothesList,utterance,diction,canOfferMission,movement_speed,primary_weapon,primary_weapon_speed,secondary_weapon,secondary_weapon_speed,primary_weapon_specials,secondary_weapon_specials,aggressive,assist,stalker,herd,death_blow";
        return columnsNames.split(",");
    }

    public static transform fromMatrix(float[][] matrix)
    {
        // Implement the actual matrix to quaternion conversion here
        // Placeholder logic; replace with correct conversion
        float rotX = matrix[0][0];
        float rotY = matrix[1][0];
        float rotZ = matrix[2][0];
        float rotW = 1.0f; // Default or calculated value

        return new transform(rotX, rotY, rotZ, rotW);
    }

    public static String applyGradient(String input, Color startColor, Color endColor)
    {
        int length = input.length();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++)
        {
            int r = (int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * ((float) i / (length - 1)));
            int g = (int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * ((float) i / (length - 1)));
            int b = (int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * ((float) i / (length - 1)));
            String hexColor = String.format("\\#%02X%02X%02X", r, g, b);
            result.append(hexColor).append(input.charAt(i));
        }

        return result.toString();
    }

    public static void placeObjectsFromText(obj_id self, String inputText, String objectTemplate)
    {
        if (self == null || inputText == null || inputText.isEmpty() || objectTemplate == null)
        {
            broadcast(self, "Invalid leader, input text, or object template.");
            return;
        }

        location leaderLoc = getLocation(self);
        if (leaderLoc == null)
        {
            broadcast(self, "Failed to get leader location.");
            return;
        }

        final float scale = getObjectCollisionRadius(self) * 3.0f; // Spacing for letters
        inputText = inputText.toUpperCase(); // Normalize to uppercase
        char[] chars = inputText.toCharArray();

        int xOffset = 0;
        int yOffset = 0;
        int lineHeight = 6; // Moves to the next row after a certain length

        for (char c : chars)
        {
            if (c == ' ')
            {
                xOffset += 3; // Extra spacing between words
                continue;
            }

            float[][] letterShape = getLetterFormation(c);
            if (letterShape == null)
            {
                broadcast(self, "No formation found for letter: " + c);
                continue;
            }

            for (float[] pos : letterShape)
            {
                location offset = new location();
                offset.x = leaderLoc.x + (xOffset + pos[0]) * scale;
                offset.z = leaderLoc.z + (yOffset + pos[1]) * scale;
                offset.y = leaderLoc.y; // Maintain the leader's height

                obj_id placedObject = createObject(objectTemplate, offset);
                if (placedObject == null)
                {
                    broadcast(self, "Failed to create object for letter: " + c);
                }
                else
                {
                    broadcast(self, "Placed letter: " + c);
                }
            }

            // Move xOffset forward for the next letter
            xOffset += 6;

            // Wrap to new line if too long
            if (xOffset > 40)
            {
                xOffset = 0;
                yOffset -= lineHeight;
            }
        }
    }

    private static float[][] getLetterFormation(char letter)
    {
        switch (letter)
        {
            case 'A':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {0, 1}, {2, 1}, {0, 0}, {2, 0}, {1, -1}};
            case 'B':
                return new float[][]{{0, 2}, {1, 2}, {2, 1}, {1, 0}, {2, -1}, {0, -2}, {1, -2}};
            case 'C':
                return new float[][]{{1, 2}, {0, 1}, {0, 0}, {0, -1}, {1, -2}};
            case 'D':
                return new float[][]{{0, 2}, {1, 2}, {2, 1}, {2, 0}, {1, -1}, {0, -2}};
            case 'E':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {0, 1}, {0, 0}, {1, 0}, {2, 0}, {0, -1}, {0, -2}, {1, -2}, {2, -2}};
            case 'F':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {0, 1}, {0, 0}, {1, 0}, {2, 0}, {0, -1}, {0, -2}};
            case 'G':
                return new float[][]{{1, 2}, {0, 1}, {0, 0}, {0, -1}, {1, -2}, {2, -2}, {2, -1}, {2, 0}, {1, 0}};
            case 'H':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {0, -1}, {0, -2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, 0}};
            case 'I':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {1, 1}, {1, 0}, {1, -1}, {0, -2}, {1, -2}, {2, -2}};
            case 'J':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {1, -1}, {0, -2}};
            case 'K':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {0, -1}, {0, -2}, {1, 0}, {2, 2}, {2, -2}};
            case 'L':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {0, -1}, {0, -2}, {1, -2}, {2, -2}};
            case 'M':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {0, -1}, {0, -2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, 1}};
            case 'N':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {0, -1}, {0, -2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}, {2, -2}, {1, 1}};
            case 'O':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {0, 1}, {2, 1}, {0, 0}, {2, 0}, {0, -1}, {1, -2}, {2, -1}};
            case 'P':
                return new float[][]{{0, 2}, {1, 2}, {2, 1}, {1, 0}, {0, -1}, {0, -2}};
            case 'Q':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {0, 1}, {2, 1}, {0, 0}, {2, 0}, {0, -1}, {1, -2}, {2, -1}, {3, -2}};
            case 'R':
                return new float[][]{{0, 2}, {1, 2}, {2, 1}, {1, 0}, {0, -1}, {0, -2}, {1, -1}, {2, -2}};
            case 'S':
                return new float[][]{{1, 2}, {0, 1}, {1, 0}, {2, -1}, {1, -2}};
            case 'T':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {1, 1}, {1, 0}, {1, -1}, {1, -2}};
            case 'U':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {0, -1}, {1, -2}, {2, 2}, {2, 1}, {2, 0}, {2, -1}};
            case 'V':
                return new float[][]{{0, 2}, {0, 1}, {1, 0}, {2, 1}, {2, 2}};
            case 'W':
                return new float[][]{{0, 2}, {0, 1}, {0, 0}, {1, -1}, {2, 0}, {2, 1}, {2, 2}};
            case 'X':
                return new float[][]{{0, 2}, {2, 2}, {1, 1}, {0, 0}, {2, 0}, {0, -1}, {2, -1}, {1, -2}};
            case 'Y':
                return new float[][]{{0, 2}, {2, 2}, {1, 1}, {1, 0}, {1, -1}, {1, -2}};
            case 'Z':
                return new float[][]{{0, 2}, {1, 2}, {2, 2}, {2, 1}, {1, 0}, {0, -1}, {0, -2}, {1, -2}, {2, -2}};
            default:
                return null;
        }
    }

    /**
     * cmdDeveloper
     *
     * @param self
     *         script attached to
     * @param target
     *         target designation
     * @param params
     *         text after command
     * @param defaultTime
     *         n/a
     * @return SCRIPT_CONTINUE
     * @throws InterruptedException
     *         Required
     * @throws IOException
     *         Required
     * @throws NullPointerException
     *         Required
     */
    public int cmdDeveloper(obj_id self, obj_id target, String params, float defaultTime) throws Exception
    {
        if (params.isEmpty())
        {
            broadcast(self, "Usage: /developer [command] [params]");
            broadcast(self, "Be sure to target yourself when executing any command, unless the script says otherwise.");
            return SCRIPT_CONTINUE;
        }
        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer " + params);
        obj_id iTarget = getTarget(self);
        if (iTarget == null)
        {
            iTarget = self;
        }
        StringTokenizer tok = new StringTokenizer(params);
        String cmd = tok.nextToken();

        if (cmd.equalsIgnoreCase("database"))
        {
            //openQueryWindow(self, self);
            return SCRIPT_CONTINUE;
        }

        if (cmd.equalsIgnoreCase("spellObject"))
        {
            String template = tok.nextToken();
            StringBuilder speech = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                speech.append(" ").append(tok.nextToken());
            }
            broadcast(self, "Trying to spell out " + speech.toString() + " with " + template);
            placeObjectsFromText(self, speech.toString(), template);
            return SCRIPT_CONTINUE;
        }

        else if (cmd.equalsIgnoreCase("stats"))
        {
            //handlePlayerStats(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("reloadAllScripts"))
        {
            reloadAllScripts(self);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer reloadAllScripts");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("reloadTable"))
        {
            String table = tok.nextToken();
            reloadDatatable(self, table);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer reloadTable " + table);
        }
        else if (cmd.equalsIgnoreCase("scriptLogs"))
        {
            script.library.script_logs.show(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("bounty"))
        {
            String flag = tok.nextToken();
            if (!flag.equals("set") || !flag.equals("clear"))
            {
                broadcast(self, "Usage: /developer bounty [set | clear] [name]");
                return SCRIPT_CONTINUE;
            }
            if (flag.equalsIgnoreCase("clear"))
            {
                obj_id player = null;
                String name = tok.nextToken();
                obj_id victim = getPlayerIdFromFirstName(name);
                bounty_hunter.removeJediBounty(victim, self);
            }
            else if (flag.equalsIgnoreCase("set"))
            {
                obj_id player = null;
                String name = tok.nextToken();
                obj_id victim = getPlayerIdFromFirstName(name);
                broadcast(self, "Attempting to set bounty on " + name);
                bounty_hunter.showSetBountySUI(self, victim);
            }
            else
            {
                broadcast(self, "Usage: /developer bounty [set | clear] [name]");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("addCharacterSlot"))
        {
            //1 for normal
            //2 for jedi
            //3 for spectal (im assuming force ghost)
            //we dont use anything but 1 for NGE.
            //increaseCharacterAmount(self, getTarget(self), getPlayerStationId(getTarget(self)), 1, 1);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("tcgvoucher"))
        {
            obj_id pInv = getInventoryContainer(getIntendedTarget(self));
            obj_id voucherObj = createObject("object/tangible/loot/npc_loot/datapad_flashy_generic.iff", pInv, "");
            setName(voucherObj, "Voucher: Trading Card Game");
            setDescriptionString(voucherObj, "This voucher is good for one free Trading Card Game loot item of your choice. It cannot be traded for credits or other items. \n\\#00FF00***Turn in to the Voucher Redemption Terminal to select your reward.***\\#.");
            setObjVar(voucherObj, "tcgVoucher", 1);
            setObjVar(voucherObj, "noTrade", 1);
            attachScript(voucherObj, "item.special.nomove");
            setCount(voucherObj, 1);
        }
        else if (cmd.equalsIgnoreCase("forceUVTick"))
        {
            obj_id planet = getObjIdObjVar(getPlanetByName("tatooine"), "planet_uv_controller." + getCurrentSceneName());
            broadcast(self, "Forcing a UV System tick on " + getCurrentSceneName() + "...");
            messageTo(planet, "handleStartUVDelay", null, 1f, false);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("entbuff"))
        {
            String[] buffComponentKeys =
                    {
                            "kinetic",
                            "energy",
                            "action_cost_reduction",
                            "dodge",
                            "strength",
                            "constitution",
                            "stamina",
                            "precision",
                            "agility",
                            "luck",
                            "critical_hit",
                            "healing_potency",
                            "healer",
                            "reactive_go_with_the_flow",
                            "flush_with_success",
                            "reactive_second_chance",
                            "milk_quantity",
                            "milk_exceptional_chance",
                            "milk_stun",
                            "creature_harvesting",
                            "harvest_faire",
                            "reverse_engineering_chance",
                            "crafting",
                            "crafting_success",
                            "hand_sampling",
                            "resource_quality"

                    };
            int[] buffComponentValues =
                    {
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15,
                            15
                    };
            //float currentBuffTime = performance.inspireGetMaxDuration(self);
            setScriptVar(self, "performance.buildabuff.buffComponentKeys", buffComponentKeys);
            setScriptVar(self, "performance.buildabuff.buffComponentValues", buffComponentValues);
            for (int i = 0; i < buffComponentKeys.length; i++)
            {
                broadcast(self, "Applying " + buffComponentKeys[i] + " buff of " + buffComponentValues[i] + " for 7200 seconds.");
            }
            buff.applyBuff(self, "buildabuff_inspiration", 7200);
            armor.recalculateArmorForPlayer(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getItemList"))
        {
            String searchParam = tok.nextToken();
            if (tok.countTokens() > 1)
            {
                broadcast(self, "Usage: /developer getItemList [searchParam]");
                return SCRIPT_CONTINUE;
            }
            else
            {
                StringBuilder prompt = new StringBuilder("TCG Loot List:\n");
                String[] tcgList = dataTableGetStringColumn("datatables/item/master_item/master_item.iff", "name");
                int howMany = 0;
                for (String s : tcgList)
                {
                    if (s.contains(searchParam))
                    {
                        prompt.append(s).append("\n");
                        howMany++;
                    }
                }
                broadcast(self, "Found " + howMany + " items matching " + searchParam + ".");
                int itemPid = sui.createSUIPage(SUI_MSGBOX, self, self, "noHandler");
                setSUIProperty(itemPid, "Prompt.lblPrompt", "GetsInput", "true");
                setSUIProperty(itemPid, "Prompt.lblPrompt", "Text", prompt.toString());
                setSUIProperty(itemPid, "Prompt.lblPrompt", "TextColor", "#DD1234");
                setSUIProperty(itemPid, "Prompt.lblPrompt", "Editable", "true");
                setSUIProperty(itemPid, "Prompt.lblPrompt", "Font", "bold_22");
                showSUIPage(itemPid);
                flushSUIPage(itemPid);
                broadcast(self, "Finding TCG loot..");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("generateComponent"))
        {
            float ITEM_LEVEL = 100f;
            int COMPONENT_LEVEL = 100;
            String item = tok.nextToken();
            String source_objid = tok.nextToken();
            obj_id source = obj_id.getObjId(Long.parseLong(source_objid));
            String schematic = "object/draft_schematic/" + item + ".iff";
            obj_id prototype = craftinglib.makeCraftedItem(item, ITEM_LEVEL, getInventoryContainer(self));
            if (isIdValid(prototype))
            {
                loot.randomizeComponent(target, COMPONENT_LEVEL, target);
                setCraftedId(prototype, source);
                setCrafter(prototype, self);
                setObjVar(prototype, "csr.loot.creator", self);
                sendSystemMessageTestingOnly(self, "Generated " + item + " with prototype " + prototype);
            }
            else
            {
                sendSystemMessageTestingOnly(self, "Failed to generate " + item + " with prototype " + prototype);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("heal"))
        {
            setHealth(self, getMaxHealth(self));
            broadcast(self, "Healed to full health.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("setHair"))
        {
            obj_id hair = getObjectInSlot(self, "hair");
            if (hair == null)
            {
                broadcast(self, "You must have hair equipped to use this command.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String hairTemplate = getTemplateName(target);
                obj_id hairObj = createObject(hairTemplate, getInventoryContainer(self), "hair");
                if (isIdValid(hairObj))
                {
                    destroyObject(hair);
                    sendSystemMessageTestingOnly(self, "Hair changed to " + hairTemplate);
                }
                else
                {
                    sendSystemMessageTestingOnly(self, "Failed to change hair to " + hairTemplate);
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("exotics"))
        {
            String skillmod = tok.nextToken();
            int value = stringToInt(tok.nextToken());
            if (tok.countTokens() > 2)
            {
                broadcast(self, "Usage: /developer exotics [skillmod] [value]");
                return SCRIPT_CONTINUE;
            }
            if (value < 1 || value > 350)
            {
                broadcast(self, "Value must be between 1 and 350.");
                return SCRIPT_CONTINUE;
            }
            if (skillmod.contains("expertise_"))
            {
                if (value > 35)
                {
                    broadcast(self, "Expertise value must be less than 35.");
                    value = 35;
                }
            }
            applySkillStatisticModifier(target, skillmod, value);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getCellIds"))
        {
            obj_id cell = stringToObjId(tok.nextToken());
            if (cell == null)
            {
                broadcast(self, "Usage: /developer getCellIds [cellId]");
                return SCRIPT_CONTINUE;
            }
            obj_id[] cellIds = getCellIds(cell);
            for (obj_id cellId : cellIds)
            {
                broadcast(self, "Cell ID: " + cellId);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("putInCell"))
        {
            location corvette = new location();
            corvette.area = getCurrentSceneName();
            corvette.x = 0.0f;
            corvette.y = 0.0f;
            corvette.z = 0.0f;
            corvette.cell = stringToObjId(tok.nextToken());
            if (corvette.cell == null)
            {
                broadcast(self, "Invalid cell.");
                return SCRIPT_CONTINUE;
            }
            warpPlayer(self, corvette.area, corvette.x, corvette.y, corvette.z, corvette.cell, 0.0f, 0.0f, 0.0f);
            return SCRIPT_CONTINUE;
        }
        /*else if (cmd.equalsIgnoreCase("goTo"))
        {
            final obj_id world = getTopMostContainer(self);
            if (!tok.hasMoreTokens())
            {
                sendSystemMessageTestingOnly(self, "ERROR: Specify a creature type, like \"dugeonJumpMob nightsister_elder\"");
            }
            final String mob = tok.nextToken();
            final String[] values = Arrays.stream(getPackedObjvars(world, "spawned").split("\\|"))
                    .filter(v -> v.matches("^[0-9]*$"))
                    .filter(v -> Long.parseLong(v) > 100000).toArray(String[]::new);
            for (String value : values)
            {
                final obj_id id = obj_id.getObjId(Long.parseLong(value));
                if (isIdValid(id) && isMob(id))
                {
                    if (getCreatureName(id).equalsIgnoreCase(mob))
                    {
                        sendSystemMessageTestingOnly(self, "Found mob of type " + mob + " with ID " + id + " so warping you to them...");
                        warpPlayer(self, getLocation(id), null, false);
                        return SCRIPT_CONTINUE;
                    }
                }
            }
            sendSystemMessageTestingOnly(self, "ERROR: Couldn't find a mob of name " + mob);
            return SCRIPT_CONTINUE;
        }*/
        else if (cmd.equalsIgnoreCase("toggleRadarMap"))
        {
            obj_id who = getIntendedTarget(self);
            if (who == null || isPlayer(who))
            {
                broadcast(self, "You must target an NPC to toggle their radar and map visibility.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                setVisibleOnMapAndRadar(who, !getVisibleOnMapAndRadar(who));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("rugShowcase"))
        {
            location whereAmI = getLocation(self); // Get the initial location
            String rugTemplate = "object/tangible/tarkin_custom/decorative/rug/tarkin_rug_";
            String rugSuffix = ".iff";
            int rugCount = 175;

            float maxRadius = 15.0f; // Maximum radius (base of the "tree")
            float minRadius = 1.0f; // Minimum radius (top of the "tree")
            float heightStep = 0.5f; // Vertical distance between rugs
            float angleStep = 15.0f; // Angular increment per rug (degrees)
            float centerX = whereAmI.x; // Center of the spiral (X)
            float centerY = whereAmI.y; // Center of the spiral (Y)
            float currentRadius = maxRadius; // Start at the maximum radius
            float currentAngle = 180.0f; // Start facing west
            float currentHeight = whereAmI.z; // Start height at the initial position

            for (int i = 1; i <= rugCount; i++)
            {
                // Construct the rug's template path
                String rugPath = rugTemplate + i + rugSuffix;

                // Calculate offsets using polar coordinates
                float xOffset = (float) (currentRadius * Math.cos(Math.toRadians(currentAngle)));
                float yOffset = (float) (currentRadius * Math.sin(Math.toRadians(currentAngle)));

                // Set the rug's position (updating X, Y, and Z)
                whereAmI.x = centerX + xOffset;
                whereAmI.y = centerY + yOffset;
                whereAmI.z = currentHeight++; // Adjusted height for each rug

                // Orient the rug to face the center
                float deltaX = centerX - whereAmI.x;
                float deltaY = centerY - whereAmI.y;
                float orientation = (float) Math.toDegrees(Math.atan2(deltaY, deltaX)); // Angle toward the center

                // Create the rug
                obj_id rug = create.createObject(rugPath, whereAmI);
                setLocation(rug, whereAmI);
                setYaw(rug, orientation);

                // Update the spiral:
                currentAngle += angleStep; // Increment angle for spiral
                currentHeight += heightStep; // Move upward with each rug
                currentRadius = maxRadius - ((maxRadius - minRadius) * ((float) i / rugCount)); // Decrease radius as rugs ascend
            }

            return SCRIPT_CONTINUE;
        }

        else if (cmd.equalsIgnoreCase("swap"))
        {
            obj_id intendedTarget = getIntendedTarget(self);
            obj_id mainTarget = getTarget(self);

            setLocation(intendedTarget, getLocation(mainTarget));
            setLocation(mainTarget, getLocation(intendedTarget));
        }

        else if (cmd.equalsIgnoreCase("replace"))
        {
            String template = tok.nextToken();
            if (!isIdValid(target))
            {
                sendSystemMessage(self, "No valid target found to replace.", null);
                return SCRIPT_CONTINUE;
            }

            if (!template.endsWith(".iff"))
            {
                broadcast(self, "Bad template to replace.");
                return SCRIPT_CONTINUE;
            }
            String currentTemplate = getTemplateName(target);
            location targetLoc = getLocation(target);
            float[] targetRotation = getQuaternion(target);

            obj_id newObject = createObject(template, targetLoc);
            if (isIdValid(newObject))
            {
                setQuaternion(newObject, targetRotation[0], targetRotation[1], targetRotation[2], targetRotation[3]);
                destroyObject(target);
                broadcast(self, "Object replaced successfully.");
            }
            else
            {
                broadcast(self, "Failed to spawn the replacement object.");
            }
            return SCRIPT_CONTINUE;
        }


        else if (cmd.equalsIgnoreCase("setHologram"))
        {
            obj_id who = getIntendedTarget(self);
            if (who == null || isPlayer(who))
            {
                broadcast(self, "You must target an NPC to set their hologram type.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                int type = stringToInt(tok.nextToken());
                if (type < 0 || type > 4)
                {
                    broadcast(self, "Hologram type must be between 0 and 4. Or reset with /developer clearHologram");
                    return SCRIPT_CONTINUE;
                }
                else
                {
                    setHologramType(who, type);
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("clearHologram"))
        {
            obj_id who = getIntendedTarget(self);
            if (who == null || isPlayer(who))
            {
                broadcast(self, "You must target an NPC to reset their hologram type.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                setHologramType(who, -1);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("gimmeCorvette"))
        {
            obj_id scd = space_utils.createShipControlDevice(self, "corellian_corvette", true);
            if (isIdValid(scd))
                broadcast(self, "Corellian Corvette ship control device added to your datapad.");
            else
                broadcast(self, "Failed to create Corellian Corvette ship control device (check datapad and ship templates).");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("gimmeLambda"))
        {
            obj_id scd = space_utils.createShipControlDevice(self, "lambda", true);
            if (isIdValid(scd))
                broadcast(self, "Lambda Shuttle ship control device added to your datapad.");
            else
                broadcast(self, "Failed to create Lambda Shuttle  ship control device (check datapad and ship templates).");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("awardBadge"))
        {
            String parameter = tok.nextToken();
            badge.grantBadge(target, parameter);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer awardBadge " + parameter + " on " + getName(target));
        }
        else if (cmd.equalsIgnoreCase("getConfigSetting"))
        {
            String section = tok.nextToken();
            String key = tok.nextToken();
            String setting = utils.getConfigSetting(section, key);
            if (tok.countTokens() != 3)
            {
                broadcast(self, "Usage: /developer getConfigSetting [section] [key]");
                return SCRIPT_CONTINUE;
            }
            if (setting != null)
            {
                broadcast(self, "Key for " + section + " is: " + key);
            }
            else
            {
                broadcast(self, "Config option not found.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sendMail"))
        {
            String mailFrom = tok.nextToken();
            String mailSubject = tok.nextToken();
            StringBuilder mailBody = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                mailBody.append(" ").append(tok.nextToken());
            }
            sendFakeMail(self, getAllPlayers(getLocation(self), 15f), mailFrom, mailSubject, mailBody.toString(), false);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer sendMail");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("setbonus"))
        {
            obj_id tatooine = getPlanetByName("tatooine");
            String flag = tok.nextToken();
            if (flag.equalsIgnoreCase("heroics"))
            {
                float bonus = stringToFloat(tok.nextToken());
                setObjVar(tatooine, "bonus.heroic", bonus);
                broadcast(self, "Heroic/Mustafar Token bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the heroic token bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("worldboss"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.wb", bonus);
                broadcast(self, "World Boss Token bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the world boss token bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("duty"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.duty_token", bonus);
                broadcast(self, "Duty Token bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the duty token bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("entertainer"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.entertainer", bonus);
                broadcast(self, "Entertainer Token bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the entertainer token bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("gcw"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.gcw", bonus);
                broadcast(self, "GCW Token bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the gcw token bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("gcw_points"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.gcw_points", bonus);
                broadcast(self, "GCW Point bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the gcw point bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("xp"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.xp", bonus);
                broadcast(self, "XP bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the xp bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("battlefields"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.bf", bonus);
                broadcast(self, "Battlefield Token bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the battlefield token bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("ship_creation"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.ship_creation", bonus);
                broadcast(self, "Ship Creation bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Ship Creation bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("ship_bonus"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.ship_buyback", bonus);
                broadcast(self, "Ship Component buyback bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Ship Component Sale bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("junk_dealer"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.junk_dealer", bonus);
                broadcast(self, "Junk Dealer bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Junk Dealer bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("mission"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.mission", bonus);
                broadcast(self, "Dynamic mission payout bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Dynamic Mission bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("mission_bh"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.mission_bh", bonus);
                broadcast(self, "Bounty mission payout bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Bounty Mission bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("mission_gcw"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.mission_gcw", bonus);
                broadcast(self, "GCW mission payout bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the GCW Mission bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("mission_pve"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.mission_pve", bonus);
                broadcast(self, "PVE/Adventurer mission payout bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the PvE/Adventurer Mission bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("mission_jedi_bounty"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.mission_jedi_bounty", bonus);
                broadcast(self, "Jedi Bounty mission payout bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Jedi Bounty Mission bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("cashLoot"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.cashLoot", bonus);
                broadcast(self, "Credit loot drop payout bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the credit loot drop bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("creature_harvesting"))
            {
                int bonus = stringToInt(tok.nextToken());
                setObjVar(tatooine, "bonus.creature_harvesting", bonus);
                broadcast(self, "Creature Harvesting bonus set to " + bonus + "x");
                LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Bounty Mission bonus to " + bonus + "x");
            }
            else if (flag.equalsIgnoreCase("rls"))
            {
                String subflag = tok.nextToken();
                if (subflag.equalsIgnoreCase("maxLevelsBelow"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.maxDifferenceBelow", bonus);
                    broadcast(self, "Max Levels Below Player Level set to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Max Levels Below Player Level to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("maxLevelsAbove"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.maxDifferenceAbove", bonus);
                    broadcast(self, "Max Levels Above Player Level set to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Max Levels Above Player Level to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("rare"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.rare", bonus);
                    broadcast(self, "Rare Chest Drop Chance set to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Rare Chest Drop Chance to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("exceptional"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.exceptional", bonus);
                    broadcast(self, "Exceptional Chest Drop Chance set to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Exceptional Chest Drop Chance to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("legendary"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.legendary", bonus);
                    broadcast(self, "Legendary Chest Drop Chance set to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Legendary Chest Drop Chance to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("minDistance"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.minDistance", bonus);
                    broadcast(self, "Min Distance For Next Chest  to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Min Distance Away For Another to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("minTime"))
                {
                    int bonus = stringToInt(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.minTime", bonus);
                    broadcast(self, "Min Time Between Chests set to " + bonus);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set the Min Time Between Chests to " + bonus);
                }
                else if (subflag.equalsIgnoreCase("setGroupLoot"))
                {
                    boolean value = Boolean.parseBoolean(tok.nextToken());
                    setObjVar(tatooine, "bonus.rls.group_status", value);
                    broadcast(self, "Min Time Between Chests set to " + value);
                    LOG("ethereal", "[Server Bonuses]: " + getName(self) + " set group loot value to " + value);
                }
                else
                {
                    broadcast(self, "Usage: /developer setbonus rls [maxDifferenceBelow | maxDifferenceAbove | rare | exceptional | legendary | minDistance | minTime | toggleGroupLoot] [amount]");
                    broadcast(self, "Example usage: /developer setbonus rls maxDifferenceBelow 10");
                    broadcast(self, "Example usage: /developer setbonus rls rare 65");
                    return SCRIPT_CONTINUE;
                }
                return SCRIPT_CONTINUE;
            }
            else
            {
                broadcast(self, "Usage: /developer setbonus [heroics | worldboss | duty | entertainer | gcw | gcw_points | xp | battlefields | ship_creation | ship_bonus | junk_dealer | mission | mission_bh | mission_gcw | mission_pve | mission_jedi_bounty | cashLoot] [amount]");
                broadcast(self, "Usage: /developer setbonus rls [rlsDropChance | maxDifferenceBelow | maxDifferenceAbove | rare | exceptional | legendary | minDistance | minTime] [amount]");
                broadcast(self, "Example usage: /developer setbonus heroics 2.0");
                broadcast(self, "Example usage: /developer setbonus duty 2");
                broadcast(self, "Example usage: /developer setbonus junk_dealer 4");
                broadcast(self, "Example usage: /developer setbonus mission_bh 3");
                broadcast(self, "Note: Only heroics require a float (1.0), all else require an integer (1)");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("revertbonuses"))
        {
            obj_id tatooine = getPlanetByName("tatooine");
            removeObjVar(tatooine, "bonus");
            setObjVar(tatooine, "bonus.heroic", 1.0f);
            setObjVar(tatooine, "bonus.wb", 1);
            setObjVar(tatooine, "bonus.duty_token", 1);
            setObjVar(tatooine, "bonus.entertainer", 1);
            setObjVar(tatooine, "bonus.gcw", 1);
            setObjVar(tatooine, "bonus.gcw_points", 1);
            setObjVar(tatooine, "bonus.xp", 1);
            setObjVar(tatooine, "bonus.bf", 1);
            setObjVar(tatooine, "bonus.ship_creation", 1);
            setObjVar(tatooine, "bonus.ship_buyback", 1);
            setObjVar(tatooine, "bonus.junk_dealer", 1);
            setObjVar(tatooine, "bonus.mission", 1);
            setObjVar(tatooine, "bonus.mission_bh", 1);
            setObjVar(tatooine, "bonus.mission_gcw", 1);
            setObjVar(tatooine, "bonus.mission_pve", 1);
            setObjVar(tatooine, "bonus.mission_jedi_bounty", 1);
            setObjVar(tatooine, "bonus.cashLoot", 1);
            setObjVar(tatooine, "bonus.creature_harvesting", 1);
            setObjVar(tatooine, "bonus.rls.maxDifferenceBelow", 10);
            setObjVar(tatooine, "bonus.rls.maxDifferenceAbove", 10);
            setObjVar(tatooine, "bonus.rls.rare", 65);//           ***MUST***
            setObjVar(tatooine, "bonus.rls.exceptional", 25);//***EQUAL***
            setObjVar(tatooine, "bonus.rls.legendary", 10);//     ***100***
            setObjVar(tatooine, "bonus.rls.minDistance", 15);
            setObjVar(tatooine, "bonus.rls.minTime", 15); //@Note: this is multiplied by 60 to get minutes.
            setObjVar(tatooine, "bonus.rls.status", true); //@Note: this ensures it does not get turned off on bonus reset.
            setObjVar(tatooine, "bonus.rls.group_status", true); //@Note: this ensures it does not get turned off on bonus reset.
            setObjVar(tatooine, "bonus.rls.rlsDropChance", "1");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer revertbonuses | Setting all bonuses to 1x or 1.0x");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getbonuses"))
        {
            String prompt = "Current Server Modifiers:\n";
            obj_id tatooine = getPlanetByName("tatooine");
            float heroic = getFloatObjVar(tatooine, "bonus.heroic");
            int wb = getIntObjVar(tatooine, "bonus.wb");
            int duty = getIntObjVar(tatooine, "bonus.duty_token");
            int entertainer = getIntObjVar(tatooine, "bonus.entertainer");
            int gcw = getIntObjVar(tatooine, "bonus.gcw");
            int gcw_points = getIntObjVar(tatooine, "bonus.gcw_points");
            int xp = getIntObjVar(tatooine, "bonus.xp");
            int bf = getIntObjVar(tatooine, "bonus.bf");
            int ship_creation = getIntObjVar(tatooine, "bonus.ship_creation");
            int ship_buyback = getIntObjVar(tatooine, "bonus.ship_buyback");
            int jd_bonus = getIntObjVar(tatooine, "bonus.junk_dealer");
            int mission = getIntObjVar(tatooine, "bonus.mission");
            int mission_bh = getIntObjVar(tatooine, "bonus.mission_bh");
            int mission_gcw = getIntObjVar(tatooine, "bonus.mission_gcw");
            int mission_pve = getIntObjVar(tatooine, "bonus.mission_pve");
            int mission_jedi_bounty = getIntObjVar(tatooine, "bonus.mission_jedi_bounty");
            int cashLoot = getIntObjVar(tatooine, "bonus.cashLoot");
            int creatureHarvesting = getIntObjVar(tatooine, "bonus.creature_harvesting");
            String rlsStatus = getStringObjVar(tatooine, "bonus.rls.rlsDropChance");
            int rlsMaxLevelsBelowPlayerLevel = getIntObjVar(tatooine, "bonus.rls.maxDifferenceBelow");
            int rlsMaxLevelsAbovePlayerLevel = getIntObjVar(tatooine, "bonus.rls.maxDifferenceAbove");
            int rlsRareDropChance = getIntObjVar(tatooine, "bonus.rls.rare");
            int rlsExceptionalDropChance = getIntObjVar(tatooine, "bonus.rls.exceptional");
            int rlsLegendaryDropChance = getIntObjVar(tatooine, "bonus.rls.legendary");
            int rlsMinDistance = getIntObjVar(tatooine, "bonus.rls.minDistance");
            int rlsMinTimeBetweenAwards = getIntObjVar(tatooine, "bonus.rls.minTime");
            boolean rls = getBooleanObjVar(tatooine, "bonus.rls.status");
            boolean rlsGroup = getBooleanObjVar(tatooine, "bonus.rls.statusGroup");
            prompt = prompt + "\\#DAA450Token Bonuses\\#.\n";
            prompt = prompt + "\tHeroic Tokens:  " + heroic + "x\n";
            prompt = prompt + "\tWorld Boss Tokens: " + wb + "x\n";
            prompt = prompt + "\tDuty Tokens: " + duty + "x\n";
            prompt = prompt + "\tEntertainer Tokens: " + entertainer + "x\n";
            prompt = prompt + "\tGCW Tokens: " + gcw + "x\n";
            prompt = prompt + "\tGCW Points: " + gcw_points + "x\n";
            prompt = prompt + "\tExperience: " + xp + "x\n";// this doesn't seem to affect anything.
            prompt = prompt + "\tBattlefield Tokens: " + bf + "x\n\n";
            prompt = prompt + "\\#DAA450Space Bonuses\\#.\n";
            prompt = prompt + "\tShip Creation Bonus: " + ship_creation + "x\n";
            prompt = prompt + "\tShip Component Sale Bonus: " + ship_buyback + "x\n\n";
            prompt = prompt + "\\#DAA450Mission Terminal Bonuses\\#.\n";
            prompt = prompt + "\tDynamic Mission Bonus: " + mission + "x\n";
            prompt = prompt + "\tBounty Hunter Mission Bonus: " + mission_bh + "x\n";
            prompt = prompt + "\tGCW Mission Bonus: " + mission_gcw + "x\n";
            prompt = prompt + "\tNon-Destroy Mission Bonus: " + mission_pve + "x\n";
            prompt = prompt + "\tJunk Dealer Bonus: " + mission_jedi_bounty + "x\n\n";
            prompt = prompt + "\\#DAA450Loot Bonuses\\#.\n";
            prompt = prompt + "\tLooted Credit Bonus: " + cashLoot + "x\n";
            prompt = prompt + "\tJunk Dealer Bonus: " + jd_bonus + "x\n\n";
            prompt = prompt + "\\#DAA450Creature Harvesting\\#.\n";
            prompt = prompt + "\tCreature Harvesting Bonus: " + creatureHarvesting;
            prompt = prompt + "\\#DAA450RLS Status and Bonuses\\#.\n";
            prompt = prompt + "\tRLS is: " + (rls ? "Enabled" : "Disabled") + "\n";
            prompt = prompt + "\tGroup Loot System is: " + (rlsGroup ? "Enabled" : "Disabled") + "\n";
            prompt = prompt + "\tMax Levels Below: " + rlsMaxLevelsBelowPlayerLevel + " levels.\n";
            prompt = prompt + "\tMax Levels Above: " + rlsMaxLevelsAbovePlayerLevel + " levels.\n";
            prompt = prompt + "\tRare Drop Chance: " + rlsRareDropChance + "%\n";
            prompt = prompt + "\tExceptional Drop Chance: " + rlsExceptionalDropChance + "%\n";
            prompt = prompt + "\tLegendary Drop Chance: " + rlsLegendaryDropChance + "% \n";
            prompt = prompt + "\tMin Distance Away Between Chests: " + rlsMinDistance + " meter(s).\n";
            prompt = prompt + "\tMin Time Between Chests: " + rlsMinTimeBetweenAwards + " minutes.\n";
            if (isGod(self))
            {
                prompt = prompt + "\n\n\\#DAA450Do not forget to target yourself and run \"/developer revertbonuses\" to reset all bonuses to 1x or its default value (true, \"1\", etc..)";
                if (rlsRareDropChance + rlsExceptionalDropChance + rlsLegendaryDropChance != 100)
                {
                    prompt = prompt + "\n\n\\#FF0000WARNING: " + getClusterName() + "'s RLS settings are mangled. All three chest chances need to equal 100. Fix immediately!";
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer getbonuses and found RLS settings are incorrect. Rare: " + rlsRareDropChance + " Exceptional: " + rlsExceptionalDropChance + " Legendary: " + rlsLegendaryDropChance);
                }
                if (!rls)
                {
                    prompt = prompt + "\n\n\\#FF0000WARNING: " + getClusterName() + "'s RLS system is disabled. Enable it immediately!";
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer getbonuses and found RLS system is disabled.");
                }
                else
                {
                    LOG("ethereal", "[Developer]: Certain bonuses have been checked and are correct.");
                }
            }
            int page = createSUIPage("/Script.messageBox", self, self);
            setSUIProperty(page, "Prompt.lblPrompt", "LocalText", prompt);
            setSUIProperty(page, "Prompt.lblPrompt", "Text", prompt);
            setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
            setSUIProperty(page, "bg.caption.lblTitle", "Text", getClusterName());
            setSUIProperty(page, "Prompt.lblPrompt", "Editable", "True");
            setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
            subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
            setSUIProperty(page, "btnCancel", "Visible", "false");
            setSUIProperty(page, "btnRevert", "Visible", "false");
            setSUIProperty(page, "btnOk", "Visible", "true");
            showSUIPage(page);
            flushSUIPage(page);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer getbonuses");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("modifyCollection"))
        {
            String collectionName = tok.nextToken();
            int value = stringToInt(tok.nextToken());
            if (tok.countTokens() != 2)
            {
                broadcast(self, "Usage: /developer modifyCollection [collection] [value (can be negative)]");
                return SCRIPT_CONTINUE;
            }
            else
            {
                modifyCollectionSlotValue(self, collectionName, value);
            }
        }
        else if (cmd.equalsIgnoreCase("toggleRLS"))
        {
            if (!hasObjVar(getPlanetByName("tatooine"), "bonus.rls.status"))
            {
                setObjVar(getPlanetByName("tatooine"), "bonus.rls.status", false);
                broadcast(self, "RLS System is now disabled.");
                LOG("ethereal", "[Rare Loot System]: " + getPlayerFullName(self) + " disabled the RLS system.");
            }
            else
            {
                boolean status = getBooleanObjVar(getPlanetByName("tatooine"), "bonus.rls.status");
                setObjVar(getPlanetByName("tatooine"), "bonus.rls.status", !status);
                broadcast(self, "RLS System is now " + (!status ? "disabled" : "enabled.") + ".");
                LOG("ethereal", "[Rare Loot System]: " + getPlayerFullName(self) + " has turned RLS " + (!status ? "off" : "on"));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("resetCollection"))
        {
            String collectionName = tok.nextToken();
            if (tok.countTokens() != 1)
            {
                broadcast(self, "Usage: /developer resetCollection [collection]");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String[] slotsInCollection = getAllCollectionSlotsInCollection(collectionName);
                for (String s : slotsInCollection)
                {
                    long collectionSlotValue = getCollectionSlotValue(self, s) * -1;
                    modifyCollectionSlotValue(self, s, collectionSlotValue);
                }
            }
        }
        else if (cmd.equalsIgnoreCase("getCollectionReward"))
        {
            String collection = tok.nextToken();
            if (tok.countTokens() != 1)
            {
                broadcast(self, "Usage: /developer getCollectionReward [collection]");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String COLLECTION_TABLE = "datatables/collection/rewards.iff";
                String[] collectionNames = dataTableGetStringColumnNoDefaults(COLLECTION_TABLE, "item");
                for (String collectionName : collectionNames)
                {
                    if (collectionName.equals(collection))
                    {
                        String item = dataTableGetString(COLLECTION_TABLE, collectionName, "item");
                        int amount = dataTableGetInt(COLLECTION_TABLE, collectionName, "stackAmount");
                        if (item.endsWith(".iff"))
                        {
                            obj_id reward = createObject(item, getInventoryContainer(self), "");
                            if (isIdValid(reward))
                            {
                                setCount(reward, amount);
                                broadcast(self, "You have been given the reward for the collection: " + collection + ".");
                                return SCRIPT_CONTINUE;
                            }
                        }
                        else
                        {
                            static_item.createNewItemFunction(item, self, amount);
                            broadcast(self, "You have been given the reward for the collection: " + collection + ".");
                        }
                        return SCRIPT_CONTINUE;
                    }
                }
            }
        }
        else if (cmd.equalsIgnoreCase("housing"))
        {
            String subcommand = tok.nextToken();
            if (subcommand.equalsIgnoreCase("layout"))
            {
                if (isInWorldCell(self))
                {
                    broadcast(self, "Nothing to save.");
                    return SCRIPT_CONTINUE;
                }
                else
                {
                    showImportExportOptions(self);
                    return SCRIPT_CONTINUE;
                }
            }
            else
            {
                broadcast(self, "Usage: /developer housing [layout]");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sendPrompt"))
        {
            StringBuilder prompt = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                prompt.append(" ").append(tok.nextToken());
            }
            String modelResponse = openwebui.getCompletion(openwebui.API_KEY, String.valueOf(prompt));
            response_store.addResponse(String.valueOf(prompt), modelResponse);
            chat.chat(self, modelResponse);
        }
        else if (cmd.equalsIgnoreCase("mapLocations"))
        {
            String flag = tok.nextToken();
            String category = tok.nextToken();
            String subcategory = tok.nextToken();
            StringBuilder name = new StringBuilder();
            while (tok.hasMoreTokens())
            {
                name.append(" ").append(tok.nextToken());
            }
            if (flag.equals("add"))
            {
                addDirtyPlanetMapLocation(self, name.toString(), getLocation(self), category, subcategory);
            }
            else if (flag.equals("remove"))
            {
                removeDirtyPlanetMapLocation(self, name.toString());
            }
            else
            {
                broadcast(self, "Usage: /developer mapLocations add [category] [subcategory] [name]");
                broadcast(self, "Usage: /developer mapLocations remove [name]");
                broadcast(self, "You may only have one active map location active at any given time.");
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer mapLocations " + flag + " " + category + " " + subcategory + " " + name);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("shutdown"))
        {
            //ten mins in seconds = 600
            String shutdownCommand = "/server shutdown 900 900 SWG - OR will be taken offline in 15 minutes to reboot the game server.";
            String API_KEY_DISCORD = "https://discord.com/api/webhooks/1295079110334218351/lEqOfBkJMPTHz0frb4T7VvElsMfyUpN0Kwwer4s57-litfanUk5--pd-5wFq3mE6WVVz";
            String shutdownMessage = "SWG-OR is shutting down in 15 minutes for a reboot.";
            DiscordWebhook webhook = new DiscordWebhook(API_KEY_DISCORD);
            webhook.setContent(shutdownMessage);
            webhook.execute();
            sendConsoleCommand(shutdownCommand, self);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer shutdown on " + date.getFullDate(self, "en_US") + " (" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getGameTime()) + ")");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("webhook"))
        {
            String name = tok.nextToken();
            StringBuilder message = new StringBuilder();
            while (tok.hasMoreTokens())
            {
                message.append(" ").append(tok.nextToken());
            }
            sendDiscordMessage(API, message.toString(), PFP, "System | Debug");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer webhook " + name + " " + message);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("specialPumpkin"))
        {
            obj_id pumpkin = createObject("object/tangible/holiday/halloween/pumpkin_object.iff", getLocation(target));
            attachScript(pumpkin, "event.halloween.pumpkin_smasher_object");
            setName(pumpkin, color("EEAB19", "a pumpkin"));
            setDescriptionString(pumpkin, "This pumpkin is special. Try smashing it!");
            setObjVar(pumpkin, "specialPumpkin", 1);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("treatPalette"))
        {
            obj_id bowl = createObject("object/tangible/furniture/decorative/basket_closed.iff", getInventoryContainer(self), "");
            attachScript(bowl, "event.halloween.candy_bucket");
            broadcast(self, "A treat bowl has been created.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sendMailWaypoint"))
        {
            String mailFrom = tok.nextToken();
            String mailSubject = tok.nextToken();
            StringBuilder mailBody = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                mailBody.append(" ").append(tok.nextToken());
            }
            sendFakeMail(self, getAllPlayers(getLocation(self), 15f), mailFrom, mailSubject, mailBody.toString(), true);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer sendMailWaypoint");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("environment"))
        {
            // /developer environment PATH -> os path environment
            String subcommand = tok.nextToken();
            if (subcommand.equals("get"))
            {
                String key = System.getenv(tok.nextToken());
                broadcast(self, key);
                echo(self, color("FF0000", key));
                LOG("ethereal", "[Developer]: Key value: " + key);
            }
            else
            {
                broadcast(self, "/developer environment get [key]");
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer environment " + subcommand);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("bypassCollectionTimer"))
        {
            setScriptVar(self, "collection.qa.clickBypass", 0);
            LOG("ethereal", "[Collections]: " + getPlayerFullName(self) + " has bypassed the collection timer until logout.");
            broadcast(self, "You have bypassed the collection timer until logout.");
        }
        else if (cmd.equals("listWattos"))
        {
            obj_id[] wattos = getAllObjectsWithObjVar(getLocation(self), 16000f, "watto_tag");
            for (obj_id watto : wattos)
            {
                echo(self, "Watto found: " + watto);
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer listWattos");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("adventPresent"))
        {
            obj_id present = create.object("object/tangible/event_perk/life_day_presents.iff", getLocation(self));
            attachScript(present, "event.lifeday.days_of_lifeday");
            broadcast(self, "Made present");
        }
        else if (cmd.equals("makeEventToken"))
        {
            obj_id event_token = createObject("object/tangible/loot/misc/marauder_token.iff", getLocation(self));
            setName(event_token, "Event Token");
            setDescriptionString(event_token, "These tokens can be used to swap out the item for a TCG prize..");
            attachScript(event_token, "content.tos_clicky");
        }
        else if (cmd.equals("gotoWatto"))
        {
            obj_id[] wattos = getAllObjectsWithObjVar(getLocation(self), 16000f, "watto_tag");
            if (wattos.length > 0)
            {
                location wattoLoc = getLocation(wattos[0]);
                warpPlayer(self, wattoLoc.area, wattoLoc.x, wattoLoc.y, wattoLoc.z, null, 0, 0, 0);
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer gotoWatto");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("clipboard"))
        {
            String clipboard = tok.nextToken();
            if (clipboard == null)
            {
                broadcast(self, "Not enough arguments. Usage: /developer clipboard [location | scripts | objvars]");
            }
            if (clipboard.equals("location"))
            {
                String locationString = getLocation(self).toClipboardFormat();
                int page = createSUIPage("/Script.messageBox", self, self);
                setSUIProperty(page, "Prompt.lblPrompt", "LocalText", locationString);
                setSUIProperty(page, "Prompt.lblPrompt", "Text", locationString);
                setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
                setSUIProperty(page, "bg.caption.lblTitle", "Text", "CLIPBOARD - Location");
                setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
                setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
                subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
                setSUIProperty(page, "btnCancel", "Visible", "false");
                setSUIProperty(page, "btnRevert", "Visible", "false");
                setSUIProperty(page, "btnOk", "Visible", "false");
                showSUIPage(page);
                broadcast(self, "Location copied to SUI. Press CTRL + C to copy to clipboard. (check keybinds)");
                return SCRIPT_CONTINUE;
            }
            if (clipboard.equals("scripts"))
            {
                StringBuilder scriptString = new StringBuilder();
                String[] scripts = getScriptList(target);
                for (String s : scripts)
                {
                    String removed = s.replace("script.", "");
                    scriptString.append(removed).append("\n");
                }
                String wholePrompt = "Scripts for " + target + ":\n" + scriptString;
                int page = createSUIPage("/Script.messageBox", self, self);
                setSUIProperty(page, "Prompt.lblPrompt", "LocalText", wholePrompt);
                setSUIProperty(page, "Prompt.lblPrompt", "Text", wholePrompt);
                setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
                setSUIProperty(page, "bg.caption.lblTitle", "Text", "CLIPBOARD - Scripts");
                setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
                setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
                subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
                setSUIProperty(page, "btnCancel", "Visible", "false");
                setSUIProperty(page, "btnRevert", "Visible", "false");
                setSUIProperty(page, "btnOk", "Visible", "false");
                showSUIPage(page);
                flushSUIPage(page);
                broadcast(self, "Scripts copied to SUI. Press CTRL + C to copy to clipboard. (check keybinds)");
                return SCRIPT_CONTINUE;
            }
            if (clipboard.equals("objvars"))
            {
                obj_var_list ovl = getObjVarList(target, "");
                StringBuilder objvarString = new StringBuilder();
                if (ovl != null)
                {
                    int ovCount = ovl.getNumItems();
                    for (int i = 0; i < ovCount; i++)
                    {
                        obj_var ov = ovl.getObjVar(i);
                        String ovName = ov.getName();
                        objvarString.append(ovName).append("\n");
                    }
                }
                String objvarPrompt = "ObjVars for " + target + ":\n" + objvarString;
                int page = createSUIPage("/Script.messageBox", self, self);
                setSUIProperty(page, "Prompt.lblPrompt", "LocalText", objvarPrompt);
                setSUIProperty(page, "Prompt.lblPrompt", "Text", objvarPrompt);
                setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
                setSUIProperty(page, "bg.caption.lblTitle", "Text", "CLIPBOARD - Variables");
                setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
                setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
                subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
                setSUIProperty(page, "btnCancel", "Visible", "false");
                setSUIProperty(page, "btnRevert", "Visible", "false");
                setSUIProperty(page, "btnOk", "Visible", "false");
                showSUIPage(page);
                broadcast(self, "Objvars copied to SUI. Press CTRL + C to copy to clipboard. (check keybinds)");
                return SCRIPT_CONTINUE;
            }
            if (clipboard.equals("template"))
            {
                String template = getTemplateName(target);
                String body = "Template for " + getName(target) + ":\n\n" + template + "\n\nName: " + getTemplateName(target) + "\n" + "Description: " + getDescriptionStringId(target) + "\n" + "Appearance: " + getAppearance(target);
                int page = createSUIPage("/Script.messageBox", self, self);
                setSUIProperty(page, "Prompt.lblPrompt", "LocalText", body);
                setSUIProperty(page, "Prompt.lblPrompt", "Text", body);
                setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
                setSUIProperty(page, "bg.caption.lblTitle", "Text", "CLIPBOARD - Template");
                setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
                setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
                subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
                setSUIProperty(page, "btnCancel", "Visible", "false");
                setSUIProperty(page, "btnRevert", "Visible", "false");
                setSUIProperty(page, "btnOk", "Visible", "false");
                showSUIPage(page);
                broadcast(self, "Objvars copied to SUI. Press CTRL + C to copy to clipboard. (check keybinds)");
                return SCRIPT_CONTINUE;
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer clipboard " + clipboard);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("makePet"))
        {
            obj_id pet = create.createCreature(tok.nextToken(), getLocation(self), false);
            if (isIdValid(pet))
            {
                setObjVar(pet, "gm.adhocPet", true);
                obj_id petControlDevice = pet_lib.makeControlDevice(self, pet);
                callable.setCallableCD(pet, petControlDevice);
                pet_lib.makePet(pet, self);
                ai_lib.setDefaultCalmBehavior(pet, ai_lib.BEHAVIOR_ATTACK);
                callable.setCallableLinks(pet, petControlDevice, pet);
                dictionary param = new dictionary();
                param.put("pet", pet);
                param.put("master", self);
                param.put("controlDevice", petControlDevice);
                messageTo(pet, "handleAddMaster", param, 0.5f, false);
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer makePet on " + getName(pet));
            }
            else
            {
                broadcast(self, "Failed to create pet, invalid template.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("say"))
        {
            StringBuilder speech = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                speech.append(" ").append(tok.nextToken());
            }
            chat.chat(getIntendedTarget(self), speech.toString());
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer say " + speech + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("objectsay"))
        {
            StringBuilder speech = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                speech.append(" ").append(tok.nextToken());
            }
            debugSpeakMsg(getIntendedTarget(self), speech.toString());
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer say " + speech + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("targetTest"))
        {
            echo(self, "Target: " + getTarget(self) + " | " + getName(getTarget(self)));
            echo(self, "Intended Target: " + getIntendedTarget(self) + " | " + getName(getTarget(self)));
            echo(self, "Look At Target: " + getLookAtTarget(self) + " | " + getName(getTarget(self)));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer targetTest");
        }
        else if (cmd.equalsIgnoreCase("comm"))
        {
            String speech = tok.nextToken();
            StringBuilder combinedMessage = new StringBuilder();
            while (tok.hasMoreTokens())
            {
                combinedMessage.append(speech).append(" ");
            }
            if (!combinedMessage.toString().isEmpty())
            {
                prose_package pp = prose.getPackage(new string_id(combinedMessage.toString()));
                commPlayer(self, target, pp);
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer comm " + combinedMessage + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("resourceDatapad"))
        {
            obj_id myTarget = getIntendedTarget(self);
            obj_id pInv = getInventoryContainer(myTarget);
            obj_id datapad = createObject("object/tangible/loot/npc_loot/datapad_flashy_generic.iff", pInv, "");
            setName(datapad, "Resource Analyzer");
            setBioLink(datapad, myTarget);
            setObjVar(datapad, "resource_amount", 10000);
            setObjVar(datapad, "noTrade", 1);
            attachScript(datapad, "developer.bubbajoe.dev_res");
            attachScript(datapad, "item.special.nomove");
            static_item.setStaticItemName(datapad, "Resource Analyzer");
            static_item.setDescriptionStringId(datapad, new string_id("This item is used to generate resources that have erroneously been removed from the game.\n\n" + "It is a developer tool and should not be used by players."));
            broadcast(self, "Resource Analyzer has been added to your inventory. All actions regarding this tool are logged. [Player: " + myTarget + "]");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer resourceDatapad on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("uberize"))
        {
            String type = tok.nextToken();
            String[] skillMods = dataTableGetStringColumnNoDefaults("datatables/buff/effect_mapping.iff", "SUBTYPE");
            switch (type)
            {
                case "crafting":
                    for (String s : skillMods)
                    {
                        if (s.contains("_experimentation") || s.contains("_assembly") || s.contains("_customization") || s.startsWith("jedi_saber_"))
                        {
                            setSkillModBonus(target, s, 35);
                        }
                    }
                    break;
                case "combat":
                    for (String s : skillMods)
                    {
                        if (s.startsWith("combat_") || s.endsWith("_modified") || s.startsWith("expertise_") || s.startsWith("private_"))
                        {
                            setSkillModBonus(target, s, 35);
                        }
                    }
                    break;
                case "all":
                    for (String s : skillMods)
                    {
                        setSkillModBonus(target, s, 35);
                    }
                    broadcast(self, "Rawdogging " + getName(target) + " with " + skillMods.length + " skillmods.");
                    break;
                default:
                    broadcast(self, "Invalid type. Valid types are: crafting, combat, all");
                    break;
            }
            LOG("ethereal", "[Developer]: ***" + getName(self) + "*** used /developer uberize " + type + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("socketize"))
        {
            int amount = stringToInt(tok.nextToken());
            if (isGameObjectTypeOf(getIntendedTarget(self), GOT_armor) || isGameObjectTypeOf(getIntendedTarget(self), GOT_clothing) || isGameObjectTypeOf(getIntendedTarget(self), GOT_weapon))
            {
                setSkillModSockets(target, amount);
                setCondition(target, CONDITION_MAGIC_ITEM);
            }
            else
            {
                broadcast(self, "Failed. Your target is not a piece of armor, clothing or weapon.");
            }
            LOG("ethereal", "[Developer]: ***" + getName(self) + "*** used /developer socketize " + amount + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("magicPaintingUrl"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Usage: /developer magicPaintingUrl <url>");
                return SCRIPT_CONTINUE;
            }

            String url = tok.nextToken();
            setCondition(target, CONDITION_MAGIC_PAINTING_URL);
            setObjVar(target, "texture.url", url);
            setObjVar(target, "texture.mode", "IMAGE_ONLY");
            setObjVar(target, "texture.displayMode", "CUBE");
            setObjVar(target, "texture.scrollH", "0");
            setObjVar(target, "texture.scrollV", "0");

            broadcast(self, "Enabled magic painting URL on target: " + getName(target));
            broadcast(self, "URL: " + url);
            LOG("ethereal", "[Developer]: ***" + getName(self) + "*** used /developer magicPaintingUrl " + url + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("spawnTelevision"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Usage: /developer spawnTelevision <url> [scale]");
                return SCRIPT_CONTINUE;
            }

            String url = tok.nextToken();
            float scale = 1.0f;
            if (tok.hasMoreTokens())
            {
                try
                {
                    scale = Float.parseFloat(tok.nextToken());
                }
                catch (NumberFormatException e)
                {
                    scale = 1.0f;
                }
            }
            if (scale < 0.1f) scale = 0.1f;
            if (scale > 20.0f) scale = 20.0f;

            obj_id tv = spawnVideoPlayer(self, url, scale, true);
            if (!isIdValid(tv))
            {
                broadcast(self, "Failed to create television object.");
                return SCRIPT_CONTINUE;
            }

            broadcast(self, "Spawned video player at your location.");
            broadcast(self, "URL: " + url + " | Scale: " + scale);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("dynamicsTest"))
        {
            // Open SUI panel to configure TangibleDynamics on hard target
            obj_id hardTarget = getIntendedTarget(self);
            if (!isIdValid(hardTarget))
            {
                broadcast(self, "No target selected! Target an object first.");
                return SCRIPT_CONTINUE;
            }

            // Show the dynamics test SUI
            showDynamicsTestSUI(self, hardTarget);
        }
        else if (cmd.equalsIgnoreCase("hockey"))
        {
            // Hockey game setup
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "=== Hockey Game Commands ===");
                broadcast(self, "/developer hockey setup - Auto-setup arena at your location");
                broadcast(self, "/developer hockey puck - Spawn a puck at your location");
                broadcast(self, "/developer hockey goal red - Place red goal at your location");
                broadcast(self, "/developer hockey goal blue - Place blue goal at your location");
                broadcast(self, "/developer hockey manager - Spawn a game manager");
                return SCRIPT_CONTINUE;
            }

            String subCmd = tok.nextToken().toLowerCase();
            location playerLoc = getLocation(self);

            if (subCmd.equals("setup"))
            {
                // Auto-setup: spawn manager, goals 40m apart, puck in center

                // Create manager at player location
                obj_id manager = createObject("object/tangible/terminal/terminal_mission.iff", playerLoc);
                if (isIdValid(manager))
                {
                    attachScript(manager, "systems.hockey_manager");
                    setName(manager, "Hockey Game Manager");

                    // Create red goal
                    location redLoc = new location(playerLoc.x + 20.0f, playerLoc.y, playerLoc.z, playerLoc.area);
                    obj_id redGoal = createObject("object/tangible/spawning/spawn_egg.iff", redLoc);
                    if (isIdValid(redGoal))
                    {
                        attachScript(redGoal, "systems.hockey_game");
                        setObjVar(redGoal, "hockey.team", "red");
                        setName(redGoal, "RED GOAL");
                        setObjVar(manager, "hockey.redGoalId", redGoal);
                    }

                    // Create blue goal
                    location blueLoc = new location(playerLoc.x - 20.0f, playerLoc.y, playerLoc.z, playerLoc.area);
                    obj_id blueGoal = createObject("object/tangible/spawning/spawn_egg.iff", blueLoc);
                    if (isIdValid(blueGoal))
                    {
                        attachScript(blueGoal, "systems.hockey_game");
                        setObjVar(blueGoal, "hockey.team", "blue");
                        setName(blueGoal, "BLUE GOAL");
                        setObjVar(manager, "hockey.blueGoalId", blueGoal);
                    }

                    // Set center point
                    setObjVar(manager, "hockey.centerLoc.x", playerLoc.x);
                    setObjVar(manager, "hockey.centerLoc.y", playerLoc.y);
                    setObjVar(manager, "hockey.centerLoc.z", playerLoc.z);
                    setObjVar(manager, "hockey.centerLoc.area", playerLoc.area);

                    // Store spawn location on goals
                    if (isIdValid(redGoal))
                    {
                        setObjVar(redGoal, "hockey.puckSpawnLoc.x", playerLoc.x);
                        setObjVar(redGoal, "hockey.puckSpawnLoc.y", playerLoc.y);
                        setObjVar(redGoal, "hockey.puckSpawnLoc.z", playerLoc.z);
                        setObjVar(redGoal, "hockey.puckSpawnLoc.area", playerLoc.area);
                    }
                    if (isIdValid(blueGoal))
                    {
                        setObjVar(blueGoal, "hockey.puckSpawnLoc.x", playerLoc.x);
                        setObjVar(blueGoal, "hockey.puckSpawnLoc.y", playerLoc.y);
                        setObjVar(blueGoal, "hockey.puckSpawnLoc.z", playerLoc.z);
                        setObjVar(blueGoal, "hockey.puckSpawnLoc.area", playerLoc.area);
                    }

                    // Spawn puck
                    location puckLoc = new location(playerLoc.x, playerLoc.y + 1.0f, playerLoc.z, playerLoc.area);
                    obj_id puck = createObject("object/tangible/furniture/all/frn_all_crate_s01.iff", puckLoc);
                    if (isIdValid(puck))
                    {
                        attachScript(puck, "systems.hockey_puck");
                        setObjVar(manager, "hockey.puckId", puck);
                    }

                    broadcast(self, "Hockey arena created! Goals are 40m apart. Push the puck into the opposing goal to score!");
                }
            }
            else if (subCmd.equals("puck"))
            {
                location puckLoc = new location(playerLoc.x, playerLoc.y + 1.0f, playerLoc.z, playerLoc.area);
                obj_id puck = createObject("object/tangible/furniture/all/frn_all_crate_s01.iff", puckLoc);
                if (isIdValid(puck))
                {
                    attachScript(puck, "systems.hockey_puck");
                    broadcast(self, "Hockey puck spawned! Walk into it to push it.");
                }
            }
            else if (subCmd.equals("goal"))
            {
                String team = "red";
                if (tok.hasMoreTokens())
                {
                    team = tok.nextToken().toLowerCase();
                }

                obj_id goal = createObject("object/tangible/spawning/spawn_egg.iff", playerLoc);
                if (isIdValid(goal))
                {
                    attachScript(goal, "systems.hockey_game");
                    setObjVar(goal, "hockey.team", team);
                    setName(goal, team.toUpperCase() + " GOAL");
                    broadcast(self, team.toUpperCase() + " goal placed at your location!");
                }
            }
            else if (subCmd.equals("manager"))
            {
                obj_id manager = createObject("object/tangible/terminal/terminal_mission.iff", playerLoc);
                if (isIdValid(manager))
                {
                    attachScript(manager, "systems.hockey_manager");
                    setName(manager, "Hockey Game Manager");
                    broadcast(self, "Hockey manager spawned! Use radial menu to set up the game.");
                }
            }
            else
            {
                broadcast(self, "Unknown hockey command: " + subCmd);
            }

            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("setCondition"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Usage: /developer setCondition <conditionBitmask>");
                return SCRIPT_CONTINUE;
            }

            if (!isTangible(target))
            {
                broadcast(self, "Target must be a tangible object.");
                return SCRIPT_CONTINUE;
            }

            int condition = stringToInt(tok.nextToken());
            if (condition <= 0)
            {
                broadcast(self, "Condition bitmask must be a positive integer.");
                return SCRIPT_CONTINUE;
            }

            setCondition(target, condition);
            broadcast(self, "Set condition " + condition + " on target: " + getName(target));
            LOG("ethereal", "[Developer]: ***" + getName(self) + "*** used /developer setCondition " + condition + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("craft"))
        {
            String template = tok.nextToken();
            int count = stringToInt(tok.nextToken());
            String fullTemplate = "object/draft_schematic/" + template + ".iff";
            if (fullTemplate.contains("shared_"))
            {
                fullTemplate.replace("shared_", "");
            }
            for (int i = 0; i < count; i++)
            {
                obj_id item = makeCraftedItem(fullTemplate, 100.0f, getInventoryContainer(self));
                setCrafter(item, self);
            }
            broadcast(self, "Capped craft: " + fullTemplate + "  (x" + count + ")");
        }
        else if (cmd.equalsIgnoreCase("skillmods"))
        {
            String subcommand = tok.nextToken();
            String skillmod = tok.nextToken();
            int amount = stringToInt(tok.nextToken());
            if (subcommand.equalsIgnoreCase("add"))
            {
                setSkillModBonus(target, skillmod, amount);
            }
            else if (subcommand.equalsIgnoreCase("remove"))
            {
                removeAttribOrSkillModModifier(target, skillmod);
            }
            else
            {
                broadcast(self, "Usage: /developer skillmod [add | remove] [skillmod] [amount]");
            }
        }
        else if (cmd.equalsIgnoreCase("listAllPlayersPlanetside"))
        {
            location planetside = new location();
            planetside.x = 0.0f;
            planetside.y = 0.0f;
            planetside.z = 0.0f;
            planetside.cell = null;
            obj_id[] players = getAllPlayers(planetside, PLANETWIDE);
            String[] playernames = new String[players.length]; // Adjust the size of the array to fit the number of players
            for (int i = 0; i < players.length; i++)
            {
                playernames[i] = getPlayerFullName(players[i]);
            }
            listbox(self, self, "Players found on this planet:", OK_ONLY, "Player Lookup", playernames, "noHandler");
        }
        else if (cmd.equalsIgnoreCase("planetPopulation"))
        {
            location planetside = new location();
            planetside.x = 0.0f;
            planetside.y = 0.0f;
            planetside.z = 0.0f;
            planetside.cell = null;
            obj_id[] players = getAllPlayers(planetside, PLANETWIDE);
            broadcast(self, "Planet population: " + players.length);
        }
        else if (cmd.equalsIgnoreCase("droidSockets"))
        {
            int which = stringToInt(tok.nextToken());
            if (which == 1)
            {
                setObjVar(target, "module_data.struct_maint", 1);
            }
            else if (which == 2)
            {
                setObjVar(target, "module_data.playback.modules", 2);
            }
            else if (which == 3)
            {
                setObjVar(target, "module_data.playback.modules", 3);
            }
            else
            {
                broadcast(self, "Unable to process, returning.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("describe"))
        {
            dictionary paramsDict = new dictionary();
            obj_id myTarget = getIntendedTarget(self);
            paramsDict.put("target", myTarget);
            inputbox(self, self, "Enter a description for the target.", OK_ONLY, "Describe", INPUT_NORMAL, null, "handleDescribe", paramsDict);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer describe on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("pumpkin"))
        {
            String flag = tok.nextToken();
            String[] pumpkinNames = {
                    "a plump pumpkin",
                    "a regular pumpkin",
                    "a scrawny pumpkin",
                    "a nasty pumpkin",
                    "a scary pumpkin",
                    "a jagged pumpkin",
            };
            if (flag.isEmpty())
            {
                broadcast(self, "/developer pumpkin [single | ring]");
                return SCRIPT_CONTINUE;
            }
            if (flag.equalsIgnoreCase("single"))
            {
                obj_id pumpkin = createObject("object/tangible/holiday/halloween/pumpkin_object.iff", getLocation(target));
                attachScript(pumpkin, "event.halloween.pumpkin_smasher_object");
                setName(pumpkin, color("EEAB19", pumpkinNames[rand(0, pumpkinNames.length - 1)]));
                setDescriptionString(pumpkin, "This pumpkin may contain something.... Try smashing it!");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer pumpkin " + flag + " on " + getName(target));

            }
            if (flag.equalsIgnoreCase("ghost"))
            {
                //if we have the objvar of "gm.ghost", remove and enable collision.
                if (hasObjVar(target, "gm.ghost"))
                {
                    removeObjVar(target, "gm.ghost");
                    setObjectCollidable(target, true);
                    broadcast(self, getName(target) + " is no longer a ghost.");
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " removed ghost mode from " + getName(target));
                }
                else
                {
                    setObjVar(target, "gm.ghost", true);
                    setObjectCollidable(target, false);
                    broadcast(self, getName(target) + " is now a ghost.");
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " enabled ghost mode on " + getName(target));
                }
            }
            if (flag.equalsIgnoreCase("ring"))
            {
                location loc = getLocation(target);
                int howMany = stringToInt(tok.nextToken());
                int radius = stringToInt(tok.nextToken());
                if (howMany == 0 || radius == 0)
                {
                    broadcast(self, "/developer pumpkin ring [num to spawn] [radius]");
                    return SCRIPT_CONTINUE;
                }
                if (!isIdValid(self) || !exists(self))
                {
                    return SCRIPT_CONTINUE;
                }
                float x;
                float z;
                for (int i = 0; i < howMany; i++)
                {
                    float angle = (float) (i * (360 / howMany));
                    x = loc.x + (float) Math.cos(angle) * radius;
                    z = loc.z + (float) Math.sin(angle) * radius;
                    obj_id pumpkin = create.object("object/tangible/holiday/halloween/pumpkin_object.iff", new location(x, getHeightAtLocation(x, z), z, loc.area));
                    attachScript(pumpkin, "event.halloween.pumpkin_smasher_object");
                    faceTo(pumpkin, target);
                    setName(pumpkin, color("EEAB19", pumpkinNames[rand(0, pumpkinNames.length - 1)]));
                    setDescriptionString(pumpkin, "This pumpkin may contain something.... Try smashing it!");
                }
                prose_package pp = prose.getPackage(new string_id("H'chu apenkee, Moulee-rah!"));
                commPlayer(self, target, pp, "object/mobile/jabba_the_hutt.iff");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer pumpkin " + flag + " with " + howMany + " pumpkins in a " + radius + "m radius.");
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer pumpkin " + flag + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("tree"))
        {
            String flag = tok.nextToken();
            if (flag.isEmpty())
            {
                broadcast(self, "/developer tree [single | ring]");
                return SCRIPT_CONTINUE;
            }
            if (flag.equalsIgnoreCase("single"))
            {
                obj_id tree = createObject("object/tangible/lifeday/tree.iff", getLocation(target));
                attachScript(tree, "event.lifeday.tree_chopping");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer tree " + flag + " on " + getName(target));

            }
            if (flag.equalsIgnoreCase("ring"))
            {
                location loc = getLocation(target);
                int howMany = stringToInt(tok.nextToken());
                int radius = stringToInt(tok.nextToken());
                if (howMany == 0 || radius == 0)
                {
                    broadcast(self, "/developer tree ring [num to spawn] [radius]");
                    return SCRIPT_CONTINUE;
                }
                if (!isIdValid(self) || !exists(self))
                {
                    return SCRIPT_CONTINUE;
                }
                float x;
                float z;
                for (int i = 0; i < howMany; i++)
                {
                    float angle = (float) (i * (360 / howMany));
                    x = loc.x + (float) Math.cos(angle) * radius;
                    z = loc.z + (float) Math.sin(angle) * radius;
                    obj_id tree = create.object("object/tangible/lifeday/tree.iff", new location(x, getHeightAtLocation(x, z), z, loc.area));
                    attachScript(tree, "event.lifeday.tree_chopping");
                    faceTo(tree, target);
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer tree " + flag + " with " + howMany + " trees in a " + radius + "m radius.");
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer tree " + flag + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("wiki"))
        {
            String speech = tok.nextToken();
            String wiki_link = "https://swg.fandom.com/wiki/" + speech;
            String pathed;
            pathed = wiki_link.replace(" ", "_");
            launchClientWebBrowser(self, pathed);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer wiki " + speech + "(" + pathed + ")");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("shuttleRebelDrop"))
        {
            String message = "Mayday! Mayday! Mayday! I have to drop my payload, " + getFirstName(target) + "!";
            prose_package commP = new prose_package();
            commP.stringId = new string_id(message);
            commPlayer(self, target, commP, "object/mobile/dressed_rebel_intel_officer_human_female_01.iff");
            obj_id[] players = getAllPlayers(getLocation(target), 10.0f);
            playClientEffectLoc(players, "appearance/rebel_transport_touch_and_go.prt", getLocation(target), 2.0f);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer shuttleRebelDrop on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("shuttleImperialDrop"))
        {
            String message = "Prepare for your cargo delivery, " + getFirstName(target) + "! I don't want any hiccups.";
            prose_package commP = new prose_package();
            commP.stringId = new string_id(message);
            commPlayer(self, target, commP, "object/mobile/dressed_imperial_officer_m_2.iff");
            obj_id[] players = getAllPlayers(getLocation(target), 10.0f);
            playClientEffectLoc(players, "appearance/imperial_transport_touch_and_go.prt", getLocation(target), 2.0f);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer shuttleImperialDrop on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("seedAllSchematics"))
        {
            obj_id inventory = getInventoryContainer(self);
            if (!isIdValid(inventory))
            {
                return SCRIPT_CONTINUE;
            }
            String schematicTable = "datatables/crafting/schematic_group.iff";
            String column = "SchematicName";
            obj_id myBag = createObjectInInventoryAllowOverload("object/tangible/test/qabag.iff", self);
            String[] items = dataTableGetStringColumnNoDefaults(schematicTable, column);
            int bagLimit = 0;
            for (String item : items)
            {
                //String description = "This item was created by " + getRandomHumanName(self) + " on " + getCalendarTimeStringGMT_YYYYMMDDHHMMSS(getCalendarTime()) + "\\#.";
                if (bagLimit > 500 && !hasScriptVar(self, "bagLimit"))
                {
                    broadcast(self, "Breached 500 items.");
                    setScriptVar(self, "bagLimit", 1);
                }
                obj_id madeItem = makeCraftedItem(item, 100f, myBag);
                setCrafter(madeItem, self);
                //setDescriptionStringId(madeItem, new string_id(description));
                bagLimit++;
            }
            broadcast(self, "Seeding " + items.length + " items with 100% quality.");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer seedAllSchematics");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("seedAllSchematicsByType"))
        {
            String type = "object/draft_schematic/" + tok.nextToken();
            obj_id inventory = getInventoryContainer(self);
            if (!isIdValid(inventory))
            {
                return SCRIPT_CONTINUE;
            }
            String schematicTable = "datatables/crafting/schematic_group.iff";
            String column = "SchematicName";
            obj_id myBag = createObjectInInventoryAllowOverload("object/tangible/test/qabag.iff", self);
            String[] items = dataTableGetStringColumnNoDefaults(schematicTable, column);
            int bagLimit = 0;
            for (String item : items)
            {
                if (item.startsWith(type)) // object/draft_schematic/[TYPE/subtype/etc/etc]
                {
                    String description = "This item was created by " + getRandomHumanName(self) + " on " + getCalendarTimeStringGMT_YYYYMMDDHHMMSS(getCalendarTime() - (rand(0, 106560))) + "\\#.";
                    if (bagLimit > 500 && !hasScriptVar(self, "bagLimit"))
                    {
                        broadcast(self, "Breached 500 items.");
                        setScriptVar(self, "bagLimit", 1);
                    }
                    obj_id madeItem = makeCraftedItem(item, 1000.0f, myBag);
                    setCraftedId(madeItem, myBag);
                    setCrafter(madeItem, self);
                    setCount(madeItem, 10 * 100);
                    attachScript(madeItem, "object.autostack");
                    broadcast(self, "Item " + item + " has been serialized.");
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer seedAllSchematicsByType | Adding " + item + " to bag.");
                    bagLimit++;
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer seedAllSchematicsByType | Generated " + bagLimit + " items from query " + type);
            }
            broadcast(self, "Seeding " + bagLimit + " items with 100% quality.");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer seedAllSchematicsByType");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("moveInCell"))
        {
            if (isInWorldCell(self))
            {
                broadcast(self, "You must be in a cell to use this command.");
                return SCRIPT_CONTINUE;
            }
            float x = stringToFloat(tok.nextToken());
            float y = stringToFloat(tok.nextToken());
            float z = stringToFloat(tok.nextToken());
            obj_id cell = getContainedBy(self);
            location loc = new location(x, y, z, getCurrentSceneName(), cell);
            warpPlayer(target, loc);
            broadcast(self, "Moved target to " + loc.toReadableFormat(false));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("noafk"))
        {
            String template = "object/tangible/spawning/spawn_egg.iff";
            obj_id marker = create.object(template, getLocation(self));
            attachScript(marker, "systems.antiafk.anti_afk_volume");
            broadcast(self, "Made Anti-AFK volume. To remove, delete the spawn egg.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("ballgame"))
        {
            obj_id myTarget = getIntendedTarget(self);
            obj_id pInv = getInventoryContainer(myTarget);
            obj_id hotPotato = createObject("object/tangible/loot/dungeon/geonosian_mad_bunker/relic_gbb_small_ball.iff", pInv, "");
            setName(hotPotato, "a throwable ball");
            attachScript(hotPotato, "developer.bubbajoe.pass_the_ball");
            detachScript(hotPotato, "object.autostack");
            String descUnloc = "This is a ball that can be thrown at other players. Use \"Throw Ball\" to throw it at a player.";
            setDescriptionStringId(hotPotato, new string_id(descUnloc));
            broadcast(myTarget, "You have been passed a ball! Use \"Throw Ball\" to throw it at a player.");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer ballgame on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("shell"))
        {
            if (tok.countTokens() <= 2)
            {
                broadcast(self, "/developer shell [/drive/location/] [command w/params]");
            }
            String where = tok.nextToken();
            String command = tok.nextToken();
            StringBuilder args = new StringBuilder();
            while (tok.hasMoreTokens())
            {
                args.append(tok.nextToken()).append(" ");
            }
            String fullCommand = command + " " + args;
            broadcast(self, "Running command: [/developer shell] " + where + " " + fullCommand);
            String outputString = system_process.runAndGetOutput(fullCommand, new File(where));
            int page = createSUIPage("/Script.messageBox", self, self);
            setSUIProperty(page, "Prompt.lblPrompt", "LocalText", outputString);
            setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
            setSUIProperty(page, "bg.caption.lblTitle", "Text", "Development Shell " + getClusterName());
            setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
            setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
            subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "handleShellOutput");
            setSUIProperty(page, "btnCancel", "Visible", "false");
            setSUIProperty(page, "btnRevert", "Visible", "false");
            showSUIPage(page);
            flushSUIPage(page);
            echo(self, "Command output sent to SUI. Press CTRL + C to copy to clipboard. (check keybinds)");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer shell " + where + " " + fullCommand);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("markObjects"))
        {
            obj_id[] nearbyObjects = getObjectsInRange(getLocation(self), 256.0f);
            for (obj_id nearbyObject : nearbyObjects)
            {
                if (!hasObjVar(nearbyObject, "buildout_utility.write"))
                {
                    setObjVar(nearbyObject, "buildout_utility.write", 1);
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer markObjects | Marked object " + getName(nearbyObject) + " for persisting.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getObjectsNear"))
        {
            float x = stringToFloat(tok.nextToken());
            float z = stringToFloat(tok.nextToken());
            location loc = new location(x, getHeightAtLocation(x, z), z, getCurrentSceneName());
            float range = stringToFloat(tok.nextToken());
            obj_id[] nearbyObjects = getObjectsInRange(loc, range);
            StringBuilder prompt = new StringBuilder();
            for (obj_id nearbyObject : nearbyObjects)
            {
                prompt.append("Object: ").append(nearbyObject).append(" | ").append(getName(nearbyObject)).append(" | ").append(getLocation(nearbyObject).toReadableFormat(false)).append("\n");
            }
            String completeList = prompt.toString();
            int page = createSUIPage("/Script.messageBox", self, self);
            setSUIProperty(page, "Prompt.lblPrompt", "LocalText", completeList);
            setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
            setSUIProperty(page, "bg.caption.lblTitle", "Text", "Object List " + getCurrentSceneName() + " at " + x + ", " + z + " within " + range + "m");
            setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
            setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
            subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "handleShellOutput");
            setSUIProperty(page, "btnCancel", "Visible", "false");
            setSUIProperty(page, "btnRevert", "Visible", "false");
            showSUIPage(page);
            flushSUIPage(page);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("makeAugs"))
        {
            String template = "object/tangible/component/weapon/new_weapon/enhancement_ranged_slot_one_s23.iff";
            obj_id augment = createObject(template, getLocation(self));
            attachScript(augment, "systems.crafting.weapon.component.crafting_weapon_component_attribute");
            setObjVar(augment, "attribute.bonus.0", 300);
            setObjVar(augment, "attribute.bonus.2", 300);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer makeAugs " + template + " with 300 for it's stats.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("scale"))
        {
            float original = getScale(target);
            broadcast(self, "Original Scale: " + original);
            setScale(target, stringToFloat(tok.nextToken()));
            broadcast(target, "You have been resized.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("path"))
        {
            //createClientPath(self, getLocation(self), getLocation(target));
            createClientPathAdvanced(self, getLocation(self), getLocation(target), "default");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("messageto"))
        {
            dictionary param = new dictionary();
            messageTo(target, tok.nextToken(), param, stringToFloat(tok.nextToken()), true);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer messageto " + target + " " + tok.nextToken() + " " + tok.nextToken());
            echo(self, "Message sent to " + target + " (" + getName(target) + ") with a delay of " + tok.nextToken() + " seconds.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("messagetoparams"))
        {
            // /developer messagetoparams (target) <message> <delay> <ensured> <param1> <value1> <param2> <value2> ...
            dictionary param = new dictionary();
            String message = tok.nextToken();
            float delay = stringToFloat(tok.nextToken());
            boolean ensured = Boolean.parseBoolean(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                String key = tok.nextToken();
                String value = tok.nextToken();
                param.put(key, value);
            }
            messageTo(target, message, param, delay, ensured);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getCRC"))
        {
            String hash = tok.nextToken();
            int hashValue = getStringCrc(hash);
            broadcast(self, "Hash Value: " + hashValue);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer getCRC " + hash + " with a value of " + hashValue);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("heatMap"))
        {
            location loc = getLocation(self);
            loc.x = 0.0f;
            loc.y = 0.0f;
            loc.z = 0.0f;
            loc.area = getCurrentSceneName();
            String template = tok.nextToken();
            String fileName = tok.nextToken();
            obj_id[] plotters = getAllObjectsWithTemplate(getLocation(self), 7250.0f, template);
            generateHeatmap(self, plotters, fileName + "_" + loc.area);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("travel"))
        {
            String which = tok.nextToken();
            if (which.equals("add"))
            {
                int cost = stringToInt((tok.nextToken()));
                String pointName = tok.nextToken();
                if (tok.hasMoreTokens())
                {
                    pointName += " " + tok.nextToken();
                }
                location loc = getLocation(target);

                if (cost == 0)
                {
                    cost = 125;
                }
                if (pointName == null || pointName.isEmpty())
                {
                    broadcast(self, "You must specify a point name.");
                    return SCRIPT_CONTINUE;
                }
                if (loc == null)
                {
                    broadcast(self, "You must specify a valid target.");
                    return SCRIPT_CONTINUE;
                }
                setObjVar(self, "temp_shuttle", pointName);
                debugConsoleMsg(self, "Shuttle point " + pointName + " added at " + loc.x + ", " + loc.y + ", " + loc.z + " for " + cost + " credits.");
                addPlanetTravelPoint(getCurrentSceneName(), pointName, getLocation(self), cost, true, TPT_NPC_Starport);
                return SCRIPT_CONTINUE;
            }
            if (which.equals("remove"))
            {
                String pointName = tok.nextToken();
                if (tok.hasMoreTokens())
                {
                    pointName += " " + tok.nextToken();
                }
                if (pointName == null || pointName.isEmpty())
                {
                    broadcast(self, "You must specify a point name. It must be exact.");
                    return SCRIPT_CONTINUE;
                }
                removePlanetTravelPoint(getCurrentSceneName(), pointName);
            }
            else
            {
                broadcast(self, "Invalid Syntax: /developer travel add [COST] [Epic Name With Spaces]");
                return SCRIPT_CONTINUE;
            }
        }
        else if (cmd.equalsIgnoreCase("removespecstamp"))
        {
            obj_id city_hall = getIntendedTarget(self);
            String VAR_CITY = "spec_stamp";
            String VAR_CITY_OLD = VAR_CITY + ".old";
            if (hasObjVar(city_hall, VAR_CITY))
            {
                setObjVar(city_hall, VAR_CITY_OLD, getIntObjVar(city_hall, VAR_CITY));
                removeObjVar(city_hall, VAR_CITY);
                broadcast(self, "Removed specstamp from City Hall with preservation of old specstamp.");
            }
            else
            {
                broadcast(self, "This target does not have the \"spec_stamp\" objvar.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("animate"))
        {
            String animationFile = tok.nextToken();
            //all_b_[animationFile] ?
            doAnimationAction(target, animationFile);
            broadcast(self, "Animation '" + animationFile + "' performed on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("renameContainerContents"))
        {
            StringBuilder name = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                name.append(" ").append(tok.nextToken());
            }
            obj_id[] contents = getContents(target);
            for (obj_id content : contents)
            {
                setName(content, name.toString());
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("tagContainerContents"))
        {
            StringBuilder tag = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                tag.append(" ").append(tok.nextToken());
            }
            obj_id[] contents = getContents(target);
            for (obj_id content : contents)
            {
                String craftedName = getName(content);
                String originalName = getStringName(content);
                if (!isCrafted(content))
                {
                    setName(content, originalName + " (" + tag + ")");
                }
                else
                {
                    setName(content, craftedName + " (" + tag + ")");
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("unlockContainer"))
        {
            obj_id[] contents = getContents(target);
            for (obj_id content : contents)
            {
                removeObjVar(content, "noTrade");
                detachScript(content, "item.special.nomove");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("lockContainer"))
        {
            obj_id[] contents = getContents(target);
            for (obj_id content : contents)
            {
                setObjVar(content, "noTrade", 1);
                attachScript(content, "item.special.nomove");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("touchContainer"))
        {
            String tag = " (Developer Item)";
            obj_id[] contents = getContents(target);
            for (obj_id content : contents)
            {
                String oldName = getStringName(content);
                setName(content, oldName + tag);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("revertContainerContents"))
        {
            obj_id[] contents = getContents(target);
            for (obj_id content : contents)
            {
                setName(content, getTemplateName(content));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("grantAllSchematics"))
        {
            String SCHEMATIC_TABLE = "datatables/crafting/schematic_group.iff";
            String SCHEMATIC_TABLE_COLUMN = "GroupId";
            String[] schematicGroups = dataTableGetStringColumnNoDefaults(SCHEMATIC_TABLE, SCHEMATIC_TABLE_COLUMN);
            for (String schematicGroup : schematicGroups)
            {
                grantSchematicGroup(target, schematicGroup);
                broadcast(self, "Granted schematic group " + schematicGroup + " to " + getName(target));
            }
            broadcast(self, "Granted all schematic groups to " + getName(target));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer grantAllSchematics on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("grantAllSchematicsByGroup"))
        {
            String group = tok.nextToken();
            grantSchematicGroup(target, group);
            broadcast(self, "Granted schematic group " + group + " to " + getName(target));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer grantAllSchematics on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("grantAllItems"))
        {
            String ITEM_TABLE = "datatables/item/master_item/master_item.iff";
            String ITEM_TABLE_COLUMN = "name";
            obj_id myBag = createObjectInInventoryAllowOverload("object/tangible/test/qabag.iff", self);
            String[] items = dataTableGetStringColumnNoDefaults(ITEM_TABLE, ITEM_TABLE_COLUMN);
            int bagLimit = 80;
            for (String item : items)
            {
                if (bagLimit > getContents(myBag).length)
                {
                    broadcast(self, "Bag limit reached.  Stopping!");
                    break;
                }
                static_item.createNewItemFunction(item, myBag);
                broadcast(self, "Granted item " + item + " to " + getName(target));
                bagLimit++;
            }
            broadcast(self, "Granted all items to " + getName(target));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer grantAllItems on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("compileScripts") || cmd.equalsIgnoreCase("cs"))
        {
            final String result = system_process.runAndGetOutput("ant compile_java", new File("../../"));
            if (result.contains("BUILD SUCCESSFUL"))
            {
                broadcast(self, "compileScripts: ant compile_java BUILD SUCCESSFUL.");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer compileScripts with success");
            }
            else
            {
                broadcast(self, "compileScripts: ERROR or BUILD FAILED. Sending output to console.");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer compileScripts with failure");
                sendConsoleMessage(self, result);
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer compileScripts");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("compileAndReloadScript") || cmd.equalsIgnoreCase("crs"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer crs <script>");
            }
            else
            {
                final String script = tok.nextToken();
                final String result = system_process.runAndGetOutput("ant compile_java", new File("../../"));
                if (result.contains("BUILD SUCCESSFUL"))
                {
                    broadcast(self, "compileAndReloadScript: ant compile_java BUILD SUCCESSFUL. Reloading " + script + "...");
                    sendConsoleCommand("/script reload " + script, self);
                }
                else
                {
                    broadcast(self, "compileAndReloadScript: ERROR or BUILD FAILED. Sending output to console.");
                    sendConsoleMessage(self, result);
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer compileAndReloadScript " + script);
            }
            return SCRIPT_CONTINUE;
        }
        /*else if (cmd.equalsIgnoreCase("git"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer git <command>");
                broadcast(self, "Command options: pull");
            }
            else
            {
                final String arg = tok.nextToken();
                if (arg.equals("pull"))
                {
                    final String result = system_process.runAndGetOutput("git pull origin development", new File("/home/swg/swg-main/dsrc/"));
                    if (result.contains("Already up to date."))
                    {
                        broadcast(self, "Repo 'dsrc' is already up to date.");
                    }
                    else
                    {
                        sendConsoleMessage(self, result);
                        broadcast(self, "Repo 'dsrc' has been updated. Ready for /developer crs [script]");
                    }
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer git " + arg);
            }
            return SCRIPT_CONTINUE;
        }*/

        else if (cmd.equalsIgnoreCase("git"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer git <command>");
                broadcast(self, "Command options: pull");
            }
            else
            {
                final String arg = tok.nextToken();
                if (arg.equals("pull"))
                {
                    broadcast(self, "Please Wait... Pulling and Compiling.");
                    // 1. Run git pull to update files
                    final String result = system_process.runAndGetOutput("git pull origin main", new File("/home/swg/swg-main/dsrc/"));
                    if (result.contains("Already up to date."))
                    {
                        broadcast(self, "Repo 'dsrc' is already up to date.");
                    }
                    else
                    {
                        sendConsoleMessage(self, result);
                        broadcast(self, "Repo 'dsrc' has been updated.");

                        // 2. Run ant compile_java to compile the code
                        String compileResult = system_process.runAndGetOutput("ant compile_java", new File("/home/swg/swg-main/"));
                        if (compileResult.contains("BUILD SUCCESSFUL"))
                        {
                            sendConsoleMessage(self, "BUILD SUCCESSFUL");
                        }
                        else
                        {
                            broadcast(self, "Build cancelled!");
                            return SCRIPT_CONTINUE;
                        }

                        // 3. Get the list of changed files since the last commit using git diff
                        String diffResult = system_process.runAndGetOutput("git diff --name-only HEAD~1", new File("/home/swg/swg-main/dsrc/"));

                        // Parse the diff result to get modified .java files
                        Set<String> updatedFiles = new HashSet<>();
                        String[] diffLines = diffResult.split("\n");

                        for (String line : diffLines)
                        {
                            // Check if the line refers to a .java file (either modified or added)
                            if (line.endsWith(".java"))
                            {
                                String fileName = line.trim();  // Get the file name from the diff output

                                // Chop off the "sku.0/sys.server/compiled/game/script/" part
                                fileName = fileName.replace("sku.0/sys.server/compiled/game/script/", "");

                                // Replace slashes with periods and remove the ".java" extension
                                fileName = fileName.replace("/", ".").replace(".java", "");

                                updatedFiles.add(fileName);
                            }
                        }

                        // 4. Prepare the message to display the status of each file
                        StringBuilder statusMessage = new StringBuilder();
                        for (String className : updatedFiles)
                        {
                            boolean reloadSuccess = reloadScript(className);

                            String status = reloadSuccess ? "MODIFIED | \\#11d91fCOMPILED & RELOADED\\#." : "MODIFIED | \\#bf0420COMPILATION FAILURE\\#.";
                            statusMessage.append(className).append(" (").append(status).append(")\n");
                        }

                        // 5. Show the status message in a SUI msgbox
                        msgbox(self, statusMessage.toString());
                    }
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer git " + arg);
            }
            return SCRIPT_CONTINUE;
        }


        else if (cmd.equalsIgnoreCase("magicSatchel"))
        {
            obj_id satchel = create.createObject("object/tangible/container/general/satchel.iff", getInventoryContainer(self), "");
            attachScript(satchel, "developer.bubbajoe.magic_satchel");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer magicSatchel");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("restrictArea"))
        {
            if (tok.countTokens() != 2)
            {
                broadcast(self, "[SYNTAX] /developer restrictArea [Float: radius] [String: volume name suffix, must be in 'lowercase_underscore_format']");
            }
            String radius = tok.nextToken();
            String name = tok.nextToken();
            obj_id spawnEgg = createObject("object/tangible/spawning/spawn_egg.iff", getLocation(self));
            setObjVar(spawnEgg, "expel_radius", stringToFloat(radius));
            setObjVar(spawnEgg, "volume_suffix", name);
            setObjVar(spawnEgg, "creator", getFirstName(self));
            attachScript(spawnEgg, "developer.bubbajoe.expel");
            broadcast(self, "You have restricted this area. To free this area restriction, target the spawn egg and run /developer unrestrictArea");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer restrictArea " + radius + " " + name + " at " + getLocation(self).toReadableFormat(true));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("decorate"))
        {
            //setupIncrementInputBox(self);
            startDecorationPanel(self);
        }
        else if (cmd.equalsIgnoreCase("workbench"))
        {
            //setupIncrementInputBox(self);
            startWorkbench(self);
        }
        else if (cmd.equalsIgnoreCase("adminPanel"))
        {
            startAdminPanel(self);
        }
        else if (cmd.equalsIgnoreCase("areaSpawner"))
        {
            startAreaSpawner(self);
        }
        else if (cmd.equalsIgnoreCase("lookupPlayer"))
        {
            startPlayerInfo(self);
        }
        else if (cmd.equalsIgnoreCase("deployable"))
        {
            String flag = tok.nextToken();
            if (flag.equals("create"))
            {
                String template = tok.nextToken();
                String script = tok.nextToken();
                int stack = stringToInt(tok.nextToken());
                //must be GOT_
                obj_id deployableToken = createObject("object/tangible/terminal/terminal_command_console.iff", getInventoryContainer(self), "");
                attachScript(deployableToken, "developer.bubbajoe.deployable");
                setObjVar(deployableToken, "deployable.item", template);
                setObjVar(deployableToken, "deployable.script", script);
                setCount(deployableToken, stack);
                setName(deployableToken, "Deployable");
                broadcast(self, "Deployable made and configured.");
                LOG("ethereal", "[Deployable]: " + getPlayerFullName(self) + "attempted to created a deployable with a template of " + template + ", script of " + script + ", and a stack of " + stack);
            }
            else
            {
                broadcast(self, "/developer deployable [create (template) (script) (stack size)]");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("give"))
        {
            String flag = tok.nextToken();
            if (flag.equals("credits"))
            {
                String nextFlag = tok.nextToken();
                if (nextFlag.equals("add"))
                {
                    money.deposit(self, STIPEND);
                    LOG("ethereal", "[Developer]:" + getPlayerFullName(self) + " gave themselves " + STIPEND + " credits");
                }
                else if (nextFlag.equals("remove"))
                {
                    int amount = stringToInt(tok.nextToken());
                    if (transferBankCreditsToNamedAccount(self, money.ACCT_BETA_TEST, amount, "noHandler", "noHandler", new dictionary()))
                    {
                        broadcast(self, amount + " credits removed.");
                        return SCRIPT_CONTINUE;
                    }
                    else
                    {
                        broadcast(self, "Failed to remove some or all credits from player.");
                    }
                }
                else
                {
                    broadcast(self, "/developer give credits [add | remove {amount}]");
                    return SCRIPT_CONTINUE;
                }
            }
            else if (flag.equals("gear"))
            {
                broadcast(self, "Gear granted.");
            }
            else
            {
                broadcast(self, "/developer give [credits | gear] ");
            }
        }
        else if (cmd.equalsIgnoreCase("unrestrictArea"))
        {
            obj_id whatVolumeEgg = getTarget(self);
            if (hasScript(whatVolumeEgg, "developer.bubbajoe.expel") && hasObjVar(whatVolumeEgg, "expel_radius"))
            {
                broadcast(self, "Destroying expel volume");
                destroyObject(whatVolumeEgg);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("persistArea"))
        {
            obj_id[] designerObjects = getObjectsInRange(getLocation(self), stringToFloat(tok.nextToken()));
            for (obj_id individualItem : designerObjects)
            {
                if (!isPlayer(individualItem) && !isMob(individualItem) && isIdValid(individualItem))
                {
                    setObjVar(individualItem, "nb_content_marker", true);
                    persistObject(individualItem);
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " persisted " + getTemplateName(individualItem) + " at " + getLocation(individualItem).toReadableFormat(true));
                }
                else
                {
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " tried to persist " + getTemplateName(individualItem) + " at " + getLocation(individualItem).toReadableFormat(true) + " but it was not persisted because it was a player or mob.");
                }
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer persistArea " + getLocation(self).toReadableFormat(true) + " | " + designerObjects.length + " objects persisted.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("reload"))
        {
            reloadScript("developer.bubbajoe.player_developer");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer reload");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("buffAllByName"))
        {
            String query = tok.nextToken();
            String BUFF_TABLE = "datatables/buff/buff.iff";
            String BUFF_TABLE_COLUMN = "NAME";
            String[] buffs = dataTableGetStringColumnNoDefaults(BUFF_TABLE, BUFF_TABLE_COLUMN);
            for (String buffName : buffs)
            {
                if (buffName.contains(query))
                {
                    if (buff.isDebuff(buffName))
                    {
                        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " skipping " + buffName + ", it is a debuff.");
                        return SCRIPT_CONTINUE;
                    }
                    else
                    {
                        buff.applyBuff(target, buffName);
                    }
                    broadcast(self, "Applied " + buffName + " to " + getName(target));
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " Applying " + buffName + " on " + getName(target));
                }
            }
            broadcast(self, "Granted all buffs that matched query to " + getName(target));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer buffAllByName " + query + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("grantAllSkills"))
        {
            ArrayList<String> badSkills = new ArrayList<>();
            badSkills.add("combat_melee_basic");
            badSkills.add("combat_ranged_weapons_basic");
            badSkills.add("demo_combat");
            badSkills.add("common_knowledge");
            badSkills.add("utility");
            badSkills.add("utility_beta");
            badSkills.add("utility_beta_demo_combat");
            badSkills.add("utility_beta_demo_combat");
            badSkills.add("utility_player");
            badSkills.add("swg_dev");
            badSkills.add("swg_cs");
            String query = tok.nextToken();
            String SKILL_TABLE = "datatables/skill/skills.iff";
            String SKILL_TABLE_COLUMN = "NAME";
            String[] skills = dataTableGetStringColumnNoDefaults(SKILL_TABLE, SKILL_TABLE_COLUMN);
            for (String skill : skills)
            {
                if (skill.contains(query))
                {
                    if (badSkills.contains(skill))
                    {
                        broadcast(self, "Skipping " + skill + ", bad skill.");
                        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used grantAllSkills and tried to grant bad skill so it was denied.");
                        continue;
                    }
                    grantSkill(target, skill);
                }
            }
            broadcast(self, "Granted all skills from search to " + getName(target));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer grantAllSkills " + query + " on " + getName(target));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("grantAllItemsBySearch"))
        {
            String query = tok.nextToken();
            String ITEM_TABLE = "datatables/item/master_item/master_item.iff";
            String ITEM_TABLE_COLUMN = "name";
            obj_id myBag = createObjectInInventoryAllowOverload("object/tangible/test/qabag.iff", self);
            String[] items = dataTableGetStringColumnNoDefaults(ITEM_TABLE, ITEM_TABLE_COLUMN);
            for (String item : items)
            {
                if (item.contains(query))
                {
                    static_item.createNewItemFunction(item, myBag);
                    LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer grantAllItemsBySearch " + query + " on " + getName(self) + " and gave them " + item);
                }
                else
                {
                    //LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer grantAllItemsBySearch " + query + " to " + getName(target) + " but " + item + " did not match the search parameter, so it was skipped.");
                    debugConsoleMsg(self, "Skipping item " + item);
                }
            }
            setName(myBag, "QA Backpack of '" + query + "'");
            broadcast(self, "Granted all items containing \"" + query + "\" to " + getPlayerFullName(self));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("listStaticContents"))
        {
            obj_id[] contents = getContents(target);
            StringBuilder prompt = new StringBuilder("Contents of " + getName(target) + " are: \n");
            for (obj_id content : contents)
            {
                String itemName = getStaticItemName(content);
                if (!itemName.equals("null"))
                {
                    prompt.append(itemName).append("\n");
                }
            }
            int page = createSUIPage("/Script.messageBox", self, self);
            setSUIProperty(page, "Prompt.lblPrompt", "LocalText", prompt.toString());
            setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
            setSUIProperty(page, "bg.caption.lblTitle", "Text", "Static Item Contents");
            setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
            setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
            setSUIProperty(page, "btnCancel", "Visible", "false");
            setSUIProperty(page, "btnRevert", "Visible", "false");
            showSUIPage(page);
            flushSUIPage(page);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("flytext"))
        {
            StringBuilder flytext = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                flytext.append(" ").append(tok.nextToken());
            }
            showFlyText(target, unlocalized(flytext.toString()), 2.0f, colors.WHITE);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("flytextTarget"))
        {
            StringBuilder flytext = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                flytext.append(" ").append(tok.nextToken());
            }
            showFlyText(target, unlocalized(flytext.toString()), 35.0f, colors.WHITE);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("findClosestPumpkin"))
        {
            location here = getLocation(self);
            obj_id[] pumpkins = getAllObjectsWithTemplate(getLocation(self), 1000.0f, "object/tangible/holiday/halloween/pumpkin_object.iff");
            float closestDistance = 1000.0f;
            obj_id closestPumpkin = null;
            for (obj_id pumpkin : pumpkins)
            {
                float distance = getDistance(here, getLocation(pumpkin));
                if (distance < closestDistance)
                {
                    closestDistance = distance;
                    closestPumpkin = pumpkin;
                }
            }
            warpPlayer(self, getLocation(closestPumpkin));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer findClosestPumpkin, warping to " + getLocation(closestPumpkin).toReadableFormat(true));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("junkSpawner"))
        {
            createJunkTokenClicky(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("findJunk"))
        {
            obj_id[] junkObjects = getAllObjectsWithScript(getLocation(self), 4192f, "event.gjpud.junk");
            if (junkObjects.length == 0)
            {
                broadcast(self, "No junk found within 4192 meters.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                warpPlayer(self, getLocation(junkObjects[0]));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("findClosestPlayer"))
        {
            location here = getLocation(self);
            float range = stringToFloat(tok.nextToken());
            if (tok.countTokens() != 1)
            {
                broadcast(self, "Syntax: /developer findClosestPlayer [range]");
                return SCRIPT_CONTINUE;
            }
            obj_id[] players = getPlayerCreaturesInRange(here, range);
            for (obj_id player : players)
            {
                if (player == self)
                {
                    continue;
                }
                if (!isInWorldCell(player))
                {
                    continue;
                }
                warpPlayer(self, getLocation(player));
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer findClosestPlayer, warping to " + getName(player) + " at " + getLocation(player).toReadableFormat(true));
                return SCRIPT_CONTINUE;
            }
        }
        else if (cmd.equalsIgnoreCase("toggle"))
        {
            String toggle = tok.nextToken();
            switch (toggle)
            {
                case "on":
                    sendConsoleCommand("/object setCoverVisibility " + self + " 1", self);
                    sendConsoleCommand("/object hide " + self + " 1", self);
                    sendConsoleCommand("/echo You are invisible.", self);
                    break;
                case "off":
                    sendConsoleCommand("/object setCoverVisibility " + self + " 0", self);
                    sendConsoleCommand("/object hide " + self + " 0", self);
                    sendConsoleCommand("/echo You are visible.", self);
                    break;
                default:
                    sendConsoleCommand("Usage: /developer toggle [on|off]", self);
                    break;
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("players"))
        {
            getPlayers(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("editWeapon"))
        {
            obj_id weapon = utils.getHeldWeapon(target);
            if (weapon == null)
            {
                return SCRIPT_CONTINUE;
            }
            String mod = tok.nextToken();
            if (mod == null)
            {
                broadcast(self, "No mod specified. See console for valid mods.");
                debugConsoleMsg(self, "/developer editWeapon <mod> <value>");
                debugConsoleMsg(self, "List of valid mods:\nminDamage\nmaxDamage\nattackSpeed\nwoundChance\nattackCost\naccuracy\nelementalType\nelementalValue\nrangeInfo\nresetAllStats\ndamageRadius");
                return SCRIPT_CONTINUE;
            }
            switch (mod)
            {
                case "minDamage":
                    setWeaponMinDamage(weapon, stringToInt(tok.nextToken()));
                    break;
                case "maxDamage":
                    setWeaponMaxDamage(weapon, stringToInt(tok.nextToken()));
                    break;
                case "attackSpeed":
                    setWeaponAttackSpeed(weapon, stringToFloat(tok.nextToken()));
                    break;
                case "woundChance":
                    setWeaponWoundChance(weapon, stringToFloat(tok.nextToken()));
                    break;
                case "attackCost":
                    setWeaponAttackCost(weapon, stringToInt(tok.nextToken()));
                    break;
                case "accuracy":
                    setWeaponAccuracy(weapon, stringToInt(tok.nextToken()));
                    break;
                case "elementalType":
                    setWeaponElementalType(weapon, stringToInt(tok.nextToken()));
                    break;
                case "elementalValue":
                    setWeaponElementalValue(weapon, stringToInt(tok.nextToken()));
                    break;
                case "rangeInfo":
                    setWeaponRangeInfo(weapon, stringToFloat(tok.nextToken()), stringToFloat(tok.nextToken()));
                    break;
                case "damageType":
                    setWeaponDamageType(weapon, stringToInt(tok.nextToken()));
                    break;
                case "damageRadius":
                    setWeaponDamageRadius(weapon, stringToFloat(tok.nextToken()));
                    break;
                case "resetAllStats":
                    setWeaponMinDamage(weapon, 24);
                    setWeaponMaxDamage(weapon, 64);
                    setWeaponAttackSpeed(weapon, 1.05f);
                    setWeaponWoundChance(weapon, 0f);
                    setWeaponAttackCost(weapon, 2);
                    setWeaponElementalValue(weapon, 0);
                    setWeaponElementalType(weapon, 0);
                    setWeaponAccuracy(weapon, 0);
                    setWeaponDamageRadius(weapon, 64.0f);
                    setWeaponDamageType(weapon, DAMAGE_KINETIC);
                    setWeaponRangeInfo(weapon, 0f, 64f);
                    setWeaponDamageRadius(weapon, 0f);
                    break;
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("url"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer openlink url");
            }
            else
            {
                String url = tok.nextToken();
                obj_id iTar = getIntendedTarget(self);
                launchClientWebBrowser(iTar, url);
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer url " + url + " on " + getName(iTar));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("pathToTargetPlanet"))
        {
            obj_id targetId = getIntendedTarget(self);
            obj_id[] players = getPlayerCreaturesInRange(getLocation(targetId), 16000f);
            for (obj_id player : players)
            {
                createClientPathAdvanced(player, getLocation(player), getLocation(targetId), "default");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("gjpud"))
        {
            String flag = tok.nextToken();
            if (flag.equals("workbench"))
            {
                obj_id workbench = createObject("object/tangible/furniture/all/outbreak_science_desk.iff", getLocation(self));
                attachScript(workbench, "event.gjpud.recycler");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("awardBirthday"))
        {
            obj_id slice = create.createObject("object/tangible/food/crafted/dessert_air_cake.iff", getInventoryContainer(target), "");
            setName(slice, "Slice of Birthday Cake");
            setDescriptionStringId(slice, new string_id("Cut from the most beautiful cake Master Abbub has ever made, this tasty slice will make you feel all cozy inside."));
            attachScript(slice, "developer.bubbajoe.bday_gift");
            broadcast(target, "Happy Birthday from SWG-OR!");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer awardBirthday on " + getName(target));
            return SCRIPT_CONTINUE;

        }
        else if (cmd.equals("tableView"))
        {
            //TODO: add row limit and frame index based on 10's
            String DATATABLE_MASTER_ITEM = "datatables/item/master_item/master_item.iff";
            final String[] columnHeader = {
                    "name",
                    "string_name",
                    "string_detail"
            };
            final String[] columnHeaderType = {
                    "text",
                    "text",
                    "text"
            };
            int numRows = dataTableGetNumRows(DATATABLE_MASTER_ITEM);
            int numColumns = dataTableGetNumColumns(DATATABLE_MASTER_ITEM);
            final String[][] columnData = new String[numRows][numColumns];
            for (int i = 0; i < numRows; i++)
            {
                broadcast(self, String.valueOf(numRows));
                for (int j = 0; j < numColumns; j++)
                {
                    broadcast(self, String.valueOf(numColumns));
                    String columnName = (columnHeader[j]);
                    columnData[i][j] = dataTableGetString(DATATABLE_MASTER_ITEM, i, columnName);
                }
            }
            tableColumnMajor(self, self, OK_CANCEL, "DATATABLE VIEWER", "noHandler", "Master Item Table Loaded", columnHeader, columnHeaderType, columnData, true);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playeffect"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playeffect <effect name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String effect = tok.nextToken();
                playClientEffectObj(self, effect, self, "");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playeffecttarget"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playeffecttarget <effect name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String effect = tok.nextToken();
                playClientEffectObj(target, effect, target, "");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playeffectloc"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playeffectloc <effect name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String effect = tok.nextToken();
                playClientEffectLoc(self, effect, getLocation(self), 0.0f);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playeffectloctarget"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playeffectloctarget <effect name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String effect = tok.nextToken();
                playClientEffectLoc(target, effect, getLocation(target), 0.0f);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playeffectatloc"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playeffectatloc <effect name> <x> <y> <z>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String effect = tok.nextToken();
                float x = stringToFloat(tok.nextToken());
                float y = stringToFloat(tok.nextToken());
                float z = stringToFloat(tok.nextToken());
                location loc = new location(x, y, z);
                playClientEffectLoc(self, effect, loc, 0.0f);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playsound"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playsound <sound name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String sound = tok.nextToken();
                playClientEffectObj(self, sound, self, "");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playsoundtarget"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playsoundtarget <sound name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String sound = tok.nextToken();
                playClientEffectObj(iTarget, sound, iTarget, "");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("smite"))
        {
            String EFFECT = "appearance/must_lightning_3.prt";
            String SOUNDEFFECT = "sound/wtr_lightning_strike.snd";
            obj_id[] players = getAllPlayers(getLocation(target), 2000.0f);
            playClientEffectLoc(players, EFFECT, getLocation(target), 0.0f);
            playClientEffectLoc(players, SOUNDEFFECT, getLocation(target), 0.0f);
            if (!isPlayer(target) && isMob(target))
            {
                damage(target, DAMAGE_ELEMENTAL_ELECTRICAL, HIT_LOCATION_BODY, 1000000);
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer smite on " + getName(target) + " at " + getLocation(target) + " for 1000000 electrical damage.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("staticItemStack"))
        {
            startInputAmountBox(self);
        }
        else if (cmd.equalsIgnoreCase("mortar"))
        {
            String EFFECT = "clienteffect/restuss_event_big_explosion.cef";
            String SOUNDEFFECT = "clienteffect/ion_fire.cef";
            obj_id[] players = getAllPlayers(getLocation(target), 2000.0f);
            playClientEffectLoc(players, EFFECT, getLocation(target), 0.0f);
            playClientEffectLoc(players, SOUNDEFFECT, getLocation(target), 0.0f);
            if (!isPlayer(target) && isMob(target))
            {
                damage(target, DAMAGE_KINETIC, HIT_LOCATION_BODY, 1000000);
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer mortar on " + getName(target) + " at " + getLocation(target) + " for 1000000 kinetic damage.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playsoundloc"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playsoundloc <sound name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String sound = tok.nextToken();
                playClientEffectLoc(self, sound, getLocation(self), 0.0f);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("shazam"))
        {
            obj_id source = getTarget(self);
            String sourceName = getEncodedName(source);
            String template = getTemplateName(source);
            obj_id object = createObject(template, getLocation(self));
            setName(object, sourceName);
            if (template == null)
            {
                return SCRIPT_CONTINUE;
            }
            obj_var_list vars = getObjVarList(source, "");
            if (vars == null)
            {
                return SCRIPT_CONTINUE;
            }
            int numItems = vars.getNumItems();
            if (numItems == 0)
            {
                broadcast(self, "No objvars found on " + getName(source) + " to copy.");
            }
            else
            {
                for (int i = 0; i < numItems; i++)
                {
                    obj_var ov = vars.getObjVar(i);
                    assert ov != null;
                    String name = ov.getName();
                    setObjVar(object, name, ov.getData());
                    broadcast(self, "Setting objvar " + name + " to " + ov.getData() + " on " + getName(object));
                }
            }
            String[] scripts = getScriptList(source);
            for (String script : scripts)
            {
                broadcast(self, "Attaching script " + script + " to " + getName(object));
                attachScript(object, script);
            }
            broadcast(self, "Shazam! " + getName(object) + " cloned from " + getName(source));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playsoundeveryone"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playsoundeveryone <sound name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                obj_id[] players = getAllPlayers(getLocation(self), 8000.0f);
                for (obj_id player : players)
                {
                    String sound = tok.nextToken();
                    playClientEffectObj(player, sound, player, "");
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playcefeveryone"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playcefeveryone <sound name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                obj_id[] players = getAllPlayers(getLocation(self), 8000.0f);
                for (obj_id player : players)
                {
                    String sound = tok.nextToken();
                    playClientEffectObj(player, sound, player, "head");
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("time"))
        {
            String currentDate = date.getFullDate(self, "en_US");
            String x = " or ";
            String acrossThePondDate = date.getFullDate(self, "en_GB");
            broadcast(self, "Current date is " + currentDate + x + acrossThePondDate);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("stopmacros"))
        {
            if (tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer stopmacros (target)");
                return SCRIPT_CONTINUE;
            }
            else
            {
                broadcast(self, "Stopping all macros on " + getName(target));
                sendConsoleCommand("/dumpPausedCommands", target);
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer stopmacros on " + getName(target));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("rewardArea"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer rewardArea <item> <count>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String item = tok.nextToken();
                int count = Integer.parseInt(tok.nextToken());
                obj_id[] players = getAllPlayers(getLocation(self), 250.0f);
                for (obj_id player : players)
                {
                    obj_id pInv = getInventoryContainer(player);
                    obj_id pItem = static_item.createNewItemFunction(item, pInv, count);
                    if (isIdValid(pItem))
                    {
                        dictionary itemData = static_item.getMasterItemDictionary(pItem);
                        String properName = itemData.getString("string_name");
                        if (properName == null)
                        {
                            return SCRIPT_CONTINUE;
                        }
                        broadcast(player, "You have been awarded " + color("EEAB19", String.valueOf(count)) + " " + color("EEAB19", properName) + " by the Event Team!");
                        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer rewardArea " + item + " " + count);
                    }
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sendFeed"))
        {
            //@Note: Remember, HTML color coding is not supported on Discord, please strip all colors when sending feed.
            inputbox(self, self, "Enter the message you with to send. \nCurrent channel: " + "live-events", OK_CANCEL, "Send Feed to:" + "live-events", INPUT_NORMAL, null, "sendFeedToDiscord", null);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("editlootarea"))
        {
            float radius = stringToFloat(tok.nextToken());
            int count = Integer.parseInt(tok.nextToken());
            obj_id[] creatures = getCreaturesInRange(getLocation(self), radius);
            for (obj_id creature : creatures)
            {
                if (isMob(creature))
                {
                    if (hasObjVar(self, "loot.numItems"))
                    {
                        setObjVar(creature, "loot.numItems", count);
                    }
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("createCommandTriggerVolume"))
        {
            StringBuilder commandToSave = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                commandToSave.append(" ").append(tok.nextToken());
            }
            String spawnEgg = "object/tangible/spawning/spawn_egg.iff";
            obj_id spawnEggId = createObject(spawnEgg, getLocation(self));
            setName(spawnEggId, "Command Volume: " + commandToSave);
            setObjVar(spawnEggId, "commandToExecute", commandToSave.toString());
            attachScript(spawnEggId, "developer.bubbajoe.trigger_command");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer createCommandTriggerVolume " + commandToSave + " at " + getLocation(self).toReadableFormat(true));
            return SCRIPT_CONTINUE;
        }
        /*else if (cmd.equalsIgnoreCase("formation")) //@TODO: find the proper way to iter through the mobs and assign them to the formation
        {
            String flag = tok.nextToken();
            if (flag.equals("create"))
            {
                String formation = tok.nextToken();
                String mobType = tok.nextToken();
                int count = Integer.parseInt(tok.nextToken());
                obj_id[] mobs = new obj_id{0};
                for (int i = 0; i < count; i++)
                {
                    obj_id mob = create.object(mobType, getLocation(self));
                    mobs[i] = mob;
                }
                for (obj_id mob : mobs)
                {
                    switch (formation)
                    {
                        case "box":
                            ai_lib.followInBoxFormation(mob , self, --count);
                            break;
                        case "column":
                            ai_lib.followInColumnFormation();
                            break;
                        case "line":
                            ai_lib.followInLineFormation();
                            break;
                        case "wedge":
                            ai_lib.followInWedgeFormation(mob, mobs, count);
                            break;
                        default:
                            broadcast(self, "Invalid formation type.");
                            break;
                    }
                }
            }
            else
            {
                broadcast(self, "/developer formation [create (formation type) (mob type) (count)]");
                broadcast(self, "/developer formation [remove]");
            }
            return SCRIPT_CONTINUE;
        }*/
        else if (cmd.equalsIgnoreCase("killCredit"))
        {
            target = getIntendedTarget(self);
            final String ATTACK_TYPE = "combat_rangedspecialize_pistol";
            int damage = Integer.parseInt(tok.nextToken());
            Vector attackerList = getResizeableObjIdBatchScriptVar(target, "creditForKills.attackerList.attackers");
            attackerList = addElement(attackerList, self);
            setBatchScriptVar(target, "creditForKills.attackerList.attackers", attackerList);
            setScriptVar(target, "creditForKills.attackerList." + self + ".damage", damage);
            setScriptVar(target, "creditForKills.damageCount", 100);
            setScriptVar(target, "creditForKills.damageTally", damage);
            Vector types = getResizeableObjIdBatchScriptVar(target, "creditForKills.attackerList." + self + ".xp.types");
            types = addElement(types, ATTACK_TYPE);
            setBatchScriptVar(target, "creditForKills.attackerList." + self + ".xp.types", types);
            setScriptVar(target, "creditForKills.attackerList." + self + ".xp." + ATTACK_TYPE, damage);
            broadcast(self, "You have added to the killer list for " + getName(target));
        }
        else if (cmd.equalsIgnoreCase("say"))
        {
            obj_id iTar = getIntendedTarget(self);
            StringBuilder message = new StringBuilder();
            while (tok.hasMoreTokens())
            {
                message.append(" ").append(tok.nextToken());
            }
            chat.chat(iTar, message.toString());
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer say " + message + " on " + getName(iTar));
        }
        else if (cmd.equalsIgnoreCase("setloottable"))
        {
            String table = tok.nextToken();
            if (table == null || table.isEmpty())
            {
                broadcast(self, "Syntax: /developer setLootTable <loot table name>");
            }
            else
            {
                setObjVar(self, "loot.lootTable", table);
                broadcast(self, "Loot table set to " + table);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("setnumitems"))
        {
            int level = Integer.parseInt(tok.nextToken());
            if (level == 0)
            {
                broadcast(self, "Syntax: /developer setNumItems <loot count>");
            }
            else
            {
                setObjVar(self, "loot.numItems", level);
                broadcast(self, "Number of loot items set to " + level);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("setcount"))
        {
            obj_id iTar = getIntendedTarget(self);
            setCount(iTar, Integer.parseInt(tok.nextToken()));
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer setcount " + getName(iTar) + " to " + getCount(iTar));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("setcountcontainer"))
        {
            obj_id[] contents = utils.getContents(target, true);

            // Check if contents are non-empty
            if (contents != null && contents.length > 0)
            {
                // Loop through each item in contents
                for (obj_id content : contents)
                {
                    String token = tok.nextToken();
                    int howMany = stringToInt(token);

                    // Debug output for checking parsed values
                    broadcast(self, "Setting count for " + content + " to " + howMany);

                    setCount(content, howMany);
                }
            }
            else
            {
                broadcast(self, "No contents found for target.");
            }

            return SCRIPT_CONTINUE;
        }

        else if (cmd.equalsIgnoreCase("locomotion"))
        {
            int locomotionState = Integer.parseInt(tok.nextToken());
            setLocomotion(self, locomotionState);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("state"))
        {
            String toggle = tok.nextToken();
            int state = Integer.parseInt(tok.nextToken());
            if (toggle.equalsIgnoreCase("on"))
            {
                setState(self, state, true);
            }
            else if (toggle.equalsIgnoreCase("off"))
            {
                setState(self, state, false);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("posture"))
        {
            int posture = Integer.parseInt(tok.nextToken());
            setPosture(self, posture);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("createGrid"))
        {
            titan_player.createCreatureArch(self, iTarget, tok.nextToken(),
                    stringToInt(tok.nextToken()),
                    stringToFloat(tok.nextToken()),
                    stringToFloat(tok.nextToken())
            );
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("createArch"))
        {
            titan_player.createCreatureGrid(self, iTarget, tok.nextToken(),
                    stringToInt(tok.nextToken()),
                    stringToInt(tok.nextToken()),
                    stringToFloat(tok.nextToken())
            );
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("createBarker"))
        {
            broadcast(self, "Loading mobile list. Please wait.");
            String[] templates = getAllMobiles();
            listbox(self, self, "Select a template.", OK_CANCEL, "Barker Creator", templates, "handleBarkerTemplate", true, false);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("changeLights"))
        {
            //object/tangible/tarkin_custom/decorative/lights/red/deep_red_16m.iff
            String template = "object/tangible/tarkin_custom/decorative/lights/";
            String color = tok.nextToken();
            String selection = tok.nextToken();
            String range = tok.nextToken();
            obj_id cell = getContainedBy(self);
            if (cell == null)
            {
                broadcast(self, "You must be inside a room to use this command.");
                return SCRIPT_CONTINUE;
            }
            obj_id[] lights = getContents(cell);
            for (obj_id light : lights)
            {
                if (hasScript(light, "item.content.rewards.magic_light"))
                {
                    location loc = getLocation(light);
                    float yaw = getYaw(light);
                    float[] rotation = getQuaternion(light);
                    obj_id new_light = createObject(template + color + "/" + selection + "_" + range + "m.iff", loc);
                    if (isIdValid(new_light))
                    {
                        if (!hasScript(new_light, "item.content.rewards.magic_light"))
                        {
                            attachScript(new_light, "item.content.rewards.magic_light");
                        }
                        setQuaternion(new_light, rotation[0], rotation[1], rotation[2], rotation[3]);
                        setObjVar(new_light, "claimedBy", self);
                        setLocation(new_light, loc);
                        destroyObject(light);
                        persistObject(new_light);
                    }
                    else
                    {
                        broadcast(self, "Failed to create new light, template is not valid");
                    }
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("rainbowBroadcast"))
        {
            StringBuilder message = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                message.append(" ").append(tok.nextToken());
            }
            obj_id[] players = getPlayerCreaturesInRange(getLocation(self), 16000f);
            for (obj_id player : players)
            {
                String messageToSend = applyGradient(message.toString(), new Color(16, 114, 237), new Color(130, 177, 238, 255));
                broadcast(player, messageToSend);
            }

        }
        else if (cmd.equalsIgnoreCase("createSoundEmitter"))
        {
            obj_id marker = createObject("object/tangible/ground_spawning/patrol_waypoint.iff", getLocation(self));
            setName(marker, "[DEVL] Sound Emitter");
            attachScript(marker, "content.jukebox");
            broadcast(self, "Sound Emitter created.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("templateSearch"))
        {
            String subfolder = tok.nextToken();
            String keyterm = tok.nextToken();
            setScriptVar(self, "templateSearchPath", subfolder);
            LOG("ethereal", "[Template Lookup]: Subfolder set to " + subfolder);
            setScriptVar(self, "templateSearchTerm", keyterm);
            LOG("ethereal", "[Template Lookup]: Keyterm set to " + keyterm);
            LOG("ethereal", "[Template Lookup]: Showing listbox?");
            broadcast(self, "Loading template list. Please wait...");
            listbox(self, self, "Select a template.", OK_CANCEL, "Template Search", getAllTemplates(self, subfolder, keyterm, true), "handleTemplateSearch", true, false);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("findTemplate"))
        {
            broadcast(self, "Loading template list. Please wait...");
            String keyterm = tok.nextToken();
            setScriptVar(self, "templateSearchTerm", keyterm);
            listbox(self, self, "Select a template.", OK_CANCEL, "Template Search", listObjectFilesByTerm(keyterm), "handleTemplateLookup", true, false);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("randomizeContainer"))
        {
            obj_id[] contents = utils.getContents(target, true);
            for (obj_id content : contents)
            {
                if (getCount(content) > 1)
                {
                    setCount(content, rand(1, 9999));
                }
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sendWarning"))
        {
            StringBuilder words = new StringBuilder(tok.nextToken());
            if (tok.hasMoreTokens())
            {
                while (tok.hasMoreTokens())
                {
                    words.append(" ").append(tok.nextToken());
                }
            }
            System.out.println(words);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sws") || cmd.equalsIgnoreCase("spawnWithScript"))
        {
            String template = tok.nextToken();
            String script = tok.nextToken();
            obj_id item = createObject(template, getLocation(self));
            if (item == null)
            {
                broadcast(self, "Invalid template name.");
                return SCRIPT_CONTINUE;
            }
            if (script == null)
            {
                broadcast(self, "Invalid script name.");
                return SCRIPT_CONTINUE;
            }
            if (getGameObjectType(item) == GOT_building || getGameObjectType(item) == GOT_installation || getGameObjectType(item) == GOT_installation_harvester)
            {
                broadcast(self, "Cannot spawn this game object type.");
                destroyObject(item);
                return SCRIPT_CONTINUE;
            }
            attachScript(item, script);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer s(pawn)w(ith)s(script) " + template + " " + script + " at " + getLocation(self));
            setYaw(item, getYaw(self));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("notifyGalaxy"))
        {
            StringBuilder message = new StringBuilder(tok.nextToken());
            if (tok.hasMoreTokens())
            {
                while (tok.hasMoreTokens())
                {
                    message.append(" ").append(tok.nextToken());
                }
            }
            sendSystemMessageGalaxyOob(message.toString());
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("ringspawn"))
        {
            String creatureToSpawn = tok.nextToken();
            int num = Integer.parseInt(tok.nextToken());
            float radius = stringToFloat(tok.nextToken());
            titan_player.createCircleSpawn(self, self, creatureToSpawn, num, radius);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("ringspawninside"))
        {
            String creatureToSpawn = tok.nextToken();
            int num = Integer.parseInt(tok.nextToken());
            float radius = stringToFloat(tok.nextToken());
            location where = getLocation(self);
            spawnRingInterior(self, num, radius, where, creatureToSpawn);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playsoundloctarget"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playsoundloctarget <sound name>");
            }
            else
            {
                String sound = tok.nextToken();
                playClientEffectLoc(iTarget, sound, getLocation(iTarget), 0.0f);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playsoundatloc"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playsoundatloc <sound name> <x> <y> <z>");
            }
            else
            {
                String sound = tok.nextToken();
                float x = stringToFloat(tok.nextToken());
                float y = stringToFloat(tok.nextToken());
                float z = stringToFloat(tok.nextToken());
                location loc = new location(x, y, z);
                playClientEffectLoc(self, sound, loc, 0.0f);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("areacommand"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer areacommand <radius> <command with params but no leading slash>");
            }
            else
            {
                float radius = stringToFloat(tok.nextToken());
                StringBuilder command = new StringBuilder(tok.nextToken());
                while (tok.hasMoreTokens())
                {
                    command.append(" ").append(tok.nextToken());
                }
                obj_id[] players = getPlayerCreaturesInRange(getLocation(self), radius);
                for (obj_id player : players)
                {
                    if (isGod(player))
                    {
                        broadcast(player, "Ignoring area command as God Mode avatar.");
                    }
                    else
                    {
                        sendConsoleCommand("/" + command, player);
                    }
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer areacommand " + radius + " " + command + " at " + getLocation(self));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("createLootableCorpse"))
        {
            String[] NAMEs = {
                    "an explorer",
                    "a scavenger",
                    "a slicer",
                    "a soldier",
                    "a technician",
                    "a thief",
                    "a trader",
                    "a smuggler",
                    "a bounty hunter",
                    "a mercenary",
                    "a spy",
                    "a senator",
                    "a diplomat",
                    "a pilot",
                    "a scientist",
                    "a medic",
                    "a doctor",
                    "a droid engineer"
            };
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer createLootableCorpse <table> <amount>");
            }
            else
            {
                String table = tok.nextToken();
                int amt = Integer.parseInt(tok.nextToken());
                String corpseTemplate = "object/tangible/container/drum/warren_drum_skeleton.iff";
                location treasureLoc = getLocation(self);
                obj_id treasureChest = createObject(corpseTemplate, treasureLoc);
                attachScript(treasureChest, "item.container.player_loot_crate_adhoc");
                setName(treasureChest, "a corpse of " + NAMEs[rand(0, NAMEs.length - 1)]);
                loot.makeLootInContainer(treasureChest, table, amt, 300);
                broadcast(self, "A loot chest was made with " + amt + " items from the loot table: " + table);
                obj_id[] contents = getContents(treasureChest);
                {
                    for (obj_id content : contents)
                    {
                        if (hasScript(content, "item.special.nomove"))
                        {
                            detachScript(content, "item.special.nomove");
                            broadcast(self, "Removing nomove script from " + content);
                        }
                        if (hasObjVar(content, "noTrade"))
                        {
                            removeObjVar(self, "noTrade");
                            broadcast(self, "Removing No-Trade from " + content);
                        }
                    }
                }
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer createLootableCorpse " + table + " " + amt + " at " + getLocation(self).toReadableFormat(true));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("testLoot"))
        {
            if (tok.countTokens() != 3)
            {
                broadcast(self, "Syntax: /developer testLoot <table> <amount> <level>");
            }
            else
            {
                String lootTable = tok.nextToken();
                int amount = stringToInt(tok.nextToken());
                int level = stringToInt(tok.nextToken());
                loot.makeLootInContainer(getInventoryContainer(self), lootTable, amount, 300);
            }
        }
        else if (cmd.equalsIgnoreCase("lootArea"))
        {
            float range = Float.parseFloat(tok.nextToken());
            lootArea(self, range);
        }
        else if (cmd.equalsIgnoreCase("createLootableCargo"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer createLootableCargo <table> <amount>");
            }
            else
            {
                String table = tok.nextToken();
                int amt = Integer.parseInt(tok.nextToken());
                String corpseTemplate = "object/tangible/container/loot/large_container.iff";
                location treasureLoc = getLocation(self);
                obj_id treasureChest = createObject(corpseTemplate, treasureLoc);
                attachScript(treasureChest, "item.container.player_loot_crate_adhoc");
                setName(treasureChest, "\\#FFC0CBa cargo container\\#.");
                loot.makeLootInContainer(treasureChest, table, amt, 300);
                broadcast(self, "A cargo container was made with " + amt + " items from the loot table: " + table);
                obj_id[] contents = getContents(treasureChest);
                {
                    for (obj_id content : contents)
                    {
                        if (hasScript(content, "item.special.nomove"))
                        {
                            detachScript(content, "item.special.nomove");
                            broadcast(self, "Removing nomove script from " + content);
                        }
                        if (hasObjVar(content, "noTrade"))
                        {
                            removeObjVar(self, "noTrade");
                            broadcast(self, "Removing No-Trade from " + content);
                        }
                    }
                }
                setDescriptionStringId(treasureChest, new string_id("A cargo container filled with various treasures. It is unknown how it got here..."));
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer createLootableCargo at " + getLocation(self).toReadableFormat(true));
            }

            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("defaultRLS"))
        {
            obj_id tatooine = getPlanetByName("tatooine");
            setObjVar(tatooine, "bonus.rls.maxDifferenceBelow", 10);
            setObjVar(tatooine, "bonus.rls.maxDifferenceAbove", 10);
            setObjVar(tatooine, "bonus.rls.rare", 65);//           ***MUST***
            setObjVar(tatooine, "bonus.rls.exceptional", 25);//***EQUAL***
            setObjVar(tatooine, "bonus.rls.legendary", 10);//     ***100***
            setObjVar(tatooine, "bonus.rls.minDistance", 15);
            setObjVar(tatooine, "bonus.rls.minTime", 15); //@Note: this is multiplied by 60 to get minutes.
            setObjVar(tatooine, "bonus.rls.status", true); //@Note: this ensures it does not get turned off on bonus reset.
            setObjVar(tatooine, "bonus.rls.group_status", true); //@Note: this ensures it does not get turned off on bonus reset.
            setObjVar(tatooine, "bonus.rls.rlsDropChance", "1");
            broadcast(self, "Set 10 default values for RLS");
        }
        else if (cmd.equalsIgnoreCase("createJunkCache"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer createJunkCache [total amount] [min amt of each item] [max amt of each item]");
            }
            else
            {
                String corpseTemplate = "object/tangible/container/loot/large_container.iff";
                location treasureLoc = getLocation(self);
                obj_id treasureChest = createObject(corpseTemplate, treasureLoc);
                attachScript(treasureChest, "item.container.player_loot_crate_adhoc");
                setName(treasureChest, "a cache of junk");
                String JUNK_TABLE = "datatables/crafting/reverse_engineering_junk.iff";
                int COUNT = Integer.parseInt(tok.nextToken());
                int MIN_COUNT = Integer.parseInt(tok.nextToken());
                int MAX_COUNT = Integer.parseInt(tok.nextToken());
                String column = "note";
                for (int i = 0; i < COUNT; i++)
                {
                    String junk = dataTableGetString(JUNK_TABLE, rand(1, dataTableGetNumRows(JUNK_TABLE)), column);
                    obj_id junkItem = static_item.createNewItemFunction(junk, treasureChest);
                    if (isIdValid(junkItem))
                    {
                        setCount(junkItem, rand(MIN_COUNT, MAX_COUNT));
                    }
                }
            }
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer createJunkCache at " + getLocation(self));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playmusic"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playmusic <music name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String music = tok.nextToken();
                playMusic(self, music);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("createTaxiToken"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer createTaxiToken <cost>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                int cost = Integer.parseInt(tok.nextToken());
                obj_id token = createObject("object/tangible/loot/tool/datapad_broken.iff", getInventoryContainer(self), "");
                setObjVar(token, "taxi.cost", cost);
                setObjVar(token, "taxi.location_token", getLocation(self));
                setName(token, "Taxi Token");
                LOG("ethereal", "[Developer | Taxi]: " + getName(self) + " used /developer createTaxiToken " + cost + " at " + getLocation(self) + " for use with systems.movement.taxi");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("snowspeeder"))
        {
            setupSnowspeeder(self);
        }
        else if (cmd.equalsIgnoreCase("createTaxi"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer createTaxi <index> <name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String taxiTemplate;
                int index = Integer.parseInt(tok.nextToken());
                StringBuilder name = new StringBuilder();
                while (tok.hasMoreTokens())
                {
                    name.append(tok.nextToken()).append(" ");
                }
                if (index == 1)
                {
                    taxiTemplate = "object/tangible/door/taxi_tantive4.iff";
                }
                else if (index == 2)
                {
                    taxiTemplate = "object/tangible/door/taxi_usv5.iff";
                }
                else if (index == 3)
                {
                    taxiTemplate = "object/tangible/door/zonegate_neutral.iff";
                }
                else if (index == 4)
                {
                    taxiTemplate = "object/tangible/door/zonegate_rebel.iff";
                }
                else if (index == 5)
                {
                    taxiTemplate = "object/tangible/door/zonegate_imperial.iff";
                }
                else
                {
                    broadcast(self, "Invalid index. Options are 1 (Tantive 4 appearance), 2 (USV-5 appearance), 3 (neutral appearance), 4 (rebel appearance) or 5 (imperial appearance).");
                    return SCRIPT_CONTINUE;
                }
                obj_id taxi = createObject(taxiTemplate, getLocation(self));
                setObjVar(taxi, "taxi.name", name.toString());
                attachScript(taxi, "systems.movement.taxi");
                LOG("ethereal", "[Developer | Taxi]: " + getName(self) + " used /developer createTaxi " + index + " at " + getLocation(self) + " for use with systems.movement.taxi");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("botHumanoid"))
        {
            String[] RACES = {
                    "bothan",
                    "human",
                    "ithorian",
                    "moncal",
                    "rodian",
                    "sullustan",
                    "twilek",
                    "wookiee",
                    "zabrak"
            };
            int randomIndex = rand(0, RACES.length - 1);
            String randomString = RACES[randomIndex];
            int genderChance = rand(1, 100);
            obj_id bot;
            if (genderChance < 49)
            {
                bot = create.object("object/creature/player/" + randomString + "_male.iff", getLocation(self));
            }
            else
            {
                bot = create.object("object/creature/player/" + randomString + "_female.iff", getLocation(self));
            }
            attachScript(bot, "bot.clone");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer botHumanoid at " + getLocation(self));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("playmusictarget"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer playmusictarget <music name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String music = tok.nextToken();
                playMusic(iTarget, music);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("getItemStringByName"))
        {
            if (!tok.hasMoreTokens())
            {
                broadcast(self, "Syntax: /developer getItemStringByName <item name>");
                return SCRIPT_CONTINUE;
            }
            else
            {
                String itemName = tok.nextToken();
                StringBuilder prompt = new StringBuilder();
                String[] items = dataTableGetStringColumnNoDefaults("datatables/item/master_item/master_item.iff", "name");
                for (String item : items)
                {
                    if (item.contains(itemName))
                    {
                        prompt.append(item).append("\n");
                    }
                }
                int page = createSUIPage("/Script.messageBox", self, self);
                String finalPrompt = "The following items were found with the name: " + itemName + "\n" + prompt;
                setSUIProperty(page, "Prompt.lblPrompt", "LocalText", finalPrompt);
                setSUIProperty(page, "Prompt.lblPrompt", "Font", "bold_22");
                setSUIProperty(page, "bg.caption.lblTitle", "Text", "Strings");
                setSUIProperty(page, "Prompt.lblPrompt", "Editable", "true");
                setSUIProperty(page, "Prompt.lblPrompt", "GetsInput", "true");
                subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
                setSUIProperty(page, "btnCancel", "Visible", "true");
                setSUIProperty(page, "btnRevert", "Visible", "false");
                setSUIProperty(page, "btnOk", "Visible", "false");
                showSUIPage(page);
                flushSUIPage(page);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("findPlayers"))
        {
            location origin = new location();
            origin.x = 0.0f;
            origin.y = 0.0f;
            origin.z = 0.0f;
            origin.cell = null;
            origin.area = getCurrentSceneName();
            float range = 7900f;
            obj_id[] playerObjects = getAllPlayers(origin, range);
            if (playerObjects.length == 0)
            {
                broadcast(self, "No players found.");
            }
            return SCRIPT_CONTINUE;
            //listbox with handler here.
        }

        else if (cmd.equalsIgnoreCase("shotgunResources"))
        {
            shotgunResources(self);
        }

        else if (cmd.equalsIgnoreCase("makeEnt"))
        {
            //@NOTE: We only want female variants. Males can't use themepark_oola mood (exotic4)
            String[] TEMPLATES = {
                    "object/mobile/dressed_commoner_naboo_human_female_01.iff",
                    "object/mobile/dressed_commoner_naboo_human_female_02.iff",
                    "object/mobile/dressed_commoner_naboo_human_female_03.iff",
                    "object/mobile/dressed_commoner_naboo_human_female_04.iff",
                    "object/mobile/dressed_commoner_naboo_human_female_05.iff"
            };
            obj_id entertainer = createObject(TEMPLATES[rand(0, TEMPLATES.length - 1)], getLocation(self));
            setName(entertainer, "a Master Entertainer");
            setInvulnerable(entertainer, true);
            attachScript(entertainer, "ai.ai");
            ai_lib.setMood(entertainer, "themepark_oola");
            ai.stop(entertainer);
            attachScript(entertainer, "bot.entertainer");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("toggleVendorCosts")) // GM command to toggle vendor costs on and off - useful for debugging item transactions
        {
            String vendorVar = "vend";
            if (hasObjVar(self, vendorVar))
            {
                removeObjVar(self, vendorVar);
                broadcast(self, "Vendor costs are now disabled.");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer toggleVendorCosts to disable vendor costs for themselves at " + getLocation(self).toReadableFormat(true));
            }
            else
            {
                setObjVar(self, vendorVar, 1);
                broadcast(self, "Vendor costs are now enabled.");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer toggleVendorCosts to enable vendor costs for themselves at " + getLocation(self).toReadableFormat(true));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("invulnerable"))
        {
            if (isInvulnerable(target))
            {
                setInvulnerable(target, false);
                broadcast(self, "Target is no longer invulnerable.");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer invulnerable on " + getName(target) + " at " + getLocation(target).toReadableFormat(true) + " to make them vulnerable.");
            }
            else
            {
                setInvulnerable(target, true);
                broadcast(self, "Target is now invulnerable.");
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer invulnerable on " + getName(target) + " at " + getLocation(target).toReadableFormat(true) + " to make them invulnerable.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("commPlanet"))
        {
            StringBuilder message = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                message.append(" ").append(tok.nextToken());
            }
            obj_id[] recipients = getPlayerCreaturesInRange(getLocation(self), PLANETWIDE);
            for (obj_id recipient : recipients)
            {
                prose_package pp = new prose_package();
                prose.setStringId(pp, new string_id(message.toString()));
                commPlayer(self, recipient, pp);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("height"))
        {
            String subcommand = tok.nextToken();
            if (subcommand.equals("copy"))
            {
                location loc = getLocation(iTarget);
                float height = loc.y;
                setObjVar(self, "developer_clipboard.height", height);
                broadcast(self, "Height of " + height + " copied.");
                return SCRIPT_CONTINUE;
            }
            else if (subcommand.equals("paste"))
            {
                if (!hasObjVar(self, "developer_clipboard.height"))
                {
                    broadcast(self, "No height to paste.");
                    return SCRIPT_CONTINUE;
                }
                float height = getFloatObjVar(self, "developer_clipboard.height");
                location loc = getLocation(iTarget);
                loc.y = height;
                setLocation(iTarget, loc);
                broadcast(self, "Height of " + height + " pasted to " + target);
                return SCRIPT_CONTINUE;
            }
        }
        else if (cmd.equalsIgnoreCase("align"))
        {
            String subcommand = tok.nextToken();
            if (subcommand.equals("x"))
            {
                String subCommand = tok.nextToken();
                if (subCommand.equals("copy"))
                {
                    location loc = getLocation(iTarget);
                    float alignment = loc.x;
                    setObjVar(self, "developer_clipboard.x", alignment);
                }
                else if (subCommand.equals("paste"))
                {
                    location loc = getLocation(iTarget);
                    loc.x = getFloatObjVar(self, "developer_clipboard.x");
                    setLocation(iTarget, loc);
                }
                return SCRIPT_CONTINUE;
            }
            else if (subcommand.equals("z"))
            {
                String subCommand = tok.nextToken();
                if (subCommand.equals("copy"))
                {
                    location loc = getLocation(iTarget);
                    float alignment = loc.z;
                    setObjVar(self, "developer_clipboard.z", alignment);
                }
                else if (subCommand.equals("paste"))
                {
                    location loc = getLocation(iTarget);
                    loc.z = getFloatObjVar(self, "developer_clipboard.z");
                    setLocation(iTarget, loc);
                }
                return SCRIPT_CONTINUE;
            }
        }
        else if (cmd.equalsIgnoreCase("copy"))
        {
            String subcommand = tok.nextToken();
            if (subcommand.equalsIgnoreCase("-onto"))
            {
                String template = getTemplateName(iTarget);
                sendConsoleCommand("/spawn " + template + " 1 0 0", self);
                return SCRIPT_CONTINUE;
            }
            else if (subcommand.equalsIgnoreCase("-into"))
            {
                String template = getTemplateName(iTarget);
                obj_id pInv = getInventoryContainer(self);
                sendConsoleCommand("/object createIn " + template + " " + pInv, self);
                return SCRIPT_CONTINUE;
            }
            else if (subcommand.equalsIgnoreCase("-template"))
            {
                String flag = tok.nextToken();
                switch (flag)
                {
                    case "copy":
                        String template = getTemplateName(iTarget);
                        setObjVar(self, "developer_clipboard.template", template);
                        broadcast(self, "Template " + template + " copied.");
                        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer copy -template copy on " + getName(iTarget) + " at " + getLocation(iTarget).toReadableFormat(true) + " to copy the template.");
                        break;
                    case "paste":
                        String targetTemplate = getStringObjVar(self, "developer_clipboard.template");
                        sendConsoleCommand("/spawn " + targetTemplate + " 1 0 0", self);
                        broadcast(self, "Template " + targetTemplate + " pasted.");
                        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer copy -template paste on " + getName(iTarget) + " at " + getLocation(iTarget).toReadableFormat(true) + " to paste the template.");
                        break;
                    case "clear":
                        removeObjVar(self, "developer_clipboard.template");
                        broadcast(self, "Template cleared.");
                        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer copy -template clear on " + getName(iTarget) + " at " + getLocation(iTarget).toReadableFormat(true) + " to clear the template.");
                        break;
                    default:
                        broadcast(self, "Usage: /developer copy -template [copy | paste | clear]");
                        return SCRIPT_CONTINUE;
                }
            }
        }
        else if (cmd.equalsIgnoreCase("distance"))
        {
            obj_id objectOne = getIntendedTarget(self);
            obj_id objectTwo = getLookAtTarget(self);
            if (!isValidId(objectOne) || !exists(objectOne))
            {
                broadcast(self, "You must have a target.");
                return SCRIPT_CONTINUE;
            }
            if (!isValidId(objectTwo) || !exists(objectTwo))
            {
                broadcast(self, "You must have a look at target.");
                return SCRIPT_CONTINUE;
            }
            float distance = getDistance2D(objectOne, objectTwo);
            broadcast(self, "The distance between these two targets is " + distance + " or " + Math.round(distance) + " rounded.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getCollision"))
        {
            obj_id who = getIntendedTarget(self);
            broadcast(self, "NetworkID: [" + who + "] | Collision radius is " + getObjectCollisionRadius(who) + "or " + Math.round(stringToFloat(getObjectCollisionRadius(who) + " rounded.")));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("makeUtilitySpawner"))
        {
            String[] TYPES = {"Medical Droid", "Tactical Probe", "Entertainer", "Artisan Enhancer"};
            listbox(self, self, "Select a utility spawner to spawn:", OK_CANCEL, "Utility Spawner", TYPES, "handleUtilitySpawnerChoice", true, false);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("decorationIncrement"))
        {
            float increment = stringToFloat(tok.nextToken());
            setObjVar(target, "decorationPanel.increment", increment);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("gonkie"))
        {
            broadcast(self, "This is experimental, do not use near players or invulnerable NPCs.");
            obj_id gonkieControlDevice = create.object("object/tangible/loot/generic_usable/frequency_jammer_wire_generic.iff", getLocation(self));
            putIn(gonkieControlDevice, getInventoryContainer(self));
            if (hasScript(gonkieControlDevice, "item.buff_click_item"))
            {
                detachScript(gonkieControlDevice, "item.buff_click_item");
            }
            setName(gonkieControlDevice, "Experimental EG-7 Grenadier Control Device");
            attachScript(gonkieControlDevice, "developer.bubbajoe.xp_gonk");
            setDescriptionStringId(gonkieControlDevice, new string_id("This control device allows users to request an experimental EG-6 unit. The unit will be delivered to the user's current location."));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("saveBuilding"))
        {
            obj_id[] contents = getContents(target);
            persistObject(target);
            for (obj_id content : contents)
            {
                persistObject(content);
                obj_id[] itemsInCell = getContents(content);
                for (obj_id itemInCell : itemsInCell)
                {
                    if (hasObjVar(itemInCell, "noPersist") || hasObjVar(itemInCell, "objParent"))
                    {
                        echo(self, "Skipping " + itemInCell + " (" + getName(itemInCell) + ") due to it being a spawner child.");
                        continue;
                    }
                    if (isPlayer(itemInCell))
                    {
                        echo(self, "Skipping " + itemInCell + " (" + getName(itemInCell) + ") due to it being a player.");
                        continue;
                    }
                    persistObject(itemInCell);
                    echo(self, "Persisted child object " + itemInCell + " (" + getName(itemInCell) + ")");
                }
                echo(self, "Persisted cell" + content + " (" + getName(content) + ")");
                echo(self, "Use [/developer saveBuildingCell OID] to save each cell individually.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("saveBuildingCell"))
        {
            obj_id building = stringToObjId(tok.nextToken());
            obj_id[] contents = getContents(building);
            persistObject(building);
            for (obj_id content : contents)
            {
                persistObject(content);
                echo(self, "Persisted " + content + " (" + getName(content) + ")");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("jail"))
        {
            String flag = tok.nextToken();
            if (flag.equals("create"))
            {
                handleJail(self, target);
            }
            else if (flag.equals("remove"))
            {
                cleanUpJail(self, target);
            }
            else
            {
                broadcast(self, "Syntax: /developer jail [create | remove]");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("meddroid"))
        {
            obj_id healer = create.object("object/mobile/fx_7_droid.iff", getLocation(self));
            attachScript(healer, "developer.bubbajoe.doctor_droid");
            setName(healer, "FX-7 Medical Assistant Droid");
            setInvulnerable(healer, true);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer meddroid at " + getLocation(self).toReadableFormat(true));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equals("setCraftedBy"))
        {
            String craftedBy = tok.nextToken();
            obj_id who = getPlayerIdFromFirstName(craftedBy);
            setCrafter(target, who);
            broadcast(self, "This Item will now display that " + craftedBy + " crafted it.");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer setCraftedBy " + craftedBy + " on " + getName(target) + " at " + getLocation(target).toReadableFormat(true));
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("uitest"))
        {
            String page = tok.nextToken();
            int pageId = createSUIPage(page, self, self);
            showSUIPage(pageId);
            broadcast(self, "UI Test: " + page);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("boxspawn"))
        {
            titan_player.createCreatureGrid(self, iTarget, tok.nextToken(), Integer.parseInt(tok.nextToken()), Integer.parseInt(tok.nextToken()), stringToFloat(tok.nextToken()));
            echo(self, "OK!");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("clone"))
        {
            obj_id pInv = getInventoryContainer(self);
            String copies = tok.nextToken();
            for (int i = 0; i < Integer.parseInt(copies); i++)
            {
                obj_id cloned_item = cloneObject(iTarget, pInv);
                String objvars = getPackedObjvars(iTarget);
                for (String objvar : objvars.split(","))
                {
                    String[] objvarData = objvar.split(":");
                    if (objvarData.length == 2)
                    {
                        setObjVar(cloned_item, objvarData[0], objvarData[1]);
                    }
                }
                for (String s : getScriptList(iTarget))
                {
                    attachScript(iTarget, s);
                    setName(cloned_item, getEncodedName(iTarget));
                }
                broadcast(self, "Attempting to clone " + getName(iTarget) + " to " + getName(self) + "'s inventory with " + copies + " copies.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("cloneSpawner"))
        {
            obj_id cloned_item = cloneObject(iTarget, getLocation(self));
            String objvars = getPackedObjvars(iTarget);
            setPackedObjvars(cloned_item, objvars);
            String scripts = getPackedScripts(iTarget);
            setPackedScripts(cloned_item, scripts);
            setName(cloned_item, getEncodedName(iTarget));
            broadcast(self, "Cloning spawner (" + getEncodedName(iTarget) + ") to your location.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("editVehicle"))
        {
            if (tok.countTokens() < 1)
            {
                broadcast(self, "Syntax: /developer editVehicle (target) <mod index> <mod value>");
                return SCRIPT_CONTINUE;
            }
            if (vehicle.isRidingVehicle(target))
            {
                obj_id vehid = getMountId(target);
                String vehicleModifier = tok.nextToken();
                float vehicleModifierValue = stringToFloat(tok.nextToken());
                vehicle.setValue(vehid, vehicleModifierValue, Integer.parseInt(vehicleModifier));
                LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer editVehicle on " + getName(target) + " at " + getLocation(target).toReadableFormat(true) + " to set " + vehicleModifier + " to " + vehicleModifierValue + ".");
                return SCRIPT_CONTINUE;
            }
            else
            {
                broadcast(self, "They are not riding a vehicle.");
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("sliderTest"))
        {
            dictionary d = new dictionary();
            broadcast(self, "Attempting to show slider...");
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer sliderTest at " + getLocation(self).toReadableFormat(true));
            //slider(self, self, "Slide the value to a random number", "Slider", 50, 0, 100, d, "handleSliderTest");
        }
        else if (cmd.equalsIgnoreCase("countdownTest"))
        {
            float maxRangeFromStart = 12.0f;
            int flags = CD_EVENT_NONE;
            flags |= CD_EVENT_COMBAT;
            flags |= CD_EVENT_LOCOMOTION;
            flags |= CD_EVENT_INCAPACITATE;
            smartCountdownTimerSUI(self, self, "quest_countdown_timer", string_id.unlocalized("Counting down..."), 0, 60, "handleCountdownTest", maxRangeFromStart, flags);
        }
        else if (cmd.equalsIgnoreCase("scriptvar"))
        {
            debugConsoleMsg(self, "scriptvar command received");
            String subcommand = tok.nextToken();
            if (subcommand.equalsIgnoreCase("set"))
            {
                String varName = tok.nextToken();
                String varValue = tok.nextToken();
                setScriptVar(target, varName, varValue);
                debugConsoleMsg(self, "scriptvar " + varName + " set to " + varValue + " for " + getPlayerFullName(target));
            }
            else if (subcommand.equalsIgnoreCase("removeTree"))
            {
                String varName = tok.nextToken();
                removeScriptVarTree(target, varName);
                debugConsoleMsg(self, "scriptvar tree" + varName + " removed from " + getPlayerFullName(target));
            }
            else if (subcommand.equalsIgnoreCase("remove"))
            {
                String varName = tok.nextToken();
                removeScriptVar(target, varName);
                debugConsoleMsg(self, "scriptvar " + varName + " removed from " + getPlayerFullName(target));
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("staticItemDetails"))
        {
            String subcommand = tok.nextToken();
            if (subcommand.equals("-t"))
            {
                if (static_item.isStaticItem(getIntendedTarget(self)))
                {
                    handleItemLookup(self, static_item.getStaticItemName(getIntendedTarget(self)));
                }
                else
                {
                    broadcast(self, "Target object is not in the master item list.");
                }
            }
            if (subcommand.equals("-s"))
            {
                String itemName = tok.nextToken();
                handleItemLookup(self, itemName);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("mobDetails"))
        {
            obj_id what = getIntendedTarget(self);
            String subcommand = tok.nextToken();
            if (subcommand.equals("-t")) // -t : target
            {
                if (!isPlayer(what) && (isMob(what)))
                {
                    handleCreatureLookup(self, getCreatureName(what));
                }
                else
                {
                    broadcast(self, "Target object is not in the master creature list.");
                }
            }
            if (subcommand.equals("-s"))// -s : string
            {
                String itemName = tok.nextToken();
                handleCreatureLookup(self, itemName);
            }
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("giveItemsForDWB"))
        {
            obj_id inv = getInventoryContainer(self);
            obj_id satchel = create.createObject("object/tangible/container/general/satchel.iff", getInventoryContainer(self), "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/binary_liquid.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/ducted_fan.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/jetpack_base.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/jetpack_stabilizer.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/fuel_injector_tank.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/fuel_dispersion_unit.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/emulsion_protection.iff", satchel, "");
            create.createObject("object/tangible/loot/dungeon/death_watch_bunker/mining_drill_reward.iff", satchel, "");
            setName(satchel, "DWB Items");
            broadcast(self, "Items have been added to a satchel in your inventory.");
        }
        else if (cmd.equalsIgnoreCase("getPrompts"))
        {
            StringBuilder prompt = new StringBuilder(tok.nextToken());
            while (tok.hasMoreTokens())
            {
                prompt.append(" ").append(tok.nextToken());
            }
            response_store.listResponses(self, prompt.toString());
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getAllResponses"))
        {
            response_store.printAllResponses(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("getAllPrompts"))
        {
            response_store.printAllPrompts(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("exportBuilding"))
        {
            if (isInWorldCell(self))
            {
                sendSystemMessageTestingOnly(self, "You must be in a cell to export a building.");
                return SCRIPT_CONTINUE;
            }
            obj_id building = getTopMostContainer(self);
            if (!isIdValid(building))
            {
                sendSystemMessageTestingOnly(self, "You must be in a building to export it.");
                return SCRIPT_CONTINUE;
            }
            exportHousingContents(self, building, tok.nextToken());
        }
        else if (cmd.equalsIgnoreCase("housingTable"))
        {
            showAllHousing(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("itemTable"))
        {
            showMasterItems(self);
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("prepareStaticStrings"))
        {
            String table = "datatables/item/master_item/master_item.iff";
            String clobberedText = "/home/swg/swg-main/exe/linux/static_item_n.txt";
            String clobberedDesc = "/home/swg/swg-main/exe/linux/static_item_d.txt";
            String[] columns = {
                    "name",
                    "string_name",
                    "string_detail"
            };
            int rows = dataTableGetNumRows(table);
            for (int i = 0; i < rows; i++)
            {
                String itemCode = dataTableGetString(table, i, columns[0]);
                String itemName = dataTableGetString(table, i, columns[1]);
                String itemDesc = dataTableGetString(table, i, columns[2]);
                String finalizedFormatName = itemCode + "\t" + itemName + "\n";
                String finalizedFormatDesc = itemCode + "\t" + itemDesc + "\n";
                BufferedWriter nameWriter = new BufferedWriter(new FileWriter(clobberedText, true));
                nameWriter.append(' ');
                nameWriter.append(finalizedFormatName);
                nameWriter.close();
                BufferedWriter descWriter = new BufferedWriter(new FileWriter(clobberedDesc, true));
                descWriter.append(' ');
                descWriter.append(finalizedFormatDesc);
                descWriter.close();
            }
            broadcast(self, "Attempting to export " + columns.length + " columns worth of data split between " + clobberedText + " and " + clobberedDesc);
            LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " used /developer prepareStaticStrings to export " + columns.length + " columns worth of data split between " + clobberedText + " and " + clobberedDesc + " |  Check /exe/linux for the files.");
            return SCRIPT_CONTINUE;
        }
        else if (cmd.equalsIgnoreCase("-help"))
        {
            debugConsoleMsg(self, "Developer Commands:  ");
            debugConsoleMsg(self, "  /developer quest grant <questname> - Grants a quest to the target.");
            debugConsoleMsg(self, "  /developer quest complete <questname> - Completes a quest for the target.");
            debugConsoleMsg(self, "  /developer quest clear <questname> - Clears a quest for the target.");
            debugConsoleMsg(self, "  /developer quest task complete <questname> <taskname> - Completes a task for the target.");
            debugConsoleMsg(self, "  /developer say <message> - Makes the target mobile or player speak a message.");
            debugConsoleMsg(self, "  /developer comm <message> - Makes the target speak a message in a comm. window.");
            debugConsoleMsg(self, "  /developer scale <float> - Resizes the target.");
            debugConsoleMsg(self, "  /developer messageto <message> <float> - Sends a message to the target.");
            debugConsoleMsg(self, "  /developer wiki <search> - Opens a wiki page in your browser.");
            debugConsoleMsg(self, "  /developer pathToMe - Creates a path to you.");
            debugConsoleMsg(self, "  /developer convertStringToCrc <string> - Converts a string to a CRC.");
            debugConsoleMsg(self, "  /developer possess <command> <params> - Possesses the target and makes them do a command.");
            debugConsoleMsg(self, "  /developer shell <directory> <command and params> - Runs a command on the server box and returns the output to a messagebox.");
            debugConsoleMsg(self, "  /developer pumpkin ring <num to spawn> <radius> - Spawns a ring of pumpkins around the pumpkin master.");
            debugConsoleMsg(self, "  /developer pumpkin single - Makes a single pumpkin");
            debugConsoleMsg(self, "  /developer ballgame - Creates a ball in your inventory.");
            debugConsoleMsg(self, "  /developer tagContainerContents <tag> - Tags all items in a container. Formats all item names in container with a parenthesis and the tag.");
            debugConsoleMsg(self, "  /developer revertContainerContents - Resets the items in a container to their template paths.");
            debugConsoleMsg(self, "  /developer renameContainerContents <name> - Renames all items in a container to the parameter.");
            debugConsoleMsg(self, "  /developer lockContainer - Applies noTrade objvar and attaches item.special.nomove");
            debugConsoleMsg(self, "  /developer unlockContainer - Removes noTrade objvar and detaches item.special.nomove");
            debugConsoleMsg(self, "  /developer areacommand [radius] [full command with no /]");
            debugConsoleMsg(self, "/developer staticItemDetails [-t (target)] | [-s (string)] - Displays details about a static item.");
            debugConsoleMsg(self, "/developer listStaticItems - Lists all static items inside the container you are targeting.");
            debugConsoleMsg(self, "/developer mobDetails [-t (target)] | [-s (string)] - Displays details about a mob.");
            debugConsoleMsg(self, "/developer restrictArea [radius] [volume suffix]- Restricts the area at location of execution.");
            debugConsoleMsg(self, "/developer unrestrictArea (target) [volume suffix]- Unrestricts the area at location of execution.");
            debugConsoleMsg(self, "/developer setCraftedBy (target) [name] - Sets the crafted by name of the target.");
            debugConsoleMsg(self, "\\#DD1234 Too many commands to list here.  Please see the script for a full list.\\#.");
            return SCRIPT_CONTINUE;
        }
        else
        {
            broadcast(self, "Too many subcommands, please read the script.");
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    private void reloadDatatable(obj_id self, String table)
    {
        broadcast(self, "Attempting to reload " + table);
        titan_utils.reloadTable(self, table, true);
    }

    public void blog(String msg)
    {
        LOG("ethereal", "[Oracle]: " + msg);
    }


    private void setupSUIPage(int page)
    {
        // Set text properties for pageText and outputPage
        setSUIProperty(page, "pageText.text", "Text", "");
        setSUIProperty(page, "pageText.text", "LocalText", "");
        setSUIProperty(page, "pageText.text", "Font", "bold_22");
        setSUIProperty(page, "pageText.text", "Editable", "True");
        setSUIProperty(page, "pageText.text", "GetsInput", "True");

        setSUIProperty(page, "outputPage.text", "Text", "");
        setSUIProperty(page, "outputPage.text", "LocalText", "");
        setSUIProperty(page, "outputPage.text", "Font", "bold_22");
        setSUIProperty(page, "outputPage.text", "Editable", "True");
        setSUIProperty(page, "outputPage.text", "GetsInput", "True");

        // Set size and location for page
        setSUIProperty(page, "", "Size", "1024,758");
        setSUIProperty(page, "", "Location", "500,500");

        // Button and caption text
        setSUIProperty(page, "btnOk", "Text", "Run Query");
        setSUIProperty(page, "bg.caption.text", "LocalText", "Oracle Query Tool");
    }

    private void subscribeToSUIEvents(int page)
    {
        // Subscribe to properties
        subscribeToSUIProperty(page, "pageText.text", "Text");
        subscribeToSUIProperty(page, "pageText.text", "LocalText");
        subscribeToSUIProperty(page, "outputPage.text", "Text");

        // Subscribe to button events
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnOk", "pageText.text", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnOk", "pageText.text", "Text");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnOk", "executeQuery");
    }

    private boolean isInvalidQuery(String queryText)
    {
        if (queryText.isEmpty())
        {
            debugSpeakMsg(getSelf(), "[Query]: Statement cannot be empty.");
            return true;
        }

        if (queryText.contains(";"))
        {
            debugSpeakMsg(getSelf(), "[Query]: Don't include semi-colons, those are done already.");
            return true;
        }

        return false;
    }


    private void updateQueryResultUI(obj_id self, String result)
    {
        int page = getIntObjVar(self, "queryPage");
        setSUIProperty(page, "outputPage.text", "Text", result);
        setSUIProperty(page, "outputPage.text", "LocalText", result);
        flushSUIPage(page);
        showSUIPage(page);
    }

    private void getPlayers(obj_id self) throws InterruptedException
    {
        location origin = new location();
        origin.x = 0.0f;
        origin.y = 0.0f;
        origin.z = 0.0f;
        origin.cell = null;
        origin.area = getCurrentSceneName();
        obj_id[] players = getPlayerCreaturesInRange(origin, 16000f);
        String[] firstName = new String[players.length];
        obj_id[] playerIds = new obj_id[players.length];
        for (int i = 0; i < players.length; i++)
        {
            firstName[i] = getFirstName(players[i]);
            playerIds[i] = players[i];
        }
        setScriptVar(self, "dev-playerIds", playerIds);
        listbox(self, self, "Select a player to manage.", OK_CANCEL, "Players", firstName, "handlePlayerSelection", true, false);
    }

    public int handlePlayerSelection(obj_id self, dictionary params) throws InterruptedException
    {
        String[] OPTIONS = {
                "Go To",
                "Summon",
                "Grant Item",
                "Deposit Money",
                "Assign Badge",
                "Apply Buffs",
                "Modify Collection",
                "Edit Inventory",
                "Edit Datapad",
                "Quest Menu",
                "Bounty Menu"
        };
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id[] playerIds = getObjIdArrayScriptVar(self, "dev-playerIds");
        obj_id player = playerIds[idx];
        if (isIdValid(player))
        {
            setScriptVar(self, "dev-selectedPlayer", player);
            broadcast(self, "Selected player: " + getPlayerFullName(player));
        }
        listbox(self, self, "Select an option to perform on the selected player.", OK_CANCEL, "Options", OPTIONS, "handlePlayerOptionSelection", true, false);
        return SCRIPT_CONTINUE;
    }

    public int handleQuestSubMenuSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            sendSystemMessage(self, "No quest selected.", null);
            return SCRIPT_CONTINUE;
        }

        // Retrieve the stored quest list.
        int[] allQuests = getIntArrayObjVar(self, "dev.questList");
        if (idx >= allQuests.length || idx < 0)
        {
            sendSystemMessage(self, "Invalid selection.", null);
            return SCRIPT_CONTINUE;
        }

        // Process selected quest.
        int questId = allQuests[idx];
        String questName = groundquests.questGetQuestName(questId);
        String[] OPTIONS = {
                "Grant",
                "Complete",
                "Clear"
        };

        // Create another menu for actions on the selected quest.
        setObjVar(self, "dev.selectedQuestId", questId);
        listbox(self, self,
                "Select an action to perform on: " + questName,
                OK_CANCEL, "Quest Actions", OPTIONS,
                "handleQuestOptionSelection", true, false);

        return SCRIPT_CONTINUE;
    }

    public int handlePlayerOptionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        switch (idx)
        {
            case 0:
                warpPlayer(self, getLocation(target));
                break;
            case 1:
                warpPlayer(target, getLocation(self));
                break;
            case 2:
                setupGiveItem(self);
                break;
            case 3:
                setupGiveMoney(self);
                break;
            case 4:
                assignBadge(self);
                break;
            case 5:
                setupEnhance(self);
                break;
            case 6:
                modifyCollection(self);
                break;
            case 7:
                sendConsoleCommand("/editInventory " + getPlayerName(target), self);
                break;
            case 8:
                sendConsoleCommand("/editDatapad " + getPlayerName(target), self);
                break;
            case 9:
                // Fetch quests related to the target player.
                int[] completedQuests = groundquests.questGetAllCompletedQuestIds(target);
                int[] quests = groundquests.questGetAllActiveQuestIds(target);

                // Combine completed and active quest arrays.
                int[] allQuests = new int[completedQuests.length + quests.length];
                System.arraycopy(quests, 0, allQuests, 0, quests.length);
                System.arraycopy(completedQuests, 0, allQuests, quests.length, completedQuests.length);

                // Check if there are quests to display.
                if (allQuests.length == 0)
                {
                    sendSystemMessage(self, "No quests available for the target player.", null);
                    break;
                }

                // Prepare a display menu with quests and corresponding statuses.
                String[] questOptions = new String[allQuests.length];
                for (int i = 0; i < quests.length; i++)
                {
                    questOptions[i] = "(A) " + groundquests.questGetQuestName(quests[i]); // Active quests.
                }
                for (int i = 0; i < completedQuests.length; i++)
                {
                    questOptions[i + quests.length] = "(C) " + groundquests.questGetQuestName(completedQuests[i]); // Completed quests.
                }

                // Create a sub-menu for quest selection.
                setObjVar(self, "dev.questList", allQuests); // Store quests for later handling in callback.
                listbox(self, self,
                        "Select a quest to modify:",
                        OK_CANCEL, "Quest List", questOptions,
                        "handleQuestSubMenuSelection", true, false);
                break;
            case 10:
                setupBountyMenu(self);
                break;
        }
        return SCRIPT_CONTINUE;
    }

    private void setupBountyMenu(obj_id self) throws InterruptedException
    {
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return;
        }
        String[] OPTIONS = {
                "Add Bounty",
                "Remove Bounty",
        };
        listbox(self, self, "Select an option to perform on the selected player's bounties.", OK_CANCEL, "Options", OPTIONS, "handleBountyOptionSelection", true, false);
    }

    public int handleBountyOptionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        if (idx == 0)
        {
            broadcast(self, "Attempting to set bounty on " + getPlayerFullName(target));
            bounty_hunter.showSetBountySUI(self, target);
        }
        else if (idx == 1)
        {
            bounty_hunter.removeJediBounty(target, self);
            broadcast(self, "Bounty removed from " + getPlayerFullName(target));
        }
        return SCRIPT_CONTINUE;
    }

    public int handleQuestSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        //Get completed and active, but append (C) of (A) in front of the quest name.
        int[] completedQuests = groundquests.questGetAllCompletedQuestIds(target);
        int[] quests = groundquests.questGetAllActiveQuestIds(target);
        int questId = quests[idx];
        // add completed after active
        int[] allQuests = new int[completedQuests.length + quests.length];
        System.arraycopy(quests, 0, allQuests, 0, quests.length);
        System.arraycopy(completedQuests, 0, allQuests, quests.length, completedQuests.length);
        StringBuilder questName = new StringBuilder(groundquests.questGetQuestName(allQuests[idx]));
        for (int i = 0; i < allQuests.length; i++)
        {
            String properQuestName = groundquests.questGetQuestName(allQuests[i]);
            if (i < quests.length)
            {
                questName.insert(0, "(A) ");
            }
            else
            {
                questName.insert(0, "(C) ");
            }
            allQuests[i] = Integer.parseInt(properQuestName);
        }
        String[] OPTIONS = {
                "Grant",
                "Complete",
                "Clear"
        };
        listbox(self, self, "Select an option to perform on the selected quest.", OK_CANCEL, "Quests", OPTIONS, "handleQuestOptionSelection", true, false);
        return SCRIPT_CONTINUE;
    }

    public int handleQuestOptionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }

        // Retrieve the target player and validate.
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }

        // Retrieve the stored quests and validate the selection index.
        int[] allQuests = getIntArrayObjVar(self, "dev.questList");
        if (allQuests == null || idx >= allQuests.length || idx < 0)
        {
            sendSystemMessage(self, "Invalid quest selection.", null);
            return SCRIPT_CONTINUE;
        }

        // Identify the selected quest ID and its name.
        int selectedQuestId = allQuests[idx];
        String selectedQuestName = groundquests.questGetQuestName(selectedQuestId);

        // Execute the selected action.
        switch (btn)
        {
            case 0: // Grant quest
                groundquests.grantQuest(target, selectedQuestName);
                broadcast(self, "Granted quest " + selectedQuestName + " to " + getPlayerFullName(target));
                break;
            case 1: // Complete quest
                groundquests.completeQuest(target, selectedQuestName);
                broadcast(self, "Completed quest " + selectedQuestName + " for " + getPlayerFullName(target));
                break;
            case 2: // Clear quest
                groundquests.clearQuest(target, selectedQuestName);
                broadcast(self, "Cleared quest " + selectedQuestName + " for " + getPlayerFullName(target));
                break;
            default:
                sendSystemMessage(self, "Invalid action selected.", null);
                return SCRIPT_CONTINUE;
        }

        // Refresh the quests and re-display the quest management menu.
        int[] refreshedQuests = groundquests.questGetAllActiveQuestIds(target);
        String[] questNames = new String[refreshedQuests.length];
        for (int i = 0; i < refreshedQuests.length; i++)
        {
            questNames[i] = groundquests.questGetQuestName(refreshedQuests[i]);
        }

        // Update the quest list stored as an objVar.
        setObjVar(self, "dev.questList", refreshedQuests);

        // Display the refreshed quest menu.
        listbox(self, self,
                "Select a quest to manage:",
                OK_CANCEL, "Quests", questNames,
                "handleQuestOptionSelection", true, false);

        return SCRIPT_CONTINUE;
    }

    public void modifyCollection(obj_id self) throws InterruptedException
    {
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return;
        }
        String[] OPTIONS = {
                "Complete Collection",
                "Reset Collection"
        };
        listbox(self, self, "Select an option to perform on the selected player's collection.", OK_CANCEL, "Options", OPTIONS, "handleCollectionOptionSelection", true, false);
    }

    public int handleCollectionOptionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        if (idx == 0)
        {
            String[] validCollections = collection.getAllCollectionPagesInBook("collection_book");
            listbox(self, self, "Select a collection to complete.", OK_CANCEL, "Collections", validCollections, "handleCompleteCollectionSelection", true, false);
        }
        else if (idx == 1)
        {
            String[] validCollections = collection.getAllCollectionPagesInBook("collection_book");
            listbox(self, self, "Select a collection to reset.", OK_CANCEL, "Collections", validCollections, "handleResetCollectionSelection", true, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleCompleteCollectionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        String[] validCollections = collection.getAllCollectionPagesInBook("collection_book");
        String collectionPage = validCollections[idx];
        String[] slots = collection.getAllCollectionSlotsInCollection(collectionPage);
        broadcast(self, "Completing collection " + collectionPage + " for " + getPlayerFullName(target));
        for (String slot : slots)
        {
            modifyCollectionSlotValue(target, slot, getCollectionSlotMaxValue(slot));
            broadcast(self, "Completed slot " + slot + " for collection " + collectionPage + " for " + getPlayerFullName(target));
        }
        return SCRIPT_CONTINUE;
    }

    public int handleResetCollectionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        String[] validCollections = collection.getAllCollectionPagesInBook("collection_book");
        String collectionPage = validCollections[idx];
        String[] slots = collection.getAllCollectionSlotsInCollection(collectionPage);
        broadcast(self, "Resetting collection " + collectionPage + " for " + getPlayerFullName(target));
        for (String slot : slots)
        {
            modifyCollectionSlotValue(target, slot, 0);
            broadcast(self, "Reset slot " + slot + " for collection " + collectionPage + " for " + getPlayerFullName(target));
        }
        return SCRIPT_CONTINUE;
    }

    public void assignBadge(obj_id self) throws InterruptedException
    {
        String[] badgePages = getAllCollectionPagesInBook("badge_book");
        listbox(self, self, "Select a badge to assign.", OK_CANCEL, "Badges", badgePages, "handleGrantBadgeSelection", true, false);
    }

    public int handleGrantBadgeSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        String[] badgePages = getAllCollectionPagesInBook("collection_book");
        String badgePage = badgePages[idx];
        setScriptVar(self, "dev-selectedBadgePage", badgePage);
        String prompt = "Regarding badge " + badgePage + ", select what you would like to do for " + getPlayerFullName(target) + "?";
        String[] OPTIONS = {
                "Grant",
                "Revoke"
        };
        listbox(self, self, prompt, OK_CANCEL, "Options", OPTIONS, "handleGrantBadgeOptionSelection", true, false);
        return SCRIPT_CONTINUE;
    }

    public int handleGrantBadgeOptionSelection(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        String badgePage = getStringScriptVar(self, "dev-selectedBadgePage");
        if (badgePage == null || badgePage.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        if (idx == 0)
        {
            badgeAssign(target, badgePage);
        }
        else if (idx == 1)
        {
            badge.revokeBadge(target, badgePage, true);
        }
        return SCRIPT_CONTINUE;
    }

    private void setupEnhance(obj_id self) throws InterruptedException
    {
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return;
        }
        String[] OPTIONS = {
                "Medic Buff",
                "Inspiration Buff",
                "Rally Banner Buff",
                "(All)",
        };
        listbox(self, self, "Select an enhancement to grant.", OK_CANCEL, "Enhancements", OPTIONS, "handleEnhanceSelection", true, false);
    }

    public int handleEnhanceSelection(obj_id self, dictionary params) throws InterruptedException
    {
        String[] OPTIONS = {
                "Medic Buff",
                "Officer Buff",
                "Rally Banner Buff",
                "(All)",
        };
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            clearSelectedPlayer(self);
            return SCRIPT_CONTINUE;
        }
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id target = getObjIdScriptVar(self, "dev-selectedPlayer");
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }
        switch (idx)
        {
            case 0:
                buff.applyBuff(target, "me_buff_health_2", 7200f, 245);
                buff.applyBuff(target, "me_buff_action_3", 7200f, 245);
                buff.applyBuff(target, "me_buff_strength_3", 7200f, 75);
                buff.applyBuff(target, "me_buff_agility_3", 7200f, 75);
                buff.applyBuff(target, "me_buff_precision_3", 7200f, 75);
                buff.applyBuff(target, "me_buff_melee_gb_1", 7200f, 10);
                buff.applyBuff(target, "me_buff_ranged_gb_1", 7200f, 5);
                break;
            case 1:
                buff.applyBuff(target, "of_focus_fire_6", 7200);
                buff.applyBuff(target, "of_tactical_drop_6", 7200);
                break;
            case 2:
                buff.applyBuff(target, "banner_buff_commando", 7200);
                buff.applyBuff(target, "banner_buff_smuggler", 7200);
                buff.applyBuff(target, "banner_buff_medic", 7200);
                buff.applyBuff(target, "banner_buff_officer", 7200);
                buff.applyBuff(target, "banner_buff_spy", 7200);
                buff.applyBuff(target, "banner_buff_bounty_hunter", 7200);
                buff.applyBuff(target, "banner_buff_force_sensitive", 7200);
                break;
            case 3:
                buff.applyBuff(target, "me_buff_health_2", 7200f, 245);
                buff.applyBuff(target, "me_buff_action_3", 7200f, 245);
                buff.applyBuff(target, "me_buff_strength_3", 7200f, 75);
                buff.applyBuff(target, "me_buff_agility_3", 7200f, 75);
                buff.applyBuff(target, "me_buff_precision_3", 7200f, 75);
                buff.applyBuff(target, "me_buff_melee_gb_1", 7200f, 10);
                buff.applyBuff(target, "me_buff_ranged_gb_1", 7200f, 5);
                buff.applyBuff(target, "of_focus_fire_6", 7200);
                buff.applyBuff(target, "of_tactical_drop_6", 7200);
                buff.applyBuff(target, "banner_buff_commando", 7200);
                buff.applyBuff(target, "banner_buff_smuggler", 7200);
                buff.applyBuff(target, "banner_buff_medic", 7200);
                buff.applyBuff(target, "banner_buff_officer", 7200);
                buff.applyBuff(target, "banner_buff_spy", 7200);
                buff.applyBuff(target, "banner_buff_bounty_hunter", 7200);
                buff.applyBuff(target, "banner_buff_force_sensitive", 7200);
                break;
        }
        listbox(self, self, "Select an enhancement to grant or hit cancel to close.", OK_CANCEL, "Enhancements", OPTIONS, "handleEnhanceSelection", true, false);
        return SCRIPT_CONTINUE;
    }

    public void clearSelectedPlayer(obj_id self) throws InterruptedException
    {
        removeScriptVar(self, "dev-selectedPlayer");
    }

    public int setupGrantBadge(obj_id self, dictionary params)
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public void badgeAssign(obj_id player, String badgeName) throws InterruptedException
    {
        if (!badge.hasBadge(player, badgeName))
        {
            badge.grantBadge(player, badgeName);
        }
        else
        {
            broadcast(getSelf(), "Player already has badge " + badgeName + ".");
        }
    }

    private void setupGiveMoney(obj_id self) throws InterruptedException
    {
        inputbox(self, self, "Enter the amount of money to give.", OK_CANCEL, "Credits", INPUT_NORMAL, null, "handleGiveMoneyInput", null);
    }

    private void setupGiveItem(obj_id self) throws InterruptedException
    {
        inputbox(self, self, "Enter the static item template to give.", OK_CANCEL, "Static Items", INPUT_NORMAL, null, "handleGiveItemInput", null);
    }

    public int handleGiveItemInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String itemTemplate = getInputBoxText(params);
        if (itemTemplate.endsWith(".iff"))
        {
            broadcast(self, "String input must be a static item!");
            inputbox(self, self, "Enter the static item template to give.", OK_CANCEL, "Item Template", INPUT_NORMAL, null, "handleGiveItemInput", null);
            return SCRIPT_CONTINUE;
        }
        if (itemTemplate.isEmpty())
        {
            broadcast(self, "You must enter a valid item template or select 'Cancel'.");
            inputbox(self, self, "Enter the static item template to give.", OK_CANCEL, "Item Template", INPUT_NORMAL, null, "handleGiveItemInput", null);
            return SCRIPT_CONTINUE;
        }
        obj_id player = getObjIdScriptVar(self, "dev-selectedPlayer");
        obj_id item = static_item.createNewItemFunction(itemTemplate, player);
        if (isIdValid(item))
        {
            sendSystemMessageTestingOnly(self, "Item " + itemTemplate + " created in " + getPlayerFullName(player) + "'s inventory.");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleGiveMoneyInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String moneys = getInputBoxText(params);
        if (moneys == null || moneys.isEmpty())
        {
            broadcast(self, "You must enter a valid amount of credits or select 'Cancel'.");
            inputbox(self, self, "Enter the amount of money to give.", OK_CANCEL, "Credits", INPUT_NORMAL, null, "handleGiveMoneyInput", null);
            return SCRIPT_CONTINUE;
        }
        obj_id player = getObjIdScriptVar(self, "dev-selectedPlayer");
        moneys = moneys.replaceAll("[^0-9]", "");
        int amount = stringToInt(moneys);
        if (amount == 0)
        {
            broadcast(self, "You must enter a valid amount of credits or select 'Cancel'.");
            inputbox(self, self, "Enter the amount of money to give.", OK_CANCEL, "Credits", INPUT_NORMAL, null, "handleGiveMoneyInput", null);
            return SCRIPT_CONTINUE;
        }
        dictionary d = new dictionary();
        d.put("payoutTarget", player);
        money.systemPayout(money.ACCT_BETA_TEST, player, amount, "handlePayoutToPlayer", d);
        broadcast(self, "Gave " + amount + " credits to " + getPlayerFullName(player) + ".");
        return SCRIPT_CONTINUE;
    }

    private void cleanUpJail(obj_id self, obj_id target)
    {
        obj_id[] jailContents = getAllObjectsWithObjVar(getLocation(target), 100, "gm_jail");
        for (obj_id jailContent : jailContents)
        {
            destroyObject(jailContent);
        }
        broadcast(self, "Jail cleaned up.");
    }

    private void handleJail(obj_id self, obj_id target)
    {
        String wallTemplate = "object/static/structure/tatooine/wall_pristine_tatooine_large_style_01.iff";
        String oolumnTemplate = "object/static/structure/tatooine/pillar_pristine_large_style_01.iff";
        String flattener = "object/building/poi/generic_flatten_small.iff";
        String filterObjvar = "gm_jail";
        //spawn 5 meters out from each side and rotate them
        location here = getLocation(target);
        location wallOne = new location(here);
        wallOne.x = wallOne.x + 0;
        wallOne.z = wallOne.z + 4;
        obj_id jailWallOne = createObject(wallTemplate, wallOne);
        location wallTwo = new location(here);
        wallTwo.x = wallTwo.x - 4;
        wallTwo.z = wallTwo.z - 0;
        obj_id jailWallTwo = createObject(wallTemplate, wallTwo);
        location wallThree = new location(here);
        wallThree.x = wallThree.x + 0;
        wallThree.z = wallThree.z - 4;
        obj_id jailWallThree = createObject(wallTemplate, wallThree);
        location wallFour = new location(here);
        wallFour.x = wallFour.x + 4;
        wallFour.z = wallFour.z + 0;
        obj_id jailWallFour = createObject(wallTemplate, wallFour);
        //columns
        location columnOne = new location(here);
        columnOne.x = columnOne.x - 3.25f;
        columnOne.z = columnOne.z + 4.75f;
        obj_id jailColumnOne = createObject(oolumnTemplate, columnOne);
        location columnTwo = new location(here);
        columnTwo.x = columnTwo.x + 4.75f;
        columnTwo.z = columnTwo.z + 4.75f;
        obj_id jailColumnTwo = createObject(oolumnTemplate, columnTwo);
        location columnThree = new location(here);
        columnThree.x = columnThree.x + 4.75f;
        columnThree.z = columnThree.z - 3.25f;
        obj_id jailColumnThree = createObject(oolumnTemplate, columnThree);
        location columnFour = new location(here);
        columnFour.x = columnFour.x - 3.25f;
        columnFour.z = columnFour.z - 3.25f;
        obj_id jailColumnFour = createObject(oolumnTemplate, columnFour);
        obj_id[] jailContents = {jailWallOne, jailWallTwo, jailWallThree, jailWallFour, jailColumnOne, jailColumnTwo, jailColumnThree, jailColumnFour};
        for (obj_id jailContent : jailContents)
        {
            setObjVar(jailContent, filterObjvar, 1);
        }
        setYaw(jailWallOne, 90);
        setYaw(jailWallTwo, 180);
        setYaw(jailWallThree, 90);
        setYaw(jailWallFour, 180);
        broadcast(self, "Jail created.");
    }

    public int setupIncrementInputBox(obj_id self) throws InterruptedException
    {
        inputbox(self, self, "Enter an increment value", OK_CANCEL, "Enter an increment value", INPUT_NORMAL, null, "handleIncrementInputBox", null);
        return SCRIPT_CONTINUE;
    }

    public int handleIncrementInputBox(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int btn = getIntButtonPressed(params);
        if (btn == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        String input = getInputBoxText(params);
        if (input == null || input.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        float increment = stringToFloat(input);
        if (increment == 0)
        {
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "decorationPanel.increment", increment);
        return SCRIPT_CONTINUE;
    }

    private void warpPlayer(obj_id self, location location)
    {
        warpPlayer(self, location.area, location.x, location.y, location.z, location.cell, location.x, location.y, location.z);
        broadcast(self, "Warping to: " + location.toReadableFormat(true));
    }

    public string_id unlocalized(String string)
    {
        return new string_id(string);
    }

    public String getRandomHumanName(obj_id self)
    {
        return npc.generateRandomName("object/creature/player/human_male.iff");
    }

    public String getRandomName(obj_id self, String race, boolean isMale)
    {
        String gender;
        if (isMale)
        {
            gender = "male";
        }
        else
        {
            gender = "female";
        }
        return npc.generateRandomName("object/creature/player/" + race + "_" + gender + ".iff");
    }

    private boolean echo(obj_id self, String s)
    {
        return sendConsoleCommand("/echo " + s, self);
    }

    private void reloadAllScripts(obj_id self)
    {
        debugServerConsoleMsg(self, "Reloading all scripts");
        String[] scripts = getScriptList(self);
        for (String script : scripts)
        {
            script.replace("script.", "");
            reloadScript(script);
            broadcast(self, "Attempting to reload " + script);
        }
    }

    public int handleShellOutput(obj_id self, dictionary params)
    {
        broadcast(self, "handleShellOutput -- we are here");
        return SCRIPT_CONTINUE;
    }

    public int getPlayerLevel(obj_id self, obj_id target, String params, float defaultTime)
    {
        int level = getLevel(target);
        debugSpeakMsg(self, "Your level is " + level);
        return SCRIPT_CONTINUE;
    }

    public location getOffsetFromRoot(obj_id self, obj_id structure_attachment)
    {
        location loc = getLocation(structure_attachment);
        location root = getLocation(self);
        return new location(loc.x - root.x, loc.y - root.y, loc.z - root.z, loc.area);
    }

    public void putPlayersInRing(obj_id[] targets, float ring_radius)
    {
        for (obj_id who : targets)
        {
            if (isIdValid(who))
            {
                location where = getLocation(who);
                float angle = rand(0, 360);
                float x = ring_radius * (float) Math.cos(angle);
                float y = ring_radius * (float) Math.sin(angle);
                where.x = where.x + x;
                where.y = where.y + y;
                setLocation(who, where);
            }
        }
    }

    public void pathToMe(obj_id self, obj_id target)
    {
        location here = getLocation(self);
        location there = getLocation(target);
        createClientPathAdvanced(target, there, here, "default");
    }

    public void pathToWho(obj_id self, obj_id target)
    {
        location here = getLocation(self);
        location there = getLocation(target);
        createClientPathAdvanced(self, here, there, "default");
    }

    public int OnLogin(obj_id self) throws InterruptedException
    {
        location here = new location(getLocation(self));
        debugConsoleMsg(self, "Cluster: " + getClusterName());
        debugConsoleMsg(self, "Planet: " + getCurrentSceneName());
        debugConsoleMsg(self, "Buildout Area: " + getBuildoutAreaName(here.x, here.z));
        debugConsoleMsg(self, "God Mode: " + (isGod(self) ? "Enabled" : "Disabled"));
        debugConsoleMsg(self, "Invulnerable: " + (isInvulnerable(self) ? "Enabled" : "Disabled"));
        debugConsoleMsg(self, "AI Ignore: " + (ai_lib.isAttackable(self) ? "Disabled" : "Enabled"));
        debugConsoleMsg(self, "Visibility: " + (getCreatureCoverVisibility(self) ? "Visible" : "Hidden"));
        LOG("ethereal", "[Developer]: " + getPlayerFullName(self) + " logged in with developer.bubbajoe.player_developer at " + here.toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int OnLogout(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    public int OnAddedToWorld(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    private void spawnRingInterior(obj_id self, int num, float radius, location where, String creatureToSpawn) throws InterruptedException
    {
        float x = where.x;
        float y = where.y;
        float z = where.z;
        float angle = 0;
        float angleInc = 360.0f / num;
        for (int i = 0; i < num; i++)
        {
            angle = angle + angleInc;
            float newX = x + (float) Math.cos(angle) * radius;
            float newY = y + (float) Math.sin(angle) * radius;
            location newLoc = new location(newX, newY, z, where.area, where.cell);
            obj_id creature = create.object(creatureToSpawn, newLoc);
            if (isIdValid(creature))
            {
                setScale(creature, 0.5f);
            }
        }
    }

    private location getO2P(obj_id self, obj_id target)
    {
        location here = getLocation(self);
        location there = getLocation(target);
        float x = here.x - there.x;
        float y = here.y - there.y;
        float z = here.z - there.z;
        return new location(x, y, z);
    }

    private location blindAxis(obj_id self, obj_id target)
    {
        location here = getLocation(self);
        location there = getLocation(target);
        float y = here.y - there.y;
        return new location(there.x, getHeightAtLocation(there.x, there.z), there.x);
    }

    public int handleDescribe(obj_id self, dictionary paramsDict) throws InterruptedException
    {
        obj_id myTarget = getIntendedTarget(self);
        String descInput = getInputBoxText(paramsDict);
        if (descInput == null || descInput.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        string_id desc = new string_id(descInput);
        setDescriptionStringId(myTarget, desc);
        setObjVar(myTarget, "null_desc", descInput);
        attachScript(myTarget, "developer.bubbajoe.sync");
        return SCRIPT_CONTINUE;
    }

    public int sendFakeMail(obj_id source, obj_id[] recipients, String from, String title, String body, boolean attachments) throws InterruptedException
    {
        location here = getLocation(source);
        for (obj_id target : recipients)
        {
            if (!attachments)
            {
                sendMail(unlocalized(title), unlocalized(body), target, from);
            }
            else
            {
                String waypointName = "Area of Interest";
                String oob;
                oob = chatAppendPersistentMessageWaypointData(null, here.area, here.x, here.z, null, waypointName);
                chatSendPersistentMessage(from, target, title, body, oob);
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int addDirtyPlanetMapLocation(obj_id self, String name, location where, String category, String subcategory)
    {
        addPlanetaryMapLocation(self, name, (int) where.x, (int) where.y, category, subcategory, MLT_PERSIST, 0);
        broadcast(self, "Added " + name + " to your planetary map under " + category + " " + subcategory);
        return SCRIPT_CONTINUE;
    }

    public int removeDirtyPlanetMapLocation(obj_id self, String name)
    {
        removePlanetaryMapLocation(self);
        broadcast(self, "Removed " + name + " from your planetary map");
        return SCRIPT_CONTINUE;
    }

    public void handleItemLookup(obj_id self, String staticItem) throws InterruptedException
    {
        dictionary itemData = dataTableGetRow("datatables/item/master_item/master_item.iff", staticItem);
        String header = "Static Item Details for: " + staticItem + "\n\n";
        String version = "Version: " + itemData.get("version") + "\n";
        String string_name = "Canonical Name: " + itemData.getString("string_name") + "\n";
        String string_details = "Canonical Description: " + itemData.getString("string_details") + "\n";
        String template_name = "Template Name: " + itemData.get("template_name") + "\n";
        String type = "Type: " + itemData.get("type") + "\n";
        String unique = "Unique " + itemData.get("unique") + "\n";
        String required_level = "Required Level: " + itemData.get("required_level") + "\n";
        String required_skill = "Required Skill: " + itemData.get("required_skill") + "\n";
        String creation_objvars = "Creation Objvars: " + itemData.get("creation_objvars") + "\n";
        String charges = "Charges: " + itemData.get("charges") + "\n";
        String tier = "Tier: " + itemData.get("tier") + "\n";
        String value = "Value: " + itemData.get("value") + "\n";
        String scriptHeader = "Scripts: \n";
        StringBuilder scriptsList = new StringBuilder();
        String scripts = itemData.get("scripts") + "\n";
        String[] formattedScriptList = scripts.split(",");
        for (String s : formattedScriptList)
        {
            scriptsList = new StringBuilder("\t" + scriptsList + s + "\n\t");
        }
        String reverse_engineer = "Can Reverse Engineer: " + itemData.get("can_reverse_engineer") + "\n\n";
        String comments = "Comments: " + itemData.get("comments") + "\n";
        String payload = header + version + string_name + string_details + template_name + type + unique + required_level + required_skill + creation_objvars + charges + tier + value + scriptHeader + scriptsList + reverse_engineer + comments;
        int itemPid = msgbox(self, self, payload, OK_ONLY, header, "noHandler");
        setSUIProperty(itemPid, "Prompt.lblPrompt", "GetsInput", "true");
        setSUIProperty(itemPid, "Prompt.lblPrompt", "Editable", "true");
        setSUIProperty(itemPid, "Prompt.lblPrompt", "Font", "bold_22");
    }

    public void handleCreatureLookup(obj_id self, String creatureName) throws InterruptedException
    {
        dictionary itemData = dataTableGetRow("datatables/mob/creatures.iff", creatureName);
        String header = "Creature Details for: " + creatureName + "\n\n";
        final String[] columns = getCreatureColumns();
        StringBuilder output = new StringBuilder();
        for (String c : columns)
        {
            output.append(toUpper(c, 0)).append(": ").append(itemData.get(c)).append("\n\n");
        }
        int creaturePid = msgbox(self, self, output.toString(), OK_ONLY, header, "noHandler");
        setSUIProperty(creaturePid, MSGBOX_BTN_OK, PROP_TEXT, "Close");
        setSUIProperty(creaturePid, "Prompt.lblPrompt", "GetsInput", "true");
        setSUIProperty(creaturePid, "Prompt.lblPrompt", "Editable", "true");
        setSUIProperty(creaturePid, "Prompt.lblPrompt", "Font", "bold_22");
    }

    public int generateFactoryCrate(obj_id self, obj_id inventory, obj_id manufacturing_schematic)
    {
        if (!isIdValid(inventory) || !exists(inventory))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(manufacturing_schematic) || !exists(manufacturing_schematic))
        {
            return SCRIPT_CONTINUE;
        }
        String crateTemplate = "object/factory/factory_crate_electronics.iff";
        obj_id crate = createObject(crateTemplate, inventory, "");
        setObjVar(crate, "crafting.source_schematic", manufacturing_schematic);
        setObjVar(crate, "crafting.crafting_attributes.crafting:charges", rand(3, 9));
        setObjVar(crate, "crafting.crafting_attributes.crafting:complexity", 100f);
        setObjVar(crate, "crafting.crafting_attributes.crafting:hitPoints", 1000f);
        setObjVar(crate, "crafting.crafting_attributes.crafting:xp", rand(1f, 100f));
        setObjVar(crate, "draftSchematic", getDraftSchematicCrc(manufacturing_schematic));
        setCount(crate, 1000);
        return SCRIPT_CONTINUE;
    }

    public obj_id[] getAllPlayers(obj_id self, float range)
    {
        return getPlayerCreaturesInRange(getLocation(self), range);
    }

    public obj_id[] getAllTangibleObjects(obj_id self, float range)
    {
        List<String> myList = new ArrayList<>();
        obj_id[] allObjects = getObjectsInRange(getLocation(self), range);
        for (obj_id index : allObjects)
        {
            if (getTemplateName(index).startsWith("object/tangible/"))
            {
                myList.add(index.toString());
            }
        }

        return myList.toArray(new obj_id[0]);
    }

    public obj_id[] getAllStaticObjects(obj_id self, float range)
    {
        List<String> myList = new ArrayList<>();
        obj_id[] allObjects = getObjectsInRange(getLocation(self), range);
        for (obj_id index : allObjects)
        {
            if (getTemplateName(index).startsWith("object/static/"))
            {
                myList.add(index.toString());
            }
        }

        return myList.toArray(new obj_id[0]);
    }

    public obj_id[] getAllPlayerHouses(obj_id self, float range)
    {
        List<String> myList = new ArrayList<>();
        obj_id[] allObjects = getObjectsInRange(getLocation(self), range);
        for (obj_id index : allObjects)
        {
            if (getTemplateName(index).startsWith("object/building/player/"))
            {
                myList.add(index.toString());
            }
        }
        return myList.toArray(new obj_id[0]);
    }

    public boolean isDataTableEmpty(String table)
    {
        return dataTableGetNumRows(table) == 0;
    }

    public int saveTextOnClientNoFormatting(obj_id self, String text)
    {
        return SCRIPT_CONTINUE;
    }

    public int exportHousingContents(obj_id who, obj_id building, String filename) throws InterruptedException
    {
        StringBuilder fileContent = new StringBuilder();
        obj_id[] cells = getCellIds(building);

        for (obj_id cell : cells)
        {
            int count = 0;
            obj_id[] contents = getContents(cell);

            for (obj_id content : contents)
            {
                // Skip AI, creatures, and locked objects
                if (hasScript(content, "ai.ai") || hasScript(content, "ai.beast") || hasScript(content, "ai.creature_combat") || hasScript(content, "ai.pet") || hasScript(content, "system.bookworm.book"))
                {
                    continue;
                }

                if (hasObjVar(content, "exportLock"))

                {
                    continue;
                }

                String template = getTemplateName(content);
                if (isBadTemplate(template))
                {
                    broadcast(who, "Amenity detected, skipping item export entry...");
                    continue;
                }

                location loc = getLocation(content);
                transform rotation = getTransform_o2p(content);

                // Build the content string with only the required fields
                StringBuilder contentString = new StringBuilder();
                contentString.append(getCellName(getContainedBy(content)))  // Parent cell name
                        .append("\t").append(template)  // Template
                        .append("\t").append(loc.x)     // X coordinate
                        .append("\t").append(loc.y)     // Y coordinate
                        .append("\t").append(loc.z)     // Z coordinate
                        .append("\t").append(rotation.toColonString()); // Rotation

                // Append objvars if available
                String objVarList = getPackedObjvars(content);
                if (objVarList != null && !objVarList.isEmpty())
                {
                    contentString.append("\t").append(objVarList);  // Packed objvars
                }
                else
                {
                    contentString.append("\t"); // Maintain structure
                }

                // Append scripts if available and separate by commas
                String scripts = getPackedScripts(content);
                if (!scripts.isEmpty())
                {
                    contentString.append("\t").append(scripts.replace("\t", ","));  // Replace tabs with commas for scripts
                }
                else
                {
                    contentString.append("\t"); // Maintain structure
                }

                fileContent.append(contentString).append("\n");
                count++;

                LOG("ethereal", "[Housing Exporter]: " + contentString + " added to " + filename);
            }

            broadcast(who, "Exported " + count + " items from " + getName(building) + " to " + filename);
            LOG("ethereal", "[Housing Exporter]: " + contents.length + " items from " + getName(building) + " to " + filename);
        }

        // Write the export content to a file
        try
        {
            FileWriter fw = new FileWriter("/home/swg/swg-main/exe/linux/server/housing_exports/" + filename + ".house");
            fw.write(fileContent.toString());
            fw.close();
        } catch (IOException e)
        {
            LOG("ethereal", "[Housing Exporter]: " + e);
        }

        // Save the exported data on the client
        saveTextOnClient(who, filename, fileContent.toString());

        return SCRIPT_CONTINUE;
    }

    private boolean isBadTemplate(String template)
    {
        if (template.startsWith("object/creature/"))
        {
            return true;
        }
        if (template.equals("object/tangible/terminal/terminal_player_structure.iff"))
        {
            return true;
        }
        return template.startsWith("object/mobile/");
    }

    public int importHousingContents(obj_id who, obj_id building, String filename)
    {
        String filePath = "/home/swg/swg-main/exe/linux/server/housing_exports/" + filename;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath)))
        {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue; // Skip empty lines or comments
                }

                try
                {
                    String[] data = line.split("\t");

                    if (data.length < 7)
                    {
                        LOG("ethereal", "[Housing Importer]: Invalid line format at line " + lineNumber + ": " + line);
                        continue; // Skip invalid lines
                    }

                    String cellName = data[0].trim();
                    String template = data[1].trim();
                    float locX = Float.parseFloat(data[2]);
                    float locY = Float.parseFloat(data[3]);
                    float locZ = Float.parseFloat(data[4]);
                    String rotationStr = data[5].trim();

                    float[][] matrix = parseMatrixRotation(rotationStr);
                    if (matrix == null)
                    {
                        LOG("ethereal", "[Housing Importer]: Invalid matrix format at line " + lineNumber + ": " + rotationStr);
                        continue; // Skip invalid matrix data
                    }

                    transform rotation = new transform(toTransform(matrix));
                    String objvars = data.length > 7 ? data[7].trim() : "";
                    String scripts = data.length > 8 ? data[8].trim() : "";

                    // Sanitize objvars by removing "\#"
                    objvars = objvars.replace("\\#", "");

                    if (!validateObjvars(objvars))
                    {
                        LOG("ethereal", "[Housing Importer]: Skipping invalid objvars at line " + lineNumber + ": " + objvars);
                        objvars = ""; // Default to empty objvars
                    }

                    obj_id cell = getCellId(building, cellName);
                    if (cell == null)
                    {
                        LOG("ethereal", "[Housing Importer]: Cell not found at line " + lineNumber + ": " + cellName);
                        continue; // Skip if cell is invalid
                    }

                    obj_id newObj = createObject(template, new location(locX, locY, locZ, getCurrentSceneName(), cell));
                    if (isIdValid(newObj))
                    {
                        setTransform_o2p(newObj, rotation);
                        setLocation(newObj, new location(locX, locY, locZ, getCurrentSceneName(), cell));

                        try
                        {
                            if (!objvars.isEmpty())
                            {
                                setPackedObjvars(newObj, objvars);
                            }
                        } catch (Exception e)
                        {
                            LOG("ethereal", "[Housing Importer]: Error setting objvars at line " + lineNumber + ": " + e.getMessage());
                        }

                        try
                        {
                            if (!scripts.isEmpty())
                            {
                                setPackedScripts(newObj, scripts);
                            }
                        } catch (Exception e)
                        {
                            LOG("ethereal", "[Housing Importer]: Error setting scripts at line " + lineNumber + ": " + e.getMessage());
                        }

                        LOG("ethereal", "[Housing Importer]: Object " + newObj + " imported at line " + lineNumber + " to location: " + locX + ", " + locY + ", " + locZ);
                    }
                    else
                    {
                        LOG("ethereal", "[Housing Importer]: Failed to create object from template at line " + lineNumber + ": " + template);
                    }
                } catch (NumberFormatException nfe)
                {
                    LOG("ethereal", "[Housing Importer]: Number format error at line " + lineNumber + ": " + nfe.getMessage());
                } catch (Exception e)
                {
                    LOG("ethereal", "[Housing Importer]: General error at line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e)
        {
            LOG("ethereal", "[Housing Importer]: File access error: " + e.getMessage());
            return SCRIPT_OVERRIDE;
        }

        return SCRIPT_CONTINUE;
    }

    // Validate objvars
    private boolean validateObjvars(String objvars)
    {
        return !(objvars.equals("$|") || objvars.isEmpty());
    }

    private float[][] parseMatrixRotation(String rotationStr)
    {
        String[] parts = rotationStr.split(":");
        if (parts.length == 12)
        {
            // Parse 3x4 matrix and convert to 4x4
            return new float[][]{
                    {Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])},
                    {Float.parseFloat(parts[4]), Float.parseFloat(parts[5]), Float.parseFloat(parts[6]), Float.parseFloat(parts[7])},
                    {Float.parseFloat(parts[8]), Float.parseFloat(parts[9]), Float.parseFloat(parts[10]), Float.parseFloat(parts[11])},
                    {0, 0, 0, 1} // Add the last row to form a 4x4 matrix
            };
        }
        else if (parts.length == 16)
        {
            // Parse 4x4 matrix directly
            return new float[][]{
                    {Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])},
                    {Float.parseFloat(parts[4]), Float.parseFloat(parts[5]), Float.parseFloat(parts[6]), Float.parseFloat(parts[7])},
                    {Float.parseFloat(parts[8]), Float.parseFloat(parts[9]), Float.parseFloat(parts[10]), Float.parseFloat(parts[11])},
                    {Float.parseFloat(parts[12]), Float.parseFloat(parts[13]), Float.parseFloat(parts[14]), Float.parseFloat(parts[15])}
            };
        }
        else
        {
            // Invalid matrix size
            return null;
        }
    }

    public transform toTransform(float[][] matrix)
    {
        float rotX = matrix[0][0];
        float rotY = matrix[1][0];
        float rotZ = matrix[2][0];
        float rotW = 1.0f; // Default or calculated value

        return new transform(rotX, rotY, rotZ, rotW);
    }

    private void setPackedScripts(obj_id newObj, String scripts)
    {
        String[] scriptList = scripts.split(",");
        for (String script : scriptList)
        {
            script = script.replace("script.", "");
            attachScript(newObj, script);
        }
    }

    private boolean isBadSpawn(String line)
    {
        String validator = "object/tangible/";
        return !line.contains(validator);
    }

    private transform parseTransform(String rotationStr)
    {
        String[] values = rotationStr.split(":");
        if (values.length != 4)
        {
            throw new IllegalArgumentException("Invalid rotation string. Expected 12 values.");
        }

        float rotX = Float.parseFloat(values[0]);
        float rotY = Float.parseFloat(values[1]);
        float rotZ = Float.parseFloat(values[2]);
        float rotW = Float.parseFloat(values[3]);

        return new transform(rotX, rotY, rotZ, rotW);
    }

    public obj_id[] getAllObjectsByFilter(obj_id self, float range, String filter)
    {
        List<String> myList = new ArrayList<>();
        obj_id[] allObjects = getObjectsInRange(getLocation(self), range);
        for (obj_id index : allObjects)
        {
            if (getTemplateName(index).startsWith(filter))
            {
                myList.add(index.toString());
            }
        }
        return myList.toArray(new obj_id[0]);
    }

    public int lootArea(obj_id player, float range) throws InterruptedException
    {
        location where = getLocation(player);
        obj_id[] targets = getAllObjectsWithObjVar(where, range, "readyToLoot");
        HashSet<obj_id> contents = new HashSet<>();

        for (obj_id persons : targets)
        {
            if (isMob(persons))
            {
                obj_id[] corpseContents = getContents(getInventoryContainer(persons));
                if (corpseContents.length == 0)
                {
                    destroyObject(persons);
                }
                else
                {
                    for (obj_id indi : corpseContents)
                    {
                        putIn(indi, getInventoryContainer(player));
                        if (!getTemplateName(indi).contains("loot_cash"))
                        {
                            broadcast(player, "You have looted " + getName(indi));
                            contents.add(indi);
                        }
                    }
                }
            }
        }

        showLootBox(player, contents.toArray(new obj_id[0]));

        return SCRIPT_CONTINUE;
    }

    public boolean isTester(obj_id player)
    {
        return hasObjVar(player, "test_center") || isGod(player);
    }

    public boolean isPlayerDualBoxed(obj_id poi1, obj_id poi2)
    {
        long stationId = getPlayerStationId(poi1);
        long stationId2 = getPlayerStationId(poi2);
        if (stationId == stationId2)
        {
            LOG("ethereal", "Player " + getPlayerFullName(poi1) + " and " + getPlayerFullName(poi2) + " are dual-logged.");
            return true;
        }
        else return false;
    }

    public void sendDiscordMessage(String webhookURL, String message, String avatarPic, String avatarName)
    {
        LOG("DiscordX", "Attempting to send a discord webhook to to | " + webhookURL + " | with the message of " + message);
        try
        {
            URL url = new URL(webhookURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            String contents = "{\"username\": \"" + avatarName + "\", \"avatar_url\": \"" + avatarPic + "\",\"content\": \"" + message + "\"}";
            try (OutputStream outputStream = connection.getOutputStream())
            {
                outputStream.write(contents.getBytes());
            }
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT)
            {
                LOG("DiscordX", "Webhook executed successfully!");
            }
            else
            {
                LOG("DiscordX", "Failed to execute webhook. Response code: " + responseCode);
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream())))
                {
                    String errorResponse;
                    while ((errorResponse = errorReader.readLine()) != null)
                    {
                        System.out.println(errorResponse);
                    }
                }
            }

            connection.disconnect();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public int sendFeedToDiscord(obj_id self, dictionary params) throws InterruptedException
    {
        String text = getInputBoxText(params);
        if (text == null || text.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        int button = getIntButtonPressed(params);
        if (button == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        if (button == BP_OK)
        {
            titan_player.sendLiveEventUpdateToDiscord(self, text);
        }
        return SCRIPT_CONTINUE;
    }

    public String color(String color, String text)
    {
        return "\\#" + color + text + "\\#.";
    }

    public int startDecorationPanel(obj_id self)
    {
        int page = createSUIPage("/Script.decorationPanel", self, self);
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnNorth", "onMoveNorth");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnSouth", "onMoveSouth");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnEast", "onMoveEast");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnWest", "onMoveWest");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnUp", "onMoveUp");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnDown", "onMoveDown");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnRotateLeft", "onRotateLeft");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnRotateRight", "onRotateRight");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnRotateRoll", "onRotateRoll");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnRotatePitch", "onRotatePitch");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnMoveLeft", "onMoveLeft");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnMoveRight", "onMoveRight");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnMoveForward", "onMoveForward");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnMoveBackward", "onMoveBackward");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnSummon", "onSummon");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnCopyYaw", "onCopyYaw");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnClone", "onCloneObject");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnSetName", "onSetName");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnAttachScript", "onAttachingScript");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnDetachScript", "onDetachingScript");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnTemplate", "onSpawnTemplate");
        subscribeToSUIEvent(page, sui_event_type.SET_onTextbox, "txtInput", "onValueChanged");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnSetName", "txtName", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnSetIncrement", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnNorth", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnSouth", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnEast", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnWest", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnUp", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnDown", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnRotateLeft", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnRotateRight", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnRotateRoll", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnRotatePitch", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnMoveLeft", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnMoveRight", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnMoveForward", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnMoveBackward", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnSummon", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnCopyYaw", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnClone", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnSetName", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnAttachScript", "txtScript", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnDetachScript", "txtScript", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnTemplate", "txtTemplate", "LocalText");
        setSUIAssociatedObject(page, self);
        boolean showResult = showSUIPage(page);
        if (!showResult)
        {
            broadcast(self, "Cannot display UI page '/Script.decorationPanel");
        }
        flushSUIPage(page);
        setObjVar(self, "decorationPanel.pid", page);
        return SCRIPT_CONTINUE;
    }

    public int onMoveNorth(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        location targetLoc = getLocation(target);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        targetLoc.z = targetLoc.z + increment;
        setLocation(target, targetLoc);
        LOG("ethereal", "Moving north by " + increment + " to " + targetLoc);
        return SCRIPT_CONTINUE;
    }

    public int onMoveSouth(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        location targetLoc = getLocation(target);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        targetLoc.z = targetLoc.z - increment;
        setLocation(target, targetLoc);
        LOG("ethereal", "Moving south by " + increment + " to " + targetLoc);
        return SCRIPT_CONTINUE;
    }

    public int onMoveEast(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        location targetLoc = getLocation(target);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        targetLoc.x = targetLoc.x + increment;
        setLocation(target, targetLoc);
        LOG("ethereal", "Moving east by " + increment + " to " + targetLoc);
        return SCRIPT_CONTINUE;
    }

    public int onMoveWest(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        location targetLoc = getLocation(target);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        targetLoc.x = targetLoc.x - increment;
        setLocation(target, targetLoc);
        LOG("ethereal", "Moving west by " + increment + " to " + targetLoc);
        return SCRIPT_CONTINUE;
    }

    public int onMoveUp(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        location targetLoc = getLocation(target);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        targetLoc.y = targetLoc.y + increment / 10;
        setLocation(target, targetLoc);
        LOG("ethereal", "Moving up by " + increment + " to " + targetLoc);
        return SCRIPT_CONTINUE;
    }

    public int onMoveDown(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        location targetLoc = getLocation(target);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        targetLoc.y = targetLoc.y - increment / 10;
        setLocation(target, targetLoc);
        LOG("ethereal", "Moving down by " + increment + " to " + targetLoc);
        LOG("ethereal", "params are: " + params);
        return SCRIPT_CONTINUE;
    }

    public int onRotateLeft(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        increment = increment * -1;
        float targetOrientation = getYaw(target);
        targetOrientation = targetOrientation + increment;
        setYaw(target, targetOrientation);
        LOG("ethereal", "Rotating left by " + increment + " to " + targetOrientation);
        return SCRIPT_CONTINUE;
    }

    public int onRotateRight(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        float increment = stringToFloat(params.getString("txtInput.LocalText"));
        float targetOrientation = getYaw(target);
        targetOrientation = targetOrientation + increment;
        setYaw(target, targetOrientation);
        LOG("ethereal", "Rotating right by " + increment + " to " + targetOrientation);
        return SCRIPT_CONTINUE;
    }

    public int onCopyYaw(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        float targetOrientation = getYaw(self);
        setYaw(target, targetOrientation);
        LOG("ethereal", "Copying yaw of " + targetOrientation);
        return SCRIPT_CONTINUE;
    }

    public int onSpawnTemplate(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        String template = params.getString("txtTemplate.LocalText");
        if (template.contains("shared_"))
        {
            template = template.replace("shared_", "");
        }
        create.object(template, getLocation(self));
        return SCRIPT_CONTINUE;
    }

    public int onSetName(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        String text = params.getString("txtName.LocalText");
        setName(target, text);
        LOG("ethereal", "Setting name to " + text);
        return SCRIPT_CONTINUE;
    }

    public int onAttachingScript(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        String text = params.getString("txtScript.LocalText");
        attachScript(target, text);
        LOG("ethereal", "Attaching script to " + text);
        return SCRIPT_CONTINUE;
    }

    public int onDetachingScript(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        obj_id target = getIntendedTarget(player);
        String text = params.getString("txtScript.LocalText");
        detachScript(target, text);
        LOG("ethereal", "Detaching script " + text);
        return SCRIPT_CONTINUE;
    }

    public int onMoveLeft(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        sendConsoleCommand("/moveFurniture left " + stringToInt(params.getString("txtInput.LocalText")), self);
        return SCRIPT_CONTINUE;
    }

    public int onMoveRight(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        sendConsoleCommand("/moveFurniture right " + stringToInt(params.getString("txtInput.LocalText")), self);
        return SCRIPT_CONTINUE;
    }

    public int onMoveForward(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        sendConsoleCommand("/moveFurniture forward " + stringToInt(params.getString("txtInput.LocalText")), self);
        return SCRIPT_CONTINUE;
    }

    public int onMoveBackward(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        sendConsoleCommand("/moveFurniture back " + stringToInt(params.getString("txtInput.LocalText")), self);
        return SCRIPT_CONTINUE;
    }

    public int onRotateRoll(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        sendConsoleCommand("/rotateFurniture roll " + stringToInt(params.getString("txtInput.LocalText")), self);
        return SCRIPT_CONTINUE;
    }

    public int onRotatePitch(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        sendConsoleCommand("/rotateFurniture pitch " + stringToInt(params.getString("txtInput.LocalText")), self);
        return SCRIPT_CONTINUE;
    }

    public int onCloneObject(obj_id self, dictionary params)
    {
        LOG("ethereal", "params are: " + params);
        String template = getTemplateName(getIntendedTarget(self));
        sendConsoleCommand("/spawn " + template + " 1 0 0", self);
        return SCRIPT_CONTINUE;
    }

    public int onSummon(obj_id self, dictionary params)
    {
        LOG("ethereal", "params are: " + params);
        location here = getLocation(self);
        setLocation(getIntendedTarget(self), here);
        LOG("ethereal", "Summoning target to " + here);
        return SCRIPT_CONTINUE;
    }

    public int onValueChanged(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "params are: " + params);
        obj_id player = getPlayerId(params);
        debugConsoleMsg(player, "New movement value is " + getInputBoxText(params));
        LOG("ethereal", "New movement value is " + getInputBoxText(params));
        return SCRIPT_CONTINUE;
    }

    public int getIncrement(obj_id self)
    {
        return (int) getFloatObjVar(self, "decorationPanel.increment");
    }

    public int startAdminPanel(obj_id self)
    {
        int page = createSUIPage("/Script.adminPanel", self, self);
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleVendorCosts", "onToggleVendorCosts");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleVis", "onToggleVis");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleGodMode", "onToggleGodMode");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleFrog", "onGiveFrog");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleRevive", "onRevive");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleHeal", "onHeal");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleHealTarget", "onHealTarget");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleBuff", "onBuff");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleReviveTarget", "onReviveTarget");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleDamageTarget", "onDamageTarget");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleDamage", "onDamageSelf");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleBuffOther", "onBuffTarget");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnWarpSave", "onWarpSave");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnWarpLoad", "onWarpLoad");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnWarpMenu", "onWarpMenuLoad");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "toggleBuffOP", "onBuffOP");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "toggleDamageTarget", "txtInput", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "speedScalar.btnClamp", "speedScalar.input", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "speedScalar.btnClamp", "speedScalar.slider", "Value");

        setSUIAssociatedObject(page, self);
        boolean showResult = showSUIPage(page);
        if (!showResult)
        {
            broadcast(self, "Cannot display UI page '/Script.adminPanel");
        }
        flushSUIPage(page);
        showSUIPage(page);
        return SCRIPT_CONTINUE;
    }

    public int onToggleVendorCosts(obj_id self, dictionary params)
    {
        if (hasObjVar(self, "vend"))
        {
            removeObjVar(self, "vend");
            broadcast(self, "Vendor costs enabled!");
        }
        else
        {
            setObjVar(self, "vend", 1);
            broadcast(self, "Vendor costs disabled!");
        }
        return SCRIPT_CONTINUE;
    }

    public int onToggleVis(obj_id self, dictionary params)
    {
        if (getCreatureCoverVisibility(self))
        {
            setCreatureCoverVisibility(self, false);
            broadcast(self, "Cover visibility disabled");
        }
        else
        {
            setCreatureCoverVisibility(self, true);
            broadcast(self, "Cover visibility enabled");
        }
        return SCRIPT_CONTINUE;
    }

    public int onWarpSave(obj_id self, dictionary params)
    {
        location here = getLocation(self);
        if (isInWorldCell(self))
        {
            setObjVar(self, "warpLoc", here);
            broadcast(self, "Saving admin warp to: " + here.toReadableFormat(true));
        }
        else
        {
            here.cell = getContainedBy(self);
            setObjVar(self, "warpLoc", here);
            broadcast(self, "Saving admin warp to: " + here.toReadableFormat(true));
        }
        return SCRIPT_CONTINUE;
    }

    public int onWarpLoad(obj_id self, dictionary params)
    {
        warpPlayer(self, getLocationObjVar(self, "warpLoc"));
        broadcast(self, "Warping to saved location.");
        return SCRIPT_CONTINUE;
    }

    public int onWarpMenuLoad(obj_id self, dictionary params)
    {
        broadcast(self, "implement warp menu from frog pls :)");
        return SCRIPT_CONTINUE;
    }

    public int onBuff(obj_id self, dictionary params) throws InterruptedException
    {
        buff.applyBuff(self, "me_buff_health_2", 7200);
        buff.applyBuff(self, "me_buff_action_3", 7200);
        buff.applyBuff(self, "me_buff_strength_3", 7200);
        buff.applyBuff(self, "me_buff_agility_3", 7200);
        buff.applyBuff(self, "me_buff_precision_3", 7200);
        buff.applyBuff(self, "me_buff_melee_gb_1", 7200);
        buff.applyBuff(self, "me_buff_ranged_gb_1", 7200);
        buff.applyBuff(self, "of_buff_def_9", 7200);
        buff.applyBuff(self, "of_focus_fire_6", 7200);
        buff.applyBuff(self, "of_tactical_drop_6", 7200);
        broadcast(self, "You have been buffed!");
        return SCRIPT_CONTINUE;
    }

    public int onBuffTarget(obj_id self, dictionary params) throws InterruptedException
    {
        String[] buffComponentKeys =
                {
                        "kinetic",
                        "energy",
                        "action_cost_reduction",
                        "dodge",
                        "strength",
                        "constitution",
                        "stamina",
                        "precision",
                        "agility",
                        "luck",
                        "critical_hit",
                        "healing",
                        "healer",
                        "reactive_go_with_the_flow",
                        "flush_with_success",
                        "reactive_second_chance",
                        "                        milk_quantity",
                        "milk_exceptional_chance",
                        "milk_stun",
                        "creature_harvesting",
                        "harvest_faire",
                        "reverse_engineering_chance",
                        "crafting",
                        "crafting_success",
                        "hand_sampling",
                        "resource_quality"

                };
        int[] buffComponentValues =
                {
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15,
                        15
                };
        obj_id target = getIntendedTarget(self);
        if (isPlayer(target))
        {
            float currentBuffTime = performance.inspireGetMaxDuration(target);
            setScriptVar(target, "performance.buildabuff.buffComponentKeys", buffComponentKeys);
            setScriptVar(target, "performance.buildabuff.buffComponentValues", buffComponentValues);
            buff.applyBuff(target, "buildabuff_inspiration", 7200);
            setScriptVar(target, "performance.buildabuff.player", target);
            buff.applyBuff(target, "me_buff_health_2", 7200, 250);
            buff.applyBuff(target, "me_buff_action_3", 7200, 250);
            buff.applyBuff(target, "me_buff_strength_3", 7200, 250);
            buff.applyBuff(target, "me_buff_agility_3", 7200, 250);
            buff.applyBuff(target, "me_buff_precision_3", 7200, 250);
            buff.applyBuff(target, "me_buff_melee_gb_1", 7200, 250);
            buff.applyBuff(target, "me_buff_ranged_gb_1", 7200, 250);
            buff.applyBuff(target, "of_buff_def_9", 7200, 250);
            buff.applyBuff(target, "of_focus_fire_6", 7200, 250);
            buff.applyBuff(target, "of_tactical_drop_6", 7200, 250);
            buff.applyBuff((target), "banner_buff_commando", 7200);
            buff.applyBuff((target), "banner_buff_smuggler", 7200);
            buff.applyBuff((target), "banner_buff_medic", 7200);
            buff.applyBuff((target), "banner_buff_officer", 7200);
            buff.applyBuff((target), "banner_buff_spy", 7200);
            buff.applyBuff((target), "banner_buff_bounty_hunter", 7200);
            buff.applyBuff((target), "banner_buff_force_sensitive", 7200);
            broadcast(target, "You have been buffed!");
            broadcast(self, "You have buffed " + getPlayerFullName(target));
        }
        return SCRIPT_CONTINUE;
    }

    public int onBuffOP(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id target = getIntendedTarget(self);
        if (isPlayer(target))
        {
            buff.applyBuff(self, "event_buff_gm", 1800, 250);
            buff.applyBuff(self, "event_buff_gm", 1800, 250);
            buff.applyBuff(self, "event_buff_gm", 1800, 250);
            buff.applyBuff(self, "event_buff_dev", 1800, 250);
            buff.applyBuff(self, "event_buff_dev", 1800, 250);
            buff.applyBuff(self, "event_buff_dev", 1800, 250);
        }
        broadcast(target, "You have been blessed by Bubba!");
        return SCRIPT_CONTINUE;
    }

    public int onToggleGodMode(obj_id self, dictionary params)
    {
        if (isGod(self))
        {
            setObjVar(self, "god", 1);
            sendConsoleCommand("/setGodMode off", self);
            broadcast(self, "God mode disabled.");
        }
        else
        {
            removeObjVar(self, "god");
            sendConsoleCommand("/setGodMode 50", self);
            broadcast(self, "God mode enabled.");
        }
        return SCRIPT_CONTINUE;
    }

    public void testWarp(obj_id self, location where)
    {
        sendConsoleCommand(where.toTeleportFormat(), self);
    }

    public int onGiveFrog(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id froggie = createObject("object/tangible/terminal/terminal_character_builder.iff", getInventoryContainer(self), "");
        setObjVar(froggie, "noTrade", 1);
        attachScript(froggie, "item.special.nomove");
        setBioLink(froggie, self);
        setObjVar(froggie, "locked", true);
        sendSystemMessageTestingOnly(self, "Character Builder Terminal granted. You must unlock it via the radial menu.");
        return SCRIPT_CONTINUE;
    }

    public int onRevive(obj_id self, dictionary params) throws InterruptedException
    {
        pclib.clearCombatData(self);
        buff.removeAllDebuffs(self);
        buff.refreshAllBuffs(self);
        removeObjVar(self, "combat.intIncapacitationCount");
        setPosture(self, POSTURE_UPRIGHT);
        queueCommand(self, (-1465754503), self, "", COMMAND_PRIORITY_IMMEDIATE);
        queueCommand(self, (-562996732), self, "", COMMAND_PRIORITY_IMMEDIATE);
        play2dNonLoopingSound(self, "sound/music_acq_healer.snd");
        removeScriptVar(self, "pvp_death");
        broadcast(self, "You have revived " + getName(self));
        return SCRIPT_CONTINUE;
    }

    public int onHeal(obj_id self, dictionary params)
    {
        int totalHealth = getMaxHealth(self);
        int totalAction = getMaxAction(self);
        setHealth(self, totalHealth);
        setAction(self, totalAction);
        broadcast(self, "Healed self for " + totalHealth + " HP");
        return SCRIPT_CONTINUE;
    }

    public int onHealTarget(obj_id self, dictionary params)
    {
        obj_id target = getTarget(self);
        int totalHealth = getMaxHealth(target);
        int totalAction = getMaxAction(self);
        setHealth(target, totalHealth);
        setAction(target, totalAction);
        if (isMob(target))
        {
            addToHealth(target, totalHealth);
        }
        broadcast(self, "Healed " + getPlayerFullName(target) + " for " + totalHealth + " HP");
        return SCRIPT_CONTINUE;
    }

    public int onReviveTarget(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id targetCreature = getIntendedTarget(self);
        pclib.clearCombatData(targetCreature);
        buff.removeAllDebuffs(targetCreature);
        buff.refreshAllBuffs(targetCreature);
        removeObjVar(targetCreature, "combat.intIncapacitationCount");
        setPosture(targetCreature, POSTURE_UPRIGHT);
        queueCommand(targetCreature, (-1465754503), targetCreature, "", COMMAND_PRIORITY_IMMEDIATE);
        queueCommand(targetCreature, (-562996732), targetCreature, "", COMMAND_PRIORITY_IMMEDIATE);
        play2dNonLoopingSound(targetCreature, "sound/music_acq_healer.snd");
        removeScriptVar(targetCreature, "pvp_death");
        broadcast(self, "You have revived " + getName(targetCreature));
        broadcast(targetCreature, "You have been revived.");
        return SCRIPT_CONTINUE;
    }

    public int onDamageTarget(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id targetCreature = getIntendedTarget(self);
        int damage = stringToInt(params.getString("txtInput.LocalText"));
        damage(targetCreature, DAMAGE_KINETIC, HIT_LOCATION_BODY, damage);
        broadcast(self, "You have damaged " + getName(targetCreature) + " for " + damage);
        return SCRIPT_CONTINUE;
    }

    public int onDamageSelf(obj_id self, dictionary params) throws InterruptedException
    {
        int damage = stringToInt(params.getString("speedScalar.input.LocalText"));
        damage(self, DAMAGE_KINETIC, HIT_LOCATION_BODY, damage);
        broadcast(self, "You have damaged yourself for " + damage);
        return SCRIPT_CONTINUE;
    }

    public int startInputAmountBox(obj_id self)
    {
        int page = createSUIPage("/Script.inputBoxAmt", self, self);
        subscribeToSUIProperty(page, "txtInput", "LocalText");
        subscribeToSUIProperty(page, "txtAmt", "LocalText");
        setSUIProperty(page, "txtInput", "GetsInput", "true");
        setSUIProperty(page, "txtInput", "Editable", "true");
        setSUIProperty(page, "txtAmt", "GetsInput", "true");
        setSUIProperty(page, "txtAmt", "Editable", "true");
        setSUIProperty(page, "txtInput", "MaxLength", "100");
        setSUIProperty(page, "txtAmt", "MaxLength", "100");
        setSUIProperty(page, "Prompt.lblPrompt", "LocalText", "Enter item template, and amount in their respective boxes.");
        setSUIProperty(page, "bg.caption.lblTitle", "LocalText", "QUANTITY ITEM GRANT");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnOk", "txtAmt", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnOk", "txtInput", "LocalText");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnOk", "onGiveItems");
        setSUIAssociatedObject(page, self);
        boolean showResult = showSUIPage(page);
        if (!showResult)
        {
            broadcast(self, "Cannot display UI page '/Script.inputBoxAmt");
        }
        flushSUIPage(page);
        return SCRIPT_CONTINUE;
    }

    public int onGiveItems(obj_id self, dictionary params) throws InterruptedException
    {
        int button = getIntButtonPressed(params);
        switch (button)
        {
            case BP_CANCEL:
                return SCRIPT_CONTINUE;
            case BP_OK:
                LOG("ethereal", "params are: " + params);
                String item = params.getString("txtInput.LocalText");
                int amount = stringToInt(params.getString("txtAmt.LocalText"));
                obj_id inventory = getInventoryContainer(self);
                if (static_item.isStaticItem(item))
                {
                    for (int i = 0; i < amount; i++)
                    {
                        static_item.createNewItemFunction(item, inventory);
                    }
                    //if they stack they will auto stack. no need for setCount()
                    sendSystemMessageTestingOnly(self, item + " granted with a stack of " + amount);
                }
                else
                {
                    for (int i = 0; i < amount; i++)
                    {
                        createObject(item, inventory, "");
                    }
                    sendSystemMessageTestingOnly(self, "Item given " + amount + " times");
                }
                break;
            default:
                break;
        }
        return SCRIPT_CONTINUE;
    }

    public void generateHeatmap(obj_id self, obj_id[] targets, String filename)
    {
        broadcast(self, "Attempting to capture " + targets.length + " objects.");
        String[] simpleArray = new String[targets.length];
        for (int i = 0; i < targets.length; i++)
        {
            simpleArray[i] = getLocation(targets[i]).x + "," + getLocation(targets[i]).z;
        }
        List<String> list = Arrays.asList(simpleArray);
        String[] unique = list.stream().distinct().toArray(String[]::new);
        String[] counts = new String[unique.length];
        saveTextOnClient(self, filename, Arrays.toString(simpleArray));
        broadcast(self, "Heatmap captured. Load into Nuna to overlay image.");
    }

    public void setupSnowspeeder(obj_id self)
    {
        obj_id target = getMountId(self);
        setBeastmasterPet(self, target);
        String[] abilities =
                {
                        "hoth_speeder_shoot",
                        "hoth_speeder_bolt",
                        "hoth_speeder_down",
                        "hoth_speeder_land",
                        "hoth_speeder_shoot",
                        "hoth_speeder_takeoff",
                        "hoth_speeder_up",
                        ""
                };
        setBeastmasterPetCommands(self, abilities);
    }

    public void sqlLiteQuery(String query)
    {
        try
        {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:swg.db");
            Statement stat = conn.createStatement();
            ResultSet rs = stat.executeQuery(query);
            while (rs.next())
            {
                System.out.println(rs.getString("name"));
            }
            rs.close();
            conn.close();
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public int calculate(String test) throws InterruptedException
    {
        return getDerivative(test);
    }

    public int getDerivative(String expression) throws InterruptedException
    {
        String[] parts = expression.split(" ");
        String operator = parts[1];
        int a = stringToInt(parts[0]);
        int b = stringToInt(parts[2]);
        if (operator.equals("+"))
        {
            return a + b;
        }
        else if (operator.equals("-"))
        {
            return a - b;
        }
        else if (operator.equals("*"))
        {
            return a * b;
        }
        else if (operator.equals("/"))
        {
            return a / b;
        }
        else
        {
            return 0;
        }
    }

    public int startWorkbench(obj_id self)
    {
        int page = createSUIPage("/Script.formObject", self, self);
        setSUIProperty(page, "txtScript", "MaxLength", "2000");
        setSUIProperty(page, "txtScript", "MaxLength", "2000");
        setSUIProperty(page, "createObject", "isDefaultButton", "true");
        setSUIProperty(page, "lblOutput", "LocalText", "\\#00FF00Ready...\\#.");
        setSUIProperty(page, "lblDescription", "LocalText", "Description");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "createObject", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtTemplate", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtName", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtScript", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtDescription", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkInteresting", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkSpaceInteresting", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkInvulnerable", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkTemporary", "onBundleForm");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkPersist", "onBundleForm");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtTemplate", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtName", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtScript", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtDescription", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkInteresting", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkSpaceInteresting", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkInvulnerable", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkHidden", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkTemporary", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkPersist", "Checked");
        setSUIAssociatedObject(page, self);
        boolean showResult = showSUIPage(page);
        if (!showResult)
        {
            broadcast(self, "Cannot display UI page '/Script.formObject");
        }
        flushSUIPage(page);
        setObjVar(self, "workbench.pid", page);
        return SCRIPT_CONTINUE;
    }

    public int onBundleForm(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = getIntObjVar(self, "workbench.pid");
        int button = getIntButtonPressed(params);
        obj_id player = getPlayerId(params);
        boolean isInteresting = Boolean.parseBoolean(params.getString("checkInteresting.Checked"));
        LOG("ethereal", "[Workbench] Interesting: " + isInteresting);
        boolean isSpaceInteresting = Boolean.parseBoolean(params.getString("checkSpaceInteresting.Checked"));
        LOG("ethereal", "[Workbench] Space Interesting: " + isSpaceInteresting);
        boolean isInvulnerable = Boolean.parseBoolean(params.getString("checkInvulnerable.Checked"));
        LOG("ethereal", "[Workbench] Invulnerable: " + isInvulnerable);
        boolean isHidden = Boolean.parseBoolean(params.getString("checkHidden.Checked"));
        LOG("ethereal", "[Workbench] Hidden: " + isHidden);
        boolean isTemporary = Boolean.parseBoolean(params.getString("checkTemporary.Checked"));
        LOG("ethereal", "[Workbench] Temporary: " + isTemporary);
        boolean isPersisted = Boolean.parseBoolean(params.getString("checkPersist.Checked"));
        LOG("ethereal", "[Workbench] Persisted: " + isPersisted);
        String template = params.getString("txtTemplate.LocalText");
        LOG("ethereal", "[Workbench] Template: " + template);
        String name = params.getString("txtName.LocalText");
        LOG("ethereal", "[Workbench] Name: " + name);
        String script = params.getString("txtScript.LocalText");
        LOG("ethereal", "[Workbench] Script: " + script);
        String description = params.getString("txtDescription.LocalText");
        LOG("ethereal", "[Workbench] Description: " + description);
        if (template.contains("shared_"))
        {
            template = template.replace("shared_", "");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Swapping to server template...\\#.");
        }
        obj_id created = createObject(template, getLocation(player));
        setName(created, name);
        if (isInteresting)
        {
            setCondition(created, CONDITION_INTERESTING);
            LOG("ethereal", "[Workbench] Condition: Interesting = true");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Interesting\\#.");
        }
        else if (isSpaceInteresting)
        {
            setCondition(created, CONDITION_SPACE_INTERESTING);
            LOG("ethereal", "[Workbench] Condition: Space Interesting = true");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Space Interesting\\#.");
        }
        else if (isInvulnerable)
        {
            setInvulnerable(created, true);
            LOG("ethereal", "[Workbench] Condition: Invulnerable = true");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Space Interesting\\#.");
        }
        else if (isHidden)
        {
            hideFromClient(created, true);
            LOG("ethereal", "[Workbench] Condition: Hidden = true");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Hidden\\#.");
        }
        else if (isTemporary && !isPersisted)
        {
            attachScript(created, "developer.bubbajoe.temp_item");
            LOG("ethereal", "[Workbench] Condition: Temporary = true");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Temporary\\#.");
        }
        else if (isPersisted && !isTemporary)
        {
            persistObject(created);
            LOG("ethereal", "[Workbench] Condition: Persisted = true");
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Persisted\\#.");
        }
        attachScript(created, script);
        setDescriptionString(created, description);
        setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00GENERATED\\#.");
        return SCRIPT_CONTINUE;
    }

    public String getPermissionLevel(obj_id who)
    {
        int adminLevel = getGodLevel(who);
        if (adminLevel == 0)
        {
            return "Player";
        }
        else if (adminLevel <= 15)
        {
            return "Contributor";
        }
        else if (adminLevel > 16 && adminLevel < 44)
        {
            return "Game Master";
        }
        else if (adminLevel > 45 && adminLevel < 49)
        {
            return "Administrator";
        }
        else if (adminLevel == 50)
        {
            return "HMFIC | Owner";
        }
        else return "unknown";
    }

    public int startAreaSpawner(obj_id self)
    {
        int page = createSUIPage("/Script.groundSpawner", self, self);
        setSUIProperty(page, "txtSpawnerName", "MaxLength", "2000");
        setSUIProperty(page, "txtSpawnerType", "MaxLength", "2000");
        setSUIProperty(page, "createObject", "isDefaultButton", "true");
        setSUIProperty(page, "lblOutput", "LocalText", "\\#00FF00Ready...\\#.");
        setSUIProperty(page, "v", "SetObject", self.toString());
        String INFO = "";
        INFO += "Planet: " + getCurrentSceneName() + "\n";
        INFO += "X/Y/Z: " + Math.round(getLocation(self).x) + ", " + Math.round(getLocation(self).y) + ", " + Math.round(getLocation(self).z) + "\n";
        INFO += "Buildout: " + getBuildoutAreaName(getLocation(self).x, getLocation(self).z) + "\n";
        INFO += "Access Level: " + getPermissionLevel(self) + "\n";
        INFO += "Players Nearby: " + getAllPlayers(getLocation(self), 100).length + "\n";
        setSUIProperty(page, "lblInfo", "LocalText", INFO);
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "createObject", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtSpawnerType", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtSpawnerName", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtLootCountOverride", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtLootTableOverride", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtSpawnerMin", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtSpawnerMax", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtSpawnerRadius", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "txtCount", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkLootCountOverride", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkLootTableOverride", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkController", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkInvulnerable", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "comboGroup", "onMakeSpawner");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "checkReset", "onMakeSpawner");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtTemplate", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtName", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtScript", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "txtDescription", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkLootCountOverride", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkLootTableOverride", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkController", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkInvulnerable", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "comboGroup", "SelectedIndex");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "checkReset", "Checked");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "createObject", "lblOutput", "Text");
        setSUIAssociatedObject(page, self);
        boolean showResult = showSUIPage(page);
        if (!showResult)
        {
            broadcast(self, "Cannot display UI page '/Script.groundSpawner");
        }
        flushSUIPage(page);
        setObjVar(self, "areaspawner.pid", page);
        return SCRIPT_CONTINUE;
    }

    public int onMakeSpawner(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = getIntObjVar(self, "areaspawner.pid");
        int button = getIntButtonPressed(params);
        obj_id player = getPlayerId(params);
        boolean isLootCountOverride = Boolean.parseBoolean(params.getString("checkLootCountOverride.Checked"));
        LOG("ethereal", "[AreaSpawner] Loot Count Override: " + isLootCountOverride);
        boolean isLootTableOverride = Boolean.parseBoolean(params.getString("checkLootTableOverride.Checked"));
        LOG("ethereal", "[AreaSpawner] Loot Table Override: " + isLootTableOverride);
        boolean isController = Boolean.parseBoolean(params.getString("checkController.Checked"));
        LOG("ethereal", "[AreaSpawner] Controller: " + isController);
        boolean isInvulnerable = Boolean.parseBoolean(params.getString("checkInvulnerable.Checked"));
        LOG("ethereal", "[AreaSpawner] Invulnerable: " + isInvulnerable);
        boolean isGetGoodLocation = Boolean.parseBoolean(params.getString("checkGoodLocation..Checked"));
        LOG("ethereal", "[AreaSpawner] Good Location: " + isGetGoodLocation);
        boolean isReset = Boolean.parseBoolean(params.getString("checkReset.Checked"));
        LOG("ethereal", "[AreaSpawner] Reset: " + isReset);
        String template = params.getString("txtSpawnerType.LocalText");
        LOG("ethereal", "[AreaSpawner] Template: " + template);
        String name = params.getString("txtSpawnerName.LocalText");
        LOG("ethereal", "[AreaSpawner] Name: " + name);
        int behavior = params.getInt("comboGroup.SelectedIndex");
        LOG("ethereal", "[AreaSpawner] Behavior: " + behavior);
        int lootCountOverride = stringToInt(params.getString("txtLootCountOverride.LocalText"));
        LOG("ethereal", "[AreaSpawner] Loot Count Override: " + lootCountOverride);
        String lootTableOverride = params.getString("txtLootTableOverride.LocalText");
        LOG("ethereal", "[AreaSpawner] Loot Table Override: " + lootTableOverride);
        int spawnerMin = stringToInt(params.getString("txtSpawnerMin.LocalText"));
        LOG("ethereal", "[AreaSpawner] Spawner Min: " + spawnerMin);
        int spawnerMax = stringToInt(params.getString("txtSpawnerMax.LocalText"));
        LOG("ethereal", "[AreaSpawner] Spawner Max: " + spawnerMax);
        int spawnerRadius = stringToInt(params.getString("txtSpawnerRadius.LocalText"));
        LOG("ethereal", "[AreaSpawner] Spawner Radius: " + spawnerRadius);
        int count = stringToInt(params.getString("txtCount.LocalText"));
        LOG("ethereal", "[AreaSpawner] Count: " + count);
        obj_id egg = createObject("object/tangible/ground_spawning/area_spawner.iff", getLocation(player));
        if (egg == null)
        {
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#FF0000Failed to create spawner.\\#.");
            return SCRIPT_CONTINUE;
        }
        setName(egg, name);
        setObjVar(egg, "strSpawnerType", "area");
        setObjVar(egg, "strName", name);
        setObjVar(egg, "intSpawnSystem", 1);
        setObjVar(egg, "strSpawns", template);
        setObjVar(egg, "fltMinSpawnTime", spawnerMin);
        setObjVar(egg, "fltMaxSpawnTime", spawnerMax);
        setObjVar(egg, "intSpawnCount", count);
        if (isLootCountOverride)
        {
            setObjVar(egg, "intLootCount", lootCountOverride);
        }
        if (isLootTableOverride)
        {
            setObjVar(egg, "strLootTable", lootTableOverride);
        }
        if (isController)
        {
            setObjVar(egg, "registerWithController", 1);
        }
        else
        {
            setObjVar(egg, "registerWithController", 0);
        }
        setObjVar(egg, "intDefaultBehavior", behavior);
        if (isGetGoodLocation)
        {
            setObjVar(egg, "getGoodLocation", 1);
        }
        else
        {
            setObjVar(egg, "getGoodLocation", 0);
        }
        if (isInvulnerable)
        {
            setObjVar(egg, "intVulnerability", 1);
        }
        setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Created!\\#.");
        setSUIProperty(pid, "v", "SetObject", egg.toString());
        attachScript(egg, "systems.spawning.spawn_area");
        setSUIProperty(pid, "lblOutput", "LocalText", params.getString("lblOutput.LocalText") + "\n Generated: " + egg + " " + "(" + name + ")");
        if (isReset)
        {
            setSUIProperty(pid, "lblOutput", "LocalText", "\\#00FF00Resetting...\\#.");
            setSUIProperty(pid, "v", "SetObject", self.toString());
            setSUIProperty(pid, "txtSpawnerName", "LocalText", "");
            setSUIProperty(pid, "txtSpawnerType", "LocalText", "");
            setSUIProperty(pid, "txtSpawnerRadius", "LocalText", "");
            setSUIProperty(pid, "txtCount", "LocalText", "");
            setSUIProperty(pid, "txtLootCountOverride", "LocalText", "");
            setSUIProperty(pid, "txtLootTableOverride", "LocalText", "");
        }
        return SCRIPT_CONTINUE;
    }

    public int startPlayerInfo(obj_id self)
    {
        LOG("ethereal", "[SUI]: Entered startPlayerInfo");
        int page = createSUIPage("/Script.playerInfo", self, self);
        LOG("ethereal", "[SUI]: Created SUI Page: " + page);
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "search", "txtSearch", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "search", "playerInfo", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "search", "playerBio", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "search", "v", "SetObject");
        LOG("ethereal", "[SUI]: Subscribed to SUI Properties for Events");
        subscribeToSUIProperty(page, "txtSearch", "LocalText");
        subscribeToSUIProperty(page, "playerInfo", "LocalText");
        subscribeToSUIProperty(page, "playerBio", "LocalText");
        subscribeToSUIProperty(page, "v", "SetObject");
        LOG("ethereal", "[SUI]: Subscribed to SUI Properties");
        setSUIProperty(page, "back.text", "LocalText", "Developer Datapad - [" + getPlayerFullName(self) + "]");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "search", "onUpdatePlayerInfo");
        subscribeToSUIEvent(page, sui_event_type.SET_onClosedCancel, "exit", "onCloseSUI");
        LOG("ethereal", "[SUI]: Subscribed to SUI Events");
        setSUIAssociatedObject(page, self);
        LOG("ethereal", "[SUI]: Set SUI Associated Object");
        boolean showResult = showSUIPage(page);
        if (!showResult)
        {
            broadcast(self, "Cannot display UI page '/Script.playerInfo");
        }
        LOG("ethereal", "[SUI]: Shown SUI Page");
        flushSUIPage(page);
        setObjVar(self, "playerinfo.pid", page);
        LOG("ethereal", "[SUI]: Tracked PID: " + page);
        return SCRIPT_CONTINUE;
    }

    public int onCloseSUI(obj_id self, dictionary params) throws InterruptedException
    {
        int pid = getIntObjVar(self, "playerinfo.pid");
        LOG("ethereal", "[SUI]: Closing SUI: " + pid);
        closeSUI(self, pid);
        return SCRIPT_CONTINUE;
    }

    public int onUpdatePlayerInfo(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[SUI]: Entered onUpdatePlayerInfo");
        int pid = getIntObjVar(self, "playerinfo.pid");
        LOG("ethereal", "[SUI]: Updating Player Info: " + pid);
        obj_id player = getPlayerId(params);
        String search = params.getString("txtSearch.LocalText");
        obj_id target = getPlayerIdFromFirstName(search);
        LOG("ethereal", "[SUI]: Player Search: " + search);
        if (!isIdValid(target))
        {
            broadcast(self, "Player not found.");
            LOG("ethereal", "[SUI]: Player not found.");
            return SCRIPT_CONTINUE;
        }
        String prompt = getPrompt(target);
        LOG("ethereal", "[SUI]: Setting Prompt");
        setSUIProperty(pid, "playerInfo", "LocalText", prompt);
        LOG("ethereal", "[SUI]: Setting Player Info");
        setSUIProperty(pid, "back.title", "LocalText", "Developer Datapad - " + getPlayerFullName(target));
        LOG("ethereal", "[SUI]: Setting Title");
        setSUIProperty(pid, "playerBio", "LocalText", getPlayerDetails(target));
        LOG("ethereal", "[SUI]: Setting Player Bio");
        setSUIProperty(pid, "v", "SetObject", target.toString());
        LOG("ethereal", "[SUI]: Param dump:\n " + params);
        return SCRIPT_CONTINUE;
    }

    public String getPlayerDetails(obj_id who)
    {
        String details = getPlayerFullName(who);
        if (getGuildId(who) != 0)
        {
            details += "\n<" + guildGetAbbrev(getGuildId(who)) + ">\n\n";
        }
        if (hasObjVar(who, "warn.level"))
        {
            details += "\nLatest Warning Level: " + getIntObjVar(who, "warn.level") + "\n";
        }
        if (hasObjVar(who, "warn.reason"))
        {
            details += "\nLatest Warn Reason: " + getIntObjVar(who, "warn.reason") + "\n";
        }
        if (hasObjVar(who, "warn.time"))
        {
            details += "\nLatest Warn Time: " + getIntObjVar(who, "warn.time") + "\n";
        }
        if (isGod(who))
        {
            details += "\nGod Mode: " + isGod(who) + " |  Access Level: " + getGodLevel(who) + "\n";
        }
        details += "\n";
        return details;
    }

    public String getPrompt(obj_id player) throws InterruptedException
    {
        StringBuilder prompt = new StringBuilder(("  ------------------   Account  ------------------ ") + "\n");
        prompt.append("Station Name: ").append(getPlayerAccountUsername(player)).append("\n");
        prompt.append("Station ID: ").append(getPlayerStationId(player)).append("\n");
        prompt.append("Full Name: ").append(getPlayerFullName(player)).append("\n");
        prompt.append("NetworkId: ").append(player).append("\n");
        if (isInWorldCell(player))
        {
            prompt.append("Location: ").append(getLocation(player).toReadableFormat(true)).append("\n");
        }
        else
        {
            prompt.append("Location: ").append(getLocation(player).toReadableFormat(true)).append(" (Cell)").append("\n");
        }
        prompt.append("Creation Date: ").append(getPlayerBirthDate(player)).append("\n");
        prompt.append("Housing Lots: ").append(getMaxHousingLots()).append("\n\n");
        prompt.append(" ------------------ " + "Character" + " ------------------ " + "\n");
        prompt.append("Posture: ").append(getPosture(player)).append("\n");
        prompt.append("Locomotion: ").append(getLocomotion(player)).append("\n");
        prompt.append("Scale: ").append(getScale(player)).append("\n");
        prompt.append("Race: ").append(getRace(player)).append("\n");
        prompt.append("Mood: ").append(getAnimationMood(player)).append("\n");
        prompt.append("Player Template: ").append(getTemplateName(player)).append("\n");
        prompt.append("Player Template CRC: ").append(utils.getStringCrc(getTemplateName(player))).append("\n\n");
        prompt.append(" ------------------ " + "Player" + " ------------------ " + "\n");
        prompt.append("Health: ").append(getAttrib(player, HEALTH)).append("\n");
        prompt.append("Action: ").append(getAttrib(player, ACTION)).append("\n");
        prompt.append("Money (total): ").append(getTotalMoney(player)).append("\n");
        prompt.append("Money (bank): ").append(getBankBalance(player)).append("\n");
        prompt.append("Money (cash): ").append(getCashBalance(player)).append("\n");
        prompt.append("Inventory Storage: ").append(getVolumeFree(getInventoryContainer(player))).append("/").append(getTotalVolume(getInventoryContainer(player))).append(" slots free\n");
        prompt.append("Datapad Storage: ").append(getVolumeFree(getDatapad(player))).append("/").append(getTotalVolume(getDatapad(player))).append(" slots free\n\n");
        prompt.append(" \n" + "------------------ " + "Terminal Missions" + " ------------------ " + "\n");
        obj_id[] quests = getMissionObjects(player); //missions from datapad
        if (quests == null)
        {
            prompt.append(color("C70039", "No missions found.")).append("\n");
        }
        else
        {
            for (obj_id quest : quests)
            {
                prompt.append("\tMission Name: ").append(localize(getMissionTitle(quest))).append("\n\n");
                prompt.append("\t\tMission Desc: ").append(localize(getMissionDescription(quest))).append("\n\n");
                prompt.append("\t\tMission ID: ").append(quest).append("\n");
                prompt.append("\t\tMission Credit Reward: ").append(getMissionReward(quest)).append("\n");
                prompt.append("\t\tMission Status: ").append(getMissionStatus(quest)).append("\n");
                prompt.append("\t\tMission Owner: ").append(getMissionHolder(quest)).append("\n");
                prompt.append("\t\tMission Creator: ").append(getMissionCreator(quest)).append("\n");
                prompt.append("\t\tMisison Difficulty: ").append(getMissionDifficulty(quest)).append("\n");
                prompt.append("\t\tMission Start Location: ").append(getMissionStartLocation(quest).toReadableFormat(true)).append("\n");
                prompt.append("\t\tMission End Location: ").append(getMissionEndLocation(quest).toReadableFormat(true)).append("\n\n");
            }
        }
        prompt.append(" ------------------ " + "Rare Loot System" + " ------------------ " + "\n");
        prompt.append("\tNote: 0 or null means they have not looted one." + "\n");
        prompt.append("\t\tLast Looted Chest Timestamp: ").append(getIntObjVar(player, "loot.rls.lastChestAwardTime")).append("\n");
        prompt.append("\t\tLast Looted Chest ID: ").append(getObjIdObjVar(player, "loot.rls.lastLootedChest")).append("\n");
        prompt.append("\t\tLast Looted Chest Location: ").append(getLocationObjVar(player, "loot.rls.lastLootedLocation")).append("\n\n");
        prompt.append(" ------------------ " + "Dungeons" + " ------------------ " + "\n");
        prompt.append("\tDeath Watch Bunker: ").append(hasObjVar(player, "mand.acknowledge") ? color("AAFF00", "Access Granted") : color("C70039", "Access Denied")).append("\n");
        obj_id[] items = utils.getContents(player, true);
        boolean hasAccessKey = false;
        for (obj_id accessKey : items)
        {
            if (getTemplateName(accessKey).equals("object/tangible/loot/dungeon/geonosian_mad_bunker/passkey.iff") && (isIdValid(accessKey)))
            {
                hasAccessKey = true;
            }
        }
        if (hasAccessKey)
        {
            prompt.append("\tGeonosian Bio-lab: ").append(color("AAFF00", "Access Granted")).append("\n");
        }
        else
        {
            prompt.append("\tGeonosian Bio-lab: ").append(color("C70039", "Access Denied")).append("\n");
        }
        prompt.append(" ------------------ " + "Heroics" + " ------------------ " + "\n"); // only heroics from Aurilia, not mustafar key checks.
        if (instance.isFlaggedForInstance(player, "echo_base"))
        {
            prompt.append("\tBattle of Echo Base - ").append(color("AAFF00", "Flagged")).append("\n");
        }
        else
        {
            prompt.append("\tBattle of Echo Base - ").append(color("C70039", "Not Flagged")).append("\n");
        }
        if (instance.isFlaggedForInstance(player, "heroic_exar_kun"))
        {
            prompt.append("\tHeroic: Exar Kun - ").append(color("AAFF00", "Flagged")).append("\n");
        }
        else
        {
            prompt.append("\tHeroic: Exar Kun - ").append(color("C70039", "Not Flagged")).append("\n");
        }
        if (instance.isFlaggedForInstance(player, "heroic_star_destroyer"))
        {
            prompt.append("\tHeroic: Lost Star Destroyer - ").append(color("AAFF00", "Flagged")).append("\n");
        }
        else
        {
            prompt.append("\tHeroic: Lost Star Destroyer - ").append(color("C70039", "Not Flagged")).append("\n");
        }
        if (instance.isFlaggedForInstance(player, "heroic_tusken_army"))
        {
            prompt.append("\tHeroic: Tusken Army - ").append(color("AAFF00", "Flagged")).append("\n");
        }
        else
        {
            prompt.append("\tHeroic: Tusken Army - ").append(color("C70039", "Not Flagged")).append("\n");
        }
        if (instance.isFlaggedForInstance(player, "heroic_axkva_min"))
        {
            prompt.append("\tHeroic: Axkva Min - ").append(color("AAFF00", "Flagged")).append("\n");
        }
        else
        {
            prompt.append("\tHeroic: Axkva Min - ").append(color("C70039", "Not Flagged")).append("\n");
        }
        if (instance.isFlaggedForInstance(player, "heroic_ig88"))
        {
            prompt.append("\tHeroic: Droid Factory - ").append(color("AAFF00", "Flagged")).append("\n");
        }
        else
        {
            prompt.append("\tHeroic: Droid Factory - ").append(color("C70039", "Not Flagged")).append("\n");
        }
        prompt.append(" ------------------ " + "Faction" + " ------------------ " + "\n");
        if (factions.isRebel(player))
        {
            prompt.append("Aligned: Rebel \n");
            prompt.append("Rank:").append(pvpGetCurrentGcwRank(player)).append("\n\n");
            prompt.append("\t(Current GCW Cycle Information) \n");
            prompt.append(" \t\tGCW Points: ").append(pvpGetCurrentGcwPoints(player)).append("\n");
            prompt.append(" \t\tCW Kills: ").append(pvpGetCurrentPvpKills(player)).append("\n");
            prompt.append(" \t\tGCW Rating: ").append(pvpGetCurrentGcwRating(player)).append("\n");
        }
        else if (factions.isImperial(player))
        {
            prompt.append("Aligned: Imperial \n");
            prompt.append("Rank: ").append(pvpGetCurrentGcwRank(player)).append("\n\n");
            prompt.append("\t(Current GCW Cycle Information) \n");
            prompt.append(" \t\tGCW Points: ").append(pvpGetCurrentGcwPoints(player)).append("\n");
            prompt.append(" \t\tCW Kills: ").append(pvpGetCurrentPvpKills(player)).append("\n");
            prompt.append(" \t\tGCW Rating: ").append(pvpGetCurrentGcwRating(player)).append("\n");
        }
        else
        {
            prompt.append("Neutral, unaligned.\n");
        }
        prompt.append(" ------------------ " + "Group" + " ------------------ " + "\n");
        if (!group.isGrouped(player))
        {
            prompt.append("Player is ungrouped. \n");
        }
        else
        {
            prompt.append("Group ID: ").append(group.getGroupObject(player)).append("\n");
            prompt.append("Group Leader: ").append(group.getLeader(group.getGroupObject(player))).append("\n");
            prompt.append("Group Members: \n");
            obj_id[] members = group.getGroupMemberIds(group.getGroupObject(player));
            for (obj_id member : members)
            {
                if (member != player)
                {
                    if (isIdValid(member))
                    {
                        prompt.append("\t").append(getPlayerFullName(member)).append("\n");
                    }
                }
            }
        }
        prompt.append(" ------------------ " + "Guild" + " ------------------ " + "\n");
        if (getGuildId(player) == 0)
        {
            prompt.append("Player is not associated with a guild. \n");
        }
        else
        {
            prompt.append("Name: ").append(guildGetName(getGuildId(player))).append("\n");
            prompt.append("Abbrev.: ").append(guildGetAbbrev(getGuildId(player))).append("\n");
            prompt.append("ID: ").append(getGuildId(player)).append("\n");
            prompt.append("Control Device: ").append(guild.getGuildRemoteDevice(player)).append("\n");
            prompt.append("Leader: ").append(guildGetLeader(getGuildId(player))).append("\n");
            prompt.append("Members:\n");
            obj_id[] members = guildGetMemberIds(getGuildId(player));
            for (obj_id member : members)
            {
                if (member != player)
                {
                    if (isIdValid(member))
                    {
                        prompt.append(getPlayerFullName(member)).append("\n");
                    }
                }
            }
        }
        prompt.append(" ------------------ " + "Skills" + " ------------------  " + "\n");
        String[] skillList = getSkillListingForPlayer(player);
        for (String value : skillList)
        {
            if (value.equals("swg_dev") || (value.equals("swg_cs")) || (value.startsWith("expertise"))) //dont show swg_dev or swg_cs, they show as [0]
            {
                continue;
            }
            prompt.append(value).append("\n");
        }
        prompt.append(" ------------------ " + "Expertise" + " ------------------  " + "\n");
        String[] expertiseList = getSkillListingForPlayer(player);
        for (int i = 0; i < skillList.length; i++)
        {
            if (expertiseList[i].startsWith("expertise_"))
            {
                prompt.append(expertiseList[i]).append("\n");
            }
        }
        prompt.append(" ------------------ " + "Event System" + " ------------------ " + "\n");
        int pumpkinPulped = getIntObjVar(player, "halloween.pulped");
        int scrapCollected = getIntObjVar(player, "gjpud.total");
        boolean hasWBB = hasObjVar(getPlanetByName("tatooine"), "wbb.grantedTo." + getPlayerAccountUsername(player));
        prompt.append("Pumpkin Pulped: ").append(pumpkinPulped).append("\n");
        prompt.append("Pumpkin Pulper Award: ").append(!hasObjVar(player, "halloween.22_award") ? "no" : "yes").append("\n");
        prompt.append("Scrap Collected: ").append(scrapCollected).append("\n");
        prompt.append("Scrap Heap: ").append(!hasObjVar(player, "gjpud.scrapheaps") ? "no" : "yes").append("\n");
        prompt.append("Received Welcome Back Boxes: ").append(hasWBB ? "yes" : "no").append("\n");
        prompt.append("RAT granted: ").append(hasObjVar(player, "wd") ? "yes" : "no").append("\n");
        prompt.append(" ------------------ " + "Scripts" + " ------------------ " + "\n");
        String[] list = getScriptList(player);
        for (String s : list)
        {
            String removed = s.replace("script.", "");
            prompt.append(removed).append("\n");
        }
        prompt.append("\n");
        prompt.append(" ------------------ " + "Buffs" + " ------------------ " + "\n");
        int[] buffs = buff.getAllBuffs(player);
        assert buffs != null;
        if (buffs.length > 0)
        {
            prompt.append("Buff Names: \n");
            for (int buffIndex : buffs)
            {
                prompt.append(buff.getBuffNameFromCrc(buffIndex)).append(" | ").append(buff.isDebuff(buffIndex) ? "(Debuff)" : "(Buff)").append("\n");
            }
        }
        else
        {
            prompt.append("No buffs active. \n");
        }
        prompt.append(" ------------------ " + "Combat" + " ------------------ " + "\n");
        prompt.append("Player is ").append(!combat.isInCombat(player) ? "disengaged" : "engaged with " + getName(getTarget(player))).append("\n");
        prompt.append("Player is in stealth: ").append(getCreatureCoverVisibility(player) ? "false" : "true").append("\n");
        prompt.append("\n");
        prompt.append(" ------------------ " + "Notes" + " ------------------ " + "\n");
        if (!hasObjVar(player, "skynet.notes"))
        {
            prompt.append(color("AAFF00", "No notes exist for this player :)")).append("\n");
        }
        else
        {
            String note = getStringObjVar(player, "skynet.notes");
            if (note.equals("cheater"))
            {
                prompt.append(color("C70039", "This player has been flagged as a cheater.")).append("\n");
            }
            else
            {
                prompt.append(color("FFC300", getStringObjVar(player, "skynet.notes"))).append("\n");
            }
        }
        prompt.append(" ------------------ " + "Object Variables" + " ------------------ " + "\n");
        obj_var_list ovl = getObjVarList(player, "");
        if (ovl == null)
        {
            prompt.append("No object variables exist for this player. \n");
        }
        if (ovl.toString().length() > 3500)
        {
            prompt.append("Object variable list too diverse for ").append(getPlayerFullName(player)).append("  Please use the /objvar list command.").append("\n");
        }
        else
        {
            for (int i = 0; i < ovl.getNumItems(); i++)
            {
                obj_var ov = ovl.getObjVar(i);
                prompt.append(ov.getName()).append(" = ").append(ov).append("\n");
            }
        }
        prompt.append(" ------------------ " + "Inventory" + " ------------------ " + "\n");
        obj_id[] contents = utils.getContents(player, true);
        for (obj_id content : contents)
        {
            if (hasObjVar(content, "noTrade"))
            {
                prompt.append("[NwID  ").append(content).append("] ").append(" ").append(color("FFD500", base_class.getNameNoSpam(content))).append("\n\t Template [").append(getTemplateName(content)).append("] ").append(color("DD1234", "[NO TRADE]")).append(colors_hex.FOOTER).append("\n\n");
            }
            else if (hasScript(content, "item.special.nomove"))
            {
                prompt.append("[NwID  ").append(content).append("] ").append(" ").append(color("FFD500", base_class.getNameNoSpam(content))).append("\n\t Template: [").append(getTemplateName(content)).append("] ").append(color("DD1234", "[NO MOVE | UNIQUE | NO TRADE]")).append(colors_hex.FOOTER).append("\n\n");
            }
            else if (getTemplateName(content).contains("character_builder"))
            {
                prompt.append("[NwID  ").append(content).append("] ").append(" ").append(color("FFD500", base_class.getNameNoSpam(content))).append("\n\t Template: [").append(getTemplateName(content)).append("] ").append(color("DD1234", "[INSTANT DELETE LIST]")).append(colors_hex.FOOTER).append("\n\n");
            }
            else if (hasObjVar(content, "item.temporary.time_stamp"))
            {
                prompt.append("[NwID  ").append(content).append("]").append(" ").append(color("FFD500", base_class.getNameNoSpam(content))).append("\n\t Template: [").append(getTemplateName(content)).append("] ").append(color("DD1234", "[TEMP ITEM]")).append(colors_hex.FOOTER).append("\n\n");
            }
            else
            {
                prompt.append("[NwID  ").append(content).append("] ").append(" ").append(color("FFD500", base_class.getNameNoSpam(content))).append("\n\t Template: [").append(getTemplateName(content)).append("] ").append("\n\n");
            }
        }
        prompt.append(" ------------------ [").append(getPlayerFullName(player)).append("]------------------ ").append("\n");
        return prompt.toString();
    }

    public String[] getAllMobiles()
    {
        String directoryPath = "/home/swg/swg-main/data/sku.0/sys.server/compiled/game/object/mobile/";
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        List<String> iffFilePaths = new ArrayList<>();
        if (files != null)
        {
            Arrays.sort(files); //@Note: to lock the indexes of the files, we sort them. This means that the indexes will always be the same, which is needed for sui.getListboxSelectedRow
            for (File file : files)
            {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".iff"))
                {
                    String filePath = file.getPath().replaceFirst("^/home/swg/swg-main/data/sku.0/sys.server/compiled/game/", "");
                    iffFilePaths.add(filePath);
                }
            }
        }
        return iffFilePaths.toArray(new String[0]);
    }

    public String[] getAllTemplates(obj_id self, String folder, String key, boolean recursive)
    {
        LOG("ethereal", "[Template Lookup]: Calling getAllTemplates(self, " + folder + ", " + key + ", " + recursive + ")");
        String directoryPath = "/home/swg/swg-main/data/sku.0/sys.server/compiled/game/object/";
        File directory = new File(directoryPath + folder);
        File[] files = directory.listFiles();
        List<String> iffFilePaths = new ArrayList<>();
        if (files != null)
        {
            LOG("ethereal", "[Template Lookup]: Found " + files.length + " files in " + directoryPath + folder);
            LOG("ethereal", "[Template Lookup]: Sorting indexing of files");
            Arrays.sort(files); //@Note: to lock the indexes of the files, we sort them. This means that the indexes will always be the same, which is needed for sui.getListboxSelectedRow
            for (File file : files)
            {
                LOG("ethereal", "[Template Lookup]: Found matching file, Adding  " + file.getName() + " to list");
                if (file.isFile() && file.getName().toLowerCase().endsWith(".iff") && file.getName().toLowerCase().contains(key.toLowerCase()))
                {
                    String filePath = file.getPath().replaceFirst("^/home/swg/swg-main/data/sku.0/sys.server/compiled/game/", "");
                    iffFilePaths.add(filePath);
                }
            }
        }
        else
        {
            LOG("ethereal", "[Template Lookup]: No files found in " + directoryPath + folder + " for key " + key);
        }
        return iffFilePaths.toArray(new String[0]);
    }

    public int handleTemplateSearch(obj_id self, dictionary params) throws InterruptedException
    {
        int button = getIntButtonPressed(params);
        obj_id player = getPlayerId(params);
        int selectionIndex = getListboxSelectedRow(params);
        if (button == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        if (button == BP_OK)
        {
            String[] selections = getAllTemplates(self, getStringScriptVar(self, "templateSearchPath"), getStringScriptVar(self, "templateSearchTerm"), true);
            obj_id template = createObject(selections[selectionIndex], getLocation(player));
            detachAllScripts(template);
            broadcast(self, "Template created: " + template);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleBarkerTemplate(obj_id self, dictionary params) throws InterruptedException
    {
        int button = getIntButtonPressed(params);
        obj_id player = getPlayerId(params);
        int selectionIndex = getListboxSelectedRow(params);
        if (button == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        if (button == BP_OK)
        {
            String[] selections = getAllMobiles();
            obj_id barker = createObject(selections[selectionIndex], getLocation(player));
            detachAllScripts(barker);
            attachScript(barker, "content.barker_nv");
            broadcast(self, "Barker created: " + barker);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleTemplateLookup(obj_id self, dictionary params) throws InterruptedException
    {
        int button = getIntButtonPressed(params);
        obj_id player = getPlayerId(params);
        int selectionIndex = getListboxSelectedRow(params);
        if (button == BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        if (button == BP_OK)
        {
            String keyterm = getStringScriptVar(self, "templateSearchTerm");
            String[] selections = listObjectFilesByTerm(keyterm);
            obj_id template = createObject(selections[selectionIndex], getLocation(player));
            detachAllScripts(template);
            broadcast(self, "Template created: " + template);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public void shotgunResources(obj_id self) throws InterruptedException
    {
        String BAG_NAME = "Bag-O-Resources";
        obj_id bagOResources = createObjectInInventoryAllowOverload("object/tangible/test/qabag.iff", self);
        setName(bagOResources, BAG_NAME);
        int quantity = 150000;
        String RESOURCE_TABLE = "datatables/resource/resource_tree.iff";
        String[] RESOURCE_NAMES = dataTableGetStringColumn(RESOURCE_TABLE, "ENUM");
        for (String type : RESOURCE_NAMES)
        {
            if (type.contains("_") || type.equalsIgnoreCase("resource"))
            {
                broadcast(self, "Skipping " + type + "...");
            }
            else
            {
                obj_id[] resourceTypes = craftinglib.getAllResourceChildren(type);
                if (resourceTypes == null || resourceTypes.length == 0)
                {
                    return;
                }
                obj_id best = craftinglib.findBestResourceByAverage(resourceTypes);
                obj_id resourceCrate = createResourceCrate(best, quantity, bagOResources);
            }
        }
    }

    public void showImportExportOptions(obj_id self) throws InterruptedException
    {
        String[] options = {"Import Housing Layout", "Export Housing Layout"};
        listbox(self, self, "Select an option:", OK_CANCEL, "Housing Management", options, "handleImportExportOption");
    }

    public void showMasterItems(obj_id self) throws InterruptedException
    {
        if (isInWorldCell(self))
        {
            broadcast(self, "You must be in a cell to use this command.");
            return;
        }

        String title = "";
        String prompt = "Master Item Data";

        // First, ask for the search term
        inputbox(self, self, "Enter Search Term", OK_CANCEL, "Master Item - Search", INPUT_NORMAL, null, "handleSearchInput", null);
    }

    public int handleSearchInput(obj_id self, dictionary params) throws InterruptedException
    {
        // Get the search term entered by the user
        String searchTerm = params.getString("input");

        // Update the search filter
        searchFilter = searchTerm != null ? searchTerm : "";

        // Prepare the data for the current page based on the filtered results
        paginatedData = buildMasterItemTable(self, currentPage);

        if (paginatedData.length == 0)
        {
            broadcast(self, "No matching master items found.");
            return SCRIPT_CONTINUE;
        }

        String[] columns = {
                "Item Code",
                "Name",
                "Desc."
        };

        String[] columnTypes = {
                "string",
                "string",
                "string"
        };

        // Calculate total pages based on the filtered data
        totalPages = (int) Math.ceil((double) buildMasterItemTable(self, 0).length / pageSize);

        // Display the table with pagination options
        int pid = tableRowMajor(self, self, OK_CANCEL, "", "handleTableOk", null, columns, columnTypes, paginatedData);

        // Optionally show "Next" or "Previous" buttons
        listboxUseOtherButton(pid, "Next");
        listboxUseOtherButton(pid, "Previous");

        return SCRIPT_CONTINUE;
    }


    // Pagination logic for building table with search filter applied
    public String[][] buildMasterItemTable(obj_id self, int pageIndex) throws InterruptedException
    {
        String[] nameColumn = dataTableGetStringColumn(MASTER_ITEM_TABLE, "name");
        String[] stringNameColumn = dataTableGetStringColumn(MASTER_ITEM_TABLE, "string_name");
        String[] stringDetailsColumn = dataTableGetStringColumn(MASTER_ITEM_TABLE, "string_detail");

        if (nameColumn == null || stringNameColumn == null || stringDetailsColumn == null)
        {
            return new String[0][0]; // Return empty table if no data is available
        }

        List<String[]> validItems = new ArrayList<>();

        // Loop through the table and filter based on the search query
        for (int i = 0; i < nameColumn.length; i++)
        {
            // Check if the name contains the search term
            if (nameColumn[i] != null && !nameColumn[i].isEmpty() && nameColumn[i].contains(searchFilter))
            {
                String[] row = new String[3];
                row[0] = nameColumn[i];
                row[1] = stringNameColumn[i];
                row[2] = stringDetailsColumn[i];
                validItems.add(row);
            }
        }

        // Paginate the filtered results based on the pageIndex and pageSize
        int startIndex = pageIndex * pageSize;
        int endIndex = Math.min(startIndex + pageSize, validItems.size());

        // Extract the relevant page data
        String[][] paginatedResults = new String[endIndex - startIndex][3];
        for (int i = startIndex; i < endIndex; i++)
        {
            paginatedResults[i - startIndex] = validItems.get(i);
        }

        return paginatedResults;
    }

    // Handle Next and Previous Page logic
    public int handleTableOk(obj_id self, dictionary params) throws InterruptedException
    {
        int buttonPressed = getIntButtonPressed(params);

        if (buttonPressed == BP_CANCEL)
        {
            if (currentPage < totalPages - 1)
            {
                currentPage++;
                showMasterItems(self);  // Refresh the table for the next page
            }
        }
        else if (buttonPressed == BP_OK)
        {
            // Move to the previous page, if not at the first page
            if (currentPage > 0)
            {
                currentPage--;
                showMasterItems(self);  // Refresh the table for the previous page
            }
        }

        return SCRIPT_CONTINUE;
    }

    public String[][] buildMasterItemTable(obj_id self) throws InterruptedException
    {
        // Retrieve the columns from the MASTER_ITEM_TABLE
        String[] nameColumn = dataTableGetStringColumn(MASTER_ITEM_TABLE, "name");
        String[] stringNameColumn = dataTableGetStringColumn(MASTER_ITEM_TABLE, "string_name");
        String[] stringDetailsColumn = dataTableGetStringColumn(MASTER_ITEM_TABLE, "string_detail");

        // Check if any of the columns are empty or null and handle the case where there's no data
        if (nameColumn == null || stringNameColumn == null || stringDetailsColumn == null)
        {
            return new String[0][0]; // Return an empty table if the columns are not properly fetched
        }

        // Retrieve the stored search term
        String savedSearchTerm = getStringObjVar(self, "masterItemSearchTerm");
        if (savedSearchTerm == null)
        {
            savedSearchTerm = ""; // Default to empty if no saved search term exists
        }

        // List to hold rows of data (Row-major format)
        List<String[]> filteredData = new ArrayList<>();

        // Loop through the data columns and create a row for each entry
        for (int i = 0; i < nameColumn.length; i++)
        {
            // Ensure that none of the data entries are null or empty
            if (nameColumn[i] != null && !nameColumn[i].isEmpty() &&
                    stringNameColumn[i] != null && !stringNameColumn[i].isEmpty() &&
                    stringDetailsColumn[i] != null && !stringDetailsColumn[i].isEmpty())
            {

                // Filter rows where the name column contains the search term
                if (nameColumn[i].toLowerCase().contains(savedSearchTerm.toLowerCase()))
                {
                    String[] row = new String[2]; // Only showing string_name and string_detail
                    row[0] = stringNameColumn[i];
                    row[1] = stringDetailsColumn[i];
                    filteredData.add(row);
                }
            }
        }

        // Return the filtered table data as row-major format
        return filteredData.toArray(new String[0][0]);
    }

    public void showAllHousing(obj_id self) throws InterruptedException
    {
        String[] columns = {
                "Name",
                "Type",
                "Count",
                "No Trade",
                "Network ID",
                "Static Item"
        };

        String[] columnTypes = {
                "text",
                "text",
                "text",
                "text",
                "text",
                "text"
        };

        if (isInWorldCell(self))
        {
            broadcast(self, "You must be in a cell to use this command.");
            return;
        }

        String title = "Housing Objects";
        String prompt = "Housing Objects";

        String[][] data = buildTableFromHouse(getTopMostContainer(self));

        if (data.length == 0 || data[0].length != columns.length)
        {
            broadcast(self, "Data array does not match the column definitions.");
            return;
        }

        // Display the table
        int pid = tableRowMajor(self, self, OK_CANCEL, title, "handleTableOk", prompt, columns, columnTypes, data);
        setSUIProperty(pid, "comp.tablePage.table", "Selectable", "true");
        setSUIProperty(pid, "comp.tablePage.table", "SelectionAllowedMultiRow", "true");
        setSUIProperty(pid, "comp.tablePage.table", "CellHeight", "30");
        setSUIProperty(pid, "comp.tablePage.table", "CellPadding", "4");
        setSUIProperty(pid, "comp.tablePage.table", "DefaultTextStyle", "bold_22");
        setSUIProperty(pid, "comp.tablePage.table", "DefaultTextColor", "#FFFFFF");
        if (pid == -1)
        {
            broadcast(self, "Failed to create the table.");
        }
        else
        {
            flushSUIPage(pid);
            showSUIPage(pid);
        }
    }

    public String[][] buildTableFromHouse(obj_id topMostContainer) throws InterruptedException
    {
        obj_id[] cells = getCellIds(topMostContainer);

        if (cells == null || cells.length == 0)
        {
            return new String[0][0];
        }

        List<String[]> validItems = new ArrayList<>();

        for (obj_id cell : cells)
        {
            obj_id[] contents = utils.getContents(cell, true);

            if (contents == null)
            {
                continue;
            }

            for (obj_id item : contents)
            {
                String template = getTemplateName(item);
                if (!(template.startsWith("object/creature/") ||
                        template.startsWith("object/mobile/") ||
                        template.startsWith("object/cell/")))
                {
                    String[] row = new String[6];
                    row[0] = getEncodedName(item);
                    row[1] = template;
                    row[2] = String.valueOf(getCount(item));
                    row[3] = hasObjVar(item, "noTrade") ? "Yes" : "No";
                    row[4] = String.valueOf(item);
                    row[5] = String.valueOf(static_item.isStaticItem(item));
                    validItems.add(row);
                }
            }
        }

        if (validItems.isEmpty())
        {
            return new String[0][0];
        }

        return validItems.toArray(new String[0][0]);
    }

    /**
     * Converts row-major data into column-major format
     */
    private String[][] transpose(List<String[]> data)
    {
        if (data.isEmpty()) return new String[0][0];

        int rowCount = data.size();
        int colCount = data.get(0).length;
        String[][] transposed = new String[colCount][rowCount];

        for (int i = 0; i < rowCount; i++)
        {
            for (int j = 0; j < colCount; j++)
            {
                transposed[j][i] = data.get(i)[j];
            }
        }

        return transposed;
    }

    public int handleImportExportOption(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = getPlayerId(params);
        int idx = getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        switch (idx)
        {
            case 0:
                getSavedHousingLayouts(self, player);
                break;
            case 1:
                exportHousingContents(player, getTopMostContainer(self), getPlayerAccountUsername(player) + "_" + player + "_" + getChoppedName(getTopMostContainer(player)));
                break;
        }

        return SCRIPT_CONTINUE;
    }

    private String getChoppedName(obj_id topMostContainer)
    {
        String fullName = getTemplateName(topMostContainer);
        int lastSlashIndex = fullName.lastIndexOf('/');

        int dotIndex = fullName.lastIndexOf('.');

        if (lastSlashIndex == -1 || dotIndex == -1)
        {
            return null;
        }

        return fullName.substring(lastSlashIndex + 1, dotIndex);
    }

    /**
     * Lists all files in a given directory regardless of owner or type.
     */
    private String[] listAllFiles(String directoryPath)
    {
        // Assume this function will interact with the file system to get all files
        // Here is a pseudo-implementation. Replace it with actual file reading logic.
        File dir = new File(directoryPath);

        // Get all files with .house extension in the directory
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".house"));

        // Convert to a String array
        if (files != null)
        {
            String[] fileNames = new String[files.length];
            for (int i = 0; i < files.length; i++)
            {
                fileNames[i] = files[i].getName();
            }
            return fileNames;
        }

        // Return an empty array if no files are found
        return new String[0];
    }

    /**
     * Extracts the third underscore-separated value (house type) from a template name.
     * Example: "object/tangible/player_house/tatooine_small_style_01.iff" -> "tatooine"
     */
    private String extractHouseType(String buildingTemplate)
    {
        if (buildingTemplate == null || buildingTemplate.isEmpty())
        {
            return null;
        }

        // Split the template by underscores
        String[] parts = buildingTemplate.split("_");

        // Ensure there are at least three parts (dynamic checks)
        if (parts.length >= 3)
        {
            return parts[2]; // Return the third part
        }

        return null;
    }

    private String[] listFilesMatchingPattern(String directoryPath, String pattern)
    {
        // List all files in the directory
        File dir = new File(directoryPath);
        if (!dir.exists() || !dir.isDirectory())
        {
            LOG("ethereal", "[getSavedHousingLayouts]: Directory not found: " + directoryPath);
            return null;
        }

        // Filter files based on the pattern
        File[] files = dir.listFiles((d, name) -> name.startsWith(pattern));
        if (files == null)
        {
            return new String[0];
        }

        // Collect matching file names
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++)
        {
            fileNames[i] = files[i].getName();
        }

        return fileNames;
    }

    public void getSavedHousingLayouts(obj_id self, obj_id player) throws InterruptedException
    {
        String directoryPath = "/home/swg/swg-main/exe/linux/server/housing_exports/";
        String[] allFiles = listAllFiles(directoryPath);

        // Check if there are files to display
        if (allFiles.length == 0)
        {
            LOG("ethereal", "[Housing Import/Export]: No saved layout files found.");
            return;
        }

        setScriptVar(self, "housing.import.targets", allFiles);
        int pid = listbox(self, player, "Select a saved layout file to load:", OK_CANCEL, "Saved Housing Layouts", allFiles, "handleHousingLayoutSelection");
        sui.showSUIPage(pid);
    }

    public int handleHousingLayoutSelection(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "[Housing Import/Export]: Handler Params: " + params);
        obj_id player = getPlayerId(params);
        int selectedIndex = getListboxSelectedRow(params);
        String[] fileList = getStringArrayScriptVar(self, "housing.import.targets");
        LOG("ethereal", "[Housing Import/Export]: Importing housing layout from file: " + fileList[selectedIndex]);
        importHousingContents(player, getTopMostContainer(player), fileList[selectedIndex]);
        return SCRIPT_CONTINUE;
    }

    public void createJunkTokenClicky(obj_id self) throws InterruptedException
    {
        obj_id inventory = getInventoryContainer(self);
        obj_id token = create.createObject("object/tangible/storyteller/story_token_prop.iff", inventory, "");
        attachScript(token, "developer.bubbajoe.junk_randomizer");
        broadcast(self, "Junk Token Clicky created.");
    }

    public int handleAllPrompts(obj_id self, dictionary params) throws InterruptedException
    {
        int buttonPressed = getIntButtonPressed(params);
        obj_id person = getPlayerId(params);

        if (buttonPressed == BP_OK)
        {
            // If OK button is pressed, continue the script without any changes
            return SCRIPT_CONTINUE;
        }

        if (buttonPressed == BP_REVERT)
        {
            forceCloseSUIPage(getIntScriptVar(self, "promptsPid"));
            response_store.printAllPrompts(self);
            chat.chat(self, "The prompt list has been updated.");

            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleUtilitySpawnerChoice(obj_id self, dictionary params) throws InterruptedException
    {
        int button = getIntButtonPressed(params);
        obj_id player = getPlayerId(params);
        int idx = getListboxSelectedRow(params);

        if (button == BP_CANCEL)
        {
            broadcast(self, "no spawner :(");
            return SCRIPT_CONTINUE;
        }

        if (button == BP_OK)
        {
            switch (idx)
            {
                case 0:
                    broadcast(self, "Medical Droid Spawner spawned");
                    spawnUtilityEgg(self, getLocation(self), 0);
                    break;
                case 1:
                    broadcast(self, "Tactical Probe Droid Spawner spawned");
                    spawnUtilityEgg(self, getLocation(self), 1);
                    break;
                case 2:
                    broadcast(self, "Entertainer Spawner spawned");
                    spawnUtilityEgg(self, getLocation(self), 2);
                    break;
                case 3:
                    broadcast(self, "Artisan Spawner spawned");
                    spawnUtilityEgg(self, getLocation(self), 3);
                    break;
                default:
                    break;
            }

        }
        return SCRIPT_CONTINUE;
    }

    public void spawnUtilityEgg(obj_id self, location where, int type) throws InterruptedException
    {
        obj_id spawner = create.object("object/tangible/spawning/spawn_egg.iff", where);
        setYaw(spawner, getYaw(self));
        setObjVar(spawner, "utility_type", type);
        attachScript(spawner, "content.npe2.utility_spawners");
    }

    public int handleSliderTest(obj_id self, dictionary params) throws InterruptedException
    {
        LOG("ethereal", "Hit handleSliderTest");
        int button = getIntButtonPressed(params);
        LOG("ethereal", "Button: " + button);
        obj_id player = getPlayerId(params);
        LOG("ethereal", "Player: " + player);
        int sliderValue = 0;
        LOG("ethereal", "Slider Value: " + sliderValue);

        if (button == BP_CANCEL)
        {
            LOG("ethereal", "Cancel pressed");
            return SCRIPT_CONTINUE;
        }

        if (button == BP_OK)
        {
            LOG("ethereal", "OK pressed");
            broadcast(self, "Slider Value: " + sliderValue);
            return SCRIPT_CONTINUE;
        }
        LOG("ethereal", "End of handleSliderTest");
        return SCRIPT_CONTINUE;
    }

    public int handleCountdownTest(obj_id self, dictionary params)
    {
        broadcast(self, "Countdown timer done.");
        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // TANGIBLE DYNAMICS TEST SUI HANDLERS
    // =========================================================================

    public int handleDynamicsTestSUI(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
            return SCRIPT_CONTINUE;

        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            removeObjVar(self, "dynamics_test");
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0)
            return SCRIPT_CONTINUE;

        obj_id target = getObjIdObjVar(self, "dynamics_test.target");
        if (!isIdValid(target))
        {
            broadcast(self, "Invalid target. Please re-select and try again.");
            removeObjVar(self, "dynamics_test");
            return SCRIPT_CONTINUE;
        }

        switch (idx)
        {
            case 0: // Enable Dynamics
                attachScript(target, "handler.tangible_dynamics_handler");
                setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
                broadcast(self, "Dynamics ENABLED on " + getName(target));
                break;

            case 1: // Disable Dynamics
                clearCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
                broadcast(self, "Dynamics DISABLED on " + getName(target));
                break;

            case 2: // Separator - do nothing
                break;

            case 3: // Breathing Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyBreathingEffect(target, 0.85f, 1.15f, 0.8f, -1.0f);
                broadcast(self, "Breathing effect applied (0.85-1.15 scale, speed 0.8)");
                break;

            case 4: // Spin Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applySpinForce(target, 3.14159f, 0.0f, 0.0f, -1.0f, false);
                broadcast(self, "Spin effect applied (PI rad/s yaw)");
                break;

            case 5: // Push Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyPushForce(target, 0.0f, 5.0f, 0.0f, 3.0f, tangible_dynamics.SPACE_WORLD);
                broadcast(self, "Push effect applied (upward 5m/s for 3s)");
                break;

            case 6: // Push with Drag
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyPushForceWithDrag(target, 5.0f, 0.0f, 0.0f, 1.5f, -1.0f, tangible_dynamics.SPACE_WORLD);
                broadcast(self, "Push with drag applied (5m/s sideways, drag 1.5)");
                break;

            case 7: // Bounce Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyBounceEffect(target, 9.8f, 0.7f, 8.0f, 10.0f);
                broadcast(self, "Bounce effect applied (gravity 9.8, elasticity 0.7, launch 8m/s)");
                break;

            case 8: // Wobble Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyWobbleEffect(target, 0.3f, 0.2f, 0.3f, 1.0f, 1.5f, 0.8f, -1.0f);
                broadcast(self, "Wobble effect applied");
                break;

            case 9: // Orbit Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                location loc = getLocation(target);
                tangible_dynamics.applyOrbitEffect(target, loc.x, loc.y, loc.z, 3.0f, 3.14159f, -1.0f);
                broadcast(self, "Orbit effect applied (3m radius, PI rad/s)");
                break;

            case 10: // Hover Effect
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyHoverEffect(target, 1.5f, 0.15f, 0.8f, -1.0f);
                broadcast(self, "Hover effect applied (1.5m height, 0.15m bob, 0.8 speed)");
                break;

            case 11: // Follow Target Effect - target follows the player
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyFollowTargetEffect(target, self, 3.0f, 4.0f, 1.5f, 0.1f, -1.0f);
                broadcast(self, "Follow target effect applied - " + getName(target) + " now follows you!");
                break;

            case 12: // Conveyor Effect - linear movement with wrap
                attachScript(target, "handler.tangible_dynamics_handler");
                location myHeading = getHeading(self);
                tangible_dynamics.applyConveyorEffectWithWrap(target, myHeading.x, 0.0f, myHeading.z, 2.0f, 10.0f);
                broadcast(self, "Conveyor effect applied (2m/s, wraps at 10m in your facing direction)");
                break;

            case 13: // Sway Effect - pendulum swing
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applySwayEffect(target, 0.15f, 0.8f, 0.0f, -1.0f);
                broadcast(self, "Sway effect applied (0.15 rad swing, 0.8 speed)");
                break;

            case 14: // Shake Effect - vibrate
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyShakeEffect(target, 0.15f, 12.0f, 5.0f);
                broadcast(self, "Shake effect applied (0.15m intensity, 12Hz, 5s duration)");
                break;

            case 15: // Float Effect - levitate with drift
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyFloatEffect(target, 0.6f, 0.4f, 0.15f, -1.0f);
                broadcast(self, "Float effect applied (0.6m height, 0.4 speed, 0.15m random drift)");
                break;

            case 16: // Carousel Effect - rotating platform
                attachScript(target, "handler.tangible_dynamics_handler");
                location carouselLoc = getLocation(target);
                tangible_dynamics.applyCarouselEffect(target, carouselLoc.x, carouselLoc.y, carouselLoc.z, 3.0f, 1.0f, 0.0f, 1.0f, -1.0f);
                broadcast(self, "Carousel effect applied (3m radius, 1 rad/s)");
                break;

            case 17: // Ferris Wheel Effect - carousel + vertical oscillation
                attachScript(target, "handler.tangible_dynamics_handler");
                location ferrisLoc = getLocation(target);
                tangible_dynamics.applyCarouselEffect(target, ferrisLoc.x, ferrisLoc.y, ferrisLoc.z, 3.0f, 0.5f, 2.0f, 1.0f, -1.0f);
                broadcast(self, "Ferris wheel effect applied (3m radius, 2m vertical amp)");
                break;

            case 18: // Combined
                attachScript(target, "handler.tangible_dynamics_handler");
                tangible_dynamics.applyCombinedForces(target, 0.0f, 1.5f, 0.0f, 1.57f, 0.0f, 0.0f, 0.9f, 1.1f, 1.0f, -1.0f);
                broadcast(self, "Combined forces applied (push + spin + breathing)");
                break;

            case 19: // Separator - do nothing
                break;

            case 20: // Enable Collision Push
                removeObjVar(target, "collideBlock");
                setCondition(target, CONDITION_MAGIC_TANGIBLE_DYNAMIC);
                attachScript(target, "handler.tangible_dynamics_handler");
                broadcast(self, "Collision push ENABLED (hockey puck mode)");
                break;

            case 21: // Disable Collision Push
                setObjVar(target, "collideBlock", 1);
                broadcast(self, "Collision push DISABLED");
                break;

            case 22: // Set Collision Radius
                setObjVar(self, "dynamics_test.param", "collisionRadius");
                sui.inputbox(self, self, "Enter collision radius (meters):", "Set Collision Radius", "handleDynamicsParamInput", "1.0");
                return SCRIPT_CONTINUE;

            case 23: // Set Push Speed
                setObjVar(self, "dynamics_test.param", "pushSpeed");
                sui.inputbox(self, self, "Enter push speed (m/s):", "Set Push Speed", "handleDynamicsParamInput", "5.0");
                return SCRIPT_CONTINUE;

            case 24: // Set Push Drag
                setObjVar(self, "dynamics_test.param", "pushDrag");
                sui.inputbox(self, self, "Enter push drag coefficient:", "Set Push Drag", "handleDynamicsParamInput", "1.5");
                return SCRIPT_CONTINUE;

            case 25: // Separator - do nothing
                break;

            case 26: // Clear Push
                tangible_dynamics.clearPushForce(target);
                broadcast(self, "Push force cleared");
                break;

            case 27: // Clear Spin
                tangible_dynamics.clearSpinForce(target);
                broadcast(self, "Spin force cleared");
                break;

            case 28: // Clear Breathing
                tangible_dynamics.clearBreathingEffect(target);
                broadcast(self, "Breathing effect cleared");
                break;

            case 29: // Clear Bounce
                tangible_dynamics.clearBounceEffect(target);
                broadcast(self, "Bounce effect cleared");
                break;

            case 30: // Clear Wobble
                tangible_dynamics.clearWobbleEffect(target);
                broadcast(self, "Wobble effect cleared");
                break;

            case 31: // Clear Orbit
                tangible_dynamics.clearOrbitEffect(target);
                broadcast(self, "Orbit effect cleared");
                break;

            case 32: // Clear Hover
                tangible_dynamics.clearHoverEffect(target);
                broadcast(self, "Hover effect cleared");
                break;

            case 33: // Clear Follow Target
                tangible_dynamics.clearFollowTargetEffect(target);
                broadcast(self, "Follow target effect cleared");
                break;

            case 34: // Clear Conveyor
                tangible_dynamics.clearConveyorEffect(target);
                broadcast(self, "Conveyor effect cleared");
                break;

            case 35: // Clear Sway
                tangible_dynamics.clearSwayEffect(target);
                broadcast(self, "Sway effect cleared");
                break;

            case 36: // Clear Shake
                tangible_dynamics.clearShakeEffect(target);
                broadcast(self, "Shake effect cleared");
                break;

            case 37: // Clear Float
                tangible_dynamics.clearFloatEffect(target);
                broadcast(self, "Float effect cleared");
                break;

            case 38: // Clear Carousel
                tangible_dynamics.clearCarouselEffect(target);
                broadcast(self, "Carousel effect cleared");
                break;

            case 39: // Clear ALL
                tangible_dynamics.clearAllForces(target);
                broadcast(self, "ALL dynamics forces cleared");
                break;

            case 40: // Separator - do nothing
                break;

            case 41: // Make Mountable
                tangible_dynamics.makeMountable(target, 0.5f);
                broadcast(self, "Object is now mountable (sit on it via radial menu)");
                break;

            case 42: // Create Hover Platform
                tangible_dynamics.createHoverPlatform(target, 1.5f, 0.5f);
                broadcast(self, "Hover platform created (1.5m hover, mountable)");
                break;

            case 43: // Create Carousel Ride
                tangible_dynamics.createCarouselRide(target, 3.0f, 0.5f, 0.5f);
                broadcast(self, "Carousel ride created (3m radius, mountable)");
                break;

            case 44: // Create Ferris Wheel Seat
                location ferrisCenter = getLocation(target);
                tangible_dynamics.createFerrisWheelSeat(target, ferrisCenter.x, ferrisCenter.y, ferrisCenter.z, 4.0f, 0.3f, 3.0f, 0.5f);
                broadcast(self, "Ferris wheel seat created (4m radius, 3m vertical amp, mountable)");
                break;

            default:
                broadcast(self, "Unknown option selected: " + idx);
                break;
        }

        // Re-open the menu (don't close until Cancel is pressed)
        showDynamicsTestSUI(self, target);
        return SCRIPT_CONTINUE;
    }

    /**
     * Helper method to show the TangibleDynamics test SUI panel
     */
    private void showDynamicsTestSUI(obj_id self, obj_id target) throws InterruptedException
    {
        if (!isIdValid(target))
        {
            broadcast(self, "Invalid target.");
            removeObjVar(self, "dynamics_test");
            return;
        }

        // Store target for handler
        setObjVar(self, "dynamics_test.target", target);

        String[] options = new String[] {
            "Enable Dynamics (attach handler + set condition)",
            "Disable Dynamics (clear condition)",
            "------- EFFECTS -------",
            "Apply Breathing Effect (pulse scale)",
            "Apply Spin Effect (rotate)",
            "Apply Push Effect (shove)",
            "Apply Push with Drag (slides then stops)",
            "Apply Bounce Effect (gravity bounce)",
            "Apply Wobble Effect (oscillate position)",
            "Apply Orbit Effect (circle around point)",
            "Apply Hover Effect (terrain-following float)",
            "Apply Follow Target (target follows YOU)",
            "Apply Conveyor Effect (linear movement with wrap)",
            "Apply Sway Effect (pendulum swing)",
            "Apply Shake Effect (vibrate)",
            "Apply Float Effect (levitate with drift)",
            "Apply Carousel Effect (rotating platform)",
            "Apply Ferris Wheel Effect (carousel + vertical)",
            "Apply Combined (push + spin + breathing)",
            "------- COLLISION -------",
            "Enable Collision Push (hockey puck)",
            "Disable Collision Push (set collideBlock)",
            "Set Collision Radius...",
            "Set Push Speed...",
            "Set Push Drag...",
            "------- CLEAR -------",
            "Clear Push Force",
            "Clear Spin Force",
            "Clear Breathing Effect",
            "Clear Bounce Effect",
            "Clear Wobble Effect",
            "Clear Orbit Effect",
            "Clear Hover Effect",
            "Clear Follow Target Effect",
            "Clear Conveyor Effect",
            "Clear Sway Effect",
            "Clear Shake Effect",
            "Clear Float Effect",
            "Clear Carousel Effect",
            "Clear ALL Forces",
            "------- MOUNTING -------",
            "Make Mountable (sit on object)",
            "Create Hover Platform (hover + mountable)",
            "Create Carousel Ride (carousel + mountable)",
            "Create Ferris Wheel Seat"
        };

        sui.listbox(self, self, "Select a TangibleDynamics option for: " + getName(target),
            sui.OK_CANCEL, "TangibleDynamics Test Panel", options, "handleDynamicsTestSUI", true, false);
    }

    public int handleDynamicsParamInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
            return SCRIPT_CONTINUE;

        int btn = sui.getIntButtonPressed(params);
        if (btn == sui.BP_CANCEL)
        {
            removeObjVar(self, "dynamics_test");
            return SCRIPT_CONTINUE;
        }

        obj_id target = getObjIdObjVar(self, "dynamics_test.target");
        String paramName = getStringObjVar(self, "dynamics_test.param");

        if (!isIdValid(target) || paramName == null || paramName.isEmpty())
        {
            broadcast(self, "Invalid target or parameter.");
            removeObjVar(self, "dynamics_test");
            return SCRIPT_CONTINUE;
        }

        String inputText = sui.getInputBoxText(params);
        float value = utils.stringToFloat(inputText);

        if (value <= 0.0f)
        {
            broadcast(self, "Invalid value. Must be a positive number.");
            removeObjVar(self, "dynamics_test");
            return SCRIPT_CONTINUE;
        }

        if (paramName.equals("collisionRadius"))
        {
            setObjVar(target, "dynamics.collisionRadius", value);
            broadcast(self, "Collision radius set to " + value + "m on " + getName(target));
        }
        else if (paramName.equals("pushSpeed"))
        {
            setObjVar(target, "dynamics.pushSpeed", value);
            broadcast(self, "Push speed set to " + value + " m/s on " + getName(target));
        }
        else if (paramName.equals("pushDrag"))
        {
            setObjVar(target, "dynamics.pushDrag", value);
            broadcast(self, "Push drag set to " + value + " on " + getName(target));
        }

        // Re-open the menu
        showDynamicsTestSUI(self, target);
        return SCRIPT_CONTINUE;
    }
}


