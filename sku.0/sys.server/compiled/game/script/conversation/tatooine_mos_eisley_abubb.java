package script.conversation;

import script.conversation.base.ConvoResponse;
import script.conversation.base.conversation_base;
import script.library.ai_lib;
import script.library.chat;
import script.library.groundquests;
import script.*;

/**
 * Abubb — Mos Eisley holocron bait quest. Attach {@code conversation.tatooine_mos_eisley_abubb} to your NPC and place him near {@code 3518, 5, -4788} on Tatooine (match {@code tatooine_abubb_holocron_ambush} wait waypoint).
 * <p>
 * Place a static tangible using {@code object/tangible/loot/creature_loot/collections/sith_holocron_01.iff} at the retrieve waypoint ({@code 3545, 5, -4685}) so {@code quest.task.ground.retrieve_item} can resolve.
 */
public class tatooine_mos_eisley_abubb extends conversation_base
{
    public static final String Q_ABUBB = "quest/tatooine_abubb_holocron_ambush";

    public tatooine_mos_eisley_abubb()
    {
        conversation = "conversation.tatooine_mos_eisley_abubb";
        scriptName = "conversation.tatooine_mos_eisley_abubb";
    }

    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }
        faceTo(self, player);

        if (groundquests.isTaskActive(player, Q_ABUBB, "abubb_returnAbubb"))
        {
            groundquests.sendSignal(player, "abubbReturnedWithHolocron");
            return serverSide_endConversation(player,
                "Abubb palms the holocron without looking at it. \"Good. My buyers hate loose ends.\" He whistles once—two cloaked figures unfold from the shadows with ignited blades.");
        }

        if (groundquests.hasCompletedQuest(player, Q_ABUBB))
        {
            return serverSide_endConversation(player,
                "Abubb's stall sits empty; the wind carries sand through the spot where he used to stand.");
        }

        if (!groundquests.isQuestActiveOrComplete(player, Q_ABUBB))
        {
            return serverSide_startConversation(player, self,
                "A scarred human leans under the awning, voice low. \"You look hungry for credits. I need a trinket out in the scrap—Sith junk. Bring it here and we talk payday.\"",
                10,
                new ConvoResponse[] {
                    convo("accept_abubb", "I'll get it."),
                    convo("decline", "Not interested.")
                });
        }

        if (groundquests.isTaskActive(player, Q_ABUBB, "abubb_retrieveHolocron"))
        {
            return serverSide_endConversation(player,
                "\"Datapad's marked,\" Abubb mutters. \"Southeast rings—don't get dust in the contacts.\"");
        }

        if (groundquests.isTaskActive(player, Q_ABUBB, "abubb_surviveElders"))
        {
            return serverSide_endConversation(player,
                "Abubb watches from the alley mouth, arms folded. \"Survive first. Talk later.\"");
        }

        chat.chat(self, player, string_id.unlocalized("Abubb counts invisible coins between his fingers."));
        return SCRIPT_CONTINUE;
    }

    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "accept_abubb"))
        {
            groundquests.grantQuest(player, Q_ABUBB);
            return serverSide_endConversation(player,
                "\"Smart. Waypoint's on your pad—don't make me send a finder droid.\"");
        }
        if (responseIdIs(response, "decline"))
        {
            return serverSide_endConversation(player,
                "Abubb shrugs. \"Plenty of thirst out here. Your loss.\"");
        }

        chat.chat(self, "tatooine_mos_eisley_abubb: unhandled response");
        return SCRIPT_CONTINUE;
    }
}
