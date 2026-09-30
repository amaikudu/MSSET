package util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DemoData {

    private static final Map<String, String> SYMBOLS = new LinkedHashMap<>();

    static {
        SYMBOLS.put("AAPL", "Apple Inc.");
        SYMBOLS.put("MSFT", "Microsoft Corp.");
        SYMBOLS.put("GOOGL", "Alphabet Inc.");
        SYMBOLS.put("AMZN", "Amazon.com Inc.");
        SYMBOLS.put("NVDA", "NVIDIA Corp.");
        SYMBOLS.put("META", "Meta Platforms");
        SYMBOLS.put("TSLA", "Tesla Inc.");
        SYMBOLS.put("AMD", "Advanced Micro Devices");
        SYMBOLS.put("INTC", "Intel Corp.");
        SYMBOLS.put("NFLX", "Netflix Inc.");
        SYMBOLS.put("JPM", "JPMorgan Chase");
        SYMBOLS.put("BAC", "Bank of America");
        SYMBOLS.put("GS", "Goldman Sachs");
        SYMBOLS.put("MS", "Morgan Stanley");
        SYMBOLS.put("WMT", "Walmart Inc.");
        SYMBOLS.put("COST", "Costco Wholesale");
        SYMBOLS.put("DIS", "Walt Disney Co.");
        SYMBOLS.put("BA", "Boeing Co.");
        SYMBOLS.put("PFE", "Pfizer Inc.");
        SYMBOLS.put("XOM", "Exxon Mobil");
    }

    private static final Map<String, Double> START_PRICES = new LinkedHashMap<>();

    static {
        START_PRICES.put("AAPL", 228.40);
        START_PRICES.put("MSFT", 431.15);
        START_PRICES.put("GOOGL", 168.75);
        START_PRICES.put("AMZN", 186.30);
        START_PRICES.put("NVDA", 121.85);
        START_PRICES.put("META", 573.20);
        START_PRICES.put("TSLA", 254.60);
        START_PRICES.put("AMD", 158.90);
        START_PRICES.put("INTC", 22.45);
        START_PRICES.put("NFLX", 712.50);
        START_PRICES.put("JPM", 218.30);
        START_PRICES.put("BAC", 40.15);
        START_PRICES.put("GS", 496.80);
        START_PRICES.put("MS", 102.35);
        START_PRICES.put("WMT", 80.25);
        START_PRICES.put("COST", 882.40);
        START_PRICES.put("DIS", 96.70);
        START_PRICES.put("BA", 154.20);
        START_PRICES.put("PFE", 29.85);
        START_PRICES.put("XOM", 118.45);
    }

    private DemoData() {

    }

    public static String[] symbols() {
        return SYMBOLS.keySet().toArray(new String[0]);
    }

    public static String name(String symbol) {
        String name = SYMBOLS.get(symbol);
        return name == null ? symbol : name;
    }

    public static String display(String symbol) {
        return symbol + "  \u2013  " + name(symbol);
    }

    public static double startPrice(String symbol) {
        Double price = START_PRICES.get(symbol);
        return price == null ? 100.0 : price;
    }

    public static String[] sampleReplayFiles() {
        return new String[] {
            "data/sample_itch_2019-07-05.bin",
            "data/NASDAQ_totalview_2024-03-11.itch",
            "data/simulated_session_A.edf",
            "data/simulated_session_B.edf"
        };
    }

    public static String[] detectionTypes() {
        return new String[] {
            "Spoofing / Layering",
            "Wash Trading",
            "Marking the Close",
            "Quote Stuffing",
            "Large Order Cancellation",
            "Momentum Ignition",
            "Unusual Volume Spike"
        };
    }

    public static String[] alertTypes() {
        return new String[] {
            "Spoofing",
            "Wash Trade",
            "Layering",
            "Quote Stuffing",
            "Large Cancellation",
            "Volume Spike",
            "Price Anomaly"
        };
    }

    public static String[] reportTypes() {
        return new String[] {
            "Daily Surveillance Summary",
            "Alert Disposition Report",
            "Rule Effectiveness Report",
            "Trader Behaviour Report",
            "Order Lifecycle Audit",
            "Regulatory Export (CAT)"
        };
    }
}
