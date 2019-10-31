/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import AutomaticSegmentation.limeSeg.SphereSegAdapted;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.commands.ClearAll;
import eu.kiaru.limeseg.struct.Cell;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.frame.RoiManager;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
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
	public static double THRESHOLD = 5;
	


	private ArrayList<ImagePlus> ImpArraylist;
	private ImagePlus originalImp;
	private ImagePlus nucleiChannel;
	private ImagePlus cellOutlineChannel;

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
	 * MainWindow attributes
	 */
	private JPanel mainPanel;
	private JComboBox<String> cbNucleiChannel;
	private JComboBox<String> cbSegmentableChannel;
	private JLabel lbNucleiChannel;
	private JLabel lbSegmentableChannel;

	private JTabbedPane tabbedPane;
	private PanelPreProcessing tpPreLimeSeg;
	private JPanel tpLimeSeg;
	private PanelPostProcessing tpPostLimeSeg;
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
		

		tpPreLimeSeg = new PanelPreProcessing(new MigLayout("fill"));
		tabbedPane.addTab("PreLimeSeg", tpPreLimeSeg);
		this.setEnablePanels(false, tpPreLimeSeg);
		this.setEnablePanels(false, tpPreLimeSeg.getThresholdMethodPanel());

		initLimeSegPanel();
		
		tpPostLimeSeg = new PanelPostProcessing(new MigLayout("fill"));
		tabbedPane.addTab("PostLimeSeg", tpPostLimeSeg);
		
		this.setEnablePanels(false, tpPostLimeSeg);


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
					tpPreLimeSeg.setNucleiChannel(null);
					setEnablePanels(false, tpPreLimeSeg);
					setEnablePanels(false, tpPreLimeSeg.getThresholdMethodPanel());
				} else {
					nucleiChannel = ImpArraylist.get(cbNucleiChannel.getSelectedIndex());
					tpPreLimeSeg.setNucleiChannel(nucleiChannel);
					setEnablePanels(true, tpPreLimeSeg);
					setEnablePanels(true, tpPreLimeSeg.getThresholdMethodPanel());
				}
			}
		});

		cbSegmentableChannel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (cbSegmentableChannel.getSelectedItem() == "" | cbSegmentableChannel.getSelectedIndex() == -1) {
					cellOutlineChannel = null;
					tpPostLimeSeg.setCellOutlineChannel(null);
					setEnablePanels(false, tpPostLimeSeg);
					setEnablePanels(false, tpLimeSeg);
				} else {
					cellOutlineChannel = ImpArraylist.get(cbSegmentableChannel.getSelectedIndex());
					tpPostLimeSeg.setCellOutlineChannel(cellOutlineChannel);
					setEnablePanels(true, tpPostLimeSeg);
					setEnablePanels(true, tpLimeSeg);
					js_zScale.setValue((float) cellOutlineChannel.getOriginalFileInfo().pixelDepth
							/ cellOutlineChannel.getOriginalFileInfo().pixelWidth);
					

				}
			}
		});

		

		/* --------------------- LIMESEG FUNCTIONS ------------------------- */

		btnSavePly.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<Cell> cell = LimeSeg.allCells;
				if (cell != null) {
					String path = cellOutlineChannel.getOriginalFileInfo().directory + "OutputLimeSeg";
					File dir = new File(path);
					if (!dir.isDirectory()) {
						System.out.println("New folder created");
						dir.mkdir();
					}

					if (dir.listFiles().length != 0) {
						// Show dialog to confirm
						int dialogResult = JOptionPane.showConfirmDialog(null,
								"Saving will remove the content of the select folder, confirm?", "Warning",
								JOptionPane.YES_NO_OPTION);
						if (dialogResult == JOptionPane.YES_OPTION) {
							purgeDirectory(dir, 1);
							LimeSeg.saveStateToXmlPly(path);
						}
					} else {
						LimeSeg.saveStateToXmlPly(path);
					}

				} else {
					IJ.log("Any cell segmented");
				}
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
						roiManager.runCommand("Open", "");
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
	}

	/** -------------------------- INIT GUI ELEMENTS ---------------------- **/

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

		btnSavePly = new JButton("Save");
		tpLimeSeg.add(new JLabel(""));
		tpLimeSeg.add(btnSavePly, "align center");

		tabbedPane.addTab("LimeSeg", tpLimeSeg);
		this.setEnablePanels(false, tpLimeSeg);
	}

	/** ------------ END INIT GUI ELEMENTS ------------------------ **/

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
		oneChannelStack.setFileInfo(originalImage.getOriginalFileInfo());
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

	public void purgeDirectory(File dir, int height) {
		// no need to clean below level
		if (height >= 0) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory())
					purgeDirectory(file, height - 1);
				file.delete();
			}
		}
	}
}
