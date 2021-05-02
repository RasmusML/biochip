package pack.algorithms;

public class Operation {

  public int id;
  public OperationType type;
  
  public Operation[] inputs;
  public Operation[] outputs;
  
  public String substance;  // only used for dispensing.
  public float targetTemperature; // only used for heating.
  
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


class Module {
  public String type;
}

class HeaterXXX extends Module {
  public float maxTemperature;
  public float heatingTime;
  public int width, height;
}
*/

/*
 * Module:
 * name: "Heature9000"
 * duration: "2 sec"
 * area: 4x3
 */

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

/*
class Module {
  public int id;
  public int flags;
}

class Dispenser extends Module {
  public int duration;
}

class Heater extends Module {
  
}
*/