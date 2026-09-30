import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import gui.MainFrame;
import util.DarkTheme;

public class Showcase {

    private static final int WIDTH = 1560;
    private static final int HEIGHT = 960;

    private static String folder = "shots";
    private static MainFrame app;

    public static void main(String[] args) throws Exception {
        folder = args.length > 0 ? args[0] : "shots";
        new File(folder).mkdirs();

        final MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            DarkTheme.install();
            holder[0] = new MainFrame();
            holder[0].setSize(WIDTH, HEIGHT);
            app = holder[0];
            app.layoutTree();
        });

        for (int i = 0; i < 10; i++) {
            select(i);
            settle(1500);
            capture(i, moduleName(i));
        }

        select(1);
        settle(300);
        SwingUtilities.invokeAndWait(() -> {
            JTextField search = firstVisible(app, JTextField.class);
            if (search != null) {
                search.setText("nv");
            }
            app.layoutTree();
        });
        settle(700);
        capture(10, "Market Data - search filter in action");

        select(5);
        settle(400);
        SwingUtilities.invokeAndWait(() -> {
            JTable table = firstVisible(app, JTable.class);
            if (table != null && table.getRowCount() > 2) {
                table.setRowSelectionInterval(2, 2);
            }
            app.layoutTree();
        });
        settle(300);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Mark Reviewed");
            app.layoutTree();
        });
        settle(500);
        capture(11, "Alerts - alert reviewed and detail shown");

        select(2);
        settle(500);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Place Order");
            app.layoutTree();
        });
        settle(900);
        SwingUtilities.invokeAndWait(() -> app.layoutTree());
        capture(12, "Order Book - order placed into the market");

        select(4);
        settle(400);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Add Condition");
            app.layoutTree();
        });
        settle(300);
        SwingUtilities.invokeAndWait(() -> app.layoutTree());
        capture(13, "Rule Builder - condition added");

        select(4);
        settle(300);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Load");
            app.layoutTree();
        });
        settle(400);
        SwingUtilities.invokeAndWait(() -> app.layoutTree());
        capture(14, "Rule Builder - saved rule loaded into the editor");

        select(3);
        settle(400);
        SwingUtilities.invokeAndWait(() -> {
            JSlider slider = firstVisible(app, JSlider.class);
            if (slider != null) {
                slider.setValue(90);
            }
            app.layoutTree();
        });
        settle(2500);
        SwingUtilities.invokeAndWait(() -> app.layoutTree());
        capture(15, "Surveillance - live monitoring activity");

        select(8);
        settle(400);
        SwingUtilities.invokeAndWait(() -> {
            JTextField path = firstVisible(app, JTextField.class);
            if (path != null) {
                path.setText("data/NASDAQ_totalview_2024-03-11.itch");
            }
            JSlider speed = firstVisible(app, JSlider.class);
            if (speed != null) {
                speed.setValue(100);
            }
            app.layoutTree();
        });
        settle(200);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Load File");
            app.layoutTree();
        });
        settle(200);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Start");
            app.layoutTree();
        });
        settle(2500);
        SwingUtilities.invokeAndWait(() -> app.layoutTree());
        capture(16, "Replay - file loaded and replay running");

        select(9);
        settle(400);
        SwingUtilities.invokeAndWait(() -> {
            JSlider risk = firstVisible(app, JSlider.class);
            if (risk != null) {
                risk.setValue(75);
            }
            app.layoutTree();
        });
        settle(300);
        SwingUtilities.invokeAndWait(() -> {
            clickButton(app, "Apply");
            app.layoutTree();
        });
        settle(500);
        capture(17, "Settings - risk threshold applied live");

        select(6);
        settle(1600);
        capture(18, "Charting and Analytics - statistics window");

        app.shutdown();
        System.out.println("Done. Images are in " + new File(folder).getAbsolutePath());
        System.exit(0);
    }

    private static String moduleName(int index) {
        String[] names = { "Dashboard", "Market Data", "Order Book", "Surveillance",
            "Rule Builder", "Alerts", "Charting and Analytics", "Audit Reports",
            "Replay", "Settings" };
        return names[index];
    }

    private static void select(int index) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            app.showModule(moduleName(index));
            app.layoutTree();
        });
    }

    private static void settle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                app.layoutTree();
                app.validate();
            });
        } catch (Exception e) {
            System.out.println("Layout pass failed: " + e);
        }
    }

    private static void capture(int number, String title) {
        try {
            BufferedImage image = paint(app);
            String fileName = String.format("%s/%02d-%s.png", folder, number,
                    title.replaceAll("[^A-Za-z0-9]+", "_"));
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("[OK]   " + fileName);
        } catch (Exception e) {
            System.out.println("[FAIL] could not capture " + title + ": " + e);
        }
    }

    private static BufferedImage paint(Component component) {
        int width = Math.max(1, component.getWidth());
        int height = Math.max(1, component.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        component.printAll(g2);
        g2.dispose();
        return image;
    }

    private static <T> T firstVisible(Component root, Class<T> type) {
        List<T> found = new ArrayList<>();
        collectVisible(root, type, found);
        return found.isEmpty() ? null : found.get(0);
    }

    private static <T> void collectVisible(Component component, Class<T> type, List<T> found) {
        if (component == null || !component.isVisible()) {
            return;
        }
        if (type.isInstance(component)) {
            found.add(type.cast(component));
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectVisible(child, type, found);
            }
        }
    }

    private static boolean clickButton(Component root, String text) {
        List<AbstractButton> buttons = new ArrayList<>();
        collectVisible(root, AbstractButton.class, buttons);
        for (AbstractButton button : buttons) {
            if (text.equals(button.getText()) && button.isEnabled()) {
                button.doClick();
                System.out.println("       clicked \"" + text + "\"");
                return true;
            }
        }
        System.out.println("       [WARN] button not found: " + text);
        return false;
    }
}
