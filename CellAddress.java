package VivinPaliath;

/**
 * Class that represents the address of a cell in the 2D grid.
 */
public class CellAddress {
    private int row;
    private int column;

    public CellAddress(int row, int column) {
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
