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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.Filters3D;
import ij.plugin.Resizer;
import ij.plugin.frame.RoiManager;

import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.geometry.Ellipsoid;
import inra.ijpb.morphology.*;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import inra.ijpb.watershed.Watershed;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.kernels.Kernels;
import net.miginfocom.swing.MigLayout;

import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.*;
import inra.ijpb.measure.region3d.Centroid3D;
import inra.ijpb.measure.region3d.InertiaEllipsoid;
import ij3d.Image3DUniverse;
import ij3d.ContentConstants;

public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel panel;
	private JButton OpenButton;
	private JComboBox<String> ComboBox;

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
		panel = new JPanel();
		panel.setLayout(new MigLayout());

		// Create 'open' button
		OpenButton = new JButton("Open new image");

		// Create 'Select Current Image' button
		CurrentImageButton = new JButton("Select current image");

		// Create ProgressBar
		progressBar = new ProgressBar(100, 25);

		// Create ComboBox
		ComboBox = new JComboBox<String>();
		ComboBox.addItem("Select a type of DAPI segmentation");
		ComboBox.addItem("Salivary glands (cylinder monolayer)");
		ComboBox.addItem("Zebrafish multilayer");

		// Add components
		panel.add(OpenButton, "wrap");
		panel.add(CurrentImageButton, "wrap");
		panel.add(ComboBox, "wrap");
		ComboBox.setSelectedIndex(0);
		panel.add(progressBar);

		CurrentImageButton.setEnabled(false);
		OpenButton.setEnabled(false);
		// Associate this panel to the window
		getContentPane().add(panel);

		ComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (ComboBox.getSelectedIndex() == 0) {
					CurrentImageButton.setEnabled(false);
					OpenButton.setEnabled(false);
				} else {
					CurrentImageButton.setEnabled(true);
					OpenButton.setEnabled(true);
				}

			}
		});
		OpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				ImagePlus imp = new ImagePlus();
				ImagePlus input = new ImagePlus();
				ImagePlus imp_segmented = new ImagePlus();
				RoiManager rm = null;

				switch (ComboBox.getSelectedIndex()) {
				case 1:
					ComboBox.setEnabled(false);
					CurrentImageButton.setEnabled(false);
					OpenButton.setEnabled(false);
					// Open the image (ADD any exception)
					imp = IJ.openImage();
					/*
					 * WindowManager.addWindow(imp.getWindow()); imp.show();
					 */
					// imp.show();
					input = imp.duplicate();
					SegmentingNucleiGlands segGland = new SegmentingNucleiGlands(input);
					imp_segmented = segGland.getOuputImp().duplicate();
					rm = getNucleiROIs(segGland.getOuputImp());
					imp_segmented.show();
					break;

				case 2:
					ComboBox.setEnabled(false);
					CurrentImageButton.setEnabled(false);
					OpenButton.setEnabled(false);
					// Open the image (ADD any exception)
					imp = IJ.openImage();
					/*
					 * WindowManager.addWindow(imp.getWindow()); imp.show();
					 */
					imp.show();
					input = imp.duplicate();
					SegmZebrafish segZeb = new SegmZebrafish(input);
					imp_segmented = segZeb.getOuputImp();
					rm = getNucleiROIs(imp_segmented);
					imp_segmented.show();
					break;

				default:
					break;
				}
				ComboBox.setEnabled(true);

				// visualization3D (imp_segmented);

			}
		});

		CurrentImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImagePlus imp = new ImagePlus();
				ImagePlus input = new ImagePlus();
				ImagePlus imp_segmented = new ImagePlus();
				RoiManager rm = null;

				switch (ComboBox.getSelectedIndex()) {
				case 1:
					ComboBox.setEnabled(false);
					CurrentImageButton.setEnabled(false);
					OpenButton.setEnabled(false);
					// Get image from the workspace (ADD any exception)
					imp = IJ.getImage();
					input = imp.duplicate();

					SegmentingNucleiGlands segGland = new SegmentingNucleiGlands(input);
					imp_segmented = segGland.getOuputImp();

					rm = getNucleiROIs(imp_segmented);
					imp_segmented.show();
					break;

				case 2:
					ComboBox.setEnabled(false);
					CurrentImageButton.setEnabled(false);
					OpenButton.setEnabled(false);
					// Get image from the workspace (ADD any exception)
					imp = IJ.getImage();
					input = imp.duplicate();
					SegmZebrafish segZeb = new SegmZebrafish(input);
					imp_segmented = segZeb.getOuputImp();
					rm = getNucleiROIs(imp_segmented);
					imp_segmented.show();
					break;

				default:
					break;
				}
				ComboBox.setEnabled(true);

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