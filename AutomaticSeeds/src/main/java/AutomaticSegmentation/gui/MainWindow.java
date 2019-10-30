/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import AutomaticSegmentation.limeSeg.SphereSegAdapted;
import AutomaticSegmentation.preProcessing.DefaultSegmentation;
import AutomaticSegmentation.preProcessing.SegmBigAndOverlappedNuclei;
import AutomaticSegmentation.preProcessing.SegmZebrafish;
import AutomaticSegmentation.preProcessing.SegmentingNucleiGlands;
import AutomaticSegmentation.preProcessing.ThresholdMethod;
import AutomaticSegmentation.utils.Utils;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.commands.ClearAll;
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
	private JLabel lbThresholdMethod;
	private JPanel ThresholdMethodPanel;
	private JButton btShowNuclei;
	private JButton btThresholdMethod;
	private ImagePlus imp_segmented;

	/**
	 * LimeSeg attributes
	 */
	private JButton btStopOptimisation;
	private JButton btnSavePly;
	private JButton btLimeSeg;
	private JButton btRoiManager;
	private JButton btShowOutlines;
	private JSpinner js_D0;
	private JSpinner js_fPressure;
	private JSpinner js_zScale;
	private JSpinner js_rangeD0;
	private JLabel label_D0;
	private JLabel label_fPressure;
	private JLabel label_zScale;
	private JLabel label_rangeD0;
	private SphereSegAdapted cf;
	/**
	 * PostLimeSeg attributes
	 */
	private PostProcessingWindow postprocessingWindow;
	private JButton btPostLimeSeg;

	/**
	 * MainWindow attributes
	 */
	private JPanel mainPanel;
	private JComboBox<String> cbNucleiChannel;
	private JComboBox<String> cbSegmentableChannel;
	private JLabel lbNucleiChannel;
	private JLabel lbSegmentableChannel;

	private JTabbedPane tabbedPane;
	private JPanel tpPreLimeSeg;
	private JPanel tpLimeSeg;
	private JPanel tpPostLimeSeg;

	private ArrayList<ImagePlus> ImpArraylist;
	private ImagePlus originalImp;
	private ImagePlus nucleiChannel;
	private ImagePlus cellOutlineChannel;
	private JButton btOpenOriginalImage;
	private JButton btRemoveItems;

	/**
	 * @throws HeadlessException
	 */
	public MainWindow() throws HeadlessException {

		cellOutlineChannel = null;
		nucleiChannel = null;
		ImpArraylist = new ArrayList<ImagePlus>();
		ImpArraylist.add(null);

		// Init GUI elements
		getContentPane().setLayout(new MigLayout());

		mainPanel = new JPanel(new MigLayout());
		tabbedPane = new JTabbedPane();
		tabbedPane.setMinimumSize((new Dimension(500, 250)));
		getContentPane().add(mainPanel, "wrap");
		getContentPane().add(tabbedPane);
		tabbedPane.setEnabled(false);

		initMainPanel();

		initPreLimeSegPanel();

		initLimeSegPanel();

		initPostLimeSegPanel();

		/*-------------------- MAIN WINDOW FUNCTIONS ----------------------*/

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

		btRemoveItems.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cbNucleiChannel.removeAllItems();
				cbNucleiChannel.addItem("");
				cbSegmentableChannel.removeAllItems();
				cbSegmentableChannel.addItem("");
				ImpArraylist.removeAll(ImpArraylist);
				ImpArraylist.add(null);
				setEnablePanels(false, tpPreLimeSeg);
				setEnablePanels(false, tpLimeSeg);
				setEnablePanels(false, tpPostLimeSeg);
			}
		});

		cbNucleiChannel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (cbNucleiChannel.getSelectedItem() == "" | cbNucleiChannel.getSelectedIndex() == -1) {
					nucleiChannel = null;
					setEnablePanels(false, tpPreLimeSeg);
					setEnablePanels(false, ThresholdMethodPanel);
				} else {
					nucleiChannel = ImpArraylist.get(cbNucleiChannel.getSelectedIndex());
					setEnablePanels(true, tpPreLimeSeg);
					setEnablePanels(true, ThresholdMethodPanel);
				}
			}
		});

		cbSegmentableChannel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (cbSegmentableChannel.getSelectedItem() == "" | cbSegmentableChannel.getSelectedIndex() == -1) {
					cellOutlineChannel = null;
					setEnablePanels(false, tpPostLimeSeg);
					setEnablePanels(false, tpLimeSeg);
				} else {
					cellOutlineChannel = ImpArraylist.get(cbSegmentableChannel.getSelectedIndex());
					setEnablePanels(true, tpPostLimeSeg);
					setEnablePanels(true, tpLimeSeg);
					js_zScale.setValue((float) cellOutlineChannel.getOriginalFileInfo().pixelDepth
							/ cellOutlineChannel.getOriginalFileInfo().pixelWidth);

				}
			}
		});

		/*
		 * ------------------------- PRELIMESEG --------------------------------
		 */

		cbPredefinedTypeSegmentation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (cbPredefinedTypeSegmentation.getSelectedIndex() == 0) {
					btPreLimeSeg.setEnabled(false);
				} else {
					btPreLimeSeg.setEnabled(true);
				}

			}
		});

		btPreLimeSeg.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				ExecutorService executor1 = Executors.newSingleThreadExecutor();
				executor1.submit(() -> {
					btPreLimeSeg.setEnabled(false);
					imp_segmented = null;
					CLIJ clij = null;
					jcbGPUEnable.setSelected(false);
					if (jcbGPUEnable.isSelected())
						clij = CLIJ.getInstance();

					nucleiChannel = RunThreshold();

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
					case 4:
						SegmBigAndOverlappedNuclei segBigOverNuc = new SegmBigAndOverlappedNuclei(nucleiChannel);
						segBigOverNuc.segmentationProtocol(clij, cbThresholdMethod.getSelectedItem().toString());
						imp_segmented = segBigOverNuc.getOuputImp().duplicate();
						break;
					}
					imp_segmented.show();
					RoiManager rm = getNucleiROIs(imp_segmented);
					btPreLimeSeg.setEnabled(true);
					executor1.shutdown();
				});

				// visualization3D (imp_segmented);
			}

		});

		btShowNuclei.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (nucleiChannel != null) {
					nucleiChannel.duplicate().show();
				}
			}
		});

		btThresholdMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				nucleiChannel.duplicate().show();
				IJ.run("Threshold...");

			}
		});

		btThresholdMethod.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent arg0) {
				btThresholdMethod.setForeground((Color.BLUE));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				btThresholdMethod.setForeground((Color.BLACK));
			}
		});

		/* --------------------- LIMESEG FUNCTIONS ------------------------- */

		btnSavePly.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String path = cellOutlineChannel.getOriginalFileInfo().directory + "/OutputLimeSeg";
				File dir = new File(path);
				if (!dir.isDirectory()) {
					System.out.println("New folder created");
					dir.mkdir();
				}
				LimeSeg.saveStateToXmlPly(path);
				System.out.println("Saved");
			}
		});

		btStopOptimisation.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				LimeSeg.stopOptimisation();
				cf.setClearOptimizer(true);
			}
		});

		btRoiManager.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				RoiManager.getRoiManager();
			}
		});

		btShowOutlines.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (cellOutlineChannel != null) {
					cellOutlineChannel.duplicate().show();
				}
			}
		});

		btLimeSeg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ExecutorService executor1 = Executors.newSingleThreadExecutor();
				executor1.submit(() -> {
					btLimeSeg.setEnabled(false);
					ClearAll clear = new ClearAll();
					cf.setImp(cellOutlineChannel);
					cf.setZ_scale(Double.valueOf(js_zScale.getValue().toString()).floatValue());
					cf.setD_0(Double.valueOf(js_D0.getValue().toString()).floatValue());
					cf.setF_pressure(Double.valueOf(js_fPressure.getValue().toString()).floatValue());
					cf.setRange_in_d0_units(Double.valueOf(js_rangeD0.getValue().toString()).floatValue());

					RoiManager roiManager = RoiManager.getRoiManager();
					if (roiManager.getRoisAsArray().length == 0) {
						roiManager.runCommand("Open", cellOutlineChannel.getOriginalFileInfo().directory);
					}
					if (roiManager.getRoisAsArray().length != 0) {
						clear.run();
						cf.run();
					} else {
						IJ.log("Error. Any Roi set selected");

					}

					btLimeSeg.setEnabled(true);
					executor1.shutdown();
				});
			}
		});

		// POSTLIMESEG FUNCTIONS

		btPostLimeSeg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				btPostLimeSeg.setEnabled(false);
				// cellOutline will show in the postProcessingWindow
				postprocessingWindow = new PostProcessingWindow(cellOutlineChannel);
				postprocessingWindow.pack();
				postprocessingWindow.setVisible(true);
				btPostLimeSeg.setEnabled(true);
			}
		});

	}

	/**
	 * 
	 */
	private void initPostLimeSegPanel() {
		tpPostLimeSeg = new JPanel(new MigLayout("fill"));
		btPostLimeSeg = new JButton("Run PostProcessing");
		tpPostLimeSeg.add(btPostLimeSeg, "align center");

		// Add to the tab
		tabbedPane.addTab("PostLimeSeg", tpPostLimeSeg);
		this.setEnablePanels(false, tpPostLimeSeg);
	}

	/** -------------------------- INIT GUI ELEMENTS ---------------------- **/

	/**
	 * 
	 */
	private void initPreLimeSegPanel() {
		tpPreLimeSeg = new JPanel();
		tpPreLimeSeg.setLayout(new MigLayout("fill"));
		imp_segmented = new ImagePlus();

		// Init GUI elements
		btPreLimeSeg = new JButton("Run!");
		btShowNuclei = new JButton("Show Nuclei");
		jcbGPUEnable = new JCheckBox("Enable GPU operations");
		jcbGPUEnable.setSelected(true);
		progressBar = new ProgressBar(100, 25);

		cbPredefinedTypeSegmentation = new JComboBox<String>();
		cbPredefinedTypeSegmentation.addItem("Select a type of DAPI segmentation");
		cbPredefinedTypeSegmentation.addItem("Default");
		cbPredefinedTypeSegmentation.addItem("Salivary glands (cylinder monolayer)");
		cbPredefinedTypeSegmentation.addItem("Zebrafish multilayer");
		cbPredefinedTypeSegmentation.addItem("Big and overlapped nuclei");

		cbThresholdMethod = new JComboBox<ThresholdMethod>(ThresholdMethod.values());
		cbThresholdMethod.setSelectedIndex(15);
		lbThresholdMethod = new JLabel("Threshold method:");
		ThresholdMethodPanel = new JPanel();
		btThresholdMethod = new JButton("Info about Threshold methods");
		btThresholdMethod.setBorderPainted(false);
		Map attributes = btThresholdMethod.getFont().getAttributes();
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		btThresholdMethod.setFont(btThresholdMethod.getFont().deriveFont(attributes));

		// Add components
		ThresholdMethodPanel.add(lbThresholdMethod);
		ThresholdMethodPanel.add(cbThresholdMethod);

		tpPreLimeSeg.add(btShowNuclei, "wrap");
		tpPreLimeSeg.add(ThresholdMethodPanel);
		tpPreLimeSeg.add(btThresholdMethod, "wrap, align left");
		tpPreLimeSeg.add(cbPredefinedTypeSegmentation, "wrap");
		cbPredefinedTypeSegmentation.setSelectedIndex(0);
		tpPreLimeSeg.add(btPreLimeSeg, "wrap");
		tpPreLimeSeg.add(progressBar, "align center");

		// Associate this panel to the TabPanel
		tabbedPane.addTab("PreLimeSeg", tpPreLimeSeg);
		this.setEnablePanels(false, tpPreLimeSeg);
		this.setEnablePanels(false, ThresholdMethodPanel);
		// tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
	}

	/**
	 * 
	 */
	private void initMainPanel() {
		// Row 1: Original image
		btRemoveItems = new JButton("Clear All");
		btOpenOriginalImage = new JButton("Open Stack");

		mainPanel.add(btRemoveItems);
		mainPanel.add(btOpenOriginalImage, "wrap");

		// Row 2: Nuclei channel
		cbNucleiChannel = new JComboBox<String>();
		cbNucleiChannel.setMinimumSize(new Dimension(100, 10));
		cbNucleiChannel.addItem("");
		lbNucleiChannel = new JLabel("Nuclei channel");
		lbNucleiChannel.setLabelFor(cbNucleiChannel);

		mainPanel.add(lbNucleiChannel);
		mainPanel.add(cbNucleiChannel, "wrap");

		// Row 3: Cell outline channel
		cbSegmentableChannel = new JComboBox<String>();
		cbSegmentableChannel.setMinimumSize(new Dimension(100, 10));
		cbSegmentableChannel.addItem("");
		lbSegmentableChannel = new JLabel("Cell outline channel");
		lbSegmentableChannel.setLabelFor(cbSegmentableChannel);

		mainPanel.add(lbSegmentableChannel);
		mainPanel.add(cbSegmentableChannel, "wrap");
	}

	/**
	 * 
	 */
	private void initLimeSegPanel() {
		tpLimeSeg = new JPanel();
		tpLimeSeg.setLayout(new MigLayout("fill"));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Init GUI
		cf = new SphereSegAdapted();
		label_D0 = new JLabel("D_0:");
		js_D0 = new JSpinner(new SpinnerNumberModel(5.5, null, null, 0.1));

		js_D0.setMinimumSize(new Dimension(100, 10));
		tpLimeSeg.add(label_D0, "align center");
		tpLimeSeg.add(js_D0, "wrap, align center");

		label_fPressure = new JLabel("F_Pressure:");
		js_fPressure = new JSpinner(new SpinnerNumberModel(0.015, -0.04, 0.04, 0.001));
		js_fPressure.setMinimumSize(new Dimension(100, 10));
		tpLimeSeg.add(label_fPressure, "align center");
		tpLimeSeg.add(js_fPressure, "wrap, align center");

		label_zScale = new JLabel("Z scale:");
		js_zScale = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.1));
		js_zScale.setMinimumSize(new Dimension(100, 10));
		tpLimeSeg.add(label_zScale, "align center");
		tpLimeSeg.add(js_zScale, "wrap, align center");

		label_rangeD0 = new JLabel("Range in D0 units:");
		js_rangeD0 = new JSpinner(new SpinnerNumberModel(2, null, null, 1));
		js_rangeD0.setMinimumSize(new Dimension(100, 10));
		tpLimeSeg.add(label_rangeD0, "align center");
		tpLimeSeg.add(js_rangeD0, "wrap, align center");

		btRoiManager = new JButton("Open Roi Manager");
		tpLimeSeg.add(btRoiManager, "align center");

		btLimeSeg = new JButton("Start");
		tpLimeSeg.add(btLimeSeg, "wrap, align center");

		btShowOutlines = new JButton("Show Stack");
		tpLimeSeg.add(btShowOutlines, "align center");

		btStopOptimisation = new JButton("Stop");
		tpLimeSeg.add(btStopOptimisation, "wrap, align center");

		btnSavePly = new JButton("Saved");
		tpLimeSeg.add(new JLabel(""));
		tpLimeSeg.add(btnSavePly, "align center");

		tabbedPane.addTab("LimeSeg", tpLimeSeg);
		this.setEnablePanels(false, tpLimeSeg);
	}

	/** ------------ END INIT GUI ELEMENTS ------------------------ **/

	public synchronized ImagePlus RunThreshold() {
		nucleiChannel.duplicate().show();
		IJ.run("Threshold...");
		while (IJ.getImage().getProcessor().isBinary() != true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return IJ.getImage().duplicate();
	}

	// GENERIC METHODS

	protected void setEnablePanels(boolean enabled, JPanel panel) {
		for (Component c : panel.getComponents()) {
			c.setEnabled(enabled);
		}
	}

	/**
	 * 
	 */
	private void newOriginalFileName() {
		if (originalImp.getNChannels() > 1) {
			for (int numChannel = 0; numChannel < originalImp.getNChannels(); numChannel++) {
				cbNucleiChannel.addItem("Original file - C=" + numChannel);
				cbSegmentableChannel.addItem("Original file - C=" + numChannel);
				ImpArraylist.add(extractChannelOfStack(numChannel + 1, originalImp));
			}
		} else {
			cbNucleiChannel.addItem(originalImp.getTitle());
			cbSegmentableChannel.addItem(originalImp.getTitle());
			ImpArraylist.add(extractChannelOfStack(1, originalImp));
		}
	}

	/**
	 * @param numChannel
	 */
	public ImagePlus extractChannelOfStack(int numChannel, ImagePlus originalImage) {
		ImageStack newChannelStack = new ImageStack(originalImage.getWidth(), originalImage.getHeight());
		int indexToAdd = 0;
		for (int numZ = 0; numZ < originalImage.getStackSize() / originalImage.getNChannels(); numZ++) {
			indexToAdd = originalImage.getStackIndex(numChannel, numZ, originalImage.getFrame());
			newChannelStack.addSlice(originalImage.getStack().getProcessor(indexToAdd));
			// newChannelStack.addSlice(originalImage.getStack().getProcessor(numZ));
		}
		ImagePlus oneChannelStack = new ImagePlus("", newChannelStack);
		oneChannelStack.setFileInfo(originalImage.getFileInfo());
		// oneChannelStack.duplicate().show();
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

		// Ellipsoid[] ellipsoid =
		// EquivalentEllipsoid.equivalentEllipsoids(imp_segmented.getImageStack(),
		// labels,
		// imp_segmented.getCalibration());

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
			double[] radii = { bboxes[i].height(), bboxes[i].width() };
			double[] calibrations = { imp_segmented.getCalibration().pixelHeight,
					imp_segmented.getCalibration().pixelWidth };
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

}
