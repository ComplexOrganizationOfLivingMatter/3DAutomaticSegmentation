
package AutomaticSegmentation.elements;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opensphere.geometry.algorithm.ConcaveHull;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;
import eu.kiaru.limeseg.struct.Vector3D;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.process.FloatPolygon;

/**
 * 
 * @author
 *
 */
public class RoiAdjustment {

	/**
	 * fill the selection with points
	 * @param allCells
	 * @param newPolygon
	 * @param frame
	 * @param id
	 */
	public static ArrayList<Cell3D> removeOverlappingRegions(ArrayList<Cell3D> allCells, PolygonRoi newPolygon,
			int frame, String id, PolygonRoi[][] lumen, float zScale) {
		// TODO: SIMPLIFY FUNCTION
		
		PolygonRoi newPolygonInterpolated = new PolygonRoi(newPolygon.getInterpolatedPolygon(2, false), 2);
		int selectedCell = -1;
		// star to read all the cells
		for (int nCell = 0; nCell < allCells.size(); nCell++) {
			// if the cell is not empty in the frame do the calculation
			if (allCells.get(nCell).getCell3DAt(frame).size() > 0) {
				// get the x,y points of the cell
				float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(frame));
				float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(frame));

				// create the shape of the cell
				ShapeRoi overlappingCell = new ShapeRoi(new PolygonRoi(xCell, yCell, 6));
				// create the shape of the selection
				ShapeRoi modifiedCell = new ShapeRoi(newPolygon);
				// verify if the selection is contain in the current cell with
				// and
				ShapeRoi overlappingZone = new ShapeRoi(modifiedCell.and(overlappingCell));
				// if the cell share the space width o height will be different
				// to 0 also verify the cell is different of the cell to change
				if ((overlappingZone.getFloatWidth() != 0 | overlappingZone.getFloatHeight() != 0)
						& allCells.get(nCell).id_Cell != id) {
					// if the cell share space, function not will return the new
					// polygon of the cell without share points
					PolygonRoi polygonNoModifiedCell = new PolygonRoi(overlappingCell.not(modifiedCell).getContainedFloatPoints(), 6);

					// get the border with ConcaveHull and threshold as 1 to
					// have all the points
					// Convert the PolygonRoi in Dots and integrate with the dots of the other frames.
					// Later, replace the selected cell by the cell with the new region
					PolygonRoi polygon = new PolygonRoi(getConcaveHull(polygonNoModifiedCell, 1).getInterpolatedPolygon(1, false), 2);
					Roi[] allRois = RoiAdjustment.getAsRoiPoints(polygon);
					allCells.get(nCell).getDotsPerSlice()[frame] = RoiAdjustment.RoisToDots(frame, allRois, zScale, allCells.get(nCell).cellTs.get(0));

				} else if (allCells.get(nCell).id_Cell == id) {
					// if the cell is the same only save the value in
					// selectedCell to add later
					selectedCell = nCell;
				}
			}
		}
		// Replace the cell with the mistaken overlay by the new cell.
		Roi[] allRois = RoiAdjustment.getAsRoiPoints(newPolygonInterpolated);
		allCells.get(selectedCell).getDotsPerSlice()[frame] = RoiAdjustment.RoisToDots(frame, allRois, zScale, allCells.get(selectedCell).cellTs.get(0));
		// verify if the frame has lumen and do the fuction to remove lumen
		// overlaps
		if (lumen != null)
			removeLumenOverlap(allCells, frame, lumen, zScale);

		return allCells;
	}

	/**
	 * create a Roi with the polygon from orderDots
	 * @param xCell
	 * @param yCell
	 */
	public static Roi[] getAsRoiPoints(PolygonRoi r) {
		int[] xCell = r.getXCoordinates();
		int[] yCell = r.getYCoordinates();
		Roi[] overRoi = new Roi[xCell.length];
		for (int nDot = 0; nDot < xCell.length; nDot++) {
			PointRoi point = new PointRoi(xCell[nDot] + (int) r.getXBase(), yCell[nDot] + (int) r.getYBase());
			overRoi[nDot] = point;
		}
		return overRoi;
	}

	/**
	 * @param shapeRoi
	 * @return
	 */
	public static Roi[] preProcessingConcaveHull(ShapeRoi shape) {
		PolygonRoi polygon = new PolygonRoi(shape.getContainedFloatPoints(), 6);
		ArrayList<Roi> overRoiList = new ArrayList<Roi>();
		Rectangle mask = new Rectangle(polygon.getBounds());

		for (int x = 0; x < mask.getWidth(); x++) {
			for (int y = 0; y < mask.getHeight(); y++) {
				if (polygon.contains(x + mask.getLocation().x, y + mask.getLocation().y)) {
					PointRoi point = new PointRoi((x + mask.getLocation().x), (y + mask.getLocation().y));
					overRoiList.add(point);
				}
			}

		}

		Roi[] overRois = overRoiList.toArray(new Roi[overRoiList.size()]);
		return overRois;
	}

	// ConcaveHull algoritm
	public static PolygonRoi getConcaveHull(PolygonRoi polygon, double threshold) {
		Roi[] rois = RoiAdjustment.getAsRoiPoints(polygon);
		GeometryFactory geoF = new GeometryFactory();
		com.vividsolutions.jts.geom.Point[] allPoints = new com.vividsolutions.jts.geom.Point[rois.length];

		for (int i = 0; i < rois.length; i++) {
			Coordinate coordinate = new Coordinate(rois[i].getXBase(), rois[i].getYBase());
			allPoints[i] = geoF.createPoint(coordinate);
		}

		ArrayList<com.vividsolutions.jts.geom.Point> geoList = new ArrayList<com.vividsolutions.jts.geom.Point>(
				Arrays.asList(allPoints));

		Geometry geoP = geoF.buildGeometry(geoList);

		ConcaveHull ch = new ConcaveHull(geoP, threshold);

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
	public static ArrayList<DotN> integrateNewRegion(ArrayList<DotN> newDots, ArrayList<DotN> oldDots, int frame,
			float zScale) {
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

	// Setter method.

	/**
	 * 
	 * @param frame
	 *            z slice which is modified
	 * @param poly:
	 *            PolygonRoi which represent the new Region
	 */
	public static ArrayList<DotN> setNewRegion(int frame, PolygonRoi poly, float zScale) {
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
	 * @param rois
	 * @param cellT 
	 * @return
	 */
	public static ArrayList<DotN> RoisToDots(int frame, Roi[] rois, float zScale, CellT cellT) {
		ArrayList<DotN> dotsNewRegion = new ArrayList<DotN>();
		for (int nDot = 0; nDot < rois.length; nDot++) {
			DotN newDot = new DotN();
			newDot.pos.x = (float) (rois[nDot].getXBase());
			newDot.pos.y = (float) (rois[nDot].getYBase());
			newDot.pos.z = (float) (frame * zScale - 1);
			newDot.ct = cellT;
			dotsNewRegion.add(newDot);
		}
		return dotsNewRegion;
	}

	// Calculate the euclidean distance between two points
	// x1 is the current point, x2 is the next point
	public static float distEu(float x2, float x1, float y2, float y1) {
		float disEu = 0;
		disEu = (float) Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
		return disEu;
	}

	// If the dots are less than 2 return the same polygon
	public static PolygonRoi getOrderDots(PolygonRoi poly) {
		FloatPolygon p = poly.getFloatPolygon();
		int dis = p.npoints;
		if (dis == 0) {
			return poly;
		}
		if (dis == 1) {
			return poly;
		}
		if (dis == 2) {
			return poly;
		}
		// if dots are more than 2 return orderDots
		return orderDots(poly);
	}

	// Order the dots according there euclidean distance
	public static PolygonRoi orderDots(PolygonRoi poly) {
		// get the x,y points from poly
		FloatPolygon p = poly.getFloatPolygon();
		float[] x = p.xpoints;
		float[] y = p.ypoints;
		int dis = p.npoints;
		float disEu[] = new float[dis];
		float[] NAX = new float[dis];
		float[] NAY = new float[dis];
		int pos = 0;
		// order the dots with the nearest points
		for (int i = 0; i < dis; i++) {
			// start the cicle with the same first point
			if (i == 0) {
				NAX[i] = x[i];// migrate the first position to new array
				NAY[i] = y[i];
				x[i] = 0; // empty the old position
				y[i] = 0;
			} else {
				// calculate the distance between the current point (i) with the
				// next point (k)
				for (int k = 0; k < dis; k++)
					disEu[k] = distEu(x[k], NAX[i - 1], y[k], NAY[i - 1]);
				float men = disEu[0];
				// calculate the less distance and save the position
				for (int k = 0; k < dis; k++) {
					float comp = disEu[k];
					if (men > comp) {
						men = comp;
						pos = k;
					}
				}
				// put in the new array the point with less distances
				NAX[i] = x[pos];
				NAY[i] = y[pos];
				// erase the number from the old array
				x[pos] = 0;
				y[pos] = 0;
			}

		}

		// order the final dots
		float sum = 0;
		// calculate all the distances and sum the values
		for (int i = 0; i < dis; i++) {
			if (i == dis - 1) {
				// the last distance is compared with the first position to
				// verify if the polygon is closed
				disEu[i] = distEu(NAX[0], NAX[i], NAY[0], NAY[i]);
				sum = sum + disEu[i];
			} else {
				disEu[i] = distEu(NAX[i + 1], NAX[i], NAY[i + 1], NAY[i]);
				sum = sum + disEu[i];
			}
		}
		// calculate the distance average
		float av = (sum / dis);
		int fin = dis - 1;
		float cond = 0;
		// create a condition if the average is bigger than 5 multiply for 5
		// with this calculation we define the less distance around 25
		if (av > 5)
			cond = av * 5;
		else
			cond = av * 10;
		// check and order all the points until the polygon is closed
		// if the polygon never close the cicle have limit of tries to not
		// create a infinite cicle
		for (int j = 0; j < fin; j++) {
			// verify if the last point is separate to the initial (polygon
			// doesn't closed)
			if (disEu[fin] > cond) {
				// if the polygon is not close, calculate the distance again to
				// find the nearest point
				for (int i = 0; i < dis; i++) {
					if (i == fin)
						disEu[i] = distEu(NAX[0], NAX[fin], NAY[0], NAY[fin]);
					else
						disEu[i] = distEu(NAX[i], NAX[fin], NAY[i], NAY[fin]);
				}
				float men = disEu[0];
				pos = 0;
				// looking for the nearest point
				for (int k = 0; k < dis; k++) {
					float comp = disEu[k];
					if (men > comp && k != fin - 1) {
						men = comp;
						pos = k;
					}
				}
				// X and Y will save the last position to insert in the new one
				float X = NAX[fin];
				float Y = NAY[fin];
				// Move the numbers to delate the last position
				// the position now is empty to insert the last number
				for (int i = fin; i > pos + 1; i--) {
					NAX[i] = NAX[i - 1];
					NAY[i] = NAY[i - 1];
				}
				// Insert the last position in the new position after the
				// nearest point
				NAX[pos + 1] = X;
				NAY[pos + 1] = Y;
				// calculate again the distance to verify if the polygon is
				// closed and repeat
				for (int i = 0; i < dis; i++) {
					if (i == dis - 1)
						disEu[i] = distEu(NAX[0], NAX[i], NAY[0], NAY[i]);
					else
						disEu[i] = distEu(NAX[i + 1], NAX[i], NAY[i + 1], NAY[i]);

				}
			}
		}
		// transform the array to polygon
		PolygonRoi pol = new PolygonRoi(NAX, NAY, dis, 2);
		// fill the distance between points with more points to define correctly
		// the polygon
		PolygonRoi postPol = new PolygonRoi(pol.getInterpolatedPolygon(2, true), 2);
		return postPol;
	}

	/**
	 * 
	 * @param allCells
	 * @param frame
	 * @param lumen
	 * @param zScale
	 */
	public static void removeLumenOverlap(ArrayList<Cell3D> allCells, int frame, PolygonRoi[][] lumen, float zScale) {
		// TODO: SIMPLIFY FUNCTION
		
		// start to verify all the cells
		for (int nCell = 0; nCell < allCells.size(); nCell++) {
			// only will do if the frame have lumen and the cell size is major
			// to 0
			if (lumen[frame - 1] != null & allCells.get(nCell).getCell3DAt(frame).size() > 0) {
				// get the x, y points of the current cell
				float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(frame));
				float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(frame));
				// create the polygon and the shape with the curren cell
				PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 6);
				ShapeRoi r = new ShapeRoi(overlappingCell);
				ShapeRoi s = new ShapeRoi(overlappingCell);
				ShapeRoi s2 = new ShapeRoi(overlappingCell);
				// verify if lumen have 2 polygon
				if (lumen[frame - 1][1] != null) {
					// transform the lumen to polygon and shape
					java.awt.Polygon l = lumen[frame - 1][0].getPolygon();
					ShapeRoi lum = new ShapeRoi(l);
					// verify if the cell overlap with the lumen
					s.and(lum);
					java.awt.Polygon l2 = lumen[frame - 1][1].getPolygon();
					ShapeRoi lum2 = new ShapeRoi(l2);
					s2.and(lum2);
					// if the cell overlap with lumen width and height must be
					// different to 0
					// this step will do twice for each part of the lumen
					if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0) {
						// get the point without lumen
						// not return all the points not contained in lumen
						PolygonRoi polygon = new PolygonRoi(r.not(lum).getContainedFloatPoints(), 6);

						// get the border of the new polygon
						PolygonRoi poly = getConcaveHull(polygon, 1);
						// save the new polygon as dots
						ArrayList<DotN> dotsNewRegion1 = setNewRegion(frame, poly, zScale);
						allCells.get(nCell).getDotsPerSlice()[frame] = dotsNewRegion1;

					}
					// repeat the same action as above
					if (s2.getFloatWidth() != 0 | s2.getFloatHeight() != 0) {
						PolygonRoi polygon2 = new PolygonRoi(r.not(lum2).getContainedFloatPoints(), 6);

						PolygonRoi poly2 = getConcaveHull(polygon2, 1);

						ArrayList<DotN> dotsNewRegion2 = setNewRegion(frame, poly2, zScale);
						allCells.get(nCell).getDotsPerSlice()[frame] = dotsNewRegion2;
					}

				}
				// if the lumen only have one polygon do the same action as
				// above once
				else if (lumen[frame - 1][0] != null) {
					java.awt.Polygon l1 = lumen[frame - 1][0].getPolygon();
					ShapeRoi lum1 = new ShapeRoi(l1);
					s.and(lum1);

					if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0) {
						PolygonRoi polygon = new PolygonRoi(r.not(lum1).getContainedFloatPoints(), 6);

						PolygonRoi poly = getConcaveHull(polygon, 1);

						ArrayList<DotN> dotsNewRegion2 = setNewRegion(frame, poly, zScale);
						allCells.get(nCell).getDotsPerSlice()[frame] = dotsNewRegion2;

					}
				}
			}
		}
	}

	public static TextRoi[][] getLabelCells(ArrayList<Cell3D> allCells, ImagePlus imp) {
		TextRoi[][] textRois = new TextRoi[allCells.size()][imp.getStackSize()];
		for (int nSlice = 0; nSlice < imp.getStackSize(); nSlice++) {
			for (int nCell = 0; nCell < allCells.size(); nCell++) {
				// if the cell is not empty in the frame do the calculation
				if (allCells.get(nCell).getCell3DAt(nSlice).size() > 0) {
					// get the x,y points of the cell
					float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(nSlice));
					float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(nSlice));

					// create the shape of the cell
					PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 6);
					double[] centroid = overlappingCell.getContourCentroid();
					double pepe = centroid[0];
					double pepe2 = centroid[1];
					if (centroid[0] > 1 & centroid[1] > 1) {
						TextRoi labelCell = new TextRoi(centroid[0], centroid[1], Integer.toString(nCell + 1));
						labelCell.setColor(Color.WHITE);
						labelCell.setLocation(centroid[0] - (labelCell.getFloatWidth() / 2),
								centroid[1] - (labelCell.getFloatHeight() / 2));
						textRois[nCell][nSlice] = labelCell;

					} else {
						textRois[nCell][nSlice] = null;
					}

				} else {
					textRois[nCell][nSlice] = null;
				}
			}
		}
		return textRois;
	}
	
	/***
	 * Interpolating method from 
	 * https://stackoverflow.com/questions/30182467/how-to-implement-linear-interpolation-method-in-java-array
	 * @param start start of the interval
	 * @param end end of the interval
	 * @param count count of output interpolated numbers
	 * @return array of interpolated number with specified count
	 */
	public static ArrayList<DotN> interpolate(Vector3D start, Vector3D end, int count) {
	    if (count < 2) {
	        throw new IllegalArgumentException("interpolate: illegal count!");
	    }
	    
	    ArrayList<DotN> middleInterpolatedDots = new ArrayList<DotN>();
	    
	    Vector3D dirVector = Vector3D.VecteurDir(end, start);
	    Vector3D interpolatedDot;
	    for (int numPlane = 0; numPlane <= count; ++ numPlane) {
	    	interpolatedDot = new Vector3D((numPlane / count) * dirVector.x, (numPlane / count) * dirVector.y, (numPlane / count) * dirVector.z);
	    	middleInterpolatedDots.add(new DotN(Vector3D.sum(start, interpolatedDot), new Vector3D(0, 0, 0)));
	    }
	    return middleInterpolatedDots;
	}
}
