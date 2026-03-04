package script.library;/*
@Origin: dsrc.script.library
@Author: BubbaJoeX
@Purpose: Base library for SWG: Titan.
@Notes:
    This file contains config settings for Discord integration.
    This file contains the handling for the World Boss system and announcements.
    This file contains some custom group functions.
@Created: Sunday, 10/1/2023, at 12:56 PM.
@Requirements: <no requirements>
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

public class titan_player extends base_script
{
    public static final string_id SID_PROMPT = new string_id("nb_player", "ui_list_objects_prompt");
    public static final string_id SID_TITLE = new string_id("nb_player", "ui_list_objects_title");
    public static final String PUSH_WEBHOOK_PRIVATE = getConfigSetting("GameServer", "leaderboardDiscordWebhook");
    public static final String PUSH_WEBHOOK_PUBLIC = getConfigSetting("GameServer", "galacticFeedDiscordWebhook");
    public static final String PUSH_WEBHOOK_LIVEEVENTS = getConfigSetting("GameServer", "liveEventsFeedDiscordWebhook");
    public static final String PUSH_WEBHOOK_TRADE = getConfigSetting("GameServer", "tradeFeedDiscordWebhook");
    public static final String PUSH_WEBHOOK_WORLD_BOSS = getConfigSetting("GameServer", "worldBossFeedDiscordWebhook");
    public static final String PUSH_WEBHOOK_SERVICES = getConfigSetting("GameServer", "servicesFeedDiscordWebhook");
    public static final String PUSH_WEBHOOK_GCW = getConfigSetting("GameServer", "gcwFeedDiscordWebhook");
    public static final String PUSH_WEBHOOK_SPACE_GCW = getConfigSetting("GameServer", "gcwSpaceFeedDiscordWebhook");
    public static final String PUSH_AVATAR = "https://i.imgur.com/rOltoeF.jpeg";
    public static final float WORLD_BOSS_CREDIT_RANGE = 75.0f;
    public static int WORLD_BOSS_PEKO = 0;
    public static int WORLD_BOSS_KRAYT = 1;
    public static int WORLD_BOSS_PAX = 2;
    public static int WORLD_BOSS_GIZMO = 3;
    public static int WORLD_BOSS_DONKDONK = 4;
    public static int WORLD_BOSS_AURRA = 5;
    public static int WORLD_BOSS_EMPERORS_HAND = 6;
    public static int WORLD_BOSS_IG24 = 7;
    public static int WORLD_BOSS_DHANAK = 8;
    public static String[] ADMIN_PREFIXES = {
            "Dev-", "Admin-", "CSR-", "QA-", "IA-", "GM-", "Event-"
    };

    public titan_player()
    {
    }

    public static int spawnWithScript(obj_id self, String template, String script) throws InterruptedException
    {
        location loc = getLocation(self);
        obj_id spawned = create.object(template, loc);
        attachScript(spawned, script);
        return SCRIPT_CONTINUE;
    }

    public boolean isExecutive(obj_id who)
    {
        String[] accountNames = dataTableGetStringColumn("datatables/admin/new_beginnings.iff", "AdminAccounts");
        return Arrays.asList(accountNames).contains(getPlayerAccountUsername(who));
    }

    public static int setupLootAmount(obj_id what, int amount)
    {
        setObjVar(what, "loot.numItems", amount);
        return SCRIPT_CONTINUE;
    }

    public static boolean isEthereal(obj_id player)
    {
        String name = getPlayerFullName(player);
        for (String ADMIN_PREFIX : ADMIN_PREFIXES)
        {
            return name.startsWith(ADMIN_PREFIX);
        }
        return false;
    }

    /** Dynamically create and execute a Discord webhook. No-op if DiscordWebhook is missing or throws. */
    private static void executeDiscordWebhook(String webhookUrl, String content, String username, boolean tts)
    {
        if (webhookUrl == null || webhookUrl.length() == 0)
            return;
        try
        {
            Class<?> clazz = Class.forName("script.DiscordWebhook");
            Constructor<?> ctor = clazz.getConstructor(String.class);
            Object hook = ctor.newInstance(webhookUrl);
            if (content != null)
            {
                Method setContent = clazz.getMethod("setContent", String.class);
                setContent.invoke(hook, content);
            }
            if (username != null)
            {
                Method setUsername = clazz.getMethod("setUsername", String.class);
                setUsername.invoke(hook, username);
            }
            Method setTts = clazz.getMethod("setTts", boolean.class);
            setTts.invoke(hook, Boolean.valueOf(tts));
            Method setAvatarUrl = clazz.getMethod("setAvatarUrl", String.class);
            setAvatarUrl.invoke(hook, PUSH_AVATAR);
            Method execute = clazz.getMethod("execute");
            execute.invoke(hook);
        }
        catch (Throwable t)
        {
            // Webhook failures must never break gameplay.
        }
    }

    public static void sendToDiscord(obj_id context, String action)  //this one is for user info (private)
    {
        if (isGod(context))
        {
            String content = "(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action;
            String username = isPlayer(context) ? getPlayerFullName(context) : "Judicator";
            executeDiscordWebhook(PUSH_WEBHOOK_PRIVATE, content, username, true);
        }
    }

    public static void sendLiveEventUpdateToDiscord(obj_id context, String action)  //event staff command to send live event announcements.
    {
        String username = isPlayer(context) ? ("Event Staff: " + getPlayerFullName(context)) : ("Live Event: " + getClusterName());
        executeDiscordWebhook(PUSH_WEBHOOK_LIVEEVENTS, action, username, true);
    }

    public static void sendToDiscord(obj_id context, String action, String username)  // this one is also for telemetry
    {
        String content = "(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action;
        executeDiscordWebhook(PUSH_WEBHOOK_PRIVATE, content, username, true);
    }

    public static void sendToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp)  //this is for gameplay actions to promote interaction with mechanics in game
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        executeDiscordWebhook(PUSH_WEBHOOK_PUBLIC, content, username, useTTS);
    }

    public static void sendToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp, boolean internal)  //this is for gameplay actions to promote interaction with mechanics in game
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        String url = internal ? PUSH_WEBHOOK_PRIVATE : PUSH_WEBHOOK_PUBLIC;
        executeDiscordWebhook(url, content, username, useTTS);
    }

    public static void sendWorldBossUpdateToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp, boolean internal)  //this is for gameplay actions to promote interaction with mechanics in game
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        String url = internal ? PUSH_WEBHOOK_PRIVATE : PUSH_WEBHOOK_WORLD_BOSS;
        executeDiscordWebhook(url, content, username, useTTS);
    }

    public static void sendGCWUpdateToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp, boolean internal)  //this is for gameplay actions to promote interaction with mechanics in game
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        String url = internal ? PUSH_WEBHOOK_PRIVATE : PUSH_WEBHOOK_GCW;
        executeDiscordWebhook(url, content, username, useTTS);
    }

    public static void sendSpaceGCWUpdateToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp, boolean internal)
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        String url = internal ? PUSH_WEBHOOK_PRIVATE : PUSH_WEBHOOK_SPACE_GCW;
        executeDiscordWebhook(url, content, username, useTTS);
    }

    public static void sendTradeUpdateToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp, boolean internal)  //this is for gameplay actions to promote interaction with mechanics in game
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        String url = internal ? PUSH_WEBHOOK_PRIVATE : PUSH_WEBHOOK_TRADE;
        executeDiscordWebhook(url, content, username, useTTS);
    }

    public static void sendServicesUpdateToDiscord(obj_id context, String action, String username, boolean useTTS, boolean timestamp, boolean internal)  //this is for gameplay actions to promote interaction with mechanics in game
    {
        String content = timestamp ? ("(" + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + ") " + action) : action;
        String url = internal ? PUSH_WEBHOOK_PRIVATE : PUSH_WEBHOOK_SERVICES;
        executeDiscordWebhook(url, content, username, useTTS);
    }

    public static void sendToOrigin(obj_id player)
    {
        location loc = getLocation(player);
        obj_id planet = getPlanetByName(loc.area);
        warpPlayer(player, loc.area, loc.x, loc.y, loc.z, null, 0, 0, 0);
    }

    public static void listAllPumpkins(obj_id player)
    {
        obj_id[] allPumpkins = getAllObjectsWithTemplate(getLocation(player), 1000, "object/tangible/holiday/halloween/pumpkin_object.iff");
        if (allPumpkins == null || allPumpkins.length == 0)
        {
            broadcast(player, "No pumpkins found.");
            return;
        }
        for (obj_id allPumpkin : allPumpkins)
        {
            broadcast(player, "Pumpkin: " + allPumpkin);
        }
    }

    public static int moveAllPlayers(obj_id who, dictionary params)
    {
        String planet = params.getString("planet");
        float x = params.getFloat("x");
        float y = params.getFloat("y");
        float z = params.getFloat("z");
        obj_id[] players = getAllPlayers(getLocation(who), 1000);
        if (players == null || players.length == 0)
        {
            sendSystemMessage(who, "No players found.", null);
            return SCRIPT_CONTINUE;
        }
        for (obj_id player : players)
        {
            warpPlayer(player, planet, x, y, z, null, 0, 0, 0);
        }
        LOG("bubbajoe", "moveAllPlayers() - " + players.length + " players moved to " + planet + " " + x + " " + y + " " + z);
        return SCRIPT_CONTINUE;
    }

    public static void pushPlayer(obj_id who, obj_id target, float distance, float angle)
    {

        location loc = getLocation(target);
        float x = loc.x + (float) Math.cos(angle) * distance;
        float z = loc.z + (float) Math.sin(angle) * distance;
        warpPlayer(target, loc.area, x, loc.y, z, null, 0, 0, 0);
        debugServerConsoleMsg(who, "pushPlayer() - player pushed.");
    }

    public static int explode(obj_id who, dictionary params)
    {
        location loc = getLocation(who);
        playClientEffectLoc(who, "clienteffect/combat_explosion_lair_large.cef", loc, 0);
        return SCRIPT_CONTINUE;
    }

    public static void renameItems(obj_id who, obj_id target, String name)
    {
        obj_id[] items = getInventoryAndEquipment(target);
        if (items == null || items.length == 0)
        {
            sendSystemMessage(who, "No items found.", null);
            return;
        }
        for (obj_id item : items)
        {
            setName(item, name);
        }
    }

    public static void createCreatureGrid(obj_id who, obj_id target, String creature, int rows, int columns, float distance) throws InterruptedException
    {
        location loc = getLocation(target);
        float startX = loc.x - ((columns - 1) * distance) / 2;
        float startZ = loc.z - ((rows - 1) * distance) / 2;

        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < columns; j++)
            {
                float x = startX + (j * distance);
                float z = startZ + (i * distance);

                obj_id creatureObj = create.object(creature, new location(x, loc.y, z, loc.area));

                if (isIdValid(creatureObj))
                {
                    broadcast(who, "Creature created at (" + x + ", " + loc.y + ", " + z + ")");
                }
                else
                {
                    broadcast(who, "Failed to create creature at (" + x + ", " + loc.y + ", " + z + ")");
                }
            }
        }

        broadcast(who, "createCreatureGrid() - Creature grid created with " + (rows * columns) + " creatures.");
    }

    public static void createCreatureArch(obj_id who, obj_id target, String creature, int count, float radius, float angle) throws InterruptedException
    {
        location loc = getLocation(target);
        float centerX = loc.x;
        float centerZ = loc.z;

        float startAngle = -angle / 2;
        float angleStep = angle / (count - 1);

        for (int i = 0; i < count; i++)
        {
            double radians = Math.toRadians(startAngle + (i * angleStep));
            float x = centerX + (float) (radius * Math.cos(radians));
            float z = centerZ + (float) (radius * Math.sin(radians));

            obj_id creatureObj = create.object(creature, new location(x, loc.y, z, loc.area));

            if (isIdValid(creatureObj))
            {
                broadcast(who, "Creature created at (" + x + ", " + loc.y + ", " + z + ")");
            }
            else
            {
                broadcast(who, "Failed to create creature at (" + x + ", " + loc.y + ", " + z + ")");
            }
        }

        broadcast(who, "createCreatureArch() - Arch of " + count + " creatures created.");
    }

    public static int createCircleSpawn(obj_id self, obj_id target, String creature, int amount, float distance) throws InterruptedException
    {
        if (!isIdValid(target) || !exists(target))
        {
            return SCRIPT_CONTINUE;
        }
        location loc = getLocation(target);
        float x;
        float z;
        for (int i = 0; i < amount; i++)
        {
            float angle = (float) (i * (360 / amount));
            x = loc.x + (float) Math.cos(angle) * distance;
            z = loc.z + (float) Math.sin(angle) * distance;
            obj_id creatureObj = create.object(creature, new location(x, loc.y, z, loc.area));
            faceTo(self, creatureObj);
        }
        return SCRIPT_CONTINUE;
    }

    public static String setRainbowName(obj_id target)
    {
        String name = getName(target);
        StringBuilder rainbowName = new StringBuilder();
        for (int i = 0; i < name.length(); i++)
        {
            Random obj = new Random();
            int rand_num = obj.nextInt(0xffffff + 1);
            rainbowName.append("\\#").append(rand_num).append(name.charAt(i));
        }
        return rainbowName.toString();
    }

    public static void relax(obj_id target)
    {
        if (!isIdValid(target) || !exists(target))
        {
            debugServerConsoleMsg(target, "relax() - target is not a valid object.");
        }
        if (!isPlayer(target))
        {
            debugServerConsoleMsg(target, "relax() - target is not a player.");
        }
        setPosture(target, POSTURE_SITTING);
        debugServerConsoleMsg(target, "relax() - player relaxed.");
    }

    public static void suspicious(obj_id target)
    {
        if (!isIdValid(target) || !exists(target))
        {
            debugServerConsoleMsg(target, "suspicious() - target is not a valid object.");
        }
        if (!isPlayer(target))
        {
            debugServerConsoleMsg(target, "suspicious() - target is not a player.");
        }
        setPosture(target, POSTURE_CROUCHED);
        debugServerConsoleMsg(target, "suspicious() - player is now suspicious.");
    }

    public static void sneak(obj_id target)
    {
        if (!isIdValid(target) || !exists(target))
        {
            debugServerConsoleMsg(target, "sneak() - target is not a valid object.");
        }
        if (!isPlayer(target))
        {
            debugServerConsoleMsg(target, "sneak() - target is not a player.");
        }
        setPosture(target, POSTURE_SNEAKING);
        debugServerConsoleMsg(target, "sneak() - player is now suspicious.");
    }

    public static void bankruptPlayer(obj_id self, obj_id target)
    {
        if (!isIdValid(target) || !exists(target))
        {
            debugServerConsoleMsg(self, "bankruptPlayer() - target is not a valid object.");
        }
        if (!isPlayer(target))
        {
            debugServerConsoleMsg(self, "bankruptPlayer() - target is not a player.");
        }
        int cash = getCashBalance(target);
        int bank = getBankBalance(target);
        if (cash > 0)
        {
            withdrawCashFromBank(target, cash, "noHandler", null, null);
        }
        if (bank > 0)
        {
            withdrawCashFromBank(target, bank, "noHandler", null, null);
        }
        debugServerConsoleMsg(self, "bankruptPlayer() - player is now bankrupt.");
    }

    public static boolean isNear(obj_id self, obj_id target, float distance)
    {
        if (!isIdValid(target) || !exists(target))
        {
            debugServerConsoleMsg(self, "isNear() - target is not a valid object.");
        }
        if (!isPlayer(target))
        {
            debugServerConsoleMsg(self, "isNear() - target is not a player.");
        }
        if (distance < 0)
        {
            debugServerConsoleMsg(self, "isNear() - distance is less than 0.");
        }
        location loc = getLocation(self);
        location targetLoc = getLocation(target);
        float dist = getDistance(loc, targetLoc);
        return dist <= distance;
    }

    public void sendPlayerToHomeBind(obj_id target)
    {
        if (!isIdValid(target) || !exists(target))
        {
            LOG("ethereal", "[Homing Beacon]: sendPlayerHomeBind() - target is not a valid object.");
        }
        if (!isPlayer(target))
        {
            LOG("ethereal", "[Homing Beacon]: sendPlayerHomeBind() - target is not a player.");
        }
        location homeBind = getHomeBind(target);
        warpPlayer(target, homeBind.area, homeBind.x, homeBind.y, homeBind.z, null, 0, 0, 0);
        LOG("ethereal", "[Homing Beacon]: " + getPlayerFullName(target) + " has used a homing beacon to return to their home bind. (" + homeBind.toReadableFormat(true) + ")");
    }

    public location getHomeBind(obj_id player)
    {
        return getLocationObjVar(player, "home_bind");
    }

    public static int getNumberOfPlayersInRange(obj_id self, float distance)
    {
        if (distance < 0)
        {
            debugServerConsoleMsg(self, "getNumberOfPlayersInRange() - distance is less than 0.");
        }
        obj_id[] players = getPlayerCreaturesInRange(getLocation(self), distance);
        return players.length;
    }

    public static void launchWookieepediaPage(obj_id self, String page) throws InterruptedException
    {
        if (page == null || page.isEmpty())
        {
            debugServerConsoleMsg(self, "launchWookieepediaPage() - page is null or empty.");
        }
        String replacedText = page.replaceAll(" ", "_");
        String url = "https://starwars.wikia.com/wiki/" + page;
        sui.msgbox(self, self, "Opening " + url + " in your browser.", sui.OK_ONLY, "Wookieepedia", "noHandler");
        launchClientWebBrowser(self, url);
        debugServerConsoleMsg(self, "launchWookieepediaPage() - wookieepedia page launched.");
    }

    public static void spawnAllItemsFromTable(obj_id self, String datatable)
    {
        if (datatable == null || datatable.isEmpty())
        {
            debugServerConsoleMsg(self, "spawnAllItemsFromTable() - datatable is null or empty.");
        }
        String[] items = dataTableGetStringColumnNoDefaults(datatable, "item");
        for (String s : items)
        {
            obj_id item = createObject(s, getLocation(self));
            if (isIdValid(item))
            {
                debugServerConsoleMsg(self, "spawnAllItemsFromTable() - item " + s + " spawned.");
            }
        }
    }

    public static void removeAllItemsOfTemplate(obj_id self, String template)
    {
        if (template == null || template.isEmpty())
        {
            debugServerConsoleMsg(self, "removeAllItemsOfTemplate() - template is null or empty.");
        }
        var items = getContents(self);
        for (obj_id item : items)
        {
            if (getTemplateName(item).equals(template))
            {
                destroyObject(item);
                debugServerConsoleMsg(self, "removeAllItemsOfTemplate() - item " + template + " removed.");
            }
        }
    }

    public static String getMemoryUsage()
    {
        Runtime runtime = Runtime.getRuntime();
        long memory = runtime.totalMemory() - runtime.freeMemory();
        return Long.toString(memory);
    }

    public static void requestMemoryUsage(obj_id self) throws InterruptedException
    {
        String memory = getMemoryUsage();
        sui.msgbox(self, self, "Memory usage: " + memory + " bytes.", sui.OK_ONLY, "Memory Usage", "noHandler");
        debugServerConsoleMsg(self, "requestMemoryUsage() - memory usage requested.");
        LOG("ethereal", "[Memory Usage]: requestMemoryUsage() - memory usage: " + getReadableMemoryUsage() + " bytes.");
    }

    public static String getReadableMemoryUsage()
    {
        Runtime runtime = Runtime.getRuntime();
        long memory = runtime.totalMemory() - runtime.freeMemory();
        return Long.toString(memory);
    }

    public static void warpGroup(obj_id groupId, location loc)
    {
        obj_id[] members = getGroupMemberIds(groupId);
        for (obj_id member : members)
        {
            if (isIdValid(member))
            {
                warpPlayer(member, loc.area, loc.x, loc.y, loc.z, null, 0, 0, 0);
            }
        }
    }

    public static void warpGroupCell(obj_id groupId, location loc)
    {
        obj_id[] members = getGroupMemberIds(groupId);
        for (obj_id member : members)
        {
            if (isIdValid(member))
            {
                warpPlayer(member, loc.area, loc.x, loc.y, loc.z, loc.cell, 0, 0, 0);
            }
        }
    }

    public static void warpGroupToPlayer(obj_id groupId, obj_id player)
    {
        location loc = getLocation(player);
        warpGroup(groupId, loc);
    }

    public static void warpGroupToObjectByName(obj_id groupId, String objectName, float tolerance)
    {
        obj_id[] objects = getAllObjectsWithTemplate(getLocation(groupId), tolerance, objectName);
        if (objects == null || objects.length == 0)
        {
            return;
        }
        location loc = getLocation(objects[0]);
        warpGroup(groupId, loc);
    }

    public static void rewardGroup(obj_id group_id, String item, boolean noTrade) throws InterruptedException
    {
        obj_id[] members = getGroupMemberIds(group_id);
        for (obj_id member : members)
        {
            obj_id inventory = utils.getInventoryContainer(member);
            if (isIdValid(member))
            {
                obj_id item_id = createObject(item, inventory, "");
                if (isIdValid(item_id))
                {
                    if (noTrade)
                    {
                        setObjVar(item_id, "noTrade", 1);
                        attachScript(item_id, "item.special.nomove");
                    }
                }
            }
        }
    }

    public static void logEthereal(String context, String logMessage)
    {
        LOG("ethereal", "[" + context + "]: " + logMessage);
    }

    public static String[] getAttackerList(obj_id target)
    {
        String[] attackerList = new String[0];
        if (isIdValid(target))
        {
            obj_id[] attackers = getHateList(target);
            if (attackers != null)
            {
                attackerList = new String[attackers.length];
                for (int i = 0; i < attackers.length; i++)
                {
                    if (isPlayer(attackers[i]))
                    {
                        attackerList[i] = getName(attackers[i]);
                    }
                }
            }
        }
        return attackerList;
    }

    public static void doWorldBossAnnounce(obj_id target, int worldboss)
    {
        location here = getLocation(target);
        switch (worldboss)
        {
            case 0:
                String pekoString = "The putrid Peko-Peko Empress has been spotted on Naboo! We are paying handsomely for it's corpse!";
                titan_player.sendWorldBossUpdateToDiscord(target, pekoString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(pekoString);
                break;
            case 1:
                String kraytString = "The Ancient Krayt Dragon has been seen hibernating on Tatooine! Guild Members are paying handsomely for any pearls!";
                titan_player.sendWorldBossUpdateToDiscord(target, kraytString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(kraytString);
                break;
            case 2:
                String mandoString = "The Mandalorian Crusader was recently seen traveling to Endor. The Guild is paying a credit case per ounce of beskar returned!";
                titan_player.sendWorldBossUpdateToDiscord(target, mandoString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(mandoString);
                break;
            case 3:
                String ewokString = " The Rebellion is looking to cull a force-sensitive human on Corellia. They are willing to pay a good price for the elimination of this threat!";
                titan_player.sendWorldBossUpdateToDiscord(target, ewokString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(ewokString);
                break;
            case 4:
                String gunganString = "The RSF is investigating a rogue Gungan on Rori. It was last seen heading toward the Rori Gungan Swamp Town. The RSF is willing to take a financial loss to get this threat out of the Chommell sector.";
                titan_player.sendWorldBossUpdateToDiscord(target, gunganString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(gunganString);
                break;
            case 5:
                String aurraString = "The Rebellion heard whispers of a cloned operative for the Empire. This operative was last seen on Talus. Hurry!";
                titan_player.sendWorldBossUpdateToDiscord(target, aurraString, "Bounty Hunters' Guild", true, false, true);
                broadcastGalaxy(aurraString);
                break;
            case 6:
                String handString = "Rebel Intelligence has put out a bounty for Mara Jade. Bring her remains and you will be paid most generously.";
                titan_player.sendWorldBossUpdateToDiscord(target, handString, "Bounty Hunters' Guild", true, false, true);
                broadcastGalaxy(handString);
            case 7:
                String droidString = "Nym has requested aid in powering off a rogue product from IG-88's Droid Factory. The Lok Revenants are paying a handsome price for this droid's servo-motor!";
                titan_player.sendWorldBossUpdateToDiscord(target, droidString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(droidString);
                break;
            case 8:
                String dhanakString = "A notorious spice smuggler from Kessel named Dhanak has been cornered on Mustafar. The Hutts and the Empire both want him dead - the bounty is astronomical!";
                titan_player.sendWorldBossUpdateToDiscord(target, dhanakString, "Bounty Hunters' Guild", true, false, false);
                broadcastGalaxy(dhanakString);
                break;
        }
    }

    public static void doWorldBossDeathMsg(obj_id target, obj_id killer) throws InterruptedException
    {
        if (isPlayer(killer))
        {
            if (group.isGrouped(killer))
            {
                obj_id[] players = group.getGroupMemberIds(group.getGroupObject(killer));
                StringBuilder names = new StringBuilder(" ");
                for (obj_id player : players)
                {
                    String creditName = getPlayerFullName(player);
                    names.append(toUpper(creditName, 0)).append(", ");
                }
                String worldBossWinMessage = "The world boss '" + getEncodedName(target) + "' has been defeated by the following adventurers: " + names + " Congratulations to all!";
                titan_player.sendWorldBossUpdateToDiscord(target, worldBossWinMessage, "Bounty Hunters' Guild", true, false, false);
            }
            else
            {
                String worldBossWinMessageLite = "The world boss '" + getEncodedName(target) + "' has been defeated by " + toUpper(getFirstName(killer), 0);
                titan_player.sendWorldBossUpdateToDiscord(target, worldBossWinMessageLite, "Bounty Hunters' Guild", true, false, false);
            }
        }
        else
        {
            LOG("ethereal", "[World Bosses]: World Boss (" + target + ") was killed by non-player object (" + killer + "). Investigate ASAP.");
        }
    }

    public boolean getConfig(String configString)
    {
        String enabled = toLower(getConfigSetting("NB", configString));
        if (enabled == null)
        {
            return false;
        }
        return enabled.equals("true") || enabled.equals("1");
    }

    public float getConfigFloat(String configString) throws InterruptedException
    {
        String value = toLower(getConfigSetting("NB", configString));
        if (value == null)
        {
            return 0.0f;
        }
        return utils.stringToFloat(value);
    }

    public int getConfigInt(String configString) throws InterruptedException
    {
        String value = toLower(getConfigSetting("NB", configString));
        if (value == null)
        {
            return 0;
        }
        return utils.stringToInt(value);
    }

    public int broadcastGroup(obj_id host, String message)
    {
        obj_id group = getGroupObject(host);
        if (isIdValid(group))
        {
            obj_id[] members = getGroupMemberIds(group);
            for (obj_id member : members)
            {
                if (isIdValid(member))
                {
                    broadcast(member, message);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int placePlayersAroundPoint(location point)
    {
        obj_id[] targets = getAllPlayers(point, 100.0f);
        for (obj_id player : targets)
        {
            // Make sure the player is valid
            if (isIdValid(player))
            {
                if (isPlayer(player))
                {
                    //place them in a ring facing the point.
                    float angle = rand(0, 360);
                    float distance = rand(1, 10);
                    float x = point.x + (float) Math.cos(angle) * distance;
                    float z = point.z + (float) Math.sin(angle) * distance;
                    warpPlayer(player, point.area, x, point.y, z, null, 0, 0, 0);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int placePlayersInGridFormation(location x1)
    {
        obj_id[] targets = getAllPlayers(x1, 100.0f);
        for (obj_id player : targets)
        {
            // Make sure the player is valid
            if (isIdValid(player))
            {
                if (isPlayer(player))
                {
                    //make a 10 x 10 grid and plot each player in a random spot in the grid.
                    float x = rand(x1.x - 5, x1.x + 5);
                    float z = rand(x1.z - 5, x1.z + 5);
                    warpPlayer(player, x1.area, x, x1.y, z, null, 0, 0, 0);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int listCreaturesAlphabetically(obj_id who, float radius, location where) throws InterruptedException
    {
        obj_id[] targets = getCreaturesInRange(where, radius);
        String[] names = new String[targets.length];
        for (int i = 0; i < targets.length; i++)
        {
            names[i] = getName(targets[i]);
        }
        Arrays.sort(names);
        sui.listbox(who, who, "Creatures in range:", sui.OK_ONLY, "Area Tracking", names, "handleTrackingSelection");
        return SCRIPT_CONTINUE;
    }

    public int handleTrackingSelection(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        // Get the list of names but we must use the index because the list isn't baked.
        int index = sui.getListboxSelectedRow(params);
        //String name = sui.getListboxSelectedRowText(params);
        String name = "not_implemented";
        obj_id[] targets = getCreaturesInRange(getLocation(self), 100.0f);
        for (obj_id target : targets)
        {
            if (getName(target).equals(name))
            {
                debugSpeakMsg(self, "I found " + name + " at " + getLocation(target));
            }
        }
        return SCRIPT_CONTINUE;
    }

    public void echoToGroup(obj_id group, String message)
    {
        obj_id[] members = getGroupMemberIds(group);
        for (obj_id member : members)
        {
            if (isIdValid(member))
            {
                sendConsoleMessage(member, message);
            }
        }
    }

    public void disarmGroup(obj_id group) throws InterruptedException
    {
        obj_id[] members = getGroupMemberIds(group);
        for (obj_id member : members)
        {
            obj_id heldWeapon = getCurrentWeapon(member);
            if (isIdValid(heldWeapon))
            {
                putIn(heldWeapon, utils.getInventoryContainer(member));
            }
        }
    }

    public static void disarmPlayer(obj_id player) throws InterruptedException
    {
        obj_id heldWeapon = getCurrentWeapon(player);
        if (isIdValid(heldWeapon))
        {
            putIn(heldWeapon, utils.getInventoryContainer(player));
        }
    }

    public void ringBomb(obj_id player, location where, float radius)
    {
        String clientEffect = "clienteffect/bacta_bomb.cef";
        int numBombs = 10; // Number of bombs to spawn
        for (int i = 0; i < numBombs; i++)
        {
            float angle = rand(0, 360);
            float distance = rand(1, radius); // Ensure bombs are within the specified radius
            float x = where.x + (float) Math.cos(angle) * distance;
            float z = where.z + (float) Math.sin(angle) * distance;
            float y = getHeightAtLocation(x, z);
            location targetPoint = new location(x, y, z);
            playClientEffectLoc(player, clientEffect, targetPoint, 0);
        }
    }

    public void ringClientEffect(obj_id player, location where, String clienteffect, int numEffects, float radius)
    {
        for (int i = 0; i < numEffects; i++)
        {
            float angle = rand(0, 360);
            float x = where.x + (float) Math.cos(angle) * radius;
            float z = where.z + (float) Math.sin(angle) * radius;
            float y = getHeightAtLocation(x, z);
            location targetPoint = new location(x, y, z);
            playClientEffectLoc(player, clienteffect, targetPoint, 1.0f);
        }
    }


    public void stripPlayer(obj_id player) throws InterruptedException
    {
        obj_id[] possessions = utils.getAllItemsInBankAndInventory(player);
        assert possessions != null;
        for (obj_id possession : possessions)
        {
            if (isIdValid(possession))
            {
                destroyObject(possession);
            }
            else
            {
                LOG("ethereal", "stripPlayer() - possession is invalid. ID: " + possession);
            }
        }
    }

    public int downloadCharacterData(obj_id avatar)
    {
        return SCRIPT_CONTINUE;
    }

    public void doClientEffect(obj_id[] players, String clientEffect)
    {
        for (obj_id player : players)
        {
            playClientEffectObj(player, clientEffect, player, "");
        }
    }

    public void doParticleEffect(obj_id[] players, String particleEffect)
    {
        for (obj_id player : players)
        {
            playClientEffectObj(player, particleEffect, player, "");
        }
    }

    public static void broadcastGalaxy(String msg)
    {
        sendSystemMessageGalaxyTestingOnly(msg);
    }

    public int cmdRequestTransferToBindPort(obj_id self, obj_id target, String params, float defaultTime)
    {
        float cooldown = 1200;
        if (defaultTime > 0)
        {
            cooldown = defaultTime;
        }
        location homePort = getHomeBind(self);
        if (homePort == null)
        {
            broadcast(self, "You do not have a home bind point set.");
            return SCRIPT_CONTINUE;
        }
        if (homePort.area.contains("space_"))
        {
            broadcast(self, "That is not a valid location for a home bind point.");
            removeObjVar(self, "home_bind");
            return SCRIPT_CONTINUE;
        }
        else
        {
            if (homePort.cell == null)
            {
                if (getFloatObjVar(self, "home_bind_cooldown") > getGameTime())
                {
                    broadcast(self, "You must wait before using this ability again.");
                    return SCRIPT_CONTINUE;
                }
                else
                {
                    sendPlayerToHomeBind(self);
                    broadcast(self, "You are being returned to your bind location.");
                    LOG("ethereal", "[Home Bind]: " + getPlayerFullName(self) + " has been returned to their bind location.");
                }
            }
            else
            {
                if (getFloatObjVar(self, "home_bind_cooldown") > getGameTime())
                {
                    broadcast(self, "You must wait before using this ability again.");
                    return SCRIPT_CONTINUE;
                }
                else
                {
                    sendPlayerToHomeBind(self);
                    broadcast(self, "You have are being returned to your home residence.");
                    LOG("ethereal", "[Home Bind]: " + getPlayerFullName(self) + " has been returned to their home bind location.");
                }
            }
            setObjVar(self, "home_bind_cooldown", getGameTime() + cooldown);
        }
        return SCRIPT_CONTINUE;
    }
}
