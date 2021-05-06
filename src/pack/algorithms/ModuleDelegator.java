package pack.algorithms;

import java.util.List;

import pack.tests.ModuleCatalog;

public class ModuleDelegator {
  
  private ModuleCatalog catalog;

  public ModuleDelegator(ModuleCatalog catalog) {
    this.catalog = catalog;

  }
  
  public Module get(String moduleName) {
    for (Module module : catalog.modules) {
      if (module.name.equals(moduleName)) return module;
    }
    return null;
  }

  public List<Module> getAllModules() {
    return catalog.modules;
  }
}
