package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

public class ModuleCatalog {
  
  public List<Module> modules;
  
  public ModuleCatalog() {
    modules = new ArrayList<>();
  }
  
  public void register(String operation, int x, int y, int width, int height, int duration, ModulePolicy policy, Tag... attributes) {
    Module module = new Module();
    module.operation = operation;
    module.position = new Point(x, y);
    module.width = width;
    module.height = height;

    module.duration = duration;
    module.policy = policy;
    
    for (Tag tag : attributes) {
      module.attributes.put(tag.key, tag.value);
    }
    
    modules.add(module);
  }
  
  public void registerDispenser(int x, int y, int duration) {
    register(OperationType.dispense, x, y, 1, 1, duration, ModulePolicy.lockedOnOperation);
  }
  
  public void registerDisposer(int x, int y) {
    register(OperationType.dispose, x, y, 1, 1, 1, ModulePolicy.alwaysOpen);
  }
  
  public void registerHeater(int x, int y, int width, int height, int duration, float temperature) {
    register(OperationType.heating, x, y, width, height, duration, ModulePolicy.lockedOnOperation, new Tag(Tags.temperature, temperature));
  }
}
