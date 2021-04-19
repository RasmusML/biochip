package pack.algorithms;

public class Operation {

  public int id;
  public OperationType type;
  
  public Operation[] inputs;
  public Operation[] outputs;
  
  public String substance;  // only used for spawn.
  
  public Droplet[] manipulating;
  public Droplet[] forwarding;
  
}

// @TODO: we will have a BioAssay/BioOperation and OperationalGraph/Operation. The above is actually the OperationalGraph/Operation

/*
class OperationAttributes {
}

class SpawnAttributes extends OperationAttributes {
  public String substance;
}

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


/*
public class Operation {
  public int id;
  public OperationType type;
  
  public String substance;  // only used for spawn.
  
  public Droplet[] input;
  public Droplet[] output;
}

class Droplet {
  public int uid;
  
  public int id;
  public Route route;

  public Operation createdBy;
  public Operation processedBy;
}
*/


