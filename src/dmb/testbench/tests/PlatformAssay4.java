package dmb.testbench.tests;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class PlatformAssay4 extends BioAssay {
  
  public PlatformAssay4() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();

    int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("B");
    int input3 = builder.createDispenseOperation("C");
    int input4 = builder.createDispenseOperation("D");
    int input5 = builder.createDispenseOperation("E");
    int input6 = builder.createDispenseOperation("F");
    int input7 = builder.createDispenseOperation("G");
    int input8 = builder.createDispenseOperation("H");
    int input9 = builder.createDispenseOperation("I");
    int input10 = builder.createDispenseOperation("J");
    int input11 = builder.createDispenseOperation("K");
    int input12 = builder.createDispenseOperation("L");
    int input13 = builder.createDispenseOperation("M");
    int input14 = builder.createDispenseOperation("N");
    int input15 = builder.createDispenseOperation("O");
    
    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input3, merge1);
    
    int merge2 = builder.createMergeOperation();
    builder.connect(input9, merge2);
    builder.connect(merge1, merge2);
    
    int merge3 = builder.createMergeOperation();
    builder.connect(input4, merge3);
    builder.connect(input5, merge3);
    
    int mix1 = builder.createMixOperation();
    builder.connect(merge3, mix1);
    
    int merge4 = builder.createMergeOperation();
    builder.connect(mix1, merge4);
    builder.connect(input2, merge4);
    
    int heater1 = builder.createHeatingOperation(9000f);
    builder.connect(merge4, heater1);
    
    int merge5 = builder.createMergeOperation();
    builder.connect(input6, merge5);
    builder.connect(input7, merge5);
    
    int merge6 = builder.createMergeOperation();
    builder.connect(merge5, merge6);
    builder.connect(merge2, merge6);
    
    int mix2 = builder.createMixOperation();
    builder.connect(merge6, mix2);
    
    int split1 = builder.createSplitOperation();
    builder.connect(mix2, split1);
    
    int heater2 = builder.createHeatingOperation(90f);
    builder.connect(input8, heater2);
    
    int heater3 = builder.createHeatingOperation(90f);
    builder.connect(input10, heater3);
    
    int merge7 = builder.createMergeOperation();
    builder.connect(heater3, merge7);
    builder.connect(input11, merge7);

    int merge8 = builder.createMergeOperation();
    builder.connect(input12, merge8);
    builder.connect(input13, merge8);
    
    int mix3 = builder.createMixOperation();
    builder.connect(merge8, mix3);

    int split2 = builder.createSplitOperation();
    builder.connect(mix3, split2);
    
    int mix5 = builder.createMixOperation();
    builder.connect(input14, mix5);
    
    int dispose1 = builder.createDisposeOperation();
    builder.connect(mix5, dispose1);
    
    int mix6 = builder.createMixOperation();
    builder.connect(input15, mix6);
    
    int dispose2 = builder.createDisposeOperation();
    builder.connect(mix6, dispose2);
    

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
