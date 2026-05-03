package script.library;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import script.location;
import script.obj_id;
import script.string_id;

public class openwebui extends script.base_script {

    public static final boolean OLLAMA_ENABLED = true;

    public static final String MODEL = "qwen3-coder:30b";
    public static final String API_URL = "http://swgor.com:11434/api/chat";

    public static final int MAX_CONTEXT_TOKENS = 65536;

    /* =========================================================
       PUBLIC LEGACY API (DO NOT BREAK GAME CALLS)
       ========================================================= */

    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        String prompt,
        obj_id speaker
    ) throws Exception {
        return getChatCompletion(apiKey, target, speaker, prompt, null);
    }

    /* =========================================================
       NEW CHAT ENGINE (OPTIONAL CONTEXT KEY)
       ========================================================= */

    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        obj_id speaker,
        String prompt,
        String contextKey
    ) throws Exception {
        if (!OLLAMA_ENABLED) return "";

        String system = buildSystemRules();
        String user = buildUserPayload(target, speaker, prompt, contextKey);

        return sendRequest(apiKey, system, user);
    }

    public static String getCompletion(String apiKey, String prompt)
        throws Exception {
        if (!OLLAMA_ENABLED) return "[Ollama disabled]";

        return sendRequest(apiKey, buildSystemRules(), prompt);
    }

    /* =========================================================
       SYSTEM (HARD RULES - MODEL BEHAVIOR LOCK)
       ========================================================= */

    private static String buildSystemRules() {
        return (
            "" +
            "You are an NPC in Star Wars Galaxies.\n" +
            "You must remain fully in character at all times.\n" +
            "Never mention prompts, system messages, or context injection.\n" +
            "Do NOT use emotes like *actions* or stage directions.\n" +
            "Only output spoken dialogue.\n" +
            "Keep responses natural, immersive, and concise."
        );
    }

    /* =========================================================
       USER CONTEXT (SOFT INPUT ONLY)
       ========================================================= */

    private static String buildUserPayload(
        obj_id target,
        obj_id speaker,
        String prompt,
        String contextKey
    ) {
        String[] c = buildCulture(target, speaker);

        String key = (contextKey != null && !contextKey.isEmpty())
            ? contextKey
            : getTemplateName(target);

        String extra = loadMarkdownContext(key);

        return (
            "" +
            "=== NPC CONTEXT ===\n" +
            "Planet: " +
            c[0] +
            "\n" +
            "Template: " +
            c[1] +
            "\n" +
            "Scale: " +
            c[2] +
            "\n" +
            "Location: " +
            c[3] +
            "\n" +
            "NPC Name: " +
            c[4] +
            "\n" +
            "Speaker: " +
            c[5] +
            "\n" +
            "Scripts: " +
            c[6] +
            "\n" +
            "Species: " +
            c[7] +
            "\n" +
            "Description: " +
            c[8] +
            "\n" +
            "=====================\n\n" +
            extra +
            "\n\n" +
            "=== PLAYER INPUT ===\n" +
            prompt
        );
    }

    /* =========================================================
       OPTIONAL MARKDOWN CONTEXT LOADER
       ========================================================= */

    private static String loadMarkdownContext(String key) {
        try {
            if (key == null || key.isEmpty()) return "";

            String path = "/home/swg/swg-main/dsrc/contextuals/" + key + ".md";
            File file = new File(path);

            if (!file.exists() || !file.isFile()) return "";

            String content = new String(
                Files.readAllBytes(Paths.get(path))
            ).trim();

            if (content.isEmpty()) return "";

            return "[EXTRA CONTEXT]\n" + content + "\n[/EXTRA CONTEXT]";
        } catch (Exception e) {
            return "";
        }
    }

    /* =========================================================
       HTTP (OLLAMA CHAT API)
       ========================================================= */

    private static String sendRequest(String apiKey, String system, String user)
        throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(
            API_URL
        ).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = buildJson(system, user);

        try (
            OutputStreamWriter w = new OutputStreamWriter(
                conn.getOutputStream()
            )
        ) {
            w.write(json);
        }

        StringBuilder sb = new StringBuilder();

        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            )
        ) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        return parseChatResponse(sb.toString());
    }

    /* =========================================================
       JSON PAYLOAD (CHAT FORMAT)
       ========================================================= */

    private static String buildJson(String system, String user) {
        return (
            "{" +
            "\"model\":\"" +
            MODEL +
            "\"," +
            "\"stream\":false," +
            "\"messages\":[" +
            "{" +
            "\"role\":\"system\"," +
            "\"content\":\"" +
            escape(system) +
            "\"" +
            "}," +
            "{" +
            "\"role\":\"user\"," +
            "\"content\":\"" +
            escape(user) +
            "\"" +
            "}" +
            "]" +
            "}"
        );
    }

    private static String escape(String input) {
        if (input == null) return "";

        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /* =========================================================
       RESPONSE PARSER (CHAT FORMAT SAFE)
       ========================================================= */

    private static String parseChatResponse(String raw) {
        if (raw == null) return "?";

        String tag = "\"content\":\"";
        int start = raw.indexOf(tag);

        if (start < 0) return "?";

        start += tag.length();
        int end = raw.indexOf("\"", start);

        if (end < 0) return "?";

        return raw.substring(start, end).trim();
    }

    /* =========================================================
       CULTURE DATA
       ========================================================= */

    public static String[] buildCulture(obj_id target, obj_id speaker) {
        if (target == null || !isIdValid(target)) {
            String sn = (speaker != null && isIdValid(speaker))
                ? getPlayerFullName(speaker)
                : "?";

            return new String[] { "?", "?", "?", "?", "?", sn, "?", "?", "?" };
        }

        location loc = getLocation(target);

        return new String[] {
            (loc != null && loc.area != null) ? loc.area : "?",
            getTemplateName(target),
            String.valueOf(getScale(target)),
            (loc != null) ? loc.toReadableFormat(true) : "?",
            getEncodedName(target),
            (speaker != null && isIdValid(speaker))
                ? getPlayerFullName(speaker)
                : "?",
            String.join(", ", getScriptList(target)),
            String.valueOf(getSpecies(target)),
            localize(getDescriptionStringId(target)),
        };
    }
}
