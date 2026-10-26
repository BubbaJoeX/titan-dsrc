package script.content;/*
@Origin: dsrc.script.content
@Author:  BubbaJoeX
@Purpose: Request instance travel from the Hub.
@Requirements: <no requirements>
@Notes: <no notes>
@Created: Sunday, 5/5/2024, at 4:44 AM, 
@Copyright © SWG: Titan 2024.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.*;
import script.library.buff;
import script.library.instance;
import script.library.money;
import script.library.sui;

public class tos_instance extends base_script
{

    public static final int COST = 25000;

    public int OnAttach(obj_id self)
    {
        clearCondition(self, CONDITION_HOLIDAY_INTERESTING);
        clearCondition(self, CONDITION_INTERESTING);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        clearCondition(self, CONDITION_HOLIDAY_INTERESTING);
        clearCondition(self, CONDITION_INTERESTING);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, new string_id("Depart from Orbital Station to Heroic Instance"));
        if (isGod(player))
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU1, new string_id("Set Instance"));
        }
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == menu_info_types.ITEM_USE)
        {
            sui.msgbox(self, player, "\\#e3d005Do you wish to pay 25,000 credits to travel to this Heroic Instance?\\#", sui.OK_CANCEL, "\\#e3d005Departure Payment", "handlePay");
            return SCRIPT_CONTINUE;
        }
        else if (item == menu_info_types.SERVER_MENU1)
        {
            String promptAddition = "";
            promptAddition += "Instances Keys:\n";
            promptAddition += "1: Axkva Min\n";
            promptAddition += "2: Exar Kun\n";
            promptAddition += "3: Tusken Army\n";
            promptAddition += "4: Blackguard ISD\n";
            promptAddition += "5: IG-88 Droid Factory\n";
            promptAddition += "6: Echo Base (Imperial)\n";
            promptAddition += "7: Echo Base (Rebel)\n";
            promptAddition += "8: HK-47\n";
            promptAddition += "9: Mustafar Droid Army\n";
            promptAddition += "10: Decrepit Droid Factory\n";
            promptAddition += "11: Working Droid Factory\n";
            promptAddition += "12: Kubaza Beetle Cavern\n";
            promptAddition += "13: Old Republic Facility\n";
            promptAddition += "14: Sher Kar Cave\n";
            promptAddition += "15: Crystal Cave\n";
            promptAddition += "16: The Battle of Naboo (Historical)\n";
            sui.inputbox(self, player, "\\#e3d005Enter the instance index for this transit.\n\n" + promptAddition + "\\#", "handleInstance");
        }
        return SCRIPT_CONTINUE;
    }

    public int handlePay(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (player == null)
        {
            return SCRIPT_CONTINUE;
        }
        int button = sui.getIntButtonPressed(params);
        if (button == sui.BP_CANCEL)
        {
            broadcast(player, "You have canceled your instance travel.");
            return SCRIPT_CONTINUE;
        }
        else
        {
            if (getTotalMoney(player) < 25000)
            {
                broadcast(player, "You do not have enough credits to depart.");
                return SCRIPT_CONTINUE;
            }
            else
            {
                money.requestPayment(player, self, COST, "no_handler", null, false);
                sui.msgbox(self, player, "\\#e3d005Final Confirmation:\\#.\n\nAre you sure you want to leave the station to depart on a Heroic Adventure?\\#", sui.OK_CANCEL, "\\#e3d005Departure Confirmation", "sendPlayer");
            }
        }
        return SCRIPT_CONTINUE;
    }

    public int handleInstance(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (player == null)
        {
            return SCRIPT_CONTINUE;
        }
        int button = sui.getIntButtonPressed(params);
        if (button == sui.BP_CANCEL)
        {
            broadcast(player, "You have canceled your instance travel.");
            return SCRIPT_CONTINUE;
        }
        else
        {
            int instance = Integer.parseInt(sui.getInputBoxText(params));
            setObjVar(self, "instance", instance);
            broadcast(player, "You have set the instance key to " + instance + ".");
        }
        return SCRIPT_CONTINUE;
    }

    public int sendPlayer(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        int instanceID = getIntObjVar(self, "instance");
        if (instanceID == 0)
        {
            broadcast(player, "The engines appear to have been sabotaged on the away transport.");
            return SCRIPT_CONTINUE;
        }
        int button = sui.getIntButtonPressed(params);
        if (button == sui.BP_CANCEL)
        {
            broadcast(player, "You have canceled your instance travel and have been refunded.");
            transferBankCreditsFromNamedAccount(money.ACCT_TRAVEL, player, COST, "no_handler", "no_handler", null);
            return SCRIPT_CONTINUE;
        }
        switch (instanceID)
        {
            case 1:
                if (instance.isFlaggedForInstance(player, "heroic_axkva_min"))
                {
                    instance.requestInstanceMovement(player, "heroic_axkva_min");
                    broadcast(player, "You have been sent to The Chamber of Banishment.");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 2:
                if (instance.isFlaggedForInstance(player, "heroic_exar_kun"))
                {
                    instance.requestInstanceMovement(player, "heroic_exar_kun");
                    broadcast(player, "You have been sent to the Temple of Exar Kun.");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 3:
                if (instance.isFlaggedForInstance(player, "heroic_tusken_army"))
                {
                    instance.requestInstanceMovement(player, "heroic_tusken_army");
                    broadcast(player, "You have been sent to the Tusken Invasion of Mos Espa.");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 4:
                if (instance.isFlaggedForInstance(player, "heroic_star_destroyer"))
                {
                    instance.requestInstanceMovement(player, "heroic_star_destroyer");
                    broadcast(player, "You have been sent to the Blackguard Imperial Star Destroyer.");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 5:
                if (instance.requestInstanceMovement(player, "heroic_ig88"))
                {
                    broadcast(player, "You have been sent to IG-88's Droid Factory.");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 6:
                boolean canGoToHothImp = instance.isFlaggedForInstance(player, "echo_base");
                if (canGoToHothImp)
                {
                    instance.requestInstanceMovement(player, "echo_base", 2, "imperial");
                    broadcast(player, "You have been deployed to the Battle of Echo Base.");
                    buff.applyBuff(player, "instance_launching");
                }
                break;
            case 7:
                boolean canGoToHothReb = instance.isFlaggedForInstance(player, "echo_base");
                if (canGoToHothReb)
                {
                    instance.requestInstanceMovement(player, "echo_base", 1, "rebel");
                    broadcast(player, "You have been deployed to the Battle of Echo Base.");
                    buff.applyBuff(player, "instance_launching");
                }
                break;
            case 8:
                if (instance.isFlaggedForInstance(player, "mustafar_volcano"))
                {
                    instance.requestInstanceMovement(player, "mustafar_volcano");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 9:
                if (instance.isFlaggedForInstance(player, "mustafar_droid_army"))
                {
                    instance.requestInstanceMovement(player, "mustafar_droid_army");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 10:
                if (instance.isFlaggedForInstance(player, "decrepit_droid_factory"))
                {
                    instance.requestInstanceMovement(player, "decrepit_droid_factory");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 11:
                if (instance.isFlaggedForInstance(player, "working_droid_factory"))
                {
                    instance.requestInstanceMovement(player, "working_droid_factory");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 12:
                if (instance.isFlaggedForInstance(player, "uplink_cave"))
                {
                    instance.requestInstanceMovement(player, "uplink_cave");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 13:
                if (instance.isFlaggedForInstance(player, "old_republic_facility"))
                {
                    instance.requestInstanceMovement(player, "old_republic_facility");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 14:
                if (instance.isFlaggedForInstance(player, "sher_kar_cave"))
                {
                    instance.requestInstanceMovement(player, "sher_kar_cave");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 15:
                if (instance.isFlaggedForInstance(player, "obiwan_crystal_cave"))
                {
                    instance.requestInstanceMovement(player, "obiwan_crystal_cave");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            case 16:
                if (instance.isFlaggedForInstance(player, "heroic_naboo_battle"))
                {
                    instance.requestInstanceMovement(player, "heroic_naboo_battle");
                    broadcast(player, "You have been sent to the Great Grass Plains of Naboo.");
                }
                else
                {
                    broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                }
                break;
            default:
                broadcast(player, "The engines appear to have been sabotaged on the away transport.");
                break;
        }
        return SCRIPT_CONTINUE;
    }
}
