package script.item.content.rewards;/*
@Origin: dsrc.script.item.content.rewards
@Author:  BubbaJoeX
@Purpose: Script to manually choose a low value stat mod
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Saturday, 8/3/2024, at 7:02 AM, 
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.sui;
import script.library.utils;

import java.util.Random;

public class pick_a_stat extends script.terminal.terminal_character_builder
{
    public final int IMBUE_VALUE_GENERAL = 35;
    public final int IMBUE_VALUE_WEAPON = 4;
    public final String IMBUE_VAR = "pick_a_stat.configured";


    public int OnAttach(obj_id self)
    {
        setupEarrings(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        checkForBadEarrings(self);
        setupEarrings(self);
        return SCRIPT_CONTINUE;
    }

    private void checkForBadEarrings(obj_id self)
    {
        //if there are is more than one child under the skillmod.bonus objvar, delete me.
        obj_var_list skillmods = getObjVarList(self, "skillmod.bonus");
        if (skillmods == null)
        {
            return;
        }
        int numItems = skillmods.getNumItems();
        if (numItems > 1)
        {
            LOG("ethereal", "[Jewels]: Found a bad earring. Deleting it from the game.");
            destroyObject(self);
        }
    }

    public void setupEarrings(obj_id item)
    {
        if (hasObjVar(item, "pick_a_stat.configured"))
        {
            setName(item, "Earring of " + getDisplayStat(item));
            setDescriptionString(item, "This earring resonates with the power of " + getDisplayStat(item) + ".");
            setNoTrade(item);
        }
        else
        {
            setName(item, "Earring");
            setDescriptionString(item, "This earring can be resonated with a statistic modifier for use in combat.");
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (canManipulate(player, self, true, true, 15f, true) && !hasObjVar(self, "pick_a_stat.configured") && !utils.hasScriptVar(self, "in_use"))
        {
            mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Resonate Earring"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        utils.setScriptVar(self, "in_use", getPlayerFullName(player));
        sendDirtyObjectMenuNotification(self);
        if (item == menu_info_types.ITEM_USE)
        {
            String title = "Resonate Options";
            String prompt = "Please select a statistic modifer to resonate within this earring.";
            sui.listbox(self, player, prompt, sui.OK_CANCEL, title, getAllStatMods(), "handleImbue");
        }
        if (item == menu_info_types.SERVER_MENU1)
        {
            setupEarrings(self);
        }
        return SCRIPT_CONTINUE;
    }

    public String[] getAllStatMods() throws InterruptedException
    {
        String[] skillMods = dataTableGetStringColumn(EXOTIC_SKILL_MODS, "name");
        for (int i = 0; i < skillMods.length; i++)
        {
            skillMods[i] = utils.packStringId(new string_id("stat_n", skillMods[i]));
        }
        return skillMods;
    }

    public String getDisplayStat(obj_id item)
    {
        obj_var_list skillmods = getObjVarList(item, "skillmod.bonus");
        int numItems = skillmods.getNumItems();
        String finalMod = "";
        for (int i = 0; i < numItems; i++)
        {
            obj_var ov = skillmods.getObjVar(i);
            String name = ov.getName();
            finalMod = localize(new string_id("stat_n", name));
        }
        return finalMod;
    }

    public int handleImbue(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int idx = sui.getListboxSelectedRow(params);
        int button = sui.getIntButtonPressed(params);
        if (button == sui.BP_CANCEL)
        {
            broadcast(player, "Item resonance cancelled.");
            utils.removeScriptVar(self, "in_use");
            return SCRIPT_CONTINUE;
        }
        String[] skillMods = dataTableGetStringColumn(EXOTIC_SKILL_MODS, "name");
        if (idx < 0 || idx >= skillMods.length)
        {
            broadcast(player, "Invalid stat selection. Please try again.");
            return SCRIPT_CONTINUE;
        }
        String skillMod = skillMods[idx];
        if (skillMod.startsWith("expertise_") || skillMod.startsWith("combat_"))
        {
            doSkillModGrantLow(self, player, skillMod);
        }
        else
        {
            doSkillModGrantHigh(self, player, skillMod);
        }
        return SCRIPT_CONTINUE;
    }

    public void doSkillModGrantHigh(obj_id what, obj_id owner, String skillmod) throws InterruptedException
    {
        setSkillModBonus(what, skillmod, IMBUE_VALUE_GENERAL);
        if (!isGod(owner))
        {
            setObjVar(what, IMBUE_VAR, true);
        }
        setNoTrade(what);
        setName(what, "Earring of " + getDisplayStat(what));
        setDescriptionString(what, "This earring resonates with the power of " + getDisplayStat(what) + ".");
        LOG("ethereal", "[Jewels]: Setting expertise/weapon related earring to +4 " + skillmod + ", owned by " + getPlayerFullName(owner));
        detachScript(what, "item.static_item_base");
        utils.removeScriptVar(what, "in_use");
    }

    public void doSkillModGrantLow(obj_id what, obj_id owner, String skillmod) throws InterruptedException
    {
        setSkillModBonus(what, skillmod, IMBUE_VALUE_WEAPON);
        if (!isGod(owner))
        {
            setObjVar(what, IMBUE_VAR, true);
        }
        setName(what, "Earring of " + getDisplayStat(what));
        setDescriptionString(what, "This earring resonates with the power of " + getDisplayStat(what) + ".");
        setNoTrade(what);
        detachScript(what, "item.static_item_base");
        LOG("ethereal", "[Jewels]: Setting general statistic to +35 " + skillmod + ", owned by " + getPlayerFullName(owner));
        utils.removeScriptVar(what, "in_use");
    }

    public void setNoTrade(obj_id self)
    {
        setObjVar(self, "noTrade", 1);
        attachScript(self, "item.special.nomove");
        LOG("ethereal", "[Jewels]: Setting earring to No-Trade");
    }
}
