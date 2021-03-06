package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.testbench.builder.BioAssayBuilder;

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
