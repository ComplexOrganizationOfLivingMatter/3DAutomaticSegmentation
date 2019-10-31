/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import AutomaticSegmentation.limeSeg.SphereSegAdapted;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.commands.ClearAll;
import eu.kiaru.limeseg.struct.Cell;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * @author 
 *
 */
public class PanelLimeSeg extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
	private ImagePlus cellOutlineChannel;

	/**
	 * @param layout
	 */
	public PanelLimeSeg(LayoutManager layout) {
		super(layout);

		initLimeSegPanel();

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

	/**
	 * @return the cellOutlineChannel
	 */
	public ImagePlus getCellOutlineChannel() {
		return cellOutlineChannel;
	}

	/**
	 * @param cellOutlineChannel the cellOutlineChannel to set
	 */
	public void setCellOutlineChannel(ImagePlus cellOutlineChannel) {
		this.cellOutlineChannel = cellOutlineChannel;
	}

	/**
	 * @param js_zScale
	 *            the js_zScale to set
	 */
	public void setZScale(double d) {
		this.js_zScale.setValue(d);
	}

	/**
	 * 
	 */
	private void initLimeSegPanel() {

		// Init GUI
		cf = new SphereSegAdapted();
		label_D0 = new JLabel("D_0:");
		js_D0 = new JSpinner(new SpinnerNumberModel(5.5, null, null, 0.1));

		js_D0.setMinimumSize(new Dimension(100, 10));
		this.add(label_D0, "align center");
		this.add(js_D0, "wrap, align center");

		label_fPressure = new JLabel("F_Pressure:");
		js_fPressure = new JSpinner(new SpinnerNumberModel(0.015, -0.04, 0.04, 0.001));
		js_fPressure.setMinimumSize(new Dimension(100, 10));
		this.add(label_fPressure, "align center");
		this.add(js_fPressure, "wrap, align center");

		label_zScale = new JLabel("Z scale:");
		js_zScale = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.1));
		js_zScale.setMinimumSize(new Dimension(100, 10));
		this.add(label_zScale, "align center");
		this.add(js_zScale, "wrap, align center");

		label_rangeD0 = new JLabel("Range in D0 units:");
		js_rangeD0 = new JSpinner(new SpinnerNumberModel(2, null, null, 1));
		js_rangeD0.setMinimumSize(new Dimension(100, 10));
		this.add(label_rangeD0, "align center");
		this.add(js_rangeD0, "wrap, align center");

		btRoiManager = new JButton("Open Roi Manager");
		this.add(btRoiManager, "align center");

		btLimeSeg = new JButton("Start");
		this.add(btLimeSeg, "wrap, align center");

		btShowOutlines = new JButton("Show Stack");
		this.add(btShowOutlines, "align center");

		btStopOptimisation = new JButton("Stop");
		this.add(btStopOptimisation, "wrap, align center");

		btnSavePly = new JButton("Save");
		this.add(new JLabel(""));
		this.add(btnSavePly, "align center");
	}

	/**
	 * 
	 * @param dir
	 * @param height
	 */
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
