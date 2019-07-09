
package PostProcessingGland.Elements;

import com.github.quickhull3d.Point3d;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
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
import eu.kiaru.limeseg.struct.DotN;

public class RoiAdjustment {

	public ArrayList<DotN> dotsNewRegion;
	public ArrayList<DotN> convexHullDots;

	public void selectNewZRegion(int frame, Cell3D newCellRegion, Polygon poly) {
		dotsNewRegion = new ArrayList<DotN>();
		ArrayList<DotN> dots = newCellRegion.dotsList;
		Iterator<DotN> i = dots.iterator();
		while (i.hasNext()) {
			DotN loadedDots = i.next();
			int zpos = 1 + (int) ((float) (loadedDots.pos.z / (float) 4.06));
			if ((frame - 1) <= zpos && zpos <= (frame + 1)) {
				if (zpos == frame) {
					for (int nDot = 0; nDot < poly.xpoints.length; nDot++) {
						DotN newDot = new DotN();
						newDot.pos.x = (float) poly.xpoints[nDot];
						newDot.pos.y = (float) poly.ypoints[nDot];
						newDot.pos.z = (float) (frame * 4.06 - 1);
						dotsNewRegion.add(newDot);
					}

				}
				else {
					dotsNewRegion.add(loadedDots);
				}
			}
		}
	}

	public void convertPointsInDots(ArrayList<Point3d> points) {
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

	public ArrayList<DotN> getNewRegion() {
		return this.dotsNewRegion;
	}

	public double getCoordinate(int iteration, String type) {
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

	public void removeOverlappingRegions(ArrayList<Cell3D> allCells,
		PolygonRoi newPolygon, int frame)
	{
		for (int nCell = 0; nCell < allCells.size(); nCell++) {
			float[] xCell = allCells.get(nCell).getCoordinate("x", allCells.get(nCell)
				.getCell3DAt(frame));
			float[] yCell = allCells.get(nCell).getCoordinate("y", allCells.get(nCell)
				.getCell3DAt(frame));

			PolygonRoi overlappingCell = new PolygonRoi(xCell, yCell, 2);
			ShapeRoi s1 = new ShapeRoi(newPolygon);
			ShapeRoi s2 = new ShapeRoi(overlappingCell);

			if (s1.and(s2) != null) {
				s2.xor(s1);
				Roi[] overRois = s2.getRois();
				ArrayList<Roi> roiArrayList = new ArrayList<Roi>(Arrays.asList(
					overRois));
				Cell3D newCell = allCells.get(nCell);
				newCell.setCell3D(roiArrayList, frame);
				ArrayList<DotN> integratedDots = this.integrateNewRegion(
					newCell.dotsList, allCells.get(nCell).dotsList, frame);
				newCell = new Cell3D(allCells.get(nCell).id_Cell, integratedDots);
				allCells.set(nCell, newCell);
			}
		}

	}

}
