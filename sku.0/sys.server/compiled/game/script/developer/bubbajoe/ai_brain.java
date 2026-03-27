package script.developer.bubbajoe;/*
@Origin: dsrc.script.developer.bubbajoe
@Author:  BubbaJoeX
@Purpose: Script to handle AI Chat Prompts between two or more objects.
@Requirements: codellama and open webui.
@Notes: Object can be tangible or mobile.
@Created: Wednesday, 1/29/2025, at 6:15 PM,
@Copyright © SWG: Titan 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.chat;
import script.library.openwebui;

public class ai_brain extends script.base_script
{
    public int OnAttach(obj_id self)
    {
        debugSpeakMsg(self, "AI Attached!");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        debugSpeakMsg(self, "AI Attached!");
        return SCRIPT_CONTINUE;
    }

    public int OnHearSpeech(obj_id self, obj_id speaker, String text) throws Exception
    {
        if (!isGod(speaker) || !isMob(self))
        {
            //TEST TEST TEST
            return SCRIPT_CONTINUE;
        }

        if (getDistance(self, speaker) < 4.5f && openwebui.OLLAMA_ENABLED)
        {
            String reply = openwebui.getChatCompletion(openwebui.API_KEY, self, text, speaker);
            if (reply != null && !reply.isEmpty())
            {
                debugSpeakMsg(self, reply);
            }
        }

        return SCRIPT_CONTINUE;
    }
}

