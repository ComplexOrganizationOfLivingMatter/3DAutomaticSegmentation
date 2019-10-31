//package AutomaticSegmentation.preProcessing;
//
//import java.util.Arrays;
//
//import ij.IJ;
//import ij.ImagePlus;
//import ij.ImageStack;
//import ij.process.ImageConverter;
//import ij.process.ImageProcessor;
//import inra.ijpb.binary.BinaryImages;
//import inra.ijpb.label.LabelImages;
//import inra.ijpb.measure.IntrinsicVolumes3D;
//import inra.ijpb.morphology.Morphology;
//import inra.ijpb.morphology.Reconstruction;
//import inra.ijpb.morphology.Strel;
//import net.haesleinhuepf.clij.CLIJ;
//
///**
// * 
// * @author Pedro Gómez-Gálvez, Pedro Rodríguez-Hiruela and Pablo Vicente-Munuera
// *
// */
//public class DefaultSegmentation implements genericSegmentation {
//	
//	private ImagePlus inputNucleiImp;
//	private ImagePlus outputImp;
//	private int strelRadius2D;
//	private int strelRadius3D;
//	private int toleranceWatershed;
//	private int pixelsToOpenVolume;
//
//	public DefaultSegmentation(ImagePlus impNuclei) {
//		this.strelRadius2D = 4;
//		this.strelRadius3D = 3;
//		// 10 is a good start point for 8-bit images, 2000 for 16-bits. Minor
//		// tolerance more divided objects with watershed
//		this.toleranceWatershed = 0;
//		this.inputNucleiImp = impNuclei;
//		this.pixelsToOpenVolume = 50;
//	}
//
//	public DefaultSegmentation(ImagePlus impNuclei,int radius2D, int radius3D, int tolerance, int pixelsToOpenVolume) {
//		this.strelRadius2D = radius2D;
//		this.strelRadius3D = radius3D;
//		this.toleranceWatershed = tolerance;
//		this.inputNucleiImp = impNuclei;
//		this.pixelsToOpenVolume = pixelsToOpenVolume;
//	}
//
//	/**
//	 * @return the ouputImp
//	 */
//	public ImagePlus getOuputImp() {
//		return outputImp;
//	}
//
//	/**
//	 * @return the segmentedImage
//	 */
//	public void segmentationProtocol(CLIJ clij, String thresholdMethod) {
//
//		// Convert the image to 8-Bit
//		if (this.inputNucleiImp.getBitDepth() != 8) {
//			ImageConverter converter = new ImageConverter(this.inputNucleiImp);
//			converter.convertToGray8();
//		}
//		//this.inputNucleiImp.duplicate().show();
//		int BitD = this.inputNucleiImp.getBitDepth();
//		boolean dams = false;
//		// double resizeFactor = 1;
//
//		IJ.log(BitD + "-bits conversion");
//		System.out.println(BitD + "-bits conversion");
//
//		inputNucleiImp.duplicate().show();
//		
//		ImagePlus filteredImp = filterPreprocessing(this.inputNucleiImp, clij, strelRadius3D);
//		
//		filteredImp.duplicate().show();
//
//		//ImagePlus imp_segmented = automaticThreshold(filteredImp, thresholdMethod);
//		
//		/***** loop for closing, binarize and filling holes in 2D *****/
//		System.out.println("Closing, binarize and filling");
//		IJ.log("Closing, binarize and filling");
//		Strel shape2D = Strel.Shape.DISK.fromRadius(this.strelRadius2D);
//		ImageStack imgFilled = filteredImp.getStack().duplicate();
//		for (int i = 1; i <= filteredImp.getStackSize(); i++) {
//			
//			System.out.println(i);
//			ImageProcessor processor = filteredImp.getStack().getProcessor(i);
//			processor = Morphology.closing(processor, shape2D);
//			processor = BinaryImages.binarize(processor);
//			processor = Reconstruction.fillHoles(processor);
//			imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);
//			
//		}
//		// progressBar.show(0.1);
//
//		// Volume opening
//		System.out.println("Small volume opening");
//		IJ.log("Small volume opening");
//		ImageStack imgFilterSmall = BinaryImages.volumeOpening(imgFilled, pixelsToOpenVolume);
//
////		ImagePlus imgToShow = new ImagePlus("volumeOpening", imgFilterSmall);
////		imgToShow.show();
//
//		ImageStack resultStack = watershedProcess(BitD, dams, imgFilterSmall, strelRadius3D, toleranceWatershed);
//
//		/****** get array of volumes ******/
//		System.out.println("Get labelled volumes");
//		IJ.log("Get labelled volumes");
//
//		LabelImages.removeLargestLabel(resultStack);
//		int[] labels = LabelImages.findAllLabels(resultStack);
//		int nbLabels = labels.length;
//		// Filter using volumes 3 times smaller than the median
//		double[] volumes = IntrinsicVolumes3D.volumes(resultStack, labels,
//				new ImagePlus("", resultStack).getCalibration());
//		Arrays.sort(volumes);
//		double thresholdVolume = (volumes[nbLabels / 2] / 3);
//		// progressBar.show(0.9);
//
//		/******** volume opening ********/
//		System.out.println("Opening using the median of volumes");
//		IJ.log("Opening using the median of volumes");
//		ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));
//
//		ImagePlus imp_segmentedFinal = createColouredImageWithLabels(this.inputNucleiImp, imgFilterSize);
//
//		// progressBar.show(1);
//		
//		this.outputImp = imp_segmentedFinal;
//	}
//}
