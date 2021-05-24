package pack.tests;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.GreedyRouter;
import pack.algorithms.RoutingResult;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.MixingPercentages;
import pack.helpers.LogMode;
import pack.helpers.Logger;

public class TestSuite {
  
  private List<BioArray> arrays;
  private List<BioAssay> assays;
  
  private MixingPercentages percentages;
  
  private GreedyRouter router;
  
  private List<TestResult> testResults;
  
  public TestSuite() {
    testResults = new ArrayList<>();
    
    arrays = new ArrayList<>();
    assays = new ArrayList<>();
    
    router = new GreedyRouter();
    percentages = new DefaultMixingPercentages();
    
    Logger.mode = LogMode.Silent;

    registerAllTests();
  }

  public void runTests() {
    for (int i = 0; i < arrays.size(); i++) {
      BioArray array = arrays.get(i);
      BioAssay assay = assays.get(i);
      
      int id = i + 1;
      System.out.printf("running test %d", id);
      
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
    
  }

  private void registerAllTests() {
    register(new Test1BioAssay(), new Test1BioArray());
    register(new Test2BioAssay(), new Test2BioArray());
    register(new Test3BioAssay(), new Test3BioArray());
    register(new PCRMixingTreeAssay(), new PCRMixingTreeArray());
    register(new BlockingDispenserTestBioAssay(), new BlockingDispenserTestBioArray());
    register(new ModuleBioAssay1(), new ModuleBioArray1());
    register(new ModuleBioAssay2(), new ModuleBioArray2());
    register(new ModuleBioAssay3(), new ModuleBioArray3());
    register(new ModuleBioAssay4(), new ModuleBioArray4());
    register(new CrowdedModuleBioAssay(), new CrowdedModuleBioArray());
    register(new ParallelMixingAssay(), new ParallelMixingArray());
    //register(new DisposeBioAssay(), new DisposeBioArray());
  }
  
  private void register(BioAssay assay, BioArray array) {
    arrays.add(array);
    assays.add(assay);
  }
}

class TestResult {
  public int id;
  public boolean completed;
  public int executionTime;
  public float runningTime;
}
