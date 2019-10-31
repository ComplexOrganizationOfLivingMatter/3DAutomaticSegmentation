/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import AutomaticSegmentation.preProcessing.DefaultSegmentation;
import AutomaticSegmentation.preProcessing.SegmBigAndOverlappedNuclei;
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
	private JLabel maxNucleusSizeLb,minNucleusSizeLb,seedThresholdLb;
	private JTextField maxNucleusSizeTextF,minNucleusSizeTextF,seedThresholdTextF;
	private JButton btRun, btCancel,btLoad,btShowNuclei;
	private static final long serialVersionUID = 1L;
	private ImagePlus imp_segmented,nucleiChannel;
	private ProgressBar progressBar;
	private JPanel panelPreproc;
	
	/**
	 * @param layout
	 */
	public PanelPreProcessing(LayoutManager layout) {
		super(layout);
		
		initPreLimeSegPanel();

		
		/*btPreLimeSeg.addActionListener(new ActionListener() {

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
		*/

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
		btLoad = new JButton("Load labelled 3D nuclei");
		btShowNuclei = new JButton("Show nuclei");
		prefilteringCheckB = new JCheckBox("Pre-filtering (3D median 4-4-2");
		prefilteringCheckB.setSelected(false);
		maxNucleusSizeLb = new JLabel("Maximal nucleus size");
		minNucleusSizeLb = new JLabel("Minimal nucleus size");
		seedThresholdLb = new JLabel("Seed threshold");
		maxNucleusSizeTextF = new JTextField(String.valueOf(max_nuc_radius),10);
		minNucleusSizeTextF = new JTextField(String.valueOf(min_nuc_radius),10);
		seedThresholdTextF = new JTextField(String.valueOf(seed_threshold),10);
		progressBar = new ProgressBar(100, 25);		
		
		//panelPreproc = new JPanel();
		
		// Add components
		this.add(btShowNuclei, "align center,wrap");
		this.add(prefilteringCheckB,"align left,wrap");
		this.add(maxNucleusSizeLb,"align left");
		this.add(maxNucleusSizeTextF, "wrap,align left");
		this.add(minNucleusSizeLb,"align left");
		this.add(minNucleusSizeTextF, "wrap,align left");
		this.add(seedThresholdLb,"align left");
		this.add(seedThresholdTextF, "wrap,align left");
		this.add(btRun,"align left,wrap");
		this.add(btCancel,"align left");
		this.add(progressBar,"wrap, align left");	
		this.add(btLoad);

		
	}

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
	
}
