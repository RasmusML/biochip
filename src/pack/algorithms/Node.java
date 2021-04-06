package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

// @TODO: restructure node into subclasses

// @TODO: build verboseBioAssay from bioAssay (with merge, move etc...)
public class Node { // "fat class"

	public int id;
	public String type;
	
	public List<Node> inputs = new ArrayList<>();
	public List<Node> outputs = new ArrayList<>();

	public String substance;	// only used for input!
	
}


/*

class Node {
	public int id;
}

class InputNode extends Node {
	public String substance;
}

class OperationNode extends Node {
	public String type;
	public List<Node> inputs;
}

 */


/*
class Node {
	public int id;
}

class InputNode extends Node {
	public String substance;
}

class MixingNode extends Node {
	public Node input1, input2;
}

class SinkNode extends Node {
	public List<Node> inputs = new ArrayList<>();
}
*/