package pack.tests;

import java.util.Collections;

import pack.algorithms.BioArray;
import pack.algorithms.Point;
import pack.algorithms.components.RandomUtil;
import pack.tests.catalogs.LargeSoftPolicyModuleCatalog;

public class CrowdedModuleBioArray extends BioArray {
  
  public CrowdedModuleBioArray() {
    width = 14;
    height = 14;
    
    catalog = new LargeSoftPolicyModuleCatalog();
    
    reservoirTiles.add(new Point(0, 0));
    reservoirTiles.add(new Point(width - 1, height - 1));
    
    reservoirTiles.add(new Point(6, 3));
    reservoirTiles.add(new Point(6, 5));
    reservoirTiles.add(new Point(6, 7));
    
    reservoirTiles.add(new Point(8, 3));
    reservoirTiles.add(new Point(8, 5));
    reservoirTiles.add(new Point(8, 7));
    
    reservoirTiles.add(new Point(10, 3));
    reservoirTiles.add(new Point(10, 5));
    reservoirTiles.add(new Point(10, 7));
    
    reservoirTiles.add(new Point(12, 3));
    reservoirTiles.add(new Point(12, 5));
    reservoirTiles.add(new Point(12, 7));
    
    // shuffle for now, so merge operations don't spawn next to each other. Another options to ensure this, is to move around when the different operations are created in the assay.
    Collections.shuffle(reservoirTiles, RandomUtil.get());
    
  }
}
