package script.conversation;

import script.*;
import script.conversation.base.ConvoResponse;
import script.library.*;
import script.theme_park.heroic.naboo_battle.naboo_battle_lib;

/**
 * Bombad General Jar Jar Binks by the Royal Palace in Theed: the quest giver of the Historical heroic "The Battle
 * of Naboo" (docs/content/battle-of-naboo/README.md). Jar Jar's lines come from theme_park/heroic_naboo_battle.stf;
 * the player's replies are server-side (STF-less) responses, see SERVER_SIDE_CONVERSATIONS.md.
 *
 * Also carries the Historical marker: CONDITION_INTERESTING plus an hp_dyn app slot on the "@interesting" hardpoint
 * (appearance/titan_info_disk_historical.apt) that Titan clients show instead of the stock info disc.
 */
public class jar_jar_binks_historical extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.jar_jar_binks_historical";
    public String scriptName = "jar_jar_binks_historical";

    public static final String MARKER = "appearance/titan_info_disk_historical.apt";
    public static final int MIN_LEVEL = 90;
    public static final float FACING = 180.0f;          // toward the plaza, back to the palace steps
    public static final int SKIN_INDEX = 23;             // palette/npc_skin_gungan.pal (208,127,87), his orange-tan

    public jar_jar_binks_historical()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
    }

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setup(self);
        return SCRIPT_CONTINUE;
    }

    public void setup(obj_id self) throws InterruptedException
    {
        setCondition(self, CONDITION_CONVERSABLE);
        setCondition(self, CONDITION_INTERESTING);
        setObjVar(self, "hp_dyn.historical.kind", "app");
        setObjVar(self, "hp_dyn.historical.hp", "@interesting");
        setObjVar(self, "hp_dyn.historical.path", MARKER);
        setInvulnerable(self, true);
        hue.setColor(self, "/private/index_color_1", SKIN_INDEX);
        setYaw(self, FACING);
        ai_lib.setDefaultCalmBehavior(self, ai_lib.BEHAVIOR_SENTINEL);
    }

    public string_id line(String key)
    {
        return naboo_battle_lib.sid(key);
    }

    public boolean canGo(obj_id player) throws InterruptedException
    {
        return isGod(player) || getLevel(player) >= MIN_LEVEL;
    }

    public void branch(obj_id player, int b) throws InterruptedException
    {
        utils.setScriptVar(player, conversation + ".branchId", b);
    }

    public string_id[] ids(ConvoResponse[] r)
    {
        string_id[] out = new string_id[r.length];
        for (int i = 0; i < r.length; ++i)
            out[i] = r[i].toStringId();
        return out;
    }

    @Override
    public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
    {
        if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
            return SCRIPT_OVERRIDE;
        faceTo(self, player);
        if (!canGo(player))
        {
            npcEndConversationWithMessage(player, line("convo_not_ready"));
            return SCRIPT_CONTINUE;
        }
        boolean flagged = instance.isFlaggedForInstance(player, naboo_battle_lib.INSTANCE_NAME);
        ConvoResponse[] r = flagged
            ? new ConvoResponse[] { convo("go", "Send us to the Great Grass Plains."), convo("story", "Tell me the story again."), convo("bye", "Not now, General.") }
            : new ConvoResponse[] { convo("story", "How did you become a general?"), convo("bye", "Maybe another time.") };
        branch(player, 1);
        npcStartConversation(player, self, scriptName, line(flagged ? "convo_return" : "convo_greet"), ids(r));
        return SCRIPT_CONTINUE;
    }

    @Override
    public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
    {
        if (!conversationId.equals(scriptName))
            return SCRIPT_CONTINUE;
        int b = utils.getIntScriptVar(player, conversation + ".branchId");
        if (responseIdIs(response, "bye"))
            return end(player, "convo_remember");
        if (responseIdIs(response, "story"))
        {
            branch(player, 2);
            npcSpeak(player, line("convo_offer"));
            npcSetConversationResponses(player, ids(new ConvoResponse[] {
                convo("accept", "The Gungans need us? Count us in!"),
                convo("decline", "Perhaps another day.") }));
            return SCRIPT_CONTINUE;
        }
        if (b == 2 && responseIdIs(response, "accept"))
        {
            instance.flagPlayerForInstance(player, naboo_battle_lib.INSTANCE_NAME);
            if (!groundquests.isQuestActive(player, naboo_battle_lib.QUEST))
                groundquests.requestGrantQuest(player, naboo_battle_lib.QUEST);
            branch(player, 3);
            npcSpeak(player, line("convo_accept"));
            npcSetConversationResponses(player, ids(new ConvoResponse[] {
                convo("go", "We're ready. Send us in."),
                convo("later", "We'll gather the group first.") }));
            return SCRIPT_CONTINUE;
        }
        if (responseIdIs(response, "decline") || responseIdIs(response, "later"))
            return end(player, "convo_decline");
        if (responseIdIs(response, "go"))
        {
            if (!canGo(player))
                return end(player, "convo_not_ready");
            if (!groundquests.isQuestActive(player, naboo_battle_lib.QUEST))
                groundquests.requestGrantQuest(player, naboo_battle_lib.QUEST);
            end(player, "convo_sending");
            instance.requestInstanceMovement(player, naboo_battle_lib.INSTANCE_NAME);
            return SCRIPT_CONTINUE;
        }
        utils.removeScriptVar(player, conversation + ".branchId");
        return SCRIPT_CONTINUE;
    }

    public int end(obj_id player, String key) throws InterruptedException
    {
        utils.removeScriptVar(player, conversation + ".branchId");
        npcEndConversationWithMessage(player, line(key));
        return SCRIPT_CONTINUE;
    }
}
