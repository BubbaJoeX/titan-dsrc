package script.swgor;/*
@Origin: dsrc.script.swgor
@Author:  BubbaJoeX
@Purpose: <no purpose>
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Sunday, 1/12/2025, at 1:15 PM, 
@Copyright © SWG-OR 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.*;

public class welcome_pack extends script.terminal.terminal_character_builder
{

    public static final float PSUEDO_CRAFT_QUALITY = 84.5f;
    public static final float PSUEDO_CRAFT_QUALITY_WEAPON = 100.0f;
    public static final float PSUEDO_CRAFT_QUALITY_HARVESTERS = 100.0f;
    public static final int ARMOR_LEVEL = 2;
    public static final int INV_SPACE_NEEDED = 35;
    public static final int ITHORIAN = 33;
    public static final int WOOKIEE = 4;

    public int OnAttach(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        sync(self);
        return SCRIPT_CONTINUE;
    }

    public void sync(obj_id self)
    {
        setName(self, "SWG-OR Welcome Package");
        setDescriptionString(self, "This package contains starter goods for your account. This item can only be redeemed once per account.");
        setObjVar(self, "noTradeShared", 1);
        setObjVar(self, "noTrade", 1);
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        if (canManipulate(player, self, false, true, 3.0f, false))
        {
            if (!utils.isNestedWithinAPlayer(self))
            {
                return SCRIPT_CONTINUE;
            }
            if (hasObjVar(self, "welcome_pack.opened") && !isGod(player))
            {
                return SCRIPT_CONTINUE;
            }
            else
            {
                mi.addRootMenu(menu_info_types.ITEM_USE, string_id.unlocalized("Claim"));
            }

        }
        else
        {
            mi.addRootMenu(menu_info_types.ITEM_USE_OTHER, string_id.unlocalized("(No Access)"));
        }
        sendDirtyObjectMenuNotification(self);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            if (!utils.isNestedWithinAPlayer(self))
            {
                return SCRIPT_CONTINUE;
            }
            if (hasObjVar(self, "welcome_pack.opened") && !isGod(player))
            {
                broadcast(player, "This object does not interest you.");
                return SCRIPT_CONTINUE;
            }
            if (canManipulate(player, self, false, true, 3.0f, true))
            {
                if (getVolumeFree(utils.getInventoryContainer(player)) <= INV_SPACE_NEEDED)
                {
                    broadcast(player, "You must have at least " + INV_SPACE_NEEDED + " inventory slots open to claim this welcome package.");
                    return SCRIPT_CONTINUE;
                }
                sui.msgbox(self, player, "Are you sure you wish to claim this package? This package one-per-account and cannot be obtained again.", sui.OK_CANCEL, "SWG-OR", "handleConfirmation");
            }
            else
            {
                broadcast(player, "This object does not interest you.");
                return SCRIPT_CONTINUE;
            }
        }
        sendDirtyObjectMenuNotification(self);
        return SCRIPT_CONTINUE;
    }

    public int handleConfirmation(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int btnPressed = sui.getIntButtonPressed(params);
        if (btnPressed == sui.BP_CANCEL)
        {
            broadcast(player, "You decide to not claim this package.");
            return SCRIPT_CONTINUE;
        }
        if (btnPressed == sui.BP_OK)
        {
            handleGrant(self, player);
            broadcast(player, "You have claimed this package.");
        }
        return SCRIPT_CONTINUE;
    }

    public void handleGrant(obj_id self, obj_id player) throws InterruptedException
    {
        grantLevelItems(player);
        grantArmors(player);
        grantHousing(player);
        grantOthers(player);
        grantWeapons(player);
        grantCrafting(player);
        setObjVar(player, "welcome_pack.opened", true);
        destroyObject(self);
    }

    public void grantLevelItems(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        obj_id satchel = createObject("object/tangible/container/general/satchel.iff", pInv, "");
        setName(satchel, "Item Satchel");
        static_item.createNewItemFunction("item_auto_level_90_buddy_conversion", satchel);
        static_item.createNewItemFunction("item_auto_level_90_buddy_conversion", satchel);
        static_item.createNewItemFunction("character_heroic_unlock", satchel);
        static_item.createNewItemFunction("character_heroic_unlock", satchel);
        static_item.createNewItemFunction("starter_pack_jewels", satchel);
    }

    public void grantWeapons(obj_id player) throws InterruptedException
    {
        int profession = utils.getPlayerProfession(player);
        obj_id pInv = utils.getInventoryContainer(player);
        obj_id satchel = createObject("object/tangible/container/general/satchel.iff", pInv, "");
        if (profession == utils.FORCE_SENSITIVE)
        {
            obj_id saber1h = createObject("object/weapon/melee/sword/crafted_saber/sword_lightsaber_one_handed_gen4.iff", satchel, "");
            obj_id saberPolearm = createObject("object/weapon/melee/polearm/crafted_saber/sword_lightsaber_polearm_gen4.iff", satchel, "");
            obj_id saber2h = createObject("object/weapon/melee/2h_sword/crafted_saber/sword_lightsaber_two_handed_gen4.iff", satchel, "");
            obj_id[] lightsaber = {saber1h, saberPolearm, saber2h};
            for (obj_id lightsaberWeapon : lightsaber)
            {
                setObjVar(lightsaberWeapon, "starterWeapon", true);
                setWeaponMinDamage(lightsaberWeapon, 650);
                setWeaponMaxDamage(lightsaberWeapon, 1100);
                setWeaponAttackSpeed(lightsaberWeapon, 1.0f);
                setWeaponRangeInfo(lightsaberWeapon, 0.0f, 5.0f);
                setWeaponDamageType(lightsaberWeapon, DAMAGE_ENERGY);
                setObjVar(lightsaberWeapon, weapons.OBJVAR_WP_LEVEL, 80);
                setCrafter(lightsaberWeapon, player);
                putIn(lightsaberWeapon, satchel);
            }
            int randomIndex = rand(2, 5);
            jedi.createColorCrystal(satchel, randomIndex);
            jedi.createColorCrystal(satchel, randomIndex - 1);
            setName(satchel, "a satchel of forgotten ways");
            setObjVar(player, "veteran.freebie_fs", 1);
        }
        setName(satchel, "Weapon Satchel");
        obj_id knuckler = createObject("object/weapon/melee/special/vibroknuckler.iff", satchel, "");
        obj_id ryykSword = createObject("object/weapon/melee/sword/sword_blade_ryyk.iff", satchel, "");
        obj_id heavyBeam = createObject("object/weapon/ranged/heavy/heavy_lightning_beam.iff", satchel, "");
        obj_id rifle = createObject("object/weapon/ranged/rifle/rifle_sg82.iff", satchel, "");
        obj_id carbineE11 = createObject("object/weapon/ranged/carbine/carbine_e11.iff", satchel, "");
        obj_id pistolIntimidator = createObject("object/weapon/ranged/pistol/pistol_intimidator.iff", satchel, "");

        int rangedDPS = 1000;
        int meleeDPS = 1050;
        int heavyDPS = 1200;


        //loop through knuckler and ryyksword and edit weapon stats
        obj_id[] melee = {knuckler, ryykSword};
        obj_id[] heavy = {heavyBeam};
        obj_id[] ranged = {rifle, pistolIntimidator, carbineE11};

        for (obj_id meleeWeapon : melee)
        {
            setObjVar(meleeWeapon, "starterWeapon", true);
            setWeaponMinDamage(meleeWeapon, 800);
            setWeaponMaxDamage(meleeWeapon, 1250);
            setWeaponAttackSpeed(meleeWeapon, 1.0f);
            setWeaponRangeInfo(meleeWeapon, 0f, 4f);
            setWeaponDamageType(meleeWeapon, DAMAGE_KINETIC);
            setObjVar(meleeWeapon, weapons.OBJVAR_WP_LEVEL, 80);
        }

        for (obj_id heavyWeapon : heavy)
        {
            setObjVar(heavyWeapon, "starterWeapon", true);
            setWeaponMinDamage(heavyWeapon, 533);
            setWeaponMaxDamage(heavyWeapon, 900);
            setWeaponAttackSpeed(heavyWeapon, 1.0f);
            setWeaponElementalType(heavyWeapon, DAMAGE_ELEMENTAL_ELECTRICAL);
            setWeaponElementalDamage(heavyWeapon, DAMAGE_ELEMENTAL_ELECTRICAL, 242);
            setWeaponRangeInfo(heavyWeapon, 0f, 64f);
            setWeaponDamageRadius(heavyWeapon, 4);
        }

        for (obj_id rangedWeapon : ranged)
        {
            setObjVar(rangedWeapon, "starterWeapon", true);
            String template = getTemplateName(rangedWeapon);
            if (template.contains("rifle_"))
            {
                setWeaponRangeInfo(rangedWeapon, 0f, 64f);
                setWeaponAttackSpeed(rangedWeapon, 0.8f);
                setWeaponMinDamage(rangedWeapon, 600);
                setWeaponMaxDamage(rangedWeapon, 1000);
                setObjVar(rangedWeapon, weapons.OBJVAR_WP_LEVEL, 80);
            }
            else if (template.contains("carbine_"))
            {
                setWeaponRangeInfo(rangedWeapon, 0f, 45f);
                setWeaponAttackSpeed(rangedWeapon, 0.6f);
                setWeaponMinDamage(rangedWeapon, 300);
                setWeaponMaxDamage(rangedWeapon, 900);
                setObjVar(rangedWeapon, weapons.OBJVAR_WP_LEVEL, 80);
            }
            else if (template.contains("pistol_"))
            {
                setWeaponRangeInfo(rangedWeapon, 0f, 35f);
                setWeaponAttackSpeed(rangedWeapon, 0.4f);
                setWeaponMinDamage(rangedWeapon, 230);
                setWeaponMaxDamage(rangedWeapon, 570);
            }
            setWeaponDamageType(rangedWeapon, DAMAGE_ENERGY);
        }

    }

    public void grantArmors(obj_id player) throws InterruptedException
    {
        utils.setScriptVar(player, "character_builder.armorLevel", ARMOR_LEVEL);
        int profession = utils.getPlayerProfession(player);
        obj_id pInv = utils.getInventoryContainer(player);
        int species = getSpecies(player);
        if (species == WOOKIEE)
        {
            if (profession == utils.COMMANDO || profession == utils.BOUNTY_HUNTER)
            {
                issueAssaultArmorSet(player, ARMOR_SET_ASSAULT_WOOKIEE);
            }
            else if (profession == utils.SPY || profession == utils.OFFICER)
            {
                issueBattleArmorSet(player, ARMOR_SET_BATTLE_WOOKIEE);
            }
            else if (profession == utils.MEDIC || profession == utils.SMUGGLER)
            {
                issueReconArmorSet(player, ARMOR_SET_RECON_WOOKIEE);
            }
            else
            {
                issueBattleArmorSet(player, ARMOR_SET_BATTLE_WOOKIEE);
            }
        }
        else if (species == ITHORIAN)
        {
            if (profession == utils.COMMANDO || profession == utils.BOUNTY_HUNTER)
            {
                issueAssaultArmorSet(player, ARMOR_SET_ASSAULT_ITHORIAN);
            }
            else if (profession == utils.SPY || profession == utils.OFFICER)
            {
                issueBattleArmorSet(player, ARMOR_SET_BATTLE_ITHORIAN);
            }
            else if (profession == utils.MEDIC || profession == utils.SMUGGLER)
            {
                issueReconArmorSet(player, ARMOR_SET_RECON_ITHORIAN);
            }
            else
            {
                issueBattleArmorSet(player, ARMOR_SET_BATTLE_ITHORIAN);
            }
        }
        else //ALL
        {
            if (profession == utils.COMMANDO || profession == utils.BOUNTY_HUNTER)
            {
                issueAssaultArmorSet(player, ARMOR_SET_ASSAULT_1);
            }
            else if (profession == utils.SPY || profession == utils.OFFICER)
            {
                issueBattleArmorSet(player, ARMOR_SET_BATTLE_3);
            }
            else if (profession == utils.MEDIC || profession == utils.SMUGGLER)
            {
                issueReconArmorSet(player, ARMOR_SET_RECON_2);
            }
            else
            {
                issueBattleArmorSet(player, ARMOR_SET_BATTLE_3);
            }
        }
        makeCraftedItem("object/draft_schematic/armor/shield_generator_personal_c.iff", PSUEDO_CRAFT_QUALITY - 25.25f, pInv);
    }

    public void grantHousing(obj_id player) throws InterruptedException
    {
        obj_id pInv = utils.getInventoryContainer(player);
        obj_id deed = createObject("object/tangible/loot/npc_loot/datapad_flashy_generic.iff", pInv, "");
        attachScript(deed, "developer.bubbajoe.wd.wd_house_picker");
    }

    public void grantOthers(obj_id player)
    {
        if (!hasCommand(player, "veteranPlayerBuff"))
        {
            grantCommand(player, "veteranPlayerBuff");
        }
        doCoolEffects(player);
    }

    private void doCoolEffects(obj_id player)
    {
        location corpsePosition = getLocation(player);
        float radius = getObjectCollisionRadius(player);
        location[] offsets = new location[8];

        for (int i = 0; i < 8; i++)
        {
            float angle = (float) (i * Math.PI / 4);
            float xOffset = radius * (float) Math.cos(angle);
            float zOffset = radius * (float) Math.sin(angle);
            offsets[i] = new location(corpsePosition.x + xOffset, corpsePosition.y + radius, corpsePosition.z + zOffset, corpsePosition.area, corpsePosition.cell);
        }

        for (location offset : offsets)
        {
            if (isInWorldCell(player))
            {
                playClientEffectLoc(player, "appearance/pt_fireworks_03.prt", offset, 1f);
                playClientEffectLoc(player, "appearance/pt_skill_up.prt", offset, 1f);
            }
        }
    }

    public void grantCrafting(obj_id player) throws InterruptedException
    {
        int profession = utils.getPlayerProfession(player);
        obj_id pInv = utils.getInventoryContainer(player);

        String[] SCHEMATICS = {
                "object/draft_schematic/structure/installation_generator_solar.iff",
                "object/draft_schematic/structure/installation_mining_gas.iff",
                "object/draft_schematic/structure/installation_mining_liquid.iff",
                "object/draft_schematic/structure/installation_mining_ore.iff",
                "object/draft_schematic/structure/installation_mining_flora.iff"
        };

        for (String template : SCHEMATICS)
        {
            makeCraftedItem(template, PSUEDO_CRAFT_QUALITY_HARVESTERS, pInv);
        }
    }

}
