package script.library;/*
@Origin: dsrc.script.library
@Author:  BubbaJoeX
@Purpose: Station ID related checks
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Friday, 7/5/2024, at 4:28 PM, 
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.obj_id;
import script.location;
import script.menu_info_types;
import script.menu_info_data;
import script.menu_info;
import script.dictionary;

import java.util.*;

public class station_lib extends script.base_script
{
    /*
    @Note: Check to see if two players are on the same account.
     */
    public static boolean isSameAccount(obj_id suspect, obj_id otherPerson)
    {
        return getPlayerStationId(suspect) == getPlayerStationId(otherPerson);
    }

    /*
    @Note: Test function: Log the number of players that share a station id based on obj_id array.
     */
    public static int getMultipleClientCount(obj_id source, obj_id[] playersToCheck)
    {
        int counter = 0;
        int sourceId = getPlayerStationId(source);
        for (obj_id pCreature : playersToCheck)
        {
            if (pCreature == source)
            {
                continue;
            }
            if (getPlayerStationId(pCreature) == sourceId)
            {
                counter++;
            }
        }
        LOG("ethereal", "[MA-C]: Found " + counter + " duplicate connections from single account. From: " + source.toString());
        return SCRIPT_CONTINUE;
    }

    public static boolean shouldLog = true;

    /*
    @Note: Returns the list of player's station ids from a group object.
     */
    public static String[] getGroupStationIds(obj_id groupLeader)
    {
        obj_id[] groupMembers = getGroupMemberIds(group.getGroupObject(groupLeader));
        String[] stationIds = new String[groupMembers.length];

        for (int i = 0; i < groupMembers.length; i++)
        {
            stationIds[i] = String.valueOf(getPlayerStationId(groupMembers[i]));
        }

        return stationIds;
    }

    public static String[] removeDuplicateStationIds(String[] stationIdList)
    {
        Set<String> uniqueStationIds = new HashSet<>(Arrays.asList(stationIdList));
        return uniqueStationIds.toArray(new String[0]);
    }

    /*
    @Note: Gets a list of players from any obj_id[] function (n this case "getGroupMemberIds(group.getGroupObject(groupLeader))")  and returns a list of players to only appear once by station id.
     */
    public static obj_id[] processGroupListAndRemoveDuplicateByStationId(obj_id[] groupList)
    {
        Map<String, obj_id> uniqueMembers = new HashMap<>();

        for (obj_id member : groupList)
        {
            String stationId = String.valueOf(getPlayerStationId(member));
            if (!uniqueMembers.containsKey(stationId))
            {
                uniqueMembers.put(stationId, member);
            }
        }
        return uniqueMembers.values().toArray(new obj_id[0]);
    }

    /*
    @Note: Gets a list of players from any obj_id[] function and returns a list of players to only appear once by station id.
     */
    public static obj_id[] processPlayerListAndRemoveDuplicates(obj_id[] players)
    {
        Map<String, obj_id> uniqueMembers = new HashMap<>();

        for (obj_id member : players)
        {
            String stationId = String.valueOf(getPlayerStationId(member));
            if (!uniqueMembers.containsKey(stationId))
            {
                uniqueMembers.put(stationId, member);
            }
            else
            {
                if (isIdValid(member))
                {
                    broadcast(member, "This character was not eligible for this encounter.");
                }
            }
        }
        if (shouldLog)
        {
            LOG("ethereal", "[MA-C]: Removed " + (players.length - uniqueMembers.size()) + " players from array.");
        }
        return uniqueMembers.values().toArray(new obj_id[0]);
    }
}
