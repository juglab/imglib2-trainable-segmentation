
package net.imglib2.trainable_segmention.pixel_feature.calculator;

import clij.CLIJLoopBuilder;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.trainable_segmention.Utils;
import net.imglib2.trainable_segmention.clij_random_forest.CLIJFeatureInput;
import net.imglib2.trainable_segmention.clij_random_forest.CLIJView;
import net.imglib2.trainable_segmention.pixel_feature.filter.AbstractFeatureOp;
import net.imglib2.trainable_segmention.pixel_feature.filter.FeatureInput;
import net.imglib2.trainable_segmention.pixel_feature.filter.FeatureOp;
import net.imglib2.trainable_segmention.pixel_feature.settings.ChannelSetting;
import net.imglib2.trainable_segmention.pixel_feature.settings.FeatureSetting;
import net.imglib2.trainable_segmention.pixel_feature.settings.FeatureSettings;
import net.imglib2.trainable_segmention.pixel_feature.settings.GlobalSettings;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import preview.net.imglib2.loops.LoopBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FeatureCalculatorTest {

	public FeatureCalculatorTest(boolean useGpu) {
		this.useGpu = useGpu;
	}

	@Parameterized.Parameters(name = "useGpu = {0}")
	public static List<Boolean> data() {
		return Arrays.asList(false, true);
	}

	private final boolean useGpu;

	private final OpService ops = new Context().service(OpService.class);

	private final FeatureSetting add_42 = new FeatureSetting(AddValue.class, "value", 42);
	private final FeatureSetting add_12 = new FeatureSetting(AddValue.class, "value", 12);

	@Test
	public void test1() {
		GlobalSettings globalSettings = GlobalSettings.default2d().build();
		FeatureSettings settings = new FeatureSettings(globalSettings, add_42, add_12);
		FeatureCalculator calculator = new FeatureCalculator(ops, settings);
		calculator.setUseGPU(useGpu);
		Img<FloatType> input = ArrayImgs.floats(new float[] { 2 }, 1, 1);
		RandomAccessibleInterval<FloatType> out = calculator.apply(input);
		Utils.assertImagesEqual(ArrayImgs.floats(new float[] { 44, 14 }, 1, 1, 2), out);
	}

	@Test
	public void test2() {
		GlobalSettings globalSettings = GlobalSettings.default2d()
			.channels(ChannelSetting.multiple(2))
			.dimensions(2).sigmas(Collections.singletonList(1.0))
			.build();
		FeatureSettings settings = new FeatureSettings(globalSettings, add_42, add_12);
		FeatureCalculator calculator = new FeatureCalculator(ops, settings);
		calculator.setUseGPU(useGpu);
		Img<FloatType> input = ArrayImgs.floats(new float[] { 2, 3 }, 1, 1, 2);
		RandomAccessibleInterval<FloatType> out = calculator.apply(input);
		assertEquals(Arrays.asList("channel1_add_value_42.0", "channel2_add_value_42.0",
			"channel1_add_value_12.0", "channel2_add_value_12.0"),
			calculator.attributeLabels());
		Utils.assertImagesEqual(ArrayImgs.floats(new float[] { 44, 45, 14, 15 }, 1, 1, 4), out);
	}

	public static class AddValue extends AbstractFeatureOp implements FeatureOp {

		@Parameter
		double value;

		@Override
		public int count() {
			return 1;
		}

		@Override
		public List<String> attributeLabels() {
			return Collections.singletonList("add_value_" + value);
		}

		@Override
		public void apply(FeatureInput input,
			List<RandomAccessibleInterval<FloatType>> output)
		{
			IntervalView<FloatType> inputInterval = Views.interval(input.original(), output.get(0));
			LoopBuilder.setImages(inputInterval, output.get(0)).multiThreaded().forEachPixel(
				(in, out) -> out.set(in.get() + (float) value));
		}

		@Override
		public void prefetch(CLIJFeatureInput input) {
			input.prefetchOriginal(input.targetInterval());
		}

		@Override
		public void apply(CLIJFeatureInput input, List<CLIJView> output) {
			CLIJView image = input.original(input.targetInterval());
			CLIJLoopBuilder.clij(input.clij())
				.addInput("a", image)
				.addInput("b", (float) value)
				.addOutput("c", output.get(0)).forEachPixel("c = a + b");
		}
	}
}
