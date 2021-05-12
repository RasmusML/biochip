package pack.algorithms;

import java.util.HashMap;
import java.util.Map;

public class Operation {

  public int id;
  public String name;
  
  public Operation[] inputs;
  public Operation[] outputs;
  
  public Map<String, Object> attributes = new HashMap<>();
  
  public Droplet[] manipulating;
  public Droplet[] forwarding;
}

/*
class Operation {
  public int id;
}

class SplitOperation extends Operation {
  public Operation input;
  public Operation output1, output2;
}

class MergeOperation extends Operation {
  public Operation input1, input2;
  public Operation output;
}

class MixOperation extends Operation {
  public Operation input;
  public Operation output;
}

class DispenseOperation extends Operation {
  public Operation output;
  public String substance;
}

class HeatingOperation extends Operation {
  public Operation input;
  public Operation output;
  public float targetTemperature;
}

class DetectionOperation extends Operation {
  public Operation input;
  public Operation output;
  public String type;
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