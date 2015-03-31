package VivinPaliath;

/**
 * This class represents the result of the metabolysis of a nutrient by an enzyme.
 */
public class MetabolysisResult {

    private Nutrient nutrient;
    private Enzyme enzyme;
    private double efficiency;

    public MetabolysisResult(Nutrient nutrient, Enzyme enzyme, double efficiency) {
        this.nutrient = nutrient;
        this.enzyme = enzyme;
        this.efficiency = efficiency;
    }

    public Nutrient getNutrient() {
        return nutrient;
    }

    public Enzyme getEnzyme() {
        return enzyme;
    }

    public double getEfficiency() {
        return efficiency;
    }
}
