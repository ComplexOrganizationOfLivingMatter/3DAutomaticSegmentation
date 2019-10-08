
package AutomaticSegmentation.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.opensphere.geometry.algorithm.ConcaveHull;
import org.w3c.dom.Document;

import com.google.common.primitives.Ints;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import AutomaticSegmentation.MainAutomatic3DSegmentation;
import AutomaticSegmentation.Elements.Cell3D;
import AutomaticSegmentation.Elements.RoiAdjustment;
import epigraph.GUI.CustomElements.CustomCanvas;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.io.IOXmlPlyLimeSeg;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageRoi;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatPolygon;
import ij.gui.Overlay;
import ij.process.ImageProcessor;
import ij3d.Image3DUniverse;
import net.miginfocom.swing.MigLayout;


public class PostProcessingWindow extends ImageWindow implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

	public static double THRESHOLD = 5 ; 
	
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
	private Scrollbar zoom;
	private JLabel slicePanel;

	private PolygonRoi polyRoi;

	private String initialDirectory;
	public Cell3D PostProcessCell;
	public ArrayList<Cell3D> all3dCells;
	public PolygonRoi[][] lumenDots;
	public float zScale;

	public PostProcessingWindow(ImagePlus raw_img) {
		super(raw_img, new CustomCanvas(raw_img));
		//time 6 seconds
		this.initialDirectory = raw_img.getOriginalFileInfo().directory;
		limeSeg = new LimeSeg();
		newCell = new RoiAdjustment();
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
			IOXmlPlyLimeSeg.hydrateCellT(LimeSegCell, path);					
			Cell3D PostProcessCellCopy = new Cell3D(LimeSegCell.id_Cell, LimeSegCell.cellTs.get(0).dots);
			PostProcessCell = new Cell3D(LimeSegCell.id_Cell, LimeSegCell.cellTs.get(0).dots);
			PostProcessCell.clearCell();
			for (int i = 0; i < imp.getStackSize(); i++) {
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
		
		canvas = (CustomCanvas) super.getCanvas();
		MainAutomatic3DSegmentation.callToolbarPolygon();
		
		sliceSelector = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, (imp.getStackSize()));
		sliceSelector.setVisible(true);
		
		//Zoom, create an invisible scrollbar to limit the canvas zone (I don't understand why)
		zoom = new Scrollbar(Scrollbar.VERTICAL, 1, 1, 1, 1024);	
		zoom.setVisible(false);
		
		//Initialize lumenDots as matrix [x][2] to split the lumen in 2 polygons
		lumenDots = new PolygonRoi[imp.getStackSize()+1][2];
		loadLumen();
		removeCellLumenOverlap();
		initializeGUIItems();
		raw_img.setOverlay(addOverlay(0, canvas.getImage().getCurrentSlice(), all3dCells, raw_img, false, lumenDots));
		initGUI();
		
		//Image3DUniverse C3D = new Image3DUniverse (1024,1024);
		
	}

	private void initGUI() {

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
		rightPanel.add(slicePanel,"aligny center, wrap, south");

		leftPanel.setLayout(new MigLayout());
		
		canvas.setMaximumSize(new Dimension (1024,1024));
		canvas.setMinimumSize(new Dimension (500,500));
		Color newColor = new Color(200, 200, 255);
		sliceSelector.setBackground(newColor);		
		leftPanel.add(canvas,"wrap");
		leftPanel.add(sliceSelector, "growx");
		leftPanel.add(zoom, "west,growy");

		processingFrame.setLayout(new MigLayout());
		processingFrame.add(leftPanel);
		processingFrame.add(rightPanel, "east");
		processingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		processingFrame.pack();
		processingFrame.setVisible(true);

	}

	private void initializeGUIItems() {

		// Init attributes.
		newCell = new RoiAdjustment();

		processingFrame = new JFrame();
		upRightPanel = new JPanel();
		middlePanel = new JPanel();
		bottomRightPanel = new JPanel();
		rightPanel = new JPanel();
		leftPanel = new JPanel();
		slicePanel = new JLabel();

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

		btnLumen = new JButton("Update Lumen");
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
				int p = imp.getStackSize();
				int s = sliceSelector.getValue();
				String slice = "Current slice:" + Integer.toString(s) + "/" + Integer.toString(p);
				slicePanel.setText(slice);
				updateOverlay();

			}
		});
		//Zoom
		processingFrame.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) 
			{
				// TODO Auto-generated method stub
				int rotation = e.getWheelRotation();
				int amount = e.getScrollAmount();
				@SuppressWarnings("deprecation")
				boolean ctrl = (e.getModifiers()&Event.CTRL_MASK)!=0;
				if (IJ.debugMode) {
					IJ.log("mouseWheelMoved: "+e);
					IJ.log("  type: "+e.getScrollType());
					IJ.log("  ctrl: "+ctrl);
					IJ.log("  rotation: "+rotation);
					IJ.log("  amount: "+amount);
				}
				if (amount<1) amount=1;
				if (rotation==0)
					return;
				int width = canvas.getWidth();
				int height = canvas.getHeight();
				Rectangle srcRect = canvas.getSrcRect();
				int xstart = srcRect.x;
				int ystart = srcRect.y;
				if ((ctrl||IJ.shiftKeyDown()) && canvas!=null) {
					Point loc = canvas.getCursorLoc();
					int x = canvas.screenX(loc.x);
					int y = canvas.screenY(loc.y);
					if (rotation<0)
						canvas.zoomIn(x, y);
					else
						canvas.zoomOut(x, y);
					return;
				} else if (IJ.spaceBarDown() || srcRect.height==height) {
					srcRect.x += rotation*amount*Math.max(width/200, 1);
					if (srcRect.x<0) srcRect.x = 0;
					if (srcRect.x+srcRect.width>width) srcRect.x = width-srcRect.width;
				} else {
					srcRect.y += rotation*amount*Math.max(height/200, 1);
					if (srcRect.y<0) srcRect.y = 0;
					if (srcRect.y+srcRect.height>height) srcRect.y = height-srcRect.height;
				}
				if (srcRect.x!=xstart || srcRect.y!=ystart)
					canvas.repaint();
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
			//Check if polyRoi is different to null, if is do the modify cell 
			if(polyRoi != null)
			{
				newCell.removeOverlappingRegions(all3dCells, polyRoi, canvas.getImage().getCurrentSlice(),
						all3dCells.get((Integer) cellSpinner.getValue() - 1).id_Cell, lumenDots);
				checkOverlay.setSelectedIndex(1);
				updateOverlay();
				//After modify cell return poly to null, clean the roi
				polyRoi = null;
			}
			//If polyRoi is null show a message to prevent errors
			else
			{
				JOptionPane.showMessageDialog(middlePanel.getParent(),"You must select a new Region.");
			}
			
		}

		if (e.getSource() == btnSave) 
		{
			this.savePlyFile(all3dCells, initialDirectory);
			//After saved the plyFiles show a message to inform the user
			JOptionPane.showMessageDialog(middlePanel.getParent(),"Saved results.");
		}

		if (e.getSource() == btnLumen)
		{
			//read the lumen
			loadLumen();
			//remove the overlaps cells
			removeCellLumenOverlap();
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

	public void savePlyFile(ArrayList<Cell3D> allCells, String path_in) {
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
				if(lumen[frame-1][1] != null)
					ov.addElement(lumen[frame - 1][1]);
			}
			//TODO: change this part of the code

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
	//order the dots according the nearest dots 
	PolygonRoi prePolygon = newCell.getOrderDots(PrePolygon);
	//create a Roi with the polygon from orderDots
	Roi[] allRoi = newCell.getRois(prePolygon.getXCoordinates(), prePolygon.getYCoordinates(), prePolygon);
	//Calculate the boarder with concave hull	
	PolygonRoi poly = newCell.getConcaveHull(allRoi, THRESHOLD); 
	//Full fill the border with dots
	PolygonRoi polygon = new PolygonRoi(poly.getInterpolatedPolygon(1,false),2);
	
    Roi[] allRois = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
	ArrayList<DotN> newDots = newCell.RoisToDots(frame, allRois);
	
	return newDots;
	}
	
	//Time 53 seconds
	public void removeCellLumenOverlap() 
	{
		for (int nFrame = 1; nFrame < imp.getStackSize()+1; nFrame++) 
		{
			for (int nCell = 0; nCell < all3dCells.size(); nCell++) 
			{
				//Check if the frame have lumen and the cell is not empty
				if (lumenDots[nFrame-1] != null & all3dCells.get(nCell).getCell3DAt(nFrame).size() > 0) 
				{
					//Get the cell points
					float[] xCell = all3dCells.get(nCell).getCoordinate("x", all3dCells.get(nCell).getCell3DAt(nFrame));
					float[] yCell = all3dCells.get(nCell).getCoordinate("y", all3dCells.get(nCell).getCell3DAt(nFrame));
					//Points to polygon to shape			
					PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 6);
					ShapeRoi r = new ShapeRoi(overlappingCell);
					ShapeRoi s = new ShapeRoi(overlappingCell);
					ShapeRoi s2 = new ShapeRoi(overlappingCell);
					//Check if lumen has two polygon
					if(lumenDots[nFrame-1][1] != null)
					{
						//Transform the lumens to polygons and shapes
						java.awt.Polygon l = lumenDots[nFrame-1][0].getPolygon();
						ShapeRoi lum = new ShapeRoi(l);
						//Verify if the lumen cross with cell
						s.and(lum);
						java.awt.Polygon l2 = lumenDots[nFrame-1][1].getPolygon();
						ShapeRoi lum2 = new ShapeRoi(l2);
						s2.and(lum2);	
						//If lumen cross with cell width or height must be different to 0, if is 0 go to other cell
						if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0) 
							{
								//use not function to get all the points out of the lumen and save in polygon
								PolygonRoi polygon = new PolygonRoi(r.not(lum).getContainedFloatPoints(),6);					
									
								Roi[] overRoi = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
								//get the border of polygon without lumen parts with concavehull function															
								PolygonRoi poly = newCell.getConcaveHull(overRoi,1);	
								
								// Convert the PolygonRoi in Dots and integrate with the dots of
								// the other frames.
								// Later, replace the selected cell by the cell with the new
								// region
								ArrayList<DotN> dotsNewRegion = newCell.setNewRegion(nFrame, poly);
								ArrayList<DotN> integratedDots = newCell.integrateNewRegion(dotsNewRegion,
										all3dCells.get(nCell).dotsList, nFrame);
										
								Cell3D newCell = new Cell3D(all3dCells.get(nCell).id_Cell, integratedDots);
								all3dCells.set(nCell, newCell);
								
							}
						//do the same for the second polygon
						if (s2.getFloatWidth() != 0 | s2.getFloatHeight() != 0)
								{						
									PolygonRoi polygon2 = new PolygonRoi(r.not(lum2).getContainedFloatPoints(),6);	
										
									Roi[] overRoi2 = newCell.getRois(polygon2.getXCoordinates(), polygon2.getYCoordinates(), polygon2);
										
									PolygonRoi poly2 = newCell.getConcaveHull(overRoi2,1);
										
									ArrayList<DotN> dotsNewRegion2 = newCell.setNewRegion(nFrame, poly2);
									ArrayList<DotN> integratedDots2 = newCell.integrateNewRegion(dotsNewRegion2,
											all3dCells.get(nCell).dotsList, nFrame);
										
									Cell3D newCell2 = new Cell3D(all3dCells.get(nCell).id_Cell, integratedDots2);
									all3dCells.set(nCell, newCell2);
								}
						
					}	
					//if lumen only have one polygon do the same as before but only once
					else if(lumenDots[nFrame-1][0] != null)
					{
						java.awt.Polygon l1 = lumenDots[nFrame-1][0].getPolygon();											
						ShapeRoi lum1 = new ShapeRoi(l1);
						s.and(lum1);					
						
						if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0)
						{
							PolygonRoi polygon = new PolygonRoi(r.not(lum1).getContainedFloatPoints(),6);
								
							Roi[] overRoi = newCell.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
								
							PolygonRoi poly = newCell.getConcaveHull(overRoi,1);							
								
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
	//Time 37 seconds
	public void loadLumen ()
	{
		File dirLumen = new File(this.initialDirectory.toString() + "/SegmentedLumen");
		File[] filesLumen = dirLumen.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith("");
			}
		});
		int zIndex = 0;

		try {
			//read the Lumen Directory
			for (File f : filesLumen) {
				FileInputStream lumen = new FileInputStream(f);
				BufferedImage lumen_img = ImageIO.read(lumen);
				ImagePlus lumenImg = new ImagePlus("Lumen", lumen_img);
				//Get the image and find the Edge of the lumen
				ImageProcessor lumImg = lumenImg.getProcessor();
				lumImg.findEdges();
				//Create a new image with lumen edges (edges are white)
				ImagePlus lumEd = new ImagePlus ("LumenEdge",lumImg);	

				ArrayList<Roi> fileLumenDots = new ArrayList<Roi>();
				//Read all the pixel and save the white (65535) pixels
				for (int y = 0; y < lumEd.getProcessor().getWidth(); y++) {
					for (int x = 0; x < lumEd.getProcessor().getHeight(); x++) {
						if (lumEd.getProcessor().getPixel(x,y) == 65535) {
							PointRoi dot = new PointRoi(x, y);
							fileLumenDots.add(dot);
						}
					}
				}
							
				
				if (fileLumenDots.size() != 0) {		
					Roi[] sliceDots = new Roi[fileLumenDots.size()];
					sliceDots = fileLumenDots.toArray(sliceDots);	
					//get the x, y points from the array										
					float[] xPoints = new float[sliceDots.length];
					float[] yPoints = new float[sliceDots.length];
					int dis = sliceDots.length;
					float disEu[] = new float[dis];

					for (int i = 0; i < sliceDots.length; i++) {
						xPoints[i] = (float) sliceDots[i].getXBase();
						yPoints[i] = (float) sliceDots[i].getYBase();
					}
					//calculate the distance between each point				
					for (int i = 0; i < dis; i++)
					{
						if (i == dis-1)
						{
							disEu[i] = newCell.distEu(xPoints[i],xPoints[i],yPoints[i],yPoints[i]);
						}
						else
						{
							disEu[i] = newCell.distEu(xPoints[i+1],xPoints[i],yPoints[i+1],yPoints[i]);	
						}
					}
					
					int pos = dis;
					int rest = 0;
					//check if the distance between is less than 100 to split in two polygons
					for (int i = 0; i < dis; i++)
					{
						if(disEu[i] > 100)
						{
							//if distance more than 100 save the position and the qty of points after the position
							pos = i;
							rest = dis-i;
						}
					}
					//if the qty is different to 0 split the roi in two polygons
					if(rest!= 0)
					{
						//save the points after position, this points are the 2nd polygon
						float x[] = new float [rest-1];
						float y[] = new float [rest-1];
						int j = 0;
						for(int i = pos+1; i < dis; i++)
						{
							x[j] = xPoints[i];
							y[j] = yPoints[i];
							j++;
						}	
						//create polygon one with the point until pos
						PolygonRoi poly = new PolygonRoi(xPoints, yPoints,pos, 6);
						//create polygon two with the rest of the points
						PolygonRoi poly2 = new PolygonRoi(x, y,6);
						//fill the second polygon with points to get the correct border
						PolygonRoi postpol = new PolygonRoi(poly2.getInterpolatedPolygon(2,false),6);
					
						Roi[] roiDots = newCell.getRois(poly.getXCoordinates(), poly.getYCoordinates(), poly);
						Roi[] roiDots2 = newCell.getRois(postpol.getXCoordinates(), postpol.getYCoordinates(), postpol);
						//find the border with ConcavHull
						PolygonRoi lum = newCell.getConcaveHull(roiDots, THRESHOLD);
						PolygonRoi lum2 = newCell.getConcaveHull(roiDots2, THRESHOLD);
						lumenDots[zIndex][0] = lum; //save the border in matrix position 0
						lumenDots[zIndex][1] = lum2; //save the border in position 1
					}
					else
					{
						//if is only one polygon, get the polygon, border and save
						PolygonRoi poly = new PolygonRoi(xPoints, yPoints, 6);
						PolygonRoi postpol = new PolygonRoi(poly.getInterpolatedPolygon(2,false),6);
						Roi[] roiDots = newCell.getRois(postpol.getXCoordinates(), postpol.getYCoordinates(), postpol);							
						PolygonRoi lum = newCell.getConcaveHull(roiDots, THRESHOLD);
						lumenDots[zIndex][0] = lum;
					}
					//set the color of lumen in this case white
					Color colorCurrentCell = new Color(255, 255, 255);
					lumenDots[zIndex][0].setStrokeColor(colorCurrentCell);
					//if the 1 position is not empty set the color
					if(lumenDots[zIndex][1]!=null)
						lumenDots[zIndex][1].setStrokeColor(colorCurrentCell);
							
				}

				zIndex++;
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
}
