package AutomaticSegmentation;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
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

public class SegmZebrafish implements genericSegmentation {
	private ImagePlus outputImp;
	private int strelRadius2D;
	private int strelRadius3D;
	private int toleranceWatershed;
	private static final int PIXELSTOOPENVOLUME = 20;

	public SegmZebrafish(ImagePlus imp) {
		strelRadius2D = 2;
		strelRadius3D = 2;
		// 10 is a good start point for 8-bit images, 2000 for 16-bits. Minor
		// tolerance more divided objects with watershed
		toleranceWatershed = 0;
		segmentationProtocol(imp, false);
	}

	public SegmZebrafish(ImagePlus imp, int radius2D, int radius3D, int tolerance) {
		strelRadius2D = radius2D;
		strelRadius3D = radius3D;
		toleranceWatershed = tolerance;
		segmentationProtocol(imp, false);
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
	public void segmentationProtocol(ImagePlus initImp, boolean gpuOption) {

		// Convert the image to 8-Bit
		if (initImp.getBitDepth() != 8) {
			ImageConverter converter = new ImageConverter(initImp);
			converter.convertToGray8();
		}
		// Test
		int BitD = initImp.getBitDepth();
		boolean dams = false;
		// double resizeFactor = 1;

		IJ.log(BitD + "-bits convertion");
		System.out.println(BitD + "-bits convertion");

		initImp = filterPreprocessing(initImp, gpuOption, strelRadius3D);

		initImp.show();

		ImagePlus imp_segmented = automaticThreshold(initImp, "Huang");

		// create structuring element (cube of radius 'radius')
		Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(strelRadius3D);

		IJ.log("1 - Fill 3D particles");
		// fill 3D particles
		// ImageStack imgFilled = Reconstruction3D.fillHoles(impClosed);

		// loop for to close hole in 2D. Dilatation + erosion + fill holes
		Strel shape2D = Strel.Shape.DISK.fromRadius(strelRadius2D);

		/***** loop for closing, binarize and filling holes in 2D *****/
		System.out.println("Closing, binarize and filling");
		IJ.log("Closing, binarize and filling");
		ImageStack imgFilled = imp_segmented.getStack().duplicate();
		for (int i = 1; i <= imp_segmented.getStackSize(); i++) {
			ImageProcessor processor = imp_segmented.getStack().getProcessor(i);
			processor = Morphology.opening(processor, shape2D);
			processor = Morphology.closing(processor, shape2D);
			processor = BinaryImages.binarize(processor);
			processor = Reconstruction.fillHoles(processor);
			imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);
		}
		// progressBar.show(0.1);

		// Volume opening
		System.out.println("Small volume opening");
		IJ.log("Small volume opening");
		ImageStack imgFilterSmall = BinaryImages.volumeOpening(imgFilled, PIXELSTOOPENVOLUME);

		ImageStack resultStack = watershedProcess(BitD, dams, imgFilterSmall, strelRadius3D, toleranceWatershed);

		/****** get array of volumes ******/
		System.out.println("Get labelled volumes");
		IJ.log("Get labelled volumes");

		LabelImages.removeLargestLabel(resultStack);
		int[] labels = LabelImages.findAllLabels(resultStack);
		int nbLabels = labels.length;
		// Filter using volumes 3 times smaller than the median
		double[] volumes = IntrinsicVolumes3D.volumes(resultStack, labels,
				new ImagePlus("", resultStack).getCalibration());
		Arrays.sort(volumes);
		double thresholdVolume = (volumes[nbLabels / 2] / 3);
		// progressBar.show(0.9);

		/******** volume opening ********/
		System.out.println("Opening using the median of volumes");
		IJ.log("Opening using the median of volumes");
		ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));

		ImagePlus imp_segmentedFinal = createColouredImageWithLabels(initImp, imgFilterSize);

		// progressBar.show(1);
		this.outputImp = imp_segmentedFinal;

	}
}
