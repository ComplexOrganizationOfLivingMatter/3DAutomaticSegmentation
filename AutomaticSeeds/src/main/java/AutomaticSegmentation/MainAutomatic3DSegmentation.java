/**
 * AutomaticSegmentation
 */
package AutomaticSegmentation;

import javax.swing.SwingUtilities;

import AutomaticSegmentation.gui.MainWindow;
import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class MainAutomatic3DSegmentation implements PlugIn {

	// Window
	MainWindow mainWindow;

	/**
	 * Constructor by default
	 */
	public MainAutomatic3DSegmentation() {
		super();
	}

	/**
	 * Debug mode
	 * 
	 * @param args
	 *            default arguments
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins
		// menu
		Class<?> clazz = MainAutomatic3DSegmentation.class;
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
	 * 
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	public void run(String arg) {
		// Build GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Create the main window
				mainWindow = new MainWindow();
				mainWindow.pack();
				mainWindow.setVisible(true);
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
	 * Static method to enable rectangle selection It is mainly used to create
	 * ROIs
	 */
	public static void callToolbarRectangle() {
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.RECTANGLE);
	}
}