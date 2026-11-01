package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import model.LiveOrder;
import model.MarketRow;
import util.DemoData;

public class MarketDataService {

    public interface MarketListener {
        void onMarketUpdate(List<MarketRow> rows);
    }

    public static final int DEFAULT_TICK_MS = 1000;

    private static final double MAX_TICK_MOVE_PERCENT = 0.12;

    private final Map<String, MarketRow> rows = new LinkedHashMap<>();
    private final Map<String, Double> openPrices = new LinkedHashMap<>();
    private final List<LiveOrder> orders = new ArrayList<>();
    private final List<MarketListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ChangeListener> orderListeners = new CopyOnWriteArrayList<>();

    private final Random random = new Random(20241001L);
    private final Timer timer;

    private int tickMs = DEFAULT_TICK_MS;
    private long tickCount = 0;

    public MarketDataService() {
        loadDemoData();

        timer = new Timer(tickMs, e -> tick());
    }

    private void loadDemoData() {
        for (String symbol : DemoData.symbols()) {
            double price = round2(DemoData.startPrice(symbol));
            double spread = Math.max(0.02, price * 0.0004);
            long volume = 1_000_000L + random.nextInt(7_000_000);
            MarketRow row = new MarketRow(
                    symbol,
                    DemoData.name(symbol),
                    price,
                    round2(price - spread / 2),
                    round2(price + spread / 2),
                    volume,
                    0.0,
                    System.currentTimeMillis());
            rows.put(symbol, row);
            openPrices.put(symbol, price);
        }
    }

    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    public void setTickInterval(int millis) {
        this.tickMs = Math.max(200, millis);
        timer.setDelay(this.tickMs);
        timer.setInitialDelay(this.tickMs);
    }

    public int getTickInterval() {
        return tickMs;
    }

    public long getTickCount() {
        return tickCount;
    }

    public long getSimulatedMillisPerTick() {
        return 1000L;
    }

    public void addListener(MarketListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(MarketListener listener) {
        listeners.remove(listener);
    }

    public void addOrderListener(ChangeListener listener) {
        if (listener != null) {
            orderListeners.add(listener);
        }
    }

    private void fireOrderChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : orderListeners) {
            listener.stateChanged(event);
        }
    }

    public List<MarketRow> getRows() {
        return Collections.unmodifiableList(new ArrayList<>(rows.values()));
    }

    public MarketRow getRow(String symbol) {
        return rows.get(symbol);
    }

    public double getPrice(String symbol) {
        MarketRow row = rows.get(symbol);
        return row == null ? 0 : row.getPrice();
    }

    public double getOpenPrice(String symbol) {
        Double open = openPrices.get(symbol);
        return open == null ? 0 : open;
    }

    public long getTotalVolume() {
        long total = 0;
        for (MarketRow row : rows.values()) {
            total += row.getVolume();
        }
        return total;
    }

    public int[] getAdvanceDecline() {
        int up = 0;
        int down = 0;
        for (MarketRow row : rows.values()) {
            if (row.getChange() > 0) {
                up++;
            } else if (row.getChange() < 0) {
                down++;
            }
        }
        return new int[] { up, down, rows.size() - up - down };
    }

    public LiveOrder placeOrder(String symbol, String side, double price, int quantity) {
        String orderId = String.format("ORD-%05d", orders.size() + 1);
        LiveOrder order = new LiveOrder(orderId, symbol, side, price, quantity, System.currentTimeMillis());
        orders.add(order);
        fireOrderChanged();
        return order;
    }

    public boolean cancelOrder(String orderId) {
        for (LiveOrder order : orders) {
            if (order.getOrderId().equals(orderId) && order.isWorking()) {
                order.setStatus(LiveOrder.STATUS_CANCELLED);
                order.setCountedCancelled(true);
                fireOrderChanged();
                return true;
            }
        }
        return false;
    }

    public void simulateAbuseBurst(String symbol) {
        if (symbol == null || !rows.containsKey(symbol)) {
            symbol = "AAPL";
        }
        MarketRow row = rows.get(symbol);
        double currentPrice = row != null ? row.getPrice() : 228.40;
        List<LiveOrder> placed = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double price = round2(currentPrice - 0.05 - (i * 0.02));
            int size = 5000 + (i * 500);
            placed.add(placeOrder(symbol, LiveOrder.SIDE_BUY, price, size));
        }
        for (int i = 0; i < placed.size() - 1; i++) {
            cancelOrder(placed.get(i).getOrderId());
        }
        placeOrder(symbol, LiveOrder.SIDE_SELL, round2(currentPrice + 0.02), 10000);
        if (row != null) {
            row.setVolume(row.getVolume() + 55000);
            row.setPrice(round2(currentPrice + 0.35));
        }
        List<MarketRow> snapshot = getRows();
        for (MarketListener listener : listeners) {
            listener.onMarketUpdate(snapshot);
        }
    }

    public List<LiveOrder> getOrders() {
        List<LiveOrder> copy = new ArrayList<>(orders);
        Collections.reverse(copy);
        return copy;
    }

    public List<LiveOrder> getWorkingOrders() {
        List<LiveOrder> result = new ArrayList<>();
        for (LiveOrder order : orders) {
            if (order.isWorking()) {
                result.add(order);
            }
        }
        return result;
    }

    public int getOrderCount() {
        return orders.size();
    }

    public long getFilledOrderCount() {
        long count = 0;
        for (LiveOrder order : orders) {
            if (LiveOrder.STATUS_FILLED.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public long getCancelledOrderCount() {
        long count = 0;
        for (LiveOrder order : orders) {
            if (LiveOrder.STATUS_CANCELLED.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public double getAverageOrderLifetimeMs() {
        long total = 0;
        int count = 0;
        for (LiveOrder order : orders) {
            if (!order.isWorking()) {
                total += order.getLifetimeMs();
                count++;
            }
        }
        return count == 0 ? 0 : (double) total / count;
    }

    public long getMaxOrderSize() {
        long max = 0;
        for (LiveOrder order : orders) {
            max = Math.max(max, order.getQuantity());
        }
        return max;
    }

    private void tick() {
        tickCount++;

        for (MarketRow row : rows.values()) {
            movePrice(row);
            updateQuote(row);
            addVolume(row);
        }

        matchWorkingOrders();

        cancelStaleOrders();

        List<MarketRow> snapshot = getRows();
        for (MarketListener listener : listeners) {
            listener.onMarketUpdate(snapshot);
        }
    }

    private void movePrice(MarketRow row) {
        double price = row.getPrice();
        double open = getOpenPrice(row.getSymbol());

        double drift = (open - price) / open * 0.05;
        double noise = (random.nextDouble() - 0.5) * 2 * MAX_TICK_MOVE_PERCENT / 100.0;
        double factor = 1.0 + drift + noise;

        double newPrice = round2(Math.max(0.01, price * factor));
        row.setPrice(newPrice);
        row.setChange(round2(newPrice - open));
        row.setTimestamp(System.currentTimeMillis());
    }

    private void updateQuote(MarketRow row) {
        double price = row.getPrice();
        double spread = Math.max(0.02, round2(price * (0.0002 + random.nextDouble() * 0.0006)));
        row.setBid(round2(price - spread / 2));
        row.setAsk(round2(price + spread / 2));
    }

    private void addVolume(MarketRow row) {
        long add = 1_000L + random.nextInt(20_000);
        row.setVolume(row.getVolume() + add);
    }

    private void matchWorkingOrders() {
        boolean changed = false;

        for (LiveOrder order : orders) {
            if (!order.isWorking()) {
                continue;
            }
            MarketRow row = rows.get(order.getSymbol());
            if (row == null) {
                continue;
            }

            boolean marketReached = order.isBuy()
                    ? order.getPrice() >= row.getAsk()
                    : order.getPrice() <= row.getBid();

            if (!marketReached) {
                continue;
            }

            int remaining = order.getRemainingQuantity();
            int fill = Math.max(1, Math.min(remaining, 50 + random.nextInt(450)));
            order.setFilledQuantity(order.getFilledQuantity() + fill);
            order.setLastUpdate(System.currentTimeMillis());
            order.setStatus(order.getRemainingQuantity() <= 0
                    ? LiveOrder.STATUS_FILLED
                    : LiveOrder.STATUS_PARTIAL);
            changed = true;
        }

        if (changed) {
            fireOrderChanged();
        }
    }

    private void cancelStaleOrders() {
        boolean changed = false;
        for (LiveOrder order : orders) {
            if (order.isWorking() && order.getLifetimeMs() > 15_000 && random.nextInt(100) < 8) {
                order.setStatus(LiveOrder.STATUS_CANCELLED);
                order.setCountedCancelled(true);
                changed = true;
            }
        }
        if (changed) {
            fireOrderChanged();
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
