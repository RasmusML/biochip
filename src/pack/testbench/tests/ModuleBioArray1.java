package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.SmallModuleCatalog;

public class ModuleBioArray1 extends BioArray {
  
  public ModuleBioArray1() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
  }
}
