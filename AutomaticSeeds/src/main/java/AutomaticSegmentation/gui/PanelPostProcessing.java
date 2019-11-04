/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import AutomaticSegmentation.MainAutomatic3DSegmentation;
import AutomaticSegmentation.elements.Cell3D;
import AutomaticSegmentation.elements.RoiAdjustment;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.io.IOXmlPlyLimeSeg;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.OpenDialog;
import ij.plugin.FolderOpener;
import ij.process.ImageProcessor;

/**
 * @author Victor Hugo Arriaga, Antonio Tagua, Pablo Vicente-Munuera
 *
 */
public class PanelPostProcessing extends JPanel implements ActionListener, ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static double THRESHOLD = 5;

	private RoiAdjustment newCell;
	private Cell LimeSegCell;
	private JFileChooser fileChooser;
	private Cell3D PostProcessCell;
	private ArrayList<Cell3D> all3dCells;
	private PolygonRoi polyRoi;
	private PolygonRoi[][] lumenDots;
	private ImagePlus cellOutlineChannel;

	public JButton btPostLimeSeg;
	private JButton btnPostSave;
	private JButton btnInsert;
	private JButton btnLumen;
	private JButton btn3DDisplay;
	private JLabel cellsLabel;
	private JComboBox<String> checkOverlay;
	private JComboBox<String> checkLumen;
	private JSpinner cellSpinner;

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

		newCell = new RoiAdjustment();
		LimeSeg.allCells = new ArrayList<Cell>();
		LimeSegCell = new Cell();
		all3dCells = new ArrayList<Cell3D>();
		
		fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		initPostLimeSegPanel();
	}

	/*-------------------- FUNCTIONS ----------------------*/
	/*
	 * Group all the actions(non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void stateChanged(ChangeEvent e) {

		if (e.getSource() == cellSpinner) {
			updateOverlay();
		}
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == btPostLimeSeg) {
			
			if (all3dCells.isEmpty()) {
				btPostLimeSeg.setEnabled(false);
				setFileChooserProperties("Select the output LimeSeg folder");
				if (fileChooser.showOpenDialog(this) == fileChooser.APPROVE_OPTION) {
				
				this.cellOutlineChannel.show();
						ExecutorService executor1 = Executors.newSingleThreadExecutor();
						executor1.submit(() -> {
							openPlyFiles();
							MainAutomatic3DSegmentation.callToolbarPolygon();
							lumenDots = new PolygonRoi[cellOutlineChannel.getStackSize() + 1][2];
							removeCellOverlap();
							removeCellLumenOverlap();
							cellOutlineChannel.setOverlay(addOverlay(0, cellOutlineChannel.getCurrentSlice(),
									all3dCells, cellOutlineChannel, false, lumenDots));
							cellSpinner.setModel(new SpinnerNumberModel(1, 1, all3dCells.size(), 1));
							checkOverlay.addActionListener(this);
							checkLumen.addActionListener(this);
							btnInsert.addActionListener(this);
							btnPostSave.addActionListener(this);
							btnLumen.addActionListener(this);
							btn3DDisplay.addActionListener(this);
							cellSpinner.addChangeListener(this);
							this.setEnablePanel(true);
							checkLumen.setEnabled(false);
							executor1.shutdown();
						});
					} else{
						IJ.log("Not output LimeSeg folder selected");
					}
				
			}

		}

		if (e.getSource() == checkOverlay) {
			updateOverlay();
		}

		if (e.getSource() == checkLumen) {
			updateOverlay();
		}

		if (e.getSource() == btnInsert) {
			addROI();
			// Check if polyRoi is different to null, if is do the modify cell
			if (polyRoi != null) {
				all3dCells = newCell.removeOverlappingRegions(all3dCells, polyRoi, cellOutlineChannel.getCurrentSlice(),
						all3dCells.get((Integer) cellSpinner.getValue() - 1).id_Cell, lumenDots);
				checkOverlay.setSelectedIndex(1);
				updateOverlay();
				// After modify cell return poly to null, clean the roi
				polyRoi = null;
			}
			// If polyRoi is null show a message to prevent errors
			else {
				JOptionPane.showMessageDialog(getParent(), "You must select a new Region.");
			}

		}

		if (e.getSource() == btnPostSave) {
			savePlyFiles();
			// After saved the plyFiles show a message to inform the user
			JOptionPane.showMessageDialog(getParent(), "Saved results.");
		}

		if (e.getSource() == btnLumen) {
			ExecutorService executor1 = Executors.newSingleThreadExecutor();
			executor1.submit(() -> {
			// read the lumen
			loadLumen();
			// remove the overlaps cells
			removeCellLumenOverlap();
			updateOverlay();
			executor1.shutdown();
			});
		}

		if (e.getSource() == btn3DDisplay) {
			String path_in = fileChooser.getSelectedFile().toString();
			LimeSeg.loadStateFromXmlPly(path_in);
			LimeSeg.make3DViewVisible();
			LimeSeg.putAllCellsTo3DDisplay();
			System.out.println("READY");
		}
	}

	/**
	 * 
	 */
	private void setFileChooserProperties(String title) {
		fileChooser.setCurrentDirectory(new File(cellOutlineChannel.getOriginalFileInfo().directory));
		fileChooser.setDialogTitle(title);
	}

	/*-------------------- GETTERS AND SETTERS ----------------------*/
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
		cellOutlineChannel.addImageListener(new ImageListener() {

			@Override
			public void imageUpdated(ImagePlus imp) {
				// TODO Auto-generated method stub
				updateOverlay();
			}

			@Override
			public void imageOpened(ImagePlus imp) {
				// TODO Auto-generated method stub

			}

			@Override
			public void imageClosed(ImagePlus imp) {
				// TODO Auto-generated method stub
				setEnablePanel(false);
				btPostLimeSeg.setEnabled(true);
			}
		});
	}

	/*-------------------- METHODS ----------------------*/

	/**
	 * 
	 */
	private void initPostLimeSegPanel() {

		// Init GUI elements
		cellSpinner = new JSpinner();
		cellSpinner.setMinimumSize(new Dimension(125, 10));
		cellsLabel = new JLabel("ID Cells:");

		checkOverlay = new JComboBox<String>();
		checkOverlay.addItem("None overlay");
		checkOverlay.addItem("Cell overlay");
		checkOverlay.addItem("All overlays");
		checkOverlay.setSelectedIndex(2);
		checkOverlay.setMinimumSize(new Dimension(125, 20));

		checkLumen = new JComboBox<String>();
		checkLumen.addItem("Without lumen");
		checkLumen.addItem("Show lumen");
		checkLumen.setSelectedIndex(0);
		checkLumen.setMinimumSize(new Dimension(125, 20));

		btnInsert = new JButton("Modify Cell");
		btnInsert.setMinimumSize(new Dimension(150, 20));
		btnPostSave = new JButton("Save Results");
		btnPostSave.setMinimumSize(new Dimension(150, 20));
		btnLumen = new JButton("Load Lumen");
		btnLumen.setMinimumSize(new Dimension(150, 20));
		btn3DDisplay = new JButton("Show 3D Cell");
		btn3DDisplay.setMinimumSize(new Dimension(150, 20));
		btPostLimeSeg = new JButton("Run PostProcessing");
		btPostLimeSeg.addActionListener(this);

		// Add components
		this.add(btn3DDisplay, "align center");
		this.add(cellsLabel, "align right");
		this.add(cellSpinner, "align center, wrap");
		this.add(btPostLimeSeg, "align center");
		this.add(checkOverlay, "align center");
		this.add(checkLumen, "wrap, align center");
		this.add(btnInsert, "align center");
		this.add(btnPostSave, "align center");
		this.add(btnLumen, "align center");

	}

	/**
	 * 
	 */
	public void openPlyFiles() {
		File dir = new File(fileChooser.getSelectedFile().toString());
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("cell_");
			}
		});
		for (File f : files) {
			String path = f.toString();
			LimeSegCell.id_Cell = path.substring(path.indexOf("_") + 1);
			IOXmlPlyLimeSeg.hydrateCellT(LimeSegCell, path);
			Cell3D PostProcessCellCopy = new Cell3D(LimeSegCell.id_Cell, LimeSegCell.cellTs.get(0).dots);
			PostProcessCell = new Cell3D(LimeSegCell.id_Cell, LimeSegCell.cellTs.get(0).dots);
			PostProcessCell.clearCell();
			for (int i = 0; i < cellOutlineChannel.getStackSize(); i++) {
				if (PostProcessCellCopy.getCell3DAt(i).size() != 0) {
					PostProcessCell.addDotsList(processLimeSegOutput(PostProcessCellCopy.getCell3DAt(i), i));
				}

			}

			PostProcessCell.labelCell = Integer.parseInt(LimeSegCell.id_Cell);
			all3dCells.add(PostProcessCell);
		}

		Collections.sort(all3dCells, new Comparator<Cell3D>() {

			@Override
			public int compare(Cell3D cel1, Cell3D cel2) {
				return cel1.getID().compareTo(cel2.getID());
			}
		});
	}

	/**
	 * Add the painted Roi to the roiManager
	 */
	public void addROI() {
		Roi r = cellOutlineChannel.getRoi();
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
		cellOutlineChannel.deleteRoi();

	}

	/**
	 * 
	 * @param id
	 * @param frame
	 * @param cells
	 * @param workingImP
	 * @param allOverlays
	 * @param lumen
	 * @return
	 */
	public Overlay addOverlay(Integer id, Integer frame, ArrayList<Cell3D> cells, ImagePlus workingImP,
			boolean allOverlays, PolygonRoi[][] lumen) {
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
				ov.addElement(lumen[frame - 1][0]);
				if (lumen[frame - 1][1] != null)
					ov.addElement(lumen[frame - 1][1]);
			}
			// TODO: change this part of the code

		}
		return ov;
	}

	public ArrayList<DotN> processLimeSegOutput(ArrayList<DotN> dots, int frame) {

		int[] xPoints = new int[dots.size()];
		int[] yPoints = new int[dots.size()];

		for (int i = 0; i < yPoints.length; i++) {
			xPoints[i] = (int) dots.get(i).pos.x;
			yPoints[i] = (int) dots.get(i).pos.y;
		}
		PolygonRoi PrePolygon = new PolygonRoi(xPoints, yPoints, xPoints.length, 2);
		// order the dots according the nearest dots
		PolygonRoi prePolygon = newCell.getOrderDots(PrePolygon);
		// create a Roi with the polygon from orderDots
		Roi[] allRoi = newCell.getRois(prePolygon.getXCoordinates(), prePolygon.getYCoordinates(), prePolygon);
		// Calculate the boarder with concave hull
		PolygonRoi poly = newCell.getConcaveHull(allRoi, THRESHOLD);
		// Full fill the border with dots
		PolygonRoi polygon = new PolygonRoi(poly.getInterpolatedPolygon(1, false), 2);

		Roi[] allRois = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
		ArrayList<DotN> newDots = newCell.RoisToDots(frame, allRois);

		return newDots;
	}

	/**
	 * 
	 */
	public void removeCellLumenOverlap() {
		for (int nFrame = 1; nFrame < cellOutlineChannel.getStackSize() + 1; nFrame++) {
			for (int nCell = 0; nCell < all3dCells.size(); nCell++) {
				// Check if the frame have lumen and the cell is not empty
				if (lumenDots[nFrame - 1] != null & all3dCells.get(nCell).getCell3DAt(nFrame).size() > 0) {
					// Get the cell points
					float[] xCell = all3dCells.get(nCell).getCoordinate("x", all3dCells.get(nCell).getCell3DAt(nFrame));
					float[] yCell = all3dCells.get(nCell).getCoordinate("y", all3dCells.get(nCell).getCell3DAt(nFrame));
					// Points to polygon to shape
					PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 6);
					ShapeRoi r = new ShapeRoi(overlappingCell);
					ShapeRoi s = new ShapeRoi(overlappingCell);
					ShapeRoi s2 = new ShapeRoi(overlappingCell);
					// Check if lumen has two polygon
					if (lumenDots[nFrame - 1][1] != null) {
						// Transform the lumens to polygons and shapes
						java.awt.Polygon l = lumenDots[nFrame - 1][0].getPolygon();
						ShapeRoi lum = new ShapeRoi(l);
						// Verify if the lumen cross with cell
						s.and(lum);
						java.awt.Polygon l2 = lumenDots[nFrame - 1][1].getPolygon();
						ShapeRoi lum2 = new ShapeRoi(l2);
						s2.and(lum2);
						// If lumen cross with cell width or height must be
						// different to 0, if is 0 go to other cell
						if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0) {
							// use not function to get all the points out of the
							// lumen and save in polygon
							PolygonRoi polygon = new PolygonRoi(r.not(lum).getContainedFloatPoints(), 6);

							Roi[] overRoi = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(),
									polygon);
							// get the border of polygon without lumen parts
							// with concavehull function
							PolygonRoi poly = newCell.getConcaveHull(overRoi, 1);

							// Convert the PolygonRoi in Dots and integrate with
							// the dots of
							// the other frames.
							// Later, replace the selected cell by the cell with
							// the new
							// region
							ArrayList<DotN> dotsNewRegion = newCell.setNewRegion(nFrame, poly);
							ArrayList<DotN> integratedDots = newCell.integrateNewRegion(dotsNewRegion,
									all3dCells.get(nCell).dotsList, nFrame);

							Cell3D newCell = new Cell3D(all3dCells.get(nCell).id_Cell, integratedDots);
							all3dCells.set(nCell, newCell);

						}
						// do the same for the second polygon
						if (s2.getFloatWidth() != 0 | s2.getFloatHeight() != 0) {
							PolygonRoi polygon2 = new PolygonRoi(r.not(lum2).getContainedFloatPoints(), 6);

							Roi[] overRoi2 = newCell.getRois(polygon2.getXCoordinates(), polygon2.getYCoordinates(),
									polygon2);

							PolygonRoi poly2 = newCell.getConcaveHull(overRoi2, 1);

							ArrayList<DotN> dotsNewRegion2 = newCell.setNewRegion(nFrame, poly2);
							ArrayList<DotN> integratedDots2 = newCell.integrateNewRegion(dotsNewRegion2,
									all3dCells.get(nCell).dotsList, nFrame);

							Cell3D newCell2 = new Cell3D(all3dCells.get(nCell).id_Cell, integratedDots2);
							all3dCells.set(nCell, newCell2);
						}

					}
					// if lumen only have one polygon do the same as before but
					// only once
					else if (lumenDots[nFrame - 1][0] != null) {
						java.awt.Polygon l1 = lumenDots[nFrame - 1][0].getPolygon();
						ShapeRoi lum1 = new ShapeRoi(l1);
						s.and(lum1);

						if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0) {
							PolygonRoi polygon = new PolygonRoi(r.not(lum1).getContainedFloatPoints(), 6);

							Roi[] overRoi = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(),
									polygon);

							PolygonRoi poly = newCell.getConcaveHull(overRoi, 1);

							ArrayList<DotN> dotsNewRegion = newCell.setNewRegion(nFrame, poly);

							ArrayList<DotN> integratedDots = newCell.integrateNewRegion(dotsNewRegion,
									all3dCells.get(nCell).dotsList, nFrame);

							Cell3D newCell = new Cell3D(all3dCells.get(nCell).id_Cell, integratedDots);
							all3dCells.set(nCell, newCell);

						}
					}
				}

			}
		}
	}

	/**
	 * 
	 */
	public void removeCellOverlap() {
		for (int nFrame = 1; nFrame < cellOutlineChannel.getStackSize() + 1; nFrame++) {
			for (int nCell = 0; nCell < all3dCells.size(); nCell++) {
				// Check if the cell is not empty
				if (all3dCells.get(nCell).getCell3DAt(nFrame).size() > 0) {
					// Get the cell points
					float[] xCell = all3dCells.get(nCell).getCoordinate("x", all3dCells.get(nCell).getCell3DAt(nFrame));
					float[] yCell = all3dCells.get(nCell).getCoordinate("y", all3dCells.get(nCell).getCell3DAt(nFrame));
					// Points to polygon to shape
					PolygonRoi currentCell = new PolygonRoi(xCell, yCell, 6);
					ShapeRoi s = new ShapeRoi(currentCell);
					ShapeRoi r = new ShapeRoi(currentCell);

					for (int nC = 0; nC < all3dCells.size(); nC++) {
						// if the cell is not empty in the frame do the
						// calculation
						if (all3dCells.get(nC).getCell3DAt(nFrame).size() > 0) {
							// get the x,y points of the cell
							float[] xC = all3dCells.get(nC).getCoordinate("x", all3dCells.get(nC).getCell3DAt(nFrame));
							float[] yC = all3dCells.get(nC).getCoordinate("y", all3dCells.get(nC).getCell3DAt(nFrame));

							PolygonRoi overlappingCell = new PolygonRoi(xC, yC, 6);
							ShapeRoi sNewPolygon = new ShapeRoi(overlappingCell);
							ShapeRoi sOverlappingCell = new ShapeRoi(overlappingCell);
							ShapeRoi overlappingZone = new ShapeRoi(sNewPolygon.and(s));

							if ((overlappingZone.getFloatWidth() != 0 | overlappingZone.getFloatHeight() != 0)
									& all3dCells.get(nC).id_Cell != all3dCells.get(nCell).id_Cell) {
								PolygonRoi polygon = new PolygonRoi(sOverlappingCell.not(r).getContainedFloatPoints(),
										6);

								Roi[] overRoi = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(),
										polygon);
								PolygonRoi poly = newCell.getConcaveHull(overRoi, 1);
								ArrayList<DotN> dotsNewRegion = newCell.setNewRegion(nFrame, poly);
								ArrayList<DotN> integratedDots = newCell.integrateNewRegion(dotsNewRegion,
										all3dCells.get(nC).dotsList, nFrame);

								Cell3D newCell = new Cell3D(all3dCells.get(nC).id_Cell, integratedDots);
								all3dCells.set(nC, newCell);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 */
	public void loadLumen() {
		try {
			setFileChooserProperties("Select the segmented lumen folder");
			if (fileChooser.showOpenDialog(this) == fileChooser.APPROVE_OPTION) {
				File dirLumen = fileChooser.getSelectedFile();
				File[] filesLumen = dirLumen.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith("");
					}
				});
				int zIndex = 0;
				// read the Lumen Directory
				for (File f : filesLumen) {
					FileInputStream lumen = new FileInputStream(f);
					BufferedImage lumen_img = ImageIO.read(lumen);
					ImagePlus lumenImg = new ImagePlus("Lumen", lumen_img);
					// Get the image and find the Edge of the lumen
					ImageProcessor lumImg = lumenImg.getProcessor();
					lumImg.findEdges();
					// Create a new image with lumen edges (edges are white)
					ImagePlus lumEd = new ImagePlus("LumenEdge", lumImg);

					ArrayList<Roi> fileLumenDots = new ArrayList<Roi>();
					// Read all the pixel and save the white (65535) pixels
					for (int y = 0; y < lumEd.getProcessor().getWidth(); y++) {
						for (int x = 0; x < lumEd.getProcessor().getHeight(); x++) {
							if (lumEd.getProcessor().getPixel(x, y) == 65535) {
								PointRoi dot = new PointRoi(x, y);
								fileLumenDots.add(dot);
							}
						}
					}

					if (fileLumenDots.size() != 0) {
						Roi[] sliceDots = new Roi[fileLumenDots.size()];
						sliceDots = fileLumenDots.toArray(sliceDots);
						// get the x, y points from the array
						float[] xPoints = new float[sliceDots.length];
						float[] yPoints = new float[sliceDots.length];
						int dis = sliceDots.length;
						float disEu[] = new float[dis];

						for (int i = 0; i < sliceDots.length; i++) {
							xPoints[i] = (float) sliceDots[i].getXBase();
							yPoints[i] = (float) sliceDots[i].getYBase();
						}
						// calculate the distance between each point
						for (int i = 0; i < dis; i++) {
							if (i == dis - 1) {
								disEu[i] = newCell.distEu(xPoints[i], xPoints[i], yPoints[i], yPoints[i]);
							} else {
								disEu[i] = newCell.distEu(xPoints[i + 1], xPoints[i], yPoints[i + 1], yPoints[i]);
							}
						}

						int pos = dis;
						int rest = 0;
						// check if the distance between is less than 100 to split
						// in two polygons
						for (int i = 0; i < dis; i++) {
							if (disEu[i] > 100) {
								// if distance more than 100 save the position and
								// the qty of points after the position
								pos = i;
								rest = dis - i;
							}
						}
						// if the qty is different to 0 split the roi in two
						// polygons
						if (rest != 0) {
							// save the points after position, this points are the
							// 2nd polygon
							float x[] = new float[rest - 1];
							float y[] = new float[rest - 1];
							int j = 0;
							for (int i = pos + 1; i < dis; i++) {
								x[j] = xPoints[i];
								y[j] = yPoints[i];
								j++;
							}
							// create polygon one with the point until pos
							PolygonRoi poly = new PolygonRoi(xPoints, yPoints, pos, 6);
							// create polygon two with the rest of the points
							PolygonRoi poly2 = new PolygonRoi(x, y, 6);
							// fill the second polygon with points to get the
							// correct border
							PolygonRoi postpol = new PolygonRoi(poly2.getInterpolatedPolygon(2, false), 6);

							Roi[] roiDots = newCell.getRois(poly.getXCoordinates(), poly.getYCoordinates(), poly);
							Roi[] roiDots2 = newCell.getRois(postpol.getXCoordinates(), postpol.getYCoordinates(),
									postpol);
							// find the border with ConcavHull
							PolygonRoi lum = newCell.getConcaveHull(roiDots, THRESHOLD);
							PolygonRoi lum2 = newCell.getConcaveHull(roiDots2, THRESHOLD);
							lumenDots[zIndex][0] = lum; // save the border in matrix
														// position 0
							lumenDots[zIndex][1] = lum2; // save the border in
															// position 1
						} else {
							// if is only one polygon, get the polygon, border and
							// save
							PolygonRoi poly = new PolygonRoi(xPoints, yPoints, 6);
							PolygonRoi postpol = new PolygonRoi(poly.getInterpolatedPolygon(2, false), 6);
							Roi[] roiDots = newCell.getRois(postpol.getXCoordinates(), postpol.getYCoordinates(),
									postpol);
							PolygonRoi lum = newCell.getConcaveHull(roiDots, THRESHOLD);
							lumenDots[zIndex][0] = lum;
						}
						// set the color of lumen in this case white
						Color colorCurrentCell = new Color(255, 255, 255);
						lumenDots[zIndex][0].setStrokeColor(colorCurrentCell);
						// if the 1 position is not empty set the color
						if (lumenDots[zIndex][1] != null)
							lumenDots[zIndex][1].setStrokeColor(colorCurrentCell);

					}

					zIndex++;
				} 
				checkLumen.setEnabled(true);
			} else {
				IJ.log("Any lumen file selected");
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			IJ.log("Not valid lumen");
		}
	}

	/**
	 * Save ply files, xml and copy limeSegParams from original LimeSeg Output
	 * 
	 */
	public void savePlyFiles() {
		setFileChooserProperties("Select the segmented lumen folder");
		if (fileChooser.showOpenDialog(this) == fileChooser.APPROVE_OPTION) {
		String path = fileChooser.getSelectedFile().toString() + "/newOutputLimeSeg";
		// instance of a DocumentBuilderFactory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {

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
			purgeDirectory(dir, 1);

			DocumentBuilder db = dbf.newDocumentBuilder();

			String fromFile = fileChooser.getSelectedFile().toString() + "/OutputLimeSeg/LimeSegParams.xml";
			String toFile = fileChooser.getSelectedFile().toString() + "/newOutputLimeSeg/LimeSegParams.xml";
			copyFile(fromFile, toFile);

			all3dCells.forEach(c -> {
				// Cell Channel
				Document domCell = db.newDocument();
				Element cellParams = domCell.createElement("CellParameters");
				Element channel = domCell.createElement("channel");
				channel.appendChild(domCell.createTextNode(Integer.toString(c.cellChannel)));
				cellParams.appendChild(channel);
				// Cell color
				// create random light colors for each cell
				Random rand = new Random();
				float R = (float) (rand.nextFloat() / 2f + 0.5);
				float G = (float) (rand.nextFloat() / 2f + 0.5);
				float B = (float) (rand.nextFloat() / 2f + 0.5);

				Element color = domCell.createElement("color");
				Element r = domCell.createElement("R");
				r.appendChild(domCell.createTextNode(Float.toString(R)));
				color.appendChild(r);

				Element g = domCell.createElement("G");
				g.appendChild(domCell.createTextNode(Float.toString(G)));
				color.appendChild(g);

				Element b = domCell.createElement("B");
				b.appendChild(domCell.createTextNode(Float.toString(B)));
				color.appendChild(b);

				Element a = domCell.createElement("A");
				a.appendChild(domCell.createTextNode(Float.toString(100)));
				color.appendChild(a);
				cellParams.appendChild(color);

				// Now writes all ply files for CellT object
				String pathCell = path + File.separator + "cell_" + c.id_Cell + File.separator;
				File dirCell = new File(pathCell);
				dirCell.mkdir(); // attempt to create the directory here
				domCell.appendChild(cellParams);
				saveXmlFile(pathCell + "CellParams.xml", domCell);

				CellT cellt = new CellT(c, 1);
				cellt.dots = c.dotsList;
				if (dirCell.exists()) {
					IOXmlPlyLimeSeg.saveCellTAsPly(cellt, pathCell + "T_" + 1 + ".ply");
				}
			});
		} catch (ParserConfigurationException pce) {
			System.out.println("Save State: Error trying to instantiate DocumentBuilder " + pce);
		}
		} else {
			IJ.log("Any folder selected");
		}
	}

	/**
	 * Functions from LimeSeg to clean the files
	 * 
	 * @param dir
	 * @param height
	 */
	static void purgeDirectory(File dir, int height) {
		// no need to clean below level
		if (height >= 0) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory())
					purgeDirectory(file, height - 1);
				file.delete();
			}
		}
	}

	/**
	 * Save XML
	 * 
	 * @param path
	 * @param dom
	 */
	static void saveXmlFile(String path, Document dom) {
		try {
			Transformer tr = TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT, "yes");
			tr.setOutputProperty(OutputKeys.METHOD, "xml");
			tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			// send DOM to file

			FileOutputStream fos = new FileOutputStream(path);
			tr.transform(new DOMSource(dom), new StreamResult(fos));
			fos.close();
		} catch (TransformerException te) {
			System.out.println(te.getMessage());
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}

	/**
	 * function to copyFiles
	 * 
	 * @param fromFile
	 * @param toFile
	 * @return
	 */
	public boolean copyFile(String fromFile, String toFile) {
		File origin = new File(fromFile);
		File destination = new File(toFile);
		if (origin.exists()) {
			try {
				InputStream in = new FileInputStream(origin);
				OutputStream out = new FileOutputStream(destination);
				// We use a buffer for the copy (Usamos un buffer para la
				// copia).
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				return true;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}

	public void updateOverlay() {

		if (cellOutlineChannel.getOverlay() != null) {
			cellOutlineChannel.getOverlay().clear();
			if (checkOverlay.getSelectedItem() == "All overlays") {
				Overlay newOverlay = addOverlay(((Integer) cellSpinner.getValue() - 1),
						cellOutlineChannel.getCurrentSlice(), all3dCells, cellOutlineChannel, true, lumenDots);
				cellOutlineChannel.setOverlay(newOverlay);
			} else if (checkOverlay.getSelectedItem() == "Cell overlay") {
				Overlay newOverlay = addOverlay(((Integer) cellSpinner.getValue() - 1),
						cellOutlineChannel.getCurrentSlice(), all3dCells, cellOutlineChannel, false, lumenDots);
				cellOutlineChannel.setOverlay(newOverlay);
			}
			// this.cellOutlineChannel.updateAndRepaintWindow();
		}
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

	/**
	 * 
	 * @param enabled
	 * @param panel
	 */
	public void setEnablePanel(boolean enabled) {
		for (Component c : this.getComponents()) {
			c.setEnabled(enabled);
		}
	}

}
