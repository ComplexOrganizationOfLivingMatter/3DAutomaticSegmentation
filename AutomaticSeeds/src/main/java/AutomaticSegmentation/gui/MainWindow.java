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
	
	private ImagePlus originalImp;
	private ImagePlus nucleiChannel;
	private ImagePlus cellOutlineChannel;
	private JLabel lbNucleiFileName;
	private JButton btNucleiOpenFile;
	private JLabel lbCellOutlinesFileName;
	private JButton btCellOutlinesOpenFile;
	private JLabel lbOriginalImage;
	private JLabel lbOriginalFileName;
	private JButton btOpenOriginalImage;
	private JLabel lbEmptyLabel;

	/**
	 * @throws HeadlessException
	 */
	public MainWindow() throws HeadlessException {
		
		cellOutlineChannel = null;
		nucleiChannel = null;
		
		// GUI
		getContentPane().setLayout(new GridLayout(2, 0, 0, 0));
		
		imageChannelsPanel = new JPanel(new GridLayout(3, 4));
		buttonsPanel = new JPanel(new GridLayout(1, 3));
		getContentPane().add(imageChannelsPanel);
		getContentPane().add(buttonsPanel);

		//Row 1: Original image
		lbOriginalImage = new JLabel("Original image");
		lbOriginalFileName = new JLabel("");
		btOpenOriginalImage = new JButton("Open");
		lbEmptyLabel = new JLabel("");
		
		imageChannelsPanel.add(lbOriginalImage);
		imageChannelsPanel.add(lbOriginalFileName);
		imageChannelsPanel.add(lbEmptyLabel);
		imageChannelsPanel.add(btOpenOriginalImage);
		
		//Row 2: Nuclei channel
		cbNucleiChannel = new JComboBox<String>();
		
		lbNucleiChannel = new JLabel("Nuclei channel");
		lbNucleiChannel.setLabelFor(cbNucleiChannel);
		
		
		lbNucleiFileName = new JLabel("");
		btNucleiOpenFile = new JButton("Open");

		imageChannelsPanel.add(lbNucleiChannel);
		imageChannelsPanel.add(cbNucleiChannel);
		imageChannelsPanel.add(lbNucleiFileName);
		imageChannelsPanel.add(btNucleiOpenFile);
		
		//Row 3: Cell outline channel
		cbSegmentableChannel = new JComboBox<String>();
		lbSegmentableChannel = new JLabel("Cell outline channel");
		lbSegmentableChannel.setLabelFor(cbSegmentableChannel);
		
		lbCellOutlinesFileName = new JLabel("");
		btCellOutlinesOpenFile = new JButton("Open");
		
		imageChannelsPanel.add(lbSegmentableChannel);
		imageChannelsPanel.add(cbSegmentableChannel);
		imageChannelsPanel.add(lbCellOutlinesFileName);
		imageChannelsPanel.add(btCellOutlinesOpenFile);
		
		
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
				preLimeSeg = new PreLimeSegWindow(nucleiChannel.getChannelProcessor());
				preLimeSeg.pack();
				preLimeSeg.setVisible(true);
			}
		});
		
		btNucleiOpenFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					nucleiChannel = IJ.openImage();
					lbNucleiFileName.setText(nucleiChannel.getOriginalFileInfo().fileName);
					cbNucleiChannel.setSelectedIndex(0);
				} catch (Exception ex) {
					
				}
			}
		});
		
		btCellOutlinesOpenFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				try {
					cellOutlineChannel = IJ.openImage();
					lbCellOutlinesFileName.setText(cellOutlineChannel.getOriginalFileInfo().fileName);
					cbSegmentableChannel.setSelectedIndex(0);
				} catch (Exception ex) {
					
				}
			}
		});
		
		cbNucleiChannel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (((String) cbNucleiChannel.getSelectedItem()).equals("")){
					nucleiChannel = null;
				} else {
					originalImp.setC(cbNucleiChannel.getSelectedIndex());
					nucleiChannel = new ImagePlus("", originalImp.getChannelProcessor());
				}
			}
		});
		
		cbSegmentableChannel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (((String) cbSegmentableChannel.getSelectedItem()).equals("")){

					cellOutlineChannel = null;
				} else {
					originalImp.setC(cbSegmentableChannel.getSelectedIndex());

					cellOutlineChannel = new ImagePlus("", originalImp.getChannelProcessor());
				}
			}
		});
		
		btLimeSeg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				originalImp.setC(cbSegmentableChannel.getSelectedIndex());
				
				limeSegWindow = new LimeSegWindow(new ImagePlus("ToSegment", cellOutlineChannel.getChannelProcessor()));
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
				
				while (originalImp == null) {
					response = JOptionPane.showConfirmDialog(getParent(), "Do you want to use the open stack or another one?", "Confirm",
					        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					
					if (response == JOptionPane.NO_OPTION) {
						IJ.open();
						try {
							originalImp = IJ.getImage();
						} catch (Exception ex) {
							// TODO: handle exception
						}
					} else if (response == JOptionPane.YES_OPTION) {
						try {
							originalImp = IJ.getImage();
						} catch (Exception ex) {
							// TODO: handle exception
						}
					}
				}
				
				String fileName = originalImp.getOriginalFileInfo().fileName;
				
				cbNucleiChannel.addItem("");
				cbSegmentableChannel.addItem("");
				
				for (int numChannel = 0; numChannel < originalImp.getNChannels(); numChannel++) {
					cbNucleiChannel.addItem("C=" + numChannel);
					cbSegmentableChannel.addItem("C=" + numChannel);
				}
				
				originalImp.show();
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
