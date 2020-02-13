package AutomaticSegmentation.preProcessing;

import java.util.Arrays;

import javax.swing.JProgressBar;

import AutomaticSegmentation.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.IntrinsicVolumes3D;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Reconstruction;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Strel3D;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.*;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clij.clearcl.util.CLKernelExecutor;

/**
 * 
 * @author Pedro Gómez-Gálvez, Pedro Rodríguez-Hiruela and Pablo Vicente-Munuera
 *
 */
public class quickSegmentation {

	int CONNECTIVITY = 6;
	private ImagePlus inputNucleiImp;
	private ImagePlus outputImp;
	private int strelRadius2D;
	private int strelRadius3D;
	private int toleranceWatershed;
	private int pixelsToOpenVolume;
	private JProgressBar progressBar;
	private Boolean cancelTask;
	private boolean gpuActivated;

	/**
	 * 
	 * @param impNuclei
	 */
	public quickSegmentation(ImagePlus impNuclei, int minVolumePixels, JProgressBar progressBar, Boolean cancelTask,
			Boolean gpuActivated) {

		double minNucleiRadius = Math.cbrt(3 * minVolumePixels / (4 * Math.PI));
		System.out.println(minNucleiRadius);
		System.out.println(minVolumePixels);
		if (Math.round(minNucleiRadius / 5) > 2) {
			this.strelRadius2D = (int) Math.round(minNucleiRadius / 5);
			this.strelRadius3D = (int) Math.round(minNucleiRadius / 4);
		} else {
			this.strelRadius2D = 2;
			this.strelRadius3D = 3;
		}
		System.out.println(strelRadius2D);
		// 10 is a good start point for 8-bit images, 2000 for 16-bits. Minor
		// tolerance more divided objects with watershed
		this.toleranceWatershed = 0;
		this.inputNucleiImp = impNuclei;
		this.pixelsToOpenVolume = (int) Math.round(minVolumePixels);
		this.progressBar = progressBar;
		this.cancelTask = cancelTask;
		this.gpuActivated = gpuActivated;
	}

	/**
	 * @return the ouputImp
	 */
	public ImagePlus getOuputImp() {
		return outputImp;
	}

	/**
	 * 
	 * 
	 * 
	 */
	public ImagePlus segmentationProtocol() {
		outputImp = null;
		// Convert the image to 8-Bit
		if (this.inputNucleiImp.getBitDepth() != 8) {
			ImageConverter converter = new ImageConverter(this.inputNucleiImp);
			converter.convertToGray8();
		}
		// this.inputNucleiImp.duplicate().show();
		int BitD = this.inputNucleiImp.getBitDepth();
		boolean dams = false;
		IJ.log(BitD + "-bits conversion");

		ImagePlus filteredImp = inputNucleiImp.duplicate();
		progressBar.setValue(17);

		/** -------------- Automatic thresholding ---------------- **/
		IJ.log("Automatic thresholding");

		if (cancelTask.booleanValue()) {
			return null;
		}
		filteredImp.setSlice(filteredImp.getNSlices() / 2);
		IJ.run(filteredImp, "Make Binary", "method=Default background=Dark black");// "Make
																					// Binary",
																					// "method=Li
																					// background=black");
		ImagePlus imp_segmented = filteredImp.duplicate();

		progressBar.setValue(20);

		if (cancelTask.booleanValue()) {
			return null;
		}
		/**
		 * ------- loop for closing, binarize and filling holes in 2D ----------
		 **/
		IJ.log("Binarizing, closing and filling holes");
		Strel shape2DOp = Strel.Shape.DISK.fromRadius(this.strelRadius2D);
		Strel shape2DClo = Strel.Shape.DISK.fromRadius(this.strelRadius2D);

		ImageStack imgFilled = imp_segmented.getStack().duplicate();

		// imp_segmented.show();

		for (int i = 1; i <= imgFilled.getSize(); i++) {

			ImageProcessor processor = imp_segmented.getStack().getProcessor(i);
			processor = Morphology.opening(processor, shape2DOp);
			processor = Morphology.closing(processor, shape2DClo);
			processor = Morphology.opening(processor, shape2DOp);
			processor = BinaryImages.binarize(processor);
			processor = Reconstruction.fillHoles(processor);
			imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);

			progressBar.setValue(20 + Math.round((i / filteredImp.getStackSize()) * 10));
		}

//		ImagePlus im2Show = new ImagePlus("", imgFilled);
//		im2Show.show();

		if (cancelTask.booleanValue()) {
			return null;
		}

		progressBar.setValue(31);
		/** -------------- Volume opening ---------------- **/
		IJ.log("Small volume opening: " + pixelsToOpenVolume);
		ImageStack imgFilterSmall = BinaryImages.volumeOpening(imgFilled, pixelsToOpenVolume);

//		ImagePlus im2Show2 = new ImagePlus("", imgFilterSmall);
//		im2Show2.show();

		if (cancelTask.booleanValue()) {
			return null;
		}
		progressBar.setValue(38);

		/** -------------- Watershed ---------------- **/
		ImageStack resultStack = watershedProcess(BitD, dams, imgFilterSmall, strelRadius3D, toleranceWatershed);
		if (cancelTask.booleanValue()) {
			return null;
		}

		/** ------- Get array of volumes ------------ **/
		IJ.log("Getting labelled segmented nuclei");

		LabelImages.removeLargestLabel(resultStack);

		if (cancelTask.booleanValue()) {
			return null;
		}

		progressBar.setValue(83);
		// int[] labels = LabelImages.findAllLabels(resultStack);
		// ImagePlus im2Show4 = new ImagePlus("", resultStack);
		// im2Show4.show();

		if (cancelTask.booleanValue()) {
			return null;
		}

		// int nbLabels = labels.length;
		// // Filter using volumes 3 times smaller than the median
		// double[] volumes = IntrinsicVolumes3D.volumes(resultStack, labels,
		// new ImagePlus("", resultStack).getCalibration());
		// Arrays.sort(volumes);
		// double thresholdVolume = (volumes[nbLabels / 2] / 3); // ??? We have
		// a minimal radius
		double thresholdVolume = this.pixelsToOpenVolume;
		progressBar.setValue(90);

		/** ------------ Second volume opening ---------- **/
		if (cancelTask.booleanValue()) {
			return null;
		}
		IJ.log("Removing outliers");
		ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));

		if (cancelTask.booleanValue()) {
			return null;
		}

		/**
		 * -------------- Create coloured image from labels ----------------
		 **/
		ImagePlus imp_segmentedFinal = nuclei3DSegmentation.createColouredImageWithLabels(imgFilterSize,
				inputNucleiImp.getCalibration());

		outputImp = imp_segmentedFinal;
		progressBar.setValue(100);

		// imp_segmentedFinal.show();
		return outputImp;
	}

	public ImagePlus automaticThreshold(ImagePlus initImp) {

		/************ Store automatic thresholds **************/

		ImageProcessor processor;
		int medianThresh = 0;
		// ArrayList<Integer> thresholds = new ArrayList<Integer>();

		processor = initImp.getStack().getProcessor(Math.round(initImp.getStackSize() / 2));
		medianThresh = processor.getAutoThreshold();
		// int initSliceThresh = 4;
		// int finalSliceThresh = initImp.getStackSize()-4;
		// if (initImp.getStackSize()<10) {
		// initSliceThresh = 1;
		// finalSliceThresh = initImp.getStackSize();
		// }
		// for (int i = initSliceThresh; i <= finalSliceThresh; i++) {
		// processor = initImp.getStack().getProcessor(i);
		// IJ.log("Threshold: "+processor.getAutoThreshold());
		// thresholds.add((int) processor.getAutoThreshold());
		// }
		//
		// /************ Calculate median threshold **************/
		// Collections.sort(thresholds);
		//
		// if (thresholds.size() % 2 == 1) {
		// medianThresh = (int) (thresholds.get(thresholds.size() / 2) +
		// thresholds.get(thresholds.size() / 2 - 1))
		// / 2;
		// } else {
		// medianThresh = (int) thresholds.get(thresholds.size() / 2);
		// }
		/************ Apply the calculated threshold **************/
		ImagePlus imp_segmented = new ImagePlus("", initImp.getStack().duplicate());
		for (int i = 1; i <= imp_segmented.getStackSize(); i++) {
			imp_segmented.setSlice(i);
			processor = imp_segmented.getChannelProcessor();
			processor.threshold(medianThresh);
		}
		return imp_segmented;

	}

	public ImageStack watershedProcess(int BitD, boolean dams, ImageStack imgFilterSmall, int strelRadius3D,
			double toleranceWatershed) {
		ImageStack resultStack = null;

//		if (gpuActivated) {
//			IJ.log("Init Watershed protocol");
//			CLIJ2 clij2 = CLIJ2.getInstance();
//			CLIJx clijx = CLIJx.getInstance();
//			CLIJ clij = CLIJ.getInstance();
//
//			//Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(strelRadius3D);
//			//ImageStack imgGradient = Morphology.gradient(imgFilterSmall, shape3D);
//			// get input parameters
//			//ImagePlus impFilterSmall = new ImagePlus("", imgFilterSmall);
//			ImagePlus initImage = new ImagePlus("", imgFilterSmall);
//			
//			ClearCLBuffer binary_input = clij2.push(initImage);
//			ClearCLBuffer watershededImage = clij.create(binary_input);
//
//			ClearCLBuffer thresholded = clij.create(binary_input);
//			//clijx.threshold(binary_input, thresholded, 1);
//
//			net.haesleinhuepf.clijx.plugins.Watershed.watershed(clijx, thresholded, watershededImage);
//
//			ImagePlus watershededImagePlus = clijx.pull(watershededImage);
////			watershededImagePlus.show();
//
//			ImagePlus impMin = new ImagePlus("", watershededImagePlus.getImageStack());
//			ImageConverter converter2 = new ImageConverter(impMin);
//			converter2.convertToGray16();
//			impMin.show();
//			
//			ImageStack labelledStack = BinaryImages.componentsLabeling(impMin.getImageStack(), CONNECTIVITY,
//					impMin.getBitDepth());
//				
//			//resultStack = watershededImagePlus.getImageStack();
//			resultStack = labelledStack;
//			
////			ImagePlus resultStackToShow = new ImagePlus("", resultStack);
////			resultStackToShow.show();
//			
//			// cleanup memory on GPU
//			clij2.release(binary_input);
//			clij2.release(watershededImage);
//		} else {
			/*************
			 * Apply morphological gradient to input image
			 *************/
			if (cancelTask.booleanValue()) {
				return null;
			}
			IJ.log("-Gradient");
			Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(strelRadius3D);
			
			ImageStack imgGradient;
			if (gpuActivated)
				imgGradient = Utils.gradientCLIJ(imgFilterSmall, strelRadius3D);
			else
				imgGradient = Morphology.gradient(imgFilterSmall, shape3D);
			
			progressBar.setValue(50);

			/*************
			 * Find regional minima on gradient image with dynamic value of
			 * 'tolerance' and 'conn'-connectivity
			 *************/
			if (cancelTask.booleanValue()) {
				return null;
			}
			
//			ImagePlus imgGradientImagePlus = new ImagePlus("", imgGradient);
//			imgGradientImagePlus.show();
			
			IJ.log("-Extended Minima");
			ImageStack regionalMinima;
			if (toleranceWatershed == 0) { //When tolerance is 0 it is the same as inverting the image
				ImagePlus regionalMinimaImagePlus = new ImagePlus("", imgGradient);
				
				for (int numZ = 1; numZ <= regionalMinimaImagePlus.getImageStack().getSize(); numZ++) {
					regionalMinimaImagePlus.setSlice(numZ);
					regionalMinimaImagePlus.getProcessor().invert();
				}
				
				regionalMinima = regionalMinimaImagePlus.getImageStack();
			} else {
				regionalMinima = MinimaAndMaxima3D.extendedMinima(imgGradient, toleranceWatershed, CONNECTIVITY);
			}
			
			progressBar.setValue(60);
			
			ImagePlus regionalMinimaImagePlus = new ImagePlus("", regionalMinima);
			regionalMinimaImagePlus.show();
			

			/************************
			 * impose minima on gradient image
			 **********************/
			if (cancelTask.booleanValue()) {
				return null;
			}

			IJ.log("-Impose Minima");
			ImageStack imposedMinima;
			if (gpuActivated) {
				CLIJ2 clij2 = CLIJ2.getInstance();
				CLIJ clij = CLIJ.getInstance();
	
				ImagePlus initImage = new ImagePlus("", imgGradient);
				ClearCLBuffer binary_input = clij2.push(initImage);
				ClearCLBuffer regionalMinimaClij = clij.create(binary_input);

				//Do something
				
				ImagePlus imposeMinimaImagePlus = clij2.pull(regionalMinimaClij);
				imposedMinima = imposeMinimaImagePlus.getImageStack();
			} else {
				imposedMinima = MinimaAndMaxima3D.imposeMinima(imgGradient, regionalMinima, CONNECTIVITY);
			}
			progressBar.setValue(68);

			/************************
			 * label minima using connected components (32-bit output)
			 **********************/
			if (cancelTask.booleanValue()) {
				return null;
			}

			IJ.log("-Labelling");

			ImageStack labelledMinima;
			try {
				labelledMinima = BinaryImages.componentsLabeling(regionalMinima, CONNECTIVITY, BitD);
			} catch (Exception e) {
				IJ.log("-Corverting to 16-bits");
				ImagePlus regMinip = new ImagePlus("", regionalMinima);
				ImageConverter converter = new ImageConverter(regMinip);
				converter.convertToGray16();
				labelledMinima = BinaryImages.componentsLabeling(regMinip.getImageStack(), CONNECTIVITY,
						regMinip.getBitDepth());
				ImagePlus impMin = new ImagePlus("", imposedMinima);
				ImageConverter converter2 = new ImageConverter(impMin);
				converter2.convertToGray16();
				imposedMinima = impMin.getImageStack();
			}
			progressBar.setValue(72);

			/*****
			 * Apply marker-based watershed using the labeled minima on the
			 * minima-imposed
			 *****/
			if (cancelTask.booleanValue()) {
				return null;
			}

			IJ.log("	Watershed");
			resultStack = inra.ijpb.watershed.Watershed.computeWatershed(imposedMinima, labelledMinima, CONNECTIVITY,
					dams);
			progressBar.setValue(80);
//		}

		return resultStack;
	}

	public void setCancelTask(Boolean cancelTask) {
		this.cancelTask = cancelTask.booleanValue();
	}

}
