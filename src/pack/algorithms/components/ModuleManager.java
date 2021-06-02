package pack.algorithms.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pack.algorithms.Module;
import pack.algorithms.ModuleCatalog;
import pack.algorithms.ModulePolicy;
import pack.algorithms.Tag;
import pack.helpers.Assert;

public class ModuleManager {

  private ModuleCatalog catalog;
  private Map<Module, ModuleAllocation> moduleToModuleAllocation;

  public ModuleManager(ModuleCatalog catalog /* , ModuleAllocationStrategy strategy */) {
    this.catalog = catalog;

    moduleToModuleAllocation = new HashMap<>();

    for (Module module : catalog.modules) {
      ModuleAllocation allocation = new ModuleAllocation();
      moduleToModuleAllocation.put(module, allocation);
    }
  }

  public Module allocate(String operation, Tag... tags) {
    List<Module> modules = getModulesOfOperation(operation, tags);
    Assert.that(modules.size() > 0);

    Module module = modules.get(0); // @TODO: better policy
    allocate(module);

    return module;
  }

  public void allocate(Module module) {
    ModuleAllocation allocation = moduleToModuleAllocation.get(module);
    allocation.count += 1;
  }

  public void free(Module module) {
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

  private List<Module> getModulesOfOperation(String operation, Tag... tags) {
    List<Module> matches = new ArrayList<>();

    outer: for (Module module : catalog.modules) {
      if (module.operation.equals(operation)) {

        for (Tag tag : tags) {
          Object value = module.attributes.get(tag.key);
          if (value == null) continue outer;
          if (!value.equals(tag.value)) continue outer;
        }
        
        matches.add(module);
      }
    }
    
    return matches;
  }

  public List<Module> getModules() {
    return catalog.modules;
  }

  class ModuleAllocation {
    public int count;
  }
}
