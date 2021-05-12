package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class ModuleBioAssay2 extends BioAssay {

  public ModuleBioAssay2() {
    name = "heating_2";
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createHeatingOperation(90f);
    
    int input2 = builder.createDispenseOperation("COOH");
    int heat2 = builder.createHeatingOperation(9000);
    
    int input3 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input1, heat1);
    builder.connect(input2, heat2);
    builder.connect(input3, mix1);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
