package script.player;

import script.library.bot_lib;
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
        if (params == null || params.trim().isEmpty())
        {
            broadcast(self, "[Bot]: No parameters supplied. Usage: /botExecute <characterFirstName> <passkey> <command and args>");
            return SCRIPT_CONTINUE;
        }

        StringTokenizer st = new StringTokenizer(params);
        if (!st.hasMoreTokens())
        {
            broadcast(self, "[Bot]: No parameters supplied.");
            return SCRIPT_CONTINUE;
        }
        String playerName = st.nextToken();
        if (!st.hasMoreTokens())
        {
            broadcast(self, "[Bot]: Passkey required.");
            return SCRIPT_CONTINUE;
        }
        String passkey = st.nextToken();
        StringBuilder commandRest = new StringBuilder();
        while (st.hasMoreTokens())
        {
            if (commandRest.length() > 0)
            {
                commandRest.append(' ');
            }
            commandRest.append(st.nextToken());
        }
        String fullCommand = commandRest.toString().trim();
        if (fullCommand.isEmpty())
        {
            broadcast(self, "[Bot]: No command after passkey.");
            return SCRIPT_CONTINUE;
        }

        obj_id player = getPlayerIdFromFirstName(toLower(playerName));
        if (!isIdValid(player))
        {
            broadcast(self, "Player " + playerName + " not found!");
            return SCRIPT_CONTINUE;
        }

        if (!isGod(self) && (passkey.isEmpty() || !hasObjVar(player, "challengeCode") || !passkey.equals(getStringObjVar(player, "challengeCode"))))
        {
            broadcast(self, "Invalid challenge code for " + getPlayerFullName(player) + "!");
            return SCRIPT_CONTINUE;
        }

        String firstToken = fullCommand.split("\\s+")[0];
        if (firstToken.equalsIgnoreCase("botReset")
                || firstToken.equalsIgnoreCase("botSetup")
                || firstToken.equalsIgnoreCase("botExecute")
                || firstToken.equalsIgnoreCase("botCtsApply"))
        {
            broadcast(self, "[Bot]: Invalid or illegal command.");
            return SCRIPT_CONTINUE;
        }

        sendConsoleCommand("/" + fullCommand, player);
        broadcast(self, "[Bot]: /" + fullCommand + " → " + getPlayerFullName(player));
        return SCRIPT_CONTINUE;
    }

    /**
     * Apply a Character Transfer snapshot from one live character to another (CTS upload → download pipeline).
     * Usage: /botCtsApply &lt;targetFirstName&gt; &lt;passkey&gt; &lt;sourceFirstName&gt;
     */
    public int cmdBotCtsApply(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (params == null || params.trim().isEmpty())
        {
            broadcast(self, "Usage: /botCtsApply <targetFirstName> <passkey> <sourceFirstName>");
            return SCRIPT_CONTINUE;
        }
        StringTokenizer st = new StringTokenizer(params);
        if (st.countTokens() < 3)
        {
            broadcast(self, "Usage: /botCtsApply <targetFirstName> <passkey> <sourceFirstName>");
            return SCRIPT_CONTINUE;
        }
        String targetName = st.nextToken();
        String passkey = st.nextToken();
        String sourceName = st.nextToken();

        obj_id bot = getPlayerIdFromFirstName(toLower(targetName));
        obj_id source = getPlayerIdFromFirstName(toLower(sourceName));
        if (!isIdValid(bot))
        {
            broadcast(self, "Target character '" + targetName + "' not found.");
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(source))
        {
            broadcast(self, "Source character '" + sourceName + "' not found.");
            return SCRIPT_CONTINUE;
        }
        if (!isGod(self) && (!hasObjVar(bot, "challengeCode") || !passkey.equals(getStringObjVar(bot, "challengeCode"))))
        {
            broadcast(self, "Invalid challenge code for target " + getPlayerFullName(bot) + ".");
            return SCRIPT_CONTINUE;
        }

        byte[] packed = bot_lib.packCharacterTransferData(source, true, true);
        if (packed == null || packed.length == 0)
        {
            broadcast(self, "CTS pack failed (OnUploadCharacter). Check source character CTS/skill state and logs.");
            return SCRIPT_CONTINUE;
        }
        if (!bot_lib.applyCharacterTransferData(bot, packed))
        {
            broadcast(self, "CTS apply failed (OnDownloadCharacter).");
            return SCRIPT_CONTINUE;
        }
        broadcast(self, "CTS snapshot applied: " + getPlayerFullName(source) + " → " + getPlayerFullName(bot));
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