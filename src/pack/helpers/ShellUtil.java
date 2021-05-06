package pack.helpers;

import java.io.IOException;

public class ShellUtil {
  
  public static int execute(String command) {
    String shell = getShellExecutable();
    return execute(shell, command, true);
  }

  public static int execute(String shell, String command, boolean blocking) {
    try {
      String fullCommand = String.format("%s %s", shell, command);
      Process proc = Runtime.getRuntime().exec(fullCommand);
      if (blocking) return proc.waitFor();
      
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
    
    return 0;
  }
  
  public static String getShellExecutable() {
    String os = System.getProperty("os.name");
    if (os.startsWith("Windows")) return "cmd /c";
    throw new IllegalStateException("os unsupported!" + os);
  }
}
