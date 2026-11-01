package model;

public class PredictorParameters {

    public static final String[] ALGORITHMS = {
        "Ensemble Statistical (Recommended)",
        "Exponential Trend Predictor",
        "Weighted Logistic Risk Model",
        "Z-Score Anomaly Classifier"
    };

    private int lookbackWindow;
    private double learningRateAlpha;
    private double riskThresholdPercent;
    private double anomalyZScoreCutoff;
    private double volumeWeight;
    private double cancellationWeight;
    private double volatilityWeight;
    private String algorithm;
    private int benchmarkDatasetSize;

    public PredictorParameters() {
        this.lookbackWindow = 60;
        this.learningRateAlpha = 0.20;
        this.riskThresholdPercent = 65.0;
        this.anomalyZScoreCutoff = 2.5;
        this.volumeWeight = 0.35;
        this.cancellationWeight = 0.40;
        this.volatilityWeight = 0.25;
        this.algorithm = ALGORITHMS[0];
        this.benchmarkDatasetSize = 10000;
    }

    public int getLookbackWindow() {
        return lookbackWindow;
    }

    public void setLookbackWindow(int lookbackWindow) {
        this.lookbackWindow = Math.max(5, Math.min(2000, lookbackWindow));
    }

    public double getLearningRateAlpha() {
        return learningRateAlpha;
    }

    public void setLearningRateAlpha(double learningRateAlpha) {
        this.learningRateAlpha = Math.max(0.01, Math.min(1.0, learningRateAlpha));
    }

    public double getRiskThresholdPercent() {
        return riskThresholdPercent;
    }

    public void setRiskThresholdPercent(double riskThresholdPercent) {
        this.riskThresholdPercent = Math.max(5.0, Math.min(95.0, riskThresholdPercent));
    }

    public double getAnomalyZScoreCutoff() {
        return anomalyZScoreCutoff;
    }

    public void setAnomalyZScoreCutoff(double anomalyZScoreCutoff) {
        this.anomalyZScoreCutoff = Math.max(1.0, Math.min(5.0, anomalyZScoreCutoff));
    }

    public double getVolumeWeight() {
        return volumeWeight;
    }

    public void setVolumeWeight(double volumeWeight) {
        this.volumeWeight = Math.max(0.0, Math.min(1.0, volumeWeight));
    }

    public double getCancellationWeight() {
        return cancellationWeight;
    }

    public void setCancellationWeight(double cancellationWeight) {
        this.cancellationWeight = Math.max(0.0, Math.min(1.0, cancellationWeight));
    }

    public double getVolatilityWeight() {
        return volatilityWeight;
    }

    public void setVolatilityWeight(double volatilityWeight) {
        this.volatilityWeight = Math.max(0.0, Math.min(1.0, volatilityWeight));
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        if (algorithm != null) {
            this.algorithm = algorithm;
        }
    }

    public int getBenchmarkDatasetSize() {
        return benchmarkDatasetSize;
    }

    public void setBenchmarkDatasetSize(int benchmarkDatasetSize) {
        this.benchmarkDatasetSize = Math.max(1000, Math.min(500000, benchmarkDatasetSize));
    }
}
