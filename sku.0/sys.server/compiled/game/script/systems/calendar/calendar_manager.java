package script.systems.calendar;

import script.*;
import script.library.*;

/**
 * Calendar Manager - Attached to a planet object (cluster-wide).
 * Manages event lifecycle, checking for events that need to start/end.
 */
public class calendar_manager extends script.base_script
{
    public static final int CHECK_INTERVAL = 60; // Check every 60 seconds
    public static final String SCRIPT_NAME = "systems.calendar.calendar_manager";

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "*Calendar Manager*");
        messageTo(self, "checkCalendarEvents", null, CHECK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "checkCalendarEvents", null, CHECK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    /**
     * Periodic check for events that need to start or end.
     */
    public int checkCalendarEvents(obj_id self, dictionary params) throws InterruptedException
    {
        int currentYear = calendar.getCurrentYear();
        int currentMonth = calendar.getCurrentMonth();
        int currentDay = calendar.getCurrentDay();
        int currentHour = calendar.getCurrentHour();
        int currentMinute = calendar.getCurrentMinute();

        String[] eventIds = calendar.getAllEventIds();
        for (String eventId : eventIds)
        {
            dictionary eventData = calendar.getEvent(eventId);
            if (eventData == null)
                continue;

            int eventYear = eventData.getInt(calendar.KEY_YEAR);
            int eventMonth = eventData.getInt(calendar.KEY_MONTH);
            int eventDay = eventData.getInt(calendar.KEY_DAY);
            int eventHour = eventData.getInt(calendar.KEY_HOUR);
            int eventMinute = eventData.getInt(calendar.KEY_MINUTE);
            int duration = eventData.getInt(calendar.KEY_DURATION);
            boolean isActive = eventData.getBoolean(calendar.KEY_ACTIVE);

            // Check if event should start
            if (!isActive && shouldEventStart(currentYear, currentMonth, currentDay, currentHour, currentMinute,
                                               eventYear, eventMonth, eventDay, eventHour, eventMinute))
            {
                startEvent(eventId, eventData);
            }
            // Check if event should end
            else if (isActive && shouldEventEnd(currentYear, currentMonth, currentDay, currentHour, currentMinute,
                                                 eventYear, eventMonth, eventDay, eventHour, eventMinute, duration))
            {
                endEvent(eventId, eventData);
            }
        }

        // Schedule next check
        messageTo(self, "checkCalendarEvents", null, CHECK_INTERVAL, false);
        return SCRIPT_CONTINUE;
    }

    private boolean shouldEventStart(int curYear, int curMonth, int curDay, int curHour, int curMinute,
                                     int evtYear, int evtMonth, int evtDay, int evtHour, int evtMinute) throws InterruptedException
    {
        // Event starts when current time equals or passes event time
        if (curYear > evtYear) return true;
        if (curYear < evtYear) return false;

        if (curMonth > evtMonth) return true;
        if (curMonth < evtMonth) return false;

        if (curDay > evtDay) return true;
        if (curDay < evtDay) return false;

        if (curHour > evtHour) return true;
        if (curHour < evtHour) return false;

        return curMinute >= evtMinute;
    }

    private boolean shouldEventEnd(int curYear, int curMonth, int curDay, int curHour, int curMinute,
                                   int evtYear, int evtMonth, int evtDay, int evtHour, int evtMinute,
                                   int durationMinutes) throws InterruptedException
    {
        // Calculate end time
        int endMinute = evtMinute + durationMinutes;
        int endHour = evtHour + (endMinute / 60);
        endMinute = endMinute % 60;
        int endDay = evtDay + (endHour / 24);
        endHour = endHour % 24;

        // Simple check (doesn't handle month rollover perfectly, but good enough for most events)
        int daysInMonth = calendar.getDaysInMonth(evtYear, evtMonth);
        int endMonth = evtMonth;
        int endYear = evtYear;
        if (endDay > daysInMonth)
        {
            endDay = endDay - daysInMonth;
            endMonth++;
            if (endMonth > 12)
            {
                endMonth = 1;
                endYear++;
            }
        }

        // Check if current time is past end time
        if (curYear > endYear) return true;
        if (curYear < endYear) return false;

        if (curMonth > endMonth) return true;
        if (curMonth < endMonth) return false;

        if (curDay > endDay) return true;
        if (curDay < endDay) return false;

        if (curHour > endHour) return true;
        if (curHour < endHour) return false;

        return curMinute >= endMinute;
    }

    private void startEvent(String eventId, dictionary eventData) throws InterruptedException
    {
        calendar.setEventActive(eventId, true);

        // Broadcast if enabled
        boolean broadcastStart = eventData.getBoolean(calendar.KEY_BROADCAST_START);
        if (broadcastStart)
        {
            calendar.broadcastEventStart(eventData);
        }

        // If this is a server event, trigger the holiday system
        int eventType = eventData.getInt(calendar.KEY_EVENT_TYPE);
        if (eventType == calendar.EVENT_TYPE_SERVER)
        {
            String serverEventKey = eventData.getString(calendar.KEY_SERVER_EVENT_KEY);
            if (serverEventKey != null && !serverEventKey.isEmpty())
            {
                triggerServerEvent(serverEventKey);
            }
        }

        LOG("calendar", "Event started: " + eventData.getString(calendar.KEY_TITLE));
    }

    private void endEvent(String eventId, dictionary eventData) throws InterruptedException
    {
        calendar.setEventActive(eventId, false);

        // If this is a server event, stop the holiday system
        int eventType = eventData.getInt(calendar.KEY_EVENT_TYPE);
        if (eventType == calendar.EVENT_TYPE_SERVER)
        {
            String serverEventKey = eventData.getString(calendar.KEY_SERVER_EVENT_KEY);
            if (serverEventKey != null && !serverEventKey.isEmpty())
            {
                stopServerEvent(serverEventKey);
            }
        }

        // Handle recurring events
        boolean recurring = eventData.getBoolean(calendar.KEY_RECURRING);
        if (recurring)
        {
            scheduleNextRecurrence(eventId, eventData);
        }

        LOG("calendar", "Event ended: " + eventData.getString(calendar.KEY_TITLE));
    }

    private void triggerServerEvent(String eventKey) throws InterruptedException
    {
        // Map to holiday controller methods
        obj_id holidayController = getHolidayController();
        if (!isIdValid(holidayController))
            return;

        dictionary params = new dictionary();
        params.put("eventKey", eventKey);

        switch (eventKey)
        {
            case "halloween":
                messageTo(holidayController, "halloweenStartForReals", params, 0, false);
                break;
            case "lifeday":
                messageTo(holidayController, "lifedayStartForReals", params, 0, false);
                break;
            case "loveday":
                messageTo(holidayController, "lovedayStartForReals", params, 0, false);
                break;
            case "empireday_ceremony":
                messageTo(holidayController, "empiredayStartForReals", params, 0, false);
                break;
        }
    }

    private void stopServerEvent(String eventKey) throws InterruptedException
    {
        obj_id holidayController = getHolidayController();
        if (!isIdValid(holidayController))
            return;

        dictionary params = new dictionary();
        params.put("eventKey", eventKey);

        switch (eventKey)
        {
            case "halloween":
                messageTo(holidayController, "halloweenStopForReals", params, 0, false);
                break;
            case "lifeday":
                messageTo(holidayController, "lifedayStopForReals", params, 0, false);
                break;
            case "loveday":
                messageTo(holidayController, "lovedayStopForReals", params, 0, false);
                break;
            case "empireday_ceremony":
                messageTo(holidayController, "empiredayStopForReals", params, 0, false);
                break;
        }
    }

    private obj_id getHolidayController() throws InterruptedException
    {
        // Find the holiday controller object
        obj_id planet = getPlanetByName(getCurrentSceneName());
        if (!isIdValid(planet))
            return null;

        // The holiday controller should be a child of the planet or have a specific name
        obj_id[] contents = getContents(planet);
        if (contents != null)
        {
            for (obj_id obj : contents)
            {
                if (hasScript(obj, "event.holiday_controller"))
                    return obj;
            }
        }

        return null;
    }

    private void scheduleNextRecurrence(String eventId, dictionary eventData) throws InterruptedException
    {
        int recurrenceType = eventData.getInt(calendar.KEY_RECURRENCE_TYPE);
        int year = eventData.getInt(calendar.KEY_YEAR);
        int month = eventData.getInt(calendar.KEY_MONTH);
        int day = eventData.getInt(calendar.KEY_DAY);

        switch (recurrenceType)
        {
            case calendar.RECUR_DAILY:
                day++;
                break;
            case calendar.RECUR_WEEKLY:
                day += 7;
                break;
            case calendar.RECUR_MONTHLY:
                month++;
                break;
            case calendar.RECUR_YEARLY:
                year++;
                break;
            default:
                return;
        }

        // Handle day/month overflow
        int daysInMonth = calendar.getDaysInMonth(year, month);
        while (day > daysInMonth)
        {
            day -= daysInMonth;
            month++;
            if (month > 12)
            {
                month = 1;
                year++;
            }
            daysInMonth = calendar.getDaysInMonth(year, month);
        }

        while (month > 12)
        {
            month -= 12;
            year++;
        }

        // Update the event with new date
        dictionary updateData = new dictionary();
        updateData.put(calendar.KEY_YEAR, year);
        updateData.put(calendar.KEY_MONTH, month);
        updateData.put(calendar.KEY_DAY, day);

        calendar.updateEvent(eventId, updateData);
    }
}

