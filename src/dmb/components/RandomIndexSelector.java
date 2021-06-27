package dmb.components;

import java.util.Random;

import dmb.helpers.RandomUtil;

/**
 * A set of functions which return a random index. Each index has a probability
 * to be selected.
 */

public class RandomIndexSelector {

  private Random random;

  public RandomIndexSelector() {
    random = RandomUtil.get();
  }

  /**
   * 
   * Selects an index at random based on the indices weights. All weights have to
   * >= 0.
   * 
   * The probability of selecting an index is the normalized value of the weights.
   * 
   * @param weights
   * @return index - chosen at random based on weights
   */
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

  /**
   * Selects an index at random (uniform).
   * 
   * @param buckets - number of indices
   * @return index - chosen at random
   */

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