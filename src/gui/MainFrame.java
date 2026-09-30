package gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Taskbar;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import service.AnalyticsService;
import service.DataStore;
import service.MarketDataService;
import service.ReportService;
import service.SurveillanceService;
import util.DarkTheme;
import util.DateTimeUtil;

public class MainFrame extends JPanel {

    private static final String WINDOW_TITLE =
            "Market Surveillance Engine for Electronic Trading";

    private static final String MODULE_DASHBOARD = "Dashboard";
    private static final String MODULE_MARKET_DATA = "Market Data";
    private static final String MODULE_ORDER_BOOK = "Order Book";
    private static final String MODULE_SURVEILLANCE = "Surveillance";
    private static final String MODULE_RULE_BUILDER = "Rule Builder";
    private static final String MODULE_ALERTS = "Alerts";
    private static final String MODULE_ANALYTICS = "Charting & Analytics";
    private static final String MODULE_REPORTS = "Audit Reports";
    private static final String MODULE_REPLAY = "Replay";
    private static final String MODULE_SETTINGS = "Settings";

    private static final String[][] MODULE_LIST = {
        { MODULE_DASHBOARD, "System overview and key figures" },
        { MODULE_MARKET_DATA, "Live simulated price feed" },
        { MODULE_ORDER_BOOK, "Market depth and order entry" },
        { MODULE_SURVEILLANCE, "Detection engine control" },
        { MODULE_RULE_BUILDER, "Build surveillance rules" },
        { MODULE_ALERTS, "Suspicious event queue" },
        { MODULE_ANALYTICS, "Charts and statistics" },
        { MODULE_REPORTS, "Audit report generation" },
        { MODULE_REPLAY, "Historical data replay" },
        { MODULE_SETTINGS, "Application preferences" }
    };

    private final DataStore dataStore;
    private final MarketDataService marketData;
    private final SurveillanceService surveillance;
    private final AnalyticsService analytics;
    private final ReportService reportService;

    private final DashboardPanel dashboardPanel;
    private final MarketDataPanel marketDataPanel;
    private final OrderBookPanel orderBookPanel;
    private final SurveillancePanel surveillancePanel;
    private final RuleBuilderPanel ruleBuilderPanel;
    private final AlertsPanel alertsPanel;
    private final AnalyticsPanel analyticsPanel;
    private final AuditReportPanel auditReportPanel;
    private final ReplayPanel replayPanel;
    private final SettingsPanel settingsPanel;

    private final JPanel cardHolder = new JPanel(new CardLayout());
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final List<JToggleButton> moduleToggles = new ArrayList<>();

    private final JLabel breadcrumbLabel = DarkTheme.bold(MODULE_DASHBOARD);
    private final JLabel breadcrumbHint = DarkTheme.muted("");
    private final JLabel clockLabel = DarkTheme.bold("--:--:--");
    private final JLabel feedLabel = DarkTheme.statusLabel("FEED RUNNING", DarkTheme.GREEN);
    private final JLabel monitorLabel = DarkTheme.statusLabel("MONITORING ON", DarkTheme.GREEN);
    private final JLabel alertLabel = DarkTheme.statusLabel("0 OPEN ALERTS", DarkTheme.AMBER);
    private final JLabel tickLabel = DarkTheme.muted("Ticks: 0");

    private final Timer clockTimer = new Timer(1000, e -> updateStatusBar());

    public MainFrame() {
        dataStore = new DataStore();
        marketData = new MarketDataService();
        surveillance = new SurveillanceService(dataStore, marketData);
        analytics = new AnalyticsService(marketData, dataStore);
        reportService = new ReportService(dataStore);

        dashboardPanel = new DashboardPanel(dataStore, marketData, surveillance, analytics);
        marketDataPanel = new MarketDataPanel(marketData);
        orderBookPanel = new OrderBookPanel(marketData);
        surveillancePanel = new SurveillancePanel(dataStore, marketData, surveillance);
        ruleBuilderPanel = new RuleBuilderPanel(dataStore);
        alertsPanel = new AlertsPanel(dataStore, surveillance);
        analyticsPanel = new AnalyticsPanel(analytics);
        auditReportPanel = new AuditReportPanel(reportService);
        replayPanel = new ReplayPanel();
        settingsPanel = new SettingsPanel(marketData, surveillance);

        setLayout(new BorderLayout());
        setBackground(DarkTheme.BG);

        JPanel content = buildContent();
        content.add(buildMenuBar(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);

        wirePanels();
        showModule(MODULE_DASHBOARD);

        marketData.start();
        surveillance.setMonitoring(true);
        clockTimer.start();
        updateStatusBar();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(DarkTheme.BG);
        content.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(DarkTheme.BG);
        right.add(buildTopBar(), BorderLayout.NORTH);

        cardHolder.setBackground(DarkTheme.BG);
        cardHolder.add(dashboardPanel, MODULE_DASHBOARD);
        cardHolder.add(marketDataPanel, MODULE_MARKET_DATA);
        cardHolder.add(orderBookPanel, MODULE_ORDER_BOOK);
        cardHolder.add(surveillancePanel, MODULE_SURVEILLANCE);
        cardHolder.add(ruleBuilderPanel, MODULE_RULE_BUILDER);
        cardHolder.add(alertsPanel, MODULE_ALERTS);
        cardHolder.add(analyticsPanel, MODULE_ANALYTICS);
        cardHolder.add(auditReportPanel, MODULE_REPORTS);
        cardHolder.add(replayPanel, MODULE_REPLAY);
        cardHolder.add(settingsPanel, MODULE_SETTINGS);
        right.add(cardHolder, BorderLayout.CENTER);

        right.add(buildStatusBar(), BorderLayout.SOUTH);
        content.add(right, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(DarkTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(228, 100));
        sidebar.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

        JLabel title = new JLabel("MARKET SURVEILLANCE");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(DarkTheme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(title);

        JLabel subtitle = new JLabel("Electronic Trading Engine");
        subtitle.setFont(DarkTheme.FONT_SMALL);
        subtitle.setForeground(DarkTheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(subtitle);

        sidebar.add(Box.createVerticalStrut(10));

        JSeparator separator = new JSeparator();
        separator.setForeground(DarkTheme.BORDER);
        separator.setBackground(DarkTheme.BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(separator);

        sidebar.add(Box.createVerticalStrut(10));

        JLabel navHeading = new JLabel("MODULES");
        navHeading.setFont(DarkTheme.FONT_SMALL);
        navHeading.setForeground(DarkTheme.TEXT_MUTED);
        navHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(navHeading);

        sidebar.add(Box.createVerticalStrut(6));

        for (String[] module : MODULE_LIST) {
            JButton button = createNavButton(module[0], module[1]);
            navButtons.put(module[0], button);
            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(3));
        }

        sidebar.add(Box.createVerticalGlue());

        JSeparator separator2 = new JSeparator();
        separator2.setForeground(DarkTheme.BORDER);
        separator2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator2.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(separator2);
        sidebar.add(Box.createVerticalStrut(8));

        JLabel team = new JLabel("Team: Pranav, Shivika, Vishal");
        team.setFont(DarkTheme.FONT_SMALL);
        team.setForeground(DarkTheme.TEXT_MUTED);
        team.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(team);

        return sidebar;
    }

    private JButton createNavButton(final String module, final String hint) {
        final Color normal = DarkTheme.SIDEBAR;
        final Color hover = DarkTheme.SURFACE_ALT;

        final JButton button = new JButton(module);
        button.setFont(DarkTheme.FONT_BASE);
        button.setForeground(DarkTheme.TEXT);
        button.setBackground(normal);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        button.setPreferredSize(new Dimension(200, 34));
        button.setToolTipText(hint);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!button.getBackground().equals(DarkTheme.ACCENT)) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!button.getBackground().equals(DarkTheme.ACCENT)) {
                    button.setBackground(normal);
                }
            }
        });

        button.addActionListener(e -> showModule(module));
        return button;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(DarkTheme.SURFACE_ALT);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 0));
        left.setOpaque(false);
        breadcrumbLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        breadcrumbHint.setFont(DarkTheme.FONT_SMALL);
        left.add(breadcrumbLabel);
        left.add(breadcrumbHint);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(tickLabel);
        right.add(feedLabel);
        right.add(monitorLabel);
        right.add(alertLabel);
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
        right.add(clockLabel);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(DarkTheme.SURFACE_ALT);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        JLabel left = new JLabel("Market Surveillance Engine  |  simulated market data  |  "
                + "standard Java Swing, no external libraries");
        left.setFont(DarkTheme.FONT_SMALL);
        left.setForeground(DarkTheme.TEXT_MUTED);

        JLabel right = new JLabel("Ctrl+1..0 switch modules   |   F5 refresh   |   "
                + "Data is simulated for the project review");
        right.setFont(DarkTheme.FONT_SMALL);
        right.setForeground(DarkTheme.TEXT_MUTED);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(DarkTheme.SIDEBAR);

        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh Current View");
        JMenuItem resetDataItem = new JMenuItem("Restore Demo Data");
        JMenuItem exitItem = new JMenuItem("Exit");
        refreshItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
        refreshItem.addActionListener(e -> refreshCurrent());
        resetDataItem.addActionListener(e -> resetDemoData());
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(refreshItem);
        fileMenu.add(resetDataItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        for (int i = 0; i < MODULE_LIST.length; i++) {
            final String module = MODULE_LIST[i][0];
            JMenuItem item = new JMenuItem(module);
            item.setAccelerator(KeyStroke.getKeyStroke(
                    "control " + (i == 9 ? "0" : String.valueOf(i + 1))));
            item.addActionListener(e -> showModule(module));
            viewMenu.add(item);
        }
        viewMenu.addSeparator();
        JMenuItem compactItem = new JMenuItem("Toggle Full Screen Style");
        compactItem.addActionListener(e -> toggleCompactLayout());
        viewMenu.add(compactItem);

        JMenu feedMenu = new JMenu("Feed");
        JMenuItem startFeed = new JMenuItem("Start Market Feed");
        JMenuItem stopFeed = new JMenuItem("Stop Market Feed");
        JMenuItem toggleMonitor = new JMenuItem("Toggle Surveillance Monitoring");
        startFeed.addActionListener(e -> {
            marketData.start();
            updateStatusBar();
        });
        stopFeed.addActionListener(e -> {
            marketData.stop();
            updateStatusBar();
        });
        toggleMonitor.addActionListener(e -> {
            surveillance.setMonitoring(!surveillance.isMonitoring());
            updateStatusBar();
        });
        feedMenu.add(startFeed);
        feedMenu.add(stopFeed);
        feedMenu.addSeparator();
        feedMenu.add(toggleMonitor);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        JMenuItem moduleItem = new JMenuItem("Module Guide");
        aboutItem.addActionListener(e -> showAbout());
        moduleItem.addActionListener(e -> showModuleGuide());
        helpMenu.add(aboutItem);
        helpMenu.add(moduleItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(feedMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void wirePanels() {
        dashboardPanel.onOpenAlerts(() -> showModule(MODULE_ALERTS));
        dashboardPanel.onOpenMarketData(() -> showModule(MODULE_MARKET_DATA));
        dashboardPanel.onOpenAnalytics(() -> showModule(MODULE_ANALYTICS));
        dashboardPanel.onOpenReports(() -> showModule(MODULE_REPORTS));
        dashboardPanel.onOpenRules(() -> showModule(MODULE_RULE_BUILDER));
        dashboardPanel.refresh();

        surveillancePanel.onOpenAlerts(() -> showModule(MODULE_ALERTS));
        orderBookPanel.onOrdersChanged(() -> {
            dashboardPanel.refresh();
            analyticsPanel.refreshView();
        });
    }

    public void showModule(String module) {
        ((CardLayout) cardHolder.getLayout()).show(cardHolder, module);

        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            JButton button = entry.getValue();
            boolean selected = entry.getKey().equals(module);
            button.setBackground(selected ? DarkTheme.ACCENT : DarkTheme.SIDEBAR);
            button.setForeground(selected ? Color.WHITE : DarkTheme.TEXT);
            button.setFont(selected ? DarkTheme.FONT_BOLD : DarkTheme.FONT_BASE);
        }

        for (String[] entry : MODULE_LIST) {
            if (entry[0].equals(module)) {
                breadcrumbLabel.setText(module);
                breadcrumbHint.setText(entry[1]);
                break;
            }
        }

        refreshModule(module);
        updateStatusBar();
    }

    private void refreshModule(String module) {
        if (MODULE_DASHBOARD.equals(module)) {
            dashboardPanel.refresh();
        } else if (MODULE_ORDER_BOOK.equals(module)) {
            orderBookPanel.refresh();
        } else if (MODULE_SURVEILLANCE.equals(module)) {
            surveillancePanel.refresh();
        }
    }

    private void refreshCurrent() {
        String module = breadcrumbLabel.getText();
        refreshModule(module);
        updateStatusBar();
    }

    private void updateStatusBar() {
        clockLabel.setText(DateTimeUtil.time(System.currentTimeMillis()));

        boolean running = marketData.isRunning();
        feedLabel.setText(running ? "  \u25CF   FEED RUNNING" : "  \u25CF   FEED STOPPED");
        feedLabel.setForeground(running ? DarkTheme.GREEN : DarkTheme.AMBER);

        boolean monitoring = surveillance.isMonitoring();
        monitorLabel.setText(monitoring ? "  \u25CF   MONITORING ON" : "  \u25CF   MONITORING OFF");
        monitorLabel.setForeground(monitoring ? DarkTheme.GREEN : DarkTheme.AMBER);

        int open = dataStore.getOpenAlertCount();
        alertLabel.setText(String.format("  \u25CF   %d OPEN ALERTS", open));
        alertLabel.setForeground(open > 0 ? DarkTheme.RED : DarkTheme.GREEN);

        tickLabel.setText(String.format("Ticks: %,d", marketData.getTickCount()));
    }

    private void toggleCompactLayout() {
        Dimension current = getSize();
        java.awt.Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.setSize(current.width > 1500
                    ? new Dimension(1280, 800) : new Dimension(1560, 960));
            window.setLocationRelativeTo(null);
        } else {
            setSize(current.width > 1500
                    ? new Dimension(1280, 800) : new Dimension(1560, 960));
        }
    }

    private void resetDemoData() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Restore the demo data?\n\n"
              + "This clears all alerts, audit reports and rules, then recreates\n"
              + "the original demonstration set.",
                "Restore demo data", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        dataStore.resetDemoData();
        alertsPanel.refreshView();
        auditReportPanel.refreshView();
        ruleBuilderPanel.refreshView();
        dashboardPanel.refresh();
        updateStatusBar();
        JOptionPane.showMessageDialog(this, "Demo data restored.",
                "Market Surveillance Engine", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "MARKET SURVEILLANCE ENGINE FOR ELECTRONIC TRADING\n"
              + "==================================================\n"
              + "Version      : First review build\n"
              + "Java         : " + System.getProperty("java.version") + "\n"
              + "Look and feel: " + javax.swing.UIManager.getLookAndFeel().getName() + "\n"
              + "Modules      : 10\n"
              + "Data source  : simulated (no database, no external library)\n"
              + "--------------------------------------------------\n"
              + "A desktop market-surveillance and trading-analytics\n"
              + "terminal built entirely with Java Swing.\n"
              + "--------------------------------------------------\n"
              + "Team         : Pranav, Shivika, Vishal",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showModuleGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("MODULE GUIDE\n");
        sb.append("==================================================\n");
        int number = 1;
        for (String[] module : MODULE_LIST) {
            sb.append(String.format("%2d. %-22s %s%n", number++, module[0], module[1]));
        }
        sb.append("==================================================\n");
        sb.append("Use the sidebar, the View menu, or press Ctrl+1 to Ctrl+0\n");
        sb.append("to switch between modules.\n");

        JOptionPane.showMessageDialog(this, sb.toString(),
                "Module Guide", JOptionPane.INFORMATION_MESSAGE);
    }

    public void shutdown() {
        clockTimer.stop();
        marketData.stop();
        surveillance.setMonitoring(false);
        analytics.detach();
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public MarketDataService getMarketDataService() {
        return marketData;
    }

    public SurveillanceService getSurveillanceService() {
        return surveillance;
    }

    public AnalyticsService getAnalyticsService() {
        return analytics;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public void layoutTree() {
        layoutRecursively(this);
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layoutRecursively((Container) child);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::openWindow);
    }

    public static void openWindow() {
        DarkTheme.install();

        final MainFrame content = new MainFrame();
        final JFrame frame = new JFrame(WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1240, 760));
        frame.setSize(1560, 960);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(content);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                content.shutdown();
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
        bringToFront(frame);
    }

    private static void bringToFront(final JFrame frame) {
        frame.setAlwaysOnTop(true);
        frame.toFront();

        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.USER_ATTENTION_WINDOW)) {
                try {
                    taskbar.requestWindowUserAttention(frame);
                } catch (Exception ignored) {
                    ignored.getMessage();
                }
            }
        }

        Timer attention = new Timer(500, null);
        attention.addActionListener(new ActionListener() {
            private int attempts = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                attempts++;
                if (attempts < 4) {
                    frame.toFront();
                    frame.requestFocus();
                    return;
                }
                frame.setAlwaysOnTop(false);
                frame.requestFocus();
                ((Timer) e.getSource()).stop();
            }
        });
        attention.setRepeats(true);
        attention.start();
    }
}
