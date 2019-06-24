
package PostProcessingGland.GUI;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import PostProcessingGland.IOPlyLimeSeg;
// import JTableModel;
import PostProcessingGland.GUI.CustomElements.CustomCanvas;
import PostProcessingGland.GUI.CustomElements.ImageOverlay;
import eu.kiaru.limeseg.struct.DotN;
import fiji.util.gui.OverlayedImageCanvas.Overlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

public class PostProcessingWindow extends ImageWindow implements
	ActionListener
{

	private ArrayList<ImageOverlay> Output3dSegmentation;
	private ArrayList<ArrayList<String>> TotalpolDistriRoi;
	private ArrayList<Integer> idCells;
	private Hashtable<Integer, ArrayList<Roi>> cellsROIs;
	private CustomCanvas canvas;
	private ImageOverlay overlayResult;
	private ArrayList<DotN> dots = new ArrayList<>();

	private JFrame processingFrame = new JFrame("PostProcessing");
	private JPanel upRightPanel = new JPanel();
	private JPanel middlePanel = new JPanel();
	private JPanel bottomRightPanel = new JPanel();
	private JLabel idLabel = new JLabel("ID Cells");
	private JLabel lumenLabel = new JLabel("Lumen Processing");
	private JButton btnSave = new JButton("Save Cell");
	private JButton btnInsert = new JButton("Modify Cell");
	private JButton btnLumen = new JButton("Add Lumen");

	// private JPanel IdPanel;

	private String initialDirectory;

	// private JTableModel tableInf;
	// private Scrollbar sliceSelector;

	public PostProcessingWindow(ImagePlus raw_img) {
		super(raw_img, new CustomCanvas(raw_img));

		this.initialDirectory = raw_img.getOriginalFileInfo().directory;
		String path = this.initialDirectory.toString();
		IOPlyLimeSeg.SearchPath(dots, path);

		canvas = (CustomCanvas) super.getCanvas();

		// Init parameters
		idCells = new ArrayList<Integer>();
		Output3dSegmentation = new ArrayList<ImageOverlay>();
		cellsROIs = new Hashtable<Integer, ArrayList<Roi>>();
		// tableInf = tableInfo;
		overlayResult = new ImageOverlay();

		if (overlayResult != null) {
			if (canvas.getImageOverlay() == null) {
				canvas.clearOverlay();
				overlayResult.updateOverlay(dots, raw_img);
				overlayResult.setImage(raw_img);
				canvas.addOverlay(overlayResult);
				canvas.getImageOverlay();
			}
		}

		// removeAll();

		initGUI(raw_img);

		setEnablePanels(false);

		// threadFinished = false;

	}

	private void initGUI(ImagePlus raw_img) {

		/*upRightPanel.setLayout(new MigLayout("alignx right, wrap"));
		upRightPanel.add(idLabel, "wrap");
		
		middlePanel.setLayout(new MigLayout("alignx right, wrap, gapy 10::50"));
		middlePanel.add(btnSave, "wrap");
		middlePanel.add(btnInsert, "wrap");
		
		bottomRightPanel.setLayout(new MigLayout("alignx right, wrap, gapy 10::50"));
		bottomRightPanel.add(lumenLabel, "wrap");
		bottomRightPanel.add(btnLumen);
		
		*/
		processingFrame.add(canvas);
		// processingFrame.add(upRightPanel);
		// processingFrame.add(middlePanel);
		// processingFrame.add(bottomRightPanel);
		processingFrame.setMinimumSize(new Dimension(1024, 1024));
		processingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		processingFrame.pack();
		processingFrame.setVisible(true);

		initializeGUIItems(raw_img);

	}

	private void initializeGUIItems(ImagePlus raw_img) {
		canvas.addComponentListener(new ComponentAdapter() {

			public void componentResized(ComponentEvent ce) {
				Rectangle r = canvas.getBounds();
				canvas.setDstDimensions(r.width, r.height);
			}
		});
	}

	/**
	 * Enable/disable all the panels in the window
	 * 
	 * @param enabled true it will enable panels, false disable all panels
	 */
	protected void setEnablePanels(boolean enabled) {
		btnSave.setEnabled(true);
		btnInsert.setEnabled(true);
	}

	/**
	 * Disable all the action buttons
	 */
	protected void disableActionButtons() {
		btnSave.setEnabled(false);
		btnInsert.setEnabled(false);
	}

	/**
	 * Enable all the action buttons
	 */
	protected void enableActionButtons() {
		btnSave.setEnabled(true);
		btnInsert.setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

}
