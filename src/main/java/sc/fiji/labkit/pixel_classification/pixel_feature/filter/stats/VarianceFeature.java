
package sc.fiji.labkit.pixel_classification.pixel_feature.filter.stats;

import sc.fiji.labkit.pixel_classification.pixel_feature.filter.AbstractGroupFeatureOp;
import sc.fiji.labkit.pixel_classification.pixel_feature.filter.FeatureOp;
import sc.fiji.labkit.pixel_classification.pixel_feature.settings.FeatureSetting;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = FeatureOp.class, label = "variance filters (for each sigma)")
public class VarianceFeature extends AbstractGroupFeatureOp {

	@Override
	protected List<FeatureSetting> initFeatures() {
		return globalSettings().sigmas().stream()
			.map(r -> new FeatureSetting(SingleVarianceFeature.class, "radius", r))
			.collect(Collectors.toList());
	}
}
