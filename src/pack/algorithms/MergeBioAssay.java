package pack.algorithms;

public class MergeBioAssay extends BioAssay {
	
	public MergeBioAssay() {
		name = "test_1";
		sink = build();
		count = 6;
	}

	private Node build() {
		Node input1 = new Node();
		input1.id = 1;
		input1.type = "input";
		input1.substance = "a";

		Node input2 = new Node();
		input2.id = 2;
		input2.type = "input";
		input2.substance = "b";
		
		Node input3 = new Node();
		input3.id = 3;
		input3.type = "input";
		input3.substance = "a";
		
		Node merge1 = new Node();
		merge1.id = 4;
		merge1.type = "merge";
		
		Node merge2 = new Node();
    merge2.id = 5;
    merge2.type = "merge";
    
    Node split1 = new Node();
    split1.id = 6;
    split1.type = "split";
    
		Node sink = new Node();
		sink.type = "sink";
		//sink.id = 7;
		sink.id = 6;
    
		input1.outputs.add(merge1);
		input2.outputs.add(merge1);

		merge1.inputs.add(input1);
		merge1.inputs.add(input2);
		merge1.outputs.add(merge2);
		
    input3.outputs.add(merge2);
		
    merge2.inputs.add(merge1);
    merge2.inputs.add(input3);
    merge2.outputs.add(sink);
    
    /*
    merge2.outputs.add(split1);

    split1.inputs.add(merge2);
    split1.outputs.add(sink);
    split1.outputs.add(sink);
    
		sink.inputs.add(split1);
    sink.inputs.add(split1);
    */
    
    sink.inputs.add(merge2);

		return sink;
	}
}
