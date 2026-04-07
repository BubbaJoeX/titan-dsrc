package script.content;/*
@Origin: dsrc.script.content.mannequin
@Purpose: Invulnerable display mannequin with radial controls for name, raw/logical animation, loop, clear, bind, body-only hologram toggle, wearable add/remove via SUI listbox, and template .cdf suppression for clients.
@Notes: Uses SERVER_MENU44/45/46 under SERVER_MENU52. Hologram uses HOLOGRAM_TYPE1_QUALITY1 with setHologramAffectsWearables(false) so only the base creature mesh is holographic on clients. Remove list merges appearance inventory contents with getAllWornItems; removal uses putInOverloaded.
*/

import java.util.Vector;

import script.*;
import script.library.sui;
import script.library.utils;

public class mannequin extends base_script
{
    public static final String OV_INITIALIZED = "content.mannequin.initialized";
    public static final String OV_ANIM = "content.mannequin.animSpec";
    public static final String OV_LOOP = "content.mannequin.animLoop";

    /** @see menu_info_types — reused server menu slots (no mannequin-specific types). */
    private static final int M_ROOT = menu_info_types.SERVER_MENU52;
    private static final int M_SET_NAME = menu_info_types.SERVER_MENU47;
    private static final int M_SET_ANIM = menu_info_types.SERVER_MENU48;
    private static final int M_TOGGLE_LOOP = menu_info_types.SERVER_MENU49;
    private static final int M_APPLY = menu_info_types.SERVER_MENU50;
    private static final int M_CLEAR = menu_info_types.SERVER_MENU51;
    private static final int M_BIND = menu_info_types.SERVER_MENU53;
    private static final int M_ADD_WEARABLE = menu_info_types.SERVER_MENU44;
    private static final int M_RM_WEARABLE = menu_info_types.SERVER_MENU45;
    private static final int M_TOGGLE_HOLO = menu_info_types.SERVER_MENU46;

    private static final String SV_ROW_IDS = "content_mannequin.listboxObjIds";
    /** 1 = body-only hologram active (persisted for re-init). */
    public static final String OV_HOLO_BODY = "content.mannequin.holoBody";

    public int OnAttach(obj_id self)
    {
        syncBasics(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        syncBasics(self);
        try
        {
            applyStoredAnimation(self);
        }
        catch (InterruptedException ignored)
        {
        }
        return SCRIPT_CONTINUE;
    }

    private void syncBasics(obj_id self)
    {
        setInvulnerable(self, true);
        if (!hasObjVar(self, OV_INITIALIZED))
        {
            setObjVar(self, OV_INITIALIZED, 1);
            setName(self, "Mannequin");
        }
        setSuppressTemplateClientDataFile(self, true);
        ensureAppearanceInventory(self);
        if (hasObjVar(self, OV_HOLO_BODY) && getIntObjVar(self, OV_HOLO_BODY) != 0)
        {
            setHologramType(self, HOLOGRAM_TYPE1_QUALITY1);
            setHologramAffectsWearables(self, false);
        }
    }

    public int OnGetAttributes(obj_id self, obj_id player, String[] names, String[] attribs) throws InterruptedException
    {
        int idx = utils.getValidAttributeIndex(names);
        if (idx == -1)
        {
            return SCRIPT_CONTINUE;
        }
        names[idx] = utils.packStringId(new string_id("Animation"));
        attribs[idx] = hasObjVar(self, OV_ANIM) ? getStringObjVar(self, OV_ANIM) : "(none)";
        idx++;
        if (idx >= names.length || idx >= attribs.length)
        {
            return SCRIPT_CONTINUE;
        }
        names[idx] = utils.packStringId(new string_id("Loop .ans"));
        attribs[idx] = (hasObjVar(self, OV_LOOP) && getIntObjVar(self, OV_LOOP) != 0) ? "On" : "Off";
        idx++;
        if (idx >= names.length || idx >= attribs.length)
        {
            return SCRIPT_CONTINUE;
        }
        names[idx] = utils.packStringId(new string_id("Hologram"));
        attribs[idx] = (getHologramType(self) != HOLOGRAM_NONE) ? "On (body only)" : "Off";
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isGod(player) || !canManipulate(player, self, true, true, 32.0f, false))
        {
            return SCRIPT_CONTINUE;
        }
        int root = mi.addRootMenu(M_ROOT, string_id.unlocalized("Mannequin"));
        mi.addSubMenu(root, M_SET_NAME, string_id.unlocalized("Set Name"));
        mi.addSubMenu(root, M_SET_ANIM, string_id.unlocalized("Set Animation"));
        mi.addSubMenu(root, M_TOGGLE_LOOP, string_id.unlocalized("Toggle Animation Loop (.ans)"));
        mi.addSubMenu(root, M_APPLY, string_id.unlocalized("Apply Animation"));
        mi.addSubMenu(root, M_CLEAR, string_id.unlocalized("Clear Animation"));
        mi.addSubMenu(root, M_BIND, string_id.unlocalized("Freeze Animation"));
        mi.addSubMenu(root, M_ADD_WEARABLE, string_id.unlocalized("Add Wearable..."));
        mi.addSubMenu(root, M_RM_WEARABLE, string_id.unlocalized("Remove Wearable..."));
        boolean holoOn = getHologramType(self) != HOLOGRAM_NONE;
        mi.addSubMenu(root, M_TOGGLE_HOLO, string_id.unlocalized(holoOn ? "Hologram: Off" : "Hologram: On (body only)"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isGod(player))
        {
            return SCRIPT_CONTINUE;
        }
        if (item == M_SET_NAME)
        {
            sui.inputbox(self, player, "Display name for this mannequin.", "Mannequin Name", "handleSetName", getName(self));
            return SCRIPT_CONTINUE;
        }
        if (item == M_SET_ANIM)
        {
            String cur = hasObjVar(self, OV_ANIM) ? getStringObjVar(self, OV_ANIM) : "";
            String prompt = "Logical .ash action, or .ans path (relative to appearance/animation/ or full appearance/...), or ans:/ansl: prefix. Freeze: ans:path#keyframeIndex";
            sui.inputbox(self, player, prompt, "Mannequin Animation", "handleSetAnim", cur);
            return SCRIPT_CONTINUE;
        }
        if (item == M_TOGGLE_LOOP)
        {
            int loop = (hasObjVar(self, OV_LOOP) && getIntObjVar(self, OV_LOOP) != 0) ? 0 : 1;
            setObjVar(self, OV_LOOP, loop);
            broadcast(player, "Mannequin .ans loop is now " + (loop != 0 ? "ON" : "OFF") + ". Use Apply if a clip is already playing.");
            return SCRIPT_CONTINUE;
        }
        if (item == M_APPLY)
        {
            applyStoredAnimation(self);
            broadcast(player, "Applied animation to mannequin (see stored spec in examine).");
            return SCRIPT_CONTINUE;
        }
        if (item == M_CLEAR)
        {
            setAuthoritativeClientAnimationAction(self, "anim:clear");
            doAnimationAction(self, "anim:clear");
            broadcast(player, "Sent clear to clients (stops raw .ans loop track).");
            return SCRIPT_CONTINUE;
        }
        if (item == M_BIND)
        {
            setAuthoritativeClientAnimationAction(self, "anim:bind");
            doAnimationAction(self, "anim:bind");
            broadcast(player, "Sent bind reset to clients (stop loop/action/add tracks).");
            return SCRIPT_CONTINUE;
        }
        if (item == M_ADD_WEARABLE)
        {
            showEquipListbox(self, player);
            return SCRIPT_CONTINUE;
        }
        if (item == M_RM_WEARABLE)
        {
            showRemoveListbox(self, player);
            return SCRIPT_CONTINUE;
        }
        if (item == M_TOGGLE_HOLO)
        {
            toggleHologramBodyOnly(self, player);
            return SCRIPT_CONTINUE;
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSetName(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        String name = sui.getInputBoxText(params);
        if (name != null && !name.trim().isEmpty())
        {
            setName(self, name.trim());
            broadcast(player, "Mannequin name updated.");
        }
        return SCRIPT_CONTINUE;
    }

    public int handleSetAnim(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        String spec = sui.getInputBoxText(params);
        if (spec == null || spec.trim().isEmpty())
        {
            removeObjVar(self, OV_ANIM);
            broadcast(player, "Cleared stored animation spec.");
            return SCRIPT_CONTINUE;
        }
        setObjVar(self, OV_ANIM, spec.trim());
        broadcast(player, "Stored animation spec. Use Apply to play.");
        return SCRIPT_CONTINUE;
    }

    public int handleEquipPick(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            utils.removeScriptVar(player, SV_ROW_IDS);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        obj_id[] ids = utils.getObjIdArrayScriptVar(player, SV_ROW_IDS);
        utils.removeScriptVar(player, SV_ROW_IDS);
        if (row < 0)
        {
            broadcast(player, "Select a row, then OK.");
            return SCRIPT_CONTINUE;
        }
        if (!isGod(player) || !canManipulate(player, self, true, true, 32.0f, false))
        {
            return SCRIPT_CONTINUE;
        }
        if (ids == null || row >= ids.length || !isIdValid(ids[row]))
        {
            broadcast(player, "That choice is no longer valid; open the list again.");
            return SCRIPT_CONTINUE;
        }
        obj_id item = ids[row];
        if (!isDressableTangible(item) || getContainedBy(item) != utils.getInventoryContainer(player))
        {
            broadcast(player, "Item must still be in your main inventory.");
            return SCRIPT_CONTINUE;
        }
        equipFromPlayerInventory(self, player, item);
        return SCRIPT_CONTINUE;
    }

    public int handleRemovePick(obj_id self, dictionary params) throws InterruptedException
    {
        if (params == null || params.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }
        obj_id player = sui.getPlayerId(params);
        if (sui.getIntButtonPressed(params) == sui.BP_CANCEL)
        {
            utils.removeScriptVar(player, SV_ROW_IDS);
            return SCRIPT_CONTINUE;
        }
        int row = sui.getListboxSelectedRow(params);
        obj_id[] ids = utils.getObjIdArrayScriptVar(player, SV_ROW_IDS);
        utils.removeScriptVar(player, SV_ROW_IDS);
        if (row < 0)
        {
            broadcast(player, "Select a row, then OK.");
            return SCRIPT_CONTINUE;
        }
        if (!isGod(player) || !canManipulate(player, self, true, true, 32.0f, false))
        {
            return SCRIPT_CONTINUE;
        }
        if (ids == null || row >= ids.length || !isIdValid(ids[row]))
        {
            broadcast(player, "That choice is no longer valid; open the list again.");
            return SCRIPT_CONTINUE;
        }
        obj_id item = ids[row];
        obj_id appInv = getAppearanceInventory(self);
        if (!isIdValid(appInv) || !itemIsEquippedOrInAppearanceInv(self, item, appInv))
        {
            broadcast(player, "That item is no longer equipped or in the mannequin appearance inventory.");
            return SCRIPT_CONTINUE;
        }
        obj_id pInv = utils.getInventoryContainer(player);
        if (!isIdValid(pInv))
        {
            broadcast(player, "Could not resolve your inventory.");
            return SCRIPT_CONTINUE;
        }
        boolean moved = putInOverloaded(item, pInv);
        if (!moved)
        {
            moved = putIn(item, pInv, player);
        }
        if (moved)
        {
            broadcast(player, "Removed item from mannequin to your inventory.");
        }
        else
        {
            broadcast(player, "Could not move that item to your inventory.");
        }
        return SCRIPT_CONTINUE;
    }

    /** Ensures server created appearance_inventory in the appearance slot; fails if the template has no appearance slot. */
    private boolean requireAppearanceInventory(obj_id self, obj_id player) throws InterruptedException
    {
        ensureAppearanceInventory(self);
        if (isIdValid(getAppearanceInventory(self)))
        {
            return true;
        }
        broadcast(player, "This creature template has no appearance inventory slot. Use a humanoid-style creature (slotted + appearance slot), or add that slot to the template.");
        return false;
    }

    private void showEquipListbox(obj_id self, obj_id player) throws InterruptedException
    {
        if (!requireAppearanceInventory(self, player))
        {
            return;
        }
        utils.removeScriptVar(player, SV_ROW_IDS);
        obj_id pInv = utils.getInventoryContainer(player);
        if (!isIdValid(pInv))
        {
            broadcast(player, "No player inventory.");
            return;
        }
        obj_id[] contents = getContents(pInv);
        Vector labelVec = new Vector();
        Vector idVec = new Vector();
        if (contents != null)
        {
            for (obj_id item : contents)
            {
                if (isDressableTangible(item))
                {
                    idVec.add(item);
                    String nm = getName(item);
                    String st = getStaticItemName(item);
                    labelVec.add((nm != null ? nm : "?") + " — " + (st != null ? st : getTemplateName(item)));
                }
            }
        }
        if (idVec.size() == 0)
        {
            broadcast(player, "No equippable armor/clothing/jewelry/weapon/cybernetic in your main inventory.");
            return;
        }
        obj_id[] ids = new obj_id[idVec.size()];
        for (int i = 0; i < idVec.size(); ++i)
        {
            ids[i] = (obj_id)idVec.get(i);
        }
        utils.setScriptVar(player, SV_ROW_IDS, ids);
        String[] rows = new String[labelVec.size()];
        for (int i = 0; i < labelVec.size(); ++i)
        {
            rows[i] = (String)labelVec.get(i);
        }
        sui.listbox(self, player, "Choose an item from your inventory to equip on this mannequin.", sui.OK_CANCEL, "Mannequin: add wearable", rows, "handleEquipPick", true, false);
    }

    private void showRemoveListbox(obj_id self, obj_id player) throws InterruptedException
    {
        if (!requireAppearanceInventory(self, player))
        {
            return;
        }
        utils.removeScriptVar(player, SV_ROW_IDS);
        obj_id appInv = getAppearanceInventory(self);
        if (!isIdValid(appInv))
        {
            broadcast(player, "Mannequin has no appearance inventory.");
            return;
        }
        Vector labelVec = new Vector();
        Vector idVec = new Vector();
        obj_id[] inInv = getContents(appInv);
        if (inInv != null)
        {
            for (obj_id item : inInv)
            {
                addUniqueItemRow(idVec, labelVec, item);
            }
        }
        obj_id[] equipped = getAllWornItems(self, false);
        if (equipped != null)
        {
            for (obj_id item : equipped)
            {
                addUniqueItemRow(idVec, labelVec, item);
            }
        }
        if (idVec.size() == 0)
        {
            broadcast(player, "Nothing equipped or in the mannequin appearance inventory to remove.");
            return;
        }
        obj_id[] ids = new obj_id[idVec.size()];
        for (int i = 0; i < idVec.size(); ++i)
        {
            ids[i] = (obj_id)idVec.get(i);
        }
        utils.setScriptVar(player, SV_ROW_IDS, ids);
        String[] rows = new String[labelVec.size()];
        for (int i = 0; i < labelVec.size(); ++i)
        {
            rows[i] = (String)labelVec.get(i);
        }
        sui.listbox(self, player, "Choose a worn piece to remove to your inventory.", sui.OK_CANCEL, "Mannequin: remove wearable", rows, "handleRemovePick", true, false);
    }

    private static boolean isDressableTangible(obj_id item)
    {
        if (!isIdValid(item) || !exists(item) || !isTangible(item))
        {
            return false;
        }
        int got = getGameObjectType(item);
        return isGameObjectTypeOf(got, GOT_armor) || isGameObjectTypeOf(got, GOT_clothing) || isGameObjectTypeOf(got, GOT_weapon) || isGameObjectTypeOf(got, GOT_jewelry) || isGameObjectTypeOf(got, GOT_cybernetic);
    }

    private void equipFromPlayerInventory(obj_id self, obj_id player, obj_id item) throws InterruptedException
    {
        if (!requireAppearanceInventory(self, player))
        {
            return;
        }
        obj_id appInv = getAppearanceInventory(self);
        if (!isIdValid(appInv))
        {
            broadcast(player, "Mannequin has no appearance inventory (wrong creature template?).");
            return;
        }
        if (!putIn(item, appInv, player))
        {
            broadcast(player, "Could not move the item into the mannequin appearance inventory.");
            return;
        }
        int got = getGameObjectType(item);
        boolean ok = false;
        if (isGameObjectTypeOf(got, GOT_weapon))
        {
            ok = equip(item, self, "hold_r") || equip(item, self, "hold_l");
        }
        if (!ok)
        {
            ok = equip(item, self);
        }
        if (!ok)
        {
            broadcast(player, "Item is in the appearance container but equip failed (slots/species may not fit this mannequin).");
            return;
        }
        broadcast(player, "Equipped item on mannequin.");
    }

    private void toggleHologramBodyOnly(obj_id self, obj_id player) throws InterruptedException
    {
        boolean on = hasObjVar(self, OV_HOLO_BODY) && getIntObjVar(self, OV_HOLO_BODY) != 0;
        if (on)
        {
            removeObjVar(self, OV_HOLO_BODY);
            setHologramType(self, HOLOGRAM_NONE);
            setHologramAffectsWearables(self, true);
            broadcast(player, "Mannequin hologram off.");
        }
        else
        {
            setObjVar(self, OV_HOLO_BODY, 1);
            setHologramType(self, HOLOGRAM_TYPE1_QUALITY1);
            setHologramAffectsWearables(self, false);
            broadcast(player, "Mannequin body hologram on (wearables stay non-holographic on clients).");
        }
    }

    private static void addUniqueItemRow(Vector idVec, Vector labelVec, obj_id item) throws InterruptedException
    {
        if (!isIdValid(item) || !exists(item))
        {
            return;
        }
        for (int i = 0; i < idVec.size(); ++i)
        {
            if (((obj_id)idVec.get(i)) == item)
            {
                return;
            }
        }
        idVec.add(item);
        String nm = getName(item);
        String st = getStaticItemName(item);
        labelVec.add((nm != null ? nm : "?") + " — " + (st != null ? st : getTemplateName(item)));
    }

    private static boolean itemIsEquippedOrInAppearanceInv(obj_id self, obj_id item, obj_id appInv) throws InterruptedException
    {
        if (!isIdValid(item))
        {
            return false;
        }
        if (isIdValid(appInv) && getContainedBy(item) == appInv)
        {
            return true;
        }
        obj_id[] worn = getAllWornItems(self, false);
        if (worn != null)
        {
            for (obj_id w : worn)
            {
                if (w == item)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void applyStoredAnimation(obj_id self) throws InterruptedException
    {
        if (!hasObjVar(self, OV_ANIM))
        {
            return;
        }
        String spec = getStringObjVar(self, OV_ANIM);
        if (spec == null)
        {
            return;
        }
        spec = spec.trim();
        if (spec.isEmpty())
        {
            return;
        }
        boolean loop = hasObjVar(self, OV_LOOP) && getIntObjVar(self, OV_LOOP) != 0;
        String lower = spec.toLowerCase();

        String clientSpec;
        if (lower.startsWith("ans:") || lower.startsWith("ansl:"))
        {
            clientSpec = spec;
        }
        else if (lower.endsWith(".ans") || lower.startsWith("appearance/"))
        {
            clientSpec = (loop ? "ansl:" : "ans:") + spec;
        }
        else
        {
            clientSpec = spec;
        }
        setAuthoritativeClientAnimationAction(self, clientSpec);
        doAnimationAction(self, clientSpec);
    }
}
