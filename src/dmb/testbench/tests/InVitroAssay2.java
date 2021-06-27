package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.InVitroBuilder;

public class InVitroAssay2 extends BioAssay {

  public InVitroAssay2() {
    build();
  }

  private void build() {
    InVitroBuilder builder = new InVitroBuilder();
    builder.createInVitro(6);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
