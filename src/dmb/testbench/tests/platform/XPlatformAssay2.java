package dmb.testbench.tests.platform;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class XPlatformAssay2 extends BioAssay {
  
  public XPlatformAssay2() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("B");
    
    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);
    
    int mix1 = builder.createMixOperation();
    builder.connect(merge1, mix1);
    
    //int disposer1 = builder.createDisposeOperation();
    //builder.connect(mix1, disposer1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
