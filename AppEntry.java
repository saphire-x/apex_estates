import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.security.MessageDigest;
import java.sql.*;
import java.util.Scanner;

/**
 * Entry point — plain console login, then routes to the right panel:
 *   admin   → DBAdmin    (full access)
 *   agent   → AgentPanel (own transactions + all properties)
 *   user    → UserPanel  (own transactions + all properties)
 *
 * Change ADMIN_EMAIL to the email of your admin account in the DB.
 * Passwords must be stored as SHA-256 hex strings in users.passwordHash.
 */
public class AppEntry {

    private static final String ADMIN_EMAIL = "navneet202428@gmail.com";

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════╗");
        System.out.println("║   APEX ESTATES LOGIN     ║");
        System.out.println("╚══════════════════════════╝");

        Scanner sc = new Scanner(System.in);
        int userId = -1;

        while (userId < 0) {
            System.out.print("Email   : ");
            String email = sc.nextLine().trim();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();

            userId = authenticate(email, password);
            if (userId < 0) System.out.println("  Invalid credentials. Try again.\n");
        }

        System.out.println("  Login successful! Loading...\n");
        String role = detectRole(userId);

        Screen screen = null;
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            switch (role) {
                case "admin" -> DBAdmin.runAdminMenu(screen);
                case "agent" -> AgentPanel.run(screen, userId);
                default      -> UserPanel.run(screen, userId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (screen != null) screen.stopScreen(); } catch (Exception ignored) {}
        }
    }

    private static int authenticate(String email, String password) {
        String hashed = sha256(password);
        if (hashed == null) return -1;
        try (Connection con = DBAdmin.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT userId FROM users WHERE email = ? AND passwordHash = ?")) {
            ps.setString(1, email);
            ps.setString(2, hashed);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("userId");
        } catch (SQLException e) {
            System.out.println("  DB error: " + e.getMessage());
        }
        return -1;
    }

    private static String detectRole(int userId) {
        try (Connection con = DBAdmin.getConnection()) {
            PreparedStatement ps1 = con.prepareStatement(
                    "SELECT email FROM users WHERE userId = ?");
            ps1.setInt(1, userId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next() && ADMIN_EMAIL.equalsIgnoreCase(rs1.getString("email")))
                return "admin";

            PreparedStatement ps2 = con.prepareStatement(
                    "SELECT userId FROM agent WHERE userId = ?");
            ps2.setInt(1, userId);
            if (ps2.executeQuery().next()) return "agent";

        } catch (SQLException e) {
            System.out.println("  DB error: " + e.getMessage());
        }
        return "user";
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}