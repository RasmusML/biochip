package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class PCRMixingTreeAssay2 extends BioAssay {

  public PCRMixingTreeAssay2() {
    build();
  }

  private void build() {
    BioAssayBuilder builder = new BioAssayBuilder();
    
    int input1 = builder.createDispenseOperation("Tris-HCl");
    int input2 = builder.createDispenseOperation("KCl");
    int input3 = builder.createDispenseOperation("Bovine Serum Albumin");
    int input4 = builder.createDispenseOperation("Gelatin");
    int input5 = builder.createDispenseOperation("Primer");
    int input6 = builder.createDispenseOperation("Triphosphate");
    int input7 = builder.createDispenseOperation("AmpliTaq DNA");
    int input8 = builder.createDispenseOperation("Lambda DNA");
    
    int merge1 = builder.createMergeOperation();
    int merge2 = builder.createMergeOperation();
    
    int merge3 = builder.createMergeOperation();
    int merge4 = builder.createMergeOperation();
    
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);
    
    builder.connect(input3, merge2);
    builder.connect(input4, merge2);
    
    builder.connect(input5, merge3);
    builder.connect(input6, merge3);
    
    builder.connect(input7, merge4);
    builder.connect(input8, merge4);

    
    int mix1 = builder.createMixOperation();
    int mix2 = builder.createMixOperation();
    int mix3 = builder.createMixOperation();
    int mix4 = builder.createMixOperation();
    
    builder.connect(merge1, mix1);
    builder.connect(merge2, mix2);
    builder.connect(merge3, mix3);
    builder.connect(merge4, mix4);


    int merge5 = builder.createMergeOperation();
    int merge6 = builder.createMergeOperation();
    
    builder.connect(mix1, merge5);
    builder.connect(mix2, merge5);
    
    builder.connect(mix3, merge6);
    builder.connect(mix4, merge6);

    int mix5 = builder.createMixOperation();
    int mix6 = builder.createMixOperation();
    
    builder.connect(merge5, mix5);
    builder.connect(merge6, mix6);
    
    int merge7 = builder.createMergeOperation();
    
    builder.connect(mix5, merge7);
    builder.connect(mix6, merge7);
    
    int mix7 = builder.createMixOperation();
    
    builder.connect(merge7, mix7);

    sink = builder.getSink();
    count = builder.getOperationCount();
  }
}
