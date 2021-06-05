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
