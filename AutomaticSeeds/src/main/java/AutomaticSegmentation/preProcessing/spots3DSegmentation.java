package AutomaticSegmentation.preProcessing;

import java.util.ArrayList;

import javax.swing.JProgressBar;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import mcib3d.geom.Object3D;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.Segment3DSpots;
import mcib3d.image3d.processing.FastFilters3D;
import mcib_plugins.tools.RoiManager3D_2;

/**
 * Description of the Class
 *
 * adapted from @author thomas (2006) by @author pedgomgal1
 * 
 */
public class spots3DSegmentation {

	JProgressBar progressBar;
	ImagePlus seedPlus;
    ImageStack seedStack;
    ImageHandler seed3DImage;
    // spots
    ImagePlus spotPlus;
    ImageStack spotStack;
    ImageHandler spot3DImage;
    // segResulrs
    //IntImage3D fishImage;
    ImagePlus segPlus = null;
    ImageStack segStack;
    // res
    Calibration spotCalib = null;
    double resXY = 0.1328;
    double resZ = 0.2;
    double radiusFixed = 0;
    double weight = 0.5;
    int local_method = 0;
    int spot_method = 0;
    int output = 0;
    int seeds_threshold = 15;
    int local_background = 65;
    // local mean
    float rad0 = 2;
    float rad1 = 4;
    float rad2 = 6;
    double we = 0.5;
    // gauss_fit
    int radmax = 10;
    double sdpc = 1.0;
    private boolean watershed = true;
    private int radiusSeeds = 2;
    // volumes (pix)
    int volumeMin = 100;
    int volumeMax = 1000000;
    String[] local_methods = {"Constant", "Diff", "Local Mean", "Gaussian fit"};
    String[] spot_methods = {"Classical", "Maximum", "Block"};
    String[] outputs = {"Label Image", "Roi Manager 3D", "Both"};
    // DB
    private boolean debug = true;
    private boolean bigLabel;
    private int diff = 0;
    
    public void spotSegmentation(ImagePlus inputImp, int maxThresh, int max_nuc_radius, int min_nuc_radius,JProgressBar progressBar) {
        
        // initialize segmentation parameters
        seeds_threshold = maxThresh;
        local_background = 0;
        diff = 0;
        rad0 = 2;
        rad1 = 4;
        rad2 = 6;
        we = 0.5;
        radmax = max_nuc_radius*4;
        sdpc = max_nuc_radius/10;
        local_method = 3;//"[Gaussian fit]";
        spot_method = 2;//"Block";
        output = 0;//"[Label Image]";
        watershed = true;
        volumeMin = (int) Math.round((4/3)*Math.PI*min_nuc_radius*min_nuc_radius*min_nuc_radius);
        volumeMax = (int) Math.round((4/3)*Math.PI*max_nuc_radius*max_nuc_radius*max_nuc_radius*100);
        this.progressBar = progressBar;
        
        // in case old values was stored
        if (spot_method >= spot_methods.length) {
            spot_method = 0;
        }
        
        spotStack = inputImp.getImageStack();
        spot3DImage = ImageHandler.wrap(inputImp);
        
        computeSeeds();
        
        if (inputImp.getCalibration() != null) {
            spotCalib = inputImp.getCalibration();
        }
        IJ.log("Spot segmentation.....");
        this.Segmentation();
        IJ.log("Finished");
        if (segPlus != null) {
            segPlus.show();
        }
        IJ.log("Finished");
        
    }
	
    
    private void computeSeeds() {
        seed3DImage = ImageHandler.wrap(FastFilters3D.filterIntImageStack(spotStack, FastFilters3D.MAXLOCAL, (float) radiusSeeds, (float) radiusSeeds, (float) radiusSeeds, 0, false));
    }
    
    private void Segmentation() {
        Segment3DSpots seg = new Segment3DSpots(this.spot3DImage, this.seed3DImage);
        seg.show = debug;
               
        progressBar.setValue(62);
        // set parameters
        seg.setSeedsThreshold(this.seeds_threshold);
        seg.setLocalThreshold(local_background);
        seg.setWatershed(watershed);
        seg.setVolumeMin(volumeMin);
        seg.setVolumeMax(volumeMax);
        IJ.log("Spot Image: " + seg.getRawImage().getTitle() + "   Seed Image : " + seg.getSeeds().getTitle());
        IJ.log("Vol min: " + seg.getVolumeMin() + "   Vol max: " + seg.getVolumeMax());
        
        progressBar.setValue(63);
        
        switch (local_method) {
            case 0:
                seg.setMethodLocal(Segment3DSpots.LOCAL_CONSTANT);
                break;
            case 1:
                seg.setMethodLocal(Segment3DSpots.LOCAL_DIFF);
                seg.setLocalDiff(diff);
                break;
            case 2:
                seg.setMethodLocal(Segment3DSpots.LOCAL_MEAN);
                seg.setRadiusLocalMean(rad0, rad1, rad2, we);
                break;
            case 3:
                seg.setMethodLocal(Segment3DSpots.LOCAL_GAUSS);
                seg.setGaussPc(sdpc);
                seg.setGaussMaxr(radmax);
                break;
        }
        
        switch (spot_method) {
            case 0:
                seg.setMethodSeg(Segment3DSpots.SEG_CLASSICAL);
                break;
            case 1:
                seg.setMethodSeg(Segment3DSpots.SEG_MAX);
                break;
            case 2:
                seg.setMethodSeg(Segment3DSpots.SEG_BLOCK);
                break;
        }
        
        // big label (more than 2^16 objects)
        seg.bigLabel = bigLabel;
        
        progressBar.setValue(65);
        
        seg.segmentAll();
        
        progressBar.setValue(92);
        int size = seg.getObjects().size();
        IJ.log("Number of labelled objects: " + size);
        // output        
        if ((output == 0) || (output == 2)) {
            segPlus = new ImagePlus("seg", seg.getLabelImage().getImageStack());
            if (spotCalib != null) {
                segPlus.setCalibration(spotCalib);
            }
        }
        if ((output == 1) || (output == 2)) {
            ArrayList<Object3D> Objects = seg.getObjects();
            RoiManager3D_2 roimanager = new RoiManager3D_2();
            roimanager.addObjects3D(Objects);
        }
    }
    
    public ImagePlus getImpOutput() {
    	return segPlus;
    }
    
}

