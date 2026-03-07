package script.terminal;

import script.*;
import script.library.*;

/**
 * Calendar Terminal - Provides access to the in-game calendar system.
 * Attach to a terminal object to allow players to access the calendar.
 */
public class terminal_calendar extends script.base_script
{
    // Radial menu constants
    public static final int MENU_OPEN_CALENDAR = 200;
    public static final int MENU_CREATE_EVENT = 201;
    public static final int MENU_UPCOMING_EVENTS = 202;
    public static final int MENU_SETTINGS = 203;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "Calendar Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setName(self, "Calendar Terminal");
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isIdValid(player) || !isPlayer(player))
            return SCRIPT_CONTINUE;

        // Main calendar option
        int rootMenu = mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("ui_calendar", "open_calendar"));
        mi.addSubMenu(rootMenu, MENU_OPEN_CALENDAR, new string_id("ui_calendar", "view_calendar"));
        mi.addSubMenu(rootMenu, MENU_UPCOMING_EVENTS, new string_id("ui_calendar", "upcoming_events"));

        // Create event - only if player has permission
        boolean canCreate = false;
        for (int type = 0; type <= 3; type++)
        {
            if (calendar.canCreateEvent(player, type))
            {
                canCreate = true;
                break;
            }
        }

        if (canCreate)
        {
            mi.addSubMenu(rootMenu, MENU_CREATE_EVENT, new string_id("ui_calendar", "create_event"));
        }

        // Settings - staff only
        if (isGod(player))
        {
            mi.addSubMenu(rootMenu, MENU_SETTINGS, new string_id("ui_calendar", "settings"));
        }

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isIdValid(player) || !isPlayer(player))
            return SCRIPT_CONTINUE;

        // Ensure player has calendar script
        if (!hasScript(player, "player.player_calendar"))
            attachScript(player, "player.player_calendar");

        switch (item)
        {
            case MENU_OPEN_CALENDAR:
                openCalendar(player);
                break;

            case MENU_CREATE_EVENT:
                openCalendarWithCreate(player);
                break;

            case MENU_UPCOMING_EVENTS:
                showUpcomingEvents(player);
                break;

            case MENU_SETTINGS:
                if (isGod(player))
                    openCalendarWithSettings(player);
                break;
        }

        return SCRIPT_CONTINUE;
    }

    private void openCalendar(obj_id player) throws InterruptedException
    {
        dictionary params = new dictionary();
        messageTo(player, "openCalendar", params, 0, false);
    }

    private void openCalendarWithCreate(obj_id player) throws InterruptedException
    {
        dictionary params = new dictionary();
        messageTo(player, "openCalendar", params, 0, false);

        // Delay the create event trigger slightly to let the calendar open first
        dictionary createParams = new dictionary();
        messageTo(player, "handleCreateEvent", createParams, 0.5f, false);
    }

    private void openCalendarWithSettings(obj_id player) throws InterruptedException
    {
        dictionary params = new dictionary();
        messageTo(player, "openCalendar", params, 0, false);

        // Delay the settings trigger slightly
        dictionary settingsParams = new dictionary();
        messageTo(player, "handleSettings", settingsParams, 0.5f, false);
    }

    private void showUpcomingEvents(obj_id player) throws InterruptedException
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

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
            return SCRIPT_CONTINUE;

        names[idx] = "calendar_terminal";
        attribs[idx] = "Access the Galactic Calendar to view events.";
        idx++;

        if (idx < names.length)
        {
            names[idx] = "calendar_help";
            attribs[idx] = "Use '/calendar' to open the calendar anywhere.";
        }

        return SCRIPT_CONTINUE;
    }
}

