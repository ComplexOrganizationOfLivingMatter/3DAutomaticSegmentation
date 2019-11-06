package AutomaticSegmentation.preProcessing;

import ij.IJ;
import ij.ImagePlus;
import java.io.File;
import filters.Bandpass3D;

/**
 *
 * @author pedgomgal1
 */

public class NucleiSegmentation3D{

	public ImagePlus impPreprocessed;
	public ImagePlus impSegmented;
	public String dir;
	
	public NucleiSegmentation3D() {
		impPreprocessed = null;
        impSegmented = null;
        dir = null;
	}
    
    public NucleiSegmentation3D(ImagePlus nucleiImg, int max_nuc_radius, int min_nuc_radius, float zDepth, int maxThresh,boolean prefilter) {
				
    	dir = nucleiImg.getOriginalFileInfo().directory;
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
            subdir = dir + "SEG/";
        }
        
        
    	impPreprocessed = nucleiImg.duplicate();
        impPreprocessed.setTitle("Dapi_channel");
        if(prefilter){
            IJ.log("Pre-filtering start");
            IJ.run(impPreprocessed, "Median 3D...", "x=4 y=4 z=2");
            IJ.log("Pre-filtering completed");           
        }
        
        IJ.saveAs(impPreprocessed,"Tiff", subdir+impPreprocessed.getTitle()+".tif");
        IJ.log("Save the nuclei image as Dapi_channel.tif in "+subdir);
            
        //band pass filtering
        IJ.run(impPreprocessed, "32-bit", "");
        IJ.log("Computing Band Pass...");

		// filter image with 3D-bandpass
 		Bandpass3D bp3d = new Bandpass3D();
        
		bp3d.in_hprad = max_nuc_radius;
 		bp3d.in_lprad = min_nuc_radius;
 		bp3d.in_xz_ratio = zDepth;
		bp3d.in_image = impPreprocessed.getStack();
		if (!bp3d.checkInputParams().equals("")) {
			IJ.showMessage(bp3d.checkInputParams());
			return;
		}
		bp3d.filterit();
 		impPreprocessed = new ImagePlus("BP_nuclei", bp3d.out_result);
 		
 		
        IJ.saveAs(impPreprocessed,"Tiff", subdir+"BP_nuclei.tif");
        IJ.log("Save the band pass filtered image as BP_nuclei.tif in "+subdir);
        impSegmented = impPreprocessed.duplicate();
        impSegmented.setSlice(impSegmented.getNSlices()/2);
        IJ.run(impSegmented, "Enhance Contrast...", "saturated=0"); 
        IJ.run(impSegmented, "16-bit", "");

        //nuclei segmentation
        IJ.log("Segmenting...");
        String seg_option = "seeds_threshold="+maxThresh+" local_background=0 radius_0=2 radius_1=4 radius_2=6 weigth=0.50 radius_max="+max_nuc_radius+" sd_value="+max_nuc_radius/10+" local_threshold=[Gaussian fit] seg_spot=Block watershed volume_min="+Math.round((4/3)*Math.PI*min_nuc_radius)+" volume_max="+Math.round((4/3)*Math.PI*max_nuc_radius)+" seeds=Automatic spots=BP_nuclei radius_for_seeds=2 output=[Label Image]";
        IJ.run(impSegmented,"3D Spot Segmentation",seg_option);
        IJ.saveAs(impSegmented,"Tiff", subdir+"dapi-seg.tif");
        IJ.log("Save the segmented nuclei image as dapi-seg.tif in "+subdir);
        
    }
        
	      
}
