package dmb.testbench;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmb.algorithms.DropletSizeAwareGreedyRouter;
import dmb.algorithms.GreedyRouter;
import dmb.algorithms.Operation;
import dmb.algorithms.Router;
import dmb.algorithms.RoutingResult;
import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.mixingpercentages.DefaultMixingPercentages;
import dmb.components.mixingpercentages.MixingPercentages;
import dmb.helpers.LogMode;
import dmb.helpers.Logger;
import dmb.helpers.RandomUtil;
import dmb.testbench.bundles.BenchmarkTestBundle;
import dmb.testbench.bundles.FunctionalTestBundle;
import dmb.testbench.bundles.TestBundle;

/**
 * Runs a bundle of tests for the routing algorithms using N different seeds. 
 * The results of the tests can optionally be written to text files.
 */

public class TestSuite {

  private boolean writeToFile = false;

  private int runs = 1000;
  private int recapSeedPrintInterval = runs / 10;

  private int seed;

  private List<Test> tests;

  private TestResultFileWriter writer;

  private MixingPercentages percentages;
  private Router greedyRouter;
  private Router dropletSizeAwareGreedyRouter;

  private List<Statistics> statistics;

  private List<Router> routers;
  private List<TestResult> routeTestResults;
  private List<TestResult> allTestResults;

  public TestSuite() {
    percentages = new DefaultMixingPercentages();

    routeTestResults = new ArrayList<>();
    allTestResults = new ArrayList<>();

    statistics = new ArrayList<>();

    writer = new TestResultFileWriter();

    tests = new ArrayList<>();
    routers = new ArrayList<>();

    Logger.mode = LogMode.Silent;

    greedyRouter = new GreedyRouter();
    dropletSizeAwareGreedyRouter = new DropletSizeAwareGreedyRouter();

    routers.add(greedyRouter);
    routers.add(dropletSizeAwareGreedyRouter);
  }

  public void runAllRoutersWithBenchmarkTests() {
    TestBundle bundle = new BenchmarkTestBundle();
    tests.addAll(bundle.get());

    for (Router router : routers) {
      seed = 0;

      printHeader(router);

      for (int i = 0; i < runs; i++) {
        printSeed();

        run(router);

        seed += 1;
      }

      printSummary();

      allTestResults.addAll(routeTestResults);
      routeTestResults.clear();
    }

    printAverageOperationExecutionTimes();

    if (writeToFile) writer.writeAll(allTestResults);
    tests.clear();
  }

  public void runAllRoutersWithOperationalTests() {
    TestBundle bundle = new FunctionalTestBundle();
    tests.addAll(bundle.get());

    for (Router router : routers) {
      seed = 0;

      printHeader(router);

      for (int i = 0; i < runs; i++) {
        printSeed();

        run(router);

        seed += 1;
      }

      printSummary();

      routeTestResults.clear();
    }

    tests.clear();
  }

  private void printSeed() {
    if (recapSeedPrintInterval > 0 && seed % recapSeedPrintInterval == 0) {
      System.out.printf("seed %d\n", seed);
    }
  }

  private void printHeader(Router router) {
    String routerName = router.getClass().getSimpleName();
    System.out.printf("=== %s ===\n", routerName);
  }

  private void run(Router router) {

    for (int i = 0; i < tests.size(); i++) {
      RandomUtil.init(seed); // reset the seed after each tests, so we can reproduce the result of the test in the gui.

      Test test = tests.get(i);

      BioArray array = test.array;
      BioAssay assay = test.assay;

      String assayName = assay.getClass().getSimpleName();
      String testName = assayName.replaceAll("(BioAssay)|(Assay)", "");

      long start = System.currentTimeMillis();
      RoutingResult result = router.compute(assay, array, percentages);
      long msElapsed = System.currentTimeMillis() - start;

      if (!result.completed) System.out.printf("%s using seed %d failed\n", testName, seed);

      TestResult testResult = new TestResult();
      testResult.name = testName;
      testResult.id = i;
      testResult.completed = result.completed;
      testResult.executionTime = result.executionTime;
      testResult.compileTime = msElapsed / 1000f;
      testResult.seed = seed;
      testResult.router = router;
      testResult.test = test;

      routeTestResults.add(testResult);
    }
  }

  public void printAverageOperationExecutionTimes() {
    class Average {
      public int total;
      public int count;
    }

    Map<String, Average> operationToAverageExecutionTime = new HashMap<>();

    for (TestResult result : allTestResults) {
      if (!result.completed) continue;
      List<Operation> operations = result.test.assay.getOperations();

      for (Operation operation : operations) {
        Average average = operationToAverageExecutionTime.get(operation.name);
        if (average == null) {
          average = new Average();
          operationToAverageExecutionTime.put(operation.name, average);
        }

        average.total += operation.getDuration();
        average.count += 1;
      }
    }

    for (Map.Entry<String, Average> entry : operationToAverageExecutionTime.entrySet()) {
      String name = entry.getKey();
      Average average = entry.getValue();

      if (average.count == 0) continue;

      int avg = Math.round(average.total / average.count);
      System.out.printf("%s - avg: %d\n", name, avg);

    }
  }

  public void printSummary() {
    Statistics cumulated = new Statistics();
    cumulated.name = "cumulated";

    for (int i = 0; i < tests.size(); i++) {
      TestResult result = routeTestResults.get(i);

      Statistics stat = new Statistics();
      stat.name = result.name;
      statistics.add(stat);
    }

    for (int i = 0; i < routeTestResults.size(); i++) {
      TestResult result = routeTestResults.get(i);

      int id = result.id;
      Statistics stat = statistics.get(id);

      if (result.completed) {
        stat.completedCount += 1;
        stat.executionTime += result.executionTime;
        stat.runningTime += result.compileTime;
      } else {
        stat.failedCount += 1;
      }
    }

    for (Statistics stat : statistics) {
      cumulated.executionTime += stat.executionTime;
      cumulated.completedCount += stat.completedCount;
      cumulated.failedCount += stat.failedCount;
      cumulated.runningTime += stat.runningTime;
    }

    for (Statistics stat : statistics) {
      System.out.printf("%s - ", stat.name);
      System.out.printf("%d/%d routes succeeded!", stat.completedCount, runs);
      System.out.printf(" avg. steps: %d", stat.executionTime / stat.completedCount);
      System.out.printf(", took %.3f secs to compute.", stat.runningTime / stat.completedCount);
      System.out.printf("\n");
    }

    statistics.clear();

    System.out.printf("\n");

    System.out.printf("%s - ", cumulated.name);
    System.out.printf("%d/%d routes succeeded!", cumulated.completedCount, routeTestResults.size());
    System.out.printf(" avg. steps: %d, ", cumulated.executionTime / cumulated.completedCount);
    System.out.printf("took on avg %.3f secs to compute.", cumulated.runningTime / cumulated.completedCount);
    System.out.printf("\n");
  }

  static private class Statistics {
    public String name;
    public int completedCount;
    public int failedCount;
    public int executionTime;
    public float runningTime;
  }
}
