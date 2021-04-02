package engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

public class Utils {

	public static BufferedImage loadImage(String path) {
		BufferedImage image = null;
		
		try {
			InputStream stream = Utils.class.getResourceAsStream(path);
			image = ImageIO.read(stream);
			
		} catch (IOException e) {
			System.out.printf("%s not valid\n", path);
		}
		
		return image;
	}
	

	public static List<String> readFile(String path) {
		List<String> lines = null;
		
		try {
			URL url = Utils.class.getResource(path);
			lines = Files.readAllLines(Paths.get(url.toURI()), Charset.defaultCharset());
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return lines;
	}
	
	public static byte[] readFileAsByte(String path) {
		byte[] content = null;
		
		try {
			URL url = Utils.class.getResource(path);
			content = Files.readAllBytes(Paths.get(url.toURI()));
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return content;
	}


	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
