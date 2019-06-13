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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.function.Predicate;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.util.ArrayList;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import PostProcessingGland.GUI.PostProcessingWindows;
private String initialDirectory;

public class PostProcessingGland implements PlugIn {

	// Window
	PostProcessingWindows PostProcessingWindow;
	
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
	public void run(String arg) {
		// Build GUI
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ImagePlus raw_img;
				// Create the main window
				PostProcessingWindow = new PostProcessingWindows(raw_img);
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
	 * Static method to enable polygon selection It is mainly used to create
	 * ROIs
	 */
	public static void callToolbarPolygon() {
		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.POLYGON);
	}

/*	public void initPostProcessingWindow() {
		try {
			ImagePlus raw_img = IJ.openImage();
			
			if (raw_img != null) {
        
        // this.initialDirectory = raw_img.getOriginalFileInfo().directory;
        // postProcessing = new PostProcessingWindows(raw_img);
        // postProcessing.pack();


			} else {
				// JOptionPane.showMessageDialog(panel.getParent(), "You must introduce a valid image or set of images.");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/
  
	
}