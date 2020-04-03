
package clij;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.trainable_segmention.clij_random_forest.CLIJView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import org.junit.Test;

/**
 * Tests {@link CLIJLoopBuilder}.
 */
public class CLIJLoopBuilderTest {

	private final GpuApi gpu = GpuApi.getInstance();

	@Test
	public void testAdd() {
		long[] dims = { 2, 2 };
		ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1, 2, 3, 4 }, dims));
		ClearCLBuffer b = gpu.push(ArrayImgs.floats(new float[] { 5, 6, 7, 8 }, dims));
		ClearCLBuffer c = gpu.create(dims, NativeTypeEnum.Float);
		add(a, b, c);
		RandomAccessibleInterval<RealType<?>> result = gpu.pullRAI(c);
		RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { 6, 8, 10, 12 },
			dims);
		ImgLib2Assert.assertImageEqualsRealType(expected, result, 0.0);
	}

	private void add(ClearCLBuffer a, ClearCLBuffer b, ClearCLBuffer dst) {
		CLIJLoopBuilder.gpu(gpu)
			.addInput("a", a)
			.addInput("b", b)
			.addOutput("c", dst)
			.forEachPixel("c = a + b");
	}

	@Test
	public void testSingleImageOperation() {
		long[] dims = { 2, 2 };
		ClearCLBuffer c = gpu.create(dims, NativeTypeEnum.Float);
		CLIJLoopBuilder.gpu(gpu)
			.addOutput("output", c)
			.forEachPixel("output = 2.0");
		RandomAccessibleInterval<RealType<?>> result = gpu.pullRAI(c);
		RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { 2, 2, 2, 2 }, 2,
			2);
		ImgLib2Assert.assertImageEqualsRealType(expected, result, 0.0);
	}

	@Test
	public void testTwoImageOperation() {
		long[] dims = { 2, 2 };
		ClearCLBuffer c = gpu.create(dims, NativeTypeEnum.Byte);
		ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1, 2, 3, 4 }, dims));
		CLIJLoopBuilder.gpu(gpu)
			.addInput("in", a)
			.addOutput("out", c)
			.forEachPixel("out = 2.0 * in");
		RandomAccessibleInterval<RealType<?>> result = gpu.pullRAI(c);
		RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { 2, 4, 6, 8 },
			dims);
		ImgLib2Assert.assertImageEqualsRealType(expected, result, 0.0);
	}

	@Test
	public void testMultipleOutputs() {
		long[] dims = { 2, 2 };
		ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1, 2, 3, 4 }, dims));
		ClearCLBuffer b = gpu.create(dims, NativeTypeEnum.Float);
		ClearCLBuffer c = gpu.create(dims, NativeTypeEnum.Float);
		CLIJLoopBuilder.gpu(gpu)
			.addInput("a", a)
			.addOutput("b", b)
			.addOutput("c", c)
			.forEachPixel("b = 2 * a; c = a + b");
		RandomAccessibleInterval<RealType<?>> resultB = gpu.pullRAI(b);
		RandomAccessibleInterval<RealType<?>> resultC = gpu.pullRAI(c);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 2, 4, 6, 8 }, dims),
			resultB, 0.0);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 3, 6, 9, 12 }, dims),
			resultC, 0.0);
	}

	@Test
	public void testFourImages() {
		ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1 }, 1, 1));
		ClearCLBuffer b = gpu.push(ArrayImgs.floats(new float[] { 2 }, 1, 1));
		ClearCLBuffer c = gpu.push(ArrayImgs.floats(new float[] { 3 }, 1, 1));
		ClearCLBuffer d = gpu.push(ArrayImgs.floats(new float[] { 0 }, 1, 1));
		CLIJLoopBuilder.gpu(gpu)
			.addInput("a", a)
			.addInput("b", b)
			.addInput("c", c)
			.addOutput("d", d)
			.forEachPixel("d = a + b + c");
		RandomAccessibleInterval<FloatType> result = gpu.pullRAI(d);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 6 }, 1, 1), result, 0.0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMismatchingDimensions() {
		ClearCLBuffer c = gpu.create(new long[] { 10, 10 }, NativeTypeEnum.Float);
		ClearCLBuffer b = gpu.create(new long[] { 10, 11 }, NativeTypeEnum.Float);
		CLIJLoopBuilder.gpu(gpu)
			.addInput("c", c)
			.addInput("b", b)
			.forEachPixel("b = c");
	}

	@Test
	public void testVariable() {
		ClearCLBuffer d = gpu.push(ArrayImgs.floats(new float[] { 0 }, 1, 1));
		CLIJLoopBuilder.gpu(gpu)
			.addInput("a", 42)
			.addOutput("d", d)
			.forEachPixel("d = a");
		RandomAccessibleInterval<FloatType> result = gpu.pullRAI(d);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 42 }, 1, 1), result,
			0.0);
	}

	@Test
	public void testFloatVariable() {
		ClearCLBuffer d = gpu.push(ArrayImgs.floats(new float[] { 0 }, 1, 1));
		CLIJLoopBuilder.gpu(gpu)
			.addInput("a", 42f)
			.addOutput("d", d)
			.forEachPixel("d = a");
		RandomAccessibleInterval<FloatType> result = gpu.pullRAI(d);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 42 }, 1, 1), result,
			0.0);
	}

	@Test
	public void testCLIJViewInput() {
		CLIJView a = CLIJView.interval(gpu.push(ArrayImgs.floats(new float[] { 0, 0, 0, 42 }, 2, 2)),
			Intervals.createMinSize(1, 1, 1, 1));
		ClearCLBuffer d = gpu.push(ArrayImgs.floats(new float[] { 0 }, 1, 1));
		CLIJLoopBuilder.gpu(gpu)
			.addInput("a", a)
			.addOutput("d", d)
			.forEachPixel("d = a");
		RandomAccessibleInterval<FloatType> result = gpu.pullRAI(d);
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 42 }, 1, 1), result,
			0.0);
	}

	@Test
	public void testCLIJViewOutput() {
		CLIJView a = CLIJView.interval(gpu.create(new long[] { 2, 2 }, NativeTypeEnum.Float), Intervals
			.createMinSize(1, 1, 1, 1));
		CLIJLoopBuilder.gpu(gpu)
			.addOutput("a", a)
			.forEachPixel("a = 42");
		RandomAccessibleInterval<FloatType> result = gpu.pullRAI(a.buffer());
		ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 0, 0, 0, 42 }, 2, 2),
			result, 0.0);
	}

	@Test
	public void testDifference() {
		try (
			ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1, 7, 8 }, 3, 1));
			ClearCLBuffer o = gpu.create(new long[] { 2, 1 }, NativeTypeEnum.Float);)
		{
			CLIJLoopBuilder.gpu(gpu)
				.addInput("a", CLIJView.interval(a, Intervals.createMinSize(1, 0, 2, 1)))
				.addInput("b", CLIJView.interval(a, Intervals.createMinSize(0, 0, 2, 1)))
				.addOutput("c", o)
				.forEachPixel("c = a - b");
			RandomAccessibleInterval<FloatType> result = gpu.pullRAI(o);
			ImgLib2Assert.assertImageEqualsRealType(ArrayImgs.floats(new float[] { 6, 1 }, 2, 1), result,
				0.0);
		}
	}

	@Test
	public void test3d() {
		try (
			ClearCLBuffer a = gpu.push(create3dImage(1));
			ClearCLBuffer b = gpu.push(create3dImage(2));
			ClearCLBuffer r = gpu.create(new long[] { 21, 21, 21 }, NativeTypeEnum.Float);)
		{
			CLIJLoopBuilder.gpu(gpu)
				.addInput("a", CLIJView.wrap(a))
				.addInput("b", CLIJView.wrap(b))
				.addOutput("r", CLIJView.wrap(r))
				.forEachPixel("r = a - b");
			ImgLib2Assert.assertImageEqualsRealType(create3dImage(-1), gpu.pullRAI(r), 0);
		}

	}

	private Img<FloatType> create3dImage(float factor) {
		Img<FloatType> image = ArrayImgs.floats(21, 21, 21);
		int i = 0;
		for (FloatType pixel : image)
			pixel.setReal((++i) * factor);
		return image;
	}

	@Test
	public void testSameImage() {
		try (
			ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1 }, 1, 1));
			ClearCLBuffer c = gpu.create(new long[] { 1, 1 }, NativeTypeEnum.Float);)
		{
			CLIJLoopBuilder.gpu(gpu)
				.addInput("a", a)
				.addInput("b", a)
				.addOutput("c", c)
				.forEachPixel("c = a + b");
			RandomAccessibleInterval<FloatType> result = gpu.pullRAI(c);
			RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { 2 }, 1, 1);
			ImgLib2Assert.assertImageEqualsRealType(expected, result, 0);
		}
	}

	@Test
	public void testSameBufferCLIJView() {
		try (
			ClearCLBuffer a = gpu.push(ArrayImgs.floats(new float[] { 1, 4 }, 2, 1));
			ClearCLBuffer c = gpu.create(new long[] { 1, 1 }, NativeTypeEnum.Float);)
		{
			CLIJLoopBuilder.gpu(gpu)
				.addInput("a", CLIJView.interval(a, Intervals.createMinSize(0, 0, 1, 1)))
				.addInput("b", CLIJView.interval(a, Intervals.createMinSize(1, 0, 1, 1)))
				.addOutput("c", c)
				.forEachPixel("c = a + b");
			RandomAccessibleInterval<FloatType> result = gpu.pullRAI(c);
			RandomAccessibleInterval<FloatType> expected = ArrayImgs.floats(new float[] { 5 }, 1, 1);
			ImgLib2Assert.assertImageEqualsRealType(expected, result, 0);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalVariableName() {
		CLIJLoopBuilder.gpu(gpu).addInput("float", 7).forEachPixel("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalVariableName2() {
		try (ClearCLBuffer image = gpu.create(new long[] { 1, 1 }, NativeTypeEnum.Float)) {
			CLIJLoopBuilder.gpu(gpu).addInput("float", image).forEachPixel("");
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testVariableNameClash() {
		try (ClearCLBuffer image = gpu.create(new long[] { 1, 1 }, NativeTypeEnum.Float)) {
			CLIJLoopBuilder.gpu(gpu).addInput("coordinate_x", image).forEachPixel("coordinate_x = 8");
		}
	}
}
