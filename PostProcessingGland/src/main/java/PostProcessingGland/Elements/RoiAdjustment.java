
package PostProcessingGland.Elements;

import com.github.quickhull3d.Point3d;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.stream.IntStream;

import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;

import org.apache.commons.lang3.ArrayUtils;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import eu.kiaru.limeseg.struct.DotN;

public class RoiAdjustment {

	public ArrayList<DotN> dotsNewRegion;
	public float zScale;
	public int selectedCell;

	

	/**
	 * 
	 * @param newDots dots that represent only the new Region
	 * @param oldDots dots that represent the old Cell3D 
	 * @param frame
	 * @return all the dots which will represent the new Cell3D: (newDots + oldDots - oldDots(frame))
	 */
	public ArrayList<DotN> integrateNewRegion(ArrayList<DotN> newDots,
		ArrayList<DotN> oldDots, int frame)
	{
		ArrayList<DotN> currentDots = new ArrayList<DotN>();
		int pepe = oldDots.size();
		for (int i = 0; i < oldDots.size(); i++) {
			DotN dot = oldDots.get(i);
			int zpos = 1 + (int) ((float) (dot.pos.z / (float) 4.06));
			if ((frame - 1) > zpos | zpos > (frame + 1)) {
				currentDots.add(dot);
			}
		}
		for (int j = 0; j < newDots.size(); j++) {
			currentDots.add(newDots.get(j));
		}
		return currentDots;

	}

	public void removeOverlappingRegions(ArrayList<Cell3D> allCells,
		PolygonRoi newPolygon, int frame, String id)
	{
		for (int nCell = 0; nCell < allCells.size(); nCell++) {
			float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell)
				.getCell3DAt(frame));
			float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell)
				.getCell3DAt(frame));

			PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 2);
			
			ShapeRoi s1 = new ShapeRoi(newPolygon);
			ShapeRoi s2 = new ShapeRoi(overlappingCell);
			ShapeRoi overlappingZone = s1.and(s2);
			if ((overlappingZone.getFloatWidth() != 0 | overlappingZone.getFloatHeight() != 0) & allCells.get(nCell).id_Cell != id ) {
				s2.xor(overlappingZone); 
				PolygonRoi[] overRoi = (PolygonRoi[]) s2.getRois();
				
				this.setOverlapRegion(frame, overRoi);
				ArrayList<DotN> integratedDots = new ArrayList<DotN>(this.integrateNewRegion(
					this.dotsNewRegion, allCells.get(nCell).dotsList, frame));

				Cell3D newCell = new Cell3D(allCells.get(nCell).id_Cell,integratedDots) ;
				allCells.set(nCell, newCell);
			}
			else if (allCells.get(nCell).id_Cell == id) {
				selectedCell = nCell;
			}
		}
		this.setNewRegion(frame, newPolygon);
		ArrayList<DotN> integratedDots = new ArrayList<DotN>(this.integrateNewRegion(
			this.dotsNewRegion, allCells.get(selectedCell).dotsList, frame));
		Cell3D newCell = new Cell3D(id, integratedDots);
		allCells.set(selectedCell, newCell);
	}

	
	
	 /* public void convertPointsInDots(ArrayList<Point3d> points) {
		// ArrayList<Point3d> convexPoints = new
		// ArrayList<Point3d>(Arrays.asList(points));
		convexHullDots = new ArrayList<DotN>();
		for (int nPoint = 0; nPoint < points.size(); nPoint++) {
			DotN dot = new DotN();
			dot.pos.x = (float) points.get(nPoint).x;
			dot.pos.y = (float) points.get(nPoint).y;
			dot.pos.z = (float) points.get(nPoint).z;
			convexHullDots.add(dot);
		}
	}
	*/
	
	// Getter method.
	
	// Get the coordinates of new region in dots
	public ArrayList<DotN> getNewRegion() {
		return this.dotsNewRegion;
	}	
	
	// Setter method.
	
	/**
	 * 
	 * @param frame z slice which is modified
	 * @param poly: PolygonRoi which represent the new Region
	 */
	public void setNewRegion(int frame, PolygonRoi poly) {
		dotsNewRegion = new ArrayList<DotN>();
		zScale = (float) 4.06;
		int[] xPolygon = poly.getXCoordinates();
		int[] yPolygon = poly.getYCoordinates();
					for (int nDot = 0; nDot < xPolygon.length; nDot++) {
						DotN newDot = new DotN();
						newDot.pos.x = (float) xPolygon[nDot];
						newDot.pos.y = (float) yPolygon[nDot];
						newDot.pos.z = (float) (frame * zScale - 1);
						dotsNewRegion.add(newDot);
					}
			}
	
	public void setOverlapRegion(int frame, PolygonRoi[] poly) {
		dotsNewRegion = new ArrayList<DotN>();
		zScale = (float) 4.06;
					for (int nDot = 0; nDot < poly.length; nDot++) {
						DotN newDot = new DotN();
						newDot.pos.x = (float) poly[nDot].getXBase();
						newDot.pos.y = (float) poly[nDot].getYBase();
						newDot.pos.z = (float) (frame * zScale - 1);
						dotsNewRegion.add(newDot);
					}
			}
	
	
}
