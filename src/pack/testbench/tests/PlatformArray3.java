package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.LargeSoftPolicyModuleCatalog;

public class PlatformArray3 extends BioArray {
  
  public PlatformArray3() {
    width = 32;
    height = 20;
    
    catalog = new LargeSoftPolicyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    reservoirTiles.add(new Point(8, 7));
    reservoirTiles.add(new Point(width - 1, 0));
    reservoirTiles.add(new Point(10, 7));
    reservoirTiles.add(new Point(8, 7));
    reservoirTiles.add(new Point(4, 4));  // unused as enough reservoirs are available.
  }
}
