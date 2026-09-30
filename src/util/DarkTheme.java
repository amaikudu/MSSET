package util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public final class DarkTheme {

    public static final Color BG          = new Color(0x0E1117);
    public static final Color SURFACE     = new Color(0x161B22);
    public static final Color SURFACE_ALT = new Color(0x1C2230);
    public static final Color SIDEBAR     = new Color(0x0B0E14);
    public static final Color BORDER      = new Color(0x2A3441);
    public static final Color TEXT        = new Color(0xE6EDF3);
    public static final Color TEXT_MUTED  = new Color(0x8B949E);
    public static final Color ACCENT      = new Color(0x1F6FEB);
    public static final Color ACCENT_DARK = new Color(0x1A5FCC);

    public static final Color GREEN       = new Color(0x3FB950);
    public static final Color RED         = new Color(0xF85149);
    public static final Color AMBER       = new Color(0xD29922);
    public static final Color PURPLE      = new Color(0xBC8CFF);

    public static final Font FONT_BASE  = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_BOLD  = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_MONO  = new Font("Monospaced", Font.PLAIN, 12);
    public static final Font FONT_KPI   = new Font("SansSerif", Font.BOLD, 22);

    public static final int FIELD_HEIGHT = 28;
    public static final int ROW_HEIGHT   = 26;

    private DarkTheme() {

    }

    public static void install() {

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {

        }

        UIManager.put("control", SURFACE);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusBase", SURFACE);
        UIManager.put("nimbusLightBackground", SURFACE);
        UIManager.put("text", TEXT);
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", FONT_BASE);
        UIManager.put("Button.font", FONT_BOLD);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.background", ACCENT);
        UIManager.put("ToggleButton.font", FONT_BOLD);
        UIManager.put("ToggleButton.foreground", TEXT);
        UIManager.put("ToggleButton.background", SURFACE_ALT);
        UIManager.put("CheckBox.font", FONT_BASE);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("CheckBox.background", SURFACE);
        UIManager.put("RadioButton.font", FONT_BASE);
        UIManager.put("RadioButton.foreground", TEXT);
        UIManager.put("RadioButton.background", SURFACE);
        UIManager.put("ComboBox.font", FONT_BASE);
        UIManager.put("ComboBox.background", SURFACE_ALT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", ACCENT);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("List.font", FONT_BASE);
        UIManager.put("List.background", SURFACE_ALT);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", ACCENT);
        UIManager.put("List.selectionForeground", Color.WHITE);
        UIManager.put("TextField.font", FONT_BASE);
        UIManager.put("TextField.background", SURFACE_ALT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.selectionBackground", ACCENT);
        UIManager.put("TextArea.font", FONT_MONO);
        UIManager.put("TextArea.background", SURFACE_ALT);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", TEXT);
        UIManager.put("TextArea.selectionBackground", ACCENT);
        UIManager.put("Spinner.font", FONT_BASE);
        UIManager.put("Spinner.background", SURFACE_ALT);
        UIManager.put("Spinner.foreground", TEXT);
        UIManager.put("Table.font", FONT_BASE);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", ACCENT);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", SURFACE_ALT);
        UIManager.put("TableHeader.foreground", TEXT_MUTED);
        UIManager.put("TableHeader.font", FONT_BOLD);
        UIManager.put("TabbedPane.font", FONT_BOLD);
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", SURFACE_ALT);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("MenuBar.background", SIDEBAR);
        UIManager.put("MenuBar.foreground", TEXT);
        UIManager.put("Menu.font", FONT_BASE);
        UIManager.put("Menu.foreground", TEXT);
        UIManager.put("Menu.background", SIDEBAR);
        UIManager.put("MenuItem.font", FONT_BASE);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("MenuItem.background", SURFACE);
        UIManager.put("PopupMenu.background", SURFACE);
        UIManager.put("PopupMenu.foreground", TEXT);
        UIManager.put("ToolBar.background", SURFACE_ALT);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("Separator.foreground", BORDER);
        UIManager.put("Separator.background", BORDER);
        UIManager.put("Slider.background", SURFACE);
        UIManager.put("Slider.foreground", ACCENT);
        UIManager.put("ProgressBar.background", SURFACE_ALT);
        UIManager.put("ProgressBar.foreground", GREEN);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
        UIManager.put("ProgressBar.selectionBackground", Color.WHITE);
        UIManager.put("Tree.background", SURFACE_ALT);
        UIManager.put("Tree.foreground", TEXT);
        UIManager.put("Tree.textBackground", SURFACE_ALT);
        UIManager.put("Tree.textForeground", TEXT);
        UIManager.put("Tree.selectionBackground", ACCENT);
        UIManager.put("Tree.selectionForeground", Color.WHITE);
        UIManager.put("Tree.hash", BORDER);
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.track", SURFACE);
        UIManager.put("FileChooser.background", SURFACE);
        UIManager.put("FileChooser.foreground", TEXT);
        UIManager.put("OptionPane.buttonFont", FONT_BOLD);
        UIManager.put("ToolTip.background", SURFACE_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    public static Border plainCardBorder() {
        return BorderFactory.createLineBorder(BORDER, 1);
    }

    public static Border fieldBorder() {
        return BorderFactory.createLineBorder(BORDER, 1);
    }

    public static Border titleBorder(String title) {
        Border line = BorderFactory.createLineBorder(BORDER, 1);
        Border text = BorderFactory.createTitledBorder(line, " " + title + " ");
        return BorderFactory.createCompoundBorder(text, BorderFactory.createEmptyBorder(2, 4, 6, 4));
    }

    public static JPanel panel() {
        JPanel p = new JPanel();
        p.setBackground(BG);
        return p;
    }

    public static JPanel panel(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(BG);
        return p;
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(cardBorder());
        return p;
    }

    public static JPanel card(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(SURFACE);
        p.setBorder(cardBorder());
        return p;
    }

    public static JPanel card(String title, java.awt.LayoutManager layout) {
        JPanel p = card(layout);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER, 1), " " + title + " "),
                BorderFactory.createEmptyBorder(4, 8, 8, 8)));
        return p;
    }

    public static JPanel header(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(FONT_SMALL);
        subLabel.setForeground(TEXT_MUTED);

        p.add(titleLabel, BorderLayout.NORTH);
        p.add(subLabel, BorderLayout.SOUTH);
        return p;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BASE);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BOLD);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    public static JLabel pill(String text, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(FONT_SMALL);
        l.setForeground(color);
        l.setOpaque(true);
        l.setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return l;
    }

    public static JLabel statusLabel(String text, Color color) {
        JLabel l = new JLabel("  \u25CF   " + text);
        l.setFont(FONT_BOLD);
        l.setForeground(color);
        return l;
    }

    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        styleField(f);
        return f;
    }

    public static JTextField field(String text, int columns) {
        JTextField f = new JTextField(text, columns);
        styleField(f);
        return f;
    }

    public static JTextField searchField(String placeholder, int columns) {
        JTextField f = field(columns);
        f.putClientProperty("dsh.placeholder", placeholder);
        return f;
    }

    public static String readField(JTextField field) {
        if (field == null) {
            return "";
        }
        String text = field.getText();
        if (text == null) {
            return "";
        }
        Object placeholder = field.getClientProperty("dsh.placeholder");
        if (placeholder != null && text.equals(placeholder.toString())) {
            return "";
        }
        return text;
    }

    public static void styleField(JTextField f) {
        f.setFont(FONT_BASE);
        f.setBackground(SURFACE_ALT);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, FIELD_HEIGHT));
    }

    public static JSpinner spinner(int min, int max, int step, int value) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        s.setFont(FONT_BASE);
        s.setPreferredSize(new Dimension(90, FIELD_HEIGHT));
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(SURFACE_ALT);
            tf.setForeground(TEXT);
            tf.setCaretColor(TEXT);
            tf.setFont(FONT_BASE);
            tf.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        }
        return s;
    }

    public static JSpinner doubleSpinner(double min, double max, double step, double value) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        s.setFont(FONT_BASE);
        s.setPreferredSize(new Dimension(110, FIELD_HEIGHT));
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(SURFACE_ALT);
            tf.setForeground(TEXT);
            tf.setCaretColor(TEXT);
            tf.setFont(FONT_MONO);
            tf.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        }
        return s;
    }

    public static JButton button(String text) {
        return button(text, ACCENT);
    }

    public static JButton button(String text, Color background) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setForeground(Color.WHITE);
        b.setBackground(background);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        final Color normal = background;
        final Color hover = background.brighter();
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(normal);
            }
        });
        return b;
    }

    public static JButton outlineButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BOLD);
        b.setForeground(TEXT);
        b.setBackground(SURFACE_ALT);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(5, 13, 5, 13)));
        return b;
    }

    public static void styleTable(JTable table) {
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setFont(FONT_BASE);
        table.setGridColor(BORDER);
        table.setRowHeight(ROW_HEIGHT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(SURFACE_ALT);
        header.setForeground(TEXT_MUTED);
        header.setFont(FONT_BOLD);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(10, 30));
    }

    public static void stripeRows(JTable table) {
        final TableCellRenderer defaultRenderer = table.getDefaultRenderer(Object.class);
        table.setDefaultRenderer(Object.class, new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = defaultRenderer.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                    c.setForeground(TEXT);
                }
                return c;
            }
        });
    }

    public static void alignRight(JTable table, int columnIndex) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        r.setFont(FONT_MONO);
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(r);
    }

    public static void alignCenter(JTable table, int columnIndex) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(r);
    }

    public static void setColumnWidth(JTable table, int columnIndex, int width) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(width);
        column.setMinWidth(Math.min(width, 40));
    }

    public static JSplitPane split(Component left, Component right, int leftWidth) {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setBackground(BG);
        split.setDividerSize(8);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(false);

        split.setResizeWeight(0.0);
        split.setDividerLocation(leftWidth);

        left.setMinimumSize(new Dimension(Math.min(leftWidth, 120), 100));
        right.setMinimumSize(new Dimension(320, 100));

        if (split.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            javax.swing.plaf.basic.BasicSplitPaneDivider divider =
                    ((javax.swing.plaf.basic.BasicSplitPaneUI) split.getUI()).getDivider();
            divider.setBackground(BORDER);
        }
        return split;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(SURFACE);
        sp.setBackground(SURFACE);
        JScrollBar bar = sp.getVerticalScrollBar();
        bar.setBackground(SURFACE);
        bar.setUnitIncrement(16);
        return sp;
    }

    public static Component vgap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    public static Component hgap(int width) {
        return Box.createRigidArea(new Dimension(width, 0));
    }

    public static JPanel spacer(int height) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, height));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return p;
    }

    public static JPanel fieldBlock(String labelText, Component component) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(muted(labelText), BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color fromHex(String text, Color fallback) {
        try {
            return Color.decode(text.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean canShowDialogs() {
        return !java.awt.GraphicsEnvironment.isHeadless();
    }

    public static void info(Component parent, Object message, String title) {
        if (canShowDialogs()) {
            JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
        } else {
            System.out.println("[dialog] " + title + ": "
                    + String.valueOf(message).replace('\n', ' '));
        }
    }
}
