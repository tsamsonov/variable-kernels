/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.symbols;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import java.awt.Color;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author timofey
 */
public class PolySimple extends LineSimple {
    Color fillColor;
    
    public PolySimple(){
        super();
        fillColor = Color.WHITE;
    }
    
    /**
     * Sets the current color to the passed argument 
     * @param c — the color
     */
    public void setFillColor(Color c){
        fillColor = c;
    }
    
    @Override
    public void draw(Geometry g, GLAutoDrawable drawable) {
        if(g instanceof Polygon){
            Polygon poly = (Polygon)g;
            
            final GL2 gl = drawable.getGL().getGL2();
            
            
            Coordinate[] coords = poly.getCoordinates();
            
            boolean fillvisible = (boolean)poly.getUserData();
            
            if(fillvisible){
                gl.glColor4f(strokeColor.getRed()/255f, strokeColor.getGreen()/255f, strokeColor.getBlue()/255f, 0.5f);
                gl.glBegin(GL2.GL_POLYGON);
                for (Coordinate coord : coords) {
                    gl.glVertex2d(coord.x, coord.y);
                }
                gl.glEnd();
            } 
            
            gl.glColor3f(strokeColor.getRed()/255f, strokeColor.getGreen()/255f, strokeColor.getBlue()/255f);
            gl.glLineWidth(strokeWidth);

            gl.glBegin(GL2.GL_LINE_STRIP);
            for (Coordinate coord : coords) {
                gl.glVertex2d(coord.x, coord.y);
            }
            gl.glEnd();
            
            gl.glFlush();
        }
    }
}
