package pack.testbench.catalogs;

import pack.algorithms.ModuleCatalog;
import pack.algorithms.ModulePolicy;
import pack.algorithms.OperationType;
import pack.algorithms.Tag;
import pack.algorithms.Tags;

public class LargeSoftPolicyModuleCatalog extends ModuleCatalog {
  
  public LargeSoftPolicyModuleCatalog() {
    register(OperationType.heating, 20, ModulePolicy.lockedOnOperation, 3, 1, 2, 3, new Tag(Tags.temperature, 90f));
    register(OperationType.heating, 40, ModulePolicy.lockedOnOperation, 6, 3, 7, 7, new Tag(Tags.temperature, 9000f));
  }
}
