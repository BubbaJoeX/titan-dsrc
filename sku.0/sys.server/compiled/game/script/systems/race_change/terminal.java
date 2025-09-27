package script.systems.race_change;/*
@Origin: script.systems.race_change.race_changer
@Author: BubbaJoeX
@Purpose: Opens a race changer UI and queries the database to change your template to the server template requested.
@Notes: This is only intended to change race/gender To change gender they will need to select their same race, but select the opposite (Male/Female)
@Created: Thursday, 03/01/2023 , at 12:00 AM.
@Copyright © SWG-OR 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
//import script.library.oracle;
import script.library.sui;
import script.library.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class terminal extends script.base_script
{
    public static final String LIVE = "swg";
    public static final String LIVE_IP = "swg";
    public static final String TEST = "swg";
    public static final String TEST_IP = "swg";
    public static final String TABLE = "datatables/adhoc/race_changer.iff";
    public final int LOCKOUT_TIME = 1296000;// 15 days in seconds

    public int OnAttach(obj_id self)
    {
        setName(self, "Cosmetic Surgery Terminal");
        setDescriptionString(self, "This terminal allows you to change your race.");
        setCondition(self, CONDITION_INTERESTING);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        setName(self, "Cosmetic Surgery Terminal");
        setDescriptionString(self, "This terminal allows you to change your race.");
        setCondition(self, CONDITION_INTERESTING);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Change Race"));
        if (isGod(player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Remove Lockout"));
        }
        return SCRIPT_CONTINUE;
    }

    public boolean canRaceChange(obj_id player)
    {
        int currentTime = getCalendarTime();
        if (hasObjVar(player, "raceChange"))
        {
            int lastRaceChange = getIntObjVar(player, "raceChange");
            int timeSinceLastChange = currentTime - lastRaceChange;
            if (timeSinceLastChange < LOCKOUT_TIME)
            {
                broadcast(player, "You must wait 15 days before changing your race again.");
                return false;
            }
            else
            {
                return true;
            }
        }
        return true;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            if (canRaceChange(player))
            {
                confirmRaceChangeSUI(self, player);
            }
        }
        else if (item == menu_info_types.SERVER_MENU1 && isGod(player))
        {
            removeObjVar(player, "raceChange");
        }
        return SCRIPT_CONTINUE;
    }

    public int confirmRaceChangeSUI(obj_id self, obj_id player) throws InterruptedException
    {
        String prompt = "Changing your race will exit out of your current game client and update your character. Hitting 'OK' will start this process.\n\nAre you sure you wish to continue?";
        String title = "Race Change Confirmation";
        sui.msgbox(self, player, prompt, sui.OK_CANCEL, title, "handleRaceChangeConfirmation");
        return SCRIPT_CONTINUE;
    }

    public int handleRaceChangeConfirmation(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int button = sui.getIntButtonPressed(params);
        if (button == sui.BP_OK)
        {
            startRaceChangeSUI(self, player);
        }
        else
        {
            broadcast(player, "You have declined the race change.");
        }
        return SCRIPT_CONTINUE;
    }

    public int startRaceChangeSUI(obj_id self, obj_id player) throws InterruptedException
    {
        String[] raceSelections = dataTableGetStringColumn("datatables/adhoc/race_changer.iff", "SHOW_NAME");
        sui.listbox(self, player, "Please select a new race. \n\nNOTE: You will not be able to change races for 15 days after you make a selection.", sui.OK_CANCEL, "Race Change", raceSelections, "handleRaceSelection", true, false);
        return 0;
    }

    public int handleRaceSelection(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id operator = sui.getPlayerId(params);
        int selectionIndex = sui.getListboxSelectedRow(params);
        int buttonPressed = sui.getIntButtonPressed(params);
        if (buttonPressed == sui.OK_CANCEL)
        {
            broadcast(operator, "You have canceled the race change.");
            return SCRIPT_CONTINUE;
        }
        if (selectionIndex == -1)
        {
            return SCRIPT_CONTINUE;
        }
        String desiredSpeciesName = dataTableGetString(TABLE, selectionIndex, "SHOW_NAME");
        int desiredTemplate = dataTableGetInt(TABLE, selectionIndex, "DB_ID");
        LOG("ethereal", "[Race Change]: " + getPlayerFullName(operator) + " has selected: " + desiredSpeciesName);
        runUpdateRace(self, operator, desiredTemplate);
        return SCRIPT_CONTINUE;
    }

    public int runUpdateRace(obj_id self, obj_id player, int selection) throws InterruptedException
    {
        setObjVar(player, "raceChange", getCalendarTime());
        broadcast(player, "Exiting Game.");
        setObjVar(player, "safeLogout", true);
        sendConsoleCommand("/quit", player);
        disconnectPlayer(player);
        //updatePlayer(player);

        String clusterName = getClusterName();
        String dbHost = clusterName.equalsIgnoreCase(LIVE) ? LIVE_IP : TEST_IP;
        String serviceName = "swg";
        String dbUsername = "swg";
        String dbPassword = "swg";

        LOG("ethereal", "[Race Change]: " + getPlayerFullName(player) + " is changing their race to: " + selection);
        updateRaceInDatabase(dbHost, "1521", serviceName, dbUsername, dbPassword, selection, String.valueOf(player));
        return SCRIPT_CONTINUE;
    }

    private void updateRaceInDatabase(String dbHost, String port, String serviceName, String dbUsername, String dbPassword, int templateId, String objectId)
    {/*
        String url = "jdbc:oracle:thin:@" + dbHost + ":" + port + ":" + serviceName;
        String sql1 = "UPDATE SWG.SWG_CHARACTERS SET TEMPLATE_ID = ? WHERE OBJECT_ID = ?";
        String sql2 = "UPDATE SWG.OBJECTS SET OBJECT_TEMPLATE_ID = ? WHERE OBJECT_ID = ?";

        try (Connection conn = oracle.connect();
             PreparedStatement stmt1 = conn.prepareStatement(sql1);
             PreparedStatement stmt2 = conn.prepareStatement(sql2))
        {

            conn.setAutoCommit(false); // Start transaction

            stmt1.setString(1, String.valueOf(templateId));
            stmt1.setString(2, objectId);
            stmt1.executeUpdate();

            stmt2.setInt(1, templateId);
            stmt2.setString(2, objectId);
            stmt2.executeUpdate();

            conn.commit(); // Commit transaction

            LOG("ethereal", "[Race Change]: Database update successful for object_id " + objectId);
        } catch (SQLException e)
        {
            LOG("ethereal", "[Race Change]: Database update failed - " + e.getMessage());
        }
    }


    public void updatePlayer(obj_id player) throws InterruptedException
    {
        obj_id[] equippedItems = getInventoryAndEquipment(player);
        LOG("ethereal", "[Race Change]: " + getPlayerFullName(player) + " has " + equippedItems.length + " items to unequip. Moving all items.");
        for (obj_id equippedItem : equippedItems)
        {
            if (isIdValid(equippedItem))
            {
                putIn(equippedItem, utils.getInventoryContainer(player));
            }
        }
        obj_id hair = utils.getObjectInSlot(player, "hair");
        if (isIdValid(hair))
        {
            LOG("ethereal", "[Race Change]: " + getPlayerFullName(player) + " will have illegal hair. Destroying " + hair + " to prevent visual issues.");
            destroyObject(hair);
        }
        else
        {
            LOG("ethereal", "[Race Change]: " + getPlayerFullName(player) + " has no illegal hair to delete.");
        }
        LOG("ethereal", "[Race Change]: " + getPlayerFullName(player) + " has been updated. Setting raceUpdated objvar for next OnLogin()");
        setObjVar(player, "raceUpdated", true);
*/
    }
}