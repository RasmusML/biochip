package pack.testbench.tests.functionality;

import pack.algorithms.BioAssay;
import pack.algorithms.BioAssayBuilder;

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
