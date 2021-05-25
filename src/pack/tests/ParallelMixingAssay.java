package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class ParallelMixingAssay extends BioAssay {

  public ParallelMixingAssay() {
    name = "parallel_mixing_1";
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
