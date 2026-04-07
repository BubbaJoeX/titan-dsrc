package script.npc.vendor;

import script.*;
import script.library.*;

public class vendor extends script.base_script
{
    public static final String VENDOR_TABLE_DIRECTORY = "datatables/item/vendor/";
    public static final String VENDOR_TABLE_OBJVAR = "item.vendor.vendor_table";
    public static final String VENDOR_CONTAINER_TEMPLATE = "object/tangible/container/vendor/npc_only.iff";
    public static final String OBJECT_FOR_SALE_DEFAULT_SCRIPT = "npc.vendor.object_for_sale";
    public static final String OBJECT_FOR_SALE_CASH_COST = "item.object_for_sale.cash_cost";
    public static final String OBJECT_FOR_SALE_TOKEN_COST = "item.object_for_sale.token_cost";
    public static final String OBJECT_FOR_SALE_FACTION_COST = "item.object_for_sale.faction_cost";
    public static final String OBJECT_FOR_SALE_FACTION = "item.object_for_sale.faction";
    public static final String OBJECT_FOR_SALE_BADGE_NEEDED = "item.object_for_sale.required_badge";
    public static final String OBJECT_FOR_SALE_COLLECTION_NEEDED = "item.object_for_sale.collection_required";
    public static final String VENDOR_CONTAINER_LIST_OBJVAR = "item.vendor.container_list";
    public static final String VENDOR_TOKEN_TYPE = "item.token.type";
    public static final int IMPERIAL = 10;
    public static final int REBEL = 11;
    public vendor()
    {
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        messageTo(self, "handleInitializeVendor", null, 10, false);
        return SCRIPT_CONTINUE;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        messageTo(self, "handleInitializeVendor", null, 10, false);
        return SCRIPT_CONTINUE;
    }

    public int handleInitializeVendor(obj_id self, dictionary params) throws InterruptedException
    {
        if (!hasObjVar(self, VENDOR_TABLE_OBJVAR))
        {
            return SCRIPT_CONTINUE;
        }
        if (hasObjVar(self, VENDOR_CONTAINER_LIST_OBJVAR))
        {
            if (utils.checkConfigFlag("GameServer", "forceVendorItemRecreation"))
            {
                handleCleanVendor(self);
            }
            return SCRIPT_CONTINUE;
        }
        if (isDead(self))
        {
            return SCRIPT_CONTINUE;
        }
        String vendorTable = VENDOR_TABLE_DIRECTORY + getStringObjVar(self, VENDOR_TABLE_OBJVAR) + ".iff";
        obj_id[] containerList = new obj_id[12];
        obj_id vendorInventory = utils.getInventoryContainer(self);
        int numRowsInTable = dataTableGetNumRows(vendorTable);
        if (numRowsInTable < 1)
        {
            return SCRIPT_CONTINUE;
        }
        for (int i = 0; i < numRowsInTable; i++)
        {
            int reqClass = dataTableGetInt(vendorTable, i, "class");
            if (reqClass > -1 && reqClass < containerList.length && !isIdValid(containerList[reqClass]) && !exists(containerList[reqClass]))
            {
                containerList[reqClass] = createObjectInInventoryAllowOverload(VENDOR_CONTAINER_TEMPLATE, self);
            }
        }
        for (int row = 0; row < numRowsInTable; row++)
        {
            int reqClass = dataTableGetInt(vendorTable, row, "class");
            String item = dataTableGetString(vendorTable, row, "item");
            int creditCost = dataTableGetInt(vendorTable, row, "cash");
            String faction = dataTableGetString(vendorTable, row, "faction");
            int factionCost = dataTableGetInt(vendorTable, row, "factionCost");
            String badgeName = dataTableGetString(vendorTable, row, "badge");
            String collectionCompleted = dataTableGetString(vendorTable, row, "collectionCompleted");
            int[] tokenCost = new int[trial.HEROIC_TOKENS.length];
            if (hasObjVar(self, VENDOR_TOKEN_TYPE))
            {
                String tokenList = getStringObjVar(self, VENDOR_TOKEN_TYPE);
                String[] differentTokens = split(tokenList, ',');
                tokenCost = new int[differentTokens.length];
            }
            for (int j = 0; j < tokenCost.length; j++)
            {
                if (dataTableHasColumn(vendorTable, "token" + j))
                {
                    tokenCost[j] = dataTableGetInt(vendorTable, row, "token" + j);
                }
                else
                {
                    tokenCost[j] = 0;
                }
            }
            if (reqClass > -1 && reqClass < containerList.length)
            {
                for (int idx = 0; idx < containerList.length; idx++)
                {
                    if (isIdValid(containerList[idx]) && exists(containerList[idx]) && (reqClass == 0 || reqClass == idx))
                    {
                        obj_id objectForSale;
                        if (static_item.isStaticItem(item))
                        {
                            objectForSale = static_item.createNewItemFunction(item, containerList[idx]);
                        }
                        else
                        {
                            objectForSale = createObjectOverloaded(item, containerList[idx]);
                        }

                        if (hasObjVar(objectForSale, "noTrade"))
                        {
                            removeObjVar(objectForSale, "noTrade");
                            attachScript(objectForSale, "item.special.nomove");
                        }

                        setObjVar(objectForSale, OBJECT_FOR_SALE_CASH_COST, creditCost);
                        setObjVar(objectForSale, OBJECT_FOR_SALE_TOKEN_COST, tokenCost);
                        if (faction != null && !faction.isEmpty()) {
                            setObjVar(objectForSale, OBJECT_FOR_SALE_FACTION, faction);
                        }
                        if (factionCost > 0) {
                            setObjVar(objectForSale, OBJECT_FOR_SALE_FACTION_COST, factionCost);
                        }
                        if (badgeName != null && !badgeName.isEmpty()) {
                            setObjVar(objectForSale, OBJECT_FOR_SALE_BADGE_NEEDED, badgeName);
                        }
                        if (collectionCompleted != null && !collectionCompleted.isEmpty()) {
                            setObjVar(objectForSale, OBJECT_FOR_SALE_COLLECTION_NEEDED, collectionCompleted);
                        }
                        if (hasObjVar(self, VENDOR_TOKEN_TYPE))
                        {
                            String tokenList = getStringObjVar(self, VENDOR_TOKEN_TYPE);
                            setObjVar(objectForSale, VENDOR_TOKEN_TYPE, tokenList);
                        }
                        String datatableObjectScript = dataTableGetString(vendorTable, row, "script");
                        if (datatableObjectScript != null && !datatableObjectScript.isEmpty())
                        {
                            if (!hasScript(objectForSale, datatableObjectScript))
                            {
                                attachScript(objectForSale, datatableObjectScript);
                            }
                        }
                        else
                        {
                            if (!hasScript(objectForSale, OBJECT_FOR_SALE_DEFAULT_SCRIPT))
                            {
                                attachScript(objectForSale, OBJECT_FOR_SALE_DEFAULT_SCRIPT);
                            }
                        }
                    }
                }
            }
        }
        setObjVar(self, VENDOR_CONTAINER_LIST_OBJVAR, containerList);
        chat.chat(self, "I am now open for business!");
        if (exists(self))
        {
            String logData = "";
            if (isMob(self))
            {
                String creatureTabName = getCreatureName(self);
                if (creatureTabName != null && !creatureTabName.isEmpty())
                {
                    String creatureNameLoc = localize(new string_id("mob/creature_names", creatureTabName));
                    if (creatureNameLoc == null)
                    {
                        creatureNameLoc = "Unknown Creature Name";
                    }
                    logData += "CREATURE NAME: ";
                    logData += creatureNameLoc + " ";
                    logData += "CREATURE CODE: ";
                    logData += creatureTabName + " ";
                }
                else
                {
                    logData += "Basic Creature Name Data Unknown! Template " + getTemplateName(self) + " ";
                }
            }
            else
            {
                logData += "Vendor Template " + getTemplateName(self) + " ";
            }
            logData += "Vendor Table: " + vendorTable + " ";
            if (hasObjVar(self, VENDOR_TOKEN_TYPE))
            {
                String tokenList = getStringObjVar(self, VENDOR_TOKEN_TYPE);
                logData += "Token List: " + tokenList + " ";
            }
            else
            {
                logData += "Token List: No Tokens ";
            }
            logData += "Location: " + getLocation(self) + " ";
            CustomerServiceLog("vendor_lite", "Vendor Lite setup completed. Vendor Data: " + logData);
        }
        return SCRIPT_CONTINUE;
    }

    public int showInventorySUI(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        int profession = utils.getPlayerProfession(player);
        if (factions.isImperial(player))
        {
            profession = IMPERIAL;
        }
        if (factions.isRebel(player))
        {
            profession = REBEL;
        }
        if (!hasObjVar(self, VENDOR_CONTAINER_LIST_OBJVAR))
        {
            sendSystemMessage(player, new string_id("set_bonus", "vendor_not_qualified"));
            return SCRIPT_CONTINUE;
        }
        obj_id[] containerList = getObjIdArrayObjVar(self, VENDOR_CONTAINER_LIST_OBJVAR);
        obj_id container = null;
        if (isIdValid(containerList[profession]) && exists(containerList[profession]))
        {
            container = containerList[profession];
        }
        else
        {
            container = containerList[0];
            profession = utils.getPlayerProfession(player);
            if (isIdValid(containerList[profession]) && exists(containerList[profession]))
            {
                container = containerList[profession];
            }
        }
        if (!isIdValid(container) || !exists(container))
        {
            sendSystemMessage(player, new string_id("set_bonus", "vendor_not_qualified"));
            return SCRIPT_CONTINUE;
        }
        queueCommand(player, (1880585606), container, "", COMMAND_PRIORITY_DEFAULT);
        return SCRIPT_CONTINUE;
    }

    public int showNonClassInventory(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = params.getObjId("player");
        obj_id[] containerList = getObjIdArrayObjVar(self, VENDOR_CONTAINER_LIST_OBJVAR);
        obj_id container = containerList[0];
        if (!isIdValid(container) || !exists(container))
        {
            sendSystemMessage(player, new string_id("set_bonus", "vendor_not_qualified"));
            return SCRIPT_CONTINUE;
        }
        queueCommand(player, (1880585606), container, "", COMMAND_PRIORITY_DEFAULT);
        return SCRIPT_CONTINUE;
    }

    /*
    The bug fix applied to prevent cyclical container creation on initialization that caused issues with persisted faction recruiters inside bases
    uses an ObjVar check against if the container array has already been set. However, this creates a new bug in that vendors cannot auto-update,
    meaning changes to the items, prices, etc. in a vendor item datatable does not actually update the vendor because the ObjVar check stops it from
    iterating through the items datatable to populate the vendor's containers on initialization.

    handleVendorCleanup is intended to be called either (a) when a change to a datatable has been detected or (b) when a config option forces the cleanup.
    A versioning solution for datatables that can auto-detect a change has been made and trigger a vendor to rebuild its containers is currently in the works (king Cekis).
    Until that is available, a config option to force the cleanup and re-initialization of vendors at startup is also available.
    Cleanup is handled by deleting both all items in the containers and the containers themselves to avoid orphaning random objects, and then triggering initialization again.
     */
    //TODO full implementation with versioning
    public void handleCleanVendor(obj_id self) throws InterruptedException
    {
        obj_id[] containers = getContents(utils.getInventoryContainer(self));
        for (obj_id container : containers)
        {
            if (getTemplateName(container).equals(VENDOR_CONTAINER_TEMPLATE))
            {
                obj_id[] contents = getContents(container);
                for (obj_id item : contents)
                {
                    destroyObject(item);
                }
                destroyObject(container);
            }
        }
        removeObjVar(self, VENDOR_CONTAINER_LIST_OBJVAR);
        messageTo(self, "handleInitializeVendor", null, 10, false);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU40, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU1, new string_id("Open Vendor"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (isGod(player) && item == menu_info_types.SERVER_MENU1)
        {
            openVendor(self, player);
        }
        return SCRIPT_CONTINUE;
    }

    public int openVendor(obj_id self, obj_id player)
    {
        dictionary d = new dictionary();
        d.put("player", player);
        messageTo(self, "showNonClassInventory", d, 0, false);
        return SCRIPT_CONTINUE;
    }
}