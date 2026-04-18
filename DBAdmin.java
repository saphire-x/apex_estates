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

public class DBAdmin {

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "Root@123";

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

    // ─── Main Menu ────────────────────────────────────────────────────────────

    public static void runAdminMenu(Screen screen) throws Exception {
        int selectedIndex = 0;
        boolean running = true;

        while (running) {
            drawMenu(screen, selectedIndex);
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

    private static void drawMenu(Screen screen, int selectedIndex) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();

        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(2, 1, "REAL ESTATE DB - ADMIN PANEL");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 2, "Use UP/DOWN arrows and ENTER to select");

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

    // ─── Text Input via Lanterna ──────────────────────────────────────────────

    /**
     * Shows a prompt and reads a line of text using Lanterna keys.
     * Supports typing, Backspace, and Enter.
     * Returns null if the user presses Escape (go back).
     */
    private static String readLine(Screen screen, String prompt, int row) throws Exception {
        StringBuilder input = new StringBuilder();
        while (true) {
            // Draw prompt + current input
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, row - 2, prompt);
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.putString(2, row, "> " + input + "_");
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, row + 2, "ENTER to confirm  |  ESC to go back");
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Escape) {
                return null; // caller treats null as "go back"
            } else if (key.getKeyType() == KeyType.Enter) {
                return input.toString().trim();
            } else if (key.getKeyType() == KeyType.Backspace) {
                if (input.length() > 0) input.deleteCharAt(input.length() - 1);
            } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                input.append(key.getCharacter());
            }
        }
    }

    /**
     * Multi-field form: shows each field one at a time, collects all values.
     * Returns null if the user presses Escape on any field.
     */
    private static String[] readForm(Screen screen, String title, String[] fields) throws Exception {
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String header = title + "  [Field " + (i + 1) + " of " + fields.length + "]";
            String value = readLine(screen, header + "\n\n" + fields[i] + ":", 6);
            // readLine uses the prompt in one string; let's draw title + field separately
            // (we'll handle it inside the loop with a custom draw)
            value = readFormField(screen, title, fields, i, values);
            if (value == null) return null; // user pressed ESC
            values[i] = value;
        }
        return values;
    }

    /** Draws the form with all fields, highlighting the current one. */
    private static String readFormField(Screen screen, String title, String[] fields, int current, String[] filled) throws Exception {
        StringBuilder input = new StringBuilder();
        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, 1, title);
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, 2, "ENTER to confirm  |  ESC to go back");

            for (int i = 0; i < fields.length; i++) {
                int row = 4 + i * 2;
                if (i < current) {
                    // Already filled
                    tg.setForegroundColor(TextColor.ANSI.GREEN);
                    tg.putString(2, row, fields[i] + ": " + filled[i]);
                } else if (i == current) {
                    // Active field
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    tg.putString(2, row, fields[i] + ": " + input + "_");
                } else {
                    // Not yet reached
                    tg.setForegroundColor(TextColor.ANSI.BLACK);
                    tg.putString(2, row, fields[i] + ":");
                }
            }
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Escape) {
                return null;
            } else if (key.getKeyType() == KeyType.Enter) {
                return input.toString().trim();
            } else if (key.getKeyType() == KeyType.Backspace) {
                if (input.length() > 0) input.deleteCharAt(input.length() - 1);
            } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                input.append(key.getCharacter());
            }
        }
    }

    // ─── Menu Actions ─────────────────────────────────────────────────────────

    private static void runCustomQuery(Screen screen) throws Exception {
        String sql = readLine(screen, "Enter SQL Query:", 6);
        if (sql == null || sql.isEmpty()) return; // ESC or empty → go back

        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            String upper = sql.trim().toUpperCase();
            if (upper.startsWith("SELECT") || upper.startsWith("SHOW") || upper.startsWith("DESCRIBE")) {
                ResultSet rs = st.executeQuery(sql);
                List<String> lines = resultSetToLines(rs);
                lines.add(0, "Query: " + sql);
                lines.add(1, "─".repeat(60));
                showScrollableResult(screen, lines);
            } else {
                int affected = st.executeUpdate(sql);
                showMessage(screen, "OK — " + affected + " row(s) affected.");
            }
        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage());
        }
    }

    private static void showAllTables(Screen screen) throws Exception {
        executeAndDisplay(screen, "SHOW TABLES", "Database Tables");
    }

    private static void showAllAgents(Screen screen) throws Exception {
        executeAndDisplay(screen,
                "SELECT u.name, a.userId, a.experienceYears, a.salary, a.rating, a.dealCount, a.rentCount " +
                "FROM agent a JOIN users u ON a.userId = u.userId",
                "Agents List");
    }

    private static void showAllProperties(Screen screen) throws Exception {
        executeAndDisplay(screen, "SELECT * FROM property", "Property Inventory");
    }

    private static void showAllSales(Screen screen) throws Exception {
        executeAndDisplay(screen,
                "SELECT * FROM transactions WHERE transactionType = 'sale'",
                "Sales Records");
    }

    private static void showAllRentals(Screen screen) throws Exception {
        executeAndDisplay(screen,
                "SELECT * FROM transactions WHERE transactionType = 'rent'",
                "Rental Records");
    }

    private static void addAgent(Screen screen) throws Exception {
        String[] values = readForm(screen,
                "Add New Agent (requires an existing User ID)",
                new String[]{"User ID", "Experience Years", "Salary"});
        if (values == null) return; // user pressed ESC

        try {
            int id  = Integer.parseInt(values[0]);
            int exp = Integer.parseInt(values[1]);
            int sal = Integer.parseInt(values[2]);

            String sql = String.format(
                    "INSERT INTO agent (userId, experienceYears, salary, rating, dealCount, rentCount) " +
                    "VALUES (%d, %d, %d, 0.0, 0, 0)", id, exp, sal);

            try (Connection con = getConnection(); Statement st = con.createStatement()) {
                st.executeUpdate(sql);
                showMessage(screen, "Agent created for User ID " + id);
            }
        } catch (NumberFormatException e) {
            showMessage(screen, "Invalid number input. Please enter integers only.");
        } catch (SQLException e) {
            showMessage(screen, "DB Error: " + e.getMessage());
        }
    }

    private static void deleteRecord(Screen screen) throws Exception {
        String[] values = readForm(screen,
                "Delete a Record",
                new String[]{"Table Name", "ID Column (e.g. userId)", "ID Value"});
        if (values == null) return;

        String sql = "DELETE FROM " + values[0] + " WHERE " + values[1] + " = " + values[2];
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            int rows = st.executeUpdate(sql);
            showMessage(screen, rows + " record(s) deleted.");
        } catch (SQLException e) {
            showMessage(screen, "DB Error: " + e.getMessage());
        }
    }

    // ─── Display Helpers ──────────────────────────────────────────────────────

    static void executeAndDisplay(Screen screen, String sql, String title) throws Exception {
        try (Connection con = getConnection();
             Statement st  = con.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {

            List<String> lines = resultSetToLines(rs);
            lines.add(0, "─".repeat(60));
            lines.add(0, "  " + title);
            showScrollableResult(screen, lines);

        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage());
        }
    }

    private static List<String> resultSetToLines(ResultSet rs) throws SQLException {
        List<String> lines = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();

        // Seed widths from column header lengths
        int[] widths = new int[cols];
        for (int i = 1; i <= cols; i++) {
            widths[i - 1] = meta.getColumnName(i).length();
        }

        // Buffer all data rows first so we can measure real max widths
        List<String[]> dataRows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 1; i <= cols; i++) {
                String val = rs.getString(i);
                row[i - 1] = (val == null) ? "NULL" : val;
                widths[i - 1] = Math.max(widths[i - 1], row[i - 1].length());
            }
            dataRows.add(row);
        }

        // Header
        StringBuilder header = new StringBuilder();
        StringBuilder divider = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            String fmt = "%-" + (widths[i - 1] + 2) + "s";
            header.append(String.format(fmt, meta.getColumnName(i)));
            divider.append("-".repeat(widths[i - 1] + 2));
        }
        lines.add(header.toString());
        lines.add(divider.toString());

        // Rows
        int rowCount = dataRows.size();
        for (String[] dataRow : dataRows) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < cols; i++) {
                String fmt = "%-" + (widths[i] + 2) + "s";
                row.append(String.format(fmt, dataRow[i]));
            }
            lines.add(row.toString());
        }
        lines.add("");
        lines.add("  Total rows: " + rowCount);
        return lines;
    }

    private static void showScrollableResult(Screen screen, List<String> lines) throws Exception {
        int offset = 0;
        int visibleRows = screen.getTerminalSize().getRows() - 4;

        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();

            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, 0, "UP/DOWN to scroll  |  Q or ESC to go back");
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            for (int i = 0; i < visibleRows; i++) {
                int lineIdx = offset + i;
                if (lineIdx < lines.size()) {
                    // Clip to terminal width to avoid wrapping artifacts
                    String line = lines.get(lineIdx);
                    int maxWidth = screen.getTerminalSize().getColumns() - 2;
                    if (line.length() > maxWidth) line = line.substring(0, maxWidth);
                    tg.putString(1, i + 2, line);
                }
            }

            // Scroll indicator
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, screen.getTerminalSize().getRows() - 1,
                    "Line " + (offset + 1) + "/" + lines.size());
            screen.refresh();

            KeyStroke k = screen.readInput();
            KeyType type = k.getKeyType();

            if (type == KeyType.ArrowDown) {
                if (offset < lines.size() - visibleRows) offset++;
            } else if (type == KeyType.ArrowUp) {
                if (offset > 0) offset--;
            } else if (type == KeyType.Escape) {
                break;
            } else if (type == KeyType.Character) {
                char c = k.getCharacter();
                if (c == 'q' || c == 'Q') break;
            }
        }
    }

    private static void showMessage(Screen screen, String msg) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(2, 2, msg);
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 4, "Press any key to go back...");
        screen.refresh();
        screen.readInput();
    }

    // ─── DB Connection ────────────────────────────────────────────────────────

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}