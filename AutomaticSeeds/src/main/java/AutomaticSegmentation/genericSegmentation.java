/**
 * 
 */
package AutomaticSegmentation;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Collections;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import inra.ijpb.watershed.Watershed;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.kernels.Kernels;

/**
 * @author Pablo Vicente-Munuera, Pedro Gómez-Gálvez
 *
 */
public interface genericSegmentation {

	/**
	 * 
	 * @param initImp
	 * @param gpuOption
	 * @param strelRadius3D
	 * @return
	 */
	default ImagePlus filterPreprocessing(ImagePlus initImp, boolean gpuOption, int strelRadius3D) {
		/***** Enhance Stack contrast with median threshold ******/
		if (gpuOption) {
			// Retrieve filtered stack
			CLIJ clij = CLIJ.getInstance();
			ClearCLBuffer inputClij = clij.push(initImp);
			ClearCLBuffer temp = clij.create(inputClij);
			Kernels.meanBox(clij, inputClij, temp, strelRadius3D, strelRadius3D, strelRadius3D);
			initImp = clij.pull(temp);
		} else {
			Filters3D.filter(initImp.getStack(), Filters3D.MEAN, strelRadius3D, strelRadius3D, strelRadius3D);
		}
		return initImp;
	}
	


	/**
	 * @param initImp
	 * @return
	 */
	public default ImagePlus automaticThreshold(ImagePlus initImp, String autoThresholdMethod) {

		// Thresholder p = new Thresholder();
		// ImagePlus imp_segmented = new ImagePlus("",
		// initImp.getStack().duplicate());
		// imp_segmented.setSlice(Math.round(initImp.getStackSize()/2));
		// imp_segmented.show();
		// p.run("");
		//
		// try {
		// this.wait();
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// initImp.show();

		/*****
		 * Huang automatic threshold, getting the median threshold value for the
		 * full stack
		 *****/
		IJ.log("Applying Huang filter and automatic threshold");
		System.out.println("Applying Huang filter and automatic threshold");
		ArrayList<Integer> thresholds = new ArrayList<Integer>();
		ImageProcessor processor;
		for (int i = 1; i <= initImp.getStackSize(); i++) {
			processor = initImp.getStack().getProcessor(i);
			/**
			 * Methods: "Default", "Huang", "Intermodes", "IsoData",
			 * "IJ_IsoData", "Li","MaxEntropy", "Mean", "MinError", "Minimum",
			 * "Moments", "Otsu","Percentile", "RenyiEntropy", "Shanbhag",
			 * "Triangle" or "Yen"
			 */
			processor.setAutoThreshold(autoThresholdMethod); // "Huang"
			// System.out.println(processor.getAutoThreshold() + " " +
			// processor.getMinThreshold() + " " + processor.getMaxThreshold());
			thresholds.add((int) processor.getMaxThreshold()); // Getting max
																// instead of
																// threshold, we
																// similar
																// results
																// compared with
																// Fiji's
		}

		// Calculate median
		Collections.sort(thresholds);

		int medianThresh = 0;
		if (thresholds.size() % 2 == 1) {
			medianThresh = (int) (thresholds.get(thresholds.size() / 2) + thresholds.get(thresholds.size() / 2 - 1))
					/ 2;
		} else {
			medianThresh = (int) thresholds.get(thresholds.size() / 2);
		}
		System.out.println("thresh: " + medianThresh);

		/************ Apply the calculated threshold **************/
		ImagePlus imp_segmented = new ImagePlus("", initImp.getStack().duplicate());
		for (int i = 1; i <= imp_segmented.getStackSize(); i++) {
			imp_segmented.setSlice(i);
			processor = imp_segmented.getChannelProcessor();
			processor.threshold(medianThresh);
		}

		imp_segmented.show();
		return imp_segmented;
	}
	
	/**
	 * @param initImp
	 * @param imgFilterSize
	 * @return
	 */
	public default ImagePlus createColouredImageWithLabels(ImagePlus initImp, ImageStack imgFilterSize) {
		/*** get colored labels and return image ***/
		// create image with watershed result
		ImagePlus imp_segmentedFinal = new ImagePlus("filtered size", imgFilterSize);
		// assign right calibration
		imp_segmentedFinal.setCalibration(initImp.getCalibration());
		// optimize display range
		Images3D.optimizeDisplayRange(imp_segmentedFinal);
		// Convert the segmented image to 8-Bit
		ImageConverter converterFinal = new ImageConverter(imp_segmentedFinal);
		converterFinal.convertToGray8();
		// Color image
		byte[][] colorMap = CommonLabelMaps.fromLabel(CommonLabelMaps.GOLDEN_ANGLE.getLabel()).computeLut(255, false);
		// Border, color
		ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);

		imp_segmentedFinal.getProcessor().setColorModel(cm);
		imp_segmentedFinal.getImageStack().setColorModel(cm);
		imp_segmentedFinal.updateAndDraw();
		return imp_segmentedFinal;
	}
	
	/**
	 * @param BitD
	 * @param dams
	 * @param toleranceWatershed 
	 * @param connectivity 
	 * @param imgGradient
	 * @return
	 */
	public default ImageStack watershedProcess(int BitD, boolean dams, ImageStack imgFilterSmall, int strelRadius3D, double toleranceWatershed, int connectivity) {

		// Apply morphological gradient to input image
		System.out.println("Gradient");
		IJ.log("Gradient");
		Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(strelRadius3D);
		ImageStack imgGradient = Morphology.gradient(imgFilterSmall, shape3D);
		// progressBar.show(0.25);
		// find regional minima on gradient image with dynamic value of
		// 'tolerance' and 'conn'-connectivity
		System.out.println("Extended minima");
		IJ.log("Extended Minima");
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima(imgGradient, toleranceWatershed, connectivity);
		// progressBar.show(0.4);

		// impose minima on gradient image
		System.out.println("impose minima");
		IJ.log("Impose Minima");
		ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima(imgGradient, regionalMinima, conn);
		// progressBar.show(0.5);
		ImagePlus imgToShow = new ImagePlus("imposeMinima", imposedMinima);
		imgToShow.show();

		// label minima using connected components (32-bit output)
		System.out.println("Labelling");
		IJ.log("Labelling");
		// convert image to 16 bits to enable more labels???
		ImageStack labeledMinima;
		try {
			labeledMinima = BinaryImages.componentsLabeling(regionalMinima, conn, BitD);
		} catch (Exception e) {
			ImagePlus regMinip = new ImagePlus("", regionalMinima);
			ImageConverter converter = new ImageConverter(regMinip);
			converter.convertToGray16();
			labeledMinima = BinaryImages.componentsLabeling(regMinip.getImageStack(), conn, regMinip.getBitDepth());

			// if we change the bitDepth of labeledMinima, also the imposed
			// minima
			ImagePlus impMin = new ImagePlus("", imposedMinima);
			ImageConverter converter2 = new ImageConverter(impMin);
			converter2.convertToGray16();
			imposedMinima = impMin.getImageStack();
		}
		// progressBar.show(0.6);

		// apply marker-based watershed using the labeled minima on the
		// minima-imposed
		System.out.println("Watershed");
		IJ.log("Watershed");
		ImageStack resultStack = Watershed.computeWatershed(imposedMinima, labeledMinima, conn, dams);
		// progressBar.show(0.85);

		return resultStack;
	}

}
