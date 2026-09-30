package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.Alert;
import service.DataStore;
import service.SurveillanceService;
import util.DarkTheme;
import util.DateTimeUtil;
import util.DemoData;

public class AlertsPanel extends JPanel implements ChangeListener {

    private static final int COL_ID = 0;
    private static final int COL_SYMBOL = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_RISK = 3;
    private static final int COL_TIME = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_TRADER = 6;

    private final DataStore store;
    private final SurveillanceService surveillance;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] { "Alert ID", "Symbol", "Type", "Risk", "Time", "Status", "Trader" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[] { "All", Alert.STATUS_NEW, Alert.STATUS_REVIEW, Alert.STATUS_CLOSED });
    private final JTextField searchField = DarkTheme.searchField("Search alert id / symbol...", 16);
    private final JComboBox<String> severityFilter = new JComboBox<>(
            new String[] { "All severities", "CRITICAL", "HIGH", "MEDIUM", "LOW" });

    private final JTextArea detailArea = new JTextArea();
    private final JTextArea evidenceArea = new JTextArea();
    private final JTextArea historyArea = new JTextArea();

    private final JLabel totalLabel = DarkTheme.bold("0 alerts");
    private final JLabel newLabel = DarkTheme.statusLabel("0 NEW", DarkTheme.RED);
    private final JLabel reviewLabel = DarkTheme.statusLabel("0 UNDER REVIEW", DarkTheme.AMBER);
    private final JLabel closedLabel = DarkTheme.statusLabel("0 CLOSED", DarkTheme.GREEN);
    private final JLabel selectedLabel = DarkTheme.muted("No alert selected");

    private final SeverityBarChart severityChart = new SeverityBarChart();
    private String selectedAlertId;

    public AlertsPanel() {
        this(new DataStore(), null);
    }

    public AlertsPanel(DataStore store, SurveillanceService surveillance) {
        this.store = store;
        this.surveillance = surveillance;

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Alerts",
                "Suspicious-event queue with review workflow and evidence detail"), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        refreshTable();

        if (surveillance != null) {
            surveillance.addAlertListener(this);
        }
    }

    private JSplitPane buildSplit() {
        return DarkTheme.split(buildTableSide(), buildDetailSide(), 720);
    }

    private JPanel buildTableSide() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(DarkTheme.BG);
        side.add(buildFilterBar(), BorderLayout.NORTH);
        side.add(DarkTheme.scroll(table), BorderLayout.CENTER);
        return side;
    }

    private JPanel buildFilterBar() {

        JPanel card = DarkTheme.card(new BorderLayout(0, 6));

        JPanel row = new JPanel(new GridLayout(1, 4, 8, 0));
        row.setOpaque(false);

        typeFilter.addItem("All");
        for (String type : DemoData.alertTypes()) {
            typeFilter.addItem(type);
        }

        typeFilter.setPreferredSize(null);
        statusFilter.setPreferredSize(null);
        severityFilter.setPreferredSize(null);

        row.add(DarkTheme.fieldBlock("Type", typeFilter));
        row.add(DarkTheme.fieldBlock("Status", statusFilter));
        row.add(DarkTheme.fieldBlock("Severity", severityFilter));

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        searchRow.add(DarkTheme.fieldBlock("Search", searchField), BorderLayout.CENTER);

        JButton resetButton = DarkTheme.outlineButton("Reset");
        resetButton.addActionListener(e -> {
            typeFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            severityFilter.setSelectedIndex(0);
            searchField.setText("");
            refreshTable();
        });

        JPanel resetHolder = new JPanel(new BorderLayout(0, 4));
        resetHolder.setOpaque(false);
        resetHolder.add(DarkTheme.muted(" "), BorderLayout.NORTH);
        resetHolder.add(resetButton, BorderLayout.CENTER);
        searchRow.add(resetHolder, BorderLayout.EAST);
        row.add(searchRow);

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDetailSide() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(DarkTheme.BG);
        side.setPreferredSize(new Dimension(430, 100));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(DarkTheme.FONT_BOLD);
        tabs.setBackground(DarkTheme.SURFACE);
        tabs.setForeground(DarkTheme.TEXT);

        detailArea.setEditable(false);
        evidenceArea.setEditable(false);
        historyArea.setEditable(false);
        for (JTextArea area : new JTextArea[] { detailArea, evidenceArea, historyArea }) {
            area.setFont(DarkTheme.FONT_MONO);
            area.setBackground(DarkTheme.SURFACE_ALT);
            area.setForeground(DarkTheme.TEXT);
            area.setCaretColor(DarkTheme.TEXT);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        }

        tabs.addTab("Detail", DarkTheme.scroll(detailArea));
        tabs.addTab("Evidence", DarkTheme.scroll(evidenceArea));
        tabs.addTab("History", DarkTheme.scroll(historyArea));

        side.add(tabs, BorderLayout.CENTER);
        side.add(buildActionBar(), BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildActionBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(DarkTheme.BG);

        JPanel buttons = DarkTheme.card(new GridLayout(2, 2, 8, 8));

        JButton reviewButton = DarkTheme.button("Mark Reviewed", DarkTheme.AMBER);
        JButton closeButton = DarkTheme.button("Close Alert", DarkTheme.GREEN);
        JButton reopenButton = DarkTheme.outlineButton("Reopen");
        JButton escalateButton = DarkTheme.button("Escalate", DarkTheme.RED);

        reviewButton.addActionListener(e -> setSelectedStatus(Alert.STATUS_REVIEW));
        closeButton.addActionListener(e -> setSelectedStatus(Alert.STATUS_CLOSED));
        reopenButton.addActionListener(e -> setSelectedStatus(Alert.STATUS_NEW));
        escalateButton.addActionListener(e -> escalateSelected());

        buttons.add(reviewButton);
        buttons.add(closeButton);
        buttons.add(reopenButton);
        buttons.add(escalateButton);

        wrapper.add(buttons, BorderLayout.NORTH);

        JPanel stats = DarkTheme.card(new GridLayout(2, 2, 6, 4));
        stats.add(DarkTheme.muted("NEW"));
        stats.add(newLabel);
        stats.add(DarkTheme.muted("UNDER REVIEW"));
        stats.add(reviewLabel);
        stats.add(DarkTheme.muted("CLOSED"));
        stats.add(closedLabel);
        stats.add(DarkTheme.muted("SELECTED"));
        stats.add(selectedLabel);

        JPanel chartCard = DarkTheme.card("Alerts by severity", new BorderLayout());
        severityChart.setPreferredSize(new Dimension(400, 110));
        chartCard.add(severityChart, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(DarkTheme.BG);
        bottom.add(stats);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(chartCard);

        wrapper.add(bottom, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(totalLabel);
        left.add(DarkTheme.muted("|  Source: Surveillance Engine (simulated)"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton clearClosed = DarkTheme.outlineButton("Remove closed alerts");
        clearClosed.addActionListener(e -> {
            int removed = countClosed();
            if (removed == 0) {
                showInfo("There are no closed alerts to remove.");
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Remove " + removed + " closed alert(s) from the queue?",
                    "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                store.removeClosedAlerts();
                selectedAlertId = null;
                refreshTable();
            }
        });

        JButton exportButton = DarkTheme.button("Export Queue");
        exportButton.addActionListener(e -> exportQueue());

        right.add(clearClosed);
        right.add(exportButton);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        ActionListener filterListener = e -> refreshTable();
        typeFilter.addActionListener(filterListener);
        statusFilter.addActionListener(filterListener);
        severityFilter.addActionListener(filterListener);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshTable();
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedAlert();
            }
        });

        table.getColumnModel().getColumn(COL_RISK).setCellRenderer(new RiskRenderer());
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());
        DarkTheme.styleTable(table);
        DarkTheme.setColumnWidth(table, COL_ID, 80);
        DarkTheme.setColumnWidth(table, COL_SYMBOL, 70);
        DarkTheme.setColumnWidth(table, COL_TYPE, 130);
        DarkTheme.setColumnWidth(table, COL_RISK, 55);
        DarkTheme.setColumnWidth(table, COL_TIME, 80);
        DarkTheme.setColumnWidth(table, COL_STATUS, 110);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final JPopupMenu popup = new JPopupMenu();
        JMenuItem reviewItem = new JMenuItem("Mark reviewed");
        JMenuItem closeItem = new JMenuItem("Close alert");
        JMenuItem reopenItem = new JMenuItem("Reopen alert");
        popup.add(reviewItem);
        popup.add(closeItem);
        popup.addSeparator();
        popup.add(reopenItem);

        reviewItem.addActionListener(e -> setSelectedStatus(Alert.STATUS_REVIEW));
        closeItem.addActionListener(e -> setSelectedStatus(Alert.STATUS_CLOSED));
        reopenItem.addActionListener(e -> setSelectedStatus(Alert.STATUS_NEW));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void showInfo(String message) {
        DarkTheme.info(this, message, "Alerts");
    }

    private Alert getSelectedAlert() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || selectedAlertId == null) {
            return null;
        }
        for (Alert alert : store.getAlerts()) {
            if (alert.getAlertId().equals(selectedAlertId)) {
                return alert;
            }
        }
        return null;
    }

    private void showSelectedAlert() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        selectedAlertId = String.valueOf(tableModel.getValueAt(modelRow, COL_ID));
        Alert alert = getSelectedAlert();
        if (alert == null) {
            return;
        }

        detailArea.setText(alert.getDetailText());
        detailArea.setCaretPosition(0);

        evidenceArea.setText("EVIDENCE PACKAGE\n"
                + "------------------------------------------------------\n"
                + "Alert ID      : " + alert.getAlertId() + "\n"
                + "Severity      : " + alert.getSeverity() + "\n"
                + "Risk score    : " + alert.getRiskScore() + " / 100\n"
                + "Participant   : " + alert.getTrader() + "\n"
                + "Instrument    : " + alert.getSymbol() + "\n"
                + "Order size    : " + alert.getQuantity() + "\n"
                + "Reference px  : " + String.format("%.2f", alert.getPrice()) + "\n"
                + "Order value   : " + String.format("%.2f", alert.getPrice() * alert.getQuantity()) + "\n"
                + "Detected at   : " + DateTimeUtil.dateTime(alert.getTimestamp()) + "\n"
                + "------------------------------------------------------\n"
                + "Related events     : " + (2 + alert.getRiskScore() / 10) + "\n"
                + "Cancellation ratio : " + Math.min(99, alert.getRiskScore()) + "%\n"
                + "Avg lifetime       : " + (20 + alert.getRiskScore()) + " ms\n");
        evidenceArea.setCaretPosition(0);

        historyArea.setText("ALERT LIFECYCLE\n"
                + "------------------------------------------------------\n"
                + String.format("%s   CREATED by surveillance engine%n",
                        DateTimeUtil.time(alert.getTimestamp()))
                + String.format("%s   Risk score assigned: %d (%s)%n",
                        DateTimeUtil.time(alert.getTimestamp() + 1000), alert.getRiskScore(), alert.getSeverity())
                + String.format("%s   Current status: %s%n",
                        DateTimeUtil.time(System.currentTimeMillis()), alert.getStatus())
                + "------------------------------------------------------\n"
                + "Use the buttons below to move the alert through the\n"
                + "review workflow: NEW -> UNDER REVIEW -> CLOSED\n");
        historyArea.setCaretPosition(0);

        selectedLabel.setText(alert.getAlertId());
        selectedLabel.setForeground(severityColor(alert.getSeverity()));
    }

    private void setSelectedStatus(String status) {
        Alert alert = getSelectedAlert();
        if (alert == null) {
            showInfo("Please select an alert in the table first.");
            return;
        }
        alert.setStatus(status);
        if (surveillance != null) {
            surveillance.notifyAlertsChanged();
        }
        refreshTable();
        showSelectedAlert();
    }

    private void escalateSelected() {
        Alert alert = getSelectedAlert();
        if (alert == null) {
            showInfo("Please select an alert in the table first.");
            return;
        }
        alert.setRiskScore(Math.min(100, alert.getRiskScore() + 10));
        alert.setStatus(Alert.STATUS_REVIEW);
        if (surveillance != null) {
            surveillance.notifyAlertsChanged();
        }
        refreshTable();
        showSelectedAlert();
        showInfo("Alert " + alert.getAlertId() + " escalated.\n"
                + "Risk score increased to " + alert.getRiskScore() + ".");
    }

    private void exportQueue() {
        List<Alert> rows = surveillance == null
                ? store.getAlerts()
                : surveillance.filterAlerts((String) typeFilter.getSelectedItem(),
                        (String) statusFilter.getSelectedItem());

        StringBuilder sb = new StringBuilder();
        sb.append("Alert ID,Symbol,Type,Risk,Timestamp,Status,Trader\n");
        for (Alert alert : rows) {
            sb.append(alert.getAlertId()).append(',')
              .append(alert.getSymbol()).append(',')
              .append(alert.getType()).append(',')
              .append(alert.getRiskScore()).append(',')
              .append(DateTimeUtil.dateTime(alert.getTimestamp())).append(',')
              .append(alert.getStatus()).append(',')
              .append(alert.getTrader()).append('\n');
        }

        JTextArea preview = new JTextArea(sb.toString(), 14, 70);
        preview.setFont(DarkTheme.FONT_MONO);
        preview.setEditable(false);
        DarkTheme.info(this, DarkTheme.scroll(preview), "Export preview - " + rows.size() + " alert(s) (simulated)");
    }

    private int countClosed() {
        int count = 0;
        for (Alert alert : store.getAlerts()) {
            if (Alert.STATUS_CLOSED.equals(alert.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public void refreshView() {
        refreshTable();
    }

    private void refreshTable() {
        String type = (String) typeFilter.getSelectedItem();
        String status = (String) statusFilter.getSelectedItem();
        String severity = (String) severityFilter.getSelectedItem();
        String query = DarkTheme.readField(searchField).trim().toLowerCase();

        List<Alert> alerts = surveillance == null
                ? store.getAlerts()
                : surveillance.filterAlerts(type, status);

        tableModel.setRowCount(0);
        int shown = 0;
        for (Alert alert : alerts) {
            if (!"All severities".equals(severity) && !alert.getSeverity().equals(severity)) {
                continue;
            }
            if (!query.isEmpty()
                    && !alert.getAlertId().toLowerCase().contains(query)
                    && !alert.getSymbol().toLowerCase().contains(query)) {
                continue;
            }
            tableModel.addRow(new Object[] {
                alert.getAlertId(),
                alert.getSymbol(),
                alert.getType(),
                alert.getRiskScore(),
                DateTimeUtil.time(alert.getTimestamp()),
                alert.getStatus(),
                alert.getTrader()
            });
            shown++;
        }

        if (selectedAlertId != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (selectedAlertId.equals(tableModel.getValueAt(i, COL_ID))) {
                    table.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }

        totalLabel.setText(shown + " of " + store.getAlertCount() + " alerts");
        if (surveillance != null) {
            newLabel.setText("  \u25CF   " + surveillance.countByStatus(Alert.STATUS_NEW) + " NEW");
            reviewLabel.setText("  \u25CF   " + surveillance.countByStatus(Alert.STATUS_REVIEW) + " UNDER REVIEW");
            closedLabel.setText("  \u25CF   " + surveillance.countByStatus(Alert.STATUS_CLOSED) + " CLOSED");
        }
        severityChart.setCounts(severityCounts());
    }

    private Map<String, Integer> severityCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("CRITICAL", 0);
        counts.put("HIGH", 0);
        counts.put("MEDIUM", 0);
        counts.put("LOW", 0);
        for (Alert alert : store.getAlerts()) {
            Integer current = counts.get(alert.getSeverity());
            counts.put(alert.getSeverity(), current == null ? 1 : current + 1);
        }
        return counts;
    }

    private Color severityColor(String severity) {
        if ("CRITICAL".equals(severity)) {
            return DarkTheme.RED;
        } else if ("HIGH".equals(severity)) {
            return DarkTheme.AMBER;
        } else if ("MEDIUM".equals(severity)) {
            return DarkTheme.PURPLE;
        }
        return DarkTheme.GREEN;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        refreshTable();
    }

    private class RiskRenderer extends DefaultTableCellRenderer {
        RiskRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_BOLD);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                                boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int risk = 0;
                try {
                    risk = Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException ignored) {

                }
                Color color;
                if (risk >= 85) {
                    color = DarkTheme.RED;
                } else if (risk >= 65) {
                    color = DarkTheme.AMBER;
                } else if (risk >= 40) {
                    color = DarkTheme.PURPLE;
                } else {
                    color = DarkTheme.GREEN;
                }
                c.setForeground(color);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }

    private class StatusRenderer extends DefaultTableCellRenderer {
        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_BOLD);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                                boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String status = String.valueOf(value);
                if (Alert.STATUS_NEW.equals(status)) {
                    c.setForeground(DarkTheme.RED);
                } else if (Alert.STATUS_REVIEW.equals(status)) {
                    c.setForeground(DarkTheme.AMBER);
                } else {
                    c.setForeground(DarkTheme.GREEN);
                }
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }

    private static class SeverityBarChart extends JPanel {

        private Map<String, Integer> counts = new LinkedHashMap<>();

        SeverityBarChart() {
            setOpaque(false);
        }

        void setCounts(Map<String, Integer> counts) {
            this.counts = counts;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (counts.isEmpty()) {
                return;
            }

            int max = 1;
            for (Integer value : counts.values()) {
                max = Math.max(max, value == null ? 0 : value);
            }

            int barHeight = 16;
            int gap = 8;
            int labelWidth = 72;
            int valueWidth = 34;
            int available = getWidth() - labelWidth - valueWidth - 10;
            int y = 6;

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                int value = entry.getValue() == null ? 0 : entry.getValue();
                Color color = colorFor(entry.getKey());

                g2.setColor(DarkTheme.TEXT_MUTED);
                g2.setFont(DarkTheme.FONT_SMALL);
                g2.drawString(entry.getKey(), 4, y + barHeight - 4);

                int width = (int) Math.round(available * (double) value / max);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g2.fillRoundRect(labelWidth, y, Math.max(available, 1), barHeight, 6, 6);
                g2.setColor(color);
                g2.fillRoundRect(labelWidth, y, Math.max(width, 2), barHeight, 6, 6);

                g2.setColor(DarkTheme.TEXT);
                g2.setFont(DarkTheme.FONT_BOLD);
                g2.drawString(String.valueOf(value), labelWidth + available + 8, y + barHeight - 4);

                y += barHeight + gap;
            }
        }

        private Color colorFor(String severity) {
            if ("CRITICAL".equals(severity)) {
                return DarkTheme.RED;
            } else if ("HIGH".equals(severity)) {
                return DarkTheme.AMBER;
            } else if ("MEDIUM".equals(severity)) {
                return DarkTheme.PURPLE;
            }
            return DarkTheme.GREEN;
        }
    }
}
