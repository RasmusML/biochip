package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.components.input.BioAssayBuilder;

public class PlatformAssay1 extends BioAssay {
  
  public PlatformAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("B");
    int input3 = builder.createDispenseOperation("C");
    int input4 = builder.createDispenseOperation("D");
    int input5 = builder.createDispenseOperation("A");
    
    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);
    
    int merge2 = builder.createMergeOperation();
    builder.connect(merge1, merge2);
    builder.connect(input3, merge2);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
