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

package sc.fiji.labkit.pixel_classification.gpu.api;

import net.imglib2.Dimensions;

/**
 * A {@link GpuView} as a crop of a {@link GpuImage}.
 * <p>
 * A {@link GpuView} can also be a hyperSlice of a {@link GpuImage}, this is the
 * case if
 * {@code gpuView.dimensions().numDimensions() < gpuView.source().getDimensions().length}.
 * <p>
 *
 * @see GpuViews
 */
public class GpuView implements AutoCloseable {

	private final GpuImage source;
	private final Dimensions dimensions;
	private final long offset;

	GpuView(GpuImage source, Dimensions dimensions, long offset) {
		this.source = source;
		this.dimensions = dimensions;
		this.offset = offset;
	}

	/**
	 * Dimensions of the view.
	 */
	public Dimensions dimensions() {
		return dimensions;
	}

	/**
	 * @return underlying {@link GpuImage}. This should only be used by low level
	 *         functions.
	 */
	public GpuImage source() {
		return source;
	}

	/**
	 * @return The index of the pixel, in the underlying {@link GpuImage}, that is
	 *         considered as the origin of the image.
	 */
	public long offset() {
		return offset;
	}

	/**
	 * Closes the underlying {@link GpuImage}.
	 */
	@Override
	public void close() {
		source.close();
	}
}
