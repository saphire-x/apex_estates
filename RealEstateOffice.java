import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RealEstateOffice {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "rootkK@123";

    private static final String[] MENU_OPTIONS = {
            "Add User (Buyer / Seller / Agent)",
            "Sales Reports (All Agents)",
            "Rental Analytics (All Agents)",
            "Agent Performance",
            "Houses built after 2023 for rent",
            "Houses priced Rs.20L - Rs.60L",
            "Houses for rent in a locality",
            "View available properties",
            "Sales report by year",
            "Mark Property as SOLD",
            "Mark Property as RENTED",
            "Exit"
    };

    // Entry points
    public static void main(String[] args) {
        Screen screen = null;
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            runOfficeMenu(screen);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (screen != null)
                    screen.stopScreen();
            } catch (Exception ignored) {
            }
        }
    }

    public static void runOfficeMenu(Screen screen) throws Exception {
        int sel = 0;
        boolean running = true;

        while (running) {
            drawMenu(screen, sel);
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp)
                sel = (sel > 0) ? sel - 1 : MENU_OPTIONS.length - 1;
            else if (key.getKeyType() == KeyType.ArrowDown)
                sel = (sel < MENU_OPTIONS.length - 1) ? sel + 1 : 0;
            else if (key.getKeyType() == KeyType.Enter) {
                switch (sel) {
                    case 0 -> addUser(screen);
                    case 1 -> salesReportAllAgents(screen);
                    case 2 -> rentalAnalytics(screen);
                    case 3 -> agentPerformance(screen);
                    case 4 -> showHousesBuiltAfter2023ForRent(screen);
                    case 5 -> showHousesByPriceRange(screen);
                    case 6 -> showHousesForRentByLocality(screen);
                    case 7 -> viewAvailableProperties(screen);
                    case 8 -> salesReportByYear(screen);
                    case 9 -> markPropertyAsSold(screen);
                    case 10 -> markPropertyAsRented(screen);
                    case 11 -> running = false;
                }
            }
        }
    }

    private static void drawMenu(Screen screen, int sel) throws Exception {
        screen.clear();// clears the screen
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(2, 0, "==========================================");
        tg.putString(2, 1, "   REAL ESTATE OFFICE - STAFF PANEL       ");
        tg.putString(2, 2, "==========================================");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 3, "UP/DOWN to move, ENTER to select");
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            if (i == sel) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.GREEN);
                tg.putString(2, 5 + i, " > " + MENU_OPTIONS[i] + "   ");
            } else {
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(2, 5 + i, "   " + MENU_OPTIONS[i]);
            }
        }
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
        screen.refresh();
    }

    // Add user (buyer / seller / agent)
    private static void addUser(Screen screen) throws Exception {

        // all user details
        String name = readLine(screen, "Full Name:", 6);
        if (name == null)
            return;
        String email = readLine(screen, "Email:", 6);
        if (email == null)
            return;
        String phone = readLine(screen, "Phone (10 digits):", 6);
        if (phone == null)
            return;
        String pass = readLine(screen, "Password:", 6);
        if (pass == null)
            return;

        // role selection
        String role = pickOption(screen, "Select Role", new String[] { "Buyer", "Seller", "Agent" });
        if (role == null)
            return;

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                // insert in users table
                PreparedStatement psU = con.prepareStatement(
                        "INSERT INTO users(name, email, phoneNumber, passwordHash) VALUES(?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS);
                psU.setString(1, name);
                psU.setString(2, email);
                psU.setString(3, phone.isEmpty() ? null : phone);
                psU.setString(4, sha256(pass));
                psU.executeUpdate();
                ResultSet gk = psU.getGeneratedKeys();
                gk.next();
                int userId = gk.getInt(1);
                gk.close();

                switch (role) {
                    case "Buyer" -> insertBuyer(screen, con, userId);
                    case "Seller" -> insertSeller(screen, con, userId);
                    case "Agent" -> insertAgent(screen, con, userId);
                }

                con.commit();
                showMessage(screen, "User added! userId = " + userId + "  (Press any key)");
            } catch (CancelException e) {
                con.rollback();
                showMessage(screen, "Cancelled. No changes saved.  (Press any key)");
            } catch (Exception e) {
                con.rollback();
                showMessage(screen, "Error: " + e.getMessage() + "  (Press any key)");
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static void insertBuyer(Screen screen, Connection con, int userId) throws Exception {
        String aadhar = req(readLine(screen, "Aadhar Number (12 digits):", 6));
        String min_budget = req(readLine(screen, "Budget Min (Rs):", 6));
        String max_budget = req(readLine(screen, "Budget Max (Rs):", 6));
        String loc = req(readLine(screen, "Preferred Locality:", 6));
        String ptype = req(readLine(screen, "Property Type Preference:", 6));
        String bhk = req(readLine(screen, "BHK Need (e.g. 2 or 2.5):", 6));

        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO property_seeker(userId, aadharNumber, min_budget, max_budget, "
                        + "preferredLocality, propertyTypePreference, bhkNeed) VALUES(?,?,?,?,?,?,?)");
        ps.setInt(1, userId);
        ps.setString(2, aadhar);
        ps.setInt(3, Integer.parseInt(min_budget));
        ps.setInt(4, Integer.parseInt(max_budget));
        ps.setString(5, loc);
        ps.setString(6, ptype);
        ps.setBigDecimal(7, new BigDecimal(bhk));
        ps.executeUpdate();
    }

    private static void insertSeller(Screen screen, Connection con, int userId) throws Exception {
        PreparedStatement psBase = con.prepareStatement(
                "INSERT INTO seller(userId) VALUES(?)");
        psBase.setInt(1, userId);
        psBase.executeUpdate();

        String stype = pickOption(screen, "Seller Type", new String[] { "Individual", "Organisation" });
        if (stype == null)
            throw new CancelException();

        if (stype.equals("Individual")) {
            String aadhar = req(readLine(screen, "Aadhar Number (12 digits):", 6));
            String pan = req(readLine(screen, "PAN Number (10 chars):", 6));
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO indSeller(userId, aadharNumber, panNumber) VALUES(?,?,?)");
            ps.setInt(1, userId);
            ps.setString(2, aadhar);
            ps.setString(3, pan);
            ps.executeUpdate();
        }

        else {// organisation details taking input
            String orgName = req(readLine(screen, "Organisation Name:", 6));
            String pan = req(readLine(screen, "PAN Number (10 chars):", 6));
            String regNo = req(readLine(screen, "Registration Number:", 6));
            String offAddr = req(readLine(screen, "Office Address:", 6));
            String contact = req(readLine(screen, "Contact Number (10 digits):", 6));
            String website = readLine(screen, "Website URL (blank = none):", 6);
            if (website == null)
                throw new CancelException();

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO orgSeller(userId, organizationName, panNumber, registrationNumber, "
                            + "officeAddress, contactNumber, websiteUrl) VALUES(?,?,?,?,?,?,?)");
            ps.setInt(1, userId);
            ps.setString(2, orgName);
            ps.setString(3, pan);
            ps.setString(4, regNo);
            ps.setString(5, offAddr);
            ps.setString(6, contact);
            ps.setString(7, website.isEmpty() ? null : website);
            ps.executeUpdate();
        }
    }

    private static void insertAgent(Screen screen, Connection con, int userId) throws Exception {
        String expStr = req(readLine(screen, "Experience Years:", 6));
        String salStr = readLine(screen, "Salary (blank = 0):", 6);
        if (salStr == null)
            throw new CancelException();

        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO agent(userId, experienceYears, salary, rating, dealCount, rentCount) "
                        + "VALUES(?,?,?,0.0,0,0)");
        ps.setInt(1, userId);
        ps.setInt(2, Integer.parseInt(expStr));
        ps.setInt(3, salStr.isEmpty() ? 0 : Integer.parseInt(salStr));
        ps.executeUpdate();
    }

    private static void salesReportAllAgents(Screen screen) throws Exception {
        String sql = "SELECT u.name AS agent, " +
                "       p.property_id, p.title, p.property_type, " +
                "       p.locality_name, p.city, p.bhk, p.area_sqft, " +
                "       p.price AS listed_price, " +
                "       t.transactionAmount AS final_price, " +
                "       t.transactionDate AS sale_date " +
                "FROM transactions t " +
                "JOIN property p ON t.propertyId = p.property_id " +
                "LEFT JOIN agent a ON t.agentId = a.userId " +
                "LEFT JOIN users  u ON a.userId  = u.userId " +
                "WHERE t.transactionType = 'sale' " +
                "ORDER BY u.name, t.transactionDate DESC";
        executeAndDisplay(screen, sql, "--- Sales Reports (All Agents) ---");
    }

    private static void salesReportByYear(Screen screen) throws Exception {
        String year = readLine(screen, "Enter year (e.g. 2024):", 6);
        if (year == null || year.isEmpty())
            return;
        String sql = "SELECT u.name AS agent, p.title, p.locality_name, p.city, " +
                "       t.transactionDate AS sale_date, t.transactionAmount AS final_price " +
                "FROM transactions t " +
                "JOIN property p ON t.propertyId = p.property_id " +
                "LEFT JOIN agent a ON t.agentId = a.userId " +
                "LEFT JOIN users u ON a.userId  = u.userId " +
                "WHERE t.transactionType = 'sale' AND YEAR(t.transactionDate) = " + year + " " +
                "ORDER BY t.transactionDate DESC";
        executeAndDisplay(screen, sql, "--- Sales Report for " + year + " ---");
    }

    private static void rentalAnalytics(Screen screen) throws Exception {
        String sql = "select a.userId,p.locality_name,p.price,p.status,p.pincode from agent a inner join property p on a.userId = p.agentId where p.status = 'rented';";
        executeAndDisplay(screen, sql, "--- Rental Analytics (All Agents) ---");
    }

    private static void agentPerformance(Screen screen) throws Exception {
        String sql = "SELECT u.name, a.experienceYears AS exp_yrs, a.rating, " +
                "       a.dealCount, a.rentCount, " +
                "       (a.dealCount + a.rentCount) AS total_activity " +
                "FROM agent a " +
                "JOIN users u ON a.userId = u.userId " +
                "ORDER BY total_activity DESC";
        executeAndDisplay(screen, sql, "--- Agent Performance ---");
    }

    private static void showHousesBuiltAfter2023ForRent(Screen screen) throws Exception {
        String sql = "SELECT property_id, title, locality_name, city, bhk, price, year_built " +
                "FROM property " +
                "WHERE year_built > 2023 AND status = 'available' AND listing_type = 'rent' " +
                "ORDER BY year_built DESC";
        executeAndDisplay(screen, sql, "--- Houses Built After 2023 (For Rent) ---");
    }

    private static void showHousesByPriceRange(Screen screen) throws Exception {
        String sql = "SELECT property_id, title, locality_name, city, bhk, price, status " +
                "FROM property " +
                "WHERE price BETWEEN 2000000 AND 6000000 " +
                "ORDER BY price ASC";
        executeAndDisplay(screen, sql, "--- Houses Priced Rs.20L - Rs.60L ---");
    }

    private static void showHousesForRentByLocality(Screen screen) throws Exception {
        String locality = readLine(screen, "Enter locality name:", 6);
        if (locality == null || locality.isEmpty())
            return;
        String sql = "SELECT property_id, title, locality_name, bhk, price, status " +
                "FROM property " +
                "WHERE locality_name = '" + locality + "' AND listing_type = 'rent' " +
                "ORDER BY price ASC";
        executeAndDisplay(screen, sql, "--- Houses for Rent in " + locality + " ---");
    }

    private static void viewAvailableProperties(Screen screen) throws Exception {
        String sql = "SELECT property_id, title, locality_name, city, bhk, " +
                "       price, listing_type, property_type, year_built " +
                "FROM property WHERE status = 'available' ORDER BY city, locality_name";
        executeAndDisplay(screen, sql, "--- Available Properties ---");
    }

    private static void markPropertyAsSold(Screen screen) throws Exception {
        String propId = readLine(screen, "Property ID to mark SOLD:", 6);
        if (propId == null)
            return;
        String amount = readLine(screen, "Sale Amount (Rs.):", 6);
        if (amount == null)
            return;
        String seekerId = readLine(screen, "Buyer userId (property_seeker):", 6);
        if (seekerId == null)
            return;

        try (Connection con = getConnection()) {
            PreparedStatement chk = con.prepareStatement(
                    "SELECT sellerID, agentID FROM property WHERE property_id = ? AND status = 'available'");
            chk.setInt(1, Integer.parseInt(propId));
            ResultSet rs = chk.executeQuery();
            if (!rs.next()) {
                showMessage(screen, "Property not found or not available.  (Press any key)");
                return;
            }
            int sellerId = rs.getInt("sellerID");
            int fetchedAgentId = rs.getInt("agentID");
            rs.close();

            PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO transactions(transactionAmount, transactionType, propertyId, sellerId, seekerId, agentId) "
                            +
                            "VALUES(?,?,?,?,?,?)");
            ins.setInt(1, Integer.parseInt(amount));
            ins.setString(2, "sale");
            ins.setInt(3, Integer.parseInt(propId));
            ins.setInt(4, sellerId);
            ins.setInt(5, Integer.parseInt(seekerId));
            ins.setInt(6, fetchedAgentId);
            ins.executeUpdate();

            showMessage(screen, "Property " + propId + " marked as SOLD successfully!  (Press any key)");
        } catch (Exception e) {
            showMessage(screen, "Error: " + e.getMessage() + "  (Press any key)");
        }
    }

    private static void markPropertyAsRented(Screen screen) throws Exception {
        String propId = readLine(screen, "Property ID to mark RENTED:", 6);
        if (propId == null)
            return;
        String amount = readLine(screen, "Rent Amount (Rs.):", 6);
        if (amount == null)
            return;
        String seekerId = readLine(screen, "Tenant userId (property_seeker):", 6);
        if (seekerId == null)
            return;

        try (Connection con = getConnection()) {
            PreparedStatement chk = con.prepareStatement(
                    "SELECT sellerID, agentID FROM property WHERE property_id = ? AND status = 'available'");
            chk.setInt(1, Integer.parseInt(propId));
            ResultSet rs = chk.executeQuery();
            if (!rs.next()) {
                showMessage(screen, "Property not found or not available.  (Press any key)");
                return;
            }
            int sellerId = rs.getInt("sellerID");
            int fetchedAgentId = rs.getInt("agentID");
            rs.close();

            PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO transactions(transactionAmount, transactionType, propertyId, sellerId, seekerId, agentId) "
                            +
                            "VALUES(?,?,?,?,?,?)");
            ins.setInt(1, Integer.parseInt(amount));
            ins.setString(2, "rent");
            ins.setInt(3, Integer.parseInt(propId));
            ins.setInt(4, sellerId);
            ins.setInt(5, Integer.parseInt(seekerId));
            ins.setInt(6, fetchedAgentId);
            ins.executeUpdate();

            showMessage(screen, "Property " + propId + " marked as RENTED successfully!  (Press any key)");
        } catch (Exception e) {
            showMessage(screen, "Error: " + e.getMessage() + "  (Press any key)");
        }
    }

    // Reads a line from user
    private static String readLine(Screen screen, String prompt, int row) throws Exception {
        StringBuilder input = new StringBuilder();
        while (true) {
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
            if (key.getKeyType() == KeyType.Escape)
                return null;
            if (key.getKeyType() == KeyType.Enter)
                return input.toString().trim();
            if (key.getKeyType() == KeyType.Backspace && input.length() > 0)
                input.deleteCharAt(input.length() - 1);
            else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null)
                input.append(key.getCharacter());
        }
    }

    // highlights the selected option
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
                    tg.setBackgroundColor(TextColor.ANSI.GREEN);
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
            if (key.getKeyType() == KeyType.Escape)
                return null;
            if (key.getKeyType() == KeyType.Enter)
                return options[sel];
            if (key.getKeyType() == KeyType.ArrowUp)
                sel = (sel > 0) ? sel - 1 : options.length - 1;
            if (key.getKeyType() == KeyType.ArrowDown)
                sel = (sel < options.length - 1) ? sel + 1 : 0;
        }
    }

    private static String req(String value) throws CancelException {
        if (value == null)
            throw new CancelException();
        return value;
    }

    private static class CancelException extends Exception {
        CancelException() {
            super("Cancelled");
        }
    }

    private static void executeAndDisplay(Screen screen, String sql, String title) throws Exception {
        try (Connection con = getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            List<String> rows = resultSetToLines(rs);
            rows.add(0, title);
            showScrollableResult(screen, rows);
        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage() + "  (Press any key)");
        }
    }

    // Turns the rows into lines
    private static List<String> resultSetToLines(ResultSet rs) throws SQLException {
        List<String> lines = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        int[] widths = new int[cols];
        for (int i = 1; i <= cols; i++)
            widths[i - 1] = meta.getColumnName(i).length();

        List<String[]> dataRows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 1; i <= cols; i++) {
                String v = rs.getString(i);
                row[i - 1] = (v == null) ? "NULL" : v;
                widths[i - 1] = Math.max(widths[i - 1], row[i - 1].length());
            }
            dataRows.add(row);
        }

        StringBuilder header = new StringBuilder(), divider = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            String fmt = "%-" + (widths[i - 1] + 2) + "s";
            header.append(String.format(fmt, meta.getColumnName(i)));
            divider.append("-".repeat(widths[i - 1] + 2));
        }
        lines.add(header.toString());
        lines.add(divider.toString());
        for (String[] dr : dataRows) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < cols; i++)
                row.append(String.format("%-" + (widths[i] + 2) + "s", dr[i]));
            lines.add(row.toString());
        }
        if (lines.size() == 2)
            lines.add("(No records found)");
        return lines;
    }

    private static void showScrollableResult(Screen screen, List<String> lines) throws Exception {
        int offset = 0, hOffset = 0;
        int maxVisible = screen.getTerminalSize().getRows() - 4;
        boolean viewing = true;
        while (viewing) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            int displayWidth = screen.getTerminalSize().getColumns() - 4;
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, 0, "UP/DOWN: scroll  |  LEFT/RIGHT: pan  |  Q: back");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            for (int i = 0; i < maxVisible; i++) {
                int idx = offset + i;
                if (idx < lines.size()) {
                    String text = lines.get(idx);
                    int start = Math.min(hOffset, text.length());
                    String slice = text.substring(start);
                    if (slice.length() > displayWidth)
                        slice = slice.substring(0, displayWidth);
                    tg.putString(2, i + 2, slice);
                }
            }
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, screen.getTerminalSize().getRows() - 1,
                    "Row " + (offset + 1) + "/" + lines.size() + "  Col >" + hOffset);
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowDown) {
                if (offset + maxVisible < lines.size())
                    offset++;
            } else if (key.getKeyType() == KeyType.ArrowUp) {
                if (offset > 0)
                    offset--;
            } else if (key.getKeyType() == KeyType.ArrowRight) {
                hOffset += 8;
            } else if (key.getKeyType() == KeyType.ArrowLeft) {
                if (hOffset > 0)
                    hOffset = Math.max(0, hOffset - 8);
            } else if (key.getKeyType() == KeyType.Character
                    && (key.getCharacter() == 'q' || key.getCharacter() == 'Q')) {
                viewing = false;
            }
        }
    }

    private static void showMessage(Screen screen, String message) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 2, message);
        screen.refresh();
        screen.readInput();
    }

    // DB connection
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // converts a string to sha 256
    private static String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
