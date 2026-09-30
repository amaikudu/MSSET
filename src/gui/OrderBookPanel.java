package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.LiveOrder;
import model.MarketRow;
import service.MarketDataService;
import util.DarkTheme;
import util.DemoData;

public class OrderBookPanel extends JPanel implements MarketDataService.MarketListener {

    private static final int COL_SIDE = 0;
    private static final int COL_PRICE = 1;
    private static final int COL_SIZE = 2;
    private static final int COL_ORDERS = 3;
    private static final int COL_DEPTH = 4;

    private static final int COL_ORDER_ID = 0;
    private static final int COL_ORDER_SYMBOL = 1;
    private static final int COL_ORDER_SIDE = 2;
    private static final int COL_ORDER_PRICE = 3;
    private static final int COL_ORDER_QTY = 4;
    private static final int COL_ORDER_FILLED = 5;
    private static final int COL_ORDER_STATUS = 6;
    private static final int COL_ORDER_TIME = 7;

    private final MarketDataService marketData;
    private final Random random = new Random(5150L);

    private final JComboBox<String> symbolBox = new JComboBox<>(DemoData.symbols());
    private final JSpinner depthSpinner = DarkTheme.spinner(3, 20, 1, 8);
    private final JButton refreshButton = DarkTheme.button("Refresh Book");
    private final JCheckBox groupCheck = new JCheckBox("Group by price", true);
    private final JToggleButton autoUpdateToggle = new JToggleButton("AUTO", true);

    private final DefaultTableModel bookModel = new DefaultTableModel(
            new Object[] { "Side", "Price", "Size", "Orders", "Depth" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable bookTable = new JTable(bookModel);

    private final JRadioButton buyButton = new JRadioButton("BUY", true);
    private final JRadioButton sellButton = new JRadioButton("SELL");
    private final ButtonGroup sideGroup = new ButtonGroup();
    private final JComboBox<String> orderTypeBox = new JComboBox<>(
            new String[] { "LIMIT", "MARKET", "IOC", "FOK" });
    private final JSpinner priceSpinner = DarkTheme.doubleSpinner(0.01, 100000, 0.01, 228.00);
    private final JSpinner quantitySpinner = DarkTheme.spinner(1, 1000000, 100, 500);
    private final JTextField orderValueField = DarkTheme.field(12);
    private final JButton placeButton = DarkTheme.button("Place Order", DarkTheme.GREEN);
    private final JButton cancelButton = DarkTheme.button("Cancel Order", DarkTheme.RED);
    private final JButton clearButton = DarkTheme.outlineButton("Clear Form");

    private final DefaultTableModel ordersModel = new DefaultTableModel(
            new Object[] { "Order ID", "Symbol", "Side", "Price", "Qty", "Filled", "Status", "Time" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ordersTable = new JTable(ordersModel);

    private final JLabel bidLabel = DarkTheme.statusLabel("--", DarkTheme.GREEN);
    private final JLabel askLabel = DarkTheme.statusLabel("--", DarkTheme.RED);
    private final JLabel midLabel = DarkTheme.bold("--");
    private final JLabel spreadLabel = DarkTheme.muted("Spread: --");
    private final JLabel workingLabel = DarkTheme.bold("Working orders: 0");
    private final JLabel filledLabel = DarkTheme.muted("Filled: 0   Cancelled: 0");
    private final JLabel statusLabel = DarkTheme.statusLabel("BOOK READY", DarkTheme.GREEN);

    private Runnable onOrdersChanged;

    public OrderBookPanel() {
        this(new MarketDataService());
    }

    public OrderBookPanel(MarketDataService marketData) {
        this.marketData = marketData;

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Order Book",
                "Simulated bid/ask depth - place and cancel orders that the market really executes"),
                BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        syncPriceToMarket();
        refresh();

        marketData.addListener(this);
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(DarkTheme.BG);
        center.add(buildToolbar(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 10, 0));
        columns.setBackground(DarkTheme.BG);
        columns.add(buildBookCard());
        columns.add(buildEntryCard());
        center.add(columns, BorderLayout.CENTER);

        center.add(buildOrdersCard(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildToolbar() {
        JPanel bar = DarkTheme.card(new FlowLayout(FlowLayout.LEFT, 8, 6));

        bar.add(DarkTheme.muted("Symbol"));
        symbolBox.setPreferredSize(new Dimension(110, DarkTheme.FIELD_HEIGHT));
        bar.add(symbolBox);

        bar.add(DarkTheme.muted("Depth"));
        bar.add(depthSpinner);
        bar.add(groupCheck);
        groupCheck.setOpaque(false);
        groupCheck.setForeground(DarkTheme.TEXT);

        bar.add(refreshButton);

        autoUpdateToggle.setFont(DarkTheme.FONT_BOLD);
        autoUpdateToggle.setBackground(DarkTheme.SURFACE_ALT);
        autoUpdateToggle.setForeground(DarkTheme.TEXT);
        autoUpdateToggle.setFocusPainted(false);
        bar.add(autoUpdateToggle);

        return bar;
    }

    private JPanel buildBookCard() {
        JPanel card = DarkTheme.card("Market depth", new BorderLayout(0, 8));

        JPanel quoteRow = new JPanel(new GridLayout(1, 4, 8, 0));
        quoteRow.setOpaque(false);
        quoteRow.add(quoteBlock("Best Bid", bidLabel));
        quoteRow.add(quoteBlock("Best Ask", askLabel));
        quoteRow.add(quoteBlock("Mid Price", midLabel));
        quoteRow.add(quoteBlock("Spread", spreadLabel));
        card.add(quoteRow, BorderLayout.NORTH);

        DarkTheme.styleTable(bookTable);
        bookTable.setRowHeight(23);
        bookTable.getColumnModel().getColumn(COL_SIDE).setCellRenderer(new SideRenderer());
        bookTable.getColumnModel().getColumn(COL_PRICE).setCellRenderer(new PriceRenderer());
        DarkTheme.alignRight(bookTable, COL_SIZE);
        DarkTheme.alignRight(bookTable, COL_ORDERS);
        DarkTheme.alignRight(bookTable, COL_DEPTH);
        DarkTheme.setColumnWidth(bookTable, COL_SIDE, 55);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = DarkTheme.scroll(bookTable);
        scroll.setPreferredSize(new Dimension(400, 320));
        card.add(scroll, BorderLayout.CENTER);

        card.add(DarkTheme.muted("Click a level to load its price into the order form"),
                BorderLayout.SOUTH);
        return card;
    }

    private JPanel quoteBlock(String title, JLabel value) {
        JPanel block = new JPanel(new BorderLayout(0, 2));
        block.setOpaque(false);
        block.add(DarkTheme.muted(title), BorderLayout.NORTH);
        block.add(value, BorderLayout.CENTER);
        return block;
    }

    private JPanel buildEntryCard() {
        JPanel card = DarkTheme.card("Order entry", new BorderLayout(0, 8));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        sideGroup.add(buyButton);
        sideGroup.add(sellButton);
        buyButton.setOpaque(false);
        sellButton.setOpaque(false);
        buyButton.setForeground(DarkTheme.GREEN);
        sellButton.setForeground(DarkTheme.RED);
        buyButton.setFont(DarkTheme.FONT_BOLD);
        sellButton.setFont(DarkTheme.FONT_BOLD);

        JPanel sideRow = new JPanel(new GridLayout(1, 2, 8, 0));
        sideRow.setOpaque(false);
        sideRow.add(buyButton);
        sideRow.add(sellButton);
        form.add(titledRow("Side", sideRow));
        form.add(Box.createVerticalStrut(8));

        form.add(titledRow("Order type", orderTypeBox));
        form.add(Box.createVerticalStrut(8));

        JPanel priceRow = new JPanel(new GridLayout(1, 2, 8, 0));
        priceRow.setOpaque(false);
        priceRow.add(titledRow("Limit price", priceSpinner));
        priceRow.add(titledRow("Quantity", quantitySpinner));
        form.add(priceRow);
        form.add(Box.createVerticalStrut(8));

        orderValueField.setEditable(false);
        form.add(titledRow("Order value", orderValueField));
        form.add(Box.createVerticalStrut(10));

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 6));
        buttons.setOpaque(false);
        buttons.add(placeButton);
        buttons.add(cancelButton);
        form.add(buttons);
        form.add(Box.createVerticalStrut(6));
        form.add(clearButton);

        card.add(form, BorderLayout.NORTH);

        JPanel info = DarkTheme.card("How matching works", new GridLayout(0, 1, 0, 3));
        info.add(DarkTheme.muted("A BUY limit order fills when its price reaches the ask."));
        info.add(DarkTheme.muted("A SELL limit order fills when its price reaches the bid."));
        info.add(DarkTheme.muted("Orders fill in parts over the following ticks."));
        info.add(DarkTheme.muted("Orders left working are cancelled after a while."));
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    private JPanel titledRow(String caption, Component component) {
        JPanel row = new JPanel(new BorderLayout(0, 3));
        row.setOpaque(false);
        row.add(DarkTheme.muted(caption), BorderLayout.NORTH);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildOrdersCard() {
        JPanel card = DarkTheme.card("Session orders", new BorderLayout(0, 6));

        DarkTheme.styleTable(ordersTable);
        ordersTable.setRowHeight(23);
        ordersTable.getColumnModel().getColumn(COL_ORDER_SIDE).setCellRenderer(new SideRenderer());
        ordersTable.getColumnModel().getColumn(COL_ORDER_STATUS).setCellRenderer(new OrderStatusRenderer());
        DarkTheme.alignRight(ordersTable, COL_ORDER_PRICE);
        DarkTheme.alignRight(ordersTable, COL_ORDER_QTY);
        DarkTheme.alignRight(ordersTable, COL_ORDER_FILLED);
        DarkTheme.setColumnWidth(ordersTable, COL_ORDER_ID, 85);
        DarkTheme.setColumnWidth(ordersTable, COL_ORDER_SYMBOL, 65);
        DarkTheme.setColumnWidth(ordersTable, COL_ORDER_STATUS, 120);
        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = DarkTheme.scroll(ordersTable);
        scroll.setPreferredSize(new Dimension(900, 170));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(workingLabel);
        left.add(filledLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(DarkTheme.muted("Placed orders are executed by the market simulation"));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        symbolBox.addActionListener(e -> {
            syncPriceToMarket();
            refresh();
        });

        depthSpinner.addChangeListener(e -> refresh());
        refreshButton.addActionListener(e -> refresh());

        autoUpdateToggle.addActionListener(e -> {
            autoUpdateToggle.setForeground(autoUpdateToggle.isSelected()
                    ? DarkTheme.TEXT : DarkTheme.TEXT_MUTED);
            if (autoUpdateToggle.isSelected()) {
                refresh();
            }
        });

        groupCheck.addActionListener(e -> refresh());

        priceSpinner.addChangeListener(e -> updateOrderValue());
        quantitySpinner.addChangeListener(e -> updateOrderValue());

        placeButton.addActionListener(e -> placeOrder());
        cancelButton.addActionListener(e -> cancelSelectedOrder());
        clearButton.addActionListener(e -> clearForm());

        bookTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bookTable.getSelectedRow() >= 0) {
                int row = bookTable.convertRowIndexToModel(bookTable.getSelectedRow());
                String priceText = String.valueOf(bookModel.getValueAt(row, COL_PRICE));
                try {
                    priceSpinner.setValue(Double.parseDouble(priceText));
                } catch (NumberFormatException ignored) {
                    return;
                }
                String side = String.valueOf(bookModel.getValueAt(row, COL_SIDE));
                if (side.startsWith("B")) {
                    buyButton.setSelected(true);
                } else {
                    sellButton.setSelected(true);
                }
            }
        });

        ordersTable.getSelectionModel().addListSelectionListener(e -> updateActionState());
        ordersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = ordersTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        ordersTable.setRowSelectionInterval(row, row);
                    }
                    ordersPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        updateOrderValue();
        updateActionState();
    }

    private final JPopupMenu ordersPopup = buildOrdersPopup();

    private JPopupMenu buildOrdersPopup() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem cancelItem = new JMenuItem("Cancel this order");
        JMenuItem detailsItem = new JMenuItem("Order details");
        JMenuItem copyItem = new JMenuItem("Copy order id");
        cancelItem.addActionListener(e -> cancelSelectedOrder());
        detailsItem.addActionListener(e -> showOrderDetails());
        copyItem.addActionListener(e -> showOrderDetails());
        menu.add(cancelItem);
        menu.add(detailsItem);
        menu.addSeparator();
        menu.add(copyItem);
        return menu;
    }

    private void setSymbolPrice(double price) {
        priceSpinner.setValue(Math.max(0.01, Math.round(price * 100.0) / 100.0));
    }

    private void syncPriceToMarket() {
        String symbol = selectedSymbol();
        setSymbolPrice(marketData.getPrice(symbol));
        updateOrderValue();
    }

    private String selectedSymbol() {
        Object value = symbolBox.getSelectedItem();
        return value == null ? "AAPL" : value.toString();
    }

    private void updateOrderValue() {
        double price = ((Number) priceSpinner.getValue()).doubleValue();
        int quantity = (Integer) quantitySpinner.getValue();
        orderValueField.setText(String.format("%,.2f", price * quantity));
    }

    private void updateActionState() {
        LiveOrder selected = getSelectedOrder();
        boolean working = selected != null && selected.isWorking();
        cancelButton.setEnabled(working);
    }

    private LiveOrder getSelectedOrder() {
        int viewRow = ordersTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = ordersTable.convertRowIndexToModel(viewRow);
        String id = String.valueOf(ordersModel.getValueAt(modelRow, COL_ORDER_ID));
        for (LiveOrder order : marketData.getOrders()) {
            if (order.getOrderId().equals(id)) {
                return order;
            }
        }
        return null;
    }

    private void placeOrder() {
        String symbol = selectedSymbol();
        String side = buyButton.isSelected() ? LiveOrder.SIDE_BUY : LiveOrder.SIDE_SELL;
        double price = ((Number) priceSpinner.getValue()).doubleValue();
        int quantity = (Integer) quantitySpinner.getValue();

        LiveOrder order = marketData.placeOrder(symbol, side, price, quantity);
        statusLabel.setText("  \u25CF   ORDER " + order.getOrderId() + " PLACED");
        statusLabel.setForeground(DarkTheme.GREEN);

        refreshOrders();
        selectOrder(order);
        notifyOrdersChanged();
    }

    private void cancelSelectedOrder() {
        LiveOrder order = getSelectedOrder();
        if (order == null) {
            DarkTheme.info(this, "Select an order in the session table first.", "Order Book");
            return;
        }
        if (!order.isWorking()) {
            DarkTheme.info(this, "Order " + order.getOrderId() + " is already " + order.getStatus() + ".", "Order Book");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel order " + order.getOrderId() + "?\n\n"
              + "Symbol   : " + order.getSymbol() + "\n"
              + "Side     : " + order.getSide() + "\n"
              + "Quantity : " + order.getQuantity() + "\n"
              + "Filled   : " + order.getFilledQuantity(),
                "Confirm cancellation", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (marketData.cancelOrder(order.getOrderId())) {
            statusLabel.setText("  \u25CF   ORDER " + order.getOrderId() + " CANCELLED");
            statusLabel.setForeground(DarkTheme.AMBER);
            refreshOrders();
            notifyOrdersChanged();
        }
    }

    private void showOrderDetails() {
        LiveOrder order = getSelectedOrder();
        if (order == null) {
            return;
        }
        DarkTheme.info(this, "Order " + order.getOrderId() + "\n"
              + "--------------------------------------\n"
              + "Symbol      : " + order.getSymbol() + "\n"
              + "Side        : " + order.getSide() + "\n"
              + "Limit price : " + String.format("%.2f", order.getPrice()) + "\n"
              + "Quantity    : " + order.getQuantity() + "\n"
              + "Filled      : " + order.getFilledQuantity() + "\n"
              + "Remaining   : " + order.getRemainingQuantity() + "\n"
              + "Status      : " + order.getStatus() + "\n"
              + "Placed at   : " + order.getCreatedText() + "\n"
              + "Lifetime    : " + order.getLifetimeMs() + " ms",
                "Order details");
    }

    private void clearForm() {
        buyButton.setSelected(true);
        orderTypeBox.setSelectedIndex(0);
        quantitySpinner.setValue(500);
        syncPriceToMarket();
    }

    private void selectOrder(LiveOrder order) {
        for (int i = 0; i < ordersModel.getRowCount(); i++) {
            if (order.getOrderId().equals(ordersModel.getValueAt(i, COL_ORDER_ID))) {
                ordersTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    public void onOrdersChanged(Runnable action) {
        this.onOrdersChanged = action;
    }

    private void notifyOrdersChanged() {
        if (onOrdersChanged != null) {
            onOrdersChanged.run();
        }
    }

    public void refresh() {
        refreshBook();
        refreshQuote();
        refreshOrders();
    }

    private void refreshBook() {
        String symbol = selectedSymbol();
        MarketRow row = marketData.getRow(symbol);
        if (row == null) {
            return;
        }

        double bid = row.getBid();
        double ask = row.getAsk();
        double tick = Math.max(0.01, (ask - bid) / 2.0);
        int depth = (Integer) depthSpinner.getValue();

        List<Object[]> rows = new ArrayList<>();

        for (int i = 0; i < depth; i++) {
            double price = Math.round((ask + i * tick) * 100.0) / 100.0;
            int size = levelSize(symbol, "ASK", i);
            int orders = 1 + levelSize(symbol, "ASK", i + 900) % 9;
            rows.add(new Object[] { "ASK", String.format("%.2f", price),
                    String.format("%,d", size), String.valueOf(orders), bar(size / 50) });
        }

        for (int i = 0; i < depth; i++) {
            double price = Math.round((bid - i * tick) * 100.0) / 100.0;
            int size = levelSize(symbol, "BID", i);
            int orders = 1 + levelSize(symbol, "BID", i + 900) % 9;
            rows.add(new Object[] { "BID", String.format("%.2f", price),
                    String.format("%,d", size), String.valueOf(orders), bar(size / 50) });
        }

        int selectedRow = bookTable.getSelectedRow();
        String selectedPrice = selectedRow < 0 ? null
                : String.valueOf(bookModel.getValueAt(
                        bookTable.convertRowIndexToModel(selectedRow), COL_PRICE));

        bookModel.setRowCount(0);
        for (Object[] r : rows) {
            bookModel.addRow(r);
        }

        if (selectedPrice != null) {
            for (int i = 0; i < bookModel.getRowCount(); i++) {
                if (selectedPrice.equals(bookModel.getValueAt(i, COL_PRICE))
                        && "ASK".equals(bookModel.getValueAt(i, COL_SIDE))) {
                    bookTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private int levelSize(String symbol, String side, int level) {
        int seed = symbol.hashCode() * 31 + side.hashCode() * 17 + level * 7919;
        Random levelRandom = new Random(seed);
        return 100 + levelRandom.nextInt(4900);
    }

    private String bar(int units) {
        int count = Math.max(1, Math.min(20, units / 20));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append('\u2588');
        }
        return sb.toString();
    }

    private void refreshQuote() {
        MarketRow row = marketData.getRow(selectedSymbol());
        if (row == null) {
            return;
        }
        bidLabel.setText(String.format("  \u25CF   %.2f", row.getBid()));
        askLabel.setText(String.format("  \u25CF   %.2f", row.getAsk()));
        midLabel.setText(String.format("%.2f", row.getPrice()));
        midLabel.setForeground(row.getChange() >= 0 ? DarkTheme.GREEN : DarkTheme.RED);
        spreadLabel.setText(String.format("Spread: %.4f", row.getSpread()));
    }

    private void refreshOrders() {
        List<LiveOrder> orders = marketData.getOrders();

        ordersModel.setRowCount(0);
        for (LiveOrder order : orders) {
            ordersModel.addRow(new Object[] {
                order.getOrderId(),
                order.getSymbol(),
                order.getSide(),
                String.format("%.2f", order.getPrice()),
                String.format("%,d", order.getQuantity()),
                String.format("%,d", order.getFilledQuantity()),
                order.getStatus(),
                order.getCreatedText()
            });
        }

        workingLabel.setText("Working orders: " + marketData.getWorkingOrders().size());
        filledLabel.setText(String.format("Filled: %d   Cancelled: %d",
                marketData.getFilledOrderCount(), marketData.getCancelledOrderCount()));
        updateActionState();
    }

    @Override
    public void onMarketUpdate(List<MarketRow> rows) {
        if (!autoUpdateToggle.isSelected()) {
            return;
        }
        refreshBook();
        refreshQuote();
        refreshOrders();
    }

    private static class SideRenderer extends DefaultTableCellRenderer {
        SideRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String side = String.valueOf(value);
                c.setForeground(side.startsWith("B") ? DarkTheme.GREEN : DarkTheme.RED);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }

    private static class PriceRenderer extends DefaultTableCellRenderer {
        PriceRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
            setFont(DarkTheme.FONT_MONO);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
                String side = String.valueOf(t.getValueAt(row, COL_SIDE));
                c.setForeground(side.startsWith("B") ? DarkTheme.GREEN : DarkTheme.RED);
            }
            return c;
        }
    }

    private static class OrderStatusRenderer extends DefaultTableCellRenderer {
        OrderStatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_SMALL);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String status = String.valueOf(value);
                if (LiveOrder.STATUS_FILLED.equals(status)) {
                    c.setForeground(DarkTheme.GREEN);
                } else if (LiveOrder.STATUS_CANCELLED.equals(status)) {
                    c.setForeground(DarkTheme.RED);
                } else if (LiveOrder.STATUS_PARTIAL.equals(status)) {
                    c.setForeground(DarkTheme.PURPLE);
                } else {
                    c.setForeground(DarkTheme.AMBER);
                }
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }
}
