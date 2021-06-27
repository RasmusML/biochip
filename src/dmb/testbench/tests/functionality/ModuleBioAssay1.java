package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class ModuleBioAssay1 extends BioAssay {

  public ModuleBioAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createHeatingOperation(90f);
    
    builder.connect(input1, heat1);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
