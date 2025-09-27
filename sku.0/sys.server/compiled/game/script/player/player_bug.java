package script.player;/*
@Origin: dsrc.script.player
@Author:  BubbaJoeX
@Purpose: Mantis Bug Reporting script
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Thursday, 1/30/2025, at 1:26 PM, 
@Copyright © SWG: New Beginnings 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.library.chat;
import script.library.sui;
import script.library.utils;
import script.obj_id;
import script.location;
import script.menu_info_types;
import script.menu_info_data;
import script.menu_info;
import script.dictionary;

public class player_bug extends script.base_script
{

    public static final String SUI_BUG = "/Script.Bug";
    public static final String BUG_INSTRUCTIONS = "pageInstructions.text";
    public static final String BUG_DESC = "pageDescription.text";
    public static final String BUG_PAGE_CAPTION = "bg.caption";
    public static final String BUG_BTN_CANCEL = "buttonCancel";
    public static final String BUG_BTN_OK = "buttonSend";
    public static final String BUG_COMBO_TYPE = "comboBugType";
    public static final String BUG_COMBO_TYPE_DATASOURCE = "BugTypes";
    public static final String BUG_COMBO_SUBTYPE = "comboBugSubType";
    public static final String BUG_COMBO_SUBTYPE_DATASOURCE = BUG_COMBO_SUBTYPE + ".DataSource";
    public static final String BUG_COMBO_SYSTEM = "comboSystem";
    public static final String BUG_COMBO_SYSTEM_DATASOURCE = BUG_COMBO_SYSTEM + ".DataSource";
    public static final String BUG_COMBO_SEVERITY = "comboSeverity";
    public static final String BUG_COMBO_SEVERITY_DATASOURCE = "SeverityTypes";
    public static final String BUG_COMBO_REPEAT = "comboRepeatable";
    public static final String BUG_COMBO_REPEAT_DATASOURCE = BUG_COMBO_REPEAT + ".DataSource";

    public static final String[] CATS = {
            "Bazaar and Vendors",
            "Cave, POI, Instance",
            "Character",
            "Chat and Mail",
            "Containers and Storage",
            "Crafting",
            "Exploits",
            "Faction and GCW",
            "[All Projects] General",
            "Generic",
            "Items and Equipment",
            "Missions",
            "NPC, Creature and Lairs",
            "Profession: Beast Master",
            "Profession: Bounty Hunter",
            "Profession: Chronicler",
            "Profession: Commando",
            "Profession: Entertainer",
            "Profession: Jedi",
            "Profession: Medic",
            "Profession: Officer",
            "Profession: Politician",
            "Profession: Smuggler",
            "Profession: Spy",
            "Profession: Trader (All)",
            "Quests",
            "Resource and Spawns",
            "Travel, Vehicles and Mounts",
            "World and Exploration"
    };

    public static String[] SEVERITY = {
            "feature",
            "trival",
            "text",
    };

    public int OnAttach(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        if (text.equals("bug"))
        {
            showBugWindow(self);
        }
        return SCRIPT_CONTINUE;
    }

    public int showBugWindow(obj_id self) throws InterruptedException
    {
        int pid = createSUIPage("/Debug.Bug", self, self, "handleBugWindow");
        setSUIProperty(pid, "", "Location", "500,500");
        setSUIProperty(pid, "", "Size", "1024,768");
        clearSUIDataSource(pid, BUG_COMBO_TYPE_DATASOURCE);
        clearSUIDataSource(pid, BUG_COMBO_SEVERITY_DATASOURCE);

        for (int i = 0; i < SEVERITY.length; i++)
        {
            addSUIDataItem(pid, BUG_COMBO_SEVERITY_DATASOURCE, SEVERITY[i]);
            setSUIProperty(pid, BUG_COMBO_TYPE_DATASOURCE + "." + i, sui.PROP_TEXT, SEVERITY[i]);
        }
        for (int i = 0; i < CATS.length; i++)
        {
            addSUIDataItem(pid, BUG_COMBO_TYPE_DATASOURCE, CATS[i]);
            setSUIProperty(pid, BUG_COMBO_TYPE_DATASOURCE + "." + i, sui.PROP_TEXT, CATS[i]);
        }
        sui.subscribeToSUIProperty(pid, BUG_INSTRUCTIONS, "LocalText");
        sui.subscribeToSUIProperty(pid, BUG_DESC, "LocalText");
        sui.subscribeToSUIProperty(pid, BUG_COMBO_TYPE_DATASOURCE, "LocalText");
        sui.subscribeToSUIProperty(pid, BUG_COMBO_TYPE_DATASOURCE, "SelectedIndex");
        sui.subscribeToSUIProperty(pid, BUG_COMBO_SEVERITY_DATASOURCE, "LocalText");
        sui.subscribeToSUIProperty(pid, BUG_COMBO_SEVERITY_DATASOURCE, "SelectedIndex");
        subscribeToSUIPropertyForEvent(pid, sui_event_type.SET_onButton, BUG_BTN_OK, "", "LocalText");
        subscribeToSUIPropertyForEvent(pid, sui_event_type.SET_onClosedOk, BUG_BTN_CANCEL, "", "LocalText");
        subscribeToSUIPropertyForEvent(pid, sui_event_type.SET_onButton, BUG_BTN_OK, BUG_COMBO_SEVERITY_DATASOURCE, "SelectedIndex");
        subscribeToSUIPropertyForEvent(pid, sui_event_type.SET_onButton, BUG_BTN_OK, BUG_COMBO_TYPE_DATASOURCE, "SelectedIndex");
        flushSUIPage(pid);
        showSUIPage(pid);
        utils.setScriptVar(self, "bug_window", pid);
        return SCRIPT_CONTINUE;
    }

    public int handleBugWindow(obj_id self, dictionary params) throws InterruptedException
    {
        String bugCategory = getSUIDataItem(self, params, BUG_COMBO_TYPE);
        String bugSeverity = getSUIDataItem(self, params, BUG_COMBO_SEVERITY);
        String bugDescription = getSUITextItem(self, params, BUG_DESC);
        String bugInstructions = getSUITextItem(self, params, BUG_INSTRUCTIONS);

        // Handle the collected data, log the bug, or send it somewhere
        if (bugCategory != null && bugSeverity != null && bugDescription != null)
        {
            // Log the bug or send it to the database or external service
            //speak the mssages for now
            chat.chat(self, bugCategory);
            chat.chat(self, bugSeverity);
            chat.chat(self, bugDescription);
            chat.chat(self, bugInstructions);
        }

        // Close the bug reporting window
        forceCloseSUIPage(utils.getIntScriptVar(self, "bug_window"));

        return SCRIPT_CONTINUE;
    }

    private String getSUITextItem(obj_id self, dictionary params, String bugDesc)
    {
        return params.getString(bugDesc + "." + "LocalText");
    }

    private String getSUIDataItem(obj_id self, dictionary params, String dataSource)
    {
        return params.getString(dataSource + "." + "");
    }
}
