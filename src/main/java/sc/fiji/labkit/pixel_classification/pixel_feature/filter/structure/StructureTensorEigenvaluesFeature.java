
package sc.fiji.labkit.pixel_classification.pixel_feature.filter.structure;

import sc.fiji.labkit.pixel_classification.pixel_feature.filter.AbstractGroupFeatureOp;
import sc.fiji.labkit.pixel_classification.pixel_feature.filter.FeatureOp;
import sc.fiji.labkit.pixel_classification.pixel_feature.settings.FeatureSetting;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin(type = FeatureOp.class, label = "structure tensor eigenvalues (for each sigma)")
public class StructureTensorEigenvaluesFeature extends AbstractGroupFeatureOp {

	@Override
	protected List<FeatureSetting> initFeatures() {
		return globalSettings().sigmas().stream().flatMap(
			this::initFeaturesForSigma).collect(Collectors.toList());
	}

	private Stream<FeatureSetting> initFeaturesForSigma(Double sigma) {
		return Stream.of(1.0, 3.0).map(integrationScale -> new FeatureSetting(
			SingleStructureTensorEigenvaluesFeature.class, "sigma", sigma,
			"integrationScale", integrationScale));
	}
}
