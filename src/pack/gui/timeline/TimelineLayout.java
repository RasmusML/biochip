package pack.gui.timeline;

import java.util.List;

import pack.algorithms.Operation;

public interface TimelineLayout {
  public List<TimelineUnit> pack(List<Operation> operations);
}
