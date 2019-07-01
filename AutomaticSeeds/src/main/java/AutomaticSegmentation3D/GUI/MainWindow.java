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
import java.awt.image.ColorModel;
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
import ij.gui.Overlay;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Thresholder;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.MinimaAndMaxima;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.plugins.MorphologicalSegmentation;
import inra.ijpb.plugins.MorphologicalSegmentation.ResultMode;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import inra.ijpb.watershed.Watershed;

import vib.segment.CustomCanvas;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

import ij.util.Tools;
import Utilities.Counter3D;

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
		// Associate this panel to the window
		getContentPane().add(panel);
		
		OpenButton = new JButton("Open");
		OpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				//Open the image
				ImagePlus imp= IJ.openImage();
				
				//Test
				System.out.println("Sin convertir: "+imp.getBitDepth());
				//Convert the image to 8-Bit
				ImageConverter converter = new ImageConverter(imp);
				converter.convertToGray8();
				//Test
				System.out.println("Convertida: "+imp.getBitDepth());
				
				//Create threshold and binarize the image
				
				//IJ.run(imp,"Make Binary","");
				
				ImageProcessor processor = imp.getStack().getProcessor(30);
				
				
				int thresh = processor.getAutoThreshold()+3;

				System.out.println("thresh: "+thresh);
				for(int i=1;i<=imp.getStackSize();i++) {
					processor = imp.getStack().getProcessor(i);

					//processor.setAutoThreshold("Triangle", true, 1);
					processor.threshold(thresh);
					
					imp.getStack().setProcessor(processor, i);
				}
				
				imp.show();
				
				
				//Test
				System.out.println("Esta binarizada: "+imp.getStack().getProcessor(5).isBinary());
				
				//Morphological segmentation
				//Settings
				int gradient_radius = 5;
				int tol = 5;
				int conn = 26;
				boolean dams = true;
				
				Strel3D gradient = Strel3D.Shape.CUBE.fromRadius(gradient_radius);
				
				//Segmentation
				ImageStack image = Morphology.gradient(imp.getImageStack(), gradient);
				
				
				ImageStack regionalMinima = MinimaAndMaxima3D.extendedMinima( image, tol, conn );
				
				ImageStack imposedMinima = MinimaAndMaxima3D.imposeMinima( image, regionalMinima, conn );
				
				ImageStack labeledMinima = BinaryImages.componentsLabeling( regionalMinima, conn, 32);
				
				
				
			    // apply marker-based watershed using the labeled minima on the minima-imposed gradient image (the true value indicates the use of dams in the output)
				ImageStack ip_segmented = Watershed.computeWatershed( imposedMinima, labeledMinima, conn, true );
				
			    ImagePlus imp_segmented = new ImagePlus("MorphSegmented",ip_segmented);
			    imp_segmented.setCalibration(imp.getCalibration());
			    //ImagePlus binarized = BinaryImages.binarize(imp_segmented);
			    
			    // Adjust min and max values to display
				Images3D.optimizeDisplayRange( imp_segmented );

				byte[][] colorMap = CommonLabelMaps.fromLabel( CommonLabelMaps.GOLDEN_ANGLE.getLabel() ).computeLut( 255, false );
				ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);
				imp_segmented.getProcessor().setColorModel(cm);
				imp_segmented.getImageStack().setColorModel(cm);
				imp_segmented.updateAndDraw();
				
				//Convert the segmented image to 8-Bit
				ImageConverter converter2 = new ImageConverter(imp_segmented);
				converter2.convertToGray8();
			    
				imp_segmented.show();
				
				/*ResultsTable rt = new ResultsTable(); 
				
				int centroids = Measurements.CENTROID;
				Analyzer analyzer = new Analyzer(imp_segmented, centroids, rt);
				*/
				Counter3D counter = new Counter3D(imp_segmented, 10, 80, 92274688, true, false);
				ImagePlus resultado = counter.getObjMap(true, 30);
				float[][] centroids = counter.getCentroidList();
				resultado.show();
				System.out.println(centroids.toString());
				
			}
		});
		OpenButton.setBounds(682, 412, 97, 25);
		panel.add(OpenButton);
		
		


	}
}