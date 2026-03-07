package script.player;

import script.*;
import script.library.*;

/**
 * Player Calendar Script - Handles the in-game calendar UI.
 * Uses custom UI page: Script.CalendarSystem
 */
public class player_calendar extends script.base_script
{
    public static final String SCRIPT_NAME = "player.player_calendar";

    // SUI Page paths
    public static final String SUI_CALENDAR = "Script.CalendarSystem";
    public static final String SUI_EVENT_EDITOR = "Script.CalendarEventEditor";
    public static final String SUI_SETTINGS = "Script.CalendarSettings";

    // SUI Component paths - Main Calendar
    public static final String COMP_MONTH_YEAR = "calendarWindow.header.lblMonthYear";
    public static final String COMP_SELECTED_DATE = "calendarWindow.eventPanel.lblSelectedDate";
    public static final String COMP_EVENTS_LIST = "calendarWindow.eventPanel.lstEvents";
    public static final String COMP_EVENTS_DATA = "calendarWindow.eventPanel.dataEvents";
    public static final String COMP_BTN_SETTINGS = "calendarWindow.header.btnSettings";
    public static final String COMP_BTN_DELETE = "calendarWindow.eventPanel.btnDeleteEvent";

    // SUI Component paths - Event Editor
    public static final String EDITOR_TITLE = "eventEditor.titleSection.txtTitle";
    public static final String EDITOR_DESC = "eventEditor.descSection.txtDescription";
    public static final String EDITOR_TYPE = "eventEditor.typeSection.cmbEventType";
    public static final String EDITOR_DATE = "eventEditor.dateSection.txtDate";
    public static final String EDITOR_TIME = "eventEditor.dateSection.txtTime";
    public static final String EDITOR_DURATION = "eventEditor.durationSection.cmbDuration";
    public static final String EDITOR_SERVER_EVENT = "eventEditor.serverEventSection.cmbServerEvent";
    public static final String EDITOR_SERVER_SECTION = "eventEditor.serverEventSection";
    public static final String EDITOR_BROADCAST = "eventEditor.optionsSection.chkBroadcast";
    public static final String EDITOR_RECURRING = "eventEditor.optionsSection.chkRecurring";
    public static final String EDITOR_RECUR_SECTION = "eventEditor.recurrenceSection";
    public static final String EDITOR_RECUR_TYPE = "eventEditor.recurrenceSection.cmbRecurrence";

    // SUI Component paths - Settings
    public static final String SETTINGS_TEXTURE = "settingsWindow.textureSection.txtTexture";
    public static final String SETTINGS_RECT_X = "settingsWindow.rectSection.txtRectX";
    public static final String SETTINGS_RECT_Y = "settingsWindow.rectSection.txtRectY";
    public static final String SETTINGS_RECT_W = "settingsWindow.rectSection.txtRectW";
    public static final String SETTINGS_RECT_H = "settingsWindow.rectSection.txtRectH";

    // Script vars
    public static final String VAR_BASE = "calendar";
    public static final String VAR_SUI_PID = VAR_BASE + ".sui_pid";
    public static final String VAR_EDITOR_PID = VAR_BASE + ".editor_pid";
    public static final String VAR_SETTINGS_PID = VAR_BASE + ".settings_pid";
    public static final String VAR_YEAR = VAR_BASE + ".year";
    public static final String VAR_MONTH = VAR_BASE + ".month";
    public static final String VAR_SELECTED_DAY = VAR_BASE + ".selected_day";
    public static final String VAR_SELECTED_EVENTS = VAR_BASE + ".selected_events";
    public static final String VAR_CREATE_DATA = VAR_BASE + ".create_data";

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public int OnAttach(obj_id self) throws InterruptedException
    {
        return SCRIPT_CONTINUE;
    }

    public int OnDetach(obj_id self) throws InterruptedException
    {
        closeCalendar(self);
        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // Calendar Data Change Handler (for notifications from other players)
    // =========================================================================

    /**
     * Called when another player creates, updates, or deletes an event
     * that this player should know about.
     */
    public int onCalendarDataChanged(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null)
            return SCRIPT_CONTINUE;

        String action = params.getString("action");

        // If the calendar UI is open, refresh the display
        if (utils.hasScriptVar(self, VAR_SUI_PID))
        {
            int pid = utils.getIntScriptVar(self, VAR_SUI_PID);
            if (pid > 0)
            {
                // Get current view state
                int year = utils.hasScriptVar(self, VAR_YEAR) ? utils.getIntScriptVar(self, VAR_YEAR) : calendar.getCurrentYear();
                int month = utils.hasScriptVar(self, VAR_MONTH) ? utils.getIntScriptVar(self, VAR_MONTH) : calendar.getCurrentMonth();
                int day = utils.hasScriptVar(self, VAR_SELECTED_DAY) ? utils.getIntScriptVar(self, VAR_SELECTED_DAY) : calendar.getCurrentDay();

                // Refresh the calendar display with new data
                updateCalendarDisplay(self, pid, year, month, day);
            }
        }

        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // Calendar Open/Close
    // =========================================================================

    public int openCalendar(obj_id self, dictionary params) throws InterruptedException
    {
        int year = calendar.getCurrentYear();
        int month = calendar.getCurrentMonth();
        int day = calendar.getCurrentDay();

        if (params != null)
        {
            if (params.containsKey("year"))
                year = params.getInt("year");
            if (params.containsKey("month"))
                month = params.getInt("month");
        }

        utils.setScriptVar(self, VAR_YEAR, year);
        utils.setScriptVar(self, VAR_MONTH, month);
        utils.setScriptVar(self, VAR_SELECTED_DAY, day);

        openCalendarWindow(self, year, month, day);
        return SCRIPT_CONTINUE;
    }

    private void openCalendarWindow(obj_id player, int year, int month, int selectedDay) throws InterruptedException
    {
        // Close existing window if open
        closeCalendar(player);

        // Create the SUI page
        int pid = createSUIPage(SUI_CALENDAR, player, player, "handleCalendarCallback");
        if (pid < 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444Error: Could not open calendar window.");
            return;
        }

        utils.setScriptVar(player, VAR_SUI_PID, pid);

        // Initialize the calendar display
        updateCalendarDisplay(player, pid, year, month, selectedDay);

        // Show/hide settings button based on god status
        setSUIProperty(pid, COMP_BTN_SETTINGS, sui.PROP_VISIBLE, isGod(player) ? "true" : "false");

        // Subscribe to button events
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.header.btnPrevMonth", "handlePrevMonth");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.header.btnNextMonth", "handleNextMonth");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.header.btnToday", "handleToday");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.header.btnCreate", "handleCreateEvent");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.header.btnSettings", "handleSettings");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.btnClose", "handleClose");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.eventPanel.btnViewDetails", "handleViewDetails");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.eventPanel.btnDeleteEvent", "handleDeleteEvent");

        // Subscribe to day button events (all 42 day cells)
        for (int i = 1; i <= 42; i++)
        {
            String dayStr = i < 10 ? "0" + i : String.valueOf(i);
            subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "calendarWindow.calendarGrid.day" + dayStr, "handleDayClick");
        }

        // Subscribe to list selection
        subscribeToSUIProperty(pid, COMP_EVENTS_LIST, sui.PROP_SELECTEDROW);

        showSUIPage(pid);
    }

    private void closeCalendar(obj_id player) throws InterruptedException
    {
        if (utils.hasScriptVar(player, VAR_SUI_PID))
        {
            int pid = utils.getIntScriptVar(player, VAR_SUI_PID);
            if (pid > 0)
            {
                forceCloseSUIPage(pid);
            }
        }
        cleanupScriptVars(player);
    }

    private void cleanupScriptVars(obj_id player) throws InterruptedException
    {
        utils.removeScriptVar(player, VAR_SUI_PID);
        utils.removeScriptVar(player, VAR_YEAR);
        utils.removeScriptVar(player, VAR_MONTH);
        utils.removeScriptVar(player, VAR_SELECTED_DAY);
        utils.removeScriptVar(player, VAR_SELECTED_EVENTS);
        utils.removeScriptVar(player, VAR_CREATE_DATA);
    }

    // =========================================================================
    // Calendar Display Updates
    // =========================================================================

    private void updateCalendarDisplay(obj_id player, int pid, int year, int month, int selectedDay) throws InterruptedException
    {
        // Update month/year header
        String monthYear = calendar.getMonthName(month).toUpperCase() + " " + year;
        setSUIProperty(pid, COMP_MONTH_YEAR, sui.PROP_TEXT, monthYear);

        // Update selected date display
        String dateStr = calendar.formatDate(year, month, selectedDay);
        setSUIProperty(pid, COMP_SELECTED_DATE, sui.PROP_TEXT, dateStr);

        // Get calendar data
        int daysInMonth = calendar.getDaysInMonth(year, month);
        int firstDayOfWeek = calendar.getFirstDayOfMonth(year, month);
        int currentDay = calendar.getCurrentDay();
        int currentMonth = calendar.getCurrentMonth();
        int currentYear = calendar.getCurrentYear();

        // Get all events for the month
        dictionary[] monthEvents = calendar.getEventsForMonth(player, year, month);

        // Update all 42 day cells
        int dayNum = 1;
        for (int cell = 1; cell <= 42; cell++)
        {
            String cellStr = cell < 10 ? "0" + cell : String.valueOf(cell);
            String cellPath = "calendarWindow.calendarGrid.day" + cellStr;

            if (cell < firstDayOfWeek || dayNum > daysInMonth)
            {
                // Empty cell
                setSUIProperty(pid, cellPath, sui.PROP_VISIBLE, "false");
                setSUIProperty(pid, cellPath, sui.PROP_TEXT, "");
            }
            else
            {
                setSUIProperty(pid, cellPath, sui.PROP_VISIBLE, "true");

                // Check for events on this day
                boolean hasEvents = hasEventsOnDay(monthEvents, dayNum);
                boolean isToday = (dayNum == currentDay && month == currentMonth && year == currentYear);
                boolean isSelected = (dayNum == selectedDay);

                // Build display text
                String displayText = String.valueOf(dayNum);
                if (hasEvents)
                    displayText += " *";

                setSUIProperty(pid, cellPath, sui.PROP_TEXT, displayText);

                // Set colors based on state
                if (isSelected)
                {
                    setSUIProperty(pid, cellPath, "BackgroundTint", "#00FF00");
                }
                else if (isToday)
                {
                    setSUIProperty(pid, cellPath, "BackgroundTint", "#FFD700");
                }
                else if (hasEvents)
                {
                    setSUIProperty(pid, cellPath, "BackgroundTint", "#005577");
                }
                else
                {
                    setSUIProperty(pid, cellPath, "BackgroundTint", "#003355");
                }

                dayNum++;
            }
        }

        // Update events list for selected day
        updateEventsListForDay(player, pid, year, month, selectedDay);
    }

    private boolean hasEventsOnDay(dictionary[] monthEvents, int day) throws InterruptedException
    {
        if (monthEvents == null)
            return false;

        for (dictionary evt : monthEvents)
        {
            if (evt.getInt(calendar.KEY_DAY) == day)
                return true;
        }
        return false;
    }

    private void updateEventsListForDay(obj_id player, int pid, int year, int month, int day) throws InterruptedException
    {
        // Clear existing datasource
        clearSUIDataSource(pid, COMP_EVENTS_DATA);

        // Get events for the selected day
        dictionary[] dayEvents = calendar.getEventsForDay(player, year, month, day);

        if (dayEvents == null || dayEvents.length == 0)
        {
            addSUIDataSourceContainer(pid, COMP_EVENTS_DATA, "No events", "");
            utils.removeScriptVar(player, VAR_SELECTED_EVENTS);
        }
        else
        {
            // Store event IDs for later reference
            String[] eventIds = new String[dayEvents.length];

            for (int i = 0; i < dayEvents.length; i++)
            {
                dictionary evt = dayEvents[i];
                String title = evt.getString(calendar.KEY_TITLE);
                int evtType = evt.getInt(calendar.KEY_EVENT_TYPE);
                String eventId = evt.getString(calendar.KEY_EVENT_ID);

                eventIds[i] = eventId;

                // Add color prefix based on event type
                String prefix;
                switch (evtType)
                {
                    case calendar.EVENT_TYPE_STAFF:
                        prefix = "[S] ";
                        break;
                    case calendar.EVENT_TYPE_SERVER:
                        prefix = "[V] ";
                        break;
                    case calendar.EVENT_TYPE_GUILD:
                        prefix = "[G] ";
                        break;
                    case calendar.EVENT_TYPE_CITY:
                        prefix = "[C] ";
                        break;
                    default:
                        prefix = "";
                }

                addSUIDataSourceContainer(pid, COMP_EVENTS_DATA, prefix + title, eventId);
            }

            utils.setScriptVar(player, VAR_SELECTED_EVENTS, eventIds);
        }

        // Show delete button only if player can edit events
        boolean canDelete = false;
        if (dayEvents != null && dayEvents.length > 0)
        {
            canDelete = calendar.canDeleteEvent(player, dayEvents[0]);
        }
        setSUIProperty(pid, COMP_BTN_DELETE, sui.PROP_VISIBLE, canDelete ? "true" : "false");

        // Update selected date display
        String dateStr = calendar.formatDate(year, month, day);
        setSUIProperty(pid, COMP_SELECTED_DATE, sui.PROP_TEXT, dateStr);
    }

    // =========================================================================
    // Event Handlers - Navigation
    // =========================================================================

    public int handlePrevMonth(obj_id self, dictionary params) throws InterruptedException
    {
        int year = utils.getIntScriptVar(self, VAR_YEAR);
        int month = utils.getIntScriptVar(self, VAR_MONTH);

        month--;
        if (month < 1)
        {
            month = 12;
            year--;
        }

        utils.setScriptVar(self, VAR_YEAR, year);
        utils.setScriptVar(self, VAR_MONTH, month);
        utils.setScriptVar(self, VAR_SELECTED_DAY, 1);

        int pid = utils.getIntScriptVar(self, VAR_SUI_PID);
        updateCalendarDisplay(self, pid, year, month, 1);

        return SCRIPT_CONTINUE;
    }

    public int handleNextMonth(obj_id self, dictionary params) throws InterruptedException
    {
        int year = utils.getIntScriptVar(self, VAR_YEAR);
        int month = utils.getIntScriptVar(self, VAR_MONTH);

        month++;
        if (month > 12)
        {
            month = 1;
            year++;
        }

        utils.setScriptVar(self, VAR_YEAR, year);
        utils.setScriptVar(self, VAR_MONTH, month);
        utils.setScriptVar(self, VAR_SELECTED_DAY, 1);

        int pid = utils.getIntScriptVar(self, VAR_SUI_PID);
        updateCalendarDisplay(self, pid, year, month, 1);

        return SCRIPT_CONTINUE;
    }

    public int handleToday(obj_id self, dictionary params) throws InterruptedException
    {
        int year = calendar.getCurrentYear();
        int month = calendar.getCurrentMonth();
        int day = calendar.getCurrentDay();

        utils.setScriptVar(self, VAR_YEAR, year);
        utils.setScriptVar(self, VAR_MONTH, month);
        utils.setScriptVar(self, VAR_SELECTED_DAY, day);

        int pid = utils.getIntScriptVar(self, VAR_SUI_PID);
        updateCalendarDisplay(self, pid, year, month, day);

        return SCRIPT_CONTINUE;
    }

    public int handleDayClick(obj_id self, dictionary params) throws InterruptedException
    {
        // Determine which day was clicked from the button path
        String source = params.getString("eventSource");
        if (source == null)
            return SCRIPT_CONTINUE;

        // Extract day number from button name (day01, day02, etc.)
        int cellNum = 0;
        try
        {
            String numStr = source.substring(source.length() - 2);
            cellNum = Integer.parseInt(numStr);
        }
        catch (Exception e)
        {
            return SCRIPT_CONTINUE;
        }

        int year = utils.getIntScriptVar(self, VAR_YEAR);
        int month = utils.getIntScriptVar(self, VAR_MONTH);
        int firstDayOfWeek = calendar.getFirstDayOfMonth(year, month);
        int daysInMonth = calendar.getDaysInMonth(year, month);

        // Convert cell number to actual day
        int day = cellNum - firstDayOfWeek + 1;
        if (day < 1 || day > daysInMonth)
            return SCRIPT_CONTINUE;

        utils.setScriptVar(self, VAR_SELECTED_DAY, day);

        int pid = utils.getIntScriptVar(self, VAR_SUI_PID);
        updateCalendarDisplay(self, pid, year, month, day);

        return SCRIPT_CONTINUE;
    }

    public int handleClose(obj_id self, dictionary params) throws InterruptedException
    {
        closeCalendar(self);
        return SCRIPT_CONTINUE;
    }

    public int handleCalendarCallback(obj_id self, dictionary params) throws InterruptedException
    {
        // Handle cancel/close button
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            closeCalendar(self);
        }
        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // Event Handlers - Event Management
    // =========================================================================

    public int handleViewDetails(obj_id self, dictionary params) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, VAR_SELECTED_EVENTS))
        {
            sendSystemMessageTestingOnly(self, "\\#888888No event selected.");
            return SCRIPT_CONTINUE;
        }

        int pid = utils.getIntScriptVar(self, VAR_SUI_PID);

        // Get selected row from the list
        int selectedRow = 0;
        String selectedRowStr = getSUIProperty(pid, COMP_EVENTS_LIST, sui.PROP_SELECTEDROW);
        if (selectedRowStr != null && !selectedRowStr.isEmpty())
        {
            try
            {
                selectedRow = Integer.parseInt(selectedRowStr);
            }
            catch (NumberFormatException e)
            {
                selectedRow = 0;
            }
        }

        String[] eventIds = utils.getStringArrayScriptVar(self, VAR_SELECTED_EVENTS);
        if (eventIds == null || selectedRow < 0 || selectedRow >= eventIds.length)
        {
            sendSystemMessageTestingOnly(self, "\\#888888No event selected.");
            return SCRIPT_CONTINUE;
        }

        String eventId = eventIds[selectedRow];
        dictionary eventData = calendar.getEvent(eventId);

        if (eventData == null)
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Event not found.");
            return SCRIPT_CONTINUE;
        }

        // Display event details
        displayEventDetails(self, eventData);

        return SCRIPT_CONTINUE;
    }

    private void displayEventDetails(obj_id player, dictionary eventData) throws InterruptedException
    {
        String title = eventData.getString(calendar.KEY_TITLE);
        String desc = eventData.getString(calendar.KEY_DESCRIPTION);
        int evtType = eventData.getInt(calendar.KEY_EVENT_TYPE);
        int year = eventData.getInt(calendar.KEY_YEAR);
        int month = eventData.getInt(calendar.KEY_MONTH);
        int day = eventData.getInt(calendar.KEY_DAY);
        int hour = eventData.getInt(calendar.KEY_HOUR);
        int minute = eventData.getInt(calendar.KEY_MINUTE);
        int duration = eventData.getInt(calendar.KEY_DURATION);
        boolean active = eventData.getBoolean(calendar.KEY_ACTIVE);

        String typeName = calendar.getEventTypeName(evtType);
        String color = calendar.getEventTypeColor(evtType);
        String dateStr = calendar.formatDate(year, month, day);
        String timeStr = calendar.formatTime(hour, minute);

        sendSystemMessageTestingOnly(player, " ");
        sendSystemMessageTestingOnly(player, color + "===== EVENT DETAILS =====\\#FFFFFF");
        sendSystemMessageTestingOnly(player, color + title + "\\#FFFFFF");
        sendSystemMessageTestingOnly(player, "\\#aaaaaa Type: " + typeName);
        sendSystemMessageTestingOnly(player, "\\#aaaaaa Date: " + dateStr);
        sendSystemMessageTestingOnly(player, "\\#aaaaaa Time: " + timeStr);
        sendSystemMessageTestingOnly(player, "\\#aaaaaa Duration: " + formatDuration(duration));
        sendSystemMessageTestingOnly(player, "\\#aaaaaa Status: " + (active ? "\\#00ff00Active" : "\\#888888Scheduled"));
        if (desc != null && !desc.isEmpty())
        {
            sendSystemMessageTestingOnly(player, "\\#aaaaaa Description: " + desc);
        }
        sendSystemMessageTestingOnly(player, color + "========================\\#FFFFFF");
        sendSystemMessageTestingOnly(player, " ");
    }

    private String formatDuration(int minutes) throws InterruptedException
    {
        if (minutes >= 1440)
            return (minutes / 1440) + " day(s)";
        if (minutes >= 60)
            return (minutes / 60) + " hour(s)";
        return minutes + " minute(s)";
    }

    public int handleDeleteEvent(obj_id self, dictionary params) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, VAR_SELECTED_EVENTS))
        {
            sendSystemMessageTestingOnly(self, "\\#888888No event selected.");
            return SCRIPT_CONTINUE;
        }

        int pid = utils.getIntScriptVar(self, VAR_SUI_PID);

        int selectedRow = 0;
        String selectedRowStr = getSUIProperty(pid, COMP_EVENTS_LIST, sui.PROP_SELECTEDROW);
        if (selectedRowStr != null && !selectedRowStr.isEmpty())
        {
            try
            {
                selectedRow = Integer.parseInt(selectedRowStr);
            }
            catch (NumberFormatException e)
            {
                selectedRow = 0;
            }
        }

        String[] eventIds = utils.getStringArrayScriptVar(self, VAR_SELECTED_EVENTS);
        if (eventIds == null || selectedRow < 0 || selectedRow >= eventIds.length)
        {
            sendSystemMessageTestingOnly(self, "\\#888888No event selected.");
            return SCRIPT_CONTINUE;
        }

        String eventId = eventIds[selectedRow];
        dictionary eventData = calendar.getEvent(eventId);

        if (eventData == null)
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Event not found.");
            return SCRIPT_CONTINUE;
        }

        if (!calendar.canDeleteEvent(self, eventData))
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444You do not have permission to delete this event.");
            return SCRIPT_CONTINUE;
        }

        String title = eventData.getString(calendar.KEY_TITLE);

        // Delete the event
        if (calendar.deleteEvent(eventId))
        {
            sendSystemMessageTestingOnly(self, "\\#00ff00Event deleted: " + title);

            // Refresh the display
            int year = utils.getIntScriptVar(self, VAR_YEAR);
            int month = utils.getIntScriptVar(self, VAR_MONTH);
            int day = utils.getIntScriptVar(self, VAR_SELECTED_DAY);
            updateCalendarDisplay(self, pid, year, month, day);
        }
        else
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Failed to delete event.");
        }

        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // Event Creation - Uses Custom Event Editor UI
    // =========================================================================

    public int handleCreateEvent(obj_id self, dictionary params) throws InterruptedException
    {
        // Check if player can create any event type
        boolean canCreate = false;
        for (int type = 0; type <= 3; type++)
        {
            if (calendar.canCreateEvent(self, type))
            {
                canCreate = true;
                break;
            }
        }

        if (!canCreate)
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444You do not have permission to create events.");
            sendSystemMessageTestingOnly(self, "\\#aaaaaa  Guild leaders/officers can create guild events.");
            sendSystemMessageTestingOnly(self, "\\#aaaaaa  Mayors can create city events.");
            sendSystemMessageTestingOnly(self, "\\#aaaaaa  Staff can create all event types.");
            return SCRIPT_CONTINUE;
        }

        // Open Event Editor UI
        openEventEditorWindow(self);

        return SCRIPT_CONTINUE;
    }

    private void openEventEditorWindow(obj_id player) throws InterruptedException
    {
        // Close existing editor if open
        if (utils.hasScriptVar(player, VAR_EDITOR_PID))
        {
            int oldPid = utils.getIntScriptVar(player, VAR_EDITOR_PID);
            forceCloseSUIPage(oldPid);
        }

        // Create the Event Editor SUI page
        int pid = createSUIPage(SUI_EVENT_EDITOR, player, player, "handleEditorCallback");
        if (pid < 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444Error: Could not open event editor.");
            return;
        }

        utils.setScriptVar(player, VAR_EDITOR_PID, pid);

        // Set default date to selected day
        int year = utils.getIntScriptVar(player, VAR_YEAR);
        int month = utils.getIntScriptVar(player, VAR_MONTH);
        int day = utils.getIntScriptVar(player, VAR_SELECTED_DAY);

        String monthStr = month < 10 ? "0" + month : String.valueOf(month);
        String dayStr = day < 10 ? "0" + day : String.valueOf(day);
        String dateStr = year + "-" + monthStr + "-" + dayStr;

        setSUIProperty(pid, EDITOR_DATE, sui.PROP_TEXT, dateStr);
        setSUIProperty(pid, EDITOR_TIME, sui.PROP_TEXT, "12:00");

        // Show/hide server event section based on permissions
        boolean isStaff = isGod(player);
        setSUIProperty(pid, EDITOR_SERVER_SECTION, sui.PROP_VISIBLE, isStaff ? "true" : "false");

        // Subscribe to button events
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "eventEditor.btnCreateEvent", "handleEditorCreate");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "eventEditor.btnCancel", "handleEditorCancel");

        // Subscribe to checkbox events for recurring toggle
        subscribeToSUIEvent(pid, sui_event_type.SET_onCheckbox, "eventEditor.optionsSection.chkRecurring", "handleRecurringToggle");

        // Subscribe to event type change to show/hide server event section
        subscribeToSUIEvent(pid, sui_event_type.SET_onGenericSelection, "eventEditor.typeSection.cmbEventType", "handleEventTypeChange");

        // Subscribe to properties for reading values
        subscribeToSUIProperty(pid, EDITOR_TITLE, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, EDITOR_DESC, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, EDITOR_TYPE, sui.PROP_SELECTEDINDEX);
        subscribeToSUIProperty(pid, EDITOR_DATE, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, EDITOR_TIME, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, EDITOR_DURATION, sui.PROP_SELECTEDINDEX);
        subscribeToSUIProperty(pid, EDITOR_SERVER_EVENT, sui.PROP_SELECTEDINDEX);
        subscribeToSUIProperty(pid, EDITOR_BROADCAST, sui.PROP_CHECKED);
        subscribeToSUIProperty(pid, EDITOR_RECURRING, sui.PROP_CHECKED);
        subscribeToSUIProperty(pid, EDITOR_RECUR_TYPE, sui.PROP_SELECTEDINDEX);

        showSUIPage(pid);
    }

    public int handleEditorCallback(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_EDITOR_PID);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleRecurringToggle(obj_id self, dictionary params) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, VAR_EDITOR_PID))
            return SCRIPT_CONTINUE;

        int pid = utils.getIntScriptVar(self, VAR_EDITOR_PID);
        String checked = getSUIProperty(pid, EDITOR_RECURRING, sui.PROP_CHECKED);
        boolean isRecurring = "true".equalsIgnoreCase(checked);

        setSUIProperty(pid, EDITOR_RECUR_SECTION, sui.PROP_VISIBLE, isRecurring ? "true" : "false");

        return SCRIPT_CONTINUE;
    }

    public int handleEventTypeChange(obj_id self, dictionary params) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, VAR_EDITOR_PID))
            return SCRIPT_CONTINUE;

        int pid = utils.getIntScriptVar(self, VAR_EDITOR_PID);
        String selectedIdx = getSUIProperty(pid, EDITOR_TYPE, sui.PROP_SELECTEDINDEX);
        int eventType = 0;
        try
        {
            eventType = Integer.parseInt(selectedIdx);
        }
        catch (NumberFormatException e)
        {
            eventType = 0;
        }

        // Show server event section only for server events (type 3)
        boolean showServerEvent = (eventType == calendar.EVENT_TYPE_SERVER) && isGod(self);
        setSUIProperty(pid, EDITOR_SERVER_SECTION, sui.PROP_VISIBLE, showServerEvent ? "true" : "false");

        return SCRIPT_CONTINUE;
    }

    public int handleEditorCancel(obj_id self, dictionary params) throws InterruptedException
    {
        if (utils.hasScriptVar(self, VAR_EDITOR_PID))
        {
            int pid = utils.getIntScriptVar(self, VAR_EDITOR_PID);
            forceCloseSUIPage(pid);
            utils.removeScriptVar(self, VAR_EDITOR_PID);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleEditorCreate(obj_id self, dictionary params) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, VAR_EDITOR_PID))
            return SCRIPT_CONTINUE;

        int pid = utils.getIntScriptVar(self, VAR_EDITOR_PID);

        // Read all values from the editor
        String title = getSUIProperty(pid, EDITOR_TITLE, sui.PROP_TEXT);
        String description = getSUIProperty(pid, EDITOR_DESC, sui.PROP_TEXT);
        String eventTypeStr = getSUIProperty(pid, EDITOR_TYPE, sui.PROP_SELECTEDINDEX);
        String dateStr = getSUIProperty(pid, EDITOR_DATE, sui.PROP_TEXT);
        String timeStr = getSUIProperty(pid, EDITOR_TIME, sui.PROP_TEXT);
        String durationStr = getSUIProperty(pid, EDITOR_DURATION, sui.PROP_SELECTEDINDEX);
        String serverEventStr = getSUIProperty(pid, EDITOR_SERVER_EVENT, sui.PROP_SELECTEDINDEX);
        String broadcastStr = getSUIProperty(pid, EDITOR_BROADCAST, sui.PROP_CHECKED);
        String recurringStr = getSUIProperty(pid, EDITOR_RECURRING, sui.PROP_CHECKED);
        String recurTypeStr = getSUIProperty(pid, EDITOR_RECUR_TYPE, sui.PROP_SELECTEDINDEX);

        // Validate title
        if (title == null || title.trim().isEmpty())
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Event title is required.");
            return SCRIPT_CONTINUE;
        }

        // Parse event type
        int eventType = 0;
        try
        {
            eventType = Integer.parseInt(eventTypeStr);
        }
        catch (NumberFormatException e)
        {
            eventType = calendar.EVENT_TYPE_STAFF;
        }

        // Validate permissions
        if (!calendar.canCreateEvent(self, eventType))
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444You do not have permission to create this type of event.");
            return SCRIPT_CONTINUE;
        }

        // Parse date (YYYY-MM-DD)
        int year = calendar.getCurrentYear();
        int month = calendar.getCurrentMonth();
        int day = calendar.getCurrentDay();
        if (dateStr != null && dateStr.contains("-"))
        {
            String[] dateParts = dateStr.split("-");
            if (dateParts.length == 3)
            {
                try
                {
                    year = Integer.parseInt(dateParts[0]);
                    month = Integer.parseInt(dateParts[1]);
                    day = Integer.parseInt(dateParts[2]);
                }
                catch (NumberFormatException e)
                {
                    // Use defaults
                }
            }
        }

        // Parse time (HH:MM)
        int hour = 12;
        int minute = 0;
        if (timeStr != null && timeStr.contains(":"))
        {
            String[] timeParts = timeStr.split(":");
            if (timeParts.length >= 2)
            {
                try
                {
                    hour = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                }
                catch (NumberFormatException e)
                {
                    // Use defaults
                }
            }
        }

        // Parse duration
        int[] durations = {15, 30, 60, 120, 240, 480, 1440};
        int durationIdx = 2;
        try
        {
            durationIdx = Integer.parseInt(durationStr);
        }
        catch (NumberFormatException e)
        {
            durationIdx = 2;
        }
        int duration = (durationIdx >= 0 && durationIdx < durations.length) ? durations[durationIdx] : 60;

        // Parse server event
        String serverEventKey = "";
        if (eventType == calendar.EVENT_TYPE_SERVER && serverEventStr != null)
        {
            int serverEventIdx = 0;
            try
            {
                serverEventIdx = Integer.parseInt(serverEventStr);
            }
            catch (NumberFormatException e)
            {
                serverEventIdx = 0;
            }
            if (serverEventIdx >= 0 && serverEventIdx < calendar.SERVER_EVENTS.length)
            {
                serverEventKey = calendar.SERVER_EVENTS[serverEventIdx];
            }
        }

        // Parse options
        boolean broadcast = "true".equalsIgnoreCase(broadcastStr);
        boolean recurring = "true".equalsIgnoreCase(recurringStr);
        int recurType = calendar.RECUR_NONE;
        if (recurring)
        {
            try
            {
                recurType = Integer.parseInt(recurTypeStr) + 1; // Index 0 = DAILY (1)
            }
            catch (NumberFormatException e)
            {
                recurType = calendar.RECUR_DAILY;
            }
        }

        // Build event data
        dictionary eventData = new dictionary();
        eventData.put(calendar.KEY_TITLE, title.trim());
        eventData.put(calendar.KEY_DESCRIPTION, description != null ? description.trim() : "");
        eventData.put(calendar.KEY_EVENT_TYPE, eventType);
        eventData.put(calendar.KEY_YEAR, year);
        eventData.put(calendar.KEY_MONTH, month);
        eventData.put(calendar.KEY_DAY, day);
        eventData.put(calendar.KEY_HOUR, hour);
        eventData.put(calendar.KEY_MINUTE, minute);
        eventData.put(calendar.KEY_DURATION, duration);
        eventData.put(calendar.KEY_BROADCAST_START, broadcast);
        eventData.put(calendar.KEY_RECURRING, recurring);
        eventData.put(calendar.KEY_RECURRENCE_TYPE, recurType);
        eventData.put(calendar.KEY_SERVER_EVENT_KEY, serverEventKey);

        // Set guild/city IDs based on type
        if (eventType == calendar.EVENT_TYPE_GUILD)
        {
            eventData.put(calendar.KEY_GUILD_ID, getGuildId(self));
            eventData.put(calendar.KEY_CITY_ID, 0);
        }
        else if (eventType == calendar.EVENT_TYPE_CITY)
        {
            eventData.put(calendar.KEY_GUILD_ID, 0);
            eventData.put(calendar.KEY_CITY_ID, getCitizenOfCityId(self));
        }
        else
        {
            eventData.put(calendar.KEY_GUILD_ID, 0);
            eventData.put(calendar.KEY_CITY_ID, 0);
        }

        // Create the event
        String eventId = calendar.createEvent(self, eventData);

        // Close the editor
        forceCloseSUIPage(pid);
        utils.removeScriptVar(self, VAR_EDITOR_PID);

        if (eventId != null)
        {
            sendSystemMessageTestingOnly(self, "\\#00ff00Event created successfully: " + title.trim());
            sendSystemMessageTestingOnly(self, "\\#aaaaaa  Date: " + calendar.formatDate(year, month, day) + " at " + calendar.formatTime(hour, minute));

            // Refresh the calendar display
            if (utils.hasScriptVar(self, VAR_SUI_PID))
            {
                int calPid = utils.getIntScriptVar(self, VAR_SUI_PID);
                utils.setScriptVar(self, VAR_SELECTED_DAY, day);
                updateCalendarDisplay(self, calPid, year, month, day);
            }
        }
        else
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Failed to create event. Please try again.");
        }

        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // Legacy event creation methods (for backwards compatibility)
    // =========================================================================

    private void showEventTypeSelection(obj_id player) throws InterruptedException
    {
        java.util.Vector typeList = new java.util.Vector();
        java.util.Vector typeValues = new java.util.Vector();

        if (isGod(player))
        {
            typeList.add("Staff Event (Galaxy-Wide)");
            typeValues.add(String.valueOf(calendar.EVENT_TYPE_STAFF));
            typeList.add("Server Event (Holiday System)");
            typeValues.add(String.valueOf(calendar.EVENT_TYPE_SERVER));
        }

        if (calendar.canCreateEvent(player, calendar.EVENT_TYPE_GUILD))
        {
            int guildId = getGuildId(player);
            String guildName = guildGetName(guildId);
            typeList.add("Guild Event: " + guildName);
            typeValues.add(String.valueOf(calendar.EVENT_TYPE_GUILD));
        }

        if (calendar.canCreateEvent(player, calendar.EVENT_TYPE_CITY))
        {
            int cityId = getCitizenOfCityId(player);
            String cityName = cityGetName(cityId);
            typeList.add("City Event: " + cityName);
            typeValues.add(String.valueOf(calendar.EVENT_TYPE_CITY));
        }

        String[] types = new String[typeList.size()];
        typeList.toArray(types);
        String[] values = new String[typeValues.size()];
        typeValues.toArray(values);

        utils.setScriptVar(player, "calendar.create.type_values", values);

        int pid = sui.listbox(player, player, "Select the type of event to create:", sui.OK_CANCEL, "Create Event - Type", types, "handleCreateTypeSelection", true, false);
    }

    public int handleCreateTypeSelection(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        String[] typeValues = utils.getStringArrayScriptVar(self, "calendar.create.type_values");
        int eventType = Integer.parseInt(typeValues[idx]);

        dictionary createData = utils.getDictionaryScriptVar(self, VAR_CREATE_DATA);
        createData.put("event_type", eventType);
        utils.setScriptVar(self, VAR_CREATE_DATA, createData);

        // Get event title
        int pid = sui.inputbox(self, self, "Enter the event title:", sui.OK_CANCEL, "Create Event - Title", sui.INPUT_NORMAL, null, "handleCreateTitleInput");

        return SCRIPT_CONTINUE;
    }

    public int handleCreateTitleInput(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        String title = sui.getInputBoxText(params);
        if (title == null || title.trim().isEmpty())
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Event title cannot be empty.");
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        dictionary createData = utils.getDictionaryScriptVar(self, VAR_CREATE_DATA);
        createData.put("title", title.trim());
        utils.setScriptVar(self, VAR_CREATE_DATA, createData);

        // Get description
        int pid = sui.inputbox(self, self, "Enter the event description (optional):", sui.OK_CANCEL, "Create Event - Description", sui.INPUT_NORMAL, null, "handleCreateDescInput");

        return SCRIPT_CONTINUE;
    }

    public int handleCreateDescInput(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        String desc = sui.getInputBoxText(params);
        if (desc == null)
            desc = "";

        dictionary createData = utils.getDictionaryScriptVar(self, VAR_CREATE_DATA);
        createData.put("description", desc.trim());
        utils.setScriptVar(self, VAR_CREATE_DATA, createData);

        // Get time selection
        showTimeSelection(self);

        return SCRIPT_CONTINUE;
    }

    private void showTimeSelection(obj_id player) throws InterruptedException
    {
        String[] times = new String[24];
        for (int h = 0; h < 24; h++)
        {
            times[h] = calendar.formatTime(h, 0);
        }

        int pid = sui.listbox(player, player, "Select the event start time:", sui.OK_CANCEL, "Create Event - Time", times, "handleCreateTimeSelection", true, false);
    }

    public int handleCreateTimeSelection(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0)
            idx = 12; // Default to noon

        dictionary createData = utils.getDictionaryScriptVar(self, VAR_CREATE_DATA);
        createData.put("hour", idx);
        createData.put("minute", 0);
        utils.setScriptVar(self, VAR_CREATE_DATA, createData);

        // Get duration selection
        showDurationSelection(self);

        return SCRIPT_CONTINUE;
    }

    private void showDurationSelection(obj_id player) throws InterruptedException
    {
        String[] durations = new String[]{
            "15 minutes",
            "30 minutes",
            "1 hour",
            "2 hours",
            "4 hours",
            "8 hours",
            "All Day (24 hours)"
        };

        int pid = sui.listbox(player, player, "Select the event duration:", sui.OK_CANCEL, "Create Event - Duration", durations, "handleCreateDurationSelection", true, false);
    }

    public int handleCreateDurationSelection(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        int[] durationMinutes = {15, 30, 60, 120, 240, 480, 1440};
        int duration = idx >= 0 && idx < durationMinutes.length ? durationMinutes[idx] : 60;

        dictionary createData = utils.getDictionaryScriptVar(self, VAR_CREATE_DATA);
        createData.put("duration", duration);
        utils.setScriptVar(self, VAR_CREATE_DATA, createData);

        // Check if server event - needs event key selection
        int eventType = createData.getInt("event_type");
        if (eventType == calendar.EVENT_TYPE_SERVER)
        {
            showServerEventSelection(self);
        }
        else
        {
            finalizeEventCreation(self);
        }

        return SCRIPT_CONTINUE;
    }

    private void showServerEventSelection(obj_id player) throws InterruptedException
    {
        int pid = sui.listbox(player, player, "Select the server event to trigger:", sui.OK_CANCEL, "Create Event - Server Event", calendar.SERVER_EVENT_NAMES, "handleServerEventSelection", true, false);
    }

    public int handleServerEventSelection(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_CREATE_DATA);
            return SCRIPT_CONTINUE;
        }

        int idx = sui.getListboxSelectedRow(params);
        if (idx < 0)
            idx = 0;

        dictionary createData = utils.getDictionaryScriptVar(self, VAR_CREATE_DATA);
        createData.put("server_event_key", calendar.SERVER_EVENTS[idx]);
        utils.setScriptVar(self, VAR_CREATE_DATA, createData);

        finalizeEventCreation(self);

        return SCRIPT_CONTINUE;
    }

    private void finalizeEventCreation(obj_id player) throws InterruptedException
    {
        dictionary createData = utils.getDictionaryScriptVar(player, VAR_CREATE_DATA);
        if (createData == null)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444Error: Event creation data lost.");
            return;
        }

        // Build the event dictionary
        dictionary eventData = new dictionary();
        eventData.put(calendar.KEY_TITLE, createData.getString("title"));
        eventData.put(calendar.KEY_DESCRIPTION, createData.getString("description"));
        eventData.put(calendar.KEY_EVENT_TYPE, createData.getInt("event_type"));
        eventData.put(calendar.KEY_YEAR, createData.getInt("year"));
        eventData.put(calendar.KEY_MONTH, createData.getInt("month"));
        eventData.put(calendar.KEY_DAY, createData.getInt("day"));
        eventData.put(calendar.KEY_HOUR, createData.getInt("hour"));
        eventData.put(calendar.KEY_MINUTE, createData.getInt("minute"));
        eventData.put(calendar.KEY_DURATION, createData.getInt("duration"));
        eventData.put(calendar.KEY_BROADCAST_START, true);
        eventData.put(calendar.KEY_RECURRING, false);
        eventData.put(calendar.KEY_RECURRENCE_TYPE, calendar.RECUR_NONE);

        String serverEventKey = createData.getString("server_event_key");
        if (serverEventKey == null)
            serverEventKey = "";
        eventData.put(calendar.KEY_SERVER_EVENT_KEY, serverEventKey);

        int eventType = createData.getInt("event_type");
        if (eventType == calendar.EVENT_TYPE_GUILD)
        {
            eventData.put(calendar.KEY_GUILD_ID, getGuildId(player));
            eventData.put(calendar.KEY_CITY_ID, 0);
        }
        else if (eventType == calendar.EVENT_TYPE_CITY)
        {
            eventData.put(calendar.KEY_GUILD_ID, 0);
            eventData.put(calendar.KEY_CITY_ID, getCitizenOfCityId(player));
        }
        else
        {
            eventData.put(calendar.KEY_GUILD_ID, 0);
            eventData.put(calendar.KEY_CITY_ID, 0);
        }

        // Create the event
        String eventId = calendar.createEvent(player, eventData);

        if (eventId != null)
        {
            String title = createData.getString("title");
            int year = createData.getInt("year");
            int month = createData.getInt("month");
            int day = createData.getInt("day");
            int hour = createData.getInt("hour");
            int minute = createData.getInt("minute");

            sendSystemMessageTestingOnly(player, "\\#00ff00Event created successfully: " + title);
            sendSystemMessageTestingOnly(player, "\\#aaaaaa  Date: " + calendar.formatDate(year, month, day) + " at " + calendar.formatTime(hour, minute));

            // Refresh the calendar display
            if (utils.hasScriptVar(player, VAR_SUI_PID))
            {
                int pid = utils.getIntScriptVar(player, VAR_SUI_PID);
                updateCalendarDisplay(player, pid, year, month, day);
            }
        }
        else
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444Failed to create event. Please try again.");
        }

        // Cleanup
        utils.removeScriptVar(player, VAR_CREATE_DATA);
        utils.removeScriptVar(player, "calendar.create.type_values");
    }

    // =========================================================================
    // Settings (Staff Only) - Uses Custom Settings UI
    // =========================================================================

    public int handleSettings(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isGod(self))
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Access denied.");
            return SCRIPT_CONTINUE;
        }

        openSettingsWindow(self);
        return SCRIPT_CONTINUE;
    }

    private void openSettingsWindow(obj_id player) throws InterruptedException
    {
        // Close existing settings if open
        if (utils.hasScriptVar(player, VAR_SETTINGS_PID))
        {
            int oldPid = utils.getIntScriptVar(player, VAR_SETTINGS_PID);
            forceCloseSUIPage(oldPid);
        }

        // Create the Settings SUI page
        int pid = createSUIPage(SUI_SETTINGS, player, player, "handleSettingsCallback");
        if (pid < 0)
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444Error: Could not open settings window.");
            return;
        }

        utils.setScriptVar(player, VAR_SETTINGS_PID, pid);

        // Load current settings
        dictionary settings = calendar.getCalendarSettings();
        String bgTexture = settings.getString(calendar.SETTINGS_BG_TEXTURE);
        int srcX = settings.getInt(calendar.SETTINGS_SRC_RECT_X);
        int srcY = settings.getInt(calendar.SETTINGS_SRC_RECT_Y);
        int srcW = settings.getInt(calendar.SETTINGS_SRC_RECT_W);
        int srcH = settings.getInt(calendar.SETTINGS_SRC_RECT_H);

        // Set values
        setSUIProperty(pid, SETTINGS_TEXTURE, sui.PROP_TEXT, bgTexture != null ? bgTexture : "");
        setSUIProperty(pid, SETTINGS_RECT_X, sui.PROP_TEXT, String.valueOf(srcX));
        setSUIProperty(pid, SETTINGS_RECT_Y, sui.PROP_TEXT, String.valueOf(srcY));
        setSUIProperty(pid, SETTINGS_RECT_W, sui.PROP_TEXT, String.valueOf(srcW));
        setSUIProperty(pid, SETTINGS_RECT_H, sui.PROP_TEXT, String.valueOf(srcH));

        // Subscribe to button events
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "settingsWindow.btnApply", "handleSettingsApply");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "settingsWindow.btnReset", "handleSettingsReset");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "settingsWindow.btnClose", "handleSettingsClose");
        subscribeToSUIEvent(pid, sui_event_type.SET_onButton, "settingsWindow.rectSection.btnPreview", "handleSettingsPreview");

        // Subscribe to properties for reading values
        subscribeToSUIProperty(pid, SETTINGS_TEXTURE, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, SETTINGS_RECT_X, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, SETTINGS_RECT_Y, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, SETTINGS_RECT_W, sui.PROP_TEXT);
        subscribeToSUIProperty(pid, SETTINGS_RECT_H, sui.PROP_TEXT);

        showSUIPage(pid);
    }

    public int handleSettingsCallback(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, VAR_SETTINGS_PID);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSettingsApply(obj_id self, dictionary params) throws InterruptedException
    {
        if (!utils.hasScriptVar(self, VAR_SETTINGS_PID))
            return SCRIPT_CONTINUE;

        int pid = utils.getIntScriptVar(self, VAR_SETTINGS_PID);

        // Read values
        String texture = getSUIProperty(pid, SETTINGS_TEXTURE, sui.PROP_TEXT);
        String rectXStr = getSUIProperty(pid, SETTINGS_RECT_X, sui.PROP_TEXT);
        String rectYStr = getSUIProperty(pid, SETTINGS_RECT_Y, sui.PROP_TEXT);
        String rectWStr = getSUIProperty(pid, SETTINGS_RECT_W, sui.PROP_TEXT);
        String rectHStr = getSUIProperty(pid, SETTINGS_RECT_H, sui.PROP_TEXT);

        // Parse values
        int rectX = 0, rectY = 0, rectW = 512, rectH = 512;
        try
        {
            if (rectXStr != null) rectX = Integer.parseInt(rectXStr.trim());
            if (rectYStr != null) rectY = Integer.parseInt(rectYStr.trim());
            if (rectWStr != null) rectW = Integer.parseInt(rectWStr.trim());
            if (rectHStr != null) rectH = Integer.parseInt(rectHStr.trim());
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Invalid number format in one or more fields.");
            return SCRIPT_CONTINUE;
        }

        // Apply settings
        dictionary settings = new dictionary();
        settings.put(calendar.SETTINGS_BG_TEXTURE, texture != null ? texture.trim() : "");
        settings.put(calendar.SETTINGS_SRC_RECT_X, rectX);
        settings.put(calendar.SETTINGS_SRC_RECT_Y, rectY);
        settings.put(calendar.SETTINGS_SRC_RECT_W, rectW);
        settings.put(calendar.SETTINGS_SRC_RECT_H, rectH);
        calendar.setCalendarSettings(settings);

        sendSystemMessageTestingOnly(self, "\\#00ff00Calendar settings applied successfully.");

        return SCRIPT_CONTINUE;
    }

    public int handleSettingsReset(obj_id self, dictionary params) throws InterruptedException
    {
        calendar.setCalendarSettings(calendar.getDefaultSettings());

        // Update the UI with default values
        if (utils.hasScriptVar(self, VAR_SETTINGS_PID))
        {
            int pid = utils.getIntScriptVar(self, VAR_SETTINGS_PID);
            dictionary defaults = calendar.getDefaultSettings();

            setSUIProperty(pid, SETTINGS_TEXTURE, sui.PROP_TEXT, defaults.getString(calendar.SETTINGS_BG_TEXTURE));
            setSUIProperty(pid, SETTINGS_RECT_X, sui.PROP_TEXT, String.valueOf(defaults.getInt(calendar.SETTINGS_SRC_RECT_X)));
            setSUIProperty(pid, SETTINGS_RECT_Y, sui.PROP_TEXT, String.valueOf(defaults.getInt(calendar.SETTINGS_SRC_RECT_Y)));
            setSUIProperty(pid, SETTINGS_RECT_W, sui.PROP_TEXT, String.valueOf(defaults.getInt(calendar.SETTINGS_SRC_RECT_W)));
            setSUIProperty(pid, SETTINGS_RECT_H, sui.PROP_TEXT, String.valueOf(defaults.getInt(calendar.SETTINGS_SRC_RECT_H)));
        }

        sendSystemMessageTestingOnly(self, "\\#00ff00Calendar settings reset to defaults.");

        return SCRIPT_CONTINUE;
    }

    public int handleSettingsClose(obj_id self, dictionary params) throws InterruptedException
    {
        if (utils.hasScriptVar(self, VAR_SETTINGS_PID))
        {
            int pid = utils.getIntScriptVar(self, VAR_SETTINGS_PID);
            forceCloseSUIPage(pid);
            utils.removeScriptVar(self, VAR_SETTINGS_PID);
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSettingsPreview(obj_id self, dictionary params) throws InterruptedException
    {
        sendSystemMessageTestingOnly(self, "\\#aaaaaa[Preview functionality requires client-side implementation]");
        return SCRIPT_CONTINUE;
    }

    // =========================================================================
    // Legacy Settings Methods (for backwards compatibility)
    // =========================================================================

    public int handleSettingsSelection(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        int idx = sui.getListboxSelectedRow(params);

        switch (idx)
        {
            case 0: // Background texture
                int pid = sui.inputbox(self, self, "Enter background texture path (.dds):", sui.OK_CANCEL, "Background Texture", sui.INPUT_NORMAL, null, "handleSettingsBgInput");
                break;
            case 1:
                showSettingsRectInput(self, "X", calendar.SETTINGS_SRC_RECT_X);
                break;
            case 2:
                showSettingsRectInput(self, "Y", calendar.SETTINGS_SRC_RECT_Y);
                break;
            case 3:
                showSettingsRectInput(self, "Width", calendar.SETTINGS_SRC_RECT_W);
                break;
            case 4:
                showSettingsRectInput(self, "Height", calendar.SETTINGS_SRC_RECT_H);
                break;
            case 5: // Reset
                calendar.setCalendarSettings(calendar.getDefaultSettings());
                sendSystemMessageTestingOnly(self, "\\#00ff00Calendar settings reset to defaults.");
                break;
            default:
                break;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleSettingsBgInput(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
            return SCRIPT_CONTINUE;

        String texture = sui.getInputBoxText(params);
        if (texture != null && !texture.trim().isEmpty())
        {
            dictionary settings = new dictionary();
            settings.put(calendar.SETTINGS_BG_TEXTURE, texture.trim());
            calendar.setCalendarSettings(settings);
            sendSystemMessageTestingOnly(self, "\\#00ff00Background texture updated.");
        }

        return SCRIPT_CONTINUE;
    }

    private void showSettingsRectInput(obj_id player, String label, String key) throws InterruptedException
    {
        utils.setScriptVar(player, "calendar.settings.key", key);
        dictionary settings = calendar.getCalendarSettings();
        int current = settings.getInt(key);

        int pid = sui.inputbox(player, player, "Enter " + label + " value:\n\nCurrent: " + current, sui.OK_CANCEL, "Source Rect " + label, sui.INPUT_NORMAL, String.valueOf(current), "handleSettingsRectInput");
    }

    public int handleSettingsRectInput(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        if (bp == sui.BP_CANCEL)
        {
            utils.removeScriptVar(self, "calendar.settings.key");
            return SCRIPT_CONTINUE;
        }

        String valueStr = sui.getInputBoxText(params);
        String key = utils.getStringScriptVar(self, "calendar.settings.key");

        try
        {
            int value = Integer.parseInt(valueStr.trim());
            dictionary settings = new dictionary();
            settings.put(key, value);
            calendar.setCalendarSettings(settings);
            sendSystemMessageTestingOnly(self, "\\#00ff00Setting updated.");
        }
        catch (NumberFormatException e)
        {
            sendSystemMessageTestingOnly(self, "\\#ff4444Invalid number format.");
        }

        utils.removeScriptVar(self, "calendar.settings.key");
        return SCRIPT_CONTINUE;
    }
}

