/**
 * AutomaticSegmentation
 */
package AutomaticSegmentation;

import java.awt.Color;
import java.awt.image.ColorModel;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.GeodesicReconstruction3D;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import inra.ijpb.watershed.Watershed;




public class NucleiSegmentation{

	// attributes
	private ImagePlus inputNuclei;
	private ImagePlus segmentedNuclei;
	/**
	 * Constructor by default
	 */
	public NucleiSegmentation(ImagePlus imp) {
		this.inputNuclei = imp;
		
		//ContrastAdjuster adjuster = new ContrastAdjuster();
		//Test
		System.out.println("Sin convertir: "+imp.getBitDepth());
		//Convert the image to 8-Bit
		if(imp.getBitDepth() != 8) {
			ImageConverter converter = new ImageConverter(imp);
			converter.convertToGray8();
		}
		//Test
		System.out.println("Convertida: "+imp.getBitDepth());
		Strel3D gradient = Strel3D.Shape.CUBE.fromRadius(2);
	
		
		ImageStack filter1 = Morphology.externalGradient(imp.getImageStack(), gradient);
		ImageStack filled = GeodesicReconstruction3D.fillHoles(filter1);
		//imp.show();
		imp.setStack(filled);
		//Create threshold and binarize the image
		
		//IJ.run(imp,"Make Binary","");
		
		ImageProcessor processor = imp.getStack().getProcessor((int)imp.getStackSize()/2);
		int thresh = processor.getAutoThreshold();
		
		System.out.println("thresh: "+thresh);
		for(int i=1;i<=imp.getStackSize();i++) {
			processor = imp.getStack().getProcessor(i);

			//processor.setAutoThreshold("Triangle", true, 1);
			processor.threshold(thresh);
			
			imp.getStack().setProcessor(processor, i);
		}
		ImageStack filter2 = Morphology.erosion(imp.getImageStack(), gradient);
		ImagePlus filteredImage = new ImagePlus("Filtered Image",filter2);
		filteredImage.show();
		//Test
		System.out.println("Esta binarizada: "+imp.getStack().getProcessor(5).isBinary());
		
		//Morphological segmentation 
		//Settings
		int gradient_radius = 3;
		int tol = 4;
		int conn = 26;
		
		
		
		Strel3D gradient2 = Strel3D.Shape.CUBE.fromRadius(gradient_radius);
		
		//Segmentation
		ImageStack image = Morphology.gradient(filter2, gradient2);
		
		
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, tol, conn );
		
		ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima( image, regionalMinima, conn );
		
		ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, conn, 32);
		
		
		
	    // apply marker-based watershed using the labeled minima on the minima-imposed gradient image (the true value indicates the use of dams in the output)
		ImageStack ip_segmented = Watershed.computeWatershed( imposedMinima, labeledMinima, conn, true );
		
	    ImagePlus imp_segmented = new ImagePlus("MorphSegmented",ip_segmented);
	    imp_segmented.setCalibration(imp.getCalibration());
	    //ImagePlus binarized = BinaryImages.binarize(imp_segmented);
	    
	    // Adjust min and max values to display
		Images3D.optimizeDisplayRange( imp_segmented );

		byte[][] colorMap = CommonLabelMaps.fromLabel( CommonLabelMaps.GOLDEN_ANGLE.getLabel() ).computeLut( 255, false );
		ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);//Border color
		imp_segmented.getProcessor().setColorModel(cm);
		imp_segmented.getImageStack().setColorModel(cm);
		imp_segmented.updateAndDraw();
		
		//Convert the segmented image to 8-Bit
		ImageConverter converter2 = new ImageConverter(imp_segmented);
		converter2.convertToGray8();
		//Test
	    System.out.println("Profundidad imagen segmentada: "+ imp_segmented.getBitDepth());
		imp_segmented.show();
		
		
		
		this.segmentedNuclei = imp_segmented;
	}
	/**
	 * @return the segmentedNuclei
	 */
	public ImagePlus getSegmentedNuclei() {
		return segmentedNuclei;
	}

	public ImagePlus getInputImageNuclei() {
		return inputNuclei;
	}
	

}