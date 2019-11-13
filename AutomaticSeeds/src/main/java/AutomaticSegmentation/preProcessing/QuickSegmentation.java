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
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Reconstruction;
import inra.ijpb.morphology.Strel;

/**
 * 
 * @author Pedro Gómez-Gálvez, Pedro Rodríguez-Hiruela and Pablo Vicente-Munuera
 *
 */
public class QuickSegmentation implements genericSegmentation {
	
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
			
			imp_segmented.show();
			
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
			
			ImagePlus im2Show2 = new ImagePlus("", imgFilterSmall);
			im2Show2.show();
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			progressBar.setValue(55);
			//Watershed process
			IJ.log("Watershed");
			ImageStack resultStack = watershedProcess(BitD, dams, imgFilterSmall, strelRadius3D, toleranceWatershed);
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			
			ImagePlus im2Show3 = new ImagePlus("", resultStack);
			im2Show3.show();
			
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
			ImagePlus im2Show4 = new ImagePlus("", resultStack);
			im2Show4.show();
			
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
			
			ImagePlus im2Show5 = new ImagePlus("", imgFilterSize);
			im2Show5.show();
			
			if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
			
			ImagePlus imp_segmentedFinal = createColouredImageWithLabels(this.inputNucleiImp, imgFilterSize);
			
			outputImp = imp_segmentedFinal;
			progressBar.setValue(100);
			
			imp_segmentedFinal.show();
			
			
		}
		return outputImp;
	}
}
