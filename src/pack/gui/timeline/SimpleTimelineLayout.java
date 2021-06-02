package pack.gui.timeline;

import java.util.ArrayList;
import java.util.List;

import pack.algorithms.Droplet;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;

public class SimpleTimelineLayout implements TimelineLayout {

  @Override
  public List<TimelineUnit> pack(List<Operation> operations) {
    List<TimelineUnit> units = new ArrayList<>();
    
    for (int i = 0; i < operations.size(); i++) {
      Operation operation = operations.get(i);
      
      int start, end;
      if (operation.name.equals(OperationType.mix)) {
        Droplet droplet = operation.manipulating[0];
        
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else if (operation.name.equals(OperationType.merge)) {
        Droplet droplet0 = operation.manipulating[0];
        Droplet droplet1 = operation.manipulating[1];
        
        start = Math.max(droplet0.getStartTimestamp(), droplet1.getStartTimestamp());
        end = Math.min(droplet0.getEndTimestamp(), droplet1.getEndTimestamp());
      } else if (operation.name.equals(OperationType.split)) {
        Droplet droplet = operation.manipulating[0];
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else if (operation.name.equals(OperationType.dispense)) {
        Droplet droplet = operation.manipulating[0];
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else if (operation.name.equals(OperationType.heating)) {
        Droplet droplet = operation.manipulating[0];
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else if (operation.name.equals(OperationType.dispose)) {
        Droplet droplet = operation.manipulating[0];
        start = droplet.getStartTimestamp();
        end = droplet.getEndTimestamp();
      } else {
        throw new IllegalStateException("broken! " + operation.name);
      }
      
      int duration = end - start;

      TimelineUnit unit = new TimelineUnit();
      unit.operation = operation;
      unit.duration = duration;
      unit.start = start;
      unit.y = i;
      
      units.add(unit);
    }
    
    return units;
  }
}
