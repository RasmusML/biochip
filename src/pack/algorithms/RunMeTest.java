package pack.algorithms;

import java.io.IOException;

import pack.tests.Test2BioAssay;

public class RunMeTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		BioAssay assay = new Test2BioAssay();
		String graph = assay.asGraphvizGraph();
		String pngPath = String.format("./assays/%s.png", assay.name);
		String graphvizPath = "C:\\Program Files (x86)\\Graphviz";
		GraphvizUtil.createPngFromString(graph, pngPath, graphvizPath);
	}
}
