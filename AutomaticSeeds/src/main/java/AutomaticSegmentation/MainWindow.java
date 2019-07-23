package AutomaticSegmentation;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


import Utilities.Counter3D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.Filters3D;
import ij.plugin.Resizer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;

import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.*;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import inra.ijpb.watershed.Watershed;
import net.miginfocom.swing.MigLayout;

import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.*;
import ij3d.Image3DUniverse;
import ij3d.ContentConstants;
import org.scijava.vecmath.Color3f;
import isosurface.SmoothControl;


public class MainWindow extends JFrame{
	
	private JPanel panel;
	private JButton OpenButton;
	private JButton CurrentImageButton;
	private ProgressBar progressBar;
	
	public MainWindow() {
		String name = UIManager.getInstalledLookAndFeels()[3].getClassName();
		try {
			UIManager.setLookAndFeel(name);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		UIManager.put("Panel.background", Color.WHITE);
		UIManager.put("Slider.background", Color.WHITE);
		setMinimumSize(new Dimension(300, 200));
		setTitle("AutomaticSegmentation3D");
		// Not close Fiji when AutomaticSegmentation is closed
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		this.addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});
		
		
		// Main panel
		JPanel panel = new JPanel();
        panel.setLayout(new MigLayout());
				
		// Create 'open' button
		OpenButton = new JButton("Open new image");
		
		// Create 'Select Current Image' button
		CurrentImageButton = new JButton("Select current image");
		
		//Create ProgressBar
		progressBar = new ProgressBar(100,25);
		
		// Add components
		panel.add(OpenButton, "wrap"); 
		panel.add(CurrentImageButton, "wrap");
		panel.add(progressBar);
		
		// Associate this panel to the window
		getContentPane().add(panel);
		
		OpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
								
				//Open the image (ADD any exception)
				ImagePlus imp= IJ.openImage();	
				/*WindowManager.addWindow(imp.getWindow());
				imp.show();*/
				imp.show();
				ImagePlus input = imp.duplicate();				
				ImagePlus imp_segmented = new ImagePlus(); 
				imp_segmented = NucleiSegmentation(input);
		    	RoiManager rm = getNucleiROIs(imp_segmented);
		    	imp_segmented.show();
				
				//visualization3D (imp_segmented);

			}
		});
		
		
		CurrentImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Get image from the workspace (ADD any exception)
				ImagePlus imp= IJ.getImage();
				ImagePlus input = (ImagePlus) imp.duplicate();					
				ImagePlus imp_segmented = NucleiSegmentation(input);
				//visualization3D (imp_segmented);
				
				//RoiManager rm = getNucleiROIs(imp_segmented);
				
				
			}
		});
		

	}
	
	
	
	public ImagePlus NucleiSegmentation(ImagePlus imp) {
		
		//ContrastAdjuster adjuster = new ContrastAdjuster();
		//Test
		//Convert the image to 8-Bit
		if(imp.getBitDepth() != 8) {
			ImageConverter converter = new ImageConverter(imp);
			converter.convertToGray8();
		}
		//Test
		IJ.log(imp.getBitDepth()+"-bits convertion");
			
		
		int radius = 2;
		int tolerance = 0; //modify??
		int conn = 6;
		int BitD = imp.getBitDepth();
		
		
		/*****Enhance Stack contrast with median threshold******/
		// Retrieve filtered stack 
		Filters3D.filter(imp.getStack(),Filters3D.MEAN, 3, 3, 3); 	
		
		IJ.log("Applying Huang filter and automatic threshold");
		int[] thresh = new int[imp.getStackSize()+1];
		ArrayList<Integer> thresholds = new ArrayList<Integer>();
		for(int i=1;i<=imp.getStackSize();i++) {
			ImageProcessor processor = imp.getStack().getProcessor(i);
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
		System.out.println("thresh: "+medianThresh);
		
		
		ImagePlus imp_segmented = new ImagePlus();
		imp_segmented = (ImagePlus) imp.clone();
		for(int i=1;i<=imp.getStackSize();i++) {
			ImageProcessor processor = imp.getStack().getProcessor(i);
			processor.threshold(intMedianThresh);
			imp_segmented.getStack().setProcessor((ImageProcessor) processor.clone(), i);
		}
			
		// create structuring element (cube of radius 'radius')
		Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(radius);

		IJ.log("1 - Fill 3D particles");
		// fill 3D particles
		//ImageStack imgFilled = Reconstruction3D.fillHoles(impClosed);
		
		//loop for to close hole in 2D. Dilatation + erosion + fill holes
		Strel shape2D = Strel.Shape.DISK.fromRadius(radius);
		
		int newDepth = (int) Math.round(imp.getStackSize()*4.16);
		Resizer resizer = new Resizer();
		resizer.setAverageWhenDownsizing(true);
		ImagePlus imgResized = resizer.zScale(imp_segmented.duplicate(),newDepth,ImageProcessor.BILINEAR);
		System.out.println("Resizing");
		
		ImageStack imgFilled = imgResized.getStack().duplicate();
		
		for(int i=1;i<=imgResized.getStackSize();i++) {
			ImageProcessor processor = imgResized.getStack().getProcessor(i);
			processor = Morphology.closing(processor, shape2D);
			processor = BinaryImages.binarize(processor);
			processor = Reconstruction.fillHoles(processor);
			imgFilled.setProcessor((ImageProcessor) processor.duplicate(), i);
		}
		progressBar.show(0.1);	
		
		System.out.println("Closing and Filling");

		
		ImageStack imgFilterSmall = LabelImages.volumeOpening(imgFilled, 50);
		System.out.println("Small volume opening");

		IJ.log("2 - Gradient");
		// apply morphological gradient to input image
		ImageStack image = Morphology.gradient(imgResized.getImageStack(), shape3D);
		progressBar.show(0.25);
		System.out.println("Gradient");

		
		IJ.log("3 - Extended Minima");
		// find regional minima on gradient image with dynamic value of 'tolerance' and 'conn'-connectivity
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, tolerance, conn );
		progressBar.show(0.4);
		System.out.println("Extended minima");
		
		IJ.log("4 - Impose Minima");
		// impose minima on gradient image
		ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima( image, regionalMinima, conn );
		progressBar.show(0.5);
		System.out.println("impose minima");

		IJ.log("5 - Labelling");
		// label minima using connected components (32-bit output)
		//convert image to 16 bits to enable more labels???
		ImageStack labeledMinima;
		try {
			labeledMinima = BinaryImages.componentsLabeling( regionalMinima, conn, BitD );
		} catch (Exception e) {
			ImagePlus regMinip = new ImagePlus("",regionalMinima);
			ImageConverter converter = new ImageConverter(regMinip);
			converter.convertToGray16();
			labeledMinima = BinaryImages.componentsLabeling( regMinip.getImageStack(), conn, regMinip.getBitDepth());
		}
		progressBar.show(0.6);
		System.out.println("labelling");
		
		IJ.log("6 - Watershed");
		// apply marker-based watershed using the labeled minima on the minima-imposed 
		// gradient image (the last value indicates the use of dams in the output)
		boolean dams = false;
		//ImageStack resultStack = Watershed.computeWatershed(image,labeledMinima,imgFilled, conn, dams );
		ImageStack resultStack = Watershed.computeWatershed( imposedMinima, labeledMinima, conn, dams );
		progressBar.show(0.85);
		System.out.println("watershed");


		
		/******get array of volumes******/
		IJ.log("7 - Get Volumes");
		
		int[] labels = LabelImages.findAllLabels(resultStack);
		int nbLabels = labels.length;
		
		//Filter using volumes 3 times smallen than the median
		double[] volumes = IntrinsicVolumes3D.volumes(resultStack, labels, imp.getCalibration());
		Arrays.sort(volumes);
		double thresholdVolume = (volumes[nbLabels/2]/3);
		
		int[] labels2 = {0};
		for(int i = 0; i < nbLabels; i++)
		   {
			labels2[0] = i+1;
			LabelImages.replaceLabels(resultStack,labels2, i);
		   }			
		
		progressBar.show(0.9);
		IJ.log("8 - Volume Opening");
		ImageStack imgFilterSize = LabelImages.volumeOpening(resultStack, (int) Math.round(thresholdVolume));
		System.out.println("opening using the median of sizes");
	
		
		// create image with watershed result
		ImagePlus imp_segmentedFinal = new ImagePlus( "filtered size", imgFilterSize);
	
				
		// assign right calibration
		imp_segmentedFinal.setCalibration( imp.getCalibration() );
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
		
		progressBar.show(1);

		
		return imp_segmentedFinal;
	}
	
	
	public RoiManager getNucleiROIs(ImagePlus imp_segmented){
		//3D-OC options settings
        Prefs.set("3D-OC-Options_centroid.boolean", true);
        
		int[] labels = LabelImages.findAllLabels(imp_segmented.getImageStack());
		double[] resol = new double[]{1, 1, 1};
		// deprecatedGeometricMeasures3D - investigate about the new region3D
		double[][] centroidList = GeometricMeasures3D.centroids(imp_segmented.getImageStack(),labels);
		//double[][] ellipsoids = GeometricMeasures3D.inertiaEllipsoid(imp_segmented.getImageStack(),labels, resol);
		double[][] radiiSphere = GeometricMeasures3D.maximumInscribedSphere(imp_segmented.getImageStack(),labels,resol);
		//double[][] elongations = GeometricMeasures3D.computeEllipsoidElongations(ellipsoids);	
				
		/*Counter3D counter = new Counter3D(imp_segmented);//, 10, 650, 92274688, false, false);
		float[][] centroidList = counter.getCentroidList();*/
		//              0  0 1 2
		//              |  | | |
		//centroid -> [id][x,y,z]
		
		//Creating the ROI manager
		RoiManager rm = new RoiManager();
		//Reset to 0 the RoiManager
		rm.reset();
		//Adding ROI to ROI Manager
		ImagePlus impOpen= IJ.getImage();
		
		for(int i = 0; i<radiiSphere.length;i++) {
			//Get the slice to create the ROI
			int z = (int) Math.round(centroidList[i][2]);
			//Get the area and radius of the index i nuclei
			int r = (int) Math.round(radiiSphere[i][2]);
			
			impOpen.setSlice(z);
			Roi roi = new OvalRoi(centroidList[i][0]-r/2,centroidList[i][1]-r/2,r,r);
			rm.addRoi(roi);
			
		}
		return rm;
	};
	
	
	public void visualization3D (ImagePlus imp){
		
		/*// set to true to display messages in log window
		boolean verbose = false;*/
		
		// set display range to 0-255 so the displayed colors
		// correspond to the LUT values
		imp.setDisplayRange( 0, 255 );
		imp.updateAndDraw();

		/*// calculate array of all labels in image
		int[] labels = LabelImages.findAllLabels( imp );*/

		// create 3d universe
		Image3DUniverse univ = new Image3DUniverse();
		univ.addContent(imp, ContentConstants.VOLUME);
		univ.show();

		/*// read LUT from input image
		LUT lut = imp.getLuts()[0];
 
		// add all labels different from zero (background)
		// to 3d universe
		for(int i=0; i<labels.length; i++ )
		{
			if( labels[ i ] > 0 )
			{
				int[] labelToKeep = new int[1];
				labelToKeep[ 0 ] = labels[ i ];
				if( verbose )
					IJ.log( "Reconstructing label " + labels[ i ] + "..." );
				// create new image containing only that label
				ImagePlus labelImp = LabelImages.keepLabels( imp, labelToKeep );
				// convert image to 8-bit
				IJ.run( labelImp, "8-bit", "" );

				 // use LUT label color
				Color3f color = new Color3f( new java.awt.Color( lut.getRed( labels[ i ] ),
						lut.getGreen( labels[ i ] ),
						lut.getBlue( labels[ i ] ) ) );
				if ( verbose )
						IJ.log( "RGB( " + lut.getRed( labels[ i ] ) +", "
								+ lut.getGreen( labels[ i ] )
								+ ", " + lut.getBlue( labels[ i ] ) + ")" );
		
				boolean[] channels = new boolean[3];
				channels[ 0 ] = false;
				channels[ 1 ] = false;
				channels[ 2 ] = false;
		
				// add label image with corresponding color as an isosurface

				univ.addContent( labelImp, color, "label-"+labels[i], 0, channels, 2, ContentConstants.SURFACE);
			}
		}
		
		// launch smooth control
		SmoothControl sc = new SmoothControl( univ );*/
		
	}
	
	
	
	
}