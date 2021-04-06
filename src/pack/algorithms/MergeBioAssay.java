package pack.algorithms;

public class MergeBioAssay extends BioAssay {
	
	public MergeBioAssay() {
		name = "test_1";
		sink = build();
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
		
		Node mixer1 = new Node();
		mixer1.id = 4;
		mixer1.type = "merge";
		
		Node mixer2 = new Node();
    mixer2.id = 5;
    mixer2.type = "merge";
    
		Node sink = new Node();
		sink.type = "sink";
		sink.id = 6;
		
		input1.outputs.add(mixer1);
		input2.outputs.add(mixer1);

		mixer1.inputs.add(input1);
		mixer1.inputs.add(input2);
		mixer1.outputs.add(mixer2);
		
    input3.outputs.add(mixer2);
		
    mixer2.inputs.add(mixer1);
    mixer2.inputs.add(input3);
    mixer2.outputs.add(sink);
    
		sink.inputs.add(mixer2);

		return sink;
	}
	
	/* @TODO: pretty-print
	 * 
	 * input{1,a} -> merge{3} 
	 * input{2,b} -> merge{3} 
	 * 	  merge{3} -> sink{4}
	 */

}
