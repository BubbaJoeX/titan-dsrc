package script.bot;/*
@Origin: script.bot.clone
@Author: BubbaJoeX
@Purpose: AI for a stationary entertainer bot.
@Requirements: <no requirements>
@Note: Attach to a creature object. This script will allow players to receive a low-grade entertainer buff.
@Copyright © SWG: Titan 2025
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.ai.ai;
import script.library.*;

public class entertainer extends base_script
{
    private static final float buffDuration = consts.AI_SERVICE_DURATION;
    private static final String BUFF_PROMPT = colors_hex.HEADER + colors_hex.AQUAMARINE + "Do you wish to receive a low-grade enhancement? " + colors_hex.FOOTER;
    private static final String BUFF_TITLE = "MicroData Technologies Entertainment Service";
    private static final String[] buffComponentKeys = {
            "kinetic",
            "energy",
            "reactive_go_with_the_flow"
    };

    private static final int[] buffComponentValues =
            {
                    5,
                    5,
                    10,
            };


    private static final String[] DANCES = {
            "ansl:all_f_dnc_f_belly_loop_high_sp_1.ans",
            "ansl:all_f_dnc_f_belly_loop_mid_sp_2.ans",
            "ansl:all_f_dnc_f_belly_loop_mid.ans"
    };
    public int OnAttach(obj_id self) throws InterruptedException
    {
        doAnimationAction(self, getRandomDance());
        setupEntertainer(self);
        return SCRIPT_CONTINUE;
    }

    public String getRandomDance()
    {
        return DANCES[(int)(Math.random() * DANCES.length)];
    }

    public void setupEntertainer(obj_id self) throws InterruptedException
    {
        setName(self, "Holographic Entertainer");
        setHologramType(self, 3);
        persistObject(self);
        setDescriptionStringId(self, string_id.unlocalized("This entertainer gives low-grade service. It is intended to stay stationary, while providing a low-grade entertainment buff to all those who request it."));
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_STOP);
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setupEntertainer(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Watch Performance"));
        ai.stop(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {

        if (item == menu_info_types.SERVER_MENU1)
        {
            doAnimationAction(self, getRandomDance());
            int pid = sui.createSUIPage(sui.SUI_MSGBOX, self, player, "handleDialogInput");
            sui.setSUIProperty(pid, sui.MSGBOX_PROMPT, sui.PROP_TEXT, BUFF_PROMPT);
            sui.setSUIProperty(pid, sui.MSGBOX_TITLE, sui.PROP_TEXT, BUFF_TITLE);
            sui.msgboxButtonSetup(pid, sui.OK_ONLY);
            sui.setSUIProperty(pid, sui.MSGBOX_BTN_OK, sui.PROP_TEXT, "Allow Enhancement");
            sui.showSUIPage(pid);
            showFlyText(self, new string_id(" - ENHANCEMENT READY - "), 2.0f, colors.DEEPPINK);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleDialogInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isValidId(self))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!isValidId(player))
        {
            return SCRIPT_CONTINUE;
        }
        triggerBuff(player);
        return SCRIPT_CONTINUE;
    }

    public int triggerBuff(obj_id target) throws InterruptedException
    {
        utils.setScriptVar(target, "performance.buildabuff.buffComponentKeys", buffComponentKeys);
        utils.setScriptVar(target, "performance.buildabuff.buffComponentValues", buffComponentValues);
        utils.setScriptVar(target, "performance.buildabuff.player", target);
        buff.applyBuff(target, "general_inspiration", buffDuration, 15);
        buff.applyBuff(target, "burstRun", buffDuration, 2);
        showFlyText(target, new string_id("- ENHANCED - "), 2.0f, color.DEEPPINK);
        broadcast(target, "You have received a low-grade entertainer buff.");
        debugConsoleMsg(target, "For a full-package entertainment buff, please seek out a professional entertainer.");
        return SCRIPT_CONTINUE;
    }
}
