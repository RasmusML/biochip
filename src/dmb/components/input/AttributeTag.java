package dmb.components.input;

/**
 * Operations and modules may have optional attributes, such as target temperature for heating operations and heaters.
 */

public class AttributeTag {
  public String key;
  public Object value;

  public AttributeTag(String key, Object value) {
    this.key = key;
    this.value = value;
  }
}