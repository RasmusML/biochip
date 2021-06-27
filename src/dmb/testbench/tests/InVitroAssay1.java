package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.testbench.InVitroBuilder;

public class InVitroAssay1 extends BioAssay {

  public InVitroAssay1() {
    build();
  }

  private void build() {
    InVitroBuilder builder = new InVitroBuilder();
    builder.createInVitro(4);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
