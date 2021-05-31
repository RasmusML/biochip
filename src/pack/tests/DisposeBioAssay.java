package pack.tests;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class DisposeBioAssay extends BioAssay {
	
	public DisposeBioAssay() {
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
	  int input1 = builder.createDispenseOperation("NaOH");
	  int split1 = builder.createSplitOperation();
	  
		builder.connect(input1, split1);

		int dispose1 = builder.createDisposeOperation();
		builder.connect(split1, dispose1);

		sink = builder.getSink();
		count = builder.getOperationCount();
	}
}
