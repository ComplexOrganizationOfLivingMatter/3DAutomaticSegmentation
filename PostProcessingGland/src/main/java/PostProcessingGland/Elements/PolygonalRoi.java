
package PostProcessingGland.Elements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import eu.kiaru.limeseg.struct.DotN;

public class PolygonalRoi {
	
	PointRoi dotsRoi;
	PolygonRoi polyRoi;

	public PointRoi convertPolygonInPointRois(PolygonRoi polyRoi) {
		PointRoi dotsRoi = new PointRoi();
		return dotsRoi;
	}
	
	public void convertDotsInPointRoi(ArrayList<DotN> dots, int frame) {
		Iterator<DotN> i=dots.iterator();
         while (i.hasNext()) {
         		DotN loadedDots = i.next();
            int zpos=1+(int)(loadedDots.pos.z/ (float) 4.06); // zScale == 4.06
            if (zpos == frame) {
         		dotsRoi = new PointRoi(loadedDots.pos.x,loadedDots.pos.y);//,c); 
             Random rand = new Random();
             float r = rand.nextFloat();
             float g = rand.nextFloat();
             float b = rand.nextFloat();
             Color randomColor = new Color(r, g, b);
             //Color color = new Color((int)(loadedDots.ct.c.color[0]*255),(int)(loadedDots.ct.c.color[1]*255),(int)(loadedDots.ct.c.color[2]*255));
             dotsRoi.setColor(randomColor);
             //float zScale = workingImP  /workingImP.getHeight()
             dotsRoi.setPosition(zpos);
             }
         }
	}

}

