package dmb.testbench.tests;

import dmb.algorithms.BioArray;
import dmb.testbench.catalogs.LargeSoftPolicyModuleCatalog;

public class CrowdedModuleBioArray extends BioArray {
  
  public CrowdedModuleBioArray() {
    width = 14;
    height = 14;
    
    catalog = new LargeSoftPolicyModuleCatalog();
    
    catalog.registerDispenser(0, 0, 1);
    catalog.registerDispenser(width - 1, height - 1, 1);
    
    catalog.registerDispenser(6, 3, 1);
    catalog.registerDispenser(6, 5, 1);
    catalog.registerDispenser(6, 7, 1);
    
    catalog.registerDispenser(8, 3, 1);
    catalog.registerDispenser(8, 5, 1);
    catalog.registerDispenser(8, 7, 1);
    
    catalog.registerDispenser(10, 3, 1);
    catalog.registerDispenser(10, 5, 1);
    catalog.registerDispenser(10, 7, 1);
    
    catalog.registerDispenser(12, 3, 1);
    catalog.registerDispenser(12, 5, 1);
    catalog.registerDispenser(12, 7, 1);
    
    // shuffle for now, so merge operations don't spawn next to each other. Another options to ensure this, is to move around when the different operations are created in the assay.
    //Collections.shuffle(reservoirTiles, RandomUtil.get());
    
  }
}
