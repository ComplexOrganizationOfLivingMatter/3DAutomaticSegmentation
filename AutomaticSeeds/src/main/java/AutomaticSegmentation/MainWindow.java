package AutomaticSegmentation;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;

import Utilities.Counter3D;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
//import ij.IJ;
//import ij.ImagePlus;


public class MainWindow extends JFrame{
	
	private JPanel panel;
	private JButton OpenButton;
	private RoiManager roiManager;
	
	
	public MainWindow() {
		String name = UIManager.getInstalledLookAndFeels()[3].getClassName();
		try {
			UIManager.setLookAndFeel(name);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		UIManager.put("Panel.background", Color.WHITE);
		UIManager.put("Slider.background", Color.WHITE);
		setMinimumSize(new Dimension(1200, 600));
		setTitle("AutomaticSegmentation3D");
		// Not close Fiji when AutomaticSegmentation is closed
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		this.addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub

			}
		});

		// Main panel
		panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0)));
		panel.setLayout(null);
		
		// Create 'open' button and add to the panel
		OpenButton = new JButton("Open");
		OpenButton.setBounds(682, 412, 97, 25);
		panel.add(OpenButton);
		
		// Associate this panel to the window
		getContentPane().add(panel);
		
		OpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Open the image
				ImagePlus imp= IJ.openImage();
				
				
				imp.show();
				imp.
				NucleiSegmentation objectSegmentation = new NucleiSegmentation(imp);
				ImagePlus imp_segmented = objectSegmentation.getSegmentedNuclei();
				
				//3D-OC options settings
				Prefs.set("3D-OC-Options_volume.boolean", false);
		        Prefs.set("3D-OC-Options_objVox.boolean", false);
		        Prefs.set("3D-OC-Options_surfVox.boolean", false);
		        Prefs.set("3D-OC-Options_IntDens.boolean", false);
		        Prefs.set("3D-OC-Options_mean.boolean", false); 
		        Prefs.set("3D-OC-Options_stdDev.boolean", false);
		        Prefs.set("3D-OC-Options_median.boolean", false);
		        Prefs.set("3D-OC-Options_min.boolean", false);
		        Prefs.set("3D-OC-Options_max.boolean", false);
		        Prefs.set("3D-OC-Options_meanDist2Surf.boolean", false);
		        Prefs.set("3D-OC-Options_SDDist2Surf.boolean", false);
		        Prefs.set("3D-OC-Options_medDist2Surf.boolean", false);
		        Prefs.set("3D-OC-Options_COM.boolean", false);
		        Prefs.set("3D-OC-Options_BB.boolean", false);
		        	            
				Prefs.set("3D-OC-Options_surface.boolean", true);
		        Prefs.set("3D-OC-Options_centroid.boolean", true);
				
					
				Counter3D counter = new Counter3D(imp_segmented, 10, 650, 92274688, false, false);
				ImagePlus result = counter.getObjMap(true, 30);
				
				float[][] centroidList = counter.getCentroidList();
				//              0  0 1 2
				//              |  | | |
				//centroid -> [id][x,y,z]
				
				//Creating the ROI manager
				RoiManager rm = new RoiManager();
				//Reset to 0 the RoiManager
				rm.reset();
				//Adding ROI to ROI Manager
				for(int i = 0; i<centroidList.length;i++) {
					//Get the coordinate x
					double x = centroidList[i][0];
					//Get the cordinate y
					double y = centroidList[i][1];
					//Get the slice to create the ROI
					int z = (int)centroidList[i][2];
					//Get the area and radius of the index i nuclei
					int a = counter.getObject(i).surf_size;
					int r = Math.round((float)Math.sqrt(a/Math.PI));
					
					imp_segmented.setSlice(z);
					Roi roi = new OvalRoi(x-r/2,y-r/2,r,r);
					rm.addRoi(roi);
					
				}
				result.show();
				counter.showStatistics(true);
				counter.showSummary();
				
			
			}
		});
		
		
		


	}
}