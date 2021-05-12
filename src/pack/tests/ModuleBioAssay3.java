package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class ModuleBioAssay3 extends BioAssay {

  public ModuleBioAssay3() {
    name = "heating_3";
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createHeatingOperation(90f);
    
    builder.connect(input1, heat1);

    int input2 = builder.createDispenseOperation("COOH");
    int heat2 = builder.createHeatingOperation(9000f);

    builder.connect(input2, heat2);
    
    int input3 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input3, mix1);

    int input4 = builder.createDispenseOperation("A");
    int input5 = builder.createDispenseOperation("B");
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input4, merge1);
    builder.connect(input5, merge1);

    int input6 = builder.createDispenseOperation("C");
    int split1 = builder.createSplitOperation();
    
    builder.connect(input6, split1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
