package VivinPaliath;


import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI for the model. Shows a 2-D graph with a cellular-automata like display and a graph and some textual information on the right side of the screen.
 */
public class Visualization extends JPanel {

    //Rows and columns of the grid
    private int rows;
    private int columns;
    private Rectangle[][] cells;
    private Graphics2D graphics2D;
    private int numSamples = 100;

    private VisualizationContext visualizationContext = null;

    //Height and width of the grid in the GUI
    private static int GRID_WIDTH = 1465;
    private static int GRID_HEIGHT = 1172;

    public Visualization(int numSamples, VisualizationContext visualizationContext) {
        this.visualizationContext = visualizationContext;

        this.cells = new Rectangle[visualizationContext.getCellStatus().length][visualizationContext.getCellStatus()[0].length];
        this.rows = visualizationContext.getCellStatus().length;
        this.columns = visualizationContext.getCellStatus()[0].length;
        this.numSamples = numSamples;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        graphics2D = (Graphics2D) g.create();

        int width = GRID_WIDTH;
        int height = GRID_HEIGHT;

        int cellWidth = width / columns;
        int cellHeight = height / rows;

        int xOffset = ((width - (columns * cellWidth)) / 2);
        int yOffset = (height - (rows * cellHeight)) / 2;

        CellStatus[][] cellStatus = visualizationContext.getCellStatus();

        //Draw the grid and fill it with appropriate colors based on the nutrients in the cell
        //and whether the cell is occupied or not.
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < columns; j++) {
                Rectangle cell;

                if(cells[i][j] == null) {
                    cell = new Rectangle(
                            xOffset + (j * cellWidth),
                            yOffset + (i * cellHeight),
                            cellWidth,
                            cellHeight
                    );
                } else {
                    cell = cells[i][j];
                }

                cells[i][j] = cell;

                graphics2D.setColor(nutrientToColor(cellStatus[i][j].getNutrient()));
                graphics2D.fill(cell);

                if(cellStatus[i][j].isOccupied()) {
                    graphics2D.setColor(Color.RED);
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) cellStatus[i][j].getRatio()));
                    graphics2D.fill(cell);
                    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
                }

                graphics2D.setColor(Color.BLACK);
                graphics2D.draw(cell);
            }
        }

        //Draw the graph that tracks average efficiencies
        final int GRAPH_WIDTH = 400;
        final int GRAPH_HEIGHT = 250;

        final int GRAPH_LEFT_BOUNDARY = 1500;
        final int GRAPH_RIGHT_BOUNDARY = GRAPH_LEFT_BOUNDARY + GRAPH_WIDTH;
        final int GRAPH_TOP_BOUNDARY = 500;
        final int GRAPH_BOTTOM_BOUNDARY = GRAPH_TOP_BOUNDARY + GRAPH_HEIGHT;

        Map<Integer, Double> averageEfficiencies = visualizationContext.getAverageEfficiencies();

        graphics2D.drawLine(GRAPH_LEFT_BOUNDARY, GRAPH_TOP_BOUNDARY, GRAPH_LEFT_BOUNDARY, GRAPH_BOTTOM_BOUNDARY);
        graphics2D.drawLine(GRAPH_LEFT_BOUNDARY, GRAPH_BOTTOM_BOUNDARY, GRAPH_RIGHT_BOUNDARY, GRAPH_BOTTOM_BOUNDARY);

        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = new Font("Sans Serif", Font.BOLD, 16);
        graphics2D.setFont(font);

        graphics2D.drawString("Time", GRAPH_LEFT_BOUNDARY + 180, GRAPH_BOTTOM_BOUNDARY + 25);

        AffineTransform transform = new AffineTransform();
        transform.rotate(-Math.PI / 2);

        Font vertical = font.deriveFont(transform);
        graphics2D.setFont(vertical);

        graphics2D.drawString("Average Efficiency (%)", GRAPH_LEFT_BOUNDARY - 10, GRAPH_BOTTOM_BOUNDARY - 25);
        graphics2D.setFont(font);

        List<Integer> keyList = new ArrayList<>();

        synchronized (averageEfficiencies) {
            keyList.addAll(averageEfficiencies.keySet());

            for(int i = 0; i < keyList.size() - 1; i++) {
                int first = (int) Math.round(averageEfficiencies.get(keyList.get(i)) * 100);
                int second = (int) Math.round(averageEfficiencies.get(keyList.get(i + 1)) * 100);

                int firstY = (int) Math.round(GRAPH_BOTTOM_BOUNDARY - (2.5 * first));
                int secondY = (int) Math.round(GRAPH_BOTTOM_BOUNDARY - + (2.5 * second));

                int firstX = GRAPH_LEFT_BOUNDARY + (i * (GRAPH_WIDTH / numSamples));
                int secondX = GRAPH_LEFT_BOUNDARY + ((i + 1) * (GRAPH_WIDTH / numSamples));

                graphics2D.setColor(new Color(0, 127, 0));
                graphics2D.drawLine(firstX, firstY, secondX, secondY);
            }

            double latestAverageEfficiency = 0;
            if(keyList.size() > 0) {
                latestAverageEfficiency = averageEfficiencies.get(keyList.get(keyList.size() - 1));
            }

            graphics2D.setColor(Color.BLACK);

            graphics2D.drawString("Target nutrient: " + visualizationContext.getTargetNutrient().getBitPattern(), GRAPH_LEFT_BOUNDARY, GRAPH_TOP_BOUNDARY - 150);
            graphics2D.drawString("Most efficient enzyme: " + (visualizationContext.getMaxEfficientEnzyme() == null? "n/a" : visualizationContext.getMaxEfficientEnzyme().getBitPattern()), GRAPH_LEFT_BOUNDARY, GRAPH_TOP_BOUNDARY - 125);
            graphics2D.drawString("Enzyme efficiency: " + String.format("%.2f", (visualizationContext.getEnzymeEfficiency() * 100)) + "%", GRAPH_LEFT_BOUNDARY, GRAPH_TOP_BOUNDARY - 100);
            graphics2D.drawString("Current average efficiency: " + String.format("%.2f", (latestAverageEfficiency * 100)) + "%", GRAPH_LEFT_BOUNDARY, GRAPH_TOP_BOUNDARY - 75);
        }


        graphics2D.dispose();
    }

    //A helped function that converts a 16-bit nutrient value into a 24-bit color. This is basically a conversion of
    //a 5-6-5 16-bit RGB color into a 24-bit RGB color.
    private Color nutrientToColor(Nutrient nutrient) {
        String bitPattern = nutrient.getBitPattern();

        int red = Integer.parseInt(bitPattern.substring(0, 5), 2);
        int green = Integer.parseInt(bitPattern.substring(5, 11), 2);
        int blue = Integer.parseInt(bitPattern.substring(11), 2);

        int red24bit = (red << 3) | red;
        int green24bit = (green << 2) | green;
        int blue24bit = (blue << 3) | blue;

        return new Color(red24bit, green24bit, blue24bit);
    }
}
