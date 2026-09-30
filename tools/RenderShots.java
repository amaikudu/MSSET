import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import gui.MainFrame;
import util.DarkTheme;

public class RenderShots {

    private static final int WIDTH = 1560;
    private static final int HEIGHT = 960;

    private static final String[] MODULES = {
        "Dashboard", "Market Data", "Order Book", "Surveillance", "Rule Builder",
        "Alerts", "Charting & Analytics", "Audit Reports", "Replay", "Settings"
    };

    public static void main(String[] args) throws Exception {
        final String outputFolder = args.length > 0 ? args[0] : "shots";
        new File(outputFolder).mkdirs();

        final MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            DarkTheme.install();
            holder[0] = new MainFrame();
            holder[0].setSize(WIDTH, HEIGHT);
            holder[0].layoutTree();
        });

        MainFrame app = holder[0];
        System.out.println("Modules: " + MODULES.length);

        for (int i = 0; i < MODULES.length; i++) {
            final int index = i;
            SwingUtilities.invokeAndWait(() -> {
                app.showModule(MODULES[index]);
                app.layoutTree();
            });

            sleep(1500);

            SwingUtilities.invokeAndWait(() -> {
                app.layoutTree();
                app.validate();
            });
            sleep(150);

            BufferedImage image = paint(app);
            String fileName = String.format("%s/%02d-%s.png", outputFolder, i,
                    MODULES[i].replaceAll("[^A-Za-z0-9]+", "_"));
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("[OK]   " + fileName + "  (" + image.getWidth()
                    + "x" + image.getHeight() + ")");
        }

        app.shutdown();
        System.out.println("Rendered into: " + new File(outputFolder).getAbsolutePath());
        System.exit(0);
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

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
