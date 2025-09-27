package script.event.halloween;/*
@Origin: dsrc.script.event.halloween.trick_thief
@Author: BubbaJoeX
@Purpose: Handles the Frankenstein world boss for Galactic Moon Festival.
@Notes;
    This world boss has six mechanics.
        1. The Frankenabubb will spawn minions in a ring around him.
        2. The Frankenabubb will spawn a sidekick to assist him.
        3. The Frankenabubb will steal credits from players.
        4. The Frankenabubb will spawn a second sidekick to assist him.
        5. The Frankenabubb will spawn a second ring of minions around him.
        6. The Frankenabubb will apply a buff to himself.
    This script is meant to be used by GMs to spawn the Frankenabubb for the event.
    To set the loot drop template, change the LOOT_DROP_TEMPLATE string. Or refactor to a static item.
@Created: Sunday, 2/25/2024, at 11:42 PM,
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.library.*;
import script.location;
import script.menu_info;
import script.obj_id;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class frankenabubb extends script.base_script
{
    public static final String RING_EVENT_ONE = "event_halloween_undead_skeletons";
    public static final String RING_EVENT_TWO = "event_halloween_undead_zombies";
    public static final int RING_EVENT_NUM_MOBS = 12;
    public static final float RING_EVENT_MOB_RANGE = 24.0f;
    public static final String SIDEKICK_TEMPLATE_ONE = "event_halloween_sidekick_01";
    public static final String SIDEKICK_TEMPLATE_TWO = "event_halloween_sidekick_02";
    public static final String SIDEKICK_BUFF = "event_halloween_sidekick_buff";
    public static final String FINAL_BUFF = "event_halloween_boss_last_chance";
    public static final int FINAL_BUFF_DURATION = 5;
    public static final int FINAL_BUFF_POWER = 100;
    public static final float RUBBERBAND_DISTANCE = 16.0f;
    public static final String LOOT_DROP_TEMPLATE = "item_event_token_01_01";

    public frankenabubb()
    {
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

    public static void pushPlayer(obj_id self, obj_id player, float distance)
    {
        if (!isIdValid(player) || !exists(player))
        {
            return;
        }
        location loc = getLocation(player);
        float x;
        float z;
        float angle = rand(0.0f, 360.0f);
        x = loc.x + (float) Math.cos(angle) * distance;
        z = loc.z + (float) Math.sin(angle) * distance;
        setLocation(player, new location(x, loc.y, z, loc.area));
        //debugServerConsoleMsg(self, "pushPlayer() - player pushed.");
    }

    public static boolean isHalloween()
    {
        ZonedDateTime stamp = ZonedDateTime.now(ZoneId.of("America/Chicago"));
        LocalDate now = stamp.toLocalDate();
        return now.getMonthValue() == 10 && now.getDayOfMonth() == 31;
    }

    public int summonZombies(obj_id source) throws InterruptedException
    {
        createCircleSpawn(source, source, RING_EVENT_ONE, RING_EVENT_NUM_MOBS, RING_EVENT_MOB_RANGE);
        return SCRIPT_CONTINUE;
    }

    private void spawnSkellyGuys(obj_id self) throws InterruptedException
    {
        createCircleSpawn(self, self, frankenabubb.RING_EVENT_ONE, frankenabubb.RING_EVENT_NUM_MOBS, frankenabubb.RING_EVENT_MOB_RANGE);
    }

    public int zombieGoo(obj_id source) throws InterruptedException
    {
        broadcast(source, "You have been hit with zombie goo!");
        buff.applyBuff(source, "event_halloween_zombie_goo");
        LOG("events", "GMF Frankenabubb - " + getPlayerFullName(source) + " hit with zombie goo.");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (!isHalloween())
        {
            //destroyObject(self);
            LOG("events", "GMF Frankenabubb - Warning: Not Halloween.");
            return SCRIPT_CONTINUE;
        }
        setInvulnerable(self, false);
        setName(self, "Frankenabubb");
        setScale(self, 2.5f);
        startFog(self);
        LOG("events", "GMF Frankenabubb - Attached and started fog.");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        LOG("events", "GMF Frankenabubb - Initialized.");
        return SCRIPT_CONTINUE;
    }

    public int OnEnteredCombat(obj_id self) throws InterruptedException
    {
        if (utils.hasScriptVar(self, "event.halloween_spam"))
        {
            return SCRIPT_CONTINUE;
        }
        else
        {
            obj_id[] attackers = getHateList(self);
            if (attackers == null || attackers.length == 0)
            {
                return SCRIPT_CONTINUE;
            }
            StringBuilder attackerNames = new StringBuilder();
            for (obj_id person : attackers)
            {
                attackerNames.append(getPlayerFullName(person)).append(", ");
            }
            LOG("events", "GMF Frankenabubb - Engaged with player(s) " + attackerNames);
            utils.setScriptVar(self, "event.halloween_spam", 1);
            sendSystemMessageGalaxyTestingOnly("[World Event] Frankenabubb has been engaged on " + toUpper(getCurrentSceneName(), 0));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnCreatureDamaged(obj_id self, obj_id attacker, obj_id weapon, int[] damage) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, "event.halloween_group_checked"))
        {
            handleGroupHealthIncrease(self, attacker);
        }

        if (beast_lib.isBeast(attacker) || pet_lib.isPet(attacker))
        {
            attacker = getMaster(attacker);
        }

        if (!isPlayer(attacker))
        {
            return SCRIPT_CONTINUE;
        }

        handleHealthRatios(self, attacker);
        return SCRIPT_CONTINUE;
    }

    private void handleGroupHealthIncrease(obj_id self, obj_id attacker) throws InterruptedException
    {
        if (group.isGrouped(attacker))
        {
            obj_id[] groupMembers = getGroupMemberIds(attacker);
            if (groupMembers == null || groupMembers.length == 0)
            {
                return;
            }
            for (obj_id groupMember : groupMembers)
            {
                addToHealth(self, groupMembers.length * 12000);
                broadcast(attacker, "[World Boss System]: Being grouped has increased this enemy's statistics.");
            }
            utils.setScriptVar(self, "event.halloween_group_checked", 1);
        }
    }

    private void handleHealthRatios(obj_id self, obj_id attacker) throws InterruptedException
    {
        float max = getMaxHealth(self);
        float current = getHealth(self);
        float ratio = current / max;

        if (ratio <= 0.90f && !utils.hasScriptVar(self, "event.halloween_first_ratio"))
        {
            triggerFirstRatioEvent(self, attacker);
        }
        else if (ratio <= 0.75f && !utils.hasScriptVar(self, "event.halloween_second_ratio"))
        {
            triggerSecondRatioEvent(self, attacker);
        }
        else if (ratio <= 0.60f && !utils.hasScriptVar(self, "event.halloween_third_ratio"))
        {
            triggerThirdRatioEvent(self, attacker);
        }
        else if (ratio <= 0.45f && !utils.hasScriptVar(self, "event.halloween_fourth_ratio"))
        {
            triggerFourthRatioEvent(self, attacker);
        }
        else if (ratio <= 0.25f && !utils.hasScriptVar(self, "event.halloween_fifth_ratio"))
        {
            triggerFifthRatioEvent(self);
        }
        else if (ratio <= 0.10f && !utils.hasScriptVar(self, "event.halloween_final_ratio"))
        {
            triggerFinalRatioEvent(self, attacker);
        }
    }

    private void triggerFirstRatioEvent(obj_id self, obj_id attacker) throws InterruptedException
    {
        chat.chat(self, "RAAAAAAAAAAAAAAAAH!");
        utils.setScriptVar(self, "event.halloween_first_ratio", 1);
        createCircleSpawn(self, self, RING_EVENT_ONE, RING_EVENT_NUM_MOBS, RING_EVENT_MOB_RANGE);
    }

    private void triggerSecondRatioEvent(obj_id self, obj_id attacker) throws InterruptedException
    {
        chat.chat(self, " ABUBB NEED MONIES " + getName(attacker) + "!");
        broadcast(attacker, "Frankenabubb has stomped the ground so hard it shakes 1,000 credits from your pockets!");
        if (money.hasFunds(attacker, money.MT_TOTAL, 1000))
        {
            money.withdraw(attacker, 1000);
        }
        sendConsoleCommand("/prone", attacker);
        utils.setScriptVar(self, "event.halloween_second_ratio", 1);
    }

    private void triggerThirdRatioEvent(obj_id self, obj_id attacker) throws InterruptedException
    {
        chat.chat(self, "SKELLY PEOPLE COME HELP ABUBB!");
        spawnSkellyGuys(self);
        utils.setScriptVar(self, "event.halloween_third_ratio", 1);
    }

    private void triggerFourthRatioEvent(obj_id self, obj_id attacker) throws InterruptedException
    {
        chat.chat(self, "ZOMBIE COME HELP ABUBB!");
        spawnSidekick(self, RING_EVENT_TWO, SIDEKICK_BUFF, 1.0f, 2.5f);
        utils.setScriptVar(self, "event.halloween_fourth_ratio", 1);
    }

    private void triggerFifthRatioEvent(obj_id self) throws InterruptedException
    {
        chat.chat(self, "ABUBB NEED MORE HELP!");
        spawnSidekick(self, SIDEKICK_TEMPLATE_TWO, SIDEKICK_BUFF, 1.0f, 40.0f);
        utils.setScriptVar(self, "event.halloween_fifth_ratio", 1);
    }

    private void triggerFinalRatioEvent(obj_id self, obj_id attacker) throws InterruptedException
    {
        if (!buff.hasBuff(self, FINAL_BUFF))
        {
            chat.chat(self, "ABUBB LAST STAND! ABUBB STRONGEST!");
            buff.applyBuff(self, FINAL_BUFF, FINAL_BUFF_DURATION, FINAL_BUFF_POWER);
            utils.setScriptVar(self, "event.halloween_final_ratio", 1);
        }
    }

    public void spawnSidekick(obj_id self, String creature, String buffName, float dur, float scale) throws InterruptedException
    {
        obj_id sidekick = create.object(creature, getLocation(self));
        setScale(sidekick, scale);
        buff.applyBuff(sidekick, buffName, dur);
        LOG("events", "GMF Frankenabubb - Summoning sidekick!");
        chat.chat(sidekick, "Utinni!");
    }

    public int aiCorpsePrepared(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id[] players = getAllPlayers(getLocation(self), 128.0f);
        if (players == null || players.length == 0)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id[] validPlayers = station_lib.processPlayerListAndRemoveDuplicates(players);
        if (validPlayers == null)
        {
            return SCRIPT_CONTINUE;
        }
        for (obj_id player : validPlayers)
        {
            obj_id inv = utils.getInventoryContainer(player);
            if (inv == null)
            {
                continue;
            }
            static_item.createNewItemFunction(LOOT_DROP_TEMPLATE, inv);
            broadcast(player, "You have received a loot drop from Frankenabubb!");
        }
        return SCRIPT_CONTINUE;
    }

    private void startFog(obj_id device)
    {
        stopClientEffectObjByLabel(device, "halloweenFog");
        playClientEffectObj(device, "clienteffect/halloween_fog_machine.cef", device, "", null, "halloweenFog");
        playClientEffectObj(device, "clienteffect/halloween_fog_machine.cef", device, "", null, "halloweenFog");
        playClientEffectObj(device, "clienteffect/halloween_fog_machine.cef", device, "", null, "halloweenFog");
        messageTo(device, "continueFog", null, 18.0f, false);
    }

    public void stopFog(obj_id device)
    {
        stopClientEffectObjByLabel(device, "halloweenFog");
    }

    public int continueFog(obj_id self, dictionary params)
    {
        if (hasObjVar(self, "fogOn"))
        {
            playClientEffectObj(self, "clienteffect/halloween_fog_machine.cef", self, "", null, "halloweenFog");
            messageTo(self, "continueFog", null, 18.0f, false);
        }
        return SCRIPT_CONTINUE;
    }
}