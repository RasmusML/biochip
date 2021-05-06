package pack.tests;

import pack.algorithms.ModulePolicy;

public class LargeModuleCatalog extends ModuleCatalog {
  
  public LargeModuleCatalog() {
    register("heater90", 20, ModulePolicy.alwaysLocked, 3, 1, 2, 3);
    register("heater9000", 40, ModulePolicy.alwaysLocked, 6, 3, 7, 7);
  }
}