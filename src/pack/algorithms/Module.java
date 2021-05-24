package pack.algorithms;

import java.util.HashMap;
import java.util.Map;

public class Module {
  public String name;
  
  public int duration; // in timesteps for now.
  
  public ModulePolicy policy;

  public Point position;
  public int width, height;
}

class Setup {
  
  
  void setup() {
    
    {
      Operation operation = new Operation();
      operation.name = "heating";
      
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("temperature", 100f);
      attributes.put("temperature unit", "celcius");
      
      operation.attributes = attributes;
    }
    
    {
      Operation operation = new Operation();
      operation.name = "mix";
    }
    
    {
      OperationImp imp = new OperationImp();
      imp.name = "mix";
      imp.configurable = true;
    }
    
    {
      OperationImp imp = new OperationImp();
      imp.name = "heater";
      imp.configurable = false;
      
      Map<String, Object> attributes = new HashMap<>();
      
      Area area = new Area();
      area.x = 4;
      area.y = 2;
      area.width = 3;
      area.height = 2;
      
      attributes.put("area", area);
      
      attributes.put("temperature", 100f);
      attributes.put("temperature unit", "celcius");
      
      attributes.put("duration", 10);
      attributes.put("duration unit", "s");
  
      attributes.put("running", false);
      
      imp.attributes = attributes;
    }
  }
}

class OperationImp {
  public String name;
  public boolean configurable;
  
  public Map<String, Object> attributes;
  
}

class Area {
  public int x, y;
  public int width, height;
}
