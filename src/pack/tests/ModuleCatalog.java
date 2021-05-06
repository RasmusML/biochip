package pack.tests;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.Module;
import pack.algorithms.Point;

public class ModuleCatalog {
  
  public List<Module> modules;
  
  public ModuleCatalog() {
    modules = new ArrayList<>();
  }
  
  protected void register(String name, int duration, int x, int y, int width, int height) {
    Module module = new Module();
    module.name = name;
    module.duration = duration;
    module.position = new Point(x, y);
    module.width = width;
    module.height = height;
    
    modules.add(module);
  }
}
