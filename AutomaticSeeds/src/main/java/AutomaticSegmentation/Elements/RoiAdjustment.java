
package AutomaticSegmentation.Elements;

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
	
	public ArrayList<Cell3D> removeOverlappingRegions(ArrayList<Cell3D> allCells, PolygonRoi newPolygon, int frame, String id, PolygonRoi[][] lumen) 
	{
		//fill the selection with points
		PolygonRoi newPolygonInterpolated = new PolygonRoi(newPolygon.getInterpolatedPolygon(2, false), 2);
		//star to read all the cells
		for (int nCell = 0; nCell < allCells.size(); nCell++) 
		{		
			//if the cell is not empty in the frame do the calculation
			if (allCells.get(nCell).getCell3DAt(frame).size() > 0)
			{
				//get the x,y points of the cell
				float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(frame));
				float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(frame));
				
				//create the shape of the cell
				PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 6);
				ShapeRoi s = new ShapeRoi(overlappingCell);
				ShapeRoi r = new ShapeRoi(overlappingCell);
				//create the shape of the selection
				ShapeRoi sNewPolygon = new ShapeRoi(newPolygon);
				
				ShapeRoi sOverlappingCell = new ShapeRoi(sNewPolygon);
				//verify if the selection is contain in the current cell with and
				ShapeRoi overlappingZone = new ShapeRoi(sNewPolygon.and(s));
				//if the cell share the space width o height will be different to 0
				//also verify the cell is different of the cell to change
				if ((overlappingZone.getFloatWidth() != 0 | overlappingZone.getFloatHeight() != 0)
						& allCells.get(nCell).id_Cell != id) 
					{
					//if the cell share space, function not will return the new polygon of the cell 
					//without share points
					PolygonRoi polygon = new PolygonRoi(r.not(sOverlappingCell).getContainedFloatPoints(),6);					
					
					Roi[] overRoi = getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
					//get the border with ConcaveHull and threshold as 1 to have all the points															
					PolygonRoi poly = getConcaveHull(overRoi,1);	
					// Convert the PolygonRoi in Dots and integrate with the dots of
					// the other frames.
					// Later, replace the selected cell by the cell with the new
					// region
					ArrayList<DotN> dotsNewRegion = setNewRegion(frame, poly);	
					ArrayList<DotN> integratedDots = integrateNewRegion(dotsNewRegion, allCells.get(nCell).dotsList, frame);
	
					Cell3D newCell = new Cell3D(allCells.get(nCell).id_Cell, integratedDots);
					allCells.set(nCell, newCell);
				} 
				else if (allCells.get(nCell).id_Cell == id)
				{
					//if the cell is the same only save the value in selectedCell to add later
					selectedCell = nCell;
				}
			}
		}
		// Replace the cell with the mistaken overlay by the new cell.
		ArrayList<DotN> dotsNewRegion = setNewRegion(frame, newPolygonInterpolated);
		ArrayList<DotN> integratedDots = integrateNewRegion(dotsNewRegion, allCells.get(selectedCell).dotsList, frame);
		Cell3D newCell = new Cell3D(id, integratedDots);
		allCells.set(selectedCell, newCell);
		//verify if the frame has lumen and do the fuction to remove lumen overlaps
		if (lumen[frame-1] != null)
			removeLumenOverlap(allCells, frame, lumen);	
		return allCells;
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
	
	//Calculate the euclidean distance between two points
	//x1 is the current point, x2 is the next point
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
		//if dots are more than 2 return orderDots
		return orderDots(poly);
	}
	
	
	//Order the dots according there euclidean distance
	public PolygonRoi orderDots(PolygonRoi poly)
	{
		//get the x,y points from poly
		FloatPolygon p = poly.getFloatPolygon();
		float[] x = p.xpoints;
		float[] y = p.ypoints;
		int dis = p.npoints;
		float disEu[] = new float[dis];
		float[] NAX = new float[dis];
		float[] NAY = new float[dis];
		int pos = 0;
		//order the dots with the nearest points
		for(int i = 0; i < dis; i++)
		{
			//start the cicle with the same first point 
			if(i == 0)
			{
				NAX[i] = x[i];//migrate the first position to new array
				NAY[i] = y[i];
				x[i] = 0; //empty the old position
				y[i] = 0;
			}
			else
			{	
				//calculate the distance between the current point (i) with the next point (k)
				for(int k = 0; k < dis; k++)
					disEu[k] = distEu(x[k],NAX[i-1],y[k],NAY[i-1]);
				float men = disEu[0];
				//calculate the less distance and save the position
				for(int k = 0; k < dis; k++)
				{
					float comp = disEu[k];
					if(men > comp)
					{
						men = comp;
						pos = k;
					}
				}
				//put in the new array the point with less distances 
				NAX[i] = x[pos];
				NAY[i] = y[pos];
				//erase the number from the old array
				x[pos] = 0;
				y[pos] = 0;
			}
				
		}
		
		//order the final dots
		float sum = 0;
		//calculate all the distances and sum the values
		for (int i = 0; i < dis; i++)
		{
			if (i == dis-1)
			{
				//the last distance is compared with the first position to verify if the polygon is closed
				disEu[i] = distEu(NAX[0],NAX[i],NAY[0],NAY[i]);
				sum = sum + disEu[i];
			}
			else
			{
				disEu[i] = distEu(NAX[i+1],NAX[i],NAY[i+1],NAY[i]);	
				sum = sum + disEu[i];
			}
		}
		//calculate the distance average
		float av = (sum/dis);
		int fin = dis-1;
		float cond = 0;
		//create a condition if the average is bigger than 5 multiply for 5
		//with this calculation we define the less distance around 25
		if(av > 5)
			cond = av*5;
		else
			cond = av*10;
		//check and order all the points until the polygon is closed
		//if the polygon never close the cicle have limit of tries to not create a infinite cicle
		for(int j = 0; j < fin; j++)
		{
			//verify if the last point is separate to the initial (polygon doesn't closed)
			if(disEu[fin] > cond)
			{
				//if the polygon is not close, calculate the distance again to find the nearest point
				for (int i = 0; i < dis; i++)
				{
					if (i == fin)
						disEu[i] = distEu(NAX[0],NAX[fin],NAY[0],NAY[fin]);
					else
						disEu[i] = distEu(NAX[i],NAX[fin],NAY[i],NAY[fin]);
				}
				float men = disEu[0];
				pos = 0;
				//looking for the nearest point
				for(int k = 0; k < dis; k++)
				{
					float comp = disEu[k];
					if(men > comp && k != fin-1)
					{
						men = comp;
						pos = k;
					}
				}
				// X and Y will save the last position to insert in the new one
				float X = NAX[fin];
				float Y = NAY[fin];
				//Move the numbers to delate the last position 
				//the position now is empty to insert the last number
				for(int i = fin; i > pos+1; i--)
				{
					NAX[i] = NAX[i-1];
					NAY[i] = NAY[i-1];
				}
				//Insert the last position in the new position after the nearest point
				NAX[pos+1] = X;
				NAY[pos+1] = Y;
				//calculate again the distance to verify if the polygon is closed and repeat
				for (int i = 0; i < dis; i++)
				{
					if (i == dis-1)
						disEu[i] = distEu(NAX[0],NAX[i],NAY[0],NAY[i]);
					else
						disEu[i] = distEu(NAX[i+1],NAX[i],NAY[i+1],NAY[i]);	
	
				}
			}
		}
		//transform the array to polygon
		PolygonRoi pol = new PolygonRoi(NAX,NAY,dis,2);
		//fill the distance between points with more points to define correctly the polygon
		PolygonRoi postPol = new PolygonRoi(pol.getInterpolatedPolygon(2, true),2);
		return postPol;
	}
	
	public void removeLumenOverlap(ArrayList<Cell3D> allCells, int frame, PolygonRoi[][] lumen)
	{
		//start to verify all the cells
		for (int nCell = 0; nCell < allCells.size(); nCell++) 
		{
			//only will do if the frame have lumen and the cell size is major to 0 
			if (lumen[frame-1] != null & allCells.get(nCell).getCell3DAt(frame).size() > 0) 
			{
				//get the x, y points of the current cell
				float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell).getCell3DAt(frame));
				float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell).getCell3DAt(frame));
				//create the polygon and the shape with the curren cell			
				PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 6);
				ShapeRoi r = new ShapeRoi(overlappingCell);
				ShapeRoi s = new ShapeRoi(overlappingCell);
				ShapeRoi s2 = new ShapeRoi(overlappingCell);
				//verify if lumen have 2 polygon
				if(lumen[frame-1][1] != null)
				{
					//transform the lumen to polygon and shape
					java.awt.Polygon l = lumen[frame-1][0].getPolygon();
					ShapeRoi lum = new ShapeRoi(l);
					//verify if the cell overlap with the lumen
					s.and(lum);
					java.awt.Polygon l2 = lumen[frame-1][1].getPolygon();
					ShapeRoi lum2 = new ShapeRoi(l2);
					s2.and(lum2);	
					//if the cell overlap with lumen width and height must be different to 0 
					//this step will do twice for each part of the lumen
					if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0) 
						{
							//get the point without lumen
							//not return all the points not contained in lumen
							PolygonRoi polygon = new PolygonRoi(r.not(lum).getContainedFloatPoints(),6);					
								
							Roi[] overRoi = getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
							//get the border of the new polygon															
							PolygonRoi poly = getConcaveHull(overRoi,1);	
							//save the new polygon as dots
							ArrayList<DotN> dotsNewRegion1 = setNewRegion(frame, poly);
							ArrayList<DotN> integratedDots1 = integrateNewRegion(dotsNewRegion1,
									allCells.get(nCell).dotsList, frame);
									
							Cell3D newCell1 = new Cell3D(allCells.get(nCell).id_Cell, integratedDots1);
							allCells.set(nCell, newCell1);
							
						}
					//repeat the same action as above
					if (s2.getFloatWidth() != 0 | s2.getFloatHeight() != 0)
							{						
								PolygonRoi polygon2 = new PolygonRoi(r.not(lum2).getContainedFloatPoints(),6);	
									
								Roi[] overRoi2 = getRois(polygon2.getXCoordinates(), polygon2.getYCoordinates(), polygon2);
									
								PolygonRoi poly2 = getConcaveHull(overRoi2,1);
									
								ArrayList<DotN> dotsNewRegion2 = setNewRegion(frame, poly2);
								ArrayList<DotN> integratedDots2 = integrateNewRegion(dotsNewRegion2,
										allCells.get(nCell).dotsList, frame);
									
								Cell3D newCell2 = new Cell3D(allCells.get(nCell).id_Cell, integratedDots2);
								allCells.set(nCell, newCell2);
							}
					
				}	
				//if the lumen only have one polygon do the same action as above once
				else if(lumen[frame-1][0] != null)
				{
					java.awt.Polygon l1 = lumen[frame-1][0].getPolygon();											
					ShapeRoi lum1 = new ShapeRoi(l1);
					s.and(lum1);					
					
					if (s.getFloatWidth() != 0 | s.getFloatHeight() != 0)
					{
						PolygonRoi polygon = new PolygonRoi(r.not(lum1).getContainedFloatPoints(),6);
							
						Roi[] overRoi = getRois(polygon.getXCoordinates(), polygon.getYCoordinates(), polygon);
							
						PolygonRoi poly = getConcaveHull(overRoi,1);							
							
						ArrayList<DotN> dotsNewRegion2 = setNewRegion(frame, poly);
							
						ArrayList<DotN> integratedDots2 = integrateNewRegion(dotsNewRegion2,
								allCells.get(nCell).dotsList, frame);
															

						Cell3D newCell2 = new Cell3D(allCells.get(nCell).id_Cell, integratedDots2);
						allCells.set(nCell, newCell2);

					
					}	
				}
			}
		}
	}
	
}
