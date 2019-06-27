
package PostProcessingGland.Elements;

import java.util.ArrayList;

import PostProcessingGland.GUI.PostProcessingWindow;
import eu.kiaru.limeseg.struct.CellT;
import eu.kiaru.limeseg.struct.DotN;
import ij.gui.PointRoi;

public class Cell3D extends eu.kiaru.limeseg.struct.Cell {

	/**
	 * List of dots contained within this cell
	 */
	public ArrayList<DotN> dotsList;
	public Integer labelCell;

	/**
	 * Constructor
	 */
	public Cell3D(String id, ArrayList<DotN> dots) {
		dotsList = new ArrayList<DotN>();
		id_Cell = new String();
		this.dotsList = dots;
		this.id_Cell = id;
		}

	/**
	 * Get the dots at a specific frame for this Cell
	 * 
	 * @param frame z of this Cell
	 * @return the list of dots at this z frame, if it exists. returns null
	 *         otherwise
	 */
	public ArrayList<DotN> getCell3DAt(int frame) {
		ArrayList<DotN> allDots = new ArrayList<DotN>();
		for (int i = 0; i < dotsList.size(); i++) {
			 int zpos=1+(int)((float) (dotsList.get(i).pos.z/ (float) 4.06)); 
			if (zpos == frame) {
				DotN dot = dotsList.get(i);
				allDots.add(dot);
			}
		}
		return allDots;
	}

public void clearCell() {
    if (this.dotsList!=null) {
        this.dotsList.clear();
			}
}
    /**
     * 
     * @return the label of the cell 
     */
    public Integer getID() {
          return this.labelCell;
  			}
}
