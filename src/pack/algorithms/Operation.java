package pack.algorithms;

public class Operation {

  public int id;
  public OperationType type;
  
  public Operation[] inputs;
  public Operation[] outputs;

  public String substance;  // only used for spawn.
}

/*
public class Operation {
  public int id;
  public OperationType type;
  
  public Operation[] inputs;
  public Operation[] outputs;
}

class SpawnOperation extends Operation {
  
  public String substance;
  
  public SpawnOperation(String substance) {
    this.substance = substance;

    type = OperationType.Spawn;
    
    inputs = new Operation[0];
    outputs = new Operation[1];
  }
}

class MixOperation extends Operation {
  
  public MixOperation() {
    type = OperationType.Mix;
    
    inputs = new Operation[1];
    outputs = new Operation[1];
  }
}

class MergeOperation extends Operation {
  
  public MergeOperation() {
    type = OperationType.Merge;
    
    inputs = new Operation[2];
    outputs = new Operation[1];
  }
}

class SplitOperation extends Operation {
  
  public SplitOperation() {
    type = OperationType.Split;
    
    inputs = new Operation[1];
    outputs = new Operation[2];
  }
  
}

*/



