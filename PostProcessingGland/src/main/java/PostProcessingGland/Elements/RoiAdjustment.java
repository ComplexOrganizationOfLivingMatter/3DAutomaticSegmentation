
package PostProcessingGland.Elements;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.stream.IntStream;

import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;

import org.apache.commons.lang3.ArrayUtils;
import org.opensphere.geometry.algorithm.ConcaveHull;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import eu.kiaru.limeseg.struct.DotN;

public class RoiAdjustment {

	public float zScale = (float) 4.06;
	public int selectedCell;

	/**
	 * 
	 * @param allCells
	 * @param newPolygon
	 * @param frame
	 * @param id
	 */
	public void removeOverlappingRegions(ArrayList<Cell3D> allCells, PolygonRoi newPolygon, int frame, String id) {
		for (int nCell = 0; nCell < allCells.size(); nCell++) {
			float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(frame));
			float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(frame));

			PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 2);

			ShapeRoi sNewPolygon = new ShapeRoi(newPolygon);
			ShapeRoi sNewPolygonBackUp = new ShapeRoi(sNewPolygon);
			ShapeRoi sOverlappingCell = new ShapeRoi(overlappingCell);
			ShapeRoi overlappingZone = new ShapeRoi(sNewPolygon.and(sOverlappingCell));

			if ((overlappingZone.getFloatWidth() != 0 | overlappingZone.getFloatHeight() != 0)
					& allCells.get(nCell).id_Cell != id) {
				ShapeRoi sNotOverlappingCell = new ShapeRoi(sOverlappingCell.not(sNewPolygonBackUp));
				Roi[] overRoi = sNotOverlappingCell.getRois();
				
				PolygonRoi poly = getConcaveHull(overRoi);
				
				// Convert the PolygonRoi in Dots and integrate with the dots of
				// the other frames.
				// Later, replace the selected cell by the cell with the new
				// region
				ArrayList<DotN> dotsNewRegion = setOverlapRegion(frame, poly, sNotOverlappingCell);
				ArrayList<DotN> integratedDots = integrateNewRegion(dotsNewRegion, allCells.get(nCell).dotsList, frame);

				Cell3D newCell = new Cell3D(allCells.get(nCell).id_Cell, integratedDots);
				allCells.set(nCell, newCell);
			} else if (allCells.get(nCell).id_Cell == id) {
				selectedCell = nCell;
			}
		}

		// Replace the cell with the mistaken overlay by the new cell.
		ArrayList<DotN> dotsNewRegion = setNewRegion(frame, newPolygon);
		ArrayList<DotN> integratedDots = integrateNewRegion(dotsNewRegion, allCells.get(selectedCell).dotsList, frame);
		Cell3D newCell = new Cell3D(id, integratedDots);
		allCells.set(selectedCell, newCell);

	}

	/**
	 * 
	 * @param newDots
	 *            dots that represent only the new Region
	 * @param oldDots
	 *            dots that represent the old Cell3D
	 * @param frame
	 * @return all the dots which will represent the new Cell3D: (newDots +
	 *         oldDots - oldDots(frame))
	 */
	public ArrayList<DotN> integrateNewRegion(ArrayList<DotN> newDots, ArrayList<DotN> oldDots, int frame) {
		ArrayList<DotN> currentDots = new ArrayList<DotN>();
		for (int i = 0; i < oldDots.size(); i++) {
			DotN dot = oldDots.get(i);
			int zpos = 1 + (int) ((float) (dot.pos.z / zScale));
			if (zpos != frame) {
				currentDots.add(dot);
			}
		}
		for (int j = 0; j < newDots.size(); j++) {
			currentDots.add(newDots.get(j));
		}
		return currentDots;

	}
	
	// ConcaveHull algoritm
	public PolygonRoi getConcaveHull(Roi[] rois) {
	GeometryFactory geoF = new GeometryFactory();
	com.vividsolutions.jts.geom.Point[] allPoints = new com.vividsolutions.jts.geom.Point[rois.length];

	for (int i = 0; i < rois.length; i++) {
		Coordinate coordinate = new Coordinate(rois[i].getXBase(), rois[i].getYBase());
		allPoints[i] = geoF.createPoint(coordinate);
	}

	ArrayList<com.vividsolutions.jts.geom.Point> geoList = new ArrayList<com.vividsolutions.jts.geom.Point>(
			Arrays.asList(allPoints));
	Geometry geoP = geoF.buildGeometry(geoList);

	ConcaveHull ch = new ConcaveHull(geoP, 100000);

	Geometry newGeoP = ch.getConcaveHull();
	Coordinate[] coords = newGeoP.getCoordinates();
	int[] xPoints = new int[coords.length];
	int[] yPoints = new int[coords.length];

	for (int i = 0; i < yPoints.length; i++) {
		xPoints[i] = (int) coords[i].x;
		yPoints[i] = (int) coords[i].y;
	}

	PolygonRoi poly = new PolygonRoi(xPoints, yPoints, xPoints.length, 2);
	
	return poly;
}

	// Setter method.

	/**
	 * 
	 * @param frame
	 *            z slice which is modified
	 * @param poly:
	 *            PolygonRoi which represent the new Region
	 */
	public ArrayList<DotN> setNewRegion(int frame, PolygonRoi poly) {
		ArrayList<DotN> dotsNewRegion = new ArrayList<DotN>();
		int[] xPolygon = poly.getXCoordinates();
		int[] yPolygon = poly.getYCoordinates();
		for (int nDot = 0; nDot < xPolygon.length; nDot++) {
			DotN newDot = new DotN();
			newDot.pos.x = (float) (xPolygon[nDot] + poly.getXBase());
			newDot.pos.y = (float) (yPolygon[nDot] + poly.getYBase());
			newDot.pos.z = (float) (frame * zScale - 1);
			dotsNewRegion.add(newDot);
		}
		return dotsNewRegion;
	}

	/**
	 * 
	 * @param frame
	 * @param poly
	 * @param shape
	 */
	public ArrayList<DotN> setOverlapRegion(int frame, PolygonRoi poly, ShapeRoi shape) {
		ArrayList<DotN> dotsNewRegion = new ArrayList<DotN>();
		int[] xPolygon = poly.getXCoordinates();
		int[] yPolygon = poly.getYCoordinates();
		for (int nDot = 0; nDot < xPolygon.length; nDot++) {
			DotN newDot = new DotN();
			newDot.pos.x = (float) (xPolygon[nDot] + shape.getXBase());
			newDot.pos.y = (float) (yPolygon[nDot] + shape.getYBase());
			newDot.pos.z = (float) (frame * zScale - 1);
			dotsNewRegion.add(newDot);
		}
		return dotsNewRegion;
	}

}
