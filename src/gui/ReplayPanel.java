package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.Hashtable;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

import util.DarkTheme;
import util.DateTimeUtil;
import util.DemoData;

public class ReplayPanel extends JPanel {

    private static final int TOTAL_EVENTS = 250_000;

    private static final int SIMULATED_MS_PER_EVENT = 50;

    private final JTextField pathField = DarkTheme.field(34);
    private final JComboBox<String> recentBox = new JComboBox<>(DemoData.sampleReplayFiles());
    private final JList<String> recentList = new JList<>(DemoData.sampleReplayFiles());

    private final JButton browseButton = DarkTheme.outlineButton("Browse...");
    private final JButton loadButton = DarkTheme.button("Load File");
    private final JButton startButton = DarkTheme.button("Start", DarkTheme.GREEN);
    private final JButton pauseButton = DarkTheme.button("Pause", DarkTheme.AMBER);
    private final JButton stopButton = DarkTheme.button("Stop", DarkTheme.RED);
    private final JButton resetButton = DarkTheme.outlineButton("Reset");

    private final JSlider speedSlider = new JSlider(1, 100, 10);
    private final JComboBox<String> speedBox = new JComboBox<>(
            new String[] { "1x  real time", "5x", "10x", "50x", "100x", "Maximum" });
    private final JSpinner refreshSpinner = DarkTheme.spinner(1, 60, 1, 5);
    private final JRadioButton eventMode = new JRadioButton("Event by event", true);
    private final JRadioButton burstMode = new JRadioButton("Continuous playback");
    private final ButtonGroup modeGroup = new ButtonGroup();
    private final JCheckBox loopCheck = new JCheckBox("Loop at end of file");
    private final JCheckBox autoScrollCheck = new JCheckBox("Auto-scroll event log", true);
    private final JToggleButton debugToggle = new JToggleButton("Verbose log");

    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JTextArea eventLog = new JTextArea();

    private final JLabel statusLabel = DarkTheme.statusLabel("NO FILE LOADED", DarkTheme.TEXT_MUTED);
    private final JLabel fileNameLabel = DarkTheme.bold("No file selected");
    private final JLabel eventsLabel = DarkTheme.bold("0 / " + String.format("%,d", TOTAL_EVENTS));
    private final JLabel rateLabel = DarkTheme.muted("0 events/sec");
    private final JLabel clockLabel = DarkTheme.muted("Simulated clock: --:--:--");

    private final Timer timer = new Timer(60, e -> tick());
    private final Random random = new Random(31337L);

    private int eventsProcessed = 0;
    private long simulatedClock = 0;
    private long lastRateTime = System.currentTimeMillis();
    private int eventsSinceRate = 0;
    private boolean paused;

    public ReplayPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Replay",
                "Load a historical market-data file and replay the event stream"), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        updateButtons();
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(DarkTheme.BG);
        center.add(buildFileCard(), BorderLayout.NORTH);
        center.add(buildMainArea(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFileCard() {
        JPanel card = DarkTheme.card("Market Data File", new BorderLayout(0, 8));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(DarkTheme.muted("File path"));
        pathField.setPreferredSize(new Dimension(430, DarkTheme.FIELD_HEIGHT));
        row.add(pathField);
        row.add(browseButton);
        row.add(loadButton);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        row2.add(DarkTheme.muted("Sample files"));
        recentBox.setPreferredSize(new Dimension(340, DarkTheme.FIELD_HEIGHT));
        row2.add(recentBox);
        JButton useSampleButton = DarkTheme.outlineButton("Use selected sample");
        useSampleButton.addActionListener(e -> {
            Object selected = recentBox.getSelectedItem();
            if (selected != null) {
                pathField.setText(String.valueOf(selected));
                loadFile(String.valueOf(selected));
            }
        });
        row2.add(useSampleButton);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(row);
        top.add(Box.createVerticalStrut(4));
        top.add(row2);

        card.add(top, BorderLayout.CENTER);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        info.setOpaque(false);
        info.add(fileNameLabel);
        info.add(DarkTheme.muted("Format: ITCH / TotalView-ITCH binary, EDF supported"));
        card.add(info, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMainArea() {
        JPanel main = new JPanel(new BorderLayout(10, 0));
        main.setBackground(DarkTheme.BG);
        main.add(buildControlColumn(), BorderLayout.WEST);
        main.add(buildLogCard(), BorderLayout.CENTER);
        main.add(buildRecentColumn(), BorderLayout.EAST);
        return main;
    }

    private JPanel buildControlColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(DarkTheme.BG);
        column.setPreferredSize(new Dimension(330, 100));

        JPanel transport = DarkTheme.card("Playback Controls", new GridLayout(2, 2, 8, 8));
        transport.add(startButton);
        transport.add(pauseButton);
        transport.add(stopButton);
        transport.add(resetButton);
        column.add(transport);
        column.add(Box.createVerticalStrut(10));

        JPanel speedCard = DarkTheme.card("Replay Speed", new BorderLayout(0, 6));

        speedSlider.setBackground(DarkTheme.SURFACE);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(1, DarkTheme.muted("slow"));
        labels.put(50, DarkTheme.muted("50"));
        labels.put(100, DarkTheme.muted("fast"));
        speedSlider.setLabelTable(labels);
        speedSlider.setPaintLabels(true);
        speedCard.add(speedSlider, BorderLayout.CENTER);

        JPanel speedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        speedRow.setOpaque(false);
        speedRow.add(DarkTheme.muted("Preset"));
        speedBox.setPreferredSize(new Dimension(150, DarkTheme.FIELD_HEIGHT));
        speedRow.add(speedBox);
        speedCard.add(speedRow, BorderLayout.SOUTH);

        column.add(speedCard);
        column.add(Box.createVerticalStrut(10));

        JPanel modeCard = DarkTheme.card("Replay Mode", new GridLayout(0, 1, 0, 4));
        modeGroup.add(eventMode);
        modeGroup.add(burstMode);
        for (JRadioButton button : new JRadioButton[] { eventMode, burstMode }) {
            button.setOpaque(false);
            button.setForeground(DarkTheme.TEXT);
            button.setFont(DarkTheme.FONT_BASE);
            modeCard.add(button);
        }
        modeCard.add(Box.createVerticalStrut(4));
        loopCheck.setOpaque(false);
        loopCheck.setForeground(DarkTheme.TEXT);
        autoScrollCheck.setOpaque(false);
        autoScrollCheck.setForeground(DarkTheme.TEXT);
        modeCard.add(loopCheck);
        modeCard.add(autoScrollCheck);

        JPanel optionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        optionsRow.setOpaque(false);
        optionsRow.add(DarkTheme.muted("Batch size"));
        optionsRow.add(refreshSpinner);
        optionsRow.add(DarkTheme.muted("events"));
        modeCard.add(optionsRow);

        debugToggle.setFont(DarkTheme.FONT_BOLD);
        debugToggle.setBackground(DarkTheme.SURFACE_ALT);
        debugToggle.setForeground(DarkTheme.TEXT);
        debugToggle.setFocusPainted(false);
        modeCard.add(debugToggle);

        column.add(modeCard);
        column.add(Box.createVerticalStrut(10));

        JPanel progressCard = DarkTheme.card("Progress", new GridLayout(0, 1, 0, 6));

        progressBar.setStringPainted(true);
        progressBar.setForeground(DarkTheme.ACCENT);
        progressBar.setBackground(DarkTheme.SURFACE_ALT);
        progressBar.setBorderPainted(false);
        progressBar.setString("0%");

        progressCard.add(progressBar);

        JPanel stats = new JPanel(new GridLayout(2, 2, 6, 4));
        stats.setOpaque(false);
        stats.add(DarkTheme.muted("Events"));
        stats.add(eventsLabel);
        stats.add(DarkTheme.muted("Throughput"));
        stats.add(rateLabel);
        progressCard.add(stats);
        progressCard.add(clockLabel);

        column.add(progressCard);
        column.add(Box.createVerticalGlue());
        return column;
    }

    private JPanel buildLogCard() {
        JPanel card = DarkTheme.card("Event Log (simulated)", new BorderLayout(0, 6));

        eventLog.setEditable(false);
        eventLog.setFont(DarkTheme.FONT_MONO);
        eventLog.setBackground(DarkTheme.SURFACE_ALT);
        eventLog.setForeground(DarkTheme.TEXT);
        eventLog.setCaretColor(DarkTheme.TEXT);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);
        card.add(DarkTheme.scroll(eventLog), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton clearButton = DarkTheme.outlineButton("Clear log");
        JButton saveButton = DarkTheme.outlineButton("Save log (simulated)");
        clearButton.addActionListener(e -> eventLog.setText(""));
        saveButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save replay log");
            chooser.setSelectedFile(new File("replay_log.txt"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(this,
                        "Replay log would be written to:\n"
                      + chooser.getSelectedFile().getAbsolutePath()
                      + "\n\nThis is simulated in the current build.",
                        "Save log", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        buttons.add(clearButton);
        buttons.add(saveButton);
        card.add(buttons, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildRecentColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(DarkTheme.BG);
        column.setPreferredSize(new Dimension(240, 100));

        JPanel card = DarkTheme.card("Recent Files", new BorderLayout(0, 6));
        recentList.setBackground(DarkTheme.SURFACE_ALT);
        recentList.setForeground(DarkTheme.TEXT);
        recentList.setFont(DarkTheme.FONT_SMALL);
        recentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentList.setVisibleRowCount(10);

        recentList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = recentList.getSelectedValue();
                if (selected != null) {
                    pathField.setText(selected);
                }
            }
        });
        card.add(DarkTheme.scroll(recentList), BorderLayout.CENTER);

        JButton openSelected = DarkTheme.button("Load Selected");
        openSelected.addActionListener(e -> {
            String selected = recentList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, "Select a file from the list first.",
                        "Replay", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            loadFile(selected);
        });
        card.add(openSelected, BorderLayout.SOUTH);

        column.add(card);
        column.add(Box.createVerticalStrut(10));

        JPanel infoCard = DarkTheme.card("File Information", new GridLayout(0, 1, 0, 4));
        infoCard.add(DarkTheme.muted("File size      : 250,000 events"));
        infoCard.add(DarkTheme.muted("Session        : 09:30 - 16:00"));
        infoCard.add(DarkTheme.muted("Instruments    : 20"));
        infoCard.add(DarkTheme.muted("Message types  : A, F, E, C, X, D"));
        infoCard.add(DarkTheme.muted("Replay engine  : simulated build"));
        column.add(infoCard);

        column.add(Box.createVerticalGlue());
        return column;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(DarkTheme.muted("|  The replay engine is simulated for the first review"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(DarkTheme.muted("Events are generated, not read from disk"));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void wireEvents() {
        browseButton.addActionListener(e -> browseForFile());
        loadButton.addActionListener(e -> loadFile(pathField.getText()));

        startButton.addActionListener(e -> startReplay());
        pauseButton.addActionListener(e -> pauseReplay());
        stopButton.addActionListener(e -> stopReplay());
        resetButton.addActionListener(e -> resetReplay());

        speedSlider.addChangeListener(e -> {
            int value = speedSlider.getValue();
            if (value <= 2) {
                speedBox.setSelectedIndex(0);
            } else if (value <= 10) {
                speedBox.setSelectedIndex(1);
            } else if (value <= 25) {
                speedBox.setSelectedIndex(2);
            } else if (value <= 60) {
                speedBox.setSelectedIndex(3);
            } else if (value <= 90) {
                speedBox.setSelectedIndex(4);
            } else {
                speedBox.setSelectedIndex(5);
            }
            if (eventsProcessed > 0) {
                statusLabel.setText("  \u25CF   SPEED " + speedMultiplier() + "x");
            }
        });

        speedBox.addActionListener(e -> {
            int[] values = { 1, 5, 10, 50, 100, 100 };
            int index = speedBox.getSelectedIndex();
            if (index >= 0) {
                speedSlider.setValue(values[index]);
            }
        });

        eventMode.addActionListener(e -> refreshSpinner.setEnabled(false));
        burstMode.addActionListener(e -> refreshSpinner.setEnabled(true));
        refreshSpinner.setEnabled(false);
    }

    private void browseForFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a historical market-data file");
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "Market data files (*.bin, *.itch, *.edf)", "bin", "itch", "edf"));
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("All files", "*"));

        File sample = new File(DemoData.sampleReplayFiles()[0]);
        chooser.setCurrentDirectory(new File(sample.getAbsoluteFile().getParent()));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please choose a market-data file first.",
                    "No file selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        pathField.setText(path.trim());
        String name = new File(path.trim()).getName();
        fileNameLabel.setText("Loaded: " + name);
        statusLabel.setText("  \u25CF   FILE LOADED - READY TO REPLAY");
        statusLabel.setForeground(DarkTheme.GREEN);

        eventLog.append(String.format("[%s] File loaded: %s%n",
                DateTimeUtil.time(System.currentTimeMillis()), path.trim()));
        eventLog.append(String.format("[%s] %s events detected, 20 instruments, session 09:30-16:00%n",
                DateTimeUtil.time(System.currentTimeMillis()), String.format("%,d", TOTAL_EVENTS)));
        eventLog.append(String.format("[%s] Replay engine ready.%n%n",
                DateTimeUtil.time(System.currentTimeMillis())));
        updateButtons();
    }

    private void startReplay() {
        if (pathField.getText() == null || pathField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Load a file before starting the replay.",
                    "Replay", JOptionPane.WARNING_MESSAGE);
            return;
        }
        paused = false;
        timer.setDelay(60);
        timer.start();
        statusLabel.setText("  \u25CF   REPLAYING " + speedMultiplier() + "x");
        statusLabel.setForeground(DarkTheme.GREEN);
        eventLog.append(String.format("[%s] Replay started at %sx speed.%n",
                DateTimeUtil.time(System.currentTimeMillis()), speedMultiplier()));
        updateButtons();
    }

    private void pauseReplay() {
        if (!timer.isRunning()) {
            return;
        }
        timer.stop();
        paused = true;
        statusLabel.setText("  \u25CF   PAUSED");
        statusLabel.setForeground(DarkTheme.AMBER);
        eventLog.append(String.format("[%s] Replay paused at event %,d.%n",
                DateTimeUtil.time(System.currentTimeMillis()), eventsProcessed));
        updateButtons();
    }

    private void stopReplay() {
        timer.stop();
        paused = false;
        statusLabel.setText("  \u25CF   STOPPED");
        statusLabel.setForeground(DarkTheme.RED);
        eventLog.append(String.format("[%s] Replay stopped at event %,d (%.1f%% complete).%n",
                DateTimeUtil.time(System.currentTimeMillis()), eventsProcessed, progress()));

        if (eventsProcessed > 0 && eventsProcessed < TOTAL_EVENTS) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Replay stopped before the end of the file.\n\n"
                  + "Events processed : " + String.format("%,d", eventsProcessed) + "\n"
                  + "Progress         : " + String.format("%.1f%%", progress()) + "\n\n"
                  + "Reset the replay position?",
                    "Replay stopped", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                resetReplay();
            }
        }
        updateButtons();
    }

    private void resetReplay() {
        timer.stop();
        eventsProcessed = 0;
        simulatedClock = 0;
        lastRateTime = System.currentTimeMillis();
        eventsSinceRate = 0;
        paused = false;
        progressBar.setValue(0);
        progressBar.setString("0%");
        eventsLabel.setText("0 / " + String.format("%,d", TOTAL_EVENTS));
        rateLabel.setText("0 events/sec");
        clockLabel.setText("Simulated clock: --:--:--");
        statusLabel.setText("  \u25CF   READY");
        statusLabel.setForeground(DarkTheme.GREEN);
        eventLog.append(String.format("[%s] Replay position reset.%n",
                DateTimeUtil.time(System.currentTimeMillis())));
        updateButtons();
    }

    private void tick() {
        int batch = burstMode.isSelected() ? (Integer) refreshSpinner.getValue() : 1;
        int multiplier = speedMultiplier();
        batch = Math.max(1, batch * Math.max(1, multiplier / 5));

        for (int i = 0; i < batch && eventsProcessed < TOTAL_EVENTS; i++) {
            eventsProcessed++;
            simulatedClock += SIMULATED_MS_PER_EVENT;
            eventsSinceRate++;

            if (debugToggle.isSelected() || eventsProcessed % 5000 == 0) {
                logEvent();
            }
        }

        progressBar.setValue((int) Math.round(progress()));
        progressBar.setString(String.format("%.1f%%", progress()));
        eventsLabel.setText(String.format("%,d / %,d", eventsProcessed, TOTAL_EVENTS));
        clockLabel.setText("Simulated clock: " + simulatedTime());

        long now = System.currentTimeMillis();
        if (now - lastRateTime >= 1000) {
            long rate = eventsSinceRate * 1000L / Math.max(1, now - lastRateTime);
            rateLabel.setText(String.format("%,d events/sec", rate));
            lastRateTime = now;
            eventsSinceRate = 0;
        }

        if (eventsProcessed >= TOTAL_EVENTS) {
            timer.stop();
            if (loopCheck.isSelected()) {
                eventLog.append(String.format("[%s] End of file reached - looping.%n%n",
                        DateTimeUtil.time(System.currentTimeMillis())));
                resetReplay();
                startReplay();
            } else {
                statusLabel.setText("  \u25CF   REPLAY COMPLETE");
                statusLabel.setForeground(DarkTheme.GREEN);
                eventLog.append(String.format("[%s] Replay complete: %,d events processed.%n",
                        DateTimeUtil.time(System.currentTimeMillis()), eventsProcessed));
                updateButtons();
            }
        }
    }

    private void logEvent() {
        String[] symbols = DemoData.symbols();
        String symbol = symbols[random.nextInt(symbols.length)];
        int messageType = random.nextInt(5);
        double price = 50 + random.nextDouble() * 900;
        int size = 100 + random.nextInt(2000);

        String line;
        switch (messageType) {
            case 0:
                line = String.format("A  ADD ORDER    %-6s %s  %,8d @ %8.2f",
                        symbol, random.nextBoolean() ? "BUY " : "SELL", size, price);
                break;
            case 1:
                line = String.format("E  EXECUTE      %-6s %s  %,8d @ %8.2f",
                        symbol, "----", size, price);
                break;
            case 2:
                line = String.format("C  CANCEL       %-6s %s  %,8d @ %8.2f",
                        symbol, "----", size, price);
                break;
            case 3:
                line = String.format("D  DELETE       %-6s %s  %,8d @ %8.2f",
                        symbol, "----", size, price);
                break;
            default:
                line = String.format("X  ORDER CANCEL %-6s %s  %,8d @ %8.2f",
                        symbol, "----", size, price);
                break;
        }
        eventLog.append(String.format("%s  %,9d  %s%n", simulatedTime(), eventsProcessed, line));

        if (autoScrollCheck.isSelected()) {
            eventLog.setCaretPosition(eventLog.getDocument().getLength());
        }
    }

    private int speedMultiplier() {
        int value = speedSlider.getValue();
        if (value <= 1) {
            return 1;
        } else if (value <= 10) {
            return 5;
        } else if (value <= 25) {
            return 10;
        } else if (value <= 60) {
            return 50;
        } else if (value <= 90) {
            return 100;
        }
        return 200;
    }

    private double progress() {
        return eventsProcessed * 100.0 / TOTAL_EVENTS;
    }

    private String simulatedTime() {
        long startOfSession = 9 * 3600_000L + 30 * 60_000L;
        long millis = startOfSession + simulatedClock;
        long hours = millis / 3600_000L % 24;
        long minutes = millis / 60_000L % 60;
        long seconds = millis / 1000L % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateButtons() {
        boolean running = timer.isRunning();
        boolean hasFile = pathField.getText() != null && !pathField.getText().trim().isEmpty();
        startButton.setEnabled(!running && hasFile);
        pauseButton.setEnabled(running);
        stopButton.setEnabled(running || eventsProcessed > 0);
        resetButton.setEnabled(eventsProcessed > 0 || running);
    }
}
