package script.conversation;

import script.conversation.base.ConvoResponse;
import script.library.ai_lib;
import script.library.companion_lib;
import script.library.utils;
import script.obj_id;
import script.string_id;

/**
 * STF-less hire dialog for story companion {@code companion_greeata}.
 * World recruiters should use {@link script.library.companion_lib#prepareHireConversationNpc} after spawning
 * {@link script.library.companion_lib#GREEATA_WORLD_MOBILE_TEMPLATE} (avoid {@code greeata.iff} / missing {@code appearance/greeata.sat} client cubes).
 * Datapad pets still use creature {@code greeata} from {@code story_companions.tab}.
 */
public class companion_greeata extends script.conversation.base.conversation_base
{
    public static final String STORY_ID = "companion_greeata";
    public String conversation = "conversation.companion_greeata";
    public String scriptName = "companion_greeata";
    public companion_greeata()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
    }
    private ConvoResponse[] mainMenu() throws InterruptedException
    {
        return new ConvoResponse[]
        {
            convo("how_are_you", "How are you?"),
            convo("hire", "I'd like you to join me."),
            convo("about", "Tell me about yourself."),
            convo("goodbye", "I'll come back later.")
        };
    }
    private int openMainHub(obj_id player, obj_id self) throws InterruptedException
    {
        return serverSide_startConversation(
            player,
            self,
            "Greeata offers you a practiced smile -- the kind that plays well under stage lights. " +
            "\"Hey. You look like someone who ends up in interesting places. " +
            "If you're hiring, make it worth my time; if you're just passing through, don't waste mine. What's on your mind?\"",
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
        return openMainHub(player, self);
    }
    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(scriptName))
        {
            return SCRIPT_CONTINUE;
        }
        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");
        if (branchId == 1)
        {
            if (responseIdIs(response, "how_are_you"))
            {
                return handleHowAreYou(player, self);
            }
            if (responseIdIs(response, "hire"))
            {
                return handleHire(player, self);
            }
            if (responseIdIs(response, "about"))
            {
                return serverSide_respond(
                    player,
                    "\"I'm Greeata. I've danced for crowds that cheered and for bosses that counted credits. " +
                    "These days I prefer jobs where I pick my partners--and where the blaster fire isn't aimed at me unless I say so.\"",
                    2,
                    new ConvoResponse[]
                    {
                        convo("hub", "Let's talk about something else."),
                        convo("goodbye", "Goodbye.")
                    });
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "\"Stay sharp out there. The galaxy loves an encore--until it doesn't.\"");
            }
        }
        if (branchId == 2)
        {
            if (responseIdIs(response, "hub"))
            {
                return openMainHub(player, self);
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "\"Take care.\"");
            }
        }
        if (branchId == 10)
        {
            if (responseIdIs(response, "hub"))
            {
                return openMainHub(player, self);
            }
            if (responseIdIs(response, "goodbye"))
            {
                return serverSide_endConversation(player, "\"See you around.\"");
            }
        }
        utils.removeScriptVar(player, conversation + ".branchId");
        return SCRIPT_CONTINUE;
    }
    private int handleHowAreYou(obj_id player, obj_id self) throws InterruptedException
    {
        int inf = companion_lib.getInfluence(player, STORY_ID);
        int tier = companion_lib.getInfluenceTier(inf);
        boolean recruited = companion_lib.playerOwnsStoryCompanion(player, STORY_ID);
        String body;
        if (!recruited)
        {
            body = "\"Honestly? Curious. We haven't shipped out together yet--no bond on the roster, just small talk. " +
            "If you want to know how I really feel, get me on your crew first; then we'll have stories worth measuring.\"";
        }
        else
        {
            body = "\"With you? I'm sitting at " + inf + " regard--call it tier " + tier + " if you like numbers. ";
            if (tier <= 2)
            {
                body += "We're still learning each other's rhythm--professional, but not sentimental.\"";
            }
            else if (tier <= 5)
            {
                body += "I've seen you hold the line when it mattered. That counts for more than credits.\"";
            }
            else
            {
                body += "You're not just a handler anymore--you're the reason I don't look for a better gig.\"";
            }
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
    private int handleHire(obj_id player, obj_id self) throws InterruptedException
    {
        if (companion_lib.playerOwnsStoryCompanion(player, STORY_ID))
        {
            return serverSide_endConversation(
                player,
                "\"Already on your roster, hero--open that datapad and call me up when the shooting starts.\"");
        }
        obj_id cd = companion_lib.grantStoryCompanionToDatapad(player, STORY_ID);
        if (!isIdValid(cd) || !exists(cd))
        {
            return serverSide_endConversation(
                player,
                "\"I'd love to--but something's wrong with your datapad space or pet limits. Sort that out and find me again.\"");
        }
        companion_lib.modifyInfluence(player, STORY_ID, 50);
        return serverSide_endConversation(
            player,
            "\"Deal. You get my blaster, my sense of timing, and my patience--don't waste any of the three. " +
            "I'll be on your datapad when you need me.\"");
    }
}
