package dmb.algorithms.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmb.algorithms.Module;
import dmb.algorithms.ModuleCatalog;
import dmb.algorithms.ModulePolicy;
import dmb.algorithms.Tag;
import dmb.helpers.Assert;

public class ModuleAllocator {

  private ModuleCatalog catalog;
  private Map<Module, ModuleAllocation> moduleToModuleAllocation;

  public ModuleAllocator(ModuleCatalog catalog /* , ModuleAllocationStrategy strategy */) {
    this.catalog = catalog;

    moduleToModuleAllocation = new HashMap<>();

    for (Module module : catalog.modules) {
      ModuleAllocation allocation = new ModuleAllocation();
      moduleToModuleAllocation.put(module, allocation);
    }
  }

  public Module allocate(String operation, Tag... tags) {
    List<Module> modules = getModulesOfOperationType(operation, tags);
    Assert.that(modules.size() > 0);

    Module module = modules.get(0); // @TODO: better policy
    allocate(module);

    return module;
  }
  
  public Module allocate(String operation, int minWidth, int minHeight, Tag... tags) {
    List<Module> modules = getModulesOfOperationType(operation, minWidth, minHeight, tags);
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
    Assert.that(allocation.count > 0);
    allocation.count -= 1;
  }

  public List<Module> getInUseOrAlwaysLockedModules() {
    List<Module> inUse = new ArrayList<>();

    for (Module module : catalog.modules) {
      if (module.policy == ModulePolicy.alwaysLocked || (module.policy == ModulePolicy.lockedOnOperation && isInUse(module))) {
        inUse.add(module);
      }
    }

    return inUse;
  }

  public boolean isInUse(Module module) {
    ModuleAllocation allocation = moduleToModuleAllocation.get(module);
    return allocation.count > 0;
  }

  public List<Module> getModulesOfOperationType(String operation, Tag... attributes) {
    List<Module> matches = new ArrayList<>();

    outer: for (Module module : catalog.modules) {
      if (module.operation.equals(operation)) {

        for (Tag attribute : attributes) {
          Object value = module.attributes.get(attribute.key);
          if (value == null) continue outer;
          if (!value.equals(attribute.value)) continue outer;
        }
        
        matches.add(module);
      }
    }
    
    return matches;
  }
  
  public List<Module> getModulesOfOperationType(String operation, int minWidth, int minHeight, Tag... attributes) {
    List<Module> matches = new ArrayList<>();

    outer: for (Module module : catalog.modules) {
      if (module.width < minWidth || module.height < minHeight) continue;
      
      if (module.operation.equals(operation)) {
        for (Tag attribute : attributes) {
          Object value = module.attributes.get(attribute.key);
          if (value == null) continue outer;
          if (!value.equals(attribute.value)) continue outer;
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