package AutomaticSegmentation;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.plugin.Resizer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.IntrinsicVolumes3D;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Reconstruction;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import inra.ijpb.watershed.Watershed;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.kernels.Kernels;


public class SegmNucleiGlands{
	
	public ImagePlus inputImp;
	public ImagePlus outputImp;
	private int	strelRadius2D;
	private int	strelRadius3D;
	private int toleranceWatershed;
	
	public SegmNucleiGlands(ImagePlus imp) {
		inputImp = imp;
		strelRadius2D = 4;
		strelRadius3D = 3;
		// 10 is a good start point for 8-bit images, 2000 for 16-bits. Minor tolerance more divided objects with watershed
		toleranceWatershed = 0;
		outputImp = segmentationProtocol();
	}
	
	public SegmNucleiGlands(ImagePlus imp,int radius2D,int radius3D, int tolerance) {
		inputImp = imp;
		strelRadius2D = radius2D;
		strelRadius3D = radius3D;
		toleranceWatershed = tolerance;
		outputImp = segmentationProtocol();
	}

	/**
	 * @return the inputImp
	 */
	public ImagePlus getInputImp() {
		return inputImp;
	}

	
	/**
	 * @return the ouputImp
	 */
	public ImagePlus getOuputImp() {
		return outputImp;
	}
	
	/**
	 * @return the segmentedImage
	 */
	private ImagePlus segmentationProtocol(){
		
		
		//Convert the image to 8-Bit
		ImagePlus auxImp = inputImp;
		ImagePlus initImp = (ImagePlus) auxImp.clone();
		if(initImp.getBitDepth() != 8) {
			ImageConverter converter = new ImageConverter(initImp);
			converter.convertToGray8();
		}
		//Test
		int conn = 6;
		int BitD = initImp.getBitDepth();
		boolean dams = false;
		//double resizeFactor = 1;
		
		IJ.log(BitD+"-bits convertion");
		
		/*****Enhance Stack contrast with median threshold******/
		
		// Retrieve filtered stack 
		CLIJ clij = CLIJ.getInstance();
        ClearCLBuffer inputClij = clij.push(initImp);
        ClearCLBuffer temp = clij.create(inputClij);
        Kernels.meanBox(clij, inputClij, temp, strelRadius3D, strelRadius3D, strelRadius3D);
        initImp = clij.pull(temp);
		
		
        /*****Huang automatic threshold, getting the median threshold value for the full stack*****/
        
		IJ.log("Applying Huang filter and automatic threshold");
		int[] thresh = new int[initImp.getStackSize()+1];
		ArrayList<Integer> thresholds = new ArrayList<Integer>();
		for(int i=1;i<=initImp.getStackSize();i++) {
			ImageProcessor processor = initImp.getStack().getProcessor(i);
			processor.setAutoThreshold("Huang");
			thresh[i-1] = processor.getAutoThreshold();
			if (thresh[i-1]>5){
				thresholds.add((Integer) thresh[i-1]);
			};
		}
		Collections.sort(thresholds);
		int medianThresh = 0;
		if (thresholds.size()%2 == 1) {
			medianThresh = (int) (thresholds.get(thresholds.size()/2)+ thresholds.get(thresholds.size()/2 -1))/2;
	        } else {
	        	medianThresh = (int) thresholds.get(thresholds.size()/2);
	        }
		int intMedianThresh = (int) Math.round(medianThresh/2);
		System.out.println("thresh: "+intMedianThresh);
		
		/************Apply the calculated threshold**************/
		ImagePlus imp_segmented = initImp.duplicate();
		for(int i=1;i<=initImp.getStackSize();i++) {
			ImageProcessor processor = initImp.getStack().getProcessor(i);
			processor.threshold(intMedianThresh);
			imp_segmented.getStack().setProcessor((ImageProcessor) processor.clone(), i);
		}
			
		
		
		/*****loop for closing, binarize and filling holes in 2D*****/
		System.out.println("Closing, binarize and filling");
		IJ.log("Closing, binarize and filling");
		Strel shape2D = Strel.Shape.DISK.fromRadius(this.strelRadius2D);
		ImageStack imgFilled = imp_segmented.getStack().duplicate();
		for(int i=1;i<=imp_segmented.getStackSize();i++) {
			ImageProcessor processor = imp_segmented.getStack().getProcessor(i);
			processor = Morphology.closing(processor, shape2D);
			processor = BinaryImages.binarize(processor);
			processor = Reconstruction.fillHoles(processor);
			imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);
		}
		//progressBar.show(0.1);	
		
		//Volume opening
		System.out.println("Small volume opening");
		IJ.log("Small volume opening");
		ImageStack imgFilterSmall = BinaryImages.volumeOpening(imgFilled, 50);
		
		//Apply morphological gradient to input image
		System.out.println("Gradient");
		IJ.log("Gradient");
		Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(this.strelRadius3D);
		ImageStack imgGradient = Morphology.gradient(imgFilterSmall, shape3D);
		//progressBar.show(0.25);
		
		// find regional minima on gradient image with dynamic value of 'tolerance' and 'conn'-connectivity
		System.out.println("Extended minima");
		IJ.log("Extended Minima");
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( imgGradient, this.toleranceWatershed, conn );
		//progressBar.show(0.4);
		
		
		// impose minima on gradient image
		System.out.println("impose minima");
		IJ.log("Impose Minima");
		ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima( imgGradient, regionalMinima, conn );
		//progressBar.show(0.5);

		
		// label minima using connected components (32-bit output)
		System.out.println("Labelling");
		IJ.log("Labelling");
		//convert image to 16 bits to enable more labels???
		ImageStack labeledMinima;
		try {
			labeledMinima = BinaryImages.componentsLabeling( regionalMinima, conn, BitD );
		} catch (Exception e) {
			ImagePlus regMinip = new ImagePlus("",regionalMinima);
			ImageConverter converter = new ImageConverter(regMinip);
			converter.convertToGray16();
			labeledMinima = BinaryImages.componentsLabeling( regMinip.getImageStack(), conn, regMinip.getBitDepth());
			
			//if we change the bitDepth of labeledMinima, also the imposed minima
			ImagePlus impMin = new ImagePlus("",imposedMinima);
			ImageConverter converter2 = new ImageConverter(impMin);
			converter2.convertToGray16();
			imposedMinima = impMin.getImageStack();
		}
		//progressBar.show(0.6);
		
		
		// apply marker-based watershed using the labeled minima on the minima-imposed 
		System.out.println("Watershed");
		IJ.log("Watershed");
		ImageStack resultStack = Watershed.computeWatershed( imposedMinima, labeledMinima, conn, dams );
		//progressBar.show(0.85);


		/******get array of volumes******/
		System.out.println("Get labelled volumes");
		IJ.log("Get labelled volumes");
		
		LabelImages.removeLargestLabel(resultStack);	
		int[] labels = LabelImages.findAllLabels(resultStack);
		int nbLabels = labels.length;
		//Filter using volumes 3 times smaller than the median
		double[] volumes = IntrinsicVolumes3D.volumes(resultStack, labels, new ImagePlus("",resultStack).getCalibration());
		Arrays.sort(volumes);
		double thresholdVolume = (volumes[nbLabels/2]/3);
		//progressBar.show(0.9);
		
		
		/********volume opening********/
		System.out.println("Opening using the median of volumes");
		IJ.log("Opening using the median of volumes");
		ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));


		/***get colored labels and return image***/
		// create image with watershed result
		ImagePlus imp_segmentedFinal = new ImagePlus( "filtered size", imgFilterSize);
		// assign right calibration
		imp_segmentedFinal.setCalibration(initImp.getCalibration());
		// optimize display range
		Images3D.optimizeDisplayRange( imp_segmentedFinal );
		// Convert the segmented image to 8-Bit
		ImageConverter converterFinal = new ImageConverter(imp_segmentedFinal);
		converterFinal.convertToGray8();
		// Color image
		byte[][] colorMap = CommonLabelMaps.fromLabel( CommonLabelMaps.GOLDEN_ANGLE.getLabel() ).computeLut( 255, false );
		ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);//Border color
		imp_segmentedFinal.getProcessor().setColorModel(cm);
		imp_segmentedFinal.getImageStack().setColorModel(cm);
		imp_segmentedFinal.updateAndDraw();
		
		//progressBar.show(1);
		return imp_segmentedFinal;
	}

}
