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
	 * @param m is the method to be created
	 * @param obj if it is static is should be null, otherwise the instantiated class
	 * @param args the arguments to invoke the class
	 */
	static public void meassureTime(Method m, Object obj, Object[] args){
		long startTime = System.nanoTime();
		//Method p = new Method();
		
		try {
			m.invoke(obj, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;  //divide by 1000000 to get milliseconds.
        System.out.println("Duration " + duration + " msec");
	}

}
