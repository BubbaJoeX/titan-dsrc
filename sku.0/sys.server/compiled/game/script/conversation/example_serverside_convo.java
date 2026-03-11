package script.conversation;

import script.library.ai_lib;
import script.library.utils;
import script.obj_id;
import script.string_id;

/**
 * Example server-side conversation script demonstrating the new STF-less conversation system.
 * This conversation does not require any .stf string files.
 *
 * To use this system in your own conversations:
 * 1. Extend conversation_base (as usual)
 * 2. Use serverSide_startConversation() instead of npcStartConversation()
 * 3. Use serverSide_respond() instead of npcSpeak()/npcSetConversationResponses()
 * 4. Use serverSide_endConversation() instead of npcEndConversationWithMessage()
 * 5. Use responseIdIs() or responseIs() to match responses in OnNpcConversationResponse
 *
 * Response IDs should be unique within a branch for proper matching.
 */
public class example_serverside_convo extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.example_serverside_convo";
    public String scriptName = "example_serverside_convo";

    public example_serverside_convo()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
    }

    @Override
    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }

        // Start with a greeting and offer response options
        return serverSide_startConversation(
            player,
            self,
            "Hello there, traveler! What brings you to these parts?",
            1, // branchId
            new ConvoResponse[] {
                convo("ask_quest", "Do you have any work for me?"),
                convo("ask_info", "I'm just looking around."),
                convo("goodbye", "Nothing, I'll be on my way.")
            }
        );
    }

    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(scriptName))
        {
            return SCRIPT_CONTINUE;
        }

        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");

        // Branch 1: Initial greeting
        if (branchId == 1)
        {
            if (responseIdIs(response, "ask_quest"))
            {
                return handleAskQuest(player, self);
            }
            if (responseIdIs(response, "ask_info"))
            {
                return handleAskInfo(player, self);
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "Safe travels, friend!");
            }
        }

        // Branch 2: Quest dialog
        if (branchId == 2)
        {
            if (responseIdIs(response, "accept"))
            {
                // Player accepted the quest - give reward, set flag, etc.
                return serverSide_endConversation(player,
                    "Excellent! Head to the old mill and report back when you're done.");
            }
            if (responseIdIs(response, "decline"))
            {
                return serverSide_endConversation(player,
                    "I understand. Come back if you change your mind.");
            }
        }

        // Branch 3: Info dialog
        if (branchId == 3)
        {
            if (responseIdIs(response, "more"))
            {
                return serverSide_respond(
                    player,
                    "The town was founded 200 years ago by settlers from the north. " +
                    "We've had our share of troubles, but we persevere.",
                    4, // new branch
                    new ConvoResponse[] {
                        convo("thanks", "Thanks for the history lesson."),
                        convo("quest", "Sounds like you could use some help.")
                    }
                );
            }
            if (responseIdIs(response, "nevermind"))
            {
                return serverSide_endConversation(player, "Alright then. Take care!");
            }
        }

        // Branch 4: Follow-up from info
        if (branchId == 4)
        {
            if (responseIdIs(response, "thanks"))
            {
                return serverSide_endConversation(player, "You're welcome! Enjoy your stay.");
            }
            if (responseIdIs(response, "quest"))
            {
                return handleAskQuest(player, self);
            }
        }

        // Fallback
        utils.removeScriptVar(player, conversation + ".branchId");
        return SCRIPT_CONTINUE;
    }

    private int handleAskQuest(obj_id player, obj_id self) throws InterruptedException
    {
        return serverSide_respond(
            player,
            "Actually, yes! There's been strange activity at the old mill. " +
            "Would you be willing to investigate?",
            2, // branchId
            new ConvoResponse[] {
                convo("accept", "Sure, I'll check it out."),
                convo("decline", "Sorry, I'm busy right now.")
            }
        );
    }

    private int handleAskInfo(obj_id player, obj_id self) throws InterruptedException
    {
        return serverSide_respond(
            player,
            "This is a quiet little town. We don't get many visitors.",
            3, // branchId
            new ConvoResponse[] {
                convo("more", "Tell me more about this place."),
                convo("nevermind", "I see. Well, goodbye.")
            }
        );
    }
}

