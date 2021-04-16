package pack.algorithms;

public class Test2BioAssay extends BioAssay {
	
	public Test2BioAssay() {
		name = "test_1";
		sink = build();
		count = sink.id;
	}

	private Operation build() {
		Operation input1 = new Operation();
		input1.id = 1;
		input1.type = "input";
		input1.substance = "a";
		
		Operation split1 = new Operation();
		split1.id = 2;
		split1.type = "split";
		
		Operation merge1 = new Operation();
		merge1.id = 3;
		merge1.type = "merge";
		
		Operation mix1 = new Operation();
		mix1.id = 4;
		mix1.type = "mix";
		
		Operation sink = new Operation();
		sink.id = 5;
		sink.type = "sink";
		
		input1.outputs.add(split1);
		split1.inputs.add(input1);
		
		split1.outputs.add(merge1);
		split1.outputs.add(merge1);
		merge1.inputs.add(split1);
		merge1.inputs.add(split1);
		
		merge1.outputs.add(mix1);
		mix1.inputs.add(merge1);
    
		mix1.outputs.add(sink);
		sink.inputs.add(mix1);
		
		return sink;
	}
}
