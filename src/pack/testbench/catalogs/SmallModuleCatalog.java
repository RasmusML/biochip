package pack.testbench.catalogs;

import pack.algorithms.ModuleCatalog;
import pack.algorithms.ModulePolicy;
import pack.algorithms.OperationType;
import pack.algorithms.Tag;
import pack.algorithms.Tags;

public class SmallModuleCatalog extends ModuleCatalog {
  
  public SmallModuleCatalog() {
    register(OperationType.heating, 20, ModulePolicy.alwaysLocked, 3, 1, 2, 3, new Tag(Tags.temperature, 90f));
    register(OperationType.heating, 40, ModulePolicy.alwaysLocked, 5, 3, 2, 2, new Tag(Tags.temperature, 9000f));
  }
}
