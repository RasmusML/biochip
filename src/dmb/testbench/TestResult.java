package dmb.testbench;

import dmb.algorithms.Router;

public class TestResult {
  public String name;
  public int id;
  public int seed;
  public boolean completed;

  public float compileTime;
  public int executionTime;
 
  public Router router;
  public Test test;
}
