package pack.run;

import java.io.IOException;

import pack.algorithms.BioAssay;
import pack.helpers.GraphvizUtil;
import pack.tests.CrowdedModuleBioAssay;

public class RunMeGraphCreator {

	public static void main(String[] args) throws IOException, InterruptedException {
	  String graphvizPath = "C:\\Program Files (x86)\\Graphviz";

	  BioAssay assay = new CrowdedModuleBioAssay();
		
		String graph = assay.asGraphvizGraph();
		
		String name = assay.name.replace(" ", "");
		String pngPath = String.format("./assays/%s.png", name);
		
		GraphvizUtil.createPngFromString(graph, pngPath, graphvizPath);
	}
}
