import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class ContactSheet {

    private static final int COLUMNS = 4;
    private static final int TILE_WIDTH = 600;
    private static final int LABEL_HEIGHT = 26;

    public static void main(String[] args) throws Exception {
        String folder = args.length > 0 ? args[0] : "shots";
        String output = args.length > 1 ? args[1] : folder + "/00-ALL-MODULES.png";

        List<File> files = new ArrayList<>();

        for (int i = 0; i <= 9; i++) {
            final String prefix = String.format("%02d-", i);
            File[] matches = new File(folder).listFiles((dir, name) ->
                    name.startsWith(prefix) && name.endsWith(".png"));
            if (matches != null && matches.length > 0) {
                files.add(matches[0]);
            }
        }

        if (files.isEmpty()) {
            System.out.println("[FAIL] no module screenshots found in " + folder);
            System.exit(1);
        }

        int rows = (int) Math.ceil(files.size() / (double) COLUMNS);
        int tileHeight = 0;
        List<BufferedImage> images = new ArrayList<>();
        for (File file : files) {
            BufferedImage image = ImageIO.read(file);
            images.add(image);
            tileHeight = Math.max(tileHeight, image.getHeight());
        }

        double scale = TILE_WIDTH / (double) images.get(0).getWidth();
        int scaledHeight = (int) Math.round(images.get(0).getHeight() * scale);

        int sheetWidth = COLUMNS * TILE_WIDTH + (COLUMNS + 1) * 10;
        int sheetHeight = rows * (scaledHeight + LABEL_HEIGHT) + (rows + 1) * 10;
        BufferedImage sheet = new BufferedImage(sheetWidth, sheetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = sheet.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setColor(new Color(0x0E1117));
        g2.fillRect(0, 0, sheetWidth, sheetHeight);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));

        for (int i = 0; i < images.size(); i++) {
            int column = i % COLUMNS;
            int row = i / COLUMNS;
            int x = 10 + column * (TILE_WIDTH + 10);
            int y = 10 + row * (scaledHeight + LABEL_HEIGHT + 10);

            g2.setColor(new Color(0xE6EDF3));
            g2.drawString(title(files.get(i).getName()), x + 4, y + 16);
            g2.drawImage(images.get(i), x, y + LABEL_HEIGHT, TILE_WIDTH, scaledHeight, null);
            g2.setColor(new Color(0x2A3441));
            g2.drawRect(x, y + LABEL_HEIGHT, TILE_WIDTH - 1, scaledHeight - 1);
        }
        g2.dispose();

        ImageIO.write(sheet, "png", new File(output));
        System.out.println("[OK] " + output + " (" + sheetWidth + "x" + sheetHeight + ")");
        System.exit(0);
    }

    private static String title(String fileName) {
        String name = fileName.replaceFirst("^\\d+-", "").replace(".png", "");
        return name.replace('_', ' ');
    }
}
