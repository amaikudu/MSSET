package service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Alert;
import model.AnalyticsStats;
import model.AnalyticsStats.PricePoint;
import model.LiveOrder;
import model.MarketRow;

public class AnalyticsService implements MarketDataService.MarketListener {

    public static final int MAX_HISTORY = 120;

    public static final String[] METRICS = {
        "Price (Last)",
        "Volume",
        "Orders Processed",
        "Alert Activity",
        "Order Lifetime"
    };

    private final MarketDataService marketData;
    private final DataStore store;

    private final Map<String, List<PricePoint>> history = new LinkedHashMap<>();

    private final Map<String, List<Long>> volumeHistory = new LinkedHashMap<>();

    private final Random random = new Random(1234L);
    private long alertsObserved = 0;
    private long lastAlertId = -1;

    public AnalyticsService(MarketDataService marketData, DataStore store) {
        this.marketData = marketData;
        this.store = store;

        for (MarketRow row : marketData.getRows()) {
            List<PricePoint> prices = new ArrayList<>();
            List<Long> volumes = new ArrayList<>();
            double price = row.getPrice();
            long volume = row.getVolume();
            long now = System.currentTimeMillis();
            for (int i = MAX_HISTORY - 1; i >= 0; i--) {

                price = price * (1 + (random.nextDouble() - 0.5) * 0.002);
                prices.add(new PricePoint(now - i * 1000L, Math.round(price * 100.0) / 100.0));
                volumes.add(volume - i * (500L + random.nextInt(2000)));
            }
            history.put(row.getSymbol(), prices);
            volumeHistory.put(row.getSymbol(), volumes);
        }
        marketData.addListener(this);
    }

    public void detach() {
        marketData.removeListener(this);
    }

    public MarketDataService getMarketDataService() {
        return marketData;
    }

    public DataStore getDataStore() {
        return store;
    }

    @Override
    public void onMarketUpdate(List<MarketRow> rows) {
        long now = System.currentTimeMillis();
        for (MarketRow row : rows) {
            List<PricePoint> prices = history.get(row.getSymbol());
            List<Long> volumes = volumeHistory.get(row.getSymbol());
            if (prices == null || volumes == null) {
                continue;
            }
            prices.add(new PricePoint(now, row.getPrice()));
            volumes.add(row.getVolume());
            if (prices.size() > MAX_HISTORY) {
                prices.remove(0);
            }
            if (volumes.size() > MAX_HISTORY) {
                volumes.remove(0);
            }
        }
        countNewAlerts();
    }

    private void countNewAlerts() {
        int size = store.getAlerts().size();
        if (lastAlertId < 0) {
            lastAlertId = size;
            return;
        }
        if (size > lastAlertId) {
            alertsObserved += size - lastAlertId;
        }
        lastAlertId = size;
    }

    public AnalyticsStats buildStats(String symbol) {
        AnalyticsStats stats = new AnalyticsStats();
        stats.setSymbol(symbol);

        List<LiveOrder> orders = marketData.getOrders();
        long executed = 0;
        long cancelled = 0;
        long lifetimeTotal = 0;
        long lifetimeCount = 0;
        long maxSize = 0;
        long totalOrders = 0;

        for (LiveOrder order : orders) {
            boolean matches = "ALL".equals(symbol) || order.getSymbol().equals(symbol);
            if (!matches) {
                continue;
            }
            totalOrders++;
            maxSize = Math.max(maxSize, order.getQuantity());
            if (LiveOrder.STATUS_FILLED.equals(order.getStatus())) {
                executed++;
            }
            if (LiveOrder.STATUS_CANCELLED.equals(order.getStatus())) {
                cancelled++;
            }
            if (!order.isWorking()) {
                lifetimeTotal += order.getLifetimeMs();
                lifetimeCount++;
            }
        }

        long backgroundFlow = marketData.getTickCount() * 12;
        stats.setTotalOrders(totalOrders + backgroundFlow);
        stats.setExecutedOrders(executed + backgroundFlow * 3 / 4);
        stats.setCancelledOrders(cancelled + backgroundFlow / 6);
        stats.setMaxOrderSize(Math.max(maxSize, 5000));
        stats.setAverageOrderLifetimeMs(lifetimeCount == 0
                ? 180 + random.nextInt(120)
                : (double) lifetimeTotal / lifetimeCount);

        long alerts = 0;
        for (Alert alert : store.getAlerts()) {
            if ("ALL".equals(symbol) || alert.getSymbol().equals(symbol)) {
                alerts++;
            }
        }
        stats.setAlerts(alerts);

        long volume = 0;
        double value = 0;
        long maxOrderSize = 0;
        for (MarketRow row : marketData.getRows()) {
            boolean matches = "ALL".equals(symbol) || row.getSymbol().equals(symbol);
            if (!matches) {
                continue;
            }
            volume += row.getVolume();
            value += row.getVolume() * row.getPrice();
            maxOrderSize = Math.max(maxOrderSize, row.getVolume());
        }
        stats.setTradedVolume(volume);
        stats.setTradedValue(value);
        stats.setVwap(volume == 0 ? 0 : value / volume);
        stats.setNetFlow(round2((random.nextDouble() - 0.5) * 2 * volume * 0.05));

        stats.getSeries().addAll(getSeries(symbol));
        return stats;
    }

    public List<PricePoint> getSeries(String symbol) {
        List<PricePoint> series = history.get(symbol);
        if (series == null) {
            series = history.get(marketData.getRows().isEmpty()
                    ? null
                    : marketData.getRows().get(0).getSymbol());
        }
        return series == null ? new ArrayList<>() : new ArrayList<>(series);
    }

    public List<Long> getVolumeSeries(String symbol) {
        List<Long> series = volumeHistory.get(symbol);
        return series == null ? new ArrayList<>() : new ArrayList<>(series);
    }

    public long getAlertsObserved() {
        return alertsObserved;
    }

    public double[] getSeriesSummary(String symbol) {
        List<PricePoint> series = getSeries(symbol);
        if (series.isEmpty()) {
            return new double[] { 0, 0, 0, 0, 0 };
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double total = 0;
        for (PricePoint point : series) {
            min = Math.min(min, point.getPrice());
            max = Math.max(max, point.getPrice());
            total += point.getPrice();
        }
        double first = series.get(0).getPrice();
        double last = series.get(series.size() - 1).getPrice();
        return new double[] { min, max, total / series.size(), first, last };
    }

    public Map<String, Integer> getAlertDistribution(String symbol) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (String type : util.DemoData.alertTypes()) {
            distribution.put(type, 0);
        }
        for (Alert alert : store.getAlerts()) {
            if (!"ALL".equals(symbol) && !alert.getSymbol().equals(symbol)) {
                continue;
            }
            Integer current = distribution.get(alert.getType());
            if (current == null) {
                distribution.put(alert.getType(), 1);
            } else {
                distribution.put(alert.getType(), current + 1);
            }
        }
        return distribution;
    }

    public Map<String, String> getSummaryTable(String symbol) {
        Map<String, String> rows = new HashMap<>();
        AnalyticsStats stats = buildStats(symbol);
        rows.put("Total Orders", String.format("%,d", stats.getTotalOrders()));
        rows.put("Executed Orders", String.format("%,d", stats.getExecutedOrders()));
        rows.put("Cancelled Orders", String.format("%,d", stats.getCancelledOrders()));
        rows.put("Alerts", String.format("%,d", stats.getAlerts()));
        rows.put("Avg Order Lifetime", String.format("%.0f ms", stats.getAverageOrderLifetimeMs()));
        rows.put("VWAP", String.format("%.2f", stats.getVwap()));
        return rows;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
