package PostProcessingGland;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.smurn.jply.ElementReader;
import org.smurn.jply.ElementType;
import org.smurn.jply.PlyReader;
import org.smurn.jply.PlyReaderFile;
import org.w3c.dom.*;

/**
 * Class that will be called by Fiji/ImageJ and load the output of the 3D
 * Segmentation Adaptation of LimeSeg (Lipid Membrane Segmentation): a
 * coarse-grained lipid membrane simulation for 3D image segmentation - by Sarah
 * Machado et al. 
 * 
 * A computationally efficient and spatially continuous 3D
 * segmentation method. LimeSeg is easy-to-use and can process many and/or
 * highly convoluted objects. Based on the concept of SURFace ELements
 * (“Surfels”), LimeSeg resembles a highly coarse-grained simulation of a lipid
 * membrane in which a set of particles, analogous to lipid molecules, are
 * attracted to local image maxima. The particles are self-generating and
 * self-destructing thus providing the ability for the membrane to evolve
 * towards the contour of the objects of interest.
 * https://doi.org/10.1186/s12859-018-2471-0
 * 
 * @author Antonio Tagua
 */

public class IOXmlPlyLimeSeg {

	public IOXmlPlyLimeSeg() {
		// TODO Auto-generated constructor stub
	}

}
