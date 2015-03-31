package VivinPaliath;

/**
 * Class that holds information about the status of a single cell in the 2D grid.
 */
public class CellStatus {

    private boolean occupied;
    private Nutrient nutrient;
    private double ratio = 1;

    public CellStatus(boolean occupied, Nutrient nutrient) {
        this.occupied = occupied;
        this.nutrient = nutrient;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public Nutrient getNutrient() {
        return nutrient;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }
}
