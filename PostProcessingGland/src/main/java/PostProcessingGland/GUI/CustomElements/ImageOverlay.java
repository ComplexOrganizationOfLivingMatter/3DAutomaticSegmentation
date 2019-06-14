package PostProcessingGland.GUI.CustomElements;




import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Iterator;

import PostProcessingGland.Cell;
import PostProcessingGland.CellT;
import PostProcessingGland.DotN;
import PostProcessingGland.IJ1ScriptableMethod;
import fiji.util.gui.OverlayedImageCanvas.Overlay;
import ij.gui.PointRoi;
import ij.process.ImageProcessor;

/**
 * This class implements an overlay based on an image. The overlay paints the
 * image with a specific composite mode.
 * 
 * @author Ignacio Arganda-Carreras
 *
 */
public class ImageOverlay implements Overlay {

//----------------- 2D View
  /**
   * Put current cell to overlays (requires updateOverlay to be effective)
   */
  @IJ1ScriptableMethod(target=VIEW_2D, ui="STD", pr=0)
  static public void putCurrentCellToOverlay() { 
  	if (currentCell!=null) {
          addToOverlay(currentCell);
  	}    			
  }
  
  /**
   * Put dots of current user selected slice to overlays (requires updateOverlay to be effective)
   */
  @IJ1ScriptableMethod(target=VIEW_2D, ui="STD", pr=0)
  static public void putCurrentSliceToOverlay() { 
  	if (workingImP!=null) {
		float ZS=(float) opt.getOptParam("ZScale");
		int zSlice; //= workingImP.getZ();

        if ((workingImP.getNFrames()!=1)||(workingImP.getNChannels()!=1)) {
        	zSlice = workingImP.getZ();
        } else {
        	zSlice = workingImP.getSlice();
        }
    	for (Cell c:allCells) {
    		CellT ct = c.getCellTAt(LimeSeg.currentFrame);
    		if (ct!=null) {
    			for (DotN dn:ct.dots) {
    				if ((int)(dn.pos.z/ZS)==zSlice-1) {
    					LimeSeg.dots_to_overlay.add(dn);
    				}
    			}
    		}
    	}
  	}
  }
  
  @IJ1ScriptableMethod(target=VIEW_2D, ui="STD", pr=0)
  static public void putCurrentTimePointToOverlay() { 
  	if (workingImP!=null) {
    	for (Cell c:allCells) {
    		CellT ct = c.getCellTAt(LimeSeg.currentFrame);
    		if (ct!=null) {
    			for (DotN dn:ct.dots) {
    					LimeSeg.dots_to_overlay.add(dn);
    			}
    		}
    	}
  	}
  }
  
  /**
   *  Clears image overlay (requires updateOverlay to be effective)
   */
  @IJ1ScriptableMethod(target=VIEW_2D, ui="STD", pr=0)
  static public void clearOverlay() {
      if (dots_to_overlay!=null)
          dots_to_overlay.clear();
  }
  
  /**
   * Adds all cells into image overlay (requires updateOverlay to be effective
   */
  @IJ1ScriptableMethod(target=VIEW_2D, ui="STD", pr=1)
  static public void addAllCellsToOverlay() {
      for (int i=0;i<allCells.size();i++) {
          Cell c= allCells.get(i);
          addToOverlay(c);
      }
  } 
  
  /**
   * updates Overlay of the working image with registeres dots to be overlayed
   */
  @IJ1ScriptableMethod(target=VIEW_2D, ui="STD", pr=2)
  static public void updateOverlay() {
      Overlay ov = new Overlay();
      if (workingImP!=null) {
        workingImP.setOverlay(ov);
        Iterator<DotN> i=dots_to_overlay.iterator();
        float ZS=(float) opt.getOptParam("ZScale");
        if ((workingImP.getNFrames()!=1)||(workingImP.getNChannels()!=1)) {
            while (i.hasNext()) {
                DotN nd = i.next();
                PointRoi roi;
                roi = new PointRoi(nd.pos.x,nd.pos.y);//,c);
                Color color = new Color((int)(nd.ct.c.color[0]*255),(int)(nd.ct.c.color[1]*255),(int)(nd.ct.c.color[2]*255));
                roi.setStrokeColor(color);
                int zpos=1+(int)(nd.pos.z/ZS);
                if ((zpos>0)&&(zpos<=workingImP.getNSlices())) {
                    roi.setPosition(nd.ct.c.cellChannel, zpos, nd.ct.frame);
                    ov.addElement(roi); 
                }   
            }   
        } else {
            while (i.hasNext()) {
                DotN nd = i.next();
                PointRoi roi;
                roi = new PointRoi(nd.pos.x,nd.pos.y);//,c);   
                Color color = new Color((int)(nd.ct.c.color[0]*255),(int)(nd.ct.c.color[1]*255),(int)(nd.ct.c.color[2]*255));
                roi.setStrokeColor(color);
                int zpos=1+(int)((float) (nd.pos.z)/(float) (ZS));
                if ((zpos>0)&&(zpos<=workingImP.getNSlices())) {
                    roi.setPosition(zpos);
                    ov.addElement(roi);  
                }
            }
        }
        workingImP.updateAndDraw();
      }
  } 
	
	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}
}