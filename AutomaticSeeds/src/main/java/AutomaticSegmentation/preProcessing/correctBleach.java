package AutomaticSegmentation.preProcessing;

import histogram2.HistogramMatcher;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class correctBleach {
	
	ImagePlus impNuclei;
	ImagePlus impCellOutline;

	Roi curROI = null;
	/**
	 * @param imp
	 * @param imp2
	 */
	public correctBleach(ImagePlus imp,ImagePlus imp2) {
		super();
		this.impNuclei = imp;
		this.impCellOutline = imp2;

	}
	/** This constructor might not going to be used.
	 * 	Does not mean much to select a specific region for the reference histogram.
	 * @param imp
	 * @param imp2
	 * @param curROI
	 */
	public correctBleach(ImagePlus imp,ImagePlus imp2, Roi curROI) {
		super();
		this.impNuclei = imp;
		this.impCellOutline = imp2;
		this.curROI = curROI;
	}

	public void doCorrection(){

		int histbinnum = 0;
		int i_intermediate = 0;
		if (impNuclei.getBitDepth()==8) histbinnum = 256;
			else if (impNuclei.getBitDepth()==16) histbinnum = 65536;//65535;

		boolean is3DT = false;
		int zframes = 1;
		int timeframes = 1;
		int[] impdimA = impNuclei.getDimensions();
		IJ.log("slices"+Integer.toString(impdimA[3])+"  -- frames"+Integer.toString(impdimA[4]));
		//IJ.log(Integer.toString(imp.getNChannels())+":"+Integer.toString(imp.getNSlices())+":"+ Integer.toString(imp.getNFrames()));
		if (impdimA[3]>1 && impdimA[4]>1){	// if slices and frames are both more than 1
			is3DT =true;
			zframes = impdimA[3];
			timeframes = impdimA[4];
			if ((zframes*timeframes) != impNuclei.getStackSize()){
				IJ.showMessage("slice and time frames do not match with the length of the stack. Please correct!");
				return;
			}
		}
		ImageStack stackNuclei = impNuclei.getStack();
		ImageStack stackCellOutline = impCellOutline.getStack();
		ImageProcessor ipA = null;
		ImageProcessor ipB = null;
		HistogramMatcher m = new HistogramMatcher();
		int[] hA = new int[histbinnum];
		int[] hB = new int[histbinnum];
		int[] F = new int[histbinnum];
		int[] histB = null;	//for each slice
		int[] histA = null;
		//IJ.log(Integer.toString(stack.getSize()));
		int i =0;
		int j =0;
		int k =0;
		/* in case of 3D, stack histogram of the first time point is measured, and then
		 *  this stack histogram is used as reference (hB) for the rest of time points.
		 */
		if (is3DT){
			//should implement here,
			i_intermediate = 1;//Math.round(stackCellOutline.getSize()/10);
			for (j=0; j<zframes; j++){
				ipB = stackCellOutline.getProcessor(i_intermediate*zframes+j+1);
				histB = ipB.getHistogram();
				for (k=0; k<histbinnum; k++) hB[k] += histB[k];
			}
			
			for (i =0; i< timeframes; i++){
				for (k=0; k<histbinnum; k++) hA[k] = 0;
				for (j=0; j<zframes; j++){
					ipA = stackNuclei.getProcessor(i*zframes+j+1);
					histA = ipA.getHistogram();
					for (k=0; k<histbinnum; k++) hA[k] += histA[k];
				}
				F = m.matchHistograms(hA, hB);
				for (j=0; j<zframes; j++){
					ipA = stackNuclei.getProcessor(i*zframes+j+1);
					ipA.applyTable(F);
				}
				IJ.log("corrected time point: "+Integer.toString(i+1));
			}

		} else {		//2D case.
			
			i_intermediate = 1;//Math.round(stackCellOutline.getSize()/10);
			ipB = stackCellOutline.getProcessor(i_intermediate);
			hB = ipB.getHistogram();
			
			for (i=0; i<stackNuclei.getSize(); i++){

					ipA = stackNuclei.getProcessor(i+1);
					hA = ipA.getHistogram();
					F = m.matchHistograms(hA, hB);
					ipA.applyTable(F);
					IJ.log("corrected frame: "+Integer.toString(i+1));
			}
		}
		//imp.show();
	}
	
	public int Sum(int[] arrayInt)
	{
	   int result = 0;

	   for(int i = 0; i < arrayInt.length; i++)
	   {
	      result += arrayInt[i];
	   }

	   return result;
	}
	
	public float Average(int[] arrayInt)
	{
	   int sum = Sum(arrayInt);
	   float result = (float)sum / arrayInt.length;
	   return result;
	}


}
