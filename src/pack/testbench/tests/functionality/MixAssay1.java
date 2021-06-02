package pack.testbench.tests.functionality;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class MixAssay1 extends BioAssay {

  public MixAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input1, mix1);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}