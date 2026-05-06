package script.conversation;

import script.conversation.base.ConvoResponse;
import script.dictionary;
import script.library.ai_lib;
import script.library.money;
import script.library.open_face_vendor_lib;
import script.library.utils;
import script.obj_id;
import script.string_id;

/**
 * Cinematic "face vendor": priced props in the world within range share {@code open_face_vendor.vendor_key} with
 * this NPC; stock is built dynamically at talk time. Selecting an item pans the camera to that object, then
 * Buy / Cancel. See {@link script.library.open_face_vendor_lib}.
 */
public class open_face_vendor extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.open_face_vendor";
    public String scriptName = "open_face_vendor";

    private static final int BRANCH_GREET = 1;
    private static final int BRANCH_BROWSE = 10;
    private static final int BRANCH_ITEM = 20;

    public open_face_vendor()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setCondition(self, CONDITION_CONVERSABLE);
        setInvulnerable(self, true);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setCondition(self, CONDITION_CONVERSABLE);
        setInvulnerable(self, true);
        return SCRIPT_CONTINUE;
    }

    private String getGreeting(obj_id self) throws InterruptedException
    {
        if (hasObjVar(self, open_face_vendor_lib.OV_GREETING))
        {
            return getStringObjVar(self, open_face_vendor_lib.OV_GREETING);
        }
        return "Hello! How are you?";
    }

    private void clearPlayerVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, conversation + ".branchId");
        open_face_vendor_lib.clearPendingPurchase(player);
    }

    @Override
    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }
        clearPlayerVars(player);

        if (!open_face_vendor_lib.hasVendorKeyConfigured(self))
        {
            return serverSide_endConversation(player, "This vendor is not set up yet.");
        }
        obj_id[] stock = open_face_vendor_lib.getSellableStock(self);
        if (stock == null || stock.length < 1)
        {
            return serverSide_endConversation(player, "There's nothing for sale here yet.");
        }

        return serverSide_startConversation(
            player,
            self,
            getGreeting(self),
            BRANCH_GREET,
            new ConvoResponse[]
            {
                convo("browse", "I would like to browse your goods."),
                convo("bye", "Goodbye.")
            });
    }

    private ConvoResponse[] buildBrowseMenu(obj_id npc) throws InterruptedException
    {
        obj_id[] stock = open_face_vendor_lib.getSellableStock(npc);
        if (stock == null || stock.length < 1)
        {
            return new ConvoResponse[]
            {
                convo("bye", "Never mind.")
            };
        }
        ConvoResponse[] out = new ConvoResponse[stock.length + 1];
        for (int i = 0; i < stock.length; ++i)
        {
            String line = open_face_vendor_lib.formatBrowseLine(stock[i]);
            out[i] = convo("item_" + i, line);
        }
        out[out.length - 1] = convo("browse_back", "Goodbye.");
        return out;
    }

    private int showBrowseMenu(obj_id player, obj_id self) throws InterruptedException
    {
        return serverSide_respond(
            player,
            "This is what I have to choose from:",
            BRANCH_BROWSE,
            buildBrowseMenu(self));
    }

    private int showItemDetail(obj_id player, obj_id self, obj_id stockObj) throws InterruptedException
    {
        if (!isIdValid(stockObj) || !exists(stockObj))
        {
            return showBrowseMenu(player, self);
        }
        script.library.conversation.npcConversationCameraLookAtTarget(player, stockObj, 6.0f, 1.2f);
        String msg = open_face_vendor_lib.formatDetailMessage(stockObj);
        return serverSide_respond(
            player,
            msg,
            BRANCH_ITEM,
            new ConvoResponse[]
            {
                convo("buy", "Buy"),
                convo("cancel", "Cancel")
            });
    }

    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(scriptName))
        {
            return SCRIPT_CONTINUE;
        }

        int branchId = utils.getIntScriptVar(player, conversation + ".branchId");

        if (branchId == BRANCH_GREET)
        {
            if (responseIdIs(response, "browse"))
            {
                return showBrowseMenu(player, self);
            }
            if (responseIdIs(response, "bye"))
            {
                clearPlayerVars(player);
                return serverSide_endConversation(player, "Take care.");
            }
        }

        if (branchId == BRANCH_BROWSE)
        {
            if (responseIdIs(response, "browse_back"))
            {
                script.library.conversation.npcConversationCameraReturnToSpeaker(player);
                clearPlayerVars(player);
                return serverSide_endConversation(player, "Come back anytime.");
            }
            String rid = response.getConvoResponseId();
            if (rid != null && rid.startsWith("item_"))
            {
                try
                {
                    int idx = Integer.parseInt(rid.substring("item_".length()));
                    obj_id stockObj = open_face_vendor_lib.findStockObjectByIndex(self, idx);
                    if (!isIdValid(stockObj))
                    {
                        return showBrowseMenu(player, self);
                    }
                    utils.setScriptVar(player, conversation + ".selIdx", idx);
                    return showItemDetail(player, self, stockObj);
                }
                catch (NumberFormatException ignored)
                {
                    return showBrowseMenu(player, self);
                }
            }
        }

        if (branchId == BRANCH_ITEM)
        {
            if (responseIdIs(response, "cancel"))
            {
                script.library.conversation.npcConversationCameraReturnToSpeaker(player);
                return showBrowseMenu(player, self);
            }
            if (responseIdIs(response, "buy"))
            {
                int idx = utils.getIntScriptVar(player, conversation + ".selIdx");
                obj_id stockObj = open_face_vendor_lib.findStockObjectByIndex(self, idx);
                if (!isIdValid(stockObj) || !exists(stockObj))
                {
                    script.library.conversation.npcConversationCameraReturnToSpeaker(player);
                    return showBrowseMenu(player, self);
                }
                int price = open_face_vendor_lib.getPrice(stockObj);
                if (price < 1)
                {
                    return showItemDetail(player, self, stockObj);
                }
                if (getTotalMoney(player) < price)
                {
                    return serverSide_respond(
                        player,
                        "You can't afford that.",
                        BRANCH_ITEM,
                        new ConvoResponse[]
                        {
                            convo("buy", "Buy"),
                            convo("cancel", "Cancel")
                        });
                }
                utils.setScriptVar(player, open_face_vendor_lib.SV_PENDING_STOCK, stockObj);
                dictionary payParams = new dictionary();
                payParams.put("npc", self);
                money.requestPayment(player, self, price, "openFaceVendorPaid", payParams, true);
                return SCRIPT_CONTINUE;
            }
        }

        clearPlayerVars(player);
        return SCRIPT_CONTINUE;
    }

    /**
     * Money callback (success and failure) from {@link money#requestPayment}; registered by name on this NPC script.
     */
    public int openFaceVendorPaid(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = params.getObjId(money.DICT_PLAYER_ID);
        if (!isIdValid(player))
        {
            return SCRIPT_CONTINUE;
        }
        int code = params.containsKey(money.DICT_CODE) ? params.getInt(money.DICT_CODE) : money.RET_SUCCESS;
        obj_id pending = null;
        if (utils.hasScriptVar(player, open_face_vendor_lib.SV_PENDING_STOCK))
        {
            pending = utils.getObjIdScriptVar(player, open_face_vendor_lib.SV_PENDING_STOCK);
        }
        open_face_vendor_lib.clearPendingPurchase(player);

        if (code == money.RET_FAIL || !isIdValid(pending))
        {
            sendSystemMessage(player, string_id.unlocalized("You can't afford that."));
            return SCRIPT_CONTINUE;
        }

        if (!open_face_vendor_lib.grantPurchasedCopy(player, pending))
        {
            sendSystemMessage(player, string_id.unlocalized("Purchase failed — inventory or item issue."));
            return SCRIPT_CONTINUE;
        }

        sendSystemMessage(player, string_id.unlocalized("Sold — check your inventory."));
        script.library.conversation.npcConversationCameraReturnToSpeaker(player);

        utils.setScriptVar(player, conversation + ".branchId", BRANCH_BROWSE);
        return serverSide_respond(
            player,
            "Anything else?",
            BRANCH_BROWSE,
            buildBrowseMenu(self));
    }
}
