
package sc.fiji.labkit.pixel_classification.gpu.api;

import net.imglib2.util.Intervals;
import net.imglib2.util.ValuePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * {@link GpuPixelWiseOperation} provides a simple way to execute pixel wise
 * operations on images using CLIJ.
 */
public class GpuPixelWiseOperation {

	private List<String> COORDINATE_VARIABLE_NAMES = Arrays.asList("coordinate_x", "coordinate_y",
		"coordinate_z");

	private final GpuApi gpu;

	private final List<String> parameterDefinition = new ArrayList<>();
	private final List<String> preOperation = new ArrayList<>();
	private final List<String> postOperation = new ArrayList<>();
	private final HashMap<String, Object> parameterValues = new HashMap<>();
	private final Map<GpuImage, String> images = new HashMap<>();
	private final List<ValuePair<String, long[]>> imageSizes = new ArrayList<>();

	private GpuPixelWiseOperation(GpuApi gpu) {
		this.gpu = gpu;
	}

	public static GpuPixelWiseOperation gpu(GpuApi gpu) {
		return new GpuPixelWiseOperation(gpu);
	}

	public GpuPixelWiseOperation addInput(String variable, int image) {
		addParameter("int", variable, image);
		return this;
	}

	public GpuPixelWiseOperation addInput(String variable, long image) {
		addParameter("long", variable, image);
		return this;
	}

	public GpuPixelWiseOperation addInput(String variable, float image) {
		addParameter("float", variable, image);
		return this;
	}

	public GpuPixelWiseOperation addInput(String variable, double image) {
		addParameter("double", variable, image);
		return this;
	}

	public GpuPixelWiseOperation addInput(String variable, GpuImage image) {
		return addInput(variable, GpuViews.wrap(image));
	}

	public GpuPixelWiseOperation addOutput(String variable, GpuImage image) {
		return addOutput(variable, GpuViews.wrap(image));
	}

	public GpuPixelWiseOperation addInput(String variable, GpuView image) {
		String parameterName = addGpuViewParameter(variable, image);
		preOperation.add("IMAGE_" + parameterName + "_PIXEL_TYPE " + variable + " = " +
			pixelAt(parameterName, image.offset()));
		return this;
	}

	public GpuPixelWiseOperation addOutput(String variable, GpuView image) {
		String parameterName = addGpuViewParameter(variable, image);
		preOperation.add("IMAGE_" + parameterName + "_PIXEL_TYPE " + variable + " = 0");
		postOperation.add(pixelAt(parameterName, image.offset()) + " = " + variable);
		return this;
	}

	private String pixelAt(String parameterName, long offset) {
		return "PIXEL_OFFSET(" + parameterName + ", " + offset + ")";
	}

	private String addGpuViewParameter(String variable, GpuView image) {
		checkValidVariableName(variable);
		String parameterName = addImageParameter(image.source());
		registerSize(variable, Intervals.dimensionsAsLongArray(image.dimensions()));
		return parameterName;
	}

	private void registerSize(String variable, long[] size) {
		imageSizes.add(new ValuePair<>(variable, size));
	}

	private String addImageParameter(GpuImage image) {
		if (images.containsKey(image))
			return images.get(image);
		else {
			String parameterName = "image_" + (images.size() + 1);
			images.put(image, parameterName);
			addParameter("IMAGE_" + parameterName + "_TYPE ", parameterName, image);
			return parameterName;
		}
	}

	private void addParameter(String parameterType, String parameterName, Object value) {
		checkValidVariableName(parameterName);
		parameterDefinition.add(parameterType + " " + parameterName);
		parameterValues.put(parameterName, value);
	}

	private void checkValidVariableName(String variable) {
		if (COORDINATE_VARIABLE_NAMES.contains(variable) || !OpenCLSyntax.isValidVariableName(variable))
			throw new IllegalArgumentException("Sorry \"" + variable +
				"\" can not be used as a variable name.");
	}

	public void forEachPixel(String operation) {
		long[] dims = checkDimensions();
		HashMap<String, Object> defines = new HashMap<>();
		defines.put("PARAMETER", concatenate(", ", parameterDefinition));
		defines.put("OPERATION", concatenate("; ", preOperation, Collections.singletonList(operation),
			postOperation));
		gpu.execute(GpuPixelWiseOperation.class, "pixelwise_operation.cl", "operation", dims, null,
			parameterValues, defines);
	}

	@SafeVarargs
	private static String concatenate(String delimiter, List<String>... lists) {
		StringJoiner joiner = new StringJoiner(delimiter);
		for (List<String> values : lists)
			values.forEach(joiner::add);
		return joiner.toString();
	}

	private long[] checkDimensions() {
		long[] dims = imageSizes.get(0).getB();
		for (ValuePair<String, long[]> image : imageSizes) {
			if (!Arrays.equals(dims, image.getB()))
				wrongDimensionsError();
		}
		return dims;
	}

	private void wrongDimensionsError() {
		StringJoiner joiner = new StringJoiner(" ");
		for (ValuePair<String, long[]> pair : imageSizes) {
			String imageName = pair.getA();
			long[] imageSize = pair.getB();
			joiner.add("size(" + imageName + ")=" + Arrays.toString(imageSize));
		}
		throw new IllegalArgumentException("Error the sizes of the input images don't match: " +
			joiner);
	}
}
