package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.Alert;
import service.DataStore;
import service.MarketDataService;
import service.SurveillanceService;
import util.DarkTheme;
import util.DateTimeUtil;
import util.DemoData;

public class SurveillancePanel extends JPanel
        implements SurveillanceService.MonitoringListener, ChangeListener {

    private static final int COL_TIME = 0;
    private static final int COL_SYMBOL = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_RISK = 3;
    private static final int COL_SEVERITY = 4;

    private static final String[] WINDOWS = {
        "1 second", "5 seconds", "10 seconds", "30 seconds", "1 minute", "5 minutes"
    };

    private final DataStore store;
    private final MarketDataService marketData;
    private final SurveillanceService surveillance;

    private final JToggleButton monitoringToggle = new JToggleButton("MONITORING OFF");
    private final JComboBox<String> detectionBox = new JComboBox<>(DemoData.detectionTypes());
    private final JComboBox<String> windowBox = new JComboBox<>(WINDOWS);
    private final JSlider sensitivitySlider = new JSlider(1, 100, 60);
    private final JLabel sensitivityValue = DarkTheme.bold("60");
    private final JSpinner thresholdSpinner = DarkTheme.spinner(1, 100, 1, 30);
    private final JSpinner windowSpinner = DarkTheme.spinner(1, 5000, 10, 100);
    private final JButton startButton = DarkTheme.button("Start Monitoring", DarkTheme.GREEN);
    private final JButton stopButton = DarkTheme.button("Stop Monitoring", DarkTheme.RED);
    private final JButton resetStatsButton = DarkTheme.outlineButton("Reset Statistics");
    private final JCheckBox autoScrollCheck = new JCheckBox("Auto-scroll activity log", true);
    private final JCheckBox soundCheck = new JCheckBox("Beep on new alert");
    private final JCheckBox logEverythingCheck = new JCheckBox("Log every market scan");

    private final JLabel statusLabel = DarkTheme.statusLabel("SURVEILLANCE IDLE", DarkTheme.TEXT_MUTED);
    private final JLabel alertsGeneratedValue = DarkTheme.bold("0");
    private final JLabel eventsScannedValue = DarkTheme.bold("0");
    private final JLabel suppressedValue = DarkTheme.bold("0");
    private final JLabel openAlertsValue = DarkTheme.bold("0");
    private final JLabel avgRiskValue = DarkTheme.bold("0");
    private final JLabel maxRiskValue = DarkTheme.bold("0");

    private final JProgressBar sensitivityBar = new JProgressBar(0, 100);
    private final JProgressBar detectionRateBar = new JProgressBar(0, 100);
    private final JProgressBar queueBar = new JProgressBar(0, 100);

    private final DefaultListModel<String> activityModel = new DefaultListModel<>();
    private final JList<String> activityList = new JList<>(activityModel);

    private final DefaultTableModel recentAlertsModel = new DefaultTableModel(
            new Object[] { "Time", "Symbol", "Type", "Risk", "Severity" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable recentAlertsTable = new JTable(recentAlertsModel);

    private final JTextArea ruleSummaryArea = new JTextArea();

    private int alertsGenerated;
    private int lastKnownAlertCount;

    private JButton openAlertsButton;
    private Runnable onOpenAlerts;

    public SurveillancePanel() {
        this(new DataStore(), new MarketDataService(), null);
    }

    public SurveillancePanel(DataStore store, MarketDataService marketData,
                             SurveillanceService surveillance) {
        this.store = store;
        this.marketData = marketData;
        this.surveillance = surveillance;
        this.lastKnownAlertCount = store.getAlertCount();

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Surveillance",
                "Monitoring control - detection type, sensitivity, risk threshold and live scan activity"),
                BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        loadFromService();
        refresh();

        if (surveillance != null) {
            surveillance.addMonitoringListener(this);
            surveillance.addAlertListener(this);
        }
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(DarkTheme.BG);
        center.add(buildControlCard(), BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 6, 10, 0));
        stats.setBackground(DarkTheme.BG);
        stats.add(statCard("Alerts Generated", alertsGeneratedValue, DarkTheme.RED, "By this engine"));
        stats.add(statCard("Events Scanned", eventsScannedValue, DarkTheme.ACCENT, "Market events examined"));
        stats.add(statCard("Below Threshold", suppressedValue, DarkTheme.AMBER, "Found but filtered out"));
        stats.add(statCard("Open Alerts", openAlertsValue, DarkTheme.AMBER, "Awaiting review"));
        stats.add(statCard("Average Risk", avgRiskValue, DarkTheme.PURPLE, "Mean score"));
        stats.add(statCard("Highest Risk", maxRiskValue, DarkTheme.RED, "Worst case seen"));

        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.setBackground(DarkTheme.BG);
        middle.add(stats, BorderLayout.NORTH);
        middle.add(buildActivitySplit(), BorderLayout.CENTER);

        center.add(middle, BorderLayout.CENTER);
        return center;
    }

    private JPanel statCard(String title, JLabel value, java.awt.Color colour, String note) {
        JPanel card = DarkTheme.card(new BorderLayout(0, 2));
        value.setFont(DarkTheme.FONT_KPI);
        value.setForeground(colour);
        card.add(DarkTheme.muted(title), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(DarkTheme.muted(note), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildControlCard() {
        JPanel card = DarkTheme.card(new BorderLayout(0, 10));

        JPanel topRow = new JPanel(new GridLayout(1, 4, 10, 0));
        topRow.setOpaque(false);

        monitoringToggle.setFont(DarkTheme.FONT_BOLD);
        monitoringToggle.setBackground(DarkTheme.SURFACE_ALT);
        monitoringToggle.setForeground(DarkTheme.TEXT);
        monitoringToggle.setFocusPainted(false);
        topRow.add(titledBlock("Monitoring", monitoringToggle));

        detectionBox.setPreferredSize(null);
        topRow.add(titledBlock("Detection type", detectionBox));

        windowBox.setPreferredSize(null);
        topRow.add(titledBlock("Analysis window", windowBox));

        JPanel buttons = new JPanel(new GridLayout(2, 1, 6, 6));
        buttons.setOpaque(false);
        buttons.add(startButton);
        buttons.add(stopButton);
        topRow.add(titledBlock("Control", buttons));

        card.add(topRow, BorderLayout.NORTH);

        JPanel secondRow = new JPanel(new GridLayout(1, 3, 10, 0));
        secondRow.setOpaque(false);

        JPanel sensitivityPanel = new JPanel(new BorderLayout(6, 0));
        sensitivityPanel.setOpaque(false);
        sensitivitySlider.setBackground(DarkTheme.SURFACE);
        sensitivitySlider.setMajorTickSpacing(25);
        sensitivitySlider.setMinorTickSpacing(5);
        sensitivitySlider.setPaintTicks(true);
        sensitivitySlider.setPaintLabels(true);
        sensitivityPanel.add(sensitivitySlider, BorderLayout.CENTER);
        sensitivityValue.setPreferredSize(new Dimension(36, 20));
        sensitivityPanel.add(sensitivityValue, BorderLayout.EAST);
        secondRow.add(titledBlock("Detection sensitivity", sensitivityPanel));

        JPanel numbers = new JPanel(new GridLayout(1, 2, 8, 0));
        numbers.setOpaque(false);
        thresholdSpinner.setPreferredSize(null);
        windowSpinner.setPreferredSize(null);
        numbers.add(titledBlock("Risk threshold", thresholdSpinner));
        numbers.add(titledBlock("Minimum size", windowSpinner));
        secondRow.add(numbers);

        JPanel checks = new JPanel(new GridLayout(3, 1, 0, 2));
        checks.setOpaque(false);
        for (JCheckBox box : new JCheckBox[] { autoScrollCheck, soundCheck, logEverythingCheck }) {
            box.setOpaque(false);
            box.setForeground(DarkTheme.TEXT);
            checks.add(box);
        }
        secondRow.add(titledBlock("Options", checks));

        card.add(secondRow, BorderLayout.CENTER);
        return card;
    }

    private JPanel titledBlock(String caption, Component component) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        panel.add(DarkTheme.muted(caption), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActivitySplit() {
        JPanel logCard = DarkTheme.card("Activity log", new BorderLayout(0, 6));

        activityList.setBackground(DarkTheme.SURFACE_ALT);
        activityList.setForeground(DarkTheme.TEXT);
        activityList.setFont(DarkTheme.FONT_MONO);
        activityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane logScroll = DarkTheme.scroll(activityList);
        logScroll.setPreferredSize(new Dimension(560, 260));
        logCard.add(logScroll, BorderLayout.CENTER);

        JPanel logButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        logButtons.setOpaque(false);
        JButton clearLog = DarkTheme.outlineButton("Clear log");
        clearLog.addActionListener(e -> activityModel.clear());
        logButtons.add(clearLog);
        logCard.add(logButtons, BorderLayout.SOUTH);

        JPanel rightCard = DarkTheme.card("Detections in this session", new BorderLayout(0, 6));

        DarkTheme.styleTable(recentAlertsTable);
        recentAlertsTable.setRowHeight(22);
        recentAlertsTable.getColumnModel().getColumn(COL_RISK)
                .setCellRenderer(new RiskRenderer());
        recentAlertsTable.getColumnModel().getColumn(COL_SEVERITY)
                .setCellRenderer(new SeverityRenderer());
        recentAlertsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = DarkTheme.scroll(recentAlertsTable);
        tableScroll.setPreferredSize(new Dimension(380, 200));
        rightCard.add(tableScroll, BorderLayout.CENTER);

        ruleSummaryArea.setEditable(false);
        ruleSummaryArea.setFont(DarkTheme.FONT_MONO);
        ruleSummaryArea.setBackground(DarkTheme.SURFACE_ALT);
        ruleSummaryArea.setForeground(DarkTheme.TEXT);
        ruleSummaryArea.setRows(5);
        rightCard.add(DarkTheme.scroll(ruleSummaryArea), BorderLayout.SOUTH);

        JSplitPane split = DarkTheme.split(logCard, rightCard, 620);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(DarkTheme.BG);
        wrapper.add(split, BorderLayout.CENTER);

        JPanel bars = DarkTheme.card(new GridLayout(1, 3, 10, 0));
        sensitivityBar.setStringPainted(true);
        sensitivityBar.setForeground(DarkTheme.ACCENT);
        sensitivityBar.setBackground(DarkTheme.SURFACE_ALT);
        sensitivityBar.setBorderPainted(false);

        detectionRateBar.setStringPainted(true);
        detectionRateBar.setForeground(DarkTheme.AMBER);
        detectionRateBar.setBackground(DarkTheme.SURFACE_ALT);
        detectionRateBar.setBorderPainted(false);

        queueBar.setStringPainted(true);
        queueBar.setForeground(DarkTheme.RED);
        queueBar.setBackground(DarkTheme.SURFACE_ALT);
        queueBar.setBorderPainted(false);

        bars.add(sensitivityBar);
        bars.add(detectionRateBar);
        bars.add(queueBar);

        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setBackground(DarkTheme.BG);
        outer.add(wrapper, BorderLayout.CENTER);
        outer.add(bars, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(DarkTheme.muted("|  Engine reads the live market feed every tick"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        openAlertsButton = DarkTheme.button("Open Alerts Queue");
        right.add(openAlertsButton);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        monitoringToggle.addActionListener(e -> toggleMonitoring());

        startButton.addActionListener(e -> {
            if (surveillance == null) {
                return;
            }
            surveillance.startMonitoring();
            appendLog("Monitoring started - detection: " + detectionBox.getSelectedItem());
        });

        stopButton.addActionListener(e -> {
            if (surveillance == null) {
                return;
            }
            surveillance.stopMonitoring();
            appendLog("Monitoring stopped by operator");
        });

        detectionBox.addActionListener(e -> {
            if (surveillance != null) {
                surveillance.setDetectionType(String.valueOf(detectionBox.getSelectedItem()));
            }
            appendLog("Detection type changed to: " + detectionBox.getSelectedItem());
        });

        sensitivitySlider.addChangeListener(e -> {
            int value = sensitivitySlider.getValue();
            sensitivityValue.setText(String.valueOf(value));
            if (surveillance != null) {
                surveillance.setSensitivity(value);
            }
            refreshBars();
        });

        thresholdSpinner.addChangeListener(e -> {
            if (surveillance != null) {
                surveillance.setRiskThreshold((Integer) thresholdSpinner.getValue());
            }
            refresh();
        });

        windowBox.addActionListener(e -> appendLog("Analysis window: " + windowBox.getSelectedItem()));

        resetStatsButton.addActionListener(e -> {
            alertsGenerated = 0;
            activityModel.clear();
            appendLog("Statistics reset");
            refresh();
        });

        openAlertsButton.addActionListener(e -> {
            if (onOpenAlerts != null) {
                onOpenAlerts.run();
            }
        });
    }

    private void toggleMonitoring() {
        if (surveillance == null) {
            return;
        }
        if (surveillance.isMonitoring()) {
            surveillance.stopMonitoring();
            appendLog("Monitoring stopped");
        } else {
            surveillance.startMonitoring();
            appendLog("Monitoring started");
        }
    }

    public void onOpenAlerts(Runnable action) {
        this.onOpenAlerts = action;
        if (openAlertsButton != null && action != null) {
            for (ActionListener old : openAlertsButton.getActionListeners()) {
                openAlertsButton.removeActionListener(old);
            }
            openAlertsButton.addActionListener(e -> action.run());
        }
    }

    private void loadFromService() {
        if (surveillance == null) {
            appendLog("Surveillance service not attached");
            return;
        }
        sensitivitySlider.setValue(surveillance.getSensitivity());
        sensitivityValue.setText(String.valueOf(surveillance.getSensitivity()));
        thresholdSpinner.setValue(surveillance.getRiskThreshold());
        detectionBox.setSelectedItem(surveillance.getDetectionType());
        appendLog("Surveillance engine ready - detection: " + surveillance.getDetectionType());
    }

    private void appendLog(String message) {
        activityModel.add(0, DateTimeUtil.time(System.currentTimeMillis()) + "   " + message);
        while (activityModel.size() > 120) {
            activityModel.remove(activityModel.size() - 1);
        }
        if (autoScrollCheck.isSelected() && activityModel.size() > 0) {
            activityList.ensureIndexIsVisible(0);
        }
    }

    public void refresh() {
        if (surveillance == null) {
            return;
        }

        boolean monitoring = surveillance.isMonitoring();
        monitoringToggle.setSelected(monitoring);
        monitoringToggle.setText(monitoring ? "MONITORING ON" : "MONITORING OFF");
        monitoringToggle.setForeground(monitoring ? DarkTheme.GREEN : DarkTheme.TEXT);

        statusLabel.setText(monitoring
                ? "  \u25CF   MONITORING ACTIVE" : "  \u25CF   SURVEILLANCE IDLE");
        statusLabel.setForeground(monitoring ? DarkTheme.GREEN : DarkTheme.TEXT_MUTED);

        eventsScannedValue.setText(String.format("%,d", surveillance.getEventsScanned()));
        suppressedValue.setText(String.format("%,d", surveillance.getSuppressedByThreshold()));
        openAlertsValue.setText(String.format("%,d", store.getOpenAlertCount()));
        avgRiskValue.setText(String.format("%.0f", surveillance.getAverageRiskScore()));
        maxRiskValue.setText(String.valueOf(surveillance.getMaxRiskScore()));

        alertsGeneratedValue.setText(String.format("%,d",
                Math.max(alertsGenerated, store.getAlertCount())));

        sensitivityBar.setValue(surveillance.getSensitivity());
        sensitivityBar.setString("Sensitivity " + surveillance.getSensitivity() + "%");

        int generated = Math.max(alertsGenerated, store.getAlertCount());
        int scanPercent = (int) Math.min(100, generated * 100L / 25);
        detectionRateBar.setValue(scanPercent);
        detectionRateBar.setString("Detections " + generated);

        int queuePercent = (int) Math.min(100, store.getOpenAlertCount() * 100L / 25);
        queueBar.setValue(queuePercent);
        queueBar.setString("Open alerts " + store.getOpenAlertCount());

        ruleSummaryArea.setText(buildRuleSummary());
        refreshRecentAlerts();
    }

    private void refreshBars() {
        sensitivityBar.setValue(sensitivitySlider.getValue());
        sensitivityBar.setString("Sensitivity " + sensitivitySlider.getValue() + "%");
    }

    private String buildRuleSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("ACTIVE CONFIGURATION\n");
        sb.append("--------------------------------------\n");
        sb.append(String.format("Monitoring      : %s%n",
                surveillance.isMonitoring() ? "ON" : "OFF"));
        sb.append(String.format("Detection type  : %s%n", surveillance.getDetectionType()));
        sb.append(String.format("Sensitivity     : %d / 100%n", surveillance.getSensitivity()));
        sb.append(String.format("Risk threshold  : %d%n", surveillance.getRiskThreshold()));
        sb.append(String.format("Analysis window : %s%n", windowBox.getSelectedItem()));
        sb.append(String.format("Minimum size    : %d%n", (Integer) windowSpinner.getValue()));
        sb.append("--------------------------------------\n");
        sb.append(String.format("Rules defined   : %d%n", store.getRuleCount()));
        sb.append(String.format("Rules enabled   : %d%n", store.getEnabledRuleCount()));
        sb.append(String.format("Alerts detected : %d%n", store.getAlertCount()));
        sb.append(String.format("Below threshold : %d%n", surveillance.getSuppressedByThreshold()));
        sb.append("--------------------------------------\n");
        sb.append("Events above the threshold become alerts.\n");
        sb.append("Events below it are counted but discarded.\n");
        return sb.toString();
    }

    private void refreshRecentAlerts() {
        recentAlertsModel.setRowCount(0);
        List<Alert> alerts = store.getAlerts();
        int shown = 0;
        for (Alert alert : alerts) {
            recentAlertsModel.addRow(new Object[] {
                DateTimeUtil.time(alert.getTimestamp()),
                alert.getSymbol(),
                alert.getType(),
                alert.getRiskScore(),
                alert.getSeverity()
            });
            shown++;
            if (shown >= 40) {
                break;
            }
        }
    }

    @Override
    public void onAlertsChanged() {
        stateChanged(null);
    }

    @Override
    public void onMonitoringChanged(boolean monitoring) {
        monitoringToggle.setSelected(monitoring);
        monitoringToggle.setText(monitoring ? "MONITORING ON" : "MONITORING OFF");
        appendLog(monitoring ? "Monitoring switched ON" : "Monitoring switched OFF");
        refresh();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int size = store.getAlertCount();
        if (size > lastKnownAlertCount) {
            int added = size - lastKnownAlertCount;
            alertsGenerated += added;
            Alert latest = store.getAlerts().isEmpty() ? null : store.getAlerts().get(0);
            if (latest != null) {
                appendLog(String.format("ALERT %s  %s  risk %d (%s)  type %s",
                        latest.getAlertId(), latest.getSymbol(), latest.getRiskScore(),
                        latest.getSeverity(), latest.getType()));
            }
            if (soundCheck.isSelected()) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
        lastKnownAlertCount = size;
        refresh();
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
                int risk = 0;
                try {
                    risk = Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    risk = 0;
                }
                c.setForeground(risk >= 85 ? DarkTheme.RED
                        : risk >= 65 ? DarkTheme.AMBER
                        : risk >= 40 ? DarkTheme.PURPLE : DarkTheme.GREEN);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
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
                c.setForeground("CRITICAL".equals(severity) ? DarkTheme.RED
                        : "HIGH".equals(severity) ? DarkTheme.AMBER
                        : "MEDIUM".equals(severity) ? DarkTheme.PURPLE : DarkTheme.GREEN);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }
}
