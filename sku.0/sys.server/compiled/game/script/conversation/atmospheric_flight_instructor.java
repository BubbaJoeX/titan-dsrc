package script.conversation;

import script.conversation.base.ConvoResponse;
import script.library.ai_lib;
import script.library.utils;
import script.obj_id;
import script.string_id;

/**
 * Verbose tutorial conversation: atmospheric flight basics. Pure server-side text (no STF).
 * Attach {@code conversation.atmospheric_flight_instructor} to any conversable NPC or object.
 */
public class atmospheric_flight_instructor extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.atmospheric_flight_instructor";
    public String scriptName = "atmospheric_flight_instructor";

    private static final int BR_MAIN = 1;
    private static final int BR_OVERVIEW = 2;
    private static final int BR_LAUNCH = 3;
    private static final int BR_LAND = 4;
    private static final int BR_SINGLE_SHIP = 5;
    private static final int BR_AUTOPILOT = 6;
    private static final int BR_BOARDING = 7;
    private static final int BR_COMBAT = 8;

    public atmospheric_flight_instructor()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
    }

    private ConvoResponse[] mainMenuResponses()
    {
        return new ConvoResponse[] {
            convo("topic_overview", "What is atmospheric flight?"),
            convo("topic_launch", "How do I launch a ship on a planet?"),
            convo("topic_land", "How do I land or pack my ship?"),
            convo("topic_single", "Why can I only have one ship out?"),
            convo("topic_autopilot", "Explain autopilot and the planet map."),
            convo("topic_boarding", "Boarding, parking, summon, and leaving the ship."),
            convo("topic_combat", "Fighting from the air — basics and safety."),
            convo("goodbye", "Thanks, that is enough for now.")
        };
    }

    private ConvoResponse[] backOrDone()
    {
        return new ConvoResponse[] {
            convo("back_main", "Back to the main topics."),
            convo("goodbye", "Goodbye.")
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
            "Welcome. I am cleared to brief pilots on atmospheric flight — that is, flying a starship in the open sky over a planetary surface, "
                + "rather than only in a dedicated space zone.\n\n"
                + "If you are new to planetside operations, start with \"What is atmospheric flight?\" "
                + "and work through the topics in any order you like. I will keep the explanation thorough.",
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

        if (!utils.hasScriptVar(player, conversation + ".branchId"))
        {
            return SCRIPT_CONTINUE;
        }

        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");

        if (responseIdIs(response, "goodbye"))
        {
            return serverSide_endConversation(player,
                "Fly safe, maintain situational awareness, and remember: the ground is much closer than it looks. Good hunting.");
        }

        if (responseIdIs(response, "back_main"))
        {
            return serverSide_respond(
                player,
                "Of course. What would you like to hear about next?",
                BR_MAIN,
                mainMenuResponses()
            );
        }

        if (branchId == BR_MAIN)
        {
            if (responseIdIs(response, "topic_overview"))
            {
                return serverSide_respond(player, MSG_OVERVIEW, BR_OVERVIEW, backOrDone());
            }
            if (responseIdIs(response, "topic_launch"))
            {
                return serverSide_respond(player, MSG_LAUNCH, BR_LAUNCH, backOrDone());
            }
            if (responseIdIs(response, "topic_land"))
            {
                return serverSide_respond(player, MSG_LAND, BR_LAND, backOrDone());
            }
            if (responseIdIs(response, "topic_single"))
            {
                return serverSide_respond(player, MSG_SINGLE_SHIP, BR_SINGLE_SHIP, backOrDone());
            }
            if (responseIdIs(response, "topic_autopilot"))
            {
                return serverSide_respond(player, MSG_AUTOPILOT, BR_AUTOPILOT, backOrDone());
            }
            if (responseIdIs(response, "topic_boarding"))
            {
                return serverSide_respond(player, MSG_BOARDING, BR_BOARDING, backOrDone());
            }
            if (responseIdIs(response, "topic_combat"))
            {
                return serverSide_respond(player, MSG_COMBAT, BR_COMBAT, backOrDone());
            }
        }

        if (branchId == BR_OVERVIEW || branchId == BR_LAUNCH || branchId == BR_LAND || branchId == BR_SINGLE_SHIP
            || branchId == BR_AUTOPILOT || branchId == BR_BOARDING || branchId == BR_COMBAT)
        {
            if (responseIdIs(response, "back_main"))
            {
                return serverSide_respond(
                    player,
                    "Certainly. Pick another topic, or say goodbye when you are finished.",
                    BR_MAIN,
                    mainMenuResponses()
                );
            }
        }

        utils.removeScriptVar(player, conversation + ".branchId");
        return SCRIPT_CONTINUE;
    }

    // --- Long-form copy (no STF) -------------------------------------------------

    private static final String MSG_OVERVIEW =
        "Atmospheric flight means your ship is treated as a real vehicle in the planet's airspace: you fly above terrain, "
            + "you can see and interact with the surface in many of the same ways space combat interacts with targets, "
            + "and you land or \"pack\" the ship when you are done instead of leaving it in orbit.\n\n"
            + "Not every world allows it: atmospheric flight is generally available on standard ground planets, "
            + "but certain special environments may exclude it. Think of it as \"low altitude space\" tied to the ground scene.\n\n"
            + "The client and server both treat this as a ship scene (space or atmospheric), so your flight controls, "
            + "components, and many space rules still apply — you are simply operating in the planet's coordinate space "
            + "with terrain height underneath you instead of empty void.\n\n"
            + "Why bother? You can move quickly across a continent, position for ground support, board passengers from terrain, "
            + "and use autopilot to cross long distances without hand-flying every meter.";

    private static final String MSG_LAUNCH =
        "To launch, you typically use your Ship Control Device — the datapad object that owns your vessel — from its radial menu. "
            + "Look for options related to atmospheric flight and choose Launch Ship. Starport terminals and similar interfaces "
            + "may offer the same capability depending on configuration.\n\n"
            + "Behind the scenes the server checks that you are allowed to unpack a ship here: you must not already have "
            + "another hull deployed in this mode, and the usual ownership and certification rules still apply. "
            + "When the launch succeeds, your ship appears above you at altitude (commonly on the order of a few hundred meters), "
            + "you are placed in the pilot seat, and the atmospheric boarding logic comes online so others can approach and board "
            + "when the ship is parked appropriately.\n\n"
            + "If something fails, you will usually get a system message rather than a silent failure — read it carefully: "
            + "it often means another ship is already out, you are in the wrong place, or the interior is still building.";

    private static final String MSG_LAND =
        "Landing, in practice, means telling the server to pack your ship back into the control device safely. "
            + "From the Ship Control Device radial (or equivalent), choose the land / pack option for atmospheric flight.\n\n"
            + "The server must clear autopilot, detach temporary boarding helpers, and move any passengers off the hull "
            + "to the terrain below before the ship is stowed. In atmosphere this is deliberately staged: "
            + "there can be a short delay so the client unloads the vessel cleanly and avoids rendering glitches when "
            + "a large mesh disappears near the ground.\n\n"
            + "Do not panic if the ship lingers a few seconds — that is normal. Once packing completes, the hull returns to your "
            + "control device and your radial menus refresh so you can launch again later. "
            + "If you are aboard as a passenger, expect to be placed on the ground near where the ship was when the pilot initiates landing.";

    private static final String MSG_SINGLE_SHIP =
        "Operational policy: one deployed ship per pilot in this environment. The server tracks which control device "
            + "currently has a live hull in the world; you cannot spawn a second copy until the first is packed or otherwise cleared.\n\n"
            + "That sounds restrictive, but it keeps performance predictable and avoids duplicate entities fighting over the same "
            + "certificate and ownership data. Your other ships remain safely stored on their own devices — "
            + "they are simply not \"out\" at the same time.\n\n"
            + "After every launch or land, menus on all of your control devices are dirtied so the radial updates immediately: "
            + "you should never need to relog to see Launch disappear while a ship is already flying.";

    private static final String MSG_AUTOPILOT =
        "Autopilot here is a structured flight computer: it is not just \"hold speed\" — it runs phases. "
            + "Typically the ship climbs to a takeoff altitude (on the order of five hundred meters), "
            + "cruises toward your chosen ground point at a set altitude, then descends to a landing window above the terrain "
            + "before releasing control back to you.\n\n"
            + "You can often trigger a destination from the planet map: pick a point and use the auto-pilot action when you are "
            + "allowed to (ownership and ship type rules apply). While autopilot runs, the flight model may ignore direct player "
            + "stick input so the trajectory stays stable — cancel from the map or take manual control when the system allows.\n\n"
            + "Status ticks and roleplay-style updates may fire on a timer so you know which phase you are in and how far you have left. "
            + "If you cancel mid-route, expect a clean shutdown rather than an instant snap roll.";

    private static final String MSG_BOARDING =
        "Player ships with interiors — often called POB hulls — are special: once launched in atmosphere they can remain "
            + "in the world while empty, \"parked,\" so friends can run up and board from the ground radial when distance and "
            + "permissions allow.\n\n"
            + "Boarding from terrain generally uses the exterior radial on the ship while you are close enough and the scene "
            + "rules permit it. The server transfers you into the interior cell with proper permissions — "
            + "owners can tighten or loosen who may enter.\n\n"
            + "If you are inside and need to step out, use the airlock / escape hatch style interactions: "
            + "when the ship is not under pilot control, you can often depart through the boarding ramp to the terrain below. "
            + "Summon-ship features, where implemented, send your already-deployed hull to your ground position via autopilot "
            + "so you do not have to hike back to where you parked.\n\n"
            + "Always confirm the ship is stable — not mid-jump, not under hostile fire — before stepping out.";

    private static final String MSG_COMBAT =
        "In atmospheric flight your ship weapons can engage ground targets: projectiles respect terrain height, "
            + "and creatures may be resolved with proximity rules when full collision is not available. "
            + "Splash weapons can affect an area; expect the same general damage pipeline as space, with ground-specific tuning.\n\n"
            + "Player-versus-player from the air is gated by the same faction and TEF concepts you know from ground PvP: "
            + "do not assume you can strafe neutrals without consequences. When in doubt, identify your target.\n\n"
            + "Finally, remember visibility: ground networks and air networks bridge through special visibility managers at altitude. "
            + "If something fails to render, altitude and range are the first suspects — not always a bug.";
}
