package dmb.testbench.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmb.algorithms.Operation;
import dmb.algorithms.OperationType;
import dmb.components.Droplet;
import dmb.components.input.AttributeTags;
import dmb.components.input.BioAssay;
import dmb.helpers.ArrayUtils;
import dmb.helpers.UidGenerator;

/**
 * An API to connect and create operations for an assay.
 * 
 * Operations are identified with an unique ID by the API. The IDs are used to
 * connect one operation to another operation.
 * 
 * An operation may be connected multiple times if it has multiple predecessor
 * or successor operation, e.g. split operations and merge operations.
 * 
 * @see BioAssay
 */

public class BioAssayBuilder {

  private UidGenerator generator;
  private Map<Integer, Operation> idToOperation;

  public BioAssayBuilder() {
    generator = new UidGenerator();
    idToOperation = new HashMap<>();
  }

  /**
   * Creates a dispense operation.
   * 
   * @param substance - droplet substance
   * @return ID
   */
  public int createDispenseOperation(String substance) {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.dispense;
    operation.inputs = new Operation[0];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[1];

    Map<String, Object> attributes = operation.attributes;
    attributes.put(AttributeTags.substance, substance);

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Creates a mixing operation.
   * 
   * @return ID
   */
  public int createMixOperation() {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.mix;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[1];

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Creates a merge operation.
   * 
   * @return ID
   */
  public int createMergeOperation() {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.merge;
    operation.inputs = new Operation[2];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[2];
    operation.forwarding = new Droplet[1];

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Creates a split operation.
   * 
   * @return ID
   */
  public int createSplitOperation() {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.split;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[2];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[2];

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Creates a dispose operation.
   * 
   * @return ID
   */
  public int createDisposeOperation() {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.dispose;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[0];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[0];

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Creates a heating operation.
   * 
   * @param temperature - the target temperature of the droplet
   * @return ID
   */
  public int createHeatingOperation(float temperature) {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.heating;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[1];

    Map<String, Object> attributes = operation.attributes;
    attributes.put(AttributeTags.temperature, temperature);

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Creates a detection operation.
   * 
   * @param sensor - sensor type
   * @return ID
   */
  public int createDetectionOperation(String sensor) {
    int id = generator.getId();

    Operation operation = new Operation();
    operation.id = id;
    operation.name = OperationType.detection;
    operation.inputs = new Operation[1];
    operation.outputs = new Operation[1];
    operation.manipulating = new Droplet[1];
    operation.forwarding = new Droplet[1];

    Map<String, Object> attributes = operation.attributes;
    attributes.put(AttributeTags.sensor, sensor);

    idToOperation.put(id, operation);

    return id;
  }

  /**
   * Connects operation with ID <code>fromId</code> to operation with ID
   * <code>toId</code>.
   * 
   * @param fromId - input operation id
   * @param toId   - output operation id
   */
  public void connect(int fromId, int toId) {
    Operation from = idToOperation.get(fromId);
    Operation to = idToOperation.get(toId);

    int toInput = ArrayUtils.getFirstEmptySlotIndex(to.inputs);
    int fromOutput = ArrayUtils.getFirstEmptySlotIndex(from.outputs);

    if (toInput == -1) {
      String error = String.format("all %d input operations of operation %d (%s) are occupied.", to.inputs.length, to.id, to.name);
      throw new IllegalStateException(error);
    }

    if (fromOutput == -1) {
      String error = String.format("all %d output operations of operation %d (%s) are occupied.", from.outputs.length, from.id, from.name);
      throw new IllegalStateException(error);
    }

    to.inputs[toInput] = from;
    from.outputs[fromOutput] = to;
  }

  public int getOperationCount() {
    return idToOperation.size();
  }

  /**
   * @return all leaf operations
   */
  public Operation[] getSink() {
    List<Operation> finalOperations = new ArrayList<>();

    for (Operation operation : idToOperation.values()) {

      if (operation.outputs.length == 0) {
        finalOperations.add(operation);
      } else {
        for (Operation output : operation.outputs) {
          if (output == null) {
            finalOperations.add(operation);
            break;
          }
        }
      }
    }

    return finalOperations.toArray(new Operation[0]);
  }
}
