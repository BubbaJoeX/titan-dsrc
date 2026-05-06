package script.item.container;

import script.dictionary;
import script.library.open_face_vendor_lib;
import script.library.sui;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

/**
 * Designer helper container: radial {@code [OFV] Staging} opens submenus to type vendor key, price, and description in
 * SUI input boxes; values apply to <b>every object inside this container</b> and are mirrored onto the crate objvars
 * ({@link open_face_vendor_lib#OV_VENDOR_KEY}, {@link #OV_STAMP_PRICE}, {@link #OV_STAMP_CUSTOM_DESCRIPTION}) for
 * persistence. Move the crate with god client as needed.
 *
 * <p>Radial access: gods by default, or anyone if {@link #OV_PUBLIC_STAMP_MENU} is set on the crate.</p>
 */
public class open_face_vendor_staging extends script.base_script
{
    public open_face_vendor_staging()
    {
    }

    /** Optional int on staging crate; mirrored when setting price via menu. */
    public static final String OV_STAMP_PRICE = "open_face_vendor.stamp_price";

    /** Optional string on staging crate; mirrored when setting description via menu. */
    public static final String OV_STAMP_CUSTOM_DESCRIPTION = "open_face_vendor.stamp_custom_description";

    /** If set on the staging crate, non-gods may use the staging radial. */
    public static final String OV_PUBLIC_STAMP_MENU = "open_face_vendor.public_stamp_menu";

    private static final int MENU_ROOT_OFV = menu_info_types.SERVER_MENU37;
    private static final int MENU_OFV_SET_KEY = menu_info_types.SERVER_MENU38;
    private static final int MENU_OFV_SET_PRICE = menu_info_types.SERVER_MENU39;
    private static final int MENU_OFV_SET_DESC = menu_info_types.SERVER_MENU40;

    private static final String HANDLER_SET_KEY = "handleOfvStagingKeyInput";
    private static final String HANDLER_SET_PRICE = "handleOfvStagingPriceInput";
    private static final String HANDLER_SET_DESC = "handleOfvStagingDescInput";

    private boolean canUseStaging(obj_id player, obj_id crate) throws InterruptedException
    {
        return isGod(player) || hasObjVar(crate, OV_PUBLIC_STAMP_MENU);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!canUseStaging(player, self))
        {
            return SCRIPT_CONTINUE;
        }

        int root = mi.addRootMenu(MENU_ROOT_OFV, string_id.unlocalized("[OFV] Staging"));
        mi.addSubMenu(root, MENU_OFV_SET_KEY, string_id.unlocalized("Set vendor key (all contents)"));
        mi.addSubMenu(root, MENU_OFV_SET_PRICE, string_id.unlocalized("Set price / credits (all contents)"));
        mi.addSubMenu(root, MENU_OFV_SET_DESC, string_id.unlocalized("Set custom description (all contents)"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!canUseStaging(player, self))
        {
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_OFV_SET_KEY)
        {
            String def = "";
            String vk = open_face_vendor_lib.getVendorKey(self);
            if (vk != null)
            {
                def = vk;
            }
            sui.inputbox(self, player, "Vendor challenge key (must match your NPC). Applies to every item in this container.", sui.OK_CANCEL, "[OFV] Vendor key", sui.INPUT_NORMAL, new String[]{def}, HANDLER_SET_KEY, null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_OFV_SET_PRICE)
        {
            String def = "";
            if (hasObjVar(self, OV_STAMP_PRICE))
            {
                def = Integer.toString(getIntObjVar(self, OV_STAMP_PRICE));
            }
            sui.inputbox(self, player, "Credit price for vendor listing (≥ 1). Leave empty or 0 to remove price from contents.", sui.OK_CANCEL, "[OFV] Price", sui.INPUT_NORMAL, new String[]{def}, HANDLER_SET_PRICE, null);
            return SCRIPT_CONTINUE;
        }

        if (item == MENU_OFV_SET_DESC)
        {
            String def = "";
            if (hasObjVar(self, OV_STAMP_CUSTOM_DESCRIPTION))
            {
                def = getStringObjVar(self, OV_STAMP_CUSTOM_DESCRIPTION);
            }
            sui.inputbox(self, player, "Optional inspect text for conversation detail. Leave empty to remove custom description from contents.", sui.OK_CANCEL, "[OFV] Description", sui.INPUT_NORMAL, new String[]{def}, HANDLER_SET_DESC, null);
            return SCRIPT_CONTINUE;
        }

        return SCRIPT_CONTINUE;
    }

    public int handleOfvStagingKeyInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!canUseStaging(player, self))
        {
            return SCRIPT_CONTINUE;
        }
        String raw = sui.getInputBoxText(params);
        if (raw == null)
        {
            return SCRIPT_CONTINUE;
        }
        String key = raw.trim();
        if (key.length() < 1)
        {
            sendSystemMessage(player, "[OFV] Vendor key cannot be empty.", null);
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, open_face_vendor_lib.OV_VENDOR_KEY, key);
        obj_id[] contents = getContents(self);
        int n = 0;
        if (contents != null)
        {
            for (obj_id obj : contents)
            {
                if (!isIdValid(obj) || !exists(obj))
                {
                    continue;
                }
                setObjVar(obj, open_face_vendor_lib.OV_VENDOR_KEY, key);
                ++n;
            }
        }
        sendSystemMessage(player, "[OFV] Vendor key set on crate and " + n + " object(s) inside.", null);
        return SCRIPT_CONTINUE;
    }

    public int handleOfvStagingPriceInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!canUseStaging(player, self))
        {
            return SCRIPT_CONTINUE;
        }
        String raw = sui.getInputBoxText(params);
        if (raw == null)
        {
            raw = "";
        }
        raw = raw.trim();
        if (raw.length() < 1 || raw.equals("0"))
        {
            removeObjVar(self, OV_STAMP_PRICE);
            obj_id[] contents = getContents(self);
            int n = 0;
            if (contents != null)
            {
                for (obj_id obj : contents)
                {
                    if (!isIdValid(obj) || !exists(obj))
                    {
                        continue;
                    }
                    if (hasObjVar(obj, open_face_vendor_lib.OV_PRICE))
                    {
                        removeObjVar(obj, open_face_vendor_lib.OV_PRICE);
                    }
                    ++n;
                }
            }
            sendSystemMessage(player, "[OFV] Removed price from crate and " + n + " object(s) inside.", null);
            return SCRIPT_CONTINUE;
        }

        int price;
        try
        {
            price = Integer.parseInt(raw);
        }
        catch (NumberFormatException e)
        {
            sendSystemMessage(player, "[OFV] Invalid price — enter a whole number (credits).", null);
            return SCRIPT_CONTINUE;
        }
        if (price < 1)
        {
            sendSystemMessage(player, "[OFV] Price must be at least 1 credit, or leave empty to clear.", null);
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, OV_STAMP_PRICE, price);
        obj_id[] contents = getContents(self);
        int n = 0;
        if (contents != null)
        {
            for (obj_id obj : contents)
            {
                if (!isIdValid(obj) || !exists(obj))
                {
                    continue;
                }
                setObjVar(obj, open_face_vendor_lib.OV_PRICE, price);
                ++n;
            }
        }
        sendSystemMessage(player, "[OFV] Price " + price + " set on crate and " + n + " object(s) inside.", null);
        return SCRIPT_CONTINUE;
    }

    public int handleOfvStagingDescInput(obj_id self, dictionary params) throws InterruptedException
    {
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (!canUseStaging(player, self))
        {
            return SCRIPT_CONTINUE;
        }
        String raw = sui.getInputBoxText(params);
        if (raw == null)
        {
            raw = "";
        }
        String desc = raw.trim();

        if (desc.length() < 1)
        {
            removeObjVar(self, OV_STAMP_CUSTOM_DESCRIPTION);
            obj_id[] contents = getContents(self);
            int n = 0;
            if (contents != null)
            {
                for (obj_id obj : contents)
                {
                    if (!isIdValid(obj) || !exists(obj))
                    {
                        continue;
                    }
                    if (hasObjVar(obj, open_face_vendor_lib.OV_CUSTOM_DESCRIPTION))
                    {
                        removeObjVar(obj, open_face_vendor_lib.OV_CUSTOM_DESCRIPTION);
                    }
                    ++n;
                }
            }
            sendSystemMessage(player, "[OFV] Cleared custom description on crate and " + n + " object(s) inside.", null);
            return SCRIPT_CONTINUE;
        }

        setObjVar(self, OV_STAMP_CUSTOM_DESCRIPTION, desc);
        obj_id[] contents = getContents(self);
        int n = 0;
        if (contents != null)
        {
            for (obj_id obj : contents)
            {
                if (!isIdValid(obj) || !exists(obj))
                {
                    continue;
                }
                setObjVar(obj, open_face_vendor_lib.OV_CUSTOM_DESCRIPTION, desc);
                ++n;
            }
        }
        sendSystemMessage(player, "[OFV] Custom description set on crate and " + n + " object(s) inside.", null);
        return SCRIPT_CONTINUE;
    }
}
