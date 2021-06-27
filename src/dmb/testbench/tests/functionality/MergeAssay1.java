package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class MergeAssay1 extends BioAssay {

  public MergeAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int input2 = builder.createDispenseOperation("COOH");
    
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
