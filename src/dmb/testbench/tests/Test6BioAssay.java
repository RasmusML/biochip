package dmb.testbench.tests;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class Test6BioAssay extends BioAssay {
	
	public Test6BioAssay() {
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();

	  int input1 = builder.createDispenseOperation("A");
    int input2 = builder.createDispenseOperation("B");

    int merge1 = builder.createMergeOperation();
    builder.connect(input1, merge1);
    builder.connect(input2, merge1);

    int mix0 = builder.createMixOperation();
    builder.connect(merge1, mix0);

    int split1 = builder.createSplitOperation();
    builder.connect(mix0, split1);
    
    int input3 = builder.createDispenseOperation("C");
    int input4 = builder.createDispenseOperation("C");
    
    int heater1 = builder.createHeatingOperation(90f);
    builder.connect(input3, heater1);

    int merge4 = builder.createMergeOperation();
    builder.connect(heater1, merge4);
    builder.connect(input4, merge4);
    
    int mix5 = builder.createMixOperation();
    builder.connect(merge4, mix5);
    
    int split3 = builder.createSplitOperation();
    builder.connect(mix5, split3);

    int heater3 = builder.createHeatingOperation(90f);
    builder.connect(split3, heater3);
    
    int detector1 = builder.createDetectionOperation("metal");
    builder.connect(heater3, detector1);

    int merge2 = builder.createMergeOperation();
    builder.connect(split1, merge2);
    builder.connect(split3, merge2);

    int mix1 = builder.createMixOperation();
    builder.connect(split1, mix1);

    int mix2 = builder.createMixOperation();
    builder.connect(merge2, mix2);
    
    int dispose1 = builder.createDisposeOperation();
    builder.connect(mix1, dispose1);
    
    int split2 = builder.createSplitOperation();
    builder.connect(mix2, split2);
    
    int heater2 = builder.createHeatingOperation(90f);
    builder.connect(split2, heater2);
    
    int dispose2 = builder.createDisposeOperation();
    builder.connect(split2, dispose2);
    
    int dispose3 = builder.createDisposeOperation();
    builder.connect(heater2, dispose3);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
	}
}
