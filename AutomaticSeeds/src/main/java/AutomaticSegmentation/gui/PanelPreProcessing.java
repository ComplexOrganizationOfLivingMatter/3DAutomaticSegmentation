/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import AutomaticSegmentation.preProcessing.nuclei3DSegmentation;
import AutomaticSegmentation.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import inra.ijpb.geometry.Box3D;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.region3d.BoundingBox3D;
import inra.ijpb.measure.region3d.Centroid3D;


/**
 * @author
 *
 */
public class PanelPreProcessing extends JPanel {

	/**
	 * 
	 */
	String dir = null;
    int max_nuc_radius = 28,min_nuc_radius = 18, seed_threshold = 29000;
    private JCheckBox prefilteringCheckB,quickSegmentationCheckB,gpuCheckBox;
    public JComboBox<String> cbSegmentedImg;
	private JLabel maxNucleusSizeLb,minNucleusSizeLb,localMaximaThresholdLb,zScaleLb;
	private JSpinner maxNucleusSizeSpin,minNucleusSizeSpin,localMaximaThresholdSpin,zScaleSpin;
	public JButton btRunCancel;
	public JButton btLoad;
	public JButton btShowNuclei;
	public JButton btCalculateROIs;
	private static final long serialVersionUID = 1L;
	private ImagePlus nucleiChannel,segmentedImp,segmentedLoadImg,auxImp;
	private JProgressBar progressBar;
    private Thread preprocessingTask;
	private ExecutorService exec = Executors.newCachedThreadPool();
	private Boolean cancelTask;
	nuclei3DSegmentation nuc3Dseg;
	public ArrayList<ImagePlus> ImpArraylistSegImg;
	private RoiManager rm;
	private JComboBox<String> cbROIShapes;

	/**
	 * @param layout
	 */
	public PanelPreProcessing(LayoutManager layout)  {
		super(layout);
		initPreLimeSegPanel();

	}
	
	/**
	 * @return the nucleiChannel
	 */
	public ImagePlus getNucleiChannel() {
		return nucleiChannel;
	}

	/**
	 * @param nucleiChannel the nucleiChannel to set
	 */
	public void setNucleiChannel(ImagePlus nucleiChannel) {
		this.nucleiChannel = nucleiChannel.duplicate();
		this.nucleiChannel.setFileInfo(nucleiChannel.getOriginalFileInfo());
		zScaleSpin.setValue((float) nucleiChannel.getOriginalFileInfo().pixelDepth/nucleiChannel.getOriginalFileInfo().pixelWidth);

	}

	/**
	 * @return the imp_segmented
	 */
	public ImagePlus getSegmentedImp() {
		return segmentedImp;
	}

	/**
	 * @param imp_segmented the imp_segmented to set
	 */
	public void setSegmentedImp(ImagePlus segmentedImp) {
		this.segmentedImp = segmentedImp;
	}

	
	
	/**
	 * 
	 */
	private void initPreLimeSegPanel() {

		// Init GUI elements
		btRunCancel = new JButton("Run");	

		btLoad = new JButton("Load segmentation");
		btShowNuclei = new JButton("Show nuclei");
		btCalculateROIs = new JButton("Calculate ROIs");
		prefilteringCheckB = new JCheckBox("Pre-filtering (3D median 4-4-2)");
		prefilteringCheckB.setSelected(false);
		quickSegmentationCheckB = new JCheckBox("Quick segmentation");
		quickSegmentationCheckB.setSelected(true);
		gpuCheckBox = new JCheckBox("Use GPU");
		gpuCheckBox.setSelected(false);
		maxNucleusSizeLb = new JLabel("Maximal nucleus radius (pixels)");
		minNucleusSizeLb = new JLabel("Minimal nucleus radius (pixels)");
		localMaximaThresholdLb = new JLabel("Local maxima threshold");
		zScaleLb = new JLabel("Z scale");
		cbSegmentedImg = new JComboBox<String>();
		cbSegmentedImg.addItem("<select labelled image>");
		cbSegmentedImg.setMinimumSize(new Dimension(100, 10));
		cbSegmentedImg.setMaximumSize(new Dimension(300, 60));
		
		cbROIShapes = new JComboBox<String>();
		cbROIShapes.addItem("Outer ROI");
		cbROIShapes.addItem("Inner ROI");

		
		/*MODIFY the limits of JSPinners*/
		maxNucleusSizeSpin = new JSpinner(new SpinnerNumberModel(max_nuc_radius, null, null, 1));
		maxNucleusSizeSpin.setMinimumSize(new Dimension(100, 10));
		minNucleusSizeSpin = new JSpinner(new SpinnerNumberModel(min_nuc_radius, null, null, 1));
		minNucleusSizeSpin.setMinimumSize(new Dimension(100, 10));
		localMaximaThresholdSpin = new JSpinner(new SpinnerNumberModel(seed_threshold, null, null, 1));
		localMaximaThresholdSpin.setMinimumSize(new Dimension(100, 10));
		zScaleSpin = new JSpinner(new SpinnerNumberModel(1.00, null, null, 0.01));
		zScaleSpin.setMinimumSize(new Dimension(100, 10));
		
		
		progressBar = new JProgressBar(0,100);	
		progressBar.setMaximumSize(new Dimension(100, 25));
		cancelTask = false;
		cbSegmentedImg.setEnabled(false);
			
		// Add components
		this.add(btShowNuclei, "align center");
		this.add(prefilteringCheckB,"align left");
		this.add(quickSegmentationCheckB,"align left,wrap");
		this.add(maxNucleusSizeSpin,"align center");
		this.add(maxNucleusSizeLb, "align left");
		this.add(gpuCheckBox, "align left,wrap");
		this.add(minNucleusSizeSpin,"align center");
		this.add(minNucleusSizeLb, "wrap,align left");
		this.add(localMaximaThresholdSpin,"align center");
		this.add(localMaximaThresholdLb, "wrap,align left");
		this.add(zScaleSpin,"align center");
		this.add(zScaleLb, "wrap,align left");
		this.add(progressBar,"align center");
		this.add(btRunCancel,"align left,wrap");
		this.add(new JLabel(""),"wrap");
		this.add(btLoad,"align center");
		this.add(cbSegmentedImg,"align left,wrap");
		this.add(new JLabel(""));
		this.add(btCalculateROIs,"align left");
		this.add(cbROIShapes,"align left");

		btRunCancel.addActionListener(listener);
		btLoad.addActionListener(listener);
		btShowNuclei.addActionListener(listener);
		btCalculateROIs.addActionListener(listener);
		cbSegmentedImg.addActionListener(listener);
		btCalculateROIs.setEnabled(false);
		
		ImpArraylistSegImg = new ArrayList<ImagePlus>();
		ImpArraylistSegImg.add(null);
		
		rm = null;
	}

	
	
	private ActionListener listener = new ActionListener() {

		public void actionPerformed(final ActionEvent e) {
			
			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {
				public void run()
				{
					if(e.getSource() == btRunCancel){
						final String command = e.getActionCommand();
						
						/**************** Run nuclei segmentation *****************/
						if (command.equals("Run")) {	
							int maxN = Integer.valueOf(maxNucleusSizeSpin.getValue().toString()).intValue();
							int minN = Integer.valueOf(minNucleusSizeSpin.getValue().toString()).intValue();
							int maxThresh = Integer.valueOf(localMaximaThresholdSpin.getValue().toString()).intValue();
							float zStep = Float.valueOf(zScaleSpin.getValue().toString()).floatValue();
							
							btLoad.setEnabled(false);
							cbSegmentedImg.setEnabled(false);
							btCalculateROIs.setEnabled(false);
							
							preprocessingTask = new Thread() {
								
								public void run(){
									
									btRunCancel.setText("Cancel");
									dir = nucleiChannel.getOriginalFileInfo().directory;
									segmentedImp =null;
									try {
										
										Instant start = Instant.now();
									   
										nuc3Dseg = new nuclei3DSegmentation(nucleiChannel,maxN,minN,maxThresh,zStep,dir,cancelTask,progressBar,prefilteringCheckB,quickSegmentationCheckB, gpuCheckBox);
										segmentedImp = nuc3Dseg.segmentationProtocol();
										
										Instant finish = Instant.now();
										 
									    long timeElapsed = Duration.between(start, finish).getSeconds(); 
									    IJ.log("duration:"+timeElapsed);
									    
										cancelTask =false;
										// If there was not cancelled the process:
										if(segmentedImp!=null && progressBar.getValue()==100) {
											IJ.error("Nuclei segmentation completed. Please extract their ROIs!");
											newSegmentedFileName(segmentedImp.duplicate());
											cbSegmentedImg.setEnabled(true);
											segmentedImp.show();
										}
										
										progressBar.setValue(0);
										btRunCancel.setText("Run");
										btRunCancel.setEnabled(true);
										btLoad.setEnabled(true);
										btCalculateROIs.setEnabled(true);
										cbSegmentedImg.setEnabled(true);
										
									}catch(Exception ex) {
										ex.printStackTrace();
									}
									
								}
							};
							preprocessingTask.start();
						}
						/**************** Cancel nuclei segmentation *****************/
						else if(command.equals("Cancel")){
							btRunCancel.setText("Run");
							segmentedImp=null;
							
							btRunCancel.setEnabled(false);
							btLoad.setEnabled(false);
							btCalculateROIs.setEnabled(false);
							cbSegmentedImg.setEnabled(false);
							
							IJ.log("Stopping...");
							cancelTask =true;
							nuc3Dseg.setCancelTask(cancelTask);
							
							progressBar.setValue(0);
							//IJ.run("Close All");
							try {
								preprocessingTask.join();
								preprocessingTask.interrupt();
								preprocessingTask = null;
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							btRunCancel.setEnabled(true);
							btLoad.setEnabled(true);
							btCalculateROIs.setEnabled(true);
							cbSegmentedImg.setEnabled(true);
						}
					
					}
										
					/******************* Load segmented nuclei ********************/
					else if(e.getSource() == btLoad){
						try {
							// We just can't open multiple images
							auxImp = IJ.openImage();
							if (auxImp!=null) {
								newSegmentedFileName(auxImp.duplicate());
								cbSegmentedImg.setEnabled(true);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					
					/******************* Select segmented nuclei ********************/
					}else if(e.getSource()==cbSegmentedImg) {
						if (cbSegmentedImg.getSelectedItem() == "<select labelled image>" | cbSegmentedImg.getSelectedIndex() == -1) {
							btCalculateROIs.setEnabled(false);
							cbROIShapes.setEnabled(false);
						} else {
							segmentedLoadImg = ImpArraylistSegImg.get(cbSegmentedImg.getSelectedIndex()).duplicate();
							btCalculateROIs.setEnabled(true);
							cbROIShapes.setEnabled(true);
						}
					}
					
					/******************* Show nuclei channel ********************/
					else if(e.getSource() == btShowNuclei){
						if (nucleiChannel != null) {
							nucleiChannel.duplicate().show();
						}
					}
					
					/******************* Calculte ROIs from segmented nuclei ********************/
					else if(e.getSource() == btCalculateROIs){
						if (segmentedLoadImg!=null) {
							btCalculateROIs.setEnabled(false);
							cbROIShapes.setEnabled(false);
							
							try {
								getNucleiROIs(segmentedLoadImg, progressBar, cbROIShapes);
							}catch(Exception ex) {
								ex.printStackTrace();
								IJ.error("Close RoiManager please");
							}
							btCalculateROIs.setEnabled(true);
							cbROIShapes.setEnabled(true);
							segmentedLoadImg.show();
						}else {
							IJ.error("select a segmented and labelled image");
						}
						
					}
					
				}			
			});
		}
	};
	
        
	/**
	 * 
	 * @param imp
	 */
	public void newSegmentedFileName(ImagePlus imp) {
		if (imp.getNChannels() > 1) {
			for (int numChannel = 0; numChannel < imp.getNChannels(); numChannel++) {
				cbSegmentedImg.addItem(imp.getTitle()+" - C=" + numChannel);
				ImpArraylistSegImg.add((ImagePlus) MainWindow.extractChannelOfStack(numChannel + 1, imp.duplicate()));
			}
		} else {
			cbSegmentedImg.addItem(imp.getTitle().replace("DUP_", ""));
			ImpArraylistSegImg.add((ImagePlus) MainWindow.extractChannelOfStack(1, imp.duplicate()));
		}
	}

	/**
	 * 
	 * @param imp_segmented
	 * @param progressBar2 
	 * @param cbROIShapes 
	 * @return
	 */
	public void getNucleiROIs(ImagePlus imp_segmented, JProgressBar progressBar2, JComboBox<String> cbROIShapes) {
		// 3D-OC options settings
		Prefs.set("3D-OC-Options_centroid.boolean", true);
		
		progressBar2.setValue(0);

		int[] labels = LabelImages.findAllLabels(imp_segmented.getImageStack());
		// deprecatedGeometricMeasures3D - investigate about the new region3D

		double[][] centroidList = Centroid3D.centroids(imp_segmented.getImageStack(), labels);
		// double[][] centroidList = Centroid3D.centroids();
		// 0 0 1 2
		// | | | |
		// centroid -> [id][x,y,z]

		Box3D[] bboxes = BoundingBox3D.boundingBoxes(imp_segmented.getImageStack(), labels,
				imp_segmented.getCalibration());

		// Creating the ROI manager
		if (rm == null) {
			rm = new RoiManager();
			// Reset to 0 the RoiManager
			rm.reset();
		}
		progressBar2.setValue(20);
		for (int i = 0; i < centroidList.length; i++) {
			// Get the slice to create the ROI
			int z = (int) Math.round(centroidList[i][2]);
			// Get the area and radius of the index i nuclei
			double[] radii = { bboxes[i].height(), bboxes[i].width() };
			double[] calibrations = { imp_segmented.getCalibration().pixelHeight,
					imp_segmented.getCalibration().pixelWidth };
			
			double growingROIFactor = 1.2; //Default for outer ROIs
			if (cbROIShapes.getSelectedIndex() == 1) // Inner roi
				growingROIFactor = 0.5;
			
			double majorRadius = growingROIFactor * Utils.getMin(radii) / Utils.getMean(calibrations);
			int r = (int) Math.round(majorRadius);
			Roi roi = new OvalRoi(centroidList[i][0] - r / 2, centroidList[i][1] - r / 2, r, r);
			roi.setPosition(z);
			rm.addRoi(roi);
			progressBar2.setValue(20 + (i/centroidList.length*80));
		}
	
		progressBar2.setValue(100);
	}
}
