package model;

import java.util.ArrayList;
import java.util.List;

import util.DateTimeUtil;

public class SurveillanceRule {

    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    public static final String LOGIC_AND = "AND";
    public static final String LOGIC_OR = "OR";

    public static final String[] LOGIC_OPTIONS = { LOGIC_AND, LOGIC_OR };

    public static final String[] FIELDS = {
        "Order Size",
        "Order Lifetime (ms)",
        "Cancellation Ratio (%)",
        "Message Rate (per sec)",
        "Price Deviation (%)",
        "Trade Volume",
        "Orders per Second",
        "Quote Updates"
    };

    public static final String[] OPERATORS = {
        ">", ">=", "<", "<=", "=", "!=", "between"
    };

    public static final String[] SEVERITIES = {
        SEVERITY_LOW, SEVERITY_MEDIUM, SEVERITY_HIGH, SEVERITY_CRITICAL
    };

    public static class RuleCondition {

        private String field;
        private String operator;
        private double value;
        private double value2;

        public RuleCondition(String field, String operator, double value, double value2) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.value2 = value2;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double getValue2() {
            return value2;
        }

        public void setValue2(double value2) {
            this.value2 = value2;
        }

        public boolean isRange() {
            return "between".equals(operator);
        }

        public String getValueText() {
            if (isRange()) {
                return trim(value) + " and " + trim(value2);
            }
            return trim(value);
        }

        public String getConditionText() {
            return field + " " + operator + " " + getValueText();
        }

        private static String trim(double number) {
            if (number == Math.floor(number) && !Double.isInfinite(number)) {
                return String.valueOf((long) number);
            }
            return String.format("%.2f", number);
        }

        @Override
        public String toString() {
            return getConditionText();
        }
    }

    private final String ruleId;
    private String name;
    private final List<RuleCondition> conditions = new ArrayList<>();
    private String logic = LOGIC_AND;
    private String severity = SEVERITY_HIGH;
    private String detectionType = "Spoofing / Layering";
    private boolean enabled = true;
    private final long createdAt;
    private int matchCount;

    public SurveillanceRule(String ruleId, String name, long createdAt) {
        this.ruleId = ruleId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RuleCondition> getConditions() {
        return conditions;
    }

    public void addCondition(RuleCondition condition) {
        if (condition != null) {
            conditions.add(condition);
        }
    }

    public void removeCondition(int index) {
        if (index >= 0 && index < conditions.size()) {
            conditions.remove(index);
        }
    }

    public void clearConditions() {
        conditions.clear();
    }

    public String getLogic() {
        return logic;
    }

    public void setLogic(String logic) {
        if (LOGIC_AND.equals(logic) || LOGIC_OR.equals(logic)) {
            this.logic = logic;
        }
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        if (severity != null) {
            this.severity = severity;
        }
    }

    public String getDetectionType() {
        return detectionType;
    }

    public void setDetectionType(String detectionType) {
        if (detectionType != null) {
            this.detectionType = detectionType;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public String getDateText() {
        return DateTimeUtil.date(createdAt);
    }

    public String getConditionCountText() {
        return conditions.size() + (conditions.size() == 1 ? " condition" : " conditions");
    }

    public String getSummary() {
        if (conditions.isEmpty()) {
            return "No conditions defined";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conditions.size(); i++) {
            if (i > 0) {
                sb.append(" ").append(logic).append(" ");
            }
            sb.append(conditions.get(i).getConditionText());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return ruleId + " - " + name;
    }
}
