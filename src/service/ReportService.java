package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.Alert;
import model.AuditReport;
import util.DateTimeUtil;

public class ReportService {

    public static final String[] EXPORT_FORMATS = { "CSV", "TXT", "PDF", "XLSX" };

    private final DataStore store;
    private final Random random = new Random(555L);

    public ReportService(DataStore store) {
        this.store = store;
    }

    public AuditReport generateReport(String reportType, String periodLabel) {
        long now = System.currentTimeMillis();
        int alertCount = store.getAlertCount();

        AuditReport report = new AuditReport(
                store.nextReportId(),
                reportType,
                now,
                AuditReport.STATUS_READY,
                alertCount,
                periodLabel == null || periodLabel.trim().isEmpty()
                        ? DateTimeUtil.date(now) : periodLabel.trim(),
                80 + random.nextInt(900));

        store.addReport(report);
        return report;
    }

    public List<AuditReport> getReports() {
        return new ArrayList<>(store.getReports());
    }

    public List<AuditReport> filterByType(String type) {
        List<AuditReport> result = new ArrayList<>();
        for (AuditReport report : store.getReports()) {
            if (type == null || "All Types".equals(type) || report.getReportType().equals(type)) {
                result.add(report);
            }
        }
        return result;
    }

    public boolean deleteReport(AuditReport report) {
        return report != null && store.getReports().remove(report);
    }

    public String exportReport(AuditReport report, String format, java.io.File destination) {
        if (report == null) {
            return "No report selected.";
        }
        StringBuilder message = new StringBuilder();
        message.append("Report ").append(report.getReportId())
               .append(" exported successfully (simulated).\n\n");
        message.append("Format    : ").append(format).append("\n");
        message.append("File      : ")
               .append(destination == null ? "not selected" : destination.getAbsolutePath())
               .append("\n");
        message.append("Records   : ").append(report.getAlertCount()).append(" alerts\n");
        message.append("Size      : ").append(report.getSizeKb()).append(" KB\n\n");
        message.append("In this build the export is simulated so the demo does not\n");
        message.append("depend on external libraries.");
        return message.toString();
    }

    public String getReportText(AuditReport report) {
        return report == null ? "No report selected." : report.getReportText();
    }

    public int getOpenAlertCount() {
        int count = 0;
        for (Alert alert : store.getAlerts()) {
            if (!Alert.STATUS_CLOSED.equals(alert.getStatus())) {
                count++;
            }
        }
        return count;
    }
}
