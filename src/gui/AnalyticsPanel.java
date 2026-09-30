package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;

import model.AnalyticsStats;
import model.AnalyticsStats.PricePoint;
import service.AnalyticsService;
import service.MarketDataService;
import util.DarkTheme;
import util.DemoData;

public class AnalyticsPanel extends JPanel implements MarketDataService.MarketListener {

    private final AnalyticsService analytics;
    private final MarketDataService marketData;

    private final JComboBox<String> symbolBox = new JComboBox<>();
    private final JComboBox<String> metricBox = new JComboBox<>(AnalyticsService.METRICS);
    private final JComboBox<String> resolutionBox = new JComboBox<>(
            new String[] { "Last 120 ticks", "Last 60 ticks", "Last 30 ticks" });

    private final JSlider smoothingSlider = new JSlider(1, 15, 1);
    private final JSpinner refreshSpinner = DarkTheme.spinner(1, 120, 1, 5);
    private final JCheckBox gridCheck = new JCheckBox("Grid", true);
    private final JCheckBox fillCheck = new JCheckBox("Fill area", true);
    private final JCheckBox averageCheck = new JCheckBox("Moving average", true);
    private final JToggleButton liveToggle = new JToggleButton("LIVE UPDATES", true);

    private final JButton refreshButton = DarkTheme.button("Recalculate");
    private final JButton exportButton = DarkTheme.outlineButton("Snapshot");

    private final JLabel chartTitle = DarkTheme.bold("Price chart");
    private final JLabel chartHint = DarkTheme.muted("Move the mouse over the chart to inspect values");
    private final JLabel selectedInstrumentLabel = DarkTheme.bold("AAPL - Apple Inc.");
    private final JLabel lastValueLabel = DarkTheme.statusLabel("--", DarkTheme.GREEN);

    private final PriceChart priceChart = new PriceChart();
    private final BarChart volumeChart = new BarChart();
    private final BarChart alertChart = new BarChart();

    private final DefaultTableModel summaryModel = new DefaultTableModel(
            new Object[] { "Metric", "Value", "Share" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable summaryTable = new JTable(summaryModel);

    private final JLabel kpiOrders = DarkTheme.bold("0");
    private final JLabel kpiExecuted = DarkTheme.bold("0");
    private final JLabel kpiCancelled = DarkTheme.bold("0");
    private final JLabel kpiAlerts = DarkTheme.bold("0");
    private final JLabel kpiLifetime = DarkTheme.bold("0 ms");
    private final JLabel kpiVwap = DarkTheme.bold("0.00");
    private final JLabel kpiVolume = DarkTheme.bold("0");
    private final JLabel kpiFlow = DarkTheme.bold("0");

    private final JProgressBar executionBar = new JProgressBar(0, 100);
    private final JProgressBar cancellationBar = new JProgressBar(0, 100);

    private long lastRefresh;
    private AnalyticsStats currentStats = new AnalyticsStats();

    public AnalyticsPanel() {
        this(new AnalyticsService(new MarketDataService(), new service.DataStore()));
    }

    public AnalyticsPanel(AnalyticsService analytics) {
        this.analytics = analytics;
        this.marketData = analytics.getMarketDataService();

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Charting & Analytics",
                "Order flow statistics and custom-painted charts for the selected instrument"),
                BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        updateSymbolBox();
        recalculate();

        marketData.addListener(this);
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(DarkTheme.BG);
        center.add(buildToolbar(), BorderLayout.NORTH);

        JPanel charts = new JPanel();
        charts.setLayout(new BoxLayout(charts, BoxLayout.Y_AXIS));
        charts.setBackground(DarkTheme.BG);

        charts.add(buildPriceChartCard());
        charts.add(Box.createVerticalStrut(10));
        charts.add(buildSecondaryCharts());
        charts.add(Box.createVerticalStrut(10));
        charts.add(buildControlsCard());

        JScrollPane scroll = new JScrollPane(charts);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DarkTheme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JPanel splitWrapper = new JPanel(new BorderLayout(10, 0));
        splitWrapper.setBackground(DarkTheme.BG);
        splitWrapper.add(scroll, BorderLayout.CENTER);
        splitWrapper.add(buildStatsSide(), BorderLayout.EAST);
        return splitWrapper;
    }

    private JPanel buildToolbar() {
        JPanel bar = DarkTheme.card(new FlowLayout(FlowLayout.LEFT, 8, 6));

        bar.add(DarkTheme.muted("Instrument"));
        symbolBox.setPreferredSize(new Dimension(200, DarkTheme.FIELD_HEIGHT));
        bar.add(symbolBox);

        bar.add(DarkTheme.muted("Metric"));
        metricBox.setPreferredSize(new Dimension(160, DarkTheme.FIELD_HEIGHT));
        bar.add(metricBox);

        bar.add(DarkTheme.muted("Window"));
        resolutionBox.setPreferredSize(new Dimension(150, DarkTheme.FIELD_HEIGHT));
        bar.add(resolutionBox);

        bar.add(refreshButton);
        bar.add(exportButton);

        liveToggle.setFont(DarkTheme.FONT_BOLD);
        liveToggle.setBackground(DarkTheme.SURFACE_ALT);
        liveToggle.setForeground(DarkTheme.TEXT);
        liveToggle.setFocusPainted(false);
        bar.add(liveToggle);

        return bar;
    }

    private JPanel buildPriceChartCard() {
        JPanel card = DarkTheme.card(new BorderLayout(0, 6));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(chartTitle, BorderLayout.WEST);

        JPanel rightTitle = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTitle.setOpaque(false);
        rightTitle.add(selectedInstrumentLabel);
        rightTitle.add(lastValueLabel);
        titleRow.add(rightTitle, BorderLayout.EAST);

        card.add(titleRow, BorderLayout.NORTH);
        priceChart.setPreferredSize(new Dimension(700, 260));
        priceChart.setMinimumSize(new Dimension(300, 200));
        card.add(priceChart, BorderLayout.CENTER);
        card.add(chartHint, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildSecondaryCharts() {
        JPanel row = new JPanel(new GridLayout(1, 2, 10, 0));
        row.setBackground(DarkTheme.BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(DarkTheme.FONT_BOLD);
        tabs.setBackground(DarkTheme.SURFACE);
        tabs.setForeground(DarkTheme.TEXT);

        volumeChart.setPreferredSize(new Dimension(320, 150));
        alertChart.setPreferredSize(new Dimension(320, 150));

        tabs.addTab("Volume", volumeChart);
        tabs.addTab("Alerts", alertChart);

        JPanel tabCard = DarkTheme.card(new BorderLayout());
        tabCard.add(tabs, BorderLayout.CENTER);

        JPanel orderFlowCard = DarkTheme.card("Order Flow", new BorderLayout(0, 8));
        orderFlowCard.add(buildKpiGrid(), BorderLayout.NORTH);
        orderFlowCard.add(buildFlowBars(), BorderLayout.CENTER);

        row.add(tabCard);
        row.add(orderFlowCard);
        return row;
    }

    private JPanel buildKpiGrid() {
        JPanel grid = new JPanel(new GridLayout(4, 4, 6, 6));
        grid.setOpaque(false);

        grid.add(DarkTheme.muted("Total Orders"));
        grid.add(kpiOrders);
        grid.add(DarkTheme.muted("Executed"));
        grid.add(kpiExecuted);
        grid.add(DarkTheme.muted("Cancelled"));
        grid.add(kpiCancelled);
        grid.add(DarkTheme.muted("Alerts"));
        grid.add(kpiAlerts);
        grid.add(DarkTheme.muted("Avg Lifetime"));
        grid.add(kpiLifetime);
        grid.add(DarkTheme.muted("VWAP"));
        grid.add(kpiVwap);
        grid.add(DarkTheme.muted("Traded Volume"));
        grid.add(kpiVolume);
        grid.add(DarkTheme.muted("Net Flow"));
        grid.add(kpiFlow);

        return grid;
    }

    private JPanel buildFlowBars() {
        JPanel wrapper = new JPanel(new GridLayout(4, 1, 0, 6));
        wrapper.setOpaque(false);

        executionBar.setStringPainted(true);
        executionBar.setForeground(DarkTheme.GREEN);
        executionBar.setBackground(DarkTheme.SURFACE_ALT);
        executionBar.setBorderPainted(false);

        cancellationBar.setStringPainted(true);
        cancellationBar.setForeground(DarkTheme.RED);
        cancellationBar.setBackground(DarkTheme.SURFACE_ALT);
        cancellationBar.setBorderPainted(false);

        wrapper.add(DarkTheme.muted("Execution rate"));
        wrapper.add(executionBar);
        wrapper.add(DarkTheme.muted("Cancellation rate"));
        wrapper.add(cancellationBar);
        return wrapper;
    }

    private JPanel buildControlsCard() {
        JPanel card = DarkTheme.card("Chart Controls", new FlowLayout(FlowLayout.LEFT, 12, 6));

        card.add(DarkTheme.muted("Smoothing"));
        smoothingSlider.setBackground(DarkTheme.SURFACE);
        smoothingSlider.setPreferredSize(new Dimension(180, DarkTheme.FIELD_HEIGHT));
        smoothingSlider.setMajorTickSpacing(5);
        smoothingSlider.setPaintTicks(true);
        smoothingSlider.setPaintLabels(true);
        card.add(smoothingSlider);

        card.add(DarkTheme.muted("Refresh every"));
        card.add(refreshSpinner);
        card.add(DarkTheme.muted("ticks"));

        for (JCheckBox box : new JCheckBox[] { gridCheck, fillCheck, averageCheck }) {
            box.setOpaque(false);
            box.setForeground(DarkTheme.TEXT);
            card.add(box);
        }

        return card;
    }

    private JPanel buildStatsSide() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(DarkTheme.BG);
        side.setPreferredSize(new Dimension(330, 100));

        JPanel summaryCard = DarkTheme.card("Statistics", new BorderLayout(0, 6));
        DarkTheme.styleTable(summaryTable);
        summaryTable.setRowHeight(24);
        JScrollPane scroll = DarkTheme.scroll(summaryTable);
        scroll.setPreferredSize(new Dimension(300, 240));
        summaryCard.add(scroll, BorderLayout.CENTER);
        side.add(summaryCard);
        side.add(Box.createVerticalStrut(10));

        JPanel infoCard = DarkTheme.card("Session Information", new GridLayout(0, 1, 0, 4));
        infoCard.add(DarkTheme.muted("Feed status : " + (marketData.isRunning() ? "RUNNING" : "STOPPED")));
        infoCard.add(DarkTheme.muted("Ticks so far : " + marketData.getTickCount()));
        infoCard.add(DarkTheme.muted("Alerts seen : " + analytics.getAlertsObserved()));
        infoCard.add(DarkTheme.muted("Instruments : " + marketData.getRows().size()));
        infoCard.add(DarkTheme.muted("Data source : simulated ITCH feed"));
        side.add(infoCard);

        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(DarkTheme.statusLabel("ANALYTICS ENGINE READY", DarkTheme.GREEN));
        left.add(DarkTheme.muted("|  All figures are simulated for the project review"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(DarkTheme.muted("Custom Swing rendering - no external chart library"));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        symbolBox.addActionListener(e -> recalculate());
        metricBox.addActionListener(e -> recalculate());
        resolutionBox.addActionListener(e -> recalculate());
        refreshButton.addActionListener(e -> recalculate());

        smoothingSlider.addChangeListener(e -> {
            priceChart.setSmoothing(smoothingSlider.getValue());
            priceChart.repaint();
        });

        gridCheck.addActionListener(e -> {
            priceChart.setShowGrid(gridCheck.isSelected());
            priceChart.repaint();
        });

        fillCheck.addActionListener(e -> {
            priceChart.setFillArea(fillCheck.isSelected());
            priceChart.repaint();
        });

        averageCheck.addActionListener(e -> {
            priceChart.setShowAverage(averageCheck.isSelected());
            priceChart.repaint();
        });

        liveToggle.addActionListener(e -> {
            liveToggle.setForeground(liveToggle.isSelected() ? DarkTheme.TEXT : DarkTheme.TEXT_MUTED);
            if (liveToggle.isSelected()) {
                recalculate();
            }
        });

        exportButton.addActionListener(e -> showSnapshot());
    }

    private void showSnapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append("ANALYTICS SNAPSHOT\n");
        sb.append("==================================================\n");
        sb.append("Instrument : ").append(currentStats.getSymbol()).append("\n");
        sb.append("Metric     : ").append(metricBox.getSelectedItem()).append("\n");
        sb.append("--------------------------------------------------\n");
        for (int i = 0; i < summaryModel.getRowCount(); i++) {
            sb.append(String.format("%-20s %s%n",
                    summaryModel.getValueAt(i, 0), summaryModel.getValueAt(i, 1)));
        }
        sb.append("--------------------------------------------------\n");
        sb.append("This snapshot is generated from simulated data.\n");

        javax.swing.JTextArea area = new javax.swing.JTextArea(sb.toString(), 18, 52);
        area.setFont(DarkTheme.FONT_MONO);
        area.setEditable(false);
        javax.swing.JOptionPane.showMessageDialog(this, DarkTheme.scroll(area),
                "Analytics snapshot (simulated export)", javax.swing.JOptionPane.PLAIN_MESSAGE);
    }

    private void updateSymbolBox() {
        String previous = (String) symbolBox.getSelectedItem();
        symbolBox.removeAllItems();
        symbolBox.addItem("ALL");
        for (String symbol : DemoData.symbols()) {
            symbolBox.addItem(symbol);
        }
        if (previous != null) {
            symbolBox.setSelectedItem(previous);
        } else {
            symbolBox.setSelectedItem("AAPL");
        }
    }

    private String selectedSymbol() {
        Object value = symbolBox.getSelectedItem();
        return value == null ? "AAPL" : value.toString();
    }

    public void refreshView() {
        recalculate();
    }

    private void recalculate() {
        String symbol = selectedSymbol();
        String metric = (String) metricBox.getSelectedItem();

        currentStats = analytics.buildStats(symbol);

        kpiOrders.setText(String.format("%,d", currentStats.getTotalOrders()));
        kpiExecuted.setText(String.format("%,d", currentStats.getExecutedOrders()));
        kpiCancelled.setText(String.format("%,d", currentStats.getCancelledOrders()));
        kpiAlerts.setText(String.format("%,d", currentStats.getAlerts()));
        kpiLifetime.setText(String.format("%.0f ms", currentStats.getAverageOrderLifetimeMs()));
        kpiVwap.setText(String.format("%.2f", currentStats.getVwap()));
        kpiVolume.setText(String.format("%,d", currentStats.getTradedVolume()));
        kpiFlow.setText(String.format("%,.0f", currentStats.getNetFlow()));

        executionBar.setValue(currentStats.getExecutionRatePercent());
        executionBar.setString("Executed " + currentStats.getExecutionRatePercent() + "%");
        cancellationBar.setValue(currentStats.getCancellationRatePercent());
        cancellationBar.setString("Cancelled " + currentStats.getCancellationRatePercent() + "%");

        double max = Math.max(1, currentStats.getTotalOrders());
        summaryModel.setRowCount(0);
        summaryModel.addRow(new Object[] { "Total Orders",
                String.format("%,d", currentStats.getTotalOrders()), "100%" });
        summaryModel.addRow(new Object[] { "Executed Orders",
                String.format("%,d", currentStats.getExecutedOrders()),
                String.format("%d%%", currentStats.getExecutionRatePercent()) });
        summaryModel.addRow(new Object[] { "Cancelled Orders",
                String.format("%,d", currentStats.getCancelledOrders()),
                String.format("%d%%", currentStats.getCancellationRatePercent()) });
        summaryModel.addRow(new Object[] { "Alerts",
                String.format("%,d", currentStats.getAlerts()),
                String.format("%d%%", (int) Math.round(currentStats.getAlerts() * 100.0 / max)) });
        summaryModel.addRow(new Object[] { "Avg Order Lifetime",
                String.format("%.0f ms", currentStats.getAverageOrderLifetimeMs()), "-" });
        summaryModel.addRow(new Object[] { "Max Order Size",
                String.format("%,d", currentStats.getMaxOrderSize()), "-" });
        summaryModel.addRow(new Object[] { "Traded Volume",
                String.format("%,d", currentStats.getTradedVolume()), "-" });
        summaryModel.addRow(new Object[] { "VWAP",
                String.format("%.2f", currentStats.getVwap()), "-" });
        DarkTheme.alignRight(summaryTable, 1);
        DarkTheme.alignCenter(summaryTable, 2);

        List<PricePoint> series = analytics.getSeries(symbol);
        priceChart.setSeries(series, metric);
        priceChart.setSmoothing(smoothingSlider.getValue());
        priceChart.setShowGrid(gridCheck.isSelected());
        priceChart.setFillArea(fillCheck.isSelected());
        priceChart.setShowAverage(averageCheck.isSelected());

        volumeChart.setBars(toVolumeBars(analytics.getVolumeSeries(symbol)), DarkTheme.ACCENT);
        volumeChart.setCaption("Volume traded per observation");

        Map<String, Integer> distribution = analytics.getAlertDistribution(symbol);
        alertChart.setBars(distribution, DarkTheme.AMBER);
        alertChart.setCaption("Alerts by detection type");

        selectedInstrumentLabel.setText(symbol + ("ALL".equals(symbol)
                ? " instruments" : "  " + DemoData.name(symbol)));
        chartTitle.setText(metric + " chart");

        double[] summary = analytics.getSeriesSummary(symbol);
        boolean up = summary[4] >= summary[3];
        lastValueLabel.setText(String.format("%.2f", summary[4]));
        lastValueLabel.setForeground(up ? DarkTheme.GREEN : DarkTheme.RED);

        chartHint.setText(String.format(
                "Min %.2f   Max %.2f   Avg %.2f   Change %+.2f   Observations %d",
                summary[0], summary[1], summary[2], summary[4] - summary[3], series.size()));

        lastRefresh = System.currentTimeMillis();
    }

    private Map<String, Integer> toVolumeBars(List<Long> volumes) {
        Map<String, Integer> bars = new LinkedHashMap<>();
        if (volumes.isEmpty()) {
            return bars;
        }
        long max = 1;
        for (Long value : volumes) {
            max = Math.max(max, value);
        }
        for (int i = 0; i < volumes.size(); i++) {
            bars.put(String.valueOf(i), (int) (volumes.get(i) * 100 / max));
        }
        return bars;
    }

    @Override
    public void onMarketUpdate(List<model.MarketRow> rows) {
        if (!liveToggle.isSelected()) {
            return;
        }

        int everyTicks = (Integer) refreshSpinner.getValue();
        if (marketData.getTickCount() % Math.max(1, everyTicks) != 0) {
            return;
        }
        recalculate();
    }

    private static class PriceChart extends JPanel {

        private List<PricePoint> series = new ArrayList<>();
        private String metric = "Price (Last)";
        private int smoothing = 1;
        private boolean showGrid = true;
        private boolean fillArea = true;
        private boolean showAverage = true;
        private int hoverIndex = -1;

        PriceChart() {
            setBackground(DarkTheme.SURFACE);
            setOpaque(true);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverIndex = indexAt(e.getX());
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverIndex = -1;
                    repaint();
                }
            };
            addMouseMotionListener(mouse);
            addMouseListener(mouse);
        }

        void setSeries(List<PricePoint> series, String metric) {
            this.series = series == null ? new ArrayList<>() : series;
            this.metric = metric;
            repaint();
        }

        void setSmoothing(int smoothing) {
            this.smoothing = Math.max(1, smoothing);
        }

        void setShowGrid(boolean showGrid) {
            this.showGrid = showGrid;
        }

        void setFillArea(boolean fillArea) {
            this.fillArea = fillArea;
        }

        void setShowAverage(boolean showAverage) {
            this.showAverage = showAverage;
        }

        private int indexAt(int x) {
            int left = 58;
            int right = getWidth() - 14;
            if (series.isEmpty() || x < left || x > right) {
                return -1;
            }
            double ratio = (double) (x - left) / Math.max(1, right - left);
            int index = (int) Math.round(ratio * (series.size() - 1));
            return Math.max(0, Math.min(series.size() - 1, index));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int left = 58;
            int right = getWidth() - 14;
            int top = 12;
            int bottom = getHeight() - 26;
            int width = right - left;
            int height = bottom - top;

            if (width <= 10 || height <= 10) {
                return;
            }

            if (series.size() < 2) {
                g2.setColor(DarkTheme.TEXT_MUTED);
                g2.setFont(DarkTheme.FONT_BASE);
                g2.drawString("Waiting for data...", left + 10, top + 20);
                return;
            }

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (PricePoint point : series) {
                min = Math.min(min, point.getPrice());
                max = Math.max(max, point.getPrice());
            }
            double padding = Math.max((max - min) * 0.1, 0.05);
            min -= padding;
            max += padding;
            double range = max - min;

            if (showGrid) {
                g2.setFont(DarkTheme.FONT_SMALL);
                for (int i = 0; i <= 5; i++) {
                    int y = top + height * i / 5;
                    g2.setColor(new Color(DarkTheme.BORDER.getRed(), DarkTheme.BORDER.getGreen(),
                            DarkTheme.BORDER.getBlue(), 130));
                    g2.drawLine(left, y, right, y);

                    double value = max - range * i / 5;
                    g2.setColor(DarkTheme.TEXT_MUTED);
                    g2.drawString(String.format("%.2f", value), 6, y + 4);
                }
                for (int i = 0; i <= 4; i++) {
                    int x = left + width * i / 4;
                    g2.setColor(new Color(DarkTheme.BORDER.getRed(), DarkTheme.BORDER.getGreen(),
                            DarkTheme.BORDER.getBlue(), 90));
                    g2.drawLine(x, top, x, bottom);

                    int index = (int) Math.round((double) i / 4 * (series.size() - 1));
                    g2.setColor(DarkTheme.TEXT_MUTED);
                    String label = util.DateTimeUtil.time(series.get(index).getTime());
                    int labelWidth = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, x - labelWidth / 2, bottom + 16);
                }
            }

            int[] xs = new int[series.size()];
            int[] ys = new int[series.size()];
            for (int i = 0; i < series.size(); i++) {
                xs[i] = left + (int) Math.round((double) width * i / (series.size() - 1));
                double value = series.get(i).getPrice();
                ys[i] = top + (int) Math.round(height * (max - value) / range);
            }

            boolean rising = series.get(series.size() - 1).getPrice() >= series.get(0).getPrice();
            Color lineColor = rising ? DarkTheme.GREEN : DarkTheme.RED;

            if (fillArea) {
                int[] fillX = new int[series.size() + 2];
                int[] fillY = new int[series.size() + 2];
                System.arraycopy(xs, 0, fillX, 0, xs.length);
                System.arraycopy(ys, 0, fillY, 0, ys.length);
                fillX[series.size()] = xs[xs.length - 1];
                fillY[series.size()] = bottom;
                fillX[series.size() + 1] = xs[0];
                fillY[series.size() + 1] = bottom;
                g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 45));
                g2.fillPolygon(fillX, fillY, fillX.length);
            }

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(xs, ys, xs.length);

            if (showAverage && smoothing > 1) {
                g2.setColor(DarkTheme.PURPLE);
                g2.setStroke(new BasicStroke(1.6f));
                int[] ax = new int[series.size()];
                int[] ay = new int[series.size()];
                for (int i = 0; i < series.size(); i++) {
                    ax[i] = xs[i];
                    double sum = 0;
                    int count = 0;
                    for (int j = Math.max(0, i - smoothing + 1); j <= i; j++) {
                        sum += series.get(j).getPrice();
                        count++;
                    }
                    double avg = sum / count;
                    ay[i] = top + (int) Math.round(height * (max - avg) / range);
                }
                g2.drawPolyline(ax, ay, ax.length);
            }

            int lastX = xs[xs.length - 1];
            int lastY = ys[ys.length - 1];
            g2.setColor(lineColor);
            g2.fillOval(lastX - 4, lastY - 4, 8, 8);

            if (hoverIndex >= 0 && hoverIndex < series.size()) {
                int hx = xs[hoverIndex];
                int hy = ys[hoverIndex];

                g2.setColor(new Color(DarkTheme.TEXT_MUTED.getRed(), DarkTheme.TEXT_MUTED.getGreen(),
                        DarkTheme.TEXT_MUTED.getBlue(), 120));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0, new float[] { 4f, 4f }, 0f));
                g2.drawLine(hx, top, hx, bottom);
                g2.drawLine(left, hy, right, hy);

                g2.setColor(DarkTheme.TEXT);
                g2.fillOval(hx - 5, hy - 5, 10, 10);
                g2.setColor(DarkTheme.SURFACE);
                g2.fillOval(hx - 2, hy - 2, 4, 4);

                PricePoint point = series.get(hoverIndex);
                String text = String.format("%s  %.2f", util.DateTimeUtil.time(point.getTime()),
                        point.getPrice());
                g2.setFont(DarkTheme.FONT_BOLD);
                int textWidth = g2.getFontMetrics().stringWidth(text);
                int boxX = Math.min(hx + 8, right - textWidth - 12);
                int boxY = Math.max(top + 2, hy - 26);
                g2.setColor(new Color(0, 0, 0, 190));
                g2.fillRoundRect(boxX, boxY, textWidth + 14, 22, 6, 6);
                g2.setColor(lineColor);
                g2.drawRoundRect(boxX, boxY, textWidth + 14, 22, 6, 6);
                g2.setColor(DarkTheme.TEXT);
                g2.drawString(text, boxX + 7, boxY + 15);
            }

            g2.setColor(DarkTheme.TEXT_MUTED);
            g2.setFont(DarkTheme.FONT_SMALL);
            g2.drawString(metric, left + 4, top + 12);
        }
    }

    private static class BarChart extends JPanel {

        private Map<String, Integer> bars = new LinkedHashMap<>();
        private Color barColor = DarkTheme.ACCENT;
        private String caption = "";

        private boolean numericKeys;

        BarChart() {
            setBackground(DarkTheme.SURFACE);
            setOpaque(true);
        }

        void setBars(Map<String, Integer> bars, Color color) {
            this.bars = bars == null ? new LinkedHashMap<>() : bars;
            this.barColor = color;
            this.numericKeys = !this.bars.isEmpty() && isNumeric(this.bars.keySet().iterator().next());
            repaint();
        }

        void setCaption(String caption) {
            this.caption = caption;
            repaint();
        }

        private static boolean isNumeric(String text) {
            if (text == null || text.isEmpty()) {
                return false;
            }
            for (int i = 0; i < text.length(); i++) {
                if (!Character.isDigit(text.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(DarkTheme.TEXT_MUTED);
            g2.setFont(DarkTheme.FONT_SMALL);
            g2.drawString(caption, 8, 14);

            if (bars.isEmpty()) {
                g2.drawString("No data yet", 8, 40);
                return;
            }

            int left = numericKeys ? 8 : 8;
            int right = getWidth() - 10;
            int top = 24;
            int bottom = getHeight() - 26;
            int width = right - left;
            int height = bottom - top;
            if (width <= 10 || height <= 10) {
                return;
            }

            int max = 1;
            for (Integer value : bars.values()) {
                max = Math.max(max, value == null ? 0 : value);
            }

            int count = bars.size();
            int slot = Math.max(2, width / count);
            int barWidth = Math.max(2, slot - (numericKeys ? 1 : 10));
            int i = 0;

            for (Map.Entry<String, Integer> entry : bars.entrySet()) {
                int value = entry.getValue() == null ? 0 : entry.getValue();
                int barHeight = (int) Math.round(height * (double) value / max);
                int x = left + i * slot + (slot - barWidth) / 2;
                int y = bottom - barHeight;

                g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 70));
                g2.fillRoundRect(x, top, barWidth, height, 4, 4);
                g2.setColor(barColor);
                g2.fillRoundRect(x, y, barWidth, Math.max(barHeight, 2), 4, 4);

                if (!numericKeys) {
                    g2.setColor(DarkTheme.TEXT_MUTED);
                    g2.setFont(DarkTheme.FONT_SMALL);
                    String label = entry.getKey();
                    if (label.length() > 9) {
                        label = label.substring(0, 9);
                    }
                    int labelWidth = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, x + barWidth / 2 - labelWidth / 2, getHeight() - 10);
                    g2.setColor(DarkTheme.TEXT);
                    g2.setFont(DarkTheme.FONT_BOLD);
                    String valueText = String.valueOf(value);
                    int valueWidth = g2.getFontMetrics().stringWidth(valueText);
                    if (barHeight > 16) {
                        g2.setColor(Color.WHITE);
                        g2.drawString(valueText, x + barWidth / 2 - valueWidth / 2, y + 13);
                    } else {
                        g2.drawString(valueText, x + barWidth / 2 - valueWidth / 2, y - 4);
                    }
                }
                i++;
            }

            if (numericKeys) {
                g2.setColor(DarkTheme.TEXT_MUTED);
                g2.setFont(DarkTheme.FONT_SMALL);
                g2.drawString("oldest", left, getHeight() - 10);
                String now = "newest";
                int textWidth = g2.getFontMetrics().stringWidth(now);
                g2.drawString(now, right - textWidth, getHeight() - 10);
                g2.drawString("max " + max, right - 70, 14);
            }
        }
    }
}
