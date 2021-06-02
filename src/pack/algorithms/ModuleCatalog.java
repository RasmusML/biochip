package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

public class ModuleCatalog {
  
  public List<Module> modules;
  
  public ModuleCatalog() {
    modules = new ArrayList<>();
  }
  
  protected void register(String operation, int duration, ModulePolicy policy, int x, int y, int width, int height, Tag... tags) {
    Module module = new Module();
    module.operation = operation;
    module.duration = duration;
    module.policy = policy;
    module.position = new Point(x, y);
    module.width = width;
    module.height = height;
    
    for (Tag tag : tags) {
      module.attributes.put(tag.key, tag.value);
    }
    
    modules.add(module);
  }
}
