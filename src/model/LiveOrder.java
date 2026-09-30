package model;

import util.DateTimeUtil;

public class LiveOrder {

    public static final String SIDE_BUY = "BUY";
    public static final String SIDE_SELL = "SELL";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PARTIAL = "PARTIALLY FILLED";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final String orderId;
    private final String symbol;
    private final String side;
    private final double price;
    private final int quantity;
    private int filledQuantity;
    private String status;
    private final long createdAt;
    private long lastUpdate;
    private boolean countedCancelled;

    public LiveOrder(String orderId, String symbol, String side, double price, int quantity, long createdAt) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = 0;
        this.status = STATUS_PENDING;
        this.createdAt = createdAt;
        this.lastUpdate = createdAt;
        this.countedCancelled = false;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(int filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public int getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdate = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isCountedCancelled() {
        return countedCancelled;
    }

    public void setCountedCancelled(boolean countedCancelled) {
        this.countedCancelled = countedCancelled;
    }

    public boolean isBuy() {
        return SIDE_BUY.equals(side);
    }

    public boolean isWorking() {
        return STATUS_PENDING.equals(status) || STATUS_PARTIAL.equals(status);
    }

    public long getLifetimeMs() {
        return Math.max(0, lastUpdate - createdAt);
    }

    public String getCreatedText() {
        return DateTimeUtil.time(createdAt);
    }

    public double getNotional() {
        return price * quantity;
    }

    @Override
    public String toString() {
        return orderId;
    }
}
