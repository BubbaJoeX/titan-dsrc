package script.item.content.rewards;/*
@Origin: script.item.content.rewards.parcel_mailbox
@Author: BubbaJoeX
@Purpose: Allows players to send items to other players online (or offline) if the mailboxes are on the same planet and setup.
@Note:
    This does not work cross-planets and most likely never will. This is more so for moving items across the planet.
    An example: Player A has a house outside the janta stronghold,  Player B has a house near the Pirate Outpost, you can send those items to your house with a short delay.
@Requirements  <no requirements>
@TODO: Pipe item to move them into a specified container inside.
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.*/

import script.*;
import script.library.city;
import script.library.sui;
import script.library.utils;

import java.util.Random;

@SuppressWarnings("unused")
public class parcel_mailbox extends base_script
{
    public static final String VAR_ADDRESS = "parcel_mailbox.address";
    public static final String VAR_OWNER = "parcel_mailbox.owner";
    public static final String VAR_SETUP = "parcel_mailbox.setup";
    //public static final float VAR_MAIL_SPEED_NON_CITY = 300f;
    public static final float VAR_MAIL_SPEED_NON_CITY = 2f;//live
    //public static final float VAR_MAIL_SPEED_CITY = 60f;
    public static final float VAR_MAIL_SPEED_CITY = 1f;//live

    public parcel_mailbox()
    {
    }

    public void sendItemMail(obj_id self, obj_id player) throws InterruptedException
    {
        sui.inputbox(self, player, "Please enter the non-capitalized name of which you wish to send these items to.", sui.OK_CANCEL, "MAILBOX", sui.INPUT_NORMAL, null, "handleMailTo", null);
    }

    public String generatePostalCode(obj_id self) throws InterruptedException
    {
        Random rand = new Random();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        StringBuilder sb = new StringBuilder();
        sb.append("MB-");
        for (int i = 0; i < 4; i++)
        {
            sb.append(letters.charAt(rand.nextInt(letters.length())));
        }
        sb.append("-");
        sb.append(digits.charAt(rand.nextInt(10)));
        sb.append(digits.charAt(rand.nextInt(10)));

        if (city.isInCity(getLocation(self)))
        {
            sb.append("-C");
        }
        else
        {
            sb.append("-W");
        }
        return sb.toString();
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        reinitializeMailbox(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        reinitializeMailbox(self);
        return SCRIPT_CONTINUE;
    }

    public int reinitializeMailbox(obj_id self)
    {
        setName(self, "Parcel Mailbox");
        setDescriptionStringId(self, new string_id("This mailbox can be used to send items to other players on the same planet as you."));
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self)
    {
        if (hasObjVar(getPlanetByName("tatooine"), "mailbox_" + getObjIdObjVar(self, VAR_OWNER)))
        {
            removeObjVar(getPlanetByName("tatooine"), "mailbox_" + getObjIdObjVar(self, VAR_OWNER));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnAboutToReceiveItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (isPlayer(transferer) || hasScript(srcContainer, "item.content.rewards.parcel_mailbox"))
        {
            LOG("ethereal", "[Mailbox]: " + "Item " + getNameNoSpam(item) + " is being placed in the mailbox by " + transferer + ".");
            return SCRIPT_CONTINUE;
        }
        else
        {
            LOG("ethereal", "[Mailbox]: " + "Item " + getNameNoSpam(item) + " was blocked from being placed in the mailbox by " + transferer + ".");
            return SCRIPT_OVERRIDE;
        }
    }

    public int OnAboutToLoseItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (hasScript(srcContainer, "item.content.rewards.parcel_mailbox") || transferer == getObjIdObjVar(self, VAR_OWNER))
        {
            LOG("ethereal", "[Mailbox]: " + "Item " + getNameNoSpam(item) + " is being removed from the mailbox by " + transferer + ".");
            return SCRIPT_CONTINUE;
        }
        else
        {
            LOG("ethereal", "[Mailbox]: " + "Item " + getNameNoSpam(item) + " was blocked from being removed from the mailbox by " + transferer + ".");
            return SCRIPT_OVERRIDE;
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!hasObjVar(self, VAR_SETUP))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU10, new string_id("Initialize Mailbox"));
        }
        else if (player == getObjIdObjVar(self, VAR_OWNER) && getFilledVolume(self) > 0 && hasObjVar(self, VAR_SETUP))
        {
            int mailRadial = mi.addRootMenu(menu_info_types.SERVER_MENU11, new string_id("Mail Menu"));
            mi.addSubMenu(mailRadial, menu_info_types.SERVER_MENU12, new string_id("Send Parcels"));
            mi.addSubMenu(mailRadial, menu_info_types.SERVER_MENU13, new string_id("Retrieve Parcels"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.SERVER_MENU10)
        {
            if (hasObjVar(player, "mailboxObj"))
            {
                broadcast(player, "You already have initialized a mailbox, and cannot setup another.");
                LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to initialize a mailbox but already has one.");
                return SCRIPT_CONTINUE;
            }
            if (!hasObjVar(self, VAR_OWNER))
            {
                setObjVar(self, VAR_ADDRESS, generatePostalCode(self));
                setObjVar(self, VAR_OWNER, player);
                setObjVar(self, VAR_SETUP, 1);
                setObjVar(getPlanetByName("tatooine"), "mailbox_" + player, self);
                broadcast(player, "You have initialized your mailbox. In order to send parcels, your recipient's mailbox must be initialized as well.");
                setObjVar(player, "mailboxObj", self);
                LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " has initialized mailbox " + self + ".");
                return SCRIPT_CONTINUE;
            }
        }
        else if (hasObjVar(self, VAR_OWNER))
        {
            if (player == getObjIdObjVar(self, VAR_OWNER))
            {
                if (item == menu_info_types.SERVER_MENU13)
                {
                    obj_id[] contents = getContents(self);
                    for (obj_id discardedItem : contents)
                    {
                        putInOverloaded(discardedItem, utils.getInventoryContainer(player));
                    }
                    return SCRIPT_CONTINUE;
                }
                if (item == menu_info_types.SERVER_MENU12)
                {
                    LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " is attempting to send items to another player.");
                    sendItemMail(self, player);
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnReceivedItem(obj_id self, obj_id srcContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (hasObjVar(self, VAR_OWNER))
        {
            if (isIdValid(getObjIdObjVar(self, VAR_OWNER)) && transferer != getObjIdObjVar(self, VAR_OWNER))
            {
                //fails if player is not on planet.
                broadcast(transferer, "You have received a parcel from " + getPlayerName(getObjIdObjVar(self, VAR_OWNER)) + ".");
                return SCRIPT_CONTINUE;
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int handleMailTo(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        String recipient = sui.getInputBoxText(params);
        if (recipient == null || recipient.isEmpty())
        {
            broadcast(player, "The name you entered is not valid. Try again.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to send items to an invalid player. [Call 1]");
            return SCRIPT_CONTINUE;
        }
        obj_id recipientId = getPlayerIdFromFirstName(recipient);
        if (!isIdValid(recipientId))
        {
            broadcast(player, "That player does not exist.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to send items to an invalid player. [Call 2]");
            return SCRIPT_CONTINUE;
        }
        if (recipientId == player)
        {
            broadcast(player, "You cannot send items to yourself.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to send items to themselves.");
            return SCRIPT_CONTINUE;
        }
        obj_id destinationContainer = getObjIdObjVar(getPlanetByName("tatooine"), "mailbox_" + recipientId);
        if (!canContentsFitInto(self, destinationContainer))
        {
            broadcast(player, "That player's mailbox is full.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to send items to " + getPlayerName(recipientId) + "'s mailbox but it was full.");
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(destinationContainer))
        {
            broadcast(player, "That player does not have a mailbox on this planet.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to send items to " + getPlayerName(recipientId) + "'s mailbox but they do not have one on this planet.");
            return SCRIPT_CONTINUE;
        }
        if (getFilledVolume(self) == 0)
        {
            broadcast(player, "You have no items to send.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " attempted to send items to " + getPlayerName(recipientId) + "'s mailbox but they had no items to send.");
            return SCRIPT_CONTINUE;
        }
        if (!isCityMailbox(self))
        {
            dictionary d = new dictionary();
            d.put("sender", player);
            d.put("recipientId", recipientId);
            messageTo(self, "handleDelayedMailTo", d, VAR_MAIL_SPEED_NON_CITY, true);
            broadcast(player, "Your items will be sent to " + getPlayerName(recipientId) + "'s mailbox in 5 minutes. You can cancel this by retrieving your items from your mailbox.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " has sent items to " + getPlayerName(recipientId) + "'s mailbox. Delay: " + VAR_MAIL_SPEED_NON_CITY + " seconds.");
        }
        else
        {
            obj_id[] items = getContents(self);
            int numItems = items.length;
            for (obj_id item : items)
            {
                putIn(item, destinationContainer);
            }
            broadcast(player, "You have sent " + numItems + " items to " + getPlayerName(recipientId) + "'s mailbox.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " has sent " + numItems + " items to " + getPlayerName(recipientId) + "'s mailbox. Delay: 0 seconds.");
        }
        LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " ended mailbox transaction.");
        return SCRIPT_CONTINUE;
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
        {
            LOG("ethereal", "[Mailbox]: " + "Attributes idx was -1 for mailbox.");
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(self, VAR_ADDRESS))
        {
            names[idx] = utils.packStringId(new string_id("Address"));
            attribs[idx] = getStringObjVar(self, VAR_ADDRESS);
            idx++;
        }
        if (hasObjVar(self, VAR_OWNER))
        {
            names[idx] = utils.packStringId(new string_id("Owner"));
            attribs[idx] = getPlayerName(getObjIdObjVar(self, VAR_OWNER));
        }
        return SCRIPT_CONTINUE;
    }

    public boolean isCityMailbox(obj_id self)
    {
        String postalCode = getStringObjVar(self, VAR_ADDRESS);
        return postalCode.endsWith("-C");
    }

    public int handleDelayedMailTo(obj_id self, dictionary d) throws InterruptedException
    {
        obj_id player = d.getObjId("sender");
        obj_id recipient = d.getObjId("recipientId");
        obj_id[] items = getContents(self);
        int numItems = items.length;
        if (numItems == 0)
        {
            broadcast(player, "You have canceled a delivery.");
            LOG("ethereal", "[Mailbox]: " + utils.getStringName(player) + " canceled their delayed delivery.");
            return SCRIPT_CONTINUE;
        }
        obj_id destinationContainer = getObjIdObjVar(getPlanetByName("tatooine"), "mailbox_" + recipient);
        for (obj_id item : items)
        {
            LOG("ethereal", "[Mailbox]: Attempting to place item " + getNameNoSpam(item) + " inside " + getPlayerName(recipient) + "'s mailbox.");
            putIn(item, destinationContainer, player);
        }
        return SCRIPT_CONTINUE;
    }

    public boolean canContentsFitInto(obj_id containerSelf, obj_id containerDestination)
    {
        return getVolumeFree(containerDestination) >= getFilledVolume(containerSelf);
    }
}
