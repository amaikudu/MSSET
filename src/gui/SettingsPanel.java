package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import service.MarketDataService;
import service.PredictorService;
import service.SurveillanceService;
import util.DarkTheme;
import util.DemoData;
import util.SettingsStore;

public class SettingsPanel extends JPanel {

    private static final String CARD_APPEARANCE = "Appearance";
    private static final String CARD_MARKET = "Market Data";
    private static final String CARD_ALERTS = "Alerts";
    private static final String CARD_NOTIFICATIONS = "Notifications";
    private static final String CARD_ADVANCED = "Advanced";
    private static final String CARD_PREDICTOR = "Predictor & ML";

    private final MarketDataService marketData;
    private final SurveillanceService surveillance;
    private final PredictorService predictor;

    private final JPanel cardHolder = new JPanel(new java.awt.CardLayout());
    private final JTree settingsTree;
    private final JLabel statusLabel = DarkTheme.statusLabel("SETTINGS READY", DarkTheme.GREEN);
    private final JLabel changedLabel = DarkTheme.muted("No unsaved changes");

    private final JRadioButton darkTheme = new JRadioButton("Dark terminal theme", true);
    private final JRadioButton lightTheme = new JRadioButton("Light theme");
    private final ButtonGroup themeGroup = new ButtonGroup();
    private final JSpinner fontSizeSpinner = DarkTheme.spinner(9, 22, 1, SettingsStore.fontSize);
    private final JSpinner rowHeightSpinner = DarkTheme.spinner(18, 44, 2, SettingsStore.tableRowHeight);
    private final JCheckBox gridLinesCheck = new JCheckBox("Show table grid lines", SettingsStore.showGridLines);
    private final JCheckBox stripeCheck = new JCheckBox("Alternate row colours", SettingsStore.alternateRowColors);
    private final JCheckBox monoCheck = new JCheckBox("Monospaced numbers", SettingsStore.useMonospacedNumbers);
    private final ColorSwatch accentSwatch = new ColorSwatch(SettingsStore.accentColor);
    private final ColorSwatch alertSwatch = new ColorSwatch(SettingsStore.alertColor);
    private final ColorSwatch criticalSwatch = new ColorSwatch(SettingsStore.criticalColor);
    private final PreviewPanel previewPanel = new PreviewPanel();

    private final JSpinner intervalSpinner = DarkTheme.spinner(1, 30, 1, SettingsStore.refreshIntervalMs / 1000);
    private final JComboBox<String> defaultSymbolBox = new JComboBox<>(DemoData.symbols());
    private final JToggleButton autoConnectToggle = new JToggleButton("Auto-connect on start",
            SettingsStore.autoConnect);
    private final JCheckBox autoRefreshCheck = new JCheckBox("Auto-refresh tables", true);
    private final JSpinner depthSpinner = DarkTheme.spinner(5, 50, 1, SettingsStore.orderBookDepth);
    private final JSpinner decimalsSpinner = DarkTheme.spinner(0, 6, 1, SettingsStore.priceDecimals);
    private final JCheckBox thousandsCheck = new JCheckBox("Use thousands separator",
            SettingsStore.thousandsSeparator);
    private final JLabel feedStatusLabel = DarkTheme.muted("Feed: --");

    private final JSlider riskSlider = new JSlider(1, 100, SettingsStore.riskThreshold);
    private final JLabel riskValueLabel = DarkTheme.bold(String.valueOf(SettingsStore.riskThreshold));
    private final JCheckBox lowCheck = new JCheckBox("LOW (0-39)", true);
    private final JCheckBox mediumCheck = new JCheckBox("MEDIUM (40-64)", true);
    private final JCheckBox highCheck = new JCheckBox("HIGH (65-84)", true);
    private final JCheckBox criticalCheck = new JCheckBox("CRITICAL (85-100)", true);
    private final JCheckBox soundCheck = new JCheckBox("Play sound on new alert", SettingsStore.alertSound);
    private final JCheckBox autoReviewCheck = new JCheckBox("Automatically mark LOW alerts as reviewed",
            SettingsStore.autoMarkReviewed);
    private final JSpinner queueSpinner = DarkTheme.spinner(50, 5000, 50, SettingsStore.maxAlertQueue);
    private final JComboBox<String> defaultSeverityBox = new JComboBox<>(
            new String[] { "All severities", "CRITICAL", "HIGH", "MEDIUM", "LOW" });

    private final JCheckBox desktopCheck = new JCheckBox("Desktop notifications",
            SettingsStore.desktopNotifications);
    private final JCheckBox emailCheck = new JCheckBox("Email notifications", SettingsStore.emailNotifications);
    private final JTextField emailField = DarkTheme.field(SettingsStore.notificationEmail, 22);
    private final JCheckBox popupCheck = new JCheckBox("Popup dialog for CRITICAL alerts",
            SettingsStore.popupOnCritical);
    private final JComboBox<String> frequencyBox = new JComboBox<>(new String[] {
        "Real time", "Every 5 minutes", "Every 15 minutes", "Hourly", "End of day"
    });
    private final JCheckBox summaryCheck = new JCheckBox("Attach daily summary to email", true);

    private final JSpinner retentionSpinner = DarkTheme.spinner(7, 3650, 30, SettingsStore.archiveRetentionDays);
    private final JComboBox<String> logLevelBox = new JComboBox<>(
            new String[] { "ERROR", "WARN", "INFO", "DEBUG" });
    private final JTextField exportPathField = DarkTheme.field(SettingsStore.exportPath, 26);
    private final JComboBox<String> databaseBox = new JComboBox<>(
            new String[] { "None (demo data only)", "Embedded H2", "PostgreSQL", "Oracle" });

    private final JComboBox<String> predictorAlgoBox = new JComboBox<>(model.PredictorParameters.ALGORITHMS);
    private final JSpinner predictorWindowSpinner = DarkTheme.spinner(10, 1000, 10, 60);
    private final JSpinner predictorAlphaSpinner = DarkTheme.doubleSpinner(0.01, 1.0, 0.05, 0.20);
    private final JSpinner predictorZScoreSpinner = DarkTheme.doubleSpinner(1.0, 5.0, 0.5, 2.5);
    private final JSpinner largeDatasetCapacitySpinner = DarkTheme.spinner(1000, 500000, 5000, 50000);
    private final JSpinner volumeWeightSpinner = DarkTheme.doubleSpinner(0.0, 1.0, 0.05, 0.35);
    private final JSpinner cancelWeightSpinner = DarkTheme.doubleSpinner(0.0, 1.0, 0.05, 0.40);
    private final JSpinner volatilityWeightSpinner = DarkTheme.doubleSpinner(0.0, 1.0, 0.05, 0.25);

    public SettingsPanel() {
        this(new MarketDataService(), null, null);
    }

    public SettingsPanel(MarketDataService marketData, SurveillanceService surveillance) {
        this(marketData, surveillance, null);
    }

    public SettingsPanel(MarketDataService marketData, SurveillanceService surveillance, PredictorService predictor) {
        this.marketData = marketData;
        this.surveillance = surveillance;
        this.predictor = predictor;

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Settings",
                "Application preferences - changes take effect immediately"), BorderLayout.NORTH);

        settingsTree = buildTree();
        add(buildSplit(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        loadCurrentValues();
        selectTreeCategory(CARD_APPEARANCE);
    }

    private JTree buildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Preferences");

        DefaultMutableTreeNode appearance = new DefaultMutableTreeNode(CARD_APPEARANCE);
        appearance.add(new DefaultMutableTreeNode("Theme"));
        appearance.add(new DefaultMutableTreeNode("Colours"));
        appearance.add(new DefaultMutableTreeNode("Fonts and tables"));

        DefaultMutableTreeNode market = new DefaultMutableTreeNode(CARD_MARKET);
        market.add(new DefaultMutableTreeNode("Feed"));
        market.add(new DefaultMutableTreeNode("Display"));

        DefaultMutableTreeNode alerts = new DefaultMutableTreeNode(CARD_ALERTS);
        alerts.add(new DefaultMutableTreeNode("Thresholds"));
        alerts.add(new DefaultMutableTreeNode("Severity filters"));

        DefaultMutableTreeNode notifications = new DefaultMutableTreeNode(CARD_NOTIFICATIONS);
        notifications.add(new DefaultMutableTreeNode("Channels"));
        notifications.add(new DefaultMutableTreeNode("Summary"));

        DefaultMutableTreeNode advanced = new DefaultMutableTreeNode(CARD_ADVANCED);
        advanced.add(new DefaultMutableTreeNode("Storage"));
        advanced.add(new DefaultMutableTreeNode("Logging"));
        advanced.add(new DefaultMutableTreeNode("About"));

        DefaultMutableTreeNode predictorNode = new DefaultMutableTreeNode(CARD_PREDICTOR);
        predictorNode.add(new DefaultMutableTreeNode("Hyperparameters"));
        predictorNode.add(new DefaultMutableTreeNode("Large Dataset Tuning"));

        root.add(appearance);
        root.add(market);
        root.add(alerts);
        root.add(notifications);
        root.add(advanced);
        root.add(predictorNode);

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setBackground(DarkTheme.SURFACE_ALT);
        tree.setForeground(DarkTheme.TEXT);
        tree.setFont(DarkTheme.FONT_BASE);
        tree.setRowHeight(24);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree t, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean focus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(
                        t, value, selected, expanded, leaf, row, focus);
                label.setFont(DarkTheme.FONT_BASE);
                if (!selected) {
                    label.setForeground(DarkTheme.TEXT);
                    label.setBackground(DarkTheme.SURFACE_ALT);
                }
                setBackgroundSelectionColor(DarkTheme.ACCENT);
                setBackgroundNonSelectionColor(DarkTheme.SURFACE_ALT);
                setTextSelectionColor(Color.WHITE);
                setTextNonSelectionColor(DarkTheme.TEXT);
                setBorderSelectionColor(DarkTheme.ACCENT);
                return label;
            }
        });

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        return tree;
    }

    private JSplitPane buildSplit() {

        return DarkTheme.split(buildTreeSide(), buildCardSide(), 230);
    }

    private JPanel buildTreeSide() {
        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBackground(DarkTheme.BG);
        side.setPreferredSize(new Dimension(230, 100));

        JPanel card = DarkTheme.card("Categories", new BorderLayout());
        JScrollPane scroll = DarkTheme.scroll(settingsTree);
        scroll.setPreferredSize(new Dimension(200, 300));
        card.add(scroll, BorderLayout.CENTER);
        side.add(card, BorderLayout.CENTER);
        side.add(DarkTheme.muted("<html>Select a category to edit its<br>preferences on the right.</html>"),
                BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildCardSide() {
        cardHolder.setBackground(DarkTheme.BG);
        cardHolder.add(buildAppearanceCard(), CARD_APPEARANCE);
        cardHolder.add(buildMarketCard(), CARD_MARKET);
        cardHolder.add(buildAlertsCard(), CARD_ALERTS);
        cardHolder.add(buildNotificationsCard(), CARD_NOTIFICATIONS);
        cardHolder.add(buildAdvancedCard(), CARD_ADVANCED);
        cardHolder.add(buildPredictorSettingsCard(), CARD_PREDICTOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(DarkTheme.BG);
        wrapper.add(cardHolder, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildPredictorSettingsCard() {
        JPanel card = DarkTheme.card("Predictor & Large Dataset Parameters", new BorderLayout(0, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0));
        row1.setOpaque(false);
        row1.add(titledChoice("Model algorithm", predictorAlgoBox));
        row1.add(titledChoice("Lookback window (ticks)", predictorWindowSpinner));
        form.add(row1);
        form.add(Box.createVerticalStrut(10));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 10, 0));
        row2.setOpaque(false);
        row2.add(titledChoice("Learning rate (alpha)", predictorAlphaSpinner));
        row2.add(titledChoice("Anomaly Z-Score cutoff", predictorZScoreSpinner));
        form.add(row2);
        form.add(Box.createVerticalStrut(10));

        JPanel row3 = new JPanel(new GridLayout(1, 2, 10, 0));
        row3.setOpaque(false);
        row3.add(titledChoice("Large dataset buffer capacity", largeDatasetCapacitySpinner));
        row3.add(titledChoice("Volume feature weight", volumeWeightSpinner));
        form.add(row3);
        form.add(Box.createVerticalStrut(10));

        JPanel row4 = new JPanel(new GridLayout(1, 2, 10, 0));
        row4.setOpaque(false);
        row4.add(titledChoice("Cancellation ratio weight", cancelWeightSpinner));
        row4.add(titledChoice("Volatility expansion weight", volatilityWeightSpinner));
        form.add(row4);

        card.add(form, BorderLayout.NORTH);

        JPanel infoCard = DarkTheme.card("Model Architecture Information", new GridLayout(0, 1, 0, 4));
        infoCard.add(DarkTheme.muted("Predictor engine: Ensemble Statistical & Dynamic Trend Scoring"));
        infoCard.add(DarkTheme.muted("Streaming buffer: High-capacity O(1) circular ring buffer"));
        infoCard.add(DarkTheme.muted("Evaluation metrics: Live Accuracy %, Precision, Recall, F1, MAE"));
        card.add(infoCard, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildAppearanceCard() {
        JPanel card = DarkTheme.card("Appearance", new BorderLayout(0, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        themeGroup.add(darkTheme);
        themeGroup.add(lightTheme);
        for (JRadioButton button : new JRadioButton[] { darkTheme, lightTheme }) {
            button.setOpaque(false);
            button.setForeground(DarkTheme.TEXT);
            button.setFont(DarkTheme.FONT_BASE);
        }

        JPanel themePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        themePanel.setOpaque(false);
        themePanel.add(titledChoice("Theme", darkTheme, lightTheme));
        themePanel.add(titledChoice("Font size", fontSizeSpinner));
        form.add(themePanel);
        form.add(Box.createVerticalStrut(10));

        JPanel sizePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        sizePanel.setOpaque(false);
        sizePanel.add(titledChoice("Table row height", rowHeightSpinner));
        sizePanel.add(titledChoice("Alert colour", alertSwatch));
        form.add(sizePanel);
        form.add(Box.createVerticalStrut(10));

        JPanel colourPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        colourPanel.setOpaque(false);
        colourPanel.add(titledChoice("Accent colour", accentSwatch));
        colourPanel.add(titledChoice("Critical colour", criticalSwatch));
        form.add(colourPanel);
        form.add(Box.createVerticalStrut(10));

        JPanel options = new JPanel(new GridLayout(1, 3, 10, 0));
        options.setOpaque(false);
        for (JCheckBox box : new JCheckBox[] { gridLinesCheck, stripeCheck, monoCheck }) {
            box.setOpaque(false);
            box.setForeground(DarkTheme.TEXT);
            options.add(box);
        }
        form.add(options);

        card.add(form, BorderLayout.NORTH);

        JPanel previewCard = DarkTheme.card("Live preview", new BorderLayout());
        previewPanel.setPreferredSize(new Dimension(400, 150));
        previewCard.add(previewPanel, BorderLayout.CENTER);
        card.add(previewCard, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildMarketCard() {
        JPanel card = DarkTheme.card("Market Data", new BorderLayout(0, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0));
        row1.setOpaque(false);
        row1.add(titledChoice("Refresh interval (seconds)", intervalSpinner));
        row1.add(titledChoice("Default symbol", defaultSymbolBox));
        form.add(row1);
        form.add(Box.createVerticalStrut(10));

        JPanel row2 = new JPanel(new GridLayout(1, 3, 10, 0));
        row2.setOpaque(false);
        row2.add(titledChoice("Order book depth (levels)", depthSpinner));
        row2.add(titledChoice("Price decimals", decimalsSpinner));
        row2.add(titledChoice("Thousands separator", thousandsCheck));
        form.add(row2);
        form.add(Box.createVerticalStrut(10));

        JPanel row3 = new JPanel(new GridLayout(1, 1, 10, 0));
        row3.setOpaque(false);
        row3.add(titledChoice("Feed behaviour", autoConnectToggle, autoRefreshCheck));
        form.add(row3);

        card.add(form, BorderLayout.NORTH);

        JPanel statusCard = DarkTheme.card("Feed status", new GridLayout(0, 1, 0, 4));
        statusCard.add(feedStatusLabel);
        statusCard.add(DarkTheme.muted("Changing the refresh interval applies to the live feed immediately."));
        statusCard.add(DarkTheme.muted("The feed can also be paused from the Market Data module."));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        buttons.setOpaque(false);
        JButton startFeed = DarkTheme.button("Start feed");
        JButton stopFeed = DarkTheme.outlineButton("Stop feed");
        startFeed.addActionListener(e -> {
            marketData.start();
            updateFeedStatus();
            setStatus("Market feed started", DarkTheme.GREEN);
        });
        stopFeed.addActionListener(e -> {
            marketData.stop();
            updateFeedStatus();
            setStatus("Market feed stopped", DarkTheme.AMBER);
        });
        buttons.add(startFeed);
        buttons.add(stopFeed);
        statusCard.add(buttons);

        card.add(statusCard, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildAlertsCard() {
        JPanel card = DarkTheme.card("Alerts", new BorderLayout(0, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JPanel thresholdRow = new JPanel(new BorderLayout(10, 0));
        thresholdRow.setOpaque(false);
        JLabel caption = DarkTheme.muted("Risk threshold (alerts below this score are ignored)");
        thresholdRow.add(caption, BorderLayout.NORTH);

        JPanel sliderRow = new JPanel(new BorderLayout(10, 0));
        sliderRow.setOpaque(false);
        riskSlider.setBackground(DarkTheme.SURFACE);
        riskSlider.setMajorTickSpacing(20);
        riskSlider.setMinorTickSpacing(5);
        riskSlider.setPaintTicks(true);
        riskSlider.setPaintLabels(true);
        sliderRow.add(riskSlider, BorderLayout.CENTER);
        riskValueLabel.setPreferredSize(new Dimension(40, 24));
        sliderRow.add(riskValueLabel, BorderLayout.EAST);
        thresholdRow.add(sliderRow, BorderLayout.CENTER);
        form.add(thresholdRow);
        form.add(Box.createVerticalStrut(10));

        JPanel severityRow = new JPanel(new GridLayout(1, 4, 10, 0));
        severityRow.setOpaque(false);
        for (JCheckBox box : new JCheckBox[] { lowCheck, mediumCheck, highCheck, criticalCheck }) {
            box.setOpaque(false);
            box.setForeground(DarkTheme.TEXT);
            severityRow.add(box);
        }
        form.add(titledPanel("Severity filters", severityRow));
        form.add(Box.createVerticalStrut(10));

        JPanel row2 = new JPanel(new GridLayout(1, 3, 10, 0));
        row2.setOpaque(false);
        row2.add(titledChoice("Default severity filter", defaultSeverityBox));
        row2.add(titledChoice("Maximum queue size", queueSpinner));
        row2.add(titledChoice("Behaviour", soundCheck, autoReviewCheck));
        form.add(row2);

        card.add(form, BorderLayout.NORTH);

        JPanel infoCard = DarkTheme.card("Surveillance engine", new GridLayout(0, 1, 0, 4));
        infoCard.add(DarkTheme.muted("The threshold and severity filters are shared with the"));
        infoCard.add(DarkTheme.muted("Surveillance and Alerts modules."));
        JButton testSound = DarkTheme.outlineButton("Test alert sound");
        testSound.addActionListener(e -> {
            Toolkit.getDefaultToolkit().beep();
            setStatus("Test alert sound played", DarkTheme.GREEN);
        });
        infoCard.add(testSound);
        card.add(infoCard, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildNotificationsCard() {
        JPanel card = DarkTheme.card("Notifications", new BorderLayout(0, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0));
        row1.setOpaque(false);
        row1.add(titledChoice("Channels", desktopCheck, emailCheck));
        row1.add(titledChoice("Recipient email", emailField));
        form.add(row1);
        form.add(Box.createVerticalStrut(10));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 10, 0));
        row2.setOpaque(false);
        row2.add(titledChoice("Critical alerts", popupCheck, summaryCheck));
        row2.add(titledChoice("Summary frequency", frequencyBox));
        form.add(row2);

        card.add(form, BorderLayout.NORTH);

        JPanel testCard = DarkTheme.card("Test", new BorderLayout(0, 6));
        testCard.add(DarkTheme.muted("Send a simulated notification to check the configuration."),
                BorderLayout.NORTH);
        JButton testButton = DarkTheme.button("Send test notification");
        testButton.addActionListener(e -> sendTestNotification());
        testCard.add(testButton, BorderLayout.CENTER);
        card.add(testCard, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildAdvancedCard() {
        JPanel card = DarkTheme.card("Advanced", new BorderLayout(0, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0));
        row1.setOpaque(false);
        row1.add(titledChoice("Archive retention (days)", retentionSpinner));
        row1.add(titledChoice("Log level", logLevelBox));
        form.add(row1);
        form.add(Box.createVerticalStrut(10));

        JPanel exportRow = new JPanel(new BorderLayout(8, 0));
        exportRow.setOpaque(false);
        exportRow.add(DarkTheme.muted("Default export folder"), BorderLayout.NORTH);

        JPanel exportInner = new JPanel(new BorderLayout(6, 0));
        exportInner.setOpaque(false);
        exportInner.add(exportPathField, BorderLayout.CENTER);
        JButton browseButton = DarkTheme.outlineButton("Browse...");
        browseButton.addActionListener(e -> chooseExportFolder());
        exportInner.add(browseButton, BorderLayout.EAST);
        exportRow.add(exportInner, BorderLayout.CENTER);
        form.add(exportRow);
        form.add(Box.createVerticalStrut(10));

        JPanel row3 = new JPanel(new GridLayout(1, 2, 10, 0));
        row3.setOpaque(false);
        row3.add(titledChoice("Database backend", databaseBox));
        row3.add(titledChoice("Storage", retentionSpinner));
        form.add(row3);

        card.add(form, BorderLayout.NORTH);

        JPanel infoCard = DarkTheme.card("Danger zone and information", new BorderLayout(0, 8));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setOpaque(false);

        JButton resetDemoButton = DarkTheme.button("Restore demo data", DarkTheme.AMBER);
        resetDemoButton.addActionListener(e -> restoreDemoData());

        JButton aboutButton = DarkTheme.outlineButton("About this build");
        aboutButton.addActionListener(e -> showAbout());

        buttonRow.add(resetDemoButton);
        buttonRow.add(aboutButton);

        infoCard.add(buttonRow, BorderLayout.NORTH);
        infoCard.add(DarkTheme.muted("Database support is planned but not used in the review build."),
                BorderLayout.CENTER);
        card.add(infoCard, BorderLayout.CENTER);

        return card;
    }

    private JPanel titledChoice(String caption, Component... components) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setOpaque(false);
        wrapper.add(DarkTheme.muted(caption), BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        for (Component component : components) {
            row.add(component);
        }
        wrapper.add(row, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel titledPanel(String caption, Component content) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.setOpaque(false);
        wrapper.add(DarkTheme.muted(caption), BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(changedLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton applyButton = DarkTheme.button("Apply");
        JButton restoreButton = DarkTheme.outlineButton("Restore defaults");
        JButton cancelButton = DarkTheme.outlineButton("Cancel changes");

        applyButton.addActionListener(e -> applySettings());
        restoreButton.addActionListener(e -> restoreDefaults());
        cancelButton.addActionListener(e -> {
            loadCurrentValues();
            setStatus("Changes discarded", DarkTheme.TEXT_MUTED);
        });

        right.add(applyButton);
        right.add(restoreButton);
        right.add(cancelButton);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {

        settingsTree.addTreeSelectionListener((TreeSelectionEvent e) -> {
            TreePath path = settingsTree.getSelectionPath();
            if (path == null) {
                return;
            }
            Object selected = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            String name = String.valueOf(selected);
            if (isKnownCategory(name)) {
                selectTreeCategory(name);
            }
        });

        ActionListener markChanged = e -> {
            changedLabel.setText("Unsaved changes - press Apply");
            changedLabel.setForeground(DarkTheme.AMBER);
        };

        darkTheme.addActionListener(markChanged);
        lightTheme.addActionListener(markChanged);
        fontSizeSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        rowHeightSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        gridLinesCheck.addActionListener(markChanged);
        stripeCheck.addActionListener(markChanged);
        monoCheck.addActionListener(markChanged);

        accentSwatch.onClick(() -> chooseColour("Accent colour", accentSwatch));
        alertSwatch.onClick(() -> chooseColour("Alert colour", alertSwatch));
        criticalSwatch.onClick(() -> chooseColour("Critical colour", criticalSwatch));

        intervalSpinner.addChangeListener(e -> {
            int seconds = (Integer) intervalSpinner.getValue();
            marketData.setTickInterval(seconds * 1000);
            updateFeedStatus();
            markChanged.actionPerformed(null);
        });

        defaultSymbolBox.addActionListener(e -> {
            SettingsStore.defaultSymbol = String.valueOf(defaultSymbolBox.getSelectedItem());
            markChanged.actionPerformed(null);
        });

        autoConnectToggle.addActionListener(markChanged);
        autoRefreshCheck.addActionListener(markChanged);
        depthSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        decimalsSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        thousandsCheck.addActionListener(markChanged);

        riskSlider.addChangeListener(e -> {
            int value = riskSlider.getValue();
            riskValueLabel.setText(String.valueOf(value));
            if (surveillance != null) {
                surveillance.setRiskThreshold(value);
            }
            markChanged.actionPerformed(null);
        });

        for (JCheckBox box : new JCheckBox[] { lowCheck, mediumCheck, highCheck, criticalCheck }) {
            box.addActionListener(markChanged);
        }
        soundCheck.addActionListener(e -> {
            if (soundCheck.isSelected()) {
                Toolkit.getDefaultToolkit().beep();
            }
            markChanged.actionPerformed(null);
        });
        autoReviewCheck.addActionListener(markChanged);
        queueSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        defaultSeverityBox.addActionListener(markChanged);

        desktopCheck.addActionListener(markChanged);
        emailCheck.addActionListener(e -> {
            emailField.setEnabled(emailCheck.isSelected());
            markChanged.actionPerformed(null);
        });
        popupCheck.addActionListener(markChanged);
        summaryCheck.addActionListener(markChanged);
        frequencyBox.addActionListener(markChanged);

        retentionSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        logLevelBox.addActionListener(markChanged);
        databaseBox.addActionListener(markChanged);

        predictorAlgoBox.addActionListener(markChanged);
        predictorWindowSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        predictorAlphaSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        predictorZScoreSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        largeDatasetCapacitySpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        volumeWeightSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        cancelWeightSpinner.addChangeListener(e -> markChanged.actionPerformed(null));
        volatilityWeightSpinner.addChangeListener(e -> markChanged.actionPerformed(null));

        accentSwatch.onClick(() -> previewPanel.repaint());
        alertSwatch.onClick(() -> previewPanel.repaint());

        previewPanel.repaint();
    }

    private boolean isKnownCategory(String name) {
        return CARD_APPEARANCE.equals(name)
                || CARD_MARKET.equals(name)
                || CARD_ALERTS.equals(name)
                || CARD_NOTIFICATIONS.equals(name)
                || CARD_ADVANCED.equals(name)
                || CARD_PREDICTOR.equals(name);
    }

    private void selectTreeCategory(String category) {
        ((java.awt.CardLayout) cardHolder.getLayout()).show(cardHolder, category);

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) settingsTree.getModel().getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            if (category.equals(String.valueOf(child.getUserObject()))) {
                settingsTree.setSelectionPath(new TreePath(child.getPath()));
                break;
            }
        }
        if (CARD_MARKET.equals(category)) {
            updateFeedStatus();
        }
        changedLabel.setText("Editing: " + category);
        changedLabel.setForeground(DarkTheme.TEXT_MUTED);
    }

    private void chooseColour(String title, ColorSwatch swatch) {
        Color chosen = JColorChooser.showDialog(this, title, swatch.getColor());
        if (chosen != null) {
            swatch.setColor(chosen);
            previewPanel.repaint();
            changedLabel.setText("Unsaved changes - press Apply");
            changedLabel.setForeground(DarkTheme.AMBER);
        }
    }

    private void chooseExportFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the default export folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            exportPathField.setText(chooser.getSelectedFile().getAbsolutePath());
            changedLabel.setText("Unsaved changes - press Apply");
            changedLabel.setForeground(DarkTheme.AMBER);
        }
    }

    private void applySettings() {
        SettingsStore.theme = darkTheme.isSelected() ? "Dark" : "Light";
        SettingsStore.fontSize = (Integer) fontSizeSpinner.getValue();
        SettingsStore.tableRowHeight = (Integer) rowHeightSpinner.getValue();
        SettingsStore.accentColor = accentSwatch.getColor();
        SettingsStore.alertColor = alertSwatch.getColor();
        SettingsStore.criticalColor = criticalSwatch.getColor();
        SettingsStore.showGridLines = gridLinesCheck.isSelected();
        SettingsStore.alternateRowColors = stripeCheck.isSelected();
        SettingsStore.useMonospacedNumbers = monoCheck.isSelected();

        SettingsStore.refreshIntervalMs = (Integer) intervalSpinner.getValue() * 1000;
        SettingsStore.defaultSymbol = String.valueOf(defaultSymbolBox.getSelectedItem());
        SettingsStore.autoConnect = autoConnectToggle.isSelected();
        SettingsStore.orderBookDepth = (Integer) depthSpinner.getValue();
        SettingsStore.priceDecimals = (Integer) decimalsSpinner.getValue();
        SettingsStore.thousandsSeparator = thousandsCheck.isSelected();

        SettingsStore.riskThreshold = riskSlider.getValue();
        SettingsStore.alertSound = soundCheck.isSelected();
        SettingsStore.autoMarkReviewed = autoReviewCheck.isSelected();
        SettingsStore.showLowSeverity = lowCheck.isSelected();
        SettingsStore.maxAlertQueue = (Integer) queueSpinner.getValue();

        SettingsStore.desktopNotifications = desktopCheck.isSelected();
        SettingsStore.emailNotifications = emailCheck.isSelected();
        SettingsStore.notificationEmail = emailField.getText();
        SettingsStore.popupOnCritical = popupCheck.isSelected();
        SettingsStore.summaryFrequency = String.valueOf(frequencyBox.getSelectedItem());

        SettingsStore.archiveRetentionDays = (Integer) retentionSpinner.getValue();
        SettingsStore.logLevel = String.valueOf(logLevelBox.getSelectedItem());
        SettingsStore.exportPath = exportPathField.getText();

        marketData.setTickInterval(SettingsStore.refreshIntervalMs);
        if (surveillance != null) {
            surveillance.setRiskThreshold(SettingsStore.riskThreshold);
        }

        if (predictor != null) {
            predictor.getParameters().setAlgorithm(String.valueOf(predictorAlgoBox.getSelectedItem()));
            predictor.getParameters().setLookbackWindow((Integer) predictorWindowSpinner.getValue());
            predictor.getParameters().setLearningRateAlpha(((Number) predictorAlphaSpinner.getValue()).doubleValue());
            predictor.getParameters().setAnomalyZScoreCutoff(((Number) predictorZScoreSpinner.getValue()).doubleValue());
            predictor.getParameters().setBenchmarkDatasetSize((Integer) largeDatasetCapacitySpinner.getValue());
            predictor.getParameters().setVolumeWeight(((Number) volumeWeightSpinner.getValue()).doubleValue());
            predictor.getParameters().setCancellationWeight(((Number) cancelWeightSpinner.getValue()).doubleValue());
            predictor.getParameters().setVolatilityWeight(((Number) volatilityWeightSpinner.getValue()).doubleValue());
        }

        if ("Light".equals(SettingsStore.theme)) {
            setStatus("Settings applied (light theme selected)", DarkTheme.AMBER);
            JOptionPane.showMessageDialog(this,
                    "Preferences applied.\n\n"
                  + "The dark terminal theme is the build used for the review, so the\n"
                  + "light theme choice is recorded but not applied to the open windows.\n"
                  + "The refresh interval and risk threshold have been applied to the\n"
                  + "live market feed and the surveillance engine.",
                    "Settings applied", JOptionPane.INFORMATION_MESSAGE);
        } else {
            setStatus("Settings applied successfully", DarkTheme.GREEN);
        }

        changedLabel.setText("All changes saved");
        changedLabel.setForeground(DarkTheme.GREEN);
        updateFeedStatus();
        previewPanel.repaint();
    }

    private void loadCurrentValues() {
        darkTheme.setSelected("Dark".equals(SettingsStore.theme));
        lightTheme.setSelected(!"Dark".equals(SettingsStore.theme));
        fontSizeSpinner.setValue(SettingsStore.fontSize);
        rowHeightSpinner.setValue(SettingsStore.tableRowHeight);
        gridLinesCheck.setSelected(SettingsStore.showGridLines);
        stripeCheck.setSelected(SettingsStore.alternateRowColors);
        monoCheck.setSelected(SettingsStore.useMonospacedNumbers);
        accentSwatch.setColor(SettingsStore.accentColor);
        alertSwatch.setColor(SettingsStore.alertColor);
        criticalSwatch.setColor(SettingsStore.criticalColor);

        intervalSpinner.setValue(Math.max(1, SettingsStore.refreshIntervalMs / 1000));
        defaultSymbolBox.setSelectedItem(SettingsStore.defaultSymbol);
        autoConnectToggle.setSelected(SettingsStore.autoConnect);
        depthSpinner.setValue(SettingsStore.orderBookDepth);
        decimalsSpinner.setValue(SettingsStore.priceDecimals);
        thousandsCheck.setSelected(SettingsStore.thousandsSeparator);

        riskSlider.setValue(SettingsStore.riskThreshold);
        riskValueLabel.setText(String.valueOf(SettingsStore.riskThreshold));
        soundCheck.setSelected(SettingsStore.alertSound);
        autoReviewCheck.setSelected(SettingsStore.autoMarkReviewed);
        queueSpinner.setValue(SettingsStore.maxAlertQueue);

        desktopCheck.setSelected(SettingsStore.desktopNotifications);
        emailCheck.setSelected(SettingsStore.emailNotifications);
        emailField.setText(SettingsStore.notificationEmail);
        emailField.setEnabled(SettingsStore.emailNotifications);
        popupCheck.setSelected(SettingsStore.popupOnCritical);
        frequencyBox.setSelectedItem(SettingsStore.summaryFrequency);

        retentionSpinner.setValue(SettingsStore.archiveRetentionDays);
        logLevelBox.setSelectedItem(SettingsStore.logLevel);
        exportPathField.setText(SettingsStore.exportPath);

        if (predictor != null) {
            predictorAlgoBox.setSelectedItem(predictor.getParameters().getAlgorithm());
            predictorWindowSpinner.setValue(predictor.getParameters().getLookbackWindow());
            predictorAlphaSpinner.setValue(predictor.getParameters().getLearningRateAlpha());
            predictorZScoreSpinner.setValue(predictor.getParameters().getAnomalyZScoreCutoff());
            largeDatasetCapacitySpinner.setValue(predictor.getParameters().getBenchmarkDatasetSize());
            volumeWeightSpinner.setValue(predictor.getParameters().getVolumeWeight());
            cancelWeightSpinner.setValue(predictor.getParameters().getCancellationWeight());
            volatilityWeightSpinner.setValue(predictor.getParameters().getVolatilityWeight());
        }

        updateFeedStatus();
        previewPanel.repaint();
    }

    private void restoreDefaults() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Restore every preference to its default value?",
                "Restore defaults", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        SettingsStore.restoreDefaults();
        loadCurrentValues();
        marketData.setTickInterval(SettingsStore.refreshIntervalMs);
        setStatus("Defaults restored", DarkTheme.ACCENT);
        changedLabel.setText("Defaults restored - press Apply to keep them");
        changedLabel.setForeground(DarkTheme.AMBER);
    }

    private void restoreDemoData() {
        int choice = JOptionPane.showConfirmDialog(this,
                "This clears all alerts and reports and recreates the demo data.\n\n"
              + "Continue?",
                "Restore demo data", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        if (surveillance == null) {
            setStatus("Demo data service not available", DarkTheme.RED);
            return;
        }

        setStatus("Demo data restore is handled by the data service", DarkTheme.ACCENT);
        JOptionPane.showMessageDialog(this,
                "Demo data will be recreated the next time the application starts.\n\n"
              + "To clear the alert queue immediately, use \"Remove closed alerts\"\n"
              + "in the Alerts module.",
                "Restore demo data", JOptionPane.INFORMATION_MESSAGE);
    }

    private void sendTestNotification() {
        if (!desktopCheck.isSelected() && !emailCheck.isSelected()) {
            JOptionPane.showMessageDialog(this,
                    "Enable at least one notification channel first.",
                    "Notifications", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append("Test notification (simulated)\n\n");
        if (desktopCheck.isSelected()) {
            message.append("Desktop notification : sent\n");
        }
        if (emailCheck.isSelected()) {
            message.append("Email notification   : sent to ").append(emailField.getText()).append("\n");
        }
        message.append("Summary frequency    : ").append(frequencyBox.getSelectedItem()).append("\n");
        message.append("Popup on critical    : ").append(popupCheck.isSelected() ? "yes" : "no").append("\n\n");
        message.append("Real delivery is not implemented in the review build.");

        JOptionPane.showMessageDialog(this, message.toString(),
                "Test notification", JOptionPane.INFORMATION_MESSAGE);
        setStatus("Test notification sent (simulated)", DarkTheme.GREEN);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "MARKET SURVEILLANCE ENGINE FOR ELECTRONIC TRADING\n"
              + "--------------------------------------------------\n"
              + "Build        : First review (Swing GUI demo)\n"
              + "Java         : " + System.getProperty("java.version") + "\n"
              + "Look and feel: " + javax.swing.UIManager.getLookAndFeel().getName() + "\n"
              + "Modules      : Dashboard, Market Data, Order Book, Surveillance,\n"
              + "               Rule Builder, Alerts, Charting & Analytics,\n"
              + "               Audit Reports, Replay, Settings\n"
              + "--------------------------------------------------\n"
              + "Data in this build is simulated. No database, no external\n"
              + "libraries: standard Java Swing only.",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateFeedStatus() {
        int seconds = Math.max(1, marketData.getTickInterval() / 1000);
        String state = marketData.isRunning() ? "RUNNING" : "STOPPED";
        feedStatusLabel.setText(String.format("Feed: %s  |  interval %d s  |  ticks %,d",
                state, seconds, marketData.getTickCount()));
        feedStatusLabel.setForeground(marketData.isRunning() ? DarkTheme.GREEN : DarkTheme.AMBER);
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText("  \u25CF   " + text.toUpperCase());
        statusLabel.setForeground(color);
    }

    private static class ColorSwatch extends JPanel {

        private Color color;
        private Runnable onClick;

        ColorSwatch(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(74, 26));
            setBackground(color);
            setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER, 1));
            setToolTipText("Click to choose a colour");
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onClick != null) {
                        onClick.run();
                    }
                }
            });
        }

        void onClick(Runnable action) {
            this.onClick = action;
        }

        Color getColor() {
            return color;
        }

        void setColor(Color color) {
            this.color = color;
            setBackground(color);
            setToolTipText("Colour " + DarkTheme.toHex(color) + " - click to change");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 6, 6);
            g2.setColor(DarkTheme.BORDER);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 6, 6);
        }
    }

    private class PreviewPanel extends JPanel {

        PreviewPanel() {
            setBackground(DarkTheme.SURFACE);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int rowHeight = (Integer) rowHeightSpinner.getValue();
            int fontSize = (Integer) fontSizeSpinner.getValue();
            Font font = new Font("SansSerif", Font.PLAIN, Math.max(9, fontSize - 1));
            Font bold = new Font("SansSerif", Font.BOLD, Math.max(9, fontSize - 1));
            g2.setFont(font);

            g2.setColor(DarkTheme.SURFACE_ALT);
            g2.fillRect(0, 0, getWidth(), 26);
            g2.setColor(accentSwatch.getColor());
            g2.fillRect(0, 25, getWidth(), 2);
            g2.setColor(DarkTheme.TEXT_MUTED);
            g2.setFont(bold);
            g2.drawString("ALERT ID", 10, 18);
            g2.drawString("SYMBOL", 110, 18);
            g2.drawString("RISK", 190, 18);
            g2.drawString("STATUS", 250, 18);

            String[][] rows = {
                { "ALR-0042", "AAPL", "92", "NEW" },
                { "ALR-0041", "NVDA", "71", "UNDER REVIEW" },
                { "ALR-0040", "TSLA", "45", "CLOSED" },
                { "ALR-0039", "MSFT", "88", "NEW" }
            };

            g2.setFont(font);
            for (int i = 0; i < rows.length; i++) {
                int y = 26 + i * rowHeight;
                if (y > getHeight() - 6) {
                    break;
                }
                if (stripeCheck.isSelected() && i % 2 == 1) {
                    g2.setColor(DarkTheme.SURFACE_ALT);
                    g2.fillRect(0, y, getWidth(), rowHeight);
                }
                if (gridLinesCheck.isSelected()) {
                    g2.setColor(DarkTheme.BORDER);
                    g2.drawLine(0, y + rowHeight - 1, getWidth(), y + rowHeight - 1);
                }

                int baseline = y + rowHeight - 8;
                g2.setColor(DarkTheme.TEXT);
                g2.drawString(rows[i][0], 10, baseline);
                g2.drawString(rows[i][1], 110, baseline);

                int risk = Integer.parseInt(rows[i][2]);
                g2.setColor(risk >= 85 ? criticalSwatch.getColor()
                        : risk >= 65 ? DarkTheme.AMBER
                        : risk >= 40 ? DarkTheme.PURPLE
                        : DarkTheme.GREEN);
                g2.drawString(rows[i][2], 190, baseline);

                g2.setColor("CLOSED".equals(rows[i][3]) ? DarkTheme.GREEN
                        : "NEW".equals(rows[i][3]) ? alertSwatch.getColor()
                        : DarkTheme.AMBER);
                g2.drawString(rows[i][3], 250, baseline);
            }

            g2.setColor(accentSwatch.getColor());
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(0, getHeight() - 3, getWidth(), getHeight() - 3);

            g2.setColor(DarkTheme.TEXT_MUTED);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString("Preview - " + (darkTheme.isSelected() ? "dark" : "light")
                    + " theme, font " + fontSize + ", row height " + rowHeight, 10, getHeight() - 10);
        }
    }
}
