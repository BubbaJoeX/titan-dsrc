package script.player;

import script.library.sui;
import script.obj_id;
import script.dictionary;

import java.util.StringTokenizer;

public class player_drag extends script.base_script
{
    public int OnAttach(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public String[] BAD_COMMANDS = {
            "quit",
            "logout",
    };

    public int handleChallengeCode(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isIdValid(self) || params == null)
        {
            return SCRIPT_CONTINUE;
        }

        obj_id target = sui.getPlayerId(params);
        if (!isIdValid(target))
        {
            return SCRIPT_CONTINUE;
        }

        String challengeCode = sui.getInputBoxText(params);
        if (challengeCode == null || challengeCode.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, "challengeCode", challengeCode);
        broadcast(self, "Challenge Code set to: " + challengeCode);
        return SCRIPT_CONTINUE;
    }

    public int cmdBotExecute(obj_id self, obj_id target, String params, float defaultTime)
    {
        if (params == null || params.isEmpty())
        {
            broadcast(self, "[Bot]: No parameters supplied.");
            return SCRIPT_CONTINUE;
        }

        String[] paramsArray = split(params, ' ');
        String playerName = paramsArray[0];
        String passkey = paramsArray.length > 1 ? paramsArray[1] : "";
        String commandToSend = paramsArray.length > 2 ? paramsArray[2] : "";
        StringBuilder commandParameters = new StringBuilder();

        for (int i = 3; i < paramsArray.length; i++)
        {
            commandParameters.append(paramsArray[i]).append(" ");
        }

        obj_id player = getPlayerIdFromFirstName(toLower(playerName));
        if (!isIdValid(player))
        {
            broadcast(self, "Player " + playerName + " not found!");
            return SCRIPT_CONTINUE;
        }

        if (passkey.isEmpty() || (!passkey.equals(getStringObjVar(player, "challengeCode")) && !isGod(self)))
        {
            broadcast(self, "Invalid challenge code for " + getPlayerFullName(player) + "!");
            return SCRIPT_CONTINUE;
        }

        if (commandToSend.contains("botReset") || commandToSend.contains("botSetup") || commandToSend.contains("botExecute"))
        {
            broadcast(self, "[Bot]: Invalid or illegal command.");
            return SCRIPT_CONTINUE;
        }

        sendConsoleCommand("/" + commandToSend, player);
        broadcast(self, "[Bot]: Command " + commandToSend + " sent to " + getPlayerFullName(player));
        return SCRIPT_CONTINUE;
    }

    public int cmdBotSetup(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (!hasObjVar(self, "challengeCode"))
        {
            setupChallengeCode(self, "");
        }
        else
        {
            LOG("ethereal", "[Drag]: cmdBotSetup: challengeCode already set.");
            broadcast(self, "[Bot]: Challenge Code already set. Use /botReset to reset it.");
        }
        return SCRIPT_CONTINUE;
    }

    public int cmdBotReset(obj_id self, obj_id target, String params, float defaultTime)
    {
        if (hasObjVar(self, "challengeCode"))
        {
            removeObjVar(self, "challengeCode");
        }
        broadcast(self, "[Bot]: Challenge Code Reset. Use /botSetup to set a new one.");
        return SCRIPT_CONTINUE;
    }

    public void setupChallengeCode(obj_id self, String challengeCode) throws InterruptedException
    {
        if (!isIdValid(self) || challengeCode == null || challengeCode.isEmpty())
        {
            return;
        }

        String prompt = "Enter a challenge code for players to use to verify that you allowed this character to be controlled via commands.";
        String box_title = "Challenge Code";
        int page = sui.inputbox(self, self, prompt, box_title, "handleChallengeCode", 15, false, challengeCode);
        setSUIProperty(page, sui.INPUTBOX_INPUT, sui.PROP_LOCALTEXT, challengeCode);
    }
}