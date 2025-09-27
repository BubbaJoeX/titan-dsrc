package script.content;/*
@Origin: dsrc.script.content
@Author:  BubbaJoeX
@Purpose: Barks a message on a timer
@Requirements: <no requirements>
@Notes: This script is an advanced version of the barker script. It allows for 4 chatter options and a barking cycle to help with a more dynamic environment.
@Created: Wednesday, 4/24/2024, at 9:58 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.chat;
import script.library.sui;

public class barker_nv extends base_script
{
    public int setup(obj_id self) throws InterruptedException
    {
        setCondition(self, CONDITION_CONVERSABLE);
        setInvulnerable(self, true);
        if (hasObjVar(self, "barker.name"))
        {
            setName(self, getStringObjVar(self, "barker.name"));
        }
        if (hasObjVar(self, "posture"))
        {
            setPosture(self, getIntObjVar(self, "posture"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setup(self);
        kickOffChatter(self);
        return SCRIPT_CONTINUE;
    }

    public void kickOffChatter(obj_id self)
    {
        if (hasObjVar(self, "barkMessage"))
        {
            dictionary details = new dictionary();
            details.put("barkMessage", getStringObjVar(self, "barkMessage"));
            if (hasObjVar(self, "barkDelay"))
            {
                details.put("barkDelay", getFloatObjVar(self, "barkDelay"));
            }
            else
            {
                setObjVar(self, "barkDelay", 60.0f);
                details.put("barkDelay", 60.0f);
            }
            messageTo(self, "bark", details, 60.0f, false);
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (isGod(player))
        {
            int barkMenu = mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Configure Barking"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU1, new string_id("Start Cycle"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU31, new string_id("Set Posture"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU2, new string_id("Set Message"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU3, new string_id("Set Delay"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU4, new string_id("Stop Cycle"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU5, new string_id("Speak Message (No Cycle)"));
            mi.addSubMenu(barkMenu, menu_info_types.SERVER_MENU11, new string_id("Set Name"));
            int chatterMenu = mi.addRootMenu(menu_info_types.SERVER_MENU6, new string_id("Configure Messages"));
            mi.addSubMenu(chatterMenu, menu_info_types.SERVER_MENU7, new string_id("Option 1"));
            mi.addSubMenu(chatterMenu, menu_info_types.SERVER_MENU8, new string_id("Option 2"));
            mi.addSubMenu(chatterMenu, menu_info_types.SERVER_MENU9, new string_id("Option 3"));
            mi.addSubMenu(chatterMenu, menu_info_types.SERVER_MENU10, new string_id("Option 4"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU1)
        {
            dictionary details = new dictionary();
            if (!hasObjVar(self, "barkMessage"))
            {
                broadcast(player, "You must set a message first.");
                return SCRIPT_CONTINUE;
            }
            if (!hasObjVar(self, "barkDelay"))
            {
                broadcast(player, "You must set a delay in seconds first.");
                return SCRIPT_CONTINUE;
            }
            details.put("barkMessage", getStringObjVar(self, "barkMessage"));
            details.put("barkDelay", getFloatObjVar(self, "barkDelay"));
            messageTo(self, "bark", details, getFloatObjVar(self, "barkDelay"), false);
        }
        else if (item == menu_info_types.SERVER_MENU2)
        {
            sui.inputbox(self, player, "Enter the message you would like this entity to bark.", "handleMessageInput");
        }
        else if (item == menu_info_types.SERVER_MENU3)
        {
            sui.inputbox(self, player, "Enter the delay in seconds you would like this entity to bark.", "handleDelayInput");
        }
        else if (item == menu_info_types.SERVER_MENU4)
        {
            removeObjVar(self, "barkMessage");
            removeObjVar(self, "barkDelay");
            stopListeningToMessage(self, "bark");
            broadcast(player, "Barking cycle stopped.");
            return SCRIPT_CONTINUE;
        }
        else if (item == menu_info_types.SERVER_MENU5)
        {
            if (!hasObjVar(self, "barkMessage"))
            {
                broadcast(player, "You must set a message first.");
                return SCRIPT_CONTINUE;
            }
            String message = getStringObjVar(self, "barkMessage");
            chat.chat(self, message);
        }
        else if (item == menu_info_types.SERVER_MENU7)
        {
            sui.inputbox(self, player, "Chatter Option 1:", "handleChatterType1");
        }
        else if (item == menu_info_types.SERVER_MENU8)
        {
            sui.inputbox(self, player, "Chatter Option 2:", "handleChatterType2");
        }
        else if (item == menu_info_types.SERVER_MENU9)
        {
            sui.inputbox(self, player, "Chatter Option 3:", "handleChatterType3");
        }
        else if (item == menu_info_types.SERVER_MENU10)
        {
            sui.inputbox(self, player, "Chatter Option 4:", "handleChatterType4");
        }
        else if (item == menu_info_types.SERVER_MENU11)
        {
            sui.inputbox(self, player, "Name this mobile.", "handleSetName");
        }
        else if (item == menu_info_types.SERVER_MENU31)
        {
            String[] VALID_POSTURES = {
                    "Upright",
                    "Crouched",
                    "Prone",
                    "Sitting",
                    "Sneaking",
                    "Knocked Down",
                    "Skill Animating",
            };
            sui.listbox(self, player, "Select a posture", sui.OK_CANCEL, "Posture", VALID_POSTURES, "handlePostureSelect", true);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleMessageInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String message = sui.getInputBoxText(params);
        setObjVar(self, "barkMessage", message);
        broadcast(player, "Message set to: " + message);
        return SCRIPT_CONTINUE;
    }

    public int handleSetName(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String message = sui.getInputBoxText(params);
        if (message.length() >= 200)
        {
            broadcast(player, "That name is too long!");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "barker.name", message);
        setName(self, message);
        broadcast(player, "Name set to: " + message);
        return SCRIPT_CONTINUE;
    }

    public int handlePostureSelect(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        int idx = sui.getListboxSelectedRow(params);
        int[] postureIds = {
                POSTURE_UPRIGHT,
                POSTURE_CROUCHED,
                POSTURE_PRONE,
                POSTURE_SITTING,
                POSTURE_SNEAKING,
                POSTURE_KNOCKED_DOWN,
                POSTURE_SKILL_ANIMATING
        };
        setObjVar(self, "posture", postureIds[idx]);
        if (!hasScript(self, "content.tos_posture"))
        {
            attachScript(self, "content.tos_posture");
        }
        broadcast(player, "Posture ID set to: " + postureIds[idx]);
        return SCRIPT_CONTINUE;
    }

    public int handleDelayInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        float delay = Float.parseFloat(sui.getInputBoxText(params));
        setObjVar(self, "barkDelay", delay);
        broadcast(player, "Delay set to: " + delay + " seconds.");
        return SCRIPT_CONTINUE;
    }

    public int bark(obj_id self, dictionary details) throws InterruptedException
    {
        String message = details.getString("barkMessage");
        chat.chat(self, message);
        messageTo(self, "bark", details, getFloatObjVar(self, "barkDelay"), false);
        return SCRIPT_CONTINUE;
    }

    public int OnStartNpcConversation(obj_id self, obj_id speaker) throws InterruptedException
    {
        String chatterType1 = getStringObjVar(self, "chatterType1");
        if (chatterType1 == null || chatterType1.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        String[] chatter = new String[4];
        chatter[0] = chatterType1;
        for (int i = 1; i < 4; i++)
        {
            String temp = getStringObjVar(self, "chatterType" + (i + 1));
            chatter[i] = (temp != null && !temp.isEmpty()) ? temp : chatterType1;
        }

        chat.chat(self, chatter[rand(0, 3)]);
        return SCRIPT_CONTINUE;
    }


    public int handleChatterType1(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String message = sui.getInputBoxText(params);
        if (message == null || message.isEmpty())
        {
            broadcast(player, "You must enter a message.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "chatterType1", message);
        broadcast(player, "Chatter Option 1 set to: " + message);
        return SCRIPT_CONTINUE;
    }

    public int handleChatterType2(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String message = sui.getInputBoxText(params);
        if (message == null || message.isEmpty())
        {
            broadcast(player, "You must enter a message.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "chatterType2", message);
        broadcast(player, "Chatter Option 2 set to: " + message);
        return SCRIPT_CONTINUE;
    }

    public int handleChatterType3(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String message = sui.getInputBoxText(params);
        if (message == null || message.isEmpty())
        {
            broadcast(player, "You must enter a message.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "chatterType3", message);
        broadcast(player, "Chatter Option 3 set to: " + message);
        return SCRIPT_CONTINUE;
    }

    public int handleChatterType4(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String message = sui.getInputBoxText(params);
        if (message == null || message.isEmpty())
        {
            broadcast(player, "You must enter a message.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, "chatterType4", message);
        broadcast(player, "Chatter Option 4 set to: " + message);
        return SCRIPT_CONTINUE;
    }
}
