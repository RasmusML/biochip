package dmb.helpers;

public class Assert {

  public static void that(boolean is, String message) {
    if (!is) {
      throw new IllegalStateException(message);
    }
  }

  public static void that(boolean is) {
    that(is, "Assertions failed!");
  }
}
