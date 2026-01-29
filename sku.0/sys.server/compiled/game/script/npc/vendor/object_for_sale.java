package script.npc.vendor;

/*
 * Copyright © SWG-OR 2024.
 *
 * Unauthorized usage, viewing or sharing of this file is prohibited.
 */

import script.dictionary;
import script.library.*;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

import java.util.HashMap;
import java.util.Map;

public class object_for_sale extends script.base_script
{
    public static final String VENDOR_TOKEN_TYPE = "item.token.type";
    public static final string_id SID_INV_FULL = new string_id("spam", "npc_vendor_player_inv_full");

    public object_for_sale()
    {
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setObjVar(self, township.OBJECT_FOR_SALE_ON_VENDOR, true);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_PUBLIC_CONTAINER_USE1)
        {
            if (hasObjVar(self, "faction_recruiter.faction"))
            {
                String itemFactionName = getStringObjVar(self, "faction_recruiter.faction");
                int playerFaction = pvpGetAlignedFaction(player);
                String playerFactionName = factions.getFactionNameByHashCode(playerFaction);
                if (!itemFactionName.equals(playerFactionName))
                {
                    sendSystemMessage(player, new string_id("spam", "wrong_faction"));
                    return SCRIPT_OVERRIDE;
                }
            }
            if (!confirmInventory(self, player))
            {
                return SCRIPT_OVERRIDE;
            }
            if (confirmFunds(self, player))
            {
                processItemPurchase(self, player);
            }
            else
            {
                broadcast(player, "You do not have enough credits, currency, faction points or the required badge to purchase this item.");
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        if (names == null || attribs == null || names.length != attribs.length)
        {
            return SCRIPT_CONTINUE;
        }
        final int firstFreeIndex = getFirstFreeIndex(names);
        if (firstFreeIndex >= 0 && firstFreeIndex < names.length)
        {
            names[firstFreeIndex] = utils.packStringId(new string_id("set_bonus", "vendor_cost"));
            attribs[firstFreeIndex] = createPureConcatenatedJoy(self);
        }
        if (hasObjVar(self, vendor.OBJECT_FOR_SALE_FACTION) && hasObjVar(self, vendor.OBJECT_FOR_SALE_FACTION_COST))
        {
            final int nextFreeIndex = getFirstFreeIndex(names);
            if (nextFreeIndex >= 0 && nextFreeIndex < names.length)
            {
                String faction = getStringObjVar(self, vendor.OBJECT_FOR_SALE_FACTION);
                String factionDisplayName = getFactionalPurchaseDisplayName(self, faction);
                int factionCost = getIntObjVar(self, vendor.OBJECT_FOR_SALE_FACTION_COST);
                names[nextFreeIndex] = utils.packStringId(new string_id("Factional Cost"));
                attribs[nextFreeIndex] = "(" + factionDisplayName + " - " + factionCost + ")";
            }
        }
        if (hasObjVar(self, "item.object_for_sale.required_badge"))
        {
            final int nextFreeIndex = getFirstFreeIndex(names);
            if (nextFreeIndex >= 0 && nextFreeIndex < names.length)
            {
                String badgeRequired = getStringObjVar(self, vendor.OBJECT_FOR_SALE_BADGE_NEEDED);
                names[nextFreeIndex] = utils.packStringId(new string_id("Badge Needed"));
                if (!badge.hasBadge(player, badgeRequired))
                {
                    attribs[nextFreeIndex] = "\\#FF0000" + localize(new string_id("collection_n", badgeRequired));
                }
                else
                {
                    attribs[nextFreeIndex] = "\\#00FF00" + localize(new string_id("collection_n", badgeRequired));
                }
            }
        }
        if (hasObjVar(self, vendor.OBJECT_FOR_SALE_COLLECTION_NEEDED))
        {
            final int nextFreeIndex = getFirstFreeIndex(names);
            if (nextFreeIndex >= 0 && nextFreeIndex < names.length)
            {
                String collectionNeeded = getStringObjVar(self, vendor.OBJECT_FOR_SALE_COLLECTION_NEEDED);
                names[nextFreeIndex] = utils.packStringId(new string_id("Collection completion required"));
                if (!badge.hasBadge(player, collectionNeeded))
                {
                    attribs[nextFreeIndex] = "\\#FF0000" + localize(new string_id("collection_n", collectionNeeded));
                }
                else
                {
                    attribs[nextFreeIndex] = "\\#00FF00" + localize(new string_id("collection_n", collectionNeeded));
                }
            }
        }

        return SCRIPT_CONTINUE;
    }

    private String getFactionalPurchaseDisplayName(obj_id self, String faction)
    {
        dictionary itemData = dataTableGetRow("datatables/faction/faction.iff", faction);
        if (itemData != null)
        {
            return itemData.getString("displayName");
        }
        return "";
    }

    public String createPureConcatenatedJoy(obj_id self)
    {
        StringBuilder pureConcatenatedJoy = new StringBuilder(getString(new string_id("set_bonus", "vendor_sale_object_justify_line")));
        int creditCost = 0;
        int tokenArrayLength = 0;
        creditCost = getIntObjVar(self, "item.object_for_sale.cash_cost");
        int[] tokenCost = getIntArrayObjVar(self, "item.object_for_sale.token_cost");
        if (!hasObjVar(self, VENDOR_TOKEN_TYPE))
        {
            if (tokenCost.length > trial.HEROIC_TOKENS.length)
            {
                tokenArrayLength = trial.HEROIC_TOKENS.length;
            }
            else if (tokenCost.length > 0)
            {
                tokenArrayLength = tokenCost.length;
            }
            for (int i = 0; i < tokenArrayLength; i++)
            {
                if (tokenCost[i] > 0)
                {
                    pureConcatenatedJoy.append("[").append(tokenCost[i]).append("] ").append(getString(new string_id("static_item_n", trial.HEROIC_TOKENS[i]))).append("\n");
                }
            }
        }
        else
        {
            String tokenList = getStringObjVar(self, VENDOR_TOKEN_TYPE);
            String[] differentTokens = split(tokenList, ',');
            if (tokenCost.length > differentTokens.length)
            {
                tokenArrayLength = differentTokens.length;
            }
            else if (tokenCost.length > 0)
            {
                tokenArrayLength = tokenCost.length;
            }
            for (int i = 0; i < tokenArrayLength; i++)
            {
                if (tokenCost[i] > 0)
                {
                    pureConcatenatedJoy.append("[").append(tokenCost[i]).append("] ").append(getString(new string_id("static_item_n", differentTokens[i]))).append("\n");
                }
            }
        }
        if (creditCost > 0)
        {
            pureConcatenatedJoy.append(creditCost).append(getString(new string_id("set_bonus", "vendor_credits")));
        }
        return pureConcatenatedJoy.toString();
    }

    public boolean confirmInventory(obj_id self, obj_id player)
    {
        obj_id pInv = utils.getInventoryContainer(player);
        if (!isValidId(pInv) || !exists(pInv))
        {
            return false;
        }
        if (getVolumeFree(pInv) <= 0)
        {
            sendSystemMessage(player, SID_INV_FULL);
            return false;
        }
        return true;
    }

    public boolean confirmFunds(obj_id self, obj_id player) throws InterruptedException
    {
        if (isGod(player) || hasObjVar(player, "vend")) return true; // if you're a god or have the vend var, you can buy anything.
        int creditCost = getIntObjVar(self, "item.object_for_sale.cash_cost");
        int[] tokenCosts = getIntArrayObjVar(self, "item.object_for_sale.token_cost");
        String faction = getStringObjVar(self, vendor.OBJECT_FOR_SALE_FACTION);
        int factionCost = getIntObjVar(self, vendor.OBJECT_FOR_SALE_FACTION_COST);
        String badgeRequired = getStringObjVar(self, vendor.OBJECT_FOR_SALE_BADGE_NEEDED);
        String collectionRequired = getStringObjVar(self, vendor.OBJECT_FOR_SALE_COLLECTION_NEEDED);

        // do we have enough credits for the item?
        boolean hasTheCredits = (creditCost < 1 || money.hasFunds(player, money.MT_TOTAL, creditCost));

        // bail if we don't have enough credits - doesn't even matter if we have enough tokens or not.
        if (!hasTheCredits)
        {
            LOG("ethereal", "[NPC Vendor]: Player " + getFirstName(player) + "(" + player + ") attempted to purchase item " + getName(self) + "(" + self + ") but did not have enough credits.");
            return false;
        }

        // check if the player has enough faction points
        if (faction != null && factionCost > 0 && !confirmFactionPoints(player, faction, (float) factionCost))
        {
            LOG("ethereal", "[NPC Vendor]: Player " + getFirstName(player) + "(" + player + ") attempted to purchase item " + getName(self) + "(" + self + ") but did not have enough faction points.");
            return false;
        }

        if (badgeRequired != null && !badgeRequired.isEmpty() && !badge.hasBadge(player, badgeRequired))
        {
            LOG("ethereal", "[NPC Vendor]: Player " + getFirstName(player) + "(" + player + ") attempted to purchase item " + getName(self) + "(" + self + ") but did not have the required badge.");
            return false;
        }

        if (collectionRequired != null && !collectionRequired.isEmpty() && !collection.hasCompletedCollection(player, collectionRequired))
        {
            LOG("ethereal", "[NPC Vendor]: Player " + getFirstName(player) + "(" + player + ") attempted to purchase item " + getName(self) + "(" + self + ") but did not have the required badge.");
            return false;
        }

        // this thing may have token cost requirements to it so we have to check if we have enough tokens now.
        Map<String, Integer> tokensNeeded = new HashMap<>();
        Map<String, Integer> tokensFound = new HashMap<>();

        // see if the vendor has a token type assigned to it.  If it does, store that in tokensNeeded.
        // if it doesn't, then we still need to check for heroic or special coin costs.  We don't yet
        // have a situation where something costs s special coin AND a heroic coin so ignore that possibility.
        if (hasObjVar(self, VENDOR_TOKEN_TYPE))
        {
            String[] vendorTokenTypes = split(getStringObjVar(self, VENDOR_TOKEN_TYPE), ',');
            for (int i = 0; i < tokenCosts.length; i++)
            {
                if (tokenCosts[i] > 0)
                {
                    tokensNeeded.put(vendorTokenTypes[i], tokenCosts[i]);
                    tokensFound.put(vendorTokenTypes[i], 0);
                }
            }
        }
        else
        {
            for (int i = 0; i < tokenCosts.length; i++)
            {
                if (tokenCosts[i] > 0)
                {
                    tokensNeeded.put(trial.HEROIC_TOKENS[i], tokenCosts[i]);
                    tokensFound.put(trial.HEROIC_TOKENS[i], 0);
                }
            }
        }

        // bail if we don't need tokens because we've already proven we have the credits for it
        if (tokensNeeded.isEmpty())
        {
            return true;
        }

        boolean foundTokenHolderBox = false;
        obj_id[] playerInventoryItems = getInventoryAndEquipment(player);
        for (obj_id inventoryItem : playerInventoryItems)
        {
            String itemName = getStaticItemName(inventoryItem);
            if (itemName == null || itemName.isEmpty()) continue;

            // first see if the item is a token
            if (tokensNeeded.containsKey(itemName) && tokensNeeded.get(itemName) > 0)
            {
                // this is a token that we need, let's see if we have enough of them
                tokensFound.put(itemName, tokensFound.get(itemName) + getCount(inventoryItem));
            }
            // next, see if the item is a token box - all other item types can be ignored
            else if (itemName.equalsIgnoreCase("item_heroic_token_box_01_01") && !foundTokenHolderBox)
            {
                // this is a token box... let's see if we have this token in our box
                // first, let's be efficient - we can only have one so set a flag that prevents us from "finding another"
                foundTokenHolderBox = true;
                int[] tokensInBox = getIntArrayObjVar(inventoryItem, "item.set.tokens_held");
                // check if we have this token type in the box
                for (int k = 0; k < trial.HEROIC_TOKENS.length; k++)
                {
                    String checkToken = trial.HEROIC_TOKENS[k];
                    if (!tokensNeeded.containsKey(checkToken))
                    {
                        continue;
                    }
                    if (tokensNeeded.containsKey(checkToken) && tokensNeeded.get(checkToken) > 0 && tokensInBox[k] > 0)
                    {
                        tokensFound.put(checkToken, tokensFound.get(checkToken) + tokensInBox[k]);
                    }
                }
            }
        }

        // now after reviewing all inventory items, let's check how many tokens we have
        for (Map.Entry<String, Integer> token : tokensNeeded.entrySet())
        {
            // bail if we don't have enough of a token type
            if (tokensFound.get(token.getKey()) < token.getValue())
            {
                return false;
            }
        }

        return true;
    }

    public void processItemPurchase(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id inventory = utils.getInventoryContainer(player);
        obj_id[] inventoryContents = getInventoryAndEquipment(player);
        int creditCost = getIntObjVar(self, "item.object_for_sale.cash_cost");
        int[] tokenCostForReals = getIntArrayObjVar(self, "item.object_for_sale.token_cost");
        String faction = getStringObjVar(self, vendor.OBJECT_FOR_SALE_FACTION);
        int factionCost = getIntObjVar(self, vendor.OBJECT_FOR_SALE_FACTION_COST);
        String badge = getStringObjVar(self, vendor.OBJECT_FOR_SALE_BADGE_NEEDED);
        String collection = getStringObjVar(self, vendor.OBJECT_FOR_SALE_COLLECTION_NEEDED);
        if (faction != null && factionCost > 0)
        {
            float currentStanding = factions.getFactionStanding(player, faction);
            if (currentStanding < 0)
            {
                broadcast(player, "Purchasing this item would make you go negative with that faction. Please try again later when you have the proper faction points.");
                return;
            }
            if (currentStanding > 4000)
            {
                factionCost *= 0.750;
                broadcast(player, "You have received a 25% discount due to your high reputation with the required faction.");
            }
            factions.setFactionStanding(player, faction, currentStanding - factionCost);
        }
        obj_id purchasedItem;
        String myName;
        if (static_item.isStaticItem(self))
        {
            myName = static_item.getStaticItemName(self);
            purchasedItem = static_item.createNewItemFunction(myName, inventory);
        }
        else
        {
            myName = getTemplateName(self);
            purchasedItem = createObjectOverloaded(myName, inventory);
        }
        if (hasScript(purchasedItem, "npc.faction_recruiter.biolink_item"))
        {
            setBioLink(purchasedItem, player);
        }
        LOG("ethereal", "[Vending]: " + getFirstName(player) + " purchased " + getNameNoSpam(self) + "(" + purchasedItem + ")");
        if (!exists(purchasedItem))
        {
            LOG("ethereal", "[Vending]: " + getFirstName(player) + " could not purchase " + getNameNoSpam(self) + "(" + purchasedItem + ")");
            sendSystemMessage(player, new string_id("set_bonus", "vendor_cant_purchase"));
            return;
        }
        String dummyItemName = getNameNoSpam(self);
        // tell the player what they bought for how many tokens
        broadcast(player, "You have purchased " + dummyItemName);
        if (creditCost >= 1)
        {
            obj_id containedBy = getContainedBy(getContainedBy(getContainedBy(self)));
            money.requestPayment(player, containedBy, creditCost, "no_handler", null, false);
        }
        boolean foundTokenHolderBox = false;
        for (obj_id inventoryContent : inventoryContents)
        {
            String itemName = getStaticItemName(inventoryContent);
            if (itemName != null && !itemName.isEmpty())
            {
                if (hasObjVar(self, VENDOR_TOKEN_TYPE))
                {
                    String tokenList = getStringObjVar(self, VENDOR_TOKEN_TYPE);
                    String[] differentTokens = split(tokenList, ',');
                    for (int j = 0; j < differentTokens.length; j++)
                    {
                        if (itemName.equals(differentTokens[j]) && tokenCostForReals[j] > 0)
                        {
                            if (getCount(inventoryContent) > 1)
                            {
                                int numInStack = getCount(inventoryContent);
                                for (int m = 0; m < numInStack - 1; m++)
                                {
                                    if (tokenCostForReals[j] > 0)
                                    {
                                        tokenCostForReals[j]--;
                                        setCount(inventoryContent, getCount(inventoryContent) - 1);
                                    }
                                }
                            }
                            if (getCount(inventoryContent) <= 1 && tokenCostForReals[j] > 0)
                            {
                                tokenCostForReals[j]--;
                            }
                        }
                    }
                }
                else
                {
                    for (int j = 0; j < trial.HEROIC_TOKENS.length; j++)
                    {
                        if (itemName.equals(trial.HEROIC_TOKENS[j]) && tokenCostForReals[j] > 0)
                        {
                            if (getCount(inventoryContent) > 1)
                            {
                                int numInStack = getCount(inventoryContent);
                                for (int m = 0; m < numInStack - 1; m++)
                                {
                                    if (tokenCostForReals[j] > 0)
                                    {
                                        tokenCostForReals[j]--;
                                        setCount(inventoryContent, getCount(inventoryContent) - 1);
                                    }
                                }
                            }
                            if (getCount(inventoryContent) <= 1 && tokenCostForReals[j] > 0)
                            {
                                destroyObject(inventoryContent);
                                tokenCostForReals[j]--;
                            }
                        }
                    }
                    if (!foundTokenHolderBox && itemName.equals("item_heroic_token_box_01_01"))
                    {
                        foundTokenHolderBox = true;
                        if (hasObjVar(inventoryContent, "item.set.tokens_held"))
                        {
                            int[] virtualTokens = getIntArrayObjVar(inventoryContent, "item.set.tokens_held");
                            for (int k = 0; k < trial.HEROIC_TOKENS.length; k++)
                            {
                                if (tokenCostForReals[k] > 0 && virtualTokens[k] > 0)
                                {
                                    int paymentCounter = tokenCostForReals[k];
                                    for (int l = 0; l < paymentCounter; l++)
                                    {
                                        if (virtualTokens[k] > 0)
                                        {
                                            virtualTokens[k]--;
                                            tokenCostForReals[k]--;
                                        }
                                    }
                                }
                            }
                            setObjVar(inventoryContent, "item.set.tokens_held", virtualTokens);
                        }
                    }
                }
            }
            playClientEffectObj(player, "sound/item_vend.snd", player, "");
        }
        LOG("ethereal", "[NPC Vendor]: Player " + getFirstName(player) + "(" + player + ") purchased item " + getNameNoSpam(self) + "(" + self + ") from a vendor");
    }

    public string_id parseNameToStringId(String itemName, obj_id item) throws InterruptedException
    {
        String[] parsedString = split(itemName, ':');
        string_id itemNameSID;
        if (static_item.isStaticItem(item))
        {
            itemNameSID = static_item.getStaticItemStringIdName(item);
        }
        else if (parsedString.length > 1)
        {
            String stfFile = parsedString[0];
            String reference = parsedString[1];
            itemNameSID = new string_id(stfFile, reference);
        }
        else
        {
            String stfFile = parsedString[0];
            itemNameSID = new string_id(stfFile, " ");
        }
        return itemNameSID;
    }

    public boolean confirmFactionPoints(obj_id player, String faction, float requiredPoints) throws InterruptedException
    {
        float playerFactionPoints = factions.getFactionStanding(player, faction);
        return playerFactionPoints >= requiredPoints;
    }
}