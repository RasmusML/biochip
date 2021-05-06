package pack.tests;

import pack.algorithms.ModulePolicy;

public class SmallModuleCatalog extends ModuleCatalog {
  
  public SmallModuleCatalog() {
    register("heater90", 20, ModulePolicy.alwaysLocked, 3, 1, 2, 3);
    register("heater9000", 40, ModulePolicy.alwaysLocked, 5, 3, 2, 2);
  }
}
