package VivinPaliath;

import java.util.Map;

/**
 * This class holds information that the visualizer uses to render the GUI. An instance is shared between the transducer and the visualizer. The transducer
 * updates values in this class and the visualizer renders the updated values.
 */
public class VisualizationContext {
    private CellStatus[][] cellStatus;
    private Map<Integer, Double> averageEfficiencies;
    private Nutrient targetNutrient;
    private Enzyme maxEfficientEnzyme;
    private double enzymeEfficiency;

    public VisualizationContext(CellStatus[][] cellStatus, Map<Integer, Double> averageEfficiencies, Nutrient targetNutrient, Enzyme maxEfficientEnzyme, double enzymeEfficiency) {
        this.cellStatus = cellStatus;
        this.averageEfficiencies = averageEfficiencies;
        this.targetNutrient = targetNutrient;
        this.maxEfficientEnzyme = maxEfficientEnzyme;
        this.enzymeEfficiency = enzymeEfficiency;
    }

    public CellStatus[][] getCellStatus() {
        return cellStatus;
    }

    public Map<Integer, Double> getAverageEfficiencies() {
        return averageEfficiencies;
    }

    public Nutrient getTargetNutrient() {
        return targetNutrient;
    }

    public Enzyme getMaxEfficientEnzyme() {
        return maxEfficientEnzyme;
    }

    public double getEnzymeEfficiency() {
        return enzymeEfficiency;
    }

    public void setCellStatus(CellStatus[][] cellStatus) {
        this.cellStatus = cellStatus;
    }

    public void setAverageEfficiencies(Map<Integer, Double> averageEfficiencies) {
        this.averageEfficiencies = averageEfficiencies;
    }

    public void setTargetNutrient(Nutrient targetNutrient) {
        this.targetNutrient = targetNutrient;
    }

    public void setMaxEfficientEnzyme(Enzyme maxEfficientEnzyme) {
        this.maxEfficientEnzyme = maxEfficientEnzyme;
    }

    public void setEnzymeEfficiency(double enzymeEfficiency) {
        this.enzymeEfficiency = enzymeEfficiency;
    }
}
