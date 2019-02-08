/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;

import com.jhlabs.map.proj.Projection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Dimension;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryCollection;
import java.awt.Toolkit;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

/**
 * MapRenderer class is used to render map and graticule in the scene
 * @author tsamsonov
 */
public class MapRenderer implements GLEventListener {
    
    /*
    THE MAP
    */
    Map map;
    
    /*
    Graticule to draw upon map
    */
    Graticule graticule;
    
    /*
    Translations during visualization
    */
    int dx = 0; 
    int dy = 0;
    
    /*
    Scale during visualization
    */
    
    float scale  = 1.0f;
    
    /*
    Points around which the scaling should be made
    */
    int cx = 0;
    int cy = 0;
    
    /*
    current projection matrix
    */
    double[] matrix;
    
    /*
    current viewport
    */
    int[] viewPort;
    
    /*
    Screen resolution
    */
    int resolution;
    
    /*
    Envelope to fit into
    */
    Envelope fitEnvelope; 
    
    /*
    Pixel envelope to fit into
    */
    Envelope fitPixelEnvelope;
    
    /*
    Fitting mode
    */
    byte fitMode = 0;
            
    public MapRenderer(Map map){
        super();
        this.map = map;
        graticule = new Graticule();
        
        matrix = new double[16];
        viewPort = new int[4];
        
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        
        resolution = toolkit.getScreenResolution();
    }
    
    /**
     * Sets fitWidth flag for the map
     * @param f 
     */
    
//    public void setFit(int layerNumber, int fit){
//        map.setFit(layerNumber, fit);
//    }
    
    void fitWidth(int layerNumber) {
        Layer lyr = map.getLayer(layerNumber);
        fitEnvelope = lyr.getEnvelope();
        if(lyr instanceof GridLayer){
            fitPixelEnvelope = ((GridLayer)lyr).getPixelEnvelope();
        }
        fitMode = 1;
    }
    
    void fitHeight(int layerNumber) {
        Layer lyr = map.getLayer(layerNumber);
        fitEnvelope = lyr.getEnvelope();
        if(lyr instanceof GridLayer){
            fitPixelEnvelope = ((GridLayer)lyr).getPixelEnvelope();
        }
        fitMode = 2;
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        
        // initialize current projection and viewport backup
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, matrix, 0);
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
        
        // Enable blending and antialiasing
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_BLEND);
        
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }
    
    /**
     * Moves the map in dx and dy directions
     * @param dx
     * @param dy 
     */
    public void move(int dx, int dy){
        this.dx = dx;
        this.dy = dy;
    }
    
    /**
     * Scales the map around the point (x, y)
     * @param scale 
     * @param cx 
     * @param cy 
     */
    public void scale(float scale, int cx, int cy){
        this.scale = scale;
        this.cx = cx;
        this.cy = cy;
    }
    
    /**
     * Returns projected coordinates from screen coordinates
     * @param col canvas coordinate
     * @param row canvas coordinate
     * @return 
     */
    public Coordinate getCoordinates(int col, int row){
        
        int width = viewPort[2];
        int height = viewPort[3];
        
        float scaleX  = (float) (matrix[0]);
        float scaleY  = (float) (matrix[5]);
        
        float deltaX = (float) (matrix[12]);
        float deltaY = (float) (matrix[13]);
        
        // viewport coordinates [-1, 1]
        double xvp =   2.f * (float)col/width - 1.f;
        double yvp = -(2.f * (float)row/height - 1.f);
        
        // projection coordinates
        double x = (xvp - deltaX)/scaleX;
        double y = (yvp - deltaY)/scaleY;
        
        Coordinate c = new Coordinate(x,y);
        
        return c;
    }
    
    @Override
    public void display(GLAutoDrawable drawable) {
        
        GL2 gl = drawable.getGL().getGL2();
        
        // backup current projection and viewport
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, matrix, 0);
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
        
        // if map should be moved
        if(dx != 0 || dy != 0){
            // calculate current scale
            
            int wptWidth = viewPort[2];
            int wptHeight = viewPort[3];
            
            // normalized scales
            double scaleX  = matrix[0]*wptWidth/4;
            double scaleY  = matrix[5]*wptHeight/4;
            
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glTranslated(dx/scaleX, dy/scaleY, 0);
            
            dx = dy = 0;
        }
        
        // if map should be scaled
        if(scale != 1.0){
            gl.glMatrixMode(GL2.GL_PROJECTION);
            
            Coordinate c = getCoordinates(cx, cy);
            
            gl.glTranslated(c.x, c.y, 0);
            gl.glScaled(scale, scale, 1);
            gl.glTranslated(-c.x, -c.y, 0);
            
            float zoom[] = new float[1];
            
            gl.glGetFloatv(GL2.GL_ZOOM_X, zoom, 0);
            gl.glPixelZoom(zoom[0]*scale, zoom[0]*scale);
            
            scale = 1.0f;
        }
        
        // if map should be fitted
        if(fitMode == 1){ // fit raster by width
            
            double width = fitPixelEnvelope.getWidth();
            
            // Zoom to fit by shortest side
            int[] viewPort = new int[4];
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
            int wptWidth = viewPort[2];
            int wptHeight = viewPort[3];
            float wRatio = (float)wptWidth/(float)width;
            
            gl.glPixelZoom(wRatio, wRatio);

            double ymax = wptHeight*(fitEnvelope.getMaxX() - fitEnvelope.getMinX())/wptWidth + fitEnvelope.getMinY();
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrtho(fitEnvelope.getMinX(), fitEnvelope.getMaxX(), fitEnvelope.getMinY(), ymax+1, -1, 1);
            
            fitMode = 0; // reset fitting
            
        } else if (fitMode == 2){ // fit raster by height
            
            double height = fitPixelEnvelope.getHeight();
            
            // Zoom to fit by shortest side
            int[] viewPort = new int[4];
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
            int wptWidth = viewPort[2];
            int wptHeight = viewPort[3];
            float hRatio = (float)wptHeight/(float)height;

            gl.glPixelZoom(hRatio, hRatio);

            double xmax = wptWidth*(fitEnvelope.getMaxY() - fitEnvelope.getMinY())/wptHeight + fitEnvelope.getMinX();
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(fitEnvelope.getMinX(), xmax, fitEnvelope.getMinY(), fitEnvelope.getMaxY(), -1, 1);
            
            fitMode = 0;
            
        }
        
        map.draw(drawable);
        
        graticule.draw(drawable);
        
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }
    
    public void showGraticule(boolean show){
        graticule.setGraticuleVisible(show);
    }
    
    public void showTissot(boolean show){
        graticule.setTissotVisible(show);
    }
    
    public void showVoronoy(boolean show){
        graticule.setVoronoyVisible(show);
    }
    
    public void showLabels(boolean show) {
        graticule.setTextVisible(show);
    }
    
    
    /**
     * Sets graticule parameters
     * @param phi0
     * @param lam0
     * @param phiStep
     * @param lamStep 
     */
    public void setGraticule(int phi0, int lam0, float phiStep, int lamStep){
        graticule.setGraticule(phi0, lam0, phiStep, lamStep);
    }
    
    public void setGraticuleClipped(boolean clipped){
        graticule.setClipped(clipped);
    }
    
    /**
     * Sets scaling of tissot indicatrix
     * @param scale 
     */
    public void setTissotScale(double scale){
        graticule.setTissotScale(scale);
    }
    
    public void setLabelsScale(float scale){
        graticule.setLabelsScale(scale);
    }
    
    public void setGraticuleEnvelope(Envelope env){
        graticule.setEnvelope(env);
    }
    
    public GeometryCollection getControlPoints(){
        return graticule.getControlPoints();
    }
    
    public GeometryCollection getControlZones(){
        return graticule.getControlZones();
    }
    
    public void setProjection(Projection projection){
        graticule.setProjection(projection);
    }

    void setGraticuleLabelParameter(String s) {
        graticule.setLabelParameter(s);
    }

    

}
