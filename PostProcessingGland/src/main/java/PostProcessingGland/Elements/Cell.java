package PostProcessingGland.Elements;

import java.util.ArrayList;

import PostProcessingGland.IOPlyLimeSeg;
import PostProcessingGland.GUI.PostProcessingWindow;
import eu.kiaru.limeseg.struct.DotN;
import ij.gui.PointRoi;

public class Cell {
                         
  /**
   * static counter to generate new cell identifiers
   */
  public static int idCell = 1;  
  
  /**
   *  List of dots contained within this cell
   */
  public static ArrayList<DotN> dotsList;
 
  
  /**
   * 
   */
  public static String prefixCell = "cell_";
  
  /**
   * unique(?) String identifier of Cell 
   */
  public String id_Cell;   
  
  /**
   * Color of the cell format RGBA as float [3]
   * A is currently disabled
   */
  public float color[] = new float[] { 0.1f, 0.8f, 0.0f, 1.0f };
  
  /**
   * 
   * label of the folder where the cell coordinates are
   */
  public String labelCell;
  
  /**
   * Constructor
   */
  public Cell() {     
      //idCell++;
      id_Cell=prefixCell+idCell;
      // Dirty Hack to avoid duplicate keys...
      // Fix : change allCells into hashtable...
      boolean idAlreadyExists=false;
      for (Cell c: IOPlyLimeSeg.allCells) {
      	if (c.id_Cell.equals(id_Cell)) {
      		idAlreadyExists=true;
      		break;
      	}
      }
      if (idAlreadyExists) {
      	while (idAlreadyExists) {
      		idCell++;
      		id_Cell=prefixCell+idCell;
              // Dirty Hack to avoid duplicate keys...
              // Fix : change allCells into hashtable...
              idAlreadyExists=false;
              for (Cell c: IOPlyLimeSeg.allCells) {
              	if (c.id_Cell.equals(id_Cell)) {
              		idAlreadyExists=true;
              		break;
              	}
              }
      	}
      }
      id_Cell=prefixCell+idCell;
  }
  
  /**
   * Get the dots at a specific frame for this Cell
   * @param frame z of this Cell
   * @return the list of dots at this z frame, if it exists. returns null otherwise
   */
  public ArrayList<DotN> getCellAt(int frame) {
  	ArrayList<DotN> allDots = new ArrayList<DotN>();
  	for (int i=0;i<dotsList.size();i++) {
      if (allDots.get(i).pos.z == frame) {
              DotN dot = dotsList.get(i);
              allDots.add(dot);
          }
      }
      return allDots;
  } 
  
  /**
   * 
   * @param id
   * @param currentDots
   */
  
  public void addDots(int id, ArrayList<DotN> currentDots) {
    this.dotsList = currentDots;
    this.idCell = id;
} 

@Override
public String toString() {
	return this.id_Cell;
}

  
}