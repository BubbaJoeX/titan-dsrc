package script.conversation;

import script.conversation.base.ConvoResponse;
import script.conversation.base.conversation_base;
import script.library.ai_lib;
import script.library.chat;
import script.library.groundquests;
import script.*;

/**
 * Flitz scrap-buyer quest line. Attach to a {@code object/mobile/jawa_male.iff} (or similar) at ~3254, 6, -4975, tatooine.
 * <p>
 * <b>Content setup:</b> {@code quest.task.ground.retrieve_item} needs world objects matching each task's
 * {@code SERVER_TEMPLATE} at the waypoint coordinates in {@code tatooine_flitz_salvage_0N.tab}. Spawn static tangibles or
 * place them in the buildout; then compile tabs → iff, refresh quest CRC, {@code reloadQuests} + {@code reloadTable}.
 */
public class tatooine_jawa_flitz extends conversation_base
{
    public static final String Q1 = "quest/tatooine_flitz_salvage_01";
    public static final String Q2 = "quest/tatooine_flitz_salvage_02";
    public static final String Q3 = "quest/tatooine_flitz_salvage_03";

    public tatooine_jawa_flitz()
    {
        conversation = "conversation.tatooine_jawa_flitz";
        scriptName = "conversation.tatooine_jawa_flitz";
    }

    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }
        faceTo(self, player);

        if (groundquests.isTaskActive(player, Q1, "flitzQ1_returnToFlitz"))
        {
            groundquests.sendSignal(player, "flitzReturnedQ1");
            return serverSide_endConversation(player, "Flitz snatches the salvage from your hands, eyes gleaming beneath the hood. \"Utinni! Good weight, good friend. Credits as promised.\"");
        }
        if (groundquests.isTaskActive(player, Q2, "flitzQ2_returnToFlitz"))
        {
            groundquests.sendSignal(player, "flitzReturnedQ2");
            return serverSide_endConversation(player, "\"The bottle—still sealed! Buyer pays extra this cycle. Here, take this.\"");
        }
        if (groundquests.isTaskActive(player, Q3, "flitzQ3_returnToFlitz"))
        {
            groundquests.sendSignal(player, "flitzReturnedQ3");
            return serverSide_endConversation(player, "\"Heavy core, clean contacts. You earn big, off-worlder. No questions asked, yes-yes.\"");
        }

        if (groundquests.hasCompletedQuest(player, Q3))
        {
            return serverSide_endConversation(player, "Flitz waves a cloth-wrapped claw. \"Come when Flitz has new lists. Fair rates always.\"");
        }

        if (groundquests.hasCompletedQuest(player, Q2) && !groundquests.isQuestActiveOrComplete(player, Q3))
        {
            return serverSide_startConversation(player, self,
                "Flitz taps a greasy datapad. \"Third run is messy—grav core at the old liner scatter. Sandcrawlers argue for that scrap. You still brave?\"",
                30,
                new ConvoResponse[] {
                    convo("accept_q3", "I'll get the core."),
                    convo("decline", "Not today.")
                });
        }

        if (groundquests.hasCompletedQuest(player, Q1) && !groundquests.isQuestActiveOrComplete(player, Q2))
        {
            return serverSide_startConversation(player, self,
                "\"Part two,\" he chirps, shoving a holomap into your hand. \"Podrace bleachers junk—one sealed bottle for off-world snobs. Quick feet.\"",
                20,
                new ConvoResponse[] {
                    convo("accept_q2", "I'll find the bottle."),
                    convo("decline", "Later.")
                });
        }

        if (!groundquests.isQuestActiveOrComplete(player, Q1))
        {
            return serverSide_startConversation(player, self,
                "The Jawa peers up from under brown wraps. \"Utinni! Tall walker—Flitz pays real credits for choice scrap south of Mos Eisley. Two picks, easy maps. Deal?\"",
                10,
                new ConvoResponse[] {
                    convo("accept_q1", "Deal."),
                    convo("decline", "Not interested.")
                });
        }

        if (groundquests.isQuestActive(player, Q1))
        {
            return serverSide_endConversation(player, "\"Waypoints on your datapad. Bring pieces back clean—Flitz deducts for dents!\"");
        }
        if (groundquests.isQuestActive(player, Q2))
        {
            return serverSide_endConversation(player, "\"Bottle first, chatter later! Sealed means sealed!\"");
        }
        if (groundquests.isQuestActive(player, Q3))
        {
            return serverSide_endConversation(player, "\"Core heavy—don't drop it in the dust. Static kills the payout!\"");
        }

        chat.chat(self, player, string_id.unlocalized("Flitz clicks his teeth nervously."));
        return SCRIPT_CONTINUE;
    }

    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "accept_q1"))
        {
            groundquests.grantQuest(player, Q1);
            return serverSide_endConversation(player, "\"Smart-smart! Flitz already loaded your waypoints. Don't let other jawas swipe the bins!\"");
        }
        if (responseIdIs(response, "accept_q2"))
        {
            groundquests.grantQuest(player, Q2);
            return serverSide_endConversation(player, "\"Quick feet, sealed lip. Flitz remembers who helped.\"");
        }
        if (responseIdIs(response, "accept_q3"))
        {
            groundquests.grantQuest(player, Q3);
            return serverSide_endConversation(player, "\"Liner wreck on your map—grav rack sticks out like a bantha horn. Go!\"");
        }
        if (responseIdIs(response, "decline"))
        {
            return serverSide_endConversation(player, "Flitz shrugs. \"Someone else takes the credits, then.\"");
        }

        chat.chat(self, "tatooine_jawa_flitz: unhandled response");
        return SCRIPT_CONTINUE;
    }
}
