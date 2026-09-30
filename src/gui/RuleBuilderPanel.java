package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.SurveillanceRule;
import model.SurveillanceRule.RuleCondition;
import service.DataStore;
import util.DarkTheme;
import util.DateTimeUtil;

public class RuleBuilderPanel extends JPanel {

    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_CONDITIONS = 2;
    private static final int COL_LOGIC = 3;
    private static final int COL_SEVERITY = 4;
    private static final int COL_ENABLED = 5;
    private static final int COL_MATCHES = 6;
    private static final int COL_CREATED = 7;

    private final DataStore store;

    private final JTextField nameField = DarkTheme.searchField("Rule name, e.g. Large Order Cancellation", 26);
    private final JComboBox<String> fieldBox = new JComboBox<>(SurveillanceRule.FIELDS);
    private final JComboBox<String> operatorBox = new JComboBox<>(SurveillanceRule.OPERATORS);
    private final JSpinner valueSpinner = DarkTheme.doubleSpinner(0, 10_000_000, 100, 5000);
    private final JSpinner value2Spinner = DarkTheme.doubleSpinner(0, 10_000_000, 100, 10000);
    private final JLabel betweenLabel = DarkTheme.muted("and");

    private final DefaultTableModel conditionModel = new DefaultTableModel(
            new Object[] { "#", "Field", "Operator", "Value" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable conditionTable = new JTable(conditionModel);

    private final JRadioButton andButton = new JRadioButton("AND (all conditions must match)", true);
    private final JRadioButton orButton = new JRadioButton("OR (any condition may match)");
    private final ButtonGroup logicGroup = new ButtonGroup();
    private final JComboBox<String> severityBox = new JComboBox<>(SurveillanceRule.SEVERITIES);
    private final JComboBox<String> detectionBox = new JComboBox<>(util.DemoData.detectionTypes());
    private final JToggleButton enabledToggle = new JToggleButton("Rule enabled", true);
    private final JCheckBox alertOnMatchCheck = new JCheckBox("Raise alert when matched", true);
    private final JCheckBox notifyCheck = new JCheckBox("Notify supervision team", false);

    private final JButton addConditionButton = DarkTheme.button("Add Condition");
    private final JButton removeConditionButton = DarkTheme.outlineButton("Remove Selected");
    private final JButton clearButton = DarkTheme.outlineButton("Clear Conditions");
    private final JButton saveButton = DarkTheme.button("Save Rule", DarkTheme.GREEN);
    private final JButton newButton = DarkTheme.outlineButton("New Rule");
    private final JButton deleteButton = DarkTheme.button("Delete Rule", DarkTheme.RED);
    private final JButton validateButton = DarkTheme.outlineButton("Validate");

    private final DefaultTableModel rulesModel = new DefaultTableModel(
            new Object[] { "Rule ID", "Rule Name", "Conditions", "Logic", "Severity",
                           "Enabled", "Matches", "Created" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable rulesTable = new JTable(rulesModel);

    private final JTextArea previewArea = new JTextArea();
    private final JTextArea summaryArea = new JTextArea();

    private final JLabel statusLabel = DarkTheme.statusLabel("RULE BUILDER READY", DarkTheme.GREEN);
    private final JLabel countLabel = DarkTheme.bold("0 rules");
    private final JLabel enabledLabel = DarkTheme.muted("0 enabled");
    private final JLabel selectedRuleLabel = DarkTheme.muted("No rule loaded");

    private SurveillanceRule editingRule;

    public RuleBuilderPanel() {
        this(new DataStore());
    }

    public RuleBuilderPanel(DataStore store) {
        this.store = store;

        setLayout(new BorderLayout(0, 10));
        setBackground(DarkTheme.BG);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(DarkTheme.header("Rule Builder",
                "Build surveillance rules visually - conditions, logic, severity and saved rule set"),
                BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        wireEvents();
        severityBox.setSelectedItem(SurveillanceRule.SEVERITY_HIGH);
        refreshRules();
        updatePreview();
    }

    private JSplitPane buildSplit() {
        return DarkTheme.split(buildEditorSide(), buildRulesSide(), 660);
    }

    private JPanel buildEditorSide() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(DarkTheme.BG);
        side.setPreferredSize(new Dimension(660, 100));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        JPanel identity = buildIdentityCard();
        identity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        JPanel conditions = buildConditionCard();
        conditions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));
        JPanel logic = buildLogicCard();
        logic.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        top.add(identity);
        top.add(Box.createVerticalStrut(8));
        top.add(conditions);
        top.add(Box.createVerticalStrut(8));
        top.add(logic);
        side.add(top, BorderLayout.NORTH);
        side.add(buildPreviewCard(), BorderLayout.CENTER);

        return side;
    }

    private JPanel buildIdentityCard() {
        JPanel card = DarkTheme.card("Rule identity", new GridLayout(2, 2, 10, 8));

        nameField.setPreferredSize(null);
        card.add(titledBlock("Rule name", nameField));

        detectionBox.setPreferredSize(null);
        card.add(titledBlock("Detection type", detectionBox));

        severityBox.setPreferredSize(null);
        card.add(titledBlock("Severity when matched", severityBox));

        enabledToggle.setFont(DarkTheme.FONT_BOLD);
        enabledToggle.setBackground(DarkTheme.SURFACE_ALT);
        enabledToggle.setForeground(DarkTheme.TEXT);
        enabledToggle.setFocusPainted(false);
        card.add(titledBlock("Rule status", enabledToggle));

        return card;
    }

    private JPanel buildConditionCard() {
        JPanel card = DarkTheme.card("Conditions", new BorderLayout(0, 8));

        JPanel editor = new JPanel(new GridLayout(2, 1, 0, 6));
        editor.setOpaque(false);

        JPanel rowOne = new JPanel(new BorderLayout(8, 0));
        rowOne.setOpaque(false);

        fieldBox.setPreferredSize(null);
        operatorBox.setPreferredSize(null);
        valueSpinner.setPreferredSize(null);
        value2Spinner.setPreferredSize(null);

        JPanel inputs = new JPanel(new GridLayout(1, 4, 8, 0));
        inputs.setOpaque(false);
        inputs.add(titledBlock("Field", fieldBox));
        inputs.add(titledBlock("Operator", operatorBox));
        inputs.add(titledBlock("Value", valueSpinner));
        inputs.add(titledBlock("Upper bound", value2Spinner));

        JPanel addHolder = new JPanel(new BorderLayout(0, 3));
        addHolder.setOpaque(false);
        addHolder.add(DarkTheme.muted(" "), BorderLayout.NORTH);
        addHolder.add(addConditionButton, BorderLayout.CENTER);

        rowOne.add(inputs, BorderLayout.CENTER);
        rowOne.add(addHolder, BorderLayout.EAST);

        JPanel rowTwo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowTwo.setOpaque(false);
        rowTwo.add(betweenLabel);
        rowTwo.add(removeConditionButton);
        rowTwo.add(clearButton);
        rowTwo.add(validateButton);

        editor.add(rowOne);
        editor.add(rowTwo);

        card.add(editor, BorderLayout.NORTH);

        DarkTheme.styleTable(conditionTable);
        conditionTable.setRowHeight(23);
        DarkTheme.setColumnWidth(conditionTable, 0, 35);
        DarkTheme.setColumnWidth(conditionTable, 1, 200);
        DarkTheme.setColumnWidth(conditionTable, 2, 80);
        conditionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = DarkTheme.scroll(conditionTable);
        scroll.setPreferredSize(new Dimension(620, 110));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildLogicCard() {
        JPanel card = DarkTheme.card("Logic, severity and actions", new BorderLayout(0, 8));

        JPanel top = new JPanel(new GridLayout(1, 2, 10, 0));
        top.setOpaque(false);

        logicGroup.add(andButton);
        logicGroup.add(orButton);
        andButton.setOpaque(false);
        orButton.setOpaque(false);
        andButton.setForeground(DarkTheme.TEXT);
        orButton.setForeground(DarkTheme.TEXT);

        JPanel logicPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        logicPanel.setOpaque(false);
        logicPanel.add(andButton);
        logicPanel.add(orButton);
        top.add(titledBlock("Condition logic", logicPanel));

        JPanel checks = new JPanel(new GridLayout(2, 1, 0, 2));
        checks.setOpaque(false);
        alertOnMatchCheck.setOpaque(false);
        alertOnMatchCheck.setForeground(DarkTheme.TEXT);
        notifyCheck.setOpaque(false);
        notifyCheck.setForeground(DarkTheme.TEXT);
        checks.add(alertOnMatchCheck);
        checks.add(notifyCheck);
        top.add(titledBlock("Actions", checks));

        card.add(top, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(newButton);
        buttons.add(deleteButton);
        card.add(buttons, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildPreviewCard() {
        JPanel card = DarkTheme.card("Rule preview and evaluation summary", new GridLayout(1, 2, 10, 0));

        previewArea.setEditable(false);
        previewArea.setFont(DarkTheme.FONT_MONO);
        previewArea.setBackground(DarkTheme.SURFACE_ALT);
        previewArea.setForeground(DarkTheme.TEXT);
        previewArea.setRows(6);

        summaryArea.setEditable(false);
        summaryArea.setFont(DarkTheme.FONT_MONO);
        summaryArea.setBackground(DarkTheme.SURFACE_ALT);
        summaryArea.setForeground(DarkTheme.TEXT);
        summaryArea.setRows(6);

        card.add(DarkTheme.scroll(previewArea));
        card.add(DarkTheme.scroll(summaryArea));
        return card;
    }

    private JPanel buildRulesSide() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setBackground(DarkTheme.BG);

        side.add(buildRulesToolbar(), BorderLayout.NORTH);

        JPanel card = DarkTheme.card("Saved rules", new BorderLayout());
        DarkTheme.styleTable(rulesTable);
        rulesTable.setRowHeight(23);
        rulesTable.getColumnModel().getColumn(COL_SEVERITY).setCellRenderer(new SeverityRenderer());
        rulesTable.getColumnModel().getColumn(COL_ENABLED).setCellRenderer(new EnabledRenderer());
        DarkTheme.alignCenter(rulesTable, COL_ENABLED);
        DarkTheme.alignRight(rulesTable, COL_MATCHES);
        DarkTheme.setColumnWidth(rulesTable, COL_ID, 70);
        DarkTheme.setColumnWidth(rulesTable, COL_NAME, 190);
        DarkTheme.setColumnWidth(rulesTable, COL_CONDITIONS, 90);
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        card.add(DarkTheme.scroll(rulesTable), BorderLayout.CENTER);
        side.add(card, BorderLayout.CENTER);

        return side;
    }

    private JPanel buildRulesToolbar() {
        JPanel bar = DarkTheme.card(new FlowLayout(FlowLayout.LEFT, 6, 6));

        JButton loadButton = DarkTheme.outlineButton("Load");
        JButton toggleButton = DarkTheme.outlineButton("Enable / Disable");
        JButton duplicateButton = DarkTheme.outlineButton("Duplicate");
        JButton exportButton = DarkTheme.outlineButton("Export");

        loadButton.addActionListener(e -> loadSelectedRule());
        toggleButton.addActionListener(e -> toggleSelectedRule());
        duplicateButton.addActionListener(e -> duplicateSelectedRule());
        exportButton.addActionListener(e -> exportRules());

        bar.add(loadButton);
        bar.add(toggleButton);
        bar.add(duplicateButton);
        bar.add(exportButton);

        return bar;
    }

    private JPanel buildFooter() {
        JPanel bar = DarkTheme.card(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(statusLabel);
        left.add(countLabel);
        left.add(enabledLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(selectedRuleLabel);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel titledBlock(String caption, Component component) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        panel.add(DarkTheme.muted(caption), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        operatorBox.addActionListener(e -> updateBetweenState());
        addConditionButton.addActionListener(e -> addCondition());
        removeConditionButton.addActionListener(e -> removeCondition());
        clearButton.addActionListener(e -> {
            conditionModel.setRowCount(0);
            updatePreview();
        });
        validateButton.addActionListener(e -> validateRule());
        saveButton.addActionListener(e -> saveRule());
        newButton.addActionListener(e -> newRule());
        deleteButton.addActionListener(e -> deleteSelectedRule());

        andButton.addActionListener(e -> updatePreview());
        orButton.addActionListener(e -> updatePreview());
        severityBox.addActionListener(e -> updatePreview());
        detectionBox.addActionListener(e -> updatePreview());
        enabledToggle.addActionListener(e -> {
            enabledToggle.setForeground(enabledToggle.isSelected()
                    ? DarkTheme.GREEN : DarkTheme.TEXT_MUTED);
            updatePreview();
        });

        stateChangedForPreview(nameField);

        rulesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && rulesTable.getSelectedRow() >= 0) {
                showSelectedRuleSummary();
            }
        });

        rulesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && rulesTable.getSelectedRow() >= 0) {
                    loadSelectedRule();
                }
            }

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
                    int row = rulesTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        rulesTable.setRowSelectionInterval(row, row);
                    }
                    rulesPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        updateBetweenState();
    }

    private final JPopupMenu rulesPopup = buildRulesPopup();

    private JPopupMenu buildRulesPopup() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem loadItem = new JMenuItem("Load into editor");
        JMenuItem toggleItem = new JMenuItem("Enable / disable");
        JMenuItem duplicateItem = new JMenuItem("Duplicate rule");
        JMenuItem deleteItem = new JMenuItem("Delete rule");

        loadItem.addActionListener(e -> loadSelectedRule());
        toggleItem.addActionListener(e -> toggleSelectedRule());
        duplicateItem.addActionListener(e -> duplicateSelectedRule());
        deleteItem.addActionListener(e -> deleteSelectedRule());

        menu.add(loadItem);
        menu.add(toggleItem);
        menu.add(duplicateItem);
        menu.addSeparator();
        menu.add(deleteItem);
        return menu;
    }

    private void stateChangedForPreview(JTextField field) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updatePreview();
            }
        });
    }

    private void updateBetweenState() {
        boolean between = "between".equals(operatorBox.getSelectedItem());
        value2Spinner.setEnabled(between);
        betweenLabel.setForeground(between ? DarkTheme.TEXT : DarkTheme.TEXT_MUTED);
    }

    private void addCondition() {
        String field = (String) fieldBox.getSelectedItem();
        String operator = (String) operatorBox.getSelectedItem();
        double value = ((Number) valueSpinner.getValue()).doubleValue();
        double value2 = ((Number) value2Spinner.getValue()).doubleValue();

        if ("between".equals(operator) && value2 < value) {
            DarkTheme.info(this, "For a \"between\" condition the upper bound must be greater than\n"
                  + "the lower bound.\n\nLower bound : " + value + "\nUpper bound : " + value2, "Invalid condition");
            return;
        }

        if (conditionModel.getRowCount() >= 8) {
            DarkTheme.info(this, "A rule may hold at most 8 conditions in this build.", "Rule Builder");
            return;
        }

        RuleCondition condition = new RuleCondition(field, operator, value, value2);
        conditionModel.addRow(new Object[] {
            conditionModel.getRowCount() + 1,
            condition.getField(),
            condition.getOperator(),
            condition.getValueText()
        });

        statusLabel.setText("  \u25CF   CONDITION ADDED");
        statusLabel.setForeground(DarkTheme.GREEN);
        updatePreview();
    }

    private void removeCondition() {
        int row = conditionTable.getSelectedRow();
        if (row < 0) {
            DarkTheme.info(this, "Select a condition row to remove.", "Rule Builder");
            return;
        }
        conditionModel.removeRow(conditionTable.convertRowIndexToModel(row));
        renumberConditions();
        updatePreview();
    }

    private void renumberConditions() {
        for (int i = 0; i < conditionModel.getRowCount(); i++) {
            conditionModel.setValueAt(i + 1, i, 0);
        }
    }

    private void updatePreview() {
        StringBuilder sb = new StringBuilder();
        sb.append("RULE DEFINITION\n");
        sb.append("--------------------------------------\n");
        sb.append(String.format("Name      : %s%n", ruleName()));
        sb.append(String.format("Detection : %s%n", detectionBox.getSelectedItem()));
        sb.append(String.format("Severity  : %s%n", severityBox.getSelectedItem()));
        sb.append(String.format("Status    : %s%n", enabledToggle.isSelected() ? "ENABLED" : "DISABLED"));
        sb.append("--------------------------------------\n");
        sb.append("CONDITIONS\n");
        sb.append("--------------------------------------\n");

        if (conditionModel.getRowCount() == 0) {
            sb.append("(no conditions defined yet)\n");
        } else {
            String logic = orButton.isSelected() ? "OR" : "AND";
            for (int i = 0; i < conditionModel.getRowCount(); i++) {
                if (i > 0) {
                    sb.append(String.format("%-10s%n", logic));
                }
                sb.append(String.format("%d. %s %s %s%n", i + 1,
                        conditionModel.getValueAt(i, 1),
                        conditionModel.getValueAt(i, 2),
                        conditionModel.getValueAt(i, 3)));
            }
        }
        sb.append("--------------------------------------\n");
        sb.append("Actions: ");
        if (alertOnMatchCheck.isSelected()) {
            sb.append("raise alert  ");
        }
        if (notifyCheck.isSelected()) {
            sb.append("notify team");
        }
        if (!alertOnMatchCheck.isSelected() && !notifyCheck.isSelected()) {
            sb.append("none");
        }
        sb.append('\n');

        previewArea.setText(sb.toString());
        previewArea.setCaretPosition(0);
        updateSummary();
    }

    private void updateSummary() {
        int conditions = conditionModel.getRowCount();
        boolean enabled = enabledToggle.isSelected();
        String logic = orButton.isSelected() ? "OR" : "AND";
        String severity = String.valueOf(severityBox.getSelectedItem());

        int base = 22;
        int estimate;
        if (conditions == 0) {
            estimate = 0;
        } else if ("AND".equals(logic)) {
            estimate = Math.max(1, base / conditions);
        } else {
            estimate = Math.min(90, base + (conditions - 1) * 12);
        }
        if (!enabled) {
            estimate = 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("EVALUATION SUMMARY\n");
        sb.append("--------------------------------------\n");
        sb.append(String.format("Conditions      : %d%n", conditions));
        sb.append(String.format("Combined with   : %s%n", logic));
        sb.append(String.format("Severity        : %s%n", severity));
        sb.append(String.format("State           : %s%n", enabled ? "ACTIVE" : "INACTIVE"));
        sb.append("--------------------------------------\n");
        sb.append(String.format("Estimated hit rate : about %d%% of scanned events%n", estimate));
        sb.append("--------------------------------------\n");
        sb.append("How the logic works\n");
        if (conditions == 0) {
            sb.append("A rule needs at least one condition before it\ncan match anything.\n");
        } else if ("AND".equals(logic)) {
            sb.append("Every condition must be true at the same time.\n");
            sb.append("This finds precise patterns but matches less often.\n");
        } else {
            sb.append("Any single condition being true is enough.\n");
            sb.append("This catches more events but produces more\nfalse positives.\n");
        }
        sb.append("--------------------------------------\n");
        sb.append("The hit rate is an estimate for the demo. A real\n");
        sb.append("backtest would replay historical data through the\nrule and count the matches.\n");

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
    }

    private String ruleName() {
        String name = DarkTheme.readField(nameField).trim();
        return name.isEmpty() ? "(unnamed rule)" : name;
    }

    private void validateRule() {
        List<String> problems = new java.util.ArrayList<>();

        if (DarkTheme.readField(nameField).trim().isEmpty()) {
            problems.add("The rule needs a name.");
        }
        if (conditionModel.getRowCount() == 0) {
            problems.add("The rule needs at least one condition.");
        }
        if ("AND".equals(andButton.isSelected() ? "AND" : "OR") && conditionModel.getRowCount() > 5) {
            problems.add("More than 5 conditions combined with AND will almost never match.");
        }
        if (!enabledToggle.isSelected()) {
            problems.add("Warning: the rule is currently disabled and will not run.");
        }

        if (problems.isEmpty()) {
            DarkTheme.info(this, "Validation passed.\n\n"
                  + "Rule      : " + ruleName() + "\n"
                  + "Conditions: " + conditionModel.getRowCount() + "\n"
                  + "Logic     : " + (orButton.isSelected() ? "OR" : "AND") + "\n"
                  + "Severity  : " + severityBox.getSelectedItem(), "Rule valid");
            statusLabel.setText("  \u25CF   RULE VALID");
            statusLabel.setForeground(DarkTheme.GREEN);
        } else {
            StringBuilder sb = new StringBuilder("The rule is not ready:\n\n");
            for (String problem : problems) {
                sb.append("\u2022 ").append(problem).append('\n');
            }
            DarkTheme.info(this, sb.toString(), "Validation result");
            statusLabel.setText("  \u25CF   VALIDATION FAILED");
            statusLabel.setForeground(DarkTheme.AMBER);
        }
    }

    private void saveRule() {
        String name = DarkTheme.readField(nameField).trim();
        if (name.isEmpty()) {
            DarkTheme.info(this, "Please enter a rule name before saving.", "Rule Builder");
            nameField.requestFocusInWindow();
            return;
        }
        if (conditionModel.getRowCount() == 0) {
            DarkTheme.info(this, "Add at least one condition before saving the rule.", "Rule Builder");
            return;
        }

        SurveillanceRule rule;
        boolean updating = editingRule != null;
        if (updating) {
            rule = editingRule;
            rule.setName(name);
            rule.clearConditions();
        } else {
            rule = new SurveillanceRule(store.nextRuleId(), name, System.currentTimeMillis());
        }

        for (int i = 0; i < conditionModel.getRowCount(); i++) {
            String field = String.valueOf(conditionModel.getValueAt(i, 1));
            String operator = String.valueOf(conditionModel.getValueAt(i, 2));
            String valueText = String.valueOf(conditionModel.getValueAt(i, 3));

            double value;
            double value2 = 0;
            if (valueText.contains(" and ")) {
                String[] parts = valueText.split(" and ");
                value = parse(parts[0]);
                value2 = parse(parts.length > 1 ? parts[1] : parts[0]);
            } else {
                value = parse(valueText);
            }
            rule.addCondition(new RuleCondition(field, operator, value, value2));
        }

        rule.setLogic(orButton.isSelected() ? SurveillanceRule.LOGIC_OR
                : SurveillanceRule.LOGIC_AND);
        rule.setSeverity(String.valueOf(severityBox.getSelectedItem()));
        rule.setDetectionType(String.valueOf(detectionBox.getSelectedItem()));
        rule.setEnabled(enabledToggle.isSelected());

        if (!updating) {
            store.addRule(rule);
        }

        refreshRules();
        selectRule(rule);
        statusLabel.setText("  \u25CF   RULE " + rule.getRuleId()
                + (updating ? " UPDATED" : " SAVED"));
        statusLabel.setForeground(DarkTheme.GREEN);
        selectedRuleLabel.setText("Editing: " + rule.getRuleId() + " - " + rule.getName());

        DarkTheme.info(this, "Rule " + (updating ? "updated" : "saved") + " successfully.\n\n"
              + "Rule ID    : " + rule.getRuleId() + "\n"
              + "Name       : " + rule.getName() + "\n"
              + "Conditions : " + rule.getConditions().size() + "\n"
              + "Logic      : " + rule.getLogic() + "\n"
              + "Severity   : " + rule.getSeverity() + "\n"
              + "Status     : " + (rule.isEnabled() ? "ENABLED" : "DISABLED"), "Rule Builder");
    }

    private double parse(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void newRule() {
        editingRule = null;
        nameField.setText("");
        conditionModel.setRowCount(0);
        fieldBox.setSelectedIndex(0);
        operatorBox.setSelectedIndex(0);
        valueSpinner.setValue(5000);
        value2Spinner.setValue(10000);
        andButton.setSelected(true);
        severityBox.setSelectedItem(SurveillanceRule.SEVERITY_HIGH);
        detectionBox.setSelectedIndex(0);
        enabledToggle.setSelected(true);
        alertOnMatchCheck.setSelected(true);
        notifyCheck.setSelected(false);
        selectedRuleLabel.setText("Editing: new rule");
        statusLabel.setText("  \u25CF   NEW RULE");
        statusLabel.setForeground(DarkTheme.ACCENT);
        updatePreview();
    }

    private SurveillanceRule getSelectedRule() {
        int viewRow = rulesTable.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = rulesTable.convertRowIndexToModel(viewRow);
        String id = String.valueOf(rulesModel.getValueAt(modelRow, COL_ID));
        for (SurveillanceRule rule : store.getRules()) {
            if (rule.getRuleId().equals(id)) {
                return rule;
            }
        }
        return null;
    }

    private void loadSelectedRule() {
        SurveillanceRule rule = getSelectedRule();
        if (rule == null) {
            DarkTheme.info(this, "Select a rule in the table first.", "Rule Builder");
            return;
        }

        editingRule = rule;
        nameField.setText(rule.getName());
        detectionBox.setSelectedItem(rule.getDetectionType());
        severityBox.setSelectedItem(rule.getSeverity());
        enabledToggle.setSelected(rule.isEnabled());
        if (SurveillanceRule.LOGIC_OR.equals(rule.getLogic())) {
            orButton.setSelected(true);
        } else {
            andButton.setSelected(true);
        }

        conditionModel.setRowCount(0);
        for (RuleCondition condition : rule.getConditions()) {
            conditionModel.addRow(new Object[] {
                conditionModel.getRowCount() + 1,
                condition.getField(),
                condition.getOperator(),
                condition.getValueText()
            });
        }

        selectedRuleLabel.setText("Editing: " + rule.getRuleId() + " - " + rule.getName());
        statusLabel.setText("  \u25CF   RULE " + rule.getRuleId() + " LOADED");
        statusLabel.setForeground(DarkTheme.ACCENT);
        updatePreview();
    }

    private void toggleSelectedRule() {
        SurveillanceRule rule = getSelectedRule();
        if (rule == null) {
            DarkTheme.info(this, "Select a rule in the table first.", "Rule Builder");
            return;
        }
        rule.setEnabled(!rule.isEnabled());
        refreshRules();
        selectRule(rule);
        statusLabel.setText("  \u25CF   RULE " + rule.getRuleId()
                + (rule.isEnabled() ? " ENABLED" : " DISABLED"));
        statusLabel.setForeground(rule.isEnabled() ? DarkTheme.GREEN : DarkTheme.AMBER);
    }

    private void duplicateSelectedRule() {
        SurveillanceRule rule = getSelectedRule();
        if (rule == null) {
            DarkTheme.info(this, "Select a rule in the table first.", "Rule Builder");
            return;
        }

        SurveillanceRule copy = new SurveillanceRule(store.nextRuleId(),
                rule.getName() + " (copy)", System.currentTimeMillis());
        for (RuleCondition condition : rule.getConditions()) {
            copy.addCondition(new RuleCondition(condition.getField(), condition.getOperator(),
                    condition.getValue(), condition.getValue2()));
        }
        copy.setLogic(rule.getLogic());
        copy.setSeverity(rule.getSeverity());
        copy.setDetectionType(rule.getDetectionType());
        copy.setEnabled(rule.isEnabled());

        store.addRule(copy);
        refreshRules();
        selectRule(copy);
        statusLabel.setText("  \u25CF   RULE DUPLICATED AS " + copy.getRuleId());
        statusLabel.setForeground(DarkTheme.GREEN);
    }

    private void deleteSelectedRule() {
        SurveillanceRule rule = getSelectedRule();
        if (rule == null) {
            DarkTheme.info(this, "Select a rule in the table first.", "Rule Builder");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Delete rule " + rule.getRuleId() + "?\n\n"
              + "Name       : " + rule.getName() + "\n"
              + "Conditions : " + rule.getConditions().size() + "\n\n"
              + "This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        store.getRules().remove(rule);
        if (editingRule == rule) {
            editingRule = null;
        }
        refreshRules();
        selectedRuleLabel.setText("No rule loaded");
        statusLabel.setText("  \u25CF   RULE " + rule.getRuleId() + " DELETED");
        statusLabel.setForeground(DarkTheme.RED);
    }

    private void exportRules() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rule ID,Name,Conditions,Logic,Severity,Enabled,Matches,Created\n");
        for (SurveillanceRule rule : store.getRules()) {
            sb.append(rule.getRuleId()).append(',')
              .append('"').append(rule.getName()).append('"').append(',')
              .append(rule.getConditions().size()).append(',')
              .append(rule.getLogic()).append(',')
              .append(rule.getSeverity()).append(',')
              .append(rule.isEnabled() ? "YES" : "NO").append(',')
              .append(rule.getMatchCount()).append(',')
              .append(rule.getDateText()).append('\n');
        }

        JTextArea area = new JTextArea(sb.toString(), 14, 76);
        area.setFont(DarkTheme.FONT_MONO);
        area.setEditable(false);
        DarkTheme.info(this, DarkTheme.scroll(area), "Export preview - " + store.getRuleCount() + " rule(s) (simulated)");
    }

    private void selectRule(SurveillanceRule rule) {
        for (int i = 0; i < rulesModel.getRowCount(); i++) {
            if (rule.getRuleId().equals(rulesModel.getValueAt(i, COL_ID))) {
                rulesTable.setRowSelectionInterval(i, i);
                break;
            }
        }
    }

    private void showSelectedRuleSummary() {
        SurveillanceRule rule = getSelectedRule();
        if (rule == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SELECTED RULE\n");
        sb.append("--------------------------------------\n");
        sb.append(String.format("ID         : %s%n", rule.getRuleId()));
        sb.append(String.format("Name       : %s%n", rule.getName()));
        sb.append(String.format("Detection  : %s%n", rule.getDetectionType()));
        sb.append(String.format("Severity   : %s%n", rule.getSeverity()));
        sb.append(String.format("Logic      : %s%n", rule.getLogic()));
        sb.append(String.format("Created    : %s%n", DateTimeUtil.date(rule.getCreatedAt())));
        sb.append(String.format("Matches    : %d%n", rule.getMatchCount()));
        sb.append(String.format("Enabled    : %s%n", rule.isEnabled() ? "YES" : "NO"));
        sb.append("--------------------------------------\n");
        sb.append("CONDITIONS\n");
        sb.append("--------------------------------------\n");
        if (rule.getConditions().isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (int i = 0; i < rule.getConditions().size(); i++) {
                if (i > 0) {
                    sb.append(String.format("%-10s%n", rule.getLogic()));
                }
                sb.append(String.format("%d. %s%n", i + 1,
                        rule.getConditions().get(i).getConditionText()));
            }
        }
        sb.append("--------------------------------------\n");
        sb.append("SUMMARY\n");
        sb.append(rule.getSummary()).append('\n');

        summaryArea.setText(sb.toString());
        summaryArea.setCaretPosition(0);
        selectedRuleLabel.setText("Selected: " + rule.getRuleId() + " - " + rule.getName());
    }

    public void refreshView() {
        refreshRules();
        updatePreview();
    }

    private void refreshRules() {
        rulesModel.setRowCount(0);
        for (SurveillanceRule rule : store.getRules()) {
            rulesModel.addRow(new Object[] {
                rule.getRuleId(),
                rule.getName(),
                rule.getConditionCountText(),
                rule.getLogic(),
                rule.getSeverity(),
                rule.isEnabled() ? "YES" : "NO",
                rule.getMatchCount(),
                rule.getDateText()
            });
        }
        countLabel.setText(store.getRuleCount() + " rules");
        enabledLabel.setText(store.getEnabledRuleCount() + " enabled");
    }

    private static class SeverityRenderer extends DefaultTableCellRenderer {
        SeverityRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_SMALL);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String severity = String.valueOf(value);
                c.setForeground("CRITICAL".equals(severity) ? DarkTheme.RED
                        : "HIGH".equals(severity) ? DarkTheme.AMBER
                        : "MEDIUM".equals(severity) ? DarkTheme.PURPLE : DarkTheme.GREEN);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }

    private static class EnabledRenderer extends DefaultTableCellRenderer {
        EnabledRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(DarkTheme.FONT_BOLD);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground("YES".equals(String.valueOf(value))
                        ? DarkTheme.GREEN : DarkTheme.TEXT_MUTED);
                c.setBackground(row % 2 == 0 ? DarkTheme.SURFACE : DarkTheme.SURFACE_ALT);
            }
            return c;
        }
    }
}
