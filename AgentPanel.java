import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgentPanel {

    private static final String[] OPTIONS = {
        "My Properties",
        "My Sales",
        "My Rentals",
        "My Performance Summary",
        "My Profile",
        "Exit"
    };


    public static void run(Screen screen, int userId) throws Exception {
        String agentName = resolveAgentName(userId);
        int sel = 0;
        boolean running = true;
        while (running) {
            drawMenu(screen, sel, agentName);
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp)
                sel = (sel > 0) ? sel - 1 : OPTIONS.length - 1;
            else if (key.getKeyType() == KeyType.ArrowDown)
                sel = (sel < OPTIONS.length - 1) ? sel + 1 : 0;
            else if (key.getKeyType() == KeyType.Enter) {
                switch (sel) {
                    case 0 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT property_id, title, listing_type, price, status, city, locality_name, bhk, area_sqft " +
                            "FROM property WHERE agentID = " + userId + " ORDER BY status, city",
                            "My Properties");

                    case 1 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT t.transactionId, p.title, t.transactionDate, t.transactionAmount " +
                            "FROM transactions t JOIN property p ON t.propertyId = p.property_id " +
                            "WHERE t.agentId = " + userId + " AND t.transactionType = 'sale' " +
                            "ORDER BY t.transactionDate DESC",
                            "My Sales");

                    case 2 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT t.transactionId, p.title, t.transactionDate, t.transactionAmount " +
                            "FROM transactions t JOIN property p ON t.propertyId = p.property_id " +
                            "WHERE t.agentId = " + userId + " AND t.transactionType = 'rent' " +
                            "ORDER BY t.transactionDate DESC",
                            "My Rentals");

                    case 3 -> showPerformanceSummary(screen, userId, agentName);

                    case 4 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT u.userId, u.name, u.email, u.phoneNumber, " +
                            "a.experienceYears, a.salary, a.rating, " +
                            "(SELECT COUNT(*) FROM transactions WHERE agentId = a.userId AND transactionType = 'sale') AS dealCount, " +
                            "(SELECT COUNT(*) FROM transactions WHERE agentId = a.userId AND transactionType = 'rent') AS rentCount " +
                            "FROM agent a JOIN users u ON a.userId = u.userId WHERE a.userId = " + userId,
                            "My Profile");

                    case 5 -> running = false;
                }
            }
        }
    }

    private static void drawMenu(Screen screen, int sel, String agentName) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.MAGENTA);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(2, 1, "APEX ESTATES — AGENT PANEL");
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(2, 2, "Logged in as: " + agentName);
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 3, "UP/DOWN to move  |  ENTER to select");
        for (int i = 0; i < OPTIONS.length; i++) {
            int row = 5 + i;
            if (i == sel) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.MAGENTA);
                tg.putString(2, row, " > " + OPTIONS[i] + " ");
            } else {
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(2, row, "   " + OPTIONS[i]);
            }
        }
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
        screen.refresh();
    }

   

    private static void showPerformanceSummary(Screen screen, int userId, String agentName) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("--- Performance Summary for " + agentName + " ---");
        lines.add("");
        try (Connection con = DBAdmin.getConnection(); Statement st = con.createStatement()) {
            ResultSet rs1 = st.executeQuery(
                "SELECT COUNT(*) AS num_sales, COALESCE(SUM(transactionAmount), 0) AS total_sale_value " +
                "FROM transactions WHERE agentId = " + userId + " AND transactionType = 'sale'");
            lines.addAll(resultSetToLines(rs1)); rs1.close();
            lines.add("");
            ResultSet rs2 = st.executeQuery(
                "SELECT COUNT(*) AS num_rentals, COALESCE(SUM(transactionAmount), 0) AS total_rent_collected " +
                "FROM transactions WHERE agentId = " + userId + " AND transactionType = 'rent'");
            lines.addAll(resultSetToLines(rs2)); rs2.close();
            lines.add("");
            ResultSet rs3 = st.executeQuery(
                "SELECT COUNT(*) AS active_listings FROM property " +
                "WHERE agentID = " + userId + " AND status = 'available'");
            lines.addAll(resultSetToLines(rs3)); rs3.close();
        } catch (SQLException e) {
            lines.add("SQL Error: " + e.getMessage());
        }
        showScrollableResult(screen, lines);
    }
    // ─── Add New Property ────────────────────────────────────────────────────

    private static void addNewProperty(Screen screen, int agentUserId) throws Exception 

    {
        String sellerIdStr  = readLine(screen, "Seller userId:", 6);            if (sellerIdStr == null) return;
        String title        = readLine(screen, "Title:", 6);                    if (title       == null) return;
        String propType     = pickOption(screen, "Property Type",
                                new String[]{"villa","flat","apartment","bungalow","mansion","duplex","triplex","house"});
                                                                                 if (propType    == null) return;
        String listingType  = pickOption(screen, "Listing Type", new String[]{"rent","sale"});
                                                                                 if (listingType == null) return;
        String price        = readLine(screen, "Price (Rs.):", 6);              if (price       == null) return;
        String areaSqft     = readLine(screen, "Area (sqft):", 6);              if (areaSqft    == null) return;
        String bhk          = readLine(screen, "BHK (e.g. 2 or 2.5):", 6);      if (bhk         == null) return;
        String houseNo      = readLine(screen, "House No.:", 6);                if (houseNo     == null) return;
        String houseName    = readLine(screen, "House Name (blank=none):", 6);  if (houseName   == null) return;
        String locality     = readLine(screen, "Locality Name:", 6);            if (locality    == null) return;
        String city         = readLine(screen, "City:", 6);                     if (city        == null) return;
        String pincode      = readLine(screen, "Pincode (6 digits):", 6);       if (pincode     == null) return;
        String yearBuilt    = readLine(screen, "Year Built:", 6);               if (yearBuilt   == null) return;

        try (Connection con = DBAdmin.getConnection()) 
        {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO property(agentID, sellerID, title, property_type, listing_type, price, " +
                "area_sqft, bhk, house_no, house_name, locality_name, city, pincode, year_built) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setInt(1, agentUserId);
            ps.setInt(2, Integer.parseInt(sellerIdStr));
            ps.setString(3, title);
            ps.setString(4, propType);
            ps.setString(5, listingType);
            ps.setInt(6, Integer.parseInt(price));
            ps.setBigDecimal(7, new java.math.BigDecimal(areaSqft));
            ps.setBigDecimal(8, new java.math.BigDecimal(bhk));
            ps.setString(9, houseNo);
            ps.setString(10, houseName.isEmpty() ? null : houseName);
            ps.setString(11, locality);
            ps.setString(12, city);
            ps.setString(13, pincode);
            ps.setInt(14, Integer.parseInt(yearBuilt));
            ps.executeUpdate();
            showMessage(screen, "Property listing added successfully!  (Press any key)");
        } 
        catch (Exception e) 
        {
            showMessage(screen, "Error: " + e.getMessage() + "  (Press any key)");
        }
    }


    //Lanterna input helpers
    private static String readLine(Screen screen, String prompt, int row) throws Exception 
    {
        StringBuilder input = new StringBuilder();
        while (true) 
        {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, row - 2, prompt);
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.putString(2, row, "> " + input + "_");
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, row + 2, "ENTER to confirm  |  ESC to cancel");
            screen.refresh();
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Escape)    return null;
            if (key.getKeyType() == KeyType.Enter)     return input.toString().trim();
            if (key.getKeyType() == KeyType.Backspace && input.length() > 0)
                input.deleteCharAt(input.length() - 1);
            else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null)
                input.append(key.getCharacter());
        }
    }

    private static String pickOption(Screen screen, String title, String[] options) throws Exception {
        int sel = 0;
        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, 1, title);
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, 2, "UP/DOWN  |  ENTER to confirm  |  ESC to cancel");
            for (int i = 0; i < options.length; i++) {
                if (i == sel) {
                    tg.setForegroundColor(TextColor.ANSI.BLACK);
                    tg.setBackgroundColor(TextColor.ANSI.MAGENTA);
                    tg.putString(4, 4 + i, " > " + options[i] + " ");
                } else {
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                    tg.putString(4, 4 + i, "   " + options[i]);
                }
            }
            tg.setForegroundColor(TextColor.ANSI.DEFAULT);
            tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
            screen.refresh();
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Escape)    return null;
            if (key.getKeyType() == KeyType.Enter)     return options[sel];
            if (key.getKeyType() == KeyType.ArrowUp)   sel = (sel > 0) ? sel - 1 : options.length - 1;
            if (key.getKeyType() == KeyType.ArrowDown) sel = (sel < options.length - 1) ? sel + 1 : 0;
        }
    }



    private static List<String> resultSetToLines(ResultSet rs) throws SQLException {
        List<String> lines = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        int[] widths = new int[cols];
        for (int i = 1; i <= cols; i++) widths[i-1] = meta.getColumnName(i).length();
        List<String[]> dataRows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 1; i <= cols; i++) {
                String v = rs.getString(i); row[i-1] = (v == null) ? "NULL" : v;
                widths[i-1] = Math.max(widths[i-1], row[i-1].length());
            }
            dataRows.add(row);
        }
        StringBuilder header = new StringBuilder(), divider = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            String fmt = "%-" + (widths[i-1] + 2) + "s";
            header.append(String.format(fmt, meta.getColumnName(i)));
            divider.append("-".repeat(widths[i-1] + 2));
        }
        lines.add(header.toString()); lines.add(divider.toString());
        for (String[] dr : dataRows) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < cols; i++) row.append(String.format("%-" + (widths[i] + 2) + "s", dr[i]));
            lines.add(row.toString());
        }
        if (dataRows.isEmpty()) lines.add("(No records found)");
        return lines;
    }

    //helps to scroll (left ,right) and (up ,down)
    private static void showScrollableResult(Screen screen, List<String> lines) throws Exception {
        int offset = 0, hOffset = 0;
        int maxVisible = screen.getTerminalSize().getRows() - 4;
        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            int displayWidth = screen.getTerminalSize().getColumns() - 4;
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, 0, "UP/DOWN: scroll  |  LEFT/RIGHT: pan  |  Q/ESC: back");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            for (int i = 0; i < maxVisible; i++) {
                int idx = offset + i;
                if (idx < lines.size()) {
                    String text = lines.get(idx);
                    int start = Math.min(hOffset, text.length());
                    String slice = text.substring(start);
                    if (slice.length() > displayWidth) slice = slice.substring(0, displayWidth);
                    tg.putString(2, i + 2, slice);
                }
            }
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, screen.getTerminalSize().getRows() - 1,
                "Row " + (offset + 1) + "/" + lines.size() + "  Col >" + hOffset);
            screen.refresh();
            KeyStroke k = screen.readInput();
            if (k.getKeyType() == KeyType.ArrowDown) { if (offset + maxVisible < lines.size()) offset++; }
            else if (k.getKeyType() == KeyType.ArrowUp)    { if (offset > 0) offset--; }
            else if (k.getKeyType() == KeyType.ArrowRight) { hOffset += 8; }
            else if (k.getKeyType() == KeyType.ArrowLeft)  { if (hOffset > 0) hOffset = Math.max(0, hOffset - 8); }
            else if (k.getKeyType() == KeyType.Escape) break;
            else if (k.getKeyType() == KeyType.Character) {
                char c = k.getCharacter();
                if (c == 'q' || c == 'Q') break;
            }
        }
    }

    private static void showMessage(Screen screen, String message) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(2, 2, message);
        screen.refresh();
        screen.readInput();
    }

    private static String resolveAgentName(int userId) {
        try (Connection con = DBAdmin.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM users WHERE userId = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException ignored) {}
        return "Agent #" + userId;
    }
}