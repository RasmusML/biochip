package pack.algorithms.experimental;

import java.util.ArrayList;
import java.util.List;

public class Operation {
  public int id;
  public OperationType type;
}

enum OperationType {
  Spawn,
  Mix,
  Merge,
  Split,
  Module;
}

class SpawnOperation extends Operation {
  public String substance;
  public Operation output;
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

class ModuleOperation extends Operation {
  public String name;
  public List<Operation> inputs;
  public List<Operation> outputs;
}

class Sink {
  public List<Operation> operations = new ArrayList<>();
}

class Source {
  public List<Operation> operations = new ArrayList<>();
}