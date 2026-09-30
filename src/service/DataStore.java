package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.Alert;
import model.AuditReport;
import model.SurveillanceRule;
import util.DateTimeUtil;
import util.DemoData;

public class DataStore {

    private final Random random = new Random(4711L);

    private final List<Alert> alerts = new ArrayList<>();
    private final List<AuditReport> reports = new ArrayList<>();
    private final List<SurveillanceRule> rules = new ArrayList<>();

    private int alertSequence = 0;
    private int reportSequence = 0;
    private int ruleSequence = 0;

    public DataStore() {
        seedAlerts();
        seedReports();
        seedRules();
    }

    private void seedAlerts() {
        long now = System.currentTimeMillis();
        String[] symbols = DemoData.symbols();
        String[] types = DemoData.alertTypes();

        int[][] plan = {
            { 92, 0, 3 },  { 78, 0, 9 },  { 64, 1, 16 }, { 88, 1, 24 },
            { 45, 2, 33 }, { 71, 0, 41 }, { 36, 2, 55 }, { 83, 1, 68 }
        };

        for (int[] entry : plan) {
            int risk = entry[0];
            String status = statusFor(entry[1]);
            long when = now - entry[2] * 60_000L;
            String symbol = symbols[random.nextInt(symbols.length)];
            String type = types[random.nextInt(types.length)];
            addAlert(new Alert(
                    nextAlertId(),
                    symbol,
                    type,
                    risk,
                    when,
                    status,
                    randomTrader(),
                    200 + random.nextInt(9000),
                    Math.round((50 + random.nextDouble() * 900) * 100.0) / 100.0,
                    descriptionFor(type, symbol)));
        }
    }

    private String statusFor(int code) {
        if (code == 0) {
            return Alert.STATUS_NEW;
        } else if (code == 1) {
            return Alert.STATUS_REVIEW;
        }
        return Alert.STATUS_CLOSED;
    }

    private String randomTrader() {
        return "MM-" + (100 + random.nextInt(900)) + (char) ('A' + random.nextInt(6));
    }

    private String descriptionFor(String type, String symbol) {
        if ("Spoofing".equals(type)) {
            return "Multiple large buy orders were placed on " + symbol + " and cancelled\n"
                 + "within 40 ms while the participant was selling on the opposite side\n"
                 + "of the book. The pattern repeated 6 times in 2 seconds.\n\n"
                 + "RULE       : Spoofing / Layering v2\n"
                 + "EVIDENCE   : 6 cancelled orders, 92% cancellation ratio, avg order\n"
                 + "             lifetime 31 ms, no genuine execution attempt.\n"
                 + "SUGGESTED  : Escalate to market supervision team for review.";
        } else if ("Wash Trade".equals(type)) {
            return "Two accounts under the same beneficial owner appear on both sides\n"
                 + "of " + symbol + " at the same price and quantity within 5 ms.\n\n"
                 + "RULE       : Self-Match Detection / Wash Trade v1\n"
                 + "EVIDENCE   : 4 matched pairs, identical size, no economic change.\n"
                 + "SUGGESTED  : Request trader explanation, confirm account ownership.";
        } else if ("Layering".equals(type)) {
            return "A sequence of orders was placed on one side of the " + symbol + " book\n"
                 + "to create false depth, then cancelled once the opposite order filled.\n\n"
                 + "RULE       : Layering Pattern v1\n"
                 + "EVIDENCE   : 8 stacked levels, 3 cancels within 100 ms of execution.\n"
                 + "SUGGESTED  : Review order and cancel sequence for the participant.";
        } else if ("Quote Stuffing".equals(type)) {
            return "An extremely high message rate was observed from a single session\n"
                 + "on " + symbol + ", far above the peer group.\n\n"
                 + "RULE       : Message Rate Anomaly v1\n"
                 + "EVIDENCE   : 14,200 messages in 1 second (peer average 900).\n"
                 + "SUGGESTED  : Apply message rate throttle and notify the participant.";
        } else if ("Large Cancellation".equals(type)) {
            return "A large resting order on " + symbol + " was cancelled after moving the\n"
                 + "best price, with no execution taking place.\n\n"
                 + "RULE       : Large Order Cancellation v1\n"
                 + "EVIDENCE   : Order size 8,500, lifetime 220 ms, 0 filled.\n"
                 + "SUGGESTED  : Monitor the participant for repeated behaviour.";
        } else if ("Volume Spike".equals(type)) {
            return "Traded volume on " + symbol + " exceeded the rolling average by a\n"
                 + "large multiple inside a short window.\n\n"
                 + "RULE       : Volume Spike Detection v1\n"
                 + "EVIDENCE   : 12x the 30-tick average volume.\n"
                 + "SUGGESTED  : Confirm whether the activity is news driven.";
        }
        return "The quoted price of " + symbol + " moved outside the expected range for\n"
             + "the current market conditions.\n\n"
             + "RULE       : Price Anomaly Detection v1\n"
             + "EVIDENCE   : Deviation of 4.2 standard deviations from the session mean.\n"
             + "SUGGESTED  : Verify data integrity, then review participant activity.";
    }

    public void addAlert(Alert alert) {
        alerts.add(0, alert);
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public int getAlertCount() {
        return alerts.size();
    }

    public int getNewAlertCount() {
        int count = 0;
        for (Alert alert : alerts) {
            if (Alert.STATUS_NEW.equals(alert.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int getOpenAlertCount() {
        int count = 0;
        for (Alert alert : alerts) {
            if (!Alert.STATUS_CLOSED.equals(alert.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public String nextAlertId() {
        alertSequence++;
        return String.format("ALR-%04d", alertSequence);
    }

    private void seedReports() {
        long now = System.currentTimeMillis();
        String[] types = DemoData.reportTypes();
        String[] statuses = { AuditReport.STATUS_READY, AuditReport.STATUS_READY,
                              AuditReport.STATUS_ARCHIVED, AuditReport.STATUS_READY };

        for (int i = 0; i < 4; i++) {
            reports.add(new AuditReport(
                    nextReportId(),
                    types[i],
                    now - (i + 1) * 26L * 60 * 60 * 1000,
                    statuses[i],
                    5 + random.nextInt(30),
                    DateTimeUtil.date(now - (i + 2) * 26L * 60 * 60 * 1000)
                            + " to " + DateTimeUtil.date(now - (i + 1) * 26L * 60 * 60 * 1000),
                    80 + random.nextInt(900)));
        }
    }

    public void addReport(AuditReport report) {
        reports.add(0, report);
    }

    public List<AuditReport> getReports() {
        return reports;
    }

    public int getReportCount() {
        return reports.size();
    }

    public String nextReportId() {
        reportSequence++;
        return String.format("RPT-2024-%03d", reportSequence);
    }

    private void seedRules() {
        long now = System.currentTimeMillis();

        SurveillanceRule spoofing = new SurveillanceRule(nextRuleId(),
                "Large Order Cancellation", now - 3L * 24 * 60 * 60 * 1000);
        spoofing.addCondition(new SurveillanceRule.RuleCondition("Order Size", ">", 5000, 0));
        spoofing.addCondition(new SurveillanceRule.RuleCondition("Order Lifetime (ms)", "<", 500, 0));
        spoofing.addCondition(new SurveillanceRule.RuleCondition("Cancellation Ratio (%)", ">", 80, 0));
        spoofing.setLogic(SurveillanceRule.LOGIC_AND);
        spoofing.setSeverity(SurveillanceRule.SEVERITY_HIGH);
        spoofing.setDetectionType("Large Order Cancellation");
        spoofing.setMatchCount(12);
        rules.add(spoofing);

        SurveillanceRule layering = new SurveillanceRule(nextRuleId(),
                "Layering Detection", now - 2L * 24 * 60 * 60 * 1000);
        layering.addCondition(new SurveillanceRule.RuleCondition("Message Rate (per sec)", ">", 200, 0));
        layering.addCondition(new SurveillanceRule.RuleCondition("Order Lifetime (ms)", "<", 100, 0));
        layering.setLogic(SurveillanceRule.LOGIC_AND);
        layering.setSeverity(SurveillanceRule.SEVERITY_CRITICAL);
        layering.setDetectionType("Spoofing / Layering");
        layering.setMatchCount(5);
        rules.add(layering);

        SurveillanceRule volume = new SurveillanceRule(nextRuleId(),
                "Unusual Volume Spike", now - 24L * 60 * 60 * 1000);
        volume.addCondition(new SurveillanceRule.RuleCondition("Trade Volume", ">", 500000, 0));
        volume.setLogic(SurveillanceRule.LOGIC_OR);
        volume.setSeverity(SurveillanceRule.SEVERITY_MEDIUM);
        volume.setDetectionType("Unusual Volume Spike");
        volume.setEnabled(false);
        volume.setMatchCount(0);
        rules.add(volume);
    }

    public void addRule(SurveillanceRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    public List<SurveillanceRule> getRules() {
        return rules;
    }

    public int getRuleCount() {
        return rules.size();
    }

    public int getEnabledRuleCount() {
        int count = 0;
        for (SurveillanceRule rule : rules) {
            if (rule.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    public String nextRuleId() {
        ruleSequence++;
        return String.format("RUL-%03d", ruleSequence);
    }

    public void resetDemoData() {
        alerts.clear();
        reports.clear();
        rules.clear();
        alertSequence = 0;
        reportSequence = 0;
        ruleSequence = 0;
        seedAlerts();
        seedReports();
        seedRules();
    }

    public void removeClosedAlerts() {
        alerts.removeIf(alert -> Alert.STATUS_CLOSED.equals(alert.getStatus()));
    }
}
