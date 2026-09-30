package service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import model.Alert;
import model.MarketRow;
import util.DemoData;

public class SurveillanceService implements MarketDataService.MarketListener {

    public interface MonitoringListener {
        void onMonitoringChanged(boolean monitoring);

        void onAlertsChanged();
    }

    private static final long DEFAULT_SIMULATED_MILLIS_PER_TICK = 1000L;

    private final DataStore store;
    private final MarketDataService marketData;
    private final Random random = new Random(90210L);
    private final List<MonitoringListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ChangeListener> alertListeners = new CopyOnWriteArrayList<>();

    private boolean monitoring;
    private int sensitivity = 60;
    private int riskThreshold = 30;
    private String detectionType = DemoData.detectionTypes()[0];
    private long eventsScanned = 0;
    private int suppressedByThreshold = 0;

    public SurveillanceService(DataStore store, MarketDataService marketData) {
        this.store = store;
        this.marketData = marketData;

        marketData.addListener(this);
    }

    public boolean isMonitoring() {
        return monitoring;
    }

    public void setMonitoring(boolean monitoring) {
        this.monitoring = monitoring;
        for (MonitoringListener listener : listeners) {
            listener.onMonitoringChanged(monitoring);
        }
    }

    public void startMonitoring() {
        setMonitoring(true);
    }

    public void stopMonitoring() {
        setMonitoring(false);
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = Math.max(1, Math.min(100, sensitivity));
    }

    public int getRiskThreshold() {
        return riskThreshold;
    }

    public void setRiskThreshold(int riskThreshold) {
        this.riskThreshold = Math.max(1, Math.min(100, riskThreshold));
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        if (detectionType != null) {
            this.detectionType = detectionType;
        }
    }

    public long getEventsScanned() {
        return eventsScanned;
    }

    public int getSuppressedByThreshold() {
        return suppressedByThreshold;
    }

    public void addMonitoringListener(MonitoringListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeMonitoringListener(MonitoringListener listener) {
        listeners.remove(listener);
    }

    public void addAlertListener(ChangeListener listener) {
        if (listener != null) {
            alertListeners.add(listener);
        }
    }

    private void fireAlertsChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : alertListeners) {
            if (listener instanceof MonitoringListener) {
                ((MonitoringListener) listener).onAlertsChanged();
            } else {
                listener.stateChanged(event);
            }
        }
    }

    public void notifyAlertsChanged() {
        fireAlertsChanged();
    }

    @Override
    public void onMarketUpdate(List<MarketRow> rows) {
        if (!monitoring || rows.isEmpty()) {
            return;
        }
        eventsScanned += rows.size();

        int chance = 4 + sensitivity / 4;
        if (random.nextInt(100) >= chance) {
            return;
        }

        MarketRow row = rows.get(random.nextInt(rows.size()));
        int risk = 25 + random.nextInt(70);

        if (risk < riskThreshold) {

            suppressedByThreshold++;
            return;
        }

        Alert alert = createAlert(row, risk);
        store.addAlert(alert);
        fireAlertsChanged();
    }

    public Alert createAlert(MarketRow row, int risk) {
        String type = alertTypeFor(detectionType);
        long now = System.currentTimeMillis();

        return new Alert(
                store.nextAlertId(),
                row == null ? "AAPL" : row.getSymbol(),
                type,
                risk,
                now,
                Alert.STATUS_NEW,
                "MM-" + (100 + random.nextInt(900)) + (char) ('A' + random.nextInt(6)),
                200 + random.nextInt(9000),
                row == null ? 100.0 : row.getPrice(),
                descriptionFor(type, row == null ? "AAPL" : row.getSymbol()));
    }

    private String alertTypeFor(String detection) {
        if (detection == null) {
            return "Spoofing";
        }
        if (detection.startsWith("Spoofing")) {
            return "Spoofing";
        } else if (detection.startsWith("Wash")) {
            return "Wash Trade";
        } else if (detection.startsWith("Marking")) {
            return "Price Anomaly";
        } else if (detection.startsWith("Quote")) {
            return "Quote Stuffing";
        } else if (detection.startsWith("Large")) {
            return "Large Cancellation";
        } else if (detection.startsWith("Momentum")) {
            return "Layering";
        } else if (detection.startsWith("Unusual")) {
            return "Volume Spike";
        }
        return "Spoofing";
    }

    private String descriptionFor(String type, String symbol) {
        return "Detection type : " + detectionType + "\n"
             + "Instrument     : " + symbol + "\n"
             + "Sensitivity    : " + sensitivity + " / 100\n"
             + "Risk threshold : " + riskThreshold + "\n\n"
             + "The simulated surveillance engine flagged this event while\n"
             + "monitoring the live market feed. Open the Alerts module to\n"
             + "review the evidence and change the alert status.\n\n"
             + "RULE       : " + type + " detection\n"
             + "SUGGESTED  : Mark as reviewed or close the alert.";
    }

    public List<Alert> filterAlerts(String typeFilter, String statusFilter) {
        List<Alert> result = new ArrayList<>();
        for (Alert alert : store.getAlerts()) {
            boolean typeOk = typeFilter == null || "All".equals(typeFilter)
                    || alert.getType().equals(typeFilter);
            boolean statusOk = statusFilter == null || "All".equals(statusFilter)
                    || alert.getStatus().equals(statusFilter);
            if (typeOk && statusOk) {
                result.add(alert);
            }
        }
        return result;
    }

    public int countByStatus(String status) {
        int count = 0;
        for (Alert alert : store.getAlerts()) {
            if (alert.getStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }

    public int countBySeverity(String severity) {
        int count = 0;
        for (Alert alert : store.getAlerts()) {
            if (alert.getSeverity().equals(severity)) {
                count++;
            }
        }
        return count;
    }

    public double getAverageRiskScore() {
        if (store.getAlerts().isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Alert alert : store.getAlerts()) {
            total += alert.getRiskScore();
        }
        return (double) total / store.getAlerts().size();
    }

    public int getMaxRiskScore() {
        int max = 0;
        for (Alert alert : store.getAlerts()) {
            max = Math.max(max, alert.getRiskScore());
        }
        return max;
    }

    public int[] getAlertTypeCounts() {
        String[] types = DemoData.alertTypes();
        int[] counts = new int[types.length];
        for (Alert alert : store.getAlerts()) {
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(alert.getType())) {
                    counts[i]++;
                }
            }
        }
        return counts;
    }

    public long getSimulatedEventsPerTick() {
        return DEFAULT_SIMULATED_MILLIS_PER_TICK;
    }
}
