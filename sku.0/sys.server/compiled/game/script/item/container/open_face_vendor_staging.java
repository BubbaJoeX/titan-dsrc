package script.item.container;

import script.library.open_face_vendor_lib;
import script.menu_info;
import script.menu_info_data;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

/**
 * Designer-only helper container: drop tangibles inside, set stamp objvars on this crate, then use the radial
 * (god mode by default) to copy {@link open_face_vendor_lib#OV_VENDOR_KEY} / optional price / optional
 * description onto every contained object. Pull props back out and place them in-world near your vendor.
 *
 * <p><b>Spawn:</b> create any normal tangible container (e.g. {@code object/tangible/container/loot/large_container.iff}),
 * then attach this script ({@code item.container.open_face_vendor_staging}). Move the crate with god client as needed.</p>
 *
 * <p><b>Objvars on this crate (staging)</b></p>
 * <ul>
 *   <li>{@link open_face_vendor_lib#OV_VENDOR_KEY} — required for stamping (same value you will use on the NPC).</li>
 *   <li>{@link #OV_STAMP_PRICE} — optional; if set and ≥ 1, written to each item as {@link open_face_vendor_lib#OV_PRICE}.</li>
 *   <li>{@link #OV_STAMP_CUSTOM_DESCRIPTION} — optional; if non-empty, sets {@link open_face_vendor_lib#OV_CUSTOM_DESCRIPTION} on each item.</li>
 * </ul>
 *
 * <p>Radial “Stamp…” appears for gods, or for anyone if {@link #OV_PUBLIC_STAMP_MENU} is set on this object.</p>
 */
public class open_face_vendor_staging extends script.base_script
{
    public open_face_vendor_staging()
    {
    }

    /** Optional int on staging crate; applied to each stamped item as {@link open_face_vendor_lib#OV_PRICE}. */
    public static final String OV_STAMP_PRICE = "open_face_vendor.stamp_price";

    /** Optional string; if set, copied to each item as {@link open_face_vendor_lib#OV_CUSTOM_DESCRIPTION}. */
    public static final String OV_STAMP_CUSTOM_DESCRIPTION = "open_face_vendor.stamp_custom_description";

    /** If set on the staging crate, non-gods may see/use the Stamp radial (for internal builder accounts). */
    public static final String OV_PUBLIC_STAMP_MENU = "open_face_vendor.public_stamp_menu";

    private static final int RADIAL_STAMP = menu_info_types.SERVER_MENU37;

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        boolean allow = isGod(player);
        if (!allow && hasObjVar(self, OV_PUBLIC_STAMP_MENU))
        {
            allow = true;
        }
        if (!allow)
        {
            return SCRIPT_CONTINUE;
        }

        int stampRoot = mi.addRootMenu(RADIAL_STAMP, string_id.unlocalized("[OFV] Stamp key / price onto contents"));
        menu_info_data stampData = mi.getMenuItemById(stampRoot);
        if (stampData != null)
        {
            stampData.setServerNotify(true);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != RADIAL_STAMP)
        {
            return SCRIPT_CONTINUE;
        }
        boolean allow = isGod(player);
        if (!allow && hasObjVar(self, OV_PUBLIC_STAMP_MENU))
        {
            allow = true;
        }
        if (!allow)
        {
            return SCRIPT_CONTINUE;
        }

        String vendorKey = open_face_vendor_lib.getVendorKey(self);
        if (vendorKey == null)
        {
            sendSystemMessage(player, "Set string objvar open_face_vendor.vendor_key on this staging crate first.", null);
            return SCRIPT_CONTINUE;
        }

        obj_id[] contents = getContents(self);
        if (contents == null || contents.length < 1)
        {
            sendSystemMessage(player, "Container is empty.", null);
            return SCRIPT_CONTINUE;
        }

        boolean setPrice = hasObjVar(self, OV_STAMP_PRICE);
        int price = setPrice ? getIntObjVar(self, OV_STAMP_PRICE) : 0;

        boolean setDesc = hasObjVar(self, OV_STAMP_CUSTOM_DESCRIPTION);
        String desc = setDesc ? getStringObjVar(self, OV_STAMP_CUSTOM_DESCRIPTION) : null;
        if (desc != null && desc.trim().length() < 1)
        {
            desc = null;
            setDesc = false;
        }

        int count = 0;
        for (obj_id obj : contents)
        {
            if (!isIdValid(obj) || !exists(obj))
            {
                continue;
            }
            setObjVar(obj, open_face_vendor_lib.OV_VENDOR_KEY, vendorKey);
            if (setPrice && price >= 1)
            {
                setObjVar(obj, open_face_vendor_lib.OV_PRICE, price);
            }
            if (setDesc && desc != null)
            {
                setObjVar(obj, open_face_vendor_lib.OV_CUSTOM_DESCRIPTION, desc);
            }
            ++count;
        }

        sendSystemMessage(player, "[OFV] Stamped " + count + " object(s). vendor_key copied from this crate; price/description applied only if stamp_* objvars were set.", null);
        return SCRIPT_CONTINUE;
    }
}
