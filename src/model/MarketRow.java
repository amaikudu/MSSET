package model;

public class MarketRow {

    private final String symbol;
    private final String company;
    private double price;
    private double bid;
    private double ask;
    private long volume;
    private double change;
    private long timestamp;

    public MarketRow(String symbol, String company, double price, double bid, double ask,
                     long volume, double change, long timestamp) {
        this.symbol = symbol;
        this.company = company;
        this.price = price;
        this.bid = bid;
        this.ask = ask;
        this.volume = volume;
        this.change = change;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompany() {
        return company;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getChangePercent() {
        double open = price - change;
        if (open == 0) {
            return 0;
        }
        return change / open * 100.0;
    }

    public double getSpread() {
        return ask - bid;
    }

    public String getChangeText() {
        return String.format("%+.2f", change);
    }

    @Override
    public String toString() {
        return symbol;
    }
}
