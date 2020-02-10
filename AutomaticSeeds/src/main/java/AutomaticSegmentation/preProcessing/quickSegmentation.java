package AutomaticSegmentation.preProcessing;

import java.util.Arrays;

import javax.swing.JProgressBar;

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
import inra.ijpb.watershed.Watershed;

/**
 * 
 * @author Pedro Gómez-Gálvez, Pedro Rodríguez-Hiruela and Pablo Vicente-Munuera
 *
 */
public class quickSegmentation{
	
	int CONNECTIVITY = 6;
	private ImagePlus inputNucleiImp;
	private ImagePlus outputImp;
	private int strelRadius2D;
	private int strelRadius3D;
	private int toleranceWatershed;
	private int pixelsToOpenVolume;
	private JProgressBar progressBar;
	private Boolean cancelTask;
	/**
	 * 
	 * @param impNuclei
	 */
	public quickSegmentation(ImagePlus impNuclei, int minNucleiRadius,JProgressBar progressBar,Boolean cancelTask) {
		if (Math.round(minNucleiRadius/5)>2) {
			this.strelRadius2D = (int) Math.round(minNucleiRadius/5);
			this.strelRadius3D = (int) Math.round(minNucleiRadius/4);
		}else {
			this.strelRadius2D = 2;
			this.strelRadius3D = 3;
		}
		// 10 is a good start point for 8-bit images, 2000 for 16-bits. Minor
		// tolerance more divided objects with watershed
		this.toleranceWatershed = 0;
		this.inputNucleiImp = impNuclei;
		this.pixelsToOpenVolume = (int) Math.round(minNucleiRadius*minNucleiRadius*Math.PI/3);
		this.progressBar = progressBar;
		this.cancelTask = cancelTask;
	}

	/**
	 * 
	 * @param impNuclei
	 * @param radius2D
	 * @param radius3D
	 * @param tolerance
	 * @param pixelsToOpenVolume
	 */
	public quickSegmentation(ImagePlus impNuclei,int radius2D, int radius3D, int tolerance, int pixelsToOpenVolume,JProgressBar progressBar,Boolean cancelTask) {
		this.strelRadius2D = radius2D;
		this.strelRadius3D = radius3D;
		this.toleranceWatershed = tolerance;
		this.inputNucleiImp = impNuclei;
		this.pixelsToOpenVolume = pixelsToOpenVolume;
		this.progressBar = progressBar;
		this.cancelTask = cancelTask;
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
		outputImp =null;
		
		while(!cancelTask.booleanValue() && progressBar.getValue()!=100)  {
			// Convert the image to 8-Bit
 			if (this.inputNucleiImp.getBitDepth() != 8) {
 				ImageConverter converter = new ImageConverter(this.inputNucleiImp);
 				converter.convertToGray8();
 			}
			//this.inputNucleiImp.duplicate().show();
			int BitD = this.inputNucleiImp.getBitDepth();
			boolean dams = false;
			IJ.log(BitD + "-bits conversion");
		
			ImagePlus filteredImp = inputNucleiImp.duplicate();
			progressBar.setValue(17);
			
			IJ.log("Automatic thresholding");
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			filteredImp.setSlice(filteredImp.getNSlices()/2);
			IJ.run(filteredImp,"Make Binary", "method=Default background=Dark black");//"Make Binary", "method=Li background=black");
			ImagePlus imp_segmented = filteredImp.duplicate();
			
			//ImagePlus imp_segmented = automaticThreshold(filteredImp);
			//if (filteredImp==null) {
			//	break;
			//}
			//imp_segmented.duplicate().show();
			
			progressBar.setValue(20);
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			/***** loop for closing, binarize and filling holes in 2D *****/
			IJ.log("Binarizing, closing and filling holes");
			Strel shape2DOp = Strel.Shape.DISK.fromRadius(this.strelRadius2D);
			Strel shape2DClo = Strel.Shape.DISK.fromRadius(this.strelRadius2D);

			ImageStack imgFilled = imp_segmented.getStack().duplicate();
			
//			imp_segmented.show();
			
			for (int i = 1; i <= imgFilled.getSize(); i++) {
				
				ImageProcessor processor = imp_segmented.getStack().getProcessor(i);
				processor = Morphology.opening(processor, shape2DOp);
				processor = Morphology.closing(processor, shape2DClo);
				processor = Morphology.opening(processor, shape2DOp);
				processor = BinaryImages.binarize(processor);
				processor = Reconstruction.fillHoles(processor);
				imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);
				
				progressBar.setValue(20 + Math.round((i/filteredImp.getStackSize())*10));
				
				
			}
			
			ImagePlus im2Show = new ImagePlus("", imgFilled);
//			im2Show.show();
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			progressBar.setValue(31);
			// Volume opening
			IJ.log("Small volume opening");
			ImageStack imgFilterSmall = BinaryImages.volumeOpening(imgFilled, pixelsToOpenVolume);
			
//			ImagePlus im2Show2 = new ImagePlus("", imgFilterSmall);
//			im2Show2.show();
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			progressBar.setValue(38);
			//Watershed process
			IJ.log("Init Watershed protocol");
			ImageStack resultStack = watershedProcess(BitD, dams, imgFilterSmall, strelRadius3D, toleranceWatershed);
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
//			ImagePlus im2Show3 = new ImagePlus("", resultStack);
//			im2Show3.show();
			
			/****** get array of volumes ******/
			IJ.log("Getting labelled segmented nuclei");
	
			LabelImages.removeLargestLabel(resultStack);
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			progressBar.setValue(83);
			int[] labels = LabelImages.findAllLabels(resultStack);
//			ImagePlus im2Show4 = new ImagePlus("", resultStack);
//			im2Show4.show();
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			int nbLabels = labels.length;
			// Filter using volumes 3 times smaller than the median
			double[] volumes = IntrinsicVolumes3D.volumes(resultStack, labels,
					new ImagePlus("", resultStack).getCalibration());
			Arrays.sort(volumes);
			double thresholdVolume = (volumes[nbLabels / 2] / 3);
			progressBar.setValue(90);
	
			/******** volume opening ********/
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			IJ.log("Removing outliers");
			ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));
			
//			ImagePlus im2Show5 = new ImagePlus("", imgFilterSize);
//			im2Show5.show();
			
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			ImagePlus imp_segmentedFinal = nuclei3DSegmentation.createColouredImageWithLabels(inputNucleiImp, imgFilterSize);
			
			outputImp = imp_segmentedFinal;
			progressBar.setValue(100);
			
//			imp_segmentedFinal.show();
			
			
		}
		return outputImp;
	}
	
	public ImagePlus automaticThreshold(ImagePlus initImp) {

		/************ Store automatic thresholds **************/
		
		ImageProcessor processor;
		int medianThresh = 0;
		//ArrayList<Integer> thresholds = new ArrayList<Integer>();

		processor = initImp.getStack().getProcessor(Math.round(initImp.getStackSize()/2));
		medianThresh = processor.getAutoThreshold(); 
//		int initSliceThresh = 4;
//		int finalSliceThresh = initImp.getStackSize()-4;
//		if (initImp.getStackSize()<10) {
//			initSliceThresh = 1;
//			finalSliceThresh = initImp.getStackSize();
//		}
//		for (int i = initSliceThresh; i <= finalSliceThresh; i++) {
//			processor = initImp.getStack().getProcessor(i);
//			IJ.log("Threshold: "+processor.getAutoThreshold());
//			thresholds.add((int) processor.getAutoThreshold()); 
//		}
//		
//		/************ Calculate median threshold **************/
//		Collections.sort(thresholds);
//		
//		if (thresholds.size() % 2 == 1) {
//			medianThresh = (int) (thresholds.get(thresholds.size() / 2) + thresholds.get(thresholds.size() / 2 - 1))
//					/ 2;
//		} else {
//			medianThresh = (int) thresholds.get(thresholds.size() / 2);
//		}
		/************ Apply the calculated threshold **************/
		ImagePlus imp_segmented = new ImagePlus("", initImp.getStack().duplicate());
		for (int i = 1; i <= imp_segmented.getStackSize(); i++) {
			imp_segmented.setSlice(i);
			processor = imp_segmented.getChannelProcessor();
			processor.threshold(medianThresh);
		}
		return imp_segmented;
		

		
	}
	
	public ImageStack watershedProcess(int BitD, boolean dams, ImageStack imgFilterSmall, int strelRadius3D, double toleranceWatershed) {
		ImageStack resultStack = null;
		while(!cancelTask.booleanValue() && progressBar.getValue()!=80)  {
			/*************Apply morphological gradient to input image*************/
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			IJ.log("-Gradient");
			Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(strelRadius3D);
			ImageStack imgGradient = Morphology.gradient(imgFilterSmall, shape3D);
			progressBar.setValue(50);

			/*************Find regional minima on gradient image with dynamic value of 'tolerance' and 'conn'-connectivity*************/
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			IJ.log("-Extended Minima");
			ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima(imgGradient, toleranceWatershed, CONNECTIVITY);
			progressBar.setValue(60);

			/************************impose minima on gradient image**********************/
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			IJ.log("-Impose Minima");
			ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima(imgGradient, regionalMinima, CONNECTIVITY);
			progressBar.setValue(68);

			/************************label minima using connected components (32-bit output)**********************/
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			IJ.log("-Labelling");
			ImageStack labeledMinima;
			try {
				labeledMinima = BinaryImages.componentsLabeling(regionalMinima, CONNECTIVITY, BitD);
			} catch (Exception e) {
				IJ.log("-Corverting to 16-bits");
				ImagePlus regMinip = new ImagePlus("", regionalMinima);
				ImageConverter converter = new ImageConverter(regMinip);
				converter.convertToGray16();
				labeledMinima = BinaryImages.componentsLabeling(regMinip.getImageStack(), CONNECTIVITY, regMinip.getBitDepth());
				ImagePlus impMin = new ImagePlus("", imposedMinima);
				ImageConverter converter2 = new ImageConverter(impMin);
				converter2.convertToGray16();
				imposedMinima = impMin.getImageStack();
			}
			progressBar.setValue(72);

			/*****Apply marker-based watershed using the labeled minima on the minima-imposed*****/
			if (cancelTask.booleanValue()) {
	        	break;
	        }
			
			IJ.log("	Watershed");
			resultStack = Watershed.computeWatershed(imposedMinima, labeledMinima, CONNECTIVITY, dams);
			progressBar.setValue(80);

		}
		return resultStack;
	}
	
	public void setCancelTask(Boolean cancelTask) {
		this.cancelTask = cancelTask.booleanValue();
	}
	
}
