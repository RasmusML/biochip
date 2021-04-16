package pack.algorithms;

import java.util.ArrayList;
import java.util.List;

public class Operation {

	public int id;
	public String type;
	
	public List<Operation> inputs = new ArrayList<>();
	public List<Operation> outputs = new ArrayList<>();

	public String substance;	// only used for input.
	
}

/*
class Operation {
  public int id;
  public String type;
}

class SpawnOperation extends Operation {
  public String substance;
}

class MixOperation extends Operation {
  public Operation input;
  public Operation output;
}

class MergeOperation extends Operation {
  public Operation input1, input2;
  public Operation output;
}

class SplitOperation extends Operation {
  public Operation input;
  public Operation output1, output2;
}

class Sink {
  public List<Operation> operations = new ArrayList<>();
}
*/
