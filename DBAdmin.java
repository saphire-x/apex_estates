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

    private static final String DB_URL = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "rootkK@123";

    private static final String[] MENU_OPTIONS = {
            "Run custom SQL query",
            "Show all table names",
            "View all Agents",
            "View all Properties",
            "View all Sales Transactions",
            "View all Rental Transactions",
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
            e.printStackTrace();
        } finally {
            try {
                if (screen != null)
                    screen.stopScreen();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

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
            tg.putString(2, row + 2, "ENTER to confirm  |  ESC to go back");
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Escape) {
                return null;
            } else if (key.getKeyType() == KeyType.Enter) {
                return input.toString().trim();
            } else if (key.getKeyType() == KeyType.Backspace) {
                if (input.length() > 0)
                    input.deleteCharAt(input.length() - 1);
            } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                input.append(key.getCharacter());
            }
        }
    }

    private static String[] readForm(Screen screen, String title, String[] fields) throws Exception {
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String value = readFormField(screen, title, fields, i, values);
            if (value == null)
                return null;
            values[i] = value;
        }
        return values;
    }

    private static String readFormField(Screen screen, String title, String[] fields, int current, String[] filled)
            throws Exception {
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
                    tg.setForegroundColor(TextColor.ANSI.GREEN);
                    String display = fields[i].toLowerCase().contains("password") ? "********" : filled[i];
                    tg.putString(2, row, fields[i] + ": " + display);
                } else if (i == current) {
                    tg.setForegroundColor(TextColor.ANSI.WHITE);
                    String typed = fields[i].toLowerCase().contains("password")
                            ? "*".repeat(input.length())
                            : input.toString();
                    tg.putString(2, row, fields[i] + ": " + typed + "_");
                } else {
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
                if (input.length() > 0)
                    input.deleteCharAt(input.length() - 1);
            } else if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
                input.append(key.getCharacter());
            }
        }
    }

    // Menu options started
    private static void runCustomQuery(Screen screen) throws Exception {
        String sql = readLine(screen, "Enter SQL query:", 6);
        if (sql == null || sql.isEmpty())
            return;

        String upper = sql.trim().toUpperCase();
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            if (upper.startsWith("SELECT")) {
                ResultSet rs = st.executeQuery(sql);
                List<String> lines = resultSetToLines(rs);
                lines.add(0, "─".repeat(60));
                lines.add(0, "  Query Result");
                showScrollableResult(screen, lines);
            } else {
                int rows = st.executeUpdate(sql);
                showMessage(screen, rows + " row(s) affected.");
            }
        } catch (SQLException e) {
            showMessage(screen, "SQL Error: " + e.getMessage());
        }
    }

    private static void showAllTables(Screen screen) throws Exception {
        executeAndDisplay(screen,
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()",
                "Tables in apex_estates");
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
                "select a.userId,p.locality from agent a inner join property p on a.userId = p.agentId where p.status = 'rented';",
                "Rental Records");
    }

    private static void addAgent(Screen screen) throws Exception {
        String[] values = readForm(screen,
                "Add New Agent",
                new String[] { "Name", "Email", "Phone Number", "Password", "Experience Years", "Salary" });
        if (values == null)
            return;

        String name = values[0];
        String email = values[1];
        String phone = values[2];
        String password = values[3];
        String expStr = values[4];
        String salStr = values[5];

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage(screen, "Name, email, and password are required. Press any key.");
            return;
        }

        // Hash the password before storing
        String hashedPassword = AppEntry.sha256(password);
        if (hashedPassword == null) {
            showMessage(screen, "Error hashing password. Press any key.");
            return;
        }

        try {
            int exp = expStr.isEmpty() ? 0 : Integer.parseInt(expStr);
            int sal = salStr.isEmpty() ? 0 : Integer.parseInt(salStr);

            try (Connection con = getConnection()) {
                con.setAutoCommit(false);

                PreparedStatement psUser = con.prepareStatement(
                        "INSERT INTO users (name, email, phoneNumber, passwordHash) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                psUser.setString(1, name);
                psUser.setString(2, email);
                psUser.setString(3, phone);
                psUser.setString(4, hashedPassword);
                psUser.executeUpdate();

                ResultSet generatedKeys = psUser.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    con.rollback();
                    showMessage(screen, "Failed to get new userId. Press any key.");
                    return;
                }
                int newUserId = generatedKeys.getInt(1);

                PreparedStatement psAgent = con.prepareStatement(
                        "INSERT INTO agent (userId, experienceYears, salary, rating, dealCount, rentCount) " +
                                "VALUES (?, ?, ?, 0.0, 0, 0)");
                psAgent.setInt(1, newUserId);
                psAgent.setInt(2, exp);
                psAgent.setInt(3, sal);
                psAgent.executeUpdate();

                con.commit();
                showMessage(screen, "Agent '" + name + "' created with userId " + newUserId + ". Press any key.");
            }
        } catch (NumberFormatException e) {
            showMessage(screen, "Experience and Salary must be numbers. Press any key.");
        } catch (SQLException e) {
            showMessage(screen, "DB Error: " + e.getMessage());
        }
    }

    private static void deleteRecord(Screen screen) throws Exception {
        String[] values = readForm(screen,
                "Delete a Record",
                new String[] { "Table Name", "ID Column (e.g. userId)", "ID Value" });
        if (values == null)
            return;

        String sql = "DELETE FROM " + values[0] + " WHERE " + values[1] + " = " + values[2];
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            int rows = st.executeUpdate(sql);
            showMessage(screen, rows + " record(s) deleted.");
        } catch (SQLException e) {
            showMessage(screen, "DB Error: " + e.getMessage());
        }
    }

    static void executeAndDisplay(Screen screen, String sql, String title) throws Exception {
        try (Connection con = getConnection();
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(sql)) {

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

        int[] widths = new int[cols];
        for (int i = 1; i <= cols; i++) {
            widths[i - 1] = meta.getColumnName(i).length();
        }

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

        StringBuilder header = new StringBuilder();
        StringBuilder divider = new StringBuilder();
        for (int i = 1; i <= cols; i++) {
            String fmt = "%-" + (widths[i - 1] + 2) + "s";
            header.append(String.format(fmt, meta.getColumnName(i)));
            divider.append("-".repeat(widths[i - 1] + 2));
        }
        lines.add(header.toString());
        lines.add(divider.toString());

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
        int offset = 0; // vertical scroll
        int hOffset = 0; // horizontal scroll
        int visibleRows = screen.getTerminalSize().getRows() - 4;

        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            int termCols = screen.getTerminalSize().getColumns();
            int displayWidth = termCols - 2;

            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, 0, "Arrows: scroll  |  LEFT/RIGHT: pan  |  Q/ESC: back");
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            for (int i = 0; i < visibleRows; i++) {
                int lineIdx = offset + i;
                if (lineIdx < lines.size()) {
                    String line = lines.get(lineIdx);
                    // horizontal slice
                    int start = Math.min(hOffset, line.length());
                    String slice = line.substring(start);
                    if (slice.length() > displayWidth)
                        slice = slice.substring(0, displayWidth);
                    tg.putString(1, i + 2, slice);
                }
            }

            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(2, screen.getTerminalSize().getRows() - 1,
                    "Row " + (offset + 1) + "/" + lines.size() + "  Col >" + hOffset);
            screen.refresh();

            KeyStroke k = screen.readInput();
            KeyType type = k.getKeyType();

            if (type == KeyType.ArrowDown) {
                if (offset < lines.size() - visibleRows)
                    offset++;
            } else if (type == KeyType.ArrowUp) {
                if (offset > 0)
                    offset--;
            } else if (type == KeyType.ArrowRight) {
                hOffset += 8;
            } else if (type == KeyType.ArrowLeft) {
                if (hOffset > 0)
                    hOffset = Math.max(0, hOffset - 8);
            } else if (type == KeyType.Escape) {
                break;
            } else if (type == KeyType.Character) {
                char c = k.getCharacter();
                if (c == 'q' || c == 'Q')
                    break;
            }
        }
    }

    static void showMessage(Screen screen, String msg) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(2, 2, msg);
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 4, "Press any key to go back...");
        screen.refresh();
        screen.readInput();
    }

    // DB connection
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}