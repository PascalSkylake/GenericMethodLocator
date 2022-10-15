import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.DoubleToIntFunction;


/**
 * A simple interactive 2D graph implementation. 
 * @author paf
 */
public class SimpleGraph extends JPanel {

    private double shiftX = 0.025;
    private double shiftY = 0.025;

    private double maxValueX;
    private double maxValueY;

    private double gridSpreadX;
    private double gridSpreadY;

    private int pointSize = 10;

    private final int tickSize = 8;

    private final List<IGraphShape> shapes = new ArrayList<>();
    private final List<Chunk> chunks = new ArrayList<>();

    private Point glassChunk;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private JFrame frame;


    public SimpleGraph() {
        this(10,10);
    }

    public SimpleGraph(double maxValueX, double maxValueY) {
        this(maxValueX,maxValueY,1.0,1.0);
    }

    public SimpleGraph(double maxValueX, double maxValueY, double gridSpreadX, double gridSpreadY) {
        this.maxValueX = maxValueX;
        this.maxValueY = maxValueY;

        this.gridSpreadX = gridSpreadX;
        this.gridSpreadY = gridSpreadY;

        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(700, 700));

        JPopupMenu popupMenu = new JPopupMenu("Menu");

        popupMenu.add(new JMenuItem(new AbstractAction("Centralize") {

            @Override
            public void actionPerformed(ActionEvent e) {
                centralize();
                repaint();
            }
        }));

        popupMenu.add(new JMenuItem(new AbstractAction("Reset") {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeAllShapes();
                repaint();
            }
        }));

        popupMenu.add(new JMenuItem(new AbstractAction("Enter Glass Chunk") {

            @Override
            public void actionPerformed(ActionEvent e) {
                GlassChunkPanel glassChunkPanel = new GlassChunkPanel();
                int option = JOptionPane.showConfirmDialog(SimpleGraph.this, glassChunkPanel, "Graph settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if(option==JOptionPane.OK_OPTION){
                    glassChunk = new Point((Integer)glassChunkPanel.x.getValue(), (Integer)glassChunkPanel.z.getValue());
                    glassChunkPanel.updateGraph();
                    System.out.println(glassChunk.toString());

                    //calculate best chunk
                    ArrayList<Chunk> toCheck = new ArrayList<>();
                    for (int i = 0; i < 21; i++) {
                        for (int j = 0; j < 21; j++) {
                            for (Chunk c : chunks) {
                                if (c.isPoint(new Point(glassChunk.x - 10 + i, glassChunk.y - 10 + j)))
                                    toCheck.add(c);
                            }
                        }
                    }

                    for (Chunk c : toCheck) {
                        boolean bad = false;
                        for (int i = 0; i < 5; i++) {
                            for (int j = 0; j < 5; j++) {
                                if (((i == 0) || (i == 4)) || ((j == 0) || (j == 4))) {
                                    for (Chunk c2 : chunks) {
                                        if (c2.isPoint(new Point(i - 2 + c.pos.x, j - 2 + c.pos.y))) {
                                            c.amountOfClustering += c2.difference;
                                            System.out.println(c.amountOfClustering);
                                        }
                                    }
                                } else {
                                    for (Chunk c2 : chunks) {
                                        if (c2.isPoint(new Point(i - 2 + c.pos.x, j - 2 + c.pos.y))) {
                                            if (c2.difference != 0) {
                                                bad = true;
                                            }
                                        }
                                    }
                                }
                                if (bad) {
                                    c.amountOfClustering = 0;
                                }
                            }
                        }
                    }

                    Collections.sort(toCheck);
                    for (int i = 1; i < 4; i++){
                        toCheck.get(toCheck.size() - i).colorOverride = true;
                        toCheck.get(toCheck.size() - i).colorOverrideC = Color.GREEN;
                    }
                }
                repaint();
            }
        }));

        popupMenu.add(new JMenuItem(new AbstractAction("Graph settings") {

            @Override
            public void actionPerformed(ActionEvent e) {

                SettingsPanel settingsPanel = new SettingsPanel();

                int option = JOptionPane.showConfirmDialog(SimpleGraph.this, settingsPanel, "Graph settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if(option==JOptionPane.OK_OPTION){
                    settingsPanel.updateGraph();
                }

            }
        }));

        popupMenu.add(new JMenuItem(new CommonUIActions.SelectFile("Select loadedChunks dump",System.getProperty("user.home"),false) {
            @Override
            public void doWithSelectedDirectory(File selectedFile) {
                if(!selectedFile.getName().endsWith(".csv")) {
                    JOptionPane.showMessageDialog(SimpleGraph.this, "Must be a CSV obtained with /loadedChunks dump!");
                } else {
                    try {
                        Scanner sc = new Scanner(selectedFile);
                        sc.useDelimiter(",");
                        sc.nextLine();
                        while (sc.hasNextLine()) {
                            String line = sc.nextLine();
                            ArrayList<Long> lv = new ArrayList<>();
                            Scanner rowScanner = new Scanner(line);
                            rowScanner.useDelimiter(",");
                            try {
                                while (rowScanner.hasNext()) {
                                    String next = rowScanner.next();
                                    if (!next.equals(""))
                                        lv.add(Long.parseLong(next));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                lv.clear();
                                continue;
                            }
                            if (lv.size() > 1) {
                                chunks.add(new Chunk(lv.get(0), lv.get(1), lv.get(2), lv.get(3), lv.get(4)));
                            } else {
                                chunks.add(new Chunk(lv.get(0), 0, 0, 0, 0));
                            }
                            lv.clear();
                        }
                        for (Chunk c : chunks) {
                            if (c.key != 0)
                                addShape(c);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    repaint();
                }
            }
        }));

        setComponentPopupMenu(popupMenu);

        MouseAdapter mouseInputHandler = new MouseAdapter() {

            private double startShiftX;
            private double startShiftY;

            @Override
            public void mousePressed(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)){
                    int width = getWidth();
                    int height = getHeight();

                    Point point = e.getPoint();

                    startShiftX = point.getX()/width - shiftX;
                    startShiftY = 1.0-point.getY()/height - shiftY;

                    repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)){
                    int width = getWidth();
                    int height = getHeight();

                    Point point = e.getPoint();

                    shiftX = point.getX()/width-startShiftX;
                    shiftY = 1.0-point.getY()/height-startShiftY;

                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();

                Point point = e.getPoint();
                double width = getWidth();
                double height = getHeight();

                double b = point.getY()/height;
                double d = point.getX()/width;

                double startX = getXValueFor(d);
                double startY = getYValueFor(b);


                double moveX = rotation*SimpleGraph.this.maxValueX*0.1;
                SimpleGraph.this.maxValueX += moveX;
                double moveY = rotation*SimpleGraph.this.maxValueY*0.1;
                SimpleGraph.this.maxValueY += moveY;


                shiftX = d-startX/SimpleGraph.this.maxValueX;
                shiftY = 1.0-b-startY/SimpleGraph.this.maxValueY;

                repaint();
            }

        };
        addMouseListener(mouseInputHandler);
        addMouseMotionListener(mouseInputHandler);
        addMouseWheelListener(mouseInputHandler);
    }

    public void addShape(IGraphShape graphShape) {
        shapes.add(graphShape);
    }

    public void removeShape(IGraphShape graphShape) {
        shapes.remove(graphShape);
    }

    public void removeAllShapes() {
        shapes.clear();
    }

    public void centralize() {
        shiftX = 0.5;
        shiftY = 0.5;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Color c = g.getColor();
        int width = getWidth();
        int height = getHeight();

        int xCoordinateHeight = (int)((1.0-shiftY)*height);
        int yCoordinateWidth = (int)(shiftX*width);
        int yCoordinateStartHeight = (int)(0.5*shiftY*height);

        double upperXLimit = (1.0-shiftX)*maxValueX;
        double upperYLimit = (1.0-shiftY)*maxValueY;

        //draw grid
        {
            g.setColor(Color.LIGHT_GRAY);

            double start1 = (int) ((upperXLimit - maxValueX) / gridSpreadX) * gridSpreadX;
            for (double xValue = start1; xValue <= upperXLimit; xValue += gridSpreadX) {
                int x = calculateX(yCoordinateWidth, xValue / maxValueX);
                g.drawLine(x, 0, x, height);
            }

            double start3 = (int) ((upperYLimit - maxValueY) / gridSpreadY) * gridSpreadY;
            for (double yValue = start3; yValue <= upperYLimit; yValue += gridSpreadY) {
                int y = calculateY(yCoordinateStartHeight, yValue / maxValueY);
                g.drawLine(0, y, width, y);
            }
            g.setColor(c);
        }

        //draw axes
        {
            g.setColor(Color.BLACK);

            //X axis
            if(xCoordinateHeight>0 && xCoordinateHeight<height)
                g.drawLine(0, xCoordinateHeight, width, xCoordinateHeight);

            //Y axis
            if(yCoordinateWidth>0 && yCoordinateWidth<width)
                g.drawLine(yCoordinateWidth, 0, yCoordinateWidth, height);
        }

        //draw axis ticks
        {
            //X-axis ticks
            double start1 = (int)((upperXLimit-maxValueX)/gridSpreadX)*gridSpreadX;
            for(double p=start1;p<=upperXLimit;p+=gridSpreadX){
                int x = calculateX(yCoordinateWidth, p/maxValueX);
                g.drawLine(x, xCoordinateHeight-tickSize/2, x, xCoordinateHeight+tickSize/2);
                g.drawString(decimalFormat.format(p), x-tickSize, xCoordinateHeight-tickSize);
            }
            //Y-axis ticks
            double start3 = (int)((upperYLimit-maxValueY)/gridSpreadY)*gridSpreadY;
            for(double p=start3;p<=upperYLimit;p+=gridSpreadY){
                int y = calculateY(yCoordinateStartHeight, p/maxValueY);
                g.drawLine(yCoordinateWidth-tickSize/2, y, yCoordinateWidth+tickSize/2, y);
                g.drawString(decimalFormat.format(-p), yCoordinateWidth+tickSize, y+tickSize/2);
            }
        }

        //draw all shapes (including chunks)
        g.setColor(c);
        for(IGraphShape shape:shapes) {
            shape.draw(g, x->calculateX(yCoordinateWidth, x/maxValueX), y->calculateY(yCoordinateStartHeight, y/maxValueY));
        }

        g.setColor(c);
    }

    private double getYValueFor(double percentageOfHeight) {
        return (1.0-shiftY-percentageOfHeight)*SimpleGraph.this.maxValueY;
    }

    private double getXValueFor(double percentageOfWidth) {
        return (percentageOfWidth-shiftX)*SimpleGraph.this.maxValueX;
    }

    private int calculateY(int yCoordinateStartHeight, double value) {
        int height = getHeight();
        return (int)((1.0-value)*height)-2*yCoordinateStartHeight;
    }

    private int calculateX(int yCoordinateWidth, double value) {
        int width = getWidth();
        return (int)(value*width)+yCoordinateWidth;
    }

    private class SettingsPanel extends JPanel{

        private final  JSpinner maxXSpinner;
        private final  JSpinner maxYSpinner;
        private final  JSpinner stepXSpinner;
        private final  JSpinner stepYSpinner;
        private final  JSpinner pointSizeSpinner;


        public SettingsPanel() {
            setLayout(new GridLayout(0, 2));
            maxXSpinner = new JSpinner(new SpinnerNumberModel(SimpleGraph.this.maxValueX, 0.01, 1e6, 0.001));
            maxYSpinner = new JSpinner(new SpinnerNumberModel(SimpleGraph.this.maxValueY, 0.01, 1e6, 0.001));
            stepXSpinner = new JSpinner(new SpinnerNumberModel(SimpleGraph.this.gridSpreadX, 0.001, 1e3, 0.001));
            stepYSpinner = new JSpinner(new SpinnerNumberModel(SimpleGraph.this.gridSpreadY, 0.001, 1e3, 0.001));
            pointSizeSpinner = new JSpinner(new SpinnerNumberModel(SimpleGraph.this.pointSize, 2, 20, 1));

            add(new JLabel("Max X value: "));
            add(maxXSpinner);
            add(new JLabel("Max Y value: "));
            add(maxYSpinner);
            add(new JLabel("Step X: "));
            add(stepXSpinner);
            add(new JLabel("Step Y: "));
            add(stepYSpinner);
            add(new JLabel("Point size: "));
            add(pointSizeSpinner);
        }

        public void updateGraph(){
            SimpleGraph.this.maxValueX = (Double) maxXSpinner.getValue();
            SimpleGraph.this.maxValueY = (Double) maxYSpinner.getValue();
            SimpleGraph.this.gridSpreadX = (Double) stepXSpinner.getValue();
            SimpleGraph.this.gridSpreadY = (Double) stepYSpinner.getValue();
            SimpleGraph.this.pointSize = (Integer) pointSizeSpinner.getValue();

            SimpleGraph.this.repaint();
        }

    }

    private class GlassChunkPanel extends JPanel {
        private final JSpinner x;
        private final JSpinner z;

        public GlassChunkPanel() {
            setLayout(new GridLayout(0, 2));
            x = new JSpinner();
            z = new JSpinner();

            add(new JLabel("Chunk X"));
            add(x);
            add(new JLabel("Chunk Z"));
            add(z);
        }

        public void updateGraph() {
            SimpleGraph.this.glassChunk = new Point((Integer) (x.getValue()), (Integer) (z.getValue()));
        }
    }


    public void display() {
        display(500, 150, 800, 824);
    }


    public void display(int x, int y, int width, int height) {
        SwingUtilities.invokeLater(()->{

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}

            frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setBounds(x, y, width, height);

            frame.add(new JScrollPane(this),BorderLayout.CENTER);
            frame.setMinimumSize(new Dimension(300, 300));

            frame.setVisible(true);
        });
    }




    public interface IGraphShape {
        void draw(Graphics g, DoubleToIntFunction xValueToScreenPosition, DoubleToIntFunction yValueToScreenPosition);
    }

    public class Chunk implements IGraphShape, Comparable {

        private final Point pos;
        private final int index;
        private final int hash;
        public final int difference;
        private final long key;
        private final boolean clustered = true;
        public boolean colorOverride;
        public Color colorOverrideC;
        public int amountOfClustering;

        public Chunk(long index, long key, long x, long y, long hash) {
            this.index = (int)index;
            this.key = key;
            this.pos = new Point((int)x, (int)y);
            this.hash = (int)hash;
            this.difference = this.index - this.hash;
            this.amountOfClustering = 0;
        }

        public boolean isPoint(Point p) {
            return p.x == pos.x && p.y == pos.y;
        }

        @Override
        public void draw(Graphics g, DoubleToIntFunction xValueToScreenPosition, DoubleToIntFunction yValueToScreenPosition) {
            //optimization so your computer doesn't break
            if (xValueToScreenPosition.applyAsInt(pos.x - 1) > SimpleGraph.this.frame.getWidth() || yValueToScreenPosition.applyAsInt(-pos.y + 1) > SimpleGraph.this.frame.getHeight() + 24 || xValueToScreenPosition.applyAsInt(pos.x) < 0 || yValueToScreenPosition.applyAsInt(-pos.y) < 0)
                return;

            if (clustered && !colorOverride) {
                g.setColor(new Color(difference / 20, 0, 0));
            } else if (colorOverride) {
                g.setColor(colorOverrideC);
            }
            int width, height;


            width = (xValueToScreenPosition.applyAsInt(pos.x) - xValueToScreenPosition.applyAsInt(pos.x + 1)) - 1;
            height = yValueToScreenPosition.applyAsInt(-pos.y) - yValueToScreenPosition.applyAsInt(-pos.y + 1);
            g.drawRect(xValueToScreenPosition.applyAsInt(pos.x), yValueToScreenPosition.applyAsInt(-pos.y + 1), width - 1, height);
            g.setColor(Color.gray);
            g.setFont(g.getFont().deriveFont(7f));
            g.drawString("(" + pos.x + "," + pos.y + ")", xValueToScreenPosition.applyAsInt(pos.x - 1), yValueToScreenPosition.applyAsInt(-pos.y + 1) + 10);
            g.setFont(g.getFont().deriveFont(9f));
            g.drawString("" + difference, xValueToScreenPosition.applyAsInt(pos.x - 1), yValueToScreenPosition.applyAsInt(-pos.y + 1) + 20);
            g.drawString("" + amountOfClustering, xValueToScreenPosition.applyAsInt(pos.x - 1), yValueToScreenPosition.applyAsInt(-pos.y + 1) + 30);
        }

        @Override
        public int compareTo(Object o) {
            return this.amountOfClustering - ((Chunk) o).amountOfClustering;
        }
    }
}