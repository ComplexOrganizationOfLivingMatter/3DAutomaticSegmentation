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
import javax.swing.JFileChooser;
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
import ij.ImageListener;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;

/**
 * @author
 *
 */
public class PanelLimeSeg extends JPanel implements ActionListener {

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
	private JFileChooser fileChooser;

	/**
	 * @param layout
	 */
	public PanelLimeSeg(LayoutManager layout) {
		super(layout);

		initLimeSegPanel();
		fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}
	

	public void actionPerformed(ActionEvent e) {

			if (e.getSource() == btnSavePly) {

				ArrayList<Cell> cell = LimeSeg.allCells;
				if (cell.size() > 0) {
				
					fileChooser.setCurrentDirectory(new File(cellOutlineChannel.getOriginalFileInfo().directory));
					fileChooser.setDialogTitle("Select save folder");
					
					if (fileChooser.showOpenDialog(this) == fileChooser.APPROVE_OPTION) {
						// String path =
						// cellOutlineChannel.getOriginalFileInfo().directory +
						// "OutputLimeSeg";
						File dir = new File(fileChooser.getSelectedFile().toString() + "/OutputLimeSeg");
						if (!dir.isDirectory()) {
							System.out.println("New folder created");
							dir.mkdir();
						}

						if (dir.listFiles().length != 0) {
							// Show dialog to confirm
							int dialogResult = JOptionPane.showConfirmDialog(btnSavePly.getParent(),
									"Saving will remove the content of the select folder, confirm?", "Warning",
									JOptionPane.YES_NO_OPTION);
							if (dialogResult == JOptionPane.YES_OPTION) {
								purgeDirectory(dir, 1);
								LimeSeg.saveStateToXmlPly(dir.toString());
								JOptionPane.showMessageDialog(btnSavePly.getParent(), "Saved!");
							}
							
						} else {
							LimeSeg.saveStateToXmlPly(dir.toString());
							JOptionPane.showMessageDialog(btnSavePly.getParent(), "Saved!");
						}
						
					} else {
						IJ.log("Any folder selected");
					}

				} else {
					IJ.log("Any cell segmented");
				}
			}

			if (e.getSource() == btStopOptimisation) {
				LimeSeg.stopOptimisation();
				cf.setClearOptimizer(true);
			}

			if (e.getSource() == btRoiManager) {
				RoiManager.getRoiManager();
			}

			if (e.getSource() == btShowOutlines) {
				if (cellOutlineChannel != null) {
					cellOutlineChannel.duplicate().show();
				}
			}

			if (e.getSource() == btLimeSeg) {
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
	}

	/**
	 * @return the cellOutlineChannel
	 */
	public ImagePlus getCellOutlineChannel() {
		return cellOutlineChannel;
	}

	/**
	 * @param cellOutlineChannel
	 *            the cellOutlineChannel to set
	 */
	public void setCellOutlineChannel(ImagePlus cellOutlineChannel) {
		this.cellOutlineChannel = cellOutlineChannel;
	}

	/**
	 * @param js_zScale
	 *            the js_zScale to set
	 */
	public void setZScale(double js_zScale) {
		this.js_zScale.setValue(js_zScale);
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
		js_zScale = new JSpinner(new SpinnerNumberModel(1.0, null, null, 0.01));
		js_zScale.setMinimumSize(new Dimension(100, 10));
		this.add(label_zScale, "align center");
		this.add(js_zScale, "wrap, align center");

		label_rangeD0 = new JLabel("Range in D0 units:");
		js_rangeD0 = new JSpinner(new SpinnerNumberModel(2, null, null, 1));
		js_rangeD0.setMinimumSize(new Dimension(100, 10));
		this.add(label_rangeD0, "align center");
		this.add(js_rangeD0, "wrap, align center");

		btRoiManager = new JButton("Open Roi Manager");
		btRoiManager.addActionListener(this);
		this.add(btRoiManager, "align center");

		btLimeSeg = new JButton("Start");
		btLimeSeg.addActionListener(this);
		this.add(btLimeSeg, "wrap, align center");

		btShowOutlines = new JButton("Show Stack");
		btShowOutlines.addActionListener(this);
		this.add(btShowOutlines, "align center");

		btStopOptimisation = new JButton("Stop");
		btStopOptimisation.addActionListener(this);
		this.add(btStopOptimisation, "wrap, align center");

		btnSavePly = new JButton("Save");
		btnSavePly.addActionListener(this);
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
