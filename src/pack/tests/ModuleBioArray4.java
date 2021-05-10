package pack.tests;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.tests.catalogs.LargeSoftPolicyModuleCatalog;

public class ModuleBioArray4 extends BioArray {
  
  public ModuleBioArray4() {
    width = 14;
    height = 14;
    
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
