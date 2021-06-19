package dmb.algorithms;

import java.util.ArrayList;
import java.util.List;

import dmb.components.module.Module;
import framework.input.Droplet;

public class RoutingResult {
  public boolean completed;
  public int executionTime;

  public List<Droplet> droplets = new ArrayList<>();
  public List<Module> modules = new ArrayList<>();
}
