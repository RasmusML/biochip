package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

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
