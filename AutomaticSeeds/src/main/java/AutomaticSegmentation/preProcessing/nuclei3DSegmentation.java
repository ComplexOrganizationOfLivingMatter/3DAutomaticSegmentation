package AutomaticSegmentation.preProcessing;

import java.awt.Color;
import java.awt.image.ColorModel;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JProgressBar;

import filters.Bandpass3D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Filters3D;
import ij.process.ImageConverter;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.util.ColorMaps;
import inra.ijpb.util.ColorMaps.CommonLabelMaps;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.kernels.Kernels;

public class nuclei3DSegmentation{

	private int max_nuc_radius, min_nuc_radius, maxThresh;
	private float zStep;
	private String dir;
	private Boolean cancelTask;
	private JProgressBar progressBar;
	private ImagePlus nucleiChannel,preprocessedImp,segmentedImp;
	private JCheckBox prefilteringCheckB,quickSegmentationCheckB,gpuCheckBox;
	private quickSegmentation qseg;
		
	public nuclei3DSegmentation(ImagePlus nucleiChannel,int max_nuc_radius, int min_nuc_radius, int maxThresh, float zStep,
			String dir, Boolean cancelTask, JProgressBar progressBar,JCheckBox prefilteringCheckB, JCheckBox quickSegmentationCheckB, JCheckBox gpuCheckBox) {
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
		this.gpuCheckBox = gpuCheckBox;
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
	        
	        
	        /******************** Median filter **********************/
	        if(prefilteringCheckB.isSelected()){
	        	
	            IJ.log("Pre-filtering start");
	            filterPreprocessing(preprocessedImp, 4, 4, 2);
	            IJ.log("Pre-filtering completed");   
	            
	            if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
	            
	            progressBar.setValue(12);
		        IJ.saveAs(preprocessedImp,"Tiff", subdir+preprocessedImp.getTitle()+".tif");
		        IJ.log("Save the nuclei image as dapi_channel.tif in "+subdir);
	        }

	        if (cancelTask.booleanValue()) {
	        	IJ.log("nuclei segmentation STOPPED");
	        	break;
	        }
	       
	        /********************** QUICK SEGMENTATION PROTOCOL **********************/
	        if (quickSegmentationCheckB.isSelected()) {
	        	
	        	/****************** Enhance contrast ******************/
	        	//preprocessedImp.setSlice(preprocessedImp.getNSlices()/2);
		        //IJ.run(preprocessedImp, "Enhance Contrast...", "saturated=0");
		        		        
		        if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        progressBar.setValue(15);
	        	qseg = new quickSegmentation(preprocessedImp,min_nuc_radius, progressBar,cancelTask);
	        	
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
	        	
	        	
	        }
	        /********************** SPOT 3D SEGMENTATION **********************/
	        else {        
		        /************** BandPass 3D filtering ***************/
	        	IJ.run(preprocessedImp, "32-bit", "");
		        IJ.log("Computing Band Pass...");
		        
		        preprocessedImp.duplicate().show();
		        
		        if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        progressBar.setValue(35);
		        preprocessedImp = runBandPass3D(preprocessedImp);
		        progressBar.setValue(55);
		 		
		        preprocessedImp.duplicate().show();
		        
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
		        
		        segmentedImp.duplicate().show();

		        if (cancelTask.booleanValue()) {
		        	IJ.log("nuclei segmentation STOPPED");
		        	break;
		        }
		        progressBar.setValue(60);
		        
		        IJ.run(segmentedImp, "16-bit", "");
		        
		        segmentedImp.duplicate().show();
		        
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
		       
		        if (cancelTask.booleanValue()) {
		        	IJ.log("Nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        segmentedImp = sp3dSeg.getImpOutput();
		        progressBar.setValue(95);
		        
		        /******************** Label and colour the nuclei *********************/
		        segmentedImp = createColouredImageWithLabels(nucleiChannel, segmentedImp.getStack());
		        
		        if (cancelTask.booleanValue()) {
		        	IJ.log("Nuclei segmentation STOPPED");
		        	break;
		        }
		        
		        segmentedImp.setTitle("dapi-seg");
		        IJ.saveAs(segmentedImp,"Tiff", subdir+"dapi-seg.tif");       
		        IJ.log("Save the segmented nuclei image as dapi-seg.tif in "+subdir);
		        progressBar.setValue(100);

	        }
    	}
		return segmentedImp;
		
	}
	/**
	 * 
	 * @param imp
	 * @return
	 */
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
	
	/**
	 * 
	 * @param nucleiImp
	 * @param xRad
	 * @param yRad
	 * @param zRad
	 * @return
	 */
	public ImagePlus filterPreprocessing(ImagePlus nucleiImp, int xRad,int yRad, int zRad) {
		
		ImagePlus nucleiImp2 = nucleiImp.duplicate(); 
		if (gpuCheckBox.isSelected()) { // More info at https://clij.github.io/clij-docs/referenceJava
			try {
				// init CLIJ and GPU
				CLIJ clij = CLIJ.getInstance();			
				ClearCLBuffer inputClij = clij.push(nucleiImp);
				ClearCLBuffer temp = clij.create(inputClij);
				Kernels.medianBox(clij, inputClij, temp, xRad, yRad, zRad);
				nucleiImp2 = clij.pull(temp);
				inputClij.close();
				temp.close();
			}
			catch(Exception ex){
				ex.printStackTrace();
				IJ.error("Try deselect 'use GPU' ");
			}
		} else {
			Filters3D.filter(nucleiImp2.getStack(), Filters3D.MEDIAN, xRad, yRad, zRad);
		}
		return nucleiImp2;
	}
	/**
	 * 
	 * @param initImp
	 * @param imgFilterSize
	 * @return
	 */
	public static ImagePlus createColouredImageWithLabels(ImagePlus initImp, ImageStack imgFilterSize) {
		/*** get colored labels and return image ***/
		// create image with watershed result
		ImagePlus imp_segmentedFinal = new ImagePlus("filtered size", imgFilterSize);
		// assign right calibration
		imp_segmentedFinal.setCalibration(initImp.getCalibration());
		// optimize display range
		Images3D.optimizeDisplayRange(imp_segmentedFinal);
		// Convert the segmented image to 8-Bit
		ImageConverter converterFinal = new ImageConverter(imp_segmentedFinal);
		converterFinal.convertToGray8();
		// Color image
		byte[][] colorMap = CommonLabelMaps.fromLabel(CommonLabelMaps.GOLDEN_ANGLE.getLabel()).computeLut(255, false);
		// Border, color
		ColorModel cm = ColorMaps.createColorModel(colorMap, Color.BLACK);

		imp_segmentedFinal.getProcessor().setColorModel(cm);
		imp_segmentedFinal.getImageStack().setColorModel(cm);
		imp_segmentedFinal.updateAndDraw();
		return imp_segmentedFinal;
	}
	
	/**
	 * 
	 * @param cancelTask
	 */
	public void setCancelTask(Boolean cancelTask) {
		this.cancelTask = cancelTask.booleanValue();
		if (quickSegmentationCheckB.isSelected()) {
			qseg.setCancelTask(cancelTask);
		}
	}

}
