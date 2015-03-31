package VivinPaliath;

import GenCol.entity;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This is the coupled model that represents the entire experiment.
 */
public class Evolution extends ViewableDigraph {

    //Random number generator
    private final Random random = new Random(Double.doubleToLongBits(Math.random()));

    //Maps direction to its appropriate delta.
    private static final Map<Direction, Delta> deltaToDirection = new HashMap<>();

    //Maps direction to the appropriate output port on a cell
    private static final Map<Direction, Cell.Port> directionToCellPort = new HashMap<>();

    //Static block that initializes the above maps.
    static {
        deltaToDirection.put(Direction.NW, new Delta(-1, -1));
        deltaToDirection.put(Direction.N, new Delta(-1, 0));
        deltaToDirection.put(Direction.NE, new Delta(-1, 1));
        deltaToDirection.put(Direction.W, new Delta(0, -1));
        deltaToDirection.put(Direction.E, new Delta(0, 1));
        deltaToDirection.put(Direction.SW, new Delta(1, -1));
        deltaToDirection.put(Direction.S, new Delta(1, 0));
        deltaToDirection.put(Direction.SE, new Delta(1, 1));

        directionToCellPort.put(Direction.NW, Cell.Port.OUTPUT_NW);
        directionToCellPort.put(Direction.N, Cell.Port.OUTPUT_N);
        directionToCellPort.put(Direction.NE, Cell.Port.OUTPUT_NE);
        directionToCellPort.put(Direction.W, Cell.Port.OUTPUT_W);
        directionToCellPort.put(Direction.E, Cell.Port.OUTPUT_E);
        directionToCellPort.put(Direction.SW, Cell.Port.OUTPUT_SW);
        directionToCellPort.put(Direction.S, Cell.Port.OUTPUT_S);
        directionToCellPort.put(Direction.SE, Cell.Port.OUTPUT_SE);
    }

    //Enum that represents the ports on this model
    public static enum Port {

        START("start");

        private String portName;

        private Port(String portName) {
            this.portName = portName;
        }

        public String portName() {
            return this.portName;
        }
    }

    public Evolution() {
        super("Evolution");

        //Set the rows, columns, and number of bacteria.
        int rows = 30;
        int columns = 30;
        int numBacteria = 50;

        addInport(Port.START.portName);
        addTestInput(Port.START.portName, new entity("start"));

        //Define the nutrients used
        Nutrient first = new Nutrient("1100 0110 1100 0111");
        Nutrient second = new Nutrient("0011 1001 0011 1000");

        List<Nutrient> nutrients = new ArrayList<>();
        nutrients.add(first);
        nutrients.add(second);

        //Define the properties of all, initial bacterial agents.
        int lifespan = 20;
        int freeEnergy = 20000;
        int reproductionThreshold = 50000;
        int metabolicEnergy = 255;
        List<Enzyme> enzymes = Collections.singletonList(new Enzyme("0011 1001 0011 1000"));

        //Initialize the grid. Here we also take care of the seeding the grid with bacteria at random locations
        Cell[][] cells = new Cell[rows][columns];
        CellStatus[][] cellStatus = new CellStatus[rows][columns];
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < columns; j++) {
                Nutrient nutrient = (j >= (columns / (nutrients.size()))) ? second : first;

                Bacterium bacterium = null;
                if(numBacteria > 0 && nutrient == first) {
                    bacterium = random.nextInt(4) == 3 ? new Bacterium(lifespan, freeEnergy, reproductionThreshold, metabolicEnergy, enzymes) : null;

                    if(bacterium != null) {
                        numBacteria--;
                    }
                }

                if(bacterium != null) {
                    cellStatus[i][j] = new CellStatus(true, nutrient);
                    cells[i][j] = new Cell(i, j, nutrient, bacterium);
                } else {
                    cellStatus[i][j] = new CellStatus(false, nutrient);
                    cells[i][j] = new Cell(i, j, nutrient, null);
                }
            }
        }

        //Sampling interval and samples for the transducer
        int maximumSamples = 50;
        double samplingInterval = 10;
        Transducer transducer = new Transducer(cellStatus, second, maximumSamples, samplingInterval);
        add(transducer);

        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < columns; j++) {
                add(cells[i][j]);
            }
        }

        //Couple the cells to each other and to the transducer
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < columns; j++) {

                Cell cell = cells[i][j];
                addCoupling(this, Port.START.portName, cell, Cell.Port.START.portName());
                addCoupling(cell, Cell.Port.CELL_STATUS.portName(), transducer, Transducer.Port.IN.portName());
                addCoupling(cell, Cell.Port.QUERY.portName(), transducer, Transducer.Port.IN.portName());
                addCoupling(transducer, Transducer.Port.QUERY_RESPONSE.portName(), cell, Cell.Port.QUERY_RESPONSE.portName());

                for(Direction direction : deltaToDirection.keySet()) {
                    Delta delta = deltaToDirection.get(direction);

                    Cell neighbor = cells[wrap(i + delta.getDr(), rows)][wrap(j + delta.getDc(), columns)];
                    addCoupling(cell, directionToCellPort.get(direction).portName(), neighbor, Cell.Port.INPUT_BACTERIUM.portName());
                }
            }
        }
    }

    //Helper function to implement the wrapping of indices.
    private int wrap(int index, int size) {
        if(index < 0) {
            return (size + index) % size;
        } else {
            return (index % size);
        }
    }
    /**
     * Automatically generated by the SimView program.
     * Do not edit this manually, as such changes will get overwritten.
     */
    public void layoutForSimView()
    {
        preferredSize = new Dimension(1578, 987);
        ((ViewableComponent)withName("Cell(0,0)")).setPreferredLocation(new Point(88, 130));
        ((ViewableComponent)withName("Cell(1,1)")).setPreferredLocation(new Point(395, 330));
        ((ViewableComponent)withName("Cell(0,1)")).setPreferredLocation(new Point(394, 129));
        ((ViewableComponent)withName("Cell(2,0)")).setPreferredLocation(new Point(93, 531));
        ((ViewableComponent)withName("Transducer")).setPreferredLocation(new Point(459, 738));
        ((ViewableComponent)withName("Cell(1,0)")).setPreferredLocation(new Point(90, 333));
        ((ViewableComponent)withName("Cell(2,2)")).setPreferredLocation(new Point(691, 527));
        ((ViewableComponent)withName("Cell(2,1)")).setPreferredLocation(new Point(396, 531));
        ((ViewableComponent)withName("Cell(0,2)")).setPreferredLocation(new Point(691, 128));
        ((ViewableComponent)withName("Cell(1,2)")).setPreferredLocation(new Point(689, 331));
    }
}
