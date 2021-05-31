package pack.testbench.tests.functionality;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

public class DisposeAssay1 extends BioAssay {
	
	public DisposeAssay1() {
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
	  int input1 = builder.createDispenseOperation("NaOH");

		int dispose1 = builder.createDisposeOperation();
		
		builder.connect(input1, dispose1);

		sink = builder.getSink();
		count = builder.getOperationCount();
	}
}
