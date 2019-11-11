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
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
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
	 * MainWindow attributes
	 */
	private JPanel mainPanel;
	private JComboBox<String> cbNucleiChannel;
	private JComboBox<String> cbSegmentableChannel;
	private JLabel lbNucleiChannel;
	private JLabel lbSegmentableChannel;

	private JTabbedPane tabbedPane;
	private PanelPreProcessing tpPreLimeSeg;
	private PanelLimeSeg tpLimeSeg;
	private PanelPostProcessing tpPostLimeSeg;
	private JButton btOpenOriginalImage;
	private JButton btRemoveItems;
	private ExecutorService exec;
	
	private ActionListener listener = new ActionListener() {				
		public void actionPerformed(final ActionEvent e) {
			
			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {
												
				@SuppressWarnings("static-access")
				public void run(){
					if(e.getSource() == btOpenOriginalImage){
						try {
							// We just can't open multiple images
							originalImp = IJ.openImage();
							newOriginalFileName();
							tabbedPane.setEnabled(true);
						} catch (Exception ex) {
							
						}
					}
					else if(e.getSource() == btRemoveItems){
						cbNucleiChannel.removeAllItems();
						cbNucleiChannel.addItem("<select image>");
						cbSegmentableChannel.removeAllItems();
						cbSegmentableChannel.addItem("<select image>");
						tpPreLimeSeg.cbSegmentedImg.removeAllItems();
						tpPreLimeSeg.cbSegmentedImg.addItem("<select image>");
						ImpArraylist.removeAll(ImpArraylist);
						tpPreLimeSeg.ImpArraylistSegImg.removeAll(tpPreLimeSeg.ImpArraylistSegImg);
						ImpArraylist.add(null);
						setEnablePanels(false, tpPreLimeSeg);
						tpPreLimeSeg.btLoad.setEnabled(true);
						tpPreLimeSeg.cbSegmentedImg.setEnabled(true);
						setEnablePanels(false, tpLimeSeg);
						setEnablePanels(false, tpPostLimeSeg);
						
					}
					else if(e.getSource() == cbNucleiChannel){
						if (cbNucleiChannel.getSelectedItem() == "<select image>" | cbNucleiChannel.getSelectedIndex() == -1) {
							//nucleiChannel = null;
							//tpPreLimeSeg.setNucleiChannel(null);
							boolean isEnab = tpPreLimeSeg.btCalculateROIs.isEnabled();
							setEnablePanels(false, tpPreLimeSeg);
							tpPreLimeSeg.btLoad.setEnabled(true);
							tpPreLimeSeg.cbSegmentedImg.setEnabled(true);
							tpPreLimeSeg.btCalculateROIs.setEnabled(isEnab);
						} else {
							nucleiChannel = duplicateImp(ImpArraylist.get(cbNucleiChannel.getSelectedIndex()));
							tpPreLimeSeg.setNucleiChannel(nucleiChannel.duplicate());
							setEnablePanels(true, tpPreLimeSeg);
						}
					}
					else if(e.getSource() == cbSegmentableChannel){
						if (cbSegmentableChannel.getSelectedItem() == "<select image>"| cbSegmentableChannel.getSelectedIndex() == -1) {
							cellOutlineChannel = null;
							tpPostLimeSeg.setCellOutlineChannel(null);
							tpLimeSeg.setCellOutlineChannel(null);
							setEnablePanels(false, tpPostLimeSeg);
							setEnablePanels(false, tpLimeSeg);
						} else {
							cellOutlineChannel = duplicateImp(ImpArraylist.get(cbSegmentableChannel.getSelectedIndex()));
							tpPostLimeSeg.setCellOutlineChannel(cellOutlineChannel);
							tpLimeSeg.setCellOutlineChannel(cellOutlineChannel);
							tpPostLimeSeg.btPostLimeSeg.setEnabled(true);
							setEnablePanels(true, tpLimeSeg);
							tpLimeSeg.setZScale((float) cellOutlineChannel.getOriginalFileInfo().pixelDepth
									/ cellOutlineChannel.getOriginalFileInfo().pixelWidth);
							
							cellOutlineChannel.addImageListener(new ImageListener() {
								@Override
								public void imageUpdated(ImagePlus imp) {
									// TODO Auto-generated method stub
									if (tabbedPane.getSelectedIndex() == 2) {
										tpPostLimeSeg.updateOverlay();
									}
								}

								@Override
								public void imageOpened(ImagePlus imp) {
									// TODO Auto-generated method stub

								}

								@Override
								public void imageClosed(ImagePlus imp) {
									if (tabbedPane.getSelectedIndex() == 2) {
										// TODO Auto-generated method stub
										setEnablePanels(false, tpPostLimeSeg);
										tpPostLimeSeg.btPostLimeSeg.setEnabled(true);
										tpPostLimeSeg.clear3dCells();
										tpPostLimeSeg.setCellOutlineChannel(duplicateImp(ImpArraylist.get(cbSegmentableChannel.getSelectedIndex())));
									}
						}
							});

						}
					}
				}			
			});
		}
	};
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
		exec = Executors.newFixedThreadPool(1);

		initMainPanel();
	
	}

	/**
	 * 
	 */
	private void initMainPanel() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// Row 1: Original image
		btRemoveItems = new JButton("Clear All");
		btOpenOriginalImage = new JButton("Open Stack");
		
		mainPanel.add(btRemoveItems);
		mainPanel.add(btOpenOriginalImage, "wrap");

		// Row 2: Nuclei channel
		cbNucleiChannel = new JComboBox<String>();
		cbNucleiChannel.setMinimumSize(new Dimension(100, 10));
		cbNucleiChannel.addItem("<select image>");
		

		lbNucleiChannel = new JLabel("Nuclei channel");
		lbNucleiChannel.setLabelFor(cbNucleiChannel);

		mainPanel.add(lbNucleiChannel);
		mainPanel.add(cbNucleiChannel, "wrap");

		// Row 3: Cell outline channel
		cbSegmentableChannel = new JComboBox<String>();
		cbSegmentableChannel.setMinimumSize(new Dimension(100, 10));
		cbSegmentableChannel.addItem("<select image>");	
		lbSegmentableChannel = new JLabel("Cell outline channel");
		lbSegmentableChannel.setLabelFor(cbSegmentableChannel);

		mainPanel.add(lbSegmentableChannel);
		mainPanel.add(cbSegmentableChannel, "wrap");

		tpPreLimeSeg = new PanelPreProcessing(new MigLayout("fill"));
		tabbedPane.addTab("PreLimeSeg", tpPreLimeSeg);
		this.setEnablePanels(false, tpPreLimeSeg);
		tpPreLimeSeg.btLoad.setEnabled(true);
		tpPreLimeSeg.cbSegmentedImg.setEnabled(true);

		tpLimeSeg = new PanelLimeSeg(new MigLayout("fill"));
		tabbedPane.addTab("LimeSeg", tpLimeSeg);
		this.setEnablePanels(false, tpLimeSeg);

		tpPostLimeSeg = new PanelPostProcessing(new MigLayout("fill"));
		tabbedPane.addTab("PostLimeSeg", tpPostLimeSeg);
		this.setEnablePanels(false, tpPostLimeSeg);
		
		
		cbSegmentableChannel.addActionListener(listener);
		btOpenOriginalImage.addActionListener(listener);
		cbNucleiChannel.addActionListener(listener);
		btRemoveItems.addActionListener(listener);
	}

	/**
	 * 
	 * @param enabled
	 * @param panel
	 */
	private void setEnablePanels(boolean enabled, JPanel panel) {
		for (Component c : panel.getComponents()) {
			c.setEnabled(enabled);
		}
	}
	/**
	 * 
	 */
	private ImagePlus duplicateImp(ImagePlus imp) {
		ImagePlus impDuplicated = imp.duplicate();
		impDuplicated.setFileInfo(imp.getOriginalFileInfo());
		return impDuplicated;
	}

	/**
	 * 
	 */
	public void newOriginalFileName() {
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
	 * 
	 * @param numChannel
	 * @param originalImage
	 * @return
	 */
	public static ImagePlus extractChannelOfStack(int numChannel, ImagePlus originalImage) {
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
}
