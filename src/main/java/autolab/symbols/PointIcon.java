/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package autolab.symbols;
import org.locationtech.jts.geom.Geometry;
import java.awt.image.Raster;
import java.awt.Color;
import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author timofey
 */
public class PointIcon extends Symbol{
    Raster icon;
    Color fillColor;
    Color strokeColor;
    float size;
    float strokeWidth;

    @Override
    public void draw(Geometry g, GLAutoDrawable drawable) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
