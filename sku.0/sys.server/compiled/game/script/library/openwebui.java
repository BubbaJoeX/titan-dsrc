package script.library;

import script.obj_id;
import script.string_id;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class openwebui extends script.base_script
{

    public static final String API_KEY = "sk-15c09f1f547549c5b0553525a45ddf89";
    public static final String PROMPT_LIMITER = "Respond to the prompt in character, with no more than 200 characters, unless you need to finish the sentence but you MUST keep it short. Context: ";

    public static String getChatCompletion(String apiKey, obj_id target, String prompt, obj_id speaker) throws Exception
    {
        String url = "http://swgor.com:8888/ollama/api/generate";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");

        String jsonBody = "{"
                + "\"model\": \"codellama\","
                + "\"prompt\": \"" + PROMPT_LIMITER + Arrays.toString(buildCulture(target, speaker)) + prompt + "\","
                + "\"stream\": false"
                + "}";

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(jsonBody);
        writer.close();

        // Read response
        connection.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder responseText = new StringBuilder();
        while ((line = reader.readLine()) != null)
        {
            responseText.append(line);
        }

        String responseString = responseText.toString();
        responseString = decodeHtmlEntities(responseString);

        responseString = responseString.replace("\uFEFF", "").trim();

        String responseTag = "\"response\":\"";
        int startIndex = responseString.indexOf(responseTag);
        if (startIndex != -1)
        {
            startIndex += responseTag.length();
            int endIndex = responseString.indexOf("\"", startIndex);
            if (endIndex != -1)
            {
                // Return decoded response content
                return responseString.substring(startIndex, endIndex).trim();
            }
        }

        return "?";
    }

    public static String getCompletion(String apiKey, String prompt) throws Exception
    {
        String url = "http://swgor.com:8888/ollama/api/generate";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");

        // Manually create JSON body as String
        String jsonBody = "{"
                + "\"model\": \"llama3:latest\","
                + "\"prompt\": \"" + prompt + "\","
                + "\"stream\": false"
                + "}";

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(jsonBody);
        writer.close();

        connection.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder responseText = new StringBuilder();
        while ((line = reader.readLine()) != null)
        {
            responseText.append(line);
        }

        String responseString = responseText.toString();

        responseString = decodeHtmlEntities(responseString);

        responseString = responseString.replace("\uFEFF", "").trim();
        String responseTag = "\"response\":\"";
        int startIndex = responseString.indexOf(responseTag);
        if (startIndex != -1)
        {
            startIndex += responseTag.length();
            int endIndex = responseString.indexOf("\"", startIndex);
            if (endIndex != -1)
            {
                return responseString.substring(startIndex, endIndex).trim();
            }
        }

        return "?";
    }

    private static String decodeHtmlEntities(String input)
    {
        input = input.replaceAll("\\\\u003c", "<");
        input = input.replaceAll("\\\\u003e", ">");
        input = input.replaceAll("\\\\u0026", "&");
        input = input.replaceAll("\\\\n", "\n");
        input = input.replaceAll("\\\\r", "\r");
        input = input.replaceAll("\\\\t", "\t");
        input = input.replaceAll("\\\\", "");

        return input;
    }

    public static String[] buildCulture(obj_id target, obj_id speaker)
    {
        String speakerName = getPlayerFullName(speaker);
        String currentPlanet = getLocation(target).area;
        String template = getTemplateName(target);
        String name = getEncodedName(target);
        float size = getScale(target);
        String location = getLocation(target).toReadableFormat(true);
        String[] scripts = getScriptList(target);
        int species = getSpecies(target);

        string_id descriptionId = getDescriptionStringId(target);
        String description = localize(descriptionId);

        return new String[]{currentPlanet, template, String.valueOf(size), location, name, speakerName, String.join(", ", scripts), String.valueOf(species), description};
    }

    public static String makePromptWIthContext(obj_id self, obj_id speaker, String prompt)
    {
        String[] cultureData = buildCulture(self, speaker);
        String main = "\n";
        main += "\nMy Current Planet: " + cultureData[0];
        main += "\nMy Template: " + cultureData[1];
        main += "\nMy Size is: " + cultureData[2];
        main += "\nMy Location is: " + cultureData[3];
        main += "\nMy Name Is: " + cultureData[4];
        main += "\nSpeaker Name: " + cultureData[5];
        main += "\nMy Scripts: " + cultureData[6];
        main += "\nMy Species: " + cultureData[7];
        main += "\nMy Description: " + cultureData[8];
        main += "\n";
        main += "You are an NPC in Star Wars Galaxies. Use the previous context to respond to the following prompt. The Speaker Name is the person writing the prompt. Never repeat any contextual values unless specifically asked for. Never repeat them their name. Never state that you are an AI. Also, keep the text length short and sweet. Prompt: ";
        return main;
    }

}
