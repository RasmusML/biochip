package dmb.testbench.tests.functionality;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class MergeAssay2 extends BioAssay {

  public MergeAssay2() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int input2 = builder.createDispenseOperation("COOH");
    
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);

    int input3 = builder.createDispenseOperation("NaOH2");
    int input4 = builder.createDispenseOperation("COOH3");
    
    int merge2 = builder.createMergeOperation();
    
    builder.connect(input3, merge2);
    builder.connect(input4, merge2);

    int merge3 = builder.createMergeOperation();

    builder.connect(merge1, merge3);
    builder.connect(merge2, merge3);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
