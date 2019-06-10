package AutomaticSegmentation3D.GUI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.PlotWindow;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.morphology.MinimaAndMaxima;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.watershed.Watershed;
import vib.segment.CustomCanvas;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

//import ij.IJ;
//import ij.ImagePlus;


public class MainWindow extends JFrame{
	
	private JPanel panel;
	private JButton OpenButton;
	
	
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
		// Associate this panel to the window
		getContentPane().add(panel);
		
		OpenButton = new JButton("Open");
		OpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ImagePlus imp= IJ.openImage();
				//raw_img.show();
				//ImageStack image = imp.getImageStack();
				// find regional minima on gradient image with dynamic value of 'tolerance' and 'conn'-connectivity
				//ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, 3, 26 );
				// impose minima on gradient image
				//ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima( image, regionalMinima, 26 );
				// label minima using connected components (32-bit output)
				//ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, 6, 32 );
				// apply marker-based watershed using the labeled minima on the minima-imposed 
				// gradient image (the last value indicates the use of dams in the output)
				//ImageStack resultStack = Watershed.computeWatershed( imposedMinima, labeledMinima, 6, true );
				imp.show();
				// create image with watershed result
				//ImagePlus resultImage = new ImagePlus( "ImposedMinima", imposedMinima );
				//resultImage.show();
				RoiManager rm = RoiManager.getInstance();
				if (rm == null) {
					rm = new RoiManager();
				}
				IJ.run("ROI Manager...", "");
				IJ.run("To ROI Manager", "");
				rm.select(0);
				IJ.setThreshold(imp, 105, 362);
				
				//Create an overlay
				//IJ.run(imp, "Analyze Particles...", "size=50-Infinity display add");
				
				
				
				//ImageProcessor mascara= raw_img.getMask();
				//ImagePlus imageMasc= new ImagePlus("mascara",mascara);
				//imageMasc.show();
			
			}
		});
		OpenButton.setBounds(682, 412, 97, 25);
		panel.add(OpenButton);
		
		


	}
}