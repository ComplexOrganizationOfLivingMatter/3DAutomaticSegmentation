package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import AutomaticSegmentation.preProcessing.DefaultSegmentation;
import AutomaticSegmentation.preProcessing.SegmZebrafish;
import AutomaticSegmentation.preProcessing.SegmentingNucleiGlands;
import AutomaticSegmentation.preProcessing.ThresholdMethod;
import AutomaticSegmentation.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import inra.ijpb.geometry.Box3D;
import inra.ijpb.geometry.Ellipsoid;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.region3d.BoundingBox3D;
import inra.ijpb.measure.region3d.Centroid3D;
import inra.ijpb.measure.region3d.EquivalentEllipsoid;
import net.haesleinhuepf.clij.CLIJ;
import net.miginfocom.swing.MigLayout;

public class PreLimeSegWindow extends JFrame {

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
	private JComboBox<String> cbPredefinedTypeSegmentation;

	/**
	 * 
	 */
	private JButton btRun;
	
	/**
	 * 
	 */
	private JCheckBox jcbGPUEnable;
	
	/**
	 * 
	 */
	private ProgressBar progressBar;
	
	/**
	 * 
	 */
	private JComboBox<ThresholdMethod> cbThresholdMethod;
	
	/**
	 * 
	 */
	private JButton btCreateROIs;
	
	/**
	 * 
	 */
	private ImagePlus imp_segmented;
	
	private ImageProcessor imp_original;

	/**
	 * 
	 */
	public PreLimeSegWindow(ImageProcessor imgProcessor) {
		this.imp_original = imgProcessor;
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

		
		// Init GUI elements
		btRun = new JButton("Run!");
		btCreateROIs = new JButton("Create ROIs");
		jcbGPUEnable = new JCheckBox("Enable GPU operations");
		jcbGPUEnable.setSelected(true);
		progressBar = new ProgressBar(100, 25);

		cbPredefinedTypeSegmentation = new JComboBox<String>();
		cbPredefinedTypeSegmentation.addItem("Select a type of DAPI segmentation");
		cbPredefinedTypeSegmentation.addItem("Default");
		cbPredefinedTypeSegmentation.addItem("Salivary glands (cylinder monolayer)");
		cbPredefinedTypeSegmentation.addItem("Zebrafish multilayer");
		
		cbThresholdMethod = new JComboBox<ThresholdMethod>(ThresholdMethod.values());
		cbThresholdMethod.setSelectedIndex(16);

		// Add components
		panel.add(btCreateROIs, "wrap");
		panel.add(cbThresholdMethod, "wrap");
		panel.add(cbPredefinedTypeSegmentation, "wrap");
		cbPredefinedTypeSegmentation.setSelectedIndex(0);
		panel.add(btRun, "wrap");
		panel.add(progressBar);

		btRun.setEnabled(false);
		// Associate this panel to the window
		getContentPane().add(panel);

		cbPredefinedTypeSegmentation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (cbPredefinedTypeSegmentation.getSelectedIndex() == 0) {
					btRun.setEnabled(false);
				} else {
					btRun.setEnabled(true);
				}

			}
		});

		btRun.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imp_segmented = runSegmentation(new ImagePlus("Nuclei", imp_original.duplicate()));
				// visualization3D (imp_segmented);
			}
		});
		
		btCreateROIs.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (imp_segmented == null){
					imp_segmented = IJ.openImage();
					imp_segmented.show();
				}
				
				RoiManager rm = getNucleiROIs(imp_segmented);
			}
		});

	}

	/**
	 * 
	 * @param imp_segmented
	 * @return
	 */
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

//		Ellipsoid[] ellipsoid = EquivalentEllipsoid.equivalentEllipsoids(imp_segmented.getImageStack(), labels,
//				imp_segmented.getCalibration());
		
		Box3D[] bboxes = BoundingBox3D.boundingBoxes(imp_segmented.getImageStack(), labels,
				imp_segmented.getCalibration());

		// Creating the ROI manager
		RoiManager rm = new RoiManager();
		// Reset to 0 the RoiManager
		rm.reset();

		for (int i = 0; i < centroidList.length; i++) {
			// Get the slice to create the ROI
			int z = (int) Math.round(centroidList[i][2]);
			// Get the area and radius of the index i nuclei
			double[] radii = {bboxes[i].height(), bboxes[i].width()};
			double[] calibrations = {imp_segmented.getCalibration().pixelHeight, imp_segmented.getCalibration().pixelWidth};
			double majorRadius = 1.2 * Utils.getMean(radii) / Utils.getMean(calibrations);
			int r = (int) Math.round(majorRadius);
			imp_segmented.setSlice(z);
			Roi roi = new OvalRoi(centroidList[i][0] - r / 2, centroidList[i][1] - r / 2, r, r);
			rm.addRoi(roi);
		}
		return rm;
	}
	
	/**
	 * @param input
	 * @param imp_segmented
	 */
	public ImagePlus runSegmentation(ImagePlus input) {
		cbPredefinedTypeSegmentation.setEnabled(false);
		btRun.setEnabled(false);
		
		ImagePlus imp_segmented = null;
		
		CLIJ clij = null;
		
		if (jcbGPUEnable.isSelected())
			clij = CLIJ.getInstance();

		switch (cbPredefinedTypeSegmentation.getSelectedIndex()) {
		case 1:
			DefaultSegmentation defaultGland = new DefaultSegmentation(input);
			defaultGland.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
			imp_segmented = defaultGland.getOuputImp().duplicate();
			break;
		case 2:
			SegmentingNucleiGlands segGland = new SegmentingNucleiGlands(input);
			segGland.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
			imp_segmented = segGland.getOuputImp().duplicate();
			break;

		case 3:
			SegmZebrafish segZeb = new SegmZebrafish(input);
			segZeb.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
			imp_segmented = segZeb.getOuputImp().duplicate();
			break;
		}
		imp_segmented.show();
		cbPredefinedTypeSegmentation.setEnabled(true);
		
		return imp_segmented;
	}

	/**
	 * 
	 * @param imp
	 */
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