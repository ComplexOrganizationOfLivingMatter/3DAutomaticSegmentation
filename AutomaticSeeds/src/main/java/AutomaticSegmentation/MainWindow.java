package AutomaticSegmentation;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ColorModel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;

import Utilities.Counter3D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
//import ij.IJ;
//import ij.ImagePlus;
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
import net.miginfocom.swing.MigLayout;


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
		setMinimumSize(new Dimension(300, 300));
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
				imp.show();
				ImagePlus imp_segmented = NucleiSegmentation(imp);
				RoiManager rm = getNucleiROIs(imp_segmented);

			}
		});
		
		
		CurrentImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Get image from the workspace (ADD any exception)
				ImagePlus imp= IJ.getImage();
				ImagePlus imp_segmented = NucleiSegmentation(imp);
				RoiManager rm = getNucleiROIs(imp_segmented);
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
		
		
		
		return imp_segmented;
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
}