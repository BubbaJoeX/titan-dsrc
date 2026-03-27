package script.conversation;

import script.conversation.base.ConvoResponse;
import script.library.ai_lib;
import script.library.companion_lib;
import script.library.utils;
import script.obj_id;
import script.string_id;

/**
 * Generic hire dialog for any {@code story_companions} row that does not use a bespoke script (e.g. {@link script.conversation.companion_greeata}).
 * The NPC must have {@link script.library.companion_lib#OBJVAR_HIRE_COMPANION_ID} set to the story id (done by {@link script.library.companion_lib#applyMakeHireableToNpc}
 * or by hand on world spawns).
 */
public class companion_common_hire extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.companion_common_hire";
    public String scriptName = "companion_common_hire";
    public companion_common_hire()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
    }
    private String getHireStoryId(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, companion_lib.OBJVAR_HIRE_COMPANION_ID))
        {
            return null;
        }
        return getStringObjVar(self, companion_lib.OBJVAR_HIRE_COMPANION_ID);
    }
    private ConvoResponse[] mainMenu() throws InterruptedException
    {
        return new ConvoResponse[]
        {
            convo("hire", "I'd like you on my crew."),
            convo("about", "What do you do around here?"),
            convo("how_are_you", "How are things?"),
            convo("goodbye", "Never mind.")
        };
    }
    private int openMainHub(obj_id player, obj_id self, String storyId) throws InterruptedException
    {
        String displayName = companion_lib.getStoryCompanionDisplayName(storyId);
        if (displayName == null)
        {
            displayName = storyId;
        }
        return serverSide_startConversation(
            player,
            self,
            displayName + " gives you a cautious once-over. \"Everyone's selling something. " +
            "If you're offering work and a way off this rock when it goes bad, I'm listening. What's the play?\"",
            1,
            mainMenu());
    }
    @Override
    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }
        String storyId = getHireStoryId(self);
        if (storyId == null || storyId.length() < 1)
        {
            return serverSide_endConversation(player, "They don't seem to be looking for work.");
        }
        if (!companion_lib.isValidStoryCompanionRow(storyId))
        {
            return serverSide_endConversation(player, "Something's wrong with this hire offer.");
        }
        return openMainHub(player, self, storyId);
    }
    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(scriptName))
        {
            return SCRIPT_CONTINUE;
        }
        String storyId = getHireStoryId(self);
        if (storyId == null || storyId.length() < 1)
        {
            utils.removeScriptVar(player, conversation + ".branchId");
            return SCRIPT_CONTINUE;
        }
        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");
        if (branchId == 1)
        {
            if (responseIdIs(response, "hire"))
            {
                return handleHire(player, self, storyId);
            }
            if (responseIdIs(response, "about"))
            {
                return serverSide_respond(
                    player,
                    "\"Same as anyone with sense--stay fed, stay quiet when the wrong people are listening, " +
                    "and keep a bag packed. If you're offering something steadier than that, say it.\"",
                    2,
                    new ConvoResponse[]
                    {
                        convo("hub", "Back to the other topics."),
                        convo("goodbye", "Goodbye.")
                    });
            }
            if (responseIdIs(response, "how_are_you"))
            {
                return handleHowAreYou(player, self, storyId);
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "\"Suit yourself. Doors stay open until they don't.\"");
            }
        }
        if (branchId == 2)
        {
            if (responseIdIs(response, "hub"))
            {
                return openMainHub(player, self, storyId);
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "\"Right.\"");
            }
        }
        if (branchId == 10)
        {
            if (responseIdIs(response, "hub"))
            {
                return openMainHub(player, self, storyId);
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "\"See you.\"");
            }
        }
        utils.removeScriptVar(player, conversation + ".branchId");
        return SCRIPT_CONTINUE;
    }
    private int handleHowAreYou(obj_id player, obj_id self, String storyId) throws InterruptedException
    {
        int inf = companion_lib.getInfluence(player, storyId);
        int tier = companion_lib.getInfluenceTier(inf);
        boolean recruited = companion_lib.playerOwnsStoryCompanion(player, storyId);
        String body;
        if (!recruited)
        {
            body = "\"Still breathing. Ask me again after we've actually shipped out together.\"";
        }
        else
        {
            body = "\"With you? Call it tier " + tier + " regard at " + inf + " influence--enough to mean we're not strangers.\"";
        }
        return serverSide_respond(
            player,
            body,
            10,
            new ConvoResponse[]
            {
                convo("hub", "Let's talk about something else."),
                convo("goodbye", "I'll leave you to it.")
            });
    }
    private int handleHire(obj_id player, obj_id self, String storyId) throws InterruptedException
    {
        if (companion_lib.playerOwnsStoryCompanion(player, storyId))
        {
            return serverSide_endConversation(
                player,
                "\"Already on your roster--pull me out of the datapad when you need me.\"");
        }
        obj_id cd = companion_lib.grantStoryCompanionToDatapad(player, storyId);
        if (!isIdValid(cd) || !exists(cd))
        {
            return serverSide_endConversation(
                player,
                "\"I'd sign on, but something's blocking it--datapad full, pet limits, or the galaxy's just saying not today.\"");
        }
        companion_lib.modifyInfluence(player, storyId, 40);
        return serverSide_endConversation(
            player,
                "\"All right. I'm on your datapad. Don't make me regret it.\"");
    }
}
