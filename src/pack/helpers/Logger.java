package pack.helpers;

public class Logger {
  
  public static LogMode mode = LogMode.Verbose;
  
  public static void log(String format, Object... args) {
    if (mode == LogMode.Silent) return;
    System.out.printf(format, args);
  }
}

