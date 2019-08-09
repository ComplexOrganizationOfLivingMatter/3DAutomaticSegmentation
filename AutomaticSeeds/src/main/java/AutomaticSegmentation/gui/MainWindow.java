/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.commands.CoarsenRefineSegmentation;
import eu.kiaru.limeseg.commands.SphereSeg;
import eu.kiaru.limeseg.commands.SphereSegAdvanced;
import eu.kiaru.limeseg.commands.TestCurvature;
import eu.kiaru.limeseg.gui.JPanelLimeSeg;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.patcher.LegacyInjector;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;

import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
	 * 
	 */
	private JPanel imageChannelsPanel;
	private JPanel buttonsPanel;
	private JComboBox cbNucleiChannel;
	private JComboBox cbSegmentableChannel;
	private JLabel lbNucleiChannel;
	private JLabel lbSegmentableChannel;
	
	private ImagePlus workingImp;

	/**
	 * @throws HeadlessException
	 */
	public MainWindow() throws HeadlessException {
		getContentPane().setLayout(new GridLayout(2, 0, 0, 0));
		
		imageChannelsPanel = new JPanel(new GridLayout(2, 2));
		buttonsPanel = new JPanel(new GridLayout(1, 3));
		getContentPane().add(imageChannelsPanel);
		getContentPane().add(buttonsPanel);

		cbNucleiChannel = new JComboBox<>();
		
		lbNucleiChannel = new JLabel("Nuclei Channel");
		lbNucleiChannel.setLabelFor(cbNucleiChannel);
		

		imageChannelsPanel.add(lbNucleiChannel);
		imageChannelsPanel.add(cbNucleiChannel);
		
		cbSegmentableChannel = new JComboBox<>();
		lbSegmentableChannel = new JLabel("Channel to segment");
		lbSegmentableChannel.setLabelFor(cbSegmentableChannel);
		
		imageChannelsPanel.add(lbSegmentableChannel);
		imageChannelsPanel.add(cbSegmentableChannel);
		
		
		
		//Buttons panel
		JButton btPreLimeSeg = new JButton("Preprocess to LimeSeg");
		buttonsPanel.add(btPreLimeSeg);
		
		JButton btLimeSeg = new JButton("LimeSeg");
		buttonsPanel.add(btLimeSeg);
		
		JButton btPostLimeSeg = new JButton("Postprocess LimeSeg's output");
		buttonsPanel.add(btPostLimeSeg);
		
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
				ImagePlus image = IJ.openImage();
				image.show();
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
				
				int response;
				
				while (workingImp == null) {
					response = JOptionPane.showConfirmDialog(getParent(), "Do you want to use the open stack or another one?", "Confirm",
					        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					
					if (response == JOptionPane.NO_OPTION) {
						workingImp = IJ.openImage();
					} else if (response == JOptionPane.YES_OPTION) {
						try {
							workingImp = IJ.getImage();
						} catch (Exception ex) {
							// TODO: handle exception
						}
					}
				}
				
				System.out.println(workingImp.getNChannels());
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
