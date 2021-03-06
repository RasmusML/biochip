package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class ModuleBioAssay4 extends BioAssay {

  public ModuleBioAssay4() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createHeatingOperation(90f);
    
    builder.connect(input1, heat1);

    int input3 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input3, mix1);

    int input4 = builder.createDispenseOperation("A");
    int input5 = builder.createDispenseOperation("B");
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input4, merge1);
    builder.connect(input5, merge1);
    
    int heat2 = builder.createHeatingOperation(9000f);
    builder.connect(merge1, heat2);

    /* Droplet-aware routing can't split a 1 area droplet
    int input6 = builder.createDispenseOperation("C");
    int split1 = builder.createSplitOperation();
    
    builder.connect(input6, split1);
    */
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
