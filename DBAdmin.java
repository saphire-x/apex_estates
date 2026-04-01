import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DBAdmin {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "rootkK@123";

    private static final Scanner scanner = new Scanner(System.in);

    private static final String[] MENU_OPTIONS = {
            "Run custom SQL query",
            "Show all table names",
            "View all Agents",
            "View all Properties",
            "View all Sales Transactions",
            "View all Rental Transactions",
            "Add a new Agent (Basic)",
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
            e.printStackTrace();
        } finally {
            try {
                if (screen != null) screen.stopScreen();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void runAdminMenu(Screen screen) throws Exception {
        int selectedIndex = 0;
        boolean running = true;

        while (running) {
            drawAdminMenu(screen, selectedIndex);
            KeyStroke key = screen.readInput();

            if (key.getKeyType() == KeyType.ArrowUp) {
                selectedIndex = (selectedIndex > 0) ? selectedIndex - 1 : MENU_OPTIONS.length - 1;
            } else if (key.getKeyType() == KeyType.ArrowDown) {
                selectedIndex = (selectedIndex < MENU_OPTIONS.length - 1) ? selectedIndex + 1 : 0;
            } else if (key.getKeyType() == KeyType.Enter) {
                switch (selectedIndex) {
                    case 0 -> runCustomQuery(screen);
                    case 1 -> showAllTables(screen);
                    case 2 -> showAllAgents(screen);
                    case 3 -> showAllProperties(screen);
                    case 4 -> showAllSales(screen);
                    case 5 -> showAllRentals(screen);
                    case 6 -> addAgent(screen);
                    case 7 -> deleteRecord(screen);
                    case 8 -> running = false;
                }
            }
        }
    }

    private static void drawAdminMenu(Screen screen, int selectedIndex) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(2, 1, "REAL ESTATE DB - ADMIN PANEL (Schema Aligned)");
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            if (i == selectedIndex) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.YELLOW);
                tg.putString(2, 4 + i, " > " + MENU_OPTIONS[i] + " ");
            } else {
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(2, 4 + i, "   " + MENU_OPTIONS[i]);
            }
        }
        screen.refresh();
    }

    private static void runCustomQuery(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.print("\nSQL> ");
        String sql = scanner.nextLine().trim();
        screen.startScreen();

        if (sql.isEmpty()) return;

        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            if (sql.toUpperCase().startsWith("SELECT") || sql.toUpperCase().startsWith("SHOW")) {
                ResultSet rs = st.executeQuery(sql);
                showScrollableResult(screen, resultSetToLines(rs));
            } else {
                int affected = st.executeUpdate(sql);
                showMessage(screen, affected + " row(s) affected.");
            }
        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage());
        }
    }
    

    private static void showAllTables(Screen screen) throws Exception {
        executeAndDisplay(screen, "SHOW TABLES", "--- Database Tables ---");
    }

    private static void showAllAgents(Screen screen) throws Exception {
        // Joining with users table to get the name as per schema
        executeAndDisplay(screen, "SELECT u.name, a.* FROM agent a JOIN users u ON a.userId = u.userId", "--- Agents List ---");
    }

    private static void showAllProperties(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM property", "--- Property Inventory ---");
    }

    private static void showAllSales(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM transactions WHERE transactionType = 'sale'", "--- Sales Records ---");
    }

    private static void showAllRentals(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM transactions WHERE transactionType = 'rent'", "--- Rental Records ---");
    }

    private static void addAgent(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Add Agent (Requires existing User ID) ---");
        System.out.print("Enter existing User ID: ");
        int id = Integer.parseInt(scanner.nextLine());
        System.out.print("Experience Years: ");
        int exp = Integer.parseInt(scanner.nextLine());
        System.out.print("Salary: ");
        int sal = Integer.parseInt(scanner.nextLine());
        screen.startScreen();

        String sql = String.format("INSERT INTO agent (userId, experienceYears, salary, rating, dealCount, rentCount) VALUES (%d, %d, %d, 0.0, 0, 0)", id, exp, sal);
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(sql);
            showMessage(screen, "Agent record created for User " + id);
        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage());
        }
    }

    private static void deleteRecord(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.println("--- Delete Record ---");
        System.out.print("Table Name: ");
        String table = scanner.nextLine().trim();
        System.out.print("ID Column (e.g., property_id or userId): ");
        String idCol = scanner.nextLine().trim();
        System.out.print("Value: ");
        String val = scanner.nextLine().trim();
        screen.startScreen();

        String sql = "DELETE FROM " + table + " WHERE " + idCol + " = " + val;
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            int rows = st.executeUpdate(sql);
            showMessage(screen, rows + " record(s) deleted.");
        } catch (SQLException e) {
            showMessage(screen, "Error: " + e.getMessage());
        }
    }

    private static void executeAndDisplay(Screen screen, String sql, String title) throws Exception {
        try (Connection con = getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<String> rows = resultSetToLines(rs);
            rows.add(0, title);
            showScrollableResult(screen, rows);
        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage());
        }
    }

    private static List<String> resultSetToLines(ResultSet rs) throws SQLException {
        List<String> lines = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= cols; i++) header.append(String.format("%-18s", meta.getColumnName(i)));
        lines.add(header.toString());

        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                row.append(String.format("%-18s", val == null ? "NULL" : val));
            }
            lines.add(row.toString());
        }
        return lines;
    }

    private static void showScrollableResult(Screen screen, List<String> lines) throws Exception {
        int offset = 0;
        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.putString(2, 0, "Viewing Results (UP/DOWN to Scroll, Q to Back)");
            for (int i = 0; i < screen.getTerminalSize().getRows() - 4; i++) {
                if (offset + i < lines.size()) {
                    tg.putString(1, i + 2, lines.get(offset + i));
                }
            }
            screen.refresh();
            KeyStroke k = screen.readInput();
            if (k.getKeyType() == KeyType.ArrowDown && offset < lines.size() - 5) offset++;
            else if (k.getKeyType() == KeyType.ArrowUp && offset > 0) offset--;
            else if (k.getCharacter() != null && (k.getCharacter() == 'q' || k.getCharacter() == 'Q')) break;
        }
    }

    private static void showMessage(Screen screen, String msg) throws Exception {
        screen.clear();
        screen.newTextGraphics().putString(2, 2, msg + " [Press any key]");
        screen.refresh();
        screen.readInput();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}