package VivinPaliath;

import model.modeling.content;
import model.modeling.message;
import view.modeling.ViewableAtomic;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class that represents the transducer model
 */
public class Transducer extends ViewableAtomic {

    private double clock;

    //References to the visualizer.
    private JFrame canvas;
    private Visualization visualization;

    //An object that is shared between the transducer and the visualizer. This visualizer uses this object to render the GUI
    private VisualizationContext visualizationContext;

    //Sampling interval, number of samples, and data related to average efficiencies, and efficient enzymes
    private int maximumSamples = 50;
    private double samplingInterval = 1;
    private int lastInterval = -1;
    private final Map<Integer, Double> averageEfficiencies = Collections.synchronizedMap(new LinkedHashMap<Integer, Double>());
    private Map<Integer, Integer> intervalSampleCount = new HashMap<>();
    private double maxEfficiency = 0;
    private boolean firstFunctionalEnzymeFound = false;
    private double peakAverageEfficiency = 0;
    private double peakInterval = -1;

    //Queue for incoming messages
    private Queue<CellMessage> cellMessages = new ArrayDeque<>();

    //List of deltas
    private List<Delta> deltas;

    //Map that maps deltas to directions
    private Map<Delta, Direction> deltaToDirection;

    //Two-dimensional array of CellStatus instances representing the state of the grid as a whole
    private CellStatus[][] cellStatus;
    private int rows = -1;
    private int columns = -1;

    //The nutrient whose metabolysis we are interested in measuring
    private Nutrient targetNutrient;

    private static final double processingTime = 0;

    private QueryResponse queryResponse = null;

    private static final Random random = new Random(Double.doubleToLongBits(Math.random()));

    //Ports for this model
    public static enum Port {

        IN("in"),
        QUERY_RESPONSE("query_response");

        private String portName;

        private Port(String portName) {
            this.portName = portName;
        }

        public String portName() {
            return this.portName;
        }
    }

    //Phases for this model
    public static enum Phase {

        PASSIVE("passive"),
        PROCESSING("processing"),
        SEND_QUERY_RESPONSE("send_query_response");

        private String phaseName;

        private Phase(String phaseName) {
            this.phaseName = phaseName;
        }

        public String phaseName() {
            return this.phaseName;
        }
    }

    public Transducer() {
        this(new CellStatus[10][10], null, 100, .0001);
    }

    public Transducer(CellStatus[][] cellStatus, Nutrient targetNutrient, int maximumSamples, double samplingInterval) {
        super("Transducer");

        this.clock = 0;
        this.canvas = new JFrame("Evolution");

        addInport(Port.IN.portName);
        addOutport(Port.QUERY_RESPONSE.portName);

        this.cellStatus = cellStatus;
        this.rows = this.cellStatus.length;
        this.columns = this.cellStatus[0].length;
        this.targetNutrient = targetNutrient;
        this.maximumSamples = maximumSamples;
        this.samplingInterval = samplingInterval;

        //Initialize a VisualizationContext instance for the visualizer.
        this.visualizationContext = new VisualizationContext(this.cellStatus, averageEfficiencies, this.targetNutrient, null, 0);
        this.visualization = new Visualization(maximumSamples, visualizationContext);
        this.canvas.add(visualization);
        this.canvas.setSize(new Dimension(1920, 1080));
        this.canvas.setVisible(true);

        this.deltas = new ArrayList<>();
        this.deltaToDirection = new HashMap<>();

        Delta northWest = new Delta(-1, -1);
        Delta north = new Delta(-1, 0);
        Delta northEast = new Delta(-1, 1);
        Delta west = new Delta(0, -1);
        Delta east = new Delta(0, 1);
        Delta southWest = new Delta(1, -1);
        Delta south = new Delta(1, 0);
        Delta southEast = new Delta(1, 1);

        deltas.add(northWest);
        deltas.add(north);
        deltas.add(northEast);
        deltas.add(west);
        deltas.add(east);
        deltas.add(southWest);
        deltas.add(south);
        deltas.add(southEast);

        deltaToDirection.put(northWest, Direction.NW);
        deltaToDirection.put(north, Direction.N);
        deltaToDirection.put(northEast, Direction.NE);
        deltaToDirection.put(west, Direction.W);
        deltaToDirection.put(east, Direction.E);
        deltaToDirection.put(southWest, Direction.SW);
        deltaToDirection.put(south, Direction.S);
        deltaToDirection.put(southEast, Direction.SE);

        phase = Phase.PASSIVE.phaseName;
        sigma = INFINITY;
    }

    @Override
    public void deltint() {
        clock = clock + sigma;

        if(phaseIs(Phase.PROCESSING.phaseName)) { //If we are in the processing phase, it means we have messages to process

            CellMessage cellMessage = cellMessages.remove(); //remove the first element in the queue
            if(cellMessage.isQueryMessage()) {
                //If this is a query message, figure out if we have any vacant neighbors
                CellAddress cellAddress = cellMessage.getQueryContext().getCellAddress();
                Delta delta = getEmptyNeighborDelta(cellAddress.getRow(), cellAddress.getColumn());

                //If we don't have any vacant neighbors we send a direction of NONE. Otherwise we send the direction of the vacant neighbor
                if(delta == null) {
                    queryResponse = new QueryResponse(cellAddress, Direction.NONE);
                } else {
                    queryResponse = new QueryResponse(cellAddress, deltaToDirection.get(delta));
                }

                //Reserve the location in the grid by marking it as occupied
                if(delta != null) {
                    markOccupiedCell(wrap(cellAddress.getRow() + delta.getDr(), rows), wrap(cellAddress.getColumn() + delta.getDc(), columns));
                }

                //Transition to SEND_QUERY_RESPONSE to tsend the response
                holdIn(Phase.SEND_QUERY_RESPONSE.phaseName, processingTime);
            } else if(cellMessage.isRestMessage()) {
                //If this is a rest message then we need to update the grid based on the status of the agent
                CellMessage.RestContext restContext = cellMessage.getRestContext();
                CellAddress cellAddress = restContext.getCellAddress();

                if(restContext.getLifespan() == 0 || restContext.getFreeEnergy() <= 0) {
                    markDeadCell(cellAddress.getRow(), cellAddress.getColumn()); //if the agent is dead mark its location as vacant
                } else {
                    paintLiveCell(restContext); //paint the cell based on its current lifespan in relation to its maximum lifespan.
                }

                //If we have more messages to process, stay in the PROCESSING phase. Otherwise transition to the PASSIVE phase.
                if(!cellMessages.isEmpty()) {
                    holdIn(Phase.PROCESSING.phaseName, processingTime);
                } else {
                    holdIn(Phase.PASSIVE.phaseName, INFINITY);
                }
            } else if(cellMessage.isFeedMessage()) {
                //If this is a feed message we need to record information about efficiencies, etc.
                CellMessage.FeedContext feedContext = cellMessage.getFeedContext();
                int interval = (int) Math.round(clock / samplingInterval);
                recordAverageEfficiency(interval, feedContext); //record the average efficiency

                //If we have more messages to process, stay in the PROCESSING phase. Otherwise transition to the PASSIVE phase.
                if(!cellMessages.isEmpty()) {
                    holdIn(Phase.PROCESSING.phaseName, processingTime);
                } else {
                    holdIn(Phase.PASSIVE.phaseName, INFINITY);
                }
            }
        } else if(phaseIs(Phase.SEND_QUERY_RESPONSE.phaseName)) {

            //If we are in the SEND_QUERY_RESPONSE phase, we just sent a response and we still need to check on the status of the
            //queue to see if we have more messages to process. If so, transition to the PROCESSING phase. Otherwise transition to
            //the PASSIVE phase.
            if(!cellMessages.isEmpty()) {
                holdIn(Phase.PROCESSING.phaseName, processingTime);
            } else {
                holdIn(Phase.PASSIVE.phaseName, INFINITY);
            }
        }
    }

    @Override
    public message out() {
        message m = new message();

        //If we are in the SEND_QUERY_RESPONSE phase, send a response on the QUERY_RESPONSE port
        if(phaseIs(Phase.SEND_QUERY_RESPONSE.phaseName)) {
            content c = makeContent(Port.QUERY_RESPONSE.portName, queryResponse);
            m.add(c);

            queryResponse = null;
        }

        return m;
    }

    @Override
    public void deltext(double e, message x) {
        clock = clock + e;
        Continue(e);

        if(phaseIs(Phase.PASSIVE.phaseName)) {
            //If the phase is PASSIVE and we have a message on the input port, enqueue it and transition to the PROCESSING phase
            for(int i = 0; i < x.getLength(); i++) {
                if(messageOnPort(x, Port.IN.portName, i)) {
                    CellMessage cellMessage = (CellMessage) x.getValOnPort(Port.IN.portName, i);

                    cellMessages.add(cellMessage);
                    holdIn(Phase.PROCESSING.phaseName, processingTime);
                }
            }
        } else if(phaseIs(Phase.PROCESSING.phaseName) || phaseIs(Phase.SEND_QUERY_RESPONSE.phaseName)) {
            //If the phase is PROCESSING and we have a message on the input port, simply enqueue it.
            for(int i = 0; i < x.getLength(); i++) {
                if(messageOnPort(x, Port.IN.portName, i)) {
                    CellMessage cellMessage = (CellMessage) x.getValOnPort(Port.IN.portName, i);

                    cellMessages.add(cellMessage);
                }
            }
        }
    }

    @Override
    public void deltcon(double e, message x) {
        deltint();
        deltext(0, x);
    }

    //Helper function that choses a neighbor delta at random from available vacant-neighbors.
    private Delta getEmptyNeighborDelta(int row, int column) {
        List<Delta> availableNeighbors = new ArrayList<>();

        for(Delta delta : deltas) {
            if(!isCellOccupied(wrap(row + delta.getDr(), rows), wrap(column + delta.getDc(), columns))) {
                availableNeighbors.add(delta);
            }
        }

        Delta neighborDelta = null;
        if(availableNeighbors.size() > 0) {
            neighborDelta = availableNeighbors.get(random.nextInt(availableNeighbors.size()));
        }

        return neighborDelta;
    }

    private void markDeadCell(int row, int column) {
        cellStatus[row][column].setOccupied(false);
        visualization.repaint();
    }

    private void markOccupiedCell(int row, int column) {
        cellStatus[row][column].setOccupied(true);
        visualization.repaint();
    }

    private void paintLiveCell(CellMessage.RestContext restContext) {
        double ratio = ((double) restContext.getLifespan() / (double) restContext.getMaximumLifespan());
        cellStatus[restContext.getCellAddress().getRow()][restContext.getCellAddress().getColumn()].setRatio(ratio);
        visualization.repaint();
    }

    //Records average efficiencies
    private void recordAverageEfficiency(int interval, CellMessage.FeedContext feedContext) {
        MetabolysisResult metabolysisResult = feedContext.getMetabolysisResult();

        //We only need to record information if we have a metabolysis result, if the nutrient that was metabolized is the one we are interested in, or if the agent
        //came from a parent who was also in a cell with the target nutrient
        if(metabolysisResult != null &&
                metabolysisResult.getNutrient().getBitPattern().equals(targetNutrient.getBitPattern()) &&
                (metabolysisResult.getEfficiency() > 0 || feedContext.getParentCellNutrient().getBitPattern().equals(targetNutrient.getBitPattern()))) {
            if(metabolysisResult.getEfficiency() > maxEfficiency) {
                maxEfficiency = metabolysisResult.getEfficiency();
                visualizationContext.setMaxEfficientEnzyme(metabolysisResult.getEnzyme());
                visualizationContext.setEnzymeEfficiency(maxEfficiency);
            }

            //This map is shared between the transducer and visualizer and so needs to have synchronized access
            synchronized (averageEfficiencies) {
                if(averageEfficiencies.size() == 0) {
                    averageEfficiencies.put(interval, metabolysisResult.getEfficiency());
                    intervalSampleCount.put(interval, 1);
                } else {

                    if(interval > lastInterval && averageEfficiencies.size() == maximumSamples) {
                        if(averageEfficiencies.get(lastInterval) > peakAverageEfficiency) {
                            peakAverageEfficiency = averageEfficiencies.get(lastInterval);
                            peakInterval = lastInterval;
                            System.out.println("Peak average efficiency (thus far) of " + peakAverageEfficiency + " at time " + (peakInterval * samplingInterval));
                        }

                        System.out.println("###Average efficiency for interval " + lastInterval + ": " + averageEfficiencies.get(lastInterval));

                        averageEfficiencies.remove(averageEfficiencies.entrySet().iterator().next().getKey());
                        averageEfficiencies.put(interval, metabolysisResult.getEfficiency());
                        intervalSampleCount.put(interval, 1);

                    } else if(interval > lastInterval && averageEfficiencies.size() < maximumSamples) {
                        if(averageEfficiencies.get(lastInterval) > peakAverageEfficiency) {
                            peakAverageEfficiency = averageEfficiencies.get(lastInterval);
                            peakInterval = lastInterval;
                            System.out.println("Peak average efficiency (thus far) of " + peakAverageEfficiency + " at time " + (peakInterval * samplingInterval));
                        }

                        System.out.println("###Average efficiency for interval " + lastInterval + ": " + averageEfficiencies.get(lastInterval));

                        averageEfficiencies.put(interval, metabolysisResult.getEfficiency());
                        intervalSampleCount.put(interval, 1);
                    } else if(interval == lastInterval) {
                        double currentAverage = averageEfficiencies.get(interval);
                        int sampleCount = intervalSampleCount.get(interval);
                        double newAverage = (((double) sampleCount * currentAverage) + metabolysisResult.getEfficiency()) / ((double) (sampleCount + 1));

                        averageEfficiencies.put(interval, newAverage);
                        intervalSampleCount.put(interval, sampleCount + 1);
                    }
                }

                lastInterval = interval;

                if(!firstFunctionalEnzymeFound && metabolysisResult.getEfficiency() > 0) {
                    System.out.println("First functional enzyme found at clock " + clock + ". Enzyme is " + metabolysisResult.getEnzyme().getBitPattern() + " and efficiency was " + metabolysisResult.getEfficiency());
                    firstFunctionalEnzymeFound = true;
                }
            }
        }
    }

    private boolean isCellOccupied(int row, int column) {
        return cellStatus[row][column].isOccupied();
    }

    private int wrap(int index, int size) {
        if(index < 0) {
            return (size + index) % size;
        } else {
            return (index % size);
        }
    }
}
