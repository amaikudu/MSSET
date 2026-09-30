package model;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsStats {

    private String symbol = "ALL";
    private long totalOrders;
    private long executedOrders;
    private long cancelledOrders;
    private long alerts;
    private double averageOrderLifetimeMs;
    private long maxOrderSize;
    private double tradedValue;
    private long tradedVolume;
    private double vwap;
    private double netFlow;

    public static class PricePoint {
        private final long time;
        private final double price;

        public PricePoint(long time, double price) {
            this.time = time;
            this.price = price;
        }

        public long getTime() {
            return time;
        }

        public double getPrice() {
            return price;
        }
    }

    private final List<PricePoint> series = new ArrayList<>();

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getExecutedOrders() {
        return executedOrders;
    }

    public void setExecutedOrders(long executedOrders) {
        this.executedOrders = executedOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public long getAlerts() {
        return alerts;
    }

    public void setAlerts(long alerts) {
        this.alerts = alerts;
    }

    public double getAverageOrderLifetimeMs() {
        return averageOrderLifetimeMs;
    }

    public void setAverageOrderLifetimeMs(double averageOrderLifetimeMs) {
        this.averageOrderLifetimeMs = averageOrderLifetimeMs;
    }

    public long getMaxOrderSize() {
        return maxOrderSize;
    }

    public void setMaxOrderSize(long maxOrderSize) {
        this.maxOrderSize = maxOrderSize;
    }

    public double getTradedValue() {
        return tradedValue;
    }

    public void setTradedValue(double tradedValue) {
        this.tradedValue = tradedValue;
    }

    public long getTradedVolume() {
        return tradedVolume;
    }

    public void setTradedVolume(long tradedVolume) {
        this.tradedVolume = tradedVolume;
    }

    public double getVwap() {
        return vwap;
    }

    public void setVwap(double vwap) {
        this.vwap = vwap;
    }

    public double getNetFlow() {
        return netFlow;
    }

    public void setNetFlow(double netFlow) {
        this.netFlow = netFlow;
    }

    public List<PricePoint> getSeries() {
        return series;
    }

    public int getExecutionRatePercent() {
        if (totalOrders == 0) {
            return 0;
        }
        return (int) Math.round(executedOrders * 100.0 / totalOrders);
    }

    public int getCancellationRatePercent() {
        if (totalOrders == 0) {
            return 0;
        }
        return (int) Math.round(cancelledOrders * 100.0 / totalOrders);
    }
}
