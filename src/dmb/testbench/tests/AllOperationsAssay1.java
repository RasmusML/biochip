package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.components.input.BioAssayBuilder;

public class AllOperationsAssay1 extends BioAssay {
  
  public AllOperationsAssay1() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NH3+");
    int input2 = builder.createDispenseOperation("Cl");
    
    int input3 = builder.createDispenseOperation("NaOH");
    
    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);

    int mix1 = builder.createMixOperation();
    builder.connect(merge1, mix1);

    int split1 = builder.createSplitOperation();
    builder.connect(mix1, split1);
    
    int dispose1 = builder.createDisposeOperation();
    builder.connect(split1, dispose1);
    
    int heat1 = builder.createHeatingOperation(90f);
    builder.connect(input3, heat1);

    int merge2 = builder.createMergeOperation();
    builder.connect(heat1, merge2);
    builder.connect(split1, merge2);

    int mix3 = builder.createMixOperation();
    builder.connect(merge2, mix3);

    int detector1 = builder.createDetectionOperation("pH?");
    builder.connect(mix3, detector1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }

}
