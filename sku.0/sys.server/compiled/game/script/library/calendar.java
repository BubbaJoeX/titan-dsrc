package script.library;

import script.*;
import java.util.Vector;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Calendar library for managing in-game events.
 * Supports Staff, Guild, City, and Server events.
 */
public class calendar extends script.base_script
{
    // Event types
    public static final int EVENT_TYPE_STAFF = 0;
    public static final int EVENT_TYPE_GUILD = 1;
    public static final int EVENT_TYPE_CITY = 2;
    public static final int EVENT_TYPE_SERVER = 3;

    // Recurrence types
    public static final int RECUR_NONE = 0;
    public static final int RECUR_DAILY = 1;
    public static final int RECUR_WEEKLY = 2;
    public static final int RECUR_MONTHLY = 3;
    public static final int RECUR_YEARLY = 4;

    // Event data keys
    public static final String KEY_EVENT_ID = "event_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_EVENT_TYPE = "event_type";
    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_END_TIME = "end_time";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_CREATOR_ID = "creator_id";
    public static final String KEY_GUILD_ID = "guild_id";
    public static final String KEY_CITY_ID = "city_id";
    public static final String KEY_SERVER_EVENT_KEY = "server_event_key";
    public static final String KEY_RECURRING = "recurring";
    public static final String KEY_RECURRENCE_TYPE = "recurrence_type";
    public static final String KEY_BROADCAST_START = "broadcast_start";
    public static final String KEY_ACTIVE = "active";
    public static final String KEY_YEAR = "year";
    public static final String KEY_MONTH = "month";
    public static final String KEY_DAY = "day";
    public static final String KEY_HOUR = "hour";
    public static final String KEY_MINUTE = "minute";

    // Server event keys (link to existing holiday system)
    public static final String[] SERVER_EVENTS = {
        "halloween",
        "lifeday",
        "loveday",
        "empireday_ceremony"
    };

    public static final String[] SERVER_EVENT_NAMES = {
        "Halloween",
        "Life Day",
        "Love Day",
        "Empire Day"
    };

    // Cluster-wide objvar storage location
    public static final String CALENDAR_OBJVAR_ROOT = "calendar";
    public static final String CALENDAR_EVENTS_OBJVAR = CALENDAR_OBJVAR_ROOT + ".events";
    public static final String CALENDAR_SETTINGS_OBJVAR = CALENDAR_OBJVAR_ROOT + ".settings";
    public static final String CALENDAR_EVENT_COUNT = CALENDAR_OBJVAR_ROOT + ".event_count";

    // Settings keys
    public static final String SETTINGS_BG_TEXTURE = "bg_texture";
    public static final String SETTINGS_SRC_RECT_X = "src_rect_x";
    public static final String SETTINGS_SRC_RECT_Y = "src_rect_y";
    public static final String SETTINGS_SRC_RECT_W = "src_rect_w";
    public static final String SETTINGS_SRC_RECT_H = "src_rect_h";

    // CUI Mediator names (must match SwgCuiMediatorTypes.h)
    public static final String CUI_CALENDAR = "WS_Calendar";
    public static final String CUI_CALENDAR_EVENT_EDITOR = "WS_CalendarEventEditor";
    public static final String CUI_CALENDAR_SETTINGS = "WS_CalendarSettings";

    // Broadcast colors
    public static final String COLOR_STAFF = "\\#FFD700";
    public static final String COLOR_GUILD = "\\#00FF00";
    public static final String COLOR_CITY = "\\#00BFFF";
    public static final String COLOR_SERVER = "\\#FF4500";
    public static final String COLOR_WHITE = "\\#FFFFFF";

    // =========================================================================
    // Permission Checks
    // =========================================================================

    public static boolean canCreateEvent(obj_id player, int eventType) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        // Staff can create any event type
        if (isGod(player))
            return true;

        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
                return false; // Only staff

            case EVENT_TYPE_GUILD:
                int guildId = getGuildId(player);
                if (guildId <= 0)
                    return false;
                // Check if player is guild leader or officer
                return guildGetLeader(guildId) == player ||
                       guildHasPermission(player, guild.GUILD_PERMISSION_MAIL);

            case EVENT_TYPE_CITY:
                int cityId = getCitizenOfCityId(player);
                if (cityId <= 0)
                    return false;
                // Check if player is mayor
                return city.isAMayor(player);

            case EVENT_TYPE_SERVER:
                return false; // Only staff can set server event dates

            default:
                return false;
        }
    }

    private static boolean guildHasPermission(obj_id player, int permission) throws InterruptedException
    {
        int guildId = getGuildId(player);
        if (guildId <= 0)
            return false;

        int memberPermissions = guildGetMemberPermissions(guildId, player);
        return (memberPermissions & permission) != 0;
    }

    public static boolean canEditEvent(obj_id player, dictionary eventData) throws InterruptedException
    {
        if (!isIdValid(player) || eventData == null)
            return false;

        if (isGod(player))
            return true;

        int eventType = eventData.getInt(KEY_EVENT_TYPE);
        String creatorId = eventData.getString(KEY_CREATOR_ID);

        // Creator can always edit their own event
        if (creatorId != null && creatorId.equals(player.toString()))
            return true;

        switch (eventType)
        {
            case EVENT_TYPE_GUILD:
                int guildId = eventData.getInt(KEY_GUILD_ID);
                int playerGuildId = getGuildId(player);
                if (guildId != playerGuildId)
                    return false;
                return guildGetLeader(guildId) == player;

            case EVENT_TYPE_CITY:
                int cityId = eventData.getInt(KEY_CITY_ID);
                int playerCityId = getCitizenOfCityId(player);
                if (cityId != playerCityId)
                    return false;
                return city.isAMayor(player);

            default:
                return false;
        }
    }

    public static boolean canDeleteEvent(obj_id player, dictionary eventData) throws InterruptedException
    {
        return canEditEvent(player, eventData);
    }

    public static boolean canViewEvent(obj_id player, dictionary eventData) throws InterruptedException
    {
        if (!isIdValid(player) || eventData == null)
            return false;

        if (isGod(player))
            return true;

        int eventType = eventData.getInt(KEY_EVENT_TYPE);

        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
            case EVENT_TYPE_SERVER:
                return true; // Everyone can view staff and server events

            case EVENT_TYPE_GUILD:
                int guildId = eventData.getInt(KEY_GUILD_ID);
                int playerGuildId = getGuildId(player);
                return guildId == playerGuildId;

            case EVENT_TYPE_CITY:
                int cityId = eventData.getInt(KEY_CITY_ID);
                int playerCityId = getCitizenOfCityId(player);
                return cityId == playerCityId;

            default:
                return false;
        }
    }

    // =========================================================================
    // Event CRUD Operations
    // =========================================================================

    public static String createEvent(obj_id creator, dictionary eventData) throws InterruptedException
    {
        if (!isIdValid(creator) || eventData == null)
            return null;

        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return null;

        // Generate unique event ID
        int eventCount = 0;
        if (hasObjVar(calendarObj, CALENDAR_EVENT_COUNT))
            eventCount = getIntObjVar(calendarObj, CALENDAR_EVENT_COUNT);

        eventCount++;
        String eventId = "evt_" + eventCount + "_" + getGameTime();

        // Set event data
        eventData.put(KEY_EVENT_ID, eventId);
        eventData.put(KEY_CREATOR_ID, creator.toString());
        eventData.put(KEY_ACTIVE, false);

        // Store event
        String eventObjVar = CALENDAR_EVENTS_OBJVAR + "." + eventId;
        setObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_ID, eventId);
        setObjVar(calendarObj, eventObjVar + "." + KEY_TITLE, eventData.getString(KEY_TITLE));
        setObjVar(calendarObj, eventObjVar + "." + KEY_DESCRIPTION, eventData.getString(KEY_DESCRIPTION));
        setObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_TYPE, eventData.getInt(KEY_EVENT_TYPE));
        setObjVar(calendarObj, eventObjVar + "." + KEY_START_TIME, eventData.getInt(KEY_START_TIME));
        setObjVar(calendarObj, eventObjVar + "." + KEY_END_TIME, eventData.getInt(KEY_END_TIME));
        setObjVar(calendarObj, eventObjVar + "." + KEY_DURATION, eventData.getInt(KEY_DURATION));
        setObjVar(calendarObj, eventObjVar + "." + KEY_CREATOR_ID, creator.toString());
        setObjVar(calendarObj, eventObjVar + "." + KEY_GUILD_ID, eventData.getInt(KEY_GUILD_ID));
        setObjVar(calendarObj, eventObjVar + "." + KEY_CITY_ID, eventData.getInt(KEY_CITY_ID));
        setObjVar(calendarObj, eventObjVar + "." + KEY_SERVER_EVENT_KEY, eventData.getString(KEY_SERVER_EVENT_KEY));
        setObjVar(calendarObj, eventObjVar + "." + KEY_RECURRING, eventData.getBoolean(KEY_RECURRING));
        setObjVar(calendarObj, eventObjVar + "." + KEY_RECURRENCE_TYPE, eventData.getInt(KEY_RECURRENCE_TYPE));
        setObjVar(calendarObj, eventObjVar + "." + KEY_BROADCAST_START, eventData.getBoolean(KEY_BROADCAST_START));
        setObjVar(calendarObj, eventObjVar + "." + KEY_ACTIVE, false);
        setObjVar(calendarObj, eventObjVar + "." + KEY_YEAR, eventData.getInt(KEY_YEAR));
        setObjVar(calendarObj, eventObjVar + "." + KEY_MONTH, eventData.getInt(KEY_MONTH));
        setObjVar(calendarObj, eventObjVar + "." + KEY_DAY, eventData.getInt(KEY_DAY));
        setObjVar(calendarObj, eventObjVar + "." + KEY_HOUR, eventData.getInt(KEY_HOUR));
        setObjVar(calendarObj, eventObjVar + "." + KEY_MINUTE, eventData.getInt(KEY_MINUTE));

        // Update event count
        setObjVar(calendarObj, CALENDAR_EVENT_COUNT, eventCount);

        // Add to event index
        addEventToIndex(calendarObj, eventId);

        // Notify relevant players about the new event
        notifyEventCreated(eventData);

        return eventId;
    }

    private static void addEventToIndex(obj_id calendarObj, String eventId) throws InterruptedException
    {
        String indexObjVar = CALENDAR_OBJVAR_ROOT + ".event_index";
        String[] currentIndex = null;

        if (hasObjVar(calendarObj, indexObjVar))
            currentIndex = getStringArrayObjVar(calendarObj, indexObjVar);

        String[] newIndex;
        if (currentIndex == null || currentIndex.length == 0)
        {
            newIndex = new String[]{eventId};
        }
        else
        {
            newIndex = new String[currentIndex.length + 1];
            System.arraycopy(currentIndex, 0, newIndex, 0, currentIndex.length);
            newIndex[currentIndex.length] = eventId;
        }

        setObjVar(calendarObj, indexObjVar, newIndex);
    }

    private static void removeEventFromIndex(obj_id calendarObj, String eventId) throws InterruptedException
    {
        String indexObjVar = CALENDAR_OBJVAR_ROOT + ".event_index";

        if (!hasObjVar(calendarObj, indexObjVar))
            return;

        String[] currentIndex = getStringArrayObjVar(calendarObj, indexObjVar);
        if (currentIndex == null || currentIndex.length == 0)
            return;

        Vector newIndexVec = new Vector();
        for (String id : currentIndex)
        {
            if (!id.equals(eventId))
                newIndexVec.add(id);
        }

        String[] newIndex = new String[newIndexVec.size()];
        newIndexVec.toArray(newIndex);
        setObjVar(calendarObj, indexObjVar, newIndex);
    }

    public static boolean updateEvent(String eventId, dictionary eventData) throws InterruptedException
    {
        if (eventId == null || eventData == null)
            return false;

        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return false;

        String eventObjVar = CALENDAR_EVENTS_OBJVAR + "." + eventId;
        if (!hasObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_ID))
            return false;

        // Update fields
        if (eventData.containsKey(KEY_TITLE))
            setObjVar(calendarObj, eventObjVar + "." + KEY_TITLE, eventData.getString(KEY_TITLE));
        if (eventData.containsKey(KEY_DESCRIPTION))
            setObjVar(calendarObj, eventObjVar + "." + KEY_DESCRIPTION, eventData.getString(KEY_DESCRIPTION));
        if (eventData.containsKey(KEY_START_TIME))
            setObjVar(calendarObj, eventObjVar + "." + KEY_START_TIME, eventData.getInt(KEY_START_TIME));
        if (eventData.containsKey(KEY_END_TIME))
            setObjVar(calendarObj, eventObjVar + "." + KEY_END_TIME, eventData.getInt(KEY_END_TIME));
        if (eventData.containsKey(KEY_DURATION))
            setObjVar(calendarObj, eventObjVar + "." + KEY_DURATION, eventData.getInt(KEY_DURATION));
        if (eventData.containsKey(KEY_BROADCAST_START))
            setObjVar(calendarObj, eventObjVar + "." + KEY_BROADCAST_START, eventData.getBoolean(KEY_BROADCAST_START));
        if (eventData.containsKey(KEY_RECURRING))
            setObjVar(calendarObj, eventObjVar + "." + KEY_RECURRING, eventData.getBoolean(KEY_RECURRING));
        if (eventData.containsKey(KEY_RECURRENCE_TYPE))
            setObjVar(calendarObj, eventObjVar + "." + KEY_RECURRENCE_TYPE, eventData.getInt(KEY_RECURRENCE_TYPE));
        if (eventData.containsKey(KEY_YEAR))
            setObjVar(calendarObj, eventObjVar + "." + KEY_YEAR, eventData.getInt(KEY_YEAR));
        if (eventData.containsKey(KEY_MONTH))
            setObjVar(calendarObj, eventObjVar + "." + KEY_MONTH, eventData.getInt(KEY_MONTH));
        if (eventData.containsKey(KEY_DAY))
            setObjVar(calendarObj, eventObjVar + "." + KEY_DAY, eventData.getInt(KEY_DAY));
        if (eventData.containsKey(KEY_HOUR))
            setObjVar(calendarObj, eventObjVar + "." + KEY_HOUR, eventData.getInt(KEY_HOUR));
        if (eventData.containsKey(KEY_MINUTE))
            setObjVar(calendarObj, eventObjVar + "." + KEY_MINUTE, eventData.getInt(KEY_MINUTE));

        // Notify relevant players about the update
        dictionary updatedEvent = getEvent(eventId);
        if (updatedEvent != null)
        {
            notifyEventUpdated(updatedEvent);
        }

        return true;
    }

    public static boolean deleteEvent(String eventId) throws InterruptedException
    {
        if (eventId == null)
            return false;

        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return false;

        String eventObjVar = CALENDAR_EVENTS_OBJVAR + "." + eventId;
        if (!hasObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_ID))
            return false;

        // Get event data before deletion for notification
        dictionary eventData = getEvent(eventId);

        // Delete the event
        removeObjVar(calendarObj, eventObjVar);
        removeEventFromIndex(calendarObj, eventId);

        // Notify relevant players about the deletion
        if (eventData != null)
        {
            notifyEventDeleted(eventData);
        }

        return true;
    }

    public static dictionary getEvent(String eventId) throws InterruptedException
    {
        if (eventId == null)
            return null;

        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return null;

        String eventObjVar = CALENDAR_EVENTS_OBJVAR + "." + eventId;
        if (!hasObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_ID))
            return null;

        dictionary eventData = new dictionary();
        eventData.put(KEY_EVENT_ID, getStringObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_ID));
        eventData.put(KEY_TITLE, getStringObjVar(calendarObj, eventObjVar + "." + KEY_TITLE));
        eventData.put(KEY_DESCRIPTION, getStringObjVar(calendarObj, eventObjVar + "." + KEY_DESCRIPTION));
        eventData.put(KEY_EVENT_TYPE, getIntObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_TYPE));
        eventData.put(KEY_START_TIME, getIntObjVar(calendarObj, eventObjVar + "." + KEY_START_TIME));
        eventData.put(KEY_END_TIME, getIntObjVar(calendarObj, eventObjVar + "." + KEY_END_TIME));
        eventData.put(KEY_DURATION, getIntObjVar(calendarObj, eventObjVar + "." + KEY_DURATION));
        eventData.put(KEY_CREATOR_ID, getStringObjVar(calendarObj, eventObjVar + "." + KEY_CREATOR_ID));
        eventData.put(KEY_GUILD_ID, getIntObjVar(calendarObj, eventObjVar + "." + KEY_GUILD_ID));
        eventData.put(KEY_CITY_ID, getIntObjVar(calendarObj, eventObjVar + "." + KEY_CITY_ID));
        eventData.put(KEY_SERVER_EVENT_KEY, getStringObjVar(calendarObj, eventObjVar + "." + KEY_SERVER_EVENT_KEY));
        eventData.put(KEY_RECURRING, getBooleanObjVar(calendarObj, eventObjVar + "." + KEY_RECURRING));
        eventData.put(KEY_RECURRENCE_TYPE, getIntObjVar(calendarObj, eventObjVar + "." + KEY_RECURRENCE_TYPE));
        eventData.put(KEY_BROADCAST_START, getBooleanObjVar(calendarObj, eventObjVar + "." + KEY_BROADCAST_START));
        eventData.put(KEY_ACTIVE, getBooleanObjVar(calendarObj, eventObjVar + "." + KEY_ACTIVE));
        eventData.put(KEY_YEAR, getIntObjVar(calendarObj, eventObjVar + "." + KEY_YEAR));
        eventData.put(KEY_MONTH, getIntObjVar(calendarObj, eventObjVar + "." + KEY_MONTH));
        eventData.put(KEY_DAY, getIntObjVar(calendarObj, eventObjVar + "." + KEY_DAY));
        eventData.put(KEY_HOUR, getIntObjVar(calendarObj, eventObjVar + "." + KEY_HOUR));
        eventData.put(KEY_MINUTE, getIntObjVar(calendarObj, eventObjVar + "." + KEY_MINUTE));

        return eventData;
    }

    public static String[] getAllEventIds() throws InterruptedException
    {
        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return new String[0];

        String indexObjVar = CALENDAR_OBJVAR_ROOT + ".event_index";
        if (!hasObjVar(calendarObj, indexObjVar))
            return new String[0];

        return getStringArrayObjVar(calendarObj, indexObjVar);
    }

    public static dictionary[] getEventsForPlayer(obj_id player) throws InterruptedException
    {
        String[] allEventIds = getAllEventIds();
        Vector visibleEvents = new Vector();

        for (String eventId : allEventIds)
        {
            dictionary eventData = getEvent(eventId);
            if (eventData != null && canViewEvent(player, eventData))
                visibleEvents.add(eventData);
        }

        dictionary[] result = new dictionary[visibleEvents.size()];
        visibleEvents.toArray(result);
        return result;
    }

    public static dictionary[] getEventsForMonth(obj_id player, int year, int month) throws InterruptedException
    {
        dictionary[] allEvents = getEventsForPlayer(player);
        Vector monthEvents = new Vector();

        for (dictionary eventData : allEvents)
        {
            int eventYear = eventData.getInt(KEY_YEAR);
            int eventMonth = eventData.getInt(KEY_MONTH);

            if (eventYear == year && eventMonth == month)
                monthEvents.add(eventData);
        }

        dictionary[] result = new dictionary[monthEvents.size()];
        monthEvents.toArray(result);
        return result;
    }

    public static dictionary[] getEventsForDay(obj_id player, int year, int month, int day) throws InterruptedException
    {
        dictionary[] allEvents = getEventsForPlayer(player);
        Vector dayEvents = new Vector();

        for (dictionary eventData : allEvents)
        {
            int eventYear = eventData.getInt(KEY_YEAR);
            int eventMonth = eventData.getInt(KEY_MONTH);
            int eventDay = eventData.getInt(KEY_DAY);

            if (eventYear == year && eventMonth == month && eventDay == day)
                dayEvents.add(eventData);
        }

        dictionary[] result = new dictionary[dayEvents.size()];
        dayEvents.toArray(result);
        return result;
    }

    // =========================================================================
    // Time Utilities
    // =========================================================================

    public static int getCurrentYear() throws InterruptedException
    {
        return getCalendarTimeNow().get(Calendar.YEAR);
    }

    public static int getCurrentMonth() throws InterruptedException
    {
        return getCalendarTimeNow().get(Calendar.MONTH) + 1; // Calendar months are 0-based
    }

    public static int getCurrentDay() throws InterruptedException
    {
        return getCalendarTimeNow().get(Calendar.DAY_OF_MONTH);
    }

    public static int getCurrentHour() throws InterruptedException
    {
        return getCalendarTimeNow().get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentMinute() throws InterruptedException
    {
        return getCalendarTimeNow().get(Calendar.MINUTE);
    }

    public static Calendar getCalendarTimeNow() throws InterruptedException
    {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(System.currentTimeMillis());
        return cal;
    }

    public static int getDaysInMonth(int year, int month) throws InterruptedException
    {
        Calendar cal = new GregorianCalendar(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static int getDayOfWeek(int year, int month, int day) throws InterruptedException
    {
        Calendar cal = new GregorianCalendar(year, month - 1, day);
        return cal.get(Calendar.DAY_OF_WEEK); // 1=Sunday, 7=Saturday
    }

    public static int getFirstDayOfMonth(int year, int month) throws InterruptedException
    {
        return getDayOfWeek(year, month, 1);
    }

    public static String getMonthName(int month) throws InterruptedException
    {
        String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        if (month >= 1 && month <= 12)
            return months[month - 1];
        return "Unknown";
    }

    public static String formatDate(int year, int month, int day) throws InterruptedException
    {
        return getMonthName(month) + " " + day + ", " + year;
    }

    public static String formatTime(int hour, int minute) throws InterruptedException
    {
        String ampm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0)
            displayHour = 12;
        String minuteStr = minute < 10 ? "0" + minute : String.valueOf(minute);
        return displayHour + ":" + minuteStr + " " + ampm;
    }

    // =========================================================================
    // Broadcasting
    // =========================================================================

    public static void broadcastEventStart(dictionary eventData) throws InterruptedException
    {
        if (eventData == null)
            return;

        int eventType = eventData.getInt(KEY_EVENT_TYPE);
        String title = eventData.getString(KEY_TITLE);
        String description = eventData.getString(KEY_DESCRIPTION);

        String message;
        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
                message = COLOR_STAFF + "[GALACTIC EVENT]: " + title + "\n" + COLOR_WHITE + description;
                broadcastGalaxyWide(message);
                break;

            case EVENT_TYPE_GUILD:
                int guildId = eventData.getInt(KEY_GUILD_ID);
                message = COLOR_GUILD + "[GUILD EVENT]: " + title + "\n" + COLOR_WHITE + description;
                broadcastToGuild(guildId, message);
                break;

            case EVENT_TYPE_CITY:
                int cityId = eventData.getInt(KEY_CITY_ID);
                message = COLOR_CITY + "[CITY EVENT]: " + title + "\n" + COLOR_WHITE + description;
                broadcastToCity(cityId, message);
                break;

            case EVENT_TYPE_SERVER:
                String eventKey = eventData.getString(KEY_SERVER_EVENT_KEY);
                message = COLOR_SERVER + "[SERVER EVENT]: " + title + " has begun!\n" +
                          COLOR_WHITE + "The " + eventKey + " event is now active across the galaxy!";
                broadcastGalaxyWide(message);
                break;
        }
    }

    public static void broadcastGalaxyWide(String message) throws InterruptedException
    {
        // Use cluster-wide broadcast
        obj_id[] allPlayers = getAllPlayers();
        for (obj_id player : allPlayers)
        {
            if (isIdValid(player) && isPlayer(player))
                sendSystemMessageTestingOnly(player, message);
        }
    }

    public static void broadcastToGuild(int guildId, String message) throws InterruptedException
    {
        if (guildId <= 0)
            return;

        obj_id[] guildMembers = guild.getMemberIds(guildId, false, true);
        if (guildMembers == null)
            return;

        for (obj_id member : guildMembers)
        {
            if (isIdValid(member) && isPlayer(member))
                sendSystemMessageTestingOnly(member, message);
        }
    }

    public static void broadcastToCity(int cityId, String message) throws InterruptedException
    {
        if (cityId <= 0)
            return;

        obj_id[] citizens = cityGetCitizenIds(cityId);
        if (citizens == null)
            return;

        for (obj_id citizen : citizens)
        {
            if (isIdValid(citizen) && isPlayer(citizen))
                sendSystemMessageTestingOnly(citizen, message);
        }
    }

    // =========================================================================
    // Calendar Object Management
    // =========================================================================

    public static obj_id getCalendarObject() throws InterruptedException
    {
        // Use the planet object as cluster-wide storage
        obj_id planet = getPlanetByName("tatooine");
        if (!isIdValid(planet))
        {
            // Fallback to any available planet
            String[] planets = {"tatooine", "naboo", "corellia", "dantooine"};
            for (String p : planets)
            {
                planet = getPlanetByName(p);
                if (isIdValid(planet))
                    break;
            }
        }
        return planet;
    }

    // =========================================================================
    // Settings Management
    // =========================================================================

    public static dictionary getCalendarSettings() throws InterruptedException
    {
        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return getDefaultSettings();

        if (!hasObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_BG_TEXTURE))
            return getDefaultSettings();

        dictionary settings = new dictionary();
        settings.put(SETTINGS_BG_TEXTURE, getStringObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_BG_TEXTURE));
        settings.put(SETTINGS_SRC_RECT_X, getIntObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_X));
        settings.put(SETTINGS_SRC_RECT_Y, getIntObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_Y));
        settings.put(SETTINGS_SRC_RECT_W, getIntObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_W));
        settings.put(SETTINGS_SRC_RECT_H, getIntObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_H));

        return settings;
    }

    public static dictionary getDefaultSettings() throws InterruptedException
    {
        dictionary settings = new dictionary();
        settings.put(SETTINGS_BG_TEXTURE, "ui_calendar_bg.dds");
        settings.put(SETTINGS_SRC_RECT_X, 0);
        settings.put(SETTINGS_SRC_RECT_Y, 0);
        settings.put(SETTINGS_SRC_RECT_W, 512);
        settings.put(SETTINGS_SRC_RECT_H, 512);
        return settings;
    }

    public static void setCalendarSettings(dictionary settings) throws InterruptedException
    {
        if (settings == null)
            return;

        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return;

        if (settings.containsKey(SETTINGS_BG_TEXTURE))
            setObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_BG_TEXTURE, settings.getString(SETTINGS_BG_TEXTURE));
        if (settings.containsKey(SETTINGS_SRC_RECT_X))
            setObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_X, settings.getInt(SETTINGS_SRC_RECT_X));
        if (settings.containsKey(SETTINGS_SRC_RECT_Y))
            setObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_Y, settings.getInt(SETTINGS_SRC_RECT_Y));
        if (settings.containsKey(SETTINGS_SRC_RECT_W))
            setObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_W, settings.getInt(SETTINGS_SRC_RECT_W));
        if (settings.containsKey(SETTINGS_SRC_RECT_H))
            setObjVar(calendarObj, CALENDAR_SETTINGS_OBJVAR + "." + SETTINGS_SRC_RECT_H, settings.getInt(SETTINGS_SRC_RECT_H));
    }

    // =========================================================================
    // Event Type Helpers
    // =========================================================================

    public static String getEventTypeName(int eventType) throws InterruptedException
    {
        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
                return "Staff Event";
            case EVENT_TYPE_GUILD:
                return "Guild Event";
            case EVENT_TYPE_CITY:
                return "City Event";
            case EVENT_TYPE_SERVER:
                return "Server Event";
            default:
                return "Unknown";
        }
    }

    public static String getEventTypeColor(int eventType) throws InterruptedException
    {
        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
                return COLOR_STAFF;
            case EVENT_TYPE_GUILD:
                return COLOR_GUILD;
            case EVENT_TYPE_CITY:
                return COLOR_CITY;
            case EVENT_TYPE_SERVER:
                return COLOR_SERVER;
            default:
                return COLOR_WHITE;
        }
    }

    // =========================================================================
    // Server Event Integration
    // =========================================================================

    public static void setEventActive(String eventId, boolean active) throws InterruptedException
    {
        obj_id calendarObj = getCalendarObject();
        if (!isIdValid(calendarObj))
            return;

        String eventObjVar = CALENDAR_EVENTS_OBJVAR + "." + eventId;
        if (hasObjVar(calendarObj, eventObjVar + "." + KEY_EVENT_ID))
            setObjVar(calendarObj, eventObjVar + "." + KEY_ACTIVE, active);
    }

    public static boolean isEventActive(String eventId) throws InterruptedException
    {
        dictionary eventData = getEvent(eventId);
        if (eventData == null)
            return false;
        return eventData.getBoolean(KEY_ACTIVE);
    }

    // =========================================================================
    // CUI Mediator Integration
    // =========================================================================

    /**
     * Opens the main Calendar CUI window on the client.
     * @param player The player to show the calendar to.
     * @return true if the UI effect was sent successfully.
     */
    public static boolean openCalendarCUI(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        return playUiEffect(player, "showMediator=" + CUI_CALENDAR);
    }

    /**
     * Closes the main Calendar CUI window on the client.
     * @param player The player to close the calendar for.
     * @return true if the UI effect was sent successfully.
     */
    public static boolean closeCalendarCUI(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        return playUiEffect(player, "hideMediator=" + CUI_CALENDAR);
    }

    /**
     * Opens the Event Editor CUI window on the client.
     * @param player The player to show the editor to.
     * @return true if the UI effect was sent successfully.
     */
    public static boolean openEventEditorCUI(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        return playUiEffect(player, "showMediator=" + CUI_CALENDAR_EVENT_EDITOR);
    }

    /**
     * Closes the Event Editor CUI window on the client.
     * @param player The player to close the editor for.
     * @return true if the UI effect was sent successfully.
     */
    public static boolean closeEventEditorCUI(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        return playUiEffect(player, "hideMediator=" + CUI_CALENDAR_EVENT_EDITOR);
    }

    /**
     * Opens the Calendar Settings CUI window on the client (staff only).
     * @param player The player to show settings to.
     * @return true if the UI effect was sent successfully.
     */
    public static boolean openSettingsCUI(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        if (!isGod(player))
        {
            sendSystemMessageTestingOnly(player, "\\#ff4444Access denied. Staff only.");
            return false;
        }

        return playUiEffect(player, "showMediator=" + CUI_CALENDAR_SETTINGS);
    }

    /**
     * Closes the Calendar Settings CUI window on the client.
     * @param player The player to close settings for.
     * @return true if the UI effect was sent successfully.
     */
    public static boolean closeSettingsCUI(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return false;

        return playUiEffect(player, "hideMediator=" + CUI_CALENDAR_SETTINGS);
    }

    /**
     * Sends calendar event data to the client.
     * Uses object variables on the player to pass data that the client CUI can read.
     * @param player The player to send data to.
     * @param events Array of event dictionaries.
     */
    public static void sendCalendarDataToClient(obj_id player, dictionary[] events) throws InterruptedException
    {
        if (!isIdValid(player) || events == null)
            return;

        // Clear any existing calendar data on the player
        removeObjVar(player, "calendar_ui");

        // Store event count
        setObjVar(player, "calendar_ui.event_count", events.length);

        // Store each event
        for (int i = 0; i < events.length; i++)
        {
            dictionary evt = events[i];
            if (evt == null)
                continue;

            String prefix = "calendar_ui.events." + i;
            setObjVar(player, prefix + ".id", evt.getString(KEY_EVENT_ID));
            setObjVar(player, prefix + ".title", evt.getString(KEY_TITLE));
            setObjVar(player, prefix + ".type", evt.getInt(KEY_EVENT_TYPE));
            setObjVar(player, prefix + ".year", evt.getInt(KEY_YEAR));
            setObjVar(player, prefix + ".month", evt.getInt(KEY_MONTH));
            setObjVar(player, prefix + ".day", evt.getInt(KEY_DAY));
            setObjVar(player, prefix + ".hour", evt.getInt(KEY_HOUR));
            setObjVar(player, prefix + ".minute", evt.getInt(KEY_MINUTE));
            setObjVar(player, prefix + ".duration", evt.getInt(KEY_DURATION));
            setObjVar(player, prefix + ".active", evt.getBoolean(KEY_ACTIVE));
        }
    }

    /**
     * Clears calendar UI data from the player.
     * @param player The player to clear data from.
     */
    public static void clearCalendarDataFromClient(obj_id player) throws InterruptedException
    {
        if (!isIdValid(player))
            return;

        removeObjVar(player, "calendar_ui");
    }

    // =========================================================================
    // Event Notification System
    // =========================================================================

    /**
     * Notifies all relevant players that a calendar event was created.
     * @param eventData The event that was created.
     */
    public static void notifyEventCreated(dictionary eventData) throws InterruptedException
    {
        if (eventData == null)
            return;

        int eventType = eventData.getInt(KEY_EVENT_TYPE);
        String title = eventData.getString(KEY_TITLE);
        String color = getEventTypeColor(eventType);
        String typeName = getEventTypeName(eventType);

        String message = color + "[Calendar] New " + typeName + " event: " + title;

        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
            case EVENT_TYPE_SERVER:
                // Notify all online players
                notifyAllOnlinePlayers(message, eventData);
                break;

            case EVENT_TYPE_GUILD:
                // Notify guild members
                int guildId = eventData.getInt(KEY_GUILD_ID);
                notifyGuildMembers(guildId, message, eventData);
                break;

            case EVENT_TYPE_CITY:
                // Notify city citizens
                int cityId = eventData.getInt(KEY_CITY_ID);
                notifyCityCitizens(cityId, message, eventData);
                break;
        }
    }

    /**
     * Notifies all relevant players that a calendar event was updated.
     * @param eventData The event that was updated.
     */
    public static void notifyEventUpdated(dictionary eventData) throws InterruptedException
    {
        if (eventData == null)
            return;

        int eventType = eventData.getInt(KEY_EVENT_TYPE);
        String title = eventData.getString(KEY_TITLE);
        String color = getEventTypeColor(eventType);

        String message = color + "[Calendar] Event updated: " + title;

        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
            case EVENT_TYPE_SERVER:
                notifyAllOnlinePlayers(message, eventData);
                break;

            case EVENT_TYPE_GUILD:
                int guildId = eventData.getInt(KEY_GUILD_ID);
                notifyGuildMembers(guildId, message, eventData);
                break;

            case EVENT_TYPE_CITY:
                int cityId = eventData.getInt(KEY_CITY_ID);
                notifyCityCitizens(cityId, message, eventData);
                break;
        }
    }

    /**
     * Notifies all relevant players that a calendar event was deleted.
     * @param eventData The event that was deleted.
     */
    public static void notifyEventDeleted(dictionary eventData) throws InterruptedException
    {
        if (eventData == null)
            return;

        int eventType = eventData.getInt(KEY_EVENT_TYPE);
        String title = eventData.getString(KEY_TITLE);

        String message = "\\#888888[Calendar] Event cancelled: " + title;

        switch (eventType)
        {
            case EVENT_TYPE_STAFF:
            case EVENT_TYPE_SERVER:
                notifyAllOnlinePlayers(message, eventData);
                break;

            case EVENT_TYPE_GUILD:
                int guildId = eventData.getInt(KEY_GUILD_ID);
                notifyGuildMembers(guildId, message, eventData);
                break;

            case EVENT_TYPE_CITY:
                int cityId = eventData.getInt(KEY_CITY_ID);
                notifyCityCitizens(cityId, message, eventData);
                break;
        }
    }

    /**
     * Notifies all online players about a calendar change.
     */
    private static void notifyAllOnlinePlayers(String message, dictionary eventData) throws InterruptedException
    {
        // Get all online players
        obj_id[] onlinePlayers = getAllPlayers();
        if (onlinePlayers == null)
            return;

        for (obj_id player : onlinePlayers)
        {
            if (isIdValid(player))
            {
                sendCalendarNotification(player, message, eventData);
            }
        }
    }

    /**
     * Notifies all online guild members about a calendar change.
     */
    private static void notifyGuildMembers(int guildId, String message, dictionary eventData) throws InterruptedException
    {
        if (guildId <= 0)
            return;

        obj_id[] members = guild.getMemberIds(guildId, false, true);
        if (members == null)
            return;

        for (obj_id member : members)
        {
            // Only notify if player is online
            if (isIdValid(member) && isPlayerActive(member))
            {
                sendCalendarNotification(member, message, eventData);
            }
        }
    }

    /**
     * Notifies all online city citizens about a calendar change.
     */
    private static void notifyCityCitizens(int cityId, String message, dictionary eventData) throws InterruptedException
    {
        if (cityId <= 0)
            return;

        obj_id[] citizens = cityGetCitizenIds(cityId);
        if (citizens == null)
            return;

        for (obj_id citizen : citizens)
        {
            // Only notify if player is online
            if (isIdValid(citizen) && isPlayerActive(citizen))
            {
                sendCalendarNotification(citizen, message, eventData);
            }
        }
    }

    /**
     * Sends a calendar notification to a specific player.
     * This includes a chat message and triggers the client to refresh if calendar is open.
     */
    private static void sendCalendarNotification(obj_id player, String message, dictionary eventData) throws InterruptedException
    {
        if (!isIdValid(player))
            return;

        // Send chat message
        sendSystemMessageTestingOnly(player, message);

        // Set a flag to indicate calendar data has changed
        setObjVar(player, "calendar_ui.data_changed", true);
        setObjVar(player, "calendar_ui.last_update", getGameTime());

        // Send a message to the player's calendar script if attached
        if (hasScript(player, "player.player_calendar"))
        {
            dictionary params = new dictionary();
            params.put("event_data", eventData);
            params.put("action", "refresh");
            messageTo(player, "onCalendarDataChanged", params, 0, false);
        }
    }

    /**
     * Checks if a player is currently online/active.
     */
    public static boolean isPlayerActive(obj_id player)
    {
        if (!isIdValid(player))
            return false;

        // Check if player object exists and is connected
        try
        {
            return exists(player) && isPlayer(player);
        }
        catch (InterruptedException e)
        {
            return false;
        }
    }

    /**
     * Gets all online player objects.
     * Note: This uses the PC room tracking or similar mechanism.
     */
    private static obj_id[] getAllPlayers() throws InterruptedException
    {
        // Use the cluster-wide player tracking
        // This is a simplified version - in production you'd use proper player tracking
        return getPlayerCreaturesInRange(getLocation(getCalendarObject()), 1000000.0f);
    }
}
