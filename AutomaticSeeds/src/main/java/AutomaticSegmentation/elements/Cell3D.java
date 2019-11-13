
package AutomaticSegmentation.elements;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import AutomaticSegmentation.gui.PanelPostProcessing;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.DotN;
import eu.kiaru.limeseg.struct.Vector3D;
import graphics.scenery.Mesh;
import eu.kiaru.limeseg.struct.CellT;
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
	 * @param cell father cell
	 * @param zScale difference between Zs
	 * @param totalFrames max number of frames
	 */
	public Cell3D(Cell cell, float zScale, int totalFrames) {
		super();
		//Copy father info
		this.id_Cell = cell.id_Cell;
		this.cellChannel = cell.cellChannel;
		this.cellTs = cell.cellTs;
		this.color = cell.color;
		this.display_mode = 1;
		
		// New info of this class
		this.totalFrames = totalFrames;
		this.dotsList = new ArrayList<DotN>();
		this.zScale = zScale;
		this.id = Integer.parseInt(cell.id_Cell);
		this.dotsList = processLimeSegOutput(cell.cellTs.get(0).dots, zScale);
		cell.cellTs.get(0).dots = this.dotsList;
	}

	/**
	 * @param totalFrames max number of frames
	 * @param id identifier
	 * @param zScale difference between Zs
	 */
	public Cell3D(int id, float zScale, int totalFrames) {
		super();
		this.totalFrames = totalFrames;
		this.dotsList = new ArrayList<DotN>();
		this.id = id;
		this.zScale = zScale;
		this.cellTs = new ArrayList<CellT>();
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
	 * @param dots all the dots of the cell
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
	 * @param axis selected axis
	 * @param dots points of the cell
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
	 * @param frame selected Z frame
	 * @return cell points
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
	 * @param dots add new points
	 */
	public void addDotsList(ArrayList<DotN> dots) {
		for (int nDot = 0; nDot < dots.size(); nDot++) {
			this.dotsList.add(dots.get(nDot));
		}
	}

	/**
	 * 
	 * @param allDots the dots
	 * @param zScale difference between Zs
	 * @return the new dots processed
	 */
	public ArrayList<DotN> processLimeSegOutput(ArrayList<DotN> allDots, float zScale) {
		ArrayList<DotN> newDots = new ArrayList<DotN>();
		List<Integer> xPoints;
		List<Integer> yPoints;
		
		for (int numFrame = 0; numFrame < this.totalFrames; numFrame++) {

			xPoints = new ArrayList<Integer>();
			yPoints = new ArrayList<Integer>();

			getDotsAtFrame(allDots, zScale, xPoints, yPoints, numFrame);
			
			if (xPoints.size() > 0) {
				int[] xPoints_array = xPoints.stream().mapToInt(i -> i).toArray();
				int[] yPoints_array = yPoints.stream().mapToInt(i -> i).toArray();

				PolygonRoi PrePolygon = new PolygonRoi(xPoints_array, yPoints_array, yPoints_array.length, 2);
				// order the dots according the nearest dots
				PolygonRoi prePolygon = RoiAdjustment.getOrderDots(PrePolygon);
				// create a Roi with the polygon from orderDots
				Roi[] allRoi = RoiAdjustment.getAsRoiPoints(prePolygon.getXCoordinates(), prePolygon.getYCoordinates(),
						prePolygon);
				// Calculate the boarder with concave hull
				PolygonRoi poly = RoiAdjustment.getConcaveHull(allRoi, PanelPostProcessing.THRESHOLD);
				// Full fill the border with dots
				PolygonRoi polygon = new PolygonRoi(poly.getInterpolatedPolygon(1, false), 2);

				Roi[] allRois = RoiAdjustment.getAsRoiPoints(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
				newDots.addAll(RoiAdjustment.RoisToDots(numFrame, allRois, zScale, this.cellTs.get(0)));
			}
		}
		
		updateDotsNormals(newDots);

		return newDots;
	}

	/**
	 * @param allDots
	 * @param zScale
	 * @param xPoints
	 * @param yPoints
	 * @param numFrame
	 */
	private void getDotsAtFrame(ArrayList<DotN> allDots, float zScale, List<Integer> xPoints, List<Integer> yPoints,
			int numFrame) {
		for (int i = 0; i < allDots.size(); i++) {
			int zpos = 1 + (int) ((float) (allDots.get(i).pos.z / zScale));
			if (zpos == numFrame) {
				xPoints.add((int) allDots.get(i).pos.x);
				yPoints.add((int) allDots.get(i).pos.y);
			}
		}
	}

	/**
	 * 
	 * @param newDots to calculate the normals 
	 */
	public void updateDotsNormals(ArrayList<DotN> newDots) {
		double[] cellCentroid = getCellCentroid();
		
		for (DotN dot : newDots) {
			dot.Norm = new Vector3D(dot.pos.x - cellCentroid[0], dot.pos.y - cellCentroid[1], dot.pos.z - cellCentroid[2]);
	        dot.Norm.normalize();
		}
	}

	/**
	 * 
	 * @return the centroid
	 */
	public double[] getCellCentroid() {
		// TODO Auto-generated method stub
		double[] centroid = {0, 0, 0};
		
		for (DotN singleDot : dotsList) {
			centroid[0] += singleDot.pos.x;
			centroid[1] += singleDot.pos.y;
			centroid[2] += singleDot.pos.z;
		}

		centroid[0] = centroid[0] / dotsList.size();
		centroid[1] = centroid[1] / dotsList.size();
		centroid[2] = centroid[2] / dotsList.size();
		
		return centroid;
	}
	
	public Mesh buildMesh(){
		Mesh cellMesh = new Mesh(this.id_Cell);
		
		ArrayList<DotN> dotsActualFrame;
		ArrayList<DotN> dotsNextFrame;
		for (int numFrame = 1; numFrame < totalFrames; numFrame++){
			dotsActualFrame = getCell3DAt(this.dotsList, numFrame-1);
			dotsNextFrame = getCell3DAt(this.dotsList, numFrame);
			
			
		}
		
		// Construct mesh using Facets and Vertices (points)
		
		
		return cellMesh;
	}
}
