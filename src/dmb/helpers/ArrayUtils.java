package dmb.helpers;

public class ArrayUtils {

  public static <T> int getFirstEmptySlotIndex(T[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] == null) return i;
    }

    return -1;
  }

  public static <T> int countOccupiedSlots(T[] array) {
    int count = 0;
    for (int i = 0; i < array.length; i++) {
      if (array[i] != null) count += 1;
    }
    return count;
  }
}
