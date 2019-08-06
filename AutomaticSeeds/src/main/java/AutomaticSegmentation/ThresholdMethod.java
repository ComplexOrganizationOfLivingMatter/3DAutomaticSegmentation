package AutomaticSegmentation;
/**
 * 
 */

/**
 * @author PhD-Student
 *
 */
public enum ThresholdMethod {
	DEFAULT("Default"), HUANG("Huang"), INTERMODES("Intermodes"), ISODATA("IsoData"), IJ_ISODATA("IJ_IsoData"), LI(
			"Li"), MAXENTROPY("MaxEntropy"), MEAN("Mean"), MINERROR("MinError"), MINIMUM("Minimum"), MOMENTS(
					"Moments"), OTSU("Otsu"), PERCENTILE("Percentile"), RENYIENTROPY(
							"RenyiEntropy"), SHANBHAG("Shanghag"), TRIANGLE("Triangle"), YEN("Yen");

	private String nameToDisplay;

	ThresholdMethod(String nameToDisplay) {
		this.nameToDisplay = nameToDisplay;
	}

	public String getNameToDisplay() {
		return nameToDisplay;
	}
	
	public String toString(){
		return nameToDisplay;
	}
}
