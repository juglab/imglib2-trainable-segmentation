
package sc.fiji.labkit.pixel_classification.pixel_feature.filter.gradient;

import sc.fiji.labkit.pixel_classification.pixel_feature.calculator.FeatureCalculator;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GaussianGradientMagnitudeFeatureTest {

	private final FeatureCalculator calculator = FeatureCalculator.default2d()
		.sigmas(1.0, 2.0)
		.addFeature(GaussianGradientMagnitudeFeature.class).build();

	@Test
	public void testAttributes() {
		List<String> attributeLabels = calculator.attributeLabels();
		List<String> expected = Arrays.asList(
			"gaussian gradient magnitude sigma=1.0",
			"gaussian gradient magnitude sigma=2.0");
		assertEquals(expected, attributeLabels);
	}
}
