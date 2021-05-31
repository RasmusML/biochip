package pack.testbench.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class CrowdedModuleBioAssay extends BioAssay {

  public CrowdedModuleBioAssay() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("NaOH");
    int input2 = builder.createDispenseOperation("COOH");
    
    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);
    
    int heat1 = builder.createHeatingOperation(9000f);
    builder.connect(merge1, heat1);
    
    int input3 = builder.createDispenseOperation("NaCl");
    int mix1 = builder.createMixOperation();
    
    builder.connect(input3, mix1);

    int input4 = builder.createDispenseOperation("A");
    int input5 = builder.createDispenseOperation("B");
    int merge2 = builder.createMergeOperation();
    
    builder.connect(input4, merge2);
    builder.connect(input5, merge2);

    /*
    int input6 = builder.createDispenseOperation("C");
    int split1 = builder.createSplitOperation();
    
    builder.connect(input6, split1);
    */
    
    int input7 = builder.createDispenseOperation("CaCl2");
    int mix2 = builder.createMixOperation();
    
    builder.connect(input7, mix2);
    
    int input8 = builder.createDispenseOperation("CaCl3");
    int mix3 = builder.createMixOperation();
    
    builder.connect(input8, mix3);

    int input9 = builder.createDispenseOperation("CaCl4");
    int mix4 = builder.createMixOperation();
    
    builder.connect(input9, mix4);
    
    /*
    int input10 = builder.createDispenseOperation("CaCl5");
    int split2 = builder.createSplitOperation();
    
    builder.connect(input10, split2);
    */
    
    /*
    int input11 = builder.createDispenseOperation("CaCl6");
    int split3 = builder.createSplitOperation();
    
    builder.connect(input11, split3);
    */
    
    int input12 = builder.createDispenseOperation("D");
    int input13 = builder.createDispenseOperation("E");
    int merge3 = builder.createMergeOperation();
    
    builder.connect(input12, merge3);
    builder.connect(input13, merge3);
    
    int input14 = builder.createDispenseOperation("CaCl7");
    int mix5 = builder.createMixOperation();

    builder.connect(input14, mix5);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
  }
  
}
