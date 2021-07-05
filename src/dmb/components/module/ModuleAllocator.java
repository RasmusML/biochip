package dmb.components.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmb.components.input.AttributeTag;
import dmb.helpers.Assert;

/**
 * Allocates and frees modules using a strategy.
 * 
 * @see ModuleAllocationStrategy
 */

public class ModuleAllocator {

  private ModuleCatalog catalog;
  private Map<Module, ModuleAllocation> moduleToModuleAllocation;

  private ModuleAllocationStrategy strategy;

  public ModuleAllocator(ModuleCatalog catalog, ModuleAllocationStrategy strategy) {
    this.catalog = catalog;
    this.strategy = strategy;

    moduleToModuleAllocation = new HashMap<>();

    for (Module module : catalog.modules) {
      ModuleAllocation allocation = new ModuleAllocation();
      moduleToModuleAllocation.put(module, allocation);
    }
  }

  public Module allocate(String operation, AttributeTag... tags) {
    List<Module> modules = getModulesOfOperationType(operation, tags);
    Assert.that(modules.size() > 0);

    Module module = strategy.select(modules, this);
    allocate(module);

    return module;
  }

  public Module allocate(String operation, int minWidth, int minHeight, AttributeTag... tags) {
    List<Module> modules = getModulesOfOperationType(operation, minWidth, minHeight, tags);
    Assert.that(modules.size() > 0);

    Module module = strategy.select(modules, this);
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

  public int getUsedCount(Module module) {
    ModuleAllocation allocation = moduleToModuleAllocation.get(module);
    return allocation.count;
  }

  public boolean isInUse(Module module) {
    return getUsedCount(module) > 0;
  }

  public List<Module> getModulesOfOperationType(String operation, AttributeTag... attributes) {
    List<Module> matches = new ArrayList<>();

    for (Module module : catalog.modules) {
      if (module.operation.equals(operation)) {
        if (!satifiesModuleOperationAttributes(module, attributes)) continue;

        matches.add(module);
      }
    }

    return matches;
  }

  public List<Module> getModulesOfOperationType(String operation, int minWidth, int minHeight, AttributeTag... attributes) {
    List<Module> matches = new ArrayList<>();

    for (Module module : catalog.modules) {
      if (module.width < minWidth || module.height < minHeight) continue;

      if (module.operation.equals(operation)) {
        if (!satifiesModuleOperationAttributes(module, attributes)) continue;

        matches.add(module);
      }
    }

    return matches;
  }

  private boolean satifiesModuleOperationAttributes(Module module, AttributeTag... attributes) {
    for (AttributeTag attribute : attributes) {
      Object value = module.attributes.get(attribute.key);
      if (value == null) return false;
      if (!value.equals(attribute.value)) return false;
    }

    return true;
  }

  public List<Module> getModules() {
    return catalog.modules;
  }
}
