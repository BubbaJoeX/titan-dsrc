package script.conversation.v2;

import script.conversation.base.ConvoResponse;
import script.library.ai_lib;
import script.library.utils;
import script.library.conversation; // <-- assumes your wrapper lives here
import script.obj_id;
import script.string_id;

public class camera_test_convo extends script.conversation.base.conversation_base
{
    public String convoId  = "camera_test_convo";
    public String scriptName = "camera_test_convo";

    private static final int BR_MAIN = 1;

    public camera_test_convo()
    {
        super.scriptName = scriptName;
        super.conversation = convoId ;
    }

    public int OnAttach(obj_id self)
    {
        setName(self, "Cinematic Camera Tester");
        setInvulnerable(self, true);
        setCondition(self, CONDITION_CONVERSABLE);
        return SCRIPT_CONTINUE;
    }

    private ConvoResponse[] mainMenuResponses()
    {
        return new ConvoResponse[] {
            convo("look_self", "Look at NPC (self)"),
            convo("look_player", "Look at me (player)"),
            convo("look_self_short", "Look at NPC (2s hold)"),
            convo("look_player_slow", "Look at me (slow transition)"),
            convo("look_toggle", "Toggle between NPC and player"),
            convo("goodbye", "Done testing")
        };
    }

    @Override
    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }

        return serverSide_startConversation(
            player,
            self,
            "Camera test interface.\n\nSelect an option to move the conversation camera.",
            BR_MAIN,
            mainMenuResponses()
        );
    }

    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(scriptName))
        {
            return SCRIPT_CONTINUE;
        }

        if (responseIdIs(response, "goodbye"))
        {
            return serverSide_endConversation(player, "Ending camera test.");
        }

        if (responseIdIs(response, "look_self"))
        {
            script.library.conversation.npcConversationCameraLookAtTarget(player, self, 0f, 1.0f);
            return serverSide_respond(player, "Camera now looking at NPC (permanent).", BR_MAIN, mainMenuResponses());
        }

        if (responseIdIs(response, "look_player"))
        {
            script.library.conversation.npcConversationCameraLookAtTarget(player, player, 0f, 1.0f);
            return serverSide_respond(player, "Camera now looking at player (permanent).", BR_MAIN, mainMenuResponses());
        }

        if (responseIdIs(response, "look_self_short"))
        {
            script.library.conversation.npcConversationCameraLookAtTarget(player, self, 2.0f, 0.5f);
            return serverSide_respond(player, "Looking at NPC for 2 seconds, then returning.", BR_MAIN, mainMenuResponses());
        }

        if (responseIdIs(response, "look_player_slow"))
        {
            script.library.conversation.npcConversationCameraLookAtTarget(player, player, 0f, 3.0f);
            return serverSide_respond(player, "Slow transition to player view.", BR_MAIN, mainMenuResponses());
        }

        if (responseIdIs(response, "look_toggle"))
        {
            boolean toggle = utils.getBooleanScriptVar(player, this.convoId  + ".toggle");

            if (toggle)
            {
                script.library.conversation.npcConversationCameraLookAtTarget(player, self, 0f, 1.0f);
                utils.setScriptVar(player, this.convoId  + ".toggle", false);
                return serverSide_respond(player, "Toggled: now looking at NPC.", BR_MAIN, mainMenuResponses());
            }
            else
            {
                script.library.conversation.npcConversationCameraLookAtTarget(player, player, 0f, 1.0f);
                utils.setScriptVar(player, this.convoId  + ".toggle", true);
                return serverSide_respond(player, "Toggled: now looking at player.", BR_MAIN, mainMenuResponses());
            }
        }

        return SCRIPT_CONTINUE;
    }
}