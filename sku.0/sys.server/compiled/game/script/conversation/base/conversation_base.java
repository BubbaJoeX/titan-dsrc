package script.conversation.base;

import script.library.ai_lib;
import script.library.chat;
import script.library.utils;
import script.*;

public class conversation_base extends script.base_script
{
	public String c_stringFile = "";
	public String scriptName = "";
	public String conversation = "";
	public conversation_base()
	{
	}
	public int OnInitialize(obj_id self) throws InterruptedException
	{
		if ((!isTangible(self)) || (isPlayer(self)))
		{
			detachScript(self, conversation);
		}
		setCondition(self, CONDITION_CONVERSABLE);
		return SCRIPT_CONTINUE;
	}
	public int OnAttach(obj_id self) throws InterruptedException
	{
		setCondition(self, CONDITION_CONVERSABLE);
		return SCRIPT_CONTINUE;
	}
	public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info menuInfo) throws InterruptedException
	{
		int menu = menuInfo.addRootMenu(menu_info_types.CONVERSE_START, null);
		menu_info_data menuInfoData = menuInfo.getMenuItemById(menu);
		menuInfoData.setServerNotify(false);
		setCondition(self, CONDITION_CONVERSABLE);
		return SCRIPT_CONTINUE;
	}
	public int OnIncapacitated(obj_id self, obj_id killer) throws InterruptedException
	{
		clearCondition(self, CONDITION_CONVERSABLE);
		detachScript(self, conversation);
		return SCRIPT_CONTINUE;
	}
	public boolean npcStartConversation(obj_id player, obj_id npc, String convoName, string_id greetingId, prose_package greetingProse, string_id[] responses) throws InterruptedException
	{
		Object[] objects = new Object[responses.length];
		System.arraycopy(responses, 0, objects, 0, responses.length);
		return npcStartConversation(player, npc, convoName, greetingId, greetingProse, objects);
	}
	public int OnStartNpcConversation(obj_id self, obj_id player) throws InterruptedException
	{
		if (ai_lib.isInCombat(self) || ai_lib.isInCombat(player))
		{
			return SCRIPT_OVERRIDE;
		}
		chat.chat(self, "Error:  All conditions for OnStartNpcConversation were false.");
		return SCRIPT_CONTINUE;
	}
	public int OnNpcConversationResponse(obj_id self, String conversationId, obj_id player, string_id response) throws InterruptedException
	{
		chat.chat(self, "Error:  Fell through all branches and responses for OnNpcConversationResponse.");
		utils.removeScriptVar(player, conversation + ".branchId");
		return SCRIPT_CONTINUE;
	}

	// ========================================================================
	// Legacy STF-based conversation methods (backwards compatible)
	// ========================================================================

	protected int craft_response(String[] responseStrings, int branchId, obj_id player) throws InterruptedException{
		string_id message = new string_id(c_stringFile, responseStrings[0]);
		string_id responses[] = new string_id[responseStrings.length];
		for(int i = 1; i < responseStrings.length; i++){
			responses[i] = new string_id(c_stringFile, responseStrings[i]);
		}
		if(responses.length > 1){
			utils.setScriptVar(player, conversation + ".branchId", branchId);
			npcSpeak(player, message);
			npcSetConversationResponses(player, responses);
		}
		else
		{
			utils.removeScriptVar(player, conversation + ".branchId");
			npcEndConversationWithMessage(player, message);
		}
		return SCRIPT_CONTINUE;
	}
	protected int craft_repeater(String[] responseStrings, int branchId, obj_id player, obj_id self) throws InterruptedException{
		string_id message = new string_id(c_stringFile, responseStrings[0]);
		string_id responses[] = new string_id[responseStrings.length];

		for(int i = 1; i < responseStrings.length; i++){
			responses[i] = new string_id(c_stringFile, responseStrings[i]);
		}

		if(responseStrings.length > 1) {
			utils.setScriptVar(player, conversation + ".branchId", branchId);
			npcStartConversation(player, self, scriptName, message, responses);
		}
		else{
			chat.chat(self, player, message);
		}

		return SCRIPT_CONTINUE;

	}
	protected int craft_response_prose(String[] responseStrings, int branchId, obj_id player, obj_id self, String name) throws InterruptedException{
		string_id message = new string_id(c_stringFile, responseStrings[0]);
		string_id responses[] = new string_id[responseStrings.length];
		for(int i = 1; i < responseStrings.length; i++){
			responses[i] = new string_id(c_stringFile, responseStrings[i]);
		}
		if(responses.length > 1){
			utils.setScriptVar(player, conversation + ".branchId", branchId);
			prose_package pp = new prose_package();
			pp.stringId = message;
			pp.actor.set(player);
			pp.target.set(self);
			pp.other.set(name);
			npcStartConversation(player, self, scriptName, null, pp, responses);
		}
		else
		{
			prose_package pp = new prose_package();
			pp.stringId = message;
			pp.actor.set(player);
			pp.target.set(self);
			pp.other.set(name);
			chat.chat(self, player, null, null, pp);
		}
		return SCRIPT_CONTINUE;
	}

	// ========================================================================
	// Server-side conversation methods (no STF files required)
	// These methods allow pure server-sided conversations without string files.
	// Response matching uses unique response IDs instead of STF string indices.
	// ========================================================================

	/**
	 * Represents a server-side conversation response with an ID for matching and display text.
	 */
	public static class ConvoResponse
	{
		public final String id;
		public final String text;

		public ConvoResponse(String id, String text)
		{
			this.id = id;
			this.text = text;
		}

		/**
		 * Creates the string_id for this response, combining id and text.
		 * The id is used for matching in OnNpcConversationResponse.
		 */
		public string_id toStringId()
		{
			return string_id.convoResponse(id, text);
		}

		/**
		 * Returns the full match string for use with response.equals() in OnNpcConversationResponse.
		 * Format: "id|text"
		 */
		public String getMatchString()
		{
			return id + "|" + text;
		}
	}

	/**
	 * Creates a conversation response for server-side conversations.
	 *
	 * @param id   Unique identifier for matching in OnNpcConversationResponse
	 * @param text Display text shown to the player
	 * @return A ConvoResponse object for use with serverSide_* methods
	 */
	protected ConvoResponse convo(String id, String text)
	{
		return new ConvoResponse(id, text);
	}

	/**
	 * Starts a server-side conversation with the NPC speaking and providing response options.
	 * No STF files are required - all text is provided directly.
	 *
	 * @param player     The player to converse with
	 * @param self       The NPC
	 * @param npcMessage What the NPC says to the player
	 * @param branchId   Branch ID for tracking conversation state
	 * @param responses  Array of ConvoResponse objects (id + display text)
	 * @return SCRIPT_CONTINUE
	 */
	protected int serverSide_startConversation(obj_id player, obj_id self, String npcMessage, int branchId, ConvoResponse[] responses) throws InterruptedException
	{
		if (responses == null || responses.length == 0)
		{
			// No responses - just end with the message
			npcEndConversationWithMessage(player, string_id.unlocalized(npcMessage));
			return SCRIPT_CONTINUE;
		}

		utils.setScriptVar(player, conversation + ".branchId", branchId);

		// Build string_id array for responses
		string_id[] responseIds = new string_id[responses.length];
		for (int i = 0; i < responses.length; i++)
		{
			responseIds[i] = responses[i].toStringId();
		}

		// Start conversation with unlocalized NPC message
		string_id greeting = string_id.unlocalized(npcMessage);
		npcStartConversation(player, self, scriptName, greeting, responseIds);
		return SCRIPT_CONTINUE;
	}

	/**
	 * Continues a server-side conversation with new NPC text and response options.
	 *
	 * @param player     The player in the conversation
	 * @param npcMessage What the NPC says to the player
	 * @param branchId   Branch ID for tracking conversation state
	 * @param responses  Array of ConvoResponse objects (id + display text)
	 * @return SCRIPT_CONTINUE
	 */
	protected int serverSide_respond(obj_id player, String npcMessage, int branchId, ConvoResponse[] responses) throws InterruptedException
	{
		if (responses == null || responses.length == 0)
		{
			// No responses - end conversation with message
			utils.removeScriptVar(player, conversation + ".branchId");
			npcEndConversationWithMessage(player, string_id.unlocalized(npcMessage));
			return SCRIPT_CONTINUE;
		}

		utils.setScriptVar(player, conversation + ".branchId", branchId);

		// Speak the NPC message
		npcSpeak(player, string_id.unlocalized(npcMessage));

		// Build string_id array for responses
		string_id[] responseIds = new string_id[responses.length];
		for (int i = 0; i < responses.length; i++)
		{
			responseIds[i] = responses[i].toStringId();
		}

		npcSetConversationResponses(player, responseIds);
		return SCRIPT_CONTINUE;
	}

	/**
	 * Ends a server-side conversation with a final NPC message.
	 *
	 * @param player     The player in the conversation
	 * @param npcMessage Final message from the NPC
	 * @return SCRIPT_CONTINUE
	 */
	protected int serverSide_endConversation(obj_id player, String npcMessage) throws InterruptedException
	{
		utils.removeScriptVar(player, conversation + ".branchId");
		npcEndConversationWithMessage(player, string_id.unlocalized(npcMessage));
		return SCRIPT_CONTINUE;
	}

	/**
	 * Checks if a response matches a given ConvoResponse. Use this in OnNpcConversationResponse
	 * for server-side conversations.
	 *
	 * @param response     The response string_id from OnNpcConversationResponse
	 * @param convoResponse The ConvoResponse to check against
	 * @return true if the response matches the given ConvoResponse
	 */
	protected boolean responseIs(string_id response, ConvoResponse convoResponse)
	{
		if (response == null || convoResponse == null)
		{
			return false;
		}
		return response.equals(convoResponse.getMatchString());
	}

	/**
	 * Checks if a response matches a given response ID. Use this in OnNpcConversationResponse
	 * for server-side conversations. This method extracts just the ID portion for matching.
	 *
	 * @param response   The response string_id from OnNpcConversationResponse
	 * @param responseId The response ID to check against
	 * @return true if the response's ID matches the given responseId
	 */
	protected boolean responseIdIs(string_id response, String responseId)
	{
		if (response == null || responseId == null)
		{
			return false;
		}
		return response.getConvoResponseId().equals(responseId);
	}
}
