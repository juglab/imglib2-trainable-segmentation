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

package sc.fiji.labkit.pixel_classification.pixel_feature.filter.stats;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.StackProcessor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.test.RandomImgs;
import sc.fiji.labkit.pixel_classification.pixel_feature.calculator.FeatureCalculator;
import sc.fiji.labkit.pixel_classification.pixel_feature.filter.deprecated.stats.SingleSphereShapedFeature;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.StopWatch;
import net.imglib2.view.Views;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark that compares the performance of max filters in IJ1, ops and
 * {@link MinMaxFilter}.
 */
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 4)
@Measurement(iterations = 4)
@BenchmarkMode(Mode.AverageTime)
public class MaxFilterBenchmark {

	private ImageStack inputStack = randomize(ImageStack.create(100, 100, 100, 32));
	private ImageStack outputStack = ImageStack.create(100, 100, 100, 32);

	private Img<FloatType> input = RandomImgs.seed(42).nextImage(new FloatType(), 100, 100, 100);
	private Img<FloatType> output = ArrayImgs.floats(100, 100, 100);

	private final int radius = 3;

	private ImageStack randomize(ImageStack stack) {
		RandomImgs.seed(42).randomize(ImageJFunctions.wrapFloat(new ImagePlus("", stack)));
		return stack;
	}

	@Benchmark
	public void benchMarkIJ1() {
		StackProcessor processor = new StackProcessor(inputStack);
		processor.filter3D(outputStack, radius, radius, radius, 1, 100, StackProcessor.FILTER_MAX);
	}

	@Benchmark
	public void benchmarkMaxFilter() {
		int width = 3 * radius + 1;
		MinMaxFilter.maxFilter(width, width, width).process(Views.extendBorder(input), output);
	}

	@Deprecated
	@Benchmark
	public void benchmarkOps() {
		FeatureCalculator calculator = FeatureCalculator.default2d()
			.dimensions(3)
			.addFeature(SingleSphereShapedFeature.class, "radius", 4,
				"operation", SingleSphereShapedFeature.MAX)
			.build();
		calculator.apply(Views.extendBorder(input), Views.addDimension(output, 0, 0));
	}

	// @Setup
	public void warmup() {
		for (int i = 0; i < 2; i++) {
			maxFilter(new IntType());
			maxFilter(new DoubleType());
			maxFilter(new FloatType());
			maxFilter(new UnsignedByteType());
		}
	}

	private <T extends RealType<T> & NativeType<T>> void maxFilter(T type) {
		Img<T> input = RandomImgs.seed(42).nextImage(type, 100, 100, 100);
		Img<T> output = new ArrayImgFactory<>(type).create(100, 100, 100);
		StopWatch stopWatch = StopWatch.createAndStart();
		int width = 3 * radius + 1;
		MinMaxFilter.maxFilter(width, width, width).process(Views.extendBorder(input), output);
		System.out.println(type.getClass().getSimpleName() + ": " + stopWatch);
	}

	public static void main(String... args) throws RunnerException {
		Options options = new OptionsBuilder().include(MaxFilterBenchmark.class.getSimpleName())
			.build();
		new Runner(options).run();
	}

}
