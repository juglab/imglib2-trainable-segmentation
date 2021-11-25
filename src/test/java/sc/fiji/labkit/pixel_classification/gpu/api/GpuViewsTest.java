
package sc.fiji.labkit.pixel_classification.gpu.api;

import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link GpuViews}.
 */
public class GpuViewsTest extends AbstractGpuTest {

	@Test
	public void testWrap() {
		GpuImage image = gpu.create(new long[] { 2, 3 }, NativeTypeEnum.Float);
		GpuView view = GpuViews.wrap(image);
		assertArrayEquals(new long[] { 2, 3 }, Intervals.dimensionsAsLongArray(view.dimensions()));
		assertSame(image, view.source());
		assertEquals(0, view.offset());
	}

	@Test
	public void testCrop() {
		GpuImage image = gpu.create(new long[] { 6, 4 }, NativeTypeEnum.Float);
		GpuView view = GpuViews.crop(image, FinalInterval.createMinSize(1, 2, 3, 1));
		assertArrayEquals(new long[] { 3, 1 }, Intervals.dimensionsAsLongArray(view.dimensions()));
		assertSame(image, view.source());
		assertEquals(13, view.offset());
	}

	@Test
	public void testChannel() {
		GpuImage image = gpu.create(new long[] { 6, 4 }, 2, NativeTypeEnum.Float);
		List<GpuView> channels = GpuViews.channels(image);
		GpuView channel = channels.get(1);
		assertEquals(2, channels.size());
		assertArrayEquals(new long[] { 6, 4 }, Intervals.dimensionsAsLongArray(channel.dimensions()));
		assertSame(image, channel.source());
		assertEquals(24, channel.offset());
	}
}
