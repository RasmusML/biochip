package pack.testbench.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class Test5BioAssay extends BioAssay {
	
	public Test5BioAssay() {
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
    
    int heater1 = builder.createHeatingOperation(90f);
    builder.connect(input3, heater1);
    
    int merge2 = builder.createMergeOperation();
    builder.connect(split1, merge2);
    builder.connect(heater1, merge2);

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
