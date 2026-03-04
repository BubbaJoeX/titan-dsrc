package script.player;/*
@Origin: dsrc.script.player.player_nb
@Author: BubbaJoeX
@Purpose: SWG: Titan Player Handler
@Notes:
    This script is the main handler for all player-related events and actions that are custom to SWG: Titan.
    This script is responsible for handling player login, logout, and other player-related events.
    This script is also responsible for handling the player's arrival sound, Christmas present, and other player-related actions.
    This script also logs most triggers to the "ethereal.log" log file.
    There are 5 booleans and 10 integers that are used for various settings and calculations. Please read what they are for.

@Created: Monday, 8/28/2023, at 3:31 PM,
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;
import java.util.StringTokenizer;

import script.combat_engine.hit_result;

import static script.library.factions.setFaction;
import static script.library.utils.getIntScriptVar;

public class player_titan extends base_script
{
    public boolean requireEntBuffRecycle = false;
    public boolean restoredContent = false;
    public boolean LOGGING = true;
    public boolean doArrival = false;
    public boolean showOneTimer = false;
    public boolean craftingRollEnabled = false;
    public boolean craftingTokenEnabled = false;
    public boolean grantLifeDay = false;
    public boolean allowAnniPresent = false;
    public int CRAFTING_TOKEN_LIMIT = 100;
    public int CRAFTING_TOKEN_SF_BONUS = 5;
    public int CRAFTING_HOUSING_DEDUCTION = 5;
    public int CRAFTING_TOKEN_GROUP_BONUS = 8;
    public int CRAFTING_BONUS_MIN_LEVEL = 75;
    public int CRAFTING_FLOOR = 20;
    public int CRAFTING_CEILING = 80;
    public int CRAFTING_DIVISOR = 18;
    public int CRAFTING_MID = 50;

    public String[] IGNORE_VOLUMES = {
            "group_buff_breach",
            "invis_break_far",
            "invis_break__far",
            "invis_break__near",
            "invis_break_near",
            "performance_watch_volume",
            "performance_listen_volume"
    };

    public int OnAttach(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnLogin(obj_id self) throws InterruptedException
    {
        if (doArrival)
        {
            arrivalSound(self);
        }
        location here = getLocation(self);
        if (requireEntBuffRecycle)
        {
            restoreEntertainerBuffs(self);
            return SCRIPT_CONTINUE;
        }
        if (showOneTimer)
        {
            showServerInfo(self);
        }
        if (isGod(self))
        {
            LOG("ethereal", "[Avatar]: Admin " + getPlayerFullName(self) + " has logged in on " + getCurrentSceneName() + " at " + getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime()) + " at " + here.toReadableFormat(true));
        }
        if (isOnLiveCluster() && !getPlayerFullName(self).toLowerCase().contains("bubba"))
        {
            nukeFrog(self);
        }
        if (grantLifeDay)
        {
            if (!hasChristmasPresent(self))
            {
                grantChristmasPresent(self);
            }
        }
        if (allowAnniPresent)
        {
            if (!hasAnniversaryPresent(self))
            {
                LOG("ethereal", "[Anniversary]: " + getPlayerFullName(self) + " does not have their anniversary present. Entering generation.");
                grantAnniversaryPresent(self);
            }
        }
        checkForRaceChange(self);
        clearConditionsFromPlayer(self);
        checkRenownStatus(self);
        return SCRIPT_CONTINUE;
    }

    public void checkRenownStatus(obj_id self) throws InterruptedException
    {
        renown.checkForRankUp(self);
    }

    public int handleRenownTableFunction(obj_id player, dictionary params) throws InterruptedException
    {
        if (params == null)
        {
            return SCRIPT_CONTINUE;
        }
        int buttonPressed = sui.getIntButtonPressed(params);
        if (buttonPressed == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        if (buttonPressed == sui.BP_REVERT)
        {
            renown.showRenownTable(player);
        }
        if (buttonPressed == sui.BP_OK)
        {
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public void clearConditionsFromPlayer(obj_id self)
    {
        if (hasCondition(self, CONDITION_INTERESTING))
        {
            clearCondition(self, CONDITION_INTERESTING);
        }
        if (hasCondition(self, CONDITION_SPACE_INTERESTING))
        {
            clearCondition(self, CONDITION_SPACE_INTERESTING);
        }
        if (hasCondition(self, CONDITION_HOLIDAY_INTERESTING))
        {
            clearCondition(self, CONDITION_HOLIDAY_INTERESTING);
        }
    }

    private void grantAnniversaryPresent(obj_id self) throws InterruptedException
    {
        String GIFT_TEMPLATE = "object/tangible/item/target_dummy_publish_giftbox.iff";
        obj_id gift = createObject(GIFT_TEMPLATE, utils.getInventoryContainer(self), "");
        attachScript(gift, "event.anniversary.gift_object");
        setObjVar(self, "anni_26", true);
        broadcast(self, "You have received a present! Happy Anniversary from the Titan Staff!");
        LOG("ethereal", "[Anniversary]: " + getPlayerFullName(self) + " has received their anniversary gift.");
    }

    private boolean hasAnniversaryPresent(obj_id self)
    {
        return hasObjVar(self, "anni_26");
    }

    public void checkForRaceChange(obj_id self)
    {
        if (hasObjVar(self, "raceUpdated"))
        {
            broadcast(self, "Your race has been updated. Speak with an Image Designer about updating your appearance further.");
            removeObjVar(self, "raceUpdated");
        }
    }

    public void grantChristmasPresent(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, "lifeday.gift_25"))
        {
            obj_id inventory = utils.getInventoryContainer(self);
            obj_id giftbox = createObject("object/tangible/event_perk/life_day_presents.iff", inventory, "");
            attachScript(giftbox, "event.lifeday.gift_25");
            setObjVar(self, "lifeday.gift_25", true);
            sendConsoleMessage(self, "You have received a present! Happy Holidays from the Titan Staff!");
            play2dNonLoopingSound(self, "sound/utinni.snd");
        }
    }

    public void nukeFrog(obj_id self) throws InterruptedException
    {
        //Nukes frog if the server is not named "development" or "swg", preventing the frog from being transported around on live.
        obj_id[] inventory = utils.getContents(self, true);
        for (obj_id frog : inventory)
        {
            if (getTemplateName(frog).contains("terminal_character_builder"))
            {
                destroyObject(frog);
                LOG("ethereal", "[Sanitation | GM Intervention]: " + getPlayerFullName(self) + " has had their frog nuked." + " Deleted ID: " + frog + " at location " + getLocation(self).toReadableFormat(true) + " on cluster " + getClusterName() + ".");
                setObjVar(getPlanetByName("tatooine"), "skynet.nuked_frog." + self, true);
                broadcast(self, "You had an illegal item in your inventory. The item has been removed and this incident has been logged.");
            }
        }
    }

    public int OnLogout(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }


    public void arrivalSound(obj_id self)
    {
        if (hasObjVar(self, "disableArrivalSounds"))
        {
            return;
        }
        if (isGod(self))
        {
            return;
        }
        String planetName = getCurrentSceneName();
        if (planetName.equals("tutorial"))
        {
            broadcast(self, "Welcome to SWG: Titan!");
        }
        else if (planetName.equals("corellia"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_corellia.snd", self, "");
        }
        else if (planetName.equals("dantooine"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_dantooine.snd", self, "");
        }
        else if (planetName.equals("dathomir"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_dathomir.snd", self, "");
        }
        else if (planetName.equals("endor"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_endor.snd", self, "");
        }
        else if (planetName.equals("lok"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_lok.snd", self, "");
        }
        else if (planetName.equals("naboo"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_naboo.snd", self, "");
        }
        else if (planetName.equals("rori"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_rori.snd", self, "");
        }
        else if (planetName.equals("talus"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_talus.snd", self, "");
        }
        else if (planetName.equals("tanaab"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_tanaab.snd", self, "");
        }
        else if (planetName.equals("tatooine"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_tatooine.snd", self, "");
        }
        else if (planetName.equals("yavin4"))
        {
            playClientEffectObj(self, "voice/sound/voice_trnspt_welcome_yavin4.snd", self, "");
        }
        else if (isDevelopmentPlanet(planetName) && isGod(self))
        {
            broadcast(self, "You are on a planet that is not available to the public. Do not bring players here unless it is a staff sponsored event. Welcome to " + planetName + "!");
        }
    }

    public void showServerInfo(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, "titan_welcome_onetimer") && (isInAllowedScene()))
        {
            if (isOnTestCluster())
            {
                debugConsoleMsg(self, "You are currently on a test server. The server welcome message box may show inaccurate details.");
            }

            String welcomeName = "\\#.Welcome,  " + getPlayerFullName(self) + "!\n";
            String pleaseRead = "Please be sure to read the " + color("Rules & Policies", "D2B48C") + " and " + color("F.A.Q.", "D2B48C") + " in our Discord before starting your adventures.\n";
            String numCharacters = "# of Allowed Characters: " + color("5", "FFD700") + "\n";
            String maxLogin = "# of Allowed Characters Online: " + color("3", "FFD700") + "\n";
            String unlocks = "# of Unlockable Character Slots: " + color("5", "FFD700") + "\n";
            String nl = "\n\\#.";
            String title = "Titan Account and Character Notice:" + "\\#.";
            final String welcome = title + nl + nl + pleaseRead + numCharacters + maxLogin + unlocks + nl;

            int page = createServerWelcomePage(self, welcome, welcomeName);
            showSUIPage(page);
            flushSUIPage(page);
            setObjVar(self, "titan_welcome_onetimer", 1);
        }
    }

    public int createServerWelcomePage(obj_id self, String welcome, String title) throws InterruptedException
    {
        String buttonLabel = "Begin!";
        int page = sui.createSUIPage("/Script.welcomeMessage", self, self);
        if (page == 0)
        {
            LOG("ethereal", "[NPE2]: Unable to create welcome message page for " + getPlayerFullName(self) + ".");
            return SCRIPT_CONTINUE;
        }
        utils.removeScriptVar(self, "welcomePage");
        utils.setScriptVar(self, "welcomePage", page);
        setSUIProperty(page, "flow.titleLabel", "LocalText", title);
        setSUIProperty(page, "flow.titleMessage", "LocalText", welcome);
        setSUIProperty(page, "flow.titlePlay", "LocalText", buttonLabel);
        setSUIProperty(page, "exit", "isDefaultButton", "true");
        //setSUIProperty(page, "exit", "OnPress", "Parent.isVisible=false");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "exit", "handleWelcomePage");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "exit", "", "OnPress");
        String strippedText = stripColorCodes(welcome);
        saveTextOnClient(self, getClusterName().toLowerCase() + "_server_welcome.txt", strippedText);
        flushSUIPage(page);
        showSUIPage(page);
        LOG("ethereal", "[NPE2]: Welcome message page for " + getName(self) + " has been shown and information saved to their client folder.");
        return page;
    }

    public int handleWelcomePage(obj_id self, dictionary params) throws InterruptedException
    {
        int page = utils.getIntScriptVar(self, "welcomePage");
        //log params to console
        for (Object key : params.keySet())
        {
            LOG("ethereal", "[NPE2]: " + key + ": " + params.get(key));
            debugConsoleMsg(self, key + ": " + params.get(key));
        }
        playClientEffectObj(self, "clienteffect/level_granted.cef", self, "root");
        forceCloseSUIPage(page);
        return SCRIPT_CONTINUE;
    }

    private boolean isInAllowedScene()
    {
        String currentScene = getCurrentSceneName();
        return currentScene.equals("taanab") || currentScene.equals("tatooine");
    }

    private boolean isOnTestCluster()
    {
        String clusterName = getClusterName();
        return clusterName.equalsIgnoreCase("swg") || clusterName.equalsIgnoreCase("development");
    }

    private boolean isDevelopmentPlanet(String scene)
    {
        return scene.equals("dungeon_hub") || scene.equals("adventure3") || scene.equals("simple");
    }

    private boolean isOnLiveCluster()
    {
        String clusterName = getClusterName();
        return clusterName.equals("Titan");
    }

    private String color(String text, String colorCode)
    {
        return " \\#" + colorCode + text + "\\#.";
    }

    private String stripColorCodes(String text)
    {
        return text.replaceAll("#[0-9A-Fa-f]{6}", "");
    }

    public void restoreEntertainerBuffs(obj_id self) throws InterruptedException
    {
        utils.removeScriptVar(self, "performance.buildabuff");
        utils.setScriptVar(self, "performance.buildabuff.buffComponentKeys", getStringArrayObjVar(self, "saved_performance.buildabuff.buffComponentKeys"));
        utils.setScriptVar(self, "performance.buildabuff.buffComponentValues", getIntArrayObjVar(self, "saved_performance.buildabuff.buffComponentValues"));
        utils.setScriptVar(self, "performance.buildabuff.bufferId", getObjIdObjVar(self, "saved_performance.buildabuff.bufferId"));
        float buffTime = getFloatObjVar(self, "saved_performance.buildabuff.buffTime");
        buff.applyBuff(self, "buildabuff_inspiration", buffTime);
        if (requireEntBuffRecycle)
        {
            removeObjVar(self, "saved_performance");
            //debugConsoleMsg(self, "Your entertainment buff package has been restored.");
        }
        else
        {
            //debugConsoleMsg(self, "\\#DAA520Your entertainment buff package has been restored. You will need to seek out an entertainer to change your buff package values.\\#.");
        }
    }

    public void incrementPlayerCount(obj_id self)
    {
        int count = 1;
        obj_id tatooine = getPlanetByName("tatooine");
        if (!hasObjVar(tatooine, "avatarCount"))
        {
            setObjVar(tatooine, "avatarCount", count);
        }
        else
        {
            int playerCount = getIntObjVar(tatooine, "avatarCount");
            playerCount++;
            setObjVar(tatooine, "avatarCount", playerCount);
        }
        LOG("ethereal", "[Player Count]: " + "Player " + getPlayerFullName(self) + " has zoned in to " + getCurrentSceneName() + ". Current zone count is " + getIntObjVar(tatooine, "avatarCount") + " on cluster " + getClusterName() + ".");
    }

    public void decrementPlayerCount(obj_id self)
    {
        int count = 1;
        obj_id tatooine = getPlanetByName("tatooine");
        if (!hasObjVar(tatooine, "avatarCount"))
        {
            setObjVar(tatooine, "avatarCount", count);
        }
        else
        {
            int playerCount = getIntObjVar(tatooine, "avatarCount");
            --playerCount;
            setObjVar(tatooine, "avatarCount", playerCount);
        }
        LOG("ethereal", "[Player Count]: " + "Player " + getPlayerFullName(self) + " has zoned out to " + getCurrentSceneName() + ". Current zone count is " + getIntObjVar(tatooine, "avatarCount") + " on cluster " + getClusterName() + ".");
    }

    // @NOTE: [used with /showContent]
    public int cmdContentFinder(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        String arg = params.trim();
        if (arg.equalsIgnoreCase("help") || arg.equalsIgnoreCase("?") || arg.equalsIgnoreCase(""))
        {
            broadcast(self, "Usage: /showContent status\tShows the current status of world bosses and dungeons.");
            broadcast(self, "Usage: /showContent self\tShows the current status of you.");
            return SCRIPT_CONTINUE;
        }
        if (arg.equalsIgnoreCase("status"))
        {
            listAllContentStatuses(self);
        }
        if (arg.equalsIgnoreCase("self"))
        {
            //@TODO: implement world boss tracker sui
            //listAllContentForSelf(self);
        }
        return SCRIPT_CONTINUE;
    }

    public void listAllContentStatuses(obj_id self) throws InterruptedException
    {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("\\#FFFFFF=== CONTENT STATUS ===\\#.\n\n");

        // World Bosses Section
        sb.append("\\#00BFFF[ World Bosses ]\\#.\n");
        appendBossStatus(sb, "Elder Ancient Krayt Dragon", "world_boss.krayt");
        appendBossStatus(sb, "Mutated Peko-Peko Empress", "world_boss.peko");
        appendBossStatus(sb, "Darth Rolii", "world_boss.gizmo");
        appendBossStatus(sb, "The Crusader", "world_boss.pax");
        appendBossStatus(sb, "Donk-Donk Binks", "world_boss.donkdonk_binks");
        appendBossStatus(sb, "IG-24", "world_boss.ig24");
        appendBossStatus(sb, "Dhanak the Kessel Runner", "world_boss.dhanak");
        sb.append("\n");

        // Dungeons Section
        sb.append("\\#00BFFF[ Dungeons ]\\#.\n");
        appendDungeonWithCount(sb, "Geonosian Bio-lab", "geo_count");
        appendDungeonWithCount(sb, "Death Watch Bunker", "dwb_count");
        appendBossStatus(sb, "  \\#AAAAAA>\\#. Death Watch Overlord", "dungeon.death_watch_bunker.overlord");
        sb.append("\n");

        // Open World Section
        sb.append("\\#00BFFF[ Open World ]\\#.\n");
        appendBossStatus(sb, "Emperor's Hand", "open_world.hand");
        if (restoredContent)
        {
            appendBossStatus(sb, "Aurra Sing", "world_boss.aurra_sing");
        }
        sb.append("\n");

        // Dynamic Dungeons Section (if enabled)
        if (restoredContent)
        {
            sb.append("\\#00BFFF[ Dynamic Dungeons ]\\#.\n");
            appendBossStatus(sb, "Czerka Hideout", "dynamic_dungeon.czerka");
            appendBossStatus(sb, "Mos Eisley Caverns", "dynamic_dungeon.caverns");
            sb.append("\n");
        }

        // Footer
        sb.append("\\#888888Last Updated: ").append(getCalendarTimeStringLocal_YYYYMMDDHHMMSS(getCalendarTime())).append("\\#.");

        String finalPrompt = sb.toString();
        int page = sui.msgbox(self, self, finalPrompt);
        setSUIProperty(page, sui.MSGBOX_PROMPT, "Text", finalPrompt);
        setSUIProperty(page, sui.MSGBOX_PROMPT, "Font", "bold_22");
        setSUIProperty(page, sui.MSGBOX_TITLE, "Text", "Content Status: " + getClusterName());
        setSUIProperty(page, sui.MSGBOX_PROMPT, "Editable", "false");
        setSUIProperty(page, sui.MSGBOX_PROMPT, "GetsInput", "false");
        subscribeToSUIEvent(page, sui_event_type.SET_onButton, "%btnOk%", "noHandler");
        setSUIProperty(page, "btnCancel", "Visible", "false");
        setSUIProperty(page, "btnRevert", "Visible", "false");
        setSUIProperty(page, "btnOk", "Visible", "false");
        showSUIPage(page);
    }

    private void appendBossStatus(StringBuilder sb, String bossName, String statusKey)
    {
        sb.append("  ").append(bossName).append(": ").append(getDungeonStatus(statusKey)).append("\n");
    }

    private void appendDungeonWithCount(StringBuilder sb, String dungeonName, String countKey)
    {
        obj_id tatooine = getPlanetByName("tatooine");
        Object playerCount = getObjVar(tatooine, "dungeon_finder." + countKey);
        String countStr = (playerCount != null) ? playerCount.toString() : "0";
        sb.append("  ").append(dungeonName).append(" \\#888888(Players: ").append(countStr).append(")\\#.\n");
    }

    public String getDungeonStatus(String dungeonName)
    {
        obj_id tatooine = getPlanetByName("tatooine");
        String dungeonStatus = getStringObjVar(tatooine, "dungeon_finder." + dungeonName);
        if (dungeonStatus == null || dungeonStatus.isEmpty())
        {
            dungeonStatus = "\\#FFFF00Unknown\\#.";
        }
        if (dungeonStatus.equals("Inactive"))
        {
            dungeonStatus = "\\#F32B2BInactive\\#.";
        }
        else if (dungeonStatus.equals("Active"))
        {
            dungeonStatus = "\\#7CFC00Active\\#.";
        }
        else if (dungeonStatus.equals("Contested"))
        {
            dungeonStatus = "\\#FF8C00Contested\\#.";
        }
        else if (dungeonStatus.equals("Engaged"))
        {
            dungeonStatus = "\\#EDBB17Engaged\\#.";
        }
        return dungeonStatus;
    }

    public int OnCollectionSlotModified(obj_id self, String bookName, String pageName, String collectionName, String slotName, boolean isCounterTypeSlot, int previousValue, int currentValue, int maxSlotValue, boolean slotCompleted)
    {
        String monkeyCollection = "halloween_24";
        if (collectionName.equals(monkeyCollection))
        {
            //handle pumpkin collection
        }
        return SCRIPT_CONTINUE;
    }

    public int OnCraftedPrototype(obj_id self, obj_id prototypeObject, draft_schematic manufacturingSchematic) throws InterruptedException
    {
        //fillCandyBowl(self);
        repeatables.updateDailyCraft(self);
        if (isGod(self))
        {
            LOG("ethereal", "[Crafting]: " + getPlayerFullName(self) + " has crafted " + getName(prototypeObject) + " while in GodMode from source schematic " + manufacturingSchematic + " " + getNameFromTemplate(manufacturingSchematic.getObjectTemplateCreated()));
            return SCRIPT_CONTINUE;
        }
        else
        {
            LOG("ethereal", "[Crafting]: " + getPlayerFullName(self) + " has crafted " + getName(prototypeObject) + " | Template: " + getTemplateName(prototypeObject));
        }
        if (craftingRollEnabled)
        {
            String message = "\\#FFBF00You have received a bonus crafting buff!";
            int profession = getProfession(self);
            int roll = rand(1, 100);
            if (roll < 76 && getLevel(self) > CRAFTING_BONUS_MIN_LEVEL)
            {
                switch (profession)
                {
                    case 0:
                        broadcast(self, message);
                        awardCraftingBonus(self, 0);
                        break;
                    case 1:
                        broadcast(self, message);
                        awardCraftingBonus(self, 1);
                        break;
                    case 2:
                        broadcast(self, message);
                        awardCraftingBonus(self, 2);
                        break;
                    case 3:
                        broadcast(self, message);
                        awardCraftingBonus(self, 3);
                        break;
                    default:
                        break;
                }
            }
            else
            {
                return SCRIPT_CONTINUE;
            }
        }
        if (craftingTokenEnabled)
        {
            int finalBonus = getTokenCalculations(self);
            //ok now round up (#3 or #4 etc  -> #5)
            double preparedBonus = roundBonus(finalBonus);
            int rollChance = rand(0, 100);// random number 0-100
            if (rollChance >= CRAFTING_FLOOR && rollChance <= CRAFTING_CEILING) // if number is between 25 and 75 continue,
            {
                int subRoll = rand(1, 3);
                if (subRoll == 1 || subRoll == 3)// if odd roll (1-3) 75% chance
                {
                    grantCraftingToken(self, preparedBonus * 2);
                }
                else // if even roll, award base (aww darn roll)
                {
                    grantCraftingToken(self, preparedBonus);
                }
            }
            else
            {
                LOG("ethereal", "[Trader System]: " + getPlayerFullName(self) + " has failed initial crafting token roll.");
            }
        }
        checkForGCWParticipation(self, prototypeObject);
        return SCRIPT_CONTINUE;
    }

    private void fillCandyBowl(obj_id self) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(self);
        LOG("ethereal", "[Halloween]: " + getPlayerFullName(self) + " has crafted an item. Checking for candy bowl.");
        obj_id[] contents = getContents(pInv);
        LOG("ethereal", "[Halloween]: " + getPlayerFullName(self) + " has " + contents.length + " items in their inventory.");
        for (obj_id content : contents)
        {
            if (hasObjVar(content, "halloween_24_tag"))
            {
                LOG("ethereal", "[Halloween]: " + getPlayerFullName(self) + " has a treat bowl in their inventory.");
                float fillLevel = getFloatObjVar(content, "halloween_24");
                if (fillLevel >= 100)
                {
                    broadcast(pInv, "Your treat bowl is already full. Place this object in your house or bank until October 31st to hide this message.");
                    LOG("ethereal", "[Halloween]: " + getPlayerFullName(self) + " has a full treat bowl. Returning.");
                    return;
                }
                float newFillLevel = fillLevel + 0.05f;
                LOG("ethereal", "[Halloween]: " + getPlayerFullName(self) + " has added candy to their treat bowl. It is now at " + newFillLevel + "% capacity.");
                setObjVar(content, "halloween_24", newFillLevel);
                broadcast(self, "You have added candy to your treat bowl. It is now at " + newFillLevel + "% capacity.");
                playClientEffectObj(self, "clienteffect/item_bugs_bats.cef", self, "head");
                LOG("ethereal", "[Halloween]: " + getPlayerFullName(self) + " - end of candy bowl check.");
            }
        }
    }

    //@NOTE: This function checks for GCW participation and awards points if the player is in the correct region from crafting invasion tools
    private void checkForGCWParticipation(obj_id self, obj_id prototypeObject) throws InterruptedException
    {
        String template = getTemplateName(prototypeObject);
        if (template.contains("gcw_"))
        {
            String[] REGION_NAMES = {
                    "@tatooine_region_names:bestine",
                    "@talus_region_names:dearic",
                    "@naboo_region_names:keren"
            };
            location here = getLocation(self);
            if (isInWorldCell(self))
            {
                for (String region : REGION_NAMES)
                {
                    if (here.area.equals(region))
                    {
                        if (factions.isImperialorImperialHelper(self) || factions.isRebelorRebelHelper(self))
                        {
                            obj_id gcw_city = getGcwCityObject(self);
                            if (!isIdValid(gcw_city))
                            {
                                return;
                            }
                            if (gcw_city != null && getIntScriptVar(gcw_city, "gcw.invasionPhase") != 1)
                            {
                                return;
                            }
                            //@Note: bonuses will apply
                            int points = rand(50, 75);
                            int roundedNumber = (int) Math.ceil((double) points / 5) * 5;
                            gcw.grantUnmodifiedGcwPoints(self, roundedNumber);
                            LOG("gcw", "[GCW]: " + getPlayerFullName(self) + " has been awarded GCW points for crafting the invasion tools in " + region + " at " + here.toReadableFormat(true));
                            return;
                        }
                        else
                        {
                            LOG("gcw", "[GCW]: " + getPlayerFullName(self) + " has failed to receive GCW points for crafting the invasion tools due to alignment in " + region + " at " + here.toReadableFormat(true));
                            break;
                        }
                    }
                }
            }
        }
    }

    private obj_id getGcwCityObject(obj_id self)
    {
        //@Note: this should really be 1 object per planet unless i am understanding this wrong
        obj_id[] searchList = getAllObjectsWithScript(getLocation(self), 1000, "systems.gcw.gcw_city");
        if (searchList != null && searchList.length > 0)
        {
            return searchList[0];
        }
        return null;
    }

    public double roundBonus(int i)
    {
        LOG("ethereal", "[Trader Token]: " + "Rounding bonus of " + i);
        return 5 * ((double) Math.abs(i / 5));
    }

    public int getTokenCalculations(obj_id self) throws InterruptedException
    {
        int tokenBonus = Math.max(1, Math.min(getLevel(self) / CRAFTING_DIVISOR, 25));
        LOG("ethereal", "[Trader Token]: Base token bonus calculated: " + tokenBonus);

        // Bonus for being in a group in an outdoor location
        if (group.isGrouped(self) && isInWorldCell(self))
        {
            int groupSize = group.getGroupMemberIds(getGroupObject(self)).length;
            tokenBonus += groupSize;
            LOG("ethereal", "[Trader Token]: Group Trader Token Bonus: " + groupSize);
        }

        // Bonus for being declared in Special Forces in an outdoor location
        if (factions.isDeclared(self) && isInWorldCell(self))
        {
            tokenBonus += CRAFTING_TOKEN_SF_BONUS;
            LOG("ethereal", "[Trader Token]: Special Forces Trader Token Bonus: " + CRAFTING_TOKEN_SF_BONUS);
        }

        // Deduction if in a private house
        if (!isInWorldCell(self))
        {
            tokenBonus -= CRAFTING_HOUSING_DEDUCTION;
            LOG("ethereal", "[Trader Token]: Private Housing Check Deduction: " + CRAFTING_HOUSING_DEDUCTION);
        }

        // Apply a "pity check" if the token bonus is very low
        if (tokenBonus <= 5)
        {
            int pityMultiplier = rand(1, 3);
            tokenBonus *= pityMultiplier;
            LOG("ethereal", "[Trader Token]: Pity Check Applied, Multiplier: " + pityMultiplier + ", Token Bonus: " + tokenBonus);
        }

        // Final clamping of token bonus within defined limits
        tokenBonus = Math.max(5, Math.min(tokenBonus, CRAFTING_TOKEN_LIMIT));
        LOG("ethereal", "[Trader Token]: Final Calculated Token Bonus: " + tokenBonus);

        return tokenBonus;
    }

    public void grantCraftingToken(obj_id self, double tokenAmount) throws InterruptedException
    {
        //sanity check just in case.
        if (tokenAmount >= 100)
        {
            tokenAmount = 100;
        }
        if (isGod(self))
        {
            broadcast(self, "You would have received " + tokenAmount + " Trader Tokens from crafting this prototype but you are in god and that is admin abuse my guy.");
            LOG("ethereal", "[Trader System]: " + getPlayerFullName(self) + " was in god mode and is not eligible for trader tokens.");
        }
        else
        {
            obj_id inventory = utils.getInventoryContainer(self);
            obj_id tokens = static_item.createNewItemFunction("item_trader_token", inventory, (int) tokenAmount);
            showLootBox(self, new obj_id[]{tokens});
            LOG("ethereal", "[Trader System]: " + getPlayerFullName(self) + " has received " + tokenAmount + " trader tokens from crafting their most recent prototype.");
        }
        LOG("ethereal", "[Trader System]: Completed all transactions for " + getPlayerFullName(self));
    }

    public void awardCraftingBonus(obj_id self, int i) throws InterruptedException
    {
        switch (i)
        {
            case 0:
                buff.applyBuff(self, "architect_inspiration", 1800);
                buff.applyBuff(self, "shipwright_inspiration", 1800);
                buff.applyBuff(self, "merchant_inspiration", 1800);
                break;
            case 1:
                buff.applyBuff(self, "chef_inspiration", 1800);
                buff.applyBuff(self, "tailor_inspiration", 1800);
                buff.applyBuff(self, "merchant_inspiration", 1800);
                break;
            case 2:
                buff.applyBuff(self, "droidengineer_inspiration", 1800);
                buff.applyBuff(self, "merchant_inspiration", 1800);
                break;
            case 3:
                buff.applyBuff(self, "weaponsmith_inspiration", 1800);
                buff.applyBuff(self, "armorsmith_inspiration", 1800);
                break;
            default:
                buff.applyBuff(self, "artisan_inspiration", 1800);
                buff.applyBuff(self, "musician_inspiration", 1800);
                buff.applyBuff(self, "entertainer_inspiration", 1800);
                buff.applyBuff(self, "imagedesigner_inspiration", 1800);
                break;
        }
        LOG("ethereal", "[Crafting]: " + "Player " + getPlayerFullName(self) + " has received a crafting bonus buff. [Group: " + getProfession(self) + "  |  " + i + "]");
    }

    public int getProfession(obj_id self) //you dumby this is defined in utils
    {
        if (hasSkill(self, "class_structures_phase1_novice"))
        {
            return 0;
        }
        else if (hasSkill(self, "class_domestics_phase1_novice"))
        {
            return 1;
        }
        else if (hasSkill(self, "class_munitions_phase1_novice"))
        {
            return 2;
        }
        else if (hasSkill(self, "class_engineering_phase1_novice"))
        {
            return 3;
        }
        else return 4;
    }

    public int OnAddedToWorld(obj_id self) throws InterruptedException
    {
        LOG("ethereal", "[World]: " + "Player " + getPlayerFullName(self) + " has been added to zone " + getCurrentSceneName());
        return SCRIPT_CONTINUE;
    }

    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        LOG("ethereal", "[Spatial Chat]: " + "Player " + getFirstName(self) + " said: " + text);
        return SCRIPT_CONTINUE;
    }

    public int OnDuelRequest(obj_id self, obj_id actor, obj_id target)
    {
        LOG("ethereal", "[Dueling]: " + "Player has initiated a duel request with " + getPlayerFullName(target));
        return SCRIPT_CONTINUE;
    }

    public int OnDuelStart(obj_id self, obj_id actor, obj_id target)
    {
        LOG("ethereal", "[Dueling]: " + "Player has started a duel with " + getPlayerFullName(target) + " (Actor: " + actor + ")");
        return SCRIPT_CONTINUE;
    }

    public int OnUnsticking(obj_id self)
    {
        LOG("ethereal", "[Unstick]: " + "Player " + getPlayerFullName(self) + " has initiated an unstick request at " + getLocation(self).toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int OnEnvironmentalDeath(obj_id self) throws InterruptedException
    {
        LOG("ethereal", "[Environment]: " + "Player " + getPlayerFullName(self) + " has been fried by the environment at location " + getLocation(self).toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int OnDeath(obj_id self, obj_id killer, obj_id corpseId) throws InterruptedException
    {
        LOG("ethereal", "[Death]: " + "Player " + getPlayerFullName(self) + " has died at [" + getLocation(self).toReadableFormat(true) + "] from " + (isMob(killer) ? getCreatureName(killer) : getPlayerFullName(killer)));
        return SCRIPT_CONTINUE;
    }

    public int OnCityChanged(obj_id self, int oldCityId, int newCityId) throws InterruptedException
    {
        String oldCityName = cityGetName(oldCityId);
        String newCityName = cityGetName(newCityId);
        if (oldCityName == null || oldCityName.isEmpty())
        {
            LOG("ethereal", "[City]: " + "Player " + getPlayerFullName(self) + " has entered " + newCityName);
        }
        else
        {
            LOG("ethereal", "[City]: " + "Player " + getPlayerFullName(self) + " has left " + oldCityName);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnEnterSwimming(obj_id self)
    {
        LOG("ethereal", "[Swimming]: " + "Player " + getPlayerFullName(self) + " has entered the water at location " + getLocation(self).toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int OnEnterRegion(obj_id self, String planet, String name) throws InterruptedException
    {
        if (name.equals("refugee_camp"))
        {
            if (hasObjVar(self, "npe2_camp"))
            {
                return SCRIPT_CONTINUE;
            }
            else return SCRIPT_OVERRIDE;
        }
        return SCRIPT_CONTINUE;
    }

    public int OnExitRegion(obj_id self, String planet, String name) throws InterruptedException
    {
        if (name.equals("refugee_camp"))
        {
            if (!hasObjVar(self, "npe2_camp"))
            {
                return SCRIPT_OVERRIDE;//no
            }
            else return SCRIPT_CONTINUE;//meh
        }
        return SCRIPT_CONTINUE;//ok
    }

    public int OnExitSwimming(obj_id self)
    {
        LOG("ethereal", "[Swimming]: " + "Player " + getPlayerFullName(self) + " has exited the water at location " + getLocation(self).toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int OnCollectionServerFirst(obj_id self, String bookName, String pageName, String collectionName)
    {
        LOG("ethereal", "[Collections]: " + "Player " + getPlayerFullName(self) + " has earned Server First for collection " + bookName + "/" + pageName + "/" + collectionName);
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id attacker) throws InterruptedException
    {
        LOG("ethereal", "[Incapacitation]: " + "Player " + getPlayerFullName(self) + " has been incapacitated by " + attacker + " at location " + getLocation(self).toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int OnGiveItem(obj_id self, obj_id item, obj_id player) throws InterruptedException
    {
        LOG("ethereal", "[Interaction]: Player " + getPlayerFullName(self) + " is attempting to move " + getName(item) + " to " + getPlayerFullName(player));
        return SCRIPT_CONTINUE;
    }

    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws InterruptedException
    {
        if (isGod(self) && (isPlayer(speaker)))
        {
            LOG("ethereal", "[Spatial Chat]: " + "Player " + getFirstName(self) + " overheard '" + text + "' from " + getFirstName(speaker));
        }
        return SCRIPT_CONTINUE;
    }

    //Note: Inventory transfer logging
    public int OnReceivedItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (srcContainer == utils.getInventoryContainer(self))
        {
            LOG("ethereal", "[Inventory]: Player " + getFirstName(self) + " has received " + getNameNoSpam(item) + " (" + item + ") from " + getName(transferer) + " " + (transferer) + " from container " + getName(srcContainer) + " (" + srcContainer + ")");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnLostItem(obj_id self, obj_id destContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (destContainer == utils.getInventoryContainer(self))
        {
            LOG("ethereal", "[Inventory]: Player " + getFirstName(self) + " has lost " + getNameNoSpam(item) + " (" + item + ") from " + getName(transferer) + " " + (transferer) + " to container " + getName(destContainer) + " (" + destContainer + ")");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnEnteredCombat(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, "combat_debug"))
        {
            LOG("ethereal", "[Combat]: " + "Player " + getPlayerFullName(self) + " has entered combat at location " + getLocation(self).toReadableFormat(true));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnExitedCombat(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, "combat_debug"))
        {
            LOG("ethereal", "[Combat]: " + "Player " + getPlayerFullName(self) + " has exited combat at location " + getLocation(self).toReadableFormat(true));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnPreloadComplete(obj_id self) throws InterruptedException
    {
        LOG("ethereal", "[Preload]: " + "Player " + getPlayerFullName(self) + " has completed preloading.");
        return SCRIPT_CONTINUE;
    }

    public int OnPlaceStructure(obj_id self, obj_id player, obj_id deed, location position, int rotation)
    {
        String[] directions = {"North", "East", "South", "West"};
        String direction = (rotation >= 0 && rotation <= 3) ? directions[rotation] : "Unknown";

        LOG("ethereal", "[Structure]: Player " + getPlayerFullName(player) +
                " has placed a structure " + getName(deed) +
                " at location " + position.toReadableFormat(true) +
                " facing " + direction + ".");

        return SCRIPT_CONTINUE;
    }

    public int OnPvpFactionChanged(obj_id self, int oldFaction, int newFaction)
    {
        LOG("ethereal", "[PvP]: " + "Player " + getPlayerFullName(self) + " has changed PvP faction from " + oldFaction + " to " + newFaction);
        return SCRIPT_CONTINUE;
    }

    public int OnPurchaseTicketInstantTravel(obj_id self, obj_id player, String departPlanetName, String departTravelPointName, String arrivePlanetName, String arriveTravelPointName, boolean roundTrip)
    {
        LOG("ethereal", "[Travel]: " + "Player " + getPlayerFullName(self) + " has purchased an instant travel ticket to " + arrivePlanetName + " from " + departPlanetName + " with a round trip of " + roundTrip);
        return SCRIPT_CONTINUE;
    }

    public int OnPurchaseTicket(obj_id self, obj_id player, String departPlanetName, String departTravelPointName, String arrivePlanetName, String arriveTravelPointName, boolean roundTrip)
    {
        LOG("ethereal", "[Travel]: " + "Player " + getPlayerFullName(self) + " has purchased a travel ticket to " + arriveTravelPointName + ", " + arrivePlanetName + " from " + departTravelPointName + ", " + departPlanetName + (roundTrip ? " with a round trip." : " without a round trip."));
        return SCRIPT_CONTINUE;
    }

    public int OnSkillModsChanged(obj_id self, String[] modNames, int[] modValues) throws InterruptedException
    {
        mapped_strings skillMods = new mapped_strings();
        ;
        for (int i = 0; i < modNames.length - 1; i++)
        {
            String element = modNames[i];
            int value = modValues[i];
            LOG("ethereal", "[Skill Mods]: Player " + getFirstName(self) + " has had their " + element + " skill mod changed to " + value);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnGrantedSchematic(int ident, boolean status)
    {
        obj_id self = getSelf();
        LOG("ethereal", "[Schematics]: " + "Player " + getPlayerFullName(self) + " has been granted schematic " + ident + " with a status of " + status);
        return SCRIPT_CONTINUE;
    }

    public int OnRevokedSchematic(int ident, boolean status)
    {
        obj_id self = getSelf();
        LOG("ethereal", "[Schematics]: " + "Player " + getPlayerFullName(self) + " has been granted schematic " + ident + " with a status of " + status);
        return SCRIPT_CONTINUE;
    }

    public int OnGroupFormed(obj_id self)
    {
        LOG("ethereal", "[Group]: " + "Player " + getPlayerFullName(self) + " has formed a group.");
        return SCRIPT_CONTINUE;
    }

    public int OnAddedToGroup(obj_id self) throws InterruptedException
    {
        obj_id groupie = getGroupObject(self);
        LOG("ethereal", "[Group]: " + "Player " + getPlayerFullName(self) + " has been added to group " + groupie + " led by " + group.getLeader(groupie));
        return SCRIPT_CONTINUE;
    }

    public int OnGroupDisbanded(obj_id self)
    {
        LOG("ethereal", "[Group]: " + "Player " + getPlayerFullName(self) + " has disbanded their group.");
        return SCRIPT_CONTINUE;
    }

    public int OnRemovedFromGroup(obj_id self) throws InterruptedException
    {
        obj_id groupie = getGroupObject(self);
        LOG("ethereal", "[Group]: " + "Player " + getPlayerFullName(self) + " has been removed from their group led by " + group.getLeader(groupie));
        return SCRIPT_CONTINUE;
    }

    public int OnGroupLeaderChanged(obj_id self, obj_id groupId, obj_id newLeader, obj_id oldLeader) throws InterruptedException
    {
        if (oldLeader == null)
        {
            LOG("ethereal", "[Group]: " + "Player " + getPlayerFullName(self) + " was made group leader for group " + groupId);
        }
        else
        {
            LOG("ethereal", "[Group]: " + "Player " + getPlayerFullName(self) + " has changed group leaders in group " + groupId + " from " + getPlayerFullName(oldLeader) + " to " + getPlayerFullName(newLeader));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnChatLogin(obj_id self)
    {
        LOG("ethereal", "[Chat]: " + "Player " + getPlayerFullName(self) + " has logged into chat.");
        return SCRIPT_CONTINUE;
    }

    public int OnApplyPowerup(obj_id self, obj_id player, obj_id target)
    {
        LOG("ethereal", "[Powerup]: " + "Player " + getPlayerFullName(self) + " has applied a power-up to " + target);
        return SCRIPT_CONTINUE;
    }

    public int OnSawAttack(obj_id self, obj_id defender, obj_id[] attackers) throws InterruptedException
    {
        if (isGod(self) && (isPlayer(defender)))
        {
            LOG("ethereal", "[Combat]: " + "Player " + getFirstName(self) + " saw " + getFirstName(defender) + " attacked by " + attackers.length + " attackers.");
        }
        return SCRIPT_CONTINUE;
    }

    public int OnSetGodModeOn(obj_id self) throws InterruptedException
    {
        LOG("ethereal", "[God Mode]: " + "Player " + getPlayerFullName(self) + " has enabled God Mode.");
        refreshShipCellPermissions(self);
        return SCRIPT_CONTINUE;
    }

    public int OnSetGodModeOff(obj_id self) throws InterruptedException
    {
        LOG("ethereal", "[God Mode]: " + "Player " + getPlayerFullName(self) + " has disabled God Mode.");
        refreshShipCellPermissions(self);
        return SCRIPT_CONTINUE;
    }

    private void refreshShipCellPermissions(obj_id player) throws InterruptedException
    {
        obj_id ship = space_transition.getContainingShip(player);
        if (!isIdValid(ship))
            return;
        String[] cellNames = getCellNames(ship);
        if (cellNames == null)
            return;
        for (String cellName : cellNames)
        {
            obj_id cellId = getCellId(ship, cellName);
            if (isIdValid(cellId))
            {
                permissionsMakePublic(cellId);
                sendDirtyCellPermissionsUpdate(cellId, player, true);
            }
        }
    }

    public int OnChangedAppearance(obj_id self, obj_id player)
    {
        LOG("ethereal", "[Appearance]: " + "Player " + getPlayerFullName(self) + " has changed their appearance.");
        return SCRIPT_CONTINUE;
    }

    public int OnRevertedAppearance(obj_id self, obj_id player)
    {
        LOG("ethereal", "[Appearance]: " + "Player " + getPlayerFullName(self) + " has changed their appearance.");
        return SCRIPT_CONTINUE;
    }

    public int OnAbandonPlayerQuest(obj_id self)
    {
        LOG("ethereal", "[Quest]: " + "Player " + getPlayerFullName(self) + " has abandoned a chronicler quest.");
        return SCRIPT_CONTINUE;
    }

    public int OnRatingFinished(obj_id self, int rating)
    {
        LOG("ethereal", "[Quest]: " + "Player " + getPlayerFullName(self) + " has rated a chronicler quest with a rating of " + rating);
        return SCRIPT_CONTINUE;
    }

    public int OnSawEmote(obj_id self, obj_id performer, String emote) throws InterruptedException
    {
        if (isGod(self) && (isPlayer(performer)))
        {
            LOG("ethereal", "[Emotes]: " + "Player " + getFirstName(self) + " saw " + getFirstName(performer) + " perform " + emote);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnPerformEmote(obj_id self, String emote) throws InterruptedException
    {
        obj_id defender = getTarget(self);
        if (defender == null)
        {
            return SCRIPT_CONTINUE;
        }
        if (defender == self)
        {
            return SCRIPT_CONTINUE;
        }
        if (combat.isInCombat(self))
        {
            if (emote.equals("slap"))
            {
                hit_result d = new hit_result();
                d.damage = rand(300, 700);
                d.strikethrough = true;
                d.strikethroughAmmount = 1.0f;
                d.success = true;
                doDamage(self, self, d);
                knockdownPlayer(defender);
                broadcast(self, "You have slapped " + getPlayerFullName(defender));
                broadcast(defender, getPlayerFullName(self) + " has slapped you!");
            }
        }
        return SCRIPT_CONTINUE;
    }

    private void knockdownPlayer(obj_id who)
    {
        setPosture(who, POSTURE_KNOCKED_DOWN);
        messageTo(who, "clearPostureChange", null, 1.2f, false);
    }

    public int clearPostureChange(obj_id self, dictionary params)
    {
        setPosture(self, POSTURE_UPRIGHT);
        return SCRIPT_CONTINUE;
    }

    public int OnTaskActivated(obj_id self, int questCrc, int taskId) throws InterruptedException
    {
        LOG("ethereal", "[Quest]: " + "Player " + getPlayerFullName(self) + " has activated task " + taskId + " for quest " + questCrc);
        return SCRIPT_CONTINUE;
    }

    public int OnTaskCompleted(obj_id self, int questCrc, int taskId) throws InterruptedException
    {
        LOG("ethereal", "[Quest]: " + "Player " + getPlayerFullName(self) + " has completed task " + taskId + " for quest " + questCrc);
        return SCRIPT_CONTINUE;
    }

    public int OnPlayerReportedChat(obj_id self, obj_id reporter, String spammerName, String spammerMessage)
    {
        LOG("ethereal", "[Chat]: " + "Player " + getPlayerFullName(reporter) + " has reported " + spammerName + " for spamming " + spammerMessage);
        return SCRIPT_CONTINUE;
    }

    public int OnSkillGranted(obj_id self, String skill) throws InterruptedException
    {
        LOG("ethereal", "[Skills]: " + "Player " + getPlayerFullName(self) + " has earned the skill " + skill);
        return SCRIPT_CONTINUE;
    }

    public int OnSkillRevoked(obj_id self, String skill) throws InterruptedException
    {
        LOG("ethereal", "[Skills]: " + "Player " + getPlayerFullName(self) + " has had the skill " + skill + " revoked.");
        return SCRIPT_CONTINUE;
    }

    public int cmdAiManipulate(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        LOG("ethereal", "[AI]: " + "Player used /aiManipulate with params of '" + params + "'");
        if (!isGod(self))
        {
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(target) || isPlayer(target) || params == null || params.equalsIgnoreCase(""))
        {
            broadcast(self, "[syntax] /aiManipulate [command] ([subcommand])");
        }
        else
        {
            StringTokenizer st = new StringTokenizer(params);
            String command = st.nextToken();
            if (command.equals("aggroPlayer"))
            {
                obj_id whom = getPlayerIdFromFirstName(st.nextToken());
                if (isIdValid(whom))
                {
                    startCombat(target, whom);
                    broadcast(self, "Aggroing " + getPlayerFullName(whom) + " on " + (isMob(target) ? getTemplateName(target) : getCreatureName(target)));
                }
            }
            if (command.equals("aggroMob"))
            {
                obj_id whom = getTarget(self);
                obj_id assaulter = getIntendedTarget(self);
                if (isIdValid(assaulter) && isIdValid(whom))
                {
                    startCombat(assaulter, whom);
                    broadcast(self, "Aggroing " + assaulter + " on " + target);
                }
                else
                {
                    broadcast(self, "Invalid target(s) for aggroMob");
                }
            }
            if (command.equals("flee"))
            {
                float minDistance = utils.stringToFloat(st.nextToken());
                float maxDistance = utils.stringToFloat(st.nextToken());
                if (st.countTokens() != 2)
                {
                    broadcast(self, "Invalid number of parameters for flee");
                    broadcast(self, "[syntax] /aiManipulate flee [minDistance] [maxDistance]");
                    return SCRIPT_CONTINUE;
                }
                ai_lib.flee(target, self, minDistance, maxDistance);
            }
            if (command.equals("movement"))
            {
                String movement = st.nextToken();
                switch (movement)
                {
                    case "wander":
                    {
                        ai_lib.setDefaultCalmBehavior(target, ai_lib.BEHAVIOR_WANDER);
                        broadcast(self, "Setting " + target + " to wander");
                    }
                    case "sentinel":
                    {
                        ai_lib.setDefaultCalmBehavior(target, ai_lib.BEHAVIOR_SENTINEL);
                        broadcast(self, "Setting " + target + " to sentinel");
                    }
                    case "loiter":
                    {
                        ai_lib.setDefaultCalmBehavior(target, ai_lib.BEHAVIOR_LOITER);
                        broadcast(self, "Setting " + target + " to loiter");
                    }
                    case "stop":
                    {
                        ai_lib.setDefaultCalmBehavior(target, ai_lib.BEHAVIOR_STOP);
                        broadcast(self, "Setting " + target + " to stop");
                    }
                    default:
                        broadcast(self, "Invalid movement type specified. Valid types are: wander, sentinel, loiter, stop");
                }
            }
            if (command.equals("follow"))
            {
                String flag = st.nextToken();
                if (flag.equals("-name"))
                {
                    obj_id whom = getPlayerIdFromFirstName(st.nextToken());
                    if (isIdValid(whom))
                    {
                        ai_lib.aiFollow(target, whom);
                        broadcast(self, "Following " + getPlayerFullName(whom));
                    }
                }
                else if (flag.equals("-self"))
                {
                    ai_lib.aiFollow(target, self);
                    broadcast(self, "Making " + (isMob(target) ? getTemplateName(target) : getCreatureName(target)) + " follow " + getPlayerFullName(self));
                }
                else if (flag.equals("-stop"))
                {
                    ai_lib.aiStopFollowing(target);
                    broadcast(self, "Stopping " + (isMob(target) ? getTemplateName(target) : getCreatureName(target)) + " from following.");
                }
                else
                {
                    broadcast(self, "Invalid flag specified. Valid flags are: -name, -self, -stop");
                }
            }
            if (command.equals("level"))
            {
                int level = utils.stringToInt(st.nextToken());
                if (level < 1 || level > 90)
                {
                    broadcast(self, "Invalid level specified. Valid levels are 1-90");
                    return SCRIPT_CONTINUE;
                }
                setLevel(target, level);
            }
            if (command.equals("health"))
            {
                int health = utils.stringToInt(st.nextToken());
                if (health < 1)
                {
                    broadcast(self, "Invalid health specified. Valid health is 1 or greater");
                    return SCRIPT_CONTINUE;
                }
                setMaxAttrib(target, HEALTH, health);
                setAttrib(target, HEALTH, health);
            }
            if (command.equals("action"))
            {
                int action = utils.stringToInt(st.nextToken());
                if (action < 1)
                {
                    broadcast(self, "Invalid action specified. Valid action is 1 or greater");
                    return SCRIPT_CONTINUE;
                }
                setMaxAttrib(target, ACTION, action);
                setAttrib(target, ACTION, action);
            }
            if (command.equalsIgnoreCase("copycat"))
            {
                if (isPlayer(target))
                {
                    broadcast(self, "You cannot use this command a player.");
                    return SCRIPT_CONTINUE;
                }
                obj_id[] currentGear = getAllWornItems(target, false);
                for (obj_id deleteMe : currentGear)
                {
                    if (isIdValid(deleteMe))
                    {
                        destroyObject(deleteMe);
                    }
                }
                obj_id[] equipments = getAllWornItems(self, true);
                for (obj_id equipment : equipments)
                {
                    if (isIdValid(equipment))
                    {
                        String template = getTemplateName(equipment);
                        obj_id newEquipment = createObject(template, target, "");
                        if (isIdValid(newEquipment))
                        {
                            equip(newEquipment, target);
                        }
                    }
                }
                broadcast(self, "Dressing " + target + " with " + getPlayerName(self) + "'s gear");
            }
            if (command.equals("mood"))
            {
                ai_lib.setMood(target, st.nextToken());
                broadcast(self, "Attempting to set mood for " + target + " to " + st.nextToken());
            }
            if (command.equals("name"))
            {
                String name = st.nextToken();
                setName(target, name);
                broadcast(self, "Attempting to set name for " + target + " to " + name);
            }
            if (command.equals("faction"))
            {
                String faction = st.nextToken();
                setFaction(target, faction);
                broadcast(self, "Attempting to set faction for " + target + " to " + faction);
            }
            if (command.equals("noClap"))
            {
                String flag = st.nextToken();
                if (flag.equals("on"))
                {
                    setObjVar(target, "ai.noClap", true);
                    broadcast(self, "This creature will no longer clap near an entertainer.");
                }
                else if (flag.equals("off"))
                {
                    removeObjVar(target, "ai.noClap");
                    broadcast(self, "This creature will now clap near an entertainer.");
                }
            }
            if (command.equals("expel"))
            {
                String flag = st.nextToken();
                if (flag.equals("on"))
                {
                    setObjVar(target, "ai.expel", true);
                }
                else if (flag.equals("off"))
                {
                    removeObjVar(target, "ai.expel");
                }
            }

            if (command.equals("stripNaked"))
            {
                obj_id[] wearables = getInventoryAndEquipment(target);
                if (!isPlayer(target))
                {
                    for (obj_id desMe : wearables)
                    {
                        broadcast(self, "Stripping off " + desMe + " from " + target);
                        destroyObject(desMe);
                    }
                }
            }

            if (command.equals("dressUp"))
            {
                if (!hasScript(target, "developer.bubbajoe.wear"))
                {
                    attachScript(target, "developer.bubbajoe.wear");
                }
            }

            if (command.equals("queueCommandSelf"))
            {
                String commandName = st.nextToken();
                StringBuilder commandArgs = new StringBuilder();
                int hashValue = getStringCrc(commandName);
                while (st.hasMoreTokens())
                {
                    commandArgs.append(" ").append(st.nextToken());
                }
                queueCommand(target, hashValue, self, commandArgs.toString(), COMMAND_PRIORITY_IMMEDIATE);
            }
            if (command.equals("queueCommandTarget"))
            {
                String targetName = st.nextToken();
                String commandName = st.nextToken();
                StringBuilder commandArgs = new StringBuilder();
                int hashValue = getStringCrc(commandName);
                while (st.hasMoreTokens())
                {
                    commandArgs.append(" ").append(st.nextToken());
                }
                queueCommand(target, hashValue, utils.getPlayerIdFromFirstName(targetName), commandArgs.toString(), COMMAND_PRIORITY_IMMEDIATE);
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdInviteGroupArea(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (group.isGrouped(self))
        {
            if (group.isLeader(self))
            {
                obj_id[] players = getPlayerCreaturesInRange(getLocation(self), 64.0f);
                if (players == null || players.length == 0)
                {
                    broadcast(self, "No players found to area invite.");
                    return SCRIPT_CONTINUE;
                }
                for (obj_id player : players)
                {
                    if (isPlayer(player) && player != self)
                    {
                        if (getLocation(self).cell != getLocation(player).cell)
                        {
                            //Skip players that do not share the same cell. world->world or cell->-cell only
                            continue;
                        }
                        sendConsoleCommand("/invite " + getPlayerName(player), self);
                    }
                }
            }
            else
            {
                broadcast(self, "You must be the group leader to use this command while in a group..");
            }
        }
        else
        {
            obj_id[] players = getPlayerCreaturesInRange(getLocation(self), 64.0f);
            if (players == null || players.length == 0)
            {
                broadcast(self, "No players found to area invite.");
                return SCRIPT_CONTINUE;
            }
            for (obj_id player : players)
            {
                if (isPlayer(player) && player != self)
                {
                    if (getLocation(self).cell != getLocation(player).cell)
                    {
                        //Skip players that do not share the same cell. world->world or cell->-cell invites only.
                        continue;
                    }
                    sendConsoleCommand("/invite " + getPlayerName(player), self);
                }
            }
        }
        LOG("ethereal", "[Group]: " + getFirstName(self) + " is inviting all players in range to group at " + getLocation(self).toReadableFormat(true));
        return SCRIPT_CONTINUE;
    }

    public int cmdTellArea(obj_id self, obj_id target, String params, float defaultTime)
    {
        StringTokenizer st = new StringTokenizer(params);
        StringBuilder message = new StringBuilder();
        while (st.hasMoreTokens())
        {
            message.append(" ").append(st.nextToken());
        }
        obj_id[] players = getPlayerCreaturesInRange(getLocation(self), 64.0f);
        if (players == null || players.length == 0)
        {
            broadcast(self, "No players found to send a tell to.");
            return SCRIPT_CONTINUE;
        }
        for (obj_id player : players)
        {
            if (isPlayer(player) && player != self)
            {
                sendConsoleCommand("/tell " + getPlayerName(player) + " " + message, self);
            }
        }
        LOG("ethereal", "[IM]: [" + getFirstName(self) + "] to [Area]: " + message);
        return SCRIPT_CONTINUE;
    }

    public int cmdTellPlanet(obj_id self, obj_id target, String params, float defaultTime)
    {
        if (!isGod(self))
        {
            return SCRIPT_CONTINUE;
        }
        StringTokenizer st = new StringTokenizer(params);
        StringBuilder message = new StringBuilder();
        while (st.hasMoreTokens())
        {
            message.append(" ").append(st.nextToken());
        }
        obj_id[] players = getPlayerCreaturesInRange(getLocation(self), 16000.0f);
        if (players == null || players.length == 0)
        {
            broadcast(self, "No players found to send a tell to.");
            return SCRIPT_CONTINUE;
        }
        for (obj_id player : players)
        {
            if (isPlayer(player) && player != self)
            {
                sendConsoleCommand("/tell " + getPlayerName(player) + " " + message, self);
            }
        }
        LOG("ethereal", "[IM]: [" + getFirstName(self) + "] to [Area]: " + message);
        return SCRIPT_CONTINUE;
    }

    public int cmdAnnounceTrade(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (utils.getPlayerProfession(self) != utils.TRADER)
        {
            broadcast(self, "You must be a trader to use this command.");
            return SCRIPT_CONTINUE;
        }
        //send a message to the trade channel in discord
        int cooldown = 3600;
        int lastAnnounce = getIntObjVar(self, "trade.lastAnnounce");
        int currentTime = getCalendarTime();
        if (currentTime < lastAnnounce + cooldown)
        {
            int timeRemaining = lastAnnounce + cooldown - currentTime;
            broadcast(self, "You must wait " + timeRemaining / 60 + " minutes before you can announce your trade goods again.");
            return SCRIPT_CONTINUE;
        }
        if (params == null || params.isEmpty())
        {
            broadcast(self, "You must specify a message to announce.");
            return SCRIPT_CONTINUE;
        }
        else
        {
            if (isAppropriateText(params))
            {
                titan_player.sendTradeUpdateToDiscord(self, params, "Galactic Trading Association: " + getPlayerFullName(self), false, false, false);
                setObjVar(self, "trade.lastAnnounce", currentTime);
            }
            else
            {
                broadcast(self, "Your message contains inappropriate language. Please try again.");
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdTapeMeasure(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        obj_id objectOne = getIntendedTarget(self);
        obj_id objectTwo = getLookAtTarget(self);
        if (isIdValid(objectOne) && isIdValid(objectTwo))
        {
            location locOne = getLocation(objectOne);
            location locTwo = getLocation(objectTwo);

            float dx = locOne.x - locTwo.x;
            float dy = locOne.y - locTwo.y;
            float dz = locOne.z - locTwo.z;

            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            broadcast(self, "The 3D distance between these two targets is " + distance + " or " + Math.round(distance) + " rounded.");
        }
        else
        {
            broadcast(self, "You must have two targets selected (mouse-over and target) to use this command.");
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdTell(obj_id self, obj_id target, String params, float defaultTime)
    {
        if (LOGGING)
        {
            //String s_outputfile = "/home/swg/swg-main/exe/linux/logs/im.log";
            StringTokenizer parser = new StringTokenizer(params);
            String who = parser.nextToken();
            String message = "";
            while (parser.hasMoreTokens())
            {
                message = parser.nextToken() + " ";
            }
            String finalizedMessage = message;
            LOG("ethereal", "[IM]: [" + getFirstName(self) + "] to [" + toUpper(who, 0) + "]: " + finalizedMessage);
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdSaveCharacter(obj_id self, obj_id target, String params, float defaultTime)
    {
        if (isGod(self))
        {
            save(self);
            broadcast(self, "Character saved.");
        }
        else
        {
            broadcast(self, "You must be ethereal to use this command.");
        }
        return SCRIPT_CONTINUE;
    }

    private void save(obj_id self)
    {
        broadcast(self, "Saving character.");
        /*
        String characterName = getFirstName(self);
        String characterFile = "save/" + characterName + ".iff";
        obj_id[] inventory = getInventoryAndEquipment(self);
        obj_id[] bank = getInventoryAndEquipment(getBankId(self));
        obj_id[] datapad = getInventoryAndEquipment(getDatapadId(self));
        obj_id[] pInv = getInventoryAndEquipment(getPlayerId(self));
        obj_id[] objects = new obj_id[inventory.length + bank.length + datapad.length + pInv.length];
        int i = 0;
        for (obj_id item : inventory)
        {
            objects[i] = item;
            i++;
        }
        for (obj_id item : bank)
        {
            objects[i] = item;
            i++;
        }
        for (obj_id item : datapad)
        {
            objects[i] = item;
            i++;
        }
        for (obj_id item : pInv)
        {
            objects[i] = item;
            i++;
        }
        utils.saveObject(objects, characterFile, "objects");*/
    }

    public boolean hasChristmasPresent(obj_id object)
    {
        return hasObjVar(object, "lifeday.gift_23");
    }

    public int cmdCityWarn(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        location currentSpot = getLocation(self);
        int city_id = getCityAtLocation(getLocation(self), 0);
        if (city_id == 0)
        {
            broadcast(self, "You must be in a city to execute this command.");
            return SCRIPT_CONTINUE;
        }
        obj_id cityHall = cityGetCityHall(city_id);
        float radius = 0f;
        if (city.isTheCityMayor(self, city_id))
        {
            obj_id enemy = getPlayerIdFromFirstName(params);
            if (!isIdValid(enemy) || !isPlayer(enemy))
            {
                broadcast(self, "Player not found.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                switch (city.getCityRank(city_id))
                {
                    case 1:
                        radius = 150f;
                        break;
                    case 2:
                        radius = 200f;
                        break;
                    case 3:
                        radius = 300f;
                        break;
                    case 4:
                        radius = 400f;
                        break;
                    case 5:
                        radius = 450f;
                        break;
                }
                if (getDistance(enemy, currentSpot) >= radius)
                {
                    broadcast(self, "That person does not pose a current threat to " + cityGetName(city_id));
                }
                else
                {
                    //TODO: make enemy attackable by all citizens online.
                    if (group.isGrouped(enemy))
                    {
                        groupTEF(self, group.getGroupObject(enemy), city_id);
                    }
                    else
                    {
                        //solo warn
                        soloTEF(self, enemy, city_id);
                    }
                }
            }

        }
        return SCRIPT_CONTINUE;
    }

    public int handleWarnPulse(obj_id self, dictionary params)
    {
        /*int lastCheck = params.getInt("lastPulse");
        if (lastCheck > getGameTime() - 60)
        {
            return SCRIPT_CONTINUE;
        }// help
        */
        location currentSpot = getLocation(self);
        int city_id = getCityAtLocation(currentSpot, 0);
        obj_id cityHall = cityGetCityHall(city_id);
        if (cityHall != null) //assuming this is 0 if not near a city hall. idk :)
        {
            if (!hasObjVar(self, "death_warrant"))
            {
                setObjVar(self, "death_warrant", city_id);
                broadcast(self, "The mayor of " + cityGetName(city_id) + " has signed your death warrant!");
                messageTo(self, "handleWarnPulse", null, 120.0f, isGod(self));
            }
            else
            {
                broadcast(self, "You still have an active death warrant out for in this city!");
                messageTo(self, "handleWarnPulse", null, 120.0f, isGod(self));
            }
        }
        else
        {
            //remove tef if not in city limits
            pvpRemoveAllTempEnemyFlags(self);
            removeObjVar(self, "death_warrant");
            broadcast(self, "You have escaped your death! It seems best to stay away from that place.");
        }
        return SCRIPT_CONTINUE;
    }

    public int groupTEF(obj_id self, obj_id group, int city_id)
    {
        //get All players in city
        obj_id[] validCitizens = cityGetCitizenIds(city_id);
        if (validCitizens != null)
        {
            for (obj_id citizen : validCitizens)
            {
                if (citizen != self)
                {
                    obj_id[] objPlayers = getGroupMemberIds(group);
                    if (objPlayers != null)
                    {
                        for (obj_id objPlayer : objPlayers)
                        {
                            if (objPlayer != self)
                            {
                                if (isInWorldCell(objPlayer))
                                {
                                    //TEF the player
                                    pvpSetPersonalEnemyFlag(objPlayer, citizen);
                                    dictionary d = new dictionary();
                                    d.put("lastPulse", getGameTime());
                                    messageTo(objPlayer, "handleWarnPulse", d, 120.0f, isGod(self));
                                    LOG("ethereal", "[City TEF System]:" + getFirstName(objPlayer) + " has been TEF'd for " + getPlayerFullName(citizen));
                                }
                            }
                        }
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int soloTEF(obj_id self, obj_id target, int city_id)
    {
        obj_id[] validCitizens = cityGetCitizenIds(city_id);
        if (validCitizens != null)
        {
            for (obj_id citizen : validCitizens)
            {
                if (citizen != self)
                {
                    if (isInWorldCell(citizen))
                    {
                        //TEF the player
                        pvpSetPersonalEnemyFlag(citizen, target);
                        dictionary d = new dictionary();
                        d.put("lastPulse", getGameTime());
                        messageTo(target, "handleWarnPulse", d, 60.0f, isGod(self));
                        LOG("ethereal", "[City TEF System]:" + getFirstName(self) + " has been TEF'd for " + getPlayerFullName(citizen));
                    }
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdGroupLootToggle(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        // Ensure the player is in a group or is a god
        if (!group.isGrouped(self) && !isGod(self))
        {
            broadcast(self, "You must be in a group to use this command.");
            return SCRIPT_CONTINUE;
        }
        // Ensure the player is the group leader or is a god
        if (!group.isLeader(self) && !isGod(self))
        {
            broadcast(self, "You must be the group leader to use this command.");
            return SCRIPT_CONTINUE;
        }

        // Show the SUI pop-up window for enable/disable toggle
        dictionary paramsDict = new dictionary();
        paramsDict.put("group", getGroupObject(self));
        String prompt = "Select enable or disable to toggle Chronicles *AND* Beast Mastery loot. \n\nNote: You will need to reset this option once leaving the group.";
        String title = "Group Loot Toggle";
        sui.msgbox(self, self, prompt, sui.YES_NO, title, "handleGroupLootToggle");

        return SCRIPT_CONTINUE;
    }

    public int handleGroupLootToggle(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        obj_id group = getGroupObject(player);
        obj_id[] members = getGroupMemberIds(group);
        int buttonPressed = sui.getIntButtonPressed(params);

        if (buttonPressed == sui.BP_CANCEL)
        {
            for (obj_id member : members)
            {
                setObjVar(member, "enzymeBlock", true);
                setObjVar(member, "chroniclesLoot_toggledOff", true);
                broadcast(member, "Your group leader has disabled Chronicles loot and beast enzymes.");
            }
        }
        else if (buttonPressed == sui.BP_OK)
        {
            for (obj_id member : members)
            {
                removeObjVar(member, "enzymeBlock");
                removeObjVar(member, "chroniclesLoot_toggledOff");
                broadcast(member, "Your group leader has enabled Chronicles loot and beast enzymes.");
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdReadyCheck(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (group.isGrouped(self))
        {
            obj_id group = getGroupObject(self);
            obj_id[] members = getGroupMemberIds(group);
            obj_id leader = getGroupLeaderId(group);
            if (leader != self && !isGod(self))
            {
                broadcast(self, "You must be the group leader to use this command.");
                return SCRIPT_CONTINUE;
            }

            // Initialize counters
            setObjVar(group, "readyCheck.ready", 0);
            setObjVar(group, "readyCheck.notReady", 0);
            setObjVar(group, "readyCheck.total", members.length);

            for (obj_id member : members)
            {
                if (isPlayer(member))
                {
                    broadcast(member, getPlayerFullName(self) + " has initiated a ready check.");
                    sui.msgbox(self, member, "Are you ready?", sui.YES_NO, getPlayerFullName(leader) + "'s Group", "handleReadyCheck");
                }
            }
        }
        else
        {
            broadcast(self, "You must be in a group to use this command.");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleReadyCheck(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id target = sui.getPlayerId(params);
        obj_id group = getGroupObject(target);
        obj_id leader = getGroupLeaderId(group);

        if (!isIdValid(group) || !isIdValid(leader))
        {
            return SCRIPT_CONTINUE;
        }

        int bp = sui.getIntButtonPressed(params);
        boolean isReady = (bp != sui.BP_CANCEL);

        // Update counts
        String varToUpdate = isReady ? "readyCheck.ready" : "readyCheck.notReady";
        int currentCount = getIntObjVar(group, varToUpdate);
        setObjVar(group, varToUpdate, currentCount + 1);

        // Notify leader
        sendConsoleCommand("/tell " + getPlayerName(leader) + " " + getPlayerName(target) + (isReady ? " is ready." : " is not ready."), target);
        showFlyText(target, string_id.unlocalized(isReady ? "Ready!" : "Not Ready!"), 2.0f, isReady ? colors.GREEN : colors.RED);
        broadcast(target, "You inform your group leader that you are " + (isReady ? "ready." : "not ready."));

        // Check if all responses are in
        int total = getIntObjVar(group, "readyCheck.total");
        int readyCount = getIntObjVar(group, "readyCheck.ready");
        int notReadyCount = getIntObjVar(group, "readyCheck.notReady");

        if (readyCount + notReadyCount >= total)
        {
            // Broadcast results to leader
            String resultMessage = "Ready Check Results: " + readyCount + " Ready, " + notReadyCount + " Not Ready.";
            broadcast(leader, resultMessage);
            debugSpeakMsg(leader, resultMessage);
        }

        return SCRIPT_CONTINUE;
    }

    public int manageLoot(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        /*if (combat.isInCombat(self) || getState(self, STATE_FEIGN_DEATH) == 1 || getState(self, STATE_COMBAT) == 1)
        {
            broadcast(self, "You cannot manage your inventory while in combat.");
            return SCRIPT_CONTINUE;
        }
        if (getState(self, STATE_SWIMMING) == 1)
        {
            broadcast(self, "You cannot sell all your loot while swimming.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_INCAPACITATED)
        {
            broadcast(self, "You cannot manage your inventory while incapacitated.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_DRIVING_VEHICLE)//rm
        {
            broadcast(self, "You cannot manage your inventory while driving a vehicle.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_PRONE)
        {
            broadcast(self, "You cannot manage your inventory while prone.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_KNOCKED_DOWN)
        {
            broadcast(self, "You cannot manage your inventory while knocked down.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_RIDING_CREATURE)//rm
        {
            broadcast(self, "You cannot manage your inventory while riding a creature.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_SITTING)
        {
            broadcast(self, "You cannot manage your inventory while sitting.");
            return SCRIPT_CONTINUE;
        }
        if (getPosture(self) == POSTURE_CROUCHED)
        {
            broadcast(self, "You cannot manage your inventory while kneeling.");
            return SCRIPT_CONTINUE;
        }
        if (!getCreatureCoverVisibility(self))
        {
            broadcast(self, "You cannot manage your inventory while in stealth.");
            return SCRIPT_CONTINUE;
        }
        if (!isInWorldCell(self) && !isGod(self))
        {
            broadcast(self, "You must be outside to manage your inventory in this capacity.");
            return SCRIPT_CONTINUE;
        }
        int cooldownTime = 60 * 5;
        int lastTransaction = getIntObjVar(self, "ui.sellLootLast");
        if (lastTransaction > getCalendarTime() - cooldownTime)
        {
            if (isGod(self))
            {
                broadcast(self, "[GM]: Bypassing cooldown for testing purposes.");
            }
            else
            {
                int timeRemaining = lastTransaction + cooldownTime - getCalendarTime();
                broadcast(self, "You must wait " + timeRemaining / 60 + " minutes before you can sell all your loot again.");
                return SCRIPT_CONTINUE;
            }
        }

        if (sui.hasPid(self, "manageLoot"))
        {
            int pid = sui.getPid(self, "manageLoot");
            forceCloseSUIPage(pid);
            sui.removePid(self, "manageLoot");
        }
        String okButton = "Sell All Loot";
        String cancelButton = "Cancel";
        String title = "Task: Sell Loot";
        String textMsg = "Confirm that you wish to sell \\#DAA450*ALL*\\#. of your loot for 1/3rd the amount of credits you would normally get at a Junk Dealer.";
        textMsg += target_dummy.addLineBreaks(2);
        textMsg += "Would you like to " + okButton + "?";
        int pid = sui.createSUIPage(sui.SUI_MSGBOX, self, self, "sellInvJunk");
        setSUIProperty(pid, sui.MSGBOX_TITLE, sui.PROP_TEXT, title);
        setSUIProperty(pid, sui.MSGBOX_PROMPT, sui.PROP_TEXT, textMsg);
        sui.msgboxButtonSetup(pid, sui.YES_NO);
        setSUIProperty(pid, sui.MSGBOX_BTN_OK, sui.PROP_TEXT, okButton);
        setSUIProperty(pid, sui.MSGBOX_BTN_CANCEL, sui.PROP_TEXT, cancelButton);
        sui.showSUIPage(pid);
        sui.setPid(self, pid, "manageLoot");
        */
        return SCRIPT_CONTINUE;
    }

    public int cmdVehicleMechanic(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (hasObjVar(self, "myCurrentVehicle"))
        {
            removeObjVar(self, "myCurrentVehicle");
        }
        if (hasObjVar(self, "vehicleHorn.pid"))
        {
            forceCloseSUIPage(getIntObjVar(self, "vehicleHorn.pid"));
            removeObjVar(self, "vehicleHorn.pid");
        }
        if (combat.isInCombat(self))
        {
            broadcast(self, "You cannot manage your vehicle while in combat.");
            return SCRIPT_CONTINUE;
        }
        obj_id mount = getMountId(self);
        if (mount == null)
        {
            broadcast(self, "You must be driving a vehicle you own to manage it.");
            return SCRIPT_CONTINUE;
        }
        else
        {
            reattachScripts(self, "player.player_vehicle");
        }
        return SCRIPT_CONTINUE;
    }

    public int reattachScripts(obj_id target, String script)
    {
        if (hasScript(target, script))
        {
            detachScript(target, script);
            attachScript(target, script);
        }
        else
        {
            attachScript(target, script);
        }
        return SCRIPT_CONTINUE;
    }

    public int onFactionStandingResponse(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        if (sui.getIntButtonPressed(params) == sui.BP_OK)
        {
            broadcast(player, "Saving factional information...");
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdGmCraftSchematic(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (!isGod(self))
            return SCRIPT_CONTINUE;

        if (params == null || params.isEmpty())
        {
            sendSystemMessageTestingOnly(self, "[GM Craft] No schematic template provided.");
            return SCRIPT_CONTINUE;
        }

        String schematic = params.trim();
        obj_id inventory = utils.getInventoryContainer(self);
        if (!isIdValid(inventory))
        {
            sendSystemMessageTestingOnly(self, "[GM Craft] Could not find inventory.");
            return SCRIPT_CONTINUE;
        }

        obj_id item = makeCraftedItem(schematic, 1000.0f, inventory);
        if (isIdValid(item))
        {
            sendSystemMessageTestingOnly(self, "[GM Craft] Created: " + getName(item) + " (q1000) in your inventory.");
            CustomerServiceLog("gmCraft", "GM " + getPlayerFullName(self) + " crafted " + item + " from " + schematic + " at quality 1000.");
        }
        else
        {
            sendSystemMessageTestingOnly(self, "[GM Craft] Failed to create item from: " + schematic);
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdGmCrateSchematic(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (!isGod(self))
            return SCRIPT_CONTINUE;

        if (params == null || params.isEmpty())
        {
            sendSystemMessageTestingOnly(self, "[GM Crate] No schematic template provided.");
            return SCRIPT_CONTINUE;
        }

        String schematic = params.trim();
        obj_id inventory = utils.getInventoryContainer(self);
        if (!isIdValid(inventory))
        {
            sendSystemMessageTestingOnly(self, "[GM Crate] Could not find inventory.");
            return SCRIPT_CONTINUE;
        }

        // generateFactoryCrate creates the crate directly from the schematic (no prototype objvar needed)
        obj_id crate = generateFactoryCrate(schematic, 1000.0f, inventory);
        if (!isIdValid(crate))
        {
            sendSystemMessageTestingOnly(self, "[GM Crate] Failed to create factory crate from: " + schematic);
            return SCRIPT_CONTINUE;
        }

        setCount(crate, 100);
        obj_id[] contents = getContents(crate);
        if (contents != null && contents.length > 0)
        {
            String prototypeName = getName(contents[0]);
            if (prototypeName != null && !prototypeName.isEmpty())
                setName(crate, prototypeName);
        }
        sendSystemMessageTestingOnly(self, "[GM Crate] Created factory crate: " + getName(crate) + " (q1000 x100) in your inventory.");
        CustomerServiceLog("gmCraft", "GM " + getPlayerFullName(self) + " created factory crate " + crate + " from " + schematic + " at quality 1000 x100.");
        return SCRIPT_CONTINUE;
    }

}
