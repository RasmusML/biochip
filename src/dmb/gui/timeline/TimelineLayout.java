package dmb.gui.timeline;

import java.util.List;

import dmb.algorithms.Operation;

public interface TimelineLayout {
  public List<TimelineUnit> pack(List<Operation> operations);
}
