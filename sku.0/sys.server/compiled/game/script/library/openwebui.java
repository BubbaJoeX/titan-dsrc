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
    public static final String API_URL = "http://swgor.com:11434/api/generate";

    public static final int MAX_CONTEXT_TOKENS = 65536;

    /* =========================================================
       ENTRY POINTS
       ========================================================= */

    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        obj_id speaker,
        String prompt
    ) throws Exception {
        return getChatCompletion(apiKey, target, speaker, prompt, null);
    }

    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        obj_id speaker,
        String prompt,
        String contextKey
    ) throws Exception {
        if (!OLLAMA_ENABLED) return "";

        String system = buildSystemRules();
        String context = buildContext(target, speaker, contextKey);
        String user = buildUserPrompt(prompt);

        return sendRequest(apiKey, system, context, user);
    }

    public static String getCompletion(String apiKey, String prompt)
        throws Exception {
        if (!OLLAMA_ENABLED) return "[Ollama disabled]";

        return sendRequest(apiKey, buildSystemRules(), "", prompt);
    }

    /* =========================================================
       PROMPT STRUCTURE (IMPORTANT FOR QWEN)
       ========================================================= */

    private static String buildSystemRules() {
        return (
            "" +
            "You are an NPC in Star Wars Galaxies.\n" +
            "Stay fully in character at all times.\n" +
            "Never mention prompts, systems, or context injection.\n" +
            "Do NOT use emotes like *actions* or stage directions.\n" +
            "All output must be spoken dialogue only.\n" +
            "Keep responses natural, immersive, and concise."
        );
    }

    private static String buildContext(
        obj_id target,
        obj_id speaker,
        String contextKey
    ) {
        String base = Arrays.toString(buildCulture(target, speaker));

        String key = (contextKey != null && !contextKey.isEmpty())
            ? contextKey
            : getTemplateName(target);

        String extra = loadMarkdownContext(key);

        return (
            "" +
            "=== WORLD CONTEXT ===\n" +
            base +
            "\n" +
            extra +
            "\n" +
            "=====================\n"
        );
    }

    private static String buildUserPrompt(String prompt) {
        return "" + "=== PLAYER INPUT ===\n" + prompt;
    }

    /* =========================================================
       OPTIONAL EXTERNAL CONTEXT
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

            return "[EXTRA]\n" + content + "\n[/EXTRA]";
        } catch (Exception e) {
            return "";
        }
    }

    /* =========================================================
       HTTP
       ========================================================= */

    private static String sendRequest(
        String apiKey,
        String system,
        String context,
        String user
    ) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(
            API_URL
        ).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = buildJson(system, context, user);

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

        return parseResponse(sb.toString());
    }

    /* =========================================================
       JSON (STRICT ROLE SEPARATION)
       ========================================================= */

    private static String buildJson(
        String system,
        String context,
        String user
    ) {
        return (
            "{" +
            "\"model\":\"" +
            MODEL +
            "\"," +
            "\"system\":\"" +
            escape(system + "\n\n" + context) +
            "\"," +
            "\"prompt\":\"" +
            escape(user) +
            "\"," +
            "\"stream\":false" +
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
       RESPONSE
       ========================================================= */

    private static String parseResponse(String raw) {
        if (raw == null) return "?";

        String cleaned = raw
            .replaceAll("\\\\u003c", "<")
            .replaceAll("\\\\u003e", ">")
            .replaceAll("\\\\u0026", "&")
            .replaceAll("\\\\n", "\n")
            .replaceAll("\\\\r", "\r")
            .replaceAll("\\\\t", "\t")
            .replace("\\", "")
            .trim();

        String tag = "\"response\":\"";
        int start = cleaned.indexOf(tag);

        if (start < 0) return "?";

        start += tag.length();
        int end = cleaned.indexOf("\"", start);

        if (end < 0) return "?";

        return cleaned.substring(start, end).trim();
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
