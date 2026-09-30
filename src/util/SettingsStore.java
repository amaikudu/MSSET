package util;

import java.awt.Color;

public final class SettingsStore {

    private SettingsStore() {

    }

    public static String theme = "Dark";
    public static int fontSize = 13;
    public static int tableRowHeight = 26;
    public static Color accentColor = DarkTheme.ACCENT;
    public static Color alertColor = DarkTheme.RED;
    public static Color criticalColor = new Color(0xFF6B6B);
    public static boolean showGridLines = true;
    public static boolean alternateRowColors = true;
    public static boolean useMonospacedNumbers = true;

    public static int refreshIntervalMs = 1000;
    public static String defaultSymbol = "AAPL";
    public static boolean autoConnect = true;
    public static int orderBookDepth = 10;
    public static int priceDecimals = 2;
    public static boolean thousandsSeparator = true;

    public static int riskThreshold = 30;
    public static boolean alertSound = true;
    public static boolean autoMarkReviewed = false;
    public static boolean showLowSeverity = true;
    public static int maxAlertQueue = 500;

    public static boolean desktopNotifications = true;
    public static boolean emailNotifications = false;
    public static String notificationEmail = "surveillance@exchange.example";
    public static boolean popupOnCritical = true;
    public static String summaryFrequency = "Every 15 minutes";

    public static int archiveRetentionDays = 90;
    public static String logLevel = "INFO";
    public static String exportPath = System.getProperty("user.home") + "/surveillance-reports";

    public static void restoreDefaults() {
        theme = "Dark";
        fontSize = 13;
        tableRowHeight = 26;
        accentColor = DarkTheme.ACCENT;
        alertColor = DarkTheme.RED;
        criticalColor = new Color(0xFF6B6B);
        showGridLines = true;
        alternateRowColors = true;
        useMonospacedNumbers = true;

        refreshIntervalMs = 1000;
        defaultSymbol = "AAPL";
        autoConnect = true;
        orderBookDepth = 10;
        priceDecimals = 2;
        thousandsSeparator = true;

        riskThreshold = 30;
        alertSound = true;
        autoMarkReviewed = false;
        showLowSeverity = true;
        maxAlertQueue = 500;

        desktopNotifications = true;
        emailNotifications = false;
        notificationEmail = "surveillance@exchange.example";
        popupOnCritical = true;
        summaryFrequency = "Every 15 minutes";

        archiveRetentionDays = 90;
        logLevel = "INFO";
        exportPath = System.getProperty("user.home") + "/surveillance-reports";
    }
}
