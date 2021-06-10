package dmb.testbench.tests;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class PlatformAssay2 extends BioAssay {
  
  public PlatformAssay2() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createHeatingOperation(90f);
    
    builder.connect(input1, heat1);

    int input3 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input3, mix1);

    int input4 = builder.createDispenseOperation("A");
    int input5 = builder.createDispenseOperation("B");
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input4, merge1);
    builder.connect(input5, merge1);
    
    int heat2 = builder.createHeatingOperation(9000f);
    builder.connect(merge1, heat2);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
