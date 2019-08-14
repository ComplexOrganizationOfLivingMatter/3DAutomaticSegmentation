
package PostProcessingGland.GUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.primitives.Ints;

import PostProcessingGland.PostProcessingGland;
import PostProcessingGland.Elements.Cell3D;
import PostProcessingGland.Elements.RoiAdjustment;
import epigraph.GUI.CustomElements.CustomCanvas;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.io.IOXmlPlyLimeSeg;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Overlay;
import net.miginfocom.swing.MigLayout;

public class PostProcessingWindow extends ImageWindow implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IOXmlPlyLimeSeg OutputLimeSeg;
	private CustomCanvas canvas;
	private Cell LimeSegCell;
	private LimeSeg limeSeg;
	public ArrayList<Cell> allCells;
	public RoiAdjustment newCell;
	public PolygonRoi polyRoi2;

	private JFrame processingFrame;
	private JPanel upRightPanel;
	private JPanel middlePanel;
	private JPanel bottomRightPanel;
	private JPanel rightPanel;
	private JPanel leftPanel;
	private JButton btnSave;
	private JButton btnInsert;
	private JButton btnLumen;
	private JComboBox<String> checkOverlay;
	private JComboBox<String> checkLumen;
	private JSpinner cellSpinner;
	private Scrollbar sliceSelector;

	private PolygonRoi polyRoi;

	private String initialDirectory;
	public Cell3D PostProcessCell;
	public ArrayList<Cell3D> all3dCells;
	public Roi[] lumenDots;
	public float zScale;

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
			PostProcessCell = new Cell3D(LimeSegCell.id_Cell, LimeSegCell.cellTs.get(0).dots);
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

		// String scale_zStack = JOptionPane.showInputDialog("Z_Scale: ");
		// zScale = Float.parseFloat(scale_zStack);

		sliceSelector = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, (imp.getStackSize() + 1));
		sliceSelector.setVisible(true);

		lumenDots = new Roi[imp.getStackSize() + 1];

		initializeGUIItems(raw_img);
		raw_img.setOverlay(addOverlay(0, canvas.getImage().getCurrentSlice(), all3dCells, raw_img, false, lumenDots));
		initGUI(raw_img);

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
		bottomRightPanel.setBorder(BorderFactory.createTitledBorder("Lumen Processing"));
		bottomRightPanel.add(btnLumen, "wrap");
		bottomRightPanel.add(checkLumen);

		rightPanel.setLayout(new MigLayout());
		rightPanel.add(upRightPanel, "wrap, gapy 10::50, aligny top");
		rightPanel.add(middlePanel, "aligny center, wrap, gapy 10::50");
		rightPanel.add(bottomRightPanel);

		leftPanel.setLayout(new MigLayout());
		canvas.setMaximumSize(new Dimension(1024, 1024));
		canvas.setMinimumSize(new Dimension(500, 500));
		Color newColor = new Color(200, 200, 255);
		sliceSelector.setBackground(newColor);
		leftPanel.add(canvas, "wrap");
		leftPanel.add(sliceSelector, "growx");
		;

		processingFrame.setLayout(new MigLayout());
		processingFrame.add(leftPanel);
		processingFrame.add(rightPanel, "east");
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
		rightPanel = new JPanel();
		leftPanel = new JPanel();

		cellSpinner = new JSpinner();
		cellSpinner.setModel(new SpinnerNumberModel(1, 1, all3dCells.size(), 1));
		cellSpinner.addChangeListener(listener);

		checkOverlay = new JComboBox<String>();
		checkOverlay.addItem("None overlay");
		checkOverlay.addItem("Cell overlay");
		checkOverlay.addItem("All overlays");
		checkOverlay.setSelectedIndex(1);
		checkOverlay.addActionListener(this);

		checkLumen = new JComboBox<String>();
		checkLumen.addItem("Without lumen");
		checkLumen.addItem("Show lumen");
		checkLumen.setSelectedIndex(1);
		checkLumen.addActionListener(this);

		btnInsert = new JButton("Modify Cell");
		btnInsert.addActionListener(this);

		btnSave = new JButton("Save Results");
		btnSave.addActionListener(this);

		btnLumen = new JButton("Add Lumen");
		btnLumen.addActionListener(this);

		canvas.addComponentListener(new ComponentAdapter() {

			public void componentResized(ComponentEvent ce) {
				Rectangle r = canvas.getBounds();
				canvas.setDstDimensions(r.width, r.height);
			}

		});

		sliceSelector.addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				int z = sliceSelector.getValue();
				imp.setSlice(z);
				updateOverlay();

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
			updateOverlay();
		}

		if (e.getSource() == checkLumen) {
			updateOverlay();
		}

		if (e.getSource() == btnInsert) {
			this.addROI();
			newCell.removeOverlappingRegions(all3dCells, polyRoi, canvas.getImage().getCurrentSlice(),
					all3dCells.get((Integer) cellSpinner.getValue() - 1).id_Cell);

			checkOverlay.setSelectedIndex(1);
			updateOverlay();
		}

		if (e.getSource() == btnSave) {
			this.savePlyFile(all3dCells, initialDirectory);
		}

		if (e.getSource() == btnLumen) {
			File dirLumen = new File(this.initialDirectory.toString() + "/SegmentedLumen");
			File[] filesLumen = dirLumen.listFiles(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					return name.startsWith("SegmentedLumen");
				}
			});
			int zIndex = 0;

			try {
				for (File f : filesLumen) {
					FileInputStream lumen = new FileInputStream(f);
					BufferedImage lumen_img = ImageIO.read(lumen);
					ImagePlus lumenImg = new ImagePlus("Lumen", lumen_img);

					ArrayList<Roi> fileLumenDots = new ArrayList<Roi>();

					for (int x = 0; x < lumenImg.getProcessor().getWidth(); x++) {
						for (int y = 0; y < lumenImg.getProcessor().getHeight(); y++) {
							if (lumenImg.getProcessor().getPixel(x, y) == 0) {
								PointRoi dot = new PointRoi(x, y);
								fileLumenDots.add(dot);
							}
						}
					}

					if (fileLumenDots.size() != 0) {
						Roi[] sliceDots = new Roi[fileLumenDots.size()];
						sliceDots = fileLumenDots.toArray(sliceDots);
						/*
						float[] xPoints = new float[sliceDots.length];
						float[] yPoints = new float[sliceDots.length];
						for (int i = 0; i < yPoints.length; i++) {
							xPoints[i] = (float) (sliceDots[i].getXBase() + sliceDots[i].getFloatWidth());
							yPoints[i] = (float) (sliceDots[i].getYBase() + sliceDots[i].getFloatHeight());
						}

						PointRoi poly = new PointRoi(xPoints, yPoints);
						//Color colorCurrentCell = new Color(255, 255, 255);
						//poly.setStrokeColor(colorCurrentCell);
						*/
						lumenDots[zIndex] = newCell.getConcaveHull(sliceDots);
						
						Color colorCurrentCell = new Color(255, 255, 255);
						lumenDots[zIndex].setStrokeColor(colorCurrentCell);
					}

					zIndex++;
				}

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//removeCellLumenOverlap();
			updateOverlay();
		}

	}

	/**
	 * 
	 */
	private void updateOverlay() {

		canvas.clearOverlay();
		canvas.getImage().getOverlay().clear();

		if (checkOverlay.getSelectedItem() == "All overlays") {
			Overlay newOverlay = addOverlay(((Integer) cellSpinner.getValue() - 1), canvas.getImage().getCurrentSlice(),
					all3dCells, canvas.getImage(), true, lumenDots);
			canvas.getImage().setOverlay(newOverlay);
		} else if (checkOverlay.getSelectedItem() == "Cell overlay") {
			Overlay newOverlay = addOverlay(((Integer) cellSpinner.getValue() - 1), canvas.getImage().getCurrentSlice(),
					all3dCells, canvas.getImage(), false, lumenDots);
			canvas.getImage().setOverlay(newOverlay);
		}

		canvas.setImageUpdated();
		canvas.repaint();
	}

	ChangeListener listener = new ChangeListener() {

		@Override
		public void stateChanged(ChangeEvent e) {
			updateOverlay();
		}
	};

	/**
	 * Add the painted Roi to the roiManager
	 */
	public void addROI() {
		Roi r = this.getImagePlus().getRoi();
		if (r != null) {
			int[] xPoly = ((PolygonRoi) r).getXCoordinates();
			int[] yPoly = ((PolygonRoi) r).getYCoordinates();
			int i = 0;
			while (xPoly[i] != 0 | xPoly[i + 1] != 0) {
				i++;
			}
			polyRoi = new PolygonRoi(Arrays.copyOfRange(xPoly, 0, i - 1), Arrays.copyOfRange(yPoly, 0, i - 1), i - 1,
					2);
			polyRoi.setLocation(r.getXBase(), r.getYBase());
		}
		this.getImagePlus().deleteRoi();

	}

	/**
	 * 
	 * @param allCells
	 * @param path_in
	 */

	static public void savePlyFile(ArrayList<Cell3D> allCells, String path_in) {
		if (!path_in.endsWith(File.separator)) {
			path_in = path_in + File.separator;
		}
		String path = path_in + "/newOutputLimeSeg";
		File dir = new File(path);
		if (!dir.isDirectory()) {
			System.out.println("New folder created");
			dir.mkdir();
		}
		// By default removes all files in the folder
		// But ask for confirmation if the folder is not empty...
		if (dir.listFiles().length != 0) {
			System.out.println("Saving will remove the content of the folder " + path + " that contains "
					+ dir.listFiles().length + " files and folders.");
		}

		// create the optimizer parameter element
		allCells.forEach(c -> {

			// Now writes all ply files for CellT object

			CellT cellt = new CellT(c, 1);
			cellt.dots = c.dotsList;
			String pathCell = path + File.separator + "cell_" + c.id_Cell + File.separator;
			File dirCell = new File(pathCell);
			// attempt to create the directory here
			if (dirCell.mkdir()) {
				IOXmlPlyLimeSeg.saveCellTAsPly(cellt, pathCell + "T_" + 1 + ".ply");
			} else {
				if (dirCell.exists()) {
					IOXmlPlyLimeSeg.saveCellTAsPly(cellt, pathCell + "T_" + 1 + ".ply");
				}

			}
		});
	}

	/**
	 * 
	 * @param id
	 * @param frame
	 * @param cells
	 * @param workingImP
	 * @param allOverlays
	 * @return
	 */
	public Overlay addOverlay(Integer id, Integer frame, ArrayList<Cell3D> cells, ImagePlus workingImP,
			boolean allOverlays, Roi[] lumen) {
		Overlay ov = new Overlay();
		if (workingImP != null) {
			PointRoi roi;
			if (allOverlays) {

				for (int nCell = 0; nCell < cells.size(); nCell++) {
					ArrayList<DotN> dots = cells.get(nCell).getCell3DAt(frame);
					Iterator<DotN> i = dots.iterator();
					while (i.hasNext()) {
						DotN loadedDots = i.next();
						roi = new PointRoi(loadedDots.pos.x, loadedDots.pos.y);
						Color colorCurrentCell = new Color(0, 0, 255);
						roi.setStrokeColor(colorCurrentCell);
						if (nCell == id) {
							colorCurrentCell = new Color(255, 0, 0);
							roi.setStrokeColor(colorCurrentCell);
						}
						ov.addElement(roi);
					}
				}
			} else {
				ArrayList<DotN> dots = cells.get(id).getCell3DAt(frame);
				Iterator<DotN> i = dots.iterator();
				while (i.hasNext()) {
					DotN loadedDots = i.next();
					roi = new PointRoi(loadedDots.pos.x, loadedDots.pos.y);
					Color colorCurrentCell = new Color(255, 0, 0);
					roi.setStrokeColor(colorCurrentCell);
					ov.addElement(roi);
				}
			}

			if (lumen[lumen.length / 2] != null & checkLumen.getSelectedItem() == "Show lumen") {
				ov.addElement(lumen[frame - 1]);
			}

		}
		return ov;
	}

	public void removeCellLumenOverlap() {
		for (int nFrame = 1; nFrame < imp.getStackSize(); nFrame++) {
			for (int nCell = 1; nCell < all3dCells.size(); nCell++) {
				if (lumenDots[nFrame] != null) {

					float[] xCell = all3dCells.get(nCell).getCoordinate("x", all3dCells.get(nCell).getCell3DAt(nFrame));
					float[] yCell = all3dCells.get(nCell).getCoordinate("y", all3dCells.get(nCell).getCell3DAt(nFrame));

					PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 2);
					ShapeRoi r = new ShapeRoi(overlappingCell);
					ShapeRoi lum = new ShapeRoi(lumenDots[nFrame]);
					r.not(lum);
					Roi[] overRoi = r.getRois();

					PolygonRoi poly = newCell.getConcaveHull(overRoi);

					// Convert the PolygonRoi in Dots and integrate with the
					// dots of
					// the other frames.
					// Later, replace the selected cell by the cell with the new
					// region
					ArrayList<DotN> dotsNewRegion = newCell.setOverlapRegion(nFrame, poly, r);
					ArrayList<DotN> integratedDots = newCell.integrateNewRegion(dotsNewRegion,
							all3dCells.get(nCell).dotsList, nFrame);

					Cell3D newCell = new Cell3D(all3dCells.get(nCell).id_Cell, integratedDots);
					all3dCells.set(nCell, newCell);

				}
			}
			int pepe = 0;
			pepe++;
		}
	}

}
