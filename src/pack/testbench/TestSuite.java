package pack.testbench;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.GreedyRouter;
import pack.algorithms.NotDropletAwareGreedyRouter;
import pack.algorithms.Router;
import pack.algorithms.RoutingResult;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.MixingPercentages;
import pack.helpers.LogMode;
import pack.helpers.Logger;
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
import pack.testbench.tests.PlatformAssay1;
import pack.testbench.tests.PlatformAssay2;
import pack.testbench.tests.PlatformAssay3;
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
  
  private List<BioArray> arrays;
  private List<BioAssay> assays;
  
  private MixingPercentages percentages;
  
  private List<Router> routers;
  private List<TestResult> testResults;
  
  public TestSuite() {
    testResults = new ArrayList<>();
    
    arrays = new ArrayList<>();
    assays = new ArrayList<>();

    routers = new ArrayList<>();
    
    percentages = new DefaultMixingPercentages();
    
    Logger.mode = LogMode.Silent;

    register(new NotDropletAwareGreedyRouter());
    register(new GreedyRouter());
    
    registerAllTests();
  }

  public void run() {
    for (Router router : routers) {
      printHeader(router);
      runTests(router);
      printSummary();

      testResults.clear();
    }
  }

  private void printHeader(Router router) {
    String routerName = router.getClass().getSimpleName();
    System.out.printf("=== %s ===\n", routerName);
  }
  
  private void runTests(Router router) {

    for (int i = 0; i < arrays.size(); i++) {
      BioArray array = arrays.get(i);
      BioAssay assay = assays.get(i);
      
      String assayName = assay.getClass().getSimpleName();
      String testName = assayName.replaceAll("(BioAssay)|(Assay)", "");
      
      System.out.printf("running %s", testName);
      
      long start = System.currentTimeMillis();
      RoutingResult result = router.compute(assay, array, percentages);
      long msElapsed = System.currentTimeMillis() - start;
      
      System.out.printf(" ... ");

      if (result.completed) {
        System.out.printf("ok");
      } else {
        System.out.printf("failed");
      }
      
      System.out.printf("\n");

      int id = i + 1;
      
      TestResult testResult = new TestResult();
      testResult.completed = result.completed;
      testResult.id = id;
      testResult.executionTime = result.executionTime;
      testResult.runningTime = msElapsed / 1000f;
      
      testResults.add(testResult);
    }
    
    System.out.printf("completed all tests.\n\n");
  }
  
  public void printSummary() {
    for (int i = 0; i < testResults.size(); i++) {
      TestResult result = testResults.get(i);
     
      String message;
      if (result.completed) {
        message = String.format("test %d found a solution with %d steps and took %.3f secs to compute.\n", result.id, result.executionTime, result.runningTime);
      } else {
        message = String.format("test %d failed.\n", result.id);
      }
      
      System.out.printf(message);
    }
    
    int completedCount = 0;
    int avgStepSize = 0;
    
    for (int i = 0; i < testResults.size(); i++) {
      TestResult result = testResults.get(i);
      if (result.completed) {
        completedCount += 1;
        avgStepSize += result.executionTime;
      }
    }
    
    System.out.printf("\n%d/%d routes succeeded!\n", completedCount, testResults.size());
    System.out.printf("avg. steps: %d\n", avgStepSize / completedCount);
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
  }
  
  private void register(BioAssay assay, BioArray array) {
    arrays.add(array);
    assays.add(assay);
  }
  
  private void register(Router router) {
    routers.add(router);
  }

  static private class TestResult {
    public int id;
    public boolean completed;
    public int executionTime;
    public float runningTime;
  }
}

