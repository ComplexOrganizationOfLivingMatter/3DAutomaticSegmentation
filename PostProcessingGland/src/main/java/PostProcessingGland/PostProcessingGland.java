/**
 * PostProcessingGland
 */

package PostProcessingGland;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;
import javax.swing.SwingUtilities;
import PostProcessingGland.GUI.OpeningWindow;

public class PostProcessingGland implements PlugIn {

	// Window
	OpeningWindow openingWindow;

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
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') +
			".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz
			.getName().length() - ".class".length() - "classes".length());
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
	public void run(String arg) {
		// Build GUI
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				// Create the main window
				openingWindow = new OpeningWindow();
				openingWindow.pack();
				openingWindow.setVisible(true);
			}
		});
	}

	/**
	 * Static method to enable multipoint selection It is mainly used to create
	 * ROIs
	 */
	public static void callToolbarMultiPoint() {
		ij.gui.Toolbar.getInstance().setTool("multi");
	}

	/**
	 * Static method to enable polygon selection It is mainly used to create ROIs
	 */
	public static void callToolbarPolygon() {
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.POLYGON);
	}

}
