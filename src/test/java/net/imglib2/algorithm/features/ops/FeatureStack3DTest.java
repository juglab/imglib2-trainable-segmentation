package net.imglib2.algorithm.features.ops;

import ij.ImagePlus;
import ij.ImageStack;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.features.FeatureGroup;
import net.imglib2.algorithm.features.FeatureSetting;
import net.imglib2.algorithm.features.FeatureStackTest;
import net.imglib2.algorithm.features.Features;
import net.imglib2.algorithm.features.GlobalSettings;
import net.imglib2.algorithm.features.SingleFeatures;
import net.imglib2.algorithm.features.Utils;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.junit.Test;
import org.scijava.Context;
import trainableSegmentation.FeatureStack3D;
import trainableSegmentation.FeatureStackArray;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Matthias Arzt
 */
public class FeatureStack3DTest {

	private Context context = new Context();
	private OpService ops = context.service(OpService.class);
	private Img<DoubleType> img = initSample();
	private ImagePlus image = ImageJFunctions.wrap(img, "input");

	private Img<DoubleType> initSample() {
		Img<DoubleType> img = ops.create().img(new long[]{20, 20, 20});
		String equation = "Math.sin(0.2 * p[0]) + Math.sin(0.1 * p[1]) + Math.cos(0.1 * p[2])";
		//String equation = "p[0] * p[0] + p[1] * p[1] + p[2] * p[2]";
		ops.image().equation(img, equation);
		return img;
	}

	@Test
	public void testHessian() {
		FeatureStackArray fsa = calculateFeatureStack(FeatureStack3D.HESSIAN);
		FeatureGroup group = setupFeatureGroup(FeatureSetting.fromClass(Hessian3DFeature.class));
		List<String> expectedLabels = oldAttributes(fsa);
		List<String> actualLabels = getAttributeLabels(group);
		assertEquals(expectedLabels, actualLabels);
		RandomAccessibleInterval<FloatType> expected = getImage(fsa);
		RandomAccessibleInterval<FloatType> result = Features.applyOnImg(group, img);
		Utils.assertImagesEqual(40, expected, result);
	}

	@Test
	public void testDerivatives() {
		FeatureStackArray fsa = calculateFeatureStack(FeatureStack3D.DERIVATIVES);
	}

	private FeatureGroup setupFeatureGroup(FeatureSetting featureSetting) {
		return Features.group(Utils.ops(), GlobalSettings.default3dSettings(), SingleFeatures.identity(), featureSetting);
	}

	private RandomAccessibleInterval<FloatType> getImage(FeatureStackArray fsa) {
		List<Img<FloatType>> slices = IntStream.range(0, fsa.getSize())
				.mapToObj(index -> stackToImg(fsa.get(index).getStack(), Integer.toString(index)))
				.collect(Collectors.toList());
		return Views.permute(Views.stack(slices), 2, 3);
	}

	private FeatureStackArray calculateFeatureStack(int featureIndex) {
		boolean[] enabledFeatures = new boolean[FeatureStack3D.availableFeatures.length];
		enabledFeatures[featureIndex] = true;
		FeatureStack3D stack = new FeatureStack3D(image.duplicate());
		stack.setEnableFeatures(enabledFeatures);
		stack.updateFeaturesMT();
		return stack.getFeatureStackArray();
	}

	private List<String> getAttributeLabels(FeatureGroup group) {
		return group.attributeLabels();
	}

	private List<String> oldAttributes(FeatureStackArray fsa) {
		return FeatureStackTest.oldAttributes(fsa.get(0));
	}

	private Img<FloatType> stackToImg(ImageStack stack1, String title) {
		return ImageJFunctions.wrap(new ImagePlus("title", stack1));
	}
}