package pack.testbench;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.DropletSizeAwareGreedyRouter;
import pack.algorithms.GreedyRouter;
import pack.algorithms.Router;
import pack.algorithms.RoutingResult;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.MixingPercentages;
import pack.helpers.LogMode;
import pack.helpers.Logger;
import pack.helpers.RandomUtil;
import pack.testbench.tests.BlockingDispenserTestBioArray;
import pack.testbench.tests.BlockingDispenserTestBioAssay;
import pack.testbench.tests.CrowdedModuleBioArray;
import pack.testbench.tests.CrowdedModuleBioAssay;
import pack.testbench.tests.ModuleBioArray1;
import pack.testbench.tests.ModuleBioArray2;
import pack.testbench.tests.ModuleBioArray3;
import pack.testbench.tests.ModuleBioArray4;
import pack.testbench.tests.ModuleBioAssay1;
import pack.testbench.tests.ModuleBioAssay2;
import pack.testbench.tests.ModuleBioAssay3;
import pack.testbench.tests.ModuleBioAssay4;
import pack.testbench.tests.PCRMixingTreeArray;
import pack.testbench.tests.PCRMixingTreeAssay;
import pack.testbench.tests.PlatformArray1;
import pack.testbench.tests.PlatformArray2;
import pack.testbench.tests.PlatformArray3;
import pack.testbench.tests.PlatformArray4;
import pack.testbench.tests.PlatformAssay1;
import pack.testbench.tests.PlatformAssay2;
import pack.testbench.tests.PlatformAssay3;
import pack.testbench.tests.PlatformAssay4;
import pack.testbench.tests.Test1BioArray;
import pack.testbench.tests.Test1BioAssay;
import pack.testbench.tests.Test2BioArray;
import pack.testbench.tests.Test2BioAssay;
import pack.testbench.tests.Test3BioArray;
import pack.testbench.tests.Test3BioAssay;
import pack.testbench.tests.Test4BioArray;
import pack.testbench.tests.Test4BioAssay;
import pack.testbench.tests.functionality.DisposeArray1;
import pack.testbench.tests.functionality.DisposeAssay1;
import pack.testbench.tests.functionality.MixArray1;
import pack.testbench.tests.functionality.MixAssay1;

public class TestSuite {
  
  private boolean verbose = false;  // verbose or recap mode

  private int runs = 2;
  private int recapSeedPrintInterval = runs / 10;

  private int seed;

  private List<BioArray> arrays;
  private List<BioAssay> assays;
  
  private MixingPercentages percentages;
  
  private List<Statistics> statistics;
  
  private List<Router> routers;
  private List<TestResult> testResults;
  
  public TestSuite() {
    testResults = new ArrayList<>();
    statistics = new ArrayList<>();
    
    arrays = new ArrayList<>();
    assays = new ArrayList<>();

    routers = new ArrayList<>();
    
    percentages = new DefaultMixingPercentages();
    
    
    Logger.mode = LogMode.Silent;

    registerAllTests();
  }

  public void runAllRouters() {
    register(new GreedyRouter());
    register(new DropletSizeAwareGreedyRouter());
    
    for (Router router : routers) {
      seed = 0;
      
      printHeader(router);

      for (int i = 0; i < runs; i++) {
        printSeed();
        
        run(router);
        
        seed += 1;
      }

      printSummary();

      // @TODO: write the content to a file and create some graphs with python
      testResults.clear();
    }
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

    for (int i = 0; i < arrays.size(); i++) {
      RandomUtil.init(seed);  // reset the seed after each tests, so we can reproduce the result of the test in the gui.
      
      BioArray array = arrays.get(i);
      BioAssay assay = assays.get(i);
      
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
      
      testResults.add(testResult);
    }
    
    if (verbose) System.out.printf("completed all tests.\n");
  }
  
  public void printSummary() {
    
    System.out.printf("\n");
    
    if (verbose) {
      for (int i = 0; i < testResults.size(); i++) {
        TestResult result = testResults.get(i);
       
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

    for (int i = 0; i < arrays.size(); i++) {
      TestResult result = testResults.get(i);

      Statistics stat = new Statistics();
      stat.name = result.name;
      statistics.add(stat);
    }
    
    for (int i = 0; i < testResults.size(); i++) {
      TestResult result = testResults.get(i);
      
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
    System.out.printf("%d/%d routes succeeded!", cumulated.completedCount, testResults.size());
    System.out.printf(" avg. steps: %d, ", cumulated.executionTime / cumulated.completedCount);
    System.out.printf("took %.3f secs to compute.", cumulated.runningTime / cumulated.completedCount);
    System.out.printf("\n");
  }

  private void registerAllTests() {
    // functional tests
    register(new DisposeAssay1(), new DisposeArray1());
    register(new MixAssay1(), new MixArray1());
    
    // tests
    register(new Test1BioAssay(), new Test1BioArray());
    register(new Test2BioAssay(), new Test2BioArray());
    register(new Test3BioAssay(), new Test3BioArray());
    register(new Test4BioAssay(), new Test4BioArray());
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
    arrays.add(array);
    assays.add(assay);
  }
  
  private void register(Router router) {
    routers.add(router);
  }

  static private class TestResult {
    public String name;
    public int id;
    public int seed;
    public boolean completed;
    public int executionTime;
    public float runningTime;
  }
  
  static private class Statistics {
    public String name;
    public int completedCount;
    public int failedCount;
    public int executionTime;
    public float runningTime;
  }
}

