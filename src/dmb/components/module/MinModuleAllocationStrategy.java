package dmb.components.module;

import java.util.List;

/**
 * Allocates the module with the fewest number of allocations.
 */

public class MinModuleAllocationStrategy implements ModuleAllocationStrategy {

  @Override
  public Module select(List<Module> modules, ModuleAllocator allocator) {
    int minUsage = Integer.MAX_VALUE;
    Module selected = null;

    for (Module module : modules) {
      int usage = allocator.getUsedCount(module);
      if (usage < minUsage) {
        minUsage = usage;
        selected = module;
      }
    }

    return selected;
  }
}
