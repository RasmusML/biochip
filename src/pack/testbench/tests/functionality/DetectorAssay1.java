package pack.testbench.tests.functionality;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class DetectorAssay1 extends BioAssay {

  public DetectorAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int detector1 = builder.createDetectionOperation("metal");
    
    builder.connect(input1, detector1);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
