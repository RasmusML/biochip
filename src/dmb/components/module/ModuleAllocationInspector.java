package dmb.components.module;

import java.util.HashMap;
import java.util.Map;

public class ModuleAllocationInspector {
  
  private Map<Module, ModuleAllocation> moduleToModuleAllocation;
  
  public ModuleAllocationInspector(ModuleCatalog catalog) {
    moduleToModuleAllocation = new HashMap<>();

    for (Module module : catalog.modules) {
      ModuleAllocation allocation = new ModuleAllocation();
      moduleToModuleAllocation.put(module, allocation);
    }
  }

}
