package dmb.gui.timeline;

import java.util.ArrayList;
import java.util.List;

import dmb.algorithms.Operation;

/**
 * A timeline layout where each operation is on an different row.
 */

public class SimpleTimelineLayout implements TimelineLayout {

  @Override
  public List<TimelineUnit> pack(List<Operation> operations) {
    List<TimelineUnit> units = new ArrayList<>();

    for (int i = 0; i < operations.size(); i++) {
      Operation operation = operations.get(i);

      int start = operation.getStartTime();
      int end = operation.getEndTime();
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
