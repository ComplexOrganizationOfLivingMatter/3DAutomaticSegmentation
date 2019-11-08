/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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

import AutomaticSegmentation.utils.Utils;
import filters.Bandpass3D;
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
    private JCheckBox prefilteringCheckB;
    public JComboBox<String> cbSegmentedImg;
	private JLabel maxNucleusSizeLb,minNucleusSizeLb,localMaximaThresholdLb,zScaleLb;
	private JSpinner maxNucleusSizeSpin,minNucleusSizeSpin,localMaximaThresholdSpin,zScaleSpin;
	private JButton btRunCancel, btLoad,btShowNuclei,btCalculateROIs;
	private static final long serialVersionUID = 1L;
	private ImagePlus nucleiChannel,preprocessedImp,segmentedImp,segmentedLoadImg,auxImp;
	private JProgressBar progressBar;
    private Thread preprocessingTask;
	//private ExecutorService exec = Executors.newFixedThreadPool(1);
	private ExecutorService exec = Executors.newCachedThreadPool();
	private Boolean cancelTask;
	public ArrayList<ImagePlus> ImpArraylistSegImg;


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
		this.nucleiChannel = nucleiChannel;
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
		btCalculateROIs = new JButton("Get ROIs");
		prefilteringCheckB = new JCheckBox("Pre-filtering (3D median 4-4-2");
		prefilteringCheckB.setSelected(false);
		maxNucleusSizeLb = new JLabel("Maximal nucleus radius (pixels)");
		minNucleusSizeLb = new JLabel("Minimal nucleus radius (pixels)");
		localMaximaThresholdLb = new JLabel("Local maxima threshold");
		zScaleLb = new JLabel("Z scale");
		cbSegmentedImg = new JComboBox<String>();
		cbSegmentedImg.addItem("<select image>");
		cbSegmentedImg.setMinimumSize(new Dimension(100, 10));
		cbSegmentedImg.setMaximumSize(new Dimension(300, 60));

		
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
		progressBar.setMaximumSize(new Dimension(60, 8));
		cancelTask = false;
		cbSegmentedImg.setEnabled(false);
			
		// Add components
		this.add(btShowNuclei, "align center,wrap");
		this.add(prefilteringCheckB,"align left,wrap");
		this.add(maxNucleusSizeLb,"align left");
		this.add(maxNucleusSizeSpin, "wrap,align left");
		this.add(minNucleusSizeLb,"align left");
		this.add(minNucleusSizeSpin, "wrap,align left");
		this.add(localMaximaThresholdLb,"align left");
		this.add(localMaximaThresholdSpin, "wrap,align left");
		this.add(zScaleLb,"align left");
		this.add(zScaleSpin, "wrap,align left");
		this.add(btRunCancel,"align left,wrap");
		this.add(progressBar,"align left");
		this.add(btLoad,"wrap");
		this.add(cbSegmentedImg,"align left,wrap");
		this.add(btCalculateROIs,"align left");

		btRunCancel.addActionListener(listener);
		btLoad.addActionListener(listener);
		btShowNuclei.addActionListener(listener);
		btCalculateROIs.addActionListener(listener);
		cbSegmentedImg.addActionListener(listener);
		btCalculateROIs.setEnabled(false);
		
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
	
	private ActionListener listener = new ActionListener() {

		public void actionPerformed(final ActionEvent e) {
			
			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {
				public void run()
				{
					if(e.getSource() == btRunCancel){
						final String command = e.getActionCommand();
						if (command.equals("Run")) {	
							int maxN = Integer.valueOf(maxNucleusSizeSpin.getValue().toString()).intValue();
							int minN = Integer.valueOf(minNucleusSizeSpin.getValue().toString()).intValue();
							int maxThresh = Integer.valueOf(localMaximaThresholdSpin.getValue().toString()).intValue();
							float zStep = Float.valueOf(zScaleSpin.getValue().toString()).floatValue();
							btLoad.setEnabled(false);

							preprocessingTask = new Thread() {
								
								public void run(){
									
																	
									btRunCancel.setText("Cancel");
									dir = nucleiChannel.getOriginalFileInfo().directory;
									preprocessedImp = null;
									segmentedImp =null;
									try {
										nucleiSegmentation3D(maxN,minN,zStep,maxThresh,prefilteringCheckB.isSelected());
										cancelTask =false;
										if(segmentedImp!=null) {
											if(segmentedImp.getTitle().compareTo("dapi-seg")==0) {
												newSegmentedFileName(segmentedImp.duplicate());
												cbSegmentedImg.setEnabled(true);
											}
										}else {
											preprocessedImp = null;
											segmentedImp =null;
										}
										
										btRunCancel.setEnabled(true);
									}catch(Exception ex) {
										ex.printStackTrace();
									}
									
								}
							};
							preprocessingTask.start();
						}
						else if(command.equals("Cancel")){
							btRunCancel.setText("Run");
							
							preprocessedImp=null;
							segmentedImp=null;
							btRunCancel.setEnabled(false);
							IJ.log("Stopping...");
							cancelTask =true;
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
						}
					
					}
											
					else if(e.getSource() == btLoad){
						try {
							// We just can't open multiple images
							auxImp = IJ.openImage();
							newSegmentedFileName(auxImp.duplicate());
							cbSegmentedImg.setEnabled(true);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						//
					}else if(e.getSource()==cbSegmentedImg) {
						if (cbSegmentedImg.getSelectedItem() == "<select image>" | cbSegmentedImg.getSelectedIndex() == -1) {
							btCalculateROIs.setEnabled(false);
						} else {
							segmentedLoadImg = ImpArraylistSegImg.get(cbSegmentedImg.getSelectedIndex()).duplicate();
							btCalculateROIs.setEnabled(true);
						}
					}
					else if(e.getSource() == btShowNuclei){
						if (nucleiChannel != null) {
							nucleiChannel.duplicate().show();
						}
					}
					else if(e.getSource() == btCalculateROIs){
						if (segmentedLoadImg!=null) {
							btCalculateROIs.setEnabled(false);
							segmentedLoadImg.show();
							try {
								RoiManager rm = getNucleiROIs(segmentedLoadImg.duplicate());
							}catch(Exception ex) {
								ex.printStackTrace();
							}
							btCalculateROIs.setEnabled(true);
							
						}else {
							IJ.error("select a segmented and labelled image");
						}
						
					}
					
				}			
			});
		}
	};
	
	
	public void nucleiSegmentation3D(int max_nuc_radius, int min_nuc_radius, float zDepth, int maxThresh,boolean prefilter) {
		
    	while(!cancelTask.booleanValue()) {
    		
	    	String subdir = null;
	        File fl = new File(dir+"SEG");
	        if (!fl.exists()){
	            if (fl.mkdir()) {
	                subdir = dir + "SEG/";
	            } else {
	                subdir = dir;
	            }
	        }
	        else{
	            subdir = this.dir + "SEG/";
	        }
	        
	        progressBar.setValue(5);
	    	preprocessedImp = nucleiChannel.duplicate();
	    	preprocessedImp.setTitle("Dapi_channel");
	        
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        
	        if(prefilteringCheckB.isSelected()){
	        	
	            IJ.log("Pre-filtering start");
	            IJ.run(this.preprocessedImp, "Median 3D...", "x=4 y=4 z=2");
	            IJ.log("Pre-filtering completed");           
	        }
	        
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        
	        progressBar.setValue(20);
	        
	        IJ.saveAs(preprocessedImp,"Tiff", subdir+preprocessedImp.getTitle()+".tif");
	        IJ.log("Save the nuclei image as Dapi_channel.tif in "+subdir);
	        

	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(25);

	        //band pass filtering
	        IJ.run(preprocessedImp, "32-bit", "");
	        IJ.log("Computing Band Pass...");
	
	        
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(35);

			// filter image with 3D-bandpass
	 		Bandpass3D bp3d = new Bandpass3D();
	        
			bp3d.in_hprad = max_nuc_radius;
	 		bp3d.in_lprad = min_nuc_radius;
	 		bp3d.in_xz_ratio = zDepth;
			bp3d.in_image = preprocessedImp.getStack();
			if (!bp3d.checkInputParams().equals("")) {
				IJ.showMessage(bp3d.checkInputParams());
				return;
			}
			bp3d.filterit();
			
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(60);
			
	        this.preprocessedImp = new ImagePlus("BP_nuclei", bp3d.out_result);
	 			 		
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(63);
	        
	        IJ.saveAs(this.preprocessedImp,"Tiff", subdir+"BP_nuclei.tif");
	        IJ.log("Save the band pass filtered image as BP_nuclei.tif in "+subdir);
	        segmentedImp = preprocessedImp.duplicate();
	        segmentedImp.setSlice(segmentedImp.getNSlices()/2);
	        progressBar.setValue(64);
	        IJ.run(this.segmentedImp, "Enhance Contrast...", "saturated=0"); 
	        
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(65);
	        
	        IJ.run(segmentedImp, "16-bit", "");
	        
	        if (this.cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(67);
	        
	        //nuclei segmentation
	        IJ.log("Segmenting nuclei");
	        String seg_option = "seeds_threshold="+maxThresh+" local_background=0 radius_0=2 radius_1=4 radius_2=6 weigth=0.50 radius_max="+max_nuc_radius+" sd_value="+max_nuc_radius/10+" local_threshold=[Gaussian fit] seg_spot=Block watershed volume_min="+Math.round((4/3)*Math.PI*min_nuc_radius)+" volume_max="+Math.round((4/3)*Math.PI*max_nuc_radius)+" seeds=Automatic spots=BP_nuclei radius_for_seeds=2 output=[Label Image]";
	        IJ.run(segmentedImp,"3D Spot Segmentation",seg_option);
	        
	        segmentedImp.setTitle("dapi-seg");
	        IJ.saveAs(segmentedImp,"Tiff", subdir+"dapi-seg.tif");
	        
	        if (cancelTask.booleanValue()) {
	        	IJ.log("Nuclei segmentation STOPPED");
	        	break;
	        }
	        progressBar.setValue(100);
	        IJ.log("Save the segmented nuclei image as dapi-seg.tif in "+subdir);
    	}
        
    }
        
	
	public void newSegmentedFileName(ImagePlus imp) {
		if (imp.getNChannels() > 1) {
			for (int numChannel = 0; numChannel < imp.getNChannels(); numChannel++) {
				cbSegmentedImg.addItem(imp.getTitle()+" - C=" + numChannel);
				ImpArraylistSegImg.add(MainWindow.extractChannelOfStack(numChannel + 1, imp.duplicate()));
			}
		} else {
			cbSegmentedImg.addItem(imp.getTitle());
			ImpArraylistSegImg.add(MainWindow.extractChannelOfStack(1, imp.duplicate()));
		}
	}

}
