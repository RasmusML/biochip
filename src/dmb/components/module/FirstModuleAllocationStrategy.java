package dmb.components.module;

import java.util.List;

/**
 * Allocates the first module.
 */

public class FirstModuleAllocationStrategy implements ModuleAllocationStrategy {

  @Override
  public Module select(List<Module> modules, ModuleAllocator allocator) {
    return modules.get(0);
  }
}
