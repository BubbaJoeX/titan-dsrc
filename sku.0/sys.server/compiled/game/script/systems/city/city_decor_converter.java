package script.systems.city;/*
@Origin: script.systems.city.city_decor_converter
@Author: BubbaJoeX
@Purpose: Converts storyteller props and tangible items into city decorations. Also allows you to create city actor deeds if you are a GM.
@Copyright © SWG-OR 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.create;
import script.library.gm;
import script.library.static_item;
import script.library.utils;

import static script.library.storyteller.STORYTELLER_DATATABLE;

public class city_decor_converter extends base_script
{
    public city_decor_converter()
    {
    }

    //@Converts a story teller object to a city decor object
    public static void createPropObject(obj_id self, obj_id token) throws InterruptedException
    {
        obj_id prop = null;
        String itemName = getStaticItemName(token);
        int row = dataTableSearchColumnForString(itemName, "name", STORYTELLER_DATATABLE);
        dictionary dict = dataTableGetRow(STORYTELLER_DATATABLE, itemName);
        String template = dict.getString("template_name");
        prop = create.createObject(template, self, "");
        if (isIdValid(prop))
        {
            setName(prop, " ");
            obj_id mainInv = utils.getTopMostContainer(self);
            putIn(prop, mainInv);
            destroyObject(token);
        }
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setName(self, "City Decor Converter");
        attachScript(self, "item.special.nomove");
        return SCRIPT_CONTINUE;
    }

    public int OnInitalize(obj_id self) throws InterruptedException
    {
        setName(self, "City Decor Converter");
        attachScript(self, "item.special.nomove");
        return SCRIPT_CONTINUE;
    }

    public int OnAboutToReceiveItem(obj_id self, obj_id destContainer, obj_id transferer, obj_id item) throws InterruptedException
    {
        if (isIdValid(item))
        {
            if (hasObjVar(item, "noTrade") || (hasScript(item, "item.special.nomove")))
            {
                broadcast(transferer, "You cannot decorate your city with this item.");
                return SCRIPT_OVERRIDE;
            }
            if (getTemplateName(item).equals("object/tangible/storyteller/story_token_prop.iff"))
            {
                createPropObject(self, item);
                LOG("ethereal", "[City Decor Converter]: " + transferer + " converted storyteller prop token " + item + " into a city decoration.");
                return SCRIPT_CONTINUE;
            }
            if (getTemplateName(item).equals("object/tangible/storyteller/story_token_flavor_npc.iff"))
            {
                //createActorProp(self, item, transferer);
                broadcast(transferer, "This feature is not yet implemented.");
                LOG("ethereal", "[City Decor Converter]: " + transferer + " attempted to convert " + item + " to a city decoration but this feature is not yet implemented.");
                return SCRIPT_OVERRIDE;
            }
            if (getTemplateName(item).equals("object/tangible/storyteller/story_token_combat_npc.iff"))
            {
                //createActorProp(self, item, transferer);
                broadcast(transferer, "This feature is not yet implemented.");
                LOG("ethereal", "[City Decor Converter]: " + transferer + " attempted to convert " + item + " to a city decoration but this feature is not yet implemented.");
                return SCRIPT_OVERRIDE;
            }
            if (getTemplateName(item).contains("furniture/city/statue") || getTemplateName(item).contains("furniture/city/streetlamp") || getTemplateName(item).contains("furniture/city/fountain"))
            {
                broadcast(transferer, "These items do not require conversion.");
                LOG("ethereal", "[City Decor Converter]: " + transferer + " attempted to convert " + item + " to a city decoration but they are already a city decoration.");
                return SCRIPT_OVERRIDE;
            }
            else if (getTemplateName(item).contains("object/tangible/"))
            {
                String oldName = "";
                if (static_item.isStaticItem(item))
                {
                    oldName = getNameNoSpam(item);
                }
                else
                {
                    oldName = getEncodedName(item);
                }
                setName(item, oldName + " (City Decoration)");
                removeAllObjVars(item);
                detachAllScripts(item);
                attachScript(item, "systems.city.city_furniture");
                setObjVar(item, "convertedCityDecor", 1);
                LOG("ethereal", "[City Decor Converter]: " + transferer + " converted " + item + " to a city decoration.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                broadcast(transferer, "You can only convert physical items.");
                return SCRIPT_OVERRIDE;
            }
        }
        else
        {
            broadcast(transferer, "This item is not valid.");
        }
        return SCRIPT_CONTINUE;
    }

    //@Note: Converts a story teller object to a city actor object
    public void createActorProp(obj_id self, obj_id item, obj_id transferer) throws InterruptedException
    {
        String itemName = getStaticItemName(item);
        int row = dataTableSearchColumnForString(itemName, "name", STORYTELLER_DATATABLE);
        dictionary dict = dataTableGetRow(STORYTELLER_DATATABLE, itemName);
        String template = dict.getString("template_name");
        obj_id prop = create.createObject(template, utils.getInventoryContainer(transferer), "");
        if (isIdValid(prop))
        {
            detachAllScripts(prop);
            attachScript(prop, "systems.city.city_actor");
            destroyObject(item);
        }
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU40, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU1, new string_id("Create Actor Deed"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {

        if (item == menu_info_types.SERVER_MENU1)
        {
            obj_id deed = static_item.createNewItemFunction("item_city_actor_deed", utils.getInventoryContainer(player));
            if (isIdValid(deed))
            {
                debugConsoleMsg(player, "Deed Object: " + deed);
                if (!hasScript(deed, "systems.city.city_hire"))
                {
                    attachScript(deed, "systems.city.city_hire");
                }
            }
        }
        return SCRIPT_CONTINUE;
    }

}
