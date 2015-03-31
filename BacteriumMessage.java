package VivinPaliath;

import GenCol.entity;

/**
 * This class maintains information about a child agent that is being sent to another cell.
 */
public class BacteriumMessage extends entity {

    //Address of the destination cell
    private CellAddress cellAddress;

    //Genome of the child agent
    private String genome;

    //Nutrient in the parent cell - we use this to accurately calculate efficiencies for some cases. For example, we don't care about
    //cells that have crossed over from the non-novel region into the novel region with an enzymatic efficiency of 0. But we do care
    //about agents that move within the novel region
    private Nutrient parentCellNutrient;

    public BacteriumMessage(CellAddress cellAddress, String genome, Nutrient nutrient) {
        this.cellAddress = cellAddress;
        this.genome = genome;
        this.parentCellNutrient = nutrient;
    }

    public CellAddress getCellAddress() {
        return cellAddress;
    }

    public String getGenome() {
        return genome;
    }

    public Nutrient getParentCellNutrient() {
        return parentCellNutrient;
    }
}
