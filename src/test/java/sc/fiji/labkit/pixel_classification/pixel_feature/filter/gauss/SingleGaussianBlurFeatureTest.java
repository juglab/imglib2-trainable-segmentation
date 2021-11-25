
package net.imglib2.trainable_segmentation.pixel_feature.filter.gauss;

import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.trainable_segmentation.Utils;
import net.imglib2.trainable_segmentation.pixel_feature.calculator.FeatureCalculator;
import net.imglib2.trainable_segmentation.pixel_feature.filter.SingleFeatures;
import net.imglib2.trainable_segmentation.utils.CpuGpuRunner;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(CpuGpuRunner.class)
public class SingleGaussianBlurFeatureTest {

	public SingleGaussianBlurFeatureTest(boolean useGpu) {
		calculator.setUseGpu(useGpu);
		calculator3d.setUseGpu(useGpu);
	}

	private final double sigma = 5.0;

	private final FeatureCalculator calculator = FeatureCalculator.default2d()
		.addFeature(SingleFeatures.gauss(sigma))
		.build();

	private final FeatureCalculator calculator3d = FeatureCalculator.default2d()
		.dimensions(3)
		.addFeature(SingleFeatures.gauss(sigma))
		.build();

	@Test
	public void test() {
		RandomAccessible<FloatType> input = Utils.dirac(2);
		RandomAccessibleInterval<FloatType> output = ArrayImgs.floats(5, 5);
		RandomAccessibleInterval<FloatType> expected =
			Utils.create2dImage(output, (x, y) -> Utils.gauss(sigma, x, y));
		calculator.apply(input, Views.addDimension(output, 0, 0));
		ImgLib2Assert.assertImageEqualsRealType(expected, output, 0.001);
	}

	@Test
	public void test3D() {
		RandomAccessible<FloatType> input = Utils.dirac(3);
		FinalInterval interval = new FinalInterval(5, 5, 5);
		RandomAccessibleInterval<FloatType> result = Views.hyperSlice(calculator3d.apply(input,
			interval), 3, 0);
		RandomAccessibleInterval<FloatType> expected = Utils.create3dImage(interval, (x, y, z) -> Utils
			.gauss(sigma, x, y, z));
		Utils.assertImagesEqual(40, expected, result);
	}

	@Test
	public void testAttributeLabels() {
		List<String> attributes = calculator.attributeLabels();
		List<String> expected = Collections.singletonList("gaussian blur sigma=5.0");
		assertEquals(expected, attributes);
	}
}
