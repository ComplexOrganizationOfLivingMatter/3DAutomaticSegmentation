
package AutomaticSegmentation.elements;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import AutomaticSegmentation.gui.PanelPostProcessing;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;
import eu.kiaru.limeseg.struct.Vector3D;
import graphics.scenery.Mesh;
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
	public ArrayList<DotN>[] dotsPerSlice;
	private ArrayList<DotN> allDots;
	public int id;
	public float zScale;
	public int totalSlices;

	/**
	 * 
	 * @param cell
	 *            father cell
	 * @param zScale
	 *            difference between Zs
	 * @param totalSlices
	 *            max number of frames
	 */
	public Cell3D(Cell cell, float zScale, int totalSlices) {
		super();
		// Copy father info
		this.id_Cell = cell.id_Cell;
		this.cellChannel = cell.cellChannel;
		this.cellTs = cell.cellTs;
		this.color = cell.color;
		this.display_mode = 1;

		// New info of this class
		this.totalSlices = totalSlices;
		this.dotsPerSlice = (ArrayList<DotN>[]) new ArrayList[totalSlices];
		this.zScale = zScale;
		this.id = Integer.parseInt(cell.id_Cell);
		processLimeSegOutput(cell.cellTs.get(0).dots, zScale);
	}

	/**
	 * @param totalSlices
	 *            max number of frames
	 * @param id
	 *            identifier
	 * @param zScale
	 *            difference between Zs
	 */
	public Cell3D(int id, float zScale, int totalSlices) {
		super();
		this.totalSlices = totalSlices;
		this.id = id;
		this.zScale = zScale;
		this.cellTs = new ArrayList<CellT>();
	}

	/**
	 * Get the dots at a specific slice for this Cell
	 * 
	 * @param slice
	 *            z of this Cell
	 * @return the list of dots at this z frame, if it exists. returns null
	 *         otherwise
	 */
	public ArrayList<DotN> getCell3DAt(int slice) {
		return this.dotsPerSlice[slice];
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
	 *            selected axis
	 * @param dots
	 *            points of the cell
	 * @return
	 */
	public static float[] getCoordinate(String axis, ArrayList<DotN> dots) {
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
	 * @param slice
	 *            selected Z frame
	 * @return cell points
	 */
	public Point[] getPoints(int slice) {
		ArrayList<DotN> cellZDots = this.getCell3DAt(slice);
		Point[] cellPoints = new Point[cellZDots.size()];
		for (int nDot = 0; nDot < cellZDots.size(); nDot++) {
			cellPoints[nDot] = new Point((int) cellZDots.get(nDot).pos.x, (int) cellZDots.get(nDot).pos.y);
		}

		return cellPoints;
	}

	/**
	 * @return the dotsList
	 */
	public ArrayList<DotN>[] getDotsPerSlice() {
		return dotsPerSlice;
	}

	/**
	 * @param dotsList
	 *            the dotsList to set
	 */
	public void setDotsPerSlice(ArrayList<DotN>[] dotsList) {
		this.dotsPerSlice = dotsList;
		this.cellTs.get(0).dots = mergeAll(this.dotsPerSlice);
	}

	/**
	 * @return the dotsListMerged
	 */
	public ArrayList<DotN> getAllDots() {
		return allDots;
	}

	/**
	 * 
	 */
	public void clearCell() {
		if (this.dotsPerSlice != null) {
			this.allDots.clear();
			for (ArrayList<DotN> arrayFrame : dotsPerSlice) {
				arrayFrame.clear();
			}
		}
	}

	/**
	 * 
	 * @param allDots
	 *            the dots
	 * @param zScale
	 *            difference between Zs
	 * @return the new dots processed
	 */
	public void processLimeSegOutput(ArrayList<DotN> allDots, float zScale) {
		this.dotsPerSlice = (ArrayList<DotN>[]) new ArrayList[this.totalSlices];
		List<Integer> xPoints;
		List<Integer> yPoints;

		for (int numFrame = 0; numFrame < this.totalSlices; numFrame++) {

			this.dotsPerSlice[numFrame] = new ArrayList<DotN>();

			xPoints = new ArrayList<Integer>();
			yPoints = new ArrayList<Integer>();

			getDotsAtFrame(allDots, zScale, xPoints, yPoints, numFrame);

			if (xPoints.size() > 0) {
				int[] xPoints_array = xPoints.stream().mapToInt(i -> i).toArray();
				int[] yPoints_array = yPoints.stream().mapToInt(i -> i).toArray();

				PolygonRoi PrePolygon = new PolygonRoi(xPoints_array, yPoints_array, yPoints_array.length, 2);
				// order the dots according the nearest dots
				PolygonRoi prePolygon = RoiAdjustment.getOrderDots(PrePolygon);
				// Calculate the boarder with concave hull
				PolygonRoi poly = RoiAdjustment.getConcaveHull(prePolygon, PanelPostProcessing.THRESHOLD);
				// Full fill the border with dots
				PolygonRoi polygon = new PolygonRoi(poly.getInterpolatedPolygon(1, false), 2);
				Roi[] allRois = RoiAdjustment.getAsRoiPoints(polygon);
				this.dotsPerSlice[numFrame] = RoiAdjustment.RoisToDots(numFrame, allRois, zScale, this.cellTs.get(0));
			}
		}

		this.cellTs.get(0).dots = mergeAll(this.dotsPerSlice);
		this.allDots = this.cellTs.get(0).dots;
		updateDotsNormals(this.cellTs.get(0).dots);

	}

	/**
	 * 
	 * @param dotsList2
	 * @return
	 */
	public ArrayList<DotN> mergeAll(ArrayList<DotN>[] dotsListToUnify) {
		ArrayList<DotN> mergedArray = new ArrayList<DotN>();

		for (ArrayList<DotN> arrayToMerge : dotsListToUnify) {
			mergedArray.addAll(arrayToMerge);
		}

		return mergedArray;
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
		for (int numPoint = 0; numPoint < allDots.size(); numPoint++) {
			int zpos = 1 + (int) ((float) (allDots.get(numPoint).pos.z / zScale));
			if (zpos == numFrame) {
				xPoints.add((int) allDots.get(numPoint).pos.x);
				yPoints.add((int) allDots.get(numPoint).pos.y);
			}
		}
	}

	/**
	 * 
	 * @param newDots
	 *            to calculate the normals
	 */
	public void updateDotsNormals(ArrayList<DotN> newDots) {
		double[] cellCentroid = getCellCentroid();

		for (DotN dot : newDots) {
			dot.Norm = new Vector3D(dot.pos.x - cellCentroid[0], dot.pos.y - cellCentroid[1],
					dot.pos.z - cellCentroid[2]);
			dot.Norm.normalize();
		}
	}

	/**
	 * 
	 * @return the centroid
	 */
	public double[] getCellCentroid() {
		// TODO Auto-generated method stub
		double[] centroid = { 0, 0, 0 };

		for (DotN singleDot : this.allDots) {
			centroid[0] += singleDot.pos.x;
			centroid[1] += singleDot.pos.y;
			centroid[2] += singleDot.pos.z;
		}

		centroid[0] = centroid[0] / this.allDots.size();
		centroid[1] = centroid[1] / this.allDots.size();
		centroid[2] = centroid[2] / this.allDots.size();

		return centroid;
	}

	public Mesh buildMesh() {
		// Mesh cellMesh = new Mesh(this.id_Cell);

		ArrayList<DotN> dotsActualFrame;

		int firstFrame = -1;
		int lastFrame = -1;
		for (int numFrame = 0; numFrame < totalSlices; numFrame++) {
			dotsActualFrame = this.dotsPerSlice[numFrame];
			// dotsNextFrame = this.dotsPerSlice[numFrame];

			if (dotsActualFrame.isEmpty() == false) {
				lastFrame = numFrame;
			}

			if (dotsActualFrame.isEmpty() == false && firstFrame == -1) {
				firstFrame = numFrame;
			}

			// NOT WORKING
			// for (DotN dotA : dotsActualFrame) {
			// for (DotN dotN : dotsNextFrame) {
			// newIntermediateDots.addAll(RoiAdjustment.interpolate(dotN.pos,
			// dotA.pos, Math.round(zScale), dotA));
			// }
			// }
		}
		

		ArrayList<DotN> newIntermediateDots = new ArrayList<DotN>();
		Collections.shuffle(this.allDots);
		
		int percentageOfShowing = 10;

		for (int i = 0; i < Math.floor(this.allDots.size() / percentageOfShowing); i++) {
			newIntermediateDots.add(this.allDots.get(i));
		}

		// Add last frames and first frames
		ArrayList<DotN> lastDots = new ArrayList<DotN>();
		lastDots.addAll(RoiAdjustment.getContainedPoints(this.dotsPerSlice[lastFrame]));
		Collections.shuffle(lastDots);

		for (int i = 0; i < Math.floor(lastDots.size() / percentageOfShowing); i++) {
			newIntermediateDots.add(lastDots.get(i));
		}
		
		ArrayList<DotN> firstDots = new ArrayList<DotN>();
		firstDots.addAll(RoiAdjustment.getContainedPoints(this.dotsPerSlice[firstFrame]));
		Collections.shuffle(firstDots);

		for (int i = 0; i < Math.floor(firstDots.size() / percentageOfShowing); i++) {
			newIntermediateDots.add(firstDots.get(i));
		}

		// As in LimeSeg.constructMesh();
		int ans = -1;
		if (LimeSeg.currentCell != null) {
			CellT ct = new CellT(this, LimeSeg.currentCell.cellTs.get(0).frame);
			if (ct != null) {
				ct.dots = newIntermediateDots;
				ct.updateCenter();
				updateDotsNormals(ct.dots);
				ans = ct.constructMesh();
				ct.modified = true;
				LimeSeg.notifyCellRendererCellsModif = true;
				LimeSeg.notifyCellExplorerCellsModif = true;
				this.cellTs.set(0, ct);
			}
			LimeSeg.currentCell.modified = true;
		}

		return null;
	}
}
