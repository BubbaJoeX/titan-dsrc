package script.library;

import script.*;
import script.dictionary;
import script.obj_id;

/**
 * Character Transfer (CTS) snapshot helpers — same pack/unpack path as upload/download:
 * {@code OnUploadCharacter} → {@link dictionary#pack()} / {@link dictionary#unpack(byte[])} → {@code OnDownloadCharacter}.
 * Not lore cloning; this is the transfer-pipeline binary snapshot.
 */
public class bot_lib extends script.base_script
{
    /**
     * Build the packed byte[] the transfer server expects (same as {@code OnStartCharacterUpload}).
     *
     * @param sourcePlayer  character to read (inventory, skills, objvars, etc.)
     * @param withItems     whether to pack contained items (match CTS “with items”)
     * @param allowOverride CTS pack allowOverride flag (helps edge cases during dev/bot apply)
     * @return packed data, or null if {@code OnUploadCharacter} did not return SCRIPT_CONTINUE
     */
    public static byte[] packCharacterTransferData(obj_id sourcePlayer, boolean withItems, boolean allowOverride) throws InterruptedException
    {
        if (!isIdValid(sourcePlayer) || !isPlayer(sourcePlayer))
        {
            return null;
        }
        dictionary characterData = new dictionary();
        characterData.put("withItems", withItems);
        characterData.put("allowOverride", allowOverride);
        Object[] triggerParams = new Object[2];
        triggerParams[0] = sourcePlayer;
        triggerParams[1] = characterData;
        int err = script_entry.runScripts("OnUploadCharacter", triggerParams);
        if (err != SCRIPT_CONTINUE)
        {
            return null;
        }
        return characterData.pack();
    }

    /**
     * Apply a CTS snapshot (same entry as transfer download). Removes {@code hasTransferred} first so
     * you can re-apply when iterating on a bot account.
     */
    public static boolean applyCharacterTransferData(obj_id targetPlayer, byte[] packedData) throws InterruptedException
    {
        if (!isIdValid(targetPlayer) || !isPlayer(targetPlayer) || packedData == null || packedData.length == 0)
        {
            return false;
        }
        if (hasObjVar(targetPlayer, "hasTransferred"))
        {
            removeObjVar(targetPlayer, "hasTransferred");
        }
        Object[] triggerParams = new Object[2];
        triggerParams[0] = targetPlayer;
        triggerParams[1] = packedData;
        int err = script_entry.runScripts("OnDownloadCharacter", triggerParams);
        return err == SCRIPT_CONTINUE;
    }

    private bot_lib()
    {
    }
}
