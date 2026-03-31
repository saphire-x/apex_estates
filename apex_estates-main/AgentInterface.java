import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
 * AgentInterface.java
 * Interface for Individual Agents.
 * Navigate menu with UP/DOWN arrow keys, press ENTER to select.
 */
public class AgentInterface {

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "rootkK@123";

    private static final Scanner scanner = new Scanner(System.in);

    private static int    loggedInAgentId   = -1;
    private static String loggedInAgentName = "";

    private static final String[] MENU_OPTIONS = {
        "View my property listings",
        "Mark a property as SOLD",
        "Mark a property as RENTED",
        "Add a new property listing",
        "View my sales history",
        "View my rental history",
        "My performance summary",
        "Logout & Exit"
    };

    public static void main(String[] args) {
        Screen screen = null;
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            boolean loggedIn = loginAgent(screen);

            if (loggedIn) {
                runAgentMenu(screen);
            } else {
                showMessage(screen, "Login failed. Press any key to exit.");
            }

        } catch (Exception e) {
            System.out.println("Error starting terminal: " + e.getMessage());
        } finally {
            try {
                if (screen != null) {
                    screen.stopScreen();
                }
            } catch (Exception ex) {
                System.out.println("Error closing screen: " + ex.getMessage());
            }
        }
    }

    // ---------- Simple login: ask for agent_id, verify it exists in DB ----------
    private static boolean loginAgent(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("=== AGENT LOGIN ===");
        System.out.print("Enter your Agent ID: ");
        String idStr = scanner.nextLine().trim();
        screen.startScreen();

        int agentId;
        try {
            agentId = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return false;
        }

        String sql = "SELECT agent_id, name FROM Agent WHERE agent_id = " + agentId;
        try (Connection con = getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            if (rs.next()) {
                loggedInAgentId   = rs.getInt("agent_id");
                loggedInAgentName = rs.getString("name");
                return true;
            }
        } catch (SQLException e) {
            showMessage(screen, "DB Error during login: " + e.getMessage() + "  Press any key.");
        }
        return false;
    }

    // ---------- Main menu loop with arrow key navigation ----------
    private static void runAgentMenu(Screen screen) throws Exception {
        int selectedIndex = 0;
        int totalOptions  = MENU_OPTIONS.length;
        boolean running   = true;

        while (running) {
            drawAgentMenu(screen, selectedIndex);
            KeyStroke key = screen.readInput();

            if (key.getKeyType() == KeyType.ArrowUp) {
                if (selectedIndex > 0) {
                    selectedIndex = selectedIndex - 1;
                } else {
                    selectedIndex = totalOptions - 1;
                }

            } else if (key.getKeyType() == KeyType.ArrowDown) {
                if (selectedIndex < totalOptions - 1) {
                    selectedIndex = selectedIndex + 1;
                } else {
                    selectedIndex = 0;
                }

            } else if (key.getKeyType() == KeyType.Enter) {
                switch (selectedIndex) {
                    case 0 -> viewMyProperties(screen);
                    case 1 -> markPropertyAsSold(screen);
                    case 2 -> markPropertyAsRented(screen);
                    case 3 -> addNewProperty(screen);
                    case 4 -> viewMySalesHistory(screen);
                    case 5 -> viewMyRentalHistory(screen);
                    case 6 -> viewMyPerformanceSummary(screen);
                    case 7 -> { running = false; }
                }
            }
        }
    }

    // ---------- Draw the agent menu with highlighted selection ----------
    private static void drawAgentMenu(Screen screen, int selectedIndex) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();

        tg.setForegroundColor(TextColor.ANSI.MAGENTA);
        tg.putString(2, 0, "==========================================");
        tg.putString(2, 1, "   REAL ESTATE - AGENT PANEL              ");
        tg.putString(2, 2, "   Logged in as: " + loggedInAgentName);
        tg.putString(2, 3, "==========================================");

        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 4, "Use UP/DOWN arrows to move, ENTER to select");

        int startRow = 6;
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            if (i == selectedIndex) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.MAGENTA);
                tg.putString(2, startRow + i, " > " + MENU_OPTIONS[i] + "   ");
            } else {
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(2, startRow + i, "   " + MENU_OPTIONS[i]);
            }
        }

        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
        screen.refresh();
    }

    // ================================================================
    //  AGENT ACTION METHODS
    // ================================================================

    private static void viewMyProperties(Screen screen) throws Exception {
        String sql = "SELECT property_id, address, locality, city, bedrooms, "
                   + "       price, rent_amount, prop_type, year_built, status "
                   + "FROM Property WHERE agent_id = " + loggedInAgentId + " "
                   + "ORDER BY status, address";
        executeAndDisplay(screen, sql, "--- My Properties ---");
    }

    private static void markPropertyAsSold(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Mark Property as SOLD ---");
        System.out.print("Property ID           : "); String propId    = scanner.nextLine().trim();
        System.out.print("Sale Date (YYYY-MM-DD): "); String saleDate  = scanner.nextLine().trim();
        System.out.print("Sale Price (Rs.)       : "); String salePrice = scanner.nextLine().trim();
        screen.startScreen();

        String checkSql = "SELECT property_id FROM Property "
                        + "WHERE property_id = " + propId
                        + "  AND agent_id = " + loggedInAgentId
                        + "  AND status = 'available'";

        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {

            ResultSet rs = st.executeQuery(checkSql);
            if (!rs.next()) {
                showMessage(screen, "Property not found, not yours, or already sold/rented. Press any key.");
                return;
            }
            rs.close();

            st.executeUpdate("INSERT INTO Sale (property_id, agent_id, sale_date, sale_price) "
                           + "VALUES (" + propId + ", " + loggedInAgentId + ", '"
                           + saleDate + "', " + salePrice + ")");

            st.executeUpdate("UPDATE Property SET status = 'sold' WHERE property_id = " + propId);

            showMessage(screen, "Property " + propId + " marked as SOLD. Press any key.");

        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage() + "  Press any key.");
        }
    }

    private static void markPropertyAsRented(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Mark Property as RENTED ---");
        System.out.print("Property ID            : "); String propId    = scanner.nextLine().trim();
        System.out.print("Rent Amount (Rs.)       : "); String rentAmt   = scanner.nextLine().trim();
        System.out.print("Start Date (YYYY-MM-DD): "); String startDate = scanner.nextLine().trim();
        System.out.print("End Date   (YYYY-MM-DD): "); String endDate   = scanner.nextLine().trim();
        screen.startScreen();

        String checkSql = "SELECT property_id FROM Property "
                        + "WHERE property_id = " + propId
                        + "  AND agent_id = " + loggedInAgentId
                        + "  AND status = 'available'";

        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {

            ResultSet rs = st.executeQuery(checkSql);
            if (!rs.next()) {
                showMessage(screen, "Property not found, not yours, or already sold/rented. Press any key.");
                return;
            }
            rs.close();

            st.executeUpdate("INSERT INTO Rental (property_id, agent_id, rent_amount, start_date, end_date) "
                           + "VALUES (" + propId + ", " + loggedInAgentId + ", "
                           + rentAmt + ", '" + startDate + "', '" + endDate + "')");

            st.executeUpdate("UPDATE Property SET status = 'rented' WHERE property_id = " + propId);

            showMessage(screen, "Property " + propId + " marked as RENTED. Press any key.");

        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage() + "  Press any key.");
        }
    }

    private static void addNewProperty(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Add New Property ---");
        System.out.print("Address          : "); String address   = scanner.nextLine().trim();
        System.out.print("Locality         : "); String locality  = scanner.nextLine().trim();
        System.out.print("City             : "); String city      = scanner.nextLine().trim();
        System.out.print("Bedrooms         : "); String bedrooms  = scanner.nextLine().trim();
        System.out.print("Sale Price (Rs.) : "); String price     = scanner.nextLine().trim();
        System.out.print("Rent/month (Rs.) : "); String rentAmt   = scanner.nextLine().trim();
        System.out.print("Type (sale/rent) : "); String propType  = scanner.nextLine().trim();
        System.out.print("Year Built       : "); String yearBuilt = scanner.nextLine().trim();
        screen.startScreen();

        String sql = "INSERT INTO Property "
                   + "(address, locality, city, bedrooms, price, rent_amount, "
                   + " prop_type, year_built, status, agent_id) "
                   + "VALUES ('" + address  + "', '"
                                 + locality + "', '"
                                 + city     + "', "
                                 + bedrooms + ", "
                                 + price    + ", "
                                 + rentAmt  + ", '"
                                 + propType + "', "
                                 + yearBuilt + ", 'available', "
                                 + loggedInAgentId + ")";

        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {
            int rows = st.executeUpdate(sql);
            showMessage(screen, rows + " property added successfully. Press any key.");
        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage() + "  Press any key.");
        }
    }

    private static void viewMySalesHistory(Screen screen) throws Exception {
        String sql = "SELECT s.sale_id, p.address, p.city, s.sale_date, s.sale_price "
                   + "FROM Sale s "
                   + "JOIN Property p ON s.property_id = p.property_id "
                   + "WHERE s.agent_id = " + loggedInAgentId + " "
                   + "ORDER BY s.sale_date DESC";
        executeAndDisplay(screen, sql, "--- My Sales History ---");
    }

    private static void viewMyRentalHistory(Screen screen) throws Exception {
        String sql = "SELECT r.rental_id, p.address, p.city, "
                   + "       r.rent_amount, r.start_date, r.end_date "
                   + "FROM Rental r "
                   + "JOIN Property p ON r.property_id = p.property_id "
                   + "WHERE r.agent_id = " + loggedInAgentId + " "
                   + "ORDER BY r.start_date DESC";
        executeAndDisplay(screen, sql, "--- My Rental History ---");
    }

    private static void viewMyPerformanceSummary(Screen screen) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("--- Performance Summary for " + loggedInAgentName + " ---");

        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {

            ResultSet rs1 = st.executeQuery(
                "SELECT COUNT(*) AS num_sales, COALESCE(SUM(sale_price), 0) AS total_sale_value "
              + "FROM Sale WHERE agent_id = " + loggedInAgentId);
            lines.addAll(resultSetToLines(rs1));
            rs1.close();

            lines.add("");

            ResultSet rs2 = st.executeQuery(
                "SELECT COUNT(*) AS num_rentals, COALESCE(SUM(rent_amount), 0) AS total_rent_collected "
              + "FROM Rental WHERE agent_id = " + loggedInAgentId);
            lines.addAll(resultSetToLines(rs2));
            rs2.close();

            lines.add("");

            ResultSet rs3 = st.executeQuery(
                "SELECT COUNT(*) AS active_listings FROM Property "
              + "WHERE agent_id = " + loggedInAgentId + " AND status = 'available'");
            lines.addAll(resultSetToLines(rs3));
            rs3.close();

        } catch (SQLException e) {
            lines.add("SQL Error: " + e.getMessage());
        }

        showScrollableResult(screen, lines);
    }

    // ================================================================
    //  HELPER METHODS
    // ================================================================

    private static void executeAndDisplay(Screen screen, String sql, String title) throws Exception {
        try (Connection con = getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            List<String> rows = resultSetToLines(rs);
            rows.add(0, title);
            showScrollableResult(screen, rows);

        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage() + "  Press any key.");
        }
    }

    private static List<String> resultSetToLines(ResultSet rs) throws SQLException {
        List<String> lines = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            header.append(String.format("%-20s", meta.getColumnName(i)));
        }
        lines.add(header.toString());
        lines.add("-".repeat(cols * 20));

        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                if (val == null) val = "NULL";
                row.append(String.format("%-20s", val));
            }
            lines.add(row.toString());
        }

        if (lines.size() == 2) {
            lines.add("(No records found)");
        }
        return lines;
    }

    private static void showScrollableResult(Screen screen, List<String> lines) throws Exception {
        int offset     = 0;
        int maxVisible = screen.getTerminalSize().getRows() - 4;
        boolean viewing = true;

        while (viewing) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, 0, "UP/DOWN to scroll  |  Q to go back");
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            for (int i = 0; i < maxVisible; i++) {
                int lineIndex = offset + i;
                if (lineIndex < lines.size()) {
                    String text = lines.get(lineIndex);
                    int maxCols = screen.getTerminalSize().getColumns() - 4;
                    if (text.length() > maxCols) {
                        text = text.substring(0, maxCols);
                    }
                    tg.putString(2, i + 2, text);
                }
            }
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowDown) {
                if (offset + maxVisible < lines.size()) {
                    offset = offset + 1;
                }
            } else if (key.getKeyType() == KeyType.ArrowUp) {
                if (offset > 0) {
                    offset = offset - 1;
                }
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

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
