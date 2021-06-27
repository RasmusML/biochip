package dmb.testbench.catalogs;

import dmb.algorithms.OperationType;
import dmb.components.input.AttributeTag;
import dmb.components.input.AttributeTags;
import dmb.components.module.ModuleCatalog;
import dmb.components.module.ModulePolicy;

public class LargeSoftPolicyModuleCatalog extends ModuleCatalog {

  public LargeSoftPolicyModuleCatalog() {
    register(OperationType.heating, 3, 1, 2, 3, 20, ModulePolicy.lockedOnOperation, new AttributeTag(AttributeTags.temperature, 90f));
    register(OperationType.heating, 6, 3, 7, 7, 40, ModulePolicy.lockedOnOperation, new AttributeTag(AttributeTags.temperature, 9000f));
  }
}
