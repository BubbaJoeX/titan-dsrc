# Server-Side Conversations (STF-less)

This document describes the new server-side conversation system that allows creating conversations without requiring `.stf` string files.

## Overview

The traditional conversation system requires:
1. A `.stf` string table file with all dialog text
2. Response matching via string IDs like `"s_6"`, `"s_7"`

The new server-side system allows:
1. Dialog text defined directly in Java code
2. Custom response IDs for matching
3. Full backwards compatibility with existing STF-based conversations

## Usage

### Basic Structure

```java
public class my_conversation extends script.conversation.base.conversation_base
{
    public String conversation = "conversation.my_conversation";
    public String scriptName = "my_conversation";

    public my_conversation()
    {
        super.scriptName = scriptName;
        super.conversation = conversation;
        // Note: c_stringFile is not needed for server-side conversations
    }
}
```

### Starting a Conversation

```java
@Override
public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
{
    if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
    {
        return SCRIPT_OVERRIDE;
    }

    return serverSide_startConversation(
        player,
        self,
        "Hello! How can I help you today?",  // NPC message
        1,  // branchId for tracking conversation state
        new ConvoResponse[] {
            convo("quest", "Do you have any tasks?"),
            convo("shop", "I'd like to browse your wares."),
            convo("bye", "Goodbye.")
        }
    );
}
```

### Handling Responses

```java
@Override
public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
{
    if (!conversationId.equals(scriptName))
    {
        return SCRIPT_CONTINUE;
    }

    int branchId = utils.getIntScriptVar(player, conversation + ".branchId");

    if (branchId == 1)
    {
        if (responseIdIs(response, "quest"))
        {
            return serverSide_respond(
                player,
                "I need someone to deliver this package.",
                2,  // new branchId
                new ConvoResponse[] {
                    convo("accept", "I'll do it."),
                    convo("decline", "Not right now.")
                }
            );
        }
        if (responseIdIs(response, "shop"))
        {
            // Open shop UI, etc.
            return serverSide_endConversation(player, "Take your time browsing.");
        }
        if (responseIdIs(response, "bye"))
        {
            return serverSide_endConversation(player, "Farewell, traveler!");
        }
    }

    if (branchId == 2)
    {
        if (responseIdIs(response, "accept"))
        {
            // Grant quest, etc.
            return serverSide_endConversation(player, "Excellent! Here's the package.");
        }
        if (responseIdIs(response, "decline"))
        {
            return serverSide_endConversation(player, "Come back if you change your mind.");
        }
    }

    utils.removeScriptVar(player, conversation + ".branchId");
    return SCRIPT_CONTINUE;
}
```

## API Reference

### ConvoResponse Class

Represents a conversation response option.

```java
ConvoResponse convo(String id, String text)
```

- `id` - Unique identifier for matching in `OnNpcConversationResponse`
- `text` - Display text shown to the player

### Server-Side Methods

#### serverSide_startConversation

Starts a new conversation with response options.

```java
int serverSide_startConversation(obj_id player, obj_id self, String npcMessage, int branchId, ConvoResponse[] responses)
```

#### serverSide_respond

Continues a conversation with new text and options.

```java
int serverSide_respond(obj_id player, String npcMessage, int branchId, ConvoResponse[] responses)
```

#### serverSide_endConversation

Ends the conversation with a final message.

```java
int serverSide_endConversation(obj_id player, String npcMessage)
```

### Response Matching

#### responseIdIs

Checks if the response matches a given ID (recommended for most cases).

```java
boolean responseIdIs(string_id response, String responseId)
```

#### responseIs

Checks if the response matches a specific ConvoResponse object.

```java
boolean responseIs(string_id response, ConvoResponse convoResponse)
```

## Backwards Compatibility

Existing conversations using STF files continue to work unchanged. The legacy methods are preserved:

- `craft_response()` - For STF-based responses
- `craft_repeater()` - For STF-based repeating conversations
- `craft_response_prose()` - For STF-based prose package conversations

The traditional response matching `response.equals("s_6")` continues to work for STF-based conversations.

## Technical Details

### How It Works

1. Response IDs and display text are combined in format: `"responseId|displayText"`
2. The client's LocalizationManager recognizes the special `"convo_response"` table
3. Client displays only the text after the `|` pipe character
4. Server matches responses using the full string including the ID

### Files Modified

- `string_id.java` - Added `convoResponse()` method and helper methods
- `conversation_base.java` - Added `serverSide_*` methods and `ConvoResponse` class
- `LocalizationManager.cpp` (client & server) - Added `convo_response` table handling

## Example

See `conversation/example_serverside_convo.java` for a complete working example.

