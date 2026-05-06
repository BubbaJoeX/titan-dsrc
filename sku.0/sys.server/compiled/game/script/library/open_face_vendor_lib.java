package script.library;

import script.obj_id;
import script.string_id;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Helpers for {@link script.conversation.open_face_vendor}: vendors whose stock are tangible props placed in
 * the world near the NPC. Stock is discovered at conversation time by scanning (default 64 m;
 * override with {@link #OV_SCAN_RANGE_M} on the NPC), requiring {@link #OV_PRICE} and a matching
 * {@link #OV_VENDOR_KEY} so multiple stalls do not steal each other's props.
 *
 * <h2>Designer setup</h2>
 * <ol>
 *   <li>Spawn the vendor NPC and attach script {@code conversation.open_face_vendor}.</li>
 *   <li>Set on the <b>NPC</b> {@link #OV_VENDOR_KEY} (string) — a unique id for this stall (e.g. {@code mos_eisley_curio_a}).</li>
 *   <li>Place sellable tangibles within range (default 64 m, or {@link #OV_SCAN_RANGE_M} on the NPC).</li>
 *   <li>On each prop set {@link #OV_VENDOR_KEY} to the <b>same</b> string as the NPC, and {@link #OV_PRICE} (credits).</li>
 *   <li>Optional: {@link #OV_CUSTOM_DESCRIPTION} on props; {@link #OV_GREETING} on NPC; {@link #OV_SCAN_RANGE_M} on NPC.</li>
 * </ol>
 */
public class open_face_vendor_lib extends script.base_script
{
    public open_face_vendor_lib()
    {
    }

    /**
     * Objvar on NPC and on each sellable prop: must match exactly so only this vendor's props appear (challenge key).
     */
    public static final String OV_VENDOR_KEY = "open_face_vendor.vendor_key";

    /** Optional objvar on NPC: overrides the opening greeting line. */
    public static final String OV_GREETING = "open_face_vendor.greeting";

    /** Objvar on each sellable object: credit price (required to appear in the list). */
    public static final String OV_PRICE = "open_face_vendor.price";

    /** Optional objvar on sellable object: overrides description text in the conversation. */
    public static final String OV_CUSTOM_DESCRIPTION = "open_face_vendor.custom_description";

    /**
     * Optional objvar on NPC: scan radius in meters (float). Defaults to {@link #DEFAULT_SCAN_RANGE_M}.
     */
    public static final String OV_SCAN_RANGE_M = "open_face_vendor.scan_range_m";

    /** Scriptvar on player during payment: pending display object for fulfillment. */
    public static final String SV_PENDING_STOCK = "open_face_vendor.pending_stock";

    public static final float DEFAULT_SCAN_RANGE_M = 64.0f;

    public static final int MAX_LISTED_ITEMS = 5;

    /** Non-null, non-empty trimmed vendor key on the NPC, or null if misconfigured. */
    public static String getVendorKey(obj_id npc) throws InterruptedException
    {
        if (!hasObjVar(npc, OV_VENDOR_KEY))
        {
            return null;
        }
        String k = getStringObjVar(npc, OV_VENDOR_KEY);
        if (k == null)
        {
            return null;
        }
        k = k.trim();
        return k.length() > 0 ? k : null;
    }

    public static boolean hasVendorKeyConfigured(obj_id npc) throws InterruptedException
    {
        return getVendorKey(npc) != null;
    }

    public static float getScanRangeM(obj_id npc) throws InterruptedException
    {
        if (hasObjVar(npc, OV_SCAN_RANGE_M))
        {
            float r = getFloatObjVar(npc, OV_SCAN_RANGE_M);
            if (r > 0.5f && r <= 512.0f)
            {
                return r;
            }
        }
        return DEFAULT_SCAN_RANGE_M;
    }

    public static int getPrice(obj_id shelfObject) throws InterruptedException
    {
        if (!hasObjVar(shelfObject, OV_PRICE))
        {
            return -1;
        }
        return getIntObjVar(shelfObject, OV_PRICE);
    }

    private static boolean propMatchesVendor(obj_id obj, String vendorKey) throws InterruptedException
    {
        if (!hasObjVar(obj, OV_VENDOR_KEY))
        {
            return false;
        }
        String ok = getStringObjVar(obj, OV_VENDOR_KEY);
        if (ok == null || vendorKey == null)
        {
            return false;
        }
        return vendorKey.equals(ok.trim());
    }

    /**
     * Finds priced props within range of the NPC whose vendor key matches the NPC's key.
     * Sorted by display name, capped at {@link #MAX_LISTED_ITEMS}.
     */
    public static obj_id[] getSellableStock(obj_id npc) throws InterruptedException
    {
        String vendorKey = getVendorKey(npc);
        if (vendorKey == null)
        {
            return new obj_id[0];
        }
        float rangeM = getScanRangeM(npc);
        obj_id[] nearby = getObjectsInRange(npc, rangeM);
        if (nearby == null || nearby.length < 1)
        {
            return new obj_id[0];
        }
        obj_id[] filtered = new obj_id[nearby.length];
        int n = 0;
        for (obj_id obj : nearby)
        {
            if (!isIdValid(obj) || !exists(obj))
            {
                continue;
            }
            if (obj == npc)
            {
                continue;
            }
            if (isPlayer(obj))
            {
                continue;
            }
            if (!propMatchesVendor(obj, vendorKey))
            {
                continue;
            }
            int price = getPrice(obj);
            if (price < 1)
            {
                continue;
            }
            filtered[n++] = obj;
        }
        if (n < 1)
        {
            return new obj_id[0];
        }
        obj_id[] result = new obj_id[n];
        System.arraycopy(filtered, 0, result, 0, n);
        Arrays.sort(result, new Comparator<obj_id>()
        {
            public int compare(obj_id a, obj_id b)
            {
                try
                {
                    String na = getDisplayNameSafe(a);
                    String nb = getDisplayNameSafe(b);
                    return na.compareToIgnoreCase(nb);
                }
                catch (InterruptedException e)
                {
                    return 0;
                }
            }
        });
        if (result.length > MAX_LISTED_ITEMS)
        {
            obj_id[] cap = new obj_id[MAX_LISTED_ITEMS];
            System.arraycopy(result, 0, cap, 0, MAX_LISTED_ITEMS);
            return cap;
        }
        return result;
    }

    public static String getDisplayNameSafe(obj_id obj) throws InterruptedException
    {
        String staticName = getStaticItemName(obj);
        if (staticName != null && staticName.length() > 0)
        {
            return getString(new string_id(static_item.STATIC_ITEM_NAME, staticName));
        }
        String name = getName(obj);
        if (name != null && name.length() > 0)
        {
            return name;
        }
        return getTemplateName(obj);
    }

    public static String getDescriptionSafe(obj_id obj) throws InterruptedException
    {
        if (hasObjVar(obj, OV_CUSTOM_DESCRIPTION))
        {
            return getStringObjVar(obj, OV_CUSTOM_DESCRIPTION);
        }
        String staticName = getStaticItemName(obj);
        if (staticName != null && staticName.length() > 0)
        {
            return getString(new string_id("static_item_d", staticName));
        }
        return "A closer look might tell you more once you buy it.";
    }

    public static String formatBrowseLine(obj_id obj) throws InterruptedException
    {
        String nm = getDisplayNameSafe(obj);
        int price = getPrice(obj);
        return nm + " — " + price + " cr";
    }

    public static String formatDetailMessage(obj_id obj) throws InterruptedException
    {
        String nm = getDisplayNameSafe(obj);
        String desc = getDescriptionSafe(obj);
        int price = getPrice(obj);
        return nm + "\\n\\n" + desc + "\\n\\nPrice: " + price + " credits.";
    }

    public static obj_id findStockObjectByIndex(obj_id npc, int index) throws InterruptedException
    {
        obj_id[] stock = getSellableStock(npc);
        if (stock == null || index < 0 || index >= stock.length)
        {
            return null;
        }
        return stock[index];
    }

    public static boolean grantPurchasedCopy(obj_id player, obj_id shelfDisplayObject) throws InterruptedException
    {
        obj_id inv = utils.getInventoryContainer(player);
        if (!isIdValid(inv))
        {
            return false;
        }
        String staticName = getStaticItemName(shelfDisplayObject);
        if (staticName != null && staticName.length() > 0)
        {
            obj_id created = static_item.createNewItemFunction(staticName, inv);
            return isIdValid(created);
        }
        String template = getTemplateName(shelfDisplayObject);
        if (template == null || template.length() < 1)
        {
            return false;
        }
        obj_id created = createObject(template, inv, "");
        return isIdValid(created);
    }

    public static void clearPendingPurchase(obj_id player) throws InterruptedException
    {
        if (utils.hasScriptVar(player, SV_PENDING_STOCK))
        {
            utils.removeScriptVar(player, SV_PENDING_STOCK);
        }
    }
}
