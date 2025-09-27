package script.conversation;

import script.*;
import script.library.ai_lib;
import script.library.chat;
import script.library.utils;

public class kitchen_salesperson extends script.base_script
{
    public static String c_stringFile = "conversation/kitchen_salesperson";

    public boolean kitchen_salesperson_condition__defaultCondition(obj_id player, obj_id npc) throws InterruptedException
    {
        return true;
    }

    public void showTokenVendorUI(obj_id player, obj_id npc, float delay) throws InterruptedException
    {
        dictionary d = new dictionary();
        d.put("player", player);
        messageTo(npc, "showInventorySUI", d, 0, false);
    }

    public boolean kitchen_salesperson_condition_canBuy(obj_id player, obj_id npc) throws InterruptedException
    {
        return getTotalMoney(player) > 15000;
    }

    public void kitchen_salesperson_action_endConvo(obj_id player, obj_id npc) throws InterruptedException
    {
        npcEndConversation(player);
    }

    int kitchen_salesperson_handleBranch1(obj_id player, obj_id npc, string_id response) throws InterruptedException
    {
        if (response.equals("s_4"))
        {
            if (kitchen_salesperson_condition__defaultCondition(player, npc))
            {
                string_id message = new string_id(c_stringFile, "s_5");
                utils.removeScriptVar(player, "conversation.kitchen_salesperson.branchId");
                showTokenVendorUI(player, npc, 1.0f);
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        if (response.equals("s_6"))
        {
            if (kitchen_salesperson_condition__defaultCondition(player, npc))
            {
                string_id message = new string_id(c_stringFile, "s_7");
                utils.removeScriptVar(player, "conversation.kitchen_salesperson.branchId");
                npcEndConversationWithMessage(player, message);
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_DEFAULT;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        runSetup(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        runSetup(self);
        return SCRIPT_CONTINUE;
    }

    public int runSetup(obj_id self)
    {
        if ((!isMob(self)) || (isPlayer(self)))
        {
            detachScript(self, "conversation.kitchen_salesperson");
        }
        setName(self, "Engineer Gizmoton");
        setDescriptionString(self, "Gizmoton spends his days tinkering with appliances. What a nerd! But at least he has decent prices!");
        setCondition(self, CONDITION_CONVERSABLE);
        setCondition(self, CONDITION_HOLIDAY_INTERESTING);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException
    {
        int menu = menuInfo.addRootMenu(menu_info_types.CONVERSE_START, null);
        menu_info_data menuInfoData = menuInfo.getMenuItemById(menu);
        menuInfoData.setServerNotify(false);
        setCondition(self, CONDITION_CONVERSABLE);
        return SCRIPT_CONTINUE;
    }

    public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
    {
        clearCondition(self, CONDITION_CONVERSABLE);
        detachScript(self, "conversation.kitchen_salesperson");
        return SCRIPT_CONTINUE;
    }

    boolean npcStartConversation(obj_id player, obj_id npc, String convoName, string_id greetingId, prose_package greetingProse, string_id[] responses)
    {
        Object[] objects = new Object[responses.length];
        System.arraycopy(responses, 0, objects, 0, responses.length);
        return npcStartConversation(player, npc, convoName, greetingId, greetingProse, objects);
    }

    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
        {
            return SCRIPT_OVERRIDE;
        }
        if (kitchen_salesperson_condition__defaultCondition(player, self))
        {
            string_id message = new string_id(c_stringFile, "s_3");
            int numberOfResponses = 0;
            boolean hasResponse = false;
            boolean hasResponse0 = false;
            if (kitchen_salesperson_condition_canBuy(player, self))
            {
                ++numberOfResponses;
                hasResponse = true;
                hasResponse0 = true;
            }
            boolean hasResponse1 = false;
            if (kitchen_salesperson_condition__defaultCondition(player, self))
            {
                ++numberOfResponses;
                hasResponse = true;
                hasResponse1 = true;
            }
            if (hasResponse)
            {
                int responseIndex = 0;
                string_id[] responses = new string_id[numberOfResponses];

                if (hasResponse0)
                    responses[responseIndex++] = new string_id(c_stringFile, "s_4");

                if (hasResponse1)
                    responses[responseIndex++] = new string_id(c_stringFile, "s_6");

                utils.setScriptVar(player, "conversation.kitchen_salesperson.branchId", 1);
                npcStartConversation(player, self, "kitchen_salesperson", message, responses);
            }
            else
            {
                chat.chat(self, player, message);
            }
            return SCRIPT_CONTINUE;
        }
        chat.chat(self, "Error:  All conditions for OnStartNpcConversation were false.");
        return SCRIPT_CONTINUE;
    }

    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals("kitchen_salesperson"))
        {
            return SCRIPT_CONTINUE;
        }
        int branchId = utils.getIntScriptVar(player, "conversation.kitchen_salesperson.branchId");
        if (branchId == 1 && kitchen_salesperson_handleBranch1(player, self, response) == SCRIPT_CONTINUE) return SCRIPT_CONTINUE;
        chat.chat(self, "Error:  Fell through all branches and responses for OnNpcConversationResponse.");
        utils.removeScriptVar(player, "conversation.kitchen_salesperson.branchId");
        return SCRIPT_CONTINUE;
    }
}