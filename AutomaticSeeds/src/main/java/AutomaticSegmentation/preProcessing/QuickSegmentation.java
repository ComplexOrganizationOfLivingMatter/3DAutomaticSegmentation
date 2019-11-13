package AutomaticSegmentation.preProcessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
public class QuickSegmentation{
	
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
	public QuickSegmentation(ImagePlus impNuclei,JProgressBar progressBar,Boolean cancelTask) {
		this.strelRadius2D = 4;
		this.strelRadius3D = 3;
		// 10 is a good start point for 8-bit images, 2000 for 16-bits. Minor
		// tolerance more divided objects with watershed
		this.toleranceWatershed = 0;
		this.inputNucleiImp = impNuclei;
		this.pixelsToOpenVolume = 50;
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
	public QuickSegmentation(ImagePlus impNuclei,int radius2D, int radius3D, int tolerance, int pixelsToOpenVolume,JProgressBar progressBar,Boolean cancelTask) {
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
			progressBar.setValue(21);
			
			IJ.log("Automatic thresholding");
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			
			ImagePlus imp_segmented = automaticThreshold(filteredImp);
			progressBar.setValue(30);
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			/***** loop for closing, binarize and filling holes in 2D *****/
			IJ.log("Binarizing, closing and filling holes");
			Strel shape2D = Strel.Shape.DISK.fromRadius(this.strelRadius2D);
			ImageStack imgFilled = imp_segmented.getStack().duplicate();
			
//			imp_segmented.show();
			
			for (int i = 1; i <= imgFilled.getSize(); i++) {
				
				ImageProcessor processor = imp_segmented.getStack().getProcessor(i);
				processor = Morphology.closing(processor, shape2D);
				processor = BinaryImages.binarize(processor);
				processor = Reconstruction.fillHoles(processor);
				imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);
				
				progressBar.setValue(30 + Math.round((i/filteredImp.getStackSize())*15));
				
				
			}
			
			ImagePlus im2Show = new ImagePlus("", imgFilled);
			im2Show.show();
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			
			progressBar.setValue(46);
			// Volume opening
			IJ.log("Small volume opening");
			ImageStack imgFilterSmall = BinaryImages.volumeOpening(imgFilled, pixelsToOpenVolume);
			
//			ImagePlus im2Show2 = new ImagePlus("", imgFilterSmall);
//			im2Show2.show();
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			progressBar.setValue(55);
			//Watershed process
			IJ.log("Init Watershed protocol");
			ImageStack resultStack = watershedProcess(BitD, dams, imgFilterSmall, strelRadius3D, toleranceWatershed);
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			
//			ImagePlus im2Show3 = new ImagePlus("", resultStack);
//			im2Show3.show();
			
			progressBar.setValue(80);
			/****** get array of volumes ******/
			IJ.log("Getting labelled segmented nuclei");
	
			LabelImages.removeLargestLabel(resultStack);
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			
			progressBar.setValue(83);
			int[] labels = LabelImages.findAllLabels(resultStack);
//			ImagePlus im2Show4 = new ImagePlus("", resultStack);
//			im2Show4.show();
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
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
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			IJ.log("Removing outliers");
			ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));
			
//			ImagePlus im2Show5 = new ImagePlus("", imgFilterSize);
//			im2Show5.show();
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
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
		ArrayList<Integer> thresholds = new ArrayList<Integer>();
		ImageProcessor processor;
		for (int i = 1; i <= initImp.getStackSize(); i++) {
			processor = initImp.getStack().getProcessor(i);
			
			if (processor.getAutoThreshold()>10) {
				thresholds.add((int) processor.getAutoThreshold()); 
			}	
		}
		if (thresholds.isEmpty()) {
			IJ.error("extremely dark images");
			return null;
		}else {
		
			/************ Calculate median threshold **************/
			Collections.sort(thresholds);
			int medianThresh = 0;
			if (thresholds.size() % 2 == 1) {
				medianThresh = (int) (thresholds.get(thresholds.size() / 2) + thresholds.get(thresholds.size() / 2 - 1))
						/ 2;
			} else {
				medianThresh = (int) thresholds.get(thresholds.size() / 2);
			}
			/************ Apply the calculated threshold **************/
			ImagePlus imp_segmented = new ImagePlus("", initImp.getStack().duplicate());
			for (int i = 1; i <= imp_segmented.getStackSize(); i++) {
				imp_segmented.setSlice(i);
				processor = imp_segmented.getChannelProcessor();
				processor.threshold(medianThresh);
			}
			return imp_segmented;
		}

		
	}
	
	public ImageStack watershedProcess(int BitD, boolean dams, ImageStack imgFilterSmall, int strelRadius3D, double toleranceWatershed) {

		/*************Apply morphological gradient to input image*************/
		IJ.log("	Gradient");
		Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(strelRadius3D);
		ImageStack imgGradient = Morphology.gradient(imgFilterSmall, shape3D);

		/*************Find regional minima on gradient image with dynamic value of 'tolerance' and 'conn'-connectivity*************/
		IJ.log("	Extended Minima");
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima(imgGradient, toleranceWatershed, CONNECTIVITY);

		/************************impose minima on gradient image**********************/
		IJ.log("	Impose Minima");
		ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima(imgGradient, regionalMinima, CONNECTIVITY);

		/************************label minima using connected components (32-bit output)**********************/
		IJ.log("	Labelling");
		ImageStack labeledMinima;
		try {
			labeledMinima = BinaryImages.componentsLabeling(regionalMinima, CONNECTIVITY, BitD);
		} catch (Exception e) {
			IJ.log("	Corverting to 16-bits");
			ImagePlus regMinip = new ImagePlus("", regionalMinima);
			ImageConverter converter = new ImageConverter(regMinip);
			converter.convertToGray16();
			labeledMinima = BinaryImages.componentsLabeling(regMinip.getImageStack(), CONNECTIVITY, regMinip.getBitDepth());
			ImagePlus impMin = new ImagePlus("", imposedMinima);
			ImageConverter converter2 = new ImageConverter(impMin);
			converter2.convertToGray16();
			imposedMinima = impMin.getImageStack();
		}
		
		/*****Apply marker-based watershed using the labeled minima on the minima-imposed*****/
		IJ.log("	Watershed");
		ImageStack resultStack = Watershed.computeWatershed(imposedMinima, labeledMinima, CONNECTIVITY, dams);

		return resultStack;
	}
	
}
