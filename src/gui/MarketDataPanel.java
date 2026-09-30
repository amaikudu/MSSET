package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import model.MarketRow;
import service.MarketDataService;
import util.DarkTheme;
import util.DemoData;

public class MarketDataPanel extends JPanel implements MarketDataService.MarketListener {

    private static final int COL_SYMBOL = 0;
    private static final int COL_COMPANY = 1;
    private static final int COL_PRICE = 2;
    private static final int COL_BID = 3;
    private static final int COL_ASK = 4;
    private static final int COL_VOLUME = 5;
    private static final int COL_CHANGE = 6;
    private static final int COL_TIME = 7;

    private final MarketDataService marketData;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] { "Symbol", "Company", "Last", "Bid", "Ask", "Volume", "Change", "Time" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JTextField searchField = DarkTheme.searchField("Search symbol or company...", 18);
    private final JComboBox<String> symbolBox = new JComboBox<>(DemoData.symbols());
    private final JComboBox<String> viewBox = new JComboBox<>(
            new String[] { "All symbols", "Top gainers", "Top losers", "Watchlist only" });
    private final JButton refreshButton = DarkTheme.button("Refresh Now");
    private final JToggleButton liveToggle = new JToggleButton("LIVE", true);
    private final JCheckBox autoScrollCheck = new JCheckBox("Auto-refresh table");
    private final JSpinner intervalSpinner = DarkTheme.spinner(1, 60, 1, 1);

    private final DefaultListModel<String> watchlistModel = new DefaultListModel<>();
    private final JList<String> watchlist = new JList<>(watchlistModel);

    private final JLabel statusLabel = DarkTheme.statusLabel("FEED CONNECTED", DarkTheme.GREEN);
    private final JLabel rateLabel = DarkTheme.muted("Updates: 0");
    private final JLabel symbolCountLabel = DarkTheme.muted("Symbols: 0");
    private final JLabel lastUpdateLabel = DarkTheme.muted("Last update: --:--:--");
    private final JLabel selectedLabel = DarkTheme.bold("Selected: AAPL");

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private long updateCount = 0;
    private String lastSelectedSymbol = "AAPL";

    public MarketDataPanel() {
        this(new MarketDataService());
    }

    public MarketDataPanel(MarketDataService marketData) {
        this.marketData = marketData;

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Market Data",
                "Simulated live feed - search, filter and inspect instrument quotes"), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        wireEvents();
        refreshTable(null);

        marketData.addListener(this);
        liveToggle.setSelected(marketData.isRunning());
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(DarkTheme.BG);
        center.add(buildToolbar(), BorderLayout.NORTH);
        center.add(DarkTheme.scroll(table), BorderLayout.CENTER);
        center.add(buildSidePanel(), BorderLayout.EAST);
        return center;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = DarkTheme.card(new FlowLayout(FlowLayout.LEFT, 8, 6));

        toolbar.add(DarkTheme.muted("Search"));
        toolbar.add(searchField);

        toolbar.add(DarkTheme.muted("Symbol"));
        symbolBox.setPreferredSize(new Dimension(110, DarkTheme.FIELD_HEIGHT));
        toolbar.add(symbolBox);

        toolbar.add(DarkTheme.muted("View"));
        viewBox.setPreferredSize(new Dimension(150, DarkTheme.FIELD_HEIGHT));
        toolbar.add(viewBox);

        toolbar.add(Box.createHorizontalStrut(4));
        toolbar.add(refreshButton);

        liveToggle.setFont(DarkTheme.FONT_BOLD);
        liveToggle.setBackground(DarkTheme.SURFACE_ALT);
        liveToggle.setForeground(DarkTheme.TEXT);
        liveToggle.setFocusPainted(false);
        toolbar.add(liveToggle);

        toolbar.add(DarkTheme.muted("Every"));
        toolbar.add(intervalSpinner);
        toolbar.add(DarkTheme.muted("s"));
        toolbar.add(autoScrollCheck);

        return toolbar;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(DarkTheme.BG);
        side.setPreferredSize(new Dimension(250, 100));
        side.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        JPanel selectedCard = DarkTheme.card("Selected Instrument", new BorderLayout(0, 6));
        selectedLabel.setFont(DarkTheme.FONT_BOLD);
        selectedCard.add(selectedLabel, BorderLayout.NORTH);
        selectedCard.add(DarkTheme.muted("<html>Click any row in the table to inspect<br>"
                + "the instrument here.</html>"), BorderLayout.CENTER);
        side.add(selectedCard);
        side.add(Box.createVerticalStrut(10));

        JPanel watchCard = DarkTheme.card("Watchlist", new BorderLayout(0, 6));
        watchlist.setBackground(DarkTheme.SURFACE_ALT);
        watchlist.setForeground(DarkTheme.TEXT);
        watchlist.setFont(DarkTheme.FONT_BASE);
        watchlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        watchlist.setVisibleRowCount(8);
        JScrollPane listScroll = DarkTheme.scroll(watchlist);
        watchScroll = listScroll;
        watchCard.add(listScroll, BorderLayout.CENTER);

        JPanel watchButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        watchButtons.setOpaque(false);
        JButton addWatch = DarkTheme.outlineButton("Add");
        JButton removeWatch = DarkTheme.outlineButton("Remove");
        watchButtons.add(addWatch);
        watchButtons.add(removeWatch);
        watchCard.add(watchButtons, BorderLayout.SOUTH);

        addWatch.addActionListener(e -> addSelectedToWatchlist());
        removeWatch.addActionListener(e -> removeFromWatchlist());

        side.add(watchCard);
        side.add(Box.createVerticalStrut(10));

        JPanel breadth = DarkTheme.card("Market Breadth", new GridLayout(3, 2, 6, 4));
        upLabel = DarkTheme.statusLabel("0", DarkTheme.GREEN);
        downLabel = DarkTheme.statusLabel("0", DarkTheme.RED);
        flatLabel = DarkTheme.statusLabel("0", DarkTheme.TEXT_MUTED);
        breadth.add(DarkTheme.muted("Advancing"));
        breadth.add(upLabel);
        breadth.add(DarkTheme.muted("Declining"));
        breadth.add(downLabel);
        breadth.add(DarkTheme.muted("Unchanged"));
        breadth.add(flatLabel);
        side.add(breadth);

        side.add(Box.createVerticalGlue());
        return side;
    }

    private JScrollPane watchScroll;
    private JLabel upLabel;
    private JLabel downLabel;
    private JLabel flatLabel;

    private JPanel buildStatusBar() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(symbolCountLabel);
        left.add(rateLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(lastUpdateLabel);
        right.add(DarkTheme.muted("Source: SIMULATED ITCH FEED"));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        refreshButton.addActionListener(e -> {
            refreshTable(null);
            DarkTheme.styleTable(table);
        });

        liveToggle.addActionListener(e -> {
            if (liveToggle.isSelected()) {
                marketData.start();
                liveToggle.setText("LIVE");
                liveToggle.setForeground(DarkTheme.TEXT);
                statusLabel.setText("  \u25CF   FEED CONNECTED");
                statusLabel.setForeground(DarkTheme.GREEN);
            } else {
                marketData.stop();
                liveToggle.setText("PAUSED");
                liveToggle.setForeground(DarkTheme.AMBER);
                statusLabel.setText("  \u25CF   FEED PAUSED");
                statusLabel.setForeground(DarkTheme.AMBER);
            }
        });

        intervalSpinner.addChangeListener(e -> {
            int seconds = (Integer) intervalSpinner.getValue();
            marketData.setTickInterval(seconds * 1000);
        });

        viewBox.addActionListener(e -> refreshTable(null));

        symbolBox.addActionListener(e -> {
            String symbol = (String) symbolBox.getSelectedItem();
            if (symbol != null) {
                showSelected(symbol);
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshTable(null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshTable(null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshTable(null);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                String symbol = String.valueOf(tableModel.getValueAt(modelRow, COL_SYMBOL));
                showSelected(symbol);
            }
        });

        final JPopupMenu popup = new JPopupMenu();
        JMenuItem watchItem = new JMenuItem("Add to watchlist");
        JMenuItem copyItem = new JMenuItem("Copy symbol");
        JMenuItem chartItem = new JMenuItem("Open in Analytics");
        popup.add(watchItem);
        popup.add(copyItem);
        popup.addSeparator();
        popup.add(chartItem);

        watchItem.addActionListener(e -> addSelectedToWatchlist());
        copyItem.addActionListener(e -> showHint("Symbol copied: " + getSelectedSymbol()));
        chartItem.addActionListener(e -> showHint(
                "Analytics module would open for " + getSelectedSymbol() + "."));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    String symbol = getSelectedSymbol();
                    MarketRow row = marketData.getRow(symbol);
                    if (row != null) {
                        javax.swing.JOptionPane.showMessageDialog(MarketDataPanel.this,
                                "Instrument : " + symbol + " (" + row.getCompany() + ")\n"
                              + "Last price : " + String.format("%.2f", row.getPrice()) + "\n"
                              + "Bid / Ask  : " + String.format("%.2f", row.getBid())
                              + " / " + String.format("%.2f", row.getAsk()) + "\n"
                              + "Spread     : " + String.format("%.4f", row.getSpread()) + "\n"
                              + "Volume     : " + String.format("%,d", row.getVolume()) + "\n"
                              + "Change     : " + row.getChangeText()
                              + String.format("  (%.2f%%)", row.getChangePercent()) + "\n"
                              + "Updated    : " + timeFormat.format(new Date(row.getTimestamp())),
                                "Instrument Detail - " + symbol,
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        autoScrollCheck.setSelected(true);
        autoScrollCheck.setOpaque(false);
        autoScrollCheck.setForeground(DarkTheme.TEXT);
    }

    private void showHint(String message) {
        javax.swing.JOptionPane.showMessageDialog(this, message, "Market Data",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    private String getSelectedSymbol() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return lastSelectedSymbol;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return String.valueOf(tableModel.getValueAt(modelRow, COL_SYMBOL));
    }

    private void showSelected(String symbol) {
        lastSelectedSymbol = symbol;
        MarketRow row = marketData.getRow(symbol);
        if (row == null) {
            selectedLabel.setText("Selected: " + symbol);
            return;
        }
        String sign = row.getChange() >= 0 ? "+" : "";
        selectedLabel.setText(String.format("%s  %.2f  (%s%.2f)", symbol, row.getPrice(),
                sign, row.getChange()));
        selectedLabel.setForeground(row.getChange() >= 0 ? DarkTheme.GREEN : DarkTheme.RED);
    }

    private void addSelectedToWatchlist() {
        String symbol = getSelectedSymbol();
        if (symbol == null || symbol.isEmpty()) {
            return;
        }
        if (!watchlistModel.contains(symbol)) {
            watchlistModel.addElement(symbol);
        }
    }

    private void removeFromWatchlist() {
        String selected = watchlist.getSelectedValue();
        if (selected != null) {
            watchlistModel.removeElement(selected);
        }
    }

    private void refreshTable(List<MarketRow> rows) {
        List<MarketRow> source = rows == null ? marketData.getRows() : rows;
        List<MarketRow> filtered = applyFilter(source);

        int selectedViewRow = table.getSelectedRow();
        String keepSymbol = selectedViewRow < 0 ? lastSelectedSymbol : getSelectedSymbol();

        tableModel.setRowCount(0);
        for (MarketRow row : filtered) {
            tableModel.addRow(new Object[] {
                row.getSymbol(),
                row.getCompany(),
                String.format("%.2f", row.getPrice()),
                String.format("%.2f", row.getBid()),
                String.format("%.2f", row.getAsk()),
                String.format("%,d", row.getVolume()),
                row.getChangeText(),
                timeFormat.format(new Date(row.getTimestamp()))
            });
        }

        if (table.getColumnCount() >= 8) {
            DarkTheme.alignRight(table, COL_PRICE);
            DarkTheme.alignRight(table, COL_BID);
            DarkTheme.alignRight(table, COL_ASK);
            DarkTheme.alignRight(table, COL_VOLUME);
            DarkTheme.alignCenter(table, COL_CHANGE);
            DarkTheme.alignCenter(table, COL_TIME);
            DarkTheme.setColumnWidth(table, COL_SYMBOL, 70);
            DarkTheme.setColumnWidth(table, COL_COMPANY, 170);
            DarkTheme.setColumnWidth(table, COL_VOLUME, 100);
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (keepSymbol != null && keepSymbol.equals(tableModel.getValueAt(i, COL_SYMBOL))) {
                table.setRowSelectionInterval(i, i);
                break;
            }
        }

        symbolCountLabel.setText("Symbols: " + filtered.size());
        updateBreadth(source);
    }

    private List<MarketRow> applyFilter(List<MarketRow> source) {

        String query = DarkTheme.readField(searchField).trim().toLowerCase();
        String view = (String) viewBox.getSelectedItem();

        List<MarketRow> result = new ArrayList<>();
        for (MarketRow row : source) {
            if (!query.isEmpty()) {
                boolean matchesSymbol = row.getSymbol().toLowerCase().contains(query);
                boolean matchesCompany = row.getCompany().toLowerCase().contains(query);
                if (!matchesSymbol && !matchesCompany) {
                    continue;
                }
            }
            if ("Watchlist only".equals(view) && !watchlistModel.contains(row.getSymbol())) {
                continue;
            }
            result.add(row);
        }

        if ("Top gainers".equals(view) || "Top losers".equals(view)) {
            result.sort((a, b) -> "Top gainers".equals(view)
                    ? Double.compare(b.getChange(), a.getChange())
                    : Double.compare(a.getChange(), b.getChange()));
            if (result.size() > 10) {
                result = new ArrayList<>(result.subList(0, 10));
            }
        }
        return result;
    }

    private void updateBreadth(List<MarketRow> rows) {
        int up = 0;
        int down = 0;
        int flat = 0;
        for (MarketRow row : rows) {
            if (row.getChange() > 0) {
                up++;
            } else if (row.getChange() < 0) {
                down++;
            } else {
                flat++;
            }
        }
        upLabel.setText("  \u25CF   " + up);
        downLabel.setText("  \u25CF   " + down);
        flatLabel.setText("  \u25CF   " + flat);
    }

    @Override
    public void onMarketUpdate(List<MarketRow> rows) {
        updateCount++;
        rateLabel.setText("Updates: " + updateCount);
        lastUpdateLabel.setText("Last update: " + timeFormat.format(new Date()));
        if (autoScrollCheck.isSelected()) {
            refreshTable(rows);
            showSelected(lastSelectedSymbol);
        }
    }
}
