package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.LargeStrictPolicyModuleCatalog;

public class ModuleBioArray3 extends BioArray {
  
  public ModuleBioArray3() {
    width = 14;
    height = 14;
    
    catalog = new LargeStrictPolicyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    reservoirTiles.add(new Point(8, 5));
    reservoirTiles.add(new Point(width - 1, 0));
    reservoirTiles.add(new Point(10, 7));
    reservoirTiles.add(new Point(8, 7));
    
    
  }
}
