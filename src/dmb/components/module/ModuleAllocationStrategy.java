package dmb.components.module;

import java.util.List;

public interface ModuleAllocationStrategy {
  public Module select(List<Module> modules, ModuleAllocator allocator);
}
