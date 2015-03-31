package VivinPaliath;

import GenCol.entity;

/**
 * This class represents the query response sent by a transducer to a cell that requests information about vacant neighbors.
 */
public class QueryResponse extends entity {

    private CellAddress cellAddress = null;
    private Direction direction = Direction.NONE;

    public QueryResponse(CellAddress cellAddress, Direction direction) {
        this.cellAddress = cellAddress;
        this.direction = direction;
    }

    public CellAddress getCellAddress() {
        return cellAddress;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean emptyCellAvailable() {
        return direction != Direction.NONE;
    }
}
