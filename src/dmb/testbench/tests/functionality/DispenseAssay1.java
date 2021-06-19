package dmb.testbench.tests.functionality;

import dmb.components.input.BioAssay;
import dmb.components.input.BioAssayBuilder;

public class DispenseAssay1 extends BioAssay {
	
	public DispenseAssay1() {
	  build();
	}

	private void build() {
	  BioAssayBuilder builder = new BioAssayBuilder();
	  
	  int input1 = builder.createDispenseOperation("NaOH");

		sink = builder.getSink();
		count = builder.getOperationCount();
	}
}
