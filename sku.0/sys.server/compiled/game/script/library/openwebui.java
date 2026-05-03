package script.library;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import script.location;
import script.obj_id;
import script.string_id;

public class openwebui extends script.base_script {

    public static final boolean OLLAMA_ENABLED = true;

    public static final String API_KEY = "ollama";

    // Updated for large context models (65536)
    public static final int MAX_CONTEXT_TOKENS = 65536;

    // Soft instruction only (no hard truncation conflict)
    public static final String PROMPT_LIMITER =
        "Respond in-character as a Star Wars Galaxies NPC. Keep responses concise unless necessary for clarity. If you are a named celebrity, act as such. **ENSURE ALL TEXT FITS IN CHAT BUBBLE*** Read from context culture and prepare a response that is appropriate: ";

    public static final String MODEL = "mistral-small3.2:latest";

    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        String prompt,
        obj_id speaker
    ) throws Exception {
        if (!OLLAMA_ENABLED) {
            return "";
        }

        String url = "http://swgor.com:11434/api/generate";
        HttpURLConnection connection = (HttpURLConnection) new URL(
            url
        ).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");

        String context = Arrays.toString(buildCulture(target, speaker));

        String jsonBody =
            "{" +
            "\"model\": \"" +
            MODEL +
            "\"," +
            "\"prompt\": \"" +
            escapeJson(PROMPT_LIMITER + context + prompt) +
            "\"," +
            "\"stream\": false" +
            "}";

        OutputStreamWriter writer = new OutputStreamWriter(
            connection.getOutputStream()
        );
        writer.write(jsonBody);
        writer.close();

        connection.connect();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        );
        String line;
        StringBuilder responseText = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }

        String responseString = decodeHtmlEntities(responseText.toString());
        responseString = responseString.replace("\uFEFF", "").trim();

        String responseTag = "\"response\":\"";
        int startIndex = responseString.indexOf(responseTag);

        if (startIndex != -1) {
            startIndex += responseTag.length();
            int endIndex = responseString.indexOf("\"", startIndex);

            if (endIndex != -1) {
                return responseString.substring(startIndex, endIndex).trim();
            }
        }

        return "?";
    }

    public static String getCompletion(String apiKey, String prompt)
        throws Exception {
        if (!OLLAMA_ENABLED) {
            return "[Ollama disabled]";
        }

        String url = "http://swgor.com:11434/api/generate";
        HttpURLConnection connection = (HttpURLConnection) new URL(
            url
        ).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");

        String jsonBody =
            "{" +
            "\"model\": \"" +
            MODEL +
            "\"," +
            "\"prompt\": \"" +
            escapeJson(prompt) +
            "\"," +
            "\"stream\": false" +
            "}";

        OutputStreamWriter writer = new OutputStreamWriter(
            connection.getOutputStream()
        );
        writer.write(jsonBody);
        writer.close();

        connection.connect();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream())
        );
        String line;
        StringBuilder responseText = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }

        String responseString = decodeHtmlEntities(responseText.toString());
        responseString = responseString.replace("\uFEFF", "").trim();

        String responseTag = "\"response\":\"";
        int startIndex = responseString.indexOf(responseTag);

        if (startIndex != -1) {
            startIndex += responseTag.length();
            int endIndex = responseString.indexOf("\"", startIndex);

            if (endIndex != -1) {
                return responseString.substring(startIndex, endIndex).trim();
            }
        }

        return "?";
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static String decodeHtmlEntities(String input) {
        input = input.replaceAll("\\\\u003c", "<");
        input = input.replaceAll("\\\\u003e", ">");
        input = input.replaceAll("\\\\u0026", "&");
        input = input.replaceAll("\\\\n", "\n");
        input = input.replaceAll("\\\\r", "\r");
        input = input.replaceAll("\\\\t", "\t");
        input = input.replaceAll("\\\\", "");
        return input;
    }

    public static String[] buildCulture(obj_id target, obj_id speaker) {
        if (target == null || !isIdValid(target)) {
            String sn = (speaker != null && isIdValid(speaker))
                ? getPlayerFullName(speaker)
                : "?";
            return new String[] { "?", "?", "?", "?", "?", sn, "?", "?", "?" };
        }

        location loc = getLocation(target);
        String speakerName = (speaker != null && isIdValid(speaker))
            ? getPlayerFullName(speaker)
            : "?";
        String currentPlanet = (loc != null && loc.area != null)
            ? loc.area
            : "?";
        String template = getTemplateName(target);
        String name = getEncodedName(target);
        float size = getScale(target);
        String location = (loc != null) ? loc.toReadableFormat(true) : "?";
        String[] scripts = getScriptList(target);
        int species = getSpecies(target);

        string_id descriptionId = getDescriptionStringId(target);
        String description = localize(descriptionId);

        return new String[] {
            currentPlanet,
            template,
            String.valueOf(size),
            location,
            name,
            speakerName,
            String.join(", ", scripts),
            String.valueOf(species),
            description,
        };
    }

    public static String makePromptWIthContext(
        obj_id self,
        obj_id speaker,
        String prompt
    ) {
        String[] c = buildCulture(self, speaker);

        return (
            "\n" +
            "My Current Planet: " +
            c[0] +
            "\n" +
            "My Template: " +
            c[1] +
            "\n" +
            "My Size is: " +
            c[2] +
            "\n" +
            "My Location is: " +
            c[3] +
            "\n" +
            "My Name Is: " +
            c[4] +
            "\n" +
            "Speaker Name: " +
            c[5] +
            "\n" +
            "My Scripts: " +
            c[6] +
            "\n" +
            "My Species: " +
            c[7] +
            "\n" +
            "My Description: " +
            c[8] +
            "\n\n" +
            "You are an NPC in Star Wars Galaxies. Stay in character. Keep responses concise and natural. Prompt: "
        );
    }
}
