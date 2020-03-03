
package net.imglib2.trainable_segmention.pixel_feature.filter;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.imagej.ops.Op;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.trainable_segmention.clij_random_forest.CLIJView;
import net.imglib2.trainable_segmention.pixel_feature.settings.GlobalSettings;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.plugin.SciJavaPlugin;

import java.util.List;

/**
 * @author Matthias Arzt
 */
public interface FeatureOp extends SciJavaPlugin, Op,
	UnaryFunctionOp<RandomAccessibleInterval<FloatType>, RandomAccessibleInterval<FloatType>>
{

	int count();

	List<String> attributeLabels();

	default void apply(RandomAccessible<FloatType> input,
		List<RandomAccessibleInterval<FloatType>> output)
	{
		apply(new FeatureInput(input, output.get(0), globalSettings().pixelSizeAsDoubleArray()),
			output);
	}

	void apply(FeatureInput input, List<RandomAccessibleInterval<FloatType>> output);

	default void applyWithCLIJ(CLIJ2 clij, FeatureInput input, List<CLIJView> output) {
		throw new UnsupportedOperationException();
	}

	GlobalSettings globalSettings();

	default boolean checkGlobalSettings(GlobalSettings globals) {
		return true;
	}
}
