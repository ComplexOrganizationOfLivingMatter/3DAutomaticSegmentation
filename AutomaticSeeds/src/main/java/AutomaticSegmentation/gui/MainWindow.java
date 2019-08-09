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
import eu.kiaru.limeseg.commands.SphereSegAdvanced;
import ij.IJ;
import ij.ImagePlus;
import io.scif.img.ImgOpener;
import net.imagej.patcher.LegacyInjector;

import java.awt.GridLayout;

import javax.swing.ComboBoxModel;
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
	private LimeSegWindow limeSegWindow;
	
	/**
	 * 
	 */
	private JPanel imageChannelsPanel;
	private JPanel buttonsPanel;
	private JComboBox<String> cbNucleiChannel;
	private JComboBox<String> cbSegmentableChannel;
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

		cbNucleiChannel = new JComboBox<String>();
		
		lbNucleiChannel = new JLabel("Nuclei Channel");
		lbNucleiChannel.setLabelFor(cbNucleiChannel);
		

		imageChannelsPanel.add(lbNucleiChannel);
		imageChannelsPanel.add(cbNucleiChannel);
		
		cbSegmentableChannel = new JComboBox<String>();
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
				workingImp.setC(cbSegmentableChannel.getSelectedIndex());
				
				limeSegWindow = new LimeSegWindow(new ImagePlus("ToSegment", workingImp.getChannelProcessor()));
				limeSegWindow.pack();
				limeSegWindow.setVisible(true);
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
						IJ.open();
						try {
							workingImp = IJ.getImage();
						} catch (Exception ex) {
							// TODO: handle exception
						}
					} else if (response == JOptionPane.YES_OPTION) {
						try {
							workingImp = IJ.getImage();
						} catch (Exception ex) {
							// TODO: handle exception
						}
					}
				}
				
				String fileName = workingImp.getOriginalFileInfo().fileName;
				
				for (int numChannel = 0; numChannel < workingImp.getNChannels(); numChannel++) {
					cbNucleiChannel.addItem(fileName + " - C=" + numChannel);
					cbSegmentableChannel.addItem(fileName + " - C=" + numChannel);
				}
				
				workingImp.show();
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
