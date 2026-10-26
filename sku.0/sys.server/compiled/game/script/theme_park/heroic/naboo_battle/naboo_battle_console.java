package script.theme_park.heroic.naboo_battle;

import script.*;
import script.library.*;

/*
 * Server-console dry run of the Battle of Naboo (no player or client needed), through the CentralServer console:
 *
 *   titan.sh console "game any runScript theme_park.heroic.naboo_battle.naboo_battle_console hrc adventure_heroic status"
 *
 *   hrc <scene> status    phase, dome, spawned children, players in the area
 *   hrc <scene> start     beginSpawn on the controller egg (spawns the army; phase 1 waits up to 3 min for players)
 *   hrc <scene> advance   end the current phase as if its goal had been met (hrc_nbooGmAdvance)
 *   hrc <scene> reset     cleanupSpawn (removes every spawned child)
 *   hrc <scene> probe [x z [template part]]   up to 12 objects within 300 m of (x, z) (default the tile centre)
 *
 * "game any" lands on a random game server: a handler on the wrong one answers "wrong scene <name>"; repeat.
 * The method name must not occur in the script path: CentralCommandParserGame takes the parameters from after the
 * FIRST occurrence of the method name in the command line ("heroic" would cut into "theme_park.heroic...").
 * Console handlers have no owner script, so every messageTo uses a delay > 0.
 */
public class naboo_battle_console extends script.base_script
{
    public naboo_battle_console()
    {
    }

    public String hrc(String args) throws InterruptedException
    {
        String[] a = args.trim().split("\\s+");
        int c = 1;
        while (c < a.length && !java.util.Arrays.asList("status", "start", "advance", "reset", "probe").contains(a[c]))
            ++c;
        if (c >= a.length)
            return "usage: hrc <scene> status|start|advance|reset|probe [x z [template part]]";
        String want = a[c - 1], cmd = a[c];
        a = java.util.Arrays.copyOfRange(a, c - 1, a.length);
        String scene = getCurrentSceneName();
        if (!want.equals(scene))
            return "wrong scene " + scene;
        if (cmd.equals("probe"))
        {
            // what is loaded round a point (default the tile centre): is the chunk loaded, did the egg spawn?
            float x = a.length >= 4 ? utils.stringToFloat(a[2]) : 0.0f;
            float z = a.length >= 4 ? utils.stringToFloat(a[3]) : 0.0f;
            String part = a.length >= 5 ? a[4] : "";
            obj_id[] all = getObjectsInRange(new location(x, getHeightAtLocation(x, z), z, scene), 300.0f);
            StringBuilder sb = new StringBuilder("objects within 300 m of (" + x + ", " + z + ") on " + scene + ": " + (all == null ? 0 : all.length));
            int shown = 0;
            if (all != null)
                for (int i = 0; i < all.length && shown < 12; ++i)
                {
                    String template = getTemplateName(all[i]);
                    if (template == null || !template.contains(part))
                        continue;
                    ++shown;
                    String[] scripts = getScriptList(all[i]);
                    sb.append(" | ").append(all[i]).append(" ").append(template).append(" @ ")
                        .append(getLocation(all[i])).append(" scripts ")
                        .append(scripts == null ? "none" : String.join(",", scripts));
                }
            return sb.toString();
        }
        obj_id controller = null;
        obj_id[] near = getObjectsInRange(new location(0.0f, getHeightAtLocation(0.0f, 0.0f), 0.0f, scene), 50.0f);
        if (near != null)
            for (obj_id o : near)
                if (isIdValid(o) && hasScript(o, naboo_battle_lib.SCRIPT_CONTROLLER))
                    controller = o;
        if (!isIdValid(controller))
            return "no Battle of Naboo controller near (0, 0) on " + scene;
        switch (cmd)
        {
            case "start":
                messageTo(controller, "beginSpawn", null, 0.5f, false);
                return "beginSpawn sent to " + controller;
            case "advance":
                messageTo(controller, "hrc_nbooGmAdvance", null, 0.5f, false);
                return "advance sent to " + controller + " (phase " + naboo_battle_lib.getPhase(controller) + ")";
            case "reset":
                messageTo(controller, "cleanupSpawn", null, 0.5f, false);
                return "cleanupSpawn sent to " + controller;
            default:
            {
                return "controller " + controller + " at " + getLocation(controller) + " phase " + naboo_battle_lib.getPhase(controller)
                    + " dome " + (naboo_battle_lib.isDomeUp(controller) ? "up" : "down")
                    + " children " + naboo_battle_lib.children(controller, "spawn_id").length
                    + " players " + naboo_battle_lib.getPlayers(controller).length;
            }
        }
    }
}
