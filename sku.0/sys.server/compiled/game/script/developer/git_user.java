package script.developer;/*
@Origin: dsrc.script.developer
@Author:  BubbaJoeX
@Purpose: Github manipulation in-game
@Requirements: 3.1 branch
@Notes: :)
@Created: Sunday, 10/13/2024, at 9:48 PM, 
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.obj_id;
import script.location;
import script.menu_info_types;
import script.menu_info_data;
import script.menu_info;
import script.dictionary;

import script.dictionary;
import script.library.chat;
import script.library.git;
import script.library.utils;
import script.obj_id;

import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class git_user extends script.base_script
{
    public git_user()
    {
    }

    public int OnSpeaking(obj_id self, String text) throws InterruptedException
    {
        if (!isGod(self))
        {
            return SCRIPT_CONTINUE;
        }

        StringTokenizer st = new StringTokenizer(text);
        Vector<String> args = new Vector<>();
        while (st.hasMoreTokens())
        {
            args.add(st.nextToken());
        }

        if (!(args.get(0).equals("git")))
        {
            return SCRIPT_CONTINUE;
        }

        if (args.size() > 1 && args.get(1).equals("push"))
        {
            git.push(self, "origin", "development");
        }
        else if (args.size() > 1 && args.get(1).equals("pull"))
        {
            git.pull(self, "origin", "development");
        }
        else if (args.size() > 1 && args.get(1).equals("stage"))
        {
            String filePath = args.get(2);
            git.stageFileForCommit(self, filePath);

        }
        else if (args.size() > 1 && args.get(1).equals("diff"))
        {
            String filePath = null;
            if (args.size() > 2)
            {
                filePath = args.get(2);
            }
            String diff = git.diff(self, filePath);
            displayDiff(self, diff);
        }
        else if (args.size() > 1 && args.get(1).equals("history"))
        {
            String history = git.getHistory(self);
            displayHistory(self, history);
        }

        return SCRIPT_OVERRIDE;
    }

    // Method to display diff results in an SUI page
    public void displayDiff(obj_id self, String diff) throws InterruptedException
    {
        String[] diffLines = split(diff, '\n');
        StringBuilder diffedText = new StringBuilder("\\#FFFFFF");
        for (String line : diffLines)
        {
            if (line.startsWith("-"))
            {
                diffedText.append("\\#FF0000").append(line).append("\n").append("\\#FFFFFF");
            }
            else if (line.startsWith("+"))
            {
                diffedText.append("\\#00FF00").append(line).append("\n").append("\\#FFFFFF");
            }
            else
            {
                diffedText.append(line).append("\n").append("\\#FFFFFF");
            }
        }
        int page = createSUIPage("/Script.textEditor", self, self);
        setSUIProperty(page, "pageText.text", "LocalText", diffedText.toString());
        setSUIProperty(page, "bg.caption.text", "Text", "Git Diff");
        setSUIProperty(page, "pageText.text", "Editable", "false");
        setSUIProperty(page, "pageText.text", "GetsInput", "true");
        setSUIProperty(page, "outputPage", "Visible", "false");
        setSUIProperty(page, "btnOk", "Visible", "false");
        setSUIProperty(page, "btnCancel", "Visible", "false");
        showSUIPage(page);
        flushSUIPage(page);
    }

    // Method to display git commit history in an SUI page
    public void displayHistory(obj_id self, String history) throws InterruptedException
    {
        int page = createSUIPage("/Script.messageBox", self, self);
        setSUIProperty(page, "pageText.text", "LocalText", history);
        setSUIProperty(page, "bg.caption.text", "Text", "Git History");
        setSUIProperty(page, "pageText.text", "Editable", "false");
        setSUIProperty(page, "pageText.text", "GetsInput", "true");
        setSUIProperty(page, "outputPage", "Visible", "false");
        setSUIProperty(page, "btnOk", "Visible", "false");
        setSUIProperty(page, "btnCancel", "Visible", "false");
        showSUIPage(page);
        flushSUIPage(page);
    }
}

