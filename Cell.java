package VivinPaliath;

import model.modeling.content;
import model.modeling.message;
import view.modeling.ViewableAtomic;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the cell model and the bacterial agent's lifecycle.
 */
public class Cell extends ViewableAtomic {

    //The row and colum that identifies this cell's location in the grid.
    private int row = -1;
    private int column = -1;

    //Nutrient in the cell
    private Nutrient nutrient = null;

    //Nutrient of the cell that a child agent came from
    private Nutrient parentCellNutrient = null;

    //The agent in this cell
    private Bacterium bacterium = null;

    //The genome of the child agent that we will send to a vacant neighbor
    private String childGenome = null;

    //The result of metabolysis that we'll send to the transducer
    private MetabolysisResult metabolysisResult = null;

    //The response from the transducer regarding a query about spaces available.
    private QueryResponse queryResponse = null;

    //The time to transition between states
    private double stateTransitionTime = 1;

    //Maps direction to the appropriate output port. This is how we can identify neighboring cells based on the direction from the current cell.
    private Map<Direction, Port> directionToPort = new HashMap<>();

    //An enum that represents the ports on this model
    public static enum Port {

        OUTPUT_N("output_N"),
        OUTPUT_NW("output_NW"),
        OUTPUT_NE("output_NE"),
        OUTPUT_S("output_S"),
        OUTPUT_SW("output_SW"),
        OUTPUT_SE("output_SE"),
        OUTPUT_E("output_E"),
        OUTPUT_W("output_W"),
        QUERY("query"),
        CELL_STATUS("cell_status"),

        START("start"),
        INPUT_BACTERIUM("input_bacterium"),
        QUERY_RESPONSE("query_response");

        private String portName;

        private Port(String portName) {
            this.portName = portName;
        }

        public String portName() {
            return this.portName;
        }
    }

    //An enum that represents the phases of this model.
    public static enum Phase {

        FEED("feed"),
        SEND_FEED_MESSAGE("send_feed_message"),
        REST("rest"),
        SEND_REST_MESSAGE("send_rest_message"),
        REPRODUCE("reproduce"),
        SEND_CHILD("send_child"),
        DEAD("dead"),
        QUERY("query"),
        WAIT("wait");

        private String phaseName;

        private Phase(String phaseName) {
            this.phaseName = phaseName;
        }

        public String phaseName() {
            return this.phaseName;
        }
    }

    public Cell() {
        this(0, 0, new Nutrient("0111101010001111"), new Bacterium(
                "0000011100000011101100010000110010110001000011000110011110011100"
        ));
    }

    public Cell(int row, int column, Nutrient nutrient, Bacterium bacterium) {
        super("Cell(" + row + "," + column + ")");

        this.row = row;
        this.column = column;

        directionToPort.put(Direction.N, Port.OUTPUT_N);
        directionToPort.put(Direction.NW, Port.OUTPUT_NW);
        directionToPort.put(Direction.NE, Port.OUTPUT_NE);
        directionToPort.put(Direction.S, Port.OUTPUT_S);
        directionToPort.put(Direction.SW, Port.OUTPUT_SW);
        directionToPort.put(Direction.SE, Port.OUTPUT_SE);
        directionToPort.put(Direction.E, Port.OUTPUT_E);
        directionToPort.put(Direction.W, Port.OUTPUT_W);

        addOutport(Port.OUTPUT_N.portName);
        addOutport(Port.OUTPUT_NW.portName);
        addOutport(Port.OUTPUT_NE.portName);
        addOutport(Port.OUTPUT_S.portName);
        addOutport(Port.OUTPUT_SW.portName);
        addOutport(Port.OUTPUT_SE.portName);
        addOutport(Port.OUTPUT_E.portName);
        addOutport(Port.OUTPUT_W.portName);
        addOutport(Port.QUERY.portName);
        addOutport(Port.CELL_STATUS.portName);

        addInport(Port.START.portName);
        addInport(Port.INPUT_BACTERIUM.portName);
        addInport(Port.QUERY_RESPONSE.portName);

        this.nutrient = nutrient;
        this.parentCellNutrient = nutrient;
        this.bacterium = bacterium;

        //We initially start in the DEAD phase even if we have a resident agent. We will not start until we get a START message.
        phase = Phase.DEAD.phaseName;
        sigma = INFINITY;
    }

    @Override
    public void deltint() {

        if(phaseIs(Phase.REST.phaseName)) {  //If we are in the REST phase, invoke the rest behavior and then transition to the SEND_REST_MESSAGE phase immediately
            bacterium.rest();
            holdIn(Phase.SEND_REST_MESSAGE.phaseName, 0);

        } else if(phaseIs(Phase.SEND_REST_MESSAGE.phaseName)) { //If we are in the SEND_REST_MESSAGE phase it means we just sent a mesage. Check to see if the agent is still alive.
            if(bacterium.getLifespan() == 0 || bacterium.getFreeEnergy() <= 0) {
                bacterium = null;
                holdIn(Phase.DEAD.phaseName, INFINITY); //transition into the DEAD (passive) phase since agent is dead
            } else if(bacterium.getFreeEnergy() >= bacterium.getReproductionThreshold()) {
                holdIn(Phase.QUERY.phaseName, 0); //transition to the QUERY phase to send a query to the transducer about vacant neighbors
            } else {
                holdIn(Phase.FEED.phaseName, stateTransitionTime); //transition to the FEED phase.
            }

        } else if(phaseIs(Phase.FEED.phaseName)) { //If we are in the FEED phase, invoke feeding behavior and transition to the SEND_FEED_MESSAGE phase immediately
            metabolysisResult = bacterium.feed(nutrient);
            holdIn(Phase.SEND_FEED_MESSAGE.phaseName, 0);

        } else if(phaseIs(Phase.SEND_FEED_MESSAGE.phaseName)) { //We just sent a message regarding feeding behavior, so transition to the REST phase.
            holdIn(Phase.REST.phaseName, stateTransitionTime);

        } else if(phaseIs(Phase.QUERY.phaseName)) { //If we are in the QUERY phase, we need to wait for a response from the transducer, so transition to the passive WAIT phase.
            holdIn(Phase.WAIT.phaseName, INFINITY);

        } else if(phaseIs(Phase.REPRODUCE.phaseName)) { //If we are in the REPRODUCE phase, invoke the reproduce behavior and transition to the SEND_CHILD
            childGenome = bacterium.reproduce();
            holdIn(Phase.SEND_CHILD.phaseName, 0);

        } else if(phaseIs(Phase.SEND_CHILD.phaseName)) { //We just sent a child to a vacant neighbor so transition to the REST phase.
            holdIn(Phase.REST.phaseName, stateTransitionTime);

        }
    }

    @Override
    public void deltext(double e, message x) {
        Continue(e);

        if(phaseIs(Phase.DEAD.phaseName)) { //If we are in the DEAD (passive) phase
            for(int i = 0; i < x.getLength(); i++) {
                if(messageOnPort(x, Port.START.portName, i) && bacterium != null) {
                    holdIn(Phase.REST.phaseName, stateTransitionTime); //If we got a START message, start the lifecycle of the agent by transitioning to the REST phase
                } else if(messageOnPort(x, Port.INPUT_BACTERIUM.portName, i)) { //If we got a bacterium message, we got a child agent from another cell. So let's instantiate and assign it to this cell
                    BacteriumMessage bacteriumMessage = (BacteriumMessage) x.getValOnPort(Port.INPUT_BACTERIUM.portName, i);
                    bacterium = new Bacterium(bacteriumMessage.getGenome());
                    parentCellNutrient = bacteriumMessage.getParentCellNutrient();

                    holdIn(Phase.REST.phaseName, 0);
                }
            }
        } else if(phaseIs(Phase.WAIT.phaseName)) { //If we are in the WAIT (passive) phase
            for(int i = 0; i < x.getLength(); i++) {
                if(messageOnPort(x, Port.QUERY_RESPONSE.portName, i)) { //If we got a response from the transducer, let's process it.
                    queryResponse = (QueryResponse) x.getValOnPort(Port.QUERY_RESPONSE.portName, i);
                    CellAddress queryResponseCellAddress = queryResponse.getCellAddress();

                    //If no room is available, transition to the REST phase. Otherwise transition to the REPRODUCE phase.
                    if(queryResponseCellAddress.getRow() == row && queryResponseCellAddress.getColumn() == column) {
                        if(queryResponse.emptyCellAvailable()) {
                            holdIn(Phase.REPRODUCE.phaseName, stateTransitionTime);
                        } else {
                            holdIn(Phase.REST.phaseName, stateTransitionTime);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void deltcon(double e, message x) {
        deltint();
        deltext(0, x);
    }

    @Override
    public message out() {
        message m = new message();

        if(phaseIs(Phase.QUERY.phaseName)) { //Send a query to the transducer if we are in the QUERY phase
            content c = makeContent(Port.QUERY.portName, CellMessage.createQueryMessage(new CellAddress(row, column)));
            m.add(c);
        } else if(phaseIs(Phase.SEND_CHILD.phaseName)) { //Send a bacterium-message to a vacant neighbor if we are in the SEND_CHILD phase
            Port port = directionToPort.get(queryResponse.getDirection());
            content c = makeContent(port.portName, new BacteriumMessage(new CellAddress(row, column), childGenome, nutrient));
            m.add(c);
        } else if(phaseIs(Phase.SEND_REST_MESSAGE.phaseName)) { //Send a rest-message to the transducer if we are in the SEND_REST_MESSAGE phase
            content c = makeContent(Port.CELL_STATUS.portName, CellMessage.createRestMessage(new CellAddress(row, column), bacterium.getMaximumLifespan(), bacterium.getLifespan(), bacterium.getMaximumFreeEnergy(), bacterium.getFreeEnergy()));
            m.add(c);
        } else if(phaseIs(Phase.SEND_FEED_MESSAGE.phaseName)) { //Send a feed-message to the transducer if we are in the SEND_FEED_MESSAGE phase
            content c = makeContent(Port.CELL_STATUS.portName, CellMessage.createFeedMessage(new CellAddress(row, column), metabolysisResult, parentCellNutrient));
            m.add(c);
        }

        return m;
    }

    public CellAddress getAddress() {
        return new CellAddress(row, column);
    }

    public Nutrient getNutrient() {
        return this.nutrient;
    }

    public Bacterium getBacterium() {
        return this.bacterium;
    }

    public MetabolysisResult metabolysisResult() {
        return this.metabolysisResult;
    }
}
