package script.command;

import script.*;
import script.library.*;

/**
 * Command handler for /calendar command.
 * Opens the in-game calendar CUI system.
 *
 * Note: All calendar data exchange now happens via network messages,
 * not commands. This command only opens the UI.
 */
public class command_calendar extends script.base_script
{
    public int OnAttach(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    /**
     * /calendar - Opens the calendar
     * /calendar help - Show help
     */
    public int cmdCalendar(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        if (!isIdValid(self) || !isPlayer(self))
            return SCRIPT_CONTINUE;

        if (params == null || params.trim().isEmpty())
        {
            // Open main calendar CUI
            calendar.openCalendarCUI(self);
            return SCRIPT_CONTINUE;
        }

        String[] args = params.trim().split("\\s+");
        String subCommand = args[0].toLowerCase();

        switch (subCommand)
        {
            case "help":
                showHelp(self);
                break;

            case "list":
                // Quick list of upcoming events (still useful as text output)
                handleListCommand(self);
                break;

            default:
                // For any other command, just open the calendar
                calendar.openCalendarCUI(self);
                break;
        }

        return SCRIPT_CONTINUE;
    }

    private void handleListCommand(obj_id player) throws InterruptedException
    {
        dictionary[] events = calendar.getEventsForPlayer(player);

        if (events == null || events.length == 0)
        {
            sendSystemMessageTestingOnly(player, "\\#888888No upcoming events found.");
            return;
        }

        sendSystemMessageTestingOnly(player, " ");
        sendSystemMessageTestingOnly(player, "\\#FFD700========== Upcoming Events ==========\\#FFFFFF");

        int shown = 0;
        int currentYear = calendar.getCurrentYear();
        int currentMonth = calendar.getCurrentMonth();
        int currentDay = calendar.getCurrentDay();

        for (dictionary evt : events)
        {
            if (shown >= 10)
            {
                sendSystemMessageTestingOnly(player, "\\#aaaaaa... and " + (events.length - 10) + " more events. Use /calendar to see all.");
                break;
            }

            int evtYear = evt.getInt(calendar.KEY_YEAR);
            int evtMonth = evt.getInt(calendar.KEY_MONTH);
            int evtDay = evt.getInt(calendar.KEY_DAY);

            // Only show future events
            if (evtYear < currentYear)
                continue;
            if (evtYear == currentYear && evtMonth < currentMonth)
                continue;
            if (evtYear == currentYear && evtMonth == currentMonth && evtDay < currentDay)
                continue;

            String title = evt.getString(calendar.KEY_TITLE);
            int evtType = evt.getInt(calendar.KEY_EVENT_TYPE);
            int hour = evt.getInt(calendar.KEY_HOUR);
            int minute = evt.getInt(calendar.KEY_MINUTE);

            String color = calendar.getEventTypeColor(evtType);
            String typeName = calendar.getEventTypeName(evtType);
            String dateStr = calendar.formatDate(evtYear, evtMonth, evtDay);
            String timeStr = calendar.formatTime(hour, minute);

            sendSystemMessageTestingOnly(player, color + "[" + typeName + "] " + title + "\\#FFFFFF");
            sendSystemMessageTestingOnly(player, "\\#aaaaaa  " + dateStr + " at " + timeStr);

            shown++;
        }

        if (shown == 0)
        {
            sendSystemMessageTestingOnly(player, "\\#888888No upcoming events found.");
        }

        sendSystemMessageTestingOnly(player, "\\#FFD700======================================\\#FFFFFF");
        sendSystemMessageTestingOnly(player, " ");
    }

    private void showHelp(obj_id player) throws InterruptedException
    {
        sendSystemMessageTestingOnly(player, " ");
        sendSystemMessageTestingOnly(player, "\\#FFD700========== Calendar Commands ==========\\#FFFFFF");
        sendSystemMessageTestingOnly(player, "\\#00ff00/calendar\\#FFFFFF - Open the calendar interface");
        sendSystemMessageTestingOnly(player, "\\#00ff00/calendar list\\#FFFFFF - List upcoming events");
        sendSystemMessageTestingOnly(player, "\\#00ff00/calendar help\\#FFFFFF - Show this help");
        sendSystemMessageTestingOnly(player, " ");
        sendSystemMessageTestingOnly(player, "\\#aaaaaa Event Types:");
        sendSystemMessageTestingOnly(player, "\\#FFD700  Staff Events\\#FFFFFF - Galaxy-wide announcements (Staff only)");
        sendSystemMessageTestingOnly(player, "\\#FF4500  Server Events\\#FFFFFF - Holiday events (Staff only)");
        sendSystemMessageTestingOnly(player, "\\#00FF00  Guild Events\\#FFFFFF - Visible to guild members");
        sendSystemMessageTestingOnly(player, "\\#00BFFF  City Events\\#FFFFFF - Visible to city citizens");
        sendSystemMessageTestingOnly(player, "\\#FFD700========================================\\#FFFFFF");
        sendSystemMessageTestingOnly(player, " ");
    }
}

