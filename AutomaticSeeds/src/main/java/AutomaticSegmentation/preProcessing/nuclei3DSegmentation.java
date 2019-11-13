package AutomaticSegmentation.preProcessing;

import java.awt.peer.PanelPeer;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JProgressBar;

import AutomaticSegmentation.gui.PanelPreProcessing;
import filters.Bandpass3D;
import ij.IJ;
import ij.ImagePlus;

public class nuclei3DSegmentation implements genericSegmentation{

	private int max_nuc_radius, min_nuc_radius, maxThresh;
	private float zStep;
	private String dir;
	private Boolean cancelTask;
	private JProgressBar progressBar;
	private ImagePlus nucleiChannel,preprocessedImp,segmentedImp;
	private JCheckBox prefilteringCheckB,quickSegmentationCheckB;
	
	public nuclei3DSegmentation(ImagePlus nucleiChannel,int max_nuc_radius, int min_nuc_radius, int maxThresh, float zStep,
			String dir, Boolean cancelTask, JProgressBar progressBar,JCheckBox prefilteringCheckB, JCheckBox quickSegmentationCheckB) {
		super();
		this.max_nuc_radius = max_nuc_radius;
		this.min_nuc_radius = min_nuc_radius;
		this.maxThresh = maxThresh;
		this.zStep = zStep;
		this.dir = dir;
		this.cancelTask = cancelTask;
		this.progressBar = progressBar;
		this.nucleiChannel = nucleiChannel;
		this.prefilteringCheckB = prefilteringCheckB;
		this.quickSegmentationCheckB = quickSegmentationCheckB;
	}
	
	public ImagePlus segmentationProtocol() {
		preprocessedImp = null;
		segmentedImp = null;
		while(!cancelTask.booleanValue() && progressBar.getValue()!=100) {
	    	String subdir = null;
	        File fl = new File(dir+"SEG");
	        if (!fl.exists()){
	            if (fl.mkdir()) {
	                subdir = dir + "SEG/";
	            } else {
	                subdir = dir;
	            }
	        }
	        else{
	            subdir = this.dir + "SEG/";
	        }
	        
	        progressBar.setValue(3);
	    	preprocessedImp = nucleiChannel.duplicate();
	    	preprocessedImp.setTitle("dapi_channel");
	        
	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	        
	        if(prefilteringCheckB.isSelected()){
	        	
	            IJ.log("Pre-filtering start");
	            
	            //IF GPU ----> CLIJ median 3D
	            
	            IJ.run(this.preprocessedImp, "Median 3D...", "x=4 y=4 z=2");
	            IJ.log("Pre-filtering completed");   
	            
	            if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
	            
	            progressBar.setValue(15);
		        IJ.saveAs(preprocessedImp,"Tiff", subdir+preprocessedImp.getTitle()+".tif");
		        IJ.log("Save the nuclei image as dapi_channel.tif in "+subdir);
	        }

	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	       

	        if (quickSegmentationCheckB.isSelected()) {
	        	preprocessedImp.setSlice(preprocessedImp.getNSlices()/2);
		        IJ.run(preprocessedImp, "Enhance Contrast...", "saturated=0");
		        progressBar.setValue(19);
	        	QuickSegmentation qseg = new QuickSegmentation(preprocessedImp, progressBar,cancelTask);
	        	
	        	if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
	        	qseg.segmentationProtocol();
	        	
	        	if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
	        	
	        	segmentedImp = qseg.getOuputImp();
	        	segmentedImp.setTitle("dapi-seg");
		        IJ.saveAs(segmentedImp,"Tiff", subdir+"dapi-seg.tif");

		        IJ.log("Save the segmented nuclei image as dapi-seg.tif in "+subdir);
		        progressBar.setValue(100);
	        	
	        	
	        }else {        
		        /************** BandPass 3D filtering ***************/
	        	IJ.run(preprocessedImp, "32-bit", "");
		        IJ.log("Computing Band Pass...");
		        
		        if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        progressBar.setValue(35);
		        preprocessedImp = runBandPass3D(preprocessedImp);
		        progressBar.setValue(55);
		 			 		
		        if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }

		        IJ.saveAs(this.preprocessedImp,"Tiff", subdir+"BP_nuclei.tif");
		        IJ.log("Save the band pass filtered image as BP_nuclei.tif in "+subdir);
		        
		        
		        /********************** Enhance contrast **********************/
		        segmentedImp = preprocessedImp.duplicate();
		        segmentedImp.setSlice(segmentedImp.getNSlices()/2);
		        IJ.run(this.segmentedImp, "Enhance Contrast...", "saturated=0"); 
		        
		        if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
		        progressBar.setValue(60);
		        
		        IJ.run(segmentedImp, "16-bit", "");
		        
		        if (this.cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        progressBar.setValue(61);
		        
		        /****************** Spot 3D segmentation ********************/
		        IJ.log("Spot 3D segmentation");
		        //String seg_option = "seeds_threshold="+maxThresh+" local_background=0 radius_0=2 radius_1=4 radius_2=6 weigth=0.50 radius_max="+max_nuc_radius+" sd_value="+max_nuc_radius/10+" local_threshold=[Gaussian fit] seg_spot=Block watershed volume_min="+Math.round((4/3)*Math.PI*min_nuc_radius)+" volume_max="+Math.round((4/3)*Math.PI*max_nuc_radius)+" seeds=Automatic spots=BP_nuclei radius_for_seeds=2 output=[Label Image]";
		        //IJ.run(segmentedImp,"3D Spot Segmentation",seg_option);
		        
		        spots3DSegmentation sp3dSeg = new spots3DSegmentation();
		        sp3dSeg.spotSegmentation(segmentedImp, maxThresh, max_nuc_radius, min_nuc_radius,progressBar);
		        segmentedImp = sp3dSeg.getImpOutput();
		        progressBar.setValue(95);
		        
		        /******************** Label and colour the nuclei *********************/
		        segmentedImp = createColouredImageWithLabels(nucleiChannel, segmentedImp.getStack());
		        segmentedImp.setTitle("dapi-seg");
		        IJ.saveAs(segmentedImp,"Tiff", subdir+"dapi-seg.tif");
		        
		        if (cancelTask.booleanValue()) {
		        	IJ.log("Nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        IJ.log("Save the segmented nuclei image as dapi-seg.tif in "+subdir);
		        progressBar.setValue(100);

	        }
    	}
		return segmentedImp;
		
	}
	
	public ImagePlus runBandPass3D(ImagePlus imp) {
		// filter image with 3D-bandpass
 		Bandpass3D bp3d = new Bandpass3D();
        
		bp3d.in_hprad = max_nuc_radius;
 		bp3d.in_lprad = min_nuc_radius;
 		bp3d.in_xz_ratio = zStep;
		bp3d.in_image = imp.getStack();
		if (!bp3d.checkInputParams().equals("")) {
			IJ.showMessage(bp3d.checkInputParams());
		}
		bp3d.filterit();
		
        imp = new ImagePlus("BP_nuclei", bp3d.out_result);
        
        return imp;
	}
	
}
