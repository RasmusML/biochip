package dmb.components.module;

/**
 * 
 * alwaysOpen - droplets may move on top of an module, whether or not it is currently allocated.
 * lockedOnOperation - droplets may move on top of an module if it not currently allocated.
 * alwaysLocked - only droplets allocating the module may be on top of the module.
 */

public enum ModulePolicy {
  alwaysOpen,
  lockedOnOperation,
  alwaysLocked;
}
