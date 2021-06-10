package dmb.testbench.catalogs;

import dmb.algorithms.ModuleCatalog;
import dmb.algorithms.ModulePolicy;
import dmb.algorithms.OperationType;
import dmb.algorithms.Tag;
import dmb.algorithms.Tags;

public class LargeStrictPolicyModuleCatalog extends ModuleCatalog {
  
  public LargeStrictPolicyModuleCatalog() {
    register(OperationType.heating, 3, 1, 2, 3, 20, ModulePolicy.alwaysLocked, new Tag(Tags.temperature, 90f));
    register(OperationType.heating, 6, 3, 7, 7, 40, ModulePolicy.alwaysLocked, new Tag(Tags.temperature, 9000f));
  }
}
