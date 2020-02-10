/**
 * 
 */
package AutomaticSegmentation.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

}
