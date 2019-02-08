/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;
import autolab.grid.Geogrid;
import com.jhlabs.map.proj.Projection;
import java.awt.BorderLayout;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryCollection;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tsamsonov
 */
public class MapPanel extends JPanel {
    
    private static final float ZOOMFACTOR = 0.1f;
    
    Map map;
    
    MapRenderer mapRenderer;
    
    public GLCanvas canvas;
    
    private final Animator anim;
    
    private final boolean animated;
    
    Point startPoint;
    
    boolean movable;
    
    boolean sendCoordinates = false;
    
    JLabel xValueLabel, yValueLabel, zValueLabel;
    
    int zLayerNumber = 0; // layer from which z-value is extracted for display
    
    
    public MapPanel(Map map){
        super();
        
        this.map = map;
        
        movable = false;
        
        mapRenderer = new MapRenderer(map);
        
        startPoint = new Point(0,0);
                
        canvas = new GLCanvas();
        canvas.setIgnoreRepaint(true);
        canvas.setSize(this.getSize());
        canvas.addGLEventListener(mapRenderer);
        
        anim = new Animator(canvas);
        anim.setRunAsFastAsPossible(true);
        canvas.setSize(1000,695);
        
        add(canvas, BorderLayout.CENTER);
        
        anim.start();
        
        animated = true;
        
        canvas.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e){
                startPoint = e.getPoint();
            }
        });
        
        canvas.addMouseMotionListener(new MouseMotionAdapter(){
            @Override
            public void mouseDragged(MouseEvent e) {
                if(movable){
                    Point p = e.getPoint();
                    int dx = p.x - startPoint.x;
                    int dy = p.y - startPoint.y;

                    mapRenderer.move(dx, -dy);

                    startPoint = p;
                    
                    // show coordinates under cursor
                    if(sendCoordinates){
                        Coordinate c = getCurrentCoordinate();
                        DecimalFormat numFormat= new DecimalFormat("##,###,###.#");
                        xValueLabel.setText(numFormat.format(c.x));
                        yValueLabel.setText(numFormat.format(c.y));
                    }
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e){
                startPoint = e.getPoint();
                // show coordinates under cursor
                if(sendCoordinates){
                    Coordinate c = getCurrentCoordinate();
                    DecimalFormat numFormat= new DecimalFormat("##,###,###.#");
                    xValueLabel.setText(numFormat.format(c.x));
                    yValueLabel.setText(numFormat.format(c.y));
                    
                    Layer lyr = map.getLayer(zLayerNumber);
                    if(lyr instanceof GridLayer){
                        Geogrid g = ((GridLayer)lyr).getGrid();
                        int ij[] = g.getIJfromXY(c.x, c.y);
                        
                        if(ij != null){
                            double z = g.getZ(ij[0], ij[1]);
                            zValueLabel.setText(numFormat.format(z));
                        }
                    }
                    
                }
            }
            
        });
        
        canvas.addMouseWheelListener(new MouseAdapter(){
            @Override
            public void mouseWheelMoved(MouseWheelEvent e){
                float zoom = ZOOMFACTOR * (float)e.getPreciseWheelRotation();
                
                float scale = (zoom < 0) ? 1/(1-zoom) : (1+zoom);
                
                mapRenderer.scale(scale, startPoint.x, startPoint.y);
            }
        });
        
    }
    
    public void setZLayerNumber(int number){
        zLayerNumber = number;
    }
    
    public void setZLabel(JLabel zValue){
        if (zValueLabel != null){
            zValueLabel.setText("");
        }
        
        zValueLabel = zValue;
    }
    
    public void setCoordinateLabels(JLabel xValue, JLabel yValue, JLabel zValue){
        this.xValueLabel = xValue;
        this.yValueLabel = yValue;
        this.zValueLabel = zValue;
        sendCoordinates = true;
    }
    
    public Coordinate getCurrentCoordinate(){
        return mapRenderer.getCoordinates(startPoint.x, startPoint.y);
    }
    
    public void showGraticule(boolean show){
        mapRenderer.showGraticule(show);
    }
    
    public void showTissot(boolean show){
        mapRenderer.showTissot(show);
    }
    
    public void showLabels(boolean show){
        mapRenderer.showLabels(show);
    }
    
    public void setMovable(boolean movable){
        this.movable = movable;
    }
    
    // fits an image into frame
//    public void setFit(int layerNumber, int fit){
//        mapRenderer.setFit(layerNumber, fit);
//    }
    
    public void fitWidth(int layerNumber){
        mapRenderer.fitWidth(layerNumber);
    }
    
    public void fitHeight(int layerNumber){
        mapRenderer.fitHeight(layerNumber);
    }
    
    public void stop(){
        anim.stop();
    }
    
     public void start(){
        anim.start();
    }
    
    public MapRenderer getRenderer(){
        return mapRenderer;
    }
    
    /**
     * Sets graticule parameters
     * @param phi0
     * @param lam0
     * @param phiStep
     * @param lamStep 
     */
    public void setGraticule(int phi0, int lam0, float phiStep, int lamStep){
        mapRenderer.setGraticule(phi0, lam0, phiStep, lamStep);
    }
    
    public void setGraticuleClipped(boolean clipped){
        mapRenderer.setGraticuleClipped(clipped);
    }
    
    public void showVoronoy(boolean show){
        mapRenderer.showVoronoy(show);
    }
    
    /**
     * Sets scaling of tissot indicatrix
     * @param scale 
     */
    public void setTissotScale(double scale){
        mapRenderer.setTissotScale(scale);
    }
    
    /**
     * Sets scaling of tissot indicatrix
     * @param scale 
     */
    public void setLabelsScale(float scale){
        mapRenderer.setLabelsScale(scale);
    }
    
    // sets the envelope of graticule
    public void setGraticuleEnvelope(Envelope env){
        mapRenderer.setGraticuleEnvelope(env);
    }
    
    public GeometryCollection getControlPoints(){
        return mapRenderer.getControlPoints();
    }
    
    public GeometryCollection getControlZones(){
        return mapRenderer.getControlZones();
    }
    
    public void setProjection(Projection projection){
        mapRenderer.setProjection(projection);
    }
    
    public void setGraticuleLabelParameter(String s){
        mapRenderer.setGraticuleLabelParameter(s);
    }
}
