package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

public class Test1BioAssay extends BioAssay {
	
	public Test1BioAssay() {
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
	  int input1 = builder.createDispenseOperation("NaOH");
	  int input2 = builder.createDispenseOperation("COOH");
	  int input3 = builder.createDispenseOperation("NaOH");

	  int merge1 = builder.createMergeOperation();
	  int merge2 = builder.createMergeOperation();
    
	  int split1 = builder.createSplitOperation();
    
		builder.connect(input1, merge1);
		builder.connect(input2, merge1);
		
		builder.connect(merge1, merge2);
		builder.connect(input3, merge2);

		builder.connect(merge2, split1);

		sink = builder.getSink();
		count = builder.getOperationCount();
	}
}
