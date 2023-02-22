/*-
 * #%L
 * The implementation of the pixel classification algorithm, that is used the Labkit image segmentation plugin for Fiji.
 * %%
 * Copyright (C) 2017 - 2023 Matthias Arzt
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

package sc.fiji.labkit.pixel_classification.gpu.algorithms;

import sc.fiji.labkit.pixel_classification.gpu.api.GpuPixelWiseOperation;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuApi;
import sc.fiji.labkit.pixel_classification.gpu.api.GpuView;

import java.util.List;

public class GpuEigenvalues {

	public static void symmetric(GpuApi gpu, List<GpuView> matrix, List<GpuView> eigenvalues) {
		if (matrix.size() == 3 && eigenvalues.size() == 2)
			symmetric2d(gpu, matrix.get(0), matrix.get(1), matrix.get(2), eigenvalues.get(0), eigenvalues
				.get(1));
		else if (matrix.size() == 6 && eigenvalues.size() == 3)
			symmetric3d(gpu, matrix.get(0), matrix.get(1), matrix.get(2), matrix.get(3), matrix.get(4),
				matrix.get(5),
				eigenvalues.get(0), eigenvalues.get(1), eigenvalues.get(2));
		else
			throw new UnsupportedOperationException();
	}

	/**
	 * Calculates the eigenvalues of a symmetric 2x2-matrices. All the images
	 * provided: xx, xy, yy, eigenvalue1, eigenvalue2 must have the same size. For
	 * every position in the image the pixels of xx, xy, yy define a symmetric
	 * 2x2-matrix [xx, xy; xy, yy] of which the eigenvalues are calculated. The
	 * larger eigenvalue is written to eigenvalue1, and the smaller eigenvalue ist
	 * written to eigenvalue2.
	 */
	public static void symmetric2d(GpuApi gpu, GpuView xx, GpuView xy, GpuView yy,
		GpuView eigenvalue1, GpuView eigenvalue2)
	{
		GpuPixelWiseOperation.gpu(gpu)
			.addInput("s_xx", xx)
			.addInput("s_xy", xy)
			.addInput("s_yy", yy)
			.addOutput("l", eigenvalue1)
			.addOutput("s", eigenvalue2)
			.forEachPixel("float trace = s_xx + s_yy;" +
				"l = (float) (trace / 2.0 + sqrt(4 * s_xy * s_xy + (s_xx - s_yy) * (s_xx - s_yy)) / 2.0);" +
				"s = (float) (trace / 2.0 - sqrt(4 * s_xy * s_xy + (s_xx - s_yy) * (s_xx - s_yy)) / 2.0);");
	}

	/**
	 * Calculates the eigenvalues of the symmetric 3x3-matrices. All the images
	 * provided must have the same size. For every position in the image the pixels
	 * of xx, xy, zy, yy, yz and zz define a symmetric 3x3-matrix [xx, xy, xz; xy,
	 * yy, yz; xz, yz, zz] of which the eigenvalues are calculated. The largest
	 * eigenvalue is written to eigenvalue1, the middle eigenvalue is written to
	 * eigenvalue2, and the smallest eigenvalue ist written to eigenvalue3.
	 */
	public static void symmetric3d(GpuApi gpu, GpuView xx, GpuView xy, GpuView xz, GpuView yy,
		GpuView yz, GpuView zz, GpuView eigenvalue1, GpuView eigenvalue2, GpuView eigenvalue3)
	{

		GpuPixelWiseOperation.gpu(gpu)
			.addInput("s_xx", xx)
			.addInput("s_xy", xy)
			.addInput("s_xz", xz)
			.addInput("s_yy", yy)
			.addInput("s_yz", yz)
			.addInput("s_zz", zz)
			.addOutput("large", eigenvalue1)
			.addOutput("middle", eigenvalue2)
			.addOutput("small", eigenvalue3)
			.forEachPixel(
				"double g_xx = s_xx, g_xy = s_xy, g_xz = s_xz, g_yy = s_yy, g_yz = s_yz, g_zz = s_zz;" +
					"double a = -(g_xx + g_yy + g_zz);" +
					"double b = g_xx * g_yy + g_xx * g_zz + g_yy * g_zz - g_xy * g_xy - g_xz * g_xz - g_yz * g_yz;" +
					"double c = g_xx * (g_yz * g_yz - g_yy * g_zz) + g_yy * g_xz * g_xz + g_zz * g_xy * g_xy - 2 * g_xy * g_xz * g_yz;" +
					"double x[3];" +
					"solve_cubic_equation(c, b, a, x);" +
					"large = (float) x[2];" +
					"middle = (float) x[1];" +
					"small = (float) x[0];");
	}
}
