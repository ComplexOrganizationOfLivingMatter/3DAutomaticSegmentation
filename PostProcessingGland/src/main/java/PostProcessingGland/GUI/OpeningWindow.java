
package PostProcessingGland.GUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import net.miginfocom.swing.MigLayout;

import PostProcessingGland.GUI.PostProcessingWindow;
import ij.IJ;
import ij.ImagePlus;

public class OpeningWindow extends JFrame {

	private JPanel centralPanel = new JPanel();
	private JButton btnOpenButton = new JButton("Open");
	private String initialDirectory;
	private PostProcessingWindow postprocessingWindow;
	private JFrame fatherWindow = new JFrame();
	private JFileChooser fileChooser;

	public OpeningWindow() {
		String name = UIManager.getInstalledLookAndFeels()[3].getClassName();
		try {
			UIManager.setLookAndFeel(name);
		}
		catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		UIManager.put("Panel.background", Color.WHITE);
		UIManager.put("Slider.background", Color.WHITE);
		fatherWindow = this;
		setTitle("PostProcessing Gland");
		setMinimumSize(new Dimension(300, 50));
		
		// Not close Fiji when PostProcessingGland is closed
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		this.addWindowListener(new WindowListener() {

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub
				if (postprocessingWindow != null) if (!postprocessingWindow.isClosed())
					postprocessingWindow.close();
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
		centralPanel.setLayout(new MigLayout("", "[59px]", "[23px]"));
		centralPanel.add(btnOpenButton, "cell 0 0,alignx left,aligny top");
		getContentPane().add(centralPanel);
		pack();
		initGUIItems();
		fileChooser = new JFileChooser();
		
	}

	/**
	 * Initialize the gui items and set up properly within the window
	 */
	private void initGUIItems() {
		btnOpenButton = new JButton("Open");
		btnOpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						initPostProcessingWindow();
					}
				});
			}
			});
	}

	private void initPostProcessingWindow() {
						// TODO Auto-generated method stub
						try {
							ImagePlus raw_img = IJ.openImage();
							
							/**
							 * Create the image processing window. However, restrictions are applied with
							 * the selected image. It cannot exceed 3000 neither in width nor height.
							 */

							
							if (raw_img != null) {
				        if (raw_img.getHeight() >= 3000 || raw_img.getWidth() >= 3000) {
				          JOptionPane.showMessageDialog(centralPanel.getParent(),
				              "Warning! Large image detected. It may take time to process it.");
				        }
				        
				        this.initialDirectory = raw_img.getOriginalFileInfo().directory;
				        postprocessingWindow = new PostProcessingWindow(raw_img);
				        postprocessingWindow.pack();

				        postprocessingWindow.addWindowListener(new WindowListener() {

				          @Override
				          public void windowOpened(WindowEvent e) {
				            // TODO Auto-generated method stub
				            btnOpenButton.setEnabled(false);
				          }

				          @Override
				          public void windowClosing(WindowEvent e) {
				            // TODO Auto-generated method stub
				            btnOpenButton.setEnabled(true);
				          }

				          @Override
				          public void windowClosed(WindowEvent e) {
				            // TODO Auto-generated method stub

				          }

				          @Override
				          public void windowIconified(WindowEvent e) {
				            // TODO Auto-generated method stub

				          }

				          @Override
				          public void windowDeiconified(WindowEvent e) {
				            // TODO Auto-generated method stub

				          }

				          @Override
				          public void windowActivated(WindowEvent e) {
				            // TODO Auto-generated method stub

				          }

				          @Override
				          public void windowDeactivated(WindowEvent e) {
				            // TODO Auto-generated method stub

				          }

				        });

							} else {
								JOptionPane.showMessageDialog(centralPanel.getParent(), "You must introduce a valid image or set of images.");
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
}
