package dmb.platform;

public class Platform {
  public int rows;
  public int columns;

  public boolean[][] array;

  public boolean highVoltageOn;

  public Platform() {
    rows = 20;
    columns = 32;
    array = new boolean[rows][columns];
    highVoltageOn = false;
  }

  public void clearElectrodes() {
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        array[row][column] = false;
      }
    }
  }

  public boolean isElectrodeOnByXY(int x, int y) {
    int col = x;
    int row = rows - 1 - y;

    return array[row][col];
  }

  public void flipElectrodeStateByXY(int x, int y) {
    int col = x;
    int row = rows - 1 - y;

    array[row][col] = !array[row][col];
  }
}