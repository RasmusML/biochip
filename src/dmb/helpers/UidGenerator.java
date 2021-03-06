package dmb.helpers;

/**
 * Creates unique ids.
 */

public class UidGenerator {
  
  private int nextId;
  
  public void reset() {
    nextId = 0;
  }
  
  public int getId() {
    int id = nextId;
    nextId += 1;
    return id;
  }
}
