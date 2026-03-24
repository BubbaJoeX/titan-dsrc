package script.systems.crafting;

import script.dictionary;
import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

import script.library.metrics;
import script.library.sui;
import script.library.tailor_texture;
import script.library.utils;

import java.util.Vector;

public class tool_tailor_texture extends script.base_script
{
    private static final String VAR_ARMOR_LIST = "tool_tailor_texture.armors";
    private static final String VAR_PENDING_ARMOR = "tool_tailor_texture.pendingArmor";
    private static final String SKILL_TAILOR_NOVICE = "crafting_tailor_novice";

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player) || !utils.isNestedWithin(self, player))
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasSkill(player, SKILL_TAILOR_NOVICE))
        {
            if (!isGod(player)) // allow non-novices to use the tool if they are gods
            {
                return SCRIPT_CONTINUE;
            }
        }
        mi.addRootMenu(menu_info_types.SERVER_TAILOR_TEXTURE_SET_ARMOR, string_id.unlocalized("Armor: Set PNG URL"));
        mi.addRootMenu(menu_info_types.SERVER_TAILOR_TEXTURE_CLEAR_ARMOR, string_id.unlocalized("Armor: Clear PNG URL"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (!isIdValid(player) || !exists(player) || !utils.isNestedWithin(self, player))
        {
            return SCRIPT_CONTINUE;
        }
        if (!hasSkill(player, SKILL_TAILOR_NOVICE))
        {
            return SCRIPT_CONTINUE;
        }
        if (item == menu_info_types.SERVER_TAILOR_TEXTURE_SET_ARMOR)
        {
            beginSetTexture(self, player);
        }
        else if (item == menu_info_types.SERVER_TAILOR_TEXTURE_CLEAR_ARMOR)
        {
            beginClearTexture(self, player);
        }
        return SCRIPT_CONTINUE;
    }

    private void collectArmor(obj_id player, Vector armorIds, Vector labels, boolean tailorOnly) throws InterruptedException
    {
        obj_id[] equipped = metrics.getWornItems(player);
        if (equipped != null)
        {
            for (obj_id piece : equipped)
            {
                if (!tailor_texture.isArmorTextureCandidate(piece))
                {
                    continue;
                }
                if (tailorOnly && !tailor_texture.hasTailorPngTexture(piece))
                {
                    continue;
                }
                armorIds.add(piece);
                labels.add(getName(piece) + "  (worn)");
            }
        }
        obj_id[] inv = utils.getContents(utils.getInventoryContainer(player), true);
        if (inv != null)
        {
            for (obj_id piece : inv)
            {
                if (!tailor_texture.isArmorTextureCandidate(piece))
                {
                    continue;
                }
                if (tailorOnly && !tailor_texture.hasTailorPngTexture(piece))
                {
                    continue;
                }
                armorIds.add(piece);
                labels.add(getName(piece));
            }
        }
    }

    private void beginSetTexture(obj_id self, obj_id player) throws InterruptedException
    {
        Vector armorIds = new Vector();
        Vector labels = new Vector();
        collectArmor(player, armorIds, labels, false);
        if (armorIds.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("No armor found in your inventory or on your character."));
            return;
        }
        String[] labelArr = new String[labels.size()];
        labels.copyInto(labelArr);
        obj_id[] arr = new obj_id[armorIds.size()];
        for (int i = 0; i < armorIds.size(); i++)
        {
            arr[i] = (obj_id) armorIds.get(i);
        }
        utils.setScriptVar(self, VAR_ARMOR_LIST, arr);
        String prompt = "Choose a piece, then enter a direct link to a .png file (http or https). Other players see the same texture.";
        sui.listbox(self, player, prompt, sui.OK_CANCEL, "Tailor: armor PNG URL", labelArr, "handlePickArmorForSet", true, false);
    }

    public int handlePickArmorForSet(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        int row = sui.getListboxSelectedRow(params);
        obj_id player = sui.getPlayerId(params);
        obj_id[] armors = utils.getObjIdArrayScriptVar(self, VAR_ARMOR_LIST);
        utils.removeScriptVar(self, VAR_ARMOR_LIST);
        if (bp == sui.BP_CANCEL || row < 0 || armors == null || row >= armors.length)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id armor = armors[row];
        if (!isIdValid(armor) || !tailor_texture.isArmorTextureCandidate(armor))
        {
            return SCRIPT_CONTINUE;
        }
        utils.setScriptVar(player, VAR_PENDING_ARMOR, armor);
        String current = "";
        if (hasObjVar(armor, tailor_texture.OBJ_TEXTURE_URL))
        {
            current = getStringObjVar(armor, tailor_texture.OBJ_TEXTURE_URL);
        }
        String prompt = "Paste a direct PNG link (e.g. https://example.com/skin.png). Same download behavior as Magic Paintings.";
        sui.inputbox(self, player, prompt, "Armor PNG URL", "handleUrlInput", 1024, false, current);
        return SCRIPT_CONTINUE;
    }

    public int handleUrlInput(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int bp = sui.getIntButtonPressed(params);
        if (!utils.hasScriptVar(player, VAR_PENDING_ARMOR))
        {
            return SCRIPT_CONTINUE;
        }
        obj_id armor = utils.getObjIdScriptVar(player, VAR_PENDING_ARMOR);
        utils.removeScriptVar(player, VAR_PENDING_ARMOR);
        if (bp == sui.BP_CANCEL)
        {
            return SCRIPT_CONTINUE;
        }
        if (!isIdValid(armor) || !tailor_texture.isArmorTextureCandidate(armor))
        {
            return SCRIPT_CONTINUE;
        }
        String raw = sui.getInputBoxText(params);
        String url = tailor_texture.validatePngUrl(raw);
        if (url == null)
        {
            sendSystemMessage(player, string_id.unlocalized("That URL was not accepted. Use http:// or https:// and ensure the path ends in .png"));
            return SCRIPT_CONTINUE;
        }
        tailor_texture.applyTailorPngUrl(armor, url);
        sendSystemMessage(player, string_id.unlocalized("Custom armor PNG link saved on that piece. It is visible to other players."));
        return SCRIPT_CONTINUE;
    }

    private void beginClearTexture(obj_id self, obj_id player) throws InterruptedException
    {
        Vector armorIds = new Vector();
        Vector labels = new Vector();
        collectArmor(player, armorIds, labels, true);
        if (armorIds.isEmpty())
        {
            sendSystemMessage(player, string_id.unlocalized("No armor with a custom PNG link was found."));
            return;
        }
        String[] labelArr = new String[labels.size()];
        labels.copyInto(labelArr);
        obj_id[] arr = new obj_id[armorIds.size()];
        for (int i = 0; i < armorIds.size(); i++)
        {
            arr[i] = (obj_id) armorIds.get(i);
        }
        utils.setScriptVar(self, VAR_ARMOR_LIST, arr);
        sui.listbox(self, player, "Clear stored PNG link from which piece?", sui.OK_CANCEL, "Tailor: clear PNG", labelArr, "handlePickArmorForClear", true, false);
    }

    public int handlePickArmorForClear(obj_id self, dictionary params) throws InterruptedException
    {
        int bp = sui.getIntButtonPressed(params);
        int row = sui.getListboxSelectedRow(params);
        obj_id[] armors = utils.getObjIdArrayScriptVar(self, VAR_ARMOR_LIST);
        utils.removeScriptVar(self, VAR_ARMOR_LIST);
        if (bp == sui.BP_CANCEL || row < 0 || armors == null || row >= armors.length)
        {
            return SCRIPT_CONTINUE;
        }
        obj_id armor = armors[row];
        if (!isIdValid(armor))
        {
            return SCRIPT_CONTINUE;
        }
        tailor_texture.clearTailorPngUrl(armor);
        obj_id player = sui.getPlayerId(params);
        sendSystemMessage(player, string_id.unlocalized("Custom armor PNG link removed from that piece."));
        return SCRIPT_CONTINUE;
    }
}
