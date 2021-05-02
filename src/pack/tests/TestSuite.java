package pack.tests;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.BioArray;
import pack.algorithms.BioAssay;
import pack.algorithms.GreedyRouter;
import pack.algorithms.LogMode;
import pack.algorithms.Logger;
import pack.algorithms.RoutingResult;
import pack.algorithms.components.DefaultMixingPercentages;
import pack.algorithms.components.MixingPercentages;

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
      int id = i + 1;
      
      System.out.printf("running test %d", id);
      
      BioArray array = arrays.get(i);
      BioAssay assay = assays.get(i);
      
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
      System.out.printf("test %d fould a solution with %d steps and took %.3f secs to compute.\n", result.id, result.executionTime, result.runningTime);
    }
  }

  private void registerAllTests() {
    register(new Test1BioAssay(), new Test1BioArray());
    register(new Test2BioAssay(), new Test2BioArray());
    register(new Test3BioAssay(), new Test3BioArray());
    register(new PCRMixingTreeAssay(), new Test3BioArray());
  }
  
  private void register(BioAssay assay, BioArray array) {
    arrays.add(array);
    assays.add(assay);
  }
}

class TestResult {
  public int id;
  public int executionTime;
  public float runningTime;
}
