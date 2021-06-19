package dmb.testbench;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.DropletSizeAwareGreedyRouter;
import dmb.algorithms.GreedyRouter;
import dmb.algorithms.Router;
import dmb.algorithms.RoutingResult;
import dmb.components.input.BioArray;
import dmb.components.input.BioAssay;
import dmb.components.mixingpercentages.DefaultMixingPercentages;
import dmb.components.mixingpercentages.MixingPercentages;
import dmb.helpers.LogMode;
import dmb.helpers.Logger;
import dmb.helpers.RandomUtil;
import dmb.testbench.tests.BlockingDispenserTestBioArray;
import dmb.testbench.tests.BlockingDispenserTestBioAssay;
import dmb.testbench.tests.CrowdedModuleBioArray;
import dmb.testbench.tests.CrowdedModuleBioAssay;
import dmb.testbench.tests.ModuleBioArray1;
import dmb.testbench.tests.ModuleBioArray2;
import dmb.testbench.tests.ModuleBioArray3;
import dmb.testbench.tests.ModuleBioArray4;
import dmb.testbench.tests.ModuleBioAssay1;
import dmb.testbench.tests.ModuleBioAssay2;
import dmb.testbench.tests.ModuleBioAssay3;
import dmb.testbench.tests.ModuleBioAssay4;
import dmb.testbench.tests.PCRMixingTreeArray;
import dmb.testbench.tests.PCRMixingTreeAssay;
import dmb.testbench.tests.PlatformArray1;
import dmb.testbench.tests.PlatformArray2;
import dmb.testbench.tests.PlatformArray3;
import dmb.testbench.tests.PlatformArray4;
import dmb.testbench.tests.PlatformAssay1;
import dmb.testbench.tests.PlatformAssay2;
import dmb.testbench.tests.PlatformAssay3;
import dmb.testbench.tests.PlatformAssay4;
import dmb.testbench.tests.Test1BioArray;
import dmb.testbench.tests.Test1BioAssay;
import dmb.testbench.tests.Test2BioArray;
import dmb.testbench.tests.Test2BioAssay;
import dmb.testbench.tests.Test3BioArray;
import dmb.testbench.tests.Test3BioAssay;
import dmb.testbench.tests.Test4BioArray;
import dmb.testbench.tests.Test4BioAssay;
import dmb.testbench.tests.Test5BioArray;
import dmb.testbench.tests.Test5BioAssay;
import dmb.testbench.tests.Test6BioArray;
import dmb.testbench.tests.Test6BioAssay;
import dmb.testbench.tests.functionality.DetectorArray1;
import dmb.testbench.tests.functionality.DetectorAssay1;
import dmb.testbench.tests.functionality.DispenseArray1;
import dmb.testbench.tests.functionality.DispenseAssay1;
import dmb.testbench.tests.functionality.DisposeArray1;
import dmb.testbench.tests.functionality.DisposeAssay1;
import dmb.testbench.tests.functionality.MergeArray1;
import dmb.testbench.tests.functionality.MergeArray2;
import dmb.testbench.tests.functionality.MergeArray3;
import dmb.testbench.tests.functionality.MergeAssay1;
import dmb.testbench.tests.functionality.MergeAssay2;
import dmb.testbench.tests.functionality.MergeAssay3;
import dmb.testbench.tests.functionality.MixArray1;
import dmb.testbench.tests.functionality.MixArray2;
import dmb.testbench.tests.functionality.MixAssay1;
import dmb.testbench.tests.functionality.MixAssay2;

public class TestSuite {
  
  private boolean verbose = false;  // verbose or recap mode
  private boolean writeToFile = false;

  private int runs = 100;
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
    
    register(greedyRouter);
    register(dropletSizeAwareGreedyRouter);

    registerAllTests();
  }

  public void runAllRouters() {
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
    
    if (writeToFile) writer.writeAll(allTestResults);
  }

  private void printSeed() {
    if (verbose) {
      System.out.printf("seed %d\n", seed);
    } else {
      if (recapSeedPrintInterval > 0 && seed % recapSeedPrintInterval == 0) {
        System.out.printf("seed %d\n", seed);
      }
    }
  }

  private void printHeader(Router router) {
    String routerName = router.getClass().getSimpleName();
    System.out.printf("=== %s ===\n", routerName);
  }
  
  private void run(Router router) {

    for (int i = 0; i < tests.size(); i++) {
      RandomUtil.init(seed);  // reset the seed after each tests, so we can reproduce the result of the test in the gui.
      
      Test test = tests.get(i);
      
      BioArray array = test.array;
      BioAssay assay = test.assay;
      
      String assayName = assay.getClass().getSimpleName();
      String testName = assayName.replaceAll("(BioAssay)|(Assay)", "");
      
      if (verbose) System.out.printf("running %s ... ", testName);
      
      long start = System.currentTimeMillis();
      RoutingResult result = router.compute(assay, array, percentages);
      long msElapsed = System.currentTimeMillis() - start;
      
      if (verbose) {
        if (result.completed) System.out.printf("ok\n");
        else System.out.printf("failed\n");
      } else {
        if (!result.completed) System.out.printf("%s using seed %d failed\n", testName, seed);
      }

      TestResult testResult = new TestResult();
      testResult.name = testName;
      testResult.id = i;
      testResult.completed = result.completed;
      testResult.executionTime = result.executionTime;
      testResult.runningTime = msElapsed / 1000f;
      testResult.seed = seed;
      testResult.router = router;
      
      routeTestResults.add(testResult);
    }
    
    if (verbose) System.out.printf("completed all tests.\n\n");
  }
  
  public void printSummary() {
    
    if (verbose) {
      for (int i = 0; i < routeTestResults.size(); i++) {
        TestResult result = routeTestResults.get(i);
       
        String message;
        if (result.completed) {
          message = String.format("%s found a solution with %d steps and took %.3f secs to compute.\n", result.name, result.executionTime, result.runningTime);
        } else {
          message = String.format("%s failed.\n", result.name);
        }
        
        System.out.printf(message);
        
      }
      
      System.out.printf("\n");
    }
    
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
        stat.runningTime += result.runningTime;
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

  private void registerAllTests() {
    
    // @TODO: dont use the functional tests for statistics.
    
    // functional tests
    register(new DetectorAssay1(), new DetectorArray1());
    register(new DispenseAssay1(), new DispenseArray1());
    register(new DisposeAssay1(), new DisposeArray1());
    register(new MergeAssay1(), new MergeArray1());
    register(new MergeAssay2(), new MergeArray2());
    register(new MergeAssay3(), new MergeArray3());
    register(new MixAssay1(), new MixArray1());
    register(new MixAssay2(), new MixArray2());
    
    // tests
    register(new Test1BioAssay(), new Test1BioArray());
    register(new Test2BioAssay(), new Test2BioArray());
    register(new Test3BioAssay(), new Test3BioArray());
    register(new Test4BioAssay(), new Test4BioArray());
    register(new Test5BioAssay(), new Test5BioArray());
    register(new Test6BioAssay(), new Test6BioArray());
    register(new PCRMixingTreeAssay(), new PCRMixingTreeArray());
    register(new BlockingDispenserTestBioAssay(), new BlockingDispenserTestBioArray());
    register(new ModuleBioAssay1(), new ModuleBioArray1());
    register(new ModuleBioAssay2(), new ModuleBioArray2());
    register(new ModuleBioAssay3(), new ModuleBioArray3());
    register(new ModuleBioAssay4(), new ModuleBioArray4());
    register(new CrowdedModuleBioAssay(), new CrowdedModuleBioArray());
    register(new PlatformAssay1(), new PlatformArray1());
    register(new PlatformAssay2(), new PlatformArray2());
    register(new PlatformAssay3(), new PlatformArray3());
    register(new PlatformAssay4(), new PlatformArray4());
  }
  
  private void register(BioAssay assay, BioArray array) {
    Test test = new Test();
    test.array = array;
    test.assay = assay;
    tests.add(test);
  }
  
  private void register(Router router) {
    routers.add(router);
  }
  
  static private class Test {
    public BioArray array;
    public BioAssay assay;
  }

  static private class Statistics {
    public String name;
    public int completedCount;
    public int failedCount;
    public int executionTime;
    public float runningTime;
  }
}

