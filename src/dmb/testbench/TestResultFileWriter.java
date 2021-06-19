package dmb.testbench;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dmb.algorithms.Router;
import dmb.helpers.IOUtils;

public class TestResultFileWriter {
  
  public void writeTestResultsFromSingleTestAndRouter(List<TestResult> testSeeds) {
    StringBuilder content = new StringBuilder();
    for (TestResult test : testSeeds) {
      String line = String.format("%d %b %d %.3f", test.seed, test.completed, test.executionTime, test.runningTime);

      content.append(line);
      content.append("\n");
    }

    TestResult result = testSeeds.get(0);
    
    String testName = result.name;
    String routerName = result.router.getClass().getSimpleName();

    String filename = String.format("%s-%s", routerName, testName);
    
    String file = String.format("./test/%s.txt", filename);
    
    IOUtils.writeStringToFile(content.toString(), file);
  }
  
  public void writeAll(List<TestResult> tests) {
    List<TestResult> all = new ArrayList<>(tests);
    
    while (all.size() > 0) {
      TestResult current = all.get(0);
      
      List<TestResult> testSeeds = extract(current.router, current.name, all);
      all.removeAll(testSeeds);
      
      writeTestResultsFromSingleTestAndRouter(testSeeds);
    }
    
  }
  
  public List<TestResult> extract(Router router, String testName, List<TestResult> tests) {
    List<TestResult> extracted = new ArrayList<>(tests);
    
    for (Iterator<TestResult> it = extracted.iterator(); it.hasNext();) {
      TestResult result = it.next();
    
      if (!result.router.equals(router)) it.remove();
      else if (!result.name.equals(testName)) it.remove();
    }
    
    return extracted;
  }

}
