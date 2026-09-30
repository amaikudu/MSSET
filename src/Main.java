import javax.swing.SwingUtilities;

import gui.MainFrame;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::openWindow);
    }
}
