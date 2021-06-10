package dmb.testbench.tests;

import dmb.algorithms.BioAssay;
import dmb.algorithms.BioAssayBuilder;

public class Test2BioAssay extends BioAssay {
	
	public Test2BioAssay() {
		build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
		int input1 = builder.createDispenseOperation("A");
		int input2 = builder.createDispenseOperation("A");
    
		int merge = builder.createMergeOperation();
		
		builder.connect(input1, merge);
		builder.connect(input2, merge);
		
    int split1 = builder.createSplitOperation();
    int merge1 = builder.createMergeOperation();
    int mix1 = builder.createMixOperation();
		
    builder.connect(merge, split1);
    
    builder.connect(split1, merge1);
    builder.connect(split1, merge1);
    
    builder.connect(merge1, mix1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();
	}
}
