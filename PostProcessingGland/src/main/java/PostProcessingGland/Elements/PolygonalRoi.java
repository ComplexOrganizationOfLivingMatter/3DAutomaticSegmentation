
package PostProcessingGland.Elements;

import com.github.quickhull3d.Point3d;

import java.awt.Color;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.plugin.frame.RoiManager;
import eu.kiaru.limeseg.struct.DotN;

public class PolygonalRoi {
	
	public ArrayList<DotN> dotsNewRegion;
	public ArrayList<DotN> convexHullDots;
	
	public void selectZRegionToSmooth(int frame, Cell3D newCellRegion, Polygon poly) {
		dotsNewRegion = new ArrayList<DotN>();
		ArrayList<DotN> dots = newCellRegion.dotsList;
		Iterator<DotN> i = dots.iterator();
		while (i.hasNext()) {
			DotN loadedDots = i.next();
			int zpos=1+(int)((float) (loadedDots.pos.z/ (float) 4.06)); 
			if ((frame-1) <= zpos && zpos <= (frame+1)) {
				 if (zpos == frame) {
					 for (int nDot=0; nDot < poly.xpoints.length; nDot++) {
						 DotN newDot = new DotN();
						 newDot.pos.x = (float) poly.xpoints[nDot];
						 newDot.pos.y = (float) poly.ypoints[nDot];
						 newDot.pos.z = (float) (frame * 4.06 - 1);
						 dotsNewRegion.add(newDot);
					 }

				 } else {
					 dotsNewRegion.add(loadedDots);
			}
			}
	}
}
	
	public void convertPointsInDots(Point3d[] points) {
		ArrayList<Point3d> convexPoints = new ArrayList<Point3d>(Arrays.asList(points));
		convexHullDots = new ArrayList<DotN>();
		for (int nPoint = 0; nPoint < convexPoints.size(); nPoint++) {
			DotN dot = new DotN();
			dot.pos.x = (float) convexPoints.get(nPoint).x;
			dot.pos.y =	(float) convexPoints.get(nPoint).y;
			dot.pos.z =	(float) convexPoints.get(nPoint).z;
			convexHullDots.add(dot);
		}
	}
	
	public ArrayList<DotN> integrateNewData (ArrayList <DotN> newDots, ArrayList<DotN> oldDots, int frame) {
		ArrayList<DotN> currentDots = new ArrayList<DotN>();
		int pepe = oldDots.size();
		for (int i= 0; i < oldDots.size(); i++) {
			DotN dot = oldDots.get(i);
			int zpos=1+(int)((float) (dot.pos.z/ (float) 4.06)); 
			if ((frame-1) > zpos | zpos > (frame+1)) {
				currentDots.add(dot);
			}
		}
		for (int j= 0; j < newDots.size(); j++) {
			currentDots.add(newDots.get(j)); 
		}
			return currentDots;
		
	}
	
	public ArrayList<DotN> getNewRegion(){
		return this.dotsNewRegion;
	}
	
	public double getCoordinate(int iteration,String type) {
		if (type == "x") {
			return this.dotsNewRegion.get(iteration).pos.x;
		}
		else if (type == "y") {
			return this.dotsNewRegion.get(iteration).pos.y;
		}
		else {
			return this.dotsNewRegion.get(iteration).pos.z;
		}
	}
		
}

