package script.systems.missions.dynamic;

import script.conversation.base.ConvoResponse;
import script.library.ai_lib;
import script.library.bounty_hunter;
import script.library.utils;
import script.obj_id;
import script.string_id;

public class item_bounty_broker extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.item_bounty_broker";
    public String scriptName = "item_bounty_broker";

    public item_bounty_broker()
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

        boolean canTurnIn = isIdValid(bounty_hunter.getItemBountyMission(player));
        int[] pickupIds = bounty_hunter.getReadyItemBountiesForCreator(player, self);
        boolean canPickup = pickupIds != null && pickupIds.length > 0;

        if (!canTurnIn && !canPickup)
        {
            return serverSide_endConversation(player, "I can receive recovered item bounties and return ready items to creators.");
        }

        if (canTurnIn && canPickup)
        {
            return serverSide_startConversation(
                player,
                self,
                "Broker services available. Do you need to turn in a recovered target, or collect a ready item?",
                1,
                new ConvoResponse[] {
                    convo("turn_in", "Turn in recovered item"),
                    convo("pickup", "Collect ready item")
                }
            );
        }
        if (canTurnIn)
        {
            return serverSide_startConversation(
                player,
                self,
                "You have an active item bounty target. Turn it in now?",
                2,
                new ConvoResponse[] {
                    convo("turn_in", "Turn in recovered item"),
                    convo("later", "Not now")
                }
            );
        }
        return serverSide_startConversation(
            player,
            self,
            "I have an item waiting for you from your posted bounty. Retrieve it now?",
            3,
            new ConvoResponse[] {
                convo("pickup", "Collect ready item"),
                convo("later", "Not now")
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
        if (responseIdIs(response, "later"))
        {
            return serverSide_endConversation(player, "Understood. Return when you are ready.");
        }

        if (responseIdIs(response, "turn_in"))
        {
            if (bounty_hunter.turnInItemBounty(player, self))
            {
                return serverSide_endConversation(player, "Turn-in accepted. The bounty creator has been notified for pickup.");
            }
            return serverSide_endConversation(player, "I cannot accept a turn-in right now.");
        }

        if (responseIdIs(response, "pickup"))
        {
            int[] pickupIds = bounty_hunter.getReadyItemBountiesForCreator(player, self);
            if (pickupIds == null || pickupIds.length < 1)
            {
                return serverSide_endConversation(player, "No item bounty pickups are ready for you at this broker.");
            }

            if (pickupIds.length == 1)
            {
                if (bounty_hunter.pickupItemBounty(player, self, pickupIds[0]))
                {
                    return serverSide_endConversation(player, "Recovered item bounty delivered.");
                }
                return serverSide_endConversation(player, "Pickup failed. Ensure your inventory has space.");
            }

            ConvoResponse[] responses = new ConvoResponse[pickupIds.length];
            for (int i = 0; i < pickupIds.length; ++i)
            {
                responses[i] = convo("pickup_" + pickupIds[i], "Collect item bounty #" + pickupIds[i]);
            }
            return serverSide_respond(player, "Multiple pickups are ready. Select one.", 4, responses);
        }

        if (branchId == 4)
        {
            String id = response.getConvoResponseId();
            if (id != null && id.startsWith("pickup_"))
            {
                int bountyId = utils.stringToInt(id.substring("pickup_".length()));
                if (bounty_hunter.pickupItemBounty(player, self, bountyId))
                {
                    return serverSide_endConversation(player, "Recovered item bounty #" + bountyId + " delivered.");
                }
                return serverSide_endConversation(player, "Pickup failed. Ensure your inventory has space.");
            }
        }

        return serverSide_endConversation(player, "Broker transaction cancelled.");
    }
}
