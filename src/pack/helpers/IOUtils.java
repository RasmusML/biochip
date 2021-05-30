package pack.helpers;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

public class IOUtils {

  public static BufferedImage loadImage(String path) {
    BufferedImage image = null;

    try {
      InputStream stream = IOUtils.class.getResourceAsStream(path);
      image = ImageIO.read(stream);

    } catch (IOException e) {
      System.out.printf("\"%s\" could not be found!\n", path);
    }

    return image;
  }

  public static List<String> readFileAsString(String path) {
    List<String> lines = null;

    try {
      URL url = IOUtils.class.getResource(path);
      lines = Files.readAllLines(Paths.get(url.toURI()), Charset.defaultCharset());
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }
    return lines;
  }

  public static byte[] readFileAsBytes(String path) {
    byte[] content = null;

    try {
      URL url = IOUtils.class.getResource(path);
      content = Files.readAllBytes(Paths.get(url.toURI()));
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }
    return content;
  }

  public static void writeStringToFile(String content, String path) {
    try {
      Files.write(Paths.get(path), content.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
