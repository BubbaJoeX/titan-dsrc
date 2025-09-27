package script.library;/*
@Origin: dsrc.script.library
@Author: BubbaJoeX
@Purpose: <no purpose>
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Wednesday, 1/29/2025, at 9:09 PM,
@Copyright © SWG: New Beginnings 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.obj_id;
import script.location;
import script.menu_info_types;
import script.menu_info_data;
import script.menu_info;
import script.dictionary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class response_store extends script.base_script
{
    private static final Map<String, List<String>> responseHistory = new HashMap<>();

    public static void addResponse(String prompt, String response)
    {
        if (!responseHistory.containsKey(prompt))
        {
            responseHistory.put(prompt, new ArrayList<>());
        }
        responseHistory.get(prompt).add(response);
    }

    public static void listResponses(obj_id self, String prompt) throws InterruptedException
    {
        List<String> responses = responseHistory.getOrDefault(prompt, new ArrayList<>());
        if (responses.isEmpty())
        {
            debugConsoleMsg(self, "No responses found for the prompt: \"" + prompt + "\".");
        }
        else
        {
            debugConsoleMsg(self, "Responses for prompt: \"" + prompt + "\":");
            for (int i = 0; i < responses.size(); i++)
            {
                debugConsoleMsg(self, (i + 1) + ". " + responses.get(i));
            }
        }
    }

    public static void removeAllResponses(obj_id player)
    {
        responseHistory.clear();
        broadcast(player, "All responses have been removed.");
    }

    public static int printAllPrompts(obj_id player) throws InterruptedException
    {
        // Create a StringBuilder to accumulate all prompts
        StringBuilder promptList = new StringBuilder();

        // Check if there are any prompts in the responseHistory map
        if (response_store.responseHistory.isEmpty())
        {
            promptList.append("No prompts found.");
        }
        else
        {
            // Loop through all prompts in responseHistory and add them to the promptList with line spacings
            for (String prompt : response_store.responseHistory.keySet())
            {
                promptList.append(prompt).append("\n\n");  // Adding double newline for spacing
            }
        }

        // Send the accumulated list of prompts to the player in a SUI message box
        int pid = sui.msgbox(player, player, promptList.toString(), sui.OK_REFRESH, "handleAllPrompts");
        utils.setScriptVar(player, "promptsPid", pid);

        // Return SCRIPT_CONTINUE to continue the script execution
        return SCRIPT_CONTINUE;
    }

    public static int printAllResponses(obj_id player) throws InterruptedException
    {
        // Create a StringBuilder to accumulate all responses
        StringBuilder responseList = new StringBuilder();

        // Check if there are any responses in the responseHistory map
        if (response_store.responseHistory.isEmpty())
        {
            responseList.append("No responses found.");
        }
        else
        {
            // Loop through all prompts in responseHistory and retrieve responses for each prompt
            for (Map.Entry<String, List<String>> entry : response_store.responseHistory.entrySet())
            {
                String prompt = entry.getKey();
                List<String> responses = entry.getValue();

                responseList.append("Responses for prompt: ").append(prompt).append("\n");

                // Loop through all responses for this prompt and add them to the responseList
                for (String response : responses)
                {
                    responseList.append(response).append("\n");  // Adding response
                }
                responseList.append("\n");  // Add a space between different prompt responses
            }
        }

        // Send the accumulated list of responses to the player in a SUI message box
        int pid = sui.msgbox(player, player, responseList.toString(), sui.OK_REFRESH, "handleAllResponses");
        utils.setScriptVar(player, "responsesPid", pid);

        // Return SCRIPT_CONTINUE to continue the script execution
        return SCRIPT_CONTINUE;
    }


    public static void removeResponsesForPrompt(obj_id player, String prompt)
    {
        if (responseHistory.containsKey(prompt))
        {
            responseHistory.remove(prompt);
            broadcast(player, "Responses for prompt: \"" + prompt + "\" have been removed.");
        }
        else
        {
            broadcast(player, "No responses found for the prompt: \"" + prompt + "\" to remove.");
        }
    }
}
