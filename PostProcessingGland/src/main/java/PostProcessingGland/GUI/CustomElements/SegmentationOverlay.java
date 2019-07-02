
package PostProcessingGland.GUI.CustomElements;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import PostProcessingGland.Elements.Cell3D;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;

import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;

/**
 * CONTENT Helper class for LimeSeg segmentation The segmentation objects are
 * called Cells They are contained within the Array allCells Each Cell contains
 * one or Many CellT, which is a Cell at a timepoint Each CellT contains many
 * dots. It a represent a 3D object This class contains many functions to: -
 * store / retrieves these objects (I/O section) - display them in 2D on
 * ImagePlus image - display them in 3D in a custom 3D viewer
 */
public class SegmentationOverlay extends
	epigraph.GUI.CustomElements.ImageOverlay
{

//List of cells currently stored by LimeSeg
	static public ArrayList<Cell> allCells;

//Set of dots being overlayed on workingImP
	static public ArrayList<DotN> dots_to_overlay;

//Current selected Cell of limeseg
	static public Cell currentCell;

//Current selected Dot of limeseg
	static public ArrayList<DotN> currentDots;

//Current working image in IJ1 format 
	private ImagePlus workingImP;

// Used for IJ1 interaction (ROI, and JOGLCellRenderer synchronization)        
	static public int currentFrame = 1;
	private static PointRoi roi;
	public Polygon polygonOverlay;

//----------------- 2D View  
	/**
	 * Put dots of current user selected slice to overlays (requires updateOverlay
	 * to be effective)
	 * 
	 * @return
	 */

	static public void putCurrentSliceToOverlay(ImagePlus workingImP) {
		if (workingImP != null) {
			int zSlice; // = workingImP.getZ();

			if ((workingImP.getNFrames() != 1) || (workingImP.getNChannels() != 1)) {
				zSlice = workingImP.getZ();
			}
			else {
				zSlice = workingImP.getSlice();
			}
			for (Cell c : allCells) {
				CellT ct = c.getCellTAt(SegmentationOverlay.currentFrame);
				if (ct != null) {
					for (DotN dn : ct.dots) {
						if ((int) (dn.pos.z) == zSlice - 1) {
							SegmentationOverlay.dots_to_overlay.add(dn);
						}
					}
				}
			}
		}
	}

	static public void putCurrentZPositionToOverlay(ImagePlus workingImP) {
		if (workingImP != null) {
			for (Cell c : allCells) {
				CellT ct = c.getCellTAt(SegmentationOverlay.currentFrame);
				if (ct != null) {
					for (DotN dn : ct.dots) {
						SegmentationOverlay.dots_to_overlay.add(dn);
					}
				}
			}
		}
	}

	/**
	 * Clears image overlay (requires updateOverlay to be effective)
	 */
	static public void clearOverlay() {
		if (dots_to_overlay != null) dots_to_overlay.clear();
	}

	/**
	 * Adds all cells into image overlay (requires updateOverlay to be effective)
	 */

	static public void addAllCellsToOverlay() {
		for (int i = 0; i < allCells.size(); i++) {
			Cell c = allCells.get(i);
			// addToOverlay(c);
		}
	}

	/**
	 * updates Overlay of the working image with registeres dots to be overlayed
	 * 
	 * @return
	 */

	public Overlay getOverlay(Integer id, Integer frame,
		ArrayList<Cell3D> cells, ImagePlus workingImP, boolean allOverlays)
	{
		Overlay ov = new Overlay();
		if (workingImP != null) {
			workingImP.setOverlay(ov);
			if (allOverlays) {
				
			for (int nCell = 0; nCell < cells.size(); nCell++) {
				ArrayList<DotN> dots = cells.get(nCell).getCell3DAt(frame);
				Iterator<DotN> i = dots.iterator();
				while (i.hasNext()) {
					DotN loadedDots = i.next();
					roi = new PointRoi(loadedDots.pos.x, loadedDots.pos.y);
					Color colorCurrentCell = new Color(0, 0, 255);
					roi.setColor(colorCurrentCell);
					if (nCell == id) {
						colorCurrentCell = new Color(255, 0, 0);
						roi.setStrokeColor(colorCurrentCell);
					}
					
					ov.addElement(roi);
				}	
			}
		}
			else {
				ArrayList<DotN> dots = cells.get(id).getCell3DAt(frame);
				Iterator<DotN> i = dots.iterator();
				while (i.hasNext()) {
					DotN loadedDots = i.next();
					roi = new PointRoi(loadedDots.pos.x, loadedDots.pos.y);
					Color colorCurrentCell = new Color(255, 0, 0);
					roi.setColor(colorCurrentCell);
					polygonOverlay.addPoint((int) roi.getFloatWidth(), (int) roi.getFloatHeight());
					PolygonRoi polyRoi = new PolygonRoi(polygonOverlay, Roi.POLYGON);
					ov.addElement(polyRoi);
				}
			}
		}
		return ov;
	}

	static public void setWorkingImage(ImagePlus workingImP) {
		currentFrame = workingImP.getCurrentSlice();
	}

	static void updateWorkingImage(ImagePlus workingImP) {
		if (workingImP != null) {
			setWorkingImage(workingImP);
		}
		else {
			// IJ.log("Cannot change image : the Optimizer is running");
		}
	}

	static public void setCurrentFrame(ImagePlus workingImP, int cFrame) {
		currentFrame = cFrame;
		updateWorkingImage(workingImP);
	}

	public void setImage(ImagePlus workingImP) {
		this.workingImP = workingImP;
	}

	/**
	 * @return imageplus to be painted in the overlay
	 */
	/* public ImagePlus getImage() {
		return workingImP;
	}
	*/
	@Override
	public void setComposite(Composite composite) {
		// TODO Auto-generated method stub

	}

	@Override
	public void paint(Graphics g, int x, int y, double magnification) {
		// TODO Auto-generated method stub

	}

}
