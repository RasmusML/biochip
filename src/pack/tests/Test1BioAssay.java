package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class Test1BioAssay extends BioAssay {
	
	public Test1BioAssay() {
	  name = "test_1";
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
	  int input1 = builder.createSpawnOperation("NaOH");
	  int input2 = builder.createSpawnOperation("COOH");
	  int input3 = builder.createSpawnOperation("NaOH");

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
