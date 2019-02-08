/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.symbols;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import java.awt.Color;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author timofey
 */
public class LineSimple extends Symbol {
    Color strokeColor;
    float strokeWidth;
    
    public LineSimple(){
        strokeColor = Color.BLACK;
        strokeWidth = 1.0f;
    }
    
    /**
     * Sets the current strokeColor to the passed argument 
     * @param c — the strokeColor
     */
    public void setStrokeColor(Color c){
        strokeColor = c;
    }
    
    /**
     * Sets the current strokeColor to the passed argument
     * @param w — the strokeWidth
     */
    public void setStrokeWidth(float w){
        strokeWidth = w;
    }

    @Override
    public void draw(Geometry g, GLAutoDrawable drawable) {
        if(g instanceof LineString){
            LineString line = (LineString)g;
            drawLine(line, drawable);
        } else if(g instanceof MultiLineString){
            MultiLineString multiLine = (MultiLineString)g;
            // MultiLineString can consist from MultiLineStrings again
            // So the draw function is called recursively until it decomposes
            // line into simple LineStrings
            for (int i = 0; i < multiLine.getNumGeometries(); i++) {
                Geometry line = multiLine.getGeometryN(i);
                draw(line, drawable);
            }
        }
    }
    
    /**
     * Draws the line
     * @param line
     * @param drawable 
     */
    public void drawLine(LineString line, GLAutoDrawable drawable){
        final GL2 gl = drawable.getGL().getGL2();
        gl.glColor3f(strokeColor.getRed()/255f, strokeColor.getGreen()/255f, strokeColor.getBlue()/255f);
        gl.glLineWidth(strokeWidth);

        gl.glBegin(GL2.GL_LINE_STRIP);
        for (int i = 0; i < line.getNumPoints(); i++) {
            gl.glVertex2d(line.getCoordinateN(i).x, line.getCoordinateN(i).y);
        }
        gl.glEnd();
        gl.glFlush();
    }
}
