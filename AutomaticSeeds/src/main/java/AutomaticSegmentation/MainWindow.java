package AutomaticSegmentation;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ColorModel;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


import Utilities.Counter3D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
//import ij.IJ;
//import ij.ImagePlus;
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
import ij3d.Image3DUniverse;
import ij3d.ContentConstants;
import org.scijava.vecmath.Color3f;
import isosurface.SmoothControl;


public class MainWindow extends JFrame{
	
	private JPanel panel;
	private JButton OpenButton;
	private JButton CurrentImageButton;
	
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
		
		// Add components
		panel.add(OpenButton, "wrap"); 
		panel.add(CurrentImageButton);

		// Associate this panel to the window
		getContentPane().add(panel);
		
		OpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
								
				//Open the image (ADD any exception)
				ImagePlus imp= IJ.openImage();	
				/*WindowManager.addWindow(imp.getWindow());
				imp.show();*/


				ImagePlus imp_segmented = NucleiSegmentation((ImagePlus) imp.clone());
				imp_segmented.show();
				//RoiManager rm = getNucleiROIs(imp_segmented);
				//visualization3D (imp_segmented);

			}
		});
		
		
		CurrentImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Get image from the workspace (ADD any exception)
				ImagePlus imp= IJ.getImage();
				ImagePlus imp_segmented = NucleiSegmentation(imp);
				
				visualization3D (imp_segmented);
				
				//RoiManager rm = getNucleiROIs(imp_segmented);
				
				
			}
		});
		

	}
	
	
	
	public ImagePlus NucleiSegmentation(ImagePlus imp) {
		
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
			
		
		int radius = 2;
		int tolerance = 20;
		int conn = 26;
		int BitD = imp.getBitDepth();
		
		
		/*****Enhace Stack contrast with median threshold******/
		
		int[] thresh = new int[imp.getStackSize()+1];
		for(int i=1;i<=imp.getStackSize();i++) {
			ImageProcessor processor = imp.getStack().getProcessor(i);
			processor.setAutoThreshold("Huang");
			thresh[i-1] = processor.getAutoThreshold();
		}
		Arrays.sort(thresh);
		double medianThresh = 0;
		if (thresh.length % 2 == 0){
			medianThresh = ((double)thresh[thresh.length/2] + (double)thresh[thresh.length/2 - 1])/2;
		}else{
			medianThresh = (double) thresh[thresh.length/2];
		}
		int intMedianThresh = (int) Math.round(medianThresh);
		System.out.println("thresh: "+medianThresh);
		
		
		ImagePlus imp_segmented = new ImagePlus();
		imp_segmented = (ImagePlus) imp.clone();
		for(int i=1;i<=imp.getStackSize();i++) {
			ImageProcessor processor = imp.getStack().getProcessor(i);
			processor.threshold(intMedianThresh);
			imp_segmented.getStack().setProcessor((ImageProcessor) processor.clone(), i);
		}
		
		 System.out.println("Profundidad imagen segmentada: "+ imp_segmented.getBitDepth());
	
		
		// create structuring element (cube of radius 'radius')
		Strel3D shape3D = Strel3D.Shape.BALL.fromRadius(radius);
		// apply morphological gradient to input image
		ImageStack image = new ImageStack();	
		image = Morphology.gradient( imp_segmented.getImageStack(), shape3D );

		// find regional minima on gradient image with dynamic value of 'tolerance' and 'conn'-connectivity
		ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, tolerance, conn );
		// impose minima on gradient image
		ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima( image, regionalMinima, conn );
		// label minima using connected components (32-bit output)
		ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, conn, BitD );
		// apply marker-based watershed using the labeled minima on the minima-imposed 
		// gradient image (the last value indicates the use of dams in the output)
		boolean dams = false;
		ImageStack resultStack = Watershed.computeWatershed( imposedMinima, labeledMinima, conn, dams );

				
		// create image with watershed result
		ImagePlus imp_segmented2 = new ImagePlus( "watershed", resultStack );
		// assign right calibration
		imp_segmented2.setCalibration( imp.getCalibration() );
		// optimize display range
		Images3D.optimizeDisplayRange( imp_segmented2 );
		
			
		/*
		
		ImageStack imgTopHat = Morphology.whiteTopHat(imp.getImageStack(), shape3D);
		ImageStack imgFilled = Reconstruction3D.fillHoles(imgTopHat);
		
		ImageStack imgFilterSize = volumeOpening(imgFilled, int minVolume)
		
		
		//imp.show();
		imp.setStack(imgFilled);
		//Create threshold and binarize the image
		
		//IJ.run(imp,"Make Binary","");
		
		*/
		
		
	//    ImagePlus imp_segmented = new ImagePlus("MorphSegmented",ip_segmented);
	//    imp_segmented.setCalibration(imp.getCalibration());
	    //ImagePlus binarized = BinaryImages.binarize(imp_segmented);
	    
//	    // Adjust min and max values to display
//		Images3D.optimizeDisplayRange( imp_segmented );

		
//		byte[][] colorMap = CommonLabelMaps.fromLabel( CommonLabelMaps.GOLDEN_ANGLE.getLabel() ).computeLut( 255, false );
//		ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);//Border color
//		imp_segmented.getProcessor().setColorModel(cm);
//		imp_segmented.getImageStack().setColorModel(cm);
//		imp_segmented.updateAndDraw();
//		
//		//Convert the segmented image to 8-Bit
		ImageConverter converter2 = new ImageConverter(imp_segmented2);
		converter2.convertToGray8();
		//Test
	    System.out.println("Profundidad imagen segmentada: "+ imp_segmented2.getBitDepth());
		
		return imp_segmented2;
	}
	
	
	public RoiManager getNucleiROIs(ImagePlus imp_segmented){
		//3D-OC options settings
        Prefs.set("3D-OC-Options_centroid.boolean", true);
		
			
		Counter3D counter = new Counter3D(imp_segmented, 10, 650, 92274688, false, false);
		ImagePlus result = counter.getObjMap(true, 30);
		
		float[][] centroidList = counter.getCentroidList();
		//              0  0 1 2
		//              |  | | |
		//centroid -> [id][x,y,z]
		
		//Creating the ROI manager
		RoiManager rm = new RoiManager();
		//Reset to 0 the RoiManager
		rm.reset();
		//Adding ROI to ROI Manager
		for(int i = 0; i<centroidList.length;i++) {
			//Get the coordinate x
			double x = centroidList[i][0];
			//Get the cordinate y
			double y = centroidList[i][1];
			//Get the slice to create the ROI
			int z = (int)centroidList[i][2];
			//Get the area and radius of the index i nuclei
			int a = counter.getObject(i).surf_size;
			int r = Math.round((float)Math.sqrt(a/Math.PI));
			
			imp_segmented.setSlice(z);
			Roi roi = new OvalRoi(x-r/2,y-r/2,r,r);
			rm.addRoi(roi);
			
		}
		result.show();
		counter.showStatistics(true);
		counter.showSummary();
		
		return rm;
	};
	
	
	public void visualization3D (ImagePlus imp){
		
		// set to true to display messages in log window
		boolean verbose = false;
		
		// set display range to 0-255 so the displayed colors
		// correspond to the LUT values
		imp.setDisplayRange( 0, 255 );
		imp.updateAndDraw();

		// calculate array of all labels in image
		int[] labels = LabelImages.findAllLabels( imp );

		// create 3d universe
		Image3DUniverse univ = new Image3DUniverse();
		univ.show();

		// read LUT from input image
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
		SmoothControl sc = new SmoothControl( univ );
		
	}
	
}