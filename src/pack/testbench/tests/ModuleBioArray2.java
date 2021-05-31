package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.SmallModuleCatalog;

public class ModuleBioArray2 extends BioArray {
  
  public ModuleBioArray2() {
    width = 7;
    height = 7;
    
    catalog = new SmallModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    reservoirTiles.add(new Point(3, 1));
  }
}
