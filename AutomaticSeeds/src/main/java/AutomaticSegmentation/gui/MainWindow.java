/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;

import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.commands.CoarsenRefineSegmentation;
import eu.kiaru.limeseg.commands.SphereSeg;
import eu.kiaru.limeseg.commands.SphereSegAdvanced;
import eu.kiaru.limeseg.commands.TestCurvature;
import eu.kiaru.limeseg.gui.JPanelLimeSeg;
import ij.IJ;
import ij.ImagePlus;
import java.awt.GridLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author Pablo Vicente-Munuera, Pedro Gómez-Gálvez
 *
 */
public class MainWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private PreLimeSegWindow preLimeSeg;
	
	/**
	 * 
	 */
	private SphereSegAdvanced limeSeg;

	/**
	 * @throws HeadlessException
	 */
	public MainWindow() throws HeadlessException {
		getContentPane().setLayout(new GridLayout(1, 0, 0, 0));
		
		JButton btPreLimeSeg = new JButton("Preprocess to LimeSeg");
		getContentPane().add(btPreLimeSeg);
		
		JButton btLimeSeg = new JButton("LimeSeg");
		getContentPane().add(btLimeSeg);
		
		JButton btPostLimeSeg = new JButton("Postprocess LimeSeg's output");
		getContentPane().add(btPostLimeSeg);
		
		//Functions
		btPreLimeSeg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				preLimeSeg = new PreLimeSegWindow();
				preLimeSeg.pack();
				preLimeSeg.setVisible(true);
			}
		});
		
		btLimeSeg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
//				ImagePlus image = IJ.openImage();
//				image.show();
//				
//				LimeSeg ls = new LimeSeg();
//				ls.initialize();
//				ls.opt.setWorkingImage(IJ.openImage(), 1, 1);
//				ls.run();
////				
////				JPanelLimeSeg jp = new J(ls);
////				jp.setVisible(true);
//				
//				JPanelLimeSeg jp = new JPanelLimeSeg(ls);
//				jp.setVisible(true);
//				jp.repaint();
				
				SphereSegAdvanced cf = new SphereSegAdvanced();
				cf.run();
				//IJ.runPlugIn("eu.kiaru.limeseg.commands.SphereSeg", ""); Does not show anything
			  
			}
		});
	}

	/**
	 * @param gc
	 */
	public MainWindow(GraphicsConfiguration gc) {
		super(gc);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @throws HeadlessException
	 */
	public MainWindow(String title) throws HeadlessException {
		super(title);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @param gc
	 */
	public MainWindow(String title, GraphicsConfiguration gc) {
		super(title, gc);
		// TODO Auto-generated constructor stub
	}

}
