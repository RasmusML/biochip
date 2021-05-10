package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class ModuleBioAssay4 extends BioAssay {

  public ModuleBioAssay4() {
    name = "heating_4";
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int heat1 = builder.createModuleOperation("heater90");
    
    builder.connect(input1, heat1);

    int input3 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input3, mix1);

    int input4 = builder.createDispenseOperation("A");
    int input5 = builder.createDispenseOperation("B");
    int merge1 = builder.createMergeOperation();
    
    builder.connect(input4, merge1);
    builder.connect(input5, merge1);
    
    int heat2 = builder.createModuleOperation("heater9000");
    builder.connect(merge1, heat2);

    int input6 = builder.createDispenseOperation("C");
    int split1 = builder.createSplitOperation();
    
    builder.connect(input6, split1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
