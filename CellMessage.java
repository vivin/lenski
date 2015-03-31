package VivinPaliath;

import GenCol.entity;

/**
 * A utility class that lets us construct different kinds of messages we can send from a cell to the transducer or other cells
 */
public class CellMessage extends entity {

    public static class RestContext {

        private CellAddress cellAddress = null;
        private int lifespan = 0;
        private int maximumLifespan = 0;
        private double freeEnergy = 0;
        private double maximumFreeEnergy = 0;

        private RestContext(CellAddress cellAddress, int maximumLifespan, int lifespan, double maximumFreeEnergy, double freeEnergy) {
            this.cellAddress = cellAddress;
            this.maximumLifespan = maximumLifespan;
            this.lifespan = lifespan;
            this.maximumFreeEnergy = freeEnergy;
            this.freeEnergy = freeEnergy;
        }

        public CellAddress getCellAddress() {
            return cellAddress;
        }

        public int getLifespan() {
            return lifespan;
        }

        public int getMaximumLifespan() {
            return maximumLifespan;
        }

        public double getFreeEnergy() {
            return freeEnergy;
        }

        public double getMaximumFreeEnergy() {
            return maximumFreeEnergy;
        }
    }

    public static class FeedContext {

        private CellAddress cellAddress = null;
        private MetabolysisResult metabolysisResult = null;
        private Nutrient parentCellNutrient = null;

        private FeedContext(CellAddress cellAddress, MetabolysisResult metabolysisResult, Nutrient parentCellNutrient) {
            this.cellAddress = cellAddress;
            this.metabolysisResult = metabolysisResult;
            this.parentCellNutrient = parentCellNutrient;
        }

        public CellAddress getCellAddress() {
            return cellAddress;
        }

        public MetabolysisResult getMetabolysisResult() {
            return metabolysisResult;
        }

        public Nutrient getParentCellNutrient() {
            return parentCellNutrient;
        }
    }

    public static class QueryContext {

        private CellAddress cellAddress = null;

        private QueryContext(CellAddress cellAddress) {
            this.cellAddress = cellAddress;
        }

        public CellAddress getCellAddress() {
            return cellAddress;
        }
    }

    private QueryContext queryContext = null;
    private RestContext restContext = null;
    private FeedContext feedContext = null;

    private Type type;

    private enum Type {
        REST_STATUS, FEED_STATUS, QUERY
    }

    private CellMessage() {};

    public static CellMessage createQueryMessage(CellAddress cellAddress) {
        CellMessage cellMessage = new CellMessage();
        cellMessage.type = Type.QUERY;
        cellMessage.queryContext = new QueryContext(cellAddress);

        return cellMessage;
    }

    public static CellMessage createFeedMessage(CellAddress cellAddress, MetabolysisResult metabolysisResult, Nutrient parentCellNutrient) {
        CellMessage cellMessage = new CellMessage();
        cellMessage.type = Type.FEED_STATUS;
        cellMessage.feedContext = new FeedContext(cellAddress, metabolysisResult, parentCellNutrient);

        return cellMessage;
    }

    public static CellMessage createRestMessage(CellAddress cellAddress, int maximumLifespan, int lifespan, double maximumFreeEnergy, double freeEnergy) {
        CellMessage cellMessage = new CellMessage();
        cellMessage.type = Type.REST_STATUS;
        cellMessage.restContext = new RestContext(cellAddress, maximumLifespan, lifespan, maximumFreeEnergy, freeEnergy);

        return cellMessage;
    }

    public boolean isQueryMessage() {
        return this.type == Type.QUERY;
    }

    public boolean isFeedMessage() {
        return this.type == Type.FEED_STATUS;
    }

    public boolean isRestMessage() {
        return this.type == Type.REST_STATUS;
    }

    public QueryContext getQueryContext() {
        if(isQueryMessage()) {
            return queryContext;
        } else {
            throw new IllegalStateException("Message is not a query message");
        }
    }

    public RestContext getRestContext() {
        if(isRestMessage()) {
            return restContext;
        } else {
            throw new IllegalStateException("Message is not a rest message");
        }
    }

    public FeedContext getFeedContext() {
        if(isFeedMessage()) {
            return feedContext;
        } else {
            throw new IllegalStateException("Message is not a feed message");
        }
    }
}
