package script.systems.city;

import script.*;
import script.library.*;

import java.util.Vector;

public class city_judge_election extends script.base_script
{
    public city_judge_election()
    {
    }

    public static final String ELECTION_VAR_ROOT = "city.judge_election";

    public static void startJudgeElection(int cityId) throws InterruptedException
    {
        obj_id cityHall = cityGetCityHall(cityId);
        if (!isIdValid(cityHall))
        {
            return;
        }

        setObjVar(cityHall, ELECTION_VAR_ROOT + ".active", 1);
        setObjVar(cityHall, ELECTION_VAR_ROOT + ".start_time", getGameTime());
        setObjVar(cityHall, ELECTION_VAR_ROOT + ".end_time", getGameTime() + city.JUDGE_ELECTION_DURATION);

        broadcastToCitizens(cityId, city.SID_JUDGE_ELECTION_STARTED);

        dictionary params = new dictionary();
        params.put("cityId", cityId);
        messageTo(cityHall, "handleJudgeElectionEnd", params, city.JUDGE_ELECTION_DURATION, false);

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Judge election started. City: " + cityName + " (" + cityId + ")");
    }

    public static void registerCandidate(obj_id citizen, int cityId) throws InterruptedException
    {
        if (!city.isCitizenOfCity(citizen, cityId))
        {
            sendSystemMessage(citizen, new string_id("city/city", "not_citizen"));
            return;
        }

        if (hasObjVar(citizen, "city.eviction.initiated_time"))
        {
            sendSystemMessage(citizen, city.SID_CANNOT_RUN_UNDER_EVICTION);
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);
        if (!hasObjVar(cityHall, ELECTION_VAR_ROOT + ".active"))
        {
            sendSystemMessage(citizen, new string_id("city/city", "no_election_active"));
            return;
        }

        obj_id[] candidates = getObjIdArrayObjVar(cityHall, ELECTION_VAR_ROOT + ".candidates");
        if (candidates == null)
        {
            candidates = new obj_id[0];
        }

        for (obj_id c : candidates)
        {
            if (c.equals(citizen))
            {
                sendSystemMessage(citizen, new string_id("city/city", "already_registered_candidate"));
                return;
            }
        }

        candidates = utils.addElement(candidates, citizen);
        setObjVar(cityHall, ELECTION_VAR_ROOT + ".candidates", candidates);

        sendSystemMessage(citizen, city.SID_REGISTERED_AS_JUDGE_CANDIDATE);

        String cityName = cityGetName(cityId);
        String citizenName = cityGetCitizenName(cityId, citizen);
        CustomerServiceLog("player_city", "Judge candidate registered. City: " + cityName + " Citizen: " + citizenName);
    }

    public static void castVote(obj_id voter, obj_id candidate, int cityId) throws InterruptedException
    {
        if (!city.isCitizenOfCity(voter, cityId))
        {
            return;
        }

        obj_id cityHall = cityGetCityHall(cityId);
        if (!hasObjVar(cityHall, ELECTION_VAR_ROOT + ".active"))
        {
            sendSystemMessage(voter, new string_id("city/city", "no_election_active"));
            return;
        }

        obj_id[] hasVoted = getObjIdArrayObjVar(cityHall, ELECTION_VAR_ROOT + ".voted");
        if (hasVoted != null)
        {
            for (obj_id v : hasVoted)
            {
                if (v.equals(voter))
                {
                    sendSystemMessage(voter, city.SID_ALREADY_VOTED);
                    return;
                }
            }
        }

        obj_id[] candidates = getObjIdArrayObjVar(cityHall, ELECTION_VAR_ROOT + ".candidates");
        if (candidates == null)
        {
            return;
        }

        boolean isValidCandidate = false;
        for (obj_id c : candidates)
        {
            if (c.equals(candidate))
            {
                isValidCandidate = true;
                break;
            }
        }

        if (!isValidCandidate)
        {
            sendSystemMessage(voter, new string_id("city/city", "invalid_candidate"));
            return;
        }

        String voteKey = ELECTION_VAR_ROOT + ".votes." + candidate;
        int currentVotes = getIntObjVar(cityHall, voteKey);
        setObjVar(cityHall, voteKey, currentVotes + 1);

        if (hasVoted == null)
        {
            hasVoted = new obj_id[0];
        }
        hasVoted = utils.addElement(hasVoted, voter);
        setObjVar(cityHall, ELECTION_VAR_ROOT + ".voted", hasVoted);

        sendSystemMessage(voter, city.SID_VOTE_RECORDED);
    }

    public static void finalizeElection(int cityId) throws InterruptedException
    {
        obj_id cityHall = cityGetCityHall(cityId);
        if (!isIdValid(cityHall))
        {
            return;
        }

        obj_id[] candidates = getObjIdArrayObjVar(cityHall, ELECTION_VAR_ROOT + ".candidates");

        if (candidates == null || candidates.length == 0)
        {
            removeObjVar(cityHall, ELECTION_VAR_ROOT);
            return;
        }

        int maxJudges = city.getMaxJudgeCount(cityId);

        Vector candidateVotes = new Vector();
        for (obj_id candidate : candidates)
        {
            String voteKey = ELECTION_VAR_ROOT + ".votes." + candidate;
            int votes = getIntObjVar(cityHall, voteKey);
            candidateVotes.add(new Object[]{candidate, votes});
        }

        candidateVotes.sort((a, b) -> {
            int votesA = (Integer)((Object[])a)[1];
            int votesB = (Integer)((Object[])b)[1];
            return votesB - votesA;
        });

        obj_id[] currentJudges = city.getCityJudges(cityId);
        for (obj_id judge : currentJudges)
        {
            city.removeJudge(cityId, judge);
        }

        int judgesAdded = 0;
        for (int i = 0; i < candidateVotes.size() && judgesAdded < maxJudges; i++)
        {
            Object[] entry = (Object[])candidateVotes.get(i);
            obj_id winner = (obj_id)entry[0];
            int votes = (Integer)entry[1];

            if (votes > 0)
            {
                city.addJudge(cityId, winner);
                setObjVar(winner, "city.judge.votes_received", votes);
                judgesAdded++;
            }
        }

        removeObjVar(cityHall, ELECTION_VAR_ROOT);

        broadcastToCitizens(cityId, city.SID_JUDGE_ELECTION_COMPLETE);

        String cityName = cityGetName(cityId);
        CustomerServiceLog("player_city", "Judge election completed. City: " + cityName + " Judges elected: " + judgesAdded);
    }

    public static void broadcastToCitizens(int cityId, string_id message) throws InterruptedException
    {
        obj_id[] citizens = cityGetCitizenIds(cityId);
        if (citizens == null)
        {
            return;
        }

        for (obj_id citizen : citizens)
        {
            if (isIdValid(citizen))
            {
                sendSystemMessage(citizen, message);
            }
        }
    }

    public int handleJudgeElectionEnd(obj_id self, dictionary params) throws InterruptedException
    {
        int cityId = params.getInt("cityId");
        finalizeElection(cityId);
        return SCRIPT_CONTINUE;
    }
}

