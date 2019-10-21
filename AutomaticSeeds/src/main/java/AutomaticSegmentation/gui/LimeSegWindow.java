/**
 * 
 */
package AutomaticSegmentation.gui;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import AutomaticSegmentation.limeSeg.SphereSegAdapted;
import eu.kiaru.limeseg.LimeSeg;
import eu.kiaru.limeseg.commands.ClearAll;
import eu.kiaru.limeseg.commands.SphereSegAdvanced;
import eu.kiaru.limeseg.opt.Optimizer;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.frame.RoiManager;
import net.miginfocom.swing.MigLayout;

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
	private JPanel Panel;
	private JButton btnSavePly;
	private JButton btRunSegmentation;
	
	private String initialDirectory;

	/**
	 * @throws HeadlessException
	 */
	public LimeSegWindow(ImagePlus workingImp) throws HeadlessException {
		// TODO Auto-generated constructor stub
		this.workingImp = workingImp;
		
		this.initialDirectory = workingImp.getOriginalFileInfo().directory;
			
		//processingFrame = new JFrame("LimeSeg");
		Panel = new JPanel();
		Panel.setLayout(new MigLayout());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		btStopOptimisation = new JButton("Stop");
		Panel.add(btStopOptimisation, "right");
		
		//btRunSegmentation = new JButton("Start");
		//Panel.add(btRunSegmentation, "right");
		
		btnSavePly = new JButton("Saved");
		Panel.add(btnSavePly, "left");
		
		getContentPane().add(Panel);
		
		SphereSegAdapted cf = new SphereSegAdapted();
		cf.setZ_scale(4.06);
		cf.setD_0(5);
		cf.setF_pressure((float)0.015);
		cf.setImp(this.workingImp);
		
        RoiManager roiManager = RoiManager.getRoiManager();
        if (roiManager==null) {
        	System.err.println("No roi manager found - command aborted.");
        } 
        if (roiManager.getRoisAsArray().length == 0) {
        	roiManager.runCommand("Open", new OpenDialog("Open Roi set").getPath());
        }
		
		cf.run();
		
		
//		//ClearAll clear = new ClearAll();
//		SphereSegAdvanced cf = new SphereSegAdvanced();
//		
//		
//		//clear.run();
//		LimeSeg ls = new LimeSeg();
//		ls.initialize();
//		ls.opt.setWorkingImage(this.workingImp, 1, 1);
//		LimeSeg.setWorkingImage(this.workingImp, 1, 1);
//        RoiManager roiManager = RoiManager.getRoiManager();
//        if (roiManager==null) {
//        	System.err.println("No roi manager found - command aborted.");
//        } 
//        if (roiManager.getRoisAsArray().length == 0) {
//        	roiManager.runCommand("Open", new OpenDialog("Open Roi set").getPath());
//        }
//        
//		cf.run();
//		System.out.println("Finish");
		
		//execute the listener in parallel with run to stop if is necessary
		ExecutorService executor1 = Executors.newSingleThreadExecutor();
		executor1.submit(() -> {
			
			btStopOptimisation.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					LimeSeg.stopOptimisation();
				}
			});

			btnSavePly.addActionListener(new ActionListener() {
		
				@Override
				public void actionPerformed(ActionEvent e) {
					String path = initialDirectory + "/OutputLimeSeg";
					File dir = new File(path);
					if (!dir.isDirectory()) {
						System.out.println("New folder created");
						dir.mkdir();
					}
					LimeSeg.saveStateToXmlPly(path);
					System.out.println("Saved");
				}
			});
			executor1.shutdown();
		});
		
		//btRunSegmentation.addActionListener(new ActionListener() {
			
			//@Override
			//public void actionPerformed(ActionEvent e) {
			//	SphereSegAdvanced cf = new SphereSegAdvanced();
			//	cf.run();
			//	System.out.println("Finish");
			//}
		//});
		
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
