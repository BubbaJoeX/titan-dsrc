package script.library;

import script.*;

/**
 * Server-side script logs: append messages for a player and show them in a "Script Logs" SUI on-demand.
 * Use script_logs.log(player, "message") from any script to add a line.
 * Player opens the console with /developer scriptLogs (god only).
 */
public class script_logs extends script.base_script
{
    private static final String SCRIPTVAR_BUFFER = "script_logs.buffer";
    private static final int MAX_LINES = 500;
    private static final int MAX_BUFFER_CHARS = 100000;
    private static final String SUI_PAGE = "/Script.editScript";
    private static final String SUI_TITLE = "Script Logs";

    public script_logs()
    {
    }

    /**
     * Append a log line for the given player. Safe to call from any script.
     */
    public static void log(obj_id player, String message) throws InterruptedException
    {
        if (player == null || !isIdValid(player))
            return;
        if (message == null)
            message = "";
        String line = "[" + formatTime(getGameTime()) + "] " + message;
        String existing = utils.hasScriptVar(player, SCRIPTVAR_BUFFER) ? utils.getStringScriptVar(player, SCRIPTVAR_BUFFER) : "";
        String combined = existing.length() > 0 ? (existing + "\n" + line) : line;
        String trimmed = trimBuffer(combined);
        utils.setScriptVar(player, SCRIPTVAR_BUFFER, trimmed);
    }

    /**
     * Get current log content for the player.
     */
    public static String getLogContent(obj_id player) throws InterruptedException
    {
        if (player == null || !isIdValid(player))
            return "";
        return utils.hasScriptVar(player, SCRIPTVAR_BUFFER) ? utils.getStringScriptVar(player, SCRIPTVAR_BUFFER) : "";
    }

    /**
     * Clear log buffer for the player.
     */
    public static void clear(obj_id player) throws InterruptedException
    {
        if (player == null || !isIdValid(player))
            return;
        if (utils.hasScriptVar(player, SCRIPTVAR_BUFFER))
            utils.removeScriptVar(player, SCRIPTVAR_BUFFER);
    }

    /**
     * Show the Script Logs SUI for the player (on-demand). Renders current buffer.
     */
    public static boolean show(obj_id player) throws InterruptedException
    {
        if (player == null || !isIdValid(player))
            return false;
        String content = getLogContent(player);
        if (content == null)
            content = "";
        if (content.length() == 0)
            content = "(No script logs yet. Use script_logs.log(player, \"message\") from scripts.)";

        int page = createSUIPage(SUI_PAGE, player, player);
        if (page < 0)
            return false;
        setSUIProperty(page, "pageText.text", "Text", content);
        setSUIProperty(page, "pageText.text", "Editable", "False");
        setSUIProperty(page, "bg.caption.text", "LocalText", SUI_TITLE);
        setSUIProperty(page, "bg.caption.lblTitle", "Text", SUI_TITLE);
        setSUIAssociatedObject(page, player);
        return showSUIPage(page);
    }

    private static String formatTime(int gameTime)
    {
        int sec = gameTime % 60;
        int min = (gameTime / 60) % 60;
        int hr = (gameTime / 3600) % 24;
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    private static String trimBuffer(String combined)
    {
        String[] lines = split(combined, '\n');
        if (lines == null || lines.length <= MAX_LINES)
        {
            if (combined.length() <= MAX_BUFFER_CHARS)
                return combined;
            int from = combined.length() - MAX_BUFFER_CHARS;
            int nl = combined.indexOf('\n', from);
            return nl >= 0 ? combined.substring(nl + 1) : combined.substring(from);
        }
        StringBuilder sb = new StringBuilder(MAX_BUFFER_CHARS + 500);
        int start = lines.length - MAX_LINES;
        for (int i = start; i < lines.length; i++)
        {
            if (sb.length() > 0)
                sb.append('\n');
            sb.append(lines[i]);
        }
        String result = sb.toString();
        if (result.length() > MAX_BUFFER_CHARS)
            result = result.substring(result.length() - MAX_BUFFER_CHARS);
        return result;
    }
}
