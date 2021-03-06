package dmb.testbench.tests;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class Test3BioAssay extends BioAssay {
	
	public Test3BioAssay() {
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
	  int input1 = builder.createDispenseOperation("NaOH");
	  int input2 = builder.createDispenseOperation("COOH");
	  int input3 = builder.createDispenseOperation("H2O");
	  int input4 = builder.createDispenseOperation("Fe3O");

	  int merge1 = builder.createMergeOperation();
	  int merge2 = builder.createMergeOperation();
	  
	  int mix1 = builder.createMixOperation();
	  int mix2 = builder.createMixOperation();

	  int split1 = builder.createSplitOperation();

	  int merge3 = builder.createMergeOperation();
    
		builder.connect(input1, merge1);
		builder.connect(input2, merge1);
		
		builder.connect(input3, merge2);
		builder.connect(input4, merge2);

    builder.connect(merge1, mix1);
    builder.connect(merge2, mix2);
    
    builder.connect(mix1, split1);
    
    builder.connect(split1, merge3);
    builder.connect(mix2, merge3);

		sink = builder.getSink();
		count = builder.getOperationCount();
	}
}
