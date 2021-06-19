package dmb.testbench.tests;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class BlockingDispenserTestBioAssay extends BioAssay {

  public BlockingDispenserTestBioAssay() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("A");
    
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}