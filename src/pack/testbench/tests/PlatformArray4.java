package pack.testbench.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.testbench.catalogs.LargeSoftPolicyModuleCatalog;

public class PlatformArray4 extends BioArray {
  
  public PlatformArray4() {
    width = 32;
    height = 20;
    
    catalog = new LargeSoftPolicyModuleCatalog();
    
    reservoirTiles.add(new Point(6, 3));
    reservoirTiles.add(new Point(6, 5));
    reservoirTiles.add(new Point(6, 7));
    reservoirTiles.add(new Point(8, 7));
    reservoirTiles.add(new Point(10, 5));
    reservoirTiles.add(new Point(10, 7));
    reservoirTiles.add(new Point(8, 5));
    reservoirTiles.add(new Point(10, 3));
    reservoirTiles.add(new Point(12, 7));
    reservoirTiles.add(new Point(8, 3));
    reservoirTiles.add(new Point(10, 9));
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    reservoirTiles.add(new Point(width - 1, 0));
    reservoirTiles.add(new Point(20, 14));
    
    wasteTiles.add(new Point(0, height - 1));
  }
}
