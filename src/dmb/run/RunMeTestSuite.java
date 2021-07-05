package dmb.run;

import dmb.testbench.TestSuite;

public class RunMeTestSuite {

  public static void main(String[] args) {
    TestSuite testSuite = new TestSuite();

    testSuite.runAllRoutersWithOperationalTests();
    testSuite.runAllRoutersWithBenchmarkTests();
  }
}
