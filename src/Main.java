import javax.swing.SwingUtilities;

import gui.MainFrame;
import util.DarkTheme;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DarkTheme.install();
            new MainFrame().setVisible(true);
        });
    }
}
