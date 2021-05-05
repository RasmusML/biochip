package pack.algorithms.components;

import java.util.Random;

public class RandomIndexSelector {

  private Random random;

  public RandomIndexSelector() {
    random = RandomUtil.get();
  }

  public int select(float[] weights) {
    float totalWeight = 0;
    for (float weight : weights) {
      totalWeight += weight;
    }

    float cutOffPercentage = random.nextFloat();
    float cumulatedPercentage = 0;

    for (int index = 0; index < weights.length - 1; index++) {
      float probability = weights[index] / totalWeight;
      cumulatedPercentage += probability;
      if (cumulatedPercentage >= cutOffPercentage) return index;
    }

    return weights.length - 1;
  }

  public int selectUniformly(int buckets) {
    return randomInt(0, buckets - 1);
  }

  /**
   * @param min
   * @param max
   * @return random integer in the range [min;max] (both inclusive)
   */
  private int randomInt(int min, int max) {
    return (int) (min + random.nextFloat() * ((max + 1) - min));
  }
}