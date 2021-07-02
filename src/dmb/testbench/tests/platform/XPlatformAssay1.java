package dmb.testbench.tests.platform;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class XPlatformAssay1 extends BioAssay {
  
  public XPlatformAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("A");
    int mix1 = builder.createMixOperation();
    builder.connect(input1, mix1);
    
    int disposer1 = builder.createDisposeOperation();
    builder.connect(mix1, disposer1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
