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
	public static void callToolbarRectangle() {
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.RECTANGLE);
	}

	@Override
	public void run(String arg) {
		// TODO Auto-generated method stub
		
	}
}