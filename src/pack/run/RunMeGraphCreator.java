package pack.run;

import java.io.IOException;

import pack.algorithms.BioAssay;
import pack.algorithms.GraphvizUtil;
import pack.tests.HeatingBioAssay;

public class RunMeGraphCreator {

	public static void main(String[] args) throws IOException, InterruptedException {
		BioAssay assay = new HeatingBioAssay();
		
		String graph = assay.asGraphvizGraph();
		System.out.println(graph);
		String name = assay.name.replace(" ", "");
		
		String pngPath = String.format("./assays/%s.png", name);
		
		String graphvizPath = "C:\\Program Files (x86)\\Graphviz";
		
		GraphvizUtil.createPngFromString(graph, pngPath, graphvizPath);
	}
}
