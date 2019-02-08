/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.geolayer;

import autolab.grid.Geogrid;
import autolab.grid.GridHeader;
import autolab.math.Stats.StretchMethod;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import java.nio.ByteBuffer;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_UNPACK_ALIGNMENT;
import static com.jogamp.opengl.GL2ES2.GL_UNPACK_SKIP_PIXELS;
import static com.jogamp.opengl.GL2ES2.GL_UNPACK_SKIP_ROWS;

/**
 *
 * @author tsamsonov
 */
public class GridLayer extends Layer {
    
    /*
    Grid used for rendering
    */
    Geogrid grid;
    
    /*
    Grayscale image using histogram equalize method
    */
    ByteBuffer imgBufferEqualize;
    
    /*
    Grayscale image using minimum-maximum method
    */
    ByteBuffer imgBufferMinmax;
    
    /*
    Grayscale image using standard deviations method
    */
    ByteBuffer imgBufferStdev;
    
    /*
    Points to selected buffer for drawing
    */
    ByteBuffer imgBufferForDrawing;
    
    /*
    Histogram stretch method used for rendering
    */
    StretchMethod stretchMethod;
    
    int width;
    
    int height;
    
    /*
    Byte frequency histogram
    */
    int freqs[];
    
    /*
    Byte cumulative frequency histogram
    */
    int cfreqs[];
    
    /**
     * Sets an image for display
     * @param grid 
     */
    public GridLayer(Geogrid grid){
        this.grid = grid;
        stretchMethod = StretchMethod.MINMAX;
        prepareGrayscaleImageForRendering();
        isVisible = true;
        
        GridHeader h = grid.getHeader();
        
        envelope = new Envelope(h.xmin, h.xmax, h.ymin, h.ymax);
        
    }
    
    
    /**
     * Returns current grid
     * @return 
     */
    public Geogrid getGrid(){
        return grid;
    }
    
    /**
     * Sets current grid
     * @param grid 
     */
    public void setGrid(Geogrid grid){
        this.grid = grid;
        prepareGrayscaleImageForRendering();
    }
    
    /**
     * Returns current stretch method for grid layer
     * @return 
     */
    public StretchMethod getStretchMethod(){
        return stretchMethod;
    }
    
    public void setStretchmethod(StretchMethod sm){
        stretchMethod = sm;
        stretchHistogram();
    }
    
    
    /**
     * Creates byte array for OpenGL rendering as pixmap 
     */
    private void prepareGrayscaleImageForRendering(){
        int npx = grid.getHeader().ncol * grid.getHeader().nrow;
        float min = grid.getHeader().zmin;
        float max = grid.getHeader().zmax;
        
        width = grid.getHeader().ncol;
        height = grid.getHeader().nrow;
        
        // grayscale bytes
        byte[] bytes = new byte[npx];
        
        // bytes frequencies
        freqs = new int[127];
        for (int i = 0; i < freqs.length; i++) {
            freqs[i] = 0;
        }

        // 1. Use min-max method for colouring pixels
        int nvalidpx = 0;
        int k = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                
                float z = grid.getZ(i, j);
                
                if(z == grid.getHeader().noData || Float.isNaN(z)){
                   bytes[k] = (byte) 127; 
                } else {
                   bytes[k] = (byte)((126*(float)(z-min)/(float)(max-min)));
                   freqs[bytes[k]]++;
                   nvalidpx++;
                }
                k++;
                
            }
        }
        
        imgBufferMinmax = ByteBuffer.wrap(bytes);
        
        // cumulative frequency of each element
        cfreqs = new int[127];
        cfreqs[0] = freqs[0];
        for (int b = 1; b < 127; b++) {
            cfreqs[b] = cfreqs[b-1] + freqs[b];
        }
        
        imgBufferForDrawing = imgBufferMinmax;
        
        // Fast histogram equalization using rendrered(1) values
        byte[] img = new byte[npx];
        int denominator = nvalidpx-cfreqs[0];
        if(denominator == 0)
            denominator = 1;
        for (int i = 0; i < img.length; i++) {
            if(bytes[i]==127)
                img[i] = 127;
            else
                img[i] = (byte) ((126*(float)(cfreqs[bytes[i]]-cfreqs[0])/(float)denominator));
        }

        imgBufferEqualize = ByteBuffer.wrap(img);
    }
    
    /**
     * 
     */
    private void stretchHistogram(){
        switch(stretchMethod){
            case MINMAX:
                imgBufferForDrawing = imgBufferMinmax;
                break;
            case EQUALIZE:
                imgBufferForDrawing = imgBufferEqualize;
                break;
            case STDEV:
                imgBufferForDrawing = imgBufferMinmax;
                break;
            default:
                break;
        }
    }
    
    /**
     * Draws the underlying grid
     * @param drawable 
     */
    @Override
    public void drawLayer(GLAutoDrawable drawable) {
        
        if (isVisible == false){
            return;
        }
        
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        GLU glu;
        
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        gl.glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        
//        if (imgBufferForDrawing != null){
//            if(fit)
//                fit(drawable);
//            else if(fitWidth)
//                fitWidth(drawable);
//            else if(fitHeight)
//                fitHeight(drawable);
//        }
        
        // Get current view port
        int[] viewPort = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
        
        // calculate current scale
        double[] matrix = new double[16];
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, matrix, 0);
        
        // normalized scales
        float scaleX  = (float) (matrix[0]);
        float scaleY  = (float) (matrix[5]);
        
        double deltaX = (float) (matrix[12]);
        double deltaY = (float) (matrix[13]);
        
        // place the raster at the center of the viewport
        gl.glRasterPos2d(-deltaX/scaleX, -deltaY/scaleY);
        
        // calculate true viewport position
        double xscreen = grid.getHeader().xmin*scaleX + deltaX;
        double yscreen = grid.getHeader().ymin*scaleY + deltaY;
        
        int wptWidth = viewPort[2];
        int wptHeight = viewPort[3];
        
        int col = (int)(0.5*wptWidth*(xscreen));
        int row = (int)(0.5*wptHeight*(yscreen));
        
        // move raster position to the true value
        gl.glBitmap(0, 0, 0, 0, col, row, null);
        
        // draw raster
        
        gl.glDrawPixels(width, height, GL.GL_LUMINANCE, GL.GL_BYTE, imgBufferForDrawing);
        
    }
    
    Envelope getPixelEnvelope() {
        GridHeader h = grid.getHeader();
        return new Envelope(0, h.ncol, 0, h.nrow);
    }
    
//    /**
//     * Fits image into height of the viewport
//     * @param drawable 
//     */
//    @Override
//    void fitHeight(GLAutoDrawable drawable){
//        GL2 gl = drawable.getGL().getGL2();
//        
//        // Zoom to fit by shortest side
//        int[] viewPort = new int[4];
//        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
//        int wptWidth = viewPort[2];
//        int wptHeight = viewPort[3];
//        float hRatio = (float)wptHeight/(float)height;
//        
//        gl.glPixelZoom(hRatio, hRatio);
//        
//        double xmax = wptWidth*(grid.getHeader().ymax-grid.getHeader().ymin)/wptHeight + grid.getHeader().xmin;
//        gl.glMatrixMode(GL2.GL_PROJECTION);
//        gl.glLoadIdentity();
//        
//        gl.glOrtho(grid.getHeader().xmin, xmax, grid.getHeader().ymin, grid.getHeader().ymax, -1, 1);
//        
//        fitHeight = false;
//    }
//    
//    /**
//     * Fits image into width of the viewport
//     * @param drawable 
//     */
//    @Override
//    void fitWidth(GLAutoDrawable drawable){
//        GL2 gl = drawable.getGL().getGL2();
//        
//        // Zoom to fit by shortest side
//        int[] viewPort = new int[4];
//        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
//        int wptWidth = viewPort[2];
//        int wptHeight = viewPort[3];
//        float wRatio = (float)wptWidth/(float)width;
//        gl.glPixelZoom(wRatio, wRatio);
//        
//        double ymax = wptHeight*(grid.getHeader().xmax-grid.getHeader().xmin)/wptWidth + grid.getHeader().ymin;
//        gl.glMatrixMode(GL2.GL_PROJECTION);
//        gl.glLoadIdentity();
//        gl.glOrtho(grid.getHeader().xmin, grid.getHeader().xmax, grid.getHeader().ymin, ymax+1, -1, 1);
//        
//        fitWidth = false;
//    }
//    
//    /** Fits image into the viewport
//     * @param drawable 
//     */
//    @Override
//    void fit(GLAutoDrawable drawable){
//        GL2 gl = drawable.getGL().getGL2();
//        
//        // Zoom to fit by shortest side
//        int[] viewPort = new int[4];
//        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewPort, 0);
//        int wptWidth = viewPort[2];
//        int wptHeight = viewPort[3];
//        float wRatio = (float)wptWidth/(float)width;
//        float hRatio = (float)wptHeight/(float)height;
//        float zoom = (wRatio < hRatio) ? wRatio : hRatio;
//        gl.glPixelZoom(zoom, zoom);
//        
//        fit = false;
//    }

    @Override
    public void add(Geometry g) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void remove(int n) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
