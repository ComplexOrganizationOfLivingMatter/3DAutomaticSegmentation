/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import AutomaticSegmentation.elements.Cell3D;
import AutomaticSegmentation.elements.RoiAdjustment;
import eu.kiaru.limeseg.LimeSeg;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;

/**
 * @author 
 *
 */
public class PanelPostProcessing extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton btPostLimeSeg;
	
	/**
	 * PostLimeSeg attributes
	 */
	private PostProcessingWindow postprocessingWindow;
	public RoiAdjustment newCell;
	public PolygonRoi polyRoi2;

	private JButton btnPostSave;
	private JButton btnInsert;
	private JButton btnLumen;
	private JButton btn3DDisplay;
	private JComboBox<String> checkOverlay;
	private JComboBox<String> checkLumen;
	private JSpinner cellSpinner;
	private PolygonRoi polyRoi;
	public Cell3D PostProcessCell;
	public PolygonRoi[][] lumenDots;
	private ImagePlus cellOutlineChannel;

	/**
	 * 
	 */
	public PanelPostProcessing() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param layout
	 */
	public PanelPostProcessing(LayoutManager layout) {
		super(layout);
		// TODO Auto-generated constructor stub
		btPostLimeSeg = new JButton("Run PostProcessing");
		this.add(btPostLimeSeg);

		btPostLimeSeg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// btPostLimeSeg.setEnabled(false);
				// cellOutline will show in the postProcessingWindow
				initPostLimeSegPanel();
				btPostLimeSeg.setEnabled(false);
				checkOverlay.addActionListener(this);
				checkLumen.addActionListener(this);
				btnInsert.addActionListener(this);
				btnPostSave.addActionListener(this);
				btnLumen.addActionListener(this);
				btn3DDisplay.addActionListener(this);
				cellSpinner.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						updateOverlay();
					}
				});

				if (e.getSource() == checkOverlay) {
					updateOverlay();
				}

				if (e.getSource() == checkLumen) {
					updateOverlay();
				}

				if (e.getSource() == btnInsert) {
					postprocessingWindow.addROI();
					// Check if polyRoi is different to null, if is do the
					// modify cell
					if (postprocessingWindow.polyRoi != null) {
						postprocessingWindow.all3dCells = newCell.removeOverlappingRegions(
								postprocessingWindow.all3dCells, polyRoi, cellOutlineChannel.getCurrentSlice(),
								postprocessingWindow.all3dCells.get((Integer) cellSpinner.getValue() - 1).id_Cell,
								postprocessingWindow.lumenDots);
						checkOverlay.setSelectedIndex(1);
						updateOverlay();
						// After modify cell return poly to null, clean the roi
						polyRoi = null;
					}
					// If polyRoi is null show a message to prevent errors
					else {
						JOptionPane.showMessageDialog((Component) e.getSource(), "You must select a new Region.");
					}

				}

				if (e.getSource() == btnPostSave) {
					postprocessingWindow.savePlyFiles(postprocessingWindow.all3dCells,
							postprocessingWindow.initialDirectory);
					// After saved the plyFiles show a message to inform the
					// user
					JOptionPane.showMessageDialog((Component) e.getSource(), "Saved results.");
				}

				if (e.getSource() == btnLumen) {
					// read the lumen
					postprocessingWindow.loadLumen();
					// remove the overlaps cells
					postprocessingWindow.removeCellLumenOverlap();
					updateOverlay();
				}

				if (e.getSource() == btn3DDisplay) {
					String path_in = postprocessingWindow.initialDirectory;
					LimeSeg.loadStateFromXmlPly(path_in);
					LimeSeg.make3DViewVisible();
					LimeSeg.putAllCellsTo3DDisplay();
					System.out.println("READY");
				}

				// btPostLimeSeg.setEnabled(true);
			}
		});
	}
	
	/**
	 * 
	 * @return
	 */
	public ImagePlus getCellOutlineChannel() {
		return cellOutlineChannel;
	}

	/**
	 * 
	 * @param cellOutlineChannel
	 */
	public void setCellOutlineChannel(ImagePlus cellOutlineChannel) {
		this.cellOutlineChannel = cellOutlineChannel;
	}

	/**
	 * 
	 */
	private void initPostLimeSegPanel() {
		postprocessingWindow = new PostProcessingWindow(cellOutlineChannel);
		cellOutlineChannel = postprocessingWindow.workingImp;
		//Init GUI elements
		cellSpinner = new JSpinner();
		cellSpinner.setModel(new SpinnerNumberModel(1, 1, postprocessingWindow.all3dCells.size(), 1));
		

		checkOverlay = new JComboBox<String>();
		checkOverlay.addItem("None overlay");
		checkOverlay.addItem("Cell overlay");
		checkOverlay.addItem("All overlays");
		checkOverlay.setSelectedIndex(2);
		
		checkLumen = new JComboBox<String>();
		checkLumen.addItem("Without lumen");
		checkLumen.addItem("Show lumen");
		checkLumen.setSelectedIndex(0);
		
		btnInsert = new JButton("Modify Cell");
		btnPostSave = new JButton("Save Results");
		btnLumen = new JButton("Update Lumen");
		btn3DDisplay = new JButton("Show 3D Cell");
 		
 		//Add components
 		this.add(cellSpinner);
 		this.add(btPostLimeSeg, "wrap");
 		this.add(checkOverlay);
 		this.add(btnInsert, "wrap");
 		this.add(btnLumen);
 		this.add(btnPostSave);
 		this.add(checkLumen);
	}
	
	//PostLimeSeg Methods
	public void updateOverlay() {

		cellOutlineChannel.getOverlay().clear();

		if (checkOverlay.getSelectedItem() == "All overlays") {
			Overlay newOverlay = postprocessingWindow.addOverlay(((Integer) cellSpinner.getValue() - 1), cellOutlineChannel.getCurrentSlice(),
					postprocessingWindow.all3dCells, cellOutlineChannel, true, lumenDots);
			cellOutlineChannel.setOverlay(newOverlay);
		} else if (checkOverlay.getSelectedItem() == "Cell overlay") {
			Overlay newOverlay = postprocessingWindow.addOverlay(((Integer) cellSpinner.getValue() - 1), cellOutlineChannel.getCurrentSlice(),
					postprocessingWindow.all3dCells, cellOutlineChannel, false, lumenDots);
			cellOutlineChannel.setOverlay(newOverlay);
		}

		cellOutlineChannel.updateAndRepaintWindow();
	}

	/**
	 * @param isDoubleBuffered
	 */
	public PanelPostProcessing(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param layout
	 * @param isDoubleBuffered
	 */
	public PanelPostProcessing(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		// TODO Auto-generated constructor stub
	}

}
