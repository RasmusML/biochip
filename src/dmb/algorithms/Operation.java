package dmb.algorithms;

import java.util.HashMap;
import java.util.Map;

import framework.input.Droplet;

public class Operation {

  public int id;
  public String name;
  
  public Operation[] inputs;
  public Operation[] outputs;
  
  public Map<String, Object> attributes = new HashMap<>();
  
  public Droplet[] manipulating;
  public Droplet[] forwarding;
  
  public boolean completed() {
    for (Droplet droplet : manipulating) {
      if (droplet == null) return false;
    }
    return true;
  }
  
  public int getStartTime() {
    int start = Integer.MIN_VALUE;
    
    for (Droplet droplet : manipulating) {
      int dropletStart = droplet.getStartTimestamp();
      if (dropletStart > start) start = dropletStart;
    }
    
    return start;
  }
  
  public int getEndTime() {
    int end = Integer.MAX_VALUE;
    
    for (Droplet droplet : manipulating) {
      int dropletEnd = droplet.getEndTimestamp();
      if (dropletEnd < end) end = dropletEnd;
    }
    
    return end;
  }
  
  public int getDuration() {
    return getEndTime() - getStartTime();
  }

}
