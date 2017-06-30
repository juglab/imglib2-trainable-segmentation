package net.imglib2.algorithm.features;

import net.imglib2.*;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static net.imglib2.algorithm.features.RevampUtils.nCopies;

/**
 * @author Matthias Arzt
 */
public class LipschitzFeature {

	private LipschitzFeature() {
		// prevent from being instantiated
	}

	public static Feature group(long border) {
		return new FeatureGroup(initFeatures(border));
	}

	private static List<Feature> initFeatures(long border) {
		return Arrays.stream(new double[]{5, 10, 15, 20, 25})
				.mapToObj(slope -> new SingleLipschitzFeature(slope, border))
				.collect(Collectors.toList());
	}

	public static Feature single(double slope, long border) {
		return new SingleLipschitzFeature(slope, border);
	}

	/**
	 * @author Matthias Arzt
	 */
	static class SingleLipschitzFeature implements Feature {

		private final double slope;

		private final long border;

		public SingleLipschitzFeature(double slope, long border) {
			this.slope = slope;
			this.border = border;
		}

		@Override
		public int count() {
			return 1;
		}

		@Override
		public void apply(RandomAccessible<FloatType> in, List<RandomAccessibleInterval<FloatType>> out) {
			apply(in, out.get(0));
		}

		private void apply(RandomAccessible<FloatType> in, RandomAccessibleInterval<FloatType> out) {
			Interval expandedInterval = Intervals.expand(out, nCopies(out.numDimensions(), border));
			Img<FloatType> tmp = RevampUtils.ops().create().img(expandedInterval, new FloatType());
			copy(tmp, in);
			lipschitz(tmp);
			outEquals255PlusAMinusB(Views.iterable(out), in, tmp); // out = 255 + in - tmp
		}

		private <T extends Type<T>> void copy(IterableInterval<T> out, RandomAccessible<T> in) {
			Cursor<T> o = out.cursor();
			RandomAccess<T> a = in.randomAccess();
			while(o.hasNext()) {
				o.fwd();
				a.setPosition(o);
				o.get().set(a.get());
			}
		}

		private <T extends RealType<T>> void outEquals255PlusAMinusB(IterableInterval<T> out, RandomAccessible<T> A, RandomAccessible<T> B) {
			Cursor<T> o = out.cursor();
			RandomAccess<T> a = A.randomAccess();
			RandomAccess<T> b = B.randomAccess();
			T offset = a.get().createVariable();
			offset.setReal(255);
			while(o.hasNext()) {
				o.fwd();
				a.setPosition(o);
				b.setPosition(o);
				T ov = o.get();
				ov.set(offset);
				ov.sub(b.get());
				ov.add(a.get());
			}
		}

		@Override
		public List<String> attributeLabels() {
			return Collections.singletonList("Lipschitz_true_true_" + slope);
		}

		<T extends RealType<T>> void lipschitz(RandomAccessibleInterval<T> inOut) {
			int n = inOut.numDimensions();
			Interval interval = new FinalInterval(nCopies(n, -1), nCopies(n, 1));
			for(Localizable location : RevampUtils.neigborsLocations(n)) {
				if(!isZero(location))
					forward(Views.extendBorder(inOut), inOut, locationToArray(location));
			}
		}

		<T extends RealType<T>> void forward(final RandomAccessible<T> in, final RandomAccessibleInterval<T> out, final long[] translation) {
			final double slope = RevampUtils.distance(new Point(out.numDimensions()), Point.wrap(translation)) * this.slope;
			final RandomAccessibleInterval<Pair<T, T>> pair = invert(Views.interval(Views.pair(Views.translate(in, translation), out), out), translation);
			Views.flatIterable(pair).forEach(p -> p.getB().setReal(
					Math.max(p.getB().getRealDouble(), p.getA().getRealDouble() - slope)
			));
		}

		private boolean isZero(Localizable location) {
			return locationToStream(location).allMatch(x -> x == 0);
		}

		private long[] locationToArray(Localizable cursor) {
			return locationToStream(cursor).toArray();
		}

		private LongStream locationToStream(Localizable cursor) {
			return IntStream.range(0, cursor.numDimensions()).mapToLong(cursor::getLongPosition);
		}

		private <T> RandomAccessibleInterval<T> invert(RandomAccessibleInterval<T> pair, long[] translation) {
			for(int i = pair.numDimensions() - 1; i >= 0; i--)
				if(translation[i] < 0)
					return Views.invertAxis(pair, i);
			return pair;
		}

	}
}
