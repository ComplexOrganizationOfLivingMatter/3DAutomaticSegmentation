/**
 * PostProcessingGland
 */
package PostProcessingGland;

import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

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
	 * Static method to enable rectangle selection It is mainly used to create
	 * ROIs
	 */
	public static void callToolbarPolygon() {
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.POLYGON);
	}
	
	// ------------------- I/O
	/**
	 * Writes current data in XML/Ply files
	 * 
	 * @param path
	 */
	// @IJ1ScriptableMethod(target=IO, ui="PathWriter", tt="(String path)", pr=1)
	static public void saveStateToXmlPlyv1(String path) {
		IOXmlPlyLimeSeg.saveState(new LimeSeg(), "0.1", path);
	}

	@IJ1ScriptableMethod(target = IO, ui = "PathWriter", tt = "(String path)",
		pr = 1)
	static public void saveStateToXmlPly(String path) {
		IOXmlPlyLimeSeg.saveState(new LimeSeg(), "0.2", path);
	}

	/**
	 * Loads current data in XML/Ply files
	 * 
	 * @param path
	 */
	@IJ1ScriptableMethod(target = IO, ui = "PathOpener", tt = "(String path)",
		pr = 0)
	static public void loadStateFromXmlPly(String path) {
		if (jcr != null) {
			clear3DDisplay();
		}
		IOXmlPlyLimeSeg.loadState(new LimeSeg(), path);
		notifyCellRendererCellsModif = true;
		notifyCellExplorerCellsModif = true;
	}

	/**
	 * get currentChannel parameter (IJ1 macroextension style)
	 * 
	 * @param value
	 */
	@IJ1ScriptableMethod(target = OPT, tt = "(String paramName, Double value)")
	static public void getCurrentChannel(Double[] value) {
		value[0] = (double) currentChannel;
	}

	/**
	 * get currentFrame parameter (IJ1 macroextension style)
	 * 
	 * @param value
	 */
	@IJ1ScriptableMethod(target = OPT, tt = "(String paramName, Double value)")
	static public void getCurrentFrame(Double[] value) {
		value[0] = (double) currentFrame;
	}

	/**
	 * get number of cells present in LimeSeg (IJ1 macroextension style)
	 * 
	 * @param value
	 */
	@IJ1ScriptableMethod(target = OPT, tt = "(String paramName, Double value)")
	static public void getNCells(Double[] value) {
		assert allCells != null;
		value[0] = (double) allCells.size();
	}

	/**
	 * Gets 3D view center position (IJ1 macroextension style)
	 * 
	 * @param pX
	 * @param pY
	 * @param pZ
	 */
	@IJ1ScriptableMethod(target = VIEW_3D,
		tt = "(String paramName, Double value)")
	static public void get3DViewCenter(Double[] pX, Double[] pY, Double[] pZ) {
		make3DViewVisible();
		pX[0] = (double) (jcr.lookAt.x);
		pY[0] = (double) (jcr.lookAt.y);
		pZ[0] = (double) (jcr.lookAt.z / (float) opt.getOptParam("ZScale"));
	}

	/**
	 * Get current cell color (IJ1 macroextension style)
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	@IJ1ScriptableMethod(target = CURRENT_CELL,
		tt = "(String paramName, Double value)")
	static public void getCellColor(Double[] r, Double[] g, Double[] b,
		Double[] a)
	{
		if (currentCell != null) {
			r[0] = new Double(currentCell.color[0]);
			g[0] = new Double(currentCell.color[1]);
			b[0] = new Double(currentCell.color[2]);
			a[0] = new Double(currentCell.color[3]);
		}
		else {
			r[0] = new Double(0);
			g[0] = new Double(0);
			b[0] = new Double(0);
			a[0] = new Double(0);
		}
	}

	/**
	 * See {@link #set3DViewMode(int)}
	 * 
	 * @param value
	 */
	@IJ1ScriptableMethod(target = VIEW_3D,
		tt = "(String paramName, Double value)")
	static public void get3DViewMode(Double[] value) {
		value[0] = (double) jcr.getViewMode();
	}

	/**
	 * Center the 3D Viewer on the current selected cell
	 */
	@IJ1ScriptableMethod(target = VIEW_3D, ui = "STD", tt = "()")
	static public void enableTrackCurrentCell() {
		make3DViewVisible();
		jcr.trackCurrentCell = true;
	}

	/**
	 * Disable centering the 3D Viewer on the current selected cell
	 */
	@IJ1ScriptableMethod(target = VIEW_3D, ui = "STD", tt = "()")
	static public void disableTrackCurrentCell() {
		make3DViewVisible();
		jcr.trackCurrentCell = false;
	}

	/**
	 * See {@link #setCell3DDisplayMode(int)}
	 * 
	 * @param vMode
	 */
	@IJ1ScriptableMethod(target = CURRENT_CELL + TS + VIEW_3D, tt = "(int vMode)")
	static public void getCell3DDisplayMode(Double[] vMode) {
		if (currentCell != null) {
			vMode[0] = (double) currentCell.display_mode;
		}
	}

	/**
	 * Get current dot pos (IJ1 macro extension style)
	 * 
	 * @param px
	 * @param py
	 * @param pz
	 */
	@IJ1ScriptableMethod(target = CURRENT_CELL,
		tt = "(String paramName, Double value)")
	static public void getDotPos(Double[] px, Double[] py, Double[] pz) {
		if (currentDot != null) {
			px[0] = new Double(currentDot.pos.x);
			py[0] = new Double(currentDot.pos.y);
			pz[0] = new Double(currentDot.pos.z);/// (float)opt.getOptParam("ZScale"));
		}
		else {
			px[0] = new Double(0);
			py[0] = new Double(0);
			pz[0] = new Double(0);
		}
	}

	/**
	 * Get norm of current dot (IJ1 Macro extension style)
	 * 
	 * @param nx
	 * @param ny
	 * @param nz
	 */
	@IJ1ScriptableMethod(target = CURRENT_CELL,
		tt = "(String paramName, Double value)")
	static public void getDotNorm(Double[] nx, Double[] ny, Double[] nz) {
		if (currentDot != null) {
			nx[0] = new Double(currentDot.Norm.x);
			ny[0] = new Double(currentDot.Norm.y);
			nz[0] = new Double(currentDot.Norm.z);/// (float)opt.getOptParam("ZScale"));
		}
		else {
			nx[0] = new Double(0);
			ny[0] = new Double(0);
			nz[0] = new Double(0);
		}
	}

	/**
	 * Gets number of dots contained in current cell at current frame (IJ1 macro
	 * extension style)
	 * 
	 * @param value
	 */
	@IJ1ScriptableMethod(target = CURRENT_CELLT, tt = "(Double value)")
	static public void getNDots(Double[] value) {
		value[0] = (double) 0;
		if (currentCell != null) {
			if (currentCell.getCellTAt(currentFrame) != null) {
				if (currentCell.getCellTAt(currentFrame).dots != null) {
					value[0] = new Double(currentCell.getCellTAt(currentFrame).dots
						.size());
				}
			}
		}
	}

	/**
	 * Get current cell at current frame center
	 * 
	 * @param px
	 * @param py
	 * @param pz
	 */
	@IJ1ScriptableMethod(target = CURRENT_CELLT,
		tt = "(Double[] px, Double[] py, Double[] pz)")
	static public void getCellCenter(Double[] px, Double[] py, Double[] pz) {
		px[0] = new Double(0);
		py[0] = new Double(0);
		pz[0] = new Double(0);
		if (currentCell != null) {
			if (currentCell.getCellTAt(currentFrame) != null) {
				if (currentCell.getCellTAt(currentFrame).dots != null) {
					CellT ct = currentCell.getCellTAt(currentFrame);
					ct.updateCenter();
					px[0] = new Double(ct.center.x);
					py[0] = new Double(ct.center.y);
					pz[0] = new Double(ct.center.z / (float) opt.getOptParam("ZScale"));
				}
			}
		}
	}

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}
}