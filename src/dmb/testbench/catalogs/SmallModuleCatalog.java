package dmb.testbench.catalogs;

import dmb.algorithms.OperationType;
import dmb.components.input.AttributeTag;
import dmb.components.input.AttributeTags;
import dmb.components.module.ModuleCatalog;
import dmb.components.module.ModulePolicy;

public class SmallModuleCatalog extends ModuleCatalog {
  
  public SmallModuleCatalog() {
    register(OperationType.heating, 3, 1, 2, 3, 20, ModulePolicy.alwaysLocked, new AttributeTag(AttributeTags.temperature, 90f));
    register(OperationType.heating, 5, 3, 2, 2, 40, ModulePolicy.alwaysLocked, new AttributeTag(AttributeTags.temperature, 9000f));
  }
}
