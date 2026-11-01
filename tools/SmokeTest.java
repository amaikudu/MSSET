import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import gui.MainFrame;
import model.SurveillanceRule;
import service.DataStore;
import service.MarketDataService;
import service.SurveillanceService;
import util.DarkTheme;

public class SmokeTest {

    private static final AtomicInteger failures = new AtomicInteger();
    private static final AtomicReference<Throwable> firstFailure = new AtomicReference<>();

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            failures.incrementAndGet();
            firstFailure.compareAndSet(null, throwable);
            System.out.println("[FAIL] Uncaught exception on " + thread.getName() + ": " + throwable);
            throwable.printStackTrace(System.out);
        });

        DataStore store = new DataStore();
        System.out.println("DataStore created. alerts=" + store.getAlertCount()
                + " reports=" + store.getReportCount()
                + " rules=" + store.getRuleCount());
        check(store.getAlertCount() > 0, "seed alerts exist");
        check(store.getReportCount() > 0, "seed reports exist");
        check(store.getRuleCount() > 0, "seed rules exist");
        check(store.getEnabledRuleCount() > 0, "at least one rule is enabled");

        final MainFrame[] holder = new MainFrame[1];
        runOnEdt("MainFrame (all 10 modules)", () -> {
            holder[0] = new MainFrame();
            holder[0].setSize(1560, 960);
            holder[0].layoutTree();
        });

        MainFrame app = holder[0];
        if (app == null) {
            System.out.println("[FAIL] MainFrame was not created.");
            System.exit(1);
        }

        final DataStore appStore = app.getDataStore();
        final MarketDataService marketData = app.getMarketDataService();
        final SurveillanceService surveillance = app.getSurveillanceService();
        System.out.println("Frame services: symbols=" + marketData.getRows().size()
                + " AAPL=" + marketData.getPrice("AAPL")
                + " running=" + marketData.isRunning());
        check(marketData.getRows().size() == 20, "20 instruments loaded");
        check(marketData.isRunning(), "market feed is running");

        String[] modules = {
            "Dashboard", "Market Data", "Order Book", "Surveillance", "Rule Builder",
            "Alerts", "Charting & Analytics", "Audit Reports", "Replay", "Settings"
        };
        for (String module : modules) {
            final String name = module;
            runOnEdt("module: " + name, () -> {
                app.showModule(name);
                app.layoutTree();
            });
        }

        System.out.println("Running the simulated market clock for 3 seconds...");
        Thread.sleep(3000);

        runOnEdt("refresh all", () -> {
            app.showModule("Dashboard");
            app.layoutTree();
        });

        System.out.println("Ticks processed      : " + marketData.getTickCount());
        System.out.println("Alerts after run     : " + appStore.getAlertCount());
        System.out.println("Orders processed     : " + marketData.getOrderCount());
        check(marketData.getTickCount() > 0, "the market clock ticked");

        runAction("place and cancel order", () -> {
            model.LiveOrder order = marketData.placeOrder("AAPL", model.LiveOrder.SIDE_BUY,
                    marketData.getPrice("AAPL") + 5, 500);
            check(order.isWorking(), "new order is working");
            check(marketData.cancelOrder(order.getOrderId()), "order can be cancelled");
        });

        runAction("rule builder round trip", () -> {
            SurveillanceRule rule = new SurveillanceRule(appStore.nextRuleId(),
                    "Smoke Test Rule", System.currentTimeMillis());
            rule.addCondition(new SurveillanceRule.RuleCondition("Order Size", ">", 1000, 0));
            appStore.addRule(rule);
            check(appStore.getRules().contains(rule), "rule was stored");
            appStore.getRules().remove(rule);
            check(!appStore.getRules().contains(rule), "rule was removed");
        });

        runAction("reset demo data", () -> {
            appStore.resetDemoData();
            check(appStore.getAlertCount() > 0, "alerts recreated after reset");
            check(appStore.getRuleCount() > 0, "rules recreated after reset");
        });

        runAction("large dataset benchmark test", () -> {
            service.PredictorService predictor = app.getPredictorService();
            check(predictor != null, "predictor service is initialized");
            model.PredictionResult pred = predictor.getPrediction("AAPL");
            check(pred != null && pred.getAccuracyPercent() > 0.0, "predictor accuracy computed");
            service.PredictorService.BenchmarkReport report = predictor.runLargeDatasetBenchmark(10000);
            check(report.getTotalRecords() == 10000, "10k records benchmarked");
            check(report.getAccuracyPercent() > 0.0, "benchmark accuracy calculated");
            check(report.getThroughputPerSec() > 1000.0, "high-speed throughput verified");
            model.PredictionResult sim = predictor.injectSimulatedAbuse("AAPL");
            check("CRITICAL".equals(sim.getRiskLevel()), "simulated abuse sets critical risk");
            check(sim.getAnomalyScore() > 3.0, "simulated abuse anomaly flagged");
        });

        runOnEdt("shutdown", app::shutdown);

        System.out.println();
        if (failures.get() == 0) {
            System.out.println("SMOKE TEST PASSED - all 10 modules built and updated without exceptions.");
        } else {
            System.out.println("SMOKE TEST FAILED - " + failures.get()
                    + " problem(s). First: " + firstFailure.get());
            System.exit(1);
        }
    }

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("[OK]   " + description);
        } else {
            failures.incrementAndGet();
            System.out.println("[FAIL] " + description);
        }
    }

    private static void runOnEdt(String name, Runnable body) {
        try {
            SwingUtilities.invokeAndWait(body);
            System.out.println("[OK]   " + name);
        } catch (InvocationTargetException e) {
            failures.incrementAndGet();
            System.out.println("[FAIL] " + name + " threw: " + e.getCause());
            e.getCause().printStackTrace(System.out);
        } catch (Exception e) {
            failures.incrementAndGet();
            System.out.println("[FAIL] " + name + " could not run: " + e);
        }
    }

    private static void runAction(String name, Runnable action) {
        runOnEdt(name, action);
    }
}
