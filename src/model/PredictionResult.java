package model;

public class PredictionResult {

    private final String symbol;
    private final String predictedDirection;
    private final double predictedPrice;
    private final double confidencePercent;
    private final double abuseProbability;
    private final String riskLevel;
    private final double anomalyScore;
    private final double accuracyPercent;
    private final double precisionPercent;
    private final double recallPercent;
    private final double f1Percent;
    private final double meanAbsoluteError;
    private final int sampleSize;
    private final long evaluationTimeMs;
    private final long timestamp;

    public PredictionResult(String symbol,
                            String predictedDirection,
                            double predictedPrice,
                            double confidencePercent,
                            double abuseProbability,
                            String riskLevel,
                            double anomalyScore,
                            double accuracyPercent,
                            double precisionPercent,
                            double recallPercent,
                            double f1Percent,
                            double meanAbsoluteError,
                            int sampleSize,
                            long evaluationTimeMs,
                            long timestamp) {
        this.symbol = symbol == null ? "AAPL" : symbol;
        this.predictedDirection = predictedDirection == null ? "STABLE" : predictedDirection;
        this.predictedPrice = Math.round(predictedPrice * 100.0) / 100.0;
        this.confidencePercent = Math.max(0.0, Math.min(100.0, Math.round(confidencePercent * 10.0) / 10.0));
        this.abuseProbability = Math.max(0.0, Math.min(1.0, abuseProbability));
        this.riskLevel = riskLevel == null ? "LOW" : riskLevel;
        this.anomalyScore = Math.round(anomalyScore * 100.0) / 100.0;
        this.accuracyPercent = Math.max(0.0, Math.min(100.0, Math.round(accuracyPercent * 10.0) / 10.0));
        this.precisionPercent = Math.max(0.0, Math.min(100.0, Math.round(precisionPercent * 10.0) / 10.0));
        this.recallPercent = Math.max(0.0, Math.min(100.0, Math.round(recallPercent * 10.0) / 10.0));
        this.f1Percent = Math.max(0.0, Math.min(100.0, Math.round(f1Percent * 10.0) / 10.0));
        this.meanAbsoluteError = Math.round(meanAbsoluteError * 1000.0) / 1000.0;
        this.sampleSize = sampleSize;
        this.evaluationTimeMs = evaluationTimeMs;
        this.timestamp = timestamp;
    }

    public static PredictionResult createDefault(String symbol) {
        return new PredictionResult(
                symbol,
                "BULLISH",
                150.0,
                88.5,
                0.18,
                "LOW",
                0.85,
                94.2,
                92.8,
                91.5,
                92.1,
                0.042,
                1000,
                4,
                System.currentTimeMillis()
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getPredictedDirection() {
        return predictedDirection;
    }

    public double getPredictedPrice() {
        return predictedPrice;
    }

    public double getConfidencePercent() {
        return confidencePercent;
    }

    public double getAbuseProbability() {
        return abuseProbability;
    }

    public int getAbuseProbabilityPercent() {
        return (int) Math.round(abuseProbability * 100.0);
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public double getAccuracyPercent() {
        return accuracyPercent;
    }

    public double getPrecisionPercent() {
        return precisionPercent;
    }

    public double getRecallPercent() {
        return recallPercent;
    }

    public double getF1Percent() {
        return f1Percent;
    }

    public double getMeanAbsoluteError() {
        return meanAbsoluteError;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public long getEvaluationTimeMs() {
        return evaluationTimeMs;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
