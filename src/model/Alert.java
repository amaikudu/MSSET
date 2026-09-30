package model;

import util.DateTimeUtil;

public class Alert {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_REVIEW = "UNDER REVIEW";
    public static final String STATUS_CLOSED = "CLOSED";

    private final String alertId;
    private String symbol;
    private String type;
    private int riskScore;
    private long timestamp;
    private String status;
    private String trader;
    private int quantity;
    private double price;
    private final String description;

    public Alert(String alertId, String symbol, String type, int riskScore, long timestamp,
                 String status, String trader, int quantity, double price, String description) {
        this.alertId = alertId;
        this.symbol = symbol;
        this.type = type;
        this.riskScore = riskScore;
        this.timestamp = timestamp;
        this.status = status;
        this.trader = trader;
        this.quantity = quantity;
        this.price = price;
        this.description = description;
    }

    public String getAlertId() {
        return alertId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTrader() {
        return trader;
    }

    public void setTrader(String trader) {
        this.trader = trader;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        if (riskScore >= 85) {
            return "CRITICAL";
        } else if (riskScore >= 65) {
            return "HIGH";
        } else if (riskScore >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public String getTimeText() {
        return DateTimeUtil.time(timestamp);
    }

    public String getDetailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("ALERT ").append(alertId).append("   [").append(getSeverity()).append("]\n");
        sb.append("------------------------------------------------------\n");
        sb.append(String.format("Type        : %s%n", type));
        sb.append(String.format("Symbol      : %s%n", symbol));
        sb.append(String.format("Trader ID   : %s%n", trader));
        sb.append(String.format("Risk Score  : %d / 100%n", riskScore));
        sb.append(String.format("Quantity    : %,d%n", quantity));
        sb.append(String.format("Price       : %.2f%n", price));
        sb.append(String.format("Detected    : %s%n", DateTimeUtil.dateTime(timestamp)));
        sb.append(String.format("Status      : %s%n", status));
        sb.append("------------------------------------------------------\n");
        sb.append("DETAIL\n");
        sb.append(description).append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return alertId + " | " + symbol + " | " + type;
    }
}
