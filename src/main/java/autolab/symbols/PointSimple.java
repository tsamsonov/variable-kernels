/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.symbols;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import java.awt.Color;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author timofey
 */
public class PointSimple extends Symbol {
    Color fillColor;
    Color strokeColor;
    float size;
    float strokeWidth;
    
    
    public PointSimple(){
        fillColor = Color.CYAN;
        strokeColor = Color.MAGENTA;
        size = 8.0f;
        strokeWidth = 1.0f;
    }

    @Override
    public void draw(Geometry g, GLAutoDrawable drawable) {
        if(g instanceof Point){
            Point p = (Point)g;
            final GL2 gl = drawable.getGL().getGL2();
            
            gl.glPointSize(size);
            
            gl.glColor3f(fillColor.getRed()/255f, fillColor.getGreen()/255f, fillColor.getBlue()/255f);
            
            gl.glBegin(GL2.GL_POINTS);
                gl.glVertex2d(p.getX(), p.getY());
            gl.glEnd();
        }
    }
}
