package pack.gui.timeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pack.algorithms.Droplet;
import pack.algorithms.Operation;
import pack.algorithms.OperationType;

public class CompactTimelineLayout implements TimelineLayout {

  // Packing the timeline corresponds to the activity selection problem.
  // The optimal solution the activity selection problem can be found using a greedy-algorithm. Thus, the computation is tractable.
  // https://en.wikipedia.org/wiki/Activity_selection_problem
  
  @Override
  public List<TimelineUnit> pack(List<Operation> operations) {
    List<TimelineUnit> left = new ArrayList<>();
    
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
        
      } else if (operation.name.equals(OperationType.detection)) {
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
      
      left.add(unit);
    }
    
    // activity selection problem is solved here with the greedy-algorithm.
    // https://en.wikipedia.org/wiki/Activity_selection_problem#Optimal_solution
    
    left.sort((u1, u2) -> {
      int end1 = u1.start + u1.duration;
      int end2 = u2.start + u2.duration;
      return end1 - end2;
    });
    
    List<TimelineUnit> result = new ArrayList<>(left);

    int y = 0;
    while (left.size() > 0) {
      int end = 0;
      
      for (Iterator<TimelineUnit> it = left.iterator(); it.hasNext();) {
        TimelineUnit unit = it.next();
        if (unit.start >= end) {
          end = unit.start + unit.duration;
          unit.y = y;
          
          it.remove();
        }
      }

      y += 1;
    }
    
    return result;
  }
}
