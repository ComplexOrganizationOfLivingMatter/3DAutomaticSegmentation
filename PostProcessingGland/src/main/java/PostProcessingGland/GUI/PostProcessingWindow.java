
package PostProcessingGland.GUI;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import PostProcessingGland.Elements.Cell3D;
// import JTableModel;
import PostProcessingGland.GUI.CustomElements.CustomCanvas;
import PostProcessingGland.GUI.CustomElements.SegmentationOverlay;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.io.IOXmlPlyLimeSeg;
import eu.kiaru.limeseg.struct.Cell;
import eu.kiaru.limeseg.struct.DotN;
import fiji.util.gui.OverlayedImageCanvas.Overlay;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

public class PostProcessingWindow extends ImageWindow implements
	ActionListener
{

	
	private IOXmlPlyLimeSeg OutputLimeSeg;
	private ArrayList<Integer> idCells;
	private Hashtable<Integer, ArrayList<Roi>> cellsROIs;
	private CustomCanvas canvas;
	private SegmentationOverlay overlayResult;
	private Cell LimeSegCell;
	private LimeSeg limeSeg;
	public ArrayList<Cell> allCells;

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
	public Cell3D PostProcessCell;
	public ArrayList<Cell3D> all3dCells;

	// private JTableModel tableInf;
	// private Scrollbar sliceSelector;

	public PostProcessingWindow(ImagePlus raw_img) {
		super(raw_img, new CustomCanvas(raw_img));

		this.initialDirectory = raw_img.getOriginalFileInfo().directory;
		limeSeg = new LimeSeg();
		LimeSeg.allCells = new ArrayList<Cell>();
		LimeSegCell = new Cell();
		all3dCells = new ArrayList<Cell3D>();
		String directory = this.initialDirectory.toString();
		File dir = new File(directory + "/OutputLimeSeg");
		File[] files = dir.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.startsWith("cell");
			}
		});

		for (File f : files) {
			String path = f.toString();
			LimeSegCell.id_Cell = path.substring(path.indexOf("_") + 1);
			OutputLimeSeg.hydrateCellT(LimeSegCell, path);
			PostProcessCell = new Cell3D(LimeSegCell.id_Cell,LimeSegCell.cellTs.get(0).dots);
			all3dCells.add(PostProcessCell);
		}

		canvas = (CustomCanvas) super.getCanvas();

		// Init parameters
		idCells = new ArrayList<Integer>();
		cellsROIs = new Hashtable<Integer, ArrayList<Roi>>();
		// tableInf = tableInfo;
		overlayResult = new SegmentationOverlay();
		if (overlayResult != null) {
			if (canvas.getImageOverlay() == null) {
				canvas.clearOverlay();
				raw_img.setOverlay(overlayResult.updateOverlay(all3dCells.get(43).dotsList, raw_img));
				overlayResult.setImage(raw_img);
				
				canvas.addOverlay(overlayResult);
				canvas.setImageOverlay(overlayResult);
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
