package pack.tests.catalogs;

import pack.algorithms.ModuleCatalog;
import pack.algorithms.ModulePolicy;

public class LargeStrictPolicyModuleCatalog extends ModuleCatalog {
  
  public LargeStrictPolicyModuleCatalog() {
    register("heater90", 20, ModulePolicy.alwaysLocked, 3, 1, 2, 3);
    register("heater9000", 40, ModulePolicy.alwaysLocked, 6, 3, 7, 7);
  }
}
