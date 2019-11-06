
package AutomaticSegmentation.elements;

import java.awt.Point;
import java.util.ArrayList;

import AutomaticSegmentation.gui.PanelPostProcessing;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.DotN;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Class adding some behaviour to LimeSeg's cell. It is used as basic element
 * for the postprocessing protocol.
 * 
 * @author Antonio Tagua, Pablo Vicente-Munuera
 *
 */
public class Cell3D extends Cell {

	/**
	 * List of dots contained within this cell
	 */
	public ArrayList<DotN> dotsList;
	public int id;
	public float zScale;
	public int totalFrames;

	/**
	 * 
	 */
	public Cell3D(Cell cell, float zScale, int totalFrames) {
		super();
		this.totalFrames = totalFrames;
		this.dotsList = new ArrayList<DotN>();
		this.zScale = zScale;
		this.dotsList = processLimeSegOutput(cell.cellTs.get(0).dots, zScale);
		this.id_Cell = cell.id_Cell;
	}

	/**
	 * @param dotsList
	 * @param id
	 * @param zScale
	 */
	public Cell3D(int id, float zScale, int totalFrames) {
		super();
		this.totalFrames = totalFrames;
		this.dotsList = new ArrayList<DotN>();
		this.id = id;
		this.zScale = zScale;
	}

	/**
	 * Get the dots at a specific frame for this Cell
	 * 
	 * @param frame
	 *            z of this Cell
	 * @return the list of dots at this z frame, if it exists. returns null
	 *         otherwise
	 */
	public ArrayList<DotN> getCell3DAt(int frame) {
		ArrayList<DotN> allDots = new ArrayList<DotN>();
		if (dotsList != null) {
			for (int i = 0; i < dotsList.size(); i++) {
				int zpos = 1 + (int) ((float) (dotsList.get(i).pos.z / zScale));
				if (zpos == frame) {
					DotN dot = dotsList.get(i);
					allDots.add(dot);
				}
			}
		}
		return allDots;
	}

	/**
	 * Get the dots at a specific frame for this Cell
	 * 
	 * @param frame
	 *            z of this Cell
	 * @return the list of dots at this z frame, if it exists. returns null
	 *         otherwise
	 */
	public ArrayList<DotN> getCell3DAt(ArrayList<DotN> dots, int frame) {
		ArrayList<DotN> allDots = new ArrayList<DotN>();
		if (dots != null) {
			for (int i = 0; i < dots.size(); i++) {
				int zpos = 1 + (int) ((float) (dots.get(i).pos.z / zScale));
				if (zpos == frame) {
					DotN dot = dots.get(i);
					allDots.add(dot);
				}
			}
		}
		return allDots;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the label of the cell
	 */
	public Integer getID() {
		return this.id;
	}

	/**
	 * 
	 * @param axis
	 * @param dots
	 * @return
	 */
	public float[] getCoordinate(String axis, ArrayList<DotN> dots) {
		float[] coordinates = new float[dots.size()];
		for (int i = 0; i < dots.size(); i++) {
			if (axis == "x") {
				coordinates[i] = dots.get(i).pos.x;
			}

			else if (axis == "y") {
				coordinates[i] = dots.get(i).pos.y;
			}

			else {
				coordinates[i] = dots.get(i).pos.z;
			}

		}
		return coordinates;
	}

	/**
	 * 
	 * @param frame
	 * @return
	 */
	public Point[] getPoints(int frame) {
		ArrayList<DotN> cellZDots = this.getCell3DAt(frame);
		Point[] cellPoints = new Point[cellZDots.size()];
		for (int nDot = 0; nDot < cellZDots.size(); nDot++) {
			cellPoints[nDot] = new Point((int) cellZDots.get(nDot).pos.x, (int) cellZDots.get(nDot).pos.y);
		}

		return cellPoints;
	}

	/**
	 * @return the dotsList
	 */
	public ArrayList<DotN> getDotsList() {
		return dotsList;
	}

	/**
	 * @param dotsList
	 *            the dotsList to set
	 */
	public void setDotsList(ArrayList<DotN> dotsList) {
		this.dotsList = dotsList;
	}

	/**
	 * 
	 */
	public void clearCell() {
		if (this.dotsList != null) {
			this.dotsList.clear();
		}
	}

	/**
	 * 
	 * @param dots
	 */
	public void addDotsList(ArrayList<DotN> dots) {
		for (int nDot = 0; nDot < dots.size(); nDot++) {
			this.dotsList.add(dots.get(nDot));
		}
	}

	/**
	 * 
	 * @param allDots
	 * @param zScale
	 * @return
	 */
	public ArrayList<DotN> processLimeSegOutput(ArrayList<DotN> allDots, float zScale) {
		ArrayList<DotN> newDots = new ArrayList<DotN>();
		for (int numFrame = 0; numFrame < this.totalFrames; numFrame++) {
			ArrayList<DotN> dots = getCell3DAt(allDots, numFrame);

			if (dots.size() > 0) {
				int[] xPoints = new int[dots.size()];
				int[] yPoints = new int[dots.size()];

				for (int i = 0; i < yPoints.length; i++) {
					xPoints[i] = (int) dots.get(i).pos.x;
					yPoints[i] = (int) dots.get(i).pos.y;
				}
				PolygonRoi PrePolygon = new PolygonRoi(xPoints, yPoints, xPoints.length, 2);
				// order the dots according the nearest dots
				PolygonRoi prePolygon = RoiAdjustment.getOrderDots(PrePolygon);
				// create a Roi with the polygon from orderDots
				Roi[] allRoi = RoiAdjustment.getRois(prePolygon.getXCoordinates(), prePolygon.getYCoordinates(),
						prePolygon);
				// Calculate the boarder with concave hull
				PolygonRoi poly = RoiAdjustment.getConcaveHull(allRoi, PanelPostProcessing.THRESHOLD);
				// Full fill the border with dots
				PolygonRoi polygon = new PolygonRoi(poly.getInterpolatedPolygon(1, false), 2);

				Roi[] allRois = RoiAdjustment.getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
				newDots.addAll(RoiAdjustment.RoisToDots(numFrame, allRois, zScale));
			}
		}

		return newDots;
	}
}
