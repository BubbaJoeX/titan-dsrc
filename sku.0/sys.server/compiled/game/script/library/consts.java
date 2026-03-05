package script.library;/*
@Origin: dsrc.script.library
@Author: BubbaJoeX
@Purpose: Repo for variables
@Note: Update as needed to reflect content changes.
@Requirements: <no requirements>
@Created: Wednesday, 11/15/2023, at 9:38 AM, 
@Copyright © SWG: TItan 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

public class consts extends script.base_script
{

    public static final String FROG_NAME = "Mystical Frog";
    public static final String FROG_PROMPT = "Greetings, tester. How may I service you?";
    public static final String FROG_CUE = "sound/item_ding.snd";
    public static final int MAX_NAME_LENGTH = 128;
    public static final int MAX_TITLE_LENGTH = 128;
    public static final float MAXIMUM_PLAYER_SEARCH_DISTANCE = 128.0f;
    public static final float MAXIMUM_CREATURE_SEARCH_DISTANCE = 64.0f;
    public static final int PLAYER_LEVEL = 90;
    public static final int MAXIMUM_CREDITS = 10000000;
    public static final int MAXIMUM_BANK_CREDITS = 1000000000;
    public static final int MAXIMUM_INVENTORY = 80;
    public static final int MAXIMUM_RESOURCE_DISBERSEMENT = 100500;
    public static final int MAXIMUM_FACTION_DISBERSEMENT = 5000;
    public static final float AI_SERVICE_RANGE = 3.24f;
    public static final float AI_SERVICE_DURATION = 3600f;

    // TangibleDynamics conditions
    public static final int CONDITION_MAGIC_TANGIBLE_DYNAMIC = 0x80000000;

    public static String color(String html, String message)
    {
        return "\\#" + html + "" + message + "\\#.";
    }

    public static String stripColor(String message)
    {
        return message.replaceAll("#\\w{6}", "");
    }

}
