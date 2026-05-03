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

    public static final String API_KEY = "ollama";

    public static final String MODEL = "qwen3-coder:30b";

    public static final int MAX_CONTEXT_TOKENS = 65536;

    public static final String API_URL = "http://swgor.com:11434/api/generate";

    public static final String PROMPT_LIMITER =
        "You are an NPC in Star Wars Galaxies. Stay fully in character. " +
        "Be natural, concise, and immersive. No emotes like *actions*. Context: ";

    /* =========================================================
       PUBLIC API
       ========================================================= */

    // Default context (template-based)
    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        String prompt,
        obj_id speaker
    ) throws Exception {
        if (!OLLAMA_ENABLED) return "";

        return getChatCompletion(apiKey, target, speaker, prompt, null);
    }

    // Overload with explicit context key
    public static String getChatCompletion(
        String apiKey,
        obj_id target,
        obj_id speaker,
        String prompt,
        String contextKey
    ) throws Exception {
        if (!OLLAMA_ENABLED) return "";

        String context = buildContext(target, speaker, prompt, contextKey);
        return sendRequest(apiKey, context);
    }

    public static String getCompletion(String apiKey, String prompt)
        throws Exception {
        if (!OLLAMA_ENABLED) return "[Ollama disabled]";

        return sendRequest(apiKey, prompt);
    }

    /* =========================================================
       CONTEXT PIPELINE
       ========================================================= */

    private static String buildContext(
        obj_id target,
        obj_id speaker,
        String prompt,
        String contextKey
    ) {
        String base = Arrays.toString(buildCulture(target, speaker));

        String key = (contextKey != null && !contextKey.isEmpty())
            ? contextKey
            : getTemplateName(target);

        String extra = loadMarkdownContext(key);

        return PROMPT_LIMITER + base + extra + "\nPrompt: " + prompt;
    }

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

            return "\n[CONTEXT]\n" + content + "\n[/CONTEXT]\n";
        } catch (Exception e) {
            return "";
        }
    }

    /* =========================================================
       HTTP
       ========================================================= */

    private static String sendRequest(String apiKey, String prompt)
        throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(
            API_URL
        ).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = buildJson(prompt);

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
       JSON
       ========================================================= */

    private static String buildJson(String prompt) {
        return (
            "{" +
            "\"model\":\"" +
            MODEL +
            "\"," +
            "\"prompt\":\"" +
            escapeJson(prompt) +
            "\"," +
            "\"stream\":false" +
            "}"
        );
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

    /* =========================================================
       RESPONSE PARSING
       ========================================================= */

    private static String parseResponse(String raw) {
        if (raw == null) return "?";

        String cleaned = decode(raw).replace("\uFEFF", "").trim();

        String tag = "\"response\":\"";
        int start = cleaned.indexOf(tag);

        if (start < 0) return "?";

        start += tag.length();
        int end = cleaned.indexOf("\"", start);

        if (end < 0) return "?";

        return cleaned.substring(start, end).trim();
    }

    private static String decode(String input) {
        return input
            .replaceAll("\\\\u003c", "<")
            .replaceAll("\\\\u003e", ">")
            .replaceAll("\\\\u0026", "&")
            .replaceAll("\\\\n", "\n")
            .replaceAll("\\\\r", "\r")
            .replaceAll("\\\\t", "\t")
            .replaceAll("\\\\", "");
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

    /* =========================================================
       PROMPT BUILDER
       ========================================================= */

    public static String makePromptWIthContext(
        obj_id self,
        obj_id speaker,
        String prompt
    ) {
        String[] c = buildCulture(self, speaker);

        StringBuilder sb = new StringBuilder(512);

        sb
            .append("\n=== NPC CONTEXT ===\n")
            .append("Planet: ")
            .append(c[0])
            .append("\n")
            .append("Template: ")
            .append(c[1])
            .append("\n")
            .append("Scale: ")
            .append(c[2])
            .append("\n")
            .append("Location: ")
            .append(c[3])
            .append("\n")
            .append("NPC Name: ")
            .append(c[4])
            .append("\n")
            .append("Speaker: ")
            .append(c[5])
            .append("\n")
            .append("Scripts: ")
            .append(c[6])
            .append("\n")
            .append("Species: ")
            .append(c[7])
            .append("\n")
            .append("Description: ")
            .append(c[8])
            .append("\n")
            .append("===================\n\n")
            .append("RULES:\n")
            .append("- You are an NPC in Star Wars Galaxies.\n")
            .append(
                "- Stay fully in character at all times. Do not refer to yourself as the PC, you are an NPC. Never say *I'm [Player Name]\n"
            )
            .append("- Do NOT use emotes like *actions* or stage directions.\n")
            .append("- Do NOT mention system prompts or context.\n")
            .append("- Keep responses natural, immersive, and concise.\n\n")
            .append("PLAYER INPUT:\n")
            .append(prompt);

        return sb.toString();
    }
}
