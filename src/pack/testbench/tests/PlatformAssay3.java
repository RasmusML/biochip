package pack.testbench.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class PlatformAssay3 extends BioAssay {
  
  public PlatformAssay3() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createHeatingOperation(90f);
    
    builder.connect(input1, heat1);

    int input2 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input2, mix1);

    int input3 = builder.createDispenseOperation("A");
    int input4 = builder.createDispenseOperation("B");
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input3, merge1);
    builder.connect(input4, merge1);
    
    int heat2 = builder.createHeatingOperation(9000f);
    builder.connect(merge1, heat2);
    
    int merge2 = builder.createMergeOperation();
    builder.connect(heat2, merge2);
    builder.connect(mix1, merge2);

    int merge3 = builder.createMergeOperation();
    builder.connect(merge2, merge3);
    builder.connect(heat1, merge3);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
