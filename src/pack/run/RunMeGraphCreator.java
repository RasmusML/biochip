package pack.run;

import java.io.IOException;

import pack.algorithms.BioAssay;
import pack.helpers.GraphvizUtil;
import pack.tests.ModuleBioAssay1;

public class RunMeGraphCreator {

	public static void main(String[] args) throws IOException, InterruptedException {
		BioAssay assay = new ModuleBioAssay1();
		
		String graph = assay.asGraphvizGraph();
		System.out.println(graph);
		String name = assay.name.replace(" ", "");
		
		String pngPath = String.format("./assays/%s.png", name);
		
		String graphvizPath = "C:\\Program Files (x86)\\Graphviz";
		
		GraphvizUtil.createPngFromString(graph, pngPath, graphvizPath);
	}
}
