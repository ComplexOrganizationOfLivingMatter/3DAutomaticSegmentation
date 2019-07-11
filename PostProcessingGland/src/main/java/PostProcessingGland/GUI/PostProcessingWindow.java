
package PostProcessingGland.GUI;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import PostProcessingGland.Elements.Cell3D;
import PostProcessingGland.Elements.RoiAdjustment;
// import JTableModel;
import PostProcessingGland.GUI.CustomElements.CustomCanvas;
import PostProcessingGland.GUI.CustomElements.SegmentationOverlay;
import PostProcessingGland.PostProcessingGland;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.io.IOXmlPlyLimeSeg;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.DotN;
import fiji.util.gui.OverlayedImageCanvas.Overlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class PostProcessingWindow extends ImageWindow implements
	ActionListener
{

	private IOXmlPlyLimeSeg OutputLimeSeg;
	private Hashtable<Integer, ArrayList<Roi>> cellsROIs;
	private CustomCanvas canvas;
	private SegmentationOverlay overlayResult;
	private Cell LimeSegCell;
	private LimeSeg limeSeg;
	public ArrayList<Cell> allCells;
	public RoiAdjustment newCell;
	public PolygonRoi polyRoi2;
	
	private JFrame processingFrame;
	private JPanel upRightPanel;
	private JPanel middlePanel;
	private JPanel bottomRightPanel;
	private JButton btnSave;
	private JButton btnInsert;
	private JButton btnLumen;
	private JComboBox checkOverlay;
	private JSpinner cellSpinner;

	private PointRoi dotsRoi;
	private PolygonRoi polyRoi;

	// private JPanel IdPanel;

	private String initialDirectory;
	public Cell3D PostProcessCell;
	public ArrayList<Cell3D> all3dCells;

	// private JTableModel tableInf;
	// private Scrollbar sliceSelector;

	public PostProcessingWindow(ImagePlus raw_img) {
		super(raw_img, new CustomCanvas(raw_img));

		this.initialDirectory = raw_img.getOriginalFileInfo().directory;
		limeSeg = new LimeSeg();
		LimeSeg.allCells = new ArrayList<Cell>();
		LimeSegCell = new Cell();
		all3dCells = new ArrayList<Cell3D>();
		File dir = new File(this.initialDirectory.toString() + "/OutputLimeSeg");
		File[] files = dir.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.startsWith("cell_");
			}
		});
		for (File f : files) {
			String path = f.toString();
			LimeSegCell.id_Cell = path.substring(path.indexOf("_") + 1);
			OutputLimeSeg.hydrateCellT(LimeSegCell, path);
			PostProcessCell = new Cell3D(LimeSegCell.id_Cell, LimeSegCell.cellTs.get(
				0).dots);
			PostProcessCell.labelCell = Integer.parseInt(LimeSegCell.id_Cell);
			all3dCells.add(PostProcessCell);
		}

		Collections.sort(all3dCells, new Comparator<Cell3D>() {

			@Override
			public int compare(Cell3D cel1, Cell3D cel2) {
				return cel1.getID().compareTo(cel2.getID());
			}
		});

		canvas = (CustomCanvas) super.getCanvas();
		PostProcessingGland.callToolbarPolygon();

		initializeGUIItems(raw_img);

		// tableInf = tableInfo;
		overlayResult = new SegmentationOverlay();
		if (overlayResult != null) {

			if (canvas.getImageOverlay() == null) {
				canvas.clearOverlay();
				raw_img.setOverlay(overlayResult.getOverlay(0, canvas.getImage()
					.getCurrentSlice(), all3dCells, raw_img, false));
				overlayResult.setImage(raw_img);
				canvas.addOverlay(overlayResult);
				canvas.setImageOverlay(overlayResult);

			}
		}
		// removeAll();

		initGUI(raw_img);

		setEnablePanels(false);

		// threadFinished = false;

	}

	private void initGUI(ImagePlus raw_img) {

		upRightPanel.setLayout(new MigLayout());
		upRightPanel.setBorder(BorderFactory.createTitledBorder("ID Cell"));
		upRightPanel.add(cellSpinner, "wrap");
		upRightPanel.add(checkOverlay);

		middlePanel.setLayout(new MigLayout());
		middlePanel.setBorder(BorderFactory.createTitledBorder("Cell Correction"));
		middlePanel.add(btnInsert, "wrap");
		middlePanel.add(btnSave, "wrap");

		bottomRightPanel.setLayout(new MigLayout());
		bottomRightPanel.setBorder(BorderFactory.createTitledBorder(
			"Lumen Processing"));
		bottomRightPanel.add(btnLumen);

		processingFrame.setLayout(new MigLayout());
		processingFrame.add(canvas, "alignx center, span 1 5");
		processingFrame.add(upRightPanel, "wrap, gapy 10::50, aligny top");
		processingFrame.add(middlePanel, "aligny center, wrap, gapy 10::50");
		processingFrame.add(bottomRightPanel);
		processingFrame.setMinimumSize(new Dimension(1024, 1024));
		processingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		processingFrame.pack();
		processingFrame.setVisible(true);

	}

	private void initializeGUIItems(ImagePlus raw_img) {

		// Init attributes.
		newCell = new RoiAdjustment();

		processingFrame = new JFrame("PostProcessingGland");
		upRightPanel = new JPanel();
		middlePanel = new JPanel();
		bottomRightPanel = new JPanel();

		cellSpinner = new JSpinner();
		cellSpinner.setModel(new SpinnerNumberModel(1, 1, all3dCells.size(), 1));
		cellSpinner.addChangeListener(listener);

		checkOverlay = new JComboBox();
		checkOverlay.addItem("None overlay");
		checkOverlay.addItem("Cell overlay");
		checkOverlay.addItem("All overlays");
		checkOverlay.addActionListener(this);

		btnInsert = new JButton("Modify Cell");
		btnInsert.addActionListener(this);
		
		btnSave = new JButton("Save Results");
		btnLumen = new JButton("Add Lumen");

		canvas.addComponentListener(new ComponentAdapter() {

			public void componentResized(ComponentEvent ce) {
				Rectangle r = canvas.getBounds();
				canvas.setDstDimensions(r.width, r.height);
			}

		});
	}

	/*
	 * Group all the actions(non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == checkOverlay) {

			if (checkOverlay.getSelectedItem() == "All overlays") {
				canvas.setOverlay(overlayResult.getOverlay(((Integer) cellSpinner
					.getValue() - 1), canvas.getImage().getCurrentSlice(), all3dCells,
					canvas.getImage(), true));
				overlayResult.setImage(canvas.getImage());
				canvas.addOverlay(overlayResult);
				canvas.setImageOverlay(overlayResult);
			}

			else {
				canvas.clearOverlay();
				overlayResult.ov.clear();
				overlayResult.workingImP.setHideOverlay(true);

				if (checkOverlay.getSelectedItem() == "Cell overlay") {
					canvas.setOverlay(overlayResult.getOverlay(((Integer) cellSpinner
						.getValue() - 1), canvas.getImage().getCurrentSlice(), all3dCells,
						canvas.getImage(), false));
					overlayResult.setImage(canvas.getImage());
				}

				canvas.setImageOverlay(overlayResult);

			}

		}

		if (e.getSource() == btnInsert) {
			this.addROI();
			newCell.removeOverlappingRegions(all3dCells, polyRoi, canvas.getImage().getCurrentSlice(), all3dCells.get((Integer) cellSpinner.getValue() - 1).id_Cell);

			canvas.clearOverlay();
			
			if ((checkOverlay.getSelectedItem() == "All overlays")) {
				canvas.setOverlay(overlayResult.getOverlay(((Integer) cellSpinner
					.getValue() - 1), canvas.getImage().getCurrentSlice(), all3dCells,
					canvas.getImage(), true));
			}
			else {
				canvas.setOverlay(overlayResult.getOverlay(((Integer) cellSpinner
					.getValue() - 1), canvas.getImage().getCurrentSlice(), all3dCells,
					canvas.getImage(), false));
			}
			overlayResult.setImage(canvas.getImage());
			canvas.addOverlay(overlayResult);
			canvas.setImageOverlay(overlayResult);

		}
		
		if (e.getSource() == btnSave) {
			
			
		}

	}

	ChangeListener listener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			// TODO Auto-generated method stub
			canvas.clearOverlay();

			if (checkOverlay.getSelectedItem() == "All overlays") {
				canvas.setOverlay(overlayResult.getOverlay(((Integer) cellSpinner
					.getValue() - 1), canvas.getImage().getCurrentSlice(), all3dCells,
					canvas.getImage(), true));
			}
			else {

				if (checkOverlay.getSelectedItem() == "Cell overlay") {
					canvas.setOverlay(overlayResult.getOverlay(((Integer) cellSpinner
						.getValue() - 1), canvas.getImage().getCurrentSlice(), all3dCells,
						canvas.getImage(), false));
				}
				else {
					canvas.clearOverlay();
				}

			}

			overlayResult.setImage(canvas.getImage());
			canvas.addOverlay(overlayResult);
			canvas.setImageOverlay(overlayResult);
			canvas.setImageUpdated();

		}
	};

	/**
	 * Enable/disable all the panels in the window
	 * 
	 * @param enabled true it will enable panels, false disable all panels
	 */
	protected void setEnablePanels(boolean enabled) {
		btnSave.setEnabled(true);
		btnInsert.setEnabled(true);
	}

	/**
	 * Disable all the action buttons
	 */
	protected void disableActionButtons() {
		btnSave.setEnabled(false);
		btnInsert.setEnabled(false);
	}

	/**
	 * Enable all the action buttons
	 */
	protected void enableActionButtons() {
		btnSave.setEnabled(true);
		btnInsert.setEnabled(true);
	}

	/**
	 * Add the painted Roi to the roiManager
	 */
	public void addROI() {
		Roi r = this.getImagePlus().getRoi();
		if (r != null) {
			int[] xPoly = ((PolygonRoi) r).getXCoordinates();
			int[] yPoly = ((PolygonRoi) r).getYCoordinates();
			int i = 0;
				while (xPoly[i] != 0  | xPoly[i+1] != 0) {
					i++;
				}
				polyRoi = new PolygonRoi(Arrays.copyOfRange(xPoly, 0, i-1), Arrays.copyOfRange(yPoly, 0, i-1), i-1, 2);
				polyRoi.setLocation(r.getXBase(), r.getYBase());
		}
		this.getImagePlus().deleteRoi();
		
	}
	
	public void addPoints(ArrayList <DotN> newDots) {
		int yinc = 10; 						//the increment in the y axis between each point's y coordinate
		int xinc = 10; 
		for (int nSides=0; nSides < newDots.size(); nSides++) {
			float ylength = newDots.get(nSides).pos.y - newDots.get(nSides+1).pos.y; 						//finds the length of the line in the y axis by taking the first y coordinate from the second
			float xlength = newDots.get(nSides).pos.x - newDots.get(nSides+1).pos.x;						//finds the length in the x axis	the same way
			for (int j=0; j<= xlength/xinc; j++) { 						//a for loop to plot points, repeats for the total number of segments + 1 (as 0 is counted)
				float xcoord = newDots.get(nSides).pos.x - (xinc*j); 				//calculates the x coordinate of the point to be plotted by subtracting the increment value (multiplied by the current segment number defined by 'j') from the first x coord of the line
				float ycoord = newDots.get(nSides).pos.y - (yinc*j); 				//calculates the y coordinate as above from the top-most point on the line
				DotN dot = new DotN();
				dot.pos.x = xcoord; 				
				dot.pos.y = ycoord;
				newDots.add(dot);
			}
		}
		
	}

}
