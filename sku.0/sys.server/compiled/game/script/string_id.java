/*
 Title:        string_id
 Description:  Wrapper for a string table id.
 */

package script;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class string_id implements Comparable, Serializable
{
    private final static long serialVersionUID = -1331982663286942264L;

    // A string_id is represented by a table name, and either an ascii text id
    // or an integer index id. The ascii text id is the default id to use; the
    // index id will only be used if the ascii id is 0 length or null
    //
    protected String m_table = "";        // table/file of string id
    protected transient String m_asciiId = "";    // english ascii text for string id
    protected transient int m_indexId = -1;        // index for string id

    public string_id()
    {
    }

    /**
     * Class constructor.
     *
     * @param table
     *         the string table
     * @param id
     *         the string table id
     */
    public string_id(String table, String id)
    {
        if (table != null)
            m_table = table.toLowerCase();
        else
            m_table = "";
        if (id != null)
            m_asciiId = id.toLowerCase();
        else
            m_asciiId = "";
    }    // string_id

    /**
     * Class constructor.
     *
     * @param table
     *         the string table
     * @param id
     *         the string table id
     */
    public string_id(String table, int id)
    {
        if (table != null)
            m_table = table.toLowerCase();
        else
            m_table = "";
        m_indexId = id;
    }    // string_id

    /**
     * Copy constructor.
     *
     * @param src
     *         class instance to copy
     */
    public string_id(string_id src)
    {
        m_table = src.m_table;
        m_indexId = src.m_indexId;
        m_asciiId = src.m_asciiId;
    }    // string_id(string_id)

    /**
     * Overloaded version of string_id constructor that uses a dummy string not a localized one
     * Allows forcing of inline-defined Strings in lieu of creating strings in .STF where a string_id is expected by the code
     * Can be used directly as a string_id or pushed into a prose_package
     * This is predominately intended for use in admin/debug scenarios where adding a real string does not make sense
     * However, this seemingly works perfectly fine for production use provided that you never intend to localize your client in another language
     * Good for radial menu items, comm messages, and all those other annoying functions that want a string_id
     * You should NOT use this for conversations, though, as conversation logic relies on STF indexing
     * <p>
     * Example Usage: string_id message = new string_id("a message you don't want to put into a .STF here");
     *
     * @param dummyText
     *         String text to display in the client
     */
    public string_id(String dummyText)
    {
        m_table = "dummy_string_table"; // client relies on this name to not parse as a string_id
        m_asciiId = dummyText;
    } // string_id(dummyText)

    /**
     * Creates an unlocalized string_id for server-side conversation responses.
     * The format is "responseId|displayText" where:
     * - responseId is used for matching in OnNpcConversationResponse (use response.equals("responseId|displayText"))
     * - displayText is what the player sees
     * <p>
     * The client extracts and displays only the displayText portion.
     * <p>
     * Example Usage: string_id.convoResponse("accept", "Yes, I'll help you!")
     *   - Client displays: "Yes, I'll help you!"
     *   - Script matches: response.equals("accept|Yes, I'll help you!")
     *
     * @param responseId  Unique identifier for this response (used for matching)
     * @param displayText Text to display to the player
     * @return A string_id suitable for server-side conversation responses
     */
    public static string_id convoResponse(String responseId, String displayText)
    {
        string_id sid = new string_id();
        sid.m_table = "convo_response";
        sid.m_asciiId = responseId + "|" + displayText;
        return sid;
    }

    /**
     * Checks if this string_id is a server-side conversation response.
     *
     * @return true if this is a conversation response created via convoResponse()
     */
    public boolean isConvoResponse()
    {
        return "convo_response".equals(m_table);
    }

    /**
     * Gets the response ID from a server-side conversation response.
     * Returns the full asciiId if this is not a convo_response.
     *
     * @return The response ID portion (before the pipe)
     */
    public String getConvoResponseId()
    {
        if (!isConvoResponse())
        {
            return m_asciiId;
        }
        int pipePos = m_asciiId.indexOf('|');
        if (pipePos > 0)
        {
            return m_asciiId.substring(0, pipePos);
        }
        return m_asciiId;
    }

    /**
     * Accessor function.
     *
     * @return the string table
     */
    public String getTable()
    {
        return m_table;
    }    // getTable

    /**
     * Accessor function.
     *
     * @return the ascii id
     */
    public String getAsciiId()
    {
        return m_asciiId;
    }    // getStringId

    /**
     * Accessor function.
     *
     * @return the index id
     */
    public int getIndexId()
    {
        return m_indexId;
    }    // getIndexId

    /**
     *
     */
    public boolean isValid()
    {
        return m_table != null && m_table.length() > 0 && m_asciiId != null && m_asciiId.length() > 0;
    }

    /**
     *
     */
    public boolean isEmpty()
    {
        return (m_table == null || m_table.length() == 0) && (m_asciiId == null || m_asciiId.length() == 0);
    }

    /**
     * Conversion function.
     *
     * @return the id as a string.
     */
    public String toString()
    {
        StringBuilder sbuf = new StringBuilder(m_table);
        sbuf.append(':');

        if (m_asciiId != null)
            sbuf.append(m_asciiId);
        else
            sbuf.append(m_indexId);

        return sbuf.toString();
    }

    /**
     * Compares this to a generic object.
     *
     * @returns <, =, or > 0 if the object is a string_id, else throws
     *         ClassCastException
     */
    public int compareTo(Object o) throws ClassCastException
    {
        return compareTo((string_id) o);
    }    // compareTo(Object)

    /**
     * Compares this to another string_id.
     *
     * @returns <, =, or > 0
     */
    public int compareTo(string_id id)
    {
        int result = m_table.compareTo(id.m_table);
        if (result == 0)
        {
            if (m_asciiId != null && m_asciiId.length() != 0)
                result = m_asciiId.compareTo(id.m_asciiId);
            else
                result = m_indexId - id.m_indexId;
        }
        return result;
    }    // compareTo(string_id)

    /**
     * Compares this to a generic object.
     *
     * @returns true if the objects have the same data, false if not
     */
    public boolean equals(String o)
    {
        return m_asciiId.equals(o);
    }

    public boolean equals(string_id o)
    {
        if (m_table.equals(o.m_table))
        {
            if (m_asciiId != null && m_asciiId.length() != 0)
            {
                return m_asciiId.equals(o.m_asciiId);
            }
            else return m_indexId == o.m_indexId;
        }
        return false;
    }
    // equals

    /**
     * \defgroup serialize Serialize support functions
     *
     * @{
     */

    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        if (m_asciiId.length() > 0)
        {
            out.writeBoolean(true);
            out.writeObject(m_asciiId);
        }
        else
        {
            out.writeBoolean(false);
            out.writeInt(m_indexId);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        boolean isTextId = in.readBoolean();
        if (isTextId)
        {
            m_asciiId = (String) in.readObject();
        }
        else
        {
            m_indexId = in.readInt();
        }
    }


    public static string_id unlocalized(String input)
    {
        return new string_id(input);
    }

    /*@}*/

}    // class string_id
