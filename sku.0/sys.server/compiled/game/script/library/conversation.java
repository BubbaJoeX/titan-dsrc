package script.library;

import script.base_class;
import script.location;
import script.obj_id;
import script.prose_package;
import script.string_id;

public class conversation extends script.base_script
{
    public conversation()
    {
    }
    public static void echoToGroup(obj_id player, obj_id actor, obj_id target, string_id sid) throws InterruptedException
    {
        if (!group.isGrouped(player))
        {
            return;
        }
        chat.chat(actor, target, sid, chat.ChatFlag_targetAndSourceGroup | chat.ChatFlag_skipTarget | chat.ChatFlag_skipSource);
    }
    public static void echoToGroup(obj_id player, obj_id actor, obj_id target, prose_package pp) throws InterruptedException
    {
        if (!group.isGrouped(player))
        {
            return;
        }
        chat.chat(actor, target, pp, chat.ChatFlag_targetAndSourceGroup | chat.ChatFlag_skipTarget | chat.ChatFlag_skipSource);
    }

    public static boolean npcConversationCameraLookAtTarget(obj_id player, obj_id target, float holdTime, float transitionDuration)
    {
        return base_class.npcConversationCameraLookAtTarget(player, target, holdTime, transitionDuration);
    }

    public static boolean npcConversationCameraLookAtTarget(obj_id player, obj_id target, float holdTime)
    {
        return base_class.npcConversationCameraLookAtTarget(player, target, holdTime);
    }

    public static boolean npcConversationCameraLookAtTarget(obj_id player, obj_id target)
    {
        return base_class.npcConversationCameraLookAtTarget(player, target);
    }

    public static boolean npcConversationCameraLookAtPosition(obj_id player, float x, float y, float z, float holdTime, float transitionDuration)
    {
        return base_class.npcConversationCameraLookAtPosition(player, x, y, z, holdTime, transitionDuration);
    }

    public static boolean npcConversationCameraLookAtPosition(obj_id player, float x, float y, float z, float holdTime)
    {
        return base_class.npcConversationCameraLookAtPosition(player, x, y, z, holdTime);
    }

    public static boolean npcConversationCameraLookAtPosition(obj_id player, float x, float y, float z)
    {
        return base_class.npcConversationCameraLookAtPosition(player, x, y, z);
    }

    public static boolean npcConversationCameraLookAtPosition(obj_id player, location loc, float holdTime, float transitionDuration)
    {
        return base_class.npcConversationCameraLookAtPosition(player, loc, holdTime, transitionDuration);
    }

    public static boolean npcConversationCameraLookAtPosition(obj_id player, location loc, float holdTime)
    {
        return base_class.npcConversationCameraLookAtPosition(player, loc, holdTime);
    }

    public static boolean npcConversationCameraLookAtPosition(obj_id player, location loc)
    {
        return base_class.npcConversationCameraLookAtPosition(player, loc);
    }

    public static boolean npcConversationCameraReturnToSpeaker(obj_id player)
    {
        return base_class.npcConversationCameraReturnToSpeaker(player);
    }
}
