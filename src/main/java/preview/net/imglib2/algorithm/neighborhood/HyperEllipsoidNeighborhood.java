/*
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

package preview.net.imglib2.algorithm.neighborhood;

import net.imglib2.AbstractEuclideanSpace;
import net.imglib2.AbstractLocalizable;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RealPositionable;
import net.imglib2.algorithm.neighborhood.Neighborhood;

import java.util.Iterator;

/**
 * TODO
 *
 * @author Tobias Pietzsch
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class HyperEllipsoidNeighborhood<T> extends AbstractLocalizable implements Neighborhood<T> {

	public static NeighborhoodFactory factory(double[] radius) {
		return new NeighborhoodFactory() {

			@Override
			public <T> Neighborhood<T> create(long[] position, RandomAccess<T> sourceRandomAccess) {
				return new HyperEllipsoidNeighborhood<>(position, radius, sourceRandomAccess);
			}
		};
	}

	private final RandomAccess<T> sourceRandomAccess;

	private final long[] radii;

	private final double[] radiusRatii;

	private final double lastRadius;

	private final int maxDim;

	private final long size;

	private final Interval structuringElementBoundingBox;

	HyperEllipsoidNeighborhood(final long[] position, final double[] radii,
		final RandomAccess<T> sourceRandomAccess)
	{
		super(position);
		this.sourceRandomAccess = sourceRandomAccess;
		this.radii = new long[radii.length];
		for (int i = 0; i < radii.length; i++) {
			this.radii[i] = (long) radii[i];
		}
		maxDim = n - 1;
		lastRadius = radii[maxDim];
		radiusRatii = new double[maxDim];
		for (int d = 0; d < maxDim; ++d) {
			radiusRatii[d] = (double) radii[d] / (double) radii[d + 1];
		}

		size = computeSize();

		final long[] min = new long[n];
		final long[] max = new long[n];

		for (int d = 0; d < n; d++) {
			min[d] = -this.radii[d];
			max[d] = this.radii[d];
		}

		structuringElementBoundingBox = new FinalInterval(min, max);
	}

	/**
	 * Compute the number of elements for iteration
	 */
	protected long computeSize() {
		final LocalCursor cursor = new LocalCursor(sourceRandomAccess);

		// "compute number of pixels"
		long size = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			++size;
		}

		return size;
	}

	public final class LocalCursor extends AbstractEuclideanSpace implements Cursor<T> {

		private final RandomAccess<T> source;

		// the current radius in each dimension we are at
		private final double[] r;

		// the current radius in each dimension truncated to long
		private final long[] ri;

		// the remaining number of steps in each dimension we still have to go
		private final long[] s;

		public LocalCursor(final RandomAccess<T> source) {
			super(source.numDimensions());
			this.source = source;
			r = new double[n];
			ri = new long[n];
			s = new long[n];
			reset();
		}

		protected LocalCursor(final LocalCursor c) {
			super(c.numDimensions());
			source = c.source.copyRandomAccess();
			r = c.r.clone();
			ri = c.ri.clone();
			s = c.s.clone();
		}

		@Override
		public T get() {
			return source.get();
		}

		@Override
		public void fwd() {

			if (--s[0] >= 0) {
				source.fwd(0);
			}
			else {
				int d = 1;
				for (; d < n; ++d) {
					if (--s[d] >= 0) {
						source.fwd(d);
						break;
					}
				}

				for (; d > 0; --d) {
					final int e = d - 1;
					final double rd = r[d];
					final long pd = s[d] - ri[d];

					// final double rad = Math.sqrt( rd * rd - pd * pd );

					final double rad = radiusRatii[e] * Math.sqrt(rd * rd - pd * pd);

					final long radi = (long) rad;
					r[e] = rad;
					ri[e] = radi;
					s[e] = 2 * radi;

					source.setPosition(position[e] - radi, e);
				}
			}
		}

		@Override
		public void jumpFwd(final long steps) {
			for (long i = 0; i < steps; ++i) {
				fwd();
			}
		}

		@Override
		public T next() {
			fwd();
			return get();
		}

		@Override
		public void remove() {
			// NB: no action.
		}

		@Override
		public void reset() {
			for (int d = 0; d < maxDim; ++d) {
				r[d] = ri[d] = s[d] = 0;
				source.setPosition(position[d], d);
			}

			source.setPosition(position[maxDim] - radii[maxDim] - 1, maxDim);

			r[maxDim] = lastRadius;
			ri[maxDim] = radii[maxDim];
			s[maxDim] = 1 + 2 * radii[maxDim];
		}

		@Override
		public boolean hasNext() {
			if (s[0] > 0)
				return true;
			for (int d = 1; d < n; ++d)
				if (s[d] > 0)
					return true;
			return false;
		}

		@Override
		public float getFloatPosition(final int d) {
			return source.getFloatPosition(d);
		}

		@Override
		public double getDoublePosition(final int d) {
			return source.getDoublePosition(d);
		}

		@Override
		public int getIntPosition(final int d) {
			return source.getIntPosition(d);
		}

		@Override
		public long getLongPosition(final int d) {
			return source.getLongPosition(d);
		}

		@Override
		public void localize(final long[] position) {
			source.localize(position);
		}

		@Override
		public void localize(final float[] position) {
			source.localize(position);
		}

		@Override
		public void localize(final double[] position) {
			source.localize(position);
		}

		@Override
		public void localize(final int[] position) {
			source.localize(position);
		}

		@Override
		public LocalCursor copy() {
			return new LocalCursor(this);
		}

		@Override
		public LocalCursor copyCursor() {
			return copy();
		}
	}

	@Override
	public Interval getStructuringElementBoundingBox() {
		return structuringElementBoundingBox;
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public T firstElement() {
		return cursor().next();
	}

	@Override
	public Object iterationOrder() {
		return this; // iteration order is only compatible with ourselves
	}

	@Override
	public Iterator<T> iterator() {
		return cursor();
	}

	@Override
	public long min(final int d) {
		return position[d] - radii[d];
	}

	@Override
	public long max(final int d) {
		return position[d] + radii[d];
	}

	public long dimension(final int d) {
		return 2 * radii[d] + 1;
	}

	@Override
	public LocalCursor cursor() {
		return new LocalCursor(sourceRandomAccess.copyRandomAccess());
	}

	@Override
	public LocalCursor localizingCursor() {
		return cursor();
	}

}
