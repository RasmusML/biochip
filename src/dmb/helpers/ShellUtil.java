package dmb.helpers;

import java.io.IOException;

public class ShellUtil {
  
  /**
   * Executes a shell command and blocks till the command has been executed
   * 
   * @param command - shell command to execute
   * @return return-value of the shell command
   */
  
  public static int execute(String command) {
    return execute(command, true);
  }

  /**
   * Executes a shell command
   * 
   * @param command - shell command to execute
   * @param blocking - whether the function should stall till the command is done executing.
   * @return return-value of the shell command
   */
  
  public static int execute(String command, boolean blocking) {
    try {
      Process proc = Runtime.getRuntime().exec(command);
      if (blocking) return proc.waitFor();
      
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      return -1;
    }
    
    return 0;
  }
}
