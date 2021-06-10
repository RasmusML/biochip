package dmb.testbench.catalogs;

import dmb.algorithms.ModuleCatalog;
import dmb.algorithms.ModulePolicy;
import dmb.algorithms.OperationType;
import dmb.algorithms.Tag;
import dmb.algorithms.Tags;

public class SmallModuleCatalog extends ModuleCatalog {
  
  public SmallModuleCatalog() {
    register(OperationType.heating, 3, 1, 2, 3, 20, ModulePolicy.alwaysLocked, new Tag(Tags.temperature, 90f));
    register(OperationType.heating, 5, 3, 2, 2, 40, ModulePolicy.alwaysLocked, new Tag(Tags.temperature, 9000f));
  }
}
