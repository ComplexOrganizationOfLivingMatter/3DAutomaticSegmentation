package PostProcessingGland.GUI.CustomElements;




import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import fiji.util.gui.OverlayedImageCanvas.Overlay;
import ij.process.ImageProcessor;

/**
 * This class implements an overlay based on an image. The overlay paints the
 * image with a specific composite mode.
 * 
 * @author Ignacio Arganda-Carreras
 *
 */
public class ImageOverlay implements Overlay {

	private ImageProcessor imp = null;
	private Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f);

	/**
	 * Constructor by default
	 */
	public ImageOverlay() {
		super();
	}

	/**
	 * 
	 * @param imp
	 *            image overlay to set
	 */
	public ImageOverlay(ImageProcessor imp) {
		this.imp = imp;
	}

	/*
	 * @Override(non-Javadoc)
	 * 
	 * @see fiji.util.gui.OverlayedImageCanvas.Overlay#paint(java.awt.Graphics,
	 * int, int, double)
	 */
	public void paint(Graphics g, int x, int y, double magnification) {
		if (null == this.imp)
			return;

		Graphics2D g2d = (Graphics2D) g;

		final AffineTransform originalTransform = g2d.getTransform();
		final AffineTransform at = new AffineTransform();
		at.scale(magnification, magnification);
		at.translate(-x, -y);
		at.concatenate(originalTransform);

		g2d.setTransform(at);

		final Composite originalComposite = g2d.getComposite();
		g2d.setComposite(composite);
		g2d.drawImage(imp.getBufferedImage(), null, null);

		g2d.setTransform(originalTransform);
		g2d.setComposite(originalComposite);
	}

	/**
	 * Set composite mode
	 * 
	 * @param composite
	 *            composite mode
	 */
	public void setComposite(Composite composite) {
		this.composite = composite;
	}

	/**
	 * Set image processor to be painted in the overlay
	 * 
	 * @param imp
	 *            input image
	 */
	public void setImage(ImageProcessor imp) {
		this.imp = imp;
	}

	/**
	 * 
	 * @return image processor to be painted in the overlay
	 */
	public ImageProcessor getImage() {
		return imp;
	}
}
