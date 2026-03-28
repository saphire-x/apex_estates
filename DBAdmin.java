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
 * DBAdmin.java
 * Interface for the Database Administrator.
 * Navigate menu with UP/DOWN arrow keys, press ENTER to select.
 */
public class DBAdmin {

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "rootkK@123";

    private static final Scanner scanner = new Scanner(System.in);

    // All menu option labels in order (index 0 = first item)
    private static final String[] MENU_OPTIONS = {
        "Run custom SQL query",
        "Show all table names",
        "View all Agents",
        "View all Properties",
        "View all Sales",
        "View all Rentals",
        "Add a new Agent",
        "Delete a record by ID",
        "Exit"
    };

    public static void main(String[] args) {
        Screen screen = null;
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            runAdminMenu(screen);
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

    // ---------- Main menu loop with arrow key navigation ----------
    private static void runAdminMenu(Screen screen) throws Exception {
        int selectedIndex = 0;           // which menu item is highlighted
        int totalOptions  = MENU_OPTIONS.length;
        boolean running   = true;

        while (running) {
            drawAdminMenu(screen, selectedIndex);
            KeyStroke key = screen.readInput();

            if (key.getKeyType() == KeyType.ArrowUp) {
                // Move highlight up; wrap around to bottom if already at top
                if (selectedIndex > 0) {
                    selectedIndex = selectedIndex - 1;
                } else {
                    selectedIndex = totalOptions - 1;
                }

            } else if (key.getKeyType() == KeyType.ArrowDown) {
                // Move highlight down; wrap around to top if already at bottom
                if (selectedIndex < totalOptions - 1) {
                    selectedIndex = selectedIndex + 1;
                } else {
                    selectedIndex = 0;
                }

            } else if (key.getKeyType() == KeyType.Enter) {
                // Execute whichever option is currently highlighted
                switch (selectedIndex) {
                    case 0 -> runCustomQuery(screen);
                    case 1 -> showAllTables(screen);
                    case 2 -> showAllAgents(screen);
                    case 3 -> showAllProperties(screen);
                    case 4 -> showAllSales(screen);
                    case 5 -> showAllRentals(screen);
                    case 6 -> addAgent(screen);
                    case 7 -> deleteRecord(screen);
                    case 8 -> { running = false; }
                }
            }
            // Any other key press is simply ignored
        }
    }

    // ---------- Draw the admin menu; highlight the selected row ----------
    private static void drawAdminMenu(Screen screen, int selectedIndex) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();

        // Title
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(2, 0, "======================================");
        tg.putString(2, 1, "   REAL ESTATE DB - ADMIN PANEL       ");
        tg.putString(2, 2, "======================================");

        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 3, "Use UP/DOWN arrows to move, ENTER to select");

        // Draw each menu option; highlighted row gets a different color + arrow marker
        int startRow = 5;
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            if (i == selectedIndex) {
                // Highlighted: bright yellow background, black text, arrow prefix
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.YELLOW);
                tg.putString(2, startRow + i, " > " + MENU_OPTIONS[i] + "   ");
            } else {
                // Normal: white text, default background
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(2, startRow + i, "   " + MENU_OPTIONS[i]);
            }
        }

        // Reset colors after drawing menu so nothing bleeds into refresh
        tg.setForegroundColor(TextColor.ANSI.DEFAULT);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);

        screen.refresh();
    }

    // ---------- 1. Run any SQL the admin types ----------
    private static void runCustomQuery(Screen screen) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(2, 0, "--- Custom SQL Query ---");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 2, "Type your SQL below and press ENTER.");
        tg.putString(2, 3, "(For SELECT queries results will be shown.)");
        screen.refresh();

        screen.stopScreen();
        System.out.print("SQL> ");
        String sql = scanner.nextLine().trim();
        screen.startScreen();

        if (sql.isEmpty()) {
            showMessage(screen, "No query entered. Press any key.");
            return;
        }

        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {

            boolean isSelect = sql.toUpperCase().startsWith("SELECT");

            if (isSelect) {
                ResultSet rs = st.executeQuery(sql);
                List<String> rows = resultSetToLines(rs);
                showScrollableResult(screen, rows);
            } else {
                int affected = st.executeUpdate(sql);
                showMessage(screen, affected + " row(s) affected. Press any key.");
            }

        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage() + "  Press any key.");
        }
    }

    // ---------- 2. Show all table names ----------
    private static void showAllTables(Screen screen) throws Exception {
        executeAndDisplay(screen, "SHOW TABLES", "--- All Tables ---");
    }

    // ---------- 3. View all agents ----------
    private static void showAllAgents(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM Agent", "--- All Agents ---");
    }

    // ---------- 4. View all properties ----------
    private static void showAllProperties(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM Property", "--- All Properties ---");
    }

    // ---------- 5. View all sales ----------
    private static void showAllSales(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM Sale", "--- All Sales ---");
    }

    // ---------- 6. View all rentals ----------
    private static void showAllRentals(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM Rental", "--- All Rentals ---");
    }

    // ---------- 7. Add a new agent ----------
    private static void addAgent(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Add New Agent ---");
        System.out.print("Agent Name  : ");  String name  = scanner.nextLine().trim();
        System.out.print("Phone       : ");  String phone = scanner.nextLine().trim();
        System.out.print("Email       : ");  String email = scanner.nextLine().trim();
        screen.startScreen();

        String sql = "INSERT INTO Agent (name, phone, email) VALUES ('"
                     + name + "', '" + phone + "', '" + email + "')";
        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {
            int rows = st.executeUpdate(sql);
            showMessage(screen, rows + " agent added. Press any key.");
        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage() + "  Press any key.");
        }
    }

    // ---------- 8. Delete a record by table name and ID ----------
    private static void deleteRecord(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Delete a Record ---");
        System.out.print("Table name (Agent/Property/Sale/Rental): ");
        String table = scanner.nextLine().trim();
        System.out.print("ID column name (e.g. agent_id)          : ");
        String idCol = scanner.nextLine().trim();
        System.out.print("ID value                                 : ");
        String idVal = scanner.nextLine().trim();
        screen.startScreen();

        String sql = "DELETE FROM " + table + " WHERE " + idCol + " = " + idVal;
        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {
            int rows = st.executeUpdate(sql);
            showMessage(screen, rows + " row(s) deleted. Press any key.");
        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage() + "  Press any key.");
        }
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

    // Scrollable result viewer -- UP/DOWN to scroll, Q to go back
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
