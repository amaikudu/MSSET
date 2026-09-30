package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.Alert;
import service.AnalyticsService;
import service.DataStore;
import service.MarketDataService;
import service.SurveillanceService;
import util.DarkTheme;
import util.DateTimeUtil;

public class DashboardPanel extends JPanel implements MarketDataService.MarketListener {

    private static final int COL_SEVERITY = 0;
    private static final int COL_SYMBOL = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_RISK = 3;
    private static final int COL_TIME = 4;
    private static final int COL_STATUS = 5;

    private final DataStore store;
    private final MarketDataService marketData;
    private final SurveillanceService surveillance;
    private final AnalyticsService analytics;

    private final JLabel systemStatus = DarkTheme.statusLabel("SYSTEM ONLINE", DarkTheme.GREEN);
    private final JLabel feedStatus = DarkTheme.statusLabel("FEED RUNNING", DarkTheme.GREEN);
    private final JLabel marketStatus = DarkTheme.statusLabel("MARKET OPEN", DarkTheme.GREEN);
    private final JLabel surveillanceStatus = DarkTheme.statusLabel("MONITORING ON", DarkTheme.GREEN);
    private final JLabel clockLabel = DarkTheme.bold("--:--:--");

    private final JToggleButton feedToggle = new JToggleButton("LIVE FEED", true);
    private final JToggleButton monitorToggle = new JToggleButton("SURVEILLANCE ON", true);

    private final JLabel ordersValue = DarkTheme.bold("0");
    private final JLabel executedValue = DarkTheme.bold("0");
    private final JLabel cancelledValue = DarkTheme.bold("0");
    private final JLabel alertsValue = DarkTheme.bold("0");
    private final JLabel suspiciousValue = DarkTheme.bold("0");
    private final JLabel avgRiskValue = DarkTheme.bold("0");
    private final JLabel instrumentsValue = DarkTheme.bold("20");
    private final JLabel ticksValue = DarkTheme.bold("0");

    private final JProgressBar alertLoadBar = new JProgressBar(0, 100);
    private final JProgressBar riskBar = new JProgressBar(0, 100);
    private final JProgressBar systemHealthBar = new JProgressBar(0, 100);
    private final JProgressBar volumeBar = new JProgressBar(0, 100);

    private final DefaultTableModel alertModel = new DefaultTableModel(
            new Object[] { "Severity", "Symbol", "Type", "Risk", "Time", "Status" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable alertTable = new JTable(alertModel);

    private final DefaultTableModel statModel = new DefaultTableModel(
            new Object[] { "Market Statistic", "Value" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable statTable = new JTable(statModel);

    private final JCheckBox autoRefreshCheck = new JCheckBox("Auto-refresh (1s)", true);

    private JButton alertsButton;
    private JButton marketDataButton;
    private JButton analyticsButton;
    private JButton reportsButton;
    private JButton rulesButton;

    public DashboardPanel() {
        this(new DataStore(), new MarketDataService(),
                null, null);
    }

    public DashboardPanel(DataStore store, MarketDataService marketData,
                          SurveillanceService surveillance, AnalyticsService analytics) {
        this.store = store;
        this.marketData = marketData;
        this.surveillance = surveillance;
        this.analytics = analytics;

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Dashboard",
                "System overview - status, key figures and the latest suspicious events"),
                BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        refresh();

        marketData.addListener(this);
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(DarkTheme.BG);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBackground(DarkTheme.BG);
        top.add(buildStatusRow(), BorderLayout.NORTH);
        top.add(buildKpiRow(), BorderLayout.CENTER);
        center.add(top, BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(10, 0));
        lower.setBackground(DarkTheme.BG);
        lower.add(buildAlertsCard(), BorderLayout.CENTER);

        JPanel tables = new JPanel(new GridLayout(2, 1, 0, 10));
        tables.setBackground(DarkTheme.BG);
        tables.add(buildStatsCard());
        tables.add(buildHealthCard());
        tables.setPreferredSize(new Dimension(330, 100));
        lower.add(tables, BorderLayout.EAST);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setBackground(DarkTheme.BG);
        actions.add(buildQuickActionsCard(), BorderLayout.NORTH);
        lower.add(actions, BorderLayout.WEST);

        center.add(lower, BorderLayout.CENTER);

        return center;
    }

    private JPanel buildStatusRow() {
        JPanel row = DarkTheme.card(new GridLayout(1, 4, 10, 0));

        row.add(statusBlock("System", systemStatus, "All services responding"));
        row.add(statusBlock("Market Feed", feedStatus, "Simulated ITCH feed"));
        row.add(statusBlock("Market Session", marketStatus, "09:30 - 16:00"));
        row.add(statusBlock("Surveillance", surveillanceStatus, "Rule engine active"));

        return row;
    }

    private JPanel statusBlock(String title, JLabel status, String note) {
        JPanel block = new JPanel(new BorderLayout(0, 2));
        block.setOpaque(false);
        block.add(DarkTheme.muted(title), BorderLayout.NORTH);
        block.add(status, BorderLayout.CENTER);
        block.add(DarkTheme.muted(note), BorderLayout.SOUTH);
        return block;
    }

    private JPanel buildKpiRow() {
        JPanel grid = new JPanel(new GridLayout(2, 4, 10, 10));
        grid.setBackground(DarkTheme.BG);

        grid.add(kpiCard("Orders Processed", ordersValue, DarkTheme.ACCENT, "All orders this session"));
        grid.add(kpiCard("Executed", executedValue, DarkTheme.GREEN, "Filled or partially filled"));
        grid.add(kpiCard("Cancelled", cancelledValue, DarkTheme.AMBER, "Cancelled before completion"));
        grid.add(kpiCard("Active Alerts", alertsValue, DarkTheme.RED, "Not yet closed"));
        grid.add(kpiCard("Suspicious Events", suspiciousValue, DarkTheme.RED, "Total detected"));
        grid.add(kpiCard("Average Risk", avgRiskValue, DarkTheme.PURPLE, "Mean risk score"));
        grid.add(kpiCard("Instruments", instrumentsValue, DarkTheme.TEXT, "Symbols on the feed"));
        grid.add(kpiCard("Feed Ticks", ticksValue, DarkTheme.TEXT, "Updates received"));

        return grid;
    }

    private JPanel kpiCard(String title, JLabel value, Color colour, String note) {
        JPanel card = DarkTheme.card(new BorderLayout(0, 2));

        JLabel titleLabel = DarkTheme.muted(title);
        value.setFont(DarkTheme.FONT_KPI);
        value.setForeground(colour);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(DarkTheme.muted(note), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAlertsCard() {
        JPanel card = DarkTheme.card("Latest suspicious events", new BorderLayout(0, 8));

        DarkTheme.styleTable(alertTable);
        alertTable.setRowHeight(24);
        alertTable.getColumnModel().getColumn(COL_SEVERITY).setCellRenderer(new SeverityRenderer());
        alertTable.getColumnModel().getColumn(COL_RISK).setCellRenderer(new RiskRenderer());
        DarkTheme.setColumnWidth(alertTable, COL_SEVERITY, 80);
        DarkTheme.setColumnWidth(alertTable, COL_SYMBOL, 60);
        DarkTheme.setColumnWidth(alertTable, COL_TYPE, 130);
        DarkTheme.setColumnWidth(alertTable, COL_RISK, 50);
        DarkTheme.setColumnWidth(alertTable, COL_TIME, 70);
        DarkTheme.setColumnWidth(alertTable, COL_STATUS, 105);

        card.add(DarkTheme.scroll(alertTable), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildHealthCard() {
        JPanel card = DarkTheme.card("System health", new GridLayout(0, 1, 0, 6));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        card.setPreferredSize(new Dimension(300, 145));

        systemHealthBar.setValue(98);
        systemHealthBar.setStringPainted(true);
        systemHealthBar.setString("System health 98%");
        systemHealthBar.setForeground(DarkTheme.GREEN);
        systemHealthBar.setBackground(DarkTheme.SURFACE_ALT);
        systemHealthBar.setBorderPainted(false);

        alertLoadBar.setStringPainted(true);
        alertLoadBar.setString("Queue load 40%");
        alertLoadBar.setForeground(DarkTheme.RED);
        alertLoadBar.setBackground(DarkTheme.SURFACE_ALT);
        alertLoadBar.setBorderPainted(false);

        riskBar.setStringPainted(true);
        riskBar.setString("Average risk 0%");
        riskBar.setForeground(DarkTheme.AMBER);
        riskBar.setBackground(DarkTheme.SURFACE_ALT);
        riskBar.setBorderPainted(false);

        volumeBar.setStringPainted(true);
        volumeBar.setString("Feed throughput 0%");
        volumeBar.setForeground(DarkTheme.ACCENT);
        volumeBar.setBackground(DarkTheme.SURFACE_ALT);
        volumeBar.setBorderPainted(false);

        card.add(systemHealthBar);
        card.add(alertLoadBar);
        card.add(riskBar);
        card.add(volumeBar);
        return card;
    }

    private JPanel buildQuickActionsCard() {
        JPanel card = DarkTheme.card("Quick actions", new GridLayout(4, 2, 6, 6));
        card.setPreferredSize(new Dimension(450, 170));

        JButton startFeed = DarkTheme.button("Start market feed", DarkTheme.GREEN);
        JButton stopFeed = DarkTheme.outlineButton("Stop market feed");
        alertsButton = DarkTheme.button("Open Alerts queue", DarkTheme.RED);
        marketDataButton = DarkTheme.outlineButton("Open Market Data");
        analyticsButton = DarkTheme.outlineButton("Open Charting & Analytics");
        reportsButton = DarkTheme.outlineButton("Generate audit report");
        rulesButton = DarkTheme.outlineButton("Open Rule Builder");

        startFeed.addActionListener(e -> {
            marketData.start();
            feedToggle.setSelected(true);
            feedToggle.setText("LIVE FEED");
            refresh();
        });

        stopFeed.addActionListener(e -> {
            marketData.stop();
            feedToggle.setSelected(false);
            feedToggle.setText("PAUSED");
            refresh();
        });

        card.add(startFeed);
        card.add(stopFeed);
        card.add(alertsButton);
        card.add(marketDataButton);
        card.add(analyticsButton);
        card.add(reportsButton);
        card.add(rulesButton);
        card.add(new JLabel());
        return card;
    }

    private JPanel buildStatsCard() {
        JPanel card = DarkTheme.card("Market statistics", new BorderLayout());

        DarkTheme.styleTable(statTable);
        statTable.setRowHeight(24);
        DarkTheme.alignRight(statTable, 1);
        card.add(DarkTheme.scroll(statTable), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(systemStatus);
        left.add(DarkTheme.muted("|  Last refresh:"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        feedToggle.setFont(DarkTheme.FONT_BOLD);
        feedToggle.setBackground(DarkTheme.SURFACE_ALT);
        feedToggle.setForeground(DarkTheme.TEXT);
        feedToggle.setFocusPainted(false);

        monitorToggle.setFont(DarkTheme.FONT_BOLD);
        monitorToggle.setBackground(DarkTheme.SURFACE_ALT);
        monitorToggle.setForeground(DarkTheme.TEXT);
        monitorToggle.setFocusPainted(false);

        autoRefreshCheck.setOpaque(false);
        autoRefreshCheck.setForeground(DarkTheme.TEXT);

        right.add(clockLabel);
        right.add(autoRefreshCheck);
        right.add(feedToggle);
        right.add(monitorToggle);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        feedToggle.addActionListener(e -> {
            if (feedToggle.isSelected()) {
                marketData.start();
                feedToggle.setText("LIVE FEED");
            } else {
                marketData.stop();
                feedToggle.setText("PAUSED");
            }
            refresh();
        });

        monitorToggle.addActionListener(e -> {
            if (surveillance == null) {
                return;
            }
            boolean on = monitorToggle.isSelected();
            surveillance.setMonitoring(on);
            monitorToggle.setText(on ? "SURVEILLANCE ON" : "SURVEILLANCE OFF");
            refresh();
        });

        if (surveillance != null) {
            surveillance.addAlertListener(event -> refresh());
        }
    }

    public void onOpenAlerts(Runnable action) {
        setAction(alertsButton, action, "Open Alerts queue", DarkTheme.RED);
    }

    public void onOpenMarketData(Runnable action) {
        setAction(marketDataButton, action, "Open Market Data", null);
    }

    public void onOpenAnalytics(Runnable action) {
        setAction(analyticsButton, action, "Open Charting & Analytics", null);
    }

    public void onOpenReports(Runnable action) {
        setAction(reportsButton, action, "Generate audit report", null);
    }

    public void onOpenRules(Runnable action) {
        setAction(rulesButton, action, "Open Rule Builder", null);
    }

    private void setAction(JButton button, Runnable action, String text, Color colour) {
        if (button == null) {
            return;
        }
        for (ActionListener old : button.getActionListeners()) {
            button.removeActionListener(old);
        }
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
        button.setText(text);
    }

    public void refresh() {
        boolean running = marketData.isRunning();
        feedStatus.setText(running ? "  \u25CF   FEED RUNNING" : "  \u25CF   FEED STOPPED");
        feedStatus.setForeground(running ? DarkTheme.GREEN : DarkTheme.AMBER);
        feedToggle.setSelected(running);
        feedToggle.setText(running ? "LIVE FEED" : "PAUSED");

        boolean monitoring = surveillance != null && surveillance.isMonitoring();
        surveillanceStatus.setText(monitoring
                ? "  \u25CF   MONITORING ON" : "  \u25CF   MONITORING OFF");
        surveillanceStatus.setForeground(monitoring ? DarkTheme.GREEN : DarkTheme.AMBER);
        monitorToggle.setSelected(monitoring);
        monitorToggle.setText(monitoring ? "SURVEILLANCE ON" : "SURVEILLANCE OFF");

        marketStatus.setText("  \u25CF   MARKET OPEN");
        marketStatus.setForeground(DarkTheme.GREEN);

        ordersValue.setText(String.format("%,d", marketData.getOrderCount()));
        executedValue.setText(String.format("%,d", marketData.getFilledOrderCount()));
        cancelledValue.setText(String.format("%,d", marketData.getCancelledOrderCount()));

        int totalAlerts = store.getAlertCount();
        int openAlerts = store.getOpenAlertCount();
        alertsValue.setText(String.format("%,d", openAlerts));
        suspiciousValue.setText(String.format("%,d", totalAlerts));

        double avgRisk = surveillance == null ? 0 : surveillance.getAverageRiskScore();
        avgRiskValue.setText(String.format("%.0f", avgRisk));

        instrumentsValue.setText(String.valueOf(marketData.getRows().size()));
        ticksValue.setText(String.format("%,d", marketData.getTickCount()));

        clockLabel.setText("Last refresh: " + DateTimeUtil.time(System.currentTimeMillis()));

        int maxQueue = 50;
        int queuePercent = (int) Math.min(100, openAlerts * 100L / maxQueue);
        alertLoadBar.setValue(queuePercent);
        alertLoadBar.setString("Queue load " + queuePercent + "%");

        riskBar.setValue((int) Math.round(avgRisk));
        riskBar.setString(String.format("Average risk %.0f%%", avgRisk));

        long totalVolume = marketData.getTotalVolume();
        int volumePercent = (int) Math.min(100, totalVolume / 200_000L);
        volumeBar.setValue(volumePercent);
        volumeBar.setString("Feed throughput " + volumePercent + "%");

        refreshAlertTable();
        refreshStatTable(totalVolume);
    }

    private void refreshAlertTable() {
        alertModel.setRowCount(0);
        List<Alert> alerts = store.getAlerts();
        int shown = 0;
        for (Alert alert : alerts) {
            alertModel.addRow(new Object[] {
                alert.getSeverity(),
                alert.getSymbol(),
                alert.getType(),
                alert.getRiskScore(),
                DateTimeUtil.time(alert.getTimestamp()),
                alert.getStatus()
            });
            shown++;
            if (shown >= 12) {
                break;
            }
        }
    }

    private void refreshStatTable(long totalVolume) {
        int[] breadth = marketData.getAdvanceDecline();

        statModel.setRowCount(0);
        statModel.addRow(new Object[] { "Traded volume", String.format("%,d", totalVolume) });
        statModel.addRow(new Object[] { "Advancing", String.valueOf(breadth[0]) });
        statModel.addRow(new Object[] { "Declining", String.valueOf(breadth[1]) });
        statModel.addRow(new Object[] { "Unchanged", String.valueOf(breadth[2]) });
        statModel.addRow(new Object[] { "Alerts (total)", String.valueOf(store.getAlertCount()) });
        statModel.addRow(new Object[] { "Alerts (new)", String.valueOf(store.getNewAlertCount()) });
        statModel.addRow(new Object[] { "Alerts (closed)", String.valueOf(store.getAlertCount()
                - store.getOpenAlertCount()) });
        statModel.addRow(new Object[] { "Reports generated", String.valueOf(store.getReportCount()) });
        statModel.addRow(new Object[] { "Rules defined", String.valueOf(store.getRuleCount()) });
        statModel.addRow(new Object[] { "Rules enabled", String.valueOf(store.getEnabledRuleCount()) });
        if (analytics != null) {
            statModel.addRow(new Object[] { "Events scanned",
                    String.format("%,d", surveillance == null ? 0 : surveillance.getEventsScanned()) });
        }
        statModel.addRow(new Object[] { "Feed ticks", String.format("%,d", marketData.getTickCount()) });
    }

    @Override
    public void onMarketUpdate(List<model.MarketRow> rows) {
        if (!autoRefreshCheck.isSelected()) {
            return;
        }
        refresh();
    }

    private static class SeverityRenderer extends DefaultTableCellRenderer {
        SeverityRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_SMALL);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String severity = String.valueOf(value);
                if ("CRITICAL".equals(severity)) {
                    c.setForeground(DarkTheme.RED);
                } else if ("HIGH".equals(severity)) {
                    c.setForeground(DarkTheme.AMBER);
                } else if ("MEDIUM".equals(severity)) {
                    c.setForeground(DarkTheme.PURPLE);
                } else {
                    c.setForeground(DarkTheme.GREEN);
                }
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }

    private static class RiskRenderer extends DefaultTableCellRenderer {
        RiskRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(DarkTheme.FONT_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground(DarkTheme.TEXT);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }
}
