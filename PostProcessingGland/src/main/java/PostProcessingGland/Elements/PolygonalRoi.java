
package PostProcessingGland.Elements;

import java.awt.Color;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.plugin.frame.RoiManager;
import eu.kiaru.limeseg.struct.DotN;

public class PolygonalRoi {
	
	ArrayList<PointRoi> dotsNewRegion;
	
	public void selectZRegionToSmooth(int frame, Cell3D newCellRegion, PointRoi newDots) {
		dotsNewRegion = new ArrayList<PointRoi>();
		ArrayList<DotN> dots = newCellRegion.dotsList;
		Iterator<DotN> i = dots.iterator();
		while (i.hasNext()) {
			DotN loadedDots = i.next();
			PointRoi roi = new PointRoi(loadedDots.pos.x, loadedDots.pos.y);
			 int zpos=1+(int)((float) (loadedDots.pos.z/ (float) 4.06)); 
			if ((frame-1) < zpos && zpos < (frame+1)) {
				 if (zpos == frame) {
					 dotsNewRegion.add(newDots);
				 } else {
					 dotsNewRegion.add(roi);
			}
			}
	}
}


}

