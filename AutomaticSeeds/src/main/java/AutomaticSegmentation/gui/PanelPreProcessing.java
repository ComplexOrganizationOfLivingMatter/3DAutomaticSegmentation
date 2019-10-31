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
	private static final long serialVersionUID = 1L;
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
	private ImagePlus nucleiChannel;

	/**
	 * @param layout
	 */
	public PanelPreProcessing(LayoutManager layout) {
		super(layout);
		
		initPreLimeSegPanel();

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
	 * @return the thresholdMethodPanel
	 */
	public JPanel getThresholdMethodPanel() {
		return ThresholdMethodPanel;
	}

	/**
	 * @param thresholdMethodPanel
	 *            the thresholdMethodPanel to set
	 */
	public void setThresholdMethodPanel(JPanel thresholdMethodPanel) {
		ThresholdMethodPanel = thresholdMethodPanel;
	}
	
	/**
	 * 
	 */
	private void initPreLimeSegPanel() {
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

		this.add(btShowNuclei, "wrap");
		this.add(ThresholdMethodPanel);
		this.add(btThresholdMethod, "wrap, align left");
		this.add(cbPredefinedTypeSegmentation, "wrap");
		cbPredefinedTypeSegmentation.setSelectedIndex(0);
		this.add(btPreLimeSeg, "wrap");
		this.add(progressBar, "align center");
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
