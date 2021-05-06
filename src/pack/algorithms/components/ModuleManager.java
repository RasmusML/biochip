package pack.algorithms.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pack.algorithms.Module;
import pack.algorithms.ModuleCatalog;
import pack.algorithms.ModulePolicy;

public class ModuleManager {
  
  private ModuleCatalog catalog;
  private Map<Module, ModuleAllocation> moduleToModuleAllocation;
  
  public ModuleManager(ModuleCatalog catalog) {
    this.catalog = catalog;
    
    moduleToModuleAllocation = new HashMap<>();
    
    for (Module module : catalog.modules) {
      ModuleAllocation allocation = new ModuleAllocation();
      moduleToModuleAllocation.put(module, allocation);
    }
  }
  
  public Module allocate(String moduleName) {
    Module module = getModule(moduleName);
    ModuleAllocation allocation = moduleToModuleAllocation.get(module);
    allocation.count += 1;
    
    return module;
  }
  
  //@TODO: handle when 2 modules are identical
  
  public void free(String moduleName) {
    Module module = getModule(moduleName);
    ModuleAllocation allocation = moduleToModuleAllocation.get(module);
    allocation.count -= 1;
  }
  
  public List<Module> getInUseOrAlwaysLockedModules() {
    List<Module> inUse = new ArrayList<>();
    
    for (Module module : catalog.modules) {
      if (module.policy == ModulePolicy.alwaysLocked || isInUse(module)) {
        inUse.add(module);
      }
    }
    
    return inUse;
  }
  
  private boolean isInUse(Module module) {
    ModuleAllocation allocation = moduleToModuleAllocation.get(module);
    return allocation.count > 0;
  }
  
  private Module getModule(String name) {
    for (Module module : catalog.modules) {
      if (module.name.equals(name)) {
        return module;
      }
    }
    return null;
  }

  public List<Module> getModules() {
    return catalog.modules;
  }
}

class ModuleAllocation {
  public int count;
}