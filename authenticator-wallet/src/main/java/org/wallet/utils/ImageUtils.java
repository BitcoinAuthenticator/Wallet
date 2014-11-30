package org.wallet.utils;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;

public class ImageUtils {
	public static javafx.scene.image.Image javaFXImageFromBytes(byte[] data) throws IOException {
		return new Image(new ByteArrayInputStream(data));
	}
}
