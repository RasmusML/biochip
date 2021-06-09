package pack.testbench.tests.functionality;

import pack.algorithms.BioArray;
import pack.algorithms.ModuleCatalog;

public class DetectorArray1 extends BioArray {
  
  public DetectorArray1() {
    width = 14;
    height = 14;
    
    catalog = new ModuleCatalog();
    catalog.registerDetector(5, 5, 1, 1, 20, "metal");
    
    catalog.registerDispenser(0, 0, 1);
  }
}
