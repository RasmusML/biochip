package dmb.testbench.tests.functionality;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class MergeAssay3 extends BioAssay {

  public MergeAssay3() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("B");
    int input3 = builder.createDispenseOperation("C");
    
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input1, merge1);
    builder.connect(input3, merge1);

    int heater = builder.createHeatingOperation(90f);
    
    builder.connect(input2, heater);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
