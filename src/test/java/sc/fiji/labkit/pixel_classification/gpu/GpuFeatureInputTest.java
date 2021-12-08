/*-
 * #%L
 * The implementation of the pixel classification algorithm, that is used the Labkit image segmentation plugin for Fiji.
 * %%
 * Copyright (C) 2017 - 2021 Matthias Arzt
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

package sc.fiji.labkit.pixel_classification.gpu;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.ImgLib2Assert;
import sc.fiji.labkit.pixel_classification.Utils;
import sc.fiji.labkit.pixel_classification.gpu.api.AbstractGpuTest;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuApi;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuPool;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuView;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.junit.After;
import org.junit.Test;

/**
 * Test {@link GpuFeatureInput}.
 */
public class GpuFeatureInputTest extends AbstractGpuTest {

	private final double[] pixelSize = new double[] { 1, 1 };

	@Test
	public void test() {
		RandomAccessibleInterval<FloatType> original = ArrayImgs.floats(new float[] { 1, 2, 3, 4 }, 4,
			1);
		FinalInterval interval = FinalInterval.createMinSize(1, 0, 2, 1);
		GpuFeatureInput featureInput = new GpuFeatureInput(gpu, original, interval, pixelSize);
		featureInput.prefetchOriginal(interval);
		RandomAccessibleInterval<FloatType> result = pull(featureInput.original(interval));
		RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { 2, 3 }, 2, 1);
		ImgLib2Assert.assertImageEquals(expected, result);
	}

	@Test
	public void testGauss() {
		RandomAccessible<FloatType> original = Utils.dirac(2);
		double sigma = 2;
		Interval targetInterval = FinalInterval.createMinSize(4, 0, 1, 1);
		Interval interval = FinalInterval.createMinSize(3, 0, 1, 1);
		GpuFeatureInput featureInput = new GpuFeatureInput(gpu, original, targetInterval, pixelSize);
		featureInput.prefetchGauss(sigma, interval);
		RandomAccessibleInterval<FloatType> result = pull(featureInput.gauss(sigma, interval));
		RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { (float) Utils
			.gauss(sigma, 3, 0) }, 1, 1);
		ImgLib2Assert.assertImageEqualsRealType(expected, result, 0.0001);
	}

	@Test
	public void testDerivative() {
		RandomAccessibleInterval<FloatType> original = Utils.create2dImage(Intervals.createMinMax(-10,
			-10, 10, 10), (x, y) -> 4 * x + 1 * y);
		int sigma = 1;
		FinalInterval interval = new FinalInterval(1, 1);
		GpuFeatureInput featureInput = new GpuFeatureInput(gpu, original, interval, pixelSize);
		featureInput.prefetchDerivative(sigma, 0, interval);
		featureInput.prefetchDerivative(sigma, 1, interval);
		RandomAccessibleInterval<FloatType> resultX = pull(featureInput.derivative(sigma, 0, interval));
		RandomAccessibleInterval<FloatType> resultY = pull(featureInput.derivative(sigma, 1, interval));
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 4 }, 1, 1), resultX,
			0.0001);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 1 }, 1, 1), resultY,
			0.0001);
	}

	@Test
	public void testSecondDerivative() {
		RandomAccessibleInterval<FloatType> original = Utils.create2dImage(Intervals.createMinMax(-10,
			-10, 10, 10), (x, y) -> 4 * x * x + 1 * y * y + 3 * x * y);
		int sigma = 1;
		FinalInterval interval = new FinalInterval(1, 1);
		GpuFeatureInput featureInput = new GpuFeatureInput(gpu, original, interval, pixelSize);
		featureInput.prefetchSecondDerivative(sigma, 0, 0, interval);
		featureInput.prefetchSecondDerivative(sigma, 1, 1, interval);
		featureInput.prefetchSecondDerivative(sigma, 0, 1, interval);
		featureInput.prefetchSecondDerivative(sigma, 1, 0, interval);
		RandomAccessibleInterval<FloatType> resultXX = pull(featureInput.secondDerivative(sigma, 0, 0,
			interval));
		RandomAccessibleInterval<FloatType> resultYY = pull(featureInput.secondDerivative(sigma, 1, 1,
			interval));
		RandomAccessibleInterval<FloatType> resultXY = pull(featureInput.secondDerivative(sigma, 0, 1,
			interval));
		RandomAccessibleInterval<FloatType> resultYX = pull(featureInput.secondDerivative(sigma, 1, 0,
			interval));
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 8 }, 1, 1), resultXX,
			0.0001);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 2 }, 1, 1), resultYY,
			0.0001);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 3 }, 1, 1), resultXY,
			0.0001);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 3 }, 1, 1), resultYX,
			0.0001);
	}

	private RandomAccessibleInterval<FloatType> pull(GpuView result) {
		return gpu.pullRAI(GpuViews.asGpuImage(gpu, result));
	}
}
