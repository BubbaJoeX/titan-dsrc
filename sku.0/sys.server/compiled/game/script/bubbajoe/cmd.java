package script.bubbajoe;/*
@Origin: dsrc.script.bubbajoe
@Author:  BubbaJoeX
@Purpose: Command Handler for /developer
@Requirements: N/A
@Notes: For testing use only.
@Created: Saturday, 9/27/2025, at 10:26 AM, 
@Copyright © SWG: Titan 2025.
    Unauthorized usage, viewing or sharing of this file is prohibited.
*/

import script.library.utils;
import script.obj_id;

import java.util.StringTokenizer;

public class cmd extends script.base_script
{
    @SuppressWarnings("unused")
    public int cmdDeveloper(obj_id self, obj_id target, String params, float defaultTime) throws InterruptedException
    {
        // Check for empty command
        if (params == null || params.isEmpty())
        {
            broadcast(self, "Invalid command.");
            return SCRIPT_CONTINUE;
        }

        // Validate target
        if (!isIdValid(target))
        {
            broadcast(self, "Invalid target.");
            return SCRIPT_CONTINUE;
        }

        // Ensure caller is a player, sanity check lel
        if (!isPlayer(self))
        {
            broadcast(self, "Only players can use this command.");
            return SCRIPT_CONTINUE;
        }

        // Tokenize parameters
        StringTokenizer st = new StringTokenizer(params);
        if (!st.hasMoreTokens())
        {
            broadcast(self, "No subcommand provided.");
            return SCRIPT_CONTINUE;
        }

        // Get main subcommand
        String subcommand = st.nextToken().toLowerCase();

        switch (subcommand)
        {
            case "teleport":
                if (st.countTokens() < 2)
                {
                    broadcast(self, "Usage: teleport <x> <z> [optional: height]");
                }
                else
                {
                    try
                    {
                        float x = Float.parseFloat(st.nextToken());
                        float z = Float.parseFloat(st.nextToken());
                        float y = st.hasMoreTokens() ? Float.parseFloat(st.nextToken()) : 0f;
                        broadcast(self, "Teleporting to " + x + ", " + y + ", " + z);
                    } catch (NumberFormatException e)
                    {
                        broadcast(self, "Invalid coordinates.");
                    }
                }
                break;

            case "heal":

                int maxHealth = getMaxHealth(self);
                int maxAction = getMaxAction(self);
                setAttrib(self,  HEALTH, maxHealth);
                setAttrib(self,  ACTION, maxAction);
                broadcast(self, "Healed for " + maxHealth + "HP and " + maxAction + "AP.");
                break;

            case "toggle":
                /*
                Handles the showing of object spam.
                 */
                if (st.countTokens() < 1)
                {
                    broadcast(self, "Usage: toggle [on/off]");
                }
                else
                {
                    if (st.nextToken().equalsIgnoreCase("off"))
                    {
                        if (utils.hasScriptVar(self, "toggleDevSpam"))
                        {
                            utils.removeScriptVar(self, "toggleDevSpam");
                        }
                    }
                    else if (st.nextToken().equalsIgnoreCase("on"))
                    {
                        utils.setScriptVar(self, "toggleDevSpam", 1);
                    }
                }
                break;

            default:
                broadcast(self, "Unknown developer subcommand: " + subcommand);
                break;
        }

        return SCRIPT_CONTINUE;
    }
}
