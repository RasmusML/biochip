package dmb.components.module;

import java.util.HashMap;
import java.util.Map;

import dmb.algorithms.Point;

/**
 * Modules are the "implementation" which execute non-configurable operations.
 * Multiple modules may be able to execute the same non-configurable operation.
 * A module is defined by the non-configuration operation it executes, its size
 * and execution-time and possibly module-specific attributes.
 */

public class Module {
  public String operation;
  public int duration; // in timesteps for now.

  public Point position;
  public int width, height;

  public ModulePolicy policy;

  // same attributes as attributes of operations
  public Map<String, Object> attributes = new HashMap<>();
}