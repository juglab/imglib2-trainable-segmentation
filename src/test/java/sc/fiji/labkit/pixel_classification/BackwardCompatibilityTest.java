/*-
 * #%L
 * The implementation of the pixel classification algorithm, that is used the Labkit image segmentation plugin for Fiji.
 * %%
 * Copyright (C) 2017 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.labkit.pixel_classification;

import ij.IJ;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import sc.fiji.labkit.pixel_classification.classification.Segmenter;
import sc.fiji.labkit.pixel_classification.gson.GsonUtils;
import sc.fiji.labkit.pixel_classification.utils.SingletonContext;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.junit.Test;
import org.scijava.Context;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test if a saved classifier gives the expected features and segmentation.
 */
public class BackwardCompatibilityTest {

	private final Context context = SingletonContext.getInstance();

	@Test
	public void testSaved2dClassifier() {
		Interval interval = new FinalInterval(100, 100);
		testSavedClassifier("compatibility/2d/", interval);
	}

	@Test
	public void testSaved3dClassifier() throws InterruptedException {
		FinalInterval interval = new FinalInterval(40, 40, 40);
		testSavedClassifier("compatibility/3d/", interval);
	}

	private void testSavedClassifier(String folder, Interval interval) {
		Segmenter segmenter = openSegmenter(folder);
		Img<FloatType> image = ImageJFunctions.wrapFloat(Utils.loadImage(folder + "test-image.tif"));
		testFeatures(folder, segmenter, image, interval);
		testSegmentation(folder, segmenter, image, interval);
	}

	private void testFeatures(String folder, Segmenter segmenter, Img<FloatType> image,
		Interval interval)
	{
		List<RandomAccessibleInterval<FloatType>> features = RevampUtils.slices(segmenter.features()
			.apply(Views.extendBorder(image), interval));
		List<RandomAccessibleInterval<FloatType>> expected = RevampUtils.slices(ImageJFunctions
			.wrapFloat(Utils.loadImage(folder + "expected-features.tif")));
		List<String> attributeLabels = segmenter.features().attributeLabels();
		assertEquals(expected.size(), features.size());
		for (int i = 0; i < expected.size(); i++) {
			RandomAccessibleInterval<FloatType> feature = features.get(i);
			RandomAccessibleInterval<FloatType> expectedFeature = expected.get(i);
			double psnr = Utils.psnr(expectedFeature, feature);
			if (psnr < 30) fail("Feature " + attributeLabels.get(i) + " PSNR: " + psnr + " is to low.");
		}
	}

	private void testSegmentation(String folder, Segmenter segmenter, Img<FloatType> image,
		Interval interval)
	{
		Img<UnsignedByteType> result = ArrayImgs.unsignedBytes(Intervals.dimensionsAsLongArray(
			interval));
		segmenter.segment(result, Views.extendBorder(image));
		Img<UnsignedShortType> expected = ImageJFunctions.wrapShort(Utils.loadImage(folder +
			"expected-segmentation.tif"));
		Utils.assertImagesEqual(30, expected, result);
	}

	private Segmenter openSegmenter(String folder) {
		InputStream inputStream = BackwardCompatibilityTest.class.getResourceAsStream("/" + folder +
			"test.classifier");
		return Segmenter.fromJson(context, GsonUtils.read(inputStream));
	}

	private void save2dTestImage() {
		Img<FloatType> image = testImage(100, 100);
		IJ.save(ImageJFunctions.wrap(image, ""), "test-image.tif");
	}

	public void save3dTestImage() {
		Img<FloatType> image = testImage(100, 100, 100);
		IJ.save(ImageJFunctions.wrap(image, ""), "test-image.tif");
	}

	private static Img<FloatType> testImage(long... dims) {
		Img<FloatType> image = ArrayImgs.floats(dims);
		addScaledNoise(image, 2);
		addScaledNoise(image, 5);
		addScaledNoise(image, 10);
		return image;
	}

	private static Img<FloatType> addScaledNoise(Img<FloatType> result, int step) {
		Img<FloatType> image = ArrayImgs.floats(Intervals.dimensionsAsLongArray(result));
		Random random = new Random(42);
		float intensity = step * step;
		Views.iterable(Views.subsample(image, step)).forEach(pixel -> pixel.setReal(random.nextFloat() *
			intensity));
		Gauss3.gauss(step, Views.extendZero(image), image);
		LoopBuilder.setImages(result, image).forEachPixel(FloatType::add);
		return result;
	}
}
