import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.security.MessageDigest;
import java.sql.*;

public class AppEntry {

    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String OFFICE_EMAIL = "office@apexestates.com";

    public static void main(String[] args) {
        Screen screen = null;
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            boolean proceed = showInitialMenu(screen);
            if (!proceed)
                return;

            int userId = -1;
            while (userId < 0) {
                String[] credentials = showLoginForm(screen);
                if (credentials == null)
                    return;

                userId = authenticate(credentials[0], credentials[1]);
                if (userId < 0) {
                    showError(screen, "Invalid Credentials! Press any key.");
                }
            }

            // 3. Routing
            String role = detectRole(userId);
            switch (role) {
                case "admin" -> DBAdmin.runAdminMenu(screen);
                case "office" -> RealEstateOffice.runOfficeMenu(screen);
                case "agent" -> AgentPanel.run(screen, userId);
                default -> UserPanel.run(screen, userId);
            }

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

    private static String[] showLoginForm(Screen screen) throws Exception {
        String email = "";
        String pass = "";
        boolean typingEmail = true;

        while (true) {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            tg.putString(2, 1, "╔═════════════ LOGIN ═════════════╗");
            tg.putString(2, 2, "║  Email:                         ║");
            tg.putString(2, 3, "║  Pass :                         ║");
            tg.putString(2, 4, "╚═════════════════════════════════╝");

            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.putString(12, 2, email + (typingEmail ? "_" : ""));
            tg.putString(12, 3, "*".repeat(pass.length()) + (!typingEmail ? "_" : ""));
            tg.putString(2, 6, "[TAB] Switch | [ENTER] Login | [ESC] Quit");
            screen.refresh();

            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.Escape)
                return null;
            if (key.getKeyType() == KeyType.Tab) {
                typingEmail = !typingEmail;
                continue;
            }
            if (key.getKeyType() == KeyType.Enter && !email.isEmpty())
                return new String[] { email, pass };

            if (key.getKeyType() == KeyType.Backspace) {
                if (typingEmail && !email.isEmpty())
                    email = email.substring(0, email.length() - 1);
                else if (!typingEmail && !pass.isEmpty())
                    pass = pass.substring(0, pass.length() - 1);
            } else if (key.getKeyType() == KeyType.Character) {
                if (typingEmail)
                    email += key.getCharacter();
                else
                    pass += key.getCharacter();
            }
        }
    }

    private static void showError(Screen screen, String msg) throws Exception {
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.RED);
        tg.putString(2, 8, msg);
        screen.refresh();
        screen.readInput();
    }

    private static boolean showInitialMenu(Screen screen) throws Exception {
        final String[] OPTIONS = { "Login", "About" };
        int sel = 0;
        while (true) {
            drawInitialMenu(screen, OPTIONS, sel);
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp)
                sel = (sel > 0) ? sel - 1 : OPTIONS.length - 1;
            else if (key.getKeyType() == KeyType.ArrowDown)
                sel = (sel < OPTIONS.length - 1) ? sel + 1 : 0;
            else if (key.getKeyType() == KeyType.Enter) {
                if (sel == 0)
                    return true;
                if (sel == 1)
                    showAbout(screen);
            }
        }
    }

    private static void drawInitialMenu(Screen screen, String[] options, int sel) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        tg.putString(2, 1, "╔══════════════════════════════════════╗");
        tg.putString(2, 2, "║         APEX ESTATES SYSTEM          ║");
        tg.putString(2, 3, "╚══════════════════════════════════════╝");
        for (int i = 0; i < options.length; i++) {
            if (i == sel) {
                tg.setForegroundColor(TextColor.ANSI.BLACK).setBackgroundColor(TextColor.ANSI.YELLOW);
                tg.putString(4, 5 + i, " > " + options[i] + " ");
            } else {
                tg.setForegroundColor(TextColor.ANSI.WHITE).setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(4, 5 + i, "   " + options[i]);
            }
        }
        screen.refresh();
    }

    private static void showAbout(Screen screen) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        int col = 2;

        tg.putString(col, 1, "==================================================");
        tg.putString(col, 2, "              APEX ESTATES v1.0");
        tg.putString(col, 3, "       Real-Estate Management Platform");
        tg.putString(col, 4, "==================================================");

        tg.putString(col, 6, "DEVELOPED BY:");

        tg.putString(col, 8, "Navneet Yadav (2401132)");
        tg.putString(col, 9, "  - AgentPanel.java , AppEntry.java,RealEstateOffice.java");

        tg.putString(col, 11, "Shailaj Gupta (2401181)");
        tg.putString(col, 12, "  - RealEstateOffice.java, AgentPanel.java");

        tg.putString(col, 14, "Shashank Kumar (2401182)");
        tg.putString(col, 15, "  - RealEstateOffice.java, AppEntry.java");

        tg.putString(col, 17, "Priya (2401151)");
        tg.putString(col, 18, "  - RealEstateOffice.java,AgentPanel.java");

        tg.putString(col, 21, "Built with Java + Lanterna + MySQL");
        tg.putString(col, 22, "(c) 2024 Apex Estates Team");

        tg.putString(col, 25, "Press any key to go back...");

        screen.refresh();
        screen.readInput();
    }

    private static int authenticate(String email, String password) {
        String hashed = sha256(password);
        if (hashed == null)
            return -1;
        try (Connection con = DBAdmin.getConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT userId FROM users WHERE email = ? AND passwordHash = ?")) {
            ps.setString(1, email);
            ps.setString(2, hashed);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("userId");
        } catch (SQLException ignored) {
        }
        return -1;
    }

    private static String detectRole(int userId) {
        try (Connection con = DBAdmin.getConnection()) {
            PreparedStatement ps1 = con.prepareStatement("SELECT email FROM users WHERE userId = ?");
            ps1.setInt(1, userId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                String email = rs1.getString("email");
                if (ADMIN_EMAIL.equalsIgnoreCase(email))
                    return "admin";
                if (OFFICE_EMAIL.equalsIgnoreCase(email))
                    return "office";
            }
            PreparedStatement ps2 = con.prepareStatement("SELECT userId FROM agent WHERE userId = ?");
            ps2.setInt(1, userId);
            if (ps2.executeQuery().next())
                return "agent";
        } catch (SQLException ignored) {
        }
        return "user";
    }

    static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}