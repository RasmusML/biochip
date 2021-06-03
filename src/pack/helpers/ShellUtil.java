package pack.helpers;

import java.io.IOException;

public class ShellUtil {
  
  public static int execute(String command) {
    String shell = getShellRunCommand();
    return execute(shell, command, true);
  }

  public static int execute(String shellRunCommand, String command, boolean blocking) {
    try {
      String fullCommand = String.format("%s %s", shellRunCommand, command);
      Process proc = Runtime.getRuntime().exec(fullCommand);
      if (blocking) return proc.waitFor();
      
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      return -1;
    }
    
    return 0;
  }
  
  public static String getShellRunCommand() {
    String os = System.getProperty("os.name");
    if (os.startsWith("Windows")) return "cmd /c";
    throw new IllegalStateException("os unsupported! " + os);
  }
}
