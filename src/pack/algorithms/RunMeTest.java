package pack.algorithms;

import java.util.List;

public class RunMeTest {

	public static void main(String[] args) {
		BioAssay assay = new MergeBioAssay();
		BioArray array = new MergeBioArray();
		MixingPercentages percentages = new MixingPercentages();
		
		MergeRouter router = new MergeRouter();
		List<Route> routes = router.compute(assay, array, percentages);
	}
}
