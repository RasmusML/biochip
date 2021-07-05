package dmb.components.moves;

import java.util.List;

import dmb.components.Droplet;
import dmb.components.DropletUnit;
import dmb.components.input.BioArray;
import dmb.components.module.Module;

/**
 * Computes the valid moves for a droplet at a timestep.
 */

public abstract class MoveFinder {

  // a couple of overloads with "default" arguments for convenience.

  public List<Move> getValidMoves(Droplet droplet, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, null, null, timestamp, droplets, modules, array);
  }

  public List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, mergeSibling, null, timestamp, droplets, modules, array);
  }

  public List<Move> getValidMoves(Droplet droplet, Module module, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(droplet, null, module, timestamp, droplets, modules, array);
  }

  public List<Move> getValidMoves(DropletUnit unit, Droplet droplet, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array) {
    return getValidMoves(unit, droplet, null, timestamp, droplets, modules, array);
  }

  // specific for the assumption on how droplets move, e.g. can droplets occupy multiple cells or not.
  public abstract List<Move> getValidMoves(Droplet droplet, Droplet mergeSibling, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array);

  public abstract List<Move> getValidMoves(DropletUnit dropletUnit, Droplet droplet, Module targetModule, int timestamp, List<Droplet> droplets, List<Module> modules, BioArray array);

}
