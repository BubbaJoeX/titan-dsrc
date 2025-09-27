package script.library;/*
@Origin: dsrc.script.library
@Author:  BubbaJoeX
@Purpose: consts for new quests
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Monday, 9/9/2024, at 4:15 AM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.obj_id;

public class repeatables extends groundquests
{
    public static final String D_CRAFT_QUEST = "daily_craft";
    public static final String D_CRAFT_SIGNAL = "dailyCrafts";
    public static final String D_RECON_QUEST = "daily_recon";
    public static final String D_RECON_SIGNAL = "dailyTravels";
    public static final String D_SLAYER_QUEST = "daily_slayer";
    public static final String D_SLAYER_SIGNAL = "dailySlays";
    public static final String W_CRAFT_SIGNAL = "weeklyCrafts"; //@TODO: Add weekly craft quest
    public static final String W_RECON_SIGNAL = "weeklyTravels"; //@TODO: Add weekly recon quest
    public static final String W_SLAYER_QUEST = "weekly_slayer";
    public static final String W_SLAYER_SIGNAL = "weeklySlays";
    public static final int DAILY_REPEAT_TIME = 86400; // 24 hours
    public static final int D_TOKEN_REWARD_AMOUNT = 2; // 24 hours
    public static final int W_TOKEN_REWARD_AMOUNT = 8; // 24 hours
    public static final int WEEKLY_REPEAT_TIME = DAILY_REPEAT_TIME * 7; // 24 hours

    public static void grantDailyCraft(obj_id player) throws InterruptedException
    {
        if (hasCompletedQuest(player, D_CRAFT_QUEST))
        {
            grantQuest(player, D_CRAFT_QUEST);
            setNextRepeatTime(player, D_CRAFT_QUEST, DAILY_REPEAT_TIME);
        }
    }

    public static void grantDailyRecon(obj_id player) throws InterruptedException
    {
        if (hasCompletedQuest(player, D_RECON_QUEST))
        {
            grantQuest(player, D_CRAFT_QUEST);
            setNextRepeatTime(player, D_RECON_QUEST, DAILY_REPEAT_TIME);
        }
    }

    public static void grantDailySlayer(obj_id player) throws InterruptedException
    {
        if (hasCompletedQuest(player, D_SLAYER_QUEST))
        {
            grantQuest(player, D_SLAYER_QUEST);
            setNextRepeatTime(player, D_SLAYER_QUEST, DAILY_REPEAT_TIME);
        }
    }

    public static void updateDailyCraft(obj_id player) throws InterruptedException
    {
        if (isTaskActive(player, D_CRAFT_QUEST, D_CRAFT_SIGNAL))
        {
            sendSignal(player, D_CRAFT_SIGNAL);
        }
    }

    public static void updateDailyRecon(obj_id player) throws InterruptedException
    {
        if (isTaskActive(player, D_RECON_QUEST, D_RECON_SIGNAL))
        {
            sendSignal(player, D_RECON_SIGNAL);
        }
    }

    public static void updateDailySlayer(obj_id player) throws InterruptedException
    {
        if (isTaskActive(player, D_SLAYER_QUEST, D_SLAYER_SIGNAL))
        {
            sendSignal(player, D_SLAYER_SIGNAL);
        }
    }

    public static void updateWeeklySlayer(obj_id player) throws InterruptedException
    {
        if (isTaskActive(player, W_SLAYER_QUEST, W_SLAYER_SIGNAL))
        {
            sendSignal(player, W_SLAYER_SIGNAL);
        }
    }

    public static boolean canRepeatQuest(obj_id player, String questName) throws InterruptedException
    {
        int checkerLevel = 0;
        if (!isQuestActive(player, questName))
        {
            checkerLevel++;
        }
        if (getCalendarTime() >= getIntObjVar(player, "repeatables." + questName + ".next")) //checks if the time is greater than the next time
        {
            checkerLevel++;
        }
        return checkerLevel == 2;
    }

    public static void setNextRepeatTime(obj_id player, String questName, int time)
    {
        //@TODO: split weekly and daily repeat times
        setObjVar(player, "repeatables." + questName + ".next", getCalendarTime() + DAILY_REPEAT_TIME);
    }

    public static void grantWeeklySlayer(obj_id player) throws InterruptedException
    {
        if (hasCompletedQuest(player, W_SLAYER_QUEST))
        {
            grantQuest(player, W_SLAYER_QUEST);
            setNextRepeatTime(player, W_SLAYER_QUEST, DAILY_REPEAT_TIME);
        }
    }

    public static void grantWeeklyCraft(obj_id player) throws InterruptedException
    {
        if (hasCompletedQuest(player, W_CRAFT_SIGNAL))
        {
            grantQuest(player, W_CRAFT_SIGNAL);
            setNextRepeatTime(player, W_CRAFT_SIGNAL, DAILY_REPEAT_TIME);
        }
    }

    public static void grantWeeklyRecon(obj_id player) throws InterruptedException
    {
        if (hasCompletedQuest(player, W_RECON_SIGNAL))
        {
            grantQuest(player, W_RECON_SIGNAL);
            setNextRepeatTime(player, W_RECON_SIGNAL, DAILY_REPEAT_TIME);
        }
    }

    public static void updateWeeklyCraft(obj_id player) throws InterruptedException
    {
        if (isTaskActive(player, W_CRAFT_SIGNAL, W_CRAFT_SIGNAL))
        {
            sendSignal(player, W_CRAFT_SIGNAL);
        }
    }

    public static void updateWeeklyRecon(obj_id player) throws InterruptedException
    {
        if (isTaskActive(player, W_RECON_SIGNAL, W_RECON_SIGNAL))
        {
            sendSignal(player, W_RECON_SIGNAL);
        }
    }

    public static void grantRepeatable(obj_id player, String questName) throws InterruptedException
    {
        if (canRepeatQuest(player, questName))
        {
            grantQuest(player, questName);
            setNextRepeatTime(player, questName, DAILY_REPEAT_TIME);
        }
    }

    public static void grantWeekly(obj_id player, String questName) throws InterruptedException
    {
        if (canRepeatQuest(player, questName))
        {
            grantQuest(player, questName);
            setNextRepeatTime(player, questName, WEEKLY_REPEAT_TIME);
        }
    }
}
