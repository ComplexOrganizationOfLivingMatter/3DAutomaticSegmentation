/**
 * PostProcessingGland
 */
package PostProcessingGland;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.macro.Functions;
import ij.plugin.PlugIn;
import ij.process.FloatPolygon;
import net.imglib2.RandomAccessibleInterval;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.ArrayList;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import 3DAutomaticSegmentation.PostProcessingGland;

public class PostProcessingGland implements PlugIn {

	// Need to create a Window

	/**
	 * Constructor by default
	 */
	public PostProcessingGland() {
		super();
	}

	/**
	 * Debug mode
	 * 
	 * @param args default arguments
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins
		// menu
		Class<?> clazz = PostProcessingGland.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(),
				url.length() - clazz.getName().length() - ".class".length() - "classes".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

	/*
	 * Plugin run method (non-Javadoc)
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */

	/**
	 * Static method to enable multipoint selection It is mainly used to create
	 * ROIs
	 */
	public static void callToolbarMultiPoint() {
		ij.gui.Toolbar.getInstance().setTool("multi");
	}

	/**
	 * Static method to enable polygon selection It is mainly used to create
	 * ROIs
	 */
	public static void callToolbarPolygon() {
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.POLYGON);
	}

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