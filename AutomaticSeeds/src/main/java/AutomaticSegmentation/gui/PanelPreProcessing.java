/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import AutomaticSegmentation.preProcessing.NucleiSegmentation3D;
import AutomaticSegmentation.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import inra.ijpb.geometry.Box3D;
import inra.ijpb.label.LabelImages;
import inra.ijpb.measure.region3d.BoundingBox3D;
import inra.ijpb.measure.region3d.Centroid3D;
import net.haesleinhuepf.clij.CLIJ;

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
	private JLabel maxNucleusSizeLb,minNucleusSizeLb,localMaximaThresholdLb,zScaleLb;
	private JSpinner maxNucleusSizeSpin,minNucleusSizeSpin,localMaximaThresholdSpin,zScaleSpin;
	private JButton btRun, btCancel,btLoad,btShowNuclei;
	private static final long serialVersionUID = 1L;
	private ImagePlus imp_segmented,nucleiChannel;
	private ProgressBar progressBar;
    private Thread preprocessingTask;
	private final ExecutorService exec = Executors.newFixedThreadPool(1);

	/**
	 * @param layout
	 */
	public PanelPreProcessing(LayoutManager layout)  {
		super(layout);
		initPreLimeSegPanel();
		preprocessingTask = new Thread() {};
		btCancel.setEnabled(false);
		
//		executor = Executors.newSingleThreadExecutor();
		btRun.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				//executor = Executors.newSingleThreadExecutor();
				//executor.submit(() -> 
				preprocessingTask = new Thread() { 
					public void run(){
						btRun.setEnabled(false);
						btCancel.setEnabled(true);
						
						int maxN = Integer.valueOf(maxNucleusSizeSpin.getValue().toString()).intValue();
						int minN = Integer.valueOf(minNucleusSizeSpin.getValue().toString()).intValue();
						int maxThresh = Integer.valueOf(localMaximaThresholdSpin.getValue().toString()).intValue();
						float zStep = Float.valueOf(zScaleSpin.getValue().toString()).floatValue();
	
						NucleiSegmentation3D nucSeg3D = new NucleiSegmentation3D(nucleiChannel,maxN,minN,zStep,maxThresh,prefilteringCheckB.isSelected());
						imp_segmented = nucSeg3D.impSegmented.duplicate();
						btRun.setEnabled(true);
						imp_segmented.show();
						RoiManager rm = getNucleiROIs(imp_segmented);
						//executor.shutdown();
					}
				};
				preprocessingTask.run();
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
		
		btCancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				preprocessingTask.interrupt();
				preprocessingTask = null;
				//executor.shutdown();
				btRun.setEnabled(true);
			}
		});

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
	public ImagePlus getImp_segmented() {
		return imp_segmented;
	}

	/**
	 * @param imp_segmented the imp_segmented to set
	 */
	public void setImp_segmented(ImagePlus imp_segmented) {
		this.imp_segmented = imp_segmented;
	}

	
	
	/**
	 * 
	 */
	private void initPreLimeSegPanel() {
		imp_segmented = new ImagePlus();

		// Init GUI elements
		btRun = new JButton("Run");	
		btCancel = new JButton("Cancel");
		btCancel.setEnabled(false);
		btLoad = new JButton("Load labelled 3D nuclei");
		btShowNuclei = new JButton("Show nuclei");
		prefilteringCheckB = new JCheckBox("Pre-filtering (3D median 4-4-2");
		prefilteringCheckB.setSelected(false);
		maxNucleusSizeLb = new JLabel("Maximal nucleus size");
		minNucleusSizeLb = new JLabel("Minimal nucleus size");
		localMaximaThresholdLb = new JLabel("Seed threshold");
		zScaleLb = new JLabel("Z scale");

		
		/*MODIFY the limits of JSPinners*/
		maxNucleusSizeSpin = new JSpinner(new SpinnerNumberModel(max_nuc_radius, null, null, 1));
		maxNucleusSizeSpin.setMinimumSize(new Dimension(100, 10));
		minNucleusSizeSpin = new JSpinner(new SpinnerNumberModel(min_nuc_radius, null, null, 1));
		minNucleusSizeSpin.setMinimumSize(new Dimension(100, 10));
		localMaximaThresholdSpin = new JSpinner(new SpinnerNumberModel(seed_threshold, null, null, 1));
		localMaximaThresholdSpin.setMinimumSize(new Dimension(100, 10));
		zScaleSpin = new JSpinner(new SpinnerNumberModel(1.00, null, null, 0.01));
		zScaleSpin.setMinimumSize(new Dimension(100, 10));
		
		
		progressBar = new ProgressBar(100, 25);		
		
		//panelPreproc = new JPanel();
		
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
		this.add(btRun,"align left,wrap");
		this.add(btCancel,"align left");
		this.add(progressBar,"wrap, align left");	
		this.add(btLoad);

		btRun.addActionListener(listener);
		btCancel.addActionListener(listener);
		btLoad.addActionListener(listener);
		btShowNuclei.addActionListener(listener);
		
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
					if(e.getSource() == btRun)
					{
						preprocessingTask = new Thread() { 
							public void run(){
								btRun.setEnabled(false);
								btCancel.setEnabled(true);
								
								int maxN = Integer.valueOf(maxNucleusSizeSpin.getValue().toString()).intValue();
								int minN = Integer.valueOf(minNucleusSizeSpin.getValue().toString()).intValue();
								int maxThresh = Integer.valueOf(localMaximaThresholdSpin.getValue().toString()).intValue();
								float zStep = Float.valueOf(zScaleSpin.getValue().toString()).floatValue();
			
								NucleiSegmentation3D nucSeg3D = new NucleiSegmentation3D(nucleiChannel,maxN,minN,zStep,maxThresh,prefilteringCheckB.isSelected());
								imp_segmented = nucSeg3D.impSegmented.duplicate();
								btRun.setEnabled(true);
								imp_segmented.show();
								RoiManager rm = getNucleiROIs(imp_segmented);
								//executor.shutdown();
							}
						};
						preprocessingTask.run();
					}
					else if(e.getSource() == btCancel){
						try { 
							
							if(null != preprocessingTask){
								Thread newTask = new Thread();
								newTask = preprocessingTask;
								newTask.interrupt();
								// Although not recommended and already deprecated,
								// use stop command so WEKA classifiers are actually
								// stopped.
								newTask.stop();
							}else {
								IJ.log("Error: interrupting training failed becaused the thread is null!");
							}
						}
						catch(Exception ex){
							ex.printStackTrace();
						}
						
					}
					else if(e.getSource() == btLoad){
						//
					}
					else if(e.getSource() == btShowNuclei){
						// 
					}
				}			
			});
		}
	};
	
}
