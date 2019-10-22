/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import AutomaticSegmentation.preProcessing.DefaultSegmentation;
import AutomaticSegmentation.preProcessing.SegmZebrafish;
import AutomaticSegmentation.preProcessing.SegmentingNucleiGlands;
import AutomaticSegmentation.preProcessing.ThresholdMethod;
import AutomaticSegmentation.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import inra.ijpb.geometry.Box3D;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.region3d.BoundingBox3D;
import inra.ijpb.measure.region3d.Centroid3D;
import net.haesleinhuepf.clij.CLIJ;
import net.miginfocom.swing.MigLayout;

/**
 * @author Pablo Vicente-Munuera, Pedro Gómez-Gálvez
 *
 */
public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * PreLimeSeg attributes
	 */
	private JComboBox<String> cbPredefinedTypeSegmentation;
	private JButton btPreLimeSeg;
	private JCheckBox jcbGPUEnable;
	private ProgressBar progressBar;
	private JComboBox<ThresholdMethod> cbThresholdMethod;
	private JButton btCreateROIs;
	private ImagePlus imp_segmented;
	/**
	 * 
	 */
	private LimeSegWindow limeSegWindow;
	private ImagePlus workingImp;
	private float d_0;
	private float f_pressure;
	private float range_D_0;
	private JButton btStopOptimisation;
	private JPanel Panel;
	private JButton btnSavePly;
	private JButton btRunSegmentation;
	/**
	 * 
	 */
	private PostProcessingWindow postprocessingWindow;
	
	/**
	 * 
	 */
	private JPanel imageChannelsPanel;
	private JPanel buttonsPanel;
	private JComboBox<String> cbNucleiChannel;
	private JComboBox<String> cbSegmentableChannel;
	private JLabel lbNucleiChannel;
	private JLabel lbSegmentableChannel;
	
	private JTabbedPane tabbedPane;	
	private JPanel tpPreLimeSeg;
	private JPanel tpLimeSeg;
	private JPanel tpPostLimeSeg;
	
	private ImagePlus originalImp;
	private ImagePlus nucleiChannel;
	private ImagePlus cellOutlineChannel;
	private JLabel lbNucleiFileName;
	private JButton btNucleiOpenFile;
	private JLabel lbCellOutlinesFileName;
	private JButton btCellOutlinesOpenFile;
	private JLabel lbOriginalImage;
	private JLabel lbOriginalFileName;
	private JButton btOpenOriginalImage;
	private JLabel lbEmptyLabel;

	/**
	 * @throws HeadlessException
	 */
	public MainWindow() throws HeadlessException {
		
		cellOutlineChannel = null;
		nucleiChannel = null;
		
		/*
		 * MAIN WINDOW
		 * DESCRIPTION
		 */
		
		// Init GUI elements
		getContentPane().setLayout(new GridLayout(2, 0, 0, 0));
		
		imageChannelsPanel = new JPanel(new GridLayout(3, 4));
		tabbedPane = new JTabbedPane();
		getContentPane().add(imageChannelsPanel);
		getContentPane().add(tabbedPane);
		tabbedPane.setEnabled(false);

		//Row 1: Original image
		lbOriginalImage = new JLabel("Original image");
		lbOriginalFileName = new JLabel("");
		btOpenOriginalImage = new JButton("Open");
		lbEmptyLabel = new JLabel("");
		
		imageChannelsPanel.add(lbOriginalImage);
		imageChannelsPanel.add(lbOriginalFileName);
		imageChannelsPanel.add(lbEmptyLabel);
		imageChannelsPanel.add(btOpenOriginalImage);
		
		//Row 2: Nuclei channel
		cbNucleiChannel = new JComboBox<String>();
		
		lbNucleiChannel = new JLabel("Nuclei channel");
		lbNucleiChannel.setLabelFor(cbNucleiChannel);
		
		
		lbNucleiFileName = new JLabel("");
		btNucleiOpenFile = new JButton("Open");

		imageChannelsPanel.add(lbNucleiChannel);
		imageChannelsPanel.add(cbNucleiChannel);
		imageChannelsPanel.add(lbNucleiFileName);
		imageChannelsPanel.add(btNucleiOpenFile);
		
		//Row 3: Cell outline channel
		cbSegmentableChannel = new JComboBox<String>();
		lbSegmentableChannel = new JLabel("Cell outline channel");
		lbSegmentableChannel.setLabelFor(cbSegmentableChannel);
		
		lbCellOutlinesFileName = new JLabel("");
		btCellOutlinesOpenFile = new JButton("Open");
		
		imageChannelsPanel.add(lbSegmentableChannel);
		imageChannelsPanel.add(cbSegmentableChannel);
		imageChannelsPanel.add(lbCellOutlinesFileName);
		imageChannelsPanel.add(btCellOutlinesOpenFile);
		
		/* PRELIMESEG PANEL
		 * DESCRIPTION
		 */
		
		tpPreLimeSeg = new JPanel();
		tpPreLimeSeg.setLayout(new MigLayout());
		imp_segmented = new ImagePlus();
		
		// Init GUI elements
		btPreLimeSeg = new JButton("Run!");
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
		cbThresholdMethod.setSelectedIndex(15);

		// Add components
		tpPreLimeSeg.add(btCreateROIs, "wrap");
		tpPreLimeSeg.add(cbThresholdMethod, "wrap");
		tpPreLimeSeg.add(cbPredefinedTypeSegmentation, "wrap");
		cbPredefinedTypeSegmentation.setSelectedIndex(0);
		tpPreLimeSeg.add(btPreLimeSeg, "wrap");
		tpPreLimeSeg.add(progressBar);

		// Associate this panel to the TabPanel
		tabbedPane.addTab("PreLimeSeg", tpPreLimeSeg);
		this.setEnablePanels(false,tpPreLimeSeg);	
		//tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

		
		
		/* LIMESEG PANEL
		 * DESCRIPTION
		 */

		tpLimeSeg = new JPanel(new GridLayout(3, 4));
		tpLimeSeg.setEnabled(false);
		tabbedPane.addTab("LimeSeg", tpLimeSeg);
		//tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		
		
		/* POSTLIMESEG PANEL
		 * DESCRIPTION
		 */

		tpPostLimeSeg = new JPanel();
		tabbedPane.addTab("PostLimeSeg", tpPostLimeSeg);
		//tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
		
		//Buttons panel
//		JButton btPreLimeSeg = new JButton("Preprocess to LimeSeg");
//		buttonsPanel.add(btPreLimeSeg);
//		
//		JButton btLimeSeg = new JButton("LimeSeg");
//		buttonsPanel.add(btLimeSeg);
//		
//		JButton btPostLimeSeg = new JButton("Postprocess LimeSeg's output");
//		buttonsPanel.add(btPostLimeSeg);
//		
		
		
		//Functions
//		btPreLimeSeg.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				preLimeSeg = new PreLimeSegWindow(nucleiChannel.getStack());
//				preLimeSeg.pack();
//				preLimeSeg.setVisible(true);
//			}
//		});
		
		btNucleiOpenFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					nucleiChannel = IJ.openImage();
					if (lbNucleiFileName.getText().length() <= 2) {
					lbNucleiFileName.setText(nucleiChannel.getOriginalFileInfo().fileName);
					cbNucleiChannel.addItem(nucleiChannel.getTitle());
					//cbNucleiChannel.setSelectedIndex(0);
					}
				} catch (Exception ex) {
					
				}
			}
		});
		
		btCellOutlinesOpenFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					cellOutlineChannel = IJ.openImage();
					if (lbCellOutlinesFileName.getText().length() <= 2) {
					lbCellOutlinesFileName.setText(cellOutlineChannel.getOriginalFileInfo().fileName);
					cbSegmentableChannel.addItem(cellOutlineChannel.getTitle());
					//cbSegmentableChannel.setSelectedIndex(0);
					}
				} catch (Exception ex) {
					
				}
			}
		});
		
		btOpenOriginalImage.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					originalImp = IJ.openImage();
					newOriginalFileName();
					tabbedPane.setEnabled(true);
				} catch (Exception ex) {
				}
			}
		});
		
		cbNucleiChannel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (((String) cbNucleiChannel.getSelectedItem()).equals("")){
					nucleiChannel = null;
				} else if ((boolean) ((String) cbNucleiChannel.getSelectedItem()).contains("Original file - C=")) {
					nucleiChannel = extractChannelOfStack(cbNucleiChannel.getSelectedIndex(), originalImp);
					//lbNucleiFileName.setText("");
					setEnablePanels(true, tpPreLimeSeg);
					btPreLimeSeg.setEnabled(false);
				} 
					
			}
		});
		
		cbSegmentableChannel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (((String) cbSegmentableChannel.getSelectedItem()).equals("")){
					cellOutlineChannel = null;
				} else if ((boolean) ((String) cbSegmentableChannel.getSelectedItem()).contains("Original file - C=")) {
					cellOutlineChannel = extractChannelOfStack(cbSegmentableChannel.getSelectedIndex(), originalImp);
					tpLimeSeg.setEnabled(true);
				}
			}
		});
		
//		btLimeSeg.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				originalImp.setC(cbSegmentableChannel.getSelectedIndex());
//				
//				//limeSegWindow = new LimeSegWindow(cellOutlineChannel.duplicate());
//				//send the original image to get the directory and save the cells
//				//cellOutline.duplicate() send a null directory
//				limeSegWindow = new LimeSegWindow(cellOutlineChannel); 	
//				limeSegWindow.pack();
//				limeSegWindow.setVisible(true);
//			}
//		});
//		
//		btPostLimeSeg.addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				//cellOutline will show in the postProcessingWindow
//				postprocessingWindow = new PostProcessingWindow(cellOutlineChannel);
//				postprocessingWindow.pack();
//				postprocessingWindow.setVisible(true);
//			}
//		});
		
		
		// FUNCTIONS PRELIMESEG
		
		cbPredefinedTypeSegmentation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (cbPredefinedTypeSegmentation.getSelectedIndex() == 0) {
					btPreLimeSeg.setEnabled(false);
				} else {
					btPreLimeSeg.setEnabled(true);
				}

			}
		});

		btPreLimeSeg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setEnablePanels(false,tpPreLimeSeg);	
				imp_segmented = null;
				
				CLIJ clij = null;
				jcbGPUEnable.setSelected(false);
				if (jcbGPUEnable.isSelected())
					clij = CLIJ.getInstance();

				switch (cbPredefinedTypeSegmentation.getSelectedIndex()) {
				case 1:
					DefaultSegmentation defaultGland = new DefaultSegmentation(nucleiChannel);
					defaultGland.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
					imp_segmented = defaultGland.getOuputImp().duplicate();
					break;
				case 2:
					SegmentingNucleiGlands segGland = new SegmentingNucleiGlands(nucleiChannel);
					segGland.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
					imp_segmented = segGland.getOuputImp().duplicate();
					break;

				case 3:
					SegmZebrafish segZeb = new SegmZebrafish(nucleiChannel);
					segZeb.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
					imp_segmented = segZeb.getOuputImp().duplicate();
					break;
				}
				imp_segmented.show();
				RoiManager rm = getNucleiROIs(imp_segmented);
				setEnablePanels(true,tpPreLimeSeg);
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


		
		
		
//		
//		this.addWindowListener(new WindowListener() {
//
//			@Override
//			public void windowClosing(WindowEvent e) {
//				// TODO Auto-generated method stub
//			}
//
//			@Override
//			public void windowActivated(WindowEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void windowClosed(WindowEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void windowDeactivated(WindowEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void windowDeiconified(WindowEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void windowIconified(WindowEvent e) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void windowOpened(WindowEvent e) {
//				// TODO Auto-generated method stub
//				
//				int response;
//				
//				while (originalImp == null) {
//					response = JOptionPane.showConfirmDialog(getParent(), "Do you want to use the open stack or another one?", "Confirm",
//					        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
//					
//					if (response == JOptionPane.NO_OPTION) {
//						IJ.open();
//						try {
//							originalImp = IJ.getImage();
//						} catch (Exception ex) {
//							// TODO: handle exception
//						}
//					} else if (response == JOptionPane.YES_OPTION) {
//						try {
//							originalImp = IJ.getImage();
//						} catch (Exception ex) {
//							// TODO: handle exception
//						}
//					}
//				}
//				
//				newOriginalFileName();
//				
//				originalImp.show();
//			}
//		});
	}



	/**
	 * 
	 */
	private void newOriginalFileName() {
		cbNucleiChannel.removeAllItems();
		cbSegmentableChannel.removeAllItems();
		lbOriginalFileName.setText(originalImp.getOriginalFileInfo().fileName);

		cbNucleiChannel.addItem("");
		cbSegmentableChannel.addItem("");

		for (int numChannel = 0; numChannel < originalImp.getNChannels(); numChannel++) {
			cbNucleiChannel.addItem("Original file - C=" + numChannel);
			cbSegmentableChannel.addItem("Original file - C=" + numChannel);
		}
	}

	/**
	 * @param numChannel
	 */
	public ImagePlus extractChannelOfStack(int numChannel, ImagePlus originalImage) {
		ImageStack newChannelStack = new ImageStack(originalImage.getWidth(), originalImage.getHeight());
		
		int indexToAdd = 0;
		for (int numZ = 0; numZ < originalImage.getStackSize()/originalImage.getNChannels(); numZ++) {
			indexToAdd = originalImage.getStackIndex(numChannel, numZ, originalImage.getFrame());
			newChannelStack.addSlice(originalImage.getStack().getProcessor(indexToAdd));
		}
		ImagePlus oneChannelStack = new ImagePlus("", newChannelStack); 
		oneChannelStack.setFileInfo(originalImage.getFileInfo());
		return oneChannelStack;
	}



	/**
	 * @param gc
	 */
	public MainWindow(GraphicsConfiguration gc) {
		super(gc);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public MainWindow(String title) throws HeadlessException {
		super(title);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @param gc
	 */
	public MainWindow(String title, GraphicsConfiguration gc) {
		super(title, gc);
		// TODO Auto-generated constructor stub
	}
	
	// METHODS PRELIMESEG
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
	
	}
	
	// GENERIC METHODS
	
	protected void setEnablePanels(boolean enabled, JPanel panel) {
		for (Component c : panel.getComponents()) {
			c.setEnabled(enabled);
		}
	}
		
}
