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

package sc.fiji.labkit.pixel_classification.pixel_feature.filter.deprecated.dog;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import sc.fiji.labkit.pixel_classification.pixel_feature.filter.AbstractFeatureOp;
import sc.fiji.labkit.pixel_classification.pixel_feature.filter.FeatureInput;
import sc.fiji.labkit.pixel_classification.pixel_feature.filter.FeatureOp;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * @author Matthias Arzt
 */
@Deprecated
@Plugin(type = FeatureOp.class, label = "Difference of Gaussians")
public class SingleDifferenceOfGaussiansFeature extends AbstractFeatureOp {

	@Parameter
	private double sigma1 = 2.0;

	@Parameter
	private double sigma2 = 4.0;

	@Override
	public int count() {
		return 1;
	}

	@Override
	public void apply(FeatureInput in, List<RandomAccessibleInterval<FloatType>> out) {
		dog(in, out.get(0));
	}

	@Override
	public List<String> attributeLabels() {
		return Collections.singletonList("Difference_of_gaussians_" + sigma1 + "_" + sigma2);
	}

	private void dog(FeatureInput in, RandomAccessibleInterval<FloatType> out) {
		LoopBuilder.setImages(in.gauss(sigma2 * 0.4), in.gauss(sigma1 * 0.4), out)
			.forEachPixel((a, b, r) -> r.setReal(a.getRealFloat() - b.getRealFloat()));
	}
}
