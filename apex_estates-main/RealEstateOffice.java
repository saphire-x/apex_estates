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
 * RealEstateOffice.java
 * Interface for the Real Estate Agent's Office staff.
 * Navigate menu with UP/DOWN arrow keys, press ENTER to select.
 */
public class RealEstateOffice {

    private static final String DB_URL      = "jdbc:mysql://localhost:3306/apex_estates";
    private static final String DB_USER     = "root";
    private static final String DB_PASSWORD = "rootkK@123";

    private static final Scanner scanner = new Scanner(System.in);

    private static final String[] MENU_OPTIONS = {
        "Houses built after 2023 available for rent",
        "Houses priced between Rs.20L - Rs.60L",
        "Houses for rent in a specific locality",
        "Agent who sold most property in 2023",
        "Avg sale price & time on market in 2018",
        "Most expensive house & highest rent",
        "Sales report by year",
        "Rentals given by each agent",
        "View available properties",
        "Exit"
    };

    public static void main(String[] args) {
        Screen screen = null;
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();
            runOfficeMenu(screen);
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
    private static void runOfficeMenu(Screen screen) throws Exception {
        int selectedIndex = 0;
        int totalOptions  = MENU_OPTIONS.length;
        boolean running   = true;

        while (running) {
            drawOfficeMenu(screen, selectedIndex);
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
                    case 0 -> queryHousesBuiltAfter2023ForRent(screen);
                    case 1 -> queryHousesByPriceRange(screen);
                    case 2 -> queryHousesForRentByLocality(screen);
                    case 3 -> queryTopAgentBySalesIn2023(screen);
                    case 4 -> queryAvgSalePriceAndTimeIn2018(screen);
                    case 5 -> queryMostExpensiveAndHighestRent(screen);
                    case 6 -> salesReportByYear(screen);
                    case 7 -> rentalReportByAgent(screen);
                    case 8 -> viewAvailableProperties(screen);
                    case 9 -> { running = false; }
                }
            }
        }
    }

    // ---------- Draw the office menu with highlighted selection ----------
    private static void drawOfficeMenu(Screen screen, int selectedIndex) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();

        tg.setForegroundColor(TextColor.ANSI.GREEN);
        tg.putString(2, 0, "==========================================");
        tg.putString(2, 1, "   REAL ESTATE OFFICE - STAFF PANEL       ");
        tg.putString(2, 2, "==========================================");

        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 3, "Use UP/DOWN arrows to move, ENTER to select");

        int startRow = 5;
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            if (i == selectedIndex) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.GREEN);
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
    //  QUERY METHODS
    // ================================================================

    private static void queryHousesBuiltAfter2023ForRent(Screen screen) throws Exception {
        String sql = "SELECT property_id, address, locality, city, bedrooms, rent_amount, year_built "
                   + "FROM Property "
                   + "WHERE year_built > 2023 AND status = 'available' AND prop_type = 'rent' "
                   + "ORDER BY year_built DESC";
        executeAndDisplay(screen, sql, "--- Houses Built After 2023 (For Rent) ---");
    }

    private static void queryHousesByPriceRange(Screen screen) throws Exception {
        String sql = "SELECT property_id, address, locality, city, bedrooms, price, status "
                   + "FROM Property "
                   + "WHERE price BETWEEN 2000000 AND 6000000 "
                   + "ORDER BY price ASC";
        executeAndDisplay(screen, sql, "--- Houses Priced Between Rs.20L - Rs.60L ---");
    }

    private static void queryHousesForRentByLocality(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.print("Enter locality name (e.g. G.S.Road): ");
        String locality = scanner.nextLine().trim();
        screen.startScreen();

        String sql = "SELECT property_id, address, locality, bedrooms, rent_amount, status "
                   + "FROM Property "
                   + "WHERE locality = '" + locality + "' AND prop_type = 'rent' "
                   + "ORDER BY rent_amount ASC";
        executeAndDisplay(screen, sql, "--- Houses for Rent in " + locality + " ---");
    }

    private static void queryTopAgentBySalesIn2023(Screen screen) throws Exception {
        String sql = "SELECT a.agent_id, a.name, SUM(s.sale_price) AS total_sales "
                   + "FROM Agent a "
                   + "JOIN Sale s ON a.agent_id = s.agent_id "
                   + "WHERE YEAR(s.sale_date) = 2023 "
                   + "GROUP BY a.agent_id, a.name "
                   + "ORDER BY total_sales DESC "
                   + "LIMIT 1";
        executeAndDisplay(screen, sql, "--- Top Agent by Sales in 2023 ---");
    }

    private static void queryAvgSalePriceAndTimeIn2018(Screen screen) throws Exception {
        String sql = "SELECT a.name, "
                   + "       AVG(s.sale_price) AS avg_sale_price_2018, "
                   + "       AVG(DATEDIFF(s.sale_date, "
                   + "           STR_TO_DATE(CONCAT(p.year_built, '-01-01'), '%Y-%m-%d'))) AS avg_days_on_market "
                   + "FROM Agent a "
                   + "JOIN Sale s ON a.agent_id = s.agent_id "
                   + "JOIN Property p ON s.property_id = p.property_id "
                   + "WHERE YEAR(s.sale_date) = 2018 "
                   + "GROUP BY a.agent_id, a.name "
                   + "ORDER BY avg_sale_price_2018 DESC";
        executeAndDisplay(screen, sql, "--- Avg Sale Price & Time on Market (2018) ---");
    }

    private static void queryMostExpensiveAndHighestRent(Screen screen) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("--- Most Expensive & Highest Rent Property ---");

        try (Connection con = getConnection();
             Statement  st  = con.createStatement()) {

            ResultSet rs1 = st.executeQuery(
                "SELECT 'Most Expensive (Sale)' AS category, property_id, address, locality, city, bedrooms, price "
              + "FROM Property ORDER BY price DESC LIMIT 1");
            lines.addAll(resultSetToLines(rs1));
            rs1.close();

            lines.add("");

            ResultSet rs2 = st.executeQuery(
                "SELECT 'Highest Rent' AS category, property_id, address, locality, city, bedrooms, rent_amount AS price "
              + "FROM Property ORDER BY rent_amount DESC LIMIT 1");
            lines.addAll(resultSetToLines(rs2));
            rs2.close();

        } catch (SQLException e) {
            lines.add("SQL Error: " + e.getMessage());
        }

        showScrollableResult(screen, lines);
    }

    private static void salesReportByYear(Screen screen) throws Exception {
        screen.stopScreen();
        System.out.print("Enter year for sales report (e.g. 2024): ");
        String year = scanner.nextLine().trim();
        screen.startScreen();

        String sql = "SELECT s.sale_id, p.address, a.name AS agent, s.sale_date, s.sale_price "
                   + "FROM Sale s "
                   + "JOIN Property p ON s.property_id = p.property_id "
                   + "JOIN Agent    a ON s.agent_id    = a.agent_id "
                   + "WHERE YEAR(s.sale_date) = " + year + " "
                   + "ORDER BY s.sale_date DESC";
        executeAndDisplay(screen, sql, "--- Sales Report for Year " + year + " ---");
    }

    private static void rentalReportByAgent(Screen screen) throws Exception {
        String sql = "SELECT a.name, COUNT(r.rental_id) AS total_rentals, "
                   + "       SUM(r.rent_amount) AS total_rent_collected "
                   + "FROM Agent a "
                   + "JOIN Rental r ON a.agent_id = r.agent_id "
                   + "GROUP BY a.agent_id, a.name "
                   + "ORDER BY total_rent_collected DESC";
        executeAndDisplay(screen, sql, "--- Rentals by Agent ---");
    }

    private static void viewAvailableProperties(Screen screen) throws Exception {
        String sql = "SELECT property_id, address, locality, city, bedrooms, "
                   + "       price, rent_amount, prop_type, year_built "
                   + "FROM Property WHERE status = 'available' ORDER BY city, locality";
        executeAndDisplay(screen, sql, "--- Available Properties ---");
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
