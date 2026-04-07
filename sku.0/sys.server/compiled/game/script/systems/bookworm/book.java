package script.systems.bookworm;

import script.*;
import script.library.buff;
import script.library.gm;
import script.library.sui;
//import script.library.oracle;
import script.library.utils;

import java.sql.*;

public class book extends base_script
{
    /*public static boolean enableBookwormBuff = false;

    public int OnAttach(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self)
    {
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(menu_info_types.ITEM_USE, unlocalized("Open Book"));
        if (getCrafter(self) == player) //only the creator of the book can edit it.
        {
            mi.addRootMenu(menu_info_types.SERVER_MENU2, unlocalized("Name Book"));
            mi.addRootMenu(menu_info_types.SERVER_MENU3, unlocalized("Describe Book"));
        }
        if (isGod(player))
        {
            int gmRoot = mi.addRootMenu(menu_info_types.SERVER_MENU40, gm.SID_RADIAL_GM_ROOT);
            mi.addSubMenu(gmRoot, menu_info_types.SERVER_MENU4, unlocalized("Claim Book"));
        }
        return SCRIPT_CONTINUE;
    }

    private string_id unlocalized(String text)
    {
        return new string_id(text);
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int menu) throws InterruptedException
    {
        if (menu == menu_info_types.ITEM_USE)
        {
            openBook(self, player);
        }
        if (menu == menu_info_types.SERVER_MENU2)
        {
            sui.inputbox(self, player, "Enter a name for your book.", "handleName");
        }
        if (menu == menu_info_types.SERVER_MENU3)
        {
            sui.inputbox(self, player, "Enter a description for your book.", "handleDescribe");
        }
        if (menu == menu_info_types.SERVER_MENU4)
        {
            setCrafter(self, player);
            LOG("ethereal", "[Bookworm]: GM " + getPlayerFullName(player) + " forcefully claimed book " + self);
        }
        return SCRIPT_CONTINUE;
    }

    public int openBook(obj_id book, obj_id who) throws InterruptedException
    {
        int page = createSUIPage("/Script.editScript", book, who);

        // Retrieve book data from database
        String bookText = getBookTextFromDatabase(book);
        String bookTitle = getBookTitleFromDatabase(book);

        setSUIProperty(page, "pageText.text", "Text", bookText);
        setSUIProperty(page, "pageText.text", "Font", "bold_22");
        if (getCrafter(book) == who)
        {
            setSUIProperty(page, "pageText.text", "Editable", "True");
        }
        else
        {
            setSUIProperty(page, "pageText.text", "Editable", "False");
        }
        setSUIProperty(page, "pageText.text", "GetsInput", "True"); // allow copy and pasting.
        setSUIProperty(page, "outputPage.text", "Text", bookTitle);//title
        setSUIProperty(page, "btnOk", "Text", "Save");//save button
        if (getCrafter(book) == who)
        {
            setSUIProperty(page, "btnOk", "Text", "Save");//save button
            setSUIProperty(page, "bg.caption.text", "LocalText", "Edit Book");
            subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnOk", "saveText");
        }
        else
        {
            setSUIProperty(page, "btnOk", "Text", "Close");//close button actually
            setSUIProperty(page, "bg.caption.text", "LocalText", "View Book");
            subscribeToSUIEvent(page, sui_event_type.SET_onButton, "btnOk", "readText");
        }

        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnOk", "pageText.text", "LocalText");
        subscribeToSUIPropertyForEvent(page, sui_event_type.SET_onButton, "btnOk", "outputPage.text", "LocalText");
        setSUIAssociatedObject(page, book);
        showSUIPage(page);
        flushSUIPage(page);
        if (!utils.hasScriptVar(who, "pageId"))
        {
            utils.setScriptVar(book, "pageId", page);
        }
        setObjVar(book, "bookPage", page);

        return SCRIPT_OVERRIDE;
    }

    public int saveText(obj_id self, dictionary params) throws InterruptedException
    {
        String bookText = params.getString("pageText.text.LocalText");
        obj_id player = sui.getPlayerId(params);
        if (bookText.length() < 2000)
        {
            saveBookTextToDatabase(self, bookText);
            broadcast(player, "You have modified this text within this book.");
        }
        else
        {
            broadcast(player, "The maximum word count for this book is 2000 characters.");
        }
        sui.closeSUI(player, getIntObjVar(self, "bookPage"));
        LOG("ethereal", "[Bookworm]: " + player + " has saved book " + self + " with text: " + bookText.length() + " characters.");
        return SCRIPT_CONTINUE;
    }

    public int readText(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(params);
        if (enableBookwormBuff)
        {
            if (!buff.hasBuff(player, "content_bookworm"))
            {
                buff.applyBuff(player, "content_bookworm");
                playClientEffectLoc(player, "clienteffect/of_scatter.cef", getLocation(player), 1.0f);
                broadcast(player, "You have read the contents of this book, and have gained the Bookworm buff.");
            }
            else
            {
                broadcast(player, "You had read the contents of this book.");
            }
        }
        else
        {
            broadcast(player, "You had read the contents of this book.");
            sui.closeSUI(player, getIntObjVar(self, "bookPage"));
        }
        return SCRIPT_CONTINUE;
    }

    public int handleName(obj_id self, dictionary paramsDict) throws InterruptedException
    {
        obj_id player = sui.getPlayerId(paramsDict);
        String bookTitle = sui.getInputBoxText(paramsDict);
        setName(self, bookTitle);
        setObjVar(self, "book.title", bookTitle);

        // Save the new book name to the database
        saveBookTitleToDatabase(self, bookTitle);

        broadcast(player, "You have renamed this book to: " + bookTitle);
        return SCRIPT_CONTINUE;
    }

    public int handleDescribe(obj_id self, dictionary paramsDict) throws InterruptedException
    {
        String descInput = sui.getInputBoxText(paramsDict);
        if (descInput == null || descInput.isEmpty())
        {
            return SCRIPT_CONTINUE;
        }

        // Save the description to the database
        saveBookDescriptionToDatabase(self, descInput);

        string_id desc = new string_id(descInput);
        setDescriptionStringId(self, desc);
        setObjVar(self, "null_desc", descInput);
        if (!hasScript(self, "developer.bubbajoe.sync"))
        {
            attachScript(self, "developer.bubbajoe.sync");
        }
        return SCRIPT_CONTINUE;
    }

    private void saveBookTextToDatabase(obj_id book, String bookText) throws InterruptedException
    {
        try (Connection conn = oracle.connect()) {
            String checkQuery = "SELECT COUNT(*) FROM books WHERE book_tag = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, getBookTag(book));
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Book exists, update text
                        String updateQuery = "UPDATE books SET book_text = ? WHERE book_tag = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                            stmt.setString(1, bookText);
                            stmt.setString(2, getBookTag(book));
                            stmt.executeUpdate();
                        }
                    } else {
                        // Book doesn't exist, insert new record
                        String insertQuery = "INSERT INTO books (book_tag, book_text) VALUES (?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                            stmt.setString(1, getBookTag(book));
                            stmt.setString(2, bookText);
                            stmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG("ethereal", "Error saving book text to database: " + e.getMessage());
        }
    }

    // Save Book Title to Database (Update or Insert)
    private void saveBookTitleToDatabase(obj_id book, String title) throws InterruptedException
    {
        try (Connection conn = oracle.connect()) {
            String checkQuery = "SELECT COUNT(*) FROM books WHERE book_tag = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, getBookTag(book));
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Book exists, update title
                        String updateQuery = "UPDATE books SET book_name = ? WHERE book_tag = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                            stmt.setString(1, title);
                            stmt.setString(2, getBookTag(book));
                            stmt.executeUpdate();
                        }
                    } else {
                        // Book doesn't exist, insert new record
                        String insertQuery = "INSERT INTO books (book_tag, book_name) VALUES (?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                            stmt.setString(1, getBookTag(book));
                            stmt.setString(2, title);
                            stmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG("ethereal", "Error saving book title to database: " + e.getMessage());
        }
    }

    // Save Book Description to Database (Update or Insert)
    private void saveBookDescriptionToDatabase(obj_id book, String description) throws InterruptedException
    {
        try (Connection conn = oracle.connect()) {
            String checkQuery = "SELECT COUNT(*) FROM books WHERE book_tag = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, getBookTag(book));
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Book exists, update description
                        String updateQuery = "UPDATE books SET book_description = ? WHERE book_tag = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                            stmt.setString(1, description);
                            stmt.setString(2, getBookTag(book));
                            stmt.executeUpdate();
                        }
                    } else {
                        // Book doesn't exist, insert new record
                        String insertQuery = "INSERT INTO books (book_tag, book_description) VALUES (?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                            stmt.setString(1, getBookTag(book));
                            stmt.setString(2, description);
                            stmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG("ethereal", "Error saving book description to database: " + e.getMessage());
        }
    }


    private String getBookTextFromDatabase(obj_id book) throws InterruptedException
    {
        try (Connection conn = oracle.connect()) {
            String query = "SELECT book_text FROM books WHERE book_tag = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, getBookTag(book));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("book_text");
                    }
                }
            }
        } catch (SQLException e) {
            LOG("ethereal", "Error retrieving book text from database: " + e.getMessage());
        }
        return "";
    }

    private String getBookTitleFromDatabase(obj_id book) throws InterruptedException
    {
        try (Connection conn = oracle.connect()) {
            String query = "SELECT book_name FROM books WHERE book_tag = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, getBookTag(book));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("book_name");
                    }
                }
            }
        } catch (SQLException e) {
            LOG("ethereal", "Error retrieving book title from database: " + e.getMessage());
        }
        return "";
    }

    private String getBookTag(obj_id book)
    {
        //Assuming the book's tag is its obj_id for database tracking
        return book.toString();
    }*/
}
