
package PostProcessingGland.Elements;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang3.ArrayUtils;
import org.opensphere.geometry.algorithm.ConcaveHull;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
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
	
	public void removeOverlappingRegions(ArrayList<Cell3D> allCells, PolygonRoi newPolygon, int frame, String id, int threshold) {
		//zScale = z_scale;
		PolygonRoi newPolygonInterpolated = new PolygonRoi(newPolygon.getInterpolatedPolygon(2, false), 2);
		for (int nCell = 0; nCell < allCells.size(); nCell++) {
			float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(frame));
			float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(frame));
			
			ShapeRoi sNewPolygon = new ShapeRoi(newPolygon);
			ShapeRoi sNewPolygonBackUp = new ShapeRoi(sNewPolygon);
			
			ShapeRoi sOverlappingCell = new ShapeRoi(sNewPolygon);
			
			ShapeRoi overlappingZone = new ShapeRoi(sNewPolygon.and(sOverlappingCell));

			if ((overlappingZone.getFloatWidth() != 0 | overlappingZone.getFloatHeight() != 0)
					& allCells.get(nCell).id_Cell != id) {
				//ShapeRoi sNotOverlappingCell = new ShapeRoi(sOverlappingCell.not(sNewPolygonBackUp));
				PolygonRoi p= new PolygonRoi(sOverlappingCell.getContainedFloatPoints(),2);
				int[] xPolygon = p.getXCoordinates();
				int[] yPolygon = p.getYCoordinates();
				Roi[] overRois = new Roi[xPolygon.length];
				for (int nDot = 0; nDot < xPolygon.length; nDot++) {
					PointRoi r = new PointRoi(xPolygon[nDot],yPolygon[nDot]);
					overRois[nDot] = r;
				}
				
//				Roi[] overRois = preProcessingConcaveHull(sOverlappingCell);
				PolygonRoi polygon = getConcaveHull(overRois, threshold);
				
				float[] xPoints = new float[overRois.length];
				float[] yPoints = new float[overRois.length];

				for (int i = 0; i < overRois.length; i++) {
					xPoints[i] = (float) overRois[i].getXBase();
					yPoints[i] = (float) overRois[i].getYBase();
				}
				PolygonRoi poly = new PolygonRoi(xPoints, yPoints, 2); 
				// Convert the PolygonRoi in Dots and integrate with the dots of
				// the other frames.
				// Later, replace the selected cell by the cell with the new
				// region
			ArrayList<DotN> dotsNewRegion = RoisToDots(frame, overRois);	
			ArrayList<DotN> integratedDots = integrateNewRegion(dotsNewRegion, allCells.get(nCell).dotsList, frame);

				Cell3D newCell = new Cell3D(allCells.get(nCell).id_Cell, integratedDots);
				allCells.set(nCell, newCell);
			} else if (allCells.get(nCell).id_Cell == id) {
				selectedCell = nCell;
			}
		}
		
		// Replace the cell with the mistaken overlay by the new cell.
		ArrayList<DotN> dotsNewRegion = setNewRegion(frame, newPolygonInterpolated);
		ArrayList<DotN> integratedDots = integrateNewRegion(dotsNewRegion, allCells.get(selectedCell).dotsList, frame);
		Cell3D newCell = new Cell3D(id, integratedDots);
		allCells.set(selectedCell, newCell);

	}
	 
	/**
	 * @param xCell
	 * @param yCell
	 */
	public Roi[] getRois(int[] xCell, int[] yCell, Roi r) {
		Roi[] overRoi = new Roi[xCell.length];
		for (int nDot = 0; nDot < xCell.length; nDot++) {
			PointRoi point = new PointRoi(xCell[nDot] +(int) r.getXBase(),yCell[nDot] + (int) r.getYBase());
			overRoi[nDot] = point;
		}
		return overRoi;
	}

	/**
	 * @param shapeRoi
	 * @return
	 */
	public Roi[] preProcessingConcaveHull(ShapeRoi shape) {
		PolygonRoi polygon = new PolygonRoi(shape.getContainedFloatPoints(),6);
		
		/*System.out.println("S not");
		FloatPolygon p = polygon.getFloatPolygon();
		for(int i=0; i<p.npoints;i++)
		{
			System.out.println(p.xpoints[i]+" "+p.ypoints[i]);
		}*/
		ArrayList<Roi> overRoiList = new ArrayList<Roi>();
		Rectangle mask = new Rectangle(polygon.getBounds());

			for (int x = 0; x < mask.getWidth(); x++) {
				 for (int y = 0; y < mask.getHeight(); y++) {
					 if (polygon.contains(x+mask.getLocation().x, y+mask.getLocation().y)) {
						PointRoi point = new PointRoi((x + mask.getLocation().x), (y + mask.getLocation().y));
						overRoiList.add(point);
					}
				}
				
			}
		
		Roi[] overRois = overRoiList.toArray(new Roi[overRoiList.size()]);
		return overRois;
	}	
	
	// ConcaveHull algoritm
	public PolygonRoi getConcaveHull(Roi[] rois, double threshold) {
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
 * @param rois
 * @return
 */
	public ArrayList<DotN> RoisToDots(int frame, Roi[] rois) {
		ArrayList<DotN> dotsNewRegion = new ArrayList<DotN>();
		for (int nDot = 0; nDot < rois.length; nDot++) {
			DotN newDot = new DotN();
			newDot.pos.x = (float) (rois[nDot].getXBase());
			newDot.pos.y = (float) (rois[nDot].getYBase());
			newDot.pos.z = (float) (frame * zScale - 1);
			dotsNewRegion.add(newDot);
		}
		return dotsNewRegion;
	}
	
	//Calculate the ecuclidean distance
	public float distEu (float x2,float x1, float y2, float y1)
	{
		float disEu = 0;
			disEu = (float) Math.sqrt(Math.pow((x2-x1), 2) + Math.pow((y2-y1), 2));
		return disEu;
	}
	
	//If the dots are less than 2 return the same polygon
	public PolygonRoi getOrderDots(PolygonRoi poly)
	{
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

		return orderDots(poly);
	}
	
	
	//Order the dots according there euclidean distance
	public PolygonRoi orderDots(PolygonRoi poly)
	{
		FloatPolygon p = poly.getFloatPolygon();
		float[] x = p.xpoints;
		float[] y = p.ypoints;
		int dis = p.npoints;
		float disEu[] = new float[dis];
		float[] NAX = new float[dis];
		float[] NAY = new float[dis];
		int pos = 0;
		for(int i = 0; i < dis; i++)
		{
			if(i == 0)
			{
				NAX[i] = x[i];
				NAY[i] = y[i];
				x[i] = 0;
				y[i] = 0;
			}
			else
			{
				for(int k = 0; k < dis; k++)
					disEu[k] = distEu(x[k],NAX[i-1],y[k],NAY[i-1]);
				float men = disEu[0];
				for(int k = 0; k < dis; k++)
				{
					float comp = disEu[k];
					if(men > comp)
					{
						men = comp;
						pos = k;
					}
				}
				NAX[i] = x[pos];
				NAY[i] = y[pos];
				x[pos] = 0;
				y[pos] = 0;
			}
				
		}
		
		float sum = 0;
		for (int i = 0; i < dis; i++)
		{
			if (i == dis-1)
			{
				disEu[i] = distEu(NAX[0],NAX[i],NAY[0],NAY[i]);
				sum = sum + disEu[i];
			}
			else
			{
				disEu[i] = distEu(NAX[i+1],NAX[i],NAY[i+1],NAY[i]);	
				sum = sum + disEu[i];
			}
		}
		float av = (sum/dis);
		int fin = dis-1;
		float cond = 0;
		if(av > 5)
			cond = av*5;
		else
			cond = av*10;
		for(int j = 0; j < fin; j++)
		{
			if(disEu[fin] > cond)
			{
				for (int i = 0; i < dis; i++)
				{
					if (i == fin)
						disEu[i] = distEu(NAX[0],NAX[fin],NAY[0],NAY[fin]);
					else
						disEu[i] = distEu(NAX[i],NAX[fin],NAY[i],NAY[fin]);
				}
				float men = disEu[0];
				pos = 0;
				for(int k = 0; k < dis; k++)
				{
					float comp = disEu[k];
					if(men > comp && k != fin-1)
					{
						men = comp;
						pos = k;
					}
				}
				float X = NAX[fin];
				float Y = NAY[fin];
				for(int i = fin; i > pos+1; i--)
				{
					NAX[i] = NAX[i-1];
					NAY[i] = NAY[i-1];
				}
				NAX[pos+1] = X;
				NAY[pos+1] = Y;
				for (int i = 0; i < dis; i++)
				{
					if (i == dis-1)
						disEu[i] = distEu(NAX[0],NAX[i],NAY[0],NAY[i]);
					else
						disEu[i] = distEu(NAX[i+1],NAX[i],NAY[i+1],NAY[i]);	
	
				}
			}
		}
		//for(int i = 0; i < dis; i++)
		//	System.out.println(NAX[i] + " " + NAY[i]);
		PolygonRoi pol = new PolygonRoi(NAX,NAY,dis,2);
		PolygonRoi postPol = new PolygonRoi(pol.getInterpolatedPolygon(2, true),2);
		return postPol;
	}
	
}
