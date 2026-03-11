package script.space.atmo.content;

import script.library.ai_lib;
import script.library.utils;
import script.obj_id;
import script.string_id;

/**
 * TaggeCo Atmospheric Safety Platform TAT-A7
 *
 * Automated atmospheric landing and safety platform located on Tatooine.
 * Platform provides docking, navigation telemetry, and storm monitoring.
 * Station interface communicates only when queried by pilots.
 */
public class atmospheric_station extends script.conversation.base.conversation_base
{
    public static final String CONVERSATION = "space.atmo.content.atmospheric_station";
    public static final String SCRIPT_NAME = "atmospheric_station";

    // Branch IDs
    private static final int BRANCH_MAIN = 1;
    private static final int BRANCH_LANDING = 2;
    private static final int BRANCH_FEE_EXPLAIN = 3;
    private static final int BRANCH_STATION_INFO = 4;
    private static final int BRANCH_OPERATOR_INFO = 5;
    private static final int BRANCH_HAZARD_REPORT = 6;

    public atmospheric_station()
    {
        super.scriptName = SCRIPT_NAME;
        super.conversation = CONVERSATION;
    }

    public int OnAttach(obj_id self)
    {
        setCondition()
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
            "Platform TAT-A7 interface active.\nAwaiting request.",
            BRANCH_MAIN,
            new ConvoResponse[] {
                convo("landing", "Request landing clearance."),
                convo("station_id", "Station identification."),
                convo("operator_id", "Operator identification."),
                convo("hazard", "Local hazard report.")
            }
        );
    }

    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(SCRIPT_NAME))
        {
            return SCRIPT_CONTINUE;
        }

        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");

        // Main menu
        if (branchId == BRANCH_MAIN)
        {
            return handleMainMenu(player, self, response);
        }

        // Landing clearance branch
        if (branchId == BRANCH_LANDING)
        {
            return handleLandingBranch(player, self, response);
        }

        // Fee explanation branch
        if (branchId == BRANCH_FEE_EXPLAIN)
        {
            return handleFeeExplainBranch(player, self, response);
        }

        // Station information branch
        if (branchId == BRANCH_STATION_INFO)
        {
            return handleStationInfoBranch(player, self, response);
        }

        // Operator information branch
        if (branchId == BRANCH_OPERATOR_INFO)
        {
            return handleOperatorInfoBranch(player, self, response);
        }

        // Hazard report branch
        if (branchId == BRANCH_HAZARD_REPORT)
        {
            return handleHazardReportBranch(player, self, response);
        }

        utils.removeScriptVar(player, conversation + ".branchId");
        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // Main Menu Handler
    // ========================================================================

    private int handleMainMenu(obj_id player, obj_id self, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "landing"))
        {
            return serverSide_respond(
                player,
                "Scanning vessel transponder.\nLanding pad availability: two.\nDocking fee: one hundred seventy-five credits.",
                BRANCH_LANDING,
                new ConvoResponse[] {
                    convo("authorize", "Authorize docking fee."),
                    convo("cancel", "Cancel request."),
                    convo("fee_info", "Fee justification.")
                }
            );
        }

        if (responseIdIs(response, "station_id"))
        {
            return serverSide_respond(
                player,
                "Designation: Atmospheric Safety Platform TAT-A7.\nClassification: automated landing and navigation platform.\nLocation: Tatooine upper-atmosphere approach corridor.",
                BRANCH_STATION_INFO,
                new ConvoResponse[] {
                    convo("install_reason", "Reason for installation."),
                    convo("op_duration", "Operational duration.")
                }
            );
        }

        if (responseIdIs(response, "operator_id"))
        {
            return serverSide_respond(
                player,
                "Platform owned by TaggeCo Interstellar Logistics.\nOperational control managed by automated traffic intelligence.",
                BRANCH_OPERATOR_INFO,
                new ConvoResponse[] {
                    convo("crew", "Crew presence."),
                    convo("authority", "Authority oversight.")
                }
            );
        }

        if (responseIdIs(response, "hazard"))
        {
            return serverSide_respond(
                player,
                "Atmospheric turbulence level: elevated.\nSandstorm activity detected in surrounding region.\nMultiple vessel signatures detected within sensor range.",
                BRANCH_HAZARD_REPORT,
                new ConvoResponse[] {
                    convo("identify_vessels", "Identify vessels."),
                    convo("acknowledged", "Acknowledged.")
                }
            );
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // Landing Clearance Handler
    // ========================================================================

    private int handleLandingBranch(obj_id player, obj_id self, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "authorize"))
        {
            // TODO: Deduct credits from player, assign landing pad
            return serverSide_endConversation(
                player,
                "Payment accepted.\nTransmitting landing vector.\nPad three assigned."
            );
        }

        if (responseIdIs(response, "cancel"))
        {
            return serverSide_endConversation(
                player,
                "Request cancelled.\nAtmospheric turbulence outside platform perimeter exceeds recommended landing parameters."
            );
        }

        if (responseIdIs(response, "fee_info"))
        {
            return serverSide_respond(
                player,
                "Docking fees fund navigation beacons, structural maintenance, and atmospheric monitoring systems.",
                BRANCH_FEE_EXPLAIN,
                new ConvoResponse[] {
                    convo("authorize", "Authorize docking fee."),
                    convo("cancel", "Cancel request.")
                }
            );
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // Fee Explanation Handler
    // ========================================================================

    private int handleFeeExplainBranch(obj_id player, obj_id self, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "authorize"))
        {
            // TODO: Deduct credits from player, assign landing pad
            return serverSide_endConversation(
                player,
                "Payment accepted.\nTransmitting landing vector.\nPad three assigned."
            );
        }

        if (responseIdIs(response, "cancel"))
        {
            return serverSide_endConversation(
                player,
                "Request cancelled.\nAtmospheric turbulence outside platform perimeter exceeds recommended landing parameters."
            );
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // Station Information Handler
    // ========================================================================

    private int handleStationInfoBranch(obj_id player, obj_id self, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "install_reason"))
        {
            return serverSide_endConversation(
                player,
                "Historical data indicates multiple vessel losses in this atmospheric corridor due to sandstorm interference.\nPlatform installed to provide emergency landing and navigation telemetry."
            );
        }

        if (responseIdIs(response, "op_duration"))
        {
            return serverSide_endConversation(
                player,
                "Platform operational time: eighteen standard years.\nSuccessful landings recorded: twelve thousand four hundred forty-two."
            );
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // Operator Information Handler
    // ========================================================================

    private int handleOperatorInfoBranch(obj_id player, obj_id self, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "crew"))
        {
            return serverSide_endConversation(
                player,
                "Negative.\nPlatform maintenance conducted periodically by offworld technicians."
            );
        }

        if (responseIdIs(response, "authority"))
        {
            return serverSide_endConversation(
                player,
                "Platform complies with Outer Rim shipping regulations.\nTransponder data may be transmitted to regional authorities when required."
            );
        }

        return SCRIPT_CONTINUE;
    }

    // ========================================================================
    // Hazard Report Handler
    // ========================================================================

    private int handleHazardReportBranch(obj_id player, obj_id self, string_id response) throws InterruptedException
    {
        if (responseIdIs(response, "identify_vessels"))
        {
            return serverSide_endConversation(
                player,
                "Several detected vessels possess inactive transponders.\nClassification unavailable."
            );
        }

        if (responseIdIs(response, "acknowledged"))
        {
            return serverSide_endConversation(
                player,
                "Acknowledgement recorded.\nInterface available for further requests."
            );
        }

        return SCRIPT_CONTINUE;
    }
}

