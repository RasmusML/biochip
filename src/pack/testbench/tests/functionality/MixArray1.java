package pack.testbench.tests.functionality;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.EmptyModuleCatalog;

public class MixArray1 extends BioArray {
  
  public MixArray1() {
    width = 14;
    height = 14;
    
    catalog = new EmptyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
  }
}
