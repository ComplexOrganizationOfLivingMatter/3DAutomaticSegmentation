/**
 * 
 */
package AutomaticSegmentation.utils;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

import AutomaticSegmentation.preProcessing.nuclei3DSegmentation;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import inra.ijpb.morphology.Strel3D;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;

/**
 * @author PhD-Student
 *
 */
final public class Utils {

	/**
	 * 
	 * Example:
	 * 
	 * <pre>
	 * Object[] args = { imp_segmented.getImageStack(), labels };
	 * try {
	 * 	Utils.measureTime(Centroid3D.class.getMethod("centroids", imp_segmented.getImageStack().getClass(), labels.getClass()), null, args);
	 * } catch (SecurityException | NoSuchMethodException e) {
	 * 	// TODO Auto-generated catch block
	 * 	e.printStackTrace();
	 * }
	 * </pre>
	 * 
	 * @param m
	 *            is the method to be created
	 * @param obj
	 *            if it is static is should be null, otherwise the instantiated
	 *            class
	 * @param args
	 *            the arguments to invoke the class
	 */
	static public void measureTime(Method m, Object obj, Object[] args) {
		long startTime = System.nanoTime();
		// Method p = new Method();

		try {
			m.invoke(obj, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000; // divide by 1000000 to
															// get milliseconds.
		System.out.println("Duration " + duration + " msec");
	}
	
	/**
	 * Taken from
	 * https://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-median-and-standard-deviation-in-c-or-java
	 * Mr. White's answer
	 * 
	 * @param data
	 *            to get the mean
	 * @return the mean of the data
	 */
	public static double getMean(double[] data) {
		double sum = 0.0;
		for (double a : data)
			sum += a;
		return sum / data.length;
	}

	public static double getMin(double[] data) {
		double min = Double.POSITIVE_INFINITY;
		for (double a : data) {
			if (min > a)
				min = a;
		}
		return min;
	}
	
	/**^
	 * 
	 * @param image
	 * @param strel
	 * @return
	 */
	public static ImageStack gradientCLIJ(ImageStack image, int radiusStrel) {
		// First performs dilation and erosion
		CLIJ2 clij2 = CLIJ2.getInstance();
		CLIJ clij = CLIJ.getInstance();

		ImagePlus initImage = new ImagePlus("", image);
		
		ClearCLBuffer binary_input = clij2.push(initImage);
		ClearCLBuffer dilatedImage = clij.create(binary_input);
		ClearCLBuffer erodedImage = clij.create(binary_input);
		
		clij2.maximum3DSphere(binary_input, dilatedImage, radiusStrel, radiusStrel, radiusStrel); //Dilation
		clij2.minimum3DSphere(binary_input, erodedImage, radiusStrel, radiusStrel, radiusStrel); //erosion
		
		ImagePlus dilatedImagePlus = clij2.pull(dilatedImage);
		dilatedImagePlus.show();
		
		ImagePlus erodedImagePlus = clij2.pull(erodedImage);
		erodedImagePlus.show();
		
		ImageStack erodedStack = erodedImagePlus.getImageStack();
		ImageStack resultStack = dilatedImagePlus.getImageStack();
		
		clij2.release(binary_input);
		clij2.release(erodedImage);
		clij2.release(dilatedImage);
		
		// Determine max possible value from bit depth
		double maxVal = getMaxPossibleValue(image);
	
		// Compute subtraction of result from original image
		int nx = image.getWidth();
		int ny = image.getHeight();
		int nz = image.getSize();
		for (int z = 0; z < nz; z++) 
		{
			for (int y = 0; y < ny; y++) 
			{
				for (int x = 0; x < nx; x++) 
				{
					double v1 = resultStack.getVoxel(x, y, z);
					double v2 = erodedStack.getVoxel(x, y, z);
					resultStack.setVoxel(x, y, z, min(max(v1 - v2, 0), maxVal));
				}
			}
		}
		
		return resultStack;
	}
	
	/**
	 * From inra.ijpb.morphology
	 * @author David Legland
	 * Determine max possible value from bit depth.
	 *  8 bits -> 255
	 * 16 bits -> 65535
	 * 32 bits -> Float.MAX_VALUE
	 */
	private static final double getMaxPossibleValue(ImageStack stack)
	{
		double maxVal = 255;
		int bitDepth = stack.getBitDepth(); 
		if (bitDepth == 16)
		{
			maxVal = 65535;
		}
		else if (bitDepth == 32)
		{
			maxVal = Float.MAX_VALUE;
		}
		return maxVal;
	}
}
