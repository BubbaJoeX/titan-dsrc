package script.player;/*
@Origin: script.player.
@Author: BubbaJoeX
@Purpose: If player is in a anti-afk volume,
@Notes:
@Created: Saturday, 2/9/2025, at 9:45 PM,
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.dictionary;
import script.obj_id;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class player_anti_afk extends script.base_script
{
    private static final String VOLUME = "anti_macro_volume";
    private static final int CHECK_INTERVAL = 120;
    private static final int MAX_MACRO_FLUSH_COUNT = 10;

    private final HashMap<obj_id, Integer> playerMacroFlushCount = new HashMap<>();

    public int OnAttach(obj_id self)
    {
        messageTo(self, "checkAfkStatus", null, CHECK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        messageTo(self, "checkAfkStatus", null, CHECK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    public int OnTriggerVolumeEntered(obj_id self, String name, obj_id who) throws InterruptedException
    {
        if (!name.equals(VOLUME))
        {
            return SCRIPT_CONTINUE;
        }
        handleMacroTermination(self, who);
        return SCRIPT_CONTINUE;
    }

    public int OnTriggerVolumeExited(obj_id self, String name, obj_id who) throws InterruptedException
    {
        if (!name.equals(VOLUME))
        {
            return SCRIPT_CONTINUE;
        }
        handleMacroTermination(self, who);
        return SCRIPT_CONTINUE;
    }

    private final HashMap<obj_id, Boolean> messageScheduled = new HashMap<>();

    public int checkAfkStatus(obj_id self, dictionary params) throws InterruptedException
    {
        return SCRIPT_CONTINUE;/*
        obj_id[] playersInVolume = getPlayersInVolume(self, VOLUME);

        if (playersInVolume.length == 0) // If no players are in the anti-AFK volume
        {
            return SCRIPT_CONTINUE; // Continue the script without doing anything
        }

        for (obj_id player : playersInVolume)
        {
            if (isPlayer(player))
            {
                int flushCount = playerMacroFlushCount.getOrDefault(player, 0);
                flushCount++;
                playerMacroFlushCount.put(player, flushCount);

                // Dump macros
                handleMacroTermination(self, player);

                // Check if flush count exceeds the limit
                if (flushCount >= MAX_MACRO_FLUSH_COUNT)
                {
                    // Ensure the message is only sent once
                    if (!messageScheduled.getOrDefault(player, false)) {
                        broadcast(player, "You have been warned for excessive AFK behavior. Continued abuse may result in disconnection.");
                        messageScheduled.put(player, true);  // Mark the message as scheduled for this player
                        LOG("ethereal", "[Anti-AFK] " + getName(player) + " has reached the max flush count and has been warned.");
                    }
                }
            }
        }

        // Re-run the check every 2 minutes
        messageTo(self, "checkAfkStatus", null, CHECK_INTERVAL, true);
        return SCRIPT_CONTINUE;*/
    }


    private obj_id[] getPlayersInVolume(obj_id self, String volume)
    {
        obj_id[] contents = getTriggerVolumeContents(self, volume);
        obj_id[] playersInVolume = new obj_id[contents.length];
        int playerCount = 0;

        for (obj_id obj : contents)
        {
            if (isPlayer(obj))  // Only add players to the array
            {
                playersInVolume[playerCount] = obj;
                playerCount++;
            }
        }

        // Resize the array to the actual number of players found
        obj_id[] finalPlayersInVolume = new obj_id[playerCount];
        System.arraycopy(playersInVolume, 0, finalPlayersInVolume, 0, playerCount);

        return finalPlayersInVolume;
    }

    private void handleMacroTermination(obj_id self, obj_id who)
    {
        if (isPlayer(who))
        {
            broadcast(who, "Your macros have been dumped.");
            sendConsoleCommand("/dumpPausedCommands", who);
            LOG("ethereal", "[Anti-AFK] " + getName(who) + " has had their macros turned off by " + self);
        }
    }
}
