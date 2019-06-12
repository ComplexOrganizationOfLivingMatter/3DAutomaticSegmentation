package PostProcessingGland.GUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;

// import JTableModel;
import PostProcessingGland.GUI.CustomElements.CustomCanvas;
import PostProcessingGland.GUI.CustomElements.ImageOverlay;
import epigraph.JTableModel;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

public class PostProcessingWindows extends ImageWindow implements ActionListener {

	private ArrayList<ImageOverlay> OverlayResultList;
	private ArrayList<ArrayList<String>> TotalpolDistriRoi;
	private ArrayList<Integer> IdCells;
	private Hashtable<Integer, ArrayList<Roi>> z_position;
	private CustomCanvas canvas;
	private ImageOverlay overlayResult;
	
	private String initialDirectory;
	// private JTableModel tableInf;
	
	public PostProcessingWindows(ImagePlus raw_img) {
		super(raw_img, new CustomCanvas(raw_img));
		//super(raw_img, new CustomCanvas(raw_img));
		
		this.initialDirectory = raw_img.getOriginalFileInfo().directory;
	
		canvas = (CustomCanvas) super.getCanvas();
		
		// Init parameters
		IdCells = new ArrayList<Integer>();
		OverlayResultList = new ArrayList<ImageOverlay>();
		z_position = new Hashtable<Integer, ArrayList<Roi>>();
		// tableInf = tableInfo;
		overlayResult = new ImageOverlay();

		// TODO Auto-generated constructor stub
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
