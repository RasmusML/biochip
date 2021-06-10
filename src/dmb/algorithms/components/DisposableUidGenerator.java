package dmb.algorithms.components;

import java.util.ArrayList;
import java.util.List;

public class DisposableUidGenerator {

  private List<Integer> availableIds;
  
  public DisposableUidGenerator() {
    this(1, 1000);
  }
  
  public DisposableUidGenerator(int offset, int totalIdCount) {
    availableIds = new ArrayList<>(totalIdCount);
    
    for (int i = 0; i < totalIdCount; i++) {
      int id = i + offset;
      availableIds.add(id);
    }
  }
  
  public int getId() {
    return availableIds.remove(0);
  }
  
  public void dispose(int id) {
    availableIds.add(id);
  }
}
