/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AutomaticSegmentation.preProcessing;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import java.io.File;
import filters.Bandpass3D;

/**
 *
 * @author pedgomgal1
 */
public class NucleiSegmentation3D{
	
    String dir = null;
    int max_nuc_radius, min_nuc_radius, seed_threshold;
    boolean prefilter;
    ImagePlus nucleiImg;
    
    public NucleiSegmentation3D(ImagePlus nucleiImg,String dir, int max_nuc_radius, int min_nuc_radius, int seed_threshold,boolean prefilter) {
		super();
		this.nucleiImg = nucleiImg;
		this.dir = dir;
		this.max_nuc_radius = max_nuc_radius;
		this.min_nuc_radius = min_nuc_radius;
		this.seed_threshold = seed_threshold;
		this.prefilter = prefilter;
	}

   
    public ImagePlus segment()
    {
        ImagePlus imp2 = null;
        ImagePlus imp3 = null;
        if(prefilter)
        {
            IJ.log("Pre-filtering...");
            IJ.run(nucleiImg, "Median 3D...", "x=4 y=4 z=2");
            IJ.log("Duplicate...");
            imp2 = nucleiImg.duplicate();
            imp2.setTitle("C4-dapi");
            if(dir!=null)
            {
                IJ.selectWindow(imp2.getTitle());
                IJ.saveAs("Tiff", dir+imp2.getTitle()+".tif");
                IJ.log("Save the nuclei image as C4-dapi.tif in "+dir);
            }    
            
        }
        else{
            IJ.log("Duplicate...");
            imp2 = nucleiImg.duplicate();
            imp2.setTitle("C4-dapi");
//            if(dir!=null)
//            {
//                IJ.saveAs("Tiff", dir+"C4-dapi.tif");
//                IJ.log("Save the nuclei image as C4-dapi.tif in "+dir);
//            }  
        }
        
        //band pass filtering
        IJ.run(imp2, "32-bit", "");
        IJ.log("Computing Band Pass...");
//        // This is probabliy a shit
//        DF_Bandpass df = new DF_Bandpass();
//        // This is better
//		// filter image with 3D-bandpass
//		Bandpass3D bp3d = new Bandpass3D();
//		bp3d.in_hprad = hprad;
//		bp3d.in_lprad = lprad;
//		bp3d.in_xz_ratio = zdx;
//		bp3d.in_image = stack.getStack();
//		if (!bp3d.checkInputParams().equals("")) {
//			IJ.showMessage(bp3d.checkInputParams());
//			return;
//		}
//		bp3d.filterit();
//		ImagePlus impOut = new ImagePlus("BP_" + stack.getTitle(), bp3d.out_result);
        IJ.run(imp2, "DF_Bandpass", "maximal_feature_size="+max_nuc_radius+" minimal_feature_size="+min_nuc_radius+" z/x_aspect_ratio=3.000");
        IJ.selectWindow("BP_C4-dapi");
        if(dir!=null)
        {
            String subdir = null;
            File fl = new File(dir+"SEG");
            if (!fl.exists()) 
            {
                if (fl.mkdir()) {
                    subdir = dir + "SEG/";
                } else {
                    subdir = dir;
                }
            }
            else{
                subdir = dir + "SEG/";
            }
            IJ.selectWindow("BP_C4-dapi");
            IJ.saveAs("Tiff", subdir+"BP_C4-dapi.tif");
            IJ.log("Save the band pass filtered image as BP_C4-dapi.tif in "+subdir);
            imp3 = IJ.openImage(subdir+"BP_C4-dapi.tif");
            imp3.setSlice(imp3.getNSlices()/2);
            IJ.run(imp3, "Enhance Contrast...", "saturated=0"); 
            IJ.run(imp3, "16-bit", "");

            //nuclei segmentation
            IJ.log("Segmenting...");
            String seg_option = "seeds_threshold="+seed_threshold+" local_background=0 radius_0=2 radius_1=4 radius_2=6 weigth=0.50 radius_max=20 sd_value=1.8 local_threshold=[Gaussian fit] seg_spot=Block watershed volume_min=100 volume_max=1000000 seeds=Automatic spots=BP_C4-dapi radius_for_seeds=2 output=[Label Image]";
            IJ.run("3D Spot Segmentation",seg_option);
            IJ.selectWindow("seg");
            IJ.saveAs("Tiff", subdir+"dapi-seg.tif");
            IJ.log("Save the segmented nuclei image as dapi-seg.tif in "+subdir);
        }
        else
        {
            imp3 = WindowManager.getImage("BP_C4-dapi");
            IJ.log(imp3.getTitle()+" data here");
    //        ImagePlus imp3 = IJ.openImage(dir+"BP_C4-dapi.tif");
            imp3.setSlice(imp3.getNSlices()/2);
            IJ.run(imp3, "Enhance Contrast...", "saturated=0"); 
            IJ.run(imp3, "16-bit", "");

            //nuclei segmentation
            IJ.log("Segmenting...");
            String seg_option = "seeds_threshold="+seed_threshold+" local_background=0 radius_0=2 radius_1=4 radius_2=6 weigth=0.50 radius_max=20 sd_value=1.8 local_threshold=[Gaussian fit] seg_spot=Block watershed volume_min=100 volume_max=1000000 seeds=Automatic spots=BP_C4-dapi radius_for_seeds=2 output=[Label Image]";
            IJ.run("3D Spot Segmentation",seg_option);
            IJ.selectWindow("seg");
            IJ.log("Program can not read saving directory folder, please manual save image");
        }
        
		return imp3;
    }        
}
