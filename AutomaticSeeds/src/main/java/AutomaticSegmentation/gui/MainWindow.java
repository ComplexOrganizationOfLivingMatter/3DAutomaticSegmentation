package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import AutomaticSegmentation.SegmZebrafish;
import AutomaticSegmentation.SegmentingNucleiGlands;
import AutomaticSegmentation.ThresholdMethod;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import inra.ijpb.geometry.Ellipsoid;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.region3d.Centroid3D;
import inra.ijpb.measure.region3d.InertiaEllipsoid;
import net.miginfocom.swing.MigLayout;

public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private JPanel panel;
	/**
	 * 
	 */
	private JButton btNewImage;
	/**
	 * 
	 */
	private JComboBox<String> cbPredefinedTypeSegmentation;

	/**
	 * 
	 */
	private JButton btCurrentImageButton;
	
	/**
	 * 
	 */
	private ProgressBar progressBar;
	
	/**
	 * 
	 */
	private JComboBox<ThresholdMethod> cbThresholdMethod;

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

		// Main panel
		panel = new JPanel();
		panel.setLayout(new MigLayout());

		// Create 'open' button
		btNewImage = new JButton("Open new image");

		// Create 'Select Current Image' button
		btCurrentImageButton = new JButton("Select current image");

		// Create ProgressBar
		progressBar = new ProgressBar(100, 25);

		// Create ComboBox
		cbPredefinedTypeSegmentation = new JComboBox<String>();
		cbPredefinedTypeSegmentation.addItem("Select a type of DAPI segmentation");
		cbPredefinedTypeSegmentation.addItem("Salivary glands (cylinder monolayer)");
		cbPredefinedTypeSegmentation.addItem("Zebrafish multilayer");
		
		cbThresholdMethod = new JComboBox<ThresholdMethod>(ThresholdMethod.values());
		cbThresholdMethod.setSelectedIndex(0);
		

		// Add components
		panel.add(cbThresholdMethod, "wrap");
		panel.add(btNewImage, "wrap");
		panel.add(btCurrentImageButton, "wrap");
		panel.add(cbPredefinedTypeSegmentation, "wrap");
		cbPredefinedTypeSegmentation.setSelectedIndex(0);
		panel.add(progressBar);

		btCurrentImageButton.setEnabled(false);
		btNewImage.setEnabled(false);
		// Associate this panel to the window
		getContentPane().add(panel);

		cbPredefinedTypeSegmentation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (cbPredefinedTypeSegmentation.getSelectedIndex() == 0) {
					btCurrentImageButton.setEnabled(false);
					btNewImage.setEnabled(false);
				} else {
					btCurrentImageButton.setEnabled(true);
					btNewImage.setEnabled(true);
				}

			}
		});
		btNewImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Open the image (ADD any exception)
				ImagePlus imp = IJ.openImage();
				/*
				 * WindowManager.addWindow(imp.getWindow()); imp.show();
				 */
				imp.show();
				
				ImagePlus imp_segmented = runSegmentation(imp.duplicate());

				// visualization3D (imp_segmented);

			}
		});

		btCurrentImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Get image from the workspace (ADD any exception)
				ImagePlus imp = IJ.getImage();

				ImagePlus imp_segmented = runSegmentation(imp.duplicate());

				// visualization3D (imp_segmented);

			}
		});

	}

	public RoiManager getNucleiROIs(ImagePlus imp_segmented) {
		// 3D-OC options settings
		Prefs.set("3D-OC-Options_centroid.boolean", true);

		int[] labels = LabelImages.findAllLabels(imp_segmented.getImageStack());
		// deprecatedGeometricMeasures3D - investigate about the new region3D
		double[][] centroidList = Centroid3D.centroids(imp_segmented.getImageStack(), labels);

		// double[][] centroidList = Centroid3D.centroids();
		// 0 0 1 2
		// | | | |
		// centroid -> [id][x,y,z]

		Ellipsoid[] ellipsoid = InertiaEllipsoid.inertiaEllipsoids(imp_segmented.getImageStack(), labels,
				imp_segmented.getCalibration());

		// Creating the ROI manager
		RoiManager rm = new RoiManager();
		// Reset to 0 the RoiManager
		rm.reset();
		// Adding ROI to ROI Manager
		ImagePlus impOpen = IJ.getImage();

		for (int i = 0; i < centroidList.length; i++) {
			// Get the slice to create the ROI
			int z = (int) Math.round(centroidList[i][2]);
			// Get the area and radius of the index i nuclei
			double majorRadius = 1.5 * (ellipsoid[i].radius1()) / (imp_segmented.getCalibration().pixelHeight);
			int r = (int) Math.round(majorRadius);
			impOpen.setSlice(z);
			Roi roi = new OvalRoi(centroidList[i][0] - r / 2, centroidList[i][1] - r / 2, r, r);
			rm.addRoi(roi);
		}
		return rm;
	};
	
	/**
	 * @param input
	 * @param imp_segmented
	 */
	public ImagePlus runSegmentation(ImagePlus input) {
		cbPredefinedTypeSegmentation.setEnabled(false);
		btCurrentImageButton.setEnabled(false);
		btNewImage.setEnabled(false);
		
		ImagePlus imp_segmented = null;

		switch (cbPredefinedTypeSegmentation.getSelectedIndex()) {
		case 1:
			SegmentingNucleiGlands segGland = new SegmentingNucleiGlands(input);
			segGland.segmentationProtocol(false, (ThresholdMethod) cbThresholdMethod.getSelectedItem());
			imp_segmented = segGland.getOuputImp().duplicate();
			break;

		case 2:
			SegmZebrafish segZeb = new SegmZebrafish(input);
			imp_segmented = segZeb.getOuputImp().duplicate();
			break;
		}
		
		RoiManager rm = getNucleiROIs(imp_segmented);
		imp_segmented.show();
		cbPredefinedTypeSegmentation.setEnabled(true);
		
		return imp_segmented;
	}

	public void visualization3D(ImagePlus imp) {

		/*
		 * // set to true to display messages in log window boolean verbose =
		 * false;
		 */

		// set display range to 0-255 so the displayed colors
		// correspond to the LUT values
		imp.setDisplayRange(0, 255);
		imp.updateAndDraw();

		/*
		 * // calculate array of all labels in image int[] labels =
		 * LabelImages.findAllLabels( imp );
		 */

		// create 3d universe
		Image3DUniverse univ = new Image3DUniverse();
		univ.addContent(imp, ContentConstants.VOLUME);
		univ.show();

		/*
		 * // read LUT from input image LUT lut = imp.getLuts()[0];
		 * 
		 * // add all labels different from zero (background) // to 3d universe
		 * for(int i=0; i<labels.length; i++ ) { if( labels[ i ] > 0 ) { int[]
		 * labelToKeep = new int[1]; labelToKeep[ 0 ] = labels[ i ]; if( verbose
		 * ) IJ.log( "Reconstructing label " + labels[ i ] + "..." ); // create
		 * new image containing only that label ImagePlus labelImp =
		 * LabelImages.keepLabels( imp, labelToKeep ); // convert image to 8-bit
		 * IJ.run( labelImp, "8-bit", "" );
		 * 
		 * // use LUT label color Color3f color = new Color3f( new
		 * java.awt.Color( lut.getRed( labels[ i ] ), lut.getGreen( labels[ i ]
		 * ), lut.getBlue( labels[ i ] ) ) ); if ( verbose ) IJ.log( "RGB( " +
		 * lut.getRed( labels[ i ] ) +", " + lut.getGreen( labels[ i ] ) + ", "
		 * + lut.getBlue( labels[ i ] ) + ")" );
		 * 
		 * boolean[] channels = new boolean[3]; channels[ 0 ] = false; channels[
		 * 1 ] = false; channels[ 2 ] = false;
		 * 
		 * // add label image with corresponding color as an isosurface
		 * 
		 * univ.addContent( labelImp, color, "label-"+labels[i], 0, channels, 2,
		 * ContentConstants.SURFACE); } }
		 * 
		 * // launch smooth control SmoothControl sc = new SmoothControl( univ
		 * );
		 */

	}

}