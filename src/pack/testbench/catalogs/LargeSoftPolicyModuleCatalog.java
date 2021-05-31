package pack.testbench.catalogs;

import pack.algorithms.ModuleCatalog;
import pack.algorithms.ModulePolicy;

public class LargeSoftPolicyModuleCatalog extends ModuleCatalog {
  
  public LargeSoftPolicyModuleCatalog() {
    register("heater90", 20, ModulePolicy.lockedOnOperation, 3, 1, 2, 3);
    register("heater9000", 40, ModulePolicy.lockedOnOperation, 6, 3, 7, 7);
  }
}
