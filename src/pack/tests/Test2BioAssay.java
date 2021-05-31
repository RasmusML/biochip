package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class Test2BioAssay extends BioAssay {
	
	public Test2BioAssay() {
		build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
		int input1 = builder.createDispenseOperation("A");
    int split1 = builder.createSplitOperation();
    int merge1 = builder.createMergeOperation();
    int mix1 = builder.createMixOperation();
		
    builder.connect(input1, split1);
    
    builder.connect(split1, merge1);
    builder.connect(split1, merge1);
    
    builder.connect(merge1, mix1);
    
    sink = builder.getSink();
    count = builder.getOperationCount();

	}
}
