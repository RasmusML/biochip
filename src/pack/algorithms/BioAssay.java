package pack.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BioAssay {
	
	public String name;
	public Node sink;
	
	public void traverse(Consumer<Node> fn, Node sink) {
		fn.accept(sink);
		
		for (Node node : sink.inputs) {
			traverse(fn, node);
		}
	}
	
	public int getRouteCount() {
		Wrapper<Integer> routes = new Wrapper<>();
		routes.value = 0;
		
		traverse(node -> routes.value += node.inputs.size(), sink);
		return routes.value;
	}
	
	public int getInputCount() {
		Wrapper<Integer> inputs = new Wrapper<>();
		inputs.value = 0;
		
		traverse(node -> { if ("input".equals(node.type)) inputs.value += 1; }, sink);
		
		return inputs.value;
	}
	
	public List<String> getInputSubstances() {
		List<String> substances = new ArrayList<>();
		traverse(node -> { if ("input".equals(node.type)) substances.add(node.substance); }, sink);
		return substances;
	}
	
	public List<Node> getNodesOfType(String type) {
		List<Node> desired = new ArrayList<>();
		traverse(node -> { if (type.equals(node.type)) desired.add(node); }, sink);
		return desired;
	}
	
	public List<Integer> getNodeIdsOfType(String type) {
		List<Integer> ids = new ArrayList<>();
		traverse(node -> { if (type.equals(node.type)) ids.add(node.id); }, sink);
		return ids;
	}

	
	public List<Node> getOperationalBase() {
		List<Node> operations = new ArrayList<>();
		
		traverse(node -> {
    	if ("input".equals(node.type)) return;
    	
    	for (Node child : node.inputs) {
    	  if (!"input".equals(child.type)) return;
    	}
    	
    	operations.add(node);
    	
    }, sink);
		
		return operations;
	}
	
	public Node getNode(int id) {
		Wrapper<Node> match = new Wrapper<>();
		traverse(node -> { if (node.id == id) match.value = node; }, sink);
		return match.value;
	}

	public List<Integer> getNodeIds() {
		List<Integer> ids = new ArrayList<>();
		traverse(node -> ids.add(node.id), sink);
		return ids;
	}

	private static class Wrapper<T> {
		public T value;
	}

  public List<Node> getNodes() {
    List<Node> nodes = new ArrayList<>();
    traverse(node -> nodes.add(node), sink);
    return nodes;
  }
}
