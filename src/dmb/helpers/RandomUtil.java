package dmb.helpers;

import java.util.Random;

public class RandomUtil {
  
  private static Random random;
  private static long defaultSeed = 42;
  
  public static void init(long seed) {
    random = new Random(seed);
  }
  
  public static void init() {
    init(System.currentTimeMillis());
  }
  
  public static Random get() {
    if (random == null) init(defaultSeed);
    return random;
  }

  /**
   * @param min
   * @param max
   * @return random integer in the range [min;max] (both inclusive)
   */
  public static int randomInt(int min, int max) {
    return (int) (min + random.nextFloat() * ((max + 1) - min));
  }
}
