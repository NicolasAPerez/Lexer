package edu.ufl.cise.plc.runtime;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ColorImageExtra {
    public static BufferedImage setAllPixels(BufferedImage image, ColorTuple val) {

		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++) {
				image.setRGB(x, y,  val.pack());
			}
			return image;
	}

	public static BufferedImage setAllPixels(int w, int h, ColorTuple val) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++) {
				image.setRGB(x, y,  val.pack());
			}
		return image;
	}

	public static BufferedImage setAllPixels(int w, int h, int val) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < image.getWidth(); x++)
			for (int y = 0; y < image.getHeight(); y++) {
				image.setRGB(x, y,  val);
			}
		return image;
	}

	public static boolean binaryImageImageOp(ImageOps.BoolOP op , BufferedImage image0, BufferedImage image1) {
		int[] pixels0 = image0.getRGB(0,0,image0.getWidth(), image0.getHeight(), null,0,image0.getWidth());
		int[] pixels1 = image1.getRGB(0,0,image1.getWidth(), image1.getHeight(), null,0,image1.getWidth());
		return (op == ImageOps.BoolOP.EQUALS)? Arrays.equals(pixels0, pixels1) : !Arrays.equals(pixels0, pixels1);
	}

}
