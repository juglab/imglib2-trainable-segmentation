
package net.imglib2.trainable_segmention.pixel_feature.filter.gauss;

import net.imglib2.trainable_segmentation.pixel_feature.filter.AbstractSigmaGroupFeatureOp;
import net.imglib2.trainable_segmentation.pixel_feature.filter.FeatureOp;
import org.scijava.plugin.Plugin;

/**
 * @author Matthias Arzt
 */
@Deprecated
@Plugin(type = FeatureOp.class, label = "Gauss (for each sigma)")
public class GaussFeature extends AbstractSigmaGroupFeatureOp {

	public GaussFeature() {
		super(false);
	}

	@Override
	protected Class<? extends FeatureOp> getSingleFeatureClass() {
		return SingleGaussFeature.class;
	}
}
