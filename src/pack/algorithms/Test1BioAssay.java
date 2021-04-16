package pack.algorithms;

public class Test1BioAssay extends BioAssay {
	
	public Test1BioAssay() {
		name = "test_1";
		sink = build();
		count = 7;
	}

	private Operation build() {
		Operation input1 = new Operation();
		input1.id = 1;
		input1.type = "input";
		input1.substance = "a";

		Operation input2 = new Operation();
		input2.id = 2;
		input2.type = "input";
		input2.substance = "b";
		
		Operation input3 = new Operation();
		input3.id = 3;
		input3.type = "input";
		input3.substance = "a";
		
		Operation merge1 = new Operation();
		merge1.id = 4;
		merge1.type = "merge";
		
		Operation merge2 = new Operation();
    merge2.id = 5;
    merge2.type = "merge";
    
    Operation split1 = new Operation();
    split1.id = 6;
    split1.type = "split";
    
		Operation sink = new Operation();
		sink.type = "sink";
		sink.id = 7;
    
		input1.outputs.add(merge1);
		input2.outputs.add(merge1);

		merge1.inputs.add(input1);
		merge1.inputs.add(input2);
		merge1.outputs.add(merge2);
		
    input3.outputs.add(merge2);
		
    merge2.inputs.add(merge1);
    merge2.inputs.add(input3);
    merge2.outputs.add(split1);

    split1.inputs.add(merge2);
    split1.outputs.add(sink);
    split1.outputs.add(sink);
    
		sink.inputs.add(split1);
    sink.inputs.add(split1);

		return sink;
	}
}
