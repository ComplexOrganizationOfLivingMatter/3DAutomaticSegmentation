
package AutomaticSegmentation.elements;

import java.awt.Point;
import java.util.ArrayList;

import eu.kiaru.limeseg.struct.DotN;

/**
 * Class adding some behaviour to LimeSeg's cell.
 * It is used as basic element for the postprocessing protocol.
 * @author Antonio Tagua
 *
 */
public class Cell3D extends eu.kiaru.limeseg.struct.Cell {

	/**
	 * List of dots contained within this cell
	 */
	public ArrayList<DotN> dotsList;
	public Integer labelCell;

	/**
	 * Constructor
	 * @param id of the cell
	 * @param dots points of the cell
	 */
	public Cell3D(String id, ArrayList<DotN> dots) {
		dotsList = new ArrayList<DotN>();
		id_Cell = new String();
		this.dotsList.addAll(dots);
		this.id_Cell = id;
	}

	/**
	 * Get the dots at a specific frame for this Cell
	 * 
	 * @param frame z of this Cell
	 * @return the list of dots at this z frame, if it exists. returns null
	 *         otherwise
	 */
	public ArrayList<DotN> getCell3DAt(int frame) {
		ArrayList<DotN> allDots = new ArrayList<DotN>();
		for (int i = 0; i < dotsList.size(); i++) {
			int zpos = 1 + (int) ((float) (dotsList.get(i).pos.z / (float) 4.06));
			if (zpos == frame) {
				DotN dot = dotsList.get(i);
				allDots.add(dot);
			}
		}
		return allDots;
	}
	
	/**
	 * @return the label of the cell
	 */
	public Integer getID() {
		return this.labelCell;
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
	 * 
	 * @param dots
	 */
	public void setCell3D(ArrayList<DotN> dots) {
		this.dotsList = dots;
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
}
