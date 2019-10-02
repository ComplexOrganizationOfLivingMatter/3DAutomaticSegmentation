/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

import javax.swing.JButton;
import javax.swing.JFrame;

import eu.kiaru.limeseg.commands.SphereSegAdvanced;
import ij.IJ;
import ij.ImagePlus;

/**
 * @author Pablo Vicente-Munuera
 *
 */
public class LimeSegWindow extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ImagePlus workingImp;
	
	private float d_0;
	
	private float f_pressure;
	
	private float range_D_0;
	
	private JButton btStopOptimisation;
	
	private JButton btRunSegmentation;

	/**
	 * @throws HeadlessException
	 */
	public LimeSegWindow(ImagePlus workingImp) throws HeadlessException {
		// TODO Auto-generated constructor stub
		this.workingImp = workingImp;
		
		
		//WHEN CLICKING BUTTON RUNSEGMENTATION
//		
//		LimeSeg ls = new LimeSeg();
//		ls.initialize();
//		ls.opt.setWorkingImage(IJ.openImage(), 1, 1);
//		ls.run();
////		
////		JPanelLimeSeg jp = new J(ls);
////		jp.setVisible(true);
//		
//		JPanelLimeSeg jp = new JPanelLimeSeg(ls);
//		jp.setVisible(true);
//		jp.repaint();
		
		SphereSegAdvanced cf = new SphereSegAdvanced();
		cf.run();
	}

	/**
	 * @return the f_pressure
	 */
	public float getF_pressure() {
		return f_pressure;
	}

	/**
	 * @param f_pressure the f_pressure to set
	 */
	public void setF_pressure(float f_pressure) {
		this.f_pressure = f_pressure;
	}

	/**
	 * @return the workingImp
	 */
	public ImagePlus getWorkingImp() {
		return workingImp;
	}

	/**
	 * @param workingImp the workingImp to set
	 */
	public void setWorkingImp(ImagePlus workingImp) {
		this.workingImp = workingImp;
	}

	/**
	 * @return the d_0
	 */
	public float getD_0() {
		return d_0;
	}

	/**
	 * @param d_0 the d_0 to set
	 */
	public void setD_0(float d_0) {
		this.d_0 = d_0;
	}

	/**
	 * @return the range_D_0
	 */
	public float getRange_D_0() {
		return range_D_0;
	}

	/**
	 * @param range_D_0 the range_D_0 to set
	 */
	public void setRange_D_0(float range_D_0) {
		this.range_D_0 = range_D_0;
	}

}
