import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;

public class UserPanel {

    private static final String[] OPTIONS = {
            "View All Properties",
            "My Transactions",
            "My Profile",
            "Exit"
    };

    public static void run(Screen screen, int userId) throws Exception {
        int sel = 0;
        boolean running = true;
        while (running) {
            drawMenu(screen, sel);
            KeyStroke key = screen.readInput();
            if (key.getKeyType() == KeyType.ArrowUp)
                sel = (sel > 0) ? sel - 1 : OPTIONS.length - 1;
            else if (key.getKeyType() == KeyType.ArrowDown)
                sel = (sel < OPTIONS.length - 1) ? sel + 1 : 0;
            else if (key.getKeyType() == KeyType.Enter) {
                switch (sel) {
                    case 0 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT * FROM property",
                            "All Properties");
                    case 1 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT * FROM transactions WHERE seekerId = " + userId,
                            "My Transactions");
                    case 2 -> DBAdmin.executeAndDisplay(screen,
                            "SELECT userId, name, email, phoneNumber FROM users WHERE userId = " + userId,
                            "My Profile");
                    case 3 -> running = false;
                }
            }
        }
    }

    private static void drawMenu(Screen screen, int sel) throws Exception {
        screen.clear();
        TextGraphics tg = screen.newTextGraphics();
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
        tg.putString(2, 1, "APEX ESTATES — USER PANEL");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.putString(2, 2, "UP/DOWN to move  |  ENTER to select");
        for (int i = 0; i < OPTIONS.length; i++) {
            if (i == sel) {
                tg.setForegroundColor(TextColor.ANSI.BLACK);
                tg.setBackgroundColor(TextColor.ANSI.YELLOW);
                tg.putString(2, 4 + i, " > " + OPTIONS[i] + " ");
            } else {
                tg.setForegroundColor(TextColor.ANSI.WHITE);
                tg.setBackgroundColor(TextColor.ANSI.DEFAULT);
                tg.putString(2, 4 + i, "   " + OPTIONS[i]);
            }
        }
        screen.refresh();
    }
}