package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.AuditReport;
import service.ReportService;
import util.DarkTheme;
import util.DateTimeUtil;
import util.DemoData;

public class AuditReportPanel extends JPanel {

    private static final int COL_ID = 0;
    private static final int COL_TYPE = 1;
    private static final int COL_DATE = 2;
    private static final int COL_STATUS = 3;
    private static final int COL_ALERTS = 4;
    private static final int COL_PERIOD = 5;

    private final ReportService reportService;

    private final JComboBox<String> typeBox = new JComboBox<>(DemoData.reportTypes());
    private final JComboBox<String> filterBox = new JComboBox<>();
    private final JComboBox<String> periodBox = new JComboBox<>(new String[] {
        "Today", "Last 7 days", "Last 30 days", "This month", "Custom range"
    });
    private final JFormattedTextField dateField;
    private final JTextField generatedByField = DarkTheme.field("compliance.officer", 14);
    private final JTextField noteField = DarkTheme.field("Routine post-trade surveillance report", 26);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] { "Report ID", "Report Type", "Date", "Status", "Alerts", "Period" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea preview = new JTextArea();

    private final JLabel countLabel = DarkTheme.bold("0 reports");
    private final JLabel statusLabel = DarkTheme.statusLabel("REPORT ENGINE READY", DarkTheme.GREEN);
    private final JLabel selectedLabel = DarkTheme.muted("No report selected");
    private final JLabel openAlertsLabel = DarkTheme.muted("Open alerts: 0");

    public AuditReportPanel() {
        this(new ReportService(new service.DataStore()));
    }

    public AuditReportPanel(ReportService reportService) {
        this.reportService = reportService;

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);
        dateField = new JFormattedTextField(format);
        dateField.setValue(new Date());
        dateField.setFont(DarkTheme.FONT_BASE);
        dateField.setBackground(DarkTheme.SURFACE_ALT);
        dateField.setForeground(DarkTheme.TEXT);
        dateField.setCaretColor(DarkTheme.TEXT);
        dateField.setPreferredSize(new Dimension(120, DarkTheme.FIELD_HEIGHT));
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Audit Reports",
                "Generate, review and export simulated surveillance audit reports"), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        refreshTable();
    }

    private JSplitPane buildSplit() {
        return DarkTheme.split(buildTableSide(), buildDetailSide(), 700);
    }

    private JPanel buildTableSide() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(DarkTheme.BG);
        side.add(buildGeneratorCard(), BorderLayout.NORTH);
        side.add(DarkTheme.scroll(table), BorderLayout.CENTER);
        return side;
    }

    private JPanel buildGeneratorCard() {
        JPanel card = DarkTheme.card("Generate a new report", new BorderLayout(0, 8));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setOpaque(false);

        form.add(DarkTheme.muted("Report Type"));
        typeBox.setPreferredSize(new Dimension(230, DarkTheme.FIELD_HEIGHT));
        form.add(typeBox);

        form.add(DarkTheme.muted("Report Date"));
        form.add(dateField);

        form.add(DarkTheme.muted("Period"));
        periodBox.setPreferredSize(new Dimension(140, DarkTheme.FIELD_HEIGHT));
        form.add(periodBox);

        form.add(DarkTheme.muted("Generated By"));
        form.add(generatedByField);

        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        secondRow.setOpaque(false);
        secondRow.add(DarkTheme.muted("Note"));
        noteField.setPreferredSize(new Dimension(320, DarkTheme.FIELD_HEIGHT));
        secondRow.add(noteField);

        JButton generateButton = DarkTheme.button("Generate Report");
        JButton previewButton = DarkTheme.outlineButton("Preview");
        JButton clearButton = DarkTheme.outlineButton("Clear");
        secondRow.add(generateButton);
        secondRow.add(previewButton);
        secondRow.add(clearButton);

        generateButton.addActionListener(e -> generateReport());
        previewButton.addActionListener(e -> previewConfiguration());
        clearButton.addActionListener(e -> {
            noteField.setText("");
            dateField.setValue(new Date());
            typeBox.setSelectedIndex(0);
            preview.setText("");
        });

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(form);
        top.add(secondRow);

        card.add(top, BorderLayout.CENTER);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.add(DarkTheme.muted("Filter by type"));
        filterBox.addItem("All Types");
        for (String type : DemoData.reportTypes()) {
            filterBox.addItem(type);
        }
        filterBox.setPreferredSize(new Dimension(230, DarkTheme.FIELD_HEIGHT));
        filterBox.addActionListener(e -> refreshTable());
        filterRow.add(filterBox);

        JButton viewButton = DarkTheme.outlineButton("View");
        JButton exportButton = DarkTheme.button("Export");
        JButton deleteButton = DarkTheme.outlineButton("Delete");
        viewButton.addActionListener(e -> viewSelectedReport());
        exportButton.addActionListener(e -> exportSelectedReport());
        deleteButton.addActionListener(e -> deleteSelectedReport());
        filterRow.add(viewButton);
        filterRow.add(exportButton);
        filterRow.add(deleteButton);

        card.add(filterRow, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildDetailSide() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(DarkTheme.BG);
        side.setPreferredSize(new Dimension(470, 100));

        JPanel card = DarkTheme.card("Report Preview", new BorderLayout(0, 6));

        preview.setEditable(false);
        preview.setFont(DarkTheme.FONT_MONO);
        preview.setBackground(DarkTheme.SURFACE_ALT);
        preview.setForeground(DarkTheme.TEXT);
        preview.setCaretColor(DarkTheme.TEXT);
        preview.setLineWrap(false);
        card.add(DarkTheme.scroll(preview), BorderLayout.CENTER);

        JPanel info = new JPanel(new GridLayout(0, 1, 0, 4));
        info.setOpaque(false);
        info.add(selectedLabel);
        info.add(openAlertsLabel);
        info.add(DarkTheme.muted("Reports are simulated and are not written to disk in this build."));
        card.add(info, BorderLayout.SOUTH);

        side.add(card, BorderLayout.CENTER);
        side.add(buildQuickStats(), BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildQuickStats() {
        JPanel stats = DarkTheme.card("Report Statistics", new GridLayout(2, 4, 6, 4));
        stats.add(DarkTheme.muted("Total"));
        stats.add(DarkTheme.muted("Ready"));
        stats.add(DarkTheme.muted("Archived"));
        stats.add(DarkTheme.muted("Alerts"));
        stats.add(DarkTheme.bold("0"));
        stats.add(DarkTheme.bold("0"));
        stats.add(DarkTheme.bold("0"));
        stats.add(DarkTheme.bold("0"));

        totalValue = (JLabel) stats.getComponent(4);
        readyValue = (JLabel) stats.getComponent(5);
        archivedValue = (JLabel) stats.getComponent(6);
        alertsValue = (JLabel) stats.getComponent(7);
        return stats;
    }

    private JLabel totalValue;
    private JLabel readyValue;
    private JLabel archivedValue;
    private JLabel alertsValue;

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(countLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(DarkTheme.muted("Format: database extract / CSV / PDF (simulated)"));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        DarkTheme.styleTable(table);
        DarkTheme.setColumnWidth(table, COL_ID, 110);
        DarkTheme.setColumnWidth(table, COL_TYPE, 210);
        DarkTheme.setColumnWidth(table, COL_DATE, 95);
        DarkTheme.setColumnWidth(table, COL_STATUS, 90);
        DarkTheme.setColumnWidth(table, COL_ALERTS, 60);
        table.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(COL_ALERTS).setCellRenderer(new NumberRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedReport();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    viewSelectedReport();
                }
            }

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

        periodBox.addActionListener(e -> {
            boolean custom = "Custom range".equals(periodBox.getSelectedItem());
            dateField.setEnabled(custom);
            if (!custom) {
                dateField.setValue(new Date());
            }
        });
        dateField.setEnabled(false);
    }

    private final JPopupMenu popup = createPopup();

    private JPopupMenu createPopup() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem viewItem = new JMenuItem("View report");
        JMenuItem exportItem = new JMenuItem("Export report");
        JMenuItem duplicateItem = new JMenuItem("Regenerate report");
        JMenuItem deleteItem = new JMenuItem("Delete report");

        viewItem.addActionListener(e -> viewSelectedReport());
        exportItem.addActionListener(e -> exportSelectedReport());
        duplicateItem.addActionListener(e -> regenerateSelected());
        deleteItem.addActionListener(e -> deleteSelectedReport());

        menu.add(viewItem);
        menu.add(exportItem);
        menu.add(duplicateItem);
        menu.addSeparator();
        menu.add(deleteItem);
        return menu;
    }

    private AuditReport getSelectedReport() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = String.valueOf(tableModel.getValueAt(modelRow, COL_ID));
        for (AuditReport report : reportService.getReports()) {
            if (report.getReportId().equals(id)) {
                return report;
            }
        }
        return null;
    }

    private void generateReport() {
        String reportType = (String) typeBox.getSelectedItem();
        String period = periodLabel();

        if ("Custom range".equals(periodBox.getSelectedItem())) {
            String text = String.valueOf(dateField.getValue());
            if (!DateTimeUtil.isValidDate(text)) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid report date in the format yyyy-MM-dd.",
                        "Invalid date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        AuditReport report = reportService.generateReport(reportType, period);

        int choice = JOptionPane.showConfirmDialog(this,
                "Generate report?\n\n"
              + "Type       : " + reportType + "\n"
              + "Period     : " + period + "\n"
              + "Generated  : " + report.getDateTimeText() + "\n"
              + "Alerts     : " + report.getAlertCount() + "\n\n"
              + "The report will appear at the top of the table.",
                "Confirm report generation", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.OK_OPTION) {
            refreshTable();
            selectReport(report);
            preview.setText(report.getReportText());
            statusLabel.setText("  \u25CF   REPORT " + report.getReportId() + " READY");
            statusLabel.setForeground(DarkTheme.GREEN);
        }
    }

    private String periodLabel() {
        String selected = (String) periodBox.getSelectedItem();
        if ("Custom range".equals(selected)) {
            return String.valueOf(dateField.getValue());
        } else if ("Today".equals(selected)) {
            return DateTimeUtil.today();
        } else if ("Last 7 days".equals(selected)) {
            return DateTimeUtil.date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
                    + " to " + DateTimeUtil.today();
        } else if ("Last 30 days".equals(selected)) {
            return DateTimeUtil.date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                    + " to " + DateTimeUtil.today();
        } else if ("This month".equals(selected)) {
            String today = DateTimeUtil.today();
            return today.substring(0, 7) + "-01 to " + today;
        }
        return DateTimeUtil.today();
    }

    private void previewConfiguration() {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORT CONFIGURATION PREVIEW\n");
        sb.append("=====================================================\n");
        sb.append("Report type   : ").append(typeBox.getSelectedItem()).append("\n");
        sb.append("Period        : ").append(periodLabel()).append("\n");
        sb.append("Report date   : ").append(String.valueOf(dateField.getValue())).append("\n");
        sb.append("Generated by  : ").append(generatedByField.getText()).append("\n");
        sb.append("Note          : ").append(noteField.getText()).append("\n");
        sb.append("Open alerts   : ").append(reportService.getOpenAlertCount()).append("\n");
        sb.append("-----------------------------------------------------\n");
        sb.append("Press \"Generate Report\" to create the file.\n");
        preview.setText(sb.toString());
        preview.setCaretPosition(0);
    }

    private void viewSelectedReport() {
        AuditReport report = getSelectedReport();
        if (report == null) {
            JOptionPane.showMessageDialog(this, "Please select a report in the table first.",
                    "Audit Reports", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        showReportDialog(report);
    }

    private void showReportDialog(AuditReport report) {
        final javax.swing.JDialog dialog = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                "Audit Report - " + report.getReportId(),
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        JTextArea area = new JTextArea(report.getReportText());
        area.setEditable(false);
        area.setFont(DarkTheme.FONT_MONO);
        area.setBackground(DarkTheme.SURFACE_ALT);
        area.setForeground(DarkTheme.TEXT);
        area.setCaretColor(DarkTheme.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = DarkTheme.card(new BorderLayout(0, 2));
        header.add(DarkTheme.bold(report.getReportId() + "  -  " + report.getReportType()), BorderLayout.NORTH);
        header.add(DarkTheme.muted("Generated " + report.getDateTimeText()
                + "   |   Status " + report.getStatus()
                + "   |   " + report.getAlertCount() + " alerts"), BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBackground(DarkTheme.SURFACE);
        JButton exportButton = DarkTheme.button("Export...");
        JButton closeButton = DarkTheme.outlineButton("Close");
        exportButton.addActionListener(e -> {
            dialog.dispose();
            exportReport(report);
        });
        closeButton.addActionListener(e -> dialog.dispose());
        buttons.add(exportButton);
        buttons.add(closeButton);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(DarkTheme.SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(header, BorderLayout.NORTH);
        content.add(DarkTheme.scroll(area), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setSize(720, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void exportSelectedReport() {
        AuditReport report = getSelectedReport();
        if (report == null) {
            JOptionPane.showMessageDialog(this, "Please select a report in the table first.",
                    "Audit Reports", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        exportReport(report);
    }

    private void exportReport(AuditReport report) {
        JComboBox<String> formatBox = new JComboBox<>(ReportService.EXPORT_FORMATS);
        formatBox.setBackground(DarkTheme.SURFACE_ALT);
        formatBox.setForeground(DarkTheme.TEXT);

        JPanel options = new JPanel(new GridLayout(0, 1, 4, 6));
        options.setBackground(DarkTheme.SURFACE);
        options.add(DarkTheme.label("Export format"));
        options.add(formatBox);
        options.add(DarkTheme.muted("A suggested file name is provided below."));

        int choice = JOptionPane.showConfirmDialog(this, options, "Export options",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String format = String.valueOf(formatBox.getSelectedItem());
        String extension = format.toLowerCase();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export report " + report.getReportId());
        chooser.setSelectedFile(new java.io.File(report.getReportId() + "." + extension));
        chooser.setAcceptAllFileFilterUsed(true);

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        String message = reportService.exportReport(report, format, chooser.getSelectedFile());
        JOptionPane.showMessageDialog(this, message, "Export complete",
                JOptionPane.INFORMATION_MESSAGE);

        preview.setText("EXPORT LOG\n"
                + "------------------------------------------------\n"
                + "Report     : " + report.getReportId() + "\n"
                + "Format     : " + format + "\n"
                + "Destination: " + chooser.getSelectedFile().getAbsolutePath() + "\n"
                + "Time       : " + DateTimeUtil.dateTime(System.currentTimeMillis()) + "\n"
                + "------------------------------------------------\n"
                + "Simulated export - no file was written in this build.\n");
        preview.setCaretPosition(0);
    }

    private void regenerateSelected() {
        AuditReport report = getSelectedReport();
        if (report == null) {
            JOptionPane.showMessageDialog(this, "Please select a report in the table first.",
                    "Audit Reports", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AuditReport regenerated = reportService.generateReport(report.getReportType(), report.getPeriod());
        refreshTable();
        selectReport(regenerated);
        JOptionPane.showMessageDialog(this,
                "A new report was generated from the same configuration:\n\n"
              + regenerated.getReportId() + " - " + regenerated.getReportType(),
                "Report regenerated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedReport() {
        AuditReport report = getSelectedReport();
        if (report == null) {
            JOptionPane.showMessageDialog(this, "Please select a report in the table first.",
                    "Audit Reports", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete report " + report.getReportId() + "?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            reportService.deleteReport(report);
            preview.setText("");
            refreshTable();
            statusLabel.setText("  \u25CF   REPORT DELETED");
            statusLabel.setForeground(DarkTheme.AMBER);
        }
    }

    private void selectReport(AuditReport report) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (report.getReportId().equals(tableModel.getValueAt(i, COL_ID))) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void showSelectedReport() {
        AuditReport report = getSelectedReport();
        if (report == null) {
            return;
        }
        preview.setText(report.getReportText());
        preview.setCaretPosition(0);
        selectedLabel.setText("Selected: " + report.getReportId() + "  (" + report.getReportType() + ")");
        selectedLabel.setForeground(DarkTheme.TEXT);
    }

    public void refreshView() {
        refreshTable();
    }

    private void refreshTable() {
        String filter = (String) filterBox.getSelectedItem();
        List<AuditReport> reports = reportService.filterByType(filter);

        tableModel.setRowCount(0);
        for (AuditReport report : reports) {
            tableModel.addRow(new Object[] {
                report.getReportId(),
                report.getReportType(),
                report.getDateText(),
                report.getStatus(),
                report.getAlertCount(),
                report.getPeriod()
            });
        }

        countLabel.setText(reports.size() + " of " + reportService.getReports().size() + " reports");
        openAlertsLabel.setText("Open alerts: " + reportService.getOpenAlertCount());

        int ready = 0;
        int archived = 0;
        int alerts = 0;
        for (AuditReport report : reportService.getReports()) {
            if (AuditReport.STATUS_READY.equals(report.getStatus())) {
                ready++;
            } else if (AuditReport.STATUS_ARCHIVED.equals(report.getStatus())) {
                archived++;
            }
            alerts += report.getAlertCount();
        }
        if (totalValue != null) {
            totalValue.setText(String.valueOf(reportService.getReports().size()));
            readyValue.setText(String.valueOf(ready));
            archivedValue.setText(String.valueOf(archived));
            alertsValue.setText(String.valueOf(alerts));
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String status = String.valueOf(value);
                if (AuditReport.STATUS_READY.equals(status)) {
                    c.setForeground(DarkTheme.GREEN);
                } else if (AuditReport.STATUS_GENERATING.equals(status)) {
                    c.setForeground(DarkTheme.AMBER);
                } else if (AuditReport.STATUS_FAILED.equals(status)) {
                    c.setForeground(DarkTheme.RED);
                } else {
                    c.setForeground(DarkTheme.TEXT_MUTED);
                }
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }

    private static class NumberRenderer extends DefaultTableCellRenderer {
        NumberRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(DarkTheme.FONT_MONO);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }
}
