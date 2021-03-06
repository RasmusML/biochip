package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class MixAssay2 extends BioAssay {

  public MixAssay2() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int input2 = builder.createDispenseOperation("COOH");
    
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);

    int mix1 = builder.createMixOperation();
    
    builder.connect(merge1, mix1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
